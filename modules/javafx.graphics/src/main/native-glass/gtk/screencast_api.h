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
 * screencast_api.h - the flat C ABI of the screen-capture and remote-desktop part of the GTK Glass
 * library (libglassgtk3.so): the xdg-desktop-portal, D-Bus and PipeWire code of screencast_pipewire.c
 * and screencast_portal.c. Linux only.
 *
 * This is the surface com.sun.glass.ui.gtk.screencast binds through java.lang.foreign. Every function
 * takes and returns <stdint.h> scalars or caller-owned buffers; nothing here knows the JVM (no jobject,
 * no jarray, no JNIEnv, no jni.h). The one call from the C into Java - the restore token the portal hands
 * back - goes through the callback table installed with sc_set_token_callbacks; the calls from Java into
 * the C go through the sc_* functions below, one per JNI native of
 * com.sun.glass.ui.gtk.screencast.ScreencastHelper at commit 033187ad90 (loadPipewire is two - see
 * sc_load_pipewire).
 *
 * WHAT THE FUNCTIONS REPLACE. Up to commit 033187ad90 Java reached this code through the
 * Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_* functions of screencast_pipewire.c, and
 * storeRestoreToken called Java through glass_jvm, a global reference to TokenStorage and a cached
 * jmethodID. Those functions, that global reference, that method id and glass_jvm are gone: the calls
 * into the C are the sc_* functions below and the one call out is the store_token slot. The C logic -
 * the order of the calls, the one retry of the screencast, the early returns, which GLib/GDK/PipeWire
 * variant is used, what is freed when - is the C of commit 033187ad90, unchanged; only the mechanism by
 * which Java reaches it differs. Every function documents the native it stands for.
 *
 * NULL SLOT. storeRestoreToken dials the store_token slot when it is non-NULL and makes NO CALL when it
 * is NULL: the restore token the portal answered is then not stored, which is what commit 033187ad90 did
 * whenever the thread it answered on had no JNIEnv. Nothing else in the library depends on the slot, and
 * Java installs it on the class-initialization path of ScreencastHelper (see INSTALLATION), so a NULL
 * slot is only seen by a caller that never installed a table.
 *
 * UNATTACHED THREADS - A BEHAVIOUR DIFFERENCE. Commit 033187ad90 asked glass_jvm for the JNIEnv of the
 * calling thread and, when that thread was not attached to the JVM, printed "!!! Could not get env" and
 * DROPPED the token. An FFM upcall stub has no such branch: measured on JDK 25.0.4 and JDK 26.0.2, a stub
 * called from a thread created with pthread_create that the JVM has never seen attaches that thread (as a
 * daemon platform thread whose context class loader is null), runs the Java target, hands its return value
 * back to the C, and detaches the thread when it exits; the first call costs about 1.5 ms, the following
 * ones on the same thread nothing. So with the slot installed the token is stored on threads where it used
 * to be lost, and the Java target can run on such a thread.
 *
 * EXCEPTIONS. The slot returns an int32_t status: SC_UPCALL_OK when the Java target returned normally,
 * SC_UPCALL_THREW when it threw. A slot must never let a Throwable escape (an exception escaping an FFM
 * upcall stub terminates the JVM): it reports it exactly as the EXCEPTION_CHECK_DESCRIBE of
 * screencast_pipewire.c did at commit 033187ad90. That macro is JNI ExceptionDescribe, which prints the
 * pending exception to the error stream and CLEARS it (measured on JDK 25.0.4 and 26.0.2):
 * `Exception in thread "<the thread's name>" ` followed by Throwable.printStackTrace() on the same
 * stream, and a Throwable thrown by printStackTrace is swallowed. The C ignores the status, as it ignored
 * the exception that macro had already cleared.
 * THE FUNCTIONS below never throw and never leave anything pending: where the JNI native returned with an
 * exception pending, the function reports it in an out-parameter and Java throws the same exception type
 * with the same message after the downcall returns - the point where the pending exception surfaced.
 * sc_get_rgb_pixels is the only one.
 *
 * STRINGS AND ARRAYS. A restore token crosses in either direction as the bytes the JNI moved: into the C
 * as modified UTF-8 (what GetStringUTFChars produced: com.sun.javafx.font.JniStringCodec.toModifiedUtf8),
 * NUL terminated, never a charset encode; out of the C as the bytes NewStringUTF was given, which Java
 * decodes with JniStringCodec.fromNewStringUtf. Every pointer a function or a slot is handed is borrowed
 * for the duration of that call: the callee neither keeps nor frees it. int32_t buffers are plain copies -
 * the screen-bounds rectangles in, the captured pixels out.
 *
 * THREAD. Java serialises these functions itself, as it did the natives (ScreencastHelper's methods are
 * static synchronized; loadPipewire runs once, in the class initializer). sc_get_rgb_pixels and the four
 * sc_remote_desktop_* functions run on the thread that iterates GLib's default main context - the JavaFX
 * application thread - because com.sun.glass.ui.gtk.GtkRobot calls them there; they run nested GTK main
 * loops and wait for D-Bus replies and for the PipeWire thread loop, so they BLOCK, for as long as the
 * portal takes, including a user consent dialog. sc_close_session is called from the java.util.Timer
 * thread "auto-close screencast session" two seconds after the last use, and tears the session down while
 * the application thread may be inside GTK - that is the C of commit 033187ad90.
 * The store_token slot is dialled from inside sc_get_rgb_pixels and the sc_remote_desktop_* functions:
 * the portal's Response callback (screencast_portal.c callbackScreenCastStart) runs in the nested
 * gtk_main of portalScreenCastStart. The slot is therefore normally dialled on the application thread;
 * see UNATTACHED THREADS for the other threads it may be dialled on.
 *
 * INSTALLATION. sc_set_token_callbacks copies the table by value (the caller may free the struct), NULL
 * clears it, the last call wins, and there is no lock: install it once, on the class-initialization path
 * of ScreencastHelper, before sc_load_pipewire. The stubs the table points to must stay valid for the
 * life of the process (Arena.global()): the portal can answer at any time.
 *
 * critical(true): FORBIDDEN on every function of this header but sc_abi_version and
 * sc_sizeof_token_callbacks. All the others talk D-Bus to the desktop portal, run nested GTK main loops,
 * wait on the PipeWire thread loop or dial the slot. A critical downcall keeps its thread in Java for the
 * whole call, so every safepoint of the VM would wait for a portal round trip.
 */

#ifndef SCREENCAST_API_H
#define SCREENCAST_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define SC_EXPORT __declspec(dllexport)
#else
#  define SC_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Must be bumped for a removal, or a prototype, struct-layout or constant change, in anything the Java
 * side binds. An export added together with its Java binding rides the current version. Java calls
 * sc_abi_version first and refuses the library on a mismatch.
 * ABI 1 = the token callback table, its installer and sizeof probe, and the functions of the seven
 * natives of ScreencastHelper below.
 * Deleting the JNI of commit 033187ad90 from this library did NOT move it: no export, prototype,
 * struct layout or constant the Java side binds changed, and the only behaviour that did - a library
 * whose store_token slot was never installed now stores no token instead of calling Java through
 * glass_jvm - is unreachable from Java, which installs the table before sc_load_pipewire and never
 * clears it.
 */
#define GLASS_SCREENCAST_ABI_VERSION 1

SC_EXPORT int32_t sc_abi_version(void);

/* Status of this header's own functions (the installer). */
#define SC_OK 0

/* Status the callback slot returns (see EXCEPTIONS). */
#define SC_UPCALL_OK    0
#define SC_UPCALL_THREW 1

/*
 * VALUES SHARED WITH JAVA. What sc_load_pipewire takes and what the capture and remote-desktop functions
 * answer. They are the XdgPortalMethod and ScreenCastResult enumerations of screencast_portal.h, which
 * this library uses unchanged (screencast_api.c pins the two sides against each other at compile time),
 * and the XDG_METHOD_* / ERROR / DENIED / OUT_OF_BOUNDS / NO_STREAMS constants of ScreencastHelper.java.
 * A result of 0 or more means the action was carried out.
 */
#define SC_METHOD_SCREENCAST      0
#define SC_METHOD_REMOTE_DESKTOP  1

#define SC_RESULT_OK              0
#define SC_RESULT_ERROR         (-1)
#define SC_RESULT_DENIED       (-11)
#define SC_RESULT_OUT_OF_BOUNDS (-12)
#define SC_RESULT_NO_STREAMS   (-13)

/*
 * ---- ScTokenCallbacks: the one target in Java ----
 */
typedef struct ScTokenCallbacks {
    /*
     * TokenStorage.storeTokenFromNative(String oldToken, String newToken, int[] allowedScreenBounds)
     * (static). Replaces the CallStaticVoidMethod of screencast_pipewire.c storeRestoreToken,
     * which screencast_portal.c callbackScreenCastStart calls with the restore_token of the portal's
     * Start response and the token the session was started with.
     * `old_token` is NULL when the session was started without one (the JNI built no String then and
     * passed null); `new_token` is the portal's token and is never NULL. Both are the bytes the JNI
     * handed to NewStringUTF (decode with JniStringCodec.fromNewStringUtf), NUL terminated and borrowed.
     * `bounds` holds `bounds_len` ints - x, y, width, height of each screen of the session, in the order
     * the C holds them - which Java passes on as an int[bounds_len]; it is borrowed and only read.
     * The slot is dialled ONLY when the session has at least one screen: at commit 033187ad90 the
     * CallStaticVoidMethod sat inside `if (screenSpace.screenCount > 0)`, so with no screen Java was not
     * called at all (its own first act is to return when the bounds array is null).
     * The status is ignored by the C; see EXCEPTIONS for what a throw must do.
     */
    int32_t (*store_token)(const char* old_token, const char* new_token,
                           const int32_t* bounds, int32_t bounds_len);
} ScTokenCallbacks;   /* 1 pointer */

SC_EXPORT int32_t sc_sizeof_token_callbacks(void);   /* == sizeof(void*) today */

/* Installs the table (copied by value); NULL clears it. Returns SC_OK. See INSTALLATION. */
SC_EXPORT int32_t sc_set_token_callbacks(const ScTokenCallbacks* cb);

/*
 * ---- The functions of the natives ----
 */

/*
 * ScreencastHelper.loadPipewire(int method, boolean isDebug), first half
 * (Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_loadPipewire up to its FindClass): turns the
 * library's screencast debug output on or off (`debug` 0 / 1, the -Djavafx.robot.screenshotDebug of
 * ScreencastHelper), then answers 0 at once when `method` is neither SC_METHOD_SCREENCAST nor
 * SC_METHOD_REMOTE_DESKTOP - before anything else, including the remote-desktop flag. Otherwise it
 * records whether this is a remote-desktop session (which also decides how glass_key.cpp maps keys) and
 * dlopen's libpipewire-0.3.so.0 and resolves the symbols it uses: 1 when they all resolved, 0 when the
 * library is absent or a symbol is missing.
 * THE SPLIT: the JNI looked TokenStorage and its storeTokenFromNative up between this work and the portal
 * probe below, and returned false - without touching the portal - when either lookup failed. So the two
 * halves are two functions, and Java does its part of loadPipewire (installing the table, and whatever
 * initializing of that class the lookup did) between them, in the order commit 033187ad90 had.
 */
SC_EXPORT int32_t sc_load_pipewire(int32_t method, int32_t debug);

/*
 * ScreencastHelper.loadPipewire, second half (the tail of that same JNI function): empties the active
 * session token, probes the xdg-desktop-portal on the session bus - its ScreenCast (or RemoteDesktop)
 * interface and version - and cleans that probe's session up again. Answers 1 when the portal is usable,
 * which is what loadPipewire returned and what ScreencastHelper.isAvailable() reports, 0 when it is not.
 * Call it only after sc_load_pipewire answered 1. BLOCKS on D-Bus.
 */
SC_EXPORT int32_t sc_init_xdg_desktop_portal(void);

/*
 * ScreencastHelper.closeSession()
 * (Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_closeSession): closes the screencast session -
 * the PipeWire streams and thread loop, the portal session and its D-Bus proxies - and forgets the active
 * session token. Does nothing when the session is already closed. See THREAD: Java calls this from its
 * Timer thread.
 */
SC_EXPORT void sc_close_session(void);

/*
 * ScreencastHelper.getRGBPixelsImpl(int x, int y, int width, int height, int[] pixelArray,
 * int[] affectedScreensBoundsArray, String token)
 * (Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_getRGBPixelsImpl): captures the width x height
 * area at (x, y) in device pixels through the portal and writes it into `pixels`.
 * `bounds` holds `bounds_len` ints - x, y, width, height of each screen the area touches, the int[] the
 * JNI read; NULL is the null array (then bounds_len is ignored and no screen is named), and a bounds_len
 * that is not a multiple of 4 answers SC_RESULT_ERROR, as the JNI did, after the debug line "incorrect
 * array length". `token` is the restore token to start the session with, modified UTF-8 and NUL
 * terminated, or NULL for no token (a null String); it is borrowed.
 * The session is started, and started a SECOND time when the first attempt failed with anything but
 * SC_RESULT_DENIED - the retry of commit 033187ad90. Returns what that attempt returned: 0 or more when
 * the pixels were captured, else SC_RESULT_ERROR / _DENIED / _OUT_OF_BOUNDS / _NO_STREAMS (the negative
 * PipeWire file descriptor the C stores a failure reason in is answered as it is, as the JNI answered
 * it). UPCALLS (the store_token slot) and BLOCKS.
 * `pixels` is a buffer of `pixels_len` ints that the function writes the captured rows into, at the
 * offsets the JNI passed to SetIntArrayRegion: it is the pixel array of ScreencastHelper.getRGBPixels,
 * and Java hands over a native copy of it, never the Java array itself - the call blocks on the portal,
 * so no critical downcall and no pinned array may be held across it. Every row copy is range-checked
 * against pixels_len exactly as SetIntArrayRegion checked it against the array's length: a row that does
 * not fit is NOT copied, the rows that fit still are, and the rejected ones are reported in
 * `out_rejected_region` - an int32_t[3] the caller provides, which the function zero-fills first:
 *   [0] how many row copies were rejected,
 *   [1] the start and [2] the length, both in ints, of the LAST rejected one.
 * NULL is allowed and drops the report, not the check. On [0] != 0 Java throws the exception HotSpot threw
 * for that last rejected copy and whose return value it dropped: ArrayIndexOutOfBoundsException, message
 * "Array region <start>..<start + length> out of bounds for length <pixels_len>", or
 * "Length <length> is negative" when the length is negative (both measured on JDK 25.0.4 and 26.0.2).
 * The offsets are computed in int32_t arithmetic and may overflow, as they did at commit 033187ad90.
 */
SC_EXPORT int32_t sc_get_rgb_pixels(int32_t x, int32_t y, int32_t width, int32_t height,
                                    int32_t* pixels, int32_t pixels_len,
                                    const int32_t* bounds, int32_t bounds_len,
                                    const char* token, int32_t* out_rejected_region);

/*
 * ScreencastHelper.remoteDesktopMouseMoveImpl(int x, int y, String token)
 * (Java_com_sun_glass_ui_gtk_screencast_ScreencastHelper_remoteDesktopMouseMoveImpl): starts the
 * remote-desktop session with `token` if it is not running and moves the pointer to the absolute device
 * pixel (x, y). Returns SC_RESULT_OK when the session was started and the move was sent, SC_RESULT_DENIED
 * when the portal refused the move, and otherwise the session's failure reason (an SC_RESULT_* or the
 * negative PipeWire file descriptor), exactly as the JNI returned it. `token` as for sc_get_rgb_pixels.
 * UPCALLS and BLOCKS.
 */
SC_EXPORT int32_t sc_remote_desktop_mouse_move(int32_t x, int32_t y, const char* token);

/*
 * ScreencastHelper.remoteDesktopMouseButtonImpl(boolean isPress, int buttons, String token)
 * (Java_..._remoteDesktopMouseButtonImpl): as sc_remote_desktop_mouse_move, with a press (is_press 1) or
 * release (0) of the Glass button mask `buttons`. UPCALLS and BLOCKS.
 */
SC_EXPORT int32_t sc_remote_desktop_mouse_button(int32_t is_press, int32_t buttons, const char* token);

/*
 * ScreencastHelper.remoteDesktopMouseWheelImpl(int wheelAmt, String token)
 * (Java_..._remoteDesktopMouseWheelImpl): as sc_remote_desktop_mouse_move, with a wheel notch of
 * `wheel_amt`. UPCALLS and BLOCKS.
 */
SC_EXPORT int32_t sc_remote_desktop_mouse_wheel(int32_t wheel_amt, const char* token);

/*
 * ScreencastHelper.remoteDesktopKeyImpl(boolean isPress, int key, String token)
 * (Java_..._remoteDesktopKeyImpl): as sc_remote_desktop_mouse_move, with a press (is_press 1) or release
 * (0) of the Glass key code `key`. The key is resolved first, before the session is touched: the GDK
 * keyval of the Glass code, lowered, and for a to z the keyboard's scancode for that letter when the
 * layout has one (the QWERTY mapping of commit 033187ad90), else the keyval itself. A key that resolves
 * to a negative value answers SC_RESULT_ERROR without starting anything, after the debug line "failed to
 * find a key". (The JNI also answered SC_RESULT_ERROR when an exception was pending at that point, which
 * cannot happen on entry to a JNI native.) UPCALLS and BLOCKS.
 */
SC_EXPORT int32_t sc_remote_desktop_key(int32_t is_press, int32_t key, const char* token);

#ifdef __cplusplus
}
#endif

#endif /* SCREENCAST_API_H */
