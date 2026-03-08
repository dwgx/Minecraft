package client.ui.irc.component;

import client.chat.model.MailMessage;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Mail detail view showing full message content.
 */
public final class IRCMailDetail
{
    private boolean visible;
    private MailMessage mail;
    private int replyBtnHover;
    private int deleteBtnHover;

    public void show(MailMessage mail) { this.mail = mail; this.visible = true; }
    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public MailMessage getMail() { return this.mail; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r, int mx, int my)
    {
        if (!this.visible || this.mail == null || r == null) return;

        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.panelArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        float pad = 12.0F;
        float y = r.y + pad;

        // Subject
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 16.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(vg, r.x + pad, y, this.mail.getSubject());
        y += 24;

        // From/To + date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        NanoVG.nvgFontFaceId(vg, fontNormal);
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
        NanoVG.nvgText(vg, r.x + pad, y, "From: " + this.mail.getFromNick()
                + "  |  To: " + this.mail.getToNick()
                + "  |  " + sdf.format(new Date(this.mail.getCreatedMs())));
        y += 20;

        // Separator
        NanoRenderUtils.fillRect(vg, r.x + pad, y, r.w - pad * 2, 1,
                NanoRenderUtils.argb(stack, theme.borderArgb()));
        y += 8;

        // Body
        NanoVG.nvgFontFaceId(vg, fontNormal);
        NanoVG.nvgFontSize(vg, 13.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        String body = this.mail.getBody() != null ? this.mail.getBody() : "";
        NanoVG.nvgTextBox(vg, r.x + pad, y, r.w - pad * 2, body);

        // Buttons at bottom
        float btnW = 70.0F;
        float btnH = 26.0F;
        float btnY = r.y + r.h - btnH - pad;

        // Reply
        boolean rHover = mx >= r.x + pad && mx <= r.x + pad + btnW && my >= btnY && my <= btnY + btnH;
        this.replyBtnHover = rHover ? 1 : 0;
        int rBg = rHover ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, r.x + pad, btnY, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, rBg));
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 11.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, r.x + pad + btnW * 0.5F, btnY + btnH * 0.5F, "Reply");

        // Delete
        float dX = r.x + pad + btnW + 8;
        boolean dHover = mx >= dX && mx <= dX + btnW && my >= btnY && my <= btnY + btnH;
        this.deleteBtnHover = dHover ? 1 : 0;
        int dBg = dHover ? 0xFFD83C3E : 0xFFED4245;
        NanoRenderUtils.fillRoundedRect(vg, dX, btnY, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, dBg));
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgText(vg, dX + btnW * 0.5F, btnY + btnH * 0.5F, "Delete");
    }

    /** @return 1=reply, 2=delete, 0=nothing */
    public int handleClick()
    {
        if (!this.visible) return 0;
        if (this.replyBtnHover == 1) return 1;
        if (this.deleteBtnHover == 1) return 2;
        return 0;
    }
}
