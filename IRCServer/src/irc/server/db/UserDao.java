package irc.server.db;

import java.sql.*;
import java.util.Random;

/**
 * User data access object.
 * Each user gets a unique UID (8-char alphanumeric) for friend-add-by-ID.
 */
public final class UserDao
{
    private static final String UID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int UID_LEN = 8;
    private static final Random RNG = new Random();

    private final MariaDbPool pool;

    public UserDao(MariaDbPool pool) { this.pool = pool; }

    public long ensureUser(String nick) throws SQLException
    {
        long id = getUserId(nick);
        if (id > 0) return id;

        String uid = generateUid();
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (nick, display_nick, uid) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                    Statement.RETURN_GENERATED_KEYS);
            try
            {
                ps.setString(1, nick.toLowerCase());
                ps.setString(2, nick);
                ps.setString(3, uid);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return getUserId(nick);
    }

    public long getUserId(String nick) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick = ?");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return -1;
    }

    /** Look up nick by UID. Returns null if not found. */
    public String getNickByUid(String uid) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("SELECT nick FROM users WHERE uid = ?");
            try
            {
                ps.setString(1, uid.toUpperCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return null;
    }

    /** Get UID for a nick. */
    public String getUid(String nick) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("SELECT uid FROM users WHERE nick = ?");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return null;
    }

    public String getDisplayNick(String nick) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("SELECT display_nick FROM users WHERE nick = ?");
            try
            {
                ps.setString(1, nick.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return nick;
    }

    public void setDisplayNick(String nick, String displayNick) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET display_nick = ? WHERE nick = ?");
            try
            {
                ps.setString(1, displayNick);
                ps.setString(2, nick.toLowerCase());
                ps.executeUpdate();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    private String generateUid()
    {
        StringBuilder sb = new StringBuilder(UID_LEN);
        for (int i = 0; i < UID_LEN; i++)
        {
            sb.append(UID_CHARS.charAt(RNG.nextInt(UID_CHARS.length())));
        }
        return sb.toString();
    }
}
