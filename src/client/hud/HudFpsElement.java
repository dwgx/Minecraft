package client.hud;

import client.core.ClientBootstrap;
import client.render.RenderContext2D;
import client.setting.EnumSetting;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.nvgFontFaceId;
import static org.lwjgl.nanovg.NanoVG.nvgFontSize;
import static org.lwjgl.nanovg.NanoVG.nvgText;
import static org.lwjgl.nanovg.NanoVG.nvgTextAlign;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Minimal HUD element showing current FPS for transform/layout verification.
 */
public final class HudFpsElement extends HudElement
{
    public enum FpsSource
    {
        GAME_DEBUG,
        DISPLAY_REFRESH
    }

    private final EnumSetting<FpsSource> source;

    public HudFpsElement()
    {
        super("hud_fps", "FPS", HudLayer.FOREGROUND);
        this.source = this.addSetting(new EnumSetting<FpsSource>("fps_source", "FPS Source", "Choose FPS source", FpsSource.class, FpsSource.GAME_DEBUG));
    }

    public FpsSource getSource()
    {
        return this.source.get();
    }

    public void cycleSource()
    {
        FpsSource[] values = FpsSource.values();
        int index = this.getSource().ordinal();
        int next = (index + 1) % values.length;
        this.source.set(values[next]);
    }

    public String sourceText()
    {
        return this.getSource() == FpsSource.DISPLAY_REFRESH ? this.tr("hud.fps.source.display", "display") : this.tr("hud.fps.source.debug", "debug");
    }

    public String displayText()
    {
        return this.tr("hud.fps.text", "FPS: {0}", Integer.valueOf(this.resolveFps()));
    }

    private int resolveFps()
    {
        if (this.getSource() == FpsSource.DISPLAY_REFRESH)
        {
            return Math.max(0, Display.getDesktopDisplayMode().getFrequency());
        }

        return Math.max(0, Minecraft.getDebugFPS());
    }

    public void render(RenderContext2D context)
    {
        if (context == null || context.getNanoVG() == null)
        {
            return;
        }

        long vg = context.getNanoVG().getHandle();

        if (vg == 0L)
        {
            return;
        }

        NanoFontBook.ensureLoaded(vg);
        int font = NanoFontBook.uiBold();
        String text = this.displayText();

        try (MemoryStack stack = stackPush())
        {
            NVGColor color = NanoRenderUtils.rgba(stack, 255, 255, 255, 220);
            nvgFontFaceId(vg, font);
            nvgFontSize(vg, 16.0F);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
            nvgFillColor(vg, color);
            nvgText(vg, 0.0F, 0.0F, text);
        }
    }

    private String tr(String key, String fallback, Object... args)
    {
        client.i18n.I18nManager i18n = ClientBootstrap.instance().getI18n();
        return i18n == null ? fallback : i18n.translateOrDefault(key, fallback, args);
    }
}
