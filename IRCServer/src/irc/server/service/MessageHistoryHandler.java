package irc.server.service;

import com.google.gson.JsonObject;
import irc.server.db.MessageHistoryDao;

/**
 * Handles MSG_HISTORY commands for retrieving chat history.
 */
public final class MessageHistoryHandler
{
    private final MessageHistoryDao dao;

    public MessageHistoryHandler(MessageHistoryDao dao)
    {
        this.dao = dao;
    }

    /**
     * Get channel history.
     * Payload: {"channel":"#general","limit":50,"beforeId":0}
     */
    public String getChannelHistory(String nick, JsonObject payload) throws Exception
    {
        String channel = payload.has("channel") ? payload.get("channel").getAsString() : "#general";
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 50;
        long beforeId = payload.has("beforeId") ? payload.get("beforeId").getAsLong() : 0;
        if (limit > 200) limit = 200;

        String messages = this.dao.getChannelHistory(channel, limit, beforeId);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("channel", channel);
        result.addProperty("messagesJson", messages);
        return result.toString();
    }

    /**
     * Get private message history.
     * Payload: {"nick":"otherUser","limit":50,"beforeId":0}
     */
    public String getPrivateHistory(String nick, JsonObject payload) throws Exception
    {
        String otherNick = payload.has("nick") ? payload.get("nick").getAsString() : "";
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 50;
        long beforeId = payload.has("beforeId") ? payload.get("beforeId").getAsLong() : 0;
        if (limit > 200) limit = 200;

        String messages = this.dao.getPrivateHistory(nick, otherNick, limit, beforeId);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("nick", otherNick);
        result.addProperty("messagesJson", messages);
        return result.toString();
    }
}
