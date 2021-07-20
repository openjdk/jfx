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
        notify_on_top( event->new_window_state & GDK_WINDOW_STATE_ABOVE);
    }
}

void WindowContextBase::process_focus(GdkEventFocus* event) {
    if (!event->in && WindowContextBase::sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }
    if (!event->in && WindowContextBase::sm_grab_window == this) {
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
                    event->in ? com_sun_glass_events_WindowEvent_FOCUS_GAINED : com_sun_glass_events_WindowEvent_FOCUS_LOST);
            CHECK_JNI_EXCEPTION(mainEnv)
        } else {
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

    // Upper layers expects from us Windows behavior:
    // all mouse events should be delivered to window where drag begins
    // and no exit/enter event should be reported during this drag.
    // We can grab mouse pointer for these needs.
    if (press) {
        grab_mouse_drag_focus();
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
        if (enter) { // workaround for RT-21590
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

void WindowContextBase::paint(void* data, jint width, jint height)
{
    if (!is_visible()) {
        return;
    }
#ifdef GLASS_GTK3
    cairo_region_t *region = gdk_window_get_clip_region(gdk_window);
    gdk_window_begin_paint_region(gdk_window, region);
#endif
    cairo_t* context;
    context = gdk_cairo_create(gdk_window);

    cairo_surface_t* cairo_surface;
    cairo_surface = cairo_image_surface_create_for_data(
            (unsigned char*)data,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    applyShapeMask(data, width, height);

    cairo_set_source_surface(context, cairo_surface, 0, 0);
    cairo_set_operator (context, CAIRO_OPERATOR_SOURCE);
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

void WindowContextBase::show_or_hide_children(bool show) {
    std::set<WindowContextTop*>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        (*it)->set_minimized(!show);
        (*it)->show_or_hide_children(show);
    }
}

void WindowContextBase::reparent_children(WindowContext* parent) {
    std::set<WindowContextTop*>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        (*it)->set_owner(parent);
        parent->add_child(*it);
    }
    children.clear();
}

void WindowContextBase::set_visible(bool visible) {
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
#ifdef GLASS_GTK3
    GdkRGBA rgba = {0, 0, 0, 1.};
    rgba.red = r;
    rgba.green = g;
    rgba.blue = b;
    gdk_window_set_background_rgba(gdk_window, &rgba);
#else
    GdkColor color;
    color.red   = (guint16) (r * 65535);
    color.green = (guint16) (g * 65535);
    color.blue  = (guint16) (b * 65535);
    gtk_widget_modify_bg(gtk_widget, GTK_STATE_NORMAL, &color);
#endif
}

WindowContextBase::~WindowContextBase() {
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

////////////////////////////// WindowContextTop /////////////////////////////////
WindowFrameExtents WindowContextTop::normal_extents = {28, 1, 1, 1};
WindowFrameExtents WindowContextTop::utility_extents = {28, 1, 1, 1};


WindowContextTop::WindowContextTop(jobject _jwindow, WindowContext* _owner, long _screen,
        WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf) :
            WindowContextBase(),
            screen(_screen),
            frame_type(_frame_type),
            window_type(type),
            owner(_owner),
            geometry(),
            resizable(),
            frame_extents_initialized(),
            map_received(false),
            location_assigned(false),
            size_assigned(false),
            on_top(false),
            requested_bounds()
{
    jwindow = mainEnv->NewGlobalRef(_jwindow);

    gtk_widget =  gtk_window_new(type == POPUP ? GTK_WINDOW_POPUP : GTK_WINDOW_TOPLEVEL);

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

//    glong xdisplay = (glong)mainEnv->GetStaticLongField(jApplicationCls, jApplicationDisplay);
//    gint  xscreenID = (gint)mainEnv->GetStaticIntField(jApplicationCls, jApplicationScreen);
    glong xvisualID = (glong)mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);

    if (xvisualID != 0) {
        GdkVisual *visual = gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID);
        glass_gtk_window_configure_from_visual(gtk_widget, visual);
    }

    gtk_widget_set_size_request(gtk_widget, 0, 0);
    gtk_widget_set_events(gtk_widget, GDK_FILTERED_EVENTS_MASK);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);
    if (frame_type != TITLED) {
        gtk_window_set_decorated(GTK_WINDOW(gtk_widget), FALSE);
    }

    glass_gtk_configure_transparency_and_realize(gtk_widget, frame_type == TRANSPARENT);
    gtk_window_set_title(GTK_WINDOW(gtk_widget), "");

    gdk_window = gtk_widget_get_window(gtk_widget);
    gdk_window_set_events(gdk_window, GDK_FILTERED_EVENTS_MASK);

    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);

    gdk_window_register_dnd(gdk_window);

    gdk_windowManagerFunctions = wmf;
    if (wmf) {
        gdk_window_set_functions(gdk_window, wmf);
    }

    if (frame_type == TITLED) {
        request_frame_extents();
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

static GdkAtom
get_net_frame_extents_atom() {
    static const char * extents_str = "_NET_FRAME_EXTENTS";
    return gdk_atom_intern(extents_str, TRUE);
}

void
WindowContextTop::request_frame_extents() {
    Display *display = GDK_DISPLAY_XDISPLAY(gdk_window_get_display(gdk_window));
    Atom rfeAtom = XInternAtom(display, "_NET_REQUEST_FRAME_EXTENTS", True);
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

void WindowContextTop::activate_window() {
    Display *display = GDK_DISPLAY_XDISPLAY (gdk_window_get_display (gdk_window));
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
                   (XEvent *) &clientMessage);
        XFlush(display);
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


bool WindowContextTop::update_frame_extents() {
    bool changed = false;
    int top, left, bottom, right;
    if (get_frame_extents_property(&top, &left, &bottom, &right)) {
        changed = geometry.extents.top != top
                    || geometry.extents.left != left
                    || geometry.extents.bottom != bottom
                    || geometry.extents.right != right;
        if (changed) {
            geometry.extents.top = top;
            geometry.extents.left = left;
            geometry.extents.bottom = bottom;
            geometry.extents.right = right;
            if (!is_null_extents()) {
                set_cached_extents(geometry.extents);
            }
        }
    }
    return changed;
}

bool
WindowContextTop::get_frame_extents_property(int *top, int *left,
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

static int geometry_get_window_width(const WindowGeometry *windowGeometry) {
     return (windowGeometry->final_width.type != BOUNDSTYPE_WINDOW)
                   ? windowGeometry->final_width.value
                         + windowGeometry->extents.left
                         + windowGeometry->extents.right
                   : windowGeometry->final_width.value;
}

static int geometry_get_window_height(const WindowGeometry *windowGeometry) {
    return (windowGeometry->final_height.type != BOUNDSTYPE_WINDOW)
                   ? windowGeometry->final_height.value
                         + windowGeometry->extents.top
                         + windowGeometry->extents.bottom
                   : windowGeometry->final_height.value;
}

static int geometry_get_content_width(WindowGeometry *windowGeometry) {
    return (windowGeometry->final_width.type != BOUNDSTYPE_CONTENT)
                   ? windowGeometry->final_width.value
                         - windowGeometry->extents.left
                         - windowGeometry->extents.right
                   : windowGeometry->final_width.value;
}
static int geometry_get_content_height(WindowGeometry *windowGeometry) {
    return (windowGeometry->final_height.type != BOUNDSTYPE_CONTENT)
                   ? windowGeometry->final_height.value
                         - windowGeometry->extents.top
                         - windowGeometry->extents.bottom
                   : windowGeometry->final_height.value;
}

static int geometry_get_window_x(const WindowGeometry *windowGeometry) {
    float value = windowGeometry->refx;
    if (windowGeometry->gravity_x != 0) {
        value -= geometry_get_window_width(windowGeometry)
                     * windowGeometry->gravity_x;
    }
    return (int) value;
}

static int geometry_get_window_y(const WindowGeometry *windowGeometry) {
    float value = windowGeometry->refy;
    if (windowGeometry->gravity_y != 0) {
        value -= geometry_get_window_height(windowGeometry)
                     * windowGeometry->gravity_y;
    }
    return (int) value;
}

static void geometry_set_window_x(WindowGeometry *windowGeometry, int value) {
    float newValue = value;
    if (windowGeometry->gravity_x != 0) {
        newValue += geometry_get_window_width(windowGeometry)
                * windowGeometry->gravity_x;
    }
    windowGeometry->refx = newValue;
}

static void geometry_set_window_y(WindowGeometry *windowGeometry, int value) {
    float newValue = value;
    if (windowGeometry->gravity_y != 0) {
        newValue += geometry_get_window_height(windowGeometry)
                * windowGeometry->gravity_y;
    }
    windowGeometry->refy = newValue;
}

void WindowContextTop::process_net_wm_property() {
    // Workaround for https://bugs.launchpad.net/unity/+bug/998073

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

    if (event->atom == atom_net_wm_state && event->window == gdk_window) {
        process_net_wm_property();
    }
}

void WindowContextTop::process_configure(GdkEventConfigure* event) {
    gint x, y, w, h;
    bool updateWindowConstraints = false;
    if (gtk_window_get_decorated(GTK_WINDOW(gtk_widget))) {
        GdkRectangle frame;
        gint top, left, bottom, right;

        gdk_window_get_frame_extents(gdk_window, &frame);
#ifdef GLASS_GTK3
        gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h);
#else
        gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h, NULL);
#endif
        x = frame.x;
        y = frame.y;
        geometry.current_width = frame.width;
        geometry.current_height = frame.height;

        if (update_frame_extents()) {
            updateWindowConstraints = true;
            if (!frame_extents_initialized && !is_null_extents()) {
                frame_extents_initialized = true;
                set_bounds(0, 0, false, false,
                    requested_bounds.width, requested_bounds.height,
                    requested_bounds.client_width, requested_bounds.client_height
                );
            }
        }
    } else {
        x = event->x;
        y = event->y;
        w = event->width;
        h = event->height;
    }

    if (size_assigned && w <= 1 && h <= 1 && (geometry.final_width.value > 1 ||
                                             geometry.final_height.value > 1)) {
        // skip artifact
        return;
   }

    // JDK-8232811: to avoid conflicting events, update the geometry only after window pops.
    if (map_received) {
        geometry.final_width.value = w;
        geometry.final_width.type = BOUNDSTYPE_CONTENT;
        geometry.final_height.value = h;
        geometry.final_height.type = BOUNDSTYPE_CONTENT;
    }

    geometry_set_window_x(&geometry, x);
    geometry_set_window_y(&geometry, y);

    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyResize,
                event->width,
                event->height);
        CHECK_JNI_EXCEPTION(mainEnv)
        mainEnv->CallVoidMethod(jview, jViewNotifyView,
                com_sun_glass_events_ViewEvent_MOVE);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                (is_maximized)
                    ? com_sun_glass_events_WindowEvent_MAXIMIZE
                    : com_sun_glass_events_WindowEvent_RESIZE,
                geometry.current_width,
                geometry.current_height);
        CHECK_JNI_EXCEPTION(mainEnv)

        mainEnv->CallVoidMethod(jwindow, jWindowNotifyMove, x, y);
        CHECK_JNI_EXCEPTION(mainEnv)
    }

    glong to_screen = getScreenPtrForLocation(x, y);
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

    if (resizable.request != REQUEST_NONE) {
        set_window_resizable(resizable.request == REQUEST_RESIZABLE);
        resizable.request = REQUEST_NONE;
    } else if (!resizable.value) {
        set_window_resizable(false);
    } else if (updateWindowConstraints) {
        update_window_constraints();
    }
}

void WindowContextTop::update_window_constraints() {
    if (resizable.value) {
        GdkGeometry geom = {
            (resizable.minw == -1) ? 1
                    : resizable.minw - geometry.extents.left - geometry.extents.right,
            (resizable.minh == -1) ? 1
                    : resizable.minh - geometry.extents.top - geometry.extents.bottom,
            (resizable.maxw == -1) ? 100000
                    : resizable.maxw - geometry.extents.left - geometry.extents.right,
            (resizable.maxh == -1) ? 100000
                    : resizable.maxh - geometry.extents.top - geometry.extents.bottom,
            0, 0, 0, 0, 0.0, 0.0, GDK_GRAVITY_NORTH_WEST
        };
        gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &geom,
                static_cast<GdkWindowHints> (GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
    }
}

void WindowContextTop::set_window_resizable(bool res) {
    if(!res) {
        int w = geometry_get_content_width(&geometry);
        int h = geometry_get_content_height(&geometry);
        if (w == -1 && h == -1) {
            gtk_window_get_size(GTK_WINDOW(gtk_widget), &w, &h);
        }
        GdkGeometry geom = {w, h, w, h, 0, 0, 0, 0, 0.0, 0.0, GDK_GRAVITY_NORTH_WEST};
        gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &geom,
                static_cast<GdkWindowHints>(GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
        resizable.value = false;
    } else {
        resizable.value = true;
        update_window_constraints();
    }
}

void WindowContextTop::set_resizable(bool res) {
    resizable.prev = false;
    gint w, h;
    gtk_window_get_size(GTK_WINDOW(gtk_widget), &w, &h);
    if (map_received || w > 1 || h > 1) {
        set_window_resizable(res);
    } else {
        //Since window is not ready yet set only request for change of resizable.
        resizable.request  = res ? REQUEST_RESIZABLE : REQUEST_NOT_RESIZABLE;
    }
}

void WindowContextTop::set_visible(bool visible)
{
    if (visible) {
        if (!size_assigned) {
            set_bounds(0, 0, false, false, 320, 200, -1, -1);
        }
        if (!location_assigned) {
            set_bounds(0, 0, true, true, -1, -1, -1, -1);
        }
    }
    WindowContextBase::set_visible(visible);
    //JDK-8220272 - fire event first because GDK_FOCUS_CHANGE is not always in order
    if (visible && jwindow && isEnabled()) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);
        CHECK_JNI_EXCEPTION(mainEnv);
    }
}

void WindowContextTop::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch) {
    requested_bounds.width = w;
    requested_bounds.height = h;
    requested_bounds.client_width = cw;
    requested_bounds.client_height = ch;

    if (!frame_extents_initialized && frame_type == TITLED) {
        update_frame_extents();
        if (is_null_extents()) {
            if (!is_null_extents(get_cached_extents())) {
                geometry.extents = get_cached_extents();
            }
        } else {
            frame_extents_initialized = true;
        }
    }

    XWindowChanges windowChanges;
    unsigned int windowChangesMask = 0;
    if (w > 0) {
        geometry.final_width.value = w;
        geometry.final_width.type = BOUNDSTYPE_WINDOW;
        geometry.current_width = geometry_get_window_width(&geometry);
        windowChanges.width = geometry_get_content_width(&geometry);
        windowChangesMask |= CWWidth;
    } else if (cw > 0) {
        geometry.final_width.value = cw;
        geometry.final_width.type = BOUNDSTYPE_CONTENT;
        geometry.current_width = geometry_get_window_width(&geometry);
        windowChanges.width = geometry_get_content_width(&geometry);
        windowChangesMask |= CWWidth;
    }

    if (h > 0) {
        geometry.final_height.value = h;
        geometry.final_height.type = BOUNDSTYPE_WINDOW;
        geometry.current_height = geometry_get_window_height(&geometry);
        windowChanges.height = geometry_get_content_height(&geometry);
        windowChangesMask |= CWHeight;
    } else if (ch > 0) {
        geometry.final_height.value = ch;
        geometry.final_height.type = BOUNDSTYPE_CONTENT;
        geometry.current_height = geometry_get_window_height(&geometry);
        windowChanges.height = geometry_get_content_height(&geometry);
        windowChangesMask |= CWHeight;
    }

    if (xSet || ySet) {
        if (xSet) {
            geometry.refx = x + geometry.current_width * geometry.gravity_x;
        }

        windowChanges.x = geometry_get_window_x(&geometry);
        windowChangesMask |= CWX;

        if (ySet) {
            geometry.refy = y + geometry.current_height * geometry.gravity_y;
        }

        windowChanges.y = geometry_get_window_y(&geometry);
        windowChangesMask |= CWY;

        location_assigned = true;
    }

    if (w > 0 || h > 0 || cw > 0 || ch > 0) size_assigned = true;

    window_configure(&windowChanges, windowChangesMask);

}

void WindowContextTop::process_map() {
    map_received = true;
}

void WindowContextTop::window_configure(XWindowChanges *windowChanges,
        unsigned int windowChangesMask) {
    if (windowChangesMask == 0) {
        return;
    }

    if (windowChangesMask & (CWX | CWY)) {
        gint newX, newY;
        gtk_window_get_position(GTK_WINDOW(gtk_widget), &newX, &newY);

        if (windowChangesMask & CWX) {
            newX = windowChanges->x;
        }
        if (windowChangesMask & CWY) {
            newY = windowChanges->y;
        }
        gtk_window_move(GTK_WINDOW(gtk_widget), newX, newY);
    }

    if (windowChangesMask & (CWWidth | CWHeight)) {
        gint newWidth, newHeight;
        gtk_window_get_size(GTK_WINDOW(gtk_widget), &newWidth, &newHeight);

        if (windowChangesMask & CWWidth) {
            newWidth = windowChanges->width;
        }
        if (windowChangesMask & CWHeight) {
            newHeight = windowChanges->height;
        }

        if (!resizable.value) {
            GdkGeometry geom;
            GdkWindowHints hints = (GdkWindowHints)(GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE);
            geom.min_width = geom.max_width = newWidth;
            geom.min_height = geom.max_height = newHeight;
            gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &geom, hints);
        }
        gtk_window_resize(GTK_WINDOW(gtk_widget), newWidth, newHeight);

        //JDK-8193502: Moved here from WindowContextBase::set_view because set_view is called
        //first and the size is not set yet. This also guarantees that the size will be correct
        //see: gtk_window_get_size doc for more context.
        if (jview) {
            mainEnv->CallVoidMethod(jview, jViewNotifyResize, newWidth, newHeight);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }
}

void WindowContextTop::applyShapeMask(void* data, uint width, uint height)
{
    if (frame_type != TRANSPARENT) {
        return;
    }

    glass_window_apply_shape_mask(gtk_widget_get_window(gtk_widget), data, width, height);
}

void WindowContextTop::ensure_window_size() {
    gint w, h;
#ifdef GLASS_GTK3
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h);
#else
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h, NULL);
#endif
    if (size_assigned && (geometry.final_width.value != w
                       || geometry.final_height.value != h)) {

        gdk_window_resize(gdk_window, geometry.final_width.value,
                                      geometry.final_height.value);
    }
}

void WindowContextTop::set_minimized(bool minimize) {
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
void WindowContextTop::set_maximized(bool maximize) {
    is_maximized = maximize;
    if (maximize) {
        // enable the functionality on the window manager as it might ignore the maximize command,
        // for example when the window is undecorated.
        GdkWMFunction wmf = (GdkWMFunction)(gdk_windowManagerFunctions | GDK_FUNC_MAXIMIZE);
        gdk_window_set_functions(gdk_window, wmf);

        ensure_window_size();
        gtk_window_maximize(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_unmaximize(GTK_WINDOW(gtk_widget));
    }
}

void WindowContextTop::enter_fullscreen() {
    ensure_window_size();
    gtk_window_fullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContextTop::exit_fullscreen() {
    gtk_window_unfullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContextTop::request_focus() {
    //JDK-8212060: Window show and then move glitch.
    //The WindowContextBase::set_visible will take care of showing the window.
    //The below code will only handle later request_focus.
    if (is_visible()) {
        gtk_window_present(GTK_WINDOW(gtk_widget));
    }
}

void WindowContextTop::set_focusable(bool focusable) {
    gtk_window_set_accept_focus(GTK_WINDOW(gtk_widget), focusable ? TRUE : FALSE);
}

void WindowContextTop::set_title(const char* title) {
    gtk_window_set_title(GTK_WINDOW(gtk_widget),title);
}

void WindowContextTop::set_alpha(double alpha) {
    gtk_window_set_opacity(GTK_WINDOW(gtk_widget), (gdouble)alpha);
}

void WindowContextTop::set_enabled(bool enabled) {
    if (enabled) {
        if (resizable.prev) {
            set_window_resizable(true);
        }
    } else {
        if (resizable.value) {
            set_window_resizable(false);
            resizable.prev = true;
        } else if (resizable.request == REQUEST_RESIZABLE) {
            resizable.request = REQUEST_NOT_RESIZABLE;
            resizable.prev = true;
        }
    }
}

void WindowContextTop::set_minimum_size(int w, int h) {
    resizable.minw = w;
    resizable.minh = h;
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

void WindowContextTop::restack(bool restack) {
    gdk_window_restack(gdk_window, NULL, restack ? TRUE : FALSE);
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

WindowFrameExtents WindowContextTop::get_frame_extents() {
    return geometry.extents;
}

void WindowContextTop::set_gravity(float x, float y) {
    int oldX = geometry_get_window_x(&geometry);
    int oldY = geometry_get_window_y(&geometry);
    geometry.gravity_x = x;
    geometry.gravity_y = y;
    geometry_set_window_x(&geometry, oldX);
    geometry_set_window_y(&geometry, oldY);
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

void WindowContextTop::process_destroy() {
    if (owner) {
        owner->remove_child(this);
    }

    WindowContextBase::process_destroy();
}

////////////////////////////// WindowContextPlug ////////////////////////////////

static gboolean plug_configure(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    (void)widget;

    if (event->type == GDK_CONFIGURE) {
        ((WindowContextPlug*)user_data)->process_gtk_configure(&event->configure);
    }
    return FALSE;
}

WindowContextPlug::WindowContextPlug(jobject _jwindow, void* _owner) :
        WindowContextBase(),
        parent()
{
    jwindow = mainEnv->NewGlobalRef(_jwindow);

    gtk_widget = gtk_plug_new((Window)PTR_TO_JLONG(_owner));

    g_signal_connect(G_OBJECT(gtk_widget), "configure-event", G_CALLBACK(plug_configure), this);

    gtk_widget_set_size_request(gtk_widget, 0, 0);
    gtk_widget_set_events(gtk_widget, GDK_FILTERED_EVENTS_MASK);
    gtk_widget_set_can_focus(GTK_WIDGET(gtk_widget), TRUE);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);

    gtk_widget_realize(gtk_widget);
    gdk_window = gtk_widget_get_window(gtk_widget);
    gdk_window_set_events(gdk_window, GDK_FILTERED_EVENTS_MASK);

    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);
    gdk_window_register_dnd(gdk_window);

    gtk_container = gtk_fixed_new();
    gtk_container_add (GTK_CONTAINER(gtk_widget), gtk_container);
    gtk_widget_realize(gtk_container);
}

GtkWindow *WindowContextPlug::get_gtk_window() {
    return GTK_WINDOW(gtk_widget);
}

void WindowContextPlug::process_configure(GdkEventConfigure* event) {
    (void)event;

    //Note: process_gtk_configure is used, so there's no need to handle GDK events
}

void WindowContextPlug::process_gtk_configure(GdkEventConfigure* event) {
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyResize,
                event->width,
                event->height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }

    mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
            com_sun_glass_events_WindowEvent_RESIZE,
            event->width,
            event->height);
    CHECK_JNI_EXCEPTION(mainEnv)

    if (!embedded_children.empty()) {
        WindowContextChild* child = embedded_children.back();
        child->process_configure(event);
    }
}

bool WindowContextPlug::set_view(jobject view) {
    // probably never called for applet window
    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
    }

    if (view) {
        gint width, height;
        jview = mainEnv->NewGlobalRef(view);
        gtk_window_get_size(GTK_WINDOW(gtk_widget), &width, &height);
        mainEnv->CallVoidMethod(view, jViewNotifyResize, width, height);
        CHECK_JNI_EXCEPTION_RET(mainEnv, FALSE)
    } else {
        jview = NULL;
    }
    return TRUE;
}

void WindowContextPlug::window_configure(XWindowChanges *windowChanges,
        unsigned int windowChangesMask) {
    if (windowChangesMask == 0) {
        return;
    }

    if (windowChangesMask & (CWX | CWY)) {
        gint newX, newY;
        gtk_window_get_position(GTK_WINDOW(gtk_widget), &newX, &newY);

        if (windowChangesMask & CWX) {
            newX = windowChanges->x;
        }
        if (windowChangesMask & CWY) {
            newY = windowChanges->y;
        }
        gtk_window_move(GTK_WINDOW(gtk_widget), newX, newY);
    }

    if (windowChangesMask & (CWWidth | CWHeight)) {
        gint newWidth, newHeight;
        gtk_window_get_size(GTK_WINDOW(gtk_widget),
                &newWidth, &newHeight);

        if (windowChangesMask & CWWidth) {
            newWidth = windowChanges->width;
        }
        if (windowChangesMask & CWHeight) {
            newHeight = windowChanges->height;
        };
        gtk_widget_set_size_request(gtk_widget, newWidth, newHeight);
    }
}

void WindowContextPlug::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch) {
    XWindowChanges windowChanges;
    unsigned int windowChangesMask = 0;

    if (xSet) {
        windowChanges.x = x;
        windowChangesMask |= CWX;
    }

    if (ySet) {
        windowChanges.y = y;
        windowChangesMask |= CWY;
    }

    if (w > 0) {
        windowChanges.width = w;
        windowChangesMask |= CWWidth;
    } else if (cw > 0) {
        windowChanges.width = cw;
        windowChangesMask |= CWWidth;
    }

    if (h > 0) {
        windowChanges.height = h;
        windowChangesMask |= CWHeight;
    } else if (ch > 0) {
        windowChanges.height = ch;
        windowChangesMask |= CWHeight;
    }

    window_configure(&windowChanges, windowChangesMask);
}
////////////////////////////// WindowContextChild ////////////////////////////////

void WindowContextChild::process_mouse_button(GdkEventButton* event) {
    WindowContextBase::process_mouse_button(event);
   // gtk_window_set_focus (GTK_WINDOW (gtk_widget_get_ancestor(gtk_widget, GTK_TYPE_WINDOW)), NULL);
    gtk_widget_grab_focus(gtk_widget);
}

static gboolean child_focus_callback(GtkWidget *widget, GdkEvent *event, gpointer user_data)
{
    (void)widget;

    WindowContext *ctx = (WindowContext *)user_data;
    ctx->process_focus(&event->focus_change);
    return TRUE;
}

WindowContextChild::WindowContextChild(jobject _jwindow,
                                       void* _owner,
                                       GtkWidget *parent_widget,
                                       WindowContextPlug *parent_ctx) :
        WindowContextBase(),
        parent(),
        full_screen_window(),
        view()
{
    (void)_owner;

    jwindow = mainEnv->NewGlobalRef(_jwindow);
    gtk_widget = gtk_drawing_area_new();
    parent = parent_ctx;

    glong xvisualID = (glong) mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);

    if (xvisualID != 0) {
        GdkVisual *visual = gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID);
        glass_gtk_window_configure_from_visual(gtk_widget, visual);
    }

    gtk_widget_set_events(gtk_widget, GDK_FILTERED_EVENTS_MASK);
    gtk_widget_set_can_focus(GTK_WIDGET(gtk_widget), TRUE);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);
    gtk_container_add (GTK_CONTAINER(parent_widget), gtk_widget);
    gtk_widget_realize(gtk_widget);
    gdk_window = gtk_widget_get_window(gtk_widget);
    gdk_window_set_events(gdk_window, GDK_FILTERED_EVENTS_MASK);
    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);
    gdk_window_register_dnd(gdk_window);
    g_signal_connect(gtk_widget, "focus-in-event", G_CALLBACK(child_focus_callback), this);
    g_signal_connect(gtk_widget, "focus-out-event", G_CALLBACK(child_focus_callback), this);
}

void WindowContextChild::set_visible(bool visible) {
    std::vector<WindowContextChild*> &embedded_children =
            dynamic_cast<WindowContextPlug*>(parent)->embedded_children;

    if (visible) {
        embedded_children.push_back(this);
    } else {
        std::vector<WindowContextChild*>::iterator pos
                = std::find(embedded_children.begin(), embedded_children.end(), this);
        if (pos != embedded_children.end()) {
            embedded_children.erase((pos));
        }
    }

    WindowContextBase::set_visible(visible);
}

GtkWindow *WindowContextChild::get_gtk_window() {
    return GTK_WINDOW(gtk_widget_get_ancestor(gtk_widget, GTK_TYPE_WINDOW));
}

void WindowContextChild::process_configure(GdkEventConfigure* event) {
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyResize,
                event->width,
                event->height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }

    gtk_widget_set_size_request(gtk_widget, event->width, event->height);

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                com_sun_glass_events_WindowEvent_RESIZE,
                event->width,
                event->height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

bool WindowContextChild::set_view(jobject view) {
    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
    }

    if (view) {
        gint width, height;
        jview = mainEnv->NewGlobalRef(view);
        GtkAllocation ws;
        gtk_widget_get_allocation(gtk_widget, &ws);
        width = ws.width;
        height = ws.height;
        mainEnv->CallVoidMethod(view, jViewNotifyResize, width, height);
        CHECK_JNI_EXCEPTION_RET(mainEnv, FALSE)
    } else {
        jview = NULL;
    }
    return TRUE;
}

void WindowContextChild::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch) {

    if (x > 0 || y > 0 || xSet || ySet) {
        gint newX, newY;
        gdk_window_get_origin(gdk_window, &newX, &newY);
        if (jwindow) {
            mainEnv->CallVoidMethod(jwindow,
                    jWindowNotifyMove,
                    newX, newY);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }

    // As we have no frames, there's no difference between the calls
    if ((cw | ch) > 0) {
        w = cw; h = ch;
    }

    if (w > 0 || h > 0) {
        gint newWidth, newHeight;
        GtkAllocation ws;
        gtk_widget_get_allocation(gtk_widget, &ws);
        newWidth = ws.width;
        newHeight = ws.height;

        if (w > 0) {
            newWidth = w;
        }
        if (h > 0) {
            newHeight = h;
        }
        gtk_widget_set_size_request(gtk_widget, newWidth, newHeight);
        // FIXME: hack to set correct size to view
        if (jview) {
            mainEnv->CallVoidMethod(jview,
                    jViewNotifyResize,
                    newWidth, newHeight);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }
}

int WindowContextChild::getEmbeddedX() {
    int x;
    gdk_window_get_origin(gdk_window, &x, NULL);
    return x;
}

int WindowContextChild::getEmbeddedY() {
    int y;
    gdk_window_get_origin(gdk_window, NULL, &y);
    return y;

}

void WindowContextChild::restack(bool toFront) {
    std::vector<WindowContextChild*> &embedded_children =
                dynamic_cast<WindowContextPlug*>(parent)->embedded_children;

    std::vector<WindowContextChild*>::iterator pos
        = std::find(embedded_children.begin(), embedded_children.end(), this);

    embedded_children.erase(pos);
    if (toFront) {
        embedded_children.push_back(this);
    } else {
        embedded_children.insert(embedded_children.begin(), this);
    }

    gdk_window_restack(gdk_window, NULL, toFront ? TRUE : FALSE);
}

void WindowContextChild::enter_fullscreen() {
    if (full_screen_window) {
        return;
    }

    full_screen_window = new WindowContextTop(jwindow, NULL, 0L, UNTITLED,
                                                NORMAL, (GdkWMFunction) 0);
    int x, y, w, h;
    gdk_window_get_origin(gdk_window, &x, &y);
#ifdef GLASS_GTK3
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h);
#else
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h, NULL);
#endif
    full_screen_window->set_bounds(x, y, true, true, w, h, -1, -1);

    if (WindowContextBase::sm_grab_window == this) {
        ungrab_focus();
    }

    reparent_children(full_screen_window);

    full_screen_window->set_visible(true);
    full_screen_window->enter_fullscreen();

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyDelegatePtr, (jlong)full_screen_window);
        CHECK_JNI_EXCEPTION(mainEnv)
    }

    if (jview) {
        this->view = (GlassView*)mainEnv->GetLongField(jview, jViewPtr);

        this->view->current_window = full_screen_window;
        this->view->embedded_window = this;
        full_screen_window->set_view(jview);
        this->set_view(NULL);
    }
}

void WindowContextChild::exit_fullscreen() {
    if (!full_screen_window) {
        return;
    }

    if (WindowContextBase::sm_grab_window == this) {
        ungrab_focus();
    }

    full_screen_window->reparent_children(this);

    mainEnv->CallVoidMethod(jwindow, jWindowNotifyDelegatePtr, (jlong)NULL);
    CHECK_JNI_EXCEPTION(mainEnv)

    if (this->view) {
        this->view->current_window = this;
        this->view->embedded_window = NULL;
    }
    this->set_view(full_screen_window->get_jview());

    full_screen_window->detach_from_java();

    full_screen_window->set_view(NULL);

    full_screen_window->set_visible(false);

    destroy_and_delete_ctx(full_screen_window);
    full_screen_window = NULL;
    this->view = NULL;
}

void WindowContextChild::process_destroy() {
    if (full_screen_window) {
        destroy_and_delete_ctx(full_screen_window);
    }

    std::vector<WindowContextChild*> &embedded_children =
            dynamic_cast<WindowContextPlug*>(parent)->embedded_children;

    std::vector<WindowContextChild*>::iterator pos
                = std::find(embedded_children.begin(), embedded_children.end(), this);
    if (pos != embedded_children.end()) {
        embedded_children.erase((pos));
    }

    WindowContextBase::process_destroy();
}
