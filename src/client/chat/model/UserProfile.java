package client.chat.model;

import java.util.List;

/**
 * User profile data.
 */
public final class UserProfile
{
    private String nick;
    private String displayName;
    private String bio;
    private String avatarHash;
    private String themeColor;
    private String gameStats;
    private String uid;
    private int visitorCount;
    private long joinDate;
    private List<String> friends;
    private List<String> achievements;

    public UserProfile(String nick) { this.nick = nick; }

    public String getNick() { return this.nick; }
    public void setNick(String nick) { this.nick = nick; }
    public String getDisplayName() { return this.displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return this.bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarHash() { return this.avatarHash; }
    public void setAvatarHash(String avatarHash) { this.avatarHash = avatarHash; }
    public String getThemeColor() { return this.themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
    public String getGameStats() { return this.gameStats; }
    public void setGameStats(String gameStats) { this.gameStats = gameStats; }
    public String getUid() { return this.uid; }
    public void setUid(String uid) { this.uid = uid; }
    public int getVisitorCount() { return this.visitorCount; }
    public void setVisitorCount(int visitorCount) { this.visitorCount = visitorCount; }
    public long getJoinDate() { return this.joinDate; }
    public void setJoinDate(long joinDate) { this.joinDate = joinDate; }
    public List<String> getFriends() { return this.friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }
    public List<String> getAchievements() { return this.achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }
}
