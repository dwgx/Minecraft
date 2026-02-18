package client.ui.template;

/**
 * Shared animation helpers used by Nano UI screens and window templates.
 */
public final class UiAnimation
{
    public enum Type
    {
        LINEAR,
        EASE_OUT,
        EASE_IN_OUT,
        SPRING;

        public String displayName()
        {
            switch (this)
            {
                case LINEAR:
                    return "Linear";
                case EASE_IN_OUT:
                    return "EaseInOut";
                case SPRING:
                    return "Spring";
                case EASE_OUT:
                default:
                    return "EaseOut";
            }
        }
    }

    private UiAnimation()
    {
    }

    public static float responseFromSpeed(float speed, float smooth, Type type, boolean interacting)
    {
        float s = UiMotion.clamp01(speed);
        float smoothClamped = UiMotion.clamp01(smooth);
        float response = UiMotion.responseFromSpeed(s);
        response *= 0.65F + smoothClamped * 1.65F;

        Type mode = type == null ? Type.EASE_OUT : type;

        switch (mode)
        {
            case LINEAR:
                response *= 0.86F;
                break;
            case EASE_IN_OUT:
                response *= 1.06F;
                break;
            case SPRING:
                response *= 1.24F;
                break;
            case EASE_OUT:
            default:
                break;
        }

        if (interacting)
        {
            response *= 1.20F;
        }

        return UiMotion.clamp(response, 2.0F, 180.0F);
    }

    public static float step(float current, float target, float response, float deltaSeconds, Type type, float smooth)
    {
        float dt = UiMotion.clampDeltaSeconds(deltaSeconds);
        float k = Math.max(0.0001F, response);
        float alpha = 1.0F - (float)Math.exp((double)(-k * dt));
        alpha = applyType(alpha, type, smooth);
        float next = current + (target - current) * alpha;
        float epsilon = 0.03F + (1.0F - UiMotion.clamp01(smooth)) * 0.03F;
        return UiMotion.snapIfClose(next, target, epsilon);
    }

    private static float applyType(float alpha, Type type, float smooth)
    {
        float t = UiMotion.clamp01(alpha);
        float smoothClamped = UiMotion.clamp01(smooth);
        Type mode = type == null ? Type.EASE_OUT : type;

        switch (mode)
        {
            case LINEAR:
                return t;
            case EASE_IN_OUT:
                return smoothStep(t, 1.0F + smoothClamped * 0.75F);
            case SPRING:
                float wave = (float)Math.sin((double)(t * 3.1415927F));
                float extra = wave * (0.08F + smoothClamped * 0.20F) * (1.0F - t);
                return UiMotion.clamp(t + extra, 0.0F, 1.16F);
            case EASE_OUT:
            default:
                float power = 2.0F + smoothClamped * 1.6F;
                return 1.0F - (float)Math.pow((double)(1.0F - t), (double)power);
        }
    }

    private static float smoothStep(float t, float power)
    {
        float clamped = UiMotion.clamp01(t);

        if (clamped <= 0.5F)
        {
            return 0.5F * (float)Math.pow((double)(clamped * 2.0F), (double)power);
        }

        return 1.0F - 0.5F * (float)Math.pow((double)((1.0F - clamped) * 2.0F), (double)power);
    }
}
