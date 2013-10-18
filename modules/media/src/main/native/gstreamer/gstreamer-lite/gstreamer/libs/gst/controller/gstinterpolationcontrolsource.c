/* GStreamer
 *
 * Copyright (C) 2007,2009 Sebastian Dröge <sebastian.droege@collabora.co.uk>
 *
 * gstinterpolationcontrolsource.c: Control source that provides several
 *                                  interpolation methods
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
 * SECTION:gstinterpolationcontrolsource
 * @short_description: interpolation control source
 *
 * #GstInterpolationControlSource is a #GstControlSource, that interpolates values between user-given
 * control points. It supports several interpolation modes and property types.
 *
 * To use #GstInterpolationControlSource get a new instance by calling
 * gst_interpolation_control_source_new(), bind it to a #GParamSpec, select a interpolation mode with
 * gst_interpolation_control_source_set_interpolation_mode() and set some control points by calling
 * gst_interpolation_control_source_set().
 *
 * All functions are MT-safe.
 *
 */

#include <glib-object.h>
#include <gst/gst.h>

#include "gstcontrolsource.h"
#include "gstinterpolationcontrolsource.h"
#include "gstinterpolationcontrolsourceprivate.h"

#define GST_CAT_DEFAULT controller_debug
GST_DEBUG_CATEGORY_EXTERN (GST_CAT_DEFAULT);

G_DEFINE_TYPE (GstInterpolationControlSource, gst_interpolation_control_source,
    GST_TYPE_CONTROL_SOURCE);

static GObjectClass *parent_class = NULL;

/*
 * gst_control_point_free:
 * @prop: the object to free
 *
 * Private method which frees all data allocated by a #GstControlPoint
 * instance.
 */
static void
gst_control_point_free (GstControlPoint * cp)
{
  g_return_if_fail (cp);

  g_value_unset (&cp->value);
  g_slice_free (GstControlPoint, cp);
}

static void
gst_interpolation_control_source_reset (GstInterpolationControlSource * self)
{
  GstControlSource *csource = (GstControlSource *) self;

  csource->get_value = NULL;
  csource->get_value_array = NULL;

  self->priv->type = self->priv->base = G_TYPE_INVALID;

  if (G_IS_VALUE (&self->priv->default_value))
    g_value_unset (&self->priv->default_value);
  if (G_IS_VALUE (&self->priv->minimum_value))
    g_value_unset (&self->priv->minimum_value);
  if (G_IS_VALUE (&self->priv->maximum_value))
    g_value_unset (&self->priv->maximum_value);

  if (self->priv->values) {
    g_sequence_free (self->priv->values);
    self->priv->values = NULL;
  }

  self->priv->nvalues = 0;
  self->priv->valid_cache = FALSE;
}

/**
 * gst_interpolation_control_source_new:
 *
 * This returns a new, unbound #GstInterpolationControlSource.
 *
 * Returns: a new, unbound #GstInterpolationControlSource.
 */
GstInterpolationControlSource *
gst_interpolation_control_source_new (void)
{
  return g_object_newv (GST_TYPE_INTERPOLATION_CONTROL_SOURCE, 0, NULL);
}

/**
 * gst_interpolation_control_source_set_interpolation_mode:
 * @self: the #GstInterpolationControlSource object
 * @mode: interpolation mode
 *
 * Sets the given interpolation mode.
 *
 * <note><para>User interpolation is not yet available and quadratic interpolation
 * is deprecated and maps to cubic interpolation.</para></note>
 *
 * Returns: %TRUE if the interpolation mode could be set, %FALSE otherwise
 */
/* *INDENT-OFF* */
gboolean
gst_interpolation_control_source_set_interpolation_mode (
    GstInterpolationControlSource * self, GstInterpolateMode mode)
/* *INDENT-ON* */
{
  gboolean ret = TRUE;
  GstControlSource *csource = GST_CONTROL_SOURCE (self);

  if (mode >= priv_gst_num_interpolation_methods
      || priv_gst_interpolation_methods[mode] == NULL) {
    GST_WARNING ("interpolation mode %d invalid or not implemented yet", mode);
    return FALSE;
  }

  if (mode == GST_INTERPOLATE_QUADRATIC) {
    GST_WARNING ("Quadratic interpolation mode is deprecated, using cubic"
        "interpolation mode");
  }

  if (mode == GST_INTERPOLATE_USER) {
    GST_WARNING ("User interpolation mode is not implemented yet");
    return FALSE;
  }

  g_mutex_lock (self->lock);
  switch (self->priv->base) {
    case G_TYPE_INT:
      csource->get_value = priv_gst_interpolation_methods[mode]->get_int;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_int_value_array;
      break;
    case G_TYPE_UINT:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_uint;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_uint_value_array;
      break;
    }
    case G_TYPE_LONG:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_long;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_long_value_array;
      break;
    }
    case G_TYPE_ULONG:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_ulong;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_ulong_value_array;
      break;
    }
    case G_TYPE_INT64:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_int64;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_int64_value_array;
      break;
    }
    case G_TYPE_UINT64:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_uint64;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_uint64_value_array;
      break;
    }
    case G_TYPE_FLOAT:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_float;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_float_value_array;
      break;
    }
    case G_TYPE_DOUBLE:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_double;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_double_value_array;
      break;
    }
    case G_TYPE_BOOLEAN:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_boolean;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_boolean_value_array;
      break;
    }
    case G_TYPE_ENUM:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_enum;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_enum_value_array;
      break;
    }
    case G_TYPE_STRING:{
      csource->get_value = priv_gst_interpolation_methods[mode]->get_string;
      csource->get_value_array =
          priv_gst_interpolation_methods[mode]->get_string_value_array;
      break;
    }
    default:
      ret = FALSE;
      break;
  }

  /* Incomplete implementation */
  if (!ret || !csource->get_value || !csource->get_value_array) {
    gst_interpolation_control_source_reset (self);
    ret = FALSE;
  }

  self->priv->valid_cache = FALSE;
  self->priv->interpolation_mode = mode;

  g_mutex_unlock (self->lock);

  return ret;
}

static gboolean
gst_interpolation_control_source_bind (GstControlSource * source,
    GParamSpec * pspec)
{
  GType type, base;
  GstInterpolationControlSource *self =
      (GstInterpolationControlSource *) source;
  gboolean ret = TRUE;

  /* get the fundamental base type */
  self->priv->type = base = type = G_PARAM_SPEC_VALUE_TYPE (pspec);
  while ((type = g_type_parent (type)))
    base = type;

  self->priv->base = base;
  /* restore type */
  type = self->priv->type;

  if (!gst_interpolation_control_source_set_interpolation_mode (self,
          self->priv->interpolation_mode))
    return FALSE;

  switch (base) {
    case G_TYPE_INT:{
      GParamSpecInt *tpspec = G_PARAM_SPEC_INT (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_int (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_int (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_int (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_UINT:{
      GParamSpecUInt *tpspec = G_PARAM_SPEC_UINT (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_uint (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_uint (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_uint (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_LONG:{
      GParamSpecLong *tpspec = G_PARAM_SPEC_LONG (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_long (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_long (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_long (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_ULONG:{
      GParamSpecULong *tpspec = G_PARAM_SPEC_ULONG (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_ulong (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_ulong (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_ulong (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_INT64:{
      GParamSpecInt64 *tpspec = G_PARAM_SPEC_INT64 (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_int64 (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_int64 (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_int64 (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_UINT64:{
      GParamSpecUInt64 *tpspec = G_PARAM_SPEC_UINT64 (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_uint64 (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_uint64 (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_uint64 (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_FLOAT:{
      GParamSpecFloat *tpspec = G_PARAM_SPEC_FLOAT (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_float (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_float (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_float (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_DOUBLE:{
      GParamSpecDouble *tpspec = G_PARAM_SPEC_DOUBLE (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_double (&self->priv->default_value, tpspec->default_value);
      g_value_init (&self->priv->minimum_value, type);
      g_value_set_double (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_double (&self->priv->maximum_value, tpspec->maximum);
      break;
    }
    case G_TYPE_BOOLEAN:{
      GParamSpecBoolean *tpspec = G_PARAM_SPEC_BOOLEAN (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_boolean (&self->priv->default_value, tpspec->default_value);
      break;
    }
    case G_TYPE_ENUM:{
      GParamSpecEnum *tpspec = G_PARAM_SPEC_ENUM (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_enum (&self->priv->default_value, tpspec->default_value);
      break;
    }
    case G_TYPE_STRING:{
      GParamSpecString *tpspec = G_PARAM_SPEC_STRING (pspec);

      g_value_init (&self->priv->default_value, type);
      g_value_set_string (&self->priv->default_value, tpspec->default_value);
      break;
    }
    default:
      GST_WARNING ("incomplete implementation for paramspec type '%s'",
          G_PARAM_SPEC_TYPE_NAME (pspec));
      ret = FALSE;
      break;
  }

  if (ret) {
    self->priv->valid_cache = FALSE;
    self->priv->nvalues = 0;
  } else {
    gst_interpolation_control_source_reset (self);
  }

  return ret;
}

/*
 * gst_control_point_compare:
 * @p1: a pointer to a #GstControlPoint
 * @p2: a pointer to a #GstControlPoint
 *
 * Compare function for g_list operations that operates on two #GstControlPoint
 * parameters.
 */
static gint
gst_control_point_compare (gconstpointer p1, gconstpointer p2)
{
  GstClockTime ct1 = ((GstControlPoint *) p1)->timestamp;
  GstClockTime ct2 = ((GstControlPoint *) p2)->timestamp;

  return ((ct1 < ct2) ? -1 : ((ct1 == ct2) ? 0 : 1));
}

/*
 * gst_control_point_find:
 * @p1: a pointer to a #GstControlPoint
 * @p2: a pointer to a #GstClockTime
 *
 * Compare function for g_list operations that operates on a #GstControlPoint and
 * a #GstClockTime.
 */
static gint
gst_control_point_find (gconstpointer p1, gconstpointer p2)
{
  GstClockTime ct1 = ((GstControlPoint *) p1)->timestamp;
  GstClockTime ct2 = *(GstClockTime *) p2;

  return ((ct1 < ct2) ? -1 : ((ct1 == ct2) ? 0 : 1));
}

static GstControlPoint *
_make_new_cp (GstInterpolationControlSource * self, GstClockTime timestamp,
    const GValue * value)
{
  GstControlPoint *cp;

  /* create a new GstControlPoint */
  cp = g_slice_new0 (GstControlPoint);
  cp->timestamp = timestamp;
  g_value_init (&cp->value, self->priv->type);
  g_value_copy (value, &cp->value);

  return cp;
}

static void
gst_interpolation_control_source_set_internal (GstInterpolationControlSource *
    self, GstClockTime timestamp, const GValue * value)
{
  GSequenceIter *iter;

  /* check if a control point for the timestamp already exists */

  /* iter contains the iter right *after* timestamp */
  if (G_LIKELY (self->priv->values)) {
    iter =
        g_sequence_search (self->priv->values, &timestamp,
        (GCompareDataFunc) gst_control_point_find, NULL);
    if (iter) {
      GSequenceIter *prev = g_sequence_iter_prev (iter);
      GstControlPoint *cp = g_sequence_get (prev);

      /* If the timestamp is the same just update the control point value */
      if (cp->timestamp == timestamp) {
        /* update control point */
        g_value_reset (&cp->value);
        g_value_copy (value, &cp->value);
        goto done;
      }
    }
  } else {
    self->priv->values =
        g_sequence_new ((GDestroyNotify) gst_control_point_free);
  }

  /* sort new cp into the prop->values list */
  g_sequence_insert_sorted (self->priv->values, _make_new_cp (self, timestamp,
          value), (GCompareDataFunc) gst_control_point_compare, NULL);
  self->priv->nvalues++;

done:
  self->priv->valid_cache = FALSE;
}


/**
 * gst_interpolation_control_source_set:
 * @self: the #GstInterpolationControlSource object
 * @timestamp: the time the control-change is scheduled for
 * @value: the control-value
 *
 * Set the value of given controller-handled property at a certain time.
 *
 * Returns: FALSE if the values couldn't be set, TRUE otherwise.
 */
gboolean
gst_interpolation_control_source_set (GstInterpolationControlSource * self,
    GstClockTime timestamp, const GValue * value)
{
  g_return_val_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self), FALSE);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (timestamp), FALSE);
  g_return_val_if_fail (G_IS_VALUE (value), FALSE);
  g_return_val_if_fail (G_VALUE_TYPE (value) == self->priv->type, FALSE);

  g_mutex_lock (self->lock);
  gst_interpolation_control_source_set_internal (self, timestamp, value);
  g_mutex_unlock (self->lock);

  return TRUE;
}

/**
 * gst_interpolation_control_source_set_from_list:
 * @self: the #GstInterpolationControlSource object
 * @timedvalues: a list with #GstTimedValue items
 *
 * Sets multiple timed values at once.
 *
 * Returns: FALSE if the values couldn't be set, TRUE otherwise.
 */
gboolean
gst_interpolation_control_source_set_from_list (GstInterpolationControlSource *
    self, const GSList * timedvalues)
{
  const GSList *node;
  GstTimedValue *tv;
  gboolean res = FALSE;

  g_return_val_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self), FALSE);

  for (node = timedvalues; node; node = g_slist_next (node)) {
    tv = node->data;
    if (!GST_CLOCK_TIME_IS_VALID (tv->timestamp)) {
      GST_WARNING ("GstTimedValued with invalid timestamp passed to %s",
          GST_FUNCTION);
    } else if (!G_IS_VALUE (&tv->value)) {
      GST_WARNING ("GstTimedValued with invalid value passed to %s",
          GST_FUNCTION);
    } else if (G_VALUE_TYPE (&tv->value) != self->priv->type) {
      GST_WARNING ("incompatible value type for property");
    } else {
      g_mutex_lock (self->lock);
      gst_interpolation_control_source_set_internal (self, tv->timestamp,
          &tv->value);
      g_mutex_unlock (self->lock);
      res = TRUE;
    }
  }
  return res;
}

/**
 * gst_interpolation_control_source_unset:
 * @self: the #GstInterpolationControlSource object
 * @timestamp: the time the control-change should be removed from
 *
 * Used to remove the value of given controller-handled property at a certain
 * time.
 *
 * Returns: FALSE if the value couldn't be unset (i.e. not found, TRUE otherwise.
 */
gboolean
gst_interpolation_control_source_unset (GstInterpolationControlSource * self,
    GstClockTime timestamp)
{
  GSequenceIter *iter;
  gboolean res = FALSE;

  g_return_val_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self), FALSE);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (timestamp), FALSE);

  g_mutex_lock (self->lock);
  /* check if a control point for the timestamp exists */
  if (G_LIKELY (self->priv->values) && (iter =
          g_sequence_search (self->priv->values, &timestamp,
              (GCompareDataFunc) gst_control_point_find, NULL))) {
    GstControlPoint *cp;

    /* Iter contains the iter right after timestamp, i.e.
     * we need to get the previous one and check the timestamp
     */
    iter = g_sequence_iter_prev (iter);
    cp = g_sequence_get (iter);
    if (cp->timestamp == timestamp) {
      g_sequence_remove (iter);
      self->priv->nvalues--;
      self->priv->valid_cache = FALSE;
      res = TRUE;
    }
  }
  g_mutex_unlock (self->lock);

  return res;
}

/**
 * gst_interpolation_control_source_unset_all:
 * @self: the #GstInterpolationControlSource object
 *
 * Used to remove all time-stamped values of given controller-handled property
 *
 */
void
gst_interpolation_control_source_unset_all (GstInterpolationControlSource *
    self)
{
  g_return_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self));

  g_mutex_lock (self->lock);
  /* free GstControlPoint structures */
  if (self->priv->values) {
    g_sequence_free (self->priv->values);
    self->priv->values = NULL;
  }
  self->priv->nvalues = 0;
  self->priv->valid_cache = FALSE;

  g_mutex_unlock (self->lock);
}

static void
_append_control_point (GstControlPoint * cp, GList ** l)
{
  *l = g_list_prepend (*l, cp);
}

/**
 * gst_interpolation_control_source_get_all:
 * @self: the #GstInterpolationControlSource to get the list from
 *
 * Returns a read-only copy of the list of #GstTimedValue for the given property.
 * Free the list after done with it.
 *
 * Returns: a copy of the list, or %NULL if the property isn't handled by the controller
 */
GList *
gst_interpolation_control_source_get_all (GstInterpolationControlSource * self)
{
  GList *res = NULL;

  g_return_val_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self), NULL);

  g_mutex_lock (self->lock);
  if (G_LIKELY (self->priv->values))
    g_sequence_foreach (self->priv->values, (GFunc) _append_control_point,
        &res);
  g_mutex_unlock (self->lock);

  return g_list_reverse (res);
}

/**
 * gst_interpolation_control_source_get_count:
 * @self: the #GstInterpolationControlSource to get the number of values from
 *
 * Returns the number of control points that are set.
 *
 * Returns: the number of control points that are set.
 *
 */
gint
gst_interpolation_control_source_get_count (GstInterpolationControlSource *
    self)
{
  g_return_val_if_fail (GST_IS_INTERPOLATION_CONTROL_SOURCE (self), 0);
  return self->priv->nvalues;
}


static void
gst_interpolation_control_source_init (GstInterpolationControlSource * self)
{
  self->lock = g_mutex_new ();
  self->priv =
      G_TYPE_INSTANCE_GET_PRIVATE (self, GST_TYPE_INTERPOLATION_CONTROL_SOURCE,
      GstInterpolationControlSourcePrivate);
  self->priv->interpolation_mode = GST_INTERPOLATE_NONE;
}

static void
gst_interpolation_control_source_finalize (GObject * obj)
{
  GstInterpolationControlSource *self = GST_INTERPOLATION_CONTROL_SOURCE (obj);

  g_mutex_lock (self->lock);
  gst_interpolation_control_source_reset (self);
  g_mutex_unlock (self->lock);
  g_mutex_free (self->lock);
  G_OBJECT_CLASS (parent_class)->finalize (obj);
}

static void
gst_interpolation_control_source_dispose (GObject * obj)
{
  G_OBJECT_CLASS (parent_class)->dispose (obj);
}

static void
gst_interpolation_control_source_class_init (GstInterpolationControlSourceClass
    * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstControlSourceClass *csource_class = GST_CONTROL_SOURCE_CLASS (klass);

  parent_class = g_type_class_peek_parent (klass);
  g_type_class_add_private (klass,
      sizeof (GstInterpolationControlSourcePrivate));

  gobject_class->finalize = gst_interpolation_control_source_finalize;
  gobject_class->dispose = gst_interpolation_control_source_dispose;
  csource_class->bind = gst_interpolation_control_source_bind;
}
