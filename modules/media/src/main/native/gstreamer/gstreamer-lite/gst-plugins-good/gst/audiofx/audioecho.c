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
 */

/**
 * SECTION:element-audioecho
 * @Since: 0.10.14
 *
 * audioecho adds an echo or (simple) reverb effect to an audio stream. The echo
 * delay, intensity and the percentage of feedback can be configured.
 *
 * For getting an echo effect you have to set the delay to a larger value,
 * for example 200ms and more. Everything below will result in a simple
 * reverb effect, which results in a slightly metallic sound.
 *
 * Use the max-delay property to set the maximum amount of delay that
 * will be used. This can only be set before going to the PAUSED or PLAYING
 * state and will be set to the current delay by default.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch filesrc location="melo1.ogg" ! audioconvert ! audioecho delay=500000000 intensity=0.6 feedback=0.4 ! audioconvert ! autoaudiosink
 * gst-launch filesrc location="melo1.ogg" ! decodebin ! audioconvert ! audioecho delay=50000000 intensity=0.6 feedback=0.4 ! audioconvert ! autoaudiosink
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>
#include <gst/audio/audio.h>
#include <gst/audio/gstaudiofilter.h>
#include <gst/controller/gstcontroller.h>

#include "audioecho.h"

#define GST_CAT_DEFAULT gst_audio_echo_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

enum
{
  PROP_0,
  PROP_DELAY,
  PROP_MAX_DELAY,
  PROP_INTENSITY,
  PROP_FEEDBACK
};

#define ALLOWED_CAPS \
    "audio/x-raw-float,"                                              \
    " width=(int) { 32, 64 }, "                                       \
    " endianness=(int)BYTE_ORDER,"                                    \
    " rate=(int)[1,MAX],"                                             \
    " channels=(int)[1,MAX]"

#define DEBUG_INIT(bla) \
  GST_DEBUG_CATEGORY_INIT (gst_audio_echo_debug, "audioecho", 0, "audioecho element");

GST_BOILERPLATE_FULL (GstAudioEcho, gst_audio_echo, GstAudioFilter,
    GST_TYPE_AUDIO_FILTER, DEBUG_INIT);

static void gst_audio_echo_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_audio_echo_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);
static void gst_audio_echo_finalize (GObject * object);

static gboolean gst_audio_echo_setup (GstAudioFilter * self,
    GstRingBufferSpec * format);
static gboolean gst_audio_echo_stop (GstBaseTransform * base);
static GstFlowReturn gst_audio_echo_transform_ip (GstBaseTransform * base,
    GstBuffer * buf);

static void gst_audio_echo_transform_float (GstAudioEcho * self,
    gfloat * data, guint num_samples);
static void gst_audio_echo_transform_double (GstAudioEcho * self,
    gdouble * data, guint num_samples);

/* GObject vmethod implementations */

static void
gst_audio_echo_base_init (gpointer klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GstCaps *caps;

  gst_element_class_set_details_simple (element_class, "Audio echo",
      "Filter/Effect/Audio",
      "Adds an echo or reverb effect to an audio stream",
      "Sebastian Dröge <sebastian.droege@collabora.co.uk>");

  caps = gst_caps_from_string (ALLOWED_CAPS);
  gst_audio_filter_class_add_pad_templates (GST_AUDIO_FILTER_CLASS (klass),
      caps);
  gst_caps_unref (caps);
}

static void
gst_audio_echo_class_init (GstAudioEchoClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;
  GstBaseTransformClass *basetransform_class = (GstBaseTransformClass *) klass;
  GstAudioFilterClass *audioself_class = (GstAudioFilterClass *) klass;

  gobject_class->set_property = gst_audio_echo_set_property;
  gobject_class->get_property = gst_audio_echo_get_property;
  gobject_class->finalize = gst_audio_echo_finalize;

  g_object_class_install_property (gobject_class, PROP_DELAY,
      g_param_spec_uint64 ("delay", "Delay",
          "Delay of the echo in nanoseconds", 1, G_MAXUINT64,
          1, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS
          | GST_PARAM_CONTROLLABLE));

  g_object_class_install_property (gobject_class, PROP_MAX_DELAY,
      g_param_spec_uint64 ("max-delay", "Maximum Delay",
          "Maximum delay of the echo in nanoseconds"
          " (can't be changed in PLAYING or PAUSED state)",
          1, G_MAXUINT64, 1,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | GST_PARAM_CONTROLLABLE));

  g_object_class_install_property (gobject_class, PROP_INTENSITY,
      g_param_spec_float ("intensity", "Intensity",
          "Intensity of the echo", 0.0, 1.0,
          0.0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS
          | GST_PARAM_CONTROLLABLE));

  g_object_class_install_property (gobject_class, PROP_FEEDBACK,
      g_param_spec_float ("feedback", "Feedback",
          "Amount of feedback", 0.0, 1.0,
          0.0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS
          | GST_PARAM_CONTROLLABLE));

  audioself_class->setup = GST_DEBUG_FUNCPTR (gst_audio_echo_setup);
  basetransform_class->transform_ip =
      GST_DEBUG_FUNCPTR (gst_audio_echo_transform_ip);
  basetransform_class->stop = GST_DEBUG_FUNCPTR (gst_audio_echo_stop);
}

static void
gst_audio_echo_init (GstAudioEcho * self, GstAudioEchoClass * klass)
{
  self->delay = 1;
  self->max_delay = 1;
  self->intensity = 0.0;
  self->feedback = 0.0;

  gst_base_transform_set_in_place (GST_BASE_TRANSFORM (self), TRUE);
}

static void
gst_audio_echo_finalize (GObject * object)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (object);

  g_free (self->buffer);
  self->buffer = NULL;

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
gst_audio_echo_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (object);

  switch (prop_id) {
    case PROP_DELAY:{
      guint64 max_delay, delay;

      GST_BASE_TRANSFORM_LOCK (self);
      delay = g_value_get_uint64 (value);
      max_delay = self->max_delay;

      if (delay > max_delay && GST_STATE (self) > GST_STATE_READY) {
        GST_WARNING_OBJECT (self, "New delay (%" GST_TIME_FORMAT ") "
            "is larger than maximum delay (%" GST_TIME_FORMAT ")",
            GST_TIME_ARGS (delay), GST_TIME_ARGS (max_delay));
        self->delay = max_delay;
      } else {
        self->delay = delay;
        self->max_delay = MAX (delay, max_delay);
      }
      GST_BASE_TRANSFORM_UNLOCK (self);
    }
      break;
    case PROP_MAX_DELAY:{
      guint64 max_delay, delay;

      GST_BASE_TRANSFORM_LOCK (self);
      max_delay = g_value_get_uint64 (value);
      delay = self->delay;

      if (GST_STATE (self) > GST_STATE_READY) {
        GST_ERROR_OBJECT (self, "Can't change maximum delay in"
            " PLAYING or PAUSED state");
      } else {
        self->delay = delay;
        self->max_delay = max_delay;
      }
      GST_BASE_TRANSFORM_UNLOCK (self);
    }
      break;
    case PROP_INTENSITY:{
      GST_BASE_TRANSFORM_LOCK (self);
      self->intensity = g_value_get_float (value);
      GST_BASE_TRANSFORM_UNLOCK (self);
    }
      break;
    case PROP_FEEDBACK:{
      GST_BASE_TRANSFORM_LOCK (self);
      self->feedback = g_value_get_float (value);
      GST_BASE_TRANSFORM_UNLOCK (self);
    }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_audio_echo_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (object);

  switch (prop_id) {
    case PROP_DELAY:
      GST_BASE_TRANSFORM_LOCK (self);
      g_value_set_uint64 (value, self->delay);
      GST_BASE_TRANSFORM_UNLOCK (self);
      break;
    case PROP_MAX_DELAY:
      GST_BASE_TRANSFORM_LOCK (self);
      g_value_set_uint64 (value, self->max_delay);
      GST_BASE_TRANSFORM_UNLOCK (self);
      break;
    case PROP_INTENSITY:
      GST_BASE_TRANSFORM_LOCK (self);
      g_value_set_float (value, self->intensity);
      GST_BASE_TRANSFORM_UNLOCK (self);
      break;
    case PROP_FEEDBACK:
      GST_BASE_TRANSFORM_LOCK (self);
      g_value_set_float (value, self->feedback);
      GST_BASE_TRANSFORM_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* GstAudioFilter vmethod implementations */

static gboolean
gst_audio_echo_setup (GstAudioFilter * base, GstRingBufferSpec * format)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (base);
  gboolean ret = TRUE;

  if (format->type == GST_BUFTYPE_FLOAT && format->width == 32)
    self->process = (GstAudioEchoProcessFunc)
        gst_audio_echo_transform_float;
  else if (format->type == GST_BUFTYPE_FLOAT && format->width == 64)
    self->process = (GstAudioEchoProcessFunc)
        gst_audio_echo_transform_double;
  else
    ret = FALSE;

  g_free (self->buffer);
  self->buffer = NULL;
  self->buffer_pos = 0;
  self->buffer_size = 0;
  self->buffer_size_frames = 0;

  return ret;
}

static gboolean
gst_audio_echo_stop (GstBaseTransform * base)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (base);

  g_free (self->buffer);
  self->buffer = NULL;
  self->buffer_pos = 0;
  self->buffer_size = 0;
  self->buffer_size_frames = 0;

  return TRUE;
}

#define TRANSFORM_FUNC(name, type) \
static void \
gst_audio_echo_transform_##name (GstAudioEcho * self, \
    type * data, guint num_samples) \
{ \
  type *buffer = (type *) self->buffer; \
  guint channels = GST_AUDIO_FILTER (self)->format.channels; \
  guint rate = GST_AUDIO_FILTER (self)->format.rate; \
  guint i, j; \
  guint echo_index = self->buffer_size_frames - self->delay_frames; \
  gdouble echo_off = ((((gdouble) self->delay) * rate) / GST_SECOND) - self->delay_frames; \
  \
  if (echo_off < 0.0) \
    echo_off = 0.0; \
  \
  num_samples /= channels; \
  \
  for (i = 0; i < num_samples; i++) { \
    guint echo0_index = ((echo_index + self->buffer_pos) % self->buffer_size_frames) * channels; \
    guint echo1_index = ((echo_index + self->buffer_pos +1) % self->buffer_size_frames) * channels; \
    guint rbout_index = (self->buffer_pos % self->buffer_size_frames) * channels; \
    for (j = 0; j < channels; j++) { \
      gdouble in = data[i*channels + j]; \
      gdouble echo0 = buffer[echo0_index + j]; \
      gdouble echo1 = buffer[echo1_index + j]; \
      gdouble echo = echo0 + (echo1-echo0)*echo_off; \
      type out = in + self->intensity * echo; \
      \
      data[i*channels + j] = out; \
      \
      buffer[rbout_index + j] = in + self->feedback * echo; \
    } \
    self->buffer_pos = (self->buffer_pos + 1) % self->buffer_size_frames; \
  } \
}

TRANSFORM_FUNC (float, gfloat);
TRANSFORM_FUNC (double, gdouble);

/* GstBaseTransform vmethod implementations */
static GstFlowReturn
gst_audio_echo_transform_ip (GstBaseTransform * base, GstBuffer * buf)
{
  GstAudioEcho *self = GST_AUDIO_ECHO (base);
  guint num_samples;
  GstClockTime timestamp, stream_time;

  timestamp = GST_BUFFER_TIMESTAMP (buf);
  stream_time =
      gst_segment_to_stream_time (&base->segment, GST_FORMAT_TIME, timestamp);

  GST_DEBUG_OBJECT (self, "sync to %" GST_TIME_FORMAT,
      GST_TIME_ARGS (timestamp));

  if (GST_CLOCK_TIME_IS_VALID (stream_time))
    gst_object_sync_values (G_OBJECT (self), stream_time);

  num_samples =
      GST_BUFFER_SIZE (buf) / (GST_AUDIO_FILTER (self)->format.width / 8);

  if (self->buffer == NULL) {
    guint width, rate, channels;

    width = GST_AUDIO_FILTER (self)->format.width / 8;
    rate = GST_AUDIO_FILTER (self)->format.rate;
    channels = GST_AUDIO_FILTER (self)->format.channels;

    self->delay_frames =
        MAX (gst_util_uint64_scale (self->delay, rate, GST_SECOND), 1);
    self->buffer_size_frames =
        MAX (gst_util_uint64_scale (self->max_delay, rate, GST_SECOND), 1);

    self->buffer_size = self->buffer_size_frames * width * channels;
    self->buffer = g_try_malloc0 (self->buffer_size);
    self->buffer_pos = 0;

    if (self->buffer == NULL) {
      GST_ERROR_OBJECT (self, "Failed to allocate %u bytes", self->buffer_size);
      return GST_FLOW_ERROR;
    }
  }

  self->process (self, GST_BUFFER_DATA (buf), num_samples);

  return GST_FLOW_OK;
}
