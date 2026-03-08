package client.ui.irc.component;

import client.chat.model.ChatEmoji;
import client.ui.layout.UiRect;
import dwgx.nano.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Emoji/sticker picker popup. Shows a grid of emoji shortcodes.
 * Appears above the input bar when the emoji button is clicked.
 */
public final class IRCEmojiPicker
{
    private static final float WIDTH = 280.0F;
    private static final float HEIGHT = 220.0F;
    private static final float CELL = 32.0F;
    private static final float PAD = 8.0F;
    private static final float HEADER_H = 28.0F;

    /** Built-in text emoji set (no image assets needed). */
    private static final String[] BUILTIN_EMOJI = {
        ":)", ":(", ":D", ";)", ":P", ":O", "<3", "XD",
        ":3", ":/", "B)", ">:(", "T_T", "^_^", "o_O", ":*"
    };

    private boolean visible;
    private float popupX;
    private float popupY;
    private int hoveredIndex = -1;
    private final List<ChatEmoji> customEmojis = new ArrayList<ChatEmoji>();

    public boolean isVisible() { return this.visible; }

    public void show(float anchorX, float anchorY, float screenW, float screenH)
    {
        this.visible = true;
        this.popupX = Math.min(anchorX, screenW - WIDTH - 8.0F);
        this.popupY = anchorY - HEIGHT - 4.0F;
        if (this.popupY < 8.0F) this.popupY = 8.0F;
    }

    public void hide() { this.visible = false; }

    public void setCustomEmojis(List<ChatEmoji> emojis)
    {
        this.customEmojis.clear();
        if (emojis != null) this.customEmojis.addAll(emojis);
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme, int mx, int my)
    {
        if (!this.visible) return;

        float x = this.popupX;
        float y = this.popupY;

        // Card background
        NanoUi.drawWindow(vg, stack, x, y, WIDTH, HEIGHT, theme);

        int fontBold = NanoFontBook.uiBold();
        int fontRegular = NanoFontBook.uiRegular();

        // Header
        NanoRenderUtils.drawLabel(vg, stack, fontBold, x + PAD, y + HEADER_H * 0.5F,
                12.0F, "Emoji", theme.textArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Grid
        NanoUi.beginClip(vg, x, y + HEADER_H, WIDTH, HEIGHT - HEADER_H);

        int cols = (int) ((WIDTH - PAD * 2.0F) / CELL);
        this.hoveredIndex = -1;

        for (int i = 0; i < BUILTIN_EMOJI.length; i++)
        {
            int col = i % cols;
            int row = i / cols;
            float cx = x + PAD + col * CELL;
            float cy = y + HEADER_H + PAD + row * CELL;

            boolean hovered = mx >= cx && mx <= cx + CELL && my >= cy && my <= cy + CELL;
            if (hovered)
            {
                this.hoveredIndex = i;
                NanoRenderUtils.fillRoundedRect(vg, cx, cy, CELL, CELL, 4.0F,
                        NanoRenderUtils.argb(stack, theme.controlHoverArgb()));
            }

            NanoRenderUtils.drawLabel(vg, stack, fontRegular,
                    cx + CELL * 0.5F, cy + CELL * 0.5F, 14.0F,
                    BUILTIN_EMOJI[i], theme.textArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }

        NanoUi.endClip(vg);
    }

    /**
     * Handle click. Returns the selected emoji string, or null if no selection.
     * Returns empty string "" if clicked outside (dismiss).
     */
    public String handleClick(int mx, int my)
    {
        if (!this.visible) return null;

        if (mx < this.popupX || mx > this.popupX + WIDTH
                || my < this.popupY || my > this.popupY + HEIGHT)
        {
            this.visible = false;
            return "";
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < BUILTIN_EMOJI.length)
        {
            String emoji = BUILTIN_EMOJI[this.hoveredIndex];
            this.visible = false;
            return emoji;
        }
        return null;
    }

    public boolean containsMouse(int mx, int my)
    {
        return this.visible && mx >= this.popupX && mx <= this.popupX + WIDTH
                && my >= this.popupY && my <= this.popupY + HEIGHT;
    }
}
