package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import dwgx.nano.NanoTheme;
import org.lwjgl.system.MemoryStack;

/**
 * Animated list row with hover/select state and stagger reveal.
 * Replaces duplicated row rendering across all Nano screens.
 */
public class NanoRow extends NanoComponent
{
    private boolean selected;
    private float reveal = 1.0F;

    public NanoRow(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public boolean isSelected()
    {
        return this.selected;
    }

    public void setReveal(float reveal)
    {
        this.reveal = UiMotion.clamp01(reveal);
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        boolean hovered = this.isHovered(mouseX, mouseY);
        float hoverRatio = UiControlAnimations.focus(this.animKey("hover"), hovered, animProfile);
        float selectRatio = UiControlAnimations.focus(this.animKey("select"), this.selected, animProfile);

        NanoScreenKit.drawAnimatedRow(vg, stack, theme,
            this.x, this.y, this.width, this.height,
            hoverRatio, selectRatio, this.reveal);
    }
}
