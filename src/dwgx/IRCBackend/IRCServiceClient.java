package dwgx.IRCBackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side service command sender and reply parser.
 * Sends CTCP-style commands to *IRCService and parses replies.
 *
 * Send: PRIVMSG *IRCService :\x01CMD json\x01
 * Recv: NOTICE nick :\x01CMD_REPLY json\x01
 */
public final class IRCServiceClient
{
    private static final String SERVICE_NICK = "*IRCService";

    private final IRCConnection connection;
    private final List<IRCServiceListener> listeners = new CopyOnWriteArrayList<IRCServiceListener>();

    public IRCServiceClient(IRCConnection connection)
    {
        this.connection = connection;
    }

    public void addListener(IRCServiceListener listener) { this.listeners.add(listener); }
    public void removeListener(IRCServiceListener listener) { this.listeners.remove(listener); }

    // --- Send commands ---

    public void sendFriendRequest(String nick)
    {
        sendCommand("FRIEND_REQUEST", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void acceptFriendRequest(String nick)
    {
        sendCommand("FRIEND_ACCEPT", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void rejectFriendRequest(String nick)
    {
        sendCommand("FRIEND_REJECT", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void removeFriend(String nick)
    {
        sendCommand("FRIEND_REMOVE", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void listFriends()
    {
        sendCommand("FRIEND_LIST", "{}");
    }

    public void listPendingRequests()
    {
        sendCommand("FRIEND_PENDING", "{}");
    }

    /** Add friend by UID. */
    public void addFriendByUid(String uid)
    {
        sendCommand("FRIEND_ADD_BY_UID", "{\"uid\":\"" + escapeJson(uid) + "\"}");
    }

    /** Request own UID from server. */
    public void requestMyUid()
    {
        sendCommand("GET_MY_UID", "{}");
    }

    public void queryProfile(String nick)
    {
        sendCommand("PROFILE_QUERY", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void updateProfile(String jsonData)
    {
        sendCommand("PROFILE_UPDATE", "{\"data\":" + jsonData + "}");
    }

    public void visitProfile(String nick)
    {
        sendCommand("PROFILE_VISIT", "{\"nick\":\"" + escapeJson(nick) + "\"}");
    }

    public void sendMail(String toNick, String subject, String body)
    {
        sendCommand("MAIL_SEND", "{\"to\":\"" + escapeJson(toNick)
                + "\",\"subject\":\"" + escapeJson(subject)
                + "\",\"body\":\"" + escapeJson(body) + "\"}");
    }

    public void listMail(boolean inbox, int limit, int offset)
    {
        sendCommand("MAIL_LIST", "{\"box\":\"" + (inbox ? "inbox" : "sent")
                + "\",\"limit\":" + limit + ",\"offset\":" + offset + "}");
    }

    public void markMailRead(long mailId)
    {
        sendCommand("MAIL_READ", "{\"id\":" + mailId + "}");
    }

    public void deleteMail(long mailId)
    {
        sendCommand("MAIL_DELETE", "{\"id\":" + mailId + "}");
    }

    public void getUnreadMailCount()
    {
        sendCommand("MAIL_UNREAD_COUNT", "{}");
    }

    public void createPost(String content, String imageUrl)
    {
        String json = "{\"content\":\"" + escapeJson(content) + "\"";
        if (imageUrl != null && !imageUrl.isEmpty())
        {
            json += ",\"imageUrl\":\"" + escapeJson(imageUrl) + "\"";
        }
        json += "}";
        sendCommand("SOCIAL_POST", json);
    }

    public void getFeed(int limit, int offset)
    {
        sendCommand("SOCIAL_FEED", "{\"limit\":" + limit + ",\"offset\":" + offset + "}");
    }

    public void toggleLike(long postId)
    {
        sendCommand("SOCIAL_LIKE", "{\"postId\":" + postId + "}");
    }

    public void addComment(long postId, String content)
    {
        sendCommand("SOCIAL_COMMENT", "{\"postId\":" + postId
                + ",\"content\":\"" + escapeJson(content) + "\"}");
    }

    public void getComments(long postId, int limit)
    {
        sendCommand("SOCIAL_COMMENTS", "{\"postId\":" + postId + ",\"limit\":" + limit + "}");
    }

    /** Request channel message history from server. */
    public void requestChannelHistory(String channel, int limit, long beforeId)
    {
        sendCommand("MSG_HISTORY_CHANNEL", "{\"channel\":\"" + escapeJson(channel)
                + "\",\"limit\":" + limit + ",\"beforeId\":" + beforeId + "}");
    }

    /** Request private message history from server. */
    public void requestPrivateHistory(String nick, int limit, long beforeId)
    {
        sendCommand("MSG_HISTORY_PRIVATE", "{\"nick\":\"" + escapeJson(nick)
                + "\",\"limit\":" + limit + ",\"beforeId\":" + beforeId + "}");
    }

    // --- Reply handling (called from adapter's onNotice) ---

    /**
     * Try to handle a NOTICE as a service reply.
     * @return true if it was a service reply and was handled
     */
    public boolean handleNotice(String sender, String text)
    {
        if (!SERVICE_NICK.equals(sender)) return false;
        if (text == null || text.isEmpty()) return false;

        // Strip CTCP delimiters
        String content = text;
        if (content.charAt(0) == '\u0001' && content.charAt(content.length() - 1) == '\u0001')
        {
            content = content.substring(1, content.length() - 1);
        }

        // Parse: CMD_REPLY json or NOTIFICATION json
        int spaceIdx = content.indexOf(' ');
        if (spaceIdx < 0) return false;

        String replyCmd = content.substring(0, spaceIdx);
        String jsonPayload = content.substring(spaceIdx + 1);

        // Handle push notifications
        if ("FRIEND_REQUEST_NOTIFY".equals(replyCmd))
        {
            try
            {
                JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
                String fromNick = obj.get("fromNick").getAsString();
                for (IRCServiceListener l : this.listeners) l.onFriendRequestNotify(fromNick);
            }
            catch (Exception ignored) {}
            return true;
        }
        if ("FRIEND_ACCEPT_NOTIFY".equals(replyCmd))
        {
            try
            {
                JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
                String nick = obj.get("nick").getAsString();
                for (IRCServiceListener l : this.listeners) l.onFriendAcceptNotify(nick);
            }
            catch (Exception ignored) {}
            return true;
        }
        if ("MAIL_NOTIFY".equals(replyCmd))
        {
            try
            {
                JsonObject obj = new JsonParser().parse(jsonPayload).getAsJsonObject();
                String fromNick = obj.get("fromNick").getAsString();
                String subject = obj.get("subject").getAsString();
                long mailId = obj.get("mailId").getAsLong();
                for (IRCServiceListener l : this.listeners) l.onMailNotify(fromNick, subject, mailId);
            }
            catch (Exception ignored) {}
            return true;
        }

        // Handle command replies (CMD_REPLY)
        if (replyCmd.endsWith("_REPLY"))
        {
            String originalCmd = replyCmd.substring(0, replyCmd.length() - 6);
            for (IRCServiceListener l : this.listeners) l.onServiceReply(originalCmd, jsonPayload);
            return true;
        }

        return false;
    }

    // --- Internal ---

    private void sendCommand(String cmd, String json)
    {
        if (!this.connection.isConnected()) return;
        this.connection.sendRaw("PRIVMSG " + SERVICE_NICK + " :\u0001" + cmd + " " + json + "\u0001");
    }

    private static String escapeJson(String s)
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}