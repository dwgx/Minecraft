package dwgx.foundation.animation;

import dwgx.foundation.math.ScalarMath;

/**
 * Shared animation tuning formulas used by UI and overlays.
 */
public final class AnimationDomain
{
    private AnimationDomain()
    {
    }

    public static float composeWindowSpeed(float profileSpeed, float globalSpeed)
    {
        float profile = ScalarMath.clamp(profileSpeed, 0.05F, 1.0F);
        float global = ScalarMath.clamp(globalSpeed, 0.05F, 1.0F);
        return ScalarMath.clamp(profile * (0.55F + global), 0.05F, 1.0F);
    }

    public static float boostInteractionSpeed(float speed, boolean interacting)
    {
        if (!interacting)
        {
            return ScalarMath.clamp(speed, 0.05F, 1.0F);
        }

        return ScalarMath.clamp(speed * 1.22F + 0.06F, 0.05F, 1.0F);
    }

    public static float scrollSpeed(float controlSpeed)
    {
        return ScalarMath.clamp(controlSpeed * 0.95F + 0.10F, 0.05F, 1.0F);
    }
}

