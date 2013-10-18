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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstcontrolsource
 * @short_description: base class for control source sources
 *
 * The #GstControlSource is a base class for control value sources that could
 * be used by #GstController to get timestamp-value pairs.
 *
 * A #GstControlSource is used by first getting an instance, binding it to a
 * #GParamSpec (for example by using gst_controller_set_control_source()) and
 * then by having it used by the #GstController or calling
 * gst_control_source_get_value() or gst_control_source_get_value_array().
 *
 * For implementing a new #GstControlSource one has to implement a
 * #GstControlSourceBind method, which will depending on the #GParamSpec set up
 * the control source for use and sets the #GstControlSourceGetValue and
 * #GstControlSourceGetValueArray functions. These are then used by
 * gst_control_source_get_value() or gst_control_source_get_value_array()
 * to get values for specific timestamps.
 *
 */

#include <glib-object.h>
#include <gst/gst.h>

#include "gstcontrolsource.h"

#define GST_CAT_DEFAULT controller_debug
GST_DEBUG_CATEGORY_EXTERN (GST_CAT_DEFAULT);

G_DEFINE_ABSTRACT_TYPE (GstControlSource, gst_control_source, G_TYPE_OBJECT);

static GObjectClass *parent_class = NULL;

static void
gst_control_source_class_init (GstControlSourceClass * klass)
{
  //GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  parent_class = g_type_class_peek_parent (klass);

  /* Has to be implemented by children */
  klass->bind = NULL;
}

static void
gst_control_source_init (GstControlSource * self)
{
  /* Set default handlers that print a warning */
  self->get_value = NULL;
  self->get_value_array = NULL;
  self->bound = FALSE;
}

/**
 * gst_control_source_get_value:
 * @self: the #GstControlSource object
 * @timestamp: the time for which the value should be returned
 * @value: the value
 *
 * Gets the value for this #GstControlSource at a given timestamp.
 *
 * Returns: FALSE if the value couldn't be returned, TRUE otherwise.
 */
gboolean
gst_control_source_get_value (GstControlSource * self, GstClockTime timestamp,
    GValue * value)
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
 * gst_control_source_get_value_array:
 * @self: the #GstControlSource object
 * @timestamp: the time that should be processed
 * @value_array: array to put control-values in
 *
 * Gets an array of values for one element property.
 *
 * All fields of @value_array must be filled correctly. Especially the
 * @value_array->values array must be big enough to keep the requested amount
 * of values.
 *
 * The type of the values in the array is the same as the property's type.
 *
 * Returns: %TRUE if the given array could be filled, %FALSE otherwise
 */
gboolean
gst_control_source_get_value_array (GstControlSource * self,
    GstClockTime timestamp, GstValueArray * value_array)
{
  g_return_val_if_fail (GST_IS_CONTROL_SOURCE (self), FALSE);

  if (G_LIKELY (self->get_value_array)) {
    return self->get_value_array (self, timestamp, value_array);
  } else {
    GST_ERROR ("Not bound to a specific property yet!");
    return FALSE;
  }
}

/**
 * gst_control_source_bind:
 * @self: the #GstControlSource object
 * @pspec: #GParamSpec for the property for which this #GstControlSource should generate values.
 *
 * Binds a #GstControlSource to a specific property. This must be called only once for a
 * #GstControlSource.
 *
 * Returns: %TRUE if the #GstControlSource was bound correctly, %FALSE otherwise.
 */
gboolean
gst_control_source_bind (GstControlSource * self, GParamSpec * pspec)
{
  gboolean ret = FALSE;

  g_return_val_if_fail (GST_IS_CONTROL_SOURCE (self), FALSE);
  g_return_val_if_fail (GST_CONTROL_SOURCE_GET_CLASS (self)->bind, FALSE);
  g_return_val_if_fail (!self->bound, FALSE);

  ret = GST_CONTROL_SOURCE_GET_CLASS (self)->bind (self, pspec);

  if (ret)
    self->bound = TRUE;

  return ret;
}
