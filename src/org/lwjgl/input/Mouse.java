package org.lwjgl.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.Display;

import java.util.ArrayDeque;
import java.util.Queue;

public final class Mouse {
    private static final int MAX_EVENT_QUEUE_SIZE = 1024;
    private static long window;
    private static boolean created;
    private static boolean grabbed;
    private static boolean insideWindow = true;
    private static boolean suppressNextMove;

    private static int x;
    private static int y;
    private static int grabX;
    private static int grabY;
    private static int dx;
    private static int dy;
    private static int dWheel;
    private static double wheelRemainder;

    private static double lastRawX;
    private static double lastRawY;
    private static final double[] cursorXBuffer = new double[1];
    private static final double[] cursorYBuffer = new double[1];

    private static final Queue<MouseEvent> events = new ArrayDeque<MouseEvent>();
    private static MouseEvent current;

    private Mouse() {
    }

    public static void attachWindow(long win) {
        window = win;
        created = true;
        grabbed = false;
        insideWindow = true;
        suppressNextMove = false;
        syncFromWindow();
        grabX = x;
        grabY = y;
        resetMotion();
        clearEvents();
    }

    public static void reset() {
        window = 0L;
        created = false;
        grabbed = false;
        insideWindow = true;
        suppressNextMove = false;
        x = 0;
        y = 0;
        grabX = 0;
        grabY = 0;
        dx = 0;
        dy = 0;
        dWheel = 0;
        wheelRemainder = 0.0;
        lastRawX = 0.0;
        lastRawY = 0.0;
        clearEvents();
    }

    public static boolean isCreated() {
        return created;
    }

    public static void setGrabbed(boolean grab) {
        boolean previous = grabbed;
        grabbed = grab;
        if (!created) {
            return;
        }

        if (grab && !previous) {
            grabX = x;
            grabY = y;
        }

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, grab ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        if (!grab && previous) {
            suppressNextMove = true;
            warpCursorTo(grabX, grabY);
        } else {
            suppressNextMove = false;
        }
        syncFromWindow();
        insideWindow = true;
        resetMotion();
        clearEvents();
    }

    public static boolean isGrabbed() {
        return grabbed;
    }

    public static void setCursorPosition(int newX, int newY) {
        if (!created) {
            return;
        }

        int clampedX = clipX(newX);
        int clampedY = clipY(newY);
        x = clampedX;
        y = clampedY;

        if (grabbed) {
            grabX = clampedX;
            grabY = clampedY;
            return;
        }

        suppressNextMove = true;
        warpCursorTo(clampedX, clampedY);
        resetMotion();
        clearEvents();
    }

    public static int getDX() {
        int value = dx;
        dx = 0;
        return value;
    }

    public static int getDY() {
        int value = dy;
        dy = 0;
        return value;
    }

    public static int getDWheel() {
        int value = dWheel;
        dWheel = 0;
        return value;
    }

    public static int getEventDWheel() {
        return current != null ? current.wheel : 0;
    }

    public static int getX() {
        return x;
    }

    public static int getY() {
        return y;
    }

    public static boolean isButtonDown(int button) {
        if (!created || button < 0) {
            return false;
        }
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    public static boolean next() {
        current = events.poll();
        return current != null;
    }

    public static int getEventButton() {
        return current != null ? current.button : -1;
    }

    public static boolean getEventButtonState() {
        return current != null && current.pressed;
    }

    public static int getEventX() {
        return current != null ? current.x : x;
    }

    public static int getEventY() {
        return current != null ? current.y : y;
    }

    public static boolean isInsideWindow() {
        return insideWindow || grabbed;
    }

    public static void pushButtonEvent(int button, boolean pressed, double rawX, double rawY) {
        if (!created) {
            return;
        }
        int eventX = grabbed ? x : windowToDisplayX(rawX);
        int eventY = grabbed ? y : windowToDisplayY(rawY);
        enqueueEvent(new MouseEvent(button, pressed, 0, eventX, eventY));
    }

    public static void pushWheelEvent(double yoffset, double rawX, double rawY) {
        if (!created) {
            return;
        }

        double wheelDelta = yoffset * 120.0 + wheelRemainder;
        int amount = (int)wheelDelta;
        wheelRemainder = wheelDelta - amount;
        if (amount == 0) {
            return;
        }

        dWheel += amount;
        int eventX = grabbed ? x : windowToDisplayX(rawX);
        int eventY = grabbed ? y : windowToDisplayY(rawY);
        enqueueEvent(new MouseEvent(-1, false, amount, eventX, eventY));
    }

    public static void pushMoveEvent(double rawX, double rawY) {
        if (!created) {
            return;
        }

        double rawDeltaX = rawX - lastRawX;
        double rawDeltaY = rawY - lastRawY;
        lastRawX = rawX;
        lastRawY = rawY;

        if (suppressNextMove) {
            suppressNextMove = false;
            return;
        }

        if (grabbed) {
            int moveX = windowDeltaToDisplayX(rawDeltaX);
            int moveY = windowDeltaToDisplayY(rawDeltaY);
            if (moveX == 0 && moveY == 0) {
                return;
            }
            dx += moveX;
            dy += moveY;
            x = clipX(x + moveX);
            y = clipY(y + moveY);
            return;
        }

        int newX = windowToDisplayX(rawX);
        int newY = windowToDisplayY(rawY);
        int moveX = newX - x;
        int moveY = newY - y;
        if (moveX == 0 && moveY == 0) {
            return;
        }

        x = newX;
        y = newY;
        dx += moveX;
        dy += moveY;
        enqueueEvent(new MouseEvent(-1, false, 0, x, y));
    }

    public static void onWindowFocusChanged(boolean focused) {
        insideWindow = focused || grabbed;
        if (!created || !focused) {
            return;
        }

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, grabbed ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        suppressNextMove = grabbed;
        syncFromWindow();
        resetMotion();
        clearEvents();
    }

    public static void onCursorEntered(boolean entered) {
        insideWindow = entered || grabbed;
    }

    private static void syncFromWindow() {
        GLFW.glfwGetCursorPos(window, cursorXBuffer, cursorYBuffer);
        lastRawX = cursorXBuffer[0];
        lastRawY = cursorYBuffer[0];
        x = windowToDisplayX(lastRawX);
        y = windowToDisplayY(lastRawY);
    }

    private static void warpCursorTo(int displayX, int displayY) {
        double rawX = displayToWindowX(displayX);
        double rawY = displayToWindowY(displayY);
        GLFW.glfwSetCursorPos(window, rawX, rawY);
        lastRawX = rawX;
        lastRawY = rawY;
    }

    private static void resetMotion() {
        dx = 0;
        dy = 0;
        dWheel = 0;
        wheelRemainder = 0.0;
    }

    private static void clearEvents() {
        events.clear();
        current = null;
    }

    private static void enqueueEvent(MouseEvent event) {
        if (events.size() >= MAX_EVENT_QUEUE_SIZE) {
            events.poll();
        }
        events.add(event);
    }

    private static int windowToDisplayX(double rawX) {
        double windowW = Math.max(1.0, Display.getWindowWidth());
        double displayW = Math.max(1.0, Display.getWidth());
        return clipX((int)Math.round(rawX * displayW / windowW));
    }

    private static int windowToDisplayY(double rawY) {
        double windowH = Math.max(1.0, Display.getWindowHeight());
        double displayH = Math.max(1.0, Display.getHeight());
        double topBased = rawY * displayH / windowH;
        return clipY((int)Math.round(displayH - 1.0 - topBased));
    }

    private static int windowDeltaToDisplayX(double rawDeltaX) {
        double windowW = Math.max(1.0, Display.getWindowWidth());
        double displayW = Math.max(1.0, Display.getWidth());
        return (int)Math.round(rawDeltaX * displayW / windowW);
    }

    private static int windowDeltaToDisplayY(double rawDeltaY) {
        double windowH = Math.max(1.0, Display.getWindowHeight());
        double displayH = Math.max(1.0, Display.getHeight());
        return (int)Math.round(-rawDeltaY * displayH / windowH);
    }

    private static double displayToWindowX(int displayX) {
        double windowW = Math.max(1.0, Display.getWindowWidth());
        double displayW = Math.max(1.0, Display.getWidth());
        return displayX * windowW / displayW;
    }

    private static double displayToWindowY(int displayY) {
        double windowH = Math.max(1.0, Display.getWindowHeight());
        double displayH = Math.max(1.0, Display.getHeight());
        return (displayH - 1.0 - displayY) * windowH / displayH;
    }

    private static int clipX(int value) {
        return clamp(value, 0, Math.max(0, Display.getWidth() - 1));
    }

    private static int clipY(int value) {
        return clamp(value, 0, Math.max(0, Display.getHeight() - 1));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class MouseEvent {
        final int button;
        final boolean pressed;
        final int wheel;
        final int x;
        final int y;

        MouseEvent(int button, boolean pressed, int wheel, int x, int y) {
            this.button = button;
            this.pressed = pressed;
            this.wheel = wheel;
            this.x = x;
            this.y = y;
        }
    }
}
