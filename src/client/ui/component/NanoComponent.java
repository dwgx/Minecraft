package client.ui.component;

import client.ui.template.UiAnimProfile;
import dwgx.nano.NanoTheme;
import org.lwjgl.system.MemoryStack;

/**
 * Base class for self-contained NanoVG UI components.
 * Each component manages its own state, animation keys, and rendering.
 */
public abstract class NanoComponent
{
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean visible = true;
    protected final String animKeyPrefix;

    protected NanoComponent(String animKeyPrefix)
    {
        this.animKeyPrefix = animKeyPrefix;
    }

    /**
     * Update layout bounds. Called by the parent screen before rendering.
     */
    public void setBounds(float x, float y, float width, float height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    /**
     * Common render guard: invisible or zero-size components should not render.
     */
    protected boolean shouldSkipRender()
    {
        return !this.visible || this.width <= 0.0F || this.height <= 0.0F;
    }

    /**
     * Render this component using NanoVG.
     */
    public abstract void render(long vg, MemoryStack stack, NanoTheme theme,
                                UiAnimProfile animProfile, float scale, int mouseX, int mouseY);

    /**
     * Handle mouse click. Returns true if the click was consumed.
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        return false;
    }

    /**
     * Handle mouse release. Returns true if the event was consumed.
     */
    public boolean mouseReleased(int mouseX, int mouseY, int button)
    {
        return false;
    }

    /**
     * Handle mouse drag.
     */
    public void mouseDragged(int mouseX, int mouseY)
    {
    }

    /**
     * Handle mouse scroll. Returns true if consumed.
     */
    public boolean mouseScrolled(int mouseX, int mouseY, int delta)
    {
        return false;
    }

    /**
     * Handle key typed. Returns true if consumed.
     */
    public boolean keyTyped(char typedChar, int keyCode)
    {
        return false;
    }

    /**
     * Check if point is within this component's bounds.
     */
    public boolean isHovered(int mouseX, int mouseY)
    {
        return this.visible
            && mouseX >= this.x && mouseX <= this.x + this.width
            && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    protected String animKey(String suffix)
    {
        return this.animKeyPrefix + "." + suffix;
    }
}
