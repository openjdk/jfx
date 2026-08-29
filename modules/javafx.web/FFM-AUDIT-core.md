# FFM audit -- javafx.web core (non-DOM) WebKitLegacy layer

Scope, C++ (paths relative to `modules/javafx.web/src/main/native/`):
`Source/WebKitLegacy/java/**` excluding `DOM/`, `Tools/DumpRenderTree/java/**`,
`Tools/TestRunnerShared/java/**`.

Scope, Java (relative to `modules/javafx.web/src/main/java/`): `com/sun/webkit/{WebPage,
BackForwardList,PageCache,ContextMenu,PopupMenu,ColorChooser,WCWidget,WCPluginWidget,Disposer,
MainThread,Timer}.java`, `com/sun/javafx/webkit/UIClientImpl.java`,
`com/sun/javafx/webkit/drt/DumpRenderTree.java`, `javafx/scene/web/{WebEngine,WebView}.java`.

Read-only audit. Nothing in this report was applied to the tree.

**Audit snapshot.** Taken against the working tree of 2026-08-28. While this audit was running, a
parallel `ffm-migrator` session began Phase A in the same tree: it added
`Source/WebKitLegacy/java/api/webkit_java_api.h`, `Source/WTF/wtf/java/WKJHandle.h`,
`Source/WebCore/bindings/java/WKJDOMUtils.h` and `.cpp`, `com.sun.webkit.WebKitNative`, and a
`wkj_*` glob to both export lists (`Source/WebCore/mapfile-vers:6`, `mapfile-macosx:4`). Line
numbers below are from the current tree and already account for those insertions. **No file this
audit rules on was modified** -- `WebPage.cpp`, the `*ClientJava.cpp` files, `BackForwardList.cpp`
and the `Tools/*/java` sources are all clean in `git status`.

---

## 1. Verdict

**Split, and delete at the edges before migrating the middle.** The C++ in this slice is not glue
over an OS API -- it is glue over the *WebKit engine*, and the engine exposes no C-callable surface
that Java could bind instead. Hard evidence: `Source/WebCore/mapfile-vers` exports exactly **138**
pre-existing non-JNI symbols from `jfxwebkit` (139 now, counting the `wkj_*` glob the parallel
migrator added): `JNI_OnLoad`/`JNI_OnUnload`, ~100 JavaScriptCore C-API functions,
~25 WTF/JSC/WebCoreTestSupport mangled C++ symbols, and `WebPage_doJSCGarbageCollection` -- alongside
**1857** `Java_com_sun_*` entries; `dumpbin -dependents` on the shipped
`../caches/sdk/bin/jfxwebkit.dll` shows imports of only `KERNEL32/USER32/ADVAPI32/WINMM/WS2_32/
bcrypt` plus the MSVC CRT -- **no `jvm.dll`**. No `WebCore::Page`, `Settings`, `FrameLoader`,
`Editor` or `BackForwardCache` symbol appears in either list, and the WTF ones that do take
`WTF::String` (a refcounted 8/16-bit `StringImpl*`) by reference. So the `WRAPPER` verdict -- the
high-value one -- almost never applies here: **1 of 120** live downcalls is a true `WRAPPER`.

Counts over the 120 live downcalls in scope: **112 `OS-CALL`**, **1 `WRAPPER`**, **4 `PURE`**
(3 clear the parity gate as `PARITY: exact`, 1 stays native because the state it writes lives inside
the library), **0 `PURE-HOT`**, and **3 orphan exports** with no Java declaration anywhere in the
tree. Outside the 120 there are **2 dead files** (208 lines) that are in no build list. Nothing in
this slice is `PARITY: unprovable` -- there is no rasteriser, codec or shaper here whose C output is
the de facto spec.

Recommendation in one line: **migrate this library, but land a deletion commit first** removing
~390 lines of C++ and 3 Java `native` declarations, so the `wkj_*` facade is never designed around
code that should not exist. The mechanical part (112 `OS-CALL` downcalls) is easy; the real work is
the **62 live upcall sites / ~56 distinct Java target methods** across seven client classes -- all
of which run on a single thread (the FX application thread), which makes this callback-table
conversion markedly safer than Glass or media.

---

## 2. Summary counts

| Metric | Count | Where measured |
|---|---:|---|
| `JNIEXPORT` in scope (all files, incl. dead) | 128 | `grep -c JNIEXPORT` over the scope file set |
| -- `Source/WebKitLegacy/java/**` (non-DOM) | 116 | |
| -- `Tools/DumpRenderTree/java/**` | 12 | 10 natives + `JNI_OnLoad` + `JNI_OnUnLoad` |
| -- `Tools/TestRunnerShared/java/**` | 0 | `CMakeLists.txt` only |
| **Live downcalls** (compiled and in a build list) | **120** | 110 WebKitLegacy + 10 DRT |
| Java `native` declarations in scope | 121 | 87 `WebPage`, 18 `BackForwardList`, 2 `PageCache`, 1 `ContextMenu`, 2 `PopupMenu`, 1 `ColorChooser`, 1 `WCWidget`, 4 `WCPluginWidget`, 2 `MainThread`, 1 `Timer`, 10 `DumpRenderTree`. `WebEngine`, `WebView`, `Disposer`, `UIClientImpl` declare **none**. |
| Upcall sites (`Call*Method`), all files | 77 | 5 in dead files, **72 live** |
| -- live, WebKitLegacy | 62 | plus `notifyHistoryItemDestroyed`, entered *into* scope from `Source/WebCore/history/HistoryItem.cpp:78` |
| -- live, DRT | 11 | 2 of them (`EventSender.cpp:157`, `:487`) fan out to 20 distinct Java methods |
| Distinct Java upcall targets | ~56 methods + 4 field reads + 1 ctor | see section 6 |
| `GetMethodID`/`GetStaticMethodID` | 93 | |
| `FindClass` by name | 15 | 10 distinct classes |
| `GetFieldID` | 4 | all `WCRectangle.{x,y,w,h}`, `ChromeClientJava.cpp:187-193` |
| Global refs | 34 declarations | 1 explicit `NewGlobalRef`/`DeleteGlobalRef` pair (`Tools/DumpRenderTree/java/JavaEnv.cpp:56,119`); the rest are RAII `JGlobalRef` (`Source/WTF/wtf/java/JavaRef.h:132-172`), paired by construction |
| `Get*ArrayCritical` | 3 | `WebPage.cpp:1693,1727,2065` -- all write-only fills of a freshly allocated array |
| `GetStringCritical` | 2 explicit | `Tools/DumpRenderTree/java/TestRunnerJava.cpp:48,50`; plus every `String(env, jstring)` via `Source/WTF/wtf/java/StringJava.cpp:42` (21 call sites in scope) |
| `Get*ArrayElements` | 3 | `WebPage.cpp:1073` (release mode `0` = copy back), `WebPage.cpp:2012` (`JNI_ABORT`), `SocketStreamHandleImplJava.cpp:179` (`JNI_ABORT`, dead file) |
| `GetStringUTFChars` (modified UTF-8) | 3 | `WebPage.cpp:1219`, `Tools/DumpRenderTree/java/DumpRenderTree.cpp:60,61` |
| `AttachCurrentThread` | **0 in scope** | the template is `Source/WTF/wtf/java/JavaEnv.h:83-114`; no in-scope file instantiates it |
| `GetJavaVM` | 0 in scope | `jvm` is a global set by `JNI_OnLoad` (`Source/WTF/wtf/java/JavaEnv.cpp:119`, out of scope; `Tools/DumpRenderTree/java/JavaEnv.cpp:104`, in scope) |
| `ThrowNew` | **0** | no exception is ever thrown from C++ in this slice |
| `ExceptionCheck`/`Describe`/`Clear` | 6 | all in `Tools/DumpRenderTree/java/JavaEnv.cpp`; the WebKitLegacy half uses `WTF::CheckAndClearException` (80 calls), which **swallows** |
| `JNI_OnLoad` in scope | 1 | `Tools/DumpRenderTree/java/JavaEnv.cpp:104`, plus `JNI_OnUnLoad` at `:116` |

Every number above came from a `grep -c` / `grep -o | wc -l` over the explicit scope file set
(`find Source/WebKitLegacy/java -not -path "*/DOM/*" -type f -name '*.cpp' -o -name '*.h'`, plus the
two `Tools/*/java` directories). The "~56 distinct upcall targets" figure is the only approximate
one; the exact list is in section 6.

---

## 3. Java classes and C files

### 3.1 Java classes

| Java class | natives | static / instance | notes |
|---|---:|---|---|
| `com.sun.webkit.WebPage` | 87 | 3 static (`twkInitWebCore`, `twkWorkerThreadCount`, `twkDoJSCGarbageCollection`), 84 instance | `WebPage.java:129-166` -- `static{}` calls `NativeLibLoader.loadLibrary("jfxwebkit")` then `twkInitWebCore(...)`, and registers a shutdown hook calling `MainThread.twkSetShutdown(true)`. Handle field `private long pPage` (set by `twkCreatePage`, `WebPage.java:201`); frame handles are bare `long` in `Set<Long> frames` (`WebPage.java:101`). 2 of the 87 (`twkGetDocument`, `twkGetOwnerElement`, `WebPage.java:2613-2614`) are implemented **outside this scope**, at `Source/WebCore/bindings/java/JavaDOMUtils.cpp:107,122`. |
| `com.sun.webkit.BackForwardList` | 18 | all static | `BackForwardList.java:315-333`. Handles `long pitem` / `long ppage` live on the inner `Entry`, which is **constructed by native code** (`BackForwardList.cpp:107`). |
| `com.sun.webkit.PageCache` | 2 | static | `PageCache.java:62-63` |
| `com.sun.webkit.PopupMenu` | 2 | instance | `PopupMenu.java:79-80`; `long pdata` = `PopupMenuJava*` |
| `com.sun.webkit.ColorChooser` | 1 | instance | `ColorChooser.java:82`; `long data` = `ColorChooserJava*` |
| `com.sun.webkit.ContextMenu` | 1 | instance | `ContextMenu.java:76`; implemented **out of scope** at `Source/WebCore/platform/java/ContextMenuJava.cpp:241` |
| `com.sun.webkit.WCWidget` | 1 | static `initIDs()` | `WCWidget.java:122`; out of scope at `Source/WebCore/platform/java/WidgetJava.cpp:225`. With `WCPluginWidget` these are the **only `_initIDs`-shaped natives** in javafx.web. |
| `com.sun.webkit.WCPluginWidget` | 4 | 1 static `initIDs()`, 3 instance | `WCPluginWidget.java:48,157,159,201`; out of scope at `Source/WebCore/platform/java/PluginWidgetJava.cpp:63,108,116,124` |
| `com.sun.webkit.MainThread` | 2 | static | `MainThread.java:39-40`; out of scope at `Source/WTF/wtf/java/MainThreadJava.cpp:117,128` |
| `com.sun.webkit.Timer` | 1 | static | `Timer.java:101`; out of scope at `Source/WebCore/platform/java/MainThreadSharedTimerJava.cpp:76` |
| `com.sun.javafx.webkit.drt.DumpRenderTree` | 10 | all static | `DumpRenderTree.java:388-398,597`. Loads its library with plain `System.loadLibrary("DumpRenderTreeJava")` (`DumpRenderTree.java:208`), **not** `NativeLibLoader`. |
| `com.sun.webkit.Disposer` | 0 | -- | pure Java `ReferenceQueue`/`DisposerRecord`. Nothing to migrate; it is the *lifetime mechanism* the migration must preserve. |
| `com.sun.javafx.webkit.UIClientImpl` | 0 | -- | pure Java; reached only *through* the `WebPage.fwk*` upcalls in section 6 |
| `javafx.scene.web.WebEngine` | 0 | -- | **no `native` methods.** grep -n native returns only comment hits at `:324,:376,:891`. The `jfx-web-native` skill claim of "2 each" for `WebEngine`/`WebView` is wrong for this tree. |
| `javafx.scene.web.WebView` | 0 | -- | same; `:1250` is a comment |

### 3.2 C files

Live = named in `Source/WebKitLegacy/PlatformJava.cmake:119-135` or
`Tools/DumpRenderTree/java/CMakeLists.txt:8-20`.

| File | Live | `JNIEXPORT` | Upcall sites | Global refs held |
|---|---|---:|---:|---|
| `Source/WebKitLegacy/java/WebCoreSupport/WebPage.cpp` | yes | 89 (88 JNI + 1 plain C) | 3 | -- |
| `.../BackForwardList.cpp` | yes | 18 | 3 (+1 entered from WebCore) | `BackForwardList::m_hostObject` (`BackForwardList.h:90`), 2 `JGClass` |
| `.../ChromeClientJava.cpp` | yes | 0 | **28** | `m_webPage` (`ChromeClientJava.h:190`), 3 `JGClass` |
| `.../FrameLoaderClientJava.cpp` | yes | 0 | 13 | `m_webPage` (`FrameLoaderClientJava.h:200`) **per frame**, 2 `JGClass` |
| `.../EditorClientJava.cpp` | yes | 0 | 1 | `m_webPage` (`EditorClientJava.h:162`) |
| `.../InspectorClientJava.cpp` | yes | 0 | 2 | `m_webPage` (`InspectorClientJava.h:54`), 1 `JGClass` |
| `.../ProgressTrackerClientJava.cpp` | yes | 0 | 1 | `m_webPage` (`ProgressTrackerClientJava.h:45`), 1 `JGClass` |
| `.../DragClientJava.cpp` | yes | 0 | 1 | `m_webPage` (`DragClientJava.h:53`), 2 `JGClass` |
| `.../ContextMenuClientJava.cpp` | yes | 0 | **0** | `m_webPage` (`ContextMenuClientJava.h:48`) -- **stored and never read**; see section 9 |
| `.../PopupMenuJava.cpp` | yes | 2 | 6 | `m_popup` (`PopupMenuJava.h:55`), 1 `JGClass` |
| `.../ColorChooserJava.cpp` | yes | 1 | 3 | `m_colorChooserRef` (`ColorChooserJava.h:54`) |
| `.../PageCacheJava.cpp` | yes | 2 | 0 | -- |
| `.../SearchPopupMenuJava.cpp` | yes | 0 | 0 | -- (delegates to `PopupMenuJava`) |
| `.../VisitedLinkStoreJava.cpp` | yes | 0 | 0 | JNI-free |
| `.../PlatformStrategiesJava.cpp` | yes | 0 | 0 | JNI-free |
| `.../WebKitPrefix.cpp` | yes (PCH) | 0 | 0 | JNI-free |
| `.../NotificationClientJava.h`, `.../FrameNetworkingContextJava.h` | headers | 0 | 0 | JNI-free |
| `Source/WebKitLegacy/java/storage/WebDatabaseProviderJava.cpp` | yes | 0 | 0 | JNI-free; 6 lines of body |
| `.../SocketStreamHandleImplJava.cpp` | **NO** | 4 | 4 | 1 `JGClass` -- **dead duplicate**, section 9 |
| `.../HistoryItemClientJava.cpp` and `.h` | **NO** | 0 | 1 | **dead**, section 9 |
| `Tools/DumpRenderTree/java/DumpRenderTree.cpp` | yes | 10 | 0 | 1 `JGClass` (`java/lang/String`) |
| `Tools/DumpRenderTree/java/JavaEnv.cpp` | yes | 2 (`JNI_OnLoad`, `JNI_OnUnLoad`) | 0 | 1 explicit global-ref pair + 8 cached static `jmethodID` |
| `Tools/DumpRenderTree/java/TestRunnerJava.cpp` | yes | 0 | 7 | -- |
| `Tools/DumpRenderTree/java/EventSender.cpp` | yes | 0 | 2 sites / 20 methods | 2 `JGClass`, per-object `JGObject` in `JSObjectGetPrivate` (`:57,:622`) |
| `Tools/DumpRenderTree/java/WorkQueueItemJava.cpp` | yes | 0 | 2 | -- |
| `Tools/DumpRenderTree/java/GCControllerJava.cpp` | yes | 0 | 0 | calls the plain-C `WebPage_doJSCGarbageCollection()` (`:30,:34`) |
| `Tools/DumpRenderTree/java/UIScriptControllerJava.cpp` | yes | 0 | 0 | JNI-free |
| `Tools/TestRunnerShared/java/CMakeLists.txt` | yes | 0 | 0 | **no JNI at all** -- only `${JAVA_INCLUDE_PATH}` / `${JAVA_INCLUDE_PATH2}` (`:37-38`) and `${JAVA_JVM_LIBRARY}` (`:19`) to remove at the end |

---

## 4. External dependencies

`jfxwebkit` is not built from source in this fork (`WEBKIT-MEDIA-STUBS.md`); the evidence below comes
from the shipped `../caches/sdk/bin/jfxwebkit.dll` (100 MB, dated 2026-08-21) plus the build files.

**Declared link inputs.** `Source/WebKitLegacy/PlatformJava.cmake:139-144` --
`WebKit::WebCoreTestSupport`, `${ICU_I18N_LIBRARIES}`, `${ICU_DATA_LIBRARIES}`, `${ICU_LIBRARIES}`;
all in-tree or static. Output name `jfxwebkit` (`:154`).
`Tools/DumpRenderTree/java/CMakeLists.txt:22-28` links `WebKitLegacy`, `TestRunnerShared`,
**`${JAVA_JVM_LIBRARY}`**, ICU.

**Imports actually present in the binary** (`dumpbin -dependents`):

    WINMM.dll  USER32.dll  ADVAPI32.dll  KERNEL32.dll  bcrypt.dll  WS2_32.dll
    MSVCP140.dll  MSVCP140_2.dll  VCRUNTIME140.dll  VCRUNTIME140_1.dll
    api-ms-win-crt-runtime / math / heap / string / environment / time /
    stdio / utility / filesystem / convert  -l1-1-0.dll

**`jvm.dll` is absent.** `jfxwebkit` reaches the JVM only through the `JNIEnv` function table it is
handed, so the FFM migration does not change its link line at all. `DumpRenderTreeJava` *does* link
`${JAVA_JVM_LIBRARY}`; that entry does come out at the end.

**Exports** (`dumpbin -exports`): 1956 `Java_com_sun_*`, of which **89** are
`Java_com_sun_webkit_WebPage_twk*` -- exactly the 87 compiled from `WebPage.cpp` plus the 2 from
`JavaDOMUtils.cpp`. That arithmetic independently confirms `twkProcessTouchEvent` is **not in the
shipped binary**. Also exported: `JNI_OnLoad` (ordinal 3324) and `WebPage_doJSCGarbageCollection`
(ordinal 3B83).

**Export control -- the sharpest edge in this migration.** On Linux the link line is
`-Xlinker -version-script=${WEBCORE_DIR}/mapfile-vers -Wl,--no-undefined`
(`Source/WebKitLegacy/PlatformJava.cmake:166`); on macOS,
`-exported_symbols_list ${WEBCORE_DIR}/mapfile-macosx` (`:163`). Both files enumerate **every**
`Java_com_sun_*` symbol by name (1857 and 1612 entries respectively). Windows has no export list and
relies on `JNIEXPORT` = `__declspec(dllexport)`.

> Every new `wkj_*` symbol must be reachable through `Source/WebCore/mapfile-vers` **and**
> `Source/WebCore/mapfile-macosx`, or it will link on Windows and silently fail to resolve on Linux
> and macOS. CI (`.github/workflows/submit.yml`, plain `mvn -B -ntp -fae install`) does not build
> WebKit and will not catch this.
>
> The parallel `ffm-migrator` session has already handled this with a glob -- `wkj_*;` at
> `mapfile-vers:6` and `_wkj_*` at `mapfile-macosx:4` -- which is the right answer and removes the
> per-symbol maintenance burden. **Two things still need verifying on a real build, because neither
> is guaranteed by inspection:** that GNU `ld` glob matching in a version script does not capture
> anything unintended, and that the `ld64` on the macOS toolchain this fork targets honours `*` in
> an `-exported_symbols_list` (older `ld64` warns on unmatched patterns and can fail the link).
> Confirm with `nm -D --defined-only libjfxwebkit.so` filtered for `wkj_` and the macOS equivalent
> before relying on it.

**What Java could bind directly instead of C++.** The only C-callable engine surface `jfxwebkit`
exports is the **JavaScriptCore C API** (`JSEvaluateScript`, `JSObjectSetProperty`,
`JSStringCreateWithCharacters`, `JSContextGroupSetExecutionTimeLimit`, `JSGarbageCollect`, about 100
functions at `mapfile-vers:39-129` and `:2000`), plus `WebPage_doJSCGarbageCollection`
(`mapfile-vers:142`, `mapfile-macosx:135`) and roughly 25 Itanium-mangled WTF/JSC/WebCoreTestSupport
symbols (`mapfile-vers:13-37,130-141`). **Nothing** from `WebCore::Page`, `Settings`, `FrameLoader`,
`Editor`, `LocalFrameView` or `BackForwardCache` is exported.

That is the named evidence behind every `OS-CALL` verdict in section 5. A one-line body such as
`page->settings().setUserAgent(String(env, userAgent))` (`WebPage.cpp:2511-2518`) *looks* like a
`WRAPPER`, but there is no exported, C-callable `Settings::setUserAgent` for Java to bind, and its
parameter is a `WTF::String&` -- a refcounted `StringImpl*` with an 8-bit/16-bit payload union that
Java cannot construct. The wrapper is load-bearing.

**Build-side gap worth flagging.** The C++ in scope includes generated constant headers --
`com_sun_webkit_WebPage.h`, `com_sun_webkit_event_WCKeyEvent.h`, `com_sun_webkit_event_WCFocusEvent.h`,
`com_sun_webkit_event_WCMouseEvent.h` (`WebPage.cpp:132-135`), `com_sun_webkit_LoadListenerClient.h`
(`FrameLoaderClientJava.cpp:57`), `com_sun_webkit_PageCache.h`, `com_sun_webkit_PopupMenu.h`,
`com_sun_webkit_network_SocketStreamHandle.h`. **Nothing in this repository generates them:**
`modules/javafx.web/pom.xml` has no `-h` compiler argument and no header-generation execution
(unlike `modules/javafx.graphics/pom.xml`), and no `*.cmake`/`CMakeLists.txt` under
`src/main/native` runs `javac -h`. A from-source WebKit build here would need that step restored.
For the FFM migration this is an *opportunity*: those constants (`WCKeyEvent.VK_*`,
`LoadListenerClient.PAGE_STARTED`, `WebPage.DND_DST_*`) should become plain C `#define`s or an enum
in `webkit_java_api.h`, removing the last generated-JNI-header dependency from the C++ side.

---

## 5. Triage and proposed flat C ABI

### 5.0 ABI conventions used by every prototype below

Naming follows the `jfx-web-native` skill: `wkj_*`, in
`Source/WebKitLegacy/java/api/webkit_java_api.h`. Types are `<stdint.h>`; no `JNIEnv`, `jobject`,
`jclass` or `jni.h` appears anywhere.

    /* opaque handles -- the current jlong peer fields */
    typedef struct WKJPage  WKJPage;   /* WebCore::WebPage*      (WebPage.java pPage)          */
    typedef struct WKJFrame WKJFrame;  /* WebCore::Frame*        (WebPage.java frames set)     */
    typedef struct WKJItem  WKJItem;   /* WebCore::HistoryItem*  (BackForwardList.Entry.pitem) */
    typedef struct WKJPopup WKJPopup;  /* WebCore::PopupMenuJava*                              */
    typedef struct WKJColorChooser WKJColorChooser;
    typedef struct WKJRenderQueue  WKJRenderQueue; /* registry id for a Java WCRenderQueue     */

    /* booleans: FFM has no boolean layout. int32_t 0/1 everywhere, matched by JAVA_INT. */

    /* strings IN: UTF-16, len < 0 means Java null. This preserves the current exact semantics:
       String(env, jstring) reads UTF-16 via GetStringCritical
       (Source/WTF/wtf/java/StringJava.cpp:42) -- it is NOT modified UTF-8.
       Java side: arena.allocateFrom(JAVA_CHAR, s.toCharArray()). */
    typedef struct WKJStr { const uint16_t* data; int32_t length; } WKJStr;

    /* strings OUT: retained handle plus explicit release. A returned length < 0 means Java null;
       length == 0 means the empty string. That distinction is load-bearing -- see section 11. */
    typedef struct WKJString { const uint16_t* data; int32_t length; void* owner; } WKJString;
    JFX_EXPORT void wkj_string_release(WKJString s);

    /* JFX_EXPORT: __declspec(dllexport) on MSVC,
       __attribute__((visibility("default"))) elsewhere, AND an entry in
       Source/WebCore/mapfile-vers + mapfile-macosx (see section 4). */

Verdict legend: `OS-CALL` reaches a WebCore/JavaScriptCore entry point that `jfxwebkit` does not
export in C-callable form (evidence: section 4). `WRAPPER` marshals around an already-exported
plain-C symbol. `PURE` touches no external symbol. `DEAD` has no caller or is not compiled.

### 5.1 com.sun.webkit.WebPage -- 87 Java natives, 88 exports in WebPage.cpp

All 85 `OS-CALL` rows share one evidence line, not repeated per row:
**they call `WebCore::` / `JSC::` C++ members (`Page`, `LocalFrame`, `FrameLoader`, `Editor`,
`EventHandler`, `Settings`, `LocalFrameView`, `PrintContext`, `DragController`,
`PageInspectorController`, `WorkerThread`), and none of those is exported by `jfxwebkit` --
`Source/WebCore/mapfile-vers` has 138 non-JNI entries, all JSC C-API, WTF, or JSC-mangled.**
Non-`OS-CALL` rows carry their own evidence in the last column.

Java declarations are in `com/sun/webkit/WebPage.java`; C++ in
`Source/WebKitLegacy/java/WebCoreSupport/WebPage.cpp`. `Z`=boolean, `J`=long, `I`=int, `F`=float,
`D`=double. Handles are `WKJPage*` for a page and `WKJFrame*` for a frame.

| C symbol (prefix `Java_com_sun_webkit_WebPage_`) | C line | Java line | JNI signature | Verdict | Proposed C ABI / evidence |
|---|---:|---:|---|---|---|
| `twkInitWebCore` | 892 | 2597 | `(ZZZ)V` | **PURE**, PARITY exact, **not deletable** | `void wkj_set_startup_options(int32_t useJIT, int32_t useDFGJIT, int32_t useCSS3D)`. Body writes three file-static bools (`WebPage.cpp:884-886`) read later by `twkCreatePage`; no external symbol. Java cannot hold library-internal state, so it stays native. |
| `twkCreatePage` | 899 | 2598 | `(Z)J` | OS-CALL | `WKJPage* wkj_page_create(int32_t editable, const WKJPageCallbacks* cb, uint64_t user)` |
| `twkInit` | 966 | 2599 | `(JZF)V` | OS-CALL | `void wkj_page_init(WKJPage*, int32_t usePlugins, float devicePixelScale)` |
| `twkDestroyPage` | 1014 | 2600 | `(J)V` | OS-CALL | `void wkj_page_destroy(WKJPage*)` |
| `twkGetMainFrame` | 1031 | 2602 | `(J)J` | OS-CALL | `WKJFrame* wkj_page_main_frame(WKJPage*)` |
| `twkGetParentFrame` | 1046 | 2603 | `(J)J` | OS-CALL | `WKJFrame* wkj_frame_parent(WKJFrame*)` |
| `twkGetChildFrames` | 1061 | 2604 | `(J)[J` | OS-CALL | `int32_t wkj_frame_children(WKJFrame*, WKJFrame** out, int32_t cap)` returning the count; `cap==0` to size. **Fixes two latent bugs, section 11.** |
| `twkGetName` | 1086 | 2606 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_name(WKJFrame*)` |
| `twkGetURL` | 1097 | 2607 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_url(WKJFrame*)` |
| `twkGetInnerText` | 1112 | 2608 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_inner_text(WKJFrame*)` |
| `twkGetRenderTree` | 1139 | 2609 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_render_tree(WKJFrame*)` |
| `twkGetContentType` | 1156 | 2610 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_content_type(WKJFrame*)` |
| `twkGetTitle` | 1167 | 2611 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_title(WKJFrame*)` |
| `twkGetIconURL` | 1178 | 2612 | `(J)Ljava/lang/String;` | **PURE**, PARITY exact -- **DELETE** | none. `ENABLE(ICONDATABASE)` is never defined for this port (absent from `Source/cmake/OptionsJava.cmake` and `Source/cmake/WebKitFeatures.cmake`), so the body reduces to `return 0;` for every input, and `WebPage.getIcon()` (`WebPage.java:1229-1249`) always returns `null`. |
| `twkGetDocument` | JavaDOMUtils.cpp:107 | 2613 | `(J)Lorg/w3c/dom/Document;` | out of scope | DOM slice |
| `twkGetOwnerElement` | JavaDOMUtils.cpp:122 | 2614 | `(J)Lorg/w3c/dom/Element;` | out of scope | DOM slice |
| `twkOpen` | 1193 | 2616 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_frame_open(WKJFrame*, WKJStr url)` |
| `twkOverridePreference` | 1362 | 2617 | `(JLjava/lang/String;Ljava/lang/String;)V` | OS-CALL | `void wkj_page_override_preference(WKJPage*, WKJStr key, WKJStr value)` |
| `twkResetToConsistentStateBeforeTesting` | 1454 | 2618 | `(J)V` | OS-CALL | `void wkj_page_reset_for_testing(WKJPage*)` |
| `twkLoad` | 1210 | 2619 | `(JLjava/lang/String;Ljava/lang/String;)V` | OS-CALL | `void wkj_frame_load(WKJFrame*, const uint8_t* utf8, int32_t len, WKJStr contentType)`. **Encoding hazard, section 11.** |
| `twkIsLoading` | 1240 | 2620 | `(J)Z` | OS-CALL | `int32_t wkj_frame_is_loading(WKJFrame*)` |
| `twkStop` | 1249 | 2621 | `(J)V` | OS-CALL | `void wkj_frame_stop(WKJFrame*)` |
| `twkStopAll` | 1261 | 2622 | `(J)V` | OS-CALL | `void wkj_page_stop_all(WKJPage*)` |
| `twkRefresh` | 1273 | 2623 | `(J)V` | OS-CALL | `void wkj_frame_refresh(WKJFrame*)` |
| `twkGoBackForward` | 1285 | 2625 | `(JI)Z` | OS-CALL | `int32_t wkj_page_go_back_forward(WKJPage*, int32_t distance)` |
| `twkCopy` | 1301 | 2627 | `(J)Z` | OS-CALL | `int32_t wkj_frame_copy(WKJFrame*)` |
| `twkFindInPage` | 1319 | 2628 | `(JLjava/lang/String;ZZZ)Z` | OS-CALL | `int32_t wkj_page_find(WKJPage*, WKJStr, int32_t fwd, int32_t wrap, int32_t matchCase)` |
| `twkFindInFrame` | 1340 | 2631 | `(JLjava/lang/String;ZZZ)Z` | OS-CALL | `int32_t wkj_frame_find(WKJFrame*, WKJStr, int32_t fwd, int32_t wrap, int32_t matchCase)` |
| `twkGetZoomFactor` | 1528 | 2635 | `(JZ)F` | OS-CALL | `float wkj_frame_get_zoom(WKJFrame*, int32_t textOnly)` |
| `twkSetZoomFactor` | 1542 | 2636 | `(JFZ)V` | OS-CALL | `void wkj_frame_set_zoom(WKJFrame*, float, int32_t textOnly)` |
| `twkExecuteScript` | 1558 | 2638 | `(JLjava/lang/String;)Ljava/lang/Object;` | OS-CALL | LiveConnect; `int32_t wkj_frame_execute_script(WKJFrame*, WKJStr, WKJSValue* out)` -- see section 7.4 |
| `twkAddJavaScriptBinding` | 1576 | **none** | -- | **DEAD orphan export** -- **DELETE** | none. No Java `native` declaration in any source root (`src/main`, `src/test`, `src/shims`, `src/android`, `src/ios`); present in the shipped DLL at ordinal 33E9 and in both mapfiles only because those are hand-maintained. 28 lines, and one of only two `WebPage.cpp` functions touching the `bridge/jni` LiveConnect reflection layer. |
| `twkReset` | 1605 | 2640 | `(J)V` | OS-CALL | `void wkj_frame_clear_name(WKJFrame*)` |
| `twkGetFrameHeight` | 1638 | 2642 | `(J)I` | OS-CALL | `int32_t wkj_frame_height(WKJFrame*)` |
| `twkBeginPrinting` | 1617 | 2643 | `(JFF)I` | OS-CALL | `int32_t wkj_page_begin_printing(WKJPage*, float w, float h)` |
| `twkEndPrinting` | 1623 | 2644 | `(J)V` | OS-CALL | `void wkj_page_end_printing(WKJPage*)` |
| `twkPrint` | 1629 | 2645 | `(JLcom/sun/webkit/graphics/WCRenderQueue;IF)V` | OS-CALL | `void wkj_page_print(WKJPage*, WKJRenderQueue*, int32_t pageIndex, float width)`. Blocked on the graphics slice, section 11. |
| `twkAdjustFrameHeight` | 1659 | 2646 | `(JFFF)F` | OS-CALL | `float wkj_frame_adjust_height(WKJFrame*, float oldTop, float oldBottom, float bottomLimit)` |
| `twkGetVisibleRect` | 1680 | 2648 | `(J)[I` | OS-CALL | `int32_t wkj_frame_visible_rect(WKJFrame*, int32_t out_xywh[4])`, 0 if no view |
| `twkScrollToPosition` | 1703 | 2649 | `(JII)V` | OS-CALL | `void wkj_frame_scroll_to(WKJFrame*, int32_t x, int32_t y)` |
| `twkGetContentSize` | 1714 | 2650 | `(J)[I` | OS-CALL | `int32_t wkj_frame_content_size(WKJFrame*, int32_t out_wh[2])` |
| `twkSetTransparent` | 1735 | 2651 | `(JZ)V` | OS-CALL | `void wkj_frame_set_transparent(WKJFrame*, int32_t)` |
| `twkSetBackgroundColor` | 1746 | 2652 | `(JI)V` | OS-CALL | `void wkj_frame_set_background_color(WKJFrame*, uint32_t rgba)` |
| `twkSetBounds` | 1674 | 2654 | `(JIIII)V` | OS-CALL | `void wkj_page_set_bounds(WKJPage*, int32_t x, int32_t y, int32_t w, int32_t h)` |
| `twkPrePaint` | 1757 | 2655 | `(J)V` | OS-CALL | `void wkj_page_pre_paint(WKJPage*)` |
| `twkUpdateContent` | 1763 | 2656 | `(JLcom/sun/webkit/graphics/WCRenderQueue;IIII)V` | OS-CALL | `void wkj_page_update_content(WKJPage*, WKJRenderQueue*, int32_t x, int32_t y, int32_t w, int32_t h)` |
| `twkUpdateRendering` | 1769 | 2657 | `(J)V` | OS-CALL | `void wkj_page_update_rendering(WKJPage*)` |
| `twkPostPaint` | 1775 | 2658 | `(JLcom/sun/webkit/graphics/WCRenderQueue;IIII)V` | OS-CALL | `void wkj_page_post_paint(WKJPage*, WKJRenderQueue*, int32_t x, int32_t y, int32_t w, int32_t h)` |
| `twkGetEncoding` | 1781 | 2661 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_page_get_encoding(WKJPage*)` |
| `twkSetEncoding` | 1794 | 2662 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_page_set_encoding(WKJPage*, WKJStr)` |
| `twkProcessFocusEvent` | 1806 | 2664 | `(JII)V` | OS-CALL | `void wkj_page_focus_event(WKJPage*, int32_t id, int32_t direction)` |
| `twkProcessKeyEvent` | 1844 | 2665 | `(JILjava/lang/String;Ljava/lang/String;IZZZZD)Z` | OS-CALL | `int32_t wkj_page_key_event(WKJPage*, int32_t type, WKJStr text, WKJStr keyId, int32_t vk, int32_t modifiers, double timestamp)`, packing the four booleans into a bitmask |
| `twkProcessMouseEvent` | 1858 | 2670 | `(JIIIIIIIIZZZZZD)Z` | OS-CALL | `int32_t wkj_page_mouse_event(WKJPage*, const WKJMouseEvent*)` -- 15 arguments today; use a struct pointer |
| `twkProcessMouseWheelEvent` | 1938 | 2675 | `(JIIIIFFZZZZD)Z` | OS-CALL | `int32_t wkj_page_wheel_event(WKJPage*, const WKJWheelEvent*)` |
| `twkProcessTouchEvent` | 1965 | **none** | -- | **DEAD, not compiled** -- **DELETE** | none. Guarded by `#if ENABLE(TOUCH_EVENTS)`; `Source/cmake/OptionsJava.cmake:82` sets it `OFF` for this port; no Java declaration exists; and the symbol is absent from the shipped DLL (89 `WebPage_twk*` exports = 87 compiled here + 2 from `JavaDOMUtils.cpp`). 19 lines. |
| `twkProcessInputTextChange` | 1984 | 2680 | `(JLjava/lang/String;Ljava/lang/String;[II)Z` | OS-CALL | `int32_t wkj_page_input_text_change(WKJPage*, WKJStr committed, WKJStr composed, const int32_t* attrs, int32_t attrCount, int32_t caret)` |
| `twkProcessCaretPositionChange` | 2029 | 2682 | `(JI)Z` | OS-CALL | `int32_t wkj_page_caret_position_change(WKJPage*, int32_t caret)` |
| `twkGetTextLocation` | 2051 | 2683 | `(JI)[I` | OS-CALL | `int32_t wkj_page_text_location(WKJPage*, int32_t charIndex, int32_t out_xywh[4])` |
| `twkGetLocationOffset` | 2076 | **none** | -- | **DEAD orphan export** -- **DELETE** | none. No Java `native` declaration in any source root. Its apparent Java caller `WebPage.getClientLocationOffset(x,y)` (`WebPage.java:960-973`) **ignores x and y and calls `twkGetInsertPositionOffset` instead** -- a separate IME defect, not to be fixed in a migration commit. 36 lines. |
| `twkGetInsertPositionOffset` | 2113 | 2684 | `(J)I` | OS-CALL | `int32_t wkj_page_insert_position_offset(WKJPage*)` |
| `twkGetCommittedTextLength` | 2141 | 2685 | `(J)I` | OS-CALL | `int32_t wkj_page_committed_text_length(WKJPage*)` |
| `twkGetCommittedText` | 2167 | 2686 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_page_committed_text(WKJPage*)` |
| `twkGetSelectedText` | 2203 | 2687 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_page_selected_text(WKJPage*)` |
| `twkProcessDrag` | 2251 | 2689 | `(JI[Ljava/lang/String;[Ljava/lang/String;IIIII)I` | OS-CALL | `int32_t wkj_page_process_drag(WKJPage*, int32_t actionId, const WKJStr* mimes, const WKJStr* values, int32_t n, int32_t x, int32_t y, int32_t sx, int32_t sy, int32_t javaAction)`; `n < 0` selects the source (non-target) branch, replacing the current `jMimes == NULL` test |
| `twkExecuteCommand` | 2340 | 2696 | `(JLjava/lang/String;Ljava/lang/String;)Z` | OS-CALL | `int32_t wkj_page_execute_command(WKJPage*, WKJStr cmd, WKJStr value)` |
| `twkQueryCommandEnabled` | 2353 | 2698 | `(JLjava/lang/String;)Z` | OS-CALL | `int32_t wkj_page_query_command_enabled(WKJPage*, WKJStr)` |
| `twkQueryCommandState` | 2366 | 2699 | `(JLjava/lang/String;)Z` | OS-CALL | `int32_t wkj_page_query_command_state(WKJPage*, WKJStr)` |
| `twkQueryCommandValue` | 2379 | 2700 | `(JLjava/lang/String;)Ljava/lang/String;` | OS-CALL | `WKJString wkj_page_query_command_value(WKJPage*, WKJStr)` |
| `twkIsEditable` | 2392 | 2701 | `(J)Z` | OS-CALL | `int32_t wkj_page_is_editable(WKJPage*)` |
| `twkSetEditable` | 2403 | 2702 | `(JZ)V` | OS-CALL | `void wkj_page_set_editable(WKJPage*, int32_t)` |
| `twkGetHtml` | 2414 | 2703 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_frame_html(WKJFrame*)` |
| `twkGetUsePageCache` | 2437 | 2705 | `(J)Z` | OS-CALL | `int32_t wkj_page_get_use_page_cache(WKJPage*)` |
| `twkSetUsePageCache` | 2446 | 2706 | `(JZ)V` | OS-CALL | `void wkj_page_set_use_page_cache(WKJPage*, int32_t)` |
| `twkGetDeveloperExtrasEnabled` | 2543 | 2707 | `(J)Z` | OS-CALL | `int32_t wkj_page_get_developer_extras(WKJPage*)` |
| `twkSetDeveloperExtrasEnabled` | 2552 | 2708 | `(JZ)V` | OS-CALL | `void wkj_page_set_developer_extras(WKJPage*, int32_t)` |
| `twkIsJavaScriptEnabled` | 2455 | 2710 | `(J)Z` | OS-CALL | `int32_t wkj_page_is_script_enabled(WKJPage*)` |
| `twkSetJavaScriptEnabled` | 2466 | 2711 | `(JZ)V` | OS-CALL | `void wkj_page_set_script_enabled(WKJPage*, int32_t)` |
| `twkIsContextMenuEnabled` | 2475 | 2712 | `(J)Z` | OS-CALL | `int32_t wkj_page_is_context_menu_enabled(WKJPage*)` |
| `twkSetContextMenuEnabled` | 2484 | 2713 | `(JZ)V` | OS-CALL | `void wkj_page_set_context_menu_enabled(WKJPage*, int32_t)` |
| `twkSetUserStyleSheetLocation` | 2493 | 2714 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_page_set_user_stylesheet(WKJPage*, WKJStr url)` |
| `twkGetUserAgent` | 2502 | 2715 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_page_get_user_agent(WKJPage*)` |
| `twkSetUserAgent` | 2511 | 2716 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_page_set_user_agent(WKJPage*, WKJStr)` |
| `twkSetLocalStorageDatabasePath` | 2520 | 2717 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_page_set_local_storage_path(WKJPage*, WKJStr)` |
| `twkSetLocalStorageEnabled` | 2533 | 2718 | `(JZ)V` | OS-CALL | `void wkj_page_set_local_storage_enabled(WKJPage*, int32_t)` |
| `twkGetUnloadEventListenersCount` | 2561 | 2720 | `(J)I` | OS-CALL | `int32_t wkj_frame_unload_listener_count(WKJFrame*)` |
| `twkConnectInspectorFrontend` | 2573 | 2722 | `(J)V` | OS-CALL | `void wkj_page_inspector_connect(WKJPage*)` |
| `twkDisconnectInspectorFrontend` | 2588 | 2723 | `(J)V` | OS-CALL | `void wkj_page_inspector_disconnect(WKJPage*)` |
| `twkDispatchInspectorMessageFromFrontend` | 2605 | 2724 | `(JLjava/lang/String;)V` | OS-CALL | `void wkj_page_inspector_dispatch(WKJPage*, WKJStr message)` |
| `twkWorkerThreadCount` | 2618 | 2524 | `()I` static | OS-CALL | `int32_t wkj_worker_thread_count(void)`. `WorkerThread::workerThreadCount()` is not exported. |
| `twkDoJSCGarbageCollection` | 2624 | 2726 | `()V` static | **WRAPPER** -- **DELETE** | none. The entire body is `WebPage_doJSCGarbageCollection();`, and that symbol is already an exported plain-C zero-argument function: defined at `WebPage.cpp:822`, exported at `Source/WebCore/mapfile-vers:142` and `Source/WebCore/mapfile-macosx:135`, present in the shipped DLL at ordinal 3B83. Java binds `WebPage_doJSCGarbageCollection` directly with `FunctionDescriptor.ofVoid()`. |
| `WebPage_doJSCGarbageCollection` (non-JNI) | 822 | -- | -- | OS-CALL, **already flat C** | keep unchanged; it is the FFM binding target above and is also called by `Tools/DumpRenderTree/java/GCControllerJava.cpp:34` |

### 5.2 com.sun.webkit.BackForwardList -- 18 natives, 18 exports

C++ in `Source/WebKitLegacy/java/WebCoreSupport/BackForwardList.cpp`; Java in
`com/sun/webkit/BackForwardList.java`. All static natives. Shared `OS-CALL` evidence: they call
`WebCore::BackForwardList`, `WebCore::HistoryItem`, `WebCore::Page::backForward()` and
`WebCore::BackForwardCache`, none of which is exported by `jfxwebkit` (section 4).

| C symbol (prefix `Java_com_sun_webkit_BackForwardList_`) | C line | Java line | JNI signature | Verdict | Proposed C ABI / evidence |
|---|---:|---:|---|---|---|
| `bflItemGetURL` | 172 | 315 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_bfl_item_url(WKJItem*)` |
| `bflItemGetTitle` | 180 | 316 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_bfl_item_title(WKJItem*)` |
| `bflItemGetIcon` | 189 | 317 | `(J)Lcom/sun/webkit/graphics/WCImage;` | **PURE**, PARITY exact -- **DELETE** | none. The whole body is commented out (`BackForwardList.cpp:191-201`); it is `return nullptr;` for every input. Java `Entry.getIcon()` (`BackForwardList.java:102-104`) always yields `null`. 15 lines. |
| `bflItemGetLastVisitedDate` | 206 | 318 | `(J)J` | **PURE**, PARITY exact -- **DELETE**, and the Java native too | none. Body is `return 0;` (`:208-211`). Worse, it is a **complete orphan on the Java side as well**: `Entry.getLastVisitedDate()` (`BackForwardList.java:110-112`) returns the `lastVisitedDate` field and never calls it; `grep -rn bflItemGetLastVisitedDate modules/javafx.web/src` finds only the declaration, the C definition and the two mapfile entries. 7 lines of C plus 1 Java declaration. |
| `bflItemIsTargetItem` | 215 | 319 | `(J)Z` | OS-CALL | `int32_t wkj_bfl_item_is_target(WKJItem*)` |
| `bflItemGetChildren` | 245 | 320 | `(JJ)[Lcom/sun/webkit/BackForwardList$Entry;` | OS-CALL | `int32_t wkj_bfl_item_children(WKJItem*, WKJItem** out, int32_t cap)`. **Today the C++ constructs the Java `Entry` objects itself** (`BackForwardList.cpp:248-253` via `createEntry`); under FFM C returns raw item handles and Java builds the `Entry` array, removing a `NewObject` and a `NewObjectArray`. |
| `bflItemGetTarget` | 222 | 321 | `(J)Ljava/lang/String;` | OS-CALL | `WKJString wkj_bfl_item_target(WKJItem*)` (returns null for an empty target -- preserve that, `:226-230`) |
| `bflClearBackForwardListForDRT` | 233 | 322 | `(J)V` | OS-CALL | `void wkj_bfl_clear_for_drt(WKJPage*)` |
| `bflSize` | 257 | 324 | `(J)I` | OS-CALL | `int32_t wkj_bfl_size(WKJPage*)` |
| `bflGetMaximumSize` | 263 | 325 | `(J)I` | OS-CALL | `int32_t wkj_bfl_get_capacity(WKJPage*)` |
| `bflSetMaximumSize` | 270 | 326 | `(JI)V` | OS-CALL | `void wkj_bfl_set_capacity(WKJPage*, int32_t)` |
| `bflGetCurrentIndex` | 277 | 327 | `(J)I` | OS-CALL | `int32_t wkj_bfl_current_index(WKJPage*)` |
| `bflIndexOf` | 324 | 328 | `(JJZ)I` | OS-CALL | `int32_t wkj_bfl_index_of(WKJPage*, WKJItem*, int32_t reverse)` |
| `bflSetEnabled` | 284 | 329 | `(JZ)V` | OS-CALL | `void wkj_bfl_set_enabled(WKJPage*, int32_t)` |
| `bflIsEnabled` | 291 | 330 | `(J)Z` | OS-CALL | `int32_t wkj_bfl_is_enabled(WKJPage*)` |
| `bflGet` | 298 | 331 | `(JI)Ljava/lang/Object;` | OS-CALL | `WKJItem* wkj_bfl_item_at(WKJPage*, int32_t index)`. Same change as `bflItemGetChildren`: the `hostObject` caching (`:304-308`) moves to a Java-side `Map<Long, Entry>` registry, which also removes the `JGObject HistoryItem::m_hostObject` global ref (`Source/WebCore/history/HistoryItem.h:298`). |
| `bflSetCurrentIndex` | 312 | 332 | `(JI)I` | OS-CALL | `int32_t wkj_bfl_set_current_index(WKJPage*, int32_t)` |
| `bflSetHostObject` | 340 | 333 | `(JLjava/lang/Object;)V` | OS-CALL, but **shape changes** | `void wkj_bfl_set_host(WKJPage*, uint64_t user)` -- store a registry id, not a global ref (`BackForwardList.cpp:343` currently does `bfl->setHostObject(JLObject(host, true))`) |

### 5.3 Small peer classes

| C symbol | File:line | Java native | JNI signature | Verdict | Proposed C ABI / evidence |
|---|---|---|---|---|---|
| `Java_com_sun_webkit_PageCache_twkGetCapacity` | `PageCacheJava.cpp:34` | `PageCache.java:62` | `()I` static | OS-CALL | `int32_t wkj_page_cache_get_capacity(void)`. Calls `WebCore::BackForwardCache::singleton().maxSize()` -- not exported. |
| `Java_com_sun_webkit_PageCache_twkSetCapacity` | `PageCacheJava.cpp:40` | `PageCache.java:63` | `(I)V` static | OS-CALL | `void wkj_page_cache_set_capacity(int32_t)` |
| `Java_com_sun_webkit_PopupMenu_twkSelectionCommited` | `PopupMenuJava.cpp:183` | `PopupMenu.java:79` | `(JI)V` | OS-CALL | `void wkj_popup_selection_committed(WKJPopup*, int32_t index)`. Calls `PopupMenuClient::valueChanged`. |
| `Java_com_sun_webkit_PopupMenu_twkPopupClosed` | `PopupMenuJava.cpp:199` | `PopupMenu.java:80` | `(J)V` | OS-CALL | `void wkj_popup_closed(WKJPopup*)` |
| `Java_com_sun_webkit_ColorChooser_twkSetSelectedColor` | `ColorChooserJava.cpp:111` | `ColorChooser.java:82` | `(JIII)V` | OS-CALL | `void wkj_color_chooser_set_selected(WKJColorChooser*, int32_t r, int32_t g, int32_t b)`. Calls `ColorChooserClient::didChooseColor`. Live: `ENABLE_INPUT_TYPE_COLOR` is `ON` for this port (`Source/cmake/OptionsJava.cmake:88`) and the symbol is in the shipped DLL. |

### 5.4 DumpRenderTree harness (separate library `DumpRenderTreeJava`)

C++ in `Tools/DumpRenderTree/java/`; Java in `com/sun/javafx/webkit/drt/DumpRenderTree.java`.

| C symbol (prefix `Java_com_sun_javafx_webkit_drt_DumpRenderTree_`) | C line | Java line | JNI signature | Verdict | Proposed C ABI / evidence |
|---|---:|---:|---|---|---|
| `initDRT` | `DumpRenderTree.cpp:49` | 388 | `()V` | OS-CALL | `void wkj_drt_init(void)`. Body calls three zero-argument functions that *are* exported (`_ZN3WTF26setPermissionsOfConfigPageEv`, `_ZN3WTF6Config25disableFreezingForTestingEv`, `_ZN3JSC6Config23enableRestrictedOptionsEv` -- `mapfile-vers:34,15,126`), so this is *nearly* a `WRAPPER`. It is ruled `OS-CALL` because those are C++ mangled names whose spelling differs between the Itanium ABI and MSVC, so a `SymbolLookup.find` from Java would not be portable. Adding `extern "C"` aliases would make it a genuine `WRAPPER`; not worth the churn for one call. |
| `initTest` | `DumpRenderTree.cpp:57` | 389 | `(Ljava/lang/String;Ljava/lang/String;)V` | OS-CALL | `void wkj_drt_init_test(WKJStr testPath, WKJStr pixelsHash)`. **Currently the only other modified-UTF-8 site**: `GetStringUTFChars` at `:60,61`. Switch to `WKJStr` (UTF-16) or standard UTF-8 and note it. |
| `didClearWindowObject` | `DumpRenderTree.cpp:74` | 390 | `(JJLcom/sun/javafx/webkit/drt/EventSender;)V` | OS-CALL | `void wkj_drt_did_clear_window_object(void* jsContext, void* jsWindowObject, const WKJEventSenderCallbacks* cb, uint64_t user)` -- the `EventSender` jobject becomes a callback table plus a registry id (section 8.8) |
| `dispose` | `DumpRenderTree.cpp:99` | 392 | `()V` | OS-CALL | `void wkj_drt_dispose(void)` (calls `JSC::waitForVMDestruction`) |
| `dumpAsText` | `DumpRenderTree.cpp:110` | 394 | `()Z` | OS-CALL | `int32_t wkj_drt_dump_as_text(void)`. Reads `gTestRunner` state written by JSC script callbacks; not `PURE`. |
| `dumpChildFramesAsText` | `DumpRenderTree.cpp:117` | 395 | `()Z` | OS-CALL | `int32_t wkj_drt_dump_child_frames_as_text(void)` |
| `didFinishLoad` | `DumpRenderTree.cpp:124` | 597 | `()Z` | OS-CALL | `int32_t wkj_drt_did_finish_load(void)` (runs `DRT::WorkQueue::processWork`, which upcalls) |
| `dumpBackForwardList` | `DumpRenderTree.cpp:131` | 396 | `()Z` | OS-CALL | `int32_t wkj_drt_dump_back_forward_list(void)` |
| `shouldStayOnPageAfterHandlingBeforeUnload` | `DumpRenderTree.cpp:138` | 397 | `()Z` | OS-CALL | `int32_t wkj_drt_should_stay_on_page(void)` |
| `openPanelFiles` | `DumpRenderTree.cpp:145` | 398 | `()[Ljava/lang/String;` | OS-CALL | `int32_t wkj_drt_open_panel_files(WKJString* out, int32_t cap)`. Today it also does the only `NewStringUTF` in scope (`:152,154`). |
| `JNI_OnLoad` | `JavaEnv.cpp:104` | -- | -- | **PURE glue** -- **DELETE** | none. Stashes `JavaVM* jvm` and calls `initRefs`, which caches 8 static `jmethodID` and takes 1 global ref (`:53-80`). Under FFM there is no `JavaVM` and no `jmethodID`; the whole file (146 lines) collapses to the `WKJDrtCallbacks` table installed by `wkj_drt_install_callbacks`. |
| `JNI_OnUnLoad` | `JavaEnv.cpp:116` | -- | -- | **PURE glue** -- **DELETE** | none (also note the typo: JNI spells the hook `JNI_OnUnload`, so this function is **never invoked by the JVM** and its `DeleteGlobalRef` never runs -- a real, if harmless, leak of one class global ref) |

### 5.5 Java classes in scope whose C++ lives outside the C++ scope

Listed for completeness; audit them with the `Source/WebCore/platform/java` and `Source/WTF/wtf/java`
slices. All are `OS-CALL` on inspection but were not the subject of this audit.

| Java native | Implementation | Note |
|---|---|---|
| `ContextMenu.twkHandleItemSelected` | `Source/WebCore/platform/java/ContextMenuJava.cpp:241` | reached from `WebPage.cpp:1930` (`ContextMenuJava(...).show(&cmc, self, loc)`), so the *context-menu upcall path is entered from in-scope code*; 8 upcall targets on `ContextMenuItem` + 3 on `ContextMenu` |
| `WCWidget.initIDs` | `Source/WebCore/platform/java/WidgetJava.cpp:225` | the only `_initIDs` pattern (P1) in javafx.web besides `WCPluginWidget` |
| `WCPluginWidget.initIDs`, `twkConvertToPage`, `twkInvalidateWindowlessPluginRect`, `twkSetPlugunFocused` | `Source/WebCore/platform/java/PluginWidgetJava.cpp:63,124,108,116` | |
| `MainThread.twkScheduleDispatchFunctions`, `twkSetShutdown` | `Source/WTF/wtf/java/MainThreadJava.cpp:117,128` | `twkSetShutdown` writes the `g_ShuttingDown` global that gates `AttachThreadToJavaEnv` (`Source/WTF/wtf/java/JavaEnv.h:87`) -- it must be migrated *with* the JavaEnv replacement, not before |
| `Timer.twkFireTimerEvent` | `Source/WebCore/platform/java/MainThreadSharedTimerJava.cpp:76` | paired with the `Timer.fwkSetFireTime` / `fwkStopTimer` upcalls at `:38-64` |

---

## 6. Upcall inventory

### 6.1 The thread answer, first

**Every upcall in this slice runs on the JavaFX application thread.** Evidence chain:

* `WebPage.java:186` -- the `WebPage` constructor asserts `Invoker.getInvoker().checkEventThread()`.
* `WebPage.java:201` -- `twkCreatePage` is therefore called on the FX thread; it runs
  `WTF::initializeMainThread()` (`WebPage.cpp:905`), which calls
  `initializeMainThreadPlatform` (`Source/WTF/wtf/java/MainThreadJava.cpp:56-95`), which records
  `s_mainThread = pthread_self()` / `Thread::currentID()`. So **the WebKit main thread *is* the FX
  application thread**.
* Every path that can re-enter native from another thread marshals first:
  `WebPage.beginPrinting/endPrinting/print` (`WebPage.java:1833,1861,1889`) and
  `WebPage.print(WCGraphicsContext,...)` (`:685`) wrap their `twk*` call in
  `Invoker.getInvoker().invokeOnEventThread(...)` and block on a `CountDownLatch`/`FutureTask`.
* `grep -nE 'AttachCurrentThread|GetJavaVM' <scope>` returns **zero hits** in scope. The
  `AttachThreadToJavaEnv` template exists (`Source/WTF/wtf/java/JavaEnv.h:83-114`) but is
  instantiated only outside this slice (`MainThreadJava.cpp:78`, `FileSystemJava`).

Consequences for the migrator: upcall stubs for this slice may in principle use
`Arena.ofConfined()`, but **use `Arena.ofShared()` anyway** -- the `Disposer` queue and the JVM
shutdown hook (`WebPage.java:157-165`) can close a page from a different thread, and a confined
arena would throw `WrongThreadException` there instead of failing cleanly. Nothing in this slice
needs the marshalling-to-FX-thread machinery that Glass and media need.

### 6.2 Upcall table

C site column gives file:line of the `Call*Method`. Thread is **FX application thread** for every
row (see 6.1), so the column records the *entry point* that put us there instead.

**WebPage.cpp** (3 sites)

| Java target | Signature | C site | Entered from |
|---|---|---|---|
| `WebPage.getPage` | `()J` | `WebPage.cpp:170` | any `webPageFromJObject` call, i.e. every client callback that needs the peer |
| `WebPage.fwkScroll` | `(IIIIII)V` | `WebPage.cpp:341` | `ChromeClientJava::scroll` -> layout/scroll inside a `twk*` downcall |
| `WebPage.fwkRepaint` | `(IIII)V` | `WebPage.cpp:371` | `WebPage::repaint` / `markForSync` / `postPaint` animation loop |

**ChromeClientJava.cpp** (28 sites, all on the `WebPage` peer unless noted)

| Java target | Signature | C site | Entered from |
|---|---|---|---|
| `WebPage.getHostWindow` | `()Lcom/sun/webkit/WCWidget;` | `:791` | `platformPageClient()` |
| `WebPage.fwkGetWindowBounds` | `()Lcom/sun/webkit/graphics/WCRectangle;` | `:231` | `windowRect()` |
| `WebPage.fwkSetWindowBounds` | `(IIII)V` | `:251` | `setWindowRect()` |
| `WebPage.fwkGetPageBounds` | `()Lcom/sun/webkit/graphics/WCRectangle;` | `:264` | `pageRect()` |
| `WCRectangle.x/y/w/h` field reads | `F` x4 | `:235-238`, `:268-271` | same two methods; `GetFieldID` at `:187-193` |
| `WebPage.fwkSetFocus` | `(Z)V` | `:286` (focus), `:298` (unfocus) | `focus()` / `unfocus()`; `focus()` is reached from `twkProcessMouseEvent` (`WebPage.cpp:1895`) |
| `WebPage.fwkTransferFocus` | `(Z)V` | `:315` | `takeFocus()` |
| `WebPage.fwkCreateWindow` | `(ZZZZ)Lcom/sun/webkit/WebPage;` | `:349` | `createWindow()` -- **re-enters `twkCreatePage` from inside an upcall** |
| `WebPage.fwkCloseWindow` | `()V` | `:376` | `closeWindow()` |
| `WebPage.fwkShowWindow` | `()V` | `:386` | `show()` |
| `WebPage.fwkSetScrollbarsVisible` | `(Z)V` | `:434` | `setScrollbarsVisible()` |
| `WebPage.fwkSetStatusbarText` | `(Ljava/lang/String;)V` | `:461` | `setStatusbarText()` |
| `WebPage.fwkSetCursor` | `(J)V` | `:473` | `setCursor()` -- passes a raw `platformCursor()` pointer as `jlong` |
| `WebPage.fwkAlert` | `(Ljava/lang/String;)V` | `:488` | JS `alert()` |
| `WebPage.fwkConfirm` | `(Ljava/lang/String;)Z` | `:498` | JS `confirm()` |
| `WebPage.fwkPrompt` | `(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | `:514` | JS `prompt()` |
| `WebPage.fwkChooseFile` | `(Ljava/lang/String;ZLjava/lang/String;)[Ljava/lang/String;` | `:549` | `runOpenPanel()`; result array read back at `:556-559` |
| `WebPage.fwkCanRunBeforeUnloadConfirmPanel` | `()Z` | `:576` | beforeunload |
| `WebPage.fwkRunBeforeUnloadConfirmPanel` | `(Ljava/lang/String;)Z` | `:587` | beforeunload |
| `WebPage.fwkAddMessageToConsole` | `(Ljava/lang/String;ILjava/lang/String;)V` | `:599` | JS console |
| `WebPage.fwkSetTooltip` | `(Ljava/lang/String;)V` | `:641` | `mouseDidMoveOverElement()` -> `setToolTip()` |
| `WebPage.fwkPrint` | `()V` | `:657` | JS `window.print()` |
| `WebPage.fwkScreenToWindow` | `(WCPoint)WCPoint` | `:709` | `screenToRootView()`; also `WCPoint.<init>(FF)` at `:703` and `WCPoint.getX/getY` at `:714,:717` |
| `WebPage.fwkWindowToScreen` | `(WCPoint)WCPoint` | `:754` | `rootViewToScreen()`; `WCPoint.<init>` at `:748`, `getX/getY` at `:759,:762` |

**FrameLoaderClientJava.cpp** (13 sites)

| Java target | Signature | C site | Entered from |
|---|---|---|---|
| `WebPage.fwkFrameDestroyed` | `(J)V` | `:210` | `~FrameLoaderClientJava` -- runs during `twkDestroyPage`/frame teardown |
| `WebPage.fwkSetRequestURL` | `(JILjava/lang/String;)V` | `:257` | resource start |
| `WebPage.fwkRemoveRequestURL` | `(JI)V` | `:267` | resource finish/fail |
| `WebPage.fwkFireLoadEvent` | `(JILjava/lang/String;Ljava/lang/String;DI)V` | `:296` | all page-level load transitions |
| `WebPage.fwkFireResourceLoadEvent` | `(JIILjava/lang/String;DI)V` | `:312` | all resource-level transitions |
| `WebPage.fwkPermitNewWindowAction` | `(JLjava/lang/String;)Z` | `:426` | `dispatchDecidePolicyForNewWindowAction` |
| `WebPage.fwkPermitSubmitDataAction` | `(JLjava/lang/String;Ljava/lang/String;Z)Z` | `:467` | `dispatchDecidePolicyForNavigationAction`, form branch |
| `WebPage.fwkPermitRedirectAction` | `(JLjava/lang/String;)Z` | `:474` | same, redirect branch |
| `WebPage.fwkPermitNavigateAction` | `(JLjava/lang/String;)Z` | `:480` | same, default branch |
| `WebPage.fwkFrameCreated` | `(J)V` | `:523` | `createFrame()` |
| `WebPage.fwkPermitAcceptResourceAction` | `(JLjava/lang/String;)Z` | `:651` | `dispatchWillSendRequest` |
| `NetworkContext.canHandleURL` (**static**) | `(Ljava/lang/String;)Z` | `:916` | `canHandleRequest()` |
| `WebPage.fwkDidClearWindowObject` | `(JJ)V` | `:1058` | `dispatchDidClearWindowObjectInWorld()` |
| `WebPage.fwkPermitEnableScriptsAction` | `(JLjava/lang/String;)Z` | **cached at `:120`, never called** | **orphan on both sides** -- delete the cache and the Java method (`WebPage.java:2467`) |

**Remaining WebKitLegacy clients**

| Java target | Signature | C site | Entered from |
|---|---|---|---|
| `WebPage.setInputMethodState` | `(Z)V` | `EditorClientJava.cpp:619` | `EditorClientJava::setInputMethodState`, on focus change. Note: **not** `fwk`-prefixed, and it is `public` (`WebPage.java:502`). |
| `WebPage.fwkRepaintAll` | `()V` | `InspectorClientJava.cpp:96` | `highlight()` / `hideHighlight()` |
| `WebPage.fwkSendInspectorMessageToFrontend` | `(Ljava/lang/String;)Z` | `InspectorClientJava.cpp:111` | `sendMessageToFrontend()`; return value is **discarded** at the C site |
| `WebPage.fwkFireLoadEvent` | `(JILjava/lang/String;Ljava/lang/String;DI)V` | `ProgressTrackerClientJava.cpp:88` | `progressEstimateChanged()`; same Java method as FrameLoaderClient |
| `WebPage.fwkStartDrag` | `(Ljava/lang/Object;IIII[Ljava/lang/String;[Ljava/lang/Object;Z)V` | `DragClientJava.cpp:141` | `DragClientJava::startDrag`, from the drag controller inside `twkProcessMouseEvent`. Builds two `jobjectArray`s (`:103-104`) and passes a `WCImage`/`WCImageFrame` **jobject** for the drag image. |
| `BackForwardList$Entry.<init>` | `(JJ)V` | `BackForwardList.cpp:107` | `createEntry`, from `bflGet` / `bflItemGetChildren` |
| `BackForwardList$Entry.notifyItemDestroyed` | `()V` | `BackForwardList.cpp:166` | `notifyHistoryItemDestroyed`, **entered from outside this scope**: `Source/WebCore/history/HistoryItem.cpp:78` (the `HistoryItem` destructor) |
| `BackForwardList.notifyChanged` | `()V` | `BackForwardList.cpp:155` | `notifyBackForwardListChanged`, from `addItem`/`goToItem`/`setCapacity`/`removeItem` |
| `BackForwardList$Entry.notifyItemChanged` | `()V` | `BackForwardList.cpp:119` | **dead** -- `historyItemChangedImpl` has no live caller (the hook assignment is commented out at `:345`) |
| `ColorChooser.fwkCreateAndShowColorChooser` (**static**) | `(Lcom/sun/webkit/WebPage;IIIJ)Lcom/sun/webkit/ColorChooser;` | `ColorChooserJava.cpp:48` | `ChromeClientJava::createColorChooser` |
| `ColorChooser.fwkShowColorChooser` | `(III)V` | `ColorChooserJava.cpp:74` | `reattachColorChooser()` |
| `ColorChooser.fwkHideColorChooser` | `()V` | `ColorChooserJava.cpp:101` | `endChooser()` |
| `PopupMenu.fwkCreatePopupMenu` (**static**) | `(J)Lcom/sun/webkit/PopupMenu;` | `PopupMenuJava.cpp:94` | `show()` |
| `PopupMenu.fwkAppendItem` | `(Ljava/lang/String;ZZZIILcom/sun/webkit/graphics/WCFont;)V` | `PopupMenuJava.cpp:116` | `populate()`; passes a **`WCFont` jobject** taken from `platformData().nativeFontData()` |
| `PopupMenu.fwkSetSelectedItem` | `(I)V` | `PopupMenuJava.cpp:59` | `show()` and `updateFromElement()` |
| `PopupMenu.fwkShow` | `(Lcom/sun/webkit/WebPage;III)V` | `PopupMenuJava.cpp:146` | `show()` |
| `PopupMenu.fwkHide` | `()V` | `PopupMenuJava.cpp:163` | `hide()` |
| `PopupMenu.fwkDestroy` | `()V` | `PopupMenuJava.cpp:82` | `~PopupMenuJava` |

**Dead files** (listed so the migrator does not build tables for them)

| Java target | C site | Status |
|---|---|---|
| `SocketStreamHandle.fwkCreate` / `fwkNotifyDisposed` / `fwkSend` / `fwkClose` | `SocketStreamHandleImplJava.cpp:63,84,105,122` | file is not in any build list; the live copy is `Source/WebCore/platform/network/java/SocketStreamHandleImplJava.cpp` (`Source/WebCore/SourcesJava.txt:105`) |
| `BackForwardList$Entry.notifyItemChanged` | `HistoryItemClientJava.cpp:39` | file is not in any build list and cannot link |

**DumpRenderTree** (11 sites; all on the FX thread, reached from JS running in the page)

| Java target | Signature | C site | Entered from |
|---|---|---|---|
| `DumpRenderTree.clearBackForwardList` (static) | `()V` | `TestRunnerJava.cpp:78` | `testRunner.clearBackForwardList()` in a layout test |
| `DumpRenderTree.notifyDone` (static) | `()V` | `TestRunnerJava.cpp:143` | `testRunner.notifyDone()` |
| `DumpRenderTree.overridePreference` (static) | `(Ljava/lang/String;Ljava/lang/String;)V` | `TestRunnerJava.cpp:152` | `testRunner.overridePreference()` |
| `DumpRenderTree.getBackForwardItemCount` (static) | `()I` | `TestRunnerJava.cpp:170` | `testRunner.webHistoryItemCount` |
| `DumpRenderTree.resolveURL` (static) | `(Ljava/lang/String;)Ljava/lang/String;` | `TestRunnerJava.cpp:179` | `testRunner.queueLoad()` |
| `DumpRenderTree.waitUntilDone` (static) | `()V` | `TestRunnerJava.cpp:278` | `testRunner.waitUntilDone()` |
| `DumpRenderTree.loadURL` (static) | `(Ljava/lang/String;)V` | `WorkQueueItemJava.cpp:41` | work-queue drain inside `didFinishLoad` |
| `DumpRenderTree.goBackForward` (static) | `(I)V` | `WorkQueueItemJava.cpp:62` | same |
| `EventSender.{keyDown, mouseUpDown, mouseMoveTo, mouseScroll, leapForward, contextClick, scheduleAsynchronousClick, touchStart, touchCancel, touchMove, touchEnd, addTouchPoint, updateTouchPoint, cancelTouchPoint, releaseTouchPoint, clearTouchPoints, setTouchModifier, scalePageBy, zoom, beginDragWithFiles, setDragMode}` | 21 methods, IDs cached at `EventSender.cpp:515-578` | one varargs site, `EventSender.cpp:157` (`CallVoidMethodV`) | `eventSender.*` from a layout test |
| `EventSender.getDragMode` | `()Z` | `EventSender.cpp:487` | `eventSender.dragMode` getter |

`TestRunnerJava.cpp:257` (`getWorkerThreadCount`) is inside a commented-out block (`:250-261`) and
is not compiled.

---

## 7. Cached IDs, global refs, strings, arrays, threads, exceptions

### 7.1 Cached `jclass` / `jmethodID` / `jfieldID`

| Owner | What is cached | Where |
|---|---|---|
| `ChromeClientJava` | 3 `JGClass` (`com/sun/webkit/WebPage`, `.../graphics/WCRectangle`, `.../graphics/WCPoint`) via the `DECLARE_STATIC_CLASS` macro, 22 `jmethodID`, 4 `jfieldID` | `:60-69`, `:71-110`, `initRefs` at `:112-204` |
| `FrameLoaderClientJava` | 2 `JGClass` (`WebPage`, `com/sun/webkit/network/NetworkContext`), 14 `jmethodID` | `:63-83`, `initRefs` at `:85-144` |
| `InspectorClientJava` | 1 `JGClass`, 2 `jmethodID` | `:37-39`, `initRefs` at `:41-60` |
| `ProgressTrackerClientJava` | 1 `JGClass`, 1 `jmethodID` | `:41-42`, `initRefs` at `:44-56` |
| `BackForwardList.cpp` | 2 function-local `static JGClass` (`BackForwardList$Entry`, `BackForwardList`), 4 function-local `static jmethodID` | `:85`, `:95`, `:105`, `:117`, `:148`, `:163` |
| `PopupMenuJava.cpp` | 1 function-local `static JGClass`, 6 function-local `static jmethodID` | `:47`, `:55`, `:78`, `:90`, `:105`, `:140`, `:160` |
| `ColorChooserJava.cpp` | 3 `jmethodID`, one of them **non-static and re-looked-up per construction** | `:41`, `:66`, `:95` |
| `DragClientJava.cpp` | 2 `JGClass` (`java/lang/String`, `java/lang/Object`), 1 `jmethodID` | `:94-95`, `:99` |
| `EditorClientJava.cpp` | 1 `jmethodID` | `:613` |
| `WebPage.cpp` | 3 function-local `static jmethodID` | `:164`, `:335`, `:365` |
| `Tools/DumpRenderTree/java/JavaEnv.cpp` | 1 explicit `NewGlobalRef` class + 8 static `jmethodID` | `:53-80` |
| `Tools/DumpRenderTree/java/EventSender.cpp` | 2 `JGClass`, 21 `jmethodID` | `:466`, `:508-578` |
| `Tools/DumpRenderTree/java/DumpRenderTree.cpp` | 1 `JGClass` | `:150` |

`FindClass` is called with these names: `com/sun/webkit/WebPage`,
`com/sun/webkit/graphics/WCRectangle`, `com/sun/webkit/graphics/WCPoint`,
`com/sun/webkit/network/NetworkContext`, `com/sun/webkit/BackForwardList`,
`com/sun/webkit/BackForwardList$Entry`, `com/sun/webkit/PopupMenu`,
`com/sun/webkit/network/SocketStreamHandle` (dead file), `java/lang/String`, `java/lang/Object`,
`com/sun/javafx/webkit/drt/DumpRenderTree`, `com/sun/javafx/webkit/drt/EventSender`.
All become callback-table slots, so **no class-loader lookup remains** -- which also removes the
class-loader fragility that `Source/WTF/wtf/java/MainThreadJava.cpp:56-73` documents at length.

### 7.2 Global refs

There is exactly one hand-written `NewGlobalRef`/`DeleteGlobalRef` pair, and it is broken:
`Tools/DumpRenderTree/java/JavaEnv.cpp:56` takes the ref and `:119` releases it in
**`JNI_OnUnLoad`** -- misspelled (JNI expects `JNI_OnUnload`), so it is never called and the ref
leaks for the process lifetime. Harmless in a test harness, but do not reproduce it.

Everything else uses the RAII `JGlobalRef` template (`Source/WTF/wtf/java/JavaRef.h:132-172`), which
pairs by construction. The interesting fact is therefore not leaks but **pinning**: a single
`WebPage` Java object is held by **eight** global refs at once --

`ChromeClientJava::m_webPage`, `ContextMenuClientJava::m_webPage`, `EditorClientJava::m_webPage`,
`DragClientJava::m_webPage`, `InspectorClientJava::m_webPage`, `ProgressTrackerClientJava::m_webPage`
(all built from the single `JLObject jlself(self, true)` at `WebPage.cpp:927` and installed at
`:934-952`), plus `PageSupplementJava::m_webPage`
(`Source/WebCore/platform/java/PageSupplementJava.h:48`, installed at `WebPage.cpp:958`), plus one
`FrameLoaderClientJava::m_webPage` **per frame** (`WebPage.cpp:946`, and again per subframe at
`FrameLoaderClientJava.cpp:509`).

Under FFM all eight become the same `uint64_t user` registry id and the peer becomes collectable as
soon as `WebPage.dispose()` removes the registry entry -- a leak-class fix as well as a mechanical
change. One of the eight is pure dead weight today: `ContextMenuClientJava::m_webPage`
(`ContextMenuClientJava.h:48`) is stored by the constructor (`ContextMenuClientJava.cpp:32-35`) and
**never read**; the file has zero `Call*Method` sites.

Other retained refs: `BackForwardList::m_hostObject` (`BackForwardList.h:90`),
`HistoryItem::m_hostObject` (`Source/WebCore/history/HistoryItem.h:298`, one **per history entry**),
`PopupMenuJava::m_popup` (`PopupMenuJava.h:55`), `ColorChooserJava::m_colorChooserRef`
(`ColorChooserJava.h:54`), and the per-`EventSender` `JGObject` stored in JSC private data
(`Tools/DumpRenderTree/java/EventSender.cpp:622`).

### 7.3 Strings

**This slice is UTF-16, not modified UTF-8, almost everywhere.** `String(env, jstring)` reads via
`GetStringCritical` (`Source/WTF/wtf/java/StringJava.cpp:42-47`) and `String::toJavaString` writes
via `NewString` (`:56-73`) -- 21 in-scope call sites of the former, 43 of the latter. So
`arena.allocateFrom(JAVA_CHAR, s.toCharArray())` plus a UTF-16 out-struct are the behaviour-neutral
replacements, **not** `arena.allocateFrom(String)`.

Three exceptions, and the first one matters:

| Site | Call | Risk |
|---|---|---|
| `WebPage.cpp:1219-1224` (`twkLoad`) | `GetStringUTFChars` + `GetStringUTFLength`, bytes handed to `SharedBuffer::create` inside a `ResourceResponse` that declares charset `"UTF-8"` | **Modified UTF-8 dependence.** For U+0000 and for any supplementary character, modified UTF-8 differs from standard UTF-8 (CESU-8 surrogate pairs, 6 bytes instead of 4). Today `WebEngine.loadContent(html)` carrying an astral character feeds CESU-8 to a decoder that was told it is UTF-8. Switching to `arena.allocateFrom(String)` would **change observable behaviour** -- it would fix a latent bug. See section 11. |
| `Tools/DumpRenderTree/java/DumpRenderTree.cpp:60-61` (`initTest`) | `GetStringUTFChars` on a test path and a hash | ASCII in practice; low risk, but state it in the commit |
| `Tools/DumpRenderTree/java/DumpRenderTree.cpp:152-154` (`openPanelFiles`) | `NewStringUTF` from `std::string` | same |

`Tools/DumpRenderTree/java/TestRunnerJava.cpp:38-52` converts both ways between `JSStringRef` and
`jstring` using `NewString` / `GetStringCritical` -- pure UTF-16, and the natural place to bind the
already-exported `JSStringCreateWithCharacters` / `JSStringGetCharactersPtr` from Java instead.

### 7.4 Arrays, critical sections, direct buffers

| Site | Idiom | Release mode | Notes |
|---|---|---|---|
| `WebPage.cpp:1690-1700` (`twkGetVisibleRect`) | `NewIntArray` + `GetPrimitiveArrayCritical` | `0`, copy back | write-only fill of a fresh array; becomes an `int32_t out[4]` parameter, no `critical(true)` required |
| `WebPage.cpp:1724-1732` (`twkGetContentSize`) | same | `0` | `int32_t out[2]` |
| `WebPage.cpp:2058-2073` (`twkGetTextLocation`) | same | **`JNI_ABORT`** | `JNI_ABORT` on a `GetPrimitiveArrayCritical` region is a no-op when the region is direct, so the writes *are* visible today only because HotSpot pins rather than copies. The Java caller `WebPage.getClientTextLocation` reads all four ints. An `int32_t out[4]` parameter removes the ambiguity -- but note the code was **relying on non-copying behaviour**, so this is a fragility being removed, not preserved. |
| `WebPage.cpp:1072-1081` (`twkGetChildFrames`) | `NewLongArray` + `GetLongArrayElements` | `0`, copy back | two latent bugs, section 11 |
| `WebPage.cpp:2009-2021` (`twkProcessInputTextChange`) | `GetIntArrayElements` on the IME attribute array | `JNI_ABORT` | genuinely read-only, so `JNI_ABORT` is correct. Becomes `const int32_t* attrs, int32_t count`; a `critical(true)` downcall would also be legal here (short, non-blocking, no upcall). |
| `WebPage.cpp:2263-2270` (`twkProcessDrag`) | `GetArrayLength` + `GetObjectArrayElement` over two `String[]` | -- | becomes an array of `WKJStr` built in a confined arena |
| `ChromeClientJava.cpp:556-559` | `GetArrayLength` + `GetObjectArrayElement` over the `fwkChooseFile` result | -- | upcall out-param, section 8.2 |
| `DragClientJava.cpp:103-121` | two `NewObjectArray` + `SetObjectArrayElement` | -- | upcall in-param: two `const WKJStr*` plus a count, section 8.6 |
| `BackForwardList.cpp:248-253` | `NewObjectArray` of `Entry` + `SetObjectArrayElement` | -- | C stops constructing Java objects and returns `WKJItem*` handles |
| `SocketStreamHandleImplJava.cpp:92-97,179-181` | `NewByteArray`/`SetByteArrayRegion`, `GetByteArrayElements` with `JNI_ABORT` | -- | dead file |

There is **no `GetDirectBufferAddress` anywhere in scope**. The `WCRenderQueue` command buffer is
reached through `RenderingQueue` (`Source/WebCore/platform/graphics/java/RenderingQueue.h:107`),
which is outside this slice -- see section 11.

### 7.5 Threads and exceptions

* `AttachCurrentThread` / `DetachCurrentThread` / `GetJavaVM`: **zero sites in scope** (section 6.1).
  The `AttachThreadToJavaEnv` template at `Source/WTF/wtf/java/JavaEnv.h:83-114` is instantiated
  only outside it.
* `ThrowNew`: **zero sites in scope.** Nothing here throws a Java exception from C++, so the
  "C returns an error code, Java throws" conversion (`jni-pattern-catalog` P9) has no work to do in
  this slice.
* Exceptions travelling the other way are handled by `WTF::CheckAndClearException` -- **80 calls in
  scope** -- which describes and clears. That is a *swallow*: if `fwkAlert` throws, WebKit continues
  as though it had not. FFM upcall stubs must reproduce that exactly (catch `Throwable`, log through
  `PlatformLogger`, return the default) and must **not** start propagating; doing so would be a
  behaviour change hiding inside a migration commit.
* `Tools/DumpRenderTree/java/JavaEnv.cpp:90-98` has its own `CheckAndClearException` with the same
  semantics, plus the only `ExceptionDescribe` calls in scope (`:58`, `:93`).

---

## 8. Proposed callback tables

One table per client, matching the grouping in section 6. Every entry takes the `uint64_t user`
registry id as its first argument; native code never holds a Java reference. Every table must
tolerate a `NULL` slot. All stubs live in an `Arena.ofShared()` owned by the Java `WebPage` and are
closed after `wkj_page_destroy` returns (section 6.1 explains why shared rather than confined).

Strings crossing *into* Java use `WKJStr` (UTF-16, `length < 0` means null), matching todays
`String::toJavaString` semantics exactly. Strings crossing *out of* Java use a caller-provided
`WKJString*` buffer plus capacity, so no Java-owned memory is retained by C.

### 8.1 Installation

    typedef struct WKJPageCallbacks {
        const WKJChromeCallbacks*          chrome;
        const WKJFrameLoaderCallbacks*     loader;
        const WKJEditorCallbacks*          editor;
        const WKJInspectorCallbacks*       inspector;
        const WKJDragCallbacks*            drag;
        const WKJProgressCallbacks*        progress;
        const WKJPageNotifyCallbacks*      notify;   /* WebPage.cpp itself */
    } WKJPageCallbacks;

    JFX_EXPORT WKJPage* wkj_page_create(int32_t editable,
                                        const WKJPageCallbacks* cb, uint64_t user);

`ContextMenuClientJava` needs **no table**: it has zero upcalls (section 4/7.2). Drop its
`m_webPage` field and its constructor parameter.

### 8.2 `WKJChromeCallbacks` -- replaces ChromeClientJava (22 methods + 4 field reads)

    typedef struct WKJRect  { float x, y, w, h; } WKJRect;
    typedef struct WKJPoint { float x, y; }       WKJPoint;

    typedef struct WKJChromeCallbacks {
        /* HostWindow / geometry. The WCRectangle GetFieldID reads at
           ChromeClientJava.cpp:187-193 collapse into these struct-by-value out-params. */
        int32_t  (*get_window_bounds)(uint64_t user, WKJRect* out);   /* 0 == Java returned null */
        void     (*set_window_bounds)(uint64_t user, int32_t x, int32_t y, int32_t w, int32_t h);
        int32_t  (*get_page_bounds)(uint64_t user, WKJRect* out);
        void     (*screen_to_window)(uint64_t user, const WKJPoint* in, WKJPoint* out);
        void     (*window_to_screen)(uint64_t user, const WKJPoint* in, WKJPoint* out);
        uint64_t (*get_host_window)(uint64_t user);                   /* WCWidget registry id */

        /* focus, cursor, chrome */
        void     (*set_focus)(uint64_t user, int32_t focused);
        void     (*transfer_focus)(uint64_t user, int32_t forward);
        void     (*set_cursor)(uint64_t user, int64_t platformCursor);
        void     (*set_tooltip)(uint64_t user, WKJStr text);
        void     (*set_scrollbars_visible)(uint64_t user, int32_t visible);
        void     (*set_statusbar_text)(uint64_t user, WKJStr text);

        /* window lifecycle */
        uint64_t (*create_window)(uint64_t user, int32_t menuBar, int32_t statusBar,
                                  int32_t toolBar, int32_t resizable);  /* new page registry id */
        void     (*show_window)(uint64_t user);
        void     (*close_window)(uint64_t user);

        /* JS dialogs; prompt writes into a caller-owned buffer, returns length or -1 for null */
        void     (*alert)(uint64_t user, WKJStr text);
        int32_t  (*confirm)(uint64_t user, WKJStr text);
        int32_t  (*prompt)(uint64_t user, WKJStr text, WKJStr defaultValue,
                           uint16_t* out, int32_t cap);
        int32_t  (*can_run_before_unload)(uint64_t user);
        int32_t  (*run_before_unload)(uint64_t user, WKJStr message);
        void     (*add_message_to_console)(uint64_t user, WKJStr message,
                                           int32_t lineNumber, WKJStr sourceId);
        void     (*print)(uint64_t user);

        /* file chooser: fills up to cap entries, returns the count, -1 for a Java null result.
           MUST NOT be marked critical -- it opens a modal dialog and blocks. */
        int32_t  (*choose_file)(uint64_t user, WKJStr initialFileName, int32_t multiple,
                                WKJStr mimeFilters, WKJString* out, int32_t cap);
    } WKJChromeCallbacks;

`create_window` returning a registry id, rather than a `jobject`, is what removes
`WebPage::pageFromJObject` (`WebPage.cpp:160-174`) and with it the `WebPage.getPage` upcall.

### 8.3 `WKJFrameLoaderCallbacks` -- replaces FrameLoaderClientJava (13 live + 1 static)

    typedef struct WKJFrameLoaderCallbacks {
        void    (*frame_created)(uint64_t user, WKJFrame* frame);
        void    (*frame_destroyed)(uint64_t user, WKJFrame* frame);

        void    (*set_request_url)(uint64_t user, WKJFrame*, int32_t id, WKJStr url);
        void    (*remove_request_url)(uint64_t user, WKJFrame*, int32_t id);

        void    (*fire_load_event)(uint64_t user, WKJFrame*, int32_t state,
                                   WKJStr url, WKJStr contentType,
                                   double progress, int32_t errorCode);
        void    (*fire_resource_load_event)(uint64_t user, WKJFrame*, int32_t state, int32_t id,
                                            WKJStr contentType, double progress, int32_t errorCode);

        int32_t (*permit_navigate)(uint64_t user, WKJFrame*, WKJStr url);
        int32_t (*permit_redirect)(uint64_t user, WKJFrame*, WKJStr url);
        int32_t (*permit_accept_resource)(uint64_t user, WKJFrame*, WKJStr url);
        int32_t (*permit_submit_data)(uint64_t user, WKJFrame*, WKJStr url,
                                      WKJStr httpMethod, int32_t isSubmit);
        int32_t (*permit_new_window)(uint64_t user, WKJFrame*, WKJStr url);
        /* NO permit_enable_scripts slot: cached at FrameLoaderClientJava.cpp:120 and never
           called; WebPage.fwkPermitEnableScriptsAction (WebPage.java:2467) has no caller. */

        void    (*did_clear_window_object)(uint64_t user, void* jsContext, void* jsWindowObject);

        /* process-wide, not per page: NetworkContext.canHandleURL is a static Java method
           (FrameLoaderClientJava.cpp:141,916). Install once via wkj_install_network_callbacks. */
    } WKJFrameLoaderCallbacks;

    typedef struct WKJNetworkCallbacks {
        int32_t (*can_handle_url)(WKJStr url);
    } WKJNetworkCallbacks;
    JFX_EXPORT void wkj_install_network_callbacks(const WKJNetworkCallbacks*);

Note that `fire_load_event` is shared with `WKJProgressCallbacks` (both C++ files cache the same
`fwkFireLoadEvent` id -- `FrameLoaderClientJava.cpp:97` and `ProgressTrackerClientJava.cpp:51`).
Keep one Java target and one stub; give `ProgressTrackerClientJava` a pointer to the same table.

### 8.4 `WKJEditorCallbacks` -- replaces EditorClientJava (1 method)

    typedef struct WKJEditorCallbacks {
        void (*set_input_method_state)(uint64_t user, int32_t enabled);
    } WKJEditorCallbacks;

The Java target is `WebPage.setInputMethodState(boolean)` (`WebPage.java:502`), which is `public`
and **not** `fwk`-prefixed -- do not rename it during the migration; `InputMethodClientImpl` and the
FX text-input path depend on the current name.

### 8.5 `WKJInspectorCallbacks` -- replaces InspectorClientJava (2 methods)

    typedef struct WKJInspectorCallbacks {
        void (*repaint_all)(uint64_t user);
        void (*send_message_to_frontend)(uint64_t user, WKJStr message);
    } WKJInspectorCallbacks;

`send_message_to_frontend` returns `void`, not `int32_t`: the Java method returns `boolean` but
`InspectorClientJava.cpp:111` **discards the result**. Preserving that is behaviour-neutral;
plumbing it through would not be.

### 8.6 `WKJDragCallbacks` -- replaces DragClientJava (1 method)

    typedef struct WKJDragCallbacks {
        void (*start_drag)(uint64_t user,
                           uint64_t imageId,        /* WCImage or WCImageFrame registry id, 0 = none */
                           int32_t offsetX, int32_t offsetY,
                           int32_t eventX,  int32_t eventY,
                           const WKJStr* mimeTypes, const WKJStr* values, int32_t count,
                           int32_t isImageSource);
    } WKJDragCallbacks;

`imageId` is the one genuinely awkward parameter: `DragClientJava.cpp:136-138` passes a raw
`jobject` that may be either a `WCImage` **or** a `WCImageFrame`, with the comment that the rasters
are too different to convert in native code. Under FFM it must become a registry id minted by the
graphics slice, so this table cannot be finished before `Source/WebCore/platform/graphics/java` is
migrated. Sequence it accordingly (section 12).

### 8.7 `WKJPageNotifyCallbacks` and `WKJProgressCallbacks`

    typedef struct WKJPageNotifyCallbacks {   /* the 2 live upcalls in WebPage.cpp itself */
        void (*repaint)(uint64_t user, int32_t x, int32_t y, int32_t w, int32_t h);
        void (*scroll)(uint64_t user, int32_t x, int32_t y, int32_t w, int32_t h,
                       int32_t deltaX, int32_t deltaY);
        /* no get_page slot: WebPage.cpp:164-174 exists only to turn a jobject back into a
           WebPage*, which the uint64_t user id makes unnecessary. */
    } WKJPageNotifyCallbacks;

    typedef struct WKJProgressCallbacks {
        void (*fire_load_event)(uint64_t user, WKJFrame*, int32_t state,
                                WKJStr url, WKJStr contentType,
                                double progress, int32_t errorCode);
    } WKJProgressCallbacks;

### 8.8 Peer-scoped tables: back/forward list, popup menu, colour chooser

    typedef struct WKJBackForwardCallbacks {
        void (*list_changed)(uint64_t listUser);
        void (*item_destroyed)(uint64_t itemUser);
        /* no item_changed slot: BackForwardList.cpp:115-122 (historyItemChangedImpl) has no
           live caller -- the hook assignment at :345 is commented out. */
    } WKJBackForwardCallbacks;
    JFX_EXPORT void wkj_bfl_set_callbacks(WKJPage*, const WKJBackForwardCallbacks*, uint64_t user);

    typedef struct WKJPopupCallbacks {
        uint64_t (*create)(WKJPopup* popup);          /* -> PopupMenu.fwkCreatePopupMenu */
        void     (*append_item)(uint64_t user, WKJStr text, int32_t isLabel, int32_t isSeparator,
                                int32_t isEnabled, uint32_t bgArgb, uint32_t fgArgb,
                                uint64_t fontId);     /* WCFont registry id, graphics slice */
        void     (*set_selected_item)(uint64_t user, int32_t index);
        void     (*show)(uint64_t user, uint64_t pageUser, int32_t x, int32_t y, int32_t width);
        void     (*hide)(uint64_t user);
        void     (*destroy)(uint64_t user);
    } WKJPopupCallbacks;
    JFX_EXPORT void wkj_install_popup_callbacks(const WKJPopupCallbacks*);

    typedef struct WKJColorChooserCallbacks {
        uint64_t (*create_and_show)(uint64_t pageUser, int32_t r, int32_t g, int32_t b,
                                    WKJColorChooser* chooser);
        void     (*show)(uint64_t user, int32_t r, int32_t g, int32_t b);
        void     (*hide)(uint64_t user);
    } WKJColorChooserCallbacks;
    JFX_EXPORT void wkj_install_color_chooser_callbacks(const WKJColorChooserCallbacks*);

`append_item` carries the same graphics dependency as `start_drag`: `PopupMenuJava.cpp:122` passes a
`WCFont` jobject straight out of `platformData().nativeFontData()`.

### 8.9 DumpRenderTree tables

    typedef struct WKJDrtCallbacks {          /* the 8 static DumpRenderTree methods */
        void    (*wait_until_done)(void);
        void    (*notify_done)(void);
        void    (*override_preference)(WKJStr key, WKJStr value);
        int32_t (*get_back_forward_item_count)(void);
        void    (*clear_back_forward_list)(void);
        int32_t (*resolve_url)(WKJStr relative, uint16_t* out, int32_t cap);
        void    (*load_url)(WKJStr url);
        void    (*go_back_forward)(int32_t howFar);
    } WKJDrtCallbacks;
    JFX_EXPORT void wkj_drt_install_callbacks(const WKJDrtCallbacks*);

    typedef struct WKJEventSenderCallbacks {  /* the 21 EventSender methods */
        void    (*key_down)(uint64_t user, WKJStr key, int32_t modifiers);
        void    (*mouse_up_down)(uint64_t user, int32_t button, int32_t modifiers);
        void    (*mouse_move_to)(uint64_t user, int32_t x, int32_t y);
        void    (*mouse_scroll)(uint64_t user, float dx, float dy, int32_t continuous);
        void    (*leap_forward)(uint64_t user, int32_t ms);
        void    (*context_click)(uint64_t user);
        void    (*schedule_asynchronous_click)(uint64_t user);
        void    (*touch_start)(uint64_t user);
        void    (*touch_cancel)(uint64_t user);
        void    (*touch_move)(uint64_t user);
        void    (*touch_end)(uint64_t user);
        void    (*add_touch_point)(uint64_t user, int32_t x, int32_t y);
        void    (*update_touch_point)(uint64_t user, int32_t i, int32_t x, int32_t y);
        void    (*cancel_touch_point)(uint64_t user, int32_t i);
        void    (*release_touch_point)(uint64_t user, int32_t i);
        void    (*clear_touch_points)(uint64_t user);
        void    (*set_touch_modifier)(uint64_t user, int32_t modifier, int32_t on);
        void    (*scale_page_by)(uint64_t user, float scale, int32_t x, int32_t y);
        void    (*zoom)(uint64_t user, int32_t in, int32_t textOnly);
        void    (*begin_drag_with_files)(uint64_t user, const WKJStr* files, int32_t count);
        int32_t (*get_drag_mode)(uint64_t user);
        void    (*set_drag_mode)(uint64_t user, int32_t on);
    } WKJEventSenderCallbacks;

This is the one table whose conversion is a real simplification rather than a translation: it
replaces `EventSender.cpp`s single `CallVoidMethodV` varargs dispatcher (`:150-160`) plus 21 cached
`jmethodID`s with 21 typed slots, and removes the untyped `va_list` path entirely.

---

## 9. Deletion candidates (all clear the parity gate)

Nothing in this list is a rendering, shaping or decoding algorithm, so none of it is
`PARITY: unprovable`. Each entry gives the parity verdict, lines removed, per-platform copies,
whether a Java counterpart already exists, and the test that would prove it.

### 9.1 Dead code -- no behaviour to preserve at all

| Item | Lines | Copies | Evidence it is dead | Parity test |
|---|---:|---:|---|---|
| `Java_..._WebPage_twkProcessTouchEvent` (`WebPage.cpp:1964-1982`) | 19 | 1 | `#if ENABLE(TOUCH_EVENTS)`; `Source/cmake/OptionsJava.cmake:82` sets it `OFF`; no Java `native` declaration; **absent from the shipped `jfxwebkit.dll`** (89 `WebPage_twk*` exports = 87 compiled + 2 from `JavaDOMUtils.cpp`) | none needed -- it is not in the binary. Assert the export count is unchanged after removal. |
| `Java_..._WebPage_twkAddJavaScriptBinding` (`WebPage.cpp:1576-1603`) plus 2 mapfile lines | 28 + 2 | 1 | no Java `native` declaration in `src/main`, `src/test`, `src/shims`, `src/android` or `src/ios` | `JavaScriptBridgeTest` and `BindingTest` stay green -- they use `JSObject.setMember`, a different path in `Source/WebCore/bridge/jni` |
| `Java_..._WebPage_twkGetLocationOffset` (`WebPage.cpp:2076-2111`) plus 2 mapfile lines | 36 + 2 | 1 | no Java `native` declaration; its apparent caller `WebPage.getClientLocationOffset` (`WebPage.java:960-973`) calls `twkGetInsertPositionOffset` instead | none. Record the pre-existing IME defect in the commit message but **do not fix it here**. |
| `Java_..._BackForwardList_bflItemGetLastVisitedDate` (`BackForwardList.cpp:205-212`), the Java native at `BackForwardList.java:318`, plus 2 mapfile lines | 7 + 1 + 2 | 1 | body is `return 0;`; `Entry.getLastVisitedDate()` (`BackForwardList.java:110-112`) returns the field and never calls it; the only other hits are the two mapfiles | `HistoryTest` -- `WebHistory.Entry.getLastVisitedDate()` is driven by `updateLastVisitedDate` (`BackForwardList.java:114-117`) and must be unchanged |
| `Source/WebKitLegacy/java/WebCoreSupport/SocketStreamHandleImplJava.cpp` | 204 | 2 (this one and the live `Source/WebCore/platform/network/java/SocketStreamHandleImplJava.cpp`) | not named in `Source/WebKitLegacy/PlatformJava.cmake`; the live copy is at `Source/WebCore/SourcesJava.txt:105`; this copy still calls the removed `didReceiveSocketStreamData(*this, data, length)` overload, so it would not compile | none -- delete and confirm `jfxwebkit` still links |
| `Source/WebKitLegacy/java/WebCoreSupport/HistoryItemClientJava.cpp` and `.h` | 47 + 40 | 1 | in no build list; the class name appears nowhere else in the tree; the `.cpp` calls `initMethod`, `getJEntryClass` and `historyItemChangedImpl`, all of which exist only in an **anonymous namespace inside `BackForwardList.cpp`**, so it cannot link | none |
| `historyItemChangedImpl` (`BackForwardList.cpp:115-122`) | 8 | 1 | its only references are the commented-out hook at `:345` and the dead `HistoryItemClientJava.cpp` | none. Keep `initMethod`/`initCtor` (`:67-77`): `createEntry`, `notifyBackForwardListChanged` and `notifyHistoryItemDestroyed` still use them. |
| `permitEnableScriptsActionMID` (`FrameLoaderClientJava.cpp:77,120-122`) and `WebPage.fwkPermitEnableScriptsAction` (`WebPage.java:2467-2474`) | 4 + 8 | 1 | cached and never called; the Java method has no caller | `LoadNotificationsTest` and `LoadTest` stay green. Leave `PolicyClient.permitEnableScriptsAction` (`PolicyClient.java:42`) alone -- it is interface-shaped; delete only the unreachable `WebPage` bridge. |
| `ContextMenuClientJava::m_webPage` (`ContextMenuClientJava.h:48`) and the constructor parameter | 3 | 1 | the file has zero `Call*Method` sites; the field is written at `ContextMenuClientJava.cpp:32-35` and never read | context-menu behaviour is unchanged by construction; `tests/system` `robot/javafx/web/PointerEventTest` covers the path |

**Total: about 390 lines of C++, one Java `native` declaration, one Java method, eight mapfile
lines.** All portable C++ with a single copy, so the per-platform multiplier is 1. The value is not
the line count -- it is that **three of these are exported symbols in a shipped binary that nothing
can call**, exactly the sort of thing an FFM facade would otherwise have faithfully reproduced.

### 9.2 Live code that should be deleted rather than re-plumbed

| Item | Verdict | Lines | Existing Java counterpart | Parity test |
|---|---|---:|---|---|
| `Java_..._WebPage_twkGetIconURL` (`WebPage.cpp:1178-1191`) | `PURE`, `PARITY: exact` | 14 | `WebPage.getIcon(long)` (`WebPage.java:1229-1249`) already handles a null result; it becomes an unconditional `return null` | assert `WebPage.getIcon(mainFrame)` is `null` before and after, plus `HistoryTest`. The C is `return 0;` for every input because `ENABLE(ICONDATABASE)` is never defined for this port. |
| `Java_..._BackForwardList_bflItemGetIcon` (`BackForwardList.cpp:188-203`) | `PURE`, `PARITY: exact` | 15 | `Entry.getIcon()` (`BackForwardList.java:102-104`) becomes `return null` | `HistoryTest`; assert the `WebHistory.Entry` icon accessors are unchanged. The body is entirely commented out (`:191-201`). |
| `Java_..._WebPage_twkDoJSCGarbageCollection` (`WebPage.cpp:2624-2628`) | `WRAPPER` | 5 | none needed -- Java binds the already-exported plain-C `WebPage_doJSCGarbageCollection` (`mapfile-vers:142`, `mapfile-macosx:135`, DLL ordinal 3B83) with `FunctionDescriptor.ofVoid()` | `LeakTest` and `EventListenerLeakTest`, which depend on `WebPage.collectJSCGarbages` (`WebPage.java:170-177`) |
| the JNI half of `Tools/DumpRenderTree/java/JavaEnv.cpp` (`:30-51`, `:53-80`, `:83-121`) | `PURE` glue | about 90 of 146 | replaced by `WKJDrtCallbacks` (section 8.9); no Java counterpart needed | run a `LayoutTests` subset through `com.sun.javafx.webkit.drt.DumpRenderTree` and diff the expected results before and after |

### 9.3 Not a deletion candidate despite being PURE

`Java_..._WebPage_twkInitWebCore` (`WebPage.cpp:892-897`) touches no external symbol and its parity
is exact, so the verdict is `PURE` -- but it writes three **file-static** bools (`s_useJIT`,
`s_useDFGJIT`, `s_useCSS3D`, `WebPage.cpp:884-886`) that `twkCreatePage` reads at `:922-924`. Java
cannot own state that lives inside `jfxwebkit`. It stays native as
`wkj_set_startup_options(int32_t, int32_t, int32_t)`. This is an ownership constraint, not a parity
one -- do not list it as a deletion candidate.

---

## 10. Kept native despite being pure

The `PARITY: unprovable` / `PARITY: unknown` list. It is short, which is the honest result for this
slice: there is no rasteriser, shaper or codec here whose C output is the de facto specification.

| Function | Verdict | Parity | Why it stays native |
|---|---|---|---|
| `twkInitWebCore` (`WebPage.cpp:892`) | `PURE` | `PARITY: exact` | the state it writes lives inside the library (section 9.3). An ownership constraint, not a parity one. |
| `initDRT` (`Tools/DumpRenderTree/java/DumpRenderTree.cpp:49`) | `OS-CALL`, borderline `WRAPPER` | `PARITY: unknown` | Its three callees **are** exported -- `_ZN3WTF26setPermissionsOfConfigPageEv` (`mapfile-vers:38`), `_ZN3WTF6Config25disableFreezingForTestingEv` (`:21`), `_ZN3JSC6Config23enableRestrictedOptionsEv` (`:130`) -- but only under Itanium C++ mangling; the Windows build has no export list and spells them differently. **Experiment that settles it:** `dumpbin -exports DumpRenderTreeJava.dll` filtered for `setPermissionsOfConfigPage` on a Windows WebKit build, and `nm -D --defined-only libjfxwebkit.so` filtered the same way on Linux. If the spellings differ it stays `OS-CALL`; adding `extern "C"` aliases would make it a real `WRAPPER`. Not worth the churn for one call. |
| `dumpAsText`, `dumpChildFramesAsText`, `dumpBackForwardList`, `shouldStayOnPageAfterHandlingBeforeUnload`, `openPanelFiles` (`Tools/DumpRenderTree/java/DumpRenderTree.cpp:110-157`) | `OS-CALL` | n/a | they read `gTestRunner`, which looks like a plain struct but is mutated from **JavaScriptCore callbacks** running the layout test (`TestRunnerJava.cpp` throughout). Moving that state to Java means moving the whole `testRunner` JS binding surface. |

Everything else in the slice is `OS-CALL` on the named evidence in sections 4 and 5, and needs no
parity argument at all.

---

## 11. Build and test touchpoints

### 11.1 Build touchpoints

| What | Where | Action |
|---|---|---|
| WebKitLegacy source list | `Source/WebKitLegacy/PlatformJava.cmake:119-135` | add `java/api/webkit_java_api.cpp`; remove nothing until the JNI half goes |
| Include dirs | `Source/WebKitLegacy/PlatformJava.cmake:184-191` | add `java/api` |
| **Exported symbol lists** | `Source/WebCore/mapfile-vers` (1857 `Java_com_sun_*` entries), `Source/WebCore/mapfile-macosx` (1612) | **Already handled** by the parallel migrator session with a glob: `wkj_*;` at `mapfile-vers:6`, `_wkj_*` at `mapfile-macosx:4`. Windows needs nothing (no export list; `JNIEXPORT`/`WKJ_EXPORT` alone). Verify the globs actually match on both linkers before trusting them -- section 4 and risk 11. |
| JNI include paths | `Source/WebCore/PlatformJava.cmake:35-36`, `Source/WTF/wtf/PlatformJava.cmake:8-9`, `Source/JavaScriptCore/PlatformJava.cmake:16-17`, `Tools/DumpRenderTree/java/CMakeLists.txt:47-48`, `Tools/TestRunnerShared/java/CMakeLists.txt:37-38` | remove only when the whole module is JNI-free; `find_package(JNI REQUIRED)` at `Source/cmake/OptionsJava.cmake:43` goes with them |
| `${JAVA_JVM_LIBRARY}` | `Tools/DumpRenderTree/java/CMakeLists.txt:25`, `Tools/TestRunnerShared/java/CMakeLists.txt:19` | remove when the DRT natives are JNI-free. Note `jfxwebkit` itself never linked it (section 4). |
| Generated constant headers | `com_sun_webkit_WebPage.h`, `com_sun_webkit_event_WCKeyEvent.h`, `com_sun_webkit_event_WCFocusEvent.h`, `com_sun_webkit_event_WCMouseEvent.h` (`WebPage.cpp:132-135`), `com_sun_webkit_LoadListenerClient.h` (`FrameLoaderClientJava.cpp:57`), `com_sun_webkit_PageCache.h`, `com_sun_webkit_PopupMenu.h` | **nothing in this repository generates them.** `modules/javafx.web/pom.xml` has no `-h` compiler argument and no `javac -h` step exists in any `*.cmake` under `src/main/native`. Replace them with plain C enums in `webkit_java_api.h`; that removes the last generated-JNI-header dependency from the C++ side and closes a real build gap. |
| Java facade | new `com.sun.webkit.WebKitNative` | holds `SymbolLookup.loaderLookup()` taken after `NativeLibLoader.loadLibrary("jfxwebkit")` (`WebPage.java:130`), plus all `MethodHandle`s and `MemoryLayout`s. `--enable-native-access=javafx.graphics,javafx.media,javafx.web` is already on the surefire `argLine` of `modules/javafx.web/pom.xml` and `tests/system/pom.xml`. |
| DRT facade | new class beside `com.sun.javafx.webkit.drt.DumpRenderTree` | it uses plain `System.loadLibrary("DumpRenderTreeJava")` (`DumpRenderTree.java:208`), so `SymbolLookup.loaderLookup()` works there too |

### 11.2 Tests

Coverage of these classes, from `grep -rln` over `modules/javafx.web/src/test/java` and
`tests/system/src/test/java`:

* `test/javafx/scene/web/WebPageTest.java` -- the only test that drives `WebPage` directly
  (`getHtml`, frame count via `WebPageShim.getFramesCount`). Uses
  `modules/javafx.web/src/shims/java/com/sun/webkit/WebPageShim.java` and
  `javafx/scene/web/WebEngineShim.java`.
* `LoadTest`, `LoadNotificationsTest`, `HistoryTest`, `HistoryStateTest` -- `FrameLoaderClientJava`
  and `BackForwardList` upcalls.
* `JavaScriptBridgeTest`, `BindingTest`, `CallbackTest`, `DebuggerTest`, `IrresponsiveScriptTest` --
  `twkExecuteScript`, the inspector, the JSC watchdog.
* `FormControlsTest`, `HTMLEditingTest`, `CSSTest`, `SVGTest`, `CanvasTest`, `OpacityTest`,
  `ShadowTest`, `MathMLRenderTest` -- the paint path
  (`twkPrePaint` / `twkUpdateContent` / `twkPostPaint`).
* `LeakTest`, `EventListenerLeakTest` -- `twkDoJSCGarbageCollection` and the global-ref pinning.
* `LocalStorageTest`, `DirectoryLockTest`, `UserDataDirectoryTest` -- `twkSetLocalStorage*`.
* `tests/system/src/test/java/test/robot/javafx/web/PointerEventTest.java`, `TextSelectionTest.java`,
  `TooltipFXTest.java` -- the mouse, selection and `fwkSetTooltip` paths.
* `com.sun.javafx.webkit.drt.DumpRenderTree` -- the regression net for the client classes. Nothing in
  the build runs it; invoke it by hand and diff against the WebKit `LayoutTests` expectations.

**No test stubs or overrides any of these natives.** There is no `_initIDs` test double and no
`StubToolkit` hook for javafx.web, so every test above needs a real `jfxwebkit` on
`java.library.path` and is skipped by default (`jfx.web.skipTests=true`,
`modules/javafx.web/pom.xml:48`). Run with:

    mvn -pl modules/javafx.web test -Djfx.web.skipTests=false
    mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Dtest="test/robot/javafx/web/**"

CI (`.github/workflows/submit.yml`, plain `mvn -B -ntp -fae install`) runs neither and does not build
WebKit. Every migration PR in this slice must state which local WebKit build and which prebuilt
`jfxwebkit` verified it.

---

## 12. Risks

1. **Modified UTF-8 in `twkLoad` (`WebPage.cpp:1219-1224`).** `GetStringUTFChars` plus
   `GetStringUTFLength` hand modified-UTF-8 bytes to `SharedBuffer::create` inside a
   `ResourceResponse` that declares charset `"UTF-8"`. For U+0000 and for supplementary characters
   the two encodings differ (CESU-8 surrogate pairs, 6 bytes instead of 4). Migrating to
   `arena.allocateFrom(String)` **changes behaviour** -- it fixes a latent bug. Either keep
   byte-for-byte compatibility by encoding modified UTF-8 on the Java side, or make the fix a
   **separate, non-migration commit** with a test that calls `WebEngine.loadContent` with an astral
   character (U+1F600, say) and asserts the resulting DOM text. Do not let this slip into a
   migration commit.
2. **`JNI_ABORT` on a critical region (`WebPage.cpp:2065-2070`).** `twkGetTextLocation` fills a
   fresh int array under `GetPrimitiveArrayCritical` and releases it with `JNI_ABORT`. That works
   only because HotSpot pins rather than copies. The out-param ABI removes the hazard, but note in
   the commit that the pre-migration code was relying on unspecified behaviour.
3. **Eight global refs pin one `WebPage`** (section 7.2). The registry-id conversion is also a
   leak-class fix, so `LeakTest` and `EventListenerLeakTest` may *improve*, which is not
   behaviour-neutral in the strict sense. Say so in the PR rather than letting a reviewer discover it.
4. **Callbacks that block.** `choose_file` (`ChromeClientJava.cpp:549`), `prompt` (`:514`),
   `confirm` (`:498`), `alert` (`:488`) and `run_before_unload` (`:587`) open modal UI and block.
   Their downcall counterparts must **never** carry `Linker.Option.critical(true)`, and the arena
   holding their stubs must outlive the modal.
5. **Re-entrancy through `create_window`.** `ChromeClientJava::createWindow`
   (`ChromeClientJava.cpp:339-368`) upcalls into Java, which constructs a new `WebPage`, which calls
   `twkCreatePage` -- a downcall inside an upcall inside a downcall. The outer stub arena must not be
   closed on that path, and the registry must tolerate an insertion while an upcall is in flight
   (use `ConcurrentHashMap`).
6. **Java objects crossing the boundary that are not `WebPage`.** Three upcall paths pass a
   non-`WebPage` Java object today and therefore cannot be completed before the graphics slice:
   `PopupMenu.fwkAppendItem` takes a `WCFont` (`PopupMenuJava.cpp:122`), `WebPage.fwkStartDrag`
   takes a `WCImage` **or** a `WCImageFrame` (`DragClientJava.cpp:136-138`, with an explicit comment
   that the rasters cannot be converted in native code), and `twkUpdateContent` / `twkPostPaint` /
   `twkPrint` take a `WCRenderQueue` (`WebPage.cpp:1763,1775,1629`), consumed by
   `PlatformContextJava` (`Source/WebCore/platform/graphics/java/PlatformContextJava.h:42`).
   **Sequence the graphics slice before these three.**
7. **Struct-by-value returns.** `WKJString` and `WKJRect` are shown returned by value in section 5.0
   and 8.2. Small-struct return ABIs differ across SysV, Win64 and AArch64. Either verify each with a
   `wkj_sizeof_*` test (`jni-to-ffm-migration` section 4) or, safer, convert them to out-params. The
   recommendation is out-params for anything wider than 8 bytes.
8. **32-bit assumptions: none found.** `jlong_to_ptr` / `ptr_to_jlong`
   (`Source/WTF/wtf/java/JavaEnv.h:128-129`, duplicated at `Source/WTF/wtf/java/JavaRef.h:46-47`) go
   through `uintptr_t` and are correct. Keep the Java-side peer fields as `long` at the public
   boundary and convert once inside `WebKitNative` with `MemorySegment.ofAddress`.
9. **Exceptions are swallowed today** -- 80 `WTF::CheckAndClearException` calls in scope. Upcall
   stubs must keep swallowing (catch `Throwable`, log via `PlatformLogger`, return the default).
   A stub that lets a `Throwable` escape terminates the JVM.
10. **`twkGetChildFrames` has two latent bugs** (`WebPage.cpp:1061-1084`) that the ABI change will
    expose. It returns a null array when the frame is not a `LocalFrame`, and
    `WebPage.getChildFrames` (`WebPage.java:1601-1605`) iterates the result without a null check
    (NPE). It also sizes the array with `tree.childCount()` while skipping non-local children, so
    trailing zeroes reach Java as frame id `0`. Neither is reachable with site isolation off. The
    count-returning ABI fixes both by construction -- **call that out in the commit** rather than
    letting it look like a silent behaviour change.
11. **The mapfiles are hand-maintained and already wrong.** They list `twkAddJavaScriptBinding`,
    `twkGetLocationOffset` and `bflItemGetLastVisitedDate` -- none of which any Java code calls
    (section 9.1). The `wkj_*` glob the migrator added (`mapfile-vers:6`, `mapfile-macosx:4`) avoids
    repeating that drift for the new ABI, which is the right call. Two residual risks: the glob is
    untested on both linkers (section 4), and the three stale `Java_*` entries should come out in
    the same commit that deletes the dead exports, otherwise the export lists name symbols that no
    longer exist -- harmless on GNU `ld`, a hard error on some `ld64` versions.
12. **Nothing in this repository compiles the code being migrated.** `mvn -pl modules/javafx.web
    install` builds Java only. Every step needs an out-of-band WebKit build before it can even be
    compiled, let alone run. Plan on migrating one whole client class per verified rebuild rather
    than one function at a time.

---

## 13. Recommendation

**Migrate this slice -- it is 112/120 `OS-CALL` and there is nothing for Java to bind instead -- but
open with a deletion commit and close with the JavaEnv replacement.** Keep migration commits
(behaviour-neutral) and deletion/reimplementation commits (not behaviour-neutral) strictly separate,
per `openjfx-conventions`.

### Step 0 -- `FFM-web-0: Remove dead JNI surface from the WebKitLegacy Java port`

Not a migration commit. Purely subtractive, and it shrinks everything that follows.

* Delete `twkProcessTouchEvent`, `twkAddJavaScriptBinding`, `twkGetLocationOffset` from
  `WebPage.cpp` and their 4 mapfile lines.
* Delete `bflItemGetLastVisitedDate` (C + the Java `native` at `BackForwardList.java:318`) and its
  2 mapfile lines.
* Delete `SocketStreamHandleImplJava.cpp` and `HistoryItemClientJava.{cpp,h}` from
  `Source/WebKitLegacy/java/WebCoreSupport/`.
* Delete `historyItemChangedImpl` (`BackForwardList.cpp:115-122`),
  `permitEnableScriptsActionMID` (`FrameLoaderClientJava.cpp:77,120-122`),
  `WebPage.fwkPermitEnableScriptsAction` (`WebPage.java:2467-2474`), and
  `ContextMenuClientJava::m_webPage` plus its constructor parameter.
* Verify: `jfxwebkit` still links; the `Java_com_sun_*` export count drops by exactly 4; the full
  `modules/javafx.web` suite plus the three web robot tests pass.

About 390 lines of C++ out, with no facade written for any of it.

### Step 1 -- `FFM-web-1: Add the wkj_* C ABI header beside the JNI entry points`

`Source/WebKitLegacy/java/api/webkit_java_api.h` plus its `.cpp`. Both ABIs compile side by side;
no Java changes. Include the constant enums that replace `com_sun_webkit_*.h` (section 11.1). Add
every symbol to both mapfiles in this commit, and add the mapfile generator if you are going to
(risk 11).

### Step 2 -- `FFM-web-2: WebKitNative facade plus PageCache, BackForwardList, PopupMenu, ColorChooser`

Start with the four small classes, not `WebPage`: 23 downcalls, 13 upcalls, and they exercise the
whole pattern set (registry ids in `bflSetHostObject`, peer tables in `WKJPopupCallbacks`, string
out-params, an array out-param in `bflItemGetChildren`). `BackForwardList` also removes
`HistoryItem::m_hostObject`, the one *per-object* global ref in the slice.
Verify with `HistoryTest`, `HistoryStateTest`, `FormControlsTest`.

### Step 3 -- `FFM-web-3: WebPage downcalls`

The 84 remaining `OS-CALL` downcalls, in four sub-commits so each is reviewable and each has a test:
lifecycle + frame tree (`twkCreatePage` .. `twkGetHtml`); settings (`twkSet*`/`twkGet*`/
`twkOverridePreference`); input (`twkProcess*`, `twkGetTextLocation`, IME); paint + print. Leave
`twkUpdateContent`/`twkPostPaint`/`twkPrint` and `twkExecuteScript` for later -- they depend on the
graphics and LiveConnect slices (risk 6, section 5.1).

### Step 4 -- `FFM-web-4: client callback tables`

`WKJChromeCallbacks` (largest, 22 slots), then `WKJFrameLoaderCallbacks`, then the one-slot
`WKJEditorCallbacks`, `WKJInspectorCallbacks`, `WKJProgressCallbacks`, `WKJPageNotifyCallbacks`.
`WKJDragCallbacks` waits on graphics. Delete `initRefs`, every `JGClass`, every `FindClass` and
every `jmethodID` in each file as it is converted. After this step no `Java_com_sun_webkit_WebPage_*`
symbol remains.

### Step 5 -- `FFM-web-5: DumpRenderTree harness`

`WKJDrtCallbacks` + `WKJEventSenderCallbacks`, then delete the JNI half of `JavaEnv.cpp` including
`JNI_OnLoad`/`JNI_OnUnLoad`, and drop `${JAVA_JVM_LIBRARY}` and the JNI include paths from both
`Tools/*/java/CMakeLists.txt`. Verify by running a `LayoutTests` subset by hand and diffing.

### Step 6 -- `FFM-web-6: Java reimplementations` (separate, behaviour-affecting)

`twkGetIconURL` and `bflItemGetIcon` become `return null` in Java; `twkDoJSCGarbageCollection` binds
the already-exported `WebPage_doJSCGarbageCollection` directly. Each with the parity test named in
section 9.2. Nothing else in this slice is a Java-reimplementation candidate.

### Ordering against the rest of the module

This slice cannot finish before `Source/WebCore/platform/graphics/java` (three upcalls and three
downcalls carry graphics jobjects, risk 6) and cannot finish before
`Source/WTF/wtf/java/JavaEnv.{h,cpp}` and `JavaRef.h` are replaced (the `JGObject`/`JLObject` types
are in every client header). Steps 0-3 are independent of both and can start now. The DOM slice
(`Source/WebKitLegacy/java/DOM/`) is independent of this one except for `twkGetDocument` and
`twkGetOwnerElement`, which are declared on `WebPage` but implemented in
`Source/WebCore/bindings/java/JavaDOMUtils.cpp` -- decide there, not here.
