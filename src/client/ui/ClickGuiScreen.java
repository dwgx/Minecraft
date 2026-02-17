package client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;

public class ClickGuiScreen extends GuiScreen
{
    private final Theme theme = new Theme();
    private final List<Panel> panels = new ArrayList<Panel>();

    public Theme getTheme()
    {
        return this.theme;
    }

    public void addPanel(Panel panel)
    {
        if (panel != null)
        {
            this.panels.add(panel);
        }
    }

    public List<Panel> getPanels()
    {
        return Collections.unmodifiableList(this.panels);
    }
}
