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

#ifndef __GST_ID_STR_PRIVATE_H__
#define __GST_ID_STR_PRIVATE_H__

#include <gst/gstconfig.h>
#include <gst/gstidstr.h>
#include <glib-object.h>
#include <string.h>

G_BEGIN_DECLS

typedef struct {
  /* < private > */
  union {
    struct {
      guint8 padding[15];
      // t == 0: Inline-allocated short_string
      // t == 1: Heap-allocated pointer_string that needs freeing
      // t == 2: Statically allocated pointer_string that needs no freeing
      guint8 t;
    } string_type;
    struct {
      gchar s[16];
      // t == 0 is the NUL terminator
    } short_string;
    struct {
      gchar *s;           // to be freed if t == 1
      guint32 len;       // Length of the string without NUL-terminator
#if GLIB_SIZEOF_VOID_P == 8
      guint8 padding[3]; // always zero
#elif GLIB_SIZEOF_VOID_P == 4
      guint8 padding[7]; // always zero
#else
  #error "Only 32 bit and 64 bit pointers supported currently"
#endif
      guint8 t; // always 1 or 2, see above
    } pointer_string;
  } s;
} GstIdStrPrivate;

static inline void
_gst_id_str_init_inline (GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;
  memset (sp, 0, sizeof (*sp));
}

static inline gsize
_gst_id_str_get_len (const GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;

  switch (sp->s.string_type.t) {
    case 0:
      return strlen (sp->s.short_string.s);
    case 1:
    case 2:
      return sp->s.pointer_string.len;
    default:
      g_assert_not_reached ();
      return 0;
  }
}

static inline void
_gst_id_str_set_with_len_inline (GstIdStr * s, const gchar * value, gsize len)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;

  g_assert (len <= G_MAXUINT32);

  if (sp->s.string_type.t == 1) {
    if (sp->s.pointer_string.s == value)
      return;
    g_free (sp->s.pointer_string.s);
  }

  if (len <= 15) {
    memcpy (sp->s.short_string.s, value, len);
    memset (&sp->s.short_string.s[len], 0, 16 - len);
  } else {
    sp->s.pointer_string.t = 1;
    sp->s.pointer_string.len = len;
    sp->s.pointer_string.s = (gchar *) g_malloc (len + 1);
    memcpy (sp->s.pointer_string.s, value, len);
    sp->s.pointer_string.s[len] = '\0';
  }
}

static inline void
_gst_id_str_set_inline (GstIdStr * s, const gchar * value)
{
  gsize len = strlen (value);
  _gst_id_str_set_with_len_inline (s, value, len);
}

static inline void
_gst_id_str_set_static_str_with_len_inline (GstIdStr * s, const gchar * value, gsize len)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;

  g_assert (len <= G_MAXUINT32);

  if (sp->s.string_type.t == 1)
    g_free (sp->s.pointer_string.s);

  if (len <= 15) {
    memcpy (sp->s.short_string.s, value, len);
    memset (&sp->s.short_string.s[len], 0, 16 - len);
  } else {
    sp->s.pointer_string.t = 2;
    sp->s.pointer_string.len = len;
    sp->s.pointer_string.s = (gchar *) value;
  }
}

static inline void
_gst_id_str_set_static_str_inline (GstIdStr * s, const gchar * value)
{
  gsize len = strlen (value);
  _gst_id_str_set_static_str_with_len_inline (s, value, len);
}

static inline void
_gst_id_str_clear_inline (GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;

  if (sp->s.string_type.t == 1) {
    g_free (sp->s.pointer_string.s);
  }
  memset (sp, 0, sizeof (*sp));
}

static inline void
_gst_id_str_copy_into_inline (GstIdStr * d,
    const GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;
  GstIdStrPrivate *dp = (GstIdStrPrivate *) d;

  _gst_id_str_clear_inline (d);

  *dp = *sp;
  if (dp->s.string_type.t == 1) {
#if GLIB_CHECK_VERSION (2, 68, 0)
      dp->s.pointer_string.s = (gchar *) g_memdup2 (dp->s.pointer_string.s, dp->s.pointer_string.len + 1);
#else
      dp->s.pointer_string.s = (gchar *) g_memdup (dp->s.pointer_string.s, dp->s.pointer_string.len + 1);
#endif
  }
}

static inline void
_gst_id_str_move_inline (GstIdStr * d,
    GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;
  GstIdStrPrivate *dp = (GstIdStrPrivate *) d;

  _gst_id_str_clear_inline (d);

  memcpy (dp, s, sizeof (*sp));
  memset (sp, 0, sizeof (*sp));
}

static inline GstIdStr *
_gst_id_str_new_inline (void)
{
  return (GstIdStr *) g_new0 (GstIdStrPrivate, 1);
}

static inline GstIdStr *
_gst_id_str_copy_inline (const GstIdStr * s)
{
  GstIdStr *copy = _gst_id_str_new_inline ();

  _gst_id_str_copy_into_inline (copy, s);

  return copy;
}

static inline void
_gst_id_str_free_inline (GstIdStr * s)
{
  _gst_id_str_clear_inline (s);
  g_free (s);
}

static inline const gchar *
_gst_id_str_as_str_inline (const GstIdStr * s)
{
  GstIdStrPrivate *sp = (GstIdStrPrivate *) s;

  switch (sp->s.string_type.t) {
    case 0:
      return sp->s.short_string.s;
    case 1:
    case 2:
      return sp->s.pointer_string.s;
    default:
      g_assert_not_reached ();
      return NULL;
  }
}

static inline gboolean
_gst_id_str_is_equal_inline (const GstIdStr * s1,
    const GstIdStr * s2)
{
  GstIdStrPrivate *sp1 = (GstIdStrPrivate *) s1;
  GstIdStrPrivate *sp2 = (GstIdStrPrivate *) s2;

  // Covers the short_string case and equal pointer_string pointers
  if (sp1 == sp2 || memcmp (sp1, sp2, sizeof (*sp1)) == 0)
    return TRUE;

  // If one of the strings is a short_string then they can't be equal at this
  // point: either they're both short_strings and not the same, or one is a
  // short_string and the other a pointer_string which would mean that they have
  // different lengths.
  if (sp1->s.string_type.t == 0 || sp2->s.string_type.t == 0)
    return FALSE;

  // Otherwise they're both pointer_strings
  if (sp1->s.pointer_string.len != sp2->s.pointer_string.len)
    return FALSE;
  return memcmp (sp1->s.pointer_string.s, sp2->s.pointer_string.s, sp1->s.pointer_string.len) == 0;
}

static inline gboolean
_gst_id_str_is_equal_to_str_inline (const GstIdStr * s1,
    const gchar * s2)
{
  return strcmp (_gst_id_str_as_str_inline (s1), s2) == 0;
}

static inline gboolean
_gst_id_str_is_equal_to_str_with_len_inline (const GstIdStr * s1,
    const gchar * s2, gsize len)
{
  GstIdStr s2_int = GST_ID_STR_INIT;

  _gst_id_str_set_static_str_with_len_inline (&s2_int, s2, len);
  return _gst_id_str_is_equal_inline (s1, &s2_int);
}

#ifndef GST_ID_STR_DISABLE_INLINES
#define gst_id_str_init(s) _gst_id_str_init_inline(s)
#define gst_id_str_get_len(s) _gst_id_str_get_len(s)
#define gst_id_str_set(s, value) _gst_id_str_set_inline(s, value)
#define gst_id_str_set_with_len(s, value, len) _gst_id_str_set_with_len_inline(s, value, len)
#define gst_id_str_set_static_str(s, value) _gst_id_str_set_static_str_inline(s, value)
#define gst_id_str_set_static_str_with_len(s, value, len) _gst_id_str_set_static_str_with_len_inline(s, value, len)
#define gst_id_str_clear(s) _gst_id_str_clear_inline(s)
#define gst_id_str_copy(s) _gst_id_str_copy_inline(s)
#define gst_id_str_new() _gst_id_str_new_inline()
#define gst_id_str_free(s) _gst_id_str_free_inline(s)
#define gst_id_str_copy_into(d, s) _gst_id_str_copy_into_inline(d, s)
#define gst_id_str_move(d, s) _gst_id_str_move_inline(d, s)
#define gst_id_str_is_equal(s1, s2) _gst_id_str_is_equal_inline(s1, s2)
#define gst_id_str_is_equal_to_str(s1, s2) _gst_id_str_is_equal_to_str_inline(s1, s2)
#define gst_id_str_is_equal_to_str_with_len(s1, s2, len) _gst_id_str_is_equal_to_str_with_len_inline(s1, s2, len)
#define gst_id_str_as_str(s) _gst_id_str_as_str_inline(s)
#endif

G_END_DECLS

#endif
