package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimation;
import client.ui.template.UiAnimationBus;
import client.ui.template.UiLayoutProfile;
import client.ui.template.UiMotion;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

import java.util.List;

/**
 * Blueprint renderer for GuiEdit.
 * Zone/edge positions use fast-follow animation during drag (same pattern as
 * NanoSliderController), normal-speed animation when idle.
 * Only hover/drag visual effects use control-speed channels.
 */
public final class GuiPreviewRenderer
{
    private static final String ANIM_EDGE = "guiedit.edge.";
    private static final String ANIM_ZONE = "guiedit.z.";

    private GuiPreviewRenderer() {}

    /**
     * Fast-follow animate: during drag, speed is boosted so positions track the
     * mouse with minimal lag but still feel smooth (same idea as
     * NanoSliderController.resolveDisplayRatio with dragging=true).
     */
    private static float followAnimate(String key, float target, UiAnimProfile anim, boolean dragging)
    {
        float speed = anim.sliderSpeed();
        float smooth = anim.smooth();
        UiAnimation.Type type = anim.type();
        boolean enabled = anim.isEnabled();

        if (dragging)
        {
            // Boost speed during drag — matches NanoSliderController pattern
            speed = UiMotion.clamp(speed * 1.62F + 0.12F, 0.12F, 1.0F);
        }

        return UiAnimationBus.animate(key, target, speed, smooth, type, enabled);
    }

    // ── Zone rendering ──

    public static void renderZones(long vg, MemoryStack stack, List<GuiBlueprint.Zone> zones,
                                   NanoTheme theme, float scale, UiAnimProfile anim,
                                   boolean dragging)
    {
        int regular = NanoFontBook.uiRegular();
        int bold = NanoFontBook.uiBold();

        for (int i = 0; i < zones.size(); i++)
        {
            GuiBlueprint.Zone zone = zones.get(i);
            if (zone.rw < 2 || zone.rh < 2) continue;

            String p = ANIM_ZONE + i;
            float rx = followAnimate(p + "x", zone.rx, anim, dragging);
            float ry = followAnimate(p + "y", zone.ry, anim, dragging);
            float rw = followAnimate(p + "w", zone.rw, anim, dragging);
            float rh = followAnimate(p + "h", zone.rh, anim, dragging);
            if (rw < 2 || rh < 2) continue;

            float radius;
            int topColor, bottomColor, borderColor;

            switch (zone.depth)
            {
                case 0:
                    radius = theme.windowRadius() * 0.5F * scale;
                    topColor = zone.fillArgb;
                    bottomColor = NanoRenderUtils.withAlpha(zone.fillArgb, (int)((zone.fillArgb >>> 24) * 0.7F));
                    borderColor = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 100);
                    break;
                case 1:
                    radius = theme.cardRadius() * 0.5F * scale;
                    topColor = zone.fillArgb;
                    bottomColor = NanoRenderUtils.withAlpha(zone.fillArgb, (int)((zone.fillArgb >>> 24) * 0.8F));
                    borderColor = NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 60);
                    break;
                default:
                    radius = 3.0F * scale;
                    topColor = zone.fillArgb;
                    bottomColor = zone.fillArgb;
                    borderColor = 0;
                    break;
            }

            if (zone.depth <= 1)
            {
                NanoRenderUtils.fillRoundedRectGradient(vg, stack, rx, ry, rw, rh,
                    radius, topColor, bottomColor, true);
                NanoRenderUtils.strokeRoundedRect(vg, rx, ry, rw, rh,
                    radius, 1.0F, NanoRenderUtils.argb(stack, borderColor));
            }
            else
            {
                NanoRenderUtils.fillRoundedRect(vg, rx, ry, rw, rh,
                    radius, NanoRenderUtils.argb(stack, topColor));
            }

            if (zone.label != null && !zone.label.isEmpty())
            {
                if (zone.depth == 0)
                {
                    float fs = Math.max(9.0F, 12.0F * scale);
                    NanoUi.drawCenterText(vg, stack, bold,
                        rx + rw * 0.5F, ry + fs + 4.0F * scale,
                        fs, NanoRenderUtils.withAlpha(theme.textArgb(), 200), zone.label);
                }
                else if (zone.depth == 1)
                {
                    float fs = Math.max(8.0F, 10.0F * scale);
                    NanoUi.drawCenterText(vg, stack, regular,
                        rx + rw * 0.5F, ry + fs + 3.0F * scale,
                        fs, NanoRenderUtils.withAlpha(theme.textMutedArgb(), 220), zone.label);
                }
            }

            if (zone.depth == 2 && rw > 16 * scale && rh > 6 * scale)
            {
                renderExampleContent(vg, stack, zone.label, rx, ry, rw, rh, theme, scale);
            }
        }
    }

    private static void renderExampleContent(long vg, MemoryStack stack, String label,
                                              float rx, float ry, float rw, float rh,
                                              NanoTheme theme, float k)
    {
        if (label == null) label = "";
        float pad = 4.0F * k;
        float cy = ry + rh * 0.5F;

        if (label.startsWith("Set"))
        {
            float textW = Math.min((rw - pad * 2) * 0.4F, 50 * k);
            float h = 2.5F * k;
            NanoRenderUtils.fillRoundedRect(vg, rx + pad, cy - h * 0.5F, textW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textWeakArgb(), 45)));
            float slW = Math.min((rw - pad * 2) * 0.3F, 40 * k);
            float slX = rx + rw - pad - slW;
            NanoRenderUtils.fillRoundedRect(vg, slX, cy - h * 0.5F, slW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.controlArgb(), 70)));
            float fillW = slW * 0.55F;
            NanoRenderUtils.fillRoundedRect(vg, slX, cy - h * 0.5F, fillW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.accentArgb(), 90)));
        }
        else if (label.startsWith("Cat"))
        {
            float textW = Math.min((rw - pad * 2) * 0.5F, 42 * k);
            float h = 2.5F * k;
            NanoRenderUtils.fillRoundedRect(vg, rx + pad + 4 * k, cy - h * 0.5F, textW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textArgb(), 40)));
        }
        else if (label.startsWith("Mod"))
        {
            float textW = Math.min((rw - pad * 2) * 0.45F, 45 * k);
            float h = 2.5F * k;
            NanoRenderUtils.fillRoundedRect(vg, rx + pad + 4 * k, cy - h * 0.5F - 1.5F * k, textW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textArgb(), 40)));
            float pillW = 14 * k;
            float pillH = 7 * k;
            boolean on = (label.hashCode() & 1) == 0;
            int pillCol = NanoRenderUtils.withAlpha(on ? theme.accentArgb() : theme.controlArgb(), on ? 80 : 60);
            NanoRenderUtils.fillRoundedRect(vg, rx + rw - pad - pillW, cy - pillH * 0.5F, pillW, pillH,
                pillH * 0.5F, NanoRenderUtils.argb(stack, pillCol));
        }
        else if (label.startsWith("Acc"))
        {
            float sz = Math.min(rh - 4 * k, 12 * k);
            NanoRenderUtils.fillRoundedRect(vg, rx + pad + 2 * k, cy - sz * 0.5F, sz, sz,
                sz * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.controlHoverArgb(), 60)));
            float textW = Math.min((rw - pad * 2) * 0.35F, 35 * k);
            float h = 2.5F * k;
            NanoRenderUtils.fillRoundedRect(vg, rx + pad + sz + 6 * k, cy - h * 0.5F, textW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textArgb(), 40)));
        }
        else if (isButtonLabel(label))
        {
            float textW = Math.min((rw - pad * 2) * 0.5F, 30 * k);
            float h = 2.5F * k;
            NanoRenderUtils.fillRoundedRect(vg, rx + (rw - textW) * 0.5F, cy - h * 0.5F, textW, h,
                h * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.textMutedArgb(), 40)));
        }
    }

    private static boolean isButtonLabel(String label)
    {
        return "Add".equals(label) || "Login".equals(label) || "Delete".equals(label)
            || "Clear".equals(label) || "MS Login".equals(label) || "Input".equals(label)
            || "Theme".equals(label) || "Anim".equals(label);
    }

    // ── Edge rendering ──

    public static void renderEdges(long vg, MemoryStack stack, List<GuiBlueprint.DragEdge> edges,
                                   String hoveredId, String draggingId, NanoTheme theme,
                                   float scale, UiAnimProfile anim, UiLayoutProfile lp,
                                   UiRect previewBounds)
    {
        int bold = NanoFontBook.uiBold();
        boolean anyDragging = draggingId != null;

        int handleIdle = NanoRenderUtils.withAlpha(theme.accentSoftArgb(), 0x66);
        int handleHover = theme.accentArgb();
        int handleDrag = theme.controlActiveArgb();

        for (int i = 0; i < edges.size(); i++)
        {
            GuiBlueprint.DragEdge edge = edges.get(i);
            boolean hovered = edge.id.equals(hoveredId);
            boolean dragging = edge.id.equals(draggingId);

            float hoverT = UiAnimationBus.animateControl(
                ANIM_EDGE + edge.id + ".hover",
                (hovered || dragging) ? 1.0F : 0.0F, anim);
            float dragT = UiAnimationBus.animateControl(
                ANIM_EDGE + edge.id + ".drag",
                dragging ? 1.0F : 0.0F, anim);

            // Edge positions: fast-follow during drag, normal animate when idle
            String ep = ANIM_EDGE + edge.id;
            float ehx = followAnimate(ep + "x", edge.hx, anim, anyDragging);
            float ehy = followAnimate(ep + "y", edge.hy, anim, anyDragging);
            float ehw = followAnimate(ep + "w", edge.hw, anim, anyDragging);
            float ehh = followAnimate(ep + "h", edge.hh, anim, anyDragging);

            if (hoverT < 0.01F) continue;

            float cx = ehx + ehw * 0.5F;
            float cy = ehy + ehh * 0.5F;

            if (hoverT > 0.05F)
            {
                int guideBase = dragT > 0.5F ? handleDrag : handleIdle;
                int guideColor = NanoRenderUtils.withAlpha(guideBase,
                    (int)(UiMotion.clamp01(hoverT * 0.4F) * 255));

                if (edge.vertical)
                {
                    NanoRenderUtils.fillRect(vg, cx - 0.5F, previewBounds.y,
                        1.0F, previewBounds.h, NanoRenderUtils.argb(stack, guideColor));
                }
                else
                {
                    NanoRenderUtils.fillRect(vg, previewBounds.x, cy - 0.5F,
                        previewBounds.w, 1.0F, NanoRenderUtils.argb(stack, guideColor));
                }
            }

            int baseColor = lerpArgb(handleIdle, handleHover, hoverT);
            int handleColor = lerpArgb(baseColor, handleDrag, dragT);
            float thickness = 1.5F + hoverT * 1.0F + dragT * 0.5F;
            float glowA = UiMotion.clamp01(hoverT * 0.3F + dragT * 0.2F);

            if (edge.vertical)
            {
                float lx = cx - thickness * 0.5F;
                if (glowA > 0.01F)
                {
                    NanoRenderUtils.fillRoundedRect(vg, lx - 3.0F, ehy, thickness + 6.0F, ehh,
                        3.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(handleColor, (int)(glowA * 60))));
                }
                NanoRenderUtils.fillRoundedRect(vg, lx, ehy, thickness, ehh,
                    thickness * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(handleColor, hoverT)));
            }
            else
            {
                float ly = cy - thickness * 0.5F;
                if (glowA > 0.01F)
                {
                    NanoRenderUtils.fillRoundedRect(vg, ehx, ly - 3.0F, ehw, thickness + 6.0F,
                        3.0F, NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(handleColor, (int)(glowA * 60))));
                }
                NanoRenderUtils.fillRoundedRect(vg, ehx, ly, ehw, thickness,
                    thickness * 0.5F, NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(handleColor, hoverT)));
            }

            float gripSize = (3.0F + hoverT * 1.5F + dragT * 1.0F) * scale;
            NanoRenderUtils.fillRoundedRect(vg,
                cx - gripSize * 0.5F, cy - gripSize * 0.5F, gripSize, gripSize, gripSize * 0.5F,
                NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(handleColor, UiMotion.clamp01(hoverT * 1.2F))));

            // Value badge
            if (hoverT > 0.1F && edge.valueProvider != null)
            {
                String valueText = edge.valueProvider.value(lp);
                if (valueText == null || valueText.isEmpty()) continue;

                float badgeAlpha = UiMotion.clamp01((hoverT - 0.1F) * 1.5F);
                float fontSize = Math.max(8.0F, 10.0F * scale);
                float textW = NanoRenderUtils.textWidth(vg, bold, fontSize, valueText);
                float padH = 6.0F * scale;
                float padV = 3.0F * scale;
                float badgeW = textW + padH * 2;
                float badgeH = fontSize + padV * 2;
                float badgeR = badgeH * 0.4F;

                float bx, by;
                if (edge.vertical)
                {
                    bx = cx + 8.0F * scale;
                    by = cy - badgeH * 0.5F;
                }
                else
                {
                    bx = cx - badgeW * 0.5F;
                    by = cy - badgeH - 6.0F * scale;
                }

                bx = UiMotion.clamp(bx, previewBounds.x + 2, previewBounds.x2() - badgeW - 2);
                by = Math.max(previewBounds.y + 2, by);

                int bgCol = NanoRenderUtils.mulAlpha(
                    NanoRenderUtils.withAlpha(theme.windowTopArgb(), 0xEE), badgeAlpha);
                int txtCol = dragging
                    ? NanoRenderUtils.mulAlpha(theme.controlActiveArgb(), badgeAlpha)
                    : NanoRenderUtils.mulAlpha(theme.textArgb(), badgeAlpha);

                NanoRenderUtils.fillRoundedRect(vg, bx + 1, by + 1.5F, badgeW, badgeH, badgeR,
                    NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(0xFF000000, (int)(badgeAlpha * 40))));
                NanoRenderUtils.fillRoundedRect(vg, bx, by, badgeW, badgeH, badgeR,
                    NanoRenderUtils.argb(stack, bgCol));
                NanoRenderUtils.strokeRoundedRect(vg, bx, by, badgeW, badgeH, badgeR, 0.5F,
                    NanoRenderUtils.argb(stack, NanoRenderUtils.mulAlpha(
                        NanoRenderUtils.withAlpha(handleColor, 80), badgeAlpha)));
                NanoUi.drawCenterText(vg, stack, bold,
                    bx + badgeW * 0.5F, by + badgeH * 0.5F, fontSize, txtCol, valueText);

                if (dragT < 0.3F && edge.tooltip != null && !edge.tooltip.isEmpty())
                {
                    float tipAlpha = UiMotion.clamp01((hoverT - 0.3F) * 2.0F) * (1.0F - dragT);
                    if (tipAlpha > 0.01F)
                    {
                        int regular = NanoFontBook.uiRegular();
                        float tipFs = Math.max(7.0F, 8.5F * scale);
                        float tipTextW = NanoRenderUtils.textWidth(vg, regular, tipFs, edge.tooltip);
                        float tipPH = 5.0F * scale;
                        float tipPV = 2.5F * scale;
                        float tipW = tipTextW + tipPH * 2;
                        float tipH = tipFs + tipPV * 2;
                        float tipR = tipH * 0.35F;

                        float tipX = bx + (badgeW - tipW) * 0.5F;
                        float tipY = by + badgeH + 3.0F * scale;

                        tipX = UiMotion.clamp(tipX, previewBounds.x + 2, previewBounds.x2() - tipW - 2);
                        tipY = Math.min(tipY, previewBounds.y2() - tipH - 2);

                        int tipBg = NanoRenderUtils.mulAlpha(
                            NanoRenderUtils.withAlpha(theme.windowTopArgb(), 0xCC), tipAlpha);
                        int tipText = NanoRenderUtils.mulAlpha(theme.textMutedArgb(), tipAlpha);

                        NanoRenderUtils.fillRoundedRect(vg, tipX, tipY, tipW, tipH, tipR,
                            NanoRenderUtils.argb(stack, tipBg));
                        NanoUi.drawCenterText(vg, stack, regular,
                            tipX + tipW * 0.5F, tipY + tipH * 0.5F, tipFs, tipText, edge.tooltip);
                    }
                }
            }
        }
    }

    // ── Hit testing ──

    public static String hitTestEdge(List<GuiBlueprint.DragEdge> edges, float mx, float my)
    {
        for (int i = 0; i < edges.size(); i++)
        {
            if (edges.get(i).containsMouse(mx, my))
            {
                return edges.get(i).id;
            }
        }
        return null;
    }

    public static GuiBlueprint.DragEdge findEdge(List<GuiBlueprint.DragEdge> edges, String id)
    {
        if (id == null) return null;
        for (int i = 0; i < edges.size(); i++)
        {
            if (id.equals(edges.get(i).id)) return edges.get(i);
        }
        return null;
    }

    private static int lerpArgb(int from, int to, float t)
    {
        if (t <= 0.0F) return from;
        if (t >= 1.0F) return to;
        int fa = (from >>> 24) & 0xFF, fr = (from >>> 16) & 0xFF, fg = (from >>> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >>> 16) & 0xFF, tg = (to >>> 8) & 0xFF, tb = to & 0xFF;
        return ((int)(fa + (ta - fa) * t) & 0xFF) << 24
             | ((int)(fr + (tr - fr) * t) & 0xFF) << 16
             | ((int)(fg + (tg - fg) * t) & 0xFF) << 8
             | ((int)(fb + (tb - fb) * t) & 0xFF);
    }
}
