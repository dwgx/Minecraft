package client.ui.template;

import dwgx.nano.NanoRenderUtils;
import dwgx.nano.NanoTheme;
import dwgx.nano.NanoUi;
import org.lwjgl.system.MemoryStack;

/**
 * Shared ON/OFF pill switch used across Nano UI surfaces.
 */
public final class UiStateToggle
{
    private UiStateToggle()
    {
    }

    public static void draw(
        long vg,
        MemoryStack stack,
        float x,
        float y,
        float w,
        float h,
        boolean enabled,
        boolean hovered,
        NanoTheme theme,
        UiAnimProfile profile,
        String animKey,
        int font,
        float scale,
        String offLabel,
        String onLabel
    )
    {
        if (vg == 0L || stack == null || theme == null)
        {
            return;
        }

        UiAnimProfile animProfile = profile == null ? UiAnimProfile.defaults() : profile;
        float k = UiMotion.clamp(scale, 0.35F, 1.85F);
        float ratio = UiAnimationBus.animateControl(animKey, enabled ? 1.0F : 0.0F, animProfile);
        float focus = UiAnimationBus.animateControl(animKey + ".focus", hovered ? 1.0F : 0.0F, animProfile);
        float pillH = Math.min(h, scaled(14.0F, k));
        float pillW = Math.min(w, scaled(92.0F, k));
        float px = x + (w - pillW);
        float py = y + (h - pillH) * 0.5F;
        float thumbW = pillW * 0.5F - scaled(2.0F, k);
        float thumbX = px + scaled(1.0F, k) + (pillW - scaled(2.0F, k) - thumbW) * ratio;
        float thumbY = py + scaled(1.0F, k);
        float thumbH = pillH - scaled(2.0F, k);
        float thumbExpand = scaled(1.3F, k) * focus;
        int base = mixArgb(theme.controlArgb(), theme.controlHoverArgb(), UiMotion.clamp01(0.18F + focus * 0.48F));
        NanoUi.drawSurface(vg, stack, px, py, pillW, pillH, pillH * 0.5F, base, NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 98));
        NanoUi.drawSurface(vg, stack, px + pillW * 0.5F, py + scaled(1.0F, k), scaled(1.0F, k), pillH - scaled(2.0F, k), scaled(0.5F, k), NanoRenderUtils.withAlpha(theme.windowBorderArgb(), 74), 0);
        int thumb = mixArgb(mixArgb(theme.cardAltArgb(), theme.controlHoverArgb(), 0.48F), theme.controlActiveArgb(), UiMotion.clamp01(ratio));
        NanoUi.drawSurface(vg, stack, thumbX - thumbExpand * 0.5F, thumbY - thumbExpand * 0.5F, thumbW + thumbExpand, thumbH + thumbExpand, (thumbH + thumbExpand) * 0.5F, thumb, NanoRenderUtils.withAlpha(0xFFFFFFFF, 84));
        int disableText = mixArgb(theme.textArgb(), theme.textMutedArgb(), UiMotion.clamp01(ratio * 0.95F));
        int enableText = mixArgb(theme.textMutedArgb(), theme.textArgb(), UiMotion.clamp01(ratio * 0.95F));
        float textCenterY = py + pillH * 0.5F + scaled(0.9F, k);
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.25F, textCenterY, scaled(8.2F, k), disableText, offLabel == null ? "OFF" : offLabel);
        NanoUi.drawCenterText(vg, stack, font, px + pillW * 0.75F, textCenterY, scaled(8.2F, k), enableText, onLabel == null ? "ON" : onLabel);
    }

    private static float scaled(float value, float scale)
    {
        return value * scale;
    }

    private static int mixArgb(int from, int to, float ratio)
    {
        float t = UiMotion.clamp01(ratio);
        int ar = from >>> 16 & 255;
        int ag = from >>> 8 & 255;
        int ab = from & 255;
        int aa = from >>> 24 & 255;
        int br = to >>> 16 & 255;
        int bg = to >>> 8 & 255;
        int bb = to & 255;
        int ba = to >>> 24 & 255;
        int r = Math.round((float)ar + (float)(br - ar) * t);
        int g = Math.round((float)ag + (float)(bg - ag) * t);
        int b = Math.round((float)ab + (float)(bb - ab) * t);
        int a = Math.round((float)aa + (float)(ba - aa) * t);
        return (NanoRenderUtils.clamp255(a) & 255) << 24 | (NanoRenderUtils.clamp255(r) & 255) << 16 | (NanoRenderUtils.clamp255(g) & 255) << 8 | NanoRenderUtils.clamp255(b) & 255;
    }
}
