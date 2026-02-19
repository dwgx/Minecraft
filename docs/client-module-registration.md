# Client Module and HUD Registration

## Registration entry points

- Modules: `client.module.ModuleRegistry.registerBuiltins(ModuleManager)`
- HUD elements: `client.hud.HudRegistry.registerBuiltins(HudManager)`
- Bootstrap wiring:
  - `ClientBootstrap.registerBuiltinModules()`
  - `ClientBootstrap.registerBuiltinHudElements()`

## Why this structure

- Keeps builtin lists centralized and predictable.
- Preserves registration order in one place.
- Reduces hardcoded constructor sprawl inside bootstrap/runtime code.

## Config compatibility contract

When changing registration lists, keep these stable unless an explicit migration is added:

- module IDs (`Module#getId`)
- setting keys
- enum constant names persisted in config

Current config files:

- `client.json`
- `modules.json`
- `hud.json`

`ConfigManager` deserializes by module ID and setting key; changing identifiers without migration breaks user configs.

## Adding a new module

1. Implement module class under `src/client/module/impl/...`.
2. Add a single `modules.register(new YourModule())` line in `ModuleRegistry`.
3. Keep the new module ID unique and stable.
4. Verify config load/save still works (`modules.json` round-trip).

## Adding a new HUD element

1. Implement element class under `src/client/hud/...`.
2. Add `hud.register(new YourHudElement())` in `HudRegistry`.
3. Ensure transform defaults are sane for first launch.
4. Verify `hud.json` load/save and HUD editor behavior.
