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
    private static final float MAX_STABLE_RADIUS = 28.0F;
    private static final float[] TEXT_BOUNDS_BUFFER = new float[4];

    private NanoRenderUtils()
    {
    }

    public static NVGColor rgba(MemoryStack stack, int r, int g, int b, int a)
    {
        NVGColor color = NVGColor.mallocStack(stack);
        nvgRGBA((byte)clamp255(r), (byte)clamp255(g), (byte)clamp255(b), (byte)clamp255(a), color);
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
        float width = safeDimension(w);
        float height = safeDimension(h);

        if (!isFinite(x) || !isFinite(y) || width <= 0.0F || height <= 0.0F || color == null)
        {
            return;
        }

        float r = stableRadius(radius, width, height);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, r);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void fillRoundedRectGradient(long vg, MemoryStack stack, float x, float y, float w, float h, float radius, int fromArgb, int toArgb, boolean vertical)
    {
        float width = safeDimension(w);
        float height = safeDimension(h);

        if (!isFinite(x) || !isFinite(y) || width <= 0.0F || height <= 0.0F)
        {
            return;
        }

        NVGPaint paint = NVGPaint.mallocStack(stack);
        NVGColor from = argb(stack, fromArgb);
        NVGColor to = argb(stack, toArgb);
        float r = stableRadius(radius, width, height);

        if (vertical)
        {
            nvgLinearGradient(vg, x, y, x, y + height, from, to, paint);
        }
        else
        {
            nvgLinearGradient(vg, x, y, x + width, y, from, to, paint);
        }

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, r);
        nvgFillPaint(vg, paint);
        nvgFill(vg);
    }

    public static void fillRect(long vg, float x, float y, float w, float h, NVGColor color)
    {
        float width = safeDimension(w);
        float height = safeDimension(h);

        if (!isFinite(x) || !isFinite(y) || width <= 0.0F || height <= 0.0F || color == null)
        {
            return;
        }

        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public static void strokeRoundedRect(long vg, float x, float y, float w, float h, float radius, float strokeWidth, NVGColor color)
    {
        float width = safeDimension(w);
        float height = safeDimension(h);

        if (!isFinite(x) || !isFinite(y) || width <= 0.0F || height <= 0.0F || color == null || strokeWidth <= 0.0F)
        {
            return;
        }

        float r = stableRadius(radius, width, height);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, r);
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
        float width = safeDimension(w);
        float height = safeDimension(h);

        if (!isFinite(x) || !isFinite(y) || width <= 0.0F || height <= 0.0F)
        {
            return;
        }

        float r = stableRadius(radius, width, height);
        NVGPaint shadow = boxShadow(vg, stack, x - 4.0F, y - 4.0F, width + 8.0F, height + 8.0F, stableRadius(r + 2.0F, width + 8.0F, height + 8.0F), 12.0F, shadowInnerAlpha, 0);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x - 8.0F, y - 8.0F, width + 16.0F, height + 16.0F, stableRadius(r + 6.0F, width + 16.0F, height + 16.0F));
        nvgFillPaint(vg, shadow);
        nvgFill(vg);

        fillRoundedRect(vg, x, y, width, height, r, argb(stack, fillArgb));
        strokeRoundedRect(vg, x, y, width, height, r, strokeWidth, argb(stack, strokeArgb));
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

    public static void drawLabel(long vg, MemoryStack stack, int fontId, float x, float y, float size, String text, int argb, int align)
    {
        drawLabel(vg, stack, fontId, x, y, size, text, (argb >>> 16) & 255, (argb >>> 8) & 255, argb & 255, (argb >>> 24) & 255, align);
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
    public static float textWidth(long vg, int fontId, float size, String text)
    {
        if (fontId < 0 || text == null)
        {
            return 0.0F;
        }

        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextBounds(vg, 0.0F, 0.0F, text, TEXT_BOUNDS_BUFFER);
        return TEXT_BOUNDS_BUFFER[2] - TEXT_BOUNDS_BUFFER[0];
    }

    public static int withAlpha(int argb, int alpha)
    {
        return (clamp255(alpha) & 255) << 24 | argb & 16777215;
    }

    public static int mulAlpha(int argb, float multiplier)
    {
        float m = Math.max(0.0F, Math.min(1.0F, multiplier));
        int a = argb >>> 24 & 255;
        return withAlpha(argb, Math.round((float)a * m));
    }

    public static int clamp255(int value)
    {
        return Math.max(0, Math.min(255, value));
    }

    public static float stableRadius(float radius, float width, float height)
    {
        if (!isFinite(radius) || !isFinite(width) || !isFinite(height))
        {
            return 0.0F;
        }

        float w = Math.max(0.0F, width);
        float h = Math.max(0.0F, height);
        float maxBySize = Math.max(0.0F, Math.min(w, h) * 0.5F - 0.75F);
        float cap = Math.min(MAX_STABLE_RADIUS, maxBySize);
        return Math.max(0.0F, Math.min(Math.max(0.0F, radius), cap));
    }

    private static float safeDimension(float value)
    {
        return isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    static boolean isFinite(float value)
    {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
