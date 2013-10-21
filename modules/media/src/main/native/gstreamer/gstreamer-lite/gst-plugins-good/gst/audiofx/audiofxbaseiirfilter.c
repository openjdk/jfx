/* 
 * GStreamer
 * Copyright (C) 2007-2009 Sebastian Dröge <sebastian.droege@collabora.co.uk>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>
#include <gst/audio/audio.h>
#include <gst/audio/gstaudiofilter.h>
#include <gst/controller/gstcontroller.h>

#include <math.h>

#include "audiofxbaseiirfilter.h"

#define GST_CAT_DEFAULT gst_audio_fx_base_iir_filter_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define ALLOWED_CAPS \
    "audio/x-raw-float,"                                              \
    " width = (int) { 32, 64 }, "                                     \
    " endianness = (int) BYTE_ORDER,"                                 \
    " rate = (int) [ 1, MAX ],"                                       \
    " channels = (int) [ 1, MAX ]"

#define DEBUG_INIT(bla) \
  GST_DEBUG_CATEGORY_INIT (gst_audio_fx_base_iir_filter_debug, "audiofxbaseiirfilter", 0, "Audio IIR Filter Base Class");

GST_BOILERPLATE_FULL (GstAudioFXBaseIIRFilter,
    gst_audio_fx_base_iir_filter, GstAudioFilter, GST_TYPE_AUDIO_FILTER,
    DEBUG_INIT);

static gboolean gst_audio_fx_base_iir_filter_setup (GstAudioFilter * filter,
    GstRingBufferSpec * format);
static GstFlowReturn
gst_audio_fx_base_iir_filter_transform_ip (GstBaseTransform * base,
    GstBuffer * buf);
static gboolean gst_audio_fx_base_iir_filter_stop (GstBaseTransform * base);

static void process_64 (GstAudioFXBaseIIRFilter * filter,
    gdouble * data, guint num_samples);
static void process_32 (GstAudioFXBaseIIRFilter * filter,
    gfloat * data, guint num_samples);

/* GObject vmethod implementations */

static void
gst_audio_fx_base_iir_filter_base_init (gpointer klass)
{
  GstCaps *caps;

  caps = gst_caps_from_string (ALLOWED_CAPS);
  gst_audio_filter_class_add_pad_templates (GST_AUDIO_FILTER_CLASS (klass),
      caps);
  gst_caps_unref (caps);
}

static void
gst_audio_fx_base_iir_filter_dispose (GObject * object)
{
  GstAudioFXBaseIIRFilter *filter = GST_AUDIO_FX_BASE_IIR_FILTER (object);

  if (filter->a) {
    g_free (filter->a);
    filter->a = NULL;
  }

  if (filter->b) {
    g_free (filter->b);
    filter->b = NULL;
  }

  if (filter->channels) {
    GstAudioFXBaseIIRFilterChannelCtx *ctx;
    guint i;

    for (i = 0; i < filter->nchannels; i++) {
      ctx = &filter->channels[i];
      g_free (ctx->x);
      g_free (ctx->y);
    }

    g_free (filter->channels);
    filter->channels = NULL;
  }

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_audio_fx_base_iir_filter_class_init (GstAudioFXBaseIIRFilterClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;
  GstBaseTransformClass *trans_class = (GstBaseTransformClass *) klass;
  GstAudioFilterClass *filter_class = (GstAudioFilterClass *) klass;

  gobject_class->dispose = gst_audio_fx_base_iir_filter_dispose;

  filter_class->setup = GST_DEBUG_FUNCPTR (gst_audio_fx_base_iir_filter_setup);

  trans_class->transform_ip =
      GST_DEBUG_FUNCPTR (gst_audio_fx_base_iir_filter_transform_ip);
  trans_class->stop = GST_DEBUG_FUNCPTR (gst_audio_fx_base_iir_filter_stop);
}

static void
gst_audio_fx_base_iir_filter_init (GstAudioFXBaseIIRFilter * filter,
    GstAudioFXBaseIIRFilterClass * klass)
{
  gst_base_transform_set_in_place (GST_BASE_TRANSFORM (filter), TRUE);

  filter->a = NULL;
  filter->na = 0;
  filter->b = NULL;
  filter->nb = 0;
  filter->channels = NULL;
  filter->nchannels = 0;
}

/* Evaluate the transfer function that corresponds to the IIR
 * coefficients at zr + zi*I and return the magnitude */
gdouble
gst_audio_fx_base_iir_filter_calculate_gain (gdouble * a, guint na, gdouble * b,
    guint nb, gdouble zr, gdouble zi)
{
  gdouble sum_ar, sum_ai;
  gdouble sum_br, sum_bi;
  gdouble gain_r, gain_i;

  gdouble sum_r_old;
  gdouble sum_i_old;

  gint i;

  sum_ar = 0.0;
  sum_ai = 0.0;
  for (i = na - 1; i >= 0; i--) {
    sum_r_old = sum_ar;
    sum_i_old = sum_ai;

    sum_ar = (sum_r_old * zr - sum_i_old * zi) + a[i];
    sum_ai = (sum_r_old * zi + sum_i_old * zr) + 0.0;
  }

  sum_br = 0.0;
  sum_bi = 0.0;
  for (i = nb - 1; i >= 0; i--) {
    sum_r_old = sum_br;
    sum_i_old = sum_bi;

    sum_br = (sum_r_old * zr - sum_i_old * zi) - b[i];
    sum_bi = (sum_r_old * zi + sum_i_old * zr) - 0.0;
  }
  sum_br += 1.0;
  sum_bi += 0.0;

  gain_r =
      (sum_ar * sum_br + sum_ai * sum_bi) / (sum_br * sum_br + sum_bi * sum_bi);
  gain_i =
      (sum_ai * sum_br - sum_ar * sum_bi) / (sum_br * sum_br + sum_bi * sum_bi);

  return (sqrt (gain_r * gain_r + gain_i * gain_i));
}

void
gst_audio_fx_base_iir_filter_set_coefficients (GstAudioFXBaseIIRFilter * filter,
    gdouble * a, guint na, gdouble * b, guint nb)
{
  guint i;

  g_return_if_fail (GST_IS_AUDIO_FX_BASE_IIR_FILTER (filter));

  GST_BASE_TRANSFORM_LOCK (filter);

  g_free (filter->a);
  g_free (filter->b);

  filter->a = filter->b = NULL;

  if (filter->channels) {
    GstAudioFXBaseIIRFilterChannelCtx *ctx;
    gboolean free = (na != filter->na || nb != filter->nb);

    for (i = 0; i < filter->nchannels; i++) {
      ctx = &filter->channels[i];

      if (free)
        g_free (ctx->x);
      else
        memset (ctx->x, 0, filter->na * sizeof (gdouble));

      if (free)
        g_free (ctx->y);
      else
        memset (ctx->y, 0, filter->nb * sizeof (gdouble));
    }

    g_free (filter->channels);
    filter->channels = NULL;
  }

  filter->na = na;
  filter->nb = nb;

  filter->a = a;
  filter->b = b;

  if (filter->nchannels && !filter->channels) {
    GstAudioFXBaseIIRFilterChannelCtx *ctx;

    filter->channels =
        g_new0 (GstAudioFXBaseIIRFilterChannelCtx, filter->nchannels);
    for (i = 0; i < filter->nchannels; i++) {
      ctx = &filter->channels[i];

      ctx->x = g_new0 (gdouble, filter->na);
      ctx->y = g_new0 (gdouble, filter->nb);
    }
  }

  GST_BASE_TRANSFORM_UNLOCK (filter);
}

/* GstAudioFilter vmethod implementations */

static gboolean
gst_audio_fx_base_iir_filter_setup (GstAudioFilter * base,
    GstRingBufferSpec * format)
{
  GstAudioFXBaseIIRFilter *filter = GST_AUDIO_FX_BASE_IIR_FILTER (base);
  gboolean ret = TRUE;

  if (format->width == 32)
    filter->process = (GstAudioFXBaseIIRFilterProcessFunc)
        process_32;
  else if (format->width == 64)
    filter->process = (GstAudioFXBaseIIRFilterProcessFunc)
        process_64;
  else
    ret = FALSE;

  if (format->channels != filter->nchannels) {
    guint i;
    GstAudioFXBaseIIRFilterChannelCtx *ctx;

    if (filter->channels) {

      for (i = 0; i < filter->nchannels; i++) {
        ctx = &filter->channels[i];

        g_free (ctx->x);
        g_free (ctx->y);
      }

      g_free (filter->channels);
      filter->channels = NULL;
    }

    filter->nchannels = format->channels;

    filter->channels =
        g_new0 (GstAudioFXBaseIIRFilterChannelCtx, filter->nchannels);
    for (i = 0; i < filter->nchannels; i++) {
      ctx = &filter->channels[i];

      ctx->x = g_new0 (gdouble, filter->na);
      ctx->y = g_new0 (gdouble, filter->nb);
    }
  }

  return ret;
}

static inline gdouble
process (GstAudioFXBaseIIRFilter * filter,
    GstAudioFXBaseIIRFilterChannelCtx * ctx, gdouble x0)
{
  gdouble val = filter->a[0] * x0;
  gint i, j;

  for (i = 1, j = ctx->x_pos; i < filter->na; i++) {
    val += filter->a[i] * ctx->x[j];
    j--;
    if (j < 0)
      j = filter->na - 1;
  }

  for (i = 1, j = ctx->y_pos; i < filter->nb; i++) {
    val += filter->b[i] * ctx->y[j];
    j--;
    if (j < 0)
      j = filter->nb - 1;
  }

  if (ctx->x) {
    ctx->x_pos++;
    if (ctx->x_pos >= filter->na)
      ctx->x_pos = 0;
    ctx->x[ctx->x_pos] = x0;
  }
  if (ctx->y) {
    ctx->y_pos++;
    if (ctx->y_pos >= filter->nb)
      ctx->y_pos = 0;

    ctx->y[ctx->y_pos] = val;
  }

  return val;
}

#define DEFINE_PROCESS_FUNC(width,ctype) \
static void \
process_##width (GstAudioFXBaseIIRFilter * filter, \
    g##ctype * data, guint num_samples) \
{ \
  gint i, j, channels = GST_AUDIO_FILTER (filter)->format.channels; \
  gdouble val; \
  \
  for (i = 0; i < num_samples / channels; i++) { \
    for (j = 0; j < channels; j++) { \
      val = process (filter, &filter->channels[j], *data); \
      *data++ = val; \
    } \
  } \
}

DEFINE_PROCESS_FUNC (32, float);
DEFINE_PROCESS_FUNC (64, double);

#undef DEFINE_PROCESS_FUNC

/* GstBaseTransform vmethod implementations */
static GstFlowReturn
gst_audio_fx_base_iir_filter_transform_ip (GstBaseTransform * base,
    GstBuffer * buf)
{
  GstAudioFXBaseIIRFilter *filter = GST_AUDIO_FX_BASE_IIR_FILTER (base);
  guint num_samples;
  GstClockTime timestamp, stream_time;

  timestamp = GST_BUFFER_TIMESTAMP (buf);
  stream_time =
      gst_segment_to_stream_time (&base->segment, GST_FORMAT_TIME, timestamp);

  GST_DEBUG_OBJECT (filter, "sync to %" GST_TIME_FORMAT,
      GST_TIME_ARGS (timestamp));

  if (GST_CLOCK_TIME_IS_VALID (stream_time))
    gst_object_sync_values (G_OBJECT (filter), stream_time);

  num_samples =
      GST_BUFFER_SIZE (buf) / (GST_AUDIO_FILTER (filter)->format.width / 8);

  if (gst_base_transform_is_passthrough (base))
    return GST_FLOW_OK;

  g_return_val_if_fail (filter->a != NULL, GST_FLOW_ERROR);

  filter->process (filter, GST_BUFFER_DATA (buf), num_samples);

  return GST_FLOW_OK;
}


static gboolean
gst_audio_fx_base_iir_filter_stop (GstBaseTransform * base)
{
  GstAudioFXBaseIIRFilter *filter = GST_AUDIO_FX_BASE_IIR_FILTER (base);
  guint channels = GST_AUDIO_FILTER (filter)->format.channels;
  GstAudioFXBaseIIRFilterChannelCtx *ctx;
  guint i;

  /* Reset the history of input and output values if
   * already existing */
  if (channels && filter->channels) {
    for (i = 0; i < channels; i++) {
      ctx = &filter->channels[i];
      g_free (ctx->x);
      g_free (ctx->y);
    }
    g_free (filter->channels);
  }
  filter->channels = NULL;

  return TRUE;
}
