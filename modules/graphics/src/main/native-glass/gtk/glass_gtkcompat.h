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
#ifndef GLASS_GTKCOMPAT_H
#define        GLASS_GTKCOMPAT_H

#include <gtk/gtk.h>
#include <gdk/gdkx.h>
#include <gdk/gdkkeysyms.h>

#if GTK_CHECK_VERSION(2, 22, 0)

#define GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(context) \
    gdk_drag_context_get_selected_action(context)

#define GLASS_GDK_DRAG_CONTEXT_GET_ACTIONS(context) \
    gdk_drag_context_get_actions(context)

#define GLASS_GDK_DRAG_CONTEXT_LIST_TARGETS(context) \
    gdk_drag_context_list_targets(context)

#define GLASS_GDK_DRAG_CONTEXT_GET_SUGGESTED_ACTION(context) \
    gdk_drag_context_get_suggested_action(context)

#else /* GTK_CHECK_VERSION(2, 22, 0) */

#define GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(context) \
    (context->action)

#define GLASS_GDK_DRAG_CONTEXT_GET_ACTIONS(context) \
    (context->actions)

#define GLASS_GDK_DRAG_CONTEXT_LIST_TARGETS(context) \
    (context->targets)

#define GLASS_GDK_DRAG_CONTEXT_GET_SUGGESTED_ACTION(context) \
    (context->suggested_action)

#endif /* GTK_CHECK_VERSION(2, 22, 0) */

#if GTK_CHECK_VERSION(2, 24, 0)

#define GLASS_GDK_KEY_CONSTANT(key) (GDK_KEY_ ## key)

#define GLASS_GDK_WINDOW_FOREIGN_NEW_FOR_DISPLAY(display, anid) \
    gdk_x11_window_foreign_new_for_display(display, anid)

#define GLASS_GDK_WINDOW_LOOKUP_FOR_DISPLAY(display, anid) \
    gdk_x11_window_lookup_for_display(display, anid)

#else /* GTK_CHECK_VERSION(2, 24, 0) */

#define GLASS_GDK_KEY_CONSTANT(key) (GDK_ ## key)

#define GLASS_GDK_WINDOW_FOREIGN_NEW_FOR_DISPLAY(display, anid) \
    gdk_window_foreign_new_for_display(display, anid)

#define GLASS_GDK_WINDOW_LOOKUP_FOR_DISPLAY(display, anid) \
    gdk_window_lookup_for_display(display, anid)

#endif /* GTK_CHECK_VERSION(2, 24, 0) */

#if GTK_CHECK_VERSION(3, 0, 0)

#define GLASS_GTK_WINDOW_SET_HAS_RESIZE_GRIP(window, value) \
    gtk_window_set_has_resize_grip(window, TRUE)

#define GLASS_GDK_SELECTION_EVENT_GET_REQUESTOR(event) \
    (event->requestor)

#define GLASS_GDK_DRAG_CONTEXT_GET_DEST_WINDOW(context) \
    gdk_drag_context_get_dest_window(context)

#else /* GTK_CHECK_VERSION(3, 0, 0) */

#define GLASS_GTK_WINDOW_SET_HAS_RESIZE_GRIP(window, value) \
    (void) window;                                          \
    (void) value;

#define GLASS_GDK_SELECTION_EVENT_GET_REQUESTOR(event) \
    GLASS_GDK_WINDOW_FOREIGN_NEW_FOR_DISPLAY(          \
        gdk_display_get_default(), event->requestor)

#define GLASS_GDK_DRAG_CONTEXT_GET_DEST_WINDOW(context) \
    ((context != NULL) ? context->dest_window : NULL)

#endif /* GTK_CHECK_VERSION(3, 0, 0) */

GdkScreen * glass_gdk_window_get_screen(GdkWindow * gdkWindow);
GdkDisplay * glass_gdk_window_get_display(GdkWindow * gdkWindow);

gboolean glass_gdk_mouse_devices_grab(GdkWindow * gdkWindow);
gboolean glass_gdk_mouse_devices_grab_with_cursor(GdkWindow * gdkWindow, GdkCursor *cursor);
gboolean glass_gdk_mouse_devices_grab_with_cursor(GdkWindow * gdkWindow, GdkCursor *cursor, gboolean owner_events);
void glass_gdk_mouse_devices_ungrab();

void glass_gdk_master_pointer_grab(GdkWindow *window, GdkCursor *cursor);
void glass_gdk_master_pointer_ungrab();
void glass_gdk_master_pointer_get_position(gint *x, gint *y);

gboolean glass_gdk_device_is_grabbed(GdkDevice *device);
void glass_gdk_device_ungrab(GdkDevice *device);
GdkWindow *glass_gdk_device_get_window_at_position(
               GdkDevice *device, gint *x, gint *y);

void glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                                  gboolean transparent);

const guchar * glass_gtk_selection_data_get_data_with_length(
        GtkSelectionData * selectionData,
        gint * length);

void glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual);

int glass_gtk_fixup_typed_key(int key, int keyval);

void glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h);

void glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y);


#endif        /* GLASS_GTKCOMPAT_H */

