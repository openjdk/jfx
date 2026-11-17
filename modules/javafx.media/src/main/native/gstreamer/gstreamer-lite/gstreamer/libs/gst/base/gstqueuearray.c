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

#include <gst/gst.h>
#include "gstqueuearray.h"

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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
GstQueueArray *
gst_queue_array_new_for_struct (gsize struct_size, guint initial_size)
{
  return (GstQueueArray *) gst_vec_deque_new_for_struct (struct_size,
      initial_size);
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
  return (GstQueueArray *) gst_vec_deque_new (initial_size);
}

/**
 * gst_queue_array_free: (skip)
 * @array: a #GstQueueArray object
 *
 * Frees queue @array and all memory associated to it.
 *
 * Since: 1.2
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_free (GstQueueArray * array)
{
  gst_vec_deque_free ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_set_clear_func (GstQueueArray * array,
    GDestroyNotify clear_func)
{
  gst_vec_deque_set_clear_func ((GstVecDeque *) array, clear_func);
}

/**
 * gst_queue_array_clear: (skip)
 * @array: a #GstQueueArray object
 *
 * Clears queue @array and frees all memory associated to it.
 *
 * Since: 1.16
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_clear (GstQueueArray * array)
{
  gst_vec_deque_clear ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_pop_head_struct (GstQueueArray * array)
{
  return gst_vec_deque_pop_head_struct ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_pop_head (GstQueueArray * array)
{
  return gst_vec_deque_pop_head ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_head_struct (GstQueueArray * array)
{
  return gst_vec_deque_peek_head_struct ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_head (GstQueueArray * array)
{
  return gst_vec_deque_peek_head ((GstVecDeque *) array);
}

/**
 * gst_queue_array_peek_nth: (skip)
 *
 * Returns the item at @idx in @array, but does not remove it from the queue.
 *
 * Returns: (nullable): The item, or %NULL if @idx was out of bounds
 *
 * Since: 1.16
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_nth (GstQueueArray * array, guint idx)
{
  return gst_vec_deque_peek_nth ((GstVecDeque *) array, idx);
}

/**
 * gst_queue_array_peek_nth_struct: (skip)
 *
 * Returns the item at @idx in @array, but does not remove it from the queue.
 *
 * Returns: (nullable): The item, or %NULL if @idx was out of bounds
 *
 * Since: 1.16
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_nth_struct (GstQueueArray * array, guint idx)
{
  return gst_vec_deque_peek_nth_struct ((GstVecDeque *) array, idx);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_push_tail_struct (GstQueueArray * array, gpointer p_struct)
{
  gst_vec_deque_push_tail_struct ((GstVecDeque *) array, p_struct);
}

/**
 * gst_queue_array_push_tail: (skip)
 * @array: a #GstQueueArray object
 * @data: object to push
 *
 * Pushes @data to the tail of the queue @array.
 *
 * Since: 1.2
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_push_tail (GstQueueArray * array, gpointer data)
{
  gst_vec_deque_push_tail ((GstVecDeque *) array, data);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_push_sorted (GstQueueArray * array, gpointer data,
    GCompareDataFunc func, gpointer user_data)
{
  gst_vec_deque_push_sorted ((GstVecDeque *) array, data, func, user_data);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_push_sorted_struct (GstQueueArray * array, gpointer p_struct,
    GCompareDataFunc func, gpointer user_data)
{
  gst_vec_deque_push_sorted_struct ((GstVecDeque *) array, p_struct, func,
      user_data);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
void
gst_queue_array_sort (GstQueueArray * array, GCompareDataFunc compare_func,
    gpointer user_data)
{
  gst_vec_deque_sort ((GstVecDeque *) array, compare_func, user_data);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_tail (GstQueueArray * array)
{
  return gst_vec_deque_peek_tail ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_peek_tail_struct (GstQueueArray * array)
{
  return gst_vec_deque_peek_tail_struct ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_pop_tail (GstQueueArray * array)
{
  return gst_vec_deque_pop_tail ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_pop_tail_struct (GstQueueArray * array)
{
  return gst_vec_deque_pop_tail_struct ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gboolean
gst_queue_array_is_empty (GstQueueArray * array)
{
  return gst_vec_deque_is_empty ((GstVecDeque *) array);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gboolean
gst_queue_array_drop_struct (GstQueueArray * array, guint idx,
    gpointer p_struct)
{
  return gst_vec_deque_drop_struct ((GstVecDeque *) array, idx, p_struct);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
gpointer
gst_queue_array_drop_element (GstQueueArray * array, guint idx)
{
  return gst_vec_deque_drop_element ((GstVecDeque *) array, idx);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
guint
gst_queue_array_find (GstQueueArray * array, GCompareFunc func, gpointer data)
{
  return gst_vec_deque_find ((GstVecDeque *) array, func, data);
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
 * Deprecated: 1.26: Use #GstVecDeque instead.
 */
guint
gst_queue_array_get_length (GstQueueArray * array)
{
  return gst_vec_deque_get_length ((GstVecDeque *) array);
}
