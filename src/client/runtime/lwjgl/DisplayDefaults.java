package client.runtime.lwjgl;

/**
 * Default display dimensions shared between Main, Display shim, and any
 * other code that needs the canonical initial window size.
 */
public final class DisplayDefaults
{
    public static final int WIDTH = 854;
    public static final int HEIGHT = 480;

    private DisplayDefaults()
    {
    }
}
