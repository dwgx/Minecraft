package irc.server.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;

/**
 * Persists and retrieves channel/private message history.
 */
public final class MessageHistoryDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public MessageHistoryDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    /** Save a channel message. */
    public void saveChannelMessage(String fromNick, String channel, String content) throws SQLException
    {
        long fromId = this.userDao.ensureUser(fromNick);
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO message_history (from_user_id, channel_name, content) VALUES (?, ?, ?)");
            try
            {
                ps.setLong(1, fromId);
                ps.setString(2, channel);
                ps.setString(3, content);
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Save a private message. */
    public void savePrivateMessage(String fromNick, String toNick, String content) throws SQLException
    {
        long fromId = this.userDao.ensureUser(fromNick);
        long toId = this.userDao.ensureUser(toNick);
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO message_history (from_user_id, to_user_id, content) VALUES (?, ?, ?)");
            try
            {
                ps.setLong(1, fromId);
                ps.setLong(2, toId);
                ps.setString(3, content);
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Get channel message history. Returns JSON array string. */
    public String getChannelHistory(String channel, int limit, long beforeId) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            String sql = "SELECT h.id, u.nick AS from_nick, h.content, h.created_at "
                    + "FROM message_history h JOIN users u ON u.id = h.from_user_id "
                    + "WHERE h.channel_name = ? AND h.to_user_id IS NULL"
                    + (beforeId > 0 ? " AND h.id < ?" : "")
                    + " ORDER BY h.id DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            try
            {
                int idx = 1;
                ps.setString(idx++, channel);
                if (beforeId > 0) ps.setLong(idx++, beforeId);
                ps.setInt(idx, limit);
                ResultSet rs = ps.executeQuery();
                JsonArray arr = new JsonArray();
                while (rs.next())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", rs.getLong("id"));
                    obj.addProperty("from", rs.getString("from_nick"));
                    obj.addProperty("content", rs.getString("content"));
                    obj.addProperty("ts", rs.getTimestamp("created_at").getTime());
                    arr.add(obj);
                }
                return arr.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Get private message history between two users. Returns JSON array string. */
    public String getPrivateHistory(String nick1, String nick2, int limit, long beforeId) throws SQLException
    {
        long id1 = this.userDao.getUserId(nick1);
        long id2 = this.userDao.getUserId(nick2);
        if (id1 < 0 || id2 < 0) return "[]";

        Connection conn = this.pool.getConnection();
        try
        {
            String sql = "SELECT h.id, u.nick AS from_nick, h.content, h.created_at "
                    + "FROM message_history h JOIN users u ON u.id = h.from_user_id "
                    + "WHERE h.channel_name IS NULL "
                    + "AND ((h.from_user_id = ? AND h.to_user_id = ?) OR (h.from_user_id = ? AND h.to_user_id = ?))"
                    + (beforeId > 0 ? " AND h.id < ?" : "")
                    + " ORDER BY h.id DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            try
            {
                int idx = 1;
                ps.setLong(idx++, id1);
                ps.setLong(idx++, id2);
                ps.setLong(idx++, id2);
                ps.setLong(idx++, id1);
                if (beforeId > 0) ps.setLong(idx++, beforeId);
                ps.setInt(idx, limit);
                ResultSet rs = ps.executeQuery();
                JsonArray arr = new JsonArray();
                while (rs.next())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", rs.getLong("id"));
                    obj.addProperty("from", rs.getString("from_nick"));
                    obj.addProperty("content", rs.getString("content"));
                    obj.addProperty("ts", rs.getTimestamp("created_at").getTime());
                    arr.add(obj);
                }
                return arr.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }
}
