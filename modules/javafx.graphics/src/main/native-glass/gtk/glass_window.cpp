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
#include "glass_evloop.h"

#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_ViewEvent.h>
#include <com_sun_glass_events_MouseEvent.h>
#include <com_sun_glass_events_KeyEvent.h>
#include <com_sun_glass_ui_Window_Level.h>

#include <cairo.h>
#include <gdk/gdk.h>

#include <string.h>
#include <algorithm>
#include <optional>

#define MOUSE_BACK_BTN 8
#define MOUSE_FORWARD_BTN 9

#define NONNEGATIVE_OR(val, fallback) (((val) < 0) ? (fallback) : (val))


void destroy_and_delete_ctx(WindowContext* ctx) {
    LOG0("destroy_and_delete_ctx\n");
    if (ctx) {
        ctx->process_destroy();

        if (!ctx->get_events_count()) {
            LOG0("delete ctx\n");
            delete ctx;
        }
        // else: ctx will be deleted in EventsCounterHelper after completing
        // an event processing
    }
}

static bool gdk_visual_is_rgba(GdkVisual *visual) {
    if (!visual) return false;

    int depth = gdk_visual_get_depth(visual);
    guint32 red_mask, green_mask, blue_mask;
    gdk_visual_get_red_pixel_details(visual, &red_mask, NULL, NULL);
    gdk_visual_get_green_pixel_details(visual, &green_mask, NULL, NULL);
    gdk_visual_get_blue_pixel_details(visual, &blue_mask, NULL, NULL);

    return (depth == 32
            && red_mask == 0xff0000
            && green_mask == 0x00ff00
            && blue_mask == 0x0000ff);
}

static gboolean is_window_floating(GdkWindowState state) {
    return !(state & GDK_WINDOW_STATE_MAXIMIZED)
        && !(state & GDK_WINDOW_STATE_FULLSCREEN);
}

static inline jint gdk_button_number_to_mouse_button(guint button) {
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

WindowContext * WindowContext::sm_grab_window = NULL;
WindowContext * WindowContext::sm_mouse_drag_window = NULL;

// Work-around because frame extents are only obtained after window is shown.
// This is used to know the total window size (content + decoration)
// The first window will have a duplicated resize event, subsequent windows will use the cached value.
std::optional<GdkRectangle> WindowContext::normal_extents;
std::optional<GdkRectangle> WindowContext::utility_extents;

WindowContext::WindowContext(jobject _jwindow, WindowContext* _owner, long _screen,
        WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf) :
            screen(_screen),
            frame_type(_frame_type),
            window_type(type),
            owner(_owner),
            geometry(),
            resizable(),
            im_ctx() {
    jwindow = mainEnv->NewGlobalRef(_jwindow);
    initial_wmf = wmf;
    current_wmf = wmf;
    is_mouse_entered = false;
    is_disabled = false;
    on_top = false;
    can_be_deleted = false;
    mapped = false;
    initial_state_mask = 0;

    if (frame_type != TITLED) {
        initial_wmf = GDK_FUNC_ALL;
    }

    load_cached_extents();
    update_window_size();

    GdkWindowAttributesType attr_types = (GdkWindowAttributesType) (GDK_WA_VISUAL);
    GdkWindowAttr attributes;
    attributes.visual = find_best_visual();
    attributes.wclass = GDK_INPUT_OUTPUT;
    attributes.event_mask = GDK_FILTERED_EVENTS_MASK;
    attributes.width = DEFAULT_WIDTH;
    attributes.height = DEFAULT_HEIGHT;

    if (window_type == POPUP) {
        attributes.window_type = GDK_WINDOW_TEMP;
        attr_types = (GdkWindowAttributesType) (attr_types | GDK_WA_NOREDIR);
        attributes.override_redirect = TRUE;
    } else {
        attributes.window_type = GDK_WINDOW_TOPLEVEL;
        if (gchar* app_name = get_application_name()) {
            attributes.wmclass_name = app_name;
            attributes.wmclass_class = app_name;
            attr_types = (GdkWindowAttributesType) (attr_types | GDK_WA_WMCLASS);
        }
    }

    if (window_type == UTILITY) {
        attributes.type_hint = GDK_WINDOW_TYPE_HINT_UTILITY;
        attr_types = (GdkWindowAttributesType) (attr_types | GDK_WA_TYPE_HINT);
    }

    gdk_window = gdk_window_new(gdk_get_default_root_window(), &attributes, attr_types);

    if (frame_type == TITLED) {
        request_frame_extents();
    }

    if (frame_type != TRANSPARENT) {
        GdkRGBA white = { 1.0, 1.0, 1.0, 1.0 };
        gdk_window_set_background_rgba(gdk_window, &white);
    }

    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, NULL);
    gdk_window_register_dnd(gdk_window);

    if (initial_wmf) {
        gdk_window_set_functions(gdk_window, initial_wmf);
    }

    if (frame_type != TITLED) {
        gdk_window_set_decorations(gdk_window,  (GdkWMDecoration) 0);
    }

    if (owner) {
        owner->add_child(this);
        if (on_top_inherited()) {
            gdk_window_set_keep_above(gdk_window, TRUE);
        }
    }

    set_title("");
    update_window_constraints();
}

GdkVisual* WindowContext::find_best_visual() {
    // This comes from prism-es2
    static glong xvisualID = (glong)mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);
    static GdkVisual *prismVisual = (xvisualID != 0)
                ? gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID)
                : NULL;

    if (frame_type == TRANSPARENT && !gdk_visual_is_rgba(prismVisual)) {
        GdkVisual *rgbaVisual = gdk_screen_get_rgba_visual(gdk_screen_get_default());
        if (rgbaVisual) {
            return rgbaVisual;
        } else {
            glass_print_window_transparency_not_supported();
        }
    }

    if (prismVisual != NULL) {
        LOG0("Using prism visual");
        return prismVisual;
    }

    LOG0("Using GDK system visual\n");
    return gdk_screen_get_system_visual(gdk_screen_get_default());
}

GdkWindow* WindowContext::get_gdk_window() {
    if (GDK_IS_WINDOW(gdk_window)) {
        return gdk_window;
    }

    return NULL;
}

// Returns de XWindow ID to be used in rendering
XID WindowContext::get_native_window() {
    // This is used to delay the window map (it's only really mapped when there's
    // something rendered)
    if (!is_visible()) return 0;

    return GDK_WINDOW_XID(gdk_window);
}

jobject WindowContext::get_jview() {
    return jview;
}

jobject WindowContext::get_jwindow() {
    return jwindow;
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

void WindowContext::process_map() {
    // We need only first map
    if (mapped || window_type == POPUP) return;

    LOG0("--------------------------------------------------------> mapped\n");
    move_resize(geometry.x, geometry.y, true, true, geometry.width.view, geometry.height.view);
    mapped = true;

    if (initial_state_mask != 0) {
        update_initial_state();
    }
}

void WindowContext::process_focus(GdkEventFocus *event) {
    if (!event->in && WindowContext::sm_grab_window == this) {
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

void WindowContext::increment_events_counter() {
    ++events_processing_cnt;
}

void WindowContext::decrement_events_counter() {
    --events_processing_cnt;
}

size_t WindowContext::get_events_count() {
    return events_processing_cnt;
}

bool WindowContext::is_dead() {
    return can_be_deleted;
}

void WindowContext::process_destroy() {
    LOG0("process_destroy\n");

    if (owner) {
        owner->remove_child(this);
    }

    if (WindowContext::sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }

    if (WindowContext::sm_grab_window == this) {
        ungrab_focus();
    }

    std::set<WindowContext*>::iterator it;
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

void WindowContext::process_delete() {
    LOG0("process_delete\n");
    if (jwindow && isEnabled()) {
        LOG0("jWindowNotifyClose\n");
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyClose);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_repaint() {
    if (jview) {
        LOG0("jViewNotifyRepaint\n");
        mainEnv->CallVoidMethod(jview, jViewNotifyRepaint, 0, 0,
                            geometry.width.view, geometry.height.view);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_repaint(GdkRectangle *rect) {
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyRepaint, rect->x, rect->y, rect->width, rect->height);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::process_mouse_button(GdkEventButton *event) {
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

    jint button = gdk_button_number_to_mouse_button(event->button);

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

void WindowContext::process_mouse_motion(GdkEventMotion *event) {
    jint glass_modifier = gdk_modifier_mask_to_glass(event->state);
    jint isDrag = glass_modifier & (
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK |
            com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD);
    jint button = com_sun_glass_events_MouseEvent_BUTTON_NONE;

    if (isDrag && WindowContext::sm_mouse_drag_window == NULL) {
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

void WindowContext::process_mouse_scroll(GdkEventScroll *event) {
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

void WindowContext::process_mouse_cross(GdkEventCrossing *event) {
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

    // TYPED events should only be sent for printable characters.
    // jview is checked again because previous call might be an exit key
    if (press && key > 0 && jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                com_sun_glass_events_KeyEvent_TYPED,
                com_sun_glass_events_KeyEvent_VK_UNDEFINED,
                jChars,
                glassModifier);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::paint(void* data, jint width, jint height) {
    cairo_rectangle_int_t rect = {0, 0, width, height};
    cairo_region_t *region = cairo_region_create_rectangle(&rect);
    gdk_window_begin_paint_region(gdk_window, region);

    cairo_t* context = gdk_cairo_create(gdk_window);

    cairo_surface_t* cairo_surface =
        cairo_image_surface_create_for_data(
            (unsigned char*)data,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    cairo_set_source_surface(context, cairo_surface, 0, 0);
    cairo_set_operator(context, CAIRO_OPERATOR_SOURCE);
    cairo_paint(context);

    gdk_window_end_paint(gdk_window);
    cairo_region_destroy(region);

    cairo_destroy(context);
    cairo_surface_destroy(cairo_surface);
}

void WindowContext::add_child(WindowContext* child) {
    children.insert(child);
    gdk_window_set_transient_for(child->get_gdk_window(), gdk_window);
}

void WindowContext::remove_child(WindowContext* child) {
    children.erase(child);
}

bool WindowContext::is_visible() {
    return gdk_window_is_visible(gdk_window);
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

bool WindowContext::grab_mouse_drag_focus() {
    if (glass_gdk_mouse_devices_grab_with_cursor(
            gdk_window, gdk_window_get_cursor(gdk_window), FALSE)) {
        WindowContext::sm_mouse_drag_window = this;
        return true;
    } else {
        return false;
    }
}

void WindowContext::ungrab_mouse_drag_focus() {
    WindowContext::sm_mouse_drag_window = NULL;
    glass_gdk_mouse_devices_ungrab();
    if (WindowContext::sm_grab_window) {
        WindowContext::sm_grab_window->grab_focus();
    }
}

bool WindowContext::grab_focus() {
    if (WindowContext::sm_mouse_drag_window
            || glass_gdk_mouse_devices_grab(gdk_window)) {
        WindowContext::sm_grab_window = this;
        return true;
    } else {
        return false;
    }
}

void WindowContext::ungrab_focus() {
    if (!WindowContext::sm_mouse_drag_window) {
        glass_gdk_mouse_devices_ungrab();
    }
    WindowContext::sm_grab_window = NULL;

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::set_cursor(GdkCursor* cursor) {
    if (!is_in_drag()) {
        if (WindowContext::sm_mouse_drag_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    WindowContext::sm_mouse_drag_window->get_gdk_window(), cursor, FALSE);
        } else if (WindowContext::sm_grab_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    WindowContext::sm_grab_window->get_gdk_window(), cursor, TRUE);
        }
    }
    gdk_window_set_cursor(gdk_window, cursor);
}

void WindowContext::set_background(float r, float g, float b) {
    GdkRGBA rgba = {r, g, b, 1.0};
    gdk_window_set_background_rgba(gdk_window, &rgba);
}

GdkAtom WindowContext::get_net_frame_extents_atom() {
    static GdkAtom atom = NULL;
    if (atom == NULL) {
        atom = gdk_atom_intern_static_string("_NET_FRAME_EXTENTS");
    }
    return atom;
}

void WindowContext::request_frame_extents() {
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


void WindowContext::update_initial_state() {
    GdkWindowState state = gdk_window_get_state(gdk_window);

    if (initial_state_mask & GDK_WINDOW_STATE_MAXIMIZED) {
        LOG0("update_initial_state: maximized\n");
        maximize(true);
    }

    if (initial_state_mask & GDK_WINDOW_STATE_FULLSCREEN) {
        LOG0("update_initial_state: fullscreen\n");
        enter_fullscreen();
    }

    if (initial_state_mask & GDK_WINDOW_STATE_ICONIFIED) {
        LOG0("update_initial_state: iconify\n");
        iconify(true);
    }

    initial_state_mask = 0;
}

void WindowContext::update_frame_extents() {
    if (frame_type != TITLED) return;

    int top, left, bottom, right;

    if (get_frame_extents_property(&top, &left, &bottom, &right)) {
        if (top > 0 || right > 0 || bottom > 0 || left > 0) {
            bool changed = geometry.extents.x != left
                        || geometry.extents.y != top
                        || geometry.extents.width != (left + right)
                        || geometry.extents.height != (top + bottom);

            LOG1(" ------------------------------------------- frame extents - changed: %d\n", changed);

            if (!changed) return;

            GdkRectangle rect = { left, top, (left + right), (top + bottom) };
            set_cached_extents(rect);

            int newW = geometry.width.view;
            int newH = geometry.height.view;

            // Here the user might change the desktop theme and in consequence
            // change decoration sizes.

            if (geometry.width.type == BOUNDSTYPE_WINDOW) {
                // Re-add the extents and then subtract the new
                newW = newW
                    + ((geometry.frame_extents_received) ? geometry.extents.width : 0)
                    - rect.width;
            }

            if (geometry.height.type == BOUNDSTYPE_WINDOW) {
                // Re-add the extents and then subtract the new
                newH = newH
                    + ((geometry.frame_extents_received) ? geometry.extents.height : 0)
                    - rect.height;
            }

            newW = NONNEGATIVE_OR(newW, 1);
            newH = NONNEGATIVE_OR(newH, 1);

            LOG2("extents received -> new view size: %d, %d\n", newW, newH);
            int x = geometry.x;
            int y = geometry.y;

            // Gravity x, y are used in centerOnScreen(). Here it's used to adjust the position
            // accounting decorations
            if (geometry.gravity_x > 0 && x > 0) {
                x -= geometry.gravity_x * (float) (geometry.extents.width);
                x = NONNEGATIVE_OR(x, 0);
            }

            if (geometry.gravity_y > 0 && y > 0) {
                y -= geometry.gravity_y  * (float) (geometry.extents.height);
                y = NONNEGATIVE_OR(y, 0);
            }

            geometry.extents = rect;
            geometry.frame_extents_received = true;
            geometry.width.view = newW;
            geometry.height.view = newH;
            geometry.x = x;
            geometry.y = y;
            update_window_size();

            LOG6("Geometry after frame extents: x,y: %d,%d / cw,ch: %d,%d / ww,wh: %d,%d\n", geometry.x, geometry.y,
                    geometry.width.view, geometry.height.view, geometry.width.window, geometry.height.window);

            update_window_constraints();

            if (is_window_floating(gdk_window_get_state(gdk_window))) {
                move_resize(x, y, true, true, newW, newH);
            }
        }
    }
}

bool WindowContext::get_frame_extents_property(int *top, int *left,
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

void WindowContext::set_cached_extents(GdkRectangle ex) {
    if (window_type == UTILITY) {
        utility_extents = ex;
    } else {
        normal_extents = ex;
    }
}

void WindowContext::load_cached_extents() {
    if (frame_type != TITLED) return;

    if (window_type == NORMAL && normal_extents.has_value()) {
        geometry.extents = normal_extents.value();
        LOG4("Loaded Normal Extents: x = %d, y = %d, width = %d, height = %d\n",
                    geometry.extents.x, geometry.extents.y, geometry.extents.width, geometry.extents.height);
        geometry.frame_extents_received = true;
        return;
    }

    if (window_type == UTILITY && utility_extents.has_value()) {
        geometry.extents = utility_extents.value();
        LOG4("Loaded Utility Extents: x = %d, y = %d, width = %d, height = %d\n",
                    geometry.extents.x, geometry.extents.y, geometry.extents.width, geometry.extents.height);
        geometry.frame_extents_received = true;
    }
}

void WindowContext::process_property_notify(GdkEventProperty *event) {
//    LOG1("process_property_notify: %s\n", gdk_atom_name(event->atom));
    if (event->atom == get_net_frame_extents_atom()) {
        update_frame_extents();
    }
}

void WindowContext::process_state(GdkEventWindowState *event) {
    if (!(event->changed_mask & (GDK_WINDOW_STATE_ICONIFIED
                                | GDK_WINDOW_STATE_MAXIMIZED
                                | GDK_WINDOW_STATE_FULLSCREEN
                                | GDK_WINDOW_STATE_ABOVE))) {
        return;
    }

    LOG0("process_state\n");
    if (event->changed_mask & GDK_WINDOW_STATE_ABOVE) {
        notify_on_top(event->new_window_state & GDK_WINDOW_STATE_ABOVE);

        // Only state mask
        if (event->new_window_state == GDK_WINDOW_STATE_ABOVE) return;
    }

    if ((event->changed_mask & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_ICONIFIED))
        && ((event->new_window_state & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_ICONIFIED)) == 0)) {
        LOG0("com_sun_glass_events_WindowEvent_RESTORE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_RESTORE);
    } else if (event->new_window_state & (GDK_WINDOW_STATE_ICONIFIED)) {
        LOG0("com_sun_glass_events_WindowEvent_MINIMIZE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_MINIMIZE);
    } else if (event->new_window_state & (GDK_WINDOW_STATE_MAXIMIZED)) {
        LOG0("com_sun_glass_events_WindowEvent_MAXIMIZE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_MAXIMIZE);
    }

    if (event->changed_mask & GDK_WINDOW_STATE_ICONIFIED
        && (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) == 0) {
        remove_wmf(GDK_FUNC_MINIMIZE);

        //FIXME: remove when 8351867 is fixed
        notify_repaint();
    }

    // If only iconified, no further processing
    if (event->new_window_state == GDK_WINDOW_STATE_ICONIFIED) return;

    if (event->changed_mask & GDK_WINDOW_STATE_MAXIMIZED
        && (event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED) == 0) {
        remove_wmf(GDK_FUNC_MAXIMIZE);
    }

    if (jview && event->changed_mask & GDK_WINDOW_STATE_FULLSCREEN) {
        if (event->new_window_state & GDK_WINDOW_STATE_FULLSCREEN) {
            LOG0("com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER\n");
            mainEnv->CallVoidMethod(jview, jViewNotifyView, com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER);
            CHECK_JNI_EXCEPTION(mainEnv)
        } else {
            LOG0("com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT\n");
            mainEnv->CallVoidMethod(jview, jViewNotifyView, com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    }

    notify_view_resize();
    // Since FullScreen (or custom modes of maximized) can undecorate the
    // window, request view position change
    notify_view_move();
}

void WindowContext::notify_window_resize(int state) {
    if (jwindow) {
        LOG3("jWindowNotifyResize: %d -> %d, %d\n", state,
                    geometry.width.window, geometry.height.window);
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize, state,
                    geometry.width.window, geometry.height.window);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_window_move() {
    if (jwindow) {
        LOG2("jWindowNotifyMove: %d, %d\n", geometry.x, geometry.y);
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyMove, geometry.x, geometry.y);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_view_resize() {
    if (jview) {
        LOG2("jViewNotifyResize: %d, %d\n", geometry.width.view, geometry.height.view);
        mainEnv->CallVoidMethod(jview, jViewNotifyResize, geometry.width.view, geometry.height.view);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_current_sizes() {
    GdkWindowState state = gdk_window_get_state(gdk_window);

    notify_window_resize((state & GDK_WINDOW_STATE_MAXIMIZED)
                                ? com_sun_glass_events_WindowEvent_MAXIMIZE
                                : com_sun_glass_events_WindowEvent_RESIZE);

    notify_view_resize();
}

void WindowContext::notify_view_move() {
    if (jview) {
        LOG0("com_sun_glass_events_ViewEvent_MOVE\n");
        mainEnv->CallVoidMethod(jview, jViewNotifyView,
                com_sun_glass_events_ViewEvent_MOVE);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::process_configure(GdkEventConfigure *event) {
    LOG5("Configure Event - send_event: %d, x: %d, y: %d, width: %d, height: %d\n",
            event->send_event, event->x, event->y, event->width, event->height);

    GdkWindowState state = gdk_window_get_state(gdk_window);

    if (state & GDK_WINDOW_STATE_ICONIFIED) {
        return;
    }

    int root_x, root_y, origin_x, origin_y;
    gdk_window_get_root_origin(gdk_window, &root_x, &root_y);
    gdk_window_get_origin(gdk_window, &origin_x, &origin_y);

    // view_x and view_y represent the position of the content relative to the left corner of the window,
    // taking into account window decorations (such as title bars and borders) applied by the window manager
    // and might vary by window state.
    geometry.view_x = origin_x - root_x;
    geometry.view_y = origin_y - root_y;
    LOG2("view x, y: %d, %d\n", geometry.view_x, geometry.view_y);

    int ww = event->width;
    int wh = event->height;

    // Fullscreen usually have no decorations
    if (geometry.view_x > 0) {
        ww += geometry.extents.width;
    }

    if (geometry.view_y > 0) {
        wh += geometry.extents.height;
    }

    if (mapped) {
        geometry.x = root_x;
        geometry.y = root_y;
        geometry.width.view = event->width;
        geometry.height.view = event->height;
        geometry.width.window = ww;
        geometry.height.window = wh;
    }

    notify_window_resize((state & GDK_WINDOW_STATE_MAXIMIZED)
                            ? com_sun_glass_events_WindowEvent_MAXIMIZE
                            : com_sun_glass_events_WindowEvent_RESIZE);
    notify_view_resize();

    notify_window_move();
    notify_view_move();

    glong to_screen = getScreenPtrForLocation(event->x, event->y);
    if (to_screen != -1 && to_screen != screen) {
        if (jwindow) {
            LOG0("jWindowNotifyMoveToAnotherScreen\n");
            //notify screen changed
            jobject jScreen = createJavaScreen(mainEnv, to_screen);
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyMoveToAnotherScreen, jScreen);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
        screen = to_screen;
    }
}

void WindowContext::update_window_constraints() {
    update_window_constraints(false);
}

void WindowContext::update_window_constraints(bool ignored_resizable) {
    // Not ready to re-apply the constraints
    if (!is_window_floating(gdk_window_get_state(gdk_window))
        || !is_window_floating((GdkWindowState) initial_state_mask)) {
        LOG0("not floating: update_window_constraints ignored\n");
        return;
    }

    GdkGeometry hints;

    if ((resizable.value || ignored_resizable) && !is_disabled) {
        hints.min_width = (resizable.minw == -1)
                     ? 1
                     : NONNEGATIVE_OR(resizable.minw - geometry.extents.width, 1);
        hints.min_height = (resizable.minh == -1)
                     ? 1
                     : NONNEGATIVE_OR(resizable.minh - geometry.extents.height, 1);
        hints.max_width = (resizable.maxw == -1)
                    ? G_MAXINT
                    : NONNEGATIVE_OR(resizable.maxw - geometry.extents.width, 1);
        hints.max_height = (resizable.maxh == -1)
                    ? G_MAXINT
                    : NONNEGATIVE_OR(resizable.maxh - geometry.extents.height, 1);
    } else {
        hints.min_width = geometry.width.view;
        hints.min_height = geometry.height.view;
        hints.max_width = geometry.width.view;
        hints.max_height = geometry.height.view;
    }

    LOG4("geometry hints: min w,h: %d, %d - max w,h: %d, %d\n", hints.min_width,
            hints.min_height, hints.max_width, hints.max_height);

    // GDK_HINT_USER_POS is used for the initial position to work
    gdk_window_set_geometry_hints(gdk_window, &hints,
            (GdkWindowHints) (GDK_HINT_USER_POS |  GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
}

void WindowContext::set_resizable(bool res) {
    LOG1("set_resizable: %d\n", res);
    resizable.value = res;
    update_window_constraints();
}

void WindowContext::set_visible(bool visible) {
    LOG1("set_visible: %d\n", visible);
    if (visible) {
        gdk_window_show(gdk_window);

        // JDK-8220272 - fire event first because GDK_FOCUS_CHANGE is not always in order
        if (jwindow && isEnabled()) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);
            CHECK_JNI_EXCEPTION(mainEnv);
        }
    } else {
        gdk_window_hide(gdk_window);
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

void WindowContext::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch,
                               float gravity_x, float gravity_y) {
    LOG10("set_bounds -> x = %d, y = %d, xset = %d, yset = %d, w = %d, h = %d, cw = %d, ch = %d, gx = %f, gy = %f\n",
            x, y, xSet, ySet, w, h, cw, ch, gravity_x, gravity_y);
    // newW / newH are view/content sizes
    int newW = 0;
    int newH = 0;

    geometry.gravity_x = gravity_x;
    geometry.gravity_y = gravity_y;

    if (w > 0) {
        geometry.width.type = BOUNDSTYPE_WINDOW;
        newW = NONNEGATIVE_OR(w - geometry.extents.width, 1);
    } else if (cw > 0) {
        // once set to window, stick with it
        if (BOUNDSTYPE_UNKNOWN) geometry.width.type = BOUNDSTYPE_VIEW;
        newW = cw;
    }

    if (h > 0) {
        geometry.height.type = BOUNDSTYPE_WINDOW;
        newH = NONNEGATIVE_OR(h - geometry.extents.height, 1);
    } else if (ch > 0) {
        // once set to window, stick with it
        if (BOUNDSTYPE_UNKNOWN) geometry.height.type = BOUNDSTYPE_VIEW;
        newH = ch;
    }

    GdkWindowState state = gdk_window_get_state(gdk_window);

    // Ignore when maximized / fullscreen
    if (!is_window_floating(state)) {
        notify_current_sizes();
        notify_window_move();
        return;
    }

    move_resize(x, y, xSet, ySet, newW, newH);
}

void WindowContext::iconify(bool state) {
    if (state) {
        add_wmf(GDK_FUNC_MINIMIZE);
        gdk_window_iconify(gdk_window);
    } else {
        gdk_window_deiconify(gdk_window);
        gdk_window_focus(gdk_window, GDK_CURRENT_TIME);
    }
}

void WindowContext::maximize(bool state) {
    if (state) {
        add_wmf(GDK_FUNC_MAXIMIZE);
        update_window_constraints(true);
        gdk_window_maximize(gdk_window);
    } else {
        gdk_window_unmaximize(gdk_window);
    }
}

void WindowContext::set_minimized(bool state) {
    LOG1("set_minimized = %d\n", state);
    if (mapped) {
        iconify(state);
    } else {
        initial_state_mask = state
            ? (initial_state_mask | GDK_WINDOW_STATE_ICONIFIED)
            : (initial_state_mask & ~GDK_WINDOW_STATE_ICONIFIED);
    }
}

void WindowContext::set_maximized(bool state) {
    LOG1("set_maximized = %d\n", state);
    if (mapped) {
        maximize(state);
    } else {
        initial_state_mask = state
            ? (initial_state_mask | GDK_WINDOW_STATE_MAXIMIZED)
            : (initial_state_mask & ~GDK_WINDOW_STATE_MAXIMIZED);
    }
}

void WindowContext::enter_fullscreen() {
    LOG0("enter_fullscreen\n");
    if (mapped) {
        update_window_constraints(true);
        gdk_window_fullscreen(gdk_window);
    } else {
        initial_state_mask |= GDK_WINDOW_STATE_FULLSCREEN;
    }
}

void WindowContext::exit_fullscreen() {
    LOG0("exit_fullscreen\n");
    if (mapped) {
        gdk_window_unfullscreen(gdk_window);
    } else {
        initial_state_mask &= ~GDK_WINDOW_STATE_FULLSCREEN;
    }
}

void WindowContext::request_focus() {
    LOG0("request_focus\n");
    if (!is_visible()) return;

    gdk_window_focus(gdk_window, gdk_x11_display_get_user_time(gdk_window_get_display(gdk_window)));
}

void WindowContext::set_focusable(bool focusable) {
    gdk_window_set_accept_focus(gdk_window, focusable ? TRUE : FALSE);
}

void WindowContext::set_title(const char* title) {
    gdk_window_set_title(gdk_window, title);
}

// This only works o Xorg
void WindowContext::set_alpha(double alpha) {
    gdk_window_set_opacity(gdk_window, (gdouble)alpha);
}

void WindowContext::set_enabled(bool enabled) {
    is_disabled = !enabled;
    update_window_constraints();
}

void WindowContext::set_minimum_size(int w, int h) {
    LOG2("set_minimum_size: %d, %d\n", w, h);
    resizable.minw = w;
    resizable.minh = h;
    update_window_constraints();
}

void WindowContext::set_maximum_size(int w, int h) {
    LOG2("set_maximum_size: %d, %d\n", w, h);
    resizable.maxw = (w == -1) ? -1 : w;
    resizable.maxh = (h == -1) ? -1 : h;
    update_window_constraints();
}

void WindowContext::set_icon(GdkPixbuf* icon) {
    if (icon == NULL || !GDK_IS_PIXBUF (icon)) return;

    GList *icons = NULL;
    icons = g_list_append(icons, icon);
    gdk_window_set_icon_list(gdk_window, icons);
    g_list_free(icons);
}

void WindowContext::to_front() {
    LOG0("to_front\n");
    gdk_window_raise(gdk_window);
}

void WindowContext::to_back() {
    LOG0("to_back\n");
    gdk_window_lower(gdk_window);
}

void WindowContext::set_modal(bool modal, WindowContext* parent) {
    if (modal) {
        if (parent) {
            gdk_window_set_transient_for(gdk_window, parent->get_gdk_window());
        }
    }
    gdk_window_set_modal_hint(gdk_window, modal ? TRUE : FALSE);
}

WindowGeometry WindowContext::get_geometry() {
    return geometry;
}

void WindowContext::update_ontop_tree(bool on_top) {
    bool effective_on_top = on_top || this->on_top;
    gdk_window_set_keep_above(gdk_window, effective_on_top ? TRUE : FALSE);
    for (std::set<WindowContext*>::iterator it = children.begin(); it != children.end(); ++it) {
        (*it)->update_ontop_tree(effective_on_top);
    }
}

bool WindowContext::on_top_inherited() {
    WindowContext* o = owner;
    while (o) {
        WindowContext* topO = dynamic_cast<WindowContext*>(o);
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
        WindowContext* topO = dynamic_cast<WindowContext*>(owner);
        return (topO && topO->effective_on_top()) || on_top;
    }
    return on_top;
}

void WindowContext::update_window_size() {
    geometry.width.window = geometry.width.view;
    geometry.height.window = geometry.height.view;

    if (frame_type == TITLED) {
        geometry.width.window += geometry.extents.width;
        geometry.height.window += geometry.extents.height;
    }
}

// Values are view size
void WindowContext::move_resize(int x, int y, bool xSet, bool ySet, int width, int height) {
    LOG4("move_resize: x,y: %d,%d / cw,ch: %d,%d\n", x, y, width, height);
    int newW = (width > 0) ? width : geometry.width.view;
    int newH = (height > 0) ? height : geometry.height.view;

    // Windows that are undecorated or transparent will not respect
    // minimum or maximum size constraints
    if (resizable.minw > 0 && newW < resizable.minw) {
        newW = NONNEGATIVE_OR(resizable.minw - geometry.extents.width, 1);
    }

    if (resizable.maxw > 0 && newW > resizable.maxw) {
        newW = NONNEGATIVE_OR(resizable.maxw - geometry.extents.width, 1);
    }

    if (resizable.minh > 0 && newH < resizable.minh) {
        newH = NONNEGATIVE_OR(resizable.minh - geometry.extents.height, 1);
    }

    if (resizable.maxh > 0 && newH > resizable.maxh) {
        newH = NONNEGATIVE_OR(resizable.maxh - geometry.extents.height, 1);
    }

    if (mapped && geometry.width.view == newW && geometry.height.view == newH) {
        // No change
        newW = -1;
        newH = -1;
    } else {
        geometry.width.view = newW;
        geometry.height.view = newH;
    }

    update_window_size();

    if (!resizable.value) {
        update_window_constraints();
    }

    if (xSet) geometry.x = x;
    if (ySet) geometry.y = y;

    LOG6("gdk_window_move_resize: x,y: %d,%d / cw,ch: %d,%d / ww,wh: %d,%d\n",
        geometry.x, geometry.y, newW, newH, geometry.width.window, geometry.width.window);

    gdk_window_move_resize(gdk_window, geometry.x, geometry.y, newW, newH);

    // Or else let to process_configure
    if (!mapped) {
        notify_window_move();
        notify_current_sizes();
    }
}

void WindowContext::add_wmf(GdkWMFunction wmf) {
    if ((initial_wmf & wmf) == 0) {
        current_wmf = (GdkWMFunction)((int)current_wmf | (int)wmf);
        gdk_window_set_functions(gdk_window, current_wmf);
    }
}

void WindowContext::remove_wmf(GdkWMFunction wmf) {
    if ((initial_wmf & wmf) == 0) {
        current_wmf = (GdkWMFunction)((int)current_wmf & ~(int)wmf);
        gdk_window_set_functions(gdk_window, current_wmf);
    }
}

void WindowContext::notify_on_top(bool top) {
    // Do not report effective (i.e. native) values to the FX, only if the user sets it manually
    if (top != effective_on_top() && jwindow) {
        if (on_top_inherited() && !top) {
            // Disallow user's "on top" handling on windows that inherited the property
            gdk_window_set_keep_above(gdk_window, TRUE);
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

void WindowContext::set_owner(WindowContext * owner_ctx) {
    owner = owner_ctx;
}

void WindowContext::update_view_size() {
    if (jview) {
        notify_view_resize();
    }
}

WindowContext::~WindowContext() {
    LOG0("~WindowContext\n");
    disableIME();
    gdk_window_destroy(gdk_window);
}

