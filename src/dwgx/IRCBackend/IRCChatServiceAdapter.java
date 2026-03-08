package dwgx.IRCBackend;

import client.chat.model.*;
import client.chat.service.ChatService;
import client.chat.service.ChatServiceListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapts IRCConnection to the ChatService interface.
 * Bridges IRC protocol events into the chat UI's data model.
 * Thread-safe: IRC events arrive on Netty I/O thread, UI reads on main thread.
 */
public final class IRCChatServiceAdapter implements ChatService, IRCEventListener
{
    private final IRCConnection connection;
    private final IRCConfig config;
    private final List<ChatServiceListener> listeners = new CopyOnWriteArrayList<ChatServiceListener>();
    private final Map<String, ChatUser> users = Collections.synchronizedMap(new LinkedHashMap<String, ChatUser>());
    private final Map<String, ChatGroup> groups = Collections.synchronizedMap(new LinkedHashMap<String, ChatGroup>());
    private final List<ChatConversation> conversations = Collections.synchronizedList(new ArrayList<ChatConversation>());
    private final Map<String, List<ChatMessage>> messages = Collections.synchronizedMap(new LinkedHashMap<String, List<ChatMessage>>());
    private final AtomicInteger msgIdCounter = new AtomicInteger(0);
    private final List<ChatEmoji> customEmojis = Collections.synchronizedList(new ArrayList<ChatEmoji>());
    private ChatUser localUser;
    private boolean allowDm = true;
    private int maxHistoryPerConversation = 500;
    private String activeConversationId;

    /** Default IRC channel name (without #). */
    private static final String DEFAULT_CHANNEL = "general";

    /** Shared image folder for cross-client image display. */
    private static final String SHARED_IMAGE_DIR = "config/client/irc-images";

    public IRCChatServiceAdapter(IRCConfig config)
    {
        this.config = config;
        this.connection = new IRCConnection(config);
        this.connection.addListener(this);
        this.localUser = new ChatUser("local", config.getNickname(), null, UserStatus.ONLINE);
        this.users.put("local", this.localUser);

        // Bootstrap default #general group + conversation so the UI has something to show
        ChatGroup defaultGroup = new ChatGroup(DEFAULT_CHANNEL, "General", GroupType.OFFICIAL, "local");
        defaultGroup.addChannel(new ChatChannel(DEFAULT_CHANNEL, DEFAULT_CHANNEL, ChannelType.TEXT, DEFAULT_CHANNEL));
        defaultGroup.addMember("local");
        this.groups.put(DEFAULT_CHANNEL, defaultGroup);

        ChatConversation generalConv = ChatConversation.channel(
                "conv-" + DEFAULT_CHANNEL, DEFAULT_CHANNEL, DEFAULT_CHANNEL);
        generalConv.addParticipant("local");
        this.conversations.add(generalConv);
        this.messages.put(generalConv.getId(), Collections.synchronizedList(new ArrayList<ChatMessage>()));
    }

    public void setAllowDm(boolean allow) { this.allowDm = allow; }
    public void setMaxHistoryPerConversation(int max) { this.maxHistoryPerConversation = max; }
    public void setActiveConversationId(String id) { this.activeConversationId = id; }

    // --- ChatService implementation ---

    @Override public void connect() { this.connection.connect(); }
    @Override public void disconnect() { this.connection.disconnect(); }
    @Override public boolean isConnected() { return this.connection.isConnected(); }
    public boolean isConnecting() { return this.connection.isConnecting(); }
    @Override public ChatUser getLocalUser() { return this.localUser; }

    @Override public void updateNickname(String nickname)
    {
        this.localUser.setNickname(nickname);
        this.connection.changeNick(nickname);
    }

    @Override public void updateStatus(UserStatus status) { this.localUser.setStatus(status); }
    @Override public void updateAvatar(String imagePath) { this.localUser.setAvatarPath(imagePath); }

    @Override public List<ChatUser> getContacts()
    {
        List<ChatUser> result = new ArrayList<ChatUser>(this.users.values());
        result.remove(this.localUser);
        return result;
    }

    @Override public ChatUser getUserById(String userId) { return this.users.get(userId); }

    @Override public List<ChatConversation> getConversations()
    {
        return Collections.unmodifiableList(new ArrayList<ChatConversation>(this.conversations));
    }

    @Override public ChatConversation getConversation(String id)
    {
        synchronized (this.conversations)
        {
            for (ChatConversation c : this.conversations)
            {
                if (c.getId().equals(id)) return c;
            }
        }
        return null;
    }

    @Override public ChatConversation getOrCreateDm(String userId)
    {
        synchronized (this.conversations)
        {
            for (ChatConversation c : this.conversations)
            {
                if (c.isDm() && c.getParticipantIds().contains(userId)) return c;
            }
        }
        ChatConversation dm = ChatConversation.dm("dm-" + userId);
        dm.addParticipant("local");
        dm.addParticipant(userId);
        this.conversations.add(dm);
        return dm;
    }

    @Override public List<ChatMessage> getMessages(String conversationId, int offset, int limit)
    {
        List<ChatMessage> all = this.messages.get(conversationId);
        if (all == null) return Collections.emptyList();
        synchronized (all)
        {
            int start = Math.max(0, Math.min(offset, all.size()));
            int end = Math.min(all.size(), start + limit);
            return new ArrayList<ChatMessage>(all.subList(start, end));
        }
    }

    @Override public void sendTextMessage(String conversationId, String text)
    {
        ChatConversation conv = getConversation(conversationId);
        if (conv == null) return;

        // Determine IRC target
        String ircTarget;
        if (conv.isDm())
        {
            ircTarget = findOtherParticipant(conv);
        }
        else
        {
            ircTarget = "#" + conv.getChannelId();
        }

        if (ircTarget != null)
        {
            this.connection.sendMessage(ircTarget, text);
        }

        ChatMessage msg = ChatMessage.text(nextMsgId(), "local", conversationId, text,
                System.currentTimeMillis());
        addMessageToStore(conversationId, msg);
        for (ChatServiceListener l : this.listeners) l.onMessageReceived(msg);
    }

    @Override public void sendImageMessage(String conversationId, String imagePath)
    {
        if (imagePath == null || imagePath.isEmpty()) return;
        ChatConversation conv = getConversation(conversationId);
        if (conv == null) return;

        // Extract just the filename for IRC (don't send full local path — receiver doesn't have it)
        String fileName = imagePath.replace('\\', '/');
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) fileName = fileName.substring(lastSlash + 1);

        // Copy image to shared folder so other clients on same machine can display it
        String sharedPath = copyToSharedFolder(imagePath, fileName);

        // Send a short CTCP tag with just the filename
        String ircText = "\u0001IMAGE " + fileName + "\u0001";
        String ircTarget = resolveIrcTarget(conv);
        if (ircTarget != null) this.connection.sendMessage(ircTarget, ircText);

        // Local message keeps the shared path for rendering
        String displayPath = sharedPath != null ? sharedPath : imagePath;
        ChatAttachment att = new ChatAttachment("att-" + this.msgIdCounter.get(),
                fileName, "image/png", 0L, displayPath, null, 0, 0, 0);
        ChatMessage msg = ChatMessage.image(nextMsgId(), "local", conversationId, att,
                System.currentTimeMillis());
        addMessageToStore(conversationId, msg);
        for (ChatServiceListener l : this.listeners) l.onMessageReceived(msg);
    }

    @Override public void sendVoiceMessage(String conversationId, String audioPath, int durationMs)
    {
        if (audioPath == null || audioPath.isEmpty()) return;
        ChatConversation conv = getConversation(conversationId);
        if (conv == null) return;

        String ircText = "\u0001VOICE " + audioPath + " " + durationMs + "\u0001";
        String ircTarget = resolveIrcTarget(conv);
        if (ircTarget != null) this.connection.sendMessage(ircTarget, ircText);

        ChatAttachment att = new ChatAttachment("att-" + this.msgIdCounter.get(),
                audioPath.substring(audioPath.lastIndexOf('/') + 1),
                "audio/ogg", 0L, audioPath, null, durationMs, 0, 0);
        ChatMessage msg = new ChatMessage(nextMsgId(), "local", conversationId,
                MessageType.VOICE, null, att, System.currentTimeMillis(), false);
        addMessageToStore(conversationId, msg);
        for (ChatServiceListener l : this.listeners) l.onMessageReceived(msg);
    }

    @Override public void sendEncryptedMessage(String conversationId, String text) { sendTextMessage(conversationId, text); }

    @Override public List<ChatGroup> getGroups()
    {
        return Collections.unmodifiableList(new ArrayList<ChatGroup>(this.groups.values()));
    }

    @Override public ChatGroup createGroup(String name, GroupType type)
    {
        String id = name.toLowerCase().replace(' ', '-');
        ChatGroup g = new ChatGroup(id, name, type, "local");
        g.addChannel(new ChatChannel(id + "-general", "general", ChannelType.TEXT, id));
        g.addMember("local");
        this.groups.put(id, g);
        this.connection.joinChannel("#" + id);
        return g;
    }

    @Override public void joinGroup(String groupId) { this.connection.joinChannel("#" + groupId); }
    @Override public void leaveGroup(String groupId) { this.connection.partChannel("#" + groupId, null); }
    @Override public void inviteToGroup(String groupId, String userId) { this.connection.sendRaw("INVITE " + userId + " #" + groupId); }

    @Override public List<ChatUser> getGroupMembers(String groupId)
    {
        ChatGroup g = this.groups.get(groupId);
        if (g == null) return Collections.emptyList();
        List<ChatUser> result = new ArrayList<ChatUser>();
        for (String mid : g.getMemberIds())
        {
            ChatUser u = this.users.get(mid);
            if (u != null) result.add(u);
        }
        return result;
    }

    @Override public void startVoiceCall(String conversationId) { /* Not supported in IRC */ }
    @Override public void endVoiceCall() { /* Not supported in IRC */ }
    @Override public boolean isInVoiceCall() { return false; }
    @Override public List<ChatEmoji> getCustomEmojis()
    {
        return Collections.unmodifiableList(new ArrayList<ChatEmoji>(this.customEmojis));
    }

    @Override public void addCustomEmoji(String shortcode, String imagePath, boolean isSticker)
    {
        if (shortcode == null || shortcode.isEmpty()) return;
        String id = "emoji-" + shortcode.toLowerCase().replaceAll("[^a-z0-9_]", "");
        // Prevent duplicates
        synchronized (this.customEmojis)
        {
            for (ChatEmoji e : this.customEmojis)
            {
                if (e.getShortcode().equals(shortcode)) return;
            }
            this.customEmojis.add(new ChatEmoji(id, shortcode, imagePath, "local", isSticker));
        }
    }

    @Override public void removeCustomEmoji(String emojiId)
    {
        if (emojiId == null) return;
        synchronized (this.customEmojis)
        {
            java.util.Iterator<ChatEmoji> it = this.customEmojis.iterator();
            while (it.hasNext())
            {
                if (emojiId.equals(it.next().getId())) { it.remove(); break; }
            }
        }
    }
    @Override public void addListener(ChatServiceListener l) { this.listeners.add(l); }
    @Override public void removeListener(ChatServiceListener l) { this.listeners.remove(l); }

    // --- IRCEventListener implementation ---

    @Override
    public void onConnected()
    {
        System.out.println("[IRCAdapter] Connected to IRC server, joining " + this.groups.size() + " group(s)...");
        for (ChatServiceListener l : this.listeners) l.onConnectionStateChanged(true);
        for (ChatGroup g : this.groups.values())
        {
            System.out.println("[IRCAdapter] Joining #" + g.getId());
            this.connection.joinChannel("#" + g.getId());
        }
    }

    @Override
    public void onDisconnected(String reason)
    {
        System.out.println("[IRCAdapter] Disconnected: " + reason);
        for (ChatServiceListener l : this.listeners) l.onConnectionStateChanged(false);
    }

    @Override
    public void onMessage(String sender, String target, String text)
    {
        if (sender == null || text == null) return;
        System.out.println("[IRCAdapter] Message from " + sender + " to " + target + ": " + text);
        ChatUser senderUser = getOrCreateRemoteUser(sender);
        boolean isDm = !target.startsWith("#");
        String conversationId;

        if (isDm)
        {
            if (!this.allowDm) return;
            ChatConversation dm = getOrCreateDm(senderUser.getId());
            conversationId = dm.getId();
        }
        else
        {
            String channelName = target.substring(1);
            conversationId = findConversationForChannel(channelName);
            if (conversationId == null) return;
        }

        ChatMessage msg = parseIncomingMessage(senderUser.getId(), conversationId, text);
        addMessageToStore(conversationId, msg);
        for (ChatServiceListener l : this.listeners) l.onMessageReceived(msg);
    }

    /**
     * Parse incoming IRC text for CTCP-tagged image/voice, or plain text.
     */
    private ChatMessage parseIncomingMessage(String senderId, String conversationId, String text)
    {
        long now = System.currentTimeMillis();

        // CTCP IMAGE: \x01IMAGE path\x01
        if (text.startsWith("\u0001IMAGE ") && text.endsWith("\u0001"))
        {
            String path = text.substring(7, text.length() - 1).trim();
            String fileName = path;
            int slash = path.lastIndexOf('/');
            if (slash >= 0) fileName = path.substring(slash + 1);
            // Resolve to shared image folder
            String localPath = resolveSharedImage(fileName);
            ChatAttachment att = new ChatAttachment("att-" + this.msgIdCounter.get(),
                    fileName, "image/png", 0L, localPath, path, 0, 0, 0);
            return ChatMessage.image(nextMsgId(), senderId, conversationId, att, now);
        }

        // CTCP VOICE: \x01VOICE path durationMs\x01
        if (text.startsWith("\u0001VOICE ") && text.endsWith("\u0001"))
        {
            String payload = text.substring(7, text.length() - 1).trim();
            int lastSpace = payload.lastIndexOf(' ');
            String path = lastSpace > 0 ? payload.substring(0, lastSpace) : payload;
            int durationMs = 0;
            if (lastSpace > 0)
            {
                try { durationMs = Integer.parseInt(payload.substring(lastSpace + 1)); }
                catch (NumberFormatException ignored) {}
            }
            ChatAttachment att = new ChatAttachment("att-" + this.msgIdCounter.get(),
                    path.substring(path.lastIndexOf('/') + 1),
                    "audio/ogg", 0L, null, path, durationMs, 0, 0);
            return new ChatMessage(nextMsgId(), senderId, conversationId,
                    MessageType.VOICE, null, att, now, false);
        }

        // Plain text (also detect common image URL patterns)
        if (looksLikeImageUrl(text))
        {
            ChatAttachment att = new ChatAttachment("att-" + this.msgIdCounter.get(),
                    "image", "image/png", 0L, null, text.trim(), 0, 0, 0);
            return ChatMessage.image(nextMsgId(), senderId, conversationId, att, now);
        }

        return ChatMessage.text(nextMsgId(), senderId, conversationId, text, now);
    }

    private static boolean looksLikeImageUrl(String text)
    {
        if (text == null) return false;
        String lower = text.trim().toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp"));
    }

    private IRCServiceClient serviceClient;

    public IRCServiceClient getServiceClient()
    {
        if (this.serviceClient == null)
        {
            this.serviceClient = new IRCServiceClient(this.connection);
        }
        return this.serviceClient;
    }

    @Override
    public void onNotice(String sender, String target, String text)
    {
        // Route service replies to IRCServiceClient
        if (this.serviceClient != null && this.serviceClient.handleNotice(sender, text))
        {
            return;
        }
        // Server notices — no conversation action needed
    }

    @Override
    public void onJoin(String nick, String channel)
    {
        if (nick == null || channel == null) return;
        String channelName = channel.startsWith("#") ? channel.substring(1) : channel;
        ChatUser user = getOrCreateRemoteUser(nick);

        ChatGroup group = this.groups.get(channelName);
        if (group != null)
        {
            group.addMember(user.getId());
            for (ChatServiceListener l : this.listeners) l.onGroupUpdated(group);
        }

        if (nick.equals(this.connection.getCurrentNick()))
        {
            ensureGroupAndConversation(channelName);
        }
        else
        {
            String convId = findConversationForChannel(channelName);
            if (convId != null)
            {
                postSystemMessage(convId, nick + " joined the channel");
            }
        }
    }

    @Override
    public void onPart(String nick, String channel, String reason)
    {
        if (nick == null || channel == null) return;
        String channelName = channel.startsWith("#") ? channel.substring(1) : channel;
        ChatGroup group = this.groups.get(channelName);
        if (group != null)
        {
            group.removeMember(nickToUserId(nick));
            for (ChatServiceListener l : this.listeners) l.onGroupUpdated(group);
        }

        String convId = findConversationForChannel(channelName);
        if (convId != null)
        {
            String text = reason != null && !reason.isEmpty()
                    ? nick + " left (" + reason + ")"
                    : nick + " left the channel";
            postSystemMessage(convId, text);
        }
    }

    @Override
    public void onQuit(String nick, String reason)
    {
        if (nick == null) return;
        String userId = nickToUserId(nick);
        ChatUser user = this.users.get(userId);
        if (user != null)
        {
            user.setStatus(UserStatus.OFFLINE);
            user.setLastSeenMs(System.currentTimeMillis());
            for (ChatServiceListener l : this.listeners) l.onUserStatusChanged(userId, UserStatus.OFFLINE);
        }

        // Post quit system message to all channels this user was in
        for (ChatGroup g : this.groups.values())
        {
            if (g.getMemberIds().contains(userId))
            {
                g.removeMember(userId);
                String convId = findConversationForChannel(g.getId());
                if (convId != null)
                {
                    String text = reason != null && !reason.isEmpty()
                            ? nick + " quit (" + reason + ")"
                            : nick + " quit";
                    postSystemMessage(convId, text);
                }
            }
        }
    }

    @Override
    public void onNickChange(String oldNick, String newNick)
    {
        if (oldNick == null || newNick == null) return;
        String userId = nickToUserId(oldNick);
        ChatUser user = this.users.get(userId);
        if (user != null) user.setNickname(newNick);
        if (oldNick.equals(this.localUser.getNickname()))
        {
            this.localUser.setNickname(newNick);
        }

        // Post nick change system message to all shared channels
        for (ChatGroup g : this.groups.values())
        {
            if (g.getMemberIds().contains(userId))
            {
                String convId = findConversationForChannel(g.getId());
                if (convId != null)
                {
                    postSystemMessage(convId, oldNick + " is now known as " + newNick);
                }
            }
        }
    }

    @Override
    public void onTopic(String channel, String topic, String setter) { /* ignored */ }

    @Override
    public void onNumeric(int code, String[] params, String trailing)
    {
        if (code == 353 && params != null && params.length >= 3)
        {
            String ch = params[2].startsWith("#") ? params[2].substring(1) : params[2];
            ChatGroup group = this.groups.get(ch);
            if (group != null && trailing != null)
            {
                for (String raw : trailing.split(" "))
                {
                    String nick = raw.replaceAll("^[@+%&~!]", "");
                    if (!nick.isEmpty())
                    {
                        ChatUser u = getOrCreateRemoteUser(nick);
                        group.addMember(u.getId());
                    }
                }
            }
        }
    }

    @Override public void onRaw(IRCMessage message) { /* unhandled */ }

    @Override public void onError(String message)
    {
        for (ChatServiceListener l : this.listeners) l.onError(message);
    }

    // --- Helper methods ---

    private String nextMsgId()
    {
        return "msg-" + this.msgIdCounter.incrementAndGet();
    }

    private void postSystemMessage(String conversationId, String text)
    {
        ChatMessage sys = new ChatMessage(nextMsgId(), "system", conversationId,
                MessageType.SYSTEM, text, null, System.currentTimeMillis(), false);
        addMessageToStore(conversationId, sys);
        for (ChatServiceListener l : this.listeners) l.onMessageReceived(sys);
    }

    private void addMessageToStore(String conversationId, ChatMessage msg)
    {
        List<ChatMessage> list = this.messages.get(conversationId);
        if (list == null)
        {
            list = Collections.synchronizedList(new ArrayList<ChatMessage>());
            this.messages.put(conversationId, list);
        }
        list.add(msg);

        // Enforce history cap
        synchronized (list)
        {
            while (list.size() > this.maxHistoryPerConversation)
            {
                list.remove(0);
            }
        }

        ChatConversation conv = getConversation(conversationId);
        if (conv != null)
        {
            conv.setLastMessagePreview(msg.getDisplayText());
            conv.setLastActivityMs(msg.getTimestampMs());
            // Track unread for non-active conversations
            if (!conversationId.equals(this.activeConversationId)
                    && !"local".equals(msg.getSenderId()))
            {
                conv.incrementUnread();
            }
            for (ChatServiceListener l : this.listeners) l.onConversationUpdated(conv);
        }
    }

    private String resolveIrcTarget(ChatConversation conv)
    {
        if (conv.isDm()) return findOtherParticipant(conv);
        return "#" + conv.getChannelId();
    }

    private String findOtherParticipant(ChatConversation conv)
    {
        for (String pid : conv.getParticipantIds())
        {
            if (!"local".equals(pid))
            {
                ChatUser u = this.users.get(pid);
                return u != null ? u.getNickname() : pid;
            }
        }
        return null;
    }

    private ChatUser getOrCreateRemoteUser(String nick)
    {
        String userId = nickToUserId(nick);
        ChatUser user = this.users.get(userId);
        if (user == null)
        {
            user = new ChatUser(userId, nick, null, UserStatus.ONLINE);
            this.users.put(userId, user);
        }
        else
        {
            user.setStatus(UserStatus.ONLINE);
            user.setLastSeenMs(System.currentTimeMillis());
        }
        return user;
    }

    private String nickToUserId(String nick)
    {
        return nick.toLowerCase().replaceAll("[^a-z0-9_\\-]", "");
    }

    private String findConversationForChannel(String channelName)
    {
        synchronized (this.conversations)
        {
            for (ChatConversation c : this.conversations)
            {
                if (!c.isDm() && channelName.equals(c.getChannelId())) return c.getId();
            }
        }
        ChatGroup group = this.groups.get(channelName);
        if (group != null)
        {
            for (ChatChannel ch : group.getChannels())
            {
                String convId = "conv-" + ch.getId();
                if (getConversation(convId) != null) return convId;
            }
        }
        return null;
    }

    private void ensureGroupAndConversation(String channelName)
    {
        ChatGroup group = this.groups.get(channelName);
        if (group == null)
        {
            group = new ChatGroup(channelName, channelName, GroupType.CUSTOM, "local");
            group.addChannel(new ChatChannel(channelName + "-general", "general", ChannelType.TEXT, channelName));
            group.addMember("local");
            this.groups.put(channelName, group);
        }
        for (ChatChannel ch : group.getChannels())
        {
            if (ch.isText())
            {
                String convId = "conv-" + ch.getId();
                if (getConversation(convId) == null)
                {
                    ChatConversation conv = ChatConversation.channel(convId, group.getId(), ch.getId());
                    conv.addParticipant("local");
                    this.conversations.add(conv);
                }
                break;
            }
        }
    }

    /**
     * Copy an image file to the shared image folder so other clients can display it.
     * Returns the path in the shared folder, or null on failure.
     */
    private static String copyToSharedFolder(String sourcePath, String fileName)
    {
        try
        {
            File dir = new File(SHARED_IMAGE_DIR);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, fileName);
            if (dest.exists()) return dest.getAbsolutePath(); // already there
            File src = new File(sourcePath);
            if (!src.isFile()) return null;
            FileInputStream in = new FileInputStream(src);
            try
            {
                FileOutputStream out = new FileOutputStream(dest);
                try
                {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                finally { out.close(); }
            }
            finally { in.close(); }
            return dest.getAbsolutePath();
        }
        catch (Exception e)
        {
            System.err.println("[IRC] Failed to copy image to shared folder: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolve a filename to the shared image folder path.
     * Returns the absolute path if the file exists, null otherwise.
     */
    private static String resolveSharedImage(String fileName)
    {
        if (fileName == null || fileName.isEmpty()) return null;
        File f = new File(SHARED_IMAGE_DIR, fileName);
        return f.isFile() ? f.getAbsolutePath() : null;
    }
}
