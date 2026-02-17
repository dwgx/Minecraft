package client.ui;

public final class Animation
{
    private float value;
    private float target;
    private float speed = 12.0F;

    public float getValue()
    {
        return this.value;
    }

    public void setValue(float value)
    {
        this.value = value;
    }

    public float getTarget()
    {
        return this.target;
    }

    public void setTarget(float target)
    {
        this.target = target;
    }

    public float getSpeed()
    {
        return this.speed;
    }

    public void setSpeed(float speed)
    {
        this.speed = Math.max(0.1F, speed);
    }

    public void update(float deltaSeconds)
    {
        float t = Math.max(0.0F, Math.min(1.0F, this.speed * deltaSeconds));
        this.value = this.value + (this.target - this.value) * t;
    }
}
