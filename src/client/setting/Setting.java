package client.setting;

/**
 * 设置基类，模块配置与 UI 控件共用这套模型。
 */
public abstract class Setting<T>
{
    private final String key;
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private Visibility visibility;

    protected Setting(String key, String name, String description, T defaultValue)
    {
        this.key = key;
        this.name = name;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visibility = Visibility.ALWAYS;
    }

    public String getKey()
    {
        return this.key;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    public T get()
    {
        return this.value;
    }

    public void set(T value)
    {
        this.value = this.normalize(value);
    }

    public boolean isVisible()
    {
        return this.visibility == null || this.visibility.isVisible();
    }

    public Setting<T> visibleWhen(Visibility visibility)
    {
        this.visibility = visibility == null ? Visibility.ALWAYS : visibility;
        return this;
    }

    public void reset()
    {
        this.value = this.defaultValue;
    }

    protected T normalize(T value)
    {
        return value;
    }
}
