package dwgx.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.Display;
import org.lwjgl.system.MemoryStack;

import java.io.File;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;
import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgBoxGradient;
import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgFillPaint;
import static org.lwjgl.nanovg.NanoVG.nvgFontFaceId;
import static org.lwjgl.nanovg.NanoVG.nvgFontSize;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;
import static org.lwjgl.nanovg.NanoVG.nvgRoundedRect;
import static org.lwjgl.nanovg.NanoVG.nvgText;
import static org.lwjgl.nanovg.NanoVG.nvgTextAlign;
import static org.lwjgl.nanovg.NanoVGGL2.nvgCreate;
import static org.lwjgl.nanovg.NanoVGGL2.nvgDelete;
import static org.lwjgl.nanovg.NanoVGGL2.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL2.NVG_STENCIL_STROKES;

/**
 * Optional NanoVG-based overlay UI for modern animated widgets.
 * Enable with --dwgxUi or -Ddwgx.ui.enabled=true.
 */
public final class DwgxNanoUiOverlay
{
    private static final int TOGGLE_KEY = 67; // F9 in LWJGL2 key codes

    private final Minecraft mc;
    private long vg;
    private int fontId = -1;
    private boolean visible = true;
    private boolean lastToggleKeyDown;
    private boolean lastLeftDown;
    private float openAnim = 1.0F;
    private float hoverAnim;
    private long lastFrameNanos = System.nanoTime();

    private DwgxNanoUiOverlay(Minecraft mc)
    {
        this.mc = mc;
    }

    public static DwgxNanoUiOverlay create(Minecraft mc)
    {
        if (!Boolean.getBoolean("dwgx.ui.enabled"))
        {
            return null;
        }

        DwgxNanoUiOverlay overlay = new DwgxNanoUiOverlay(mc);
        return overlay.init() ? overlay : null;
    }

    private boolean init()
    {
        try
        {
            this.vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);

            if (this.vg == 0L)
            {
                System.err.println("[DWGX-UI] NanoVG context create failed");
                return false;
            }

            this.fontId = loadDefaultFont(this.vg);
            return true;
        }
        catch (Throwable t)
        {
            System.err.println("[DWGX-UI] NanoVG init failed: " + t.getClass().getSimpleName());
            this.destroy();
            return false;
        }
    }

    public void render(float partialTicks)
    {
        if (this.vg == 0L)
        {
            return;
        }

        boolean keyDown = Keyboard.isKeyDown(TOGGLE_KEY);

        if (keyDown && !this.lastToggleKeyDown)
        {
            this.visible = !this.visible;
        }

        this.lastToggleKeyDown = keyDown;

        long now = System.nanoTime();
        float dt = Math.min(0.05F, (float)(now - this.lastFrameNanos) / 1000000000.0F);
        this.lastFrameNanos = now;

        float target = this.visible ? 1.0F : 0.0F;
        this.openAnim += (target - this.openAnim) * Math.min(1.0F, dt * 12.0F);

        if (this.openAnim < 0.01F)
        {
            return;
        }

        int fbWidth = Math.max(1, Display.getWidth());
        int fbHeight = Math.max(1, Display.getHeight());
        int winWidth = Math.max(1, Display.getWindowWidth());
        float pixelRatio = (float)fbWidth / (float)winWidth;

        float mouseX = Mouse.getX();
        float mouseY = fbHeight - 1 - Mouse.getY();
        boolean leftDown = Mouse.isButtonDown(0);
        boolean clicked = leftDown && !this.lastLeftDown;
        this.lastLeftDown = leftDown;

        float panelW = 330.0F;
        float panelH = 170.0F;
        float margin = 18.0F;
        float panelX = fbWidth - panelW - margin;
        float panelY = margin - (1.0F - this.openAnim) * (panelH + 24.0F);

        float buttonX = panelX + 16.0F;
        float buttonY = panelY + panelH - 54.0F;
        float buttonW = panelW - 32.0F;
        float buttonH = 34.0F;

        boolean buttonHover = contains(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH);
        float hoverTarget = buttonHover ? 1.0F : 0.0F;
        this.hoverAnim += (hoverTarget - this.hoverAnim) * Math.min(1.0F, dt * 14.0F);

        if (clicked && buttonHover)
        {
            toggleAggressiveProfile(this.mc.gameSettings);
        }

        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            nvgBeginFrame(this.vg, fbWidth, fbHeight, pixelRatio);

            NVGColor shadowInner = rgba(stack, 0, 0, 0, (int)(90 * this.openAnim));
            NVGColor shadowOuter = rgba(stack, 0, 0, 0, 0);
            NVGPaint shadowPaint = NVGPaint.mallocStack(stack);
            nvgBoxGradient(this.vg, panelX - 4.0F, panelY - 4.0F, panelW + 8.0F, panelH + 8.0F, 12.0F, 14.0F, shadowInner, shadowOuter, shadowPaint);
            nvgBeginPath(this.vg);
            nvgRoundedRect(this.vg, panelX - 10.0F, panelY - 10.0F, panelW + 20.0F, panelH + 20.0F, 14.0F);
            nvgFillPaint(this.vg, shadowPaint);
            nvgFill(this.vg);

            nvgBeginPath(this.vg);
            nvgRoundedRect(this.vg, panelX, panelY, panelW, panelH, 12.0F);
            nvgFillColor(this.vg, rgba(stack, 20, 24, 34, (int)(220 * this.openAnim)));
            nvgFill(this.vg);

            nvgBeginPath(this.vg);
            nvgRoundedRect(this.vg, panelX, panelY, panelW, 8.0F, 12.0F);
            nvgFillColor(this.vg, rgba(stack, 70, 170, 255, (int)(220 * this.openAnim)));
            nvgFill(this.vg);

            drawLabel(stack, panelX + 16.0F, panelY + 24.0F, 18.0F, "DWGX Render Layer", 225, 236, 255, 235, NVG_ALIGN_LEFT);
            drawLabel(stack, panelX + 16.0F, panelY + 48.0F, 14.0F, "NanoVG + LWJGL3 backend (F9 toggle)", 150, 172, 205, 220, NVG_ALIGN_LEFT);

            drawLabel(stack, panelX + 16.0F, panelY + 76.0F, 14.0F, "FPS " + Math.max(0, Minecraft.getDebugFPS()), 186, 255, 211, 220, NVG_ALIGN_LEFT);
            drawLabel(stack, panelX + panelW - 16.0F, panelY + 76.0F, 14.0F, "Workers " + this.mc.renderGlobal.getDebugInfoRenders().trim(), 170, 190, 222, 190, NVG_ALIGN_RIGHT);

            int buttonBlue = (int)(95 + 80 * this.hoverAnim);
            int buttonAlpha = (int)(165 + 50 * this.hoverAnim);
            nvgBeginPath(this.vg);
            nvgRoundedRect(this.vg, buttonX, buttonY, buttonW, buttonH, 9.0F);
            nvgFillColor(this.vg, rgba(stack, 35, buttonBlue, 220, buttonAlpha));
            nvgFill(this.vg);

            drawLabel(stack, buttonX + 14.0F, buttonY + buttonH / 2.0F, 15.0F, profileLabel(this.mc.gameSettings), 255, 255, 255, 240, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

            nvgEndFrame(this.vg);
        }
        catch (Throwable t)
        {
            System.err.println("[DWGX-UI] Render error: " + t.getClass().getSimpleName());
            this.destroy();
        }
        finally
        {
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
        }
    }

    public void destroy()
    {
        if (this.vg != 0L)
        {
            nvgDelete(this.vg);
            this.vg = 0L;
        }
    }

    private void drawLabel(MemoryStack stack, float x, float y, float size, String text, int r, int g, int b, int a, int align)
    {
        if (this.fontId < 0)
        {
            return;
        }

        nvgFontFaceId(this.vg, this.fontId);
        nvgFontSize(this.vg, size);
        nvgTextAlign(this.vg, align);
        nvgFillColor(this.vg, rgba(stack, r, g, b, a));
        nvgText(this.vg, x, y, text);
    }

    private static NVGColor rgba(MemoryStack stack, int r, int g, int b, int a)
    {
        NVGColor color = NVGColor.mallocStack(stack);
        nvgRGBA((byte)r, (byte)g, (byte)b, (byte)a, color);
        return color;
    }

    private static boolean contains(float x, float y, float rx, float ry, float rw, float rh)
    {
        return x >= rx && y >= ry && x <= rx + rw && y <= ry + rh;
    }

    private static String profileLabel(GameSettings settings)
    {
        boolean fast = settings != null && !settings.fancyGraphics && settings.particleSetting >= 1 && !settings.enableVsync;
        return fast ? "Switch To Balanced Profile" : "Switch To Aggressive Profile";
    }

    private static void toggleAggressiveProfile(GameSettings settings)
    {
        if (settings == null)
        {
            return;
        }

        boolean toAggressive = settings.fancyGraphics || settings.enableVsync || settings.particleSetting == 0;

        if (toAggressive)
        {
            settings.enableVsync = false;
            settings.fboEnable = false;
            settings.fancyGraphics = false;
            settings.ambientOcclusion = 0;
            settings.clouds = 0;
            settings.entityShadows = false;
            settings.viewBobbing = false;
            settings.particleSetting = 2;
            settings.useVbo = true;
            settings.renderDistanceChunks = Math.max(4, Math.min(settings.renderDistanceChunks, 10));
            settings.limitFramerate = Math.max(120, settings.limitFramerate);
        }
        else
        {
            settings.enableVsync = true;
            settings.fboEnable = true;
            settings.fancyGraphics = true;
            settings.ambientOcclusion = 2;
            settings.clouds = 2;
            settings.entityShadows = true;
            settings.viewBobbing = true;
            settings.particleSetting = 0;
            settings.renderDistanceChunks = Math.min(16, Math.max(settings.renderDistanceChunks, 10));
        }

        settings.saveOptions();
    }

    private static int loadDefaultFont(long vg)
    {
        String custom = System.getProperty("dwgx.ui.font");

        if (isExistingFile(custom))
        {
            int id = nvgCreateFont(vg, "dwgx-ui", custom);

            if (id >= 0)
            {
                return id;
            }
        }

        String winDir = System.getenv("WINDIR");

        if (winDir != null)
        {
            String[] candidates = new String[] {
                winDir + "\\Fonts\\segoeui.ttf",
                winDir + "\\Fonts\\arial.ttf",
                winDir + "\\Fonts\\msyh.ttc"
            };

            for (String candidate : candidates)
            {
                if (!isExistingFile(candidate))
                {
                    continue;
                }

                int id = nvgCreateFont(vg, "dwgx-ui", candidate);

                if (id >= 0)
                {
                    return id;
                }
            }
        }

        System.err.println("[DWGX-UI] No font found, text rendering disabled");
        return -1;
    }

    private static boolean isExistingFile(String path)
    {
        return path != null && !path.isEmpty() && (new File(path)).isFile();
    }
}
