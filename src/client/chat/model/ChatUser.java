package client.chat.model;

public final class ChatUser {
    private final String id;
    private String nickname;
    private String avatarPath;
    private UserStatus status;
    private long lastSeenMs;

    public ChatUser(String id, String nickname, String avatarPath, UserStatus status) {
        this.id = id;
        this.nickname = nickname;
        this.avatarPath = avatarPath;
        this.status = status;
        this.lastSeenMs = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public long getLastSeenMs() { return lastSeenMs; }
    public void setLastSeenMs(long lastSeenMs) { this.lastSeenMs = lastSeenMs; }
    public boolean isOnline() { return status == UserStatus.ONLINE || status == UserStatus.AWAY || status == UserStatus.DND; }

    public String getInitial() {
        if (nickname == null || nickname.isEmpty()) return "?";
        return nickname.substring(0, 1).toUpperCase();
    }
}
