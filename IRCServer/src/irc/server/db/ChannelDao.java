package irc.server.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel persistence data access object.
 */
public final class ChannelDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public ChannelDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    public void ensureChannel(String name) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO channels (name) VALUES (?)");
            try { ps.setString(1, name.toLowerCase()); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    public void addMember(String channelName, String nick) throws SQLException
    {
        ensureChannel(channelName);
        long userId = this.userDao.ensureUser(nick);
        long channelId = getChannelId(channelName);
        if (channelId < 0) return;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO channel_members (channel_id, user_id) VALUES (?, ?)");
            try { ps.setLong(1, channelId); ps.setLong(2, userId); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    public void removeMember(String channelName, String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        long channelId = getChannelId(channelName);
        if (userId < 0 || channelId < 0) return;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM channel_members WHERE channel_id = ? AND user_id = ?");
            try { ps.setLong(1, channelId); ps.setLong(2, userId); ps.executeUpdate(); }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    public List<String> getUserChannels(String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        List<String> result = new ArrayList<String>();
        if (userId < 0) return result;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.name FROM channel_members cm JOIN channels c ON c.id = cm.channel_id WHERE cm.user_id = ?");
            try
            {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) result.add(rs.getString(1));
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return result;
    }

    private long getChannelId(String name) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM channels WHERE name = ?");
            try
            {
                ps.setString(1, name.toLowerCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return -1;
    }
}
