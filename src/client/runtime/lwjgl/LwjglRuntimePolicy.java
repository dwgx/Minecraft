package client.runtime.lwjgl;

/**
 * Central policy holder for LWJGL compatibility flags.
 */
public final class LwjglRuntimePolicy
{
    private LwjglRuntimePolicy()
    {
    }

    public static String requiredVersion()
    {
        return "3.x-compat";
    }

    public static boolean allowLegacyInputPath()
    {
        return true;
    }
}

