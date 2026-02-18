package client.ui.template;

public final class UiMotion
{
    private static final float MIN_DELTA_SECONDS = 0.001F;
    private static final float MAX_DELTA_SECONDS = 0.050F;

    private UiMotion()
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

    public static float approach(float current, float target, float speed)
    {
        float t = clamp(speed, 0.0F, 1.0F);
        return current + (target - current) * t;
    }

    public static float clampDeltaSeconds(float deltaSeconds)
    {
        if (Float.isNaN(deltaSeconds) || Float.isInfinite(deltaSeconds))
        {
            return 1.0F / 60.0F;
        }

        return clamp(deltaSeconds, MIN_DELTA_SECONDS, MAX_DELTA_SECONDS);
    }

    public static float responseFromSpeed(float speed)
    {
        float t = clamp01(speed);
        return 4.0F + (float)Math.pow(t, 0.85D) * 34.0F;
    }

    public static float expSmoothing(float current, float target, float response, float deltaSeconds)
    {
        float dt = clampDeltaSeconds(deltaSeconds);
        float k = Math.max(0.0001F, response);
        float alpha = 1.0F - (float)Math.exp((double)(-k * dt));
        return current + (target - current) * alpha;
    }

    public static float snapIfClose(float current, float target, float epsilon)
    {
        return Math.abs(target - current) <= Math.max(0.0F, epsilon) ? target : current;
    }

    public static float roundToStep(float value, float step)
    {
        if (step <= 0.0F)
        {
            return value;
        }

        return Math.round(value / step) * step;
    }
}
