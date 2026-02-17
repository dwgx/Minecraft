package client.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;

public final class NanoUtils
{
    private NanoUtils()
    {
    }

    public static NVGColor rgba(MemoryStack stack, int r, int g, int b, int a)
    {
        NVGColor color = NVGColor.mallocStack(stack);
        nvgRGBA((byte)clamp255(r), (byte)clamp255(g), (byte)clamp255(b), (byte)clamp255(a), color);
        return color;
    }

    public static NVGColor argb(MemoryStack stack, int argb)
    {
        int a = argb >>> 24 & 255;
        int r = argb >>> 16 & 255;
        int g = argb >>> 8 & 255;
        int b = argb & 255;
        return rgba(stack, r, g, b, a);
    }

    public static void fillRect(long vg, float x, float y, float width, float height, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void fillRoundedRect(long vg, float x, float y, float width, float height, float radius, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void strokeRoundedRect(long vg, float x, float y, float width, float height, float radius, float strokeWidth, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgStrokeWidth(vg, strokeWidth);
        nvgStrokeColor(vg, color);
        nvgStroke(vg);
    }

    public static void drawLine(long vg, float x1, float y1, float x2, float y2, float width, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgStrokeWidth(vg, width);
        nvgStrokeColor(vg, color);
        nvgStroke(vg);
    }

    public static void drawCircle(long vg, float cx, float cy, float radius, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgCircle(vg, cx, cy, radius);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void drawPanel(long vg, MemoryStack stack, float x, float y, float width, float height, float radius, int fillArgb, int borderArgb, float borderWidth, int shadowInnerAlpha)
    {
        NVGColor shadowInner = rgba(stack, 0, 0, 0, shadowInnerAlpha);
        NVGColor shadowOuter = rgba(stack, 0, 0, 0, 0);
        NVGPaint shadowPaint = NVGPaint.mallocStack(stack);
        nvgBoxGradient(vg, x - 4.0F, y - 4.0F, width + 8.0F, height + 8.0F, radius + 1.0F, 12.0F, shadowInner, shadowOuter, shadowPaint);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x - 8.0F, y - 8.0F, width + 16.0F, height + 16.0F, radius + 5.0F);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);
        fillRoundedRect(vg, x, y, width, height, radius, argb(stack, fillArgb));
        strokeRoundedRect(vg, x, y, width, height, radius, borderWidth, argb(stack, borderArgb));
    }

    public static void drawText(long vg, MemoryStack stack, int fontId, float x, float y, float size, int align, int argb, String text)
    {
        if (fontId < 0 || text == null)
        {
            return;
        }

        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, align);
        nvgFillColor(vg, argb(stack, argb));
        nvgText(vg, x, y, text);
    }

    public static float measureTextWidth(long vg, int fontId, float size, String text)
    {
        if (fontId < 0 || text == null)
        {
            return 0.0F;
        }

        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        float[] bounds = new float[4];
        nvgTextBounds(vg, 0.0F, 0.0F, text, bounds);
        return bounds[2] - bounds[0];
    }

    public static void beginClip(long vg, float x, float y, float width, float height)
    {
        nvgSave(vg);
        nvgScissor(vg, x, y, width, height);
    }

    public static void endClip(long vg)
    {
        nvgRestore(vg);
    }

    public static float pixelRatio(int framebufferWidth, int windowWidth)
    {
        if (framebufferWidth <= 0 || windowWidth <= 0)
        {
            return 1.0F;
        }

        return (float)framebufferWidth / (float)windowWidth;
    }

    private static int clamp255(int value)
    {
        return Math.max(0, Math.min(255, value));
    }
}
