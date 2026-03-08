package client.ui.irc.component;

import client.chat.model.*;
import client.chat.store.ChatStore;
import client.ui.layout.UiRect;
import dwgx.nano.*;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Scrollable message area with QQ/WeChat-style bubbles.
 * Own messages right-aligned (accent), others left-aligned (card).
 * Supports multi-line word-wrap, image thumbnails, and voice playback bars.
 */
public final class IRCMessageArea
{
    private static final float PAD = 12.0F;
    private static final float AVATAR_SIZE = 28.0F;
    private static final float BUBBLE_PAD = 8.0F;
    private static final float FONT_SIZE = 13.0F;
    private static final float SMALL_FONT = 10.0F;
    private static final float LINE_SPACING = 3.0F;
    private static final float IMAGE_MAX_W = 200.0F;
    private static final float IMAGE_MAX_H = 150.0F;
    private static final float VOICE_BAR_W = 160.0F;
    private static final float VOICE_BAR_H = 32.0F;

    /** NanoVG image handle cache: localPath → nvg image id. -1 = failed to load. */
    private final Map<String, Integer> imageCache = new HashMap<String, Integer>();
    /** Per-frame layout cache for accurate click hit-testing. */
    private final List<MessageHitBox> hitBoxes = new ArrayList<MessageHitBox>();

    private float scrollOffset;
    private float scrollTarget;
    private boolean pendingScrollToBottom;

    public float getScrollTarget() { return this.scrollTarget; }
    public void setScrollTarget(float target) { this.scrollTarget = target; }

    public void scroll(int delta)
    {
        this.scrollTarget -= delta * 0.5F;
        if (this.scrollTarget < 0.0F) this.scrollTarget = 0.0F;
    }

    public void scrollToBottom() { this.pendingScrollToBottom = true; }

    public void resetScroll()
    {
        this.scrollTarget = 0.0F;
        this.scrollOffset = 0.0F;
        this.pendingScrollToBottom = false;
    }

    /** Release all cached NanoVG images. Call when screen closes. */
    public void cleanup(long vg)
    {
        for (int handle : this.imageCache.values())
        {
            if (handle > 0) nvgDeleteImage(vg, handle);
        }
        this.imageCache.clear();
    }

    /**
     * Load or retrieve a cached NanoVG image handle for the given local path.
     * Returns the handle (>0) on success, or -1 if the file doesn't exist or failed to load.
     */
    private int getOrLoadImage(long vg, String localPath)
    {
        if (localPath == null || localPath.isEmpty()) return -1;

        Integer cached = this.imageCache.get(localPath);
        if (cached != null) return cached;

        // Check file exists before attempting load
        File file = new File(localPath);
        if (!file.isFile())
        {
            this.imageCache.put(localPath, -1);
            return -1;
        }

        int handle = nvgCreateImage(vg, localPath, NVG_IMAGE_GENERATE_MIPMAPS);
        if (handle <= 0) handle = -1;
        this.imageCache.put(localPath, handle);
        return handle;
    }
    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       ChatStore store, String activeConvId, int mx, int my)
    {
        if (r == null) return;
        this.hitBoxes.clear();
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.mainArgb()));

        NanoUi.beginClip(vg, r.x, r.y, r.w, r.h);

        List<ChatMessage> msgs = store.getMessages(activeConvId);
        if (msgs.isEmpty())
        {
            NanoRenderUtils.drawLabel(vg, stack, NanoFontBook.uiRegular(),
                    r.x + r.w * 0.5F, r.y + r.h * 0.5F, 13.0F,
                    "No messages yet", theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            NanoUi.endClip(vg);
            return;
        }

        this.scrollOffset += (this.scrollTarget - this.scrollOffset) * 0.35F;

        float bubbleMaxW = r.w * 0.65F;
        float bubbleRadius = theme.cardRadius();
        float y = r.y + PAD - this.scrollOffset;
        int fontRegular = NanoFontBook.uiRegular();
        int fontBold = NanoFontBook.uiBold();
        String localId = store.getLocalUser() != null ? store.getLocalUser().getId() : "local";

        for (int i = 0; i < msgs.size(); i++)
        {
            ChatMessage msg = msgs.get(i);
            boolean isLocal = localId.equals(msg.getSenderId());

            if (msg.getType() == MessageType.SYSTEM)
            {
                String dt = msg.getDisplayText();
                if (dt != null)
                {
                    NanoRenderUtils.drawLabel(vg, stack, fontRegular, r.x + r.w * 0.5F,
                            y + 10.0F, SMALL_FONT, dt, theme.textWeakArgb(),
                            NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);
                }
                y += 28.0F;
                continue;
            }

            ChatUser sender = store.getUser(msg.getSenderId());
            String senderName = sender != null ? sender.getNickname() : msg.getSenderId();
            String time = IRCRenderUtils.formatTime(msg.getTimestampMs());

            if (msg.getType() == MessageType.IMAGE)
            {
                y = renderImageBubble(vg, stack, theme, r, y, bubbleRadius,
                        fontRegular, fontBold, senderName, time, msg, isLocal);
            }
            else if (msg.getType() == MessageType.VOICE)
            {
                y = renderVoiceBubble(vg, stack, theme, r, y, bubbleRadius,
                        fontRegular, fontBold, senderName, time, msg, isLocal);
            }
            else
            {
                y = renderTextBubble(vg, stack, theme, r, y, bubbleMaxW, bubbleRadius,
                        fontRegular, fontBold, senderName, time, msg, isLocal);
            }
        }

        float contentHeight = y - (r.y + PAD - this.scrollOffset);
        float maxScroll = Math.max(0.0F, contentHeight - r.h);

        // Handle pending scroll-to-bottom: snap directly to maxScroll
        if (this.pendingScrollToBottom)
        {
            this.scrollTarget = maxScroll;
            this.scrollOffset = maxScroll;
            this.pendingScrollToBottom = false;
        }

        this.scrollTarget = Math.min(this.scrollTarget, maxScroll);
        if (this.scrollTarget < 0.0F) this.scrollTarget = 0.0F;

        NanoUi.endClip(vg);
    }
    // --- Word-wrapped text bubble ---

    private float renderTextBubble(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                   float y, float bubbleMaxW, float bubbleRadius,
                                   int fontRegular, int fontBold,
                                   String senderName, String time, ChatMessage msg, boolean isLocal)
    {
        String text = msg.getDisplayText() != null ? msg.getDisplayText() : "";
        float maxTextW = bubbleMaxW - BUBBLE_PAD * 2.0F;

        // Word-wrap into lines
        List<String> lines = wrapText(vg, fontRegular, FONT_SIZE, text, maxTextW);
        float longestLine = 0.0F;
        for (String line : lines)
        {
            float lw = NanoRenderUtils.textWidth(vg, fontRegular, FONT_SIZE, line);
            if (lw > longestLine) longestLine = lw;
        }

        float bubbleW = Math.min(bubbleMaxW, longestLine + BUBBLE_PAD * 2.0F);
        float textBlockH = lines.size() * FONT_SIZE + (lines.size() - 1) * LINE_SPACING;
        float bubbleH = textBlockH + BUBBLE_PAD * 2.0F;

        int bubbleBg = isLocal ? theme.accentSoftArgb() : theme.cardArgb();
        int avatarBg = isLocal ? theme.accentArgb() : theme.controlArgb();
        int avatarFg = isLocal ? 0xFFFFFFFF : theme.textArgb();

        y = drawBubbleFrame(vg, stack, theme, r, y, bubbleW, bubbleH, bubbleRadius,
                fontRegular, fontBold, senderName, time, avatarBg, avatarFg, bubbleBg, isLocal, msg);

        // Draw wrapped text lines inside bubble
        float bubbleX = isLocal
                ? r.x + r.w - PAD - AVATAR_SIZE - 6.0F - bubbleW
                : r.x + PAD + AVATAR_SIZE + 6.0F;
        float bubbleTopY = y - Math.max(AVATAR_SIZE, bubbleH) - 8.0F;

        for (int li = 0; li < lines.size(); li++)
        {
            float lineY = bubbleTopY + BUBBLE_PAD + li * (FONT_SIZE + LINE_SPACING);
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, bubbleX + BUBBLE_PAD, lineY,
                    FONT_SIZE, lines.get(li), theme.textArgb(),
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
        }

        return y;
    }
    // --- Image thumbnail bubble ---

    private float renderImageBubble(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                    float y, float bubbleRadius,
                                    int fontRegular, int fontBold,
                                    String senderName, String time, ChatMessage msg, boolean isLocal)
    {
        ChatAttachment att = msg.getAttachment();
        String label = att != null && att.getFileName() != null ? att.getFileName() : "[Image]";

        // Try to load the actual image
        String localPath = att != null ? att.getLocalPath() : null;
        // Fallback: try shared image folder if localPath is missing
        if ((localPath == null || localPath.isEmpty()) && att != null && att.getRemoteUrl() != null)
        {
            String fileName = att.getRemoteUrl();
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) fileName = fileName.substring(slash + 1);
            File shared = new File("config/client/irc-images", fileName);
            if (shared.isFile()) localPath = shared.getAbsolutePath();
        }
        int imgHandle = getOrLoadImage(vg, localPath);

        // Determine thumbnail dimensions
        float thumbW = IMAGE_MAX_W;
        float thumbH = IMAGE_MAX_H;

        if (imgHandle > 0)
        {
            // Get actual image dimensions from NanoVG
            int[] iw = new int[1];
            int[] ih = new int[1];
            nvgImageSize(vg, imgHandle, iw, ih);
            if (iw[0] > 0 && ih[0] > 0)
            {
                float scale = Math.min(IMAGE_MAX_W / iw[0], IMAGE_MAX_H / ih[0]);
                if (scale > 1.0F) scale = 1.0F; // don't upscale
                thumbW = iw[0] * scale;
                thumbH = ih[0] * scale;
            }
        }
        else if (att != null && att.getImageWidth() > 0 && att.getImageHeight() > 0)
        {
            float scale = Math.min(IMAGE_MAX_W / att.getImageWidth(), IMAGE_MAX_H / att.getImageHeight());
            thumbW = att.getImageWidth() * scale;
            thumbH = att.getImageHeight() * scale;
        }

        float bubbleW = thumbW + BUBBLE_PAD * 2.0F;
        float bubbleH = thumbH + FONT_SIZE + LINE_SPACING + BUBBLE_PAD * 2.0F;

        int bubbleBg = isLocal ? theme.accentSoftArgb() : theme.cardArgb();
        int avatarBg = isLocal ? theme.accentArgb() : theme.controlArgb();
        int avatarFg = isLocal ? 0xFFFFFFFF : theme.textArgb();

        y = drawBubbleFrame(vg, stack, theme, r, y, bubbleW, bubbleH, bubbleRadius,
                fontRegular, fontBold, senderName, time, avatarBg, avatarFg, bubbleBg, isLocal, msg);

        float bubbleX = isLocal
                ? r.x + r.w - PAD - AVATAR_SIZE - 6.0F - bubbleW
                : r.x + PAD + AVATAR_SIZE + 6.0F;
        float bubbleTopY = y - Math.max(AVATAR_SIZE, bubbleH) - 8.0F;

        float imgX = bubbleX + BUBBLE_PAD;
        float imgY = bubbleTopY + BUBBLE_PAD;

        if (imgHandle > 0)
        {
            // Render actual image with rounded corners
            NVGPaint imgPaint = NVGPaint.mallocStack(stack);
            nvgImagePattern(vg, imgX, imgY, thumbW, thumbH, 0.0F, imgHandle, 1.0F, imgPaint);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, imgX, imgY, thumbW, thumbH, 4.0F);
            nvgFillPaint(vg, imgPaint);
            nvgFill(vg);
        }
        else
        {
            // Placeholder for images we can't load (received from others, no local file)
            NanoRenderUtils.fillRoundedRect(vg, imgX, imgY, thumbW, thumbH, 4.0F,
                    NanoRenderUtils.argb(stack, NanoRenderUtils.withAlpha(theme.controlArgb(), 180)));
            // Image icon placeholder
            NanoRenderUtils.drawLabel(vg, stack, fontBold, imgX + thumbW * 0.5F, imgY + thumbH * 0.5F - 8.0F,
                    24.0F, "\u25A3", theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, imgX + thumbW * 0.5F, imgY + thumbH * 0.5F + 14.0F,
                    11.0F, label, theme.textMutedArgb(),
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }

        // File name label below thumbnail
        NanoRenderUtils.drawLabel(vg, stack, fontRegular, imgX, imgY + thumbH + LINE_SPACING,
                SMALL_FONT, label, theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);

        return y;
    }
    // --- Voice playback bar bubble ---

    private float renderVoiceBubble(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                    float y, float bubbleRadius,
                                    int fontRegular, int fontBold,
                                    String senderName, String time, ChatMessage msg, boolean isLocal)
    {
        ChatAttachment att = msg.getAttachment();
        int durationSec = att != null ? att.getDurationMs() / 1000 : 0;
        String durLabel = durationSec + "s";

        float bubbleW = VOICE_BAR_W + BUBBLE_PAD * 2.0F;
        float voiceBubbleH = VOICE_BAR_H + BUBBLE_PAD * 2.0F;

        int bubbleBg = isLocal ? theme.accentSoftArgb() : theme.cardArgb();
        int avatarBg = isLocal ? theme.accentArgb() : theme.controlArgb();
        int avatarFg = isLocal ? 0xFFFFFFFF : theme.textArgb();

        y = drawBubbleFrame(vg, stack, theme, r, y, bubbleW, voiceBubbleH, bubbleRadius,
                fontRegular, fontBold, senderName, time, avatarBg, avatarFg, bubbleBg, isLocal, msg);

        float bubbleX = isLocal
                ? r.x + r.w - PAD - AVATAR_SIZE - 6.0F - bubbleW
                : r.x + PAD + AVATAR_SIZE + 6.0F;
        float bubbleTopY = y - Math.max(AVATAR_SIZE, voiceBubbleH) - 8.0F;

        float barX = bubbleX + BUBBLE_PAD;
        float barY = bubbleTopY + BUBBLE_PAD;

        // Play button circle
        float btnR = VOICE_BAR_H * 0.4F;
        float btnCx = barX + btnR + 2.0F;
        float btnCy = barY + VOICE_BAR_H * 0.5F;
        NanoRenderUtils.fillRoundedRect(vg, btnCx - btnR, btnCy - btnR, btnR * 2.0F, btnR * 2.0F,
                btnR, NanoRenderUtils.argb(stack, theme.accentArgb()));
        // Play triangle
        NanoRenderUtils.drawLabel(vg, stack, fontBold, btnCx + 1.0F, btnCy, 11.0F,
                ">", 0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Waveform placeholder (horizontal bars)
        float waveX = btnCx + btnR + 8.0F;
        float waveW = VOICE_BAR_W - btnR * 2.0F - 12.0F - 30.0F;
        float waveY = barY + VOICE_BAR_H * 0.5F;
        int barColor = NanoRenderUtils.withAlpha(theme.textMutedArgb(), 120);
        for (int b = 0; b < 16; b++)
        {
            float bx = waveX + b * (waveW / 16.0F);
            float bh = 4.0F + (float)(Math.sin(b * 0.8) * 6.0F + 6.0F);
            NanoRenderUtils.fillRoundedRect(vg, bx, waveY - bh * 0.5F, 2.0F, bh, 1.0F,
                    NanoRenderUtils.argb(stack, barColor));
        }

        // Duration label
        NanoRenderUtils.drawLabel(vg, stack, fontRegular,
                barX + VOICE_BAR_W - 4.0F, barY + VOICE_BAR_H * 0.5F,
                SMALL_FONT, durLabel, theme.textMutedArgb(),
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

        return y;
    }
    // --- Shared bubble frame (header + avatar + background rect) ---

    /**
     * Draws the header line, avatar, and bubble background. Returns the new Y after this bubble.
     */
    private float drawBubbleFrame(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                                  float y, float bubbleW, float bubbleH, float bubbleRadius,
                                  int fontRegular, int fontBold,
                                  String senderName, String time,
                                  int avatarBg, int avatarFg, int bubbleBg, boolean isLocal,
                                  ChatMessage msg)
    {
        if (isLocal)
        {
            float avatarX = r.x + r.w - PAD - AVATAR_SIZE;
            float bubbleX = r.x + r.w - PAD - AVATAR_SIZE - 6.0F - bubbleW;

            String header = time + "  " + senderName;
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, avatarX + AVATAR_SIZE, y, SMALL_FONT,
                    header, theme.textMutedArgb(), NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP);

            float avatarY = y + SMALL_FONT + 4.0F;
            NanoRenderUtils.fillRoundedRect(vg, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    AVATAR_SIZE * 0.5F, NanoRenderUtils.argb(stack, avatarBg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold, avatarX + AVATAR_SIZE * 0.5F,
                    avatarY + AVATAR_SIZE * 0.5F, 11.0F,
                    senderName.substring(0, 1).toUpperCase(), avatarFg,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            NanoRenderUtils.fillRoundedRect(vg, bubbleX, avatarY, bubbleW, bubbleH, bubbleRadius,
                    NanoRenderUtils.argb(stack, bubbleBg));
            cacheHitBox(msg, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    bubbleX, avatarY, bubbleW, bubbleH);
        }
        else
        {
            float avatarX = r.x + PAD;
            float bubbleX = avatarX + AVATAR_SIZE + 6.0F;

            String header = senderName + "  " + time;
            NanoRenderUtils.drawLabel(vg, stack, fontRegular, bubbleX, y, SMALL_FONT,
                    header, theme.textMutedArgb(), NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);

            float avatarY = y + SMALL_FONT + 4.0F;
            NanoRenderUtils.fillRoundedRect(vg, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    AVATAR_SIZE * 0.5F, NanoRenderUtils.argb(stack, avatarBg));
            NanoRenderUtils.drawLabel(vg, stack, fontBold, avatarX + AVATAR_SIZE * 0.5F,
                    avatarY + AVATAR_SIZE * 0.5F, 11.0F,
                    senderName.substring(0, 1).toUpperCase(), avatarFg,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            NanoRenderUtils.fillRoundedRect(vg, bubbleX, avatarY, bubbleW, bubbleH, bubbleRadius,
                    NanoRenderUtils.argb(stack, bubbleBg));
            cacheHitBox(msg, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE,
                    bubbleX, avatarY, bubbleW, bubbleH);
        }

        return y + SMALL_FONT + 4.0F + Math.max(AVATAR_SIZE, bubbleH) + 8.0F;
    }

    private void cacheHitBox(ChatMessage msg,
                             float avatarX, float avatarY, float avatarW, float avatarH,
                             float bubbleX, float bubbleY, float bubbleW, float bubbleH)
    {
        if (msg == null || msg.getId() == null) return;
        this.hitBoxes.add(new MessageHitBox(
                msg.getId(), msg.getSenderId(),
                avatarX, avatarY, avatarW, avatarH,
                bubbleX, bubbleY, bubbleW, bubbleH));
    }

    /** Returns senderId when mouse is over an avatar, else null. */
    public String hitTestAvatarSenderId(int mx, int my)
    {
        for (int i = this.hitBoxes.size() - 1; i >= 0; i--)
        {
            MessageHitBox hb = this.hitBoxes.get(i);
            if (mx >= hb.avatarX && mx <= hb.avatarX + hb.avatarW
                    && my >= hb.avatarY && my <= hb.avatarY + hb.avatarH)
            {
                return hb.senderId;
            }
        }
        return null;
    }

    /** Returns message id when mouse is over a bubble, else null. */
    public String hitTestMessageId(int mx, int my)
    {
        for (int i = this.hitBoxes.size() - 1; i >= 0; i--)
        {
            MessageHitBox hb = this.hitBoxes.get(i);
            if (mx >= hb.bubbleX && mx <= hb.bubbleX + hb.bubbleW
                    && my >= hb.bubbleY && my <= hb.bubbleY + hb.bubbleH)
            {
                return hb.messageId;
            }
        }
        return null;
    }

    private static final class MessageHitBox
    {
        final String messageId;
        final String senderId;
        final float avatarX;
        final float avatarY;
        final float avatarW;
        final float avatarH;
        final float bubbleX;
        final float bubbleY;
        final float bubbleW;
        final float bubbleH;

        private MessageHitBox(String messageId, String senderId,
                              float avatarX, float avatarY, float avatarW, float avatarH,
                              float bubbleX, float bubbleY, float bubbleW, float bubbleH)
        {
            this.messageId = messageId;
            this.senderId = senderId;
            this.avatarX = avatarX;
            this.avatarY = avatarY;
            this.avatarW = avatarW;
            this.avatarH = avatarH;
            this.bubbleX = bubbleX;
            this.bubbleY = bubbleY;
            this.bubbleW = bubbleW;
            this.bubbleH = bubbleH;
        }
    }
    // --- Word-wrap utility ---

    /**
     * Breaks text into lines that fit within maxWidth.
     * Respects explicit newlines and wraps at word boundaries.
     */
    private static List<String> wrapText(long vg, int fontId, float fontSize, String text, float maxWidth)
    {
        List<String> result = new ArrayList<String>();
        if (text == null || text.isEmpty()) { result.add(""); return result; }

        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs)
        {
            if (para.isEmpty()) { result.add(""); continue; }

            float paraW = NanoRenderUtils.textWidth(vg, fontId, fontSize, para);
            if (paraW <= maxWidth)
            {
                result.add(para);
                continue;
            }

            // Word-level wrapping
            StringBuilder line = new StringBuilder();
            float lineW = 0.0F;
            int i = 0;
            while (i < para.length())
            {
                // Find next word boundary
                int wordEnd = i;
                while (wordEnd < para.length() && para.charAt(wordEnd) != ' ') wordEnd++;
                String word = para.substring(i, wordEnd);
                String space = line.length() > 0 ? " " : "";
                float wordW = NanoRenderUtils.textWidth(vg, fontId, fontSize, space + word);

                if (lineW + wordW > maxWidth && line.length() > 0)
                {
                    result.add(line.toString());
                    line.setLength(0);
                    lineW = 0.0F;
                    space = "";
                    wordW = NanoRenderUtils.textWidth(vg, fontId, fontSize, word);
                }

                // If single word exceeds maxWidth, break by character
                if (wordW > maxWidth && line.length() == 0)
                {
                    for (int c = 0; c < word.length(); c++)
                    {
                        String ch = String.valueOf(word.charAt(c));
                        float chW = NanoRenderUtils.textWidth(vg, fontId, fontSize, ch);
                        if (lineW + chW > maxWidth && line.length() > 0)
                        {
                            result.add(line.toString());
                            line.setLength(0);
                            lineW = 0.0F;
                        }
                        line.append(ch);
                        lineW += chW;
                    }
                }
                else
                {
                    line.append(space).append(word);
                    lineW += wordW;
                }

                i = wordEnd;
                while (i < para.length() && para.charAt(i) == ' ') i++;
            }
            if (line.length() > 0) result.add(line.toString());
        }

        if (result.isEmpty()) result.add("");
        return result;
    }
}
