/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
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
 * SECTION:gstfilter
 * @short_description: A utility function to filter GLists.
 *
 * <example>
 * <title>Filtering a list</title>
 *   <programlisting>
 *     GList *node;
 *     GstObject *result = NULL;
 *     
 *     node = gst_filter_run (list, (GstFilterFunc) my_filter, TRUE, NULL);
 *     if (node) {
 *       result = GST_OBJECT (node->data);
 *       gst_object_ref (result);
 *       g_list_free (node);
 *     }
 *   </programlisting>
 * </example>
 */
#include "gst_private.h"
#include <gst/gstfilter.h>

/**
 * gst_filter_run:
 * @list: a linked list
 * @func: (scope call): the function to execute for each item
 * @first: flag to stop execution after a successful item
 * @user_data: (closure): user data
 *
 * Iterates over the elements in @list, calling @func with the
 * list item data for each item.  If @func returns TRUE, @data is
 * prepended to the list of results returned.  If @first is true,
 * the search is halted after the first result is found.
 *
 * Since gst_filter_run() knows nothing about the type of @data, no
 * reference will be taken (if @data refers to an object) and no copy of
 * @data wil be made in any other way when prepending @data to the list of
 * results.
 *
 * Returns: (transfer container): the list of results. Free with g_list_free()
 *     when no longer needed (the data contained in the list is a flat copy
 *     and does need to be unreferenced or freed).
 */
GList *
gst_filter_run (const GList * list, GstFilterFunc func, gboolean first,
    gpointer user_data)
{
  const GList *walk = list;
  GList *result = NULL;

  while (walk) {
    gboolean res = TRUE;
    gpointer data = walk->data;

    walk = g_list_next (walk);

    if (func)
      res = func (data, user_data);

    if (res) {
      result = g_list_prepend (result, data);

      if (first)
        break;
    }
  }

  return result;
}
