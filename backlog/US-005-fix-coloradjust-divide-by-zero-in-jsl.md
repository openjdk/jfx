# US-005 — Fix the ColorAdjust divide-by-zero in `ColorAdjust.jsl`

**Status:** 📋 Ready · **Found:** 2026-09-08, Decora parity study (finding F1) · **Deferred from:** the `decora_sse` deletion (2026-09-14)

## Story
As a JavaFX app developer using `ColorAdjust`,
I want pixels whose brightest channel lands exactly on 0 to render a defined colour,
so that the effect looks the same on every pipeline, and doesn't depend on how the driver or JIT handles NaN.

## Problem
`rgb_to_hsb` in `modules/javafx.graphics/src/main/jsl-decora/ColorAdjust.jsl` computes `s = (cmax - cmin) / cmax` inside `if (cmax > cmin)`.

- A positive contrast can push a channel to exactly 0.0 while another channel goes negative. Then `cmax == 0`, `s` is +Inf, and `hsb.y += (1 - s) * sat` becomes NaN.
- The Java peer renders two channels black on those pixels. The old native `/fp:fast` build rendered white. GPU output depends on the driver.
- It is reachable whenever contrast > 0 and saturation > 0. It only hits a small number of pixels: 1 of 3,072 at 64x48 and 19 of 33,153 at 257x129 in the test corpus.

## Proposed fix
`ColorAdjust.jsl:61`: `if (cmax > cmin) {` becomes `if (cmax > cmin && cmax != 0.0) {`. JSL has `&&` but no ternary.

## Acceptance criteria
- A test that fails before the fix and passes after, on the full-contrast case (hue = sat = bri = con = 1).
- The regenerated outputs are exactly the expected set: `JSWColorAdjustPeer.java`, the D3D HLSL, ES2 GLSL and Metal shaders, and `PPSColorAdjustPeer.java` if its text changes. Diff `target/gensrc/jsl-decora` before and after.
- `DecoraJavaGoldenTest` stays green. Its F1 cause predicate currently explains exactly these pixels; update it by cause, and **never regenerate the golden**. The affected rows (full contrast at both sizes, and `ColorAdjust hue=-0.25 sat=0.90 bri=-0.60 con=0.40 257x129`) are stored as full frames, so they stay judgeable.
- Visual check on the D3D and ES2 pipelines for the same input.

## Notes
- Changes all three backends (Java software peer and GPU shaders), which is why it was kept out of the deletion.
- Evidence: Decora parity study, section 4 "F1".
