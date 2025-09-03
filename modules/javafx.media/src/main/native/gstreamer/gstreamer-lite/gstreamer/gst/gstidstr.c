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

/**
 * SECTION:gstidstr
 * @title: GstIdStr
 * @short_description: String type optimized for short strings
 * @see_also: #GstStructure
 *
 * A #GstIdStr is string type optimized for short strings and used for structure
 * names, structure field names and in other places.
 *
 * Strings up to 16 bytes (including NUL terminator) are stored inline, other
 * strings are stored on the heap.
 *
 * ```cpp
 * GstIdStr s = GST_ID_STR_INIT;
 *
 * gst_id_str_set (&s, "Hello, World!");
 * g_print ("%s\n", gst_id_str_as_str (&s));
 *
 * gst_id_str_clear (&s);
 * ```
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#define GST_ID_STR_DISABLE_INLINES

#include "gstidstr.h"
#include "gstidstr-private.h"

G_DEFINE_BOXED_TYPE (GstIdStr, gst_id_str, gst_id_str_copy, gst_id_str_free);

// Make sure the string type fields are in the same place for each variant
// See https://developercommunity.visualstudio.com/t/C-offsetof-an-array-item-is-accepted-by/10736612
#ifndef _MSC_VER
G_STATIC_ASSERT (G_STRUCT_OFFSET (GstIdStrPrivate,
        s.string_type.t) == G_STRUCT_OFFSET (GstIdStrPrivate,
        s.short_string.s[15]));
#endif
G_STATIC_ASSERT (G_STRUCT_OFFSET (GstIdStrPrivate,
        s.string_type.t) == G_STRUCT_OFFSET (GstIdStrPrivate,
        s.pointer_string.t));
// The overall struct should be 16 bytes large and at least pointer aligned
G_STATIC_ASSERT (sizeof (GstIdStrPrivate) == 16);
// See https://developercommunity.visualstudio.com/t/C-offsetof-an-array-item-is-accepted-by/10736612
#ifndef _MSC_VER
G_STATIC_ASSERT (G_ALIGNOF (GstIdStrPrivate) >= G_ALIGNOF (gpointer));
// Alignment and size of the private and public type must be the same
G_STATIC_ASSERT (G_ALIGNOF (GstIdStrPrivate) == G_ALIGNOF (GstIdStr));
#endif
G_STATIC_ASSERT (sizeof (GstIdStrPrivate) == sizeof (GstIdStr));

/**
 * gst_id_str_init:
 * @s: A %GstIdStr
 *
 * Initializes a (usually stack-allocated) id string @s. The newly-initialized
 * id string will contain an empty string by default as value.
 *
 * Since: 1.26
 */
void
gst_id_str_init (GstIdStr * s)
{
  _gst_id_str_init_inline (s);
}

/**
 * gst_id_str_get_len:
 * @s: A %GstIdStr
 *
 * Returns the length of @s, exluding the NUL-terminator. This is equivalent to
 * calling `strcmp()` but potentially faster.
 *
 * Since: 1.26
 */
gsize
gst_id_str_get_len (const GstIdStr * s)
{
  return _gst_id_str_get_len (s);
}

/**
 * gst_id_str_set:
 * @s: A %GstIdStr
 * @value: A NUL-terminated string
 *
 * Sets @s to the string @value.
 *
 * Since: 1.26
 */
void
gst_id_str_set (GstIdStr * s, const gchar * value)
{
  _gst_id_str_set_inline (s, value);
}

/**
 * gst_id_str_set_with_len:
 * @s: A %GstIdStr
 * @value: A string
 * @len: Length of the string
 *
 * Sets @s to the string @value of length @len. @value does not have to be
 * NUL-terminated and @len should not include the NUL-terminator.
 *
 * Since: 1.26
 */
void
gst_id_str_set_with_len (GstIdStr * s, const gchar * value, gsize len)
{
  _gst_id_str_set_with_len_inline (s, value, len);
}

/**
 * gst_id_str_set_static_str:
 * @s: A %GstIdStr
 * @value: A NUL-terminated string
 *
 * Sets @s to the string @value. @value needs to be valid for the remaining
 * lifetime of the process, e.g. has to be a static string.
 *
 * Since: 1.26
 */
void
gst_id_str_set_static_str (GstIdStr * s, const gchar * value)
{
  _gst_id_str_set_static_str_inline (s, value);
}

/**
 * gst_id_str_set_static_str_with_len:
 * @s: A %GstIdStr
 * @value: A string
 * @len: Length of the string
 *
 * Sets @s to the string @value of length @len. @value needs to be valid for the
 * remaining lifetime of the process, e.g. has to be a static string.
 *
 * @value must be NUL-terminated and @len should not include the
 * NUL-terminator.
 *
 * Since: 1.26
 */
void
gst_id_str_set_static_str_with_len (GstIdStr * s, const gchar * value,
    gsize len)
{
  _gst_id_str_set_static_str_with_len_inline (s, value, len);
}

/**
 * gst_id_str_clear:
 * @s: A %GstIdStr
 *
 * Clears @s and sets it to the empty string.
 *
 * Since: 1.26
 */
void
gst_id_str_clear (GstIdStr * s)
{
  _gst_id_str_clear_inline (s);
}

/**
 * gst_id_str_copy:
 * @s: A %GstIdStr
 *
 * Copies @s into newly allocated heap memory.
 *
 * Returns: (transfer full): A heap-allocated copy of @s.
 *
 * Since: 1.26
 */
GstIdStr *
gst_id_str_copy (const GstIdStr * s)
{
  return _gst_id_str_copy_inline (s);
}

/**
 * gst_id_str_new:
 *
 * Returns a newly heap allocated empty string.
 *
 * Returns: (transfer full): A heap-allocated string.
 *
 * Since: 1.26
 */
GstIdStr *
gst_id_str_new (void)
{
  return _gst_id_str_new_inline ();
}

/**
 * gst_id_str_free:
 * @s: A heap allocated %GstIdStr
 *
 * Frees @s. This should only be called for heap-allocated #GstIdStr.
 *
 * Since: 1.26
 */
void
gst_id_str_free (GstIdStr * s)
{
  _gst_id_str_free_inline (s);
}

/**
 * gst_id_str_copy_into:
 * @d: The destination %GstIdStr
 * @s: The source %GstIdStr
 *
 * Copies @s into @d.
 *
 * Since: 1.26
 */
void
gst_id_str_copy_into (GstIdStr * d, const GstIdStr * s)
{
  _gst_id_str_copy_into_inline (d, s);
}

/**
 * gst_id_str_move:
 * @d: The destination %GstIdStr
 * @s: The source %GstIdStr
 *
 * Moves @s into @d and resets @s.
 *
 * Since: 1.26
 */
void
gst_id_str_move (GstIdStr * d, GstIdStr * s)
{
  _gst_id_str_move_inline (d, s);
}

/**
 * gst_id_str_is_equal:
 * @s1: A %GstIdStr
 * @s2: A %GstIdStr
 *
 * Compares @s1 and @s2 for equality.
 *
 * Returns: %TRUE if @s1 and @s2 are equal.
 *
 * Since: 1.26
 */
gboolean
gst_id_str_is_equal (const GstIdStr * s1, const GstIdStr * s2)
{
  return _gst_id_str_is_equal_inline (s1, s2);
}

/**
 * gst_id_str_is_equal_to_str:
 * @s1: A %GstIdStr
 * @s2: A string
 *
 * Compares @s1 and @s2 for equality.
 *
 * Returns: %TRUE if @s1 and @s2 are equal.
 *
 * Since: 1.26
 */
gboolean
gst_id_str_is_equal_to_str (const GstIdStr * s1, const gchar * s2)
{
  return _gst_id_str_is_equal_to_str_inline (s1, s2);
}

/**
 * gst_id_str_is_equal_to_str_with_len:
 * @s1: A %GstIdStr
 * @s2: A string
 * @len: Length of @s2.
 *
 * Compares @s1 and @s2 with length @len for equality. @s2 does not have to be
 * NUL-terminated and @len should not include the NUL-terminator.
 *
 * This is generally faster than gst_id_str_is_equal_to_str() if the length is
 * already known.
 *
 * Returns: %TRUE if @s1 and @s2 are equal.
 *
 * Since: 1.26
 */
gboolean
gst_id_str_is_equal_to_str_with_len (const GstIdStr * s1, const gchar * s2,
    gsize len)
{
  return _gst_id_str_is_equal_to_str_with_len_inline (s1, s2, len);
}

/**
 * gst_id_str_as_str:
 * @s: A %GstIdStr
 *
 * Returns: the NUL-terminated string representation of @s.
 *
 * Since: 1.26
 */
const gchar *
gst_id_str_as_str (const GstIdStr * s)
{
  return _gst_id_str_as_str_inline (s);
}
