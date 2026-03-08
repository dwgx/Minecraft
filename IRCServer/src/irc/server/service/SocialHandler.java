package irc.server.service;

import com.google.gson.JsonObject;
import irc.server.db.SocialDao;

import java.sql.SQLException;

/**
 * Handles social feed service commands (posts, likes, comments).
 */
public final class SocialHandler
{
    private final SocialDao socialDao;

    public SocialHandler(SocialDao socialDao) { this.socialDao = socialDao; }

    public String createPost(String nick, JsonObject payload) throws SQLException
    {
        String content = payload.has("content") ? payload.get("content").getAsString() : null;
        if (content == null || content.isEmpty()) return IRCServiceDispatcher.errorJson("Missing content");
        String imageUrl = payload.has("imageUrl") ? payload.get("imageUrl").getAsString() : null;

        long postId = this.socialDao.createPost(nick, content, imageUrl);
        if (postId < 0) return IRCServiceDispatcher.errorJson("Failed to create post");

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("postId", postId);
        return result.toString();
    }

    public String getFeed(String nick, JsonObject payload) throws SQLException
    {
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 20;
        int offset = payload.has("offset") ? payload.get("offset").getAsInt() : 0;

        String feedJson = this.socialDao.getFeed(nick, limit, offset);
        return "{\"ok\":true,\"posts\":" + feedJson + "}";
    }

    public String toggleLike(String nick, JsonObject payload) throws SQLException
    {
        long postId = payload.has("postId") ? payload.get("postId").getAsLong() : -1;
        if (postId < 0) return IRCServiceDispatcher.errorJson("Missing postId");

        boolean liked = this.socialDao.toggleLike(nick, postId);
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("liked", liked);
        return result.toString();
    }

    public String addComment(String nick, JsonObject payload) throws SQLException
    {
        long postId = payload.has("postId") ? payload.get("postId").getAsLong() : -1;
        String content = payload.has("content") ? payload.get("content").getAsString() : null;
        if (postId < 0 || content == null) return IRCServiceDispatcher.errorJson("Missing postId/content");

        long commentId = this.socialDao.addComment(nick, postId, content);
        if (commentId < 0) return IRCServiceDispatcher.errorJson("Failed to add comment");

        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("commentId", commentId);
        return result.toString();
    }

    public String getComments(JsonObject payload) throws SQLException
    {
        long postId = payload.has("postId") ? payload.get("postId").getAsLong() : -1;
        if (postId < 0) return IRCServiceDispatcher.errorJson("Missing postId");
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 50;

        String commentsJson = this.socialDao.getComments(postId, limit);
        return "{\"ok\":true,\"comments\":" + commentsJson + "}";
    }
}
