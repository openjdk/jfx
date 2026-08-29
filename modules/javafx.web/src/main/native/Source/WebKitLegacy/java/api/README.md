# The `wkj_*` C ABI of jfxwebkit

This directory holds the public C ABI that replaces JNI as the boundary between `javafx.web`
and the WebKit Java port. The design it implements is `modules/javafx.web/FFM-ABI-CONTRACT.md`;
this file records only what someone rebuilding the library has to know.

`jfxwebkit` is **not built by this repository**. Everything here is compiled out of tree with
the WebKit CMake/ninja toolchain, so nothing below is checked by `mvn install`.

## Files

| File | Written by | Contents |
|---|---|---|
| `webkit_java_api.h` | hand | the core ABI: `WKJ_EXPORT`, `wkj_abi_version`, `wkj_ref`, `wkj_peer`, `wkj_to_ptr` / `wkj_from_ptr`, `WKJExceptionSlot` + `wkj_exception_slot`, `WKJHost` + `wkj_init` |
| `webkit_java_api_dom.h` | generated | the 1796 compiled `wkj_dom_<Type>_<method>` entry points, from `buildtools/ffm-web/dom-abi.tsv` via `spec-to-header.pl`. It includes `webkit_java_api.h`; the reverse must never happen |
| `webkit_java_api_page.h` | hand | the WebPage / BackForwardList / PageCache / ColorChooser downcalls and the ten client callback tables. Includes `webkit_java_api.h` |
| `webkit_java_api_platform.h` | hand | `WKJHostGraphics`, `WKJHostNetwork`, `WKJHostMedia` plus the graphics and network downcalls. **#errors if included directly** - it is pulled in by `webkit_java_api.h`, because its structs are members of `WKJHost` and must be complete mid-header |
| `webkit_java_api_theme.h` | hand | `WKJHostTheme` (43 slots) and `WKJHostFileSystem` (10). Same include direction as `_platform.h`, for the same reason |
| `webkit_java_api_wtf.h` | hand | `WKJHostWTF` (1 slot, `com.sun.webkit.MainThread`) plus `wkj_main_thread_dispatch_functions` and `wkj_set_shutdown`. Same include direction as `_platform.h` |
| `webkit_java_api_pal.h` | hand | `WKJHostPAL` (4 slots): `com.sun.webkit.security.WCMessageDigest` and `PAL::systemBeep`. Same include direction as `_platform.h` |
| `webkit_java_api_bridge.h` | hand | the LiveConnect ABI: nine `wkj_js_*` entry points, `wkj_frame_execute_script`, and `WKJLiveConnectHost` (26 slots). Includes `webkit_java_api.h` normally - its table is installed separately by `wkj_live_connect_init`, so it is not a member of `WKJHost` |
| `wkj_constants.h` | generated | the 315 constants shared with Java, from the Java sources via `buildtools/ffm-web/gen-wkj-constants.pl`. Replaces the 23 `com_sun_webkit_*.h` headers `javac -h` used to emit |
| `../../../WTF/wtf/java/WKJHandle.h` | hand | `WKJHandle`, the RAII owner of a `wkj_ref`, replacing `JLocalRef` / `JGlobalRef` |
| `../../../WTF/wtf/java/WKJRuntime.{h,cpp}` | hand | the successor to the deleted `wtf/java/JavaEnv.{h,cpp}`: the `wkj_host` global, `wkj_init`, `wkj_abi_version`, the `g_ShuttingDown` flag with `wkjIsShuttingDown()` / `WKJ_RETURN_IF_SHUTTING_DOWN`, `wkjCheckAndClearException()`, and the UTF-16 string bridge (`wkjMakeString`, `WKJStringArg`, `wkjFetchString`) that `wtf/java/StringJava.cpp` used to be |
| `../../../WebCore/bindings/java/WKJDOMUtils.{h,cpp}` | hand | the helpers the generated DOM sources call: `WKJString`, `WKJReturnString`, `WKJReturnPeer`, `raiseOnDOMError`, `raiseTypeErrorException`, `raiseNotSupportedErrorException`, plus the definition of `wkj_exception_slot` |

## Rules

1. **No JVM in the library.** No JNI header, no JNI type, no JNI environment, no cached
   member ids anywhere below this boundary. A Java object is a `wkj_ref` registry id; a
   Java call is a function pointer in `WKJHost`.
2. **`<stdint.h>` types only** in anything exported. No `long`, no `bool`, no `size_t` in a
   signature: MSVC and GCC disagree about their widths and FFM has no boolean layout. A
   Java `boolean` is `int32_t` carrying 0 or 1.
3. **Every exported function is `WKJ_EXPORT`.** There is no `.def` file and no export list.
   On Windows the macro is the only thing that exports a symbol; on Linux and macOS it is
   what keeps working if the build ever moves to `-fvisibility=hidden`.
4. **Every callback slot in `WKJHost` may be `NULL`.** Check the pointer, then fall back to
   the default documented on the slot. Java is allowed to install a partial table, and a
   newer Java against an older library is exactly the case this makes survivable.
5. **`WKJ_ABI_VERSION` is bumped by any shape change** - a struct member added, removed or
   reordered, a signature changed, a documented meaning changed. `WebKitNative` compares it
   with `wkj_abi_version()` at load time, which is what turns "a stale `jfxwebkit.dll` is on
   the library path" into one readable sentence.
6. **Strings are UTF-16 both ways.** Into the library, `const uint16_t* s, int32_t s_len`:
   `NULL` and a non-`NULL` pointer with length 0 both become the empty `WTF::String`, which
   is the collapse the JNI constructor has always done (contract §11.1). Out of the library,
   the **caller provides the buffer** (contract §13) and the function returns `WKJ_STR_OK`,
   `WKJ_STR_NULL` or `WKJ_STR_OVERFLOW`. The library owns no string memory and returns no
   pointer, so there is no lifetime rule anywhere in this ABI. Null stays distinguishable
   from empty on the way out: `WKJ_STR_NULL` versus `WKJ_STR_OK` with length 0.
7. **C never throws.** It fills the calling thread's `WKJExceptionSlot`, whose message lives
   inline in the slot, and returns a default; Java reads the slot from memory, throws and
   clears it by storing `WKJ_EXC_NONE` into `type`. **Every `wkj_*` function clears the slot
   on entry**, so a missed check on the Java side cannot leak an exception into the next,
   unrelated call on that thread.
8. **Do not hand-write a DOM declaration.** The DOM half of the ABI is generated from the
   JNI sources. A hand-written sample of it already got `wkj_dom_Element_getScrollTop`
   wrong - `double` where the JNI function returns `jint` - which is the whole argument for
   generating it.

## Build wiring

`${WEBKITLEGACY_DIR}/java/api` is on the include path of WTF, WebCore and WebKitLegacy, so
**every layer spells the include the same way: `#include <webkit_java_api.h>`**. Use that
spelling. `<WebKitLegacy/java/api/webkit_java_api.h>` resolves only where
`${CMAKE_SOURCE_DIR}/Source` happens to be on the include path, which is true for WTF and
not for WebCore or WebKitLegacy.

Already wired (see `git log` for the commit):

```cmake
# Source/WTF/wtf/PlatformJava.cmake
list(APPEND WTF_INCLUDE_DIRECTORIES "${WEBKITLEGACY_DIR}/java/api")
list(APPEND WTF_PUBLIC_HEADERS java/WKJHandle.h)

# Source/WebCore/PlatformJava.cmake
list(APPEND WebCore_INCLUDE_DIRECTORIES "${WEBKITLEGACY_DIR}/java/api")
list(APPEND WebCore_PRIVATE_FRAMEWORK_HEADERS bindings/java/WKJDOMUtils.h)

# Source/WebKitLegacy/PlatformJava.cmake
list(APPEND WebKitLegacy_INCLUDE_DIRECTORIES "${WEBKITLEGACY_DIR}/java/api")

# Source/WebCore/SourcesJava.txt
bindings/java/WKJDOMUtils.cpp
```

Still to do, when JavaScriptCore stops using JNI (phase D, LiveConnect):

```cmake
# Source/JavaScriptCore/PlatformJava.cmake
list(APPEND JavaScriptCore_INCLUDE_DIRECTORIES "${WEBKITLEGACY_DIR}/java/api")
```

Note the layering: `WKJHandle.h` lives in WTF and includes a header under `WebKitLegacy`.
That inversion is nominal - the header declares types and function pointers and pulls in no
code, so there is no link dependency - but it is the reason the include directory has to be
added to WTF rather than only to WebCore.

## The load hook

`wtf/java/WKJRuntime.cpp` holds the three definitions the ABI needs before anything else can
run, and the global that goes with them. It is what `wtf/java/JavaEnv.cpp` became.

* `uint32_t wkj_abi_version(void)` - returns `WKJ_ABI_VERSION`.
* `int32_t wkj_init(const WKJHost*, int32_t, uint32_t)` - validates `abi_version` against
  `WKJ_ABI_VERSION` and `host_size` against both `host->size` and `sizeof(WKJHost)`, then
  stores the pointer in `wkj_host`. It replaces `JNI_OnLoad`, `JNI_OnUnload` and
  `JNI_OnLoad_jfxwebkit`, all three of which are gone.
* `const WKJHost* wkj_host` - the stored table, `NULL` until `wkj_init` succeeds.

Two consequences of that replacement are worth knowing.

`JNI_OnLoad` resolved `com.sun.webkit.FileSystem` eagerly because the class loader that loaded
`jfxwebkit` was reachable only from that hook. There is no class lookup in this ABI, so the
eager resolution, the `JGClass` that held it and the load-time ordering requirement are all
gone together.

`JNI_OnUnload` also called `_CrtDumpMemoryLeaks()` in a Windows debug build. There is no
unload hook, so the flag setup moved into `wkj_init` and the dump now comes from
`_CRTDBG_LEAK_CHECK_DF` at process exit instead. Debug diagnostics only, and `JNI_OnUnload`
almost never ran anyway - a library loaded with `System.load` is unloaded only when its class
loader is collected.

`wkj_exception_slot()` is still defined in `WebCore/bindings/java/WKJDOMUtils.cpp`, because
the DOM bindings remain its only writer.

`WKJHost` sub-structs still holding a single `reserved` member - `webpage`, `frameloader`,
`chrome`, `editor`, `contextmenu`, `inspector`, `drag` - are placeholders, because an empty
struct is not valid C. `core`, `graphics`, `network`, `media`, `filesystem`, `theme`, `wtf`
and `pal` are real.

**`WKJHost` gained two members after the Java-side layout was first written**: `wtf` and
`pal`, appended after `theme`. Whoever owns the Java `WKJHost` layout has to re-derive it -
`wkj_init` will otherwise reject the table with `WKJ_INIT_ERR_HOST_SIZE`, which is loud, but
only at startup. `WKJ_ABI_VERSION` is still 1: every slice so far has left it alone because
nothing has shipped, and it should be bumped exactly once, by whoever cuts the first release.

## Verifying a rebuilt library

```sh
# Windows
dumpbin /EXPORTS jfxwebkit.dll | findstr wkj_
# Linux
nm -D --defined-only libjfxwebkit.so | grep ' T wkj_'
# macOS
nm -gU libjfxwebkit.dylib | grep _wkj_
```

Every symbol `WebKitNative` binds must appear, and `wkj_abi_version` must return the
`WKJ_ABI_VERSION` the Java side was compiled against. Drop the rebuilt library into
`../caches/sdk/bin` (Windows) or `../caches/sdk/lib` (Linux, macOS) next to the repository -
see `WEBKIT-MEDIA-STUBS.md` - and run
`mvn -pl modules/javafx.web test -Djfx.web.skipTests=false`.

## Phase B hazards: three things that do not survive a mechanical rewrite

`JavaEnv.h` and `JavaRef.h` are replaced by `WKJHost` and `WKJHandle`, but three behaviours
of the JNI code have no automatic counterpart. Each one fails silently — no compile error,
no exception, just a crash or a wrong result later — so they are recorded here rather than
left to be rediscovered.

### 1. Shutdown gating disappears

`WTF::AttachThreadToJavaEnv` (`JavaEnv.h:83-114`) checks `g_ShuttingDown` first, and when it
is set it leaves `m_env` null instead of attaching. `WC_GETJAVAENV_CHKRET` then turns a null
env into an early return at each of its **10 call sites** (it was 11 until
Source/WebCore/platform/java/TextCodecJava.cpp was deleted as dead code):

```
Source/WebCore/bindings/java/JavaEventListener.cpp:78
Source/WebCore/PAL/pal/system/java/SoundJava.cpp:36
Source/WebCore/platform/graphics/java/BitmapImageJava.cpp:45          (returns a value)
Source/WebCore/platform/graphics/java/MediaPlayerPrivateJava.cpp:289
Source/WebCore/platform/java/MainThreadSharedTimerJava.cpp:45, :57
Source/WebCore/platform/network/java/SocketStreamHandleImplJava.cpp:76
Source/WebKitLegacy/java/WebCoreSupport/BackForwardList.cpp:153
Source/WebKitLegacy/java/WebCoreSupport/FrameLoaderClientJava.cpp:200
Source/WebKitLegacy/java/WebCoreSupport/PopupMenuJava.cpp:76
```

`g_ShuttingDown` is set from `Java_com_sun_webkit_MainThread_twkSetShutdown`
(`wtf/java/MainThreadJava.cpp:131`) and is also read by `ThreadTimers.cpp:76`.

With FFM there is no attach and no null env, so unless the replacement keeps an explicit
`g_ShuttingDown` check at those 11 sites, **upcalls start firing during shutdown where they
previously did not** — timers, socket callbacks, frame-loader notifications and popup menus,
into a Java side that is tearing down. Keep the flag (it becomes a plain C global set by an
exported `wkj_*` setter) and keep a guard macro with the same shape, so the rewrite is a
substitution rather than a deletion.


#### Correction, measured while doing the work: those ten sites were not the shutdown gate

The paragraph above is right that a gate disappears, and wrong about where it was. Reading the
two macros apart settles it:

* `WC_GETJAVAENV_CHKRET` expands to `WTF::GetJavaEnv()` plus an early return, and
  `GetJavaEnv()` is `jvm->GetEnv(&env, JNI_VERSION_1_2)`. It never looks at `g_ShuttingDown`.
  It yields null in exactly one case: **the calling thread is not attached to the JVM.** So
  those ten sites gated on thread attachment, not on shutdown.
* `AttachThreadToJavaEnv` (`JavaEnv.h:87-99`) is the one that tested `g_ShuttingDown`, and it
  left `m_env` null when it was set.

There were seven `AttachThread*` sites, and six of them - `WorkQueueGeneric.cpp` (x2),
`AsyncFileStream.cpp`, `WorkerThread.cpp`, `StorageThread.cpp` and
`MainThreadJava.cpp::initializeMainThreadPlatform` - simply declared the RAII object and then
ran their body regardless; a null env meant "did not attach", not "do not proceed". Exactly
**one** call in the tree was actually gated on shutdown:

```
Source/WTF/wtf/java/MainThreadJava.cpp   scheduleDispatchFunctionsOnMainThread()
    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();
    if (env) { ... }          <-- the whole shutdown gate of the library
```

That one is preserved explicitly, as `if (wkjIsShuttingDown()) return;`. `g_ShuttingDown`
itself survives unchanged, in `wtf/java/WKJRuntime.cpp`, set by the exported
`wkj_set_shutdown` and read through `WTF::wkjIsShuttingDown()`; `ThreadTimers.cpp` still reads
it the same way it always did.

What the ten `WC_GETJAVAENV_CHKRET` sites lose is different and worth stating in its own
terms: an FFM upcall stub attaches the calling thread itself, so **an upcall made from a
WebKit-spawned thread that was never attached now happens where it used to be silently
dropped**. For the sites the ABI headers already document as main-thread-only that is no
change at all, because the main thread is always attached. The ones to look at are the
callbacks that can run off the main thread - `MediaPlayerPrivateJava`, `BitmapImageJava` and
`SocketStreamHandleImplJava`. `WKJ_RETURN_IF_SHUTTING_DOWN` in `wtf/java/WKJRuntime.h` is
shaped like the macro it replaces so that any of them can take the shutdown half of the gate
back with a one-line substitution.

### 2. Leaked local references were free; leaked ids are not

JNI reclaimed every local reference when the native method returned, so a missed
`DeleteLocalRef` cost nothing. A registry reclaims nothing: an id that is never released
pins its Java object for the life of the process. The **23 `releaseLocal()` / 2
`releaseGlobal()` sites** each hand ownership to a caller, and every one needs a named owner
on the other side after the rewrite — a `WKJHandle` that will be destroyed, or an explicit
`release`. `WKJHandle::leakRef()` is marked `[[nodiscard]]` for exactly this reason.

The same applies to the raw-versus-wrapper constructor split documented at the top of
`wtf/java/WKJHandle.h`: `JGlobalRef(T raw)` **consumes** its argument while
`JGlobalRef(const JLocalRef&)` **copies**. Treating both as adopt, or both as copy, produces
a double release across the 399 sites that name one of those types.

### 3. What the Java registry must promise

The ABI asks for one thing only: **every id obtained from `retain` or `retain_weak` is
released exactly once**. Two implementations satisfy it.

* **Fresh id per retain** — the JNI model. Simple: a `ConcurrentHashMap<Long, Object>` plus
  a counter, `release` removes. `a == b` is then false for two ids naming one object, which
  is exactly what `JLocalRef::operator==` did with two `NewLocalRef` results.
* **Interned by object identity, with a reference count** — `retain` returns the same id and
  increments; `release` decrements and removes at zero. This is the friendlier model and the
  recommended one, because it makes id equality mean object equality, so any future
  comparison site works without a `core.equals` call.

Interning **without** a reference count is a bug: one owner's `release` would invalidate an
id another owner still holds. Note also that `retain_weak` exists because
`bridge/jni/JobjectWrapper.cpp:45` takes `NewWeakGlobalRef` by default; weak ids must not
keep the object reachable, and `core.is_live` is how C asks whether one is still valid.

For the record, the choice is currently unobservable from C++: a sweep of all 101 files that
name a `JLObject`/`JGObject`-family type found **no comparison of one handle with another** —
every use of the comparison operators on those types is a null test through `operator!`.
`core.hash_code` and `core.equals` are provisioned for the LiveConnect phase, not required by
anything today.
