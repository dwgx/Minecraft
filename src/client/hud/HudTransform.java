package client.hud;

public final class HudTransform
{
    private Anchor anchor;
    private Dock dock;
    private float offsetX;
    private float offsetY;
    private float scale;
    private boolean snapToGrid;

    public HudTransform()
    {
        this.anchor = Anchor.TOP_LEFT;
        this.dock = Dock.NONE;
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        this.scale = 1.0F;
        this.snapToGrid = false;
    }

    public Anchor getAnchor()
    {
        return this.anchor;
    }

    public void setAnchor(Anchor anchor)
    {
        Anchor value = anchor == null ? Anchor.TOP_LEFT : anchor;

        if (this.anchor == value)
        {
            return;
        }

        this.anchor = value;
        this.markHudDirty();
    }

    public Dock getDock()
    {
        return this.dock;
    }

    public void setDock(Dock dock)
    {
        Dock value = dock == null ? Dock.NONE : dock;

        if (this.dock == value)
        {
            return;
        }

        this.dock = value;
        this.markHudDirty();
    }

    public float getOffsetX()
    {
        return this.offsetX;
    }

    public void setOffsetX(float offsetX)
    {
        if (Float.compare(this.offsetX, offsetX) == 0)
        {
            return;
        }

        this.offsetX = offsetX;
        this.markHudDirty();
    }

    public float getOffsetY()
    {
        return this.offsetY;
    }

    public void setOffsetY(float offsetY)
    {
        if (Float.compare(this.offsetY, offsetY) == 0)
        {
            return;
        }

        this.offsetY = offsetY;
        this.markHudDirty();
    }

    public float getScale()
    {
        return this.scale;
    }

    public void setScale(float scale)
    {
        float value = Math.max(0.1F, Math.min(4.0F, scale));

        if (Float.compare(this.scale, value) == 0)
        {
            return;
        }

        this.scale = value;
        this.markHudDirty();
    }

    public boolean isSnapToGrid()
    {
        return this.snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid)
    {
        if (this.snapToGrid == snapToGrid)
        {
            return;
        }

        this.snapToGrid = snapToGrid;
        this.markHudDirty();
    }

    private void markHudDirty()
    {
        client.config.ConfigManager config = client.core.ClientBootstrap.instance().getConfigManager();

        if (config != null)
        {
            config.markHudDirty();
        }
    }
}
