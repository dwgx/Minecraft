package client.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 轻量同步事件总线，按事件类型进行订阅与分发。
 */
public final class EventBus
{
    private static final Logger LOGGER = LogManager.getLogger(EventBus.class);
    private final Map<Class<?>, List<Listener<?>>> listeners = new HashMap<Class<?>, List<Listener<?>>>();
    private long dispatchFailureCount;

    public synchronized <T extends Event> void register(Class<T> eventType, Listener<T> listener)
    {
        if (eventType == null || listener == null)
        {
            return;
        }

        List<Listener<?>> list = this.listeners.get(eventType);

        if (list == null)
        {
            list = new ArrayList<Listener<?>>();
            this.listeners.put(eventType, list);
        }

        list.add(listener);
    }

    public synchronized <T extends Event> void unregister(Class<T> eventType, Listener<T> listener)
    {
        List<Listener<?>> list = this.listeners.get(eventType);

        if (list == null)
        {
            return;
        }

        list.remove(listener);

        if (list.isEmpty())
        {
            this.listeners.remove(eventType);
        }
    }

    public synchronized void clear()
    {
        this.listeners.clear();
    }

    public void post(Event event)
    {
        if (event == null)
        {
            return;
        }

        Class<?> eventClass = event.getClass();
        List<Listener<?>> direct = this.getSnapshot(eventClass);
        this.dispatch(event, direct);

        if (eventClass != Event.class)
        {
            List<Listener<?>> generic = this.getSnapshot(Event.class);
            this.dispatch(event, generic);
        }
    }

    private synchronized List<Listener<?>> getSnapshot(Class<?> eventType)
    {
        List<Listener<?>> list = this.listeners.get(eventType);
        return list == null ? Collections.<Listener<?>>emptyList() : new ArrayList<Listener<?>>(list);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dispatch(Event event, List<Listener<?>> listenersForType)
    {
        for (int i = 0; i < listenersForType.size(); ++i)
        {
            Listener listener = listenersForType.get(i);

            if (listener == null)
            {
                continue;
            }

            this.dispatchSafely(event, listener);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dispatchSafely(Event event, Listener listener)
    {
        try
        {
            listener.onEvent(event);
        }
        catch (Throwable throwable)
        {
            synchronized (this)
            {
                ++this.dispatchFailureCount;
            }

            String eventName = event == null ? "unknown" : event.getClass().getName();
            LOGGER.error("Event listener {} failed for {}.", listener.getClass().getName(), eventName, throwable);
        }
    }

    public synchronized long getDispatchFailureCount()
    {
        return this.dispatchFailureCount;
    }
}
