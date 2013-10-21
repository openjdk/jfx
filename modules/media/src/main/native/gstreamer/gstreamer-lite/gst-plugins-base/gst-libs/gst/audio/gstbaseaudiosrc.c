/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstbaseaudiosrc.c: 
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
 * SECTION:gstbaseaudiosrc
 * @short_description: Base class for audio sources
 * @see_also: #GstAudioSrc, #GstRingBuffer.
 *
 * This is the base class for audio sources. Subclasses need to implement the
 * ::create_ringbuffer vmethod. This base class will then take care of
 * reading samples from the ringbuffer, synchronisation and flushing.
 *
 * Last reviewed on 2006-09-27 (0.10.12)
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <string.h>

#include "gstbaseaudiosrc.h"

#include "gst/gst-i18n-plugin.h"

GST_DEBUG_CATEGORY_STATIC (gst_base_audio_src_debug);
#define GST_CAT_DEFAULT gst_base_audio_src_debug

GType
gst_base_audio_src_slave_method_get_type (void)
{
  static volatile gsize slave_method_type = 0;
  /* FIXME 0.11: nick should be "retimestamp" not "re-timestamp" */
  static const GEnumValue slave_method[] = {
    {GST_BASE_AUDIO_SRC_SLAVE_RESAMPLE,
        "GST_BASE_AUDIO_SRC_SLAVE_RESAMPLE", "resample"},
    {GST_BASE_AUDIO_SRC_SLAVE_RETIMESTAMP,
        "GST_BASE_AUDIO_SRC_SLAVE_RETIMESTAMP", "re-timestamp"},
    {GST_BASE_AUDIO_SRC_SLAVE_SKEW, "GST_BASE_AUDIO_SRC_SLAVE_SKEW", "skew"},
    {GST_BASE_AUDIO_SRC_SLAVE_NONE, "GST_BASE_AUDIO_SRC_SLAVE_NONE", "none"},
    {0, NULL, NULL},
  };

  if (g_once_init_enter (&slave_method_type)) {
    GType tmp =
        g_enum_register_static ("GstBaseAudioSrcSlaveMethod", slave_method);
    g_once_init_leave (&slave_method_type, tmp);
  }
  return (GType) slave_method_type;
}

#define GST_BASE_AUDIO_SRC_GET_PRIVATE(obj)  \
   (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_BASE_AUDIO_SRC, GstBaseAudioSrcPrivate))

struct _GstBaseAudioSrcPrivate
{
  gboolean provide_clock;

  /* the clock slaving algorithm in use */
  GstBaseAudioSrcSlaveMethod slave_method;
};

/* BaseAudioSrc signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

#define DEFAULT_BUFFER_TIME     ((200 * GST_MSECOND) / GST_USECOND)
#define DEFAULT_LATENCY_TIME    ((10 * GST_MSECOND) / GST_USECOND)
#define DEFAULT_ACTUAL_BUFFER_TIME     -1
#define DEFAULT_ACTUAL_LATENCY_TIME    -1
#define DEFAULT_PROVIDE_CLOCK   TRUE
#define DEFAULT_SLAVE_METHOD    GST_BASE_AUDIO_SRC_SLAVE_SKEW

enum
{
  PROP_0,
  PROP_BUFFER_TIME,
  PROP_LATENCY_TIME,
  PROP_ACTUAL_BUFFER_TIME,
  PROP_ACTUAL_LATENCY_TIME,
  PROP_PROVIDE_CLOCK,
  PROP_SLAVE_METHOD,
  PROP_LAST
};

static void
_do_init (GType type)
{
  GST_DEBUG_CATEGORY_INIT (gst_base_audio_src_debug, "baseaudiosrc", 0,
      "baseaudiosrc element");

#ifdef ENABLE_NLS
  GST_DEBUG ("binding text domain %s to locale dir %s", GETTEXT_PACKAGE,
      LOCALEDIR);
  bindtextdomain (GETTEXT_PACKAGE, LOCALEDIR);
  bind_textdomain_codeset (GETTEXT_PACKAGE, "UTF-8");
#endif /* ENABLE_NLS */
}

GST_BOILERPLATE_FULL (GstBaseAudioSrc, gst_base_audio_src, GstPushSrc,
    GST_TYPE_PUSH_SRC, _do_init);

static void gst_base_audio_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_base_audio_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);
static void gst_base_audio_src_dispose (GObject * object);

static GstStateChangeReturn gst_base_audio_src_change_state (GstElement *
    element, GstStateChange transition);

static GstClock *gst_base_audio_src_provide_clock (GstElement * elem);
static GstClockTime gst_base_audio_src_get_time (GstClock * clock,
    GstBaseAudioSrc * src);

static GstFlowReturn gst_base_audio_src_create (GstBaseSrc * bsrc,
    guint64 offset, guint length, GstBuffer ** buf);
static gboolean gst_base_audio_src_check_get_range (GstBaseSrc * bsrc);

static gboolean gst_base_audio_src_event (GstBaseSrc * bsrc, GstEvent * event);
static void gst_base_audio_src_get_times (GstBaseSrc * bsrc,
    GstBuffer * buffer, GstClockTime * start, GstClockTime * end);
static gboolean gst_base_audio_src_setcaps (GstBaseSrc * bsrc, GstCaps * caps);
static gboolean gst_base_audio_src_query (GstBaseSrc * bsrc, GstQuery * query);
static void gst_base_audio_src_fixate (GstBaseSrc * bsrc, GstCaps * caps);

/* static guint gst_base_audio_src_signals[LAST_SIGNAL] = { 0 }; */

static void
gst_base_audio_src_base_init (gpointer g_class)
{
}

static void
gst_base_audio_src_class_init (GstBaseAudioSrcClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSrcClass *gstbasesrc_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;
  gstbasesrc_class = (GstBaseSrcClass *) klass;

  g_type_class_add_private (klass, sizeof (GstBaseAudioSrcPrivate));

  gobject_class->set_property = gst_base_audio_src_set_property;
  gobject_class->get_property = gst_base_audio_src_get_property;
  gobject_class->dispose = gst_base_audio_src_dispose;

  g_object_class_install_property (gobject_class, PROP_BUFFER_TIME,
      g_param_spec_int64 ("buffer-time", "Buffer Time",
          "Size of audio buffer in microseconds", 1,
          G_MAXINT64, DEFAULT_BUFFER_TIME,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LATENCY_TIME,
      g_param_spec_int64 ("latency-time", "Latency Time",
          "Audio latency in microseconds", 1,
          G_MAXINT64, DEFAULT_LATENCY_TIME,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstBaseAudioSrc:actual-buffer-time:
   *
   * Actual configured size of audio buffer in microseconds.
   *
   * Since: 0.10.20
   **/
  g_object_class_install_property (gobject_class, PROP_ACTUAL_BUFFER_TIME,
      g_param_spec_int64 ("actual-buffer-time", "Actual Buffer Time",
          "Actual configured size of audio buffer in microseconds",
          DEFAULT_ACTUAL_BUFFER_TIME, G_MAXINT64, DEFAULT_ACTUAL_BUFFER_TIME,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  /**
   * GstBaseAudioSrc:actual-latency-time:
   *
   * Actual configured audio latency in microseconds.
   *
   * Since: 0.10.20
   **/
  g_object_class_install_property (gobject_class, PROP_ACTUAL_LATENCY_TIME,
      g_param_spec_int64 ("actual-latency-time", "Actual Latency Time",
          "Actual configured audio latency in microseconds",
          DEFAULT_ACTUAL_LATENCY_TIME, G_MAXINT64, DEFAULT_ACTUAL_LATENCY_TIME,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PROVIDE_CLOCK,
      g_param_spec_boolean ("provide-clock", "Provide Clock",
          "Provide a clock to be used as the global pipeline clock",
          DEFAULT_PROVIDE_CLOCK, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_SLAVE_METHOD,
      g_param_spec_enum ("slave-method", "Slave Method",
          "Algorithm to use to match the rate of the masterclock",
          GST_TYPE_BASE_AUDIO_SRC_SLAVE_METHOD, DEFAULT_SLAVE_METHOD,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_base_audio_src_change_state);
  gstelement_class->provide_clock =
      GST_DEBUG_FUNCPTR (gst_base_audio_src_provide_clock);

  gstbasesrc_class->set_caps = GST_DEBUG_FUNCPTR (gst_base_audio_src_setcaps);
  gstbasesrc_class->event = GST_DEBUG_FUNCPTR (gst_base_audio_src_event);
  gstbasesrc_class->query = GST_DEBUG_FUNCPTR (gst_base_audio_src_query);
  gstbasesrc_class->get_times =
      GST_DEBUG_FUNCPTR (gst_base_audio_src_get_times);
  gstbasesrc_class->create = GST_DEBUG_FUNCPTR (gst_base_audio_src_create);
  gstbasesrc_class->check_get_range =
      GST_DEBUG_FUNCPTR (gst_base_audio_src_check_get_range);
  gstbasesrc_class->fixate = GST_DEBUG_FUNCPTR (gst_base_audio_src_fixate);

  /* ref class from a thread-safe context to work around missing bit of
   * thread-safety in GObject */
  g_type_class_ref (GST_TYPE_AUDIO_CLOCK);
  g_type_class_ref (GST_TYPE_RING_BUFFER);
}

static void
gst_base_audio_src_init (GstBaseAudioSrc * baseaudiosrc,
    GstBaseAudioSrcClass * g_class)
{
  baseaudiosrc->priv = GST_BASE_AUDIO_SRC_GET_PRIVATE (baseaudiosrc);

  baseaudiosrc->buffer_time = DEFAULT_BUFFER_TIME;
  baseaudiosrc->latency_time = DEFAULT_LATENCY_TIME;
  baseaudiosrc->priv->provide_clock = DEFAULT_PROVIDE_CLOCK;
  baseaudiosrc->priv->slave_method = DEFAULT_SLAVE_METHOD;
  /* reset blocksize we use latency time to calculate a more useful 
   * value based on negotiated format. */
  GST_BASE_SRC (baseaudiosrc)->blocksize = 0;

  baseaudiosrc->clock = gst_audio_clock_new ("GstAudioSrcClock",
      (GstAudioClockGetTimeFunc) gst_base_audio_src_get_time, baseaudiosrc);

  /* we are always a live source */
  gst_base_src_set_live (GST_BASE_SRC (baseaudiosrc), TRUE);
  /* we operate in time */
  gst_base_src_set_format (GST_BASE_SRC (baseaudiosrc), GST_FORMAT_TIME);
}

static void
gst_base_audio_src_dispose (GObject * object)
{
  GstBaseAudioSrc *src;

  src = GST_BASE_AUDIO_SRC (object);

  GST_OBJECT_LOCK (src);
  if (src->clock) {
    gst_audio_clock_invalidate (src->clock);
    gst_object_unref (src->clock);
    src->clock = NULL;
  }

  if (src->ringbuffer) {
    gst_object_unparent (GST_OBJECT_CAST (src->ringbuffer));
    src->ringbuffer = NULL;
  }
  GST_OBJECT_UNLOCK (src);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static GstClock *
gst_base_audio_src_provide_clock (GstElement * elem)
{
  GstBaseAudioSrc *src;
  GstClock *clock;

  src = GST_BASE_AUDIO_SRC (elem);

  /* we have no ringbuffer (must be NULL state) */
  if (src->ringbuffer == NULL)
    goto wrong_state;

  if (!gst_ring_buffer_is_acquired (src->ringbuffer))
    goto wrong_state;

  GST_OBJECT_LOCK (src);
  if (!src->priv->provide_clock)
    goto clock_disabled;

  clock = GST_CLOCK_CAST (gst_object_ref (src->clock));
  GST_OBJECT_UNLOCK (src);

  return clock;

  /* ERRORS */
wrong_state:
  {
    GST_DEBUG_OBJECT (src, "ringbuffer not acquired");
    return NULL;
  }
clock_disabled:
  {
    GST_DEBUG_OBJECT (src, "clock provide disabled");
    GST_OBJECT_UNLOCK (src);
    return NULL;
  }
}

static GstClockTime
gst_base_audio_src_get_time (GstClock * clock, GstBaseAudioSrc * src)
{
  guint64 raw, samples;
  guint delay;
  GstClockTime result;

  if (G_UNLIKELY (src->ringbuffer == NULL || src->ringbuffer->spec.rate == 0))
    return GST_CLOCK_TIME_NONE;

  raw = samples = gst_ring_buffer_samples_done (src->ringbuffer);

  /* the number of samples not yet processed, this is still queued in the
   * device (not yet read for capture). */
  delay = gst_ring_buffer_delay (src->ringbuffer);

  samples += delay;

  result = gst_util_uint64_scale_int (samples, GST_SECOND,
      src->ringbuffer->spec.rate);

  GST_DEBUG_OBJECT (src,
      "processed samples: raw %" G_GUINT64_FORMAT ", delay %u, real %"
      G_GUINT64_FORMAT ", time %" GST_TIME_FORMAT, raw, delay, samples,
      GST_TIME_ARGS (result));

  return result;
}

static gboolean
gst_base_audio_src_check_get_range (GstBaseSrc * bsrc)
{
  /* we allow limited pull base operation of which the details
   * will eventually exposed in an as of yet non-existing query.
   * Basically pulling can be done on any number of bytes as long
   * as the offset is -1 or sequentially increasing. */
  return TRUE;
}

/**
 * gst_base_audio_src_set_provide_clock:
 * @src: a #GstBaseAudioSrc
 * @provide: new state
 *
 * Controls whether @src will provide a clock or not. If @provide is %TRUE, 
 * gst_element_provide_clock() will return a clock that reflects the datarate
 * of @src. If @provide is %FALSE, gst_element_provide_clock() will return NULL.
 *
 * Since: 0.10.16
 */
void
gst_base_audio_src_set_provide_clock (GstBaseAudioSrc * src, gboolean provide)
{
  g_return_if_fail (GST_IS_BASE_AUDIO_SRC (src));

  GST_OBJECT_LOCK (src);
  src->priv->provide_clock = provide;
  GST_OBJECT_UNLOCK (src);
}

/**
 * gst_base_audio_src_get_provide_clock:
 * @src: a #GstBaseAudioSrc
 *
 * Queries whether @src will provide a clock or not. See also
 * gst_base_audio_src_set_provide_clock.
 *
 * Returns: %TRUE if @src will provide a clock.
 *
 * Since: 0.10.16
 */
gboolean
gst_base_audio_src_get_provide_clock (GstBaseAudioSrc * src)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_BASE_AUDIO_SRC (src), FALSE);

  GST_OBJECT_LOCK (src);
  result = src->priv->provide_clock;
  GST_OBJECT_UNLOCK (src);

  return result;
}

/**
 * gst_base_audio_src_set_slave_method:
 * @src: a #GstBaseAudioSrc
 * @method: the new slave method
 *
 * Controls how clock slaving will be performed in @src. 
 *
 * Since: 0.10.20
 */
void
gst_base_audio_src_set_slave_method (GstBaseAudioSrc * src,
    GstBaseAudioSrcSlaveMethod method)
{
  g_return_if_fail (GST_IS_BASE_AUDIO_SRC (src));

  GST_OBJECT_LOCK (src);
  src->priv->slave_method = method;
  GST_OBJECT_UNLOCK (src);
}

/**
 * gst_base_audio_src_get_slave_method:
 * @src: a #GstBaseAudioSrc
 *
 * Get the current slave method used by @src.
 *
 * Returns: The current slave method used by @src.
 *
 * Since: 0.10.20
 */
GstBaseAudioSrcSlaveMethod
gst_base_audio_src_get_slave_method (GstBaseAudioSrc * src)
{
  GstBaseAudioSrcSlaveMethod result;

  g_return_val_if_fail (GST_IS_BASE_AUDIO_SRC (src), -1);

  GST_OBJECT_LOCK (src);
  result = src->priv->slave_method;
  GST_OBJECT_UNLOCK (src);

  return result;
}

static void
gst_base_audio_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstBaseAudioSrc *src;

  src = GST_BASE_AUDIO_SRC (object);

  switch (prop_id) {
    case PROP_BUFFER_TIME:
      src->buffer_time = g_value_get_int64 (value);
      break;
    case PROP_LATENCY_TIME:
      src->latency_time = g_value_get_int64 (value);
      break;
    case PROP_PROVIDE_CLOCK:
      gst_base_audio_src_set_provide_clock (src, g_value_get_boolean (value));
      break;
    case PROP_SLAVE_METHOD:
      gst_base_audio_src_set_slave_method (src, g_value_get_enum (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_base_audio_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstBaseAudioSrc *src;

  src = GST_BASE_AUDIO_SRC (object);

  switch (prop_id) {
    case PROP_BUFFER_TIME:
      g_value_set_int64 (value, src->buffer_time);
      break;
    case PROP_LATENCY_TIME:
      g_value_set_int64 (value, src->latency_time);
      break;
    case PROP_ACTUAL_BUFFER_TIME:
      GST_OBJECT_LOCK (src);
      if (src->ringbuffer && src->ringbuffer->acquired)
        g_value_set_int64 (value, src->ringbuffer->spec.buffer_time);
      else
        g_value_set_int64 (value, DEFAULT_ACTUAL_BUFFER_TIME);
      GST_OBJECT_UNLOCK (src);
      break;
    case PROP_ACTUAL_LATENCY_TIME:
      GST_OBJECT_LOCK (src);
      if (src->ringbuffer && src->ringbuffer->acquired)
        g_value_set_int64 (value, src->ringbuffer->spec.latency_time);
      else
        g_value_set_int64 (value, DEFAULT_ACTUAL_LATENCY_TIME);
      GST_OBJECT_UNLOCK (src);
      break;
    case PROP_PROVIDE_CLOCK:
      g_value_set_boolean (value, gst_base_audio_src_get_provide_clock (src));
      break;
    case PROP_SLAVE_METHOD:
      g_value_set_enum (value, gst_base_audio_src_get_slave_method (src));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_base_audio_src_fixate (GstBaseSrc * bsrc, GstCaps * caps)
{
  GstStructure *s;
  gint width, depth;

  s = gst_caps_get_structure (caps, 0);

  /* fields for all formats */
  gst_structure_fixate_field_nearest_int (s, "rate", 44100);
  gst_structure_fixate_field_nearest_int (s, "channels", 2);
  gst_structure_fixate_field_nearest_int (s, "width", 16);

  /* fields for int */
  if (gst_structure_has_field (s, "depth")) {
    gst_structure_get_int (s, "width", &width);
    /* round width to nearest multiple of 8 for the depth */
    depth = GST_ROUND_UP_8 (width);
    gst_structure_fixate_field_nearest_int (s, "depth", depth);
  }
  if (gst_structure_has_field (s, "signed"))
    gst_structure_fixate_field_boolean (s, "signed", TRUE);
  if (gst_structure_has_field (s, "endianness"))
    gst_structure_fixate_field_nearest_int (s, "endianness", G_BYTE_ORDER);
}

static gboolean
gst_base_audio_src_setcaps (GstBaseSrc * bsrc, GstCaps * caps)
{
  GstBaseAudioSrc *src = GST_BASE_AUDIO_SRC (bsrc);
  GstRingBufferSpec *spec;

  spec = &src->ringbuffer->spec;

  spec->buffer_time = src->buffer_time;
  spec->latency_time = src->latency_time;

  GST_OBJECT_LOCK (src);
  if (!gst_ring_buffer_parse_caps (spec, caps))
  {
    GST_OBJECT_UNLOCK (src);
    goto parse_error;
  }

  /* calculate suggested segsize and segtotal */
  spec->segsize =
      spec->rate * spec->bytes_per_sample * spec->latency_time / GST_MSECOND;
  spec->segtotal = spec->buffer_time / spec->latency_time;

  GST_OBJECT_UNLOCK (src);

  GST_DEBUG ("release old ringbuffer");

  gst_ring_buffer_release (src->ringbuffer);

  gst_ring_buffer_debug_spec_buff (spec);

  GST_DEBUG ("acquire new ringbuffer");

  if (!gst_ring_buffer_acquire (src->ringbuffer, spec))
    goto acquire_error;

  /* calculate actual latency and buffer times */
  spec->latency_time =
      spec->segsize * GST_MSECOND / (spec->rate * spec->bytes_per_sample);
  spec->buffer_time =
      spec->segtotal * spec->segsize * GST_MSECOND / (spec->rate *
      spec->bytes_per_sample);

  gst_ring_buffer_debug_spec_buff (spec);

  g_object_notify (G_OBJECT (src), "actual-buffer-time");
  g_object_notify (G_OBJECT (src), "actual-latency-time");

  return TRUE;

  /* ERRORS */
parse_error:
  {
    GST_DEBUG ("could not parse caps");
    return FALSE;
  }
acquire_error:
  {
    GST_DEBUG ("could not acquire ringbuffer");
    return FALSE;
  }
}

static void
gst_base_audio_src_get_times (GstBaseSrc * bsrc, GstBuffer * buffer,
    GstClockTime * start, GstClockTime * end)
{
  /* no need to sync to a clock here, we schedule the samples based
   * on our own clock for the moment. */
  *start = GST_CLOCK_TIME_NONE;
  *end = GST_CLOCK_TIME_NONE;
}

static gboolean
gst_base_audio_src_query (GstBaseSrc * bsrc, GstQuery * query)
{
  GstBaseAudioSrc *src = GST_BASE_AUDIO_SRC (bsrc);
  gboolean res = FALSE;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_LATENCY:
    {
      GstClockTime min_latency, max_latency;
      GstRingBufferSpec *spec;

      GST_OBJECT_LOCK (src);
      if (G_UNLIKELY (src->ringbuffer == NULL
              || src->ringbuffer->spec.rate == 0)) {
        GST_OBJECT_UNLOCK (src);
        goto done;
      }

      spec = &src->ringbuffer->spec;

      /* we have at least 1 segment of latency */
      min_latency =
          gst_util_uint64_scale_int (spec->segsize, GST_SECOND,
          spec->rate * spec->bytes_per_sample);
      /* we cannot delay more than the buffersize else we lose data */
      max_latency =
          gst_util_uint64_scale_int (spec->segtotal * spec->segsize, GST_SECOND,
          spec->rate * spec->bytes_per_sample);
      GST_OBJECT_UNLOCK (src);

      GST_DEBUG_OBJECT (src,
          "report latency min %" GST_TIME_FORMAT " max %" GST_TIME_FORMAT,
          GST_TIME_ARGS (min_latency), GST_TIME_ARGS (max_latency));

      /* we are always live, the min latency is 1 segment and the max latency is
       * the complete buffer of segments. */
      gst_query_set_latency (query, TRUE, min_latency, max_latency);

      res = TRUE;
      break;
    }
    default:
      res = GST_BASE_SRC_CLASS (parent_class)->query (bsrc, query);
      break;
  }
done:
  return res;
}

static gboolean
gst_base_audio_src_event (GstBaseSrc * bsrc, GstEvent * event)
{
  GstBaseAudioSrc *src = GST_BASE_AUDIO_SRC (bsrc);
  gboolean res;

  res = TRUE;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_FLUSH_START:
      GST_DEBUG_OBJECT (bsrc, "flush-start");
      gst_ring_buffer_pause (src->ringbuffer);
      gst_ring_buffer_clear_all (src->ringbuffer);
      break;
    case GST_EVENT_FLUSH_STOP:
      GST_DEBUG_OBJECT (bsrc, "flush-stop");
      /* always resync on sample after a flush */
      src->next_sample = -1;
      gst_ring_buffer_clear_all (src->ringbuffer);
      break;
    case GST_EVENT_SEEK:
      GST_DEBUG_OBJECT (bsrc, "refuse to seek");
      res = FALSE;
      break;
    default:
      GST_DEBUG_OBJECT (bsrc, "dropping event %p", event);
      break;
  }
  return res;
}

/* get the next offset in the ringbuffer for reading samples.
 * If the next sample is too far away, this function will position itself to the
 * next most recent sample, creating discontinuity */
static guint64
gst_base_audio_src_get_offset (GstBaseAudioSrc * src)
{
  guint64 sample;
  gint readseg, segdone, segtotal, sps;
  gint diff;

  /* assume we can append to the previous sample */
  sample = src->next_sample;

  sps = src->ringbuffer->samples_per_seg;
  segtotal = src->ringbuffer->spec.segtotal;

  /* get the currently processed segment */
  segdone = g_atomic_int_get (&src->ringbuffer->segdone)
      - src->ringbuffer->segbase;

  if (sample != -1) {
    GST_DEBUG_OBJECT (src, "at segment %d and sample %" G_GUINT64_FORMAT,
        segdone, sample);
    /* figure out the segment and the offset inside the segment where
     * the sample should be read from. */
    readseg = sample / sps;

    /* see how far away it is from the read segment, normally segdone (where new
     * data is written in the ringbuffer) is bigger than readseg (where we are
     * reading). */
    diff = segdone - readseg;
    if (diff >= segtotal) {
      GST_DEBUG_OBJECT (src, "dropped, align to segment %d", segdone);
      /* sample would be dropped, position to next playable position */
      sample = ((guint64) (segdone)) * sps;
    }
  } else {
    /* no previous sample, go to the current position */
    GST_DEBUG_OBJECT (src, "first sample, align to current %d", segdone);
    sample = ((guint64) (segdone)) * sps;
    readseg = segdone;
  }

  GST_DEBUG_OBJECT (src,
      "reading from %d, we are at %d, sample %" G_GUINT64_FORMAT, readseg,
      segdone, sample);

  return sample;
}

static GstFlowReturn
gst_base_audio_src_create (GstBaseSrc * bsrc, guint64 offset, guint length,
    GstBuffer ** outbuf)
{
  GstBaseAudioSrc *src = GST_BASE_AUDIO_SRC (bsrc);
  GstBuffer *buf;
  guchar *data;
  guint samples, total_samples;
  guint64 sample;
  gint bps;
  GstRingBuffer *ringbuffer;
  GstRingBufferSpec *spec;
  guint read;
  GstClockTime timestamp, duration;
  GstClock *clock;

  ringbuffer = src->ringbuffer;
  spec = &ringbuffer->spec;

  if (G_UNLIKELY (!gst_ring_buffer_is_acquired (ringbuffer)))
    goto wrong_state;

  bps = spec->bytes_per_sample;

  if ((length == 0 && bsrc->blocksize == 0) || length == -1)
    /* no length given, use the default segment size */
    length = spec->segsize;
  else
    /* make sure we round down to an integral number of samples */
    length -= length % bps;

  /* figure out the offset in the ringbuffer */
  if (G_UNLIKELY (offset != -1)) {
    sample = offset / bps;
    /* if a specific offset was given it must be the next sequential
     * offset we expect or we fail for now. */
    if (src->next_sample != -1 && sample != src->next_sample)
      goto wrong_offset;
  } else {
    /* calculate the sequentially next sample we need to read. This can jump and
     * create a DISCONT. */
    sample = gst_base_audio_src_get_offset (src);
  }

  GST_DEBUG_OBJECT (src, "reading from sample %" G_GUINT64_FORMAT, sample);

  /* get the number of samples to read */
  total_samples = samples = length / bps;

  /* FIXME, using a bufferpool would be nice here */
  buf = gst_buffer_new_and_alloc (length);
  data = GST_BUFFER_DATA (buf);

  do {
    read = gst_ring_buffer_read (ringbuffer, sample, data, samples);
    GST_DEBUG_OBJECT (src, "read %u of %u", read, samples);
    /* if we read all, we're done */
    if (read == samples)
      break;

    /* else something interrupted us and we wait for playing again. */
    GST_DEBUG_OBJECT (src, "wait playing");
    if (gst_base_src_wait_playing (bsrc) != GST_FLOW_OK)
      goto stopped;

    GST_DEBUG_OBJECT (src, "continue playing");

    /* read next samples */
    sample += read;
    samples -= read;
    data += read * bps;
  } while (TRUE);

  /* mark discontinuity if needed */
  if (G_UNLIKELY (sample != src->next_sample) && src->next_sample != -1) {
    GST_WARNING_OBJECT (src,
        "create DISCONT of %" G_GUINT64_FORMAT " samples at sample %"
        G_GUINT64_FORMAT, sample - src->next_sample, sample);
    GST_ELEMENT_WARNING (src, CORE, CLOCK,
        (_("Can't record audio fast enough")),
        ("Dropped %" G_GUINT64_FORMAT " samples. This is most likely because "
            "downstream can't keep up and is consuming samples too slowly.",
            sample - src->next_sample));
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DISCONT);
  }

  src->next_sample = sample + samples;

  /* get the normal timestamp to get the duration. */
  timestamp = gst_util_uint64_scale_int (sample, GST_SECOND, spec->rate);
  duration = gst_util_uint64_scale_int (src->next_sample, GST_SECOND,
      spec->rate) - timestamp;

  GST_OBJECT_LOCK (src);
  if (!(clock = GST_ELEMENT_CLOCK (src)))
    goto no_sync;

  if (clock != src->clock) {
    /* we are slaved, check how to handle this */
    switch (src->priv->slave_method) {
      case GST_BASE_AUDIO_SRC_SLAVE_RESAMPLE:
        /* not implemented, use skew algorithm. This algorithm should
         * work on the readout pointer and produces more or less samples based
         * on the clock drift */
      case GST_BASE_AUDIO_SRC_SLAVE_SKEW:
      {
        GstClockTime running_time;
        GstClockTime base_time;
        GstClockTime current_time;
        guint64 running_time_sample;
        gint running_time_segment;
        gint last_read_segment;
        gint segment_skew;
        gint sps;
        gint segments_written;
        gint last_written_segment;

        /* get the amount of segments written from the device by now */
        segments_written = g_atomic_int_get (&ringbuffer->segdone);

        /* subtract the base to segments_written to get the number of the
           last written segment in the ringbuffer (one segment written = segment 0) */
        last_written_segment = segments_written - ringbuffer->segbase - 1;

        /* samples per segment */
        sps = ringbuffer->samples_per_seg;

        /* get the current time */
        current_time = gst_clock_get_time (clock);

        /* get the basetime */
        base_time = GST_ELEMENT_CAST (src)->base_time;

        /* get the running_time */
        running_time = current_time - base_time;

        /* the running_time converted to a sample (relative to the ringbuffer) */
        running_time_sample =
            gst_util_uint64_scale_int (running_time, spec->rate, GST_SECOND);

        /* the segmentnr corrensponding to running_time, round down */
        running_time_segment = running_time_sample / sps;

        /* the segment currently read from the ringbuffer */
        last_read_segment = sample / sps;

        /* the skew we have between running_time and the ringbuffertime (last written to) */
        segment_skew = running_time_segment - last_written_segment;

        GST_DEBUG_OBJECT (bsrc,
            "\n running_time                                              = %"
            GST_TIME_FORMAT
            "\n timestamp                                                  = %"
            GST_TIME_FORMAT
            "\n running_time_segment                                       = %d"
            "\n last_written_segment                                       = %d"
            "\n segment_skew (running time segment - last_written_segment) = %d"
            "\n last_read_segment                                          = %d",
            GST_TIME_ARGS (running_time), GST_TIME_ARGS (timestamp),
            running_time_segment, last_written_segment, segment_skew,
            last_read_segment);

        /* Resync the ringbuffer if:
         *
         * 1. We are more than the length of the ringbuffer behind.
         *    The length of the ringbuffer then gets to dictate
         *    the threshold for what is concidered "too late"
         *
         * 2. If this is our first buffer.
         *    We know that we should catch up to running_time
         *    the first time we are ran.
         */
        if ((segment_skew >= ringbuffer->spec.segtotal) ||
            (last_read_segment == 0)) {
          gint new_read_segment;
          gint segment_diff;
          guint64 new_sample;

          /* the difference between running_time and the last written segment */
          segment_diff = running_time_segment - last_written_segment;

          /* advance the ringbuffer */
          gst_ring_buffer_advance (ringbuffer, segment_diff);

          /* we move the  new read segment to the last known written segment */
          new_read_segment =
              g_atomic_int_get (&ringbuffer->segdone) - ringbuffer->segbase;

          /* we calculate the new sample value */
          new_sample = ((guint64) new_read_segment) * sps;

          /* and get the relative time to this -> our new timestamp */
          timestamp =
              gst_util_uint64_scale_int (new_sample, GST_SECOND, spec->rate);

          /* we update the next sample accordingly */
          src->next_sample = new_sample + samples;

          GST_DEBUG_OBJECT (bsrc,
              "Timeshifted the ringbuffer with %d segments: "
              "Updating the timestamp to %" GST_TIME_FORMAT ", "
              "and src->next_sample to %" G_GUINT64_FORMAT, segment_diff,
              GST_TIME_ARGS (timestamp), src->next_sample);
        }
        break;
      }
      case GST_BASE_AUDIO_SRC_SLAVE_RETIMESTAMP:
      {
        GstClockTime base_time, latency;

        /* We are slaved to another clock, take running time of the pipeline clock and
         * timestamp against it. Somebody else in the pipeline should figure out the
         * clock drift. We keep the duration we calculated above. */
        timestamp = gst_clock_get_time (clock);
        base_time = GST_ELEMENT_CAST (src)->base_time;

        if (GST_CLOCK_DIFF (timestamp, base_time) < 0)
          timestamp -= base_time;
        else
          timestamp = 0;

        /* subtract latency */
        latency =
            gst_util_uint64_scale_int (total_samples, GST_SECOND, spec->rate);
        if (timestamp > latency)
          timestamp -= latency;
        else
          timestamp = 0;
      }
      case GST_BASE_AUDIO_SRC_SLAVE_NONE:
        break;
    }
  } else {
    GstClockTime base_time;

    /* to get the timestamp against the clock we also need to add our offset */
    timestamp = gst_audio_clock_adjust (clock, timestamp);

    /* we are not slaved, subtract base_time */
    base_time = GST_ELEMENT_CAST (src)->base_time;

    if (GST_CLOCK_DIFF (timestamp, base_time) < 0) {
      timestamp -= base_time;
      GST_LOG_OBJECT (src,
          "buffer timestamp %" GST_TIME_FORMAT " (base_time %" GST_TIME_FORMAT
          ")", GST_TIME_ARGS (timestamp), GST_TIME_ARGS (base_time));
    } else {
      GST_LOG_OBJECT (src,
          "buffer timestamp 0, ts %" GST_TIME_FORMAT " <= base_time %"
          GST_TIME_FORMAT, GST_TIME_ARGS (timestamp),
          GST_TIME_ARGS (base_time));
      timestamp = 0;
    }
  }

no_sync:
  GST_OBJECT_UNLOCK (src);

  GST_BUFFER_TIMESTAMP (buf) = timestamp;
  GST_BUFFER_DURATION (buf) = duration;
  GST_BUFFER_OFFSET (buf) = sample;
  GST_BUFFER_OFFSET_END (buf) = sample + samples;

  *outbuf = buf;

  return GST_FLOW_OK;

  /* ERRORS */
wrong_state:
  {
    GST_DEBUG_OBJECT (src, "ringbuffer in wrong state");
    return GST_FLOW_WRONG_STATE;
  }
wrong_offset:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, SEEK,
        (NULL), ("resource can only be operated on sequentially but offset %"
            G_GUINT64_FORMAT " was given", offset));
    return GST_FLOW_ERROR;
  }
stopped:
  {
    gst_buffer_unref (buf);
    GST_DEBUG_OBJECT (src, "ringbuffer stopped");
    return GST_FLOW_WRONG_STATE;
  }
}

/**
 * gst_base_audio_src_create_ringbuffer:
 * @src: a #GstBaseAudioSrc.
 *
 * Create and return the #GstRingBuffer for @src. This function will call the
 * ::create_ringbuffer vmethod and will set @src as the parent of the returned
 * buffer (see gst_object_set_parent()).
 *
 * Returns: The new ringbuffer of @src.
 */
GstRingBuffer *
gst_base_audio_src_create_ringbuffer (GstBaseAudioSrc * src)
{
  GstBaseAudioSrcClass *bclass;
  GstRingBuffer *buffer = NULL;

  bclass = GST_BASE_AUDIO_SRC_GET_CLASS (src);
  if (bclass->create_ringbuffer)
    buffer = bclass->create_ringbuffer (src);

  if (G_LIKELY (buffer))
    gst_object_set_parent (GST_OBJECT_CAST (buffer), GST_OBJECT_CAST (src));

  return buffer;
}

static GstStateChangeReturn
gst_base_audio_src_change_state (GstElement * element,
    GstStateChange transition)
{
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
  GstBaseAudioSrc *src = GST_BASE_AUDIO_SRC (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      GST_DEBUG_OBJECT (src, "NULL->READY");
      GST_OBJECT_LOCK (src);
      if (src->ringbuffer == NULL) {
        gst_audio_clock_reset (GST_AUDIO_CLOCK (src->clock), 0);
        src->ringbuffer = gst_base_audio_src_create_ringbuffer (src);
      }
      GST_OBJECT_UNLOCK (src);
      if (!gst_ring_buffer_open_device (src->ringbuffer))
        goto open_failed;
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      GST_DEBUG_OBJECT (src, "READY->PAUSED");
      src->next_sample = -1;
      gst_ring_buffer_set_flushing (src->ringbuffer, FALSE);
      gst_ring_buffer_may_start (src->ringbuffer, FALSE);
      /* Only post clock-provide messages if this is the clock that
       * we've created. If the subclass has overriden it the subclass
       * should post this messages whenever necessary */
      if (src->clock && GST_IS_AUDIO_CLOCK (src->clock) &&
          GST_AUDIO_CLOCK_CAST (src->clock)->func ==
          (GstAudioClockGetTimeFunc) gst_base_audio_src_get_time)
        gst_element_post_message (element,
            gst_message_new_clock_provide (GST_OBJECT_CAST (element),
                src->clock, TRUE));
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      GST_DEBUG_OBJECT (src, "PAUSED->PLAYING");
      gst_ring_buffer_may_start (src->ringbuffer, TRUE);
      break;
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      GST_DEBUG_OBJECT (src, "PLAYING->PAUSED");
      gst_ring_buffer_may_start (src->ringbuffer, FALSE);
      gst_ring_buffer_pause (src->ringbuffer);
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      GST_DEBUG_OBJECT (src, "PAUSED->READY");
      /* Only post clock-lost messages if this is the clock that
       * we've created. If the subclass has overriden it the subclass
       * should post this messages whenever necessary */
      if (src->clock && GST_IS_AUDIO_CLOCK (src->clock) &&
          GST_AUDIO_CLOCK_CAST (src->clock)->func ==
          (GstAudioClockGetTimeFunc) gst_base_audio_src_get_time)
        gst_element_post_message (element,
            gst_message_new_clock_lost (GST_OBJECT_CAST (element), src->clock));
      gst_ring_buffer_set_flushing (src->ringbuffer, TRUE);
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      GST_DEBUG_OBJECT (src, "PAUSED->READY");
      gst_ring_buffer_release (src->ringbuffer);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      GST_DEBUG_OBJECT (src, "READY->NULL");
      gst_ring_buffer_close_device (src->ringbuffer);
      GST_OBJECT_LOCK (src);
      gst_object_unparent (GST_OBJECT_CAST (src->ringbuffer));
      src->ringbuffer = NULL;
      GST_OBJECT_UNLOCK (src);
      break;
    default:
      break;
  }

  return ret;

  /* ERRORS */
open_failed:
  {
    /* subclass must post a meaningfull error message */
    GST_DEBUG_OBJECT (src, "open failed");
    return GST_STATE_CHANGE_FAILURE;
  }

}
