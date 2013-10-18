/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim@fluendo.com>
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
 * SECTION:gstringbuffer
 * @short_description: Base class for audio ringbuffer implementations
 * @see_also: #GstBaseAudioSink, #GstAudioSink
 *
 * <refsect2>
 * <para>
 * This object is the base class for audio ringbuffers used by the base
 * audio source and sink classes.
 * </para>
 * <para>
 * The ringbuffer abstracts a circular buffer of data. One reader and
 * one writer can operate on the data from different threads in a lockfree
 * manner. The base class is sufficiently flexible to be used as an
 * abstraction for DMA based ringbuffers as well as a pure software
 * implementations.
 * </para>
 * </refsect2>
 *
 * Last reviewed on 2006-02-02 (0.10.4)
 */

#include <string.h>

#include "gstringbuffer.h"

GST_DEBUG_CATEGORY_STATIC (gst_ring_buffer_debug);
#define GST_CAT_DEFAULT gst_ring_buffer_debug

static void gst_ring_buffer_dispose (GObject * object);
static void gst_ring_buffer_finalize (GObject * object);

static gboolean gst_ring_buffer_pause_unlocked (GstRingBuffer * buf);
static void default_clear_all (GstRingBuffer * buf);
static guint default_commit (GstRingBuffer * buf, guint64 * sample,
    guchar * data, gint in_samples, gint out_samples, gint * accum);

/* ringbuffer abstract base class */
G_DEFINE_ABSTRACT_TYPE (GstRingBuffer, gst_ring_buffer, GST_TYPE_OBJECT);

static void
gst_ring_buffer_class_init (GstRingBufferClass * klass)
{
  GObjectClass *gobject_class;
  GstRingBufferClass *gstringbuffer_class;

  gobject_class = (GObjectClass *) klass;
  gstringbuffer_class = (GstRingBufferClass *) klass;

  GST_DEBUG_CATEGORY_INIT (gst_ring_buffer_debug, "ringbuffer", 0,
      "ringbuffer class");

  gobject_class->dispose = gst_ring_buffer_dispose;
  gobject_class->finalize = gst_ring_buffer_finalize;

  gstringbuffer_class->clear_all = GST_DEBUG_FUNCPTR (default_clear_all);
  gstringbuffer_class->commit = GST_DEBUG_FUNCPTR (default_commit);
}

static void
gst_ring_buffer_init (GstRingBuffer * ringbuffer)
{
  ringbuffer->open = FALSE;
  ringbuffer->acquired = FALSE;
  ringbuffer->state = GST_RING_BUFFER_STATE_STOPPED;
  ringbuffer->cond = g_cond_new ();
  ringbuffer->waiting = 0;
  ringbuffer->empty_seg = NULL;
  ringbuffer->abidata.ABI.flushing = TRUE;
}

static void
gst_ring_buffer_dispose (GObject * object)
{
  GstRingBuffer *ringbuffer = GST_RING_BUFFER (object);

  gst_caps_replace (&ringbuffer->spec.caps, NULL);

  G_OBJECT_CLASS (gst_ring_buffer_parent_class)->dispose (G_OBJECT
      (ringbuffer));
}

static void
gst_ring_buffer_finalize (GObject * object)
{
  GstRingBuffer *ringbuffer = GST_RING_BUFFER (object);

  g_cond_free (ringbuffer->cond);
  g_free (ringbuffer->empty_seg);

  G_OBJECT_CLASS (gst_ring_buffer_parent_class)->finalize (G_OBJECT
      (ringbuffer));
}

typedef struct
{
  const GstBufferFormat format;
  const guint8 silence[4];
} FormatDef;

static const FormatDef linear_defs[4 * 2 * 2] = {
  {GST_S8, {0x00, 0x00, 0x00, 0x00}},
  {GST_S8, {0x00, 0x00, 0x00, 0x00}},
  {GST_U8, {0x80, 0x80, 0x80, 0x80}},
  {GST_U8, {0x80, 0x80, 0x80, 0x80}},
  {GST_S16_LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S16_BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U16_LE, {0x00, 0x80, 0x00, 0x80}},
  {GST_U16_BE, {0x80, 0x00, 0x80, 0x00}},
  {GST_S24_LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S24_BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U24_LE, {0x00, 0x00, 0x80, 0x00}},
  {GST_U24_BE, {0x80, 0x00, 0x00, 0x00}},
  {GST_S32_LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S32_BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U32_LE, {0x00, 0x00, 0x00, 0x80}},
  {GST_U32_BE, {0x80, 0x00, 0x00, 0x00}}
};

static const FormatDef linear24_defs[3 * 2 * 2] = {
  {GST_S24_3LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S24_3BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U24_3LE, {0x00, 0x00, 0x80, 0x00}},
  {GST_U24_3BE, {0x80, 0x00, 0x00, 0x00}},
  {GST_S20_3LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S20_3BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U20_3LE, {0x00, 0x00, 0x08, 0x00}},
  {GST_U20_3BE, {0x08, 0x00, 0x00, 0x00}},
  {GST_S18_3LE, {0x00, 0x00, 0x00, 0x00}},
  {GST_S18_3BE, {0x00, 0x00, 0x00, 0x00}},
  {GST_U18_3LE, {0x00, 0x00, 0x02, 0x00}},
  {GST_U18_3BE, {0x02, 0x00, 0x00, 0x00}}
};

static const FormatDef *
build_linear_format (int depth, int width, int unsignd, int big_endian)
{
  const FormatDef *formats;

  if (width == 24) {
    switch (depth) {
      case 24:
        formats = &linear24_defs[0];
        break;
      case 20:
        formats = &linear24_defs[4];
        break;
      case 18:
        formats = &linear24_defs[8];
        break;
      default:
        return NULL;
    }
  } else {
    switch (depth) {
      case 8:
        formats = &linear_defs[0];
        break;
      case 16:
        formats = &linear_defs[4];
        break;
      case 24:
        formats = &linear_defs[8];
        break;
      case 32:
        formats = &linear_defs[12];
        break;
      default:
        return NULL;
    }
  }
  if (unsignd)
    formats += 2;
  if (big_endian)
    formats += 1;

  return formats;
}

#ifndef GSTREAMER_LITE
#ifndef GST_DISABLE_GST_DEBUG
static const gchar *format_type_names[] = {
  "linear",
  "float",
  "mu law",
  "a law",
  "ima adpcm",
  "mpeg",
  "gsm",
  "iec958",
  "ac3",
  "eac3",
  "dts"
};

static const gchar *format_names[] = {
  "unknown",
  "s8",
  "u8",
  "s16_le",
  "s16_be",
  "u16_le",
  "u16_be",
  "s24_le",
  "s24_be",
  "u24_le",
  "u24_be",
  "s32_le",
  "s32_be",
  "u32_le",
  "u32_be",
  "s24_3le",
  "s24_3be",
  "u24_3le",
  "u24_3be",
  "s20_3le",
  "s20_3be",
  "u20_3le",
  "u20_3be",
  "s18_3le",
  "s18_3be",
  "u18_3le",
  "u18_3be",
  "float32_le",
  "float32_be",
  "float64_le",
  "float64_be",
  "mu_law",
  "a_law",
  "ima_adpcm",
  "mpeg",
  "gsm",
  "iec958",
  "ac3",
  "eac3",
  "dts"
};
#endif
#else // GSTREAMER_LITE
#ifdef GST_DISABLE_GST_DEBUG
static const gchar *format_type_names[] = {
  "linear",
  "float",
  "mu law",
  "a law",
  "ima adpcm",
  "mpeg",
  "gsm",
  "iec958",
  "ac3",
  "eac3",
  "dts"
};

static const gchar *format_names[] = {
  "unknown",
  "s8",
  "u8",
  "s16_le",
  "s16_be",
  "u16_le",
  "u16_be",
  "s24_le",
  "s24_be",
  "u24_le",
  "u24_be",
  "s32_le",
  "s32_be",
  "u32_le",
  "u32_be",
  "s24_3le",
  "s24_3be",
  "u24_3le",
  "u24_3be",
  "s20_3le",
  "s20_3be",
  "u20_3le",
  "u20_3be",
  "s18_3le",
  "s18_3be",
  "u18_3le",
  "u18_3be",
  "float32_le",
  "float32_be",
  "float64_le",
  "float64_be",
  "mu_law",
  "a_law",
  "ima_adpcm",
  "mpeg",
  "gsm",
  "iec958",
  "ac3",
  "eac3",
  "dts"
};
#endif
#endif // GSTREAMER_LITE

/**
 * gst_ring_buffer_debug_spec_caps:
 * @spec: the spec to debug
 *
 * Print debug info about the parsed caps in @spec to the debug log.
 */
void
gst_ring_buffer_debug_spec_caps (GstRingBufferSpec * spec)
{
  gint i, bytes;

  GST_DEBUG ("spec caps: %p %" GST_PTR_FORMAT, spec->caps, spec->caps);
  GST_DEBUG ("parsed caps: type:         %d, '%s'", spec->type,
      format_type_names[spec->type]);
  GST_DEBUG ("parsed caps: format:       %d, '%s'", spec->format,
      format_names[spec->format]);
  GST_DEBUG ("parsed caps: width:        %d", spec->width);
  GST_DEBUG ("parsed caps: depth:        %d", spec->depth);
  GST_DEBUG ("parsed caps: sign:         %d", spec->sign);
  GST_DEBUG ("parsed caps: bigend:       %d", spec->bigend);
  GST_DEBUG ("parsed caps: rate:         %d", spec->rate);
  GST_DEBUG ("parsed caps: channels:     %d", spec->channels);
  GST_DEBUG ("parsed caps: sample bytes: %d", spec->bytes_per_sample);
  bytes = (spec->width >> 3) * spec->channels;
  for (i = 0; i < bytes; i++) {
    GST_DEBUG ("silence byte %d: %02x", i, spec->silence_sample[i]);
  }
}

/**
 * gst_ring_buffer_debug_spec_buff:
 * @spec: the spec to debug
 *
 * Print debug info about the buffer sized in @spec to the debug log.
 */
void
gst_ring_buffer_debug_spec_buff (GstRingBufferSpec * spec)
{
  GST_DEBUG ("acquire ringbuffer: buffer time: %" G_GINT64_FORMAT " usec",
      spec->buffer_time);
  GST_DEBUG ("acquire ringbuffer: latency time: %" G_GINT64_FORMAT " usec",
      spec->latency_time);
  GST_DEBUG ("acquire ringbuffer: total segments: %d", spec->segtotal);
  GST_DEBUG ("acquire ringbuffer: latency segments: %d", spec->seglatency);
  GST_DEBUG ("acquire ringbuffer: segment size: %d bytes = %d samples",
      spec->segsize, spec->segsize / spec->bytes_per_sample);
  GST_DEBUG ("acquire ringbuffer: buffer size: %d bytes = %d samples",
      spec->segsize * spec->segtotal,
      spec->segsize * spec->segtotal / spec->bytes_per_sample);
}

/**
 * gst_ring_buffer_parse_caps:
 * @spec: a spec
 * @caps: a #GstCaps
 *
 * Parse @caps into @spec.
 *
 * Returns: TRUE if the caps could be parsed.
 */
gboolean
gst_ring_buffer_parse_caps (GstRingBufferSpec * spec, GstCaps * caps)
{
  const gchar *mimetype;
  GstStructure *structure;
  gint i;

  structure = gst_caps_get_structure (caps, 0);

  /* we have to differentiate between int and float formats */
  mimetype = gst_structure_get_name (structure);

  if (!strncmp (mimetype, "audio/x-raw-int", 15)) {
    gint endianness;
    const FormatDef *def;
    gint j, bytes;

    spec->type = GST_BUFTYPE_LINEAR;

    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate) &&
            gst_structure_get_int (structure, "channels", &spec->channels) &&
            gst_structure_get_int (structure, "width", &spec->width) &&
            gst_structure_get_int (structure, "depth", &spec->depth) &&
            gst_structure_get_boolean (structure, "signed", &spec->sign)))
      goto parse_error;

    /* extract endianness if needed */
    if (spec->width > 8) {
      if (!gst_structure_get_int (structure, "endianness", &endianness))
        goto parse_error;
    } else {
      endianness = G_BYTE_ORDER;
    }

    spec->bigend = endianness == G_LITTLE_ENDIAN ? FALSE : TRUE;

    def = build_linear_format (spec->depth, spec->width, spec->sign ? 0 : 1,
        spec->bigend ? 1 : 0);

    if (def == NULL)
      goto parse_error;

    spec->format = def->format;

    bytes = spec->width >> 3;

    for (i = 0; i < spec->channels; i++) {
      for (j = 0; j < bytes; j++) {
        spec->silence_sample[i * bytes + j] = def->silence[j];
      }
    }
  } else if (!strncmp (mimetype, "audio/x-raw-float", 17)) {

    spec->type = GST_BUFTYPE_FLOAT;

    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate) &&
            gst_structure_get_int (structure, "channels", &spec->channels) &&
            gst_structure_get_int (structure, "width", &spec->width)))
      goto parse_error;

    /* match layout to format wrt to endianness */
    switch (spec->width) {
      case 32:
        spec->format =
            G_BYTE_ORDER == G_LITTLE_ENDIAN ? GST_FLOAT32_LE : GST_FLOAT32_BE;
        break;
      case 64:
        spec->format =
            G_BYTE_ORDER == G_LITTLE_ENDIAN ? GST_FLOAT64_LE : GST_FLOAT64_BE;
        break;
      default:
        goto parse_error;
    }
    /* float silence is all zeros.. */
    memset (spec->silence_sample, 0, 32);
  } else if (!strncmp (mimetype, "audio/x-alaw", 12)) {
    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate) &&
            gst_structure_get_int (structure, "channels", &spec->channels)))
      goto parse_error;

    spec->type = GST_BUFTYPE_A_LAW;
    spec->format = GST_A_LAW;
    spec->width = 8;
    spec->depth = 8;
    for (i = 0; i < spec->channels; i++)
      spec->silence_sample[i] = 0xd5;
  } else if (!strncmp (mimetype, "audio/x-mulaw", 13)) {
    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate) &&
            gst_structure_get_int (structure, "channels", &spec->channels)))
      goto parse_error;

    spec->type = GST_BUFTYPE_MU_LAW;
    spec->format = GST_MU_LAW;
    spec->width = 8;
    spec->depth = 8;
    for (i = 0; i < spec->channels; i++)
      spec->silence_sample[i] = 0xff;
  } else if (!strncmp (mimetype, "audio/x-iec958", 14)) {
    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate)))
      goto parse_error;

    spec->type = GST_BUFTYPE_IEC958;
    spec->format = GST_IEC958;
    spec->width = 16;
    spec->depth = 16;
    spec->channels = 2;
  } else if (!strncmp (mimetype, "audio/x-ac3", 11)) {
    /* extract the needed information from the cap */
    if (!(gst_structure_get_int (structure, "rate", &spec->rate)))
      goto parse_error;

    spec->type = GST_BUFTYPE_AC3;
    spec->format = GST_AC3;
    spec->width = 16;
    spec->depth = 16;
    spec->channels = 2;
  } else {
    goto parse_error;
  }

  spec->bytes_per_sample = (spec->width >> 3) * spec->channels;

  gst_caps_replace (&spec->caps, caps);

  g_return_val_if_fail (spec->latency_time != 0, FALSE);

  /* calculate suggested segsize and segtotal. segsize should be one unit
   * of 'latency_time' samples, scaling for the fact that latency_time is
   * currently stored in microseconds (FIXME: in 0.11) */
  spec->segsize = gst_util_uint64_scale (spec->rate * spec->bytes_per_sample,
      spec->latency_time, GST_SECOND / GST_USECOND);
  /* Round to an integer number of samples */
  spec->segsize -= spec->segsize % spec->bytes_per_sample;

  spec->segtotal = spec->buffer_time / spec->latency_time;
  /* leave the latency undefined now, implementations can change it but if it's
   * not changed, we assume the same value as segtotal */
  spec->seglatency = -1;

  gst_ring_buffer_debug_spec_caps (spec);
  gst_ring_buffer_debug_spec_buff (spec);

  return TRUE;

  /* ERRORS */
parse_error:
  {
    GST_DEBUG ("could not parse caps");
    return FALSE;
  }
}

/**
 * gst_ring_buffer_convert:
 * @buf: the #GstRingBuffer
 * @src_fmt: the source format
 * @src_val: the source value
 * @dest_fmt: the destination format
 * @dest_val: a location to store the converted value
 *
 * Convert @src_val in @src_fmt to the equivalent value in @dest_fmt. The result
 * will be put in @dest_val.
 *
 * Returns: TRUE if the conversion succeeded.
 *
 * Since: 0.10.22.
 */
gboolean
gst_ring_buffer_convert (GstRingBuffer * buf,
    GstFormat src_fmt, gint64 src_val, GstFormat dest_fmt, gint64 * dest_val)
{
  gboolean res = TRUE;
  gint bps, rate;

  GST_DEBUG ("converting value %" G_GINT64_FORMAT " from %s (%d) to %s (%d)",
      src_val, gst_format_get_name (src_fmt), src_fmt,
      gst_format_get_name (dest_fmt), dest_fmt);

  if (src_fmt == dest_fmt || src_val == -1) {
    *dest_val = src_val;
    goto done;
  }

  /* get important info */
  GST_OBJECT_LOCK (buf);
  bps = buf->spec.bytes_per_sample;
  rate = buf->spec.rate;
  GST_OBJECT_UNLOCK (buf);

  if (bps == 0 || rate == 0) {
    GST_DEBUG ("no rate or bps configured");
    res = FALSE;
    goto done;
  }

  switch (src_fmt) {
    case GST_FORMAT_BYTES:
      switch (dest_fmt) {
        case GST_FORMAT_TIME:
          *dest_val = gst_util_uint64_scale_int (src_val / bps, GST_SECOND,
              rate);
          break;
        case GST_FORMAT_DEFAULT:
          *dest_val = src_val / bps;
          break;
        default:
          res = FALSE;
          break;
      }
      break;
    case GST_FORMAT_DEFAULT:
      switch (dest_fmt) {
        case GST_FORMAT_TIME:
          *dest_val = gst_util_uint64_scale_int (src_val, GST_SECOND, rate);
          break;
        case GST_FORMAT_BYTES:
          *dest_val = src_val * bps;
          break;
        default:
          res = FALSE;
          break;
      }
      break;
    case GST_FORMAT_TIME:
      switch (dest_fmt) {
        case GST_FORMAT_DEFAULT:
          *dest_val = gst_util_uint64_scale_int (src_val, rate, GST_SECOND);
          break;
        case GST_FORMAT_BYTES:
          *dest_val = gst_util_uint64_scale_int (src_val, rate, GST_SECOND);
          *dest_val *= bps;
          break;
        default:
          res = FALSE;
          break;
      }
      break;
    default:
      res = FALSE;
      break;
  }
done:
  GST_DEBUG ("ret=%d result %" G_GINT64_FORMAT, res, *dest_val);

  return res;
}

/**
 * gst_ring_buffer_set_callback:
 * @buf: the #GstRingBuffer to set the callback on
 * @cb: the callback to set
 * @user_data: user data passed to the callback
 *
 * Sets the given callback function on the buffer. This function
 * will be called every time a segment has been written to a device.
 *
 * MT safe.
 */
void
gst_ring_buffer_set_callback (GstRingBuffer * buf, GstRingBufferCallback cb,
    gpointer user_data)
{
  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  GST_OBJECT_LOCK (buf);
  buf->callback = cb;
  buf->cb_data = user_data;
  GST_OBJECT_UNLOCK (buf);
}


/**
 * gst_ring_buffer_open_device:
 * @buf: the #GstRingBuffer
 *
 * Open the audio device associated with the ring buffer. Does not perform any
 * setup on the device. You must open the device before acquiring the ring
 * buffer.
 *
 * Returns: TRUE if the device could be opened, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_open_device (GstRingBuffer * buf)
{
  gboolean res = TRUE;
  GstRingBufferClass *rclass;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "opening device");

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (buf->open))
    goto was_opened;

  buf->open = TRUE;

  /* if this fails, something is wrong in this file */
  g_assert (!buf->acquired);

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->open_device))
    res = rclass->open_device (buf);

  if (G_UNLIKELY (!res))
    goto open_failed;

  GST_DEBUG_OBJECT (buf, "opened device");

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
was_opened:
  {
    GST_DEBUG_OBJECT (buf, "Device for ring buffer already open");
    g_warning ("Device for ring buffer %p already open, fix your code", buf);
    res = TRUE;
    goto done;
  }
open_failed:
  {
    buf->open = FALSE;
    GST_DEBUG_OBJECT (buf, "failed opening device");
    goto done;
  }
}

/**
 * gst_ring_buffer_close_device:
 * @buf: the #GstRingBuffer
 *
 * Close the audio device associated with the ring buffer. The ring buffer
 * should already have been released via gst_ring_buffer_release().
 *
 * Returns: TRUE if the device could be closed, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_close_device (GstRingBuffer * buf)
{
  gboolean res = TRUE;
  GstRingBufferClass *rclass;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "closing device");

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (!buf->open))
    goto was_closed;

  if (G_UNLIKELY (buf->acquired))
    goto was_acquired;

  buf->open = FALSE;

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->close_device))
    res = rclass->close_device (buf);

  if (G_UNLIKELY (!res))
    goto close_error;

  GST_DEBUG_OBJECT (buf, "closed device");

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
was_closed:
  {
    GST_DEBUG_OBJECT (buf, "Device for ring buffer already closed");
    g_warning ("Device for ring buffer %p already closed, fix your code", buf);
    res = TRUE;
    goto done;
  }
was_acquired:
  {
    GST_DEBUG_OBJECT (buf, "Resources for ring buffer still acquired");
    g_critical ("Resources for ring buffer %p still acquired", buf);
    res = FALSE;
    goto done;
  }
close_error:
  {
    buf->open = TRUE;
    GST_DEBUG_OBJECT (buf, "error closing device");
    goto done;
  }
}

/**
 * gst_ring_buffer_device_is_open:
 * @buf: the #GstRingBuffer
 *
 * Checks the status of the device associated with the ring buffer.
 *
 * Returns: TRUE if the device was open, FALSE if it was closed.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_device_is_open (GstRingBuffer * buf)
{
  gboolean res = TRUE;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_OBJECT_LOCK (buf);
  res = buf->open;
  GST_OBJECT_UNLOCK (buf);

  return res;
}

/**
 * gst_ring_buffer_acquire:
 * @buf: the #GstRingBuffer to acquire
 * @spec: the specs of the buffer
 *
 * Allocate the resources for the ringbuffer. This function fills
 * in the data pointer of the ring buffer with a valid #GstBuffer
 * to which samples can be written.
 *
 * Returns: TRUE if the device could be acquired, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_acquire (GstRingBuffer * buf, GstRingBufferSpec * spec)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;
  gint i, j;
  gint segsize, bps;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "acquiring device %p", buf);

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (!buf->open))
    goto not_opened;

  if (G_UNLIKELY (buf->acquired))
    goto was_acquired;

  buf->acquired = TRUE;

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->acquire))
    res = rclass->acquire (buf, spec);

  if (G_UNLIKELY (!res))
    goto acquire_failed;

  if (G_UNLIKELY ((bps = buf->spec.bytes_per_sample) == 0))
    goto invalid_bps;

  /* if the seglatency was overwritten with something else than -1, use it, else
   * assume segtotal as the latency */
  if (buf->spec.seglatency == -1)
    buf->spec.seglatency = buf->spec.segtotal;

  segsize = buf->spec.segsize;

  buf->samples_per_seg = segsize / bps;

  /* create an empty segment */
  g_free (buf->empty_seg);
  buf->empty_seg = g_malloc (segsize);

  /* FIXME, we only have 32 silence samples, which might not be enough to
   * represent silence in all channels */
  bps = MIN (bps, 32);
  for (i = 0, j = 0; i < segsize; i++) {
    buf->empty_seg[i] = buf->spec.silence_sample[j];
    j = (j + 1) % bps;
  }
  GST_DEBUG_OBJECT (buf, "acquired device");

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
not_opened:
  {
    GST_DEBUG_OBJECT (buf, "device not opened");
    g_critical ("Device for %p not opened", buf);
    res = FALSE;
    goto done;
  }
was_acquired:
  {
    res = TRUE;
    GST_DEBUG_OBJECT (buf, "device was acquired");
    goto done;
  }
acquire_failed:
  {
    buf->acquired = FALSE;
    GST_DEBUG_OBJECT (buf, "failed to acquire device");
    goto done;
  }
invalid_bps:
  {
    g_warning
        ("invalid bytes_per_sample from acquire ringbuffer %p, fix the element",
        buf);
    buf->acquired = FALSE;
    res = FALSE;
    goto done;
  }
}

/**
 * gst_ring_buffer_release:
 * @buf: the #GstRingBuffer to release
 *
 * Free the resources of the ringbuffer.
 *
 * Returns: TRUE if the device could be released, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_release (GstRingBuffer * buf)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "releasing device");

  gst_ring_buffer_stop (buf);

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (!buf->acquired))
    goto was_released;

  buf->acquired = FALSE;

  /* if this fails, something is wrong in this file */
  g_assert (buf->open == TRUE);

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->release))
    res = rclass->release (buf);

  /* signal any waiters */
  GST_DEBUG_OBJECT (buf, "signal waiter");
  GST_RING_BUFFER_SIGNAL (buf);

  if (G_UNLIKELY (!res))
    goto release_failed;

  g_free (buf->empty_seg);
  buf->empty_seg = NULL;
  GST_DEBUG_OBJECT (buf, "released device");

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
was_released:
  {
    res = TRUE;
    GST_DEBUG_OBJECT (buf, "device was released");
    goto done;
  }
release_failed:
  {
    buf->acquired = TRUE;
    GST_DEBUG_OBJECT (buf, "failed to release device");
    goto done;
  }
}

/**
 * gst_ring_buffer_is_acquired:
 * @buf: the #GstRingBuffer to check
 *
 * Check if the ringbuffer is acquired and ready to use.
 *
 * Returns: TRUE if the ringbuffer is acquired, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_is_acquired (GstRingBuffer * buf)
{
  gboolean res;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_OBJECT_LOCK (buf);
  res = buf->acquired;
  GST_OBJECT_UNLOCK (buf);

  return res;
}

/**
 * gst_ring_buffer_activate:
 * @buf: the #GstRingBuffer to activate
 * @active: the new mode
 *
 * Activate @buf to start or stop pulling data.
 *
 * MT safe.
 *
 * Returns: TRUE if the device could be activated in the requested mode,
 * FALSE on error.
 *
 * Since: 0.10.22.
 */
gboolean
gst_ring_buffer_activate (GstRingBuffer * buf, gboolean active)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "activate device");

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (active && !buf->acquired))
    goto not_acquired;

  if (G_UNLIKELY (buf->abidata.ABI.active == active))
    goto was_active;

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  /* if there is no activate function we assume it was started/released
   * in the acquire method */
  if (G_LIKELY (rclass->activate))
    res = rclass->activate (buf, active);
  else
    res = TRUE;

  if (G_UNLIKELY (!res))
    goto activate_failed;

  buf->abidata.ABI.active = active;

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
not_acquired:
  {
    GST_DEBUG_OBJECT (buf, "device not acquired");
    g_critical ("Device for %p not acquired", buf);
    res = FALSE;
    goto done;
  }
was_active:
  {
    res = TRUE;
    GST_DEBUG_OBJECT (buf, "device was active in mode %d", active);
    goto done;
  }
activate_failed:
  {
    GST_DEBUG_OBJECT (buf, "failed to activate device");
    goto done;
  }
}

/**
 * gst_ring_buffer_is_active:
 * @buf: the #GstRingBuffer
 *
 * Check if @buf is activated.
 *
 * MT safe.
 *
 * Returns: TRUE if the device is active.
 *
 * Since: 0.10.22.
 */
gboolean
gst_ring_buffer_is_active (GstRingBuffer * buf)
{
  gboolean res;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_OBJECT_LOCK (buf);
  res = buf->abidata.ABI.active;
  GST_OBJECT_UNLOCK (buf);

  return res;
}


/**
 * gst_ring_buffer_set_flushing:
 * @buf: the #GstRingBuffer to flush
 * @flushing: the new mode
 *
 * Set the ringbuffer to flushing mode or normal mode.
 *
 * MT safe.
 */
void
gst_ring_buffer_set_flushing (GstRingBuffer * buf, gboolean flushing)
{
  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  GST_OBJECT_LOCK (buf);
  buf->abidata.ABI.flushing = flushing;

  if (flushing) {
    gst_ring_buffer_pause_unlocked (buf);
  } else {
    gst_ring_buffer_clear_all (buf);
  }
  GST_OBJECT_UNLOCK (buf);
}

/**
 * gst_ring_buffer_start:
 * @buf: the #GstRingBuffer to start
 *
 * Start processing samples from the ringbuffer.
 *
 * Returns: TRUE if the device could be started, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_start (GstRingBuffer * buf)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;
  gboolean resume = FALSE;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "starting ringbuffer");

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (buf->abidata.ABI.flushing))
    goto flushing;

  if (G_UNLIKELY (!buf->acquired))
    goto not_acquired;

  if (G_UNLIKELY (g_atomic_int_get (&buf->abidata.ABI.may_start) == FALSE))
    goto may_not_start;

  /* if stopped, set to started */
  res = g_atomic_int_compare_and_exchange (&buf->state,
      GST_RING_BUFFER_STATE_STOPPED, GST_RING_BUFFER_STATE_STARTED);

  if (!res) {
    GST_DEBUG_OBJECT (buf, "was not stopped, try paused");
    /* was not stopped, try from paused */
    res = g_atomic_int_compare_and_exchange (&buf->state,
        GST_RING_BUFFER_STATE_PAUSED, GST_RING_BUFFER_STATE_STARTED);
    if (!res) {
      /* was not paused either, must be started then */
      res = TRUE;
      GST_DEBUG_OBJECT (buf, "was not paused, must have been started");
      goto done;
    }
    resume = TRUE;
    GST_DEBUG_OBJECT (buf, "resuming");
  }

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (resume) {
    if (G_LIKELY (rclass->resume))
      res = rclass->resume (buf);
  } else {
    if (G_LIKELY (rclass->start))
      res = rclass->start (buf);
  }

  if (G_UNLIKELY (!res)) {
    buf->state = GST_RING_BUFFER_STATE_PAUSED;
    GST_DEBUG_OBJECT (buf, "failed to start");
  } else {
    GST_DEBUG_OBJECT (buf, "started");
  }

done:
  GST_OBJECT_UNLOCK (buf);

  return res;

flushing:
  {
    GST_DEBUG_OBJECT (buf, "we are flushing");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
not_acquired:
  {
    GST_DEBUG_OBJECT (buf, "we are not acquired");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
may_not_start:
  {
    GST_DEBUG_OBJECT (buf, "we may not start");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
}

static gboolean
gst_ring_buffer_pause_unlocked (GstRingBuffer * buf)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;

  GST_DEBUG_OBJECT (buf, "pausing ringbuffer");

  /* if started, set to paused */
  res = g_atomic_int_compare_and_exchange (&buf->state,
      GST_RING_BUFFER_STATE_STARTED, GST_RING_BUFFER_STATE_PAUSED);

  if (!res)
    goto not_started;

  /* signal any waiters */
  GST_DEBUG_OBJECT (buf, "signal waiter");
  GST_RING_BUFFER_SIGNAL (buf);

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->pause))
    res = rclass->pause (buf);

  if (G_UNLIKELY (!res)) {
    buf->state = GST_RING_BUFFER_STATE_STARTED;
    GST_DEBUG_OBJECT (buf, "failed to pause");
  } else {
    GST_DEBUG_OBJECT (buf, "paused");
  }

  return res;

not_started:
  {
    /* was not started */
    GST_DEBUG_OBJECT (buf, "was not started");
    return TRUE;
  }
}

/**
 * gst_ring_buffer_pause:
 * @buf: the #GstRingBuffer to pause
 *
 * Pause processing samples from the ringbuffer.
 *
 * Returns: TRUE if the device could be paused, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_pause (GstRingBuffer * buf)
{
  gboolean res = FALSE;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (buf->abidata.ABI.flushing))
    goto flushing;

  if (G_UNLIKELY (!buf->acquired))
    goto not_acquired;

  res = gst_ring_buffer_pause_unlocked (buf);
  GST_OBJECT_UNLOCK (buf);

  return res;

  /* ERRORS */
flushing:
  {
    GST_DEBUG_OBJECT (buf, "we are flushing");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
not_acquired:
  {
    GST_DEBUG_OBJECT (buf, "not acquired");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
}

/**
 * gst_ring_buffer_stop:
 * @buf: the #GstRingBuffer to stop
 *
 * Stop processing samples from the ringbuffer.
 *
 * Returns: TRUE if the device could be stopped, FALSE on error.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_stop (GstRingBuffer * buf)
{
  gboolean res = FALSE;
  GstRingBufferClass *rclass;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  GST_DEBUG_OBJECT (buf, "stopping");

  GST_OBJECT_LOCK (buf);

  /* if started, set to stopped */
  res = g_atomic_int_compare_and_exchange (&buf->state,
      GST_RING_BUFFER_STATE_STARTED, GST_RING_BUFFER_STATE_STOPPED);

  if (!res) {
    GST_DEBUG_OBJECT (buf, "was not started, try paused");
    /* was not started, try from paused */
    res = g_atomic_int_compare_and_exchange (&buf->state,
        GST_RING_BUFFER_STATE_PAUSED, GST_RING_BUFFER_STATE_STOPPED);
    if (!res) {
      /* was not paused either, must have been stopped then */
      res = TRUE;
      GST_DEBUG_OBJECT (buf, "was not paused, must have been stopped");
      goto done;
    }
  }

  /* signal any waiters */
  GST_DEBUG_OBJECT (buf, "signal waiter");
  GST_RING_BUFFER_SIGNAL (buf);

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->stop))
    res = rclass->stop (buf);

  if (G_UNLIKELY (!res)) {
    buf->state = GST_RING_BUFFER_STATE_STARTED;
    GST_DEBUG_OBJECT (buf, "failed to stop");
  } else {
    GST_DEBUG_OBJECT (buf, "stopped");
  }
done:
  GST_OBJECT_UNLOCK (buf);

  return res;
}

/**
 * gst_ring_buffer_delay:
 * @buf: the #GstRingBuffer to query
 *
 * Get the number of samples queued in the audio device. This is
 * usually less than the segment size but can be bigger when the
 * implementation uses another internal buffer between the audio
 * device.
 *
 * For playback ringbuffers this is the amount of samples transfered from the
 * ringbuffer to the device but still not played.
 *
 * For capture ringbuffers this is the amount of samples in the device that are
 * not yet transfered to the ringbuffer.
 *
 * Returns: The number of samples queued in the audio device.
 *
 * MT safe.
 */
guint
gst_ring_buffer_delay (GstRingBuffer * buf)
{
  GstRingBufferClass *rclass;
  guint res;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), 0);

  /* buffer must be acquired */
  if (G_UNLIKELY (!gst_ring_buffer_is_acquired (buf)))
    goto not_acquired;

  rclass = GST_RING_BUFFER_GET_CLASS (buf);
  if (G_LIKELY (rclass->delay))
    res = rclass->delay (buf);
  else
    res = 0;

  return res;

not_acquired:
  {
    GST_DEBUG_OBJECT (buf, "not acquired");
    return 0;
  }
}

/**
 * gst_ring_buffer_samples_done:
 * @buf: the #GstRingBuffer to query
 *
 * Get the number of samples that were processed by the ringbuffer
 * since it was last started. This does not include the number of samples not
 * yet processed (see gst_ring_buffer_delay()).
 *
 * Returns: The number of samples processed by the ringbuffer.
 *
 * MT safe.
 */
guint64
gst_ring_buffer_samples_done (GstRingBuffer * buf)
{
  gint segdone;
  guint64 samples;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), 0);

  /* get the amount of segments we processed */
  segdone = g_atomic_int_get (&buf->segdone);

  /* convert to samples */
  samples = ((guint64) segdone) * buf->samples_per_seg;

  return samples;
}

/**
 * gst_ring_buffer_set_sample:
 * @buf: the #GstRingBuffer to use
 * @sample: the sample number to set
 *
 * Make sure that the next sample written to the device is
 * accounted for as being the @sample sample written to the
 * device. This value will be used in reporting the current
 * sample position of the ringbuffer.
 *
 * This function will also clear the buffer with silence.
 *
 * MT safe.
 */
void
gst_ring_buffer_set_sample (GstRingBuffer * buf, guint64 sample)
{
  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  if (sample == -1)
    sample = 0;

  if (G_UNLIKELY (buf->samples_per_seg == 0))
    return;

  /* FIXME, we assume the ringbuffer can restart at a random 
   * position, round down to the beginning and keep track of
   * offset when calculating the processed samples. */
  buf->segbase = buf->segdone - sample / buf->samples_per_seg;

  gst_ring_buffer_clear_all (buf);

  GST_DEBUG_OBJECT (buf, "set sample to %" G_GUINT64_FORMAT ", segbase %d",
      sample, buf->segbase);
}

static void
default_clear_all (GstRingBuffer * buf)
{
  gint i;

  /* not fatal, we just are not negotiated yet */
  if (G_UNLIKELY (buf->spec.segtotal <= 0))
    return;

  GST_DEBUG_OBJECT (buf, "clear all segments");

  for (i = 0; i < buf->spec.segtotal; i++) {
    gst_ring_buffer_clear (buf, i);
  }
}

/**
 * gst_ring_buffer_clear_all:
 * @buf: the #GstRingBuffer to clear
 *
 * Fill the ringbuffer with silence.
 *
 * MT safe.
 */
void
gst_ring_buffer_clear_all (GstRingBuffer * buf)
{
  GstRingBufferClass *rclass;

  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  rclass = GST_RING_BUFFER_GET_CLASS (buf);

  if (G_LIKELY (rclass->clear_all))
    rclass->clear_all (buf);
}


static gboolean
wait_segment (GstRingBuffer * buf)
{
  gint segments;
  gboolean wait = TRUE;

  /* buffer must be started now or we deadlock since nobody is reading */
  if (G_UNLIKELY (g_atomic_int_get (&buf->state) !=
          GST_RING_BUFFER_STATE_STARTED)) {
    /* see if we are allowed to start it */
    if (G_UNLIKELY (g_atomic_int_get (&buf->abidata.ABI.may_start) == FALSE))
      goto no_start;

    GST_DEBUG_OBJECT (buf, "start!");
    segments = g_atomic_int_get (&buf->segdone);
    gst_ring_buffer_start (buf);

    /* After starting, the writer may have wrote segments already and then we
     * don't need to wait anymore */
    if (G_LIKELY (g_atomic_int_get (&buf->segdone) != segments))
      wait = FALSE;
  }

  /* take lock first, then update our waiting flag */
  GST_OBJECT_LOCK (buf);
  if (G_UNLIKELY (buf->abidata.ABI.flushing))
    goto flushing;

  if (G_UNLIKELY (g_atomic_int_get (&buf->state) !=
          GST_RING_BUFFER_STATE_STARTED))
    goto not_started;

  if (G_LIKELY (wait)) {
    if (g_atomic_int_compare_and_exchange (&buf->waiting, 0, 1)) {
      GST_DEBUG_OBJECT (buf, "waiting..");
      GST_RING_BUFFER_WAIT (buf);

      if (G_UNLIKELY (buf->abidata.ABI.flushing))
        goto flushing;

      if (G_UNLIKELY (g_atomic_int_get (&buf->state) !=
              GST_RING_BUFFER_STATE_STARTED))
        goto not_started;
    }
  }
  GST_OBJECT_UNLOCK (buf);

  return TRUE;

  /* ERROR */
not_started:
  {
    g_atomic_int_compare_and_exchange (&buf->waiting, 1, 0);
    GST_DEBUG_OBJECT (buf, "stopped processing");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
flushing:
  {
    g_atomic_int_compare_and_exchange (&buf->waiting, 1, 0);
    GST_DEBUG_OBJECT (buf, "flushing");
    GST_OBJECT_UNLOCK (buf);
    return FALSE;
  }
no_start:
  {
    GST_DEBUG_OBJECT (buf, "not allowed to start");
    return FALSE;
  }
}

#define FWD_SAMPLES(s,se,d,de)		 	\
G_STMT_START {					\
  /* no rate conversion */			\
  guint towrite = MIN (se + bps - s, de - d);	\
  /* simple copy */				\
  if (!skip)					\
    memcpy (d, s, towrite);			\
  in_samples -= towrite / bps;			\
  out_samples -= towrite / bps;			\
  s += towrite;					\
  GST_DEBUG ("copy %u bytes", towrite);		\
} G_STMT_END

/* in_samples >= out_samples, rate > 1.0 */
#define FWD_UP_SAMPLES(s,se,d,de) 	 	\
G_STMT_START {					\
  guint8 *sb = s, *db = d;			\
  while (s <= se && d < de) {			\
    if (!skip)					\
      memcpy (d, s, bps);			\
    s += bps;					\
    *accum += outr;				\
    if ((*accum << 1) >= inr) {			\
      *accum -= inr;				\
      d += bps;					\
    }						\
  }						\
  in_samples -= (s - sb)/bps;			\
  out_samples -= (d - db)/bps;			\
  GST_DEBUG ("fwd_up end %d/%d",*accum,*toprocess);	\
} G_STMT_END

/* out_samples > in_samples, for rates smaller than 1.0 */
#define FWD_DOWN_SAMPLES(s,se,d,de) 	 	\
G_STMT_START {					\
  guint8 *sb = s, *db = d;			\
  while (s <= se && d < de) {			\
    if (!skip)					\
      memcpy (d, s, bps);			\
    d += bps;					\
    *accum += inr;				\
    if ((*accum << 1) >= outr) {		\
      *accum -= outr;				\
      s += bps;					\
    }						\
  }						\
  in_samples -= (s - sb)/bps;			\
  out_samples -= (d - db)/bps;			\
  GST_DEBUG ("fwd_down end %d/%d",*accum,*toprocess);	\
} G_STMT_END

#define REV_UP_SAMPLES(s,se,d,de) 	 	\
G_STMT_START {					\
  guint8 *sb = se, *db = d;			\
  while (s <= se && d < de) {			\
    if (!skip)					\
      memcpy (d, se, bps);			\
    se -= bps;					\
    *accum += outr;				\
    while (d < de && (*accum << 1) >= inr) {	\
      *accum -= inr;				\
      d += bps;					\
    }						\
  }						\
  in_samples -= (sb - se)/bps;			\
  out_samples -= (d - db)/bps;			\
  GST_DEBUG ("rev_up end %d/%d",*accum,*toprocess);	\
} G_STMT_END

#define REV_DOWN_SAMPLES(s,se,d,de) 	 	\
G_STMT_START {					\
  guint8 *sb = se, *db = d;			\
  while (s <= se && d < de) {			\
    if (!skip)					\
      memcpy (d, se, bps);			\
    d += bps;					\
    *accum += inr;				\
    while (s <= se && (*accum << 1) >= outr) {	\
      *accum -= outr;				\
      se -= bps;				\
    }						\
  }						\
  in_samples -= (sb - se)/bps;			\
  out_samples -= (d - db)/bps;			\
  GST_DEBUG ("rev_down end %d/%d",*accum,*toprocess);	\
} G_STMT_END

static guint
default_commit (GstRingBuffer * buf, guint64 * sample,
    guchar * data, gint in_samples, gint out_samples, gint * accum)
{
  gint segdone;
  gint segsize, segtotal, bps, sps;
  guint8 *dest, *data_end;
  gint writeseg, sampleoff;
  gint *toprocess;
  gint inr, outr;
  gboolean reverse;

  g_return_val_if_fail (buf->data != NULL, -1);
  g_return_val_if_fail (data != NULL, -1);

  dest = GST_BUFFER_DATA (buf->data);
  segsize = buf->spec.segsize;
  segtotal = buf->spec.segtotal;
  bps = buf->spec.bytes_per_sample;
  sps = buf->samples_per_seg;

  reverse = out_samples < 0;
  out_samples = ABS (out_samples);

  if (in_samples >= out_samples)
    toprocess = &in_samples;
  else
    toprocess = &out_samples;

  inr = in_samples - 1;
  outr = out_samples - 1;

  /* data_end points to the last sample we have to write, not past it. This is
   * needed to properly handle reverse playback: it points to the last sample. */
  data_end = data + (bps * inr);

  /* figure out the segment and the offset inside the segment where
   * the first sample should be written. */
  writeseg = *sample / sps;
  sampleoff = (*sample % sps) * bps;

  /* write out all samples */
  while (*toprocess > 0) {
    gint avail;
    guint8 *d, *d_end;
    gint ws;
    gboolean skip;

    while (TRUE) {
      gint diff;

      /* get the currently processed segment */
      segdone = g_atomic_int_get (&buf->segdone) - buf->segbase;

      /* see how far away it is from the write segment */
      diff = writeseg - segdone;

      GST_DEBUG
          ("pointer at %d, write to %d-%d, diff %d, segtotal %d, segsize %d, base %d",
          segdone, writeseg, sampleoff, diff, segtotal, segsize, buf->segbase);

      /* segment too far ahead, writer too slow, we need to drop, hopefully UNLIKELY */
      if (G_UNLIKELY (diff < 0)) {
        /* we need to drop one segment at a time, pretend we wrote a
         * segment. */
        skip = TRUE;
        break;
      }

      /* write segment is within writable range, we can break the loop and
       * start writing the data. */
      if (diff < segtotal) {
        skip = FALSE;
        break;
      }

      /* else we need to wait for the segment to become writable. */
      if (!wait_segment (buf))
        goto not_started;
    }

    /* we can write now */
    ws = writeseg % segtotal;
    avail = MIN (segsize - sampleoff, bps * out_samples);

    d = dest + (ws * segsize) + sampleoff;
    d_end = d + avail;
    *sample += avail / bps;

    GST_DEBUG_OBJECT (buf, "write @%p seg %d, sps %d, off %d, avail %d",
        dest + ws * segsize, ws, sps, sampleoff, avail);

    if (G_LIKELY (inr == outr && !reverse)) {
      /* no rate conversion, simply copy samples */
      FWD_SAMPLES (data, data_end, d, d_end);
    } else if (!reverse) {
      if (inr >= outr)
        /* forward speed up */
        FWD_UP_SAMPLES (data, data_end, d, d_end);
      else
        /* forward slow down */
        FWD_DOWN_SAMPLES (data, data_end, d, d_end);
    } else {
      if (inr >= outr)
        /* reverse speed up */
        REV_UP_SAMPLES (data, data_end, d, d_end);
      else
        /* reverse slow down */
        REV_DOWN_SAMPLES (data, data_end, d, d_end);
    }

    /* for the next iteration we write to the next segment at the beginning. */
    writeseg++;
    sampleoff = 0;
  }
  /* we consumed all samples here */
  data = data_end + bps;

done:
  return inr - ((data_end - data) / bps);

  /* ERRORS */
not_started:
  {
    GST_DEBUG_OBJECT (buf, "stopped processing");
    goto done;
  }
}

/**
 * gst_ring_buffer_commit_full:
 * @buf: the #GstRingBuffer to commit
 * @sample: the sample position of the data
 * @data: the data to commit
 * @in_samples: the number of samples in the data to commit
 * @out_samples: the number of samples to write to the ringbuffer
 * @accum: accumulator for rate conversion.
 *
 * Commit @in_samples samples pointed to by @data to the ringbuffer @buf. 
 *
 * @in_samples and @out_samples define the rate conversion to perform on the the
 * samples in @data. For negative rates, @out_samples must be negative and
 * @in_samples positive.
 *
 * When @out_samples is positive, the first sample will be written at position @sample
 * in the ringbuffer. When @out_samples is negative, the last sample will be written to
 * @sample in reverse order.
 *
 * @out_samples does not need to be a multiple of the segment size of the ringbuffer
 * although it is recommended for optimal performance. 
 *
 * @accum will hold a temporary accumulator used in rate conversion and should be
 * set to 0 when this function is first called. In case the commit operation is
 * interrupted, one can resume the processing by passing the previously returned
 * @accum value back to this function.
 *
 * MT safe.
 *
 * Returns: The number of samples written to the ringbuffer or -1 on error. The
 * number of samples written can be less than @out_samples when @buf was interrupted
 * with a flush or stop.
 *
 * Since: 0.10.11.
 */
guint
gst_ring_buffer_commit_full (GstRingBuffer * buf, guint64 * sample,
    guchar * data, gint in_samples, gint out_samples, gint * accum)
{
  GstRingBufferClass *rclass;
  guint res = -1;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), -1);

  if (G_UNLIKELY (in_samples == 0 || out_samples == 0))
    return in_samples;

  rclass = GST_RING_BUFFER_GET_CLASS (buf);

  if (G_LIKELY (rclass->commit))
    res = rclass->commit (buf, sample, data, in_samples, out_samples, accum);

  return res;
}

/**
 * gst_ring_buffer_commit:
 * @buf: the #GstRingBuffer to commit
 * @sample: the sample position of the data
 * @data: the data to commit
 * @len: the number of samples in the data to commit
 *
 * Same as gst_ring_buffer_commit_full() but with a in_samples and out_samples
 * equal to @len, ignoring accum.
 *
 * Returns: The number of samples written to the ringbuffer or -1 on
 * error.
 *
 * MT safe.
 */
guint
gst_ring_buffer_commit (GstRingBuffer * buf, guint64 sample, guchar * data,
    guint len)
{
  guint res;
  guint64 samplep = sample;

  res = gst_ring_buffer_commit_full (buf, &samplep, data, len, len, NULL);

  return res;
}

/**
 * gst_ring_buffer_read:
 * @buf: the #GstRingBuffer to read from
 * @sample: the sample position of the data
 * @data: where the data should be read
 * @len: the number of samples in data to read
 *
 * Read @len samples from the ringbuffer into the memory pointed 
 * to by @data.
 * The first sample should be read from position @sample in
 * the ringbuffer.
 *
 * @len should not be a multiple of the segment size of the ringbuffer
 * although it is recommended.
 *
 * Returns: The number of samples read from the ringbuffer or -1 on
 * error.
 *
 * MT safe.
 */
guint
gst_ring_buffer_read (GstRingBuffer * buf, guint64 sample, guchar * data,
    guint len)
{
  gint segdone;
  gint segsize, segtotal, bps, sps;
  guint8 *dest;
  guint to_read;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), -1);
  g_return_val_if_fail (buf->data != NULL, -1);
  g_return_val_if_fail (data != NULL, -1);

  dest = GST_BUFFER_DATA (buf->data);
  segsize = buf->spec.segsize;
  segtotal = buf->spec.segtotal;
  bps = buf->spec.bytes_per_sample;
  sps = buf->samples_per_seg;

  to_read = len;
  /* read enough samples */
  while (to_read > 0) {
    gint sampleslen;
    gint readseg, sampleoff;

    /* figure out the segment and the offset inside the segment where
     * the sample should be read from. */
    readseg = sample / sps;
    sampleoff = (sample % sps);

    while (TRUE) {
      gint diff;

      /* get the currently processed segment */
      segdone = g_atomic_int_get (&buf->segdone) - buf->segbase;

      /* see how far away it is from the read segment, normally segdone (where
       * the hardware is writing) is bigger than readseg (where software is
       * reading) */
      diff = segdone - readseg;

      GST_DEBUG
          ("pointer at %d, sample %" G_GUINT64_FORMAT
          ", read from %d-%d, to_read %d, diff %d, segtotal %d, segsize %d",
          segdone, sample, readseg, sampleoff, to_read, diff, segtotal,
          segsize);

      /* segment too far ahead, reader too slow */
      if (G_UNLIKELY (diff >= segtotal)) {
        /* pretend we read an empty segment. */
        sampleslen = MIN (sps, to_read);
        memcpy (data, buf->empty_seg, sampleslen * bps);
        goto next;
      }

      /* read segment is within readable range, we can break the loop and
       * start reading the data. */
      if (diff > 0)
        break;

      /* else we need to wait for the segment to become readable. */
      if (!wait_segment (buf))
        goto not_started;
    }

    /* we can read now */
    readseg = readseg % segtotal;
    sampleslen = MIN (sps - sampleoff, to_read);

    GST_DEBUG_OBJECT (buf, "read @%p seg %d, off %d, sampleslen %d",
        dest + readseg * segsize, readseg, sampleoff, sampleslen);

    memcpy (data, dest + (readseg * segsize) + (sampleoff * bps),
        (sampleslen * bps));

  next:
    to_read -= sampleslen;
    sample += sampleslen;
    data += sampleslen * bps;
  }

  return len - to_read;

  /* ERRORS */
not_started:
  {
    GST_DEBUG_OBJECT (buf, "stopped processing");
    return len - to_read;
  }
}

/**
 * gst_ring_buffer_prepare_read:
 * @buf: the #GstRingBuffer to read from
 * @segment: the segment to read
 * @readptr: the pointer to the memory where samples can be read
 * @len: the number of bytes to read
 *
 * Returns a pointer to memory where the data from segment @segment
 * can be found. This function is mostly used by subclasses.
 *
 * Returns: FALSE if the buffer is not started.
 *
 * MT safe.
 */
gboolean
gst_ring_buffer_prepare_read (GstRingBuffer * buf, gint * segment,
    guint8 ** readptr, gint * len)
{
  guint8 *data;
  gint segdone;

  g_return_val_if_fail (GST_IS_RING_BUFFER (buf), FALSE);

  if (buf->callback == NULL) {
    /* push mode, fail when nothing is started */
    if (g_atomic_int_get (&buf->state) != GST_RING_BUFFER_STATE_STARTED)
      return FALSE;
  }

  g_return_val_if_fail (buf->data != NULL, FALSE);
  g_return_val_if_fail (segment != NULL, FALSE);
  g_return_val_if_fail (readptr != NULL, FALSE);
  g_return_val_if_fail (len != NULL, FALSE);

  data = GST_BUFFER_DATA (buf->data);

  /* get the position of the pointer */
  segdone = g_atomic_int_get (&buf->segdone);

  *segment = segdone % buf->spec.segtotal;
  *len = buf->spec.segsize;
  *readptr = data + *segment * *len;

  GST_LOG ("prepare read from segment %d (real %d) @%p",
      *segment, segdone, *readptr);

  /* callback to fill the memory with data, for pull based
   * scheduling. */
  if (buf->callback)
    buf->callback (buf, *readptr, *len, buf->cb_data);

  return TRUE;
}

/**
 * gst_ring_buffer_advance:
 * @buf: the #GstRingBuffer to advance
 * @advance: the number of segments written
 *
 * Subclasses should call this function to notify the fact that 
 * @advance segments are now processed by the device.
 *
 * MT safe.
 */
void
gst_ring_buffer_advance (GstRingBuffer * buf, guint advance)
{
  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  /* update counter */
  g_atomic_int_add (&buf->segdone, advance);

  /* the lock is already taken when the waiting flag is set,
   * we grab the lock as well to make sure the waiter is actually
   * waiting for the signal */
  if (g_atomic_int_compare_and_exchange (&buf->waiting, 1, 0)) {
    GST_OBJECT_LOCK (buf);
    GST_DEBUG_OBJECT (buf, "signal waiter");
    GST_RING_BUFFER_SIGNAL (buf);
    GST_OBJECT_UNLOCK (buf);
  }
}

/**
 * gst_ring_buffer_clear:
 * @buf: the #GstRingBuffer to clear
 * @segment: the segment to clear
 *
 * Clear the given segment of the buffer with silence samples.
 * This function is used by subclasses.
 *
 * MT safe.
 */
void
gst_ring_buffer_clear (GstRingBuffer * buf, gint segment)
{
  guint8 *data;

  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  /* no data means it's already cleared */
  if (G_UNLIKELY (buf->data == NULL))
    return;

  /* no empty_seg means it's not opened */
  if (G_UNLIKELY (buf->empty_seg == NULL))
    return;

  segment %= buf->spec.segtotal;

  data = GST_BUFFER_DATA (buf->data);
  data += segment * buf->spec.segsize;

  GST_LOG ("clear segment %d @%p", segment, data);

  memcpy (data, buf->empty_seg, buf->spec.segsize);
}

/**
 * gst_ring_buffer_may_start:
 * @buf: the #GstRingBuffer
 * @allowed: the new value
 *
 * Tell the ringbuffer that it is allowed to start playback when
 * the ringbuffer is filled with samples. 
 *
 * MT safe.
 *
 * Since: 0.10.6
 */
void
gst_ring_buffer_may_start (GstRingBuffer * buf, gboolean allowed)
{
  g_return_if_fail (GST_IS_RING_BUFFER (buf));

  GST_LOG_OBJECT (buf, "may start: %d", allowed);
  g_atomic_int_set (&buf->abidata.ABI.may_start, allowed);
}
