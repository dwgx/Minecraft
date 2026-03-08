package client.ui.irc.component;

import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Mail compose panel with to, subject, body fields and send button.
 */
public final class IRCMailCompose
{
    private static final float PAD = 10.0F;

    private boolean visible;
    private String toNick = "";
    private String subject = "";
    private String body = "";
    private int activeField; // 0=to, 1=subject, 2=body
    private int sendBtnHover;

    public void show(String toNick)
    {
        this.toNick = toNick != null ? toNick : "";
        this.subject = "";
        this.body = "";
        this.activeField = toNick != null ? 1 : 0;
        this.visible = true;
    }

    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public String getToNick() { return this.toNick; }
    public String getSubject() { return this.subject; }
    public String getBody() { return this.body; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r, int mx, int my)
    {
        if (!this.visible || r == null) return;

        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.panelArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        float y = r.y + PAD;
        float fieldW = r.w - PAD * 2;
        float fieldH = 26.0F;

        // Title
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 14.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(vg, r.x + PAD, y, "Compose Mail");
        y += 24;

        // To field
        renderField(vg, stack, theme, r.x + PAD, y, fieldW, fieldH, "To:", this.toNick, this.activeField == 0);
        y += fieldH + 6;

        // Subject field
        renderField(vg, stack, theme, r.x + PAD, y, fieldW, fieldH, "Subject:", this.subject, this.activeField == 1);
        y += fieldH + 6;

        // Body field (taller)
        float bodyH = Math.max(60.0F, r.y + r.h - y - 44);
        renderField(vg, stack, theme, r.x + PAD, y, fieldW, bodyH, "Body:", this.body, this.activeField == 2);
        y += bodyH + 8;

        // Send button
        float btnW = 80.0F;
        float btnH = 28.0F;
        float btnX = r.x + r.w - PAD - btnW;
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= y && my <= y + btnH;
        this.sendBtnHover = hover ? 1 : 0;
        int bg = hover ? 0xFF3BA55D : 0xFF43B581;
        NanoRenderUtils.fillRoundedRect(vg, btnX, y, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, bg));
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, btnX + btnW * 0.5F, y + btnH * 0.5F, "Send");
    }

    private void renderField(long vg, MemoryStack stack, NanoTheme theme,
                             float x, float y, float w, float h, String label, String value, boolean active)
    {
        int bg = active ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, x, y, w, h, 4.0F, NanoRenderUtils.argb(stack, bg));
        if (active)
        {
            NanoRenderUtils.strokeRoundedRect(vg, x, y, w, h, 4.0F, 1.0F,
                    NanoRenderUtils.argb(stack, theme.accentArgb()));
        }
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiRegular());
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        float textY = h > 30 ? y + 14 : y + h * 0.5F;
        if (value.isEmpty())
        {
            NanoVG.nvgText(vg, x + 8, textY, label);
        }
        else
        {
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
            NanoVG.nvgText(vg, x + 8, textY, value);
        }
    }

    public void handleClick(int mx, int my, UiRect r)
    {
        if (!this.visible || r == null) return;
        float y = r.y + PAD + 24;
        float fieldH = 26.0F;
        if (my >= y && my < y + fieldH) this.activeField = 0;
        else if (my >= y + fieldH + 6 && my < y + fieldH * 2 + 6) this.activeField = 1;
        else if (my >= y + fieldH * 2 + 12) this.activeField = 2;
    }

    public boolean isSendHovered() { return this.sendBtnHover == 1; }

    public void typeChar(char c)
    {
        if (this.activeField == 0) this.toNick += c;
        else if (this.activeField == 1) this.subject += c;
        else this.body += c;
    }

    public void backspace()
    {
        if (this.activeField == 0 && !this.toNick.isEmpty())
            this.toNick = this.toNick.substring(0, this.toNick.length() - 1);
        else if (this.activeField == 1 && !this.subject.isEmpty())
            this.subject = this.subject.substring(0, this.subject.length() - 1);
        else if (this.activeField == 2 && !this.body.isEmpty())
            this.body = this.body.substring(0, this.body.length() - 1);
    }
}
