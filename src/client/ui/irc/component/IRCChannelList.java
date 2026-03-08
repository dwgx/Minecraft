package client.ui.irc.component;

import client.chat.model.*;
import client.chat.store.ChatStore;
import client.ui.layout.UiRect;
import client.ui.template.NanoScreenKit;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import dwgx.nano.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.List;

/**
 * Channel / conversation list panel.
 * Shows DM list when serverIndex==0, group channels otherwise.
 * Animated hover/select transitions via UiAnimationBus.
 */
public final class IRCChannelList
{
    private static final float ROW_H = 36.0F;
    private static final String ANIM = "chat.ch.";

    private UiAnimProfile animProfile;

    public void setAnimProfile(UiAnimProfile profile) { this.animProfile = profile; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       ChatStore store, int serverIndex, String activeConvId, int mx, int my)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.mainArgb()));

        NanoUi.beginClip(vg, r.x, r.y, r.w, r.h);
        float y = r.y + 4.0F;
        int fontRegular = NanoFontBook.uiRegular();
        int fontBold = NanoFontBook.uiBold();

        if (serverIndex == 0)
        {
            renderDmList(vg, stack, theme, r, store, activeConvId, fontRegular, fontBold, y, mx, my);
        }
        else
        {
            renderGroupChannels(vg, stack, theme, r, store, serverIndex, activeConvId,
                    fontRegular, fontBold, y, mx, my);
        }
        NanoUi.endClip(vg);
    }

    private void renderDmList(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                              ChatStore store, String activeConvId,
                              int fontRegular, int fontBold, float y, int mx, int my)
    {
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + 10.0F, y + 10.0F, 11.0F,
                "DIRECT MESSAGES", theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 24.0F;

        List<ChatConversation> convs = store.getConversations();
        for (int i = 0; i < convs.size(); i++)
        {
            ChatConversation conv = convs.get(i);
            if (!conv.isDm()) continue;

            boolean hover = mx >= r.x && mx <= r.x + r.w && my >= y && my <= y + ROW_H;
            boolean selected = conv.getId().equals(activeConvId);
            int rowBg = selected ? theme.rowSelectedArgb() : (hover ? theme.rowHoverArgb() : 0);
            if (rowBg != 0)
            {
                NanoRenderUtils.fillRoundedRect(vg, r.x + 4.0F, y, r.w - 8.0F, ROW_H,
                        theme.cardRadius(), NanoRenderUtils.argb(stack, rowBg));
            }

            String otherName = "Unknown";
            UserStatus otherStatus = UserStatus.OFFLINE;
            for (String pid : conv.getParticipantIds())
            {
                if (!"local".equals(pid))
                {
                    ChatUser u = store.getUser(pid);
                    if (u != null)
                    {
                        otherName = u.getNickname();
                        otherStatus = u.getStatus();
                    }
                }
            }

            // Status dot
            int dotColor = IRCRenderUtils.statusColor(otherStatus);
            float dotX = r.x + 16.0F;
            float dotY = y + ROW_H * 0.5F;
            NanoRenderUtils.fillRoundedRect(vg, dotX - 4.0F, dotY - 4.0F, 8.0F, 8.0F, 4.0F,
                    NanoRenderUtils.argb(stack, dotColor));

            NanoRenderUtils.drawLabel(vg, stack, fontRegular, r.x + 28.0F, y + ROW_H * 0.38F,
                    13.0F, otherName, theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            String preview = conv.getLastMessagePreview();
            if (preview != null)
            {
                String ellipsized = NanoScreenKit.ellipsize(vg, fontRegular, 11.0F, preview, r.w - 40.0F);
                NanoRenderUtils.drawLabel(vg, stack, fontRegular, r.x + 28.0F, y + ROW_H * 0.68F,
                        11.0F, ellipsized, theme.textMutedArgb(),
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            }

            // Unread badge
            if (conv.getUnreadCount() > 0)
            {
                String badge = String.valueOf(conv.getUnreadCount());
                float bw = Math.max(16.0F, NanoRenderUtils.textWidth(vg, fontBold, 10.0F, badge) + 8.0F);
                float bx = r.x + r.w - bw - 10.0F;
                float by = y + (ROW_H - 16.0F) * 0.5F;
                NanoRenderUtils.fillRoundedRect(vg, bx, by, bw, 16.0F, 8.0F,
                        NanoRenderUtils.argb(stack, theme.dangerArgb()));
                NanoRenderUtils.drawLabel(vg, stack, fontBold, bx + bw * 0.5F, by + 8.0F, 10.0F,
                        badge, 0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }
            y += ROW_H;
        }
    }

    private void renderGroupChannels(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                     ChatStore store, int serverIndex, String activeConvId,
                                     int fontRegular, int fontBold, float y, int mx, int my)
    {
        int groupIdx = serverIndex - 1;
        List<ChatGroup> groups = store.getGroups();
        if (groupIdx < 0 || groupIdx >= groups.size()) return;

        ChatGroup group = groups.get(groupIdx);
        NanoRenderUtils.drawLabel(vg, stack, fontBold, r.x + 10.0F, y + 10.0F, 11.0F,
                group.getName().toUpperCase(), theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        y += 24.0F;


        for (ChatChannel ch : group.getChannels())
        {
            String prefix = ch.isText() ? "# " : "~ ";
            String convId = "conv-" + ch.getId();
            boolean hover = mx >= r.x && mx <= r.x + r.w && my >= y && my <= y + ROW_H;
            boolean selected = convId.equals(activeConvId);
            float hoverT = UiAnimationBus.animateControl(ANIM + "g." + ch.getId() + ".h", hover ? 1.0F : 0.0F, this.animProfile);
            float selectT = UiAnimationBus.animateControl(ANIM + "g." + ch.getId() + ".s", selected ? 1.0F : 0.0F, this.animProfile);
            float t = Math.max(hoverT * 0.5F, selectT);
            if (t > 0.01F)
            {
                int rowBg = NanoScreenKit.mixArgb(theme.rowHoverArgb(), theme.rowSelectedArgb(), selectT);
                NanoRenderUtils.fillRoundedRect(vg, r.x + 4.0F, y, r.w - 8.0F, ROW_H,
                        theme.cardRadius(), NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(rowBg, t)));
            }
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, r.x + 14.0F, y + ROW_H * 0.5F,
                    13.0F, prefix + ch.getName(), theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            y += ROW_H;
        }
    }

    /**
     * Returns conversation ID for clicked row, or null.
     */
    public String hitTest(UiRect r, ChatStore store, int serverIndex, int mx, int my)
    {
        if (r == null || !r.contains(mx, my)) return null;
        float y = r.y + 4.0F + 24.0F;

        if (serverIndex == 0)
        {
            for (ChatConversation conv : store.getConversations())
            {
                if (!conv.isDm()) continue;
                if (my >= y && my <= y + ROW_H) return conv.getId();
                y += ROW_H;
            }
        }
        else
        {
            int groupIdx = serverIndex - 1;
            List<ChatGroup> groups = store.getGroups();
            if (groupIdx >= 0 && groupIdx < groups.size())
            {
                for (ChatChannel ch : groups.get(groupIdx).getChannels())
                {
                    if (my >= y && my <= y + ROW_H) return "conv-" + ch.getId();
                    y += ROW_H;
                }
            }
        }
        return null;
    }
}