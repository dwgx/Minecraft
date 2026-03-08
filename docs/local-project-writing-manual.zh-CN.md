# 双项目本地写法手册（中文，单文档）

适用仓库：
- `d:/Project/Minecraft`
- `d:/Project/OpenMyau-main`

策略：
- 以 `Minecraft docs` 为主要规范来源。
- 以 `OpenMyau` 为现状对照来源。
- 采用“现状优先”，记录行为与风险，不强推重构。

---

## 1. 适用范围与术语

### 1.1 适用范围
- 本手册用于本地开发、阅读、改动和回归验证。
- 覆盖范围包含：
`net.minecraft.*` 顶层 25 包、`client.*`、`dwgx.*`、`myau.*`、`me.ksyz.*`。
- 手册目标是“包级 + 关键类/关键方法”，不是逐类注释文档。

### 1.2 术语
- 原版层：`src/net/minecraft/**`
- 本地扩展层（Minecraft 仓库）：`src/client/**`、`src/dwgx/**`
- OpenMyau 层：`src/main/java/myau/**`、`src/main/java/me/ksyz/**`
- 守护脚本：`scripts/check-*.ps1`
- 兼容契约：`docs/refactor-contract.md`、`docs/config-identity-freeze.md`

---

## 2. 规则来源清单与链路来源清单

### 2.1 规则来源清单（可执行约束）

| 类别 | 文件 |
|---|---|
| 总体约束 | `README.md` |
| 重构契约 | `docs/refactor-contract.md` |
| 配置标识冻结 | `docs/config-identity-freeze.md` |
| net/org 改动登记 | `docs/net-org-patch-manifest.md` |
| 模块实现模板 | `docs/review/module-implementation-template.md` |
| 全量检查入口 | `scripts/check-all.ps1` |
| 编码检查 | `scripts/check-encoding.ps1` |
| 渲染守护 | `scripts/check-render-rules.ps1` |
| Checkstyle-lite | `scripts/check-checkstyle.ps1` |
| SpotBugs-lite | `scripts/check-spotbugs.ps1` |
| 重构守护 | `scripts/check-refactor-guards.ps1` |
| Smoke Tests | `scripts/check-smoke-tests.ps1` |
| Checkstyle 规则 | `config/checkstyle/checkstyle.xml` |
| SpotBugs 排除 | `config/spotbugs/exclude.xml` |
| OpenMyau 构建入口 | `d:/Project/OpenMyau-main/build.gradle.kts` |

### 2.2 链路来源清单（架构事实）

| 链路主题 | 文件 |
|---|---|
| 客户端启动与分层 | `docs/review/local-code-architecture-2026-02-21.md` |
| client 核心代码图 | `docs/review/code-map-client-core.md` |
| 客户端渲染流水线 | `docs/client-render-pipeline.md` |
| 模块注册结构 | `docs/client-module-registration.md` |
| 认证与皮肤依赖 | `docs/auth-dependencies.md` |
| 启动画面架构 | `docs/splash-architecture.md` |
| 模块迁移阶段说明 | `docs/module-migration-phase1.md` |
| KB 渲染 | `docs/knowledge-base/rendering.md` |
| KB 网络 | `docs/knowledge-base/networking.md` |
| KB 玩家 | `docs/knowledge-base/player.md` |
| KB GUI | `docs/knowledge-base/gui.md` |
| KB 世界 | `docs/knowledge-base/world.md` |
| KB 资源 | `docs/knowledge-base/assets.md` |
| KB 线程 | `docs/knowledge-base/threading.md` |
| KB 数据 | `docs/knowledge-base/data.md` |
| OpenMyau 初始化 | `d:/Project/OpenMyau-main/src/main/java/myau/Myau.java` |
| OpenMyau 游戏 Hook | `d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinMinecraft.java` |
| OpenMyau 网络 Hook | `d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinNetworkManager.java` |

---

## 3. 双项目总览与链路图（启动/事件/渲染/网络/配置/命令）

### 3.1 启动链路

Minecraft 仓库：
1. `src/net/minecraft/client/main/Main.java` 进入。
2. `src/net/minecraft/client/Minecraft.java:startGame` 启动运行时。
3. `ClientBootstrap.initialize(...)` 完成本地扩展层初始化（`src/client/core/ClientBootstrap.java`）。

OpenMyau：
1. `myau/mixin/MixinMinecraft.java` 注入 `Minecraft.startGame`。
2. `startGame@HEAD` 调 `new Initializer()`。
3. `startGame@RETURN` 调 `new Myau()`，在 `myau/Myau.java:init` 集中注册模块/命令/管理器。

### 3.2 事件链路

Minecraft 扩展层：
- 入口：`ClientBootstrap.onTick/onRender2D/onPacketSend/onPacketReceive`
- 总线：`src/client/event/EventBus.java`
- 模块分发：`src/client/module/ModuleManager.java`

OpenMyau：
- 入口：`MixinMinecraft` 在 `runTick/clickMouse/rightClickMouse` 注入事件。
- 总线：`myau/event/EventManager.java`
- 模块监听：`@EventTarget` + `ModuleManager` + 各模块 `onEnabled/onDisabled`。

### 3.3 渲染链路

Minecraft 扩展层：
1. `Minecraft` 持有 NanoVG 生命周期（`initializeNanoVG/beginNanoUiFrame/endNanoUiFrame/destroyNanoVG`）。
2. `ClientBootstrap.onRender2D` 固定分段：
`dispatchRenderEvent -> renderModules -> renderHud -> renderNanoScreen`。
3. 渲染守护由 `scripts/check-render-rules.ps1` 强制约束。

OpenMyau：
- 通过 mixin + 事件驱动注入 2D/3D 渲染。
- GUI 主体为 `myau/ui/ClickGui.java` + `myau/ui/components/*`。

### 3.4 网络链路

Minecraft 扩展层：
- `src/net/minecraft/network/NetworkManager.java` 是发送/接收主干。
- 发送桥接：`ClientBootstrap.onPacketSend(packet)`。
- 接收桥接：`ClientBootstrap.onPacketReceive(packet)`。

OpenMyau：
- `myau/mixin/MixinNetworkManager.java` 拦截 `channelRead0/sendPacket`。
- 发包/收包统一封装为 `myau/events/PacketEvent`，由 `EventManager.call` 分发。

### 3.5 配置链路

Minecraft 扩展层：
- `src/client/config/ConfigManager.java` 管理 profile 维度配置。
- 路径族：`config/client/profiles/<profile>/{client/modules/hud}`。
- 兼容稳定点：模块 ID、setting key、enum 常量名。

OpenMyau：
- `myau/config/Config.java` 使用 `./config/Myau/<name>.json`。
- 好友/目标名单文件：
`./config/Myau/friends.txt`、`./config/Myau/enemies.txt`（`myau/management/*Manager.java`）。

### 3.6 命令链路

Minecraft 扩展层：
- `src/client/command/ClientCommandManager.java`。
- 聊天入口短路位于 GUI 链路（见 `docs/review/code-map-client-core.md`）。

OpenMyau：
- `myau/command/CommandManager.java:onPacket` 拦截 `C01PacketChatMessage`。
- 命令前缀为 `.`，命中后取消原聊天包并本地执行。

---

## 4. Minecraft 全量包级写法索引（`net.minecraft.*`）

覆盖包（25）：
`block/client/command/crash/creativetab/dispenser/enchantment/entity/event/init/inventory/item/nbt/network/pathfinding/potion/profiler/realms/scoreboard/server/stats/tileentity/util/village/world`

### 4.1 `net.minecraft.block`
- 关键类：`src/net/minecraft/block/Block.java`
- 关键方法：`getIdFromBlock`、`getStateId`、`getBlockById`、`isFullBlock`、`getLightValue`
- 写法要点：方块行为改动优先保持状态/光照兼容，避免破坏 registry 与 metadata 语义。

### 4.2 `net.minecraft.client`
- 关键类：`src/net/minecraft/client/Minecraft.java`
- 关键方法：`startGame`、`run`、`createDisplay`、`displayCrashReport`、`registerMetadataSerializers`
- 写法要点：窗口循环、Nano 生命周期、主线程调度都集中在该类；扩展逻辑走桥接，不直接抢主循环所有权。

### 4.3 `net.minecraft.command`
- 关键类：`src/net/minecraft/command/CommandBase.java`
- 关键方法：`parseInt`、`parseDouble`、`parseBlockPos`、`getPlayer`、`addTabCompletionOptions`
- 写法要点：命令参数解析复用基类 helper，避免各命令重复解析与异常文案。

### 4.4 `net.minecraft.crash`
- 关键类：`src/net/minecraft/crash/CrashReport.java`
- 关键方法：`makeCategory`、`getCompleteReport`、`saveToFile`、`makeCrashReport`
- 写法要点：异常落盘信息要保留环境上下文，便于本地回归定位。

### 4.5 `net.minecraft.creativetab`
- 关键类：`src/net/minecraft/creativetab/CreativeTabs.java`
- 关键方法：`getTabIconItem`、`getTranslatedTabLabel`、`setBackgroundImageName`、`setNoScrollbar`
- 写法要点：创造栏显示属于 UI 语义，不在运行逻辑中塞副作用。

### 4.6 `net.minecraft.dispenser`
- 关键类：`src/net/minecraft/dispenser/BehaviorDefaultDispenseItem.java`
- 关键方法：`dispense`、`dispenseStack`、`doDispense`、`playDispenseSound`、`spawnDispenseParticles`
- 写法要点：发射行为分步组织，音效/粒子单独处理，便于覆盖具体发射物。

### 4.7 `net.minecraft.enchantment`
- 关键类：`src/net/minecraft/enchantment/EnchantmentHelper.java`
- 关键方法：`getEnchantmentLevel`、`getEnchantments`、`setEnchantments`、`getModifierForCreature`、`getEfficiencyModifier`
- 写法要点：附魔计算统一走 helper，避免物品/模块侧硬编码重复公式。

### 4.8 `net.minecraft.entity`
- 关键类：`src/net/minecraft/entity/Entity.java`
- 关键方法：`onUpdate`、`setPosition`、`setRotation`、`setDead`、`getDataWatcher`
- 写法要点：实体位姿与生命周期敏感；改动要明确 tick 时机与同步路径。

### 4.9 `net.minecraft.event`
- 关键类：`src/net/minecraft/event/ClickEvent.java`
- 关键方法：`getAction`、`getValue`、`toString`
- 写法要点：聊天事件对象以不可变读取为主，避免在渲染阶段做行为型副作用。

### 4.10 `net.minecraft.init`
- 关键类：`src/net/minecraft/init/Bootstrap.java`
- 关键方法：`register`、`isRegistered`、`redirectOutputToLog`、`printToSYSOUT`
- 写法要点：初始化顺序固定，注册动作不宜被模块层动态改写。

### 4.11 `net.minecraft.inventory`
- 关键类：`src/net/minecraft/inventory/Container.java`
- 关键方法：`detectAndSendChanges`、`slotClick`、`transferStackInSlot`、`onContainerClosed`、`putStackInSlot`
- 写法要点：容器改动要同时考虑客户端 UI 与服务端同步包行为。

### 4.12 `net.minecraft.item`
- 关键类：`src/net/minecraft/item/ItemStack.java`
- 关键方法：`splitStack`、`onItemUse`、`useItemRightClick`、`writeToNBT`、`readFromNBT`
- 写法要点：物品状态变化优先经 `ItemStack`，保持 NBT 序列化一致。

### 4.13 `net.minecraft.nbt`
- 关键类：`src/net/minecraft/nbt/NBTTagCompound.java`
- 关键方法：`setTag`、`setString`、`getTag`、`getTagId`、`getKeySet`
- 写法要点：键名与类型要稳定，兼容逻辑尽量在读取侧兜底。

### 4.14 `net.minecraft.network`
- 关键类：`src/net/minecraft/network/NetworkManager.java`
- 关键方法：`sendPacket`、`channelRead0`、`flushOutboundQueue`、`processReceivedPackets`、`closeChannel`
- 写法要点：网络线程与主线程边界必须清晰；对 `Minecraft/world` 的变更要回主线程。

### 4.15 `net.minecraft.pathfinding`
- 关键类：`src/net/minecraft/pathfinding/PathNavigate.java`
- 关键方法：`getPathToXYZ`、`tryMoveToEntityLiving`、`setPath`、`onUpdateNavigation`、`checkForStuck`
- 写法要点：路径导航逻辑是实体 AI 内核，不在渲染路径做复杂写入。

### 4.16 `net.minecraft.potion`
- 关键类：`src/net/minecraft/potion/Potion.java`
- 关键方法：`performEffect`、`affectEntity`、`isReady`、`isInstant`、`setPotionName`
- 写法要点：药水效果执行与显示分离，数值和显示键都要兼容。

### 4.17 `net.minecraft.profiler`
- 关键类：`src/net/minecraft/profiler/Profiler.java`
- 关键方法：`startSection`、`endSection`、`endStartSection`、`getProfilingData`、`clearProfiling`
- 写法要点：性能诊断应成对开闭 section，避免采样污染。

### 4.18 `net.minecraft.realms`
- 关键类：`src/net/minecraft/realms/RealmsBridge.java`
- 关键方法：`switchToRealms`、`getNotificationScreen`、`init`
- 写法要点：Realms 入口属于 GUI 过渡层，不应承担核心游戏逻辑。

### 4.19 `net.minecraft.scoreboard`
- 关键类：`src/net/minecraft/scoreboard/Scoreboard.java`
- 关键方法：`addScoreObjective`、`getValueFromObjective`、`setObjectiveInDisplaySlot`、`getTeam`、`removeObjective`
- 写法要点：计分板变更伴随网络同步，保持 objective/team 标识稳定。

### 4.20 `net.minecraft.server`
- 关键类：`src/net/minecraft/server/MinecraftServer.java`
- 关键方法：`startServer`、`createNewCommandManager`、`loadAllWorlds`、`initialWorldChunkLoad`、`canStructuresSpawn`
- 写法要点：服务端时序与世界加载顺序固定，客户端项目通常只做兼容性读取。

### 4.21 `net.minecraft.stats`
- 关键类：`src/net/minecraft/stats/StatList.java`
- 关键方法：`init`、`initCraftableStats`、`initMiningStats`、`initStats`、`getOneShotStat`
- 写法要点：统计项初始化顺序依赖物品/方块注册，避免后置注入造成空洞。

### 4.22 `net.minecraft.tileentity`
- 关键类：`src/net/minecraft/tileentity/TileEntity.java`
- 关键方法：`readFromNBT`、`writeToNBT`、`markDirty`、`getDescriptionPacket`、`invalidate`
- 写法要点：TileEntity 以 NBT 与描述包为同步核心，写法优先可序列化。

### 4.23 `net.minecraft.util`
- 关键类：`src/net/minecraft/util/ChatStyle.java`
- 关键方法：`getColor`、`setColor`、`setBold`、`setItalic`、`getChatClickEvent`
- 写法要点：文本样式对象常链式组合，变更应避免破坏兄弟/父样式继承关系。

### 4.24 `net.minecraft.village`
- 关键类：`src/net/minecraft/village/Village.java`
- 关键方法：`setWorld`、`tick`、`getCenter`、`getVillageRadius`、`getNearestDoor`
- 写法要点：村庄状态在 tick 内演进，局部缓存与世界引用需同步失效。

### 4.25 `net.minecraft.world`
- 关键类：`src/net/minecraft/world/World.java`
- 关键方法：`initialize`、`getBiomeGenForCoords`、`setInitialSpawnLocation`、`isAirBlock`、`getChunkFromBlockCoords`
- 写法要点：世界访问是多数系统共享入口，线程安全和区块加载前置判断必须保留。

---

## 5. 本地扩展层写法（`client.*` / `dwgx.*`，含 guard/check）

### 5.1 Guard/Check 基线

- 兼容约束：
`docs/refactor-contract.md`、`docs/config-identity-freeze.md`、`docs/net-org-patch-manifest.md`
- 渲染边界：
`scripts/check-render-rules.ps1`（禁止 client 层直接操作 display loop/viewport/clear）
- 命名与结构：
`config/checkstyle/checkstyle.xml` + `scripts/check-checkstyle.ps1`
- 危险调用扫描：
`scripts/check-spotbugs.ps1`
- 桥接守护：
`scripts/check-refactor-guards.ps1`
- 轻量自动化测试：
`scripts/check-smoke-tests.ps1`

### 5.2 `client.auth`
- 关键类：
`src/client/auth/AccountRepository.java`、`src/client/auth/MicrosoftAuthService.java`、`src/client/auth/MicrosoftSessionManager.java`
- 关键方法：
`load/save/upsertMicrosoft`、`loginWithAuthorizationInput/startDeviceLogin/fetchMinecraftProfile`、`applyMicrosoftSession/applyOfflineSession/setSession`
- 写法要点：账户持久化与会话切换分离；失败路径通过安全包装函数兜底。

### 5.3 `client.bridge`
- 关键接口：
`src/client/bridge/GameBridge.java`、`src/client/bridge/PlayerBridge.java`、`src/client/bridge/WorldBridge.java`
- 关键方法：
`currentScreen/isScreenOpen/isWorldLoaded`、`localPlayer/hasPlayer/yaw/pitch`、`localWorld/hasWorld`
- 写法要点：桥接层提供“只读快照 + 能力判断”，避免模块直连底层对象细节。

### 5.4 `client.command`
- 关键类：`src/client/command/ClientCommandManager.java`
- 关键方法：`execute`、`complete`、`applyBind`、`resolveModule`、`printUsage`
- 写法要点：命令执行与补全共用 token 解析，减少参数分支重复。

### 5.5 `client.config`
- 关键类：`src/client/config/ConfigManager.java`
- 关键方法：`loadAll`、`saveAll`、`loadModules`、`saveModules`、`onModuleChanged`
- 写法要点：拆分 client/modules/hud 三份配置文件，模块变更触发脏标记而非即时散写。

### 5.6 `client.core`
- 关键类：`src/client/core/ClientBootstrap.java`
- 关键方法：`initialize`、`onTick`、`onRender2D`、`onPacketSend`、`onPacketReceive`
- 写法要点：核心编排层只做“扇出调度 + 异常隔离”，业务逻辑留在模块和服务层。

### 5.7 `client.event`
- 关键类：`src/client/event/EventBus.java`
- 关键方法：`register`、`unregister`、`post`、`dispatch`、`dispatchSafely`
- 写法要点：监听分发失败应隔离，不得让单一监听器击穿全链路。

### 5.8 `client.feature`
- 关键文件：`src/client/feature/module/package-info.java`、`src/client/feature/hud/package-info.java`
- 说明：当前主要承担包语义与编排分域标识，核心实现位于 `client.module` 与 `client.hud`。
- 写法要点：该层不承载运行时状态。

### 5.9 `client.hud`
- 关键类：`src/client/hud/HudManager.java`、`src/client/hud/HudRegistry.java`
- 关键方法：`register/getElements/render`、`renderLayer/applyTransform`、`registerBuiltins`
- 写法要点：HUD 走注册表集中管理，布局数学独立在 `HudLayoutMath`。

### 5.10 `client.i18n`
- 关键类：`src/client/i18n/I18nManager.java`
- 关键方法：`reload`、`setCurrentLocale`、`translate`、`translateOrDefault`、`normalizeLocale`
- 写法要点：翻译缺失可回退，不把 i18n 失败传播为功能异常。

### 5.11 `client.module`
- 关键类：`src/client/module/Module.java`、`src/client/module/ModuleManager.java`、`src/client/module/ModuleRegistry.java`
- 关键方法：`setEnabled/onEnable/onDisable/onTick/onRender2D`、`onPacketSend/onPacketReceive/onKey`、`registerBuiltins`
- 写法要点：注册列表集中，生命周期对称，ID/setting key 稳定。

### 5.12 `client.render`
- 关键类：`src/client/render/NanoVGContext.java`、`src/client/render/NanoRenderer.java`
- 关键方法：`beginFrame/endFrame/save/restore/resetLegacyPipelineState`、`render`
- 写法要点：客户端扩展只消费 context，不持有 Nano context 生命周期。

### 5.13 `client.runtime`
- 关键类：`src/client/runtime/lwjgl/LwjglRuntimePolicy.java`
- 关键方法：`requiredVersion`、`allowLegacyInputPath`
- 写法要点：运行时兼容策略集中声明，不在业务类散落硬编码分支。

### 5.14 `client.setting`
- 关键类：`src/client/setting/Setting.java`
- 关键方法：`get/set`、`visibleWhen`、`normalize`、`reset`、`markConfigDirty`
- 写法要点：设置项自己承担归一化和脏标记，调用方只关心语义值。

### 5.15 `client.ui`
- 关键类：`src/client/ui/ClickGuiScreen.java`
- 关键方法：`initGui`、`drawScreen`、`handleMouseInput`、`keyTyped`、`renderNano`
- 写法要点：输入处理、绘制、Nano 渲染分层，不把复杂业务塞入 GUI 事件回调。

### 5.16 `dwgx.foundation`
- 关键类：`src/dwgx/foundation/rotation/RotationDomain.java`
- 关键方法：`fromEyeTo`、`stepSmooth`、`sendSilentLook`、`capture`、`restore`
- 写法要点：旋转域封装“计算 + 应用 + 还原”，降低模块重复实现。

### 5.17 `dwgx.modulekit`
- 关键类：`src/dwgx/modulekit/ModuleKit.java`
- 关键方法：`randomDelayMs`、`cpsDelayMs`、`findStealableSlot`、`findBestToolHotbarSlot`、`raycastHitsEntity`
- 写法要点：模块通用启发式收敛到 kit 层，模块类只组织策略。

### 5.18 `dwgx.nano`
- 关键类：`src/dwgx/nano/NanoUi.java`
- 关键方法：`drawWindow`、`drawPanel`、`drawRow`、`beginClip`、`endClip`
- 写法要点：Nano 组件化绘制函数复用，避免各 UI 自行实现基础控件。

### 5.19 `dwgx.ui`
- 关键类：`src/dwgx/ui/ext/UiExtensionManager.java`、`src/dwgx/ui/ext/MainMenuBackgroundRuntime.java`
- 关键方法：`getMainMenuBackgroundMode/setMainMenuBackgroundMode/createMainMenuScreen`、`setScene/renderSceneBackground/tickScene/close`
- 写法要点：主菜单扩展由 manager + runtime 双层控制，资源关闭必须在 `close` 收口。

---

## 6. OpenMyau 包级写法索引（`myau.*` / `me.ksyz.*`）

### 6.1 `myau` 根包
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/Myau.java`
- 关键方法：`init`
- 写法要点：集中式注册（manager/module/command/property/event）都在一个入口初始化。

### 6.2 `myau.command`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/command/CommandManager.java`
- 关键方法：`handleCommand`、`isTypingCommand`、`onPacket`
- 写法要点：通过聊天包拦截实现命令系统，命中命令后直接 cancel 原包。

### 6.3 `myau.config`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/config/Config.java`
- 关键方法：`load`、`save`
- 写法要点：模块名作为 JSON 顶层 key；属性由 `Property.read/write` 反序列化。

### 6.4 `myau.data`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/data/Box.java`
- 关键方法：构造器 `Box(T value)`（数据载体类）
- 写法要点：该包当前以轻量容器为主，行为逻辑较少。

### 6.5 `myau.enums`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/enums/ChatColors.java`
- 关键方法：`toString`、`toAwtColor`、`formatColor`
- 写法要点：聊天色值与 AWT 色彩映射放在枚举内，调用方只消费结果。

### 6.6 `myau.event`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/event/EventManager.java`
- 关键方法：`register`、`unregister`、`call`、`cleanMap`、`invoke`
- 写法要点：反射注册 + 优先级排序 + 可取消事件模型。

### 6.7 `myau.events`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/events/PacketEvent.java`
- 关键方法：`getType`、`getPacket`
- 写法要点：事件类型枚举区分 SEND/RECEIVE，供模块做包过滤。

### 6.8 `myau.init`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/init/FMLLoadingPlugin.java`
- 关键方法：`onLoad`、`getMixins`、`tryAddMixinClass`、`shouldApplyMixin`、`acceptTargets`
- 写法要点：通过 Mixin config plugin 动态收集 mixin 类。

### 6.9 `myau.management`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/management/RotationManager.java`
- 关键方法：`setRotation`、`resetRotationState`、`onTick`、`onRender3D`
- 写法要点：旋转状态集中管理，模块通过 manager 协作，不直接互改状态。

### 6.10 `myau.mixin`
- 关键类：
`d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinMinecraft.java`、
`d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinNetworkManager.java`
- 关键方法：
`startGame/runTick/loadWorld`（注入点）、
`channelRead0/sendPacket/sendPacket2`（包钩子）
- 写法要点：行为入口几乎都靠 mixin 注入，需严格控制取消条件与副作用。

### 6.11 `myau.module`
- 关键类：
`d:/Project/OpenMyau-main/src/main/java/myau/module/Module.java`、
`d:/Project/OpenMyau-main/src/main/java/myau/module/ModuleManager.java`
- 关键方法：
`setEnabled/toggle/onEnabled/onDisabled`、`onKey/onTick/getModule`
- 写法要点：模块开关状态与按键绑定在基类统一处理。

### 6.12 `myau.property`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/property/Property.java`
- 关键方法：`getValue`、`setValue`、`parseString`、`read`、`write`
- 写法要点：属性对象负责序列化与可见性逻辑，模块只持有字段声明。

### 6.13 `myau.ui`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/ui/ClickGui.java`
- 关键方法：`initGui`、`drawScreen`、`mouseClicked`、`keyTyped`、`savePositions`
- 写法要点：GUI 位置状态本地持久化，界面生命周期与组件绘制分开。

### 6.14 `myau.util`
- 关键类：`d:/Project/OpenMyau-main/src/main/java/myau/util/ItemUtil.java`
- 关键方法：`getAttackBonus`、`getToolEfficiency`、`findSwordInInventorySlot`、`findArmorInventorySlot`、`isHoldingSword`
- 写法要点：常见战斗/背包启发式集中在 util，模块复用而非复制。

### 6.15 `me.ksyz.accountmanager`
- 关键类：
`d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/AccountManager.java`、
`d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/auth/MicrosoftAuth.java`、
`d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/gui/GuiAccountManager.java`
- 关键方法：
`init/load/save`、`getMSAuthLink/login/acquireMCAccessToken`、`initGui/drawScreen/actionPerformed`
- 写法要点：账号子系统与 `myau` 主模块系统并行，界面与认证链路独立维护。

---

## 7. 差异矩阵（现状优先）

| 主题 | Minecraft 本地扩展层现状 | OpenMyau 现状 | 风险 | 可选改进 |
|---|---|---|---|---|
| 启动组织 | `ClientBootstrap` 分层编排 | `Myau.init` 集中注册 | 单入口文件持续膨胀 | 逐步抽 registry 类，保持行为不变 |
| 事件总线 | `EventBus` + `@EventLink` 缓存调用点 | 反射 `@EventTarget` + 优先级数组 | 反射热点性能波动 | 缓存 MethodData 索引，先做无行为变更优化 |
| 渲染边界 | Nano 生命周期仅 `Minecraft` 持有，脚本守护 | Mixin + GUI 自绘并存 | 渲染状态污染更难定位 | 增加最小渲染守护脚本（只报警） |
| 网络钩子 | `NetworkManager` -> `ClientBootstrap` -> `ModuleManager` | `MixinNetworkManager` 直接分发/取消 | 取消条件分散 | 收敛包过滤策略到单点工具类 |
| 配置结构 | profile 分层目录（client/modules/hud） | 单 JSON（模块名顶层）+ 文本名单 | 配置兼容迁移难 | 增加 schema/version 字段，兼容旧读取 |
| 模块注册 | `ModuleRegistry.registerBuiltins` | `Myau.init` 手工 `modules.put` | 维护成本高 | 保持现状下先提取注册列表常量 |
| 守护体系 | `check-all` + 多脚本 | 主要依赖 `./gradlew build` | 隐性回归不易发现 | 增加轻量 lint/check 脚本，不改运行逻辑 |

说明：
- 本矩阵仅记录现状行为、风险和可选路径，不要求立即重构。

---

## 8. 可复制模板（Minecraft 风格 + OpenMyau 风格）

### 8.1 模板 A：模块生命周期

#### A1. Minecraft 风格（`client.module`）
```java
package client.module.impl.misc;

import client.module.Category;
import client.module.Module;

public final class SampleModule extends Module {
    public SampleModule() {
        super("sample_module", "Sample Module", Category.MISC);
    }

    @Override
    public void onEnable() {
        // init runtime state only
    }

    @Override
    public void onDisable() {
        // restore forced state here
    }

    @Override
    public void onTick() {
        // guard first, then behavior
    }
}
```
- 何时使用：新增 `client.module.impl.*` 模块时。
- 禁用条件：不通过 `ModuleRegistry.registerBuiltins` 直接注入匿名实例。
- 常见坑：`onDisable` 忘记恢复按键/输入状态导致粘键。

#### A2. OpenMyau 风格（`myau.module`）
```java
package myau.module.modules;

import myau.module.Module;

public class SampleModule extends Module {
    public SampleModule() {
        super("Sample", false);
    }

    @Override
    public void onEnabled() {
        // prepare state
    }

    @Override
    public void onDisabled() {
        // cleanup state
    }
}
```
- 何时使用：新增 `myau/module/modules/*` 模块时。
- 禁用条件：不要绕开 `Module.setEnabled/toggle` 直接写 `enabled` 字段。
- 常见坑：模块属性字段未加入 `Property`，配置不会持久化。

### 8.2 模板 B：事件监听

#### B1. Minecraft 风格（`client.event`）
```java
package client.module.impl.misc;

import client.event.annotations.EventLink;
import client.event.Listener;
import client.event.MotionUpdateEvent;
import client.module.Module;

public final class SampleEventModule extends Module {
    @EventLink
    private final Listener<MotionUpdateEvent> onMotion = event -> {
        if (!isEnabled()) {
            return;
        }
        // handle event
    };
}
```
- 何时使用：在模块内部监听 client 事件总线。
- 禁用条件：不要在监听器里抛出未处理异常。
- 常见坑：忽略 `isEnabled()` 判断导致关闭模块后仍执行业务。

#### B2. OpenMyau 风格（`myau.event`）
```java
package myau.module.modules;

import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.module.Module;

public class SampleEventModule extends Module {
    public SampleEventModule() {
        super("SampleEvent", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        // handle tick
    }
}
```
- 何时使用：OpenMyau 中所有事件回调入口。
- 禁用条件：不要在事件回调中直接阻塞（sleep/长循环）。
- 常见坑：方法签名不合法（非单参数）会导致注册后不生效。

### 8.3 模板 C：配置读写

#### C1. Minecraft 风格（`client.config`）
```java
package client.core;

import client.config.ConfigManager;

public final class SampleConfigUse {
    private final ConfigManager config;

    public SampleConfigUse(ConfigManager config) {
        this.config = config;
    }

    public void loadThenRun() {
        this.config.loadAll();
        // use loaded state
    }

    public void markAndSave() {
        this.config.markModulesDirty();
        this.config.saveModules();
    }
}
```
- 何时使用：分层配置（client/modules/hud）读写逻辑。
- 禁用条件：不要在高频路径每帧写盘。
- 常见坑：新增 setting 后忘记更新冻结快照与 i18n key。

#### C2. OpenMyau 风格（`myau.config`）
```java
package myau.config;

public final class SampleConfigUse {
    public void loadDefault() {
        Config config = new Config("default", true);
        config.load();
    }

    public void saveDefault() {
        Config config = new Config("default", false);
        config.save();
    }
}
```
- 何时使用：配置命令、启动恢复、退出保存。
- 禁用条件：不要在未初始化 `Myau.propertyManager` 前调用加载。
- 常见坑：模块名改动会导致旧 JSON 顶层 key 失配。

### 8.4 模板 D：命令拦截

#### D1. Minecraft 风格（`ClientCommandManager`）
```java
package client.command;

public final class SampleCommandHook {
    private final ClientCommandManager manager;

    public SampleCommandHook(ClientCommandManager manager) {
        this.manager = manager;
    }

    public boolean handleChatInput(String raw) {
        return this.manager.execute(raw);
    }
}
```
- 何时使用：聊天输入本地命令短路。
- 禁用条件：不要把普通聊天误判为命令。
- 常见坑：补全和执行使用两套 parser，导致行为不一致。

#### D2. OpenMyau 风格（`CommandManager#onPacket`）
```java
package myau.command;

import myau.event.EventTarget;
import myau.events.PacketEvent;
import net.minecraft.network.play.client.C01PacketChatMessage;

public final class SampleCommandInterceptor {
    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof C01PacketChatMessage)) {
            return;
        }
        String msg = ((C01PacketChatMessage) event.getPacket()).getMessage();
        if (msg.startsWith(".")) {
            event.setCancelled(true);
            // dispatch local command
        }
    }
}
```
- 何时使用：OpenMyau 聊天命令拦截。
- 禁用条件：不要拦截非聊天包。
- 常见坑：取消后未反馈错误文案，用户无感知。

### 8.5 模板 E：工具类空值守卫

#### E1. Minecraft 风格
```java
package client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public final class GuardUtil {
    private GuardUtil() {
    }

    public static boolean hasReadyPlayer(Minecraft mc) {
        if (mc == null) {
            return false;
        }
        EntityPlayerSP p = mc.thePlayer;
        WorldClient w = mc.theWorld;
        return p != null && w != null;
    }
}
```
- 何时使用：tick/render/network 入口前置守卫。
- 禁用条件：不要在空值分支继续访问玩家或世界对象。
- 常见坑：只判 `thePlayer` 不判 `theWorld` 导致换图时 NPE。

#### E2. OpenMyau 风格（与 `ItemUtil` 一致）
```java
package myau.util;

import net.minecraft.item.ItemStack;

public final class GuardUtil {
    private GuardUtil() {
    }

    public static boolean hasStack(ItemStack stack) {
        return stack != null && stack.stackSize > 0;
    }
}
```
- 何时使用：背包扫描、武器判定、工具选择等 util 逻辑。
- 禁用条件：不要在 `null` 分支返回可用态。
- 常见坑：忽略 `stackSize` 导致空堆栈被当作有效物品。

---

## 9. 提交前检查清单（命令化）

### 9.1 Minecraft 仓库

全量检查：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-all.ps1
```

分项检查：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-encoding.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-render-rules.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-checkstyle.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-spotbugs.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-refactor-guards.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-smoke-tests.ps1
```

### 9.2 OpenMyau 仓库

构建验证：
```powershell
cd d:\Project\OpenMyau-main
.\gradlew build
```

建议最小人工回归：
- 启动后模块列表是否可切换。
- 命令前缀 `.` 是否被本地拦截。
- 配置加载/保存是否对齐预期文件。
- 关键 GUI（ClickGui、账号管理）是否可正常开关。

---

## 10. 验收记录（覆盖/路径/模板）

- 覆盖性（包级）：
`net.minecraft` 顶层包章节 25/25。
- 覆盖性（扩展层）：
`client.*` + `dwgx.*` + `myau.*` + `me.ksyz.*` 章节 33/33。
- 路径有效性：
文档内路径样本校验 98/98 存在（过滤占位符与示意路径后）。
- 模板可用性：
5 类模板均提供 Minecraft 风格与 OpenMyau 风格，共 10 段代码模板。
- 现状优先：
差异矩阵仅提供“现状行为 + 风险 + 可选改进”，未使用强制改造措辞。
- 检查命令：
已列 `scripts/check-all.ps1` 与子检查脚本，及 OpenMyau 的 `./gradlew build`。

---

## 11. 来源文件索引

### 11.1 Minecraft 文档与规则
- `README.md`
- `docs/refactor-contract.md`
- `docs/config-identity-freeze.md`
- `docs/net-org-patch-manifest.md`
- `docs/client-module-registration.md`
- `docs/client-render-pipeline.md`
- `docs/module-migration-phase1.md`
- `docs/auth-dependencies.md`
- `docs/splash-architecture.md`
- `docs/knowledge-base/README.md`
- `docs/knowledge-base/rendering.md`
- `docs/knowledge-base/networking.md`
- `docs/knowledge-base/player.md`
- `docs/knowledge-base/gui.md`
- `docs/knowledge-base/world.md`
- `docs/knowledge-base/assets.md`
- `docs/knowledge-base/threading.md`
- `docs/knowledge-base/data.md`
- `docs/review/local-code-architecture-2026-02-21.md`
- `docs/review/code-map-client-core.md`
- `docs/review/module-implementation-template.md`
- `config/checkstyle/checkstyle.xml`
- `config/spotbugs/exclude.xml`
- `scripts/check-all.ps1`
- `scripts/check-encoding.ps1`
- `scripts/check-render-rules.ps1`
- `scripts/check-checkstyle.ps1`
- `scripts/check-spotbugs.ps1`
- `scripts/check-refactor-guards.ps1`
- `scripts/check-smoke-tests.ps1`

### 11.2 Minecraft 代码锚点
- `src/net/minecraft/client/Minecraft.java`
- `src/net/minecraft/network/NetworkManager.java`
- `src/client/core/ClientBootstrap.java`
- `src/client/module/ModuleManager.java`
- `src/client/module/ModuleRegistry.java`
- `src/client/config/ConfigManager.java`
- `src/client/command/ClientCommandManager.java`
- `src/client/hud/HudManager.java`
- `src/client/event/EventBus.java`
- `src/client/ui/ClickGuiScreen.java`
- `src/dwgx/ui/ext/UiExtensionManager.java`
- `src/dwgx/ui/ext/MainMenuBackgroundRuntime.java`
- `src/dwgx/foundation/rotation/RotationDomain.java`
- `src/dwgx/modulekit/ModuleKit.java`

### 11.3 OpenMyau 代码锚点
- `d:/Project/OpenMyau-main/README.md`
- `d:/Project/OpenMyau-main/build.gradle.kts`
- `d:/Project/OpenMyau-main/src/main/java/myau/Myau.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/module/Module.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/module/ModuleManager.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/config/Config.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/event/EventManager.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/command/CommandManager.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/management/RotationManager.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinMinecraft.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/mixin/MixinNetworkManager.java`
- `d:/Project/OpenMyau-main/src/main/java/myau/util/ItemUtil.java`
- `d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/AccountManager.java`
- `d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/auth/MicrosoftAuth.java`
- `d:/Project/OpenMyau-main/src/main/java/me/ksyz/accountmanager/gui/GuiAccountManager.java`
