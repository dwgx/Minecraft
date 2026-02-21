package client.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

/**
 * Client-side local player skin/cape override service.
 *
 * It resolves official Minecraft profile textures by player name and applies
 * them as local texture overrides without changing server-side identity.
 */
public final class SkinCapeOverrideService
{
    private static final JsonParser JSON = new JsonParser();
    private static final Object LOCK = new Object();
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "DWGX-Minecraft-SkinOverride/1.0";

    private static volatile ResourceLocation skinLocation;
    private static volatile ResourceLocation capeLocation;
    private static volatile String skinType = "default";
    private static volatile String requestedName = "";
    private static volatile String appliedName = "";
    private static volatile String lastError = "";
    private static volatile boolean loading;
    private static volatile int requestSerial;

    private SkinCapeOverrideService()
    {
    }

    public static void requestApplyByOfficialName(String rawName)
    {
        final String name = sanitizeName(rawName);

        if (name.isEmpty())
        {
            clear();
            return;
        }

        final int serial;

        synchronized (LOCK)
        {
            if (loading && name.equalsIgnoreCase(requestedName))
            {
                return;
            }

            if (!loading && name.equalsIgnoreCase(appliedName) && skinLocation != null)
            {
                return;
            }

            requestedName = name;
            loading = true;
            lastError = "";
            serial = ++requestSerial;
        }

        Thread worker = new Thread("SkinCapeOverride-" + name)
        {
            public void run()
            {
                try
                {
                    ResolvedTextures resolved = resolveTexturesByName(name);
                    applyResolved(serial, resolved);
                }
                catch (Exception ex)
                {
                    fail(serial, ex.getMessage());
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    public static void clear()
    {
        synchronized (LOCK)
        {
            ++requestSerial;
            requestedName = "";
            appliedName = "";
            skinLocation = null;
            capeLocation = null;
            skinType = "default";
            lastError = "";
            loading = false;
        }
    }

    public static boolean isLoading()
    {
        return loading;
    }

    public static String getAppliedName()
    {
        return appliedName;
    }

    public static String getLastError()
    {
        return lastError;
    }

    public static ResourceLocation getOverrideSkinForLocalPlayer(AbstractClientPlayer player)
    {
        return isLocalPlayer(player) ? skinLocation : null;
    }

    public static ResourceLocation getOverrideCapeForLocalPlayer(AbstractClientPlayer player)
    {
        return isLocalPlayer(player) ? capeLocation : null;
    }

    public static String getOverrideSkinTypeForLocalPlayer(AbstractClientPlayer player)
    {
        if (!isLocalPlayer(player) || skinLocation == null)
        {
            return null;
        }

        return "slim".equalsIgnoreCase(skinType) ? "slim" : "default";
    }

    private static void applyResolved(final int serial, final ResolvedTextures resolved)
    {
        final Minecraft mc = Minecraft.getMinecraft();

        if (mc == null)
        {
            fail(serial, "Minecraft instance unavailable.");
            return;
        }

        Runnable applyTask = new Runnable()
        {
            public void run()
            {
                if (serial != requestSerial)
                {
                    return;
                }

                TextureManager textureManager = mc.getTextureManager();

                if (textureManager == null)
                {
                    fail(serial, "Texture manager unavailable.");
                    return;
                }

                UUID fallbackUuid = mc.thePlayer == null ? UUID.randomUUID() : mc.thePlayer.getUniqueID();
                ResourceLocation fallbackSkin = DefaultPlayerSkin.getDefaultSkin(fallbackUuid);
                ResourceLocation newSkinLocation = buildTextureLocation("skin", resolved.playerName, resolved.skinUrl);
                ThreadDownloadImageData skinTexture = new ThreadDownloadImageData((File)null, resolved.skinUrl, fallbackSkin, new ImageBufferDownload());
                textureManager.loadTexture(newSkinLocation, skinTexture);

                ResourceLocation newCapeLocation = null;

                if (!isEmpty(resolved.capeUrl))
                {
                    newCapeLocation = buildTextureLocation("cape", resolved.playerName, resolved.capeUrl);
                    ThreadDownloadImageData capeTexture = new ThreadDownloadImageData((File)null, resolved.capeUrl, null, null);
                    textureManager.loadTexture(newCapeLocation, capeTexture);
                }

                synchronized (LOCK)
                {
                    if (serial != requestSerial)
                    {
                        return;
                    }

                    skinLocation = newSkinLocation;
                    capeLocation = newCapeLocation;
                    skinType = resolved.skinType;
                    appliedName = resolved.playerName;
                    requestedName = resolved.playerName;
                    lastError = "";
                    loading = false;
                }
            }
        };

        if (mc.isCallingFromMinecraftThread())
        {
            applyTask.run();
        }
        else
        {
            mc.addScheduledTask(applyTask);
        }
    }

    private static void fail(int serial, String message)
    {
        synchronized (LOCK)
        {
            if (serial != requestSerial)
            {
                return;
            }

            skinLocation = null;
            capeLocation = null;
            skinType = "default";
            appliedName = "";
            loading = false;
            lastError = isEmpty(message) ? "Unknown error." : message;
        }
    }

    private static ResolvedTextures resolveTexturesByName(String name) throws Exception
    {
        JsonObject profile = getJson("https://api.mojang.com/users/profiles/minecraft/" + urlEncode(name), true);

        if (profile == null)
        {
            throw new Exception("Official profile not found: " + name);
        }

        String uuid = getString(profile, "id");
        String resolvedName = firstNonEmpty(getString(profile, "name"), name);

        if (isEmpty(uuid))
        {
            throw new Exception("Profile lookup failed: missing UUID.");
        }

        JsonObject session = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false", false);
        String texturesValue = extractTexturesProperty(session);

        if (isEmpty(texturesValue))
        {
            throw new Exception("Profile textures missing.");
        }

        String decoded = new String(Base64.getDecoder().decode(texturesValue), StandardCharsets.UTF_8);
        JsonObject texturesRoot = parseJsonObject(decoded);

        if (texturesRoot == null)
        {
            throw new Exception("Invalid textures payload.");
        }

        JsonObject textures = asObject(texturesRoot.get("textures"));

        if (textures == null)
        {
            throw new Exception("Textures object missing.");
        }

        JsonObject skin = asObject(textures.get("SKIN"));
        String skinUrl = skin == null ? "" : getString(skin, "url");

        if (isEmpty(skinUrl))
        {
            throw new Exception("Skin URL missing.");
        }

        String model = "default";
        JsonObject metadata = skin == null ? null : asObject(skin.get("metadata"));

        if (metadata != null)
        {
            String declaredModel = getString(metadata, "model");

            if ("slim".equalsIgnoreCase(declaredModel))
            {
                model = "slim";
            }
        }

        JsonObject cape = asObject(textures.get("CAPE"));
        String capeUrl = cape == null ? "" : getString(cape, "url");
        return new ResolvedTextures(resolvedName, skinUrl, capeUrl, model);
    }

    private static JsonObject getJson(String endpoint, boolean allowNotFound) throws IOException
    {
        HttpURLConnection connection = null;

        try
        {
            connection = (HttpURLConnection)(new URL(endpoint)).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();

            if (allowNotFound && (status == 204 || status == 404))
            {
                return null;
            }

            InputStream stream = status >= 200 && status < 400 ? connection.getInputStream() : connection.getErrorStream();
            String payload = readAll(stream);

            if (status < 200 || status >= 300)
            {
                throw new IOException("HTTP " + status + ": " + payload);
            }

            JsonObject json = parseJsonObject(payload);

            if (json == null)
            {
                throw new IOException("Invalid JSON from " + endpoint);
            }

            return json;
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private static String extractTexturesProperty(JsonObject session)
    {
        if (session == null)
        {
            return "";
        }

        JsonArray properties = asArray(session.get("properties"));

        if (properties == null)
        {
            return "";
        }

        for (int i = 0; i < properties.size(); ++i)
        {
            JsonObject entry = asObject(properties.get(i));

            if (entry == null)
            {
                continue;
            }

            if ("textures".equalsIgnoreCase(getString(entry, "name")))
            {
                return getString(entry, "value");
            }
        }

        return "";
    }

    private static boolean isLocalPlayer(AbstractClientPlayer player)
    {
        if (player == null || skinLocation == null)
        {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.thePlayer != null && player.getUniqueID().equals(mc.thePlayer.getUniqueID());
    }

    private static ResourceLocation buildTextureLocation(String type, String playerName, String url)
    {
        String name = sanitizePath(playerName);
        String hash = Integer.toHexString(url == null ? 0 : url.hashCode());
        return new ResourceLocation("client/override/" + type + "/" + name + "/" + hash);
    }

    private static String sanitizeName(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String input = raw.trim();

        if (input.isEmpty())
        {
            return "";
        }

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < input.length() && out.length() < 16; ++i)
        {
            char c = input.charAt(i);

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')
            {
                out.append(c);
            }
        }

        return out.toString();
    }

    private static String sanitizePath(String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return "player";
        }

        String lower = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < lower.length(); ++i)
        {
            char c = lower.charAt(i);

            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-')
            {
                out.append(c);
            }
        }

        return out.length() == 0 ? "player" : out.toString();
    }

    private static String urlEncode(String value) throws Exception
    {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private static JsonObject parseJsonObject(String payload)
    {
        if (payload == null || payload.trim().isEmpty())
        {
            return null;
        }

        try
        {
            JsonElement element = JSON.parse(payload);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
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

        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static String readAll(InputStream stream) throws IOException
    {
        if (stream == null)
        {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        try
        {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[2048];
            int read;

            while ((read = reader.read(buffer)) >= 0)
            {
                out.append(buffer, 0, read);
            }

            return out.toString();
        }
        finally
        {
            reader.close();
        }
    }

    private static String firstNonEmpty(String a, String b)
    {
        return isEmpty(a) ? (isEmpty(b) ? "" : b) : a;
    }

    private static boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private static final class ResolvedTextures
    {
        private final String playerName;
        private final String skinUrl;
        private final String capeUrl;
        private final String skinType;

        private ResolvedTextures(String playerName, String skinUrl, String capeUrl, String skinType)
        {
            this.playerName = playerName;
            this.skinUrl = skinUrl;
            this.capeUrl = capeUrl;
            this.skinType = skinType;
        }
    }
}
