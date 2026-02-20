package dwgx.foundation.inventory;

/**
 * Immutable hotbar capture used by inventory strategies.
 */
public final class HotbarSnapshot
{
    private final Object[] hotbar;

    public HotbarSnapshot(Object[] source)
    {
        this.hotbar = new Object[9];

        for (int i = 0; i < this.hotbar.length && source != null && i < source.length; ++i)
        {
            this.hotbar[i] = source[i];
        }
    }

    public Object get(int slot)
    {
        return slot >= 0 && slot < this.hotbar.length ? this.hotbar[slot] : null;
    }
}
