/* 
 * GStreamer
 * Copyright (C) 2007 Sebastian Dröge <slomo@circular-chaos.org>
 * Copyright (C) 2006 Stefan Kost <ensonic@users.sf.net>
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
 * SECTION:element-audioamplify
 *
 * Amplifies an audio stream by a given factor and allows the selection of different clipping modes.
 * The difference between the clipping modes is best evaluated by testing.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch audiotestsrc wave=saw ! audioamplify amplification=1.5 ! alsasink
 * gst-launch filesrc location="melo1.ogg" ! oggdemux ! vorbisdec ! audioconvert ! audioamplify amplification=1.5 method=wrap-negative ! alsasink
 * gst-launch audiotestsrc wave=saw ! audioconvert ! audioamplify amplification=1.5 method=wrap-positive ! audioconvert ! alsasink
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

#include "audioamplify.h"

#define GST_CAT_DEFAULT gst_audio_amplify_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_AMPLIFICATION,
  PROP_CLIPPING_METHOD
};

enum
{
  METHOD_CLIP = 0,
  METHOD_WRAP_NEGATIVE,
  METHOD_WRAP_POSITIVE,
  METHOD_NOCLIP,
  NUM_METHODS
};

#define GST_TYPE_AUDIO_AMPLIFY_CLIPPING_METHOD (gst_audio_amplify_clipping_method_get_type ())
static GType
gst_audio_amplify_clipping_method_get_type (void)
{
  static GType gtype = 0;

  if (gtype == 0) {
    static const GEnumValue values[] = {
      {METHOD_CLIP, "Normal clipping (default)", "clip"},
      {METHOD_WRAP_NEGATIVE,
            "Push overdriven values back from the opposite side",
          "wrap-negative"},
      {METHOD_WRAP_POSITIVE, "Push overdriven values back from the same side",
          "wrap-positive"},
      {METHOD_NOCLIP, "No clipping", "none"},
      {0, NULL, NULL}
    };

    /* FIXME 0.11: rename to GstAudioAmplifyClippingMethod */
    gtype = g_enum_register_static ("GstAudioPanoramaClippingMethod", values);
  }
  return gtype;
}

#define ALLOWED_CAPS                                                  \
    "audio/x-raw-int,"                                                \
    " depth=(int)8,"                                                  \
    " width=(int)8,"                                                  \
    " endianness=(int)BYTE_ORDER,"                                    \
    " signed=(bool)TRUE,"                                             \
    " rate=(int)[1,MAX],"                                             \
    " channels=(int)[1,MAX]; "                                        \
    "audio/x-raw-int,"                                                \
    " depth=(int)16,"                                                 \
    " width=(int)16,"                                                 \
    " endianness=(int)BYTE_ORDER,"                                    \
    " signed=(bool)TRUE,"                                             \
    " rate=(int)[1,MAX],"                                             \
    " channels=(int)[1,MAX]; "                                        \
    "audio/x-raw-int,"                                                \
    " depth=(int)32,"                                                 \
    " width=(int)32,"                                                 \
    " endianness=(int)BYTE_ORDER,"                                    \
    " signed=(bool)TRUE,"                                             \
    " rate=(int)[1,MAX],"                                             \
    " channels=(int)[1,MAX]; "                                        \
    "audio/x-raw-float,"                                              \
    " width=(int){32,64},"                                            \
    " endianness=(int)BYTE_ORDER,"                                    \
    " rate=(int)[1,MAX],"                                             \
    " channels=(int)[1,MAX]"

#define DEBUG_INIT(bla) \
  GST_DEBUG_CATEGORY_INIT (gst_audio_amplify_debug, "audioamplify", 0, "audioamplify element");

GST_BOILERPLATE_FULL (GstAudioAmplify, gst_audio_amplify, GstAudioFilter,
    GST_TYPE_AUDIO_FILTER, DEBUG_INIT);

static gboolean gst_audio_amplify_set_process_function (GstAudioAmplify *
    filter, gint clipping, gint format, gint width);
static void gst_audio_amplify_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_audio_amplify_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static gboolean gst_audio_amplify_setup (GstAudioFilter * filter,
    GstRingBufferSpec * format);
static GstFlowReturn gst_audio_amplify_transform_ip (GstBaseTransform * base,
    GstBuffer * buf);

#define MIN_gint8 G_MININT8
#define MAX_gint8 G_MAXINT8
#define MIN_gint16 G_MININT16
#define MAX_gint16 G_MAXINT16
#define MIN_gint32 G_MININT32
#define MAX_gint32 G_MAXINT32

#define MAKE_INT_FUNCS(type,largetype)                                        \
static void                                                                   \
gst_audio_amplify_transform_##type##_clip (GstAudioAmplify * filter,          \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    largetype val = *d * filter->amplification;                               \
    *d++ =  CLAMP (val, MIN_##type, MAX_##type);                              \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_wrap_negative (GstAudioAmplify * filter, \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    largetype val = *d * filter->amplification;                               \
    if (val > MAX_##type)                                                     \
      val = MIN_##type + (val - MIN_##type) % ((largetype) MAX_##type + 1 -   \
          MIN_##type);                                                        \
    else if (val < MIN_##type)                                                \
      val = MAX_##type - (MAX_##type - val) % ((largetype) MAX_##type + 1 -   \
          MIN_##type);                                                        \
    *d++ = val;                                                               \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_wrap_positive (GstAudioAmplify * filter, \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    largetype val = *d * filter->amplification;                               \
    do {                                                                      \
      if (val > MAX_##type)                                                   \
        val = MAX_##type - (val - MAX_##type);                                \
      else if (val < MIN_##type)                                              \
        val = MIN_##type + (MIN_##type - val);                                \
      else                                                                    \
        break;                                                                \
    } while (1);                                                              \
    *d++ = val;                                                               \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_noclip (GstAudioAmplify * filter,        \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--)                                                       \
    *d++ *= filter->amplification;                                            \
}

#define MAKE_FLOAT_FUNCS(type)                                                \
static void                                                                   \
gst_audio_amplify_transform_##type##_clip (GstAudioAmplify * filter,          \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    type val = *d* filter->amplification;                                     \
    *d++ = CLAMP (val, -1.0, +1.0);                                           \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_wrap_negative (GstAudioAmplify *         \
    filter, void * data, guint num_samples)                                   \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    type val = *d * filter->amplification;                                    \
    do {                                                                      \
      if (val > 1.0)                                                          \
        val = -1.0 + (val - 1.0);                                             \
      else if (val < -1.0)                                                    \
        val = 1.0 - (1.0 - val);                                              \
      else                                                                    \
        break;                                                                \
    } while (1);                                                              \
    *d++ = val;                                                               \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_wrap_positive (GstAudioAmplify * filter, \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--) {                                                     \
    type val = *d* filter->amplification;                                     \
    do {                                                                      \
      if (val > 1.0)                                                          \
        val = 1.0 - (val - 1.0);                                              \
      else if (val < -1.0)                                                    \
        val = -1.0 + (-1.0 - val);                                            \
      else                                                                    \
        break;                                                                \
    } while (1);                                                              \
    *d++ = val;                                                               \
  }                                                                           \
}                                                                             \
static void                                                                   \
gst_audio_amplify_transform_##type##_noclip (GstAudioAmplify * filter,        \
    void * data, guint num_samples)                                           \
{                                                                             \
  type *d = data;                                                             \
                                                                              \
  while (num_samples--)                                                       \
    *d++ *= filter->amplification;                                            \
}

/* *INDENT-OFF* */
MAKE_INT_FUNCS (gint8,gint)
MAKE_INT_FUNCS (gint16,gint)
MAKE_INT_FUNCS (gint32,gint64)
MAKE_FLOAT_FUNCS (gfloat)
MAKE_FLOAT_FUNCS (gdouble)
/* *INDENT-ON* */

/* GObject vmethod implementations */

static void
gst_audio_amplify_base_init (gpointer klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GstCaps *caps;

  gst_element_class_set_details_simple (element_class, "Audio amplifier",
      "Filter/Effect/Audio",
      "Amplifies an audio stream by a given factor",
      "Sebastian Dröge <slomo@circular-chaos.org>");

  caps = gst_caps_from_string (ALLOWED_CAPS);
  gst_audio_filter_class_add_pad_templates (GST_AUDIO_FILTER_CLASS (klass),
      caps);
  gst_caps_unref (caps);
}

static void
gst_audio_amplify_class_init (GstAudioAmplifyClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = (GObjectClass *) klass;
  gobject_class->set_property = gst_audio_amplify_set_property;
  gobject_class->get_property = gst_audio_amplify_get_property;

  g_object_class_install_property (gobject_class, PROP_AMPLIFICATION,
      g_param_spec_float ("amplification", "Amplification",
          "Factor of amplification", -G_MAXFLOAT, G_MAXFLOAT,
          1.0,
          G_PARAM_READWRITE | GST_PARAM_CONTROLLABLE | G_PARAM_STATIC_STRINGS));

  /**
   * GstAudioAmplify:clipping-method
   *
   * Clipping method: clip mode set values higher than the maximum to the
   * maximum. The wrap-negative mode pushes those values back from the
   * opposite side, wrap-positive pushes them back from the same side.
   *
   **/
  g_object_class_install_property (gobject_class, PROP_CLIPPING_METHOD,
      g_param_spec_enum ("clipping-method", "Clipping method",
          "Selects how to handle values higher than the maximum",
          GST_TYPE_AUDIO_AMPLIFY_CLIPPING_METHOD, METHOD_CLIP,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  GST_AUDIO_FILTER_CLASS (klass)->setup =
      GST_DEBUG_FUNCPTR (gst_audio_amplify_setup);
  GST_BASE_TRANSFORM_CLASS (klass)->transform_ip =
      GST_DEBUG_FUNCPTR (gst_audio_amplify_transform_ip);
}

static void
gst_audio_amplify_init (GstAudioAmplify * filter, GstAudioAmplifyClass * klass)
{
  filter->amplification = 1.0;
  gst_audio_amplify_set_process_function (filter, METHOD_CLIP,
      GST_BUFTYPE_LINEAR, 16);
  gst_base_transform_set_in_place (GST_BASE_TRANSFORM (filter), TRUE);
  gst_base_transform_set_gap_aware (GST_BASE_TRANSFORM (filter), TRUE);
}

static GstAudioAmplifyProcessFunc
gst_audio_amplify_process_function (gint clipping, gint format, gint width)
{
  static const struct process
  {
    gint format;
    gint width;
    gint clipping;
    GstAudioAmplifyProcessFunc func;
  } process[] = {
    {
    GST_BUFTYPE_FLOAT, 32, METHOD_CLIP,
          gst_audio_amplify_transform_gfloat_clip}, {
    GST_BUFTYPE_FLOAT, 32, METHOD_WRAP_NEGATIVE,
          gst_audio_amplify_transform_gfloat_wrap_negative}, {
    GST_BUFTYPE_FLOAT, 32, METHOD_WRAP_POSITIVE,
          gst_audio_amplify_transform_gfloat_wrap_positive}, {
    GST_BUFTYPE_FLOAT, 32, METHOD_NOCLIP,
          gst_audio_amplify_transform_gfloat_noclip}, {
    GST_BUFTYPE_FLOAT, 64, METHOD_CLIP,
          gst_audio_amplify_transform_gdouble_clip}, {
    GST_BUFTYPE_FLOAT, 64, METHOD_WRAP_NEGATIVE,
          gst_audio_amplify_transform_gdouble_wrap_negative}, {
    GST_BUFTYPE_FLOAT, 64, METHOD_WRAP_POSITIVE,
          gst_audio_amplify_transform_gdouble_wrap_positive}, {
    GST_BUFTYPE_FLOAT, 64, METHOD_NOCLIP,
          gst_audio_amplify_transform_gdouble_noclip}, {
    GST_BUFTYPE_LINEAR, 8, METHOD_CLIP, gst_audio_amplify_transform_gint8_clip}, {
    GST_BUFTYPE_LINEAR, 8, METHOD_WRAP_NEGATIVE,
          gst_audio_amplify_transform_gint8_wrap_negative}, {
    GST_BUFTYPE_LINEAR, 8, METHOD_WRAP_POSITIVE,
          gst_audio_amplify_transform_gint8_wrap_positive}, {
    GST_BUFTYPE_LINEAR, 8, METHOD_NOCLIP,
          gst_audio_amplify_transform_gint8_noclip}, {
    GST_BUFTYPE_LINEAR, 16, METHOD_CLIP,
          gst_audio_amplify_transform_gint16_clip}, {
    GST_BUFTYPE_LINEAR, 16, METHOD_WRAP_NEGATIVE,
          gst_audio_amplify_transform_gint16_wrap_negative}, {
    GST_BUFTYPE_LINEAR, 16, METHOD_WRAP_POSITIVE,
          gst_audio_amplify_transform_gint16_wrap_positive}, {
    GST_BUFTYPE_LINEAR, 16, METHOD_NOCLIP,
          gst_audio_amplify_transform_gint16_noclip}, {
    GST_BUFTYPE_LINEAR, 32, METHOD_CLIP,
          gst_audio_amplify_transform_gint32_clip}, {
    GST_BUFTYPE_LINEAR, 32, METHOD_WRAP_NEGATIVE,
          gst_audio_amplify_transform_gint32_wrap_negative}, {
    GST_BUFTYPE_LINEAR, 32, METHOD_WRAP_POSITIVE,
          gst_audio_amplify_transform_gint32_wrap_positive}, {
    GST_BUFTYPE_LINEAR, 32, METHOD_NOCLIP,
          gst_audio_amplify_transform_gint32_noclip}, {
    0, 0, 0, NULL}
  };
  const struct process *p;

  for (p = process; p->func; p++)
    if (p->format == format && p->width == width && p->clipping == clipping)
      return p->func;
  return NULL;
}

static gboolean
gst_audio_amplify_set_process_function (GstAudioAmplify * filter, gint
    clipping_method, gint format, gint width)
{
  GstAudioAmplifyProcessFunc process;

  /* set processing function */

  process = gst_audio_amplify_process_function (clipping_method, format, width);
  if (!process) {
    GST_DEBUG ("wrong format");
    return FALSE;
  }

  filter->process = process;
  filter->clipping_method = clipping_method;
  filter->format = format;
  filter->width = width;

  return TRUE;
}

static void
gst_audio_amplify_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAudioAmplify *filter = GST_AUDIO_AMPLIFY (object);

  switch (prop_id) {
    case PROP_AMPLIFICATION:
      filter->amplification = g_value_get_float (value);
      gst_base_transform_set_passthrough (GST_BASE_TRANSFORM (filter),
          filter->amplification == 1.0);
      break;
    case PROP_CLIPPING_METHOD:
      gst_audio_amplify_set_process_function (filter, g_value_get_enum (value),
          filter->format, filter->width);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_audio_amplify_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAudioAmplify *filter = GST_AUDIO_AMPLIFY (object);

  switch (prop_id) {
    case PROP_AMPLIFICATION:
      g_value_set_float (value, filter->amplification);
      break;
    case PROP_CLIPPING_METHOD:
      g_value_set_enum (value, filter->clipping_method);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/* GstAudioFilter vmethod implementations */
static gboolean
gst_audio_amplify_setup (GstAudioFilter * base, GstRingBufferSpec * format)
{
  GstAudioAmplify *filter = GST_AUDIO_AMPLIFY (base);

  return gst_audio_amplify_set_process_function (filter,
      filter->clipping_method, format->type, format->width);
}

/* GstBaseTransform vmethod implementations */
static GstFlowReturn
gst_audio_amplify_transform_ip (GstBaseTransform * base, GstBuffer * buf)
{
  GstAudioAmplify *filter = GST_AUDIO_AMPLIFY (base);
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

  if (gst_base_transform_is_passthrough (base) ||
      G_UNLIKELY (GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_GAP)))
    return GST_FLOW_OK;

  filter->process (filter, GST_BUFFER_DATA (buf), num_samples);

  return GST_FLOW_OK;
}
