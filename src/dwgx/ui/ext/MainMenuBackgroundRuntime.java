package dwgx.ui.ext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Orchestrates main-menu background scene + shader rendering and guards GL state.
 */
public final class MainMenuBackgroundRuntime
{
    private final MainMenuBackgroundScene scene;
    private MainMenuSplashShader shader;
    private boolean shaderRenderWarned;
    private boolean textureUnitWarned;

    public MainMenuBackgroundRuntime(MainMenuBackgroundScene scene)
    {
        if (scene == null)
        {
            throw new IllegalArgumentException("scene cannot be null");
        }

        this.scene = scene;
    }

    public MainMenuSplashShader getOrCreateShader()
    {
        if (this.shader == null)
        {
            this.shader = new MainMenuSplashShader();
        }

        return this.shader;
    }

    public void initializeScene()
    {
        this.scene.initialize();
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        return this.scene.handleKeyInput(typedChar, keyCode, sceneEnabled);
    }

    public void tickScene(boolean sceneEnabled)
    {
        this.scene.tick(sceneEnabled);
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        return this.scene.getOverlayHint(sceneEnabled);
    }

    public boolean renderSceneBackground(Minecraft mc, int screenWidth, int screenHeight, float zLevel, boolean sceneEnabled, Logger logger)
    {
        if (!sceneEnabled || mc == null)
        {
            return false;
        }

        if (screenWidth <= 0 || screenHeight <= 0)
        {
            return false;
        }

        MainMenuSplashShader shaderProgram = this.getOrCreateShader();
        this.scene.applyShaderUniforms(shaderProgram);
        boolean shaderBound = shaderProgram.begin(16777215, MainMenuSplashShader.TextureAlphaMode.IGNORE_SOURCE_ALPHA);

        if (!shaderBound)
        {
            return false;
        }

        try
        {
            this.guardTextureUnit(logger);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(Gui.optionsBackground);
            GlStateManager.disableCull();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldRenderer.pos(0.0D, (double)screenHeight, (double)zLevel).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos((double)screenWidth, (double)screenHeight, (double)zLevel).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos((double)screenWidth, 0.0D, (double)zLevel).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos(0.0D, 0.0D, (double)zLevel).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            return true;
        }
        catch (Throwable throwable)
        {
            if (!this.shaderRenderWarned && logger != null)
            {
                this.shaderRenderWarned = true;
                logger.warn("主菜单着色器背景渲染失败，已回退到天空盒。", throwable);
            }

            return false;
        }
        finally
        {
            shaderProgram.end();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void close()
    {
        if (this.shader != null)
        {
            this.shader.close();
            this.shader = null;
        }
    }

    private void guardTextureUnit(Logger logger)
    {
        int activeTexUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        if (activeTexUnit != OpenGlHelper.defaultTexUnit && !this.textureUnitWarned)
        {
            this.textureUnitWarned = true;

            if (logger != null)
            {
                logger.info(
                    "主菜单背景渲染前检测到活动纹理单元异常（active={} expected={}），已自动修正。",
                    Integer.valueOf(activeTexUnit),
                    Integer.valueOf(OpenGlHelper.defaultTexUnit)
                );
            }
        }
    }
}
