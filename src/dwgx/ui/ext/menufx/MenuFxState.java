package dwgx.ui.ext.menufx;

public final class MenuFxState
{
    private boolean expanded;
    private int scrollRows;
    private float scrollVisual;

    public void reset()
    {
        this.expanded = false;
        this.scrollRows = 0;
        this.scrollVisual = 0.0F;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
    }

    public void toggleExpanded()
    {
        this.expanded = !this.expanded;
    }

    public int getScrollRows()
    {
        return this.scrollRows;
    }

    public void setScrollRows(int scrollRows)
    {
        this.scrollRows = Math.max(0, scrollRows);
    }

    public float getScrollVisual()
    {
        return this.scrollVisual;
    }

    public void setScrollVisual(float scrollVisual)
    {
        this.scrollVisual = Math.max(0.0F, scrollVisual);
    }
}
