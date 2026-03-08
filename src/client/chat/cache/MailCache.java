package client.chat.cache;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caches mail in SQLite.
 */
public final class MailCache
{
    private final IRCLocalCache cache;

    public MailCache(IRCLocalCache cache) { this.cache = cache; }

    public void saveMail(long id, String otherNick, String subject, String body,
                         boolean inbox, boolean read, long createdMs)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO mail (id, other_nick, subject, body, is_inbox, is_read, created_ms) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)");
            try
            {
                ps.setLong(1, id);
                ps.setString(2, otherNick);
                ps.setString(3, subject);
                ps.setString(4, body);
                ps.setInt(5, inbox ? 1 : 0);
                ps.setInt(6, read ? 1 : 0);
                ps.setLong(7, createdMs);
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[MailCache] Save failed: " + e.getMessage());
        }
    }

    public void markRead(long id)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "UPDATE mail SET is_read = 1 WHERE id = ?");
            try { ps.setLong(1, id); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
    }

    public void deleteMail(long id)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "DELETE FROM mail WHERE id = ?");
            try { ps.setLong(1, id); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
    }

    /** Returns list of [id, otherNick, subject, body, isRead, createdMs]. */
    public List<String[]> getMail(boolean inbox, int limit)
    {
        if (!this.cache.isOpen()) return Collections.emptyList();
        List<String[]> result = new ArrayList<String[]>();
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "SELECT id, other_nick, subject, body, is_read, created_ms FROM mail "
                            + "WHERE is_inbox = ? ORDER BY created_ms DESC LIMIT ?");
            try
            {
                ps.setInt(1, inbox ? 1 : 0);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                {
                    result.add(new String[]{
                            String.valueOf(rs.getLong(1)), rs.getString(2), rs.getString(3),
                            rs.getString(4), String.valueOf(rs.getInt(5)), String.valueOf(rs.getLong(6))
                    });
                }
            }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
        return result;
    }
}
