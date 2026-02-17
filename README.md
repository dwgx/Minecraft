# Minecraft 1.8.9 Client (MCP 9.18)

Vanilla Minecraft 1.8.9 client sources decompiled with MCP 9.18 (stable_22).
Only manual tweak: a small loop-init fix in `StructureMineshaftPieces.Room` so it recompiles to the original bytecode behaviour.

## Project layout

- `src/` – full decompiled and deobfuscated client sources.
- `lib/` – only the runtime/compile-time JARs shipped with 1.8.9 (authlib 1.5.21, LWJGL 3.3.3 with compatibility shims, Netty 4.0.23, etc.).
- `natives/` – Windows native DLLs from the vanilla distribution.

## Prerequisites

- JDK 8 (tested with `D:\Software\DEV\Java\jdk1.8.0_471`).
- Game assets from the official launcher: `%APPDATA%\.minecraft\assets` (run the vanilla launcher once to download if missing).

## Importing in IntelliJ IDEA

1. Open the folder `D:\Project\Minecraft` as a project.
2. Set Project SDK to JDK 8.
3. Ensure the module uses `src` and the library folder `lib`; no `.idea` or `.iml` files are tracked in git.
4. Build → Rebuild Project.

## Running from IDEA

Create an Application run configuration:

- Main class: `net.minecraft.client.main.Main`
- VM options: `-Djava.library.path=D:/Project/Minecraft/natives`
- Program arguments:
  ```
  --username Player --version 1.8.9 --gameDir . --assetsDir %APPDATA%\.minecraft\assets --assetIndex 1.8 --accessToken 0
  ```
- Working directory: `D:\Project\Minecraft`
- JRE: JDK 8

> 常见错误：同一个参数名不要写两次（例如既写 `--assets` 又写 `--assetsDir` 会触发 “MultipleArgumentsForOption”）。上面一行已经是正确且最小的参数集合。

## Building from command line (PowerShell)

```powershell
$env:JAVA_HOME = 'D:\Software\DEV\Java\jdk1.8.0_471'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Remove-Item -Recurse -Force .\out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path .\out\classes | Out-Null
javac -source 1.6 -target 1.6 -encoding UTF-8 `
      -classpath "lib/*" `
      -d out\classes `
      (Get-ChildItem -Recurse src -Filter *.java | % FullName)
```

Then run:

```powershell
java -Djava.library.path=.\natives `
     -cp "out\classes;lib/*" `
     net.minecraft.client.main.Main `
     --username Player --version 1.8.9 --gameDir . `
     --assetsDir "$env:APPDATA\.minecraft\assets" --assetIndex 1.8 --accessToken 0
```

### Why `-source 1.6 -target 1.6`?
Mojang built 1.8.9 for the Java 6 bytecode level. Compiling to 1.6 with JDK 8 reproduces the same class format and avoids class file version mismatches at runtime.

## Contributing / hygiene
- Do **not** commit IDE metadata (`.idea/`, `*.iml`), caches, logs, downloaded assets, or experimental third-party libraries.
- Keep `lib/` limited to the vanilla 1.8.9 jars already present.
- Match upstream behaviour; avoid gameplay changes in this repository.

---
Maintainer: (fill in your name)
