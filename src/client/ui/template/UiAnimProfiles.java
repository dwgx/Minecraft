package client.ui.template;

import client.module.impl.client.ClickGuiModule;
import client.module.impl.client.UiScaleEditModule;

/**
 * Shared animation profile builders used by Nano UI surfaces.
 */
public final class UiAnimProfiles
{
    private UiAnimProfiles()
    {
    }

    public static UiAnimProfile clickGuiProfile(ClickGuiModule clickGui)
    {
        return fromClickGui(clickGui, 0.56F, 0.62F, 0.62F);
    }

    public static UiAnimProfile settingsProfile(ClickGuiModule clickGui)
    {
        return fromClickGui(clickGui, 0.58F, 0.62F, 0.62F);
    }

    public static UiAnimProfile uiScaleProfile(ClickGuiModule clickGui)
    {
        return fromClickGui(clickGui, 0.58F, 0.62F, 0.62F);
    }

    public static UiAnimProfile hudProfile(ClickGuiModule clickGui)
    {
        return fromClickGui(clickGui, 0.58F, 0.64F, 0.64F);
    }

    public static UiAnimProfile clickGuiWindowProfile(ClickGuiModule clickGui, UiScaleEditModule uiScale, boolean interacting)
    {
        UiAnimProfile base = clickGuiProfile(clickGui);
        float profileSpeed = uiScale == null ? 0.30F : uiScale.getMotionSpeed(UiScaleEditModule.UiTarget.CLICK_GUI);
        float speed = composeWindowSpeed(profileSpeed, base.windowSpeed());
        return withWindowSpeed(base, boostInteractionSpeed(speed, interacting));
    }

    public static UiAnimProfile settingsWindowProfile(ClickGuiModule clickGui, boolean interacting)
    {
        UiAnimProfile base = settingsProfile(clickGui);
        return withWindowSpeed(base, boostInteractionSpeed(base.windowSpeed(), interacting));
    }

    public static UiAnimProfile uiScaleWindowProfile(ClickGuiModule clickGui, UiScaleEditModule module, UiScaleEditModule.UiTarget target, boolean interacting)
    {
        UiAnimProfile base = uiScaleProfile(clickGui);
        float profileSpeed = module == null ? 0.30F : module.getMotionSpeed(target);
        float speed = composeWindowSpeed(profileSpeed, base.windowSpeed());
        return withWindowSpeed(base, boostInteractionSpeed(speed, interacting));
    }

    public static UiAnimProfile fromClickGui(ClickGuiModule clickGui, float fallbackControlSpeed, float fallbackSliderSpeed, float fallbackSmooth)
    {
        if (clickGui == null)
        {
            return new UiAnimProfile(true, 0.56F, fallbackControlSpeed, fallbackSliderSpeed, fallbackSmooth, UiAnimation.Type.EASE_OUT);
        }

        return new UiAnimProfile(
            clickGui.isGlobalAnimationEnabled(),
            clickGui.getGlobalAnimationSpeed(),
            clickGui.getControlAnimationSpeed(),
            clickGui.getSliderAnimationSpeed(),
            clickGui.getGlobalAnimationSmooth(),
            clickGui.getControlAnimationType()
        );
    }

    public static float composeWindowSpeed(float profileSpeed, float globalSpeed)
    {
        float profile = UiMotion.clamp(profileSpeed, 0.05F, 1.0F);
        float global = UiMotion.clamp(globalSpeed, 0.05F, 1.0F);
        return UiMotion.clamp(profile * (0.55F + global), 0.05F, 1.0F);
    }

    public static float boostInteractionSpeed(float speed, boolean interacting)
    {
        if (!interacting)
        {
            return UiMotion.clamp(speed, 0.05F, 1.0F);
        }

        return UiMotion.clamp(speed * 1.22F + 0.06F, 0.05F, 1.0F);
    }

    public static float scrollSpeed(UiAnimProfile profile)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        return UiMotion.clamp(resolved.controlSpeed() * 0.95F + 0.10F, 0.05F, 1.0F);
    }

    public static UiAnimProfile withWindowSpeed(UiAnimProfile profile, float speed)
    {
        UiAnimProfile resolved = profile == null ? UiAnimProfile.defaults() : profile;
        return new UiAnimProfile(
            resolved.isEnabled(),
            UiMotion.clamp(speed, 0.05F, 1.0F),
            resolved.controlSpeed(),
            resolved.sliderSpeed(),
            resolved.smooth(),
            resolved.type()
        );
    }
}
