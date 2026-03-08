package client.ui.editor;

import client.ui.layout.UiRect;
import client.ui.template.UiLayoutProfile;

import java.util.List;

/**
 * Abstract blueprint describing a GUI's layout zones and draggable edges.
 * Each GUI type (ClickGui, Settings, Account) provides its own blueprint
 * that knows how to compute zones from UiLayoutProfile and apply drag deltas.
 *
 * Blueprints cache their zone/edge lists internally and reuse objects to
 * avoid per-frame allocation during drag operations.
 */
public interface GuiBlueprint
{
    /** Human-readable name shown in the editor. */
    String displayName();

    /** Unique key for animation channels. */
    String key();

    /**
     * Compute all layout zones for the given window area.
     * Implementations MUST reuse cached lists and mutate existing Zone objects
     * rather than allocating new ones each frame.
     */
    List<Zone> computeZones(UiRect window, UiLayoutProfile lp, float scale);

    /**
     * Compute all draggable edges between zones.
     * Implementations MUST reuse cached lists and mutate existing DragEdge objects
     * rather than allocating new ones each frame.
     */
    List<DragEdge> computeEdges(UiRect window, UiLayoutProfile lp, float scale);

    /**
     * A named rectangular zone in the GUI layout. Mutable for reuse.
     */
    final class Zone
    {
        public String label;
        public float rx, ry, rw, rh;
        public int depth; // 0 = window, 1 = panel, 2 = sub-area
        public int fillArgb;

        public Zone() {}

        /** Legacy constructor for compatibility. Extracts UiRect fields inline. */
        public Zone(String label, UiRect rect, int depth, int fillArgb)
        {
            this.label = label;
            this.rx = rect.x;
            this.ry = rect.y;
            this.rw = rect.w;
            this.rh = rect.h;
            this.depth = depth;
            this.fillArgb = fillArgb;
        }

        public Zone set(String label, float x, float y, float w, float h, int depth, int fillArgb)
        {
            this.label = label;
            this.rx = x;
            this.ry = y;
            this.rw = w;
            this.rh = h;
            this.depth = depth;
            this.fillArgb = fillArgb;
            return this;
        }
    }

    /**
     * A draggable edge that maps to a layout parameter. Mutable for reuse.
     */
    final class DragEdge
    {
        public String id;
        public String tooltip;
        public float hx, hy, hw, hh; // hit rect
        public boolean vertical; // true = drag left/right, false = drag up/down
        public DragHandler handler;
        public ValueProvider valueProvider;
        public float snapStep; // 0 = no snap, >0 = snap to multiples

        public DragEdge() {}

        /** Legacy constructor for compatibility. Extracts UiRect fields inline. */
        public DragEdge(String id, String tooltip, UiRect hitRect, boolean vertical,
                        DragHandler handler)
        {
            this(id, tooltip, hitRect, vertical, handler, null, 0.0F);
        }

        /** Legacy constructor for compatibility. Extracts UiRect fields inline. */
        public DragEdge(String id, String tooltip, UiRect hitRect, boolean vertical,
                        DragHandler handler, ValueProvider valueProvider, float snapStep)
        {
            this.id = id;
            this.tooltip = tooltip;
            this.hx = hitRect.x;
            this.hy = hitRect.y;
            this.hw = hitRect.w;
            this.hh = hitRect.h;
            this.vertical = vertical;
            this.handler = handler;
            this.valueProvider = valueProvider;
            this.snapStep = snapStep;
        }

        public DragEdge set(String id, String tooltip, float hx, float hy, float hw, float hh,
                            boolean vertical, DragHandler handler, ValueProvider valueProvider,
                            float snapStep)
        {
            this.id = id;
            this.tooltip = tooltip;
            this.hx = hx;
            this.hy = hy;
            this.hw = hw;
            this.hh = hh;
            this.vertical = vertical;
            this.handler = handler;
            this.valueProvider = valueProvider;
            this.snapStep = snapStep;
            return this;
        }

        public boolean containsMouse(float mx, float my)
        {
            return mx >= this.hx && mx <= this.hx + this.hw
                && my >= this.hy && my <= this.hy + this.hh;
        }
    }

    /**
     * Callback to apply a drag position to the layout profile.
     */
    interface DragHandler
    {
        void apply(UiLayoutProfile lp, float mousePos, UiRect window, float scale);
    }

    /**
     * Provides a formatted string showing the current value of the parameter
     * controlled by a DragEdge. Displayed as a live badge next to the handle.
     */
    interface ValueProvider
    {
        String value(UiLayoutProfile lp);
    }
}
