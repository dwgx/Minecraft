package client.ui.component;

import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiControlAnimations;
import client.ui.template.UiMotion;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Inline numeric input field with validation and confirm/cancel.
 * Replaces duplicated number input logic across screens.
 */
public class NanoNumberInput extends NanoComponent
{
    public interface ValueCommitListener
    {
        void onCommit(double value);
    }

    private static final int KEY_ESCAPE = 1;
    private static final int KEY_RETURN = 28;
    private static final int KEY_NUMPADENTER = 156;
    private static final int KEY_BACKSPACE = 14;

    private boolean active;
    private StringBuilder buffer = new StringBuilder(16);
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    private int cursorPos;
    private ValueCommitListener listener;

    public NanoNumberInput(String animKeyPrefix)
    {
        super(animKeyPrefix);
    }

    public void setRange(double min, double max)
    {
        this.minValue = min;
        this.maxValue = max;
    }

    public void setListener(ValueCommitListener listener)
    {
        this.listener = listener;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void activate(double currentValue)
    {
        this.active = true;
        this.buffer.setLength(0);
        String formatted = formatValue(currentValue);
        this.buffer.append(formatted);
        this.cursorPos = formatted.length();
    }

    public void deactivate()
    {
        this.active = false;
        this.buffer.setLength(0);
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme,
                       UiAnimProfile animProfile, float scale, int mouseX, int mouseY)
    {
        if (this.shouldSkipRender() || !this.active)
        {
            return;
        }

        float radius = Math.min(this.height * 0.4F, theme.controlRadius());
        boolean hovered = this.isHovered(mouseX, mouseY);
        float focus = UiControlAnimations.focus(this.animKey("focus"), hovered || this.active, animProfile);

        // Background
        int fill = NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlActiveArgb(),
            UiMotion.clamp01(focus * 0.5F));
        int border = NanoScreenKit.mixArgb(
            NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 100),
            NanoRenderUtils.withAlpha(theme.accentArgb(), 180),
            UiMotion.clamp01(focus * 0.6F));
        NanoUi.drawSurface(vg, stack, this.x, this.y, this.width, this.height, radius, fill, border);

        // Text
        String text = this.buffer.toString();
        float fontSize = NanoScreenKit.scaled(11.0F, scale);
        float textX = this.x + NanoScreenKit.scaled(6.0F, scale);
        float textY = this.y + this.height * 0.5F;
        NanoUi.drawLeftText(vg, stack, -1, textX, textY, fontSize, theme.textArgb(), text);

        // Cursor blink
        long ms = System.currentTimeMillis();

        if ((ms / 500) % 2 == 0)
        {
            String beforeCursor = text.substring(0, Math.min(this.cursorPos, text.length()));
            float cursorX = textX + NanoRenderUtils.textWidth(vg, -1, fontSize, beforeCursor);
            NanoRenderUtils.fillRect(vg, cursorX, this.y + NanoScreenKit.scaled(3.0F, scale),
                NanoScreenKit.scaled(1.0F, scale), this.height - NanoScreenKit.scaled(6.0F, scale),
                NanoRenderUtils.argb(stack, theme.textArgb()));
        }
    }

    public boolean keyTyped(char typedChar, int keyCode)
    {
        if (!this.active)
        {
            return false;
        }

        if (keyCode == KEY_ESCAPE)
        {
            this.deactivate();
            return true;
        }

        if (keyCode == KEY_RETURN || keyCode == KEY_NUMPADENTER)
        {
            this.commit();
            return true;
        }

        if (keyCode == KEY_BACKSPACE)
        {
            if (this.cursorPos > 0 && this.buffer.length() > 0)
            {
                this.buffer.deleteCharAt(this.cursorPos - 1);
                this.cursorPos--;
            }

            return true;
        }

        // Allow digits, minus, dot
        if (isNumericChar(typedChar) && this.buffer.length() < 20)
        {
            this.buffer.insert(this.cursorPos, typedChar);
            this.cursorPos++;
            return true;
        }

        return true;
    }

    private void commit()
    {
        try
        {
            double value = Double.parseDouble(this.buffer.toString().trim());
            value = Math.max(this.minValue, Math.min(this.maxValue, value));

            if (this.listener != null)
            {
                this.listener.onCommit(value);
            }
        }
        catch (NumberFormatException ignored)
        {
        }

        this.deactivate();
    }

    private static boolean isNumericChar(char c)
    {
        return (c >= '0' && c <= '9') || c == '.' || c == '-';
    }

    private static String formatValue(double value)
    {
        if (value == (long) value)
        {
            return Long.toString((long) value);
        }

        return String.format("%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
