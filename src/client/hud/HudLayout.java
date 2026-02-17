package client.hud;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HudLayout
{
    private final Map<String, HudTransform> transforms = new HashMap<String, HudTransform>();

    public void put(String elementId, HudTransform transform)
    {
        if (elementId != null && transform != null)
        {
            this.transforms.put(elementId, transform);
        }
    }

    public HudTransform get(String elementId)
    {
        return this.transforms.get(elementId);
    }

    public Map<String, HudTransform> asMap()
    {
        return Collections.unmodifiableMap(this.transforms);
    }
}
