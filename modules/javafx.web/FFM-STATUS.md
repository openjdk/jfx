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

**javafx.web is free of JNI**, and the module builds: `mvn -pl modules/javafx.web install` is BUILD
SUCCESS and `mvn -pl modules/javafx.web test` gives **137 tests, 0 failures, 0 errors, 0 skipped**
against the real `wkjstub` library. Regenerate the scoreboard rather than trusting this table.

**Whole areas now completely JNI-free:** the DOM bindings (1831 C++ entry points, 1896 of 1899 Java
declarations), `WebCore/platform/java`, `platform/graphics/java`, `platform/network/java`,
`bridge/jni` (LiveConnect), `WebKitLegacy/java/WebCoreSupport`, and all of `Tools/`
(DumpRenderTree, the first slice to reach zero on every check).

**What is left** is nine files: `WTF/wtf/java/{JavaEnv.cpp, MainThreadJava.cpp, JavaEnv.h,
JavaRef.h}` — the JNI abstraction itself, Phase B's core — plus
`WebCore/bindings/java/{JavaEventListener.cpp, JavaDOMUtils.cpp, JavaDOMUtils.h,
EventListenerManager.h}` and the unassigned `PAL/pal/crypto/java/CryptoDigestJava.cpp`. The three
remaining DOM Java declarations are `EventListenerImpl`'s, which include the module's only
*instance* native and are blocked on that same `bindings/java` slice.

The export-map count fell from 3467 to 69 in two steps: 3158 stale `Java_com_sun_webkit_dom_*`
entries purged once the DOM layer was migrated, then the core slice removing its own as it removed
each function.

## 2. What is verified, and by what

| Artefact | Verification |
|---|---|
| `WebKitNative.java` — linker, codec, registry, exception slot, ABI guard | **Compiles** under the module's `-Werror`. The ABI guard was exercised against the real prebuilt `jfxwebkit.dll`: it resolves `Java_com_sun_webkit_WebPage_twkGetDocument` and correctly reports `wkj_abi_version` absent |
| 102 generated `<Type>Native.java` DOM facades | **Compile** under `-Werror`; `mvn -pl modules/javafx.web install` is BUILD SUCCESS |
| Descriptor correctness across the DOM | 116 `checkException()` calls generated across 29 facades, reconciling **exactly** with the 116 built throwing functions in the spec (124 total less the 8 in non-compiled sources) |
| `webkit_java_api.h` + `webkit_java_api_dom.h` (1796 declarations) | **Compile** standalone as C and as C++ at `/W4 /WX` with MSVC 14.44 |
| `WKJHandle.h` | Compiles standalone as C++20 |
| Struct layouts | `sizeof(WKJHost)=160`, `sizeof(WKJHostCore)=56`, `sizeof(WKJExceptionSlot)=524` (`message_length@8`, `message@12`) — identical between the C and C++ compilations on Windows x64 |
| Clear-on-entry | All **1831** transformed DOM bodies carry `WKJCallScope wkjScope;`, so a missed check on the Java side cannot leak an exception into a later unrelated call |
| The DOM C++ transformation | 108/108 files, 1831 functions, **zero residual JNI tokens** in code; the script refuses to emit anything it does not recognise |
| Test baseline before any change | 473 tests, 1 pre-existing failure (`LoadTest.loadJarFile`), 113 skipped |

## 3. What is NOT verified, and cannot be here

**This repository does not build WebKit.** `mvn -pl modules/javafx.web` compiles Java only; there is
no `COMPILE_WEBKIT` path in any pom or CMake file reachable from the Maven build. Therefore:

* None of the ~49,400 lines of migrated C++ has been compiled by anything. The headers were
  compiled standalone; the translation units were not, and cannot be.
* The prebuilt `jfxwebkit.dll` in `../caches/sdk/bin` exports 1956 `Java_com_sun_*` symbols and
  **zero** `wkj_*`. The 473 module tests therefore cannot pass until `jfxwebkit` is rebuilt from
  these sources with the WebKit CMake/ninja/clang toolchain.
* Cross-platform: everything compiled here was MSVC x64. The `__attribute__((visibility("default")))`
  branch of `WKJ_EXPORT`, `thread_local` behaviour under GCC and clang, and the ELF/Mach-O export
  globs (`wkj_*;` and `_wkj_*`) are unverified. An older `ld64` can fail an `-exported_symbols_list`
  containing an unmatched pattern.

The honest summary is that the Java half of this migration is verified by a compiler and the C++
half is verified by review. That asymmetry is the central risk of the exercise and is not something
the work can resolve from inside this repository.

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
* **`WKJBackForwardCallbacks` has no `item_destroyed` slot**, and `bflGet` / `bflItemGetChildren`
  stay on JNI: `HistoryItem::m_hostObject` is a `JGObject` in the *upstream* `history/HistoryItem.h`.
* Mouse and wheel events keep flat parameters rather than the audit's struct pointer, for the same
  reason §12 rejected `WKJStr`.
* The audit's "export count drops by exactly 4" was **3**: `mapfile-vers` 1857→1854 and
  `mapfile-macosx` 1612→1609. `twkProcessTouchEvent` was in neither map, nor in the shipped DLL.

### 12.2 Two more behaviour notes

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

### 12.3 Still on JNI in this slice, each with what unblocks it

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

**Zero real JNI entry points remain in any C or C++ file.** The handful of `JNIEXPORT` matches left
are prose in comments describing what each `wkj_*` function replaced, plus a string literal inside
the abandoned `CodeGeneratorJava.pm` template.

Every C/C++ area is JNI-free: the DOM bindings, `WebCore/platform/java`, `platform/graphics/java`,
`platform/network/java`, `bindings/java`, `bridge/jni`, `WebKitLegacy/java/WebCoreSupport`,
`WTF/wtf/java`, and all of `Tools/`.

What is left is bookkeeping and the Java side:

* **~56 Java `native` declarations** in `src/main/java`, plus the 3 in `EventListenerImpl.java` and
  2 in `WebPage.java` that the last two C++ slices just unblocked. Only the Java agent may touch
  these.
* **69 export-map lines**, of which all but two are stale. The two live ones are
  `Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions` and `_twkSetShutdown`, which the
  Phase B slice is converting now; the rest come out with them.
* **7 build-file entries** — `find_package(JNI REQUIRED)` in `Source/cmake/OptionsJava.cmake` and
  the `JAVA_INCLUDE_PATH{,2}` / `${JAVA_JVM_LIBRARY}` lines in the JavaScriptCore, WebCore and WTF
  `PlatformJava.cmake` files. These come out **last**, because WTF's is applied `PUBLIC` and
  propagates to PAL, JSC, WebCore and WebKitLegacy — pulling it early silently blanks the include
  path rather than erroring.

## 21. Done — and what is deliberately not done

The verifier reports **0 across all eleven checks**, `mvn -pl modules/javafx.web install` is BUILD
SUCCESS, and the 137 FFM binding tests pass against the real `wkjstub` library. Every `native`
method, every `JNIEXPORT`, every `jni.h`, every JNI upcall, the whole `JavaEnv`/`JavaRef`
abstraction, both linker export maps and every JDK build dependency are gone.

### 21.1 The one thing this repository cannot do

**`jfxwebkit` must be rebuilt before any of this runs.** `modules/javafx.web` compiles Java only;
no pom or CMake file reachable from the Maven build compiles WebKit. So:

* The ~49,400 lines of migrated C++ have been **reviewed, not compiled**. Only the ABI headers were
  put through a compiler — all nine of them together, as C11 and C++20 at `/W4 /WX`, each
  double-included, with real function pointers assigned into every distinctive slot shape.
* The prebuilt `jfxwebkit.dll` still on `java.library.path` exports 1956 `Java_com_sun_*` symbols
  and zero `wkj_*`. The 473 module tests therefore cannot pass until the library is rebuilt from
  these sources with the WebKit CMake/ninja/clang toolchain. `WebKitLibraryAbiTest` is a deliberate
  non-skippable sentinel so that a green build cannot hide a stale library.
* Everything compiled here was **MSVC x64**. GCC 14 and Xcode 15 have seen none of it, nor has the
  `__attribute__((visibility("default")))` branch of `WKJ_EXPORT`, nor the ELF/Mach-O export globs.

### 21.2 Before this is trusted

1. Build `jfxwebkit` from these sources on all three platforms; confirm with `dumpbin /EXPORTS`,
   `nm -D --defined-only` and `nm -gU` that the `wkj_*` symbols are exported — the globs `wkj_*;`
   and `_wkj_*` are the only thing standing between the ABI and a silent link-time hole.
2. `mvn -pl modules/javafx.web test -Djfx.web.skipTests=false` against that library, and
   `mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Dtest='test/robot/javafx/web/**'`
   with a display. Those robot tests cover the pointer, editor and chrome upcall paths.
3. A DumpRenderTree `LayoutTests` run, diffed against the expected results.
4. Fix the `WKJ_EXPORT` export/import split before any second library calls a `wkj_*` function —
   it is unconditionally `dllexport`, so the first such call from `DumpRenderTreeJava` will fail to
   link on Windows.
5. Bump `WKJ_ABI_VERSION` once, at the first release.

### 21.3 What was removed, not merely rewritten

Whole files deleted: `JavaEnv.{h,cpp}`, `JavaRef.h`, `StringJava.cpp`, the old
`JavaDOMUtils.{h,cpp}`, `jni_jsobject.h`, DumpRenderTree's own `JavaEnv.{h,cpp}`,
`TouchEventJava.cpp`, and the thirteen dead files the WTF/WebCore audit identified — plus the 23
generated `com_sun_webkit_*.h` headers replaced by one checked-in `wkj_constants.h`, and 3467
lines of linker export map.

The C++ that remains is the same engine glue it always was, minus the JVM: it no longer includes a
JNI header, names a JNI type, caches a method id, or knows that Java exists beyond a table of
function pointers and an integer handle.

## 22. Two functional gaps the zero score does not cover

The verifier answers one question — is there any JNI left? — and the answer is no. It does **not**
say the module would work against a rebuilt `jfxwebkit`, and right now it would not. Two host tables
are declared in C and installed by nothing, so the C++ side has no route back into Java for them.
Neither is a `native` declaration, so neither showed up in any check.

### 22.1 `WKJHost` is 15 groups in C and 13 in Java

`webkit_java_api.h` declares fifteen groups; `WebKitNative.HOST_LAYOUT` models thirteen. Java is
missing `wtf` and `pal` entirely, and still models `graphics`, `network`, `media`, `filesystem` and
`theme` as one-pointer placeholders where C now has real tables. So `sizeof(WKJHost)` disagrees and
**`wkj_init` would answer `WKJ_INIT_ERR_HOST_SIZE` against a real library** — the ABI guard doing
exactly its job, but failing the whole module at startup.

The concrete consequence, beyond the size check: `WKJHostWTF.main_thread_schedule_dispatch` is
unfilled, so `WTF::callOnMainThread` work never reaches `MainThread.fwkScheduleDispatchFunctions`.
The downcall half of that round trip is bound; the upcall half is not.

This was left deliberately and correctly. `WebKitLayoutTest` asserts `HOST_LAYOUT` against the
**checked-in `wkjstub`**, which was generated when `WKJHost` had 13 groups. Changing the Java layout
alone turns 137 green tests red without fixing anything; the stub has to be regenerated from the
current headers in the same change.

### 22.2 `wkj_live_connect_init` is unbound

Along with the three `wkj_bridge_sizeof_*` self-check exports. Without the `WKJLiveConnectHost`
table (26 slots) the reflective half of the bridge — `JavaClassJSC`, `JavaFieldJSC`,
`JavaMethodJSC`, `JavaArrayJSC` — has no path back into Java, so **exposing a Java object to page
script would not work**, even though `JSObject` itself is now fully bound. The symbol test reports
these four as a note rather than a failure.

### 22.3 What closing them takes

One coordinated change, not two independent ones: regenerate `wkjstub` from the current nine ABI
headers, extend `HOST_LAYOUT` to all fifteen groups with their real shapes, fill `wtf` and `pal`,
replace the five placeholder groups, bind `wkj_live_connect_init` and install `WKJLiveConnectHost`.
The layout test then asserts the new shape against the new stub, and `wkj_init` stops rejecting.

Until that lands, the accurate statement is: **the module contains no JNI and compiles, and its
binding layer is tested, but it is not yet wired end to end.** That distinction is the whole reason
this file separates "verified" from "written".
