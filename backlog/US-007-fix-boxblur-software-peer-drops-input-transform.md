# US-007 — Keep the input transform in the software BoxBlur peer

**Status:** 📋 Ready · **Found:** 2026-09-13, impact assessment for the `decora_sse` deletion (scene snapshots) · **Deferred from:** the `decora_sse` deletion

## Story
As a JavaFX app developer applying a box blur to an offset image input on the software pipeline,
I want the blurred result drawn where the GPU pipeline draws it,
so that `prism.order=sw` (and fallback machines) don't misplace blurred content.

## Problem
`JSWBoxBlurPeer` returns `new ImageData(fctx, dst, bounds)` without `inputs[0].getTransform()`. The deleted `SSEBoxBlurPeer` dropped it too.

- `BoxBlur(input = ImageInput(img, x, y))` on a translated node renders at the wrong position on the software pipeline: max per-channel difference 255 vs D3D. The GPU path is correct.
- The same bug was fixed for BoxShadow in the deletion: `JSWBoxShadowPeer.java:141` now passes the input transform, matching upstream JDK-8093087. BoxBlur was left alone because both software backends agreed.

## Proposed fix
Pass `inputs[0].getTransform()` into the returned `ImageData` in `JSWBoxBlurPeer`, as `JSWBoxShadowPeer` now does.

## Acceptance criteria
- A test that fails before the fix and passes after: a translated `ImageInput` under a spread-0 box blur. Assert the result transform and pixel placement against an oracle that doesn't use the peer (the D3D snapshot, or an unblurred-offset computation).
- **The golden pins today's behaviour.** The two `translated/BoxBlur` rows in `decora-sse-win-golden.txt` store an identity transform (`tx = I`). The fix will fail those rows by design. Handle it as a reviewed test change: add a documented cause for exactly those rows. **Never regenerate the golden.**
- Snapshot check on D3D vs sw for the same scene.

## Notes
- Consider filing upstream with the BoxShadow half (JDK-8093087).
