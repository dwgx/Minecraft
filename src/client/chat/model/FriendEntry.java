package client.chat.model;

/**
 * A friend list entry with online status and relationship state.
 */
public final class FriendEntry
{
    private final String nick;
    private final String displayName;
    private final boolean online;
    private final FriendStatus friendStatus;

    public FriendEntry(String nick, String displayName, boolean online, FriendStatus friendStatus)
    {
        this.nick = nick;
        this.displayName = displayName;
        this.online = online;
        this.friendStatus = friendStatus;
    }

    public String getNick() { return this.nick; }
    public String getDisplayName() { return this.displayName; }
    public boolean isOnline() { return this.online; }
    public FriendStatus getFriendStatus() { return this.friendStatus; }
}
