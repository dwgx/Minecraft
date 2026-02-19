# Module Migration Phase 1

## Strategy

This phase follows:

- staged migration with compile-safe increments
- Rise-first behavior patterns
- FDP utility-style reinforcement
- core category coverage (`combat/movement/player/other/render/client`)

## New Shared Utilities (`dwgx.modulekit`)

- `PulseMath`: click/inventory cadence and randomized delay helpers
- `InventoryProbe`: chest loot scoring + tool-slot selection
- `SightProbe`: entity/block raycast verification
- `ModuleKit`: facade entry for module usage

Design goal:

- keep module code compact
- avoid copy-paste heuristics across combat/world/misc modules
- preserve existing config keys and IDs in old modules

## New Core Modules Added

- `AutoClicker` (`combat`) - CPS scheduler with optional weapon-only gate
- `AutoTool` (`world/player`) - hotbar tool auto-switch while mining
- `Stealer` (`world/player`) - chest looting with useful-item and close-delay logic
- `InventoryMove` (`misc/other`) - movement controls while inventory GUI is open
- `TargetPanel` (`render`) - compact target HUD linked to KillAura target

## Existing Modules Refined

- `KillAura` now reuses `ModuleKit` for click cadence and raycast hit verification
- `Scaffold` raycast verification now reuses `ModuleKit` placement matching

## Compatibility Notes

- existing module IDs and setting keys were not renamed
- `modules.json` compatibility remains intact for old modules
- new modules append to registry without altering old IDs

## Next Phase (Planned)

- split larger module internals into strategy classes (combat/movement)
- add per-module anti-regression smoke tests
- migrate more player/misc/render modules with same utility contract

