package client.ui.irc.component;

import client.chat.model.MailMessage;
import client.ui.layout.UiRect;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import client.ui.template.NanoScreenKit;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Mail inbox/outbox list panel.
 */
public final class IRCMailInbox
{
    private static final String ANIM = "irc.mail.";
    private static final float ROW_H = 48.0F;
    private static final float PAD = 8.0F;

    private UiAnimProfile animProfile;
    private float scrollOffset;
    private int hoveredIndex = -1;
    private boolean showInbox = true;

    public void setAnimProfile(UiAnimProfile profile) { this.animProfile = profile; }
    public boolean isShowInbox() { return this.showInbox; }
    public void setShowInbox(boolean inbox) { this.showInbox = inbox; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       List<MailMessage> mails, int mx, int my)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.surfaceArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        float y = r.y + PAD - this.scrollOffset;
        this.hoveredIndex = -1;

        // Tab bar: Inbox | Sent
        float tabW = 60.0F;
        float tabH = 24.0F;
        float tabY = r.y + 4;

        // Inbox tab
        boolean inboxHover = mx >= r.x + PAD && mx <= r.x + PAD + tabW && my >= tabY && my <= tabY + tabH;
        int inboxBg = this.showInbox ? theme.accentArgb() : (inboxHover ? theme.controlHoverArgb() : theme.controlArgb());
        NanoRenderUtils.fillRoundedRect(vg, r.x + PAD, tabY, tabW, tabH, 4.0F, NanoRenderUtils.argb(stack, inboxBg));
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 11.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, this.showInbox ? 0xFFFFFFFF : theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, r.x + PAD + tabW * 0.5F, tabY + tabH * 0.5F, "Inbox");

        // Sent tab
        float sentX = r.x + PAD + tabW + 4;
        boolean sentHover = mx >= sentX && mx <= sentX + tabW && my >= tabY && my <= tabY + tabH;
        int sentBg = !this.showInbox ? theme.accentArgb() : (sentHover ? theme.controlHoverArgb() : theme.controlArgb());
        NanoRenderUtils.fillRoundedRect(vg, sentX, tabY, tabW, tabH, 4.0F, NanoRenderUtils.argb(stack, sentBg));
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, !this.showInbox ? 0xFFFFFFFF : theme.textArgb()));
        NanoVG.nvgText(vg, sentX + tabW * 0.5F, tabY + tabH * 0.5F, "Sent");

        y = tabY + tabH + 8;

        // Mail list
        if (mails != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            for (int i = 0; i < mails.size(); i++)
            {
                if (y + ROW_H < r.y || y > r.y + r.h) { y += ROW_H; continue; }
                MailMessage mail = mails.get(i);
                boolean hover = mx >= r.x && mx <= r.x + r.w && my >= y && my < y + ROW_H;
                if (hover) this.hoveredIndex = i;

                float hoverT = UiAnimationBus.animateControl(ANIM + i + ".h", hover ? 1.0F : 0.0F, this.animProfile);
                int bg = NanoScreenKit.mixArgb(theme.surfaceArgb(), theme.controlHoverArgb(), hoverT);
                NanoRenderUtils.fillRoundedRect(vg, r.x + 2, y, r.w - 4, ROW_H - 2, 4.0F,
                        NanoRenderUtils.argb(stack, bg));

                // Unread indicator
                if (!mail.isRead())
                {
                    NanoRenderUtils.fillCircle(vg, r.x + PAD + 4, y + ROW_H * 0.5F, 3.0F,
                            NanoRenderUtils.argb(stack, theme.accentArgb()));
                }

                // Subject
                NanoVG.nvgFontFaceId(vg, mail.isRead() ? fontNormal : fontBold);
                NanoVG.nvgFontSize(vg, 13.0F);
                NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
                NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                NanoVG.nvgText(vg, r.x + PAD + 14, y + 6, mail.getSubject());

                // From/To + date
                String other = this.showInbox ? mail.getFromNick() : mail.getToNick();
                String dateStr = sdf.format(new Date(mail.getCreatedMs()));
                NanoVG.nvgFontFaceId(vg, fontNormal);
                NanoVG.nvgFontSize(vg, 11.0F);
                NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
                NanoVG.nvgText(vg, r.x + PAD + 14, y + 24, (this.showInbox ? "From: " : "To: ") + other + "  " + dateStr);

                y += ROW_H;
            }
        }
    }

    public int getHoveredIndex() { return this.hoveredIndex; }

    /** Handle click on tabs. Returns true if tab changed. */
    public boolean handleTabClick(int mx, int my, UiRect r)
    {
        if (r == null) return false;
        float tabY = r.y + 4;
        float tabH = 24.0F;
        float tabW = 60.0F;
        if (my >= tabY && my <= tabY + tabH)
        {
            if (mx >= r.x + PAD && mx <= r.x + PAD + tabW) { this.showInbox = true; return true; }
            if (mx >= r.x + PAD + tabW + 4 && mx <= r.x + PAD + tabW * 2 + 4) { this.showInbox = false; return true; }
        }
        return false;
    }

    public void scroll(float delta) { this.scrollOffset = Math.max(0, this.scrollOffset - delta * 20.0F); }
}