package client.ui.irc.component;

import client.chat.model.UserProfile;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Edit own profile: display name, bio, theme color.
 */
public final class IRCProfileEditPanel
{
    private static final float PAD = 12.0F;

    private boolean visible;
    private String displayName = "";
    private String bio = "";
    private String themeColor = "#5B9BD5";
    private int activeField; // 0=name, 1=bio, 2=color
    private int saveBtnHover;

    public void show(UserProfile profile)
    {
        this.visible = true;
        if (profile != null)
        {
            this.displayName = profile.getDisplayName() != null ? profile.getDisplayName() : "";
            this.bio = profile.getBio() != null ? profile.getBio() : "";
            this.themeColor = profile.getThemeColor() != null ? profile.getThemeColor() : "#5B9BD5";
        }
    }

    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public String getDisplayName() { return this.displayName; }
    public String getBio() { return this.bio; }
    public String getThemeColor() { return this.themeColor; }

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
        NanoVG.nvgText(vg, r.x + PAD, y, "Edit Profile");
        y += 24;

        // Display Name
        renderField(vg, stack, theme, r.x + PAD, y, fieldW, fieldH, "Display Name", this.displayName, this.activeField == 0);
        y += fieldH + 8;

        // Bio
        float bioH = 60.0F;
        renderField(vg, stack, theme, r.x + PAD, y, fieldW, bioH, "Bio", this.bio, this.activeField == 1);
        y += bioH + 8;

        // Theme Color
        renderField(vg, stack, theme, r.x + PAD, y, fieldW - 30, fieldH, "Theme Color", this.themeColor, this.activeField == 2);
        // Color preview
        int previewColor = parseColor(this.themeColor);
        NanoRenderUtils.fillRoundedRect(vg, r.x + r.w - PAD - 22, y + 3, 20, 20, 4.0F,
                NanoRenderUtils.argb(stack, previewColor));
        y += fieldH + 16;

        // Save button
        float btnW = 80.0F;
        float btnH = 28.0F;
        float btnX = r.x + r.w - PAD - btnW;
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= y && my <= y + btnH;
        this.saveBtnHover = hover ? 1 : 0;
        int bg = hover ? 0xFF3BA55D : 0xFF43B581;
        NanoRenderUtils.fillRoundedRect(vg, btnX, y, btnW, btnH, 4.0F, NanoRenderUtils.argb(stack, bg));
        NanoVG.nvgFontFaceId(vg, fontBold);
        NanoVG.nvgFontSize(vg, 12.0F);
        NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, 0xFFFFFFFF));
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(vg, btnX + btnW * 0.5F, y + btnH * 0.5F, "Save");
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
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        float textY = h > 30 ? y + 14 : y + h * 0.5F;
        if (value.isEmpty())
        {
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
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
        float bioH = 60.0F;
        float nameEnd = y + fieldH;
        float bioStart = nameEnd + 8;
        float bioEnd = bioStart + bioH;
        float colorStart = bioEnd + 8;
        if (my >= y && my < nameEnd) this.activeField = 0;
        else if (my >= bioStart && my < bioEnd) this.activeField = 1;
        else if (my >= colorStart && my < colorStart + fieldH) this.activeField = 2;
    }

    public boolean isSaveHovered() { return this.saveBtnHover == 1; }

    public void typeChar(char c)
    {
        if (this.activeField == 0) this.displayName += c;
        else if (this.activeField == 1) this.bio += c;
        else this.themeColor += c;
    }

    public void backspace()
    {
        if (this.activeField == 0 && !this.displayName.isEmpty())
            this.displayName = this.displayName.substring(0, this.displayName.length() - 1);
        else if (this.activeField == 1 && !this.bio.isEmpty())
            this.bio = this.bio.substring(0, this.bio.length() - 1);
        else if (this.activeField == 2 && !this.themeColor.isEmpty())
            this.themeColor = this.themeColor.substring(0, this.themeColor.length() - 1);
    }

    private static int parseColor(String hex)
    {
        try
        {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return 0xFF000000 | Integer.parseInt(hex, 16);
        }
        catch (Exception e) { return 0xFF5B9BD5; }
    }
}
