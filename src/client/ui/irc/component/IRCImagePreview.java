package client.ui.irc.component;

import client.ui.layout.UiRect;
import dwgx.nano.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Fullscreen image preview overlay. Shown when clicking an image message.
 * Renders a dimmed backdrop with the image centered, click anywhere to dismiss.
 */
public final class IRCImagePreview
{
    private static final float MAX_RATIO = 0.8F;
    private static final float PAD = 16.0F;

    private boolean visible;
    private String imagePath;
    private float imgW;
    private float imgH;

    public boolean isVisible() { return this.visible; }

    public void show(String imagePath, float naturalW, float naturalH)
    {
        this.imagePath = imagePath;
        this.imgW = naturalW > 0 ? naturalW : 320.0F;
        this.imgH = naturalH > 0 ? naturalH : 240.0F;
        this.visible = true;
    }

    public void hide() { this.visible = false; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, float screenW, float screenH, int mx, int my)
    {
        if (!this.visible) return;

        // Dimmed backdrop
        NanoRenderUtils.fillRect(vg, 0, 0, screenW, screenH,
                NanoRenderUtils.argb(stack, 0xCC000000));

        // Fit image within screen bounds
        float maxW = screenW * MAX_RATIO;
        float maxH = screenH * MAX_RATIO;
        float scale = Math.min(maxW / this.imgW, maxH / this.imgH);
        if (scale > 1.0F) scale = 1.0F;
        float drawW = this.imgW * scale;
        float drawH = this.imgH * scale;
        float x = (screenW - drawW) * 0.5F;
        float y = (screenH - drawH) * 0.5F;

        // Image placeholder (rounded rect with path label)
        NanoRenderUtils.fillRoundedRect(vg, x, y, drawW, drawH, theme.cardRadius(),
                NanoRenderUtils.argb(stack, theme.cardArgb()));

        int font = NanoFontBook.uiRegular();
        if (this.imagePath != null)
        {
            NanoRenderUtils.drawLabel(vg, stack, font, x + drawW * 0.5F, y + drawH * 0.5F,
                    13.0F, this.imagePath, theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }

        // Close hint
        NanoRenderUtils.drawLabel(vg, stack, font, screenW * 0.5F, y + drawH + PAD,
                11.0F, "Click anywhere to close", theme.textWeakArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);
    }

    /** Returns true if click was consumed (dismiss). */
    public boolean handleClick(int mx, int my)
    {
        if (!this.visible) return false;
        this.visible = false;
        return true;
    }
}
