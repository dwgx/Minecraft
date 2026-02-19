package client.event;

import client.event.annotations.EventLink;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Synchronous event bus with typed registration and annotation-driven listeners.
 */
public final class EventBus
{
    private static final Logger LOGGER = LogManager.getLogger(EventBus.class);
    private final Map<Class<?>, List<Listener<?>>> listeners = new HashMap<Class<?>, List<Listener<?>>>();
    private final Map<Class<?>, List<CallSite>> callSiteMap = new HashMap<Class<?>, List<CallSite>>();
    private final Map<Class<?>, List<Listener<?>>> listenerCache = new HashMap<Class<?>, List<Listener<?>>>();
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

    public synchronized void register(Object subscriber)
    {
        if (subscriber == null)
        {
            return;
        }

        boolean changed = false;
        Field[] fields = subscriber.getClass().getDeclaredFields();

        for (int i = 0; i < fields.length; ++i)
        {
            Field field = fields[i];
            EventLink annotation = field.getAnnotation(EventLink.class);

            if (annotation == null)
            {
                continue;
            }

            Class<? extends Event> eventType = this.resolveEventType(field);

            if (eventType == null)
            {
                continue;
            }

            field.setAccessible(true);

            Object value;

            try
            {
                value = field.get(subscriber);
            }
            catch (IllegalAccessException ex)
            {
                LOGGER.warn("Failed to access event listener field {}#{}.", subscriber.getClass().getName(), field.getName(), ex);
                continue;
            }

            if (!(value instanceof Listener))
            {
                continue;
            }

            @SuppressWarnings("unchecked")
            Listener<? extends Event> listener = (Listener<? extends Event>)value;
            List<CallSite> callSites = this.callSiteMap.get(eventType);

            if (callSites == null)
            {
                callSites = new ArrayList<CallSite>();
                this.callSiteMap.put(eventType, callSites);
            }

            if (containsCallSite(callSites, subscriber, field.getName()))
            {
                continue;
            }

            callSites.add(new CallSite(subscriber, field.getName(), listener, annotation.value()));
            sortCallSites(callSites);
            changed = true;
        }

        if (changed)
        {
            this.populateListenerCache();
        }
    }

    public synchronized void unregister(Object subscriber)
    {
        if (subscriber == null)
        {
            return;
        }

        boolean changed = false;
        Iterator<Map.Entry<Class<?>, List<CallSite>>> iterator = this.callSiteMap.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<Class<?>, List<CallSite>> entry = iterator.next();
            List<CallSite> callSites = entry.getValue();

            if (callSites == null || callSites.isEmpty())
            {
                iterator.remove();
                changed = true;
                continue;
            }

            for (int i = callSites.size() - 1; i >= 0; --i)
            {
                if (callSites.get(i).owner == subscriber)
                {
                    callSites.remove(i);
                    changed = true;
                }
            }

            if (callSites.isEmpty())
            {
                iterator.remove();
            }
        }

        if (changed)
        {
            this.populateListenerCache();
        }
    }

    public synchronized void clear()
    {
        this.listeners.clear();
        this.callSiteMap.clear();
        this.listenerCache.clear();
    }

    public void post(Event event)
    {
        if (event == null)
        {
            return;
        }

        Class<?> eventClass = event.getClass();
        this.dispatch(event, this.getDirectSnapshot(eventClass));
        this.dispatch(event, this.getLinkedSnapshot(eventClass));

        if (eventClass != Event.class)
        {
            this.dispatch(event, this.getDirectSnapshot(Event.class));
            this.dispatch(event, this.getLinkedSnapshot(Event.class));
        }
    }

    private synchronized List<Listener<?>> getDirectSnapshot(Class<?> eventType)
    {
        List<Listener<?>> list = this.listeners.get(eventType);
        return list == null ? Collections.<Listener<?>>emptyList() : new ArrayList<Listener<?>>(list);
    }

    private synchronized List<Listener<?>> getLinkedSnapshot(Class<?> eventType)
    {
        List<Listener<?>> list = this.listenerCache.get(eventType);
        return list == null ? Collections.<Listener<?>>emptyList() : new ArrayList<Listener<?>>(list);
    }

    private synchronized void populateListenerCache()
    {
        this.listenerCache.clear();
        Iterator<Map.Entry<Class<?>, List<CallSite>>> iterator = this.callSiteMap.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<Class<?>, List<CallSite>> entry = iterator.next();
            List<CallSite> callSites = entry.getValue();

            if (callSites == null || callSites.isEmpty())
            {
                iterator.remove();
                continue;
            }

            List<Listener<?>> cached = new ArrayList<Listener<?>>(callSites.size());

            for (int i = 0; i < callSites.size(); ++i)
            {
                Listener<? extends Event> listener = callSites.get(i).listener;

                if (listener != null)
                {
                    cached.add(listener);
                }
            }

            if (!cached.isEmpty())
            {
                this.listenerCache.put(entry.getKey(), cached);
            }
        }
    }

    private static boolean containsCallSite(List<CallSite> callSites, Object owner, String fieldName)
    {
        for (int i = 0; i < callSites.size(); ++i)
        {
            CallSite callSite = callSites.get(i);

            if (callSite.owner == owner && callSite.fieldName.equals(fieldName))
            {
                return true;
            }
        }

        return false;
    }

    private static void sortCallSites(List<CallSite> callSites)
    {
        Collections.sort(callSites, new java.util.Comparator<CallSite>()
        {
            public int compare(CallSite left, CallSite right)
            {
                return right.priority - left.priority;
            }
        });
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

    private Class<? extends Event> resolveEventType(Field field)
    {
        if (field == null || !Listener.class.isAssignableFrom(field.getType()))
        {
            return null;
        }

        Type generic = field.getGenericType();

        if (!(generic instanceof ParameterizedType))
        {
            return null;
        }

        Type[] actual = ((ParameterizedType)generic).getActualTypeArguments();

        if (actual.length != 1 || !(actual[0] instanceof Class<?>))
        {
            return null;
        }

        Class<?> eventType = (Class<?>)actual[0];

        if (!Event.class.isAssignableFrom(eventType))
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        Class<? extends Event> casted = (Class<? extends Event>)eventType;
        return casted;
    }

    public synchronized long getDispatchFailureCount()
    {
        return this.dispatchFailureCount;
    }

    private static final class CallSite
    {
        private final Object owner;
        private final String fieldName;
        private final Listener<? extends Event> listener;
        private final byte priority;

        private CallSite(Object owner, String fieldName, Listener<? extends Event> listener, byte priority)
        {
            this.owner = owner;
            this.fieldName = fieldName == null ? "" : fieldName;
            this.listener = listener;
            this.priority = priority;
        }
    }
}
