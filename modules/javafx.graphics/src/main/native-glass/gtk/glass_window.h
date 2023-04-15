/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

enum WindowManager {
    COMPIZ,
    UNKNOWN
};

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

struct WindowFrameExtents {
    int top;
    int left;
    int bottom;
    int right;
};

static const guint MOUSE_BUTTONS_MASK = (guint) (GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK);

enum BoundsType {
    BOUNDSTYPE_CONTENT,
    BOUNDSTYPE_WINDOW
};

struct WindowGeometry {
    WindowGeometry(): final_width(), final_height(),
    size_assigned(false), x(), y(), gravity_x(), gravity_y(), extents() {}
    // estimate of the final width the window will get after all pending
    // configure requests are processed by the window manager
    struct {
        int value;
        BoundsType type;
    } final_width;

    struct {
        int value;
        BoundsType type;
    } final_height;

    bool size_assigned;

    int x;
    int y;
    float gravity_x;
    float gravity_y;

    WindowFrameExtents extents;
};

class WindowContext {
    size_t events_processing_cnt;
    jlong screen;
    WindowFrameType frame_type;
    WindowType window_type;
    struct WindowContext *owner;
    WindowGeometry geometry;
    struct _Resizable {// we can't use set/get gtk_window_resizable function
        _Resizable(): value(true),
                minw(-1), minh(-1), maxw(-1), maxh(-1) {}
        bool value; //actual value of resizable for a window
        int minw, minh, maxw, maxh; //minimum and maximum window width/height;
    } resizable;

    struct _XIM {
        _XIM() : im(NULL), ic(NULL), enabled(false) {}
        XIM im;
        XIC ic;
        bool enabled;
    } xim;

    bool on_top;
    bool is_fullscreen;
    bool is_iconified;
    bool is_maximized;
    bool is_mouse_entered;
    bool is_disabled;
    bool can_be_deleted;

    static WindowFrameExtents normal_extents;
    static WindowFrameExtents utility_extents;

    WindowManager wmanager;
    std::set<WindowContext*> children;
    jobject jwindow;
    jobject jview;

    GtkWidget* gtk_widget;
    GdkWindow* gdk_window;
    GdkWMFunction gdk_windowManagerFunctions;

    /*
     * sm_grab_window points to WindowContext holding a mouse grab.
     * It is mostly used for popup windows.
     */
    static WindowContext* sm_grab_window;

    /*
     * sm_mouse_drag_window points to a WindowContext from which a mouse drag started.
     * This WindowContext holding a mouse grab during this drag. After releasing
     * all mouse buttons sm_mouse_drag_window becomes NULL and sm_grab_window's
     * mouse grab should be restored if present.
     *
     * This is done in order to mimic Windows behavior:
     * All mouse events should be delivered to a window from which mouse drag
     * started, until all mouse buttons released. No mouse ENTER/EXIT events
     * should be reported during this drag.
     */
    static WindowContext* sm_mouse_drag_window;

public:
    WindowContext(jobject, WindowContext*, long, WindowFrameType, WindowType, GdkWMFunction);

    bool hasIME();
    bool filterIME(GdkEvent *);
    void enableOrResetIME();
    void disableIME();
    GdkWindow *get_gdk_window();
    jobject get_jwindow();
    jobject get_jview();
    bool isEnabled();
    WindowFrameExtents get_frame_extents();
    GtkWindow *get_gtk_window();

    void paint(void*, jint, jint);
    void add_child(WindowContext*);
    void remove_child(WindowContext*);
    void show_or_hide_children(bool);
    bool is_visible();
    bool set_view(jobject);
    bool grab_focus();
    bool grab_mouse_drag_focus();
    void ungrab_focus();
    void ungrab_mouse_drag_focus();

    void process_focus(GdkEventFocus*);
    void process_destroy();
    void process_delete();
    void process_expose(GdkEventExpose*);
    void process_mouse_button(GdkEventButton*);
    void process_mouse_motion(GdkEventMotion*);
    void process_mouse_scroll(GdkEventScroll*);
    void process_mouse_cross(GdkEventCrossing*);
    void process_key(GdkEventKey*);
    void process_state(GdkEventWindowState*);
    void process_property_notify(GdkEventProperty*);
    void process_configure(GdkEventConfigure*);

    void notify_state(jint);
    void work_around_compiz_state();

    void set_cursor(GdkCursor*);
    void set_background(float, float, float);
    void set_minimized(bool);
    void set_maximized(bool);
    void set_bounds(int, int, bool, bool, int, int, int, int, float, float);
    void set_resizable(bool);
    void set_focusable(bool);
    void set_title(const char*);
    void set_alpha(double);
    void set_enabled(bool);
    void set_minimum_size(int, int);
    void set_maximum_size(int, int);
    void set_icon(GdkPixbuf*);
    void set_modal(bool, WindowContext* parent = NULL);
    void set_level(int);
    void set_visible(bool);
    void set_owner(WindowContext*);
    void to_front();
    void to_back();
    void request_focus();

    void notify_on_top(bool);

    void enter_fullscreen();
    void exit_fullscreen();

    void detach_from_java();
    void increment_events_counter();
    void decrement_events_counter();
    size_t get_events_count();
    bool is_dead();

    ~WindowContext();
protected:
    void applyShapeMask(void*, uint width, uint height);
private:
    bool im_filter_keypress(GdkEventKey*);
    void request_frame_extents();
    void update_frame_extents();
    void set_cached_extents(WindowFrameExtents ex);
    WindowFrameExtents get_cached_extents();
    bool get_frame_extents_property(int *, int *, int *, int *);
    void update_window_constraints();
    void update_ontop_tree(bool);
    bool on_top_inherited();
    bool effective_on_top();
    void notify_window_move();
    void notify_window_resize();
    WindowContext(WindowContext&);
    WindowContext& operator= (const WindowContext&);
};

void destroy_and_delete_ctx(WindowContext* ctx);

class EventsCounterHelper {
private:
    WindowContext* ctx;
public:
    explicit EventsCounterHelper(WindowContext* context) {
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

