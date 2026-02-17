package dwgx.nano;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Reusable NanoVG rendering helpers to keep overlay code compact.
 */
public final class NanoRenderUtils
{
    private NanoRenderUtils()
    {
    }

    public static NVGColor rgba(MemoryStack stack, int r, int g, int b, int a)
    {
        NVGColor color = NVGColor.mallocStack(stack);
        nvgRGBA((byte)r, (byte)g, (byte)b, (byte)a, color);
        return color;
    }

    /**
     * ARGB packed int (0xAARRGGBB) to NVGColor.
     */
    public static NVGColor argb(MemoryStack stack, int argb)
    {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return rgba(stack, r, g, b, a);
    }

    public static NVGPaint boxShadow(long vg, MemoryStack stack, float x, float y, float w, float h, float radius, float feather, int innerAlpha, int outerAlpha)
    {
        NVGColor inner = rgba(stack, 0, 0, 0, innerAlpha);
        NVGColor outer = rgba(stack, 0, 0, 0, outerAlpha);
        NVGPaint paint = NVGPaint.mallocStack(stack);
        nvgBoxGradient(vg, x, y, w, h, radius, feather, inner, outer, paint);
        return paint;
    }

    public static void fillRoundedRect(long vg, float x, float y, float w, float h, float radius, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void fillRect(long vg, float x, float y, float w, float h, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void strokeRoundedRect(long vg, float x, float y, float w, float h, float radius, float strokeWidth, NVGColor color)
    {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, strokeWidth);
        nvgStroke(vg);
    }

    /**
     * Convenience: shadow + fill + stroke panel.
     */
    public static void drawPanel(long vg, MemoryStack stack, float x, float y, float w, float h,
                                 float radius, int fillArgb, int strokeArgb, float strokeWidth,
                                 int shadowInnerAlpha)
    {
        NVGPaint shadow = boxShadow(vg, stack, x - 4.0F, y - 4.0F, w + 8.0F, h + 8.0F, radius + 2.0F, 12.0F, shadowInnerAlpha, 0);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x - 8.0F, y - 8.0F, w + 16.0F, h + 16.0F, radius + 6.0F);
        nvgFillPaint(vg, shadow);
        nvgFill(vg);

        fillRoundedRect(vg, x, y, w, h, radius, argb(stack, fillArgb));
        strokeRoundedRect(vg, x, y, w, h, radius, strokeWidth, argb(stack, strokeArgb));
    }

    public static void drawLabel(long vg, MemoryStack stack, int fontId, float x, float y, float size, String text, int r, int g, int b, int a, int align)
    {
        if (fontId < 0 || text == null)
        {
            return;
        }

        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, align);
        nvgFillColor(vg, rgba(stack, r, g, b, a));
        nvgText(vg, x, y, text);
    }

    public static float pixelRatio(int framebufferWidth, int windowWidth)
    {
        if (framebufferWidth <= 0 || windowWidth <= 0)
        {
            return 1.0F;
        }

        return (float)framebufferWidth / (float)windowWidth;
    }

    /**
     * Measure horizontal text advance.
     */
    public static float textWidth(long vg, int fontId, float size, String text, MemoryStack stack)
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
}
