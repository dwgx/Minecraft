package client.hud;

import client.render.RenderContext2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager
{
    private final List<HudElement> elements = new ArrayList<HudElement>();

    public synchronized void register(HudElement element)
    {
        if (element != null)
        {
            this.elements.add(element);
        }
    }

    public synchronized List<HudElement> getElements()
    {
        return Collections.unmodifiableList(new ArrayList<HudElement>(this.elements));
    }

    public void render(RenderContext2D context)
    {
        this.renderLayer(context, HudLayer.BACKGROUND);
        this.renderLayer(context, HudLayer.CONTENT);
        this.renderLayer(context, HudLayer.FOREGROUND);
    }

    public void renderDebug(RenderContext2D context)
    {
        this.renderLayer(context, HudLayer.DEBUG);
    }

    private void renderLayer(RenderContext2D context, HudLayer layer)
    {
        List<HudElement> snapshot = this.getElements();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            HudElement element = snapshot.get(i);

            if (element.isEnabled() && element.getLayer() == layer)
            {
                context.getNanoVG().save();

                try
                {
                    element.render(context);
                }
                finally
                {
                    context.getNanoVG().restore();
                }
            }
        }
    }
}
