package client.chat.cache;

import java.sql.*;

/**
 * Caches user profiles in SQLite with TTL.
 */
public final class ProfileCache
{
    private static final long TTL_MS = 5 * 60 * 1000; // 5 minutes

    private final IRCLocalCache cache;

    public ProfileCache(IRCLocalCache cache) { this.cache = cache; }

    public void saveProfile(String nick, String jsonData)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO profiles (nick, json_data, cached_ms) VALUES (?, ?, ?)");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ps.setString(2, jsonData);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[ProfileCache] Save failed: " + e.getMessage());
        }
    }

    /** Get cached profile JSON, or null if expired/missing. */
    public String getProfile(String nick)
    {
        if (!this.cache.isOpen()) return null;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "SELECT json_data, cached_ms FROM profiles WHERE nick = ?");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                {
                    long cachedMs = rs.getLong("cached_ms");
                    if (System.currentTimeMillis() - cachedMs < TTL_MS)
                    {
                        return rs.getString("json_data");
                    }
                }
            }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
        return null;
    }

    /** Clear all expired profiles. */
    public void clearExpired()
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "DELETE FROM profiles WHERE cached_ms < ?");
            try
            {
                ps.setLong(1, System.currentTimeMillis() - TTL_MS);
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
    }
}
