package irc.server.db;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.*;

/**
 * Profile data access object.
 */
public final class ProfileDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public ProfileDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    public void ensureProfile(long userId) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO profiles (user_id) VALUES (?)");
            try { ps.setLong(1, userId); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Get profile as JSON string. */
    public String getProfile(String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return null;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.bio, p.avatar_hash, p.theme_color, p.game_stats, p.visitor_count, "
                            + "u.display_nick, u.created_at, u.uid "
                            + "FROM profiles p JOIN users u ON u.id = p.user_id WHERE p.user_id = ?");
            try
            {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;

                JsonObject obj = new JsonObject();
                obj.addProperty("nick", nick);
                obj.addProperty("displayName", rs.getString("display_nick"));
                obj.addProperty("bio", rs.getString("bio") != null ? rs.getString("bio") : "");
                obj.addProperty("avatarHash", rs.getString("avatar_hash") != null ? rs.getString("avatar_hash") : "");
                obj.addProperty("themeColor", rs.getString("theme_color"));
                obj.addProperty("gameStats", rs.getString("game_stats") != null ? rs.getString("game_stats") : "");
                obj.addProperty("visitorCount", rs.getInt("visitor_count"));
                obj.addProperty("joinDate", rs.getTimestamp("created_at").getTime());
                obj.addProperty("uid", rs.getString("uid") != null ? rs.getString("uid") : "");
                return obj.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Update profile fields from JSON. */
    public boolean updateProfile(String nick, String jsonStr) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        if (userId < 0) return false;
        ensureProfile(userId);

        JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        Connection conn = this.pool.getConnection();
        try
        {
            StringBuilder sb = new StringBuilder("UPDATE profiles SET ");
            boolean first = true;
            if (json.has("bio")) { sb.append("bio = ?"); first = false; }
            if (json.has("avatarHash")) { if (!first) sb.append(", "); sb.append("avatar_hash = ?"); first = false; }
            if (json.has("themeColor")) { if (!first) sb.append(", "); sb.append("theme_color = ?"); first = false; }
            if (json.has("gameStats")) { if (!first) sb.append(", "); sb.append("game_stats = ?"); first = false; }
            if (first) return false;
            sb.append(" WHERE user_id = ?");

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            try
            {
                int idx = 1;
                if (json.has("bio")) ps.setString(idx++, json.get("bio").getAsString());
                if (json.has("avatarHash")) ps.setString(idx++, json.get("avatarHash").getAsString());
                if (json.has("themeColor")) ps.setString(idx++, json.get("themeColor").getAsString());
                if (json.has("gameStats")) ps.setString(idx++, json.get("gameStats").getAsString());
                ps.setLong(idx, userId);
                ps.executeUpdate();
            }
            finally { ps.close(); }

            // Update display nick if provided
            if (json.has("displayName"))
            {
                this.userDao.setDisplayNick(nick, json.get("displayName").getAsString());
            }
            return true;
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Record a profile visit and increment visitor count. */
    public void recordVisit(String profileNick, String visitorNick) throws SQLException
    {
        long profileId = this.userDao.getUserId(profileNick);
        long visitorId = this.userDao.getUserId(visitorNick);
        if (profileId < 0 || visitorId < 0 || profileId == visitorId) return;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO profile_visitors (profile_user_id, visitor_user_id) VALUES (?, ?)");
            try { ps.setLong(1, profileId); ps.setLong(2, visitorId); ps.executeUpdate(); }
            finally { ps.close(); }

            PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE profiles SET visitor_count = visitor_count + 1 WHERE user_id = ?");
            try { ps2.setLong(1, profileId); ps2.executeUpdate(); }
            finally { ps2.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }
}
