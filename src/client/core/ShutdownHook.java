package client.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered shutdown actions for saving state and releasing resources.
 */
public final class ShutdownHook implements Runnable
{
    private final List<Runnable> actions = new ArrayList<Runnable>();

    public synchronized void addAction(Runnable action)
    {
        if (action != null)
        {
            this.actions.add(action);
        }
    }

    public synchronized int size()
    {
        return this.actions.size();
    }

    public void run()
    {
        List<Runnable> snapshot;

        synchronized (this)
        {
            snapshot = new ArrayList<Runnable>(this.actions);
        }

        for (int i = snapshot.size() - 1; i >= 0; --i)
        {
            try
            {
                snapshot.get(i).run();
            }
            catch (Throwable ignored)
            {
            }
        }
    }
}
