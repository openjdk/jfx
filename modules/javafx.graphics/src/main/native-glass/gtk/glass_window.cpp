/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_general.h"
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

WindowContext * WindowContextBase::sm_grab_window = NULL;
WindowContext * WindowContextBase::sm_mouse_drag_window = NULL;

GdkWindow* WindowContextBase::get_gdk_window(){
    return gdk_window;
}

jobject WindowContextBase::get_jview() {
    return jview;
}

jobject WindowContextBase::get_jwindow() {
    return jwindow;
}

bool WindowContextBase::isEnabled() {
    if (jwindow) {
        bool result = (JNI_TRUE == mainEnv->CallBooleanMethod(jwindow, jWindowIsEnabled));
        LOG_EXCEPTION(mainEnv)
        return result;
    } else {
        return false;
    }
}

void WindowContextBase::notify_state(jint glass_state) {
    if (glass_state == com_sun_glass_events_WindowEvent_RESTORE) {
        if (is_maximized) {
            glass_state = com_sun_glass_events_WindowEvent_MAXIMIZE;
        }

        int w, h;
        glass_gdk_window_get_size(gdk_window, &w, &h);
        if (jview) {
            mainEnv->CallVoidMethod(jview,
                    jViewNotifyRepaint,
                    0, 0, w, h);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }

    if (jwindow) {
       mainEnv->CallVoidMethod(jwindow,
               jGtkWindowNotifyStateChanged,
               glass_state);
       CHECK_JNI_EXCEPTION(mainEnv);
    }
}

void WindowContextBase::process_state(GdkEventWindowState* event) {
    if (event->changed_mask & (GDK_WINDOW_STATE_ICONIFIED | GDK_WINDOW_STATE_MAXIMIZED)) {

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
            if ((gdk_windowManagerFunctions & GDK_FUNC_MINIMIZE) == 0
                || (gdk_windowManagerFunctions & GDK_FUNC_MAXIMIZE) == 0) {
                // in this case - the window manager will not support the programatic
                // request to iconify / maximize - so we need to restore it now.
                gdk_window_set_functions(gdk_window, gdk_windowManagerFunctions);
            }
        }

        notify_state(stateChangeEvent);
    } else if (event->changed_mask & GDK_WINDOW_STATE_ABOVE) {
        notify_on_top(event->new_window_state & GDK_WINDOW_STATE_ABOVE);
    }
}

void WindowContextBase::process_focus(GdkEventFocus* event) {
    if (!event->in && WindowContextBase::sm_grab_window == this) {
        ungrab_focus();
    }

    if (im_ctx.enabled && im_ctx.ctx) {
        if (event->in) {
            gtk_im_context_focus_in(im_ctx.ctx);
        } else {
            gtk_im_context_focus_out(im_ctx.ctx);
        }
    }

    if (jwindow) {
        if (!event->in || isEnabled()) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus,
                    event->in ? com_sun_glass_events_WindowEvent_FOCUS_GAINED
                              : com_sun_glass_events_WindowEvent_FOCUS_LOST);
            CHECK_JNI_EXCEPTION(mainEnv)
        } else {
            // when the user tries to activate a disabled window, send FOCUS_DISABLED
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusDisabled);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContextBase::increment_events_counter() {
    ++events_processing_cnt;
}

void WindowContextBase::decrement_events_counter() {
    --events_processing_cnt;
}

size_t WindowContextBase::get_events_count() {
    return events_processing_cnt;
}

bool WindowContextBase::is_dead() {
    return can_be_deleted;
}

void destroy_and_delete_ctx(WindowContext* ctx) {
    if (ctx) {
        ctx->process_destroy();

        if (!ctx->get_events_count()) {
            delete ctx;
        }
        // else: ctx will be deleted in EventsCounterHelper after completing
        // an event processing
    }
}

void WindowContextBase::process_destroy() {
    if (WindowContextBase::sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }

    if (WindowContextBase::sm_grab_window == this) {
        ungrab_focus();
    }

    std::set<WindowContextTop*>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        // FIX JDK-8226537: this method calls set_owner(NULL) which prevents
        // WindowContextTop::process_destroy() to call remove_child() (because children
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

void WindowContextBase::process_delete() {
    if (jwindow && isEnabled()) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyClose);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContextBase::process_expose(GdkEventExpose* event) {
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyRepaint, event->area.x, event->area.y, event->area.width, event->area.height);
        CHECK_JNI_EXCEPTION(mainEnv)
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

void WindowContextBase::process_mouse_button(GdkEventButton* event) {
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
    }

    if (!press) {
        if ((event->state & MOUSE_BUTTONS_MASK) && !(state & MOUSE_BUTTONS_MASK)) { // all buttons released
            ungrab_mouse_drag_focus();
        } else if (event->button == 8 || event->button == 9) {
            // GDK X backend interprets button press events for buttons 4-7 as
            // scroll events so GDK_BUTTON4_MASK and GDK_BUTTON5_MASK will never
            // be set on the event->state from GDK. Thus we cannot check if all
            // buttons have been released in the usual way (as above).
            ungrab_mouse_drag_focus();
        }
    }

    jint button = gtk_button_number_to_mouse_button(event->button);

    if (jview && button != com_sun_glass_events_MouseEvent_BUTTON_NONE) {
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                press ? com_sun_glass_events_MouseEvent_DOWN : com_sun_glass_events_MouseEvent_UP,
                button,
                (jint) event->x, (jint) event->y,
                (jint) event->x_root, (jint) event->y_root,
                gdk_modifier_mask_to_glass(state),
                (event->button == 3 && press) ? JNI_TRUE : JNI_FALSE,
                JNI_FALSE);
        CHECK_JNI_EXCEPTION(mainEnv)

        if (jview && event->button == 3 && press) {
            mainEnv->CallVoidMethod(jview, jViewNotifyMenu,
                    (jint)event->x, (jint)event->y,
                    (jint)event->x_root, (jint)event->y_root,
                    JNI_FALSE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContextBase::process_mouse_motion(GdkEventMotion* event) {
    jint glass_modifier = gdk_modifier_mask_to_glass(event->state);
    jint isDrag = glass_modifier & (
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD);
    jint button = com_sun_glass_events_MouseEvent_BUTTON_NONE;

    if (isDrag && WindowContextBase::sm_mouse_drag_window == NULL) {
        // Upper layers expects from us Windows behavior:
        // all mouse events should be delivered to window where drag begins
        // and no exit/enter event should be reported during this drag.
        // We can grab mouse pointer for these needs.
        grab_mouse_drag_focus();
    }

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
}

void WindowContextBase::process_mouse_scroll(GdkEventScroll* event) {
    jdouble dx = 0;
    jdouble dy = 0;

    // converting direction to change in pixels
    switch (event->direction) {
#if GTK_CHECK_VERSION(3, 4, 0)
        case GDK_SCROLL_SMOOTH:
            //FIXME 3.4 ???
            break;
#endif
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

void WindowContextBase::process_mouse_cross(GdkEventCrossing* event) {
    bool enter = event->type == GDK_ENTER_NOTIFY;
    if (jview) {
        guint state = event->state;
        if (enter) { // workaround for JDK-8126843
            state &= ~MOUSE_BUTTONS_MASK;
        }

        if (enter != is_mouse_entered) {
            is_mouse_entered = enter;
            mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                    enter ? com_sun_glass_events_MouseEvent_ENTER : com_sun_glass_events_MouseEvent_EXIT,
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

void WindowContextBase::process_key(GdkEventKey* event) {
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

    if (!jview) {
        return;
    }

    mainEnv->CallVoidMethod(jview, jViewNotifyKey,
            (press) ? com_sun_glass_events_KeyEvent_PRESS
                    : com_sun_glass_events_KeyEvent_RELEASE,
            glassKey,
            jChars,
            glassModifier);
    CHECK_JNI_EXCEPTION(mainEnv)

    // jview is checked again because previous call might be an exit key
    if (press && key > 0 && jview) { // TYPED events should only be sent for printable characters.
        mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                com_sun_glass_events_KeyEvent_TYPED,
                com_sun_glass_events_KeyEvent_VK_UNDEFINED,
                jChars,
                glassModifier);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContextBase::paint(void* data, jint width, jint height) {
#ifdef GLASS_GTK3
    cairo_rectangle_int_t rect = {0, 0, width, height};
    cairo_region_t *region = cairo_region_create_rectangle(&rect);
    gdk_window_begin_paint_region(gdk_window, region);
#endif
    cairo_t* context = gdk_cairo_create(gdk_window);

    cairo_surface_t* cairo_surface =
        cairo_image_surface_create_for_data(
            (unsigned char*)data,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    applyShapeMask(data, width, height);

    cairo_set_source_surface(context, cairo_surface, 0, 0);
    cairo_set_operator(context, CAIRO_OPERATOR_SOURCE);
    cairo_paint(context);

#ifdef GLASS_GTK3
    gdk_window_end_paint(gdk_window);
    cairo_region_destroy(region);
#endif

    cairo_destroy(context);
    cairo_surface_destroy(cairo_surface);
}

void WindowContextBase::add_child(WindowContextTop* child) {
    children.insert(child);
    gtk_window_set_transient_for(child->get_gtk_window(), this->get_gtk_window());
}

void WindowContextBase::remove_child(WindowContextTop* child) {
    children.erase(child);
    gtk_window_set_transient_for(child->get_gtk_window(), NULL);
}

void WindowContextBase::set_visible(bool visible) {
    if (visible) {
        gtk_widget_show(gtk_widget);
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
}

bool WindowContextBase::is_visible() {
    return gtk_widget_get_visible(gtk_widget);
}

bool WindowContextBase::set_view(jobject view) {
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

bool WindowContextBase::grab_mouse_drag_focus() {
    if (glass_gdk_mouse_devices_grab_with_cursor(
            gdk_window, gdk_window_get_cursor(gdk_window), FALSE)) {
        WindowContextBase::sm_mouse_drag_window = this;
        return true;
    } else {
        return false;
    }
}

void WindowContextBase::ungrab_mouse_drag_focus() {
    WindowContextBase::sm_mouse_drag_window = NULL;
    glass_gdk_mouse_devices_ungrab();
    if (WindowContextBase::sm_grab_window) {
        WindowContextBase::sm_grab_window->grab_focus();
    }
}

bool WindowContextBase::grab_focus() {
    if (WindowContextBase::sm_mouse_drag_window
            || glass_gdk_mouse_devices_grab(gdk_window)) {
        WindowContextBase::sm_grab_window = this;
        return true;
    } else {
        return false;
    }
}

void WindowContextBase::ungrab_focus() {
    if (!WindowContextBase::sm_mouse_drag_window) {
        glass_gdk_mouse_devices_ungrab();
    }
    WindowContextBase::sm_grab_window = NULL;

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContextBase::set_cursor(GdkCursor* cursor) {
    if (!is_in_drag()) {
        if (WindowContextBase::sm_mouse_drag_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    WindowContextBase::sm_mouse_drag_window->get_gdk_window(), cursor, FALSE);
        } else if (WindowContextBase::sm_grab_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    WindowContextBase::sm_grab_window->get_gdk_window(), cursor, TRUE);
        }
    }
    gdk_window_set_cursor(gdk_window, cursor);
}

void WindowContextBase::set_background(float r, float g, float b) {
    GdkRGBA rgba = {r, g, b, 1.};
    gtk_widget_override_background_color(gtk_widget, GTK_STATE_FLAG_NORMAL, &rgba);
}

WindowContextBase::~WindowContextBase() {
    disableIME();
    gtk_widget_destroy(gtk_widget);
}

////////////////////////////// WindowContextTop /////////////////////////////////


// Work-around because frame extents are only obtained after window is shown.
// This is used to know the total window size (content + decoration)
// The first window will have a duplicated resize event, subsequent windows will use the cached value.
WindowFrameExtents WindowContextTop::normal_extents = {0, 0, 0, 0};
WindowFrameExtents WindowContextTop::utility_extents = {0, 0, 0, 0};


static void event_realize(GtkWidget* self, gpointer user_data) {
    WindowContextTop *ctx = ((WindowContextTop *) user_data);
    ctx->process_realize();
}

static int geometry_get_window_width(const WindowGeometry *windowGeometry) {
     return (windowGeometry->final_width.type == BOUNDSTYPE_WINDOW)
                   ? windowGeometry->final_width.value
                   : windowGeometry->final_width.value
                         + windowGeometry->extents.left
                         + windowGeometry->extents.right;
}

static int geometry_get_window_height(const WindowGeometry *windowGeometry) {
    return (windowGeometry->final_height.type == BOUNDSTYPE_WINDOW)
                   ? windowGeometry->final_height.value
                   : windowGeometry->final_height.value
                         + windowGeometry->extents.top
                         + windowGeometry->extents.bottom;
}

static int geometry_get_content_width(WindowGeometry *windowGeometry) {
    return (windowGeometry->final_width.type == BOUNDSTYPE_CONTENT)
                   ? windowGeometry->final_width.value
                   : windowGeometry->final_width.value
                         - windowGeometry->extents.left
                         - windowGeometry->extents.right;
}

static int geometry_get_content_height(WindowGeometry *windowGeometry) {
    return (windowGeometry->final_height.type == BOUNDSTYPE_CONTENT)
                   ? windowGeometry->final_height.value
                   : windowGeometry->final_height.value
                         - windowGeometry->extents.top
                         - windowGeometry->extents.bottom;
}

static GdkAtom get_net_frame_extents_atom() {
    static const char * extents_str = "_NET_FRAME_EXTENTS";
    return gdk_atom_intern(extents_str, FALSE);
}

WindowContextTop::WindowContextTop(jobject _jwindow, WindowContext* _owner, long _screen,
        WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf) :
            WindowContextBase(),
            screen(_screen),
            frame_type(_frame_type),
            window_type(type),
            owner(_owner),
            geometry(),
            resizable(),
            on_top(false),
            is_fullscreen(false) {
    jwindow = mainEnv->NewGlobalRef(_jwindow);
    gdk_windowManagerFunctions = wmf;

    gtk_widget = gtk_window_new(type == POPUP ? GTK_WINDOW_POPUP : GTK_WINDOW_TOPLEVEL);
    g_signal_connect(G_OBJECT(gtk_widget), "realize", G_CALLBACK(event_realize), this);

    if (gchar* app_name = get_application_name()) {
        gtk_window_set_wmclass(GTK_WINDOW(gtk_widget), app_name, app_name);
        g_free(app_name);
    }

    if (owner) {
        owner->add_child(this);
        if (on_top_inherited()) {
            gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), TRUE);
        }
    }

    if (type == UTILITY) {
        gtk_window_set_type_hint(GTK_WINDOW(gtk_widget), GDK_WINDOW_TYPE_HINT_UTILITY);
    }

    const char* wm_name = gdk_x11_screen_get_window_manager_name(gdk_screen_get_default());
    wmanager = (g_strcmp0("Compiz", wm_name) == 0) ? COMPIZ : UNKNOWN;

//    glong xdisplay = (glong)mainEnv->GetStaticLongField(jApplicationCls, jApplicationDisplay);
//    gint  xscreenID = (gint)mainEnv->GetStaticIntField(jApplicationCls, jApplicationScreen);
    glong xvisualID = (glong)mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);

    if (xvisualID != 0) {
        GdkVisual *visual = gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID);
        glass_gtk_window_configure_from_visual(gtk_widget, visual);
    }

    gtk_widget_set_events(gtk_widget, GDK_FILTERED_EVENTS_MASK);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);

    glass_configure_window_transparency(gtk_widget, frame_type == TRANSPARENT);
    gtk_window_set_title(GTK_WINDOW(gtk_widget), "");

    if (frame_type != TITLED) {
        gtk_window_set_decorated(GTK_WINDOW(gtk_widget), FALSE);
    } else {
        geometry.extents = get_cached_extents();
    }
}

// Applied to a temporary full screen window to prevent sending events to Java
void WindowContextTop::detach_from_java() {
    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
        jview = NULL;
    }
    if (jwindow) {
        mainEnv->DeleteGlobalRef(jwindow);
        jwindow = NULL;
    }
}

void WindowContextTop::request_frame_extents() {
    Display *display = GDK_DISPLAY_XDISPLAY(gdk_window_get_display(gdk_window));
    static Atom rfeAtom = XInternAtom(display, "_NET_REQUEST_FRAME_EXTENTS", False);

    if (rfeAtom != None) {
        XClientMessageEvent clientMessage;
        memset(&clientMessage, 0, sizeof(clientMessage));

        clientMessage.type = ClientMessage;
        clientMessage.window = GDK_WINDOW_XID(gdk_window);
        clientMessage.message_type = rfeAtom;
        clientMessage.format = 32;

        XSendEvent(display, XDefaultRootWindow(display), False,
                   SubstructureRedirectMask | SubstructureNotifyMask,
                   (XEvent *) &clientMessage);
        XFlush(display);
    }
}

void WindowContextTop::update_frame_extents() {
    int top, left, bottom, right;

    if (get_frame_extents_property(&top, &left, &bottom, &right)) {
        if (top > 0 || right > 0 || bottom > 0 || left > 0) {
            bool changed = geometry.extents.top != top
                            || geometry.extents.left != left
                            || geometry.extents.bottom != bottom
                            || geometry.extents.right != right;

            if (changed) {
                geometry.extents.top = top;
                geometry.extents.left = left;
                geometry.extents.bottom = bottom;
                geometry.extents.right = right;

                set_cached_extents(geometry.extents);

                // set bounds again to correct window size
                // accounting decorations
                int w = geometry_get_window_width(&geometry);
                int h = geometry_get_window_height(&geometry);
                int cw = geometry_get_content_width(&geometry);
                int ch = geometry_get_content_height(&geometry);

                int x = geometry.x;
                int y = geometry.y;

                if (geometry.gravity_x != 0) {
                    x -= geometry.gravity_x * (float) (left + right);
                }

                if (geometry.gravity_y != 0) {
                    y -= geometry.gravity_y * (float) (top + bottom);
                }

                set_bounds(x, y, true, true, w, h, cw, ch, 0, 0);
           }
        }
    }
}

void WindowContextTop::set_cached_extents(WindowFrameExtents ex) {
    if (window_type == NORMAL) {
        normal_extents = ex;
    } else {
        utility_extents = ex;
    }
}

WindowFrameExtents WindowContextTop::get_cached_extents() {
    return window_type == NORMAL ? normal_extents : utility_extents;
}

bool WindowContextTop::get_frame_extents_property(int *top, int *left,
        int *bottom, int *right) {
    unsigned long *extents;

    if (gdk_property_get(gdk_window,
            get_net_frame_extents_atom(),
            gdk_atom_intern("CARDINAL", FALSE),
            0,
            sizeof (unsigned long) * 4,
            FALSE,
            NULL,
            NULL,
            NULL,
            (guchar**) & extents)) {
        *left = extents [0];
        *right = extents [1];
        *top = extents [2];
        *bottom = extents [3];

        g_free(extents);
        return true;
    }

    return false;
}

void WindowContextTop::work_around_compiz_state() {
    // Workaround for https://bugs.launchpad.net/unity/+bug/998073
    if (wmanager != COMPIZ) {
        return;
    }

    static GdkAtom atom_atom = gdk_atom_intern_static_string("ATOM");
    static GdkAtom atom_net_wm_state = gdk_atom_intern_static_string("_NET_WM_STATE");
    static GdkAtom atom_net_wm_state_hidden = gdk_atom_intern_static_string("_NET_WM_STATE_HIDDEN");
    static GdkAtom atom_net_wm_state_above = gdk_atom_intern_static_string("_NET_WM_STATE_ABOVE");

    gint length;

    glong* atoms = NULL;

    if (gdk_property_get(gdk_window, atom_net_wm_state, atom_atom,
            0, G_MAXLONG, FALSE, NULL, NULL, &length, (guchar**) &atoms)) {

        bool is_hidden = false;
        bool is_above = false;
        for (gint i = 0; i < (gint)(length / sizeof(glong)); i++) {
            if (atom_net_wm_state_hidden == (GdkAtom)atoms[i]) {
                is_hidden = true;
            } else if (atom_net_wm_state_above == (GdkAtom)atoms[i]) {
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

void WindowContextTop::process_property_notify(GdkEventProperty* event) {
    static GdkAtom atom_net_wm_state = gdk_atom_intern_static_string("_NET_WM_STATE");

    if (event->window == gdk_window) {
        if (event->atom == get_net_frame_extents_atom()) {
            update_frame_extents();
        } else if (event->atom == atom_net_wm_state) {
            work_around_compiz_state();
        }
    }
}

void WindowContextTop::process_state(GdkEventWindowState* event) {
    if (event->changed_mask & GDK_WINDOW_STATE_FULLSCREEN) {
        is_fullscreen = event->new_window_state & GDK_WINDOW_STATE_FULLSCREEN;
    }

    if (event->changed_mask & GDK_WINDOW_STATE_MAXIMIZED
        && !(event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED)) {
        gtk_window_resize(GTK_WINDOW(gtk_widget), geometry_get_content_width(&geometry),
                                    geometry_get_content_height(&geometry));
    }

    WindowContextBase::process_state(event);
}

void WindowContextTop::process_realize() {
    gdk_window = gtk_widget_get_window(gtk_widget);
    if (frame_type == TITLED) {
        request_frame_extents();
    }

    gdk_window_set_events(gdk_window, GDK_FILTERED_EVENTS_MASK);
    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);
    gdk_window_register_dnd(gdk_window);

    if (gdk_windowManagerFunctions) {
        gdk_window_set_functions(gdk_window, gdk_windowManagerFunctions);
    }
}

void WindowContextTop::process_configure(GdkEventConfigure* event) {
    int ww = event->width + geometry.extents.left + geometry.extents.right;
    int wh = event->height + geometry.extents.top + geometry.extents.bottom;

    // Do not report if iconified, because Java side would set the state to NORMAL
    if (jwindow && !is_iconified) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                (is_maximized)
                    ? com_sun_glass_events_WindowEvent_MAXIMIZE
                    : com_sun_glass_events_WindowEvent_RESIZE,
                ww, wh);
        CHECK_JNI_EXCEPTION(mainEnv)

        if (jview) {
            mainEnv->CallVoidMethod(jview, jViewNotifyResize, event->width, event->height);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }

    if (!is_iconified && !is_fullscreen && !is_maximized) {
        geometry.final_width.value = (geometry.final_width.type == BOUNDSTYPE_CONTENT)
                ? event->width : ww;

        geometry.final_height.value = (geometry.final_height.type == BOUNDSTYPE_CONTENT)
                ? event->height : wh;
    }

    gint root_x, root_y, origin_x, origin_y;
    gdk_window_get_root_origin(gdk_window, &root_x, &root_y);
    gdk_window_get_origin(gdk_window, &origin_x, &origin_y);

    // x and y represent the position of the top-left corner of the window relative to the desktop area
    geometry.x = root_x;
    geometry.y = root_y;

    // view_x and view_y represent the position of the content relative to the top-left corner of the window,
    // taking into account window decorations (such as title bars and borders) applied by the window manager.
    geometry.view_x = origin_x - root_x;
    geometry.view_y = origin_y - root_y;
    notify_window_move();

    glong to_screen = getScreenPtrForLocation(geometry.x, geometry.y);
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

void WindowContextTop::update_window_constraints() {
    bool is_floating = !is_iconified && !is_fullscreen && !is_maximized;

    if (!is_floating) {
        // window is not floating on the screen
        return;
    }

    GdkGeometry hints;

    if (resizable.value && !is_disabled) {
        int min_w = (resizable.minw == -1) ? 1
                      : resizable.minw - geometry.extents.left - geometry.extents.right;
        int min_h =  (resizable.minh == -1) ? 1
                      : resizable.minh - geometry.extents.top - geometry.extents.bottom;

        hints.min_width = (min_w < 1) ? 1 : min_w;
        hints.min_height = (min_h < 1) ? 1 : min_h;

        hints.max_width = (resizable.maxw == -1) ? G_MAXINT
                            : resizable.maxw - geometry.extents.left - geometry.extents.right;

        hints.max_height = (resizable.maxh == -1) ? G_MAXINT
                           : resizable.maxh - geometry.extents.top - geometry.extents.bottom;
    } else {
        int w = geometry_get_content_width(&geometry);
        int h = geometry_get_content_height(&geometry);

        hints.min_width = w;
        hints.min_height = h;
        hints.max_width = w;
        hints.max_height = h;
    }

    gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &hints,
                                  (GdkWindowHints)(GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
}

void WindowContextTop::set_resizable(bool res) {
    resizable.value = res;
    update_window_constraints();
}

void WindowContextTop::set_visible(bool visible) {
    WindowContextBase::set_visible(visible);

    if (visible && !geometry.size_assigned) {
        set_bounds(0, 0, false, false, 320, 200, -1, -1, 0, 0);
    }

    //JDK-8220272 - fire event first because GDK_FOCUS_CHANGE is not always in order
    if (visible && jwindow && isEnabled()) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);
        CHECK_JNI_EXCEPTION(mainEnv);
    }
}

void WindowContextTop::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch,
                                  float gravity_x, float gravity_y) {
//     fprintf(stderr, "set_bounds -> x = %d, y = %d, xset = %d, yset = %d, w = %d, h = %d, cw = %d, ch = %d, gx = %f, gy = %f\n",
//            x, y, xSet, ySet, w, h, cw, ch, gravity_x, gravity_y);
    // newW / newH are view/content sizes
    int newW = 0;
    int newH = 0;

    geometry.gravity_x = gravity_x;
    geometry.gravity_y = gravity_y;

    if (w > 0) {
        geometry.final_width.type = BOUNDSTYPE_WINDOW;
        geometry.final_width.value = w;
        newW = w - (geometry.extents.left + geometry.extents.right);
    } else if (cw > 0) {
        geometry.final_width.type = BOUNDSTYPE_CONTENT;
        geometry.final_width.value = cw;
        newW = cw;
    } else {
        newW = geometry_get_content_width(&geometry);
    }

    if (h > 0) {
        geometry.final_height.type = BOUNDSTYPE_WINDOW;
        geometry.final_height.value = h;
        newH = h - (geometry.extents.top + geometry.extents.bottom);
    } else if (ch > 0) {
        geometry.final_height.type = BOUNDSTYPE_CONTENT;
        geometry.final_height.value = ch;
        newH = ch;
    } else {
        newH = geometry_get_content_height(&geometry);
    }


    if (newW > 0 || newH > 0) {
        // call update_window_constraints() to let gtk_window_resize succeed, because it's bound to geometry constraints
        update_window_constraints();

        if (gtk_widget_get_realized(gtk_widget)) {
            gtk_window_resize(GTK_WINDOW(gtk_widget), newW, newH);
        } else {
            gtk_window_set_default_size(GTK_WINDOW(gtk_widget), newW, newH);
        }
        geometry.size_assigned = true;
        notify_window_resize();
    }

    if (xSet || ySet) {
        if (xSet) {
            geometry.x = x;
        }

        if (ySet) {
            geometry.y = y;
        }

        gtk_window_move(GTK_WINDOW(gtk_widget), geometry.x, geometry.y);
        notify_window_move();
    }
}

void WindowContextTop::applyShapeMask(void* data, uint width, uint height) {
    if (frame_type != TRANSPARENT) {
        return;
    }

    glass_window_apply_shape_mask(gtk_widget_get_window(gtk_widget), data, width, height);
}

void WindowContextTop::set_minimized(bool minimize) {
    is_iconified = minimize;
    if (minimize) {
        if (frame_type == TRANSPARENT && wmanager == COMPIZ) {
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
        gdk_window_focus(gdk_window, GDK_CURRENT_TIME);
    }
}

void WindowContextTop::set_maximized(bool maximize) {
    is_maximized = maximize;
    if (maximize) {
        // enable the functionality on the window manager as it might ignore the maximize command,
        // for example when the window is undecorated.
        GdkWMFunction wmf = (GdkWMFunction)(gdk_windowManagerFunctions | GDK_FUNC_MAXIMIZE);
        gdk_window_set_functions(gdk_window, wmf);

        gtk_window_maximize(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_unmaximize(GTK_WINDOW(gtk_widget));
    }
}

void WindowContextTop::enter_fullscreen() {
    gtk_window_fullscreen(GTK_WINDOW(gtk_widget));
    is_fullscreen = true;
}

void WindowContextTop::exit_fullscreen() {
    gtk_window_unfullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContextTop::request_focus() {
    if (is_visible()) {
        gtk_window_present(GTK_WINDOW(gtk_widget));
    }
}

void WindowContextTop::set_focusable(bool focusable) {
    gtk_window_set_accept_focus(GTK_WINDOW(gtk_widget), focusable ? TRUE : FALSE);
}

void WindowContextTop::set_title(const char* title) {
    gtk_window_set_title(GTK_WINDOW(gtk_widget), title);
}

void WindowContextTop::set_alpha(double alpha) {
    gtk_window_set_opacity(GTK_WINDOW(gtk_widget), (gdouble)alpha);
}

void WindowContextTop::set_enabled(bool enabled) {
    is_disabled = !enabled;
    update_window_constraints();
}

void WindowContextTop::set_minimum_size(int w, int h) {
    resizable.minw = (w <= 0) ? 1 : w;
    resizable.minh = (h <= 0) ? 1 : h;
    update_window_constraints();
}

void WindowContextTop::set_maximum_size(int w, int h) {
    resizable.maxw = w;
    resizable.maxh = h;
    update_window_constraints();
}

void WindowContextTop::set_icon(GdkPixbuf* pixbuf) {
    gtk_window_set_icon(GTK_WINDOW(gtk_widget), pixbuf);
}

void WindowContextTop::to_front() {
    gdk_window_raise(gdk_window);
}

void WindowContextTop::to_back() {
    gdk_window_lower(gdk_window);
}

void WindowContextTop::set_modal(bool modal, WindowContext* parent) {
    if (modal) {
        //gtk_window_set_type_hint(GTK_WINDOW(gtk_widget), GDK_WINDOW_TYPE_HINT_DIALOG);
        if (parent) {
            gtk_window_set_transient_for(GTK_WINDOW(gtk_widget), parent->get_gtk_window());
        }
    }
    gtk_window_set_modal(GTK_WINDOW(gtk_widget), modal ? TRUE : FALSE);
}

GtkWindow *WindowContextTop::get_gtk_window() {
    return GTK_WINDOW(gtk_widget);
}

WindowGeometry WindowContextTop::get_geometry() {
    return geometry;
}

void WindowContextTop::update_ontop_tree(bool on_top) {
    bool effective_on_top = on_top || this->on_top;
    gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), effective_on_top ? TRUE : FALSE);
    for (std::set<WindowContextTop*>::iterator it = children.begin(); it != children.end(); ++it) {
        (*it)->update_ontop_tree(effective_on_top);
    }
}

bool WindowContextTop::on_top_inherited() {
    WindowContext* o = owner;
    while (o) {
        WindowContextTop* topO = dynamic_cast<WindowContextTop*>(o);
        if (!topO) break;
        if (topO->on_top) {
            return true;
        }
        o = topO->owner;
    }
    return false;
}

bool WindowContextTop::effective_on_top() {
    if (owner) {
        WindowContextTop* topO = dynamic_cast<WindowContextTop*>(owner);
        return (topO && topO->effective_on_top()) || on_top;
    }
    return on_top;
}

void WindowContextTop::notify_on_top(bool top) {
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
                    top ? com_sun_glass_ui_Window_Level_FLOATING :  com_sun_glass_ui_Window_Level_NORMAL);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }
}

void WindowContextTop::set_level(int level) {
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

void WindowContextTop::set_owner(WindowContext * owner_ctx) {
    owner = owner_ctx;
}

void WindowContextTop::update_view_size() {
    // Notify the view size only if size is oriented by WINDOW, otherwise it knows its own size
    if (geometry.final_width.type == BOUNDSTYPE_WINDOW
        || geometry.final_height.type == BOUNDSTYPE_WINDOW) {

        notify_view_resize();
    }
}

void WindowContextTop::notify_view_resize() {
    if (jview) {
        int cw = geometry_get_content_width(&geometry);
        int ch = geometry_get_content_height(&geometry);

        mainEnv->CallVoidMethod(jview, jViewNotifyResize, cw, ch);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContextTop::notify_window_resize() {
    int w = geometry_get_window_width(&geometry);
    int h = geometry_get_window_height(&geometry);

    mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                 com_sun_glass_events_WindowEvent_RESIZE, w, h);
    CHECK_JNI_EXCEPTION(mainEnv)

    notify_view_resize();
}

void WindowContextTop::notify_window_move() {
    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyMove,
                                 geometry.x, geometry.y);
        CHECK_JNI_EXCEPTION(mainEnv)

        if (jview) {
            mainEnv->CallVoidMethod(jview, jViewNotifyView,
                    com_sun_glass_events_ViewEvent_MOVE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

void WindowContextTop::process_destroy() {
    if (owner) {
        owner->remove_child(this);
    }

    WindowContextBase::process_destroy();
}
