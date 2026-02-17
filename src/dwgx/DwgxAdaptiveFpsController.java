package dwgx;

import net.minecraft.client.settings.GameSettings;

/**
 * Runtime FPS controller that trades quality for frame stability.
 * Enable with -Dlwjgl3.autoTuneFps=true or --autoTuneFps.
 */
public final class DwgxAdaptiveFpsController
{
    private static boolean initialized;
    private static boolean enabled;
    private static int targetFps;
    private static int minRenderDistance;
    private static int maxRenderDistance;
    private static int lowFpsStreak;
    private static int highFpsStreak;
    private static long lastAdjustMillis;

    private DwgxAdaptiveFpsController()
    {
    }

    public static void onFpsSample(GameSettings settings, int fps)
    {
        if (settings == null || fps <= 0)
        {
            return;
        }

        if (!initialized)
        {
            initialize(settings);
        }

        if (!enabled)
        {
            return;
        }

        if (fps < targetFps - 15)
        {
            ++lowFpsStreak;
            highFpsStreak = 0;
        }
        else if (fps > targetFps + 25)
        {
            ++highFpsStreak;
            lowFpsStreak = 0;
        }
        else
        {
            lowFpsStreak = 0;
            highFpsStreak = 0;
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastAdjustMillis < 2500L)
        {
            return;
        }

        if (lowFpsStreak >= 3 && degrade(settings))
        {
            lastAdjustMillis = now;
            lowFpsStreak = 0;
            System.err.println("[DWGX-FPS] Lowered quality for FPS stability (fps=" + fps + ", target=" + targetFps + ")");
            return;
        }

        if (highFpsStreak >= 8 && recover(settings))
        {
            lastAdjustMillis = now;
            highFpsStreak = 0;
            System.err.println("[DWGX-FPS] Restored quality step (fps=" + fps + ", target=" + targetFps + ")");
        }
    }

    private static void initialize(GameSettings settings)
    {
        initialized = true;
        enabled = Boolean.getBoolean("lwjgl3.autoTuneFps");

        if (!enabled)
        {
            return;
        }

        targetFps = clamp(readIntProperty("lwjgl3.autoTuneFpsTarget", 120), 45, 300);
        minRenderDistance = clamp(readIntProperty("lwjgl3.autoTuneMinRenderDistance", 4), 2, 10);
        maxRenderDistance = clamp(settings.renderDistanceChunks, minRenderDistance, 16);
    }

    private static boolean degrade(GameSettings settings)
    {
        if (settings.renderDistanceChunks > minRenderDistance)
        {
            --settings.renderDistanceChunks;
            settings.sendSettingsToServer();
            return true;
        }

        if (settings.particleSetting < 2)
        {
            ++settings.particleSetting;
            return true;
        }

        if (settings.entityShadows)
        {
            settings.entityShadows = false;
            return true;
        }

        if (settings.clouds != 0)
        {
            settings.clouds = 0;
            return true;
        }

        if (settings.ambientOcclusion != 0)
        {
            settings.ambientOcclusion = 0;
            return true;
        }

        return false;
    }

    private static boolean recover(GameSettings settings)
    {
        if (settings.renderDistanceChunks < maxRenderDistance)
        {
            ++settings.renderDistanceChunks;
            settings.sendSettingsToServer();
            return true;
        }

        if (settings.particleSetting > 1)
        {
            --settings.particleSetting;
            return true;
        }

        return false;
    }

    private static int readIntProperty(String key, int defaultValue)
    {
        String raw = System.getProperty(key);

        if (raw == null)
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException var3)
        {
            return defaultValue;
        }
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
