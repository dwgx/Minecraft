package client.setting;

public final class IntSetting extends Setting<Integer>
{
    private final int min;
    private final int max;
    private final int step;

    public IntSetting(String key, String name, String description, int defaultValue, int min, int max, int step)
    {
        super(key, name, description, Integer.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 1 : step;
        this.set(Integer.valueOf(defaultValue));
    }

    protected Integer normalize(Integer value)
    {
        if (value == null)
        {
            return this.getDefaultValue();
        }

        int v = value.intValue();
        v = Math.max(this.min, Math.min(this.max, v));
        v = this.min + Math.round((float)(v - this.min) / (float)this.step) * this.step;
        v = Math.max(this.min, Math.min(this.max, v));
        return Integer.valueOf(v);
    }

    public int getMin()
    {
        return this.min;
    }

    public int getMax()
    {
        return this.max;
    }

    public int getStep()
    {
        return this.step;
    }
}
