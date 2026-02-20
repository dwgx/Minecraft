package dwgx.foundation.motion;

import dwgx.foundation.math.ScalarMath;

/**
 * Shared motion interpolation helpers.
 */
public final class MotionDomain
{
    private MotionDomain()
    {
    }

    public static float approach(float current, float target, float speed)
    {
        return current + (target - current) * ScalarMath.clamp01(speed);
    }
}

