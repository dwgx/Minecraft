package client.render;

import dwgx.nano.NanoRenderUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgCircle;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgLineTo;
import static org.lwjgl.nanovg.NanoVG.nvgMoveTo;
import static org.lwjgl.nanovg.NanoVG.nvgStroke;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeColor;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeWidth;

/**
 * Compatibility wrapper; prefer {@link NanoRenderUtils} for new code.
 */
@Deprecated
public final class NanoUtils
{
    private NanoUtils()
    {
    }

    public static NVGColor rgba(MemoryStack stack, int r, int g, int b, int a)
    {
        return NanoRenderUtils.rgba(stack, r, g, b, a);
    }

    public static NVGColor argb(MemoryStack stack, int argb)
    {
        return NanoRenderUtils.argb(stack, argb);
    }

    public static void fillRect(long vg, float x, float y, float width, float height, NVGColor color)
    {
        NanoRenderUtils.fillRect(vg, x, y, width, height, color);
    }

    public static void fillRoundedRect(long vg, float x, float y, float width, float height, float radius, NVGColor color)
    {
        NanoRenderUtils.fillRoundedRect(vg, x, y, width, height, radius, color);
    }

    public static void strokeRoundedRect(long vg, float x, float y, float width, float height, float radius, float strokeWidth, NVGColor color)
    {
        NanoRenderUtils.strokeRoundedRect(vg, x, y, width, height, radius, strokeWidth, color);
    }

    public static void drawLine(long vg, float x1, float y1, float x2, float y2, float width, NVGColor color)
    {
        if (color == null || width <= 0.0F)
        {
            return;
        }

        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgStrokeWidth(vg, width);
        nvgStrokeColor(vg, color);
        nvgStroke(vg);
    }

    public static void drawCircle(long vg, float cx, float cy, float radius, NVGColor color)
    {
        if (color == null || radius <= 0.0F)
        {
            return;
        }

        nvgBeginPath(vg);
        nvgCircle(vg, cx, cy, radius);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void drawPanel(long vg, MemoryStack stack, float x, float y, float width, float height, float radius, int fillArgb, int borderArgb, float borderWidth, int shadowInnerAlpha)
    {
        NanoRenderUtils.drawPanel(vg, stack, x, y, width, height, radius, fillArgb, borderArgb, borderWidth, shadowInnerAlpha);
    }

    public static void drawText(long vg, MemoryStack stack, int fontId, float x, float y, float size, int align, int argb, String text)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontId, x, y, size, text, (argb >>> 16) & 255, (argb >>> 8) & 255, argb & 255, (argb >>> 24) & 255, align);
    }

    public static float measureTextWidth(long vg, int fontId, float size, String text)
    {
        return NanoRenderUtils.textWidth(vg, fontId, size, text);
    }

    public static void beginClip(long vg, float x, float y, float width, float height)
    {
        dwgx.nano.NanoUi.beginClip(vg, x, y, width, height);
    }

    public static void endClip(long vg)
    {
        dwgx.nano.NanoUi.endClip(vg);
    }

    public static float pixelRatio(int framebufferWidth, int windowWidth)
    {
        return NanoRenderUtils.pixelRatio(framebufferWidth, windowWidth);
    }
}
