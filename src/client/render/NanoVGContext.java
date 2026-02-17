package client.render;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVG.nvgIntersectScissor;
import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

/**
 * Thin wrapper around the NanoVG frame lifecycle.
 */
public final class NanoVGContext
{
    private final long vg;
    private boolean frameActive;

    public NanoVGContext(long vg)
    {
        this.vg = vg;
    }

    public long getHandle()
    {
        return this.vg;
    }

    public boolean isFrameActive()
    {
        return this.frameActive;
    }

    public void beginFrame(DisplayMetrics metrics)
    {
        this.beginFrame(metrics.getWindowWidth(), metrics.getWindowHeight(), metrics.getPixelRatio());
    }

    public void beginFrame(int windowWidth, int windowHeight, float pixelRatio)
    {
        nvgBeginFrame(this.vg, windowWidth, windowHeight, pixelRatio);
        this.frameActive = true;
    }

    public void endFrame()
    {
        nvgEndFrame(this.vg);
        this.frameActive = false;
    }

    public void save()
    {
        nvgSave(this.vg);
    }

    public void restore()
    {
        nvgRestore(this.vg);
    }

    public void scissor(float x, float y, float width, float height)
    {
        nvgScissor(this.vg, x, y, width, height);
    }

    public void intersectScissor(float x, float y, float width, float height)
    {
        nvgIntersectScissor(this.vg, x, y, width, height);
    }

    public void resetScissor()
    {
        nvgResetScissor(this.vg);
    }
}
