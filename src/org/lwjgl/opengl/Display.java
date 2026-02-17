package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight GLFW-backed replacement for LWJGL2 Display.
 * Only methods used by Minecraft 1.8.9 are implemented.
 */
public final class Display {
    private static long window = 0;
    private static GLFWErrorCallback errorCallback;
    private static boolean vsync = false;
    private static boolean resizable = true;
    private static boolean fullscreen = false;
    private static boolean wasResized = false;
    private static int width = 854;
    private static int height = 480;
    private static int windowWidth = 854;
    private static int windowHeight = 480;
    private static DisplayMode desktopMode;
    private static DisplayMode currentMode;
    private static GLFWKeyCallback keyCallback;
    private static GLFWMouseButtonCallback mouseButtonCallback;
    private static GLFWCursorPosCallback cursorPosCallback;
    private static GLFWCursorEnterCallback cursorEnterCallback;
    private static GLFWScrollCallback scrollCallback;
    private static GLFWWindowFocusCallback windowFocusCallback;
    private static final int[] windowWidthBuffer = new int[1];
    private static final int[] windowHeightBuffer = new int[1];
    private static final int[] framebufferWidth = new int[1];
    private static final int[] framebufferHeight = new int[1];
    private static final double[] cursorXBuffer = new double[1];
    private static final double[] cursorYBuffer = new double[1];
    private static long lastSyncNanos = System.nanoTime();

    private Display() {}

    public static void setVSyncEnabled(boolean enabled) {
        vsync = enabled;
        if (isCreated()) {
            GLFW.glfwSwapInterval(enabled ? 1 : 0);
        }
    }

    public static void setResizable(boolean r) {
        resizable = r;
        if (isCreated()) {
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_RESIZABLE, r ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static void setTitle(String title) {
        if (isCreated()) {
            GLFW.glfwSetWindowTitle(window, title);
        }
    }

    public static void create() throws LWJGLException {
        create(new PixelFormat());
    }

    public static void create(PixelFormat ignored) throws LWJGLException {
        if (isCreated()) return;

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!GLFW.glfwInit()) {
            errorCallback.free();
            errorCallback = null;
            throw new LWJGLException("Unable to initialize GLFW");
        }

        try {
            GLFWVidMode desktop = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            desktopMode = new DisplayMode(desktop.width(), desktop.height(), desktop.redBits() + desktop.greenBits() + desktop.blueBits(), desktop.refreshRate());
            currentMode = new DisplayMode(width, height, desktopMode.getBitsPerPixel(), desktopMode.getFrequency());

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

            long monitor = fullscreen ? GLFW.glfwGetPrimaryMonitor() : 0;
            window = GLFW.glfwCreateWindow(currentMode.getWidth(), currentMode.getHeight(), "Minecraft", monitor, 0);
            if (window == 0) {
                throw new LWJGLException("Failed to create GLFW window");
            }

            if (!fullscreen) {
                GLFW.glfwSetWindowPos(window, (desktop.width() - currentMode.getWidth()) / 2, (desktop.height() - currentMode.getHeight()) / 2);
            }

            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            GLFW.glfwSwapInterval(vsync ? 1 : 0);

            // attach input callbacks
            Keyboard.attachWindow(window);
            Mouse.attachWindow(window);

            keyCallback = GLFWKeyCallback.create((win, key, scancode, action, mods) -> {
                if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT) {
                    Keyboard.pushEvent(key, action, scancode, mods);
                }
            });
            GLFW.glfwSetKeyCallback(window, keyCallback);

            mouseButtonCallback = GLFWMouseButtonCallback.create((win, button, action, mods) -> {
                readCursorPos(win);
                Mouse.pushButtonEvent(button, action == GLFW.GLFW_PRESS, cursorXBuffer[0], cursorYBuffer[0]);
            });
            GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback);

            cursorPosCallback = GLFWCursorPosCallback.create((win, xpos, ypos) -> Mouse.pushMoveEvent(xpos, ypos));
            GLFW.glfwSetCursorPosCallback(window, cursorPosCallback);

            cursorEnterCallback = GLFWCursorEnterCallback.create((win, entered) -> Mouse.onCursorEntered(entered));
            GLFW.glfwSetCursorEnterCallback(window, cursorEnterCallback);

            scrollCallback = GLFWScrollCallback.create((win, xoff, yoff) -> {
                readCursorPos(win);
                Mouse.pushWheelEvent(yoff, cursorXBuffer[0], cursorYBuffer[0]);
            });
            GLFW.glfwSetScrollCallback(window, scrollCallback);

            windowFocusCallback = GLFWWindowFocusCallback.create((win, focused) -> Mouse.onWindowFocusChanged(focused));
            GLFW.glfwSetWindowFocusCallback(window, windowFocusCallback);

            refreshDimensions();
            lastSyncNanos = System.nanoTime();
            GL11.glViewport(0, 0, width, height);
        } catch (Throwable t) {
            cleanupAfterCreateFailure();
            if (t instanceof LWJGLException) {
                throw (LWJGLException)t;
            }
            throw new LWJGLException("Failed to initialize GLFW display", t);
        }
    }

    public static boolean isCreated() {
        return window != 0;
    }

    public static void destroy() {
        if (window != 0) {
            GLFW.glfwMakeContextCurrent(0);
            GL.setCapabilities(null);

            if (keyCallback != null) {
                keyCallback.free();
                keyCallback = null;
            }
            if (mouseButtonCallback != null) {
                mouseButtonCallback.free();
                mouseButtonCallback = null;
            }
            if (windowFocusCallback != null) {
                windowFocusCallback.free();
                windowFocusCallback = null;
            }
            if (cursorEnterCallback != null) {
                cursorEnterCallback.free();
                cursorEnterCallback = null;
            }
            if (cursorPosCallback != null) {
                cursorPosCallback.free();
                cursorPosCallback = null;
            }
            if (scrollCallback != null) {
                scrollCallback.free();
                scrollCallback = null;
            }
            GLFW.glfwDestroyWindow(window);
            window = 0;
            GLFW.glfwTerminate();
        }

        if (errorCallback != null) {
            GLFW.glfwSetErrorCallback(null);
            errorCallback.free();
            errorCallback = null;
        }

        Keyboard.reset();
        Mouse.reset();
        wasResized = false;
        lastSyncNanos = System.nanoTime();
    }

    public static boolean isCloseRequested() {
        return isCreated() && GLFW.glfwWindowShouldClose(window);
    }

    public static void setDisplayMode(DisplayMode mode) {
        windowWidth = mode.getWidth();
        windowHeight = mode.getHeight();
        width = windowWidth;
        height = windowHeight;
        currentMode = mode;
        if (!isCreated()) return;

        GLFW.glfwSetWindowSize(window, windowWidth, windowHeight);
        refreshDimensions();
        wasResized = true;
    }

    public static void setFullscreen(boolean enable) {
        fullscreen = enable;
        if (!isCreated()) return;
        GLFWVidMode vid = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (enable) {
            GLFW.glfwSetWindowMonitor(window, GLFW.glfwGetPrimaryMonitor(), 0, 0, vid.width(), vid.height(), vid.refreshRate());
        } else {
            int targetWidth = currentMode != null ? currentMode.getWidth() : windowWidth;
            int targetHeight = currentMode != null ? currentMode.getHeight() : windowHeight;
            GLFW.glfwSetWindowMonitor(window, 0, 0, 0, targetWidth, targetHeight, vid.refreshRate());
        }
        refreshDimensions();
        wasResized = true;
    }

    public static DisplayMode getDisplayMode() {
        if (currentMode == null) {
            GLFWVidMode vid = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            currentMode = new DisplayMode(vid.width(), vid.height(), vid.redBits() + vid.greenBits() + vid.blueBits(), vid.refreshRate());
        }
        return currentMode;
    }

    public static DisplayMode getDesktopDisplayMode() {
        if (desktopMode == null) {
            GLFWVidMode vid = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            desktopMode = new DisplayMode(vid.width(), vid.height(), vid.redBits() + vid.greenBits() + vid.blueBits(), vid.refreshRate());
        }
        return desktopMode;
    }

    public static DisplayMode[] getAvailableDisplayModes() {
        List<DisplayMode> list = new ArrayList<>();
        GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
        if (modes != null) {
            for (int i = 0; i < modes.limit(); i++) {
                GLFWVidMode m = modes.get(i);
                list.add(new DisplayMode(m.width(), m.height(), m.redBits() + m.greenBits() + m.blueBits(), m.refreshRate()));
            }
        } else {
            list.add(getDesktopDisplayMode());
        }
        return list.toArray(new DisplayMode[0]);
    }

    public static void setIcon(ByteBuffer[] icons) {
        if (!isCreated() || icons == null || icons.length == 0) {
            return;
        }

        int validCount = 0;
        int[] sizes = new int[icons.length];
        for (int i = 0; i < icons.length; i++) {
            ByteBuffer icon = icons[i];
            if (icon == null) {
                continue;
            }
            int bytes = icon.remaining();
            if (bytes < 4 || (bytes & 3) != 0) {
                continue;
            }
            int pixels = bytes >> 2;
            int side = (int)Math.round(Math.sqrt(pixels));
            if (side <= 0 || side * side != pixels) {
                continue;
            }
            sizes[i] = side;
            validCount++;
        }

        if (validCount == 0) {
            return;
        }

        GLFWImage.Buffer glfwIcons = GLFWImage.malloc(validCount);
        // Keep strong references until glfwSetWindowIcon returns.
        ByteBuffer[] nativeIconRefs = new ByteBuffer[validCount];
        int index = 0;
        for (int i = 0; i < icons.length; i++) {
            if (sizes[i] <= 0) {
                continue;
            }
            ByteBuffer iconData = toNativeIconBuffer(icons[i]);
            nativeIconRefs[index] = iconData;
            glfwIcons.position(index);
            glfwIcons.width(sizes[i]);
            glfwIcons.height(sizes[i]);
            glfwIcons.pixels(iconData);
            index++;
        }
        glfwIcons.position(0);
        GLFW.glfwSetWindowIcon(window, glfwIcons);
        glfwIcons.free();
    }

    public static void update() {
        if (!isCreated()) return;
        GLFW.glfwPollEvents();
        refreshDimensions();
        GLFW.glfwSwapBuffers(window);
    }

    public static void sync(int fps) {
        if (fps <= 0) return;

        long frameTime = 1000000000L / fps;
        long now = System.nanoTime();
        long target = lastSyncNanos + frameTime;

        if (target > now) {
            long sleepNanos = target - now;
            long sleepMillis = sleepNanos / 1000000L;
            int sleepNanoRemainder = (int)(sleepNanos % 1000000L);
            try {
                Thread.sleep(sleepMillis, sleepNanoRemainder);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            now = System.nanoTime();
        }

        lastSyncNanos = Math.max(target, now);
    }

    public static boolean wasResized() {
        boolean r = wasResized;
        wasResized = false;
        return r;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getWindowWidth() {
        return windowWidth;
    }

    public static int getWindowHeight() {
        return windowHeight;
    }

    public static boolean isActive() {
        return isCreated() && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    private static void refreshDimensions() {
        if (!isCreated()) {
            return;
        }

        GLFW.glfwGetWindowSize(window, windowWidthBuffer, windowHeightBuffer);
        GLFW.glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

        int newWindowWidth = Math.max(1, windowWidthBuffer[0]);
        int newWindowHeight = Math.max(1, windowHeightBuffer[0]);
        int newWidth = Math.max(1, framebufferWidth[0]);
        int newHeight = Math.max(1, framebufferHeight[0]);

        if (newWindowWidth != windowWidth || newWindowHeight != windowHeight || newWidth != width || newHeight != height) {
            windowWidth = newWindowWidth;
            windowHeight = newWindowHeight;
            width = newWidth;
            height = newHeight;
            wasResized = true;
        }
    }

    private static void readCursorPos(long win) {
        GLFW.glfwGetCursorPos(win, cursorXBuffer, cursorYBuffer);
    }

    private static void cleanupAfterCreateFailure() {
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }
        if (windowFocusCallback != null) {
            windowFocusCallback.free();
            windowFocusCallback = null;
        }
        if (cursorEnterCallback != null) {
            cursorEnterCallback.free();
            cursorEnterCallback = null;
        }
        if (cursorPosCallback != null) {
            cursorPosCallback.free();
            cursorPosCallback = null;
        }
        if (scrollCallback != null) {
            scrollCallback.free();
            scrollCallback = null;
        }
        if (window != 0) {
            GLFW.glfwMakeContextCurrent(0);
            GL.setCapabilities(null);
            GLFW.glfwDestroyWindow(window);
            window = 0;
        }
        GLFW.glfwTerminate();
        if (errorCallback != null) {
            GLFW.glfwSetErrorCallback(null);
            errorCallback.free();
            errorCallback = null;
        }
        Keyboard.reset();
        Mouse.reset();
    }

    private static ByteBuffer toNativeIconBuffer(ByteBuffer icon) {
        if (icon.isDirect()) {
            return icon.duplicate();
        }

        ByteBuffer copy = ByteBuffer.allocateDirect(icon.remaining());
        copy.put(icon.duplicate());
        copy.flip();
        return copy;
    }
}
