# 窗口切换黑屏跳屏修复

## 问题现象

窗口最大化/还原时，屏幕会短暂闪黑一帧。

## 根本原因

Windows DWM 合成器在窗口尺寸变化时需要立即显示新尺寸的内容，但游戏主循环还没来得及渲染出新尺寸的帧。

之前的代码流程：

```
Frame N:
  渲染到 FBO → blit FBO 到后缓冲 → swap（旧帧可见）
  → updateDisplay() 中 checkWindowResize()
  → 检测到新尺寸 → resize() → resizeFramebuffer() → framebufferClear()
  → FBO 被清成黑色

Frame N+1:
  FBO 已经是黑的 → 渲染新帧 → blit → swap
```

关键问题有两个：

1. `updateDisplay()` 在 `glfwSwapBuffers()` 之后调用 `checkWindowResize()`，导致 FBO 在 swap 后被重建并清黑
2. GLFW 的 `windowSizeCallback` / `framebufferSizeCallback` / `windowRefreshCallback` 触发时，游戏没有立即呈现当前帧，DWM 合成器拿不到有效内容

## 修复方案

### 1. 从 `updateDisplay()` 移除 `checkWindowResize()`

**文件**: `src/net/minecraft/client/Minecraft.java`

```java
// 修改前
public void updateDisplay() {
    this.mcProfiler.startSection("display_update");
    Display.update();
    this.mcProfiler.endSection();
    this.checkWindowResize();  // ← swap 后重建 FBO，清黑
}

// 修改后
public void updateDisplay() {
    this.mcProfiler.startSection("display_update");
    Display.update();
    this.mcProfiler.endSection();
    // checkWindowResize 已移除
    // resize 由帧起始（line 1126）和 tick 后（line 1182）的调用处理
}
```

resize 检测仍然在游戏循环的两个安全位置执行：
- 帧起始 `processMessages()` 之后
- tick 循环之后

这两个位置都在渲染之前，FBO 重建后会立即被渲染覆盖，不会出现黑帧。

### 2. 注册 GLFW 回调即时呈现处理器

**文件**: `src/org/lwjgl/opengl/Display.java`

新增 `refreshPresentHandler` 机制：

```java
private static Runnable refreshPresentHandler;

public static void setRefreshPresentHandler(Runnable handler) {
    refreshPresentHandler = handler;
}

public static long getWindow() {
    return window;
}
```

三个 GLFW 回调都触发 handler：

- `windowSizeCallback` — 窗口逻辑尺寸变化（最大化/还原/拖边框）
- `framebufferSizeCallback` — 帧缓冲像素尺寸变化（DPI 变化）
- `windowRefreshCallback` — 系统请求重绘（窗口被遮挡后恢复等）

```java
windowSizeCallback = GLFWWindowSizeCallback.create((win, w, h) -> {
    // ... 更新尺寸 ...
    refreshDimensions();
    markRefreshRequested("window-size-callback");
    Runnable handler = refreshPresentHandler;
    if (handler != null) {
        handler.run();  // ← 立即呈现当前帧
    }
});
```

**文件**: `src/net/minecraft/client/Minecraft.java`

注册处理器，在回调中立即将当前 FBO 内容 blit 到屏幕并 swap：

```java
private void registerResizeRefreshHandler() {
    final Minecraft mc = this;
    Display.setRefreshPresentHandler(new Runnable() {
        public void run() {
            if (mc.framebufferMc == null) return;
            int presentW = Math.max(1, Display.getWidth());
            int presentH = Math.max(1, Display.getHeight());
            mc.framebufferMc.framebufferRenderExt(presentW, presentH, true);
            GLFW.glfwSwapBuffers(Display.getWindow());
        }
    });
}
```

在 `framebufferMc` 创建后立即注册。

## 效果

- 窗口尺寸变化时，GLFW 回调立即将上一帧内容（可能略有拉伸）呈现到新尺寸窗口
- DWM 合成器始终有有效帧可显示，不再出现黑屏
- FBO 重建只在帧起始时发生，重建后立即渲染覆盖，无黑帧泄漏

## 涉及文件

| 文件 | 改动 |
|------|------|
| `src/net/minecraft/client/Minecraft.java` | 移除 `updateDisplay()` 中的 `checkWindowResize()`；新增 `registerResizeRefreshHandler()` |
| `src/org/lwjgl/opengl/Display.java` | 新增 `refreshPresentHandler` / `getWindow()` / `setRefreshPresentHandler()`；三个回调触发 handler |
