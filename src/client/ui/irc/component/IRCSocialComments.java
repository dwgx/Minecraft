package client.ui.irc.component;

import client.chat.model.SocialComment;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Comments list for a social post with inline compose.
 */
public final class IRCSocialComments
{
    private static final float ROW_H = 36.0F;
    private static final float PAD = 8.0F;

    private boolean visible;
    private long postId;
    private String inputText = "";
    private int sendBtnHover;

    public void show(long postId) { this.postId = postId; this.inputText = ""; this.visible = true; }
    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public long getPostId() { return this.postId; }
    public String getInputText() { return this.inputText; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       List<SocialComment> comments, int mx, int my)
    {
        if (!this.visible || r == null) return;

        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.panelArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
        float y = r.y + PAD;

        // Title
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 13.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(vg, r.x + PAD, y, "Comments");
        y += 20;

        // Comment list
        if (comments != null)
        {
            for (SocialComment c : comments)
            {
                if (y + ROW_H > r.y + r.h - 40) break;

                NanoVG.nvgFontFaceId(vg, fontBold);
                NanoVG.nvgFontSize(vg, 12.0F);
                NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.accentArgb()));
                NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                NanoVG.nvgText(vg, r.x + PAD, y, c.getAuthorNick());

                NanoVG.nvgFontFaceId(vg, fontNormal);
                NanoVG.nvgFontSize(vg, 11.0F);
                NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
                NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP);
                NanoVG.nvgText(vg, r.x + r.w - PAD, y, sdf.format(new Date(c.getCreatedMs())));

                NanoVG.nvgFontFaceId(vg, fontNormal);
                NanoVG.nvgFontSize(vg, 12.0F);
                NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
                NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                NanoVG.nvgText(vg, r.x + PAD, y + 16, c.getContent());

                y += ROW_H;
            }
        }

        // Input bar at bottom
        float inputY = r.y + r.h - 34;
        float inputW = r.w - PAD * 2 - 60;
        NanoRenderUtils.fillRoundedRect(vg, r.x + PAD, inputY, inputW, 26, 4.0F,
                NanoRenderUtils.argb(stack, theme.controlArgb()));
        NanoVG.nvgFontFaceId(vg, fontNormal);
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack,
                this.inputText.isEmpty() ? theme.textDimArgb() : theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, r.x + PAD + 6, inputY + 13,
                this.inputText.isEmpty() ? "Write a comment..." : this.inputText);

        // Send button
        float btnX = r.x + PAD + inputW + 4;
        float btnW = 52.0F;
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= inputY && my <= inputY + 26;
        this.sendBtnHover = hover ? 1 : 0;
        int bg = hover ? theme.accentArgb() : 0xFF5865F2;
        NanoRenderUtils.fillRoundedRect(vg, btnX, inputY, btnW, 26, 4.0F, NanoRenderUtils.argb(stack, bg));
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 11.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, btnX + btnW * 0.5F, inputY + 13, "Send");
    }

    public boolean isSendHovered() { return this.sendBtnHover == 1; }

    public void typeChar(char c) { this.inputText += c; }

    public void backspace()
    {
        if (!this.inputText.isEmpty())
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
    }

    public void clearInput() { this.inputText = ""; }
}
