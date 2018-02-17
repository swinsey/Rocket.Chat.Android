package chat.rocket.android.widget.emoji

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.TabLayout
import android.support.v4.app.DialogFragment
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import chat.rocket.android.R
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator


open class EmojiBottomPicker : DialogFragment() {
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout

    companion object {
        const val PREF_EMOJI_RECENTS = "PREF_EMOJI_RECENTS"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.emoji_popup_layout, container, false)
        viewPager = view.findViewById(R.id.pager_categories)
        tabLayout = view.findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val parent = dialog.findViewById<View>(R.id.design_bottom_sheet)
                parent?.let {
                    val bottomSheetBehavior = BottomSheetBehavior.from(parent)
                    if (bottomSheetBehavior != null) {
                        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            }

                            override fun onStateChanged(bottomSheet: View, newState: Int) {
                                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                }
                            }
                        })
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                }
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val callback = when (activity) {
            is OnEmojiClickCallback -> activity as OnEmojiClickCallback
            else -> {
                val fragments = activity?.supportFragmentManager?.fragments
                if (fragments == null || fragments.size == 0 || !(fragments[0] is OnEmojiClickCallback)) {
                    throw IllegalStateException("activity/fragment should implement OnEmojiClickCallback interface")
                }
                fragments[0] as OnEmojiClickCallback
            }
        }

        viewPager.adapter = CategoryPagerAdapter(object : OnEmojiClickCallback {
            override fun onEmojiAdded(emoji: Emoji) {
                dismiss()
                EmojiLoader.addToRecents(emoji)
                callback.onEmojiAdded(emoji)
            }
        })

        for (category in EmojiCategory.values()) {
            val tab = tabLayout.getTabAt(category.ordinal)
            val tabView = layoutInflater.inflate(R.layout.emoji_picker_tab, null)
            tab?.setCustomView(tabView)
            val textView = tabView.findViewById(R.id.text) as TextView
            textView.text = category.icon()
        }

        val currentTab = if (EmojiLoader.getRecents().isEmpty()) EmojiCategory.PEOPLE.ordinal else
            EmojiCategory.RECENTS.ordinal
        viewPager.setCurrentItem(currentTab)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(context!!, theme)
    }

    class EmojiTextWatcher(val editor: EditText) : TextWatcher {
        @Volatile private var emojiToRemove = mutableListOf<EmojiTypefaceSpan>()

        override fun afterTextChanged(s: Editable) {
            val message = editor.getEditableText()

            // Commit the emoticons to be removed.
            for (span in emojiToRemove.toList()) {
                val start = message.getSpanStart(span)
                val end = message.getSpanEnd(span)

                // Remove the span
                message.removeSpan(span)

                // Remove the remaining emoticon text.
                if (start != end) {
                    message.delete(start, end)
                }
                break
            }
            emojiToRemove.clear()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (after < count) {
                val end = start + count
                val message = editor.getEditableText()
                val list = message.getSpans(start, end, EmojiTypefaceSpan::class.java)

                for (span in list) {
                    val spanStart = message.getSpanStart(span)
                    val spanEnd = message.getSpanEnd(span)
                    if (spanStart < end && spanEnd > start) {
                        // Add to remove list
                        emojiToRemove.add(span)
                    }
                }
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }
    }

    interface OnEmojiClickCallback {
        /**
         * Callback triggered after an emoji is selected on the picker.
         *
         * @param emoji The selected emoji
         */
        fun onEmojiAdded(emoji: Emoji)
    }
}