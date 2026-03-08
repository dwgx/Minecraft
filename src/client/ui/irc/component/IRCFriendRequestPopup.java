package client.ui.irc.component;

import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import client.ui.template.NanoScreenKit;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Friend request confirmation popup.
 */
public final class IRCFriendRequestPopup
{
    private static final float WIDTH = 280.0F;
    private static final float HEIGHT = 120.0F;

    private boolean visible;
    private String fromNick;
    private int acceptBtnHover;
    private int rejectBtnHover;

    public void show(String fromNick)
    {
        this.fromNick = fromNick;
        this.visible = true;
    }

    public void show(String fromNick, float x, float y, float screenW, float screenH)
    {
        show(fromNick);
    }

    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public String getFromNick() { return this.fromNick; }
    public String getNick() { return this.fromNick; }

    /** @return 0=none, 1=accept, 2=reject */
    public int render(long vg, MemoryStack stack, NanoTheme theme, float screenW, float screenH, int mx, int my)
    {
        if (!this.visible || this.fromNick == null) return 0;

        float x = (screenW - WIDTH) * 0.5F;
        float y = (screenH - HEIGHT) * 0.5F;

        // Backdrop
        NanoRenderUtils.fillRect(vg, 0, 0, screenW, screenH, NanoRenderUtils.argb(stack, 0x80000000));

        // Panel
        NanoRenderUtils.fillRoundedRect(vg, x, y, WIDTH, HEIGHT, 8.0F,
                NanoRenderUtils.argb(stack, theme.panelArgb()));
        NanoRenderUtils.strokeRoundedRect(vg, x, y, WIDTH, HEIGHT, 8.0F, 1.0F,
                NanoRenderUtils.argb(stack, theme.borderArgb()));

        // Title
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiBold());
        NanoVG.nvgFontSize(vg, 14.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(vg, x + WIDTH * 0.5F, y + 12, "Friend Request");

        // Message
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiRegular());
        NanoVG.nvgFontSize(vg, 13.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
        NanoVG.nvgText(vg, x + WIDTH * 0.5F, y + 36, this.fromNick + " wants to be your friend");

        // Buttons
        float btnW = 100.0F;
        float btnH = 28.0F;
        float btnY = y + HEIGHT - btnH - 14;

        // Accept
        float ax = x + WIDTH * 0.5F - btnW - 8;
        boolean aHover = mx >= ax && mx <= ax + btnW && my >= btnY && my <= btnY + btnH;
        int aBg = aHover ? 0xFF3BA55D : 0xFF43B581;
        NanoRenderUtils.fillRoundedRect(vg, ax, btnY, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, aBg));
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgText(vg, ax + btnW * 0.5F, btnY + 6, "Accept");

        // Reject
        float rx = x + WIDTH * 0.5F + 8;
        boolean rHover = mx >= rx && mx <= rx + btnW && my >= btnY && my <= btnY + btnH;
        int rBg = rHover ? 0xFFD83C3E : 0xFFED4245;
        NanoRenderUtils.fillRoundedRect(vg, rx, btnY, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, rBg));
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgText(vg, rx + btnW * 0.5F, btnY + 6, "Reject");

        this.acceptBtnHover = aHover ? 1 : 0;
        this.rejectBtnHover = rHover ? 1 : 0;
        return 0;
    }

    /** Call on click. Returns 1=accept, 2=reject, -1=outside, 0=nothing. */
    public int handleClick(int mx, int my)
    {
        if (!this.visible) return 0;
        if (this.acceptBtnHover == 1) { hide(); return 1; }
        if (this.rejectBtnHover == 1) { hide(); return 2; }
        return -1;
    }
}
