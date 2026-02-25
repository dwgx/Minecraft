package client.ui.template;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Immutable animation profile shared by Nano UI surfaces.
 * Supports JSON serialization for persistence in client.json.
 */
public final class UiAnimProfile
{
    private final boolean enabled;
    private final float windowSpeed;
    private final float controlSpeed;
    private final float sliderSpeed;
    private final float smooth;
    private final UiAnimation.Type type;

    public UiAnimProfile(boolean enabled, float windowSpeed, float controlSpeed, float sliderSpeed, float smooth, UiAnimation.Type type)
    {
        this.enabled = enabled;
        this.windowSpeed = UiMotion.clamp(windowSpeed, 0.05F, 1.0F);
        this.controlSpeed = UiMotion.clamp(controlSpeed, 0.05F, 1.0F);
        this.sliderSpeed = UiMotion.clamp(sliderSpeed, 0.05F, 1.0F);
        this.smooth = UiMotion.clamp(smooth, 0.0F, 1.0F);
        this.type = type == null ? UiAnimation.Type.EASE_OUT : type;
    }

    public static UiAnimProfile defaults()
    {
        return new UiAnimProfile(true, 0.56F, 0.58F, 0.62F, 0.62F, UiAnimation.Type.EASE_OUT);
    }

    public boolean isEnabled() { return this.enabled; }
    public float windowSpeed() { return this.windowSpeed; }
    public float controlSpeed() { return this.controlSpeed; }
    public float sliderSpeed() { return this.sliderSpeed; }
    public float smooth() { return this.smooth; }
    public UiAnimation.Type type() { return this.type; }

    // --- Serialization ---

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("windowSpeed", new JsonPrimitive(this.windowSpeed));
        obj.add("controlSpeed", new JsonPrimitive(this.controlSpeed));
        obj.add("sliderSpeed", new JsonPrimitive(this.sliderSpeed));
        obj.add("smooth", new JsonPrimitive(this.smooth));
        obj.add("type", new JsonPrimitive(this.type.name()));
        return obj;
    }

    public static UiAnimProfile fromJson(JsonObject obj)
    {
        UiAnimProfile def = defaults();

        if (obj == null)
        {
            return def;
        }

        boolean enabled = getBool(obj, "enabled", def.enabled);
        float windowSpeed = getFloat(obj, "windowSpeed", def.windowSpeed);
        float controlSpeed = getFloat(obj, "controlSpeed", def.controlSpeed);
        float sliderSpeed = getFloat(obj, "sliderSpeed", def.sliderSpeed);
        float smooth = getFloat(obj, "smooth", def.smooth);
        UiAnimation.Type type = getType(obj, "type", def.type);
        return new UiAnimProfile(enabled, windowSpeed, controlSpeed, sliderSpeed, smooth, type);
    }

    private static float getFloat(JsonObject obj, String key, float fallback)
    {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive())
        {
            return fallback;
        }

        try
        {
            return obj.get(key).getAsFloat();
        }
        catch (NumberFormatException ex)
        {
            return fallback;
        }
    }

    private static boolean getBool(JsonObject obj, String key, boolean fallback)
    {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive())
        {
            return fallback;
        }

        try
        {
            return obj.get(key).getAsBoolean();
        }
        catch (RuntimeException ex)
        {
            return fallback;
        }
    }

    private static UiAnimation.Type getType(JsonObject obj, String key, UiAnimation.Type fallback)
    {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive())
        {
            return fallback;
        }

        try
        {
            return UiAnimation.Type.valueOf(obj.get(key).getAsString());
        }
        catch (IllegalArgumentException ex)
        {
            return fallback;
        }
    }
}
