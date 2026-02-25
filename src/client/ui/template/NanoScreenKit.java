package client.ui.template;

import client.runtime.lwjgl.GlfwMouse;
import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;

/**
 * Shared drawing primitives for all Nano UI screens.
 * Eliminates duplicated slider, row, scroll, tab, and text rendering
 * across ClickGui, ClientSettings, UIScale, HudEditor, and AccountManager.
 */
public final class NanoScreenKit
{
    private NanoScreenKit()
    {
    }

    // ── Color interpolation ──

    public static int mixArgb(int from, int to, float t)
    {
        float k = UiMotion.clamp01(t);
        int a = lerpChannel((from >>> 24) & 255, (to >>> 24) & 255, k);
        int r = lerpChannel((from >>> 16) & 255, (to >>> 16) & 255, k);
        int g = lerpChannel((from >>> 8) & 255, (to >>> 8) & 255, k);
        int b = lerpChannel(from & 255, to & 255, k);
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    public static int lerpChannel(int from, int to, float t)
    {
        return NanoRenderUtils.clamp255(Math.round((float)from + (float)(to - from) * UiMotion.clamp01(t)));
    }

    public static float lerp(float from, float to, float t)
    {
        return from + (to - from) * UiMotion.clamp01(t);
    }

    // ── Text ellipsize ──

    public static String ellipsize(long vg, int fontId, float size, String text, float maxWidth)
    {
        String value = text == null ? "" : text;

        if (maxWidth <= 0.0F || value.isEmpty())
        {
            return "";
        }
        if (NanoRenderUtils.textWidth(vg, fontId, size, value) <= maxWidth)
        {
            return value;
        }

        String suffix = "...";
        float suffixWidth = NanoRenderUtils.textWidth(vg, fontId, size, suffix);
        String out = value;

        while (!out.isEmpty() && NanoRenderUtils.textWidth(vg, fontId, size, out) + suffixWidth > maxWidth)
        {
            out = out.substring(0, out.length() - 1);
        }

        if (out.isEmpty())
        {
            return suffixWidth <= maxWidth ? suffix : "";
        }

        return out + suffix;
    }

    // ── Slider track + knob + glow rendering ──

    /**
     * Draws a complete slider track with fill, glow, and knob.
     * Shared by ClickGui, HudEditor, UIScale, and ClientSettings.
     *
     * @param trackX      track left edge
     * @param trackY      track top edge
     * @param trackW      track width
     * @param trackH      track height
     * @param scale       UI scale factor (clamped 0.35..1.85)
     * @param fillRatio   animated fill ratio 0..1
     * @param knobRatio   animated knob position ratio 0..1
     * @param displayRatio animated display ratio for line glow
     * @param focus       hover/drag focus 0..1
     * @param glowRatio   glow intensity 0..1
     * @param dragging    whether the slider is being dragged
     * @param theme       current NanoTheme
     */
    public static void drawSliderTrack(long vg, MemoryStack stack, NanoTheme theme,
                                       float trackX, float trackY, float trackW, float trackH,
                                       float scale, float fillRatio, float knobRatio,
                                       float displayRatio, float focus, float glowRatio,
                                       boolean dragging)
    {
        float trackRadius = Math.min(trackH * 0.5F, Math.max(2.0F, theme.controlRadius()));
        int trackFill = mixArgb(theme.cardAltArgb(), theme.controlArgb(), UiMotion.clamp01(0.44F + focus * 0.30F));
        NanoUi.drawSurface(vg, stack, trackX, trackY, trackW, trackH, trackRadius,
                trackFill, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 114));

        float knobBase = scaled(5.8F, scale);
        float knobSize = knobBase + scaled(1.4F, scale) * focus
                + scaled(1.0F, scale) * glowRatio + (dragging ? scaled(1.1F, scale) : 0.0F);
        float rawHandleX = trackX + knobRatio * trackW;
        float handleX = UiMotion.clamp(rawHandleX, trackX + knobSize * 0.5F, trackX + trackW - knobSize * 0.5F);
        float knobX = handleX - knobSize * 0.5F;
        float knobY = trackY + (trackH - knobSize) * 0.5F;

        float inset = scaled(1.0F, scale);
        float trackInnerX = trackX + inset;
        float trackInnerW = Math.max(0.0F, trackW - inset * 2.0F);
        float knobLeadX = handleX - knobSize * 0.5F;
        // Fill bar (up to knob lead edge)
        float fillTargetEnd = trackInnerX + trackInnerW * fillRatio;
        float fillEnd = Math.min(fillTargetEnd, knobLeadX);
        float fillW = Math.max(0.0F, fillEnd - trackInnerX);
        int activeFill = mixArgb(theme.controlActiveArgb(), theme.accentArgb(), 0.74F);
        float innerRadius = Math.max(scaled(1.6F, scale), trackRadius - scaled(1.6F, scale));
        float innerH = trackH - inset * 2.0F;
        NanoUi.drawSurface(vg, stack, trackInnerX, trackY + inset, fillW,
                Math.max(0.0F, innerH), innerRadius, activeFill, 0);

        // Line glow overlay
        float lineGlowTargetEnd = trackInnerX + trackInnerW * displayRatio;
        float lineGlowEnd = Math.min(lineGlowTargetEnd, knobLeadX);
        float lineGlowW = Math.max(0.0F, lineGlowEnd - trackInnerX);
        float glowRadius = Math.max(scaled(1.2F, scale), trackRadius - scaled(2.2F, scale));
        NanoUi.drawSurface(vg, stack, trackInnerX, trackY + inset, lineGlowW,
                Math.max(0.0F, innerH), glowRadius,
                NanoRenderUtils.withAlpha(theme.accentSoftArgb(), 40 + Math.round(glowRatio * 56.0F)), 0);

        // Knob glow halo
        float glow = knobSize + scaled(1.2F, scale) * focus + scaled(1.8F, scale) * glowRatio;
        NanoUi.drawSurface(vg, stack, handleX - glow * 0.5F, trackY + (trackH - glow) * 0.5F,
                glow, glow, glow * 0.5F,
                NanoRenderUtils.withAlpha(0xFFF5F9FF, 48 + Math.round((focus * 0.52F + glowRatio * 0.48F) * 74.0F)), 0);

        // Knob circle
        int knobColor = mixArgb(theme.accentArgb(), 0xFFF8FBFF, UiMotion.clamp01(0.40F + focus * 0.52F));
        NanoUi.drawSurface(vg, stack, knobX, knobY, knobSize, knobSize, knobSize * 0.5F,
                knobColor, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 110));
    }

    // ── Animated row rendering ──

    /**
     * Draws a single list row with hover/select animation and optional stagger reveal.
     *
     * @param rowX        row left edge
     * @param rowY        row top edge
     * @param rowW        row width
     * @param rowH        row height
     * @param hovered     mouse is over this row
     * @param selected    row is selected
     * @param hoverRatio  animated hover 0..1
     * @param selectRatio animated select 0..1
     * @param reveal      stagger reveal 0..1 (1 = fully visible)
     * @param theme       current NanoTheme
     */
    public static void drawAnimatedRow(long vg, MemoryStack stack, NanoTheme theme,
                                       float rowX, float rowY, float rowW, float rowH,
                                       float hoverRatio, float selectRatio, float reveal)
    {
        int fill = mixArgb(theme.rowArgb(), theme.rowHoverArgb(), UiMotion.clamp01(hoverRatio * 0.80F));
        fill = mixArgb(fill, theme.rowSelectedArgb(), UiMotion.clamp01(selectRatio * 0.88F));
        int border = mixArgb(
                NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 72),
                NanoRenderUtils.withAlpha(theme.accentArgb(), 146),
                UiMotion.clamp01(selectRatio * 0.72F + hoverRatio * 0.20F));
        fill = NanoRenderUtils.mulAlpha(fill, reveal);
        border = NanoRenderUtils.mulAlpha(border, reveal);
        float radius = Math.min(theme.controlRadius(), rowH * 0.36F);
        NanoUi.drawSurface(vg, stack, rowX, rowY, rowW, rowH, radius, fill, border);
    }

    // ── Scroll animation ──

    /**
     * Computes the next visual scroll position with smooth animation.
     *
     * @param channelKey  animation bus channel key
     * @param target      target scroll value
     * @param current     current visual scroll value
     * @param profile     animation profile
     * @return animated scroll value
     */
    public static float animateScroll(String channelKey, float target, float current, UiAnimProfile profile)
    {
        if (profile == null || !profile.isEnabled())
        {
            return target;
        }

        float speed = UiAnimProfiles.scrollSpeed(profile);
        float distance = Math.abs(target - current);
        float adaptiveSpeed = UiMotion.clamp(speed + Math.min(0.38F, distance * 0.16F), 0.05F, 1.0F);
        return UiAnimationBus.animateWithSpeed(channelKey, target, profile, adaptiveSpeed);
    }

    // ── Tab rendering with animation ──

    /**
     * Draws a tab button with animated focus/active state.
     *
     * @param tabX      tab left edge
     * @param tabY      tab top edge
     * @param tabW      tab width
     * @param tabH      tab height
     * @param active    whether this tab is currently selected
     * @param hovered   whether mouse is over this tab
     * @param animKey   animation channel key prefix
     * @param profile   animation profile
     * @param theme     current NanoTheme
     * @param font      font id
     * @param fontSize  font size
     * @param label     tab label text
     */
    public static void drawTab(long vg, MemoryStack stack, NanoTheme theme,
                               float tabX, float tabY, float tabW, float tabH,
                               boolean active, boolean hovered, String animKey,
                               UiAnimProfile profile, int font, float fontSize, String label)
    {
        float focusVal = UiControlAnimations.focus(animKey + ".focus", hovered || active, profile);
        float activeVal = UiAnimationBus.animateControl(animKey + ".active", active ? 1.0F : 0.0F, profile);
        int fill = mixArgb(theme.controlArgb(), theme.controlActiveArgb(), UiMotion.clamp01(activeVal * 0.82F + focusVal * 0.18F));
        int border = mixArgb(
                NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 80),
                NanoRenderUtils.withAlpha(theme.accentArgb(), 140),
                UiMotion.clamp01(activeVal * 0.60F + focusVal * 0.25F));
        float radius = Math.min(tabH * 0.42F, theme.controlRadius());
        NanoUi.drawSurface(vg, stack, tabX, tabY, tabW, tabH, radius, fill, border);
        int textColor = mixArgb(theme.textMutedArgb(), theme.textArgb(), UiMotion.clamp01(activeVal * 0.72F + focusVal * 0.28F));
        NanoUi.drawCenterText(vg, stack, font, tabX + tabW * 0.5F, tabY + tabH * 0.5F, fontSize, textColor, label);
    }

    // ── Scaled helper ──

    public static float scaled(float base, float scale)
    {
        return base * scale;
    }

    // ── Mouse coordinate conversion ──

    public static float liveMouseX(Minecraft mc, int fallbackMouseX, int screenWidth)
    {
        if (mc == null)
        {
            return (float) fallbackMouseX;
        }

        int displayWidth = Math.max(1, mc.displayWidth);
        float raw = (float) GlfwMouse.getX() * (float) screenWidth / (float) displayWidth;
        return UiMotion.clamp(raw, 0.0F, Math.max(0.0F, (float) screenWidth - 1.0F));
    }

    public static float liveMouseY(Minecraft mc, int fallbackMouseY, int screenHeight)
    {
        if (mc == null)
        {
            return (float) fallbackMouseY;
        }

        int displayHeight = Math.max(1, mc.displayHeight);
        float raw = (float) screenHeight - (float) GlfwMouse.getY() * (float) screenHeight / (float) displayHeight - 1.0F;
        return UiMotion.clamp(raw, 0.0F, Math.max(0.0F, (float) screenHeight - 1.0F));
    }

}
