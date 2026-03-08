package irc.server;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-based persistence for IRC user data: nicknames, friends, joined channels.
 * Data stored in config/irc_users.json by default.
 * Thread-safe: all maps are concurrent.
 */
public final class IRCUserStore
{
    private static final String DEFAULT_PATH = "config/irc_users.json";

    private final Path filePath;
    /** nick (lowercase) -> UserData */
    private final Map<String, UserData> users = new ConcurrentHashMap<String, UserData>();

    public IRCUserStore() { this(DEFAULT_PATH); }
    public IRCUserStore(String path) { this.filePath = Paths.get(path); }

    /** Ensure a user entry exists. */
    public void ensureUser(String nick)
    {
        if (nick == null) return;
        String key = nick.toLowerCase();
        if (!this.users.containsKey(key))
        {
            this.users.put(key, new UserData(nick));
        }
    }

    // --- Friends ---

    public void addFriend(String nick, String friendNick)
    {
        UserData data = getOrCreate(nick);
        data.friends.add(friendNick.toLowerCase());
    }

    public void removeFriend(String nick, String friendNick)
    {
        UserData data = this.users.get(nick.toLowerCase());
        if (data != null) data.friends.remove(friendNick.toLowerCase());
    }

    public Set<String> getFriends(String nick)
    {
        UserData data = this.users.get(nick.toLowerCase());
        return data != null ? Collections.unmodifiableSet(data.friends) : Collections.<String>emptySet();
    }

    public boolean isFriend(String nick, String other)
    {
        UserData data = this.users.get(nick.toLowerCase());
        return data != null && data.friends.contains(other.toLowerCase());
    }

    // --- Channels ---

    public void addChannel(String nick, String channel)
    {
        UserData data = getOrCreate(nick);
        data.channels.add(channel.toLowerCase());
    }

    public void removeChannel(String nick, String channel)
    {
        UserData data = this.users.get(nick.toLowerCase());
        if (data != null) data.channels.remove(channel.toLowerCase());
    }

    public Set<String> getChannels(String nick)
    {
        UserData data = this.users.get(nick.toLowerCase());
        return data != null ? Collections.unmodifiableSet(data.channels) : Collections.<String>emptySet();
    }

    // --- Display nick ---

    public String getDisplayNick(String nick)
    {
        UserData data = this.users.get(nick.toLowerCase());
        return data != null ? data.displayNick : nick;
    }

    public void setDisplayNick(String nick, String displayNick)
    {
        UserData data = getOrCreate(nick);
        data.displayNick = displayNick;
    }

    // --- Persistence ---

    /** Load user data from JSON file. Safe to call if file doesn't exist yet. */
    public void load()
    {
        if (!Files.exists(this.filePath)) return;
        try
        {
            byte[] bytes = Files.readAllBytes(this.filePath);
            String json = new String(bytes, StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet())
            {
                String key = entry.getKey();
                JsonObject obj = entry.getValue().getAsJsonObject();

                String displayNick = obj.has("displayNick") ? obj.get("displayNick").getAsString() : key;
                UserData data = new UserData(displayNick);

                if (obj.has("friends"))
                {
                    for (JsonElement e : obj.getAsJsonArray("friends"))
                    {
                        data.friends.add(e.getAsString());
                    }
                }
                if (obj.has("channels"))
                {
                    for (JsonElement e : obj.getAsJsonArray("channels"))
                    {
                        data.channels.add(e.getAsString());
                    }
                }
                this.users.put(key, data);
            }
        }
        catch (Exception e)
        {
            System.err.println("[IRCUserStore] Failed to load " + this.filePath + ": " + e.getMessage());
        }
    }
/* APPEND_USERSTORE_SAVE */

    /** Save all user data to JSON file. Creates parent directories if needed. */
    public void save()
    {
        try
        {
            Path parent = this.filePath.getParent();
            if (parent != null && !Files.exists(parent))
            {
                Files.createDirectories(parent);
            }

            JsonObject root = new JsonObject();
            for (Map.Entry<String, UserData> entry : this.users.entrySet())
            {
                UserData data = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("displayNick", data.displayNick);

                JsonArray friendsArr = new JsonArray();
                for (String f : data.friends) { friendsArr.add(new JsonPrimitive(f)); }
                obj.add("friends", friendsArr);

                JsonArray channelsArr = new JsonArray();
                for (String c : data.channels) { channelsArr.add(new JsonPrimitive(c)); }
                obj.add("channels", channelsArr);

                root.add(entry.getKey(), obj);
            }

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);

            Path tmp = this.filePath.resolveSibling(this.filePath.getFileName() + ".tmp");
            Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
            try
            {
                Files.move(tmp, this.filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException e2)
            {
                Files.move(tmp, this.filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e)
        {
            System.err.println("[IRCUserStore] Failed to save " + this.filePath + ": " + e.getMessage());
        }
    }

    // --- Internal ---

    private UserData getOrCreate(String nick)
    {
        String key = nick.toLowerCase();
        UserData data = this.users.get(key);
        if (data == null)
        {
            data = new UserData(nick);
            this.users.put(key, data);
        }
        return data;
    }

    /** Per-user persistent data. */
    static final class UserData
    {
        String displayNick;
        final Set<String> friends = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        final Set<String> channels = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        UserData(String displayNick) { this.displayNick = displayNick; }
    }
}
