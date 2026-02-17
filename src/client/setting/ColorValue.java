package client.setting;

public final class ColorValue
{
    private final int r;
    private final int g;
    private final int b;
    private final int a;
    private final boolean rainbow;

    public ColorValue(int r, int g, int b, int a, boolean rainbow)
    {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
        this.rainbow = rainbow;
    }

    public int getR()
    {
        return this.r;
    }

    public int getG()
    {
        return this.g;
    }

    public int getB()
    {
        return this.b;
    }

    public int getA()
    {
        return this.a;
    }

    public boolean isRainbow()
    {
        return this.rainbow;
    }

    public int toArgb()
    {
        return (this.a & 255) << 24 | (this.r & 255) << 16 | (this.g & 255) << 8 | this.b & 255;
    }

    private static int clamp(int value)
    {
        return Math.max(0, Math.min(255, value));
    }
}
