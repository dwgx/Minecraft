package irc.server.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Friend relationship data access object.
 */
public final class FriendDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public FriendDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    /** Send a friend request. Returns true if created. */
    public boolean sendRequest(String fromNick, String toNick) throws SQLException
    {
        long fromId = this.userDao.ensureUser(fromNick);
        long toId = this.userDao.ensureUser(toNick);
        if (fromId == toId) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO friends (user_id, friend_id, status) VALUES (?, ?, 'pending')");
            try
            {
                ps.setLong(1, fromId);
                ps.setLong(2, toId);
                return ps.executeUpdate() > 0;
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Accept a pending friend request (bidirectional). */
    public boolean acceptRequest(String nick, String fromNick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        long fromId = this.userDao.getUserId(fromNick);
        if (userId < 0 || fromId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            // Update the pending request to accepted
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE friends SET status = 'accepted' WHERE user_id = ? AND friend_id = ? AND status = 'pending'");
            try
            {
                ps.setLong(1, fromId);
                ps.setLong(2, userId);
                if (ps.executeUpdate() == 0) return false;
            }
            finally { ps.close(); }

            // Create reverse relationship
            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT IGNORE INTO friends (user_id, friend_id, status) VALUES (?, ?, 'accepted')");
            try
            {
                ps2.setLong(1, userId);
                ps2.setLong(2, fromId);
                ps2.executeUpdate();
            }
            finally { ps2.close(); }
            return true;
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Reject a pending friend request. */
    public boolean rejectRequest(String nick, String fromNick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        long fromId = this.userDao.getUserId(fromNick);
        if (userId < 0 || fromId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE friends SET status = 'rejected' WHERE user_id = ? AND friend_id = ? AND status = 'pending'");
            try
            {
                ps.setLong(1, fromId);
                ps.setLong(2, userId);
                return ps.executeUpdate() > 0;
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Remove a friend (bidirectional). */
    public boolean removeFriend(String nick, String friendNick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        long friendId = this.userDao.getUserId(friendNick);
        if (userId < 0 || friendId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)");
            try
            {
                ps.setLong(1, userId);
                ps.setLong(2, friendId);
                ps.setLong(3, friendId);
                ps.setLong(4, userId);
                return ps.executeUpdate() > 0;
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Get accepted friends list. Returns list of nicks. */
    public List<String> getFriends(String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        List<String> result = new ArrayList<String>();
        if (userId < 0) return result;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.nick FROM friends f JOIN users u ON u.id = f.friend_id "
                            + "WHERE f.user_id = ? AND f.status = 'accepted'");
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

    /** Get pending incoming friend requests. Returns list of nicks. */
    public List<String> getPendingRequests(String nick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        List<String> result = new ArrayList<String>();
        if (userId < 0) return result;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.nick FROM friends f JOIN users u ON u.id = f.user_id "
                            + "WHERE f.friend_id = ? AND f.status = 'pending'");
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

    /** Check if two users are friends. */
    public boolean isFriend(String nick, String otherNick) throws SQLException
    {
        long userId = this.userDao.getUserId(nick);
        long otherId = this.userDao.getUserId(otherNick);
        if (userId < 0 || otherId < 0) return false;

        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ? AND status = 'accepted'");
            try
            {
                ps.setLong(1, userId);
                ps.setLong(2, otherId);
                return ps.executeQuery().next();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }
}
