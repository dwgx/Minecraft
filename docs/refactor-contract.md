# Refactor Contract (Strict Compatibility)

This repository is under a strict compatibility refactor contract.

## Non-negotiable constraints

1. Keep user-visible behavior stable unless an explicit compatibility note exists.
2. Keep persisted identifiers stable:
   - module IDs
   - setting keys
   - enum constant names used in config
3. Keep Java source/target at `1.8`.
4. Preserve Nano frame ownership in `net.minecraft.client.Minecraft`.
5. All `net/` and `org/` edits must be documented in `docs/net-org-patch-manifest.md`.

## Change policy

1. Prefer extraction and delegation over logic rewrites.
2. Move duplicated logic to shared layers before introducing new behavior.
3. Keep compatibility shims when introducing new package structure.
4. Every structural migration must keep a compile-safe bridge path.

## Required checks before merge

1. `scripts/check-all.ps1`
2. `scripts/check-render-rules.ps1`
3. `scripts/check-encoding.ps1`
4. `scripts/check-checkstyle.ps1`
5. `scripts/check-spotbugs.ps1`
6. `scripts/check-refactor-guards.ps1`

