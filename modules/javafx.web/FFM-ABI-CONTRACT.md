# javafx.web JNI -> FFM: the ABI contract

Status: authoritative design contract for the `javafx.web` JNI removal. Every agent working on
this migration must follow it exactly. Derived from the `jni-to-ffm-migration`, `jfx-web-native`,
`jfx-ffm-testing` and `openjfx-conventions` skills plus a full mechanical inventory of the tree.

## 0. Measured starting point

| Surface | Java `native` | `JNIEXPORT` | Files | C++ LOC |
|---|---:|---:|---:|---:|
| DOM bindings (`WebKitLegacy/java/DOM` + `bindings/java/dom3/java`) | 1900 | 1833 | 105 + 105 | 17396 |
| WebKitLegacy core (`WebCoreSupport`, `WebPage.cpp`) | 91 + misc | ~116 | 14 | 8884 |
| `WebCore/platform/java` | - | 12 | 29 | 7628 |
| `WebCore/platform/graphics/java` | - | 12 | 27 | 7385 |
| `WebCore/platform/network/java` | - | 10 | 4 | 1964 |
| `WebCore/bindings/java` | - | 5 | 4 | 801 |
| `WebCore/bridge/jni` (LiveConnect) | - | 9 | 20 | 4163 |
| `WTF/wtf/java` | - | 5 | 11 | 1987 |
| `Tools/DumpRenderTree/java` | 11 | 12 | 6 | 1797 |
| **Total** | **2064** | **2015** | **224** | **~49400** |

Baseline before any change (recorded on this branch): `mvn -pl modules/javafx.web test
-Djfx.web.skipTests=false` = **473 tests, 1 pre-existing failure** (`LoadTest.loadJarFile`,
`ReferenceError: Can't find variable: jsc0`), 113 skipped, using the prebuilt JNI
`jfxwebkit.dll` in `../caches/sdk/bin`.

## 1. Shape taxonomy (measured, not assumed)

Extracted with `buildtools/ffm-web/extract-jni.pl`.

**The DOM layer contains no object types at all.** All 1833 functions are built from
`jlong jstring jint jboolean jshort jfloat jdouble void` plus the leading `JNIEnv*, jclass`.
64 distinct shapes; the top ten cover 1679 of them. This layer is therefore transformed by
script, with no hand editing (`jfx-web-native`: do not hand-edit the DOM binding files).

Only the CORE layer carries object types, and only in about 30 functions
(`jobject`, `jobjectArray`, `jintArray`, `jlongArray`, `jbyteArray`, `jfloatArray`). Those are
hand-designed; everything else in CORE is mechanical too.

## 2. Type mapping (mandatory)

| JNI | C ABI | Java FFM layout |
|---|---|---|
| `jlong` (peer/pointer) | `int64_t` | `JAVA_LONG` |
| `jint` | `int32_t` | `JAVA_INT` |
| `jshort` | `int16_t` | `JAVA_SHORT` |
| `jboolean` | `int32_t` (**not** `int8_t`; FFM has no boolean layout) | `JAVA_INT` |
| `jfloat` / `jdouble` | `float` / `double` | `JAVA_FLOAT` / `JAVA_DOUBLE` |
| `JNIEnv*`, `jclass`, `jobject` receiver | dropped | - |
| `jstring` parameter | `const uint16_t* s, int32_t s_len` | `ADDRESS, JAVA_INT` |
| `jstring` return | `const uint16_t*` plus `int32_t* out_len` out-param | `ADDRESS` returned, `ADDRESS` arg |
| `jobject` parameter | `wkj_ref` (`uint64_t` registry id) or flattened scalars | `JAVA_LONG` |
| `jobject` return | never; C returns scalars and Java builds the object | - |
| primitive array param | `const T* data, int32_t len` | `ADDRESS, JAVA_INT` |
| primitive array return | `int32_t wkj_x(..., T* out, int32_t out_cap)` returning count | `ADDRESS, JAVA_INT` |

### 2.1 Strings

WebKit's `WTF::String` is UTF-16 (or Latin-1) internally and JNI used `NewString`/`GetStringChars`,
i.e. **UTF-16**. The C ABI therefore uses UTF-16 throughout. Modified UTF-8 is never introduced,
so the embedded-NUL and supplementary-character hazards of `GetStringUTFChars` do not arise.

* **Into C**: `const uint16_t* s, int32_t s_len`. `s == NULL` means Java `null`;
  `s != NULL && s_len == 0` means the empty string. This distinction is load-bearing across the
  DOM and must be preserved exactly.
* **Out of C**: the function returns `const uint16_t*` and writes the length through an
  `int32_t*` out-parameter. A `NULL` return means Java `null`. The returned buffer is owned by a
  **per-thread string arena inside the library** and stays valid only until the next `wkj_*` call
  on that thread. The generated Java facade copies it into a `String` immediately and never
  retains it; because the facade is generated, that invariant lives in exactly one place.
  This mirrors the lifetime a JNI local ref had, and costs one downcall rather than two.
* The Java side allocates its UTF-16 input with `arena.allocateFrom(JAVA_CHAR, s.toCharArray())`,
  or `MemorySegment.ofArray(char[])` under `Linker.Option.critical(true)` on hot paths.

### 2.2 Exceptions

The JNI pending-exception model (`ThrowNew`, then `ExceptionCheck` inside `JavaReturn`) is replaced
by a **thread-local exception slot** that Java reads directly from memory - zero downcalls when
nothing was thrown, which is the overwhelmingly common case.

```c
typedef struct WKJExceptionSlot {
    int32_t         type;            /* WKJ_EXC_NONE = 0 */
    int32_t         code;            /* DOM exception code */
    const uint16_t* message;         /* per-thread arena, valid until cleared */
    int32_t         message_length;
} WKJExceptionSlot;

WKJ_EXPORT WKJExceptionSlot* wkj_exception_slot(void);   /* one per calling thread */
```

Java caches the pointer in a `ThreadLocal<MemorySegment>`; after any fallible call it reads
`type` with a plain `get(JAVA_INT, 0)` and, only on a non-zero value, builds and throws the same
exception JNI threw, then clears the slot. Exception types keep the existing
`JavaDOMException / JavaEventException / JavaRangeException / JavaUndefinedException` numbering
from `JavaDOMUtils.h`.

## 3. Java object handles (replaces `JLObject` / `JGObject` / `NewGlobalRef`)

Native code never holds a Java reference. Instead:

* Java keeps the registry in `WebKitNative`: a `WKJLongMap<Entry>`, a concurrent map keyed by
  primitive `long` so that a lookup does not box, from Java-assigned ids to entries. Ids are
  monotonic and never reused. Each entry holds its object strongly or weakly and carries a
  reference count.
* C sees `typedef uint64_t wkj_ref;` (0 = null).
* `WKJHost` provides `retain(wkj_ref)` and `release(wkj_ref)`, plus `retain_weak` and `is_live`
  for weak ids; a small RAII wrapper `WKJHandle` in C++ keeps the `JLocalRef` / `JGlobalRef` copy,
  move and assign shape on top of them.
* The registry does **not** intern by object identity. Registering an object mints a fresh id at
  count 1 on every call, so one object can have several ids. `retain` on a strong id returns the
  **same** id with its count raised by one; `retain` on a weak id mints a new strong id, or
  returns 0 once the object has been collected; `retain_weak` on a strong or weak id mints a new
  weak id, and returns 0 without minting for 0, an unknown id or an id whose object has been
  collected; and `release` drops one count and removes the entry at zero. §13.1 row 5 records
  how this was settled.
* Two consequences for C++ authors. A copy of a `WKJHandle` that holds a strong id shares its
  owner's id, where a JNI `NewGlobalRef` copy was an independent reference, so a stray extra
  `release` does not fail: it consumes a reference another holder still counts on. A copy of a
  handle that holds a weak id goes through `retain` and gets a new strong id, or 0 once the
  object has been collected. And equal ids name the same object, but one object can have unequal
  ids, so ask `core.equals` when identity matters.
* Registry entries are removed when the page that took them is disposed, and
  `test.javafx.scene.web.WebKitRegistryLeakTest` asserts it against the real library (a module
  test, so it runs with `-Djfx.web.skipTests=false`). Each cycle uses DOM wrappers, Java event
  listeners, the back-forward list and a Java object bound into page script, then navigates away
  and disposes the page. After a burn-in the registry must return to its baseline within two ids
  over six cycles, and a second interval that leaks one id per cycle proves the same measurement
  catches the smallest leak. The map does not empty: ids the library keeps for the life of the
  process, such as its cached `JSObject.UNDEFINED`, are taken during the burn-in and counted in
  the baseline. `WebKitRegistryTest` checks the registry's own counting against wkjstub.

## 4. Upcalls: one process-wide host table

The ~135 upcall sites are cached `jmethodID`s invoked on a `jobject`, i.e. `(object, method)`.
That maps exactly onto `(wkj_ref, function pointer)`, so a **single process-wide host table** is
used rather than per-client tables - installed once, from Java, at library init:

```c
WKJ_EXPORT int32_t wkj_init(const WKJHost* host, int32_t host_size, uint32_t abi_version);
```

`WKJHost` is a struct of named sub-structs (`core`, `webpage`, `frameloader`, `chrome`, `editor`,
`contextmenu`, `inspector`, `drag`, `graphics`, `network`, `media`, `filesystem`, `theme`).
Every entry takes its target `wkj_ref` as first parameter. The library must tolerate a `NULL`
slot in any table. `host_size` and `abi_version` let a stale library fail with a clear message
instead of crashing.

Java installs the table from **one** `Arena.ofShared()` created once per process (legitimate: the
table outlives everything and is created exactly once). Per-object stubs are not used.
Every upcall target catches `Throwable`, logs through `PlatformLogger` and returns a default - an
escaping exception would terminate the JVM.
A failed target also sets a per-thread flag, which `core.check_and_clear_exception` reports and
clears in place of `WTF::CheckAndClearException(env)`. The page, popup menu, back/forward and colour
chooser tables and the DumpRenderTree table are the exception: their targets log the failure and
clear the flag instead (`WebKitNative.clientCallbackFailed`), because their only callers are the
WebKitLegacy client classes and the harness, which never ask, and the JNI code cleared the pending
exception straight after almost every one of those calls. A flag left set there would be reported
to the next unrelated caller that does ask, such as `ImageBufferJavaBackend::create`. The
`WKJLiveConnectHost` targets (`webkit_java_api_bridge.h`) log the failure and leave the flag as it
was (`WebKitNative.logContainedFailure`): no caller in `Source/WebCore/bridge` asks either, so a
flag set there could only mislead a later, unrelated caller (§13.3).

Threading is unchanged: whatever marshalled to the FX or WebKit thread before still does, in the
same place.

## 5. ABI version guard

```c
#define WKJ_ABI_VERSION 1u
WKJ_EXPORT uint32_t wkj_abi_version(void);
```

`WebKitNative` checks this immediately after loading the library and throws an
`UnsatisfiedLinkError` naming the expected and the actual version. This is what turns "an old
prebuilt jfxwebkit is on the library path" from an obscure crash into one readable sentence.

## 6. Java-side structure

* `com.sun.webkit.WebKitNative` - the one place holding `Linker`, `SymbolLookup`, the
  `downcall()` helper, library loading, host-table installation, the object registry, the
  UTF-16 string codec and the exception-slot check. All restricted calls live here.
* `com.sun.webkit.dom.<Type>Native` - one generated facade per DOM type (105 classes), so symbol
  resolution is lazy per type instead of 1900 lookups at startup.
* Other `*Native` facades follow the owning class: `WebPageNative`, `BackForwardListNative`,
  `SharedBufferNative`, `URLLoaderNative`, `SocketStreamHandleNative`, `WCRenderQueueNative`,
  `WCMediaPlayerNative`, `JSObjectNative`, `DumpRenderTreeNative`.
* Handles stay `long peer` at existing boundaries (the DOM `NodeImpl` self-disposer hash table is
  keyed on `long peer`; changing it is a behaviour risk with no benefit). `MemorySegment` is used
  inside the facades.
* No `@SuppressWarnings("restricted")` outside `*Native` classes. No wildcard imports; list the
  `ValueLayout` constants individually.

## 7. Deletion targets (C/C++ that goes away entirely)

Ruled by the native-necessity triage. Deletions land in commits **separate** from the
behaviour-neutral migration commits.

| Target | Why |
|---|---|
| `Source/WebCore/bridge/jni/**` (~4160 LOC) | LiveConnect value conversion done reflectively through JNI; re-implemented as Java-side marshalling over a tagged-union value ABI (Phase D) |
| `Source/WTF/wtf/java/JavaEnv.{h,cpp}`, `JavaRef.h` | the `JavaVM` / `JNIEnv` abstraction itself; replaced by `WKJHost` and `WKJHandle` |
| `JNI_OnLoad`, `JNI_OnUnload`, `JNI_OnLoad_jfxwebkit` | replaced by `wkj_init` |
| every `_initIDs` and cached `jmethodID` / `jfieldID` / `jclass` | replaced by the host table |
| `find_package(JNI REQUIRED)` in `Source/cmake/OptionsJava.cmake` and the `JAVA_INCLUDE_PATH{,2}` entries in `Source/{WTF/wtf,WebCore,JavaScriptCore}/PlatformJava.cmake` and `Tools/{DumpRenderTree,TestRunnerShared}/java/CMakeLists.txt` | the library no longer needs the JVM headers |

## 8. Hard constraint: the library must be rebuilt

`modules/javafx.web` **does not compile WebKit**; `mvn` builds only Java. The prebuilt
`jfxwebkit.dll` in use here exports 1956 `Java_com_sun_*` JNI symbols and none of the `wkj_*` ABI.
Consequently:

* the Java side of this migration is fully compiled and unit-tested in this repository;
* the C++ side is **not compiled by any build in this repository** and cannot be, so it is
  reviewed and mechanically generated rather than compiler-verified;
* the 473 module tests cannot pass until `jfxwebkit` is rebuilt from the migrated sources with the
  WebKit CMake and ninja toolchain.

That was the state when this contract was written. `jfxwebkit` is now built from the migrated
sources out of tree, by `.github/workflows/build-webkit.yml`, and the `jfxwebkit.dll` in
`../caches/sdk/bin` exports 1963 `wkj_*` symbols, 0 `Java_*` and no `JNI_OnLoad`, exactly the set
the Java facades bind; the module suite runs against it (`FFM-STATUS.md` section 3). The opening
sentence still holds: the Maven build compiles Java and `wkjstub`, never WebKit.

To keep the binding layer genuinely verified rather than merely written, the migration ships a
generated **`wkjstub`** test library implementing the whole `wkj_*` ABI with recording stubs,
compiled by the module test build. It exercises symbol resolution, every descriptor, string round
trips, the exception slot, the registry and the upcall table for real.

## 9. Appendix: fixed helper names (generator and hand-written code must agree)

The DOM layer is rewritten by `buildtools/ffm-web/dom-to-ffm.pl`. Measured facts that make this
safe: the 105 DOM `.cpp` files contain **zero** direct `env->` calls, and every use of `env` is one
of six constructs. The generator therefore rewrites signatures mechanically and applies six body
substitutions; the helpers below are hand-written once, in
`Source/WebCore/bindings/java/WKJDOMUtils.h` (replacing `JavaDOMUtils.h`).

| Occurrences | JNI construct | Replacement |
|---:|---|---|
| 400 | `String(env, x)` | `WKJString(x, x##_length)` |
| 340 | `JavaReturn<String>(env, x)` | `WKJReturnString(result_length, x)` |
| 476 | `JavaReturn<T>(env, x)` | `WKJReturnPeer<T>(x)` |
| 119 | `raiseOnDOMError(env, x)` | `raiseOnDOMError(x)` |
| 38 | `raiseTypeErrorException(env)` | `raiseTypeErrorException()` |
| 1 | `Java_com_sun_webkit_dom_NamedNodeMapImpl_setNamedItemImpl(env, ...)` | direct call to the renamed function |

Exact C++ helper signatures (namespace `WebCore`):

```c++
WTF::String     WKJString(const uint16_t* s, int32_t length);          /* nullptr -> null String */
const uint16_t* WKJReturnString(int32_t* outLength, const WTF::String&); /* per-thread arena */
template<typename T> int64_t WKJReturnPeer(T*);                        /* leakRef, as JavaReturn did */
template<typename T> int64_t WKJReturnPeer(RefPtr<T>);

void            raiseTypeErrorException();
void            raiseNotSupportedErrorException();
void            raiseDOMErrorException(WebCore::Exception&&);
template<typename T> T  raiseOnDOMError(ExceptionOr<T>&&);
template<typename T> T* raiseOnDOMError(ExceptionOr<Ref<T>>&&);
template<typename T> T* raiseOnDOMError(ExceptionOr<RefPtr<T>>&&);
WTF::String     raiseOnDOMError(ExceptionOr<WTF::String>&&);
void            raiseOnDOMError(ExceptionOr<void>&&);
```

`WKJReturnPeer` and `WKJReturnString` both consult the thread exception slot exactly where
`JavaReturn::operator jlong()` / `operator jstring()` consulted `env->ExceptionCheck()`, and
return `0` / `nullptr` respectively when an exception is pending. This preserves the existing
control flow byte for byte.

Symbol naming: `Java_com_sun_webkit_dom_<Type>Impl_<method>Impl` becomes
`wkj_dom_<Type>_<method>`. The trailing `Impl` on both the class and the method is dropped; the
Java side keeps its `<method>Impl` static method names so the 105 pre-generated `*Impl.java`
wrapper bodies are untouched apart from the `native` declaration becoming a call into the
generated `<Type>Native` facade.

Export macro (`webkit_java_api.h`):

```c
#if defined(_MSC_VER)
#  define WKJ_EXPORT __declspec(dllexport)
#else
#  define WKJ_EXPORT __attribute__((visibility("default")))
#endif
```

## 10. Addendum: the surface is wider than §0 (corrections from a full sweep)

§0 was measured from the four well-known directories. A complete sweep found **227** JNI-touching
files under `src/main/native`, **60 of them outside** those four. Corrections and additions:

* DOM file counts are **108 `.cpp` / 109 `.java`**, not 105/105. Hand-written `native` methods in
  `src/main/java` are **171**, not 164.
* Three more directories carry real JNI upcalls and were not in §0:
  * `Source/WebCore/PAL/pal/crypto/java/CryptoDigestJava.cpp` - upcalls into
    `java.security.MessageDigest` (`CallStaticObjectMethod`, `NewDirectByteBuffer`).
  * `Source/WebCore/PAL/pal/system/java/SoundJava.cpp` - upcall to the AWT default toolkit.
  * `Source/WTF/wtf/unicode/java/UnicodeJava.cpp` - cached `jclass` global ref on
    `java.lang.Character` for case mapping.
* **`jni.h` leaks into platform-neutral headers** through type aliases, which is why a
  directory-scoped migration is not enough. Each must be retyped as part of Phase B:
  `platform/Widget.h` (`typedef JGObject PlatformWidget`), `platform/Cursor.h`
  (`using PlatformCursor = jlong`), `platform/PlatformKeyboardEvent.h`,
  `platform/PlatformTouchEvent.h` (carries a live `JNIEnv*` + `jobject` in a constructor),
  `platform/PlatformMouseEvent.h`, `platform/graphics/Glyph.h` (`typedef jint Glyph`),
  `platform/graphics/GlyphBufferMembers.h`, `platform/graphics/Icon.h`,
  `platform/graphics/Pattern.h`, `platform/graphics/ComplexTextController.h`,
  `platform/graphics/transforms/TransformationMatrix.h` (stray include only),
  `wtf/URL.h` (`URL(JNIEnv*, jstring)`), `wtf/text/WTFString.h` (`toJavaString`,
  `fromJavaString` - the core string bridge), `bindings/java/EventListenerManager.h`.
* `Source/JavaScriptCore/**`, `Source/WebGPU/**`, `Source/ThirdParty/**`, `Source/bmalloc/**` and
  `Configurations/**` contain **zero** JNI tokens - but `Source/JavaScriptCore/PlatformJava.cmake`
  still links `${JAVA_JVM_LIBRARY}` and adds the JDK include dirs. Pure removal candidate.

### 10.1 Exported symbols are controlled by mapfiles, not just a macro

`Source/WebKitLegacy/PlatformJava.cmake:163-168` passes explicit export maps to the linker:
`-exported_symbols_list Source/WebCore/mapfile-macosx` on Apple, `-version-script=
Source/WebCore/mapfile-vers` elsewhere. `mapfile-vers` lists `JNI_OnLoad`, `JNI_OnUnload` and
~1712 `Java_com_sun_webkit_dom_*` symbols explicitly; `mapfile-macosx` lists ~1464 (the two are
already out of sync - a pre-existing inconsistency, not something this migration fixes).

**`WKJ_EXPORT` alone is therefore not sufficient on Linux or macOS**: every new `wkj_*` symbol
must also appear in both mapfiles, or `SymbolLookup.find` fails at runtime with no build error.
These mapfiles are also a Phase-F deletion target once the JNI entries are gone.

### 10.2 The DOM code generator is stale in two independent ways

`Source/WebCore/bindings/scripts/CodeGeneratorJava.pm` (1561 lines) generates both the
`Java*.cpp` bindings and the `*Impl.java` wrappers, but:

1. It is **not wired into any build** - `GENERATE_BINDINGS` is only ever invoked with
   `GENERATOR JS`. The 108 + 109 files are checked-in artefacts.
2. It **no longer runs**: it calls `$codeGenerator->LinkOverloadedFunctions`, which upstream
   removed from `CodeGenerator.pm`. That is the only missing method.
3. Its output has **drifted** from the checked-in files (the `#define IMPL` idiom, include style,
   and the Oracle GPL headers in place of the generated banner), so even once repaired it would
   not reproduce the tree byte-for-byte.

Consequence: the checked-in files are transformed directly by script, and the generator is
migrated in the same shape so it cannot silently re-emit JNI later. Whether the generator can
reproduce the tree is being measured rather than assumed.

### 10.3 Tests carry no JNI dependency

`modules/javafx.web/src/test` (46 files) and `tests/system/.../robot/javafx/web` contain zero
`native` declarations, zero `System.loadLibrary`, zero `initIDs` and zero `UnsatisfiedLinkError`
assertions. They exercise public API only, so no test needs porting for JNI reasons - they need
a working library, nothing more.

### 10.4 Out of scope

`modules/javafx.web/src/android/**` and `src/ios/**` declared their own `native` methods and
loaded their own `webview` library, but the Maven build compiles only `src/main/java` plus
`target/gensrc/java`, so they were never built here. Both trees have since been deleted, as
javafx.graphics dropped its iOS and Android code in PR #12 (commit 26ce75d02f).

## 11. Corrections from the DOM audit and the generator experiment

### 11.1 §2.1 was wrong about inbound null (behaviour-preservation)

§2.1 says a NULL inbound pointer "means Java null". For the **outbound** direction that is right and
load-bearing (`DOMTest` asserts `assertNull(document.getDocumentURI())`). For the **inbound**
direction it is wrong as a statement about current behaviour: `WTF::String::String(JNIEnv*, const
JLString&)` in `Source/WTF/wtf/java/StringJava.cpp:34-54` maps a null `jstring` **and** a
zero-length `jstring` to `StringImpl::empty()`. A Java `null` therefore reaches WebCore today as the
*empty* string, not as a null string.

`WKJString` must reproduce that collapse exactly, and does. "Fixing" it would change what
`element.setAttribute("x", null)` does. The asymmetry is deliberate: **null and empty collapse on
the way in; they are distinguished on the way out.**

### 11.2 The counts in §0 and §1 are superseded

Measured three ways (a signature extractor, an independent audit, and the shipped DLL export table):

| | value | note |
|---|---:|---|
| DOM `JNIEXPORT` a raw grep reports | 1833 | |
| ...**live** | **1831** | 2 sit inside a `/* */` block in `JavaMouseEvent.cpp:123,129` |
| ...**actually compiled** | **1796** | `JavaDOMSelection.cpp` (26) and `JavaWheelEvent.cpp` (9) are commented out of `Source/WebKitLegacy/PlatformJava.cmake:10-11` |
| DOM `.cpp` / `.java` files | 108 / 109 | not 105 / 105 |
| Java `native` decls with no C implementation | **100** | 65 with no C at all, plus the 35 above; all throw `UnsatisfiedLinkError` today and must keep doing so |

`buildtools/ffm-web/dom-cpp-to-ffm.pl` enforces all of this: it skips `JNIEXPORT` inside comments,
and marks rows from non-compiled sources `BUILT=0` so the header, the mapfiles and the Java facades
all drop them. Binding a symbol the library does not export is an `UnsatisfiedLinkError` at
class-initialisation time, which would take out every user of `MouseEventImpl`, `DOMSelectionImpl`
and `WheelEventImpl`.

### 11.3 `jlong` is not always a pointer

Three DOM functions carry `jlong` **values**: `EventImpl.getTimeStampImpl` (milliseconds),
`HTMLInputElementImpl.getValueAsDateImpl` and `setValueAsDateImpl`. Mapping every `jlong` to
`int64_t` (as §2 does) is correct for both cases; mapping it to `void*` — the obvious alternative —
would compile and produce garbage dates.

### 11.4 The DOM code generator cannot regenerate this tree

`CodeGeneratorJava.pm` produces both halves of the DOM bindings, and was the original source of the
checked-in files, but regeneration is not a viable strategy. It was repaired far enough to run
(upstream renamed `LinkOverloadedFunctions` to `LinkOverloadedOperations`; the fix is 24 lines) and
swept over all 108 interfaces. **Zero files come back identical, and none differ only in
whitespace.** The blockers, in order of severity:

1. **12 interfaces have no IDL any more** — WebKit deleted `CSSCharsetRule`, `CSSPrimitiveValue`,
   `CSSValue`, `CSSValueList`, `Counter`, `Entity`, `EntityReference`, `HTMLAppletElement`,
   `HTMLBaseFontElement`, `RGBColor`, `Rect`, `CSSUnknownRule`. There is no input to regenerate from.
2. **345 `native` methods would disappear and 660 would appear** — `NodeImpl` alone would lose
   `addEventListener`, `removeEventListener`, `dispatchEvent`, `getNodeType`, `getAttributes`. That
   is the public `org.w3c.dom` contract of `javafx.web`; losing it is an API break, not a refactor.
3. The generator **cannot emit what the files contain**: `#define IMPL` (106 files),
   `WebCore::JSMainThreadNullState` (101 files — a hand-added main-thread guard), `@Override`
   (103 files, from `8328752`).
4. It would **silently change WebCore call targets**: generated `attributeWithoutSynchronization`
   where the checked-in code calls `getAttribute` — different semantics, and it comes from the
   current upstream `CodeGenerator::GetterExpression`, so patching the Java generator cannot fix it.

The generator was last modified 2017-04-29; the 217 files have been hand-maintained through ten
WebKit updates since. **They are the source of truth; the generator is an abandoned ancestor.** The
`LinkOverloadedFunctions` repair is kept anyway — it is cheap and stops the generator dying on its
first line of real work if anyone reaches for it again.

### 11.5 Exported symbols: solved with a glob, not 2000 entries

Both linkers accept patterns, so the export maps need one line each rather than a generated list:
`wkj_*;` in `Source/WebCore/mapfile-vers` (ELF) and `_wkj_*` in `mapfile-macosx` (Mach-O — the
leading underscore is why every existing entry reads `_Java_...`). Windows needs neither: the Java
port sets no `.def` file and no export list, so `__declspec(dllexport)` alone is sufficient there.
No `-fvisibility=hidden` is set for this port, so the version script is the only gate on Unix.

## 12. One ABI, not two: reconciling the core audit with the DOM half

`FFM-AUDIT-core.md` §5.0 proposes conventions that differ from §2 of this contract in three places.
The DOM half (1796 functions) is already implemented against §2, compiles, and is the larger and
more regular surface, so **§2 wins everywhere**. The core audit's prototypes are otherwise adopted
as written. The three reconciliations, with the reason:

| Core audit proposed | This contract requires | Why |
|---|---|---|
| opaque `WKJPage*` / `WKJFrame*` / `WKJItem*` handles | `int64_t`, layout `JAVA_LONG` | Java already holds these as `long pPage` / `long pFrame`. `ADDRESS` would force a `MemorySegment.ofAddress(long)` per call, which allocates a zero-length segment each time - measurable on hot paths such as frame walks. The DOM half made the same choice for the same reason. C++ keeps its type safety with `static_cast<WebPage*>(wkj_to_ptr(page))`, exactly as the DOM bindings do. |
| `typedef struct WKJStr { const uint16_t* data; int32_t length; }` passed by value | two parameters, `const uint16_t* s, int32_t s_len` | A by-value struct parameter needs a `StructLayout` and, on a struct-returning call, a per-call `SegmentAllocator`. The flat pair needs neither and is what all 1796 DOM functions already use. |
| `len < 0` means Java null | `s == NULL` means Java null | Same information, but the pointer test is the one the DOM helpers implement, and it makes a null impossible to confuse with a negative length arriving by accident. |

Everything else from the core audit stands, including the observation worth acting on separately:
the C++ currently `#include`s generated JNI constant headers (`com_sun_webkit_WebPage.h`,
`com_sun_webkit_event_WCKeyEvent.h`, `com_sun_webkit_LoadListenerClient.h`, ...) that **nothing in
this repository generates** - `modules/javafx.web/pom.xml` has no `-h` argument. Those constants
(`WCKeyEvent.VK_*`, `LoadListenerClient.PAGE_STARTED`, `WebPage.DND_DST_*`) become plain `#define`s
or enums in the C ABI header, which removes the last generated-JNI-header dependency from the C++
side and is a strict improvement over the status quo.

### 12.1 Header layout, so parallel work does not collide

The ABI is split by area, each header self-contained and included by the master:

```
Source/WebKitLegacy/java/api/
    webkit_java_api.h          core: types, WKJ_EXPORT, abi version, exception slot,
                               wkj_ref, WKJHost, wkj_init  (includes the rest)
    webkit_java_api_dom.h      GENERATED, 1796 DOM entry points
    webkit_java_api_page.h     WebPage + the seven client callback tables
    webkit_java_api_platform.h WebCore/platform: graphics, network, media, theme, filesystem
```

Each `WKJHost` sub-struct is defined in the header that owns its area, replacing the placeholder
`{ void (*reserved)(void); }` in the master header. One owner per header, so two agents never edit
the same file.

## 13. String returns: caller-provided buffers (supersedes §2.1)

An adversarial review and the DOM audit independently reached the same conclusion about §2.1's
"per-thread arena, valid until the next `wkj_*` call on this thread" rule, so it is **withdrawn**.

Why it had to go, concretely:

* **It is unenforceable.** The rule is a global invariant over a reentrant call graph. Reentrancy is
  real and live: `bridge/jni/jsc/JNIUtilityPrivate.cpp:124-131` upcalls `NodeImpl.getCachedImpl`,
  whose Java body makes two further downcalls - one of them string-returning - while the outer C
  frame still holds a pointer into the arena. Every DOM mutation that reaches
  `JavaEventListener::handleEvent` has the same shape. The generated DOM facade could honour the
  rule; the eight hand-written facades each had to honour it independently, with no type, test or
  compiler check to catch a slip. It fails as corrupted text or a crash, never as an exception.
* **It made the exception check corrupt the value it guards.** `checkException()` fetched the slot
  lazily, and that fetch is itself a `wkj_*` call - so the first fallible string-returning call on
  any thread read the string *after* its own guard had reset the arena. First call per thread only:
  an intermittent bug that survives every test that reuses a thread.
* **A stub cannot test it.** The `wkjstub` library exempted `wkj_exception_slot` from the arena
  reset and heap-allocated its messages, i.e. modelled a *more forgiving* contract than the real
  library would - at exactly the point where the bug lived.

**The replacement has no lifetime rule at all.** A string-returning function takes the caller's
buffer and copies into it before returning:

```c
enum { WKJ_STR_OK = 0,          /* *result_length code units written into result_buf */
       WKJ_STR_NULL = 1,        /* the Java-visible value is null; *result_length = 0 */
       WKJ_STR_OVERFLOW = 2 };  /* nothing written; *result_length = required capacity */

WKJ_EXPORT int32_t wkj_dom_Attr_getName(int64_t peer, uint16_t* result_buf,
                                        int32_t result_cap, int32_t* result_length);
```

Java allocates and frees; C allocates nothing and returns no pointer, so there is no ownership
question to get wrong and nothing to leak or dangle. On `WKJ_STR_OVERFLOW` the facade grows once
and retries. Null is still distinguished from empty - `WKJ_STR_NULL` versus `WKJ_STR_OK` with
length 0 - which is the outbound distinction §11.1 says is load-bearing.

**The retry runs the whole function again.** `WKJReturnString` (`WKJDOMUtils.cpp`) writes nothing
and keeps nothing on overflow, so the second call recomputes the value. That is harmless for a
getter, and the generated DOM facades (`dom-java-to-ffm.pl`) retry every string-returning row the
same way. Of the 356 built rows that return a string, 13 are not named `get`/`is`/`has`, and two of
those have a side effect, which the retry repeats:

* `CSSStyleDeclaration.removeProperty`: when the removed value is longer than the 256-unit first
  buffer, the first call removes the property and reports overflow, and the second finds nothing
  to remove and returns null. The JNI build made one call and returned the removed value.
* `DOMWindow.prompt`: when the answer is longer than 256 units, the retry opens a second modal
  dialog. If the second answer also overflows the grown buffer, `DOMStringCodec.decode` throws
  `IllegalStateException`. The JNI build showed one dialog.

This is a known, unfixed deviation from the JNI build (§13.3); `dom-abi.tsv` has no column that
marks a row as unsafe to repeat. Its reach is small. Both methods are public only on
`CSSStyleDeclarationImpl` and `DOMWindowImpl` in `com.sun.webkit.dom`, which `javafx.web` does not
export. No exported interface hands out a `CSSStyleDeclarationImpl`: only impl-only methods such
as `ElementImpl.getStyle` do. `DocumentView.getDefaultView` does return the `DOMWindowImpl`, but
as an `AbstractView`, which has no `prompt`. A script's `window.prompt` never reaches the DOM
facade: it goes through the chrome `prompt` slot, which serves its own retry from the answer it
already has (`webkit_java_api_page.h`). The upcall-side retry in `wkjFetchString`
(`wtf/java/WKJRuntime.h`) also repeats its Java slot, but every caller reads state or computes a
value (clipboard, cookies, localized strings, media types, file paths, IDN conversion), so the
repeat is safe; a value that grows between the two reads comes back null.

The same reasoning removes the arena from the exception slot: `WKJExceptionSlot` carries its message
in a fixed inline `uint16_t message[256]` with an explicit length, truncating beyond that. Every
current DOM exception message is a short canned string from `DOMException::description`, so nothing
truncates in practice, and the slot becomes self-contained.

`WKJReturnString` must widen 8-bit (Latin-1) `WTF::String`s rather than calling `span16()` on them:
`StringImpl::span16()` asserts `!is8Bit()`, and in a release build that assert is gone, leaving a
`length()`-byte heap overread. `StringJava.cpp:62-73` branches correctly today and is the model.

### 13.1 Other confirmed defects from the review, and their status

| # | Finding | Status |
|---|---|---|
| 1 | The transform rewrote `NamedNodeMapImpl_setNamedItemNSImpl`'s forwarding call by eating only `(env,`, leaving a call to an undeclared `clazz` with the wrong arity - non-compiling C++ that the script reported as success | **Fixed.** Both leading arguments are consumed, and the script now fails on any residual `env`/`clazz` and on any helper name the new headers do not declare |
| 2 | Contract §2.1 claimed inbound `NULL` means Java null; the JNI code collapses null and empty | **Fixed** in §11.1; `WKJString` implements the collapse |
| 3 | `checkException()` invalidated the string it guarded | **Fixed** by §13 - no arena, so no invalidation |
| 5 | `WKJHost.retain/release` are documented as refcounted, but the registry mints a fresh id per call, `unregister` removes unconditionally, and there is no `retain` to bind. Also: `JobjectWrapper.cpp:45` deliberately uses `NewWeakGlobalRef`, which a `ConcurrentHashMap` cannot model, and `JavaDOMUtils.cpp:138-163` needs `getJavaHashCode`/`isJavaEquals` host slots | **Fixed** with the host table. The registry counts references: `register` mints a fresh id at count 1, `retain` adds an owner and returns the same id (`retain(0) == 0`), `release` removes at zero, and `unregister` is `release`. Weak ids are `WeakReference` entries, so `retain_weak` does not pin and `is_live` answers 0 once the referent is collected; `hash_code` and `equals` delegate to the referents and are provisioned rather than load-bearing, both C callers still being dead. Interning by identity was **not** implemented: a sweep of all 101 files naming a handle type found no site comparing one handle with another |
| 6 | §2.1 suggested `critical(true)` on hot string paths; DOM setters upcall, and a critical downcall that re-enters the JVM is undefined behaviour | **Fixed** - forbidden outright on this ABI |
| 7 | `JNIUtilityPrivate.cpp:201` casts a JS number to `jboolean` (`unsigned char`), so 256 becomes `false`; `int32_t` makes it `true` | **Open, Phase D** - a bug fix, so its own commit with a `JavaScriptBridgeTest` case |
| 8 | `WebKitNative` throws `EventException`/`RangeException` for slot types 2 and 3, but the only raise path in the tree constructs `DOMException` | **Open** - collapse to one type until C sets another |
| 10 | The slot pointer is cached in a `ThreadLocal`, but `wkj_exception_slot()` returns the *carrier* thread's slot; a virtual thread that migrates reads the wrong one | **Open** - drop the cache or guard on `isVirtual()` |
| 11 | `WebKitNative` loads `jfxwebkit` in `<clinit>`, so the ABI-guard message is seen once and every later touch gets a bare `NoClassDefFoundError`; and nothing loads `wkjstub` instead | **Open** - cache and rethrow the failure; make the library name overridable for tests |
| 12 | 48 of the 124 `THROWS` functions return `void`; a missed check both swallows the exception and leaves the slot dirty, so the *next* unrelated call throws it | **Open** - C clears the slot on entry to every `wkj_*` function, and the facade checks on `void` too |
| 15 | The `CodeGeneratorJava.pm` repair is unrelated to FFM | **Accepted** - it stays, but as its own commit |

### 13.2 The reviewer's structural objection, recorded

The review's strongest point is that the plan front-loads the half that cannot be verified in this
repository and back-loads the half that can, and that under the §1.1 triage the DOM phase deletes
**no** C++ at all - it renames 1831 functions and adds helper code, so measured purely against "less
native code" it is net-negative. The phases that genuinely delete C++ are the LiveConnect bridge
(~4,163 LOC) and `JavaEnv`/`JavaRef` (~1,987 LOC), and the LiveConnect one needs no DOM change.

That is a fair characterisation and it is recorded here rather than argued away. The countervailing
facts: the DOM phase is what removes 1831 of the module's ~2100 JNI entry points, it is the surface
on which the string and exception contracts are provable, and the goal being executed is the
complete removal of JNI from the module, not only the deletion of C++. The reviewer's preferred
ordering - get a reproducible WebKit build first, prove the pipeline on one small slice, then do
LiveConnect - is the right ordering for a project that can build WebKit. This one cannot, which is
stated plainly in §8 and remains the central risk of the whole exercise.

### 13.3 Known behaviour differences from the JNI build

The port is meant to be behaviour-neutral. The differences below are known, and each says whether
it is deliberate or still open. "The JNI build" is the tree that commit 939aa61ead replaced.

* **`wkj_bfl_item_children` reuses child entries (deliberate).** A child whose
  `HistoryItem::m_hostObject` already holds an entry gets that entry back, and an entry is created
  only for a child that has none. The JNI `bflItemGetChildren` created a new
  `BackForwardList.Entry` for every child on every call and made it the host object, so an Entry
  from an earlier call was never sent `notifyItemDestroyed` and read its freed `HistoryItem` on its
  next getter call. Restoring parity would restore that use-after-free. The difference shows
  through `com.sun.webkit.BackForwardList` and DumpRenderTree, not through
  `javafx.scene.web.WebHistory`, which does not expose children. See `wkj_bfl_item_children` in
  `webkit_java_api_page.h` and `FFM-STATUS.md` section 12.2.
* **The shutdown gate applies on every thread (recorded, not changed).** Once
  `wkj_set_shutdown(1)` has run, the `retain`, `retain_weak`, `release` and `is_live` slots of the
  published `wkj_host` return 0 or do nothing, and every `WKJ_RETURN_IF_SHUTTING_DOWN` site returns
  early, on every thread. The JNI checks those sites and `JavaRef.h` replaced asked only whether
  the calling thread had a `JNIEnv`, so the FX thread, which is always attached, kept all of them
  working after the flag was set. The difference lasts from the shutdown hooks `WebPage` installs
  to the end of the process. See THE SHUTDOWN GATE in `wtf/java/WKJRuntime.h`.
* **Web Worker threads make upcalls the JNI build skipped or crashed on (deliberate).** The JNI
  build attached a `WebCore: Worker` thread only inside `WorkerThread::createGlobalScope`, so once
  the worker ran script `GetJavaEnv` answered null on it. Code that tested for that skipped its
  call there, and code that did not dereferenced a null `JNIEnv` and brought the JVM down: a
  `Path2D` operation that needs the platform path, such as `addPath` (`PathJava`; a bare
  `new Path2D('M0 0 L10 10 Z')`, `rect` and copying one into `new Path2D(p)` keep a `PathStream`
  and did not crash), `FontFace.load()` and a `FontFace` built from an `ArrayBuffer`
  (`FontCustomPlatformData`), and `createImageBitmap` from `ImageData` (`ImageBufferJavaBackend`)
  in a worker all ended the process. An FFM upcall stub attaches the thread by itself, so those
  calls now run. Their Java targets are synchronized or hand their work to the render thread, and
  the Threading note of `webkit_java_api_platform.h` lists the slots observed on a worker in
  thread-recording runs against the real library. `WebWorkerUpcallTest` pins two of the cases:
  `addPath` and a `FontFace` built from an `ArrayBuffer` complete on a worker, and the JVM
  survives. One result is kept as it was: the `ImageDecoderJava` constructor makes no Java
  decoder on a thread other than the main one that has entered the JavaScript VM, which in this
  port is a worker, so `createImageBitmap` from a `Blob` still rejects there with
  `InvalidStateError`, as the JNI constructor's `if (!env) return;` made it. Decoder WorkQueue
  threads create no decoder: they use the decoder that `BitmapImageSource` made on the main
  thread.

  A worker also reaches `RenderingQueue::flushBuffer`, through `createImageBitmap` from
  `ImageData` with a resize or crop, a resize of a bitmap transferred from the main thread, and a
  structured clone of a bitmap it holds, while the event thread removes entries from the same
  `a2bb` map in `wkj_rq_release` and adds its own for main-thread canvases. That hazard is closed
  by making the shared state thread-safe rather than refusing the paths. A static `WTF::Lock`
  guards every access to `a2bb`; entries leave it by move and are destroyed only after the lock
  is released, so no upcall runs under it. `ByteBuffer` and `RQRef` derive from
  `ThreadSafeRefCounted`, so the last reference may be dropped on either thread, and their
  destructors' upcalls (`core.release`, `graphics.ref_deref`) are safe on any thread. Refusing
  the paths by failing `ImageBufferJavaBackend::create` on a worker was ruled out: it would not
  reject the promise but abort at the `RELEASE_ASSERT` of `ImageBitmap::createBlankImageBuffer`,
  and it would not cover the clone. The fix is in the source but unbuilt until the next
  `build-webkit.yml` dispatch produces a `jfxwebkit` from it; the `jfxwebkit` in
  `../caches/sdk/bin` that the module tests run against predates it. Until then the evidence is:
  `RenderingQueue.cpp` and `RQRef.cpp` compiled against this tree's real WTF headers with GCC
  (C++23, debug and release); a multi-threaded stress run of the unmodified files on the real WTF
  `HashMap`, `Lock` (its contended path replaced by a spin) and `ThreadSafeRefCounted` that is
  clean under ThreadSanitizer, AddressSanitizer and UBSan, while the pre-change code fails it; and
  an MSVC run of a stub-WTF harness. Neither clang-cl, which builds the Windows library, nor Apple
  clang has compiled it. The comments on `RenderingQueue::flushBuffer` and `wkj_rq_release`
  describe the locking. The other shared state the worker-reachable slots touch was audited at
  the same time; what that audit left unchanged, including state a worker could reach only if
  OffscreenCanvas were enabled in workers, is in `FFM-STATUS.md` section 21.2, item 7.
* **An `ImageBitmap` closed on a worker releases its Java render objects (deliberate).** A bitmap
  transferred to a worker and closed or collected there is disposed on that thread:
  `rq_dispose_graphics` and `core.release` run on `WebCore: Worker`, and nothing is left after a
  collection, as for a bitmap closed on the main thread. The JNI build returned early from
  `RenderingQueue::disposeGraphics` and skipped `DeleteGlobalRef` on that unattached thread, so
  the `RTImage` and `WCRenderQueueImpl` global references leaked and kept the
  `WCBufferedContext`, its `ContextState` and the image's texture reachable: thirty bitmaps left
  thirty of each. The Java side is safe there: `Ref.deref` and `WCGraphicsManager.deref` are
  synchronized, `WCRenderQueueImpl.disposeGraphics` only posts to the render thread, and a
  registry release takes the entry's monitor.
  `WebKitRegistryLeakTest.imageBitmapsClosedOnAWorkerGiveTheirIdsBack` pins it: thirty bitmaps
  held on a worker take sixty ids, and closing them there gives all sixty back.
* **WorkQueue jobs that JNI never attached (recorded, not changed; read from the source, not
  measured).** On macOS the build uses `WorkQueueCocoa.cpp` (`USE_COCOA_EVENT_LOOP` in
  `OptionsJava.cmake`), whose libdispatch threads the JNI build never attached, where
  `WorkQueueGeneric.cpp` on Windows and Linux attached each job. So on macOS
  `ImageDecoderJava::createFrameImageAtIndex` returned no frame on the decoder queue,
  `BitmapImageSource` reported the decode as failed, and `img.decode()` on a loaded image
  would have rejected; the FFM upcall attaches the thread, so those frames now decode. On every
  platform, a decoder whose last reference is held by the `ImageFrameWorkQueue` closure (an idle
  animated-image queue after `destroyDecodedData(true)`, or a static image's short window after
  `stop()`) is now destroyed on the `ImageDecoder` queue thread, where the JNI destructor found no
  `JNIEnv` once `WorkQueueGeneric` had detached and leaked the `WCImageDecoderImpl`. That destroy
  is not guaranteed to finish cleanly off the FX thread: if the decoder's loader has started,
  `WCImageDecoderImpl.destroy` reaches `Service.cancel`, which throws `IllegalStateException`
  there, and `image_decoder_destroy` contains and logs it. The id is released either way. One run
  of an `await img.decode()` page and an animated GIF on a macOS build would settle whether the
  JNI-era failure was ever visible.
* **The DOM string retry repeats side effects (open).** `CSSStyleDeclaration.removeProperty` and
  `DOMWindow.prompt` run twice when their result overflows the first buffer. §13 has the effects
  and why they are hard to reach.
* **LiveConnect field and array failures are contained, not thrown (deliberate).** Page script
  that assigns a final field of an exposed Java object, or stores an element of the wrong type in a
  Java array, makes `Field.set*` or `Array.set` throw. The `field_get`, `field_set`, `array_get`
  and `array_set` slots of `WKJLiveConnectHost` log that at SEVERE and return (§4), and the script
  carries on; so does a `doubleValue()` that throws inside `unbox`. The JNI build never cleared
  these exceptions. One stayed pending while the script ran on, later JNI calls in the same script
  still ran, and it then surfaced in the first of three ways. The next LiveConnect method
  invocation reported it as its own exception and threw it into the script, because
  `dispatchJNICall` took whatever `ExceptionOccurred` returned. Failing that, the first
  `WTF::CheckAndClearException` to run printed and cleared it, and a check that branched on the
  answer, such as `ImageBufferJavaBackend::create` for a canvas the script went on to create, took
  its failure path; that is no longer reproduced. Failing both, it was thrown out of the Java
  method that had entered WebKit, such as `WebEngine.executeScript` or `JSObject.eval`, checked
  exceptions such as `IllegalAccessException` included. Doing the same would take one
  pending-exception state kept across every upcall and downcall of the library: rethrown when each
  downcall that stands for a JNI native method returns, saved and restored around every upcall,
  and cleared wherever the JNI code cleared. Some of those clearing points no longer call into Java
  at all; the `CheckAndClearException` calls in the JNI-era `strVect2JArray` (`StringJava.cpp`)
  are an example. That is a model of the whole bridge rather than a LiveConnect change, so it is
  not attempted. Nor do these slots set the flag `core.check_and_clear_exception` reports, which
  would outlive the script and fail the first canvas of the next one. The exception type can
  differ as well: `Array.set` reports a wrong element type as `IllegalArgumentException`, where
  `SetObjectArrayElement` raised `ArrayStoreException`.
  `LiveConnectParityTest.fieldAndArrayFailuresAreLoggedAndContained` and
  `aThrowingDoubleValueIsContainedAndLeavesNothingForTheNextScript` pin the current behaviour, and
  the comment above `LiveConnectNative.fieldGet` repeats this.
* **Two LiveConnect lookups reach less than `GetMethodID` did (narrowing, recorded).** Both are in
  `com.sun.webkit.dom.LiveConnectLookup`. Neither reaches a method the JNI build, which ignored
  access, could not have called, although for one rare class shape below `doubleValue()` calls a
  different one.
  * `+obj`, `obj * 2` and every other numeric conversion of an exposed object that is not a
    `java.lang.Number` call its `double doubleValue()`, as `callJNIMethod<jdouble>` did, so a
    JavaFX `DoubleProperty` converts to its value. JNI ignored access checks and module
    encapsulation and reflection does not, so a non-public `doubleValue()` in a package that is
    not open to `javafx.web` converts to 0 where JNI returned the value. Every package on the class
    path is open, so only named modules are affected by that. The search also passes over a class
    whose own methods cannot be listed, because one of them names a class missing at run time,
    where `GetMethodID` resolved the one method in it, and that can happen on the class path too.
    A public or package-private `doubleValue()` such a class declares still runs, through a
    declaration it overrides further up or in an interface, but only when there is one that
    reflection may call; otherwise the object converts to 0 where JNI returned the value. A private
    or static `doubleValue()` in such a class is not seen. JNI called the private one, and failed
    and converted to 0 on the static one, while the search goes on up and may call a private
    declaration, or a package-private one in another package, that JNI did not call for that
    object. `WebKitLiveConnectTest.aDoubleValueInAClassWhoseMethodsCannotBeListedRunsOnlyThroughOneAbove`
    pins the public cases.
  * The `toString()` behind `'' + obj` and `String(obj)` is still found when the class's
    `getMethods()` throws, typically because a public signature names a class that is missing at
    run time: a public lookup resolves the one method, as `GetMethodID` did. When the class that
    declares the override is itself the one that cannot be listed, Java cannot produce a `Method`
    for it, and `java.lang.Object`'s declaration stands in. It is used only after the allow list of
    `Utilities.fwkInvokeWithContext`, a public class in a package exported to everyone and a public
    method have all been checked on the class that declares the override, so the stand-in is never
    permitted where the override would not have been. An object whose class a public lookup cannot
    see, or whose `toString()` is declared in such a class, converts to the empty string, where the
    JNI build either answered or threw into the script.
  * Both lookups keep their answer for the life of the runtime class, and that includes no answer
    and an answer the `LinkageError` fallback found. `GetMethodID` resolved again on every call.
    Its answer never depended on anything that changes later, but the lookups' answers can: a
    class loader that later supplies the missing type, or a package opened to `javafx.web` after
    the first conversion, would let a fresh search answer where the cached answer does not.
