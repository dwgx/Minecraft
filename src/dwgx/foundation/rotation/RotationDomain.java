package dwgx.foundation.rotation;

/**
 * Shared rotation math policies for combat/movement modules.
 */
public final class RotationDomain
{
    public static final float MIN_PITCH = -89.9F;
    public static final float MAX_PITCH = 89.9F;

    private RotationDomain()
    {
    }

    public static float clampPitch(float pitch)
    {
        return Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
    }

    public static float yawDelta(float currentYaw, float targetYaw)
    {
        float delta = targetYaw - currentYaw;
        while (delta <= -180.0F)
        {
            delta += 360.0F;
        }

        while (delta > 180.0F)
        {
            delta -= 360.0F;
        }

        return delta;
    }

    public static float pitchDelta(float currentPitch, float targetPitch)
    {
        return targetPitch - currentPitch;
    }
}
