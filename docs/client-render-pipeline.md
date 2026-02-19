# Client Render Pipeline

## Ownership and boundaries

- `net.minecraft.client.Minecraft` owns NanoVG runtime lifecycle:
  - context creation/destruction (`initializeNanoVG`, `destroyNanoVG`)
  - frame begin/end (`beginNanoUiFrame`, `endNanoUiFrame`)
- Client-layer code must not create/destroy Nano contexts directly.
- Render safety checks are enforced by `scripts/check-render-rules.ps1`.

## Frame flow

Per frame, render execution is:

1. World/UI base render and framebuffer present in `Minecraft`.
2. Build `DisplayMetrics` from current scaled + framebuffer dimensions.
3. Attempt Nano begin phase (`beginNanoUiFrame`):
   - push GL attrib/matrix guards
   - sanitize GL state for Nano
   - `NanoVGContext.beginFrame(...)`
4. Dispatch client 2D render through:
   - `ClientBootstrap.onRender2D(RenderContext2D)`
   - event bus phase
   - module `onRender2D`
   - HUD render
   - active `NanoRenderableScreen.renderNano`
5. Finalize Nano phase (`endNanoUiFrame`):
   - `NanoVGContext.endFrame()`
   - restore matrix/attrib guards

## ClientBootstrap render stages

`ClientBootstrap.onRender2D` is intentionally split into isolated stages:

- `dispatchRenderEvent`
- `renderModules`
- `renderHud`
- `renderNanoScreen`

Each stage has its own exception boundary to prevent one failure from breaking the whole frame.

## Failure handling

- Nano begin/end failures are counted by `nanoFrameFailureCount`.
- Exceeding threshold disables Nano runtime until restart.
- When Nano is unavailable, `RenderContext2D` carries `null` Nano context and Nano-only screens skip drawing.

## Guidelines for future render changes

- Keep frame ownership in `Minecraft`; do not spread begin/end logic into UI/module code.
- Use `RenderContext2D` + `NanoVGContext` wrappers instead of raw GL/NVG calls for lifecycle behavior.
- If adding new render phases, keep ordering explicit and failure-isolated.
