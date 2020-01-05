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
#include "glass_window.h"
#include "glass_key.h"
#include "glass_screen.h"
#include "glass_dnd.h"

#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_ViewEvent.h>
#include <com_sun_glass_events_MouseEvent.h>
#include <com_sun_glass_events_KeyEvent.h>

#include <com_sun_glass_ui_Window_Level.h>

#include <X11/extensions/shape.h>
#include <cairo.h>
#include <cairo-xlib.h>
#include <gdk/gdkx.h>
#include <gdk/gdk.h>

#ifdef GLASS_GTK3
#include <gtk/gtkx.h>
#endif

#include <string.h>

#include <algorithm>

#define MOUSE_BACK_BTN 8
#define MOUSE_FORWARD_BTN 9

static gboolean ctx_configure_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_configure();
    return FALSE;
}

static gboolean ctx_property_notify_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_property_notify(&event->property);
    return TRUE;
}

static gboolean ctx_focus_change_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_focus(&event->focus_change);
    return TRUE;
}

static gboolean ctx_delete_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_delete();
    return TRUE;
}

static gboolean ctx_window_state_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_state(&event->window_state);
    return FALSE;
}

static gboolean ctx_device_button_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_mouse_button(&event->button);
    return TRUE;
}

static gboolean ctx_device_motion_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_mouse_motion(&event->motion);
    return TRUE;
}

static gboolean ctx_device_scroll_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_mouse_scroll(&event->scroll);
    return TRUE;
}

static gboolean ctx_enter_or_leave_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_mouse_cross(&event->crossing);
    return TRUE;
}

static gboolean ctx_key_press_or_release_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_key(&event->key);
    return TRUE;
}

static gboolean ctx_map_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    ((WindowContext *) user_data)->process_map();
    return TRUE;
}

static void ctx_screen_changed_callback(GtkWidget *widget,
                                        GdkScreen *previous_screen,
                                        gpointer user_data) {
    ((WindowContext *) user_data)->process_screen_changed();
}

static void connect_signals(GtkWidget *gtk_widget, WindowContext *ctx) {
    g_signal_connect(gtk_widget, "configure-event", G_CALLBACK(ctx_configure_callback), ctx);
    g_signal_connect(gtk_widget, "property-notify-event", G_CALLBACK(ctx_property_notify_callback), ctx);
    g_signal_connect(gtk_widget, "focus-in-event", G_CALLBACK(ctx_focus_change_callback), ctx);
    g_signal_connect(gtk_widget, "focus-out-event", G_CALLBACK(ctx_focus_change_callback), ctx);
    g_signal_connect(gtk_widget, "delete-event", G_CALLBACK(ctx_delete_callback), ctx);
    g_signal_connect(gtk_widget, "window-state-event", G_CALLBACK(ctx_window_state_callback), ctx);
    g_signal_connect(gtk_widget, "button-press-event", G_CALLBACK(ctx_device_button_callback), ctx);
    g_signal_connect(gtk_widget, "button-release-event", G_CALLBACK(ctx_device_button_callback), ctx);
    g_signal_connect(gtk_widget, "motion-notify-event", G_CALLBACK(ctx_device_motion_callback), ctx);
    g_signal_connect(gtk_widget, "scroll-event", G_CALLBACK(ctx_device_scroll_callback), ctx);
    g_signal_connect(gtk_widget, "enter-notify-event", G_CALLBACK(ctx_enter_or_leave_callback), ctx);
    g_signal_connect(gtk_widget, "leave-notify-event", G_CALLBACK(ctx_enter_or_leave_callback), ctx);
    g_signal_connect(gtk_widget, "key-press-event", G_CALLBACK(ctx_key_press_or_release_callback), ctx);
    g_signal_connect(gtk_widget, "key-release-event", G_CALLBACK(ctx_key_press_or_release_callback), ctx);
    g_signal_connect(gtk_widget, "map-event", G_CALLBACK(ctx_map_callback), ctx);
    g_signal_connect(gtk_widget, "screen-changed", G_CALLBACK(ctx_screen_changed_callback), ctx);
}


void destroy_and_delete_ctx(WindowContext *ctx) {
    if (ctx) {
        ctx->process_destroy();

        if (!ctx->get_events_count()) {
            delete ctx;
        }
        // else: ctx will be deleted in EventsCounterHelper after completing
        // an event processing
    }
}

static inline jint gtk_button_number_to_mouse_button(guint button) {
    switch (button) {
        case 1:
            return com_sun_glass_events_MouseEvent_BUTTON_LEFT;
        case 2:
            return com_sun_glass_events_MouseEvent_BUTTON_OTHER;
        case 3:
            return com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
        case MOUSE_BACK_BTN:
            return com_sun_glass_events_MouseEvent_BUTTON_BACK;
        case MOUSE_FORWARD_BTN:
            return com_sun_glass_events_MouseEvent_BUTTON_FORWARD;
        default:
            // Other buttons are not supported by quantum and are not reported by other platforms
            return com_sun_glass_events_MouseEvent_BUTTON_NONE;
    }
}

////////////////////////////// WindowContext /////////////////////////////////

static GdkAtom atom_net_wm_state = gdk_atom_intern_static_string("_NET_WM_STATE");
static GdkAtom atom_net_wm_frame_extents = gdk_atom_intern_static_string("_NET_FRAME_EXTENTS");

WindowContext * WindowContext::sm_mouse_drag_window = NULL;
WindowContext * WindowContext::sm_grab_window = NULL;

WindowContext::WindowContext(jobject _jwindow, WindowContext *_owner, long _screen,
                                   WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf) :
        screen(_screen),
        frame_type(_frame_type),
        window_type(type),
        owner(_owner),
        jview(NULL),
        map_received(false),
        visible_received(false),
        on_top(false),
        is_fullscreen(false),
        is_iconified(false),
        is_maximized(false),
        is_mouse_entered(false),
        can_be_deleted(false),
        events_processing_cnt(0),
        grab_pointer(NULL) {

    jwindow = mainEnv->NewGlobalRef(_jwindow);

    gtk_widget = gtk_window_new(type == POPUP ? GTK_WINDOW_POPUP : GTK_WINDOW_TOPLEVEL);

// Not useful, see: https://developer.gnome.org/gtk3/stable/GtkWindow.html#gtk-window-set-wmclass
//    if (gchar * app_name = get_application_name()) {
//        gtk_window_set_wmclass(GTK_WINDOW(gtk_widget), app_name, app_name);
//        g_free(app_name);
//    }

    if (owner) {
        owner->add_child(this);
        if (on_top_inherited()) {
            gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), TRUE);
        }
    }

    if (type == UTILITY) {
        gtk_window_set_type_hint(GTK_WINDOW(gtk_widget), GDK_WINDOW_TYPE_HINT_UTILITY);
    }

    glong xvisualID = (glong) mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);

    if (xvisualID != 0) {
        GdkVisual *visual = gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID);
        glass_gtk_window_configure_from_visual(gtk_widget, visual);
    }

    gtk_widget_set_events(gtk_widget, GDK_ALL_EVENTS_MASK);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);

    glass_gtk_configure_transparency_and_realize(gtk_widget, frame_type == TRANSPARENT);
    gtk_window_set_title(GTK_WINDOW(gtk_widget), "");

    gdk_window = gtk_widget_get_window(gtk_widget);
    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);

    glass_dnd_attach_context(this);

    gdk_windowManagerFunctions = wmf;
    if (wmf) {
        gdk_window_set_functions(gdk_window, wmf);
    }

    if (frame_type != TITLED) {
        gtk_window_set_decorated(GTK_WINDOW(gtk_widget), FALSE);
    }

    connect_signals(gtk_widget, this);
}

void WindowContext::paint(void *data, jint width, jint height) {
#ifdef GLASS_GTK3
    cairo_region_t *region = gdk_window_get_clip_region(gdk_window);
    gdk_window_begin_paint_region(gdk_window, region);
    cairo_t* context = gdk_cairo_create(gdk_window);
#else
    cairo_t *context = gdk_cairo_create(gdk_window);
#endif

    if (bg_color.is_set) {
        cairo_set_source_rgba(context, bg_color.red, bg_color.green, bg_color.blue,
                                (frame_type == TRANSPARENT) ? 0 : 1);
        cairo_set_operator(context, CAIRO_OPERATOR_SOURCE);
        cairo_paint(context);
    }

    cairo_surface_t *cairo_surface;
    cairo_surface = cairo_image_surface_create_for_data(
            (unsigned char *) data,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    cairo_set_source_surface(context, cairo_surface, 0, 0);

    applyShapeMask(data, width, height);
    cairo_set_operator(context, CAIRO_OPERATOR_SOURCE);
    cairo_paint(context);

#ifdef GLASS_GTK3
    gdk_window_end_paint(gdk_window);
    cairo_region_destroy(region);
    cairo_destroy(context);
#else
    cairo_destroy(context);
#endif

    cairo_surface_destroy(cairo_surface);
}

bool WindowContext::isEnabled() {
    if (jwindow) {
        bool result = (JNI_TRUE == mainEnv->CallBooleanMethod(jwindow, jWindowIsEnabled));
        LOG_EXCEPTION(mainEnv)
        return result;
    } else {
        return false;
    }
}

GdkWindow *WindowContext::get_gdk_window() {
    return gdk_window;
}

GtkWidget *WindowContext::get_gtk_widget() {
    return gtk_widget;
}

GtkWindow *WindowContext::get_gtk_window() {
    return GTK_WINDOW(gtk_widget);
}

WindowGeometry WindowContext::get_geometry() {
    return geometry;
}

jobject WindowContext::get_jwindow() {
    return jwindow;
}

jobject WindowContext::get_jview() {
    return jview;
}

void WindowContext::process_map() {
    map_received = true;
    calculate_adjustments();
    apply_geometry();
}

void WindowContext::process_focus(GdkEventFocus *event) {
    if (!event->in && WindowContext::sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }

    if (!event->in && WindowContext::sm_grab_window == this) {
        ungrab_focus();
    }

    if (xim.enabled && xim.ic) {
        if (event->in) {
            XSetICFocus(xim.ic);
        } else {
            XUnsetICFocus(xim.ic);
        }
    }

    if (jwindow) {
        if (!event->in || isEnabled()) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus,
                                    event->in ? com_sun_glass_events_WindowEvent_FOCUS_GAINED
                                              : com_sun_glass_events_WindowEvent_FOCUS_LOST);
            CHECK_JNI_EXCEPTION(mainEnv)
        } else {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusDisabled);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContext::process_property_notify(GdkEventProperty *event) {
    if (event->window == gdk_window) {
        // This work-around is only necessary for Unity
        if (event->atom == atom_net_wm_state) {
            process_net_wm_property();
        } else if (event->atom == atom_net_wm_frame_extents) {
            if (frame_type != TITLED) {
                return;
            }

            int top, left, bottom, right;

            if (get_frame_extents_property(&top, &left, &bottom, &right)) {
                if (top + left + bottom + right > 0) {
                    geometry.frame_extents_received = true;
                    geometry.adjust_w = left + right;
                    geometry.adjust_h = top + bottom;
                    geometry.view_x = left;
                    geometry.view_y = top;

                    // set bounds again to set to correct window size that must
                    // be the total width and height accounting extents
                    // this is ignored if size is "content size" instead of "window size"
                    if (geometry.needs_ajustment) {
                        set_bounds(0, 0, false, false, geometry.current_w, geometry.current_h, -1, -1);
                    }

                    // force position notify so java will know about view_y and view_x
                    size_position_notify(false, true);
                }
            }
        }
    }
}

void WindowContext::process_configure() {
    gint x, y, w, h, gtk_w, gtk_h;

    gtk_window_get_position(GTK_WINDOW(gtk_widget), &x, &y);
    gtk_window_get_size(GTK_WINDOW(gtk_widget), &gtk_w, &gtk_h);

    w = gtk_w + geometry.adjust_w;
    h = gtk_h + geometry.adjust_h;

    gboolean pos_changed = geometry.current_x != x || geometry.current_y != y;
    gboolean size_changed = geometry.current_w != w || geometry.current_h != h
                            || geometry.current_cw != gtk_w || geometry.current_ch != gtk_h;

    geometry.current_x = x;
    geometry.current_y = y;
    geometry.current_w = w;
    geometry.current_h = h;
    geometry.current_cw = gtk_w;
    geometry.current_ch = gtk_h;

    if (!is_fullscreen && !is_maximized) {
        geometry.last_cw = gtk_w;
        geometry.last_ch = gtk_h;
    }

    size_position_notify(size_changed, pos_changed);
}

void WindowContext::process_destroy() {
    if (owner) {
        owner->remove_child(this);
    }

    if (WindowContext::sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }

    if (WindowContext::sm_grab_window == this) {
        ungrab_focus();
    }

    std::set<WindowContext *>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        // FIX JDK-8226537: this method calls set_owner(NULL) which prevents
        // WindowContext::process_destroy() to call remove_child() (because children
        // is being iterated here) but also prevents gtk_window_set_transient_for from
        // being called - this causes the crash on gnome.
        gtk_window_set_transient_for((*it)->get_gtk_window(), NULL);
        (*it)->set_owner(NULL);
        destroy_and_delete_ctx(*it);
    }
    children.clear();

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyDestroy);
        EXCEPTION_OCCURED(mainEnv);
    }

    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
        jview = NULL;
    }

    if (jwindow) {
        mainEnv->DeleteGlobalRef(jwindow);
        jwindow = NULL;
    }

    can_be_deleted = true;
}

void WindowContext::process_delete() {
    if (jwindow && isEnabled()) {
        gtk_widget_hide_on_delete(gtk_widget);
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyClose);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::process_expose(GdkEventExpose *event) {
    if (jview && is_visible()) {
        mainEnv->CallVoidMethod(jview, jViewNotifyRepaint, event->area.x, event->area.y,
                                event->area.width, event->area.height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::process_mouse_button(GdkEventButton *event) {
    // there are other events like GDK_2BUTTON_PRESS
    if (event->type != GDK_BUTTON_PRESS && event->type != GDK_BUTTON_RELEASE) {
        return;
    }

    bool press = event->type == GDK_BUTTON_PRESS;

    guint state = event->state;
    guint mask = 0;

    // We need to add/remove current mouse button from the modifier flags
    // as X lib state represents the state just prior to the event and
    // glass needs the state just after the event
    switch (event->button) {
        case 1:
            mask = GDK_BUTTON1_MASK;
            break;
        case 2:
            mask = GDK_BUTTON2_MASK;
            break;
        case 3:
            mask = GDK_BUTTON3_MASK;
            break;
        case MOUSE_BACK_BTN:
            mask = GDK_BUTTON4_MASK;
            break;
        case MOUSE_FORWARD_BTN:
            mask = GDK_BUTTON5_MASK;
            break;
    }

    if (press) {
        state |= mask;
    } else {
        state &= ~mask;
    }

    if (press) {
        GdkDevice* device = event->device;

        if (glass_gdk_device_is_grabbed(device)
                && (glass_gdk_device_get_window_at_position(device, NULL, NULL)
                == NULL)) {
            ungrab_focus();
            return;
        }

        // Upper layers expects from us Windows behavior:
        // all mouse events should be delivered to window where drag begins
        // and no exit/enter event should be reported during this drag.
        // We can grab mouse pointer for these needs.
        grab_mouse_drag_focus(gdk_window, (GdkEvent *) event, NULL, true);
    } else {
        if ((event->state & MOUSE_BUTTONS_MASK)
            && !(state & MOUSE_BUTTONS_MASK)) { // all buttons released
            ungrab_mouse_drag_focus();
        } else if (event->button == 8 || event->button == 9) {
            // GDK X backend interprets button press events for buttons 4-7 as
            // scroll events so GDK_BUTTON4_MASK and GDK_BUTTON5_MASK will never
            // be set on the event->state from GDK. Thus we cannot check if all
            // buttons have been released in the usual way (as above).
            ungrab_mouse_drag_focus();
        }
    }

    bool is_popup_trigger = (event->button == 3);
    jint button = gtk_button_number_to_mouse_button(event->button);

    if (jview && button != com_sun_glass_events_MouseEvent_BUTTON_NONE) {
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                                press ? com_sun_glass_events_MouseEvent_DOWN : com_sun_glass_events_MouseEvent_UP,
                                button,
                                (jint) event->x, (jint) event->y,
                                (jint) event->x_root, (jint) event->y_root,
                                gdk_modifier_mask_to_glass(state),
                                (is_popup_trigger) ? JNI_TRUE : JNI_FALSE,
                                JNI_FALSE);
        CHECK_JNI_EXCEPTION(mainEnv)

        if (jview && is_popup_trigger) {
            mainEnv->CallVoidMethod(jview, jViewNotifyMenu,
                                    (jint) event->x, (jint) event->y,
                                    (jint) event->x_root, (jint) event->y_root,
                                    JNI_FALSE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContext::process_mouse_motion(GdkEventMotion *event) {
    jint glass_modifier = gdk_modifier_mask_to_glass(event->state);
    jint isDrag = glass_modifier & (
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD);
    jint button = com_sun_glass_events_MouseEvent_BUTTON_NONE;

    if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY) {
        button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE) {
        button = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY) {
        button = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK) {
        button = com_sun_glass_events_MouseEvent_BUTTON_BACK;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD) {
        button = com_sun_glass_events_MouseEvent_BUTTON_FORWARD;
    }

    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                                isDrag ? com_sun_glass_events_MouseEvent_DRAG : com_sun_glass_events_MouseEvent_MOVE,
                                button,
                                (jint) event->x, (jint) event->y,
                                (jint) event->x_root, (jint) event->y_root,
                                glass_modifier,
                                JNI_FALSE,
                                JNI_FALSE);
        CHECK_JNI_EXCEPTION(mainEnv)
    }

    gdk_event_request_motions(event);
}

void WindowContext::process_mouse_scroll(GdkEventScroll *event) {
    jdouble dx = 0, dy = 0;

    // converting direction to change in pixels
    switch (event->direction) {
        case GDK_SCROLL_UP:
            dy = 1;
            break;
        case GDK_SCROLL_DOWN:
            dy = -1;
            break;
        case GDK_SCROLL_LEFT:
            dx = 1;
            break;
        case GDK_SCROLL_RIGHT:
            dx = -1;
            break;
        default:
            break;
    }
    if (event->state & GDK_SHIFT_MASK) {
        jdouble t = dy;
        dy = dx;
        dx = t;
    }
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyScroll,
                                (jint) event->x, (jint) event->y,
                                (jint) event->x_root, (jint) event->y_root,
                                dx, dy,
                                gdk_modifier_mask_to_glass(event->state),
                                (jint) 0, (jint) 0,
                                (jint) 0, (jint) 0,
                                (jdouble) 40.0, (jdouble) 40.0);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::process_mouse_cross(GdkEventCrossing *event) {
    bool enter = event->type == GDK_ENTER_NOTIFY;

    if (jview) {
        guint state = event->state;
        if (enter) { // workaround for RT-21590
            state &= ~MOUSE_BUTTONS_MASK;
        }

        if (enter != is_mouse_entered) {
            is_mouse_entered = enter;
            mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                                    enter ? com_sun_glass_events_MouseEvent_ENTER
                                          : com_sun_glass_events_MouseEvent_EXIT,
                                    com_sun_glass_events_MouseEvent_BUTTON_NONE,
                                    (jint) event->x, (jint) event->y,
                                    (jint) event->x_root, (jint) event->y_root,
                                    gdk_modifier_mask_to_glass(state),
                                    JNI_FALSE,
                                    JNI_FALSE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContext::process_key(GdkEventKey *event) {
    bool press = event->type == GDK_KEY_PRESS;
    jint glassKey = get_glass_key(event);
    jint glassModifier = gdk_modifier_mask_to_glass(event->state);
    if (press) {
        glassModifier |= glass_key_to_modifier(glassKey);
    } else {
        glassModifier &= ~glass_key_to_modifier(glassKey);
    }
    jcharArray jChars = NULL;
    jchar key = gdk_keyval_to_unicode(event->keyval);
    if (key >= 'a' && key <= 'z' && (event->state & GDK_CONTROL_MASK)) {
        key = key - 'a' + 1; // map 'a' to ctrl-a, and so on.
    } else {
#ifdef GLASS_GTK2
        if (key == 0) {
            // Work around "bug" fixed in gtk-3.0:
            // http://mail.gnome.org/archives/commits-list/2011-March/msg06832.html
            switch (event->keyval) {
            case 0xFF08 /* Backspace */: key =  '\b';
            case 0xFF09 /* Tab       */: key =  '\t';
            case 0xFF0A /* Linefeed  */: key =  '\n';
            case 0xFF0B /* Vert. Tab */: key =  '\v';
            case 0xFF0D /* Return    */: key =  '\r';
            case 0xFF1B /* Escape    */: key =  '\033';
            case 0xFFFF /* Delete    */: key =  '\177';
            }
        }
#endif
    }

    if (key > 0) {
        jChars = mainEnv->NewCharArray(1);
        if (jChars) {
            mainEnv->SetCharArrayRegion(jChars, 0, 1, &key);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    } else {
        jChars = mainEnv->NewCharArray(0);
    }
    if (jview) {
        if (press) {
            mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                                    com_sun_glass_events_KeyEvent_PRESS,
                                    glassKey,
                                    jChars,
                                    glassModifier);
            CHECK_JNI_EXCEPTION(mainEnv)

            if (jview && key > 0) { // TYPED events should only be sent for printable characters.
                mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                                        com_sun_glass_events_KeyEvent_TYPED,
                                        com_sun_glass_events_KeyEvent_VK_UNDEFINED,
                                        jChars,
                                        glassModifier);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
        } else {
            mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                                    com_sun_glass_events_KeyEvent_RELEASE,
                                    glassKey,
                                    jChars,
                                    glassModifier);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContext::process_state(GdkEventWindowState *event) {
    if (event->changed_mask &
        (GDK_WINDOW_STATE_ICONIFIED | GDK_WINDOW_STATE_MAXIMIZED)) {

        if (event->changed_mask & GDK_WINDOW_STATE_ICONIFIED) {
            is_iconified = event->new_window_state & GDK_WINDOW_STATE_ICONIFIED;
        }
        if (event->changed_mask & GDK_WINDOW_STATE_MAXIMIZED) {
            is_maximized = event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED;
        }

        jint stateChangeEvent;

        if (is_iconified) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MINIMIZE;
        } else if (is_maximized) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MAXIMIZE;
        } else {
            stateChangeEvent = com_sun_glass_events_WindowEvent_RESTORE;
            if ((gdk_windowManagerFunctions & GDK_FUNC_MINIMIZE) == 0) {
                // in this case - the window manager will not support the programatic
                // request to iconify - so we need to restore it now.
                gdk_window_set_functions(gdk_window, gdk_windowManagerFunctions);
            }
        }

        notify_state(stateChangeEvent);
    } else if (event->changed_mask & GDK_WINDOW_STATE_ABOVE) {
        notify_on_top(event->new_window_state & GDK_WINDOW_STATE_ABOVE);
    }
}

void WindowContext::process_net_wm_property() {
    // Workaround for https://bugs.launchpad.net/unity/+bug/998073

    // This is a Unity bug
    if (!g_strcmp0("Unity", gdk_x11_screen_get_window_manager_name(gdk_screen_get_default()))) {
        return;
    }

    static GdkAtom atom_atom = gdk_atom_intern_static_string("ATOM");
    static GdkAtom atom_net_wm_state = gdk_atom_intern_static_string("_NET_WM_STATE");
    static GdkAtom atom_net_wm_state_hidden = gdk_atom_intern_static_string("_NET_WM_STATE_HIDDEN");
    static GdkAtom atom_net_wm_state_above = gdk_atom_intern_static_string("_NET_WM_STATE_ABOVE");

    gint length;
    glong *atoms = NULL;

    if (gdk_property_get(gdk_window, atom_net_wm_state, atom_atom,
                         0, G_MAXLONG, FALSE, NULL, NULL, &length, (guchar * *) & atoms)) {

        bool is_hidden = false;
        bool is_above = false;
        for (gint i = 0; i < (gint)(length / sizeof(glong)); i++) {
            if (atom_net_wm_state_hidden == (GdkAtom) atoms[i]) {
                is_hidden = true;
            } else if (atom_net_wm_state_above == (GdkAtom) atoms[i]) {
                is_above = true;
            }
        }

        g_free(atoms);

        if (is_iconified != is_hidden) {
            is_iconified = is_hidden;

            notify_state((is_hidden)
                         ? com_sun_glass_events_WindowEvent_MINIMIZE
                         : com_sun_glass_events_WindowEvent_RESTORE);
        }

        notify_on_top(is_above);
    }
}

void WindowContext::process_screen_changed() {
    glong to_screen = getScreenPtrForLocation(geometry.current_x, geometry.current_y);
    if (to_screen != -1) {
        if (to_screen != screen) {
            if (jwindow) {
                //notify screen changed
                jobject jScreen = createJavaScreen(mainEnv, to_screen);
                mainEnv->CallVoidMethod(jwindow, jWindowNotifyMoveToAnotherScreen, jScreen);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
            screen = to_screen;
        }
    }
}

void WindowContext::notify_on_top(bool top) {
    // Do not report effective (i.e. native) values to the FX, only if the user sets it manually
    if (top != effective_on_top() && jwindow) {
        if (on_top_inherited() && !top) {
            // Disallow user's "on top" handling on windows that inherited the property
            gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), TRUE);
        } else {
            on_top = top;
            update_ontop_tree(top);
            mainEnv->CallVoidMethod(jwindow,
                                    jWindowNotifyLevelChanged,
                                    top ? com_sun_glass_ui_Window_Level_FLOATING
                                        : com_sun_glass_ui_Window_Level_NORMAL);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }
}

void WindowContext::notify_repaint() {
    int w, h;
    glass_gdk_window_get_size(gdk_window, &w, &h);
    if (jview) {
        mainEnv->CallVoidMethod(jview,
                                jViewNotifyRepaint,
                                0, 0, w, h);
        CHECK_JNI_EXCEPTION(mainEnv);
    }
}

void WindowContext::notify_state(jint glass_state) {
    if (glass_state == com_sun_glass_events_WindowEvent_RESTORE) {
        if (is_maximized) {
            glass_state = com_sun_glass_events_WindowEvent_MAXIMIZE;
        }

        notify_repaint();
    }

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow,
                                jGtkWindowNotifyStateChanged,
                                glass_state);
        CHECK_JNI_EXCEPTION(mainEnv);
    }
}

bool WindowContext::set_view(jobject view) {
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                                com_sun_glass_events_MouseEvent_EXIT,
                                com_sun_glass_events_MouseEvent_BUTTON_NONE,
                                0, 0,
                                0, 0,
                                0,
                                JNI_FALSE,
                                JNI_FALSE);
        mainEnv->DeleteGlobalRef(jview);
    }

    if (view) {
        jview = mainEnv->NewGlobalRef(view);
    } else {
        jview = NULL;
    }
    return TRUE;
}

void WindowContext::set_visible(bool visible) {
    if (visible) {
        gtk_widget_show_all(gtk_widget);
    } else {
        gtk_widget_hide(gtk_widget);
        if (jview && is_mouse_entered) {
            is_mouse_entered = false;
            mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                                    com_sun_glass_events_MouseEvent_EXIT,
                                    com_sun_glass_events_MouseEvent_BUTTON_NONE,
                                    0, 0,
                                    0, 0,
                                    0,
                                    JNI_FALSE,
                                    JNI_FALSE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }

    if (visible) {
        visible_received = TRUE;
    }

    //JDK-8220272 - fire event first because GDK_FOCUS_CHANGE is not always in order
    if (visible && jwindow && isEnabled()) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);
        CHECK_JNI_EXCEPTION(mainEnv);
    }
}

void WindowContext::set_cursor(GdkCursor *cursor) {
// This seems to have no no effect on either Gtk+2 or Gtk+3
//    if (!is_in_drag()) {
//        if (WindowContext::sm_mouse_drag_window) {
//            grab_mouse_drag_focus(WindowContext::sm_mouse_drag_window->get_gdk_window(), NULL, cursor, false);
//        } else if (WindowContext::sm_grab_window) {
//            grab_mouse_drag_focus(WindowContext::sm_grab_window->get_gdk_window(), NULL, cursor, true);
//        }
//    }

    gdk_window_set_cursor(gdk_window, cursor);
}

void WindowContext::set_level(int level) {
    if (level == com_sun_glass_ui_Window_Level_NORMAL) {
        on_top = false;
    } else if (level == com_sun_glass_ui_Window_Level_FLOATING
               || level == com_sun_glass_ui_Window_Level_TOPMOST) {
        on_top = true;
    }
    // We need to emulate always on top behaviour on child windows

    if (!on_top_inherited()) {
        update_ontop_tree(on_top);
    }
}

void WindowContext::set_background(float r, float g, float b) {
    bg_color.red = r;
    bg_color.green = g;
    bg_color.blue = b;
    bg_color.is_set = true;
    notify_repaint();
}

void WindowContext::set_minimized(bool minimize) {
    is_iconified = minimize;
    if (minimize) {
        if (frame_type == TRANSPARENT) {
            // https://bugs.launchpad.net/ubuntu/+source/unity/+bug/1245571
            glass_window_reset_input_shape_mask(gtk_widget_get_window(gtk_widget));
        }

        if ((gdk_windowManagerFunctions & GDK_FUNC_MINIMIZE) == 0) {
            // in this case - the window manager will not support the programatic
            // request to iconify - so we need to disable this until we are restored.
            GdkWMFunction wmf = (GdkWMFunction)(gdk_windowManagerFunctions | GDK_FUNC_MINIMIZE);
            gdk_window_set_functions(gdk_window, wmf);
        }
        gtk_window_iconify(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_deiconify(GTK_WINDOW(gtk_widget));
        activate_window();
    }
}

void WindowContext::set_maximized(bool maximize) {
    is_maximized = maximize;

    if (maximize) {
        // enable the functionality
        GdkWMFunction wmf = (GdkWMFunction)(gdk_windowManagerFunctions | GDK_FUNC_MAXIMIZE);
        gdk_window_set_functions(gdk_window, wmf);

        ensure_window_size();
        gtk_window_maximize(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_unmaximize(GTK_WINDOW(gtk_widget));
    }
}

void WindowContext::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch) {
    // this will tell if adjustments are needed - that's because GTK does not have full window size
    // values, just content values. Frame extents (window decorations) are handled by the window manager.
    geometry.needs_ajustment = (w > 0 || h > 0) || geometry.needs_ajustment;

    // newW / newH always content sizes compatible with GTK+
    // if window has no decoration, adjustments will be ZERO
    int newW, newH;
    newW = (w > 0) ? w - geometry.adjust_w : cw;
    newH = (h > 0) ? h - geometry.adjust_h : ch;

    geometry.current_w = newW;
    geometry.current_h = newH;

    gboolean size_changed = FALSE;
    gboolean pos_changed = FALSE;

    if (newW > 0 && newH > 0) {
        size_changed = TRUE;

        // content size
        geometry.current_cw = newW;
        geometry.current_ch = newH;
        geometry.last_cw = newW;
        geometry.last_ch = newH;

        if (visible_received) {
            // call apply_geometry() to let gtk_window_resize succeed, because it's bound to
            // geometry constraints
            apply_geometry();
            gtk_window_resize(GTK_WINDOW(gtk_widget), newW, newH);
        } else {
            gtk_window_set_default_size(GTK_WINDOW(gtk_widget), newW, newH);
        }
    }

    if (xSet || ySet) {
        int newX = (xSet) ? x : geometry.current_x;
        int newY = (ySet) ? y : geometry.current_y;

        if (newX != geometry.current_x || newY != geometry.current_y) {
            pos_changed = TRUE;
            geometry.current_x = newX;
            geometry.current_y = newY;

            gtk_window_move(GTK_WINDOW(gtk_widget), newX, newY);
        }
    }

    size_position_notify(size_changed, pos_changed);
}

void WindowContext::set_resizable(bool res) {
    if (res != geometry.resizable) {
        geometry.resizable = res;
        apply_geometry();
    }
}

void WindowContext::set_focusable(bool focusable) {
    gtk_window_set_accept_focus(GTK_WINDOW(gtk_widget), focusable ? TRUE : FALSE);
}

void WindowContext::set_title(const char *title) {
    gtk_window_set_title(GTK_WINDOW(gtk_widget), title);
}

void WindowContext::set_alpha(double alpha) {
#ifdef GLASS_GTK3
    gtk_widget_set_opacity(gtk_widget, (gdouble)alpha);
#else
    gtk_window_set_opacity(GTK_WINDOW(gtk_widget), (gdouble)alpha);
#endif
}

void WindowContext::set_enabled(bool enabled) {
    if (enabled != geometry.enabled) {
        gtk_widget_set_sensitive(gtk_widget, enabled);
        geometry.enabled = enabled;
        apply_geometry();
    }
}

void WindowContext::set_minimum_size(int w, int h) {
    gboolean changed = geometry.minw != w || geometry.minh != h;

    if (!changed) {
        return;
    }

    geometry.minw = w;
    geometry.minh = h;

    apply_geometry();
}

void WindowContext::set_maximum_size(int w, int h) {
    gboolean changed = geometry.maxw != w || geometry.maxh != h;

    if (!changed) {
        return;
    }

    geometry.maxw = w;
    geometry.maxh = h;

    apply_geometry();
}

void WindowContext::set_icon(GdkPixbuf *pixbuf) {
    gtk_window_set_icon(GTK_WINDOW(gtk_widget), pixbuf);
}

void WindowContext::set_modal(bool modal, WindowContext *parent) {
    if (modal) {
        //gtk_window_set_type_hint(GTK_WINDOW(gtk_widget), GDK_WINDOW_TYPE_HINT_DIALOG);
        if (parent) {
            gtk_window_set_transient_for(GTK_WINDOW(gtk_widget), parent->get_gtk_window());
        }
    }
    gtk_window_set_modal(GTK_WINDOW(gtk_widget), modal ? TRUE : FALSE);
}

void WindowContext::set_gravity(float x, float y) {
    geometry.gravity_x = x;
    geometry.gravity_y = y;
}

void WindowContext::set_owner(WindowContext *owner_ctx) {
    owner = owner_ctx;
}

void WindowContext::add_child(WindowContext *child) {
    children.insert(child);
    gtk_window_set_transient_for(child->get_gtk_window(), this->get_gtk_window());
}

void WindowContext::remove_child(WindowContext *child) {
    children.erase(child);
    gtk_window_set_transient_for(child->get_gtk_window(), NULL);
}

void WindowContext::show_or_hide_children(bool show) {
    std::set<WindowContext *>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        (*it)->set_minimized(!show);
        (*it)->show_or_hide_children(show);
    }
}

bool WindowContext::is_visible() {
    return gtk_widget_get_visible(gtk_widget);
}

bool WindowContext::is_dead() {
    return can_be_deleted;
}

bool WindowContext::grab_focus() {
    if (WindowContext::sm_mouse_drag_window
            || grab_mouse_drag_focus(gdk_window, NULL, NULL, true)) {
        WindowContext::sm_grab_window = this;
        return true;
    } else {
        return false;
    }
}

void WindowContext::ungrab_focus() {
    if (!WindowContext::sm_mouse_drag_window) {
        ungrab_mouse_drag_focus();
    }

    WindowContext::sm_grab_window = NULL;

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::restack(bool restack) {
    gdk_window_restack(gdk_window, NULL, restack ? TRUE : FALSE);
}

void WindowContext::request_focus() {
    //JDK-8212060: Window show and then move glitch.
    //The WindowContext::set_visible will take care of showing the window.
    //The below code will only handle later request_focus.
    if (is_visible()) {
        gtk_window_present(GTK_WINDOW(gtk_widget));
    }
}

void WindowContext::enter_fullscreen() {
    is_fullscreen = TRUE;

    ensure_window_size();
    gtk_window_fullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContext::exit_fullscreen() {
    is_fullscreen = FALSE;
    gtk_window_unfullscreen(GTK_WINDOW(gtk_widget));
}

// Applied to a temporary full screen window to prevent sending events to Java
void WindowContext::detach_from_java() {
    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
        jview = NULL;
    }
    if (jwindow) {
        mainEnv->DeleteGlobalRef(jwindow);
        jwindow = NULL;
    }
}

void WindowContext::increment_events_counter() {
    ++events_processing_cnt;
}

void WindowContext::decrement_events_counter() {
    --events_processing_cnt;
}

size_t WindowContext::get_events_count() {
    return events_processing_cnt;
}

///////////////////////// PROTECTED

void WindowContext::applyShapeMask(void *data, uint width, uint height) {
    if (frame_type != TRANSPARENT) {
        return;
    }

    glass_window_apply_shape_mask(gtk_widget_get_window(gtk_widget), data, width, height);
}

///////////////////////// PRIVATE

// this is to work-around past gtk+ bug
void WindowContext::ensure_window_size() {
    gint w, h;
#ifdef GLASS_GTK3
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h);
#else
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h, NULL);
#endif
    if ((geometry.last_cw > 0 && geometry.last_ch > 0)
        && (geometry.last_cw != w || geometry.last_ch != h)) {
        gdk_window_resize(gdk_window, geometry.last_cw, geometry.last_ch);
    }
}

// This function calculate the deltas between window and window + decoration (titlebar, borders).
// It's used when the window manager does not support the _NET_FRAME_EXTENTS extension or when
// it's not received on time.
void WindowContext::calculate_adjustments() {
    if (frame_type != TITLED || geometry.frame_extents_received) {
        return;
    }

    gint x, y, rx, ry;
    gdk_window_get_origin(gdk_window, &x, &y);
    gdk_window_get_root_origin(gdk_window, &rx, &ry);

    if (rx != x || ry != y) {
        // the left extends are correct - the right one is guessed to be the same
        geometry.adjust_w = (x - rx) * 2;
        // guess that bottom size is the same as left and right
        geometry.adjust_h = (y - ry) + (x - rx);

        // those will be correct
        geometry.view_x = (x - rx);
        geometry.view_y = (y - ry);

        if (geometry.needs_ajustment) {
            set_bounds(0, 0, false, false, geometry.current_w, geometry.current_h, -1, -1);
        }

        // force position notify so java will know about view_y and view_x
        size_position_notify(false, true);
    }
}

void WindowContext::apply_geometry() {
    if (!map_received) {
        return;
    }

    GdkGeometry gdk_geometry;
    gdk_geometry.win_gravity = GDK_GRAVITY_NORTH_WEST;

    if ((!geometry.resizable || !geometry.enabled) && !(is_maximized || is_fullscreen)) {
        // not resizeable
        int w = geometry.current_cw > 0
                ? geometry.current_cw
                : geometry.current_w - geometry.adjust_w;

        int h = geometry.current_ch > 0
                ? geometry.current_ch
                : geometry.current_h - geometry.adjust_h;

        gdk_geometry.min_width = gdk_geometry.max_width = w;
        gdk_geometry.min_height = gdk_geometry.max_height = h;
    } else {
        //min/max width/height always whole window size (with decors)
        gdk_geometry.min_width = (geometry.minw - geometry.adjust_w) > 0
                                 ? geometry.minw - geometry.adjust_w : 1;
        gdk_geometry.min_height = (geometry.minh - geometry.adjust_h) > 0
                                  ? geometry.minh - geometry.adjust_h : 1;

        gdk_geometry.max_width = (geometry.maxw - geometry.adjust_w > 0)
                                 ? geometry.maxw - geometry.adjust_w : G_MAXINT;
        gdk_geometry.max_height = (geometry.maxh - geometry.adjust_h > 0)
                                  ? geometry.maxh - geometry.adjust_h : G_MAXINT;
    }

    gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &gdk_geometry,
                                  (GdkWindowHints)(GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE | GDK_HINT_WIN_GRAVITY));
}

bool WindowContext::get_frame_extents_property(int *top, int *left,
                                               int *bottom, int *right) {
    unsigned long *extents;

    if (gdk_property_get(gdk_window,
                         atom_net_wm_frame_extents,
                         gdk_atom_intern("CARDINAL", FALSE),
                         0,
                         sizeof(unsigned long) * 4,
                         FALSE,
                         NULL,
                         NULL,
                         NULL,
                         (guchar * *) & extents)) {
        *left = extents[0];
        *right = extents[1];
        *top = extents[2];
        *bottom = extents[3];

        g_free(extents);
        return true;
    }

    return false;
}

void WindowContext::activate_window() {
    Display *display = GDK_DISPLAY_XDISPLAY(gdk_window_get_display(gdk_window));
    Atom navAtom = XInternAtom(display, "_NET_ACTIVE_WINDOW", True);
    if (navAtom != None) {
        XClientMessageEvent clientMessage;
        memset(&clientMessage, 0, sizeof(clientMessage));

        clientMessage.type = ClientMessage;
        clientMessage.window = GDK_WINDOW_XID(gdk_window);
        clientMessage.message_type = navAtom;
        clientMessage.format = 32;
        clientMessage.data.l[0] = 1;
        clientMessage.data.l[1] = gdk_x11_get_server_time(gdk_window);
        clientMessage.data.l[2] = 0;

        XSendEvent(display, XDefaultRootWindow(display), False,
                   SubstructureRedirectMask | SubstructureNotifyMask,
                   (XEvent * ) & clientMessage);
        XFlush(display);
    }
}

void WindowContext::size_position_notify(bool size_changed, bool pos_changed) {

    if (jview) {
        if (size_changed) {
            mainEnv->CallVoidMethod(jview, jViewNotifyResize, geometry.current_cw, geometry.current_ch);
            CHECK_JNI_EXCEPTION(mainEnv);
        }

        if (pos_changed) {
            mainEnv->CallVoidMethod(jview, jViewNotifyView, com_sun_glass_events_ViewEvent_MOVE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }

    if (jwindow) {
        if (size_changed || is_maximized) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                                    (is_maximized)
                                    ? com_sun_glass_events_WindowEvent_MAXIMIZE
                                    : com_sun_glass_events_WindowEvent_RESIZE,
                                    geometry.current_w, geometry.current_h);
            CHECK_JNI_EXCEPTION(mainEnv)
        }

        if (pos_changed) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyMove, geometry.current_x, geometry.current_y);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContext::update_ontop_tree(bool on_top) {
    bool effective_on_top = on_top || this->on_top;
    gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), effective_on_top ? TRUE : FALSE);
    for (std::set<WindowContext *>::iterator it = children.begin(); it != children.end(); ++it) {
        (*it)->update_ontop_tree(effective_on_top);
    }
}

bool WindowContext::on_top_inherited() {
    WindowContext *o = owner;
    while (o) {
        WindowContext *topO = dynamic_cast<WindowContext *>(o);
        if (!topO) break;
        if (topO->on_top) {
            return true;
        }
        o = topO->owner;
    }
    return false;
}

bool WindowContext::effective_on_top() {
    if (owner) {
        WindowContext *topO = dynamic_cast<WindowContext *>(owner);
        return (topO && topO->effective_on_top()) || on_top;
    }
    return on_top;
}

bool WindowContext::grab_mouse_drag_focus(GdkWindow * gdk_w, GdkEvent * event, GdkCursor * cursor, bool owner_events) {
    if (is_grab_disabled()) {
        return true;
    }

    ungrab_mouse_drag_focus();

#ifdef GLASS_GTK3
    if (event != NULL) {
        grab_pointer = gdk_event_get_device (event);
    } else {
        grab_pointer = gdk_device_manager_get_client_pointer(gdk_display_get_device_manager(gtk_widget_get_display(gtk_widget)));
    }

    GdkGrabStatus status = gdk_device_grab((GdkDevice *) grab_pointer, gdk_w, GDK_OWNERSHIP_WINDOW, owner_events,
                                                (GdkEventMask)
                                                      (GDK_POINTER_MOTION_MASK
                                                          | GDK_POINTER_MOTION_HINT_MASK
                                                          | GDK_BUTTON_MOTION_MASK
                                                          | GDK_BUTTON1_MOTION_MASK
                                                          | GDK_BUTTON2_MOTION_MASK
                                                          | GDK_BUTTON3_MOTION_MASK
                                                          | GDK_BUTTON_PRESS_MASK
                                                          | GDK_BUTTON_RELEASE_MASK), cursor, GDK_CURRENT_TIME);
#else
    GdkGrabStatus status = gdk_pointer_grab(gdk_w, owner_events,
                                                (GdkEventMask)
                                                      (GDK_POINTER_MOTION_MASK
                                                          | GDK_POINTER_MOTION_HINT_MASK
                                                          | GDK_BUTTON_MOTION_MASK
                                                          | GDK_BUTTON1_MOTION_MASK
                                                          | GDK_BUTTON2_MOTION_MASK
                                                          | GDK_BUTTON3_MOTION_MASK
                                                          | GDK_BUTTON_PRESS_MASK
                                                          | GDK_BUTTON_RELEASE_MASK), NULL, cursor, GDK_CURRENT_TIME);
#endif
    WindowContext::sm_mouse_drag_window = this;

    return (status == GDK_GRAB_SUCCESS) ? true : false;
}

void WindowContext::ungrab_mouse_drag_focus() {
    if (!grab_pointer) {
        return;
    }

#ifdef GLASS_GTK3
    gdk_device_ungrab((GdkDevice *) grab_pointer, GDK_CURRENT_TIME);
#else
    gdk_pointer_ungrab(GDK_CURRENT_TIME);
#endif
    grab_pointer = NULL;
    WindowContext::sm_mouse_drag_window = NULL;

    if (WindowContext::sm_grab_window) {
        WindowContext::sm_grab_window->grab_focus();
    }
}

WindowContext::~WindowContext() {
    if (xim.ic) {
        XDestroyIC(xim.ic);
        xim.ic = NULL;
    }
    if (xim.im) {
        XCloseIM(xim.im);
        xim.im = NULL;
    }

    gtk_widget_destroy(gtk_widget);
}
