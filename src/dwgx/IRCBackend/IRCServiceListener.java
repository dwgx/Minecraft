package dwgx.IRCBackend;

/**
 * Callback interface for IRC service replies (friend, profile, mail, social).
 * All callbacks are invoked on the Netty I/O thread.
 */
public interface IRCServiceListener
{
    /** Generic service reply received. */
    void onServiceReply(String command, String jsonPayload);

    /** Friend request notification from another user. */
    void onFriendRequestNotify(String fromNick);

    /** Friend accept notification. */
    void onFriendAcceptNotify(String nick);

    /** New mail notification. */
    void onMailNotify(String fromNick, String subject, long mailId);
}
