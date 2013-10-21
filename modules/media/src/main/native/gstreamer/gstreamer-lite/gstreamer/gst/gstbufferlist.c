/* GStreamer
 * Copyright (C) 2009 Axis Communications <dev-gstreamer at axis dot com>
 * @author Jonas Holmberg <jonas dot holmberg at axis dot com>
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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstbufferlist
 * @short_description: Grouped scatter data buffer type for data-passing
 * @see_also: #GstPad, #GstMiniObject
 *
 * Buffer lists are units of grouped scatter/gather data transfer in
 * GStreamer.
 *
 * Buffer lists are created with gst_buffer_list_new() and filled with data
 * using a #GstBufferListIterator. The iterator has no current buffer; its
 * cursor position lies between buffers, immediately before the buffer that
 * would be returned by gst_buffer_list_iterator_next(). After iterating to the
 * end of a group the iterator must be advanced to the next group by a call to
 * gst_buffer_list_iterator_next_group() before any further calls to
 * gst_buffer_list_iterator_next() can return buffers again. The cursor position
 * of a newly created iterator lies before the first group; a call to
 * gst_buffer_list_iterator_next_group() is necessary before calls to
 * gst_buffer_list_iterator_next() can return buffers.
 *
 * <informalfigure>
 *   <programlisting>
 *      +--- group0 ----------------------+--- group1 ------------+
 *      |   buffer0   buffer1   buffer2   |   buffer3   buffer4   |
 *    ^   ^         ^         ^         ^   ^         ^         ^
 *    Iterator positions between buffers
 *   </programlisting>
 * </informalfigure>
 *
 * The gst_buffer_list_iterator_remove(), gst_buffer_list_iterator_steal(),
 * gst_buffer_list_iterator_take() and gst_buffer_list_iterator_do() functions
 * are not defined in terms of the cursor position; they operate on the last
 * element returned from gst_buffer_list_iterator_next().
 *
 * The basic use pattern of creating a buffer list with an iterator is as
 * follows:
 *
 * <example>
 * <title>Creating a buffer list</title>
 *   <programlisting>
 *    GstBufferList *list;
 *    GstBufferListIterator *it;
 *
 *    list = gst_buffer_list_new ();
 *    it = gst_buffer_list_iterate (list);
 *    gst_buffer_list_iterator_add_group (it);
 *    gst_buffer_list_iterator_add (it, header1);
 *    gst_buffer_list_iterator_add (it, data1);
 *    gst_buffer_list_iterator_add_group (it);
 *    gst_buffer_list_iterator_add (it, header2);
 *    gst_buffer_list_iterator_add (it, data2);
 *    gst_buffer_list_iterator_add_group (it);
 *    gst_buffer_list_iterator_add (it, header3);
 *    gst_buffer_list_iterator_add (it, data3);
 *    ...
 *    gst_buffer_list_iterator_free (it);
 *   </programlisting>
 * </example>
 *
 * The basic use pattern of iterating over a buffer list is as follows:
 *
 * <example>
 * <title>Iterating a buffer list</title>
 *   <programlisting>
 *    GstBufferListIterator *it;
 *
 *    it = gst_buffer_list_iterate (list);
 *    while (gst_buffer_list_iterator_next_group (it)) {
 *      while ((buffer = gst_buffer_list_iterator_next (it)) != NULL) {
 *        do_something_with_buffer (buffer);
 *      }
 *    }
 *    gst_buffer_list_iterator_free (it);
 *   </programlisting>
 * </example>
 *
 * The basic use pattern of modifying a buffer in a list is as follows:
 *
 * <example>
 * <title>Modifying the data of the first buffer in a list</title>
 *   <programlisting>
 *    GstBufferListIterator *it;
 *
 *    list = gst_buffer_list_make_writable (list);
 *    it = gst_buffer_list_iterate (list);
 *    if (gst_buffer_list_iterator_next_group (it)) {
 *      GstBuffer *buf
 *
 *      buf = gst_buffer_list_iterator_next (it);
 *      if (buf != NULL) {
 *        buf = gst_buffer_list_iterator_do (it,
 *            (GstBufferListDoFunction) gst_mini_object_make_writable, NULL);
 *        modify_data (GST_BUFFER_DATA (buf));
 *      }
 *    }
 *    gst_buffer_list_iterator_free (it);
 *   </programlisting>
 * </example>
 *
 * Since: 0.10.24
 */
#include "gst_private.h"

#include "gstbuffer.h"
#include "gstbufferlist.h"

#define GST_CAT_DEFAULT GST_CAT_BUFFER_LIST

#define GROUP_START NULL
static gconstpointer STOLEN = "";

/**
 * GstBufferList:
 *
 * Opaque list of grouped buffers.
 *
 * Since: 0.10.24
 */
struct _GstBufferList
{
  GstMiniObject mini_object;

  GQueue *buffers;
};

struct _GstBufferListClass
{
  GstMiniObjectClass mini_object_class;
};

/**
 * GstBufferListIterator:
 *
 * Opaque iterator for a #GstBufferList.
 *
 * Since: 0.10.24
 */
struct _GstBufferListIterator
{
  GstBufferList *list;
  GList *next;
  GList *last_returned;
};

static GType _gst_buffer_list_type = 0;

G_DEFINE_TYPE (GstBufferList, gst_buffer_list, GST_TYPE_MINI_OBJECT);

void
_gst_buffer_list_initialize (void)
{
  GType type = gst_buffer_list_get_type ();

  g_type_class_ref (type);
  _gst_buffer_list_type = type;
}

static void
gst_buffer_list_init (GstBufferList * list)
{
  list->buffers = g_queue_new ();

  GST_LOG ("init %p", list);
}

static void
gst_buffer_list_finalize (GstBufferList * list)
{
  GList *tmp;

  g_return_if_fail (list != NULL);

  GST_LOG ("finalize %p", list);

  tmp = list->buffers->head;
  while (tmp) {
    if (tmp->data != GROUP_START && tmp->data != STOLEN) {
      gst_buffer_unref (GST_BUFFER_CAST (tmp->data));
    }
    tmp = tmp->next;
  }
  g_queue_free (list->buffers);

/* Not chaining up because GstMiniObject::finalize() does nothing
  GST_MINI_OBJECT_CLASS (gst_buffer_list_parent_class)->finalize
      (GST_MINI_OBJECT_CAST (list));*/
}

static GstBufferList *
_gst_buffer_list_copy (GstBufferList * list)
{
  GstBufferList *list_copy;
  GQueue *buffers_copy;
  GList *tmp;

  g_return_val_if_fail (list != NULL, NULL);

  /* shallow copy of list and pointers */
  buffers_copy = g_queue_copy (list->buffers);

  /* ref all buffers in the list */
  tmp = list->buffers->head;
  while (tmp) {
    if (tmp->data != GROUP_START && tmp->data != STOLEN) {
      tmp->data = gst_buffer_ref (GST_BUFFER_CAST (tmp->data));
    }
    tmp = g_list_next (tmp);
  }

  list_copy = gst_buffer_list_new ();
  g_queue_free (list_copy->buffers);
  list_copy->buffers = buffers_copy;

  return list_copy;
}

static void
gst_buffer_list_class_init (GstBufferListClass * list_class)
{
  list_class->mini_object_class.copy =
      (GstMiniObjectCopyFunction) _gst_buffer_list_copy;
  list_class->mini_object_class.finalize =
      (GstMiniObjectFinalizeFunction) gst_buffer_list_finalize;
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
 *
 * Since: 0.10.24
 */
GstBufferList *
gst_buffer_list_new (void)
{
  GstBufferList *list;

  list = (GstBufferList *) gst_mini_object_new (_gst_buffer_list_type);

  GST_LOG ("new %p", list);

  return list;
}

/**
 * gst_buffer_list_n_groups:
 * @list: a #GstBufferList
 *
 * Returns the number of groups in @list.
 *
 * Returns: the number of groups in the buffer list
 *
 * Since: 0.10.24
 */
guint
gst_buffer_list_n_groups (GstBufferList * list)
{
  GList *tmp;
  guint n;

  g_return_val_if_fail (list != NULL, 0);

  tmp = list->buffers->head;
  n = 0;
  while (tmp) {
    if (tmp->data == GROUP_START) {
      n++;
    }
    tmp = g_list_next (tmp);
  }

  return n;
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
 * of @func define if this function returns or if the remaining buffers in a
 * group should be skipped.
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_foreach (GstBufferList * list, GstBufferListFunc func,
    gpointer user_data)
{
  GList *tmp, *next;
  guint group, idx;
  GstBufferListItem res;

  g_return_if_fail (list != NULL);
  g_return_if_fail (func != NULL);

  next = list->buffers->head;
  group = idx = 0;
  while (next) {
    GstBuffer *buffer;

    tmp = next;
    next = g_list_next (tmp);

    buffer = tmp->data;

    if (buffer == GROUP_START) {
      group++;
      idx = 0;
      continue;
    } else if (buffer == STOLEN)
      continue;
    else
      idx++;

    /* need to decrement the indices */
    res = func (&buffer, group - 1, idx - 1, user_data);

    if (G_UNLIKELY (buffer != tmp->data)) {
      /* the function changed the buffer */
      if (buffer == NULL) {
        /* we were asked to remove the item */
        g_queue_delete_link (list->buffers, tmp);
        idx--;
      } else {
        /* change the buffer */
        tmp->data = buffer;
      }
    }

    switch (res) {
      case GST_BUFFER_LIST_CONTINUE:
        break;
      case GST_BUFFER_LIST_SKIP_GROUP:
        while (next && next->data != GROUP_START)
          next = g_list_next (next);
        break;
      case GST_BUFFER_LIST_END:
        return;
    }
  }
}

/**
 * gst_buffer_list_get:
 * @list: a #GstBufferList
 * @group: the group
 * @idx: the index in @group
 *
 * Get the buffer at @idx in @group.
 *
 * Note that this function is not efficient for iterating over the entire list.
 * Use an iterator or gst_buffer_list_foreach() instead.
 *
 * Returns: (transfer none): the buffer at @idx in @group or NULL when there
 *     is no buffer. The buffer remains valid as long as @list is valid.
 *
 * Since: 0.10.24
 */
GstBuffer *
gst_buffer_list_get (GstBufferList * list, guint group, guint idx)
{
  GList *tmp;
  guint cgroup, cidx;

  g_return_val_if_fail (list != NULL, NULL);

  tmp = list->buffers->head;
  cgroup = 0;
  while (tmp) {
    if (tmp->data == GROUP_START) {
      if (cgroup == group) {
        /* we found the group */
        tmp = g_list_next (tmp);
        cidx = 0;
        while (tmp && tmp->data != GROUP_START) {
          if (tmp->data != STOLEN) {
            if (cidx == idx)
              return GST_BUFFER_CAST (tmp->data);
            else
              cidx++;
          }
          tmp = g_list_next (tmp);
        }
        break;
      } else {
        cgroup++;
        if (cgroup > group)
          break;
      }
    }
    tmp = g_list_next (tmp);
  }
  return NULL;
}

static GstBufferListIterator *
gst_buffer_list_iterator_copy (const GstBufferListIterator * it)
{
  GstBufferListIterator *ret;

  ret = g_slice_new (GstBufferListIterator);
  ret->list = it->list;
  ret->next = it->next;
  ret->last_returned = it->last_returned;

  return ret;
}

GType
gst_buffer_list_iterator_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0)) {
    type = g_boxed_type_register_static ("GstBufferListIterator",
        (GBoxedCopyFunc) gst_buffer_list_iterator_copy,
        (GBoxedFreeFunc) gst_buffer_list_iterator_free);
  }

  return type;
}

/**
 * gst_buffer_list_iterate:
 * @list: a #GstBufferList
 *
 * Iterate the buffers in @list. The owner of the iterator must also be the
 * owner of a reference to @list while the returned iterator is in use.
 *
 * Free-function: gst_buffer_list_iterator_free
 *
 * Returns: (transfer full): a new #GstBufferListIterator of the buffers in
 *     @list. gst_buffer_list_iterator_free() after usage
 *
 * Since: 0.10.24
 */
GstBufferListIterator *
gst_buffer_list_iterate (GstBufferList * list)
{
  GstBufferListIterator *it;

  g_return_val_if_fail (list != NULL, NULL);

  it = g_slice_new (GstBufferListIterator);
  it->list = list;
  it->next = list->buffers->head;
  it->last_returned = NULL;

  return it;
}

/**
 * gst_buffer_list_iterator_free:
 * @it: (transfer full): the #GstBufferListIterator to free
 *
 * Free the iterator.
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_iterator_free (GstBufferListIterator * it)
{
  g_return_if_fail (it != NULL);

  g_slice_free (GstBufferListIterator, it);
}

/**
 * gst_buffer_list_iterator_n_buffers:
 * @it: a #GstBufferListIterator
 *
 * Returns the number of buffers left to iterate in the current group. I.e. the
 * number of calls that can be made to gst_buffer_list_iterator_next() before
 * it returns NULL.
 *
 * This function will not move the implicit cursor or in any other way affect
 * the state of the iterator @it.
 *
 * Returns: the number of buffers left to iterate in the current group
 *
 * Since: 0.10.24
 */
guint
gst_buffer_list_iterator_n_buffers (const GstBufferListIterator * it)
{
  GList *tmp;
  guint n;

  g_return_val_if_fail (it != NULL, 0);

  tmp = it->next;
  n = 0;
  while (tmp && tmp->data != GROUP_START) {
    if (tmp->data != STOLEN) {
      n++;
    }
    tmp = g_list_next (tmp);
  }

  return n;
}

#ifndef GSTREAMER_LITE
/**
 * gst_buffer_list_iterator_add:
 * @it: a #GstBufferListIterator
 * @buffer: (transfer full): a #GstBuffer
 *
 * Inserts @buffer into the #GstBufferList iterated with @it. The buffer is
 * inserted into the current group, immediately before the buffer that would be
 * returned by gst_buffer_list_iterator_next(). The buffer is inserted before
 * the implicit cursor, a subsequent call to gst_buffer_list_iterator_next()
 * will return the buffer after the inserted buffer, if any.
 *
 * This function takes ownership of @buffer.
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_iterator_add (GstBufferListIterator * it, GstBuffer * buffer)
{
  g_return_if_fail (it != NULL);
  g_return_if_fail (buffer != NULL);

  /* adding before the first group start is not allowed */
  g_return_if_fail (it->next != it->list->buffers->head);

  /* cheap insert into the GQueue */
  if (it->next != NULL) {
    g_queue_insert_before (it->list->buffers, it->next, buffer);
  } else {
    g_queue_push_tail (it->list->buffers, buffer);
  }
}

/**
 * gst_buffer_list_iterator_add_list:
 * @it: a #GstBufferListIterator
 * @list: (transfer full) (element-type Gst.Buffer): a #GList of buffers
 *
 * Inserts @list of buffers into the #GstBufferList iterated with @it. The list is
 * inserted into the current group, immediately before the buffer that would be
 * returned by gst_buffer_list_iterator_next(). The list is inserted before
 * the implicit cursor, a subsequent call to gst_buffer_list_iterator_next()
 * will return the buffer after the last buffer of the inserted list, if any.
 *
 * This function takes ownership of @list and all its buffers.
 *
 * Since: 0.10.31
 */
void
gst_buffer_list_iterator_add_list (GstBufferListIterator * it, GList * list)
{
  GList *last;
  guint len;

  g_return_if_fail (it != NULL);
  g_return_if_fail (it->next != it->list->buffers->head);

  if (list == NULL)
    return;

  last = list;
  len = 1;
  while (last->next) {
    last = last->next;
    len++;
  }

  if (it->next) {
    last->next = it->next;
    list->prev = it->next->prev;
    it->next->prev = last;
    if (list->prev)
      list->prev->next = list;
  } else {
    it->list->buffers->tail->next = list;
    list->prev = it->list->buffers->tail;
    it->list->buffers->tail = last;
  }
  it->list->buffers->length += len;
}
#endif // GSTREAMER_LITE

/**
 * gst_buffer_list_iterator_add_group:
 * @it: a #GstBufferListIterator
 *
 * Inserts a new, empty group into the #GstBufferList iterated with @it. The
 * group is inserted immediately before the group that would be returned by
 * gst_buffer_list_iterator_next_group(). A subsequent call to
 * gst_buffer_list_iterator_next_group() will advance the iterator to the group
 * after the inserted group, if any.
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_iterator_add_group (GstBufferListIterator * it)
{
  g_return_if_fail (it != NULL);

  /* advance iterator to next group start */
  while (it->next != NULL && it->next->data != GROUP_START) {
    it->next = g_list_next (it->next);
  }

  /* cheap insert of a group start into the GQueue */
  if (it->next != NULL) {
    g_queue_insert_before (it->list->buffers, it->next, GROUP_START);
  } else {
    g_queue_push_tail (it->list->buffers, GROUP_START);
  }
}

/**
 * gst_buffer_list_iterator_next:
 * @it: a #GstBufferListIterator
 *
 * Returns the next buffer in the list iterated with @it. If the iterator is at
 * the end of a group, NULL will be returned. This function may be called
 * repeatedly to iterate through the current group.
 *
 * The caller will not get a new ref to the returned #GstBuffer and must not
 * unref it.
 *
 * Returns: (transfer none): the next buffer in the current group of the
 *     buffer list, or NULL
 *
 * Since: 0.10.24
 */
GstBuffer *
gst_buffer_list_iterator_next (GstBufferListIterator * it)
{
  GstBuffer *buffer;

  g_return_val_if_fail (it != NULL, NULL);

  while (it->next != NULL && it->next->data != GROUP_START &&
      it->next->data == STOLEN) {
    it->next = g_list_next (it->next);
  }

  if (it->next == NULL || it->next->data == GROUP_START) {
    goto no_buffer;
  }

  buffer = GST_BUFFER_CAST (it->next->data);

  it->last_returned = it->next;
  it->next = g_list_next (it->next);

  return buffer;

no_buffer:
  {
    it->last_returned = NULL;
    return NULL;
  }
}

/**
 * gst_buffer_list_iterator_next_group:
 * @it: a #GstBufferListIterator
 *
 * Advance the iterator @it to the first buffer in the next group. If the
 * iterator is at the last group, FALSE will be returned. This function may be
 * called repeatedly to iterate through the groups in a buffer list.
 *
 * Returns: TRUE if the iterator could be advanced to the next group, FALSE if
 * the iterator was already at the last group
 *
 * Since: 0.10.24
 */
gboolean
gst_buffer_list_iterator_next_group (GstBufferListIterator * it)
{
  g_return_val_if_fail (it != NULL, FALSE);

  /* advance iterator to next group start */
  while (it->next != NULL && it->next->data != GROUP_START) {
    it->next = g_list_next (it->next);
  }

  if (it->next) {
    /* move one step beyond the group start */
    it->next = g_list_next (it->next);
  }

  it->last_returned = NULL;

  return (it->next != NULL);
}

/**
 * gst_buffer_list_iterator_remove:
 * @it: a #GstBufferListIterator
 *
 * Removes the last buffer returned by gst_buffer_list_iterator_next() from
 * the #GstBufferList iterated with @it. gst_buffer_list_iterator_next() must
 * have been called on @it before this function is called. This function can
 * only be called once per call to gst_buffer_list_iterator_next().
 *
 * The removed buffer is unreffed.
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_iterator_remove (GstBufferListIterator * it)
{
  g_return_if_fail (it != NULL);
  g_return_if_fail (it->last_returned != NULL);
  g_assert (it->last_returned->data != GROUP_START);

  if (it->last_returned->data != STOLEN) {
    gst_buffer_unref (it->last_returned->data);
  }
  g_queue_delete_link (it->list->buffers, it->last_returned);
  it->last_returned = NULL;
}

/**
 * gst_buffer_list_iterator_take:
 * @it: a #GstBufferListIterator
 * @buffer: (transfer full): a #GstBuffer
 *
 * Replaces the last buffer returned by gst_buffer_list_iterator_next() with
 * @buffer in the #GstBufferList iterated with @it and takes ownership of
 * @buffer. gst_buffer_list_iterator_next() must have been called on @it before
 * this function is called. gst_buffer_list_iterator_remove() must not have been
 * called since the last call to gst_buffer_list_iterator_next().
 *
 * This function unrefs the replaced buffer if it has not been stolen with
 * gst_buffer_list_iterator_steal() and takes ownership of @buffer (i.e. the
 * refcount of @buffer is not increased).
 *
 * FIXME 0.11: this conditional taking-ownership is not good for bindings
 *
 * Since: 0.10.24
 */
void
gst_buffer_list_iterator_take (GstBufferListIterator * it, GstBuffer * buffer)
{
  g_return_if_fail (it != NULL);
  g_return_if_fail (it->last_returned != NULL);
  g_return_if_fail (buffer != NULL);
  g_assert (it->last_returned->data != GROUP_START);

  if (it->last_returned->data != STOLEN) {
    gst_buffer_unref (it->last_returned->data);
  }
  it->last_returned->data = buffer;
}

/**
 * gst_buffer_list_iterator_steal:
 * @it: a #GstBufferListIterator
 *
 * Returns the last buffer returned by gst_buffer_list_iterator_next() without
 * modifying the refcount of the buffer.
 *
 * Returns: (transfer none): the last buffer returned by
 *     gst_buffer_list_iterator_next()
 *
 * Since: 0.10.24
 */
GstBuffer *
gst_buffer_list_iterator_steal (GstBufferListIterator * it)
{
  GstBuffer *buffer;

  g_return_val_if_fail (it != NULL, NULL);
  g_return_val_if_fail (it->last_returned != NULL, NULL);
  g_return_val_if_fail (it->last_returned->data != STOLEN, NULL);
  g_assert (it->last_returned->data != GROUP_START);

  buffer = it->last_returned->data;
  it->last_returned->data = (gpointer) STOLEN;

  return buffer;
}

/**
 * gst_buffer_list_iterator_do:
 * @it: a #GstBufferListIterator
 * @do_func: (scope call): the function to be called
 * @user_data: (closure): the gpointer to optional user data.
 *
 * Calls the given function for the last buffer returned by
 * gst_buffer_list_iterator_next(). gst_buffer_list_iterator_next() must have
 * been called on @it before this function is called.
 * gst_buffer_list_iterator_remove() and gst_buffer_list_iterator_steal() must
 * not have been called since the last call to gst_buffer_list_iterator_next().
 *
 * See #GstBufferListDoFunction for more details.
 *
 * Returns: (transfer none): the return value from @do_func
 *
 * Since: 0.10.24
 */
GstBuffer *
gst_buffer_list_iterator_do (GstBufferListIterator * it,
    GstBufferListDoFunction do_func, gpointer user_data)
{
  GstBuffer *buffer;

  g_return_val_if_fail (it != NULL, NULL);
  g_return_val_if_fail (it->last_returned != NULL, NULL);
  g_return_val_if_fail (it->last_returned->data != STOLEN, NULL);
  g_return_val_if_fail (do_func != NULL, NULL);
  g_return_val_if_fail (gst_buffer_list_is_writable (it->list), NULL);
  g_assert (it->last_returned->data != GROUP_START);

  buffer = gst_buffer_list_iterator_steal (it);
  buffer = do_func (buffer, user_data);
  if (buffer == NULL) {
    gst_buffer_list_iterator_remove (it);
  } else {
    gst_buffer_list_iterator_take (it, buffer);
  }

  return buffer;
}

/**
 * gst_buffer_list_iterator_merge_group:
 * @it: a #GstBufferListIterator
 *
 * Merge a buffer list group into a normal #GstBuffer by copying its metadata
 * and memcpying its data into consecutive memory. All buffers in the current
 * group after the implicit cursor will be merged into one new buffer. The
 * metadata of the new buffer will be a copy of the metadata of the buffer that
 * would be returned by gst_buffer_list_iterator_next(). If there is no buffer
 * in the current group after the implicit cursor, NULL will be returned.
 *
 * This function will not move the implicit cursor or in any other way affect
 * the state of the iterator @it or the list.
 *
 * Returns: (transfer full): a new #GstBuffer, gst_buffer_unref() after usage,
 *     or NULL
 *
 * Since: 0.10.24
 */
GstBuffer *
gst_buffer_list_iterator_merge_group (const GstBufferListIterator * it)
{
  GList *tmp;
  guint size;
  GstBuffer *buf;
  guint8 *ptr;

  g_return_val_if_fail (it != NULL, NULL);

  /* calculate size of merged buffer */
  size = 0;
  tmp = it->next;
  while (tmp && tmp->data != GROUP_START) {
    if (tmp->data != STOLEN) {
      size += GST_BUFFER_SIZE (tmp->data);
    }
    tmp = g_list_next (tmp);
  }

  if (size == 0) {
    return NULL;
  }

  /* allocate a new buffer */
  buf = gst_buffer_new_and_alloc (size);

  /* copy metadata from the next buffer after the implicit cursor */
  gst_buffer_copy_metadata (buf, GST_BUFFER_CAST (it->next->data),
      GST_BUFFER_COPY_ALL);

  /* copy data of all buffers before the next group start into the new buffer */
  ptr = GST_BUFFER_DATA (buf);
  tmp = it->next;
  do {
    if (tmp->data != STOLEN) {
      memcpy (ptr, GST_BUFFER_DATA (tmp->data), GST_BUFFER_SIZE (tmp->data));
      ptr += GST_BUFFER_SIZE (tmp->data);
    }
    tmp = g_list_next (tmp);
  } while (tmp && tmp->data != GROUP_START);

  return buf;
}
