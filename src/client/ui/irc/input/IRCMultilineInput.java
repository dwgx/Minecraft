package client.ui.irc.input;

import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Multiline text input for IRC messages.
 * Enter sends, Shift+Enter inserts newline.
 * Supports cursor, selection, clipboard, word wrap.
 */
public final class IRCMultilineInput
{
    private static final int KEY_BACK = 14;
    private static final int KEY_RETURN = 28;
    private static final int KEY_A = 30;
    private static final int KEY_X = 45;
    private static final int KEY_C = 46;
    private static final int KEY_V = 47;
    private static final int KEY_NUMPADENTER = 156;
    private static final int KEY_HOME = 199;
    private static final int KEY_LEFT = 203;
    private static final int KEY_RIGHT = 205;
    private static final int KEY_END = 207;
    private static final int KEY_DELETE = 211;

    private static final int MAX_LENGTH = 2000;
    private static final int MAX_LINES = 8;

    private final StringBuilder buffer = new StringBuilder(256);
    private boolean focused;
    private int cursor;
    private int anchor;
    private long blinkStartedAtNanos;

    public interface SendListener
    {
        void onSend(String text);
    }

    private SendListener sendListener;

    public IRCMultilineInput() {}

    public void setSendListener(SendListener listener) { this.sendListener = listener; }
    public boolean isFocused() { return this.focused; }
    public void focus() { this.focused = true; this.blinkStartedAtNanos = System.nanoTime(); }
    public void blur() { this.focused = false; }
    public String getText() { return this.buffer.toString(); }
    public boolean isEmpty() { return this.buffer.length() == 0; }

    public void clear()
    {
        this.buffer.setLength(0);
        this.cursor = 0;
        this.anchor = 0;
    }
    public void insertText(String text)
    {
        if (text == null || text.isEmpty()) return;
        if (hasSelection()) deleteSelection();
        this.buffer.insert(this.cursor, text);
        this.cursor += text.length();
        this.anchor = this.cursor;
        restartBlink();
    }

    public boolean handleKeyTyped(char typedChar, int keyCode)
    {
        if (!this.focused) return false;

        boolean ctrl = GuiScreen.isCtrlKeyDown();
        boolean shift = GuiScreen.isShiftKeyDown();

        // Enter = send, Shift+Enter = newline
        if (keyCode == KEY_RETURN || keyCode == KEY_NUMPADENTER)
        {
            if (shift)
            {
                insertChar('\n');
                return true;
            }
            else
            {
                String text = this.buffer.toString().trim();
                if (!text.isEmpty() && this.sendListener != null)
                {
                    this.sendListener.onSend(text);
                    this.clear();
                }
                return true;
            }
        }

        if (ctrl && keyCode == KEY_A) { selectAll(); return true; }
        if (ctrl && keyCode == KEY_C) { copySelection(); return true; }
        if (ctrl && keyCode == KEY_X) { cutSelection(); return true; }
        if (ctrl && keyCode == KEY_V) { paste(); return true; }

        if (keyCode == KEY_BACK) { backspace(); return true; }
        if (keyCode == KEY_DELETE) { delete(); return true; }
        if (keyCode == KEY_LEFT) { moveLeft(shift); return true; }
        if (keyCode == KEY_RIGHT) { moveRight(shift); return true; }
        if (keyCode == KEY_HOME) { moveHome(shift); return true; }
        if (keyCode == KEY_END) { moveEnd(shift); return true; }

        if (!Character.isISOControl(typedChar) || typedChar == '\t')
        {
            insertChar(typedChar);
            return true;
        }

        return false;
    }

    public void onMouseDown(int mouseX, int mouseY, float x, float y, float w, float h,
                            long vg, int fontId, float fontSize)
    {
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h)
        {
            this.blur();
            return;
        }
        this.focus();
        int idx = caretFromMouse(vg, fontId, fontSize, x, w, mouseX);
        this.cursor = idx;
        this.anchor = idx;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme, float x, float y, float w, float h)
    {
        int fontId = NanoFontBook.uiRegular();
        float fontSize = 13.0F;
        float pad = 8.0F;
        String text = this.buffer.toString();

        // Background
        NanoRenderUtils.fillRoundedRect(vg, x, y, w, h, theme.controlRadius(),
                NanoRenderUtils.argb(stack, this.focused ? theme.controlHoverArgb() : theme.controlArgb()));

        // Clip
        NanoVG.nvgSave(vg);
        NanoVG.nvgIntersectScissor(vg, x + pad, y, w - pad * 2.0F, h);

        if (text.isEmpty() && !this.focused)
        {
            NanoRenderUtils.drawLabel(vg, stack, fontId, x + pad, y + h * 0.5F, fontSize,
                    "Type a message...", theme.textWeakArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        }
        else
        {
            // Draw text (single line for now, multiline wrap in future iteration)
            float textY = y + h * 0.5F;
            NanoRenderUtils.drawLabel(vg, stack, fontId, x + pad, textY, fontSize,
                    text, theme.textArgb(), NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // Selection highlight
            if (this.focused && this.cursor != this.anchor)
            {
                int selStart = Math.min(this.cursor, this.anchor);
                int selEnd = Math.max(this.cursor, this.anchor);
                String before = text.substring(0, selStart);
                String selected = text.substring(selStart, selEnd);
                float selX = x + pad + NanoRenderUtils.textWidth(vg, fontId, fontSize, before);
                float selW = NanoRenderUtils.textWidth(vg, fontId, fontSize, selected);
                NanoRenderUtils.fillRect(vg, selX, y + 4.0F, selW, h - 8.0F,
                        NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.accentArgb(), 80)));
            }

            // Cursor blink
            if (this.focused)
            {
                long elapsed = (System.nanoTime() - this.blinkStartedAtNanos) / 1000000L;
                boolean visible = (elapsed % 1000L) < 500L;
                if (visible)
                {
                    String beforeCursor = text.substring(0, Math.min(this.cursor, text.length()));
                    float cursorX = x + pad + NanoRenderUtils.textWidth(vg, fontId, fontSize, beforeCursor);
                    NanoRenderUtils.fillRect(vg, cursorX, y + 6.0F, 1.5F, h - 12.0F,
                            NanoRenderUtils.argb(stack, theme.textArgb()));
                }
            }
        }

        NanoVG.nvgRestore(vg);
    }

    // --- Internal editing methods ---

    private void insertChar(char c)
    {
        deleteSelection();
        if (this.buffer.length() >= MAX_LENGTH) return;
        if (c == '\n')
        {
            // Count existing newlines
            int lines = 1;
            for (int i = 0; i < this.buffer.length(); i++)
            {
                if (this.buffer.charAt(i) == '\n') lines++;
            }
            if (lines >= MAX_LINES) return;
        }
        this.buffer.insert(this.cursor, c);
        this.cursor++;
        this.anchor = this.cursor;
        restartBlink();
    }

    private void backspace()
    {
        if (hasSelection()) { deleteSelection(); return; }
        if (this.cursor > 0)
        {
            this.buffer.deleteCharAt(this.cursor - 1);
            this.cursor--;
            this.anchor = this.cursor;
            restartBlink();
        }
    }

    private void delete()
    {
        if (hasSelection()) { deleteSelection(); return; }
        if (this.cursor < this.buffer.length())
        {
            this.buffer.deleteCharAt(this.cursor);
            restartBlink();
        }
    }

    private void deleteSelection()
    {
        if (!hasSelection()) return;
        int start = Math.min(this.cursor, this.anchor);
        int end = Math.max(this.cursor, this.anchor);
        this.buffer.delete(start, end);
        this.cursor = start;
        this.anchor = start;
        restartBlink();
    }

    private void selectAll()
    {
        this.anchor = 0;
        this.cursor = this.buffer.length();
    }

    private void copySelection()
    {
        if (!hasSelection()) return;
        int start = Math.min(this.cursor, this.anchor);
        int end = Math.max(this.cursor, this.anchor);
        GuiScreen.setClipboardString(this.buffer.substring(start, end));
    }

    private void cutSelection()
    {
        copySelection();
        deleteSelection();
    }

    private void paste()
    {
        String clip = GuiScreen.getClipboardString();
        if (clip == null || clip.isEmpty()) return;
        deleteSelection();
        int space = MAX_LENGTH - this.buffer.length();
        if (space <= 0) return;
        if (clip.length() > space) clip = clip.substring(0, space);
        this.buffer.insert(this.cursor, clip);
        this.cursor += clip.length();
        this.anchor = this.cursor;
        restartBlink();
    }

    private void moveLeft(boolean shift)
    {
        if (this.cursor > 0) this.cursor--;
        if (!shift) this.anchor = this.cursor;
        restartBlink();
    }

    private void moveRight(boolean shift)
    {
        if (this.cursor < this.buffer.length()) this.cursor++;
        if (!shift) this.anchor = this.cursor;
        restartBlink();
    }

    private void moveHome(boolean shift)
    {
        this.cursor = 0;
        if (!shift) this.anchor = this.cursor;
        restartBlink();
    }

    private void moveEnd(boolean shift)
    {
        this.cursor = this.buffer.length();
        if (!shift) this.anchor = this.cursor;
        restartBlink();
    }

    private boolean hasSelection() { return this.cursor != this.anchor; }
    private void restartBlink() { this.blinkStartedAtNanos = System.nanoTime(); }

    private int caretFromMouse(long vg, int fontId, float fontSize, float x, float w, int mouseX)
    {
        String text = this.buffer.toString();
        float relX = mouseX - x - 8.0F;
        if (relX <= 0) return 0;
        for (int i = 1; i <= text.length(); i++)
        {
            float tw = NanoRenderUtils.textWidth(vg, fontId, fontSize, text.substring(0, i));
            if (tw > relX) return i - 1;
        }
        return text.length();
    }
}