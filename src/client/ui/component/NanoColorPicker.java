package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiMotion;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * HSV color picker with saturation/value panel, hue bar, alpha bar, and rainbow toggle.
 * Supports two layout modes:
 * <ul>
 *   <li>Auto layout (default): SV panel + vertical hue bar + vertical alpha bar side by side</li>
 *   <li>External layout: parent provides exact sub-rects via {@link #setSubBounds}</li>
 * </ul>
 */
public class NanoColorPicker extends NanoComponent
{
    public interface ColorChangeListener
    {
        void onColorChanged(int r, int g, int b, int a, boolean rainbow);
    }

    private float hue;
    private float saturation;
    private float brightness;
    private int alpha = 255;
    private boolean rainbow;
    private boolean draggingSv;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private ColorChangeListener listener;

    // Layout sub-regions (computed in render or set externally)
    private float svX, svY, svW, svH;
    private float hueBarX, hueBarY, hueBarW, hueBarH;
    private float alphaBarX, alphaBarY, alphaBarW, alphaBarH;
    private boolean alphaHorizontal;
    private boolean externalLayout;

    public NanoColorPicker(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setColor(int r, int g, int b, int a, boolean rainbow)
    {
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = NanoRenderUtils.clamp255(a);
        this.rainbow = rainbow;
    }

    public void setListener(ColorChangeListener listener)
    {
        this.listener = listener;
    }

    public boolean isDragging()
    {
        return this.draggingSv || this.draggingHue || this.draggingAlpha;
    }

    public void stopDragging()
    {
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
    }

    public float getHue() { return this.hue; }
    public float getSaturation() { return this.saturation; }
    public float getBrightness() { return this.brightness; }
    public int getAlpha() { return this.alpha; }
    public boolean isRainbow() { return this.rainbow; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }

    /**
     * Set external sub-bounds for the three picker areas.
     * When set, {@link #render} skips internal layout computation and uses these rects directly.
     *
     * @param alphaHorizontal true if the alpha bar is horizontal (gradient left-to-right),
     *                        false if vertical (gradient top-to-bottom)
     */
    public void setSubBounds(float svX, float svY, float svW, float svH,
                              float hueX, float hueY, float hueW, float hueH,
                              float alphaX, float alphaY, float alphaW, float alphaH,
                              boolean alphaHorizontal)
    {
        this.svX = svX;
        this.svY = svY;
        this.svW = svW;
        this.svH = svH;
        this.hueBarX = hueX;
        this.hueBarY = hueY;
        this.hueBarW = hueW;
        this.hueBarH = hueH;
        this.alphaBarX = alphaX;
        this.alphaBarY = alphaY;
        this.alphaBarW = alphaW;
        this.alphaBarH = alphaH;
        this.alphaHorizontal = alphaHorizontal;
        this.externalLayout = true;
    }

    /**
     * Clear external sub-bounds; revert to auto layout.
     */
    public void clearSubBounds()
    {
        this.externalLayout = false;
        this.alphaHorizontal = false;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender())
        {
            return;
        }

        if (!this.externalLayout)
        {
            this.computeAutoLayout(scale);
        }

        this.renderSvPanel(vg, stack, scale);
        this.renderHueBar(vg, stack, scale);
        this.renderAlphaBar(vg, stack, scale);
    }

    private void computeAutoLayout(float scale)
    {
        float pad = NanoScreenKit.scaled(4.0F, scale);
        float barW = NanoScreenKit.scaled(14.0F, scale);
        this.hueBarW = barW;
        this.alphaBarW = barW;
        this.svX = this.x;
        this.svY = this.y;
        this.svW = this.width - barW * 2.0F - pad * 2.0F;
        this.svH = this.height;
        this.hueBarX = this.svX + this.svW + pad;
        this.hueBarY = this.y;
        this.hueBarH = this.height;
        this.alphaBarX = this.hueBarX + barW + pad;
        this.alphaBarY = this.y;
        this.alphaBarH = this.height;
        this.alphaHorizontal = false;
    }

    // --- PLACEHOLDER_RENDER_METHODS ---

    private void renderSvPanel(long vg, MemoryStack stack, float scale)
    {
        float radius = NanoScreenKit.scaled(4.0F, scale);
        int hueRgb = java.awt.Color.HSBtoRGB(this.hue, 1.0F, 1.0F);
        NVGColor white = NanoRenderUtils.rgba(stack, 255, 255, 255, 255);
        NVGColor hueColor = NanoRenderUtils.argb(stack, 0xFF000000 | hueRgb);
        NVGPaint hPaint = NVGPaint.mallocStack(stack);
        nvgLinearGradient(vg, this.svX, this.svY, this.svX + this.svW, this.svY, white, hueColor, hPaint);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, this.svX, this.svY, this.svW, this.svH, radius);
        nvgFillPaint(vg, hPaint);
        nvgFill(vg);

        NVGColor transBlack = NanoRenderUtils.rgba(stack, 0, 0, 0, 0);
        NVGColor black = NanoRenderUtils.rgba(stack, 0, 0, 0, 255);
        NVGPaint vPaint = NVGPaint.mallocStack(stack);
        nvgLinearGradient(vg, this.svX, this.svY, this.svX, this.svY + this.svH, transBlack, black, vPaint);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, this.svX, this.svY, this.svW, this.svH, radius);
        nvgFillPaint(vg, vPaint);
        nvgFill(vg);

        float cx = this.svX + this.saturation * this.svW;
        float cy = this.svY + (1.0F - this.brightness) * this.svH;
        float cursorR = NanoScreenKit.scaled(5.0F, scale);
        NanoRenderUtils.strokeRoundedRect(vg, cx - cursorR, cy - cursorR,
            cursorR * 2.0F, cursorR * 2.0F, cursorR,
            NanoScreenKit.scaled(1.5F, scale),
            NanoRenderUtils.rgba(stack, 255, 255, 255, 220));
    }

    // --- PLACEHOLDER_REMAINING ---

    private void renderHueBar(long vg, MemoryStack stack, float scale)
    {
        float radius = NanoScreenKit.scaled(3.0F, scale);
        float segH = this.hueBarH / 6.0F;
        for (int i = 0; i < 6; ++i)
        {
            int c1 = java.awt.Color.HSBtoRGB(i / 6.0F, 1.0F, 1.0F);
            int c2 = java.awt.Color.HSBtoRGB((i + 1) / 6.0F, 1.0F, 1.0F);
            float sy = this.hueBarY + i * segH;
            NanoRenderUtils.fillRoundedRectGradient(vg, stack,
                this.hueBarX, sy, this.hueBarW, segH + 1.0F,
                i == 0 || i == 5 ? radius : 0.0F,
                0xFF000000 | c1, 0xFF000000 | c2, true);
        }
        float cursorY = this.hueBarY + this.hue * this.hueBarH;
        float ch = NanoScreenKit.scaled(3.0F, scale);
        NanoRenderUtils.strokeRoundedRect(vg, this.hueBarX - 1.0F, cursorY - ch * 0.5F,
            this.hueBarW + 2.0F, ch, ch * 0.5F,
            NanoScreenKit.scaled(1.2F, scale),
            NanoRenderUtils.rgba(stack, 255, 255, 255, 230));
    }

    private void renderAlphaBar(long vg, MemoryStack stack, float scale)
    {
        float radius = NanoScreenKit.scaled(3.0F, scale);
        int rgb = java.awt.Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        int opaque = 0xFF000000 | rgb;
        int transparent = rgb & 0x00FFFFFF;
        if (this.alphaHorizontal)
        {
            NanoRenderUtils.fillRoundedRectGradient(vg, stack,
                this.alphaBarX, this.alphaBarY, this.alphaBarW, this.alphaBarH,
                radius, transparent, opaque, false);
            float ratio = (float) this.alpha / 255.0F;
            float cursorX = this.alphaBarX + ratio * this.alphaBarW;
            float cw = NanoScreenKit.scaled(3.0F, scale);
            NanoRenderUtils.strokeRoundedRect(vg, cursorX - cw * 0.5F, this.alphaBarY - 1.0F,
                cw, this.alphaBarH + 2.0F, cw * 0.5F,
                NanoScreenKit.scaled(1.2F, scale),
                NanoRenderUtils.rgba(stack, 255, 255, 255, 230));
        }
        else
        {
            NanoRenderUtils.fillRoundedRectGradient(vg, stack,
                this.alphaBarX, this.alphaBarY, this.alphaBarW, this.alphaBarH,
                radius, opaque, transparent, true);
            float ratio = 1.0F - (float) this.alpha / 255.0F;
            float cursorY = this.alphaBarY + ratio * this.alphaBarH;
            float ch = NanoScreenKit.scaled(3.0F, scale);
            NanoRenderUtils.strokeRoundedRect(vg, this.alphaBarX - 1.0F, cursorY - ch * 0.5F,
                this.alphaBarW + 2.0F, ch, ch * 0.5F,
                NanoScreenKit.scaled(1.2F, scale),
                NanoRenderUtils.rgba(stack, 255, 255, 255, 230));
        }
    }

    // --- PLACEHOLDER_MOUSE ---

    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        if (button != 0) return false;
        if (inRect(mouseX, mouseY, this.svX, this.svY, this.svW, this.svH))
        {
            this.draggingSv = true;
            this.updateSv(mouseX, mouseY);
            return true;
        }
        if (inRect(mouseX, mouseY, this.hueBarX, this.hueBarY, this.hueBarW, this.hueBarH))
        {
            this.draggingHue = true;
            this.updateHue(mouseX, mouseY);
            return true;
        }
        if (inRect(mouseX, mouseY, this.alphaBarX, this.alphaBarY, this.alphaBarW, this.alphaBarH))
        {
            this.draggingAlpha = true;
            this.updateAlpha(mouseX, mouseY);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button)
    {
        boolean wasDragging = this.isDragging();
        this.draggingSv = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        return wasDragging;
    }

    public void mouseDragged(int mouseX, int mouseY)
    {
        if (this.draggingSv) this.updateSv(mouseX, mouseY);
        else if (this.draggingHue) this.updateHue(mouseX, mouseY);
        else if (this.draggingAlpha) this.updateAlpha(mouseX, mouseY);
    }

    private void updateSv(int mouseX, int mouseY)
    {
        this.saturation = UiMotion.clamp01((mouseX - this.svX) / Math.max(1.0F, this.svW));
        this.brightness = 1.0F - UiMotion.clamp01((mouseY - this.svY) / Math.max(1.0F, this.svH));
        this.notifyChange();
    }

    private void updateHue(int mouseX, int mouseY)
    {
        this.hue = UiMotion.clamp01((mouseY - this.hueBarY) / Math.max(1.0F, this.hueBarH));
        this.notifyChange();
    }

    private void updateAlpha(int mouseX, int mouseY)
    {
        if (this.alphaHorizontal)
        {
            float ratio = UiMotion.clamp01((mouseX - this.alphaBarX) / Math.max(1.0F, this.alphaBarW));
            this.alpha = NanoRenderUtils.clamp255(Math.round(ratio * 255.0F));
        }
        else
        {
            float ratio = UiMotion.clamp01((mouseY - this.alphaBarY) / Math.max(1.0F, this.alphaBarH));
            this.alpha = NanoRenderUtils.clamp255(255 - Math.round(ratio * 255.0F));
        }
        this.notifyChange();
    }

    private void notifyChange()
    {
        if (this.listener != null)
        {
            int rgb = java.awt.Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
            this.listener.onColorChanged(
                (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF,
                this.alpha, this.rainbow);
        }
    }

    public int getCurrentArgb()
    {
        int rgb = java.awt.Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        return (this.alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static boolean inRect(float px, float py, float rx, float ry, float rw, float rh)
    {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }
}
