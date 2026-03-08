package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint for the ChatOverlay layout in GuiEdit.
 * Shows the Discord/QQ-style chat panel structure with draggable edges
 * for server list width, channel ratio, top bar height, and input bar height.
 */
public final class ChatOverlayBlueprint implements GuiBlueprint
{
    private static final float PAD = 8.0F;
    private static final float GAP = 4.0F;

    private final List<Zone> zoneCache = new ArrayList<Zone>();
    private final List<DragEdge> edgeCache = new ArrayList<DragEdge>();

    public String displayName() { return "Chat Overlay"; }
    public String key() { return "chatoverlay"; }

    public List<Zone> computeZones(UiRect win, UiLayoutProfile lp, float k)
    {
        float topH = lp.chatTopBarHeight() * k;
        float statusH = lp.chatStatusBarHeight() * k;
        float inputH = lp.chatInputBarHeight() * k;
        float serverW = lp.chatServerListWidth() * k;
        float channelRatio = lp.chatChannelRatio();
        float pad = PAD * k;
        float gap = GAP * k;

        float ix = win.x + pad;
        float iy = win.y + pad;
        float iw = win.w - pad * 2.0F;
        float ih = win.h - pad * 2.0F;

        float bodyY = iy + topH + gap;
        float bodyH = ih - topH - statusH - gap * 2.0F;
        float afterServer = ix + serverW + gap;
        float remainW = iw - serverW - gap;
        float channelW = Math.max(120.0F * k, remainW * channelRatio);
        float msgX = afterServer + channelW + gap;
        float msgW = remainW - channelW - gap;
        float msgH = bodyH - inputH - gap;

        int idx = 0;
        idx = setZone(idx, "TopBar", ix, iy, iw, topH, 0, 0xAA0C151F);
        idx = setZone(idx, "ServerList", ix, bodyY, serverW, bodyH, 1, 0xBB0C151F);
        idx = setZone(idx, "ChannelList", afterServer, bodyY, channelW, bodyH, 1, 0xBB122130);
        idx = setZone(idx, "Messages", msgX, bodyY, msgW, msgH, 1, 0xBB122130);
        idx = setZone(idx, "InputBar", msgX, bodyY + msgH + gap, msgW, inputH, 1, 0x881A2736);
        idx = setZone(idx, "StatusBar", ix, iy + ih - statusH, iw, statusH, 0, 0xAA0C151F);

        // Sample server icons
        float iconSize = 36.0F * k;
        float iconGap = 8.0F * k;
        float cx = ix + serverW * 0.5F;
        float sy = bodyY + iconGap;
        idx = setZone(idx, "DM", cx - iconSize * 0.5F, sy, iconSize, iconSize, 2, 0x881A2736);
        sy += iconSize + iconGap + iconGap + 2.0F * k;
        idx = setZone(idx, "Grp1", cx - iconSize * 0.5F, sy, iconSize, iconSize, 2, 0x881A2736);
        sy += iconSize + iconGap;
        idx = setZone(idx, "Grp2", cx - iconSize * 0.5F, sy, iconSize, iconSize, 2, 0x881A2736);

        // Sample channel rows
        float rowH = 36.0F * k;
        float cy = bodyY + 28.0F * k;
        for (int i = 0; i < 4 && cy + rowH <= bodyY + bodyH; i++)
        {
            idx = setZone(idx, "Ch" + (i + 1), afterServer + 4.0F * k, cy,
                    channelW - 8.0F * k, rowH - 2.0F * k, 2, 0x881A2736);
            cy += rowH;
        }

        // Sample message bubbles
        float bubbleH = 32.0F * k;
        float bubbleGap = 12.0F * k;
        float my = bodyY + 12.0F * k;
        float bubbleMaxW = msgW * 0.55F;
        idx = setZone(idx, "Msg1", msgX + 12.0F * k, my, bubbleMaxW, bubbleH, 2, 0x881A2736);
        my += bubbleH + bubbleGap;
        idx = setZone(idx, "Msg2", msgX + msgW - 12.0F * k - bubbleMaxW, my,
                bubbleMaxW, bubbleH, 2, 0x88223142);
        my += bubbleH + bubbleGap;
        idx = setZone(idx, "Msg3", msgX + 12.0F * k, my, bubbleMaxW * 0.7F, bubbleH, 2, 0x881A2736);

        return this.zoneCache.subList(0, idx);
    }
/* APPEND_EDGES */
    public List<DragEdge> computeEdges(UiRect win, UiLayoutProfile lp, float k)
    {
        float topH = lp.chatTopBarHeight() * k;
        float statusH = lp.chatStatusBarHeight() * k;
        float inputH = lp.chatInputBarHeight() * k;
        float serverW = lp.chatServerListWidth() * k;
        float channelRatio = lp.chatChannelRatio();
        float pad = PAD * k;
        float gap = GAP * k;

        float ix = win.x + pad;
        float iy = win.y + pad;
        float iw = win.w - pad * 2.0F;
        float ih = win.h - pad * 2.0F;

        float bodyY = iy + topH + gap;
        float bodyH = ih - topH - statusH - gap * 2.0F;
        float afterServer = ix + serverW + gap;
        float remainW = iw - serverW - gap;
        float channelW = Math.max(120.0F * k, remainW * channelRatio);
        float msgX = afterServer + channelW + gap;
        float msgW = remainW - channelW - gap;
        float msgH = bodyH - inputH - gap;

        float hitW = 12.0F;
        int ei = 0;

        // Server list right edge
        ei = setEdge(ei, "chat_server_w", "Server List Width",
                ix + serverW - hitW * 0.5F, bodyY, hitW, bodyH,
                true, new DragHandler()
                {
                    public void apply(UiLayoutProfile p, float mousePos, UiRect w, float s)
                    {
                        float localX = (mousePos - w.x - PAD * s) / s;
                        p.setChatServerListWidth(UiMotion.clamp(localX, 36.0F, 120.0F));
                    }
                },
                new ValueProvider()
                {
                    public String value(UiLayoutProfile p)
                    {
                        return Math.round(p.chatServerListWidth()) + "px";
                    }
                }, 0.0F);

        // Channel list right edge
        ei = setEdge(ei, "chat_channel_r", "Channel Ratio",
                afterServer + channelW - hitW * 0.5F, bodyY, hitW, bodyH,
                true, new DragHandler()
                {
                    public void apply(UiLayoutProfile p, float mousePos, UiRect w, float s)
                    {
                        float localX = mousePos - w.x - PAD * s - p.chatServerListWidth() * s - GAP * s;
                        float remain = (w.w - PAD * 2.0F * s) - p.chatServerListWidth() * s - GAP * s;
                        if (remain > 0)
                        {
                            p.setChatChannelRatio(UiMotion.clamp(localX / remain, 0.10F, 0.45F));
                        }
                    }
                },
                new ValueProvider()
                {
                    public String value(UiLayoutProfile p)
                    {
                        return Math.round(p.chatChannelRatio() * 100) + "%";
                    }
                }, 0.0F);

        // Input bar top edge
        ei = setEdge(ei, "chat_input_h", "Input Bar Height",
                msgX, bodyY + msgH - hitW * 0.5F, msgW, hitW,
                false, new DragHandler()
                {
                    public void apply(UiLayoutProfile p, float mousePos, UiRect w, float s)
                    {
                        float bottomEdge = w.y + (w.h - PAD * s) - p.chatStatusBarHeight() * s - GAP * s;
                        float localH = (bottomEdge - mousePos) / s;
                        p.setChatInputBarHeight(UiMotion.clamp(localH, 32.0F, 100.0F));
                    }
                },
                new ValueProvider()
                {
                    public String value(UiLayoutProfile p)
                    {
                        return Math.round(p.chatInputBarHeight()) + "px";
                    }
                }, 0.0F);

        return this.edgeCache.subList(0, ei);
    }

    private int setZone(int idx, String label, float x, float y, float w, float h,
                        int depth, int fillArgb)
    {
        while (this.zoneCache.size() <= idx) this.zoneCache.add(new Zone());
        this.zoneCache.get(idx).set(label, x, y, w, h, depth, fillArgb);
        return idx + 1;
    }

    private int setEdge(int idx, String id, String tooltip,
                        float hx, float hy, float hw, float hh,
                        boolean vertical, DragHandler handler,
                        ValueProvider valueProvider, float snapStep)
    {
        while (this.edgeCache.size() <= idx) this.edgeCache.add(new DragEdge());
        this.edgeCache.get(idx).set(id, tooltip, hx, hy, hw, hh,
                vertical, handler, valueProvider, snapStep);
        return idx + 1;
    }
}
