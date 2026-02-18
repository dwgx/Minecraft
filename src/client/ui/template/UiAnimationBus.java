package client.ui.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized reusable animation channels for controls/sliders across UI screens.
 */
public final class UiAnimationBus
{
    private static final Map<String, Channel> CHANNELS = new HashMap<String, Channel>();

    private UiAnimationBus()
    {
    }

    public static float animate(String key, float target, float speed, float smooth, UiAnimation.Type type, boolean enabled)
    {
        if (key == null || key.isEmpty())
        {
            return target;
        }

        Channel channel = CHANNELS.get(key);

        if (channel == null)
        {
            channel = new Channel();
            channel.value = target;
            channel.lastNanos = System.nanoTime();
            CHANNELS.put(key, channel);
            return target;
        }

        if (!enabled)
        {
            channel.value = target;
            channel.lastNanos = System.nanoTime();
            return target;
        }

        long now = System.nanoTime();
        float dt = channel.lastNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - channel.lastNanos) * 1.0E-9D);
        channel.lastNanos = now;
        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        channel.value = UiAnimation.step(channel.value, target, response, dt, type, smooth);
        return channel.value;
    }

    public static void clearAll()
    {
        CHANNELS.clear();
    }

    public static void clearPrefix(String prefix)
    {
        if (prefix == null || prefix.isEmpty())
        {
            return;
        }

        List<String> keys = new ArrayList<String>(CHANNELS.keySet());

        for (int i = 0; i < keys.size(); ++i)
        {
            String key = keys.get(i);

            if (key != null && key.startsWith(prefix))
            {
                CHANNELS.remove(key);
            }
        }
    }

    private static final class Channel
    {
        private float value;
        private long lastNanos;
    }
}
