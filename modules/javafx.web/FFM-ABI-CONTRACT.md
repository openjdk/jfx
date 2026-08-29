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

* Java keeps a registry `ConcurrentHashMap<Long, Object>` with Java-assigned ids.
* C sees `typedef uint64_t wkj_ref;` (0 = null).
* `WKJHost` provides `retain(wkj_ref)` and `release(wkj_ref)`; a small RAII wrapper `WKJHandle`
  in C++ reproduces the `JLocalRef` / `JGlobalRef` copy, move and assign semantics on top of it.
* Registry entries are removed on dispose; a leak test asserts the map empties.

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

`modules/javafx.web/src/android/**` and `src/ios/**` declare their own `native` methods and load
their own libraries, but the Maven build compiles only `src/main/java` plus `target/gensrc/java`.
They are not built here and are left untouched; note them in the final report rather than editing
them.

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
