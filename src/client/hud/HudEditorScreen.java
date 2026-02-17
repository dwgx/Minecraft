package client.hud;

import net.minecraft.client.gui.GuiScreen;

/**
 * Placeholder editor shell used to host HUD drag/drop widgets.
 */
public class HudEditorScreen extends GuiScreen
{
    private final HudManager hudManager;
    private final SelectionModel selection = new SelectionModel();
    private final SnapGrid grid = new SnapGrid(4);

    public HudEditorScreen(HudManager hudManager)
    {
        this.hudManager = hudManager;
    }

    public HudManager getHudManager()
    {
        return this.hudManager;
    }

    public SelectionModel getSelection()
    {
        return this.selection;
    }

    public SnapGrid getGrid()
    {
        return this.grid;
    }
}
