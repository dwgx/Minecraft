package dwgx.ui.ext;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * Orchestrates input/tick/update and renders one main-menu background backend.
 *
 * Supported background backends:
 * - shader scene (builtin mini-games and external GLSL)
 * - static image
 * - video stream (provider-based)
 */
public final class MainMenuBackgroundRuntime
{
    private static final String IMAGE_TEXTURE_KEY = "dwgx_mainmenu_background_image";
    private static final String VIDEO_TEXTURE_KEY = "dwgx_mainmenu_background_video";

    private MainMenuBackgroundScene scene;
    private MainMenuSplashShader shader;
    private String shaderSourceKey = "";
    private boolean shaderRenderWarned;

    private String imageSourceKey = "";
    private DynamicTexture imageTexture;
    private ResourceLocation imageTextureLocation;
    private boolean imageLoadWarned;
    private boolean imageRenderWarned;

    private final AtomicReference<BufferedImage> asyncImageResult = new AtomicReference<>(null);
    private volatile boolean asyncImageLoading;
    private volatile String asyncImageKey = "";

    private String videoSourceKey = "";
    private UiExtensionManager.MainMenuVideoFrameProvider videoFrameProvider;
    private DynamicTexture videoTexture;
    private ResourceLocation videoTextureLocation;
    private int videoTextureWidth = -1;
    private int videoTextureHeight = -1;
    private boolean videoProviderWarned;
    private boolean videoRenderWarned;

    public MainMenuBackgroundRuntime(MainMenuBackgroundScene scene)
    {
        this.setScene(scene);
    }

    public void setScene(MainMenuBackgroundScene scene)
    {
        if (scene == null)
        {
            throw new IllegalArgumentException("scene cannot be null");
        }

        this.scene = scene;
        this.refreshShaderSourceIfNeeded();
    }

    public MainMenuSplashShader getOrCreateShader()
    {
        this.refreshShaderSourceIfNeeded();

        if (this.shader == null)
        {
            this.shader = new MainMenuSplashShader(this.scene == null ? null : this.scene.fragmentShaderSource());
        }

        return this.shader;
    }

    public void initializeScene()
    {
        if (this.scene != null)
        {
            this.scene.initialize();
        }
    }

    public boolean handleKeyInput(char typedChar, int keyCode, boolean sceneEnabled)
    {
        return this.scene != null && this.scene.handleKeyInput(typedChar, keyCode, sceneEnabled);
    }

    public void tickScene(boolean sceneEnabled)
    {
        if (this.scene != null)
        {
            this.scene.tick(sceneEnabled);
        }
    }

    public String getOverlayHint(boolean sceneEnabled)
    {
        if (!sceneEnabled)
        {
            return null;
        }

        UiExtensionManager.MainMenuBackgroundMode mode = UiExtensionManager.getMainMenuBackgroundMode();

        if (mode == UiExtensionManager.MainMenuBackgroundMode.STATIC_IMAGE)
        {
            String source = UiExtensionManager.getMainMenuBackgroundImagePath();
            return source == null || source.trim().isEmpty() ? "Image: not configured" : "Image: " + source;
        }

        if (mode == UiExtensionManager.MainMenuBackgroundMode.VIDEO_STREAM)
        {
            if (this.videoFrameProvider != null)
            {
                String hint = this.videoFrameProvider.overlayHint();

                if (hint != null && !hint.isEmpty())
                {
                    return hint;
                }
            }

            String source = UiExtensionManager.getMainMenuBackgroundVideoPath();
            return source == null || source.trim().isEmpty() ? "Video: not configured" : "Video: " + source;
        }

        return this.scene == null ? null : this.scene.getOverlayHint(true);
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

        UiExtensionManager.MainMenuBackgroundOption option = UiExtensionManager.getMainMenuBackgroundOption(UiExtensionManager.getMainMenuBackgroundMode());

        if (option == null || option.content() == UiExtensionManager.MainMenuBackgroundContent.SKYBOX)
        {
            return false;
        }

        if (option.content() == UiExtensionManager.MainMenuBackgroundContent.SHADER_SCENE
            && this.scene instanceof MainMenuBackgroundCustomRenderer)
        {
            try
            {
                boolean rendered = ((MainMenuBackgroundCustomRenderer)this.scene)
                    .renderCustomBackground(mc, screenWidth, screenHeight, zLevel, logger);

                if (rendered)
                {
                    return true;
                }
            }
            catch (Throwable throwable)
            {
                if (!this.shaderRenderWarned && logger != null)
                {
                    this.shaderRenderWarned = true;
                    logger.warn("Main-menu custom background render failed, fallback to shader/skybox.", throwable);
                }
            }
        }

        if (option.content() == UiExtensionManager.MainMenuBackgroundContent.STATIC_IMAGE)
        {
            return this.renderStaticImageBackground(mc, screenWidth, screenHeight, zLevel, logger);
        }

        if (option.content() == UiExtensionManager.MainMenuBackgroundContent.VIDEO_STREAM)
        {
            return this.renderVideoBackground(mc, screenWidth, screenHeight, zLevel, logger);
        }

        return this.renderShaderSceneBackground(mc, screenWidth, screenHeight, zLevel, logger);
    }

    public void close()
    {
        if (this.scene instanceof MainMenuBackgroundCustomRenderer)
        {
            try
            {
                ((MainMenuBackgroundCustomRenderer)this.scene).closeCustomBackground();
            }
            catch (Throwable throwable)
            {
                ;
            }
        }

        this.closeShaderOnly();
        this.closeImageOnly(Minecraft.getMinecraft());
        this.closeVideoOnly(Minecraft.getMinecraft());
        this.scene = null;
        this.shaderSourceKey = "";
    }

    private boolean renderShaderSceneBackground(Minecraft mc, int screenWidth, int screenHeight, float zLevel, Logger logger)
    {
        if (this.scene == null)
        {
            return false;
        }

        this.refreshShaderSourceIfNeeded();
        MainMenuSplashShader shaderProgram = this.getOrCreateShader();
        this.scene.applyShaderUniforms(shaderProgram);
        boolean shaderBound = shaderProgram.begin(16777215, this.scene.textureAlphaMode());

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
            this.drawFullscreenQuad(screenWidth, screenHeight, zLevel);
            return true;
        }
        catch (Throwable throwable)
        {
            if (!this.shaderRenderWarned && logger != null)
            {
                this.shaderRenderWarned = true;
                logger.warn("Main-menu shader background render failed, fallback to skybox.", throwable);
            }

            return false;
        }
        finally
        {
            shaderProgram.end();
            this.restoreRenderState();
        }
    }

    private boolean renderStaticImageBackground(Minecraft mc, int screenWidth, int screenHeight, float zLevel, Logger logger)
    {
        try
        {
            if (!this.ensureImageTexture(mc, UiExtensionManager.getMainMenuBackgroundImagePath(), logger))
            {
                return false;
            }

            return this.renderTexturedBackground(mc, this.imageTextureLocation, screenWidth, screenHeight, zLevel, logger);
        }
        catch (Throwable throwable)
        {
            if (!this.imageRenderWarned && logger != null)
            {
                this.imageRenderWarned = true;
                logger.warn("Main-menu static image background render failed, fallback to skybox.", throwable);
            }

            return false;
        }
    }

    private boolean renderVideoBackground(Minecraft mc, int screenWidth, int screenHeight, float zLevel, Logger logger)
    {
        try
        {
            if (!this.ensureVideoProvider(UiExtensionManager.getMainMenuBackgroundVideoPath(), logger))
            {
                return false;
            }

            this.videoFrameProvider.tick();

            if (!this.videoFrameProvider.hasFrame())
            {
                return false;
            }

            int frameWidth = this.videoFrameProvider.frameWidth();
            int frameHeight = this.videoFrameProvider.frameHeight();
            int[] frame = this.videoFrameProvider.copyFrameArgb();

            if (frameWidth <= 0 || frameHeight <= 0 || frame == null || frame.length < frameWidth * frameHeight)
            {
                return false;
            }

            this.ensureVideoTexture(mc, frameWidth, frameHeight);
            int[] textureData = this.videoTexture.getTextureData();
            int copyLength = Math.min(textureData.length, frameWidth * frameHeight);
            System.arraycopy(frame, 0, textureData, 0, copyLength);
            this.videoTexture.updateDynamicTexture();
            return this.renderTexturedBackground(mc, this.videoTextureLocation, screenWidth, screenHeight, zLevel, logger);
        }
        catch (Throwable throwable)
        {
            if (!this.videoRenderWarned && logger != null)
            {
                this.videoRenderWarned = true;
                logger.warn("Main-menu video background render failed, fallback to skybox.", throwable);
            }

            return false;
        }
    }

    private boolean renderTexturedBackground(Minecraft mc, ResourceLocation texture, int screenWidth, int screenHeight, float zLevel, Logger logger)
    {
        if (texture == null)
        {
            return false;
        }

        try
        {
            this.guardTextureUnit(logger);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(texture);
            this.drawFullscreenQuad(screenWidth, screenHeight, zLevel);
            return true;
        }
        finally
        {
            this.restoreRenderState();
        }
    }

    private void drawFullscreenQuad(int screenWidth, int screenHeight, float zLevel)
    {
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
    }

    private void restoreRenderState()
    {
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean ensureImageTexture(Minecraft mc, String sourcePath, Logger logger)
    {
        String normalized = sourcePath == null ? "" : sourcePath.trim();

        if (normalized.isEmpty())
        {
            this.closeImageOnly(mc);
            return false;
        }

        if (this.imageTextureLocation != null && normalized.equals(this.imageSourceKey))
        {
            return true;
        }

        // Check if an async load just completed
        BufferedImage asyncResult = this.asyncImageResult.getAndSet(null);

        if (asyncResult != null && normalized.equals(this.asyncImageKey))
        {
            this.closeImageOnly(mc);
            this.imageSourceKey = normalized;
            this.asyncImageLoading = false;

            try
            {
                this.imageTexture = new DynamicTexture(asyncResult.getWidth(), asyncResult.getHeight());
                int[] textureData = this.imageTexture.getTextureData();
                asyncResult.getRGB(0, 0, asyncResult.getWidth(), asyncResult.getHeight(), textureData, 0, asyncResult.getWidth());
                this.imageTexture.updateDynamicTexture();
                this.imageTextureLocation = mc.getTextureManager().getDynamicTextureLocation(IMAGE_TEXTURE_KEY, this.imageTexture);
                this.imageLoadWarned = false;
                return true;
            }
            catch (Throwable throwable)
            {
                if (!this.imageLoadWarned && logger != null)
                {
                    this.imageLoadWarned = true;
                    logger.warn("Main-menu image texture upload failed: {}", normalized, throwable);
                }

                return false;
            }
        }

        // For HTTP URLs, load asynchronously to avoid blocking the render thread
        boolean isRemote = normalized.startsWith("http://") || normalized.startsWith("https://");

        if (isRemote)
        {
            if (this.asyncImageLoading && normalized.equals(this.asyncImageKey))
            {
                return false;
            }

            this.closeImageOnly(mc);
            this.imageSourceKey = normalized;
            this.asyncImageLoading = true;
            this.asyncImageKey = normalized;
            final String loadKey = normalized;
            Thread loader = new Thread(new Runnable()
            {
                public void run()
                {
                    try (InputStream input = new URL(loadKey).openStream())
                    {
                        BufferedImage image = ImageIO.read(input);

                        if (image != null && loadKey.equals(asyncImageKey))
                        {
                            asyncImageResult.set(image);
                        }
                    }
                    catch (Throwable ignored)
                    {
                    }
                    finally
                    {
                        if (loadKey.equals(asyncImageKey))
                        {
                            asyncImageLoading = false;
                        }
                    }
                }
            }, "MainMenu-ImageLoader");
            loader.setDaemon(true);
            loader.start();
            return false;
        }

        // Local file — also load asynchronously to avoid blocking the render thread
        // (large JPEG decoding can take 200-500ms)
        File localFile = new File(normalized);

        if (localFile.isFile())
        {
            if (this.asyncImageLoading && normalized.equals(this.asyncImageKey))
            {
                return false;
            }

            this.closeImageOnly(mc);
            this.imageSourceKey = normalized;
            this.asyncImageLoading = true;
            this.asyncImageKey = normalized;
            final String loadKey = normalized;
            Thread loader = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        BufferedImage image = ImageIO.read(new File(loadKey));

                        if (image != null && loadKey.equals(asyncImageKey))
                        {
                            asyncImageResult.set(image);
                        }
                    }
                    catch (Throwable ignored)
                    {
                    }
                    finally
                    {
                        if (loadKey.equals(asyncImageKey))
                        {
                            asyncImageLoading = false;
                        }
                    }
                }
            }, "MainMenu-ImageLoader");
            loader.setDaemon(true);
            loader.start();
            return false;
        }

        // Resource path — load synchronously (fast, from jar)
        this.closeImageOnly(mc);
        this.imageSourceKey = normalized;

        try
        {
            BufferedImage image = this.readBackgroundImage(normalized, mc);

            if (image == null)
            {
                if (!this.imageLoadWarned && logger != null)
                {
                    this.imageLoadWarned = true;
                    logger.warn("Main-menu static image background load failed: {}", normalized);
                }

                return false;
            }

            this.imageTexture = new DynamicTexture(image.getWidth(), image.getHeight());
            int[] textureData = this.imageTexture.getTextureData();
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), textureData, 0, image.getWidth());
            this.imageTexture.updateDynamicTexture();
            this.imageTextureLocation = mc.getTextureManager().getDynamicTextureLocation(IMAGE_TEXTURE_KEY, this.imageTexture);
            this.imageLoadWarned = false;
            return true;
        }
        catch (Throwable throwable)
        {
            if (!this.imageLoadWarned && logger != null)
            {
                this.imageLoadWarned = true;
                logger.warn("Main-menu static image background load failed: {}", normalized, throwable);
            }

            return false;
        }
    }

    private BufferedImage readBackgroundImage(String sourcePath, Minecraft mc) throws Exception
    {
        File file = new File(sourcePath);

        if (file.isFile())
        {
            return ImageIO.read(file);
        }

        if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://"))
        {
            try (InputStream input = new URL(sourcePath).openStream())
            {
                return ImageIO.read(input);
            }
        }

        String resourcePath = sourcePath;

        if (resourcePath.startsWith("resource:"))
        {
            resourcePath = resourcePath.substring("resource:".length()).trim();
        }

        ResourceLocation location = new ResourceLocation(resourcePath);

        try (InputStream input = mc.getResourceManager().getResource(location).getInputStream())
        {
            return ImageIO.read(input);
        }
    }

    private boolean ensureVideoProvider(String sourcePath, Logger logger)
    {
        String normalized = sourcePath == null ? "" : sourcePath.trim();

        if (normalized.isEmpty())
        {
            this.closeVideoOnly(Minecraft.getMinecraft());
            return false;
        }

        if (!normalized.equals(this.videoSourceKey))
        {
            this.closeVideoOnly(Minecraft.getMinecraft());
            this.videoSourceKey = normalized;
        }

        if (this.videoFrameProvider != null)
        {
            return true;
        }

        this.videoFrameProvider = UiExtensionManager.createMainMenuVideoFrameProvider(normalized);

        if (this.videoFrameProvider == null)
        {
            if (!this.videoProviderWarned && logger != null)
            {
                this.videoProviderWarned = true;
                logger.info(
                    "Main-menu video provider is not configured. Set one via UiExtensionManager.setMainMenuVideoFrameProviderFactory(...)."
                );
            }

            return false;
        }

        this.videoProviderWarned = false;
        return true;
    }

    private void ensureVideoTexture(Minecraft mc, int width, int height)
    {
        if (this.videoTexture != null && this.videoTextureWidth == width && this.videoTextureHeight == height)
        {
            return;
        }

        this.closeVideoTextureOnly(mc);
        this.videoTexture = new DynamicTexture(width, height);
        this.videoTextureLocation = mc.getTextureManager().getDynamicTextureLocation(VIDEO_TEXTURE_KEY, this.videoTexture);
        this.videoTextureWidth = width;
        this.videoTextureHeight = height;
    }

    private void refreshShaderSourceIfNeeded()
    {
        if (this.scene == null)
        {
            return;
        }

        String source = this.scene.fragmentShaderSource();
        String normalized = source == null ? "" : source;

        if (!normalized.equals(this.shaderSourceKey))
        {
            this.closeShaderOnly();
            this.shaderSourceKey = normalized;
        }
    }

    private void closeShaderOnly()
    {
        if (this.shader != null)
        {
            this.shader.close();
            this.shader = null;
        }
    }

    private void closeImageOnly(Minecraft mc)
    {
        if (mc != null && this.imageTextureLocation != null)
        {
            mc.getTextureManager().deleteTexture(this.imageTextureLocation);
        }

        if (this.imageTexture != null)
        {
            this.imageTexture.deleteGlTexture();
        }

        this.imageTexture = null;
        this.imageTextureLocation = null;
        this.imageSourceKey = "";
        this.asyncImageResult.set(null);
        this.asyncImageLoading = false;
        this.asyncImageKey = "";
    }

    private void closeVideoTextureOnly(Minecraft mc)
    {
        if (mc != null && this.videoTextureLocation != null)
        {
            mc.getTextureManager().deleteTexture(this.videoTextureLocation);
        }

        if (this.videoTexture != null)
        {
            this.videoTexture.deleteGlTexture();
        }

        this.videoTexture = null;
        this.videoTextureLocation = null;
        this.videoTextureWidth = -1;
        this.videoTextureHeight = -1;
    }

    private void closeVideoOnly(Minecraft mc)
    {
        this.closeVideoTextureOnly(mc);

        if (this.videoFrameProvider != null)
        {
            try
            {
                this.videoFrameProvider.close();
            }
            catch (Throwable throwable)
            {
                ;
            }
        }

        this.videoFrameProvider = null;
        this.videoSourceKey = "";
    }

    private void guardTextureUnit(Logger logger)
    {
        // Avoid glGetInteger — it stalls the GPU pipeline.
        // Instead, just ensure the correct unit is active unconditionally.
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
