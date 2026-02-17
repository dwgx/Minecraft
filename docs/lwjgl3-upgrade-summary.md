# LWJGL 3.3.3 Upgrade Summary

This repository now runs on LWJGL 3.3.3 with a compatibility layer for legacy `org.lwjgl.*` call sites.

## What was updated

- Runtime libraries are LWJGL 3.3.3 (`lwjgl`, `lwjgl-glfw`, `lwjgl-opengl`, `lwjgl-openal`, `lwjgl-nanovg`) with matching Windows natives.
- Legacy LWJGL2-only jars were removed from `lib/`:
  - `jinput-2.0.5.jar`
  - `librarylwjglopenal-20100824.jar`
- Audio bootstrap now falls back safely when OpenAL legacy backend is absent:
  - `SoundManager` first tries `paulscode.sound.libraries.LibraryLWJGLOpenAL`
  - If unavailable, it falls back to `paulscode.sound.libraries.LibraryJavaSound`
- Compatibility markers were cleaned to avoid LWJGL2 wording in comments/docs.
- `org.lwjgl.Sys#getVersion()` now reports actual LWJGL3 version info through `org.lwjgl.Version`.

## Follow-up fixes included

- Fixed a generic iterator typing issue in `net.minecraft.util.Cartesian` to keep modern `javac` builds clean.

## Key commits

- `0032841` Remove LWJGL2 leftovers and add sound backend fallback
- `1a81a18` Report actual LWJGL3 version in Sys
- `5b3310c` Fix Cartesian generic iterator typing
