package client.hud;

import client.render.NanoVGContext;

/**
 * Shared layout math for HUD render and HUD edit overlay.
 */
public final class HudLayoutMath
{
    private static final float FALLBACK_WIDTH = 120.0F;
    private static final float FALLBACK_HEIGHT = 18.0F;

    private HudLayoutMath()
    {
    }

    public static void applyTransform(NanoVGContext nano, HudElement element, float screenW, float screenH)
    {
        if (nano == null || element == null)
        {
            return;
        }

        HudTransform transform = element.getTransform();

        if (transform == null)
        {
            return;
        }

        Bounds local = localBounds(element);
        float pivotX = resolvePivotX(transform.getAnchor(), transform.getDock(), local.w());
        float pivotY = resolvePivotY(transform.getAnchor(), transform.getDock(), local.h());
        float x = resolveAnchorBaseX(transform.getAnchor(), screenW) + transform.getOffsetX();
        float y = resolveAnchorBaseY(transform.getAnchor(), screenH) + transform.getOffsetY();
        float scale = Math.max(0.1F, transform.getScale());

        nano.translate(x, y);
        nano.scale(scale, scale);
        nano.translate(-pivotX, -pivotY);
    }

    public static Bounds resolveScreenBounds(HudElement element, float screenW, float screenH)
    {
        if (element == null)
        {
            return null;
        }

        HudTransform transform = element.getTransform();

        if (transform == null)
        {
            return null;
        }

        Bounds local = localBounds(element);
        float scale = Math.max(0.1F, transform.getScale());
        float pivotX = resolvePivotX(transform.getAnchor(), transform.getDock(), local.w());
        float pivotY = resolvePivotY(transform.getAnchor(), transform.getDock(), local.h());
        float x = resolveAnchorBaseX(transform.getAnchor(), screenW) + transform.getOffsetX() + (local.x() - pivotX) * scale;
        float y = resolveAnchorBaseY(transform.getAnchor(), screenH) + transform.getOffsetY() + (local.y() - pivotY) * scale;
        return new Bounds(x, y, local.w() * scale, local.h() * scale);
    }

    public static Bounds localBounds(HudElement element)
    {
        if (element instanceof HudFpsElement)
        {
            String text = ((HudFpsElement)element).displayText();
            float width = Math.max(36.0F, (float)text.length() * 8.4F);
            return new Bounds(0.0F, 0.0F, width, 16.0F);
        }

        return new Bounds(0.0F, 0.0F, FALLBACK_WIDTH, FALLBACK_HEIGHT);
    }

    private static float resolveAnchorBaseX(Anchor anchor, float screenW)
    {
        if (anchor == null)
        {
            return 0.0F;
        }

        switch (anchor)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return screenW;
            case CENTER:
                return screenW * 0.5F;
            case TOP_LEFT:
            case BOTTOM_LEFT:
            default:
                return 0.0F;
        }
    }

    private static float resolveAnchorBaseY(Anchor anchor, float screenH)
    {
        if (anchor == null)
        {
            return 0.0F;
        }

        switch (anchor)
        {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return screenH;
            case CENTER:
                return screenH * 0.5F;
            case TOP_LEFT:
            case TOP_RIGHT:
            default:
                return 0.0F;
        }
    }

    private static float resolvePivotX(Anchor anchor, Dock dock, float width)
    {
        switch (dock)
        {
            case LEFT:
                return 0.0F;
            case RIGHT:
                return width;
            case CENTER:
                return width * 0.5F;
            case TOP:
            case BOTTOM:
            case NONE:
            default:
                break;
        }

        switch (anchor)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return width;
            case CENTER:
                return width * 0.5F;
            case TOP_LEFT:
            case BOTTOM_LEFT:
            default:
                return 0.0F;
        }
    }

    private static float resolvePivotY(Anchor anchor, Dock dock, float height)
    {
        switch (dock)
        {
            case TOP:
                return 0.0F;
            case BOTTOM:
                return height;
            case CENTER:
                return height * 0.5F;
            case LEFT:
            case RIGHT:
            case NONE:
            default:
                break;
        }

        switch (anchor)
        {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return height;
            case CENTER:
                return height * 0.5F;
            case TOP_LEFT:
            case TOP_RIGHT:
            default:
                return 0.0F;
        }
    }

    public static final class Bounds
    {
        private final float x;
        private final float y;
        private final float w;
        private final float h;

        private Bounds(float x, float y, float w, float h)
        {
            this.x = x;
            this.y = y;
            this.w = Math.max(0.0F, w);
            this.h = Math.max(0.0F, h);
        }

        public float x()
        {
            return this.x;
        }

        public float y()
        {
            return this.y;
        }

        public float w()
        {
            return this.w;
        }

        public float h()
        {
            return this.h;
        }
    }
}
