package client.ui.template;

import net.minecraft.client.gui.GuiScreen;

/**
 * Shared transition math/controller for Nano GUI screens.
 */
public final class ScreenTransitionController
{
    public enum Mode
    {
        NONE,
        CLOSE,
        SWITCH,
        BACK
    }

    public static final class Tuning
    {
        private final float closeSpeedBoost;
        private final float backSpeedBoost;
        private final float switchSpeedBoost;
        private final float smoothBackScale;
        private final float smoothOtherScale;
        private final float dtBackScale;
        private final float dtOtherScale;
        private final float speedOffset;

        private Tuning(float closeSpeedBoost, float backSpeedBoost, float switchSpeedBoost, float smoothBackScale, float smoothOtherScale, float dtBackScale, float dtOtherScale, float speedOffset)
        {
            this.closeSpeedBoost = closeSpeedBoost;
            this.backSpeedBoost = backSpeedBoost;
            this.switchSpeedBoost = switchSpeedBoost;
            this.smoothBackScale = smoothBackScale;
            this.smoothOtherScale = smoothOtherScale;
            this.dtBackScale = dtBackScale;
            this.dtOtherScale = dtOtherScale;
            this.speedOffset = speedOffset;
        }

        public static Tuning of(float closeSpeedBoost, float backSpeedBoost, float switchSpeedBoost, float smoothBackScale, float smoothOtherScale, float dtBackScale, float dtOtherScale, float speedOffset)
        {
            return new Tuning(closeSpeedBoost, backSpeedBoost, switchSpeedBoost, smoothBackScale, smoothOtherScale, dtBackScale, dtOtherScale, speedOffset);
        }
    }

    public static final class StepResult
    {
        private final float progress;
        private final long lastNanos;
        private final boolean shouldNavigate;

        private StepResult(float progress, long lastNanos, boolean shouldNavigate)
        {
            this.progress = progress;
            this.lastNanos = lastNanos;
            this.shouldNavigate = shouldNavigate;
        }

        public float progress()
        {
            return this.progress;
        }

        public long lastNanos()
        {
            return this.lastNanos;
        }

        public boolean shouldNavigate()
        {
            return this.shouldNavigate;
        }
    }

    private ScreenTransitionController()
    {
    }

    public static boolean canRequest(Object minecraft, boolean transitioningOut)
    {
        return minecraft != null && !transitioningOut;
    }

    public static Mode modeOrDefault(Mode mode)
    {
        return mode == null ? Mode.SWITCH : mode;
    }

    public static UiAnimation.Type resolveType(boolean transitioningOut, Mode mode, UiAnimation.Type openType, UiAnimation.Type closeType, UiAnimation.Type switchType, UiAnimation.Type backType)
    {
        if (!transitioningOut)
        {
            return openType;
        }

        switch (modeOrDefault(mode))
        {
            case CLOSE:
                return closeType;
            case BACK:
                return backType;
            case SWITCH:
            default:
                return switchType;
        }
    }

    public static StepResult step(boolean transitioningOut, Mode mode, float progress, long lastNanos, float baseSpeed, float baseSmooth, UiAnimation.Type type, Tuning tuning, boolean transitionExecuted, GuiScreen transitionTarget, Object minecraft)
    {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? (1.0F / 60.0F) : (float)((double)(now - lastNanos) * 1.0E-9D);
        float speed = baseSpeed;
        float smooth = baseSmooth;
        Mode activeMode = modeOrDefault(mode);

        if (transitioningOut)
        {
            float boost;

            switch (activeMode)
            {
                case CLOSE:
                    boost = tuning.closeSpeedBoost;
                    break;
                case BACK:
                    boost = tuning.backSpeedBoost;
                    break;
                case SWITCH:
                default:
                    boost = tuning.switchSpeedBoost;
                    break;
            }

            speed = UiMotion.clamp(speed * boost + tuning.speedOffset, 0.05F, 1.0F);
            float smoothScale = activeMode == Mode.BACK ? tuning.smoothBackScale : tuning.smoothOtherScale;
            smooth = UiMotion.clamp(smooth * smoothScale, 0.0F, 1.0F);
            dt *= activeMode == Mode.BACK ? tuning.dtBackScale : tuning.dtOtherScale;
        }

        float response = UiAnimation.responseFromSpeed(speed, smooth, type, false);
        float target = transitioningOut ? 0.0F : 1.0F;
        float nextProgress = UiAnimation.step(progress, target, response, dt, type, smooth);
        boolean shouldNavigate = transitioningOut && !transitionExecuted && minecraft != null && transitionTarget != null && nextProgress <= 0.02F;
        return new StepResult(nextProgress, now, shouldNavigate);
    }

    public static float visual(float progress)
    {
        float p = UiMotion.clamp01(progress);
        float eased = (float)Math.pow((double)p, 0.82D);
        return UiMotion.clamp(eased, 0.0F, 1.0F);
    }
}
