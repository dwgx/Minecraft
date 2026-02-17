package client.core;

/**
 * Runtime identity values shown in UI/logs.
 */
public final class ClientInfo
{
    private final String id;
    private final String name;
    private final String version;

    public ClientInfo(String id, String name, String version)
    {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getVersion()
    {
        return this.version;
    }

    public static ClientInfo defaults()
    {
        return new ClientInfo("minecraft", BuildInfo.CLIENT_NAME, BuildInfo.CLIENT_VERSION);
    }
}
