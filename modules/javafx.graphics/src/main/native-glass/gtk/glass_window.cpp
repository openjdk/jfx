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
#include <com_sun_glass_ui_gtk_GtkWindow.h>

#include <cairo.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>

#include <algorithm>
#include <optional>

#define MOUSE_BACK_BTN 8
#define MOUSE_FORWARD_BTN 9

// Resize border width of EXTENDED windows
#define RESIZE_BORDER_WIDTH 5


void destroy_and_delete_ctx(WindowContext* ctx) {
    LOG(LIFECYCLE, "", "destroy_and_delete_ctx\n");
    if (!ctx) return;

    ctx->process_destroy();

    if (!ctx->get_events_count()) {
        LOG(LIFECYCLE, "", "destroy_and_delete_ctx: deleting\n");
        delete ctx;
    }
    // else: ctx will be deleted in EventsCounterHelper after completing
    // an event processing
}

static bool gdk_visual_is_rgba(GdkVisual *visual) {
    if (!visual) return false;

    int depth = gdk_visual_get_depth(visual);
    guint32 red_mask, green_mask, blue_mask;
    gdk_visual_get_red_pixel_details(visual, &red_mask, nullptr, nullptr);
    gdk_visual_get_green_pixel_details(visual, &green_mask, nullptr, nullptr);
    gdk_visual_get_blue_pixel_details(visual, &blue_mask, nullptr, nullptr);

    return (depth == 32
            && red_mask == 0xff0000
            && green_mask == 0x00ff00
            && blue_mask == 0x0000ff);
}

// Iconified not considered here
static bool is_state_floating(GdkWindowState state) {
    return (state & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_FULLSCREEN)) == 0;
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

WindowContext * WindowContext::sm_grab_window = nullptr;
WindowContext * WindowContext::sm_mouse_drag_window = nullptr;

// Work-around because frame extents are only obtained after window is shown.
// This is used to know the total window size (content + decoration)
// The first window will have a duplicated resize event, subsequent windows will use the cached value.
std::optional<Rectangle> WindowContext::normal_extents;
std::optional<Rectangle> WindowContext::utility_extents;

static void event_realize(GtkWidget* self, gpointer user_data) {
    WindowContext *ctx = static_cast<WindowContext*>(user_data);
    ctx->process_realize();
}

WindowContext::WindowContext(jobject _jwindow, WindowContext* _owner, long _screen,
        WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf) :
            owner(_owner),
            screen(_screen),
            frame_type(_frame_type),
            window_type(type),
            initial_wmf(wmf),
            current_wmf(wmf) {
    jwindow = mainEnv->NewGlobalRef(_jwindow);

    log_id = "";
    LOG(LIFECYCLE, log_id, "WindowContext: created\n");

    if (frame_type != TITLED) {
        initial_wmf = GDK_FUNC_ALL;
    }

    gtk_widget = gtk_window_new(type == POPUP ? GTK_WINDOW_POPUP : GTK_WINDOW_TOPLEVEL);
    g_signal_connect(G_OBJECT(gtk_widget), "realize", G_CALLBACK(event_realize), this);

    if (gchar* app_name = get_application_name()) {
        gtk_window_set_wmclass(GTK_WINDOW(gtk_widget), app_name, app_name);
        g_free(app_name);
    }

    if (window_type == UTILITY && frame_type != EXTENDED) {
        gtk_window_set_type_hint(GTK_WINDOW(gtk_widget), GDK_WINDOW_TYPE_HINT_UTILITY);
    }

    if (owner) {
        owner->add_child(this);
        if (on_top_inherited()) {
            gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), true);
        }
    }

    set_title("");
    GdkVisual* visual = find_best_visual();
    gtk_widget_set_visual(gtk_widget, visual);

    gtk_widget_set_events(gtk_widget, GDK_FILTERED_EVENTS_MASK);
    gtk_widget_set_app_paintable(gtk_widget, true);

    glass_configure_window_transparency(gtk_widget, frame_type == TRANSPARENT);

    gtk_window_set_decorated(GTK_WINDOW(gtk_widget), frame_type == TITLED);

    // Those will fire only if the value changes
    window_location.setOnChange([this](const OptionalAxisPoint& point) {
        notify_window_move();
    });

    view_position.setOnChange([this](const Point& point) {
        notify_view_move();
    });

    window_size.setOnChange([this](const Size& size) {
        notify_window_resize();
    });

    view_size.setOnChange([this](const Size& size) {
        notify_view_resize();
        update_window_constraints();
        // Will fire window size notification on window_size change
        update_window_size();
    });

    window_extents.setOnChange([this](const Rectangle& rect) {
        update_window_constraints();
        update_window_size();
    });

    load_cached_extents();
}

GdkVisual* WindowContext::find_best_visual() {
    // This comes from prism-es2
    static glong xvisualID = mainEnv->GetStaticLongField(jApplicationCls, jApplicationVisualID);
    static GdkVisual *prismVisual = (xvisualID != 0)
                ? gdk_x11_screen_lookup_visual(gdk_screen_get_default(), xvisualID)
                : nullptr;

    if (frame_type == TRANSPARENT && !gdk_visual_is_rgba(prismVisual)) {
        GdkVisual *rgbaVisual = gdk_screen_get_rgba_visual(gdk_screen_get_default());
        if (rgbaVisual) {
            return rgbaVisual;
        }

        fprintf(stderr, ALPHA_CHANNEL_ERROR_MSG);
        fflush(stderr);
    }

    if (prismVisual != nullptr) {
        return prismVisual;
    }

    return gdk_screen_get_system_visual(gdk_screen_get_default());
}

GdkWindow* WindowContext::get_gdk_window() {
    if (GDK_IS_WINDOW(gdk_window)) {
        return gdk_window;
    }

    return nullptr;
}

GtkWindow* WindowContext::get_gtk_window() {
    return GTK_WINDOW(gtk_widget);
}

void WindowContext::process_realize() {
    LOG(LIFECYCLE, log_id, "process_realize\n");

    gdk_window = gtk_widget_get_window(gtk_widget);

    if (frame_type == TITLED) {
        request_frame_extents();
    }

    if (frame_type != TRANSPARENT) {
        gdk_window_set_background_rgba(gdk_window, &background_color);
    }

    if (log_id.empty()) {
        log_id = std::to_string(GDK_WINDOW_XID(gdk_window));
    }

    g_object_set_data_full(G_OBJECT(gdk_window), GDK_WINDOW_DATA_CONTEXT, this, nullptr);
    gdk_window_register_dnd(gdk_window);

    if (initial_wmf) {
        gdk_window_set_functions(gdk_window, initial_wmf);
    }
}

// Returns de XWindow ID to be used in prism es2
XID WindowContext::get_native_window()  {
    if (!GDK_IS_WINDOW(gdk_window)) {
        return 0;
    }

    return GDK_WINDOW_XID(gdk_window);
}

bool WindowContext::isEnabled() {
    if (!jwindow) return false;

    return is_enabled;
}

void WindowContext::notify_focus(int focus_type) {
    if (!jwindow) return;

    LOG(FOCUS, log_id, "notify_focus: %s\n",
            focus_type == com_sun_glass_events_WindowEvent_FOCUS_GAINED ? "FOCUS_GAINED" : "FOCUS_LOST");

    mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocus, focus_type);
    CHECK_JNI_EXCEPTION(mainEnv);
}

void WindowContext::notify_focus_disabled() {
    if (!jwindow) return;

    LOG(FOCUS, log_id, "notify_focus_disabled\n");
    mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusDisabled);
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::process_expose(GdkEventExpose* event) {
    GdkRectangle r = event->area;
    notify_repaint({ r.x, r.y, r.width, r.height });
}

void WindowContext::process_map() {
    // We need only first map. Popups are override_redirect windows,
    // so the compositor does not mess with them.
    if (mapped || window_type == POPUP) return;

    LOG(LIFECYCLE, log_id, "process_map -------------------------------------------\n");
    mapped = true;

    // set_resizable may be called before, and will be applied here
    update_window_constraints();

    // The compositor may adjust the window size and position during the process,
    // so checking again increases the chances that the final geometry matches
    // the values currently stored on the Java side.
    ensure_window_geometry();

    // For window state (fullscreen, maximized, iconified) set before showing
    if (initial_state_mask != 0) {
        update_initial_state();
    }
}

void WindowContext::process_focus(GdkEventFocus *event) {
    LOG(FOCUS, log_id, "process_focus: %s\n", event->in ? "GAINED" : "LOST");
    if (!event->in && sm_grab_window == this) {
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
            notify_focus(event->in ? com_sun_glass_events_WindowEvent_FOCUS_GAINED
                                   : com_sun_glass_events_WindowEvent_FOCUS_LOST);
        } else {
            notify_focus_disabled();
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
    LOG(LIFECYCLE, log_id, "process_destroy\n");
    if (owner) {
        owner->remove_child(this);
    }

    if (sm_mouse_drag_window == this) {
        ungrab_mouse_drag_focus();
    }

    if (sm_grab_window == this) {
        ungrab_focus();
    }

    std::set<WindowContext*>::iterator it;
    for (it = children.begin(); it != children.end(); ++it) {
        // FIX JDK-8226537: this method calls set_owner(NULL) which prevents
        // WindowContextTop::process_destroy() to call remove_child() (because children
        // is being iterated here) but also prevents gtk_window_set_transient_for from
        // being called - this causes the crash on gnome.
        gtk_window_set_transient_for((*it)->get_gtk_window(), nullptr);
        (*it)->set_owner(nullptr);
        destroy_and_delete_ctx(*it);
    }
    children.clear();

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyDestroy);
        EXCEPTION_OCCURED(mainEnv);
    }

    if (jview) {
        mainEnv->DeleteGlobalRef(jview);
        jview = nullptr;
    }

    if (jwindow) {
        mainEnv->DeleteGlobalRef(jwindow);
        jwindow = nullptr;
    }

    can_be_deleted = true;
}

void WindowContext::process_delete() {
    LOG(LIFECYCLE, log_id, "process_delete\n");

    if (jwindow && isEnabled()) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyClose);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_repaint() {
    auto [width, height] = view_size.get();
    notify_repaint({ 0, 0, width, height });
}

void WindowContext::notify_repaint(Rectangle rect) {
    if (!jview) return;

    mainEnv->CallVoidMethod(jview, jViewNotifyRepaint, rect.x, rect.y, rect.width, rect.height);
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::process_mouse_button(GdkEventButton* event, bool synthesized) {
    // We only handle single press/release events here.
    if (event->type != GDK_BUTTON_PRESS && event->type != GDK_BUTTON_RELEASE) {
        return;
    }

    bool press = event->type == GDK_BUTTON_PRESS;
    LOG(INPUT, log_id, "mouse_button: %s button=%u at (%d,%d)\n",
              press ? "PRESS" : "RELEASE", event->button, (int)event->x, (int)event->y);
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
                && (glass_gdk_device_get_window_at_position(device, nullptr, nullptr)
                == nullptr)) {
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
                synthesized);
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

    if (isDrag && sm_mouse_drag_window == nullptr) {
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
    LOG(INPUT, log_id, "mouse_scroll: direction=%d at (%d,%d)\n",
              event->direction, (int)event->x, (int)event->y);
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
    LOG(INPUT, log_id, "mouse_cross: %s at (%d,%d)\n",
              enter ? "ENTER" : "EXIT", (int)event->x, (int)event->y);
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
    LOG(INPUT, log_id, "key: %s keyval=0x%x state=0x%x\n",
              press ? "PRESS" : "RELEASE", event->keyval, event->state);
    jint glassKey = get_glass_key(event);
    jint glassModifier = gdk_modifier_mask_to_glass(event->state);
    if (press) {
        glassModifier |= glass_key_to_modifier(glassKey);
    } else {
        glassModifier &= ~glass_key_to_modifier(glassKey);
    }
    jcharArray jChars = nullptr;
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
    gtk_window_set_transient_for(child->get_gtk_window(), GTK_WINDOW(gtk_widget));
    LOG(FOCUS, child->log_id, "set_transient_for: %.30s\n", log_id.c_str());
}

void WindowContext::remove_child(WindowContext* child) {
    children.erase(child);
}

bool WindowContext::is_visible() {
    return gtk_widget_get_visible(gtk_widget);
}

bool WindowContext::set_view(jobject view) {
    LOG(LIFECYCLE, log_id, "set_view: %s\n", view ? "attach" : "detach");
    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                com_sun_glass_events_MouseEvent_EXIT,
                com_sun_glass_events_MouseEvent_BUTTON_NONE,
                0, 0,
                0, 0,
                0,
                JNI_FALSE,
                JNI_FALSE);
        CHECK_JNI_EXCEPTION_RET(mainEnv, false)
        mainEnv->DeleteGlobalRef(jview);
    }

    if (view) {
        jview = mainEnv->NewGlobalRef(view);
    } else {
        jview = nullptr;
    }
    return true;
}

bool WindowContext::grab_mouse_drag_focus() {
    LOG(FOCUS, log_id, "grab_mouse_drag_focus\n");
    if (glass_gdk_mouse_devices_grab_with_cursor(
            gdk_window, gdk_window_get_cursor(gdk_window), false)) {
        sm_mouse_drag_window = this;
        return true;
    }

    return false;
}

void WindowContext::ungrab_mouse_drag_focus() {
    if (!sm_mouse_drag_window) {
        return;
    }

    LOG(FOCUS, log_id, "ungrab_mouse_drag_focus\n");
    sm_mouse_drag_window = nullptr;
    glass_gdk_mouse_devices_ungrab();
    if (sm_grab_window) {
        sm_grab_window->grab_focus();
    }
}

bool WindowContext::grab_focus() {
    LOG(FOCUS, log_id, "grab_focus\n");
    if (sm_mouse_drag_window
            || glass_gdk_mouse_devices_grab(gdk_window)) {
        sm_grab_window = this;
        return true;
    }

    return false;
}

void WindowContext::ungrab_focus() {
    LOG(FOCUS, log_id, "ungrab_focus\n");
    if (!sm_mouse_drag_window) {
        glass_gdk_mouse_devices_ungrab();
    }

    sm_grab_window = nullptr;

    if (jwindow) {
        mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::ungrab_if_grabbed() {
    if (!sm_grab_window) return;

    sm_grab_window->ungrab_focus();
}

void WindowContext::set_cursor(GdkCursor* cursor) {
    if (!is_in_drag()) {
        if (sm_mouse_drag_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    sm_mouse_drag_window->get_gdk_window(), cursor, false);
        } else if (sm_grab_window) {
            glass_gdk_mouse_devices_grab_with_cursor(
                    sm_grab_window->get_gdk_window(), cursor, true);
        }
    }

    if (gdk_cursor) {
        g_object_unref(gdk_cursor);
    }

    gdk_cursor = cursor;

    if (gdk_cursor_override == nullptr) {
        gdk_window_set_cursor(gdk_window, cursor);
    }
}

void WindowContext::set_cursor_override(GdkCursor* cursor) {
    if (gdk_cursor_override == cursor) {
        return;
    }

    gdk_cursor_override = cursor;

    if (cursor != nullptr) {
        gdk_window_set_cursor(gdk_window, cursor);
    } else {
        gdk_window_set_cursor(gdk_window, gdk_cursor);
    }
}

void WindowContext::set_background(float r, float g, float b) {
    background_color = {r, g, b, 1.0};

    if (mapped && GDK_IS_WINDOW(gdk_window)) {
        gdk_window_set_background_rgba(gdk_window, &background_color);
    }
}

GdkAtom WindowContext::get_net_frame_extents_atom() {
    static GdkAtom atom = nullptr;
    if (atom == nullptr) {
        atom = gdk_atom_intern_static_string("_NET_FRAME_EXTENTS");
    }
    return atom;
}

void WindowContext::request_frame_extents() {
    LOG(SIZE, log_id, "request_frame_extents\n");
    Display *display = GDK_DISPLAY_XDISPLAY(gdk_window_get_display(gdk_window));
    static Atom rfeAtom = XInternAtom(display, "_NET_REQUEST_FRAME_EXTENTS", False);

    if (rfeAtom != None) {
        XClientMessageEvent clientMessage = {};

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
    // Java side does set iconified = false when maximized
    if ((initial_state_mask & GDK_WINDOW_STATE_ICONIFIED) != 0
        && (initial_state_mask & GDK_WINDOW_STATE_MAXIMIZED) == 0) {
        LOG(STATE, log_id, "update_initial_state: iconify\n");
        iconify(true);
    }

    // On gnome (probably others) a window can be both maximized and fullscreen.
    // When fullscreen mode is exited it remains maximized.
    if (initial_state_mask & GDK_WINDOW_STATE_MAXIMIZED) {
        LOG(STATE, log_id, "update_initial_state: maximize\n");
        maximize(true);
    }

    if (initial_state_mask & GDK_WINDOW_STATE_FULLSCREEN) {
        LOG(STATE, log_id, "update_initial_state: fullscreen\n");
        enter_fullscreen();
    }

    initial_state_mask = 0;
}

void WindowContext::update_frame_extents() {
    if (frame_type != TITLED) return;

    int top, left, bottom, right;

    if (!get_frame_extents_property(&top, &left, &bottom, &right)) return;
    if (top <= 0 && right <= 0 && bottom <= 0 && left <= 0) return;

    Rectangle old_extents = window_extents.get();
    Rectangle new_extents = { left, top, (left + right), (top + bottom) };
    bool changed = old_extents != new_extents;

    LOG(SIZE, log_id, "update_frame_extents: changed=%d, left=%d, top=%d, right=%d, bottom=%d\n",
            changed, left, top, right, bottom);

    if (!changed) return;

    set_cached_extents(new_extents);

    if (!is_floating()) {
        // Delay for then window is restored from fullscreen or maximized
        needs_to_update_frame_extents = true;
        LOG(SIZE, log_id, "update_frame_extents: deferred (not floating)\n");
        return;
    }

    auto [newW, newH] = view_size.get();

    // Here the user might change the desktop theme which
    // may change decoration sizes.
    if (width_type == BOUNDSTYPE_WINDOW) {
        newW = newW + old_extents.width - new_extents.width;
    }

    if (height_type == BOUNDSTYPE_WINDOW) {
        newH = newH + old_extents.height - new_extents.height;
    }

    LOG(SIZE, log_id, "update_frame_extents: new view size w=%d, h=%d\n", newW, newH);

    auto loc = window_location.get();

    bool xSet = loc.x.has_value();
    bool ySet = loc.y.has_value();

    int x = xSet ? loc.x.value() : 0;
    int y = ySet ? loc.y.value() : 0;

    // The difference that needs to be adjusted
    int dx = new_extents.width - old_extents.width;
    int dy = new_extents.height - old_extents.height;

    // Gravity x, y are used in centerOnScreen(). Here it's used to adjust the position
    // accounting decorations, so calculate the difference
    if (xSet && gravity_x > 0 && dx != 0) {
        x -= gravity_x * static_cast<float>(dx);
        if (x < 0) x = 0;
        LOG(POSITION, log_id, "update_frame_extents: gravity_x=%.2f, dx=%d, adjusted x=%d\n", gravity_x, dx, x);
    }

    if (ySet && gravity_y > 0 && dy != 0) {
        y -= gravity_y * static_cast<float>(dy);
        if (y < 0) y = 0;
        LOG(POSITION, log_id, "update_frame_extents: gravity_y=%.2f, dy=%d, adjusted y=%d\n", gravity_y, dy, y);
    }

    // Reset the values since it only applies once
    gravity_x = 0;
    gravity_y = 0;

    // When window_extents changes, it will fire the observable and update window size.
    window_extents.set(new_extents);
    move_resize(x, y, xSet, ySet, newW, newH);
}

bool WindowContext::get_frame_extents_property(int *top, int *left, int *bottom, int *right) {
    unsigned long *extents;

    if (gdk_property_get(gdk_window,
            get_net_frame_extents_atom(),
            gdk_atom_intern("CARDINAL", false),
            0,
            sizeof (unsigned long) * 4,
            false,
            nullptr,
            nullptr,
            nullptr,
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

void WindowContext::set_cached_extents(Rectangle ex) {
    if (window_type == UTILITY) {
        utility_extents = ex;
    } else {
        normal_extents = ex;
    }
}

void WindowContext::load_cached_extents() {
    if (frame_type != TITLED) return;

    if (window_type == NORMAL && normal_extents.has_value()) {
        window_extents.set(normal_extents.value());
        return;
    }

    if (window_type == UTILITY && utility_extents.has_value()) {
        window_extents.set(utility_extents.value());
    }
}

void WindowContext::process_property_notify(GdkEventProperty *event) {
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

    LOG(STATE, log_id, "process_state: changed_mask=0x%x, new_state=0x%x\n",
            event->changed_mask, event->new_window_state);

    if (event->changed_mask & GDK_WINDOW_STATE_ABOVE) {
        notify_on_top(event->new_window_state & GDK_WINDOW_STATE_ABOVE);

        if (event->changed_mask == GDK_WINDOW_STATE_ABOVE) return;
    }

    if (event->changed_mask & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_ICONIFIED)
        && (event->new_window_state & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_ICONIFIED)) == 0) {
        LOG(STATE, log_id, "process_state: RESTORE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_RESTORE);
    } else if (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) {
        LOG(STATE, log_id, "process_state: MINIMIZE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_MINIMIZE);
    } else if (event->new_window_state & (GDK_WINDOW_STATE_MAXIMIZED)) {
        LOG(STATE, log_id, "process_state: MAXIMIZE\n");
        notify_window_resize(com_sun_glass_events_WindowEvent_MAXIMIZE);
    }

    if (event->changed_mask & GDK_WINDOW_STATE_ICONIFIED
        && (event->new_window_state & GDK_WINDOW_STATE_ICONIFIED) == 0) {
        remove_wmf(GDK_FUNC_MINIMIZE);
    }

    // If only iconified, no further processing
    if (event->changed_mask == GDK_WINDOW_STATE_ICONIFIED) return;

    if (event->changed_mask & GDK_WINDOW_STATE_MAXIMIZED
        && (event->new_window_state & GDK_WINDOW_STATE_MAXIMIZED) == 0) {
        remove_wmf(GDK_FUNC_MAXIMIZE);
    }

    if (jview && event->changed_mask & GDK_WINDOW_STATE_FULLSCREEN) {
        notify_fullscreen(event->new_window_state & GDK_WINDOW_STATE_FULLSCREEN);
    }

    // Since FullScreen (or custom modes of maximized) can undecorate the
    // window, request view position change
    if (frame_type == TITLED) {
        notify_view_move();
    }

    bool restored = event->changed_mask & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_FULLSCREEN)
                    && (event->new_window_state & (GDK_WINDOW_STATE_MAXIMIZED | GDK_WINDOW_STATE_FULLSCREEN)) == 0;

    if (restored && needs_to_update_frame_extents) {
        LOG(STATE, log_id, "process_state: restored, updating frame extents\n");
        needs_to_update_frame_extents = false;
        // Will fire the observable and update window size
        load_cached_extents();
    }
}

void WindowContext::notify_fullscreen(bool enter) {
    LOG(STATE, log_id, "notify_fullscreen: %s\n", enter ? "ENTER" : "EXIT");

    if (jview) {
        mainEnv->CallVoidMethod(jview, jViewNotifyView, enter
                                            ? com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER
                                            : com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

void WindowContext::notify_window_resize(int state) {
    if (!jwindow || !window_size.was_assigned()) return;

    Size size = window_size.get();

    if (!size.is_valid()) return;

    LOG(SIZE, log_id, "notify_window_resize: state=%d, w=%d, h=%d\n", state, size.width, size.height);
    mainEnv->CallVoidMethod(jwindow, jWindowNotifyResize, state, size.width, size.height);
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::notify_window_move() {
    if (!jwindow || !window_location.was_assigned()) return;

    auto loc = window_location.get();

    if (!loc.has_values()) {
        LOG(POSITION, log_id, "notify_window_move: location was not set completely\n");
        return;
    }

    LOG(POSITION, log_id, "notify_window_move: x=%d, y=%d\n", loc.x.value(), loc.y.value());
    mainEnv->CallVoidMethod(jwindow, jWindowNotifyMove, loc.x.value(), loc.y.value());
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::notify_view_resize() {
    if (!jview || !view_size.was_assigned()) return;

    Size size = view_size.get();

    if (!size.is_valid()) {
        LOG(SIZE, log_id, "notify_view_resize: view size was not set completely: %d, %d\n",
            size.width, size.height);
        return;
    }

    LOG(SIZE, log_id, "notify_view_resize: w=%d, h=%d\n", size.width, size.height);
    mainEnv->CallVoidMethod(jview, jViewNotifyResize, size.width, size.height);
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::notify_window_resize() {
    if (is_iconified()) {
        notify_window_resize(com_sun_glass_events_WindowEvent_MINIMIZE);
    } else if (is_maximized()) {
        notify_window_resize(com_sun_glass_events_WindowEvent_MAXIMIZE);
    } else {
        notify_window_resize(com_sun_glass_events_WindowEvent_RESIZE);
    }
}

void WindowContext::notify_view_move() {
    if (!jview || !view_position.was_assigned()) return;

    LOG(POSITION, log_id, "notify_view_move  (value set previously)\n");
    mainEnv->CallVoidMethod(jview, jViewNotifyView, com_sun_glass_events_ViewEvent_MOVE);
    CHECK_JNI_EXCEPTION(mainEnv)
}

void WindowContext::process_configure(GdkEventConfigure *event) {
    LOG(SIZE, log_id, "process_configure (size): send_event=%d, w=%d, h=%d\n",
            event->send_event, event->width, event->height);

    LOG(POSITION, log_id, "process_configure (position): send_event=%d, x=%d, y=%d\n",
        event->send_event, event->x, event->y);

    // Synthetized events will mess the flow with unwanted values
    if (event->send_event) {
        LOG(SIZE, log_id, "process_configure: ignored (send_event=true)\n");
        return;
    }

    int x, y;
    int view_x = 0, view_y = 0;

    if (frame_type == TITLED) {
        // view_x and view_y represent the position of the content relative to the left corner of the window,
        // taking into account window decorations (such as title bars and borders) applied by the window manager
        // and might vary by window state. For example, FullScreen will have no decorations, so view_x and
        // view_y will be 0. Maximized state may or may not have decorations depending on the desktop environment.
        int root_x, root_y;
        gdk_window_get_root_origin(gdk_window, &root_x, &root_y);

        view_x = event->x - root_x;
        view_y = event->y - root_y;

        x = root_x;
        y = root_y;

        LOG(POSITION, log_id, "process_configure: view_position x=%d, y=%d\n", view_x, view_y);
        view_position.set({view_x, view_y});
    } else {
        view_position.set({0, 0});
        x = event->x;
        y = event->y;
    }

    int ww = event->width;
    int wh = event->height;

    Rectangle extents = window_extents.get();

    if (view_x > 0) {
        ww += extents.width;
    }

    if (view_y > 0) {
        wh += extents.height;
    }

    // While the window is not yet mapped, the compositor may continue reporting
    // the initial size set by set_bounds, even if subsequent calls provide updated values.
    // If this stale geometry is propagated to Java, the window will not reflect
    // the requested size and position.
    if (mapped) {
        window_location.set({x, y});
        view_size.set({event->width, event->height});
        window_size.set({ww, wh});
    }

    glong to_screen = getScreenPtrForLocation(event->x, event->y);
    if (to_screen != -1) {
        if (to_screen != screen) {
            if (jwindow) {
                jobject jScreen = createJavaScreen(mainEnv, to_screen);
                mainEnv->CallVoidMethod(jwindow, jWindowNotifyMoveToAnotherScreen, jScreen);
                CHECK_JNI_EXCEPTION(mainEnv)
            }
            screen = to_screen;
        }
    }
}

void WindowContext::update_window_constraints() {
    if (window_type == POPUP) return;

    // Not ready to re-apply the constraints
    if (!is_floating() || !is_state_floating((GdkWindowState) initial_state_mask)) {
        LOG(SIZE, log_id, "update_window_constraints: skipped (not floating)\n");
        return;
    }

    GdkGeometry hints;

    if (is_resizable() && isEnabled()) {
        LOG(SIZE, log_id, "update_window_constraints: resizable and enabled\n");
        Size min = minimum_size.max(sys_min_size);

        Rectangle extents = window_extents.get();

        hints.min_width = std::clamp(min.width - extents.width, 1, MAX_WINDOW_SIZE);
        hints.min_height = std::clamp(min.height - extents.height, 1, MAX_WINDOW_SIZE);

        hints.max_width = std::clamp(maximum_size.width - extents.width, 1, MAX_WINDOW_SIZE);
        hints.max_height = std::clamp(maximum_size.height - extents.height, 1, MAX_WINDOW_SIZE);
    } else {
        LOG(SIZE, log_id, "update_window_constraints: NOT resizable or disabled\n");
        Size size = view_size.get();
        int w = std::clamp(size.width, 1, MAX_WINDOW_SIZE);
        int h = std::clamp(size.height, 1, MAX_WINDOW_SIZE);

        hints.min_width = w;
        hints.min_height = h;
        hints.max_width = w;
        hints.max_height = h;
    }

    LOG(SIZE, log_id, "update_window_constraints: min_w=%d, min_h=%d, max_w=%d, max_h=%d\n",
            hints.min_width, hints.min_height, hints.max_width, hints.max_height);

    gtk_window_set_geometry_hints(GTK_WINDOW(gtk_widget), nullptr, &hints,
                            (GdkWindowHints) (GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE));
}

void WindowContext::set_resizable(bool res) {
    LOG(SIZE, log_id, "set_resizable: %s\n", res ? "true" : "false");
    resizable = res;
    update_window_constraints();
}

bool WindowContext::is_resizable() {
    return resizable;
}

bool WindowContext::is_maximized() {
    if (!mapped) {
        return initial_state_mask & GDK_WINDOW_STATE_MAXIMIZED;
    }

    return GDK_IS_WINDOW(gdk_window)
        && gdk_window_get_state(gdk_window) & GDK_WINDOW_STATE_MAXIMIZED;
}

bool WindowContext::is_fullscreen() {
    if (!mapped) {
        return initial_state_mask & GDK_WINDOW_STATE_FULLSCREEN;
    }

    return GDK_IS_WINDOW(gdk_window)
        && gdk_window_get_state(gdk_window) & GDK_WINDOW_STATE_FULLSCREEN;
}

bool WindowContext::is_iconified() {
    if (!mapped) {
        return initial_state_mask & GDK_WINDOW_STATE_ICONIFIED;
    }

    return GDK_IS_WINDOW(gdk_window)
        && gdk_window_get_state(gdk_window) & GDK_WINDOW_STATE_ICONIFIED;
}

bool WindowContext::is_floating() {
    return GDK_IS_WINDOW(gdk_window)
            && is_state_floating(gdk_window_get_state(gdk_window));
}

void WindowContext::set_visible(bool visible) {
    LOG(LIFECYCLE, log_id, "set_visible: %s\n", visible ? "true" : "false");

    if (visible) {
        gtk_widget_show(gtk_widget);

        if (jwindow && isEnabled()) {
            notify_focus(com_sun_glass_events_WindowEvent_FOCUS_GAINED);
        }
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

void WindowContext::set_bounds(int x, int y, bool xSet, bool ySet, int w, int h, int cw, int ch,
                               float gravity_x, float gravity_y) {

    if (xSet || ySet || gravity_x > 0 || gravity_y > 0) {
        LOG(POSITION, log_id, "====> set_bounds position: x=%d, y=%d, xSet=%d, ySet=%d, gx=%.2f, gy=%.2f\n",
                x, y, xSet, ySet, gravity_x, gravity_y);
    }

    if (w > 0 || h > 0 || cw > 0 || ch > 0) {
        LOG(SIZE, log_id, "====> set_bounds size: w=%d, h=%d, cw=%d, ch=%d\n", w, h, cw, ch);
    }

    // newW / newH are view/content sizes
    // -1 means not set
    int newW = -1;
    int newH = -1;

    this->gravity_x = gravity_x;
    this->gravity_y = gravity_y;

    if (w > 0) {
        width_type = BOUNDSTYPE_WINDOW;
        newW = (w - window_extents.get().width);
    } else if (cw > 0) {
        // once set to window, stick with it
        if (width_type == BOUNDSTYPE_UNKNOWN) {
            width_type = BOUNDSTYPE_VIEW;
        }
        newW = cw;
    }

    if (h > 0) {
        height_type = BOUNDSTYPE_WINDOW;
        newH = (h - window_extents.get().height);
    } else if (ch > 0) {
        // once set to window, stick with it
        if (height_type == BOUNDSTYPE_UNKNOWN) {
            height_type = BOUNDSTYPE_VIEW;
        }
        newH = ch;
    }

    // Ignore when maximized / fullscreen (not floating)
    // Report back to java to correct the values
    if (mapped && !is_floating()) {
        notify_window_resize();
        notify_view_resize();
        notify_window_move();
        return;
    }

    move_resize(x, y, xSet, ySet, newW, newH);
}

void WindowContext::iconify(bool state) {
    if (state) {
        add_wmf(GDK_FUNC_MINIMIZE);
        gtk_window_iconify(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_present(GTK_WINDOW(gtk_widget));
    }
}

void WindowContext::maximize(bool state) {
    if (state) {
        add_wmf(GDK_FUNC_MAXIMIZE);
        gtk_window_maximize(GTK_WINDOW(gtk_widget));
    } else {
        gtk_window_unmaximize(GTK_WINDOW(gtk_widget));
    }
}

void WindowContext::set_minimized(bool state) {
    LOG(STATE, log_id, "set_minimized: %s\n", state ? "true" : "false");
    if (mapped) {
        iconify(state);
    } else {
        initial_state_mask = state
            ? initial_state_mask | GDK_WINDOW_STATE_ICONIFIED
            : initial_state_mask & ~GDK_WINDOW_STATE_ICONIFIED;
    }
}

void WindowContext::set_maximized(bool state) {
    LOG(STATE, log_id, "set_maximized: %s\n", state ? "true" : "false");
    if (mapped) {
        maximize(state);
    } else {
        initial_state_mask = state
            ? initial_state_mask | GDK_WINDOW_STATE_MAXIMIZED
            : initial_state_mask & ~GDK_WINDOW_STATE_MAXIMIZED;
    }
}

void WindowContext::enter_fullscreen() {
    LOG(STATE, log_id, "enter_fullscreen\n");
    if (mapped) {
        gtk_window_fullscreen(GTK_WINDOW(gtk_widget));
    } else {
        initial_state_mask |= GDK_WINDOW_STATE_FULLSCREEN;
    }
}

void WindowContext::exit_fullscreen() {
    LOG(STATE, log_id, "exit_fullscreen\n");
    if (mapped) {
        gtk_window_unfullscreen(GTK_WINDOW(gtk_widget));
    } else {
        initial_state_mask &= ~GDK_WINDOW_STATE_FULLSCREEN;
    }
}

void WindowContext::request_focus() {
    LOG(FOCUS, log_id, "request_focus\n");
    if (GDK_IS_WINDOW(gdk_window) && is_visible()) {
        gdk_window_focus(gdk_window, GDK_CURRENT_TIME);
    }
}

void WindowContext::set_focusable(bool focusable) {
    LOG(FOCUS, log_id, "set_focusable %s\n", focusable ? "true" : "false");
    gtk_window_set_accept_focus(GTK_WINDOW(gtk_widget), focusable);
}

void WindowContext::set_title(const char* title) {
    gtk_window_set_title(GTK_WINDOW(gtk_widget), title);
    log_id = title;
}

void WindowContext::set_alpha(double alpha) {
    gtk_widget_set_opacity(gtk_widget, alpha);
}

void WindowContext::set_enabled(bool enabled) {
    LOG(FOCUS, log_id, "set_enabled: %s\n", enabled ? "true" : "false");
    is_enabled = enabled;

    // When not enabled, disable minimize
    if (frame_type == TITLED && (initial_wmf & GDK_FUNC_MINIMIZE)) {
        if (!enabled) {
            current_wmf = static_cast<GdkWMFunction>(
                static_cast<int>(current_wmf) & ~static_cast<int>(GDK_FUNC_MINIMIZE));
        } else {
            current_wmf = static_cast<GdkWMFunction>(
                static_cast<int>(current_wmf) | static_cast<int>(GDK_FUNC_MINIMIZE));
        }

        if (GDK_IS_WINDOW(gdk_window)) {
            gdk_window_set_functions(gdk_window, current_wmf);
        }
    }

    // This will make the window unresizable
    update_window_constraints();
}

void WindowContext::set_minimum_size(int w, int h) {
    LOG(SIZE, log_id, "set_minimum_size: w=%d, h=%d\n", w, h);
    minimum_size = Size {w, h};
    update_window_constraints();
}

void WindowContext::set_system_minimum_size(int w, int h) {
    LOG(SIZE, log_id, "set_system_minimum_size: w=%d, h=%d\n", w, h);
    sys_min_size = Size {w, h};
    update_window_constraints();
}

void WindowContext::set_maximum_size(int w, int h) {
    LOG(SIZE, log_id, "set_maximum_size: w=%d, h=%d\n", w, h);
    int maxw = (w == -1) ? G_MAXINT : w;
    int maxh = (h == -1) ? G_MAXINT : h;

    maximum_size = Size {maxw, maxh};
    update_window_constraints();
}

void WindowContext::set_icon(GdkPixbuf* icon) {
    if (icon == nullptr || !GDK_IS_PIXBUF(icon)) return;

    gtk_window_set_icon(GTK_WINDOW(gtk_widget), icon);
}

void WindowContext::to_front() {
    LOG(STATE, log_id, "to_front\n");
    if (GDK_IS_WINDOW(gdk_window)) {
        gdk_window_raise(gdk_window);
    }
}

void WindowContext::to_back() {
    LOG(STATE, log_id, "to_back\n");

    if (GDK_IS_WINDOW(gdk_window)) {
        gdk_window_lower(gdk_window);
    }
}

void WindowContext::set_modal(bool modal, WindowContext* parent) {
    if (modal) {
        if (parent) {
            gtk_window_set_transient_for(GTK_WINDOW(gtk_widget), parent->get_gtk_window());
        }
    }
    gtk_window_set_modal(GTK_WINDOW(gtk_widget), modal ? TRUE : FALSE);
}

void WindowContext::update_ontop_tree(bool on_top) {
    bool effective_on_top = on_top || this->on_top;
    gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), effective_on_top);
    for (std::set<WindowContext*>::iterator it = children.begin(); it != children.end(); ++it) {
        (*it)->update_ontop_tree(effective_on_top);
    }
}

bool WindowContext::on_top_inherited() {
    WindowContext* o = owner;
    while (o) {
        WindowContext* topO = o;
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
        WindowContext* topO = owner;
        return (topO && topO->effective_on_top()) || on_top;
    }
    return on_top;
}

void WindowContext::update_window_size() {
    Size size = view_size.get();

    if (!size.is_valid()) {
        LOG(SIZE, log_id, "update_window_size: invalid size %d, %d\n", size.width, size.height);
        return;
    }

    if (frame_type == TITLED) {
        window_size.set({size.width + window_extents.get().width,
                                size.height + window_extents.get().height});
    } else {
        // If no title/decoration the size will be the same
        window_size.set(size);
    }
}

// -1 on width or height means not set
void WindowContext::move_resize(int x, int y, bool xSet, bool ySet, int width, int height) {
    bool wSet = width > 0;
    bool hSet = height > 0;

    if (!wSet && !hSet && !xSet && !ySet) {
        return;
    }

    auto loc = window_location.get();

    int newX = 0, newY = 0;

    bool loc_set = false;
    if ((xSet || loc.x.has_value()) && (ySet || loc.y.has_value())) {
        newX = xSet ? x : loc.x.value();
        newY = ySet ? y : loc.y.value();
        loc_set = true;
    }

    if (loc_set) {
        if (!mapped) {
            LOG(POSITION, log_id, "move_resize: not mapped\n");
            // See the comment on process_configure about the compositor changing the values until mapped.
            window_location.set({newX, newY});
        }

        LOG(POSITION, log_id, "--> move_resize: gtk_window_move: x=%d, y=%d\n", newX, newY);
        gtk_window_move(GTK_WINDOW(gtk_widget), newX, newY);
    }

    Size size = view_size.get();

    // May still be -1
    int newW = wSet ? width : size.width;
    int newH = hSet ? height : size.height;

    Rectangle extents = window_extents.get();

    // Holds view/content size
    int boundsW = newW, boundsH = newH;

    Size max_size = maximum_size;
    Size min_size = minimum_size.max(sys_min_size);

    // Windows that are undecorated or transparent may not respect
    // minimum or maximum size constraints

    if (wSet) {
        if (min_size.width > 0 && newW < min_size.width) {
            boundsW = min_size.width - extents.width;
        }

        if (max_size.width > 0 && newW > max_size.width) {
            boundsW = max_size.width - extents.width;
        }

        boundsW = std::clamp(boundsW, 1, MAX_WINDOW_SIZE);
    }

    if (hSet) {
        if (min_size.height > 0 && newH < min_size.height) {
            boundsH = min_size.height - extents.height;
        }

        if (max_size.height > 0 && newH > max_size.height) {
            boundsH = max_size.height - extents.height;
        }

        boundsH = std::clamp(boundsH, 1, MAX_WINDOW_SIZE);
    }

    if (!Size {boundsW, boundsH}.is_valid()) {
        LOG(SIZE, log_id, "move_resize: invalid size w=%d, h=%d\n", boundsW, boundsH);
        return;
    }

    // Need to force notify back to java, because it probably has wrong sizes.
    // This is triggered, for example, when size is set bellow mininum.
    if ((newW != boundsW && size.width == boundsW) || (newH != boundsH && size.height == boundsH)) {
        LOG(SIZE, log_id, "move_resize: invalidate\n");
        view_size.invalidate();
        window_size.invalidate();
    }

    if (!mapped) {
        // See the comment on process_configure about the compositor changing the values until mapped.
        LOG(SIZE, log_id, "move_resize: not mapped\n");
        view_size.set({boundsW, boundsH});
    }

    // When the window is not resizable, allow programmatic resizing.
    if (mapped && !is_resizable()) {
        LOG(SIZE, log_id, "move_resize: not resizable: %d, %d\n", boundsW, boundsH);
        view_size.set({boundsW, boundsH});
    }

    if (mapped) {
        LOG(SIZE, log_id, "--> move_resize: gtk_window_resize: w=%d, h=%d\n", boundsW, boundsH);
        gtk_window_resize(GTK_WINDOW(gtk_widget), boundsW, boundsH);
    } else {
        LOG(SIZE, log_id, "--> move_resize: gtk_window_set_default_size(GTK_WINDOW: w=%d, h=%d\n", boundsW, boundsH);
        gtk_window_set_default_size(GTK_WINDOW(gtk_widget), boundsW, boundsH);
    }
}

void WindowContext::ensure_window_geometry() {
    auto loc = window_location.get();
    auto [w, h] = view_size.get();

    bool xSet = loc.x.has_value();
    bool ySet = loc.y.has_value();

    // If never assigned, use defaults
    if (w <= 0) {
        w = DEFAULT_WIDTH;
    }

    if (h <= 0) {
        h = DEFAULT_HEIGHT;
    }

    move_resize(loc.x.value_or(0), loc.y.value_or(0), xSet, ySet, w, h);
}

void WindowContext::add_wmf(GdkWMFunction wmf) {
    if (initial_wmf & wmf) return;

    current_wmf = static_cast<GdkWMFunction>(static_cast<int>(current_wmf) | static_cast<int>(wmf));

    if (GDK_IS_WINDOW(gdk_window)) {
        gdk_window_set_functions(gdk_window, current_wmf);
    }
}

void WindowContext::remove_wmf(GdkWMFunction wmf) {
    if (initial_wmf & wmf) return;

     current_wmf = static_cast<GdkWMFunction>(static_cast<int>(current_wmf) & ~static_cast<int>(wmf));

    if (GDK_IS_WINDOW(gdk_window)) {
        gdk_window_set_functions(gdk_window, current_wmf);
    }
}

void WindowContext::notify_on_top(bool top) {
    // Do not report effective (i.e. native) values to the FX, only if the user sets it manually
    if (top != effective_on_top() && jwindow) {
        if (on_top_inherited() && !top) {
            // Disallow user's "on top" handling on windows that inherited the property
            gtk_window_set_keep_above(GTK_WINDOW(gtk_widget), true);
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

    if (on_top_inherited()) return;

    update_ontop_tree(on_top);
}

void WindowContext::set_owner(WindowContext * owner_ctx) {
    owner = owner_ctx;
}

void WindowContext::update_view_size() {
    LOG(SIZE, log_id, "update_view_size\n");
    notify_view_resize();
}

void WindowContext::show_system_menu(int x, int y) {
    GdkDisplay* display = gdk_display_get_default();
    if (!display) {
        return;
    }

    GdkSeat* seat = gdk_display_get_default_seat(display);
    GdkDevice* device = gdk_seat_get_pointer(seat);
    if (!device) {
        return;
    }

    gint rx = 0, ry = 0;
    gdk_window_get_root_coords(gdk_window, x, y, &rx, &ry);

    GdkEvent* event = (GdkEvent*)gdk_event_new(GDK_BUTTON_PRESS);
    GdkEventButton* buttonEvent = (GdkEventButton*)event;
    buttonEvent->x_root = rx;
    buttonEvent->y_root = ry;
    buttonEvent->window = (GdkWindow*)g_object_ref(gdk_window);
    buttonEvent->device = (GdkDevice*)g_object_ref(device);

    gdk_window_show_window_menu(gdk_window, event);
    gdk_event_free(event);
}

Size WindowContext::get_view_size() {
    return view_size.get();
}

Point WindowContext::get_view_position() {
    return view_position.get();
}

WindowContext::~WindowContext() {
    LOG(LIFECYCLE, log_id, "~WindowContext\n");
    disableIME();

    if (gdk_cursor) {
        g_object_unref(gdk_cursor);
    }

    gtk_widget_destroy(gtk_widget);
}

WindowContextExtended::WindowContextExtended(jobject jwin,
                                             WindowContext* owner,
                                             long screen,
                                             GdkWMFunction wmf)
                        : WindowContext(jwin, owner, screen, EXTENDED, NORMAL, wmf) {
}

/*
 * Handles mouse button events of EXTENDED windows and adds the window behaviors for non-client
 * regions that are usually provided by the window manager. Note that a full-screen window has
 * no non-client regions.
 */
void WindowContextExtended::process_mouse_button(GdkEventButton* event, bool synthesized) {
    LOG(INPUT, log_id, "ext_mouse_button: type=%d button=%u at (%d,%d)\n",
              event->type, event->button, (int)event->x, (int)event->y);
    // Non-EXTENDED or full-screen windows don't have additional behaviors, so we delegate
    // directly to the base implementation.
    if (is_fullscreen() || jwindow == nullptr) {
        WindowContext::process_mouse_button(event);
        return;
    }

    // Double-clicking on the drag area maximizes the window (or restores its size).
    if (is_resizable() && event->type == GDK_2BUTTON_PRESS) {
        jint hitTestResult = mainEnv->CallIntMethod(
            jwindow, jGtkWindowNonClientHitTest, (jint)event->x, (jint)event->y);
        CHECK_JNI_EXCEPTION(mainEnv)

        if (hitTestResult == com_sun_glass_ui_gtk_GtkWindow_HT_CAPTION) {
            set_maximized(!is_maximized());
        }

        // We don't process the GDK_2BUTTON_PRESS event in the base implementation.
        return;
    }

    if (event->button == 1 && event->type == GDK_BUTTON_PRESS) {
        jint hitTestResult = mainEnv->CallIntMethod(
            jwindow, jGtkWindowNonClientHitTest, (jint)event->x, (jint)event->y);
        CHECK_JNI_EXCEPTION(mainEnv)

        GdkWindowEdge edge;
        bool shouldStartResizeDrag =
            is_resizable() &&
            !is_maximized() &&
            get_window_edge(event->x, event->y, &edge) &&
            (edge != GDK_WINDOW_EDGE_NORTH || hitTestResult != com_sun_glass_ui_gtk_GtkWindow_HT_CLIENT);

        // Clicking on a window edge starts a move-resize operation.
        if (shouldStartResizeDrag) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);

            gint rx = 0, ry = 0;
            gdk_window_get_root_coords(get_gdk_window(), event->x, event->y, &rx, &ry);
            gtk_window_begin_resize_drag(get_gtk_window(), edge, 1, rx, ry, event->time);
            return;
        }

        // Clicking on a draggable area starts a move-drag operation.
        if (hitTestResult == com_sun_glass_ui_gtk_GtkWindow_HT_CAPTION) {
            mainEnv->CallVoidMethod(jwindow, jWindowNotifyFocusUngrab);

            gint rx = 0, ry = 0;
            gdk_window_get_root_coords(get_gdk_window(), event->x, event->y, &rx, &ry);
            gtk_window_begin_move_drag(get_gtk_window(), 1, rx, ry, event->time);
            return;
        }
    }

    // Call the base implementation for client area events.
    WindowContext::process_mouse_button(event);
}

void WindowContextExtended::process_mouse_cross(GdkEventCrossing* event) {
    LOG(INPUT, log_id, "ext_mouse_cross: %s at (%d,%d)\n",
              event->type == GDK_ENTER_NOTIFY ? "ENTER" : "EXIT", (int)event->x, (int)event->y);
    // We only send MouseEvent.EXIT if we didn't already send it when the cursor was moved
    // from the client area to the resize border. This is indicated by is_mouse_entered
    // being false at this point.
    if (is_mouse_entered && event->type != GDK_ENTER_NOTIFY) {
        is_mouse_entered = false;
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
            com_sun_glass_events_MouseEvent_EXIT,
            com_sun_glass_events_MouseEvent_BUTTON_NONE,
            (jint) event->x, (jint) event->y,
            (jint) event->x_root, (jint) event->y_root,
            gdk_modifier_mask_to_glass(event->state),
            JNI_FALSE,
            JNI_FALSE);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

/*
 * Handles mouse motion events of EXTENDED windows and changes the cursor when it is on top
 * of the internal resize border. Note that a full-screen window or maximized window has no
 * resize border.
 */
void WindowContextExtended::process_mouse_motion(GdkEventMotion* event) {
    GdkWindowEdge edge;

    // Call the base implementation for client area events.
    if (jview && (!is_floating() || !is_resizable() || !get_window_edge(event->x, event->y, &edge))) {
        // If is_mouse_entered is false at this point, the cursor was on the resize border just a moment
        // ago (which doesn't count as a client area, even though it is on the window). Since the cursor
        // has now entered the client area, we need to send MouseEvent.ENTER to FX.
        if (!is_mouse_entered) {
            is_mouse_entered = true;
            mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
                com_sun_glass_events_MouseEvent_ENTER,
                com_sun_glass_events_MouseEvent_BUTTON_NONE,
                (jint) event->x, (jint) event->y,
                (jint) event->x_root, (jint) event->y_root,
                gdk_modifier_mask_to_glass(event->state),
                JNI_FALSE,
                JNI_FALSE);
            CHECK_JNI_EXCEPTION(mainEnv)
        }

        set_cursor_override(nullptr);
        WindowContext::process_mouse_motion(event);
        return;
    }

    jint hitTestResult = mainEnv->CallIntMethod(
        jwindow, jGtkWindowNonClientHitTest, (jint)event->x, (jint)event->y);
    CHECK_JNI_EXCEPTION(mainEnv)

    if (edge == GDK_WINDOW_EDGE_NORTH && hitTestResult == com_sun_glass_ui_gtk_GtkWindow_HT_CLIENT) {
        set_cursor_override(nullptr);
        WindowContext::process_mouse_motion(event);
        return;
    }

    GdkCursor* cursor = nullptr;

    switch (edge) {
        case GDK_WINDOW_EDGE_NORTH: cursor = EdgeCursors::instance().NORTH; break;
        case GDK_WINDOW_EDGE_NORTH_EAST: cursor = EdgeCursors::instance().NORTH_EAST; break;
        case GDK_WINDOW_EDGE_EAST: cursor = EdgeCursors::instance().EAST; break;
        case GDK_WINDOW_EDGE_SOUTH_EAST: cursor = EdgeCursors::instance().SOUTH_EAST; break;
        case GDK_WINDOW_EDGE_SOUTH: cursor = EdgeCursors::instance().SOUTH; break;
        case GDK_WINDOW_EDGE_SOUTH_WEST: cursor = EdgeCursors::instance().SOUTH_WEST; break;
        case GDK_WINDOW_EDGE_WEST: cursor = EdgeCursors::instance().WEST; break;
        case GDK_WINDOW_EDGE_NORTH_WEST: cursor = EdgeCursors::instance().NORTH_WEST; break;
    }

    set_cursor_override(cursor);

    // If the cursor is not on a resize border, call the base handler.
    if (cursor == nullptr) {
        WindowContext::process_mouse_motion(event);
        return;
    }

    // If the cursor has moved to a resize border, we need to send MouseEvent.EXIT to FX,
    // since from the perspective of FX, resize borders are not a part of client area.
    if (is_mouse_entered && jview) {
        is_mouse_entered = false;
        mainEnv->CallVoidMethod(jview, jViewNotifyMouse,
            com_sun_glass_events_MouseEvent_EXIT,
            com_sun_glass_events_MouseEvent_BUTTON_NONE,
            (jint) event->x, (jint) event->y,
            (jint) event->x_root, (jint) event->y_root,
            gdk_modifier_mask_to_glass(event->state),
            JNI_FALSE,
            JNI_FALSE);
        CHECK_JNI_EXCEPTION(mainEnv)
    }
}

/*
 * Determines the GdkWindowEdge at the specified coordinate; returns true if the coordinate
 * identifies a window edge, false otherwise.
 */
bool WindowContextExtended::get_window_edge(int x, int y, GdkWindowEdge* window_edge) {
    GdkWindowEdge edge;
    Size size = get_view_size();

    if (x <= RESIZE_BORDER_WIDTH) {
        if (y <= 2 * RESIZE_BORDER_WIDTH) edge = GDK_WINDOW_EDGE_NORTH_WEST;
        else if (y >= size.height - 2 * RESIZE_BORDER_WIDTH) edge = GDK_WINDOW_EDGE_SOUTH_WEST;
        else edge = GDK_WINDOW_EDGE_WEST;
    } else if (x >= size.width - RESIZE_BORDER_WIDTH) {
        if (y <= 2 * RESIZE_BORDER_WIDTH) edge = GDK_WINDOW_EDGE_NORTH_EAST;
        else if (y >= size.height - 2 * RESIZE_BORDER_WIDTH) edge = GDK_WINDOW_EDGE_SOUTH_EAST;
        else edge = GDK_WINDOW_EDGE_EAST;
    } else if (y <= RESIZE_BORDER_WIDTH) {
        edge = GDK_WINDOW_EDGE_NORTH;
    } else if (y >= size.height - RESIZE_BORDER_WIDTH) {
        edge = GDK_WINDOW_EDGE_SOUTH;
    } else {
        return false;
    }

    if (window_edge != nullptr) {
        *window_edge = edge;
    }

    return true;
}
