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
#include "glass_window.h"
#include "glass_general.h"
#include "glass_gtkcompat.h"
#include "glass_key.h"

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

#include <string.h>

#include <iostream>
#include <algorithm>

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

void WindowContextBase::process_focus(GdkEventFocus* event) {
    if (!event->in && WindowContextBase::sm_grab_window == this) {
        ungrab_focus();
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

    // Upper layers expects from us Windows behavior:
    // all mouse events should be delivered to window where drag begins 
    // and no exit/enter event should be reported during this drag.
    // We can grab mouse pointer for these needs.
    if (press) {
        grab_mouse_drag_focus();
    } else if ((event->state & MOUSE_BUTTONS_MASK)
            && !(state & MOUSE_BUTTONS_MASK)) { // all buttons released
        ungrab_mouse_drag_focus();
    }
}

void WindowContextBase::process_mouse_motion(GdkEventMotion* event) {
    jint glass_modifier = gdk_modifier_mask_to_glass(event->state);
    jint isDrag = glass_modifier & (
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY);
    jint button = com_sun_glass_events_MouseEvent_BUTTON_NONE;

    if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY) {
        button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE) {
        button = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
    } else if (glass_modifier & com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY) {
        button = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
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
    guint keyValue;
    gint state = 0;
    state |= event->state & GDK_MOD2_MASK; //NumLock test
    gdk_keymap_translate_keyboard_state(gdk_keymap_get_default(),
            event->hardware_keycode, static_cast<GdkModifierType>(state), event->group,
            &keyValue, NULL, NULL, NULL);
    jint glassKey = gdk_keyval_to_glass(keyValue);
    jint glassModifier = gdk_modifier_mask_to_glass(event->state);
    if (press) {
        glassModifier |= glass_key_to_modifier(glassKey);
    } else {
        glassModifier &= ~glass_key_to_modifier(glassKey);
    }
    jcharArray jChars;
    jchar key = gdk_keyval_to_unicode(event->keyval);
    if (key >= 'a' && key <= 'z' && (event->state & GDK_CONTROL_MASK)) {
        key = key - 'a' + 1; // map 'a' to ctrl-a, and so on.
    } else {
        key = glass_gtk_fixup_typed_key(key, event->keyval);
    }

    if (key > 0) {
        jChars = mainEnv->NewCharArray(1);
        mainEnv->SetCharArrayRegion(jChars, 0, 1, &key);

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
    
    cairo_t* context;
    context = gdk_cairo_create(GDK_DRAWABLE(gdk_window));

    cairo_surface_t* cairo_surface;
    cairo_surface = cairo_image_surface_create_for_data(
            (unsigned char*)data,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    applyShapeMask(cairo_surface, width, height);

    cairo_set_source_surface(context, cairo_surface, 0, 0);
    cairo_set_operator (context, CAIRO_OPERATOR_SOURCE);
    cairo_paint(context);

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
        (*it)->set_visible(show);
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
    if (WindowContextBase::sm_mouse_drag_window) {
        glass_gdk_mouse_devices_grab_with_cursor(
                WindowContextBase::sm_mouse_drag_window->get_gdk_window(), cursor, FALSE);
    } else if (WindowContextBase::sm_grab_window) {
        glass_gdk_mouse_devices_grab_with_cursor(
                WindowContextBase::sm_grab_window->get_gdk_window(), cursor);
    }
    gdk_window_set_cursor(gdk_window, cursor);
}

void WindowContextBase::set_background(float r, float g, float b) {
    GdkColor color;
    color.red   = (guint16) (r * 65535);
    color.green = (guint16) (g * 65535);
    color.blue  = (guint16) (b * 65535);
    gtk_widget_modify_bg(gtk_widget, GTK_STATE_NORMAL, &color);
}

WindowContextBase::~WindowContextBase() {
    if (xim.ic) {
        XDestroyIC(xim.ic);
    }
    if (xim.im) {
        XCloseIM(xim.im);
    }

    gtk_widget_destroy(gtk_widget);
}

////////////////////////////// WindowContextTop /////////////////////////////////


WindowContextTop::WindowContextTop(jobject _jwindow, WindowContext* _owner, long _screen,
        WindowFrameType _frame_type, WindowType type)
: WindowContextBase(), screen(_screen), frame_type(_frame_type),
        owner(_owner), geometry(), stale_config_notifications(), resizable(),
        xshape(), frame_extents_initialized(), map_received(false)
{
    jwindow = mainEnv->NewGlobalRef(_jwindow);

    gtk_widget =  gtk_window_new(type == POPUP ? GTK_WINDOW_POPUP : GTK_WINDOW_TOPLEVEL);

    if (owner) {
        owner->add_child(this);
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
    gtk_widget_set_events(gtk_widget, GDK_ALL_EVENTS_MASK);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);
    if (frame_type != TITLED) {
        gtk_window_set_decorated(GTK_WINDOW(gtk_widget), FALSE);
    }

    glass_gtk_configure_transparency_and_realize(gtk_widget, frame_type == TRANSPARENT);
    gtk_window_set_title(GTK_WINDOW(gtk_widget), "");

    gdk_window = gtk_widget_get_window(gtk_widget);

    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);

    gdk_window_register_dnd(gdk_window);

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
    Display *display = GDK_WINDOW_XDISPLAY(gdk_window);
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

void
WindowContextTop::initialize_frame_extents() {
    int top, left, bottom, right;
    if (get_frame_extents_property(&top, &left, &bottom, &right)) {
        geometry.extents.top = top;
        geometry.extents.left = left;
        geometry.extents.bottom = bottom;
        geometry.extents.right = right;
    }
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

void WindowContextTop::process_property_notify(GdkEventProperty* event) {
    if (event->atom == get_net_frame_extents_atom() &&
            event->window == gdk_window) {
        int top, left, bottom, right;
        if (get_frame_extents_property(&top, &left, &bottom, &right)) {
            int oldX = geometry_get_window_x(&geometry);
            int oldY = geometry_get_window_y(&geometry);
            int oldWidth = geometry_get_content_width(&geometry);
            int oldHeight = geometry_get_content_height(&geometry);

            bool updateWindowConstraints = geometry.extents.top != top
                    || geometry.extents.left != left
                    || geometry.extents.bottom != bottom
                    || geometry.extents.right != right;

            geometry.extents.top = top;
            geometry.extents.left = left;
            geometry.extents.bottom = bottom;
            geometry.extents.right = right;

            if (updateWindowConstraints) {
                update_window_constraints();
            }

            XWindowChanges windowChanges;
            unsigned int windowChangesMask = 0;

            int newX = geometry_get_window_x(&geometry);
            int newY = geometry_get_window_y(&geometry);
            int newWidth = geometry_get_content_width(&geometry);
            int newHeight = geometry_get_content_height(&geometry);

            if (oldX != newX) {
                windowChanges.x = newX;
                windowChangesMask |= CWX;
            }

            if (oldY != newY) {
                windowChanges.y = newY;
                windowChangesMask |= CWY;
            }

            if (oldWidth != newWidth) {
                windowChanges.width = newWidth;
                windowChangesMask |= CWWidth;
            }

            if (oldHeight != newHeight) {
                windowChanges.height = newHeight;
                windowChangesMask |= CWHeight;
            }

            window_configure(&windowChanges, windowChangesMask);

            if (jview) {
                mainEnv->CallVoidMethod(jview, jViewNotifyView, com_sun_glass_events_ViewEvent_MOVE);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
        }
    }
}

static glong getScreenPtrForLocation(gint x, gint y) { //TODO: refactor to GlassApplication.cpp ?
    //Note: we are relying on the fact that javafx_screen_id == gdk_monitor_id
    return gdk_screen_get_monitor_at_point(gdk_screen_get_default(), x, y);
}

void WindowContextTop::process_configure(GdkEventConfigure* event) {

    geometry.current_width = event->width + geometry.extents.left
                                           + geometry.extents.right;
    geometry.current_height = event->height + geometry.extents.top
                                             + geometry.extents.bottom;
    gint x, y;
    if (gtk_window_get_decorated(GTK_WINDOW(gtk_widget))) {
        gtk_window_get_position(GTK_WINDOW(gtk_widget), &x, &y);
    } else {
        x = event->x;
        y = event->y;
    }

    if (stale_config_notifications == 0) {
        if ((geometry_get_content_width(&geometry) != event->width)
                || (geometry_get_content_height(&geometry) != event->height)) {
            geometry.final_width.value = event->width;
            geometry.final_width.type = BOUNDSTYPE_CONTENT;
            geometry.final_height.value = event->height;
            geometry.final_height.type = BOUNDSTYPE_CONTENT;
        }
        geometry_set_window_x(&geometry, x);
        geometry_set_window_y(&geometry, y);
    } else {
        --stale_config_notifications;
    }
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyResize,
                event->width,
                event->height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize,
                com_sun_glass_events_WindowEvent_RESIZE,
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
                mainEnv->CallVoidMethod(jwindow, jWindowNotifyMoveToAnotherScreen, screen, (jlong) to_screen);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
            screen = to_screen;
        }
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

void WindowContextTop::set_window_resizable(bool res, bool grip) {
    if(!res) {
        int w = geometry_get_content_width(&geometry);
        int h = geometry_get_content_height(&geometry);
        if (w == -1 && h == -1) {
            gtk_window_get_size(GTK_WINDOW(gtk_widget), &w, &h);
        }
        GdkGeometry geom = {w, h, w, h, 0, 0, 0, 0, 0.0, 0.0, GDK_GRAVITY_NORTH_WEST};
        gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), NULL, &geom,
                static_cast<GdkWindowHints>(GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
        GLASS_GTK_WINDOW_SET_HAS_RESIZE_GRIP(gdk_window, FALSE);
        resizable.prev = resizable.value;
        resizable.value = false;
    } else {
        resizable.prev = resizable.value;
        resizable.value = true;
        update_window_constraints();
        if (grip) {
            GLASS_GTK_WINDOW_SET_HAS_RESIZE_GRIP(gdk_window, TRUE);
        }
    }
}

void WindowContextTop::set_resizable(bool res) {
    if (map_received) {
        set_window_resizable(res, true);
    } else {
        //Since window is not ready yet set only request for change of resizable.
        resizable.request  = res ? REQUEST_RESIZABLE : REQUEST_NOT_RESIZABLE;
    }
}


void WindowContextTop::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch) {
    if (!frame_extents_initialized && frame_type == TITLED) {
        initialize_frame_extents();
        frame_extents_initialized = true;
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

    if (xSet) {
        geometry.refx = x + geometry.current_width * geometry.gravity_x;
        windowChanges.x = geometry_get_window_x(&geometry);
        windowChangesMask |= CWX;

    } else if ((geometry.gravity_x != 0) && (windowChangesMask & CWWidth)) {
        windowChanges.x = geometry_get_window_x(&geometry);
        windowChangesMask |= CWX;
    }

    if (ySet) {
        geometry.refy = y + geometry.current_height * geometry.gravity_y;
        windowChanges.y = geometry_get_window_y(&geometry);
        windowChangesMask |= CWY;

    } else if ((geometry.gravity_y != 0) && (windowChangesMask & CWHeight)) {
        windowChanges.y = geometry_get_window_y(&geometry);
        windowChangesMask |= CWY;
    }

    window_configure(&windowChanges, windowChangesMask);

}

void WindowContextTop::process_map() {
    map_received = true;
    if (resizable.request != REQUEST_NONE) {
        set_window_resizable(resizable.request == REQUEST_RESIZABLE, true);
        resizable.request = REQUEST_NONE;
    }
}

void WindowContextTop::window_configure(XWindowChanges *windowChanges,
        unsigned int windowChangesMask) {
    if (windowChangesMask == 0) {
        return;
    }

    if (!gtk_widget_get_visible(gtk_widget)) {
        // not visible yet, synchronize with gtk only
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
            }

            gtk_window_resize(GTK_WINDOW(gtk_widget), newWidth, newHeight);
        }
        stale_config_notifications = 1;
        return;
    }

    ++stale_config_notifications;

    if (!resizable.value && (windowChangesMask & (CWWidth | CWHeight))) {
        XSizeHints *sizeHints = XAllocSizeHints();
        if (sizeHints != NULL) {
            int fixedWidth = (windowChangesMask & CWWidth)
                    ? windowChanges->width
                    : geometry_get_content_width(&geometry);
            int fixedHeight = (windowChangesMask & CWHeight)
                    ? windowChanges->height
                    : geometry_get_content_height(&geometry);

            sizeHints->flags = PMinSize | PMaxSize;

            sizeHints->min_width = 1;
            sizeHints->min_height = 1;
            sizeHints->max_width = INT_MAX;
            sizeHints->max_height = INT_MAX;
            XSetWMNormalHints(GDK_WINDOW_XDISPLAY(gdk_window),
                    GDK_WINDOW_XID(gdk_window),
                    sizeHints);

            XConfigureWindow(GDK_WINDOW_XDISPLAY(gdk_window),
                    GDK_WINDOW_XID(gdk_window),
                    windowChangesMask,
                    windowChanges);

            sizeHints->min_width = fixedWidth;
            sizeHints->min_height = fixedHeight;
            sizeHints->max_width = fixedWidth;
            sizeHints->max_height = fixedHeight;
            XSetWMNormalHints(GDK_WINDOW_XDISPLAY(gdk_window),
                    GDK_WINDOW_XID(gdk_window),
                    sizeHints);

            XFree(sizeHints);
            return;
        }
    }

    XConfigureWindow(GDK_WINDOW_XDISPLAY(gdk_window),
            GDK_WINDOW_XID(gdk_window),
            windowChangesMask,
            windowChanges);
}

void WindowContextTop::process_state(GdkEventWindowState *event) {
    if (event->changed_mask & (GDK_WINDOW_STATE_ICONIFIED
            | GDK_WINDOW_STATE_MAXIMIZED)) {
        jint stateChangeEvent;

        if (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MINIMIZE;
        } else if (event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MAXIMIZE;
        } else {
            stateChangeEvent = com_sun_glass_events_WindowEvent_RESTORE;

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
                    stateChangeEvent);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }
}


void WindowContextTop::applyShapeMask(cairo_surface_t* cairo_surface, uint width, uint height)
{
    if (frame_type != TRANSPARENT) {
        return;
    }
    Display *display = GDK_DISPLAY_XDISPLAY(glass_gdk_window_get_display(gdk_window));
    Screen *screen = GDK_SCREEN_XSCREEN(glass_gdk_window_get_screen(gdk_window));

    if (xshape.surface == NULL || width != xshape.width || height != xshape.height) {
        if (xshape.surface != NULL) {
            cairo_surface_destroy(xshape.surface);
            XFreePixmap(display,
                    xshape.pixmap);
        }
        xshape.pixmap = XCreatePixmap(
                display,
                GDK_WINDOW_XID(gdk_window),
                width, height, 1
                );
        xshape.surface = cairo_xlib_surface_create_for_bitmap(
                display,
                xshape.pixmap,
                screen,
                width,
                height
                );
        xshape.width = width;
        xshape.height = height;
    }

    cairo_t *xshape_context = cairo_create(xshape.surface);

    cairo_set_operator(xshape_context, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_surface(xshape_context, cairo_surface, 0, 0);

    cairo_paint(xshape_context);

    int type;
    if (gdk_display_supports_composite(glass_gdk_window_get_display(gdk_window))
            && gdk_screen_is_composited(glass_gdk_window_get_screen(gdk_window))) {
        type = ShapeInput;
    } else {
        type = ShapeBounding;
    }

    XShapeCombineMask(display,
            GDK_WINDOW_XID(gdk_window),
            type,
            0, 0, xshape.pixmap, ShapeSet
            );

    cairo_destroy(xshape_context);
}

void WindowContextTop::set_minimized(bool minimize) {
    if (minimize) {
        gtk_window_iconify(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_deiconify(GTK_WINDOW(gtk_widget));
    }
}
void WindowContextTop::set_maximized(bool maximize) {
    if (maximize) {
        gtk_window_maximize(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_unmaximize(GTK_WINDOW(gtk_widget));
    }
}

void WindowContextTop::enter_fullscreen() {
    gtk_window_fullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContextTop::exit_fullscreen() {
    gtk_window_unfullscreen(GTK_WINDOW(gtk_widget));
}

void WindowContextTop::request_focus() {
    gtk_window_present(GTK_WINDOW(gtk_widget));
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
        //set back proper resizable value.
        set_window_resizable(resizable.prev, true);
    } else {
        //disabled window can't be resizable.
        set_window_resizable(false, false);
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

void WindowContextTop::set_level(int level) {
    if (level == com_sun_glass_ui_Window_Level_NORMAL) {
        gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), FALSE);
    } else if (level == com_sun_glass_ui_Window_Level_FLOATING
            || level == com_sun_glass_ui_Window_Level_TOPMOST) {
        gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), TRUE);
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

WindowContextTop::~WindowContextTop() {
    if (xshape.surface) {
        cairo_surface_destroy(xshape.surface);
        XFreePixmap(GDK_DISPLAY_XDISPLAY(gdk_display_get_default()), xshape.pixmap);
    }
}

////////////////////////////// WindowContextPlug ////////////////////////////////

static gboolean plug_configure(GtkWidget *widget, GdkEvent *event, gpointer user_data) {
    if (event->type == GDK_CONFIGURE) {
        ((WindowContextPlug*)user_data)->process_gtk_configure(&event->configure);
    }
    return FALSE;
}

WindowContextPlug::WindowContextPlug(jobject _jwindow, void* _owner): WindowContextBase(), parent() {
    jwindow = mainEnv->NewGlobalRef(_jwindow);

    gtk_widget = gtk_plug_new((GdkNativeWindow)PTR_TO_JLONG(_owner));

    g_signal_connect(G_OBJECT(gtk_widget), "configure-event", G_CALLBACK(plug_configure), this);

    gtk_widget_set_size_request(gtk_widget, 0, 0);
    gtk_widget_set_events(gtk_widget, GDK_ALL_EVENTS_MASK);
    gtk_widget_set_can_focus(GTK_WIDGET(gtk_widget), TRUE);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);

    gtk_widget_realize(gtk_widget);
    gdk_window = gtk_widget_get_window(gtk_widget);

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

void WindowContextPlug::process_state(GdkEventWindowState *event) {
    if (event->changed_mask & (GDK_WINDOW_STATE_ICONIFIED
            | GDK_WINDOW_STATE_MAXIMIZED)) {
        jint stateChangeEvent;

        if (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MINIMIZE;
        } else if (event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MAXIMIZE;
        } else {
            stateChangeEvent = com_sun_glass_events_WindowEvent_RESTORE;

            int w, h;
            glass_gdk_window_get_size(gdk_window, &w, &h);
            if (jview) {
                mainEnv->CallVoidMethod(jview,
                        jViewNotifyRepaint,
                        0, 0, w, h);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
        }

        if (jwindow) {
            mainEnv->CallVoidMethod(jwindow,
                    jGtkWindowNotifyStateChanged,
                    stateChangeEvent);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
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
    WindowContext *ctx = (WindowContext *)user_data;
    ctx->process_focus(&event->focus_change);
    return TRUE;
}

WindowContextChild::WindowContextChild(jobject _jwindow,
                                       void* _owner,
                                       GtkWidget *parent_widget,
                                       WindowContextPlug *parent_ctx)
        : WindowContextBase(), parent(), full_screen_window(), view() {
    jwindow = mainEnv->NewGlobalRef(_jwindow);
    gtk_widget = gtk_drawing_area_new();
    parent = parent_ctx;

    glong xvisualID = (glong) mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);

    if (xvisualID != 0) {
        GdkVisual *visual = gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID);
        glass_gtk_window_configure_from_visual(gtk_widget, visual);
    }

    gtk_widget_set_events(gtk_widget, GDK_ALL_EVENTS_MASK);
    gtk_widget_set_can_focus(GTK_WIDGET(gtk_widget), TRUE);
    gtk_widget_set_app_paintable(gtk_widget, TRUE);
    gtk_container_add (GTK_CONTAINER(parent_widget), gtk_widget);
    gtk_widget_realize(gtk_widget);
    gdk_window = gtk_widget_get_window(gtk_widget);
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

void WindowContextChild::process_state(GdkEventWindowState *event) {
    if (event->changed_mask & (GDK_WINDOW_STATE_ICONIFIED
            | GDK_WINDOW_STATE_MAXIMIZED)) {
        jint stateChangeEvent;

        if (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MINIMIZE;
        } else if (event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED) {
            stateChangeEvent = com_sun_glass_events_WindowEvent_MAXIMIZE;
        } else {
            stateChangeEvent = com_sun_glass_events_WindowEvent_RESTORE;

            int w, h;
            glass_gdk_window_get_size(gdk_window, &w, &h);
            if (jview) {
                mainEnv->CallVoidMethod(jview,
                        jViewNotifyRepaint,
                        0, 0, w, h);
            }
            CHECK_JNI_EXCEPTION(mainEnv);
        }

        if (jwindow) {
            mainEnv->CallVoidMethod(jwindow,
                    jGtkWindowNotifyStateChanged,
                    stateChangeEvent);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    }
}

bool WindowContextChild::set_view(jobject view) {
    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
    }

    if (view) {
        gint width, height;
        jview = mainEnv->NewGlobalRef(view);
        width = gtk_widget->allocation.width;
        height = gtk_widget->allocation.height;
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
        newWidth = gtk_widget->allocation.width;
        newHeight = gtk_widget->allocation.height;

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

    full_screen_window = new WindowContextTop(jwindow, NULL, 0L, UNTITLED, NORMAL);
    int x, y, w, h;
    gdk_window_get_origin(gdk_window, &x, &y);
    gdk_window_get_geometry(gdk_window, NULL, NULL, &w, &h, NULL);
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
