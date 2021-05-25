/* GLIB - Library of useful routines for C programming
 * Copyright (C) 2002, 2003, 2004, 2005, 2006, 2007
 * Soeren Sandmann (sandmann@daimi.au.dk)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#include "config.h"

#include "gsequence.h"

#include "gmem.h"
#include "gtestutils.h"
#include "gslice.h"
/**
 * SECTION:sequence
 * @title: Sequences
 * @short_description: scalable lists
 *
 * The #GSequence data structure has the API of a list, but is
 * implemented internally with a balanced binary tree. This means that
 * most of the operations  (access, search, insertion, deletion, ...) on
 * #GSequence are O(log(n)) in average and O(n) in worst case for time
 * complexity. But, note that maintaining a balanced sorted list of n
 * elements is done in time O(n log(n)).
 * The data contained in each element can be either integer values, by using
 * of the [Type Conversion Macros][glib-Type-Conversion-Macros], or simply
 * pointers to any type of data.
 *
 * A #GSequence is accessed through "iterators", represented by a
 * #GSequenceIter. An iterator represents a position between two
 * elements of the sequence. For example, the "begin" iterator
 * represents the gap immediately before the first element of the
 * sequence, and the "end" iterator represents the gap immediately
 * after the last element. In an empty sequence, the begin and end
 * iterators are the same.
 *
 * Some methods on #GSequence operate on ranges of items. For example
 * g_sequence_foreach_range() will call a user-specified function on
 * each element with the given range. The range is delimited by the
 * gaps represented by the passed-in iterators, so if you pass in the
 * begin and end iterators, the range in question is the entire
 * sequence.
 *
 * The function g_sequence_get() is used with an iterator to access the
 * element immediately following the gap that the iterator represents.
 * The iterator is said to "point" to that element.
 *
 * Iterators are stable across most operations on a #GSequence. For
 * example an iterator pointing to some element of a sequence will
 * continue to point to that element even after the sequence is sorted.
 * Even moving an element to another sequence using for example
 * g_sequence_move_range() will not invalidate the iterators pointing
 * to it. The only operation that will invalidate an iterator is when
 * the element it points to is removed from any sequence.
 *
 * To sort the data, either use g_sequence_insert_sorted() or
 * g_sequence_insert_sorted_iter() to add data to the #GSequence or, if
 * you want to add a large amount of data, it is more efficient to call
 * g_sequence_sort() or g_sequence_sort_iter() after doing unsorted
 * insertions.
 */

/**
 * GSequenceIter:
 *
 * The #GSequenceIter struct is an opaque data type representing an
 * iterator pointing into a #GSequence.
 */

/**
 * GSequenceIterCompareFunc:
 * @a: a #GSequenceIter
 * @b: a #GSequenceIter
 * @data: user data
 *
 * A #GSequenceIterCompareFunc is a function used to compare iterators.
 * It must return zero if the iterators compare equal, a negative value
 * if @a comes before @b, and a positive value if @b comes before @a.
 *
 * Returns: zero if the iterators are equal, a negative value if @a
 *     comes before @b, and a positive value if @b comes before @a.
 */

typedef struct _GSequenceNode GSequenceNode;

/**
 * GSequence:
 *
 * The #GSequence struct is an opaque data type representing a
 * [sequence][glib-Sequences] data type.
 */
struct _GSequence
{
  GSequenceNode *       end_node;
  GDestroyNotify        data_destroy_notify;
  gboolean              access_prohibited;

  /* The 'real_sequence' is used when temporary sequences are created
   * to hold nodes that are being rearranged. The 'real_sequence' of such
   * a temporary sequence points to the sequence that is actually being
   * manipulated. The only reason we need this is so that when the
   * sort/sort_changed/search_iter() functions call out to the application
   * g_sequence_iter_get_sequence() will return the correct sequence.
   */
  GSequence *           real_sequence;
};

struct _GSequenceNode
{
  gint                  n_nodes;
  GSequenceNode *       parent;
  GSequenceNode *       left;
  GSequenceNode *       right;
  gpointer              data;   /* For the end node, this field points
                                 * to the sequence
                                 */
};

/*
 * Declaration of GSequenceNode methods
 */
static GSequenceNode *node_new           (gpointer                  data);
static GSequenceNode *node_get_first     (GSequenceNode            *node);
static GSequenceNode *node_get_last      (GSequenceNode            *node);
static GSequenceNode *node_get_prev      (GSequenceNode            *node);
static GSequenceNode *node_get_next      (GSequenceNode            *node);
static gint           node_get_pos       (GSequenceNode            *node);
static GSequenceNode *node_get_by_pos    (GSequenceNode            *node,
                                          gint                      pos);
static GSequenceNode *node_find          (GSequenceNode            *haystack,
                                          GSequenceNode            *needle,
                                          GSequenceNode            *end,
                                          GSequenceIterCompareFunc  cmp,
                                          gpointer                  user_data);
static GSequenceNode *node_find_closest  (GSequenceNode            *haystack,
                                          GSequenceNode            *needle,
                                          GSequenceNode            *end,
                                          GSequenceIterCompareFunc  cmp,
                                          gpointer                  user_data);
static gint           node_get_length    (GSequenceNode            *node);
static void           node_free          (GSequenceNode            *node,
                                          GSequence                *seq);
static void           node_cut           (GSequenceNode            *split);
static void           node_insert_before (GSequenceNode            *node,
                                          GSequenceNode            *new);
static void           node_unlink        (GSequenceNode            *node);
static void           node_join          (GSequenceNode            *left,
                                          GSequenceNode            *right);
static void           node_insert_sorted (GSequenceNode            *node,
                                          GSequenceNode            *new,
                                          GSequenceNode            *end,
                                          GSequenceIterCompareFunc  cmp_func,
                                          gpointer                  cmp_data);


/*
 * Various helper functions
 */
static void
check_seq_access (GSequence *seq)
{
  if (G_UNLIKELY (seq->access_prohibited))
    {
      g_warning ("Accessing a sequence while it is "
                 "being sorted or searched is not allowed");
    }
}

static GSequence *
get_sequence (GSequenceNode *node)
{
  return (GSequence *)node_get_last (node)->data;
}

static gboolean
seq_is_end (GSequence     *seq,
            GSequenceIter *iter)
{
  return seq->end_node == iter;
}

static gboolean
is_end (GSequenceIter *iter)
{
  GSequenceIter *parent = iter->parent;

  if (iter->right)
    return FALSE;

  if (!parent)
    return TRUE;

  while (parent->right == iter)
    {
      iter = parent;
      parent = iter->parent;

      if (!parent)
        return TRUE;
    }

  return FALSE;
}

typedef struct
{
  GCompareDataFunc  cmp_func;
  gpointer          cmp_data;
  GSequenceNode    *end_node;
} SortInfo;

/* This function compares two iters using a normal compare
 * function and user_data passed in in a SortInfo struct
 */
static gint
iter_compare (GSequenceIter *node1,
              GSequenceIter *node2,
              gpointer       data)
{
  const SortInfo *info = data;
  gint retval;

  if (node1 == info->end_node)
    return 1;

  if (node2 == info->end_node)
    return -1;

  retval = info->cmp_func (node1->data, node2->data, info->cmp_data);

  return retval;
}

/*
 * Public API
 */

/**
 * g_sequence_new:
 * @data_destroy: (nullable): a #GDestroyNotify function, or %NULL
 *
 * Creates a new GSequence. The @data_destroy function, if non-%NULL will
 * be called on all items when the sequence is destroyed and on items that
 * are removed from the sequence.
 *
 * Returns: (transfer full): a new #GSequence
 *
 * Since: 2.14
 **/
GSequence *
g_sequence_new (GDestroyNotify data_destroy)
{
  GSequence *seq = g_new (GSequence, 1);
  seq->data_destroy_notify = data_destroy;

  seq->end_node = node_new (seq);

  seq->access_prohibited = FALSE;

  seq->real_sequence = seq;

  return seq;
}

/**
 * g_sequence_free:
 * @seq: a #GSequence
 *
 * Frees the memory allocated for @seq. If @seq has a data destroy
 * function associated with it, that function is called on all items
 * in @seq.
 *
 * Since: 2.14
 */
void
g_sequence_free (GSequence *seq)
{
  g_return_if_fail (seq != NULL);

  check_seq_access (seq);

  node_free (seq->end_node, seq);

  g_free (seq);
}

/**
 * g_sequence_foreach_range:
 * @begin: a #GSequenceIter
 * @end: a #GSequenceIter
 * @func: a #GFunc
 * @user_data: user data passed to @func
 *
 * Calls @func for each item in the range (@begin, @end) passing
 * @user_data to the function. @func must not modify the sequence
 * itself.
 *
 * Since: 2.14
 */
void
g_sequence_foreach_range (GSequenceIter *begin,
                          GSequenceIter *end,
                          GFunc          func,
                          gpointer       user_data)
{
  GSequence *seq;
  GSequenceIter *iter;

  g_return_if_fail (func != NULL);
  g_return_if_fail (begin != NULL);
  g_return_if_fail (end != NULL);

  seq = get_sequence (begin);

  seq->access_prohibited = TRUE;

  iter = begin;
  while (iter != end)
    {
      GSequenceIter *next = node_get_next (iter);

      func (iter->data, user_data);

      iter = next;
    }

  seq->access_prohibited = FALSE;
}

/**
 * g_sequence_foreach:
 * @seq: a #GSequence
 * @func: the function to call for each item in @seq
 * @user_data: user data passed to @func
 *
 * Calls @func for each item in the sequence passing @user_data
 * to the function. @func must not modify the sequence itself.
 *
 * Since: 2.14
 */
void
g_sequence_foreach (GSequence *seq,
                    GFunc      func,
                    gpointer   user_data)
{
  GSequenceIter *begin, *end;

  check_seq_access (seq);

  begin = g_sequence_get_begin_iter (seq);
  end   = g_sequence_get_end_iter (seq);

  g_sequence_foreach_range (begin, end, func, user_data);
}

/**
 * g_sequence_range_get_midpoint:
 * @begin: a #GSequenceIter
 * @end: a #GSequenceIter
 *
 * Finds an iterator somewhere in the range (@begin, @end). This
 * iterator will be close to the middle of the range, but is not
 * guaranteed to be exactly in the middle.
 *
 * The @begin and @end iterators must both point to the same sequence
 * and @begin must come before or be equal to @end in the sequence.
 *
 * Returns: (transfer none): a #GSequenceIter pointing somewhere in the
 *    (@begin, @end) range
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_range_get_midpoint (GSequenceIter *begin,
                               GSequenceIter *end)
{
  int begin_pos, end_pos, mid_pos;

  g_return_val_if_fail (begin != NULL, NULL);
  g_return_val_if_fail (end != NULL, NULL);
  g_return_val_if_fail (get_sequence (begin) == get_sequence (end), NULL);

  begin_pos = node_get_pos (begin);
  end_pos = node_get_pos (end);

  g_return_val_if_fail (end_pos >= begin_pos, NULL);

  mid_pos = begin_pos + (end_pos - begin_pos) / 2;

  return node_get_by_pos (begin, mid_pos);
}

/**
 * g_sequence_iter_compare:
 * @a: a #GSequenceIter
 * @b: a #GSequenceIter
 *
 * Returns a negative number if @a comes before @b, 0 if they are equal,
 * and a positive number if @a comes after @b.
 *
 * The @a and @b iterators must point into the same sequence.
 *
 * Returns: a negative number if @a comes before @b, 0 if they are
 *     equal, and a positive number if @a comes after @b
 *
 * Since: 2.14
 */
gint
g_sequence_iter_compare (GSequenceIter *a,
                         GSequenceIter *b)
{
  gint a_pos, b_pos;
  GSequence *seq_a, *seq_b;

  g_return_val_if_fail (a != NULL, 0);
  g_return_val_if_fail (b != NULL, 0);

  seq_a = get_sequence (a);
  seq_b = get_sequence (b);
  g_return_val_if_fail (seq_a == seq_b, 0);

  check_seq_access (seq_a);
  check_seq_access (seq_b);

  a_pos = node_get_pos (a);
  b_pos = node_get_pos (b);

  if (a_pos == b_pos)
    return 0;
  else if (a_pos > b_pos)
    return 1;
  else
    return -1;
}

/**
 * g_sequence_append:
 * @seq: a #GSequence
 * @data: the data for the new item
 *
 * Adds a new item to the end of @seq.
 *
 * Returns: (transfer none): an iterator pointing to the new item
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_append (GSequence *seq,
                   gpointer   data)
{
  GSequenceNode *node;

  g_return_val_if_fail (seq != NULL, NULL);

  check_seq_access (seq);

  node = node_new (data);
  node_insert_before (seq->end_node, node);

  return node;
}

/**
 * g_sequence_prepend:
 * @seq: a #GSequence
 * @data: the data for the new item
 *
 * Adds a new item to the front of @seq
 *
 * Returns: (transfer none): an iterator pointing to the new item
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_prepend (GSequence *seq,
                    gpointer   data)
{
  GSequenceNode *node, *first;

  g_return_val_if_fail (seq != NULL, NULL);

  check_seq_access (seq);

  node = node_new (data);
  first = node_get_first (seq->end_node);

  node_insert_before (first, node);

  return node;
}

/**
 * g_sequence_insert_before:
 * @iter: a #GSequenceIter
 * @data: the data for the new item
 *
 * Inserts a new item just before the item pointed to by @iter.
 *
 * Returns: (transfer none): an iterator pointing to the new item
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_insert_before (GSequenceIter *iter,
                          gpointer       data)
{
  GSequence *seq;
  GSequenceNode *node;

  g_return_val_if_fail (iter != NULL, NULL);

  seq = get_sequence (iter);
  check_seq_access (seq);

  node = node_new (data);

  node_insert_before (iter, node);

  return node;
}

/**
 * g_sequence_remove:
 * @iter: a #GSequenceIter
 *
 * Removes the item pointed to by @iter. It is an error to pass the
 * end iterator to this function.
 *
 * If the sequence has a data destroy function associated with it, this
 * function is called on the data for the removed item.
 *
 * Since: 2.14
 */
void
g_sequence_remove (GSequenceIter *iter)
{
  GSequence *seq;

  g_return_if_fail (iter != NULL);

  seq = get_sequence (iter);
  g_return_if_fail (!seq_is_end (seq, iter));

  check_seq_access (seq);

  node_unlink (iter);
  node_free (iter, seq);
}

/**
 * g_sequence_remove_range:
 * @begin: a #GSequenceIter
 * @end: a #GSequenceIter
 *
 * Removes all items in the (@begin, @end) range.
 *
 * If the sequence has a data destroy function associated with it, this
 * function is called on the data for the removed items.
 *
 * Since: 2.14
 */
void
g_sequence_remove_range (GSequenceIter *begin,
                         GSequenceIter *end)
{
  GSequence *seq_begin, *seq_end;

  seq_begin = get_sequence (begin);
  seq_end = get_sequence (end);
  g_return_if_fail (seq_begin == seq_end);
  /* check_seq_access() calls are done by g_sequence_move_range() */

  g_sequence_move_range (NULL, begin, end);
}

/**
 * g_sequence_move_range:
 * @dest: a #GSequenceIter
 * @begin: a #GSequenceIter
 * @end: a #GSequenceIter
 *
 * Inserts the (@begin, @end) range at the destination pointed to by @dest.
 * The @begin and @end iters must point into the same sequence. It is
 * allowed for @dest to point to a different sequence than the one pointed
 * into by @begin and @end.
 *
 * If @dest is %NULL, the range indicated by @begin and @end is
 * removed from the sequence. If @dest points to a place within
 * the (@begin, @end) range, the range does not move.
 *
 * Since: 2.14
 */
void
g_sequence_move_range (GSequenceIter *dest,
                       GSequenceIter *begin,
                       GSequenceIter *end)
{
  GSequence *src_seq, *end_seq, *dest_seq;
  GSequenceNode *first;

  g_return_if_fail (begin != NULL);
  g_return_if_fail (end != NULL);

  src_seq = get_sequence (begin);
  check_seq_access (src_seq);

  end_seq = get_sequence (end);
  check_seq_access (end_seq);

  if (dest)
    {
      dest_seq = get_sequence (dest);
      check_seq_access (dest_seq);
    }

  g_return_if_fail (src_seq == end_seq);

  /* Dest points to begin or end? */
  if (dest == begin || dest == end)
    return;

  /* begin comes after end? */
  if (g_sequence_iter_compare (begin, end) >= 0)
    return;

  /* dest points somewhere in the (begin, end) range? */
  if (dest && dest_seq == src_seq &&
      g_sequence_iter_compare (dest, begin) > 0 &&
      g_sequence_iter_compare (dest, end) < 0)
    {
      return;
    }

  first = node_get_first (begin);

  node_cut (begin);

  node_cut (end);

  if (first != begin)
    node_join (first, end);

  if (dest)
    {
      first = node_get_first (dest);

      node_cut (dest);

      node_join (begin, dest);

      if (dest != first)
        node_join (first, begin);
    }
  else
    {
      node_free (begin, src_seq);
    }
}

/**
 * g_sequence_sort:
 * @seq: a #GSequence
 * @cmp_func: the function used to sort the sequence
 * @cmp_data: user data passed to @cmp_func
 *
 * Sorts @seq using @cmp_func.
 *
 * @cmp_func is passed two items of @seq and should
 * return 0 if they are equal, a negative value if the
 * first comes before the second, and a positive value
 * if the second comes before the first.
 *
 * Since: 2.14
 */
void
g_sequence_sort (GSequence        *seq,
                 GCompareDataFunc  cmp_func,
                 gpointer          cmp_data)
{
  SortInfo info;

  info.cmp_func = cmp_func;
  info.cmp_data = cmp_data;
  info.end_node = seq->end_node;

  check_seq_access (seq);

  g_sequence_sort_iter (seq, iter_compare, &info);
}

/**
 * g_sequence_insert_sorted:
 * @seq: a #GSequence
 * @data: the data to insert
 * @cmp_func: the function used to compare items in the sequence
 * @cmp_data: user data passed to @cmp_func.
 *
 * Inserts @data into @seq using @cmp_func to determine the new
 * position. The sequence must already be sorted according to @cmp_func;
 * otherwise the new position of @data is undefined.
 *
 * @cmp_func is called with two items of the @seq, and @cmp_data.
 * It should return 0 if the items are equal, a negative value
 * if the first item comes before the second, and a positive value
 * if the second item comes before the first.
 *
 * Note that when adding a large amount of data to a #GSequence,
 * it is more efficient to do unsorted insertions and then call
 * g_sequence_sort() or g_sequence_sort_iter().
 *
 * Returns: (transfer none): a #GSequenceIter pointing to the new item.
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_insert_sorted (GSequence        *seq,
                          gpointer          data,
                          GCompareDataFunc  cmp_func,
                          gpointer          cmp_data)
{
  SortInfo info;

  g_return_val_if_fail (seq != NULL, NULL);
  g_return_val_if_fail (cmp_func != NULL, NULL);

  info.cmp_func = cmp_func;
  info.cmp_data = cmp_data;
  info.end_node = seq->end_node;
  check_seq_access (seq);

  return g_sequence_insert_sorted_iter (seq, data, iter_compare, &info);
}

/**
 * g_sequence_sort_changed:
 * @iter: A #GSequenceIter
 * @cmp_func: the function used to compare items in the sequence
 * @cmp_data: user data passed to @cmp_func.
 *
 * Moves the data pointed to by @iter to a new position as indicated by
 * @cmp_func. This
 * function should be called for items in a sequence already sorted according
 * to @cmp_func whenever some aspect of an item changes so that @cmp_func
 * may return different values for that item.
 *
 * @cmp_func is called with two items of the @seq, and @cmp_data.
 * It should return 0 if the items are equal, a negative value if
 * the first item comes before the second, and a positive value if
 * the second item comes before the first.
 *
 * Since: 2.14
 */
void
g_sequence_sort_changed (GSequenceIter    *iter,
                         GCompareDataFunc  cmp_func,
                         gpointer          cmp_data)
{
  GSequence *seq;
  SortInfo info;

  g_return_if_fail (iter != NULL);

  seq = get_sequence (iter);
  /* check_seq_access() call is done by g_sequence_sort_changed_iter() */
  g_return_if_fail (!seq_is_end (seq, iter));

  info.cmp_func = cmp_func;
  info.cmp_data = cmp_data;
  info.end_node = seq->end_node;

  g_sequence_sort_changed_iter (iter, iter_compare, &info);
}

/**
 * g_sequence_search:
 * @seq: a #GSequence
 * @data: data for the new item
 * @cmp_func: the function used to compare items in the sequence
 * @cmp_data: user data passed to @cmp_func
 *
 * Returns an iterator pointing to the position where @data would
 * be inserted according to @cmp_func and @cmp_data.
 *
 * @cmp_func is called with two items of the @seq, and @cmp_data.
 * It should return 0 if the items are equal, a negative value if
 * the first item comes before the second, and a positive value if
 * the second item comes before the first.
 *
 * If you are simply searching for an existing element of the sequence,
 * consider using g_sequence_lookup().
 *
 * This function will fail if the data contained in the sequence is
 * unsorted.
 *
 * Returns: (transfer none): an #GSequenceIter pointing to the position where @data
 *     would have been inserted according to @cmp_func and @cmp_data
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_search (GSequence        *seq,
                   gpointer          data,
                   GCompareDataFunc  cmp_func,
                   gpointer          cmp_data)
{
  SortInfo info;

  g_return_val_if_fail (seq != NULL, NULL);

  info.cmp_func = cmp_func;
  info.cmp_data = cmp_data;
  info.end_node = seq->end_node;
  check_seq_access (seq);

  return g_sequence_search_iter (seq, data, iter_compare, &info);
}

/**
 * g_sequence_lookup:
 * @seq: a #GSequence
 * @data: data to look up
 * @cmp_func: the function used to compare items in the sequence
 * @cmp_data: user data passed to @cmp_func
 *
 * Returns an iterator pointing to the position of the first item found
 * equal to @data according to @cmp_func and @cmp_data. If more than one
 * item is equal, it is not guaranteed that it is the first which is
 * returned. In that case, you can use g_sequence_iter_next() and
 * g_sequence_iter_prev() to get others.
 *
 * @cmp_func is called with two items of the @seq, and @cmp_data.
 * It should return 0 if the items are equal, a negative value if
 * the first item comes before the second, and a positive value if
 * the second item comes before the first.
 *
 * This function will fail if the data contained in the sequence is
 * unsorted.
 *
 * Returns: (transfer none) (nullable): an #GSequenceIter pointing to the position of the
 *     first item found equal to @data according to @cmp_func and
 *     @cmp_data, or %NULL if no such item exists
 *
 * Since: 2.28
 */
GSequenceIter *
g_sequence_lookup (GSequence        *seq,
                   gpointer          data,
                   GCompareDataFunc  cmp_func,
                   gpointer          cmp_data)
{
  SortInfo info;

  g_return_val_if_fail (seq != NULL, NULL);

  info.cmp_func = cmp_func;
  info.cmp_data = cmp_data;
  info.end_node = seq->end_node;
  check_seq_access (seq);

  return g_sequence_lookup_iter (seq, data, iter_compare, &info);
}

/**
 * g_sequence_sort_iter:
 * @seq: a #GSequence
 * @cmp_func: the function used to compare iterators in the sequence
 * @cmp_data: user data passed to @cmp_func
 *
 * Like g_sequence_sort(), but uses a #GSequenceIterCompareFunc instead
 * of a #GCompareDataFunc as the compare function
 *
 * @cmp_func is called with two iterators pointing into @seq. It should
 * return 0 if the iterators are equal, a negative value if the first
 * iterator comes before the second, and a positive value if the second
 * iterator comes before the first.
 *
 * Since: 2.14
 */
void
g_sequence_sort_iter (GSequence                *seq,
                      GSequenceIterCompareFunc  cmp_func,
                      gpointer                  cmp_data)
{
  GSequence *tmp;
  GSequenceNode *begin, *end;

  g_return_if_fail (seq != NULL);
  g_return_if_fail (cmp_func != NULL);

  check_seq_access (seq);

  begin = g_sequence_get_begin_iter (seq);
  end   = g_sequence_get_end_iter (seq);

  tmp = g_sequence_new (NULL);
  tmp->real_sequence = seq;

  g_sequence_move_range (g_sequence_get_begin_iter (tmp), begin, end);

  seq->access_prohibited = TRUE;
  tmp->access_prohibited = TRUE;

  while (!g_sequence_is_empty (tmp))
    {
      GSequenceNode *node = g_sequence_get_begin_iter (tmp);

      node_insert_sorted (seq->end_node, node, seq->end_node,
                          cmp_func, cmp_data);
    }

  tmp->access_prohibited = FALSE;
  seq->access_prohibited = FALSE;

  g_sequence_free (tmp);
}

/**
 * g_sequence_sort_changed_iter:
 * @iter: a #GSequenceIter
 * @iter_cmp: the function used to compare iterators in the sequence
 * @cmp_data: user data passed to @cmp_func
 *
 * Like g_sequence_sort_changed(), but uses
 * a #GSequenceIterCompareFunc instead of a #GCompareDataFunc as
 * the compare function.
 *
 * @iter_cmp is called with two iterators pointing into the #GSequence that
 * @iter points into. It should
 * return 0 if the iterators are equal, a negative value if the first
 * iterator comes before the second, and a positive value if the second
 * iterator comes before the first.
 *
 * Since: 2.14
 */
void
g_sequence_sort_changed_iter (GSequenceIter            *iter,
                              GSequenceIterCompareFunc  iter_cmp,
                              gpointer                  cmp_data)
{
  GSequence *seq, *tmp_seq;
  GSequenceIter *next, *prev;

  g_return_if_fail (iter != NULL);
  g_return_if_fail (iter_cmp != NULL);

  seq = get_sequence (iter);
  g_return_if_fail (!seq_is_end (seq, iter));

  check_seq_access (seq);

  /* If one of the neighbours is equal to iter, then
   * don't move it. This ensures that sort_changed() is
   * a stable operation.
   */

  next = node_get_next (iter);
  prev = node_get_prev (iter);

  if (prev != iter && iter_cmp (prev, iter, cmp_data) == 0)
    return;

  if (!is_end (next) && iter_cmp (next, iter, cmp_data) == 0)
    return;

  seq->access_prohibited = TRUE;

  tmp_seq = g_sequence_new (NULL);
  tmp_seq->real_sequence = seq;

  node_unlink (iter);
  node_insert_before (tmp_seq->end_node, iter);

  node_insert_sorted (seq->end_node, iter, seq->end_node,
                      iter_cmp, cmp_data);

  g_sequence_free (tmp_seq);

  seq->access_prohibited = FALSE;
}

/**
 * g_sequence_insert_sorted_iter:
 * @seq: a #GSequence
 * @data: data for the new item
 * @iter_cmp: the function used to compare iterators in the sequence
 * @cmp_data: user data passed to @iter_cmp
 *
 * Like g_sequence_insert_sorted(), but uses
 * a #GSequenceIterCompareFunc instead of a #GCompareDataFunc as
 * the compare function.
 *
 * @iter_cmp is called with two iterators pointing into @seq.
 * It should return 0 if the iterators are equal, a negative
 * value if the first iterator comes before the second, and a
 * positive value if the second iterator comes before the first.
 *
 * Note that when adding a large amount of data to a #GSequence,
 * it is more efficient to do unsorted insertions and then call
 * g_sequence_sort() or g_sequence_sort_iter().
 *
 * Returns: (transfer none): a #GSequenceIter pointing to the new item
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_insert_sorted_iter (GSequence                *seq,
                               gpointer                  data,
                               GSequenceIterCompareFunc  iter_cmp,
                               gpointer                  cmp_data)
{
  GSequenceNode *new_node;
  GSequence *tmp_seq;

  g_return_val_if_fail (seq != NULL, NULL);
  g_return_val_if_fail (iter_cmp != NULL, NULL);

  check_seq_access (seq);

  seq->access_prohibited = TRUE;

  /* Create a new temporary sequence and put the new node into
   * that. The reason for this is that the user compare function
   * will be called with the new node, and if it dereferences,
   * "is_end" will be called on it. But that will crash if the
   * node is not actually in a sequence.
   *
   * node_insert_sorted() makes sure the node is unlinked before
   * it is inserted.
   *
   * The reason we need the "iter" versions at all is that that
   * is the only kind of compare functions GtkTreeView can use.
   */
  tmp_seq = g_sequence_new (NULL);
  tmp_seq->real_sequence = seq;

  new_node = g_sequence_append (tmp_seq, data);

  node_insert_sorted (seq->end_node, new_node,
                      seq->end_node, iter_cmp, cmp_data);

  g_sequence_free (tmp_seq);

  seq->access_prohibited = FALSE;

  return new_node;
}

/**
 * g_sequence_search_iter:
 * @seq: a #GSequence
 * @data: data for the new item
 * @iter_cmp: the function used to compare iterators in the sequence
 * @cmp_data: user data passed to @iter_cmp
 *
 * Like g_sequence_search(), but uses a #GSequenceIterCompareFunc
 * instead of a #GCompareDataFunc as the compare function.
 *
 * @iter_cmp is called with two iterators pointing into @seq.
 * It should return 0 if the iterators are equal, a negative value
 * if the first iterator comes before the second, and a positive
 * value if the second iterator comes before the first.
 *
 * If you are simply searching for an existing element of the sequence,
 * consider using g_sequence_lookup_iter().
 *
 * This function will fail if the data contained in the sequence is
 * unsorted.
 *
 * Returns: (transfer none): a #GSequenceIter pointing to the position in @seq
 *     where @data would have been inserted according to @iter_cmp
 *     and @cmp_data
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_search_iter (GSequence                *seq,
                        gpointer                  data,
                        GSequenceIterCompareFunc  iter_cmp,
                        gpointer                  cmp_data)
{
  GSequenceNode *node;
  GSequenceNode *dummy;
  GSequence *tmp_seq;

  g_return_val_if_fail (seq != NULL, NULL);

  check_seq_access (seq);

  seq->access_prohibited = TRUE;

  tmp_seq = g_sequence_new (NULL);
  tmp_seq->real_sequence = seq;

  dummy = g_sequence_append (tmp_seq, data);

  node = node_find_closest (seq->end_node, dummy,
                            seq->end_node, iter_cmp, cmp_data);

  g_sequence_free (tmp_seq);

  seq->access_prohibited = FALSE;

  return node;
}

/**
 * g_sequence_lookup_iter:
 * @seq: a #GSequence
 * @data: data to look up
 * @iter_cmp: the function used to compare iterators in the sequence
 * @cmp_data: user data passed to @iter_cmp
 *
 * Like g_sequence_lookup(), but uses a #GSequenceIterCompareFunc
 * instead of a #GCompareDataFunc as the compare function.
 *
 * @iter_cmp is called with two iterators pointing into @seq.
 * It should return 0 if the iterators are equal, a negative value
 * if the first iterator comes before the second, and a positive
 * value if the second iterator comes before the first.
 *
 * This function will fail if the data contained in the sequence is
 * unsorted.
 *
 * Returns: (transfer none) (nullable): an #GSequenceIter pointing to the position of
 *     the first item found equal to @data according to @iter_cmp
 *     and @cmp_data, or %NULL if no such item exists
 *
 * Since: 2.28
 */
GSequenceIter *
g_sequence_lookup_iter (GSequence                *seq,
                        gpointer                  data,
                        GSequenceIterCompareFunc  iter_cmp,
                        gpointer                  cmp_data)
{
  GSequenceNode *node;
  GSequenceNode *dummy;
  GSequence *tmp_seq;

  g_return_val_if_fail (seq != NULL, NULL);

  check_seq_access (seq);

  seq->access_prohibited = TRUE;

  tmp_seq = g_sequence_new (NULL);
  tmp_seq->real_sequence = seq;

  dummy = g_sequence_append (tmp_seq, data);

  node = node_find (seq->end_node, dummy,
                    seq->end_node, iter_cmp, cmp_data);

  g_sequence_free (tmp_seq);

  seq->access_prohibited = FALSE;

  return node;
}

/**
 * g_sequence_iter_get_sequence:
 * @iter: a #GSequenceIter
 *
 * Returns the #GSequence that @iter points into.
 *
 * Returns: (transfer none): the #GSequence that @iter points into
 *
 * Since: 2.14
 */
GSequence *
g_sequence_iter_get_sequence (GSequenceIter *iter)
{
  GSequence *seq;

  g_return_val_if_fail (iter != NULL, NULL);

  seq = get_sequence (iter);

  /* For temporary sequences, this points to the sequence that
   * is actually being manipulated
   */
  return seq->real_sequence;
}

/**
 * g_sequence_get:
 * @iter: a #GSequenceIter
 *
 * Returns the data that @iter points to.
 *
 * Returns: (transfer none): the data that @iter points to
 *
 * Since: 2.14
 */
gpointer
g_sequence_get (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, NULL);
  g_return_val_if_fail (!is_end (iter), NULL);

  return iter->data;
}

/**
 * g_sequence_set:
 * @iter: a #GSequenceIter
 * @data: new data for the item
 *
 * Changes the data for the item pointed to by @iter to be @data. If
 * the sequence has a data destroy function associated with it, that
 * function is called on the existing data that @iter pointed to.
 *
 * Since: 2.14
 */
void
g_sequence_set (GSequenceIter *iter,
                gpointer       data)
{
  GSequence *seq;

  g_return_if_fail (iter != NULL);

  seq = get_sequence (iter);
  g_return_if_fail (!seq_is_end (seq, iter));

  /* If @data is identical to iter->data, it is destroyed
   * here. This will work right in case of ref-counted objects. Also
   * it is similar to what ghashtables do.
   *
   * For non-refcounted data it's a little less convenient, but
   * code relying on self-setting not destroying would be
   * pretty dubious anyway ...
   */

  if (seq->data_destroy_notify)
    seq->data_destroy_notify (iter->data);

  iter->data = data;
}

/**
 * g_sequence_get_length:
 * @seq: a #GSequence
 *
 * Returns the length of @seq. Note that this method is O(h) where `h' is the
 * height of the tree. It is thus more efficient to use g_sequence_is_empty()
 * when comparing the length to zero.
 *
 * Returns: the length of @seq
 *
 * Since: 2.14
 */
gint
g_sequence_get_length (GSequence *seq)
{
  return node_get_length (seq->end_node) - 1;
}

/**
 * g_sequence_is_empty:
 * @seq: a #GSequence
 *
 * Returns %TRUE if the sequence contains zero items.
 *
 * This function is functionally identical to checking the result of
 * g_sequence_get_length() being equal to zero. However this function is
 * implemented in O(1) running time.
 *
 * Returns: %TRUE if the sequence is empty, otherwise %FALSE.
 *
 * Since: 2.48
 */
gboolean
g_sequence_is_empty (GSequence *seq)
{
  return (seq->end_node->parent == NULL) && (seq->end_node->left == NULL);
}

/**
 * g_sequence_get_end_iter:
 * @seq: a #GSequence
 *
 * Returns the end iterator for @seg
 *
 * Returns: (transfer none): the end iterator for @seq
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_get_end_iter (GSequence *seq)
{
  g_return_val_if_fail (seq != NULL, NULL);

  return seq->end_node;
}

/**
 * g_sequence_get_begin_iter:
 * @seq: a #GSequence
 *
 * Returns the begin iterator for @seq.
 *
 * Returns: (transfer none): the begin iterator for @seq.
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_get_begin_iter (GSequence *seq)
{
  g_return_val_if_fail (seq != NULL, NULL);

  return node_get_first (seq->end_node);
}

static int
clamp_position (GSequence *seq,
                int        pos)
{
  gint len = g_sequence_get_length (seq);

  if (pos > len || pos < 0)
    pos = len;

  return pos;
}

/**
 * g_sequence_get_iter_at_pos:
 * @seq: a #GSequence
 * @pos: a position in @seq, or -1 for the end
 *
 * Returns the iterator at position @pos. If @pos is negative or larger
 * than the number of items in @seq, the end iterator is returned.
 *
 * Returns: (transfer none): The #GSequenceIter at position @pos
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_get_iter_at_pos (GSequence *seq,
                            gint       pos)
{
  g_return_val_if_fail (seq != NULL, NULL);

  pos = clamp_position (seq, pos);

  return node_get_by_pos (seq->end_node, pos);
}

/**
 * g_sequence_move:
 * @src: a #GSequenceIter pointing to the item to move
 * @dest: a #GSequenceIter pointing to the position to which
 *     the item is moved
 *
 * Moves the item pointed to by @src to the position indicated by @dest.
 * After calling this function @dest will point to the position immediately
 * after @src. It is allowed for @src and @dest to point into different
 * sequences.
 *
 * Since: 2.14
 **/
void
g_sequence_move (GSequenceIter *src,
                 GSequenceIter *dest)
{
  g_return_if_fail (src != NULL);
  g_return_if_fail (dest != NULL);
  g_return_if_fail (!is_end (src));

  if (src == dest)
    return;

  node_unlink (src);
  node_insert_before (dest, src);
}

/* GSequenceIter */

/**
 * g_sequence_iter_is_end:
 * @iter: a #GSequenceIter
 *
 * Returns whether @iter is the end iterator
 *
 * Returns: Whether @iter is the end iterator
 *
 * Since: 2.14
 */
gboolean
g_sequence_iter_is_end (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, FALSE);

  return is_end (iter);
}

/**
 * g_sequence_iter_is_begin:
 * @iter: a #GSequenceIter
 *
 * Returns whether @iter is the begin iterator
 *
 * Returns: whether @iter is the begin iterator
 *
 * Since: 2.14
 */
gboolean
g_sequence_iter_is_begin (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, FALSE);

  return (node_get_prev (iter) == iter);
}

/**
 * g_sequence_iter_get_position:
 * @iter: a #GSequenceIter
 *
 * Returns the position of @iter
 *
 * Returns: the position of @iter
 *
 * Since: 2.14
 */
gint
g_sequence_iter_get_position (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, -1);

  return node_get_pos (iter);
}

/**
 * g_sequence_iter_next:
 * @iter: a #GSequenceIter
 *
 * Returns an iterator pointing to the next position after @iter.
 * If @iter is the end iterator, the end iterator is returned.
 *
 * Returns: (transfer none): a #GSequenceIter pointing to the next position after @iter
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_iter_next (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, NULL);

  return node_get_next (iter);
}

/**
 * g_sequence_iter_prev:
 * @iter: a #GSequenceIter
 *
 * Returns an iterator pointing to the previous position before @iter.
 * If @iter is the begin iterator, the begin iterator is returned.
 *
 * Returns: (transfer none): a #GSequenceIter pointing to the previous position
 *     before @iter
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_iter_prev (GSequenceIter *iter)
{
  g_return_val_if_fail (iter != NULL, NULL);

  return node_get_prev (iter);
}

/**
 * g_sequence_iter_move:
 * @iter: a #GSequenceIter
 * @delta: A positive or negative number indicating how many positions away
 *    from @iter the returned #GSequenceIter will be
 *
 * Returns the #GSequenceIter which is @delta positions away from @iter.
 * If @iter is closer than -@delta positions to the beginning of the sequence,
 * the begin iterator is returned. If @iter is closer than @delta positions
 * to the end of the sequence, the end iterator is returned.
 *
 * Returns: (transfer none): a #GSequenceIter which is @delta positions away from @iter
 *
 * Since: 2.14
 */
GSequenceIter *
g_sequence_iter_move (GSequenceIter *iter,
                      gint           delta)
{
  gint new_pos;
  gint len;

  g_return_val_if_fail (iter != NULL, NULL);

  len = g_sequence_get_length (get_sequence (iter));

  new_pos = node_get_pos (iter) + delta;

  if (new_pos < 0)
    new_pos = 0;
  else if (new_pos > len)
    new_pos = len;

  return node_get_by_pos (iter, new_pos);
}

/**
 * g_sequence_swap:
 * @a: a #GSequenceIter
 * @b: a #GSequenceIter
 *
 * Swaps the items pointed to by @a and @b. It is allowed for @a and @b
 * to point into difference sequences.
 *
 * Since: 2.14
 */
void
g_sequence_swap (GSequenceIter *a,
                 GSequenceIter *b)
{
  GSequenceNode *leftmost, *rightmost, *rightmost_next;
  int a_pos, b_pos;

  g_return_if_fail (!g_sequence_iter_is_end (a));
  g_return_if_fail (!g_sequence_iter_is_end (b));

  if (a == b)
    return;

  a_pos = g_sequence_iter_get_position (a);
  b_pos = g_sequence_iter_get_position (b);

  if (a_pos > b_pos)
    {
      leftmost = b;
      rightmost = a;
    }
  else
    {
      leftmost = a;
      rightmost = b;
    }

  rightmost_next = node_get_next (rightmost);

  /* The situation is now like this:
   *
   *     ..., leftmost, ......., rightmost, rightmost_next, ...
   *
   */
  g_sequence_move (rightmost, leftmost);
  g_sequence_move (leftmost, rightmost_next);
}

/*
 * Implementation of a treap
 *
 *
 */
static guint
get_priority (GSequenceNode *node)
{
  guint key = GPOINTER_TO_UINT (node);

  /* This hash function is based on one found on Thomas Wang's
   * web page at
   *
   *    http://www.concentric.net/~Ttwang/tech/inthash.htm
   *
   */
  key = (key << 15) - key - 1;
  key = key ^ (key >> 12);
  key = key + (key << 2);
  key = key ^ (key >> 4);
  key = key + (key << 3) + (key << 11);
  key = key ^ (key >> 16);

  /* We rely on 0 being less than all other priorities */
  return key? key : 1;
}

static GSequenceNode *
find_root (GSequenceNode *node)
{
  while (node->parent)
    node = node->parent;

  return node;
}

static GSequenceNode *
node_new (gpointer data)
{
  GSequenceNode *node = g_slice_new0 (GSequenceNode);

  node->n_nodes = 1;
  node->data = data;
  node->left = NULL;
  node->right = NULL;
  node->parent = NULL;

  return node;
}

static GSequenceNode *
node_get_first (GSequenceNode *node)
{
  node = find_root (node);

  while (node->left)
    node = node->left;

  return node;
}

static GSequenceNode *
node_get_last (GSequenceNode *node)
{
  node = find_root (node);

  while (node->right)
    node = node->right;

  return node;
}

#define NODE_LEFT_CHILD(n)  (((n)->parent) && ((n)->parent->left) == (n))
#define NODE_RIGHT_CHILD(n) (((n)->parent) && ((n)->parent->right) == (n))

static GSequenceNode *
node_get_next (GSequenceNode *node)
{
  GSequenceNode *n = node;

  if (n->right)
    {
      n = n->right;
      while (n->left)
        n = n->left;
    }
  else
    {
      while (NODE_RIGHT_CHILD (n))
        n = n->parent;

      if (n->parent)
        n = n->parent;
      else
        n = node;
    }

  return n;
}

static GSequenceNode *
node_get_prev (GSequenceNode *node)
{
  GSequenceNode *n = node;

  if (n->left)
    {
      n = n->left;
      while (n->right)
        n = n->right;
    }
  else
    {
      while (NODE_LEFT_CHILD (n))
        n = n->parent;

      if (n->parent)
        n = n->parent;
      else
        n = node;
    }

  return n;
}

#define N_NODES(n) ((n)? (n)->n_nodes : 0)

static gint
node_get_pos (GSequenceNode *node)
{
  int n_smaller = 0;

  if (node->left)
    n_smaller = node->left->n_nodes;

  while (node)
    {
      if (NODE_RIGHT_CHILD (node))
        n_smaller += N_NODES (node->parent->left) + 1;

      node = node->parent;
    }

  return n_smaller;
}

static GSequenceNode *
node_get_by_pos (GSequenceNode *node,
                 gint           pos)
{
  int i;

  node = find_root (node);

  while ((i = N_NODES (node->left)) != pos)
    {
      if (i < pos)
        {
          node = node->right;
          pos -= (i + 1);
        }
      else
        {
          node = node->left;
        }
    }

  return node;
}

static GSequenceNode *
node_find (GSequenceNode            *haystack,
           GSequenceNode            *needle,
           GSequenceNode            *end,
           GSequenceIterCompareFunc  iter_cmp,
           gpointer                  cmp_data)
{
  gint c;

  haystack = find_root (haystack);

  do
    {
      /* iter_cmp can't be passed the end node, since the function may
       * be user-supplied
       */
      if (haystack == end)
        c = 1;
      else
        c = iter_cmp (haystack, needle, cmp_data);

      if (c == 0)
        break;

      if (c > 0)
        haystack = haystack->left;
      else
        haystack = haystack->right;
    }
  while (haystack != NULL);

  return haystack;
}

static GSequenceNode *
node_find_closest (GSequenceNode            *haystack,
                   GSequenceNode            *needle,
                   GSequenceNode            *end,
                   GSequenceIterCompareFunc  iter_cmp,
                   gpointer                  cmp_data)
{
  GSequenceNode *best;
  gint c;

  haystack = find_root (haystack);

  do
    {
      best = haystack;

      /* iter_cmp can't be passed the end node, since the function may
       * be user-supplied
       */
      if (haystack == end)
        c = 1;
      else
        c = iter_cmp (haystack, needle, cmp_data);

      /* In the following we don't break even if c == 0. Instead we go on
       * searching along the 'bigger' nodes, so that we find the last one
       * that is equal to the needle.
       */
      if (c > 0)
        haystack = haystack->left;
      else
        haystack = haystack->right;
    }
  while (haystack != NULL);

  /* If the best node is smaller or equal to the data, then move one step
   * to the right to make sure the best one is strictly bigger than the data
   */
  if (best != end && c <= 0)
    best = node_get_next (best);

  return best;
}

static gint
node_get_length    (GSequenceNode            *node)
{
  node = find_root (node);

  return node->n_nodes;
}

static void
real_node_free (GSequenceNode *node,
                GSequence     *seq)
{
  if (node)
    {
      real_node_free (node->left, seq);
      real_node_free (node->right, seq);

      if (seq && seq->data_destroy_notify && node != seq->end_node)
        seq->data_destroy_notify (node->data);

      g_slice_free (GSequenceNode, node);
    }
}

static void
node_free (GSequenceNode *node,
           GSequence *seq)
{
  node = find_root (node);

  real_node_free (node, seq);
}

static void
node_update_fields (GSequenceNode *node)
{
  int n_nodes = 1;

  n_nodes += N_NODES (node->left);
  n_nodes += N_NODES (node->right);

  node->n_nodes = n_nodes;
}

static void
node_rotate (GSequenceNode *node)
{
  GSequenceNode *tmp, *old;

  g_assert (node->parent);
  g_assert (node->parent != node);

  if (NODE_LEFT_CHILD (node))
    {
      /* rotate right */
      tmp = node->right;

      node->right = node->parent;
      node->parent = node->parent->parent;
      if (node->parent)
        {
          if (node->parent->left == node->right)
            node->parent->left = node;
          else
            node->parent->right = node;
        }

      g_assert (node->right);

      node->right->parent = node;
      node->right->left = tmp;

      if (node->right->left)
        node->right->left->parent = node->right;

      old = node->right;
    }
  else
    {
      /* rotate left */
      tmp = node->left;

      node->left = node->parent;
      node->parent = node->parent->parent;
      if (node->parent)
        {
          if (node->parent->right == node->left)
            node->parent->right = node;
          else
            node->parent->left = node;
        }

      g_assert (node->left);

      node->left->parent = node;
      node->left->right = tmp;

      if (node->left->right)
        node->left->right->parent = node->left;

      old = node->left;
    }

  node_update_fields (old);
  node_update_fields (node);
}

static void
node_update_fields_deep (GSequenceNode *node)
{
  if (node)
    {
      node_update_fields (node);

      node_update_fields_deep (node->parent);
    }
}

static void
rotate_down (GSequenceNode *node,
             guint          priority)
{
  guint left, right;

  left = node->left ? get_priority (node->left)  : 0;
  right = node->right ? get_priority (node->right) : 0;

  while (priority < left || priority < right)
    {
      if (left > right)
        node_rotate (node->left);
      else
        node_rotate (node->right);

      left = node->left ? get_priority (node->left)  : 0;
      right = node->right ? get_priority (node->right) : 0;
    }
}

static void
node_cut (GSequenceNode *node)
{
  while (node->parent)
    node_rotate (node);

  if (node->left)
    node->left->parent = NULL;

  node->left = NULL;
  node_update_fields (node);

  rotate_down (node, get_priority (node));
}

static void
node_join (GSequenceNode *left,
           GSequenceNode *right)
{
  GSequenceNode *fake = node_new (NULL);

  fake->left = find_root (left);
  fake->right = find_root (right);
  fake->left->parent = fake;
  fake->right->parent = fake;

  node_update_fields (fake);

  node_unlink (fake);

  node_free (fake, NULL);
}

static void
node_insert_before (GSequenceNode *node,
                    GSequenceNode *new)
{
  new->left = node->left;
  if (new->left)
    new->left->parent = new;

  new->parent = node;
  node->left = new;

  node_update_fields_deep (new);

  while (new->parent && get_priority (new) > get_priority (new->parent))
    node_rotate (new);

  rotate_down (new, get_priority (new));
}

static void
node_unlink (GSequenceNode *node)
{
  rotate_down (node, 0);

  if (NODE_RIGHT_CHILD (node))
    node->parent->right = NULL;
  else if (NODE_LEFT_CHILD (node))
    node->parent->left = NULL;

  if (node->parent)
    node_update_fields_deep (node->parent);

  node->parent = NULL;
}

static void
node_insert_sorted (GSequenceNode            *node,
                    GSequenceNode            *new,
                    GSequenceNode            *end,
                    GSequenceIterCompareFunc  iter_cmp,
                    gpointer                  cmp_data)
{
  GSequenceNode *closest;

  closest = node_find_closest (node, new, end, iter_cmp, cmp_data);

  node_unlink (new);

  node_insert_before (closest, new);
}
