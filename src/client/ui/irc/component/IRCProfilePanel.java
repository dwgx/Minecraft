package client.ui.irc.component;

import client.chat.model.UserProfile;
import client.ui.layout.UiRect;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Rich profile panel with banner, avatar, stats cards, bio, and action buttons.
 * Renders in the right panel area when viewing a user's profile.
 */
public final class IRCProfilePanel
{
    private static final float PAD = 14.0F;
    private static final float BANNER_H = 80.0F;
    private static final float AVATAR_SIZE = 56.0F;
    private static final float STAT_CARD_H = 48.0F;
    private static final float BTN_H = 28.0F;
    private static final float SECTION_GAP = 12.0F;

    private boolean visible;
    private UserProfile profile;
    private int addFriendHover;
    private int sendMailHover;
    private int editProfileHover;
    private float scrollOffset;
    private float scrollTarget;

    public void show(UserProfile profile) { this.profile = profile; this.visible = true; }
    public void hide() { this.visible = false; }
    public boolean isVisible() { return this.visible; }
    public UserProfile getProfile() { return this.profile; }

    public void scroll(int delta)
    {
        this.scrollTarget -= delta * 0.4F;
        if (this.scrollTarget < 0.0F) this.scrollTarget = 0.0F;
    }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r, int mx, int my, boolean isSelf)
    {
        if (!this.visible || this.profile == null || r == null) return;

        this.scrollOffset += (this.scrollTarget - this.scrollOffset) * 0.3F;

        // Background
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.mainArgb()));

        NanoUi.beginClip(vg, r.x, r.y, r.w, r.h);

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        int themeArgb = parseColor(this.profile.getThemeColor());
        float y = r.y - this.scrollOffset;

        // === Banner gradient ===
        int bannerTop = themeArgb;
        int bannerBot = NanoRenderUtils.withAlpha(themeArgb, 60);
        NanoRenderUtils.fillRoundedRectGradient(vg, stack, r.x, y, r.w, BANNER_H,
                theme.surfaceRadius(), bannerTop, bannerBot, true);

        // Decorative pattern on banner (subtle dots)
        int dotColor = NanoRenderUtils.withAlpha(0xFFFFFFFF, 20);
        for (int dx = 0; dx < (int)(r.w / 20); dx++)
        {
            for (int dy = 0; dy < 4; dy++)
            {
                float dotX = r.x + dx * 20 + (dy % 2) * 10;
                float dotY = y + dy * 20 + 5;
                NanoRenderUtils.fillCircle(vg, dotX, dotY, 1.5F,
                        NanoRenderUtils.argb(stack, dotColor));
            }
        }

        // === Avatar (overlapping banner bottom) ===
        float avatarX = r.x + PAD + 4;
        float avatarY = y + BANNER_H - AVATAR_SIZE * 0.5F;

        // Avatar shadow
        NanoRenderUtils.fillCircle(vg, avatarX + AVATAR_SIZE * 0.5F, avatarY + AVATAR_SIZE * 0.5F + 2,
                AVATAR_SIZE * 0.5F + 3, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(0xFF000000, 40)));
        // Avatar ring
        NanoRenderUtils.fillCircle(vg, avatarX + AVATAR_SIZE * 0.5F, avatarY + AVATAR_SIZE * 0.5F,
                AVATAR_SIZE * 0.5F + 2, NanoRenderUtils.argb(stack, theme.mainArgb()));
        // Avatar circle
        NanoRenderUtils.fillCircle(vg, avatarX + AVATAR_SIZE * 0.5F, avatarY + AVATAR_SIZE * 0.5F,
                AVATAR_SIZE * 0.5F, NanoRenderUtils.argb(stack, themeArgb));
        // Avatar initial
        String initial = getInitial();
        NanoRenderUtils.drawLabel(vg, stack, fontBold,
                avatarX + AVATAR_SIZE * 0.5F, avatarY + AVATAR_SIZE * 0.5F,
                22.0F, initial, 0xFFFFFFFF,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        y += BANNER_H + AVATAR_SIZE * 0.5F + 8;

        // === Name and nick ===
        String displayName = this.profile.getDisplayName() != null
                ? this.profile.getDisplayName() : this.profile.getNick();
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y,
                16.0F, displayName, theme.textArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 20;

        NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD, y,
                12.0F, "@" + this.profile.getNick(), theme.textDimArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 18;

        // UID badge
        String uid = this.profile.getUid();
        if (uid != null && !uid.isEmpty())
        {
            NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD, y,
                    10.0F, "UID: " + uid, theme.accentArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            y += 16;
        }

        // === Divider ===
        NanoRenderUtils.fillRect(vg, r.x + PAD, y, r.w - PAD * 2, 1.0F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.borderArgb(), 80)));
        y += SECTION_GAP;

        // === Bio section ===
        String bio = this.profile.getBio();
        if (bio != null && !bio.isEmpty())
        {
            NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y,
                    11.0F, "ABOUT", theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            y += 16;

            NanoVG.nvgFontFaceId(vg, fontNormal);
            NanoVG.nvgFontSize(vg, 12.0F);
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
            NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            NanoVG.nvgTextBox(vg, r.x + PAD, y, r.w - PAD * 2, bio);
            // Estimate bio height (rough: 16px per line, ~50 chars per line)
            int bioLines = 1 + bio.length() / Math.max(1, (int)((r.w - PAD * 2) / 7));
            y += Math.max(16, bioLines * 16);
            y += SECTION_GAP;
        }

        // === Stats cards row ===
        y = renderStatsRow(vg, stack, theme, r, y, fontBold, fontNormal);
        y += SECTION_GAP;

        // === Achievements section ===
        List<String> achievements = this.profile.getAchievements();
        if (achievements != null && !achievements.isEmpty())
        {
            y = renderAchievements(vg, stack, theme, r, y, fontBold, fontNormal, achievements);
            y += SECTION_GAP;
        }

        // === Game stats section ===
        String gameStats = this.profile.getGameStats();
        if (gameStats != null && !gameStats.isEmpty())
        {
            NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y,
                    11.0F, "GAME STATS", theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            y += 16;
            NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD, y,
                    12.0F, gameStats, theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            y += 20 + SECTION_GAP;
        }

        // === Action buttons ===
        y = renderActionButtons(vg, stack, theme, r, y, fontBold, mx, my, isSelf);

        // Clamp scroll
        float contentH = y - (r.y - this.scrollOffset);
        float maxScroll = Math.max(0, contentH - r.h);
        this.scrollTarget = Math.min(this.scrollTarget, maxScroll);

        NanoUi.endClip(vg);
    }
    private float renderStatsRow(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                  float y, int fontBold, int fontNormal)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y,
                11.0F, "STATS", theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 16;

        float cardW = (r.w - PAD * 2 - 8 * 2) / 3.0F;
        float cx = r.x + PAD;

        // Visitors card
        renderStatCard(vg, stack, theme, cx, y, cardW, STAT_CARD_H, fontBold, fontNormal,
                String.valueOf(this.profile.getVisitorCount()), "Visitors");
        cx += cardW + 8;

        // Friends card
        int friendCount = this.profile.getFriends() != null ? this.profile.getFriends().size() : 0;
        renderStatCard(vg, stack, theme, cx, y, cardW, STAT_CARD_H, fontBold, fontNormal,
                String.valueOf(friendCount), "Friends");
        cx += cardW + 8;

        // Join date card
        String joinStr = "N/A";
        if (this.profile.getJoinDate() > 0)
        {
            joinStr = new SimpleDateFormat("yyyy-MM").format(new Date(this.profile.getJoinDate()));
        }
        renderStatCard(vg, stack, theme, cx, y, cardW, STAT_CARD_H, fontBold, fontNormal,
                joinStr, "Joined");

        return y + STAT_CARD_H;
    }

    private void renderStatCard(long vg, MemoryStack stack, NanoTheme theme,
                                float x, float y, float w, float h,
                                int fontBold, int fontNormal, String value, String label)
    {
        NanoRenderUtils.fillRoundedRect(vg, x, y, w, h, 6.0F,
                NanoRenderUtils.argb(stack, theme.cardArgb()));
        NanoRenderUtils.drawLabel(vg, stack, fontBold, x + w * 0.5F, y + h * 0.35F,
                14.0F, value, theme.textArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoRenderUtils.drawLabel(vg, stack, fontNormal, x + w * 0.5F, y + h * 0.72F,
                10.0F, label, theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private float renderAchievements(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                     float y, int fontBold, int fontNormal, List<String> achievements)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y,
                11.0F, "ACHIEVEMENTS", theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 16;

        float badgeSize = 28.0F;
        float gap = 6.0F;
        float cx = r.x + PAD;
        int themeArgb = parseColor(this.profile.getThemeColor());

        for (int i = 0; i < achievements.size() && i < 8; i++)
        {
            if (cx + badgeSize > r.x + r.w - PAD)
            {
                cx = r.x + PAD;
                y += badgeSize + gap;
            }
            // Badge circle
            int badgeColor = shiftHue(themeArgb, i * 40);
            NanoRenderUtils.fillRoundedRect(vg, cx, y, badgeSize, badgeSize, 6.0F,
                    NanoRenderUtils.argb(stack, badgeColor));
            // Badge icon (first char of achievement name)
            String icon = achievements.get(i).length() > 0
                    ? achievements.get(i).substring(0, 1).toUpperCase() : "?";
            NanoRenderUtils.drawLabel(vg, stack, fontBold,
                    cx + badgeSize * 0.5F, y + badgeSize * 0.5F,
                    12.0F, icon, 0xFFFFFFFF,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            cx += badgeSize + gap;
        }

        return y + badgeSize;
    }

    private float renderActionButtons(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                      float y, int fontBold, int mx, int my, boolean isSelf)
    {
        // Divider before buttons
        NanoRenderUtils.fillRect(vg, r.x + PAD, y, r.w - PAD * 2, 1.0F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.borderArgb(), 80)));
        y += SECTION_GAP;

        this.addFriendHover = 0;
        this.sendMailHover = 0;
        this.editProfileHover = 0;

        if (isSelf)
        {
            // Edit Profile button (full width)
            float btnW = r.w - PAD * 2;
            boolean hover = mx >= r.x + PAD && mx <= r.x + PAD + btnW && my >= y && my <= y + BTN_H;
            this.editProfileHover = hover ? 1 : 0;
            int bg = hover ? theme.accentArgb() : theme.accentSoftArgb();
            NanoRenderUtils.fillRoundedRect(vg, r.x + PAD, y, btnW, BTN_H, 6.0F,
                    NanoRenderUtils.argb(stack, bg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold,
                    r.x + PAD + btnW * 0.5F, y + BTN_H * 0.5F,
                    12.0F, "Edit Profile", theme.textArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            y += BTN_H + 8;
        }
        else
        {
            float btnW = (r.w - PAD * 2 - 8) * 0.5F;

            // Add Friend
            float fbX = r.x + PAD;
            boolean fbHover = mx >= fbX && mx <= fbX + btnW && my >= y && my <= y + BTN_H;
            this.addFriendHover = fbHover ? 1 : 0;
            int fbBg = fbHover ? 0xFF3BA55D : 0xFF43B581;
            NanoRenderUtils.fillRoundedRect(vg, fbX, y, btnW, BTN_H, 6.0F,
                    NanoRenderUtils.argb(stack, fbBg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold,
                    fbX + btnW * 0.5F, y + BTN_H * 0.5F,
                    12.0F, "Add Friend", 0xFFFFFFFF,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            // Send Mail
            float mbX = r.x + PAD + btnW + 8;
            boolean mbHover = mx >= mbX && mx <= mbX + btnW && my >= y && my <= y + BTN_H;
            this.sendMailHover = mbHover ? 1 : 0;
            int mbBg = mbHover ? theme.controlHoverArgb() : theme.controlArgb();
            NanoRenderUtils.fillRoundedRect(vg, mbX, y, btnW, BTN_H, 6.0F,
                    NanoRenderUtils.argb(stack, mbBg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold,
                    mbX + btnW * 0.5F, y + BTN_H * 0.5F,
                    12.0F, "Send Mail", theme.textArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            y += BTN_H + 8;
        }

        return y;
    }

    /** @return 1=add friend, 2=send mail, 3=edit profile, 0=nothing */
    public int handleClick()
    {
        if (!this.visible) return 0;
        if (this.addFriendHover == 1) return 1;
        if (this.sendMailHover == 1) return 2;
        if (this.editProfileHover == 1) return 3;
        return 0;
    }

    private String getInitial()
    {
        String name = this.profile.getDisplayName();
        if (name == null || name.isEmpty()) name = this.profile.getNick();
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    private static int parseColor(String hex)
    {
        try
        {
            if (hex == null || hex.isEmpty()) return 0xFF5B9BD5;
            if (hex.startsWith("#")) hex = hex.substring(1);
            return 0xFF000000 | Integer.parseInt(hex, 16);
        }
        catch (Exception e) { return 0xFF5B9BD5; }
    }

    /** Shift hue of an ARGB color by approximate degrees. */
    private static int shiftHue(int argb, int degrees)
    {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // Simple rotation: shift RGB channels
        float shift = (degrees % 360) / 360.0F;
        int nr = (int)(r * (1 - shift) + b * shift) & 0xFF;
        int ng = (int)(g * (1 - shift) + r * shift) & 0xFF;
        int nb = (int)(b * (1 - shift) + g * shift) & 0xFF;
        return 0xFF000000 | (nr << 16) | (ng << 8) | nb;
    }
}
