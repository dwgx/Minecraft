package dwgx.IRCBackend;

/**
 * IRC backend configuration. Holds server address, port, credentials, and protocol settings.
 */
public final class IRCConfig
{
    private String serverHost = "127.0.0.1";
    private int serverPort = 1378;
    private boolean useTls = false;
    private String nickname = "Player";
    private String username = "minecraft";
    private String realName = "Minecraft Client";
    private String serverPassword;
    private int reconnectDelayMs = 1000;
    private int maxReconnectAttempts = 5;
    private int pingIntervalMs = 30000;
    private int timeoutMs = 3000;

    public String getServerHost() { return this.serverHost; }
    public void setServerHost(String host) { this.serverHost = host; }
    public int getServerPort() { return this.serverPort; }
    public void setServerPort(int port) { this.serverPort = port; }
    public boolean isUseTls() { return this.useTls; }
    public void setUseTls(boolean tls) { this.useTls = tls; }
    public String getNickname() { return this.nickname; }
    public void setNickname(String nick) { this.nickname = nick; }
    public String getUsername() { return this.username; }
    public void setUsername(String user) { this.username = user; }
    public String getRealName() { return this.realName; }
    public void setRealName(String name) { this.realName = name; }
    public String getServerPassword() { return this.serverPassword; }
    public void setServerPassword(String pass) { this.serverPassword = pass; }
    public int getReconnectDelayMs() { return this.reconnectDelayMs; }
    public void setReconnectDelayMs(int ms) { this.reconnectDelayMs = ms; }
    public int getMaxReconnectAttempts() { return this.maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int max) { this.maxReconnectAttempts = max; }
    public int getPingIntervalMs() { return this.pingIntervalMs; }
    public void setPingIntervalMs(int ms) { this.pingIntervalMs = ms; }
    public int getTimeoutMs() { return this.timeoutMs; }
    public void setTimeoutMs(int ms) { this.timeoutMs = ms; }
}
