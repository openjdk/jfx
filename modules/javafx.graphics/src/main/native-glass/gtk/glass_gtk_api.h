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
 * glass_gtk_api.h - the flat C ABI of the GTK Glass library (libglassgtk3.so). Linux only.
 *
 * This is the surface com.sun.glass.ui.gtk.GtkGlassNative binds through java.lang.foreign. Every function
 * takes and returns <stdint.h> scalars, opaque pointers or caller-owned buffers; nothing here knows the JVM
 * (no jobject, no JNIEnv, no jni.h). The calls from the C into Java go through the four callback tables
 * installed with ggtk_*_set_callbacks; the calls from Java into the C go through the ggtk_application_*,
 * ggtk_window_*, ggtk_view_* and ggtk_dnd_* functions at the end of this header, one per former JNI native
 * of GtkApplication, GtkWindow, GtkView and GtkDnDClipboard that needs the C of this library (THE NATIVES
 * names the four whose work Java now does itself).
 *
 * WHAT THE TABLES REPLACE. The window, view, drag-and-drop and application C of this library (the
 * WindowContext classes of glass_window.cpp / glass_window_ime.cpp, GlassView.cpp, glass_dnd.cpp,
 * glass_screen.cpp and glass_general.cpp) reached Java through cached jmethodIDs and mainEnv. Each such
 * call site now dials a slot of these tables. The C logic around the call - the order of calls, the
 * NULL checks, the early returns after an exception - is the C of commit 033187ad90, unchanged; only the
 * mechanism that reaches Java differs. Every slot documents the Java method it stands for and the C
 * function(s) whose call it replaces.
 *
 * NULL SLOTS. A call site dials its slot when the slot is non-NULL and makes NO CALL when it is NULL: the C
 * then continues as after a normal return of the Java target, with every out-parameter at the value the C
 * initialised it to - the value HotSpot answered after a throw (see EXCEPTIONS). So a NULL slot never
 * crashes: is_enabled leaves the window disabled (input events and GDK_DELETE are dropped),
 * non_client_hit_test answers GGTK_HT_UNSPECIFIED, notify_drag_enter / notify_drag_over answer no action,
 * source_get_data answers no value (nothing is served to the drop target and there is no drag image),
 * get_application_name sets no WM_CLASS, notify_input_method_candidate_pos_request sets no cursor
 * location, and a notify_* slot notifies nobody. Java installs every slot of every table before
 * GtkApplication._initGTK; a NULL slot is only seen when a table is cleared or filled with NULLs on purpose
 * (the shim's tests). Up to commit 033187ad90 every one of these sites called Java through JNI (a cached
 * jmethodID and the JNIEnv GtkApplication._init stored); that JNI, and the Java_* functions of GtkWindow,
 * GtkView, GtkDnDClipboard and GtkApplication, are gone.
 *
 * IDENTITY. Window and view slots carry the int64_t id Java assigned to the peer: Java hands it to the C
 * with ggtk_window_create / ggtk_view_create and keeps the id -> peer registry itself. The library never
 * dereferences an id and never holds a Java reference for it. The window's current view id is copied by
 * ggtk_window_set_view (GtkWindow._setView) from the GlassView it is given. An id stays valid until the C
 * clears it: a window's id and its view id are cleared exactly where WindowContextBase::process_destroy
 * deleted the JNI global references at commit 033187ad90 (after notify_destroy), a view id of a window where
 * ggtk_window_set_view replaced or dropped the view. Where the C of commit 033187ad90 tested whether the
 * window or view had a peer (`jwindow` / `jview`), it tests for a non-zero id.
 * ID 0 means "the JNI receiver was NULL". A few sites call Java without checking that the window or view
 * still has a peer (they are listed at their slots). There HotSpot's CallVoidMethod raised a
 * NullPointerException for the NULL receiver, and the site reported it (or left it pending) exactly as it
 * would have reported an exception of the target. A slot called with id 0 must therefore report a new
 * NullPointerException through Application.reportException and return GGTK_UPCALL_THREW; value slots then
 * leave their out-parameters untouched. A non-zero id the Java registry no longer knows is a stale peer:
 * do nothing, return GGTK_UPCALL_OK.
 *
 * EXCEPTIONS. Every slot returns an int32_t status: GGTK_UPCALL_OK when the Java target returned normally,
 * GGTK_UPCALL_THREW when it threw. A slot must never let a Throwable escape (an exception escaping an FFM
 * upcall stub terminates the JVM): it catches it and reports it exactly as glass_general.cpp's
 * check_and_clear_exception did at commit 033187ad90 - Application.reportException(t), which hands it to
 * the current thread's uncaught-exception handler, and a Throwable thrown by that handler is swallowed.
 * The C branches on the status exactly where it branched on a pending exception: a CHECK_JNI_EXCEPTION
 * site returns, an EXCEPTION_OCCURED / LOG_EXCEPTION site continues, a site that did not check at all
 * ignores the status (the JNI left the exception pending there; reporting it is the one difference an
 * upcall stub imposes, and each such site is named at its slot). A value slot writes its out-parameter
 * only when the target returned normally; the C initialises it to the value HotSpot answered after a throw
 * (0, NULL), which is also what a NULL slot leaves (see NULL SLOTS).
 *
 * STRINGS AND ARRAYS. Text the C handed to NewStringUTF crosses as the same bytes (const char* plus a byte
 * length, no terminator counted); Java decodes them as NewStringUTF did (modified UTF-8 as HotSpot reads
 * it: com.sun.javafx.font.JniStringCodec.fromNewStringUtf), never with a charset decoder. Text the C read
 * with GetStringUTFChars crosses back as modified UTF-8 (JniStringCodec.toModifiedUtf8) in a
 * NUL-terminated block Java allocates with GLib's g_malloc; the C owns it and releases it with g_free.
 * UTF-16 text crosses as uint16_t code units, lone surrogates included. Every pointer handed to a slot is
 * borrowed for the duration of the call only. The ggtk_* functions of the natives use the same codecs,
 * each stated at the function, and say who allocates and who frees each buffer.
 *
 * THREAD. Every slot runs on the thread that iterates GLib's default main context: GtkNativeMainLoopThread,
 * which is the JavaFX application thread, or the SWT thread when javafx.embed.isEventThread is set. They
 * run inside gdk_event_handler_set's handler, inside GLib/GObject signal emissions, or synchronously inside
 * a Glass native that Java called on that thread. Slots are RE-ENTRANT: a slot may call back into the
 * library (close the window, run a nested main loop - enterNestedEventLoop, a clipboard wait, the drag
 * loops of GtkDnDClipboard) and every slot may be dialled again before it returns. No slot may take a
 * lock a Glass downcall on the same thread already holds.
 *
 * INSTALLATION. The installers copy the table by value (the caller may free the struct), NULL clears a
 * table, the last call wins, and there is no lock: install every table once, on the toolkit startup path,
 * before GtkApplication._init and before the first window exists. The stubs a table points to must stay
 * valid for the life of the process (Arena.global()): there is no point at which this library can promise
 * that GTK will not deliver one more event.
 *
 * THE NATIVES' FUNCTIONS never throw and never leave anything pending: where the JNI native threw (or
 * returned with an exception pending), the function returns a status and Java throws the same exception
 * type with the same message after the downcall returns - the point where the pending exception surfaced.
 * Each function marked UPCALLS can dial a slot of the tables (directly, through a GTK signal it emits, or
 * through the events of a main loop it runs); every slot may be re-entered from those. Each function runs
 * on the GLib main-context thread above unless it says otherwise.
 *
 * critical(true): FORBIDDEN on ggtk_test_fire_callback and on every function marked UPCALLS. The
 * installers, the id getters, the sizeof probes and ggtk_abi_version neither upcall nor block;
 * critical(true) is legal there and buys nothing. For the other natives' functions no slot is on their
 * path (by reading), but they call into GTK; see each function. That includes the frame uploads
 * (ggtk_view_upload_pixels_direct / _int / _byte): no slot, but each paints a whole frame to the X server,
 * which takes milliseconds and blocks while the server does not read. A critical downcall keeps its thread
 * in Java for the whole call, so every safepoint of the VM would wait for the paint: critical(true) must
 * not be used on them.
 *
 * SHIM-ONLY TEST HOOKS - exported, bound by the test shim GtkGlassShim and NOWHERE in GtkGlassNative:
 * ggtk_test_fire_callback, ggtk_window_get_id, ggtk_window_get_view_id and ggtk_view_get_id. They ship in
 * the production library because the shim tests run against it; a removal or a prototype change of theirs
 * does not move GLASS_GTK_ABI_VERSION.
 */

#ifndef GLASS_GTK_API_H
#define GLASS_GTK_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define GGTK_EXPORT __declspec(dllexport)
#else
#  define GGTK_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Must be bumped for a removal, or a prototype, struct-layout or constant change, in anything
 * GtkGlassNative binds. An export added together with its Java binding rides the current version.
 * Java calls ggtk_abi_version first and refuses the library on a mismatch.
 * ABI 1 = the callback tables, the id getters, the sizeof probes, the test hook, the functions of the
 * natives below and the values shared with Java.
 */
#define GLASS_GTK_ABI_VERSION 1

GGTK_EXPORT int32_t ggtk_abi_version(void);

/* Status of the functions below. */
#define GGTK_OK                 0
#define GGTK_ERR_INVALID_ARG  (-1)   /* unknown slot number (ggtk_test_fire_callback) */
#define GGTK_ERR_NOT_INSTALLED (-2)  /* the slot is NULL (ggtk_test_fire_callback) */
#define GGTK_ERR_NOT_IN_DRAG  (-3)   /* no drag has entered a window: the JNI threw IllegalStateException
                                      * ("Cannot get supported actions. Drag pointer haven't entered the
                                      * application window") - the ggtk_dnd_target_* functions */
#define GGTK_ERR_GTK_VERSION  (-4)   /* GTK is older than the build's minimum: the JNI threw
                                      * UnsupportedOperationException - ggtk_application_init_gtk */

/* Status every callback slot returns (see EXCEPTIONS). */
#define GGTK_UPCALL_OK    0
#define GGTK_UPCALL_THREW 1

/*
 * VALUES SHARED WITH JAVA. The two blocks below are the values of constants of com.sun.glass.ui.gtk. The C of
 * this library compares against the GGTK_HT_*; the GGTK_QUERY_* are the answers of the library query, which is
 * Java's, and are stated here as the contract that query keeps. Up to commit 033187ad90 both were read as the
 * com_sun_glass_ui_gtk_GtkApplication_QUERY_* and com_sun_glass_ui_gtk_GtkWindow_HT_* macros of the headers
 * javac -h writes for those two classes, which tied launcher.c, GlassApplication.cpp and glass_window.cpp to a
 * generated header. They are stated here instead: the two sides must be changed together, and the Java side
 * pins each value with a test.
 */

/*
 * What GtkApplication._queryLibrary(int, boolean) answers: which glass GTK library to load, or why none can be.
 * No C reads them any more - com.sun.glass.ui.gtk.GtkGlassNative answers the query in Java and pins these
 * values against this header. Up to commit 033187ad90 launcher.c (the whole of libglass.so) answered
 * GGTK_QUERY_NO_DISPLAY, GGTK_QUERY_LOAD_GTK3 or GGTK_QUERY_ERROR, and the copy of it in GlassApplication.cpp,
 * which answered when a libglassgtk3.so build had itself been renamed to libglass.so, answered
 * GGTK_QUERY_NO_DISPLAY or GGTK_QUERY_USE_CURRENT. Both are deleted.
 */
#define GGTK_QUERY_ERROR      (-2)   /* no GTK 3 library could be loaded, or GTK 2 is already in the process:
                                      * GtkApplication throws UnsupportedOperationException */
#define GGTK_QUERY_NO_DISPLAY (-1)   /* XOpenDisplay(NULL) failed: GtkApplication throws
                                      * UnsupportedOperationException("Unable to open DISPLAY") */
#define GGTK_QUERY_USE_CURRENT  1    /* the library that answered is itself the glass GTK library; load none */
#define GGTK_QUERY_LOAD_GTK3    3    /* libgtk-3 is loaded: GtkApplication loads libglassgtk3.so */

/*
 * What GtkWindow.nonClientHitTest(int, int) answers, and so what the non_client_hit_test slot of
 * GgtkWindowCallbacks writes to *out_result. WindowContextTop::process_mouse_button and
 * WindowContextTop::process_mouse_motion (glass_window.cpp) compare the answer against these.
 */
#define GGTK_HT_UNSPECIFIED 0
#define GGTK_HT_CAPTION     1
#define GGTK_HT_CLIENT      2

/* An opaque WindowContext*: the value GtkWindow._createWindow returned (Window.ptr). */
typedef void* ggtk_window_t;

/* An opaque GlassView*: the value GtkView._create returned (View.ptr). */
typedef void* ggtk_view_t;

/*
 * ---- GgtkAppCallbacks: application-wide targets (no id) ----
 */
typedef struct GgtkAppCallbacks {
    /*
     * Screen.notifySettingsChanged() (static). Replaces glass_screen.cpp screen_settings_changed, the
     * handler of GdkScreen "monitors-changed" / "size-changed" (connected by GtkApplication._init) and of
     * the root window's _NET_WORKAREA / _NET_CURRENT_DESKTOP PropertyNotify (GlassApplication.cpp
     * process_events). LOG_EXCEPTION site: the status is ignored.
     */
    int32_t (*notify_screen_settings_changed)(void);

    /*
     * QUERY. Application.GetApplication().getName(), for WM_CLASS. Replaces glass_general.cpp
     * get_application_name, called by the WindowContextTop constructor inside GtkWindow._createWindow
     * (so this slot runs INSIDE ggtk_window_create; it carries no window id).
     * On GGTK_UPCALL_OK *out_name is the name as modified UTF-8 (what GetStringUTFChars produced), NUL
     * terminated, in a block allocated with g_malloc that the C owns and releases with g_free - the
     * g_strdup the JNI made. A null name leaves *out_name NULL (the JNI called GetStringUTFChars(NULL)
     * there, which crashes). Both calls of the JNI were CHECK_JNI_EXCEPTION_RET(NULL) sites: on
     * GGTK_UPCALL_THREW (including the NullPointerException of a null GetApplication()) the C answers
     * NULL and sets no WM_CLASS, and *out_name must not have been written. The C initialises *out_name
     * to NULL.
     */
    int32_t (*get_application_name)(char** out_name);
} GgtkAppCallbacks;   /* 2 pointers */

/*
 * ---- GgtkWindowCallbacks: Window / GtkWindow targets, keyed by the window id ----
 *
 * Boolean arguments and results are int32_t 0 / 1. All constants are the Java ones the JNI passed
 * (com.sun.glass.events.WindowEvent, com.sun.glass.ui.Window.Level).
 */
typedef struct GgtkWindowCallbacks {
    /*
     * QUERY, HOT. Window.isEnabled(). Replaces WindowContextBase::isEnabled, called for every input event
     * by GlassApplication.cpp is_window_enabled_for_event, and by process_focus, process_delete and
     * WindowContextTop::set_visible. Writes 1 (true) or 0 (false) to *out_enabled; the C returns
     * *out_enabled == 1, as it returned JNI_TRUE == CallBooleanMethod. LOG_EXCEPTION site: after a throw
     * *out_enabled stays 0 (false), as CallBooleanMethod answered after a throw, and the C continues.
     * Only called when the window still had a peer (the C checked jwindow).
     */
    int32_t (*is_enabled)(int64_t window_id, int32_t* out_enabled);

    /*
     * GtkWindow.notifyStateChanged(int) - WindowEvent.MINIMIZE / MAXIMIZE / RESTORE. Replaces the call in
     * WindowContextBase::notify_state (from process_state and WindowContextTop::work_around_compiz_state).
     * CHECK_JNI_EXCEPTION site (it is the last statement).
     */
    int32_t (*notify_state_changed)(int64_t window_id, int32_t state);

    /*
     * Window.notifyFocus(int) - WindowEvent.FOCUS_GAINED / FOCUS_LOST. Replaces the calls in
     * WindowContextBase::process_focus and WindowContextTop::set_visible (the latter INSIDE
     * GtkWindow.setVisibleImpl). CHECK_JNI_EXCEPTION sites.
     */
    int32_t (*notify_focus)(int64_t window_id, int32_t event);

    /* Window.notifyFocusDisabled(). WindowContextBase::process_focus. CHECK_JNI_EXCEPTION site. */
    int32_t (*notify_focus_disabled)(int64_t window_id);

    /*
     * Window.notifyFocusUngrab(). Replaces the calls in WindowContextBase::ungrab_focus (reached from
     * GtkWindow._ungrabFocus, _close, focus-out and a press outside a grab; CHECK_JNI_EXCEPTION site) and
     * the two in WindowContextTop::process_mouse_button for EXTENDED windows before a resize or move drag.
     * Those two did NOT check: the JNI left the exception pending while GTK started the drag; the C
     * ignores the status there and continues into gtk_window_begin_resize_drag / begin_move_drag.
     */
    int32_t (*notify_focus_ungrab)(int64_t window_id);

    /*
     * Window.notifyDestroy(). Replaces the call in WindowContextBase::process_destroy, reached from
     * GtkWindow._close (INSIDE that downcall), from a GDK_DESTROY event, and recursively for every child
     * window of a destroyed owner. EXCEPTION_OCCURED site: the C continues, clears the window's view id
     * and window id, and the window gets no further window or view upcall with a non-zero id. This is
     * where the Java registry entry should go.
     */
    int32_t (*notify_destroy)(int64_t window_id);

    /*
     * Window.notifyClose(). WindowContextBase::process_delete (GDK_DELETE, only when is_enabled answered
     * true). CHECK_JNI_EXCEPTION site.
     */
    int32_t (*notify_close)(int64_t window_id);

    /*
     * Window.notifyResize(int, int, int) - WindowEvent.RESIZE / MAXIMIZE, width and height of the whole
     * window including the frame extents. Replaces the calls in WindowContextTop::process_configure and
     * WindowContextTop::notify_window_resize (from set_bounds, INSIDE GtkWindow._setBounds and
     * setVisibleImpl, and from update_frame_extents). CHECK_JNI_EXCEPTION sites: in process_configure a
     * throw skips the view resize, the geometry update and the move / screen notifications that follow; in
     * notify_window_resize it skips the view resize only (set_bounds then goes on to notify_move).
     * notify_window_resize did not check jwindow: it can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_resize)(int64_t window_id, int32_t type, int32_t width, int32_t height);

    /*
     * Window.notifyMove(int, int). WindowContextTop::notify_window_move (from process_configure and
     * set_bounds). CHECK_JNI_EXCEPTION site: a throw skips the view's ViewEvent.MOVE.
     */
    int32_t (*notify_move)(int64_t window_id, int32_t x, int32_t y);

    /*
     * Window.notifyMoveToAnotherScreen(Screen) with the Screen of monitor `monitor_index`
     * (gdk_screen_get_monitor_at_point of the window origin), for WindowContextTop::process_configure.
     * The JNI built that Screen in C (glass_screen.cpp createJavaScreen, gone with that JNI); Java builds it
     * for that monitor index with its own copy of that arithmetic. CHECK_JNI_EXCEPTION site: on
     * GGTK_UPCALL_THREW the C returns before recording the new monitor, so the next configure event
     * notifies again. (When createJavaScreen's Screen constructor threw, the JNI reported it, left a
     * NullPointerException pending from jni_exception and passed a null Screen; that path ended in the same
     * early return.)
     */
    int32_t (*notify_move_to_another_screen)(int64_t window_id, int32_t monitor_index);

    /*
     * Window.notifyLevelChanged(int) - Window.Level.FLOATING / NORMAL. WindowContextTop::notify_on_top
     * (GDK_WINDOW_STATE with ABOVE changed, or the Compiz _NET_WM_STATE workaround).
     * CHECK_JNI_EXCEPTION site.
     */
    int32_t (*notify_level_changed)(int64_t window_id, int32_t level);

    /*
     * QUERY. GtkWindow.nonClientHitTest(int, int) -> GGTK_HT_UNSPECIFIED / GGTK_HT_CAPTION / GGTK_HT_CLIENT,
     * for EXTENDED windows. Writes the int the Java method returned to *out_result, unmodified; the C then
     * truncates it to a jboolean, as CallBooleanMethod on this (II)I method did (no GGTK_HT_* value changes).
     * After a throw *out_result stays 0 (GGTK_HT_UNSPECIFIED). Three sites: the double-click maximize in
     * WindowContextTop::process_mouse_button (CHECK_JNI_EXCEPTION: returns), the button-1 press there and
     * the resize-border motion in WindowContextTop::process_mouse_motion - these two did NOT check (the
     * JNI continued into the drag / cursor code with the exception pending); the C ignores the status
     * there. process_mouse_motion did not check jwindow: it can pass id 0 (see IDENTITY).
     */
    int32_t (*non_client_hit_test)(int64_t window_id, int32_t x, int32_t y, int32_t* out_result);
} GgtkWindowCallbacks;   /* 12 pointers */

/*
 * ---- GgtkViewCallbacks: View / GtkView targets, keyed by the view id ----
 *
 * Boolean arguments are int32_t 0 / 1. All constants are the Java ones the JNI passed
 * (com.sun.glass.events.MouseEvent, KeyEvent, ViewEvent, com.sun.glass.ui.View.IME_ATTR_*). GtkView
 * overrides notifyMenu, so the Java side must dispatch virtually through the View.
 */
typedef struct GgtkViewCallbacks {
    /*
     * View.notifyView(int) - ViewEvent.ADD / REMOVE (GtkView._setParent), FULLSCREEN_ENTER
     * (GtkView._enterFullscreen), FULLSCREEN_EXIT (GtkView._exitFullscreen) - all INSIDE those downcalls,
     * GlassView.cpp - and MOVE (WindowContextTop::notify_window_move, per configure event).
     * CHECK_JNI_EXCEPTION sites; after a throw _enterFullscreen answers false.
     */
    int32_t (*notify_view)(int64_t view_id, int32_t type);

    /*
     * View.notifyResize(int, int) - content width / height. WindowContextTop::process_configure and
     * WindowContextTop::notify_view_resize (from GtkWindow._updateViewSize and set_bounds).
     * CHECK_JNI_EXCEPTION sites.
     */
    int32_t (*notify_resize)(int64_t view_id, int32_t width, int32_t height);

    /*
     * View.notifyRepaint(int, int, int, int). WindowContextBase::process_expose (GDK_EXPOSE / GDK_DAMAGE
     * area) and WindowContextBase::notify_state (0, 0, width, height on RESTORE). CHECK_JNI_EXCEPTION
     * sites; after a throw notify_state skips notify_state_changed.
     */
    int32_t (*notify_repaint)(int64_t view_id, int32_t x, int32_t y, int32_t width, int32_t height);

    /*
     * HOT. View.notifyMouse(int, int, int, int, int, int, int, boolean, boolean) - type, button,
     * x, y, x_abs, y_abs (truncated GdkEvent doubles), modifiers, popup trigger, synthesized. One per
     * motion event. Sites: WindowContextBase::process_mouse_button, process_mouse_motion,
     * process_mouse_cross, set_visible (EXIT when hiding, INSIDE setVisibleImpl), set_view (EXIT to the
     * old view, INSIDE GtkWindow._setView), and the EXTENDED-window ENTER / EXIT of
     * WindowContextTop::process_mouse_cross and process_mouse_motion. All CHECK_JNI_EXCEPTION sites except
     * set_view: it did NOT check, the JNI left the exception pending and it was thrown out of
     * GtkWindow._setView to its Java caller after the view references were swapped. The C ignores the
     * status there; to keep that behaviour the Java side must hold the Throwable instead of reporting it
     * and rethrow it when _setView returns. The EXTENDED process_mouse_cross / process_mouse_motion
     * sites did not check jview: they can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_mouse)(int64_t view_id, int32_t type, int32_t button, int32_t x, int32_t y,
                            int32_t x_abs, int32_t y_abs, int32_t modifiers, int32_t is_popup_trigger,
                            int32_t is_synthesized);

    /*
     * View.notifyMenu(int, int, int, int, boolean). WindowContextBase::process_mouse_button, after the
     * DOWN of button 3 (skipped when that notify_mouse threw). CHECK_JNI_EXCEPTION site.
     */
    int32_t (*notify_menu)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                           int32_t is_keyboard_trigger);

    /*
     * View.notifyScroll(int, int, int, int, double, double, int, int, int, int, int, double, double).
     * WindowContextBase::process_mouse_scroll. lines, chars, default_lines and default_chars are the
     * literals 0 and x_multiplier / y_multiplier 40.0, as the JNI passed them. CHECK_JNI_EXCEPTION site.
     */
    int32_t (*notify_scroll)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                             double delta_x, double delta_y, int32_t modifiers, int32_t lines, int32_t chars,
                             int32_t default_lines, int32_t default_chars, double x_multiplier,
                             double y_multiplier);

    /*
     * View.notifyKey(int, int, char[], int) - KeyEvent.PRESS / RELEASE, then TYPED for a press with a
     * character. WindowContextBase::process_key. key_chars holds key_char_count (0 or 1) UTF-16 code
     * units: gdk_keyval_to_unicode truncated to one jchar (code points above U+FFFF lose their high bits),
     * control letters mapped to 1..26 - exactly the char[] the JNI built. Java must build a char[] of
     * that length, never null (key_chars may be NULL when the count is 0). The JNI passed the SAME
     * char[] object to the PRESS and the TYPED call. CHECK_JNI_EXCEPTION sites: a throw of the PRESS
     * skips the TYPED.
     */
    int32_t (*notify_key)(int64_t view_id, int32_t type, int32_t key_code, const uint16_t* key_chars,
                          int32_t key_char_count, int32_t modifiers);

    /*
     * GtkView.notifyInputMethodLinux(String, int, int, byte) for the GtkIMContext "preedit-changed"
     * signal (glass_window_ime.cpp on_preedit_changed): Java calls it with (text, 0, cursor_pos,
     * (byte) attr). text / text_len are the preedit string of gtk_im_context_get_preedit_string, the
     * bytes the JNI handed to NewStringUTF (decode with JniStringCodec.fromNewStringUtf); text NULL means
     * a null String. attr is View.IME_ATTR_INPUT / TARGET_NOTCONVERTED / CONVERTED. The C calls this AFTER
     * notify_input_method_candidate_pos_request, as the JNI did. LOG_EXCEPTION site. on_preedit_changed
     * did not check jview: it can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_input_method_preedit)(int64_t view_id, const char* text, int32_t text_len,
                                           int32_t cursor_pos, int32_t attr);

    /*
     * GtkView.notifyInputMethodLinux(String, int, int, byte) for a committed string
     * (WindowContextBase::commitIME, from the "commit" signal while pre-editing or outside a key event).
     * Java decodes text / text_len as NewStringUTF did, takes n = the UTF-16 length of the RESULT (the
     * JNI's GetStringLength of that jstring) and calls it with (text, n, n, (byte) 0). LOG_EXCEPTION
     * site. commitIME did not check jview: it can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_input_method_commit)(int64_t view_id, const char* text, int32_t text_len);

    /*
     * QUERY. GtkView.notifyInputMethodCandidateRelativePosRequest(int) -> double[]. Replaces
     * WindowContextBase::updateCaretPos (from on_preedit_changed; offset is always 0). When the method
     * returned an array, Java writes its elements 0 and 1 to out_xy[0] / out_xy[1] and 1 to *out_valid;
     * the C then passes ((int) x, (int) y, 0, 0) to gtk_im_context_set_cursor_location. With *out_valid 0
     * (the C's initial value) the C skips that call. The JNI did not check this call at all: a throw, or a
     * null array, made its GetDoubleArrayElements(NULL) crash the JVM; the slot reports the throw and
     * skips the cursor update instead. It did not check jview either: it can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_input_method_candidate_pos_request)(int64_t view_id, int32_t offset, double* out_xy,
                                                         int32_t* out_valid);
} GgtkViewCallbacks;   /* 10 pointers */

/*
 * ---- GgtkDndData: one value of the drag source's data map, converted as the C used it ----
 *
 * The C allocates the struct, zero-fills it and passes it to GgtkDndCallbacks.source_get_data.
 */
typedef struct GgtkDndData {
    int32_t kind;    /* GGTK_DND_DATA_* */
    int32_t count;   /* STRING: byte length without the NUL; BYTES: byte count; STRINGS: number of strings */
    void*   data;    /* STRING / BYTES / STRINGS: a g_malloc'd block the C owns and g_frees; on GGTK_UPCALL_OK
                      * never NULL for those kinds (at least one byte even when count is 0); NULL when the
                      * conversion threw (GGTK_UPCALL_THREW with kind BYTES) and for the other kinds */
    void*   pixbuf;  /* PIXBUF: the GdkPixbuf* Pixels.attachData wrote here (a new reference the C unrefs) */
} GgtkDndData;   /* 24 bytes on LP64: kind 0, count 4, data 8, pixbuf 16 */

/* GgtkDndData.kind */
#define GGTK_DND_DATA_NONE    0   /* no value: the key is absent, maps to null, Map.get threw, or the value
                                   * is not of a type the requested conversion accepts */
#define GGTK_DND_DATA_STRING  1   /* a String: modified UTF-8, NUL terminated (GetStringUTFChars) */
#define GGTK_DND_DATA_BYTES   2   /* a ByteBuffer: the WHOLE backing array of ByteBuffer.array(), position
                                   * and limit ignored */
#define GGTK_DND_DATA_PIXBUF  3   /* a Pixels: Pixels.attachData(address of GgtkDndData.pixbuf) was called */
#define GGTK_DND_DATA_STRINGS 4   /* a String[]: count strings, each modified UTF-8 and NUL terminated, back to
                                   * back in data */

/* The conversion source_get_data applies - what the C did with the jobject Map.get returned. */
#define GGTK_DND_AS_STRING  1   /* GetStringUTFChars: String -> STRING */
#define GGTK_DND_AS_BYTES   2   /* ByteBuffer.array() + GetByteArrayElements: ByteBuffer -> BYTES */
#define GGTK_DND_AS_PIXBUF  3   /* Pixels.attachData: Pixels -> PIXBUF */
#define GGTK_DND_AS_STRINGS 4   /* GetArrayLength + GetObjectArrayElement + GetStringUTFChars: String[] -> STRINGS */
#define GGTK_DND_AS_RAW     5   /* IsInstanceOf String -> STRING, else IsInstanceOf ByteBuffer -> BYTES */

/*
 * ---- GgtkDndCallbacks: the drop target's View targets (keyed by the view id) and the drag source's data ----
 *
 * Actions are com.sun.glass.ui.Clipboard.ACTION_* bit sets, translated from and to GdkDragAction by
 * glass_dnd.cpp exactly as before.
 */
typedef struct GgtkDndCallbacks {
    /*
     * QUERY. View.notifyDragEnter(int, int, int, int, int) -> int, for the first GDK_DRAG_MOTION after a
     * GDK_DRAG_ENTER (glass_dnd.cpp process_dnd_target_drag_motion). x / y are relative to the window
     * origin at the enter, x_abs / y_abs the truncated root coordinates, recommended_action the suggested
     * action. Writes the returned action to *out_action (initialised to 0). CHECK_JNI_EXCEPTION site: a
     * throw returns WITHOUT answering gdk_drag_status and keeps the drag in the "just entered" state.
     * process_dnd_target_drag_motion did not check jview: it can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_drag_enter)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                                 int32_t recommended_action, int32_t* out_action);

    /* QUERY. View.notifyDragOver(int, int, int, int, int) -> int, for every later GDK_DRAG_MOTION. As enter. */
    int32_t (*notify_drag_over)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                                int32_t recommended_action, int32_t* out_action);

    /*
     * View.notifyDragDrop(int, int, int, int, int) (its int result was discarded), for GDK_DROP_START
     * (process_dnd_target_drop_start); recommended_action is the SELECTED action. The drop target's
     * GtkDnDClipboard reads (mimesFromSystem, popFromSystem) run nested main loops inside this slot.
     * LOG_EXCEPTION site: the drop is still finished and replied TRUE. Can pass id 0 (see IDENTITY).
     */
    int32_t (*notify_drag_drop)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                                int32_t recommended_action);

    /*
     * View.notifyDragLeave(), for GDK_DRAG_LEAVE unless a drop is in progress
     * (process_dnd_target_drag_leave). CHECK_JNI_EXCEPTION site (the last statement). Can pass id 0.
     */
    int32_t (*notify_drag_leave)(int64_t view_id);

    /*
     * The drag SOURCE's data: the value the data map of the drag in progress (the HashMap
     * GtkDnDClipboard.pushToSystemImpl was called with; there is at most one drag at a time) holds for
     * `mime`, converted as `as` says. Replaces glass_dnd.cpp dnd_source_get_data (Map.get on that map)
     * together with what its callers did with the value: dnd_source_set_string (AS_STRING "text/plain"),
     * dnd_source_set_image (AS_PIXBUF "application/x-java-rawimage"), dnd_source_set_uri (AS_STRING
     * "text/uri-list", then AS_STRINGS "application/x-java-file-list"), dnd_source_set_raw (AS_RAW, the
     * target's atom name) - all from the "drag-data-get" signal - and DragView::get_drag_image_offset
     * (AS_BYTES "application/x-java-drag-image-offset"), DragView::get_drag_image (AS_BYTES
     * "application/x-java-drag-image", then AS_PIXBUF "application/x-java-rawimage") from "drag-begin".
     * All run INSIDE pushToSystemImpl (gtk_drag_begin and the drag's nested main loop).
     * mime / mime_len are the bytes the JNI handed to NewStringUTF for the key (decode with
     * JniStringCodec.fromNewStringUtf); mime NULL means a null key.
     * Status: the JNI reported a throw of Map.get and answered "no value" (EXCEPTION_OCCURED), and
     * reported a throw of the conversion (ByteBuffer.array(), Pixels.attachData) too. So:
     *   - Map.get threw: report, kind NONE, return GGTK_UPCALL_THREW;
     *   - no value, or a value of another type than `as` accepts: kind NONE, GGTK_UPCALL_OK (the JNI
     *     applied the conversion to whatever it got - a null check only, otherwise undefined behaviour);
     *   - the conversion threw: report, set kind to the value's kind (BYTES / PIXBUF) with data NULL,
     *     return GGTK_UPCALL_THREW - the C distinguishes this from "no value": after a throwing
     *     attachData dnd_source_set_image still unrefs the (NULL) pixbuf and get_drag_image gives up
     *     instead of trying the next source.
     * For AS_PIXBUF Java passes the ADDRESS of out->pixbuf to Pixels.attachData, as the JNI passed the
     * address of its local, so a pixbuf written before a throw is what the C sees.
     * AS_STRINGS: a null element of the String[] has no representation (the JNI crashed on it in
     * GetStringUTFChars).
     */
    int32_t (*source_get_data)(const char* mime, int32_t mime_len, int32_t as, GgtkDndData* out);
} GgtkDndCallbacks;   /* 5 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GgtkAppCallbacks) == 2 * sizeof(void*), "GgtkAppCallbacks must be 2 pointers");
static_assert(sizeof(GgtkWindowCallbacks) == 12 * sizeof(void*), "GgtkWindowCallbacks must be 12 pointers");
static_assert(sizeof(GgtkViewCallbacks) == 10 * sizeof(void*), "GgtkViewCallbacks must be 10 pointers");
static_assert(sizeof(GgtkDndCallbacks) == 5 * sizeof(void*), "GgtkDndCallbacks must be 5 pointers");
static_assert(sizeof(GgtkDndData) == 8 + 2 * sizeof(void*), "GgtkDndData must be 2 int32 + 2 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GgtkAppCallbacks) == 2 * sizeof(void*), "GgtkAppCallbacks must be 2 pointers");
_Static_assert(sizeof(GgtkWindowCallbacks) == 12 * sizeof(void*), "GgtkWindowCallbacks must be 12 pointers");
_Static_assert(sizeof(GgtkViewCallbacks) == 10 * sizeof(void*), "GgtkViewCallbacks must be 10 pointers");
_Static_assert(sizeof(GgtkDndCallbacks) == 5 * sizeof(void*), "GgtkDndCallbacks must be 5 pointers");
_Static_assert(sizeof(GgtkDndData) == 8 + 2 * sizeof(void*), "GgtkDndData must be 2 int32 + 2 pointers");
#endif

/*
 * sizeof probes. They RETURN the real sizeof and only DOCUMENT today's value, so a layout that drifts
 * from Java's MemoryLayout fails a test instead of shifting every slot.
 */
GGTK_EXPORT int32_t ggtk_sizeof_app_callbacks(void);      /* ==  2 * sizeof(void*) today */
GGTK_EXPORT int32_t ggtk_sizeof_window_callbacks(void);   /* == 12 * sizeof(void*) today */
GGTK_EXPORT int32_t ggtk_sizeof_view_callbacks(void);     /* == 10 * sizeof(void*) today */
GGTK_EXPORT int32_t ggtk_sizeof_dnd_callbacks(void);      /* ==  5 * sizeof(void*) today */
GGTK_EXPORT int32_t ggtk_sizeof_dnd_data(void);           /* == 24 on LP64 today */

/*
 * Install the table (cb == NULL clears it; see INSTALLATION). A NULL slot makes its call sites skip the call
 * (see NULL SLOTS). Always GGTK_OK.
 */
GGTK_EXPORT int32_t ggtk_app_set_callbacks(const GgtkAppCallbacks* cb);
GGTK_EXPORT int32_t ggtk_window_set_callbacks(const GgtkWindowCallbacks* cb);
GGTK_EXPORT int32_t ggtk_view_set_callbacks(const GgtkViewCallbacks* cb);
GGTK_EXPORT int32_t ggtk_dnd_set_callbacks(const GgtkDndCallbacks* cb);

/*
 * SHIM ONLY (see SHIM-ONLY TEST HOOKS). The window's id (the one ggtk_window_create was given), 0 after its
 * destruction; 0 for NULL.
 */
GGTK_EXPORT int64_t ggtk_window_get_id(ggtk_window_t window);

/*
 * SHIM ONLY (see SHIM-ONLY TEST HOOKS). The id of the view the window currently holds (what its view slots
 * receive): the id of the GlassView last passed to ggtk_window_set_view, 0 when it holds none, when that view
 * has id 0, or after the window's destruction; 0 for NULL. ggtk_window_set_view answers the same id without a
 * second read of the window.
 */
GGTK_EXPORT int64_t ggtk_window_get_view_id(ggtk_window_t window);

/* SHIM ONLY (see SHIM-ONLY TEST HOOKS). The view's id (the one ggtk_view_create was given); 0 for NULL. */
GGTK_EXPORT int64_t ggtk_view_get_id(ggtk_view_t view);

/*
 * TEST HOOK, SHIM ONLY (see SHIM-ONLY TEST HOOKS), unconditionally exported, never called by production
 * code. Dials slot `slot` of the INSTALLED table with a fixed argument pattern and returns the slot's
 * status - GGTK_ERR_INVALID_ARG for
 * an unknown slot number, GGTK_ERR_NOT_INSTALLED when that slot is NULL. It exists because a sizeof probe
 * cannot catch a FunctionDescriptor whose parameter list disagrees with the prototype (a table stores
 * only pointers), and because several slots cannot be reached on a test display (a second monitor, a
 * throwing Screen constructor, every DnD source conversion). `id` is passed through as the window / view
 * id; out / out2 are the caller's out-parameters, passed through unchanged. Strings and arrays below are
 * static data valid during the call.
 *     0 notify_screen_settings_changed()
 *     1 get_application_name(out)                                  out: char** (g_free the result)
 *   100 is_enabled(id, out)                                         out: int32_t*
 *   101 notify_state_changed(id, 1001)
 *   102 notify_focus(id, 1001)
 *   103 notify_focus_disabled(id)
 *   104 notify_focus_ungrab(id)
 *   105 notify_destroy(id)
 *   106 notify_close(id)
 *   107 notify_resize(id, 1001, 1002, 1003)
 *   108 notify_move(id, 1001, 1002)
 *   109 notify_move_to_another_screen(id, 1001)
 *   110 notify_level_changed(id, 1001)
 *   111 non_client_hit_test(id, 1001, 1002, out)                    out: int32_t*
 *   200 notify_view(id, 1001)
 *   201 notify_resize(id, 1001, 1002)
 *   202 notify_repaint(id, 1001, 1002, 1003, 1004)
 *   203 notify_mouse(id, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1, 0)
 *   204 notify_menu(id, 1001, 1002, 1003, 1004, 1)
 *   205 notify_scroll(id, 1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007, 1008, 1009, 3.5, 4.5)
 *   206 notify_key(id, 1001, 1002, {0x0041, 0xD800}, 2, 1003)      a lone high surrogate as 2nd unit
 *   207 notify_input_method_preedit(id, TEXT, 8, 1001, 1002)
 *   208 notify_input_method_commit(id, TEXT, 8)
 *       TEXT = the 8 bytes 41 C3 A9 F0 9F 98 80 5A ("A", U+00E9 and U+1F600 in standard UTF-8, "Z"),
 *       a NUL follows but is not counted
 *   209 notify_input_method_candidate_pos_request(id, 1001, out, out2)   out: double[2], out2: int32_t*
 *   300 notify_drag_enter(id, 1001, 1002, 1003, 1004, 1005, out)    out: int32_t*
 *   301 notify_drag_over(id, 1001, 1002, 1003, 1004, 1005, out)     out: int32_t*
 *   302 notify_drag_drop(id, 1001, 1002, 1003, 1004, 1005)
 *   303 notify_drag_leave(id)
 *   304 source_get_data("text/plain", 10, (int32_t) id, out)        out: GgtkDndData* - `id` carries `as`
 */
GGTK_EXPORT int32_t ggtk_test_fire_callback(int32_t slot, int64_t id, void* out, void* out2);

/*
 * ==== THE NATIVES ====
 *
 * One function per former JNI native of GtkApplication (4 of its 6), GtkWindow (29), GtkView (11 of its 13)
 * and GtkDnDClipboard (5 of its 6) that needs the C of this library. Each is the body of the
 * Java_com_sun_glass_ui_gtk_* function named at it, as of commit 033187ad90; that JNI function is gone and
 * GtkGlassNative binds this one in its place. The other five JNI functions are gone without a function here,
 * because nothing they did needs the C of this library, and Java does it: GtkApplication._queryLibrary chose
 * the glass GTK library, and the query is GtkGlassNative's (launcher.c, the whole of libglass.so, and the copy
 * of it in GlassApplication.cpp, are deleted with it); GtkApplication._terminateLoop called gtk_main_quit(),
 * which GtkGlassNative binds from libgtk-3 directly, and then deleted the PlatformSupport, which is Java's now;
 * GtkView._getNativeView answered 0; GtkView._scheduleRepaint ("Seems to be unused") and
 * GtkDnDClipboard.pushTargetActionToSystem ("Never called") did nothing.
 * Boolean arguments and results are int32_t: an argument is true when non-zero (the JNI's jboolean tests), a
 * result is 1 / 0. A ggtk_window_t / ggtk_view_t is the value ggtk_window_create / ggtk_view_create
 * returned, i.e. Window.ptr / View.ptr; unless a function says otherwise it is not checked for NULL, as the
 * JNI did not check it. See THE NATIVES' FUNCTIONS above for exceptions, UPCALLS and critical(true).
 */

/* ---- GtkApplication (GlassApplication.cpp, glass_key.cpp) ---- */

/*
 * GtkApplication._initGTK(int version, boolean verbose, float overrideUIScale)
 * (Java_com_sun_glass_ui_gtk_GtkApplication__1initGTK). Stores the UI-scale override and the verbose flag,
 * initialises GLib threads, ENTERS the GDK lock (gdk_threads_enter; ggtk_application_run_loop leaves it) and
 * runs gtk_init. Runs on the thread that constructs the GtkApplication, before the toolkit thread exists.
 * Then, when `version` is 3 and GTK is older than the build's minimum, returns GGTK_ERR_GTK_VERSION with
 * *out_error = the message of the JNI's UnsupportedOperationException ("Minimum GTK version required is
 * 3.m.n. System has a.b.c.", ASCII, NUL-terminated, allocated with g_malloc; the caller releases it with
 * g_free): Java throws UnsupportedOperationException(message) when the call returns, where the JNI native
 * returned with it pending. Otherwise GGTK_OK and *out_error NULL. out_error may be NULL (the message is
 * dropped). The JNI native first called ExceptionClear, which never had anything to clear.
 */
GGTK_EXPORT int32_t ggtk_application_init_gtk(int32_t version, int32_t verbose, float ui_scale,
                                              char** out_error);

/*
 * GtkApplication._init(long eventProc, boolean disableGrab) (Java_com_sun_glass_ui_gtk_GtkApplication__1init)
 * without its first statement, which stored the JNIEnv of the JNI upcalls (mainEnv). Installs GDK's event
 * handler (event_handler: the embedder's GdkEventFunc from javafx.embed.eventProc, called for the events of
 * windows that are not Glass windows; 0 for none), connects the screen signals, selects PropertyNotify on the
 * root window and sets IBUS_ENABLE_SYNC_MODE. On the toolkit thread. From here on events reach the tables.
 */
GGTK_EXPORT void ggtk_application_init(int64_t event_handler, int32_t disable_grab);

/*
 * GtkApplication._runLoop(Runnable launchable, boolean noErrorTrap)
 * (Java_com_sun_glass_ui_gtk_GtkApplication__1runLoop) AFTER its first two statements, which the caller
 * performs: the JNI ran launchable.run() before anything else and, when it threw, reported the Throwable as
 * check_and_clear_exception did (Application.reportException, a throw of that swallowed) and returned
 * WITHOUT calling this. Pushes a GDK error trap unless no_error_trap, runs gtk_main() until
 * GtkApplication._terminateLoop quits it (gtk_main_quit), then leaves the GDK lock. On the toolkit thread;
 * BLOCKS for the life of the toolkit and UPCALLS (every event).
 */
GGTK_EXPORT void ggtk_application_run_loop(int32_t no_error_trap);

/*
 * GtkApplication._getKeyCodeForChar(char c, int hint)
 * (Java_com_sun_glass_ui_gtk_GtkApplication__1getKeyCodeForChar, glass_key.cpp): the KeyEvent.VK_* code for
 * the UTF-16 code unit `character` (a lone surrogate answers VK_UNDEFINED: g_utf16_to_ucs4 rejects it).
 * `hint` is ignored, as the JNI ignored it.
 */
GGTK_EXPORT int32_t ggtk_application_get_key_code_for_char(uint16_t character, int32_t hint);

/* ---- GtkWindow (GlassWindow.cpp; the C is WindowContextTop of glass_window.cpp) ---- */

/*
 * GtkWindow._createWindow(long ownerPtr, long screenPtr, int mask)
 * (Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow): new WindowContextTop. owner: the owner window or
 * NULL. screen: the native screen of the Window's Screen (a monitor index), which process_configure compares
 * with the monitor of the window origin. mask: the com.sun.glass.ui.Window style bits GtkWindow passed
 * (the TRANSPARENT / TITLED / EXTENDED / POPUP / UTILITY / CLOSABLE / MAXIMIZABLE / MINIMIZABLE constants of
 * com_sun_glass_ui_Window.h). visual_id: GtkApplication.visualID, which the JNI read inside the
 * constructor with GetStaticLongField (0: GTK's visual). window_id: the id this window's slots receive (see
 * IDENTITY). UPCALLS GgtkAppCallbacks.get_application_name (WM_CLASS),
 * and no window slot. Never NULL.
 */
GGTK_EXPORT ggtk_window_t ggtk_window_create(ggtk_window_t owner, int64_t screen, int32_t mask,
                                             int64_t visual_id, int64_t window_id);

/*
 * GtkWindow._close (Java_com_sun_glass_ui_gtk_GtkWindow__1close): destroy_and_delete_ctx - process_destroy
 * (the child windows first, then notify_destroy of this one and its ids cleared), then the WindowContext is
 * deleted, or after the event of it being processed. `window` is DANGLING on return. Returns 1 (the JNI's
 * JNI_TRUE, "return value not used"). UPCALLS.
 */
GGTK_EXPORT int32_t ggtk_window_close(ggtk_window_t window);

/*
 * GtkWindow._setView(long ptr, View view) (Java_com_sun_glass_ui_gtk_GtkWindow__1setView):
 * WindowContextBase::set_view. `view` is the GlassView of the View (View.ptr), NULL for a null View. A View
 * whose GlassView is already gone (View.ptr 0) passes NULL too, so its window then has no view peer, where
 * the JNI kept a global reference to that closed View (not reachable through Quantum). If the window held a
 * view, that view gets notify_mouse(EXIT) first - the one UNCHECKED site here: the JNI left an exception of
 * that EXIT pending and _setView threw it to its Java caller, so Java must hold that Throwable and rethrow it
 * when this returns (see GgtkViewCallbacks.notify_mouse). Then the window stores the new view's id, which is
 * also written to *out_view_id (0 for none) when out_view_id is not NULL, so that the caller need not read the
 * window again - the EXIT handler may have closed it. Returns 1 (set_view's TRUE). UPCALLS.
 */
GGTK_EXPORT int32_t ggtk_window_set_view(ggtk_window_t window, ggtk_view_t view, int64_t* out_view_id);

/*
 * GtkWindow._updateViewSize (Java_com_sun_glass_ui_gtk_GtkWindow__1updateViewSize):
 * WindowContextTop::update_view_size - the view's notify_resize when the size is window-oriented. UPCALLS.
 */
GGTK_EXPORT void ggtk_window_update_view_size(ggtk_window_t window);

/* GtkWindow.minimizeImpl (Java_com_sun_glass_ui_gtk_GtkWindow_minimizeImpl): set_minimized (gtk_window_iconify /
 * gtk_window_deiconify and gdk_window_focus). */
GGTK_EXPORT void ggtk_window_minimize(ggtk_window_t window, int32_t minimize);

/* GtkWindow.maximizeImpl (Java_com_sun_glass_ui_gtk_GtkWindow_maximizeImpl): set_maximized; was_maximized is
 * ignored, as the JNI ignored it. */
GGTK_EXPORT void ggtk_window_maximize(ggtk_window_t window, int32_t maximize, int32_t was_maximized);

/*
 * GtkWindow._setBounds (Java_com_sun_glass_ui_gtk_GtkWindow__1setBounds): WindowContextTop::set_bounds - the
 * geometry, gtk_window_resize / gtk_window_set_default_size / gtk_window_move and the window and view resize
 * and move notifications. UPCALLS.
 */
GGTK_EXPORT void ggtk_window_set_bounds(ggtk_window_t window, int32_t x, int32_t y, int32_t x_set,
                                        int32_t y_set, int32_t w, int32_t h, int32_t cw, int32_t ch,
                                        float x_gravity, float y_gravity);

/*
 * GtkWindow.setVisibleImpl (Java_com_sun_glass_ui_gtk_GtkWindow_setVisibleImpl): WindowContextTop::set_visible
 * - show or hide (the view's EXIT when hiding an entered window), the default bounds on the first show, and
 * notify_focus(FOCUS_GAINED) when shown and enabled. UPCALLS.
 */
GGTK_EXPORT void ggtk_window_set_visible(ggtk_window_t window, int32_t visible);

/* GtkWindow._setResizable (Java_com_sun_glass_ui_gtk_GtkWindow__1setResizable): set_resizable and the geometry
 * hints. Returns 1. */
GGTK_EXPORT int32_t ggtk_window_set_resizable(ggtk_window_t window, int32_t resizable);

/* GtkWindow._requestFocus (Java_com_sun_glass_ui_gtk_GtkWindow__1requestFocus): gtk_window_present when
 * visible; `event` is ignored, as the JNI ignored it. Returns 1 ("not used"). */
GGTK_EXPORT int32_t ggtk_window_request_focus(ggtk_window_t window, int32_t event);

/* GtkWindow._setFocusable (Java_com_sun_glass_ui_gtk_GtkWindow__1setFocusable): gtk_window_set_accept_focus. */
GGTK_EXPORT void ggtk_window_set_focusable(ggtk_window_t window, int32_t focusable);

/* GtkWindow._grabFocus (Java_com_sun_glass_ui_gtk_GtkWindow__1grabFocus): WindowContextBase::grab_focus, a
 * pointer grab unless a mouse drag holds one; 1 when this window now holds the grab. */
GGTK_EXPORT int32_t ggtk_window_grab_focus(ggtk_window_t window);

/* GtkWindow._ungrabFocus (Java_com_sun_glass_ui_gtk_GtkWindow__1ungrabFocus): WindowContextBase::ungrab_focus,
 * which releases the grab and calls notify_focus_ungrab. UPCALLS. */
GGTK_EXPORT void ggtk_window_ungrab_focus(ggtk_window_t window);

/*
 * GtkWindow._setTitle(long ptr, String title) (Java_com_sun_glass_ui_gtk_GtkWindow__1setTitle). `title` is
 * the String's UTF-16 code units as GetStringChars gave them, lone surrogates included, title_len of them,
 * borrowed for the call; NULL for a null String (an empty String needs a non-NULL pointer). The C converts
 * them to standard UTF-8 with g_utf16_to_utf8 as glass_general.cpp jstring_to_utf8 did - text GLib rejects,
 * such as a lone surrogate, gives a NULL title - and calls gtk_window_set_title. Returns 1.
 */
GGTK_EXPORT int32_t ggtk_window_set_title(ggtk_window_t window, const uint16_t* title, int32_t title_len);

/* GtkWindow._setLevel (Java_com_sun_glass_ui_gtk_GtkWindow__1setLevel): WindowContextTop::set_level
 * (Window.Level NORMAL / FLOATING / TOPMOST) and the keep-above state of the owned windows. */
GGTK_EXPORT void ggtk_window_set_level(ggtk_window_t window, int32_t level);

/* GtkWindow._setAlpha (Java_com_sun_glass_ui_gtk_GtkWindow__1setAlpha): gtk_window_set_opacity. */
GGTK_EXPORT void ggtk_window_set_alpha(ggtk_window_t window, float alpha);

/* GtkWindow._setBackground (Java_com_sun_glass_ui_gtk_GtkWindow__1setBackground):
 * gtk_widget_override_background_color with alpha 1. Returns 1. */
GGTK_EXPORT int32_t ggtk_window_set_background(ggtk_window_t window, float r, float g, float b);

/* GtkWindow._setEnabled (Java_com_sun_glass_ui_gtk_GtkWindow__1setEnabled): set_enabled and the geometry
 * hints. */
GGTK_EXPORT void ggtk_window_set_enabled(ggtk_window_t window, int32_t enabled);

/* GtkWindow._setMinimumSize (Java_com_sun_glass_ui_gtk_GtkWindow__1setMinimumSize): 0 and nothing set for a
 * negative w or h, else set_minimum_size and 1. */
GGTK_EXPORT int32_t ggtk_window_set_minimum_size(ggtk_window_t window, int32_t w, int32_t h);

/* GtkWindow._setSystemMinimumSize (Java_com_sun_glass_ui_gtk_GtkWindow__1setSystemMinimumSize): the same for
 * the button area of EXTENDED windows (set_system_minimum_size). */
GGTK_EXPORT int32_t ggtk_window_set_system_minimum_size(ggtk_window_t window, int32_t w, int32_t h);

/* GtkWindow._setMaximumSize (Java_com_sun_glass_ui_gtk_GtkWindow__1setMaximumSize): 0 and nothing set for a zero
 * w or h; -1 means G_MAXSHORT; else set_maximum_size and 1. */
GGTK_EXPORT int32_t ggtk_window_set_maximum_size(ggtk_window_t window, int32_t w, int32_t h);

/*
 * GtkWindow._setIcon(long ptr, Pixels pixels) (Java_com_sun_glass_ui_gtk_GtkWindow__1setIcon) after its
 * Pixels.attachData, which the caller performs (it is Java: GtkPixels): `pixbuf` is the GdkPixbuf* attachData
 * wrote to the address it was given, NULL for a null Pixels or when nothing was written; attach_threw is
 * non-zero when attachData threw, after the caller reported that Throwable as check_and_clear_exception did
 * (the JNI's EXCEPTION_OCCURED). Sets the icon unless attach_threw, then releases `pixbuf` with
 * g_object_unref in either case: the reference attachData created passes to this function.
 */
GGTK_EXPORT void ggtk_window_set_icon(ggtk_window_t window, void* pixbuf, int32_t attach_threw);

/* GtkWindow._toFront (Java_com_sun_glass_ui_gtk_GtkWindow__1toFront): gdk_window_raise. */
GGTK_EXPORT void ggtk_window_to_front(ggtk_window_t window);

/* GtkWindow._toBack (Java_com_sun_glass_ui_gtk_GtkWindow__1toBack): gdk_window_lower. */
GGTK_EXPORT void ggtk_window_to_back(ggtk_window_t window);

/* GtkWindow._setCursorType (Java_com_sun_glass_ui_gtk_GtkWindow__1setCursorType): the GdkCursor of the
 * com.sun.glass.ui.Cursor type (GlassCursor.cpp get_native_cursor, a new one per call, never released - as
 * before) set with WindowContextBase::set_cursor. */
GGTK_EXPORT void ggtk_window_set_cursor_type(ggtk_window_t window, int32_t type);

/* GtkWindow._setCustomCursor (Java_com_sun_glass_ui_gtk_GtkWindow__1setCustomCursor): `cursor` is the GdkCursor*
 * of the Cursor's ptr field, which the JNI read with GetLongField (a null Cursor crashed there); borrowed. */
GGTK_EXPORT void ggtk_window_set_cursor(ggtk_window_t window, void* cursor);

/* GtkWindow._showSystemMenu (Java_com_sun_glass_ui_gtk_GtkWindow__1showSystemMenu): gdk_window_show_window_menu
 * for a synthetic button press at (x, y) of the window. */
GGTK_EXPORT void ggtk_window_show_system_menu(ggtk_window_t window, int32_t x, int32_t y);

/* GtkWindow.isVisible (Java_com_sun_glass_ui_gtk_GtkWindow_isVisible): gtk_widget_get_visible. */
GGTK_EXPORT int32_t ggtk_window_is_visible(ggtk_window_t window);

/* GtkWindow._getNativeWindowImpl (Java_com_sun_glass_ui_gtk_GtkWindow__1getNativeWindowImpl): the X11 window
 * (GDK_WINDOW_XID) of the window's GdkWindow, 0 before it is realized. */
GGTK_EXPORT int64_t ggtk_window_get_native_window(ggtk_window_t window);

/* ---- GtkView (GlassView.cpp) ---- */

/* GtkView.enableInputMethodEventsImpl (Java_com_sun_glass_ui_gtk_GtkView_enableInputMethodEventsImpl):
 * enableOrResetIME / disableIME of the view's window, nothing without one. UPCALLS: gtk_im_context_reset and
 * gtk_im_context_focus_in / focus_out can emit the input-method signals whose handlers dial the IME slots. */
GGTK_EXPORT void ggtk_view_enable_input_method_events(ggtk_view_t view, int32_t enable);

/* GtkView._create(Map caps) (Java_com_sun_glass_ui_gtk_GtkView__1create): new GlassView with Java's id (see
 * IDENTITY). The JNI ignored the caps map. Never NULL. */
GGTK_EXPORT ggtk_view_t ggtk_view_create(int64_t view_id);

/* GtkView._getX / _getY (Java_com_sun_glass_ui_gtk_GtkView__1getX / __1getY): the content offset in its window
 * (WindowGeometry view_x / view_y); 0 for a NULL view or a view without a window. */
GGTK_EXPORT int32_t ggtk_view_get_x(ggtk_view_t view);
GGTK_EXPORT int32_t ggtk_view_get_y(ggtk_view_t view);

/*
 * GtkView._setParent(long ptr, long parentPtr) (Java_com_sun_glass_ui_gtk_GtkView__1setParent): sets the
 * view's window (NULL detaches), then notify_view(ViewEvent.REMOVE) when it had a window and now has none,
 * else notify_view(ViewEvent.ADD) - also for a re-parent and for NULL to NULL. The JNI called notifyView on
 * the View the native ran for, which is not reached by an id: the slot gets the view's own id (0 when Java
 * gave it none). Without a notify_view slot it makes no call. UPCALLS.
 */
GGTK_EXPORT void ggtk_view_set_parent(ggtk_view_t view, ggtk_window_t parent);

/* GtkView._close (Java_com_sun_glass_ui_gtk_GtkView__1close): deletes the GlassView (NULL is fine); `view` is
 * DANGLING on return. Returns 1. */
GGTK_EXPORT int32_t ggtk_view_close(ggtk_view_t view);

/*
 * GtkView._uploadPixelsDirect(long viewPtr, Buffer pixels, int width, int height)
 * (Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsDirect): paints width x height pixels from `data` - cairo
 * CAIRO_FORMAT_ARGB32, stride width * 4 - into the view's window (WindowContextBase::paint); nothing for a
 * NULL view or a view without a window. `data` is the buffer's address (what GetDirectBufferAddress gave),
 * borrowed for the call and not checked. The JNI returned before anything for a null Buffer: the caller
 * skips the call then. No slot and no main-loop iteration on this path (by reading: GDK paint begin / end and
 * cairo only).
 */
GGTK_EXPORT void ggtk_view_upload_pixels_direct(ggtk_view_t view, void* data, int32_t width, int32_t height);

/*
 * GtkView._uploadPixelsIntArray(long viewPtr, int[] pixels, int offset, int width, int height)
 * (Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsIntArray). The JNI's checks in its order, each a silent
 * return: view NULL, pixels NULL (a null array), offset < 0, width or height <= 0,
 * width > (INT_MAX - offset) / height, width * height + offset > pixels_len (the length of `pixels` in ints;
 * the JNI's array length). Then, when the view has a window, paints from pixels + offset as
 * ggtk_view_upload_pixels_direct does. `pixels` is borrowed for the call and only read. No slot on this path
 * (by reading); the JNI held the array with GetPrimitiveArrayCritical around the paint. GtkGlassNative never
 * hands the C the Java array: it copies the frame alone (the width * height ints at offset) into a native block
 * of the view and passes that block with pixels_len = the frame's length and offset 0, through an ordinary
 * downcall (see critical(true) above). It copies only a frame the checks above accept, so they pass on the
 * copy exactly when they passed on the array.
 */
GGTK_EXPORT void ggtk_view_upload_pixels_int(ggtk_view_t view, const int32_t* pixels, int32_t pixels_len,
                                             int32_t offset, int32_t width, int32_t height);

/*
 * GtkView._uploadPixelsByteArray(long viewPtr, byte[] pixels, int offset, int width, int height)
 * (Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsByteArray): as ggtk_view_upload_pixels_int with bytes -
 * width > ((INT_MAX - offset) / 4) / height, 4 * width * height + offset > pixels_len (the length of `pixels`
 * in bytes), and the paint from pixels + offset bytes. GtkGlassNative passes a native copy of the frame alone
 * (the 4 * width * height bytes at offset; pixels_len = that length, offset 0), as for
 * ggtk_view_upload_pixels_int.
 */
GGTK_EXPORT void ggtk_view_upload_pixels_byte(ggtk_view_t view, const uint8_t* pixels, int32_t pixels_len,
                                              int32_t offset, int32_t width, int32_t height);

/*
 * GtkView._enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor)
 * (Java_com_sun_glass_ui_gtk_GtkView__1enterFullscreen): with a window, gtk_window_fullscreen and
 * notify_view(ViewEvent.FULLSCREEN_ENTER); returns 0 when that slot threw (the JNI's
 * CHECK_JNI_EXCEPTION_RET(FALSE)), else 1, also without a window. The three flags are ignored, as the JNI
 * ignored them. UPCALLS.
 */
GGTK_EXPORT int32_t ggtk_view_enter_fullscreen(ggtk_view_t view, int32_t animate, int32_t keep_ratio,
                                               int32_t hide_cursor);

/* GtkView._exitFullscreen(long ptr, boolean animate) (Java_com_sun_glass_ui_gtk_GtkView__1exitFullscreen): with
 * a window, gtk_window_unfullscreen of the embedded full-screen window if there is one, else of the window,
 * then notify_view(ViewEvent.FULLSCREEN_EXIT); `animate` is ignored. UPCALLS. */
GGTK_EXPORT void ggtk_view_exit_fullscreen(ggtk_view_t view, int32_t animate);

/* ---- GtkDnDClipboard (GlassDnDClipboard.cpp, glass_dnd.cpp) ---- */

/* GtkDnDClipboard.isOwner (Java_com_sun_glass_ui_gtk_GtkDnDClipboard_isOwner): 1 when the last drag that
 * entered a Glass window, or the last drag started here since, was started by this process. */
GGTK_EXPORT int32_t ggtk_dnd_is_owner(void);

/*
 * GtkDnDClipboard.pushToSystemImpl(HashMap data, int supportedActions)
 * (Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushToSystemImpl, glass_dnd.cpp execute_dnd). `keys` holds the
 * keys of the data map in its iteration order: key_count strings, each modified UTF-8 as GetStringUTFChars
 * gave it (JniStringCodec.toModifiedUtf8) and NUL-terminated, back to back, borrowed for the call (a null key
 * has no representation: the JNI crashed on it). `supported` is a set of Clipboard.ACTION_* bits. With
 * supported == 0 no drag starts (and the JNI walked no key); otherwise a drag widget is created, the drag
 * targets of the keys are added in key order (text/plain -> UTF8_STRING, text/plain and STRING;
 * application/x-java-rawimage -> the four image types; application/x-java-file-list -> text/uri-list; the
 * two drag-image keys -> none; any other key -> the atom of that name) and gtk_drag_begin starts the drag.
 * Then main-loop iterations run until the drag widget is gone, and the performed action is returned
 * (Clipboard.ACTION_*; with supported == 0 the one of the previous drag, as the JNI answered). The values
 * of the map are pulled through GgtkDndCallbacks.source_get_data while this runs (drag-begin,
 * drag-data-get), so the map must be the drag in progress for that slot before the call. BLOCKS for the
 * drag and UPCALLS.
 * The JNI walked the keys (Map.keySet().iterator()) where this adds the targets, after creating the drag
 * widget. A throw of that walk (JNI_EXCEPTION_TO_CPP) was reported, left a NullPointerException pending (the
 * Throwable was already cleared when jni_exception asked it for its message) and answered ACTION_NONE after
 * queueing the widget's destruction; that path has no form here, the caller walks the keys before the call.
 */
GGTK_EXPORT int32_t ggtk_dnd_push_to_system(const char* keys, int32_t key_count, int32_t supported);

/*
 * GtkDnDClipboard.supportedSourceActionsFromSystem
 * (Java_com_sun_glass_ui_gtk_GtkDnDClipboard_supportedSourceActionsFromSystem): writes the actions the source
 * of the drag that last entered a Glass window offers (Clipboard.ACTION_* bits) and returns GGTK_OK;
 * GGTK_ERR_NOT_IN_DRAG with *out_actions 0 when no drag has entered - Java throws the IllegalStateException.
 */
GGTK_EXPORT int32_t ggtk_dnd_target_get_supported_actions(int32_t* out_actions);

/*
 * GtkDnDClipboard.mimesFromSystem (Java_com_sun_glass_ui_gtk_GtkDnDClipboard_mimesFromSystem,
 * glass_dnd.cpp dnd_target_get_mimes): the mime types of the drag that last entered a Glass window.
 * GGTK_ERR_NOT_IN_DRAG when none has (Java throws the IllegalStateException; the outputs are zeroed).
 * Otherwise *out_strings holds every string the JNI added to its HashSet, IN ORDER and WITH REPEATS - Java
 * adds them in this order to a new HashSet and returns set.toArray(new String[set.size()]), the array the JNI
 * built - as *out_count NUL-terminated strings back to back, the bytes the JNI handed to NewStringUTF (decode
 * each with JniStringCodec.fromNewStringUtf), in a g_malloc'd block of at least one byte that the caller
 * releases with g_free. The list is computed once per drag enter: the first call walks the drag's targets
 * and fetches the text/uri-list data through a nested main loop (BLOCKS and UPCALLS), later calls answer the
 * same list. *out_generation numbers the computed lists: the JNI returned ONE cached String[] per drag enter,
 * so Java can return its array again while the number is the same.
 * A result >= 0 is the number of OutOfMemoryError the JNI reported while fetching (an event hook it could
 * not allocate: glass_throw_oom "Failed to allocate event hook"); Java reports as many
 * new OutOfMemoryError("Failed to allocate event hook") through Application.reportException, which the JNI
 * did at the failed allocation.
 */
GGTK_EXPORT int32_t ggtk_dnd_target_get_mimes(char** out_strings, int32_t* out_count, int64_t* out_generation);

/*
 * ---- GgtkDndTargetValue: the value of GtkDnDClipboard.popFromSystem, as C buffers ----
 *
 * ggtk_dnd_target_get_data zero-fills it and fills it; the caller releases `data` with g_free.
 */
typedef struct GgtkDndTargetValue {
    int32_t kind;     /* GGTK_DND_VALUE_* */
    int32_t count;    /* STRING: byte length without the NUL; BYTES / IMAGE: byte count; FILES: String[] length */
    void*   data;     /* a g_malloc'd block the CALLER releases with g_free; NULL for NONE */
    int32_t width;    /* IMAGE: the GtkPixels width */
    int32_t height;   /* IMAGE: the GtkPixels height */
    int32_t records;  /* FILES: the number of records in data */
} GgtkDndTargetValue;   /* 32 bytes on LP64: kind 0, count 4, data 8, width 16, height 20, records 24, padding */

/* GgtkDndTargetValue.kind - what the JNI returned */
#define GGTK_DND_VALUE_NONE   0   /* null */
#define GGTK_DND_VALUE_STRING 1   /* a String: data holds the count bytes the JNI handed to NewStringUTF and a
                                   * NUL; decode with JniStringCodec.fromNewStringUtf */
#define GGTK_DND_VALUE_BYTES  2   /* ByteBuffer.wrap(a byte[count] with the bytes of data) */
#define GGTK_DND_VALUE_IMAGE  3   /* new GtkPixels(width, height, ByteBuffer.wrap(a byte[count] with the bytes of
                                   * data)); count is rowstride * height of the decoded image */
#define GGTK_DND_VALUE_FILES  4   /* a String[count] built as glass_general.cpp uris_to_java built it: data holds
                                   * `records` records, each an int32 index and an int32 len, then - when len is
                                   * not -1 - len bytes of a path as the JNI handed it to NewStringUTF and a NUL,
                                   * then zero bytes up to a multiple of 4. Java stores each decoded path at
                                   * `index`, in record order; len -1 is a null element (g_filename_from_uri
                                   * failed). index counts ALL uris of the list, not only the file uris (a bug of
                                   * commit 033187ad90): for an index >= count the JNI's SetObjectArrayElement
                                   * threw ArrayIndexOutOfBoundsException ("Index <index> out of bounds for length
                                   * <count>"), reported it (check_and_clear_exception) and went on - Java does the
                                   * same and leaves that element null. */

#if defined(__cplusplus)
static_assert(sizeof(GgtkDndTargetValue) == (sizeof(void*) == 8 ? 32 : 24),
              "GgtkDndTargetValue must be 5 int32 + 1 pointer, padded to 8 on LP64");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GgtkDndTargetValue) == (sizeof(void*) == 8 ? 32 : 24),
               "GgtkDndTargetValue must be 5 int32 + 1 pointer, padded to 8 on LP64");
#endif

GGTK_EXPORT int32_t ggtk_sizeof_dnd_target_value(void);   /* == 32 on LP64 today */

/*
 * GtkDnDClipboard.popFromSystem(String mimeType) (Java_com_sun_glass_ui_gtk_GtkDnDClipboard_popFromSystem,
 * glass_dnd.cpp dnd_target_get_data): the drag's data for `mime` - modified UTF-8 as GetStringUTFChars gave
 * it (JniStringCodec.toModifiedUtf8), NUL-terminated, borrowed (a null String has no representation: the JNI
 * crashed on it). *out is zero-filled first. GGTK_ERR_NOT_IN_DRAG when no drag has entered a Glass window
 * (Java throws the IllegalStateException). Otherwise fetches the data through nested main loops (BLOCKS and
 * UPCALLS) exactly as the JNI did and fills *out:
 *   "text/plain": UTF8_STRING, else text/plain, else STRING converted from ISO-8859-1 -> STRING or NONE;
 *   "text/uri-list": the non-file uris of text/uri-list, CRLF-separated -> STRING, NONE without any;
 *   any other "text/..." type: that target as text -> STRING or NONE;
 *   "application/x-java-file-list": the file uris of text/uri-list -> FILES, NONE without any;
 *   "application/x-java-rawimage": the first of image/png, jpeg, tiff, bmp GdkPixbuf decodes -> IMAGE or NONE;
 *   anything else: the bytes of that target -> BYTES or NONE.
 * A result >= 0 is the number of event-hook OutOfMemoryError reports, as for ggtk_dnd_target_get_mimes. The
 * JNI built the Java objects as it went: a throw while building an image (only OutOfMemoryError is possible -
 * GtkPixels accepts every size this passes) made it try the next image type, which this cannot see.
 */
GGTK_EXPORT int32_t ggtk_dnd_target_get_data(const char* mime, GgtkDndTargetValue* out);

#ifdef __cplusplus
}
#endif

#endif /* GLASS_GTK_API_H */
