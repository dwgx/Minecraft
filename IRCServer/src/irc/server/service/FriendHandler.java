package irc.server.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import irc.server.IRCServer;
import irc.server.IRCServerHandler;
import irc.server.db.FriendDao;
import irc.server.db.UserDao;

import java.sql.SQLException;
import java.util.List;

/**
 * Handles friend-related service commands.
 */
public final class FriendHandler
{
    private final IRCServer server;
    private final FriendDao friendDao;
    private final UserDao userDao;

    public FriendHandler(IRCServer server, FriendDao friendDao, UserDao userDao)
    {
        this.server = server;
        this.friendDao = friendDao;
        this.userDao = userDao;
    }

    public String sendRequest(String nick, JsonObject payload) throws SQLException
    {
        String target = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (target == null) return IRCServiceDispatcher.errorJson("Missing nick");
        if (target.equalsIgnoreCase(nick)) return IRCServiceDispatcher.errorJson("Cannot friend yourself");

        this.userDao.ensureUser(target);
        boolean created = this.friendDao.sendRequest(nick, target);
        if (!created) return IRCServiceDispatcher.errorJson("Request already exists");

        // Notify target if online
        IRCServerHandler targetHandler = this.server.getClient(target);
        if (targetHandler != null)
        {
            JsonObject notify = new JsonObject();
            notify.addProperty("fromNick", nick);
            targetHandler.sendLine(":" + IRCServiceDispatcher.SERVICE_NICK + " NOTICE " + target
                    + " :\u0001FRIEND_REQUEST_NOTIFY " + notify.toString() + "\u0001");
        }
        return IRCServiceDispatcher.okJson();
    }

    /** Add friend by UID — resolves UID to nick, then sends request. */
    public String addByUid(String nick, JsonObject payload) throws SQLException
    {
        String uid = payload.has("uid") ? payload.get("uid").getAsString() : null;
        if (uid == null || uid.isEmpty()) return IRCServiceDispatcher.errorJson("Missing uid");

        String targetNick = this.userDao.getNickByUid(uid);
        if (targetNick == null) return IRCServiceDispatcher.errorJson("No user with UID: " + uid);
        if (targetNick.equalsIgnoreCase(nick)) return IRCServiceDispatcher.errorJson("Cannot friend yourself");

        boolean created = this.friendDao.sendRequest(nick, targetNick);
        if (!created) return IRCServiceDispatcher.errorJson("Request already exists");

        // Notify target if online
        IRCServerHandler targetHandler = this.server.getClient(targetNick);
        if (targetHandler != null)
        {
            JsonObject notify = new JsonObject();
            notify.addProperty("fromNick", nick);
            targetHandler.sendLine(":" + IRCServiceDispatcher.SERVICE_NICK + " NOTICE " + targetNick
                    + " :\u0001FRIEND_REQUEST_NOTIFY " + notify.toString() + "\u0001");
        }

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("targetNick", targetNick);
        return result.toString();
    }

    /** Get own UID. */
    public String getMyUid(String nick) throws SQLException
    {
        this.userDao.ensureUser(nick);
        String uid = this.userDao.getUid(nick);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("uid", uid != null ? uid : "");
        return result.toString();
    }

    public String acceptRequest(String nick, JsonObject payload) throws SQLException
    {
        String fromNick = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (fromNick == null) return IRCServiceDispatcher.errorJson("Missing nick");

        boolean accepted = this.friendDao.acceptRequest(nick, fromNick);
        if (!accepted) return IRCServiceDispatcher.errorJson("No pending request from " + fromNick);

        // Notify requester if online
        IRCServerHandler fromHandler = this.server.getClient(fromNick);
        if (fromHandler != null)
        {
            JsonObject notify = new JsonObject();
            notify.addProperty("nick", nick);
            fromHandler.sendLine(":" + IRCServiceDispatcher.SERVICE_NICK + " NOTICE " + fromNick
                    + " :\u0001FRIEND_ACCEPT_NOTIFY " + notify.toString() + "\u0001");
        }
        return IRCServiceDispatcher.okJson();
    }

    public String rejectRequest(String nick, JsonObject payload) throws SQLException
    {
        String fromNick = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (fromNick == null) return IRCServiceDispatcher.errorJson("Missing nick");

        boolean rejected = this.friendDao.rejectRequest(nick, fromNick);
        if (!rejected) return IRCServiceDispatcher.errorJson("No pending request from " + fromNick);
        return IRCServiceDispatcher.okJson();
    }

    public String removeFriend(String nick, JsonObject payload) throws SQLException
    {
        String target = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (target == null) return IRCServiceDispatcher.errorJson("Missing nick");

        boolean removed = this.friendDao.removeFriend(nick, target);
        if (!removed) return IRCServiceDispatcher.errorJson("Not friends with " + target);
        return IRCServiceDispatcher.okJson();
    }

    public String listFriends(String nick) throws SQLException
    {
        List<String> friends = this.friendDao.getFriends(nick);
        List<String> pending = this.friendDao.getPendingRequests(nick);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        JsonArray arr = new JsonArray();
        for (String f : friends)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("nick", f);
            entry.addProperty("online", this.server.getClient(f) != null);
            entry.addProperty("displayName", this.userDao.getDisplayNick(f));
            entry.addProperty("status", "accepted");
            String fUid = this.userDao.getUid(f);
            if (fUid != null) entry.addProperty("uid", fUid);
            arr.add(entry);
        }
        result.add("friends", arr);
        // Include pending requests
        JsonArray pendingArr = new JsonArray();
        for (String p : pending)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("nick", p);
            entry.addProperty("online", this.server.getClient(p) != null);
            entry.addProperty("status", "pending_in");
            pendingArr.add(entry);
        }
        result.add("pending", pendingArr);
        return result.toString();
    }

    public String listPending(String nick) throws SQLException
    {
        List<String> pending = this.friendDao.getPendingRequests(nick);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        JsonArray arr = new JsonArray();
        for (String p : pending) { arr.add(new JsonPrimitive(p)); }
        result.add("pending", arr);
        return result.toString();
    }

    public String getOnlineStatus(String nick, JsonObject payload) throws SQLException
    {
        String target = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (target == null) return IRCServiceDispatcher.errorJson("Missing nick");

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("nick", target);
        result.addProperty("online", this.server.getClient(target) != null);
        return result.toString();
    }
}