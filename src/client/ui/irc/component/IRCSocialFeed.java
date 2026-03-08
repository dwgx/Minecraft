package client.ui.irc.component;

import client.chat.model.SocialPost;
import client.ui.layout.UiRect;
import client.ui.template.UiAnimProfile;
import client.ui.template.UiAnimationBus;
import dwgx.nano.NanoFontBook;
import dwgx.nano.NanoRenderUtils;
import client.ui.template.NanoScreenKit;
import dwgx.nano.NanoTheme;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Social feed panel showing posts in card layout.
 */
public final class IRCSocialFeed
{
    private static final String ANIM = "irc.social.";
    private static final float CARD_PAD = 10.0F;
    private static final float CARD_GAP = 8.0F;

    private UiAnimProfile animProfile;
    private float scrollOffset;
    private int hoveredPostIndex = -1;
    private int likeHoverIndex = -1;
    private int commentHoverIndex = -1;

    public void setAnimProfile(UiAnimProfile profile) { this.animProfile = profile; }
    public int getHoveredPostIndex() { return this.hoveredPostIndex; }
    public int getLikeHoverIndex() { return this.likeHoverIndex; }
    public int getCommentHoverIndex() { return this.commentHoverIndex; }

    public void render(long vg, MemoryStack stack, NanoTheme theme, UiRect r,
                       List<SocialPost> posts, int mx, int my)
    {
        if (r == null) return;
        NanoRenderUtils.fillRoundedRect(vg, r.x, r.y, r.w, r.h, theme.surfaceRadius(),
                NanoRenderUtils.argb(stack, theme.surfaceArgb()));

        this.hoveredPostIndex = -1;
        this.likeHoverIndex = -1;
        this.commentHoverIndex = -1;

        if (posts == null || posts.isEmpty()) return;

        int fontBold = NanoFontBook.uiBold();
        int fontNormal = NanoFontBook.uiRegular();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
        float y = r.y + CARD_PAD - this.scrollOffset;
        float cardW = r.w - CARD_PAD * 2;

        for (int i = 0; i < posts.size(); i++)
        {
            SocialPost post = posts.get(i);
            float cardH = estimateCardHeight(post);

            if (y + cardH < r.y || y > r.y + r.h) { y += cardH + CARD_GAP; continue; }

            boolean cardHover = mx >= r.x + CARD_PAD && mx <= r.x + CARD_PAD + cardW
                    && my >= y && my < y + cardH;
            if (cardHover) this.hoveredPostIndex = i;

            // Card background
            float hoverT = UiAnimationBus.animateControl(ANIM + i + ".h", cardHover ? 1.0F : 0.0F, this.animProfile);
            int cardBg = NanoScreenKit.mixArgb(theme.panelArgb(), theme.controlHoverArgb(), hoverT * 0.3F);
            NanoRenderUtils.fillRoundedRect(vg, r.x + CARD_PAD, y, cardW, cardH, 6.0F,
                    NanoRenderUtils.argb(stack, cardBg));

            float cx = r.x + CARD_PAD + 10;
            float cy = y + 8;

            // Author + date
            NanoVG.nvgFontFaceId(vg, fontBold);
            NanoVG.nvgFontSize(vg, 13.0F);
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
            NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            NanoVG.nvgText(vg, cx, cy, post.getAuthorNick());

            NanoVG.nvgFontFaceId(vg, fontNormal);
            NanoVG.nvgFontSize(vg, 11.0F);
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textDimArgb()));
            NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP);
            NanoVG.nvgText(vg, r.x + CARD_PAD + cardW - 10, cy, sdf.format(new Date(post.getCreatedMs())));
            cy += 20;

            // Content
            NanoVG.nvgFontFaceId(vg, fontNormal);
            NanoVG.nvgFontSize(vg, 13.0F);
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, theme.textArgb()));
            NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
            NanoVG.nvgTextBox(vg, cx, cy, cardW - 20, post.getContent());
            cy += 40;

            // Action bar: Like | Comment
            float btnY = y + cardH - 28;
            float likeX = cx;
            float likeW = 60.0F;
            boolean likeHover = mx >= likeX && mx <= likeX + likeW && my >= btnY && my <= btnY + 22;
            if (likeHover) this.likeHoverIndex = i;

            int likeColor = post.isLikedByMe() ? 0xFFED4245 : theme.textDimArgb();
            if (likeHover) likeColor = 0xFFED4245;
            NanoVG.nvgFontFaceId(vg, fontNormal);
            NanoVG.nvgFontSize(vg, 12.0F);
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, likeColor));
            NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            String likeText = (post.isLikedByMe() ? "<3 " : "Like ") + post.getLikeCount();
            NanoVG.nvgText(vg, likeX, btnY + 11, likeText);

            float commentX = likeX + likeW + 12;
            float commentW = 80.0F;
            boolean cmtHover = mx >= commentX && mx <= commentX + commentW && my >= btnY && my <= btnY + 22;
            if (cmtHover) this.commentHoverIndex = i;

            int cmtColor = cmtHover ? theme.accentArgb() : theme.textDimArgb();
            NanoVG.nvgFillColor(vg, NanoRenderUtils.argb(stack, cmtColor));
            NanoVG.nvgText(vg, commentX, btnY + 11, "Comments");

            y += cardH + CARD_GAP;
        }
    }

    private float estimateCardHeight(SocialPost post)
    {
        float base = 80.0F; // header + action bar
        if (post.getContent() != null && post.getContent().length() > 80) base += 20.0F;
        return base;
    }

    public void scroll(float delta) { this.scrollOffset = Math.max(0, this.scrollOffset - delta * 20.0F); }
}