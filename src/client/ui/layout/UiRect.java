package client.ui.layout;

/**
 * Shared immutable rectangle used by all Nano UI screens.
 * Replaces duplicated private Rect classes in ClickGuiScreen, ClientSettingsScreen, UIScaleEditScreen.
 */
public final class UiRect
{
    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public UiRect(float x, float y, float w, float h)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float x2()
    {
        return this.x + this.w;
    }

    public float y2()
    {
        return this.y + this.h;
    }

    public boolean contains(float mx, float my)
    {
        return mx >= this.x && mx <= this.x + this.w
            && my >= this.y && my <= this.y + this.h;
    }

    public boolean contains(int mx, int my)
    {
        return this.contains((float) mx, (float) my);
    }

    public UiRect withX(float newX)
    {
        return new UiRect(newX, this.y, this.w, this.h);
    }

    public UiRect withY(float newY)
    {
        return new UiRect(this.x, newY, this.w, this.h);
    }

    public UiRect withW(float newW)
    {
        return new UiRect(this.x, this.y, newW, this.h);
    }

    public UiRect withH(float newH)
    {
        return new UiRect(this.x, this.y, this.w, newH);
    }

    public UiRect inset(float pad)
    {
        return new UiRect(this.x + pad, this.y + pad, this.w - pad * 2.0F, this.h - pad * 2.0F);
    }
}
