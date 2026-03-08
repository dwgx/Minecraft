package client.ui.irc.component;

import client.chat.model.ChatGroup;
import client.ui.layout.UiRect;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import client.ui.template.NanoScreenKit;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.List;

/**
 * Left sidebar: navigation icons (Chat/Friends/Mail/Feed) + separator +
 * DM icon + group icons (Discord-style, only in Chat mode).
 * Animated hover/select transitions via UiAnimationBus.
 */
public final class IRCServerList
{
    public static final float ICON_SIZE = 36.0F;
    public static final float SMALL_ICON = 28.0F;
    public static final float GAP = 8.0F;
    private static final String ANIM = "chat.srv.";

    /** Navigation mode constants. */
    public static final int NAV_CHAT = 0;
    public static final int NAV_FRIENDS = 1;
    public static final int NAV_MAIL = 2;
    public static final int NAV_FEED = 3;

    private static final String[] NAV_LABELS = {"Chat", "Frnd", "Mail", "Feed"};

    private UiAnimProfile animProfile;

    public void setAnimProfile(UiAnimProfile profile) { this.animProfile = profile; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       List<ChatGroup> groups, int selectedIndex, int navMode,
                       int unreadMail, int pendingFriends, int mx, int my)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.sidebarArgb()));

        float cx = r.x + r.w * 0.5F;
        float y = r.y + GAP;
        int fontBold = NanoFontBook.uiBold();

        // --- Navigation icons (Chat / Friends / Mail / Feed) ---
        for (int n = 0; n < 4; n++)
        {
            boolean hover = hitSmallIcon(cx, y, mx, my);
            boolean selected = navMode == n;
            float hoverT = UiAnimationBus.animateControl(ANIM + "nav" + n + ".h", hover ? 1.0F : 0.0F, this.animProfile);
            float selectT = UiAnimationBus.animateControl(ANIM + "nav" + n + ".s", selected ? 1.0F : 0.0F, this.animProfile);
            int bg = NanoScreenKit.mixArgb(
                    NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), hoverT),
                    theme.accentArgb(), selectT);
            float rad = SMALL_ICON * (0.5F - 0.15F * selectT);
            NanoRenderUtils.fillRoundedRect(vg, cx - SMALL_ICON * 0.5F, y, SMALL_ICON, SMALL_ICON,
                    rad, NanoRenderUtils.argb(stack, bg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold, cx, y + SMALL_ICON * 0.5F, 9.0F,
                    NAV_LABELS[n], theme.textArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            // Badge for unread mail or pending friends
            int badge = 0;
            if (n == NAV_FRIENDS) badge = pendingFriends;
            else if (n == NAV_MAIL) badge = unreadMail;
            if (badge > 0)
            {
                String badgeText = badge > 99 ? "99+" : String.valueOf(badge);
                float bx = cx + SMALL_ICON * 0.5F - 6.0F;
                float by = y - 2.0F;
                NanoRenderUtils.fillRoundedRect(vg, bx, by, 14.0F, 12.0F, 6.0F,
                        NanoRenderUtils.argb(stack, 0xFFED4245));
                NanoRenderUtils.drawLabel(vg, stack, fontBold, bx + 7.0F, by + 6.0F, 8.0F,
                        badgeText, 0xFFFFFFFF,
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }

            // Selection indicator (left pill)
            if (selectT > 0.01F)
            {
                float pillH = SMALL_ICON * 0.5F * selectT;
                NanoRenderUtils.fillRoundedRect(vg, r.x, y + (SMALL_ICON - pillH) * 0.5F,
                        3.0F, pillH, 1.5F,
                        NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), selectT)));
            }
            y += SMALL_ICON + GAP * 0.5F;
        }
        // PLACEHOLDER_SERVERLIST_CONTINUE

        // Separator
        NanoRenderUtils.fillRect(vg, cx - 12.0F, y, 24.0F, 2.0F,
                NanoRenderUtils.argb(stack, theme.windowBorderArgb()));
        y += GAP + 2.0F;

        // --- Chat mode: DM + group icons ---
        if (navMode == NAV_CHAT)
        {
            // DM icon
            boolean dmHover = hitIcon(cx, y, mx, my);
            float dmHoverT = UiAnimationBus.animateControl(ANIM + "dm.h", dmHover ? 1.0F : 0.0F, this.animProfile);
            float dmSelectT = UiAnimationBus.animateControl(ANIM + "dm.s", selectedIndex == 0 ? 1.0F : 0.0F, this.animProfile);
            int dmBg = NanoScreenKit.mixArgb(
                    NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), dmHoverT),
                    theme.accentArgb(), dmSelectT);
            float dmRad = ICON_SIZE * (0.5F - 0.2F * dmSelectT);
            NanoRenderUtils.fillRoundedRect(vg, cx - ICON_SIZE * 0.5F, y, ICON_SIZE, ICON_SIZE,
                    dmRad, NanoRenderUtils.argb(stack, dmBg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold, cx, y + ICON_SIZE * 0.5F, 13.0F,
                    "DM", theme.textArgb(), NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            if (dmSelectT > 0.01F)
            {
                float pillH = ICON_SIZE * 0.5F * dmSelectT;
                NanoRenderUtils.fillRoundedRect(vg, r.x, y + (ICON_SIZE - pillH) * 0.5F,
                        3.0F, pillH, 1.5F,
                        NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), dmSelectT)));
            }
            y += ICON_SIZE + GAP;

            // Group separator
            NanoRenderUtils.fillRect(vg, cx - 12.0F, y, 24.0F, 2.0F,
                    NanoRenderUtils.argb(stack, theme.windowBorderArgb()));
            y += GAP + 2.0F;

            // Group icons
            for (int i = 0; i < groups.size(); i++)
            {
                ChatGroup g = groups.get(i);
                boolean hover = hitIcon(cx, y, mx, my);
                boolean selected = selectedIndex == i + 1;
                float hoverT = UiAnimationBus.animateControl(ANIM + i + ".h", hover ? 1.0F : 0.0F, this.animProfile);
                float selectT = UiAnimationBus.animateControl(ANIM + i + ".s", selected ? 1.0F : 0.0F, this.animProfile);
                int bg2 = NanoScreenKit.mixArgb(
                        NanoScreenKit.mixArgb(theme.controlArgb(), theme.controlHoverArgb(), hoverT),
                        theme.accentArgb(), selectT);
                float rad2 = ICON_SIZE * (0.5F - 0.2F * selectT);
                NanoRenderUtils.fillRoundedRect(vg, cx - ICON_SIZE * 0.5F, y, ICON_SIZE, ICON_SIZE,
                        rad2, NanoRenderUtils.argb(stack, bg2));
                NanoRenderUtils.drawLabel(vg, stack, fontBold, cx, y + ICON_SIZE * 0.5F, 13.0F,
                        g.getInitial(), theme.textArgb(),
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                if (selectT > 0.01F)
                {
                    float pillH = ICON_SIZE * 0.5F * selectT;
                    NanoRenderUtils.fillRoundedRect(vg, r.x, y + (ICON_SIZE - pillH) * 0.5F,
                            3.0F, pillH, 1.5F,
                            NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(theme.accentArgb(), selectT)));
                }
                y += ICON_SIZE + GAP;
            }
        }
    }
    // PLACEHOLDER_SERVERLIST_METHODS

    /**
     * Hit-test navigation icons. Returns NAV_CHAT..NAV_FEED or -1.
     */
    public int hitTestNav(UiRect r, int mx, int my)
    {
        if (r == null || !r.contains(mx, my)) return -1;
        float cx = r.x + r.w * 0.5F;
        float y = r.y + GAP;
        for (int n = 0; n < 4; n++)
        {
            if (hitSmallIcon(cx, y, mx, my)) return n;
            y += SMALL_ICON + GAP * 0.5F;
        }
        return -1;
    }

    /**
     * Hit-test server/group icons (only valid in Chat mode).
     * Returns 0=DM, 1+=group, -1=none.
     */
    public int hitTestServer(UiRect r, List<ChatGroup> groups, int mx, int my)
    {
        if (r == null || !r.contains(mx, my)) return -1;
        float cx = r.x + r.w * 0.5F;
        // Skip past nav icons + separator
        float y = r.y + GAP + (SMALL_ICON + GAP * 0.5F) * 4 + GAP + 2.0F;

        if (hitIcon(cx, y, mx, my)) return 0;
        y += ICON_SIZE + GAP + GAP + 2.0F;

        for (int i = 0; i < groups.size(); i++)
        {
            if (hitIcon(cx, y, mx, my)) return i + 1;
            y += ICON_SIZE + GAP;
        }
        return -1;
    }

    private static boolean hitIcon(float cx, float y, int mx, int my)
    {
        return mx >= cx - ICON_SIZE * 0.5F && mx <= cx + ICON_SIZE * 0.5F
                && my >= y && my <= y + ICON_SIZE;
    }

    private static boolean hitSmallIcon(float cx, float y, int mx, int my)
    {
        return mx >= cx - SMALL_ICON * 0.5F && mx <= cx + SMALL_ICON * 0.5F
                && my >= y && my <= y + SMALL_ICON;
    }
}
