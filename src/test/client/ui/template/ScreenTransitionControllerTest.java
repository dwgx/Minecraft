package client.ui.template;

import net.minecraft.client.gui.GuiScreen;

public final class ScreenTransitionControllerTest
{
    private static final ScreenTransitionController.Tuning TUNING = ScreenTransitionController.Tuning.of(1.90F, 1.18F, 1.48F, 0.95F, 0.90F, 1.12F, 1.24F, 0.08F);

    private ScreenTransitionControllerTest()
    {
    }

    public static void main(String[] args)
    {
        testResolveType();
        testVisual();
        testStepOpenProgress();
        testStepNavigateCondition();
        System.out.println("[OK] ScreenTransitionControllerTest passed.");
    }

    private static void testResolveType()
    {
        UiAnimation.Type open = UiAnimation.Type.EASE_OUT;
        UiAnimation.Type close = UiAnimation.Type.SPRING;
        UiAnimation.Type sw = UiAnimation.Type.EASE_IN_OUT;
        UiAnimation.Type back = UiAnimation.Type.LINEAR;

        assertEquals(ScreenTransitionController.resolveType(false, ScreenTransitionController.Mode.CLOSE, open, close, sw, back), open, "open type");
        assertEquals(ScreenTransitionController.resolveType(true, ScreenTransitionController.Mode.CLOSE, open, close, sw, back), close, "close type");
        assertEquals(ScreenTransitionController.resolveType(true, ScreenTransitionController.Mode.BACK, open, close, sw, back), back, "back type");
        assertEquals(ScreenTransitionController.resolveType(true, ScreenTransitionController.Mode.SWITCH, open, close, sw, back), sw, "switch type");
    }

    private static void testVisual()
    {
        float zero = ScreenTransitionController.visual(0.0F);
        float one = ScreenTransitionController.visual(1.0F);
        float mid = ScreenTransitionController.visual(0.5F);
        assertTrue(zero >= 0.0F && zero <= 1.0F, "visual(0) range");
        assertTrue(one >= 0.0F && one <= 1.0F, "visual(1) range");
        assertTrue(mid > 0.0F && mid < 1.0F, "visual(0.5) interior");
        assertTrue(one > zero, "visual monotonic endpoints");
    }

    private static void testStepOpenProgress()
    {
        ScreenTransitionController.StepResult result = ScreenTransitionController.step(
            false,
            ScreenTransitionController.Mode.NONE,
            0.0F,
            0L,
            0.56F,
            0.62F,
            UiAnimation.Type.EASE_OUT,
            TUNING,
            false,
            null,
            null
        );
        assertTrue(result.progress() > 0.0F, "open transition should advance from 0");
        assertTrue(result.progress() <= 1.0F, "open transition should not exceed 1");
        assertTrue(!result.shouldNavigate(), "open transition should not navigate");
    }

    private static void testStepNavigateCondition()
    {
        GuiScreen target = new GuiScreen()
        {
        };
        ScreenTransitionController.StepResult result = ScreenTransitionController.step(
            true,
            ScreenTransitionController.Mode.CLOSE,
            0.0F,
            0L,
            0.56F,
            0.62F,
            UiAnimation.Type.EASE_OUT,
            TUNING,
            false,
            target,
            new Object()
        );
        assertTrue(result.shouldNavigate(), "out transition should navigate near-zero progress");
    }

    private static void assertTrue(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object actual, Object expected, String message)
    {
        if (actual == null ? expected != null : !actual.equals(expected))
        {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
