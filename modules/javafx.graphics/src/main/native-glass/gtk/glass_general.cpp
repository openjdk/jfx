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
#include "glass_general.h"

#include <gtk/gtk.h>

char const * const GDK_WINDOW_DATA_CONTEXT = "glass_window_context";

// The JNI_OnLoad / JNI_OnLoad_glassgtk3 of commit 033187ad90 cached jclass / jmethodID / jfieldID globals for
// the JNI natives of GtkWindow, GtkView, GtkDnDClipboard and GtkApplication and for the JNI upcalls of the
// window, view, drag-and-drop, screen and application C - all replaced by the ggtk_* functions and callback
// tables of glass_gtk_api.h - and the JavaVM, which screencast_pipewire.c storeRestoreToken asked for a JNIEnv
// and which the store_token slot of screencast_api.h replaced. Nothing reads either any more, so the library
// has no JNI entry point left.

static uint8_t displayValid = 0;

uint8_t
is_display_valid() {
    return displayValid;
}

guint8* convert_BGRA_to_RGBA(const int* pixels, int stride, int height) {
  if (stride <= 0 || height <= 0 || (height > INT_MAX / stride)) {
    return NULL;
  }

  guint8* new_pixels = (guint8*) g_malloc(height * stride);
  if (!new_pixels) {
    return NULL;
  }

  int i = 0;

  for (i = 0; i < height * stride; i += 4) {
      new_pixels[i] = (guint8)(*pixels >> 16);
      new_pixels[i + 1] = (guint8)(*pixels >> 8);
      new_pixels[i + 2] = (guint8)(*pixels);
      new_pixels[i + 3] = (guint8)(*pixels >> 24);
      pixels++;
  }

  return new_pixels;
}


// The returned string should be freed with g_free().
gchar* get_application_name() {
    gchar* ret = NULL;

    if (glass_app_cb.get_application_name) {
        // The slot answers the g_malloc'd modified UTF-8 copy the JNI made with g_strdup; on a throw (both
        // JNI calls were CHECK_JNI_EXCEPTION_RET(NULL) sites) it writes nothing and the answer is NULL.
        char* name = NULL;
        if (glass_app_cb.get_application_name(&name)) {
            return NULL;
        }
        return name;
    }
    return ret; // no slot, no call: no name
}

gsize get_files_count(gchar **uris) {
    if (!uris) {
        return 0;
    }

    guint size = g_strv_length(uris);
    guint files_cnt = 0;

    for (guint i = 0; i < size; ++i) {
        if (g_str_has_prefix(uris[i], FILE_PREFIX)) {
            files_cnt++;
        }
    }
    return files_cnt;
}

//***************************************************************************

typedef struct _DeviceGrabContext {
    GdkWindow * window;
    gboolean grabbed;
} DeviceGrabContext;

gboolean disableGrab = FALSE;
static gboolean configure_transparent_window(GtkWidget *window);
static void configure_opaque_window(GtkWidget *window);

GdkScreen * glass_gdk_window_get_screen(GdkWindow * gdkWindow)
{
#ifdef GLASS_GTK3
        GdkVisual * gdkVisual = gdk_window_get_visual(gdkWindow);
        return gdk_visual_get_screen(gdkVisual);
#else
        return gdk_window_get_screen(gdkWindow);
#endif
}

gboolean
glass_gdk_mouse_devices_grab(GdkWindow *gdkWindow) {
    return glass_gdk_mouse_devices_grab_with_cursor(gdkWindow, NULL, TRUE);
}

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow *gdkWindow, GdkCursor *cursor, gboolean owner_events) {
    if (disableGrab) {
        return TRUE;
    }

    GdkGrabStatus status = gdk_pointer_grab(gdkWindow, owner_events, (GdkEventMask)
                                            (GDK_POINTER_MOTION_MASK
                                                | GDK_POINTER_MOTION_HINT_MASK
                                                | GDK_BUTTON_MOTION_MASK
                                                | GDK_BUTTON1_MOTION_MASK
                                                | GDK_BUTTON2_MOTION_MASK
                                                | GDK_BUTTON3_MOTION_MASK
                                                | GDK_BUTTON_PRESS_MASK
                                                | GDK_BUTTON_RELEASE_MASK
                                                | GDK_TOUCH_MASK),
                                            NULL, cursor, GDK_CURRENT_TIME);

    return (status == GDK_GRAB_SUCCESS) ? TRUE : FALSE;
}

void
glass_gdk_mouse_devices_ungrab() {
    gdk_pointer_ungrab(GDK_CURRENT_TIME);
}

void
glass_gdk_master_pointer_get_position(gint *x, gint *y) {
#ifdef GLASS_GTK3
        gdk_device_get_position(gdk_device_manager_get_client_pointer(
                                    gdk_display_get_device_manager(
                                        gdk_display_get_default())), NULL, x, y);
#else
        gdk_display_get_pointer(gdk_display_get_default(), NULL, x, y, NULL);
#endif
}

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device) {
#ifdef GLASS_GTK3
        return gdk_display_device_is_grabbed(gdk_display_get_default(), device);
#else
        (void) device;
        return gdk_display_pointer_is_grabbed(gdk_display_get_default());
#endif
}

void
glass_gdk_device_ungrab(GdkDevice *device) {
#ifdef GLASS_GTK3
        gdk_device_ungrab(device, GDK_CURRENT_TIME);
#else
        (void) device;
        gdk_pointer_ungrab(GDK_CURRENT_TIME);
#endif
}


GdkWindow *
glass_gdk_device_get_window_at_position(GdkDevice *device, gint *x, gint *y) {
#ifdef GLASS_GTK3
        return gdk_device_get_window_at_position(device, x, y);
#else
        (void) device;
        return gdk_display_get_window_at_pointer(gdk_display_get_default(), x, y);
#endif
}

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                             gboolean transparent) {
        gboolean isTransparent = glass_configure_window_transparency(window, transparent);
        gtk_widget_realize(window);
}

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual) {
    glass_widget_set_visual(widget, visual);
}

static gboolean
configure_transparent_window(GtkWidget *window) {
    GdkScreen *default_screen = gdk_screen_get_default();
    GdkDisplay *default_display = gdk_display_get_default();

#ifdef GLASS_GTK3
        GdkVisual *visual = gdk_screen_get_rgba_visual(default_screen);
        if (visual
                && gdk_display_supports_composite(default_display)
                && gdk_screen_is_composited(default_screen)) {
            glass_widget_set_visual(window, visual);
            return TRUE;
        }
#else
        GdkColormap *colormap = gdk_screen_get_rgba_colormap(default_screen);
        if (colormap
                && gdk_display_supports_composite(default_display)
                && gdk_screen_is_composited(default_screen)) {
            gtk_widget_set_colormap(window, colormap);
            return TRUE;
        }
#endif

    return FALSE;
}

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h) {
    *w = gdk_window_get_width(window);
    *h = gdk_window_get_height(window);
}


static void
configure_opaque_window(GtkWidget *window) {
    (void) window;
/* We need to pick a visual that really is glx compatible
 * instead of using the default visual
 */
 /* see: JDK-8087516 for why this is commented out
    glass_widget_set_visual(window,
                          gdk_screen_get_system_visual(
                              gdk_screen_get_default()));
  */
}

gboolean
glass_configure_window_transparency(GtkWidget *window, gboolean transparent) {
    if (transparent) {
        if (configure_transparent_window(window)) {
            return TRUE;
        }

        fprintf(stderr,"Can't create transparent stage, because your screen doesn't"
               " support alpha channel."
               " You need to enable XComposite extension.\n");
        fflush(stderr);
    }

    configure_opaque_window(window);
    return FALSE;
}

void
glass_window_apply_shape_mask(GdkWindow *window,
    void* data, uint width, uint height)
{
#ifdef GLASS_GTK3
    (void) window;
    (void) data;
    (void) width;
    (void) height;
#else
        GdkPixbuf* pixbuf = gdk_pixbuf_new_from_data((guchar *) data,
                GDK_COLORSPACE_RGB, TRUE, 8, width, height, width * 4, NULL, NULL);

        if (GDK_IS_PIXBUF(pixbuf)) {
            GdkBitmap* mask = NULL;
            gdk_pixbuf_render_pixmap_and_mask(pixbuf, NULL, &mask, 128);

            gdk_window_input_shape_combine_mask(window, mask, 0, 0);

            g_object_unref(pixbuf);
            if (mask) {
                g_object_unref(mask);
            }
        }
#endif
}

void
glass_window_reset_input_shape_mask(GdkWindow *window)
{
#ifdef GLASS_GTK3
        gdk_window_input_shape_combine_region(window, NULL, 0, 0);
#else
        gdk_window_input_shape_combine_mask(window, NULL, 0, 0);
#endif
}

GdkWindow *
glass_gdk_drag_context_get_dest_window (GdkDragContext * context)
{
    return ((context != NULL) ? gdk_drag_context_get_dest_window(context) : NULL);
}


void glass_gdk_x11_display_set_window_scale (GdkDisplay *display,
                          gint scale)
{
#ifdef GLASS_GTK3
    // Optional call, if it does not exist then GTK3 is not yet
    // doing automatic scaling of coordinates so we do not need
    // to override it.
    wrapped_gdk_x11_display_set_window_scale(display, scale);
#else
    (void) display;
    (void) scale;
#endif
}

//-------- Glass utility ----------------------------------------

void
glass_widget_set_visual(GtkWidget *widget, GdkVisual *visual)
{
#ifdef GLASS_GTK3
        gtk_widget_set_visual (widget, visual);
#else
        GdkColormap *colormap = gdk_colormap_new(visual, TRUE);
        gtk_widget_set_colormap (widget, colormap);
#endif
}

guint glass_settings_get_guint_opt (const gchar *schema_name,
                    const gchar *key_name,
                    int defval)
{
    GSettingsSchemaSource *default_schema_source =
            wrapped_g_settings_schema_source_get_default();
    if (default_schema_source == NULL) {
        if (gtk_verbose) {
            fprintf(stderr, "No schema source dir found!\n");
        }
        return defval;
    }
    GSettingsSchema *the_schema =
            wrapped_g_settings_schema_source_lookup(default_schema_source, schema_name, TRUE);
    if (the_schema == NULL) {
        if (gtk_verbose) {
            fprintf(stderr, "schema '%s' not found!\n", schema_name);
        }
        return defval;
    }
    if (!wrapped_g_settings_schema_has_key(the_schema, key_name)) {
        if (gtk_verbose) {
            fprintf(stderr, "key '%s' not found in schema '%s'!\n", key_name, schema_name);
        }
        return defval;
    }
    if (gtk_verbose) {
        fprintf(stderr, "found schema '%s' and key '%s'\n", schema_name, key_name);
    }

    GSettings *gset = g_settings_new(schema_name);

    wrapped_g_settings_schema_unref(the_schema);

    return g_settings_get_uint(gset, key_name);
}
