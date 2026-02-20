package dwgx.foundation.math;

/**
 * Shared scalar math helpers used across modules and UI.
 */
public final class ScalarMath
{
    private ScalarMath()
    {
    }

    public static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp01(float value)
    {
        return clamp(value, 0.0F, 1.0F);
    }

    public static float lerp(float from, float to, float t)
    {
        return from + (to - from) * clamp01(t);
    }
}

