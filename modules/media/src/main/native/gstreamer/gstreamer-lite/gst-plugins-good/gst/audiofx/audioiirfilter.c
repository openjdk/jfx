/* 
 * GStreamer
 * Copyright (C) 2009 Sebastian Dröge <sebastian.droege@collabora.co.uk>
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
 * 
 */

/**
 * SECTION:element-audioiirfilter
 *
 * audioiirfilter implements a generic audio <ulink url="http://en.wikipedia.org/wiki/Infinite_impulse_response">IIR filter</ulink>. Before usage the
 * "a" and "b" properties have to be set to the filter coefficients that
 * should be used.
 *
 * The filter coefficients describe the numerator and denominator of the
 * transfer function.
 *
 * To change the filter coefficients whenever the sampling rate changes the
 * "rate-changed" signal can be used. This should be done for most
 * IIR filters as they're depending on the sampling rate.
 *
 * <refsect2>
 * <title>Example application</title>
 * |[
 * <xi:include xmlns:xi="http://www.w3.org/2003/XInclude" parse="text" href="../../../../tests/examples/audiofx/iirfilter-example.c" />
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>
#include <math.h>
#include <gst/gst.h>
#include <gst/audio/gstaudiofilter.h>
#include <gst/controller/gstcontroller.h>

#include "audioiirfilter.h"

#define GST_CAT_DEFAULT gst_audio_iir_filter_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

enum
{
  SIGNAL_RATE_CHANGED,
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_A,
  PROP_B
};

static guint gst_audio_iir_filter_signals[LAST_SIGNAL] = { 0, };

#define DEBUG_INIT(bla) \
  GST_DEBUG_CATEGORY_INIT (gst_audio_iir_filter_debug, "audioiirfilter", 0, \
      "Generic audio IIR filter plugin");

GST_BOILERPLATE_FULL (GstAudioIIRFilter, gst_audio_iir_filter, GstAudioFilter,
    GST_TYPE_AUDIO_FX_BASE_IIR_FILTER, DEBUG_INIT);

static void gst_audio_iir_filter_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_audio_iir_filter_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);
static void gst_audio_iir_filter_finalize (GObject * object);

static gboolean gst_audio_iir_filter_setup (GstAudioFilter * base,
    GstRingBufferSpec * format);

/* Element class */
static void
gst_audio_iir_filter_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (element_class,
      "Audio IIR filter", "Filter/Effect/Audio",
      "Generic audio IIR filter with custom filter kernel",
      "Sebastian Dröge <sebastian.droege@collabora.co.uk>");
}

static void
gst_audio_iir_filter_class_init (GstAudioIIRFilterClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;
  GstAudioFilterClass *filter_class = (GstAudioFilterClass *) klass;

  gobject_class->set_property = gst_audio_iir_filter_set_property;
  gobject_class->get_property = gst_audio_iir_filter_get_property;
  gobject_class->finalize = gst_audio_iir_filter_finalize;

  g_object_class_install_property (gobject_class, PROP_A,
      g_param_spec_value_array ("a", "A",
          "Filter coefficients (numerator of transfer function)",
          g_param_spec_double ("Coefficient", "Filter Coefficient",
              "Filter coefficient", -G_MAXDOUBLE, G_MAXDOUBLE, 0.0,
              G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS),
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_B,
      g_param_spec_value_array ("b", "B",
          "Filter coefficients (denominator of transfer function)",
          g_param_spec_double ("Coefficient", "Filter Coefficient",
              "Filter coefficient", -G_MAXDOUBLE, G_MAXDOUBLE, 0.0,
              G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS),
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  filter_class->setup = GST_DEBUG_FUNCPTR (gst_audio_iir_filter_setup);

  /**
   * GstAudioIIRFilter::rate-changed:
   * @filter: the filter on which the signal is emitted
   * @rate: the new sampling rate
   *
   * Will be emitted when the sampling rate changes. The callbacks
   * will be called from the streaming thread and processing will
   * stop until the event is handled.
   */
  gst_audio_iir_filter_signals[SIGNAL_RATE_CHANGED] =
      g_signal_new ("rate-changed", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstAudioIIRFilterClass, rate_changed),
      NULL, NULL, gst_marshal_VOID__INT, G_TYPE_NONE, 1, G_TYPE_INT);
}

static void
gst_audio_iir_filter_update_coefficients (GstAudioIIRFilter * self,
    GValueArray * va, GValueArray * vb)
{
  gdouble *a = NULL, *b = NULL;
  guint i;

  if (va) {
    if (self->a)
      g_value_array_free (self->a);

    self->a = va;
  }
  if (vb) {
    if (self->b)
      g_value_array_free (self->b);

    self->b = vb;
  }

  if (self->a && self->a->n_values > 0) {
    a = g_new (gdouble, self->a->n_values);

    for (i = 0; i < self->a->n_values; i++) {
      GValue *v = g_value_array_get_nth (self->a, i);
      a[i] = g_value_get_double (v);
    }
  }

  if (self->b && self->b->n_values > 0) {
    b = g_new (gdouble, self->b->n_values);
    for (i = 0; i < self->b->n_values; i++) {
      GValue *v = g_value_array_get_nth (self->b, i);
      b[i] = g_value_get_double (v);
    }
  }

  gst_audio_fx_base_iir_filter_set_coefficients (GST_AUDIO_FX_BASE_IIR_FILTER
      (self), a, (self->a) ? self->a->n_values : 0, b,
      (self->b) ? self->b->n_values : 0);
}

static void
gst_audio_iir_filter_init (GstAudioIIRFilter * self,
    GstAudioIIRFilterClass * g_class)
{
  GValue v = { 0, };
  GValueArray *a, *b;

  a = g_value_array_new (1);

  g_value_init (&v, G_TYPE_DOUBLE);
  g_value_set_double (&v, 1.0);
  g_value_array_append (a, &v);
  g_value_unset (&v);

  b = NULL;
  gst_audio_iir_filter_update_coefficients (self, a, b);

  self->lock = g_mutex_new ();
}

/* GstAudioFilter vmethod implementations */

/* get notified of caps and plug in the correct process function */
static gboolean
gst_audio_iir_filter_setup (GstAudioFilter * base, GstRingBufferSpec * format)
{
  GstAudioIIRFilter *self = GST_AUDIO_IIR_FILTER (base);

  if (self->rate != format->rate) {
    g_signal_emit (G_OBJECT (self),
        gst_audio_iir_filter_signals[SIGNAL_RATE_CHANGED], 0, format->rate);
    self->rate = format->rate;
  }

  return GST_AUDIO_FILTER_CLASS (parent_class)->setup (base, format);
}

static void
gst_audio_iir_filter_finalize (GObject * object)
{
  GstAudioIIRFilter *self = GST_AUDIO_IIR_FILTER (object);

  g_mutex_free (self->lock);
  self->lock = NULL;

  if (self->a)
    g_value_array_free (self->a);
  self->a = NULL;
  if (self->b)
    g_value_array_free (self->b);
  self->b = NULL;

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
gst_audio_iir_filter_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAudioIIRFilter *self = GST_AUDIO_IIR_FILTER (object);

  g_return_if_fail (GST_IS_AUDIO_IIR_FILTER (self));

  switch (prop_id) {
    case PROP_A:
      g_mutex_lock (self->lock);
      gst_audio_iir_filter_update_coefficients (self, g_value_dup_boxed (value),
          NULL);
      g_mutex_unlock (self->lock);
      break;
    case PROP_B:
      g_mutex_lock (self->lock);
      gst_audio_iir_filter_update_coefficients (self, NULL,
          g_value_dup_boxed (value));
      g_mutex_unlock (self->lock);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_audio_iir_filter_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAudioIIRFilter *self = GST_AUDIO_IIR_FILTER (object);

  switch (prop_id) {
    case PROP_A:
      g_value_set_boxed (value, self->a);
      break;
    case PROP_B:
      g_value_set_boxed (value, self->b);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}
