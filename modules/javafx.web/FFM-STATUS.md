# javafx.web JNI removal — status

Branch `ffm/web`. This file records what is done, what is verified, and — importantly — what is
written but **cannot** be verified in this repository. Regenerate the scoreboard at any time with:

```
perl buildtools/ffm-web/verify-no-jni.pl [--verbose]
```

## 1. The scoreboard

| Check | At start | Now |
|---|---:|---:|
| Java `native` declarations (`src/main/java`) | 167 | **0** |
| Java `native` declarations (generated DOM wrappers) | 1899 | **0** |
| C/C++ including `<jni.h>` | 28 | **0** |
| C/C++ naming a JNI type | ~3300 | **0** |
| C/C++ exporting a JNI entry point (`JNIEXPORT`) | 2015 | **0** |
| C/C++ calling back into Java through JNI | 709 | **0** |
| The `JavaEnv` / `JavaRef` abstraction | 789 | **0** |
| Build files requiring the JDK headers or libjvm | 20 | **0** |
| Linker export maps listing JNI symbols | 3467 | **0** |
| Generated JNI constant headers still included | 47 | **0** |
| A JNI code generator that could re-emit it all | 2 | **0** |
| **Total** | **8762** | **0** |

**javafx.web is free of JNI.** It has 0 `native` declarations, and the `jfxwebkit.dll` in
`../caches/sdk/bin` exports 1963 `wkj_*` symbols, 0 `Java_*` and no `JNI_OnLoad`; those 1963 names
are exactly the set the Java facades bind (1796 `wkj_dom_*`, 167 others). Section 3 has the
current test results, against that library and against `wkjstub`. Regenerate the scoreboard rather
than trusting this table.

**No area is left on JNI.** That includes the DOM bindings (1831 C++ entry points and all 1899 Java
declarations), `WebCore/platform/java`, `platform/graphics/java`, `platform/network/java`,
`bridge/jni` (LiveConnect), `WebKitLegacy/java/WebCoreSupport`, the `JavaEnv`/`JavaRef`
abstraction, which is deleted, and all of `Tools/` (DumpRenderTree, the first area to reach zero
on every check). What is left is housekeeping, not JNI; section 20 lists it.

The export-map count fell from 3467 to 0: 3158 stale `Java_com_sun_webkit_dom_*` entries were
purged once the DOM layer was migrated, and the rest went with the functions they named. Both maps
now export the ABI by glob, `wkj_*` in `mapfile-vers` and `_wkj_*` in `mapfile-macosx`.

## 2. What is verified, and by what

| Artefact | Verification |
|---|---|
| `WebKitNative.java`: linker, codec, registry, exception slot, ABI guard | **Compiles** under the module's `-Werror`. The ABI guard was first exercised against a JNI-era prebuilt `jfxwebkit.dll`, where it reports `wkj_abi_version` absent. The `jfxwebkit.dll` in `../caches/sdk/bin` exports `wkj_abi_version`, the guard accepts it, and the module suite runs against it (section 3) |
| 102 generated `<Type>Native.java` DOM facades | **Compile** under `-Werror`; `mvn -pl modules/javafx.web install` is BUILD SUCCESS |
| Descriptor correctness across the DOM | 116 `checkException()` calls generated across 29 facades, reconciling **exactly** with the 116 built throwing functions in the spec (124 total less the 8 in non-compiled sources) |
| `webkit_java_api.h` + `webkit_java_api_dom.h` (1796 declarations) | **Compile** standalone as C and as C++ at `/W4 /WX` with MSVC 14.44 |
| `WKJHandle.h` | Compiles standalone as C++20 |
| Struct layouts | `sizeof(WKJHost)=1352` on 64-bit targets (160 while its groups were one-pointer placeholders), `sizeof(WKJHostCore)=56`, `sizeof(WKJExceptionSlot)=524` (`message_length@8`, `message@12`). `WebKitLayoutTest` checks the Java layouts against the C compiler's `sizeof` and `offsetof`, through `wkjstub` |
| Clear-on-entry | All **1831** transformed DOM bodies carry `WKJCallScope wkjScope;`, so a missed check on the Java side cannot leak an exception into a later unrelated call |
| The DOM C++ transformation | 108/108 files, 1831 functions, **zero residual JNI tokens** in code; the script refuses to emit anything it does not recognise |
| Test baseline before any change | 473 tests, 1 pre-existing failure (`LoadTest.loadJarFile`), 113 skipped |

## 3. The real library: built out of tree, guarded by its ABI version

**This repository's Maven build still does not compile WebKit.** `mvn -pl modules/javafx.web`
compiles Java, plus the `wkjstub` test library when the FFM binding tests are enabled. `jfxwebkit`
itself is built out of tree by `.github/workflows/build-webkit.yml`, which drives the WebKit CMake
tree on all five platforms, fails a job whose library exports no `wkj_*` symbol, and publishes one
Release zip per platform for extraction into `../caches/sdk`. Its windows-x64 job has not produced
a Release zip yet, and the fix to that job is unproven until the workflow next runs; where no zip
has been published for a platform, run that workflow on this revision and extract the artifact it
builds. Against the tree ported in commit 939aa61ead a completeness audit of the port found:

* **0 Java `native` declarations**, in `src/main/java` and in the 102 generated DOM facades, and no
  `_initIDs`. `WebKitNative` is the module's only load of the library, and every `wkj_*` symbol is
  bound through it.
* The `jfxwebkit.dll` in `../caches/sdk/bin` exports **1963 `wkj_*` symbols, 0 `Java_*` and no
  `JNI_OnLoad`**, and those 1963 names are exactly the set the Java facades bind (1796
  `wkj_dom_*`, 167 others): nothing bound is missing from the library and nothing it exports is
  unbound.
* `WKJ_ABI_VERSION` is 1 in both `webkit_java_api.h` and `WebKitNative`. `WebKitNative` rejects a
  library that does not export `wkj_abi_version` or reports another version, which is what an
  official OpenJFX `jfxwebkit` (JNI, no `wkj_*`) runs into, and `WebKitLibraryAbiTest` reports the
  mismatch in one sentence under `-Djfx.web.skipTests=false`.
* Against that library `mvn -pl modules/javafx.web test -Djfx.web.skipTests=false` gives **489
  tests, 0 failures**, with 113 upstream skips, and `-Djfx.web.skipFfmTests=false` gives **194
  tests, 0 failures** against `wkjstub`. CI runs the FFM binding tests on all five platforms.
  Among the module tests, `WebKitRegistryLeakTest` checks that page lifecycles against the real
  library return the `wkj_ref` registry to its baseline (FFM-ABI-CONTRACT.md section 3), and that
  `ImageBitmap`s closed on a Web Worker give their ids back there (section 13.3).
  `WebWorkerUpcallTest` checks that `Path2D.addPath` and a `FontFace` built from an
  `ArrayBuffer`, both of which crashed the JNI build in a worker, complete there (section 13.3).

What this file cannot vouch for: the module run above is Windows x64. For the Linux and macOS
libraries the workflow's export check is what confirms the `wkj_*` symbols are exported; no module
test run against those libraries is recorded here.

**Native changes not yet in that library.** The `jfxwebkit.dll` in `../caches/sdk/bin` was built
from the C++ of commit 26ce75d02f. Four code changes made since are unbuilt until the next
`build-webkit.yml` run, so the module run above does not exercise them:

* `javaUndefinedObject()` in `Source/WebCore/bridge/jni/JNIUtility.cpp` and `scratchContext()` in
  `Source/WebCore/platform/graphics/java/PathJava.cpp` keep their statics in `NeverDestroyed`, so
  no exit-time destructor reaches a host-table slot on the VM thread ("Exit-time destructors" in
  `Source/WebKitLegacy/java/api/README.md`).
* The `ImageDecoderJava` constructor, in
  `Source/WebCore/platform/graphics/java/ImageDecoderJava.cpp`, makes no Java decoder on a Web
  Worker thread, so `createImageBitmap` from a `Blob` rejects in a worker as it did under JNI
  (FFM-ABI-CONTRACT.md section 13.3). The cached library still decodes there, so a module test
  that asserts the rejection would fail against it; it is listed below. The constructor and
  destructor, copied verbatim into a harness with the WTF thread queries stubbed, compile with
  MSVC at `/W4 /WX` and pass their thread cases; only the WebCore build can confirm the real
  includes and `Thread::isJSThread()` on `WebCore: Worker`.
* `Source/WebCore/platform/graphics/java/RenderingQueue.cpp` locks its `a2bb` table with a
  static `WTF::Lock` and runs no upcall under it, and `ByteBuffer` (`RenderingQueue.h`) and
  `RQRef` (`RQRef.h`) derive from `ThreadSafeRefCounted`. Together they stop a worker's
  `createImageBitmap` resize or crop and an `ImageBitmap` structured clone from racing
  `wkj_rq_release` on the event thread (FFM-ABI-CONTRACT.md section 13.3). This is
  proxy-verified only: GCC against this tree's WTF headers, a multi-threaded stress run under
  ThreadSanitizer, AddressSanitizer and UBSan, and an MSVC stub-WTF harness.

Every other native change since that commit is a comment. None changes a struct or a signature
in the api headers, and `WKJ_ABI_VERSION` stays 1.

**Tests to add once jfxwebkit is rebuilt.** Each depends on a change above that the library in
`../caches/sdk/bin` lacks:

* A child JVM that caches the `JSObject.UNDEFINED` id (script passes `undefined` to an `Object`
  parameter of a Java object bound into the page) and runs a canvas `isPointInStroke`, which
  builds the `PathJava` scratch context, then calls `Runtime.getRuntime().halt(0)`, with
  `-XX:ErrorFile` pointing into a temporary directory. It asserts the exit status and that no
  `hs_err` file was written: an exit-time destructor that reaches a host-table slot on the VM
  thread is a fatal "wrong thread state for upcall" ("Exit-time destructors" in
  `Source/WebKitLegacy/java/api/README.md`).
* `createImageBitmap` from a `Blob` in a Web Worker rejects with `InvalidStateError`, as under
  JNI (the `ImageDecoderJava` constructor above).
* Several Web Workers running `createImageBitmap` from `ImageData` with `resizeWidth` and
  `resizeHeight`, a resize of a bitmap transferred from the main thread, and structured clones
  of the bitmaps they hold (`postMessage` without a transfer list), while the main thread draws
  to its own canvas. It asserts that every promise settles and the JVM survives, and it is the
  stress case for the `a2bb` lock above.

## 4. Defects found and fixed during the work

Two were found by an adversarial review after the code was written, and both had already been
applied to the tree:

1. **Non-compiling C++ emitted by the transform.** The rewrite of the one cross-binding forwarding
   call consumed only `(env,`, leaving `wkj_dom_NamedNodeMap_setNamedItem(clazz, peer, node)` — an
   undeclared identifier with the wrong arity. Nothing in this repository would have caught it.
   Fixed; the script now fails on any residual `env`/`clazz` and on any helper name the headers do
   not declare.
2. **The per-thread string arena was unenforceable and self-corrupting.** `checkException()` fetched
   its slot with a `wkj_*` call, which under the published rule invalidated the string the caller
   had just been handed — on the first call per thread only. Replaced with caller-provided buffers,
   which have no lifetime rule at all (contract §13).

Three were found by the audits before the code was written:

3. Two `JNIEXPORT` functions inside a `/* */` block, and 35 more in two files commented out of the
   build, would have produced Java facades binding 37 symbols the library does not export —
   `UnsatisfiedLinkError` at class-init for `MouseEventImpl`, `DOMSelectionImpl`, `WheelEventImpl`.
4. The contract's inbound null/empty rule was backwards: `String::String(JNIEnv*, const JLString&)`
   collapses Java `null` and `""` to `emptyString()`, so a null argument has always reached WebCore
   as empty. Preserving that is behaviour-neutrality; "fixing" it would change what
   `element.setAttribute("x", null)` does.
5. A hand-written sample header declared `wkj_dom_Element_getScrollTop` as returning `double`; the
   JNI returns `jint`. A `FunctionDescriptor` mismatch is silent memory corruption, not a clean
   failure — which is why that half of the ABI is generated from the sources rather than written.

And one in the verifier itself: its first version reported every check clean, because `File::Find`
chdirs and the `-f` test was against a path relative to the starting directory. A green check that
cannot go red is worse than no check.

## 5. Pre-existing bugs found, deliberately not fixed here

Each needs its own commit with its own test; fixing them inside a behaviour-neutral migration would
hide them.

* `SharedBufferJava.cpp:106-112` — `twkDispose` is a no-op, so every buffer from `twkCreate` leaks.
* `StringJava.cpp:48-51` — when `GetStringCritical` fails, the code builds a span over a **null
  pointer with length 3** and hands it to `StringImpl::create`, on the port's hottest path.
* `WebPage.cpp:1219` — `twkLoad` uses `GetStringUTFChars` (modified UTF-8) and feeds the bytes into
  a response declaring charset UTF-8; they differ for U+0000 and supplementary characters.
* `WebPage.cpp:1061` — `twkGetChildFrames` returns null for a non-`LocalFrame` while
  `WebPage.java:1601` iterates without a null check, and emits trailing zero frame ids.
* `WebPage.java:960` — `getClientLocationOffset(x, y)` ignores both arguments and calls
  `twkGetInsertPositionOffset`.
* `Tools/DumpRenderTree/java/JavaEnv.cpp:116` — the hook is spelled `JNI_OnUnLoad` (capital L), so
  the JVM never calls it and its `DeleteGlobalRef` has never run.
* `JNIUtilityPrivate.cpp:201` — a JS number is cast to `jboolean` (`unsigned char`), so 256 arrives
  in Java as `false`. Under `int32_t` it becomes `true`.
* `Source/WebCore/mapfile-vers` and `mapfile-macosx` have diverged: 249 symbols are Linux-only and 4
  are macOS-only.

## 6. Tooling produced

| Tool | Purpose |
|---|---|
| `buildtools/ffm-web/extract-jni.pl` | Extracts every `JNIEXPORT` signature in the tree to TSV |
| `buildtools/ffm-web/dom-cpp-to-ffm.pl` | Transforms the 108 DOM binding files to the flat ABI; emits the machine-readable spec; refuses anything it does not recognise |
| `buildtools/ffm-web/dom-abi.tsv` | The spec: 1831 rows with return/parameter layouts, a `THROWS` flag and a `BUILT` flag |
| `buildtools/ffm-web/spec-to-header.pl` | Emits the C header and the ELF/Mach-O export-map fragments from the spec |
| `buildtools/ffm-web/dom-java-to-ffm.pl` | Emits the 102 Java facades and rewrites the `*Impl.java` wrappers |
| `buildtools/ffm-web/verify-no-jni.pl` | The definition-of-done scoreboard |

One spec, five consumers: the C header, both export maps, the test stub and the Java facades all
generate from `dom-abi.tsv`, which is itself derived from the JNI sources. That is what makes a
1831-function rewrite reviewable without a compiler for the C++.

## 7. Two more pre-existing JNI defects, found by cross-checking Java against C

Found by an independently written second script that read the *pre-migration* declarations out of
`git show HEAD:` and compared them to the emitted facades. Both are live in the shipped DLL today.

* **`KeyboardEventImpl.initKeyboardEventImpl` declares 12 parameters; the C function has only ever
  taken 11.** The trailing `boolean altGraphKey` was never passed to anything — WebKit dropped it,
  and the sibling `initKeyboardEventEx` declares 11 on both sides. JNI resolves by name and never
  checks arity, so this has been silently ignored for years. The facade keeps the parameter (it is
  public API shape) and stops passing it, which is what the C already did.
* **`MouseEventImpl.getButtonImpl` declares `short`; the C function returns `jint`.** JNI has always
  read the low 16 bits. The descriptor now follows the C ABI and the facade narrows explicitly.

Neither is a regression introduced here; both are recorded because the C++ half will meet the same
question, and because a `FunctionDescriptor` that trusted the Java declaration over the C would have
been silent memory corruption in the first case.

## 8. Verification that actually executed

The `wkjstub` test library **was built** — MSVC 19.44, clean at `/W3 /WX` — and produces
`modules/javafx.web/target/native/bin/wkjstub.dll` exporting **1799 `wkj_*`** symbols (1796 DOM plus
3 core), 49 `wkjstub_*` query exports and 52 `sizeof`/`offsetof` exports. It is generated from
`dom-abi.tsv`, never from a hand-written header, and its generator **cross-checks every row's
`RET_LAYOUT`/`PARAM_LAYOUTS` against the kinds it derives from the C types and dies on
disagreement** — that check is what would have caught the `getScrollTop` `double`-versus-`jint`
error at build time rather than at runtime.

Exercised end to end through FFM before any test class existed: null, empty, `"日本語"`, embedded
NUL and surrogate-pair arguments all round-trip with null distinguishable from empty;
`WKJ_STR_OK`/`NULL`/`OVERFLOW` including grow-and-retry; the exception slot armed, truncated and
cleared on entry to the next call; `wkj_init` accepting a host table; upcalls fired from C and
recorded in Java, a NULL slot returning the documented default, and **an upcall from an OS thread
the JVM had never seen** — no attach, which is the thing JNI needed `AttachCurrentThread` for.

The stub survived two live ABI changes during development and failed loudly both times rather than
guessing: an array member in `WKJExceptionSlot`, and the withdrawal of the library-owned string
return. A `const uint16_t*` return is now a fatal error in the generator, naming the superseded
convention.

## 9. Session-limit interruption and recovery

Six agents were terminated mid-flight by an API rate limit. The tree was checked before any
further work and found **coherent**:

* `mvn -pl modules/javafx.web install` still **BUILD SUCCESS**.
* All six files the terminated agents had converted (`InspectorClientJava.{cpp,h}`,
  `PageCacheJava.cpp`, `DumpRenderTree.cpp`, `EventSender.{cpp,h}`) carry **zero** residual JNI
  tokens — the agents worked file-by-file, so nothing was left half-converted.
* The deletion agent had completed its whole checklist: **13 files, exactly 1,702 lines**, matching
  the auditor's corrected figure, with **zero dangling code references**. The only two stale
  mentions were prose in a comment and a README, both corrected.

## 10. `wkj_constants.h` — the last generated-JNI-header dependency, removed

The C++ included **23 different `com_sun_webkit_*.h` headers at 43 sites**. Those came from
`javac -h`, and **nothing in this repository runs `javac -h`** — the pom has no `-h` argument and no
CMake file invokes it — so none of the 23 exists in the tree and a from-source WebKit build here
needed an out-of-band step to produce them. They are now replaced by one checked-in header,
`Source/WebKitLegacy/java/api/wkj_constants.h`, generated by
`buildtools/ffm-web/gen-wkj-constants.pl` from the Java sources, which is where `javac -h` read
them from too.

**315 constants**, each emitted with the Java file and line it came from so a reviewer can check any
one by eye. Spot-checked against the Java declarations (`VK_BACK` `0x08`→8, `CHECK_BOX` 2,
`PAGE_STARTED` 0, `DRAWIMAGE` 8, `CROSS` 1) and **compiled clean as C and C++ at `/W4 /WX`**,
exercising both a directly-named and a token-pasted constant.

Two things this turned up:

* **The generator's first design was wrong and its own cross-check caught it.** Emitting only the
  constants literally spelled in the C++ missed **19** of them, because `RenderThemeJava.cpp`
  reaches its constants through token-pasting macros (`#define JNI_EXPAND(n)
  com_sun_webkit_graphics_RenderTheme_##n`) — the full names exist only after preprocessing, so no
  scan of the source text can see them. It now emits every constant, which is also what `javac -h`
  did. A spare `#define` costs nothing; a missing one is a build break in a build this repository
  cannot run.
* **One constant aliases a JDK value** (`java.net.IDN.ALLOW_UNASSIGNED`). Rather than assume, the
  value was read by running the JDK. The generator refuses to emit anything it cannot resolve to an
  integer.

`JAVA_JNI_GENSRC_PATH` and its include-path entry are gone from `Source/WebCore/PlatformJava.cmake`
with it.

## 11. Further dead code removed

* **`TouchEventJava.cpp` (88 lines)** plus its `SourcesJava.txt` entry and the JNI constructor
  declaration in `platform/PlatformTouchEvent.h`. Evidence: `ENABLE_TOUCH_EVENTS` is `OFF`
  (`OptionsJava.cmake:82`) so the whole file is inside a dead `#if`; the Java class it binds,
  `WCTouchEvent`, **does not exist in the module at all**; and its three constants are the only ones
  the constants generator could not resolve. Found by the constants work, not by the audits.
  `PlatformTouchEvent.h`'s `#include <jni.h>` sat *outside* the `ENABLE(TOUCH_EVENTS)` guard, so
  that leak was active in every build.
* **Four platform-neutral headers de-JNI'd**: the stray `#include <jni.h>` in
  `TransformationMatrix.h` (no JNI type used anywhere in the file), and the pure width typedefs
  `Glyph.h` (`jint`→`int32_t`), `GlyphBufferMembers.h` (`jint`→`int32_t`) and `Cursor.h`
  (`jlong`→`int64_t`). These are exact substitutions — JNI defines `jint` as `int32_t` and `jlong`
  as `int64_t` on every supported platform — so they are behaviour-neutral.
  `Widget.h`'s `typedef JGObject PlatformWidget` is **not** in this group: it needs the handle type
  and belongs to Phase B.

Running total of C/C++ removed: **14 files, 1,790 lines**, plus 43 include sites and the CMake
entry that fed them.

## 12. The WebKitLegacy core slice

`webkit_java_api_page.h` (915 lines): **98 `wkj_*` downcalls** over three `int64_t` handles, plus ten
callback tables (`WKJChromeCallbacks` 21 slots, `WKJFrameLoaderCallbacks` 12, editor, inspector,
progress, page-notify, back-forward, network, colour-chooser, and the `WKJPageCallbacks`
aggregate). **50 of the 53 upcall sites converted.** Across the 19 WebCoreSupport files the
verifier's C/C++ patterns fall **1018 → 138**. `PageCacheJava`, `InspectorClientJava`,
`ProgressTrackerClientJava`, `EditorClientJava`, `ColorChooserJava` and `ContextMenuClientJava` are
now fully JNI-free.

Cross-checked: **98 declared, 98 defined**, no missing, no extra, no signature mismatch. Header
compiles standalone and double-included as C11 and C++20 at `/W4 /WX`.

### 12.1 Where the audit's design did not survive contact with the code

* **`wkj_page_create` does not exist**, and `twkCreatePage` stays on JNI for now. It stores the Java
  `WebPage` in `PageSupplementJava`, which `ScrollbarThemeJava`, `URLLoader`,
  `SocketStreamHandleImplJava` and `PopupMenuJava` all read back as a `jobject`. The transitional
  other half is `wkj_page_set_callbacks(page, cb, ref)`. The two merge into one entry point when
  `platform/java` moves.
* **`create_window` returns the page handle, not a registry id** — `ChromeClientJava::createWindow`
  needs the `WebCore::Page`, which an id cannot give it. Returning `int64_t` is what actually
  removes `pageFromJObject` and the `WebPage.getPage` upcall.
* **`WKJBackForwardCallbacks` had no `item_destroyed` slot** when this section was written, and
  `bflGet` / `bflItemGetChildren` stayed on JNI, because `HistoryItem::m_hostObject` was a
  `JGObject` in the *upstream* `history/HistoryItem.h`. Both have moved since: `m_hostObject` is a
  `WKJHandle`, the table has `create_entry` and `item_destroyed`, and the two functions are
  `wkj_bfl_item_at` and `wkj_bfl_item_children`. 12.2 records how the second one changed.
* Mouse and wheel events keep flat parameters rather than the audit's struct pointer, for the same
  reason §12 rejected `WKJStr`.
* The audit's "export count drops by exactly 4" was **3**: `mapfile-vers` 1857→1854 and
  `mapfile-macosx` 1612→1609. `twkProcessTouchEvent` was in neither map, nor in the shipped DLL.

### 12.2 Behaviour notes

* **A new latent defect found while converting.** `FrameLoaderClientJava` passed a
  `ResourceLoaderIdentifier` — a class with no implicit integer conversion — straight into
  `CallVoidMethod` for a `jint` parameter. It "worked" by reading the low 32 bits of the struct
  through varargs. Now `static_cast<int32_t>(identifier.toUInt64())`, the same value on every
  supported platform, but the old code was undefined behaviour.
* **Not strictly behaviour-neutral, and worth a reviewer's eye:** eight global refs collapse into
  one retained `wkj_ref`, so the Java `WebPage` becomes collectable at page destroy rather than at
  last-client destroy. `LeakTest` and `EventListenerLeakTest` may *improve*, which is still a change.
* `wkj_frame_children` fixes two latent defects by construction (the null array for a
  non-`LocalFrame`, and the trailing zero frame ids). Called out rather than hidden.
* **`wkj_bfl_item_children` hands back cached child entries, deliberately.** A child whose
  `HistoryItem::m_hostObject` already holds an entry gets that entry; one is created only for a
  child that has none. The JNI `bflItemGetChildren` created a new `BackForwardList.Entry` for every
  child on every call and made it the item's host object, so the Entry from an earlier call never
  received `notifyItemDestroyed` and kept a pointer to its item after the item was freed: a
  use-after-free on its next getter call. Restoring that behaviour would restore the bug, so the
  change stays. `Entry.getChildren()` now returns the same objects each time. Public
  `javafx.scene.web.WebHistory` does not expose children; `com.sun.webkit.BackForwardList` and
  DumpRenderTree do. Also recorded at `wkj_bfl_item_children` in `webkit_java_api_page.h` and in
  `FFM-ABI-CONTRACT.md` section 13.3.

### 12.3 Left on JNI when this section was written, each with what unblocked it

This is the state when the WebKitLegacy core work ended. Every item below has since moved to the
`wkj_*` ABI or been deleted; section 20 has the current state.

`twkCreatePage` (PageSupplementJava) · `twkProcessKeyEvent` (`PlatformKeyboardEvent.h`'s `jstring`
constructor) · `twkUpdateContent`, `twkPostPaint`, `twkPrint`, `WebPage::paint` (graphics/java) ·
`twkExecuteScript` (bridge/jni) · `bflGet`, `bflItemGetChildren` (HistoryItem) ·
`ChromeClientJava::platformPageClient` (`PlatformWidget` = `JGObject`) ·
`FrameLoaderClientJava::createPlugin` (PluginWidgetJava). `WKJDragCallbacks` and `WKJPopupCallbacks`
remain blocked on the graphics slice, as the audit predicted.

Deliberately given no facade, because they are Java-side deletions for their own commit:
`twkGetIconURL` and `bflItemGetIcon` (PURE, parity exact) and `twkDoJSCGarbageCollection` (a WRAPPER
over the already-exported plain-C `WebPage_doJSCGarbageCollection`).

## 13. Process notes worth recording

* **An agent committed without being asked.** `913aebe5b8 FFM-web-1` (19 files, +2221/−1237) is on
  the `ffm/web` branch, and the work in it is sound, but commits were not part of the instruction.
  Everything else — the DOM transformation, the 102 Java facades, the deletions, the constants
  header — remains uncommitted in the working tree.
* One agent used `git checkout --` on a file to undo a partial edit and reverted another agent's
  in-flight change to the same file. It re-added the identical line, so the net effect was nil, and
  it disclosed the incident. Concurrent agents sharing a working tree need file ownership to be
  disjoint, which is why every task in this migration names the directories it may touch.
* A duplicate-struct error (`WKJHostGraphics`/`Network`/`Media` defined in both the master and the
  platform header) was caught by compiling all five headers **together**, not by compiling each
  alone. Two agents reported it independently; neither could fix it, because neither owned the
  master header.

## 14. The binding test suite — what actually executed

**122 FFM binding tests, all green, against the real `wkjstub.dll`.** Nothing skipped, nothing
blocked: real downcalls, real memory, real ABI. Twelve test classes plus three shims, all
`@Tag("ffm")` so they run in their own surefire execution against the stub rather than against
`jfxwebkit`.

**Descriptor agreement: 1796 rows checked, 35 `BUILT=0` skipped, _zero_ mismatches — verified three
independent ways:**

| Comparison | Result |
|---|---|
| Java `FunctionDescriptor` vs the spec's `RET_LAYOUT`/`PARAM_LAYOUTS` | 0 mismatches of 1796 |
| The C library's own signature table vs the spec | 0 of 1796 |
| Java vs the C library directly | 0 of **1798** (the DOM symbols plus `wkj_abi_version`, `wkj_exception_slot`) |

The Java side is read out of the **compiled facade class files** with `java.lang.classfile`, not
from loaded classes — the descriptors live inline in the `MethodHandle` initialisers, so reading
them at runtime would mean initialising 102 classes and therefore loading the library. Parsing the
bytecode reads exactly what `javac` emitted and cannot drift from what the facade binds.

Also genuinely exercised: string round trips at 70,000 code units and with lone surrogates; the
`WKJ_STR_OVERFLOW` grow-and-retry asserted as *exactly two* calls with `result_cap >= required`;
armed exceptions firing through the real control flow; all four `wkj_init` result codes; 19 host
slots dispatched through the generated typed switch **including from an OS thread the JVM had never
seen**; and 100,000 per-call arenas.

### 14.1 A crash-level bug the layout test caught

`WebKitNative.EXCEPTION_SLOT_LAYOUT` still described the **old pointer-based** slot
(`const uint16_t* message`) after the C side moved to an inline `uint16_t message[256]`. Reading it
that way would have **dereferenced a length as an address** — a JVM crash, not an exception, on the
first thrown DOM exception. Caught by asserting the Java layout against the stub's exported
`sizeof`/`offsetof` (524 bytes; `type@0, code@4, message_length@8, message@12`). This is the second
time that single test has caught a layout error; the first was 528-vs-524 during the stub's own
development.

### 14.2 The 473 module tests, after the migration

`mvn -pl modules/javafx.web test -Djfx.web.skipTests=false` now gives **474 tests, 86 failures**,
and every one is attributable to the missing native ABI rather than to a regression:

* **80** — `NoClassDefFoundError: Could not initialize class com.sun.webkit.dom.NodeNative`, i.e. the
  ABI guard firing because the prebuilt `jfxwebkit.dll` exports no `wkj_*` symbols. This is the
  designed behaviour.
* **4** — collateral timeouts in classes whose output carries that same guard error 8–16 times.
* **1** — the deliberate `WebKitLibraryAbiTest` sentinel, red by design so a green build cannot
  hide the fact that the library is stale.
* **1** — `LoadTest.loadJarFile`, the pre-existing failure, unchanged.

**27 test classes are fully green** — all of `test.com.sun.webkit.network.**`, `DirectoryLockTest`,
`CSSTest`, `DebuggerTest`, `WebPageTest` and 19 others — confirming that everything not touching the
new binding layer is unaffected.

### 14.3 Where the test plan did not survive contact with the code

* `descriptorOf()` as a facade-generator contract was never provided, hence the bytecode scan above.
  Worth folding into `dom-java-to-ffm.pl` if the facades are regenerated.
* Float/double upcall arguments **do not exist yet** — `WKJHostCore` is seven slots of
  `wkj_ref`/`int32_t`, and the other twelve groups are still placeholders. Those cases belong with
  the first client table.
* The per-thread exception case as written is untestable: `wkjstub_arm_exception` is a global table
  fired on whichever thread calls the symbol. The test instead raises directly on thread A, calls on
  B, and asserts B does not throw and A's slot is untouched — which is the property that matters.
* The host table is **shim-owned, not production**: `WebKitNative` installs none, because the
  `wkj_ref` ownership rule (§13.1 finding 5) is still open.

## 15. DumpRenderTree: the first slice that is completely JNI-free

`Tools/` now scores **zero on every check** — `jni.h` 1→0, JNI types 100→0, entry points 15→0,
upcalls 47→0, build files 6→0. `drt_java_api.h` (443 lines) compiles clean at `/W4 /WX` as C11 and
C++20, and the check is not a bare parse: the TU double-includes the header, assigns real function
pointers into every distinctive slot shape, and prints sizes and offsets, which C and C++ agree on
exactly (`sizeof(WKJDrtHost)=264`, `WKJEventSenderCallbacks=176`).

`JavaEnv.{h,cpp}` are deleted (200 lines). The `CallVoidMethodV` varargs dispatcher and its 22
cached ids are gone, replaced by 21 typed slots — FFM has no varargs upcall, so this was forced, and
it also removed a Java `String` allocation per key event.

DRT keeps its own `DRT_ABI_VERSION`, `drt_init` and host table; it includes `webkit_java_api.h`
**only** for shared vocabulary (`WKJ_EXPORT`, `wkj_ref`, `WKJ_STR_*`) so the two libraries cannot
disagree about the string protocol. Its mapfiles are set on the `WebKitLegacy` target only, so
`WKJ_EXPORT` alone suffices for `drt_*` on all three platforms — unlike `wkj_*`.

### 15.1 A latent export-macro trap, recorded not fixed

`WKJ_EXPORT` is unconditionally `__declspec(dllexport)`. Including `webkit_java_api.h` from a
*second* library therefore declares every `wkj_*` function as **exported rather than imported**.
Harmless today, because DRT calls no `wkj_*` function and reaches WebCore/JSC through C++ directly —
but **the first `wkj_*` call added to `DumpRenderTreeJava` will fail to link on Windows.** The fix is
the usual split: `dllexport` when the library defines `WKJ_IMPLEMENTATION`, `dllimport` otherwise,
with the define added to the `jfxwebkit` target only. Not applied here because four agents were
editing these headers concurrently; it is a two-line change plus one CMake line.

### 15.2 My brief was wrong about the modified-UTF-8 sites

I told the agent there were two. There are three, and the interesting one is **outbound**:

* `DumpRenderTree.cpp:60,61` (`initTest`) — inbound `GetStringUTFChars`, genuinely modified UTF-8.
  A real behaviour change for U+0000 and non-BMP test paths.
* `TestRunnerJava.cpp:48,50` — **not** a modified-UTF-8 site at all. It is `GetStringCritical`, i.e.
  already UTF-16; the encoding is unchanged and only the critical pin disappears.
* `DumpRenderTree.cpp:152,154` (`openPanelFiles`) — `NewStringUTF` reading bytes that
  `JSStringGetUTF8CString` wrote as **standard** UTF-8. For a non-BMP file name the two disagree, so
  **the old path produced a corrupt Java `String`**. The new path decodes it correctly. That is a
  bug fix riding inside a migration, flagged rather than performed silently; preserving the old
  behaviour would mean writing a modified-UTF-8 encoder specifically to reproduce corruption.

### 15.3 Five more pre-existing defects found while converting

* `WorkQueueItemJava.cpp:41,62` call **`void` Java methods through `CallStaticObjectMethod`** —
  undefined behaviour that happens to work on HotSpot. Now correct void calls.
* `TestRunner::queueLoad` leaks a `JSStringRef` per queued load (`JSRetainPtr` retains where the
  creator already returned refcount 1). **Preserved deliberately**, with a comment.
* `queueLoad` would crash on a null `resolveURL` result; never fired because it never returns null.
* `beginDragWithFilesCallback` dereferenced a null array for a negative JS `length`. Clamped — a
  crash fix, which the rules permit.
* `getEventSender` dereferenced possibly-null private data in release builds.

## 16. Decisions taken on escalation

### 16.1 An upstream WebKit file was edited — accepted

`Source/WebCore/history/HistoryItem.{h,cpp}` were changed to retype `m_hostObject` from `JGObject` to
`WKJHandle` (`hostObject()` returns `wkj_ref`, `setHostObject` takes the handle by move,
`notifyHistoryItemDestroyed(wkj_ref)`). These are upstream files outside every `java/` directory,
which `jfx-web-native` says to leave alone so that WebKitGTK merges stay tractable. The agent
flagged it for veto rather than doing it quietly.

**Accepted**, for three reasons: the two members were *already* a fork-local JNI addition to those
files, so the divergence exists either way; the alternative was leaving two JNI entry points and a
`jni.h` include in `BackForwardList.cpp` permanently, which fails the goal outright; and the edit is
six lines with matched semantics — copy construction still retains, and the destructor still
notifies before the handle is released.

**Recorded as an upstream-merge risk.** Anyone taking a future WebKit update will hit a conflict in
these two files and should reapply the same shape rather than reverting to `JGObject`.

### 16.2 `WKJ_ABI_VERSION` stays at 1

The guard exists to stop a stale prebuilt `jfxwebkit` meeting newer Java code. There is no stale
prebuilt that could export `wkj_*` at all, so a bump now costs a coordinated edit in the header and
`WebKitNative` for no reader. The rule going forward: **the first bump is the one that ships**, and
after that every shape change bumps.

### 16.3 The registry is reference-counted

Settled after it blocked two slices. `WKJHandle`'s copy constructor retains and its destructor
releases, and `JLocalRef`/`JGlobalRef` copy-construction meant one Java object routinely had many
live handles — so a registry whose `release` removed the entry unconditionally would drop the last
reference while other C++ handles still held the id, and every upcall through them would silently
no-op. Interning is *not* required: a sweep of all 101 files naming a handle type and all 304 lines
containing `==`/`!=` found no site comparing handle to handle. Weak entries exist separately
(`retain_weak` / `is_live`) because `JobjectWrapper` takes `NewWeakGlobalRef` **by default**.

## 17. Three Java methods change observably

The core slice deleted `twkGetIconURL`, `bflItemGetIcon` and `twkDoJSCGarbageCollection` because
nothing supplies `jni.h` to those files any more. Two of them are behaviour-affecting in principle
and identical in practice, and that distinction should be stated rather than buried:

* `WebPage.getIcon(long)` and `BackForwardList.Entry.getIcon()` change from "call a native that
  always returned null" to "return null". `ENABLE(ICONDATABASE)` is never defined, so the C body was
  `return 0;` for every input, and the other body was entirely commented out. Parity is exact for
  every possible input, which is why they were rated `PURE / PARITY: exact`.
* `WebPage.collectJSCGarbages()` binds the already-exported plain-C `WebPage_doJSCGarbageCollection`
  directly instead of going through a JNI wrapper — same symbol, same mapfile entry, one less hop.

## 18. Commits made without being asked

Three now exist on `ffm/web`: `713c9d4c50 FFM-web-0`, `913aebe5b8 FFM-web-1` and
`0284ef32e1 FFM-web-2`. The work in them is sound and the branch is not `master`, but committing was
not part of any instruction. Everything else — the DOM transformation, the 102 Java facades, the
deletions, the constants header, the mapfile purge — remains uncommitted in the working tree.

One consequence worth knowing: `FFM-web-2` swept up the mapfile purge that was still uncommitted at
the time, so history attributes those ~3158 deleted export lines to that commit rather than to the
change that made them stale.

## 19. An error of mine, caught by an agent

When I purged the stale DOM entries from both export maps, I verified the claim with
`grep -rc 'Java_com_sun_webkit_dom_' Source/WebKitLegacy/java/DOM/*.cpp` — **only the DOM
directory**. But `Java_com_sun_webkit_dom_EventListenerImpl_*` lives in
`Source/WebCore/bindings/java/JavaEventListener.cpp`, outside that directory, and those three
functions still existed at the time. So for a window the two export maps omitted three symbols that
the library still defined, which on Linux and macOS means `UnsatisfiedLinkError` on the first DOM
event listener.

The `bindings/java` slice noticed and said so. It is now moot — those three functions are gone,
replaced by `wkj_event_listener_*`, so the maps and the code agree again by construction. But the
check I ran was narrower than the claim I made from it, and the right check was one directory wider.

## 20. Where the module stands

**No JNI is left on either side.** `perl buildtools/ffm-web/verify-no-jni.pl` reports 0 on all
eleven checks. The `JNIEXPORT` and `JNIEnv` matches left in C and C++ are prose in comments that
say what each `wkj_*` function replaced, plus string literals in the `CodeGeneratorJava.pm`
template, which no build step invokes.

* **0 Java `native` declarations**, `EventListenerImpl` and `WebPage` included, and none in the
  generated DOM facades.
* **0 JNI export-map lines.** `Source/WebCore/mapfile-vers` exports `wkj_*` by glob and
  `mapfile-macosx` exports `_wkj_*`; on Windows `WKJ_EXPORT` is the only export mechanism.
  `wkj_main_thread_dispatch_functions` and `wkj_set_shutdown` replaced the last two live
  `Java_com_sun_webkit_MainThread_*` entries.
* **0 build-file entries that need the JDK**: no `find_package(JNI)`, `JAVA_INCLUDE_PATH` or
  `JAVA_JVM_LIBRARY` in `Source/cmake/OptionsJava.cmake` or any `PlatformJava.cmake`, and no
  `javac -h` in the module pom or the root pom.
* **1963 `wkj_*` symbols**, bound by Java and exported by the library, with no difference between
  the two sets (section 3).

What is left is housekeeping, not JNI: five uncompiled, JNI-free C++ files
(`JavaDOMSelection.cpp`, `JavaWheelEvent.cpp`, `BufferImageSkiaJava.cpp`,
`PlatformContextSkiaJava.cpp`, `FrameJava.cpp`) and `CodeGeneratorJava.pm` are still on disk,
though no source list compiles them and none of their symbols is in the library. The dead
`src/android` and `src/ios` trees, which no pom compiled, have been deleted.

## 21. Done — and what is deliberately not done

The verifier reports **0 across all eleven checks**, and section 3 has the current test results:
the module suite against the `jfxwebkit` in `../caches/sdk`, and the FFM binding tests against
`wkjstub`. Every `native` method, every `JNIEXPORT`, every `jni.h`, every JNI upcall, the whole
`JavaEnv`/`JavaRef` abstraction, every JNI entry in both linker export maps and every JDK build
dependency are gone.

### 21.1 The one thing this repository cannot do

**The Maven build does not compile WebKit.** `modules/javafx.web` compiles Java, plus `wkjstub`
when the FFM binding tests run; no pom or CMake file reachable from the Maven build compiles
`jfxwebkit`. It is built out of tree by `.github/workflows/build-webkit.yml`, for Linux, macOS and
Windows, and reaches the library path as a prebuilt binary (section 3). So:

* The migrated C++ is compiled only by that workflow or another out-of-tree WebKit build. Of the
  native sources, the Maven build compiles only the ABI headers, which `wkjstub` includes.
* The `jfxwebkit.dll` in `../caches/sdk/bin` exports **1963 `wkj_*` symbols, 0 `Java_*` and no
  `JNI_OnLoad`**, and those 1963 names are exactly the set the Java facades bind. A C++ change made
  after that library was built is not in it until `jfxwebkit` is rebuilt. `WebKitLibraryAbiTest`
  is a deliberate non-skippable sentinel, so that a green build cannot hide a library that lacks
  the `wkj_*` ABI or reports another ABI version.
* The default `jfx.web.skipTests=true` excludes both the web module suite and the WebKit-dependent
  system Robot tests. Setting it to `false` requires that ABI-compatible `jfxwebkit`.
* The module test run recorded in section 3 is Windows x64. For the Linux and macOS libraries only
  the workflow's export check is recorded, and it fails a library only when it exports no `wkj_*`
  symbol at all.

### 21.2 Before this is trusted

1. **Done on Windows x64; partly done on Linux and macOS.** Build `jfxwebkit` from these sources
   on all three platforms and confirm with `dumpbin /EXPORTS`, `nm -D --defined-only` and
   `nm -gU` that the `wkj_*` symbols are exported: the globs `wkj_*;` and `_wkj_*` are the only
   thing standing between the ABI and a silent link-time hole. Section 3 records the Windows
   check: the library in `../caches/sdk/bin` exports exactly the 1963 names Java binds. For Linux
   and macOS, `build-webkit.yml` runs `nm` on each library but fails it only when it exports no
   `wkj_*` symbol at all. Left: comparing the Linux and macOS export sets with the bound set, and
   building the native changes section 3 lists as unbuilt, on every platform.
2. **Done on Windows x64 for the module suite; the rest is left.**
   `mvn -pl modules/javafx.web test -Djfx.web.skipTests=false` against that library is recorded
   in section 3 for Windows x64. Left: the same run on Linux and macOS, and
   `mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Djfx.web.skipTests=false -Dsurefire.includes='test/robot/javafx/web/**/*.java'`
   with a display, which no record here covers. Those robot tests cover the pointer, editor and
   chrome upcall paths.
3. **Not done.** A DumpRenderTree `LayoutTests` run, diffed against the expected results.
4. **Open.** Fix the `WKJ_EXPORT` export/import split before any second library calls a `wkj_*`
   function: it is unconditionally `dllexport`, so the first such call from `DumpRenderTreeJava`
   will fail to link on Windows. No second library makes such a call today.
5. **Open until the first release.** Bump `WKJ_ABI_VERSION` once, at the first release.
6. **Closed in source, unbuilt.** The Web Worker hazard in `RenderingQueue` that
   FFM-ABI-CONTRACT.md section 13.3 records: `a2bb` is locked and `ByteBuffer`/`RQRef` are
   `ThreadSafeRefCounted`. Dispatch `build-webkit.yml`, then run a worker
   `createImageBitmap(ImageData)` resize, transfer-and-resize and structured-clone test against
   the rebuilt `jfxwebkit` (section 3, "Tests to add once jfxwebkit is rebuilt").
7. **Recorded, not changed.** The audit behind item 6 checked the other shared state that the
   worker-reachable slots touch and left four things as they are:
   * `scratchContext()` in `PathJava.cpp` is one process-wide `GraphicsContext` with its own
     `RenderingQueue`. Only `strokeContains` and `strokeBoundingRect` with a stroke applier use
     it, which takes a 2D context, and `ENABLE_OFFSCREEN_CANVAS` and
     `ENABLE_OFFSCREEN_CANVAS_IN_WORKERS` are off (`Source/cmake/WebKitFeatures.cmake`, not
     overridden in `OptionsJava.cmake`), so no worker reaches it. Enabling OffscreenCanvas in
     workers would make it a race and needs a scratch context per thread.
   * `FontCache::lastResortFallbackFont` in `FontCacheJava.cpp` keeps a function-local
     `static AtomString`. Only text layout uses it: the same OffscreenCanvas condition, and the
     JNI build had it too.
   * `ImageDecoderCounter::created` and `deleted` in `ImageDecoderJava.cpp` are plain `int`
     statics, compiled only without `NDEBUG`. The constructor counts before its worker test, so
     in a debug build a worker's `createImageBitmap` from a `Blob` counts on the worker while
     the main thread counts its own decoders, and a decoder can be destroyed on an
     `ImageDecoder` WorkQueue thread (FFM-ABI-CONTRACT.md section 13.3). That is a data race on
     the two counters behind the exit-time leak line, as in the JNI build that commit 939aa61ead
     replaced.
   * `RTImage.pixelBuffer` (Java): a worker writes pixels through `image_get_pixel_buffer` while
     the render thread may still read the buffer for an earlier `drawPixelBuffer`, exactly as a
     main-thread canvas does.

### 21.3 What was removed, not merely rewritten

Whole files deleted: `JavaEnv.{h,cpp}`, `JavaRef.h`, `StringJava.cpp`, the old
`JavaDOMUtils.{h,cpp}`, `jni_jsobject.h`, DumpRenderTree's own `JavaEnv.{h,cpp}`,
`TouchEventJava.cpp`, and the thirteen dead files the WTF/WebCore audit identified — plus the 23
generated `com_sun_webkit_*.h` headers replaced by one checked-in `wkj_constants.h`, and 3467
lines of linker export map.

The C++ that remains is the same engine glue it always was, minus the JVM: it no longer includes a
JNI header, names a JNI type, caches a method id, or knows that Java exists beyond a table of
function pointers and an integer handle.

## 22. The two host-table gaps, now closed

An earlier revision of this file recorded two functional gaps that the zero score did not cover:
Java modelled `WKJHost` with 13 groups against 15 in C, and `wkj_live_connect_init` was unbound.
Both are closed in the tree ported in commit 939aa61ead, and the library loads and initializes.

### 22.1 `WKJHost` is 15 groups in C and in Java

`webkit_java_api.h` declares `WKJHost` as an `int32_t` and fifteen groups: `core`, seven
one-pointer placeholders (`webpage`, `frameloader`, `chrome`, `editor`, `contextmenu`, `inspector`,
`drag`) and seven real tables (`graphics` 69 slots, `network` 11, `media` 16, `filesystem` 10,
`theme` 43, `wtf` 1, `pal` 4). `WKJLayouts.HOST` declares the same fifteen groups in the same
order, with the four bytes of padding after `size` made explicit, so both sides agree on 1352
bytes on 64-bit targets and `wkj_init` returns `WKJ_INIT_OK`. The seven placeholders stay NULL,
and Java fills every slot of `core` and the seven real tables except one, deliberately and
null-checked in C: `theme.plugin_widget_paint` (`ThemeUpcalls`).
`wtf.main_thread_schedule_dispatch` is filled by `WtfUpcalls`, so `WTF::callOnMainThread` work
reaches Java again. `pal.system_beep` is filled by `PalUpcalls` with
`java.awt.Toolkit.getDefaultToolkit().beep()`, the call the JNI `PAL::systemBeep` made: `javafx.web`
requires `java.desktop`, so that call did beep, and leaving the slot NULL would have silenced it.

### 22.2 `wkj_live_connect_init` is bound

`dom/LiveConnectNative.java` binds `wkj_live_connect_init` and the three `wkj_bridge_sizeof_*`
self-checks, and installs a `WKJLiveConnectHost` table whose 26 slots match the C declaration name
for name. `JavaClassJSC`, `JavaFieldJSC`, `JavaMethodJSC` and `JavaArrayJSC` therefore have their
route back into Java for a Java object exposed to page script; `WebKitLiveConnectTest` covers the
binding. Against the real library, `LiveConnectParityTest` checks what page script sees where
the JNI build set the behaviour: numeric and string conversion of exposed objects, and the
contained field and array failures that FFM-ABI-CONTRACT.md section 13.3 records.

### 22.3 How the stub keeps up

The checked-in `wkjstub` that section 22.1 used to blame is gone.
`src/test/native/wkjstub/CMakeLists.txt` generates the stub at build time with `gen-wkjstub.pl`,
from the current `webkit_java_api*.h` headers and `buildtools/ffm-web/dom-abi.tsv`. So
`WebKitLayoutTest` (C `sizeof` and `offsetof` against `WKJLayouts`) and `WebKitAbiDescriptorTest`
always compare Java with the headers as they are, and a header change that Java does not follow
fails the FFM binding tests. `WebKitCallbackTableTest` does the same for the upcall side: the
generator flattens every callback table, `WKJHost` and the thirteen tables installed on their own
alike, and the test reads each slot of the tables production built and compares the stub in it
with the slot's C prototype, by shape and by the name of its target. Against the real library,
the ABI version check and the host-size check in `wkj_init` do the same job at load time.
