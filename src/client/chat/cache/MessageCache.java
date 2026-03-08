package client.chat.cache;

import client.chat.model.ChatMessage;
import client.chat.model.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caches chat messages in SQLite for offline viewing.
 */
public final class MessageCache
{
    private final IRCLocalCache cache;

    public MessageCache(IRCLocalCache cache) { this.cache = cache; }

    public void saveMessage(ChatMessage msg)
    {
        if (!this.cache.isOpen() || msg == null) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO messages (id, sender_id, conversation_id, type, text_content, timestamp_ms) "
                            + "VALUES (?, ?, ?, ?, ?, ?)");
            try
            {
                ps.setString(1, msg.getId());
                ps.setString(2, msg.getSenderId());
                ps.setString(3, msg.getConversationId());
                ps.setString(4, msg.getType().name());
                ps.setString(5, msg.getTextContent());
                ps.setLong(6, msg.getTimestampMs());
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[MessageCache] Save failed: " + e.getMessage());
        }
    }

    public List<ChatMessage> getMessages(String conversationId, int limit)
    {
        if (!this.cache.isOpen()) return Collections.emptyList();
        List<ChatMessage> result = new ArrayList<ChatMessage>();
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "SELECT id, sender_id, conversation_id, type, text_content, timestamp_ms "
                            + "FROM messages WHERE conversation_id = ? ORDER BY timestamp_ms DESC LIMIT ?");
            try
            {
                ps.setString(1, conversationId);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                {
                    MessageType type;
                    try { type = MessageType.valueOf(rs.getString("type")); }
                    catch (Exception e) { type = MessageType.TEXT; }

                    result.add(ChatMessage.text(
                            rs.getString("id"),
                            rs.getString("sender_id"),
                            rs.getString("conversation_id"),
                            rs.getString("text_content"),
                            rs.getLong("timestamp_ms")
                    ));
                }
            }
            finally { ps.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[MessageCache] Load failed: " + e.getMessage());
        }
        Collections.reverse(result);
        return result;
    }

    /** Trim old messages per conversation. */
    public void trimMessages(String conversationId, int maxKeep)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "DELETE FROM messages WHERE conversation_id = ? AND id NOT IN "
                            + "(SELECT id FROM messages WHERE conversation_id = ? ORDER BY timestamp_ms DESC LIMIT ?)");
            try
            {
                ps.setString(1, conversationId);
                ps.setString(2, conversationId);
                ps.setInt(3, maxKeep);
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
    }
}
