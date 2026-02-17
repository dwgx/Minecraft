package client.event;

import client.render.RenderContext2D;

public final class Render2DEvent implements Event
{
    private final RenderContext2D context;

    public Render2DEvent(RenderContext2D context)
    {
        this.context = context;
    }

    public RenderContext2D getContext()
    {
        return this.context;
    }
}
