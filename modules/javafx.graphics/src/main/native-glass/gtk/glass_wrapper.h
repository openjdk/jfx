/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __GTK_WRAPPER_H__
#define __GTK_WRAPPER_H__

#include <gtk/gtk.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>

#ifdef __cplusplus
extern "C" {
#endif

#define GLASS_GDK_KEY_CONSTANT(key) (GDK_KEY_ ## key)

extern int wrapper_load_symbols(int version, int debug);
extern int wrapper_load_symbols_gtk(int version, void *handle);
extern int wrapper_load_symbols_gdk(int version, void *handle);
extern int wrapper_load_symbols_pix(int version, void * handle);
extern int wrapper_load_symbols_gio(void * handle);

extern int wrapper_debug;
extern int wrapper_loaded;
extern int wrapper_gtk_version; // 2 or 3
extern int wrapper_gtk_versionDebug;

GtkWidget *
glass_file_chooser_dialog (
                           const gchar * title,
                           GtkWindow * parent,
                           GtkFileChooserAction action,
                           const gchar * action_text
                           );

void
glass_widget_set_visual (GtkWidget *widget, GdkVisual *visual);

gboolean
glass_gdk_pixbuf_save_to_buffer (GdkPixbuf * pixbuf,
                    gchar ** buffer,
                    gsize * buffer_size,
                    const char *type, GError ** error);

gint
glass_gdk_visual_get_depth (GdkVisual * visual);

GdkScreen *
glass_gdk_window_get_screen(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow * gdkWindow, GdkCursor *cursor, gboolean owner_events);

void
glass_gdk_mouse_devices_ungrab();

void
glass_gdk_master_pointer_grab(GdkWindow *window, GdkCursor *cursor);

void
glass_gdk_master_pointer_ungrab();

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

const guchar *
glass_gtk_selection_data_get_data_with_length(
        GtkSelectionData * selectionData,
        gint * length);

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual);

int
glass_gtk_fixup_typed_key(int key, int keyval);

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h);

void
glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y);

void
glass_gdk_x11_display_set_window_scale(GdkDisplay *display, gint scale);

gboolean
glass_configure_window_transparency(GtkWidget *window, gboolean transparent);

GdkPixbuf *
glass_pixbuf_from_window(GdkWindow *window,
    gint srcx, gint srcy,
    gint width, gint height);

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

#endif // __GTK_WRAPPER_H__
