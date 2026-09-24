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
#ifndef GLASS_GENERAL_H
#define        GLASS_GENERAL_H

#include <stdint.h>
#include <X11/Xlib.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>

#include "wrapped.h"
#include "glass_gtk_api.h"

// The callback tables Java installed with ggtk_*_set_callbacks (glass_gtk_api.cpp). A call site that
// replaced a JNI upcall dials its slot when the slot is non-NULL and makes no call otherwise (see
// glass_gtk_api.h, NULL SLOTS).
extern GgtkAppCallbacks glass_app_cb;
extern GgtkWindowCallbacks glass_window_cb;
extern GgtkViewCallbacks glass_view_cb;
extern GgtkDndCallbacks glass_dnd_cb;

#define GLASS_GTK3

#ifndef GDK_TOUCH_MASK
#define GDK_TOUCH_MASK (1 << 22)
#endif

#define GDK_FILTERED_EVENTS_MASK static_cast<GdkEventMask>(GDK_ALL_EVENTS_MASK \
                & ~GDK_TOUCH_MASK)

#define FILE_PREFIX "file://"
#define URI_LIST_COMMENT_PREFIX "#"
#define URI_LIST_LINE_BREAK "\r\n"

#define GLASS_GDK_KEY_CONSTANT(key) (GDK_KEY_ ## key)

#define SAFE_FREE(PTR)  \
    if ((PTR) != NULL) {  \
        free(PTR);     \
        (PTR) = NULL;     \
    }

    extern char const * const GDK_WINDOW_DATA_CONTEXT;

    GdkCursor* get_native_cursor(int type);

    gchar* get_application_name();

    guint8* convert_BGRA_to_RGBA(const int* pixels, int stride, int height);

    uint8_t is_display_valid();

    gsize get_files_count(gchar **uris);


#ifdef __cplusplus
extern "C" {
#endif

// -Djdk.gtk.verbose, as the jboolean of commit 033187ad90: 0 or 1, and 1 byte wide in wrapped.c too.
extern uint8_t gtk_verbose;

void
glass_widget_set_visual (GtkWidget *widget, GdkVisual *visual);

GdkScreen *
glass_gdk_window_get_screen(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow * gdkWindow, GdkCursor *cursor, gboolean owner_events);

void
glass_gdk_mouse_devices_ungrab();

void
glass_gdk_master_pointer_grab(GdkEvent *event, GdkWindow *window, GdkCursor *cursor);

void
glass_gdk_master_pointer_ungrab(GdkEvent *event);

void
glass_gdk_master_pointer_get_position(gint *x, gint *y);

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device);

void
glass_gdk_device_ungrab(GdkDevice *device);

GdkWindow *
glass_gdk_device_get_window_at_position(
               GdkDevice *device, gint *x, gint *y);

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                                  gboolean transparent);

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual);

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h);

void
glass_gdk_x11_display_set_window_scale(GdkDisplay *display, gint scale);

gboolean
glass_configure_window_transparency(GtkWidget *window, gboolean transparent);

void
glass_window_apply_shape_mask(GdkWindow *window,
    void* data, uint width, uint height);

void
glass_window_reset_input_shape_mask(GdkWindow *window);

GdkWindow *
glass_gdk_drag_context_get_dest_window (GdkDragContext * context);

guint
glass_settings_get_guint_opt (const gchar *schema_name,
                    const gchar *key_name,
                    int defval);

#ifdef __cplusplus
}
#endif

#endif        /* GLASS_GENERAL_H */
