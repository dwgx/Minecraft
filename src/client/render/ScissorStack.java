package client.render;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ScissorStack
{
    private final NanoVGContext context;
    private final Deque<Rect> stack = new ArrayDeque<Rect>();

    public ScissorStack(NanoVGContext context)
    {
        this.context = context;
    }

    public void push(float x, float y, float width, float height)
    {
        Rect next = new Rect(x, y, width, height);
        this.stack.push(next);
        this.context.save();

        if (this.stack.size() == 1)
        {
            this.context.scissor(x, y, width, height);
        }
        else
        {
            this.context.intersectScissor(x, y, width, height);
        }
    }

    public void pop()
    {
        if (this.stack.isEmpty())
        {
            return;
        }

        this.stack.pop();
        this.context.restore();
    }

    public void clear()
    {
        while (!this.stack.isEmpty())
        {
            this.pop();
        }

        this.context.resetScissor();
    }

    public int size()
    {
        return this.stack.size();
    }

    private static final class Rect
    {
        @SuppressWarnings("unused")
        private final float x;
        @SuppressWarnings("unused")
        private final float y;
        @SuppressWarnings("unused")
        private final float width;
        @SuppressWarnings("unused")
        private final float height;

        private Rect(float x, float y, float width, float height)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
