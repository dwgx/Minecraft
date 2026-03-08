package dwgx.IRCBackend;

/**
 * Callback interface for IRC events. All callbacks are invoked on the Netty I/O thread.
 * Implementations must be thread-safe or dispatch to the main thread.
 */
public interface IRCEventListener
{
    /** Connection established and registered (001 RPL_WELCOME received). */
    void onConnected();

    /** Connection lost or closed. */
    void onDisconnected(String reason);

    /** PRIVMSG received (channel or DM). */
    void onMessage(String sender, String target, String text);

    /** NOTICE received. */
    void onNotice(String sender, String target, String text);

    /** User joined a channel. */
    void onJoin(String nick, String channel);

    /** User left a channel. */
    void onPart(String nick, String channel, String reason);

    /** User quit the server. */
    void onQuit(String nick, String reason);

    /** User changed nickname. */
    void onNickChange(String oldNick, String newNick);

    /** Channel topic changed. */
    void onTopic(String channel, String topic, String setter);

    /** Numeric reply from server (e.g., 353 NAMES, 332 TOPIC). */
    void onNumeric(int code, String[] params, String trailing);

    /** Raw unhandled message. */
    void onRaw(IRCMessage message);

    /** Error from server (ERROR command or numeric 4xx/5xx). */
    void onError(String message);
}
