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
#include "glass_evloop.h"
#include "glass_window.h"
#include "glass_dnd.h"
#include "glass_screen.h"

#include <glib.h>
#include <malloc.h>

static GSList * evloopHookList;
GdkEventFunc process_events_prev;

#define GEVL_HOOK_REGISTRATION_IMPL(ptr) ((GevlHookRegistrationImpl *) ptr)

typedef struct _GevlHookRegistrationImpl {
    GevlHookFunction hookFn;
    void * data;
} GevlHookRegistrationImpl;

void glass_evloop_initialize() {
}

void glass_evloop_finalize() {
    GSList * ptr = evloopHookList;
    while (ptr != NULL) {
        free(ptr->data);
        ptr = g_slist_next(ptr);
    }

    g_slist_free(evloopHookList);
    evloopHookList = NULL;
}

void glass_evloop_call_hooks(GdkEvent * event) {
    GSList * ptr = evloopHookList;
    while (ptr != NULL) {
        GevlHookRegistrationImpl * hookReg =
                GEVL_HOOK_REGISTRATION_IMPL(ptr->data);
        hookReg->hookFn(event, hookReg->data);

        ptr = g_slist_next(ptr);
    }
}

GevlHookRegistration glass_evloop_hook_add(GevlHookFunction hookFn, void * data) {
    GevlHookRegistrationImpl * hookReg =
            (GevlHookRegistrationImpl *)
                malloc(sizeof(GevlHookRegistrationImpl));

    if (hookReg != NULL) {
        hookReg->hookFn = hookFn;
        hookReg->data = data;

        evloopHookList = g_slist_prepend(evloopHookList, hookReg);
    }

    return hookReg;
}

void glass_evloop_hook_remove(GevlHookRegistration hookReg) {
    evloopHookList = g_slist_remove(evloopHookList, hookReg);
    free(hookReg);
}

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


void glass_evloop_process_events(GdkEvent* event, gpointer data) {
    GdkWindow* window = event->any.window;

    if (is_in_drag()) {
        process_dnd_source(window, event);
    }

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
