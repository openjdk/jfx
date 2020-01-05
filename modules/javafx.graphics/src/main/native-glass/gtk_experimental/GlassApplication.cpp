/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>
#include <glib.h>

#include <cstdlib>
#include <com_sun_glass_ui_gtk_GtkApplication.h>
#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_MouseEvent.h>
#include <com_sun_glass_events_ViewEvent.h>
#include <com_sun_glass_events_KeyEvent.h>
#include <jni.h>

#include "glass_general.h"
#include "glass_evloop.h"
#include "glass_dnd.h"
#include "glass_window.h"
#include "glass_screen.h"

GdkEventFunc process_events_prev;
static void process_events(GdkEvent*, gpointer);

JNIEnv* mainEnv; // Use only with main loop thread!!!

extern gboolean disableGrab;

static gboolean call_runnable (gpointer data)
{
    RunnableContext* context = reinterpret_cast<RunnableContext*>(data);

    JNIEnv *env;
    int envStatus = javaVM->GetEnv((void **)&env, JNI_VERSION_1_6);
    if (envStatus == JNI_EDETACHED) {
        javaVM->AttachCurrentThread((void **)&env, NULL);
    }

    env->CallVoidMethod(context->runnable, jRunnableRun, NULL);
    LOG_EXCEPTION(env);
    env->DeleteGlobalRef(context->runnable);
    free(context);

    if (envStatus == JNI_EDETACHED) {
        javaVM->DetachCurrentThread();
    }

    return FALSE;
}

extern "C" {

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
static void init_threads() {
    gboolean is_g_thread_get_initialized = FALSE;
    if (glib_check_version(2, 32, 0)) { // < 2.32
        if (!glib_check_version(2, 20, 0)) {
            is_g_thread_get_initialized = g_thread_get_initialized();
        }
        if (!is_g_thread_get_initialized) {
            g_thread_init(NULL);
        }
    }
    gdk_threads_init();
}
#pragma GCC diagnostic pop

jboolean gtk_verbose = JNI_FALSE;

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _initGTK
 * Signature: (IZ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1initGTK
  (JNIEnv *env, jclass clazz, jint version, jboolean verbose, jfloat uiScale)
{
    (void) clazz;
    (void) version;

    OverrideUIScale = uiScale;
    gtk_verbose = verbose;

    env->ExceptionClear();

#if !GTK_CHECK_VERSION(3, 6, 0)
    init_threads();
    gdk_threads_enter();
#endif

    gtk_init(NULL, NULL);

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _queryLibrary
 * Signature: Signature: (IZ)I
 */
#ifndef STATIC_BUILD
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary
  (JNIEnv *env, jclass clazz, jint suggestedVersion, jboolean verbose)
{
    // If we are being called, then the launcher is
    // not in use, and we are in the proper glass library already.
    // This can be done by renaming the gtk versioned native
    // libraries to be libglass.so
    // Note: we will make no effort to complain if the suggestedVersion
    // is out of phase.

    (void)env;
    (void)clazz;
    (void)suggestedVersion;
    (void)verbose;

    Display *display = XOpenDisplay(NULL);
    if (display == NULL) {
        return com_sun_glass_ui_gtk_GtkApplication_QUERY_NO_DISPLAY;
    }
    XCloseDisplay(display);

    return com_sun_glass_ui_gtk_GtkApplication_QUERY_USE_CURRENT;
}
#endif

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1init
  (JNIEnv * env, jobject obj, jlong handler, jboolean _disableGrab)
{
    (void)obj;

    mainEnv = env;
    process_events_prev = (GdkEventFunc) handler;
    disableGrab = (gboolean) _disableGrab;

    glass_gdk_x11_display_set_window_scale(gdk_display_get_default(), 1);
    gdk_event_handler_set(process_events, NULL, NULL);

    GdkScreen *default_gdk_screen = gdk_screen_get_default();
    if (default_gdk_screen != NULL) {
        g_signal_connect(G_OBJECT(default_gdk_screen), "monitors-changed",
                         G_CALLBACK(screen_settings_changed), NULL);
        g_signal_connect(G_OBJECT(default_gdk_screen), "size-changed",
                         G_CALLBACK(screen_settings_changed), NULL);
    }

    GdkWindow *root = gdk_screen_get_root_window(default_gdk_screen);
    gdk_window_set_events(root, static_cast<GdkEventMask>(gdk_window_get_events(root) | GDK_PROPERTY_CHANGE_MASK));
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _runLoop
 * Signature: (Ljava/lang/Runnable;Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1runLoop
  (JNIEnv * env, jobject obj, jobject launchable, jboolean noErrorTrap)
{
    (void)obj;
    (void)noErrorTrap;

    env->CallVoidMethod(launchable, jRunnableRun);
    CHECK_JNI_EXCEPTION(env);

    // GTK installs its own X error handler that conflicts with AWT.
    // During drag and drop, AWT hides errors so we need to hide them
    // to avoid exit()'ing.  It's not clear that we don't want to hide
    // X error all the time, otherwise FX will exit().
    //
    // A better solution would be to coordinate with AWT and save and
    // restore the X handler.

    // Disable X error handling
#ifndef VERBOSE
    if (!noErrorTrap) {
#if GTK_CHECK_VERSION(3, 0, 0)
        gdk_x11_display_error_trap_push(gdk_display_get_default());
#else
        gdk_error_trap_push();
#endif
    }
#endif

    gtk_main();

    // When the last JFrame closes and DISPOSE_ON_CLOSE is specified,
    // Java exits with an X error. X error are hidden during the FX
    // event loop and should be restored when the event loop exits. Unfortunately,
    // this is too early. The fix is to never restore X errors.
    //
    // See RT-21408 & RT-20756

    // Restore X error handling
    // #ifndef VERBOSE
    //     if (!noErrorTrap) {
    //         gdk_error_trap_pop();
    //     }
    // #endif

#if !GTK_CHECK_VERSION(3, 6, 0)
    gdk_threads_leave();
#endif

}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _terminateLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1terminateLoop
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    gtk_main_quit();
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _submitForLaterInvocation
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1submitForLaterInvocation
  (JNIEnv * env, jobject obj, jobject runnable)
{
    (void)obj;

    RunnableContext* context = (RunnableContext*)malloc(sizeof(RunnableContext));
    context->runnable = env->NewGlobalRef(runnable);
    gdk_threads_add_idle_full(G_PRIORITY_HIGH_IDLE + 30, call_runnable, context, NULL);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    enterNestedEventLoopImpl
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_enterNestedEventLoopImpl
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    gtk_main();
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    leaveNestedEventLoopImpl
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_leaveNestedEventLoopImpl
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    gtk_main_quit();
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticScreen_getScreens
 * Signature: ()[Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticScreen_1getScreens
  (JNIEnv * env, jobject obj)
{
    (void)obj;

    try {
        return rebuild_screens(env);
    } catch (jni_exception&) {
        return NULL;
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticTimer_getMinPeriod
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticTimer_1getMinPeriod
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    return 0; // There are no restrictions on period in g_threads
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticTimer_getMaxPeriod
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticTimer_1getMaxPeriod
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    return 10000; // There are no restrictions on period in g_threads
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticView_getMultiClickTime
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickTime
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    static gint multi_click_time = -1;
    if (multi_click_time == -1) {
        g_object_get(gtk_settings_get_default(), "gtk-double-click-time", &multi_click_time, NULL);
    }
    return (jlong)multi_click_time;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticView_getMultiClickMaxX
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickMaxX
  (JNIEnv * env, jobject obj)
{
    (void)env;
    (void)obj;

    static gint multi_click_dist = -1;

    if (multi_click_dist == -1) {
        g_object_get(gtk_settings_get_default(), "gtk-double-click-distance", &multi_click_dist, NULL);
    }
    return multi_click_dist;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    staticView_getMultiClickMaxY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickMaxY
  (JNIEnv * env, jobject obj)
{
    return Java_com_sun_glass_ui_gtk_GtkApplication_staticView_1getMultiClickMaxX(env, obj);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _supportsTransparentWindows
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1supportsTransparentWindows
  (JNIEnv * env, jobject obj) {
    (void)env;
    (void)obj;

    return gdk_screen_is_composited(gdk_screen_get_default());
}

} // extern "C"

bool is_window_enabled_for_event(GdkWindow * window, WindowContext *ctx, gint event_type) {


    if (gdk_window_is_destroyed(window)) {
        return FALSE;
    }

    /*
     * GDK_DELETE can be blocked for disabled window e.q. parent window
     * which prevents from closing it
     */
    switch (event_type) {
        case GDK_CONFIGURE:
        case GDK_DESTROY:
        case GDK_EXPOSE:
        case GDK_DAMAGE:
        case GDK_WINDOW_STATE:
        case GDK_FOCUS_CHANGE:
            return TRUE;
            break;
    }//switch

    if (ctx != NULL) {
        return ctx->isEnabled();
    }
    return TRUE;
}

static void process_events(GdkEvent* event, gpointer data)
{
    GdkWindow* window = event->any.window;
    WindowContext *ctx = window != NULL ? (WindowContext*)
        g_object_get_data(G_OBJECT(window), GDK_WINDOW_DATA_CONTEXT) : NULL;

    if ((window != NULL)
            && !is_window_enabled_for_event(window, ctx, event->type)) {
        return;
    }

    if (ctx != NULL && ctx->hasIME() && ctx->filterIME(event)) {
        return;
    }

    glass_evloop_call_hooks(event);

    if (ctx != NULL) {
        EventsCounterHelper helper(ctx);

        if (event->type == GDK_EXPOSE) {
            ctx->process_expose(&event->expose);
        } else if (event->type == GDK_DRAG_LEAVE) {
            dnd_drag_leave_callback(ctx);
        } else {
            gtk_main_do_event(event);
        }
    } else {
        if (window == gdk_screen_get_root_window(gdk_screen_get_default())) {
            if (event->any.type == GDK_PROPERTY_NOTIFY) {
                if (event->property.atom == gdk_atom_intern_static_string("_NET_WORKAREA")
                        || event->property.atom == gdk_atom_intern_static_string("_NET_CURRENT_DESKTOP")) {
                    screen_settings_changed(gdk_screen_get_default(), NULL);
                }
            }
        }

        //process only for non-FX windows
        if (process_events_prev != NULL) {
            (*process_events_prev)(event, data);
        } else {
            gtk_main_do_event(event);
        }
    }
}
