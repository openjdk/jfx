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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>
#include <gst/gst.h>
#include "gstqueuearray.h"

#define gst_queue_array_idx(a, i) \
  ((a)->array + (((a)->head + (i)) % (a)->size) * (a)->elt_size)

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
  GDestroyNotify clear_func;
};

typedef struct
{
  GCompareDataFunc func;
  gpointer user_data;
} QueueSortData;

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

  array = g_new (GstQueueArray, 1);
  array->elt_size = struct_size;
  array->size = initial_size;
  array->array = g_malloc0 (struct_size * initial_size);
  array->head = 0;
  array->tail = 0;
  array->length = 0;
  array->struct_array = TRUE;
  array->clear_func = NULL;
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
  gst_queue_array_clear (array);
  g_free (array->array);
  g_free (array);
}

/**
 * gst_queue_array_set_clear_func: (skip)
 * @array: a #GstQueueArray object
 * @clear_func: a function to clear an element of @array
 *
 * Sets a function to clear an element of @array.
 *
 * The @clear_func will be called when an element in the array
 * data segment is removed and when the array is freed and data
 * segment is deallocated as well. @clear_func will be passed a
 * pointer to the element to clear, rather than the element itself.
 *
 * Note that in contrast with other uses of #GDestroyNotify
 * functions, @clear_func is expected to clear the contents of
 * the array element it is given, but not free the element itself.
 *
 * Since: 1.16
 */
void
gst_queue_array_set_clear_func (GstQueueArray * array,
    GDestroyNotify clear_func)
{
  g_return_if_fail (array != NULL);
  array->clear_func = clear_func;
}

static void
gst_queue_array_clear_idx (GstQueueArray * array, guint idx)
{
  guint pos;

  if (!array->clear_func)
    return;

  pos = (idx + array->head) % array->size;
  if (array->struct_array)
    array->clear_func (array->array + pos * array->elt_size);
  else
    array->clear_func (*(gpointer *) (array->array + pos * array->elt_size));
}

/**
 * gst_queue_array_clear: (skip)
 * @array: a #GstQueueArray object
 *
 * Clears queue @array and frees all memory associated to it.
 *
 * Since: 1.16
 */
void
gst_queue_array_clear (GstQueueArray * array)
{
  g_return_if_fail (array != NULL);

  if (array->clear_func != NULL) {
    guint i;

    for (i = 0; i < array->length; i++) {
      gst_queue_array_clear_idx (array, i);
    }
  }

  array->head = 0;
  array->tail = 0;
  array->length = 0;
}

/**
 * gst_queue_array_pop_head_struct: (skip)
 * @array: a #GstQueueArray object
 *
 * Returns the head of the queue @array and removes it from the queue.
 *
 * Returns: (nullable): pointer to element or struct, or NULL if @array was empty. The
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
 * Returns: (nullable): pointer to element or struct, or NULL if @array was empty. The
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

/**
 * gst_queue_array_peek_nth: (skip)
 *
 * Returns the item at @idx in @array, but does not remove it from the queue.
 *
 * Returns: (nullable): The item, or %NULL if @idx was out of bounds
 *
 * Since: 1.16
 */
gpointer
gst_queue_array_peek_nth (GstQueueArray * array, guint idx)
{
  g_return_val_if_fail (array != NULL, NULL);
  g_return_val_if_fail (idx < array->length, NULL);

  idx = (array->head + idx) % array->size;

  return *(gpointer *) (array->array + (sizeof (gpointer) * idx));
}

/**
 * gst_queue_array_peek_nth_struct: (skip)
 *
 * Returns the item at @idx in @array, but does not remove it from the queue.
 *
 * Returns: (nullable): The item, or %NULL if @idx was out of bounds
 *
 * Since: 1.16
 */
gpointer
gst_queue_array_peek_nth_struct (GstQueueArray * array, guint idx)
{
  g_return_val_if_fail (array != NULL, NULL);
  g_return_val_if_fail (idx < array->length, NULL);

  idx = (array->head + idx) % array->size;

  return array->array + (array->elt_size * idx);
}

static void
gst_queue_array_do_expand (GstQueueArray * array)
{
  gsize elt_size = array->elt_size;
  /* newsize is 50% bigger */
  gsize oldsize = array->size;
  guint64 newsize;

  newsize = MAX ((3 * (guint64) oldsize) / 2, (guint64) oldsize + 1);
  if (newsize > G_MAXUINT)
    g_error ("growing the queue array would overflow");

  /* copy over data */
  if (array->tail != 0) {
    guint8 *array2 = NULL;
    gsize t1 = 0;
    gsize t2 = 0;

    array2 = g_malloc0_n (newsize, elt_size);
    t1 = array->head;
    t2 = oldsize - array->head;

    /* [0-----TAIL][HEAD------SIZE]
     *
     * We want to end up with
     * [HEAD------------------TAIL][----FREEDATA------NEWSIZE]
     *
     * 1) move [HEAD-----SIZE] part to beginning of new array
     * 2) move [0-------TAIL] part new array, after previous part
     */

    memcpy (array2, array->array + (elt_size * (gsize) array->head),
        t2 * elt_size);
    memcpy (array2 + t2 * elt_size, array->array, t1 * elt_size);

    g_free (array->array);
    array->array = array2;
    array->head = 0;
  } else {
    /* Fast path, we just need to grow the array */
    array->array = g_realloc_n (array->array, newsize, elt_size);
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

/* Moves all elements in the queue placed after the given position in the internal array */
static void
gst_queue_array_move_data_after_position (GstQueueArray * array, guint pos)
{
  guint elt_size = array->elt_size;

  /* If the array does not wrap around OR if it does, but we're inserting past that point */
  if (array->head < array->tail ||
      (array->head >= array->tail && pos < array->head)) {
    memmove (array->array + (pos + 1) * elt_size, array->array + pos * elt_size,
        (array->tail - pos) * elt_size);
    return;
  }

  /* Otherwise, array wraps around and we're inserting before the breaking point.
   * First, move everything past that point by one place. */
  memmove (array->array + elt_size, array->array, array->tail * elt_size);

  /* Then move the last element from before the wrap-around point to right after it. */
  memcpy (array->array, array->array + (array->size - 1) * elt_size, elt_size);

  /* If we're inserting right before the breaking point, no further action is needed.
   * Otherwise, move data between insertion point and the breaking point by one place. */
  if (pos != array->size - 1) {
    memmove (array->array + (pos + 1) * elt_size, array->array + pos * elt_size,
        (array->size - pos - 1) * elt_size);
  }
}

/**
 * gst_queue_array_push_sorted: (skip)
 * @array: a #GstQueueArray object
 * @data: object to push
 * @func: comparison function
 * @user_data: (nullable): data for comparison function
 *
 * Pushes @data to the queue @array, finding the correct position
 * by comparing @data with each array element using @func.
 *
 * This has a time complexity of O(n), so depending on the size of the queue
 * and expected access patterns, a different data structure might be better.
 *
 * Assumes that the array is already sorted. If it is not, make sure
 * to call gst_queue_array_sort() first.
 *
 * Since: 1.24
 */
void
gst_queue_array_push_sorted (GstQueueArray * array, gpointer data,
    GCompareDataFunc func, gpointer user_data)
{
  guint i;
  gpointer *p_element;

  g_return_if_fail (array != NULL);
  g_return_if_fail (func != NULL);

  /* Check if we need to make room */
  if (G_UNLIKELY (array->length == array->size))
    gst_queue_array_do_expand (array);

  /* Compare against each element, assuming they're already sorted */
  for (i = 0; i < array->length; i++) {
    p_element = (gpointer *) gst_queue_array_idx (array, i);

    if (func (*p_element, data, user_data) > 0) {
      guint pos = (array->head + i) % array->size;
      gst_queue_array_move_data_after_position (array, pos);

      *p_element = data;
      goto finish;
    }
  }

  /* No 'bigger' element found - append to tail */
  *(gpointer *) (array->array + array->elt_size * array->tail) = data;

finish:
  array->tail++;
  array->tail %= array->size;
  array->length++;
}

/**
 * gst_queue_array_push_sorted_struct: (skip)
 * @array: a #GstQueueArray object
 * @p_struct: address of element or structure to push into the queue
 * @func: comparison function
 * @user_data: (nullable): data for comparison function
 *
 * Pushes the element at address @p_struct into the queue @array
 * (copying the contents of a structure of the struct_size specified
 * when creating the queue into the array), finding the correct position
 * by comparing the element at @p_struct with each element in the array using @func.
 *
 * This has a time complexity of O(n), so depending on the size of the queue
 * and expected access patterns, a different data structure might be better.
 *
 * Assumes that the array is already sorted. If it is not, make sure
 * to call gst_queue_array_sort() first.
 *
 * Since: 1.24
 */
void
gst_queue_array_push_sorted_struct (GstQueueArray * array, gpointer p_struct,
    GCompareDataFunc func, gpointer user_data)
{
  guint i;
  gpointer p_element;

  g_return_if_fail (array != NULL);
  g_return_if_fail (p_struct != NULL);
  g_return_if_fail (func != NULL);

  /* Check if we need to make room */
  if (G_UNLIKELY (array->length == array->size))
    gst_queue_array_do_expand (array);

  /* Compare against each element, assuming they're already sorted */
  for (i = 0; i < array->length; i++) {
    p_element = gst_queue_array_idx (array, i);

    if (func (p_element, p_struct, user_data) > 0) {
      guint pos = (array->head + i) % array->size;
      gst_queue_array_move_data_after_position (array, pos);

      memcpy (p_element, p_struct, array->elt_size);
      goto finish;
    }
  }

  /* No 'bigger' element found - append to tail */
  memcpy (array->array + array->elt_size * array->tail, p_struct,
      array->elt_size);

finish:
  array->tail++;
  array->tail %= array->size;
  array->length++;
}

static int
compare_wrapper (gpointer * a, gpointer * b, QueueSortData * sort_data)
{
  return sort_data->func (*a, *b, sort_data->user_data);
}

/**
 * gst_queue_array_sort: (skip)
 * @array: a #GstQueueArray object
 * @compare_func: comparison function
 * @user_data: (nullable): data for comparison function
 *
 * Sorts the queue @array by comparing elements against each other using
 * the provided @compare_func.
 *
 * Since: 1.24
 */
void
gst_queue_array_sort (GstQueueArray * array, GCompareDataFunc compare_func,
    gpointer user_data)
{
  g_return_if_fail (array != NULL);
  g_return_if_fail (compare_func != NULL);

  if (array->length == 0)
    return;

  /* To be able to use g_qsort_with_data, we might need to rearrange:
   * [0-----TAIL][HEAD-----SIZE] -> [HEAD-------TAIL] */
  if (array->head >= array->tail) {
    gsize t1 = array->head;
    gsize t2 = array->size - array->head;
    gsize elt_size = array->elt_size;

    /* Copy [0-------TAIL] part to a temporary buffer */
    guint8 *tmp = g_malloc0_n (t1, elt_size);
    memcpy (tmp, array->array, t1 * elt_size);

    /* Move [HEAD-----SIZE] part to the beginning of the original array */
    memmove (array->array, array->array + (elt_size * array->head),
        t2 * elt_size);

    /* Copy the temporary buffer to the end of the original array */
    memmove (array->array + (t2 * elt_size), tmp, t1 * elt_size);
    g_free (tmp);

    array->head = 0;
    array->tail = array->length % array->size;
  }

  if (array->struct_array) {
    g_qsort_with_data (array->array +
        (array->head % array->size) * array->elt_size, array->length,
        array->elt_size, compare_func, user_data);
  } else {
    /* For non-struct arrays, we need to wrap the provided compare function
     * to dereference our pointers before passing them for comparison.
     * This matches the behaviour of gst_queue_array_find(). */
    QueueSortData sort_data = { compare_func, user_data };
    g_qsort_with_data (array->array +
        (array->head % array->size) * array->elt_size, array->length,
        array->elt_size, (GCompareDataFunc) compare_wrapper, &sort_data);
  }
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
  guint actual_idx;
  guint elt_size;

  g_return_val_if_fail (array != NULL, FALSE);
  actual_idx = (array->head + idx) % array->size;

  g_return_val_if_fail (array->length > 0, FALSE);
  g_return_val_if_fail (actual_idx < array->size, FALSE);

  elt_size = array->elt_size;

  first_item_index = array->head;

  /* tail points to the first free spot */
  last_item_index = (array->tail - 1 + array->size) % array->size;

  if (p_struct != NULL)
    memcpy (p_struct, array->array + elt_size * actual_idx, elt_size);

  /* simple case actual_idx == first item */
  if (actual_idx == first_item_index) {
    /* clear current head position if needed */
    if (p_struct == NULL)
      gst_queue_array_clear_idx (array, idx);

    /* move the head plus one */
    array->head++;
    array->head %= array->size;
    array->length--;
    return TRUE;
  }

  /* simple case idx == last item */
  if (actual_idx == last_item_index) {
    /* clear current tail position if needed */
    if (p_struct == NULL)
      gst_queue_array_clear_idx (array, idx);

    /* move tail minus one, potentially wrapping */
    array->tail = (array->tail - 1 + array->size) % array->size;
    array->length--;
    return TRUE;
  }

  /* non-wrapped case */
  if (first_item_index < last_item_index) {
    /* clear idx if needed */
    if (p_struct == NULL)
      gst_queue_array_clear_idx (array, idx);

    g_assert (first_item_index < actual_idx && actual_idx < last_item_index);
    /* move everything beyond actual_idx one step towards zero in array */
    memmove (array->array + elt_size * actual_idx,
        array->array + elt_size * (actual_idx + 1),
        (last_item_index - actual_idx) * elt_size);
    /* tail might wrap, ie if tail == 0 (and last_item_index == size) */
    array->tail = (array->tail - 1 + array->size) % array->size;
    array->length--;
    return TRUE;
  }

  /* only wrapped cases left */
  g_assert (first_item_index > last_item_index);

  if (actual_idx < last_item_index) {
    /* clear idx if needed */
    if (p_struct == NULL)
      gst_queue_array_clear_idx (array, idx);

    /* actual_idx is before last_item_index, move data towards zero */
    memmove (array->array + elt_size * actual_idx,
        array->array + elt_size * (actual_idx + 1),
        (last_item_index - actual_idx) * elt_size);
    /* tail should not wrap in this case! */
    g_assert (array->tail > 0);
    array->tail--;
    array->length--;
    return TRUE;
  }

  if (actual_idx > first_item_index) {
    /* clear idx if needed */
    if (p_struct == NULL)
      gst_queue_array_clear_idx (array, idx);

    /* actual_idx is after first_item_index, move data to higher indices */
    memmove (array->array + elt_size * (first_item_index + 1),
        array->array + elt_size * first_item_index,
        (actual_idx - first_item_index) * elt_size);
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
 * @func: (nullable): comparison function, or %NULL to find @data by value
 * @data: data for comparison function
 *
 * Finds an element in the queue @array, either by comparing every element
 * with @func or by looking up @data if no compare function @func is provided,
 * and returning the index of the found element.
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
        return i;
    }
  } else {
    for (i = 0; i < array->length; i++) {
      p_element = array->array + ((i + array->head) % array->size) * elt_size;
      if (*(gpointer *) p_element == data)
        return i;
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
