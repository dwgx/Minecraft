package irc.server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import irc.server.IRCServer;
import irc.server.IRCServerHandler;
import irc.server.db.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Dispatches CTCP-style service commands sent to *IRCService.
 * Protocol: PRIVMSG *IRCService :\x01CMD json\x01
 * Reply:    NOTICE nick :\x01CMD_REPLY json\x01
 */
public final class IRCServiceDispatcher
{
    public static final String SERVICE_NICK = "*IRCService";

    private final IRCServer server;
    private final FriendHandler friendHandler;
    private final ProfileHandler profileHandler;
    private final MailHandler mailHandler;
    private final SocialHandler socialHandler;
    private final MessageHistoryHandler historyHandler;
    private final MessageHistoryDao historyDao;
    private final ChannelDao channelDao;
    private final ThreadPoolExecutor persistenceExecutor;

    public IRCServiceDispatcher(IRCServer server, MariaDbPool pool)
    {
        UserDao userDao = new UserDao(pool);
        FriendDao friendDao = new FriendDao(pool, userDao);
        ProfileDao profileDao = new ProfileDao(pool, userDao);
        MailDao mailDao = new MailDao(pool, userDao);
        SocialDao socialDao = new SocialDao(pool, userDao);
        ChannelDao channelDao = new ChannelDao(pool, userDao);
        MessageHistoryDao msgDao = new MessageHistoryDao(pool, userDao);

        this.server = server;
        this.friendHandler = new FriendHandler(server, friendDao, userDao);
        this.profileHandler = new ProfileHandler(profileDao, userDao);
        this.mailHandler = new MailHandler(server, mailDao);
        this.socialHandler = new SocialHandler(socialDao);
        this.historyHandler = new MessageHistoryHandler(msgDao);
        this.historyDao = msgDao;
        this.channelDao = channelDao;
        this.persistenceExecutor = createPersistenceExecutor();
    }

    /** Expose DAO for inline message persistence from handlePrivmsg. */
    public MessageHistoryDao getHistoryDao() { return this.historyDao; }
    public ChannelDao getChannelDao() { return this.channelDao; }

    public List<String> getUserChannels(String nick)
    {
        if (this.channelDao == null || nick == null || nick.isEmpty()) return new ArrayList<String>();
        try
        {
            return this.channelDao.getUserChannels(nick);
        }
        catch (Exception e)
        {
            System.err.println("[IRCServiceDispatcher] Failed to load channels for " + nick + ": " + e.getMessage());
            return new ArrayList<String>();
        }
    }

    public void persistChannelMessageAsync(final String fromNick, final String channel, final String content)
    {
        if (this.historyDao == null || fromNick == null || channel == null || content == null) return;
        submitPersistenceTask(new Runnable()
        {
            @Override
            public void run()
            {
                try { historyDao.saveChannelMessage(fromNick, channel, content); }
                catch (Exception e)
                {
                    System.err.println("[IRCServiceDispatcher] Failed to persist channel message: " + e.getMessage());
                }
            }
        });
    }

    public void persistPrivateMessageAsync(final String fromNick, final String toNick, final String content)
    {
        if (this.historyDao == null || fromNick == null || toNick == null || content == null) return;
        submitPersistenceTask(new Runnable()
        {
            @Override
            public void run()
            {
                try { historyDao.savePrivateMessage(fromNick, toNick, content); }
                catch (Exception e)
                {
                    System.err.println("[IRCServiceDispatcher] Failed to persist private message: " + e.getMessage());
                }
            }
        });
    }

    public void persistChannelJoinAsync(final String channelName, final String nick)
    {
        if (this.channelDao == null || channelName == null || nick == null) return;
        submitPersistenceTask(new Runnable()
        {
            @Override
            public void run()
            {
                try { channelDao.addMember(channelName, nick); }
                catch (Exception e)
                {
                    System.err.println("[IRCServiceDispatcher] Failed to persist channel join: " + e.getMessage());
                }
            }
        });
    }

    public void persistChannelPartAsync(final String channelName, final String nick)
    {
        if (this.channelDao == null || channelName == null || nick == null) return;
        submitPersistenceTask(new Runnable()
        {
            @Override
            public void run()
            {
                try { channelDao.removeMember(channelName, nick); }
                catch (Exception e)
                {
                    System.err.println("[IRCServiceDispatcher] Failed to persist channel part: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown()
    {
        this.persistenceExecutor.shutdown();
        try
        {
            if (!this.persistenceExecutor.awaitTermination(3, TimeUnit.SECONDS))
            {
                this.persistenceExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            this.persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handle a CTCP service message.
     * @param handler the client handler
     * @param text the trailing text from PRIVMSG (with CTCP delimiters stripped)
     */
    public void dispatch(IRCServerHandler handler, String text)
    {
        // Strip CTCP delimiters if present
        if (text.startsWith("\u0001") && text.endsWith("\u0001"))
        {
            text = text.substring(1, text.length() - 1);
        }

        // Parse: CMD json_payload
        int spaceIdx = text.indexOf(' ');
        String cmd;
        String jsonPayload;
        if (spaceIdx > 0)
        {
            cmd = text.substring(0, spaceIdx);
            jsonPayload = text.substring(spaceIdx + 1);
        }
        else
        {
            cmd = text;
            jsonPayload = "{}";
        }

        String nick = handler.getNick();
        if (nick == null) { sendReply(handler, cmd, errorJson("Not registered")); return; }

        try
        {
            JsonObject payload = new JsonParser().parse(jsonPayload).getAsJsonObject();
            String result = route(cmd, nick, payload, handler);
            sendReply(handler, cmd, result);
        }
        catch (Exception e)
        {
            System.err.println("[IRCServiceDispatcher] Error handling " + cmd + ": " + e.getMessage());
            sendReply(handler, cmd, errorJson(e.getMessage()));
        }
    }

    private String route(String cmd, String nick, JsonObject payload, IRCServerHandler handler) throws Exception
    {
        // Friend commands
        if ("FRIEND_REQUEST".equals(cmd)) return this.friendHandler.sendRequest(nick, payload);
        if ("FRIEND_ACCEPT".equals(cmd)) return this.friendHandler.acceptRequest(nick, payload);
        if ("FRIEND_REJECT".equals(cmd)) return this.friendHandler.rejectRequest(nick, payload);
        if ("FRIEND_REMOVE".equals(cmd)) return this.friendHandler.removeFriend(nick, payload);
        if ("FRIEND_LIST".equals(cmd)) return this.friendHandler.listFriends(nick);
        if ("FRIEND_PENDING".equals(cmd)) return this.friendHandler.listPending(nick);
        if ("FRIEND_STATUS".equals(cmd)) return this.friendHandler.getOnlineStatus(nick, payload);
        if ("FRIEND_ADD_BY_UID".equals(cmd)) return this.friendHandler.addByUid(nick, payload);
        if ("GET_MY_UID".equals(cmd)) return this.friendHandler.getMyUid(nick);

        // Profile commands
        if ("PROFILE_QUERY".equals(cmd)) return this.profileHandler.getProfile(nick, payload);
        if ("PROFILE_UPDATE".equals(cmd)) return this.profileHandler.updateProfile(nick, payload);
        if ("PROFILE_VISIT".equals(cmd)) return this.profileHandler.recordVisit(nick, payload);

        // Mail commands
        if ("MAIL_SEND".equals(cmd)) return this.mailHandler.sendMail(nick, payload);
        if ("MAIL_LIST".equals(cmd)) return this.mailHandler.listMail(nick, payload);
        if ("MAIL_READ".equals(cmd)) return this.mailHandler.markRead(nick, payload);
        if ("MAIL_DELETE".equals(cmd)) return this.mailHandler.deleteMail(nick, payload);
        if ("MAIL_UNREAD_COUNT".equals(cmd)) return this.mailHandler.getUnreadCount(nick);

        // Social commands
        if ("SOCIAL_POST".equals(cmd)) return this.socialHandler.createPost(nick, payload);
        if ("SOCIAL_FEED".equals(cmd)) return this.socialHandler.getFeed(nick, payload);
        if ("SOCIAL_LIKE".equals(cmd)) return this.socialHandler.toggleLike(nick, payload);
        if ("SOCIAL_COMMENT".equals(cmd)) return this.socialHandler.addComment(nick, payload);
        if ("SOCIAL_COMMENTS".equals(cmd)) return this.socialHandler.getComments(payload);

        // Message history commands
        if ("MSG_HISTORY_CHANNEL".equals(cmd)) return this.historyHandler.getChannelHistory(nick, payload);
        if ("MSG_HISTORY_PRIVATE".equals(cmd)) return this.historyHandler.getPrivateHistory(nick, payload);

        return errorJson("Unknown command: " + cmd);
    }

    void sendReply(IRCServerHandler handler, String cmd, String jsonResult)
    {
        handler.sendLine(":" + SERVICE_NICK + " NOTICE " + handler.getNick()
                + " :\u0001" + cmd + "_REPLY " + jsonResult + "\u0001");
    }

    static String errorJson(String message)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", true);
        obj.addProperty("message", message != null ? message : "Unknown error");
        return obj.toString();
    }

    static String okJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("ok", true);
        return obj.toString();
    }

    private void submitPersistenceTask(Runnable task)
    {
        try
        {
            this.persistenceExecutor.execute(task);
        }
        catch (Exception e)
        {
            System.err.println("[IRCServiceDispatcher] Persistence queue rejected task: " + e.getMessage());
        }
    }

    private static ThreadPoolExecutor createPersistenceExecutor()
    {
        final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(4096);
        ThreadFactory factory = new ThreadFactory()
        {
            private int idx = 1;
            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r, "IRC-Persist-" + (idx++));
                t.setDaemon(true);
                return t;
            }
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS, queue, factory);
        executor.prestartAllCoreThreads();
        return executor;
    }
}
