# US-001 — Migrate `glass/gtk`, `glass/mac` and `prism_mtl` from JNI to FFM

| Field | Value |
| --- | --- |
| **Status** | 🔶 **Open** — Linux half (`glass/gtk`, 99 natives) unblocked 2026-09-15: WSL builds and tests the module headless; macOS half (`glass/mac` 155, `prism_mtl` 60) blocked on a macOS host |
| **Parent epic** | Fully remove JNI from `javafx.graphics` (replaced by a functioning FFM API) |
| **Branch of record** | `ffm/graphics` |
| **Repository** | `Ben-Esquivel-Music/jfx-ffm` |
| **Priority** | Medium (blocking for "100% JNI-free `javafx.graphics`", not blocking for the Windows deliverable) |
| **Estimate** | XL — 3 independent workstreams, ~44k LOC of native code, 222 Java `native` methods |
| **Blocked by** | No macOS or Linux build/test capability on the current development machine |

---

## Story

> **As** a JavaFX platform maintainer,
> **I want** the Linux (`glass/gtk`), macOS (`glass/mac`) and Metal (`prism_mtl`) native
> layers migrated from JNI to the Java 22+ Foreign Function & Memory API,
> **so that** `javafx.graphics` contains no JNI on *any* supported platform and the module
> can eventually drop `jni.h`, generated JNI headers, and the `javac -h` build step entirely.

---

## Why this is being de-scoped now

The parent epic is being delivered on a **Windows-only development machine**. These three
libraries are gated behind `mac.cmake` and `linux.cmake` in
`modules/javafx.graphics/native/CMakeLists.txt` and **cannot be compiled, linked, or executed
here at all**:

| Library | Requires | Available on this machine |
| --- | --- | --- |
| `glass/gtk` | Linux + GTK 3 dev headers, X11/Wayland session | ❌ |
| `glass/mac` | macOS + Xcode + Cocoa/AppKit (Objective-C) | ❌ |
| `prism_mtl` | macOS + Xcode + a Metal-capable GPU | ❌ |

Migrating them regardless would mean rewriting the **most user-visible and least
unit-testable** parts of the toolkit — window management, input, clipboard, drag-and-drop,
screen/DPI handling, platform accessibility, and the entire macOS render pipeline — with
**no ability to compile the C/Objective-C, no ability to run a single test, and no way to
observe a regression**. That directly violates the epic's governing constraint that
migrations must be **behaviour-neutral and provably so**. Unverifiable churn in these files
is a net negative, so the work is parked behind an explicit readiness gate rather than
attempted blind.

**What remains in the current epic:** the Windows-buildable surface only — `prism_d3d`,
`prism_sw` / Pisces, `prism_common`, `decora_sse`, `javafx_font*`, `javafx_iio`, and
`glass/win`.

---

## Scope

### In scope

Measured on `ffm/graphics` at the time of de-scoping:

| Area | Native files | Files containing JNI | Native LOC | JNI upcalls (`Call*Method`) | Java files | Java `native` methods |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `src/main/native-glass/gtk` | 130 | 25 | 22,247 | 102 | 22 | 96 |
| `src/main/native-glass/mac` | 100 | 41 | 17,239 | 127 | 20 | 67 |
| `src/main/native-prism-mtl` | 24 | 6 | 4,353 | 1 | 17 | 59 |
| **Total** | **254** | **72** | **43,839** | **230** | **59** | **222** |

Highest-density Java classes (native method count):

- **GTK** — `GtkWindow` (29), `GtkApplication` (17), `GtkView` (13), `GtkRobot` (9),
  `GtkSystemClipboard` (8), `ScreencastHelper` (7), `GtkDnDClipboard` (6)
- **macOS** — `MacPasteboard` (15), `MacAccessible` (13), `MacMenuDelegate` (11),
  `MacApplication` (8), `MacWindow` (7), `MacView` (3), `MacSystemClipboard` (3)
- **Metal** — `MTLContext` (34), `MTLShader` (11), `MTLRTTexture` (6), `MTLTexture` (4)

Build targets affected: `add_jfx_library(glass …)` in both `mac.cmake` and `linux.cmake`,
and `add_jfx_library(prismMTL OUTPUT_NAME prism_mtl …)` in `mac.cmake`.

### Out of scope

- Any Windows-targeted library (delivered by the parent epic).
- `glass/ios`, `glass/monocle` and other already-removed or non-desktop backends.
- Changing rendering behaviour, redesigning the Glass/Prism SPI, or "improving" platform
  behaviour while migrating. This story is a **transport-layer change only**.

---

## Definition of Ready (entry gate)

This story **must not be started** until *all* of the following are true. This is the whole
reason it was de-scoped.

- [ ] A macOS host with Xcode and a Metal-capable GPU is available for build **and**
      interactive test runs (`-DFULL_TEST` / `-DUSE_ROBOT`).
- [ ] A Linux host with GTK 3 development headers and a real X11 and/or Wayland session is
      available for build **and** interactive test runs.
- [ ] CI runners exist for both platforms so regressions are caught on every push, not only
      on a developer's desk.
- [ ] A **green baseline** has been captured on both platforms from unmodified JNI code —
      full `javafx.graphics` module tests plus `tests/system` — so post-migration results
      have something to be compared against.
- [ ] The Windows portion of the parent epic is merged, so the `*Native` facade pattern is
      settled and this story is a mechanical application of it rather than a parallel
      invention.

---

## Acceptance criteria

1. **No JNI remains in the three libraries.** No `JNIEXPORT` symbol, no `#include <jni.h>`,
   and no `JNIEnv*` parameter survives in `native-glass/gtk`, `native-glass/mac` or
   `native-prism-mtl`.
2. **No Java `native` declarations remain** in `com.sun.glass.ui.gtk`, `com.sun.glass.ui.mac`
   or `com.sun.prism.mtl`. All 222 are served by FFM downcalls.
3. **A plain C ABI is exposed** by each library — `extern "C"` (and plain C shims over
   Objective-C for `glass/mac`) with explicit, primitive-typed parameters. No JNI types
   appear in any exported signature.
4. **All 230 upcalls are re-expressed as callback tables** — a struct of function pointers
   registered from Java via `Linker.upcallStub`, replacing every `Call*Method`. Upcall stubs
   must not be able to propagate a Java exception across the native boundary.
5. **Thread confinement is preserved.** AppKit calls on `glass/mac` still occur on the macOS
   main thread; GTK calls on `glass/gtk` still occur on the GTK main loop thread; `MTLContext`
   work still occurs on the correct render thread. Upcall stubs are bound to arenas whose
   lifetime provably outlives every native use.
6. **Behaviour is unchanged**, demonstrated by the baseline captured in the Definition of
   Ready: module tests plus `tests/system` pass on macOS and Linux with results equal to the
   pre-migration baseline. No test is deleted or `@Disabled` to make the suite pass.
7. **`prism_mtl` is visually verified** — Metal rendering output is pixel-compared against a
   pre-migration golden capture, since Metal correctness is not meaningfully covered by unit
   tests.
8. **Accessibility still works.** `MacAccessible` is exercised against real VoiceOver and
   `GtkView`/ATK against a real Linux screen reader. This is manual and non-negotiable —
   accessibility regressions are invisible to the automated suite.
9. **Build is clean.** `mac.cmake` and `linux.cmake` no longer reference `JDK_HOME` JNI
   headers or `HEADERS_DIR` for these targets, and the `javac -h` step is no longer required
   for them.
10. **`ffm-reviewer` sign-off** on the final diff for each of the three libraries.

---

## Sub-tasks

Three genuinely independent workstreams; they can be run in parallel by separate agents once
the gate is open.

### A. `prism_mtl` — do this first (smallest, cleanest)

Only 6 JNI files and a single upcall, so it is the best place to prove the pattern on Apple
hardware before touching windowing code.

- [ ] `jni-auditor` inventory of `native-prism-mtl` + `com.sun.prism.mtl`, with an
      OS-CALL / WRAPPER / PURE / PURE-HOT verdict per function.
- [ ] Capture golden render output from the unmodified JNI build.
- [ ] Define the C ABI header and export table.
- [ ] `MTLNative` facade (`MTLContext`'s 34 methods first, then `MTLShader`, `MTLRTTexture`,
      `MTLTexture`) with `StructLayout`s validated against `sizeof` on-device.
- [ ] Flip callers, delete JNI, update `mac.cmake`.
- [ ] Pixel-parity test against the golden capture.

### B. `glass/gtk`

- [ ] `jni-auditor` inventory of `native-glass/gtk` + `com.sun.glass.ui.gtk` (102 upcalls).
- [ ] Design the GTK → Java callback table; verify stub lifetime against the GTK main loop.
- [ ] `GtkNative` facade, migrating in dependency order:
      `GtkApplication` → `GtkWindow` → `GtkView` → clipboard/DnD → `GtkRobot` /
      `ScreencastHelper`.
- [ ] Verify Wayland **and** X11 separately — they are different code paths.
- [ ] Robot-driven system tests, then ATK/screen-reader verification.

### C. `glass/mac`

Hardest of the three: Objective-C, `objc_msgSend`, AppKit main-thread confinement, and the
`NSAccessibility` protocol.

- [ ] `jni-auditor` inventory of `native-glass/mac` + `com.sun.glass.ui.mac` (127 upcalls).
- [ ] Add plain-C shims over the Objective-C surface — **do not** attempt to call
      `objc_msgSend` directly from Java; its variadic ABI is not portably expressible via
      `Linker`.
- [ ] Callback table + arena strategy that respects AppKit main-thread rules.
- [ ] `MacNative` facade in dependency order: `MacApplication` → `MacWindow` → `MacView` →
      `MacPasteboard` / `MacSystemClipboard` → `MacMenuDelegate` → `MacAccessible` (last).
- [ ] VoiceOver verification of `MacAccessible`.

### D. Cross-cutting close-out

- [ ] Remove the now-dead `javac -h` / `HEADERS_DIR` plumbing for these targets.
- [ ] Update `README` / build docs to state that `javafx.graphics` is JNI-free.
- [ ] `ffm-reviewer` pass over the combined diff.

---

## Suggested agents and skills

Reuse the same toolchain the Windows work used — do not improvise a new process.

| Role | Agent |
| --- | --- |
| Pre-migration inventory + migrate-or-delete verdict | `jni-auditor` |
| Java-side implementation (**only** agent permitted to touch Java) | `java-26` |
| FFM facade + C ABI implementation | `ffm-migrator` |
| Diff review for ABI/lifetime/thread-confinement bugs | `ffm-reviewer` |
| Test authoring and golden-capture parity tests | `ffm-test-porter` |
| CMake / toolchain changes | `native-build-engineer` |
| Metal pipeline specifics | `graphics-rendering-engineer` |

Skills: `jni-to-ffm-migration`, `jfx-graphics-native`, `jfx-native-build`, `jfx-ffm-testing`,
`openjfx-conventions`.

Precedent to follow: `com.sun.prism.d3d.D3DNative` and `com.sun.pisces.PiscesNative` on
`ffm/graphics`.

---

## Risks

| # | Risk | Impact | Mitigation |
| --- | --- | --- | --- |
| 1 | AppKit main-thread violations introduced by upcall stubs | Deadlock or crash on macOS | Explicit thread-confinement assertions; `ffm-reviewer` gate |
| 2 | Arena lifetime bug frees an upcall stub still referenced by GTK/AppKit | Use-after-free, hard to reproduce | Global/shared arenas for long-lived callbacks; leak-check tests |
| 3 | `objc_msgSend` variadic ABI is not expressible via `Linker` | Blocks `glass/mac` | Mandatory plain-C shim layer; never call `objc_msgSend` from Java |
| 4 | Accessibility regressions escape automated tests | Ships broken for assistive-tech users | Manual VoiceOver + Linux screen-reader verification is an AC, not optional |
| 5 | Metal struct layout / alignment mismatch | Silent corruption or GPU crash | Validate every `StructLayout` against on-device `sizeof`/`alignof` |
| 6 | Scope creep into rendering or SPI redesign | Unreviewable diff | Transport-layer change only; behaviour neutrality outranks elegance |
| 7 | Wayland vs X11 divergence in `glass/gtk` | Half the Linux users regress | Both session types tested separately |

---

## Definition of Done

- All 10 acceptance criteria met.
- macOS and Linux CI green, matching the pre-migration baseline.
- `ffm-reviewer` sign-off recorded for each of the three libraries.
- Changes left in the working tree / a review branch — **no commits, merges or PRs without
  explicit approval from the repository owner.**

---

## References

- `modules/javafx.graphics/native/CMakeLists.txt` (lines 57–66 — platform dispatch)
- `modules/javafx.graphics/native/mac.cmake` (lines 160, 203 — `glass`, `prismMTL` targets)
- `modules/javafx.graphics/native/linux.cmake` (line 137 — `glass` target)
- `modules/javafx.graphics/src/main/java/com/sun/prism/d3d/D3DNative.java` — reference pattern
- `modules/javafx.graphics/src/main/java/com/sun/pisces/PiscesNative.java` — reference pattern
