/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

struct WindowFrameExtents {
    int top;
    int left;
    int bottom;
    int right;
};

static const guint MOUSE_BUTTONS_MASK = (guint) (GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK);


struct WindowGeometry {
    WindowGeometry() : current_x(0),
                       current_y(0),
                       current_w(0),
                       current_h(0),
                       current_cw(0),
                       current_ch(0),
                       last_cw(0),
                       last_ch(0),
                       extents({0, 0, 0, 0}),
                       frame_extents_supported(false),
                       frame_extents_received(false),
                       enabled(true),
                       resizable(true),
                       minw(-1),
                       minh(-1),
                       maxw(-1),
                       maxh(-1),
                       needs_frame_extents(false),
                       content_size(true),
                       size_assigned(false) {}

    int current_x; // current position X
    int current_y; // current position Y
    int current_w; // current window width, adjusted (see adjust_w)
    int current_h; // current window height, adjusted (see adjust_h)
    int current_cw; // current content (view) width
    int current_ch; // current content (view) height
    int last_cw; // not subjected to fullscreen / maximize
    int last_ch;

    // Window decoration sizes
    WindowFrameExtents extents;

    // If _NET_REQUEST_FRAME_EXTENTS is supported by the window manager
    bool frame_extents_supported;

    // If _NET_REQUEST_FRAME_EXTENTS was received
    bool frame_extents_received;

    bool enabled;
    bool resizable;

    int minw;
    int minh;

    int maxw;
    int maxh;

    // Frame extents are the window frame (decorations)
    bool needs_frame_extents;

    // If size is by content or window size
    bool content_size;

    bool size_assigned;
};

class WindowContextChild;
class WindowContextTop;

class WindowContext {
public:
    virtual bool isEnabled() = 0;
    virtual bool hasIME() = 0;
    virtual bool filterIME(GdkEvent *) = 0;
    virtual void enableOrResetIME() = 0;
    virtual void disableIME() = 0;
    virtual void paint(void* data, jint width, jint height) = 0;
    virtual WindowGeometry get_geometry() = 0;

    virtual void enter_fullscreen() = 0;
    virtual void exit_fullscreen() = 0;
    virtual void show_or_hide_children(bool) = 0;
    virtual void set_visible(bool) = 0;
    virtual bool is_visible() = 0;
    virtual void set_bounds(int, int, bool, bool, int, int, int, int) = 0;
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
    virtual void restack(bool) = 0;
    virtual void set_cursor(GdkCursor*) = 0;
    virtual void set_modal(bool, WindowContext* parent = NULL) = 0;
    virtual void set_gravity(float, float) = 0;
    virtual void set_level(int) = 0;
    virtual void set_background(float, float, float) = 0;

    virtual void process_property_notify(GdkEventProperty*) = 0;
    virtual void process_configure(GdkEventConfigure*) = 0;
    virtual void process_map() = 0;
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

    virtual void add_child(WindowContextTop* child) = 0;
    virtual void remove_child(WindowContextTop* child) = 0;
    virtual bool set_view(jobject) = 0;

    virtual GdkWindow *get_gdk_window() = 0;
    virtual GtkWindow *get_gtk_window() = 0;
    virtual jobject get_jview() = 0;
    virtual jobject get_jwindow() = 0;

    virtual int getEmbeddedX() = 0;
    virtual int getEmbeddedY() = 0;

    virtual void increment_events_counter() = 0;
    virtual void decrement_events_counter() = 0;
    virtual size_t get_events_count() = 0;
    virtual bool is_dead() = 0;
    virtual ~WindowContext() {}
};

class WindowContextBase: public WindowContext {

    struct _XIM{
        XIM im;
        XIC ic;
        bool enabled;
    } xim;

    size_t events_processing_cnt;
    bool can_be_deleted;
protected:
    std::set<WindowContextTop*> children;
    jobject jwindow;
    jobject jview;
    GtkWidget* gtk_widget;
    GdkWindow* gdk_window;
    GdkWMFunction gdk_windowManagerFunctions;

    bool is_iconified;
    bool is_maximized;
    bool is_mouse_entered;

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
    void disableIME();
    void paint(void*, jint, jint);
    GdkWindow *get_gdk_window();
    jobject get_jwindow();
    jobject get_jview();

    void add_child(WindowContextTop*);
    void remove_child(WindowContextTop*);
    void show_or_hide_children(bool);
    void reparent_children(WindowContext* parent);
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

    void process_map() {}
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

    int getEmbeddedX() { return 0; }
    int getEmbeddedY() { return 0; }

    void increment_events_counter();
    void decrement_events_counter();
    size_t get_events_count();
    bool is_dead();

    ~WindowContextBase();
protected:
    virtual void applyShapeMask(void*, uint width, uint height) = 0;
private:
    bool im_filter_keypress(GdkEventKey*);
};

class WindowContextPlug: public WindowContextBase {
    WindowContext* parent;
public:
    bool set_view(jobject);
    void set_bounds(int, int, bool, bool, int, int, int, int);
    WindowGeometry get_geometry() { return WindowGeometry(); }

    void enter_fullscreen() {}
    void exit_fullscreen() {}
    void set_resizable(bool) {}
    void request_focus() {}
    void set_focusable(bool) {}
    void set_title(const char*) {}
    void set_alpha(double) {}
    void set_enabled(bool) {}
    void set_minimum_size(int, int) {}
    void set_maximum_size(int, int) {}
    void set_minimized(bool) {}
    void set_maximized(bool) {}
    void set_icon(GdkPixbuf*) {}
    void restack(bool) {}
    void set_modal(bool, WindowContext*) {}
    void set_gravity(float, float) {}
    void process_property_notify(GdkEventProperty*) {}
    void process_configure(GdkEventConfigure*);
    void process_gtk_configure(GdkEventConfigure*);

    void applyShapeMask(void*, uint width, uint height) {
        (void)width;
        (void)height;
    }
    GtkWindow *get_gtk_window(); // TODO, get window from parent

    WindowContextPlug(jobject, void*);
    GtkWidget* gtk_container;
    std::vector<WindowContextChild *> embedded_children;
private:
    //HACK: remove once set_bounds is implemented correctly
    void window_configure(XWindowChanges *, unsigned int);
    WindowContextPlug(WindowContextPlug&);
    WindowContextPlug& operator= (const WindowContextPlug&);
};

class WindowContextChild: public WindowContextBase {
    WindowContextPlug* parent;
    WindowContextTop* full_screen_window;
    GlassView* view; // not null while in Full Screen
public:
    void process_mouse_button(GdkEventButton*);
    bool set_view(jobject);
    void set_bounds(int, int, bool, bool, int, int, int, int);
    WindowGeometry get_geometry() { return WindowGeometry(); }

    void enter_fullscreen();
    void exit_fullscreen();
    void set_resizable(bool) {}
    void request_focus() {}
    void set_focusable(bool) {}
    void set_title(const char*) {}
    void set_alpha(double) {}
    void set_enabled(bool) {}
    void set_minimum_size(int, int) {}
    void set_maximum_size(int, int) {}
    void set_minimized(bool) {}
    void set_maximized(bool) {}
    void set_icon(GdkPixbuf*) {}
    void restack(bool);
    void set_modal(bool, WindowContext*) {}
    void set_gravity(float, float) {}
    void process_property_notify(GdkEventProperty*) {}
    void process_configure(GdkEventConfigure*);
    void process_destroy();
    void set_visible(bool visible);

    int getEmbeddedX();
    int getEmbeddedY();

    void applyShapeMask(void*, uint width, uint height) {
        (void)width;
        (void)height;
    }
    GtkWindow *get_gtk_window(); // TODO, get window from parent

    WindowContextChild(jobject, void*, GtkWidget *parent_widget, WindowContextPlug *parent_context);
private:
    WindowContextChild(WindowContextChild&);
    WindowContextChild& operator= (const WindowContextChild&);
};

class WindowContextTop: public WindowContextBase {
    jlong screen;
    WindowFrameType frame_type;
    WindowType window_type;
    struct WindowContext *owner;
    WindowGeometry geometry;
    bool map_received;
    bool visible_received;
    bool on_top;
    bool is_maximized;
    bool is_fullscreen;

    static WindowFrameExtents normal_extents;
    static WindowFrameExtents utility_extents;
public:
    WindowContextTop(jobject, WindowContext*, long, WindowFrameType, WindowType, GdkWMFunction);
    void process_map();
    void process_property_notify(GdkEventProperty*);
    void process_configure(GdkEventConfigure*);
    void process_destroy();
    void process_net_wm_property();

    WindowGeometry get_geometry();

    void set_minimized(bool);
    void set_maximized(bool);
    void set_bounds(int, int, bool, bool, int, int, int, int);
    void set_resizable(bool);
    void request_focus();
    void set_focusable(bool);
    void set_title(const char*);
    void set_alpha(double);
    void set_enabled(bool);
    void set_minimum_size(int, int);
    void set_maximum_size(int, int);
    void set_icon(GdkPixbuf*);
    void restack(bool);
    void set_modal(bool, WindowContext* parent = NULL);
    void set_gravity(float, float);
    void set_level(int);
    void set_visible(bool);
    void notify_on_top(bool);

    void enter_fullscreen();
    void exit_fullscreen();

    void set_owner(WindowContext*);

    GtkWindow *get_gtk_window();
    void detach_from_java();
protected:
    void applyShapeMask(void*, uint width, uint height);
private:
    void size_position_notify(bool, bool);
    void apply_geometry();
    bool get_frame_extents_property(int *, int *, int *, int *);
    void request_frame_extents();
    void activate_window();
    void set_cached_extents(WindowFrameExtents ex);
    WindowFrameExtents get_cached_extents();
    void update_ontop_tree(bool);
    bool on_top_inherited();
    bool effective_on_top();
    void ensure_window_size();
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

