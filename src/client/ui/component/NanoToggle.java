package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Toggle switch component with slide animation.
 * Replaces duplicated BoolSetting rendering across screens.
 */
public class NanoToggle extends NanoComponent
{
    public interface ToggleListener
    {
        void onToggled(boolean newState);
    }

    private boolean enabled;
    private ToggleListener listener;

    public NanoToggle(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setListener(ToggleListener listener)
    {
        this.listener = listener;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        boolean hovered = this.isHovered(mouseX, mouseY);
        float focus = UiControlAnimations.focus(this.animKey("focus"), hovered, animProfile);
        float toggleVal = UiAnimationBus.animateControl(
            this.animKey("toggle"), this.enabled ? 1.0F : 0.0F, animProfile);

        float trackW = this.width;
        float trackH = this.height;
        float trackRadius = trackH * 0.5F;

        // Track background
        int trackOff = NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlActiveArgb(), focus * 0.3F);
        int trackOn = NanoScreenKit.mixArgb(theme.accentArgb(), theme.accentSoftArgb(), focus * 0.2F);
        int trackColor = NanoScreenKit.mixArgb(trackOff, trackOn, UiMotion.clamp01(toggleVal));
        int trackBorder = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 90);
        NanoUi.drawSurface(vg, stack, this.x, this.y, trackW, trackH, trackRadius, trackColor, trackBorder);

        // Knob
        float knobPad = NanoScreenKit.scaled(2.0F, scale);
        float knobSize = trackH - knobPad * 2.0F;
        float knobTravel = trackW - knobSize - knobPad * 2.0F;
        float knobX = this.x + knobPad + knobTravel * UiMotion.clamp01(toggleVal);
        float knobY = this.y + knobPad;
        float knobRadius = knobSize * 0.5F;

        int knobColor = NanoScreenKit.mixArgb(0xFFE8ECF0, 0xFFFAFCFF, UiMotion.clamp01(toggleVal * 0.6F + focus * 0.4F));
        NanoUi.drawSurface(vg, stack, knobX, knobY, knobSize, knobSize, knobRadius,
            knobColor, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80));
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        if (button != 0 || !this.isHovered(mouseX, mouseY))
        {
            return false;
        }

        this.enabled = !this.enabled;

        if (this.listener != null)
        {
            this.listener.onToggled(this.enabled);
        }

        return true;
    }
}
