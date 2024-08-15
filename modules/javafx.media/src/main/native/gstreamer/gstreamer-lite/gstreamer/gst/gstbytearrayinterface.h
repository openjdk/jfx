/* Copyright (C) 2023 Netflix Inc.
 *  Author: Xavier Claessens <xavier.claessens@collabora.com>
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

#pragma once

#include <glib.h>
#include <gst/gstconfig.h>

G_BEGIN_DECLS

/**
 * GstByteArrayInterface:
 * @data: A pointer to an array of bytes.
 * @len: Number of bytes in @data.
 * @resize: Reallocate @data.
 *
 * Interface for an array of bytes. It is expected to be subclassed to implement
 * @resize virtual method using language native array implementation, such as
 * GLib's #GByteArray, C++'s `std::vector<uint8_t>` or Rust's `Vec<u8>`.
 *
 * @resize implementation could allocate more than requested to avoid repeated
 * reallocations. It can return %FALSE, or be set to %NULL, in the case the
 * array cannot grow.
 *
 * Since: 1.24
 */
typedef struct _GstByteArrayInterface GstByteArrayInterface;
struct _GstByteArrayInterface
{
  guint8 *data;
  gsize len;
  gboolean (*resize) (GstByteArrayInterface *self, gsize length);

  /* < private > */
  gpointer _gst_reserved[GST_PADDING];
};

/**
 * gst_byte_array_interface_init:
 * @self: A #GstByteArrayInterface.
 * @length: New size.
 *
 * Initialize #GstByteArrayInterface structure.
 *
 * Since: 1.24
 */
static inline void
gst_byte_array_interface_init (GstByteArrayInterface *self)
{
  memset (self, 0, sizeof (GstByteArrayInterface));
}

/**
 * gst_byte_array_interface_set_size:
 * @self: A #GstByteArrayInterface.
 * @length: New size.
 *
 * Reallocate data pointer to fit at least @length bytes. @self->len is updated
 * to @length.
 *
 * Returns: %TRUE on success, %FALSE otherwise.
 * Since: 1.24
 */
static inline gboolean
gst_byte_array_interface_set_size (GstByteArrayInterface *self, gsize length)
{
  if (self->resize == NULL || !self->resize (self, length))
    return FALSE;
  self->len = length;
  return TRUE;
}

/**
 * gst_byte_array_interface_append:
 * @self: A #GstByteArrayInterface.
 * @size: Number of bytes to append to the array.
 *
 * Grow the array by @size bytes and return a pointer to the newly added memory.
 *
 * Returns: Pointer to added memory, or %NULL if reallocation failed.
 * Since: 1.24
 */
static inline guint8 *
gst_byte_array_interface_append (GstByteArrayInterface *self, gsize size)
{
  gsize orig = self->len;
  if (!gst_byte_array_interface_set_size (self, self->len + size))
    return NULL;
  return self->data + orig;
}

/**
 * gst_byte_array_interface_append_data:
 * @self: A #GstByteArrayInterface.
 * @data: Source data.
 * @size: Size of @data.
 *
 * Append @size bytes from @data, reallocating @self->data pointer if necessary.
 *
 * Returns: %TRUE on success, %FALSE otherwise.
 * Since: 1.24
 */
static inline gboolean
gst_byte_array_interface_append_data (GstByteArrayInterface *self, const guint8 *data, gsize size)
{
  guint8 *ptr = gst_byte_array_interface_append (self, size);
  if (ptr == NULL)
    return FALSE;
  memcpy (ptr, data, size);
  return TRUE;
}

G_END_DECLS
