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
#ifndef GLASS_WINDOW_H
#define        GLASS_WINDOW_H

#include <gtk/gtk.h>
#include <X11/Xlib.h>

#include <jni.h>
#include <set>
#include <vector>

#include "glass_view.h"
#include "glass_general.h"

enum WindowFrameType {
    TITLED,
    UNTITLED,
    TRANSPARENT
};

enum WindowType {
    NORMAL,
    UTILITY,
    POPUP
};

enum request_type {
    REQUEST_NONE,
    REQUEST_RESIZABLE,
    REQUEST_NOT_RESIZABLE
};

static const guint MOUSE_BUTTONS_MASK = (guint)(GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK);

struct BgColor {
    BgColor() : red(0), green(0), blue(0), is_set(FALSE) {}

    float red;
    float green;
    float blue;
    bool is_set;
};

struct WindowGeometry {
    WindowGeometry() : current_x(0),
                       current_y(0),
                       current_w(0),
                       current_h(0),
                       current_cw(0),
                       current_ch(0),
                       last_cw(0),
                       last_ch(0),
                       adjust_w(0),
                       adjust_h(0),
                       view_x(0),
                       view_y(0),
                       frame_extents_received(false),
                       gravity_x(1.00),
                       gravity_y(1.00),
                       enabled(true),
                       resizable(true),
                       minw(-1),
                       minh(-1),
                       maxw(-1),
                       maxh(-1),
                       needs_ajustment(false) {}

    int current_x; // current position X
    int current_y; // current position Y
    int current_w; // current window width, adjusted
    int current_h; // current window height, adjusted
    int current_cw; // current content (view) width
    int current_ch; // current content (view) height
    int last_cw; // not subjected to fullscreen / maximize
    int last_ch;

    // Used to ajust window sizes because gtk doest not account frame extents as part
    // of the window size and JavaFx does.
    int adjust_w;
    int adjust_h;

    // The position of the view relative to the window
    int view_x;
    int view_y;

    // If WM supports _NET_REQUEST_FRAME_EXTENTS and it was received
    bool frame_extents_received;

    // Currently not used
    float gravity_x;
    float gravity_y;

    bool enabled;
    bool resizable;

    int minw;
    int minh;

    int maxw;
    int maxh;

    // if the window size was set (instead of content size) - this is used to
    // "fix" the window size accouting extents.
    bool needs_ajustment;
};

class WindowContext {
private:
    jlong screen;
    WindowFrameType frame_type;
    WindowType window_type;
    struct WindowContext *owner;
    jobject jwindow;
    jobject jview;

    bool map_received;
    bool visible_received;
    bool on_top;
    bool is_fullscreen;
    bool is_iconified;
    bool is_maximized;
    bool is_mouse_entered;
    bool can_be_deleted;

    struct _XIM {
    _XIM() : im(NULL), ic(NULL), enabled(FALSE) {}
        XIM im;
        XIC ic;
        bool enabled;
    } xim;

    size_t events_processing_cnt;

    WindowGeometry geometry;
    std::set<WindowContext *> children;
    GdkWMFunction gdk_windowManagerFunctions;
    GtkWidget *gtk_widget;
    GdkWindow *gdk_window;
    BgColor bg_color;
    void *grab_pointer;

    static WindowContext* sm_mouse_drag_window;
    static WindowContext* sm_grab_window;
public:
    WindowContext(jobject, WindowContext *, long, WindowFrameType, WindowType, GdkWMFunction);

    bool hasIME();
    bool filterIME(GdkEvent *);
    void enableOrResetIME();
    void disableIME();

    void paint(void*, jint, jint);
    bool isEnabled();

    GdkWindow *get_gdk_window();
    GtkWidget *get_gtk_widget();
    GtkWindow *get_gtk_window();
    WindowGeometry get_geometry();
    jobject get_jwindow();
    jobject get_jview();

    void process_map();
    void process_focus(GdkEventFocus*);
    void process_property_notify(GdkEventProperty *);
    void process_configure();
    void process_destroy();
    void process_delete();
    void process_expose(GdkEventExpose*);
    void process_mouse_button(GdkEventButton*);
    void process_mouse_motion(GdkEventMotion*);
    void process_mouse_scroll(GdkEventScroll*);
    void process_mouse_cross(GdkEventCrossing*);
    void process_key(GdkEventKey*);
    void process_state(GdkEventWindowState*);
    void process_net_wm_property();
    void process_screen_changed();

    void notify_on_top(bool);
    void notify_repaint();
    void notify_state(jint);

    bool set_view(jobject);
    void set_visible(bool);
    void set_cursor(GdkCursor*);
    void set_level(int);
    void set_background(float, float, float);
    void set_minimized(bool);
    void set_maximized(bool);
    void set_bounds(int, int, bool, bool, int, int, int, int);
    void set_resizable(bool);
    void set_focusable(bool);
    void set_title(const char *);
    void set_alpha(double);
    void set_enabled(bool);
    void set_minimum_size(int, int);
    void set_maximum_size(int, int);
    void set_icon(GdkPixbuf *);
    void set_modal(bool, WindowContext *parent = NULL);
    void set_gravity(float, float);
    void set_owner(WindowContext *);
    void add_child(WindowContext *);
    void remove_child(WindowContext *);
    void show_or_hide_children(bool);
    bool is_visible();
    bool is_dead();
    bool grab_focus();
    void ungrab_focus();
    void restack(bool);
    void request_focus();
    void enter_fullscreen();
    void exit_fullscreen();
    void detach_from_java();
    void increment_events_counter();
    void decrement_events_counter();
    size_t get_events_count();
    ~WindowContext();

protected:
    void applyShapeMask(void *, uint width, uint height);

private:
    bool im_filter_keypress(GdkEventKey*);
    void ensure_window_size();
    void calculate_adjustments();
    void apply_geometry();
    bool get_frame_extents_property(int *, int *, int *, int *);
    void activate_window();
    void size_position_notify(bool, bool);
    void update_ontop_tree(bool);
    bool on_top_inherited();
    bool effective_on_top();
    bool grab_mouse_drag_focus(GdkWindow *, GdkEvent *, GdkCursor *, bool);
    void ungrab_mouse_drag_focus();
};

void destroy_and_delete_ctx(WindowContext *ctx);

class EventsCounterHelper {
private:
    WindowContext *ctx;
public:
    explicit EventsCounterHelper(WindowContext *context) {
        ctx = context;
        ctx->increment_events_counter();
    }

    ~EventsCounterHelper() {
        ctx->decrement_events_counter();
        if (ctx->is_dead() && ctx->get_events_count() == 0) {
            delete ctx;
        }
        ctx = NULL;
    }
};

#endif        /* GLASS_WINDOW_H */

