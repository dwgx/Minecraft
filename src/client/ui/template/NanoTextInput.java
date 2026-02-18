package client.ui.template;

import client.setting.StringSetting;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.system.MemoryStack;

public final class NanoTextInput
{
    private static final int KEY_ESCAPE = 1;
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

    private boolean focused;
    private boolean dragging;
    private int cursor;
    private int anchor;
    private float scrollX;
    private long blinkStartedAtNanos;

    public NanoTextInput()
    {
        this.focused = false;
        this.dragging = false;
        this.cursor = 0;
        this.anchor = 0;
        this.scrollX = 0.0F;
        this.blinkStartedAtNanos = System.nanoTime();
    }

    public boolean isFocused()
    {
        return this.focused;
    }

    public void blur()
    {
        this.focused = false;
        this.dragging = false;
    }

    public void onMouseDown(int mouseX, int mouseY, float x, float y, float w, float h, long vg, int fontId, float fontSize, String text)
    {
        if (!contains(mouseX, mouseY, x, y, w, h))
        {
            this.blur();
            return;
        }

        this.focused = true;
        this.dragging = true;
        int index = this.caretIndexFromMouse(vg, fontId, fontSize, safe(text), x, w, mouseX);
        this.cursor = index;
        this.anchor = index;
        this.restartBlink();
    }

    public void onMouseDrag(int mouseX, float x, float w, long vg, int fontId, float fontSize, String text)
    {
        if (!this.focused || !this.dragging)
        {
            return;
        }

        this.cursor = this.caretIndexFromMouse(vg, fontId, fontSize, safe(text), x, w, mouseX);
        this.restartBlink();
    }

    public void onMouseUp()
    {
        this.dragging = false;
    }

    public boolean handleKeyTyped(char typedChar, int keyCode, StringSetting setting)
    {
        if (!this.focused || setting == null)
        {
            return false;
        }

        String text = safe(setting.get());
        boolean shift = GuiScreen.isShiftKeyDown();
        boolean ctrl = GuiScreen.isCtrlKeyDown();

        if (ctrl)
        {
            if (keyCode == KEY_A)
            {
                this.cursor = text.length();
                this.anchor = 0;
                this.restartBlink();
                return true;
            }

            if (keyCode == KEY_C)
            {
                String selected = this.selectedText(text);

                if (!selected.isEmpty())
                {
                    GuiScreen.setClipboardString(selected);
                }

                return true;
            }

            if (keyCode == KEY_X)
            {
                String selected = this.selectedText(text);

                if (!selected.isEmpty())
                {
                    GuiScreen.setClipboardString(selected);
                    this.deleteSelection(setting, text);
                }

                return true;
            }

            if (keyCode == KEY_V)
            {
                this.insertText(setting, text, safe(GuiScreen.getClipboardString()));
                return true;
            }
        }

        if (keyCode == KEY_LEFT)
        {
            this.moveCursor(text, -1, shift);
            return true;
        }

        if (keyCode == KEY_RIGHT)
        {
            this.moveCursor(text, 1, shift);
            return true;
        }

        if (keyCode == KEY_HOME)
        {
            this.moveCursorTo(0, shift);
            return true;
        }

        if (keyCode == KEY_END)
        {
            this.moveCursorTo(text.length(), shift);
            return true;
        }

        if (keyCode == KEY_BACK)
        {
            this.backspace(setting, text);
            return true;
        }

        if (keyCode == KEY_DELETE)
        {
            this.delete(setting, text);
            return true;
        }

        if (keyCode == KEY_RETURN || keyCode == KEY_NUMPADENTER || keyCode == KEY_ESCAPE)
        {
            this.blur();
            return true;
        }

        if (typedChar != 0 && typedChar != '\r' && typedChar != '\n' && !Character.isISOControl(typedChar))
        {
            this.insertText(setting, text, String.valueOf(typedChar));
            return true;
        }

        return false;
    }

    public void draw(long vg, MemoryStack stack, int fontId, NanoTheme theme, float x, float y, float w, float h, float scale, float textSize, String text, String placeholder, boolean hovered, boolean active, String animKey)
    {
        String value = safe(text);
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        boolean focusedVisual = this.focused && active;
        float focus = UiAnimationBus.animate(animKey + ".focus", focusedVisual ? 1.0F : 0.0F, 0.58F, 0.62F, UiAnimation.Type.EASE_OUT, true);
        float hoverRatio = UiAnimationBus.animate(animKey + ".hover", hovered ? 1.0F : 0.0F, 0.58F, 0.62F, UiAnimation.Type.EASE_OUT, true);
        int fill = mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.42F + hoverRatio * 0.26F + focus * 0.22F));
        int border = mixArgb(NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 92), NanoRenderUtils.withAlpha(theme.accentArgb(), 168), UiMotion.clamp01(focus * 0.85F));
        float radius = Math.min(h * 0.5F, Math.max(2.0F, theme.controlRadius()));
        NanoUi.drawSurface(vg, stack, x, y, w, h, radius, fill, border);

        float innerPad = Math.max(5.0F, 6.0F * k);
        float innerX = x + innerPad;
        float innerW = Math.max(4.0F, w - innerPad * 2.0F);
        this.ensureCursorInRange(value.length());
        this.updateScroll(vg, fontId, textSize, value, innerW);
        float baselineY = y + h * 0.5F + Math.max(0.0F, h * 0.03F - 0.05F);
        NanoUi.beginClip(vg, x + 1.0F, y + 1.0F, Math.max(1.0F, w - 2.0F), Math.max(1.0F, h - 2.0F));

        if (focusedVisual && this.hasSelection())
        {
            int start = Math.min(this.cursor, this.anchor);
            int end = Math.max(this.cursor, this.anchor);
            float selStart = innerX + NanoRenderUtils.textWidth(vg, fontId, textSize, value.substring(0, start)) - this.scrollX;
            float selEnd = innerX + NanoRenderUtils.textWidth(vg, fontId, textSize, value.substring(0, end)) - this.scrollX;
            float selW = Math.max(0.0F, selEnd - selStart);

            if (selW > 0.0F)
            {
                NanoUi.drawSurface(vg, stack, selStart, y + 2.0F, selW, Math.max(1.0F, h - 4.0F), Math.min(2.0F, radius), NanoRenderUtils.withAlpha(theme.accentSoftArgb(), 156), 0);
            }
        }

        if (value.isEmpty())
        {
            NanoUi.drawLeftText(vg, stack, fontId, innerX, baselineY, textSize, NanoRenderUtils.withAlpha(theme.textWeakArgb(), 164), safe(placeholder));
        }
        else
        {
            NanoUi.drawLeftText(vg, stack, fontId, innerX - this.scrollX, baselineY, textSize, theme.textArgb(), value);
        }

        if (focusedVisual && this.isCaretVisible())
        {
            float caretX = innerX + NanoRenderUtils.textWidth(vg, fontId, textSize, value.substring(0, this.cursor)) - this.scrollX;
            float caretTop = y + Math.max(1.8F, h * 0.20F);
            float caretH = Math.max(2.0F, h - (caretTop - y) * 2.0F);
            NanoUi.drawSurface(vg, stack, caretX, caretTop, 1.1F, caretH, 0.55F, NanoRenderUtils.withAlpha(theme.textArgb(), 230), 0);
        }

        NanoUi.endClip(vg);
    }

    private void backspace(StringSetting setting, String text)
    {
        if (this.deleteSelection(setting, text))
        {
            return;
        }

        if (this.cursor <= 0 || text.isEmpty())
        {
            return;
        }

        int next = this.cursor - 1;
        String updated = text.substring(0, next) + text.substring(this.cursor);
        setting.set(updated);
        String normalized = safe(setting.get());
        this.cursor = Math.min(next, normalized.length());
        this.anchor = this.cursor;
        this.restartBlink();
    }

    private void delete(StringSetting setting, String text)
    {
        if (this.deleteSelection(setting, text))
        {
            return;
        }

        if (this.cursor >= text.length() || text.isEmpty())
        {
            return;
        }

        String updated = text.substring(0, this.cursor) + text.substring(this.cursor + 1);
        setting.set(updated);
        String normalized = safe(setting.get());
        this.cursor = Math.min(this.cursor, normalized.length());
        this.anchor = this.cursor;
        this.restartBlink();
    }

    private void insertText(StringSetting setting, String text, String input)
    {
        String insertion = safe(input);

        if (insertion.isEmpty())
        {
            return;
        }

        int start = Math.min(this.cursor, this.anchor);
        int end = Math.max(this.cursor, this.anchor);
        String updated = text.substring(0, start) + insertion + text.substring(end);
        setting.set(updated);
        String normalized = safe(setting.get());
        int nextCursor = start + insertion.length();
        this.cursor = Math.min(nextCursor, normalized.length());
        this.anchor = this.cursor;
        this.restartBlink();
    }

    private boolean deleteSelection(StringSetting setting, String text)
    {
        if (!this.hasSelection())
        {
            return false;
        }

        int start = Math.min(this.cursor, this.anchor);
        int end = Math.max(this.cursor, this.anchor);
        setting.set(text.substring(0, start) + text.substring(end));
        String normalized = safe(setting.get());
        this.cursor = Math.min(start, normalized.length());
        this.anchor = this.cursor;
        this.restartBlink();
        return true;
    }

    private void moveCursor(String text, int delta, boolean select)
    {
        int next = this.cursor + (delta < 0 ? -1 : 1);
        next = Math.max(0, Math.min(text.length(), next));
        this.cursor = next;

        if (!select)
        {
            this.anchor = this.cursor;
        }

        this.restartBlink();
    }

    private void moveCursorTo(int target, boolean select)
    {
        this.cursor = Math.max(0, target);

        if (!select)
        {
            this.anchor = this.cursor;
        }

        this.restartBlink();
    }

    private void ensureCursorInRange(int length)
    {
        int safeLen = Math.max(0, length);
        this.cursor = Math.max(0, Math.min(this.cursor, safeLen));
        this.anchor = Math.max(0, Math.min(this.anchor, safeLen));
    }

    private void updateScroll(long vg, int fontId, float textSize, String text, float innerWidth)
    {
        float caret = NanoRenderUtils.textWidth(vg, fontId, textSize, text.substring(0, this.cursor));
        float maxVisible = Math.max(1.0F, innerWidth - 2.0F);

        if (caret - this.scrollX > maxVisible)
        {
            this.scrollX = caret - maxVisible;
        }

        if (caret - this.scrollX < 0.0F)
        {
            this.scrollX = caret;
        }

        float textWidth = NanoRenderUtils.textWidth(vg, fontId, textSize, text);
        float maxScroll = Math.max(0.0F, textWidth - maxVisible);
        this.scrollX = UiMotion.clamp(this.scrollX, 0.0F, maxScroll);
    }

    private int caretIndexFromMouse(long vg, int fontId, float textSize, String text, float x, float w, int mouseX)
    {
        if (vg == 0L || fontId < 0)
        {
            return text.length();
        }

        float innerPad = Math.max(5.0F, Math.min(10.0F, w * 0.08F));
        float innerX = x + innerPad;
        float localX = (float)mouseX - innerX + this.scrollX;

        if (localX <= 0.0F)
        {
            return 0;
        }

        int best = text.length();
        float bestDist = Float.MAX_VALUE;

        for (int i = 0; i <= text.length(); ++i)
        {
            float px = NanoRenderUtils.textWidth(vg, fontId, textSize, text.substring(0, i));
            float dist = Math.abs(px - localX);

            if (dist < bestDist)
            {
                best = i;
                bestDist = dist;
            }
        }

        return best;
    }

    private String selectedText(String text)
    {
        if (!this.hasSelection())
        {
            return "";
        }

        int start = Math.min(this.cursor, this.anchor);
        int end = Math.max(this.cursor, this.anchor);
        return text.substring(start, end);
    }

    private boolean hasSelection()
    {
        return this.cursor != this.anchor;
    }

    private void restartBlink()
    {
        this.blinkStartedAtNanos = System.nanoTime();
    }

    private boolean isCaretVisible()
    {
        long elapsed = System.nanoTime() - this.blinkStartedAtNanos;
        return (elapsed / 500_000_000L) % 2L == 0L;
    }

    private static boolean contains(int mouseX, int mouseY, float x, float y, float w, float h)
    {
        return (float)mouseX >= x && (float)mouseY >= y && (float)mouseX <= x + w && (float)mouseY <= y + h;
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }

    private static int mixArgb(int from, int to, float t)
    {
        float k = UiMotion.clamp01(t);
        int a = lerpChannel((from >>> 24) & 255, (to >>> 24) & 255, k);
        int r = lerpChannel((from >>> 16) & 255, (to >>> 16) & 255, k);
        int g = lerpChannel((from >>> 8) & 255, (to >>> 8) & 255, k);
        int b = lerpChannel(from & 255, to & 255, k);
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    private static int lerpChannel(int from, int to, float t)
    {
        return NanoRenderUtils.clamp255(Math.round((float)from + (float)(to - from) * UiMotion.clamp01(t)));
    }
}
