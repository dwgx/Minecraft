package client.ui.template;

/**
 * Shared slider motion policy for all Nano UI screens.
 * Keeps drag-follow and animation behavior consistent.
 */
public final class NanoSliderController
{
    private NanoSliderController()
    {
    }

    public static float mouseRatio(float mouseX, float trackX, float trackWidth)
    {
        return UiMotion.clamp01((mouseX - trackX) / Math.max(1.0F, trackWidth));
    }

    public static float resolveDisplayRatio(String channelKey, float targetRatio, boolean dragging, boolean animationEnabled, float baseSpeed, float smooth, UiAnimation.Type type)
    {
        float target = UiMotion.clamp01(targetRatio);
        float speed = UiMotion.clamp(baseSpeed, 0.05F, 1.0F);

        if (dragging)
        {
            speed = UiMotion.clamp(speed * 1.42F + 0.08F, 0.08F, 1.0F);
        }

        float animated = UiAnimationBus.animate(channelKey, target, speed, smooth, type, animationEnabled);
        return animationEnabled ? animated : target;
    }

    public static float resolveDisplayRatio(String channelKey, float targetRatio, boolean dragging, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        return resolveDisplayRatio(channelKey, targetRatio, dragging, resolved.isEnabled(), resolved.sliderSpeed(), resolved.smooth(), resolved.type());
    }

    public static float resolveFocus(String channelKey, boolean hovered, boolean dragging, float speed, float smooth, UiAnimation.Type type, boolean animationEnabled)
    {
        return UiAnimationBus.animate(channelKey, hovered || dragging ? 1.0F : 0.0F, speed, smooth, type, animationEnabled);
    }

    public static float resolveFocus(String channelKey, boolean hovered, boolean dragging, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        return resolveFocus(channelKey, hovered, dragging, resolved.controlSpeed(), resolved.smooth(), resolved.type(), resolved.isEnabled());
    }

    public static float resolveFillRatio(String channelKey, float targetRatio, boolean dragging, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float speed = UiMotion.clamp(resolved.sliderSpeed() * 1.10F + 0.04F, 0.05F, 1.0F);
        return resolveDisplayRatio(channelKey + ".fill", targetRatio, dragging, resolved.isEnabled(), speed, resolved.smooth(), resolved.type());
    }

    public static float resolveKnobRatio(String channelKey, float targetRatio, boolean dragging, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float speed = UiMotion.clamp(resolved.sliderSpeed() * 1.20F + 0.08F, 0.05F, 1.0F);
        return resolveDisplayRatio(channelKey + ".knob", targetRatio, dragging, resolved.isEnabled(), speed, resolved.smooth(), resolved.type());
    }

    public static float resolveGlow(String channelKey, boolean hovered, boolean dragging, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float speed = UiMotion.clamp(resolved.controlSpeed() * 1.10F + 0.06F, 0.05F, 1.0F);
        return UiAnimationBus.animate(channelKey + ".glow", hovered || dragging ? 1.0F : 0.0F, speed, resolved.smooth(), resolved.type(), resolved.isEnabled());
    }
}
