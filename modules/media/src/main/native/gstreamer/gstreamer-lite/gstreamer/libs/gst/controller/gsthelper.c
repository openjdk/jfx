/* GStreamer
 *
 * Copyright (C) <2005> Stefan Kost <ensonic at users dot sf dot net>
 *
 * gsthelper.c: GObject convenience methods for using dynamic properties
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
 * SECTION:gstcontrollergobject
 * @short_description: #GObject convenience methods for using dynamic properties
 * @see_also: #GstController
 *
 * These methods allow to use some #GstController functionallity directly from
 * the #GObject class.
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <stdarg.h>

#include "gstcontrollerprivate.h"
#include "gstcontroller.h"

#define GST_CAT_DEFAULT controller_debug
GST_DEBUG_CATEGORY_EXTERN (GST_CAT_DEFAULT);

/**
 * gst_object_control_properties:
 * @object: the object of which some properties should be controlled
 * @...: %NULL terminated list of property names that should be controlled
 *
 * Convenience function for GObject
 *
 * Creates a GstController that allows you to dynamically control one, or more, GObject properties.
 * If the given GObject already has a GstController, it adds the given properties to the existing
 * controller and returns that controller.
 *
 * Returns: The GstController with which the user can control the given properties dynamically or NULL if
 * one or more of the given properties aren't available, or cannot be controlled, for the given element.
 * Since: 0.9
 */
GstController *
gst_object_control_properties (GObject * object, ...)
{
  GstController *ctrl;
  va_list var_args;

  g_return_val_if_fail (G_IS_OBJECT (object), NULL);

  va_start (var_args, object);
  ctrl = gst_controller_new_valist (object, var_args);
  va_end (var_args);
  return (ctrl);
}

/**
 * gst_object_uncontrol_properties:
 * @object: the object of which some properties should not be controlled anymore
 * @...: %NULL terminated list of property names that should be controlled
 *
 * Convenience function for GObject
 *
 * Removes the given element's properties from it's controller
 *
 * Returns: %FALSE if one of the given property names isn't handled by the
 * controller, %TRUE otherwise
 * Since: 0.9
 */
gboolean
gst_object_uncontrol_properties (GObject * object, ...)
{
  gboolean res = FALSE;
  GstController *ctrl;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    va_list var_args;

    va_start (var_args, object);
    res = gst_controller_remove_properties_valist (ctrl, var_args);
    va_end (var_args);
  }
  return (res);
}

/**
 * gst_object_get_controller:
 * @object: the object that has controlled properties
 *
 * Gets the controller for the given GObject
 * 
 * Returns: the controller handling some of the given element's properties, %NULL if no controller
 * Since: 0.9
 */
/* FIXME 0.11: This should return a new reference */
GstController *
gst_object_get_controller (GObject * object)
{
  g_return_val_if_fail (G_IS_OBJECT (object), NULL);

  return (g_object_get_qdata (object, priv_gst_controller_key));
}

/**
 * gst_object_set_controller:
 * @object: the object that should get the controller
 * @controller: the controller object to plug in
 *
 * Sets the controller on the given GObject
 *
 * Returns: %FALSE if the GObject already has an controller, %TRUE otherwise
 * Since: 0.9
 */
/* FIXME 0.11: This should increase the refcount before storing */
gboolean
gst_object_set_controller (GObject * object, GstController * controller)
{
  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);
  g_return_val_if_fail (controller, FALSE);

  if (!g_object_get_qdata (object, priv_gst_controller_key)) {
    g_object_set_qdata (object, priv_gst_controller_key, controller);
    return (TRUE);
  }
  return (FALSE);
}

 /**
 * gst_object_suggest_next_sync:
 * @object: the object that has controlled properties
 *
 * Convenience function for GObject
 *
 * Returns: same thing as gst_controller_suggest_next_sync()
 * Since: 0.10.13
 */
GstClockTime
gst_object_suggest_next_sync (GObject * object)
{
  GstController *ctrl = NULL;

  g_return_val_if_fail (G_IS_OBJECT (object), GST_CLOCK_TIME_NONE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_suggest_next_sync (ctrl);
  }
  return (GST_CLOCK_TIME_NONE);
}

/**
 * gst_object_sync_values:
 * @object: the object that has controlled properties
 * @timestamp: the time that should be processed
 *
 * Convenience function for GObject
 *
 * Returns: same thing as gst_controller_sync_values()
 * Since: 0.9
 */
gboolean
gst_object_sync_values (GObject * object, GstClockTime timestamp)
{
  GstController *ctrl = NULL;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_sync_values (ctrl, timestamp);
  }
  /* this is no failure, its called by elements regardless if there is a
   * controller assigned or not
   */
  return (TRUE);
}

/**
 * gst_object_set_control_source:
 * @object: the controller object
 * @property_name: name of the property for which the #GstControlSource should be set
 * @csource: the #GstControlSource that should be used for the property
 *
 * Sets the #GstControlSource for @property_name. If there already was a #GstControlSource
 * for this property it will be unreferenced.
 *
 * Returns: %FALSE if the given property isn't handled by the controller or the new #GstControlSource
 * couldn't be bound to the property, %TRUE if everything worked as expected.
 *
 * Since: 0.10.14
 */
gboolean
gst_object_set_control_source (GObject * object, const gchar * property_name,
    GstControlSource * csource)
{
  GstController *ctrl = NULL;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);
  g_return_val_if_fail (GST_IS_CONTROL_SOURCE (csource), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_set_control_source (ctrl, property_name, csource);
  }
  return FALSE;
}

/**
 * gst_object_get_control_source:
 * @object: the object
 * @property_name: name of the property for which the #GstControlSource should be get
 *
 * Gets the corresponding #GstControlSource for the property. This should be unreferenced
 * again after use.
 *
 * Returns: the #GstControlSource for @property_name or NULL if the property is not
 * controlled by this controller or no #GstControlSource was assigned yet.
 *
 * Since: 0.10.14
 */
GstControlSource *
gst_object_get_control_source (GObject * object, const gchar * property_name)
{
  GstController *ctrl = NULL;

  g_return_val_if_fail (G_IS_OBJECT (object), NULL);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_get_control_source (ctrl, property_name);
  }
  return NULL;
}

/**
 * gst_object_get_value_arrays:
 * @object: the object that has controlled properties
 * @timestamp: the time that should be processed
 * @value_arrays: list to return the control-values in
 *
 * Function to be able to get an array of values for one or more given element
 * properties.
 *
 * If the GstValueArray->values array in list nodes is NULL, it will be created
 * by the function.
 * The type of the values in the array are the same as the property's type.
 *
 * The g_object_* functions are just convenience functions for GObject
 *
 * Returns: %TRUE if the given array(s) could be filled, %FALSE otherwise
 * Since: 0.9
 */
gboolean
gst_object_get_value_arrays (GObject * object, GstClockTime timestamp,
    GSList * value_arrays)
{
  GstController *ctrl;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (timestamp), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_get_value_arrays (ctrl, timestamp, value_arrays);
  }
  return (FALSE);
}

/**
 * gst_object_get_value_array:
 * @object: the object that has controlled properties
 * @timestamp: the time that should be processed
 * @value_array: array to put control-values in
 *
 * Function to be able to get an array of values for one element properties
 *
 * If the GstValueArray->values array is NULL, it will be created by the function.
 * The type of the values in the array are the same as the property's type.
 *
 * The g_object_* functions are just convenience functions for GObject
 *
 * Returns: %TRUE if the given array(s) could be filled, %FALSE otherwise
 * Since: 0.9
 */
gboolean
gst_object_get_value_array (GObject * object, GstClockTime timestamp,
    GstValueArray * value_array)
{
  GstController *ctrl;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (timestamp), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    return gst_controller_get_value_array (ctrl, timestamp, value_array);
  }
  return (FALSE);
}

/**
 * gst_object_get_control_rate:
 * @object: the object that has controlled properties
 *
 * Obtain the control-rate for this @object. Audio processing #GstElement
 * objects will use this rate to sub-divide their processing loop and call
 * gst_object_sync_values() inbetween. The length of the processing segment
 * should be up to @control-rate nanoseconds.
 *
 * If the @object is not under property control, this will return
 * %GST_CLOCK_TIME_NONE. This allows the element to avoid the sub-dividing.
 *
 * The control-rate is not expected to change if the element is in
 * %GST_STATE_PAUSED or %GST_STATE_PLAYING.
 *
 * Returns: the control rate in nanoseconds
 * Since: 0.10.10
 */
GstClockTime
gst_object_get_control_rate (GObject * object)
{
  GstController *ctrl;
  GstClockTime control_rate = GST_CLOCK_TIME_NONE;

  g_return_val_if_fail (G_IS_OBJECT (object), FALSE);

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    g_object_get (ctrl, "control-rate", &control_rate, NULL);
  }
  return (control_rate);
}

/**
 * gst_object_set_control_rate:
 * @object: the object that has controlled properties
 * @control_rate: the new control-rate in nanoseconds.
 *
 * Change the control-rate for this @object. Audio processing #GstElement
 * objects will use this rate to sub-divide their processing loop and call
 * gst_object_sync_values() inbetween. The length of the processing segment
 * should be up to @control-rate nanoseconds.
 *
 * The control-rate should not change if the element is in %GST_STATE_PAUSED or
 * %GST_STATE_PLAYING.
 *
 * Since: 0.10.10
 */
void
gst_object_set_control_rate (GObject * object, GstClockTime control_rate)
{
  GstController *ctrl;

  g_return_if_fail (G_IS_OBJECT (object));

  if ((ctrl = g_object_get_qdata (object, priv_gst_controller_key))) {
    g_object_set (ctrl, "control-rate", control_rate, NULL);
  }
}
