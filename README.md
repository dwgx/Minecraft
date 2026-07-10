# Minecraft 1.8.9 Client Mod (MCP 9.18)

**基于 MCP 9.18 反编译的 Minecraft 1.8.9 客户端，带自定义 NanoVG UI 和模块系统的外挂客户端。**

A decompiled Minecraft 1.8.9 client (MCP 9.18 / `stable_22`) with a custom NanoVG-rendered UI, module framework, and integrated IRC chat — a full-featured cheat client.

---

## Overview / 概述

This project takes the vanilla Minecraft 1.8.9 sources (decompiled via MCP 9.18 `stable_22`) and layers a complete cheat client on top. The vanilla tree under `src/net/minecraft` stays close to upstream. The custom layer (`src/client`, `src/dwgx`) adds combat/movement/world modules, a vector-rendered UI, Microsoft authentication, HUD editor, and an IRC chat system with its own Netty server backend.

这个项目从 MCP 9.18 反编译的原版 1.8.9 客户端出发，在上面搭了一整套外挂客户端。原版代码基本保持不动，自定义层加了战斗/移动/世界模块、NanoVG 矢量渲染 UI、微软账号认证、HUD 编辑器，还有一套自带 Netty 后端的 IRC 聊天系统。

Java 8，纯 javac 编译，没有 Gradle 也没有 Maven。Windows 为主的构建环境。

---

## Features / 功能

### Client Modules / 客户端模块

- **Combat / 战斗**: KillAura, AutoClicker, Velocity
- **Movement / 移动**: Fly, Speed, Eagle, KeepSprint, Sprint, NoFall, NoPush, NoSlow
- **World / 世界**: AntiVoid, AutoTool, FastPlace, Stealer, InventoryMove
- **Misc / 杂项**: SkinChanger
- **Client / 客户端**: ClickGUI, HUD editor, settings screens

### UI System / 界面系统

- NanoVG vector-rendered ClickGUI (migrated from LWJGL 2 to LWJGL 3)
- Visual HUD editor with anchors, docking, snapping, per-element transforms
- Reusable components: sliders, dropdowns, toggles, color pickers, text inputs
- UI scale editor

### Infrastructure / 基础设施

- Synchronous event bus with prioritized, snapshot-based dispatch
- JSON config with partitioned storage, dirty tracking, atomic writes, backups, migration, profiles, autosave
- Setting framework: bool, int, float, enum, string, color, keybind, multi-select, groups
- Microsoft OAuth + Xbox Live authentication, account manager GUI, session management, skin/cape override
- Read-only Minecraft runtime bridge (`GameBridge`, `PlayerBridge`, `WorldBridge`)
- Internationalization: `en_us`, `zh_cn`, `ja_jp`

### IRC Chat / IRC 聊天

- In-game IRC client: channels, friends, mail, social feed/comments, emoji, image/voice previews, profiles
- Local SQLite cache
- Standalone Netty + MariaDB chat server backend (`IRCServer/`)

### Main Menu Extras / 主菜单彩蛋

- Animated background scenes: Flappy Bird, Kirby Jump, Nyan Cat, Atari Sokoban
- GLSL shader backgrounds, MenuFX, Bing wallpaper fetcher

---

## Tech Stack / 技术栈

| Layer | Tech |
|-------|------|
| Language | Java 8 (source/target 1.8) |
| Build | Manual `javac`, Windows `.bat` launchers, PowerShell check scripts |
| Rendering | LWJGL 3.3.3 (GLFW, OpenGL, OpenAL, NanoVG) + LWJGL 2 compatibility shims |
| Auth | authlib 1.5.21, Microsoft OAuth/Xbox Live |
| Networking | Netty 4.0.23.Final |
| Persistence | SQLite (client IRC cache), MariaDB (server) |
| Serialization | Gson 2.2.4 |
| Other | Guava 17.0, Apache Commons, Log4j 2.0-beta9, JNA, ICU4J |

---

## Project Structure / 项目结构

```
Minecraft/
├── src/
│   ├── Start.java                 # Quick launcher (wraps Main with default args)
│   ├── client/                    # Cheat client core
│   │   ├── auth/                  # Microsoft OAuth + Xbox Live
│   │   ├── bridge/                # Read-only Minecraft runtime bridge
│   │   ├── chat/                  # IRC chat models, cache, services
│   │   ├── command/               # Local chat commands
│   │   ├── config/                # JSON config manager, migration, profiles
│   │   ├── core/                  # Bootstrap, ClientInfo, BuildInfo
│   │   ├── event/                 # Event bus + event types
│   │   ├── hud/                   # HUD elements + visual editor
│   │   ├── i18n/                  # Localization
│   │   ├── module/                # Module framework + combat/movement/world/misc/client
│   │   ├── render/                # 2D render context (NanoVG)
│   │   ├── runtime/lwjgl/         # LWJGL 3 windowing/input
│   │   ├── setting/               # Setting types
│   │   └── ui/                    # NanoVG screens, components, IRC UI
│   ├── dwgx/
│   │   ├── IRCBackend/            # Client-side IRC adapter
│   │   ├── foundation/            # Math, animation, input, raycast, render utils
│   │   ├── modulekit/             # CombatKit, InventoryProbe, SightProbe, PulseMath
│   │   ├── nano/                  # NanoVG render utils, fonts, themes
│   │   └── ui/ext/               # Menu backgrounds, shaders, MenuFX, Bing wallpaper
│   └── net/minecraft/             # Decompiled vanilla client (MCP 9.18)
├── IRCServer/                     # Standalone Netty + MariaDB chat server
│   ├── src/irc/server/
│   ├── config/                    # db.json, irc_users.json
│   ├── lib/
│   └── build.bat / run.bat
├── lib/                           # Vanilla 1.8.9 runtime JARs
├── natives/                       # Windows native DLLs (OpenAL, GLFW, LWJGL)
├── scripts/                       # PowerShell static-check scripts
├── docs/                          # Architecture & migration notes
├── run2/                          # Sample game directory (world, config, logs)
└── launch-dual.bat                # Compile + launch two instances
```

---

## Getting Started / 快速开始

### Prerequisites / 前置条件

- **JDK 8** (Zulu 8 or similar)
- Game assets from the official launcher: run vanilla once so `%APPDATA%\.minecraft\assets` exists
- For `IRCServer`: JDK 21 + a reachable MariaDB instance

### Build & Run (PowerShell) / 构建与运行

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Remove-Item -Recurse -Force .\out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path .\out\classes | Out-Null
javac -source 1.8 -target 1.8 -encoding UTF-8 `
      -classpath "lib/*" `
      -d out\classes `
      (Get-ChildItem -Recurse src -Filter *.java | % FullName)
```

Run:

```powershell
java -Djava.library.path=.\natives `
     -cp "out\classes;lib/*" `
     net.minecraft.client.main.Main `
     --username Player --version 1.8.9 --gameDir . `
     --assetsDir "$env:APPDATA\.minecraft\assets" --assetIndex 1.8 --accessToken 0
```

不要同时传 `--assets` 和 `--assetsDir`，否则会触发 `MultipleArgumentsForOption`。上面的参数是最小正确集。

### IntelliJ IDEA

1. Open project folder, set SDK to JDK 8
2. Mark `src` as source root, add `lib` as library
3. Run config: Main class `net.minecraft.client.main.Main`, VM option `-Djava.library.path=<repo>/natives`, working dir at repo root

### Launch Two Instances / 双开

`launch-dual.bat` compiles and starts two client instances (working dirs `run/` and `run2/`) with distinct IRC nicknames. Paths are hardcoded — adjust `BASE` and Java paths before use. Set `DRY_RUN=1` to compile only.

### IRC Server / IRC 服务器

```bat
cd IRCServer
build.bat
run.bat [port]
```

Configure database in `IRCServer/config/db.json`.
<!-- TODO: confirm default port and whether the server binds all interfaces. -->

---

## Configuration / 配置

Client config is JSON-based, managed by `client.config.ConfigManager`, stored per-profile under `config/client/profiles/<profile>/`:

- `client.json` — general client settings
- `modules.json` — module state and settings
- `hud.json` — HUD layout/transforms

### System Properties / 系统属性

- `client.autologin` — auto-login on startup
- `irc.nickname`, `irc.username` — IRC identity
- `java.library.path` — must point to `natives/`

### Server Config / 服务器配置

- `IRCServer/config/db.json` — database connection
- `IRCServer/config/irc_users.json` — user store seed

---

## Static Checks / 静态检查

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-all.ps1
```

Individual: `check-encoding.ps1`, `check-render-rules.ps1`, `check-checkstyle.ps1`, `check-spotbugs.ps1`, `check-refactor-guards.ps1`, `check-smoke-tests.ps1`.

---

## Status / 状态

Personal project, actively developed. Recent work: LWJGL 2 to 3 migration, module refactors, UI overhaul. Build tooling is Windows-centric with hardcoded local paths in `.bat` files.

个人项目，持续开发中。最近在做 LWJGL 2 到 3 的迁移、模块重构、UI 重写。构建脚本是 Windows 专用的，bat 里路径写死了。

---

## License / 许可证

No license file present — all rights reserved by default.

仓库没有 license 文件，默认保留所有权利。

<!-- TODO: fill in maintainer name/contact -->
