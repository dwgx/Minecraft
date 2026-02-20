package client.ui.template;

/**
 * Shared control-level animation channels for text input, choice selectors and popups.
 */
public final class UiControlAnimations
{
    private UiControlAnimations()
    {
    }

    public static float hover(String key, boolean hovered, UiAnimProfile profile)
    {
        return UiAnimationBus.animateControl(key + ".hover", hovered ? 1.0F : 0.0F, profile);
    }

    public static float focus(String key, boolean focused, UiAnimProfile profile)
    {
        return UiAnimationBus.animateControl(key + ".focus", focused ? 1.0F : 0.0F, profile);
    }

    public static float value(String key, float target, UiAnimProfile profile, float speed)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float clampedTarget = UiMotion.clamp01(target);
        float clampedSpeed = UiMotion.clamp(speed, 0.05F, 1.0F);
        return UiAnimationBus.animateWithSpeed(key + ".value", clampedTarget, resolved, clampedSpeed);
    }

    public static float presence(String key, boolean visible, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float speed = UiMotion.clamp(resolved.controlSpeed() * 1.04F + 0.05F, 0.05F, 1.0F);
        return presence(key, visible, resolved, speed);
    }

    public static float presence(String key, boolean visible, UiAnimProfile profile, float speed)
    {
        return value(key + ".presence", visible ? 1.0F : 0.0F, profile, speed);
    }

    public static float stagger(String key, float master, int index, float step, UiAnimProfile profile, float speed)
    {
        float stage = UiMotion.clamp01(master);
        float delay = Math.max(0.0F, (float)Math.max(0, index) * Math.max(0.0F, step));
        float target = UiMotion.clamp01(stage * (1.0F + delay) - delay);
        return value(key + ".stagger." + Math.max(0, index), target, profile, speed);
    }

    public static float open(String key, boolean opened, UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        float speed = UiMotion.clamp(resolved.controlSpeed() * 1.06F + 0.06F, 0.05F, 1.0F);
        return UiAnimationBus.animateWithSpeed(key + ".open", opened ? 1.0F : 0.0F, resolved, speed);
    }

    public static ChoiceState choice(String key, boolean hovered, boolean expanded, UiAnimProfile profile)
    {
        float hover = hover(key, hovered, profile);
        float focus = focus(key, hovered || expanded, profile);
        float open = open(key, expanded, profile);
        return new ChoiceState(hover, focus, open);
    }

    public static final class ChoiceState
    {
        private final float hover;
        private final float focus;
        private final float open;

        private ChoiceState(float hover, float focus, float open)
        {
            this.hover = hover;
            this.focus = focus;
            this.open = open;
        }

        public float hover()
        {
            return this.hover;
        }

        public float focus()
        {
            return this.focus;
        }

        public float open()
        {
            return this.open;
        }
    }
}
