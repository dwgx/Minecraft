package client.render;

public final class DisplayMetrics
{
    private final int windowWidth;
    private final int windowHeight;
    private final int framebufferWidth;
    private final int framebufferHeight;
    private final float pixelRatio;

    public DisplayMetrics(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight)
    {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.framebufferWidth = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
        this.pixelRatio = windowWidth <= 0 ? 1.0F : (float)framebufferWidth / (float)windowWidth;
    }

    public int getWindowWidth()
    {
        return this.windowWidth;
    }

    public int getWindowHeight()
    {
        return this.windowHeight;
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
}
