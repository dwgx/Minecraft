package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint for the ClickGui three-column layout:
 * [Sidebar | Modules / Settings]
 */
public final class ClickGuiBlueprint implements GuiBlueprint
{
    private static final float EDGE_HIT = 7.0F;

    public String displayName()
    {
        return "ClickGUI";
    }

    public String key()
    {
        return "clickgui";
    }

    public List<Zone> computeZones(UiRect win, UiLayoutProfile lp, float k)
    {
        List<Zone> zones = new ArrayList<Zone>();
        float headerH = lp.headerHeight() * k;
        float pad = lp.outerPad() * k;
        float gap = lp.gapMajor() * k;

        UiRect header = new UiRect(win.x, win.y, win.w, headerH);
        float bodyY = header.y2();
        float bodyH = win.h - headerH - pad;
        float contentW = win.w - pad * 2.0F - gap;

        float sideW = UiMotion.clamp(contentW * lp.clickGuiSidebarRatio(),
            contentW * lp.clickGuiSidebarMinRatio(), contentW * lp.clickGuiSidebarMaxRatio());
        UiRect sidebar = new UiRect(win.x + pad, bodyY + pad * 0.5F, sideW, bodyH);

        float mainX = sidebar.x2() + gap;
        float mainW = Math.max(60.0F, win.x2() - mainX - pad);
        float modH = UiMotion.clamp(bodyH * lp.clickGuiModulesRatio(),
            bodyH * lp.clickGuiModulesMinRatio(), bodyH * lp.clickGuiModulesMaxRatio());
        UiRect modules = new UiRect(mainX, sidebar.y, mainW, modH);
        UiRect settings = new UiRect(mainX, modules.y2() + gap,
            mainW, Math.max(40.0F, sidebar.y2() - modules.y2() - gap));

        float rowCatH = lp.rowCategory() * k;
        float rowModH = lp.rowModule() * k;
        float rowSetH = lp.rowSetting() * k;
        float valColW = lp.valueColWidth() * k;

        zones.add(  new Zone("Header", header, 0, 0xAA0C151F));
        zones.add(new Zone("Sidebar", sidebar, 1, 0xBB0C151F));
        zones.add(new Zone("Modules", modules, 1, 0xBB122130));
        zones.add(new Zone("Settings", settings, 1, 0xBB122130));

        addSampleRows(zones, "Cat", sidebar.inset(4 * k), rowCatH, 5, 0x881A2736);
        addSampleRows(zones, "Mod", modules.inset(4 * k), rowModH, 4, 0x881A2736);
        UiRect setInner = settings.inset(4 * k);
        addSettingRows(zones, setInner, rowSetH, valColW, 5, k);

        return zones;
    }

    public List<DragEdge> computeEdges(UiRect win, UiLayoutProfile lp, float k)
    {
        List<DragEdge> edges = new ArrayList<DragEdge>();
        float headerH = lp.headerHeight() * k;
        float pad = lp.outerPad() * k;
        float gap = lp.gapMajor() * k;
        float contentW = win.w - pad * 2.0F - gap;
        float bodyY = win.y + headerH;
        float bodyH = win.h - headerH - pad;

        float sideW = UiMotion.clamp(contentW * lp.clickGuiSidebarRatio(),
            contentW * lp.clickGuiSidebarMinRatio(), contentW * lp.clickGuiSidebarMaxRatio());
        float sideRight = win.x + pad + sideW;
        float mainX = sideRight + gap;
        float mainW = Math.max(60.0F, win.x2() - mainX - pad);
        float modH = UiMotion.clamp(bodyH * lp.clickGuiModulesRatio(),
            bodyH * lp.clickGuiModulesMinRatio(), bodyH * lp.clickGuiModulesMaxRatio());
        float modBottom = bodyY + pad * 0.5F + modH;
        float valColW = lp.valueColWidth() * k;
        float settingsRight = win.x2() - pad;

        float hw = EDGE_HIT * k;

        // Sidebar right edge
        edges.add(new DragEdge("sidebar_w", "Sidebar Width",
            new UiRect(sideRight - hw * 0.5F, bodyY, hw, bodyH), true,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float mx, UiRect w, float s)
                {
                    float pd = p.outerPad() * s;
                    float gp = p.gapMajor() * s;
                    float cw = w.w - pd * 2.0F - gp;
                    float ratio = UiMotion.clamp((mx - w.x - pd) / cw,
                        p.clickGuiSidebarMinRatio(), p.clickGuiSidebarMaxRatio());
                    p.setClickGuiSidebarRatio(ratio);
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0f%%", p.clickGuiSidebarRatio() * 100);
                }
            },
            0.01F));

        // Modules bottom edge
        edges.add(new DragEdge("modules_h", "Modules Height",
            new UiRect(mainX, modBottom - hw * 0.5F, mainW, hw), false,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float my, UiRect w, float s)
                {
                    float hH = p.headerHeight() * s;
                    float pd = p.outerPad() * s;
                    float bY = w.y + hH + pd * 0.5F;
                    float bH = w.h - hH - pd;
                    float ratio = UiMotion.clamp((my - bY) / bH,
                        p.clickGuiModulesMinRatio(), p.clickGuiModulesMaxRatio());
                    p.setClickGuiModulesRatio(ratio);
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0f%%", p.clickGuiModulesRatio() * 100);
                }
            },
            0.01F));

        // Value column left edge
        float valEdgeX = settingsRight - 4 * k - valColW;
        float setTop = modBottom + gap;
        float setH = Math.max(40.0F, bodyY + pad * 0.5F + bodyH - setTop);
        edges.add(new DragEdge("value_col", "Value Column",
            new UiRect(valEdgeX - hw * 0.5F, setTop, hw, setH), true,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float mx, UiRect w, float s)
                {
                    float pd = p.outerPad() * s;
                    float right = w.x2() - pd - 4 * s;
                    float newW = (right - mx) / s;
                    p.setValueColWidth(UiMotion.clamp(newW, 80.0F, 200.0F));
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0fpx", p.valueColWidth());
                }
            },
            1.0F));

        // Gap between sidebar and main
        float gapCenter = (sideRight + mainX) * 0.5F;
        edges.add(new DragEdge("gap", "Panel Gap",
            new UiRect(gapCenter - hw * 0.5F, bodyY, hw, bodyH), true,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float mx, UiRect w, float s)
                {
                    float pd = p.outerPad() * s;
                    float cw = w.w - pd * 2.0F;
                    float sideW2 = cw * p.clickGuiSidebarRatio();
                    float sideRight2 = w.x + pd + sideW2;
                    float newGap = Math.max(0, mx - sideRight2) * 2.0F / s;
                    p.setGapMajor(UiMotion.clamp(newGap, 6.0F, 24.0F));
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0fpx", p.gapMajor());
                }
            },
            1.0F));

        // Row heights
        float rowCatH = lp.rowCategory() * k;
        float sideInnerY = bodyY + pad * 0.5F + 4 * k;
        edges.add(new DragEdge("row_cat", "Category Row",
            new UiRect(win.x + pad + 4 * k, sideInnerY + rowCatH - hw * 0.5F,
                sideW - 8 * k, hw), false,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float my, UiRect w, float s)
                {
                    float hH = p.headerHeight() * s;
                    float pd = p.outerPad() * s;
                    float baseY = w.y + hH + pd * 0.5F + 4 * s;
                    float newH = (my - baseY) / s;
                    p.setRowCategory(UiMotion.clamp(newH, 18.0F, 36.0F));
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0fpx", p.rowCategory());
                }
            },
            1.0F));

        float rowModH = lp.rowModule() * k;
        float modInnerY = bodyY + pad * 0.5F + 4 * k;
        edges.add(new DragEdge("row_mod", "Module Row",
            new UiRect(mainX + 4 * k, modInnerY + rowModH - hw * 0.5F,
                mainW - 8 * k, hw), false,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float my, UiRect w, float s)
                {
                    float hH = p.headerHeight() * s;
                    float pd = p.outerPad() * s;
                    float baseY = w.y + hH + pd * 0.5F + 4 * s;
                    float newH = (my - baseY) / s;
                    p.setRowModule(UiMotion.clamp(newH, 24.0F, 48.0F));
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0fpx", p.rowModule());
                }
            },
            1.0F));

        float rowSetH = lp.rowSetting() * k;
        float setInnerY = setTop + 4 * k;
        edges.add(new DragEdge("row_set", "Setting Row",
            new UiRect(mainX + 4 * k, setInnerY + rowSetH - hw * 0.5F,
                mainW - 8 * k, hw), false,
            new DragHandler()
            {
                public void apply(UiLayoutProfile p, float my, UiRect w, float s)
                {
                    float gp = p.gapMajor() * s;
                    float hH = p.headerHeight() * s;
                    float pd = p.outerPad() * s;
                    float bH = w.h - hH - pd;
                    float mH = bH * p.clickGuiModulesRatio();
                    float sTop = w.y + hH + pd * 0.5F + mH + gp + 4 * s;
                    float newH = (my - sTop) / s;
                    p.setRowSetting(UiMotion.clamp(newH, 20.0F, 38.0F));
                }
            },
            new ValueProvider()
            {
                public String value(UiLayoutProfile p)
                {
                    return String.format("%.0fpx", p.rowSetting());
                }
            },
            1.0F));

        return edges;
    }

    private static void addSampleRows(List<Zone> zones, String prefix, UiRect area,
                                       float rowH, int count, int color)
    {
        float y = area.y;

        for (int i = 0; i < count && y + rowH <= area.y2(); i++)
        {
            zones.add(new Zone(prefix + (i + 1),
                new UiRect(area.x, y, area.w, rowH - 2), 2, color));
            y += rowH;
        }
    }

    private static void addSettingRows(List<Zone> zones, UiRect area, float rowH,
                                        float valColW, int count, float k)
    {
        float y = area.y;

        for (int i = 0; i < count && y + rowH <= area.y2(); i++)
        {
            zones.add(new Zone("Set" + (i + 1),
                new UiRect(area.x, y, area.w, rowH - 2), 2, 0x881A2736));
            float valX = area.x + area.w - valColW;
            zones.add(new Zone("",
                new UiRect(valX, y + 1, valColW - 2, rowH - 4), 2, 0x66223142));
            y += rowH;
        }
    }
}
