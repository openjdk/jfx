/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * webkit_java_api.h - the single public C ABI header of the jfxwebkit library.
 *
 * This header is the whole contract between the JavaFX Java code (which binds it with the
 * Foreign Function & Memory API) and the WebKit Java port. It knows nothing about the JVM:
 * it includes no JNI header and names no JNI type, environment, handle or cached member
 * id. It is valid C and C++; it must stay so, because the Java-side binding tests compile
 * it as C.
 *
 * See modules/javafx.web/FFM-ABI-CONTRACT.md for the design this header implements. The
 * rules a reader of this file has to know are repeated here.
 *
 * 1. TYPES
 *    Only <stdint.h> types, float, double and pointers cross the boundary. There is no
 *    boolean type: a Java boolean is int32_t (0 or 1), because FFM has no boolean layout.
 *
 * 2. STRINGS (contract 2.1, as superseded by contract 13) - UTF-16 in both directions,
 *    never modified UTF-8.
 *
 *    Into the library:  const uint16_t* s, int32_t s_len
 *        s == NULL                 -> the Java value was null
 *        s != NULL && s_len == 0   -> the Java value was the empty string
 *        Both collapse to the empty WTF::String, which is what the JNI constructor in
 *        wtf/java/StringJava.cpp has always done (contract 11.1). Null and empty collapse
 *        on the way in; they are distinguished on the way out.
 *        The library must not retain s; it is valid only for the duration of the call.
 *
 *    Out of the library: the caller provides the buffer. There is no library-owned string
 *    memory and therefore no lifetime rule anywhere in this ABI.
 *
 *        int32_t wkj_x(..., uint16_t* result_buf, int32_t result_cap,
 *                      int32_t* result_length);
 *
 *        WKJ_STR_OK       *result_length code units were written into result_buf
 *        WKJ_STR_NULL     the Java-visible value is null; *result_length = 0
 *        WKJ_STR_OVERFLOW nothing was written; *result_length is the capacity required,
 *                         so the facade grows once and calls again
 *
 *    The earlier "per-thread arena valid until the next wkj_* call" rule was withdrawn:
 *    it was a global invariant over a reentrant call graph (a Java upcall can make further
 *    downcalls while an outer C frame still holds a returned pointer), and the exception
 *    check that guarded a returned string was itself a wkj_* call, so it invalidated the
 *    value it was guarding.
 *
 * 3. JAVA OBJECTS (contract 3)
 *    Native code never holds a Java reference. A Java object is a wkj_ref: an opaque
 *    Java-assigned registry id, 0 meaning null. The library keeps a wkj_ref alive with
 *    host->core.retain and drops it with host->core.release; WKJHandle.h is the RAII
 *    wrapper that replaces JLocalRef and JGlobalRef. Ids are handles, not objects: see
 *    WKJHostCore below for the exact ownership rule and for why two ids can name the same
 *    Java object.
 *
 * 4. EXCEPTIONS (contract 2.2)
 *    C never throws. It writes a pending exception into the calling thread's
 *    WKJExceptionSlot; Java reads the slot from memory after a fallible call, throws the
 *    corresponding exception and clears the slot by storing WKJ_EXC_NONE into `type`.
 *
 *    **Every wkj_* function clears the calling thread's slot on entry.** 48 of the 124
 *    throwing DOM functions return void, where a missed check on the Java side would both
 *    swallow the exception and leave the slot dirty, so that the next unrelated call on
 *    that thread would throw an exception belonging to the previous one. Clearing on entry
 *    bounds a missed check to the call that caused it. A raise and the check that consumes
 *    it must therefore not be separated by another wkj_* call on the same thread; in the
 *    generated DOM bodies they never are.
 *
 * 5. UPCALLS (contract 4)
 *    One process-wide WKJHost table of function pointers is installed once by wkj_init.
 *    Every callback slot may be NULL: the library checks each pointer before calling it
 *    and behaves as if the call had done nothing, returning the default documented on the
 *    slot. Java never lets an exception escape an upcall.
 *
 * 6. WHAT IS NOT HERE
 *    The DOM half of the ABI - the 1796 compiled wkj_dom_<Type>_<method> entry points that replace
 *    Java_com_sun_webkit_dom_<Type>Impl_<method>Impl - is generated, not hand written. It
 *    lives in webkit_java_api_dom.h, produced by buildtools/ffm-web/spec-to-header.pl from
 *    buildtools/ffm-web/dom-abi.tsv, and that header includes this one. Never declare a
 *    DOM entry point here by hand: the signatures come from the JNI sources mechanically,
 *    which is the only way they stay exactly right.
 */

#ifndef WEBKIT_JAVA_API_H
#define WEBKIT_JAVA_API_H

#include <stdint.h>

#if defined(_MSC_VER)
#  define WKJ_EXPORT __declspec(dllexport)
#else
#  define WKJ_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------------------- */
/* ABI version (contract 5)                                                               */
/* ------------------------------------------------------------------------------------- */

/*
 * Bump this whenever anything in this header changes shape: a struct gains, loses or
 * reorders a member, a function changes signature, or a documented meaning changes.
 * WebKitNative compares it against wkj_abi_version() right after loading the library, so
 * that a stale jfxwebkit fails with one readable sentence instead of an obscure crash.
 */
#define WKJ_ABI_VERSION 1u

/* Returns the WKJ_ABI_VERSION the library was compiled against. */
WKJ_EXPORT uint32_t wkj_abi_version(void);

/* ------------------------------------------------------------------------------------- */
/* Opaque handles                                                                         */
/* ------------------------------------------------------------------------------------- */

/*
 * A Java object, as an id assigned by the Java-side registry; 0 is null. This replaces
 * the JNI object handle types JLObject and JGObject, and NewGlobalRef (contract 3).
 */
typedef uint64_t wkj_ref;

/*
 * A native object owned by the library and held by Java as a `long peer`; 0 is null. This
 * is the former `jlong peer` parameter of the DOM bindings, unchanged in meaning.
 */
typedef int64_t wkj_peer;

/*
 * Converting a wkj_peer to and from the native object it names. These are the direct
 * replacements for the jlong_to_ptr and ptr_to_jlong macros of wtf/java/JavaEnv.h and are
 * written the same way, as macros, so that they work unchanged in C and in C++ and in the
 * `#define IMPL (static_cast<X*>(wkj_to_ptr(peer)))` line at the top of every generated
 * DOM binding file.
 */
#define wkj_to_ptr(a)   ((void*)(uintptr_t)(a))
#define wkj_from_ptr(a) ((int64_t)(uintptr_t)(a))
/* ------------------------------------------------------------------------------------- */
/* String returns (contract 13)                                                           */
/* ------------------------------------------------------------------------------------- */

/*
 * The result of a string-returning function. The caller owns the buffer, so the library
 * allocates nothing and returns no pointer: there is no ownership question to get wrong
 * and nothing to leak or dangle.
 */
#define WKJ_STR_OK       0    /* *result_length code units were written into result_buf   */
#define WKJ_STR_NULL     1    /* the Java-visible value is null; *result_length = 0       */
#define WKJ_STR_OVERFLOW 2    /* nothing written; *result_length = capacity required      */

/* ------------------------------------------------------------------------------------- */
/* Pending exceptions (contract 2.2)                                                      */
/* ------------------------------------------------------------------------------------- */

/*
 * Exception kinds. These mirror the JavaExceptionType enum of the JNI implementation
 * (JavaDOMUtils.h: JavaDOMException, JavaEventException, JavaRangeException,
 * JavaUndefinedException) in the same order, shifted by one so that 0 can mean "nothing
 * pending". JavaExceptionType's absolute values were never observable: the enum is
 * declared in JavaDOMUtils.h and read nowhere in the tree.
 *
 * Only WKJ_EXC_DOM is ever raised today: every raise path in the tree goes through
 * raiseDOMErrorException, which built one org.w3c.dom.DOMException. The other three exist
 * because JavaExceptionType named them.
 */
#define WKJ_EXC_NONE      0   /* no exception pending                                     */
#define WKJ_EXC_DOM       1   /* was JavaDOMException      -> org.w3c.dom.DOMException    */
#define WKJ_EXC_EVENT     2   /* was JavaEventException                                   */
#define WKJ_EXC_RANGE     3   /* was JavaRangeException                                   */
#define WKJ_EXC_UNDEFINED 4   /* was JavaUndefinedException                               */

/* Capacity of the inline message buffer, in UTF-16 code units. */
#define WKJ_EXC_MESSAGE_MAX 256

/*
 * The pending-exception slot of one thread. It replaces the JNI pending exception that
 * ThrowNew set and ExceptionCheck tested.
 *
 * The slot is self-contained: the message lives inline, so the slot has no lifetime rule
 * and nothing has to be freed or copied out before the next call. A message longer than
 * WKJ_EXC_MESSAGE_MAX code units is truncated, and `message_length` is always the number
 * of code units actually present. Nothing truncates in practice - every message raised
 * today is a short canned literal from DOMException::description.
 *
 * Written by the library, read by Java. The library clears the slot on entry to every
 * wkj_* function; Java clears it after throwing, by storing WKJ_EXC_NONE into `type`.
 * Only `type` needs to be stored to clear it: the other fields are meaningless while
 * `type` is WKJ_EXC_NONE.
 */
typedef struct WKJExceptionSlot {
    int32_t  type;                            /* WKJ_EXC_NONE = 0 when nothing pending    */
    int32_t  code;                            /* DOMException legacy code                 */
    int32_t  message_length;                  /* UTF-16 code units present in `message`   */
    uint16_t message[WKJ_EXC_MESSAGE_MAX];    /* not NUL terminated; length is definitive */
} WKJExceptionSlot;

/*
 * Returns the calling thread's exception slot. The pointer is stable for the lifetime of
 * the thread, so Java can read `type` with a plain memory load - no downcall at all on the
 * (overwhelmingly common) no-exception path.
 *
 * Caution for the Java side: this returns the slot of the thread that calls it, which for
 * a virtual thread is its current carrier. Caching the pointer in a ThreadLocal is only
 * safe on platform threads.
 */
WKJ_EXPORT WKJExceptionSlot* wkj_exception_slot(void);

/* ------------------------------------------------------------------------------------- */
/* The host table (contract 4)                                                            */
/* ------------------------------------------------------------------------------------- */

/*
 * core: what the WTF and WebCore layers need in order to stop using the JNI environment
 * at all. These members replace wtf/java/JavaRef.h and wtf/java/JavaEnv.h.
 *
 * OWNERSHIP OF A wkj_ref
 *
 * A wkj_ref is a handle to a Java object, not the object itself. The ABI requires exactly
 * one rule, and the library obeys only this one:
 *
 *   **Every id obtained from retain or retain_weak is released exactly once, by whoever
 *   obtained it. An id that came from somewhere else is not released.**
 *
 * That leaves the registry free to implement retain either way, and both work:
 *
 *   - fresh id per call (the JNI model: NewGlobalRef returned a new reference and
 *     DeleteGlobalRef deleted that one), or
 *   - interning by object identity with a reference count, so that retain returns the same
 *     id and release decrements.
 *
 * Interning is the friendlier model and is what the Java side is expected to do, because it
 * makes id equality mean object equality. But it is a promise the Java registry makes, not
 * one this header can enforce, so the library never assumes it:
 *
 *   - The library does not treat two ids as naming different objects, nor two ids as naming
 *     the same one. Where it must know, it calls equals or hash_code.
 *   - The library does not assume it holds the only id for an object.
 *   - Interning without a reference count is a bug: one owner releasing would invalidate an
 *     id another owner still holds.
 *
 * For what it is worth, the question is currently unobservable in C++: a sweep of all 101
 * files that name a JLObject/JGObject-family type found comparisons of a handle with a
 * *handle* in none of them - every use of the comparison operators is a null test through
 * operator!. Nothing in the tree today can tell the two models apart.
 *
 * Every slot may be NULL; the library checks before calling and falls back to the default
 * documented on the slot.
 */
typedef struct WKJHostCore {
    /*
     * Mints a new strong id for the object `ref` names, or 0 for 0 and for an id whose
     * object is gone. A strong id keeps the object reachable. Replaces NewGlobalRef and
     * NewLocalRef, which become one operation once native code holds ids.
     * Default when NULL: return 0.
     */
    wkj_ref (*retain)(wkj_ref ref);

    /*
     * Mints a new weak id for the object `ref` names, or 0 as above. A weak id does not
     * keep the object reachable; is_live reports whether it is still there, and retain on
     * a weak id returns a strong id or 0 if the object has gone.
     *
     * This exists because Source/WebCore/bridge/jni/JobjectWrapper.cpp deliberately takes
     * NewWeakGlobalRef by default (useGlobalRef defaults to false) and its destructor
     * branches on GetObjectRefType. Modelling every id as strong would keep LiveConnect
     * peers alive that are collectable today, which is a behaviour change, not a cleanup.
     * Default when NULL: return 0.
     */
    wkj_ref (*retain_weak)(wkj_ref ref);

    /*
     * Drops the id `ref`, strong or weak; other ids for the same object are unaffected.
     * release(0) is a no-op. Replaces DeleteGlobalRef, DeleteWeakGlobalRef and
     * DeleteLocalRef. Default when NULL: no-op.
     */
    void (*release)(wkj_ref ref);

    /*
     * 1 if `ref` still names a live object, 0 for 0, for an unknown id and for a weak id
     * whose object has been collected. This is the replacement for testing a weak global
     * reference against NULL. Default when NULL: 0.
     */
    int32_t (*is_live)(wkj_ref ref);

    /*
     * Object.hashCode() and Object.equals() on the referents - not on the ids, which are
     * handles (see above). These are the host-table form of getJavaHashCode and
     * isJavaEquals in Source/WebCore/bindings/java/JavaDOMUtils.cpp:138-163.
     *
     * Provisioned rather than required: both C functions are declared and defined there
     * but called from nowhere in the tree today, so nothing is ported to use these yet.
     * They are here because identity questions become unavoidable once one object can have
     * many ids, and the LiveConnect phase (JobjectWrapper) is where that lands.
     * Defaults when NULL: hash_code returns 0; equals returns (a == b).
     */
    int32_t (*hash_code)(wkj_ref ref);
    int32_t (*equals)(wkj_ref a, wkj_ref b);

    /*
     * Reports whether the last upcall made on this thread ended in a Throwable, and clears
     * that state. Replaces WTF::CheckAndClearException(env), called at 269 sites, about a
     * dozen of which branch on the result while the rest only clear. Java has already
     * caught and logged the Throwable, because contract 4 forbids letting one escape an
     * upcall, so this is the ExceptionDescribe/ExceptionClear pair minus the describing.
     * Unrelated to WKJExceptionSlot, which carries exceptions in the other direction.
     * Returns 1 if an upcall failed, 0 otherwise. Default when NULL: 0.
     */
    int32_t (*check_and_clear_exception)(void);

    /*
     * NOTE: there are deliberately no perf-logger slots here.
     * JavaEnv.h declares PL_GetLogger, PL_ResumeCount, PL_SuspendCount and PL_IsEnabled
     * and the LOG_PERF_RECORD macro around com.sun.webkit.perf.PerfLogger, but the macro
     * had exactly one use in the tree, in Source/WebCore/platform/java/TextBreakIteratorJava.cpp.
     * That file was listed in no build list, so it was never compiled, and it has since been
     * deleted along with the twelve other dead files in that layer. The whole perf-logging path
     * is dead code; provisioning four callback slots for it would entrench it. If it is ever
     * re-enabled, the slots come back with an ABI bump.
     */
} WKJHostCore;

/*
 * PLACEHOLDERS. One sub-struct per callback group named in contract 4, to be filled in
 * later commits from the upcall audit of the matching client. The `reserved` member is
 * removed at that point; it exists only because an empty struct is not valid C.
 * WKJ_ABI_VERSION is bumped by the first such change that ships. Per contract 12.1 each
 * group is defined in the header that owns its area - webkit_java_api_page.h for webpage
 * and the six client tables, webkit_java_api_platform.h for graphics, network and media,
 * webkit_java_api_theme.h for theme and filesystem - so that two agents never edit one
 * file; the typedef here is what they replace.
 *
 * Group -> what it will carry:
 *   webpage      com.sun.webkit.WebPage notifications (WebPage.cpp)
 *   frameloader  FrameLoaderClientJava
 *   chrome       ChromeClientJava, PopupMenuJava, SearchPopupMenuJava, ColorChooserJava
 *   editor       EditorClientJava
 *   contextmenu  ContextMenuClientJava
 *   inspector    InspectorClientJava
 *   drag         DragClientJava
 *   graphics     WebCore/platform/graphics/java (WCRenderQueue, images, fonts, media sink)
 *   network      WebCore/platform/network/java (URLLoader, SocketStreamHandle)
 *   media        MediaPlayerPrivateJava
 *   filesystem   com.sun.webkit.FileSystem (wtf/java/FileSystemJava.cpp)
 *   wtf          WTF/wtf/java - com.sun.webkit.MainThread (webkit_java_api_wtf.h)
 *   pal          WebCore/PAL/pal - WCMessageDigest and systemBeep (webkit_java_api_pal.h)
 *   theme        WebCore/platform/java as a whole - RenderThemeJava and ScrollbarThemeJava
 *                first, but also the cursor, context menu, pasteboard, screen, shared
 *                timer, WCWidget, WCPluginWidget, LocalizedStrings and IDN clients, none
 *                of which has a group of its own
 */
typedef struct WKJHostWebPage     { void (*reserved)(void); } WKJHostWebPage;
typedef struct WKJHostFrameLoader { void (*reserved)(void); } WKJHostFrameLoader;
typedef struct WKJHostChrome      { void (*reserved)(void); } WKJHostChrome;
typedef struct WKJHostEditor      { void (*reserved)(void); } WKJHostEditor;
typedef struct WKJHostContextMenu { void (*reserved)(void); } WKJHostContextMenu;
typedef struct WKJHostInspector   { void (*reserved)(void); } WKJHostInspector;
typedef struct WKJHostDrag        { void (*reserved)(void); } WKJHostDrag;

/*
 * graphics, network, media, filesystem, theme, wtf and pal are no longer placeholders:
 * their slots are defined by the four area headers below, included here rather than by the
 * caller because they need wkj_ref and WKJ_EXPORT (declared above) and must be processed
 * before WKJHost. All four deliberately #error if they are included directly.
 */
#include "webkit_java_api_platform.h"
#include "webkit_java_api_theme.h"
#include "webkit_java_api_wtf.h"
#include "webkit_java_api_pal.h"

/*
 * The bindings/java slice, included here for the same reason as the three above: it needs
 * wkj_ref and WKJ_EXPORT, and it #errors if it is included directly. It differs in one way -
 * it defines no WKJHost member. Its callback table is installed by
 * wkj_install_event_listener_callbacks instead, because the shape of WKJHost is already fixed
 * and asserted from the Java side.
 */
#include "webkit_java_api_events.h"

/*
 * The process-wide callback table. Java allocates and fills it once, in one
 * Arena.ofShared() that lives as long as the process, and installs it with wkj_init.
 * `size` must be sizeof(WKJHost) as the caller sees it and must equal the host_size
 * argument of wkj_init. Both exist so that a mismatched pair of library and Java code is
 * rejected with a message instead of reading past the end of the table.
 */
typedef struct WKJHost {
    int32_t             size;
    WKJHostCore         core;
    WKJHostWebPage      webpage;
    WKJHostFrameLoader  frameloader;
    WKJHostChrome       chrome;
    WKJHostEditor       editor;
    WKJHostContextMenu  contextmenu;
    WKJHostInspector    inspector;
    WKJHostDrag         drag;
    WKJHostGraphics     graphics;
    WKJHostNetwork      network;
    WKJHostMedia        media;
    WKJHostFileSystem   filesystem;
    WKJHostTheme        theme;
    WKJHostWTF          wtf;
    WKJHostPAL          pal;
} WKJHost;

/* wkj_init result codes. */
#define WKJ_INIT_OK                   0
#define WKJ_INIT_ERR_NULL_HOST      (-1)
#define WKJ_INIT_ERR_ABI_VERSION    (-2)
#define WKJ_INIT_ERR_HOST_SIZE      (-3)
#define WKJ_INIT_ERR_ALREADY_INITED (-4)

/*
 * Installs the host table. Called exactly once per process, from the static initializer of
 * WebKitNative, before any other wkj_* call. `host` must outlive the library, which keeps
 * the pointer. `host_size` must equal host->size and sizeof(WKJHost) as the library sees
 * it; `abi_version` must equal WKJ_ABI_VERSION.
 *
 * Returns WKJ_INIT_OK or one of the negative codes above. It replaces JNI_OnLoad, which
 * cached the JavaVM, the com.sun.webkit.FileSystem class and every cached method id.
 */
WKJ_EXPORT int32_t wkj_init(const WKJHost* host, int32_t host_size, uint32_t abi_version);

/*
 * Library-internal: the installed table, NULL until wkj_init succeeds. It is declared in
 * this header rather than a private one because it is read on the hot path of WKJHandle
 * (retain and release) exactly the way `extern JavaVM* jvm` was read from JavaEnv.h. It is
 * not part of the Java-facing ABI and is not exported from the shared library.
 */
extern const WKJHost* wkj_host;

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_H */
