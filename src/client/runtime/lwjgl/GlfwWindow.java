package client.runtime.lwjgl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight GLFW-backed replacement for legacy Display API usage.
 * Only methods used by Minecraft 1.8.9 are implemented.
 */
public final class GlfwWindow
{
    private static final boolean RESIZE_DEBUG = Boolean.getBoolean("dwgx.render.resizeDebug");
    private static long window = 0;
    private static GLFWErrorCallback errorCallback;
    private static boolean vsync = false;
    private static boolean resizable = true;
    private static boolean fullscreen = false;
    private static boolean wasResized = false;
    private static int refreshBudgetFrames = 0;
    private static int width = DisplayDefaults.WIDTH;
    private static int height = DisplayDefaults.HEIGHT;
    private static int windowWidth = DisplayDefaults.WIDTH;
    private static int windowHeight = DisplayDefaults.HEIGHT;
    private static GlfwWindowMode desktopMode;
    private static GlfwWindowMode currentMode;

    private static GLFWKeyCallback keyCallback;
    private static GLFWMouseButtonCallback mouseButtonCallback;
    private static GLFWCursorPosCallback cursorPosCallback;
    private static GLFWCursorEnterCallback cursorEnterCallback;
    private static GLFWScrollCallback scrollCallback;
    private static GLFWWindowFocusCallback windowFocusCallback;
    private static GLFWWindowSizeCallback windowSizeCallback;
    private static GLFWFramebufferSizeCallback framebufferSizeCallback;
    private static GLFWWindowRefreshCallback windowRefreshCallback;
    private static final int[] windowWidthBuffer = new int[1];
    private static final int[] windowHeightBuffer = new int[1];
    private static final int[] framebufferWidth = new int[1];
    private static final int[] framebufferHeight = new int[1];
    private static final int[] windowPosXBuffer = new int[1];
    private static final int[] windowPosYBuffer = new int[1];
    private static final double[] cursorXBuffer = new double[1];
    private static final double[] cursorYBuffer = new double[1];
    private static int windowedPosX;
    private static int windowedPosY;
    private static int windowedWidth = DisplayDefaults.WIDTH;
    private static int windowedHeight = DisplayDefaults.HEIGHT;
    private static boolean hasWindowedPlacement;
    private static long lastSyncNanos = System.nanoTime();
    private static boolean refreshRequested;
    private static Runnable refreshPresentHandler;
    private static final long SYNC_SLEEP_MARGIN_NANOS = 2000000L;
    private static final long SYNC_YIELD_MARGIN_NANOS = 200000L;
    private static final long SYNC_RESET_LAG_MULTIPLIER = 4L;

    private GlfwWindow() {}

    public static void setVSyncEnabled(boolean enabled)
    {
        vsync = enabled;
        if (isCreated()) {
            GLFW.glfwSwapInterval(enabled ? 1 : 0);
        }
    }

    public static void setResizable(boolean r)
    {
        resizable = r;
        if (isCreated()) {
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_RESIZABLE, r ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static void setTitle(String title)
    {
        if (isCreated()) {
            GLFW.glfwSetWindowTitle(window, title);
        }
    }


    public static void create() throws GlfwInitException
    {
        create(null);
    }

    public static void create(Object ignored) throws GlfwInitException
    {
        if (isCreated()) return;

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!GLFW.glfwInit()) {
            errorCallback.free();
            errorCallback = null;
            throw new GlfwInitException("Unable to initialize GLFW");
        }

        try {
            GLFWVidMode desktop = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            desktopMode = new GlfwWindowMode(desktop.width(), desktop.height(), desktop.redBits() + desktop.greenBits() + desktop.blueBits(), desktop.refreshRate());
            currentMode = new GlfwWindowMode(width, height, desktopMode.getBitsPerPixel(), desktopMode.getFrequency());

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

            long monitor = fullscreen ? GLFW.glfwGetPrimaryMonitor() : 0;
            window = GLFW.glfwCreateWindow(currentMode.getWidth(), currentMode.getHeight(), "Minecraft", monitor, 0);
            if (window == 0) {
                throw new GlfwInitException("Failed to create GLFW window");
            }

            if (!fullscreen) {
                GLFW.glfwSetWindowPos(window, (desktop.width() - currentMode.getWidth()) / 2, (desktop.height() - currentMode.getHeight()) / 2);
            }

            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            GLFW.glfwSwapInterval(vsync ? 1 : 0);

            // attach input callbacks
            GlfwKeyboard.attachWindow(window);
            GlfwMouse.attachWindow(window);


            keyCallback = GLFWKeyCallback.create((win, key, scancode, action, mods) -> {
                if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT) {
                    GlfwKeyboard.pushEvent(key, action, scancode, mods);
                }
            });
            GLFW.glfwSetKeyCallback(window, keyCallback);

            mouseButtonCallback = GLFWMouseButtonCallback.create((win, button, action, mods) -> {
                readCursorPos(win);
                GlfwMouse.pushButtonEvent(button, action == GLFW.GLFW_PRESS, cursorXBuffer[0], cursorYBuffer[0]);
            });
            GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback);

            cursorPosCallback = GLFWCursorPosCallback.create((win, xpos, ypos) -> GlfwMouse.pushMoveEvent(xpos, ypos));
            GLFW.glfwSetCursorPosCallback(window, cursorPosCallback);

            cursorEnterCallback = GLFWCursorEnterCallback.create((win, entered) -> GlfwMouse.onCursorEntered(entered));
            GLFW.glfwSetCursorEnterCallback(window, cursorEnterCallback);

            scrollCallback = GLFWScrollCallback.create((win, xoff, yoff) -> {
                readCursorPos(win);
                GlfwMouse.pushWheelEvent(yoff, cursorXBuffer[0], cursorYBuffer[0]);
            });
            GLFW.glfwSetScrollCallback(window, scrollCallback);

            windowFocusCallback = GLFWWindowFocusCallback.create((win, focused) -> {
                GlfwMouse.onWindowFocusChanged(focused);

                if (focused) {
                    refreshDimensions();
                    markRefreshRequested("window-focus-callback");
                    lastSyncNanos = System.nanoTime();
                }
            });
            GLFW.glfwSetWindowFocusCallback(window, windowFocusCallback);


            windowSizeCallback = GLFWWindowSizeCallback.create((win, newWindowWidth, newWindowHeight) -> {
                if (newWindowWidth <= 0 || newWindowHeight <= 0) {
                    return;
                }

                windowWidth = newWindowWidth;
                windowHeight = newWindowHeight;
                refreshDimensions();
                markRefreshRequested("window-size-callback");
                Runnable handler = refreshPresentHandler;
                if (handler != null) {
                    handler.run();
                }
            });
            GLFW.glfwSetWindowSizeCallback(window, windowSizeCallback);

            framebufferSizeCallback = GLFWFramebufferSizeCallback.create((win, newFramebufferWidth, newFramebufferHeight) -> {
                if (newFramebufferWidth <= 0 || newFramebufferHeight <= 0) {
                    return;
                }

                width = newFramebufferWidth;
                height = newFramebufferHeight;
                markRefreshRequested("framebuffer-size-callback");
                Runnable handler = refreshPresentHandler;
                if (handler != null) {
                    handler.run();
                }
            });
            GLFW.glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);

            windowRefreshCallback = GLFWWindowRefreshCallback.create((win) -> {
                refreshDimensions();
                markRefreshRequested("window-refresh-callback");
                Runnable handler = refreshPresentHandler;
                if (handler != null) {
                    handler.run();
                }
            });
            GLFW.glfwSetWindowRefreshCallback(window, windowRefreshCallback);

            refreshDimensions();
            if (!fullscreen) {
                captureWindowedPlacement();
            }
            lastSyncNanos = System.nanoTime();
            refreshRequested = false;
            GL11.glViewport(0, 0, width, height);
            debugResize("create");

        } catch (Throwable t) {
            cleanupAfterCreateFailure();
            if (t instanceof GlfwInitException) {
                throw (GlfwInitException)t;
            }
            throw new GlfwInitException("Failed to initialize GLFW display", t);
        }
    }

    public static boolean isCreated()
    {
        return window != 0;
    }

    public static void destroy()
    {
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
            if (windowSizeCallback != null) {
                windowSizeCallback.free();
                windowSizeCallback = null;
            }
            if (framebufferSizeCallback != null) {
                framebufferSizeCallback.free();
                framebufferSizeCallback = null;
            }
            if (windowRefreshCallback != null) {
                windowRefreshCallback.free();
                windowRefreshCallback = null;
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

        GlfwKeyboard.reset();
        GlfwMouse.reset();
        wasResized = false;
        refreshRequested = false;
        refreshPresentHandler = null;
        hasWindowedPlacement = false;
        lastSyncNanos = System.nanoTime();
    }

    public static boolean isCloseRequested()
    {
        return isCreated() && GLFW.glfwWindowShouldClose(window);
    }

    public static void setDisplayMode(GlfwWindowMode mode)
    {
        if (mode == null) {
            return;
        }

        debugResize("setDisplayMode:before " + mode.toString());
        windowWidth = mode.getWidth();
        windowHeight = mode.getHeight();
        width = windowWidth;
        height = windowHeight;
        currentMode = mode;

        if (!fullscreen) {
            windowedWidth = Math.max(1, windowWidth);
            windowedHeight = Math.max(1, windowHeight);
        }


        if (!isCreated()) return;

        if (fullscreen) {
            long monitor = GLFW.glfwGetPrimaryMonitor();
            GLFWVidMode desktop = GLFW.glfwGetVideoMode(monitor);
            GLFW.glfwSetWindowMonitor(
                window,
                monitor,
                0,
                0,
                Math.max(1, mode.getWidth()),
                Math.max(1, mode.getHeight()),
                resolveRefreshRate(mode, desktop)
            );
        } else {
            GLFW.glfwSetWindowSize(window, Math.max(1, windowWidth), Math.max(1, windowHeight));
        }

        refreshDimensions();
        if (!fullscreen) {
            captureWindowedPlacement();
        }
        wasResized = true;
        debugResize("setDisplayMode:after " + mode.toString());
    }

    public static void setFullscreen(boolean enable)
    {
        debugResize("setFullscreen:before enable=" + enable);

        if (enable == fullscreen && isCreated()) {
            debugResize("setFullscreen:no-op enable=" + enable);
            return;
        }

        boolean wasFullscreen = fullscreen;
        fullscreen = enable;
        if (!isCreated()) return;

        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode desktop = GLFW.glfwGetVideoMode(monitor);


        if (enable) {
            captureWindowedPlacement();
            GlfwWindowMode mode = currentMode;

            if (!wasFullscreen) {
                GlfwWindowMode desktopModeNow = getDesktopDisplayMode();
                if (mode == null || (mode.getWidth() == width && mode.getHeight() == height)) {
                    mode = desktopModeNow;
                }
            }

            if (mode == null) {
                mode = getDesktopDisplayMode();
            }

            currentMode = mode;
            GLFW.glfwSetWindowMonitor(
                window,
                monitor,
                0,
                0,
                Math.max(1, mode.getWidth()),
                Math.max(1, mode.getHeight()),
                resolveRefreshRate(mode, desktop)
            );
        } else {
            int targetWidth = hasWindowedPlacement ? windowedWidth : (currentMode != null ? currentMode.getWidth() : windowWidth);
            int targetHeight = hasWindowedPlacement ? windowedHeight : (currentMode != null ? currentMode.getHeight() : windowHeight);
            int targetPosX;
            int targetPosY;

            if (hasWindowedPlacement) {
                targetPosX = windowedPosX;
                targetPosY = windowedPosY;
            } else if (desktop != null) {
                targetPosX = (desktop.width() - targetWidth) / 2;
                targetPosY = (desktop.height() - targetHeight) / 2;
            } else {
                targetPosX = 0;
                targetPosY = 0;
            }

            GLFW.glfwSetWindowMonitor(
                window,
                0,
                targetPosX,
                targetPosY,
                Math.max(1, targetWidth),
                Math.max(1, targetHeight),
                GLFW.GLFW_DONT_CARE
            );
        }


        refreshDimensions();
        if (!fullscreen) {
            captureWindowedPlacement();
        }
        wasResized = true;
        debugResize("setFullscreen:after enable=" + enable);
    }

    public static GlfwWindowMode getDisplayMode()
    {
        if (currentMode == null) {
            GLFWVidMode vid = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            currentMode = new GlfwWindowMode(vid.width(), vid.height(), vid.redBits() + vid.greenBits() + vid.blueBits(), vid.refreshRate());
        }
        return currentMode;
    }

    public static GlfwWindowMode getDesktopDisplayMode()
    {
        if (desktopMode == null) {
            GLFWVidMode vid = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            desktopMode = new GlfwWindowMode(vid.width(), vid.height(), vid.redBits() + vid.greenBits() + vid.blueBits(), vid.refreshRate());
        }
        return desktopMode;
    }

    public static GlfwWindowMode[] getAvailableDisplayModes()
    {
        List<GlfwWindowMode> list = new ArrayList<GlfwWindowMode>();
        GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
        if (modes != null) {
            for (int i = 0; i < modes.limit(); i++) {
                GLFWVidMode m = modes.get(i);
                list.add(new GlfwWindowMode(m.width(), m.height(), m.redBits() + m.greenBits() + m.blueBits(), m.refreshRate()));
            }
        } else {
            list.add(getDesktopDisplayMode());
        }
        return list.toArray(new GlfwWindowMode[0]);
    }


    public static void setIcon(ByteBuffer[] icons)
    {
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


    public static void update()
    {
        if (!isCreated()) return;

        int iconified = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED);
        int focused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED);

        boolean hasRefreshWork = refreshRequested || wasResized || refreshBudgetFrames > 0;

        if (iconified != GLFW.GLFW_TRUE && (focused == GLFW.GLFW_TRUE || hasRefreshWork)) {
            GLFW.glfwSwapBuffers(window);
            if (refreshBudgetFrames > 0) {
                refreshBudgetFrames--;
            }
        }
        refreshDimensions();
    }

    public static void processMessages()
    {
        if (!isCreated()) return;
        GLFW.glfwPollEvents();
        refreshDimensions();
    }

    public static boolean consumeRefreshRequested()
    {
        boolean requested = refreshRequested;
        refreshRequested = false;
        return requested;
    }

    public static void sync(int fps)
    {
        if (fps <= 0) return;

        long frameTime = 1000000000L / fps;
        long now = System.nanoTime();

        if (now < lastSyncNanos - frameTime * SYNC_RESET_LAG_MULTIPLIER) {
            lastSyncNanos = now;
        }

        long target = lastSyncNanos + frameTime;

        if (target <= now) {
            lastSyncNanos = now;
            return;
        }


        while (target - now > SYNC_SLEEP_MARGIN_NANOS) {
            long sleepNanos = target - now - SYNC_YIELD_MARGIN_NANOS;

            if (sleepNanos <= 0L) {
                break;
            }

            long sleepMillis = sleepNanos / 1000000L;
            int sleepNanoRemainder = (int)(sleepNanos % 1000000L);

            if (sleepMillis <= 0L && sleepNanoRemainder <= 0) {
                break;
            }

            try {
                Thread.sleep(Math.max(0L, sleepMillis), Math.max(0, sleepNanoRemainder));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }

            now = System.nanoTime();
        }

        while (target - now > SYNC_YIELD_MARGIN_NANOS) {
            Thread.yield();
            now = System.nanoTime();
        }

        while (now < target) {
            now = System.nanoTime();
        }

        lastSyncNanos = target;
    }

    public static boolean wasResized()
    {
        boolean r = wasResized;
        wasResized = false;
        return r;
    }

    public static int getWidth()
    {
        return width;
    }

    public static int getHeight()
    {
        return height;
    }


    public static int getWindowWidth()
    {
        return windowWidth;
    }

    public static int getWindowHeight()
    {
        return windowHeight;
    }

    public static boolean isActive()
    {
        return isCreated() && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    public static long getWindow()
    {
        return window;
    }

    public static void setRefreshPresentHandler(Runnable handler)
    {
        refreshPresentHandler = handler;
    }

    public static void syncDimensions()
    {
        refreshDimensions();
    }

    private static void refreshDimensions()
    {
        if (!isCreated()) {
            return;
        }

        boolean iconified = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
        GLFW.glfwGetWindowSize(window, windowWidthBuffer, windowHeightBuffer);
        GLFW.glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

        int rawWindowWidth = windowWidthBuffer[0];
        int rawWindowHeight = windowHeightBuffer[0];
        int rawFramebufferWidth = framebufferWidth[0];
        int rawFramebufferHeight = framebufferHeight[0];


        if (iconified || rawWindowWidth <= 0 || rawWindowHeight <= 0 || rawFramebufferWidth <= 0 || rawFramebufferHeight <= 0) {
            if (RESIZE_DEBUG) {
                System.out.println(
                    "[resize-debug][GlfwWindow] refreshDimensions:skip-invalid"
                        + " iconified=" + iconified
                        + " rawWindow=" + rawWindowWidth + "x" + rawWindowHeight
                        + " rawFramebuffer=" + rawFramebufferWidth + "x" + rawFramebufferHeight
                );
            }
            return;
        }

        int newWindowWidth = rawWindowWidth;
        int newWindowHeight = rawWindowHeight;
        int newWidth = rawFramebufferWidth;
        int newHeight = rawFramebufferHeight;

        if (newWindowWidth != windowWidth || newWindowHeight != windowHeight || newWidth != width || newHeight != height) {
            windowWidth = newWindowWidth;
            windowHeight = newWindowHeight;
            width = newWidth;
            height = newHeight;
            markRefreshRequested("refreshDimensions");
        }
    }

    private static int resolveRefreshRate(GlfwWindowMode mode, GLFWVidMode desktop)
    {
        if (mode != null && mode.getFrequency() > 0) {
            return mode.getFrequency();
        }

        return desktop == null ? GLFW.GLFW_DONT_CARE : desktop.refreshRate();
    }

    private static void captureWindowedPlacement()
    {
        if (!isCreated() || fullscreen) {
            return;
        }

        GLFW.glfwGetWindowPos(window, windowPosXBuffer, windowPosYBuffer);
        windowedPosX = windowPosXBuffer[0];
        windowedPosY = windowPosYBuffer[0];
        windowedWidth = Math.max(1, windowWidth);
        windowedHeight = Math.max(1, windowHeight);
        hasWindowedPlacement = true;
    }


    private static void debugResize(String stage)
    {
        if (!RESIZE_DEBUG) {
            return;
        }

        String mode = currentMode == null ? "null" : currentMode.toString();
        System.out.println(
            "[resize-debug][GlfwWindow] " + stage
                + " | fullscreen=" + fullscreen
                + " display=" + width + "x" + height
                + " window=" + windowWidth + "x" + windowHeight
                + " mode=" + mode
        );
    }

    private static void readCursorPos(long win)
    {
        GLFW.glfwGetCursorPos(win, cursorXBuffer, cursorYBuffer);
    }

    private static void cleanupAfterCreateFailure()
    {
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
        if (windowSizeCallback != null) {
            windowSizeCallback.free();
            windowSizeCallback = null;
        }
        if (framebufferSizeCallback != null) {
            framebufferSizeCallback.free();
            framebufferSizeCallback = null;
        }
        if (windowRefreshCallback != null) {
            windowRefreshCallback.free();
            windowRefreshCallback = null;
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
        GlfwKeyboard.reset();
        GlfwMouse.reset();
        refreshRequested = false;
        hasWindowedPlacement = false;
    }

    private static void markRefreshRequested(String stage)
    {
        wasResized = true;
        refreshRequested = true;
        refreshBudgetFrames = Math.max(refreshBudgetFrames, 12);
        if (RESIZE_DEBUG) {
            debugResize(stage);
        }
    }

    private static ByteBuffer toNativeIconBuffer(ByteBuffer icon)
    {
        if (icon.isDirect()) {
            return icon.duplicate();
        }

        ByteBuffer copy = ByteBuffer.allocateDirect(icon.remaining());
        copy.put(icon.duplicate());
        copy.flip();
        return copy;
    }
}
