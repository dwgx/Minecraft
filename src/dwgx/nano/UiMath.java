package dwgx.nano;

/**
 * Small UI-oriented math helpers to keep NanoVG code tidy.
 */
public final class UiMath
{
    private UiMath()
    {
    }

    public static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }

    public static float saturate(float value)
    {
        return clamp(value, 0.0F, 1.0F);
    }

    public static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    /**
     * Critically damped smoothing. rate is in 1/seconds.
     */
    public static float damp(float current, float target, float rate, float deltaSeconds)
    {
        float k = clamp(rate * deltaSeconds, 0.0F, 1.0F);
        return current + (target - current) * k;
    }

    public static boolean contains(float x, float y, float rx, float ry, float rw, float rh)
    {
        return x >= rx && y >= ry && x <= rx + rw && y <= ry + rh;
    }
}
