package client.event;

public interface Listener<T extends Event>
{
    void onEvent(T event);
}
