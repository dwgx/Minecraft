package client.ui.irc.component;

import client.ui.layout.UiRect;
import dwgx.nano.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Inline voice message player widget. Renders a play/pause button with
 * a progress bar and duration label inside a message bubble.
 */
public final class IRCVoicePlayer
{
    private static final float BTN_SIZE = 24.0F;
    private static final float BAR_H = 4.0F;
    private static final float PAD = 6.0F;

    private boolean playing;
    private String activeMessageId;
    private float progress;
    private int durationMs;

    public boolean isPlaying() { return this.playing; }
    public String getActiveMessageId() { return this.activeMessageId; }

    public void play(String messageId, int durationMs)
    {
        this.activeMessageId = messageId;
        this.durationMs = durationMs;
        this.progress = 0.0F;
        this.playing = true;
    }

    public void stop()
    {
        this.playing = false;
        this.progress = 0.0F;
        this.activeMessageId = null;
    }

    public void tick()
    {
        if (!this.playing || this.durationMs <= 0) return;
        // Simulate playback progress (~50ms per tick at 20 TPS)
        this.progress += 50.0F / this.durationMs;
        if (this.progress >= 1.0F)
        {
            this.progress = 1.0F;
            this.playing = false;
        }
    }

    /**
     * Render a voice player inline at the given position.
     * Returns the total width consumed.
     */
    public float render(long vg, MemoryStack stack, NanoTheme theme,
                        float x, float y, float maxW, String messageId, int durationMs,
                        int mx, int my)
    {
        boolean isActive = messageId != null && messageId.equals(this.activeMessageId);
        float barW = Math.min(maxW - BTN_SIZE - PAD * 3.0F, 120.0F);
        float totalW = BTN_SIZE + PAD + barW + PAD * 2.0F;
        float h = BTN_SIZE + PAD * 2.0F;

        // Background
        NanoRenderUtils.fillRoundedRect(vg, x, y, totalW, h, theme.cardRadius(),
                NanoRenderUtils.argb(stack, theme.cardArgb()));

        // Play/Pause button
        float btnX = x + PAD;
        float btnY = y + (h - BTN_SIZE) * 0.5F;
        boolean btnHover = mx >= btnX && mx <= btnX + BTN_SIZE && my >= btnY && my <= btnY + BTN_SIZE;
        int btnColor = btnHover ? theme.accentArgb() : theme.controlArgb();
        NanoRenderUtils.fillRoundedRect(vg, btnX, btnY, BTN_SIZE, BTN_SIZE,
                BTN_SIZE * 0.5F, NanoRenderUtils.argb(stack, btnColor));

        int font = NanoFontBook.uiBold();
        String icon = (isActive && this.playing) ? "||" : ">";
        NanoRenderUtils.drawLabel(vg, stack, font, btnX + BTN_SIZE * 0.5F,
                btnY + BTN_SIZE * 0.5F, 10.0F, icon, 0xFFFFFFFF,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Progress bar
        float barX = btnX + BTN_SIZE + PAD;
        float barY = y + (h - BAR_H) * 0.5F - 4.0F;
        NanoRenderUtils.fillRoundedRect(vg, barX, barY, barW, BAR_H,
                BAR_H * 0.5F, NanoRenderUtils.argb(stack, theme.controlArgb()));

        float fillW = isActive ? barW * this.progress : 0.0F;
        if (fillW > 0.0F)
        {
            NanoRenderUtils.fillRoundedRect(vg, barX, barY, fillW, BAR_H,
                    BAR_H * 0.5F, NanoRenderUtils.argb(stack, theme.accentArgb()));
        }

        // Duration label
        int secs = durationMs / 1000;
        String durText = secs + "s";
        NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                barX + barW * 0.5F, barY + BAR_H + 6.0F, 10.0F,
                durText, theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);

        return totalW;
    }

    /** Hit test the play button. */
    public boolean hitPlayButton(float x, float y, float maxW, int mx, int my)
    {
        float btnX = x + PAD;
        float h = BTN_SIZE + PAD * 2.0F;
        float btnY = y + (h - BTN_SIZE) * 0.5F;
        return mx >= btnX && mx <= btnX + BTN_SIZE && my >= btnY && my <= btnY + BTN_SIZE;
    }
}
