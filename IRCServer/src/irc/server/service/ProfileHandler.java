package irc.server.service;

import com.google.gson.JsonObject;
import irc.server.db.ProfileDao;
import irc.server.db.UserDao;

import java.sql.SQLException;

/**
 * Handles profile-related service commands.
 */
public final class ProfileHandler
{
    private final ProfileDao profileDao;
    private final UserDao userDao;

    public ProfileHandler(ProfileDao profileDao, UserDao userDao)
    {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    public String getProfile(String requesterNick, JsonObject payload) throws SQLException
    {
        String targetNick = payload.has("nick") ? payload.get("nick").getAsString() : requesterNick;
        long userId = this.userDao.getUserId(targetNick);
        if (userId < 0) return IRCServiceDispatcher.errorJson("User not found: " + targetNick);

        this.profileDao.ensureProfile(userId);
        String profileJson = this.profileDao.getProfile(targetNick);
        if (profileJson == null) return IRCServiceDispatcher.errorJson("Profile not found");

        return "{\"ok\":true,\"profile\":" + profileJson + "}";
    }

    public String updateProfile(String nick, JsonObject payload) throws SQLException
    {
        long userId = this.userDao.ensureUser(nick);
        this.profileDao.ensureProfile(userId);

        String data = payload.has("data") ? payload.get("data").toString() : payload.toString();
        boolean updated = this.profileDao.updateProfile(nick, data);
        if (!updated) return IRCServiceDispatcher.errorJson("Nothing to update");
        return IRCServiceDispatcher.okJson();
    }

    public String recordVisit(String visitorNick, JsonObject payload) throws SQLException
    {
        String profileNick = payload.has("nick") ? payload.get("nick").getAsString() : null;
        if (profileNick == null) return IRCServiceDispatcher.errorJson("Missing nick");

        this.profileDao.recordVisit(profileNick, visitorNick);
        return IRCServiceDispatcher.okJson();
    }
}
