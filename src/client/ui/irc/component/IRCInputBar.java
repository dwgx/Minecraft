package client.ui.irc.component;

import client.ui.irc.input.IRCMultilineInput;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Input bar: multiline text field + emoji button + image button + send button.
 */
public final class IRCInputBar
{
    private static final float BTN_W = 50.0F;
    private static final float BTN_H = 28.0F;
    private static final float ICON_SIZE = 28.0F;
    private static final float GAP = 8.0F;

    private boolean sendHovered;
    private boolean emojiHovered;
    private boolean imageHovered;

    public boolean isSendHovered() { return this.sendHovered; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       IRCMultilineInput textInput, int mx, int my)
    {
        if (r == null) return;

        // Emoji button (left side)
        float emojiX = r.x + GAP;
        float emojiY = r.y + (r.h - ICON_SIZE) * 0.5F;
        this.emojiHovered = mx >= emojiX && mx <= emojiX + ICON_SIZE
                && my >= emojiY && my <= emojiY + ICON_SIZE;
        int emojiBg = this.emojiHovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, emojiX, emojiY, ICON_SIZE, ICON_SIZE,
                ICON_SIZE * 0.5F, NanoRenderUtils.argb(stack, emojiBg));
        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                emojiX + ICON_SIZE * 0.5F, emojiY + ICON_SIZE * 0.5F, 14.0F,
                ":)", theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Image button (next to emoji)
        float imgX = emojiX + ICON_SIZE + GAP * 0.5F;
        float imgY = emojiY;
        this.imageHovered = mx >= imgX && mx <= imgX + ICON_SIZE
                && my >= imgY && my <= imgY + ICON_SIZE;
        int imgBg = this.imageHovered ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, imgX, imgY, ICON_SIZE, ICON_SIZE,
                ICON_SIZE * 0.5F, NanoRenderUtils.argb(stack, imgBg));
        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                imgX + ICON_SIZE * 0.5F, imgY + ICON_SIZE * 0.5F, 11.0F,
                "IMG", this.imageHovered ? theme.accentArgb() : theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Text input (between buttons and send)
        float inputX = imgX + ICON_SIZE + GAP;
        float inputW = r.w - ICON_SIZE * 2 - BTN_W - GAP * 4.5F;
        float inputH = r.h - 12.0F;
        float inputY = r.y + (r.h - inputH) * 0.5F;

        textInput.render(vg, stack, theme, inputX, inputY, inputW, inputH);

        // Send button
        float btnX = r.x + r.w - BTN_W - GAP;
        float btnY = r.y + (r.h - BTN_H) * 0.5F;
        this.sendHovered = mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H;
        int sendBg = this.sendHovered ? theme.accentArgb() : theme.accentSoftArgb();
        NanoRenderUtils.fillRoundedRect(vg, btnX, btnY, BTN_W, BTN_H, theme.controlRadius(),
                NanoRenderUtils.argb(stack, sendBg));
        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiBold(),
                btnX + BTN_W * 0.5F, btnY + BTN_H * 0.5F, 12.0F,
                "Send", theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    public boolean hitSend(UiRect r, int mx, int my)
    {
        if (r == null) return false;
        float btnX = r.x + r.w - BTN_W - GAP;
        float btnY = r.y + (r.h - BTN_H) * 0.5F;
        return mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H;
    }

    public boolean hitEmoji(UiRect r, int mx, int my)
    {
        if (r == null) return false;
        float emojiX = r.x + GAP;
        float emojiY = r.y + (r.h - ICON_SIZE) * 0.5F;
        return mx >= emojiX && mx <= emojiX + ICON_SIZE
                && my >= emojiY && my <= emojiY + ICON_SIZE;
    }

    public boolean hitImage(UiRect r, int mx, int my)
    {
        if (r == null) return false;
        float emojiX = r.x + GAP;
        float imgX = emojiX + ICON_SIZE + GAP * 0.5F;
        float imgY = r.y + (r.h - ICON_SIZE) * 0.5F;
        return mx >= imgX && mx <= imgX + ICON_SIZE
                && my >= imgY && my <= imgY + ICON_SIZE;
    }
}
