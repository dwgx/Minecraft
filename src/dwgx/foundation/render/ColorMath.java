package dwgx.foundation.render;

import dwgx.foundation.math.ScalarMath;

/**
 * Shared ARGB blending utilities.
 */
public final class ColorMath
{
    private ColorMath()
    {
    }

    public static int mixArgb(int from, int to, float t)
    {
        float k = ScalarMath.clamp01(t);
        int a = lerp((from >>> 24) & 255, (to >>> 24) & 255, k);
        int r = lerp((from >>> 16) & 255, (to >>> 16) & 255, k);
        int g = lerp((from >>> 8) & 255, (to >>> 8) & 255, k);
        int b = lerp(from & 255, to & 255, k);
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    private static int lerp(int from, int to, float t)
    {
        int value = Math.round((float)from + (float)(to - from) * ScalarMath.clamp01(t));
        return Math.max(0, Math.min(255, value));
    }
}

