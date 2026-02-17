package client.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 进程退出时按顺序执行的清理动作（保存配置、释放资源等）。
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
