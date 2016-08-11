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

#include <stdio.h>
#include <stdlib.h>
#include <linux/fb.h>
#include <fcntl.h>
#ifndef __USE_GNU       // required for dladdr() & Dl_info
#define __USE_GNU
#endif
#include <dlfcn.h>
#include <sys/ioctl.h>

#include <string.h>
#include <strings.h>

#include <assert.h>

#include <gdk/gdk.h>
#include <gdk/gdkx.h>

#include "glass_wrapper.h"

static GdkAtom (*_gdk_atom_intern) (const gchar * atom_name,
                    gboolean only_if_exists);
static GdkAtom (*_gdk_atom_intern_static_string) (const gchar * atom_name);
static gchar *(*_gdk_atom_name) (GdkAtom atom);
static cairo_t *(*_gdk_cairo_create) (GdkDrawable * drawable);
static GdkColormap *(*_gdk_colormap_new) (GdkVisual * visual,
                      gboolean allocate);
static GdkCursor *(*_gdk_cursor_new) (GdkCursorType cursor_type);
static GdkCursor *(*_gdk_cursor_new_from_name) (GdkDisplay * display,
                        const gchar * name);
static GdkCursor *(*_gdk_cursor_new_from_pixbuf) (GdkDisplay * display,
                          GdkPixbuf * pixbuf,
                          gint x, gint y);
static GdkDisplay *(*_gdk_display_get_default) (void);
static guint (*_gdk_display_get_default_cursor_size) (GdkDisplay * display);
static void (*_gdk_display_get_pointer) (GdkDisplay * display,
                     GdkScreen ** screen,
                     gint * x,
                     gint * y, GdkModifierType * mask);
static GdkWindow *(*_gdk_display_get_window_at_pointer) (GdkDisplay * display,
                             gint * win_x,
                             gint * win_y);
static gboolean (*_gdk_display_pointer_is_grabbed) (GdkDisplay * display);
static gboolean (*_gdk_display_supports_composite) (GdkDisplay * display);
static void (*_gdk_drag_abort) (GdkDragContext * context, guint32 time_);
static gboolean (*_gdk_drag_motion) (GdkDragContext * context,
                     GdkWindow * dest_window,
                     GdkDragProtocol protocol,
                     gint x_root,
                     gint y_root,
                     GdkDragAction suggested_action,
                     GdkDragAction possible_actions,
                     guint32 time_);
static void (*_gdk_drag_drop) (GdkDragContext * context, guint32 time_);
static GdkDragContext *(*_gdk_drag_begin) (GdkWindow * window,
                       GList * targets);
static GdkDragAction (*_gdk_drag_context_get_actions) (GdkDragContext *
                               context);
static GdkDragAction (*_gdk_drag_context_get_selected_action) (GdkDragContext
                                   * context);
static GdkDragAction (*_gdk_drag_context_get_suggested_action) (GdkDragContext
                                * context);
static GList *(*_gdk_drag_context_list_targets) (GdkDragContext * context);
static void (*_gdk_drag_find_window_for_screen) (GdkDragContext * context,
                         GdkWindow * drag_window,
                         GdkScreen * screen,
                         gint x_root,
                         gint y_root,
                         GdkWindow ** dest_window,
                         GdkDragProtocol * protocol);
static GdkAtom (*_gdk_drag_get_selection) (GdkDragContext * context);
static GdkWindow *(*_gdk_drag_context_get_dest_window) (GdkDragContext *
                            context);
static void (*_gdk_drag_status) (GdkDragContext * context,
                 GdkDragAction action, guint32 time_);
static void (*_gdk_drop_reply) (GdkDragContext * context, gboolean ok,
                guint32 time_);
static void (*_gdk_drop_finish) (GdkDragContext * context, gboolean success,
                 guint32 time_);
static GdkScreen *(*_gdk_window_get_screen) (GdkWindow * window);
static GdkDisplay *(*_gdk_window_get_display) (GdkWindow * window);
static int (*_gdk_window_get_width) (GdkWindow * window);
static int (*_gdk_window_get_height) (GdkWindow * window);
static void (*_gdk_error_trap_push) (void);
static void (*_gdk_event_request_motions) (const GdkEventMotion * event);
static void (*_gdk_event_handler_set) (GdkEventFunc func,
                       gpointer data, GDestroyNotify notify);
static GdkWindow *(*_gdk_get_default_root_window) (void);
static GdkKeymap *(*_gdk_keymap_get_default) (void);
static gboolean (*_gdk_keymap_get_entries_for_keyval) (GdkKeymap * keymap,
                               guint keyval,
                               GdkKeymapKey ** keys,
                               gint * n_keys);
static guint (*_gdk_keymap_lookup_key) (GdkKeymap * keymap,
                    const GdkKeymapKey * key);
static gboolean (*_gdk_keymap_translate_keyboard_state) (GdkKeymap * keymap,
                             guint
                             hardware_keycode,
                             GdkModifierType
                             state, gint group,
                             guint * keyval,
                             gint *
                             effective_group,
                             gint * level,
                             GdkModifierType *
                             consumed_modifiers);
static guint32 (*_gdk_keyval_to_unicode) (guint keyval) G_GNUC_CONST;
static GdkPixbuf *(*_gdk_pixbuf_get_from_drawable) (GdkPixbuf * dest,
                            GdkDrawable * src,
                            GdkColormap * cmap,
                            int src_x,
                            int src_y,
                            int dest_x,
                            int dest_y,
                            int width, int height);
static void (*_gdk_pixbuf_render_pixmap_and_mask) (GdkPixbuf * pixbuf,
                           GdkPixmap ** pixmap_return,
                           GdkBitmap ** mask_return,
                           int alpha_threshold);
static void (*_gdk_pixbuf_render_pixmap_and_mask_for_colormap) (GdkPixbuf *
                                pixbuf,
                                GdkColormap *
                                colormap,
                                GdkPixmap **
                                pixmap_return,
                                GdkBitmap **
                                mask_return,
                                int
                                alpha_threshold);
static GdkGrabStatus (*_gdk_pointer_grab) (GdkWindow * window,
                       gboolean owner_events,
                       GdkEventMask event_mask,
                       GdkWindow * confine_to,
                       GdkCursor * cursor, guint32 time_);
static void (*_gdk_pointer_ungrab) (guint32 time_);
static void (*_gdk_property_change) (GdkWindow * window,
                     GdkAtom property,
                     GdkAtom type,
                     gint format,
                     GdkPropMode mode,
                     const guchar * data, gint nelements);
static gboolean (*_gdk_property_get) (GdkWindow * window,
                      GdkAtom property,
                      GdkAtom type,
                      gulong offset,
                      gulong length,
                      gint pdelete,
                      GdkAtom * actual_property_type,
                      gint * actual_format,
                      gint * actual_length, guchar ** data);
static gboolean (*_gdk_rectangle_intersect) (const GdkRectangle * src1,
                         const GdkRectangle * src2,
                         GdkRectangle * dest);
static void (*_gdk_region_destroy) (GdkRegion * region);
static GdkRegion *(*_gdk_region_new) (void);
static GdkScreen *(*_gdk_screen_get_default) (void);
static gint (*_gdk_screen_get_height) (GdkScreen * screen);
static gint (*_gdk_screen_get_monitor_at_point) (GdkScreen * screen,
                         gint x, gint y);
static void (*_gdk_screen_get_monitor_geometry) (GdkScreen * screen,
                         gint monitor_num,
                         GdkRectangle * dest);
static gint (*_gdk_screen_get_n_monitors) (GdkScreen * screen);
static gint (*_gdk_screen_get_monitor_width_mm) (GdkScreen *screen,
                         gint monitor_num);
static gint (*_gdk_screen_get_monitor_height_mm) (GdkScreen *screen,
                         gint monitor_num);
static gint (*_gdk_screen_get_width_mm) (GdkScreen *screen);
static gint (*_gdk_screen_get_height_mm) (GdkScreen *screen);
static gdouble (*_gdk_screen_get_resolution) (GdkScreen * screen);
static GdkColormap *(*_gdk_screen_get_rgba_colormap) (GdkScreen * screen);
static GdkColormap *(*_gdk_screen_get_rgb_colormap) (GdkScreen * screen);
static GdkWindow *(*_gdk_screen_get_root_window) (GdkScreen * screen);
static GdkVisual *(*_gdk_screen_get_system_visual) (GdkScreen * screen);
static gint (*_gdk_screen_get_width) (GdkScreen * screen);
static gboolean (*_gdk_screen_is_composited) (GdkScreen * screen);
static void (*_gdk_selection_convert) (GdkWindow * requestor,
                       GdkAtom selection,
                       GdkAtom target, guint32 time_);
static gboolean (*_gdk_selection_owner_set) (GdkWindow * owner,
                         GdkAtom selection,
                         guint32 time_,
                         gboolean send_event);
static gint (*_gdk_selection_property_get) (GdkWindow * requestor,
                        guchar ** data,
                        GdkAtom * prop_type,
                        gint * prop_format);
static void (*_gdk_selection_send_notify) (GdkNativeWindow requestor,
                       GdkAtom selection,
                       GdkAtom target,
                       GdkAtom property, guint32 time_);
static guint (*_gdk_unicode_to_keyval) (guint32 wc) G_GNUC_CONST;
static guint (*_gdk_threads_add_idle_full) (gint priority,
                        GSourceFunc function,
                        gpointer data,
                        GDestroyNotify notify);
static guint (*_gdk_threads_add_idle) (GSourceFunc function, gpointer data);
static guint (*_gdk_threads_add_timeout_full) (gint priority,
                           guint interval,
                           GSourceFunc function,
                           gpointer data,
                           GDestroyNotify notify);
static void (*_gdk_threads_enter) (void);
static void (*_gdk_threads_init) (void);
static void (*_gdk_threads_leave) (void);
static void (*_gdk_window_destroy) (GdkWindow * window);
static GdkCursor *(*_gdk_window_get_cursor) (GdkWindow * window);
static GdkEventMask (*_gdk_window_get_events) (GdkWindow * window);
static void (*_gdk_window_get_geometry) (GdkWindow * window,
                     gint * x,
                     gint * y,
                     gint * width,
                     gint * height, gint * depth);
static gint (*_gdk_window_get_origin) (GdkWindow * window,
                       gint * x, gint * y);
static void (*_gdk_window_input_shape_combine_mask) (GdkWindow * window,
                             GdkBitmap * mask,
                             gint x, gint y);
static void (*_gdk_window_shape_combine_region) (GdkWindow *window,
                     const cairo_region_t *shape_region,
                      gint offset_x,
                      gint offset_y);
static void (*_gdk_window_input_shape_combine_region) (GdkWindow * window,
                               const cairo_region_t * shape_region,
                               gint offset_x,
                               gint offset_y);
static gboolean (*_gdk_window_is_destroyed) (GdkWindow * window);
static void (*_gdk_window_move) (GdkWindow * window, gint x, gint y);
static GdkWindow *(*_gdk_window_new) (GdkWindow * parent,
                      GdkWindowAttr * attributes,
                      gint attributes_mask);
static void (*_gdk_window_register_dnd) (GdkWindow * window);
static void (*_gdk_window_resize) (GdkWindow * window,
                   gint width, gint height);
static void (*_gdk_window_restack) (GdkWindow * window,
                    GdkWindow * sibling, gboolean above);
static void (*_gdk_window_set_cursor) (GdkWindow * window,
                       GdkCursor * cursor);
static void (*_gdk_window_set_events) (GdkWindow * window,
                       GdkEventMask event_mask);
static void (*_gdk_window_set_functions) (GdkWindow * window,
                      GdkWMFunction functions);
static void (*_gdk_window_show) (GdkWindow * window);
static Display *(*_gdk_x11_display_get_xdisplay) (GdkDisplay * display);
static void (*_gdk_x11_display_set_window_scale) (GdkDisplay *display,
                                  gint scale);
static XID (*_gdk_x11_drawable_get_xid) (GdkDrawable * drawable);
static gint (*_gdk_x11_get_default_screen) (void);
static Display *(*_gdk_x11_get_default_xdisplay) (void);
static guint32 (*_gdk_x11_get_server_time) (GdkWindow * window);
static GdkVisual *(*_gdk_x11_screen_lookup_visual) (GdkScreen * screen,
                            VisualID xvisualid);
static GdkWindow *(*_gdk_x11_window_foreign_new_for_display) (GdkDisplay *
                                  display,
                                  Window window);
static GdkWindow *(*_gdk_x11_window_lookup_for_display) (GdkDisplay * display,
                             Window window);
static gint (*_gdk_visual_get_depth) (GdkVisual * visual);

static GType (*_gdk_window_object_get_type) (void);

//----------- GTK 3.0 ------------------------------------------------------

typedef struct _GdkDeviceManager      GdkDeviceManager;
struct _GdkRGBA
{
  gdouble red;
  gdouble green;
  gdouble blue;
  gdouble alpha;
};
typedef struct _GdkRGBA               GdkRGBA;

typedef enum {
  GDK_DEVICE_TYPE_MASTER,
  GDK_DEVICE_TYPE_SLAVE,
  GDK_DEVICE_TYPE_FLOATING
} GdkDeviceType;

typedef enum
{
  GDK_OWNERSHIP_NONE,
  GDK_OWNERSHIP_WINDOW,
  GDK_OWNERSHIP_APPLICATION
} GdkGrabOwnership;


static GdkVisual *   (*_gdk_window_get_visual) (GdkWindow     *window);
static GdkScreen    *(*_gdk_visual_get_screen) (GdkVisual *visual);
static GList * (*_gdk_device_manager_list_devices) (GdkDeviceManager *device_manager,
                                 GdkDeviceType type);
static GdkDeviceManager * (*_gdk_display_get_device_manager) (GdkDisplay *display);
static GdkVisual *  (*_gdk_screen_get_rgba_visual) (GdkScreen   *screen);
static GdkInputSource (*_gdk_device_get_source) (GdkDevice      *device);
static GdkGrabStatus (*_gdk_device_grab) (GdkDevice        *device,
                                      GdkWindow        *window,
                                      GdkGrabOwnership  grab_ownership,
                                      gboolean          owner_events,
                                      GdkEventMask      event_mask,
                                      GdkCursor        *cursor,
                                      guint32           time_);
static void (*_gdk_device_ungrab) (GdkDevice *device, guint32 time_);
static GdkDevice * (*_gdk_device_manager_get_client_pointer) (GdkDeviceManager *device_manager);
static void  (*_gdk_device_get_position) (GdkDevice         *device,
                                  GdkScreen        **screen,
                                  gint              *x,
                                  gint              *y);
static gboolean    (*_gdk_display_device_is_grabbed) (GdkDisplay  *display,
                                            GdkDevice   *device);
static GdkWindow * (*_gdk_device_get_window_at_position) (GdkDevice         *device,
                                  gint              *win_x,
                                  gint              *win_y);
static void (*_gdk_window_set_background) (GdkWindow      *window,
                      const GdkColor  *color);
static void (*_gdk_window_set_background_rgba) (GdkWindow     *window,
                                              const GdkRGBA *rgba);
static Window   (*_gdk_x11_window_get_xid) (GdkWindow   *window);

static GdkPixbuf *(*_gdk_pixbuf_get_from_window) (GdkWindow       *window,
                                        gint             src_x,
                                        gint             src_y,
                                        gint             width,
                                        gint             height);

static GType (*_gdk_window_get_type) (void);

static cairo_region_t * (*_gdk_cairo_region_create_from_surface) (cairo_surface_t *surface);

/***** Utilities ***********************************************************/


#define PRELOAD_SYMBOL_GDK(x) \
    _##x = dlsym(libgdk, #x); \
    if (_##x == NULL) { \
        symbol_load_errors++; \
        fprintf(stderr,"failed loading %s\n", #x); \
    }

#define PRELOAD_SYMBOL_GDK_OPT(x) \
    _##x = dlsym(libgdk, #x); \
    if (wrapper_debug && _##x == NULL) { \
        symbol_load_missing++; \
        fprintf(stderr, "missing optional %s\n", #x); \
    }

int wrapper_load_symbols_gdk (int version, void * libgdk)
{
    int symbol_load_missing = 0;
    int symbol_load_errors = 0;

    PRELOAD_SYMBOL_GDK (gdk_atom_intern);
    PRELOAD_SYMBOL_GDK (gdk_atom_intern_static_string);
    PRELOAD_SYMBOL_GDK (gdk_atom_name);
    PRELOAD_SYMBOL_GDK (gdk_cairo_create);
    PRELOAD_SYMBOL_GDK (gdk_cursor_new);
    PRELOAD_SYMBOL_GDK (gdk_cursor_new_from_name);
    PRELOAD_SYMBOL_GDK (gdk_cursor_new_from_pixbuf);
    PRELOAD_SYMBOL_GDK (gdk_display_get_default);
    PRELOAD_SYMBOL_GDK (gdk_display_get_default_cursor_size);
    PRELOAD_SYMBOL_GDK (gdk_display_get_pointer);
    PRELOAD_SYMBOL_GDK (gdk_display_get_window_at_pointer);
    PRELOAD_SYMBOL_GDK (gdk_display_pointer_is_grabbed);
    PRELOAD_SYMBOL_GDK (gdk_display_supports_composite);
    PRELOAD_SYMBOL_GDK (gdk_drag_abort);
    PRELOAD_SYMBOL_GDK (gdk_drag_motion);
    PRELOAD_SYMBOL_GDK (gdk_drag_drop);
    PRELOAD_SYMBOL_GDK (gdk_drag_begin);
    PRELOAD_SYMBOL_GDK (gdk_drag_context_get_actions);
    PRELOAD_SYMBOL_GDK (gdk_drag_context_get_selected_action);
    PRELOAD_SYMBOL_GDK (gdk_drag_context_get_suggested_action);
    PRELOAD_SYMBOL_GDK (gdk_drag_context_list_targets);
    PRELOAD_SYMBOL_GDK (gdk_drag_find_window_for_screen);
    PRELOAD_SYMBOL_GDK (gdk_drag_get_selection);
    PRELOAD_SYMBOL_GDK (gdk_drag_context_get_dest_window);
    PRELOAD_SYMBOL_GDK (gdk_drag_status);
    PRELOAD_SYMBOL_GDK (gdk_drop_reply);
    PRELOAD_SYMBOL_GDK (gdk_drop_finish);
    PRELOAD_SYMBOL_GDK (gdk_error_trap_push);
    PRELOAD_SYMBOL_GDK (gdk_event_request_motions);
    PRELOAD_SYMBOL_GDK (gdk_event_handler_set);
    PRELOAD_SYMBOL_GDK (gdk_get_default_root_window);
    PRELOAD_SYMBOL_GDK (gdk_keymap_get_default);
    PRELOAD_SYMBOL_GDK (gdk_keymap_get_entries_for_keyval);
    PRELOAD_SYMBOL_GDK (gdk_keymap_lookup_key);
    PRELOAD_SYMBOL_GDK (gdk_keymap_translate_keyboard_state);
    PRELOAD_SYMBOL_GDK (gdk_keyval_to_unicode);
    PRELOAD_SYMBOL_GDK (gdk_pointer_grab);
    PRELOAD_SYMBOL_GDK (gdk_pointer_ungrab);
    PRELOAD_SYMBOL_GDK (gdk_property_change);
    PRELOAD_SYMBOL_GDK (gdk_property_get);
    PRELOAD_SYMBOL_GDK (gdk_rectangle_intersect);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_default);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_height);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_monitor_at_point);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_monitor_geometry);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_n_monitors);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_monitor_width_mm);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_monitor_height_mm);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_width_mm);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_height_mm);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_resolution);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_root_window);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_system_visual);
    PRELOAD_SYMBOL_GDK (gdk_screen_get_width);
    PRELOAD_SYMBOL_GDK (gdk_screen_is_composited);
    PRELOAD_SYMBOL_GDK (gdk_selection_convert);
    PRELOAD_SYMBOL_GDK (gdk_selection_owner_set);
    PRELOAD_SYMBOL_GDK (gdk_selection_property_get);
    PRELOAD_SYMBOL_GDK (gdk_selection_send_notify);
    PRELOAD_SYMBOL_GDK (gdk_unicode_to_keyval);
    PRELOAD_SYMBOL_GDK (gdk_threads_add_idle_full);
    PRELOAD_SYMBOL_GDK (gdk_threads_add_idle);
    PRELOAD_SYMBOL_GDK (gdk_threads_add_timeout_full);
    PRELOAD_SYMBOL_GDK (gdk_threads_enter);
    PRELOAD_SYMBOL_GDK (gdk_threads_init);
    PRELOAD_SYMBOL_GDK (gdk_threads_leave);
    PRELOAD_SYMBOL_GDK (gdk_window_destroy);
    PRELOAD_SYMBOL_GDK (gdk_window_get_cursor);
    PRELOAD_SYMBOL_GDK (gdk_window_get_events);
    PRELOAD_SYMBOL_GDK (gdk_window_get_geometry);
    PRELOAD_SYMBOL_GDK (gdk_window_get_origin);
    PRELOAD_SYMBOL_GDK (gdk_window_is_destroyed);
    PRELOAD_SYMBOL_GDK (gdk_window_move);
    PRELOAD_SYMBOL_GDK (gdk_window_new);
    PRELOAD_SYMBOL_GDK (gdk_window_register_dnd);
    PRELOAD_SYMBOL_GDK (gdk_window_resize);
    PRELOAD_SYMBOL_GDK (gdk_window_restack);
    PRELOAD_SYMBOL_GDK (gdk_window_set_cursor);
    PRELOAD_SYMBOL_GDK (gdk_window_set_events);
    PRELOAD_SYMBOL_GDK (gdk_window_set_functions);
    PRELOAD_SYMBOL_GDK (gdk_window_show);
    PRELOAD_SYMBOL_GDK (gdk_x11_display_get_xdisplay);
    PRELOAD_SYMBOL_GDK (gdk_x11_get_default_screen);
    PRELOAD_SYMBOL_GDK (gdk_x11_get_default_xdisplay);
    PRELOAD_SYMBOL_GDK (gdk_x11_get_server_time);
    PRELOAD_SYMBOL_GDK (gdk_x11_screen_lookup_visual);
    PRELOAD_SYMBOL_GDK (gdk_x11_window_foreign_new_for_display);
    PRELOAD_SYMBOL_GDK (gdk_x11_window_lookup_for_display);
    PRELOAD_SYMBOL_GDK (gdk_window_get_display);
    PRELOAD_SYMBOL_GDK (gdk_window_get_height);
    PRELOAD_SYMBOL_GDK (gdk_window_get_width);
    PRELOAD_SYMBOL_GDK (gdk_window_get_screen);
    PRELOAD_SYMBOL_GDK (gdk_visual_get_screen); // 2.2

    if (version == 2) {
        PRELOAD_SYMBOL_GDK (gdk_colormap_new);
        PRELOAD_SYMBOL_GDK (gdk_pixbuf_get_from_drawable);
        PRELOAD_SYMBOL_GDK (gdk_pixbuf_render_pixmap_and_mask);
        PRELOAD_SYMBOL_GDK (gdk_pixbuf_render_pixmap_and_mask_for_colormap);
        PRELOAD_SYMBOL_GDK (gdk_region_destroy);
        PRELOAD_SYMBOL_GDK (gdk_region_new);
        PRELOAD_SYMBOL_GDK (gdk_screen_get_rgba_colormap);
        PRELOAD_SYMBOL_GDK (gdk_screen_get_rgb_colormap);
        PRELOAD_SYMBOL_GDK (gdk_window_input_shape_combine_mask);
        PRELOAD_SYMBOL_GDK (gdk_x11_drawable_get_xid);
        PRELOAD_SYMBOL_GDK (gdk_window_object_get_type);
        PRELOAD_SYMBOL_GDK (gdk_visual_get_depth);
    }

    if (version == 3) {
        // gtk version 3 unique symbols
        PRELOAD_SYMBOL_GDK (gdk_window_get_visual);  // both
        PRELOAD_SYMBOL_GDK (gdk_device_manager_list_devices); //both
        PRELOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        PRELOAD_SYMBOL_GDK (gdk_screen_get_rgba_visual);
        PRELOAD_SYMBOL_GDK (gdk_device_get_source); // both
        PRELOAD_SYMBOL_GDK (gdk_device_grab);
        PRELOAD_SYMBOL_GDK (gdk_device_ungrab);
        PRELOAD_SYMBOL_GDK (gdk_device_manager_get_client_pointer)
        PRELOAD_SYMBOL_GDK (gdk_device_get_position)
        PRELOAD_SYMBOL_GDK (gdk_display_device_is_grabbed)
        PRELOAD_SYMBOL_GDK (gdk_device_get_window_at_position)
        PRELOAD_SYMBOL_GDK (gdk_window_set_background_rgba)
        PRELOAD_SYMBOL_GDK (gdk_x11_window_get_xid);
        PRELOAD_SYMBOL_GDK (gdk_pixbuf_get_from_window);
        PRELOAD_SYMBOL_GDK (gdk_window_get_type);
        PRELOAD_SYMBOL_GDK (gdk_cairo_region_create_from_surface);
        PRELOAD_SYMBOL_GDK (gdk_window_shape_combine_region);
        PRELOAD_SYMBOL_GDK (gdk_window_input_shape_combine_region);
        PRELOAD_SYMBOL_GDK_OPT (gdk_x11_display_set_window_scale);
    }

    if (symbol_load_errors && wrapper_debug) {
        fprintf (stderr, "failed to load %d required gdk symbols\n", symbol_load_errors);
    }

    if (symbol_load_missing && wrapper_debug) {
        fprintf (stderr, "missing %d optional gdk symbols\n", symbol_load_missing);
    }

    return symbol_load_errors;

}

#define CHECK_LOAD_SYMBOL_GDK(x) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr,"missing %s\n", #x); \
            assert(_##x); \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr,"using %s\n", #x); \
               fflush(stderr); \
            } \
        } \
    }

#define CHECK_LOAD_SYMBOL_GDK_OPT(x) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr,"missing optional %s\n", #x); \
            return; \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr,"using %s\n", #x); \
               fflush(stderr); \
            } \
        } \
    }


GdkAtom gdk_atom_intern (const gchar * atom_name, gboolean only_if_exists)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_atom_intern);
    return (*_gdk_atom_intern) (atom_name, only_if_exists);
}

GdkAtom gdk_atom_intern_static_string (const gchar * atom_name)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_atom_intern_static_string);
    return (*_gdk_atom_intern_static_string) (atom_name);
}

gchar *gdk_atom_name (GdkAtom atom)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_atom_name);
    return (*_gdk_atom_name) (atom);
}

cairo_t *gdk_cairo_create (GdkDrawable * drawable)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_cairo_create);
    return (*_gdk_cairo_create) (drawable);
}

GdkColormap *gdk_colormap_new (GdkVisual * visual, gboolean allocate)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_colormap_new);
    return (*_gdk_colormap_new) (visual, allocate);
}

GdkCursor *gdk_cursor_new (GdkCursorType cursor_type)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_cursor_new);
    return (*_gdk_cursor_new) (cursor_type);
}

GdkCursor *gdk_cursor_new_from_name (GdkDisplay * display, const gchar * name)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_cursor_new_from_name);
    return (*_gdk_cursor_new_from_name) (display, name);
}

GdkCursor *gdk_cursor_new_from_pixbuf (GdkDisplay * display,
                       GdkPixbuf * pixbuf, gint x, gint y)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_cursor_new_from_pixbuf);
    return (*_gdk_cursor_new_from_pixbuf) (display, pixbuf, x, y);
}

GdkDisplay *gdk_display_get_default (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);
    return (*_gdk_display_get_default) ();
}

guint gdk_display_get_default_cursor_size (GdkDisplay * display)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default_cursor_size);
    return (*_gdk_display_get_default_cursor_size) (display);
}

void gdk_display_get_pointer (GdkDisplay * display,
                  GdkScreen ** screen,
                  gint * x, gint * y, GdkModifierType * mask)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_get_pointer);
    (*_gdk_display_get_pointer) (display, screen, x, y, mask);
}

GdkWindow *gdk_display_get_window_at_pointer (GdkDisplay * display,
                          gint * win_x, gint * win_y)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_get_window_at_pointer);
    return (*_gdk_display_get_window_at_pointer) (display, win_x, win_y);
}

gboolean gdk_display_pointer_is_grabbed (GdkDisplay * display)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_pointer_is_grabbed);
    return (*_gdk_display_pointer_is_grabbed) (display);
}

gboolean gdk_display_supports_composite (GdkDisplay * display)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_display_supports_composite);
    return (*_gdk_display_supports_composite) (display);
}

void gdk_drag_abort (GdkDragContext * context, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_abort);
    (*_gdk_drag_abort) (context, time_);
}

gboolean gdk_drag_motion (GdkDragContext * context,
              GdkWindow * dest_window,
              GdkDragProtocol protocol,
              gint x_root,
              gint y_root,
              GdkDragAction suggested_action,
              GdkDragAction possible_actions, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_motion);
    return (*_gdk_drag_motion) (context, dest_window, protocol, x_root,
                y_root, suggested_action, possible_actions,
                time_);
}

void gdk_drag_drop (GdkDragContext * context, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_drop);
    (*_gdk_drag_drop) (context, time_);
}

GdkDragContext *gdk_drag_begin (GdkWindow * window, GList * targets)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_begin);
    return (*_gdk_drag_begin) (window, targets);
}

GdkDragAction gdk_drag_context_get_actions (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_get_actions);
    return (*_gdk_drag_context_get_actions) (context);
}

GdkDragAction gdk_drag_context_get_selected_action (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_get_selected_action);
    return (*_gdk_drag_context_get_selected_action) (context);
}

GdkDragAction gdk_drag_context_get_suggested_action (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_get_suggested_action);
    return (*_gdk_drag_context_get_suggested_action) (context);
}

GList *gdk_drag_context_list_targets (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_list_targets);
    return (*_gdk_drag_context_list_targets) (context);
}

void gdk_drag_find_window_for_screen (GdkDragContext * context,
                      GdkWindow * drag_window,
                      GdkScreen * screen,
                      gint x_root,
                      gint y_root,
                      GdkWindow ** dest_window,
                      GdkDragProtocol * protocol)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_find_window_for_screen);
    (*_gdk_drag_find_window_for_screen) (context, drag_window, screen, x_root,
                     y_root, dest_window, protocol);
}

GdkAtom gdk_drag_get_selection (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_get_selection);
    return (*_gdk_drag_get_selection) (context);
}

GdkWindow *gdk_drag_context_get_dest_window (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_get_dest_window);
    return (*_gdk_drag_context_get_dest_window) (context);
}

void gdk_drag_status (GdkDragContext * context,
              GdkDragAction action, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_status);
    (*_gdk_drag_status) (context, action, time_);
}

void gdk_drop_reply (GdkDragContext * context, gboolean ok, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drop_reply);
    (*_gdk_drop_reply) (context, ok, time_);
}

void gdk_drop_finish (GdkDragContext * context,
              gboolean success, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drop_finish);
    (*_gdk_drop_finish) (context, success, time_);
}

void gdk_error_trap_push (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_error_trap_push);
    return (*_gdk_error_trap_push) ();
}

void gdk_event_request_motions (const GdkEventMotion * event)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_event_request_motions);
    (*_gdk_event_request_motions) (event);
}

void gdk_event_handler_set (GdkEventFunc func,
                gpointer data, GDestroyNotify notify)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_event_handler_set);
    (*_gdk_event_handler_set) (func, data, notify);
}

GdkWindow *gdk_get_default_root_window (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_get_default_root_window);
    return (*_gdk_get_default_root_window) ();
}

GdkKeymap *gdk_keymap_get_default (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_keymap_get_default);
    return (*_gdk_keymap_get_default) ();
}

gboolean gdk_keymap_get_entries_for_keyval (GdkKeymap * keymap,
                        guint keyval,
                        GdkKeymapKey ** keys,
                        gint * n_keys)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_keymap_get_entries_for_keyval);
    return (*_gdk_keymap_get_entries_for_keyval) (keymap, keyval, keys,
                          n_keys);
}

guint gdk_keymap_lookup_key (GdkKeymap * keymap, const GdkKeymapKey * key)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_keymap_lookup_key);
    return (*_gdk_keymap_lookup_key) (keymap, key);
}

gboolean gdk_keymap_translate_keyboard_state (GdkKeymap * keymap,
                          guint hardware_keycode,
                          GdkModifierType state,
                          gint group,
                          guint * keyval,
                          gint * effective_group,
                          gint * level,
                          GdkModifierType *
                          consumed_modifiers)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_keymap_translate_keyboard_state);
    return (*_gdk_keymap_translate_keyboard_state) (keymap, hardware_keycode,
                            state, group, keyval,
                            effective_group, level,
                            consumed_modifiers);
}

guint32 gdk_keyval_to_unicode (guint keyval)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_keyval_to_unicode);
    return (*_gdk_keyval_to_unicode) (keyval);
}

GdkPixbuf *gdk_pixbuf_get_from_drawable (GdkPixbuf * dest,
                     GdkDrawable * src,
                     GdkColormap * cmap,
                     int src_x,
                     int src_y,
                     int dest_x,
                     int dest_y, int width, int height)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_pixbuf_get_from_drawable);
    return (*_gdk_pixbuf_get_from_drawable) (dest, src, cmap, src_x, src_y,
                         dest_x, dest_y, width, height);
}

void gdk_pixbuf_render_pixmap_and_mask (GdkPixbuf * pixbuf,
                    GdkPixmap ** pixmap_return,
                    GdkBitmap ** mask_return,
                    int alpha_threshold)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_pixbuf_render_pixmap_and_mask);
    (*_gdk_pixbuf_render_pixmap_and_mask) (pixbuf, pixmap_return, mask_return,
                       alpha_threshold);
}

void gdk_pixbuf_render_pixmap_and_mask_for_colormap (GdkPixbuf * pixbuf,
                             GdkColormap * colormap,
                             GdkPixmap **
                             pixmap_return,
                             GdkBitmap ** mask_return,
                             int alpha_threshold)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_pixbuf_render_pixmap_and_mask_for_colormap);
    (*_gdk_pixbuf_render_pixmap_and_mask_for_colormap) (pixbuf, colormap,
                            pixmap_return,
                            mask_return,
                            alpha_threshold);
}

GdkGrabStatus gdk_pointer_grab (GdkWindow * window,
                gboolean owner_events,
                GdkEventMask event_mask,
                GdkWindow * confine_to,
                GdkCursor * cursor, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_pointer_grab);
    return (*_gdk_pointer_grab) (window, owner_events, event_mask, confine_to,
                 cursor, time_);
}

void gdk_pointer_ungrab (guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_pointer_ungrab);
    return (*_gdk_pointer_ungrab) (time_);
}

void gdk_property_change (GdkWindow * window,
              GdkAtom property,
              GdkAtom type,
              gint format,
              GdkPropMode mode,
              const guchar * data, gint nelements)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_property_change);
    return (*_gdk_property_change) (window, property, type, format, mode,
                    data, nelements);
}

gboolean gdk_property_get (GdkWindow * window,
               GdkAtom property,
               GdkAtom type,
               gulong offset,
               gulong length,
               gint pdelete,
               GdkAtom * actual_property_type,
               gint * actual_format,
               gint * actual_length, guchar ** data)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_property_get);
    return (*_gdk_property_get) (window, property, type, offset, length,
                 pdelete, actual_property_type, actual_format,
                 actual_length, data);
}

gboolean gdk_rectangle_intersect (const GdkRectangle * src1,
                  const GdkRectangle * src2,
                  GdkRectangle * dest)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_rectangle_intersect);
    return (*_gdk_rectangle_intersect) (src1, src2, dest);
}

void gdk_region_destroy (GdkRegion * region)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_region_destroy);
    (*_gdk_region_destroy) (region);
}

GdkRegion *gdk_region_new (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_region_new);
    return (*_gdk_region_new) ();
}

GdkScreen *gdk_screen_get_default (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_default);
    return (*_gdk_screen_get_default) ();
}

gint gdk_screen_get_height (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_height);
    return (*_gdk_screen_get_height) (screen);
}

gint gdk_screen_get_monitor_at_point (GdkScreen * screen, gint x, gint y)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_monitor_at_point);
    return (*_gdk_screen_get_monitor_at_point) (screen, x, y);
}

void gdk_screen_get_monitor_geometry (GdkScreen * screen,
                      gint monitor_num, GdkRectangle * dest)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_monitor_geometry);
    (*_gdk_screen_get_monitor_geometry) (screen, monitor_num, dest);
}

gint gdk_screen_get_n_monitors (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_n_monitors);
    return (*_gdk_screen_get_n_monitors) (screen);
}

gint gdk_screen_get_width_mm (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_width_mm);
    return (*_gdk_screen_get_width_mm) (screen);
}

gint gdk_screen_get_height_mm (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_height_mm);
    return (*_gdk_screen_get_height_mm) (screen);
}

gint gdk_screen_get_monitor_width_mm (GdkScreen * screen, gint monitor_num)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_monitor_width_mm);
    return (*_gdk_screen_get_monitor_width_mm) (screen, monitor_num);
}

gint gdk_screen_get_monitor_height_mm (GdkScreen * screen, gint monitor_num)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_monitor_height_mm);
    return (*_gdk_screen_get_monitor_height_mm) (screen, monitor_num);
}

gdouble gdk_screen_get_resolution (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_resolution);
    return (*_gdk_screen_get_resolution) (screen);
}

GdkColormap *gdk_screen_get_rgba_colormap (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_rgba_colormap);
    return (*_gdk_screen_get_rgba_colormap) (screen);
}

GdkColormap *gdk_screen_get_rgb_colormap (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_rgb_colormap);
    return (*_gdk_screen_get_rgb_colormap) (screen);
}

GdkWindow *gdk_screen_get_root_window (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_root_window);
    return (*_gdk_screen_get_root_window) (screen);
}

GdkVisual *gdk_screen_get_system_visual (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_system_visual);
    return (*_gdk_screen_get_system_visual) (screen);
}

gint gdk_screen_get_width (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_width);
    return (*_gdk_screen_get_width) (screen);
}

gboolean gdk_screen_is_composited (GdkScreen * screen)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_is_composited);
    return (*_gdk_screen_is_composited) (screen);
}

void gdk_selection_convert (GdkWindow * requestor,
                GdkAtom selection, GdkAtom target, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_selection_convert);
    (*_gdk_selection_convert) (requestor, selection, target, time_);
}

gboolean gdk_selection_owner_set (GdkWindow * owner,
                  GdkAtom selection,
                  guint32 time_, gboolean send_event)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_selection_owner_set);
    return (*_gdk_selection_owner_set) (owner, selection, time_, send_event);
}

gint gdk_selection_property_get (GdkWindow * requestor,
                 guchar ** data,
                 GdkAtom * prop_type, gint * prop_format)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_selection_property_get);
    return (*_gdk_selection_property_get) (requestor, data, prop_type,
                       prop_format);
}

void gdk_selection_send_notify (GdkNativeWindow requestor,
                GdkAtom selection,
                GdkAtom target,
                GdkAtom property, guint32 time_)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_selection_send_notify);
    return (*_gdk_selection_send_notify) (requestor, selection, target,
                      property, time_);
}

guint gdk_unicode_to_keyval (guint32 wc)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_unicode_to_keyval);
    return (*_gdk_unicode_to_keyval) (wc);
}

guint gdk_threads_add_idle_full (gint priority,
                 GSourceFunc function,
                 gpointer data, GDestroyNotify notify)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_add_idle_full);
    return (*_gdk_threads_add_idle_full) (priority, function, data, notify);
}

guint gdk_threads_add_idle (GSourceFunc function, gpointer data)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_add_idle);
    return (*_gdk_threads_add_idle) (function, data);
}

guint gdk_threads_add_timeout_full (gint priority,
                    guint interval,
                    GSourceFunc function,
                    gpointer data, GDestroyNotify notify)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_add_timeout_full);
    return (*_gdk_threads_add_timeout_full) (priority, interval, function,
                         data, notify);
}

void gdk_threads_enter (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_enter);
    (*_gdk_threads_enter) ();
}

void gdk_threads_init (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_init);
    (*_gdk_threads_init) ();
}

void gdk_threads_leave (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_threads_leave);
    (*_gdk_threads_leave) ();
}

void gdk_window_destroy (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_destroy);
    (*_gdk_window_destroy) (window);
}

GdkCursor *gdk_window_get_cursor (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_cursor);
    return (*_gdk_window_get_cursor) (window);
}

GdkEventMask gdk_window_get_events (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_events);
    return (*_gdk_window_get_events) (window);
}

void gdk_window_get_geometry (GdkWindow * window,
                  gint * x,
                  gint * y,
                  gint * width, gint * height, gint * depth)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_geometry);
    (*_gdk_window_get_geometry) (window, x, y, width, height, depth);
}

gint gdk_window_get_origin (GdkWindow * window, gint * x, gint * y)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_origin);
    return (*_gdk_window_get_origin) (window, x, y);
}

gboolean gdk_window_is_destroyed (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_is_destroyed);
    return (*_gdk_window_is_destroyed) (window);
}

void gdk_window_move (GdkWindow * window, gint x, gint y)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_move);
    (*_gdk_window_move) (window, x, y);
}

GdkWindow *gdk_window_new (GdkWindow * parent,
               GdkWindowAttr * attributes, gint attributes_mask)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_new);
    return (*_gdk_window_new) (parent, attributes, attributes_mask);
}

void gdk_window_register_dnd (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_register_dnd);
    (*_gdk_window_register_dnd) (window);
}

void gdk_window_resize (GdkWindow * window, gint width, gint height)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_resize);
    (*_gdk_window_resize) (window, width, height);
}

void gdk_window_restack (GdkWindow * window,
             GdkWindow * sibling, gboolean above)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_restack);
    (*_gdk_window_restack) (window, sibling, above);
}

void gdk_window_set_cursor (GdkWindow * window, GdkCursor * cursor)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_set_cursor);
    (*_gdk_window_set_cursor) (window, cursor);
}

void gdk_window_set_events (GdkWindow * window, GdkEventMask event_mask)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_set_events);
    (*_gdk_window_set_events) (window, event_mask);
}

void gdk_window_set_functions (GdkWindow * window, GdkWMFunction functions)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_set_functions);
    (*_gdk_window_set_functions) (window, functions);
}

void gdk_window_show (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_show);
    (*_gdk_window_show) (window);
}

Display *gdk_x11_display_get_xdisplay (GdkDisplay * display)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_display_get_xdisplay);
    return (*_gdk_x11_display_get_xdisplay) (display);
}

void glass_gdk_x11_display_set_window_scale (GdkDisplay *display,
                          gint scale)
{
    if (wrapper_gtk_version >= 3) {
        // Optional call, if it does not exist then GTK3 is not yet
        // doing automatic scaling of coordinates so we do not need
        // to override it.  CHECK_LOAD_SYMBOL_GDK_OPT will simply
        // return if the symbol was not found.
        CHECK_LOAD_SYMBOL_GDK_OPT (gdk_x11_display_set_window_scale);
        (*_gdk_x11_display_set_window_scale) (display, scale);
    }
}

XID gdk_x11_drawable_get_xid (GdkDrawable * drawable)
{
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_x11_drawable_get_xid);
        return (*_gdk_x11_drawable_get_xid) (drawable);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_x11_window_get_xid);
        return (*_gdk_x11_window_get_xid) (drawable);
    }
}

gint gdk_x11_get_default_screen (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_get_default_screen);
    return (*_gdk_x11_get_default_screen) ();
}

Display *gdk_x11_get_default_xdisplay (void)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_get_default_xdisplay);
    return (*_gdk_x11_get_default_xdisplay) ();
}

guint32 gdk_x11_get_server_time (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_get_server_time);
    return (*_gdk_x11_get_server_time) (window);
}

GdkVisual *gdk_x11_screen_lookup_visual (GdkScreen * screen,
                     VisualID xvisualid)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_screen_lookup_visual);
    return (*_gdk_x11_screen_lookup_visual) (screen, xvisualid);
}

GdkWindow *gdk_x11_window_foreign_new_for_display (GdkDisplay * display,
                           Window window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_window_foreign_new_for_display);
    return (*_gdk_x11_window_foreign_new_for_display) (display, window);
}

GdkWindow *gdk_x11_window_lookup_for_display (GdkDisplay * display,
                          Window window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_window_lookup_for_display);
    return (*_gdk_x11_window_lookup_for_display) (display, window);
}

GdkDisplay *gdk_window_get_display (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_display);
    return (*_gdk_window_get_display) (window);
}

int gdk_window_get_height (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_height);
    return (*_gdk_window_get_height) (window);
}

int gdk_window_get_width (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_width);
    return (*_gdk_window_get_width) (window);
}

GdkScreen *gdk_window_get_screen (GdkWindow * window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_screen);
    return (*_gdk_window_get_screen) (window);
}


//--------------------------------------------------------------------------------------


GdkVisual *   gdk_window_get_visual (GdkWindow     *window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_visual);
    return (*_gdk_window_get_visual)(window);
}

GdkScreen    *gdk_visual_get_screen (GdkVisual *visual)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_visual_get_screen);
    return (*_gdk_visual_get_screen)(visual);
}

//--------------------------------------------------------------------------------------

Window
gdk_x11_window_get_xid(GdkWindow   *window)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_x11_window_get_xid);
    return (*_gdk_x11_window_get_xid)(window);
}

GType
gdk_window_object_get_type       (void)
{
    if (wrapper_gtk_version == 2) {
        return (*_gdk_window_object_get_type)();
    } else {
        return (*_gdk_window_get_type)();
    }
}

cairo_region_t *
gdk_cairo_region_create_from_surface (cairo_surface_t *surface)
{
    return (*_gdk_cairo_region_create_from_surface)(surface);
}

//--------------------------------------------------------------------------------------

typedef struct _DeviceGrabContext {
    GdkWindow * window;
    gboolean grabbed;
} DeviceGrabContext;



gboolean disableGrab = FALSE;
static gboolean configure_transparent_window(GtkWidget *window);
static void configure_opaque_window(GtkWidget *window);

static void grab_mouse_device(GdkDevice *device, DeviceGrabContext *context);
static void ungrab_mouse_device(GdkDevice *device);

gint glass_gdk_visual_get_depth (GdkVisual * visual)
{
    // gdk_visual_get_depth is GTK 2.2 +
    if (_gdk_visual_get_depth) {
        CHECK_LOAD_SYMBOL_GDK (gdk_visual_get_depth);
        return (*_gdk_visual_get_depth) (visual);
    } else {
        return visual ? visual->depth : 0;
    }
}

GdkScreen * glass_gdk_window_get_screen(GdkWindow * gdkWindow)
{
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_get_screen);
        return (*_gdk_window_get_screen)(gdkWindow);
    } else {
        GdkVisual * gdkVisual = gdk_window_get_visual(gdkWindow);
        return gdk_visual_get_screen(gdkVisual);
    }
}

gboolean
glass_gdk_mouse_devices_grab(GdkWindow *gdkWindow) {
    if (wrapper_gtk_version == 2) {
        return glass_gdk_mouse_devices_grab_with_cursor(gdkWindow, NULL, TRUE);
    } else {
        if (disableGrab) {
            return TRUE;
        }

        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_list_devices);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);

        DeviceGrabContext context;
        GList *devices = (*_gdk_device_manager_list_devices) (
                             (*_gdk_display_get_device_manager)(
                                 gdk_display_get_default()),
                                 GDK_DEVICE_TYPE_MASTER);

        context.window = gdkWindow;
        context.grabbed = FALSE;
        g_list_foreach(devices, (GFunc) grab_mouse_device, &context);

        return context.grabbed;
    }
}

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow *gdkWindow, GdkCursor *cursor, gboolean owner_events) {
    if (disableGrab) {
        return TRUE;
    }
    CHECK_LOAD_SYMBOL_GDK (gdk_pointer_grab);
    GdkGrabStatus status = (*_gdk_pointer_grab)(gdkWindow, owner_events, (GdkEventMask)
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
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_pointer_ungrab);
        (*_gdk_pointer_ungrab)(GDK_CURRENT_TIME);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_list_devices);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);

        GList *devices = (*_gdk_device_manager_list_devices)(
                             (*_gdk_display_get_device_manager)(
                                 (*_gdk_display_get_default)()),
                                 GDK_DEVICE_TYPE_MASTER);
        g_list_foreach(devices, (GFunc) ungrab_mouse_device, NULL);
    }
}

void
glass_gdk_master_pointer_grab(GdkWindow *window, GdkCursor *cursor) {
    if (disableGrab) {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_set_cursor);
        (*_gdk_window_set_cursor)(window, cursor);
        return;
    }
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_pointer_grab);
        (*_gdk_pointer_grab)(window, FALSE, (GdkEventMask)
                         (GDK_POINTER_MOTION_MASK
                             | GDK_BUTTON_MOTION_MASK
                             | GDK_BUTTON1_MOTION_MASK
                             | GDK_BUTTON2_MOTION_MASK
                             | GDK_BUTTON3_MOTION_MASK
                             | GDK_BUTTON_RELEASE_MASK),
                         NULL, cursor, GDK_CURRENT_TIME);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_grab);
        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_get_client_pointer);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);

        (*_gdk_device_grab)((*_gdk_device_manager_get_client_pointer)(
                    (*_gdk_display_get_device_manager)(
                        (*_gdk_display_get_default)())),
                    window, GDK_OWNERSHIP_NONE, FALSE, GDK_ALL_EVENTS_MASK,
                    cursor, GDK_CURRENT_TIME);
    }
}

void
glass_gdk_master_pointer_ungrab() {
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_pointer_ungrab);
        (*_gdk_pointer_ungrab)(GDK_CURRENT_TIME);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_ungrab);
        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_get_client_pointer);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);
        (*_gdk_device_ungrab)((*_gdk_device_manager_get_client_pointer)(
                              (*_gdk_display_get_device_manager)(
                                  (*_gdk_display_get_default)())),
                          GDK_CURRENT_TIME);
    }
}

void
glass_gdk_master_pointer_get_position(gint *x, gint *y) {
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_pointer);
        (*_gdk_display_get_pointer)(gdk_display_get_default(), NULL, x, y, NULL);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_get_client_pointer);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_default);
        (*_gdk_device_get_position)((*_gdk_device_manager_get_client_pointer)(
                                    (*_gdk_display_get_device_manager)(
                                        (*_gdk_display_get_default)())),
                                NULL, x, y);
    }
}

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device) {
    if (wrapper_gtk_version == 2) {
        (void) device;
        CHECK_LOAD_SYMBOL_GDK (gdk_display_pointer_is_grabbed);
        return (*_gdk_display_pointer_is_grabbed)(gdk_display_get_default());
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_display_device_is_grabbed);
        return (*_gdk_display_device_is_grabbed)((*_gdk_display_get_default)(), device);
    }
}

void
glass_gdk_device_ungrab(GdkDevice *device) {
    if (wrapper_gtk_version == 2) {
        (void) device;
        CHECK_LOAD_SYMBOL_GDK (gdk_pointer_ungrab);
        (*_gdk_pointer_ungrab)(GDK_CURRENT_TIME);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_ungrab);
        (*_gdk_device_ungrab)(device, GDK_CURRENT_TIME);
    }
}

GdkWindow *
glass_gdk_device_get_window_at_position(GdkDevice *device, gint *x, gint *y) {
    if (wrapper_gtk_version == 2) {
        (void) device;
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_window_at_pointer);
        return (*_gdk_display_get_window_at_pointer)(gdk_display_get_default(), x, y);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_get_window_at_position);
        return (*_gdk_device_get_window_at_position)(device, x, y);
    }
}

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                             gboolean transparent) {
    if (wrapper_gtk_version == 2) {
        glass_configure_window_transparency(window, transparent);
        gtk_widget_realize(window);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_set_background_rgba);
        gboolean isTransparent = glass_configure_window_transparency(window, transparent);
        gtk_widget_realize(window);
        if (isTransparent) {
            GdkRGBA rgba = { 1.0, 1.0, 1.0, 0.0 };
            (*_gdk_window_set_background_rgba)(gtk_widget_get_window(window), &rgba);
        }
    }
}

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual) {
    glass_widget_set_visual(widget, visual);
}

static gboolean
configure_transparent_window(GtkWidget *window) {
    GdkScreen *default_screen = (*_gdk_screen_get_default)();
    GdkDisplay *default_display = (*_gdk_display_get_default)();

    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_rgba_colormap);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_supports_composite);
        CHECK_LOAD_SYMBOL_GDK (gdk_screen_is_composited);
        GdkColormap *colormap = (*_gdk_screen_get_rgba_colormap)(default_screen);
        if (colormap
                && (*_gdk_display_supports_composite)(default_display)
                && (*_gdk_screen_is_composited)(default_screen)) {
            gtk_widget_set_colormap(window, colormap);
            return TRUE;
        }
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_display_supports_composite);
        CHECK_LOAD_SYMBOL_GDK (gdk_screen_is_composited);
        GdkVisual *visual = (*_gdk_screen_get_rgba_visual)(default_screen);
        if (visual
                && (*_gdk_display_supports_composite)(default_display)
                && (*_gdk_screen_is_composited)(default_screen)) {
            glass_widget_set_visual(window, visual);
            return TRUE;
        }

        return FALSE;
    }

    return FALSE;
}

int
glass_gtk_fixup_typed_key(int key, int keyval) {
    if (wrapper_gtk_version == 2) {
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
    }
    return key;
}

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h) {
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_width);
    CHECK_LOAD_SYMBOL_GDK (gdk_window_get_height);
    *w = (*_gdk_window_get_width)(window);
    *h = (*_gdk_window_get_height)(window);
}

void
glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y) {
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_pointer);
        (*_gdk_display_get_pointer)(display, NULL, x, y, NULL);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_device_get_position);
        CHECK_LOAD_SYMBOL_GDK (gdk_device_manager_get_client_pointer);
        CHECK_LOAD_SYMBOL_GDK (gdk_display_get_device_manager);
        (*_gdk_device_get_position)(
            (*_gdk_device_manager_get_client_pointer)(
                (*_gdk_display_get_device_manager)(display)), NULL , x, y);
    }
}


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
    (void) window;
/* We need to pick a visual that really is glx compatible
 * instead of using the default visual
 *
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_system_visual);
    CHECK_LOAD_SYMBOL_GDK (gdk_screen_get_default);
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

static void
grab_mouse_device(GdkDevice *device, DeviceGrabContext *context) {
    CHECK_LOAD_SYMBOL_GDK (gdk_device_get_source);
    CHECK_LOAD_SYMBOL_GDK (gdk_device_grab);
    GdkInputSource source = (*_gdk_device_get_source)(device);
    if (source == GDK_SOURCE_MOUSE) {
        GdkGrabStatus status = (*_gdk_device_grab)(device,
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
    CHECK_LOAD_SYMBOL_GDK (gdk_device_get_source);
    CHECK_LOAD_SYMBOL_GDK (gdk_device_ungrab);
    GdkInputSource source = (*_gdk_device_get_source)(device);
    if (source == GDK_SOURCE_MOUSE) {
        (*_gdk_device_ungrab)(device, GDK_CURRENT_TIME);
    }
}

GdkPixbuf *
glass_pixbuf_from_window(GdkWindow *window,
    gint srcx, gint srcy,
    gint width, gint height)
{
    GdkPixbuf * ret = NULL;

    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_pixbuf_get_from_drawable);
        ret = (*_gdk_pixbuf_get_from_drawable) (NULL,
            window,
            NULL,
            srcx, srcy,
            0, 0,
            width, height);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_pixbuf_get_from_window);
        ret = (*_gdk_pixbuf_get_from_window) (window, srcx, srcy, width, height);
    }

    return ret;
}

void
glass_window_apply_shape_mask(GdkWindow *window,
    void* data, uint width, uint height)
{
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_input_shape_combine_mask);

        GdkPixbuf* pixbuf = gdk_pixbuf_new_from_data((guchar *) data,
                GDK_COLORSPACE_RGB, TRUE, 8, width, height, width * 4, NULL, NULL);

        if (GDK_IS_PIXBUF(pixbuf)) {
            GdkBitmap* mask = NULL;
            gdk_pixbuf_render_pixmap_and_mask(pixbuf, NULL, &mask, 128);

            (*_gdk_window_input_shape_combine_mask)(window, mask, 0, 0);

            g_object_unref(pixbuf);
            if (mask) {
                g_object_unref(mask);
            }
        }
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_shape_combine_region);
        CHECK_LOAD_SYMBOL_GDK (gdk_window_input_shape_combine_region);
        CHECK_LOAD_SYMBOL_GDK (gdk_cairo_region_create_from_surface);

        cairo_surface_t * shape = cairo_image_surface_create_for_data(
                 (unsigned char *)data,
                 CAIRO_FORMAT_ARGB32,
                 width, height,
                 width * 4
                 );
        cairo_region_t *region = (*_gdk_cairo_region_create_from_surface) (shape);

        (*_gdk_window_shape_combine_region) (window, region, 0, 0);

        (*_gdk_window_input_shape_combine_region) (window, region, 0, 0);

        cairo_region_destroy(region);
        cairo_surface_finish (shape);
    }
}

void
glass_window_reset_input_shape_mask(GdkWindow *window)
{
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_input_shape_combine_mask);
        (*_gdk_window_input_shape_combine_mask)(window, NULL, 0, 0);
    } else {
        CHECK_LOAD_SYMBOL_GDK (gdk_window_input_shape_combine_region);
        (*_gdk_window_input_shape_combine_region) (window, NULL, 0, 0);
    }
}

GdkWindow *
glass_gdk_drag_context_get_dest_window (GdkDragContext * context)
{
    CHECK_LOAD_SYMBOL_GDK (gdk_drag_context_get_dest_window);
    return ((context != NULL) ? gdk_drag_context_get_dest_window(context) : NULL);
}


