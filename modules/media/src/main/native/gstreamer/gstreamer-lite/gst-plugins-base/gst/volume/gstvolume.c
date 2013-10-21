/* -*- c-basic-offset: 2 -*-
 * vi:si:et:sw=2:sts=8:ts=8:expandtab
 *
 * GStreamer
 * Copyright (C) 1999-2001 Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) 2005 Andy Wingo <wingo@pobox.com>
 * Copyright (C) 2010 Sebastian Dröge <sebastian.droege@collabora.co.uk>
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
 * SECTION:element-volume
 *
 * The volume element changes the volume of the audio data.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m audiotestsrc ! volume volume=0.5 ! level ! fakesink silent=TRUE
 * ]| This pipeline shows that the level of audiotestsrc has been halved
 * (peak values are around -6 dB and RMS around -9 dB) compared to
 * the same pipeline without the volume element.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>
#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>
#include <gst/audio/audio.h>
#include <gst/interfaces/mixer.h>
#include <gst/controller/gstcontroller.h>
#include <gst/audio/audio.h>
#include <gst/audio/gstaudiofilter.h>

#ifdef HAVE_ORC
#include <orc/orcfunctions.h>
#else
#define orc_memset memset
#endif

#ifndef GSTREAMER_LITE
#include "gstvolumeorc.h"
#else
#include "gstvolumeorc-dist.h"
#endif
#include "gstvolume.h"

/* some defines for audio processing */
/* the volume factor is a range from 0.0 to (arbitrary) VOLUME_MAX_DOUBLE = 10.0
 * we map 1.0 to VOLUME_UNITY_INT*
 */
#define VOLUME_UNITY_INT8            32 /* internal int for unity 2^(8-3) */
#define VOLUME_UNITY_INT8_BIT_SHIFT  5  /* number of bits to shift for unity */
#define VOLUME_UNITY_INT16           8192       /* internal int for unity 2^(16-3) */
#define VOLUME_UNITY_INT16_BIT_SHIFT 13 /* number of bits to shift for unity */
#define VOLUME_UNITY_INT24           2097152    /* internal int for unity 2^(24-3) */
#define VOLUME_UNITY_INT24_BIT_SHIFT 21 /* number of bits to shift for unity */
#define VOLUME_UNITY_INT32           134217728  /* internal int for unity 2^(32-5) */
#define VOLUME_UNITY_INT32_BIT_SHIFT 27
#define VOLUME_MAX_DOUBLE            10.0
#define VOLUME_MAX_INT8              G_MAXINT8
#define VOLUME_MIN_INT8              G_MININT8
#define VOLUME_MAX_INT16             G_MAXINT16
#define VOLUME_MIN_INT16             G_MININT16
#define VOLUME_MAX_INT24             8388607
#define VOLUME_MIN_INT24             -8388608
#define VOLUME_MAX_INT32             G_MAXINT32
#define VOLUME_MIN_INT32             G_MININT32

/* number of steps we use for the mixer interface to go from 0.0 to 1.0 */
# define VOLUME_STEPS           100

#define GST_CAT_DEFAULT gst_volume_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

#define DEFAULT_PROP_MUTE       FALSE
#define DEFAULT_PROP_VOLUME     1.0

enum
{
  PROP_0,
  PROP_MUTE,
  PROP_VOLUME
};

#define ALLOWED_CAPS \
        "audio/x-raw-float, " \
        "rate = (int) [ 1, MAX ], " \
        "channels = (int) [ 1, MAX ], " \
        "endianness = (int) BYTE_ORDER, " \
        "width = (int) {32, 64}; " \
        "audio/x-raw-int, " \
        "channels = (int) [ 1, MAX ], " \
        "rate = (int) [ 1,  MAX ], " \
        "endianness = (int) BYTE_ORDER, " \
        "width = (int) 8, " \
        "depth = (int) 8, " \
        "signed = (bool) TRUE; " \
        "audio/x-raw-int, " \
        "channels = (int) [ 1, MAX ], " \
        "rate = (int) [ 1,  MAX ], " \
        "endianness = (int) BYTE_ORDER, " \
        "width = (int) 16, " \
        "depth = (int) 16, " \
        "signed = (bool) TRUE; " \
        "audio/x-raw-int, " \
        "channels = (int) [ 1, MAX ], " \
        "rate = (int) [ 1,  MAX ], " \
        "endianness = (int) BYTE_ORDER, " \
        "width = (int) 24, " \
        "depth = (int) 24, " \
        "signed = (bool) TRUE; " \
        "audio/x-raw-int, " \
        "channels = (int) [ 1, MAX ], " \
        "rate = (int) [ 1,  MAX ], " \
        "endianness = (int) BYTE_ORDER, " \
        "width = (int) 32, " \
	"depth = (int) 32, " \
	"signed = (bool) TRUE"

static void gst_volume_interface_init (GstImplementsInterfaceClass * klass);
static void gst_volume_mixer_init (GstMixerClass * iface);

#define _init_interfaces(type)                                          \
  {                                                                     \
    static const GInterfaceInfo voliface_info = {                       \
      (GInterfaceInitFunc) gst_volume_interface_init,                   \
      NULL,                                                             \
      NULL                                                              \
    };                                                                  \
    static const GInterfaceInfo volmixer_info = {                       \
      (GInterfaceInitFunc) gst_volume_mixer_init,                       \
      NULL,                                                             \
      NULL                                                              \
    };                                                                  \
    static const GInterfaceInfo svol_info = {                           \
      NULL,                                                             \
      NULL,                                                             \
      NULL                                                              \
    };                                                                  \
                                                                        \
    g_type_add_interface_static (type, GST_TYPE_IMPLEMENTS_INTERFACE,   \
        &voliface_info);                                                \
    g_type_add_interface_static (type, GST_TYPE_MIXER, &volmixer_info); \
    g_type_add_interface_static (type, GST_TYPE_STREAM_VOLUME, &svol_info); \
  }

GST_BOILERPLATE_FULL (GstVolume, gst_volume, GstAudioFilter,
    GST_TYPE_AUDIO_FILTER, _init_interfaces);

static void volume_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void volume_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static void volume_before_transform (GstBaseTransform * base,
    GstBuffer * buffer);
static GstFlowReturn volume_transform_ip (GstBaseTransform * base,
    GstBuffer * outbuf);
static gboolean volume_stop (GstBaseTransform * base);
static gboolean volume_setup (GstAudioFilter * filter,
    GstRingBufferSpec * format);

static void volume_process_double (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_double (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes);
static void volume_process_float (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_float (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes);
static void volume_process_int32 (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_int32_clamp (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_int32_clamp (GstVolume * self,
    gpointer bytes, gdouble * volume, guint channels, guint n_bytes);
static void volume_process_int24 (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_int24_clamp (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_int24_clamp (GstVolume * self,
    gpointer bytes, gdouble * volume, guint channels, guint n_bytes);
static void volume_process_int16 (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_int16_clamp (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_int16_clamp (GstVolume * self,
    gpointer bytes, gdouble * volume, guint channels, guint n_bytes);
static void volume_process_int8 (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_int8_clamp (GstVolume * self, gpointer bytes,
    guint n_bytes);
static void volume_process_controlled_int8_clamp (GstVolume * self,
    gpointer bytes, gdouble * volume, guint channels, guint n_bytes);


/* helper functions */

static gboolean
volume_choose_func (GstVolume * self)
{
  self->process = NULL;
  self->process_controlled = NULL;

  if (GST_AUDIO_FILTER (self)->format.caps == NULL)
    return FALSE;

  switch (GST_AUDIO_FILTER (self)->format.type) {
    case GST_BUFTYPE_LINEAR:
      switch (GST_AUDIO_FILTER (self)->format.width) {
        case 32:
          /* only clamp if the gain is greater than 1.0
           */
          if (self->current_vol_i32 > VOLUME_UNITY_INT32) {
            self->process = volume_process_int32_clamp;
          } else {
            self->process = volume_process_int32;
          }
          self->process_controlled = volume_process_controlled_int32_clamp;
          break;
        case 24:
          /* only clamp if the gain is greater than 1.0
           */
          if (self->current_vol_i24 > VOLUME_UNITY_INT24) {
            self->process = volume_process_int24_clamp;
          } else {
            self->process = volume_process_int24;
          }
          self->process_controlled = volume_process_controlled_int24_clamp;
          break;
        case 16:
          /* only clamp if the gain is greater than 1.0
           */
          if (self->current_vol_i16 > VOLUME_UNITY_INT16) {
            self->process = volume_process_int16_clamp;
          } else {
            self->process = volume_process_int16;
          }
          self->process_controlled = volume_process_controlled_int16_clamp;
          break;
        case 8:
          /* only clamp if the gain is greater than 1.0
           */
          if (self->current_vol_i16 > VOLUME_UNITY_INT8) {
            self->process = volume_process_int8_clamp;
          } else {
            self->process = volume_process_int8;
          }
          self->process_controlled = volume_process_controlled_int8_clamp;
          break;
      }
      break;
    case GST_BUFTYPE_FLOAT:
      switch (GST_AUDIO_FILTER (self)->format.width) {
        case 32:
          self->process = volume_process_float;
          self->process_controlled = volume_process_controlled_float;
          break;
        case 64:
          self->process = volume_process_double;
          self->process_controlled = volume_process_controlled_double;
          break;
      }
      break;
    default:
      break;
  }

  return (self->process != NULL);
}

static gboolean
volume_update_volume (GstVolume * self, gfloat volume, gboolean mute)
{
  gboolean passthrough;
  gboolean res;
  GstController *controller;

  GST_DEBUG_OBJECT (self, "configure mute %d, volume %f", mute, volume);

  if (mute) {
    self->current_mute = TRUE;
    self->current_volume = 0.0;

    self->current_vol_i8 = 0;
    self->current_vol_i16 = 0;
    self->current_vol_i24 = 0;
    self->current_vol_i32 = 0;

    passthrough = FALSE;
  } else {
    self->current_mute = FALSE;
    self->current_volume = volume;

    self->current_vol_i8 = volume * VOLUME_UNITY_INT8;
    self->current_vol_i16 = volume * VOLUME_UNITY_INT16;
    self->current_vol_i24 = volume * VOLUME_UNITY_INT24;
    self->current_vol_i32 = volume * VOLUME_UNITY_INT32;

    passthrough = (self->current_vol_i16 == VOLUME_UNITY_INT16);
  }

  /* If a controller is used, never use passthrough mode
   * because the property can change from 1.0 to something
   * else in the middle of a buffer.
   */
  controller = gst_object_get_controller (G_OBJECT (self));
  passthrough = passthrough && (controller == NULL);

  GST_DEBUG_OBJECT (self, "set passthrough %d", passthrough);

  gst_base_transform_set_passthrough (GST_BASE_TRANSFORM (self), passthrough);

  res = self->negotiated = volume_choose_func (self);

  return res;
}

/* Mixer interface */

static gboolean
gst_volume_interface_supported (GstImplementsInterface * iface, GType type)
{
  return (type == GST_TYPE_MIXER || type == GST_TYPE_STREAM_VOLUME);
}

static void
gst_volume_interface_init (GstImplementsInterfaceClass * klass)
{
  klass->supported = gst_volume_interface_supported;
}

static const GList *
gst_volume_list_tracks (GstMixer * mixer)
{
  GstVolume *self = GST_VOLUME (mixer);

  g_return_val_if_fail (self != NULL, NULL);
  g_return_val_if_fail (GST_IS_VOLUME (self), NULL);

  return self->tracklist;
}

static void
gst_volume_set_volume (GstMixer * mixer, GstMixerTrack * track, gint * volumes)
{
  GstVolume *self = GST_VOLUME (mixer);

  g_return_if_fail (self != NULL);
  g_return_if_fail (GST_IS_VOLUME (self));

  GST_OBJECT_LOCK (self);
  self->volume = (gfloat) volumes[0] / VOLUME_STEPS;
  GST_OBJECT_UNLOCK (self);
}

static void
gst_volume_get_volume (GstMixer * mixer, GstMixerTrack * track, gint * volumes)
{
  GstVolume *self = GST_VOLUME (mixer);

  g_return_if_fail (self != NULL);
  g_return_if_fail (GST_IS_VOLUME (self));

  GST_OBJECT_LOCK (self);
  volumes[0] = (gint) self->volume * VOLUME_STEPS;
  GST_OBJECT_UNLOCK (self);
}

static void
gst_volume_set_mute (GstMixer * mixer, GstMixerTrack * track, gboolean mute)
{
  GstVolume *self = GST_VOLUME (mixer);

  g_return_if_fail (self != NULL);
  g_return_if_fail (GST_IS_VOLUME (self));

  GST_OBJECT_LOCK (self);
  self->mute = mute;
  GST_OBJECT_UNLOCK (self);
}

static void
gst_volume_mixer_init (GstMixerClass * klass)
{
  GST_MIXER_TYPE (klass) = GST_MIXER_SOFTWARE;

  /* default virtual functions */
  klass->list_tracks = gst_volume_list_tracks;
  klass->set_volume = gst_volume_set_volume;
  klass->get_volume = gst_volume_get_volume;
  klass->set_mute = gst_volume_set_mute;
}

/* Element class */

static void
gst_volume_dispose (GObject * object)
{
  GstVolume *volume = GST_VOLUME (object);

  if (volume->tracklist) {
    if (volume->tracklist->data)
      g_object_unref (volume->tracklist->data);
    g_list_free (volume->tracklist);
    volume->tracklist = NULL;
  }

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_volume_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);
  GstAudioFilterClass *filter_class = GST_AUDIO_FILTER_CLASS (g_class);
  GstCaps *caps;

  gst_element_class_set_details_simple (element_class, "Volume",
      "Filter/Effect/Audio",
      "Set volume on audio/raw streams", "Andy Wingo <wingo@pobox.com>");

  caps = gst_caps_from_string (ALLOWED_CAPS);
  gst_audio_filter_class_add_pad_templates (filter_class, caps);
  gst_caps_unref (caps);
}

static void
gst_volume_class_init (GstVolumeClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseTransformClass *trans_class;
  GstAudioFilterClass *filter_class;

  gobject_class = (GObjectClass *) klass;
  trans_class = (GstBaseTransformClass *) klass;
  filter_class = (GstAudioFilterClass *) (klass);

  gobject_class->set_property = volume_set_property;
  gobject_class->get_property = volume_get_property;
  gobject_class->dispose = gst_volume_dispose;

  g_object_class_install_property (gobject_class, PROP_MUTE,
      g_param_spec_boolean ("mute", "Mute", "mute channel",
          DEFAULT_PROP_MUTE,
          G_PARAM_READWRITE | GST_PARAM_CONTROLLABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_VOLUME,
      g_param_spec_double ("volume", "Volume", "volume factor, 1.0=100%",
          0.0, VOLUME_MAX_DOUBLE, DEFAULT_PROP_VOLUME,
          G_PARAM_READWRITE | GST_PARAM_CONTROLLABLE | G_PARAM_STATIC_STRINGS));

  trans_class->before_transform = GST_DEBUG_FUNCPTR (volume_before_transform);
  trans_class->transform_ip = GST_DEBUG_FUNCPTR (volume_transform_ip);
  trans_class->stop = GST_DEBUG_FUNCPTR (volume_stop);
  filter_class->setup = GST_DEBUG_FUNCPTR (volume_setup);
}

static void
gst_volume_init (GstVolume * self, GstVolumeClass * g_class)
{
  GstMixerTrack *track = NULL;

  self->mute = DEFAULT_PROP_MUTE;;
  self->volume = DEFAULT_PROP_VOLUME;

  self->tracklist = NULL;
  self->negotiated = FALSE;

  track = g_object_new (GST_TYPE_MIXER_TRACK, NULL);

  if (GST_IS_MIXER_TRACK (track)) {
    track->label = g_strdup ("volume");
    track->num_channels = 1;
    track->min_volume = 0;
    track->max_volume = VOLUME_STEPS;
    track->flags = GST_MIXER_TRACK_SOFTWARE;
    self->tracklist = g_list_append (self->tracklist, track);
  }

  gst_base_transform_set_gap_aware (GST_BASE_TRANSFORM (self), TRUE);
}

static void
volume_process_double (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gdouble *data = (gdouble *) bytes;
  guint num_samples = n_bytes / sizeof (gdouble);

  orc_scalarmultiply_f64_ns (data, self->current_volume, num_samples);
}

static void
volume_process_controlled_double (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gdouble *data = (gdouble *) bytes;
  guint num_samples = n_bytes / (sizeof (gdouble) * channels);
  guint i, j;
  gdouble vol;

  if (channels == 1) {
    orc_process_controlled_f64_1ch (data, volume, num_samples);
  } else {
    for (i = 0; i < num_samples; i++) {
      vol = *volume++;
      for (j = 0; j < channels; j++) {
        *data++ *= vol;
      }
    }
  }
}

static void
volume_process_float (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gfloat *data = (gfloat *) bytes;
  guint num_samples = n_bytes / sizeof (gfloat);

  orc_scalarmultiply_f32_ns (data, self->current_volume, num_samples);
}

static void
volume_process_controlled_float (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gfloat *data = (gfloat *) bytes;
  guint num_samples = n_bytes / (sizeof (gfloat) * channels);
  guint i, j;
  gdouble vol;

  if (channels == 1) {
    orc_process_controlled_f32_1ch (data, volume, num_samples);
  } else if (channels == 2) {
    orc_process_controlled_f32_2ch (data, volume, num_samples);
  } else {
    for (i = 0; i < num_samples; i++) {
      vol = *volume++;
      for (j = 0; j < channels; j++) {
        *data++ *= vol;
      }
    }
  }
}

static void
volume_process_int32 (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint32 *data = (gint32 *) bytes;
  guint num_samples = n_bytes / sizeof (gint);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT32_BIT_SHIFT == 27);
  orc_process_int32 (data, self->current_vol_i32, num_samples);
}

static void
volume_process_int32_clamp (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint32 *data = (gint32 *) bytes;
  guint num_samples = n_bytes / sizeof (gint);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT32_BIT_SHIFT == 27);

  orc_process_int32_clamp (data, self->current_vol_i32, num_samples);
}

static void
volume_process_controlled_int32_clamp (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gint32 *data = (gint32 *) bytes;
  guint i, j;
  guint num_samples = n_bytes / (sizeof (gint32) * channels);
  gdouble vol, val;

  if (channels == 1) {
    orc_process_controlled_int32_1ch (data, volume, num_samples);
  } else {
    for (i = 0; i < num_samples; i++) {
      vol = *volume++;
      for (j = 0; j < channels; j++) {
        val = *data * vol;
        *data++ = (gint32) CLAMP (val, VOLUME_MIN_INT32, VOLUME_MAX_INT32);
      }
    }
  }
}

#if (G_BYTE_ORDER == G_LITTLE_ENDIAN)
#define get_unaligned_i24(_x) ( (((guint8*)_x)[0]) | ((((guint8*)_x)[1]) << 8) | ((((gint8*)_x)[2]) << 16) )

#define write_unaligned_u24(_x,samp) \
G_STMT_START { \
  *(_x)++ = samp & 0xFF; \
  *(_x)++ = (samp >> 8) & 0xFF; \
  *(_x)++ = (samp >> 16) & 0xFF; \
} G_STMT_END

#else /* BIG ENDIAN */
#define get_unaligned_i24(_x) ( (((guint8*)_x)[2]) | ((((guint8*)_x)[1]) << 8) | ((((gint8*)_x)[0]) << 16) )
#define write_unaligned_u24(_x,samp) \
G_STMT_START { \
  *(_x)++ = (samp >> 16) & 0xFF; \
  *(_x)++ = (samp >> 8) & 0xFF; \
  *(_x)++ = samp & 0xFF; \
} G_STMT_END
#endif

static void
volume_process_int24 (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;        /* treat the data as a byte stream */
  guint i, num_samples;
  guint32 samp;
  gint64 val;

  num_samples = n_bytes / (sizeof (gint8) * 3);
  for (i = 0; i < num_samples; i++) {
    samp = get_unaligned_i24 (data);

    val = (gint32) samp;
    val =
        (((gint64) self->current_vol_i24 *
            val) >> VOLUME_UNITY_INT24_BIT_SHIFT);
    samp = (guint32) val;

    /* write the value back into the stream */
    write_unaligned_u24 (data, samp);
  }
}

static void
volume_process_int24_clamp (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;        /* treat the data as a byte stream */
  guint i, num_samples;
  guint32 samp;
  gint64 val;

  num_samples = n_bytes / (sizeof (gint8) * 3);
  for (i = 0; i < num_samples; i++) {
    samp = get_unaligned_i24 (data);

    val = (gint32) samp;
    val =
        (((gint64) self->current_vol_i24 *
            val) >> VOLUME_UNITY_INT24_BIT_SHIFT);
    samp = (guint32) CLAMP (val, VOLUME_MIN_INT24, VOLUME_MAX_INT24);

    /* write the value back into the stream */
    write_unaligned_u24 (data, samp);
  }
}

static void
volume_process_controlled_int24_clamp (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;        /* treat the data as a byte stream */
  guint i, j;
  guint num_samples = n_bytes / (sizeof (gint8) * 3 * channels);
  gdouble vol, val;

  for (i = 0; i < num_samples; i++) {
    vol = *volume++;
    for (j = 0; j < channels; j++) {
      val = get_unaligned_i24 (data) * vol;
      val = CLAMP (val, VOLUME_MIN_INT24, VOLUME_MAX_INT24);
      write_unaligned_u24 (data, (gint32) val);
    }
  }
}

static void
volume_process_int16 (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint16 *data = (gint16 *) bytes;
  guint num_samples = n_bytes / sizeof (gint16);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT16_BIT_SHIFT == 13);

  orc_process_int16 (data, self->current_vol_i16, num_samples);
}

static void
volume_process_int16_clamp (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint16 *data = (gint16 *) bytes;
  guint num_samples = n_bytes / sizeof (gint16);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT16_BIT_SHIFT == 13);

  orc_process_int16_clamp (data, self->current_vol_i16, num_samples);
}

static void
volume_process_controlled_int16_clamp (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gint16 *data = (gint16 *) bytes;
  guint i, j;
  guint num_samples = n_bytes / (sizeof (gint16) * channels);
  gdouble vol, val;

  if (channels == 1) {
    orc_process_controlled_int16_1ch (data, volume, num_samples);
  } else if (channels == 2) {
    orc_process_controlled_int16_2ch (data, volume, num_samples);
  } else {
    for (i = 0; i < num_samples; i++) {
      vol = *volume++;
      for (j = 0; j < channels; j++) {
        val = *data * vol;
        *data++ = (gint16) CLAMP (val, VOLUME_MIN_INT16, VOLUME_MAX_INT16);
      }
    }
  }
}

static void
volume_process_int8 (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;
  guint num_samples = n_bytes / sizeof (gint8);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT8_BIT_SHIFT == 5);

  orc_process_int8 (data, self->current_vol_i8, num_samples);
}

static void
volume_process_int8_clamp (GstVolume * self, gpointer bytes, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;
  guint num_samples = n_bytes / sizeof (gint8);

  /* hard coded in volume.orc */
  g_assert (VOLUME_UNITY_INT8_BIT_SHIFT == 5);

  orc_process_int8_clamp (data, self->current_vol_i8, num_samples);
}

static void
volume_process_controlled_int8_clamp (GstVolume * self, gpointer bytes,
    gdouble * volume, guint channels, guint n_bytes)
{
  gint8 *data = (gint8 *) bytes;
  guint i, j;
  guint num_samples = n_bytes / (sizeof (gint8) * channels);
  gdouble val, vol;

  if (channels == 1) {
    orc_process_controlled_int8_1ch (data, volume, num_samples);
  } else if (channels == 2) {
    orc_process_controlled_int8_2ch (data, volume, num_samples);
  } else {
    for (i = 0; i < num_samples; i++) {
      vol = *volume++;
      for (j = 0; j < channels; j++) {
        val = *data * vol;
        *data++ = (gint8) CLAMP (val, VOLUME_MIN_INT8, VOLUME_MAX_INT8);
      }
    }
  }
}

/* GstBaseTransform vmethod implementations */

/* get notified of caps and plug in the correct process function */
static gboolean
volume_setup (GstAudioFilter * filter, GstRingBufferSpec * format)
{
  gboolean res;
  GstVolume *self = GST_VOLUME (filter);
  gfloat volume;
  gboolean mute;

  GST_OBJECT_LOCK (self);
  volume = self->volume;
  mute = self->mute;
  GST_OBJECT_UNLOCK (self);

  res = volume_update_volume (self, volume, mute);
  if (!res) {
    GST_ELEMENT_ERROR (self, CORE, NEGOTIATION,
        ("Invalid incoming format"), (NULL));
  }
  self->negotiated = res;

  return res;
}

static gboolean
volume_stop (GstBaseTransform * base)
{
  GstVolume *self = GST_VOLUME (base);

  g_free (self->volumes);
  self->volumes = NULL;
  self->volumes_count = 0;

  g_free (self->mutes);
  self->mutes = NULL;
  self->mutes_count = 0;

  return GST_CALL_PARENT_WITH_DEFAULT (GST_BASE_TRANSFORM_CLASS, stop, (base),
      TRUE);
}

static void
volume_before_transform (GstBaseTransform * base, GstBuffer * buffer)
{
  GstClockTime timestamp;
  GstVolume *self = GST_VOLUME (base);
  gfloat volume;
  gboolean mute;

  timestamp = GST_BUFFER_TIMESTAMP (buffer);
  timestamp =
      gst_segment_to_stream_time (&base->segment, GST_FORMAT_TIME, timestamp);

  GST_DEBUG_OBJECT (base, "sync to %" GST_TIME_FORMAT,
      GST_TIME_ARGS (timestamp));

  if (GST_CLOCK_TIME_IS_VALID (timestamp))
    gst_object_sync_values (G_OBJECT (self), timestamp);

  /* get latest values */
  GST_OBJECT_LOCK (self);
  volume = self->volume;
  mute = self->mute;
  GST_OBJECT_UNLOCK (self);

  if ((volume != self->current_volume) || (mute != self->current_mute)) {
    /* the volume or mute was updated, update our internal state before
     * we continue processing. */
    volume_update_volume (self, volume, mute);
  }
}

/* call the plugged-in process function for this instance
 * needs to be done with this indirection since volume_transform is
 * a class-global method
 */
static GstFlowReturn
volume_transform_ip (GstBaseTransform * base, GstBuffer * outbuf)
{
  GstVolume *self = GST_VOLUME (base);
  guint8 *data;
  guint size;
  GstControlSource *mute_csource, *volume_csource;

  if (G_UNLIKELY (!self->negotiated))
    goto not_negotiated;

  /* don't process data in passthrough-mode */
  if (gst_base_transform_is_passthrough (base) ||
      GST_BUFFER_FLAG_IS_SET (outbuf, GST_BUFFER_FLAG_GAP))
    return GST_FLOW_OK;

  data = GST_BUFFER_DATA (outbuf);
  size = GST_BUFFER_SIZE (outbuf);

  mute_csource = gst_object_get_control_source (G_OBJECT (self), "mute");
  volume_csource = gst_object_get_control_source (G_OBJECT (self), "volume");
  if (mute_csource || (volume_csource && !self->current_mute)) {
    gint rate = GST_AUDIO_FILTER_CAST (self)->format.rate;
    gint width = GST_AUDIO_FILTER_CAST (self)->format.width / 8;
    gint channels = GST_AUDIO_FILTER_CAST (self)->format.channels;
    guint nsamples = size / (width * channels);
    GstClockTime interval = gst_util_uint64_scale_int (1, GST_SECOND, rate);
    GstClockTime ts = GST_BUFFER_TIMESTAMP (outbuf);

    ts = gst_segment_to_stream_time (&base->segment, GST_FORMAT_TIME, ts);

    if (self->mutes_count < nsamples && mute_csource) {
      self->mutes = g_realloc (self->mutes, sizeof (gboolean) * nsamples);
      self->mutes_count = nsamples;
    }

    if (self->volumes_count < nsamples) {
      self->volumes = g_realloc (self->volumes, sizeof (gdouble) * nsamples);
      self->volumes_count = nsamples;
    }

    if (mute_csource) {
      GstValueArray va = { "mute", nsamples, interval, (gpointer) self->mutes };

      if (!gst_control_source_get_value_array (mute_csource, ts, &va))
        goto controller_failure;

      gst_object_unref (mute_csource);
      mute_csource = NULL;
    } else {
      g_free (self->mutes);
      self->mutes = NULL;
      self->mutes_count = 0;
    }

    if (volume_csource) {
      GstValueArray va =
          { "volume", nsamples, interval, (gpointer) self->volumes };

      if (!gst_control_source_get_value_array (volume_csource, ts, &va))
        goto controller_failure;

      gst_object_unref (volume_csource);
      volume_csource = NULL;
    } else {
      orc_memset_f64 (self->volumes, self->current_volume, nsamples);
    }

    if (mute_csource) {
      orc_prepare_volumes (self->volumes, self->mutes, nsamples);
    }

    self->process_controlled (self, data, self->volumes, channels, size);

    return GST_FLOW_OK;
  } else if (volume_csource) {
    gst_object_unref (volume_csource);
  }

  if (self->current_volume == 0.0 || self->current_mute) {
    orc_memset (data, 0, size);
    GST_BUFFER_FLAG_SET (outbuf, GST_BUFFER_FLAG_GAP);
  } else if (self->current_volume != 1.0) {
    self->process (self, data, size);
  }

  return GST_FLOW_OK;

  /* ERRORS */
not_negotiated:
  {
    GST_ELEMENT_ERROR (self, CORE, NEGOTIATION,
        ("No format was negotiated"), (NULL));
    return GST_FLOW_NOT_NEGOTIATED;
  }
controller_failure:
  {
    if (mute_csource)
      gst_object_unref (mute_csource);
    if (volume_csource)
      gst_object_unref (volume_csource);

    GST_ELEMENT_ERROR (self, CORE, FAILED,
        ("Failed to get values from controller"), (NULL));
    return GST_FLOW_ERROR;
  }
}

static void
volume_set_property (GObject * object, guint prop_id, const GValue * value,
    GParamSpec * pspec)
{
  GstVolume *self = GST_VOLUME (object);

  switch (prop_id) {
    case PROP_MUTE:
      GST_OBJECT_LOCK (self);
      self->mute = g_value_get_boolean (value);
      GST_OBJECT_UNLOCK (self);
      break;
    case PROP_VOLUME:
      GST_OBJECT_LOCK (self);
      self->volume = g_value_get_double (value);
      GST_OBJECT_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
volume_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstVolume *self = GST_VOLUME (object);

  switch (prop_id) {
    case PROP_MUTE:
      GST_OBJECT_LOCK (self);
      g_value_set_boolean (value, self->mute);
      GST_OBJECT_UNLOCK (self);
      break;
    case PROP_VOLUME:
      GST_OBJECT_LOCK (self);
      g_value_set_double (value, self->volume);
      GST_OBJECT_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

#ifdef GSTREAMER_LITE
gboolean
plugin_init_volume (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  gst_volume_orc_init ();

  /* initialize gst controller library */
  gst_controller_init (NULL, NULL);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, "volume", 0, "Volume gain");

  /* ref class from a thread-safe context to work around missing bit of
   * thread-safety in GObject */
  g_type_class_ref (GST_TYPE_MIXER_TRACK);

  return gst_element_register (plugin, "volume", GST_RANK_NONE,
      GST_TYPE_VOLUME);
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "volume",
    "plugin for controlling audio volume",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN);
#endif // GSTREAMER_LITE

