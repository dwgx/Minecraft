package client.chat.store;

import client.chat.model.*;
import java.util.*;

/**
 * In-memory state holder for the chat system.
 * All UI reads from here; service writes to here.
 * Single-threaded access (main game thread only).
 */
public final class ChatStore {
    private ChatUser localUser;
    private final List<ChatConversation> conversations = new ArrayList<ChatConversation>();
    private final List<ChatGroup> groups = new ArrayList<ChatGroup>();
    private final Map<String, List<ChatMessage>> messagesByConversation = new LinkedHashMap<String, List<ChatMessage>>();
    private final Map<String, ChatUser> usersById = new LinkedHashMap<String, ChatUser>();
    private final List<ChatEmoji> customEmojis = new ArrayList<ChatEmoji>();

    // Social data
    private final List<FriendEntry> friends = new ArrayList<FriendEntry>();
    private final List<MailMessage> mails = new ArrayList<MailMessage>();
    private final List<SocialPost> feed = new ArrayList<SocialPost>();
    private volatile UserProfile myProfile;
    private volatile UserProfile viewingProfile;
    private int unreadMailCount;

    private String activeConversationId;
    private String activeGroupId;
    private volatile boolean connected;
    private volatile String connectionStatus = "Disconnected";
    private long lastConnectionChangeMs;

    public ChatUser getLocalUser() { return localUser; }
    public void setLocalUser(ChatUser user) {
        this.localUser = user;
        if (user != null) usersById.put(user.getId(), user);
    }

    public List<ChatConversation> getConversations() {
        return Collections.unmodifiableList(conversations);
    }

    public void setConversations(List<ChatConversation> list) {
        conversations.clear();
        conversations.addAll(list);
    }

    public ChatConversation getConversation(String id) {
        for (ChatConversation c : conversations) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    public void addConversation(ChatConversation conv) {
        conversations.add(conv);
    }

    public List<ChatGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void setGroups(List<ChatGroup> list) {
        groups.clear();
        groups.addAll(list);
    }

    public ChatGroup getGroup(String id) {
        for (ChatGroup g : groups) {
            if (g.getId().equals(id)) return g;
        }
        return null;
    }

    public List<ChatMessage> getMessages(String conversationId) {
        List<ChatMessage> msgs = messagesByConversation.get(conversationId);
        if (msgs == null) return Collections.emptyList();
        return Collections.unmodifiableList(msgs);
    }

    public void setMessages(String conversationId, List<ChatMessage> msgs) {
        messagesByConversation.put(conversationId, new ArrayList<ChatMessage>(msgs));
    }

    public void addMessage(String conversationId, ChatMessage msg) {
        List<ChatMessage> msgs = messagesByConversation.get(conversationId);
        if (msgs == null) {
            msgs = new ArrayList<ChatMessage>();
            messagesByConversation.put(conversationId, msgs);
        }
        msgs.add(msg);
    }

    public ChatUser getUser(String id) { return usersById.get(id); }

    public void putUser(ChatUser user) { usersById.put(user.getId(), user); }

    public Collection<ChatUser> getAllUsers() {
        return Collections.unmodifiableCollection(usersById.values());
    }

    public List<ChatEmoji> getCustomEmojis() {
        return Collections.unmodifiableList(customEmojis);
    }

    public void setCustomEmojis(List<ChatEmoji> emojis) {
        customEmojis.clear();
        customEmojis.addAll(emojis);
    }

    public String getActiveConversationId() { return activeConversationId; }
    public void setActiveConversationId(String id) { this.activeConversationId = id; }
    public String getActiveGroupId() { return activeGroupId; }
    public void setActiveGroupId(String id) { this.activeGroupId = id; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) {
        this.connected = connected;
        this.connectionStatus = connected ? "Connected" : "Disconnected";
        this.lastConnectionChangeMs = System.currentTimeMillis();
    }
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String status) { this.connectionStatus = status; }
    public long getLastConnectionChangeMs() { return lastConnectionChangeMs; }

    // --- Social data accessors ---

    public List<FriendEntry> getFriends() { return Collections.unmodifiableList(friends); }
    public void setFriends(List<FriendEntry> list) { friends.clear(); friends.addAll(list); }
    public void addFriend(FriendEntry entry) { friends.add(entry); }
    public void removeFriend(String nick) {
        Iterator<FriendEntry> it = friends.iterator();
        while (it.hasNext()) { if (it.next().getNick().equals(nick)) { it.remove(); break; } }
    }

    public List<MailMessage> getMails() { return Collections.unmodifiableList(mails); }
    public void setMails(List<MailMessage> list) { mails.clear(); mails.addAll(list); }
    public void addMail(MailMessage mail) { mails.add(0, mail); }

    public List<SocialPost> getFeed() { return Collections.unmodifiableList(feed); }
    public void setFeed(List<SocialPost> list) { feed.clear(); feed.addAll(list); }
    public void addPost(SocialPost post) { feed.add(0, post); }

    public UserProfile getMyProfile() { return myProfile; }
    public void setMyProfile(UserProfile profile) { this.myProfile = profile; }

    public UserProfile getViewingProfile() { return viewingProfile; }
    public void setViewingProfile(UserProfile profile) { this.viewingProfile = profile; }

    public int getUnreadMailCount() { return unreadMailCount; }
    public void setUnreadMailCount(int count) { this.unreadMailCount = count; }
}
