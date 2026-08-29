# FFM audit — WTF + WebCore Java-port layer (`modules/javafx.web`)

Scope: `Source/WTF/wtf/java/**`, `Source/WebCore/platform/java/**`,
`Source/WebCore/platform/graphics/java/**`, `Source/WebCore/platform/network/java/**`,
`Source/WebCore/bindings/java/**` (excluding the pre-generated `dom3/java` Java tree),
`Source/WebCore/bridge/jni/**`, `Source/WebCore/PAL/pal/**`, plus the stray JNI references in
`Source/WebCore/platform/{Widget,Cursor,PlatformKeyboardEvent,PlatformTouchEvent}.h`,
`Source/WTF/wtf/{URL.h,text/WTFString.h,unicode/**}`.
`Source/WebKitLegacy/**` (`WebPage.cpp`, `WebCoreSupport/*`, the ~105 `DOM/Java*.cpp` files) is
**out of scope** and audited separately.

All paths are relative to `modules/javafx.web/src/main/native/` unless they start with
`modules/`. Read-only audit; nothing in the tree was modified.

---

## 1. Verdict

**Split it, and delete more than you migrate.**

The scope holds **53 `JNIEXPORT` functions** (50 `Java_*` + `JNI_OnLoad`/`JNI_OnLoad_jfxwebkit`/
`JNI_OnUnload`) and **230 `Call*Method` upcall sites** across ~22,200 lines of C++. Unlike
graphics/media, the dominant direction here is *outbound*: WebKit C++ calling Java, not Java calling
C++. That inverts the usual migration shape — the deliverable is mostly a **callback table
(`WebKitJavaHost`)**, not a downcall facade.

Triage over the 53 exports and the 51 distinct C++ entry points that own upcalls:

| Verdict | Count | Where |
|---|---|---|
| `OS-CALL` (engine/JSC/platform, stays native) | 27 | `BridgeUtils.cpp` JS half, `MediaPlayerPrivateJava`, `SharedBufferJava`, `RenderingQueue`, `JavaEventListener`, `URLLoader`, `SocketStreamHandleImplJava`, `MainThreadJava`, `MainThreadSharedTimerJava`, `JavaDOMUtils` |
| `WRAPPER` (marshalling only; Java binds/implements directly) | 14 | `WidgetJava_initIDs`, `WCPluginWidget_initIDs`, `IDNJava`, `LocalizedStringsJava`, `PL_*` perf hooks, the `valueOf` boxing block in `JNIUtilityPrivate.cpp:157-299`, `SoundJava` |
| `PURE` (computation/dead only) | 10 | `JavaMath.h`, `DbgUtils.h`, `RunLoopJava.cpp`, `UnicodeJava.{h,cpp}`, `TextCodecJava`, `TextNormalizerJava`, `TextBreakIteratorJava`, `TextBreakIteratorInternalICUJava`, `CPUTimeJava`, `JavaNodeFilterCondition` |
| `PURE-HOT` | 0 | none — no SIMD/intrinsics anywhere in scope (`grep -rnE 'emmintrin\|xmmintrin\|immintrin\|arm_neon\|__asm\|__builtin_ia32'` -> 0 hits in scope) |

Parity outcomes: **13 files are `PARITY: exact` deletions** because they are *not compiled at all* or
compile to nothing — a combined **1,702 lines** that can be removed today, before any FFM work,
with a build as the only proof required. (An earlier draft of this paragraph said "7 files / 1,847
lines"; both were wrong. **Section 15 is the authoritative, actionable list** and explains the
correction.) **2 items are `PARITY: unprovable` and stay native**
(`JavaMath.h::hypot`, `StringJava.cpp`). The `bridge/jni` LiveConnect layer splits: its
JavaScriptCore half is `OS-CALL` and stays; its Java-reflection half (~1,400 lines across
`JavaClassJSC`/`JavaFieldJSC`/`JavaMethodJSC`/`JNIUtility.h`) is `WRAPPER` over `java.lang.reflect`
and should move to Java — but only behind `JavaScriptBridgeTest` (22 tests, 755 lines), and
`PARITY: unknown` until overload-resolution equivalence is measured.

The single highest-value artefact of this audit is the `WebKitJavaHost` design in section 3.4: **it is the
prerequisite for every other phase**, it touches ~370 `JL*`/`JG*` sites and 480 `GetJavaEnv`/
`CheckAndClearException` sites, and getting its ownership semantics wrong silently corrupts
reference counts across the entire WebKit port.

---

## 2. Summary counts

Measured with the greps recorded beside each figure, over
`--include=*.cpp --include=*.h --include=*.mm`.

| Metric | Scope | Whole native tree |
|---|---|---|
| Files including `jni.h` or using `JNIEnv` | 77 | **209** |
| `JNIEXPORT` functions | **53** (50 `Java_*` + 3 `JNI_On*`) | — |
| Java `native` methods reaching this scope | 39 | 182 (module total) |
| `Call*Method` upcall sites | **230** | — |
| `GetMethodID`/`GetStaticMethodID`/`Get*FieldID` sites | **231** (215 method, 16 field) | — |
| `FindClass` sites / distinct class names | 84 / **49** | — |
| `NewGlobalRef`/`DeleteGlobalRef` outside `JavaRef.h` | **5 / 4** | — |
| `Get*ArrayCritical` / release pairs | **14 / 14** | — |
| `NewDirectByteBuffer` / `GetDirectBufferAddress` | **5 / 4** | — |
| `AttachCurrentThread` call sites | 2 (1 dead) | 2 |
| `AttachThreadToJavaEnv` *use* sites | 2 in scope | **7** |
| Exceptions raised from C (`Throw`/`ThrowNew`) | **4** | 4 |
| `GetStringUTFChars`/`NewStringUTF` (modified UTF-8) | 3 / 11 (6 live) | — |
| `PushLocalFrame`/`PopLocalFrame` | 1 / 1 | 1 / 1 |
| `RegisterNatives`, `MonitorEnter` | **0 / 0** | 0 / 0 |

Greps used, run from `modules/javafx.web/src/main/native/Source`:

```bash
SCOPE="WTF/wtf/java WebCore/platform/java WebCore/platform/graphics/java \
       WebCore/platform/network/java WebCore/bindings/java WebCore/bridge/jni \
       WebCore/PAL WTF/wtf/text WTF/wtf/unicode"
grep -rhoE 'JNIEXPORT [a-z]+ JNICALL (Java_[A-Za-z0-9_]+|JNI_On[A-Za-z_]+)' --include=*.cpp --include=*.h $SCOPE | sort -u | wc -l
grep -rnoE 'Call(Static|Nonvirtual)?[A-Za-z]*Method[AV]?'                    --include=*.cpp --include=*.h $SCOPE | wc -l
grep -rhoE 'FindClass\(\s*"[^"]+"'                                           --include=*.cpp --include=*.h $SCOPE | sort -u | wc -l
```

---

## 3. JavaEnv / JavaRef anatomy — the load-bearing part

### 3.1 What `JavaEnv.h` provides

| Symbol | Definition | Purpose |
|---|---|---|
| `JavaVM* jvm` | decl `WTF/wtf/java/JavaEnv.h:32`, def `JavaEnv.cpp:30` | set in `JNI_OnLoad` (`JavaEnv.cpp:135`), zeroed in `JNI_OnUnload` (`:159`) |
| `volatile bool g_ShuttingDown` | `JavaEnv.h:33`, `JavaEnv.cpp:31` | set by `MainThread.twkSetShutdown` (`MainThreadJava.cpp:131`); read at `JavaEnv.h:87` and `WebCore/platform/ThreadTimers.cpp:76` |
| `WC_GETJAVAENV_CHKRET(env, ret)` | `JavaEnv.h:35` | `GetJavaEnv()` + early return on null |
| `WTF::GetJavaEnv()` | `JavaEnv.h:41` | `jvm->GetEnv(JNI_VERSION_1_2)`; **does not attach**, returns null on a detached thread |
| `WTF::CheckAndClearException(JNIEnv*)` | `JavaEnv.cpp:36` | `ExceptionCheck` -> `ExceptionDescribe` -> `ExceptionClear`, returns whether one was pending |
| `PL_GetLogger` / `PL_ResumeCount` / `PL_SuspendCount` / `PL_IsEnabled` | `JavaEnv.h:50-53`, `JavaEnv.cpp:54/69/82/95` | `com.sun.webkit.perf.PerfLogger` hooks |
| `EntryJavaLogger` + `LOG_PERF_RECORD` | `JavaEnv.h:56-77`, `:124` | RAII scope timer |
| `AttachThreadToJavaEnv<daemon>` (+ 2 aliases) | `JavaEnv.h:83-118` | `AttachCurrentThread(AsDaemon)` on ctor, `DetachCurrentThread` on dtor **only if it attached** (`m_status == JNI_EDETACHED`) |
| `jlong_to_ptr` / `ptr_to_jlong` / `bool_to_jbool` / `jbool_to_bool` | `JavaEnv.h:128-132` | casts |
| `JINT_SZ` / `JFLOAT_SZ` | `JavaEnv.h:134-135` | `sizeof(jint)` / `sizeof(jfloat)` |
| `JGClass comSunWebkitFileSystem` | `JavaEnv.h:138`, `JavaEnv.cpp:34` | resolved in `JNI_OnLoad` (`:144-149`) so that `FileSystemJava` works from WebKit-created threads |

`JNI_OnLoad` (`JavaEnv.cpp:117-152`) does exactly three things: MSVC debug-CRT leak flags, `jvm = vm`,
and the eager `FindClass("com/sun/webkit/FileSystem")` with the comment (`:139-143`) explaining
*why*: the class loader that loaded `jfxwebkit` is only reachable during `JNI_OnLoad`; from a
WebKit-spawned thread `FindClass` would use the system loader and fail. **This constraint survives
FFM in a different form** — see section 13.

### 3.2 What `JavaRef.h` provides

| Symbol | Line | Notes |
|---|---|---|
| `JavaScriptCore_GetJavaEnv()` | `JavaRef.h:32` | null-checked twin of `WTF::GetJavaEnv()`; 8 uses |
| `JSC_GETJAVAENV_CHKRET` | `:42` | 1 use |
| `jlong_to_ptr` / `ptr_to_jlong` | `:46-47` | **duplicated** from `JavaEnv.h:128-129` |
| `JLocalRef<T>` | `:53-130` | |
| `JGlobalRef<T>` | `:132-214` | |
| `WrapJavaRef(jref)` | `:216` | identity macro; 1 use |
| `JLString/JLClass/JLObject/JLObjectArray/JLByteArray` | `:218-222` | |
| `JGString/JGClass/JGObject/JGObjectArray` | `:224-227` | |

**Exact ownership semantics that must be preserved** (this is what a replacement gets wrong):

* `JLocalRef(T ref = NULL, bool bycopy = false)` (`:78`) — **adopts** `ref` by default; with
  `bycopy = true` it `NewLocalRef`s. Adoption is the default and is relied on everywhere
  (`JLObject(env->CallObjectMethod(...))`).
* `JLocalRef(const JLocalRef&)` (`:83`) and `JLocalRef(const JGlobalRef&)` (`:88`) — both **copy**
  (`NewLocalRef`). Note the cross-kind constructor: a global can be demoted to a local.
* `JGlobalRef(T ref)` (`:157`) — `m_jref(copy(JLocalRef<T>(ref)))`. The temporary `JLocalRef`
  **adopts and then deletes** `ref`. So **`JGObject g(env->FindClass(...))` consumes the caller's
  local reference** while `JGObject g(someJLObject)` copies it. Two different ownership rules on the
  same constructor name — the most dangerous thing to get wrong in a rewrite.
* `JGlobalRef(const JLocalRef&)` (`:162`) / `JGlobalRef(const JGlobalRef&)` (`:167`) — copy
  (`NewGlobalRef`).
* Destructors (`:93`, `:172`) call `clear()`, which is **null-safe on both `env` and `m_jref`**
  (`:60-66`, `:139-145`) — this is why destruction after VM detach or during shutdown does not crash
  (`RenderingQueue.cpp:86-89` documents relying on it).
* `operator const T&()` (`:100`, `:177`) — implicit raw access; `operator!` on `JLocalRef` only.
* `operator==`/`!=` compare the **referent pointer**, not the wrapper. They are **non-`const` on
  `JLocalRef`** (`:105`, `:110`) and `const` on `JGlobalRef` (`:182`, `:187`).
* `JLocalRef::operator=` (`:115`) guards with `if (other != *this)` — i.e. it compares *referents*.
  Assigning a different wrapper that happens to hold the same `jobject` is therefore a **no-op**, not
  a re-copy. Preserve or deliberately fix; do not change silently.
* `JGlobalRef::operator=(const JGlobalRef&)` (`:192`) has the same referent guard;
  `operator=(const JLocalRef&)` (`:201`) has **no guard** and always clears first.
* `releaseLocal()` (`:124`) / `releaseGlobal()` (`:208`) — surrender ownership without releasing.
  22 and 3 uses respectively.
* **Neither class has a move constructor or move assignment.** Every by-value return of a `JLObject`
  costs a `NewLocalRef` + `DeleteLocalRef` pair.

### 3.3 Call-site counts by symbol

Whole native tree (`grep -rnE '\b<sym>\b' --include=*.cpp --include=*.h --include=*.mm`), so the
numbers include `WebKitLegacy`, which shares these headers:

| Symbol | Sites | Symbol | Sites |
|---|---|---|---|
| `jlong_to_ptr` | **511** | `JLObject` | **158** |
| `CheckAndClearException` | **256** | `JGClass` | 78 |
| `GetJavaEnv` | **224** | `JLString` | 47 |
| `ptr_to_jlong` | 42 | `JGObject` | 42 |
| `bool_to_jbool` | 43 | `JLocalRef` (explicit) | 40 |
| `jbool_to_bool` | 21 | `JLClass` | 24 |
| `WC_GETJAVAENV_CHKRET` | 13 | `releaseLocal` | 22 |
| `JavaScriptCore_GetJavaEnv` | 8 | `JGlobalRef` (explicit) | 16 |
| `AttachThreadAs*ToJavaEnv` | 7 use sites | `JLObjectArray` | 6 |
| `g_ShuttingDown` | 5 | `JLByteArray` | 5 |
| `PL_*` (4 fns) | 11 | `releaseGlobal` | 3 |
| `LOG_PERF_RECORD` | 3 | `JGObjectArray` / `JGString` / `WrapJavaRef` | 1 each |
| `JSC_GETJAVAENV_CHKRET` | 1 | | |

By directory:

| Directory | `GetJavaEnv` | `CheckAndClearException` | `JL*`/`JG*` |
|---|---|---|---|
| `WebCore/platform/graphics` (incl. `java/`) | 76 | 81 | 67 |
| `WebKitLegacy/java/WebCoreSupport` *(out of scope)* | 62 | 67 | 88 |
| `WebCore/platform/java` | 55 | 75 | 97 |
| `WTF/wtf/java` | 14 | 18 | 66 |
| `WebCore/platform/network` | 9 | 9 | 25 |
| `WebCore/PAL/pal` | 4 | 3 | 7 |
| `WebCore/bindings/java` | 3 | 1 | 16 |
| `WebCore/bridge/jni` | 1 | 0 | 46 |
| `WTF/wtf/text`, `WTF/wtf/URL.h`, `WebCore/platform/Widget.h`, `WebCore/history/*`, `WTF/wtf/FileHandle.h` | — | 2 | 12 |

`WebKitLegacy/java/DOM` (105 files) contains **zero** `GetJavaEnv`/`JL*` uses — those files receive
`JNIEnv*` as a JNI parameter and never need the ambient environment. That matters for scheduling:
the DOM bindings can be converted mechanically without the host struct existing first.

### 3.4 Phase-B replacement design

Two artefacts replace all of the above: an installed-once **`WebKitJavaHost`** vtable, and an
opaque **`wkj_handle_t`** with one RAII wrapper.

#### 3.4.1 The handle type

```c
/* Source/WTF/wtf/java/WebKitJavaHost.h - new, JVM-agnostic */
#include <stdint.h>

/* An opaque key into a Java-side registry. 0 is always "no object" and is
 * safe to retain/release. Handles are INTERNED BY OBJECT IDENTITY: two handles
 * to the same Java object compare equal with ==. That property is required by
 * JLocalRef::operator== (JavaRef.h:105) and by
 * FileSystemImpl::JavaHandleMarkableTraits (WTF/wtf/FileHandle.h). */
typedef uint64_t wkj_handle_t;
#define WKJ_NULL ((wkj_handle_t)0)
```

Interning by identity is the design decision that removes the need for a `handle_equals` callback and
keeps the 62 `==`/`!=`/`operator!` sites working unchanged. The Java registry is an
`IdentityHashMap<Object, Long>` plus a `ConcurrentHashMap<Long, Entry>` holding
`{Object ref; AtomicLong count;}`.

#### 3.4.2 `JavaHandle` — the `JLocalRef`/`JGlobalRef` replacement

JNI's local/global distinction exists only because local refs are frame-scoped. FFM has no frames,
so **one refcounted strong-reference type replaces both**, and the aliases keep the ~370 existing
sites compiling:

```cpp
class JavaHandle {
public:
    /* JLocalRef(T ref, bycopy=false) / JGlobalRef(T ref): ADOPT (JavaRef.h:78, :157) */
    static JavaHandle adopt(wkj_handle_t h) { JavaHandle j; j.m_h = h; return j; }
    /* JLocalRef(ref, bycopy=true) (JavaRef.h:78 with bycopy) */
    static JavaHandle retain(wkj_handle_t h) { wkj_host()->handle_retain(h); return adopt(h); }

    JavaHandle() = default;                                   /* JLocalRef() => NULL      */
    explicit JavaHandle(wkj_handle_t h) : m_h(h) {}           /* adopting, matches :78    */
    JavaHandle(const JavaHandle& o) : m_h(o.m_h) { wkj_host()->handle_retain(m_h); }  /* :83/:162/:167 */
    JavaHandle(JavaHandle&& o) noexcept : m_h(o.m_h) { o.m_h = WKJ_NULL; }  /* NEW: JNI version had none */
    ~JavaHandle() { clear(); }

    void clear() { if (m_h) { wkj_host()->handle_release(m_h); m_h = WKJ_NULL; } }  /* :59/:138, null-safe */

    /* JLocalRef::operator= (:115) - referent-compare guard preserved verbatim */
    JavaHandle& operator=(const JavaHandle& o)
    {
        if (o.m_h != m_h) { clear(); m_h = o.m_h; wkj_host()->handle_retain(m_h); }
        return *this;
    }
    JavaHandle& operator=(JavaHandle&& o) noexcept
    {
        if (this != &o) { clear(); m_h = o.m_h; o.m_h = WKJ_NULL; }
        return *this;
    }

    operator wkj_handle_t() const { return m_h; }             /* :100/:177 implicit raw   */
    bool operator!() const { return !m_h; }                   /* :98                      */
    bool operator==(const JavaHandle& o) const { return m_h == o.m_h; }   /* :105/:182     */
    bool operator!=(const JavaHandle& o) const { return m_h != o.m_h; }   /* :110/:187     */

    wkj_handle_t leak() { wkj_handle_t r = m_h; m_h = WKJ_NULL; return r; } /* releaseLocal/Global :124/:208 */

private:
    wkj_handle_t m_h { WKJ_NULL };
};

/* Source-compatibility aliases: 370 existing sites keep compiling unchanged. */
using JLObject = JavaHandle;  using JGObject      = JavaHandle;
using JLString = JavaHandle;  using JGString      = JavaHandle;
using JLClass  = JavaHandle;  using JGClass       = JavaHandle;
using JLObjectArray = JavaHandle; using JGObjectArray = JavaHandle;
using JLByteArray   = JavaHandle;
template<typename T> using JLocalRef  = JavaHandle;  /* T ignored; see note 3 */
template<typename T> using JGlobalRef = JavaHandle;
```

Four behavioural notes the migrator must act on, not discover:

1. **The `JGlobalRef(T raw)` consuming constructor (`JavaRef.h:157`) disappears.** Under a registry
   there is no separate "local" to consume. Every site of the form `JGObject g(env->FindClass(...))`
   becomes `JavaHandle::adopt(host->find_class(...))`, and every
   `JGObject g = someJLObject;` becomes a retaining copy. A scripted rewrite must distinguish the two
   by whether the argument is a raw expression or an existing wrapper — this is the single place a
   blind `sed` will introduce refcount bugs.
2. **Local-frame behaviour is gone, and that is a leak hazard, not a simplification.** JNI reclaimed
   leaked local refs when the native method returned. A registry does not. Every `leak()`/
   `releaseLocal()` site (22) and every raw `wkj_handle_t` returned by a host callback must have a
   named owner. Add a debug-build registry-size assertion at `WCRenderQueue.twkRelease` and at
   `WebPage` teardown to catch regressions.
3. **`JLocalRef<T>` was typed; `JavaHandle` is not.** The `T` was only used for `static_cast`
   at the JNI boundary and never enforced anything. Keeping `template<typename T> using JLocalRef =
   JavaHandle;` preserves compilation; the type-safety loss is real but was never enforced at
   runtime either (JNI would have thrown at the call, not the cast).
4. **Add the move operations.** The JNI classes had none, so every by-value `JLObject` return did a
   `NewLocalRef`/`DeleteLocalRef` round trip. Adding moves is behaviour-neutral and strictly cheaper;
   it belongs in the migration commit, not a follow-up.

#### 3.4.3 `WebKitJavaHost` — every member, with the evidence for it

Installed once via `wkj_init`; `wkj_host()` returns the process-wide pointer. **No member takes or
returns a `JNIEnv*`, `jobject`, or `JavaVM*`.** Strings cross as UTF-16 `(ptr, len)` pairs, matching
`StringJava.cpp` exactly and avoiding modified UTF-8 entirely (section 8.4).

```c
/* Source/WTF/wtf/java/WebKitJavaHost.h */
typedef struct WebKitJavaHost {
    uint32_t struct_size;        /* sizeof(WebKitJavaHost) - ABI drift guard      */
    uint32_t version;

    /* --- handle registry: replaces JLocalRef/JGlobalRef (JavaRef.h) --------- */
    void        (*handle_retain)(wkj_handle_t);          /* NewLocalRef/NewGlobalRef :70/:151 */
    void        (*handle_release)(wkj_handle_t);         /* DeleteLocalRef/DeleteGlobalRef :62/:142 */

    /* --- exceptions: replaces CheckAndClearException (256 sites) ------------ */
    int32_t     (*exception_check_and_clear)(void);      /* JavaEnv.cpp:36; see risk 1        */
    void        (*throw_dom_exception)(int16_t code, const uint16_t* msg, int32_t len);
                                                         /* JavaDOMUtils.cpp:60,69            */
    void        (*throw_js_exception_value)(wkj_handle_t value);   /* BridgeUtils.cpp:239,242  */
    void        (*throw_js_exception_message)(const char* ascii);  /* BridgeUtils.cpp:464      */
    void        (*throw_null_pointer_exception)(void);   /* BridgeUtils.cpp:115-120            */

    /* --- strings: replaces WTF::String(JNIEnv*, JLString) / toJavaString ---- */
    wkj_handle_t (*string_create)(const uint16_t* utf16, int32_t len);  /* StringJava.cpp:68,72 */
    int32_t     (*string_length)(wkj_handle_t);                         /* StringJava.cpp:39    */
    int32_t     (*string_get_chars)(wkj_handle_t, uint16_t* dst, int32_t cap);
                                                         /* replaces GetStringCritical StringJava.cpp:43 */

    /* --- arrays ------------------------------------------------------------- */
    int32_t      (*array_length)(wkj_handle_t);          /* RenderingQueue.cpp:138, BridgeUtils.cpp:476 */
    wkj_handle_t (*array_new_byte)(const uint8_t* src, int32_t len);   /* SocketStreamHandleImplJava.cpp:92 */
    wkj_handle_t (*array_new_char)(const uint16_t* src, int32_t len);  /* GlyphPageTreeNodeJava.cpp:42     */
    int32_t      (*array_get_bytes)(wkj_handle_t, uint8_t* dst, int32_t cap);   /* ImageBufferJavaBackend.cpp:132 */
    int32_t      (*array_get_ints)(wkj_handle_t, int32_t* dst, int32_t cap);    /* ImageDecoderJava.cpp:152/215/283 */
    int32_t      (*array_get_floats)(wkj_handle_t, float* dst, int32_t cap);    /* FontJava.cpp:152, ComplexTextControllerJava.cpp:142 */
    int32_t      (*array_get_longs)(wkj_handle_t, int64_t* dst, int32_t cap);   /* FileSystemJava.cpp:146 */
    wkj_handle_t (*array_get_object)(wkj_handle_t arr, int32_t index);          /* RenderingQueue.cpp:140, BridgeUtils.cpp:479 */

    /* --- direct buffers ----------------------------------------------------- */
    wkj_handle_t (*buffer_wrap)(void* addr, int64_t len);/* NewDirectByteBuffer: RenderingQueue.h:49,
                                                            ImageJava.cpp:93, RenderThemeJava.cpp:202,
                                                            FileSystemJava.cpp:305, CryptoDigestJava.cpp:126 */
    void*        (*buffer_address)(wkj_handle_t, int64_t* out_len);
                                                         /* GetDirectBufferAddress: RenderingQueue.cpp:139,
                                                            ImageBufferJavaBackend.cpp:163, URLLoader.cpp:510,
                                                            TouchEventJava.cpp:49 */

    /* --- WTF::FileSystemImpl: 11 upcalls, FileSystemJava.cpp ---------------- */
    int32_t      (*fs_file_exists)(const uint16_t* path, int32_t len);           /* :80,86  */
    int64_t      (*fs_file_size)(const uint16_t* path, int32_t len);             /* :100,106 -1 = absent */
    int32_t      (*fs_file_metadata)(const uint16_t* path, int32_t len, int64_t out3[3]); /* :131,139 */
    int32_t      (*fs_path_append)(const uint16_t* p, int32_t pl,
                                   const uint16_t* c, int32_t cl,
                                   uint16_t* out, int32_t cap);                  /* :186,206 */
    int32_t      (*fs_make_all_directories)(const uint16_t* path, int32_t len);  /* :226,232 */
    int32_t      (*fs_path_file_name)(const uint16_t* p, int32_t l,
                                      uint16_t* out, int32_t cap);               /* :367,373 */
    wkj_handle_t (*fs_open_read)(const uint16_t* path, int32_t len);             /* :253,259 0 = failed */
    void         (*fs_close)(wkj_handle_t file);                                 /* :275,281 */
    int32_t      (*fs_read)(wkj_handle_t file, void* dst, int32_t len);          /* :295,301 */
    int32_t      (*fs_seek)(wkj_handle_t file, int64_t offset);                  /* :390,396 */

    /* --- main thread / timer ------------------------------------------------ */
    void         (*main_thread_schedule_dispatch)(void); /* MainThreadJava.cpp:52 - ANY thread */
    void         (*timer_set_fire_time)(double seconds); /* MainThreadSharedTimerJava.cpp:47,56 */
    void         (*timer_stop)(void);                    /* MainThreadSharedTimerJava.cpp:59,63 */

    /* --- perf logger: PL_* (JavaEnv.cpp:47-107). Present only if the perf hooks
     * survive triage; section 9 recommends deleting them and these four members. */
    wkj_handle_t (*perf_get_logger)(const char* name);
    void         (*perf_resume)(wkj_handle_t logger, const char* probe);
    void         (*perf_suspend)(wkj_handle_t logger, const char* probe);
    int32_t      (*perf_is_enabled)(wkj_handle_t logger);
} WebKitJavaHost;

/* Installed once, from WebPage's static initializer, before anything else runs. */
JFX_EXPORT void wkj_init(const WebKitJavaHost* host);
JFX_EXPORT void wkj_set_shutdown(int32_t shutting_down);   /* MainThreadJava.cpp:128-132 */
```

The LiveConnect members (`box_*`, `unbox_*`, `value_kind`, `java_invoke`, ...) are deliberately **not**
in this struct — they belong to a separate `WebKitLiveConnectHost` installed by `JSObject`, and most
of them disappear if Phase D (section 9) is done rather than deferred. Listing them here would encode
the "port the reflection layer as-is" mistake into the ABI.

**Deleted outright by this design**, with nothing replacing them: `jvm`, `GetJavaEnv()` (224 sites ->
0), `JavaScriptCore_GetJavaEnv()` (8 -> 0), `WC_GETJAVAENV_CHKRET`/`JSC_GETJAVAENV_CHKRET` (14 -> 0),
`AttachThreadToJavaEnv` and both aliases (7 use sites -> 0; FFM upcall stubs auto-attach and
auto-detach), `JNI_OnLoad`/`JNI_OnLoad_jfxwebkit`/`JNI_OnUnload`, `comSunWebkitFileSystem`,
`bool_to_jbool`/`jbool_to_bool` (64 sites -> plain `int32_t`), `JINT_SZ`/`JFLOAT_SZ`,
`WrapJavaRef`, and one of the two duplicate `jlong_to_ptr` definitions.
`jlong_to_ptr`/`ptr_to_jlong` (553 sites) become plain `(void*)(uintptr_t)` casts per
`jni-pattern-catalog` P12.

---

## 4. Java classes in scope

| Java class | Natives | Library load / init | Notes |
|---|---|---|---|
| `com.sun.webkit.WebPage` | 87 (2 in scope) | `NativeLibLoader.loadLibrary("jfxwebkit")` at `WebPage.java:130` — **the only load site in the module** | `twkGetDocument`, `twkGetOwnerElement` implemented in `bindings/java/JavaDOMUtils.cpp:107,122`; the other 85 are `WebKitLegacy` |
| `com.sun.webkit.SharedBuffer` | 5 | none | `twkCreate/twkSize/twkGetSomeData/twkAppend/twkDispose` |
| `com.sun.webkit.dom.JSObject` | 9 | none | all in `bridge/jni/jsc/BridgeUtils.cpp` |
| `com.sun.webkit.graphics.WCMediaPlayer` | 10 | none | instance natives, `long nPtr` first arg |
| `com.sun.webkit.network.URLLoaderBase` | 6 | none | all `protected static native`, `long data` peer |
| `com.sun.webkit.network.SocketStreamHandle` | 4 | none | `private static native`, `long data` peer |
| `com.sun.webkit.WCPluginWidget` | 4 | `static { initIDs(); }` at `:51` | `long pData` field read from C (`PluginWidgetJava.cpp:84`) |
| `com.sun.webkit.MainThread` | 2 | none | `twkScheduleDispatchFunctions`, `twkSetShutdown` |
| `com.sun.webkit.WCWidget` | 1 (`initIDs`) | `static { initIDs(); }` at `:36` | pure ID-caching |
| `com.sun.webkit.Timer` | 1 | none | `twkFireTimerEvent` |
| `com.sun.webkit.ContextMenu` | 1 | none | `twkHandleItemSelected(long, int)` |
| `com.sun.webkit.graphics.WCRenderQueue` | 1 | none | `twkRelease(Object[])`; extends `Ref` |
| `com.sun.webkit.graphics.WCGraphicsManager` | 1 | none | `append(long, byte[], int)`; **owns the `refMap` registry** (`:150-164`) |
| `com.sun.webkit.dom.EventListenerImpl` | 3 | none | in the pre-generated `dom3/java` tree, `:80,114,129` |
| **`com.sun.webkit.FileSystem`** | **0** | none | pure upcall target: 11 `fwk*` statics over `java.io.File`/`RandomAccessFile` |
| **`com.sun.webkit.graphics.WCFont`** | **0** | none | pure upcall target: 11 metric/glyph methods |
| **`com.sun.webkit.plugin.Plugin`** | **0** | none | pure upcall target |
| **`com.sun.javafx.webkit.prism.WCImageDecoderImpl`** | **0** | none | pure upcall target of `ImageDecoderJava.cpp` |

Also carrying `@Native` constants consumed by the C++ through generated headers (230 constants over
16 classes): `GraphicsDecoder` (53), `RenderTheme`, `ScrollBarTheme`, `WCPath`, `WCPathIterator`,
`WCRenderQueue`, `LoadListenerClient`, `URLLoaderBase`, `ContextMenuItem`, `CursorManager`,
`WCKeyEvent`, `WCMouseEvent`, `WCFocusEvent`, `RenderMediaControls`, `TextBreakIterator`,
`TextNormalizer`.

---

## 5. C files in scope

| Area | Files | Lines | `JNIEXPORT` | Upcalls |
|---|---|---|---|---|
| `WTF/wtf/java` | 12 | 1,987 | 5 | 16 |
| `WebCore/platform/java` | 53 | 7,628 | 7 | 47 |
| `WebCore/platform/graphics/java` | 35 | 7,385 | 12 | 78 |
| `WebCore/platform/network/java` | 17 | 1,964 | 10 | 12 |
| `WebCore/bindings/java` (excl. `dom3`) | 8 | 801 | 5 | 5 |
| `WebCore/bridge/jni` (+`jsc`) | 27 | 4,163 | 9 | 38 |
| `WebCore/PAL/pal/{crypto,system,java}` | 3 | ~260 | 0 | 5 |

Build membership is declared in `WTF/wtf/PlatformJava.cmake` (`WTF_SOURCES`, 6 files) and
`WebCore/SourcesJava.txt` (unified sources, 108 files) plus `WebCore/PAL/pal/PlatformJava.cmake`
(4 files). **These lists are not exercised by this repository's Maven build** — `javafx.web/pom.xml`
compiles Java only. See section 12.

---

## 6. Full `JNIEXPORT` -> `wkj_*` ABI table

`twk*` natives are **downcalls** (Java -> C). The `wkj_*` column is the proposed flat C ABI for the
ones that stay native; "delete" means Java replaces the C entirely.

### 6.1 `WTF/wtf/java`

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions` `MainThreadJava.cpp:117` | `MainThread.twkScheduleDispatchFunctions()` | `void wkj_main_thread_dispatch_functions(void);` |
| `Java_com_sun_webkit_MainThread_twkSetShutdown` `:128` | `MainThread.twkSetShutdown(boolean)` | `void wkj_set_shutdown(int32_t shutting_down);` |
| `JNI_OnLoad` / `JNI_OnLoad_jfxwebkit` / `JNI_OnUnload` `JavaEnv.cpp:117,119,154` | — | **delete**; replaced by `void wkj_init(const WebKitJavaHost*)` (section 3.4.3) |

### 6.2 `WebCore/platform/java`

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `Java_com_sun_webkit_SharedBuffer_twkCreate` `SharedBufferJava.cpp:43` | `SharedBuffer.twkCreate()` | `void* wkj_shared_buffer_create(void);` |
| `..._twkSize` `:50` | `twkSize(long)` | `int64_t wkj_shared_buffer_size(void* buf);` |
| `..._twkGetSomeData` `:58` | `twkGetSomeData(long,long,byte[],int,int)` | `int32_t wkj_shared_buffer_get_some_data(void* buf, int64_t pos, uint8_t* dst, int32_t off, int32_t len);` — `critical(true)` |
| `..._twkAppend` `:89` | `twkAppend(long,byte[],int,int)` | `void wkj_shared_buffer_append(void* buf, const uint8_t* src, int32_t off, int32_t len);` — `critical(true)` |
| `..._twkDispose` `:106` | `twkDispose(long)` | `void wkj_shared_buffer_dispose(void* buf);` — **body is currently a no-op that leaks** (`:106-112`); the ABI must not enshrine the leak |
| `Java_com_sun_webkit_Timer_twkFireTimerEvent` `MainThreadSharedTimerJava.cpp:76` | `Timer.twkFireTimerEvent()` | `void wkj_timer_fire(void);` |
| `Java_com_sun_webkit_ContextMenu_twkHandleItemSelected` `ContextMenuJava.cpp:241` | `ContextMenu.twkHandleItemSelected(long,int)` | `void wkj_context_menu_item_selected(void* controller, int32_t action);` |
| `Java_com_sun_webkit_WCWidget_initIDs` `WidgetJava.cpp:225` | `WCWidget.initIDs()` | **delete** (`WRAPPER`; the 5 IDs become `WkjWidgetCallbacks`, section 7.1) |
| `Java_com_sun_webkit_WCPluginWidget_initIDs` `PluginWidgetJava.cpp:63` | `WCPluginWidget.initIDs()` | **delete** (`WRAPPER`; becomes `WkjPluginWidgetCallbacks`) |
| `..._twkInvalidateWindowlessPluginRect` `:108` | `twkInvalidateWindowlessPluginRect(int,int,int,int)` | `void wkj_plugin_widget_invalidate_rect(void* pw, int32_t x, int32_t y, int32_t w, int32_t h);` — `pData` moves from a `GetLongField` (`:111`) to an explicit parameter |
| `..._twkSetPlugunFocused` `:116` (sic: the typo is in the ABI today) | `twkSetPlugunFocused(boolean)` | `void wkj_plugin_widget_set_focused(void* pw, int32_t focused);` — spelling fixable now |
| `..._twkConvertToPage` `:124` | `twkConvertToPage(WCRectangle)` | `void wkj_plugin_widget_convert_to_page(void* pw, const float in_xywh[4], float out_xywh[4]);` — removes an object arg, 4 `GetFloatField` reads and a `NewObject` |

### 6.3 `WebCore/platform/graphics/java`

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `Java_com_sun_webkit_graphics_WCGraphicsManager_append` `BitmapImageJava.cpp:104` | `WCGraphicsManager.append(long,byte[],int)` | `void wkj_shared_buffer_builder_append(void* builder, const uint8_t* data, int32_t count);` — `critical(true)`; today `JNI_ABORT` (read-only), so heap-segment write-visibility is a non-issue |
| `Java_com_sun_webkit_graphics_WCRenderQueue_twkRelease` `RenderingQueue.cpp:127` | `WCRenderQueue.twkRelease(Object[])` | `void wkj_rq_release(void* const* buffer_addrs, int32_t count);` — Java passes the direct-buffer addresses it already holds; removes `GetArrayLength` + `GetObjectArrayElement` + `GetDirectBufferAddress` per element |
| `..._WCMediaPlayer_notifyNetworkStateChanged` `MediaPlayerPrivateJava.cpp:797` | `notifyNetworkStateChanged(long,int)` | `void wkj_media_notify_network_state(void* player, int32_t state);` |
| `..._notifyReadyStateChanged` `:804` | `(long,int)` | `void wkj_media_notify_ready_state(void* player, int32_t state);` |
| `..._notifyPaused` `:811` | `(long,boolean)` | `void wkj_media_notify_paused(void* player, int32_t paused);` |
| `..._notifySeeking` `:818` | `(long,boolean,int)` | `void wkj_media_notify_seeking(void* player, int32_t seeking, int32_t ready_state);` |
| `..._notifyFinished` `:825` | `(long)` | `void wkj_media_notify_finished(void* player);` |
| `..._notifyReady` `:832` | `(long,boolean,boolean,float)` | `void wkj_media_notify_ready(void* player, int32_t has_video, int32_t has_audio, float duration);` |
| `..._notifyDurationChanged` `:842` | `(long,float)` | `void wkj_media_notify_duration_changed(void* player, float duration);` |
| `..._notifySizeChanged` `:851` | `(long,int,int)` | `void wkj_media_notify_size_changed(void* player, int32_t w, int32_t h);` |
| `..._notifyNewFrame` `:858` | `(long)` | `void wkj_media_notify_new_frame(void* player);` |
| `..._notifyBufferChanged` `:865` | `(long,float[],int)` | `void wkj_media_notify_buffer_changed(void* player, const float* ranges, int32_t count, int32_t bytes_loaded);` — `critical(true)`; replaces the `GetFloatArrayElements`/`isCopy` dance at `:871-880` |

### 6.4 `WebCore/platform/network/java`

All six `URLLoaderBase` natives and all four `SocketStreamHandle` natives take an opaque `long data`
peer and are invoked **on the FX event thread**, not the network thread — `URLLoader.java:785-791`
routes every one through `Invoker.invokeOnEventThread` when `asynchronous`.

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `..._URLLoaderBase_twkDidSendData` `URLLoader.cpp:447` | `(long,long,long)` | `void wkj_url_loader_did_send_data(void* target, int64_t sent, int64_t to_send);` |
| `..._twkWillSendRequest` `:457` | `(int,String,String,long,String,String,long)` | `void wkj_url_loader_will_send_request(void* target, const WkjResponseInfo* info);` |
| `..._twkDidReceiveResponse` `:479` | same shape | `void wkj_url_loader_did_receive_response(void* target, const WkjResponseInfo* info);` |
| `..._twkDidReceiveData` `:501` | `(ByteBuffer,int,int,long)` | `void wkj_url_loader_did_receive_data(void* target, const uint8_t* data, int32_t position, int32_t remaining);` — Java passes `MemorySegment.ofBuffer(bb)`; removes `GetDirectBufferAddress` |
| `..._twkDidFinishLoading` `:516` | `(long)` | `void wkj_url_loader_did_finish_loading(void* target);` |
| `..._twkDidFail` `:526` | `(int,String,String,long)` | `void wkj_url_loader_did_fail(void* target, int32_t code, const uint16_t* url, int32_t url_len, const uint16_t* msg, int32_t msg_len);` |
| `..._SocketStreamHandle_twkDidOpen` `SocketStreamHandleImplJava.cpp:163` | `(long)` | `void wkj_socket_did_open(void* handle);` |
| `..._twkDidReceiveData` `:173` | `(byte[],int,long)` | `void wkj_socket_did_receive_data(void* handle, const uint8_t* data, int32_t len);` — `critical(true)`; today `GetByteArrayElements` + `JNI_ABORT` |
| `..._twkDidFail` `:185` | `(int,String,long)` | `void wkj_socket_did_fail(void* handle, int32_t code, const uint16_t* desc, int32_t len);` |
| `..._twkDidClose` `:195` | `(long)` | `void wkj_socket_did_close(void* handle);` |

```c
/* Replaces the 6-argument response marshalling repeated at URLLoader.cpp:457 and :479
   and the shared setupResponse() at :~400-445. */
typedef struct WkjResponseInfo {
    int32_t         status;
    int64_t         content_length;
    const uint16_t* content_type;      int32_t content_type_len;
    const uint16_t* content_encoding;  int32_t content_encoding_len;
    const uint16_t* headers;           int32_t headers_len;
    const uint16_t* url;               int32_t url_len;
} WkjResponseInfo;
```

### 6.5 `WebCore/bindings/java`

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `Java_com_sun_webkit_WebPage_twkGetDocument` `JavaDOMUtils.cpp:107` | `WebPage.twkGetDocument(long)` | `void* wkj_frame_get_document(void* frame);` — returns a ref-ed `Node*`; Java calls `NodeImpl.getImpl(ptr)` itself, removing the `FindClass` + `CallStaticObjectMethod` at `:96-103` |
| `..._twkGetOwnerElement` `:122` | `WebPage.twkGetOwnerElement(long)` | `void* wkj_frame_get_owner_element(void* frame);` — same |
| `..._EventListenerImpl_twkCreatePeer` `JavaEventListener.cpp:95` | `EventListenerImpl.twkCreatePeer()` | `void* wkj_event_listener_create(uint64_t listener_handle);` — the `jobject self` becomes an explicit registry handle |
| `..._twkDisposeJSPeer` `:101` | `(long)` | `void wkj_event_listener_dispose_js_peer(void* listener);` |
| `..._twkDispatchEvent` `:109` | `(long,long)` | `void wkj_event_listener_dispatch_event(void* listener, void* event);` |

### 6.6 `WebCore/bridge/jni/jsc/BridgeUtils.cpp` — `JSObject`

These nine are the LiveConnect boundary. Each currently returns or accepts arbitrary `jobject`s; the
proposed ABI uses a tagged union so C never constructs a Java object (`jni-pattern-catalog` P11).

| C symbol | Java method | Proposed `wkj_*` prototype |
|---|---|---|
| `..._JSObject_evalImpl` `:325` | `evalImpl(long,int,String)` | `int32_t wkj_js_eval(void* peer, int32_t peer_type, const uint16_t* src, int32_t len, WkjJSValue* out);` |
| `..._getMemberImpl` `:343` | `(long,int,String)` | `int32_t wkj_js_get_member(void* peer, int32_t peer_type, const uint16_t* name, int32_t len, WkjJSValue* out);` |
| `..._setMemberImpl` `:364` | `(long,int,String,Object,Object)` | `int32_t wkj_js_set_member(void* peer, int32_t peer_type, const uint16_t* name, int32_t len, const WkjJSValue* value);` |
| `..._removeMemberImpl` `:388` | `(long,int,String)` | `int32_t wkj_js_remove_member(void* peer, int32_t peer_type, const uint16_t* name, int32_t len);` |
| `..._getSlotImpl` `:407` | `(long,int,int)` | `int32_t wkj_js_get_slot(void* peer, int32_t peer_type, int32_t index, WkjJSValue* out);` |
| `..._setSlotImpl` `:422` | `(long,int,int,Object,Object)` | `int32_t wkj_js_set_slot(void* peer, int32_t peer_type, int32_t index, const WkjJSValue* value);` |
| `..._toStringImpl` `:437` | `(long,int)` | `int32_t wkj_js_to_string(void* peer, int32_t peer_type, uint16_t* out, int32_t cap, int32_t* out_len);` |
| `..._callImpl` `:453` | `(long,int,String,Object[],Object)` | `int32_t wkj_js_call(void* peer, int32_t peer_type, const uint16_t* name, int32_t nlen, const WkjJSValue* args, int32_t argc, WkjJSValue* out);` |
| `..._unprotectImpl` `:494` | `(long,int)` | `void wkj_js_unprotect(void* peer, int32_t peer_type);` |

```c
/* Replaces Java_Object_to_JSValue (BridgeUtils.cpp:138-207) and
   convertValueToJValue (JNIUtilityPrivate.cpp:77-~300) at the boundary.
   Return code: 0 = ok, 1 = JS exception (payload in *out), 2 = null peer. */
typedef enum {
    WKJ_JS_NULL = 0, WKJ_JS_UNDEFINED, WKJ_JS_BOOLEAN, WKJ_JS_NUMBER,
    WKJ_JS_STRING, WKJ_JS_JSOBJECT, WKJ_JS_DOM_NODE, WKJ_JS_JAVA_OBJECT
} WkjJSValueKind;

typedef struct WkjJSValue {
    int32_t  kind;             /* WkjJSValueKind                                            */
    int32_t  peer_type;        /* JS_CONTEXT_OBJECT / JS_DOM_NODE_OBJECT / JS_DOM_WINDOW_OBJECT
                                  see JSObject.java:36-38                                   */
    double   number;           /* WKJ_JS_NUMBER / WKJ_JS_BOOLEAN (0.0 or 1.0)               */
    void*    peer;             /* WKJ_JS_JSOBJECT: gc-protected JSObjectRef
                                  WKJ_JS_DOM_NODE: ref-ed Node*                             */
    uint64_t java_handle;      /* WKJ_JS_JAVA_OBJECT: registry id of the original Java object */
    const uint16_t* string;    /* WKJ_JS_STRING, owned by C until the next call on this thread */
    int32_t  string_len;
} WkjJSValue;
```

---

## 7. Upcalls, grouped by target Java class

230 `Call*Method` sites, 231 cached `jmethodID`/`jfieldID` values. Threads below are stated from the
evidence named in the last column, not inferred from naming.

| Target Java class | Sites | C file(s) | Thread | Evidence |
|---|---|---|---|---|
| `graphics.WCFont` | 13 | `FontJava.cpp:51-152`, `FontPlatformDataJava.cpp:46-125`, `GlyphPageTreeNodeJava.cpp:53`, `ComplexTextControllerJava.cpp:221` | WebKit main | reached from layout/paint on the main thread |
| `graphics.WCPath` | 16 | `PathJava.cpp:169-483`, `PlatformContextJava.h:74` | WebKit main | |
| `graphics.WCMediaPlayer` | 14 | `MediaPlayerPrivateJava.cpp:291-619` | WebKit main | `MediaPlayerPrivate` is main-thread by WebKit contract |
| `graphics.WCImageDecoder` / `WCImageFrame` | 11 | `ImageDecoderJava.cpp:68-336` | WebKit main **and** decoder threads | driven from `BitmapImage`; cross-check `WorkQueueGeneric.cpp:73,96` attach sites |
| `graphics.WCGraphicsManager` | 9 | `PathJava.cpp:63,76,104`, `ImageBufferJavaBackend.cpp:48,67`, `ImageJava.cpp:88`, `GraphicsContextJava.cpp:855`, `MediaPlayerPrivateJava.cpp:239,274`, `BitmapImageJava.cpp:74`, `FontCustomPlatformData.cpp:90`, `FontPlatformDataJava.cpp:46` | WebKit main | |
| `graphics.WCRenderQueue` | 5 | `RenderingQueue.cpp:76,91,108`, `FontCascadeJava.cpp:59,82` | WebKit main | `RenderingQueue.h:99-105` and `RenderingQueue.cpp:100` both say "Event thread" |
| `graphics.Ref` | 3 | `RQRef.cpp:39,52,56` | WebKit main, plus destructor on any thread | `RQRef.cpp:37` guards on `env` being null |
| `graphics.WCImage` | 3 | `ImageBufferJavaBackend.cpp:120,151,174` | WebKit main | |
| `graphics.WCTextRun` | 7 | `ComplexTextControllerJava.cpp:50-135` | WebKit main | |
| `graphics.RenderTheme` / `ScrollBarTheme` | 7 | `RenderThemeJava.cpp:188,278,376,448`, `ScrollbarThemeJava.cpp:86,106,158,227` | WebKit main (paint) | |
| `com.sun.webkit.FileSystem` | 11 | `FileSystemJava.cpp:80-390` | **any thread** | reached from `AsyncFileStream.cpp:93` and `WorkerThread.cpp:147`; `JavaEnv.cpp:139-149` exists solely because of this |
| `com.sun.webkit.MainThread` | 1 | `MainThreadJava.cpp:52` | **any thread** | `AttachThreadAsNonDaemonToJavaEnv` at `:49` |
| `com.sun.webkit.Timer` | 2 | `MainThreadSharedTimerJava.cpp:47,59` | WebKit main | `MainThreadSharedTimer` is main-thread by contract |
| `com.sun.webkit.ContextMenu` / `ContextMenuItem` | 10 | `ContextMenuJava.cpp:51-221` | WebKit main | |
| `com.sun.webkit.WCWidget` | 5 | `WidgetJava.cpp:228-240` | WebKit main | |
| `com.sun.webkit.WCPluginWidget` | 4 | `PluginWidgetJava.cpp:65-91` | WebKit main | |
| `com.sun.webkit.CursorManager` | 3 | `CursorJava.cpp:48,72,235` | WebKit main | |
| `network.URLLoaderBase` / `NetworkContext` / `FormDataElement` | 5 | `URLLoader.cpp:76,90,98,104` | WebKit main | `URLLoader::load` is called from `ResourceHandle` on the main thread |
| `network.SocketStreamHandle` | 4 | `SocketStreamHandleImplJava.cpp:56,78,99,116` | WebKit main | |
| `network.CookieJar` (`NetworkStorageSession`) | 2 | `NetworkStorageSessionJava.cpp:56,62` | WebKit main | |
| `dom.EventListenerImpl` | 2 | `JavaEventListener.cpp:60,81` | WebKit main / JS thread | `handleEvent` runs inside event dispatch |
| `dom.NodeImpl` | 2 | `JavaDOMUtils.cpp:97`, `JNIUtilityPrivate.cpp:125` | JS thread, JSC lock held | `JSLockHolder` at `JNIUtilityPrivate.cpp:79` |
| `dom.JSObject` + `java.lang.*` boxing | 20 | `BridgeUtils.cpp:119-242`, `JNIUtilityPrivate.cpp:135-299` | JS thread, JSC lock held | |
| `com.sun.webkit.Utilities` | 1 | `JNIUtilityPrivate.cpp:326` | JS thread | `fwkInvokeWithContext` — **does the actual `Method.invoke`** |
| `security.WCMessageDigest` | 3 | `CryptoDigestJava.cpp:52,121,138` | WebKit main / worker | WebCrypto is available in workers |
| `com.sun.webkit.LocalizedStrings` | 1 | `LocalizedStringsJava.cpp:45` | WebKit main | |
| `java.net.IDN` | 1 | `IDNJava.cpp:42` | WebKit main | |
| `java.awt.Toolkit` | 2 | `SoundJava.cpp:41,50` | WebKit main | **pulls AWT into a JavaFX process** |
| `perf.PerfLogger` | 4 | `JavaEnv.cpp:57,72,85,98` | any | |
| `plugin.PluginManager` / `PluginHandler` | 14 | `PluginDataJava.cpp:48-119`, `PluginInfoStoreJava.cpp:46-193` | WebKit main | |
| dead (see section 11) `text.TextCodec`, `text.TextBreakIterator`, `text.TextNormalizer`, `text.StringCase` | 9 | `TextCodecJava.cpp`, `TextBreakIteratorJava.cpp`, `TextNormalizerJava.cpp`, `UnicodeJava.cpp` | — | not compiled |

### 7.1 Proposed callback tables

One table per class, each carrying `void* user` (a registry id, never a Java reference), installed at
the point where the C++ object is created. All stubs live in `Arena.ofShared()` — `FileSystem`,
`MainThread` and `Ref` are reached from non-Java threads.

```c
/* Source/WebCore/platform/graphics/java/wkj_graphics_callbacks.h */

typedef struct WkjFontCallbacks {          /* WCFont - FontJava.cpp, GlyphPageTreeNodeJava.cpp */
    float   (*get_x_height)(void* user);              /* FontJava.cpp:51  */
    float   (*get_cap_height)(void* user);            /* :57  */
    float   (*get_ascent)(void* user);                /* :63  */
    float   (*get_descent)(void* user);               /* :69  */
    float   (*get_line_spacing)(void* user);          /* :75  */
    float   (*get_line_gap)(void* user);              /* :83  */
    int32_t (*has_uniform_line_metrics)(void* user);  /* :100 */
    double  (*get_glyph_width)(void* user, int32_t glyph);                      /* :133 */
    void    (*get_glyph_bounding_box)(void* user, int32_t glyph, float out[4]); /* :152, was ()[F */
    int32_t (*get_glyph_codes)(void* user, const uint16_t* chars, int32_t n,
                               uint16_t* out_glyphs);                 /* GlyphPageTreeNodeJava.cpp:53 */
    int32_t (*hash_code)(void* user);                                 /* FontPlatformDataJava.cpp:125 */
    int32_t (*equals)(void* user, void* other_user);                  /* FontPlatformDataJava.cpp:107 */
    void*   (*derive_font)(void* user, float size);                   /* FontPlatformDataJava.cpp:85  */
} WkjFontCallbacks;

typedef struct WkjPathCallbacks {          /* WCPath - PathJava.cpp */
    void    (*move_to)(void* user, double x, double y);                        /* :169 */
    void    (*add_line_to)(void* user, double x, double y);                    /* :183 */
    void    (*add_quad_curve_to)(void* user, double, double, double, double);  /* :197 */
    void    (*add_bezier_curve_to)(void* user, double, double, double, double, double, double); /* :211 */
    void    (*add_arc_to)(void* user, double, double, double, double, double); /* :233 */
    void    (*add_arc)(void* user, double, double, double, double, double, int32_t ccw); /* :256 */
    void    (*add_ellipse)(void* user, double, double, double, double);        /* :280 */
    void    (*add_rect)(void* user, double, double, double, double);           /* :296 */
    void    (*add_path)(void* user, void* other_user);                         /* PlatformContextJava.h:74 */
    void    (*close_subpath)(void* user);                                      /* :317 */
    int32_t (*is_empty)(void* user);                                           /* :369 */
    void    (*transform)(void* user, double, double, double, double, double, double); /* :392 */
    int32_t (*contains)(void* user, int32_t rule, double x, double y);         /* :413 */
    int32_t (*stroke_contains)(void* user, double x, double y, double a, double b,
                               int32_t c, int32_t d, double e,
                               const double* f, int32_t flen);                 /* :449 */
    void    (*get_bounds)(void* user, float out_xywh[4]);  /* :483 - removes 4 GetFieldID at :490-496 */
} WkjPathCallbacks;

typedef struct WkjMediaPlayerCallbacks {   /* WCMediaPlayer - MediaPlayerPrivateJava.cpp */
    void  (*dispose)(void* user);                                   /* :291 */
    void  (*load)(void* user, const uint16_t* url, int32_t ulen,
                  const uint16_t* type, int32_t tlen);              /* :313 */
    void  (*cancel_load)(void* user);                               /* :329 */
    void  (*prepare_to_play)(void* user);                           /* :340 */
    void  (*play)(void* user);                                      /* :364 */
    void  (*pause)(void* user);                                     /* :381 */
    float (*get_current_time)(void* user);                          /* :437 */
    void  (*seek)(void* user, float time);                          /* :455 */
    void  (*set_rate)(void* user, float rate);                      /* :483 */
    void  (*set_preserves_pitch)(void* user, int32_t preserve);     /* :494 */
    void  (*set_volume)(void* user, float volume);                  /* :510 */
    void  (*set_mute)(void* user, int32_t mute);                    /* :526 */
    void  (*set_size)(void* user, int32_t w, int32_t h);            /* :575 */
    void  (*set_preload)(void* user, int32_t preload);              /* :619 */
} WkjMediaPlayerCallbacks;

typedef struct WkjRenderQueueCallbacks {   /* WCRenderQueue - RenderingQueue.cpp, FontCascadeJava.cpp */
    void    (*flush)(void* user);                                        /* :76  */
    void    (*dispose_graphics)(void* user);                             /* :91  */
    void    (*add_buffer)(void* user, void* addr, int32_t len);          /* :108, was NewDirectByteBuffer */
    int32_t (*ref_int_array)(void* user, const int32_t* a, int32_t n);   /* FontCascadeJava.cpp:59  */
    int32_t (*ref_float_array)(void* user, const float* a, int32_t n);   /* FontCascadeJava.cpp:82  */
    int32_t (*ref_string)(void* user, const uint16_t* s, int32_t n);     /* WCRenderQueue.java:167  */
} WkjRenderQueueCallbacks;

/* Ref - RQRef.cpp. Note: Java ALREADY assigns the integer id (Ref.java getID(),
 * WCGraphicsManager.java:150-164 refMap). No new registry is needed here. */
typedef struct WkjRefCallbacks {
    void (*ref)(int32_t ref_id);      /* RQRef.cpp:58 */
    void (*deref)(int32_t ref_id);    /* RQRef.cpp:41 - may run on any thread; see :35-37 */
} WkjRefCallbacks;

typedef struct WkjImageDecoderCallbacks { /* WCImageDecoder/WCImageFrame - ImageDecoderJava.cpp */
    void    (*destroy)(void* user);                                          /* :92  */
    void    (*add_image_data)(void* user, const uint8_t* data, int32_t n);   /* :109 */
    void    (*get_image_size)(void* user, int32_t out_wh[2]);                /* :142 */
    int32_t (*get_frame_count)(void* user);                                  /* :167 */
    void*   (*get_frame)(void* user, int32_t index);                         /* :188 */
    void    (*get_frame_size)(void* user, int32_t index, int32_t out_wh[2]); /* :270 */
    int32_t (*get_frame_duration)(void* user, int32_t index);                /* :228 */
    int32_t (*get_frame_complete)(void* user, int32_t index);                /* :308 */
    int32_t (*get_filename_extension)(void* user, uint16_t* out, int32_t cap); /* :336 */
    void    (*get_frame_object_size)(void* frame_user, int32_t out_wh[2]);   /* :203 */
} WkjImageDecoderCallbacks;
```

---


```c
/* Source/WebCore/platform/java/wkj_platform_callbacks.h */

typedef struct WkjWidgetCallbacks {        /* WCWidget - replaces WCWidget_initIDs entirely */
    void (*set_bounds)(void* user, int32_t x, int32_t y, int32_t w, int32_t h); /* WidgetJava.cpp:228 */
    void (*request_focus)(void* user);                                          /* :231 */
    void (*set_cursor)(void* user, int64_t cursor_id);                          /* :234 */
    void (*set_visible)(void* user, int32_t visible);                           /* :237 */
    void (*destroy)(void* user);                                                /* :240 */
} WkjWidgetCallbacks;

typedef struct WkjContextMenuCallbacks {   /* ContextMenu + ContextMenuItem - ContextMenuJava.cpp */
    void* (*create_menu)(void);                                       /* :172 */
    void* (*create_item)(void);                                       /* :51  */
    void  (*item_set_type)(void* item, int32_t type);                 /* :74  */
    void  (*item_set_action)(void* item, int32_t action);             /* :94  */
    void  (*item_set_title)(void* item, const uint16_t* s, int32_t n);/* :107 */
    void  (*item_set_submenu)(void* item, void* submenu);             /* :121 */
    void  (*item_set_checked)(void* item, int32_t checked);           /* :135 */
    void  (*item_set_enabled)(void* item, int32_t enabled);           /* :148 */
    void  (*menu_append_item)(void* menu, void* item);                /* :192 */
    void  (*menu_show)(void* menu, void* page, int64_t ctrl, int32_t x, int32_t y); /* :221 */
} WkjContextMenuCallbacks;

typedef struct WkjRenderThemeCallbacks {   /* RenderTheme + ScrollBarTheme */
    void*   (*create_widget)(void* theme, int64_t id, int32_t w, int32_t h,
                             int32_t state, int32_t extra1, int32_t extra2,
                             void* extra_data, int32_t extra_len);   /* RenderThemeJava.cpp:188 */
    int32_t (*get_radio_button_size)(void* theme);                   /* :278 */
    int32_t (*get_slider_thumb_size)(int32_t part);                  /* :376 - static */
    int32_t (*get_selection_color)(void* theme, int32_t index);      /* :448 */
    void*   (*sb_create_widget)(void* sbtheme, int64_t id, int32_t, int32_t,
                                int32_t, int32_t, int32_t, int32_t); /* ScrollbarThemeJava.cpp:158 */
    void    (*sb_get_part_rect)(void* sbtheme, int64_t id, int32_t part, int32_t out[4]);
                                                                     /* ScrollbarThemeJava.cpp:106 */
    int32_t (*sb_get_thickness)(void);                               /* ScrollbarThemeJava.cpp:227 */
} WkjRenderThemeCallbacks;

/* Source/WebCore/platform/network/java/wkj_network_callbacks.h */

typedef struct WkjUrlLoaderCallbacks {
    void* (*load)(void* page, int32_t asynchronous,
                  const uint16_t* method,  int32_t mlen,
                  const uint16_t* url,     int32_t ulen,
                  const uint16_t* headers, int32_t hlen,
                  const WkjFormElement* elems, int32_t nelems,
                  void* target);                                     /* URLLoader.cpp:76  */
    void  (*cancel)(void* loader_user);                              /* URLLoader.cpp:90  */
} WkjUrlLoaderCallbacks;

typedef struct WkjSocketStreamCallbacks {
    void*   (*create)(const uint16_t* host, int32_t hlen, int32_t port,
                      int32_t ssl, void* page, void* handle);        /* SocketStreamHandleImplJava.cpp:56 */
    int32_t (*send)(void* user, const uint8_t* data, int32_t len);   /* :99  */
    void    (*close)(void* user);                                    /* :116 */
    void    (*notify_disposed)(void* user);                          /* :78  */
} WkjSocketStreamCallbacks;
```

---

## 8. Cached IDs, global refs, strings, arrays, threads, exceptions

### 8.1 Cached classes (`FindClass`)

84 sites, **49 distinct class names**, essentially all wrapped in a
`static JGClass cls(env->FindClass(...))` function-local static, so each is resolved once and
**leaked deliberately for process lifetime**. The `FIND_CACHE_CLASS` macro at `BridgeUtils.cpp:64`
makes that explicit: `static jclass cls = (jclass)(ENV)->NewGlobalRef((ENV)->FindClass(SIG));`.
Hot spots: `java/lang/Object` (4), `com/sun/webkit/plugin/PluginManager` (4),
`com/sun/webkit/graphics/WCRectangle` (3), `java/lang/{Integer,Double,Character,Boolean}` (2 each),
`com/sun/webkit/dom/{NodeImpl,EventListenerImpl}` (2 each), `com/sun/webkit/WCPluginWidget` (2),
`com/sun/webkit/plugin/PluginHandler` (2). All 49 disappear with the host struct — Java resolves its
own classes.

`PlatformJavaClasses.{h,cpp}` (`WebCore/platform/java/`) is the hub: 18 `PG_Get*Class` accessors plus
`PL_GetGraphicsManager` and `PG_GetRenderThemeObjectFromPage`. It becomes a header of handle fields
set once by `wkj_init`, or vanishes entirely once each subsystem owns its callback table.

### 8.2 Cached method/field IDs

**231 sites** (215 method, 16 field). Only two are exposed as `_initIDs` natives —
`WCWidget.initIDs` (`WidgetJava.cpp:225`, 5 methods) and `WCPluginWidget.initIDs`
(`PluginWidgetJava.cpp:63`, 4 methods + 5 fields). The rest are function-local
`static jmethodID mid = env->Get*MethodID(...)`, which is thread-safe under C++11 magic statics but
means the resolving thread is whichever gets there first — see risk 3 in section 13.

The 16 `Get*FieldID` sites read Java object internals directly and are all removable:
`PathJava.cpp:490-496` and `PlatformScreenJava.cpp:72-78` (`WCRectangle.x/y/w/h` — replace with an
out-param `float[4]`); `PluginWidgetJava.cpp:84,94-103` (`pData`, `WCRectangle` fields);
`BridgeUtils.cpp:152-153` (`JSObject.peer` / `peer_type` — replace with a `jsobject_peer` host call);
`JNIUtilityPrivate.cpp:72` (`JSObject.UNDEFINED` static field).

### 8.3 Global references

Only **5 `NewGlobalRef` / 4 `DeleteGlobalRef`** sites exist outside `JavaRef.h:142/151`:

| Site | Paired? | Note |
|---|---|---|
| `TextCodecJava.cpp:147` / `:157` | yes | dead file (section 15) |
| `JobjectWrapper.cpp:43` (strong) / `:59` | yes | LiveConnect instance |
| `JobjectWrapper.cpp:45` (**weak**) / `:57` | yes | `NewWeakGlobalRef` — the only weak ref in the tree; every deref goes through `JLObject jlinstance(obj, true)` first (`JNIUtility.h:188,238,270`, `JNIUtilityPrivate.cpp:99,310`) |
| `UnicodeJava.cpp:69` | **no** | dead file; intentional process-lifetime class ref |
| `BridgeUtils.cpp:64` (`FIND_CACHE_CLASS`) | **no** | intentional process-lifetime class ref |

**No unmatched leak was found in live code.** Everything else goes through `JGlobalRef`, whose
destructor is null-safe.

Three global refs **pin Java peers** and must become registry ids:

* `PlatformWidget = JGObject` (`WebCore/platform/Widget.h:53-56`) — pins every `WCWidget`.
* `PlatformFileHandle = JGObject` (`WTF/wtf/FileHandle.h:49-53`) — pins a `java.io.RandomAccessFile`
  per open file, with `JavaHandleMarkableTraits` comparing against `invalidPlatformFileHandle`.
* `ByteBuffer::m_nio_holder` (`RenderingQueue.h:93`) — pins each flushed direct `ByteBuffer` until
  `WCRenderQueue.twkRelease` runs on the event thread.

`PlatformCursor = jlong` (`Cursor.h:78`) is already an opaque integer handle — no work needed.

### 8.4 String conversions and modified UTF-8

The **UTF-16 path is dominant and correct**: `WTF::String(JNIEnv*, JLString)` uses
`GetStringCritical`/`ReleaseStringCritical` (`StringJava.cpp:39-47`) and `String::toJavaString` uses
`NewString` (`:68, :72`). Neither touches modified UTF-8. `URL(JNIEnv*, jstring)`
(`WTF/wtf/URL.h:248`) and `PlatformKeyboardEvent`/`PlatformTouchEvent` go through the same path.

Modified-UTF-8 dependence, live sites only:

| Site | Content | Risk |
|---|---|---|
| `JNIUtility.cpp:169` `GetStringUTFChars` | **Java class names** from `Class.getName()` | **The only real exposure.** Consumed by `JavaClassJSC.cpp:65-67` (`fastStrDup` into `JavaClass::m_name`) and `BridgeUtils.cpp:197-199` (array element-type parsing). A class name containing a supplementary character encodes as 6-byte CESU-8 here but 4-byte UTF-8 under `getString`/`allocateFrom`. Downstream use is `strcmp` against ASCII literals and array-descriptor parsing, so divergence needs an exotic class name — but it is not provably absent. **Convert this to UTF-16 in the migration** rather than betting on it. |
| `JavaEnv.cpp:63,78,91` `NewStringUTF` | perf-logger names/probes, ASCII literals | none |
| `FileSystemJava.cpp:262` `NewStringUTF("r")` | ASCII literal | none; the argument disappears entirely (`fs_open_read` is read-only by construction, `:249-251`) |
| `JavaFieldJSC.cpp:54,64`, `JavaMethodJSC.cpp:45,55,67` `NewStringUTF("<Unknown>")` | ASCII literal | none |
| `CryptoDigestJava.cpp:86` `NewStringUTF` | algorithm name, ASCII | none |

`TextCodecJava.cpp:86,94,140` also uses `GetStringUTFChars`/`NewStringUTF`, but that file is dead
(section 15).

### 8.5 Array and direct-buffer access

14 `Get*ArrayCritical` / 14 releases — **all paired**. Release modes:

* `JNI_ABORT` (discard, read-only) — `SharedBufferJava.cpp:103`, `BitmapImageJava.cpp:112`,
  `GlyphPageTreeNodeJava.cpp:82`, `TextCodecJava.cpp:205` *(dead)*. Under
  `Linker.Option.critical(true)` a heap segment is always written through, so **verify each of these
  three live sites truly does not write** before flipping. Inspection says they do not
  (`p->append(span)`, `pBuffer->append(span)`, a read loop) — but that is a code reading, not a test.
* `0` (copy back) — `ScrollbarThemeJava.cpp:123`, `SharedBufferJava.cpp:83`,
  `ComplexTextControllerJava.cpp:144`, `FontCascadeJava.cpp:57`, `GlyphPageTreeNodeJava.cpp:51`,
  `ImageBufferJavaBackend.cpp:136`, `ImageDecoderJava.cpp:155,217,285`. These map cleanly to
  `critical(true)`.
* `CryptoDigestJava.cpp:145` — `GetPrimitiveArrayCritical` whose matching release is not adjacent;
  confirm the pairing at `:145-155` before converting.

Non-critical array access: `SocketStreamHandleImplJava.cpp:180` (`GetByteArrayElements` + `JNI_ABORT`),
`FileSystemJava.cpp:146,151` (`GetLongArrayElements`, mode 0),
`MediaPlayerPrivateJava.cpp:~873` (`GetFloatArrayElements` with an `isCopy` check).

Direct buffers: 5 `NewDirectByteBuffer` (`RenderingQueue.h:49`, `ImageJava.cpp:93`,
`RenderThemeJava.cpp:202`, `FileSystemJava.cpp:305`, `CryptoDigestJava.cpp:126`) and
4 `GetDirectBufferAddress` (`RenderingQueue.cpp:139`, `ImageBufferJavaBackend.cpp:163`,
`URLLoader.cpp:510`, `TouchEventJava.cpp:49`). All become `MemorySegment.ofBuffer` /
`buffer_wrap` / raw pointers.

`PushLocalFrame`/`PopLocalFrame`: exactly one pair in the whole tree. `RegisterNatives` and
`MonitorEnter`: zero.

### 8.6 Thread attach

Two `AttachCurrentThread` call sites:

* `JavaEnv.h:91,93` — the `AttachThreadToJavaEnv<daemon>` template, **7 use sites**:
  `WebCore/fileapi/AsyncFileStream.cpp:93` (non-daemon), `WebCore/workers/WorkerThread.cpp:147`
  (daemon), `WebKitLegacy/Storage/StorageThread.cpp:79` (daemon),
  `WTF/wtf/generic/WorkQueueGeneric.cpp:73,96` (daemon), `WTF/wtf/java/MainThreadJava.cpp:49,78`
  (non-daemon).
* `JNIUtility.cpp:118` — **dead**: `ENABLE_JAVA_JSC` is `ON` (`Source/cmake/OptionsJava.cmake:12`),
  so `getJNIEnv()` takes the `#if ENABLE(JAVA_JSC)` branch at `:110` and returns
  `WTF::GetJavaEnv()`. `JNIUtility.cpp:43-62` (the `dlopen("/System/Library/Frameworks/JavaVM.framework")`
  fallback) and `:75-90` are dead with it.

All 7 sites disappear: FFM upcall stubs attach the calling thread automatically. **The daemon/
non-daemon distinction has no FFM equivalent** — see risk 4 in section 13.

### 8.7 Exceptions crossing the boundary

Four sites raise a Java exception from C:

| Site | Exception |
|---|---|
| `JavaDOMUtils.cpp:60,69` | `org.w3c.dom.DOMException(short, String)` |
| `BridgeUtils.cpp:115-120` | `java.lang.NullPointerException` |
| `BridgeUtils.cpp:239-245` | `netscape.javascript.JSException` via `JSObject.fwkMakeException` |
| `BridgeUtils.cpp:464` | `ThrowNew(JSException, "Invalid function reference")` |

All four are reachable from the `JSObject`/DOM downcalls and all four become error-code returns plus
a Java-side `throw` in the `WebKitNative` facade. The `WkjJSValue`-returning prototypes in
section 6.6 already carry the return code for this.

In the other direction, `CheckAndClearException` (256 sites) **swallows** every exception a Java
upcall raises, after `ExceptionDescribe` prints it. That behaviour must be preserved bit-for-bit —
see risk 1.

---

## 9. Native-necessity triage with parity verdicts

### 9.1 The named rulings

#### `Source/WebCore/bridge/jni/**` — LiveConnect. **Split: `OS-CALL` + `WRAPPER`.**

**Can it be deleted in favour of Java-side marshalling? Partly — and the part that can is large.**

* The **JS half is `OS-CALL`**: `BridgeUtils.cpp` calls `JSEvaluateScript` (`:261`),
  `JSObjectGetProperty` (`:359`), `JSObjectSetProperty` (`:382`), `JSObjectDeleteProperty` (`:403`),
  `JSObjectCallAsFunction` (`:483`), `JSStringCreateWithCharacters` (`:133`),
  `rootObject->gcProtect/gcUnprotect` (`JNIUtilityPrivate.cpp:137`, `BridgeUtils.cpp:504`), and
  `JavaRuntimeObject`/`JavaInstance` are JSC `RuntimeObject` subclasses that JSC itself instantiates
  and calls. None of that is reachable from Java. **Stays native.**
* The **reflection half is `WRAPPER` over `java.lang.reflect`**, and the proof is that it already
  round-trips through Java: `JavaClass::JavaClass` (`JavaClassJSC.cpp:56-119`) obtains its field and
  method tables by invoking `Class.getFields()` and `Class.getMethods()`, and `dispatchJNICall`
  (`JNIUtilityPrivate.cpp:307-381`) does the actual invocation by calling `env->ToReflectedMethod`
  (`:319`) and handing the resulting `java.lang.reflect.Method` to
  `com.sun.webkit.Utilities.fwkInvokeWithContext` (`:326-330`), **which performs `Method.invoke` in
  Java** (`Utilities.java:91-116`, including the allow/reject-list security checks). The C++ is a
  detour: Java -> C++ -> Java. There is no external symbol in this path at all.
* `JNIUtility.h:176-312` (`callJNIMethod<T>`, `callJNIMethodV<T>`, `callJNIStaticMethod<T>`, the 10
  `JNICaller<T>` specialisations) resolves methods from **runtime signature strings**. This has **no
  FFM expression whatsoever** — FFM has no `jmethodID` and no reflective invoke. Porting it is not an
  option; it must either stay JNI or move to Java.
* The boxing block `JNIUtilityPrivate.cpp:157-299` is 14 `Class.valueOf` calls. Pure `WRAPPER`.

**Recommendation:** delete `JavaClassJSC`, `JavaFieldJSC`, `JavaMethodJSC`, `JavaArrayJSC` and
`JNIUtility.{h,cpp}` (~1,400 lines) and move method/field enumeration, overload resolution and
invocation to Java, behind a `WkjLiveConnectHost` with a
`java_invoke(instance, name, argc, args, out)` member. Keep `JavaInstanceJSC`, `JavaRuntimeObject`
and the JS half of `BridgeUtils` native.

**`PARITY: unknown`.** The observable is JavaScript-visible overload resolution:
`JavaMethod::signature()` and `JavaClass::methodNamed` (`JavaClassJSC.cpp:163` onward, which parses
`name(paramTypes)` syntax) encode LiveConnect's specific method-matching rules. A Java
reimplementation using `Class.getMethods()` plus its own matching will agree on the common cases and
may not on ambiguous overloads. **The experiment that settles it:** extend
`test/javafx/scene/web/JavaScriptBridgeTest.java` (22 tests, 755 lines) with an overload-resolution
matrix — a Java class exposing `f(int)`, `f(long)`, `f(double)`, `f(String)`, `f(Object)`, `f(int[])`
and `f(int,int)` — call each from JS with every JS value kind (number int/frac, string, boolean,
null, undefined, array, JS object, Java object) and record which overload the current JNI build
selects. That table becomes the parity spec. Until it exists and passes, this is not a deletion
candidate.

#### `Source/WTF/wtf/java/FileSystemJava.cpp` — `WRAPPER`, **stays native as a host-callback consumer.**

639 lines. Eleven functions call Java (`fileExists :76`, `getFileSize :96`, `fileMetadata :127`,
`pathByAppendingComponent` x2 `:182,:202`, `makeAllDirectories :222`, `openFile :247`,
`closeFile :271`, `readFromFile :289`, `pathFileName :363`, `seekFile :382`); **about 30 more are
`fprintf(stderr, "... NOT IMPLEMENTED")` stubs** (`:421-634`). The Java side
(`modules/javafx.web/src/main/java/com/sun/webkit/FileSystem.java`, 146 lines) is plain
`java.io.File` / `RandomAccessFile` / `Files.createDirectories` with no JavaFX-specific policy.

This file **cannot be deleted in the FFM sense**: WebKit links `WTF::FileSystemImpl::fileExists` etc.
directly, so a symbol must exist natively. It becomes the consumer of the ten `fs_*` host members in
section 3.4.3.

There is a **separate, larger deletion available**: replace the Java round-trip with upstream
WebKit's own `FileSystemPOSIX.cpp` / `FileSystemWin.cpp`, deleting `FileSystemJava.cpp`,
`FileSystem.java`, and the `PlatformFileHandle = JGObject` peer pinning in one move. That is
attractive (it also fills in the 30 unimplemented stubs for free) but it is **`PARITY: unknown`** —
`File.lastModified()` epoch-ms rounding, `File.getPath()` separator normalisation, Windows path and
long-path handling, and `SecurityException`-vs-`errno` error mapping all differ in ways
`test/javafx/scene/web/{FileTest,FileReaderTest,DirectoryLockTest,UserDataDirectoryTest,LocalStorageTest}.java`
would have to be extended to pin down. **Do not bundle it with the FFM migration.**

#### `Source/WTF/wtf/java/StringJava.cpp` — `WRAPPER`, **stays native. `PARITY: unprovable`.**

78 lines, two functions, both on the hottest path in the port (every `String` crossing the boundary,
i.e. essentially all 47 `JLString` sites plus `URL.h:248`). It implements the `WTF::String`
constructor and `toJavaString`, so WebKit links the symbols; it must stay native and becomes a
consumer of `string_create` / `string_length` / `string_get_chars`.

`PARITY: unprovable` because the output is a `WTF::StringImpl`, not a comparable value — there is no
Java-side observable to assert against. It is also the wrong thing to reimplement:
`StringImpl::create` is WTF-internal.

**One defect to carry into the rewrite** (`StringJava.cpp:48-51`): when `GetStringCritical` returns
null, the code constructs a `std::span<const UChar>` over a **null pointer with length 3** and passes
it to `StringImpl::create`. That is a read of unmapped memory on OOM. The FFM version must return an
empty string here. Fixing it is a behaviour change and belongs in its own commit.

#### `Source/WTF/wtf/java/JavaMath.h` — `PURE`, **stays native. `PARITY: unprovable`.**

63 lines, one function: a 3-argument `hypot<T>` (`:36-65`) with overflow-avoiding scaling. **Zero
JNI** — it does not include `jni.h` and has no `JavaRef` use. It is pure float arithmetic
(`std::abs`, `std::sqrt`, `std::numeric_limits`), so it is textbook `PURE`.

It nonetheless **stays native**, on two independent grounds. First, its callers are WebKit C++
(nothing in Java calls it), so "reimplement in Java" is not a move that exists. Second, and the
reason it is `PARITY: unprovable` rather than `exact`: it is used in geometry that feeds rendering,
and its result depends on the platform `libm` `sqrt` and on whether the compiler contracts
`1 + yx*yx + zx*zx` into FMAs. There is no Java expression that is bit-identical to it across
Windows/Linux/macOS, and the observable (rendered geometry) is not comparable at the bit level.
**It has no JNI to remove and should simply be left alone.**

#### `Source/WTF/wtf/java/DbgUtils.h` — `PURE`, **DELETE. `PARITY: exact`.**

381 lines, **zero JNI**. Full evidence and the exact edits are in section 15.2 step 10. Summary: the
active body needs `(WIN32 || _WIN32) && (DEBUG || _DEBUG)` (`:29`); otherwise `:360-365` defines all
six macros as empty, and the `RQ_LOG_*` family additionally needs `__RQ_LOG`, **commented out at
`:30`**. Its only consumer is `RenderingQueue.h:32`, whose two `RQ_LOG_INSTANCE_COUNT` uses
(`:41`, `:108`) therefore expand to nothing in every configuration. The only two `DBG_CHECKPOINTEX`
call sites in the tree (`WebKitLegacy/java/WebCoreSupport/WebPage.cpp:265,911`) are commented out.

#### `Source/WTF/wtf/java/TextBreakIteratorInternalICUJava.cpp` — `PURE`, **stays native.**

56 lines, **zero JNI**. `currentSearchLocaleID()` and `currentTextBreakLocaleID()` both return the
hardcoded literal `"en"` via `UILanguage()` (`:37-44`), which carries a `FIXME-java: Get default
language from Java using Locale.getDefault()`. It **is** in `WTF_SOURCES`
(`WTF/wtf/PlatformJava.cmake`), and WebKit's ICU text-break code links these symbols. There is no
JNI surface to migrate and no Java caller — **leave it**. Implementing that FIXME would change
line-breaking behaviour and is a behaviour commit, not a migration one.

#### `Source/WTF/wtf/java/CPUTimeJava.cpp` — `PURE`, **stays native.**

17 lines, **zero JNI**, no copyright header (worth fixing separately). `CPUTime::get()` returns
`std::nullopt` and `CPUTime::forCurrentThread()` returns `Seconds{}` (`:6-14`). It **is** in
`WTF_SOURCES` and WebKit links both symbols. Same ruling: no JNI, no Java caller, leave it. Note
that `WTF_SOURCES` also adds `win/CPUTimeWin.cpp` on WIN32, so on Windows two definitions of
`CPUTime::get` may be in play — a duplicate-symbol error or a silently-wrong reading. **I did not
resolve this**; confirm on the next real build.

#### `platform/graphics/java` WCRenderQueue command-buffer path — `OS-CALL`, **stays native, and it is the best-shaped part of the whole scope.**

The path is `GraphicsContextJava.cpp` (1,089 lines, 40 distinct opcodes) -> `RenderingQueue`/
`ByteBuffer` (`RenderingQueue.{h,cpp}`) -> one `fwkAddBuffer` upcall -> Java `WCRenderQueue.addBuffer`
-> `GraphicsDecoder.decode`. It is **already** the architecture the FFM playbook asks for: drawing is
batched into a flat `char*` buffer of ints and floats (`RenderingQueue.h:63-73`) and handed over
once, not one upcall per primitive.

The whole JNI surface of this path is **four calls**: `NewDirectByteBuffer` (`RenderingQueue.h:49`),
`fwkAddBuffer` (`RenderingQueue.cpp:108`), `fwkFlush` (`:76`), `fwkDisposeGraphics` (`:91`), plus one
export `WCRenderQueue.twkRelease` (`:127`). Migration is nearly free:

* `NewDirectByteBuffer` -> pass `(void* addr, int32_t len)` to `add_buffer`; Java wraps with
  `MemorySegment.ofAddress(addr).reinterpret(len)` inside `WebKitNative`.
* `twkRelease(Object[] bufs)` -> `wkj_rq_release(void* const* addrs, int32_t n)`. Java already holds
  the `ByteBuffer` objects; it can pass their addresses directly, removing `GetArrayLength`,
  `GetObjectArrayElement` and `GetDirectBufferAddress` per element.
* `RQRef` (`RQRef.{h,cpp}`) needs **no new registry**: `com.sun.webkit.graphics.Ref` already assigns
  an `int id` (`Ref.java`) and `WCGraphicsManager` already keeps `refMap : Map<Integer,Ref>` with
  `ref`/`deref`/`getRef` (`WCGraphicsManager.java:150-164`). The `JGObject m_ref` in `RQRef.h:54` is
  redundant with the `jint m_refID` beside it (`:55`) — drop the global ref, keep the id, and
  `WkjRefCallbacks` (section 7.1) is a two-entry table.
* `GraphicsContextJava.cpp` must stop including the generated
  `com_sun_webkit_graphics_GraphicsDecoder.h` (`:57`) for its 53 `@Native` opcode constants; a
  hand-written `wkj_graphics_opcodes.h` replaces it (see section 12).

`ByteBuffer::m_nio_holder` (`RenderingQueue.h:93`) and the `Addr2ByteBuffer` map
(`RenderingQueue.cpp:40-46`) are the ownership mechanism keeping the buffer alive until the event
thread releases it; preserve both exactly.

### 9.2 Triage table (remainder)

| File / function | Verdict | Evidence (named symbol, or "none") | Parity | Replacement |
|---|---|---|---|---|
| `JavaEnv.cpp` `JNI_OnLoad`/`JNI_OnUnload` | `WRAPPER` | `jvm->GetEnv`, `FindClass` | `exact` | `wkj_init(const WebKitJavaHost*)` |
| `JavaEnv.cpp` `PL_*` (4 fns) | `WRAPPER` | Java `PerfLogger` only | `exact` | **Delete.** The single `LOG_PERF_RECORD` site is in a file that is not compiled (section 15.2 step 9) |
| `JavaEnv.cpp` `CheckAndClearException` | `WRAPPER` | `ExceptionCheck/Describe/Clear` | `exact` | `host->exception_check_and_clear()` |
| `JavaRef.h` `JLocalRef`/`JGlobalRef` | `WRAPPER` | `New/DeleteLocalRef`, `New/DeleteGlobalRef` | `exact` | `JavaHandle` (section 3.4.2) |
| `MainThreadJava.cpp` `scheduleDispatchFunctionsOnMainThread` | `WRAPPER` | `MainThread.fwkScheduleDispatchFunctions` | `exact` | `host->main_thread_schedule_dispatch()` |
| `MainThreadJava.cpp` `isMainThread` / `initializeMainThreadPlatform` | `OS-CALL` | `pthread_self`/`pthread_equal` (`:92,101`), `Thread::currentID` (`:94,106`) | n/a | stays; drop only the `FindClass`/`GetStaticMethodID` block (`:81-89`) |
| `MainThreadSharedTimerJava.cpp` | `WRAPPER` | `Timer.fwkSetFireTime`/`fwkStopTimer` | `exact` | `timer_set_fire_time` / `timer_stop` |
| `IDNJava.cpp` | `WRAPPER` | `java.net.IDN.toASCII` (`:42`) — a **JDK** API | `unknown` | Either a host member, or delete in favour of ICU `uidna_nameToASCII` (ICU is already linked: `USE_ICU_UNICODE`, `Source/cmake/OptionsJava.cmake:5`). `java.net.IDN` is IDNA2003; ICU defaults to UTS-46. **Experiment:** run both over the IDNA test vectors and diff. Until then, keep the Java call. |
| `LocalizedStringsJava.cpp` | `WRAPPER` | `LocalizedStrings.getLocalizedProperty` | `exact` | host member; most sibling functions already `return String()` |
| `PAL/pal/system/java/SoundJava.cpp` | `WRAPPER` | **`java.awt.Toolkit.getDefaultToolkit().beep()`** (`:41,50`) | `unknown` | Flag independently: this drags **AWT** into a JavaFX process for one beep. Replace with a JavaFX-native beep or delete. |
| `PAL/pal/crypto/java/CryptoDigestJava.cpp` | `WRAPPER` | `com.sun.webkit.security.WCMessageDigest` -> JCA (`:52,121,138`) | `exact` | host member, or bind a JCA `MessageDigest` from Java directly. `NewDirectByteBuffer` at `:126` becomes a pointer. |
| `PluginDataJava.cpp` + `PluginInfoStoreJava.cpp` | `WRAPPER` | `PluginManager`/`PluginHandler` only | `exact` | 14 upcalls -> `WkjPluginCallbacks`. Check first whether NPAPI plugins are reachable at all; if not, this is a 2-file deletion. |
| `WidgetJava.cpp` `initIDs` | `WRAPPER` | none — pure `GetMethodID` | `exact` | `WkjWidgetCallbacks` (section 7.1) |
| `PluginWidgetJava.cpp` `initIDs` | `WRAPPER` | none — pure `GetMethodID`/`GetFieldID` | `exact` | `WkjPluginWidgetCallbacks` |
| `PlatformJavaClasses.cpp` (18 `PG_Get*Class`) | `WRAPPER` | `FindClass` only | `exact` | delete; Java resolves its own classes |
| `SharedBufferJava.cpp` `twkDispose` | — | none | — | **Bug, not a verdict:** `:106-112` casts the pointer and does nothing. Every `SharedBuffer` allocated by `twkCreate` (`:47`) leaks. Fix in its own commit. |
| `BitmapImageJava.cpp` `createFromName` | — | — | — | **Dead body:** the live `#if USE(IMAGEIO)` branch (`:47-71`) is entirely commented out and returns an empty image. Worth a separate look. |

---

## 10. Deletion candidates (summary; the actionable list is section 15)

**Clears the parity gate — delete now, before any FFM work.** `PARITY: exact` because the files are
not compiled or compile to nothing, so the parity test is "the tree still builds and the tests still
pass". **13 files, 1,702 lines.** The full per-file evidence, the extra edits each one needs, and a
safe ordering are in **section 15**; that section supersedes the figures quoted in section 1.

Also removable at the same time, as `PARITY: exact` `WRAPPER` deletions rather than dead code:

* **The `PL_*` perf-logger hooks** — `JavaEnv.h:50-53,56-77,120-126` and `JavaEnv.cpp:47-107`
  (~70 lines), plus `com.sun.webkit.perf.PerfLogger`. After the `TextBreakIteratorJava.cpp` deletion
  there are **zero** `LOG_PERF_RECORD` sites left. Removing them removes 4 members from the host
  struct and 4 upcalls.
* **`PlatformJavaClasses.cpp`'s 18 `PG_Get*Class` accessors** (~190 lines) — pure `FindClass` caching
  with no purpose once Java owns class resolution.

**Clears the gate but is a behaviour commit, not a migration commit:**

* **`JavaClassJSC` + `JavaFieldJSC` + `JavaMethodJSC` + `JavaArrayJSC` + `JNIUtility.{h,cpp}`**
  (~1,400 lines) -> Java-side reflection. `PARITY: unknown` until the `JavaScriptBridgeTest` overload
  matrix in section 9.1 exists. **Highest-value deletion in the scope; do it last and on its own.**
* **`FileSystemJava.cpp` + `FileSystem.java`** (785 lines combined) -> upstream `FileSystemPOSIX`/
  `FileSystemWin`. `PARITY: unknown`. Separate commit, separate PR.
* **`SoundJava.cpp`** (~60 lines) -> remove the AWT dependency. `PARITY: unknown` (is a beep even
  audible in the test environment?), but the AWT-in-JavaFX coupling justifies the look regardless.

---

## 11. Kept native despite being pure

This list existing is the point of the triage — these are `PURE` by construction and still must not
move.

| Item | Verdict | Parity | Why it stays |
|---|---|---|---|
| `WTF/wtf/java/JavaMath.h` `javamath::hypot<T>` | `PURE` | **`unprovable`** | Zero JNI. Callers are WebKit C++ only — there is no Java caller to move it to. Result depends on platform `libm` `sqrt` and FMA contraction of `1 + yx*yx + zx*zx` (`:64`); no Java expression is bit-identical across the three platforms, and the observable (rendered geometry) is not bit-comparable. Leave the file untouched. |
| `WTF/wtf/java/StringJava.cpp` | `WRAPPER` | **`unprovable`** | Implements the `WTF::String` constructor and `toJavaString`; WebKit links both. Output is a `StringImpl`, with no Java-side observable to assert equality against. Stays native, consuming `string_create`/`string_length`/`string_get_chars`. |
| `WTF/wtf/java/TextBreakIteratorInternalICUJava.cpp` | `PURE` | `exact`, but no move exists | Zero JNI, returns `"en"` (`:43`). WebKit's ICU break-iterator code links `currentSearchLocaleID`/`currentTextBreakLocaleID`. No Java caller. Leave it; the `FIXME-java` at `:42` is a behaviour change, not a migration. |
| `WTF/wtf/java/CPUTimeJava.cpp` | `PURE` | `exact`, but no move exists | Zero JNI, returns `nullopt`/`Seconds{}`. WebKit links both symbols. No Java caller. **Open question:** `WTF_SOURCES` adds `win/CPUTimeWin.cpp` on Windows alongside this file — confirm which definition wins on the next build. |
| `WebCore/bridge/jni/jsc/JavaInstanceJSC.cpp`, `JavaRuntimeObject.cpp` | `OS-CALL` | n/a | JSC `RuntimeObject` subclasses that JavaScriptCore instantiates and dispatches on, under `JSLockHolder`. Not reachable from Java. |
| `BridgeUtils.cpp` JS half (`asJSStringRef :129`, `Java_Object_to_JSValue :138`, `executeScript :248`, `checkJSPeer :273`) | `OS-CALL` | n/a | Calls `JSEvaluateScript`, `JSObjectGetProperty`, `JSObjectCallAsFunction`, `JSStringCreateWithCharacters`, `gcProtect`/`gcUnprotect`. |
| `MainThreadJava.cpp` `isMainThread` / thread-id capture (`:91-108`) | `OS-CALL` | n/a | `pthread_self`/`pthread_equal` on Unix, `Thread::currentID` on Windows. |
| `RenderingQueue`/`ByteBuffer`/`GraphicsContextJava` opcode encoding | `PURE` | **`unprovable`** | The encoded byte stream **is** the de-facto spec that `GraphicsDecoder.decode` consumes; the observable is rendered pixels. Reimplementing the encoder in Java would also mean moving all of WebKit's `GraphicsContext` into Java, which is not a migration. Stays native; only its 4 JNI calls move. |

---

## 12. Build and test touchpoints

### 12.1 The WebKit tree is not built by this repository

`modules/javafx.web/pom.xml` compiles Java only (`:40-48` says so explicitly, and the `native-*`
CMake profiles that javafx.graphics has do not exist here). Concretely:

* **No `-h` JNI header generation.** `javafx.web/pom.xml:229-236` sets `compilerArgs` but no `-h`.
* **The 27 generated `com_sun_webkit_*.h` headers the C++ includes do not exist in the tree**
  (`find modules/javafx.web -name 'com_sun_webkit_*.h'` returns nothing) and nothing produces them.
  Most-included: `com_sun_webkit_graphics_GraphicsDecoder.h` (9 sites),
  `com_sun_webkit_dom_JSObject.h` (4), `com_sun_webkit_LoadListenerClient.h` (4).
* Therefore **anyone rebuilding `jfxwebkit` must run `javac -h` out of band** — today, before any
  FFM work. This is an existing gap, not one the migration creates, but it means the source lists in
  `WTF/wtf/PlatformJava.cmake`, `WebCore/SourcesJava.txt` and `WebCore/PAL/pal/PlatformJava.cmake`
  are **not validated by any build in this repository**. The section 15 dead-file findings rest on
  those lists plus `#if` analysis; reconfirm them against a real WebKit build before deleting.

**Migration consequence:** those 230 `@Native` constants over 16 Java classes must move to a
hand-written `wkj_constants.h` (or a generator that is part of *some* build). This is a prerequisite
for `GraphicsContextJava.cpp`, `RenderThemeJava.cpp`, `ScrollbarThemeJava.cpp`, `URLLoader.cpp`,
`PathJava.cpp`, `KeyboardEventJava.cpp`, `MouseEventJava.cpp`, `TouchEventJava.cpp` and
`BridgeUtils.cpp` compiling without JNI headers, and it is cheap to do first.

### 12.2 Include paths to remove at the end

`JAVA_INCLUDE_PATH` / `JAVA_INCLUDE_PATH2` appear in exactly three files:
`Source/WTF/wtf/PlatformJava.cmake:8-9`, `Source/WebCore/PlatformJava.cmake:35-36`,
`Source/JavaScriptCore/PlatformJava.cmake:16-17` (line numbers as of the migrator's current edits).
`WTF/wtf/PlatformJava.cmake` also links `${JAVA_JVM_LIBRARY}` — **that link is the whole point of the
exercise and its removal is the completion criterion for this scope.**

### 12.3 Tests

Web tests are compiled always and skipped unless `-Djfx.web.skipTests=false`
(`javafx.web/pom.xml:48, :272`), and need a prebuilt `jfxwebkit` on `${jfx.native.librarypath}`
(root `pom.xml:174`). The surefire `argLine` already carries
`--enable-native-access=javafx.graphics,javafx.media,javafx.web` (`:278`).

| Scope area | Tests | Notes |
|---|---|---|
| `bridge/jni` (LiveConnect) | `test/javafx/scene/web/JavaScriptBridgeTest.java` (**22 tests, 755 lines**), `CallbackTest.java` (309) | the regression net for the reflection rewrite; needs the overload matrix from section 9.1 added |
| `bindings/java` (DOM, EventListener) | `DOMTest.java` (15 tests, 469 lines), `EventListenerLeakTest.java`, `LeakTest.java` | `EventListenerLeakTest` is the guard for `EventListenerManager` refcounting |
| `SharedBufferJava` | `test/com/sun/webkit/SharedBufferTest.java` (**46 tests, 527 lines**), `SimpleSharedBufferInputStreamTest.java` | the best-covered native surface in scope |
| `platform/network/java` | `test/com/sun/webkit/network/{CookieManagerTest,CookieTest,DateParserTest,PublicSuffixesTest,UtilTest}.java`, `data/DataURLConnectionTest.java` | all exercise Java-side networking; **none touch the `URLLoader` `twk*` natives** |
| `graphics/java` (RQ, Canvas, Path) | `CanvasTest.java` (6), `PathContructorTest.java`, `SVGTest.java`, `MathMLRenderTest.java`, `ShadowTest.java`, `OpacityTest.java` | the RQ path's coverage |
| `FileSystemJava` | `FileTest.java` (76), `FileReaderTest.java` (287), `DirectoryLockTest.java`, `UserDataDirectoryTest.java`, `LocalStorageTest.java` | indirect |
| `TextBreakIteratorJava` *(dead)* | `test/com/sun/webkit/text/TextBreakIteratorTest.java` (72) | tests `TextBreakIteratorShim`, the **Java** class; passes with the C++ dead |
| System/robot | `tests/system/src/test/java/test/robot/javafx/web/{PointerEventTest,TextSelectionTest,TooltipFXTest}.java` | `mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true` |

**No test stubs or overrides any native in this scope** — there is no `_initIDs` test double and no
`StubToolkit` hook for `jfxwebkit`. Every test above needs the real library.

`com.sun.javafx.webkit.drt.DumpRenderTree` (`modules/javafx.web/src/main/java/com/sun/javafx/webkit/drt`)
remains the only broad regression net for the graphics path, and nothing in the build runs it.

---

## 13. Risks

1. **`CheckAndClearException` semantics change shape under FFM (256 sites).** Today the pattern is
   "make an upcall, then ask JNI whether it left an exception pending, print it, clear it, carry on".
   Under FFM the *Java upcall stub* must catch `Throwable` (an escaping exception terminates the
   JVM). So C++ never sees a pending exception, and `exception_check_and_clear()` must be backed by a
   **per-thread pending-exception flag** the stub sets before returning a default value. Get this
   wrong and 256 call sites silently stop noticing failures — including `URLLoader.cpp:106-108`,
   where the return value of `CheckAndClearException` decides whether `platformSendInternal` reports
   a short write.

2. **Global refs pin Java peers, and the registry makes leaks permanent.** `PlatformWidget = JGObject`
   (`Widget.h:56`) pins every `WCWidget`; `PlatformFileHandle = JGObject` (`FileHandle.h:49-53`) pins
   a `RandomAccessFile` per open file; `ByteBuffer::m_nio_holder` (`RenderingQueue.h:93`) pins each
   flushed direct buffer until the event thread runs `twkRelease`. JNI at least reclaimed leaked
   *local* refs at native-method return; a registry reclaims nothing. Add a debug-build registry-size
   assertion and check `EventListenerLeakTest`/`LeakTest` after every step.

3. **`FindClass` from a foreign thread, and the class-loader trap.** `JNI_OnLoad` resolves
   `com/sun/webkit/FileSystem` eagerly (`JavaEnv.cpp:139-149`) precisely because `FileSystemJava` is
   reached from WebKit-created threads where `FindClass` would use the system loader and fail. But
   `JavaEventListener::handleEvent` (`JavaEventListener.cpp:60-63`) does
   `env->FindClass("com/sun/webkit/dom/EventListenerImpl")` inside a **function-local `static`
   jmethodID initialiser** — whichever thread calls it first performs the resolution. That is a
   latent instance of the same bug today, and it disappears with the host struct (Java resolves its
   own classes). Do not reintroduce lazy class resolution on the C side.

4. **Daemon vs non-daemon thread attach has no FFM equivalent.** `AttachThreadToJavaEnv<true>` is
   used by `WorkerThread.cpp:147`, `StorageThread.cpp:79` and `WorkQueueGeneric.cpp:73,96`;
   `<false>` by `AsyncFileStream.cpp:93` and `MainThreadJava.cpp:49,78`. FFM upcall stubs attach as
   daemon threads and detach afterwards. **The non-daemon cases change JVM shutdown behaviour**: a
   non-daemon attached thread keeps the JVM alive. Verify against
   `test/javafx/scene/web/{WebWorkerTest,LocalStorageTest,FileReaderTest}.java` and the shutdown-hook
   behaviour before declaring the attach sites removed.

5. **Modified UTF-8 for Java class names.** `JNIUtility.cpp:169` (`GetStringUTFChars`) is the only
   live site whose content is not an ASCII literal. Convert `JavaClassJSC.cpp:65-67` and
   `BridgeUtils.cpp:197-199` to UTF-16 rather than assuming class names stay in the BMP.

6. **`critical(true)` on the wrong function.** Three live sites release with `JNI_ABORT`
   (`SharedBufferJava.cpp:103`, `BitmapImageJava.cpp:112`, `GlyphPageTreeNodeJava.cpp:82`) and rely on
   *not* copying back. Heap segments under `critical(true)` are always written through. Also, two of
   the proposed `critical(true)` downcalls sit on I/O paths — `wkj_socket_did_receive_data` and
   `wkj_url_loader_did_receive_data` call straight into `SocketStreamHandleClient` /
   `ResourceHandleClient`, which can run arbitrary WebKit work and **must not be `critical`**.
   `wkj_shared_buffer_*` and `wkj_media_notify_buffer_changed` are the safe ones.

7. **`long`-to-pointer casts, 553 sites.** `jlong_to_ptr` (511) and `ptr_to_jlong` (42) go through
   `(uintptr_t)`, so they are 64-bit clean. But `PlatformCursor = jlong` (`Cursor.h:78`) and
   `Ref.getID()` returning `jint` mean the graphics path mixes a 64-bit cursor handle with a 32-bit
   ref id. Keep the widths explicit in the ABI (`int64_t` vs `int32_t`) rather than letting `long`
   decide.

8. **The `JGlobalRef(T raw)` consuming constructor** (`JavaRef.h:157`, section 3.4.2 note 1). A
   scripted rewrite that treats it like the copying constructors produces a double-release. This is
   the single most likely way to break the Phase-B change, and it is invisible until a use-after-free.

9. **No struct-by-value returns anywhere in scope** — checked; every object return is a `jobject`.
   The proposed ABI keeps it that way (out-params and pointer returns only).

10. **`SharedBuffer.twkDispose` leaks** (`SharedBufferJava.cpp:106-112`, body is `ASSERT` +
    `UNUSED_PARAM`). Do not carry the leak into `wkj_shared_buffer_dispose`; fix it in its own commit
    so the migration stays behaviour-neutral and the fix is reviewable.

11. **`StringJava.cpp:48-51` reads a null pointer with length 3** on `GetStringCritical` failure.
    On the hottest path in the port. Separate commit.

12. **The tree is being edited concurrently.** See section 15.0 — the `ffm-migrator` agent has
    uncommitted work in this tree, including a `WKJHostCore` struct in
    `WebKitLegacy/java/api/webkit_java_api.h` and build-list entries for a `WKJDOMUtils.{cpp,h}` that
    does not exist yet. Line numbers in this report are against that modified tree.

---

## 14. Recommendation

**Migrate this scope — it is genuinely `OS-CALL`-dominated — but delete the 1,702 lines of dead code
first, and keep the LiveConnect reflection rewrite out of the migration entirely.**

Unlike `prism_es2` or `glass`, this layer is not a wrapper around OS calls: it is WebKit's platform
abstraction, and WebKit links every symbol in it. The FFM work here is genuinely a **callback-table
port**, not a facade, and the `WebKitJavaHost` struct in section 3.4.3 is the whole deliverable.

Ordering, each step its own commit:

0. **Prerequisite (no FFM):** replace the 27 generated `com_sun_webkit_*.h` includes with a
   hand-written `wkj_constants.h` carrying the 230 `@Native` constants. Nothing else can compile
   JNI-free until this exists, and it is independently useful — today the tree cannot be rebuilt
   without an out-of-band `javac -h` (section 12.1).
1. **Dead-code deletion** — the 12-step checklist in **section 15**: 13 files, 1,702 lines,
   `PARITY: exact`. Steps 1-5 (541 lines) need no edit to any other file and can go first on their
   own if you want a smaller first commit. Also fold in the `PL_*` hooks (section 15.2 step 9) and
   `PlatformJavaClasses.cpp`'s 18 accessors.
2. **`JavaHandle` + `WebKitJavaHost`** (section 3.4). The wide, mechanical change: ~370 `JL*`/`JG*`
   sites, 224 `GetJavaEnv`, 256 `CheckAndClearException`, 84 `FindClass`, 231 cached IDs. Script it,
   review the `JGlobalRef(T raw)` sites by hand (risk 8), and land it with both JNI and host paths
   able to compile so the diff is bisectable. Run the full web suite plus
   `EventListenerLeakTest`/`LeakTest` for the refcount regression.
3. **`WCRenderQueue` / `RQRef` / `GraphicsContextJava`** (section 9.1). Four JNI calls and one
   export; Java already owns the id registry (`WCGraphicsManager.java:150-164`). Best ratio in the
   scope. Verify with `CanvasTest`, `SVGTest`, `MathMLRenderTest`, `ShadowTest`, `OpacityTest`, then
   `mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Dtest='test/robot/javafx/web/**'`.
4. **`SharedBufferJava`** — 5 exports, 46 tests already written (`SharedBufferTest`). The
   best-covered surface; use it to validate `critical(true)`.
5. **Network** — `URLLoader` (6 exports, 4 upcalls) and `SocketStreamHandleImplJava` (4 exports,
   4 upcalls). Note the threading finding: every `twk*` already arrives on the FX event thread via
   `URLLoader.java:785-791`, so no new marshalling is needed — just do not remove it.
6. **`MediaPlayerPrivateJava`** — 10 exports, 14 upcalls, entirely scalar. Mechanical.
7. **Platform callbacks** — `WCWidget`, `WCPluginWidget`, `ContextMenu`, `RenderTheme`,
   `ScrollbarTheme`, `Cursor`, `PluginData`/`PluginInfoStore`. Both `_initIDs` exports and their two
   `static {}` blocks die here (section 15.5 items 13-14). Manual verification (`tests/manual`), plus
   the robot web tests.
8. **`FileSystemJava`** — 11 upcalls to the `fs_*` host members. Behaviour-neutral; the POSIX/Win
   replacement is explicitly **not** part of this step.
9. **`bindings/java`** — `JavaEventListener`, `JavaDOMUtils`, `EventListenerManager` (3+2 exports).
   Guarded by `DOMTest` and `EventListenerLeakTest`.
10. **`bridge/jni` tagged-union step** — the `WkjJSValue` union (section 6.6) for the 9 `JSObject`
    exports. Behaviour-neutral migration only; the reflection layer stays JNI at this point.
11. **Last, and only after the overload-resolution matrix from section 9.1 is written and passing:**
    delete `JavaClassJSC`/`JavaFieldJSC`/`JavaMethodJSC`/`JavaArrayJSC`/`JNIUtility.{h,cpp}` in
    favour of Java-side reflection. **This is a behaviour commit, not a migration commit**, and it is
    the only step in this plan that can change what an application observes.

Completion criterion for the scope: `${JAVA_JVM_LIBRARY}` comes out of
`Source/WTF/wtf/PlatformJava.cmake`, and `JAVA_INCLUDE_PATH`/`JAVA_INCLUDE_PATH2` come out of all
three `PlatformJava.cmake` files — which cannot happen until `WebKitLegacy` (out of scope here,
~170 files including the 105 DOM bindings) is done too.

Steps 1 and 11 are Java reimplementations or deletions and must never share a commit with steps
2-10, which are behaviour-neutral by construction.

---

## 15. Deletion checklist (`PARITY: exact`)

### 15.0 Two corrections to section 1

**Correction 1 — the count.** Section 1 says "7 files ... 1,847 lines". Both figures are wrong.
Re-measured with `wc -l` against the working tree: the set is **13 files, 1,702 lines**. The 1,847
was an arithmetic slip; the file count depended on whether `.h`/`.cpp` pairs were counted as one
item. This section is the authoritative list.

**Correction 2 — not all 13 are standalone deletions.** Four of them are referenced by a file that
**is** compiled. Per the rule that a header included by a built file is not a safe standalone
deletion, those are separated below into steps that name the extra edit required. Only steps 1-5 are
"delete the file and nothing else".

**Concurrent-work warning.** The working tree is being modified by the `ffm-migrator` agent while
this audit was written (`git status` shows uncommitted `WTF/wtf/java/WKJHandle.h`,
`WebKitLegacy/java/api/webkit_java_api.h`,
`modules/javafx.web/src/main/java/com/sun/webkit/WebKitNative.java`, and edits to
`WTF/wtf/PlatformJava.cmake`, `WebCore/PlatformJava.cmake`, `WebCore/SourcesJava.txt`,
`WebKitLegacy/PlatformJava.cmake`, `WebCore/bindings/scripts/CodeGeneratorJava.pm` and the two
mapfiles). **All line numbers below are against that modified tree.** Re-check them if the migrator
lands more edits first. Two collisions to be aware of:

* `WebCore/SourcesJava.txt:112` lists `bindings/java/WKJDOMUtils.cpp` and
  `WebCore/PlatformJava.cmake:96` lists `bindings/java/WKJDOMUtils.h`. Both files landed while this
  section was being written and now exist as untracked additions, so the lists are consistent again.
  Nothing in this checklist touches them.
* `WTF/wtf/PlatformJava.cmake` line numbers shifted by the migrator inserting `java/WKJHandle.h` at
  line 15. `DbgUtils.h` is now line 16 and `unicode/java/UnicodeJava.h` line 18. **Re-run each step's
  grep before acting on it** — the migrator is still editing this tree (as of writing it has also
  modified ~110 files under `WebKitLegacy/java/DOM/`, `WebCoreSupport/WebPage.cpp`,
  `CodeGeneratorJava.pm` and both mapfiles, none of which is in this scope). I re-verified every
  citation in this checklist against the tree in its current state, including
  `WebPage.cpp:265,911`.

### 15.1 Answer to "does any of this orphan a Java `native`?"

**No. None of the 13 files backs a single Java `native` declaration.** Verified: the four Java
classes these files upcall into declare zero native methods.

```
com.sun.webkit.text.TextCodec            0 native methods
com.sun.webkit.text.TextBreakIterator    0
com.sun.webkit.text.TextNormalizer       0
com.sun.webkit.text.StringCase           0
```

They are pure upcall *targets*, so deleting the C++ leaves them unreferenced Java classes (a
follow-up Java-side cleanup), not unbound natives. The "3 orphan Java natives" figure the coordinator
referred to is **not from this audit** — it appears in `FFM-AUDIT-core.md`, which covers
`WebKitLegacy`. Nothing in this scope contributes to it, and I have not verified that number.

### 15.2 The checklist

Ordered so that each step is safe given only the steps before it. One verification command per step;
every `grep` must come back **empty** (exit 1). Run from
`modules/javafx.web/src/main/native/Source`. Because this repository cannot compile WebKit
(section 12), the build check is out-of-band — do it once, after step 11, not per step.

#### Steps 1-5: zero-reference files. Delete the file, change nothing else.

- [ ] **Step 1 — `WTF/wtf/java/RunLoopJava.cpp` (66 lines)**
  *Evidence:* absent from `WTF_SOURCES` in `WTF/wtf/PlatformJava.cmake` for every platform; that
  list selects `win/RunLoopWin.cpp` (WIN32), `generic/RunLoopGeneric.cpp` (UNIX) and
  `cf/RunLoopCF.cpp` (APPLE) instead. Zero includes, zero forward declarations, zero CMake entries.
  *Orphans a Java native:* no.
  `grep -rn 'RunLoopJava' --include=*.cpp --include=*.h --include=*.txt --include=*.cmake .`

- [ ] **Step 2 — `WebCore/platform/java/JavaEnv.h` (0 lines, empty file)**
  *Evidence:* the file is 0 bytes. No `#include "JavaEnv.h"` in the tree resolves to it; all such
  includes resolve to `WTF/wtf/java/JavaEnv.h`.
  *Orphans a Java native:* no.
  `[ -s WebCore/platform/java/JavaEnv.h ] && echo NOT-EMPTY-STOP || echo empty-safe`

- [ ] **Step 3 — `WebCore/platform/java/TextBreakIteratorJava.cpp` (257 lines)**
  *Evidence:* absent from `WebCore/SourcesJava.txt` (`grep -c TextBreakIteratorJava` gives 0). No
  caller anywhere in the tree. There is no `TextBreakIteratorJava.h`.
  *Note:* this file holds the **only `LOG_PERF_RECORD` call site in the entire tree**
  (`TextBreakIteratorJava.cpp:69`). See step 9 — deleting it makes the whole `PL_*` perf group dead.
  *Orphans a Java native:* no (`com.sun.webkit.text.TextBreakIterator` has 0 natives). Keep the Java
  class: `test/com/sun/webkit/text/TextBreakIteratorTest.java` exercises `TextBreakIteratorShim`,
  which is pure Java and passes today with this C++ already dead.
  `grep -rn 'TextBreakIteratorJava' --include=*.cpp --include=*.h --include=*.txt --include=*.cmake .`

- [ ] **Step 4 — `WebCore/bindings/java/JavaNodeFilterCondition.cpp` (53 lines)**
  *Evidence:* absent from `WebCore/SourcesJava.txt`. No caller.
  *Orphans a Java native:* no.
  `grep -rn 'JavaNodeFilterCondition' --include=*.cpp --include=*.txt .`
  (after this step it must show only the `.h`, handled in step 6)

- [ ] **Step 5 — `WTF/wtf/unicode/java/UnicodeJava.cpp` (165 lines)**
  *Evidence:* appears in **no** source list; `WTF/wtf/PlatformJava.cmake` lists only the header. The
  only caller of anything it defines is `WebCore/editing/java/SmartReplaceJava.cpp:37`, inside
  `#if USE(JAVA_UNICODE)` and therefore compiled out (step 8).
  *Do not delete `UnicodeJava.h` here* — it is still included by a built file. That is step 11.
  *Orphans a Java native:* no (`com.sun.webkit.text.StringCase` has 0 natives).
  `grep -rn 'UnicodeJava\.cpp' --include=*.txt --include=*.cmake .`

#### Steps 6-8: one build-list or guarded-block edit alongside the file.

- [ ] **Step 6 — `WebCore/bindings/java/JavaNodeFilterCondition.h` (54 lines)**
  *Also edit:* delete `WebCore/PlatformJava.cmake:95` — `    bindings/java/JavaNodeFilterCondition.h`
  (a `WebCore_PRIVATE_FRAMEWORK_HEADERS` entry, i.e. a header-copy list, not a compile).
  *Evidence it is otherwise unreferenced:* no `#include` of it anywhere; its `.cpp` went in step 4.
  *Orphans a Java native:* no.
  `grep -rn 'JavaNodeFilterCondition' --include=*.cpp --include=*.h --include=*.txt --include=*.cmake .`

- [ ] **Step 7 — `WebCore/platform/java/TextNormalizerJava.cpp` + `.h` (72 + 47 = 119 lines)**
  *Evidence:* `.cpp` absent from `WebCore/SourcesJava.txt`. `.h` is included by
  `WebCore/PAL/pal/text/TextEncoding.cpp:39` — a **built** file — but inside a guard that is off:
  ```
  38: #if USE(JAVA_UNICODE)
  39: #include "TextNormalizerJava.h"
  40: #endif
  ```
  `Source/cmake/OptionsJava.cmake:5` sets `set(ICU_UNICODE TRUE)`, so `:13-16` selects
  `USE_ICU_UNICODE` and leaves `USE_JAVA_UNICODE` undefined.
  *Also edit:* remove `TextEncoding.cpp:38-40`, and collapse the
  `#if !USE(JAVA_UNICODE) / #else / #endif` at `TextEncoding.cpp:83-90` to its non-Java branch —
  the `#else` arm at `:88` is the only reference to `TextNormalizer::normalize`. This is a real edit
  to a built file, not just an include removal.
  *Orphans a Java native:* no (`com.sun.webkit.text.TextNormalizer` has 0 natives; its `@Native`
  constants become unused).
  `grep -rn 'TextNormalizer' --include=*.cpp --include=*.h --include=*.txt --include=*.cmake .`

- [ ] **Step 8 — `WebCore/platform/java/TextCodecJava.cpp` + `.h` (210 + 52) and `WebCore/editing/java/SmartReplaceJava.cpp` (67)**
  *Evidence (TextCodecJava):* `.cpp` absent from `WebCore/SourcesJava.txt`. `.h` is included by the
  built `WebCore/PAL/pal/text/TextEncodingRegistry.cpp:53`, inside `#if USE(JAVA_UNICODE)`
  (`:52-54`); its two uses at `:293-294` are inside the same guard opened at `:292`.
  *Evidence (SmartReplaceJava.cpp):* this one **is** compiled (`WebCore/SourcesJava.txt:36`), but its
  only function is wrapped in `#if USE(JAVA_UNICODE)` at `:34`, so it is an empty translation unit.
  The live definition of `isCharacterSmartReplaceExempt` comes from
  `WebCore/editing/SmartReplace.cpp:96`, which is in the main `WebCore/Sources.txt:1466`.
  *Also edit:* remove `TextEncodingRegistry.cpp:52-54` and `:292-295`; remove
  `WebCore/SourcesJava.txt:36`.
  *Orphans a Java native:* no (`com.sun.webkit.text.TextCodec` has 0 natives).
  `grep -rn 'TextCodecJava\|SmartReplaceJava' --include=*.cpp --include=*.h --include=*.txt --include=*.cmake .`

#### Steps 9-11: require editing a built file whose include is *not* guarded.

- [ ] **Step 9 — decide the `PL_*` perf group (no file deleted; unblocks the ABI freeze)**
  After step 3, `LOG_PERF_RECORD` has **zero** call sites left in the tree. Confirm, then either
  delete `JavaEnv.h:50-53` (`PL_*` declarations), `:56-77` (`EntryJavaLogger`), `:120-126`
  (`LOG_PERF_RECORD`) and `JavaEnv.cpp:47-107`, or consciously keep them. **This matters to the
  migrator right now:** `WebKitLegacy/java/api/webkit_java_api.h:218-232` carries four
  `WKJHostCore` members (`perf_get_logger`, `perf_resume_count`, `perf_suspend_count`,
  `perf_is_enabled`) and justifies them with "One live call site (TextBreakIteratorJava.cpp)". That
  call site is in a file that is not compiled, so the justification does not hold and the four
  members can come out of the ABI before it is frozen.
  `grep -rn 'LOG_PERF_RECORD' --include=*.cpp --include=*.h . | grep -v 'wtf/java/JavaEnv.h'`

- [ ] **Step 10 — `WTF/wtf/java/DbgUtils.h` (381 lines)**
  **Not a standalone deletion.** It is included by `WebCore/platform/graphics/java/RenderingQueue.h:32`
  and that header is compiled (`RenderingQueue.cpp` is in `WebCore/SourcesJava.txt`). The include is
  **unguarded**.
  *Why it is still safe:* every macro it exports expands to nothing in every configuration. The
  active body needs `(WIN32 || _WIN32) && (DEBUG || _DEBUG)` (`:29`); otherwise `:360-365` defines
  `DBG_CHECKPOINT`, `DBG_CHECKPOINTEX`, `LOG_INSTANCE_COUNT`, `LOG_COMMON_SIZE`,
  `LOG_COMMON_SIZE_ADD` and `LOG_COMMON_SIZE_REMOVE` as empty. The `RQ_LOG_*` family additionally
  needs `__RQ_LOG`, which is **commented out at `:30`**, so `:373-377` takes the empty branch even in
  a Windows debug build. The only two `DBG_CHECKPOINTEX` call sites in the tree
  (`WebKitLegacy/java/WebCoreSupport/WebPage.cpp:265,911`) are commented out.
  *Also edit:* remove `RenderingQueue.h:32` (the include), `:41` (`RQ_LOG_INSTANCE_COUNT(ByteBuffer)`)
  and `:108` (`RQ_LOG_INSTANCE_COUNT(RenderingQueue)`); remove `WTF/wtf/PlatformJava.cmake:16`.
  *Orphans a Java native:* no.
  `grep -rn 'DbgUtils\|RQ_LOG_\|DBG_CHECKPOINT\|LOG_INSTANCE_COUNT\|LOG_COMMON_SIZE' --include=*.cpp --include=*.h --include=*.cmake .`

- [ ] **Step 11 — `WTF/wtf/unicode/java/UnicodeJava.h` (278 lines)**
  **Not a standalone deletion, and the only step that touches an upstream WebKit file.**
  Three references:
  1. `WTF/wtf/unicode/Unicode.h:36` — inside `#elif USE(JAVA_UNICODE)` (`:30-40`). Guarded, off.
  2. `WTF/wtf/PlatformJava.cmake:18` — a header-list entry.
  3. **`WebCore/dom/Document.cpp:383` — inside `#if PLATFORM(JAVA)` only (`:382-384`), NOT
     `USE(JAVA_UNICODE)`.** `PLATFORM(JAVA)` is true for this port, so **`Document.cpp` includes this
     header in every build.** Deleting the header without editing `Document.cpp` breaks the build.
  *Also edit:* remove `Unicode.h:34-36` (the `#elif` arm), `WTF/wtf/PlatformJava.cmake:18`, and
  `Document.cpp:382-384`. The `Document.cpp` edit touches an upstream file, which the
  `jfx-web-native` skill asks you to avoid — acceptable here because the block is a fork-added
  `#if PLATFORM(JAVA)` section, but call it out in the commit message so the next WebKitGTK merge is
  not surprised. `UnicodeJava.h` also carries the tree's only
  `#include "com_sun_webkit_dom_CharacterDataImpl.h"` (`:34`), so this step removes one
  generated-JNI-header dependency (section 12).
  *Orphans a Java native:* no.
  `grep -rn 'UnicodeJava\|USE(JAVA_UNICODE)\|USE_JAVA_UNICODE' --include=*.cpp --include=*.h --include=*.cmake .`

#### Step 12 — verify

- [ ] **Step 12 — build and test.**
  This repository cannot compile WebKit (section 12), so the proof is an out-of-band build plus the
  module suite:
  ```
  # 1. rebuild jfxwebkit from this tree with your WebKit toolchain, then:
  mvn -pl modules/javafx.web test -Djfx.web.skipTests=false
  mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Dtest='test/robot/javafx/web/**'
  ```
  `PARITY: exact` holds because nothing deleted contributed a single instruction to the shipping
  library: every `.cpp` is either absent from its build list or an empty translation unit, and every
  header expands to nothing. The tests are a regression net, not a parity measurement.

### 15.3 Running totals

| Step | Item | Lines | Standalone? |
|---|---|---|---|
| 1 | `WTF/wtf/java/RunLoopJava.cpp` | 66 | yes |
| 2 | `WebCore/platform/java/JavaEnv.h` | 0 | yes |
| 3 | `WebCore/platform/java/TextBreakIteratorJava.cpp` | 257 | yes |
| 4 | `WebCore/bindings/java/JavaNodeFilterCondition.cpp` | 53 | yes |
| 5 | `WTF/wtf/unicode/java/UnicodeJava.cpp` | 165 | yes |
| 6 | `WebCore/bindings/java/JavaNodeFilterCondition.h` | 54 | + 1 cmake line |
| 7 | `WebCore/platform/java/TextNormalizerJava.{cpp,h}` | 119 | + 2 guarded blocks in `TextEncoding.cpp` |
| 8 | `WebCore/platform/java/TextCodecJava.{cpp,h}` + `WebCore/editing/java/SmartReplaceJava.cpp` | 262 + 67 | + 2 guarded blocks in `TextEncodingRegistry.cpp`, 1 line in `SourcesJava.txt` |
| 10 | `WTF/wtf/java/DbgUtils.h` | 381 | + 3 lines in `RenderingQueue.h`, 1 cmake line |
| 11 | `WTF/wtf/unicode/java/UnicodeJava.h` | 278 | + `Document.cpp`, `Unicode.h`, 1 cmake line |
| | **Total** | **1,702** | **13 files** |

Steps 1-5 alone are **541 lines** with no edit to any other file — the risk-free core of the commit
if you want to split it further.

### 15.4 The `PURE` group, sorted by what to do with it

The section 1 triage table listed 10 `PURE` items. They are not all the same kind of thing:

| Item | In the checklist? | Disposition |
|---|---|---|
| `RunLoopJava.cpp` | step 1 | dead, delete |
| `DbgUtils.h` | step 10 | dead, delete (needs the `RenderingQueue.h` edit) |
| `UnicodeJava.cpp` / `.h` | steps 5, 11 | dead, delete (`.h` needs the `Document.cpp` edit) |
| `TextCodecJava` | step 8 | dead, delete |
| `TextNormalizerJava` | step 7 | dead, delete |
| `TextBreakIteratorJava` | step 3 | dead, delete |
| `JavaNodeFilterCondition` | steps 4, 6 | dead, delete |
| **`TextBreakIteratorInternalICUJava.cpp`** (56) | **no** | **Built** (`WTF_SOURCES`). WebKit links `currentSearchLocaleID` and `currentTextBreakLocaleID`; both return the literal `"en"` (`:37-54`). No Java caller exists, so there is nothing to reimplement in Java — **keep the file, do not touch it.** Its `FIXME-java` at `:42` (use `Locale.getDefault()`) is a behaviour change to line-breaking and needs its own commit and its own test. |
| **`CPUTimeJava.cpp`** (17) | **no** | **Built** (`WTF_SOURCES`). WebKit links `CPUTime::get` and `CPUTime::forCurrentThread`; both are stubs (`:6-14`). No Java caller. **Keep.** One thing to check on the next real build: `WTF_SOURCES` also adds `win/CPUTimeWin.cpp` on WIN32, so on Windows two definitions of `CPUTime::get` may be in play — a duplicate-symbol error or a silently-wrong reading. I did not resolve this. |
| **`JavaMath.h`** | **no** | **`PARITY: unprovable` — must stay.** See section 14. |

So: **7 of the 10 are dead code in the checklist; 2 are built stubs to leave alone; 1 is
`PARITY: unprovable`.** None of the 10 needs a Java reimplementation or a parity test — there is no
`PURE` item in this scope that is both built and has a Java caller to move it to. The only
`PARITY: unprovable` items in the whole scope are `JavaMath.h::hypot` and `StringJava.cpp`
(section 14).

### 15.5 The 14 `WRAPPER` verdicts, with the reason and the Java-native question

None of the 14 is a file deletion on its own; each is "the C body stops existing, Java does the call
or the callback table does". Only two of them delete a Java `native` declaration.

| # | Item | Why Java can do it directly | Deletes a Java `native`? |
|---|---|---|---|
| 1 | `JavaEnv.cpp` `JNI_OnLoad`/`JNI_OnUnload` (`:117,119,154`) | Its whole job is capturing `JavaVM*` and pre-resolving one class; `wkj_init` is called from Java at a point where both are already known. | no (not a `Java_*` export) |
| 2 | `JavaEnv.cpp` `PL_GetLogger` (`:54`) | `PerfLogger.getLogger(String)` is a plain Java static. | no |
| 3 | `JavaEnv.cpp` `PL_ResumeCount` (`:69`) | plain Java instance call. | no |
| 4 | `JavaEnv.cpp` `PL_SuspendCount` (`:82`) | plain Java instance call. | no |
| 5 | `JavaEnv.cpp` `PL_IsEnabled` (`:95`) | plain Java instance call. | no |
| 6 | `JavaEnv.cpp` `CheckAndClearException` (`:36`) | The Java upcall stub must catch `Throwable` anyway; it can record the flag itself. | no |
| 7 | `JavaRef.h` `JLocalRef`/`JGlobalRef` | Reference lifetime becomes a Java-side registry refcount; JNI ref kinds have no FFM analogue. | no |
| 8 | `MainThreadJava.cpp` `scheduleDispatchFunctionsOnMainThread` (`:47`) | one static void call into `MainThread`. | no |
| 9 | `MainThreadSharedTimerJava.cpp` `setFireInterval`/`stop` (`:41,:56`) | two static calls into `Timer`. | no |
| 10 | `IDNJava.cpp` `toASCII` (`:56`) | It calls `java.net.IDN.toASCII`, a **JDK** API — Java can call it without leaving Java. (ICU's `uidna_nameToASCII` is the alternative, but that is IDNA2003 vs UTS-46 and `PARITY: unknown`.) | no |
| 11 | `LocalizedStringsJava.cpp` `getLocalizedProperty` (`:38`) | one static call into `LocalizedStrings`; most sibling functions already `return String()`. | no |
| 12 | `PAL/pal/system/java/SoundJava.cpp` `systemBeep` (`:38-52`) | `Toolkit.getDefaultToolkit().beep()` is pure Java — and dragging **AWT** into a JavaFX process for a beep is worth removing on its own merits. | no |
| 13 | **`WidgetJava.cpp` `Java_com_sun_webkit_WCWidget_initIDs` (`:225`)** | Body is five `GetMethodID` calls and nothing else; the IDs become `WkjWidgetCallbacks` entries filled by Java. | **yes — `WCWidget.initIDs()`, `WCWidget.java:122`; also drop the `static { initIDs(); }` at `:36`** |
| 14 | **`PluginWidgetJava.cpp` `Java_com_sun_webkit_WCPluginWidget_initIDs` (`:63`)** | Body is four `GetMethodID` plus five `GetFieldID` calls; becomes a callback table plus explicit parameters. | **yes — `WCPluginWidget.initIDs()`, `WCPluginWidget.java:48`; also drop the `static { initIDs(); }` at `:51`** |

Two more `WRAPPER`-shaped items appear in the section 9.2 triage table but are **not** among the 14,
because they are groups rather than single functions: the 14 `Class.valueOf` boxing calls in
`JNIUtilityPrivate.cpp:157-299`, and the 18 `PG_Get*Class` accessors in `PlatformJavaClasses.cpp`
(~190 lines). Neither deletes a Java `native`. Both go with the Phase-B host struct.

**Sequencing.** Items 1-12 land with the `WebKitJavaHost` change (recommendation step 2) and are
behaviour-neutral. Items 13-14 land with the platform-callback step (recommendation step 7) and are
the only two `WRAPPER` verdicts that change a Java class's shape — schedule them together so the two
`initIDs` natives and their two `static {}` blocks disappear in one commit.
