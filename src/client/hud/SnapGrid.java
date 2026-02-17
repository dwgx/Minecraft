package client.hud;

public final class SnapGrid
{
    private final int step;

    public SnapGrid(int step)
    {
        this.step = step <= 1 ? 1 : step;
    }

    public int getStep()
    {
        return this.step;
    }

    public float snap(float value)
    {
        return Math.round(value / (float)this.step) * this.step;
    }
}
