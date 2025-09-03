/* GStreamer
 * Copyright (C) 2009-2010 Edward Hervey <bilboed@bilboed.com>
 *
 * gstvecdeque.h:
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

#include <glib.h>

#ifndef __GST_VEC_DEQUE_H__
#define __GST_VEC_DEQUE_H__

#include <glib.h>
#include <gst/gstconfig.h>

G_BEGIN_DECLS

/**
 * GstVecDeque: (skip)
 *
 * Since: 1.26
 */
typedef struct _GstVecDeque GstVecDeque;

GST_API
GstVecDeque *   gst_vec_deque_new     (gsize initial_size);

GST_API
void            gst_vec_deque_free    (GstVecDeque * array);

GST_API
void            gst_vec_deque_set_clear_func (GstVecDeque *array,
                                              GDestroyNotify clear_func);

GST_API
void            gst_vec_deque_clear     (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_pop_head  (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_head (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_nth  (GstVecDeque * array, gsize idx);

GST_API
gpointer        gst_vec_deque_pop_tail  (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_tail (GstVecDeque * array);

GST_API
void            gst_vec_deque_push_tail (GstVecDeque * array,
                                         gpointer        data);
GST_API
gboolean        gst_vec_deque_is_empty  (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_drop_element (GstVecDeque * array,
                                            gsize           idx);
GST_API
gsize           gst_vec_deque_find (GstVecDeque * array,
                                    GCompareFunc    func,
                                    gpointer        data);
GST_API
gsize           gst_vec_deque_get_length (GstVecDeque * array);

/* Functions for use with structures */

GST_API
GstVecDeque * gst_vec_deque_new_for_struct (gsize struct_size,
                                            gsize initial_size);
GST_API
void            gst_vec_deque_push_tail_struct (GstVecDeque * array,
                                                  gpointer        p_struct);
GST_API
gpointer        gst_vec_deque_pop_head_struct  (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_head_struct (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_nth_struct  (GstVecDeque * array, gsize idx);

GST_API
gboolean        gst_vec_deque_drop_struct      (GstVecDeque * array,
                                                gsize           idx,
                                                gpointer        p_struct);
GST_API
gpointer        gst_vec_deque_pop_tail_struct  (GstVecDeque * array);

GST_API
gpointer        gst_vec_deque_peek_tail_struct (GstVecDeque * array);

GST_API
void            gst_vec_deque_push_sorted (GstVecDeque * array, 
                                           gpointer data,
                                           GCompareDataFunc func, 
                                           gpointer user_data);

GST_API
void            gst_vec_deque_push_sorted_struct (GstVecDeque * array, 
                                                  gpointer p_struct,
                                                  GCompareDataFunc func, 
                                                  gpointer user_data);

GST_API
void            gst_vec_deque_sort (GstVecDeque *array,
                                    GCompareDataFunc compare_func,
                                    gpointer user_data);

G_END_DECLS

#endif
