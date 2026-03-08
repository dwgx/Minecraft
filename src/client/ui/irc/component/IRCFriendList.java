package client.ui.irc.component;

import client.chat.model.FriendEntry;
import client.chat.model.FriendStatus;
import client.ui.layout.UiRect;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import client.ui.template.NanoScreenKit;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Friend list panel with online/offline grouping, pending request badge,
 * UID display, and add-friend-by-UID input.
 */
public final class IRCFriendList
{
    private static final String ANIM = "irc.friend.";
    private static final float ROW_H = 32.0F;
    private static final float PAD = 6.0F;
    private static final float UID_BAR_H = 56.0F;

    private UiAnimProfile animProfile;
    private float scrollOffset;
    private int hoveredIndex = -1;
    private boolean addBtnHover;

    // UID input state
    private String uidInput = "";
    private boolean uidFieldFocused;
    private String myUid = "";

    public void setAnimProfile(UiAnimProfile profile) { this.animProfile = profile; }
    public void setMyUid(String uid) { this.myUid = uid != null ? uid : ""; }
    public String getUidInput() { return this.uidInput; }
    public boolean isAddBtnHovered() { return this.addBtnHover; }
    public boolean isUidFieldFocused() { return this.uidFieldFocused; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       List<FriendEntry> friends, int pendingCount, int mx, int my)
    {
        if (r == null || friends == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.surfaceArgb()));

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        this.hoveredIndex = -1;
        this.addBtnHover = false;

        // === Top: My UID display + Add by UID input ===
        float uidY = r.y + PAD;
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, uidY,
                10.0F, "MY UID", theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        uidY += 13;
        NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD, uidY,
                12.0F, this.myUid.isEmpty() ? "Loading..." : this.myUid, theme.accentArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        uidY += 18;

        // UID input field + Add button
        float fieldW = r.w - PAD * 2 - 44;
        float fieldH = 22.0F;
        int fieldBg = this.uidFieldFocused ? theme.controlHoverArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, r.x + PAD, uidY, fieldW, fieldH, 4.0F,
                NanoRenderUtils.argb(stack, fieldBg));
        if (this.uidFieldFocused)
        {
            NanoRenderUtils.strokeRoundedRect(vg, r.x + PAD, uidY, fieldW, fieldH, 4.0F, 1.0F,
                    NanoRenderUtils.argb(stack, theme.accentArgb()));
        }
        String displayText = this.uidInput.isEmpty() ? "Enter UID to add..." : this.uidInput;
        int textColor = this.uidInput.isEmpty() ? theme.textDimArgb() : theme.textArgb();
        NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD + 6, uidY + fieldH * 0.5F,
                11.0F, displayText, textColor,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        float btnX = r.x + PAD + fieldW + 4;
        float btnW = 38.0F;
        boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= uidY && my <= uidY + fieldH;
        this.addBtnHover = btnHover;
        int btnBg = btnHover ? 0xFF3BA55D : 0xFF43B581;
        NanoRenderUtils.fillRoundedRect(vg, btnX, uidY, btnW, fieldH, 4.0F,
                NanoRenderUtils.argb(stack, btnBg));
        NanoRenderUtils.drawLabel(vg, stack, fontBold, btnX + btnW * 0.5F, uidY + fieldH * 0.5F,
                11.0F, "Add", 0xFFFFFFFF,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        uidY += fieldH + 8;
        NanoRenderUtils.fillRect(vg, r.x + PAD, uidY, r.w - PAD * 2, 1.0F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.borderArgb(), 60)));
        uidY += 6;

        float y = uidY - this.scrollOffset;

        // Pending requests
        List<FriendEntry> pending = new ArrayList<FriendEntry>();
        for (FriendEntry f : friends)
        {
            if (f.getFriendStatus() == FriendStatus.PENDING_IN) pending.add(f);
        }
        if (!pending.isEmpty())
        {
            NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y + 10.0F,
                    11.0F, "Pending - " + pending.size(), theme.accentArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            y += 20.0F;
            for (int i = 0; i < pending.size(); i++)
            {
                FriendEntry f = pending.get(i);
                boolean hover = mx >= r.x && mx <= r.x + r.w && my >= y && my < y + ROW_H;
                if (hover) this.hoveredIndex = -(i + 1);
                if (hover) NanoRenderUtils.fillRoundedRect(vg, r.x + 2, y, r.w - 4, ROW_H - 2, 4.0F,
                        NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.accentArgb(), 40)));
                NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD + 16, y + ROW_H * 0.5F,
                        13.0F, f.getNick(), theme.textArgb(),
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
                NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + r.w - PAD, y + ROW_H * 0.5F,
                        10.0F, "pending", theme.accentArgb(),
                        NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
                y += ROW_H;
            }
        }

        // Split accepted into online/offline
        List<FriendEntry> online = new ArrayList<FriendEntry>();
        List<FriendEntry> offline = new ArrayList<FriendEntry>();
        for (FriendEntry f : friends)
        {
            if (f.getFriendStatus() == FriendStatus.ACCEPTED)
            {
                if (f.isOnline()) online.add(f); else offline.add(f);
            }
        }
        y = renderSection(vg, stack, theme, r, "Online - " + online.size(), online, y, mx, my, 0);
        renderSection(vg, stack, theme, r, "Offline - " + offline.size(), offline, y, mx, my, online.size());
    }

    public void handleClick(int mx, int my, UiRect r)
    {
        if (r == null) return;
        float uidFieldY = r.y + PAD + 13 + 18;
        float fieldH = 22.0F;
        float fieldW = r.w - PAD * 2 - 44;
        this.uidFieldFocused = mx >= r.x + PAD && mx <= r.x + PAD + fieldW
                && my >= uidFieldY && my <= uidFieldY + fieldH;
    }

    public void typeChar(char c)
    {
        if (this.uidFieldFocused && this.uidInput.length() < 16)
        {
            this.uidInput += Character.toUpperCase(c);
        }
    }

    public void backspace()
    {
        if (this.uidFieldFocused && !this.uidInput.isEmpty())
        {
            this.uidInput = this.uidInput.substring(0, this.uidInput.length() - 1);
        }
    }

    public void clearUidInput() { this.uidInput = ""; }

    private float renderSection(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                String header, List<FriendEntry> entries, float y, int mx, int my, int indexOffset)
    {
        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();

        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + PAD, y + 10.0F,
                11.0F, header, theme.textDimArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        y += 20.0F;

        for (int i = 0; i < entries.size(); i++)
        {
            FriendEntry f = entries.get(i);
            int idx = indexOffset + i;
            boolean hover = mx >= r.x && mx <= r.x + r.w && my >= y && my < y + ROW_H;
            if (hover) this.hoveredIndex = idx;

            float hoverT = UiAnimationBus.animateControl(ANIM + idx + ".h", hover ? 1.0F : 0.0F, this.animProfile);
            int bg = NanoScreenKit.mixArgb(theme.surfaceArgb(), theme.controlHoverArgb(), hoverT);
            NanoRenderUtils.fillRoundedRect(vg, r.x + 2, y, r.w - 4, ROW_H - 2, 4.0F,
                    NanoRenderUtils.argb(stack, bg));

            int dotColor = f.isOnline() ? 0xFF43B581 : 0xFF747F8D;
            NanoRenderUtils.fillCircle(vg, r.x + PAD + 6, y + ROW_H * 0.5F, 4.0F,
                    NanoRenderUtils.argb(stack, dotColor));

            String display = f.getDisplayName() != null ? f.getDisplayName() : f.getNick();
            NanoRenderUtils.drawLabel(vg, stack, fontNormal, r.x + PAD + 16, y + ROW_H * 0.5F,
                    13.0F, display, theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            y += ROW_H;
        }
        return y;
    }

    public int getHoveredIndex() { return this.hoveredIndex; }

    public void scroll(float delta)
    {
        this.scrollOffset = Math.max(0, this.scrollOffset - delta * 0.4F);
    }

    public void resetHover() { this.hoveredIndex = -1; }
}