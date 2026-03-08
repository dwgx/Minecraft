package irc.server.service;

import com.google.gson.JsonObject;
import irc.server.IRCServer;
import irc.server.IRCServerHandler;
import irc.server.db.MailDao;

import java.sql.SQLException;

/**
 * Handles mail-related service commands.
 */
public final class MailHandler
{
    private final IRCServer server;
    private final MailDao mailDao;

    public MailHandler(IRCServer server, MailDao mailDao)
    {
        this.server = server;
        this.mailDao = mailDao;
    }

    public String sendMail(String nick, JsonObject payload) throws SQLException
    {
        String toNick = payload.has("to") ? payload.get("to").getAsString() : null;
        String subject = payload.has("subject") ? payload.get("subject").getAsString() : null;
        String body = payload.has("body") ? payload.get("body").getAsString() : "";
        if (toNick == null || subject == null) return IRCServiceDispatcher.errorJson("Missing to/subject");

        long mailId = this.mailDao.sendMail(nick, toNick, subject, body);
        if (mailId < 0) return IRCServiceDispatcher.errorJson("Recipient not found");

        // Notify recipient if online
        IRCServerHandler recipient = this.server.getClient(toNick);
        if (recipient != null)
        {
            JsonObject notify = new JsonObject();
            notify.addProperty("fromNick", nick);
            notify.addProperty("subject", subject);
            notify.addProperty("mailId", mailId);
            recipient.sendLine(":" + IRCServiceDispatcher.SERVICE_NICK + " NOTICE " + toNick
                    + " :\u0001MAIL_NOTIFY " + notify.toString() + "\u0001");
        }

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("mailId", mailId);
        return result.toString();
    }

    public String listMail(String nick, JsonObject payload) throws SQLException
    {
        boolean inbox = !payload.has("box") || "inbox".equals(payload.get("box").getAsString());
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 50;
        int offset = payload.has("offset") ? payload.get("offset").getAsInt() : 0;

        String mailsJson = this.mailDao.listMail(nick, inbox, limit, offset);
        return "{\"ok\":true,\"mails\":" + mailsJson + "}";
    }

    public String markRead(String nick, JsonObject payload) throws SQLException
    {
        long mailId = payload.has("id") ? payload.get("id").getAsLong() : -1;
        if (mailId < 0) return IRCServiceDispatcher.errorJson("Missing id");

        boolean marked = this.mailDao.markRead(nick, mailId);
        if (!marked) return IRCServiceDispatcher.errorJson("Mail not found or not yours");
        return IRCServiceDispatcher.okJson();
    }

    public String deleteMail(String nick, JsonObject payload) throws SQLException
    {
        long mailId = payload.has("id") ? payload.get("id").getAsLong() : -1;
        if (mailId < 0) return IRCServiceDispatcher.errorJson("Missing id");

        boolean deleted = this.mailDao.deleteMail(nick, mailId);
        if (!deleted) return IRCServiceDispatcher.errorJson("Mail not found or not yours");
        return IRCServiceDispatcher.okJson();
    }

    public String getUnreadCount(String nick) throws SQLException
    {
        int count = this.mailDao.getUnreadCount(nick);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("count", count);
        return result.toString();
    }
}