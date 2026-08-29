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
 * drt_java_api.h - the public C ABI of the DumpRenderTreeJava library.
 *
 * DumpRenderTreeJava is the WebKit layout-test harness for the JavaFX port. It is a
 * *separate* shared library from jfxwebkit, with its own entry points, its own
 * initialisation and its own ABI version, so nothing declared here is part of the jfxwebkit
 * ABI in Source/WebKitLegacy/java/api/webkit_java_api.h. What it does share with that
 * header is the vocabulary - WKJ_EXPORT, wkj_ref and the WKJ_STR_* string-return protocol -
 * so that one Java-side helper serves both libraries and the two can never drift apart.
 * That is the only reason this header includes it.
 *
 * The conventions are exactly those of modules/javafx.web/FFM-ABI-CONTRACT.md, sections 12
 * and 13; the entry points and the two callback tables are those of
 * modules/javafx.web/FFM-AUDIT-core.md, sections 5.4 and 8.9. Restating the rules a reader
 * of this file has to know:
 *
 * 1. TYPES
 *    Only <stdint.h> types, float, double and pointers cross the boundary. There is no
 *    boolean type: a Java boolean is int32_t (0 or 1), because FFM has no boolean layout.
 *    A native peer held by Java as a `long` is int64_t (contract 12), not a pointer type.
 *
 * 2. STRINGS (contract 12 and 13) - UTF-16 in both directions, never modified UTF-8.
 *
 *    Into the library, and into an upcall:  const uint16_t* s, int32_t s_len
 *        s == NULL                 -> the Java value was null
 *        s != NULL && s_len == 0   -> the Java value was the empty string
 *        The callee must not retain s; it is valid only for the duration of the call.
 *
 *    Out of the library, and out of an upcall: the caller provides the buffer, so there is
 *    no callee-owned string memory and no lifetime rule anywhere in this ABI.
 *
 *        int32_t f(..., uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
 *
 *        WKJ_STR_OK       *result_length code units were written into result_buf
 *        WKJ_STR_NULL     the value is null; *result_length = 0
 *        WKJ_STR_OVERFLOW nothing was written; *result_length is the capacity required,
 *                         so the caller grows once and calls again
 *
 * 3. JAVA OBJECTS (contract 3)
 *    Native code never holds a Java reference. A Java object is a wkj_ref: an opaque
 *    Java-assigned registry id, 0 meaning null. The one Java object this library holds is
 *    the com.sun.javafx.webkit.drt.EventSender that the harness attaches to a window
 *    object; it is kept alive for that wrapper object's lifetime with
 *    WKJDrtHostCore.retain and dropped with WKJDrtHostCore.release, which are what the
 *    NewGlobalRef/DeleteGlobalRef pair of the JNI implementation becomes.
 *
 * 4. EXCEPTIONS
 *    There is no exception slot in this ABI, and there was no exception path in the JNI
 *    implementation either: every upcall site cleared a pending Java exception and ignored
 *    it. Java catches and logs a Throwable inside each upcall target, so a failed upcall is
 *    invisible to C, exactly as it was.
 *
 *    The jfxwebkit rule that every wkj_* function clears the calling thread WKJExceptionSlot
 *    on entry (webkit_java_api.h, contract 2.2) therefore has no counterpart here and must
 *    not be added: DumpRenderTreeJava raises nothing into a slot, and clearing jfxwebkit's
 *    slot from this library would discard an exception that belongs to a wkj_* call the
 *    harness is in the middle of - the DRT entry points are called from Java between
 *    WebPage calls, on the same thread. No drt_* function reads or writes the slot.
 *
 * 5. UPCALLS (contract 4)
 *    One process-wide WKJDrtHost table of function pointers, installed once by drt_init.
 *    Every callback slot may be NULL: the library checks each pointer before calling it and
 *    behaves as if the call had done nothing, returning the default documented on the slot.
 *
 * 6. THREADING
 *    Every entry point and every callback here runs on the thread that called into the
 *    harness - the DumpRenderTree main thread for the downcalls and, for the callbacks,
 *    whichever thread is running JavaScript, which for this harness is the same one. No
 *    callback is invoked from a thread the JVM has not seen; the JNI implementation never
 *    called AttachCurrentThread. Java-side stubs may still be created in an Arena.ofShared,
 *    since nothing here promises confinement.
 */

#ifndef DRT_JAVA_API_H
#define DRT_JAVA_API_H

#include <stdint.h>

/* WKJ_EXPORT, wkj_ref and WKJ_STR_OK / WKJ_STR_NULL / WKJ_STR_OVERFLOW. */
#include <webkit_java_api.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------------------- */
/* ABI version                                                                            */
/* ------------------------------------------------------------------------------------- */

/*
 * Bump this whenever anything in this header changes shape: a struct gains, loses or
 * reorders a member, a function changes signature, or a documented meaning changes. It is
 * independent of WKJ_ABI_VERSION, because DumpRenderTreeJava and jfxwebkit are separate
 * libraries that are versioned and rebuilt separately.
 */
#define DRT_ABI_VERSION 1u

/* Returns the DRT_ABI_VERSION the library was compiled against. */
WKJ_EXPORT uint32_t drt_abi_version(void);

/* ------------------------------------------------------------------------------------- */
/* Callback tables (audit 8.9)                                                            */
/* ------------------------------------------------------------------------------------- */

/*
 * Lifetime of a wkj_ref. This is the DumpRenderTreeJava counterpart of WKJHostCore in
 * webkit_java_api.h, cut down to the two operations this library actually performs. Its
 * ownership rule is the same one: every id obtained from retain is released exactly once,
 * by whoever obtained it; an id that came from somewhere else is not released.
 *
 * The audit's table in section 8.9 has no lifetime slots. They are added here because the
 * JNI implementation had them: EventSender.cpp stored a JGObject - a NewGlobalRef - in the
 * private data of the JavaScript wrapper and deleted it in the wrapper's finalizer. Without
 * a counterpart the Java registry entry for every EventSender would leak, which the JNI
 * code did not do.
 */
typedef struct WKJDrtHostCore {
    /*
     * Mints a new strong id for the object `ref` names, or 0 for 0 and for an id whose
     * object is gone. Replaces NewGlobalRef. Default when NULL: the library stores the id
     * exactly as it arrived and never releases it, so a host that installs no retain must
     * keep the EventSender reachable on the Java side for as long as the test runs.
     */
    wkj_ref (*retain)(wkj_ref ref);

    /*
     * Drops the id `ref`; other ids for the same object are unaffected, and release(0) is a
     * no-op. Replaces DeleteGlobalRef. Default when NULL: no-op.
     */
    void (*release)(wkj_ref ref);
} WKJDrtHostCore;

/*
 * The eight static methods of com.sun.javafx.webkit.drt.DumpRenderTree that the harness
 * calls. They replace the eight cached static jmethodIDs and the one global class reference
 * of the deleted Tools/DumpRenderTree/java/JavaEnv.cpp. Being static Java methods, none of
 * them takes a wkj_ref.
 *
 * Called on the thread running the layout test (see 6 above).
 */
typedef struct WKJDrtCallbacks {
    /*
     * DumpRenderTree.waitUntilDone(). Reached from TestRunner::setWaitToDump(true).
     * Default when NULL: no-op.
     */
    void (*wait_until_done)(void);

    /*
     * DumpRenderTree.notifyDone(). Reached from TestRunner::notifyDone() and
     * TestRunner::forceImmediateCompletion(). Default when NULL: no-op.
     */
    void (*notify_done)(void);

    /*
     * DumpRenderTree.overridePreference(String, String). Both strings are non-NULL here:
     * they come from JavaScript strings the test supplied. Default when NULL: no-op.
     */
    void (*override_preference)(const uint16_t* key, int32_t key_len,
                                const uint16_t* value, int32_t value_len);

    /* DumpRenderTree.getBackForwardItemCount(). Default when NULL: 0. */
    int32_t (*get_back_forward_item_count)(void);

    /* DumpRenderTree.clearBackForwardList(). Default when NULL: no-op. */
    void (*clear_back_forward_list)(void);

    /*
     * DumpRenderTree.resolveURL(String) - resolves a test-relative URL against the
     * directory of the running test and returns an absolute file: URL. Follows the WKJ_STR_*
     * protocol of 2 above. The Java implementation never returns null, so WKJ_STR_NULL is
     * reserved for a future one; the library treats it as the empty string.
     * Default when NULL: WKJ_STR_NULL with *result_length = 0.
     */
    int32_t (*resolve_url)(const uint16_t* relative, int32_t relative_len,
                           uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

    /*
     * DumpRenderTree.loadURL(String). Invoked while the work queue is drained.
     * Default when NULL: no-op.
     */
    void (*load_url)(const uint16_t* url, int32_t url_len);

    /* DumpRenderTree.goBackForward(int). Default when NULL: no-op. */
    void (*go_back_forward)(int32_t how_far);
} WKJDrtCallbacks;

/*
 * The twenty-two methods of com.sun.javafx.webkit.drt.EventSender that the JavaScript
 * `eventSender` object forwards to. This table is what replaces EventSender.cpp's single
 * `static void call(JSObjectRef, <cached method id>, ...)` varargs dispatcher, which invoked
 * CallVoidMethodV with an untyped va_list: FFM has no varargs upcall, and typed slots are
 * checkable where a va_list never was.
 *
 * `user` is the wkj_ref of the EventSender instance, handed to the library by
 * drt_did_clear_window_object and stored in the private data of the JavaScript object.
 *
 * Called on the thread running the layout test (see 6 above).
 */
typedef struct WKJEventSenderCallbacks {
    /* keyDown(String, int). `key` is never NULL. Default when NULL: no-op. */
    void (*key_down)(wkj_ref user, const uint16_t* key, int32_t key_len, int32_t modifiers);

    /*
     * mouseUpDown(int, int). `button` is a com.sun.webkit.event.WCMouseEvent button
     * constant and `modifiers` an EventSender modifier mask, both computed in C exactly as
     * before. Default when NULL: no-op.
     */
    void (*mouse_up_down)(wkj_ref user, int32_t button, int32_t modifiers);

    /* mouseMoveTo(int, int). Default when NULL: no-op. */
    void (*mouse_move_to)(wkj_ref user, int32_t x, int32_t y);

    /* mouseScroll(float, float, boolean). Default when NULL: no-op. */
    void (*mouse_scroll)(wkj_ref user, float dx, float dy, int32_t continuous);

    /* leapForward(int). Default when NULL: no-op. */
    void (*leap_forward)(wkj_ref user, int32_t time_offset);

    /* contextClick(). Default when NULL: no-op. */
    void (*context_click)(wkj_ref user);

    /* scheduleAsynchronousClick(). Default when NULL: no-op. */
    void (*schedule_asynchronous_click)(wkj_ref user);

    /* touchStart(). Default when NULL: no-op. */
    void (*touch_start)(wkj_ref user);

    /* touchCancel(). Default when NULL: no-op. */
    void (*touch_cancel)(wkj_ref user);

    /* touchMove(). Default when NULL: no-op. */
    void (*touch_move)(wkj_ref user);

    /* touchEnd(). Default when NULL: no-op. */
    void (*touch_end)(wkj_ref user);

    /* addTouchPoint(int, int). Default when NULL: no-op. */
    void (*add_touch_point)(wkj_ref user, int32_t x, int32_t y);

    /* updateTouchPoint(int, int, int). Default when NULL: no-op. */
    void (*update_touch_point)(wkj_ref user, int32_t index, int32_t x, int32_t y);

    /* cancelTouchPoint(int). Default when NULL: no-op. */
    void (*cancel_touch_point)(wkj_ref user, int32_t index);

    /* releaseTouchPoint(int). Default when NULL: no-op. */
    void (*release_touch_point)(wkj_ref user, int32_t index);

    /* clearTouchPoints(). Default when NULL: no-op. */
    void (*clear_touch_points)(wkj_ref user);

    /* setTouchModifier(int, boolean). Default when NULL: no-op. */
    void (*set_touch_modifier)(wkj_ref user, int32_t modifier, int32_t set);

    /* scalePageBy(float, int, int). Default when NULL: no-op. */
    void (*scale_page_by)(wkj_ref user, float scale, int32_t x, int32_t y);

    /* zoom(boolean, boolean). Default when NULL: no-op. */
    void (*zoom)(wkj_ref user, int32_t in, int32_t text_only);

    /*
     * beginDragWithFiles(String[]). The array is passed flat, as `count` pointers and
     * `count` lengths: `files[i]` with `file_lengths[i]` is the i-th element, and a NULL
     * `files[i]` is a null element. `count` may be 0, in which case `files` and
     * `file_lengths` may be NULL. Default when NULL: no-op.
     */
    void (*begin_drag_with_files)(wkj_ref user, const uint16_t* const* files,
                                  const int32_t* file_lengths, int32_t count);

    /*
     * getDragMode() - the read half of the JavaScript `eventSender.dragMode` property.
     * Returns 0 or 1. Default when NULL: 0.
     */
    int32_t (*get_drag_mode)(wkj_ref user);

    /*
     * setDragMode(boolean) - the write half of `eventSender.dragMode`.
     * Default when NULL: no-op.
     */
    void (*set_drag_mode)(wkj_ref user, int32_t mode);
} WKJEventSenderCallbacks;

/*
 * The process-wide callback table. Java allocates and fills it once, in one
 * Arena.ofShared() that lives as long as the process, and installs it with drt_init.
 * `size` must be sizeof(WKJDrtHost) as the caller sees it and must equal the `host_size`
 * argument of drt_init; both exist so that a mismatched pair of library and Java code is
 * rejected with a message instead of reading past the end of the table.
 */
typedef struct WKJDrtHost {
    int32_t                 size;
    WKJDrtHostCore          core;
    WKJDrtCallbacks         drt;
    WKJEventSenderCallbacks event_sender;
} WKJDrtHost;

/* drt_init result codes. */
#define DRT_INIT_OK                   0
#define DRT_INIT_ERR_NULL_HOST      (-1)
#define DRT_INIT_ERR_ABI_VERSION    (-2)
#define DRT_INIT_ERR_HOST_SIZE      (-3)
#define DRT_INIT_ERR_ALREADY_INITED (-4)

/*
 * Installs the host table. Called exactly once per process, from the static initializer of
 * the Java facade, before any other drt_* call. `host` must outlive the library, which
 * keeps the pointer. `host_size` must equal host->size and sizeof(WKJDrtHost) as the
 * library sees it; `abi_version` must equal DRT_ABI_VERSION.
 *
 * Returns DRT_INIT_OK or one of the negative codes above. It replaces this library's own
 * JNI_OnLoad, which cached the JVM pointer, took a global reference to the DumpRenderTree class
 * and cached eight static method ids.
 */
WKJ_EXPORT int32_t drt_init(const WKJDrtHost* host, int32_t host_size, uint32_t abi_version);

/*
 * Library-internal: the installed table, NULL until drt_init succeeds. Declared here rather
 * than in a private header because it is read directly at every callback site, the way
 * the `jvm` global was read from the deleted JavaEnv.h. It is not part of the
 * Java-facing ABI and is not exported from the shared library.
 */
extern const WKJDrtHost* drt_host;

/* ------------------------------------------------------------------------------------- */
/* Entry points (audit 5.4)                                                               */
/* ------------------------------------------------------------------------------------- */

/*
 * DumpRenderTree.initDRT(). Opens up the WTF and JSC configuration for testing:
 * WTF::setPermissionsOfConfigPage, WTF::Config::disableFreezingForTesting and
 * JSC::Config::enableRestrictedOptions. Called once, before the first test.
 *
 * The name is the mechanical transliteration of the Java method name under the `drt_`
 * prefix (initDRT -> init_drt), which is why it reads redundantly; `drt_init` is taken by
 * the host-table installer above and means something quite different.
 */
WKJ_EXPORT void drt_init_drt(void);

/*
 * DumpRenderTree.initTest(String, String). Creates the TestRunner and the GCController for
 * one test and clears the work queue. Both strings are non-NULL.
 *
 * Note for the Java side: the JNI implementation read these two with GetStringUTFChars,
 * i.e. modified UTF-8, and this ABI is UTF-16 (contract 2.1). The two agree on every test
 * path expressible in ASCII, and on all of the BMP once decoded; they disagree on U+0000
 * and on supplementary characters, which modified UTF-8 encodes as a six-byte surrogate
 * pair. This is one of the two places in the harness where that conversion is observable.
 */
WKJ_EXPORT void drt_init_test(const uint16_t* test_path, int32_t test_path_len,
                              const uint16_t* pixels_hash, int32_t pixels_hash_len);

/*
 * DumpRenderTree.didClearWindowObject(long, long, EventSender). Installs the TestRunner,
 * the `eventSender` object, the internals object and the GCController on a freshly cleared
 * window object.
 *
 * `js_context` is a JSGlobalContextRef and `js_window_object` a JSObjectRef, both already
 * held by Java as `long` (contract 12). `event_sender` is the registry id of the Java
 * EventSender; the library retains it for as long as the JavaScript wrapper lives and
 * releases it when that wrapper is finalized.
 *
 * Does nothing at all if no test is running, which is what the JNI implementation did.
 */
WKJ_EXPORT void drt_did_clear_window_object(int64_t js_context, int64_t js_window_object,
                                            wkj_ref event_sender);

/*
 * DumpRenderTree.dispose(). Tears down the TestRunner and the GCController and waits for
 * the JSC VM to be destroyed.
 */
WKJ_EXPORT void drt_dispose(void);

/* DumpRenderTree.dumpAsText(). Returns 0 or 1. */
WKJ_EXPORT int32_t drt_dump_as_text(void);

/* DumpRenderTree.dumpChildFramesAsText(). Returns 0 or 1. */
WKJ_EXPORT int32_t drt_dump_child_frames_as_text(void);

/*
 * DumpRenderTree.didFinishLoad(). Drains one item from the work queue and reports whether
 * it started a load. This one *upcalls*: the queued item invokes load_url or
 * go_back_forward. Returns 0 or 1.
 */
WKJ_EXPORT int32_t drt_did_finish_load(void);

/* DumpRenderTree.dumpBackForwardList(). Returns 0 or 1. */
WKJ_EXPORT int32_t drt_dump_back_forward_list(void);

/* DumpRenderTree.shouldStayOnPageAfterHandlingBeforeUnload(). Returns 0 or 1. */
WKJ_EXPORT int32_t drt_should_stay_on_page_after_handling_before_unload(void);

/*
 * DumpRenderTree.openPanelFiles(), first half: the number of files the running test set
 * with testRunner.setOpenPanelFiles. Java calls this, allocates a String[] of that length
 * and fills it with drt_open_panel_file. The count cannot change between the two calls:
 * only JavaScript running in the test changes it, and it is not running while Java is
 * reading.
 */
WKJ_EXPORT int32_t drt_open_panel_file_count(void);

/*
 * DumpRenderTree.openPanelFiles(), second half: file `index` of that list, following the
 * WKJ_STR_* protocol of 2 above. An out-of-range index yields WKJ_STR_NULL.
 *
 * Note for the Java side: the JNI implementation produced these strings with NewStringUTF,
 * i.e. modified UTF-8, over bytes that TestRunner::setOpenPanelFiles had produced with
 * JSStringGetUTF8CString, i.e. standard UTF-8. The two encodings differ for supplementary
 * characters, so a file name outside the BMP was decoded incorrectly before and is decoded
 * correctly now.
 */
WKJ_EXPORT int32_t drt_open_panel_file(int32_t index, uint16_t* result_buf,
                                       int32_t result_cap, int32_t* result_length);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* DRT_JAVA_API_H */
