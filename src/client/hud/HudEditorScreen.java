package client.hud;

import net.minecraft.client.gui.GuiScreen;

/**
 * HUD 编辑器外壳，后续用于承载拖拽与对齐交互。
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
