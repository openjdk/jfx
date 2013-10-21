/* GStreamer
 *
 * Copyright (C) 2007,2010 Sebastian Dröge <sebastian.droege@collabora.co.uk>
 *
 * gstlfocontrolsource.c: Control source that provides some periodic waveforms
 *                        as control values.
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
 * SECTION:gstlfocontrolsource
 * @short_description: LFO control source
 *
 * #GstLFOControlSource is a #GstControlSource, that provides several periodic waveforms
 * as control values. It supports all fundamental, numeric GValue types as property.
 *
 * To use #GstLFOControlSource get a new instance by calling gst_lfo_control_source_new(),
 * bind it to a #GParamSpec and set the relevant properties or use
 * gst_lfo_control_source_set_waveform.
 *
 * All functions are MT-safe.
 *
 */

#include <glib-object.h>
#include <gst/gst.h>

#include "gstcontrolsource.h"
#include "gstlfocontrolsource.h"
#include "gstlfocontrolsourceprivate.h"

#include <gst/math-compat.h>

#define EMPTY(x) (x)

/* FIXME: as % in C is not the modulo operator we need here for
 * negative numbers implement our own. Are there better ways? */
static inline GstClockTime
_calculate_pos (GstClockTime timestamp, GstClockTime timeshift,
    GstClockTime period)
{
  while (timestamp < timeshift)
    timestamp += period;

  timestamp -= timeshift;

  return timestamp % period;
}

#define DEFINE_SINE(type,round,convert) \
static inline g##type \
_sine_get_##type (GstLFOControlSource *self, g##type max, g##type min, gdouble amp, gdouble off, GstClockTime timeshift, GstClockTime period, gdouble frequency, GstClockTime timestamp) \
{ \
  gdouble ret; \
  GstClockTime pos = _calculate_pos (timestamp, timeshift, period); \
  \
  ret = sin (2.0 * M_PI * (frequency / GST_SECOND) * gst_guint64_to_gdouble (pos)); \
  ret *= amp; \
  ret += off; \
  \
  if (round) \
    ret += 0.5; \
  \
  return (g##type) CLAMP (ret, convert (min), convert (max)); \
} \
\
static gboolean \
waveform_sine_get_##type (GstLFOControlSource *self, GstClockTime timestamp, \
    GValue *value) \
{ \
  g##type ret, max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  ret = _sine_get_##type (self, max, min, amp, off, timeshift, period, frequency, timestamp); \
  g_value_set_##type (value, ret); \
  g_mutex_unlock (self->lock); \
  return TRUE; \
} \
\
static gboolean \
waveform_sine_get_##type##_value_array (GstLFOControlSource *self, \
   GstClockTime timestamp, GstValueArray * value_array) \
{ \
  gint i; \
  GstClockTime ts = timestamp; \
  g##type *values = (g##type *) value_array->values; \
  g##type max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  for(i = 0; i < value_array->nbsamples; i++) { \
    *values = _sine_get_##type (self, max, min, amp, off, timeshift, period, frequency, ts); \
    ts += value_array->sample_interval; \
    values++; \
  } \
  g_mutex_unlock (self->lock); \
  return TRUE; \
}

DEFINE_SINE (int, TRUE, EMPTY);
DEFINE_SINE (uint, TRUE, EMPTY);
DEFINE_SINE (long, TRUE, EMPTY);
DEFINE_SINE (ulong, TRUE, EMPTY);
DEFINE_SINE (int64, TRUE, EMPTY);
DEFINE_SINE (uint64, TRUE, gst_guint64_to_gdouble);
DEFINE_SINE (float, FALSE, EMPTY);
DEFINE_SINE (double, FALSE, EMPTY);

static GstWaveformImplementation waveform_sine = {
  (GstControlSourceGetValue) waveform_sine_get_int,
  (GstControlSourceGetValueArray) waveform_sine_get_int_value_array,
  (GstControlSourceGetValue) waveform_sine_get_uint,
  (GstControlSourceGetValueArray) waveform_sine_get_uint_value_array,
  (GstControlSourceGetValue) waveform_sine_get_long,
  (GstControlSourceGetValueArray) waveform_sine_get_long_value_array,
  (GstControlSourceGetValue) waveform_sine_get_ulong,
  (GstControlSourceGetValueArray) waveform_sine_get_ulong_value_array,
  (GstControlSourceGetValue) waveform_sine_get_int64,
  (GstControlSourceGetValueArray) waveform_sine_get_int64_value_array,
  (GstControlSourceGetValue) waveform_sine_get_uint64,
  (GstControlSourceGetValueArray) waveform_sine_get_uint64_value_array,
  (GstControlSourceGetValue) waveform_sine_get_float,
  (GstControlSourceGetValueArray) waveform_sine_get_float_value_array,
  (GstControlSourceGetValue) waveform_sine_get_double,
  (GstControlSourceGetValueArray) waveform_sine_get_double_value_array
};

#define DEFINE_SQUARE(type,round, convert) \
\
static inline g##type \
_square_get_##type (GstLFOControlSource *self, g##type max, g##type min, gdouble amp, gdouble off, GstClockTime timeshift, GstClockTime period, gdouble frequency, GstClockTime timestamp) \
{ \
  GstClockTime pos = _calculate_pos (timestamp, timeshift, period); \
  gdouble ret; \
  \
  if (pos >= period / 2) \
    ret = amp; \
  else \
    ret = - amp; \
  \
  ret += off; \
  \
  if (round) \
    ret += 0.5; \
  \
  return (g##type) CLAMP (ret, convert (min), convert (max)); \
} \
\
static gboolean \
waveform_square_get_##type (GstLFOControlSource *self, GstClockTime timestamp, \
    GValue *value) \
{ \
  g##type ret, max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  ret = _square_get_##type (self, max, min, amp, off, timeshift, period, frequency, timestamp); \
  g_value_set_##type (value, ret); \
  g_mutex_unlock (self->lock); \
  return TRUE; \
} \
\
static gboolean \
waveform_square_get_##type##_value_array (GstLFOControlSource *self, \
   GstClockTime timestamp, GstValueArray * value_array) \
{ \
  gint i; \
  GstClockTime ts = timestamp; \
  g##type *values = (g##type *) value_array->values; \
  g##type max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  for(i = 0; i < value_array->nbsamples; i++) { \
    *values = _square_get_##type (self, max, min, amp, off, timeshift, period, frequency, ts); \
    ts += value_array->sample_interval; \
    values++; \
  } \
  g_mutex_unlock (self->lock); \
  return TRUE; \
}

DEFINE_SQUARE (int, TRUE, EMPTY);
DEFINE_SQUARE (uint, TRUE, EMPTY);
DEFINE_SQUARE (long, TRUE, EMPTY);
DEFINE_SQUARE (ulong, TRUE, EMPTY);
DEFINE_SQUARE (int64, TRUE, EMPTY);
DEFINE_SQUARE (uint64, TRUE, gst_guint64_to_gdouble);
DEFINE_SQUARE (float, FALSE, EMPTY);
DEFINE_SQUARE (double, FALSE, EMPTY);

static GstWaveformImplementation waveform_square = {
  (GstControlSourceGetValue) waveform_square_get_int,
  (GstControlSourceGetValueArray) waveform_square_get_int_value_array,
  (GstControlSourceGetValue) waveform_square_get_uint,
  (GstControlSourceGetValueArray) waveform_square_get_uint_value_array,
  (GstControlSourceGetValue) waveform_square_get_long,
  (GstControlSourceGetValueArray) waveform_square_get_long_value_array,
  (GstControlSourceGetValue) waveform_square_get_ulong,
  (GstControlSourceGetValueArray) waveform_square_get_ulong_value_array,
  (GstControlSourceGetValue) waveform_square_get_int64,
  (GstControlSourceGetValueArray) waveform_square_get_int64_value_array,
  (GstControlSourceGetValue) waveform_square_get_uint64,
  (GstControlSourceGetValueArray) waveform_square_get_uint64_value_array,
  (GstControlSourceGetValue) waveform_square_get_float,
  (GstControlSourceGetValueArray) waveform_square_get_float_value_array,
  (GstControlSourceGetValue) waveform_square_get_double,
  (GstControlSourceGetValueArray) waveform_square_get_double_value_array
};

#define DEFINE_SAW(type,round,convert) \
\
static inline g##type \
_saw_get_##type (GstLFOControlSource *self, g##type max, g##type min, gdouble amp, gdouble off, GstClockTime timeshift, GstClockTime period, gdouble frequency, GstClockTime timestamp) \
{ \
  GstClockTime pos = _calculate_pos (timestamp, timeshift, period); \
  gdouble ret; \
  \
  ret = - ((gst_guint64_to_gdouble (pos) - gst_guint64_to_gdouble (period) / 2.0) * ((2.0 * amp) / gst_guint64_to_gdouble (period)));\
  \
  ret += off; \
  \
  if (round) \
    ret += 0.5; \
  \
  return (g##type) CLAMP (ret, convert (min), convert (max)); \
} \
\
static gboolean \
waveform_saw_get_##type (GstLFOControlSource *self, GstClockTime timestamp, \
    GValue *value) \
{ \
  g##type ret, max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  ret = _saw_get_##type (self, max, min, amp, off, timeshift, period, frequency, timestamp); \
  g_value_set_##type (value, ret); \
  g_mutex_unlock (self->lock); \
  return TRUE; \
} \
\
static gboolean \
waveform_saw_get_##type##_value_array (GstLFOControlSource *self, \
   GstClockTime timestamp, GstValueArray * value_array) \
{ \
  gint i; \
  GstClockTime ts = timestamp; \
  g##type *values = (g##type *) value_array->values; \
  g##type max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  for(i = 0; i < value_array->nbsamples; i++) { \
    *values = _saw_get_##type (self, max, min, amp, off, timeshift, period, frequency, ts); \
    ts += value_array->sample_interval; \
    values++; \
  } \
  g_mutex_unlock (self->lock); \
  return TRUE; \
}

DEFINE_SAW (int, TRUE, EMPTY);
DEFINE_SAW (uint, TRUE, EMPTY);
DEFINE_SAW (long, TRUE, EMPTY);
DEFINE_SAW (ulong, TRUE, EMPTY);
DEFINE_SAW (int64, TRUE, EMPTY);
DEFINE_SAW (uint64, TRUE, gst_guint64_to_gdouble);
DEFINE_SAW (float, FALSE, EMPTY);
DEFINE_SAW (double, FALSE, EMPTY);

static GstWaveformImplementation waveform_saw = {
  (GstControlSourceGetValue) waveform_saw_get_int,
  (GstControlSourceGetValueArray) waveform_saw_get_int_value_array,
  (GstControlSourceGetValue) waveform_saw_get_uint,
  (GstControlSourceGetValueArray) waveform_saw_get_uint_value_array,
  (GstControlSourceGetValue) waveform_saw_get_long,
  (GstControlSourceGetValueArray) waveform_saw_get_long_value_array,
  (GstControlSourceGetValue) waveform_saw_get_ulong,
  (GstControlSourceGetValueArray) waveform_saw_get_ulong_value_array,
  (GstControlSourceGetValue) waveform_saw_get_int64,
  (GstControlSourceGetValueArray) waveform_saw_get_int64_value_array,
  (GstControlSourceGetValue) waveform_saw_get_uint64,
  (GstControlSourceGetValueArray) waveform_saw_get_uint64_value_array,
  (GstControlSourceGetValue) waveform_saw_get_float,
  (GstControlSourceGetValueArray) waveform_saw_get_float_value_array,
  (GstControlSourceGetValue) waveform_saw_get_double,
  (GstControlSourceGetValueArray) waveform_saw_get_double_value_array
};

#define DEFINE_RSAW(type,round,convert) \
\
static inline g##type \
_rsaw_get_##type (GstLFOControlSource *self, g##type max, g##type min, gdouble amp, gdouble off, GstClockTime timeshift, GstClockTime period, gdouble frequency, GstClockTime timestamp) \
{ \
  GstClockTime pos = _calculate_pos (timestamp, timeshift, period); \
  gdouble ret; \
  \
  ret = ((gst_guint64_to_gdouble (pos) - gst_guint64_to_gdouble (period) / 2.0) * ((2.0 * amp) / gst_guint64_to_gdouble (period)));\
  \
  ret += off; \
  \
  if (round) \
    ret += 0.5; \
  \
  return (g##type) CLAMP (ret, convert (min), convert (max)); \
} \
\
static gboolean \
waveform_rsaw_get_##type (GstLFOControlSource *self, GstClockTime timestamp, \
    GValue *value) \
{ \
  g##type ret, max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  ret = _rsaw_get_##type (self, max, min, amp, off, timeshift, period, frequency, timestamp); \
  g_value_set_##type (value, ret); \
  g_mutex_unlock (self->lock); \
  return TRUE; \
} \
\
static gboolean \
waveform_rsaw_get_##type##_value_array (GstLFOControlSource *self, \
   GstClockTime timestamp, GstValueArray * value_array) \
{ \
  gint i; \
  GstClockTime ts = timestamp; \
  g##type *values = (g##type *) value_array->values; \
  g##type max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  for(i = 0; i < value_array->nbsamples; i++) { \
    *values = _rsaw_get_##type (self, max, min, amp, off, timeshift, period, frequency, ts); \
    ts += value_array->sample_interval; \
    values++; \
  } \
  g_mutex_unlock (self->lock); \
  return TRUE; \
}

DEFINE_RSAW (int, TRUE, EMPTY);
DEFINE_RSAW (uint, TRUE, EMPTY);
DEFINE_RSAW (long, TRUE, EMPTY);
DEFINE_RSAW (ulong, TRUE, EMPTY);
DEFINE_RSAW (int64, TRUE, EMPTY);
DEFINE_RSAW (uint64, TRUE, gst_guint64_to_gdouble);
DEFINE_RSAW (float, FALSE, EMPTY);
DEFINE_RSAW (double, FALSE, EMPTY);

static GstWaveformImplementation waveform_rsaw = {
  (GstControlSourceGetValue) waveform_rsaw_get_int,
  (GstControlSourceGetValueArray) waveform_rsaw_get_int_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_uint,
  (GstControlSourceGetValueArray) waveform_rsaw_get_uint_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_long,
  (GstControlSourceGetValueArray) waveform_rsaw_get_long_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_ulong,
  (GstControlSourceGetValueArray) waveform_rsaw_get_ulong_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_int64,
  (GstControlSourceGetValueArray) waveform_rsaw_get_int64_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_uint64,
  (GstControlSourceGetValueArray) waveform_rsaw_get_uint64_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_float,
  (GstControlSourceGetValueArray) waveform_rsaw_get_float_value_array,
  (GstControlSourceGetValue) waveform_rsaw_get_double,
  (GstControlSourceGetValueArray) waveform_rsaw_get_double_value_array
};

#define DEFINE_TRIANGLE(type,round,convert) \
\
static inline g##type \
_triangle_get_##type (GstLFOControlSource *self, g##type max, g##type min, gdouble amp, gdouble off, GstClockTime timeshift, GstClockTime period, gdouble frequency, GstClockTime timestamp) \
{ \
  GstClockTime pos = _calculate_pos (timestamp, timeshift, period); \
  gdouble ret; \
  \
  if (gst_guint64_to_gdouble (pos) <= gst_guint64_to_gdouble (period) / 4.0) \
    ret = gst_guint64_to_gdouble (pos) * ((4.0 * amp) / gst_guint64_to_gdouble (period)); \
  else if (gst_guint64_to_gdouble (pos) <= (3.0 * gst_guint64_to_gdouble (period)) / 4.0) \
    ret = -(gst_guint64_to_gdouble (pos) - gst_guint64_to_gdouble (period) / 2.0) * ((4.0 * amp) / gst_guint64_to_gdouble (period)); \
  else \
    ret = gst_guint64_to_gdouble (period) - gst_guint64_to_gdouble (pos) * ((4.0 * amp) / gst_guint64_to_gdouble (period)); \
  \
  ret += off; \
  \
  if (round) \
    ret += 0.5; \
  \
  return (g##type) CLAMP (ret, convert (min), convert (max)); \
} \
\
static gboolean \
waveform_triangle_get_##type (GstLFOControlSource *self, GstClockTime timestamp, \
    GValue *value) \
{ \
  g##type ret, max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  ret = _triangle_get_##type (self, max, min, amp, off, timeshift, period, frequency, timestamp); \
  g_value_set_##type (value, ret); \
  g_mutex_unlock (self->lock); \
  return TRUE; \
} \
\
static gboolean \
waveform_triangle_get_##type##_value_array (GstLFOControlSource *self, \
   GstClockTime timestamp, GstValueArray * value_array) \
{ \
  gint i; \
  GstClockTime ts = timestamp; \
  g##type *values = (g##type *) value_array->values; \
  g##type max, min; \
  gdouble amp, off, frequency; \
  GstClockTime timeshift, period; \
  \
  g_mutex_lock (self->lock); \
  max = g_value_get_##type (&self->priv->maximum_value); \
  min = g_value_get_##type (&self->priv->minimum_value); \
  amp = convert (g_value_get_##type (&self->priv->amplitude)); \
  off = convert (g_value_get_##type (&self->priv->offset)); \
  timeshift = self->priv->timeshift; \
  period = self->priv->period; \
  frequency = self->priv->frequency; \
  \
  for(i = 0; i < value_array->nbsamples; i++) { \
    *values = _triangle_get_##type (self, max, min, amp, off, timeshift, period, frequency, ts); \
    ts += value_array->sample_interval; \
    values++; \
  } \
  g_mutex_unlock (self->lock); \
  return TRUE; \
}

DEFINE_TRIANGLE (int, TRUE, EMPTY);
DEFINE_TRIANGLE (uint, TRUE, EMPTY);
DEFINE_TRIANGLE (long, TRUE, EMPTY);
DEFINE_TRIANGLE (ulong, TRUE, EMPTY);
DEFINE_TRIANGLE (int64, TRUE, EMPTY);
DEFINE_TRIANGLE (uint64, TRUE, gst_guint64_to_gdouble);
DEFINE_TRIANGLE (float, FALSE, EMPTY);
DEFINE_TRIANGLE (double, FALSE, EMPTY);

static GstWaveformImplementation waveform_triangle = {
  (GstControlSourceGetValue) waveform_triangle_get_int,
  (GstControlSourceGetValueArray) waveform_triangle_get_int_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_uint,
  (GstControlSourceGetValueArray) waveform_triangle_get_uint_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_long,
  (GstControlSourceGetValueArray) waveform_triangle_get_long_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_ulong,
  (GstControlSourceGetValueArray) waveform_triangle_get_ulong_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_int64,
  (GstControlSourceGetValueArray) waveform_triangle_get_int64_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_uint64,
  (GstControlSourceGetValueArray) waveform_triangle_get_uint64_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_float,
  (GstControlSourceGetValueArray) waveform_triangle_get_float_value_array,
  (GstControlSourceGetValue) waveform_triangle_get_double,
  (GstControlSourceGetValueArray) waveform_triangle_get_double_value_array
};

static GstWaveformImplementation *waveforms[] = {
  &waveform_sine,
  &waveform_square,
  &waveform_saw,
  &waveform_rsaw,
  &waveform_triangle
};

static guint num_waveforms = G_N_ELEMENTS (waveforms);

enum
{
  PROP_WAVEFORM = 1,
  PROP_FREQUENCY,
  PROP_TIMESHIFT,
  PROP_AMPLITUDE,
  PROP_OFFSET
};

GType
gst_lfo_waveform_get_type (void)
{
  static gsize gtype = 0;
  static const GEnumValue values[] = {
    {GST_LFO_WAVEFORM_SINE, "GST_LFO_WAVEFORM_SINE",
        "sine"},
    {GST_LFO_WAVEFORM_SQUARE, "GST_LFO_WAVEFORM_SQUARE",
        "square"},
    {GST_LFO_WAVEFORM_SAW, "GST_LFO_WAVEFORM_SAW",
        "saw"},
    {GST_LFO_WAVEFORM_REVERSE_SAW, "GST_LFO_WAVEFORM_REVERSE_SAW",
        "reverse-saw"},
    {GST_LFO_WAVEFORM_TRIANGLE, "GST_LFO_WAVEFORM_TRIANGLE",
        "triangle"},
    {0, NULL, NULL}
  };

  if (g_once_init_enter (&gtype)) {
    GType tmp = g_enum_register_static ("GstLFOWaveform", values);
    g_once_init_leave (&gtype, tmp);
  }

  return (GType) gtype;
}

G_DEFINE_TYPE (GstLFOControlSource, gst_lfo_control_source,
    GST_TYPE_CONTROL_SOURCE);

static GObjectClass *parent_class = NULL;

static void
gst_lfo_control_source_reset (GstLFOControlSource * self)
{
  GstControlSource *csource = GST_CONTROL_SOURCE (self);

  csource->get_value = NULL;
  csource->get_value_array = NULL;

  self->priv->type = self->priv->base = G_TYPE_INVALID;

  if (G_IS_VALUE (&self->priv->minimum_value))
    g_value_unset (&self->priv->minimum_value);
  if (G_IS_VALUE (&self->priv->maximum_value))
    g_value_unset (&self->priv->maximum_value);

  if (G_IS_VALUE (&self->priv->amplitude))
    g_value_unset (&self->priv->amplitude);
  if (G_IS_VALUE (&self->priv->offset))
    g_value_unset (&self->priv->offset);
}

/**
 * gst_lfo_control_source_new:
 *
 * This returns a new, unbound #GstLFOControlSource.
 *
 * Returns: a new, unbound #GstLFOControlSource.
 */
GstLFOControlSource *
gst_lfo_control_source_new (void)
{
  return g_object_newv (GST_TYPE_LFO_CONTROL_SOURCE, 0, NULL);
}

static gboolean
gst_lfo_control_source_set_waveform (GstLFOControlSource * self,
    GstLFOWaveform waveform)
{
  GstControlSource *csource = GST_CONTROL_SOURCE (self);
  gboolean ret = TRUE;

  if (waveform >= num_waveforms || waveform < 0) {
    GST_WARNING ("waveform %d invalid or not implemented yet", waveform);
    return FALSE;
  }

  if (self->priv->base == G_TYPE_INVALID) {
    GST_WARNING ("not bound to a property yet");
    return FALSE;
  }

  switch (self->priv->base) {
    case G_TYPE_INT:
      csource->get_value = waveforms[waveform]->get_int;
      csource->get_value_array = waveforms[waveform]->get_int_value_array;
      break;
    case G_TYPE_UINT:{
      csource->get_value = waveforms[waveform]->get_uint;
      csource->get_value_array = waveforms[waveform]->get_uint_value_array;
      break;
    }
    case G_TYPE_LONG:{
      csource->get_value = waveforms[waveform]->get_long;
      csource->get_value_array = waveforms[waveform]->get_long_value_array;
      break;
    }
    case G_TYPE_ULONG:{
      csource->get_value = waveforms[waveform]->get_ulong;
      csource->get_value_array = waveforms[waveform]->get_ulong_value_array;
      break;
    }
    case G_TYPE_INT64:{
      csource->get_value = waveforms[waveform]->get_int64;
      csource->get_value_array = waveforms[waveform]->get_int64_value_array;
      break;
    }
    case G_TYPE_UINT64:{
      csource->get_value = waveforms[waveform]->get_uint64;
      csource->get_value_array = waveforms[waveform]->get_uint64_value_array;
      break;
    }
    case G_TYPE_FLOAT:{
      csource->get_value = waveforms[waveform]->get_float;
      csource->get_value_array = waveforms[waveform]->get_float_value_array;
      break;
    }
    case G_TYPE_DOUBLE:{
      csource->get_value = waveforms[waveform]->get_double;
      csource->get_value_array = waveforms[waveform]->get_double_value_array;
      break;
    }
    default:
      ret = FALSE;
      break;
  }

  if (ret)
    self->priv->waveform = waveform;
  else
    GST_WARNING ("incomplete implementation for type '%s'",
        GST_STR_NULL (g_type_name (self->priv->type)));

  return ret;
}

static gboolean
gst_lfo_control_source_bind (GstControlSource * source, GParamSpec * pspec)
{
  GType type, base;
  GstLFOControlSource *self = GST_LFO_CONTROL_SOURCE (source);
  gboolean ret = TRUE;

  /* get the fundamental base type */
  self->priv->type = base = type = G_PARAM_SPEC_VALUE_TYPE (pspec);
  while ((type = g_type_parent (type)))
    base = type;

  self->priv->base = base;
  /* restore type */
  type = self->priv->type;

  switch (base) {
    case G_TYPE_INT:{
      GParamSpecInt *tpspec = G_PARAM_SPEC_INT (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_int (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_int (&self->priv->maximum_value, tpspec->maximum);

      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_int (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_int (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_UINT:{
      GParamSpecUInt *tpspec = G_PARAM_SPEC_UINT (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_uint (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_uint (&self->priv->maximum_value, tpspec->maximum);

      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_uint (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_uint (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_LONG:{
      GParamSpecLong *tpspec = G_PARAM_SPEC_LONG (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_long (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_long (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_long (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_long (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_ULONG:{
      GParamSpecULong *tpspec = G_PARAM_SPEC_ULONG (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_ulong (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_ulong (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_ulong (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_ulong (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_INT64:{
      GParamSpecInt64 *tpspec = G_PARAM_SPEC_INT64 (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_int64 (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_int64 (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_int64 (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_int64 (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_UINT64:{
      GParamSpecUInt64 *tpspec = G_PARAM_SPEC_UINT64 (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_uint64 (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_uint64 (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_uint64 (&self->priv->amplitude, 0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_uint64 (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_FLOAT:{
      GParamSpecFloat *tpspec = G_PARAM_SPEC_FLOAT (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_float (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_float (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_float (&self->priv->amplitude, 0.0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_float (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    case G_TYPE_DOUBLE:{
      GParamSpecDouble *tpspec = G_PARAM_SPEC_DOUBLE (pspec);

      g_value_init (&self->priv->minimum_value, type);
      g_value_set_double (&self->priv->minimum_value, tpspec->minimum);
      g_value_init (&self->priv->maximum_value, type);
      g_value_set_double (&self->priv->maximum_value, tpspec->maximum);
      if (!G_IS_VALUE (&self->priv->amplitude)) {
        g_value_init (&self->priv->amplitude, type);
        g_value_set_double (&self->priv->amplitude, 0.0);
      }

      if (!G_IS_VALUE (&self->priv->offset)) {
        g_value_init (&self->priv->offset, type);
        g_value_set_double (&self->priv->offset, tpspec->default_value);
      }
      break;
    }
    default:
      GST_WARNING ("incomplete implementation for paramspec type '%s'",
          G_PARAM_SPEC_TYPE_NAME (pspec));
      ret = FALSE;
      break;
  }

  if (ret) {
    GValue amp = { 0, }
    , off = {
    0,};

    /* This should never fail unless the user already set amplitude or offset
     * with an incompatible type before _bind () */
    if (!g_value_type_transformable (G_VALUE_TYPE (&self->priv->amplitude),
            base)
        || !g_value_type_transformable (G_VALUE_TYPE (&self->priv->offset),
            base)) {
      GST_WARNING ("incompatible types for amplitude or offset");
      gst_lfo_control_source_reset (self);
      return FALSE;
    }

    /* Generate copies and transform to the correct type */
    g_value_init (&amp, base);
    g_value_transform (&self->priv->amplitude, &amp);
    g_value_init (&off, base);
    g_value_transform (&self->priv->offset, &off);

    ret = gst_lfo_control_source_set_waveform (self, self->priv->waveform);

    g_value_unset (&self->priv->amplitude);
    g_value_init (&self->priv->amplitude, self->priv->base);
    g_value_transform (&amp, &self->priv->amplitude);

    g_value_unset (&self->priv->offset);
    g_value_init (&self->priv->offset, self->priv->base);
    g_value_transform (&off, &self->priv->offset);

    g_value_unset (&amp);
    g_value_unset (&off);
  }

  if (!ret)
    gst_lfo_control_source_reset (self);

  return ret;
}

static void
gst_lfo_control_source_init (GstLFOControlSource * self)
{
  self->priv =
      G_TYPE_INSTANCE_GET_PRIVATE (self, GST_TYPE_LFO_CONTROL_SOURCE,
      GstLFOControlSourcePrivate);
  self->priv->waveform = GST_LFO_WAVEFORM_SINE;
  self->priv->frequency = 1.0;
  self->priv->period = GST_SECOND / self->priv->frequency;
  self->priv->timeshift = 0;

  self->lock = g_mutex_new ();
}

static void
gst_lfo_control_source_finalize (GObject * obj)
{
  GstLFOControlSource *self = GST_LFO_CONTROL_SOURCE (obj);

  gst_lfo_control_source_reset (self);

  if (self->lock) {
    g_mutex_free (self->lock);
    self->lock = NULL;
  }

  G_OBJECT_CLASS (parent_class)->finalize (obj);
}

static void
gst_lfo_control_source_dispose (GObject * obj)
{
  G_OBJECT_CLASS (parent_class)->dispose (obj);
}

static void
gst_lfo_control_source_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstLFOControlSource *self = GST_LFO_CONTROL_SOURCE (object);

  switch (prop_id) {
    case PROP_WAVEFORM:
      g_mutex_lock (self->lock);
      gst_lfo_control_source_set_waveform (self, g_value_get_enum (value));
      g_mutex_unlock (self->lock);
      break;
    case PROP_FREQUENCY:{
      gdouble frequency = g_value_get_double (value);

      g_return_if_fail (frequency > 0
          || ((GstClockTime) (GST_SECOND / frequency)) != 0);

      g_mutex_lock (self->lock);
      self->priv->frequency = frequency;
      self->priv->period = GST_SECOND / frequency;
      g_mutex_unlock (self->lock);
      break;
    }
    case PROP_TIMESHIFT:
      g_mutex_lock (self->lock);
      self->priv->timeshift = g_value_get_uint64 (value);
      g_mutex_unlock (self->lock);
      break;
    case PROP_AMPLITUDE:{
      GValue *val = g_value_get_boxed (value);

      if (self->priv->type != G_TYPE_INVALID) {
        g_return_if_fail (g_value_type_transformable (self->priv->type,
                G_VALUE_TYPE (val)));

        g_mutex_lock (self->lock);
        if (G_IS_VALUE (&self->priv->amplitude))
          g_value_unset (&self->priv->amplitude);

        g_value_init (&self->priv->amplitude, self->priv->type);
        g_value_transform (val, &self->priv->amplitude);
        g_mutex_unlock (self->lock);
      } else {
        g_mutex_lock (self->lock);
        if (G_IS_VALUE (&self->priv->amplitude))
          g_value_unset (&self->priv->amplitude);

        g_value_init (&self->priv->amplitude, G_VALUE_TYPE (val));
        g_value_copy (val, &self->priv->amplitude);
        g_mutex_unlock (self->lock);
      }

      break;
    }
    case PROP_OFFSET:{
      GValue *val = g_value_get_boxed (value);

      if (self->priv->type != G_TYPE_INVALID) {
        g_return_if_fail (g_value_type_transformable (self->priv->type,
                G_VALUE_TYPE (val)));

        g_mutex_lock (self->lock);
        if (G_IS_VALUE (&self->priv->offset))
          g_value_unset (&self->priv->offset);

        g_value_init (&self->priv->offset, self->priv->type);
        g_value_transform (val, &self->priv->offset);
        g_mutex_unlock (self->lock);
      } else {
        g_mutex_lock (self->lock);
        if (G_IS_VALUE (&self->priv->offset))
          g_value_unset (&self->priv->offset);

        g_value_init (&self->priv->offset, G_VALUE_TYPE (val));
        g_value_copy (val, &self->priv->offset);
        g_mutex_unlock (self->lock);
      }

      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_lfo_control_source_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstLFOControlSource *self = GST_LFO_CONTROL_SOURCE (object);

  switch (prop_id) {
    case PROP_WAVEFORM:
      g_value_set_enum (value, self->priv->waveform);
      break;
    case PROP_FREQUENCY:
      g_value_set_double (value, self->priv->frequency);
      break;
    case PROP_TIMESHIFT:
      g_value_set_uint64 (value, self->priv->timeshift);
      break;
    case PROP_AMPLITUDE:
      g_value_set_boxed (value, &self->priv->amplitude);
      break;
    case PROP_OFFSET:
      g_value_set_boxed (value, &self->priv->offset);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_lfo_control_source_class_init (GstLFOControlSourceClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstControlSourceClass *csource_class = GST_CONTROL_SOURCE_CLASS (klass);

  parent_class = g_type_class_peek_parent (klass);
  g_type_class_add_private (klass, sizeof (GstLFOControlSourcePrivate));

  gobject_class->finalize = gst_lfo_control_source_finalize;
  gobject_class->dispose = gst_lfo_control_source_dispose;
  gobject_class->set_property = gst_lfo_control_source_set_property;
  gobject_class->get_property = gst_lfo_control_source_get_property;

  csource_class->bind = gst_lfo_control_source_bind;

  /**
   * GstLFOControlSource:waveform
   *
   * Specifies the waveform that should be used for this #GstLFOControlSource.
   * 
   **/
  g_object_class_install_property (gobject_class, PROP_WAVEFORM,
      g_param_spec_enum ("waveform", "Waveform", "Waveform",
          GST_TYPE_LFO_WAVEFORM, GST_LFO_WAVEFORM_SINE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstLFOControlSource:frequency
   *
   * Specifies the frequency that should be used for the waveform
   * of this #GstLFOControlSource. It should be large enough
   * so that the period is longer than one nanosecond.
   * 
   **/
  g_object_class_install_property (gobject_class, PROP_FREQUENCY,
      g_param_spec_double ("frequency", "Frequency",
          "Frequency of the waveform", 0.0, G_MAXDOUBLE, 1.0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstLFOControlSource:timeshift
   *
   * Specifies the timeshift to the right that should be used for the waveform
   * of this #GstLFOControlSource in nanoseconds.
   *
   * To get a n nanosecond shift to the left use
   * "(GST_SECOND / frequency) - n".
   *
   **/
  g_object_class_install_property (gobject_class, PROP_TIMESHIFT,
      g_param_spec_uint64 ("timeshift", "Timeshift",
          "Timeshift of the waveform to the right", 0, G_MAXUINT64, 0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstLFOControlSource:amplitude
   *
   * Specifies the amplitude for the waveform of this #GstLFOControlSource.
   *
   * It should be given as a #GValue with a type that can be transformed
   * to the type of the bound property.
   **/
  g_object_class_install_property (gobject_class, PROP_AMPLITUDE,
      g_param_spec_boxed ("amplitude", "Amplitude", "Amplitude of the waveform",
          G_TYPE_VALUE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstLFOControlSource:offset
   *
   * Specifies the offset for the waveform of this #GstLFOControlSource.
   *
   * It should be given as a #GValue with a type that can be transformed
   * to the type of the bound property.
   **/
  g_object_class_install_property (gobject_class, PROP_OFFSET,
      g_param_spec_boxed ("offset", "Offset", "Offset of the waveform",
          G_TYPE_VALUE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
}
