package client.hud;

public final class SelectionModel
{
    private HudElement selected;

    public HudElement getSelected()
    {
        return this.selected;
    }

    public void setSelected(HudElement selected)
    {
        this.selected = selected;
    }

    public void clear()
    {
        this.selected = null;
    }
}
