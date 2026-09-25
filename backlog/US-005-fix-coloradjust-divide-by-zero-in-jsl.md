# US-005 — Fix the ColorAdjust divide-by-zero in `ColorAdjust.jsl`

**Status:** ✅ Done (2026-09-25, uncommitted) · **Found:** 2026-09-08, Decora parity study (finding F1) · **Deferred from:** the `decora_sse` deletion (2026-09-14)

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

## Resolution (2026-09-25, uncommitted)
- **Fix:** `ColorAdjust.jsl` `rgb_to_hsb` now reads `if (cmax > cmin && cmax != 0.0) {`, so a pixel whose largest
  channel lands on exactly 0 takes the grey branch (h = s = 0) instead of dividing by `cmax`. At full contrast such
  a pixel renders premultiplied white `(a,a,a,a)` on every backend.
- **Tests:**
  - New `test.com.sun.scenario.effect.ColorAdjustZeroMaxChannelTest`, 7 cases.
    `fullContrastRendersZeroMaxChannelAsWhite` covers 6 premultiplied pixels (the zero channel in red, green
    and blue, low and high alpha), and `fullContrastRendersZeroMaxChannelPixelsOfTheCorpusAsWhite` covers the
    corpus pixel (21,9). All 7 fail before the fix and pass after.
  - `test/com/sun/scenario/effect/**`: 363 run, 0 failures.
- **Generated sources:** 4 files, each changing only the `if` line:
  - `JSWColorAdjustPeer.java`
  - `ColorAdjust.hlsl`, plus the rebuilt `ColorAdjust.obj`, the only decora `.obj` that changed
  - `ColorAdjust.frag` (ES2)
  - `ColorAdjust.metal`

  This was checked by diffing all three GPU backends plus the Java peer before and after. `PPSColorAdjustPeer.java`
  and the Metal headers are unchanged, and the JSL comment is not carried into any generated file.
- **Golden test, by cause:** the golden was **not regenerated**. `DecoraCorpus.Cause` gained a `fixed` flag, and F1 is
  marked fixed, so its pixels are held to the row bound instead of being explained.
  - The Java peer now equals the native golden on every F1 pixel (1 at 64x48, 19 at 257x129).
  - The full-contrast 64x48 row is now exact.
  - `causePredicateIsNarrow` was replaced by `fixedCauseMatchesNativeOnItsPixels` and `fixedCauseExplainsNoPixel`.
    The latter keeps a negative control for the explained-count check.
  - Reverting the JSL fix makes `DecoraJavaGoldenTest` fail as well.
- **Visual check:** `new ColorAdjust(1,1,1,1)` was applied through `Node.snapshot` to the regression pixels and to the
  64x48 and 257x129 corpus inputs, on four pipelines:
  - D3D (AMD Radeon R7 240, D3D9Ex)
  - ES2 over WGL on the same GPU (a privately built `prism_es2.dll`)
  - ES2 over GLX on Mesa llvmpipe under Xvfb in WSL
  - the Java SW pipeline

  Every F1 pixel is premultiplied white on all of them. The three GPU outputs are pixel-identical to each other and
  within one step of SW (11 of 33,134 ordinary pixels at 257x129). The pre-fix GPU shaders already rendered white on
  AMD D3D9, AMD OpenGL and llvmpipe, so only the Java peer showed the bug on the hardware available. Metal is
  unverified, because there is no macOS host.
