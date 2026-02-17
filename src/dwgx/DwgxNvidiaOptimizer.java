package dwgx;

import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import java.util.Locale;

/**
 * Conservative GPU-side fast-path hints.
 * Focuses on throughput, keeps compatibility-first behavior.
 */
public final class DwgxNvidiaOptimizer
{
    private static final int GL_MULTISAMPLE = 32925;
    private static final int GL_TEXTURE_COMPRESSION_HINT = 34031;
    private static final int GL_MULTISAMPLE_FILTER_HINT_NV = 34100;
    private static boolean applied;

    private DwgxNvidiaOptimizer()
    {
    }

    public static void applyOnce(GameSettings settings)
    {
        if (applied)
        {
            return;
        }

        applied = true;

        try
        {
            String vendor = GL11.glGetString(GL11.GL_VENDOR);
            boolean isNvidia = vendor != null && vendor.toLowerCase(Locale.ROOT).contains("nvidia");
            boolean aggressive = Boolean.getBoolean("lwjgl3.aggressiveOptimize");
            GLCapabilities caps = GL.getCapabilities();

            // Baseline fast hints that are generally safe on all vendors.
            GL11.glDisable(GL11.GL_DITHER);
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
            GL11.glHint(GL_TEXTURE_COMPRESSION_HINT, GL11.GL_FASTEST);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            if (aggressive)
            {
                GL11.glDisable(GL11.GL_POINT_SMOOTH);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
                GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);
                GL11.glDisable(GL_MULTISAMPLE);
            }

            if (isNvidia)
            {
                // Keep a predictable depth clear path on NV drivers.
                GL11.glClearDepth(1.0D);

                if (caps != null && caps.GL_NV_multisample_filter_hint)
                {
                    GL11.glHint(GL_MULTISAMPLE_FILTER_HINT_NV, GL11.GL_FASTEST);
                }
            }

            if (settings != null && aggressive)
            {
                settings.enableVsync = false;
            }
        }
        catch (Throwable ignored)
        {
        }
    }
}
