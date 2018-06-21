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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * SECTION:gstinterpolationcontrolsource
 * @title: GstInterpolationControlSource
 * @short_description: interpolation control source
 *
 * #GstInterpolationControlSource is a #GstControlSource, that interpolates values between user-given
 * control points. It supports several interpolation modes and property types.
 *
 * To use #GstInterpolationControlSource get a new instance by calling
 * gst_interpolation_control_source_new(), bind it to a #GParamSpec and set some
 * control points by calling gst_timed_value_control_source_set().
 *
 * All functions are MT-safe.
 *
 */

#include <glib-object.h>
#include <gst/gst.h>

#include "gstinterpolationcontrolsource.h"
#include "gst/glib-compat-private.h"
#include "gst/math-compat.h"

#define GST_CAT_DEFAULT controller_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

/* helper functions */

static inline gboolean
_get_nearest_control_points (GstTimedValueControlSource * self,
    GstClockTime ts, GstControlPoint ** cp1, GstControlPoint ** cp2)
{
  GSequenceIter *iter;

  iter = gst_timed_value_control_source_find_control_point_iter (self, ts);
  if (iter) {
    *cp1 = g_sequence_get (iter);
    iter = g_sequence_iter_next (iter);
    if (iter && !g_sequence_iter_is_end (iter)) {
      *cp2 = g_sequence_get (iter);
    } else {
      *cp2 = NULL;
    }
    return TRUE;
  }
  return FALSE;
}

static inline void
_get_nearest_control_points2 (GstTimedValueControlSource * self,
    GstClockTime ts, GstControlPoint ** cp1, GstControlPoint ** cp2,
    GstClockTime * next_ts)
{
  GSequenceIter *iter1, *iter2 = NULL;

  *cp1 = *cp2 = NULL;
  iter1 = gst_timed_value_control_source_find_control_point_iter (self, ts);
  if (iter1) {
    *cp1 = g_sequence_get (iter1);
    iter2 = g_sequence_iter_next (iter1);
  } else {
    if (G_LIKELY (self->values)) {
      /* all values in the control point list come after the given timestamp */
      iter2 = g_sequence_get_begin_iter (self->values);
      /* why this? if !cp1 we don't interpolate anyway
       * if we can eliminate this, we can also use _get_nearest_control_points()
       * here, is this just to set next_ts? */
    } else {
      /* no values */
      iter2 = NULL;
    }
  }

  if (iter2 && !g_sequence_iter_is_end (iter2)) {
    *cp2 = g_sequence_get (iter2);
    *next_ts = (*cp2)->timestamp;
  } else {
    *next_ts = GST_CLOCK_TIME_NONE;
  }
}


/*  steps-like (no-)interpolation, default */
/*  just returns the value for the most recent key-frame */
static inline gdouble
_interpolate_none (GstTimedValueControlSource * self, GstControlPoint * cp)
{
  return cp->value;
}

static gboolean
interpolate_none_get (GstTimedValueControlSource * self, GstClockTime timestamp,
    gdouble * value)
{
  gboolean ret = FALSE;
  GSequenceIter *iter;
  GstControlPoint *cp;

  g_mutex_lock (&self->lock);

  iter =
      gst_timed_value_control_source_find_control_point_iter (self, timestamp);
  if (iter) {
    cp = g_sequence_get (iter);
    *value = _interpolate_none (self, cp);
    ret = TRUE;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}

static gboolean
interpolate_none_get_value_array (GstTimedValueControlSource * self,
    GstClockTime timestamp, GstClockTime interval, guint n_values,
    gdouble * values)
{
  gboolean ret = FALSE;
  guint i;
  GstClockTime ts = timestamp;
  GstClockTime next_ts = 0;
  GstControlPoint *cp1 = NULL, *cp2 = NULL;

  g_mutex_lock (&self->lock);

  for (i = 0; i < n_values; i++) {
    GST_LOG ("values[%3d] : ts=%" GST_TIME_FORMAT ", next_ts=%" GST_TIME_FORMAT,
        i, GST_TIME_ARGS (ts), GST_TIME_ARGS (next_ts));
    if (ts >= next_ts) {
      _get_nearest_control_points2 (self, ts, &cp1, &cp2, &next_ts);
    }
    if (cp1) {
      *values = _interpolate_none (self, cp1);
      ret = TRUE;
      GST_LOG ("values[%3d]=%lf", i, *values);
    } else {
      *values = NAN;
      GST_LOG ("values[%3d]=-", i);
    }
    ts += interval;
    values++;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}



/*  linear interpolation */
/*  smoothes inbetween values */
static inline gdouble
_interpolate_linear (GstClockTime timestamp1, gdouble value1,
    GstClockTime timestamp2, gdouble value2, GstClockTime timestamp)
{
  if (GST_CLOCK_TIME_IS_VALID (timestamp2)) {
    gdouble slope;

    slope =
        (value2 - value1) / gst_guint64_to_gdouble (timestamp2 - timestamp1);
    return value1 + (gst_guint64_to_gdouble (timestamp - timestamp1) * slope);
  } else {
    return value1;
  }
}

static gboolean
interpolate_linear_get (GstTimedValueControlSource * self,
    GstClockTime timestamp, gdouble * value)
{
  gboolean ret = FALSE;
  GstControlPoint *cp1, *cp2;

  g_mutex_lock (&self->lock);

  if (_get_nearest_control_points (self, timestamp, &cp1, &cp2)) {
    *value = _interpolate_linear (cp1->timestamp, cp1->value,
        (cp2 ? cp2->timestamp : GST_CLOCK_TIME_NONE),
        (cp2 ? cp2->value : 0.0), timestamp);
    ret = TRUE;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}

static gboolean
interpolate_linear_get_value_array (GstTimedValueControlSource * self,
    GstClockTime timestamp, GstClockTime interval, guint n_values,
    gdouble * values)
{
  gboolean ret = FALSE;
  guint i;
  GstClockTime ts = timestamp;
  GstClockTime next_ts = 0;
  GstControlPoint *cp1 = NULL, *cp2 = NULL;

  g_mutex_lock (&self->lock);

  for (i = 0; i < n_values; i++) {
    GST_LOG ("values[%3d] : ts=%" GST_TIME_FORMAT ", next_ts=%" GST_TIME_FORMAT,
        i, GST_TIME_ARGS (ts), GST_TIME_ARGS (next_ts));
    if (ts >= next_ts) {
      _get_nearest_control_points2 (self, ts, &cp1, &cp2, &next_ts);
    }
    if (cp1) {
      *values = _interpolate_linear (cp1->timestamp, cp1->value,
          (cp2 ? cp2->timestamp : GST_CLOCK_TIME_NONE),
          (cp2 ? cp2->value : 0.0), ts);
      ret = TRUE;
      GST_LOG ("values[%3d]=%lf", i, *values);
    } else {
      *values = NAN;
      GST_LOG ("values[%3d]=-", i);
    }
    ts += interval;
    values++;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}



/*  cubic interpolation */

/* The following functions implement a natural cubic spline interpolator.
 * For details look at http://en.wikipedia.org/wiki/Spline_interpolation
 *
 * Instead of using a real matrix with n^2 elements for the linear system
 * of equations we use three arrays o, p, q to hold the tridiagonal matrix
 * as following to save memory:
 *
 * p[0] q[0]    0    0    0
 * o[1] p[1] q[1]    0    0
 *    0 o[2] p[2] q[2]    .
 *    .    .    .    .    .
 */

static void
_interpolate_cubic_update_cache (GstTimedValueControlSource * self)
{
  gint i, n = self->nvalues;
  gdouble *o = g_new0 (gdouble, n);
  gdouble *p = g_new0 (gdouble, n);
  gdouble *q = g_new0 (gdouble, n);

  gdouble *h = g_new0 (gdouble, n);
  gdouble *b = g_new0 (gdouble, n);
  gdouble *z = g_new0 (gdouble, n);

  GSequenceIter *iter;
  GstControlPoint *cp;
  GstClockTime x, x_next;
  gdouble y_prev, y, y_next;

  /* Fill linear system of equations */
  iter = g_sequence_get_begin_iter (self->values);
  cp = g_sequence_get (iter);
  x = cp->timestamp;
  y = cp->value;

  p[0] = 1.0;

  iter = g_sequence_iter_next (iter);
  cp = g_sequence_get (iter);
  x_next = cp->timestamp;
  y_next = cp->value;
  h[0] = gst_guint64_to_gdouble (x_next - x);

  for (i = 1; i < n - 1; i++) {
    /* Shuffle x and y values */
    y_prev = y;
    x = x_next;
    y = y_next;
    iter = g_sequence_iter_next (iter);
    cp = g_sequence_get (iter);
    x_next = cp->timestamp;
    y_next = cp->value;

    h[i] = gst_guint64_to_gdouble (x_next - x);
    o[i] = h[i - 1];
    p[i] = 2.0 * (h[i - 1] + h[i]);
    q[i] = h[i];
    b[i] = (y_next - y) / h[i] - (y - y_prev) / h[i - 1];
  }
  p[n - 1] = 1.0;

  /* Use Gauss elimination to set everything below the diagonal to zero */
  for (i = 1; i < n - 1; i++) {
    gdouble a = o[i] / p[i - 1];
    p[i] -= a * q[i - 1];
    b[i] -= a * b[i - 1];
  }

  /* Solve everything else from bottom to top */
  for (i = n - 2; i > 0; i--)
    z[i] = (b[i] - q[i] * z[i + 1]) / p[i];

  /* Save cache next in the GstControlPoint */

  iter = g_sequence_get_begin_iter (self->values);
  for (i = 0; i < n; i++) {
    cp = g_sequence_get (iter);
    cp->cache.cubic.h = h[i];
    cp->cache.cubic.z = z[i];
    iter = g_sequence_iter_next (iter);
  }

  /* Free our temporary arrays */
  g_free (o);
  g_free (p);
  g_free (q);
  g_free (h);
  g_free (b);
  g_free (z);
}

static inline gdouble
_interpolate_cubic (GstTimedValueControlSource * self, GstControlPoint * cp1,
    gdouble value1, GstControlPoint * cp2, gdouble value2,
    GstClockTime timestamp)
{
  if (!self->valid_cache) {
    _interpolate_cubic_update_cache (self);
    self->valid_cache = TRUE;
  }

  if (cp2) {
    gdouble diff1, diff2;
    gdouble out;

    diff1 = gst_guint64_to_gdouble (timestamp - cp1->timestamp);
    diff2 = gst_guint64_to_gdouble (cp2->timestamp - timestamp);

    out =
        (cp2->cache.cubic.z * diff1 * diff1 * diff1 +
        cp1->cache.cubic.z * diff2 * diff2 * diff2) / cp1->cache.cubic.h;
    out +=
        (value2 / cp1->cache.cubic.h -
        cp1->cache.cubic.h * cp2->cache.cubic.z) * diff1;
    out +=
        (value1 / cp1->cache.cubic.h -
        cp1->cache.cubic.h * cp1->cache.cubic.z) * diff2;
    return out;
  } else {
    return value1;
  }
}

static gboolean
interpolate_cubic_get (GstTimedValueControlSource * self,
    GstClockTime timestamp, gdouble * value)
{
  gboolean ret = FALSE;
  GstControlPoint *cp1, *cp2 = NULL;

  if (self->nvalues <= 2)
    return interpolate_linear_get (self, timestamp, value);

  g_mutex_lock (&self->lock);

  if (_get_nearest_control_points (self, timestamp, &cp1, &cp2)) {
    *value = _interpolate_cubic (self, cp1, cp1->value, cp2,
        (cp2 ? cp2->value : 0.0), timestamp);
    ret = TRUE;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}

static gboolean
interpolate_cubic_get_value_array (GstTimedValueControlSource * self,
    GstClockTime timestamp, GstClockTime interval, guint n_values,
    gdouble * values)
{
  gboolean ret = FALSE;
  guint i;
  GstClockTime ts = timestamp;
  GstClockTime next_ts = 0;
  GstControlPoint *cp1 = NULL, *cp2 = NULL;

  if (self->nvalues <= 2)
    return interpolate_linear_get_value_array (self, timestamp, interval,
        n_values, values);

  g_mutex_lock (&self->lock);

  for (i = 0; i < n_values; i++) {
    GST_LOG ("values[%3d] : ts=%" GST_TIME_FORMAT ", next_ts=%" GST_TIME_FORMAT,
        i, GST_TIME_ARGS (ts), GST_TIME_ARGS (next_ts));
    if (ts >= next_ts) {
      _get_nearest_control_points2 (self, ts, &cp1, &cp2, &next_ts);
    }
    if (cp1) {
      *values = _interpolate_cubic (self, cp1, cp1->value, cp2,
          (cp2 ? cp2->value : 0.0), ts);
      ret = TRUE;
      GST_LOG ("values[%3d]=%lf", i, *values);
    } else {
      *values = NAN;
      GST_LOG ("values[%3d]=-", i);
    }
    ts += interval;
    values++;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}


/*  monotonic cubic interpolation */

/* the following functions implement monotonic cubic spline interpolation.
 * For details: http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
 *
 * In contrast to the previous cubic mode, the values won't overshoot.
 */

static void
_interpolate_cubic_monotonic_update_cache (GstTimedValueControlSource * self)
{
  gint i, n = self->nvalues;
  gdouble *dxs = g_new0 (gdouble, n);
  gdouble *dys = g_new0 (gdouble, n);
  gdouble *ms = g_new0 (gdouble, n);
  gdouble *c1s = g_new0 (gdouble, n);

  GSequenceIter *iter;
  GstControlPoint *cp;
  GstClockTime x, x_next, dx;
  gdouble y, y_next, dy;

  /* Get consecutive differences and slopes */
  iter = g_sequence_get_begin_iter (self->values);
  cp = g_sequence_get (iter);
  x_next = cp->timestamp;
  y_next = cp->value;
  for (i = 0; i < n - 1; i++) {
    x = x_next;
    y = y_next;
    iter = g_sequence_iter_next (iter);
    cp = g_sequence_get (iter);
    x_next = cp->timestamp;
    y_next = cp->value;

    dx = gst_guint64_to_gdouble (x_next - x);
    dy = y_next - y;
    dxs[i] = dx;
    dys[i] = dy;
    ms[i] = dy / dx;
  }

  /* Get degree-1 coefficients */
  c1s[0] = ms[0];
  for (i = 1; i < n; i++) {
    gdouble m = ms[i - 1];
    gdouble m_next = ms[i];

    if (m * m_next <= 0) {
      c1s[i] = 0.0;
    } else {
      gdouble dx_next, dx_sum;

      dx = dxs[i], dx_next = dxs[i + 1], dx_sum = dx + dx_next;
      c1s[i] = 3.0 * dx_sum / ((dx_sum + dx_next) / m + (dx_sum + dx) / m_next);
    }
  }
  c1s[n - 1] = ms[n - 1];

  /* Get degree-2 and degree-3 coefficients */
  iter = g_sequence_get_begin_iter (self->values);
  for (i = 0; i < n - 1; i++) {
    gdouble c1, m, inv_dx, common;
    cp = g_sequence_get (iter);

    c1 = c1s[i];
    m = ms[i];
    inv_dx = 1.0 / dxs[i];
    common = c1 + c1s[i + 1] - m - m;

    cp->cache.cubic_monotonic.c1s = c1;
    cp->cache.cubic_monotonic.c2s = (m - c1 - common) * inv_dx;
    cp->cache.cubic_monotonic.c3s = common * inv_dx * inv_dx;

    iter = g_sequence_iter_next (iter);
  }

  /* Free our temporary arrays */
  g_free (dxs);
  g_free (dys);
  g_free (ms);
  g_free (c1s);
}

static inline gdouble
_interpolate_cubic_monotonic (GstTimedValueControlSource * self,
    GstControlPoint * cp1, gdouble value1, GstControlPoint * cp2,
    gdouble value2, GstClockTime timestamp)
{
  if (!self->valid_cache) {
    _interpolate_cubic_monotonic_update_cache (self);
    self->valid_cache = TRUE;
  }

  if (cp2) {
    gdouble diff = gst_guint64_to_gdouble (timestamp - cp1->timestamp);
    gdouble diff2 = diff * diff;
    gdouble out;

    out = value1 + cp1->cache.cubic_monotonic.c1s * diff;
    out += cp1->cache.cubic_monotonic.c2s * diff2;
    out += cp1->cache.cubic_monotonic.c3s * diff * diff2;
    return out;
  } else {
    return value1;
  }
}

static gboolean
interpolate_cubic_monotonic_get (GstTimedValueControlSource * self,
    GstClockTime timestamp, gdouble * value)
{
  gboolean ret = FALSE;
  GstControlPoint *cp1, *cp2 = NULL;

  if (self->nvalues <= 2)
    return interpolate_linear_get (self, timestamp, value);

  g_mutex_lock (&self->lock);

  if (_get_nearest_control_points (self, timestamp, &cp1, &cp2)) {
    *value = _interpolate_cubic_monotonic (self, cp1, cp1->value, cp2,
        (cp2 ? cp2->value : 0.0), timestamp);
    ret = TRUE;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}

static gboolean
interpolate_cubic_monotonic_get_value_array (GstTimedValueControlSource * self,
    GstClockTime timestamp, GstClockTime interval, guint n_values,
    gdouble * values)
{
  gboolean ret = FALSE;
  guint i;
  GstClockTime ts = timestamp;
  GstClockTime next_ts = 0;
  GstControlPoint *cp1 = NULL, *cp2 = NULL;

  if (self->nvalues <= 2)
    return interpolate_linear_get_value_array (self, timestamp, interval,
        n_values, values);

  g_mutex_lock (&self->lock);

  for (i = 0; i < n_values; i++) {
    GST_LOG ("values[%3d] : ts=%" GST_TIME_FORMAT ", next_ts=%" GST_TIME_FORMAT,
        i, GST_TIME_ARGS (ts), GST_TIME_ARGS (next_ts));
    if (ts >= next_ts) {
      _get_nearest_control_points2 (self, ts, &cp1, &cp2, &next_ts);
    }
    if (cp1) {
      *values = _interpolate_cubic_monotonic (self, cp1, cp1->value, cp2,
          (cp2 ? cp2->value : 0.0), ts);
      ret = TRUE;
      GST_LOG ("values[%3d]=%lf", i, *values);
    } else {
      *values = NAN;
      GST_LOG ("values[%3d]=-", i);
    }
    ts += interval;
    values++;
  }
  g_mutex_unlock (&self->lock);
  return ret;
}


static struct
{
  GstControlSourceGetValue get;
  GstControlSourceGetValueArray get_value_array;
} interpolation_modes[] = {
  {
  (GstControlSourceGetValue) interpolate_none_get,
        (GstControlSourceGetValueArray) interpolate_none_get_value_array}, {
  (GstControlSourceGetValue) interpolate_linear_get,
        (GstControlSourceGetValueArray) interpolate_linear_get_value_array}, {
  (GstControlSourceGetValue) interpolate_cubic_get,
        (GstControlSourceGetValueArray) interpolate_cubic_get_value_array}, {
    (GstControlSourceGetValue) interpolate_cubic_monotonic_get,
        (GstControlSourceGetValueArray)
interpolate_cubic_monotonic_get_value_array}};

static const guint num_interpolation_modes = G_N_ELEMENTS (interpolation_modes);

enum
{
  PROP_MODE = 1
};

#define _do_init \
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "interpolation control source", 0, \
    "timeline value interpolating control source")

G_DEFINE_TYPE_WITH_CODE (GstInterpolationControlSource,
    gst_interpolation_control_source, GST_TYPE_TIMED_VALUE_CONTROL_SOURCE,
    _do_init);

struct _GstInterpolationControlSourcePrivate
{
  GstInterpolationMode interpolation_mode;
};

/**
 * gst_interpolation_control_source_new:
 *
 * This returns a new, unbound #GstInterpolationControlSource.
 *
 * Returns: (transfer full): a new, unbound #GstInterpolationControlSource.
 */
GstControlSource *
gst_interpolation_control_source_new (void)
{
  GstControlSource *csource =
      g_object_new (GST_TYPE_INTERPOLATION_CONTROL_SOURCE, NULL);

  /* Clear floating flag */
  gst_object_ref_sink (csource);

  return csource;
}

static gboolean
    gst_interpolation_control_source_set_interpolation_mode
    (GstInterpolationControlSource * self, GstInterpolationMode mode)
{
  GstControlSource *csource = GST_CONTROL_SOURCE (self);

  if (mode >= num_interpolation_modes || (int) mode < 0) {
    GST_WARNING ("interpolation mode %d invalid or not implemented yet", mode);
    return FALSE;
  }

  GST_TIMED_VALUE_CONTROL_SOURCE_LOCK (self);
  csource->get_value = interpolation_modes[mode].get;
  csource->get_value_array = interpolation_modes[mode].get_value_array;

  gst_timed_value_control_invalidate_cache ((GstTimedValueControlSource *)
      csource);
  self->priv->interpolation_mode = mode;

  GST_TIMED_VALUE_CONTROL_SOURCE_UNLOCK (self);

  return TRUE;
}

static void
gst_interpolation_control_source_init (GstInterpolationControlSource * self)
{
  self->priv =
      G_TYPE_INSTANCE_GET_PRIVATE (self, GST_TYPE_INTERPOLATION_CONTROL_SOURCE,
      GstInterpolationControlSourcePrivate);
  gst_interpolation_control_source_set_interpolation_mode (self,
      GST_INTERPOLATION_MODE_NONE);
}

static void
gst_interpolation_control_source_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstInterpolationControlSource *self =
      GST_INTERPOLATION_CONTROL_SOURCE (object);

  switch (prop_id) {
    case PROP_MODE:
      gst_interpolation_control_source_set_interpolation_mode (self,
          (GstInterpolationMode) g_value_get_enum (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_interpolation_control_source_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstInterpolationControlSource *self =
      GST_INTERPOLATION_CONTROL_SOURCE (object);

  switch (prop_id) {
    case PROP_MODE:
      g_value_set_enum (value, self->priv->interpolation_mode);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_interpolation_control_source_class_init (GstInterpolationControlSourceClass
    * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  //GstControlSourceClass *csource_class = GST_CONTROL_SOURCE_CLASS (klass);

  g_type_class_add_private (klass,
      sizeof (GstInterpolationControlSourcePrivate));

  gobject_class->set_property = gst_interpolation_control_source_set_property;
  gobject_class->get_property = gst_interpolation_control_source_get_property;

  g_object_class_install_property (gobject_class, PROP_MODE,
      g_param_spec_enum ("mode", "Mode", "Interpolation mode",
          GST_TYPE_INTERPOLATION_MODE, GST_INTERPOLATION_MODE_NONE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
}
