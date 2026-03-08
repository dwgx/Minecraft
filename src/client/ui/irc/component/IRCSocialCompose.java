package client.ui.irc.component;

import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Compose a new social post.
 */
public final class IRCSocialCompose
{
    private boolean visible;
    private String content = "";
    private int postBtnHover;

    public void show() { this.content = ""; this.visible = true; }
    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public String getContent() { return this.content; }
    public void clear() { this.content = ""; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r, int mx, int my)
    {
        if (r == null) return;

        float pad = 10.0F;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.panelArgb()));

        // Title
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiBold());
        NanoVG.nvgFontSize(vg, 14.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgText(vg, r.x + pad, r.y + pad, "New Post");

        // Text area
        float taY = r.y + pad + 24;
        float taH = r.h - 70;
        NanoRenderUtils.fillRoundedRect(vg, r.x + pad, taY, r.w - pad * 2, taH, 4.0F,
                NanoRenderUtils.argb(stack, theme.controlArgb()));
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiRegular());
        NanoVG.nvgFontSize(vg, 13.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack,
                this.content.isEmpty() ? theme.textDimArgb() : theme.textArgb()));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        NanoVG.nvgTextBox(vg, r.x + pad + 6, taY + 6, r.w - pad * 2 - 12,
                this.content.isEmpty() ? "What's on your mind?" : this.content);

        // Post button
        float btnW = 70.0F;
        float btnH = 28.0F;
        float btnX = r.x + r.w - pad - btnW;
        float btnY = r.y + r.h - btnH - pad;
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        this.postBtnHover = hover ? 1 : 0;
        int bg = hover ? theme.accentArgb() : 0xFF5865F2;
        NanoRenderUtils.fillRoundedRect(vg, btnX, btnY, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, bg));
        NanoVG.nvgFontFaceId(vg, NanoFontBook.uiBold());
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, btnX + btnW * 0.5F, btnY + btnH * 0.5F, "Post");
    }

    public boolean isPostHovered() { return this.postBtnHover == 1; }

    public void typeChar(char c) { this.content += c; }

    public void backspace()
    {
        if (!this.content.isEmpty())
            this.content = this.content.substring(0, this.content.length() - 1);
    }
}
