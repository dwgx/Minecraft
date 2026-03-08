package client.ui.irc.component;

import dwgx.nano.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click context menu for messages and users.
 * Shows a list of actions (copy, reply, delete, etc.).
 */
public final class IRCContextMenu
{
    private static final float ITEM_W = 140.0F;
    private static final float ITEM_H = 28.0F;
    private static final float PAD = 4.0F;

    private boolean visible;
    private float menuX;
    private float menuY;
    private int hoveredIndex = -1;
    private String targetMessageId;
    private String targetUserId;
    private final List<MenuItem> items = new ArrayList<MenuItem>();

    public boolean isVisible() { return this.visible; }
    public String getTargetMessageId() { return this.targetMessageId; }
    public String getTargetUserId() { return this.targetUserId; }

    public void showForMessage(String messageId, float x, float y, float screenW, float screenH,
                               boolean isOwnMessage)
    {
        this.targetMessageId = messageId;
        this.targetUserId = null;
        this.items.clear();
        this.items.add(new MenuItem("copy", "Copy Text"));
        this.items.add(new MenuItem("reply", "Reply"));
        if (isOwnMessage)
        {
            this.items.add(new MenuItem("delete", "Delete"));
        }
        positionAndShow(x, y, screenW, screenH);
    }

    public void showForUser(String userId, float x, float y, float screenW, float screenH)
    {
        this.targetUserId = userId;
        this.targetMessageId = null;
        this.items.clear();
        this.items.add(new MenuItem("profile", "View Profile"));
        this.items.add(new MenuItem("dm", "Send Message"));
        this.items.add(new MenuItem("add_friend", "Add Friend"));
        positionAndShow(x, y, screenW, screenH);
    }

    public void hide()
    {
        this.visible = false;
        this.items.clear();
    }

    private void positionAndShow(float x, float y, float screenW, float screenH)
    {
        float totalH = this.items.size() * ITEM_H + PAD * 2.0F;
        this.menuX = Math.min(x, screenW - ITEM_W - 8.0F);
        this.menuY = Math.min(y, screenH - totalH - 8.0F);
        if (this.menuX < 8.0F) this.menuX = 8.0F;
        if (this.menuY < 8.0F) this.menuY = 8.0F;
        this.visible = true;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme, int mx, int my)
    {
        if (!this.visible || this.items.isEmpty()) return;

        float totalH = this.items.size() * ITEM_H + PAD * 2.0F;

        // Shadow + background
        NanoUi.drawWindow(vg, stack, this.menuX, this.menuY, ITEM_W, totalH, theme);

        int font = NanoFontBook.uiRegular();
        this.hoveredIndex = -1;

        for (int i = 0; i < this.items.size(); i++)
        {
            float ix = this.menuX + PAD;
            float iy = this.menuY + PAD + i * ITEM_H;
            float iw = ITEM_W - PAD * 2.0F;

            boolean hovered = mx >= ix && mx <= ix + iw && my >= iy && my <= iy + ITEM_H;
            if (hovered)
            {
                this.hoveredIndex = i;
                NanoRenderUtils.fillRoundedRect(vg, ix, iy, iw, ITEM_H, 4.0F,
                        NanoRenderUtils.argb(stack, theme.controlHoverArgb()));
            }

            NanoRenderUtils.drawLabel(vg, stack, font, ix + 8.0F, iy + ITEM_H * 0.5F,
                    12.0F, this.items.get(i).label, theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        }
    }

    /**
     * Handle click. Returns the action id of the selected item, or null.
     * Returns "" if clicked outside (dismiss).
     */
    public String handleClick(int mx, int my)
    {
        if (!this.visible) return null;

        float totalH = this.items.size() * ITEM_H + PAD * 2.0F;
        if (mx < this.menuX || mx > this.menuX + ITEM_W
                || my < this.menuY || my > this.menuY + totalH)
        {
            this.visible = false;
            return "";
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.items.size())
        {
            String action = this.items.get(this.hoveredIndex).id;
            this.visible = false;
            return action;
        }
        return null;
    }

    public boolean containsMouse(int mx, int my)
    {
        if (!this.visible) return false;
        float totalH = this.items.size() * ITEM_H + PAD * 2.0F;
        return mx >= this.menuX && mx <= this.menuX + ITEM_W
                && my >= this.menuY && my <= this.menuY + totalH;
    }

    private static final class MenuItem
    {
        final String id;
        final String label;
        MenuItem(String id, String label) { this.id = id; this.label = label; }
    }
}
