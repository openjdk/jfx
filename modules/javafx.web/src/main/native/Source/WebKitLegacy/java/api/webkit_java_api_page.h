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
 * webkit_java_api_page.h - the WebKitLegacy core (non-DOM) half of the flat C ABI.
 *
 * It covers what used to be reached through these JNI entry points:
 *
 *   Java_com_sun_webkit_WebPage_twk*            WebCoreSupport/WebPage.cpp
 *   Java_com_sun_webkit_BackForwardList_bfl*    WebCoreSupport/BackForwardList.cpp
 *   Java_com_sun_webkit_PageCache_twk*          WebCoreSupport/PageCacheJava.cpp
 *   Java_com_sun_webkit_ColorChooser_twk*       WebCoreSupport/ColorChooserJava.cpp
 *
 * and the callbacks that used to be cached jmethodIDs in the client classes.
 *
 * CONVENTIONS (FFM-ABI-CONTRACT.md sections 2, 12 and 13; nothing here deviates)
 *
 *   handles      A WebCore object Java holds as a long is int64_t, 0 for null. There are
 *                three: a page (WebCore::WebPage*), a frame (WebCore::Frame*) and a
 *                history item (WebCore::HistoryItem*). C++ recovers the object with
 *                static_cast<T*>(wkj_to_ptr(h)), exactly as the DOM bindings do.
 *   Java objects wkj_ref, a registry id; 0 is null. Native code never holds a Java
 *                reference.
 *   booleans     int32_t, 0 or 1. FFM has no boolean layout.
 *   strings in   const uint16_t* s, int32_t s_len. UTF-16, never modified UTF-8 (the one
 *                exception is wkj_frame_load; read its comment). s == NULL means the Java
 *                value was null. Note that WebCore has always seen a null argument as the
 *                empty string - WKJString reproduces that collapse (contract 11.1) - so
 *                "NULL means null" describes the wire, not what WebCore ends up with.
 *   strings out  int32_t wkj_x(..., uint16_t* result_buf, int32_t result_cap,
 *                int32_t* result_length), returning WKJ_STR_OK, WKJ_STR_NULL or
 *                WKJ_STR_OVERFLOW. The caller owns the buffer, so nothing dangles.
 *   arrays out   int32_t wkj_x(..., T* out, int32_t out_cap) returning the element count.
 *                Calling with out == NULL and out_cap == 0 returns the count without
 *                writing anything, which is how the caller sizes its buffer.
 *   errors       Nothing in this half of the ABI reports an error: the JNI code it
 *                replaces never called ThrowNew, so the exception slot is never written
 *                here and the Java facade has nothing to check.
 *   critical     Linker.Option.critical(true) is forbidden on every function here. Most
 *                of them can re-enter the JVM through a callback, and several block on
 *                modal UI.
 *
 * THREAD
 *
 *   Every function here, and every callback slot here, runs on the JavaFX application
 *   thread, which is also the WebKit main thread: WTF::initializeMainThread() is called
 *   from page creation, which the WebPage constructor asserts happens on the FX thread,
 *   and the printing entry points are the only ones reachable from elsewhere - they
 *   marshal on the Java side before calling. There is no thread hand-off in this half of
 *   the ABI, and no callback here may block on another thread.
 *
 * CALLBACK TABLES
 *
 *   Every slot takes the page's wkj_ref as its first argument and every slot may be NULL:
 *   the library tests each pointer before calling it and behaves as though the call had
 *   done nothing, returning the default written on the slot. A callback must not let a
 *   Throwable escape - WebKit swallows upcall failures today (80 CheckAndClearException
 *   calls in this slice) and the Java side has to keep swallowing them, or a migration
 *   commit changes behaviour.
 *
 *   The tables are installed per page by wkj_page_set_callbacks, but only the wkj_ref is
 *   per page: the WKJPageCallbacks instance itself should be a single process-wide table
 *   built once in one Arena.ofShared(), and the same pointer passed for every page. That
 *   is contract section 4's one process-wide host table and audit section 8's per-client
 *   grouping at the same time. The library keeps the pointer for the life of the page, so
 *   the arena must outlive wkj_page_destroy.
 *
 * WHAT IS DELIBERATELY NOT HERE
 *
 *   Three entry points are absent because the native-necessity triage says they should be
 *   deleted rather than given a facade (FFM-AUDIT-core.md sections 9.2 and 5.1):
 *
 *     twkGetIconURL           PURE, PARITY exact: ENABLE(ICONDATABASE) is never defined
 *     bflItemGetIcon          for this port, so both are "return null" for every input.
 *                             They become Java, in their own commit.
 *     twkDoJSCGarbageCollection
 *                             WRAPPER: its whole body is a call to the already exported
 *                             plain-C WebPage_doJSCGarbageCollection(), which Java binds
 *                             directly with FunctionDescriptor.ofVoid().
 *
 *   Those three keep their JNI form in WebPage.cpp and BackForwardList.cpp, and they are
 *   the only reason those two files still include jni.h. Deleting them is behaviour
 *   affecting - WebPage.getIcon() and BackForwardList.Entry.getIcon() become an
 *   unconditional null - so it needs the Java side and a commit of its own.
 *
 *   Two BackForwardList entry points are absent for a different reason: bflGet and
 *   bflItemGetChildren construct com.sun.webkit.BackForwardList$Entry objects and cache
 *   them in HistoryItem::m_hostObject, which Source/WebCore/history/HistoryItem.cpp:78
 *   reads back to fire notifyItemDestroyed. That field is a JGObject in an upstream
 *   WebKit header outside every java/ directory, so entry lifetime cannot move to a
 *   wkj_ref without touching a file this migration keeps its hands off.
 *
 *   Finally, the WKJHostWebPage, WKJHostFrameLoader, WKJHostChrome, WKJHostEditor,
 *   WKJHostContextMenu, WKJHostInspector and WKJHostDrag placeholders in
 *   webkit_java_api.h are superseded by the tables below and should be removed by the
 *   owner of that header. They are not filled in here: contract 12.1 assumed one
 *   process-wide table for everything, while these tables are addressed per page, and
 *   redefining a struct tag in two headers does not compile. WKJHostContextMenu in
 *   particular has nothing to hold - ContextMenuClientJava makes no upcalls at all.
 */

#ifndef WEBKIT_JAVA_API_PAGE_H
#define WEBKIT_JAVA_API_PAGE_H

#include "webkit_java_api.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------------------- */
/* Constants                                                                              */
/* ------------------------------------------------------------------------------------- */

/*
 * The integer codes that travel through this ABI - the `id` and `direction` of a focus
 * event, the `id` and `button` of a mouse event, the WCKeyEvent virtual key codes, the
 * LoadListenerClient load states and error codes, and the WebPage DND_* actions - are not
 * redeclared here.
 *
 * They live in wkj_constants.h, generated from the Java sources that define them, under
 * their original com_sun_webkit_* spelling. That is the single definition for the C++
 * side, and the Java classes themselves are the single definition for the Java side, so
 * declaring a third set of names in this header would only create something to drift.
 * Each parameter below names the Java class whose constants it takes.
 */
#include "wkj_constants.h"

/* ------------------------------------------------------------------------------------- */
/* Callback tables                                                                        */
/* ------------------------------------------------------------------------------------- */

/*
 * WKJChromeCallbacks - replaces the 22 cached method ids and 4 cached field ids of
 * ChromeClientJava (ChromeClientJava.cpp:60-204).
 *
 * The four WCRectangle field reads collapse into the out parameters of get_window_bounds
 * and get_page_bounds, and the two WCPoint constructions plus four getX/getY calls of
 * screenToRootView and rootViewToScreen collapse into screen_to_window and
 * window_to_screen. That is 22 upcalls and 10 JNI field or accessor calls turned into 23
 * slots.
 *
 * alert, confirm, prompt, run_before_unload and choose_file open modal UI and block. The
 * library calls them from inside a downcall, so the Java side must be prepared to run a
 * nested event loop exactly where it does today.
 */
typedef struct WKJChromeCallbacks {
    /*
     * The Java WCWidget the page is hosted in, as a registry id, for
     * ChromeClient::platformPageClient(). Default when NULL: 0.
     *
     * PlatformPageClient is a WKJHandle typedef now (Source/WebCore/platform/Widget.h), so
     * this is what WidgetJava.cpp and PlatformScreenJava.cpp end up holding.
     */
    wkj_ref (*get_host_window)(wkj_ref page);

    /*
     * The window and page rectangles, in float device-independent units. Return 1 and fill
     * *out when Java produced a WCRectangle, 0 when it returned null - the caller then
     * uses an empty rect, which is what the JNI code did. Default when NULL: 0.
     */
    int32_t (*get_window_bounds)(wkj_ref page, float* out_x, float* out_y,
                                 float* out_width, float* out_height);
    void    (*set_window_bounds)(wkj_ref page, int32_t x, int32_t y,
                                 int32_t width, int32_t height);
    int32_t (*get_page_bounds)(wkj_ref page, float* out_x, float* out_y,
                               float* out_width, float* out_height);

    /* Coordinate conversion. Default when NULL: leave *out_x and *out_y unchanged. */
    void (*screen_to_window)(wkj_ref page, float x, float y, float* out_x, float* out_y);
    void (*window_to_screen)(wkj_ref page, float x, float y, float* out_x, float* out_y);

    /* Focus and pointer. */
    void (*set_focus)(wkj_ref page, int32_t focused);
    void (*transfer_focus)(wkj_ref page, int32_t forward);

    /*
     * Cursor::platformCursor(), which is a jlong for this port
     * (Source/WebCore/platform/Cursor.h:79) and is passed through unchanged.
     */
    void (*set_cursor)(wkj_ref page, int64_t platform_cursor);

    /*
     * The tooltip. text == NULL means "no tooltip": ChromeClientJava::setToolTip passes a
     * null jstring for an empty tooltip today and that distinction is what clears it.
     */
    void (*set_tooltip)(wkj_ref page, const uint16_t* text, int32_t text_length);

    void (*set_scrollbars_visible)(wkj_ref page, int32_t visible);
    void (*set_statusbar_text)(wkj_ref page, const uint16_t* text, int32_t text_length);

    /*
     * window.open(). Returns the new page handle - the int64_t the Java WebPage holds as
     * pPage - or 0 if Java declined to create a window. Returning the handle rather than a
     * registry id is what removes WebPage::pageFromJObject and the WebPage.getPage upcall
     * with it; the caller needs the WebCore::Page, not the Java object.
     * Default when NULL: 0.
     */
    int64_t (*create_window)(wkj_ref page, int32_t menu_bar, int32_t status_bar,
                             int32_t tool_bar, int32_t resizable);
    void    (*show_window)(wkj_ref page);
    void    (*close_window)(wkj_ref page);

    /* JavaScript dialogs. confirm and run_before_unload default to 0 when NULL. */
    void    (*alert)(wkj_ref page, const uint16_t* text, int32_t text_length);
    int32_t (*confirm)(wkj_ref page, const uint16_t* text, int32_t text_length);

    /*
     * window.prompt(). Follows the string-out protocol of contract 13 in reverse: Java
     * writes into the caller's buffer and returns WKJ_STR_OK, WKJ_STR_NULL (the user
     * cancelled) or WKJ_STR_OVERFLOW with the required capacity in *result_length.
     *
     * Because this opens a modal dialog, a WKJ_STR_OVERFLOW retry must be served from the
     * result the callee already has. It must not run the dialog a second time.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*prompt)(wkj_ref page, const uint16_t* text, int32_t text_length,
                      const uint16_t* default_value, int32_t default_value_length,
                      uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

    int32_t (*can_run_before_unload)(wkj_ref page);
    int32_t (*run_before_unload)(wkj_ref page, const uint16_t* message,
                                 int32_t message_length);

    void (*add_message_to_console)(wkj_ref page, const uint16_t* message,
                                   int32_t message_length, int32_t line_number,
                                   const uint16_t* source_id, int32_t source_id_length);

    /* window.print(). */
    void (*print)(wkj_ref page);

    /*
     * The file chooser. Returns the number of files chosen, 0 for none and -1 when Java
     * returned null (which the JNI code distinguished from an empty array by skipping
     * FileChooser::chooseFiles entirely).
     *
     * The paths are written end to end into result_buf and their lengths into
     * result_lengths. On WKJ_STR_OVERFLOW-shaped failure - more files than
     * result_lengths_cap, or more code units than result_cap - nothing is written,
     * *required_units receives the total number of code units needed and the return value
     * is still the file count, so the caller can size both buffers and call again.
     *
     * As for prompt, this opens a modal dialog and the retry must be served from the
     * result the callee already has.
     * Default when NULL: -1.
     */
    int32_t (*choose_file)(wkj_ref page,
                           const uint16_t* initial_file_name, int32_t initial_file_name_length,
                           int32_t allow_multiple,
                           const uint16_t* mime_filters, int32_t mime_filters_length,
                           uint16_t* result_buf, int32_t result_cap,
                           int32_t* result_lengths, int32_t result_lengths_cap,
                           int32_t* required_units);
} WKJChromeCallbacks;

/*
 * WKJFrameLoaderCallbacks - replaces the 12 live cached method ids of
 * FrameLoaderClientJava (FrameLoaderClientJava.cpp:63-144).
 *
 * There is no permit_enable_scripts slot: permitEnableScriptsActionMID was cached and
 * never called, and WebPage.fwkPermitEnableScriptsAction has no caller. Both were removed
 * in the deletion commit that preceded this ABI.
 *
 * The url and content_type arguments are exactly the strings the JNI code passed; a NULL
 * pointer is the null jstring it passed for an absent value.
 */
typedef struct WKJFrameLoaderCallbacks {
    void (*frame_created)(wkj_ref page, int64_t frame);
    void (*frame_destroyed)(wkj_ref page, int64_t frame);

    void (*set_request_url)(wkj_ref page, int64_t frame, int32_t id,
                            const uint16_t* url, int32_t url_length);
    void (*remove_request_url)(wkj_ref page, int64_t frame, int32_t id);

    /* state and error_code are com.sun.webkit.LoadListenerClient constants. */
    void (*fire_load_event)(wkj_ref page, int64_t frame, int32_t state,
                            const uint16_t* url, int32_t url_length,
                            const uint16_t* content_type, int32_t content_type_length,
                            double progress, int32_t error_code);
    void (*fire_resource_load_event)(wkj_ref page, int64_t frame, int32_t state, int32_t id,
                                     const uint16_t* content_type, int32_t content_type_length,
                                     double progress, int32_t error_code);

    /* The four policy decisions. Default when NULL: 0, i.e. "not permitted". */
    int32_t (*permit_navigate)(wkj_ref page, int64_t frame,
                               const uint16_t* url, int32_t url_length);
    int32_t (*permit_redirect)(wkj_ref page, int64_t frame,
                               const uint16_t* url, int32_t url_length);
    int32_t (*permit_accept_resource)(wkj_ref page, int64_t frame,
                                      const uint16_t* url, int32_t url_length);
    int32_t (*permit_new_window)(wkj_ref page, int64_t frame,
                                 const uint16_t* url, int32_t url_length);
    int32_t (*permit_submit_data)(wkj_ref page, int64_t frame,
                                  const uint16_t* url, int32_t url_length,
                                  const uint16_t* http_method, int32_t http_method_length,
                                  int32_t is_submit);

    /*
     * The two pointers are a JSGlobalContextRef and a JSObjectRef for the window object.
     * They were passed as jlongs and are passed as pointers now; Java hands them straight
     * back to the JavaScriptCore C API without dereferencing them.
     */
    void (*did_clear_window_object)(wkj_ref page, int64_t frame,
                                    void* js_context, void* js_window_object);
} WKJFrameLoaderCallbacks;

/*
 * WKJNetworkCallbacks - com.sun.webkit.network.NetworkContext.canHandleURL, the one
 * static Java method in this slice. It belongs to no page, so it is installed once for
 * the process rather than per page. Default when NULL: 0.
 */
typedef struct WKJNetworkCallbacks {
    int32_t (*can_handle_url)(const uint16_t* url, int32_t url_length);
} WKJNetworkCallbacks;

/*
 * WKJEditorCallbacks - replaces the single cached method id of EditorClientJava
 * (EditorClientJava.cpp:613).
 *
 * The Java target is WebPage.setInputMethodState(boolean), which is public and has no fwk
 * prefix. Do not rename it: InputMethodClientImpl and the FX text input path use it.
 */
typedef struct WKJEditorCallbacks {
    void (*set_input_method_state)(wkj_ref page, int32_t enabled);
} WKJEditorCallbacks;

/*
 * WKJInspectorCallbacks - replaces the two cached method ids of InspectorClientJava.
 *
 * send_message_to_frontend returns void although WebPage.fwkSendInspectorMessageToFrontend
 * returns boolean: InspectorClientJava.cpp discards the result. Preserving that is
 * behaviour neutral; plumbing it through would not be.
 */
typedef struct WKJInspectorCallbacks {
    void (*repaint_all)(wkj_ref page);
    void (*send_message_to_frontend)(wkj_ref page, const uint16_t* message,
                                     int32_t message_length);
} WKJInspectorCallbacks;

/*
 * WKJProgressCallbacks - replaces the single cached method id of
 * ProgressTrackerClientJava.
 *
 * fire_load_event is the same Java method WKJFrameLoaderCallbacks::fire_load_event names,
 * and both C++ files cached the same jmethodID. Keeping two slots keeps the two clients
 * independent; the Java side should install the same upcall stub in both.
 */
typedef struct WKJProgressCallbacks {
    void (*fire_load_event)(wkj_ref page, int64_t frame, int32_t state,
                            const uint16_t* url, int32_t url_length,
                            const uint16_t* content_type, int32_t content_type_length,
                            double progress, int32_t error_code);
} WKJProgressCallbacks;

/*
 * WKJPageNotifyCallbacks - the two live upcalls WebPage.cpp makes on its own behalf.
 *
 * There is no get_page slot. WebPage::webPageFromJObject existed only to turn the Java
 * WebPage back into a WebPage*, and the page handle that wkj_page_set_callbacks already
 * carries makes that unnecessary.
 */
typedef struct WKJPageNotifyCallbacks {
    void (*repaint)(wkj_ref page, int32_t x, int32_t y, int32_t width, int32_t height);
    void (*scroll)(wkj_ref page, int32_t x, int32_t y, int32_t width, int32_t height,
                   int32_t delta_x, int32_t delta_y);
} WKJPageNotifyCallbacks;

/*
 * WKJBackForwardCallbacks - the three live upcalls of BackForwardList.cpp.
 *
 * There is no item_changed slot: historyItemChangedImpl had no live caller and was
 * removed in the deletion commit that opened this migration.
 *
 * list_changed takes the id wkj_bfl_set_host was given. create_entry and item_destroyed
 * are about one history entry: create_entry builds the
 * com.sun.webkit.BackForwardList$Entry that mirrors a HistoryItem, and the library parks
 * the id it returns in HistoryItem::m_hostObject for the life of the item, so the entry
 * is created once and handed back on every later lookup. item_destroyed is called from
 * the HistoryItem destructor with that same id, which is the last use of it.
 */
typedef struct WKJBackForwardCallbacks {
    void (*list_changed)(wkj_ref back_forward_list);

    /* -> BackForwardList.Entry(long item, long page). Returns the new id, or 0. */
    wkj_ref (*create_entry)(int64_t item, int64_t page);

    /* -> BackForwardList.Entry.notifyItemDestroyed(). */
    void (*item_destroyed)(wkj_ref entry);
} WKJBackForwardCallbacks;

/*
 * WKJColorChooserCallbacks - the three upcalls of ColorChooserJava, installed once for
 * the process because two of the three are made on the com.sun.webkit.ColorChooser the
 * first one returns, not on the page.
 *
 * create_and_show returns the registry id of the Java ColorChooser, or 0. `chooser` is
 * the ColorChooserJava that Java passes back to wkj_color_chooser_set_selected. The
 * colour components are 0..255, as the JNI code passed them.
 * Default when NULL: create_and_show returns 0, the rest do nothing.
 */
typedef struct WKJColorChooserCallbacks {
    wkj_ref (*create_and_show)(wkj_ref page, int32_t red, int32_t green, int32_t blue,
                               int64_t chooser);
    void    (*show)(wkj_ref color_chooser, int32_t red, int32_t green, int32_t blue);
    void    (*hide)(wkj_ref color_chooser);
} WKJColorChooserCallbacks;

/*
 * WKJDragCallbacks - replaces the single cached method id of DragClientJava.
 *
 * `image` is the com.sun.webkit.graphics.WCImage or WCImageFrame the drag is drawn from,
 * as a registry id, 0 when there is none. The JNI code passed whichever of the two the
 * graphics layer produced, with a comment that the rasters are too different to convert
 * in native code; the id keeps that undisturbed - Java still receives one object of one of
 * two classes and decides which.
 *
 * The mime types and their values are two parallel arrays of `count` UTF-16 strings.
 */
typedef struct WKJDragCallbacks {
    void (*start_drag)(wkj_ref page, wkj_ref image,
                       int32_t offset_x, int32_t offset_y,
                       int32_t event_x, int32_t event_y,
                       const uint16_t* const* mime_types, const int32_t* mime_type_lengths,
                       const uint16_t* const* values, const int32_t* value_lengths,
                       int32_t count, int32_t is_image_source);
} WKJDragCallbacks;

/*
 * WKJPopupCallbacks - replaces the 6 cached method ids of PopupMenuJava.
 *
 * Installed once for the process: `create` is a static Java method and the other five are
 * made on the com.sun.webkit.PopupMenu it returns, so nothing here is addressed by page.
 *
 * append_item's `font` is a com.sun.webkit.graphics.WCFont registry id, taken from
 * nativeFontData(); the colours are 0xAARRGGBB, packed exactly as the JNI code packed
 * them. show's `page` is the Java WebPage the menu belongs to.
 */
typedef struct WKJPopupCallbacks {
    /* -> PopupMenu.fwkCreatePopupMenu(long). Returns the new PopupMenu id, or 0. */
    wkj_ref (*create)(int64_t popup);
    void    (*append_item)(wkj_ref popup, const uint16_t* text, int32_t text_length,
                           int32_t is_label, int32_t is_separator, int32_t is_enabled,
                           int32_t background_argb, int32_t foreground_argb, wkj_ref font);
    void    (*set_selected_item)(wkj_ref popup, int32_t index);
    void    (*show)(wkj_ref popup, wkj_ref page, int32_t x, int32_t y, int32_t width);
    void    (*hide)(wkj_ref popup);
    void    (*destroy)(wkj_ref popup);
} WKJPopupCallbacks;

/*
 * The per-page aggregate. A NULL sub-table means the same as a table of NULL slots.
 * WKJDragCallbacks and WKJPopupCallbacks are absent for the reason given at the top of
 * this file.
 *
 * WKJBackForwardCallbacks, WKJColorChooserCallbacks and WKJPopupCallbacks are not here,
 * and that is not an omission: the back/forward list is created before its page, and the
 * colour chooser and popup menu callbacks are made on the Java object the first slot
 * returns rather than on a page, so all three are installed once for the process by their
 * own functions below.
 */
typedef struct WKJPageCallbacks {
    const WKJChromeCallbacks*      chrome;
    const WKJFrameLoaderCallbacks* frame_loader;
    const WKJEditorCallbacks*      editor;
    const WKJInspectorCallbacks*   inspector;
    const WKJProgressCallbacks*    progress;
    const WKJPageNotifyCallbacks*  notify;
    const WKJDragCallbacks*        drag;
} WKJPageCallbacks;

/* ------------------------------------------------------------------------------------- */
/* Page lifecycle                                                                         */
/* ------------------------------------------------------------------------------------- */

/*
 * The three JSC startup options, recorded for the next page creation. Was twkInitWebCore.
 *
 * This one is PURE - it writes three file-static bools and calls nothing - but it cannot
 * become Java, because the state it writes lives inside the library and is read by page
 * creation (FFM-AUDIT-core.md section 9.3).
 */
WKJ_EXPORT void wkj_set_startup_options(int32_t use_jit, int32_t use_dfg_jit,
                                        int32_t use_css3d);

/*
 * Creates a page. Was twkCreatePage, which took the Java WebPage as a jobject; it takes
 * the registry id now, because PageSupplementJava holds a wkj_ref.
 *
 * `web_page` is retained for the life of the page and lent to the seven clients, which is
 * the one retention that replaces the eight JNI global references that used to pin the
 * same Java object.
 *
 * `callbacks` and every sub-table it points at must stay alive and unchanged until
 * wkj_page_destroy has returned; the library keeps the pointer. Only `web_page` is per
 * page - build one process-wide WKJPageCallbacks in one Arena.ofShared() and pass the
 * same pointer every time.
 *
 * Returns the page handle, or 0. Call wkj_page_init next.
 */
WKJ_EXPORT int64_t wkj_page_create(int32_t editable, const WKJPageCallbacks* callbacks,
                                   wkj_ref web_page);

/*
 * Detaches or re-attaches the callback tables of a live page. Passing a null table stops
 * every callback, which is what a Java dispose does before closing the arena that owns
 * the upcall stubs; it is also what replaces the WC_GETJAVAENV_CHKRET guard the frame
 * loader client's destructor relied on during JVM shutdown.
 */
WKJ_EXPORT void wkj_page_set_callbacks(int64_t page, const WKJPageCallbacks* callbacks,
                                       wkj_ref web_page);

/* Was twkInit. Applies the default settings and starts the JSC watchdog. */
WKJ_EXPORT void wkj_page_init(int64_t page, int32_t use_plugins, float device_pixel_scale);

/* Was twkDestroyPage. Stops the loaders, detaches the main frame and deletes the page. */
WKJ_EXPORT void wkj_page_destroy(int64_t page);

/* ------------------------------------------------------------------------------------- */
/* Frame tree                                                                             */
/* ------------------------------------------------------------------------------------- */

/* Was twkGetMainFrame. 0 when the page has no local main frame. */
WKJ_EXPORT int64_t wkj_page_main_frame(int64_t page);

/* Was twkGetParentFrame. 0 at the root and for a non-local frame. */
WKJ_EXPORT int64_t wkj_frame_parent(int64_t frame);

/*
 * Was twkGetChildFrames. Writes up to out_cap local child frames into out and returns how
 * many there are; call it with out == NULL and out_cap == 0 to get the count first.
 *
 * The count-returning shape fixes two defects of the array-returning one by construction,
 * which is a behaviour change and is called out rather than hidden: the JNI version
 * returned a null array for a non-local frame (WebPage.getChildFrames iterated it without
 * a null check), and it sized the array with FrameTree::childCount() while skipping
 * non-local children, so trailing zeroes reached Java as frame handle 0. Neither is
 * reachable with site isolation off, which is how this port is built.
 */
WKJ_EXPORT int32_t wkj_frame_children(int64_t frame, int64_t* out, int32_t out_cap);

/* ------------------------------------------------------------------------------------- */
/* Frame content                                                                          */
/* ------------------------------------------------------------------------------------- */

/* Was twkGetName. FrameTree::uniqueName(). */
WKJ_EXPORT int32_t wkj_frame_name(int64_t frame, uint16_t* result_buf, int32_t result_cap,
                                  int32_t* result_length);
/* Was twkGetURL. Document::url(). */
WKJ_EXPORT int32_t wkj_frame_url(int64_t frame, uint16_t* result_buf, int32_t result_cap,
                                 int32_t* result_length);
/* Was twkGetInnerText. Lays out first if a layout is pending. */
WKJ_EXPORT int32_t wkj_frame_inner_text(int64_t frame, uint16_t* result_buf,
                                        int32_t result_cap, int32_t* result_length);
/* Was twkGetRenderTree. externalRepresentation(). */
WKJ_EXPORT int32_t wkj_frame_render_tree(int64_t frame, uint16_t* result_buf,
                                         int32_t result_cap, int32_t* result_length);
/* Was twkGetContentType. DocumentLoader::responseMIMEType(). */
WKJ_EXPORT int32_t wkj_frame_content_type(int64_t frame, uint16_t* result_buf,
                                          int32_t result_cap, int32_t* result_length);
/* Was twkGetTitle. */
WKJ_EXPORT int32_t wkj_frame_title(int64_t frame, uint16_t* result_buf, int32_t result_cap,
                                   int32_t* result_length);
/* Was twkGetHtml. The serialized document, or WKJ_STR_NULL when there is no document. */
WKJ_EXPORT int32_t wkj_frame_html(int64_t frame, uint16_t* result_buf, int32_t result_cap,
                                  int32_t* result_length);

/* ------------------------------------------------------------------------------------- */
/* Navigation                                                                             */
/* ------------------------------------------------------------------------------------- */

/* Was twkOpen. */
WKJ_EXPORT void wkj_frame_open(int64_t frame, const uint16_t* url, int32_t url_length);

/*
 * Was twkLoad: loads `content` as substitute data with the given content type.
 *
 * ENCODING HAZARD, PRESERVED DELIBERATELY. The JNI version read the HTML with
 * GetStringUTFChars, which produces *modified* UTF-8, and handed those bytes to a
 * SharedBuffer inside a ResourceResponse that declares the charset "UTF-8". For U+0000
 * and for every supplementary character the two encodings differ - modified UTF-8 writes
 * a surrogate pair as two three-byte sequences - so WebEngine.loadContent of an astral
 * character has always fed CESU-8 to a decoder that was told it was UTF-8.
 *
 * `content` is therefore documented as modified UTF-8, and the Java side must encode it
 * that way, because a migration commit may not change behaviour. Switching to standard
 * UTF-8 fixes a real latent bug and belongs in its own commit with its own test.
 *
 * This is the only place in this half of the ABI where a string is not UTF-16.
 */
WKJ_EXPORT void wkj_frame_load(int64_t frame, const uint8_t* content, int32_t content_length,
                               const uint16_t* content_type, int32_t content_type_length);

/* Was twkIsLoading. */
WKJ_EXPORT int32_t wkj_frame_is_loading(int64_t frame);
/* Was twkStop. */
WKJ_EXPORT void wkj_frame_stop(int64_t frame);
/* Was twkStopAll. */
WKJ_EXPORT void wkj_page_stop_all(int64_t page);
/* Was twkRefresh. */
WKJ_EXPORT void wkj_frame_refresh(int64_t frame);
/* Was twkGoBackForward. 1 when the page moved. */
WKJ_EXPORT int32_t wkj_page_go_back_forward(int64_t page, int32_t distance);
/* Was twkReset: FrameTree::clearName(). */
WKJ_EXPORT void wkj_frame_clear_name(int64_t frame);

/*
 * twkExecuteScript has no counterpart here: everything it did after finding the frame was
 * LiveConnect work, so it became wkj_frame_execute_script, declared in
 * webkit_java_api_bridge.h and implemented in Source/WebCore/bridge/jni/jsc/BridgeUtils.cpp.
 */

/* ------------------------------------------------------------------------------------- */
/* Find, zoom, preferences                                                                */
/* ------------------------------------------------------------------------------------- */

/* Was twkFindInPage. */
WKJ_EXPORT int32_t wkj_page_find(int64_t page, const uint16_t* to_find, int32_t to_find_length,
                                 int32_t forward, int32_t wrap, int32_t match_case);
/* Was twkFindInFrame. */
WKJ_EXPORT int32_t wkj_frame_find(int64_t frame, const uint16_t* to_find, int32_t to_find_length,
                                  int32_t forward, int32_t wrap, int32_t match_case);

/* Was twkGetZoomFactor / twkSetZoomFactor. */
WKJ_EXPORT float wkj_frame_get_zoom(int64_t frame, int32_t text_only);
WKJ_EXPORT void  wkj_frame_set_zoom(int64_t frame, float zoom_factor, int32_t text_only);

/* Was twkOverridePreference. Used by DumpRenderTree only. */
WKJ_EXPORT void wkj_page_override_preference(int64_t page,
                                             const uint16_t* name, int32_t name_length,
                                             const uint16_t* value, int32_t value_length);
/* Was twkResetToConsistentStateBeforeTesting. */
WKJ_EXPORT void wkj_page_reset_for_testing(int64_t page);

/* ------------------------------------------------------------------------------------- */
/* Geometry, painting and printing                                                        */
/* ------------------------------------------------------------------------------------- */

/* Was twkSetBounds. */
WKJ_EXPORT void wkj_page_set_bounds(int64_t page, int32_t x, int32_t y,
                                    int32_t width, int32_t height);
/* Was twkGetFrameHeight. */
WKJ_EXPORT int32_t wkj_frame_height(int64_t frame);
/* Was twkAdjustFrameHeight. */
WKJ_EXPORT float wkj_frame_adjust_height(int64_t frame, float old_top, float old_bottom,
                                         float bottom_limit);

/*
 * Was twkGetVisibleRect / twkGetContentSize, which allocated a fresh int[] and filled it
 * under GetPrimitiveArrayCritical. out receives x, y, width, height and width, height
 * respectively. Returns 1 when the frame has a view, 0 when it does not - the JNI version
 * returned a null array in that case.
 *
 * Note for the record: twkGetTextLocation below released its critical region with
 * JNI_ABORT, which discards writes unless the VM pinned rather than copied. It worked
 * only because HotSpot pins. The out-parameter form removes that dependence.
 */
WKJ_EXPORT int32_t wkj_frame_visible_rect(int64_t frame, int32_t* out_xywh);
WKJ_EXPORT int32_t wkj_frame_content_size(int64_t frame, int32_t* out_wh);

/* Was twkScrollToPosition. */
WKJ_EXPORT void wkj_frame_scroll_to(int64_t frame, int32_t x, int32_t y);
/* Was twkSetTransparent. */
WKJ_EXPORT void wkj_frame_set_transparent(int64_t frame, int32_t transparent);
/* Was twkSetBackgroundColor. The colour is 0xAARRGGBB, as the Java side packs it. */
WKJ_EXPORT void wkj_frame_set_background_color(int64_t frame, int32_t argb);

/* Was twkPrePaint / twkUpdateRendering. */
WKJ_EXPORT void wkj_page_pre_paint(int64_t page);
WKJ_EXPORT void wkj_page_update_rendering(int64_t page);

/*
 * Was twkUpdateContent / twkPostPaint. `render_queue` is the registry id of the
 * com.sun.webkit.graphics.WCRenderQueue the drawing commands are encoded into; the JNI
 * versions took the same object as a jobject. The library does not retain it beyond the
 * call - the RenderingQueue it builds takes its own reference.
 */
WKJ_EXPORT void wkj_page_update_content(int64_t page, wkj_ref render_queue,
                                        int32_t x, int32_t y, int32_t width, int32_t height);
WKJ_EXPORT void wkj_page_post_paint(int64_t page, wkj_ref render_queue,
                                    int32_t x, int32_t y, int32_t width, int32_t height);

/* Was twkBeginPrinting / twkEndPrinting. Returns the page count. */
WKJ_EXPORT int32_t wkj_page_begin_printing(int64_t page, float width, float height);
WKJ_EXPORT void    wkj_page_end_printing(int64_t page);

/* Was twkPrint. Renders one page into `render_queue`, as wkj_page_update_content does. */
WKJ_EXPORT void wkj_page_print(int64_t page, wkj_ref render_queue, int32_t page_index,
                               float width);

/* ------------------------------------------------------------------------------------- */
/* Encoding                                                                               */
/* ------------------------------------------------------------------------------------- */

/* Was twkGetEncoding / twkSetEncoding. */
WKJ_EXPORT int32_t wkj_page_get_encoding(int64_t page, uint16_t* result_buf,
                                         int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_page_set_encoding(int64_t page, const uint16_t* encoding,
                                      int32_t encoding_length);

/* ------------------------------------------------------------------------------------- */
/* Input events                                                                           */
/* ------------------------------------------------------------------------------------- */

/*
 * Was twkProcessFocusEvent. id and direction are com.sun.webkit.event.WCFocusEvent
 * constants.
 */
WKJ_EXPORT void wkj_page_focus_event(int64_t page, int32_t id, int32_t direction);

/*
 * Was twkProcessKeyEvent. `type` and `windows_virtual_key_code` are
 * com.sun.webkit.event.WCKeyEvent constants. The four modifier flags stay four int32_t
 * parameters for the same reason as the mouse event below: the JNI parameter list is
 * reproduced one for one, and no bitmask encoding has to be agreed between the two sides.
 *
 * Returns 1 when WebKit consumed the event.
 */
WKJ_EXPORT int32_t wkj_page_key_event(int64_t page, int32_t type,
                                      const uint16_t* text, int32_t text_length,
                                      const uint16_t* key_identifier,
                                      int32_t key_identifier_length,
                                      int32_t windows_virtual_key_code,
                                      int32_t shift, int32_t ctrl, int32_t alt,
                                      int32_t meta, double timestamp);

/*
 * Was twkProcessMouseEvent. id, button and button_mask are
 * com.sun.webkit.event.WCMouseEvent constants, button_mask being the same bit set as
 * button. The four modifier flags stay four int32_t parameters rather than becoming one
 * bitmask or a struct: the JNI parameter list is reproduced one
 * for one, which is what makes the conversion reviewable, and neither a bitmask encoding
 * nor a by-value struct layout has to be agreed between the two sides.
 *
 * Returns 1 when WebKit consumed the event.
 */
WKJ_EXPORT int32_t wkj_page_mouse_event(int64_t page, int32_t id, int32_t button,
                                        int32_t button_mask, int32_t click_count,
                                        int32_t x, int32_t y,
                                        int32_t screen_x, int32_t screen_y,
                                        int32_t shift, int32_t ctrl, int32_t alt,
                                        int32_t meta, int32_t popup_trigger,
                                        double timestamp);

/* Was twkProcessMouseWheelEvent. */
WKJ_EXPORT int32_t wkj_page_wheel_event(int64_t page, int32_t x, int32_t y,
                                        int32_t screen_x, int32_t screen_y,
                                        float delta_x, float delta_y,
                                        int32_t shift, int32_t ctrl, int32_t alt,
                                        int32_t meta, double timestamp);

/*
 * Was twkProcessDrag. `action_id` is one of the com.sun.webkit.WebPage DND_* constants.
 *
 * mime_count selects the branch, replacing the "jMimes == NULL" test of the JNI version:
 * mime_count >= 0 is the drop-target branch and mime_count < 0 the drag-source branch, in
 * which the four array parameters are ignored and may be NULL.
 *
 * In the target branch, mimes and values are arrays of mime_count UTF-16 pointers with
 * their lengths in mime_lengths and value_lengths. An entry whose value pointer is NULL
 * is skipped, which is what the JNI loop did for a null array element.
 *
 * The return value is the java.awt.dnd.DnDConstants action to show, or 0.
 */
WKJ_EXPORT int32_t wkj_page_process_drag(int64_t page, int32_t action_id,
                                         const uint16_t* const* mimes,
                                         const int32_t* mime_lengths,
                                         const uint16_t* const* values,
                                         const int32_t* value_lengths,
                                         int32_t mime_count,
                                         int32_t x, int32_t y,
                                         int32_t screen_x, int32_t screen_y,
                                         int32_t java_action);

/* ------------------------------------------------------------------------------------- */
/* Input method editing                                                                   */
/* ------------------------------------------------------------------------------------- */

/*
 * Was twkProcessInputTextChange. `attributes` is the flat int[] of
 * (startOffset, endOffset, thick) triples the Java side already builds; attribute_count is
 * its length, not the number of triples. Always returns 1, as the JNI version did.
 */
WKJ_EXPORT int32_t wkj_page_input_text_change(int64_t page,
                                              const uint16_t* committed, int32_t committed_length,
                                              const uint16_t* composed, int32_t composed_length,
                                              const int32_t* attributes, int32_t attribute_count,
                                              int32_t caret_position);

/* Was twkProcessCaretPositionChange. */
WKJ_EXPORT int32_t wkj_page_caret_position_change(int64_t page, int32_t caret_position);

/* Was twkGetTextLocation. out receives x, y, width, height. Returns 1 on success. */
WKJ_EXPORT int32_t wkj_page_text_location(int64_t page, int32_t char_index, int32_t* out_xywh);

/* Was twkGetInsertPositionOffset / twkGetCommittedTextLength. */
WKJ_EXPORT int32_t wkj_page_insert_position_offset(int64_t page);
WKJ_EXPORT int32_t wkj_page_committed_text_length(int64_t page);

/* Was twkGetCommittedText / twkGetSelectedText. */
WKJ_EXPORT int32_t wkj_page_committed_text(int64_t page, uint16_t* result_buf,
                                           int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_page_selected_text(int64_t page, uint16_t* result_buf,
                                          int32_t result_cap, int32_t* result_length);

/* ------------------------------------------------------------------------------------- */
/* Editing commands                                                                       */
/* ------------------------------------------------------------------------------------- */

/* Was twkCopy. */
WKJ_EXPORT int32_t wkj_frame_copy(int64_t frame);

/* Was twkExecuteCommand and the three query entry points. */
WKJ_EXPORT int32_t wkj_page_execute_command(int64_t page,
                                            const uint16_t* command, int32_t command_length,
                                            const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_page_query_command_enabled(int64_t page, const uint16_t* command,
                                                  int32_t command_length);
WKJ_EXPORT int32_t wkj_page_query_command_state(int64_t page, const uint16_t* command,
                                                int32_t command_length);
WKJ_EXPORT int32_t wkj_page_query_command_value(int64_t page, const uint16_t* command,
                                                int32_t command_length,
                                                uint16_t* result_buf, int32_t result_cap,
                                                int32_t* result_length);

/* Was twkIsEditable / twkSetEditable. */
WKJ_EXPORT int32_t wkj_page_is_editable(int64_t page);
WKJ_EXPORT void    wkj_page_set_editable(int64_t page, int32_t editable);

/* ------------------------------------------------------------------------------------- */
/* Settings                                                                               */
/* ------------------------------------------------------------------------------------- */

WKJ_EXPORT int32_t wkj_page_get_use_page_cache(int64_t page);
WKJ_EXPORT void    wkj_page_set_use_page_cache(int64_t page, int32_t use_page_cache);
WKJ_EXPORT int32_t wkj_page_is_script_enabled(int64_t page);
WKJ_EXPORT void    wkj_page_set_script_enabled(int64_t page, int32_t enabled);
WKJ_EXPORT int32_t wkj_page_is_context_menu_enabled(int64_t page);
WKJ_EXPORT void    wkj_page_set_context_menu_enabled(int64_t page, int32_t enabled);
WKJ_EXPORT int32_t wkj_page_get_developer_extras(int64_t page);
WKJ_EXPORT void    wkj_page_set_developer_extras(int64_t page, int32_t enabled);
WKJ_EXPORT void    wkj_page_set_user_stylesheet(int64_t page, const uint16_t* url,
                                                int32_t url_length);
WKJ_EXPORT int32_t wkj_page_get_user_agent(int64_t page, uint16_t* result_buf,
                                           int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void    wkj_page_set_user_agent(int64_t page, const uint16_t* user_agent,
                                           int32_t user_agent_length);
WKJ_EXPORT void    wkj_page_set_local_storage_path(int64_t page, const uint16_t* path,
                                                   int32_t path_length);
WKJ_EXPORT void    wkj_page_set_local_storage_enabled(int64_t page, int32_t enabled);

/* Was twkGetUnloadEventListenersCount. */
WKJ_EXPORT int32_t wkj_frame_unload_listener_count(int64_t frame);

/* ------------------------------------------------------------------------------------- */
/* Web Inspector and workers                                                              */
/* ------------------------------------------------------------------------------------- */

WKJ_EXPORT void wkj_page_inspector_connect(int64_t page);
WKJ_EXPORT void wkj_page_inspector_disconnect(int64_t page);
WKJ_EXPORT void wkj_page_inspector_dispatch(int64_t page, const uint16_t* message,
                                            int32_t message_length);

/* Was twkWorkerThreadCount. Process wide. */
WKJ_EXPORT int32_t wkj_worker_thread_count(void);

/* ------------------------------------------------------------------------------------- */
/* Back/forward list (com.sun.webkit.BackForwardList)                                      */
/* ------------------------------------------------------------------------------------- */

/*
 * The item handle is a WebCore::HistoryItem*, which is what the Java Entry holds as
 * `pitem`. The list itself is addressed by the page handle, as it was before.
 */

/* Was bflItemGetURL / bflItemGetTitle. */
WKJ_EXPORT int32_t wkj_bfl_item_url(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                    int32_t* result_length);
WKJ_EXPORT int32_t wkj_bfl_item_title(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                      int32_t* result_length);
/*
 * Was bflItemGetTarget. An empty target is reported as WKJ_STR_NULL, which is what the JNI
 * version did by returning a null jstring for it.
 */
WKJ_EXPORT int32_t wkj_bfl_item_target(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                       int32_t* result_length);
/* Was bflItemIsTargetItem. */
WKJ_EXPORT int32_t wkj_bfl_item_is_target(int64_t item);

/*
 * Was bflGet: the BackForwardList entry at `index`, or 0 when there is no item there.
 * The id is borrowed - the library keeps the entry alive in HistoryItem::m_hostObject -
 * so the caller must not release it. The JNI version returned the same Java object.
 */
WKJ_EXPORT wkj_ref wkj_bfl_item_at(int64_t page, int32_t index);

/*
 * Was bflItemGetChildren: writes up to out_cap child entry ids into out and returns how
 * many there are; call it with out == NULL and out_cap == 0 to get the count first. The
 * ids are borrowed, as for wkj_bfl_item_at.
 */
WKJ_EXPORT int32_t wkj_bfl_item_children(int64_t item, int64_t page, wkj_ref* out,
                                         int32_t out_cap);

/* Was bflSize / bflGetMaximumSize / bflSetMaximumSize / bflGetCurrentIndex. */
WKJ_EXPORT int32_t wkj_bfl_size(int64_t page);
WKJ_EXPORT int32_t wkj_bfl_get_capacity(int64_t page);
WKJ_EXPORT void    wkj_bfl_set_capacity(int64_t page, int32_t capacity);
WKJ_EXPORT int32_t wkj_bfl_current_index(int64_t page);

/* Was bflSetCurrentIndex. Returns the index actually selected, or -1. */
WKJ_EXPORT int32_t wkj_bfl_set_current_index(int64_t page, int32_t index);
/* Was bflIndexOf. Negative when the item is not in the list. */
WKJ_EXPORT int32_t wkj_bfl_index_of(int64_t page, int64_t item, int32_t reverse);
/* Was bflSetEnabled / bflIsEnabled. */
WKJ_EXPORT void    wkj_bfl_set_enabled(int64_t page, int32_t enabled);
WKJ_EXPORT int32_t wkj_bfl_is_enabled(int64_t page);
/* Was bflClearBackForwardListForDRT. */
WKJ_EXPORT void    wkj_bfl_clear_for_drt(int64_t page);

/*
 * Was bflSetHostObject, which took the Java BackForwardList as a jobject and kept a global
 * reference to it. It now takes the registry id, and the id is what
 * WKJBackForwardCallbacks::list_changed is called with. Passing 0 detaches the list.
 *
 * The library retains the id with WKJHostCore::retain and releases it when the list is
 * replaced or destroyed, per the ownership rule in webkit_java_api.h.
 */
WKJ_EXPORT void wkj_bfl_set_host(int64_t page, wkj_ref back_forward_list);

/*
 * Installs the back/forward callbacks for the process. The BackForwardList is created by
 * page creation, before the page exists, so it cannot be reached through
 * wkj_page_set_callbacks; and list_changed is called with the id wkj_bfl_set_host was
 * given, not with the page, so nothing per page is needed.
 */
WKJ_EXPORT void wkj_bfl_set_callbacks(const WKJBackForwardCallbacks* callbacks);

/* ------------------------------------------------------------------------------------- */
/* Page cache (com.sun.webkit.PageCache)                                                   */
/* ------------------------------------------------------------------------------------- */

/* Was twkGetCapacity / twkSetCapacity. BackForwardCache::singleton(), process wide. */
WKJ_EXPORT int32_t wkj_page_cache_get_capacity(void);
WKJ_EXPORT void    wkj_page_cache_set_capacity(int32_t capacity);

/* ------------------------------------------------------------------------------------- */
/* Colour chooser (com.sun.webkit.ColorChooser)                                            */
/* ------------------------------------------------------------------------------------- */

/*
 * Installs the colour chooser callbacks for the process. Both this and
 * wkj_color_chooser_set_selected exist only when the library was built with
 * ENABLE(INPUT_TYPE_COLOR), which Source/cmake/OptionsJava.cmake:88 turns ON for this
 * port; a Java facade should still tolerate them being absent.
 */
WKJ_EXPORT void wkj_install_color_chooser_callbacks(const WKJColorChooserCallbacks* callbacks);

/*
 * Was twkSetSelectedColor. `chooser` is the handle create_and_show was given. The
 * components are 0..255 and are clamped, as they were before.
 */
WKJ_EXPORT void wkj_color_chooser_set_selected(int64_t chooser, int32_t red, int32_t green,
                                               int32_t blue);

/* ------------------------------------------------------------------------------------- */
/* Popup menu (com.sun.webkit.PopupMenu)                                                   */
/* ------------------------------------------------------------------------------------- */

/* Installs the popup menu callbacks for the process; see WKJPopupCallbacks. */
WKJ_EXPORT void wkj_install_popup_callbacks(const WKJPopupCallbacks* callbacks);

/*
 * Was twkSelectionCommited and twkPopupClosed. `popup` is the handle create was given -
 * a PopupMenuJava, which is what the Java PopupMenu holds as `pdata`.
 */
WKJ_EXPORT void wkj_popup_selection_committed(int64_t popup, int32_t index);
WKJ_EXPORT void wkj_popup_closed(int64_t popup);

/* ------------------------------------------------------------------------------------- */
/* Process-wide callbacks that belong to no page                                          */
/* ------------------------------------------------------------------------------------- */

/*
 * Installs NetworkContext.canHandleURL, which FrameLoaderClientJava calls as a static Java
 * method. Passing NULL detaches it, after which canHandleRequest answers 0 for every URL
 * the loader does not handle itself.
 */
WKJ_EXPORT void wkj_install_network_callbacks(const WKJNetworkCallbacks* callbacks);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_PAGE_H */
