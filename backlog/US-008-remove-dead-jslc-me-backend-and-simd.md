# US-008 — Remove the dead jslc ME backend and `AccelType.SIMD`

**Status:** 📋 Ready · **Found:** 2026-09-13, footprint audit for the `decora_sse` deletion · **Deferred from:** the `decora_sse` deletion

## Story
As a maintainer of the fork,
I want the unused JSL compiler backend and the leftover SIMD accel type gone,
so that the codebase stops carrying a JNI code generator and names that no longer mean anything.

## Problem
- **ME backend:** the jslc ME backend is under `modules/javafx.graphics/src/jslc/java/com/sun/scenario/effect/compiler/backend/sw/me/`. That is 4 files and about 1,634 lines, plus about 184 lines of `.stg` templates under the matching `resources` path. `MENativeGlue.stg` still emits JNI C. Nothing invokes it: no build step passes `-me`, and the `OUT_ME_JAVA` / `OUT_ME_NATIVE` flags have no caller. Upstream carries the same dead code.
- **SIMD accel type:** `AccelType.SIMD` (`Effect.java` around `:526-528`) and the `case SIMD:` in `BoxRenderState` (around `:302`) are unreachable. Nothing reports SIMD since `decora_sse` was deleted.

## Acceptance criteria
- Delete the ME backend (`.java` and `.stg`), remove the `OUT_ME*` constants, the `-me` option and its usage text from `JSLC.java`, and drop `AccelType.SIMD` and its `case`.
- `mvn -pl buildtools/jslc,modules/javafx.graphics clean test`: same test counts as before, and the generated `target/gensrc/jsl-decora` is name-and-md5 identical before and after.
- A `git grep` for `OUT_ME`, `MEBackend`, `backend.sw.me` and `SIMD` finds nothing outside history comments.
- The Linux (WSL) graphics build is green.

## Notes
- Pure cleanup, with no rendering change. Keep `OUT_*` bit values stable if any external tool passes numeric masks; none is known.
