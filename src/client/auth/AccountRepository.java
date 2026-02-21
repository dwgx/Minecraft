package client.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores multiple online/offline accounts for the account manager module.
 */
public final class AccountRepository
{
    public enum AccountType
    {
        MICROSOFT("microsoft"),
        OFFLINE("offline");

        private final String token;

        AccountType(String token)
        {
            this.token = token;
        }

        public String token()
        {
            return this.token;
        }

        public static AccountType fromToken(String token)
        {
            if (token == null)
            {
                return OFFLINE;
            }

            String normalized = token.trim().toLowerCase(Locale.ROOT);
            return "microsoft".equals(normalized) ? MICROSOFT : OFFLINE;
        }
    }

    public static final class AccountEntry
    {
        private String id;
        private AccountType type;
        private String name;
        private String uuid;
        private String refreshToken;
        private long createdAt;
        private long updatedAt;

        public String getId()
        {
            return this.id;
        }

        public AccountType getType()
        {
            return this.type;
        }

        public String getName()
        {
            return this.name;
        }

        public String getUuid()
        {
            return this.uuid;
        }

        public String getRefreshToken()
        {
            return this.refreshToken;
        }

        public long getCreatedAt()
        {
            return this.createdAt;
        }

        public long getUpdatedAt()
        {
            return this.updatedAt;
        }

        public boolean isMicrosoft()
        {
            return this.type == AccountType.MICROSOFT;
        }
    }

    private static final JsonParser JSON = new JsonParser();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    private final File file;
    private final File legacyFile;
    private final List<AccountEntry> accounts = new ArrayList<AccountEntry>();
    private String selectedId = "";

    public AccountRepository(File mcDataDir)
    {
        File clientRoot = new File(new File(mcDataDir, "config"), "client");
        this.file = new File(new File(clientRoot, "accounts"), "account_manager.json");
        this.legacyFile = new File(new File(clientRoot, "module"), "account_manager.json");
    }

    public synchronized void load()
    {
        this.accounts.clear();
        this.selectedId = "";

        File source = this.resolveReadableSource();

        if (source == null)
        {
            return;
        }

        boolean loadedSuccessfully = false;
        boolean fromLegacy = source.equals(this.legacyFile);

        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
            JsonObject root = JSON.parse(reader).getAsJsonObject();

            if (root == null)
            {
                return;
            }

            this.selectedId = getString(root, "selectedId");
            JsonArray array = asArray(root.get("accounts"));

            if (array != null)
            {
                for (int i = 0; i < array.size(); i++)
                {
                    JsonObject item = asObject(array.get(i));

                    if (item == null)
                    {
                        continue;
                    }

                    AccountEntry entry = new AccountEntry();
                    entry.id = nonEmpty(getString(item, "id"), UUID.randomUUID().toString());
                    entry.type = AccountType.fromToken(getString(item, "type"));
                    entry.name = sanitizeName(getString(item, "name"));

                    if (entry.name.isEmpty())
                    {
                        continue;
                    }

                    entry.uuid = normalizeUuid(getString(item, "uuid"));
                    entry.refreshToken = getString(item, "refreshToken");
                    entry.createdAt = safeLong(item.get("createdAt"));
                    entry.updatedAt = safeLong(item.get("updatedAt"));

                    if (entry.createdAt <= 0L)
                    {
                        entry.createdAt = System.currentTimeMillis();
                    }

                    if (entry.updatedAt <= 0L)
                    {
                        entry.updatedAt = entry.createdAt;
                    }

                    if (entry.type == AccountType.OFFLINE && entry.uuid.isEmpty())
                    {
                        entry.uuid = offlineUuid(entry.name);
                    }

                    this.accounts.add(copy(entry));
                }
            }

            loadedSuccessfully = true;
        }
        catch (Exception ignored)
        {
            this.accounts.clear();
            this.selectedId = "";
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {
                    ;
                }
            }
        }

        if (!this.selectedExists())
        {
            this.selectedId = this.accounts.isEmpty() ? "" : this.accounts.get(0).id;
        }

        if (fromLegacy && loadedSuccessfully)
        {
            this.save();
        }
    }

    public synchronized void save()
    {
        File parent = this.file.getParentFile();

        if (parent != null && !parent.isDirectory())
        {
            parent.mkdirs();
        }

        JsonObject root = new JsonObject();
        root.addProperty("selectedId", this.selectedId);
        JsonArray array = new JsonArray();

        for (int i = 0; i < this.accounts.size(); i++)
        {
            AccountEntry entry = this.accounts.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("id", entry.id);
            item.addProperty("type", entry.type.token());
            item.addProperty("name", entry.name);
            item.addProperty("uuid", entry.uuid);
            item.addProperty("refreshToken", entry.refreshToken);
            item.addProperty("createdAt", entry.createdAt);
            item.addProperty("updatedAt", entry.updatedAt);
            array.add(item);
        }

        root.add("accounts", array);

        OutputStreamWriter writer = null;

        try
        {
            writer = new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8);
            GSON.toJson(root, writer);
            writer.flush();
        }
        catch (IOException ignored)
        {
            ;
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException ignored)
                {
                    ;
                }
            }
        }
    }

    public synchronized List<AccountEntry> list()
    {
        List<AccountEntry> out = new ArrayList<AccountEntry>(this.accounts.size());

        for (int i = 0; i < this.accounts.size(); i++)
        {
            out.add(copy(this.accounts.get(i)));
        }

        return out;
    }

    public synchronized String getSelectedId()
    {
        return this.selectedId;
    }

    public synchronized void setSelectedId(String id)
    {
        String normalized = id == null ? "" : id.trim();
        this.selectedId = normalized;

        if (!this.selectedExists())
        {
            this.selectedId = this.accounts.isEmpty() ? "" : this.accounts.get(0).id;
        }
    }

    public synchronized AccountEntry addOffline(String rawName)
    {
        String name = sanitizeName(rawName);

        if (!isValidOfflineName(name))
        {
            throw new IllegalArgumentException("Offline name must be 1-16 chars: A-Z a-z 0-9 _");
        }

        for (int i = 0; i < this.accounts.size(); i++)
        {
            AccountEntry exists = this.accounts.get(i);

            if (exists.type == AccountType.OFFLINE && exists.name.equalsIgnoreCase(name))
            {
                this.selectedId = exists.id;
                exists.updatedAt = System.currentTimeMillis();
                return copy(exists);
            }
        }

        AccountEntry entry = new AccountEntry();
        entry.id = UUID.randomUUID().toString();
        entry.type = AccountType.OFFLINE;
        entry.name = name;
        entry.uuid = offlineUuid(name);
        entry.refreshToken = "";
        entry.createdAt = System.currentTimeMillis();
        entry.updatedAt = entry.createdAt;
        this.accounts.add(entry);
        this.selectedId = entry.id;
        return copy(entry);
    }

    public synchronized AccountEntry upsertMicrosoft(MicrosoftAuthResult result)
    {
        if (result == null)
        {
            throw new IllegalArgumentException("Auth result cannot be null.");
        }

        String uuid = normalizeUuid(result.getPlayerUuid());
        String name = sanitizeName(result.getPlayerName());
        String refresh = nonEmpty(result.getRefreshToken(), "");
        AccountEntry target = null;

        for (int i = 0; i < this.accounts.size(); i++)
        {
            AccountEntry candidate = this.accounts.get(i);

            if (candidate.type == AccountType.MICROSOFT && !uuid.isEmpty() && uuid.equalsIgnoreCase(candidate.uuid))
            {
                target = candidate;
                break;
            }
        }

        if (target == null)
        {
            for (int i = 0; i < this.accounts.size(); i++)
            {
                AccountEntry candidate = this.accounts.get(i);

                if (candidate.type == AccountType.MICROSOFT && candidate.name.equalsIgnoreCase(name))
                {
                    target = candidate;
                    break;
                }
            }
        }

        if (target == null)
        {
            target = new AccountEntry();
            target.id = UUID.randomUUID().toString();
            target.type = AccountType.MICROSOFT;
            target.createdAt = System.currentTimeMillis();
            this.accounts.add(target);
        }

        target.type = AccountType.MICROSOFT;
        target.name = nonEmpty(name, "Player");
        target.uuid = uuid;
        target.refreshToken = refresh;
        target.updatedAt = System.currentTimeMillis();
        this.selectedId = target.id;
        return copy(target);
    }

    public synchronized boolean removeById(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return false;
        }

        String key = id.trim();

        for (int i = 0; i < this.accounts.size(); i++)
        {
            if (key.equals(this.accounts.get(i).id))
            {
                this.accounts.remove(i);

                if (key.equals(this.selectedId))
                {
                    this.selectedId = this.accounts.isEmpty() ? "" : this.accounts.get(0).id;
                }

                return true;
            }
        }

        return false;
    }

    public synchronized void clear()
    {
        this.accounts.clear();
        this.selectedId = "";
    }

    public synchronized AccountEntry findById(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return null;
        }

        String key = id.trim();

        for (int i = 0; i < this.accounts.size(); i++)
        {
            AccountEntry entry = this.accounts.get(i);

            if (key.equals(entry.id))
            {
                return copy(entry);
            }
        }

        return null;
    }

    private File resolveReadableSource()
    {
        if (this.file.isFile())
        {
            return this.file;
        }

        if (this.legacyFile != null && this.legacyFile.isFile())
        {
            return this.legacyFile;
        }

        return null;
    }

    private boolean selectedExists()
    {
        if (this.selectedId == null || this.selectedId.trim().isEmpty())
        {
            return false;
        }

        for (int i = 0; i < this.accounts.size(); i++)
        {
            if (this.selectedId.equals(this.accounts.get(i).id))
            {
                return true;
            }
        }

        return false;
    }

    private static AccountEntry copy(AccountEntry entry)
    {
        AccountEntry out = new AccountEntry();
        out.id = entry.id;
        out.type = entry.type;
        out.name = entry.name;
        out.uuid = entry.uuid;
        out.refreshToken = entry.refreshToken;
        out.createdAt = entry.createdAt;
        out.updatedAt = entry.updatedAt;
        return out;
    }

    private static boolean isValidOfflineName(String name)
    {
        return name != null && name.matches("^[A-Za-z0-9_]{1,16}$");
    }

    private static String offlineUuid(String name)
    {
        String value = nonEmpty(name, "Player");
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + value).getBytes(StandardCharsets.UTF_8));
        return uuid.toString().replace("-", "");
    }

    private static String sanitizeName(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        return raw.trim();
    }

    private static String normalizeUuid(String uuid)
    {
        if (uuid == null)
        {
            return "";
        }

        return uuid.trim().replace("-", "");
    }

    private static String nonEmpty(String value, String fallback)
    {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static JsonObject asObject(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray asArray(JsonElement element)
    {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key)
    {
        if (object == null || key == null || !object.has(key))
        {
            return "";
        }

        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static long safeLong(JsonElement element)
    {
        if (element == null || element.isJsonNull())
        {
            return 0L;
        }

        try
        {
            return element.getAsLong();
        }
        catch (RuntimeException ignored)
        {
            return 0L;
        }
    }
}
