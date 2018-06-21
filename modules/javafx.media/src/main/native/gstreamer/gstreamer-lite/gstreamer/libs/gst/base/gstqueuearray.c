/* GStreamer
 * Copyright (C) 2009 Edward Hervey <bilboed@bilboed.com>
 * Copyright (C) 2015 Tim-Philipp Müller <tim@centricular.com>
 *
 * gstqueuearray.c:
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
 * SECTION:gstqueuearray
 * @title: GstQueueArray
 * @short_description: Array based queue object
 *
 * #GstQueueArray is an object that provides standard queue functionality
 * based on an array instead of linked lists. This reduces the overhead
 * caused by memory management by a large factor.
 */


#include <string.h>
#include <gst/gst.h>
#include "gstqueuearray.h"

struct _GstQueueArray
{
  /* < private > */
  guint8 *array;
  guint size;
  guint head;
  guint tail;
  guint length;
  guint elt_size;
  gboolean struct_array;
};

/**
 * gst_queue_array_new_for_struct: (skip)
 * @struct_size: Size of each element (e.g. structure) in the array
 * @initial_size: Initial size of the new queue
 *
 * Allocates a new #GstQueueArray object for elements (e.g. structures)
 * of size @struct_size, with an initial queue size of @initial_size.
 *
 * Returns: a new #GstQueueArray object
 *
 * Since: 1.6
 */
GstQueueArray *
gst_queue_array_new_for_struct (gsize struct_size, guint initial_size)
{
  GstQueueArray *array;

  g_return_val_if_fail (struct_size > 0, NULL);

  array = g_slice_new (GstQueueArray);
  array->elt_size = struct_size;
  array->size = initial_size;
  array->array = g_malloc0 (struct_size * initial_size);
  array->head = 0;
  array->tail = 0;
  array->length = 0;
  array->struct_array = TRUE;
  return array;
}

/**
 * gst_queue_array_new: (skip)
 * @initial_size: Initial size of the new queue
 *
 * Allocates a new #GstQueueArray object with an initial
 * queue size of @initial_size.
 *
 * Returns: a new #GstQueueArray object
 *
 * Since: 1.2
 */
GstQueueArray *
gst_queue_array_new (guint initial_size)
{
  GstQueueArray *array;

  array = gst_queue_array_new_for_struct (sizeof (gpointer), initial_size);
  array->struct_array = FALSE;
  return array;
}

/**
 * gst_queue_array_free: (skip)
 * @array: a #GstQueueArray object
 *
 * Frees queue @array and all memory associated to it.
 *
 * Since: 1.2
 */
void
gst_queue_array_free (GstQueueArray * array)
{
  g_return_if_fail (array != NULL);
  g_free (array->array);
  g_slice_free (GstQueueArray, array);
}

/**
 * gst_queue_array_pop_head_struct: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the head of the queue @array and removes it from the queue.
 *
 * Returns: pointer to element or struct, or NULL if @array was empty. The
 *    data pointed to by the returned pointer stays valid only as long as
 *    the queue array is not modified further!
 *
 * Since: 1.6
 */
gpointer
gst_queue_array_pop_head_struct (GstQueueArray * array)
{
  gpointer p_struct;
  g_return_val_if_fail (array != NULL, NULL);
  /* empty array */
  if (G_UNLIKELY (array->length == 0))
    return NULL;

  p_struct = array->array + (array->elt_size * array->head);

  array->head++;
  array->head %= array->size;
  array->length--;

  return p_struct;
}

/**
 * gst_queue_array_pop_head: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns and head of the queue @array and removes
 * it from the queue.
 *
 * Returns: The head of the queue
 *
 * Since: 1.2
 */
gpointer
gst_queue_array_pop_head (GstQueueArray * array)
{
  gpointer ret;
  g_return_val_if_fail (array != NULL, NULL);

  /* empty array */
  if (G_UNLIKELY (array->length == 0))
    return NULL;

  ret = *(gpointer *) (array->array + (sizeof (gpointer) * array->head));
  array->head++;
  array->head %= array->size;
  array->length--;
  return ret;
}

/**
 * gst_queue_array_peek_head_struct: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the head of the queue @array without removing it from the queue.
 *
 * Returns: pointer to element or struct, or NULL if @array was empty. The
 *    data pointed to by the returned pointer stays valid only as long as
 *    the queue array is not modified further!
 *
 * Since: 1.6
 */
gpointer
gst_queue_array_peek_head_struct (GstQueueArray * array)
{
  g_return_val_if_fail (array != NULL, NULL);
  /* empty array */
  if (G_UNLIKELY (array->length == 0))
    return NULL;

  return array->array + (array->elt_size * array->head);
}

/**
 * gst_queue_array_peek_head: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the head of the queue @array and does not
 * remove it from the queue.
 *
 * Returns: The head of the queue
 *
 * Since: 1.2
 */
gpointer
gst_queue_array_peek_head (GstQueueArray * array)
{
  g_return_val_if_fail (array != NULL, NULL);
  /* empty array */
  if (G_UNLIKELY (array->length == 0))
    return NULL;

  return *(gpointer *) (array->array + (sizeof (gpointer) * array->head));
}

static void
gst_queue_array_do_expand (GstQueueArray * array)
{
  guint elt_size = array->elt_size;
  /* newsize is 50% bigger */
  guint oldsize = array->size;
  guint newsize = MAX ((3 * oldsize) / 2, oldsize + 1);

  /* copy over data */
  if (array->tail != 0) {
    guint8 *array2 = g_malloc0 (elt_size * newsize);
    guint t1 = array->head;
    guint t2 = oldsize - array->head;

    /* [0-----TAIL][HEAD------SIZE]
     *
     * We want to end up with
     * [HEAD------------------TAIL][----FREEDATA------NEWSIZE]
     *
     * 1) move [HEAD-----SIZE] part to beginning of new array
     * 2) move [0-------TAIL] part new array, after previous part
     */

    memcpy (array2, array->array + (elt_size * array->head), t2 * elt_size);
    memcpy (array2 + t2 * elt_size, array->array, t1 * elt_size);

    g_free (array->array);
    array->array = array2;
    array->head = 0;
  } else {
    /* Fast path, we just need to grow the array */
    array->array = g_realloc (array->array, elt_size * newsize);
    memset (array->array + elt_size * oldsize, 0,
        elt_size * (newsize - oldsize));
  }
  array->tail = oldsize;
  array->size = newsize;
}

/**
 * gst_queue_array_push_element_tail: (skip)
 * @array: a #GstQueueArray object
 * @p_struct: address of element or structure to push to the tail of the queue
 *
 * Pushes the element at address @p_struct to the tail of the queue @array
 * (Copies the contents of a structure of the struct_size specified when
 * creating the queue into the array).
 *
 * Since: 1.6
 */
void
gst_queue_array_push_tail_struct (GstQueueArray * array, gpointer p_struct)
{
  guint elt_size;

  g_return_if_fail (p_struct != NULL);
  g_return_if_fail (array != NULL);
  elt_size = array->elt_size;

  /* Check if we need to make room */
  if (G_UNLIKELY (array->length == array->size))
    gst_queue_array_do_expand (array);

  memcpy (array->array + elt_size * array->tail, p_struct, elt_size);
  array->tail++;
  array->tail %= array->size;
  array->length++;
}

/**
 * gst_queue_array_push_tail: (skip)
 * @array: a #GstQueueArray object
 * @data: object to push
 *
 * Pushes @data to the tail of the queue @array.
 *
 * Since: 1.2
 */
void
gst_queue_array_push_tail (GstQueueArray * array, gpointer data)
{
  g_return_if_fail (array != NULL);

  /* Check if we need to make room */
  if (G_UNLIKELY (array->length == array->size))
    gst_queue_array_do_expand (array);

  *(gpointer *) (array->array + sizeof (gpointer) * array->tail) = data;
  array->tail++;
  array->tail %= array->size;
  array->length++;
}

/**
 * gst_queue_array_peek_tail: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the tail of the queue @array, but does not remove it from the queue.
 *
 * Returns: The tail of the queue
 *
 * Since: 1.14
 */
gpointer
gst_queue_array_peek_tail (GstQueueArray * array)
{
  guint len, idx;

  g_return_val_if_fail (array != NULL, NULL);

  len = array->length;

  /* empty array */
  if (len == 0)
    return NULL;

  idx = (array->head + (len - 1)) % array->size;

  return *(gpointer *) (array->array + (sizeof (gpointer) * idx));
}

/**
 * gst_queue_array_peek_tail_struct: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the tail of the queue @array, but does not remove it from the queue.
 *
 * Returns: The tail of the queue
 *
 * Since: 1.14
 */
gpointer
gst_queue_array_peek_tail_struct (GstQueueArray * array)
{
  guint len, idx;

  g_return_val_if_fail (array != NULL, NULL);

  len = array->length;

  /* empty array */
  if (len == 0)
    return NULL;

  idx = (array->head + (len - 1)) % array->size;

  return array->array + (array->elt_size * idx);
}

/**
 * gst_queue_array_pop_tail: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the tail of the queue @array and removes
 * it from the queue.
 *
 * Returns: The tail of the queue
 *
 * Since: 1.14
 */
gpointer
gst_queue_array_pop_tail (GstQueueArray * array)
{
  gpointer ret;
  guint len, idx;

  g_return_val_if_fail (array != NULL, NULL);

  len = array->length;

  /* empty array */
  if (len == 0)
    return NULL;

  idx = (array->head + (len - 1)) % array->size;

  ret = *(gpointer *) (array->array + (sizeof (gpointer) * idx));

  array->tail = idx;
  array->length--;

  return ret;
}

/**
 * gst_queue_array_pop_tail_struct: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the tail of the queue @array and removes
 * it from the queue.
 *
 * Returns: The tail of the queue
 *
 * Since: 1.14
 */
gpointer
gst_queue_array_pop_tail_struct (GstQueueArray * array)
{
  gpointer ret;
  guint len, idx;

  g_return_val_if_fail (array != NULL, NULL);

  len = array->length;

  /* empty array */
  if (len == 0)
    return NULL;

  idx = (array->head + (len - 1)) % array->size;

  ret = array->array + (array->elt_size * idx);

  array->tail = idx;
  array->length--;

  return ret;
}

/**
 * gst_queue_array_is_empty: (skip)
 * @array: a #GstQueueArray object
 *
 * Checks if the queue @array is empty.
 *
 * Returns: %TRUE if the queue @array is empty
 *
 * Since: 1.2
 */
gboolean
gst_queue_array_is_empty (GstQueueArray * array)
{
  g_return_val_if_fail (array != NULL, FALSE);
  return (array->length == 0);
}


/**
 * gst_queue_array_drop_struct: (skip)
 * @array: a #GstQueueArray object
 * @idx: index to drop
 * @p_struct: address into which to store the data of the dropped structure, or NULL
 *
 * Drops the queue element at position @idx from queue @array and copies the
 * data of the element or structure that was removed into @p_struct if
 * @p_struct is set (not NULL).
 *
 * Returns: TRUE on success, or FALSE on error
 *
 * Since: 1.6
 */
gboolean
gst_queue_array_drop_struct (GstQueueArray * array, guint idx,
    gpointer p_struct)
{
  int first_item_index, last_item_index;
  guint elt_size;

  g_return_val_if_fail (array != NULL, FALSE);
  g_return_val_if_fail (array->length > 0, FALSE);
  g_return_val_if_fail (idx < array->size, FALSE);

  elt_size = array->elt_size;

  first_item_index = array->head;

  /* tail points to the first free spot */
  last_item_index = (array->tail - 1 + array->size) % array->size;

  if (p_struct != NULL)
    memcpy (p_struct, array->array + elt_size * idx, elt_size);

  /* simple case idx == first item */
  if (idx == first_item_index) {
    /* move the head plus one */
    array->head++;
    array->head %= array->size;
    array->length--;
    return TRUE;
  }

  /* simple case idx == last item */
  if (idx == last_item_index) {
    /* move tail minus one, potentially wrapping */
    array->tail = (array->tail - 1 + array->size) % array->size;
    array->length--;
    return TRUE;
  }

  /* non-wrapped case */
  if (first_item_index < last_item_index) {
    g_assert (first_item_index < idx && idx < last_item_index);
    /* move everything beyond idx one step towards zero in array */
    memmove (array->array + elt_size * idx,
        array->array + elt_size * (idx + 1),
        (last_item_index - idx) * elt_size);
    /* tail might wrap, ie if tail == 0 (and last_item_index == size) */
    array->tail = (array->tail - 1 + array->size) % array->size;
    array->length--;
    return TRUE;
  }

  /* only wrapped cases left */
  g_assert (first_item_index > last_item_index);

  if (idx < last_item_index) {
    /* idx is before last_item_index, move data towards zero */
    memmove (array->array + elt_size * idx,
        array->array + elt_size * (idx + 1),
        (last_item_index - idx) * elt_size);
    /* tail should not wrap in this case! */
    g_assert (array->tail > 0);
    array->tail--;
    array->length--;
    return TRUE;
  }

  if (idx > first_item_index) {
    /* idx is after first_item_index, move data to higher indices */
    memmove (array->array + elt_size * (first_item_index + 1),
        array->array + elt_size * first_item_index,
        (idx - first_item_index) * elt_size);
    array->head++;
    /* head should not wrap in this case! */
    g_assert (array->head < array->size);
    array->length--;
    return TRUE;
  }

  g_return_val_if_reached (FALSE);
}

/**
 * gst_queue_array_drop_element: (skip)
 * @array: a #GstQueueArray object
 * @idx: index to drop
 *
 * Drops the queue element at position @idx from queue @array.
 *
 * Returns: the dropped element
 *
 * Since: 1.2
 */
gpointer
gst_queue_array_drop_element (GstQueueArray * array, guint idx)
{
  gpointer ptr;

  if (!gst_queue_array_drop_struct (array, idx, &ptr))
    return NULL;

  return ptr;
}

/**
 * gst_queue_array_find: (skip)
 * @array: a #GstQueueArray object
 * @func: (allow-none): comparison function, or %NULL to find @data by value
 * @data: data for comparison function
 *
 * Finds an element in the queue @array, either by comparing every element
 * with @func or by looking up @data if no compare function @func is provided,
 * and returning the index of the found element.
 *
 * Note that the index is not 0-based, but an internal index number with a
 * random offset. The index can be used in connection with
 * gst_queue_array_drop_element(). FIXME: return index 0-based and make
 * gst_queue_array_drop_element() take a 0-based index.
 *
 * Returns: Index of the found element or -1 if nothing was found.
 *
 * Since: 1.2
 */
guint
gst_queue_array_find (GstQueueArray * array, GCompareFunc func, gpointer data)
{
  gpointer p_element;
  guint elt_size;
  guint i;

  /* For struct arrays we need to implement this differently so that
   * the user gets a pointer to the element data not the dereferenced
   * pointer itself */

  g_return_val_if_fail (array != NULL, -1);
  g_return_val_if_fail (array->struct_array == FALSE, -1);

  elt_size = array->elt_size;

  if (func != NULL) {
    /* Scan from head to tail */
    for (i = 0; i < array->length; i++) {
      p_element = array->array + ((i + array->head) % array->size) * elt_size;
      if (func (*(gpointer *) p_element, data) == 0)
        return (i + array->head) % array->size;
    }
  } else {
    for (i = 0; i < array->length; i++) {
      p_element = array->array + ((i + array->head) % array->size) * elt_size;
      if (*(gpointer *) p_element == data)
        return (i + array->head) % array->size;
    }
  }

  return -1;
}

/**
 * gst_queue_array_get_length: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the length of the queue @array
 *
 * Returns: the length of the queue @array.
 *
 * Since: 1.2
 */
guint
gst_queue_array_get_length (GstQueueArray * array)
{
  g_return_val_if_fail (array != NULL, 0);
  return array->length;
}
