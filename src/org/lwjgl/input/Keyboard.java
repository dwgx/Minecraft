package org.lwjgl.input;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * GLFW-backed LWJGL2 keyboard compatibility layer.
 */
public final class Keyboard {
    private static final int MAX_EVENT_QUEUE_SIZE = 1024;
    private static final char[] SHIFTED_DIGITS = new char[] {')', '!', '@', '#', '$', '%', '^', '&', '*', '('};
    private static boolean repeatEnabled = false;
    private static boolean created = false;
    private static long windowHandle = 0;

    private static final Queue<KeyEvent> events = new ArrayDeque<KeyEvent>();
    private static KeyEvent currentEvent = null;
    private static final int[] GLFW_TO_LWJGL = new int[512];
    private static final int[] LWJGL_TO_GLFW = new int[256];
    private static final String[] KEY_NAMES = new String[256];

    static {
        Arrays.fill(LWJGL_TO_GLFW, -1);

        map(GLFW.GLFW_KEY_ESCAPE, 1, "ESCAPE");
        map(GLFW.GLFW_KEY_1, 2, "1");
        map(GLFW.GLFW_KEY_2, 3, "2");
        map(GLFW.GLFW_KEY_3, 4, "3");
        map(GLFW.GLFW_KEY_4, 5, "4");
        map(GLFW.GLFW_KEY_5, 6, "5");
        map(GLFW.GLFW_KEY_6, 7, "6");
        map(GLFW.GLFW_KEY_7, 8, "7");
        map(GLFW.GLFW_KEY_8, 9, "8");
        map(GLFW.GLFW_KEY_9, 10, "9");
        map(GLFW.GLFW_KEY_0, 11, "0");
        map(GLFW.GLFW_KEY_MINUS, 12, "-");
        map(GLFW.GLFW_KEY_EQUAL, 13, "=");
        map(GLFW.GLFW_KEY_BACKSPACE, 14, "BACK");
        map(GLFW.GLFW_KEY_TAB, 15, "TAB");

        map(GLFW.GLFW_KEY_Q, 16, "Q");
        map(GLFW.GLFW_KEY_W, 17, "W");
        map(GLFW.GLFW_KEY_E, 18, "E");
        map(GLFW.GLFW_KEY_R, 19, "R");
        map(GLFW.GLFW_KEY_T, 20, "T");
        map(GLFW.GLFW_KEY_Y, 21, "Y");
        map(GLFW.GLFW_KEY_U, 22, "U");
        map(GLFW.GLFW_KEY_I, 23, "I");
        map(GLFW.GLFW_KEY_O, 24, "O");
        map(GLFW.GLFW_KEY_P, 25, "P");
        map(GLFW.GLFW_KEY_LEFT_BRACKET, 26, "[");
        map(GLFW.GLFW_KEY_RIGHT_BRACKET, 27, "]");
        map(GLFW.GLFW_KEY_ENTER, 28, "RETURN");
        map(GLFW.GLFW_KEY_LEFT_CONTROL, 29, "LCONTROL");

        map(GLFW.GLFW_KEY_A, 30, "A");
        map(GLFW.GLFW_KEY_S, 31, "S");
        map(GLFW.GLFW_KEY_D, 32, "D");
        map(GLFW.GLFW_KEY_F, 33, "F");
        map(GLFW.GLFW_KEY_G, 34, "G");
        map(GLFW.GLFW_KEY_H, 35, "H");
        map(GLFW.GLFW_KEY_J, 36, "J");
        map(GLFW.GLFW_KEY_K, 37, "K");
        map(GLFW.GLFW_KEY_L, 38, "L");
        map(GLFW.GLFW_KEY_SEMICOLON, 39, ";");
        map(GLFW.GLFW_KEY_APOSTROPHE, 40, "'");
        map(GLFW.GLFW_KEY_GRAVE_ACCENT, 41, "`");
        map(GLFW.GLFW_KEY_LEFT_SHIFT, 42, "LSHIFT");
        map(GLFW.GLFW_KEY_BACKSLASH, 43, "\\");

        map(GLFW.GLFW_KEY_Z, 44, "Z");
        map(GLFW.GLFW_KEY_X, 45, "X");
        map(GLFW.GLFW_KEY_C, 46, "C");
        map(GLFW.GLFW_KEY_V, 47, "V");
        map(GLFW.GLFW_KEY_B, 48, "B");
        map(GLFW.GLFW_KEY_N, 49, "N");
        map(GLFW.GLFW_KEY_M, 50, "M");
        map(GLFW.GLFW_KEY_COMMA, 51, ",");
        map(GLFW.GLFW_KEY_PERIOD, 52, ".");
        map(GLFW.GLFW_KEY_SLASH, 53, "/");
        map(GLFW.GLFW_KEY_RIGHT_SHIFT, 54, "RSHIFT");
        map(GLFW.GLFW_KEY_KP_MULTIPLY, 55, "MULTIPLY");
        map(GLFW.GLFW_KEY_LEFT_ALT, 56, "LMENU");
        map(GLFW.GLFW_KEY_SPACE, 57, "SPACE");
        map(GLFW.GLFW_KEY_CAPS_LOCK, 58, "CAPITAL");

        map(GLFW.GLFW_KEY_F1, 59, "F1");
        map(GLFW.GLFW_KEY_F2, 60, "F2");
        map(GLFW.GLFW_KEY_F3, 61, "F3");
        map(GLFW.GLFW_KEY_F4, 62, "F4");
        map(GLFW.GLFW_KEY_F5, 63, "F5");
        map(GLFW.GLFW_KEY_F6, 64, "F6");
        map(GLFW.GLFW_KEY_F7, 65, "F7");
        map(GLFW.GLFW_KEY_F8, 66, "F8");
        map(GLFW.GLFW_KEY_F9, 67, "F9");
        map(GLFW.GLFW_KEY_F10, 68, "F10");

        map(GLFW.GLFW_KEY_NUM_LOCK, 69, "NUMLOCK");
        map(GLFW.GLFW_KEY_SCROLL_LOCK, 70, "SCROLL");
        map(GLFW.GLFW_KEY_KP_7, 71, "NUMPAD7");
        map(GLFW.GLFW_KEY_KP_8, 72, "NUMPAD8");
        map(GLFW.GLFW_KEY_KP_9, 73, "NUMPAD9");
        map(GLFW.GLFW_KEY_KP_SUBTRACT, 74, "SUBTRACT");
        map(GLFW.GLFW_KEY_KP_4, 75, "NUMPAD4");
        map(GLFW.GLFW_KEY_KP_5, 76, "NUMPAD5");
        map(GLFW.GLFW_KEY_KP_6, 77, "NUMPAD6");
        map(GLFW.GLFW_KEY_KP_ADD, 78, "ADD");
        map(GLFW.GLFW_KEY_KP_1, 79, "NUMPAD1");
        map(GLFW.GLFW_KEY_KP_2, 80, "NUMPAD2");
        map(GLFW.GLFW_KEY_KP_3, 81, "NUMPAD3");
        map(GLFW.GLFW_KEY_KP_0, 82, "NUMPAD0");
        map(GLFW.GLFW_KEY_KP_DECIMAL, 83, "DECIMAL");
        map(GLFW.GLFW_KEY_F11, 87, "F11");
        map(GLFW.GLFW_KEY_F12, 88, "F12");

        map(GLFW.GLFW_KEY_F13, 100, "F13");
        map(GLFW.GLFW_KEY_F14, 101, "F14");
        map(GLFW.GLFW_KEY_F15, 102, "F15");

        map(GLFW.GLFW_KEY_PAUSE, 197, "PAUSE");
        map(GLFW.GLFW_KEY_HOME, 199, "HOME");
        map(GLFW.GLFW_KEY_UP, 200, "UP");
        map(GLFW.GLFW_KEY_PAGE_UP, 201, "PRIOR");
        map(GLFW.GLFW_KEY_LEFT, 203, "LEFT");
        map(GLFW.GLFW_KEY_RIGHT, 205, "RIGHT");
        map(GLFW.GLFW_KEY_END, 207, "END");
        map(GLFW.GLFW_KEY_DOWN, 208, "DOWN");
        map(GLFW.GLFW_KEY_PAGE_DOWN, 209, "NEXT");
        map(GLFW.GLFW_KEY_INSERT, 210, "INSERT");
        map(GLFW.GLFW_KEY_DELETE, 211, "DELETE");
        map(GLFW.GLFW_KEY_LEFT_SUPER, 219, "LWIN");
        map(GLFW.GLFW_KEY_RIGHT_SUPER, 220, "RWIN");
        map(GLFW.GLFW_KEY_MENU, 221, "APPS");

        map(GLFW.GLFW_KEY_KP_ENTER, 156, "NUMPADENTER");
        map(GLFW.GLFW_KEY_RIGHT_CONTROL, 157, "RCONTROL");
        map(GLFW.GLFW_KEY_KP_DIVIDE, 181, "DIVIDE");
        map(GLFW.GLFW_KEY_RIGHT_ALT, 184, "RMENU");
        map(GLFW.GLFW_KEY_PRINT_SCREEN, 183, "SYSRQ");
    }

    private Keyboard() {}

    private static void map(int glfwKey, int lwjglKey, String name) {
        if (glfwKey >= 0 && glfwKey < GLFW_TO_LWJGL.length) {
            GLFW_TO_LWJGL[glfwKey] = lwjglKey;
        }

        if (lwjglKey >= 0 && lwjglKey < LWJGL_TO_GLFW.length) {
            LWJGL_TO_GLFW[lwjglKey] = glfwKey;
            KEY_NAMES[lwjglKey] = name;
        }
    }

    public static void attachWindow(long window) {
        windowHandle = window;
        created = true;
        events.clear();
        currentEvent = null;
    }

    public static void reset() {
        created = false;
        windowHandle = 0;
        repeatEnabled = false;
        events.clear();
        currentEvent = null;
    }

    public static boolean isCreated() {
        return created;
    }

    public static void enableRepeatEvents(boolean enable) {
        repeatEnabled = enable;
    }

    public static boolean next() {
        currentEvent = events.poll();
        return currentEvent != null;
    }

    public static boolean getEventKeyState() {
        return currentEvent != null && currentEvent.pressed;
    }

    public static int getEventKey() {
        return currentEvent != null ? currentEvent.key : 0;
    }

    public static char getEventCharacter() {
        return currentEvent != null ? currentEvent.ch : 0;
    }

    public static boolean isKeyDown(int key) {
        if (!created || windowHandle == 0) return false;

        int glfwKey = key >= 0 && key < LWJGL_TO_GLFW.length ? LWJGL_TO_GLFW[key] : -1;
        if (glfwKey < 0) {
            return false;
        }

        int state = GLFW.glfwGetKey(windowHandle, glfwKey);
        return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
    }

    public static String getKeyName(int key) {
        if (key >= 0 && key < KEY_NAMES.length && KEY_NAMES[key] != null) {
            return KEY_NAMES[key];
        }

        int glfwKey = key >= 0 && key < LWJGL_TO_GLFW.length ? LWJGL_TO_GLFW[key] : -1;
        if (glfwKey >= 0) {
            String name = GLFW.glfwGetKeyName(glfwKey, 0);
            if (name != null && !name.isEmpty()) {
                return name.toUpperCase();
            }
        }

        return Integer.toString(key);
    }

    public static boolean isRepeatEvent() {
        return currentEvent != null && currentEvent.repeat;
    }

    public static void pushEvent(int glfwKey, int action, int scancode, int mods) {
        if (!created) {
            return;
        }

        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE && action != GLFW.GLFW_REPEAT) {
            return;
        }

        if (action == GLFW.GLFW_REPEAT && !repeatEnabled) {
            return;
        }

        int lwjglKey = glfwKey >= 0 && glfwKey < GLFW_TO_LWJGL.length ? GLFW_TO_LWJGL[glfwKey] : 0;
        boolean pressed = action != GLFW.GLFW_RELEASE;
        boolean repeat = action == GLFW.GLFW_REPEAT;
        char ch = pressed ? translateCharacter(glfwKey, mods) : 0;
        enqueueEvent(new KeyEvent(lwjglKey, pressed, ch, repeat));
    }

    private static void enqueueEvent(KeyEvent event) {
        if (events.size() >= MAX_EVENT_QUEUE_SIZE) {
            events.poll();
        }
        events.add(event);
    }

    private static char translateCharacter(int glfwKey, int mods) {
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        if (glfwKey >= GLFW.GLFW_KEY_A && glfwKey <= GLFW.GLFW_KEY_Z) {
            char base = (char) ('a' + (glfwKey - GLFW.GLFW_KEY_A));
            return shift ? Character.toUpperCase(base) : base;
        }

        if (glfwKey >= GLFW.GLFW_KEY_0 && glfwKey <= GLFW.GLFW_KEY_9) {
            int idx = glfwKey - GLFW.GLFW_KEY_0;
            return shift ? SHIFTED_DIGITS[idx] : (char) ('0' + idx);
        }

        switch (glfwKey) {
            case GLFW.GLFW_KEY_SPACE:
                return ' ';
            case GLFW.GLFW_KEY_APOSTROPHE:
                return shift ? '"' : '\'';
            case GLFW.GLFW_KEY_COMMA:
                return shift ? '<' : ',';
            case GLFW.GLFW_KEY_MINUS:
                return shift ? '_' : '-';
            case GLFW.GLFW_KEY_PERIOD:
                return shift ? '>' : '.';
            case GLFW.GLFW_KEY_SLASH:
                return shift ? '?' : '/';
            case GLFW.GLFW_KEY_SEMICOLON:
                return shift ? ':' : ';';
            case GLFW.GLFW_KEY_EQUAL:
                return shift ? '+' : '=';
            case GLFW.GLFW_KEY_LEFT_BRACKET:
                return shift ? '{' : '[';
            case GLFW.GLFW_KEY_BACKSLASH:
                return shift ? '|' : '\\';
            case GLFW.GLFW_KEY_RIGHT_BRACKET:
                return shift ? '}' : ']';
            case GLFW.GLFW_KEY_GRAVE_ACCENT:
                return shift ? '~' : '`';
            case GLFW.GLFW_KEY_KP_0:
            case GLFW.GLFW_KEY_KP_1:
            case GLFW.GLFW_KEY_KP_2:
            case GLFW.GLFW_KEY_KP_3:
            case GLFW.GLFW_KEY_KP_4:
            case GLFW.GLFW_KEY_KP_5:
            case GLFW.GLFW_KEY_KP_6:
            case GLFW.GLFW_KEY_KP_7:
            case GLFW.GLFW_KEY_KP_8:
            case GLFW.GLFW_KEY_KP_9:
                return (char) ('0' + (glfwKey - GLFW.GLFW_KEY_KP_0));
            case GLFW.GLFW_KEY_KP_DECIMAL:
                return '.';
            case GLFW.GLFW_KEY_KP_ADD:
                return '+';
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                return '-';
            case GLFW.GLFW_KEY_KP_MULTIPLY:
                return '*';
            case GLFW.GLFW_KEY_KP_DIVIDE:
                return '/';
            default:
                return 0;
        }
    }

    private static final class KeyEvent {
        final int key;
        final boolean pressed;
        final char ch;
        final boolean repeat;

        KeyEvent(int key, boolean pressed, char ch, boolean repeat) {
            this.key = key;
            this.pressed = pressed;
            this.ch = ch;
            this.repeat = repeat;
        }
    }
}
