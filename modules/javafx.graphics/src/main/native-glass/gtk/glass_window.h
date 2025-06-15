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

#define DEFAULT_WIDTH 320
#define DEFAULT_HEIGHT 200

#define GETTER(type, name) \
    type get_##name() const { return name; }

#include <gtk/gtk.h>
#include <X11/Xlib.h>

#include <jni.h>
#include <set>
#include <vector>
#include <optional>

#include "DeletedMemDebug.h"
#include "glass_view.h"
#include "glass_general.h"

enum WindowFrameType {
    TITLED,
    UNTITLED,
    TRANSPARENT,
    EXTENDED
};

enum WindowType {
    NORMAL,
    UTILITY,
    POPUP
};

enum BoundsType {
    BOUNDSTYPE_UNKNOWN,
    BOUNDSTYPE_VIEW,
    BOUNDSTYPE_WINDOW
};

static const guint MOUSE_BUTTONS_MASK = (guint) (GDK_BUTTON1_MASK | GDK_BUTTON2_MASK | GDK_BUTTON3_MASK);

struct WindowGeometry {
    WindowGeometry():
    width{-1, -1, BOUNDSTYPE_UNKNOWN},
    height{-1, -1, BOUNDSTYPE_UNKNOWN},
    x(), y(), view_x(), view_y(),
    gravity_x(), gravity_y(),
    extents(), frame_extents_received(false),
    needs_to_update_frame_extents(false) {}

    struct {
        int window;
        int view;
        BoundsType type;
    } width;

    struct {
        int window;
        int view;
        BoundsType type;
    } height;

    int x;
    int y;

    int view_x;
    int view_y;

    float gravity_x;
    float gravity_y;

    GdkRectangle extents;

    bool frame_extents_received;
    bool needs_to_update_frame_extents;
};

class WindowContext;

class WindowContext: public DeletedMemDebug<0xCC> {
private:
    static std::optional<GdkRectangle> normal_extents;
    static std::optional<GdkRectangle> utility_extents;

    struct _ImContext {
        _ImContext(): ctx(nullptr), enabled(false), on_preedit(false),
                     send_keypress(false), on_key_event(false) {}
        GtkIMContext *ctx;
        bool enabled;
        bool on_preedit;
        bool send_keypress;
        bool on_key_event;
    } im_ctx;

    size_t events_processing_cnt{};
    std::set<WindowContext*> children;

    jobject jwindow;
    jobject jview{};

    struct WindowContext *owner;
    jlong screen;

    bool is_mouse_entered{false};
    bool is_disabled{false};
    bool on_top{false};
    bool can_be_deleted{false};
    bool mapped{false};
    gint initial_state_mask{0};

    WindowFrameType frame_type;
    WindowType window_type;
    WindowGeometry geometry;

    struct _Resizable {
        _Resizable(): value(true),
                minw(-1), minh(-1), maxw(-1), maxh(-1), sysminw(-1), sysminh(-1) {}
        bool value; //actual value of resizable for a window
        int minw, minh, maxw, maxh; //minimum and maximum window width/height;
        int sysminw, sysminh; // size of window button area of EXTENDED windows
    } resizable;
    GdkWindow *gdk_window{};

    GdkWMFunction initial_wmf;
    GdkWMFunction current_wmf;

    GdkCursor* gdk_cursor{};
    GdkCursor* gdk_cursor_override{};

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
    WindowContext() = delete;
    WindowContext(jobject, WindowContext* _owner, long _screen,
                  WindowFrameType _frame_type, WindowType type, GdkWMFunction wmf);

    GETTER(jobject, jwindow)
    GETTER(jobject, jview)
    GETTER(WindowFrameType, frame_type);
    GETTER(WindowType, window_type);
    GETTER(WindowGeometry, geometry);

    bool isEnabled();
    bool hasIME();
    bool filterIME(GdkEvent*);
    void enableOrResetIME();
    void setOnPreEdit(bool);
    void commitIME(gchar *);
    void updateCaretPos();
    void disableIME();

    void paint(void*, jint, jint);
    GdkWindow *get_gdk_window();
    XID get_native_window();

    void add_child(WindowContext*);
    void remove_child(WindowContext*);
    void set_visible(bool);
    bool is_visible();
    bool is_resizable();
    bool set_view(jobject);
    bool grab_focus();
    bool grab_mouse_drag_focus();
    void ungrab_focus();
    void ungrab_mouse_drag_focus();
    void set_cursor(GdkCursor*);
    void set_cursor_override(GdkCursor*);
    void set_background(float, float, float);

    void process_map();
    void process_focus(GdkEventFocus*);
    void process_focus(bool);
    void notify_repaint();
    void notify_repaint(GdkRectangle*);
    virtual void process_mouse_button(GdkEventButton*, bool synthesized = false);
    virtual void process_mouse_motion(GdkEventMotion*);
    void process_mouse_scroll(GdkEventScroll*);
    void process_mouse_cross(GdkEventCrossing*);
    void process_key(GdkEventKey*);
    void process_state(GdkEventWindowState*);
    void process_property_notify(GdkEventProperty*);
    void process_configure(GdkEventConfigure*);
    void process_delete();
    void process_destroy();

    void increment_events_counter();
    void decrement_events_counter();
    size_t get_events_count();
    bool is_dead();

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
    void set_modal(bool, WindowContext* parent = nullptr);
    void set_level(int);
    void set_owner(WindowContext*);
    void update_view_size();
    void enter_fullscreen();
    void exit_fullscreen();
    void update_initial_state();
    void show_system_menu(int, int);
    void set_system_minimum_size(int, int);

    virtual ~WindowContext();

private:
    GdkVisual* find_best_visual();
    void maximize(bool);
    void iconify(bool);
    void update_window_size();
    void move_resize(int, int, bool, bool, int, int);
    void add_wmf(GdkWMFunction);
    void remove_wmf(GdkWMFunction);
    void notify_on_top(bool);
    void notify_fullscreen(bool);
    void notify_window_resize(int);
    void notify_window_move();
    void notify_view_resize();
    void notify_view_move();
    void notify_current_sizes();
    GdkAtom get_net_frame_extents_atom();
    void request_frame_extents();
    void update_frame_extents();
    void set_cached_extents(GdkRectangle);
    void load_cached_extents();
    bool get_frame_extents_property(int *, int *, int *, int *);
    void update_window_constraints();
    void update_ontop_tree(bool);
    bool on_top_inherited();
    bool effective_on_top();
};

class WindowContextExtended;

class WindowContextExtended : public WindowContext {
public:
    WindowContextExtended(jobject jwin,
                          WindowContext* owner,
                          long screen,
                          WindowFrameType frameType,
                          WindowType windowType,
                          GdkWMFunction wmf)
        : WindowContext(jwin, owner, screen, frameType, windowType, wmf)
    {
        LOG("WindowContextExtended\n")
    }

public:
    void process_mouse_button(GdkEventButton*, bool synthesized = false) override;
    void process_mouse_motion(GdkEventMotion*) override;
    bool get_window_edge(int x, int y, GdkWindowEdge*);
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
                LOG("EventsCounterHelper: delete ctx\n");
                delete ctx;
            }
            ctx = nullptr;
        }
    }
};

#endif        /* GLASS_WINDOW_H */

