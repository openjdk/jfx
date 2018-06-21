/* GStreamer
 *
 * Copyright (C) 2007 Sebastian Dröge <slomo@circular-chaos.org>
 *
 * gstcontrolsource.c: Interface declaration for control sources
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
 * SECTION:gstcontrolsource
 * @title: GstControlSource
 * @short_description: base class for control source sources
 *
 * The #GstControlSource is a base class for control value sources that could
 * be used to get timestamp-value pairs. A control source essentially is a
 * function over time.
 *
 * A #GstControlSource is used by first getting an instance of a specific
 * control-source, creating a binding for the control-source to the target property
 * of the element and then adding the binding to the element. The binding will
 * convert the data types and value range to fit to the bound property.
 *
 * For implementing a new #GstControlSource one has to implement
 * #GstControlSourceGetValue and #GstControlSourceGetValueArray functions.
 * These are then used by gst_control_source_get_value() and
 * gst_control_source_get_value_array() to get values for specific timestamps.
 */

#include "gst_private.h"

#include <glib-object.h>
#include <gst/gst.h>

#include "gstcontrolsource.h"

#define GST_CAT_DEFAULT control_source_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define _do_init \
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "gstcontrolsource", 0, \
      "dynamic parameter control sources");

G_DEFINE_ABSTRACT_TYPE_WITH_CODE (GstControlSource, gst_control_source,
    GST_TYPE_OBJECT, _do_init);

static void
gst_control_source_class_init (GstControlSourceClass * klass)
{
}

static void
gst_control_source_init (GstControlSource * self)
{
  self->get_value = NULL;
  self->get_value_array = NULL;
}

/**
 * gst_control_source_get_value: (method)
 * @self: the #GstControlSource object
 * @timestamp: the time for which the value should be returned
 * @value: (out): the value
 *
 * Gets the value for this #GstControlSource at a given timestamp.
 *
 * Returns: %FALSE if the value couldn't be returned, %TRUE otherwise.
 */
gboolean
gst_control_source_get_value (GstControlSource * self, GstClockTime timestamp,
    gdouble * value)
{
  g_return_val_if_fail (GST_IS_CONTROL_SOURCE (self), FALSE);

  if (G_LIKELY (self->get_value)) {
    return self->get_value (self, timestamp, value);
  } else {
    GST_ERROR ("Not bound to a specific property yet!");
    return FALSE;
  }
}

/**
 * gst_control_source_get_value_array: (method)
 * @self: the #GstControlSource object
 * @timestamp: the first timestamp
 * @interval: the time steps
 * @n_values: the number of values to fetch
 * @values: (array length=n_values): array to put control-values in
 *
 * Gets an array of values for for this #GstControlSource. Values that are
 * undefined contain NANs.
 *
 * Returns: %TRUE if the given array could be filled, %FALSE otherwise
 */
gboolean
gst_control_source_get_value_array (GstControlSource * self,
    GstClockTime timestamp, GstClockTime interval, guint n_values,
    gdouble * values)
{
  g_return_val_if_fail (GST_IS_CONTROL_SOURCE (self), FALSE);

  if (G_LIKELY (self->get_value_array)) {
    return self->get_value_array (self, timestamp, interval, n_values, values);
  } else {
    GST_ERROR ("Not bound to a specific property yet!");
    return FALSE;
  }
}
