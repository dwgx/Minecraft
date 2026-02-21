package client.ui.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Centralized reusable animation channels for controls/sliders across UI screens.
 */
public final class UiAnimationBus
{
    private static final long STALE_CHANNEL_TTL_NANOS = 12000000000L;
    private static final long PRUNE_INTERVAL_NANOS = 2000000000L;
    private static final Map<String, Channel> CHANNELS = new HashMap<String, Channel>();
    private static long frameNowNanos;
    private static long lastPruneAtNanos;

    private UiAnimationBus()
    {
    }

    public static void beginFrame()
    {
        long now = System.nanoTime();
        frameNowNanos = now;

        if (now - lastPruneAtNanos < PRUNE_INTERVAL_NANOS)
        {
            return;
        }

        pruneStaleChannels(now);
    }

    public static float animate(String key, float target, UiAnimProfile profile)
    {
        return animateWindow(key, target, profile);
    }

    public static float animateWindow(String key, float target, UiAnimProfile profile)
    {
        UiAnimProfile resolved = resolveProfile(profile);
        return animate(key, target, resolved.windowSpeed(), resolved.smooth(), resolved.type(), resolved.isEnabled());
    }

    public static float animateControl(String key, float target, UiAnimProfile profile)
    {
        UiAnimProfile resolved = resolveProfile(profile);
        return animate(key, target, resolved.controlSpeed(), resolved.smooth(), resolved.type(), resolved.isEnabled());
    }

    public static float animateSlider(String key, float target, UiAnimProfile profile)
    {
        UiAnimProfile resolved = resolveProfile(profile);
        return animate(key, target, resolved.sliderSpeed(), resolved.smooth(), resolved.type(), resolved.isEnabled());
    }

    public static float animateWithSpeed(String key, float target, UiAnimProfile profile, float speed)
    {
        UiAnimProfile resolved = resolveProfile(profile);
        return animate(key, target, UiMotion.clamp(speed, 0.05F, 1.0F), resolved.smooth(), resolved.type(), resolved.isEnabled());
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
            channel.lastNanos = currentTimeNanos();
            channel.lastSeenNanos = channel.lastNanos;
            CHANNELS.put(key, channel);
            return target;
        }

        if (!enabled)
        {
            channel.value = target;
            channel.lastNanos = currentTimeNanos();
            channel.lastSeenNanos = channel.lastNanos;
            return target;
        }

        long now = currentTimeNanos();
        float dt = channel.lastNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - channel.lastNanos) * 1.0E-9D);
        channel.lastNanos = now;
        channel.lastSeenNanos = now;
        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        channel.value = UiAnimation.step(channel.value, target, response, dt, type, smooth);
        return channel.value;
    }

    public static void clearAll()
    {
        CHANNELS.clear();
        frameNowNanos = 0L;
        lastPruneAtNanos = 0L;
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
        private long lastSeenNanos;
    }

    private static UiAnimProfile resolveProfile(UiAnimProfile profile)
    {
        return profile == null ? UiAnimProfile.defaults() : profile;
    }

    private static long currentTimeNanos()
    {
        return frameNowNanos == 0L ? System.nanoTime() : frameNowNanos;
    }

    private static void pruneStaleChannels(long now)
    {
        lastPruneAtNanos = now;
        Iterator<Map.Entry<String, Channel>> it = CHANNELS.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Channel> entry = it.next();
            Channel channel = entry.getValue();

            if (channel == null)
            {
                it.remove();
                continue;
            }

            long idle = now - channel.lastSeenNanos;

            if (idle > STALE_CHANNEL_TTL_NANOS)
            {
                it.remove();
            }
        }
    }
}
