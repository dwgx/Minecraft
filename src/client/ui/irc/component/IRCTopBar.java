package client.ui.irc.component;

import client.chat.model.ChatUser;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Top bar: user avatar + nickname + current conversation title + close button.
 */
public final class IRCTopBar
{
    private boolean closeHovered;

    public boolean isCloseHovered() { return this.closeHovered; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       ChatUser localUser, String conversationTitle, int mx, int my)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.sidebarArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontRegular = NanoFontBook.uiRegular();

        // User avatar circle
        float avatarSize = 24.0F;
        float avatarX = r.x + 10.0F;
        float avatarY = r.y + (r.h - avatarSize) * 0.5F;
        NanoRenderUtils.fillRoundedRect(vg, avatarX, avatarY, avatarSize, avatarSize,
                avatarSize * 0.5F, NanoRenderUtils.argb(stack, theme.accentArgb()));
        String initial = localUser != null ? localUser.getInitial() : "?";
        NanoRenderUtils.drawLabel(vg, stack, fontBold, avatarX + avatarSize * 0.5F,
                avatarY + avatarSize * 0.5F, 11.0F, initial, 0xFFFFFFFF,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Nickname
        String nick = localUser != null ? localUser.getNickname() : "Chat";
        NanoRenderUtils.drawLabel(vg, stack, fontBold, avatarX + avatarSize + 8.0F,
                r.y + r.h * 0.5F, 14.0F, nick, theme.textArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Conversation title (center)
        if (conversationTitle != null)
        {
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, r.x + r.w * 0.5F,
                    r.y + r.h * 0.5F, 13.0F, conversationTitle, theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }

        // Close button
        float btnSize = 20.0F;
        float btnX = r.x + r.w - btnSize - 8.0F;
        float btnY = r.y + (r.h - btnSize) * 0.5F;
        this.closeHovered = mx >= btnX && mx <= btnX + btnSize && my >= btnY && my <= btnY + btnSize;
        int closeColor = this.closeHovered ? theme.dangerArgb() : theme.textMutedArgb();
        NanoRenderUtils.drawLabel(vg, stack, fontRegular, btnX + btnSize * 0.5F,
                btnY + btnSize * 0.5F, 14.0F, "x", closeColor,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    public boolean hitClose(UiRect r, int mx, int my)
    {
        if (r == null) return false;
        float btnSize = 20.0F;
        float btnX = r.x + r.w - btnSize - 8.0F;
        float btnY = r.y + (r.h - btnSize) * 0.5F;
        return mx >= btnX && mx <= btnX + btnSize && my >= btnY && my <= btnY + btnSize;
    }
}
