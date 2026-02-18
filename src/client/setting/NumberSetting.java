package client.setting;

import java.text.DecimalFormat;

public final class NumberSetting extends Setting<Double>
{
    private final double min;
    private final double max;
    private final double step;
    private final DecimalFormat format;

    public NumberSetting(String key, String name, String description, double defaultValue, double min, double max, double step, String formatPattern)
    {
        super(key, name, description, Double.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 1.0D : step;
        this.format = new DecimalFormat(formatPattern == null || formatPattern.isEmpty() ? "0.00" : formatPattern);
        this.set(Double.valueOf(defaultValue));
    }

    protected Double normalize(Double value)
    {
        if (value == null)
        {
            return this.getDefaultValue();
        }

        double v = value.doubleValue();
        v = Math.max(this.min, Math.min(this.max, v));
        v = this.min + Math.round((v - this.min) / this.step) * this.step;
        v = Math.max(this.min, Math.min(this.max, v));
        return Double.valueOf(v);
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public double getStep()
    {
        return this.step;
    }

    public String format()
    {
        return this.format.format(this.get().doubleValue());
    }
}
