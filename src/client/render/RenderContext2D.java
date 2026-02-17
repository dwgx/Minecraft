package client.render;

public final class RenderContext2D
{
    private final NanoVGContext nanoVGContext;
    private final DisplayMetrics metrics;
    private final float partialTicks;

    public RenderContext2D(NanoVGContext nanoVGContext, DisplayMetrics metrics, float partialTicks)
    {
        this.nanoVGContext = nanoVGContext;
        this.metrics = metrics;
        this.partialTicks = partialTicks;
    }

    public NanoVGContext getNanoVG()
    {
        return this.nanoVGContext;
    }

    public DisplayMetrics getMetrics()
    {
        return this.metrics;
    }

    public float getPartialTicks()
    {
        return this.partialTicks;
    }
}
