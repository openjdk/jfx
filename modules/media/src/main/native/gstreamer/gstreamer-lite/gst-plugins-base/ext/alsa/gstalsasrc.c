/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim@fluendo.com>
 *
 * gstalsasrc.c:
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
 * SECTION:element-alsasrc
 * @see_also: alsasink, alsamixer
 *
 * This element reads data from an audio card using the ALSA API.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch -v alsasrc ! audioconvert ! vorbisenc ! oggmux ! filesink location=alsasrc.ogg
 * ]| Record from a sound card using ALSA and encode to Ogg/Vorbis.
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

#include "gstalsasrc.h"
#include "gstalsadeviceprobe.h"

#include <gst/gst-i18n-plugin.h>

#define DEFAULT_PROP_DEVICE		"default"
#define DEFAULT_PROP_DEVICE_NAME	""
#define DEFAULT_PROP_CARD_NAME	        ""

enum
{
  PROP_0,
  PROP_DEVICE,
  PROP_DEVICE_NAME,
  PROP_CARD_NAME,
  PROP_LAST
};

static void gst_alsasrc_init_interfaces (GType type);

GST_BOILERPLATE_FULL (GstAlsaSrc, gst_alsasrc, GstAudioSrc,
    GST_TYPE_AUDIO_SRC, gst_alsasrc_init_interfaces);

GST_IMPLEMENT_ALSA_MIXER_METHODS (GstAlsaSrc, gst_alsasrc_mixer);

static void gst_alsasrc_finalize (GObject * object);
static void gst_alsasrc_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_alsasrc_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstCaps *gst_alsasrc_getcaps (GstBaseSrc * bsrc);

static gboolean gst_alsasrc_open (GstAudioSrc * asrc);
static gboolean gst_alsasrc_prepare (GstAudioSrc * asrc,
    GstRingBufferSpec * spec);
static gboolean gst_alsasrc_unprepare (GstAudioSrc * asrc);
static gboolean gst_alsasrc_close (GstAudioSrc * asrc);
static guint gst_alsasrc_read (GstAudioSrc * asrc, gpointer data, guint length);
static guint gst_alsasrc_delay (GstAudioSrc * asrc);
static void gst_alsasrc_reset (GstAudioSrc * asrc);

/* AlsaSrc signals and args */
enum
{
  LAST_SIGNAL
};

#if (G_BYTE_ORDER == G_LITTLE_ENDIAN)
# define ALSA_SRC_FACTORY_ENDIANNESS   "LITTLE_ENDIAN, BIG_ENDIAN"
#else
# define ALSA_SRC_FACTORY_ENDIANNESS   "BIG_ENDIAN, LITTLE_ENDIAN"
#endif

static GstStaticPadTemplate alsasrc_src_factory =
    GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-raw-int, "
        "endianness = (int) { " ALSA_SRC_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 32, "
        "depth = (int) 32, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SRC_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 32, "
        "depth = (int) 24, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SRC_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 24, "
        "depth = (int) 24, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "endianness = (int) { " ALSA_SRC_FACTORY_ENDIANNESS " }, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 16, "
        "depth = (int) 16, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]; "
        "audio/x-raw-int, "
        "signed = (boolean) { TRUE, FALSE }, "
        "width = (int) 8, "
        "depth = (int) 8, "
        "rate = (int) [ 1, MAX ], " "channels = (int) [ 1, MAX ]")
    );

static void
gst_alsasrc_finalize (GObject * object)
{
  GstAlsaSrc *src = GST_ALSA_SRC (object);

  g_free (src->device);
  g_mutex_free (src->alsa_lock);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
gst_alsasrc_interface_supported (GstAlsaSrc * this, GType interface_type)
{
  /* only support this one interface (wrapped by GstImplementsInterface) */
  g_assert (interface_type == GST_TYPE_MIXER);

  return gst_alsasrc_mixer_supported (this, interface_type);
}

static void
gst_implements_interface_init (GstImplementsInterfaceClass * klass)
{
  klass->supported = (gpointer) gst_alsasrc_interface_supported;
}

static void
gst_alsasrc_init_interfaces (GType type)
{
  static const GInterfaceInfo implements_iface_info = {
    (GInterfaceInitFunc) gst_implements_interface_init,
    NULL,
    NULL,
  };
  static const GInterfaceInfo mixer_iface_info = {
    (GInterfaceInitFunc) gst_alsasrc_mixer_interface_init,
    NULL,
    NULL,
  };

  g_type_add_interface_static (type, GST_TYPE_IMPLEMENTS_INTERFACE,
      &implements_iface_info);
  g_type_add_interface_static (type, GST_TYPE_MIXER, &mixer_iface_info);

  gst_alsa_type_add_device_property_probe_interface (type);
}

static void
gst_alsasrc_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (element_class,
      "Audio source (ALSA)", "Source/Audio",
      "Read from a sound card via ALSA", "Wim Taymans <wim@fluendo.com>");

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&alsasrc_src_factory));
}

static void
gst_alsasrc_class_init (GstAlsaSrcClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseSrcClass *gstbasesrc_class;
  GstAudioSrcClass *gstaudiosrc_class;

  gobject_class = (GObjectClass *) klass;
  gstbasesrc_class = (GstBaseSrcClass *) klass;
  gstaudiosrc_class = (GstAudioSrcClass *) klass;

  gobject_class->finalize = gst_alsasrc_finalize;
  gobject_class->get_property = gst_alsasrc_get_property;
  gobject_class->set_property = gst_alsasrc_set_property;

  gstbasesrc_class->get_caps = GST_DEBUG_FUNCPTR (gst_alsasrc_getcaps);

  gstaudiosrc_class->open = GST_DEBUG_FUNCPTR (gst_alsasrc_open);
  gstaudiosrc_class->prepare = GST_DEBUG_FUNCPTR (gst_alsasrc_prepare);
  gstaudiosrc_class->unprepare = GST_DEBUG_FUNCPTR (gst_alsasrc_unprepare);
  gstaudiosrc_class->close = GST_DEBUG_FUNCPTR (gst_alsasrc_close);
  gstaudiosrc_class->read = GST_DEBUG_FUNCPTR (gst_alsasrc_read);
  gstaudiosrc_class->delay = GST_DEBUG_FUNCPTR (gst_alsasrc_delay);
  gstaudiosrc_class->reset = GST_DEBUG_FUNCPTR (gst_alsasrc_reset);

  g_object_class_install_property (gobject_class, PROP_DEVICE,
      g_param_spec_string ("device", "Device",
          "ALSA device, as defined in an asound configuration file",
          DEFAULT_PROP_DEVICE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_DEVICE_NAME,
      g_param_spec_string ("device-name", "Device name",
          "Human-readable name of the sound device",
          DEFAULT_PROP_DEVICE_NAME, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_CARD_NAME,
      g_param_spec_string ("card-name", "Card name",
          "Human-readable name of the sound card",
          DEFAULT_PROP_CARD_NAME, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
}

static void
gst_alsasrc_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAlsaSrc *src;

  src = GST_ALSA_SRC (object);

  switch (prop_id) {
    case PROP_DEVICE:
      g_free (src->device);
      src->device = g_value_dup_string (value);
      if (src->device == NULL) {
        src->device = g_strdup (DEFAULT_PROP_DEVICE);
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_alsasrc_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAlsaSrc *src;

  src = GST_ALSA_SRC (object);

  switch (prop_id) {
    case PROP_DEVICE:
      g_value_set_string (value, src->device);
      break;
    case PROP_DEVICE_NAME:
      g_value_take_string (value,
          gst_alsa_find_device_name (GST_OBJECT_CAST (src),
              src->device, src->handle, SND_PCM_STREAM_CAPTURE));
      break;
    case PROP_CARD_NAME:
      g_value_take_string (value,
          gst_alsa_find_card_name (GST_OBJECT_CAST (src),
              src->device, SND_PCM_STREAM_CAPTURE));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_alsasrc_init (GstAlsaSrc * alsasrc, GstAlsaSrcClass * g_class)
{
  GST_DEBUG_OBJECT (alsasrc, "initializing");

  alsasrc->device = g_strdup (DEFAULT_PROP_DEVICE);
  alsasrc->cached_caps = NULL;

  alsasrc->alsa_lock = g_mutex_new ();
}

#define CHECK(call, error) \
G_STMT_START {                  \
if ((err = call) < 0)           \
  goto error;                   \
} G_STMT_END;


static GstCaps *
gst_alsasrc_getcaps (GstBaseSrc * bsrc)
{
  GstElementClass *element_class;
  GstPadTemplate *pad_template;
  GstAlsaSrc *src;
  GstCaps *caps;

  src = GST_ALSA_SRC (bsrc);

  if (src->handle == NULL) {
    GST_DEBUG_OBJECT (src, "device not open, using template caps");
    return NULL;                /* base class will get template caps for us */
  }

  if (src->cached_caps) {
    GST_LOG_OBJECT (src, "Returning cached caps");
    return gst_caps_ref (src->cached_caps);
  }

  element_class = GST_ELEMENT_GET_CLASS (src);
  pad_template = gst_element_class_get_pad_template (element_class, "src");
  g_return_val_if_fail (pad_template != NULL, NULL);

  caps = gst_alsa_probe_supported_formats (GST_OBJECT (src), src->handle,
      gst_pad_template_get_caps (pad_template));

  if (caps) {
    src->cached_caps = gst_caps_ref (caps);
  }

  GST_INFO_OBJECT (src, "returning caps %" GST_PTR_FORMAT, caps);

  return caps;
}

static int
set_hwparams (GstAlsaSrc * alsa)
{
  guint rrate;
  gint err;
  snd_pcm_hw_params_t *params;

  snd_pcm_hw_params_malloc (&params);

  /* choose all parameters */
  CHECK (snd_pcm_hw_params_any (alsa->handle, params), no_config);
  /* set the interleaved read/write format */
  CHECK (snd_pcm_hw_params_set_access (alsa->handle, params, alsa->access),
      wrong_access);
  /* set the sample format */
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

  if (alsa->buffer_time != -1) {
    /* set the buffer time */
    CHECK (snd_pcm_hw_params_set_buffer_time_near (alsa->handle, params,
            &alsa->buffer_time, NULL), buffer_time);
  }
  if (alsa->period_time != -1) {
    /* set the period time */
    CHECK (snd_pcm_hw_params_set_period_time_near (alsa->handle, params,
            &alsa->period_time, NULL), period_time);
  }

  /* write the parameters to device */
  CHECK (snd_pcm_hw_params (alsa->handle, params), set_hw_params);

  CHECK (snd_pcm_hw_params_get_buffer_size (params, &alsa->buffer_size),
      buffer_size);

  CHECK (snd_pcm_hw_params_get_period_size (params, &alsa->period_size, NULL),
      period_size);

  snd_pcm_hw_params_free (params);
  return 0;

  /* ERRORS */
no_config:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Broken configuration for recording: no configurations available: %s",
            snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
wrong_access:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Access type not available for recording: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
no_sample_format:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Sample format not available for recording: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
no_channels:
  {
    gchar *msg = NULL;

    if ((alsa->channels) == 1)
      msg = g_strdup (_("Could not open device for recording in mono mode."));
    if ((alsa->channels) == 2)
      msg = g_strdup (_("Could not open device for recording in stereo mode."));
    if ((alsa->channels) > 2)
      msg =
          g_strdup_printf (_
          ("Could not open device for recording in %d-channel mode"),
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
        ("Rate %iHz not available for recording: %s",
            alsa->rate, snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
rate_match:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Rate doesn't match (requested %iHz, get %iHz)", alsa->rate, err));
    snd_pcm_hw_params_free (params);
    return -EINVAL;
  }
buffer_time:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set buffer time %i for recording: %s",
            alsa->buffer_time, snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
buffer_size:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to get buffer size for recording: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
period_time:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set period time %i for recording: %s", alsa->period_time,
            snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
period_size:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to get period size for recording: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
set_hw_params:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Unable to set hw params for recording: %s", snd_strerror (err)));
    snd_pcm_hw_params_free (params);
    return err;
  }
}

static int
set_swparams (GstAlsaSrc * alsa)
{
  int err;
  snd_pcm_sw_params_t *params;

  snd_pcm_sw_params_malloc (&params);

  /* get the current swparams */
  CHECK (snd_pcm_sw_params_current (alsa->handle, params), no_config);
  /* allow the transfer when at least period_size samples can be processed */
  CHECK (snd_pcm_sw_params_set_avail_min (alsa->handle, params,
          alsa->period_size), set_avail);
  /* start the transfer on first read */
  CHECK (snd_pcm_sw_params_set_start_threshold (alsa->handle, params,
          0), start_threshold);

#if GST_CHECK_ALSA_VERSION(1,0,16)
  /* snd_pcm_sw_params_set_xfer_align() is deprecated, alignment is always 1 */
#else
  /* align all transfers to 1 sample */
  CHECK (snd_pcm_sw_params_set_xfer_align (alsa->handle, params, 1), set_align);
#endif

  /* write the parameters to the recording device */
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
alsasrc_parse_spec (GstAlsaSrc * alsa, GstRingBufferSpec * spec)
{
  switch (spec->type) {
    case GST_BUFTYPE_LINEAR:
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
gst_alsasrc_open (GstAudioSrc * asrc)
{
  GstAlsaSrc *alsa;
  gint err;

  alsa = GST_ALSA_SRC (asrc);

  CHECK (snd_pcm_open (&alsa->handle, alsa->device, SND_PCM_STREAM_CAPTURE,
          SND_PCM_NONBLOCK), open_error);

  if (!alsa->mixer)
    alsa->mixer = gst_alsa_mixer_new (alsa->device, GST_ALSA_MIXER_CAPTURE);

  return TRUE;

  /* ERRORS */
open_error:
  {
    if (err == -EBUSY) {
      GST_ELEMENT_ERROR (alsa, RESOURCE, BUSY,
          (_("Could not open audio device for recording. "
                  "Device is being used by another application.")),
          ("Device '%s' is busy", alsa->device));
    } else {
      GST_ELEMENT_ERROR (alsa, RESOURCE, OPEN_READ,
          (_("Could not open audio device for recording.")),
          ("Recording open error on device '%s': %s", alsa->device,
              snd_strerror (err)));
    }
    return FALSE;
  }
}

static gboolean
gst_alsasrc_prepare (GstAudioSrc * asrc, GstRingBufferSpec * spec)
{
  GstAlsaSrc *alsa;
  gint err;

  alsa = GST_ALSA_SRC (asrc);

  if (!alsasrc_parse_spec (alsa, spec))
    goto spec_parse;

  CHECK (snd_pcm_nonblock (alsa->handle, 0), non_block);

  CHECK (set_hwparams (alsa), hw_params_failed);
  CHECK (set_swparams (alsa), sw_params_failed);
  CHECK (snd_pcm_prepare (alsa->handle), prepare_failed);

  alsa->bytes_per_sample = spec->bytes_per_sample;
  spec->segsize = alsa->period_size * spec->bytes_per_sample;
  spec->segtotal = alsa->buffer_size / alsa->period_size;
  spec->silence_sample[0] = 0;
  spec->silence_sample[1] = 0;
  spec->silence_sample[2] = 0;
  spec->silence_sample[3] = 0;

  return TRUE;

  /* ERRORS */
spec_parse:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Error parsing spec"));
    return FALSE;
  }
non_block:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Could not set device to blocking: %s", snd_strerror (err)));
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
prepare_failed:
  {
    GST_ELEMENT_ERROR (alsa, RESOURCE, SETTINGS, (NULL),
        ("Prepare failed: %s", snd_strerror (err)));
    return FALSE;
  }
}

static gboolean
gst_alsasrc_unprepare (GstAudioSrc * asrc)
{
  GstAlsaSrc *alsa;

  alsa = GST_ALSA_SRC (asrc);

  snd_pcm_drop (alsa->handle);
  snd_pcm_hw_free (alsa->handle);
  snd_pcm_nonblock (alsa->handle, 1);

  return TRUE;
}

static gboolean
gst_alsasrc_close (GstAudioSrc * asrc)
{
  GstAlsaSrc *alsa = GST_ALSA_SRC (asrc);

  snd_pcm_close (alsa->handle);
  alsa->handle = NULL;

  if (alsa->mixer) {
    gst_alsa_mixer_free (alsa->mixer);
    alsa->mixer = NULL;
  }

  gst_caps_replace (&alsa->cached_caps, NULL);

  return TRUE;
}

/*
 *   Underrun and suspend recovery
 */
static gint
xrun_recovery (GstAlsaSrc * alsa, snd_pcm_t * handle, gint err)
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
gst_alsasrc_read (GstAudioSrc * asrc, gpointer data, guint length)
{
  GstAlsaSrc *alsa;
  gint err;
  gint cptr;
  gint16 *ptr;

  alsa = GST_ALSA_SRC (asrc);

  cptr = length / alsa->bytes_per_sample;
  ptr = data;

  GST_ALSA_SRC_LOCK (asrc);
  while (cptr > 0) {
    if ((err = snd_pcm_readi (alsa->handle, ptr, cptr)) < 0) {
      if (err == -EAGAIN) {
        GST_DEBUG_OBJECT (asrc, "Read error: %s", snd_strerror (err));
        continue;
      } else if (xrun_recovery (alsa, alsa->handle, err) < 0) {
        goto read_error;
      }
      continue;
    }

    ptr += err * alsa->channels;
    cptr -= err;
  }
  GST_ALSA_SRC_UNLOCK (asrc);

  return length - (cptr * alsa->bytes_per_sample);

read_error:
  {
    GST_ALSA_SRC_UNLOCK (asrc);
    return length;              /* skip one period */
  }
}

static guint
gst_alsasrc_delay (GstAudioSrc * asrc)
{
  GstAlsaSrc *alsa;
  snd_pcm_sframes_t delay;
  int res;

  alsa = GST_ALSA_SRC (asrc);

  res = snd_pcm_delay (alsa->handle, &delay);
  if (G_UNLIKELY (res < 0)) {
    GST_DEBUG_OBJECT (alsa, "snd_pcm_delay returned %d", res);
    delay = 0;
  }

  return CLAMP (delay, 0, alsa->buffer_size);
}

static void
gst_alsasrc_reset (GstAudioSrc * asrc)
{
  GstAlsaSrc *alsa;
  gint err;

  alsa = GST_ALSA_SRC (asrc);

  GST_ALSA_SRC_LOCK (asrc);
  GST_DEBUG_OBJECT (alsa, "drop");
  CHECK (snd_pcm_drop (alsa->handle), drop_error);
  GST_DEBUG_OBJECT (alsa, "prepare");
  CHECK (snd_pcm_prepare (alsa->handle), prepare_error);
  GST_DEBUG_OBJECT (alsa, "reset done");
  GST_ALSA_SRC_UNLOCK (asrc);

  return;

  /* ERRORS */
drop_error:
  {
    GST_ERROR_OBJECT (alsa, "alsa-reset: pcm drop error: %s",
        snd_strerror (err));
    GST_ALSA_SRC_UNLOCK (asrc);
    return;
  }
prepare_error:
  {
    GST_ERROR_OBJECT (alsa, "alsa-reset: pcm prepare error: %s",
        snd_strerror (err));
    GST_ALSA_SRC_UNLOCK (asrc);
    return;
  }
}
