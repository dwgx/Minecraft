package client.setting;

public final class FloatSetting extends Setting<Float>
{
    private final float min;
    private final float max;
    private final float step;

    public FloatSetting(String key, String name, String description, float defaultValue, float min, float max, float step)
    {
        super(key, name, description, Float.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.step = step <= 0.0F ? 0.01F : step;
        this.set(Float.valueOf(defaultValue));
    }

    protected Float normalize(Float value)
    {
        if (value == null)
        {
            return this.getDefaultValue();
        }

        float v = value.floatValue();
        v = Math.max(this.min, Math.min(this.max, v));
        v = this.min + Math.round((v - this.min) / this.step) * this.step;
        v = Math.max(this.min, Math.min(this.max, v));
        return Float.valueOf(v);
    }

    public float getMin()
    {
        return this.min;
    }

    public float getMax()
    {
        return this.max;
    }

    public float getStep()
    {
        return this.step;
    }
}
