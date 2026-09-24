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
#include <com_sun_glass_ui_Window.h>
#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_ViewEvent.h>

#include <cstdlib>
#include <cstring>
#include "glass_general.h"
#include "glass_evloop.h"
#include "glass_window.h"

#define GGTK_WINDOW_CTX(window) ((WindowContext*)(window))

static WindowFrameType glass_mask_to_window_frame_type(int32_t mask) {
    if (mask & com_sun_glass_ui_Window_TRANSPARENT) {
        return TRANSPARENT;
    }
    if (mask & com_sun_glass_ui_Window_TITLED) {
        return TITLED;
    }
    if (mask & com_sun_glass_ui_Window_EXTENDED) {
        return EXTENDED;
    }
    return UNTITLED;
}

static WindowType glass_mask_to_window_type(int32_t mask) {
    if (mask & com_sun_glass_ui_Window_POPUP) {
        return POPUP;
    }
    if (mask & com_sun_glass_ui_Window_UTILITY) {
        return UTILITY;
    }
    return NORMAL;
}

static GdkWMFunction glass_mask_to_wm_function(int32_t mask) {
    int func = GDK_FUNC_RESIZE | GDK_FUNC_MOVE;

    if (mask & com_sun_glass_ui_Window_CLOSABLE) {
        func |= GDK_FUNC_CLOSE;
    }
    if (mask & com_sun_glass_ui_Window_MAXIMIZABLE) {
        func |= GDK_FUNC_MAXIMIZE;
    }
    if (mask & com_sun_glass_ui_Window_MINIMIZABLE) {
        func |= GDK_FUNC_MINIMIZE;
    }

    return (GdkWMFunction) func;
}

/*
 * Each ggtk_window_* function below is the body of a Java_com_sun_glass_ui_gtk_GtkWindow_* function of commit
 * 033187ad90 (named at its prototype in glass_gtk_api.h, with its contract), which GtkWindow now calls through
 * GtkGlassNative. The style bits of the mask are the com.sun.glass.ui.Window constants the JNI read through
 * GtkWindow's generated header.
 */

extern "C" {

ggtk_window_t ggtk_window_create(ggtk_window_t owner, int64_t screen, int32_t mask, int64_t visual_id,
                                 int64_t window_id)
{
    WindowContext* parent = GGTK_WINDOW_CTX(owner);

    WindowContext* ctx = new WindowContextTop(window_id,
            parent,
            screen,
            glass_mask_to_window_frame_type(mask),
            glass_mask_to_window_type(mask),
            glass_mask_to_wm_function(mask),
            (glong) visual_id
            );

    return ctx;
}

int32_t ggtk_window_close(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    destroy_and_delete_ctx(ctx);
    return 1; // return value not used
}

int32_t ggtk_window_set_view(ggtk_window_t window, ggtk_view_t view, int64_t* out_view_id)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    GlassView* glass_view = (GlassView*) view;
    // What set_view stores, read before its EXIT upcall as set_view reads it
    int64_t new_view_id = (glass_view != NULL) ? glass_view->id : 0;

    bool result = ctx->set_view(glass_view);
    if (out_view_id != NULL) {
        *out_view_id = new_view_id;
    }
    return result ? 1 : 0;
}

void ggtk_window_update_view_size(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->update_view_size();
}

void ggtk_window_minimize(ggtk_window_t window, int32_t minimize)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_minimized(minimize);
}

void ggtk_window_maximize(ggtk_window_t window, int32_t maximize, int32_t was_maximized)
{
    (void)was_maximized;

    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_maximized(maximize);
}

void ggtk_window_set_bounds(ggtk_window_t window, int32_t x, int32_t y, int32_t x_set, int32_t y_set,
                            int32_t w, int32_t h, int32_t cw, int32_t ch, float x_gravity, float y_gravity)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_bounds(x, y, x_set, y_set, w, h, cw, ch, x_gravity, y_gravity);
}

void ggtk_window_set_visible(ggtk_window_t window, int32_t visible)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_visible(visible);
}

int32_t ggtk_window_set_resizable(ggtk_window_t window, int32_t resizable)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_resizable(resizable);
    return 1;
}

int32_t ggtk_window_request_focus(ggtk_window_t window, int32_t event)
{
    (void)event;

    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->request_focus();
    return 1; //not used
}

void ggtk_window_set_focusable(ggtk_window_t window, int32_t focusable)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_focusable(focusable);
}

int32_t ggtk_window_grab_focus(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    return ctx->grab_focus() ? 1 : 0;
}

void ggtk_window_ungrab_focus(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->ungrab_focus();
}

int32_t ggtk_window_set_title(ggtk_window_t window, const uint16_t* title, int32_t title_len)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);

    // glass_general.cpp jstring_to_utf8 of commit 033187ad90: the UTF-16 code units to standard UTF-8 with GLib
    // (NULL for a null String, and for text g_utf16_to_utf8 rejects)
    gchar * ctitle = (title != NULL)
            ? g_utf16_to_utf8((const gunichar2 *) title, title_len, NULL, NULL, NULL)
            : NULL;
    ctx->set_title(ctitle);
    g_free(ctitle);

    return 1;
}

void ggtk_window_set_level(ggtk_window_t window, int32_t level)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_level(level);
}

void ggtk_window_set_alpha(ggtk_window_t window, float alpha)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_alpha(alpha);
}

int32_t ggtk_window_set_background(ggtk_window_t window, float r, float g, float b)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_background(r, g, b);
    return 1;
}

void ggtk_window_set_enabled(ggtk_window_t window, int32_t enabled)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_enabled(enabled);
}

int32_t ggtk_window_set_minimum_size(ggtk_window_t window, int32_t w, int32_t h)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    if (w < 0 || h < 0) return 0;
    ctx->set_minimum_size(w, h);
    return 1;
}

int32_t ggtk_window_set_system_minimum_size(ggtk_window_t window, int32_t w, int32_t h)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    if (w < 0 || h < 0) return 0;
    ctx->set_system_minimum_size(w, h);
    return 1;
}

int32_t ggtk_window_set_maximum_size(ggtk_window_t window, int32_t w, int32_t h)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    if (w == 0 || h == 0) return 0;
    if (w == -1) w = G_MAXSHORT;
    if (h == -1) h = G_MAXSHORT;

    ctx->set_maximum_size(w, h);
    return 1;
}

void ggtk_window_set_icon(ggtk_window_t window, void* pixbuf, int32_t attach_threw)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    GdkPixbuf *icon = (GdkPixbuf *) pixbuf;
    if (!attach_threw) {
        ctx->set_icon(icon);
    }
    if (icon != NULL) g_object_unref(icon);
}

void ggtk_window_to_front(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->to_front();
}

void ggtk_window_to_back(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->to_back();
}

void ggtk_window_set_cursor_type(ggtk_window_t window, int32_t type)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    GdkCursor *cursor = get_native_cursor(type);
    ctx->set_cursor(cursor);
}

void ggtk_window_set_cursor(ggtk_window_t window, void* cursor)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->set_cursor((GdkCursor*) cursor);
}

void ggtk_window_show_system_menu(ggtk_window_t window, int32_t x, int32_t y)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    ctx->show_system_menu(x, y);
}

int32_t ggtk_window_is_visible(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    return ctx->is_visible() ? 1 : 0;
}

int64_t ggtk_window_get_native_window(ggtk_window_t window)
{
    WindowContext* ctx = GGTK_WINDOW_CTX(window);
    GdkWindow *win = ctx->get_gdk_window();

    if (win == NULL) {
        return 0;
    }

    return GDK_WINDOW_XID(win);
}

} // extern "C"
