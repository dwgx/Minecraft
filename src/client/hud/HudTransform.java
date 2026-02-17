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
        this.anchor = anchor == null ? Anchor.TOP_LEFT : anchor;
    }

    public Dock getDock()
    {
        return this.dock;
    }

    public void setDock(Dock dock)
    {
        this.dock = dock == null ? Dock.NONE : dock;
    }

    public float getOffsetX()
    {
        return this.offsetX;
    }

    public void setOffsetX(float offsetX)
    {
        this.offsetX = offsetX;
    }

    public float getOffsetY()
    {
        return this.offsetY;
    }

    public void setOffsetY(float offsetY)
    {
        this.offsetY = offsetY;
    }

    public float getScale()
    {
        return this.scale;
    }

    public void setScale(float scale)
    {
        this.scale = Math.max(0.1F, Math.min(4.0F, scale));
    }

    public boolean isSnapToGrid()
    {
        return this.snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid)
    {
        this.snapToGrid = snapToGrid;
    }
}
