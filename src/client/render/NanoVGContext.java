package client.render;

import net.minecraft.client.renderer.OpenGlHelper;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVG.nvgGlobalAlpha;
import static org.lwjgl.nanovg.NanoVG.nvgIntersectScissor;
import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgResetTransform;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;
import static org.lwjgl.nanovg.NanoVG.nvgScale;
import static org.lwjgl.nanovg.NanoVG.nvgTranslate;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;

/**
 * NanoVG 帧生命周期封装。
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
        if (this.vg == 0L)
        {
            this.frameActive = false;
            return;
        }

        if (this.frameActive)
        {
            this.endFrame();
        }

        int safeWidth = Math.max(1, windowWidth);
        int safeHeight = Math.max(1, windowHeight);
        float safePixelRatio = Float.isNaN(pixelRatio) || Float.isInfinite(pixelRatio) || pixelRatio <= 0.0F ? 1.0F : pixelRatio;

        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_STENCIL_TEST);
        nvgBeginFrame(this.vg, safeWidth, safeHeight, safePixelRatio);
        nvgResetTransform(this.vg);
        nvgResetScissor(this.vg);
        nvgGlobalAlpha(this.vg, 1.0F);
        this.frameActive = true;
    }

    public void endFrame()
    {
        if (!this.frameActive || this.vg == 0L)
        {
            this.frameActive = false;
            return;
        }

        nvgResetTransform(this.vg);
        nvgResetScissor(this.vg);
        nvgGlobalAlpha(this.vg, 1.0F);
        nvgEndFrame(this.vg);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_STENCIL_TEST);
        this.resetLegacyPipelineState();
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

    public void resetTransform()
    {
        nvgResetTransform(this.vg);
    }

    public void translate(float x, float y)
    {
        nvgTranslate(this.vg, x, y);
    }

    public void scale(float sx, float sy)
    {
        nvgScale(this.vg, sx, sy);
    }

    private void resetLegacyPipelineState()
    {
        // NanoVG(GL3) may leave program/VBO bindings active; fixed-function MC UI/world expects clean state.
        OpenGlHelper.glUseProgram(0);
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        OpenGlHelper.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);

        try
        {
            org.lwjgl.opengl.GL30.glBindVertexArray(0);
        }
        catch (Throwable ignored)
        {
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
