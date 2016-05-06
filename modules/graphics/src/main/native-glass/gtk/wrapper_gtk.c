/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>

#include <assert.h>

#include <gtk/gtk.h>

#include "glass_wrapper.h"

static GtkClipboard *(*_gtk_clipboard_get) (GdkAtom selection);
static gboolean (*_gtk_clipboard_set_with_data) (GtkClipboard * clipboard,
                         const GtkTargetEntry *
                         targets, guint n_targets,
                         GtkClipboardGetFunc get_func,
                         GtkClipboardClearFunc
                         clear_func,
                         gpointer user_data);
static GtkSelectionData *(*_gtk_clipboard_wait_for_contents) (GtkClipboard *
                                  clipboard,
                                  GdkAtom target);
static gchar *(*_gtk_clipboard_wait_for_text) (GtkClipboard * clipboard);
static GdkPixbuf *(*_gtk_clipboard_wait_for_image) (GtkClipboard * clipboard);
static gchar **(*_gtk_clipboard_wait_for_uris) (GtkClipboard * clipboard);
static gboolean (*_gtk_clipboard_wait_for_targets) (GtkClipboard * clipboard,
                            GdkAtom ** targets,
                            gint * n_targets);
static void (*_gtk_container_add) (GtkContainer * container,
                   GtkWidget * widget);
static GType (*_gtk_container_get_type) (void) G_GNUC_CONST;
static gint (*_gtk_dialog_run) (GtkDialog * dialog);
static GType (*_gtk_dialog_get_type) (void) G_GNUC_CONST;
static GtkWidget *(*_gtk_drawing_area_new) (void);
static gboolean (*_gtk_events_pending) (void);
static void (*_gtk_file_chooser_add_filter) (GtkFileChooser * chooser,
                         GtkFileFilter * filter);
static gchar *(*_gtk_file_chooser_get_filename) (GtkFileChooser * chooser);
static GSList *(*_gtk_file_chooser_get_filenames) (GtkFileChooser * chooser);
static GtkFileFilter *(*_gtk_file_chooser_get_filter) (GtkFileChooser *
                               chooser);
static GType (*_gtk_file_chooser_get_type) (void) G_GNUC_CONST;
static gboolean (*_gtk_file_chooser_set_current_folder) (GtkFileChooser *
                             chooser,
                             const gchar *
                             filename);
static void (*_gtk_file_chooser_set_current_name) (GtkFileChooser * chooser,
                           const gchar * name);
static void (*_gtk_file_chooser_set_do_overwrite_confirmation) (GtkFileChooser
                                * chooser,
                                gboolean
                                do_overwrite_confirmation);
static void (*_gtk_file_chooser_set_filter) (GtkFileChooser * chooser,
                         GtkFileFilter * filter);
static void (*_gtk_file_chooser_set_select_multiple) (GtkFileChooser *
                              chooser,
                              gboolean
                              select_multiple);


static GtkWidget *(*_gtk_file_chooser_dialog_new) (const gchar * title,
                           GtkWindow * parent,
                           GtkFileChooserAction
                           action,
                           const gchar *
                           first_button_text,
                           ...)
    G_GNUC_NULL_TERMINATED;


static void (*_gtk_file_filter_add_pattern) (GtkFileFilter * filter,
                         const gchar * pattern);
static GtkFileFilter *(*_gtk_file_filter_new) (void);
static void (*_gtk_file_filter_set_name) (GtkFileFilter * filter,
                      const gchar * name);
static GtkWidget *(*_gtk_fixed_new) (void);
static void (*_gtk_init) (int *argc, char ***argv);
static void (*_gtk_main_do_event) (GdkEvent * event);
static void (*_gtk_main) (void);
static gboolean (*_gtk_main_iteration) (void);
static void (*_gtk_main_quit) (void);
static GtkWidget *(*_gtk_plug_new) (GdkNativeWindow socket_id);
static void (*_gtk_selection_data_free) (GtkSelectionData * data);
static const guchar *(*_gtk_selection_data_get_data) (GtkSelectionData *
                              selection_data);
static gint (*_gtk_selection_data_get_length) (GtkSelectionData *
                           selection_data);
static GdkAtom (*_gtk_selection_data_get_target) (GtkSelectionData *
                          selection_data);
static void (*_gtk_selection_data_set) (GtkSelectionData * selection_data,
                    GdkAtom type, gint format,
                    const guchar * data, gint length);
static gboolean (*_gtk_selection_data_set_pixbuf) (GtkSelectionData *
                           selection_data,
                           GdkPixbuf * pixbuf);
static gboolean (*_gtk_selection_data_set_text) (GtkSelectionData *
                         selection_data,
                         const gchar * str, gint len);
static gboolean (*_gtk_selection_data_set_uris) (GtkSelectionData *
                         selection_data,
                         gchar ** uris);
static GtkSettings *(*_gtk_settings_get_default) (void);
static void (*_gtk_target_list_add) (GtkTargetList * list,
                     GdkAtom target, guint flags, guint info);
static void (*_gtk_target_list_add_image_targets) (GtkTargetList * list,
                           guint info,
                           gboolean writable);
static void (*_gtk_target_list_add_text_targets) (GtkTargetList * list,
                          guint info);
static GtkTargetList *(*_gtk_target_list_new) (const GtkTargetEntry * targets,
                           guint ntargets);
static void (*_gtk_target_list_unref) (GtkTargetList * list);
static gboolean (*_gtk_targets_include_image) (GdkAtom * targets,
                           gint n_targets,
                           gboolean writable);
static gboolean (*_gtk_targets_include_text) (GdkAtom * targets,
                          gint n_targets);
static void (*_gtk_target_table_free) (GtkTargetEntry * targets,
                       gint n_targets);
static GtkTargetEntry *(*_gtk_target_table_new_from_list) (GtkTargetList *
                               list,
                               gint * n_targets);
static void (*_gtk_widget_destroy) (GtkWidget * widget);
static GtkWidget *(*_gtk_widget_get_ancestor) (GtkWidget * widget,
                           GType widget_type);
static GdkScreen *(*_gtk_widget_get_screen) (GtkWidget * widget);
static void (*_gtk_widget_get_allocation) (GtkWidget * widget,
                       GtkAllocation * allocation);
static GType (*_gtk_widget_get_type) (void) G_GNUC_CONST;
static gboolean (*_gtk_widget_get_visible) (GtkWidget * widget);
static GdkWindow *(*_gtk_widget_get_window) (GtkWidget * widget);
static void (*_gtk_widget_grab_focus) (GtkWidget * widget);
static void (*_gtk_widget_hide) (GtkWidget * widget);
static void (*_gtk_widget_modify_bg) (GtkWidget * widget,
                      GtkStateType state,
                      const GdkColor * color);
static void (*_gtk_widget_realize) (GtkWidget * widget);
static void (*_gtk_widget_set_app_paintable) (GtkWidget * widget,
                          gboolean app_paintable);
static void (*_gtk_widget_set_can_focus) (GtkWidget * widget,
                      gboolean can_focus);
static void (*_gtk_widget_set_colormap) (GtkWidget * widget,
                     GdkColormap * colormap);
static void (*_gtk_widget_set_events) (GtkWidget * widget, gint events);
static void (*_gtk_widget_set_size_request) (GtkWidget * widget,
                         gint width, gint height);
static void (*_gtk_widget_show_all) (GtkWidget * widget);
static void (*_gtk_window_deiconify) (GtkWindow * window);
static void (*_gtk_window_fullscreen) (GtkWindow * window);
static gboolean (*_gtk_window_get_decorated) (GtkWindow * window);
static gboolean (*_gtk_window_get_decorated) (GtkWindow * window);
static void (*_gtk_window_get_position) (GtkWindow * window,
                     gint * root_x, gint * root_y);
static void (*_gtk_window_get_size) (GtkWindow * window,
                     gint * width, gint * height);
static GType (*_gtk_window_get_type) (void) G_GNUC_CONST;
static void (*_gtk_window_iconify) (GtkWindow * window);
static void (*_gtk_window_maximize) (GtkWindow * window);
static void (*_gtk_window_move) (GtkWindow * window, gint x, gint y);
static GtkWidget *(*_gtk_window_new) (GtkWindowType type);
static void (*_gtk_window_present) (GtkWindow * window);
static void (*_gtk_window_resize) (GtkWindow * window,
                   gint width, gint height);
static void (*_gtk_window_set_accept_focus) (GtkWindow * window,
                         gboolean setting);
static void (*_gtk_window_set_decorated) (GtkWindow * window,
                      gboolean setting);
static void (*_gtk_window_set_geometry_hints) (GtkWindow * window,
                           GtkWidget * geometry_widget,
                           GdkGeometry * geometry,
                           GdkWindowHints geom_mask);
static void (*_gtk_window_set_icon) (GtkWindow * window, GdkPixbuf * icon);
static void (*_gtk_window_set_keep_above) (GtkWindow * window,
                       gboolean setting);
static void (*_gtk_window_set_keep_below) (GtkWindow * window,
                       gboolean setting);
static void (*_gtk_window_set_modal) (GtkWindow * window, gboolean modal);
static void (*_gtk_window_set_opacity) (GtkWindow * window, gdouble opacity);
static void (*_gtk_window_set_title) (GtkWindow * window,
                      const gchar * title);
static void (*_gtk_window_set_transient_for) (GtkWindow * window,
                          GtkWindow * parent);
static void (*_gtk_window_set_type_hint) (GtkWindow * window,
                      GdkWindowTypeHint hint);
static void (*_gtk_window_set_wmclass) (GtkWindow * window,
                    const gchar * wmclass_name,
                    const gchar * wmclass_class);
static void (*_gtk_window_unfullscreen) (GtkWindow * window);
static void (*_gtk_window_unmaximize) (GtkWindow * window);


//--------------------------------------------------------------------------------------
// GTK3 only
static void (*_gtk_widget_set_visual) (GtkWidget *widget, GdkVisual *visual);

static void (*_gtk_widget_shape_combine_region) (GtkWidget *widget,
                                 cairo_region_t *region);

static void (*_gtk_widget_input_shape_combine_region) (GtkWidget *widget,
                                       cairo_region_t *region);


//--------------------------------------------------------------------------------------

#define PRELOAD_SYMBOL_GTK(x) \
    _##x = dlsym(libgtk, #x); \
    if (!_##x) { \
        symbol_load_errors++; \
        fprintf(stderr,"failed loading %s\n", #x); \
    }

int wrapper_load_symbols_gtk(int version, void * libgtk)
{
    int symbol_load_errors = 0;

    PRELOAD_SYMBOL_GTK (gtk_clipboard_get);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_set_with_data);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_wait_for_contents);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_wait_for_text);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_wait_for_image);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_wait_for_uris);
    PRELOAD_SYMBOL_GTK (gtk_clipboard_wait_for_targets);
    PRELOAD_SYMBOL_GTK (gtk_container_add);
    PRELOAD_SYMBOL_GTK (gtk_container_get_type);
    PRELOAD_SYMBOL_GTK (gtk_dialog_run);
    PRELOAD_SYMBOL_GTK (gtk_dialog_get_type);
    PRELOAD_SYMBOL_GTK (gtk_drawing_area_new);
    PRELOAD_SYMBOL_GTK (gtk_events_pending);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_add_filter);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_get_filename);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_get_filenames);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_get_filter);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_get_type);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_set_current_folder);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_set_current_name);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_set_do_overwrite_confirmation);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_set_filter);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_set_select_multiple);
    PRELOAD_SYMBOL_GTK (gtk_file_chooser_dialog_new);
    PRELOAD_SYMBOL_GTK (gtk_file_filter_add_pattern);
    PRELOAD_SYMBOL_GTK (gtk_file_filter_new);
    PRELOAD_SYMBOL_GTK (gtk_file_filter_set_name);
    PRELOAD_SYMBOL_GTK (gtk_fixed_new);
    PRELOAD_SYMBOL_GTK (gtk_init);
    PRELOAD_SYMBOL_GTK (gtk_main_do_event);
    PRELOAD_SYMBOL_GTK (gtk_main);
    PRELOAD_SYMBOL_GTK (gtk_main_iteration);
    PRELOAD_SYMBOL_GTK (gtk_main_quit);
    PRELOAD_SYMBOL_GTK (gtk_plug_new);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_free);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_get_data);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_get_length);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_get_target);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_set);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_set_pixbuf);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_set_text);
    PRELOAD_SYMBOL_GTK (gtk_selection_data_set_uris);
    PRELOAD_SYMBOL_GTK (gtk_settings_get_default);
    PRELOAD_SYMBOL_GTK (gtk_target_list_add);
    PRELOAD_SYMBOL_GTK (gtk_target_list_add_image_targets);
    PRELOAD_SYMBOL_GTK (gtk_target_list_add_text_targets);
    PRELOAD_SYMBOL_GTK (gtk_target_list_new);
    PRELOAD_SYMBOL_GTK (gtk_target_list_unref);
    PRELOAD_SYMBOL_GTK (gtk_targets_include_image);
    PRELOAD_SYMBOL_GTK (gtk_targets_include_text);
    PRELOAD_SYMBOL_GTK (gtk_target_table_free);
    PRELOAD_SYMBOL_GTK (gtk_target_table_new_from_list);
    PRELOAD_SYMBOL_GTK (gtk_widget_destroy);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_ancestor);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_screen);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_allocation);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_type);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_visible);
    PRELOAD_SYMBOL_GTK (gtk_widget_get_window);
    PRELOAD_SYMBOL_GTK (gtk_widget_grab_focus);
    PRELOAD_SYMBOL_GTK (gtk_widget_hide);
    PRELOAD_SYMBOL_GTK (gtk_widget_modify_bg);
    PRELOAD_SYMBOL_GTK (gtk_widget_realize);
    PRELOAD_SYMBOL_GTK (gtk_widget_set_app_paintable);
    PRELOAD_SYMBOL_GTK (gtk_widget_set_can_focus);
    PRELOAD_SYMBOL_GTK (gtk_widget_set_events);
    PRELOAD_SYMBOL_GTK (gtk_widget_set_size_request);
    PRELOAD_SYMBOL_GTK (gtk_widget_show_all);
    PRELOAD_SYMBOL_GTK (gtk_window_deiconify);
    PRELOAD_SYMBOL_GTK (gtk_window_fullscreen);
    PRELOAD_SYMBOL_GTK (gtk_window_get_decorated);
    PRELOAD_SYMBOL_GTK (gtk_window_get_position);
    PRELOAD_SYMBOL_GTK (gtk_window_get_size);
    PRELOAD_SYMBOL_GTK (gtk_window_get_type);
    PRELOAD_SYMBOL_GTK (gtk_window_iconify);
    PRELOAD_SYMBOL_GTK (gtk_window_maximize);
    PRELOAD_SYMBOL_GTK (gtk_window_move);
    PRELOAD_SYMBOL_GTK (gtk_window_new);
    PRELOAD_SYMBOL_GTK (gtk_window_present);
    PRELOAD_SYMBOL_GTK (gtk_window_resize);
    PRELOAD_SYMBOL_GTK (gtk_window_set_accept_focus);
    PRELOAD_SYMBOL_GTK (gtk_window_set_decorated);
    PRELOAD_SYMBOL_GTK (gtk_window_set_geometry_hints);
    PRELOAD_SYMBOL_GTK (gtk_window_set_icon);
    PRELOAD_SYMBOL_GTK (gtk_window_set_keep_above);
    PRELOAD_SYMBOL_GTK (gtk_window_set_keep_below);
    PRELOAD_SYMBOL_GTK (gtk_window_set_modal);
    PRELOAD_SYMBOL_GTK (gtk_window_set_opacity);
    PRELOAD_SYMBOL_GTK (gtk_window_set_title);
    PRELOAD_SYMBOL_GTK (gtk_window_set_transient_for);
    PRELOAD_SYMBOL_GTK (gtk_window_set_type_hint);
    PRELOAD_SYMBOL_GTK (gtk_window_set_wmclass);
    PRELOAD_SYMBOL_GTK (gtk_window_unfullscreen);
    PRELOAD_SYMBOL_GTK (gtk_window_unmaximize);

    if (version == 2) {
        // gtk version 2 unique symbols

        PRELOAD_SYMBOL_GTK (gtk_widget_set_colormap);
    } else if (version == 3) {
        // gtk version 3 unique symbols

        PRELOAD_SYMBOL_GTK (gtk_widget_set_visual);
        PRELOAD_SYMBOL_GTK (gtk_widget_shape_combine_region);
        PRELOAD_SYMBOL_GTK (gtk_widget_input_shape_combine_region);
    }

    if (symbol_load_errors && wrapper_debug) {
      fprintf (stderr, "failed to load %d gtk symbols",
           symbol_load_errors);
    }

    return symbol_load_errors;

}

#define CHECK_LOAD_SYMBOL_GTK(x) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr,"missing %s\n",#x); \
            assert(_##x); \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr,"using %s\n",#x); \
               fflush(stderr); \
            } \
        } \
    }

GtkClipboard *gtk_clipboard_get (GdkAtom selection)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_get);
    return (*_gtk_clipboard_get) (selection);
}

gboolean gtk_clipboard_set_with_data (GtkClipboard * clipboard,
                      const GtkTargetEntry * targets,
                      guint n_targets,
                      GtkClipboardGetFunc get_func,
                      GtkClipboardClearFunc clear_func,
                      gpointer user_data)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_set_with_data);
    return (*_gtk_clipboard_set_with_data) (clipboard, targets, n_targets,
                        get_func, clear_func, user_data);
}

GtkSelectionData *gtk_clipboard_wait_for_contents (GtkClipboard * clipboard,
                           GdkAtom target)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_wait_for_contents);
    return (*_gtk_clipboard_wait_for_contents) (clipboard, target);
}

gchar *gtk_clipboard_wait_for_text (GtkClipboard * clipboard)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_wait_for_text);
    return (*_gtk_clipboard_wait_for_text) (clipboard);
}

GdkPixbuf *gtk_clipboard_wait_for_image (GtkClipboard * clipboard)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_wait_for_image);
    return (*_gtk_clipboard_wait_for_image) (clipboard);
}

gchar **gtk_clipboard_wait_for_uris (GtkClipboard * clipboard)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_wait_for_uris);
    return (*_gtk_clipboard_wait_for_uris) (clipboard);
}

gboolean gtk_clipboard_wait_for_targets (GtkClipboard * clipboard,
                     GdkAtom ** targets, gint * n_targets)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_clipboard_wait_for_targets);
    return (*_gtk_clipboard_wait_for_targets) (clipboard, targets, n_targets);
}

void gtk_container_add (GtkContainer * container, GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_container_add);
    (*_gtk_container_add) (container, widget);
}

GType gtk_container_get_type (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_container_get_type);
    return (*_gtk_container_get_type) ();
}

gint gtk_dialog_run (GtkDialog * dialog)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_dialog_run);
    return (*_gtk_dialog_run) (dialog);
}

GType gtk_dialog_get_type (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_dialog_get_type);
    return (*_gtk_dialog_get_type) ();
}


GtkWidget *gtk_drawing_area_new (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_drawing_area_new);
    return (*_gtk_drawing_area_new) ();
}

gboolean gtk_events_pending (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_events_pending);
    return (*_gtk_events_pending) ();
}

void gtk_file_chooser_add_filter (GtkFileChooser * chooser,
                  GtkFileFilter * filter)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_add_filter);
    (*_gtk_file_chooser_add_filter) (chooser, filter);
}

gchar *gtk_file_chooser_get_filename (GtkFileChooser * chooser)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_get_filename);
    return (*_gtk_file_chooser_get_filename) (chooser);
}

GSList *gtk_file_chooser_get_filenames (GtkFileChooser * chooser)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_get_filenames);
    return (*_gtk_file_chooser_get_filenames) (chooser);
}

GtkFileFilter *gtk_file_chooser_get_filter (GtkFileChooser * chooser)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_get_filter);
    return (*_gtk_file_chooser_get_filter) (chooser);
}

GType gtk_file_chooser_get_type (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_get_type);
    return (*_gtk_file_chooser_get_type) ();
}

gboolean gtk_file_chooser_set_current_folder (GtkFileChooser * chooser,
                          const gchar * filename)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_set_current_folder);
    return (*_gtk_file_chooser_set_current_folder) (chooser, filename);
}

void gtk_file_chooser_set_current_name (GtkFileChooser * chooser,
                    const gchar * name)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_set_current_name);
    (*_gtk_file_chooser_set_current_name) (chooser, name);
}

void gtk_file_chooser_set_do_overwrite_confirmation (GtkFileChooser * chooser,
                             gboolean
                             do_overwrite_confirmation)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_set_do_overwrite_confirmation);
    (*_gtk_file_chooser_set_do_overwrite_confirmation) (chooser,
                            do_overwrite_confirmation);
}

void gtk_file_chooser_set_filter (GtkFileChooser * chooser,
                  GtkFileFilter * filter)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_set_filter);
    (*_gtk_file_chooser_set_filter) (chooser, filter);
}

void gtk_file_chooser_set_select_multiple (GtkFileChooser * chooser,
                       gboolean select_multiple)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_set_select_multiple);
    return (*_gtk_file_chooser_set_select_multiple) (chooser,
                             select_multiple);
}


void gtk_file_filter_add_pattern (GtkFileFilter * filter,
                  const gchar * pattern)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_filter_add_pattern);
    (*_gtk_file_filter_add_pattern) (filter, pattern);
}

GtkFileFilter *gtk_file_filter_new (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_filter_new);
    return (*_gtk_file_filter_new) ();
}

void gtk_file_filter_set_name (GtkFileFilter * filter, const gchar * name)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_file_filter_set_name);
    return (*_gtk_file_filter_set_name) (filter, name);
}

GtkWidget *gtk_fixed_new (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_fixed_new);
    return (*_gtk_fixed_new) ();
}

void gtk_init (int *argc, char ***argv)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_init);
    (*_gtk_init) (argc, argv);
}

void gtk_main_do_event (GdkEvent * event)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_main_do_event);
    (*_gtk_main_do_event) (event);
}

void gtk_main (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_main);
    (*_gtk_main) ();
}

gboolean gtk_main_iteration (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_main_iteration);
    return (*_gtk_main_iteration) ();
}

void gtk_main_quit (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_main_quit);
    (*_gtk_main_quit) ();
}

GtkWidget *gtk_plug_new (GdkNativeWindow socket_id)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_plug_new);
    return (*_gtk_plug_new) (socket_id);
}

void gtk_selection_data_free (GtkSelectionData * data)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_free);
    return (*_gtk_selection_data_free) (data);
}

const guchar *gtk_selection_data_get_data (GtkSelectionData * selection_data)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_get_data);
    return (*_gtk_selection_data_get_data) (selection_data);
}

gint gtk_selection_data_get_length (GtkSelectionData * selection_data)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_get_length);
    return (*_gtk_selection_data_get_length) (selection_data);
}

GdkAtom gtk_selection_data_get_target (GtkSelectionData * selection_data)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_get_target);
    return (*_gtk_selection_data_get_target) (selection_data);
}

void gtk_selection_data_set (GtkSelectionData * selection_data,
                 GdkAtom type,
                 gint format, const guchar * data, gint length)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_set);
    return (*_gtk_selection_data_set) (selection_data, type, format, data,
                       length);
}

gboolean gtk_selection_data_set_pixbuf (GtkSelectionData * selection_data,
                    GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_set_pixbuf);
    return (*_gtk_selection_data_set_pixbuf) (selection_data, pixbuf);
}

gboolean gtk_selection_data_set_text (GtkSelectionData * selection_data,
                      const gchar * str, gint len)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_set_text);
    return (*_gtk_selection_data_set_text) (selection_data, str, len);
}

gboolean gtk_selection_data_set_uris (GtkSelectionData * selection_data,
                      gchar ** uris)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_selection_data_set_uris);
    return (*_gtk_selection_data_set_uris) (selection_data, uris);
}

GtkSettings *gtk_settings_get_default (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_settings_get_default);
    return (*_gtk_settings_get_default) ();
}

void gtk_target_list_add (GtkTargetList * list,
              GdkAtom target, guint flags, guint info)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_list_add);
    (*_gtk_target_list_add) (list, target, flags, info);
}

void gtk_target_list_add_image_targets (GtkTargetList * list,
                    guint info, gboolean writable)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_list_add_image_targets);
    (*_gtk_target_list_add_image_targets) (list, info, writable);
}

void gtk_target_list_add_text_targets (GtkTargetList * list, guint info)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_list_add_text_targets);
    (*_gtk_target_list_add_text_targets) (list, info);
}

GtkTargetList *gtk_target_list_new (const GtkTargetEntry * targets,
                    guint ntargets)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_list_new);
    return (*_gtk_target_list_new) (targets, ntargets);
}

void gtk_target_list_unref (GtkTargetList * list)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_list_unref);
    (*_gtk_target_list_unref) (list);
}

gboolean gtk_targets_include_image (GdkAtom * targets,
                    gint n_targets, gboolean writable)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_targets_include_image);
    return (*_gtk_targets_include_image) (targets, n_targets, writable);
}

gboolean gtk_targets_include_text (GdkAtom * targets, gint n_targets)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_targets_include_text);
    return (*_gtk_targets_include_text) (targets, n_targets);
}

void gtk_target_table_free (GtkTargetEntry * targets, gint n_targets)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_table_free);
    (*_gtk_target_table_free) (targets, n_targets);
}

GtkTargetEntry *gtk_target_table_new_from_list (GtkTargetList * list,
                        gint * n_targets)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_target_table_new_from_list);
    return (*_gtk_target_table_new_from_list) (list, n_targets);
}


void gtk_widget_destroy (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_destroy);
    return (*_gtk_widget_destroy) (widget);
}

GtkWidget *gtk_widget_get_ancestor (GtkWidget * widget, GType widget_type)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_ancestor);
    return (*_gtk_widget_get_ancestor) (widget, widget_type);
}

GdkScreen *gtk_widget_get_screen (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_screen);
    return (*_gtk_widget_get_screen) (widget);
}

void gtk_widget_get_allocation (GtkWidget * widget,
                GtkAllocation * allocation)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_allocation);
    (*_gtk_widget_get_allocation) (widget, allocation);
}



GType gtk_widget_get_type (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_type);
    return (*_gtk_widget_get_type) ();
}

gboolean gtk_widget_get_visible (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_visible);
    return (*_gtk_widget_get_visible) (widget);
}

GdkWindow *gtk_widget_get_window (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_get_window);
    return (*_gtk_widget_get_window) (widget);
}

void gtk_widget_grab_focus (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_grab_focus);
    return (*_gtk_widget_grab_focus) (widget);
}

void gtk_widget_hide (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_hide);
    return (*_gtk_widget_hide) (widget);
}

void gtk_widget_modify_bg (GtkWidget * widget,
               GtkStateType state, const GdkColor * color)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_modify_bg);
    return (*_gtk_widget_modify_bg) (widget, state, color);
}

void gtk_widget_realize (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_realize);
    return (*_gtk_widget_realize) (widget);
}

void gtk_widget_set_app_paintable (GtkWidget * widget, gboolean app_paintable)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_app_paintable);
    return (*_gtk_widget_set_app_paintable) (widget, app_paintable);
}

void gtk_widget_set_can_focus (GtkWidget * widget, gboolean can_focus)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_can_focus);
    return (*_gtk_widget_set_can_focus) (widget, can_focus);
}

void gtk_widget_set_events (GtkWidget * widget, gint events)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_events);
    return (*_gtk_widget_set_events) (widget, events);
}

void gtk_widget_set_size_request (GtkWidget * widget, gint width, gint height)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_size_request);
    return (*_gtk_widget_set_size_request) (widget, width, height);
}

void gtk_widget_show_all (GtkWidget * widget)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_show_all);
    return (*_gtk_widget_show_all) (widget);
}


void gtk_window_deiconify (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_deiconify);
    (*_gtk_window_deiconify) (window);
}

void gtk_window_fullscreen (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_fullscreen);
    (*_gtk_window_fullscreen) (window);
}

gboolean gtk_window_get_decorated (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_get_decorated);
    return (*_gtk_window_get_decorated) (window);
}

void gtk_window_get_position (GtkWindow * window,
                  gint * root_x, gint * root_y)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_get_position);
    (*_gtk_window_get_position) (window, root_x, root_y);
}

void gtk_window_get_size (GtkWindow * window, gint * width, gint * height)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_get_size);
    (*_gtk_window_get_size) (window, width, height);
}

GType gtk_window_get_type (void)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_get_type);
    return (*_gtk_window_get_type) ();
}

void gtk_window_iconify (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_iconify);
    (*_gtk_window_iconify) (window);
}

void gtk_window_maximize (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_maximize);
    (*_gtk_window_maximize) (window);
}

void gtk_window_move (GtkWindow * window, gint x, gint y)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_move);
    (*_gtk_window_move) (window, x, y);
}

GtkWidget *gtk_window_new (GtkWindowType type)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_new);
    return (*_gtk_window_new) (type);
}

void gtk_window_present (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_present);
    (*_gtk_window_present) (window);
}

void gtk_window_resize (GtkWindow * window, gint width, gint height)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_resize);
    (*_gtk_window_resize) (window, width, height);
}

void gtk_window_set_accept_focus (GtkWindow * window, gboolean setting)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_accept_focus);
    return (*_gtk_window_set_accept_focus) (window, setting);
}

void gtk_window_set_decorated (GtkWindow * window, gboolean setting)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_decorated);
    (*_gtk_window_set_decorated) (window, setting);
}

void gtk_window_set_geometry_hints (GtkWindow * window,
                    GtkWidget * geometry_widget,
                    GdkGeometry * geometry,
                    GdkWindowHints geom_mask)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_geometry_hints);
    (*_gtk_window_set_geometry_hints) (window, geometry_widget, geometry,
                       geom_mask);
}

void gtk_window_set_icon (GtkWindow * window, GdkPixbuf * icon)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_icon);
    (*_gtk_window_set_icon) (window, icon);
}

void gtk_window_set_keep_above (GtkWindow * window, gboolean setting)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_keep_above);
    (*_gtk_window_set_keep_above) (window, setting);
}

void gtk_window_set_keep_below (GtkWindow * window, gboolean setting)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_keep_below);
    (*_gtk_window_set_keep_below) (window, setting);
}

void gtk_window_set_modal (GtkWindow * window, gboolean modal)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_modal);
    (*_gtk_window_set_modal) (window, modal);
}

void gtk_window_set_opacity (GtkWindow * window, gdouble opacity)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_opacity);
    (*_gtk_window_set_opacity) (window, opacity);
}

void gtk_window_set_title (GtkWindow * window, const gchar * title)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_title);
    (*_gtk_window_set_title) (window, title);
}

void gtk_window_set_transient_for (GtkWindow * window, GtkWindow * parent)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_transient_for);
    (*_gtk_window_set_transient_for) (window, parent);
}

void gtk_window_set_type_hint (GtkWindow * window, GdkWindowTypeHint hint)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_type_hint);
    (*_gtk_window_set_type_hint) (window, hint);
}

void gtk_window_set_wmclass (GtkWindow * window,
                 const gchar * wmclass_name,
                 const gchar * wmclass_class)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_set_wmclass);
    (*_gtk_window_set_wmclass) (window, wmclass_name, wmclass_class);
}

void gtk_window_unfullscreen (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_unfullscreen);
    (*_gtk_window_unfullscreen) (window);
}

void gtk_window_unmaximize (GtkWindow * window)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_window_unmaximize);
    return (*_gtk_window_unmaximize) (window);
}

//--------------------------------------------------------------------------------------

void gtk_widget_set_colormap (GtkWidget * widget,
    // 2 only
                     GdkColormap * colormap)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_colormap);
    (*_gtk_widget_set_colormap) (widget, colormap);
}

//-------- GTK 3 only -------------------------------------------

void gtk_widget_shape_combine_region (GtkWidget *widget,
                             cairo_region_t *region)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_shape_combine_region);
    (*_gtk_widget_shape_combine_region)(widget, region);
}

void gtk_widget_input_shape_combine_region (GtkWidget *widget,
                                       cairo_region_t *region)
{
    CHECK_LOAD_SYMBOL_GTK (gtk_widget_input_shape_combine_region);
    (*_gtk_widget_input_shape_combine_region)(widget, region);
}

//-------- Glass utility ----------------------------------------

void
glass_widget_set_visual(GtkWidget *widget, GdkVisual *visual)
{
    if (wrapper_gtk_version == 2) {
        CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_colormap);
        GdkColormap *colormap = gdk_colormap_new(visual, TRUE); //2.0 only
        (*_gtk_widget_set_colormap) (widget, colormap);
    } else { // v3.0
        CHECK_LOAD_SYMBOL_GTK (gtk_widget_set_visual);
        (*_gtk_widget_set_visual) (widget, visual);
    }
}

GtkWidget *
glass_file_chooser_dialog (const gchar * title,
                           GtkWindow * parent,
                           GtkFileChooserAction action,
                           const gchar *action_text
       ) {

    // Note: wrapped because G_GNUC_NULL_TERMINATED

    CHECK_LOAD_SYMBOL_GTK (gtk_file_chooser_dialog_new);
    return (*_gtk_file_chooser_dialog_new)(title,
        parent,
        action,

        GTK_STOCK_CANCEL,
        GTK_RESPONSE_CANCEL,

        action_text,
        GTK_RESPONSE_ACCEPT,

        NULL);

    return NULL;
}
