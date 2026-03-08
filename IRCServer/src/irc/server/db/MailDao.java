package irc.server.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mail data access object.
 */
public final class MailDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public MailDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    /** Send a mail. Returns the mail id or -1. */
    public long sendMail(String fromNick, String toNick, String subject, String body) throws SQLException
    {
        long fromId = this.userDao.ensureUser(fromNick);
        long toId = this.userDao.getUserId(toNick);
        if (toId < 0) return -1;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mail (from_user_id, to_user_id, subject, body) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            try
            {
                ps.setLong(1, fromId);
                ps.setLong(2, toId);
                ps.setString(3, subject);
                ps.setString(4, body);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return -1;
    }

    /** List mail for a user (inbox or sent). Returns JSON array string. */
    public String listMail(String nick, boolean inbox, int limit, int offset) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return "[]";

        String col = inbox ? "to_user_id" : "from_user_id";
        String otherCol = inbox ? "from_user_id" : "to_user_id";

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.id, m.subject, m.body, m.is_read, m.created_at, u.nick AS other_nick "
                            + "FROM mail m JOIN users u ON u.id = m." + otherCol
                            + " WHERE m." + col + " = ? ORDER BY m.created_at DESC LIMIT ? OFFSET ?");
            try
            {
                ps.setLong(1, userId);
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                ResultSet rs = ps.executeQuery();
                JsonArray arr = new JsonArray();
                while (rs.next())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", rs.getLong("id"));
                    obj.addProperty("subject", rs.getString("subject"));
                    obj.addProperty("body", rs.getString("body"));
                    obj.addProperty("read", rs.getBoolean("is_read"));
                    obj.addProperty("otherNick", rs.getString("other_nick"));
                    obj.addProperty("createdMs", rs.getTimestamp("created_at").getTime());
                    arr.add(obj);
                }
                return arr.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Mark a mail as read. */
    public boolean markRead(String nick, long mailId) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE mail SET is_read = 1 WHERE id = ? AND to_user_id = ?");
            try
            {
                ps.setLong(1, mailId);
                ps.setLong(2, userId);
                return ps.executeUpdate() > 0;
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Delete a mail. */
    public boolean deleteMail(String nick, long mailId) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mail WHERE id = ? AND (from_user_id = ? OR to_user_id = ?)");
            try
            {
                ps.setLong(1, mailId);
                ps.setLong(2, userId);
                ps.setLong(3, userId);
                return ps.executeUpdate() > 0;
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Get unread mail count. */
    public int getUnreadCount(String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return 0;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM mail WHERE to_user_id = ? AND is_read = 0");
            try
            {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return 0;
    }
}
