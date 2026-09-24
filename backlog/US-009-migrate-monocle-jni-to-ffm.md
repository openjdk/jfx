# US-009 — Keep Monocle (embedded Linux) and migrate it from JNI to FFM

| Field | Value |
| --- | --- |
| **Status** | ✅ **S1–S8 and D3 landed in the working tree 2026-09-23 (uncommitted)** — no `native` method under `com.sun.glass.ui.monocle` or `com.sun.prism.es2.Monocle*`, `native-glass/monocle` holds only the two EGL headers, `native-prism-es2/monocle` only `prism_es2_api_monocle.c`; gated on Windows (module suite) and WSL Linux (natives rebuilt, module suite, S0 suite, Xvfb boot + ES2 render, aarch64 static-assert probe). The CMake option is spelled `INCLUDE_ES2_MONOCLE` (AUTO/ON/OFF). Remove this file with the commit that closes the story |
| **Parent epic** | Fully remove JNI from `javafx.graphics` (replaced by a functioning FFM API) |
| **Branch of record** | `ffm/graphics` |
| **Repository** | `Ben-Esquivel-Music/jfx-ffm` |
| **Priority** | Medium — keeps embedded Linux as a future capability; nothing on the desktop depends on it |
| **Estimate** | L — 195 `native` declarations (187 glass + 8 prism), 190 `JNIEXPORT` in 12 C files, 3 528 lines of C; **194 of 195 natives are `WRAPPER`/`PURE`** and their C is deleted, **1** stays C behind the existing `es2_*` ABI |
| **Blocked by** | Nothing for S0–S8 on this machine (WSL). End-to-end hardware verification of EGL/DRM, framebuffer and e-paper needs a board nobody has; those slices are gated on stub libraries and layout goldens instead |
| **Evidence of record** | `US-009-monocle-ffm-research-dossier.md` (this folder) — four read-only research passes over the tree with file:line citations for every verdict; the per-lens JSON sources are in the Claude scratchpad of session `488a6e4a` |
| **Decisions taken** | 2026-09-22, by the user — see *Decisions* below |

---

## Story

> **As** a JavaFX platform maintainer,
> **I want** the Monocle Glass platform (Linux framebuffer, X11, EGL/DRM, e-paper, VNC, Headless)
> kept in the fork and migrated from JNI to the Java 22+ Foreign Function & Memory API,
> **so that** embedded Linux remains a reachable target for `javafx.graphics` while the module
> carries no JNI on Linux, and the Android/iOS ports stay deleted.

---

## Where Monocle stands today (verified 2026-09-22)

- **Nothing of it is built.** `native/linux.cmake` defines only `glassgtk3`, `prism_sw`,
  `prism_es2` and `javafx_iio`. `libglass_monocle.so`, `libglass_monocle_x11.so`,
  `libglass_monocle_epd.so` and `libprism_es2_monocle.so` have never existed in this fork. Every
  hardware `NativePlatform` dies in its constructor with `UnsatisfiedLinkError` (not caught by the
  cascade's `catch (Exception)`), so `-Dglass.platform=Monocle` on Linux without
  `-Dmonocle.platform=Headless` fails at startup.
- **Only Headless and VNC work**, because they are pure Java. Monocle-Headless drives the
  128-test touch/gesture robot suite in `tests/system` (`test/robot/com/sun/glass/ui/monocle`,
  opt-in via `UNSTABLE_TEST`), which is the repository's only automated feeder of touch, zoom,
  rotate and swipe events into a `Scene`. `MonocleUInput` is a Java pipe into
  `LinuxInputDeviceRegistry(true)`, so that suite needs **no native library and no display**.
- **The desktop jars exclude it** (`maven-jar-plugin` profiles for Windows, Linux and macOS).
- **Upstream** has not deprecated Monocle but has stopped investing: the Monocle native tree was
  last changed in 2022, it is excluded from every upstream desktop SDK, `armv6hf` is the only
  cross target that enables it and the wiki calls it untested, and since jfx26 (JDK-8324941)
  `com.sun.glass.ui.headless` is the endorsed replacement for Monocle-Headless in testing.
  Embedded users run Gluon's builds with Gluon's DRM/KMS EGL library through `-Dmonocle.egl.lib`.
- **Android and iOS are gone** (PR #12). The runtime selectors that still routed to them were
  fixed on 2026-09-22 (uncommitted): `Toolkit.getDefaultToolkit`, glass `Platform`,
  `PrismSettings`, es2 `GLFactory`, `NativePlatformFactory.DEFAULT_PLATFORM_ORDER`
  (`Android` dropped), `NativeLibLoader`, `PrismFontFactory`/`AndroidFontFinder`, guarded by
  `test.com.sun.glass.ui.monocle.NativePlatformFactoryTest`.

---

## The central finding: Monocle is glue, not engine

Every Monocle C function was inventoried and triaged (`jni-to-ffm-migration` §1.1). The C never
calls back into Java: no `Call*Method`, no cached IDs, no global refs, no `AttachCurrentThread`.
So there are **no upcall stubs, no callback tables and no registries** to design.

| Unit | Natives | Verdict | FFM replacement |
| --- | ---: | --- | --- |
| `LinuxSystem` (`linux/LinuxSystem.c`) | 46 | 20 `WRAPPER` (one libc call each: open/close/read/write/lseek/mkfifo/setenv/sysconf/ioctl/dlopen/dlsym/dlclose/dlerror/strerror/mmap/munmap/memcpy) + 26 `PURE` (struct accessors on `input_absinfo` / `fb_var_screeninfo`, `_IOC` arithmetic) | libc through `Linker.nativeLinker().defaultLookup()` (house pattern `FontConfigNative.Libc`, `GtkGlassNative.Loader`); `StructLayout` + `VarHandle`; ioctl numbers computed in Java from the asm-generic encoding; `errno` via `captureCallState`; `open` and `ioctl` bound with `firstVariadicArg` |
| `EPDSystem` (`epd/EPDSystem.c`) | 44 | 1 `WRAPPER` (`ioctl` with an int by address) + 43 `PURE` (the rest of the 40-field `fb_var_screeninfo`) | Shares the full 160-byte layout with `LinuxSystem`; MXCFB request structs are already Java byte buffers |
| `X` (`x11/X11.c`) | 44 | 27 `WRAPPER` (one Xlib call each) + 17 `PURE` (`XSetWindowAttributes` / `XEvent` / `XColor` accessors, `sizeof`) | `SymbolLookup.libraryLookup("libX11.so.6", Arena.global())`; the four Xlib macros bound as their function forms (`XDefaultScreenOfDisplay` …); `XEvent` = 24 C longs = 192 bytes on LP64 |
| `EGL` (`EGL.c`) | 15 | 9 `WRAPPER` over libEGL + 6 dead declarations; `setEGLAttrs` is the one `PURE` translation | `libraryLookup("libEGL.so.1")` with `libEGL.so` fallback; `setEGLAttrs` ported to Java against a golden captured from the C first (`PARITY: exact`) |
| `EGLPlatform`/`EGLScreen`/`EGLAcceleratedScreen`/`EGLCursor` (`egl/eglBridge.c`, `egl_ext.h`) | 23 | 23 `WRAPPER` — each forwards to one function the `-Dmonocle.egl.lib` vendor library exports (`getNativeWindowHandle`, `getEglDisplayHandle`, `doEgl*`, `doGet*`, cursor functions) | New `EglVendorNative`: `libraryLookup(System.getProperty("monocle.egl.lib"))`, 24 symbols bound with stdint descriptors; `egl_ext.h` republished JNI-free |
| `Udev` (`linux/Udev.c`) | 5 | 3 `WRAPPER` (netlink `socket`/`setsockopt`/`bind`, `recv`) + 2 `PURE` (uevent header sniffing). **libudev is not used** — the C speaks `NETLINK_KOBJECT_UEVENT` itself | libc sockets through the same lookup; `sockaddr_nl` is 12 bytes; header parsing in Java |
| `C` (`util/C.c`, `Monocle.h`) | 2 | `PURE` JNI glue (`NewDirectByteBuffer`, `GetDirectBufferAddress`) | `MemorySegment.ofAddress(p).reinterpret(n).asByteBuffer()` / `MemorySegment.ofBuffer(b).address()`; `C.Structure` keeps its `(ByteBuffer, long)` pair in the first pass so the eight struct subclasses compile unchanged |
| `MonocleGLFactory` (`native-prism-es2/monocle/MonocleGLFactory.c`) | 8 | **1 `OS-CALL`**: `nPopulateNativeCtxInfo` fills the C-owned `ContextInfo` (4 × `glGetString`, ~50 GL entry points) that every `es2_*` function consumes. 5 `PURE` constants, `nGetIsGL2` always false (field never written), `nInitialize` declared but never implemented | New export `es2_context_adopt(Es2ProcLoader loader, void* user)` in the ES2 ABI (version 2 → 3): the caller has made its own GLES context current; C builds the `ContextInfo` and resolves entry points through the loader. Java constants for the rest; `gl2 = false` |
| `MX6AcceleratedScreen` (`mx6/`) | 2 | `WRAPPER` around two Vivante function pointers Java already `dlsym`s | **Delete** (see *Decisions*) |
| `Dispman*` (`dispman/`, `wrapped_bcm.h`) | 6 | `WRAPPER`/`OS-CALL` into `libbcm_host.so` (legacy Raspberry Pi firmware) | **Delete** (see *Decisions*) |
| `eglUtils.c/.h`, `eglWrapper/` | 0 | Dead: reachable only from the never-implemented `nInitialize`; `eglWrapper.c` references `LENSPORT_LIBRARY_NAME`, defined nowhere | **Delete** |

**Result:** zero fork-owned glass libraries for Monocle (`libglass_monocle*` are replaced by
Java bindings to libc, `libX11.so.6`, `libEGL.so.1` and the vendor library), plus **one** new
CMake target `prism_es2_monocle` compiling the generic ES2 sources with `-DIS_EGLFB` and no
X11/GLX link. About 3 500 lines of C are deleted. `MonocleGLContext`'s `@Native` field is the
last thing making `javac -h` emit a Monocle header; it goes with the natives.

---

## Decisions (taken 2026-09-22)

| # | Question | Decision |
| --- | --- | --- |
| D1 | 32-bit ARM (`armv6hf`/`armv7`)? The JDK ships no native FFM `Linker` for Linux arm32, only a libffi fallback in some vendor builds | **LP64 only** (aarch64, x86-64). The facades read `Linker.nativeLinker().canonicalLayouts()` and refuse a 4-byte `long` with an `UnsatisfiedLinkError` naming the reason (the `FTNative.load` pattern). Fixes the `XEvent` 192/96-byte question |
| D2 | `egl_ext.h`, the fork's only third-party contract (Gluon's DRM library is the only known implementer), uses `jlong`/`jint`/`jboolean`/`jfloat` | **Republish JNI-free** (`int64_t`/`int32_t`/`uint8_t`/`float`) as `monocle_egl_ext.h`; binary-identical on LP64 so existing vendor libraries keep working; keep `egl_ext.h` as an alias for one release |
| D3 | All three jar profiles exclude `com/sun/glass/ui/monocle/**` and `com/sun/prism/es2/Monocle*` | **Include Monocle in the Linux jar once S8 lands** (own commit, after the JNI is gone); Windows and macOS keep excluding it |
| D4 | Pre-existing bugs found by the audit: `MAP_FAILED` compared as `0xffffffffL` (LP64 `mmap` failures pass as success), `LinuxSystemShim` argument-order bugs (`setenv`, and two more), `EPDInputDeviceRegistry` opens a device node and never closes it, `monocle.maliSignedStruct` depends on the private `Xlibint.h` `sizeof(struct _XDisplay)` | **Preserve in the migration commits** (behaviour-neutral), **fix in follow-ups** with their own tests |
| D5 | Hardware backends | **Keep and port**: Linux framebuffer, X11, EGL/DRM, EPD e-paper, VNC, Headless. **Delete**: Dispman (no dispmanx under KMS on Pi 4/5 or 64-bit Raspberry Pi OS), MX6 Vivante, OMAP sysfs (`-DOMAP3` is read by nothing in the tree) — none has a test path here |
| D6 | Loader shape for the ES2 context | Callback form `es2_context_adopt(Es2ProcLoader, void*)`: one export, Java can later add an `eglGetProcAddress` fallback without touching C; the loader stub lives in a confined arena scoped to the call because C stores the resolved pointers, never the loader |

---

## Slices — one behaviour-neutral commit each

| Slice | Content | Gate |
| --- | --- | --- |
| **S0** | Land the tree as it stands (Android/iOS selector fix, follow-ups, `NativePlatformFactoryTest`); record the Monocle-Headless baseline | `mvn -pl tests/system -am test -DFULL_TEST=true -DUSE_ROBOT=true -DUNSTABLE_TEST=true -Dtest='test/**/monocle/**/*Test'` on WSL, no display, never with `HEADLESS_TEST` (it forces `glass.platform=Headless`). Result recorded below |
| **S1** | `LinuxSystem` + `C` → libc facade; delete `LinuxSystem.c`, `C.c`; swappable backend seam for the framebuffer tests | new `LinuxSystemNativeTest` (mkfifo/open `O_NONBLOCK`→`ENXIO`, read/write round trip, ioctl on `/dev/null`→`ENOTTY`, `IOW('F',0x20,4)==0x40044620`, `EVIOCGABS(0)==0x80184540`, `fb_var_screeninfo` 160 / `input_absinfo` 24 with offsets); S0 suite unchanged; cross-arch static-assert probe |
| **S2** | `Udev` netlink → libc; delete `Udev.c` | uevent datagram goldens (both header formats), monitor socket opens unprivileged. **After S2 the framebuffer platform is JNI-free** |
| **S3** | `X` → `libX11.so.6`; delete `X11.c` | X11 Monocle boots on the rootless Xvfb with `-Dprism.order=sw -Dx11.geometry=640x480`, screen size asserted, XTest-driven mouse events through `X11InputDeviceRegistry` |
| **S4** | `EPDSystem`; delete `EPDSystem.c` | full 40-field layout golden; `EPDFrameBuffer` ioctl sequence against the fake backend; `EPDSettingsTest`/`FramebufferY8Test` unchanged. Hardware unverified — say so |
| **S5** | `EGL` facade; `setEGLAttrs` → Java; delete `EGL.c` | golden of `setEGLAttrs` captured from the C **before** deletion (general case and the 5/6/5/0 → `EGL_BUFFER_SIZE 16` case); Mesa surfaceless context smoke test (`EGL_PLATFORM=surfaceless`, llvmpipe) |
| **S6** | `es2_context_adopt` + `prism_es2_monocle` CMake target behind `INCLUDE_MONOCLE_ES2`; `IS_EGLFB` header hygiene; delete `MonocleGLFactory.c`, `eglUtils.*`, `eglWrapper/`; `ES2Native.LIBRARY_NAME` picks the library by `getEmbeddedType()` | `nm -D --undefined-only` shows libEGL/libGLESv2/libc only; ES2 symbol smoke against the new library; end-to-end render on Xvfb with `-Dglass.platform=Monocle -Dmonocle.platform=X11 -Dprism.order=es2` under llvmpipe, pixels read back with `xwd` |
| **S7** | Vendor bridge → `EglVendorNative`; `monocle_egl_ext.h`; delete `eglBridge.c` | 40-line stub vendor `.so` recording the call sequence; a `uint8_t 0` return read as `false` (catches a `JAVA_INT`/`JAVA_BYTE` mistake) |
| **S8** | Delete Dispman, MX6, OMAP (C, Java, `DEFAULT_PLATFORM_ORDER` → `X11,Linux,Headless`); then D3 (Linux jar includes Monocle) as its own commit | `NativePlatformFactoryTest`; `grep -rn 'jni.h\|JNIEnv' native-glass/monocle native-prism-es2` empty; Linux natives rebuilt from source and the graphics suite green on WSL |

Deletions (S8 and the dead ES2 helpers) may land **before** the ports to shrink the port diffs;
`DEFAULT_PLATFORM_ORDER` and its test change in the same commit as the deleted factories.

---

## Testing without hardware

WSL (Ubuntu, rootless `Xvfb.patched` in `~/xroot`, Mesa with `libegl-dev`/`libgles-dev`,
no `/dev/fb0`, `/dev/dri` or `/dev/input`) covers: Headless and the touch suite in-JVM, X11
Monocle on a private Xvfb, EGL through Mesa surfaceless, `prism_es2_monocle` end to end under
llvmpipe. Framebuffer, EPD and DRM use the fake `LinuxSystem` backend and stub `.so` files.
aarch64 ABI is proven by the static-assert cross-probe already built for the font work
(`~/font-abi-aarch64`: apt-downloaded arm64 dev packages + relocated clang, `checks.tsv` →
`_Static_assert(sizeof/offsetof == <Java value>)`, negative control kept). QEMU on the Windows
host is system-mode only (TCG for aarch64, no WHPX); usable for an occasional confidence run of
the Headless suite on an aarch64 JDK, not as a gate.

---

## Definition of Ready

- [x] Inventory with a verdict and evidence for every native (dossier)
- [x] Decisions D1–D6 taken by the user
- [x] Baseline run reproducible on WSL without a display
- [x] S0 baseline recorded in this file (three runs, 2026-09-22)

## Acceptance criteria

1. No `native` method remains in `com.sun.glass.ui.monocle` or `com.sun.prism.es2.Monocle*`;
   `native-glass/monocle` contains no C (only the `egl/` headers); `es2_context_adopt` lives in the generic
   `prism_es2_api.c` so every platform library exports it, and `native-prism-es2/monocle` contains only the
   `IS_EGLFB` lifecycle stubs (`prism_es2_api_monocle.c`); `native-prism-es2/eglWrapper` is gone.
2. The Monocle-Headless touch suite (S0 baseline) passes with the same per-class results after
   every slice.
3. X11 Monocle shows a stage and reports geometry on Xvfb with the sw pipe (S3) and the es2 pipe
   through `prism_es2_monocle` (S6).
4. Every `StructLayout` has a `sizeof`/`offsetof` check for x86-64 (runtime) and aarch64
   (static-assert probe); every facade refuses a non-LP64 data model with a named error (D1).
5. `setEGLAttrs` matches its captured golden exactly (S5).
6. `monocle_egl_ext.h` is published and `egl_ext.h` aliases it (D2).
7. All restricted FFM calls live in the facade classes; no `@SuppressWarnings("restricted")`
   outside them.
8. Commit notes state the deviation from the one-library-per-commit rule (Gradle's
   `libglass_monocle.so` bundled several facades; no build of this fork ever produced it).

## Definition of Done

- [ ] S1–S8 landed on `ffm/graphics`, each accepted on Windows (module suite, natives untouched)
      and WSL Linux (natives rebuilt from source, module suite, S0 suite)
- [ ] D3 commit: Linux jar includes Monocle
- [ ] Follow-up issues drafted for the D4 bugs
- [ ] This file, `INDEX.md` and the module skill inventory updated; the migration register
      records Monocle as *migrated* (fbdev, X11, EGL, EPD, VNC, Headless) and *deleted*
      (Dispman, MX6, OMAP)

---

## S0 baseline (2026-09-22) — recorded

Tree: HEAD `ab464538a2` plus the uncommitted PR #12 review fixes (22 files, see
`US-009-S0-commit-notes.md` in the Downloads register). Host: WSL Ubuntu, JDK 25, no display used,
natives from source (`-DskipNative=true` reused the same build). Command, run three times in a row:

```
mvn -B -ntp -pl tests/system -am test -DskipNative=true -DFULL_TEST=true -DUSE_ROBOT=true \
    -DUNSTABLE_TEST=true -Dtest='test/**/monocle/**/*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

Totals per run (34 `tests/system` classes; `NativePlatformFactoryTest` in the graphics module also
ran and passed each time):

| Run | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| 1 | 1 372 | 3 | 43 | 50 |
| 2 | 1 372 | 3 | 89 | 50 |
| 3 | 1 372 | 3 | 91 | 50 |

Per-test verdict across the three runs (`US-009-s0-monocle-baseline.tsv`): **stable-pass 1 222,
persistent 42, flaky 58, skipped 50.** Every error is a `TestLogShim$TestLogAssertion` 3 s timeout
waiting for a touch or mouse event; the three failures are event counts (RapidTap 19/20 and 17/20,
DragTouchInAndOutAWindow 6 instead of 0). The suite is the one upstream excluded in 2018 as
"tests that don't run well with Hudson builds" (JDK-8196607); our JVM configuration is identical to
the fork-point Gradle `UNSTABLE_TEST` run (gesture properties only under `HEADLESS_TEST`,
`unstable.test` passed through).

| Class | Tests | Stable pass | Persistent | Flaky | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| ZoomTest | 36 | 7 | 20 | 9 | 0 |
| MultiTouch2Test | 54 | 7 | 12 | 35 | 0 |
| RotateTest | 146 | 137 | 4 | 3 | 2 |
| RapidTapTest | 30 | 28 | 2 | 0 | 0 |
| DragTouchInAndOutAWindowTest | 61 | 52 | 1 | 1 | 7 |
| FuzzyTapTest | 75 | 74 | 1 | 0 | 0 |
| MultiTouch3Test | 9 | 7 | 1 | 1 | 0 |
| ScrollThresholdTest | 30 | 29 | 1 | 0 | 0 |
| ScrollTest | 210 | 170 | 0 | 4 | 36 |
| SwipeSimpleTest | 255 | 253 | 0 | 2 | 0 |
| DragAndDropTest, SingleTouchNonFullScreenTest, TouchButtonTest | 15 / 60 / 106 | 14 / 59 / 104 | 0 | 1 each | 0 / 0 / 1 |
| SingleTouchTest | 106 | 105 | 0 | 0 | 1 |
| FramebufferY8Test | 8 | 6 | 0 | 0 | 2 |
| SwipeTest | 1 | 0 | 0 | 0 | 1 |
| 19 other classes (CreateDevice, DoubleClick, EPDSettings, Framebuffer, HeadlessGeometry1/2, InputDeviceProperty, IntSet, ModalDialog, MonocleApplication, MouseLag, Robot, SimpleMouse, TouchEventLookahead, TouchException, TouchLag, TouchPipeline, USKeyboard) | 176 | 176 | 0 | 0 | 0 |

Observations that matter for later slices:
- The persistent set is the same 42 tests in all three runs and sits on the higher-numbered
  device profiles of each parameterised test (`[8]`, `[9]`, `[12]` in the TSV; indices are
  per-test, over the devices each test admits). Within `ZoomTest` and `MultiTouch2Test` the first
  timeout on a profile is followed by every later method on the same profile timing out, i.e. a
  stuck simulated touch point cascades through the class (each class runs in its own JVM,
  `reuseForks=false`, so cascades never cross classes).
- Runs 2 and 3 doubled the error count through extra flaky timeouts in the same two classes; the
  stable-pass and persistent sets did not move.
- None of the failing code paths is touched by the PR #12 fixes (touch pipeline, gesture
  recognisers, `MonocleUInput`), so the baseline is representative of HEAD as well.

**Gate for S1–S8:** rerun the command above once per slice; the 1 222 stable-pass rows must all
pass, the 42 persistent rows are the known baseline, the 58 flaky rows and the 50 skipped rows are
tracked but not gating. Classify with `s0-classify.pl` (`US-009-s0-classify.pl` in this folder) over the archived
`s0-run1..3` report directories plus the new run. A new persistent failure or a stable-pass row
turning red blocks the slice.
