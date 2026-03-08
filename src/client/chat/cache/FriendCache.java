package client.chat.cache;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caches friend list in SQLite.
 */
public final class FriendCache
{
    private final IRCLocalCache cache;

    public FriendCache(IRCLocalCache cache) { this.cache = cache; }

    public void saveFriend(String nick, String displayName, String status)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO friends (nick, display_name, status, updated_ms) VALUES (?, ?, ?, ?)");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ps.setString(2, displayName);
                ps.setString(3, status);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        catch (SQLException e)
        {
            System.err.println("[FriendCache] Save failed: " + e.getMessage());
        }
    }

    public void removeFriend(String nick)
    {
        if (!this.cache.isOpen()) return;
        try
        {
            PreparedStatement ps = this.cache.getConnection().prepareStatement(
                    "DELETE FROM friends WHERE nick = ?");
            try { ps.setString(1, nick.toLowerCase()); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        catch (SQLException ignored) {}
    }

    /** Returns list of [nick, displayName, status] arrays. */
    public List<String[]> getAllFriends()
    {
        if (!this.cache.isOpen()) return Collections.emptyList();
        List<String[]> result = new ArrayList<String[]>();
        try
        {
            Statement st = this.cache.getConnection().createStatement();
            try
            {
                ResultSet rs = st.executeQuery("SELECT nick, display_name, status FROM friends ORDER BY nick");
                while (rs.next())
                {
                    result.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
            }
            finally { st.close(); }
        }
        catch (SQLException ignored) {}
        return result;
    }
}
