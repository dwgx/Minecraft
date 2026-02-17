package dwgx.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Lightweight, draggable main-menu session panel that shows system time.
 */
public final class MainMenuSession extends Gui
{
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 52;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Minecraft mc;
    private int x;
    private int y;
    private boolean dragging;
    private int dragDx;
    private int dragDy;

    public MainMenuSession(Minecraft mc)
    {
        this.mc = mc;
    }

    public void setScreenSize(int screenWidth, int screenHeight)
    {
        // Default to top-right with margin
        this.x = screenWidth - PANEL_WIDTH - 12;
        this.y = 12;
    }

    public void render(GuiScreen screen, int mouseX, int mouseY, float partialTicks)
    {
        if (this.dragging)
        {
            this.x = clamp(mouseX - this.dragDx, 6, screen.width - PANEL_WIDTH - 6);
            this.y = clamp(mouseY - this.dragDy, 6, screen.height - PANEL_HEIGHT - 6);
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        int bgTop = 0xCC1D2634;   // dark navy with alpha
        int bgBottom = 0xCC131820;
        drawGradientRect(this.x, this.y, this.x + PANEL_WIDTH, this.y + PANEL_HEIGHT, bgTop, bgBottom);

        drawRect(this.x, this.y, this.x + PANEL_WIDTH, this.y + 6, 0xFF2D7FFF);
        drawRect(this.x, this.y, this.x + PANEL_WIDTH, this.y + 1, 0x802F3A4A);
        drawRect(this.x, this.y + PANEL_HEIGHT - 1, this.x + PANEL_WIDTH, this.y + PANEL_HEIGHT, 0x802F3A4A);
        drawRect(this.x, this.y, this.x + 1, this.y + PANEL_HEIGHT, 0x802F3A4A);
        drawRect(this.x + PANEL_WIDTH - 1, this.y, this.x + PANEL_WIDTH, this.y + PANEL_HEIGHT, 0x802F3A4A);

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        String title = "Session";
        String timeStr = TIME_FMT.format(new Date());
        this.mc.fontRendererObj.drawString(title, this.x + 8, this.y + 10, 0xE1FFFFFF);
        this.mc.fontRendererObj.drawString(timeStr, this.x + 8, this.y + 26, 0xCCF2F6FF);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton == 0 && hit(mouseX, mouseY))
        {
            this.dragging = true;
            this.dragDx = mouseX - this.x;
            this.dragDy = mouseY - this.y;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton == 0)
        {
            this.dragging = false;
        }
    }

    private boolean hit(int mx, int my)
    {
        return mx >= this.x && mx <= this.x + PANEL_WIDTH && my >= this.y && my <= this.y + PANEL_HEIGHT;
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
