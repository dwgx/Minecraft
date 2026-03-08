package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint for the GuiAccountManagerScreen layout.
 * Allocation-free: reuses cached zone/edge lists and static handlers.
 */
public final class AccountBlueprint implements GuiBlueprint
{
    private static final float EDGE_HIT = 7.0F;
    private static final float HEADER_H = 36.0F;
    private static final float OUTER_PAD = 12.0F;
    private static final float ROW_H = 30.0F;

    private final List<Zone> zoneCache = new ArrayList<Zone>();
    private int zoneCacheSize = 0;
    private final List<DragEdge> edgeCache = new ArrayList<DragEdge>();
    private boolean edgesInit = false;

    // ── Static handlers ──

    private static final DragHandler H_WIN_W = new DragHandler() {
        public void apply(UiLayoutProfile p, float mx, UiRect w, float s) {
            p.setAccountWidth(UiMotion.clamp(mx - w.x, p.accountMinWidth(), 1000.0F) / s);
        }
    };

    private static final DragHandler H_WIN_H = new DragHandler() {
        public void apply(UiLayoutProfile p, float my, UiRect w, float s) {
            p.setAccountHeight(UiMotion.clamp(my - w.y, p.accountMinHeight(), 800.0F) / s);
        }
    };

    private static final ValueProvider V_WIN_W = new ValueProvider() {
        public String value(UiLayoutProfile p) { return "W:" + Math.round(p.accountWidth()); }
    };

    private static final ValueProvider V_WIN_H = new ValueProvider() {
        public String value(UiLayoutProfile p) { return "H:" + Math.round(p.accountHeight()); }
    };

    public String displayName() { return "Account Manager"; }
    public String key() { return "account"; }

    public List<Zone> computeZones(UiRect win, UiLayoutProfile lp, float k)
    {
        float headerH = HEADER_H * k;
        float pad = OUTER_PAD * k;

        float bodyX = win.x + pad;
        float bodyY = win.y + headerH + 10 * k;
        float bodyW = win.w - pad * 2;
        float bodyH = win.h - headerH - pad * 2;

        float controlH = 22 * k;
        float gap = 6 * k;
        float infoMin = 58 * k;
        float listMin = 86 * k;
        float listMax = Math.max(listMin, bodyH - (controlH * 3 + gap * 4 + infoMin));
        float listH = UiMotion.clamp(bodyH * 0.46F, listMin, listMax);

        float controlsY = bodyY + listH + gap;
        float offlineW = UiMotion.clamp(bodyW * 0.44F, 140 * k, bodyW - 240 * k);
        float halfRemain = (bodyW - offlineW - gap * 2) * 0.5F;

        float row2Y = controlsY + controlH + gap;
        float third = (bodyW - gap * 2) / 3;
        float row3Y = row2Y + controlH + gap;
        float infoY = row3Y + controlH + gap;
        float infoH = Math.max(50 * k, bodyY + bodyH - infoY);

        int idx = 0;
        idx = setZone(idx, "Header", win.x, win.y, win.w, headerH, 0, 0xAA0C151F);
        idx = setZone(idx, "Body", bodyX, bodyY, bodyW, bodyH, 0, 0x660C151F);
        idx = setZone(idx, "Accounts", bodyX, bodyY, bodyW, listH, 1, 0xBB122130);
        idx = setZone(idx, "Input", bodyX, controlsY, offlineW, controlH, 2, 0x881A2736);
        idx = setZone(idx, "Add", bodyX + offlineW + gap, controlsY, halfRemain, controlH, 2, 0x881A2736);
        idx = setZone(idx, "MS Login", bodyX + offlineW + gap + halfRemain + gap, controlsY, halfRemain, controlH, 2, 0x881A2736);
        idx = setZone(idx, "Login", bodyX, row2Y, third, controlH, 2, 0x881A2736);
        idx = setZone(idx, "Delete", bodyX + third + gap, row2Y, third, controlH, 2, 0x881A2736);
        idx = setZone(idx, "Clear", bodyX + third * 2 + gap * 2, row2Y, third, controlH, 2, 0x881A2736);
        idx = setZone(idx, "Info", bodyX, infoY, bodyW, infoH, 1, 0xBB122130);

        // Account list sample rows
        float rowH = ROW_H * k;
        float lInX = bodyX + 8 * k;
        float lInY = bodyY + 22 * k;
        float lInW = bodyW - 16 * k - 28 * k;
        float lInH = listH - 30 * k;
        float y = lInY;
        for (int i = 0; i < 5 && y + rowH <= lInY + lInH; i++)
        {
            idx = setZone(idx, "Acc" + (i + 1), lInX, y, lInW, rowH - 2, 2, 0x881A2736);
            y += rowH;
        }

        this.zoneCacheSize = idx;
        return this.zoneCache.subList(0, idx);
    }

    public List<DragEdge> computeEdges(UiRect win, UiLayoutProfile lp, float k)
    {
        float hw = EDGE_HIT * k;

        if (!this.edgesInit)
        {
            this.edgeCache.clear();
            for (int i = 0; i < 2; i++) this.edgeCache.add(new DragEdge());
            this.edgesInit = true;
        }

        this.edgeCache.get(0).set("win_w", "Window Width",
            win.x + win.w - hw * 0.5F, win.y, hw, win.h,
            true, H_WIN_W, V_WIN_W, 1.0F);

        this.edgeCache.get(1).set("win_h", "Window Height",
            win.x, win.y + win.h - hw * 0.5F, win.w, hw,
            false, H_WIN_H, V_WIN_H, 1.0F);

        return this.edgeCache;
    }

    private int setZone(int idx, String label, float x, float y, float w, float h,
                        int depth, int fillArgb)
    {
        while (this.zoneCache.size() <= idx) this.zoneCache.add(new Zone());
        this.zoneCache.get(idx).set(label, x, y, w, h, depth, fillArgb);
        return idx + 1;
    }
}
