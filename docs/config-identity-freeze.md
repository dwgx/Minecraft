# Config Identity Freeze

This project uses strict config identity compatibility.

## Frozen identity scope

1. Module IDs (`Module#getId`)
2. Setting keys (`Setting#getKey`)
3. Persisted enum constants used by:
   - `EnumSetting`
   - serialized module/hud transform enums

Changing any of these requires an explicit migration and audit note.

## Source of truth

1. Module registration: `src/client/module/ModuleRegistry.java`
2. HUD registration: `src/client/hud/HudRegistry.java`
3. Config persistence: `src/client/config/ConfigManager.java`
4. Generated freeze snapshot: `docs/config-identity-freeze.generated.md`

## Regeneration

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-config-freeze.ps1
```

This regenerates the snapshot file from current source declarations.

