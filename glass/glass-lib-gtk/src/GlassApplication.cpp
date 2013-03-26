/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <iostream>
#include <com_sun_glass_ui_Application.h>
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

GdkEventFunc process_events_prev;
static void process_events(GdkEvent*, gpointer);

JNIEnv* mainEnv; // Use only with main loop thread!!!

extern gboolean disableGrab;

static gboolean call_runnable (gpointer data)
{
    RunnableContext* context = reinterpret_cast<RunnableContext*>(data);

    mainEnv->CallVoidMethod(context->runnable, jRunnableRun, NULL);
    LOG_EXCEPTION(mainEnv);
    mainEnv->ExceptionClear();
    mainEnv->DeleteGlobalRef(context->runnable);
    free(context);
    return FALSE;
}

static jobject createJavaScreen
  (JNIEnv* env, GdkScreen* screen, GdkRectangle workArea, gint monitor_idx)
{
    GdkRectangle monitor_geometry;
    gdk_screen_get_monitor_geometry(screen, monitor_idx, &monitor_geometry);
    LOG1("convert monitor[%d] -> glass Screen\n", monitor_idx)
    LOG4("[x: %d y: %d w: %d h: %d]\n",
            monitor_geometry.x, monitor_geometry.y,
            monitor_geometry.width, monitor_geometry.height)
    GdkRectangle working_monitor_geometry;
    gdk_rectangle_intersect(&workArea, &monitor_geometry, &working_monitor_geometry);

    jobject jScreen = env->NewObject(jScreenCls, jScreenInit,
                                    (jlong)monitor_idx,

                                    gdk_screen_get_number(screen),

                                    monitor_geometry.x,
                                    monitor_geometry.y,
                                    monitor_geometry.width,
                                    monitor_geometry.height,

                                    working_monitor_geometry.x,
                                    working_monitor_geometry.y,
                                    working_monitor_geometry.width,
                                    working_monitor_geometry.height,

                                    (jint)gdk_screen_get_resolution(screen),
                                    (jint)gdk_screen_get_resolution(screen),
                                    1.0f);
    JNI_EXCEPTION_TO_CPP(env);
    return jScreen;
}

static guint get_current_desktop(GdkScreen *screen) {
    Display* display = gdk_x11_display_get_xdisplay(gdk_display_get_default());
    Atom currentDesktopAtom = XInternAtom(display, "_NET_CURRENT_DESKTOP", True);
    guint ret = 0;

    Atom type;
    int format;
    gulong num, left;
    unsigned long *data = NULL;

    if (currentDesktopAtom == None) {
        return 0;
    }

    int result = XGetWindowProperty(display,
            GDK_WINDOW_XID(gdk_screen_get_root_window(screen)),
            currentDesktopAtom, 0, G_MAXLONG, False, XA_CARDINAL,
            &type, &format, &num, &left, (unsigned char **)&data);

    if ((result == Success) && (data != NULL)) {
        if (type == XA_CARDINAL && format == 32) {
            ret = data[0];
        }

        XFree(data);
    }

    return ret;

}

static GdkRectangle get_screen_workarea(GdkScreen *screen) {
    Display* display = gdk_x11_display_get_xdisplay(gdk_display_get_default());
    GdkRectangle ret = { 0, 0, gdk_screen_get_width(screen), gdk_screen_get_height(screen)};

    Atom workareaAtom = XInternAtom(display, "_NET_WORKAREA", True);

    Atom type;
    int format;
    gulong num, left;
    unsigned long *data = NULL;

    if (workareaAtom == None) {
        return ret;
    }

    int result = XGetWindowProperty(display,
            GDK_WINDOW_XID(gdk_screen_get_root_window(screen)),
            workareaAtom, 0, G_MAXLONG, False, AnyPropertyType,
            &type, &format, &num, &left, (unsigned char **)&data);

    if ((result == Success) && (data != NULL)) {
        if (type != None && format == 32) {
            guint current_desktop = get_current_desktop(screen);
            if (current_desktop < num / 4) {
                ret.x = data[current_desktop * 4];
                ret.y = data[current_desktop * 4 + 1];
                ret.width = data[current_desktop * 4 + 2];
                ret.height = data[current_desktop * 4 + 3];
            }
        }

        XFree(data);
    }

    return ret;

}

static jobjectArray rebuild_screens(JNIEnv* env) {
    GdkScreen *default_gdk_screen = gdk_screen_get_default();
    gint n_monitors = gdk_screen_get_n_monitors(default_gdk_screen);

    jobjectArray jscreens = env->NewObjectArray(n_monitors, jScreenCls, NULL);
    JNI_EXCEPTION_TO_CPP(env)
    LOG1("Available monitors: %d\n", n_monitors)
    int i;
    GdkRectangle workArea = get_screen_workarea(default_gdk_screen);
    LOG4("Work Area: x:%d, y:%d, w:%d, h:%d\n", workArea.x, workArea.y, workArea.width, workArea.height);

    for (i=0; i < n_monitors; i++) {
        env->SetObjectArrayElement(jscreens, i, createJavaScreen(env, default_gdk_screen, workArea, i));
        JNI_EXCEPTION_TO_CPP(env)
    }

    return jscreens;
}

static void screen_settings_changed(GdkScreen* screen, gpointer user_data) {
    mainEnv->CallStaticVoidMethod(jScreenCls, jScreenNotifySettingsChanged);
    LOG_EXCEPTION(mainEnv);
    mainEnv->ExceptionClear(); //This is callback, so clear
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1init
  (JNIEnv * env, jobject obj, jlong handler, jboolean _disableGrab)
{
    mainEnv = env;
    process_events_prev = (GdkEventFunc) handler;
    disableGrab = (gboolean) _disableGrab;

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
        gdk_error_trap_push();
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

    gdk_threads_leave();

}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _terminateLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1terminateLoop
  (JNIEnv * env, jobject obj)
{
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
    return gdk_display_supports_composite(gdk_display_get_default())
            && gdk_screen_is_composited(gdk_screen_get_default());
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

    if (ctx != NULL ) {
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

    if (ctx != NULL && dynamic_cast<WindowContextPlug*>(ctx) && ctx->get_gtk_window()) {
        WindowContextPlug* ctx_plug = dynamic_cast<WindowContextPlug*>(ctx);
        if (!ctx_plug->embedded_children.empty()) {
            // forward to child
            ctx = (WindowContext*) ctx_plug->embedded_children.back();
            window = ctx->get_gdk_window();
        }
    }

    if (is_in_drag()) {
        process_dnd_source(window, event);
    }

    if (ctx != NULL) {
        try {
            switch (event->type) {
                case GDK_PROPERTY_NOTIFY:
                    ctx->process_property_notify(&event->property);
                    gtk_main_do_event(event);
                    break;
                case GDK_CONFIGURE:
                    ctx->process_configure(&event->configure);
                    gtk_main_do_event(event);
                    break;
                case GDK_FOCUS_CHANGE:
                    ctx->process_focus(&event->focus_change);
                    gtk_main_do_event(event);
                    break;
                case GDK_DESTROY:
                    ctx->process_destroy();
                    gtk_main_do_event(event);
                    break;
                case GDK_DELETE:
                    ctx->process_delete();
                    break;
                case GDK_EXPOSE:
                case GDK_DAMAGE:
                    ctx->process_expose(&event->expose);
                    break;
                case GDK_WINDOW_STATE:
                    ctx->process_state(&event->window_state);
                    gtk_main_do_event(event);
                    break;
                case GDK_BUTTON_PRESS:
                case GDK_BUTTON_RELEASE:
                    ctx->process_mouse_button(&event->button);
                    break;
                case GDK_MOTION_NOTIFY:
                    ctx->process_mouse_motion(&event->motion);
                    gdk_event_request_motions(&event->motion);
                    break;
                case GDK_SCROLL:
                    ctx->process_mouse_scroll(&event->scroll);
                    break;
                case GDK_ENTER_NOTIFY:
                case GDK_LEAVE_NOTIFY:
                    ctx->process_mouse_cross(&event->crossing);
                    break;
                case GDK_KEY_PRESS:
                case GDK_KEY_RELEASE:
                    ctx->process_key(&event->key);
                    break;
                case GDK_DROP_START:
                case GDK_DRAG_ENTER:
                case GDK_DRAG_LEAVE:
                case GDK_DRAG_MOTION:
                    process_dnd_target(ctx, &event->dnd);
                    break;
                case GDK_MAP:
                    ctx->process_map();
                    // fall-through
                case GDK_UNMAP:
                case GDK_CLIENT_EVENT:
                case GDK_VISIBILITY_NOTIFY:
                case GDK_SETTING:
                case GDK_OWNER_CHANGE:
                    gtk_main_do_event(event);
                    break;
                default:
                    break;
            }
        } catch (jni_exception&) {
            LOG_EXCEPTION(mainEnv);
            mainEnv->ExceptionClear();
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
