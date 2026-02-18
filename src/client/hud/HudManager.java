package client.hud;

import client.render.DisplayMetrics;
import client.render.RenderContext2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager
{
    private final List<HudElement> elements = new ArrayList<HudElement>();
    private final List<HudElement> elementsView = Collections.unmodifiableList(this.elements);

    public synchronized void register(HudElement element)
    {
        if (element != null)
        {
            this.elements.add(element);
        }
    }

    public synchronized List<HudElement> getElements()
    {
        return this.elementsView;
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
        if (context == null || context.getNanoVG() == null)
        {
            return;
        }

        DisplayMetrics metrics = context.getMetrics();
        float screenW = metrics == null ? 0.0F : (float)metrics.getWindowWidth();
        float screenH = metrics == null ? 0.0F : (float)metrics.getWindowHeight();

        List<HudElement> snapshot = this.getElements();

        for (int i = 0; i < snapshot.size(); ++i)
        {
            HudElement element = snapshot.get(i);

            if (element.isEnabled() && element.getLayer() == layer)
            {
                context.getNanoVG().save();

                try
                {
                    this.applyTransform(context, element, screenW, screenH);
                    element.render(context);
                }
                finally
                {
                    context.getNanoVG().restore();
                }
            }
        }
    }

    private void applyTransform(RenderContext2D context, HudElement element, float screenW, float screenH)
    {
        if (context.getNanoVG() == null || element == null)
        {
            return;
        }

        HudLayoutMath.applyTransform(context.getNanoVG(), element, screenW, screenH);
    }
}
