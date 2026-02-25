package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.NanoSliderController;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiMotion;
import dwgx.nano.NanoTheme;
import org.lwjgl.system.MemoryStack;

/**
 * Self-contained slider component with drag state, animation, and rendering.
 * Replaces duplicated slider logic across ClickGui, ClientSettings, HudEditor, UIScale.
 */
public class NanoSlider extends NanoComponent
{
    /** Callback for value changes. */
    public interface ValueChangeListener
    {
        void onValueChanged(float normalizedValue);
    }

    private float value;
    private float minValue;
    private float maxValue;
    private float step;
    private boolean dragging;
    private ValueChangeListener listener;

    public NanoSlider(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void configure(float minValue, float maxValue, float step, float currentValue)
    {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.value = UiMotion.clamp(currentValue, minValue, maxValue);
    }

    public void setListener(ValueChangeListener listener)
    {
        this.listener = listener;
    }

    public float getValue()
    {
        return this.value;
    }

    public void setValue(float value)
    {
        this.value = UiMotion.clamp(value, this.minValue, this.maxValue);
    }

    public boolean isDragging()
    {
        return this.dragging;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        boolean hovered = this.isHovered(mouseX, mouseY);
        float targetRatio = this.normalizedValue();

        float fillRatio = NanoSliderController.resolveFillRatio(
            this.animKey("fill"), targetRatio, this.dragging, animProfile);
        float knobRatio = NanoSliderController.resolveKnobRatio(
            this.animKey("knob"), targetRatio, this.dragging, animProfile);
        float displayRatio = NanoSliderController.resolveDisplayRatio(
            this.animKey("display"), targetRatio, this.dragging, animProfile);
        float focus = NanoSliderController.resolveFocus(
            this.animKey("focus"), hovered, this.dragging, animProfile);
        float glow = NanoSliderController.resolveGlow(
            this.animKey("glow"), hovered, this.dragging, animProfile);

        NanoScreenKit.drawSliderTrack(vg, stack, theme,
            this.x, this.y, this.width, this.height,
            scale, fillRatio, knobRatio, displayRatio, focus, glow, this.dragging);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        if (button != 0 || !this.isHovered(mouseX, mouseY))
        {
            return false;
        }

        this.dragging = true;
        this.updateValueFromMouse(mouseX);
        return true;
    }

    /**
     * Force-start dragging from an external hit test (e.g. enlarged hit rect).
     * Bypasses the internal hover check.
     */
    public void forceStartDrag(int mouseX)
    {
        this.dragging = true;
        this.updateValueFromMouse(mouseX);
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button)
    {
        if (!this.dragging)
        {
            return false;
        }

        this.dragging = false;
        return true;
    }

    public void mouseDragged(int mouseX, int mouseY)
    {
        if (this.dragging)
        {
            this.updateValueFromMouse(mouseX);
        }
    }

    private void updateValueFromMouse(int mouseX)
    {
        float ratio = NanoSliderController.mouseRatio(mouseX, this.x, this.width);
        float raw = this.minValue + ratio * (this.maxValue - this.minValue);

        if (this.step > 0.0F)
        {
            raw = Math.round(raw / this.step) * this.step;
        }

        float clamped = UiMotion.clamp(raw, this.minValue, this.maxValue);

        if (clamped != this.value)
        {
            this.value = clamped;

            if (this.listener != null)
            {
                this.listener.onValueChanged(this.normalizedValue());
            }
        }
    }

    private float normalizedValue()
    {
        float range = this.maxValue - this.minValue;
        return range <= 0.0F ? 0.0F : UiMotion.clamp01((this.value - this.minValue) / range);
    }
}
