/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim@fluendo.com>
 * Copyright (C) 2006 Tim-Philipp Müller <tim centricular net>
 *
 * gstalsasink.c:
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
 * SECTION:element-alsasink
 * @see_also: alsasrc, alsamixer
 *
 * This element renders raw audio samples using the ALSA api.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch -v filesrc location=sine.ogg ! oggdemux ! vorbisdec ! audioconvert ! audioresample ! alsasink
 * ]| Play an Ogg/Vorbis file.
 * </refsect2>
 *
 * Last reviewed on 2006-03-01 (0.10.4)
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include <sys/ioctl.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <getopt.h>
#include <alsa/asoundlib.h>

#include "gstalsa.h"
#include "gstalsasink.h"
#include "gstalsadeviceprobe.h"

#include <gst/gst-i18n-plugin.h>

#define DEFAULT_DEVICE		"default"
#define DEFAULT_DEVICE_NAME	""
#define DEFAULT_CARD_NAME	""
#define SPDIF_PERIOD_SIZE 1536
#define SPDIF_BUFFER_SIZE 15360

enum
{
  PROP_0,
  PROP_DEVICE,
  PROP_DEVICE_NAME,
  PROP_CARD_NAME,
  PROP_LAST
};

static void gst_alsasink_init_interfaces (GType type);

GST_BOILERPLATE_FULL (GstAlsaSink, gst_alsasink, GstAudioSink,
    GST_TYPE_AUDIO_SINK, gst_alsasink_init_interfaces);

static void gst_alsasink_finalise (GObject * object);
static void gst_alsasink_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_alsasink_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstCaps *gst_alsasink_getcaps (GstBaseSink * bsink);

static gboolean gst_alsasink_open (GstAudioSink * asink);
static gboolean gst_alsasink_prepare (GstAudioSink * asink,
    GstRingBufferSpec * spec);
static gboolean gst_alsasink_unprepare (GstAudioSink * asink);
static gboolean gst_alsasink_close (GstAudioSink * asink);
static guint gst_alsasink_write (GstAudioSink * asink, gpointer data,
    guint length);
static guint gst_alsasink_delay (GstAudioSink * asink);
static void gst_alsasink_reset (GstAudioSink * asink);

static gint output_ref;         /* 0    */
static snd_output_t *output;    /* NULL */
static GStaticMutex output_mutex = G_STATIC_MUTEX_INIT;


#if (G_BYTE_ORDER == G_LITTLE_ENDIAN)
# define ALSA_SINK_FACTORY_ENDIANNESS	"LITTLE_ENDIAN, BIG_ENDIAN"
#else
# define ALSA_SINK_FACTORY_ENDIANNESS	"BIG_ENDIAN, LITTLE_ENDIAN"
#endif

static GstStaticPadTemplate alsasink_sink_factory =
    GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-int, "
        "endianness = (int) { " ALSA_SINK_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 32, "
        "depth = (int) 32, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SINK_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 24, "
        "depth = (int) 24, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SINK_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 32, "
        "depth = (int) 24, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SINK_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 16, "
        "depth = (int) 16, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 8, "
        "depth = (int) 8, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ];"
        "audio/x-iec958")
    );

static void
gst_alsasink_finalise (GObject * object)
{
  GstAlsaSink *sink = GST_ALSA_SINK (object);

  g_free (sink->device);
  g_mutex_free (sink->alsa_lock);

  g_static_mutex_lock (&output_mutex);
  --output_ref;
  if (output_ref == 0) {
    snd_output_close (output);
    output = NULL;
  }
  g_static_mutex_unlock (&output_mutex);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
gst_alsasink_init_interfaces (GType type)
{
  gst_alsa_type_add_device_property_probe_interface (type);
}

static void
gst_alsasink_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (element_class,
      "Audio sink (ALSA)", "Sink/Audio",
      "Output to a sound card via ALSA", "Wim Taymans <wim@fluendo.com>");

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&alsasink_sink_factory));
}

static void
gst_alsasink_class_init (GstAlsaSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseSinkClass *gstbasesink_class;
  GstAudioSinkClass *gstaudiosink_class;

  gobject_class = (GObjectClass *) klass;
  gstbasesink_class = (GstBaseSinkClass *) klass;
  gstaudiosink_class = (GstAudioSinkClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->finalize = gst_alsasink_finalise;
  gobject_class->get_property = gst_alsasink_get_property;
  gobject_class->set_property = gst_alsasink_set_property;

  gstbasesink_class->get_caps = GST_DEBUG_FUNCPTR (gst_alsasink_getcaps);

  gstaudiosink_class->open = GST_DEBUG_FUNCPTR (gst_alsasink_open);
  gstaudiosink_class->prepare = GST_DEBUG_FUNCPTR (gst_alsasink_prepare);
  gstaudiosink_class->unprepare = GST_DEBUG_FUNCPTR (gst_alsasink_unprepare);
  gstaudiosink_class->close = GST_DEBUG_FUNCPTR (gst_alsasink_close);
  gstaudiosink_class->write = GST_DEBUG_FUNCPTR (gst_alsasink_write);
  gstaudiosink_class->delay = GST_DEBUG_FUNCPTR (gst_alsasink_delay);
  gstaudiosink_class->reset = GST_DEBUG_FUNCPTR (gst_alsasink_reset);

  g_object_class_install_property (gobject_class, PROP_DEVICE,
      g_param_spec_string ("device", "Device",
          "ALSA device, as defined in an asound configuration file",
          DEFAULT_DEVICE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_DEVICE_NAME,
      g_param_spec_string ("device-name", "Device name",
          "Human-readable name of the sound device", DEFAULT_DEVICE_NAME,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_CARD_NAME,
      g_param_spec_string ("card-name", "Card name",
          "Human-readable name of the sound card", DEFAULT_CARD_NAME,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
}

static void
gst_alsasink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAlsaSink *sink;

  sink = GST_ALSA_SINK (object);

  switch (prop_id) {
    case PROP_DEVICE:
      g_free (sink->device);
      sink->device = g_value_dup_string (value);
      /* setting NULL restores the default device */
      if (sink->device == NULL) {
        sink->device = g_strdup (DEFAULT_DEVICE);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_alsasink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAlsaSink *sink;

  sink = GST_ALSA_SINK (object);

  switch (prop_id) {
    case PROP_DEVICE:
      g_value_set_string (value, sink->device);
      break;
    case PROP_DEVICE_NAME:
      g_value_take_string (value,
          gst_alsa_find_device_name (GST_OBJECT_CAST (sink),
              sink->device, sink->handle, SND_PCM_STREAM_PLAYBACK));
      break;
    case PROP_CARD_NAME:
      g_value_take_string (value,
          gst_alsa_find_card_name (GST_OBJECT_CAST (sink),
              sink->device, SND_PCM_STREAM_PLAYBACK));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_alsasink_init (GstAlsaSink * alsasink, GstAlsaSinkClass * g_class)
{
  GST_DEBUG_OBJECT (alsasink, "initializing alsasink");

  alsasink->device = g_strdup (DEFAULT_DEVICE);
  alsasink->handle = NULL;
  alsasink->cached_caps = NULL;
  alsasink->alsa_lock = g_mutex_new ();

  g_static_mutex_lock (&output_mutex);
  if (output_ref == 0) {
    snd_output_stdio_attach (&output, stdout, 0);
    ++output_ref;
  }
  g_static_mutex_unlock (&output_mutex);
}

#define CHECK(call, error) \
G_STMT_START {                  \
if ((err = call) < 0)           \
  goto error;                   \
} G_STMT_END;

static GstCaps *
gst_alsasink_getcaps (GstBaseSink * bsink)
{
  GstElementClass *element_class;
  GstPadTemplate *pad_template;
  GstAlsaSink *sink = GST_ALSA_SINK (bsink);
  GstCaps *caps;

  if (sink->handle == NULL) {
    GST_DEBUG_OBJECT (sink, "device not open, using template caps");
    return NULL;                /* base class will get template caps for us */
  }

  if (sink->cached_caps) {
    GST_LOG_OBJECT (sink, "Returning cached caps");
    return gst_caps_ref (sink->cached_caps);
  }

  element_class = GST_ELEMENT_GET_CLASS (sink);
  pad_template = gst_element_class_get_pad_template (element_class, "sink");
  g_return_val_if_fail (pad_template != NULL, NULL);

  caps = gst_alsa_probe_supported_formats (GST_OBJECT (sink), sink->handle,
      gst_pad_template_get_caps (pad_template));

  if (caps) {
    sink->cached_caps = gst_caps_ref (caps);
  }

  GST_INFO_OBJECT (sink, "returning caps %" GST_PTR_FORMAT, caps);

  return caps;
}

static int
set_hwparams (GstAlsaSink * alsa)
{
  guint rrate;
  gint err;
  snd_pcm_hw_params_t *params;
  guint period_time, buffer_time;

  snd_pcm_hw_params_malloc (&params);

  GST_DEBUG_OBJECT (alsa, "Negotiating to %d channels @ %d Hz (format = %s) "
      "SPDIF (%d)", alsa->channels, alsa->rate,
      snd_pcm_format_name (alsa->format), alsa->iec958);

  /* start with requested values, if we cannot configure alsa for those values,
   * we set these values to -1, which will leave the default alsa values */
  buffer_time = alsa->buffer_time;
  period_time = alsa->period_time;

retry:
  /* choose all parameters */
  CHECK (snd_pcm_hw_params_any (alsa->handle, params), no_config);
  /* set the interleaved read/write format */
  CHECK (snd_pcm_hw_params_set_access (alsa->handle, params, alsa->access),
      wrong_access);
  /* set the sample format */
  if (alsa->iec958) {
    /* Try to use big endian first else fallback to le and swap bytes */
    if (snd_pcm_hw_params_set_format (alsa->handle, params, alsa->format) < 0) {
      alsa->format = SND_PCM_FORMAT_S16_LE;
      alsa->need_swap = TRUE;
      GST_DEBUG_OBJECT (alsa, "falling back to little endian with swapping");
    } else {
      alsa->need_swap = FALSE;
    }
  }
  CHECK (snd_pcm_hw_params_set_format (alsa->handle, params, alsa->format),
      no_sample_format);
  /* set the count of channels */
  CHECK (snd_pcm_hw_params_set_channels (alsa->handle, params, alsa->channels),
      no_channels);
  /* set the stream rate */
  rrate = alsa->rate;
  CHECK (snd_pcm_hw_params_set_rate_near (alsa->handle, params, &rrate, NULL),
      no_rate);
  if (rrate != alsa->rate)
    goto rate_match;

#ifndef GST_DISABLE_GST_DEBUG
  /* get and dump some limits */
  {
    guint min, max;

    snd_pcm_hw_params_get_buffer_time_min (params, &min, NULL);
    snd_pcm_hw_params_get_buffer_time_max (params, &max, NULL);

    GST_DEBUG_OBJECT (alsa, "buffer time %u, min %u, max %u",
        alsa->buffer_time, min, max);

    snd_pcm_hw_params_get_period_time_min (params, &min, NULL);
    snd_pcm_hw_params_get_period_time_max (params, &max, NULL);

    GST_DEBUG_OBJECT (alsa, "period time %u, min %u, max %u",
        alsa->period_time, min, max);

    snd_pcm_hw_params_get_periods_min (params, &min, NULL);
    snd_pcm_hw_params_get_periods_max (params, &max, NULL);

    GST_DEBUG_OBJECT (alsa, "periods min %u, max %u", min, max);
  }
#endif

  /* now try to configure the buffer time and period time, if one
   * of those fail, we fall back to the defaults and emit a warning. */
  if (buffer_time != -1 && !alsa->iec958) {
    /* set the buffer time */
    if ((err = snd_pcm_hw_params_set_buffer_time_near (alsa->handle, params,
                &buffer_time, NULL)) < 0) {
      GST_ELEMENT_WARNING (alsa, RESOURCE, SETTINGS, (NULL),
          ("Unable to set buffer time %i for playback: %s",
              buffer_time, snd_strerror (err)));
      /* disable buffer_time the next round */
      buffer_time = -1;
      goto retry;
    }
    GST_DEBUG_OBJECT (alsa, "buffer time %u", buffer_time);
  }
  if (period_time != -1 && !alsa->iec958) {
    /* set the period time */
    if ((err = snd_pcm_hw_params_set_period_time_near (alsa->handle, params,
                &period_time, NULL)) < 0) {
      GST_ELEMENT_WARNING (alsa, RESOURCE, SETTINGS, (NULL),
          ("Unable to set period time %i for playback: %s",
              period_time, snd_strerror (err)));
      /* disable period_time the next round */
      period_time = -1;
      goto retry;
    }
    GST_DEBUG_OBJECT (alsa, "period time %u", period_time);
  }

  /* Set buffer size and period size manually for SPDIF */
  if (G_UNLIKELY (alsa->iec958)) {
    snd_pcm_uframes_t buffer_size = SPDIF_BUFFER_SIZE;
    snd_pcm_uframes_t period_size = SPDIF_PERIOD_SIZE;

    CHECK (snd_pcm_hw_params_set_buffer_size_near (alsa->handle, params,
            &buffer_size), buffer_size);
    CHECK (snd_pcm_hw_params_set_period_size_near (alsa->handle, params,
            &period_size, NULL), period_size);
  }

  /* write the parameters to device */
  CHECK (snd_pcm_hw_params (alsa->handle, params), set_hw_params);

  /* now get the configured values */
  CHECK (snd_pcm_hw_params_get_buffer_size (params, &alsa->buffer_size),
      buffer_size);
  CHECK (snd_pcm_hw_params_get_period_size (params, &alsa->period_size, NULL),
      period_size);

  GST_DEBUG_OBJECT (alsa, "buffer size %lu, period size %lu", alsa->buffer_size,
      alsa->period_size);

  snd_pcm_hw_params_free (params);
  return 0;

  /* ERRORS */
no_config:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Broken configuration for playback: no configurations available: %s",
            snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
wrong_access:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Access type not available for playback: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
no_sample_format:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Sample format not available for playback: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
no_channels:
  {
    gchar *msg = NULL;

    if ((alsa->channels) == 1)
      msg = g_strdup (_("Could not open device for playback in mono mode."));
    if ((alsa->channels) == 2)
      msg = g_strdup (_("Could not open device for playback in stereo mode."));
    if ((alsa->channels) > 2)
      msg =
          g_strdup_printf (_
          ("Could not open device for playback in %d-channel mode."),
          alsa->channels);
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, ("%s", msg),
        ("%s", snd_strerror (err)));
    g_free (msg);
    snd_pcm_hw_params_free (params);
    return err;
  }
no_rate:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Rate %iHz not available for playback: %s",
            alsa->rate, snd_strerror (err)));
    return err;
  }
rate_match:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Rate doesn't match (requested %iHz, get %iHz)", alsa->rate, err));
    snd_pcm_hw_params_free (params);
    return -EINVAL;
  }
buffer_size:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to get buffer size for playback: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
period_size:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to get period size for playback: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
set_hw_params:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set hw params for playback: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
}

static int
set_swparams (GstAlsaSink * alsa)
{
  int err;
  snd_pcm_sw_params_t *params;

  snd_pcm_sw_params_malloc (&params);

  /* get the current swparams */
  CHECK (snd_pcm_sw_params_current (alsa->handle, params), no_config);
  /* start the transfer when the buffer is almost full: */
  /* (buffer_size / avail_min) * avail_min */
  CHECK (snd_pcm_sw_params_set_start_threshold (alsa->handle, params,
          (alsa->buffer_size / alsa->period_size) * alsa->period_size),
      start_threshold);

  /* allow the transfer when at least period_size samples can be processed */
  CHECK (snd_pcm_sw_params_set_avail_min (alsa->handle, params,
          alsa->period_size), set_avail);

#if GST_CHECK_ALSA_VERSION(1,0,16)
  /* snd_pcm_sw_params_set_xfer_align() is deprecated, alignment is always 1 */
#else
  /* align all transfers to 1 sample */
  CHECK (snd_pcm_sw_params_set_xfer_align (alsa->handle, params, 1), set_align);
#endif

  /* write the parameters to the playback device */
  CHECK (snd_pcm_sw_params (alsa->handle, params), set_sw_params);

  snd_pcm_sw_params_free (params);
  return 0;

  /* ERRORS */
no_config:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to determine current swparams for playback: %s",
            snd_strerror (err)));
    snd_pcm_sw_params_free (params);
    return err;
  }
start_threshold:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set start threshold mode for playback: %s",
            snd_strerror (err)));
    snd_pcm_sw_params_free (params);
    return err;
  }
set_avail:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set avail min for playback: %s", snd_strerror (err)));
    snd_pcm_sw_params_free (params);
    return err;
  }
#if !GST_CHECK_ALSA_VERSION(1,0,16)
set_align:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set transfer align for playback: %s", snd_strerror (err)));
    snd_pcm_sw_params_free (params);
    return err;
  }
#endif
set_sw_params:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set sw params for playback: %s", snd_strerror (err)));
    snd_pcm_sw_params_free (params);
    return err;
  }
}

static gboolean
alsasink_parse_spec (GstAlsaSink * alsa, GstRingBufferSpec * spec)
{
  /* Initialize our boolean */
  alsa->iec958 = FALSE;

  switch (spec->type) {
    case GST_BUFTYPE_LINEAR:
      GST_DEBUG_OBJECT (alsa,
          "Linear format : depth=%d, width=%d, sign=%d, bigend=%d", spec->depth,
          spec->width, spec->sign, spec->bigend);

      alsa->format = snd_pcm_build_linear_format (spec->depth, spec->width,
          spec->sign ? 0 : 1, spec->bigend ? 1 : 0);
      break;
    case GST_BUFTYPE_FLOAT:
      switch (spec->format) {
        case GST_FLOAT32_LE:
          alsa->format = SND_PCM_FORMAT_FLOAT_LE;
          break;
        case GST_FLOAT32_BE:
          alsa->format = SND_PCM_FORMAT_FLOAT_BE;
          break;
        case GST_FLOAT64_LE:
          alsa->format = SND_PCM_FORMAT_FLOAT64_LE;
          break;
        case GST_FLOAT64_BE:
          alsa->format = SND_PCM_FORMAT_FLOAT64_BE;
          break;
        default:
          goto error;
      }
      break;
    case GST_BUFTYPE_A_LAW:
      alsa->format = SND_PCM_FORMAT_A_LAW;
      break;
    case GST_BUFTYPE_MU_LAW:
      alsa->format = SND_PCM_FORMAT_MU_LAW;
      break;
    case GST_BUFTYPE_IEC958:
      alsa->format = SND_PCM_FORMAT_S16_BE;
      alsa->iec958 = TRUE;
      break;
    default:
      goto error;

  }
  alsa->rate = spec->rate;
  alsa->channels = spec->channels;
  alsa->buffer_time = spec->buffer_time;
  alsa->period_time = spec->latency_time;
  alsa->access = SND_PCM_ACCESS_RW_INTERLEAVED;

  return TRUE;

  /* ERRORS */
error:
  {
    return FALSE;
  }
}

static gboolean
gst_alsasink_open (GstAudioSink * asink)
{
  GstAlsaSink *alsa;
  gint err;

  alsa = GST_ALSA_SINK (asink);

  /* open in non-blocking mode, we'll use snd_pcm_wait() for space to become
   * available. */
  CHECK (snd_pcm_open (&alsa->handle, alsa->device, SND_PCM_STREAM_PLAYBACK,
          SND_PCM_NONBLOCK), open_error);
  GST_LOG_OBJECT (alsa, "Opened device %s", alsa->device);

  return TRUE;

  /* ERRORS */
open_error:
  {
    if (err == -EBUSY) {
      GST_ELEMENT_ERROR (alsa, RESOURCE, BUSY,
          (_("Could not open audio device for playback. "
                  "Device is being used by another application.")),
          ("Device '%s' is busy", alsa->device));
    } else {
      GST_ELEMENT_ERROR (alsa, RESOURCE, OPEN_WRITE,
          (_("Could not open audio device for playback.")),
          ("Playback open error on device '%s': %s", alsa->device,
              snd_strerror (err)));
    }
    return FALSE;
  }
}

static gboolean
gst_alsasink_prepare (GstAudioSink * asink, GstRingBufferSpec * spec)
{
  GstAlsaSink *alsa;
  gint err;

  alsa = GST_ALSA_SINK (asink);

  if (spec->format == GST_IEC958) {
    snd_pcm_close (alsa->handle);
    alsa->handle = gst_alsa_open_iec958_pcm (GST_OBJECT (alsa));
    if (G_UNLIKELY (!alsa->handle)) {
      goto no_iec958;
    }
  }

  if (!alsasink_parse_spec (alsa, spec))
    goto spec_parse;

  CHECK (set_hwparams (alsa), hw_params_failed);
  CHECK (set_swparams (alsa), sw_params_failed);

  alsa->bytes_per_sample = spec->bytes_per_sample;
  spec->segsize = alsa->period_size * spec->bytes_per_sample;
  spec->segtotal = alsa->buffer_size / alsa->period_size;

  {
    snd_output_t *out_buf = NULL;
    char *msg = NULL;

    snd_output_buffer_open (&out_buf);
    snd_pcm_dump_hw_setup (alsa->handle, out_buf);
    snd_output_buffer_string (out_buf, &msg);
    GST_DEBUG_OBJECT (alsa, "Hardware setup: \n%s", msg);
    snd_output_close (out_buf);
    snd_output_buffer_open (&out_buf);
    snd_pcm_dump_sw_setup (alsa->handle, out_buf);
    snd_output_buffer_string (out_buf, &msg);
    GST_DEBUG_OBJECT (alsa, "Software setup: \n%s", msg);
    snd_output_close (out_buf);
  }

  return TRUE;

  /* ERRORS */
no_iec958:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, OPEN_WRITE, (NULL),
        ("Could not open IEC958 (SPDIF) device for playback"));
    return FALSE;
  }
spec_parse:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Error parsing spec"));
    return FALSE;
  }
hw_params_failed:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Setting of hwparams failed: %s", snd_strerror (err)));
    return FALSE;
  }
sw_params_failed:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Setting of swparams failed: %s", snd_strerror (err)));
    return FALSE;
  }
}

static gboolean
gst_alsasink_unprepare (GstAudioSink * asink)
{
  GstAlsaSink *alsa;

  alsa = GST_ALSA_SINK (asink);

  snd_pcm_drop (alsa->handle);
  snd_pcm_hw_free (alsa->handle);

  return TRUE;
}

static gboolean
gst_alsasink_close (GstAudioSink * asink)
{
  GstAlsaSink *alsa = GST_ALSA_SINK (asink);

  if (alsa->handle) {
    snd_pcm_close (alsa->handle);
    alsa->handle = NULL;
  }
  gst_caps_replace (&alsa->cached_caps, NULL);

  return TRUE;
}


/*
 *   Underrun and suspend recovery
 */
static gint
xrun_recovery (GstAlsaSink * alsa, snd_pcm_t * handle, gint err)
{
  GST_DEBUG_OBJECT (alsa, "xrun recovery %d", err);

  if (err == -EPIPE) {          /* under-run */
    err = snd_pcm_prepare (handle);
    if (err < 0)
      GST_WARNING_OBJECT (alsa,
          "Can't recovery from underrun, prepare failed: %s",
          snd_strerror (err));
    return 0;
  } else if (err == -ESTRPIPE) {
    while ((err = snd_pcm_resume (handle)) == -EAGAIN)
      g_usleep (100);           /* wait until the suspend flag is released */

    if (err < 0) {
      err = snd_pcm_prepare (handle);
      if (err < 0)
        GST_WARNING_OBJECT (alsa,
            "Can't recovery from suspend, prepare failed: %s",
            snd_strerror (err));
    }
    return 0;
  }
  return err;
}

static guint
gst_alsasink_write (GstAudioSink * asink, gpointer data, guint length)
{
  GstAlsaSink *alsa;
  gint err;
  gint cptr;
  gint16 *ptr = data;

  alsa = GST_ALSA_SINK (asink);

  if (alsa->iec958 && alsa->need_swap) {
    guint i;

    GST_DEBUG_OBJECT (asink, "swapping bytes");
    for (i = 0; i < length / 2; i++) {
      ptr[i] = GUINT16_SWAP_LE_BE (ptr[i]);
    }
  }

  GST_LOG_OBJECT (asink, "received audio samples buffer of %u bytes", length);

  cptr = length / alsa->bytes_per_sample;

  GST_ALSA_SINK_LOCK (asink);
  while (cptr > 0) {
    /* start by doing a blocking wait for free space. Set the timeout
     * to 4 times the period time */
    err = snd_pcm_wait (alsa->handle, (4 * alsa->period_time / 1000));
    if (err < 0) {
      GST_DEBUG_OBJECT (asink, "wait error, %d", err);
    } else {
      err = snd_pcm_writei (alsa->handle, ptr, cptr);
    }

    GST_DEBUG_OBJECT (asink, "written %d frames out of %d", err, cptr);
    if (err < 0) {
      GST_DEBUG_OBJECT (asink, "Write error: %s", snd_strerror (err));
      if (err == -EAGAIN) {
        continue;
      } else if (xrun_recovery (alsa, alsa->handle, err) < 0) {
        goto write_error;
      }
      continue;
    }

    ptr += snd_pcm_frames_to_bytes (alsa->handle, err);
    cptr -= err;
  }
  GST_ALSA_SINK_UNLOCK (asink);

  return length - (cptr * alsa->bytes_per_sample);

write_error:
  {
    GST_ALSA_SINK_UNLOCK (asink);
    return length;              /* skip one period */
  }
}

static guint
gst_alsasink_delay (GstAudioSink * asink)
{
  GstAlsaSink *alsa;
  snd_pcm_sframes_t delay;
  int res;

  alsa = GST_ALSA_SINK (asink);

  res = snd_pcm_delay (alsa->handle, &delay);
  if (G_UNLIKELY (res < 0)) {
    /* on errors, report 0 delay */
    GST_DEBUG_OBJECT (alsa, "snd_pcm_delay returned %d", res);
    delay = 0;
  }
  if (G_UNLIKELY (delay < 0)) {
    /* make sure we never return a negative delay */
    GST_WARNING_OBJECT (alsa, "snd_pcm_delay returned negative delay");
    delay = 0;
  }

  return delay;
}

static void
gst_alsasink_reset (GstAudioSink * asink)
{
  GstAlsaSink *alsa;
  gint err;

  alsa = GST_ALSA_SINK (asink);

  GST_ALSA_SINK_LOCK (asink);
  GST_DEBUG_OBJECT (alsa, "drop");
  CHECK (snd_pcm_drop (alsa->handle), drop_error);
  GST_DEBUG_OBJECT (alsa, "prepare");
  CHECK (snd_pcm_prepare (alsa->handle), prepare_error);
  GST_DEBUG_OBJECT (alsa, "reset done");
  GST_ALSA_SINK_UNLOCK (asink);

  return;

  /* ERRORS */
drop_error:
  {
    GST_ERROR_OBJECT (alsa, "alsa-reset: pcm drop error: %s",
        snd_strerror (err));
    GST_ALSA_SINK_UNLOCK (asink);
    return;
  }
prepare_error:
  {
    GST_ERROR_OBJECT (alsa, "alsa-reset: pcm prepare error: %s",
        snd_strerror (err));
    GST_ALSA_SINK_UNLOCK (asink);
    return;
  }
}
