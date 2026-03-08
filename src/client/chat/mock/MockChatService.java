package client.chat.mock;

import client.chat.model.*;
import client.chat.service.ChatService;
import client.chat.service.ChatServiceListener;
import java.util.*;

/**
 * Mock implementation of ChatService with hardcoded test data.
 * Swap for real networking implementation later.
 */
public final class MockChatService implements ChatService {
    private final List<ChatServiceListener> listeners = new ArrayList<ChatServiceListener>();
    private final Map<String, ChatUser> users = new LinkedHashMap<String, ChatUser>();
    private final List<ChatGroup> groups = new ArrayList<ChatGroup>();
    private final List<ChatConversation> conversations = new ArrayList<ChatConversation>();
    private final Map<String, List<ChatMessage>> messages = new LinkedHashMap<String, List<ChatMessage>>();
    private final List<ChatEmoji> emojis = new ArrayList<ChatEmoji>();
    private ChatUser localUser;
    private boolean connected;
    private boolean inVoiceCall;
    private int msgCounter;

    public MockChatService() {
        initMockData();
    }

    private void initMockData() {
        long now = System.currentTimeMillis();

        localUser = new ChatUser("local", "Player", null, UserStatus.ONLINE);
        users.put("local", localUser);

        ChatUser alice = new ChatUser("alice", "Alice", null, UserStatus.ONLINE);
        ChatUser bob = new ChatUser("bob", "Bob", null, UserStatus.AWAY);
        ChatUser charlie = new ChatUser("charlie", "Charlie", null, UserStatus.OFFLINE);
        users.put("alice", alice);
        users.put("bob", bob);
        users.put("charlie", charlie);

        // DM conversations
        ChatConversation dmAlice = ChatConversation.dm("dm-alice");
        dmAlice.addParticipant("local");
        dmAlice.addParticipant("alice");
        dmAlice.setLastMessagePreview("Hey, wanna play?");
        dmAlice.setLastActivityMs(now - 60000L);
        dmAlice.setUnreadCount(2);
        conversations.add(dmAlice);

        ChatConversation dmBob = ChatConversation.dm("dm-bob");
        dmBob.addParticipant("local");
        dmBob.addParticipant("bob");
        dmBob.setLastMessagePreview("Nice build!");
        dmBob.setLastActivityMs(now - 300000L);
        conversations.add(dmBob);

        // Official group
        ChatGroup official = new ChatGroup("official", "Official Server", GroupType.OFFICIAL, "server");
        official.addChannel(new ChatChannel("off-general", "general", ChannelType.TEXT, "official"));
        official.addChannel(new ChatChannel("off-announce", "announcements", ChannelType.TEXT, "official"));
        official.addChannel(new ChatChannel("off-voice", "voice", ChannelType.VOICE, "official"));
        official.addMember("local");
        official.addMember("alice");
        official.addMember("bob");
        official.addMember("charlie");
        groups.add(official);

        // Custom group
        ChatGroup custom = new ChatGroup("team", "Team Alpha", GroupType.CUSTOM, "local");
        custom.addChannel(new ChatChannel("team-general", "general", ChannelType.TEXT, "team"));
        custom.addChannel(new ChatChannel("team-strats", "strategies", ChannelType.TEXT, "team"));
        custom.addChannel(new ChatChannel("team-vc", "voice-chat", ChannelType.VOICE, "team"));
        custom.addMember("local");
        custom.addMember("alice");
        groups.add(custom);

        // Channel conversations
        for (ChatGroup g : groups) {
            for (ChatChannel ch : g.getChannels()) {
                if (ch.isText()) {
                    ChatConversation conv = ChatConversation.channel("conv-" + ch.getId(), g.getId(), ch.getId());
                    conversations.add(conv);
                }
            }
        }

        // Mock messages for DM with Alice
        List<ChatMessage> aliceMsgs = new ArrayList<ChatMessage>();
        aliceMsgs.add(ChatMessage.text("m1", "alice", "dm-alice", "Hey! Are you online?", now - 120000L));
        aliceMsgs.add(ChatMessage.text("m2", "local", "dm-alice", "Yeah, just logged in", now - 110000L));
        aliceMsgs.add(ChatMessage.text("m3", "alice", "dm-alice", "Wanna go mining? I found diamonds!", now - 100000L));
        aliceMsgs.add(ChatMessage.text("m4", "local", "dm-alice", "Sure! Where are you?", now - 90000L));
        aliceMsgs.add(ChatMessage.text("m5", "alice", "dm-alice", "At coords 234, 12, -567. Bring torches!", now - 80000L));
        aliceMsgs.add(ChatMessage.text("m6", "local", "dm-alice", "On my way", now - 70000L));
        aliceMsgs.add(ChatMessage.text("m7", "alice", "dm-alice", "Hey, wanna play?", now - 60000L));
        // Image message
        ChatAttachment imgAtt = new ChatAttachment("att-img1", "screenshot.png", "image/png", 0L, null, null, 0, 320, 240);
        aliceMsgs.add(ChatMessage.image("m8", "alice", "dm-alice", imgAtt, now - 50000L));
        // Voice message
        ChatAttachment voiceAtt = new ChatAttachment("att-v1", "voice.ogg", "audio/ogg", 0L, null, null, 5000, 0, 0);
        aliceMsgs.add(new ChatMessage("m9", "local", "dm-alice", MessageType.VOICE, null, voiceAtt, now - 40000L, false));
        // Long text for word-wrap testing
        aliceMsgs.add(ChatMessage.text("m10", "alice", "dm-alice",
                "This is a really long message to test word wrapping in the chat bubbles. It should break into multiple lines instead of overflowing the bubble width. Let me know if it works!",
                now - 30000L));
        messages.put("dm-alice", aliceMsgs);

        // Mock messages for DM with Bob
        List<ChatMessage> bobMsgs = new ArrayList<ChatMessage>();
        bobMsgs.add(ChatMessage.text("b1", "bob", "dm-bob", "Check out my new house", now - 400000L));
        bobMsgs.add(ChatMessage.text("b2", "local", "dm-bob", "Looks amazing!", now - 350000L));
        bobMsgs.add(ChatMessage.text("b3", "bob", "dm-bob", "Nice build!", now - 300000L));
        messages.put("dm-bob", bobMsgs);

        // Mock messages for official general
        List<ChatMessage> offMsgs = new ArrayList<ChatMessage>();
        offMsgs.add(new ChatMessage("s1", "server", "conv-off-general", MessageType.SYSTEM,
                "Welcome to the official server chat!", null, now - 600000L, false));
        offMsgs.add(ChatMessage.text("o1", "charlie", "conv-off-general", "Anyone want to trade?", now - 500000L));
        offMsgs.add(ChatMessage.text("o2", "alice", "conv-off-general", "What do you have?", now - 490000L));
        offMsgs.add(ChatMessage.text("o3", "charlie", "conv-off-general", "64 iron ingots for 32 gold", now - 480000L));
        messages.put("conv-off-general", offMsgs);

        msgCounter = 100;
        connected = true;
    }

    @Override public void connect() { connected = true; notifyConnectionChanged(true); }
    @Override public void disconnect() { connected = false; notifyConnectionChanged(false); }
    @Override public boolean isConnected() { return connected; }
    @Override public ChatUser getLocalUser() { return localUser; }
    @Override public void updateNickname(String nickname) { localUser.setNickname(nickname); }
    @Override public void updateStatus(UserStatus status) { localUser.setStatus(status); }
    @Override public void updateAvatar(String imagePath) { localUser.setAvatarPath(imagePath); }

    @Override public List<ChatUser> getContacts() {
        List<ChatUser> result = new ArrayList<ChatUser>(users.values());
        result.remove(localUser);
        return result;
    }

    @Override public ChatUser getUserById(String userId) { return users.get(userId); }
    @Override public List<ChatConversation> getConversations() { return Collections.unmodifiableList(conversations); }

    @Override public ChatConversation getConversation(String id) {
        for (ChatConversation c : conversations) { if (c.getId().equals(id)) return c; }
        return null;
    }

    @Override public ChatConversation getOrCreateDm(String userId) {
        for (ChatConversation c : conversations) {
            if (c.isDm() && c.getParticipantIds().contains(userId)) return c;
        }
        ChatConversation dm = ChatConversation.dm("dm-" + userId);
        dm.addParticipant("local");
        dm.addParticipant(userId);
        conversations.add(dm);
        return dm;
    }

    @Override public List<ChatMessage> getMessages(String conversationId, int offset, int limit) {
        List<ChatMessage> all = messages.get(conversationId);
        if (all == null) return Collections.emptyList();
        int start = Math.max(0, Math.min(offset, all.size()));
        int end = Math.min(all.size(), start + limit);
        return new ArrayList<ChatMessage>(all.subList(start, end));
    }

    @Override public void sendTextMessage(String conversationId, String text) {
        String id = "m" + (++msgCounter);
        long ts = System.currentTimeMillis();
        ChatMessage msg = ChatMessage.text(id, "local", conversationId, text, ts);
        getOrCreateMsgList(conversationId).add(msg);
        notifyMessageReceived(msg);
    }

    @Override public void sendImageMessage(String conversationId, String imagePath) {
        String id = "m" + (++msgCounter);
        ChatAttachment att = new ChatAttachment(id + "-att", "image.png", "image/png", 0L, imagePath, null, 0, 200, 150);
        ChatMessage msg = ChatMessage.image(id, "local", conversationId, att, System.currentTimeMillis());
        getOrCreateMsgList(conversationId).add(msg);
        notifyMessageReceived(msg);
    }

    @Override public void sendVoiceMessage(String conversationId, String audioPath, int durationMs) {
        String id = "m" + (++msgCounter);
        ChatAttachment att = new ChatAttachment(id + "-att", "voice.ogg", "audio/ogg", 0L, audioPath, null, durationMs, 0, 0);
        ChatMessage msg = new ChatMessage(id, "local", conversationId, MessageType.VOICE, null, att, System.currentTimeMillis(), false);
        getOrCreateMsgList(conversationId).add(msg);
        notifyMessageReceived(msg);
    }

    @Override public void sendEncryptedMessage(String conversationId, String text) { sendTextMessage(conversationId, text); }
    @Override public List<ChatGroup> getGroups() { return Collections.unmodifiableList(groups); }

    @Override public ChatGroup createGroup(String name, GroupType type) {
        ChatGroup g = new ChatGroup("grp-" + name.toLowerCase().replace(' ', '-'), name, type, "local");
        g.addChannel(new ChatChannel(g.getId() + "-general", "general", ChannelType.TEXT, g.getId()));
        g.addMember("local");
        groups.add(g);
        return g;
    }

    @Override public void joinGroup(String groupId) {}
    @Override public void leaveGroup(String groupId) {}
    @Override public void inviteToGroup(String groupId, String userId) {}

    @Override public List<ChatUser> getGroupMembers(String groupId) {
        for (ChatGroup g : groups) {
            if (g.getId().equals(groupId)) {
                List<ChatUser> result = new ArrayList<ChatUser>();
                for (String mid : g.getMemberIds()) {
                    ChatUser u = users.get(mid);
                    if (u != null) result.add(u);
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override public void startVoiceCall(String conversationId) { inVoiceCall = true; }
    @Override public void endVoiceCall() { inVoiceCall = false; }
    @Override public boolean isInVoiceCall() { return inVoiceCall; }
    @Override public List<ChatEmoji> getCustomEmojis() { return Collections.unmodifiableList(emojis); }

    @Override public void addCustomEmoji(String shortcode, String imagePath, boolean isSticker) {
        emojis.add(new ChatEmoji("e" + emojis.size(), shortcode, imagePath, "local", isSticker));
    }

    @Override public void removeCustomEmoji(String emojiId) {
        Iterator<ChatEmoji> it = emojis.iterator();
        while (it.hasNext()) { if (it.next().getId().equals(emojiId)) { it.remove(); break; } }
    }

    @Override public void addListener(ChatServiceListener l) { listeners.add(l); }
    @Override public void removeListener(ChatServiceListener l) { listeners.remove(l); }

    private List<ChatMessage> getOrCreateMsgList(String convId) {
        List<ChatMessage> list = messages.get(convId);
        if (list == null) { list = new ArrayList<ChatMessage>(); messages.put(convId, list); }
        return list;
    }

    private void notifyMessageReceived(ChatMessage msg) {
        for (ChatServiceListener l : listeners) l.onMessageReceived(msg);
    }

    private void notifyConnectionChanged(boolean state) {
        for (ChatServiceListener l : listeners) l.onConnectionStateChanged(state);
    }
}
