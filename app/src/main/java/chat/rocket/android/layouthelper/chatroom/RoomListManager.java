package chat.rocket.android.layouthelper.chatroom;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import chat.rocket.android.helper.TextUtils;
import chat.rocket.android.model.ddp.RoomSubscription;
import chat.rocket.android.widget.internal.RoomListItemView;

/**
 * Utility class for mapping Room list into channel list ViewGroup.
 */
public class RoomListManager {
  private ViewGroup channelsContainer;
  private ViewGroup dmContainer;
  private OnItemClickListener listener;

  /**
   * constructor with two ViewGroups.
   */
  public RoomListManager(ViewGroup channelsContainer, ViewGroup dmContainer) {
    this.channelsContainer = channelsContainer;
    this.dmContainer = dmContainer;
  }

  private static void removeItemIfExists(ViewGroup parent, String roomName) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      RoomListItemView roomListItemView = (RoomListItemView) parent.getChildAt(i);
      if (roomName.equals(roomListItemView.getRoomName())) {
        parent.removeViewAt(i);
        break;
      }
    }
  }

  /**
   * update ViewGroups with room list.
   */
  public void setRooms(List<RoomSubscription> roomSubscriptionList) {
    removeDeletedItem(channelsContainer, roomSubscriptionList);
    removeDeletedItem(dmContainer, roomSubscriptionList);

    for (RoomSubscription roomSubscription : roomSubscriptionList) {
      String name = roomSubscription.getName();
      if (TextUtils.isEmpty(name)) {
        continue;
      }

      String type = roomSubscription.getType();

      if (RoomSubscription.TYPE_CHANNEL.equals(type)
          || RoomSubscription.TYPE_PRIVATE.equals(type)) {
        insertOrUpdateItem(channelsContainer, roomSubscription);
        removeItemIfExists(dmContainer, name);
      } else if (RoomSubscription.TYPE_DIRECT_MESSAGE.equals(type)) {
        removeItemIfExists(channelsContainer, name);
        insertOrUpdateItem(dmContainer, roomSubscription);
      }
    }
  }

  /**
   * set callback on List item clicked.
   */
  public void setOnItemClickListener(OnItemClickListener listener) {
    this.listener = listener;
  }

  private void removeDeletedItem(ViewGroup parent, List<RoomSubscription> roomSubscriptionList) {
    for (int index = parent.getChildCount() - 1; index >= 0; index--) {
      RoomListItemView roomListItemView = (RoomListItemView) parent.getChildAt(index);
      final String targetRoomName = roomListItemView.getRoomName();
      if (!TextUtils.isEmpty(targetRoomName)) {
        boolean found = false;
        for (RoomSubscription roomSubscription : roomSubscriptionList) {
          if (targetRoomName.equals(roomSubscription.getName())) {
            found = true;
            break;
          }
        }
        if (!found) {
          parent.removeViewAt(index);
        }
      }
    }
  }

  private void insertOrUpdateItem(ViewGroup parent, RoomSubscription roomSubscription) {
    final String roomName = roomSubscription.getName();

    int index;
    for (index = 0; index < parent.getChildCount(); index++) {
      RoomListItemView roomListItemView = (RoomListItemView) parent.getChildAt(index);
      final String targetRoomName = roomListItemView.getRoomName();
      if (roomName.equals(targetRoomName)) {
        updateRoomItemView(roomListItemView, roomSubscription);
        return;
      }
      if (roomName.compareToIgnoreCase(targetRoomName) < 0) {
        break;
      }
    }

    RoomListItemView roomListItemView = new RoomListItemView(parent.getContext());
    updateRoomItemView(roomListItemView, roomSubscription);
    if (index == parent.getChildCount()) {
      parent.addView(roomListItemView);
    } else {
      parent.addView(roomListItemView, index);
    }
  }

  private void updateRoomItemView(RoomListItemView roomListItemView,
                                  RoomSubscription roomSubscription) {
    roomListItemView
        .setRoomId(roomSubscription.getRoomId())
        .setRoomName(roomSubscription.getName())
        .setRoomType(roomSubscription.getType())
        .setAlert(roomSubscription.isAlert())
        .setUnreadCount(roomSubscription.getUnread());

    roomListItemView.setOnClickListener(this::onItemClick);
  }

  private void onItemClick(View view) {
    if (view instanceof RoomListItemView && listener != null) {
      listener.onItemClick((RoomListItemView) view);
    }
  }

  /**
   * Callback interface for List item clicked.
   */
  public interface OnItemClickListener {
    void onItemClick(RoomListItemView roomListItemView);
  }
}
