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
#ifndef GLASS_WINDOW_H
#define        GLASS_WINDOW_H

#include <gtk/gtk.h>
#include <X11/Xlib.h>

#include <jni.h>
#include <set>
#include <vector>

#include "DeletedMemDebug.h"

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
    size_assigned(false), x(), y(), view_x(), view_y(), gravity_x(), gravity_y(), extents() {}
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
    int view_x;
    int view_y;

    float gravity_x;
    float gravity_y;

    WindowFrameExtents extents;
};

class WindowContextTop;

class WindowContext : public DeletedMemDebug<0xCC> {
public:
    virtual bool isEnabled() = 0;
    virtual bool hasIME() = 0;
    virtual bool filterIME(GdkEvent *) = 0;
    virtual void enableOrResetIME() = 0;
    virtual void updateCaretPos() = 0;
    virtual void disableIME() = 0;
    virtual void setOnPreEdit(bool) = 0;
    virtual void commitIME(gchar *) = 0;

    virtual void paint(void* data, jint width, jint height) = 0;
    virtual WindowGeometry get_geometry() = 0;

    virtual void enter_fullscreen() = 0;
    virtual void exit_fullscreen() = 0;
    virtual void set_visible(bool) = 0;
    virtual bool is_visible() = 0;
    virtual void set_bounds(int, int, bool, bool, int, int, int, int, float, float) = 0;
    virtual void set_resizable(bool) = 0;
    virtual void request_focus() = 0;
    virtual void set_focusable(bool)= 0;
    virtual bool grab_focus() = 0;
    virtual bool grab_mouse_drag_focus() = 0;
    virtual void ungrab_focus() = 0;
    virtual void ungrab_mouse_drag_focus() = 0;
    virtual void set_title(const char*) = 0;
    virtual void set_alpha(double) = 0;
    virtual void set_enabled(bool) = 0;
    virtual void set_minimum_size(int, int) = 0;
    virtual void set_maximum_size(int, int) = 0;
    virtual void set_minimized(bool) = 0;
    virtual void set_maximized(bool) = 0;
    virtual void set_icon(GdkPixbuf*) = 0;
    virtual void to_front() = 0;
    virtual void to_back() = 0;
    virtual void set_cursor(GdkCursor*) = 0;
    virtual void set_modal(bool, WindowContext* parent = NULL) = 0;
    virtual void set_level(int) = 0;
    virtual void set_background(float, float, float) = 0;

    virtual void process_realize() = 0;
    virtual void process_property_notify(GdkEventProperty*) = 0;
    virtual void process_configure(GdkEventConfigure*) = 0;
    virtual void process_focus(GdkEventFocus*) = 0;
    virtual void process_destroy() = 0;
    virtual void process_delete() = 0;
    virtual void process_expose(GdkEventExpose*) = 0;
    virtual void process_mouse_button(GdkEventButton*) = 0;
    virtual void process_mouse_motion(GdkEventMotion*) = 0;
    virtual void process_mouse_scroll(GdkEventScroll*) = 0;
    virtual void process_mouse_cross(GdkEventCrossing*) = 0;
    virtual void process_key(GdkEventKey*) = 0;
    virtual void process_state(GdkEventWindowState*) = 0;

    virtual void notify_state(jint) = 0;
    virtual void notify_on_top(bool) {}
    virtual void update_view_size() = 0;
    virtual void notify_view_resize() = 0;

    virtual void add_child(WindowContextTop* child) = 0;
    virtual void remove_child(WindowContextTop* child) = 0;
    virtual bool set_view(jobject) = 0;

    virtual GdkWindow *get_gdk_window() = 0;
    virtual GtkWindow *get_gtk_window() = 0;
    virtual jobject get_jview() = 0;
    virtual jobject get_jwindow() = 0;

    virtual void increment_events_counter() = 0;
    virtual void decrement_events_counter() = 0;
    virtual size_t get_events_count() = 0;
    virtual bool is_dead() = 0;
    virtual ~WindowContext() {}
};

class WindowContextBase: public WindowContext {

    struct ImContext {
        GtkIMContext *ctx;
        bool enabled;
        bool on_preedit;
        bool send_keypress;
        bool on_key_event;
    } im_ctx;

    size_t events_processing_cnt;
    bool can_be_deleted;
protected:
    std::set<WindowContextTop*> children;
    jobject jwindow;
    jobject jview;
    GtkWidget* gtk_widget;
    GdkWindow* gdk_window = NULL;
    GdkWMFunction gdk_windowManagerFunctions;

    bool is_iconified;
    bool is_maximized;
    bool is_mouse_entered;
    bool is_disabled;

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
    bool isEnabled();
    bool hasIME();
    bool filterIME(GdkEvent *);
    void enableOrResetIME();
    void setOnPreEdit(bool);
    void commitIME(gchar *);
    void updateCaretPos();
    void disableIME();
    void paint(void*, jint, jint);
    GdkWindow *get_gdk_window();
    jobject get_jwindow();
    jobject get_jview();

    void add_child(WindowContextTop*);
    void remove_child(WindowContextTop*);
    void set_visible(bool);
    bool is_visible();
    bool set_view(jobject);
    bool grab_focus();
    bool grab_mouse_drag_focus();
    void ungrab_focus();
    void ungrab_mouse_drag_focus();
    void set_cursor(GdkCursor*);
    void set_level(int) {}
    void set_background(float, float, float);

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

    void notify_state(jint);

    void increment_events_counter();
    void decrement_events_counter();
    size_t get_events_count();
    bool is_dead();

    ~WindowContextBase();
protected:
    virtual void applyShapeMask(void*, uint width, uint height) = 0;
};

class WindowContextTop: public WindowContextBase {
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

    bool on_top;
    bool is_fullscreen;

    static WindowFrameExtents normal_extents;
    static WindowFrameExtents utility_extents;

    WindowManager wmanager;
public:
    WindowContextTop(jobject, WindowContext*, long, WindowFrameType, WindowType, GdkWMFunction);

    void process_realize();
    void process_property_notify(GdkEventProperty*);
    void process_state(GdkEventWindowState*);
    void process_configure(GdkEventConfigure*);
    void process_destroy();
    void work_around_compiz_state();

    WindowGeometry get_geometry();

    void set_minimized(bool);
    void set_maximized(bool);
    void set_bounds(int, int, bool, bool, int, int, int, int, float, float);
    void set_resizable(bool);
    void request_focus();
    void set_focusable(bool);
    void set_title(const char*);
    void set_alpha(double);
    void set_enabled(bool);
    void set_minimum_size(int, int);
    void set_maximum_size(int, int);
    void set_icon(GdkPixbuf*);
    void to_front();
    void to_back();
    void set_modal(bool, WindowContext* parent = NULL);
    void set_level(int);
    void set_visible(bool);
    void notify_on_top(bool);
    void update_view_size();
    void notify_view_resize();

    void enter_fullscreen();
    void exit_fullscreen();

    void set_owner(WindowContext*);

    GtkWindow *get_gtk_window();
    void detach_from_java();

protected:
    void applyShapeMask(void*, uint width, uint height);
private:
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
    WindowContextTop(WindowContextTop&);
    WindowContextTop& operator= (const WindowContextTop&);
};

void destroy_and_delete_ctx(WindowContext* ctx);

class EventsCounterHelper {
private:
    WindowContext* ctx;
public:
    explicit EventsCounterHelper(WindowContext* context) {
        ctx = context;
        if (ctx != nullptr) {
            ctx->increment_events_counter();
        }
    }
    ~EventsCounterHelper() {
        if (ctx != nullptr) {
            ctx->decrement_events_counter();
            if (ctx->is_dead() && ctx->get_events_count() == 0) {
                delete ctx;
            }
            ctx = NULL;
        }
    }
};

#endif        /* GLASS_WINDOW_H */

