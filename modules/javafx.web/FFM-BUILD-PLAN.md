# javafx.web JNI -> FFM: the build-wiring plan

Companion to `FFM-ABI-CONTRACT.md`. Scope: the CMake/build inputs of the `jfxwebkit` library under
`modules/javafx.web/src/main/native` — everything the C++ migration needs from the build, and
everything the build has to lose before the library is JNI-free.

**This repository does not compile WebKit.** `mvn -pl modules/javafx.web install` builds Java only;
there is no `COMPILE_WEBKIT` switch and no Maven binding for the WebKit CMake/Ninja tree
(`jfx-web-native`, `WEBKIT-MEDIA-STUBS.md`). Everything below was derived by reading the CMake and
by grep; **none of it was compiled or linked locally**. Each step therefore carries a static
verification command that *can* be run here, and §5 lists what only an out-of-tree WebKit build
(or CI on Linux/macOS) can confirm.

All paths below are relative to `modules/javafx.web/src/main/native/` unless shown from the repo root.

---

## 1. Inventory: every place the WebKit build depends on the JVM

### 1.1 The one load-bearing include path

`Source/WTF/wtf/PlatformJava.cmake` puts `${JAVA_INCLUDE_PATH}` / `${JAVA_INCLUDE_PATH2}` into
`WTF_INCLUDE_DIRECTORIES`, and `_WEBKIT_TARGET_SETUP` (`Source/cmake/WebKitMacros.cmake:158`)
applies that variable as

```cmake
target_include_directories(${_target} PUBLIC "$<BUILD_INTERFACE:${${_logical_name}_INCLUDE_DIRECTORIES}>")
```

`PUBLIC` — so it propagates through `WTF` into `PAL`, `JavaScriptCore`, `WebCore` and
`WebKitLegacy` (`WebCore_FRAMEWORKS` at `Source/WebCore/CMakeLists.txt:2139-2143` lists `WTF`).
**Every other JDK include entry in the tree is therefore redundant today**, which is why they can be
removed in almost any order — and why the WTF one must be removed last.

### 1.2 Full inventory

| # | Site | What it is for | What breaks if removed too early |
|---|---|---|---|
| J1 | `Source/cmake/OptionsJava.cmake:42` — `find_package(JNI REQUIRED)` | Defines `JAVA_INCLUDE_PATH`, `JAVA_INCLUDE_PATH2`, `JAVA_JVM_LIBRARY`, and fails the configure step early with a readable message when no JDK is present. | Nothing *fails immediately*: CMake expands an undefined variable to the empty string, so J2-J8 silently become no-ops and the failure surfaces much later as `fatal error: jni.h: No such file or directory` in an arbitrary translation unit. Remove **last** of the JNI group. |
| J2 | `Source/WTF/wtf/PlatformJava.cmake:8-9` — `${JAVA_INCLUDE_PATH}` / `${JAVA_INCLUDE_PATH2}` in `WTF_INCLUDE_DIRECTORIES` | The `jni.h` include path for `wtf/java/JavaEnv.h`, `wtf/java/JavaRef.h`, `wtf/unicode/java/UnicodeJava.cpp` — and, because the property is `PUBLIC`, for every other framework in the build. | `jni.h` stops resolving **everywhere**, not just in WTF. This is the last include entry that may go. |
| J3 | `Source/WebCore/PlatformJava.cmake:35-36` — same two variables in `WebCore_SYSTEM_INCLUDE_DIRECTORIES` | Nominally the JDK headers for the ~89 WebCore/PAL files that touch `JNIEnv`. Applied `SYSTEM PRIVATE` (`WebKitMacros.cmake:159`), so it does not propagate onward. | Nothing today — J2 already supplies the same path publicly. Still, keep the discipline and gate it on the WebCore grep in F7. |
| J4 | `Source/JavaScriptCore/PlatformJava.cmake:16-18` — same two variables in `JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES` | Nominally the JDK headers for JavaScriptCore. | **Nothing at all — already dead.** `grep -rn 'jni\.h\|JNIEnv\|JavaVM\|jobject' Source/JavaScriptCore` returns **0 files**. Pure removal candidate, independent of the migration. |
| J5 | `Tools/DumpRenderTree/java/CMakeLists.txt:47-48` — in `DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES` | `jni.h` for `Tools/DumpRenderTree/java/JavaEnv.h` and the 12 `JNIEXPORT` entry points of the DRT harness. DRT is an independent `SHARED` library (`DumpRenderTreeJava`), not part of `jfxwebkit`. | The DRT harness stops compiling. DRT is the regression net for phases B-D, so this must not go until the DRT natives themselves are migrated (contract §6, `DumpRenderTreeNative`). |
| J6 | `Tools/TestRunnerShared/java/CMakeLists.txt:37-38` — in `TestRunnerShared_PRIVATE_INCLUDE_DIRECTORIES` | Same, for the `TestRunnerShared` OBJECT library. | Nothing — `Tools/TestRunnerShared` contains no JNI token at all; the entry is copy-paste from DRT. |
| J7 | `Source/WebCore/PlatformJava.cmake:6-10` and `:31` — `JAVA_JNI_GENSRC_PATH` in `WebCore_INCLUDE_DIRECTORIES` | The `javac -h` output directory (`<build>/../gensrc/headers/javafx.web`, JDK-8 fallback `../generated-src/headers`), added `PUBLIC`. Resolves the **48** `#include "com_sun_webkit_*.h"` lines in 28 files across WebCore *and* WebKitLegacy. | Those 48 includes stop resolving. Note `modules/javafx.web/pom.xml` has **no** `-h` compilerarg, so this directory is produced by the out-of-tree build, never by Maven here. |
| J8 | `Source/WTF/wtf/PlatformJava.cmake:40-42` — `${JDK_INCLUDE_DIRS}` in `WTF_SYSTEM_INCLUDE_DIRECTORIES` | Intended as a second JDK include path. | **Nothing — already dead.** `JDK_INCLUDE_DIRS` is set nowhere in the tree; it expands to the empty string. Free removal. |
| J9 | `Source/WTF/wtf/PlatformJava.cmake:37` — `${JAVA_JVM_LIBRARY}` in `WTF_LIBRARIES` | Link against `jvm.lib` / `libjvm.so`. `WTF_LIBRARIES` is applied `PUBLIC`, so this propagates too. | Nothing. Nothing in the tree resolves a JNI Invocation-API symbol at link time; the single reference (`Source/WebCore/bridge/jni/JNIUtility.cpp:55`) reaches `JNI_GetCreatedJavaVMs` through `dlsym`, which needs no import library. |
| J10 | `Source/WebCore/PlatformJava.cmake:81` — `${JAVA_JVM_LIBRARY}` in `WebCore_LIBRARIES` | Same. | Nothing (see J9). |
| J11 | `Source/JavaScriptCore/PlatformJava.cmake:12` — `${JAVA_JVM_LIBRARY}` in `JavaScriptCore_LIBRARIES` | Same. | Nothing. JavaScriptCore has zero JNI tokens (J4). Pure removal candidate today. |
| J12 | `Tools/DumpRenderTree/java/CMakeLists.txt:25` — `${JAVA_JVM_LIBRARY}` in `DumpRenderTree_LIBRARIES` | Same, for the DRT library. | Nothing (see J9). |
| J13 | `Tools/TestRunnerShared/java/CMakeLists.txt:19` — `${JAVA_JVM_LIBRARY}` in `TestRunnerShared_LIBRARIES` | Same, for `TestRunnerShared`. | Nothing (see J9). |
| J14 | `Source/WebCore/mapfile-vers` — `JNI_OnLoad;` / `JNI_OnUnload;` plus **1857** distinct `Java_*` entries | The GNU ld version script that decides what `libjfxwebkit.so` exports, wired in at `Source/WebKitLegacy/PlatformJava.cmake:166`. The trailing `local: *;` hides everything not listed. | `System.loadLibrary("jfxwebkit")` can no longer find `JNI_OnLoad`, and every remaining `native` method fails to bind. Must move in lockstep with the C++ symbols. |
| J15 | `Source/WebCore/mapfile-macosx` — `_JNI_OnLoad` / `_JNI_OnUnload` plus **1612** distinct `_Java_*` entries | The ld64 `-exported_symbols_list` equivalent, wired in at `Source/WebKitLegacy/PlatformJava.cmake:163`. | Same as J14, on macOS. |

### 1.3 Things that look like JVM dependencies but are not

* `Source/ThirdParty/icu/CMakeLists.txt:619` — `find_package(Java)`, used for
  `${Java_JAR_EXECUTABLE}` to unzip the ICU data archive ("Use jar instead of unzip, it will be
  helpful to get rid of cygwin dependency"). A **JDK tool** dependency, not a JNI one. **Keep it.**
* `Source/WebCore/WebCoreJava.def` — a 4-line MSVC module-definition file (`JSContextGetGlobalObject`,
  `WTFReportAssertionFailure`, `WTFReportBacktrace`). Referenced by **no** CMake file in the tree; a
  leftover from the pre-CMake build. It does **not** control Windows exports today.
* `Configurations/` — contains only `Version.xcconfig`, consumed by
  `Source/WebKitLegacy/scripts/generate-webkitversion.pl`. No JNI content.
* `WebKitLegacy_EXTERNAL_DEP` (`Source/WebKitLegacy/PlatformJava.cmake:164,168,172`) — assigned in
  all three platform branches and read nowhere. Dead variable; unrelated to JNI, but worth deleting
  while in the file.
* No `JAVA_HOME` / `JDK_HOME` reference exists anywhere under `src/main/native` outside
  `Source/ThirdParty`.

---

## 2. Source-list wiring for the new C ABI (implemented)

The WebKit build does **not** glob — unlike the javafx.graphics CMake, every source is listed:

* `Source/WebCore` sources come from `Source/WebCore/SourcesJava.txt`, referenced as
  `WebCore_UNIFIED_SOURCE_LIST_FILES` (`Source/WebCore/PlatformJava.cmake`, last stanza) and fed to
  `generate-unified-source-bundles.rb`. That script sorts the list itself, so position within the
  file does not affect bundling; grouping is cosmetic.
* `Source/WebKitLegacy` sources are a literal `list(APPEND WebKitLegacy_SOURCES …)` in
  `Source/WebKitLegacy/PlatformJava.cmake`.
* `Source/WTF` sources are a literal `list(APPEND WTF_SOURCES …)` in
  `Source/WTF/wtf/PlatformJava.cmake`.
* Headers are not picked up implicitly either. WTF headers must appear in `WTF_PUBLIC_HEADERS`
  (copied to `${WTF_FRAMEWORK_HEADERS_DIR}/wtf/…` by `WEBKIT_COPY_FILES` at
  `Source/WTF/wtf/CMakeLists.txt:805-808`, **not** `FLATTENED`, so the `java/` subdirectory survives
  and `#include <wtf/java/X.h>` works). WebCore headers must appear in
  `WebCore_PRIVATE_FRAMEWORK_HEADERS` (copied to `${WebCore_PRIVATE_FRAMEWORK_HEADERS_DIR}/WebCore`,
  which is how `Source/WebKitLegacy/java/DOM/JavaNode.cpp:47` gets `#include <WebCore/JavaDOMUtils.h>`).

Edits made for the three new files:

| New file | Owning CMake | Edit |
|---|---|---|
| `Source/WebKitLegacy/java/api/webkit_java_api.h` | `Source/WebKitLegacy/PlatformJava.cmake` | `"${WEBKITLEGACY_DIR}/java/api"` appended to `WebKitLegacy_INCLUDE_DIRECTORIES`; the same directory also added to `WTF_INCLUDE_DIRECTORIES` and `WebCore_INCLUDE_DIRECTORIES` so plain `#include "webkit_java_api.h"` resolves from all three layers (WTF is the lowest layer and must not have to spell a WebKitLegacy-relative path). Header-only: no source-list entry. |
| `Source/WTF/wtf/java/WKJHandle.h` | `Source/WTF/wtf/PlatformJava.cmake` | `java/WKJHandle.h` added to `WTF_PUBLIC_HEADERS`, beside the `java/JavaRef.h` it replaces. `wtf/java` is already on `WTF_INCLUDE_DIRECTORIES`, so no include-path change was needed. |
| `Source/WebCore/bindings/java/WKJDOMUtils.h` | `Source/WebCore/PlatformJava.cmake` | added to `WebCore_PRIVATE_FRAMEWORK_HEADERS`, beside the `bindings/java/JavaDOMUtils.h` it replaces, so the 105 WebKitLegacy DOM files can `#include <WebCore/WKJDOMUtils.h>`. |
| `Source/WebCore/bindings/java/WKJDOMUtils.cpp` | `Source/WebCore/SourcesJava.txt` | added to the `bindings/java` group. `bindings/java` is already on `WebCore_INCLUDE_DIRECTORIES`. |

Plus the export-map entries described in §4.1.

**Ordering constraint.** `WEBKIT_COPY_FILES` turns each `WTF_PUBLIC_HEADERS` entry into an
`add_custom_command(… MAIN_DEPENDENCY <file>)`, and `SourcesJava.txt` entries become real source
files. The CMake edits above therefore name files that do not exist yet. Nothing in *this*
repository builds WebKit, so nothing breaks here — but the CMake changes and the three new C++
files **must land in the same commit**, or an out-of-tree configure/build of this branch fails with
`Cannot find source file: bindings/java/WKJDOMUtils.cpp`.

**Deliberately not changed.** `Tools/DumpRenderTree/java/CMakeLists.txt` and
`Tools/TestRunnerShared/java/CMakeLists.txt` do not get the `java/api` include directory yet. DRT
already has `${CMAKE_SOURCE_DIR}/Source` on its include path, so
`#include <WebKitLegacy/java/api/webkit_java_api.h>` resolves there today; add the short form when
the DRT natives migrate.

---

## 3. Staged removal checklist for Phase F

Order matters: **F1-F4 are safe today**, F5-F9 need the C++ migration, and F10 is last. Every
verification command is run from the repository root and **must print nothing**.

### F1. JavaScriptCore JDK includes (J4) — safe today

Delete the `list(APPEND JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES ${JAVA_INCLUDE_PATH}
${JAVA_INCLUDE_PATH2})` block (lines 15-18) from `Source/JavaScriptCore/PlatformJava.cmake`.
Precondition: JavaScriptCore contains no JNI token. Already true.

```
grep -rn 'jni\.h\|JNIEnv\|JavaVM\|jobject\|jclass' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Source/JavaScriptCore
```

### F2. JavaScriptCore libjvm link (J11) — safe today

Delete the `list(APPEND JavaScriptCore_LIBRARIES ${JAVA_JVM_LIBRARY})` block (lines 11-13) from
`Source/JavaScriptCore/PlatformJava.cmake`. Precondition: same grep as F1.

### F3. Dead `${JDK_INCLUDE_DIRS}` block (J8) — safe today

Delete the `list(APPEND WTF_SYSTEM_INCLUDE_DIRECTORIES "${JDK_INCLUDE_DIRS}")` block from
`Source/WTF/wtf/PlatformJava.cmake`. Precondition: the variable is set nowhere.

```
grep -rn 'JDK_INCLUDE_DIRS' modules/javafx.web/src/main/native --include=*.cmake --include=CMakeLists.txt | grep -v 'wtf/PlatformJava.cmake'
```

### F4. TestRunnerShared JDK includes and libjvm (J6, J13) — safe today

Delete `${JAVA_INCLUDE_PATH}` / `${JAVA_INCLUDE_PATH2}` and the `${JAVA_JVM_LIBRARY}` entry from
`Tools/TestRunnerShared/java/CMakeLists.txt`. Precondition: no JNI token in that directory.

```
grep -rn 'jni\.h\|JNIEnv\|JavaVM\|jobject\|JNIEXPORT' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Tools/TestRunnerShared
```

### F5. Remaining `${JAVA_JVM_LIBRARY}` link entries (J9, J10, J12)

Delete them from `Source/WTF/wtf/PlatformJava.cmake`, `Source/WebCore/PlatformJava.cmake` and
`Tools/DumpRenderTree/java/CMakeLists.txt`. Precondition: nothing resolves a JNI Invocation-API
symbol. Already true at link time, but the one `dlsym` site lives in a Phase-D deletion target, so
land this with that commit.

```
grep -rn 'JNI_CreateJavaVM\|JNI_GetCreatedJavaVMs\|JNI_GetDefaultJavaVMInitArgs' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Source modules/javafx.web/src/main/native/Tools
```

### F6. Generated JNI header directory (J7)

Delete the `JAVA_JNI_GENSRC_PATH` definition and its `"${JAVA_JNI_GENSRC_PATH}"` entry (with the
`# JNI headers` comment) from `Source/WebCore/PlatformJava.cmake`. Precondition: no source includes
a `javac -h` header. 48 hits in 28 files today.

```
grep -rn '#include.*"com_sun_' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Source modules/javafx.web/src/main/native/Tools
```

### F7. WebCore JDK includes (J3)

Delete the `list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES …)` block from
`Source/WebCore/PlatformJava.cmake`. Precondition: no file under `Source/WebCore` (which contains
`Source/WebCore/PAL`) includes `jni.h`. 22 files today.

```
grep -rn 'jni\.h' --include=*.cpp --include=*.h --include=*.mm modules/javafx.web/src/main/native/Source/WebCore
```

### F8. DumpRenderTree JDK includes (J5)

Delete `${JAVA_INCLUDE_PATH}` / `${JAVA_INCLUDE_PATH2}` from
`Tools/DumpRenderTree/java/CMakeLists.txt`, and drop `JavaEnv.cpp` from `DumpRenderTree_SOURCES`
once `Tools/DumpRenderTree/java/JavaEnv.{h,cpp}` are deleted. Precondition: the DRT harness is on
the C ABI.

```
grep -rn 'jni\.h\|JNIEXPORT\|JNIEnv' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Tools/DumpRenderTree
```

### F9. The JNI entries in the two export maps (J14, J15)

Delete `JNI_OnLoad;` / `JNI_OnUnload;` and every `Java_*;` line from `Source/WebCore/mapfile-vers`,
and `_JNI_OnLoad` / `_JNI_OnUnload` / every `_Java_*` line from `Source/WebCore/mapfile-macosx`.
Precondition: **no** `JNIEXPORT` remains anywhere, and Java no longer expects `JNI_OnLoad`.
2014 hits today.

```
grep -rn 'JNIEXPORT\|JNI_OnLoad\|JNI_OnUnload' --include=*.cpp --include=*.h modules/javafx.web/src/main/native/Source modules/javafx.web/src/main/native/Tools
```

This is the most destructive step available: a `Java_*` symbol that still exists in the C++ but is
missing from the map still links, and then fails at **first use** with `UnsatisfiedLinkError`, not
at load. Do it in the same commit that deletes the corresponding C++.

### F10. `find_package(JNI REQUIRED)` and the WTF JDK includes (J1, J2) — last

Delete `Source/cmake/OptionsJava.cmake:42` and the two `${JAVA_INCLUDE_PATH*}` entries in
`Source/WTF/wtf/PlatformJava.cmake`. Precondition: **nothing anywhere** includes `jni.h` and no
`JAVA_*` JNI variable is still referenced.

```
grep -rn 'jni\.h\|JAVA_INCLUDE_PATH\|JAVA_JVM_LIBRARY' --include=*.cpp --include=*.h --include=*.mm --include=*.cmake --include=CMakeLists.txt modules/javafx.web/src/main/native/Source modules/javafx.web/src/main/native/Tools
```

Because CMake expands an undefined variable to the empty string, doing F10 before F2-F8 does not
error — it silently deletes include paths and produces a `jni.h: No such file or directory` in an
unrelated file. Keep the order.

### F11. Opportunistic cleanups (any time)

* Delete `Source/WebCore/WebCoreJava.def` — referenced by no CMake file.
  `grep -rn 'WebCoreJava.def' modules/javafx.web/src/main/native` must print nothing (it already does).
* Delete the three `set(WebKitLegacy_EXTERNAL_DEP …)` assignments in
  `Source/WebKitLegacy/PlatformJava.cmake` — read nowhere.
  `grep -rn '_EXTERNAL_DEP' modules/javafx.web/src/main/native/Source/cmake` must print nothing
  (it already does).

### F12. Fork-level follow-ups

* Update the library table and the "Where the JNI lives" section of
  `.claude/skills/jfx-web-native/SKILL.md`.
* `modules/javafx.web/pom.xml` needs no change: unlike `modules/javafx.media/pom.xml` it never had
  a `javac -h` compilerarg, so there is no header-generation step to retire.

---

## 4. Symbol export control, per platform

**Finding: `WKJ_EXPORT` alone is sufficient only on Windows.**
`Source/WebKitLegacy/PlatformJava.cmake:162-173`:

```cmake
if (APPLE)
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "-exported_symbols_list ${WEBCORE_DIR}/mapfile-macosx")
    set(WebKitLegacy_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-macosx")
elseif (UNIX)
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "-Xlinker -version-script=${WEBCORE_DIR}/mapfile-vers -Wl,--no-undefined")
    set_property(TARGET WebKitLegacy APPEND PROPERTY LINK_DEPENDS "${WEBCORE_DIR}/mapfile-vers")
    set(WebKitLegacy_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-vers")
elseif (WIN32)
    # Adds version information to jfxwebkit.dll created by Gradle build, see JDK-8166265
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
    set(WebKitLegacy_EXTERNAL_DEP "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
endif ()
```

| Platform | Mechanism | Is `WKJ_EXPORT` enough? |
|---|---|---|
| **Windows** | No `.def`, no export list — `LINK_FLAGS` carry only the version resource. `add_definitions(/DWEBKIT_EXPORTS …)` at `Source/WebKitLegacy/CMakeLists.txt:44-48` is explicitly guarded by `NOT PORT STREQUAL Java`. Exports come solely from `__declspec(dllexport)`, which is exactly what `JNIEXPORT` expands to today. | **Yes.** `WKJ_EXPORT` = `__declspec(dllexport)` is the whole mechanism. |
| **Linux / other Unix** | GNU ld version script `Source/WebCore/mapfile-vers`, 1999 lines, `SUNWprivate_1.0 { global: … local: *; };`. The trailing `local: *;` **hides every symbol not listed**. There is no `-fvisibility=hidden` anywhere in the Java port (`CMAKE_C(XX)_VISIBILITY_PRESET hidden` appears only in `OptionsGTK.cmake` and `OptionsJSCOnly.cmake`), so `__attribute__((visibility("default")))` is a no-op here — the version script decides. | **No, not by itself.** A `WKJ_EXPORT` function absent from `mapfile-vers` is localised and `SymbolLookup.libraryLookup` finds nothing. |
| **macOS** | ld64 `-exported_symbols_list Source/WebCore/mapfile-macosx`, 1743 lines, an explicit allow-list. Same effect: unlisted symbols become local. Note the Apple branch, unlike the Unix one, does not add the mapfile to `LINK_DEPENDS`, so editing it does not force a relink — a `touch` on the target or a clean build is needed. | **No, not by itself.** |

### 4.1 What was done about it

Rather than generating ~2000 `wkj_*` entries with a script (Perl is available in this environment —
`buildtools/ffm-web/*.pl` already uses it; Python and Ruby are **not** installed), both linkers
accept **glob patterns** in the export list, so one line per platform covers the whole ABI and needs
no maintenance as the DOM generator adds symbols:

* `Source/WebCore/mapfile-vers` — `wkj_*;` added as the first entry under `global:`. GNU ld's
  version-script grammar accepts wildcard patterns (`*`, `?`, `[…]`) for symbol names, and a pattern
  that matches nothing is not diagnosed. The accompanying comment uses `/* … */`, which the
  linker-script lexer handles in version-script mode; `#` is **not** a comment there.
* `Source/WebCore/mapfile-macosx` — `_wkj_*` added as the first entry. Note the leading underscore:
  Mach-O prefixes C symbols with `_`, which is why every entry in that file is `_Java_…`. ld64
  supports `*` globbing in `-exported_symbols_list`, and `#` starts a comment in that file format.

Neither wildcard can affect the existing JNI exports — an export list is a union — so both edits are
inert until the first `wkj_*` symbol exists.

**Not verifiable here.** There is no Linux or macOS toolchain and no WebKit build on this machine,
so the two wildcard forms were reasoned from the linker documentation, not tested. The first
out-of-tree WebKit build on each platform must confirm:

```
nm -D --defined-only libjfxwebkit.so   | grep ' T wkj_'      # Linux
nm -gU               libjfxwebkit.dylib | grep '_wkj_'        # macOS
dumpbin /EXPORTS     jfxwebkit.dll      | findstr wkj_        # Windows, VS developer shell
```

If ld64 rejects the `#` comment lines, delete them — the `_wkj_*` line is the load-bearing part.

### 4.2 Pre-existing inconsistency (recorded, not fixed)

The two export maps have drifted apart and are **not** two spellings of one list:

| | `mapfile-vers` | `mapfile-macosx` |
|---|---:|---:|
| total lines | 1999 | 1743 |
| distinct `Java_*` symbols | 1857 | 1612 |
| present only in this file | **249** | **4** |

The 249 Linux-only entries are dominated by `Java_com_sun_webkit_dom_DOMSelectionImpl_*` and
`Java_com_sun_webkit_dom_DOMImplementationImpl_hasFeatureImpl`, whose `.cpp` files are commented out
of `WebKitLegacy_SOURCES` (`Source/WebKitLegacy/PlatformJava.cmake:10-11`) — i.e. stale entries for
symbols that no longer exist. The 4 macOS-only entries are the
`Java_com_sun_webkit_network_SocketStreamHandle_twkDid*` callbacks, which look genuinely missing
from the Linux map.

**Do not fix this as part of the FFM work.** It is a pre-existing bug with its own risk profile, and
folding it into a migration commit destroys the behaviour-neutral property those commits are
supposed to have. It also disappears at step F9. It is precisely why a wildcard beats a maintained
list.

### 4.3 A note on the ABI-version guard

`wkj_abi_version()` (contract §5) is only useful if it is actually exported, and on Linux/macOS that
depends entirely on §4.1. If the wildcard were wrong, the failure mode is an `UnsatisfiedLinkError`
at the *very first* lookup in `WebKitNative` — at least immediate and readable rather than partial.

---

## 5. What could not be verified in this repository

| Claim | Why unverified | Who must verify |
|---|---|---|
| The new CMake entries actually compile the three new files | WebKit is not built by any Maven or CMake invocation in this repository. | An out-of-tree WebKit build, any platform. |
| `wkj_*;` in a GNU ld version script exports the ABI | No Linux toolchain here. | CI / a Linux WebKit build. |
| `_wkj_*` in an ld64 export list exports the ABI, and `#` is a comment in that file | No macOS toolchain here. | A macOS WebKit build. |
| Windows `__declspec(dllexport)` suffices | Consistent with how today's `JNIEXPORT` symbols leave `jfxwebkit.dll`, but not linked here. | A Windows WebKit build. |
| The 249/4 export-map divergence is stale rather than a live bug | Would need a link of the current sources on each platform. | Out of scope; F9 removes the question. |

The static parts *were* checked here: all four edited `.cmake` files parse (`cmake -P` reaches
execution and fails only on non-scriptable commands such as `add_definitions`, never on a parse
error), the diff introduces no CR characters, and every "must print nothing" grep in §3 was run to
establish the counts quoted above.

## 6. Note on file headers

The WebKit-derived `.cmake` files under `src/main/native` carry **no** copyright header — zero of
them match `Oracle and/or its affiliates`, because they descend from upstream WebKit rather than
from OpenJFX. `openjfx-conventions` asks for the GPLv2+CPE header on `.cmake` files; that rule
applies to OpenJFX-authored CMake (e.g. `modules/javafx.graphics/native/*.cmake`). Adding an Oracle
header to a WebKit port file would misstate provenance and create merge friction with upstream
WebKit updates, so the edits in §2 add none. `Source/WebCore/SourcesJava.txt` *does* carry the
Oracle header and its second year is already `2026`, so no year bump was required there either.
