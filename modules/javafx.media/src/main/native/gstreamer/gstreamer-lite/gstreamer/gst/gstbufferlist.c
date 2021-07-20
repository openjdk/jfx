/* GStreamer
 * Copyright (C) 2009 Axis Communications <dev-gstreamer at axis dot com>
 * @author Jonas Holmberg <jonas dot holmberg at axis dot com>
 * Copyright (C) 2014 Tim-Philipp Müller <tim at centricular dot com>
 *
 * gstbufferlist.c: Buffer list
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
 * SECTION:gstbufferlist
 * @title: GstBufferList
 * @short_description: Lists of buffers for data-passing
 * @see_also: #GstPad, #GstMiniObject
 *
 * Buffer lists are an object containing a list of buffers.
 *
 * Buffer lists are created with gst_buffer_list_new() and filled with data
 * using a gst_buffer_list_insert().
 *
 * Buffer lists can be pushed on a srcpad with gst_pad_push_list(). This is
 * interesting when multiple buffers need to be pushed in one go because it
 * can reduce the amount of overhead for pushing each buffer individually.
 */
#define GST_DISABLE_MINIOBJECT_INLINE_FUNCTIONS
#include "gst_private.h"

#include "gstbuffer.h"
#include "gstbufferlist.h"
#include "gstutils.h"

#define GST_CAT_DEFAULT GST_CAT_BUFFER_LIST

#define GST_BUFFER_LIST_IS_USING_DYNAMIC_ARRAY(list) \
    ((list)->buffers != &(list)->arr[0])

/**
 * GstBufferList:
 *
 * Opaque list of grouped buffers.
 */
struct _GstBufferList
{
  GstMiniObject mini_object;

  GstBuffer **buffers;
  guint n_buffers;
  guint n_allocated;

  gsize slice_size;

  /* one-item array, in reality more items are pre-allocated
   * as part of the GstBufferList structure, and that
   * pre-allocated array extends beyond the declared struct */
  GstBuffer *arr[1];
};

GType _gst_buffer_list_type = 0;

GST_DEFINE_MINI_OBJECT_TYPE (GstBufferList, gst_buffer_list);

void
_priv_gst_buffer_list_initialize (void)
{
  _gst_buffer_list_type = gst_buffer_list_get_type ();
}

static GstBufferList *
_gst_buffer_list_copy (GstBufferList * list)
{
  GstBufferList *copy;
  guint i, len;

  len = list->n_buffers;
  copy = gst_buffer_list_new_sized (list->n_allocated);

  /* add and ref all buffers in the array */
  for (i = 0; i < len; i++) {
    copy->buffers[i] = gst_buffer_ref (list->buffers[i]);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (copy->buffers[i]),
        GST_MINI_OBJECT_CAST (copy));
  }

  copy->n_buffers = len;

  return copy;
}

static void
_gst_buffer_list_free (GstBufferList * list)
{
  guint i, len;
  gsize slice_size;

  GST_LOG ("free %p", list);

  /* unrefs all buffers too */
  len = list->n_buffers;
  for (i = 0; i < len; i++) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (list->buffers[i]),
        GST_MINI_OBJECT_CAST (list));
    gst_buffer_unref (list->buffers[i]);
  }

  if (GST_BUFFER_LIST_IS_USING_DYNAMIC_ARRAY (list))
    g_free (list->buffers);

  slice_size = list->slice_size;

#ifdef USE_POISONING
  memset (list, 0xff, slice_size);
#endif

  g_slice_free1 (slice_size, list);
}

static void
gst_buffer_list_init (GstBufferList * list, guint n_allocated, gsize slice_size)
{
  gst_mini_object_init (GST_MINI_OBJECT_CAST (list), 0, _gst_buffer_list_type,
      (GstMiniObjectCopyFunction) _gst_buffer_list_copy, NULL,
      (GstMiniObjectFreeFunction) _gst_buffer_list_free);

  list->buffers = &list->arr[0];
  list->n_buffers = 0;
  list->n_allocated = n_allocated;
  list->slice_size = slice_size;

  GST_LOG ("init %p", list);
}

/**
 * gst_buffer_list_new_sized:
 * @size: an initial reserved size
 *
 * Creates a new, empty #GstBufferList. The caller is responsible for unreffing
 * the returned #GstBufferList. The list will have @size space preallocated so
 * that memory reallocations can be avoided.
 *
 * Free-function: gst_buffer_list_unref
 *
 * Returns: (transfer full): the new #GstBufferList. gst_buffer_list_unref()
 *     after usage.
 */
GstBufferList *
gst_buffer_list_new_sized (guint size)
{
  GstBufferList *list;
  gsize slice_size;
  guint n_allocated;

  if (size == 0)
    size = 1;

  n_allocated = GST_ROUND_UP_16 (size);

  slice_size = sizeof (GstBufferList) + (n_allocated - 1) * sizeof (gpointer);

  list = g_slice_alloc0 (slice_size);

  GST_LOG ("new %p", list);

  gst_buffer_list_init (list, n_allocated, slice_size);

  return list;
}

/**
 * gst_buffer_list_new:
 *
 * Creates a new, empty #GstBufferList. The caller is responsible for unreffing
 * the returned #GstBufferList.
 *
 * Free-function: gst_buffer_list_unref
 *
 * Returns: (transfer full): the new #GstBufferList. gst_buffer_list_unref()
 *     after usage.
 */
GstBufferList *
gst_buffer_list_new (void)
{
  return gst_buffer_list_new_sized (8);
}

/**
 * gst_buffer_list_length:
 * @list: a #GstBufferList
 *
 * Returns the number of buffers in @list.
 *
 * Returns: the number of buffers in the buffer list
 */
guint
gst_buffer_list_length (GstBufferList * list)
{
  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), 0);

  return list->n_buffers;
}

static inline void
gst_buffer_list_remove_range_internal (GstBufferList * list, guint idx,
    guint length, gboolean unref_old)
{
  guint i;

  if (unref_old) {
    for (i = idx; i < idx + length; ++i) {
      gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (list->buffers[i]),
          GST_MINI_OBJECT_CAST (list));
      gst_buffer_unref (list->buffers[i]);
    }
  }

  if (idx + length != list->n_buffers) {
    memmove (&list->buffers[idx], &list->buffers[idx + length],
        (list->n_buffers - (idx + length)) * sizeof (void *));
  }

  list->n_buffers -= length;
}

/**
 * gst_buffer_list_foreach:
 * @list: a #GstBufferList
 * @func: (scope call): a #GstBufferListFunc to call
 * @user_data: (closure): user data passed to @func
 *
 * Call @func with @data for each buffer in @list.
 *
 * @func can modify the passed buffer pointer or its contents. The return value
 * of @func define if this function returns or if the remaining buffers in
 * the list should be skipped.
 *
 * Returns: %TRUE when @func returned %TRUE for each buffer in @list or when
 * @list is empty.
 */
gboolean
gst_buffer_list_foreach (GstBufferList * list, GstBufferListFunc func,
    gpointer user_data)
{
  guint i, len;
  gboolean ret = TRUE;
  gboolean list_was_writable, first_warning = TRUE;

  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), FALSE);
  g_return_val_if_fail (func != NULL, FALSE);

  list_was_writable = gst_buffer_list_is_writable (list);

  len = list->n_buffers;
  for (i = 0; i < len;) {
    GstBuffer *buf, *buf_ret;
    gboolean was_writable;

    buf = buf_ret = list->buffers[i];

    /* If the buffer is writable, we remove us as parent for now to
     * allow the callback to destroy the buffer. If we get the buffer
     * back, we add ourselves as parent again.
     *
     * Non-writable buffers just get another reference as they were not
     * writable to begin with, and they would possibly become writable
     * by removing ourselves as parent
     */
    was_writable = list_was_writable && gst_buffer_is_writable (buf);

    if (was_writable)
      gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (buf),
          GST_MINI_OBJECT_CAST (list));
    else
      gst_buffer_ref (buf);

    ret = func (&buf_ret, i, user_data);

    /* Check if the function changed the buffer */
    if (buf != buf_ret) {
      /* If the list was not writable but the callback was actually changing
       * our buffer, then it wouldn't have been allowed to do so.
       *
       * Fortunately we still have a reference to the old buffer in that case
       * and just not modify the list, unref the new buffer (if any) and warn
       * about this */
      if (!list_was_writable) {
        if (first_warning) {
          g_critical
              ("gst_buffer_list_foreach: non-writable list %p was changed from callback",
              list);
          first_warning = FALSE;
        }
        if (buf_ret)
          gst_buffer_unref (buf_ret);
      } else if (buf_ret == NULL) {
        gst_buffer_list_remove_range_internal (list, i, 1, !was_writable);
        --len;
      } else {
        if (!was_writable) {
          gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (buf),
              GST_MINI_OBJECT_CAST (list));
          gst_buffer_unref (buf);
        }

        list->buffers[i] = buf_ret;
        gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (buf_ret),
            GST_MINI_OBJECT_CAST (list));
      }
    } else {
      if (was_writable)
        gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (buf),
            GST_MINI_OBJECT_CAST (list));
      else
        gst_buffer_unref (buf);
    }

    if (!ret)
      break;

    /* If the buffer was not removed by func go to the next buffer */
    if (buf_ret != NULL)
      i++;
  }
  return ret;
}

/**
 * gst_buffer_list_get:
 * @list: a #GstBufferList
 * @idx: the index
 *
 * Get the buffer at @idx.
 *
 * You must make sure that @idx does not exceed the number of
 * buffers available.
 *
 * Returns: (transfer none) (nullable): the buffer at @idx in @group
 *     or %NULL when there is no buffer. The buffer remains valid as
 *     long as @list is valid and buffer is not removed from the list.
 */
GstBuffer *
gst_buffer_list_get (GstBufferList * list, guint idx)
{
  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), NULL);
  g_return_val_if_fail (idx < list->n_buffers, NULL);

  return list->buffers[idx];
}

/**
 * gst_buffer_list_get_writable:
 * @list: a (writable) #GstBufferList
 * @idx: the index
 *
 * Gets the buffer at @idx, ensuring it is a writable buffer.
 *
 * You must make sure that @idx does not exceed the number of
 * buffers available.
 *
 * Returns: (transfer none) (nullable): the buffer at @idx in @group.
 *     The returned  buffer remains valid as long as @list is valid and
 *     the buffer is not removed from the list.
 *
 * Since: 1.14
 */
GstBuffer *
gst_buffer_list_get_writable (GstBufferList * list, guint idx)
{
  GstBuffer *new_buf;

  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), NULL);
  g_return_val_if_fail (gst_buffer_list_is_writable (list), NULL);
  g_return_val_if_fail (idx < list->n_buffers, NULL);

  /* We have to implement this manually here to correctly add/remove the
   * parent */
  if (gst_buffer_is_writable (list->buffers[idx]))
    return list->buffers[idx];

  gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (list->buffers[idx]),
      GST_MINI_OBJECT_CAST (list));
  new_buf = gst_buffer_copy (list->buffers[idx]);
  gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (new_buf),
      GST_MINI_OBJECT_CAST (list));
  gst_buffer_unref (list->buffers[idx]);
  list->buffers[idx] = new_buf;

  return new_buf;
}

/**
 * gst_buffer_list_add:
 * @l: a #GstBufferList
 * @b: a #GstBuffer
 *
 * Append @b at the end of @l.
 */
/**
 * gst_buffer_list_insert:
 * @list: a #GstBufferList
 * @idx: the index
 * @buffer: (transfer full): a #GstBuffer
 *
 * Insert @buffer at @idx in @list. Other buffers are moved to make room for
 * this new buffer.
 *
 * A -1 value for @idx will append the buffer at the end.
 */
void
gst_buffer_list_insert (GstBufferList * list, gint idx, GstBuffer * buffer)
{
  guint want_alloc;

  g_return_if_fail (GST_IS_BUFFER_LIST (list));
  g_return_if_fail (buffer != NULL);
  g_return_if_fail (gst_buffer_list_is_writable (list));

  if (idx == -1 && list->n_buffers < list->n_allocated) {
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (buffer),
        GST_MINI_OBJECT_CAST (list));
    list->buffers[list->n_buffers++] = buffer;
    return;
  }

  if (idx == -1 || idx > list->n_buffers)
    idx = list->n_buffers;

  want_alloc = list->n_buffers + 1;

  if (want_alloc > list->n_allocated) {
    if (G_UNLIKELY (list->n_allocated > (G_MAXUINT / 2)))
      g_error ("Growing GstBufferList would result in overflow");

    want_alloc = MAX (GST_ROUND_UP_16 (want_alloc), list->n_allocated * 2);

    if (GST_BUFFER_LIST_IS_USING_DYNAMIC_ARRAY (list)) {
      list->buffers = g_renew (GstBuffer *, list->buffers, want_alloc);
    } else {
      list->buffers = g_new0 (GstBuffer *, want_alloc);
      memcpy (list->buffers, &list->arr[0], list->n_buffers * sizeof (void *));
      GST_CAT_LOG (GST_CAT_PERFORMANCE, "exceeding pre-alloced array");
    }

    list->n_allocated = want_alloc;
  }

  if (idx < list->n_buffers) {
    memmove (&list->buffers[idx + 1], &list->buffers[idx],
        (list->n_buffers - idx) * sizeof (void *));
  }

  ++list->n_buffers;
  list->buffers[idx] = buffer;
  gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (buffer),
      GST_MINI_OBJECT_CAST (list));
}

/**
 * gst_buffer_list_remove:
 * @list: a #GstBufferList
 * @idx: the index
 * @length: the amount to remove
 *
 * Remove @length buffers starting from @idx in @list. The following buffers
 * are moved to close the gap.
 */
void
gst_buffer_list_remove (GstBufferList * list, guint idx, guint length)
{
  g_return_if_fail (GST_IS_BUFFER_LIST (list));
  g_return_if_fail (idx < list->n_buffers);
  g_return_if_fail (idx + length <= list->n_buffers);
  g_return_if_fail (gst_buffer_list_is_writable (list));

  gst_buffer_list_remove_range_internal (list, idx, length, TRUE);
}

/**
 * gst_buffer_list_copy_deep:
 * @list: a #GstBufferList
 *
 * Create a copy of the given buffer list. This will make a newly allocated
 * copy of the buffer that the source buffer list contains.
 *
 * Returns: (transfer full): a new copy of @list.
 *
 * Since: 1.6
 */
GstBufferList *
gst_buffer_list_copy_deep (const GstBufferList * list)
{
  guint i, len;
  GstBufferList *result = NULL;

  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), NULL);

  result = gst_buffer_list_new ();

  len = list->n_buffers;
  for (i = 0; i < len; i++) {
    GstBuffer *old = list->buffers[i];
    GstBuffer *new = gst_buffer_copy_deep (old);

    if (G_LIKELY (new)) {
      gst_buffer_list_insert (result, i, new);
    } else {
      g_warning
          ("Failed to deep copy buffer %p while deep "
          "copying buffer list %p. Buffer list copy "
          "will be incomplete", old, list);
    }
  }

  return result;
}

/**
 * gst_buffer_list_calculate_size:
 * @list: a #GstBufferList
 *
 * Calculates the size of the data contained in buffer list by adding the
 * size of all buffers.
 *
 * Returns: the size of the data contained in buffer list in bytes.
 *
 * Since: 1.14
 */
gsize
gst_buffer_list_calculate_size (GstBufferList * list)
{
  GstBuffer **buffers;
  gsize size = 0;
  guint i, n;

  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), 0);

  n = list->n_buffers;
  buffers = list->buffers;

  for (i = 0; i < n; ++i)
    size += gst_buffer_get_size (buffers[i]);

  return size;
}

/**
 * gst_buffer_list_ref: (skip)
 * @list: a #GstBufferList
 *
 * Increases the refcount of the given buffer list by one.
 *
 * Note that the refcount affects the writability of @list and its data, see
 * gst_buffer_list_make_writable(). It is important to note that keeping
 * additional references to GstBufferList instances can potentially increase
 * the number of memcpy operations in a pipeline.
 *
 * Returns: (transfer full): @list
 */
GstBufferList *
gst_buffer_list_ref (GstBufferList * list)
{
  return
      GST_BUFFER_LIST_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (list)));
}

/**
 * gst_buffer_list_unref: (skip)
 * @list: (transfer full): a #GstBufferList
 *
 * Decreases the refcount of the buffer list. If the refcount reaches 0, the
 * buffer list will be freed.
 */
void
gst_buffer_list_unref (GstBufferList * list)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (list));
}

/**
 * gst_clear_buffer_list: (skip)
 * @list_ptr: a pointer to a #GstBufferList reference
 *
 * Clears a reference to a #GstBufferList.
 *
 * @list_ptr must not be %NULL.
 *
 * If the reference is %NULL then this function does nothing. Otherwise, the
 * reference count of the list is decreased and the pointer is set to %NULL.
 *
 * Since: 1.16
 */
void
gst_clear_buffer_list (GstBufferList ** list_ptr)
{
  gst_clear_mini_object ((GstMiniObject **) list_ptr);
}

/**
 * gst_buffer_list_copy: (skip)
 * @list: a #GstBufferList
 *
 * Create a shallow copy of the given buffer list. This will make a newly
 * allocated copy of the source list with copies of buffer pointers. The
 * refcount of buffers pointed to will be increased by one.
 *
 * Returns: (transfer full): a new copy of @list.
 */
GstBufferList *
gst_buffer_list_copy (const GstBufferList * list)
{
  return
      GST_BUFFER_LIST_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST
          (list)));
}

/**
 * gst_buffer_list_replace:
 * @old_list: (inout) (transfer full) (nullable): pointer to a pointer to a
 *     #GstBufferList to be replaced.
 * @new_list: (transfer none) (allow-none): pointer to a #GstBufferList that
 *     will replace the buffer list pointed to by @old_list.
 *
 * Modifies a pointer to a #GstBufferList to point to a different
 * #GstBufferList. The modification is done atomically (so this is useful for
 * ensuring thread safety in some cases), and the reference counts are updated
 * appropriately (the old buffer list is unreffed, the new is reffed).
 *
 * Either @new_list or the #GstBufferList pointed to by @old_list may be %NULL.
 *
 * Returns: %TRUE if @new_list was different from @old_list
 *
 * Since: 1.16
 */
gboolean
gst_buffer_list_replace (GstBufferList ** old_list, GstBufferList * new_list)
{
  return gst_mini_object_replace ((GstMiniObject **) old_list,
      (GstMiniObject *) new_list);
}

/**
 * gst_buffer_list_take:
 * @old_list: (inout) (transfer full): pointer to a pointer to a #GstBufferList
 *     to be replaced.
 * @new_list: (transfer full) (allow-none): pointer to a #GstBufferList
 *     that will replace the bufferlist pointed to by @old_list.
 *
 * Modifies a pointer to a #GstBufferList to point to a different
 * #GstBufferList. This function is similar to gst_buffer_list_replace() except
 * that it takes ownership of @new_list.
 *
 * Returns: %TRUE if @new_list was different from @old_list
 *
 * Since: 1.16
 */
gboolean
gst_buffer_list_take (GstBufferList ** old_list, GstBufferList * new_list)
{
  return gst_mini_object_take ((GstMiniObject **) old_list,
      (GstMiniObject *) new_list);
}
