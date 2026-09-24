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
#include <com_sun_glass_events_ViewEvent.h>

#include <cstdlib>
#include <cstring>
#include <cassert>

#include "glass_general.h"
#include "glass_view.h"
#include "glass_window.h"

#define GGTK_GLASSVIEW(view) ((GlassView *) (view))

// The C of _setParent before its upcall: re-parents the view and answers the ViewEvent to notify
static int32_t view_set_parent(GlassView* view, WindowContext* parent)
{
    bool is_removing = view->current_window && !parent;

    view->current_window = parent;

    return is_removing ? com_sun_glass_events_ViewEvent_REMOVE : com_sun_glass_events_ViewEvent_ADD;
}

// The C of _enterFullscreen before its upcall: whether the view had a window to put in full screen
static bool view_enter_fullscreen(GlassView* view)
{
    if (view->current_window) {
        view->current_window->enter_fullscreen();
        return true;
    }
    return false;
}

// The C of _exitFullscreen before its upcall: whether the view had a window to take out of full screen
static bool view_exit_fullscreen(GlassView* view)
{
    if (view->current_window) {
        if (view->embedded_window) {
            view->embedded_window->exit_fullscreen();
        } else {
            view->current_window->exit_fullscreen();
        }
        return true;
    }
    return false;
}

/*
 * Each ggtk_view_* function below is the body of a Java_com_sun_glass_ui_gtk_GtkView_* function of commit
 * 033187ad90 (named at its prototype in glass_gtk_api.h, with its contract), which GtkView now calls through
 * GtkGlassNative. _setParent, _enterFullscreen and _exitFullscreen called Java with the View they were called on
 * as the receiver; their functions dial GgtkViewCallbacks.notify_view with the view's own id instead, and make
 * no call when that slot is NULL. _getNativeView (it answered 0) and _scheduleRepaint (it did nothing) have no
 * function here: GtkView does the same in Java.
 */

extern "C" {

void ggtk_view_enable_input_method_events(ggtk_view_t view, int32_t enable)
{
    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view->current_window) {
        if (enable) {
            glass_view->current_window->enableOrResetIME();
        } else {
            glass_view->current_window->disableIME();
        }
    }
}

ggtk_view_t ggtk_view_create(int64_t view_id)
{
    GlassView *view = new GlassView();
    view->id = view_id;
    return view;
}

int32_t ggtk_view_get_x(ggtk_view_t view)
{
    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view && glass_view->current_window) {
        return glass_view->current_window->get_geometry().view_x;
    }
    return 0;
}

int32_t ggtk_view_get_y(ggtk_view_t view)
{
    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view && glass_view->current_window) {
        return glass_view->current_window->get_geometry().view_y;
    }
    return 0;
}

void ggtk_view_set_parent(ggtk_view_t view, ggtk_window_t parent)
{
    GlassView* glass_view = GGTK_GLASSVIEW(view);
    int32_t type = view_set_parent(glass_view, (WindowContext*) parent);

    if (glass_view_cb.notify_view) {
        // CHECK_JNI_EXCEPTION site at the end of the function: nothing follows, the status is ignored
        glass_view_cb.notify_view(glass_view->id, type);
    }
}

int32_t ggtk_view_close(ggtk_view_t view)
{
    delete GGTK_GLASSVIEW(view);
    return 1;
}

void ggtk_view_upload_pixels_direct(ggtk_view_t view, void* data, int32_t width, int32_t height)
{
    if (!view) return;

    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view->current_window) {
        glass_view->current_window->paint(data, width, height);
    }
}

void ggtk_view_upload_pixels_int(ggtk_view_t view, const int32_t* pixels, int32_t pixels_len, int32_t offset,
                                 int32_t width, int32_t height)
{
    if (!view) return;
    if (!pixels) return;
    if (offset < 0) return;
    if (width <= 0 || height <= 0) return;

    if (width > ((INT_MAX - offset) / height))
    {
        return;
    }

    if ((width * height + offset) > pixels_len)
    {
        return;
    }

    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view->current_window) {
        int *data = (int*) pixels;

        glass_view->current_window->paint(data + offset, width, height);
    }
}

void ggtk_view_upload_pixels_byte(ggtk_view_t view, const uint8_t* pixels, int32_t pixels_len, int32_t offset,
                                  int32_t width, int32_t height)
{
    if (!view) return;
    if (!pixels) return;
    if (offset < 0) return;
    if (width <= 0 || height <= 0) return;

    if (width > (((INT_MAX - offset) / 4) / height))
    {
        return;
    }

    if ((4 * width * height + offset) > pixels_len)
    {
        return;
    }

    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (glass_view->current_window) {
        unsigned char *data = (unsigned char*) pixels;

        glass_view->current_window->paint(data + offset, width, height);
    }
}

int32_t ggtk_view_enter_fullscreen(ggtk_view_t view, int32_t animate, int32_t keep_ratio, int32_t hide_cursor)
{
    (void)animate;
    (void)keep_ratio;
    (void)hide_cursor;

    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (view_enter_fullscreen(glass_view)) {
        if (glass_view_cb.notify_view) {
            if (glass_view_cb.notify_view(glass_view->id, com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER)) {
                return 0;
            }
        }
    }
    return 1;
}

void ggtk_view_exit_fullscreen(ggtk_view_t view, int32_t animate)
{
    (void)animate;

    GlassView* glass_view = GGTK_GLASSVIEW(view);
    if (view_exit_fullscreen(glass_view)) {
        if (glass_view_cb.notify_view) {
            // CHECK_JNI_EXCEPTION site at the end of the function: nothing follows, the status is ignored
            glass_view_cb.notify_view(glass_view->id, com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT);
        }
    }
}

} // extern "C"
