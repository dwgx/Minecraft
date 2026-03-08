package client.ui.irc.component;

import client.chat.model.ChatUser;
import client.chat.model.UserStatus;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Popup card shown when left-clicking a user avatar.
 * Shows avatar, nickname, status, and action buttons (Add Friend / Send DM).
 */
public final class IRCUserPopup
{
    private static final float WIDTH = 200.0F;
    private static final float HEIGHT = 160.0F;
    private static final float AVATAR_SIZE = 48.0F;
    private static final float BTN_H = 26.0F;
    private static final float BTN_GAP = 6.0F;
    private static final float PAD = 12.0F;

    private boolean visible;
    private ChatUser user;
    private float popupX;
    private float popupY;
    private boolean addFriendHovered;
    private boolean sendDmHovered;

    public boolean isVisible() { return this.visible; }
    public ChatUser getUser() { return this.user; }

    public void show(ChatUser user, float x, float y, float screenW, float screenH)
    {
        this.user = user;
        this.visible = true;
        // Clamp to screen bounds
        this.popupX = Math.min(x, screenW - WIDTH - 8.0F);
        this.popupY = Math.min(y, screenH - HEIGHT - 8.0F);
        if (this.popupX < 8.0F) this.popupX = 8.0F;
        if (this.popupY < 8.0F) this.popupY = 8.0F;
    }

    public void hide()
    {
        this.visible = false;
        this.user = null;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme, int mx, int my)
    {
        if (!this.visible || this.user == null) return;

        float x = this.popupX;
        float y = this.popupY;
        int fontBold = NanoFontBook.uiBold();
        int fontRegular = NanoFontBook.uiRegular();

        // Shadow + card background
        NanoUi.drawWindow(vg, stack, x, y, WIDTH, HEIGHT, theme);

        // Avatar
        float avatarX = x + (WIDTH - AVATAR_SIZE) * 0.5F;
        float avatarY = y + PAD;
        NanoRenderUtils.fillRoundedRect(vg, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                AVATAR_SIZE * 0.5F, NanoRenderUtils.argb(stack, theme.accentArgb()));
        NanoRenderUtils.drawLabel(vg, stack, fontBold,
                avatarX + AVATAR_SIZE * 0.5F, avatarY + AVATAR_SIZE * 0.5F,
                20.0F, this.user.getInitial(), 0xFFFFFFFF,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Status dot on avatar
        float dotSize = 12.0F;
        float dotX = avatarX + AVATAR_SIZE - dotSize * 0.5F;
        float dotY = avatarY + AVATAR_SIZE - dotSize * 0.5F;
        NanoRenderUtils.fillRoundedRect(vg, dotX, dotY, dotSize, dotSize, dotSize * 0.5F,
                NanoRenderUtils.argb(stack, IRCRenderUtils.statusColor(this.user.getStatus())));

        // Nickname
        float nameY = avatarY + AVATAR_SIZE + 8.0F;
        NanoRenderUtils.drawLabel(vg, stack, fontBold, x + WIDTH * 0.5F, nameY,
                14.0F, this.user.getNickname(), theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);

        // Status text
        String statusText = statusLabel(this.user.getStatus());
        NanoRenderUtils.drawLabel(vg, stack, fontRegular, x + WIDTH * 0.5F, nameY + 18.0F,
                11.0F, statusText, theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);

        // Buttons
        float btnW = (WIDTH - PAD * 2.0F - BTN_GAP) * 0.5F;
        float btnY = y + HEIGHT - PAD - BTN_H;
        float btn1X = x + PAD;
        float btn2X = btn1X + btnW + BTN_GAP;

        // Add Friend button
        this.addFriendHovered = mx >= btn1X && mx <= btn1X + btnW && my >= btnY && my <= btnY + BTN_H;
        int btn1Bg = this.addFriendHovered ? theme.accentArgb() : theme.accentSoftArgb();
        NanoRenderUtils.fillRoundedRect(vg, btn1X, btnY, btnW, BTN_H, theme.controlRadius(),
                NanoRenderUtils.argb(stack, btn1Bg));
        NanoRenderUtils.drawLabel(vg, stack, fontRegular, btn1X + btnW * 0.5F, btnY + BTN_H * 0.5F,
                11.0F, "Add Friend", theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Send DM button
        this.sendDmHovered = mx >= btn2X && mx <= btn2X + btnW && my >= btnY && my <= btnY + BTN_H;
        int btn2Bg = this.sendDmHovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, btn2X, btnY, btnW, BTN_H, theme.controlRadius(),
                NanoRenderUtils.argb(stack, btn2Bg));
        NanoRenderUtils.drawLabel(vg, stack, fontRegular, btn2X + btnW * 0.5F, btnY + BTN_H * 0.5F,
                11.0F, "Message", theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    /** Returns: 0=none, 1=add friend, 2=send DM, -1=click outside (dismiss) */
    public int handleClick(int mx, int my)
    {
        if (!this.visible) return 0;
        if (mx < this.popupX || mx > this.popupX + WIDTH
                || my < this.popupY || my > this.popupY + HEIGHT)
        {
            return -1; // outside
        }
        if (this.addFriendHovered) return 1;
        if (this.sendDmHovered) return 2;
        return 0; // inside but no button
    }

    public boolean containsMouse(int mx, int my)
    {
        return this.visible && mx >= this.popupX && mx <= this.popupX + WIDTH
                && my >= this.popupY && my <= this.popupY + HEIGHT;
    }

    private static String statusLabel(UserStatus status)
    {
        if (status == null) return "Offline";
        switch (status)
        {
            case ONLINE: return "Online";
            case AWAY:   return "Away";
            case DND:    return "Do Not Disturb";
            default:     return "Offline";
        }
    }
}

