# US-006 — Grow the Gaussian pass-0 clip by the vertical radius

**Status:** 🔍 Needs repro · **Found:** 2026-09-13, impact assessment for the `decora_sse` deletion (code reading, finding B2) · **Deferred from:** the `decora_sse` deletion

## Story
As a JavaFX app developer blurring or shadowing a node with a larger vertical radius than horizontal radius,
I want partial repaints to render the same pixels as a full repaint,
so that dirty-region updates don't leave clipped or faded bands.

## Problem
`GaussianRenderState` (around `:466` at `f9d06d85cc`) grows the pass-0 clip by `radiusX`. The vertical padding that pass 1 needs is `radiusY`.

- It matters when pass 0 runs and `ceil(radiusX) < ceil(radiusY)`, under a clip that cuts the result.
- It is shared by every backend that uses this render state: the Java software peers and the GPU `PPS` peers. The deleted SSE peers had it too.
- Found by code reading only. **No pixel reproduction exists yet.**

## Acceptance criteria
- First, a reproducing test: an anisotropic Gaussian (for example `DropShadow` or `GaussianBlur` with `rY > rX`) rendered clipped vs unclipped-then-cropped through the production peer protocol (`DecoraBackend`). It must show a difference beyond one step near the clip's top and bottom edges. If it can't be reproduced, close the story with the evidence.
- The fix grows the clip by the radius for the pass's direction. The reproducing test passes, and `DecoraJavaGoldenTest` stays green.
- Check the hardware path (D3D, ES2) for the same scene.

## Notes
- Related to the F2 fix already landed in `JSWLinearConvolvePeer` (pass-0 destination bounds). This is the render-state side.
