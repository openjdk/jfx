/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_gtkcompat.h"
#include "glass_general.h"
#include <gdk/gdk.h>
#include <gtk/gtk.h>

gboolean disableGrab = FALSE;

static gboolean configure_transparent_window(GtkWidget *window);
static void configure_opaque_window(GtkWidget *window);
static gboolean configure_window_transparency(GtkWidget *window,
                                              gboolean transparent);

#if GTK_CHECK_VERSION(3, 0, 0)
typedef struct _DeviceGrabContext {
    GdkWindow * window;
    gboolean grabbed;
} DeviceGrabContext;

static void grab_mouse_device(GdkDevice *device, DeviceGrabContext *context);
static void ungrab_mouse_device(GdkDevice *device);

GdkScreen * 
glass_gdk_window_get_screen(GdkWindow * gdkWindow) {
    GdkVisual * gdkVisual = gdk_window_get_visual(gdkWindow);
    return gdk_visual_get_screen(gdkVisual);
}

GdkDisplay * glass_gdk_window_get_display(GdkWindow * gdkWindow) {
    return gdk_window_get_display(gdkWindow);
}


gboolean
glass_gdk_mouse_devices_grab(GdkWindow *gdkWindow) {
    if (disableGrab) {
        return TRUE;
    }

    DeviceGrabContext context;
    GList *devices = gdk_device_manager_list_devices(
                         gdk_display_get_device_manager(
                             gdk_display_get_default()),
                             GDK_DEVICE_TYPE_MASTER);

    context.window = gdkWindow;
    context.grabbed = FALSE;
    g_list_foreach(devices, (GFunc) grab_mouse_device, &context);

    return context.grabbed;
}

void
glass_gdk_mouse_devices_ungrab() {
    GList *devices = gdk_device_manager_list_devices(
                         gdk_display_get_device_manager(
                             gdk_display_get_default()),
                             GDK_DEVICE_TYPE_MASTER);
    g_list_foreach(devices, (GFunc) ungrab_mouse_device, NULL);
}

void
glass_gdk_master_pointer_grab(GdkWindow *window, GdkCursor *cursor) {
    if (disableGrab) {
        gdk_window_set_cursor(window, cursor);
        return;
    }
    gdk_device_grab(gdk_device_manager_get_client_pointer(
                        gdk_display_get_device_manager(
                            gdk_display_get_default())),
                    window, GDK_OWNERSHIP_NONE, FALSE, GDK_ALL_EVENTS_MASK,
                    cursor, GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_ungrab() {
    gdk_device_ungrab(gdk_device_manager_get_client_pointer(
                          gdk_display_get_device_manager(
                              gdk_display_get_default())),
                      GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_get_position(gint *x, gint *y) {
    gdk_device_get_position(gdk_device_manager_get_client_pointer(
                                gdk_display_get_device_manager(
                                    gdk_display_get_default())),
                            NULL, x, y);
}

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device) {
    return gdk_display_device_is_grabbed(gdk_display_get_default(), device);
}

void
glass_gdk_device_ungrab(GdkDevice *device) {
    gdk_device_ungrab(device, GDK_CURRENT_TIME);
}

GdkWindow *
glass_gdk_device_get_window_at_position(GdkDevice *device, gint *x, gint *y) {
    return gdk_device_get_window_at_position(device, x, y);
}

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                             gboolean transparent) {
    gboolean isTransparent = configure_window_transparency(window, transparent);
    gtk_widget_realize(window);
    if (isTransparent) {
        GdkRGBA rgba = { 1.0, 1.0, 1.0, 0.0 };
        gdk_window_set_background_rgba(gtk_widget_get_window(window), &rgba);
    }
}

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual) {
    gtk_widget_set_visual(widget, visual);
}

static gboolean
configure_transparent_window(GtkWidget *window) {
    GdkScreen *default_screen = gdk_screen_get_default();
    GdkDisplay *default_display = gdk_display_get_default();
    GdkVisual *visual = gdk_screen_get_rgba_visual(default_screen);
    if (visual
            && gdk_display_supports_composite(default_display)
            && gdk_screen_is_composited(default_screen)) {
        gtk_widget_set_visual(window, visual);
        return TRUE;
    }

    return FALSE;
}

static void
grab_mouse_device(GdkDevice *device, DeviceGrabContext *context) {
    GdkInputSource source = gdk_device_get_source(device);
    if (source == GDK_SOURCE_MOUSE) {
        GdkGrabStatus status = gdk_device_grab(device,
                                               context->window,
                                               GDK_OWNERSHIP_NONE,
                                               TRUE,
                                               GDK_ALL_EVENTS_MASK,
                                               NULL,
                                               GDK_CURRENT_TIME);
        if (status == GDK_GRAB_SUCCESS) {
            context->grabbed = TRUE;
        }
    }    
}

static void
ungrab_mouse_device(GdkDevice *device) {
    GdkInputSource source = gdk_device_get_source(device);
    if (source == GDK_SOURCE_MOUSE) {
        gdk_device_ungrab(device, GDK_CURRENT_TIME);
    }
}

int glass_gtk_fixup_typed_key(int key, int keyval) {
    return key;
}

void glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h) {
    *w = gdk_window_get_width(window);
    *h = gdk_window_get_height(window);
}

void glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y) {
    gdk_device_get_position(gdk_device_manager_get_client_pointer(gdk_display_get_device_manager(display)),
        NULL , x, y);
}

#else /* GTK_CHECK_VERSION(3, 0, 0) */

GdkScreen * 
glass_gdk_window_get_screen(GdkWindow * gdkWindow) {
    return gdk_drawable_get_screen(GDK_DRAWABLE(gdkWindow));
}

GdkDisplay * glass_gdk_window_get_display(GdkWindow * gdkWindow) {
    return gdk_drawable_get_display(GDK_DRAWABLE(gdkWindow));
}

gboolean
glass_gdk_mouse_devices_grab(GdkWindow *gdkWindow) {
    return glass_gdk_mouse_devices_grab_with_cursor(gdkWindow, NULL);
}

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow *gdkWindow, GdkCursor *cursor) {
    if (disableGrab) {
        return TRUE;
    }
    GdkGrabStatus status = gdk_pointer_grab(gdkWindow, TRUE, (GdkEventMask)
                                            (GDK_POINTER_MOTION_MASK
                                                | GDK_POINTER_MOTION_HINT_MASK
                                                | GDK_BUTTON_MOTION_MASK
                                                | GDK_BUTTON1_MOTION_MASK
                                                | GDK_BUTTON2_MOTION_MASK
                                                | GDK_BUTTON3_MOTION_MASK
                                                | GDK_BUTTON_PRESS_MASK
                                                | GDK_BUTTON_RELEASE_MASK),
                                            NULL, cursor, GDK_CURRENT_TIME);

    return (status == GDK_GRAB_SUCCESS) ? TRUE : FALSE;
}

void
glass_gdk_mouse_devices_ungrab() {
    gdk_pointer_ungrab(GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_grab(GdkWindow *window, GdkCursor *cursor) {
    if (disableGrab) {
        gdk_window_set_cursor(window, cursor);
        return;
    }
    gdk_pointer_grab(window, FALSE, (GdkEventMask)
                     (GDK_POINTER_MOTION_MASK
                         | GDK_BUTTON_MOTION_MASK
                         | GDK_BUTTON1_MOTION_MASK
                         | GDK_BUTTON2_MOTION_MASK
                         | GDK_BUTTON3_MOTION_MASK
                         | GDK_BUTTON_RELEASE_MASK),
                     NULL, cursor, GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_ungrab() {
    gdk_pointer_ungrab(GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_get_position(gint *x, gint *y) {
    gdk_display_get_pointer(gdk_display_get_default(), NULL, x, y, NULL);
}

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device) {
    (void) device;
    return gdk_display_pointer_is_grabbed(gdk_display_get_default());
}

void
glass_gdk_device_ungrab(GdkDevice *device) {
    (void) device;
    gdk_pointer_ungrab(GDK_CURRENT_TIME);
}

GdkWindow *
glass_gdk_device_get_window_at_position(GdkDevice *device, gint *x, gint *y) {
    (void) device;
    return gdk_display_get_window_at_pointer(gdk_display_get_default(), x, y);
}

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                             gboolean transparent) {
    configure_window_transparency(window, transparent);
    gtk_widget_realize(window);
}

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual) {
    GdkColormap *colormap = gdk_colormap_new(visual, TRUE);
    gtk_widget_set_colormap(widget, colormap);
}

static gboolean
configure_transparent_window(GtkWidget *window) {
    GdkScreen *default_screen = gdk_screen_get_default();
    GdkDisplay *default_display = gdk_display_get_default();
    GdkColormap *colormap = gdk_screen_get_rgba_colormap(default_screen);
    if (colormap
            && gdk_display_supports_composite(default_display)
            && gdk_screen_is_composited(default_screen)) {
        gtk_widget_set_colormap(window, colormap);
        return TRUE;
    }

    return FALSE;
}

int glass_gtk_fixup_typed_key(int key, int keyval) {
    if (key == 0) {
        // Work around "bug" fixed in gtk-3.0:
        // http://mail.gnome.org/archives/commits-list/2011-March/msg06832.html
        switch (keyval) {
        case 0xFF08 /* Backspace */: return '\b';
        case 0xFF09 /* Tab       */: return '\t';
        case 0xFF0A /* Linefeed  */: return '\n';
        case 0xFF0B /* Vert. Tab */: return '\v';
        case 0xFF0D /* Return    */: return '\r';
        case 0xFF1B /* Escape    */: return '\033';
        case 0xFFFF /* Delete    */: return '\177';
        }
    }
    return key;
}

void glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h) {
    gdk_drawable_get_size(GDK_DRAWABLE(window), w, h);
}

void glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y) {
    gdk_display_get_pointer(display, NULL, x, y, NULL);
}

#endif /* GTK_CHECK_VERSION(3, 0, 0) */

const guchar*
glass_gtk_selection_data_get_data_with_length(
        GtkSelectionData * selectionData,
        gint * length) {
    if (selectionData == NULL) {
        return NULL;
    }

    *length = gtk_selection_data_get_length(selectionData);
    return gtk_selection_data_get_data(selectionData);
}

static void
configure_opaque_window(GtkWidget *window) {
    gtk_widget_set_visual(window,
                          gdk_screen_get_system_visual(
                              gdk_screen_get_default()));
}

static gboolean
configure_window_transparency(GtkWidget *window, gboolean transparent) {
    if (transparent) {
        if (configure_transparent_window(window)) {
            return TRUE;
        }

        ERROR0("Can't create transparent stage, because your screen doesn't"
               " support alpha channel."
               " You need to enable XComposite extension.\n");
    }

    configure_opaque_window(window);
    return FALSE;
}

