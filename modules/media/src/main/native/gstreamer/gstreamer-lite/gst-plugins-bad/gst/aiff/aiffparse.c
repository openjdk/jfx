/* -*- Mode: C; tab-width: 2; indent-tabs-mode: t; c-basic-offset: 2 -*- */
/* GStreamer AIFF parser
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 *               <2006> Nokia Corporation, Stefan Kost <stefan.kost@nokia.com>.
 *               <2008> Pioneers of the Inevitable <songbird@songbirdnest.com>
 *
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
 * SECTION:element-aiffparse
 *
 * <refsect2>
 * <para>
 * Parse a .aiff file into raw or compressed audio.
 * </para>
 * <para>
 * AIFFparse supports both push and pull mode operations, making it possible to
 * stream from a network source.
 * </para>
 * <title>Example launch line</title>
 * <para>
 * <programlisting>
 * gst-launch filesrc location=sine.aiff ! aiffparse ! audioconvert ! alsasink
 * </programlisting>
 * Read a aiff file and output to the soundcard using the ALSA element. The
 * aiff file is assumed to contain raw uncompressed samples.
 * </para>
 * <para>
 * <programlisting>
 * gst-launch gnomevfssrc location=http://www.example.org/sine.aiff ! queue ! aiffparse ! audioconvert ! alsasink
 * </programlisting>
 * Stream data from a network url.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include <string.h>
#include <math.h>

#include "aiffparse.h"
#include <gst/audio/audio.h>
#include <gst/gst-i18n-plugin.h>

GST_DEBUG_CATEGORY (aiffparse_debug);
#define GST_CAT_DEFAULT (aiffparse_debug)

static void gst_aiff_parse_dispose (GObject * object);

static gboolean gst_aiff_parse_sink_activate (GstPad * sinkpad);
static gboolean gst_aiff_parse_sink_activate_pull (GstPad * sinkpad,
    gboolean active);
static gboolean gst_aiff_parse_send_event (GstElement * element,
    GstEvent * event);
static GstStateChangeReturn gst_aiff_parse_change_state (GstElement * element,
    GstStateChange transition);

static const GstQueryType *gst_aiff_parse_get_query_types (GstPad * pad);
static gboolean gst_aiff_parse_pad_query (GstPad * pad, GstQuery * query);
static gboolean gst_aiff_parse_pad_convert (GstPad * pad,
    GstFormat src_format,
    gint64 src_value, GstFormat * dest_format, gint64 * dest_value);

static GstFlowReturn gst_aiff_parse_chain (GstPad * pad, GstBuffer * buf);
#ifdef GSTREAMER_LITE
static gboolean gst_aiff_parse_sink_event (GstPad * pad, GstEvent * event);
#endif // GSTREAMER_LITE
static void gst_aiff_parse_loop (GstPad * pad);
static gboolean gst_aiff_parse_srcpad_event (GstPad * pad, GstEvent * event);

static GstStaticPadTemplate sink_template_factory =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-aiff")
    );

static GstStaticPadTemplate src_template_factory =
    GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (GST_AUDIO_INT_PAD_TEMPLATE_CAPS ";"
        GST_AUDIO_FLOAT_PAD_TEMPLATE_CAPS)
    );

GST_BOILERPLATE (GstAiffParse, gst_aiff_parse, GstElement, GST_TYPE_ELEMENT);

static void
gst_aiff_parse_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template_factory));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&src_template_factory));

  gst_element_class_set_details_simple (element_class,
      "AIFF audio demuxer", "Codec/Demuxer/Audio",
      "Parse a .aiff file into raw audio",
      "Pioneers of the Inevitable <songbird@songbirdnest.com>");
}

static void
gst_aiff_parse_class_init (GstAiffParseClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *object_class;

  gstelement_class = (GstElementClass *) klass;
  object_class = (GObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  object_class->dispose = gst_aiff_parse_dispose;

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_aiff_parse_change_state);
  gstelement_class->send_event = GST_DEBUG_FUNCPTR (gst_aiff_parse_send_event);
}

static void
gst_aiff_parse_reset (GstAiffParse * aiff)
{
  aiff->state = AIFF_PARSE_START;

  /* These will all be set correctly in the fmt chunk */
  aiff->rate = 0;
  aiff->width = 0;
  aiff->depth = 0;
  aiff->channels = 0;
  aiff->bps = 0;
  aiff->offset = 0;
  aiff->end_offset = 0;
  aiff->dataleft = 0;
  aiff->datasize = 0;
  aiff->datastart = 0;
  aiff->duration = 0;
  aiff->got_comm = FALSE;

  if (aiff->caps) {
    gst_caps_unref (aiff->caps);
    aiff->caps = NULL;
  }
  if (aiff->seek_event)
    gst_event_unref (aiff->seek_event);
  aiff->seek_event = NULL;
  if (aiff->adapter) {
    gst_adapter_clear (aiff->adapter);
#ifdef GSTREAMER_LITE
    g_object_unref (aiff->adapter);
#endif // GSTREAMER_LITE
    aiff->adapter = NULL;
  }
}

static void
gst_aiff_parse_dispose (GObject * object)
{
  GstAiffParse *aiff = GST_AIFF_PARSE (object);

  GST_DEBUG_OBJECT (aiff, "AIFF: Dispose");
  gst_aiff_parse_reset (aiff);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_aiff_parse_init (GstAiffParse * aiffparse, GstAiffParseClass * g_class)
{
  gst_aiff_parse_reset (aiffparse);

  /* sink */
  aiffparse->sinkpad =
      gst_pad_new_from_static_template (&sink_template_factory, "sink");
  gst_pad_set_activate_function (aiffparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_sink_activate));
  gst_pad_set_activatepull_function (aiffparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_sink_activate_pull));
  gst_pad_set_chain_function (aiffparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_chain));
#ifdef GSTREAMER_LITE
  gst_pad_set_event_function (aiffparse->sinkpad, GST_DEBUG_FUNCPTR (gst_aiff_parse_sink_event));
#endif // GSTREAMER_LITE
  gst_element_add_pad (GST_ELEMENT_CAST (aiffparse), aiffparse->sinkpad);

  /* source */
  aiffparse->srcpad =
      gst_pad_new_from_static_template (&src_template_factory, "src");
  gst_pad_use_fixed_caps (aiffparse->srcpad);
  gst_pad_set_query_type_function (aiffparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_get_query_types));
  gst_pad_set_query_function (aiffparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_pad_query));
  gst_pad_set_event_function (aiffparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_aiff_parse_srcpad_event));
  gst_element_add_pad (GST_ELEMENT_CAST (aiffparse), aiffparse->srcpad);
}

// Reverting back this method because it's necessary in other GSTREAMER_LITE methods.
#ifdef GSTREAMER_LITE
/* Compute (value * nom) % denom, avoiding overflow.  This can be used
 * to perform ceiling or rounding division together with
 * gst_util_uint64_scale[_int]. */
#define uint64_scale_modulo(val, nom, denom) \
  ((val % denom) * (nom % denom) % denom)

/* Like gst_util_uint64_scale, but performs ceiling division. */
static guint64
uint64_ceiling_scale (guint64 val, guint64 num, guint64 denom)
{
  guint64 result = gst_util_uint64_scale (val, num, denom);

  if (uint64_scale_modulo (val, num, denom) == 0)
    return result;
  else
    return result + 1;
}
#endif // GSTREAMER_LITE

static gboolean
gst_aiff_parse_parse_file_header (GstAiffParse * aiff, GstBuffer * buf)
{
  guint8 *data;
  guint32 header, type = 0;

  if (GST_BUFFER_SIZE (buf) < 12) {
    GST_WARNING_OBJECT (aiff, "Buffer too short");
    goto not_aiff;
  }

  data = GST_BUFFER_DATA (buf);

  header = GST_READ_UINT32_LE (data);
  type = GST_READ_UINT32_LE (data + 8);

  if (header != GST_MAKE_FOURCC ('F', 'O', 'R', 'M'))
    goto not_aiff;

  if (type == GST_MAKE_FOURCC ('A', 'I', 'F', 'F'))
    aiff->is_aifc = FALSE;
  else if (type == GST_MAKE_FOURCC ('A', 'I', 'F', 'C'))
    aiff->is_aifc = TRUE;
  else
    goto not_aiff;

  gst_buffer_unref (buf);
  return TRUE;

  /* ERRORS */
not_aiff:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, WRONG_TYPE, (NULL),
        ("File is not an AIFF file: %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (type)));
    gst_buffer_unref (buf);
    return FALSE;
  }
}

static GstFlowReturn
gst_aiff_parse_stream_init (GstAiffParse * aiff)
{
  GstFlowReturn res;
  GstBuffer *buf = NULL;

  if ((res = gst_pad_pull_range (aiff->sinkpad,
              aiff->offset, 12, &buf)) != GST_FLOW_OK)
    return res;
  else if (!gst_aiff_parse_parse_file_header (aiff, buf))
    return GST_FLOW_ERROR;

  aiff->offset += 12;

  return GST_FLOW_OK;
}

#ifdef GSTREAMER_LITE
static gboolean
gst_aiff_parse_time_to_bytepos (GstAiffParse * aiff, gint64 ts, gint64 * bytepos)
{
  /* -1 always maps to -1 */
  if (ts == -1) {
    *bytepos = -1;
    return TRUE;
  }

  /* 0 always maps to 0 */
  if (ts == 0) {
    *bytepos = 0;
    return TRUE;
  }

  if (aiff->bps > 0) {
    *bytepos = uint64_ceiling_scale (ts, (guint64) aiff->bps, GST_SECOND);
    return TRUE;
  }

  return FALSE;
}
#endif // GSTREAMER_LITE

/* This function is used to perform seeks on the element in
 * pull mode.
 *
 * It also works when event is NULL, in which case it will just
 * start from the last configured segment. This technique is
 * used when activating the element and to perform the seek in
 * READY.
 */
static gboolean
gst_aiff_parse_perform_seek (GstAiffParse * aiff, GstEvent * event)
{
  gboolean res;
  gdouble rate;
  GstFormat format, bformat;
  GstSeekFlags flags;
  GstSeekType cur_type = GST_SEEK_TYPE_NONE, stop_type;
  gint64 cur, stop, upstream_size;
  gboolean flush;
  gboolean update;
  GstSegment seeksegment = { 0, };
  gint64 last_stop;

  if (event) {
    GST_DEBUG_OBJECT (aiff, "doing seek with event");

    gst_event_parse_seek (event, &rate, &format, &flags,
        &cur_type, &cur, &stop_type, &stop);

    /* no negative rates yet */
    if (rate < 0.0)
      goto negative_rate;

    if (format != aiff->segment.format) {
      GST_INFO_OBJECT (aiff, "converting seek-event from %s to %s",
          gst_format_get_name (format),
          gst_format_get_name (aiff->segment.format));
      res = TRUE;
      if (cur_type != GST_SEEK_TYPE_NONE)
        res =
            gst_pad_query_convert (aiff->srcpad, format, cur,
            &aiff->segment.format, &cur);
      if (res && stop_type != GST_SEEK_TYPE_NONE)
        res =
            gst_pad_query_convert (aiff->srcpad, format, stop,
            &aiff->segment.format, &stop);
      if (!res)
        goto no_format;

      format = aiff->segment.format;
    }
  } else {
    GST_DEBUG_OBJECT (aiff, "doing seek without event");
    flags = 0;
    rate = 1.0;
    cur_type = GST_SEEK_TYPE_SET;
    stop_type = GST_SEEK_TYPE_SET;
  }

#ifdef GSTREAMER_LITE
  /* in push mode, we must delegate to upstream */
  if (aiff->streaming) {
    gboolean res = FALSE;

    /* if streaming not yet started; only prepare initial newsegment */
    if (!event || aiff->state != AIFF_PARSE_DATA) {
      if (aiff->start_segment)
        gst_event_unref (aiff->start_segment);
      aiff->start_segment =
          gst_event_new_new_segment (FALSE, aiff->segment.rate,
          aiff->segment.format, aiff->segment.last_stop, aiff->segment.duration,
          aiff->segment.last_stop);
      res = TRUE;
    } else {
      /* convert seek positions to byte positions in data sections */
      if (format == GST_FORMAT_TIME) {
        /* should not fail */
        if (!gst_aiff_parse_time_to_bytepos (aiff, cur, &cur))
          goto no_position;
        if (!gst_aiff_parse_time_to_bytepos (aiff, stop, &stop))
          goto no_position;
      }
      /* mind sample boundary and header */
      if (cur >= 0) {
        cur -= (cur % aiff->bytes_per_sample);
        cur += aiff->datastart;
      }
      if (stop >= 0) {
        stop -= (stop % aiff->bytes_per_sample);
        stop += aiff->datastart;
      }
      GST_DEBUG_OBJECT (aiff, "Pushing BYTE seek rate %g, "
          "start %" G_GINT64_FORMAT ", stop %" G_GINT64_FORMAT, rate, cur,
          stop);
      /* BYTE seek event */
      event = gst_event_new_seek (rate, GST_FORMAT_BYTES, flags, cur_type, cur,
          stop_type, stop);
      res = gst_pad_push_event (aiff->sinkpad, event);
    }
    return res;
  }
#endif // GSTREAMER_LITE

  /* get flush flag */
  flush = flags & GST_SEEK_FLAG_FLUSH;

  /* now we need to make sure the streaming thread is stopped. We do this by
   * either sending a FLUSH_START event downstream which will cause the
   * streaming thread to stop with a WRONG_STATE.
   * For a non-flushing seek we simply pause the task, which will happen as soon
   * as it completes one iteration (and thus might block when the sink is
   * blocking in preroll). */
  if (flush) {
    GST_DEBUG_OBJECT (aiff, "sending flush start");
    gst_pad_push_event (aiff->srcpad, gst_event_new_flush_start ());
  } else {
    gst_pad_pause_task (aiff->sinkpad);
  }

  /* we should now be able to grab the streaming thread because we stopped it
   * with the above flush/pause code */
  GST_PAD_STREAM_LOCK (aiff->sinkpad);

  /* save current position */
  last_stop = aiff->segment.last_stop;

  GST_DEBUG_OBJECT (aiff, "stopped streaming at %" G_GINT64_FORMAT, last_stop);

  /* copy segment, we need this because we still need the old
   * segment when we close the current segment. */
  memcpy (&seeksegment, &aiff->segment, sizeof (GstSegment));

  /* configure the seek parameters in the seeksegment. We will then have the
   * right values in the segment to perform the seek */
  if (event) {
    GST_DEBUG_OBJECT (aiff, "configuring seek");
    gst_segment_set_seek (&seeksegment, rate, format, flags,
        cur_type, cur, stop_type, stop, &update);
  }

  /* figure out the last position we need to play. If it's configured (stop !=
   * -1), use that, else we play until the total duration of the file */
  if ((stop = seeksegment.stop) == -1)
    stop = seeksegment.duration;

  GST_DEBUG_OBJECT (aiff, "cur_type =%d", cur_type);
  if ((cur_type != GST_SEEK_TYPE_NONE)) {
    /* bring offset to bytes, if the bps is 0, we have the segment in BYTES and
     * we can just copy the last_stop. If not, we use the bps to convert TIME to
     * bytes. */
    if (aiff->bps > 0)
      aiff->offset =
          gst_util_uint64_scale_ceil (seeksegment.last_stop,
          (guint64) aiff->bps, GST_SECOND);
    else
      aiff->offset = seeksegment.last_stop;
    GST_LOG_OBJECT (aiff, "offset=%" G_GUINT64_FORMAT, aiff->offset);
    aiff->offset -= (aiff->offset % aiff->bytes_per_sample);
    GST_LOG_OBJECT (aiff, "offset=%" G_GUINT64_FORMAT, aiff->offset);
    aiff->offset += aiff->datastart;
    GST_LOG_OBJECT (aiff, "offset=%" G_GUINT64_FORMAT, aiff->offset);
  } else {
    GST_LOG_OBJECT (aiff, "continue from offset=%" G_GUINT64_FORMAT,
        aiff->offset);
  }

  if (stop_type != GST_SEEK_TYPE_NONE) {
    if (aiff->bps > 0)
      aiff->end_offset =
          gst_util_uint64_scale_ceil (stop, (guint64) aiff->bps, GST_SECOND);
    else
      aiff->end_offset = stop;
    GST_LOG_OBJECT (aiff, "end_offset=%" G_GUINT64_FORMAT, aiff->end_offset);
    aiff->end_offset -= (aiff->end_offset % aiff->bytes_per_sample);
    GST_LOG_OBJECT (aiff, "end_offset=%" G_GUINT64_FORMAT, aiff->end_offset);
    aiff->end_offset += aiff->datastart;
    GST_LOG_OBJECT (aiff, "end_offset=%" G_GUINT64_FORMAT, aiff->end_offset);
  } else {
    GST_LOG_OBJECT (aiff, "continue to end_offset=%" G_GUINT64_FORMAT,
        aiff->end_offset);
  }

  /* make sure filesize is not exceeded due to rounding errors or so,
   * same precaution as in _stream_headers */
  bformat = GST_FORMAT_BYTES;
  if (gst_pad_query_peer_duration (aiff->sinkpad, &bformat, &upstream_size))
    aiff->end_offset = MIN (aiff->end_offset, upstream_size);

  /* this is the range of bytes we will use for playback */
  aiff->offset = MIN (aiff->offset, aiff->end_offset);
  aiff->dataleft = aiff->end_offset - aiff->offset;

  GST_DEBUG_OBJECT (aiff,
      "seek: rate %lf, offset %" G_GUINT64_FORMAT ", end %" G_GUINT64_FORMAT
      ", segment %" GST_TIME_FORMAT " -- %" GST_TIME_FORMAT, rate, aiff->offset,
      aiff->end_offset, GST_TIME_ARGS (seeksegment.start),
      GST_TIME_ARGS (stop));

  /* prepare for streaming again */
  if (flush) {
    /* if we sent a FLUSH_START, we now send a FLUSH_STOP */
    GST_DEBUG_OBJECT (aiff, "sending flush stop");
    gst_pad_push_event (aiff->srcpad, gst_event_new_flush_stop ());
  } else if (aiff->segment_running) {
    /* we are running the current segment and doing a non-flushing seek,
     * close the segment first based on the previous last_stop. */
    GST_DEBUG_OBJECT (aiff, "closing running segment %" G_GINT64_FORMAT
        " to %" G_GINT64_FORMAT, aiff->segment.accum, aiff->segment.last_stop);

    /* queue the segment for sending in the stream thread */
    if (aiff->close_segment)
      gst_event_unref (aiff->close_segment);
    aiff->close_segment = gst_event_new_new_segment (TRUE,
        aiff->segment.rate, aiff->segment.format,
        aiff->segment.accum, aiff->segment.last_stop, aiff->segment.accum);

    /* keep track of our last_stop */
    seeksegment.accum = aiff->segment.last_stop;
  }

  /* now we did the seek and can activate the new segment values */
  memcpy (&aiff->segment, &seeksegment, sizeof (GstSegment));

  /* if we're doing a segment seek, post a SEGMENT_START message */
  if (aiff->segment.flags & GST_SEEK_FLAG_SEGMENT) {
    gst_element_post_message (GST_ELEMENT_CAST (aiff),
        gst_message_new_segment_start (GST_OBJECT_CAST (aiff),
            aiff->segment.format, aiff->segment.last_stop));
  }

  /* now create the newsegment */
  GST_DEBUG_OBJECT (aiff, "Creating newsegment from %" G_GINT64_FORMAT
      " to %" G_GINT64_FORMAT, aiff->segment.last_stop, stop);

  /* store the newsegment event so it can be sent from the streaming thread. */
  if (aiff->start_segment)
    gst_event_unref (aiff->start_segment);
  aiff->start_segment =
      gst_event_new_new_segment (FALSE, aiff->segment.rate,
      aiff->segment.format, aiff->segment.last_stop, stop,
      aiff->segment.last_stop);

  /* mark discont if we are going to stream from another position. */
  if (last_stop != aiff->segment.last_stop) {
    GST_DEBUG_OBJECT (aiff, "mark DISCONT, we did a seek to another position");
    aiff->discont = TRUE;
  }

  /* and start the streaming task again */
  aiff->segment_running = TRUE;
  if (!aiff->streaming) {
    gst_pad_start_task (aiff->sinkpad, (GstTaskFunction) gst_aiff_parse_loop,
        aiff->sinkpad);
  }

  GST_PAD_STREAM_UNLOCK (aiff->sinkpad);

  return TRUE;

  /* ERRORS */
negative_rate:
  {
    GST_DEBUG_OBJECT (aiff, "negative playback rates are not supported yet.");
    return FALSE;
  }
no_format:
  {
    GST_DEBUG_OBJECT (aiff, "unsupported format given, seek aborted.");
    return FALSE;
  }
#ifdef GSTREAMER_LITE
no_position:
  {
    GST_DEBUG_OBJECT (aiff,
        "Could not determine byte position for desired time");
    return FALSE;
  }
#endif // GSTREAMER_LITE
}

/*
 * gst_aiff_parse_peek_chunk_info:
 * @aiff AIFFparse object
 * @tag holder for tag
 * @size holder for tag size
 *
 * Peek next chunk info (tag and size)
 *
 * Returns: %TRUE when the chunk info (header) is available
 */
static gboolean
gst_aiff_parse_peek_chunk_info (GstAiffParse * aiff, guint32 * tag,
    guint32 * size)
{
  const guint8 *data = NULL;

  if (gst_adapter_available (aiff->adapter) < 8)
    return FALSE;

  data = gst_adapter_peek (aiff->adapter, 8);
  *tag = GST_READ_UINT32_LE (data);
  *size = GST_READ_UINT32_BE (data + 4);

  GST_DEBUG ("Next chunk size is %d bytes, type %" GST_FOURCC_FORMAT, *size,
      GST_FOURCC_ARGS (*tag));

  return TRUE;
}

/*
 * gst_aiff_parse_peek_chunk:
 * @aiff AIFFparse object
 * @tag holder for tag
 * @size holder for tag size
 *
 * Peek enough data for one full chunk
 *
 * Returns: %TRUE when the full chunk is available
 */
static gboolean
gst_aiff_parse_peek_chunk (GstAiffParse * aiff, guint32 * tag, guint32 * size)
{
  guint32 peek_size = 0;
  guint available;

  if (!gst_aiff_parse_peek_chunk_info (aiff, tag, size))
    return FALSE;

  GST_DEBUG ("Need to peek chunk of %d bytes", *size);
  peek_size = (*size + 1) & ~1;

  available = gst_adapter_available (aiff->adapter);
  if (available >= (8 + peek_size)) {
    return TRUE;
  } else {
    GST_LOG ("but only %u bytes available now", available);
    return FALSE;
  }
}

static gboolean
gst_aiff_parse_peek_data (GstAiffParse * aiff, guint32 size,
    const guint8 ** data)
{
  if (gst_adapter_available (aiff->adapter) < size)
    return FALSE;

  *data = gst_adapter_peek (aiff->adapter, size);
  return TRUE;
}

/*
 * gst_aiff_parse_calculate_duration:
 * @aiff: aiffparse object
 *
 * Calculate duration on demand and store in @aiff.
 *
 * Returns: %TRUE if duration is available.
 */
static gboolean
gst_aiff_parse_calculate_duration (GstAiffParse * aiff)
{
  if (aiff->duration > 0)
    return TRUE;

  if (aiff->datasize > 0 && aiff->bps > 0) {
    aiff->duration =
        gst_util_uint64_scale_ceil (aiff->datasize, GST_SECOND,
        (guint64) aiff->bps);
    GST_INFO_OBJECT (aiff, "Got duration %" GST_TIME_FORMAT,
        GST_TIME_ARGS (aiff->duration));
    return TRUE;
  }
  return FALSE;
}

static void
gst_aiff_parse_ignore_chunk (GstAiffParse * aiff, GstBuffer * buf, guint32 tag,
    guint32 size)
{
#ifdef GSTREAMER_LITE
    guint64 flush;
#else
    guint flush;
#endif

  if (aiff->streaming) {
    if (!gst_aiff_parse_peek_chunk (aiff, &tag, &size))
      return;
  }
  GST_DEBUG_OBJECT (aiff, "Ignoring tag %" GST_FOURCC_FORMAT,
      GST_FOURCC_ARGS (tag));
#ifdef GSTREAMER_LITE
  flush = 8 + (((guint64)size + 1) & ~1);
#else
  flush = 8 + ((size + 1) & ~1);
#endif
  aiff->offset += flush;
  if (aiff->streaming) {
    gst_adapter_flush (aiff->adapter, flush);
  } else {
    gst_buffer_unref (buf);
  }
}

static double
gst_aiff_parse_read_IEEE80 (guint8 * buf)
{
  int s = buf[0] & 0xff;
  int e = ((buf[0] & 0x7f) << 8) | (buf[1] & 0xff);
  double f = ((unsigned long) (buf[2] & 0xff) << 24) |
      ((buf[3] & 0xff) << 16) | ((buf[4] & 0xff) << 8) | (buf[5] & 0xff);

  if (e == 32767) {
    if (buf[2] & 0x80)
      return HUGE_VAL;          /* Really NaN, but this won't happen in reality */
    else {
      if (s)
        return -HUGE_VAL;
      else
        return HUGE_VAL;
    }
  }

  f = ldexp (f, 32);
  f += ((buf[6] & 0xff) << 24) |
      ((buf[7] & 0xff) << 16) | ((buf[8] & 0xff) << 8) | (buf[9] & 0xff);

  return ldexp (f, e - 16446);
}

static gboolean
gst_aiff_parse_parse_comm (GstAiffParse * aiff, GstBuffer * buf)
{
  guint8 *data;
  int size;

  if (aiff->is_aifc)
    size = 22;
  else
    size = 18;

  if (GST_BUFFER_SIZE (buf) < size) {
    GST_WARNING_OBJECT (aiff, "COMM chunk too short, cannot parse header");
    return FALSE;
  }

  data = GST_BUFFER_DATA (buf);

  aiff->channels = GST_READ_UINT16_BE (data);
  aiff->total_frames = GST_READ_UINT32_BE (data + 2);
  aiff->depth = GST_READ_UINT16_BE (data + 6);
  aiff->width = GST_ROUND_UP_8 (aiff->depth);
  aiff->rate = (int) gst_aiff_parse_read_IEEE80 (data + 8);

  aiff->floating_point = FALSE;

  if (aiff->is_aifc) {
    guint32 fourcc = GST_READ_UINT32_LE (data + 18);

    /* We only support the 'trivial' uncompressed AIFC, but it can be
     * either big or little endian */
    switch (fourcc) {
      case GST_MAKE_FOURCC ('N', 'O', 'N', 'E'):
        aiff->endianness = G_BIG_ENDIAN;
        break;
      case GST_MAKE_FOURCC ('s', 'o', 'w', 't'):
        aiff->endianness = G_LITTLE_ENDIAN;
        break;
      case GST_MAKE_FOURCC ('F', 'L', '3', '2'):
      case GST_MAKE_FOURCC ('f', 'l', '3', '2'):
        aiff->floating_point = TRUE;
        aiff->width = aiff->depth = 32;
        aiff->endianness = G_BIG_ENDIAN;
        break;
      case GST_MAKE_FOURCC ('f', 'l', '6', '4'):
        aiff->floating_point = TRUE;
        aiff->width = aiff->depth = 64;
        aiff->endianness = G_BIG_ENDIAN;
        break;
      default:
        GST_WARNING_OBJECT (aiff, "Unsupported compression in AIFC "
            "file: %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (GST_READ_UINT32_LE (data + 18)));
        return FALSE;

    }
  } else
    aiff->endianness = G_BIG_ENDIAN;

  return TRUE;
}

static GstFlowReturn
gst_aiff_parse_read_chunk (GstAiffParse * aiff, guint64 * offset, guint32 * tag,
    GstBuffer ** data)
{
  guint size;
  GstFlowReturn res;
  GstBuffer *buf;

  if ((res =
          gst_pad_pull_range (aiff->sinkpad, *offset, 8, &buf)) != GST_FLOW_OK)
    return res;

  *tag = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf));
  size = GST_READ_UINT32_BE (GST_BUFFER_DATA (buf) + 4);

#ifdef GSTREAMER_LITE
  gst_buffer_unref (buf);
  buf = NULL;
#endif // GSTREAMER_LITE

  if ((res =
          gst_pad_pull_range (aiff->sinkpad, (*offset) + 8, size,
              &buf)) != GST_FLOW_OK)
    return res;
  else if (GST_BUFFER_SIZE (buf) < size)
    goto too_small;

  *data = buf;
  *offset += 8 + GST_ROUND_UP_2 (size);

  return GST_FLOW_OK;

  /* ERRORS */
too_small:
  {
    /* short read, we return UNEXPECTED to mark the EOS case */
    GST_DEBUG_OBJECT (aiff, "not enough data (available=%u, needed=%u)",
        GST_BUFFER_SIZE (buf), size);
    gst_buffer_unref (buf);
    return GST_FLOW_UNEXPECTED;
  }

}

static GstCaps *
gst_aiff_parse_create_caps (GstAiffParse * aiff)
{
  GstCaps *caps;

  if (aiff->floating_point) {
    caps = gst_caps_new_simple ("audio/x-raw-float",
        "width", G_TYPE_INT, aiff->width,
        "channels", G_TYPE_INT, aiff->channels,
        "endianness", G_TYPE_INT, aiff->endianness,
        "rate", G_TYPE_INT, aiff->rate, NULL);
  } else {
    caps = gst_caps_new_simple ("audio/x-raw-int",
        "width", G_TYPE_INT, aiff->width,
        "depth", G_TYPE_INT, aiff->depth,
        "channels", G_TYPE_INT, aiff->channels,
        "endianness", G_TYPE_INT, aiff->endianness,
        "rate", G_TYPE_INT, aiff->rate, "signed", G_TYPE_BOOLEAN, TRUE, NULL);
  }

  GST_DEBUG_OBJECT (aiff, "Created caps: %" GST_PTR_FORMAT, caps);
  return caps;
}

static GstFlowReturn
gst_aiff_parse_stream_headers (GstAiffParse * aiff)
{
  GstFlowReturn res;
  GstBuffer *buf;
  guint32 tag, size;
  gboolean gotdata = FALSE;
  gboolean done = FALSE;
  GstEvent **event_p;
  GstFormat bformat;
  gint64 upstream_size = 0;
#ifdef GSTREAMER_LITE
  guint32 chunkSize = 0;
#endif // GSTREAMER_LITE

  bformat = GST_FORMAT_BYTES;
  gst_pad_query_peer_duration (aiff->sinkpad, &bformat, &upstream_size);
  GST_DEBUG_OBJECT (aiff, "upstream size %" G_GUINT64_FORMAT, upstream_size);

  /* loop headers until we get data */
  while (!done) {
    if (aiff->streaming) {
      if (!gst_aiff_parse_peek_chunk_info (aiff, &tag, &size))
        return GST_FLOW_OK;
    } else {
      if ((res =
              gst_pad_pull_range (aiff->sinkpad, aiff->offset, 8,
                  &buf)) != GST_FLOW_OK)
        goto header_read_error;
      tag = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf));
      size = GST_READ_UINT32_BE (GST_BUFFER_DATA (buf) + 4);
    }

    GST_INFO_OBJECT (aiff,
        "Got TAG: %" GST_FOURCC_FORMAT ", offset %" G_GUINT64_FORMAT,
        GST_FOURCC_ARGS (tag), aiff->offset);

    /* We just keep reading chunks until we find the one we're interested in.
     */
    switch (tag) {
      case GST_MAKE_FOURCC ('C', 'O', 'M', 'M'):{
        if (aiff->streaming) {
          if (!gst_aiff_parse_peek_chunk (aiff, &tag, &size))
            return GST_FLOW_OK;

          gst_adapter_flush (aiff->adapter, 8);
          aiff->offset += 8;

          buf = gst_adapter_take_buffer (aiff->adapter, size);
#ifdef GSTREAMER_LITE
          aiff->offset += size;
#endif // GSTREAMER_LITE
        } else {
#ifdef GSTREAMER_LITE
         gst_buffer_unref(buf);
#endif // GSTREAMER_LITE
          if ((res = gst_aiff_parse_read_chunk (aiff,
                      &aiff->offset, &tag, &buf)) != GST_FLOW_OK)
            return res;
        }

        if (!gst_aiff_parse_parse_comm (aiff, buf)) {
          gst_buffer_unref (buf);
          goto parse_header_error;
        }

        gst_buffer_unref (buf);

        /* do sanity checks of header fields */
        if (aiff->channels == 0)
          goto no_channels;
        if (aiff->rate == 0)
          goto no_rate;

        GST_DEBUG_OBJECT (aiff, "creating the caps");

        aiff->caps = gst_aiff_parse_create_caps (aiff);
        if (!aiff->caps)
          goto unknown_format;

        gst_pad_set_caps (aiff->srcpad, aiff->caps);

        aiff->bytes_per_sample = aiff->channels * aiff->width / 8;
        aiff->bps = aiff->bytes_per_sample * aiff->rate;

        if (aiff->bytes_per_sample <= 0)
          goto no_bytes_per_sample;

        aiff->got_comm = TRUE;
        break;
      }
      case GST_MAKE_FOURCC ('S', 'S', 'N', 'D'):{
        GstBuffer *ssndbuf = NULL;
        const guint8 *ssnddata = NULL;
        guint32 datasize;

        GST_DEBUG_OBJECT (aiff, "Got 'SSND' TAG, size : %d", size);

        /* Now, read the 8-byte header in the SSND chunk */
        if (aiff->streaming) {
          if (!gst_aiff_parse_peek_data (aiff, 16, &ssnddata))
            return GST_FLOW_OK;
        } else {
          gst_buffer_unref (buf);
          if ((res =
                  gst_pad_pull_range (aiff->sinkpad, aiff->offset, 16,
                      &ssndbuf)) != GST_FLOW_OK)
            goto header_read_error;
          ssnddata = GST_BUFFER_DATA (ssndbuf);
        }

#ifdef GSTREAMER_LITE
        chunkSize = GST_READ_UINT32_BE (ssnddata + 4);
#endif // GSTREAMER_LITE
        aiff->ssnd_offset = GST_READ_UINT32_BE (ssnddata + 8);
        aiff->ssnd_blocksize = GST_READ_UINT32_BE (ssnddata + 12);

        gotdata = TRUE;
        if (aiff->streaming) {
          gst_adapter_flush (aiff->adapter, 16);
        } else {
          gst_buffer_unref (ssndbuf);
        }
        /* 8 byte chunk header, 8 byte SSND header */
        aiff->offset += 16;

#ifndef GSTREAMER_LITE
        datasize = size - 16;
#else // GSTREAMER_LITE
        datasize = chunkSize - 8;
        if (datasize <= 0)
          datasize = size - 8;
#endif // GSTREAMER_LITE

        aiff->datastart = aiff->offset + aiff->ssnd_offset;
        /* file might be truncated */
        if (upstream_size) {
          size = MIN (datasize, (upstream_size - aiff->datastart));
        }
        aiff->datasize = (guint64) datasize;
        aiff->dataleft = (guint64) datasize;
        aiff->end_offset = datasize + aiff->datastart;
        if (!aiff->streaming) {
          /* We will continue looking at chunks until the end - to read tags,
           * etc. */
          aiff->offset += datasize;
        }
        GST_DEBUG_OBJECT (aiff, "datasize = %d", datasize);
        if (aiff->streaming) {
          done = TRUE;
        }
        break;
      }
      default:
        gst_aiff_parse_ignore_chunk (aiff, buf, tag, size);
    }

    if (upstream_size && (aiff->offset >= upstream_size)) {
      /* Now we have gone through the whole file */
      done = TRUE;
    }
  }

  /* We read all the chunks (in pull mode) or reached the SSND chunk
   * (in push mode). We must have both COMM and SSND now; error out 
   * otherwise.
   */
  if (!aiff->got_comm) {
    GST_WARNING_OBJECT (aiff, "Failed to find COMM chunk");
    goto no_header;
  }
  if (!gotdata) {
    GST_WARNING_OBJECT (aiff, "Failed to find SSND chunk");
    goto no_data;
  }

  GST_DEBUG_OBJECT (aiff, "Finished parsing headers");

  if (gst_aiff_parse_calculate_duration (aiff)) {
    gst_segment_init (&aiff->segment, GST_FORMAT_TIME);
    gst_segment_set_duration (&aiff->segment, GST_FORMAT_TIME, aiff->duration);
  } else {
    /* no bitrate, let downstream peer do the math, we'll feed it bytes. */
    gst_segment_init (&aiff->segment, GST_FORMAT_BYTES);
    gst_segment_set_duration (&aiff->segment, GST_FORMAT_BYTES, aiff->datasize);
  }

  /* now we have all the info to perform a pending seek if any, if no
   * event, this will still do the right thing and it will also send
   * the right newsegment event downstream. */
  gst_aiff_parse_perform_seek (aiff, aiff->seek_event);
  /* remove pending event */
  event_p = &aiff->seek_event;
  gst_event_replace (event_p, NULL);

  /* we just started, we are discont */
  aiff->discont = TRUE;

  aiff->state = AIFF_PARSE_DATA;

  return GST_FLOW_OK;

  /* ERROR */
no_header:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, TYPE_NOT_FOUND, (NULL),
        ("Invalid AIFF header (no COMM found)"));
    return GST_FLOW_ERROR;
  }
no_data:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, TYPE_NOT_FOUND, (NULL),
        ("Invalid AIFF: no SSND found"));
    return GST_FLOW_ERROR;
  }
parse_header_error:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, DEMUX, (NULL),
        ("Couldn't parse audio header"));
    return GST_FLOW_ERROR;
  }
no_channels:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, FAILED, (NULL),
        ("Stream claims to contain no channels - invalid data"));
    return GST_FLOW_ERROR;
  }
no_rate:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, FAILED, (NULL),
        ("Stream with sample_rate == 0 - invalid data"));
    return GST_FLOW_ERROR;
  }
no_bytes_per_sample:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, FAILED, (NULL),
        ("Could not caluclate bytes per sample - invalid data"));
    return GST_FLOW_ERROR;
  }
unknown_format:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, TYPE_NOT_FOUND, (NULL),
        ("No caps found for format 0x%x, %d channels, %d Hz",
            aiff->format, aiff->channels, aiff->rate));
    return GST_FLOW_ERROR;
  }
header_read_error:
  {
    GST_ELEMENT_ERROR (aiff, STREAM, DEMUX, (NULL),
        ("Couldn't read in header"));
    return GST_FLOW_ERROR;
  }
}

/*
 * Read AIFF file tag when streaming
 */
static GstFlowReturn
gst_aiff_parse_parse_stream_init (GstAiffParse * aiff)
{
  if (gst_adapter_available (aiff->adapter) >= 12) {
    GstBuffer *tmp;

    /* _take flushes the data */
    tmp = gst_adapter_take_buffer (aiff->adapter, 12);

    GST_DEBUG ("Parsing aiff header");
    if (!gst_aiff_parse_parse_file_header (aiff, tmp))
      return GST_FLOW_ERROR;

    aiff->offset += 12;
    /* Go to next state */
    aiff->state = AIFF_PARSE_HEADER;
  }
  return GST_FLOW_OK;
}

/* handle an event sent directly to the element.
 *
 * This event can be sent either in the READY state or the
 * >READY state. The only event of interest really is the seek
 * event.
 *
 * In the READY state we can only store the event and try to
 * respect it when going to PAUSED. We assume we are in the
 * READY state when our parsing state != AIFF_PARSE_DATA.
 *
 * When we are steaming, we can simply perform the seek right
 * away.
 */
static gboolean
gst_aiff_parse_send_event (GstElement * element, GstEvent * event)
{
  GstAiffParse *aiff = GST_AIFF_PARSE (element);
  gboolean res = FALSE;
  GstEvent **event_p;

  GST_DEBUG_OBJECT (aiff, "received event %s", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
      if (aiff->state == AIFF_PARSE_DATA) {
        /* we can handle the seek directly when streaming data */
        res = gst_aiff_parse_perform_seek (aiff, event);
      } else {
        GST_DEBUG_OBJECT (aiff, "queuing seek for later");

        event_p = &aiff->seek_event;
        gst_event_replace (event_p, event);

        /* we always return true */
        res = TRUE;
      }
      break;
    default:
      break;
  }
  gst_event_unref (event);
  return res;
}

#define MAX_BUFFER_SIZE 4096

static GstFlowReturn
gst_aiff_parse_stream_data (GstAiffParse * aiff)
{
  GstBuffer *buf = NULL;
  GstFlowReturn res = GST_FLOW_OK;
  guint64 desired, obtained;
  GstClockTime timestamp, next_timestamp, duration;
  guint64 pos, nextpos;

iterate_adapter:
  GST_LOG_OBJECT (aiff,
      "offset: %" G_GINT64_FORMAT " , end: %" G_GINT64_FORMAT " , dataleft: %"
      G_GINT64_FORMAT, aiff->offset, aiff->end_offset, aiff->dataleft);

  /* Get the next n bytes and output them */
  if (aiff->dataleft == 0 || aiff->dataleft < aiff->bytes_per_sample)
    goto found_eos;

  /* scale the amount of data by the segment rate so we get equal
   * amounts of data regardless of the playback rate */
  desired =
      MIN (gst_guint64_to_gdouble (aiff->dataleft),
      MAX_BUFFER_SIZE * aiff->segment.abs_rate);

  if (desired >= aiff->bytes_per_sample && aiff->bytes_per_sample > 0)
    desired -= (desired % aiff->bytes_per_sample);

  GST_LOG_OBJECT (aiff, "Fetching %" G_GINT64_FORMAT " bytes of data "
      "from the sinkpad", desired);

  if (aiff->streaming) {
    guint avail = gst_adapter_available (aiff->adapter);

#ifdef GSTREAMER_LITE
    guint extra;

    /* flush some bytes if evil upstream sends segment that starts
     * before data or does is not send sample aligned segment */
    if (G_LIKELY (aiff->offset >= aiff->datastart)) {
      extra = (aiff->offset - aiff->datastart) % aiff->bytes_per_sample;
    } else {
      extra = aiff->datastart - aiff->offset;
    }

    if (G_UNLIKELY (extra)) {
      extra = aiff->bytes_per_sample - extra;
      if (extra <= avail) {
        GST_DEBUG_OBJECT (aiff, "flushing %d bytes to sample boundary", extra);
        gst_adapter_flush (aiff->adapter, extra);
        aiff->offset += extra;
        aiff->dataleft -= extra;
        goto iterate_adapter;
      } else {
        GST_DEBUG_OBJECT (aiff, "flushing %d bytes", avail);
        gst_adapter_clear (aiff->adapter);
        aiff->offset += avail;
        aiff->dataleft -= avail;
        return GST_FLOW_OK;
      }
    }
#endif // GSTREAMER_LITE

    if (avail < desired) {
      GST_LOG_OBJECT (aiff, "Got only %d bytes of data from the sinkpad",
          avail);
      return GST_FLOW_OK;
    }

    buf = gst_adapter_take_buffer (aiff->adapter, desired);
  } else {
    if ((res = gst_pad_pull_range (aiff->sinkpad, aiff->offset,
                desired, &buf)) != GST_FLOW_OK)
      goto pull_error;
  }

  /* If we have a pending close/start segment, send it now. */
  if (G_UNLIKELY (aiff->close_segment != NULL)) {
    gst_pad_push_event (aiff->srcpad, aiff->close_segment);
    aiff->close_segment = NULL;
  }
  if (G_UNLIKELY (aiff->start_segment != NULL)) {
    gst_pad_push_event (aiff->srcpad, aiff->start_segment);
    aiff->start_segment = NULL;
  }

  obtained = GST_BUFFER_SIZE (buf);

  /* our positions in bytes */
  pos = aiff->offset - aiff->datastart;
  nextpos = pos + obtained;

  /* update offsets, does not overflow. */
  GST_BUFFER_OFFSET (buf) = pos / aiff->bytes_per_sample;
  GST_BUFFER_OFFSET_END (buf) = nextpos / aiff->bytes_per_sample;

  if (aiff->bps > 0) {
    /* and timestamps if we have a bitrate, be careful for overflows */
    timestamp =
        gst_util_uint64_scale_ceil (pos, GST_SECOND, (guint64) aiff->bps);
    next_timestamp =
        gst_util_uint64_scale_ceil (nextpos, GST_SECOND, (guint64) aiff->bps);
    duration = next_timestamp - timestamp;

    /* update current running segment position */
    gst_segment_set_last_stop (&aiff->segment, GST_FORMAT_TIME, next_timestamp);
  } else {
    /* no bitrate, all we know is that the first sample has timestamp 0, all
     * other positions and durations have unknown timestamp. */
    if (pos == 0)
      timestamp = 0;
    else
      timestamp = GST_CLOCK_TIME_NONE;
    duration = GST_CLOCK_TIME_NONE;
    /* update current running segment position with byte offset */
    gst_segment_set_last_stop (&aiff->segment, GST_FORMAT_BYTES, nextpos);
  }
  if (aiff->discont) {
    GST_DEBUG_OBJECT (aiff, "marking DISCONT");
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DISCONT);
    aiff->discont = FALSE;
  }

  GST_BUFFER_TIMESTAMP (buf) = timestamp;
  GST_BUFFER_DURATION (buf) = duration;
  gst_buffer_set_caps (buf, aiff->caps);

  GST_LOG_OBJECT (aiff,
      "Got buffer. timestamp:%" GST_TIME_FORMAT " , duration:%" GST_TIME_FORMAT
      ", size:%u", GST_TIME_ARGS (timestamp), GST_TIME_ARGS (duration),
      GST_BUFFER_SIZE (buf));

  if ((res = gst_pad_push (aiff->srcpad, buf)) != GST_FLOW_OK)
    goto push_error;

  if (obtained < aiff->dataleft) {
    aiff->offset += obtained;
    aiff->dataleft -= obtained;
  } else {
    aiff->offset += aiff->dataleft;
    aiff->dataleft = 0;
  }

  /* Iterate until need more data, so adapter size won't grow */
  if (aiff->streaming) {
    GST_LOG_OBJECT (aiff,
        "offset: %" G_GINT64_FORMAT " , end: %" G_GINT64_FORMAT, aiff->offset,
        aiff->end_offset);
    goto iterate_adapter;
  }
  return res;

  /* ERROR */
found_eos:
  {
    GST_DEBUG_OBJECT (aiff, "found EOS");
    return GST_FLOW_UNEXPECTED;
  }
pull_error:
  {
    /* check if we got EOS */
    if (res == GST_FLOW_UNEXPECTED)
      goto found_eos;

    GST_WARNING_OBJECT (aiff,
        "Error getting %" G_GINT64_FORMAT " bytes from the "
        "sinkpad (dataleft = %" G_GINT64_FORMAT ")", desired, aiff->dataleft);
    return res;
  }
push_error:
  {
    GST_INFO_OBJECT (aiff,
        "Error pushing on srcpad %s:%s, reason %s, is linked? = %d",
        GST_DEBUG_PAD_NAME (aiff->srcpad), gst_flow_get_name (res),
        gst_pad_is_linked (aiff->srcpad));
    return res;
  }
}

static void
gst_aiff_parse_loop (GstPad * pad)
{
  GstFlowReturn ret;
  GstAiffParse *aiff = GST_AIFF_PARSE (GST_PAD_PARENT (pad));

  GST_LOG_OBJECT (aiff, "process data");

  switch (aiff->state) {
    case AIFF_PARSE_START:
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_START");
      if ((ret = gst_aiff_parse_stream_init (aiff)) != GST_FLOW_OK)
        goto pause;

      aiff->state = AIFF_PARSE_HEADER;
      /* fall-through */

    case AIFF_PARSE_HEADER:
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_HEADER");
      if ((ret = gst_aiff_parse_stream_headers (aiff)) != GST_FLOW_OK)
        goto pause;

      aiff->state = AIFF_PARSE_DATA;
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_DATA");
      /* fall-through */

    case AIFF_PARSE_DATA:
      if ((ret = gst_aiff_parse_stream_data (aiff)) != GST_FLOW_OK)
        goto pause;
      break;
    default:
      g_assert_not_reached ();
  }
  return;

  /* ERRORS */
pause:
  {
    const gchar *reason = gst_flow_get_name (ret);

    GST_DEBUG_OBJECT (aiff, "pausing task, reason %s", reason);
    aiff->segment_running = FALSE;
    gst_pad_pause_task (pad);

    if (ret == GST_FLOW_UNEXPECTED) {
      /* perform EOS logic */
      if (aiff->segment.flags & GST_SEEK_FLAG_SEGMENT) {
        GstClockTime stop;

        if ((stop = aiff->segment.stop) == -1)
          stop = aiff->segment.duration;

        gst_element_post_message (GST_ELEMENT_CAST (aiff),
            gst_message_new_segment_done (GST_OBJECT_CAST (aiff),
                aiff->segment.format, stop));
      } else {
        gst_pad_push_event (aiff->srcpad, gst_event_new_eos ());
      }
    } else if (ret < GST_FLOW_UNEXPECTED || ret == GST_FLOW_NOT_LINKED) {
      /* for fatal errors we post an error message, post the error
       * first so the app knows about the error first. */
      GST_ELEMENT_ERROR (aiff, STREAM, FAILED,
          (_("Internal data flow error.")),
          ("streaming task paused, reason %s (%d)", reason, ret));
      gst_pad_push_event (aiff->srcpad, gst_event_new_eos ());
    }
    return;
  }
}

static GstFlowReturn
gst_aiff_parse_chain (GstPad * pad, GstBuffer * buf)
{
  GstFlowReturn ret;
  GstAiffParse *aiff = GST_AIFF_PARSE (GST_PAD_PARENT (pad));

  GST_LOG_OBJECT (aiff, "adapter_push %u bytes", GST_BUFFER_SIZE (buf));

  gst_adapter_push (aiff->adapter, buf);

  switch (aiff->state) {
    case AIFF_PARSE_START:
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_START");
      if ((ret = gst_aiff_parse_parse_stream_init (aiff)) != GST_FLOW_OK)
        goto done;

      if (aiff->state != AIFF_PARSE_HEADER)
        break;

      /* otherwise fall-through */
    case AIFF_PARSE_HEADER:
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_HEADER");
      if ((ret = gst_aiff_parse_stream_headers (aiff)) != GST_FLOW_OK)
        goto done;

      if (!aiff->got_comm || aiff->datastart == 0)
        break;

      aiff->state = AIFF_PARSE_DATA;
      GST_INFO_OBJECT (aiff, "AIFF_PARSE_DATA");

      /* fall-through */
    case AIFF_PARSE_DATA:
#ifdef GSTREAMER_LITE
      if (buf && GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DISCONT))
        aiff->discont = TRUE;
#endif // GSTREAMER_LITE
      if ((ret = gst_aiff_parse_stream_data (aiff)) != GST_FLOW_OK)
        goto done;
      break;
    default:
      g_return_val_if_reached (GST_FLOW_ERROR);
  }
done:
  return ret;
}

#ifdef GSTREAMER_LITE
static GstFlowReturn
gst_aiff_parse_flush_data (GstAiffParse * aiff)
{
  GstFlowReturn ret = GST_FLOW_OK;
  guint av;

  if ((av = gst_adapter_available (aiff->adapter)) > 0) {
    aiff->dataleft = av;
    aiff->end_offset = aiff->offset + av;
    ret = gst_aiff_parse_stream_data (aiff);
  }

  return ret;
}

static gboolean
gst_aiff_parse_sink_event (GstPad * pad, GstEvent * event)
{
  GstAiffParse *aiff = GST_AIFF_PARSE (GST_PAD_PARENT (pad));
  gboolean ret = TRUE;

  GST_LOG_OBJECT (aiff, "handling %s event", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_NEWSEGMENT:
    {			
      GstFormat format;
      gdouble rate, arate;
      gint64 start, stop, time, offset = 0, end_offset = -1;
      gboolean update;
      GstSegment segment;

      /* some debug output */
      gst_segment_init (&segment, GST_FORMAT_UNDEFINED);
      gst_event_parse_new_segment_full (event, &update, &rate, &arate, &format,
          &start, &stop, &time);
      gst_segment_set_newsegment_full (&segment, update, rate, arate, format,
          start, stop, time);
      GST_DEBUG_OBJECT (aiff,
          "received format %d newsegment %" GST_SEGMENT_FORMAT, format,
          &segment);

      if (aiff->state != AIFF_PARSE_DATA) {
        GST_DEBUG_OBJECT (aiff, "still starting, eating event");
        goto exit;
      }

      /* now we are either committed to TIME or BYTE format,
       * and we only expect a BYTE segment, e.g. following a seek */
      if (format == GST_FORMAT_BYTES) {
        if (start > 0) {
          offset = start;
          start -= aiff->datastart;
          start = MAX (start, 0);
        }
        if (stop > 0) {
          end_offset = stop;
          stop -= aiff->datastart;
          stop = MAX (stop, 0);
        }
        if (aiff->segment.format == GST_FORMAT_TIME) {
          guint64 bps = aiff->bps;

          /* operating in format TIME, so we can convert */
          if (bps) {
            if (start >= 0)
              start =
                  uint64_ceiling_scale (start, GST_SECOND, (guint64) aiff->bps);
            if (stop >= 0)
              stop =
                  uint64_ceiling_scale (stop, GST_SECOND, (guint64) aiff->bps);
          }
        }
      } else {
        GST_DEBUG_OBJECT (aiff, "unsupported segment format, ignoring");
        goto exit;
      }

      /* accept upstream's notion of segment and distribute along */
      gst_segment_set_newsegment_full (&aiff->segment, update, rate, arate,
          aiff->segment.format, start, stop, start);
      /* also store the newsegment event for the streaming thread */
      if (aiff->start_segment)
        gst_event_unref (aiff->start_segment);
      aiff->start_segment =
          gst_event_new_new_segment_full (update, rate, arate,
          aiff->segment.format, start, stop, start);
      GST_DEBUG_OBJECT (aiff, "Pushing newseg update %d, rate %g, "
          "applied rate %g, format %d, start %" G_GINT64_FORMAT ", "
          "stop %" G_GINT64_FORMAT, update, rate, arate, aiff->segment.format,
          start, stop);

      /* stream leftover data in current segment */
      gst_aiff_parse_flush_data (aiff);
      /* and set up streaming thread for next one */
      aiff->offset = offset;
      aiff->end_offset = end_offset;
      if (aiff->end_offset > 0) {
        aiff->dataleft = aiff->end_offset - aiff->offset;
      } else {
        /* infinity; upstream will EOS when done */
        aiff->dataleft = G_MAXUINT64;
      }
    exit:
      gst_event_unref (event);
      break;
    }
    case GST_EVENT_EOS:
      /* stream leftover data in current segment */
      gst_aiff_parse_flush_data (aiff);
      /* fall-through */
    case GST_EVENT_FLUSH_STOP:
      gst_adapter_clear (aiff->adapter);
      aiff->discont = TRUE;
      /* fall-through */
    default:
      ret = gst_pad_event_default (aiff->sinkpad, event);
      break;
  }

  return ret;
}
#endif // GSTREAMER_LITE

static gboolean
gst_aiff_parse_pad_convert (GstPad * pad,
    GstFormat src_format, gint64 src_value,
    GstFormat * dest_format, gint64 * dest_value)
{
  GstAiffParse *aiffparse;
  gboolean res = TRUE;

  aiffparse = GST_AIFF_PARSE (GST_PAD_PARENT (pad));

  if (*dest_format == src_format) {
    *dest_value = src_value;
    return TRUE;
  }

  if (aiffparse->bytes_per_sample <= 0)
    return FALSE;

  GST_INFO_OBJECT (aiffparse, "converting value from %s to %s",
      gst_format_get_name (src_format), gst_format_get_name (*dest_format));

  switch (src_format) {
    case GST_FORMAT_BYTES:
      switch (*dest_format) {
        case GST_FORMAT_DEFAULT:
          *dest_value = src_value / aiffparse->bytes_per_sample;
#ifdef GSTREAMER_LITE
          /* make sure we end up on a sample boundary */
          *dest_value -= *dest_value % aiffparse->bytes_per_sample;
#endif // GSTREAMER_LITE
          break;
        case GST_FORMAT_TIME:
          if (aiffparse->bps > 0) {
            *dest_value = gst_util_uint64_scale_ceil (src_value, GST_SECOND,
                (guint64) aiffparse->bps);
            break;
          }
          /* Else fallthrough */
        default:
          res = FALSE;
          goto done;
      }
      break;

    case GST_FORMAT_DEFAULT:
      switch (*dest_format) {
        case GST_FORMAT_BYTES:
          *dest_value = src_value * aiffparse->bytes_per_sample;
          break;
        case GST_FORMAT_TIME:
          *dest_value = gst_util_uint64_scale (src_value, GST_SECOND,
              (guint64) aiffparse->rate);
          break;
        default:
          res = FALSE;
          goto done;
      }
      break;

    case GST_FORMAT_TIME:
      switch (*dest_format) {
        case GST_FORMAT_BYTES:
          if (aiffparse->bps > 0) {
            *dest_value = gst_util_uint64_scale (src_value,
                (guint64) aiffparse->bps, GST_SECOND);
            break;
          }
          /* Else fallthrough */
          break;
        case GST_FORMAT_DEFAULT:
          *dest_value = gst_util_uint64_scale (src_value,
              (guint64) aiffparse->rate, GST_SECOND);
          break;
        default:
          res = FALSE;
          goto done;
      }
      break;

    default:
      res = FALSE;
      goto done;
  }

done:
  return res;

}

static const GstQueryType *
gst_aiff_parse_get_query_types (GstPad * pad)
{
  static const GstQueryType types[] = {
    GST_QUERY_DURATION,
    GST_QUERY_CONVERT,
    GST_QUERY_SEEKING,
    0
  };

  return types;
}

/* handle queries for location and length in requested format */
static gboolean
gst_aiff_parse_pad_query (GstPad * pad, GstQuery * query)
{
  gboolean res = TRUE;
  GstAiffParse *aiff = GST_AIFF_PARSE (gst_pad_get_parent (pad));

  /* only if we know */
  if (aiff->state != AIFF_PARSE_DATA) {
    gst_object_unref (aiff);
    return FALSE;
  }

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_DURATION:
    {
      gint64 duration = 0;
      GstFormat format;

      gst_query_parse_duration (query, &format, NULL);

      switch (format) {
        case GST_FORMAT_TIME:{
          if ((res = gst_aiff_parse_calculate_duration (aiff))) {
            duration = aiff->duration;
          }
          break;
        }
        default:
          format = GST_FORMAT_BYTES;
          duration = aiff->datasize;
          break;
      }
      gst_query_set_duration (query, format, duration);
      break;
    }
    case GST_QUERY_CONVERT:
    {
      gint64 srcvalue, dstvalue;
      GstFormat srcformat, dstformat;

      gst_query_parse_convert (query, &srcformat, &srcvalue,
          &dstformat, &dstvalue);
      res = gst_aiff_parse_pad_convert (pad, srcformat, srcvalue,
          &dstformat, &dstvalue);
      if (res)
        gst_query_set_convert (query, srcformat, srcvalue, dstformat, dstvalue);
      break;
    }
    case GST_QUERY_SEEKING:{
      GstFormat fmt;

      gst_query_parse_seeking (query, &fmt, NULL, NULL, NULL);
      if (fmt == GST_FORMAT_TIME) {
        gboolean seekable = TRUE;

        if (!gst_aiff_parse_calculate_duration (aiff)) {
          seekable = FALSE;
        }
        gst_query_set_seeking (query, GST_FORMAT_TIME, seekable,
            0, aiff->duration);
        res = TRUE;
      }
      break;
    }
    default:
      res = gst_pad_query_default (pad, query);
      break;
  }
  gst_object_unref (aiff);
  return res;
}

static gboolean
gst_aiff_parse_srcpad_event (GstPad * pad, GstEvent * event)
{
  GstAiffParse *aiffparse = GST_AIFF_PARSE (gst_pad_get_parent (pad));
  gboolean res = FALSE;

  GST_DEBUG_OBJECT (aiffparse, "%s event", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
      /* can only handle events when we are in the data state */
      if (aiffparse->state == AIFF_PARSE_DATA) {
        res = gst_aiff_parse_perform_seek (aiffparse, event);
      }
      gst_event_unref (event);
      break;
    default:
      res = gst_pad_push_event (aiffparse->sinkpad, event);
      break;
  }
  gst_object_unref (aiffparse);
  return res;
}

static gboolean
gst_aiff_parse_sink_activate (GstPad * sinkpad)
{
  GstAiffParse *aiff = GST_AIFF_PARSE (gst_pad_get_parent (sinkpad));
  gboolean res;

  if (aiff->adapter)
    g_object_unref (aiff->adapter);

  if (gst_pad_check_pull_range (sinkpad)) {
    GST_DEBUG ("going to pull mode");
    aiff->streaming = FALSE;
#ifdef GSTREAMER_LITE
    if (aiff->adapter) {
      gst_adapter_clear (aiff->adapter);
      g_object_unref (aiff->adapter);
    }
#endif // GSTREAMER_LITE
    aiff->adapter = NULL;
    res = gst_pad_activate_pull (sinkpad, TRUE);
  } else {
    GST_DEBUG ("going to push (streaming) mode");
    aiff->streaming = TRUE;
#ifdef GSTREAMER_LITE
    if (aiff->adapter == NULL)
#endif // GSTREAMER_LITE
    aiff->adapter = gst_adapter_new ();
    res = gst_pad_activate_push (sinkpad, TRUE);
  }
  gst_object_unref (aiff);
  return res;
}


static gboolean
gst_aiff_parse_sink_activate_pull (GstPad * sinkpad, gboolean active)
{
  GstAiffParse *aiff = GST_AIFF_PARSE (GST_OBJECT_PARENT (sinkpad));

  if (active) {
    aiff->segment_running = TRUE;
    return gst_pad_start_task (sinkpad, (GstTaskFunction) gst_aiff_parse_loop,
        sinkpad);
  } else {
    aiff->segment_running = FALSE;
    return gst_pad_stop_task (sinkpad);
  }
};

static GstStateChangeReturn
gst_aiff_parse_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn ret;
  GstAiffParse *aiff = GST_AIFF_PARSE (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      gst_aiff_parse_reset (aiff);
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      gst_aiff_parse_reset (aiff);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      break;
    default:
      break;
  }
  return ret;
}
