# Project: Minecraft 1.8.9 Client Mod (MCP 9.18)

## Overview

基于 MCP 9.18 反编译的 Minecraft 1.8.9 客户端模组项目。使用 NanoVG 矢量渲染引擎构建自定义 UI 层，已从 LWJGL 2 迁移至 LWJGL 3。Java 8 源码级别，手动 javac 编译（无 Gradle/Maven）。

## 目录结构

```
src/
├── client/                    # 自定义客户端模组代码
│   ├── auth/                 # Microsoft OAuth 认证 + Xbox Live 登录
│   │   └── gui/              # 账号管理 GUI
│   ├── bridge/               # 只读 Minecraft 运行时桥接层（GameBridge, PlayerBridge, WorldBridge）
│   ├── command/              # 本地聊天命令系统（.client 前缀）
│   ├── config/               # JSON 配置管理（分区存储 + 脏标记 + 原子写入）
│   ├── core/                 # ClientBootstrap 主入口 + ClientInfo + BuildInfo
│   ├── event/                # 同步事件总线（快照分发，零分配）
│   ├── hud/                  # HUD 元素系统 + 可视化编辑器
│   ├── i18n/                 # 国际化（en_us, zh_cn, ja_jp）
│   ├── module/               # 模块系统框架 + 22 个内置模块
│   ├── render/               # 2D 渲染上下文（NanoVGContext, RenderContext2D）
│   ├── runtime/lwjgl/        # LWJGL 兼容层（DisplayDefaults）
│   ├── setting/              # 设置框架（14 种类型）
│   └── ui/                   # NanoVG 屏幕（ClickGui, Settings, ScaleEdit）
│       └── template/         # UI 组件模板（Slider, TextInput, ScreenKit）
├── dwgx/                      # 共享工具库
│   ├── foundation/           # 数学、动画、输入、运动、旋转、射线、颜色等领域工具
│   ├── modulekit/            # 模块辅助（PulseMath, InventoryProbe, SightProbe）
│   ├── nano/                 # NanoVG 渲染工具（字体、主题、调色板、UI 绘制）
│   └── ui/ext/               # 主菜单扩展（背景场景、MenuFX、Shader、Bing 壁纸）
├── net/minecraft/            # 反编译的原版 Minecraft（含定向修改）
└── resources/                # 字体与多语言资源（en_us, zh_cn, ja_jp）
```

## 核心架构

### 启动流程

```
Main.main() → Minecraft 实例化 → Minecraft.run() 主循环
  → ClientBootstrap.instance().initialize(configRoot)
    → I18nManager.reload()
    → ConfigManager 初始化
    → ModuleRegistry.registerBuiltins() → 注册 22 个模块
    → HudRegistry.registerBuiltins() → 注册 HUD 元素
    → ConfigManager.loadAll() → 加载分区配置
    → 安装 JVM ShutdownHook（保存配置）
    → 启动 15 秒自动保存定时器
```

### ClientBootstrap（单例中枢）

`client.core.ClientBootstrap` 是整个模组的核心调度器，持有所有子系统：

- `EventBus` — 事件分发
- `ModuleManager` — 模块生命周期
- `HudManager` — HUD 渲染
- `ConfigManager` — 配置持久化
- `I18nManager` — 国际化
- `ClientCommandManager` — 本地命令
- `ShutdownHook` — 关闭时保存

钩子入口（由修改后的 Minecraft 类调用）：

| 钩子 | 调用点 | 作用 |
|------|--------|------|
| `onTick(pre/post)` | `Minecraft.runTick()` | 游戏逻辑 tick |
| `onRender2D(RenderContext2D)` | `Minecraft.updateDisplay()` | 2D 渲染阶段 |
| `onKey(keyCode, pressed)` | `Minecraft.processKeyboardInput()` | 键盘输入 |
| `onMouse(button, x, y, pressed)` | `Minecraft.processMouseInput()` | 鼠标输入 |
| `onPacketSend(Packet)` | `NetworkManager.sendPacket()` | 出站数据包拦截 |
| `onPacketReceive(Packet)` | `NetworkManager.channelRead0()` | 入站数据包拦截 |

### 渲染管线（onRender2D 内部顺序）

```
1. EventBus.post(Render2DEvent)     — 事件监听器渲染
2. ModuleManager.onRender2D()       — 模块渲染
3. HudManager.render()              — HUD 元素渲染（仅在世界中）
4. NanoRenderableScreen.renderNano() — 当前 Nano 屏幕渲染
```

每个阶段独立异常隔离，单个阶段崩溃不影响其他阶段。

## 事件系统

`client.event.EventBus` — 同步事件总线，快照分发模式。

- 注册方式：直接 `register(Class, Listener)` 或注解 `@EventLink` 字段扫描
- 分发时零内存分配（volatile 快照数组，仅在注册/注销时重建）
- 支持优先级排序（数值越高越先执行）
- 异常安全：单个监听器失败不中断其他监听器

事件类型：`TickEvent`（PRE/POST）、`Render2DEvent`、`KeyEvent`（可取消）、`MouseEvent`、`MotionUpdateEvent`

## 模块系统

### 基类与生命周期

`client.module.Module` 基类，生命周期钩子：

```
onEnable() → onTick() [每帧] → onRender2D(context) → onKey(event) → onPacketSend/Receive(packet) → onDisable()
```

### 内置模块（22 个）

| 模块 | 分类 | 功能 |
|------|------|------|
| `ClickGuiModule` | CLIENT | 模块设置 GUI |
| `AccountManagerModule` | CLIENT | Microsoft 账号管理 |
| `HudEditModule` | RENDER | HUD 元素定位编辑器 |
| `UiScaleEditModule` | CLIENT | UI 缩放调节/布局编辑入口 |
| `IRCGuiModule` | CLIENT | 聊天覆盖层开关 |
| `AutoClickerModule` | COMBAT | 自动点击 |
| `KillAuraModule` | COMBAT | 实体目标攻击 |
| `VelocityModule` | COMBAT | 击退控制 |
| `EagleModule` | MOVEMENT | 边缘自动蹲下 |
| `KeepSprintModule` | MOVEMENT | 保持冲刺状态 |
| `NoFallModule` | MOVEMENT | 摔落保护 |
| `SpeedModule` | MOVEMENT | 移动速度增强 |
| `FlyModule` | MOVEMENT | 飞行控制 |
| `NoSlowModule` | MOVEMENT | 使用物品减速抑制 |
| `SprintModule` | MOVEMENT | 自动冲刺 |
| `NoPushModule` | MOVEMENT | 推力抑制 |
| `AutoToolModule` | WORLD | 自动选择最佳工具 |
| `StealerModule` | WORLD | 容器自动拾取 |
| `FastPlaceModule` | WORLD | 快速放置 |
| `AntiVoidModule` | WORLD | 防掉虚空 |
| `SkinChangerModule` | MISC | 本地皮肤覆盖 |
| `InventoryMoveModule` | MISC | 背包内移动物品 |

### 设置框架（14 种类型）

`BoolSetting`、`IntSetting`、`FloatSetting`、`NumberSetting`、`StringSetting`、`EnumSetting`、`ColorSetting`（RGBA + 彩虹模式）、`KeybindSetting`、`BindSetting`、`MultiSelectSetting`、`SettingGroup`、`ColorValue`、`Visibility`、`Setting`（基类）

设置通过 `SettingGroup` 组织，支持条件可见性（`Visibility`）。

## 配置系统

`client.config.ConfigManager` — 分区 JSON 持久化。

### 存储结构

```
config/client/
├── profiles/<profile-name>/
│   ├── client/client.json       # 客户端设置、UI 布局、动画配置
│   ├── modules/modules.json     # 模块启用状态、快捷键、设置值
│   └── hud/hud.json             # HUD 元素位置、缩放、设置
└── accounts/account_manager.json # 账号数据（含刷新令牌）
```

### 关键特性

- 分区脏标记：仅保存变更的区段
- 原子写入：防止崩溃时配置损坏
- Schema 版本控制：版本变更时自动迁移
- 15 秒自动保存 + JVM 关闭钩子保存
- 多配置文件支持
- 旧版扁平结构兼容回退

### 身份冻结合约

模块 ID（`Module#getId`）、设置键（`Setting#getKey`）、持久化枚举常量均受 `docs/config-identity-freeze.md` 约束，修改需显式迁移。

## HUD 系统

`client.hud.HudElement` 基类 + `HudManager` 注册/渲染调度。

- 变换系统：`HudTransform`（Anchor 9 点锚定 + Dock 屏幕边缘停靠 + 偏移 + 缩放）
- 布局数学：`HudLayoutMath` 计算实际屏幕坐标
- 网格吸附：`SnapGrid` + `SnapLine`
- 可视化编辑器：`HudEditorScreen`（拖拽/缩放/锚点选择/实时预览）
- 内置元素：`HudFpsElement`（FPS 计数器）
- 选择模型：`SelectionModel` 管理编辑器中的元素选中状态

## UI / NanoVG 渲染

### NanoVG 上下文管理

NanoVG 帧生命周期由 `Minecraft` 类集中管理（非模块自行管理）：

```
beginNanoUiFrame() → 清理 GL 状态 → 开始帧
  → ClientBootstrap.onRender2D(RenderContext2D)
endNanoUiFrame() → 结束帧 → 恢复 GL 状态
```

失败计数超过阈值（默认 3，可通过 `dwgx.nano.failureThreshold` 系统属性配置）时禁用 Nano 直到重启。

### 屏幕体系

- `NanoRenderableScreen` — 接口，标记支持 Nano 渲染的屏幕
- `ClickGuiScreen` — 模块设置 GUI
- `ClientSettingsScreen` — 客户端全局设置
- `UIScaleEditScreen` — UI 缩放调节
- `HudEditorScreen` — HUD 编辑器

### UI 组件库（template/）

`NanoScreenKit`（通用构建块）、`NanoSliderController`（滑块）、`NanoTextInput`（文本输入）、`UiAnimProfile`（动画配置）、`UiLayoutProfile`（布局配置）

### 可复用组件（component/）

- `NanoComponent` — 组件基类（位置/尺寸/可见性/动画键/事件处理）
- `NanoSlider` — 滑块（拖拽状态 + 值回调 + 动画渲染）
- `NanoToggle` — 开关切换（滑动动画 + 状态回调）
- `NanoColorPicker` — HSV 颜色选择器（SV 面板 + 色相条 + 透明度条）
- `NanoRow` — 动画行（hover/select 状态 + 交错显示）
- `NanoPanel` — 滚动面板（内容裁剪 + 滚动条 + 动画滚动）
- `NanoDropdown` — 下拉选择器（展开/折叠动画 + 选项列表）
- `NanoNumberInput` — 数值输入框（内联编辑 + 验证 + 光标）

### dwgx/nano 渲染工具

- `NanoRenderUtils` — 矩形、圆角、文字等绘制工具
- `NanoFontBook` — 字体加载与管理
- `NanoPalette` / `NanoTheme` / `NanoThemes` — 主题与调色板
- `NanoUi` — 高级 UI 绘制封装
- `UiMath` — UI 数学工具

## 主菜单扩展系统

`dwgx.ui.ext.MainMenuBackgroundRuntime` — 可插拔背景场景运行时。

内置场景：
- `PassiveBackgroundScene` — 默认静态背景
- `BingWallpaperFetcher` — 异步获取 Bing 每日壁纸
- `FlappyBirdBackgroundScene` — Flappy Bird 小游戏
- `NyanCatBackgroundScene` — Nyan Cat 动画
- `KirbyJumpBackgroundScene` — Kirby 跳跃游戏
- `AtariSokobanBackgroundScene` — 推箱子游戏
- `ExternalFileShaderBackgroundScene` — 外部 GLSL 着色器
- `BuiltinMainMenuGlslShader` — 内置 CRT 着色器
- `MainMenuSplashShader` — 启动画面着色器

MenuFX 子系统：`MenuFxController`（交互控制）+ `MenuFxRenderer`（Nano 渲染）+ `MenuFxState`（状态）+ `MenuFxLayout`（布局）

## 认证系统

`client.auth.MicrosoftAuthService` — Microsoft OAuth2 设备码流程。

```
设备码授权 → OAuth 令牌 → Xbox Live 认证 → XSTS 令牌 → Minecraft 登录 → 玩家档案
```

- 支持刷新令牌持久化（离线重登录）
- 皮肤/披风本地覆盖（通过 `AbstractClientPlayer` 钩子）
- 账号数据存储在 `config/client/accounts/account_manager.json`

## 命令系统

`client.command.ClientCommandManager` — 本地聊天命令。

- 前缀：`.client`
- 语法：`.client <module> [enable|disable|toggle|status|bind|locale]`
- Tab 补全：模块名 + 动作
- 语言切换：`.client locale <locale>`

## Bridge 层

`client.bridge` — 只读 Minecraft 运行时桥接，提供安全的游戏状态访问：

- `MinecraftBridge` — Minecraft 实例访问
- `GameBridge` — 游戏状态查询
- `PlayerBridge` — 玩家状态查询
- `WorldBridge` — 世界状态查询

## dwgx/foundation 领域工具

| 包 | 核心类 | 用途 |
|---|--------|------|
| `math` | `ScalarMath` | 标量数学工具 |
| `animation` | `AnimationDomain` | 缓动/动画状态机 |
| `input` | `InputSnapshot` | 输入状态快照 |
| `motion` | `MotionDomain` | 运动计算 |
| `rotation` | `RotationDomain` | 旋转计算（用于数据包级旋转调整） |
| `raycast` | `RaycastDomain` | 射线检测 |
| `render` | `ColorMath` | 颜色数学 |
| `time` | `TickClock` | Tick 时钟 |
| `inventory` | `HotbarSnapshot` | 快捷栏快照 |

## dwgx/modulekit 模块辅助

- `ModuleKit` — 模块通用工具
- `PulseMath` — 脉冲/节奏数学
- `InventoryProbe` — 背包探测
- `SightProbe` — 视线探测

## 原版 Minecraft 修改清单

所有 `net/` 和 `org/` 修改必须记录在 `docs/net-org-patch-manifest.md`。

| 文件 | 修改类型 | 目的 |
|------|----------|------|
| `Minecraft.java` | 提取/委托 + 渲染守卫 | ClientBootstrap 钩子 + NanoVG 帧管理 |
| `GuiScreen.java` | 集成钩子 | 路由 `.client` 命令 |
| `GuiChat.java` | 集成钩子 | 本地命令补全 |
| `GuiMainMenu.java` | UI 扩展 | 可替换背景 + MenuFX |
| `AbstractClientPlayer.java` | 纹理覆盖 | 本地皮肤/披风替换 |
| `NetHandlerPlayClient.java` | 菜单重连 | 断线后返回扩展主菜单 |
| `NetworkManager.java` | 数据包钩子 | 出入站数据包拦截 |
| `C03PacketPlayer.java` | API 扩展 | yaw/pitch/onGround 修改器 |
| `C07PacketPlayerDigging.java` | API 扩展 | status/position/facing 修改器 |
| `Framebuffer.java` | 稳定性修复 | 缓冲区过渡守卫 |
| `Main.java` | 启动兼容 | 常量提取（DisplayDefaults） |
| `Display.java` | 兼容垫片 | LWJGL3 桥接稳定性 |

## 构建与质量检查

### 编译

```powershell
# JDK 8, 手动 javac
javac -source 1.8 -target 1.8 -cp "lib/*" -d out/classes src/**/*.java
```

### 依赖（lib/）

- `authlib-1.5.21.jar` — Minecraft 认证
- `lwjgl3-3.3.3.jar` + natives — OpenGL/音频
- `netty-4.0.23.jar` — 网络
- `gson-2.8.0.jar` — JSON
- `log4j-core-2.0-beta9.jar` — 日志

### 质量脚本

| 脚本 | 用途 |
|------|------|
| `check-all.ps1` | 运行全部检查 |
| `check-encoding.ps1` | UTF-8 编码验证 |
| `check-render-rules.ps1` | NanoVG 所有权规则验证 |
| `check-checkstyle.ps1` | 代码风格检查 |
| `check-spotbugs.ps1` | Bug 检测 |
| `check-refactor-guards.ps1` | 重构策略执行 |
| `check-smoke-tests.ps1` | 轻量自动化测试（`src/test`） |
| `generate-config-freeze.ps1` | 生成配置身份冻结快照 |
| `package-pcl2.ps1` | PCL2 启动器打包 |

## 合约与策略

### 重构合约（docs/refactor-contract.md）

1. 保持用户可见行为稳定
2. 保持持久化标识符稳定（模块 ID、设置键、枚举常量）
3. Java source/target 保持 1.8
4. NanoVG 帧所有权保持在 `Minecraft` 类中
5. 所有 `net/` `org/` 修改必须记录在 patch manifest
6. 优先提取/委托而非逻辑重写
7. 合并前必须通过全部 check 脚本

## 扩展指南

### 添加新模块

1. 在 `src/client/module/impl/<category>/` 创建类继承 `Module`
2. 构造函数中定义设置（`addSetting` + `addGroup`）
3. 实现生命周期钩子（`onEnable/onDisable/onTick/onRender2D`）
4. 在 `ModuleRegistry.registerBuiltins()` 中注册
5. 运行 `generate-config-freeze.ps1` 更新冻结快照

### 添加新 HUD 元素

1. 在 `src/client/hud/` 创建类继承 `HudElement`
2. 实现 `render(RenderContext2D)` 和尺寸方法
3. 在 `HudRegistry.registerBuiltins()` 中注册

### 添加新事件类型

1. 在 `src/client/event/` 创建类继承 `Event`
2. 在适当的钩子点调用 `eventBus.post()`
3. 模块通过 `@EventLink` 或直接注册监听

### 添加新设置类型

1. 在 `src/client/setting/` 创建类继承 `Setting<T>`
2. 在 `ConfigManager` 中实现序列化/反序列化
3. 在 UI 屏幕中添加对应控件


## UI 系统深度源码分析报告

### 一、当前 UI 架构总览

#### 1.1 文件规模与复杂度

| 文件 | 行数（估算） | 职责 |
|------|-------------|------|
| `ClickGuiScreen.java` | ~4030 | 模块管理主界面（God Class） |
| `ClientSettingsScreen.java` | ~2650 | 客户端全局设置界面 |
| `UIScaleEditScreen.java` | ~1570 | UI 缩放/布局调节界面 |
| `NanoTextInput.java` | ~470 | 文本输入模板（选区/光标/滚动） |
| `NanoColorPicker.java` | ~340 | HSV 颜色选择器组件 |
| `NanoDropdown.java` | ~190 | 下拉选择器组件 |
| `NanoSlider.java` | ~140 | 滑块组件 |
| `NanoToggle.java` | ~100 | 开关组件 |
| `NanoPanel.java` | ~110 | 滚动面板组件 |
| `NanoRow.java` | ~50 | 动画行组件 |
| `NanoNumberInput.java` | ~170 | 数值输入组件 |
| `NanoComponent.java` | ~90 | 组件基类 |
| `UiAnimationBus.java` | ~160 | 全局动画通道管理 |
| `UiAnimation.java` | ~150 | 缓动/弹簧动画算法 |
| `UiAnimProfile.java` | ~120 | 动画配置（速度/平滑/类型） |
| `UiWindowState.java` | ~280 | 窗口拖拽/缩放状态机 |
| `NanoTheme.java` | ~130 | 主题数据（27 个颜色槽 + 4 个圆角） |
| `NanoThemes.java` | ~120 | 主题工厂（基于调色板生成） |
| `NanoRenderUtils.java` | ~230 | NanoVG 底层绘制工具 |
| `NanoUi.java` | ~160 | 高级 UI 绘制封装 |

#### 1.2 架构层次

```
┌─────────────────────────────────────────────────────┐
│  Screen 层（ClickGuiScreen / ClientSettingsScreen）  │  ← 巨型单体类
├─────────────────────────────────────────────────────┤
│  Component 层（NanoSlider / NanoToggle / ...）        │  ← 已提取但未充分使用
├─────────────────────────────────────────────────────┤
│  Template 层（NanoScreenKit / UiAnimationBus / ...）  │  ← 共享工具
├─────────────────────────────────────────────────────┤
│  Render 层（NanoUi / NanoRenderUtils）               │  ← NanoVG 封装
├─────────────────────────────────────────────────────┤
│  Theme 层（NanoTheme / NanoThemes / NanoPalette）    │  ← 主题系统
└─────────────────────────────────────────────────────┘
```

#### 1.3 当前设计优点

- 动画系统成熟：`UiAnimationBus` 全局通道管理 + 自动过期清理 + 帧同步，支持 4 种缓动类型（Linear/EaseOut/EaseInOut/Spring）
- 主题系统完整：27 个语义化颜色槽 + 5 种调色板（Cobalt/Mint/Amber/Ruby/Mono）+ 基于调色板自动生成
- 窗口状态机健壮：`UiWindowState` 支持 8 方向缩放 + 拖拽 + 动画过渡 + 屏幕边界约束
- 组件基类设计合理：`NanoComponent` 提供统一的 bounds/visible/animKey/事件接口
- 渲染安全：所有绘制函数都有 NaN/Infinity/零尺寸守卫
- 异常隔离：渲染管线四阶段独立 try-catch

#### 1.4 当前设计问题

**P0 — God Class 问题**
- `ClickGuiScreen` 4030 行，承担了布局计算、输入处理、渲染、状态管理、设置序列化、页面切换、滚动、拖拽、弹窗、颜色选择器、文本输入、数值输入等全部职责
- `ClientSettingsScreen` 2650 行，与 ClickGuiScreen 存在大量重复逻辑（slider 渲染、number input、color picker、scroll、transition、theme resolve）
- 内部嵌套了 `Layout`、`Rect`、`ChoicePopupData`、`CategoryEntry` 等私有类，无法复用

**P1 — 组件层未充分利用**
- `NanoSlider`、`NanoToggle`、`NanoPanel` 等组件已提取但 ClickGuiScreen 内部仍然手动绘制 slider/toggle，未使用这些组件
- 每个 Screen 各自维护 slider drag 状态、scroll 状态、number input 状态，逻辑高度重复

**P2 — 布局系统是硬编码的**
- `Layout` 类通过构造函数传入 30+ 个 `Rect`，全部在 `layout()` 方法中手动计算
- 没有声明式布局、没有 flex/grid、没有约束系统
- 窗口缩放时所有 Rect 需要重新计算，代码冗长且脆弱

**P3 — 缺少组件树/事件冒泡**
- 没有父子组件关系，没有事件冒泡/捕获机制
- 所有鼠标/键盘事件在 Screen 层手动分发到各个区域
- 弹窗（ChoicePopup、ColorPicker）的 Z 序和事件拦截靠手动 if-else 判断

**P4 — 缺少过渡/路由系统**
- 页面切换（ClickGui → Settings → UiScale）仍以 Screen 级路由为主
- 过渡数学已抽取到共享 `ScreenTransitionController`，但完整路由层（统一栈/历史/动画策略）仍未建立

**P5 — 主题系统缺少运行时切换能力**
- `NanoTheme` 是不可变对象，切换主题需要重新创建
- 没有主题过渡动画（从 A 主题平滑过渡到 B 主题）
- 调色板只有 5 种预设，不支持自定义色相

### 二、Neverlose 风格 UI 分析与超越方案

Neverlose 的 UI 特征：深色半透明窗口、左侧垂直导航栏（图标+文字）、顶部标签页切换子功能、右侧设置面板、模块化卡片布局、流畅的展开/折叠动画、搜索功能、配置文件管理。

以下方案在学习 Neverlose 的基础上，针对「符合人类使用习惯」做了大量改进。

#### 2.1 布局重构：声明式约束布局引擎

```
目标：替换手动 Rect 计算，引入轻量级约束布局

┌──────────────────────────────────────────────────────────┐
│ NanoLayoutEngine                                          │
│  ├── NanoConstraint（锚点/边距/比例/最小最大值）           │
│  ├── NanoLayoutNode（树形节点，持有约束 + 子节点列表）     │
│  ├── NanoFlexLayout（水平/垂直弹性布局）                  │
│  └── NanoLayoutResolver（约束求解 → 输出 Rect 数组）      │
└──────────────────────────────────────────────────────────┘

用法示例：
  NanoLayoutNode root = flex(VERTICAL)
      .child(header().height(36).fixed())
      .child(flex(HORIZONTAL)
          .child(sidebar().width(0.18f).minWidth(140))
          .child(moduleList().width(0.32f).minWidth(180))
          .child(settingsPanel().flex(1)))
      .child(statusBar().height(24).fixed());
  Rect[] rects = resolver.solve(root, windowRect);
```

**为什么比 Neverlose 更好：** Neverlose 的布局也是固定比例，窗口缩放时控件间距不自然。约束布局能让 UI 在任意窗口尺寸下都保持合理的比例和间距。

#### 2.2 组件树重构：引入 NanoView 树

```
目标：建立父子组件树，支持事件冒泡和自动布局

NanoView（新基类，替代 NanoComponent）
  ├── children: List<NanoView>
  ├── layout: NanoConstraint
  ├── computedRect: Rect（由布局引擎计算）
  ├── render(vg, stack, theme, profile)  — 自动裁剪 + 递归渲染子节点
  ├── dispatchMouse(event)              — 事件冒泡/捕获
  ├── dispatchKey(event)                — 焦点链传递
  ├── zIndex: int                       — Z 序排序
  └── opacity / visibility / enabled    — 状态控制

预制视图：
  NanoWindow       — 可拖拽/缩放窗口容器
  NanoSidebar      — 垂直导航栏（图标 + 文字 + 选中指示器）
  NanoTabBar       — 水平标签页栏
  NanoScrollView   — 滚动容器（替代 NanoPanel）
  NanoCard         — 卡片容器（圆角 + 阴影 + 内边距）
  NanoPopover      — 弹出层（自动定位 + 遮罩 + 点击外部关闭）
  NanoToast        — 轻量提示（自动消失）
  NanoSearchBar    — 搜索输入框（实时过滤）
  NanoTooltip      — 悬浮提示（延迟显示 + 跟随鼠标）
```

**为什么比 Neverlose 更好：** Neverlose 没有 tooltip 和 toast 系统，用户不知道某个设置具体做什么。组件树让事件处理自动化，不再需要手动 if-else 分发。

#### 2.3 新 ClickGui 布局方案

```
┌─────────────────────────────────────────────────────────────┐
│  ▼ 顶栏：客户端名称 + 搜索框 + 配置文件选择 + 最小化/关闭  │
├────────┬──────────────┬─────────────────────────────────────┤
│        │              │                                     │
│ 导航栏  │  模块列表     │  设置面板                           │
│ (图标)  │  (卡片网格    │  (分组折叠 + 控件)                  │
│        │   或列表视图)  │                                     │
│ Combat │              │  ┌─ 设置组标题 ──────────────────┐  │
│ Move   │  ┌────────┐  │  │ 开关    [名称]        [ON/OFF]│  │
│ World  │  │ Module │  │  │ 滑块    [名称]  ═══●═══  [值] │  │
│ Misc   │  │  Card  │  │  │ 下拉    [名称]     [选项 ▼]   │  │
│ Client │  └────────┘  │  │ 颜色    [名称]     [■ 预览]   │  │
│        │              │  │ 按键    [名称]     [按键绑定]  │  │
│ ────── │              │  └────────────────────────────────┘  │
│ 设置   │              │                                     │
│ 主题   │              │  ┌─ 颜色选择器（浮动面板）─────────┐  │
│        │              │  │  [SV面板] [色相条] [透明度条]   │  │
├────────┴──────────────┴──┴──────────────────────────────────┤
│  ▼ 状态栏：模块启用数 | FPS | 当前配置文件 | 版本号          │
└─────────────────────────────────────────────────────────────┘
```

#### 2.4 人性化交互改进清单

| 改进项 | 当前状态 | 目标 | 为什么重要 |
|--------|---------|------|-----------|
| 全局搜索 | 无 | 顶栏搜索框，实时过滤模块+设置 | 当前已 22 个模块，继续增长后检索成本更高 |
| Tooltip | 无 | 悬浮 0.5s 后显示设置说明 | 用户不知道设置具体做什么 |
| 设置分组折叠 | 有分组标题但不可折叠 | 点击组标题折叠/展开 | 减少视觉噪音 |
| 面包屑导航 | 无 | 顶栏显示 Category > Module 路径 | 用户知道自己在哪 |
| 快捷键提示 | 部分 | 每个模块行右侧显示绑定键 | 一目了然 |
| 撤销/重做 | 无 | Ctrl+Z/Ctrl+Y 撤销设置修改 | 误操作可恢复 |
| 设置预设 | 无 | 每个模块支持保存/加载预设 | 快速切换配置 |
| 拖拽排序 | 无 | 模块列表支持拖拽自定义排序 | 个性化 |
| 右键菜单 | 无 | 模块右键：启用/禁用/绑定/重置/复制设置 | 快捷操作 |
| 动画曲线预览 | 无 | 动画设置旁显示实时曲线预览 | 直观理解动画效果 |
| 响应式布局 | 部分（可缩放） | 小窗口自动隐藏侧栏，切换为汉堡菜单 | 适配各种分辨率 |
| 键盘导航 | 无 | Tab 切换焦点，方向键选择，Enter 确认 | 无障碍 + 效率 |
| 模块卡片视图 | 仅列表 | 列表/网格双视图切换 | 视觉偏好 |
| 状态栏 | 无 | 底部显示启用模块数、FPS、配置名 | 信息一览 |

#### 2.5 主题系统升级

```
当前：5 种预设调色板，不可变 NanoTheme

升级方案：
  NanoThemeEngine
    ├── 支持自定义主色相（0-360 色轮选择）
    ├── 自动生成完整主题（基于主色相 + 亮度/饱和度偏移）
    ├── 主题过渡动画（切换时所有颜色平滑插值）
    ├── 亮色/暗色模式切换
    ├── 毛玻璃效果（backdrop blur，NanoVG 不原生支持，
    │   可通过降采样 + 高斯模糊 + 混合实现近似效果）
    └── 主题导入/导出（JSON）
```

#### 2.6 实施路线图

**Phase 1 — 基础设施（不改变用户可见行为）**
1. 提取 `Rect` 为顶级公共类 `client.ui.layout.UiRect`
2. 提取 `Layout` 计算逻辑为 `ClickGuiLayoutResolver`
3. 提取 ClickGuiScreen 中的 slider/toggle/number-input/color-picker 渲染逻辑，改用已有的 `NanoSlider`/`NanoToggle`/`NanoNumberInput`/`NanoColorPicker` 组件
4. 提取 transition 逻辑为共享的 `ScreenTransitionController`
5. 提取 scroll 逻辑为共享的 `ScrollController`

**Phase 2 — 布局引擎**
1. 实现 `NanoConstraint` + `NanoLayoutNode` + `NanoFlexLayout`
2. 实现 `NanoLayoutResolver` 约束求解器
3. 将 ClickGuiScreen 的 `layout()` 方法迁移到声明式约束

**Phase 3 — 组件树**
1. 实现 `NanoView` 基类（替代 `NanoComponent`）
2. 实现事件分发系统（冒泡/捕获/焦点链）
3. 实现 `NanoScrollView`、`NanoCard`、`NanoPopover`
4. 将 ClickGuiScreen 重构为组件树组合

**Phase 4 — 人性化功能**
1. 实现 `NanoSearchBar` + 全局搜索过滤
2. 实现 `NanoTooltip` + 设置描述
3. 实现设置分组折叠动画
4. 实现面包屑导航
5. 实现右键上下文菜单
6. 实现键盘导航（Tab/方向键/Enter）

**Phase 5 — 主题与视觉**
1. 实现色轮自定义主色相
2. 实现主题过渡动画
3. 实现模块卡片网格视图
4. 实现状态栏
5. 实现近似毛玻璃效果

每个 Phase 结束后必须通过全部 check 脚本，保持用户可见行为稳定。


## GuiEdit 可视化编辑器（替代 UIScaleEdit）

### 新增文件

| 文件 | 用途 |
|------|------|
| `client/ui/layout/UiRect.java` | 共享不可变矩形类，替代各 Screen 私有 Rect |
| `client/ui/editor/GuiEditScreen.java` | 可视化 GUI 布局编辑器主屏幕 |
| `client/ui/editor/GuiPreviewRenderer.java` | 缩小版 ClickGui 骨架渲染 + 可拖拽分隔线 |

### 设计理念

旧的 `UIScaleEditScreen` 用滑块逐个调参数，用户无法直观看到效果。新的 `GuiEditScreen` 渲染一个缩小版的 ClickGui 布局骨架，用户可以直接拖拽蓝色分隔线来调整：

- 侧栏宽度（拖拽侧栏右边缘）
- 模块列表高度（拖拽模块区底边缘）
- 值列宽度（拖拽设置面板中的值列左边缘）
- 面板间距（拖拽侧栏和模块区之间的间隙）

所有修改实时反映到 `UiLayoutProfile`，下次打开 ClickGui 立即生效。

### 可拖拽分隔线（DividerZone）

| 分隔线 | 控制参数 | 范围 |
|--------|---------|------|
| `SIDEBAR_RIGHT` | `clickGuiSidebarRatio` | 15%–40% |
| `MODULES_BOTTOM` | `clickGuiModulesRatio` | 18%–55% |
| `VALUE_COL_LEFT` | `valueColWidth` | 80–200px |
| `GAP_MAJOR` | `gapMajor` | 6–24px |

### 双模式

- Visual 模式：拖拽预览中的分隔线
- Sliders 模式：传统滑块调整 Scale/Motion（保留向后兼容）

### 兼容性

- `UiScaleEditModule` 的模块 ID `"ui_scale_edit"` 和所有设置键保持不变（身份冻结合约）
- 旧的 `UIScaleEditScreen.java` 保留不删除，作为回退
- `ClickGuiScreen` 的 "UIScale" 按钮现在打开 `GuiEditScreen`
