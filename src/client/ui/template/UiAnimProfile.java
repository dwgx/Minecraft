package client.ui.template;

/**
 * Immutable animation profile shared by Nano UI surfaces.
 */
public final class UiAnimProfile
{
    private final boolean enabled;
    private final float windowSpeed;
    private final float controlSpeed;
    private final float sliderSpeed;
    private final float smooth;
    private final UiAnimation.Type type;

    public UiAnimProfile(boolean enabled, float windowSpeed, float controlSpeed, float sliderSpeed, float smooth, UiAnimation.Type type)
    {
        this.enabled = enabled;
        this.windowSpeed = UiMotion.clamp(windowSpeed, 0.05F, 1.0F);
        this.controlSpeed = UiMotion.clamp(controlSpeed, 0.05F, 1.0F);
        this.sliderSpeed = UiMotion.clamp(sliderSpeed, 0.05F, 1.0F);
        this.smooth = UiMotion.clamp(smooth, 0.0F, 1.0F);
        this.type = type == null ? UiAnimation.Type.EASE_OUT : type;
    }

    public static UiAnimProfile defaults()
    {
        return new UiAnimProfile(true, 0.56F, 0.58F, 0.62F, 0.62F, UiAnimation.Type.EASE_OUT);
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public float windowSpeed()
    {
        return this.windowSpeed;
    }

    public float controlSpeed()
    {
        return this.controlSpeed;
    }

    public float sliderSpeed()
    {
        return this.sliderSpeed;
    }

    public float smooth()
    {
        return this.smooth;
    }

    public UiAnimation.Type type()
    {
        return this.type;
    }
}

