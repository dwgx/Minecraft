package client.ui.template;

/**
 * Shared draggable/resizable window state for Nano-based utility screens.
 */
public final class UiWindowState
{
    public enum ResizeMode
    {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private final float minWidth;
    private final float minHeight;

    private boolean initialized;
    private float x;
    private float y;
    private float width;
    private float height;
    private float targetX;
    private float targetY;
    private float targetWidth;
    private float targetHeight;
    private long lastTickAtNanos;

    private boolean moving;
    private boolean resizing;
    private float moveDx;
    private float moveDy;
    private float resizeMouseX;
    private float resizeMouseY;
    private float resizeX;
    private float resizeY;
    private float resizeWidth;
    private float resizeHeight;
    private ResizeMode resizeMode = ResizeMode.BOTTOM_RIGHT;

    public UiWindowState(float minWidth, float minHeight)
    {
        this.minWidth = Math.max(64.0F, minWidth);
        this.minHeight = Math.max(64.0F, minHeight);
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public void setImmediate(float x, float y, float width, float height)
    {
        this.initialized = true;
        this.x = x;
        this.y = y;
        this.width = Math.max(this.minWidth, width);
        this.height = Math.max(this.minHeight, height);
        this.targetX = this.x;
        this.targetY = this.y;
        this.targetWidth = this.width;
        this.targetHeight = this.height;
        this.lastTickAtNanos = System.nanoTime();
    }

    public void setTarget(float x, float y, float width, float height, float screenWidth, float screenHeight, float margin)
    {
        if (!this.initialized)
        {
            this.setImmediate(x, y, width, height);
            this.clampTarget(screenWidth, screenHeight, margin);
            return;
        }

        this.targetX = x;
        this.targetY = y;
        this.targetWidth = width;
        this.targetHeight = height;
        this.clampTarget(screenWidth, screenHeight, margin);
    }

    public void tick(float speed)
    {
        this.tick(speed, UiAnimation.Type.EASE_OUT, 0.60F, true);
    }

    public void tick(float speed, UiAnimation.Type animationType, float smooth, boolean enabled)
    {
        if (!this.initialized)
        {
            return;
        }

        if (this.moving || this.resizing)
        {
            this.snapToTarget();
            return;
        }

        if (!enabled || speed >= 0.999F)
        {
            this.snapToTarget();
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = this.lastTickAtNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - this.lastTickAtNanos) * 1.0E-9D);
        this.lastTickAtNanos = now;

        float response = UiAnimation.responseFromSpeed(speed, smooth, animationType, this.moving || this.resizing);
        this.x = UiAnimation.step(this.x, this.targetX, response, deltaSeconds, animationType, smooth);
        this.y = UiAnimation.step(this.y, this.targetY, response, deltaSeconds, animationType, smooth);
        this.width = UiAnimation.step(this.width, this.targetWidth, response, deltaSeconds, animationType, smooth);
        this.height = UiAnimation.step(this.height, this.targetHeight, response, deltaSeconds, animationType, smooth);
    }

    public void startMove(float mouseX, float mouseY)
    {
        this.moving = true;
        this.resizing = false;
        this.targetX = this.x;
        this.targetY = this.y;
        this.moveDx = mouseX - this.x;
        this.moveDy = mouseY - this.y;
    }

    public void startResize(float mouseX, float mouseY)
    {
        this.startResize(mouseX, mouseY, ResizeMode.BOTTOM_RIGHT);
    }

    public void startResize(float mouseX, float mouseY, ResizeMode mode)
    {
        this.resizing = true;
        this.moving = false;
        this.targetWidth = this.width;
        this.targetHeight = this.height;
        this.targetX = this.x;
        this.targetY = this.y;
        this.resizeMouseX = mouseX;
        this.resizeMouseY = mouseY;
        this.resizeX = this.x;
        this.resizeY = this.y;
        this.resizeWidth = this.width;
        this.resizeHeight = this.height;
        this.resizeMode = mode == null ? ResizeMode.BOTTOM_RIGHT : mode;
    }

    public void updateInteraction(float mouseX, float mouseY, float screenWidth, float screenHeight, float margin)
    {
        if (this.moving)
        {
            this.targetX = mouseX - this.moveDx;
            this.targetY = mouseY - this.moveDy;
        }
        else if (this.resizing)
        {
            float dx = mouseX - this.resizeMouseX;
            float dy = mouseY - this.resizeMouseY;

            switch (this.resizeMode)
            {
                case LEFT:
                    this.targetX = this.resizeX + dx;
                    this.targetWidth = this.resizeWidth - dx;
                    break;
                case RIGHT:
                    this.targetWidth = this.resizeWidth + dx;
                    break;
                case TOP:
                    this.targetY = this.resizeY + dy;
                    this.targetHeight = this.resizeHeight - dy;
                    break;
                case BOTTOM:
                    this.targetHeight = this.resizeHeight + dy;
                    break;
                case TOP_LEFT:
                    this.targetX = this.resizeX + dx;
                    this.targetWidth = this.resizeWidth - dx;
                    this.targetY = this.resizeY + dy;
                    this.targetHeight = this.resizeHeight - dy;
                    break;
                case TOP_RIGHT:
                    this.targetWidth = this.resizeWidth + dx;
                    this.targetY = this.resizeY + dy;
                    this.targetHeight = this.resizeHeight - dy;
                    break;
                case BOTTOM_LEFT:
                    this.targetX = this.resizeX + dx;
                    this.targetWidth = this.resizeWidth - dx;
                    this.targetHeight = this.resizeHeight + dy;
                    break;
                default:
                    this.targetWidth = this.resizeWidth + dx;
                    this.targetHeight = this.resizeHeight + dy;
                    break;
            }

            if ((this.resizeMode == ResizeMode.LEFT || this.resizeMode == ResizeMode.TOP_LEFT || this.resizeMode == ResizeMode.BOTTOM_LEFT) && this.targetWidth < this.minWidth)
            {
                this.targetX = this.resizeX + (this.resizeWidth - this.minWidth);
            }

            if ((this.resizeMode == ResizeMode.TOP || this.resizeMode == ResizeMode.TOP_LEFT || this.resizeMode == ResizeMode.TOP_RIGHT) && this.targetHeight < this.minHeight)
            {
                this.targetY = this.resizeY + (this.resizeHeight - this.minHeight);
            }
        }

        this.clampTarget(screenWidth, screenHeight, margin);
    }

    public void endInteraction()
    {
        this.moving = false;
        this.resizing = false;
    }

    public boolean isMoving()
    {
        return this.moving;
    }

    public boolean isResizing()
    {
        return this.resizing;
    }

    public boolean isInteracting()
    {
        return this.moving || this.resizing;
    }

    public void snapToTarget()
    {
        this.x = this.targetX;
        this.y = this.targetY;
        this.width = this.targetWidth;
        this.height = this.targetHeight;
        this.lastTickAtNanos = System.nanoTime();
    }

    public float getX()
    {
        return this.x;
    }

    public float getY()
    {
        return this.y;
    }

    public float getWidth()
    {
        return this.width;
    }

    public float getHeight()
    {
        return this.height;
    }

    public float getTargetX()
    {
        return this.targetX;
    }

    public float getTargetY()
    {
        return this.targetY;
    }

    public float getTargetWidth()
    {
        return this.targetWidth;
    }

    public float getTargetHeight()
    {
        return this.targetHeight;
    }

    private void clampTarget(float screenWidth, float screenHeight, float margin)
    {
        float safeMargin = Math.max(0.0F, margin);
        float maxWidth = Math.max(this.minWidth, screenWidth - safeMargin * 2.0F);
        float maxHeight = Math.max(this.minHeight, screenHeight - safeMargin * 2.0F);

        this.targetWidth = UiMotion.clamp(this.targetWidth, this.minWidth, maxWidth);
        this.targetHeight = UiMotion.clamp(this.targetHeight, this.minHeight, maxHeight);

        float maxX = Math.max(safeMargin, screenWidth - this.targetWidth - safeMargin);
        float maxY = Math.max(safeMargin, screenHeight - this.targetHeight - safeMargin);
        this.targetX = UiMotion.clamp(this.targetX, safeMargin, maxX);
        this.targetY = UiMotion.clamp(this.targetY, safeMargin, maxY);
    }
}
