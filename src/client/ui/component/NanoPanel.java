package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiMotion;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Scrollable panel container with rounded background, shadow, and scroll bar.
 * Replaces duplicated scroll/panel logic across screens.
 */
public class NanoPanel extends NanoComponent
{
    private float contentHeight;
    private int scrollOffset;
    private float scrollVisual;
    private float scrollStep = 26.0F;

    public NanoPanel(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setContentHeight(float contentHeight)
    {
        this.contentHeight = contentHeight;
    }

    public void setScrollStep(float step)
    {
        this.scrollStep = step;
    }

    public int getScrollOffset()
    {
        return this.scrollOffset;
    }

    public float getScrollVisual()
    {
        return this.scrollVisual;
    }

    /**
     * Returns the visible content area Y offset (for clipping children).
     */
    public float getContentY()
    {
        return this.y - this.scrollVisual;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        float radius = Math.min(theme.controlRadius(), this.height * 0.1F);

        // Panel background
        NanoUi.drawSurface(vg, stack, this.x, this.y, this.width, this.height, radius,
            theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 72));

        // Animate scroll
        this.scrollVisual = NanoScreenKit.animateScroll(
            this.animKey("scroll"), (float) this.scrollOffset, this.scrollVisual, animProfile);

        // Scroll bar (if content overflows)
        float overflow = this.contentHeight - this.height;

        if (overflow > 0.0F)
        {
            float barH = Math.max(20.0F, this.height * (this.height / this.contentHeight));
            float barTravel = this.height - barH;
            float barY = this.y + barTravel * UiMotion.clamp01(this.scrollVisual / overflow);
            float barW = NanoScreenKit.scaled(3.0F, scale);
            float barX = this.x + this.width - barW - NanoScreenKit.scaled(2.0F, scale);
            NanoRenderUtils.fillRoundedRect(vg, barX, barY, barW, barH, barW * 0.5F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textMutedArgb(), 80)));
        }
    }

    public boolean mouseScrolled(int mouseX, int mouseY, int delta)
    {
        if (!this.isHovered(mouseX, mouseY))
        {
            return false;
        }

        float overflow = this.contentHeight - this.height;

        if (overflow <= 0.0F)
        {
            return false;
        }

        this.scrollOffset -= delta > 0 ? 1 : -1;
        int maxScroll = Math.max(0, (int) Math.ceil(overflow / this.scrollStep));
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
        return true;
    }

    /**
     * Clamp scroll to valid range. Call after content height changes.
     */
    public void clampScroll()
    {
        float overflow = this.contentHeight - this.height;

        if (overflow <= 0.0F)
        {
            this.scrollOffset = 0;
        }
        else
        {
            int maxScroll = Math.max(0, (int) Math.ceil(overflow / this.scrollStep));
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
        }
    }
}
