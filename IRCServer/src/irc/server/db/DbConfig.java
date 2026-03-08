package irc.server.db;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Database configuration loaded from config/db.json.
 */
public final class DbConfig
{
    private String host = "127.0.0.1";
    private int port = 3306;
    private String database = "irc_server";
    private String username = "root";
    private String password = "";
    private int poolSize = 5;

    public String getHost() { return this.host; }
    public int getPort() { return this.port; }
    public String getDatabase() { return this.database; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
    public int getPoolSize() { return this.poolSize; }

    public String getJdbcUrl()
    {
        return "jdbc:mariadb://" + this.host + ":" + this.port + "/" + this.database
                + "?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true";
    }

    public static DbConfig load(String path)
    {
        DbConfig cfg = new DbConfig();
        Path p = Paths.get(path);
        if (!Files.exists(p)) return cfg;
        try
        {
            byte[] bytes = Files.readAllBytes(p);
            String json = new String(bytes, StandardCharsets.UTF_8);
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            if (obj.has("host")) cfg.host = obj.get("host").getAsString();
            if (obj.has("port")) cfg.port = obj.get("port").getAsInt();
            if (obj.has("database")) cfg.database = obj.get("database").getAsString();
            if (obj.has("username")) cfg.username = obj.get("username").getAsString();
            if (obj.has("password")) cfg.password = obj.get("password").getAsString();
            if (obj.has("poolSize")) cfg.poolSize = obj.get("poolSize").getAsInt();
        }
        catch (IOException e)
        {
            System.err.println("[DbConfig] Failed to load " + path + ": " + e.getMessage());
        }
        return cfg;
    }
}
