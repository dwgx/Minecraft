package dwgx.nano;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillPaint;
import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgRoundedRect;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

public final class NanoUi
{
    private static final float BACKDROP_OVERSCAN = 1.0F;

    private NanoUi()
    {
    }

    public static void drawBackdrop(long vg, MemoryStack stack, float width, float height, NanoTheme theme)
    {
        NanoRenderUtils.fillRect(vg, -BACKDROP_OVERSCAN, -BACKDROP_OVERSCAN, width + BACKDROP_OVERSCAN * 2.0F, height + BACKDROP_OVERSCAN * 2.0F, NanoRenderUtils.argb(stack, theme.backdropArgb()));
    }

    public static void drawWindow(long vg, MemoryStack stack, float x, float y, float width, float height, NanoTheme theme)
    {
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, x, y, width, height, theme.windowRadius(), theme.windowTopArgb(), theme.windowBottomArgb(), true);
        NanoRenderUtils.strokeRoundedRect(vg, x, y, width, height, theme.windowRadius(), 1.0F, NanoRenderUtils.argb(stack, theme.windowBorderArgb()));

        int glossTop = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 58);
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, x + 1.0F, y + 1.0F, width - 2.0F, 34.0F, Math.max(8.0F, theme.windowRadius() - 3.0F), glossTop, NanoRenderUtils.withAlpha(glossTop, 0), true);
    }

    public static void drawSurface(long vg, MemoryStack stack, float x, float y, float width, float height, float radius, int fillArgb, int borderArgb)
    {
        NanoRenderUtils.fillRoundedRect(vg, x, y, width, height, radius, NanoRenderUtils.argb(stack, fillArgb));

        if ((borderArgb >>> 24) > 0)
        {
            NanoRenderUtils.strokeRoundedRect(vg, x, y, width, height, radius, 1.0F, NanoRenderUtils.argb(stack, borderArgb));
        }
    }

    public static void drawPanel(long vg, MemoryStack stack, float x, float y, float width, float height, NanoTheme theme)
    {
        drawSurface(vg, stack, x, y, width, height, theme.cardRadius(), theme.cardArgb(), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 84));
    }

    public static void drawRow(long vg, MemoryStack stack, float x, float y, float width, float height, NanoTheme theme, int index, boolean hovered, boolean selected)
    {
        int base = selected ? theme.rowSelectedArgb() : (hovered ? theme.rowHoverArgb() : (index % 2 == 0 ? theme.rowArgb() : theme.cardAltArgb()));
        NanoRenderUtils.fillRoundedRect(vg, x, y, width, height, theme.controlRadius(), NanoRenderUtils.argb(stack, base));
    }

    public static void drawTag(long vg, MemoryStack stack, float x, float y, float width, float height, NanoTheme theme, boolean active)
    {
        int fill = active ? theme.controlActiveArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, x, y, width, height, theme.controlRadius(), NanoRenderUtils.argb(stack, fill));
    }

    public static void drawLeftText(long vg, MemoryStack stack, int fontId, float x, float y, float size, int argb, String text)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontId, x, y, size, text, argb, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
    }

    public static void drawRightText(long vg, MemoryStack stack, int fontId, float x, float y, float size, int argb, String text)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontId, x, y, size, text, argb, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
    }

    public static void drawCenterText(long vg, MemoryStack stack, int fontId, float x, float y, float size, int argb, String text)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontId, x, y, size, text, argb, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
    }

    public static void beginClip(long vg, float x, float y, float width, float height)
    {
        nvgSave(vg);

        if (!NanoRenderUtils.isFinite(x) || !NanoRenderUtils.isFinite(y) || !NanoRenderUtils.isFinite(width) || !NanoRenderUtils.isFinite(height) || width <= 0.0F || height <= 0.0F)
        {
            nvgScissor(vg, -1.0F, -1.0F, 0.0F, 0.0F);
            return;
        }

        nvgScissor(vg, x, y, width, height);
    }

    public static void endClip(long vg)
    {
        nvgRestore(vg);
    }

    public static void drawAccentFlag(long vg, MemoryStack stack, float x, float y, float width, float height, int argb)
    {
        NanoRenderUtils.fillRoundedRect(vg, x, y, width, height, Math.max(1.0F, Math.min(width, height) * 0.42F), NanoRenderUtils.argb(stack, argb));
    }

    public static void drawColorSwatch(long vg, MemoryStack stack, float x, float y, float size, int argb, boolean border)
    {
        NanoRenderUtils.fillRoundedRect(vg, x, y, size, size, 3.5F, NanoRenderUtils.argb(stack, argb));

        if (border)
        {
            NanoRenderUtils.strokeRoundedRect(vg, x, y, size, size, 3.5F, 1.0F, NanoRenderUtils.argb(stack, 0xEEFFFFFF));
        }
    }
}
