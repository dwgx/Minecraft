package client.chat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatConversation {
    private final String id;
    private final boolean dm;
    private final String groupId;
    private final String channelId;
    private final List<String> participantIds;
    private String lastMessagePreview;
    private long lastActivityMs;
    private int unreadCount;

    public ChatConversation(String id, boolean dm, String groupId, String channelId) {
        this.id = id;
        this.dm = dm;
        this.groupId = groupId;
        this.channelId = channelId;
        this.participantIds = new ArrayList<String>();
    }

    public String getId() { return id; }
    public boolean isDm() { return dm; }
    public String getGroupId() { return groupId; }
    public String getChannelId() { return channelId; }
    public List<String> getParticipantIds() { return Collections.unmodifiableList(participantIds); }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String preview) { this.lastMessagePreview = preview; }
    public long getLastActivityMs() { return lastActivityMs; }
    public void setLastActivityMs(long ms) { this.lastActivityMs = ms; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int count) { this.unreadCount = count; }
    public void incrementUnread() { this.unreadCount++; }
    public void clearUnread() { this.unreadCount = 0; }

    public void addParticipant(String userId) {
        if (!participantIds.contains(userId)) participantIds.add(userId);
    }

    public static ChatConversation dm(String id) {
        return new ChatConversation(id, true, null, null);
    }

    public static ChatConversation channel(String id, String groupId, String channelId) {
        return new ChatConversation(id, false, groupId, channelId);
    }
}
