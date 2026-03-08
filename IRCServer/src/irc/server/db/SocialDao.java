package irc.server.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;

/**
 * Social posts/likes/comments data access object.
 */
public final class SocialDao
{
    private final MariaDbPool pool;
    private final UserDao userDao;

    public SocialDao(MariaDbPool pool, UserDao userDao)
    {
        this.pool = pool;
        this.userDao = userDao;
    }

    /** Create a post. Returns post id or -1. */
    public long createPost(String nick, String content, String imageUrl) throws SQLException
    {
        long authorId = this.userDao.ensureUser(nick);
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO social_posts (author_id, content, image_url) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            try
            {
                ps.setLong(1, authorId);
                ps.setString(2, content);
                ps.setString(3, imageUrl);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return -1;
    }

    /** Get feed (recent posts). Returns JSON array string. */
    public String getFeed(String viewerNick, int limit, int offset) throws SQLException
    {
        long viewerId = this.userDao.getUserId(viewerNick);
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.id, p.content, p.image_url, p.like_count, p.created_at, u.nick AS author_nick, "
                            + "(SELECT COUNT(*) FROM social_likes sl WHERE sl.post_id = p.id AND sl.user_id = ?) AS liked "
                            + "FROM social_posts p JOIN users u ON u.id = p.author_id "
                            + "ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
            try
            {
                ps.setLong(1, viewerId > 0 ? viewerId : 0);
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                ResultSet rs = ps.executeQuery();
                JsonArray arr = new JsonArray();
                while (rs.next())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", rs.getLong("id"));
                    obj.addProperty("authorNick", rs.getString("author_nick"));
                    obj.addProperty("content", rs.getString("content"));
                    String img = rs.getString("image_url");
                    obj.addProperty("imageUrl", img != null ? img : "");
                    obj.addProperty("likeCount", rs.getInt("like_count"));
                    obj.addProperty("likedByMe", rs.getInt("liked") > 0);
                    obj.addProperty("createdMs", rs.getTimestamp("created_at").getTime());
                    arr.add(obj);
                }
                return arr.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Toggle like on a post. Returns true if now liked, false if unliked. */
    public boolean toggleLike(String nick, long postId) throws SQLException
    {
        long userId = this.userDao.ensureUser(nick);
        Connection conn = this.pool.getConnection();
        try
        {
            // Check if already liked
            PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM social_likes WHERE post_id = ? AND user_id = ?");
            boolean exists;
            try
            {
                check.setLong(1, postId);
                check.setLong(2, userId);
                exists = check.executeQuery().next();
            }
            finally { check.close(); }

            if (exists)
            {
                PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM social_likes WHERE post_id = ? AND user_id = ?");
                try { del.setLong(1, postId); del.setLong(2, userId); del.executeUpdate(); }
                finally { del.close(); }

                PreparedStatement dec = conn.prepareStatement(
                        "UPDATE social_posts SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?");
                try { dec.setLong(1, postId); dec.executeUpdate(); }
                finally { dec.close(); }
                return false;
            }
            else
            {
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT IGNORE INTO social_likes (post_id, user_id) VALUES (?, ?)");
                try { ins.setLong(1, postId); ins.setLong(2, userId); ins.executeUpdate(); }
                finally { ins.close(); }

                PreparedStatement inc = conn.prepareStatement(
                        "UPDATE social_posts SET like_count = like_count + 1 WHERE id = ?");
                try { inc.setLong(1, postId); inc.executeUpdate(); }
                finally { inc.close(); }
                return true;
            }
        }
        finally { this.pool.returnConnection(conn); }
    }

    /** Add a comment to a post. Returns comment id or -1. */
    public long addComment(String nick, long postId, String content) throws SQLException
    {
        long authorId = this.userDao.ensureUser(nick);
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO social_comments (post_id, author_id, content) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            try
            {
                ps.setLong(1, postId);
                ps.setLong(2, authorId);
                ps.setString(3, content);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
        return -1;
    }

    /** Get comments for a post. Returns JSON array string. */
    public String getComments(long postId, int limit) throws SQLException
    {
        Connection conn = this.pool.getConnection();
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.id, c.content, c.created_at, u.nick AS author_nick "
                            + "FROM social_comments c JOIN users u ON u.id = c.author_id "
                            + "WHERE c.post_id = ? ORDER BY c.created_at ASC LIMIT ?");
            try
            {
                ps.setLong(1, postId);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                JsonArray arr = new JsonArray();
                while (rs.next())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", rs.getLong("id"));
                    obj.addProperty("authorNick", rs.getString("author_nick"));
                    obj.addProperty("content", rs.getString("content"));
                    obj.addProperty("createdMs", rs.getTimestamp("created_at").getTime());
                    arr.add(obj);
                }
                return arr.toString();
            }
            finally { ps.close(); }
        }
        finally { this.pool.returnConnection(conn); }
    }
}