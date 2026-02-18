package client.render;

public final class DisplayMetrics
{
    private final int windowWidth;
    private final int windowHeight;
    private final float logicalWindowWidth;
    private final float logicalWindowHeight;
    private final int framebufferWidth;
    private final int framebufferHeight;
    private final float pixelRatio;

    public DisplayMetrics(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight)
    {
        this(windowWidth, windowHeight, (float)windowWidth, (float)windowHeight, framebufferWidth, framebufferHeight);
    }

    public DisplayMetrics(int windowWidth, int windowHeight, float logicalWindowWidth, float logicalWindowHeight, int framebufferWidth, int framebufferHeight)
    {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.logicalWindowWidth = sanitizeLogicalDimension(logicalWindowWidth, windowWidth);
        this.logicalWindowHeight = sanitizeLogicalDimension(logicalWindowHeight, windowHeight);
        this.framebufferWidth = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
        this.pixelRatio = resolvePixelRatio(this.logicalWindowWidth, this.logicalWindowHeight, framebufferWidth, framebufferHeight);
    }

    public int getWindowWidth()
    {
        return this.windowWidth;
    }

    public int getWindowHeight()
    {
        return this.windowHeight;
    }

    public float getLogicalWindowWidth()
    {
        return this.logicalWindowWidth;
    }

    public float getLogicalWindowHeight()
    {
        return this.logicalWindowHeight;
    }

    public int getFramebufferWidth()
    {
        return this.framebufferWidth;
    }

    public int getFramebufferHeight()
    {
        return this.framebufferHeight;
    }

    public float getPixelRatio()
    {
        return this.pixelRatio;
    }

    private static float sanitizeLogicalDimension(float logicalDimension, int fallbackIntDimension)
    {
        if (!Float.isNaN(logicalDimension) && !Float.isInfinite(logicalDimension) && logicalDimension > 0.0F)
        {
            return logicalDimension;
        }

        return fallbackIntDimension > 0 ? (float)fallbackIntDimension : 1.0F;
    }

    private static float resolvePixelRatio(float logicalWidth, float logicalHeight, int framebufferWidth, int framebufferHeight)
    {
        float widthRatio = framebufferWidth > 0 ? (float)framebufferWidth / logicalWidth : 1.0F;
        float heightRatio = framebufferHeight > 0 ? (float)framebufferHeight / logicalHeight : widthRatio;
        float ratio = Math.max(widthRatio, heightRatio);

        if (Float.isNaN(ratio) || Float.isInfinite(ratio) || ratio <= 0.0F)
        {
            return 1.0F;
        }

        int guard = 0;

        while (guard < 8 && (isCoverageShort(logicalWidth, ratio, framebufferWidth) || isCoverageShort(logicalHeight, ratio, framebufferHeight)))
        {
            ratio = Math.nextUp(ratio);
            ++guard;
        }

        return ratio;
    }

    private static boolean isCoverageShort(float logicalDimension, float ratio, int framebufferDimension)
    {
        if (framebufferDimension <= 0 || logicalDimension <= 0.0F || Float.isNaN(logicalDimension) || Float.isInfinite(logicalDimension))
        {
            return false;
        }

        return (int)Math.floor((double)(logicalDimension * ratio)) < framebufferDimension;
    }
}
