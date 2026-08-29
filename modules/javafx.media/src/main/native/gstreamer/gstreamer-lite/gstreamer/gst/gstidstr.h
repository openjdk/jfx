/* GStreamer
 *
 * Copyright (C) 2024 Sebastian Dröge <sebastian@centricular.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#ifndef __GST_ID_STR_H__
#define __GST_ID_STR_H__

#include <gst/gstconfig.h>
#include <glib-object.h>

G_BEGIN_DECLS

/**
 * GstIdStr:
 *
 * String type optimized for short strings.
 *
 * Strings are usually stack- or inline-allocated, and for short strings smaller
 * than 16 bytes (including NUL terminator) no heap allocations are performed.
 *
 * Since: 1.26
 */
typedef struct {
  /* < private > */
  gpointer pointer;
#if GLIB_SIZEOF_VOID_P == 8
  guint8 padding[8];
#elif GLIB_SIZEOF_VOID_P == 4
  guint8 padding[12];
#else
  #error "Only 32 bit and 64 bit pointers supported currently"
#endif
} GstIdStr;

/**
 * GST_ID_STR_INIT:
 *
 * Initializer for #GstIdStr.
 *
 * Since: 1.26
 */
#define GST_ID_STR_INIT { .pointer = NULL, .padding = {0, } }

GST_API
GType gst_id_str_get_type (void);

GST_API
gsize gst_id_str_get_len (const GstIdStr *s) G_GNUC_PURE;

GST_API
void gst_id_str_set (GstIdStr *s, const gchar *value);

GST_API
void gst_id_str_set_with_len (GstIdStr *s, const gchar *value, gsize len);

GST_API
void gst_id_str_set_static_str (GstIdStr *s, const gchar *value);

GST_API
void gst_id_str_set_static_str_with_len (GstIdStr *s, const gchar *value, gsize len);

GST_API
void gst_id_str_init (GstIdStr *s);

GST_API
void gst_id_str_clear (GstIdStr *s);

GST_API
GstIdStr * gst_id_str_new (void) G_GNUC_MALLOC G_GNUC_WARN_UNUSED_RESULT;

GST_API
GstIdStr * gst_id_str_copy (const GstIdStr *s) G_GNUC_MALLOC G_GNUC_WARN_UNUSED_RESULT;

GST_API
void gst_id_str_free (GstIdStr *s);

GST_API
void gst_id_str_copy_into (GstIdStr *d, const GstIdStr *s);

GST_API
void gst_id_str_move (GstIdStr *d, GstIdStr *s);

GST_API
const gchar * gst_id_str_as_str (const GstIdStr *s) G_GNUC_PURE;

GST_API
gboolean gst_id_str_is_equal (const GstIdStr *s1, const GstIdStr *s2) G_GNUC_PURE;

GST_API
gboolean gst_id_str_is_equal_to_str (const GstIdStr *s1, const gchar *s2) G_GNUC_PURE;

GST_API
gboolean gst_id_str_is_equal_to_str_with_len (const GstIdStr *s1, const gchar *s2, gsize len) G_GNUC_PURE;

G_END_DECLS

#endif

