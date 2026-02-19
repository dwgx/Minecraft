package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import dwgx.ui.MainMenuSession;
import dwgx.ui.ext.MainMenuSplashShader;
import dwgx.ui.ext.UiExtensionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;

public class GuiMainMenu extends GuiScreen implements GuiYesNoCallback
{
    private static final AtomicInteger field_175373_f = new AtomicInteger(0);
    private static final Logger logger = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    /** Counts the number of screen updates. */
    private float updateCounter;

    /** The splash message. */
    private String splashText;
    private GuiButton buttonResetDemo;

    /** Timer used to rotate the panorama, increases every tick. */
    private int panoramaTimer;

    /**
     * Texture allocated for the current viewport of the main menu's panorama background.
     */
    private DynamicTexture viewportTexture;
    private boolean field_175375_v = true;

    /**
     * The Object object utilized as a thread lock when performing non thread-safe operations
     */
    private final Object threadLock = new Object();

    /** OpenGL graphics card warning. */
    private String openGLWarning1;

    /** OpenGL graphics card warning. */
    private String openGLWarning2;

    /** Link to the Mojang Support about minimum requirements */
    private String openGLWarningLink;
    private static final ResourceLocation splashTexts = new ResourceLocation("texts/splashes.txt");
    private static final ResourceLocation minecraftTitleTextures = new ResourceLocation("textures/gui/title/minecraft.png");

    /** An array of all the paths to the panorama pictures. */
    private static final ResourceLocation[] titlePanoramaPaths = new ResourceLocation[] {new ResourceLocation("textures/gui/title/background/panorama_0.png"), new ResourceLocation("textures/gui/title/background/panorama_1.png"), new ResourceLocation("textures/gui/title/background/panorama_2.png"), new ResourceLocation("textures/gui/title/background/panorama_3.png"), new ResourceLocation("textures/gui/title/background/panorama_4.png"), new ResourceLocation("textures/gui/title/background/panorama_5.png")};
    public static final String field_96138_a = "请点击 " + EnumChatFormatting.UNDERLINE + "这里" + EnumChatFormatting.RESET + " 查看更多信息。";
    private int field_92024_r;
    private int field_92023_s;
    private int field_92022_t;
    private int field_92021_u;
    private int field_92020_v;
    private int field_92019_w;
    private ResourceLocation backgroundTexture;

    /** Minecraft Realms button. */
    private GuiButton realmsButton;
    private boolean field_183502_L;
    private GuiScreen field_183503_M;
    private MainMenuSession dwgxSession;
    private MainMenuSplashShader splashShader;
    private static final float FLAPPY_PIPE_WIDTH = 26.0F;
    private static final float FLAPPY_PIPE_BOTTOM = 39.0F;
    private static final float FLAPPY_VERT_PIPE_DISTANCE = 55.0F;
    private static final float FLAPPY_PIPE_MIN = 20.0F;
    private static final float FLAPPY_PIPE_MAX = 70.0F;
    private static final float FLAPPY_PIPE_PER_CYCLE = 8.0F;
    private static final float FLAPPY_HORZ_PIPE_DISTANCE = 100.0F;
    private static final float FLAPPY_BIRD_X = 105.0F;
    private static final float FLAPPY_BIRD_WIDTH = 14.0F;
    private static final float FLAPPY_BIRD_HEIGHT = 12.0F;
    private static final float FLAPPY_START_Y = 110.0F;
    private static final float FLAPPY_GRAVITY = -180.0F;
    private static final float FLAPPY_FLAP_VELOCITY = 78.0F;
    private float flappyTick;
    private float flappyBirdY = FLAPPY_START_Y;
    private float flappyVelocity;
    private float flappyWingFrame;
    private boolean flappyAlive = true;
    private int flappyScore;
    private int flappyBestScore;
    private long flappyLastUpdateMs;
    private boolean flappyJumpQueued;

    public GuiMainMenu()
    {
        this.openGLWarning2 = field_96138_a;
        this.field_183502_L = false;
        this.splashText = "missingno";
        BufferedReader bufferedreader = null;

        try
        {
            List<String> list = Lists.<String>newArrayList();
            bufferedreader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(splashTexts).getInputStream(), Charsets.UTF_8));
            String s;

            while ((s = bufferedreader.readLine()) != null)
            {
                s = s.trim();

                if (!s.isEmpty())
                {
                    list.add(s);
                }
            }

            if (!list.isEmpty())
            {
                while (true)
                {
                    this.splashText = (String)list.get(RANDOM.nextInt(list.size()));

                    if (this.splashText.hashCode() != 125780783)
                    {
                        break;
                    }
                }
            }
        }
        catch (IOException var12)
        {
            ;
        }
        finally
        {
            if (bufferedreader != null)
            {
                try
                {
                    bufferedreader.close();
                }
                catch (IOException var11)
                {
                    ;
                }
            }
        }

        this.updateCounter = RANDOM.nextFloat();
        this.openGLWarning1 = "";

        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported())
        {
            this.openGLWarning1 = "当前显卡或驱动对 OpenGL / 着色器支持不足，主菜单特效可能受限。";
            this.openGLWarning2 = "点击这里查看官方最低配置和驱动建议。";
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game";
        }
    }

    private boolean func_183501_a()
    {
        return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.field_183503_M != null;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        ++this.panoramaTimer;

        if (this.func_183501_a())
        {
            this.field_183503_M.updateScreen();
        }
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_SPACE)
        {
            this.queueFlappyJump();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        this.viewportTexture = new DynamicTexture(256, 256);
        this.backgroundTexture = this.mc.getTextureManager().getDynamicTextureLocation("background", this.viewportTexture);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24)
        {
            this.splashText = "Merry X-mas!";
        }
        else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1)
        {
            this.splashText = "Happy new year!";
        }
        else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31)
        {
            this.splashText = "OOoooOOOoooo! Spooky!";
        }

        int i = 24;
        int j = this.height / 4 + 48;

        if (this.mc.isDemo())
        {
            this.addDemoButtons(j, 24);
        }
        else
        {
            this.addSingleplayerMultiplayerButtons(j, 24);
        }

        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, j + 72 + 12, 98, 20, I18n.format("menu.options", new Object[0])));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 2, j + 72 + 12, 98, 20, I18n.format("menu.quit", new Object[0])));
        this.buttonList.add(new GuiButtonLanguage(5, this.width / 2 - 124, j + 72 + 12));

        synchronized (this.threadLock)
        {
            this.field_92023_s = this.fontRendererObj.getStringWidth(this.openGLWarning1);
            this.field_92024_r = this.fontRendererObj.getStringWidth(this.openGLWarning2);
            int k = Math.max(this.field_92023_s, this.field_92024_r);
            this.field_92022_t = (this.width - k) / 2;
            this.field_92021_u = ((GuiButton)this.buttonList.get(0)).yPosition - 24;
            this.field_92020_v = this.field_92022_t + k;
            this.field_92019_w = this.field_92021_u + 24;
        }

        this.mc.setConnectedToRealms(false);

        if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.field_183502_L)
        {
            RealmsBridge realmsbridge = new RealmsBridge();
            this.field_183503_M = realmsbridge.getNotificationScreen(this);
            this.field_183502_L = true;
        }

        if (this.func_183501_a())
        {
            this.field_183503_M.setGuiSize(this.width, this.height);
            this.field_183503_M.initGui();
        }

        if (this.dwgxSession == null)
        {
            this.dwgxSession = new MainMenuSession(this.mc);
        }

        this.dwgxSession.setScreenSize(this.width, this.height);

        if (this.flappyLastUpdateMs == 0L)
        {
            this.resetFlappyGame();
        }
    }

    /**
     * Adds Singleplayer and Multiplayer buttons on Main Menu for players who have bought the game.
     */
    private void addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_)
    {
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, p_73969_1_, I18n.format("menu.singleplayer", new Object[0])));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 1, I18n.format("menu.multiplayer", new Object[0])));
        this.buttonList.add(this.realmsButton = new GuiButton(14, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, I18n.format("menu.online", new Object[0])));
    }

    /**
     * Adds Demo buttons on Main Menu for players who are playing Demo.
     */
    private void addDemoButtons(int p_73972_1_, int p_73972_2_)
    {
        this.buttonList.add(new GuiButton(11, this.width / 2 - 100, p_73972_1_, I18n.format("menu.playdemo", new Object[0])));
        this.buttonList.add(this.buttonResetDemo = new GuiButton(12, this.width / 2 - 100, p_73972_1_ + p_73972_2_ * 1, I18n.format("menu.resetdemo", new Object[0])));
        ISaveFormat isaveformat = this.mc.getSaveLoader();
        WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

        if (worldinfo == null)
        {
            this.buttonResetDemo.enabled = false;
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }

        if (button.id == 5)
        {
            this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
        }

        if (button.id == 1)
        {
            this.mc.displayGuiScreen(new GuiSelectWorld(this));
        }

        if (button.id == 2)
        {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        }

        if (button.id == 14 && this.realmsButton.visible)
        {
            this.switchToRealms();
        }

        if (button.id == 4)
        {
            this.mc.shutdown();
        }

        if (button.id == 11)
        {
            this.mc.launchIntegratedServer("Demo_World", "Demo_World", DemoWorldServer.demoWorldSettings);
        }

        if (button.id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

            if (worldinfo != null)
            {
                GuiYesNo guiyesno = GuiSelectWorld.makeDeleteWorldYesNo(this, worldinfo.getWorldName(), 12);
                this.mc.displayGuiScreen(guiyesno);
            }
        }
    }

    private void switchToRealms()
    {
        RealmsBridge realmsbridge = new RealmsBridge();
        realmsbridge.switchToRealms(this);
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            isaveformat.flushCache();
            isaveformat.deleteWorldDirectory("Demo_World");
            this.mc.displayGuiScreen(this);
        }
        else if (id == 13)
        {
            if (result)
            {
                try
                {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                    oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {new URI(this.openGLWarningLink)});
                }
                catch (Throwable throwable)
                {
                    logger.error("无法打开帮助链接。", throwable);
                }
            }

            this.mc.displayGuiScreen(this);
        }
    }

    /**
     * Draws the main menu panorama
     */
    private void drawPanorama(int p_73970_1_, int p_73970_2_, float p_73970_3_)
    {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int i = 8;

        for (int j = 0; j < i * i; ++j)
        {
            GlStateManager.pushMatrix();
            float f = ((float)(j % i) / (float)i - 0.5F) / 64.0F;
            float f1 = ((float)(j / i) / (float)i - 0.5F) / 64.0F;
            float f2 = 0.0F;
            GlStateManager.translate(f, f1, f2);
            GlStateManager.rotate(MathHelper.sin(((float)this.panoramaTimer + p_73970_3_) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-((float)this.panoramaTimer + p_73970_3_) * 0.1F, 0.0F, 1.0F, 0.0F);

            for (int k = 0; k < 6; ++k)
            {
                GlStateManager.pushMatrix();

                if (k == 1)
                {
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 2)
                {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 3)
                {
                    GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 4)
                {
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (k == 5)
                {
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                this.mc.getTextureManager().bindTexture(titlePanoramaPaths[k]);
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int l = 255 / (j + 1);
                float f3 = 0.0F;
                worldrenderer.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, l).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }

        worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    /**
     * Rotate and blurs the skybox view in the main menu
     */
    private void rotateAndBlurSkybox(float p_73968_1_)
    {
        this.mc.getTextureManager().bindTexture(this.backgroundTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        int i = 3;

        for (int j = 0; j < i; ++j)
        {
            float f = 1.0F / (float)(j + 1);
            int k = this.width;
            int l = this.height;
            float f1 = (float)(j - i / 2) / 256.0F;
            worldrenderer.pos((double)k, (double)l, (double)this.zLevel).tex((double)(0.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos((double)k, 0.0D, (double)this.zLevel).tex((double)(1.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, 0.0D, (double)this.zLevel).tex((double)(1.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, (double)l, (double)this.zLevel).tex((double)(0.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    /**
     * Renders the skybox in the main menu
     */
    private void renderSkybox(int p_73971_1_, int p_73971_2_, float p_73971_3_)
    {
        // 始终在游戏主 FBO 内绘制主菜单背景。
        // 如果中途切回默认帧缓冲，在窗口最大化/还原时可能出现黑屏闪烁。
        GlStateManager.viewport(0, 0, 256, 256);
        this.drawPanorama(p_73971_1_, p_73971_2_, p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        int viewportWidth;
        int viewportHeight;

        if (OpenGlHelper.isFramebufferEnabled())
        {
            viewportWidth = Math.max(1, this.mc.getFramebuffer().framebufferWidth);
            viewportHeight = Math.max(1, this.mc.getFramebuffer().framebufferHeight);
        }
        else
        {
            viewportWidth = Math.max(1, this.mc.displayWidth);
            viewportHeight = Math.max(1, this.mc.displayHeight);
        }

        GlStateManager.viewport(0, 0, viewportWidth, viewportHeight);
        float f = this.width > this.height ? 120.0F / (float)this.width : 120.0F / (float)this.height;
        float f1 = (float)this.height * f / 256.0F;
        float f2 = (float)this.width * f / 256.0F;
        int i = this.width;
        int j = this.height;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(0.0D, (double)j, (double)this.zLevel).tex((double)(0.5F - f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, (double)j, (double)this.zLevel).tex((double)(0.5F - f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, 0.0D, (double)this.zLevel).tex((double)(0.5F + f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, (double)this.zLevel).tex((double)(0.5F + f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.updateFlappyGame();
        GlStateManager.disableAlpha();
        boolean shaderBackgroundRendered = this.renderShaderBackground();

        if (!shaderBackgroundRendered)
        {
            this.renderSkybox(mouseX, mouseY, partialTicks);
        }

        GlStateManager.enableAlpha();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 274;
        int j = this.width / 2 - i / 2;
        int k = 30;
        this.drawGradientRect(0, 0, this.width, this.height, -2130706433, 16777215);
        this.drawGradientRect(0, 0, this.width, this.height, 0, Integer.MIN_VALUE);
        this.mc.getTextureManager().bindTexture(minecraftTitleTextures);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if ((double)this.updateCounter < 1.0E-4D)
        {
            this.drawTexturedModalRect(j + 0, k + 0, 0, 0, 99, 44);
            this.drawTexturedModalRect(j + 99, k + 0, 129, 0, 27, 44);
            this.drawTexturedModalRect(j + 99 + 26, k + 0, 126, 0, 3, 44);
            this.drawTexturedModalRect(j + 99 + 26 + 3, k + 0, 99, 0, 26, 44);
            this.drawTexturedModalRect(j + 155, k + 0, 0, 45, 155, 44);
        }
        else
        {
            this.drawTexturedModalRect(j + 0, k + 0, 0, 0, 155, 44);
            this.drawTexturedModalRect(j + 155, k + 0, 0, 45, 155, 44);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)(this.width / 2 + 90), 70.0F, 0.0F);
        GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
        float f = 1.8F - MathHelper.abs(MathHelper.sin((float)(Minecraft.getSystemTime() % 1000L) / 1000.0F * (float)Math.PI * 2.0F) * 0.1F);
        f = f * 100.0F / (float)(this.fontRendererObj.getStringWidth(this.splashText) + 32);
        GlStateManager.scale(f, f, f);
        boolean shaderActive = UiExtensionManager.isSplashShaderEnabled() && this.getSplashShader().begin(-256);

        try
        {
            this.drawCenteredString(this.fontRendererObj, this.splashText, 0, -8, -256);
        }
        finally
        {
            if (shaderActive)
            {
                this.splashShader.end();
            }
        }

        GlStateManager.popMatrix();
        String s = "Minecraft 1.8.9";

        if (this.mc.isDemo())
        {
            s = s + " Demo";
        }

        this.drawString(this.fontRendererObj, s, 2, this.height - 10, -1);
        String s1 = "Copyright Mojang AB. Do not distribute!";
        this.drawString(this.fontRendererObj, s1, this.width - this.fontRendererObj.getStringWidth(s1) - 2, this.height - 10, -1);
        this.drawFlappyHint();

        if (this.openGLWarning1 != null && this.openGLWarning1.length() > 0)
        {
            drawRect(this.field_92022_t - 2, this.field_92021_u - 2, this.field_92020_v + 2, this.field_92019_w - 1, 1428160512);
            this.drawString(this.fontRendererObj, this.openGLWarning1, this.field_92022_t, this.field_92021_u, -1);
            this.drawString(this.fontRendererObj, this.openGLWarning2, (this.width - this.field_92024_r) / 2, ((GuiButton)this.buttonList.get(0)).yPosition - 12, -1);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.dwgxSession != null)
        {
            this.dwgxSession.render(this, mouseX, mouseY, partialTicks);
        }

        if (this.func_183501_a())
        {
            this.field_183503_M.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private boolean renderShaderBackground()
    {
        if (!UiExtensionManager.isMainMenuBackgroundShaderEnabled())
        {
            return false;
        }

        MainMenuSplashShader shader = this.getSplashShader();
        shader.setFlappyState(this.flappyTick, this.flappyBirdY, this.flappyWingFrame, this.flappyAlive, true);
        boolean shaderActive = shader.begin(16777215);

        if (!shaderActive)
        {
            return false;
        }

        try
        {
            this.mc.getTextureManager().bindTexture(Gui.optionsBackground);
            GlStateManager.disableCull();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldrenderer.pos(0.0D, (double)this.height, (double)this.zLevel).tex(0.0D, 1.0D).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((double)this.width, (double)this.height, (double)this.zLevel).tex(1.0D, 1.0D).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((double)this.width, 0.0D, (double)this.zLevel).tex(1.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(0.0D, 0.0D, (double)this.zLevel).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            return true;
        }
        catch (Throwable throwable)
        {
            logger.warn("Main menu shader background render failed. Falling back to skybox.", throwable);
            return false;
        }
        finally
        {
            shader.end();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void updateFlappyGame()
    {
        if (!UiExtensionManager.isMainMenuBackgroundShaderEnabled())
        {
            this.flappyLastUpdateMs = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();

        if (this.flappyLastUpdateMs == 0L)
        {
            this.flappyLastUpdateMs = now;
            return;
        }

        float dt = (float)(now - this.flappyLastUpdateMs) / 1000.0F;
        this.flappyLastUpdateMs = now;

        if (dt <= 0.0F)
        {
            return;
        }

        if (dt > 0.05F)
        {
            dt = 0.05F;
        }

        this.flappyWingFrame += dt * 12.0F;

        if (this.flappyJumpQueued)
        {
            this.flappyJumpQueued = false;

            if (!this.flappyAlive)
            {
                this.resetFlappyGame();
                return;
            }

            this.flappyVelocity = FLAPPY_FLAP_VELOCITY;
        }

        if (!this.flappyAlive)
        {
            return;
        }

        this.flappyTick += dt * 60.0F;
        this.flappyVelocity += FLAPPY_GRAVITY * dt;
        this.flappyBirdY += this.flappyVelocity * dt;

        float levelHeight = this.getFlappyLevelHeight();

        if (this.flappyBirdY <= FLAPPY_PIPE_BOTTOM)
        {
            this.flappyBirdY = FLAPPY_PIPE_BOTTOM;
            this.markFlappyFailed();
            return;
        }

        if (this.flappyBirdY + FLAPPY_BIRD_HEIGHT >= levelHeight - 1.0F)
        {
            this.flappyBirdY = levelHeight - FLAPPY_BIRD_HEIGHT - 1.0F;
            this.markFlappyFailed();
            return;
        }

        if (this.isFlappyPipeCollision())
        {
            this.markFlappyFailed();
            return;
        }

        this.flappyScore = Math.max(0, (int)Math.floor((this.flappyTick + FLAPPY_BIRD_X) / FLAPPY_HORZ_PIPE_DISTANCE));
        this.flappyBestScore = Math.max(this.flappyBestScore, this.flappyScore);
    }

    private boolean isFlappyPipeCollision()
    {
        float cycleLength = FLAPPY_HORZ_PIPE_DISTANCE * FLAPPY_PIPE_PER_CYCLE;
        float offset = this.flappyTick % cycleLength;
        float xPos = -offset;
        float birdLeft = FLAPPY_BIRD_X;
        float birdRight = FLAPPY_BIRD_X + FLAPPY_BIRD_WIDTH;
        float birdBottom = this.flappyBirdY;
        float birdTop = this.flappyBirdY + FLAPPY_BIRD_HEIGHT;

        for (int i = 0; i < 12; ++i)
        {
            float pipeLeft = xPos;
            float pipeRight = xPos + FLAPPY_PIPE_WIDTH;

            if (birdRight >= pipeLeft && birdLeft <= pipeRight)
            {
                float bottomHeight = this.getFlappyBottomPipeHeight(i);
                float gapBottom = FLAPPY_PIPE_BOTTOM + bottomHeight;
                float gapTop = gapBottom + FLAPPY_VERT_PIPE_DISTANCE;

                if (birdBottom < gapBottom || birdTop > gapTop)
                {
                    return true;
                }
            }

            xPos += FLAPPY_HORZ_PIPE_DISTANCE;
        }

        return false;
    }

    private float getFlappyBottomPipeHeight(int index)
    {
        float center = (FLAPPY_PIPE_MAX + FLAPPY_PIPE_MIN) / 2.0F;
        float halfTop = (center + FLAPPY_PIPE_MAX) / 2.0F;
        float halfBottom = (center + FLAPPY_PIPE_MIN) / 2.0F;
        int cycle = index % 8;

        if (cycle == 1 || cycle == 3)
        {
            return halfTop;
        }

        if (cycle == 2)
        {
            return FLAPPY_PIPE_MAX;
        }

        if (cycle == 5 || cycle == 7)
        {
            return halfBottom;
        }

        if (cycle == 6)
        {
            return FLAPPY_PIPE_MIN;
        }

        return center;
    }

    private float getFlappyLevelHeight()
    {
        float y = (float)Math.max(1, Display.getHeight()) / 2.0F;

        if (y >= 320.0F)
        {
            y /= 2.0F;
        }

        if (y < 100.0F)
        {
            y *= 2.0F;
        }

        return y;
    }

    private void markFlappyFailed()
    {
        if (!this.flappyAlive)
        {
            return;
        }

        this.flappyAlive = false;
        this.flappyBestScore = Math.max(this.flappyBestScore, this.flappyScore);
    }

    private void resetFlappyGame()
    {
        this.flappyTick = 0.0F;
        this.flappyBirdY = FLAPPY_START_Y;
        this.flappyVelocity = 0.0F;
        this.flappyWingFrame = 0.0F;
        this.flappyAlive = true;
        this.flappyScore = 0;
        this.flappyJumpQueued = false;
        this.flappyLastUpdateMs = System.currentTimeMillis();
    }

    private void queueFlappyJump()
    {
        if (!UiExtensionManager.isMainMenuBackgroundShaderEnabled())
        {
            return;
        }

        this.flappyJumpQueued = true;
    }

    private void drawFlappyHint()
    {
        if (!UiExtensionManager.isMainMenuBackgroundShaderEnabled())
        {
            return;
        }

        String status = this.flappyAlive
            ? "空格跳跃  分数: " + this.flappyScore + "  最高: " + this.flappyBestScore
            : "失败了，按空格重新开始  本次: " + this.flappyScore + "  最高: " + this.flappyBestScore;
        this.drawString(this.fontRendererObj, status, 6, 6, 16777215);
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.dwgxSession != null)
        {
            this.dwgxSession.mouseClicked(mouseX, mouseY, mouseButton);
        }

        synchronized (this.threadLock)
        {
            if (this.openGLWarning1.length() > 0 && mouseX >= this.field_92022_t && mouseX <= this.field_92020_v && mouseY >= this.field_92021_u && mouseY <= this.field_92019_w)
            {
                GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                guiconfirmopenlink.disableSecurityWarning();
                this.mc.displayGuiScreen(guiconfirmopenlink);
            }
        }

        if (this.func_183501_a())
        {
            this.field_183503_M.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);

        if (this.dwgxSession != null)
        {
            this.dwgxSession.mouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        if (this.splashShader != null)
        {
            this.splashShader.close();
            this.splashShader = null;
        }

        if (this.field_183503_M != null)
        {
            this.field_183503_M.onGuiClosed();
        }
    }

    private MainMenuSplashShader getSplashShader()
    {
        if (this.splashShader == null)
        {
            this.splashShader = new MainMenuSplashShader();
        }

        return this.splashShader;
    }
}


