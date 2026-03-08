package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint for the ClientSettingsScreen layout.
 * Allocation-free: reuses cached zone/edge lists and static handlers.
 */
public final class SettingsBlueprint implements GuiBlueprint
{
    private static final float EDGE_HIT = 7.0F;
    private static final float HEADER_H = 36.0F;
    private static final float OUTER_PAD = 12.0F;
    private static final float INFO_CARD_H = 62.0F;
    private static final float DEBUG_CARD_H = 34.0F;
    private static final float ROW_SETTING = 26.0F;

    private final List<Zone> zoneCache = new ArrayList<Zone>();
    private int zoneCacheSize = 0;
    private final List<DragEdge> edgeCache = new ArrayList<DragEdge>();
    private boolean edgesInit = false;

    // ── Static handlers ──

    private static final DragHandler H_WIN_W = new DragHandler() {
        public void apply(UiLayoutProfile p, float mx, UiRect w, float s) {
            p.setSettingsWidth(UiMotion.clamp(mx - w.x, p.settingsMinWidth(), 1200.0F) / s);
        }
    };

    private static final DragHandler H_WIN_H = new DragHandler() {
        public void apply(UiLayoutProfile p, float my, UiRect w, float s) {
            p.setSettingsHeight(UiMotion.clamp(my - w.y, p.settingsMinHeight(), 900.0F) / s);
        }
    };

    private static final DragHandler H_ROW_SET = new DragHandler() {
        public void apply(UiLayoutProfile p, float my, UiRect w, float s) {
            float hH = 36.0F * s;
            float pd = 12.0F * s;
            float iH = 62.0F * s;
            float dH = 34.0F * s;
            float tY = w.y + hH + 10 * s + 10 * s + iH + 8 * s + dH + 10 * s;
            float rTop = tY + 20 * s + 10 * s;
            p.setRowSetting(UiMotion.clamp((my - rTop) / s, 20.0F, 38.0F));
        }
    };

    private static final ValueProvider V_WIN_W = new ValueProvider() {
        public String value(UiLayoutProfile p) { return "W:" + Math.round(p.settingsWidth()); }
    };

    private static final ValueProvider V_WIN_H = new ValueProvider() {
        public String value(UiLayoutProfile p) { return "H:" + Math.round(p.settingsHeight()); }
    };

    private static final ValueProvider V_ROW_SET = new ValueProvider() {
        public String value(UiLayoutProfile p) { return Math.round(p.rowSetting()) + "px"; }
    };

    public String displayName() { return "Settings"; }
    public String key() { return "settings"; }

    public List<Zone> computeZones(UiRect win, UiLayoutProfile lp, float k)
    {
        float headerH = HEADER_H * k;
        float pad = OUTER_PAD * k;

        float bodyX = win.x + pad;
        float bodyY = win.y + headerH + 10 * k;
        float bodyW = win.w - pad * 2;
        float bodyH = win.h - headerH - pad * 2;

        float infoH = UiMotion.clamp(INFO_CARD_H * k, 52 * k, 82 * k);
        float infoX = bodyX + 12 * k;
        float infoY = bodyY + 10 * k;
        float infoW = bodyW - 24 * k;

        float debugH = UiMotion.clamp(DEBUG_CARD_H * k, 28 * k, 42 * k);
        float debugY = infoY + infoH + 8 * k;

        float tabY = debugY + debugH + 10 * k;
        float tabW = (bodyW - 24 * k - 86 * k - 12 * k) * 0.5F;
        float tabX = bodyX + 12 * k;

        float rowsTop = tabY + 20 * k + 10 * k;
        float rowsBottom = bodyY + bodyH - 10 * k;
        float rowsH = Math.max(30 * k, rowsBottom - rowsTop);

        int idx = 0;
        idx = setZone(idx, "Header", win.x, win.y, win.w, headerH, 0, 0xAA0C151F);
        idx = setZone(idx, "Body", bodyX, bodyY, bodyW, bodyH, 0, 0x660C151F);
        idx = setZone(idx, "Info", infoX, infoY, infoW, infoH, 1, 0xBB122130);
        idx = setZone(idx, "Debug", infoX, debugY, infoW, debugH, 1, 0x88122130);
        idx = setZone(idx, "Theme", tabX, tabY, tabW, 20 * k, 2, 0x881A2736);
        idx = setZone(idx, "Anim", tabX + tabW + 6 * k, tabY, tabW, 20 * k, 2, 0x881A2736);
        idx = setZone(idx, "Rows", infoX, rowsTop, infoW, rowsH, 1, 0xBB122130);

        // Sample rows
        float rowH = ROW_SETTING * k;
        float rInX = infoX + 4 * k, rInY = rowsTop + 4 * k;
        float rInW = infoW - 8 * k, rInH = rowsH - 8 * k;
        float y = rInY;
        for (int i = 0; i < 8 && y + rowH <= rInY + rInH; i++)
        {
            idx = setZone(idx, "Set" + (i + 1), rInX, y, rInW, rowH - 2, 2, 0x881A2736);
            y += rowH;
        }

        this.zoneCacheSize = idx;
        return this.zoneCache.subList(0, idx);
    }

    public List<DragEdge> computeEdges(UiRect win, UiLayoutProfile lp, float k)
    {
        float hw = EDGE_HIT * k;
        float headerH = HEADER_H * k;
        float pad = OUTER_PAD * k;
        float infoH = UiMotion.clamp(INFO_CARD_H * k, 52 * k, 82 * k);
        float debugH = UiMotion.clamp(DEBUG_CARD_H * k, 28 * k, 42 * k);
        float tabY = win.y + headerH + 10 * k + 10 * k + infoH + 8 * k + debugH + 10 * k;
        float rowsTop = tabY + 20 * k + 10 * k;
        float rowH = ROW_SETTING * k;

        if (!this.edgesInit)
        {
            this.edgeCache.clear();
            for (int i = 0; i < 3; i++) this.edgeCache.add(new DragEdge());
            this.edgesInit = true;
        }

        this.edgeCache.get(0).set("win_w", "Window Width",
            win.x + win.w - hw * 0.5F, win.y, hw, win.h,
            true, H_WIN_W, V_WIN_W, 1.0F);

        this.edgeCache.get(1).set("win_h", "Window Height",
            win.x, win.y + win.h - hw * 0.5F, win.w, hw,
            false, H_WIN_H, V_WIN_H, 1.0F);

        this.edgeCache.get(2).set("row_set", "Setting Row Height",
            win.x + pad + 16 * k, rowsTop + rowH - hw * 0.5F,
            win.w - pad * 2 - 32 * k, hw,
            false, H_ROW_SET, V_ROW_SET, 1.0F);

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
