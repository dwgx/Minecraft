package client.hud;

public final class SnapLine
{
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;

    public SnapLine(float x1, float y1, float x2, float y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public float getX1()
    {
        return this.x1;
    }

    public float getY1()
    {
        return this.y1;
    }

    public float getX2()
    {
        return this.x2;
    }

    public float getY2()
    {
        return this.y2;
    }
}
