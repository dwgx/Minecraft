package client.auth;

import com.mojang.authlib.properties.PropertyMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

/**
 * Applies authenticated account data to the live Minecraft client.
 */
public final class MicrosoftSessionManager
{
    private MicrosoftSessionManager()
    {
    }

    public static void applyMicrosoftSession(Minecraft mc, MicrosoftAuthResult result) throws Exception
    {
        if (mc == null)
        {
            throw new IllegalArgumentException("Minecraft instance cannot be null.");
        }

        if (result == null)
        {
            throw new IllegalArgumentException("Auth result cannot be null.");
        }

        Session session = new Session(
            safe(result.getPlayerName(), "Player"),
            safe(result.getPlayerUuid(), UUID.randomUUID().toString().replace("-", "")),
            safe(result.getMinecraftAccessToken(), "0"),
            "mojang"
        );
        setSession(mc, session);
        clearProfileProperties(mc);
    }

    public static void applyOfflineSession(Minecraft mc, String preferredName) throws Exception
    {
        if (mc == null)
        {
            throw new IllegalArgumentException("Minecraft instance cannot be null.");
        }

        String name = safe(preferredName, "Player");

        Session session = new Session(
            name,
            UUID.randomUUID().toString().replace("-", ""),
            "0",
            "legacy"
        );
        setSession(mc, session);
        clearProfileProperties(mc);
    }

    private static void setSession(Minecraft mc, Session session) throws Exception
    {
        Field sessionField = Minecraft.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        removeFinalModifier(sessionField);
        sessionField.set(mc, session);
    }

    private static void clearProfileProperties(Minecraft mc)
    {
        try
        {
            Field profileField = Minecraft.class.getDeclaredField("profileProperties");
            profileField.setAccessible(true);
            Object map = profileField.get(mc);

            if (map instanceof PropertyMap)
            {
                ((PropertyMap)map).clear();
            }
        }
        catch (Exception ignored)
        {
            ;
        }
    }

    private static void removeFinalModifier(Field field)
    {
        try
        {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
        catch (Exception ignored)
        {
            ;
        }
    }

    private static String safe(String value, String fallback)
    {
        if (value == null || value.trim().isEmpty())
        {
            return fallback;
        }

        return value.trim();
    }
}
