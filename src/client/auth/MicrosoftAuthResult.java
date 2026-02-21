package client.auth;

/**
 * Immutable result of a successful Microsoft -> Xbox -> Minecraft auth flow.
 */
public final class MicrosoftAuthResult
{
    private final String playerName;
    private final String playerUuid;
    private final String minecraftAccessToken;
    private final String refreshToken;

    public MicrosoftAuthResult(String playerName, String playerUuid, String minecraftAccessToken, String refreshToken)
    {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.minecraftAccessToken = minecraftAccessToken;
        this.refreshToken = refreshToken;
    }

    public String getPlayerName()
    {
        return this.playerName;
    }

    public String getPlayerUuid()
    {
        return this.playerUuid;
    }

    public String getMinecraftAccessToken()
    {
        return this.minecraftAccessToken;
    }

    public String getRefreshToken()
    {
        return this.refreshToken;
    }
}
