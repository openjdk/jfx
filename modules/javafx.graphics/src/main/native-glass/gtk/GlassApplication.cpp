/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include <X11/Xatom.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>
#include <glib.h>
#include <sstream>

#include <cstdlib>
#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_MouseEvent.h>
#include <com_sun_glass_events_ViewEvent.h>
#include <com_sun_glass_events_KeyEvent.h>

#include "glass_general.h"
#include "glass_evloop.h"
#include "glass_dnd.h"
#include "glass_window.h"
#include "glass_screen.h"

GdkEventFunc process_events_prev;
static void process_events(GdkEvent*, gpointer);

extern gboolean disableGrab;

// The check of commit 033187ad90's checkGtkVersion: NULL when the GTK version is good enough, else the message
// of the UnsupportedOperationException it threw, in a block to release with g_free.
static gchar* checkGtkVersion(int32_t reqMajor) {
    // Major version is checked before loading
    // GTK_3_MIN_MINOR_VERSION and GTK_3_MIN_MICRO_VERSION comes from the build system
    if (reqMajor == 3
        && gtk_check_version(reqMajor, GTK_3_MIN_MINOR_VERSION, GTK_3_MIN_MICRO_VERSION)) {

        std::ostringstream oss;
        oss << "Minimum GTK version required is " << reqMajor << "." << GTK_3_MIN_MINOR_VERSION
            << "." << GTK_3_MIN_MICRO_VERSION << ". System has " << gtk_major_version << "."
            << gtk_minor_version << "." << gtk_micro_version << ".";

        return g_strdup(oss.str().c_str());
    }
    return NULL;
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

uint8_t gtk_verbose = 0;

/*
 * Each ggtk_application_* function below is the body of the Java_com_sun_glass_ui_gtk_GtkApplication_* function
 * of the same name at commit 033187ad90, which GtkApplication now calls through GtkGlassNative. The contracts are
 * in glass_gtk_api.h. Two of the natives of commit 033187ad90 have no function here: _queryLibrary, the query
 * for the glass GTK library, which launcher.c and a copy in this file answered and GtkGlassNative now answers
 * in Java; and _terminateLoop, because GtkApplication calls gtk_main_quit through GtkGlassNative itself, and
 * the PlatformSupport the JNI function deleted next is Java's.
 */

int32_t ggtk_application_init_gtk(int32_t version, int32_t verbose, float ui_scale, char** out_error)
{
    OverrideUIScale = ui_scale;
    gtk_verbose = verbose ? 1 : 0;

    init_threads();

    gdk_threads_enter();
    gtk_init(NULL, NULL);

    gchar* error = checkGtkVersion(version);
    if (out_error != NULL) {
        *out_error = error;
    } else {
        g_free(error);
    }
    return (error != NULL) ? GGTK_ERR_GTK_VERSION : GGTK_OK;
}

void ggtk_application_init(int64_t handler, int32_t disable_grab)
{
    process_events_prev = (GdkEventFunc) handler;
    disableGrab = (gboolean) disable_grab;

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

    // Set ibus to sync mode
    setenv("IBUS_ENABLE_SYNC_MODE", "1", 1);
}

// _runLoop after the launchable has run: the C ran nothing before Runnable.run, and returned right after it
// when it threw (CHECK_JNI_EXCEPTION), so the caller runs it and this is the rest.
void ggtk_application_run_loop(int32_t no_error_trap)
{
    (void)no_error_trap;

    // GTK installs its own X error handler that conflicts with AWT.
    // During drag and drop, AWT hides errors so we need to hide them
    // to avoid exit()'ing.  It's not clear that we don't want to hide
    // X error all the time, otherwise FX will exit().
    //
    // A better solution would be to coordinate with AWT and save and
    // restore the X handler.

    // Disable X error handling
#ifndef VERBOSE
    if (!no_error_trap) {
        gdk_error_trap_push();
    }
#endif

    gtk_main();

    // When the last JFrame closes and DISPOSE_ON_CLOSE is specified,
    // Java exits with an X error. X error are hidden during the FX
    // event loop and should be restored when the event loop exits. Unfortunately,
    // this is too early. The fix is to never restore X errors.
    //
    // See JDK-8126059 & JDK-8118745

    // Restore X error handling
    // #ifndef VERBOSE
    //     if (!noErrorTrap) {
    //         gdk_error_trap_pop();
    //     }
    // #endif

    gdk_threads_leave();

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

    EventsCounterHelper helper(ctx);

    if ((event->type == GDK_KEY_PRESS || event->type == GDK_KEY_RELEASE) && ctx != NULL && ctx->filterIME(event)) {
        return;
    }

    glass_evloop_call_hooks(event);

    if (ctx != NULL) {
        switch (event->type) {
            case GDK_PROPERTY_NOTIFY:
                // let gtk handle it first to prevent a glitch
                gtk_main_do_event(event);
                ctx->process_property_notify(&event->property);
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
                destroy_and_delete_ctx(ctx);
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
            case GDK_2BUTTON_PRESS:
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
