/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
 * Copyright (C) <2006> Wim Taymans <wim@fluendo.com>
 * Copyright (C) <2007> Julien Moutte <julien@fluendo.com>
 * Copyright (C) <2009> Tim-Philipp Müller <tim centricular net>
 * Copyright (C) <2009> STEricsson <benjamin.gaignard@stericsson.com>
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
 * SECTION:element-qtdemux
 *
 * Demuxes a .mov file into raw or compressed audio and/or video streams.
 *
 * This element supports both push and pull-based scheduling, depending on the
 * capabilities of the upstream elements.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch filesrc location=test.mov ! qtdemux name=demux  demux.audio_00 ! decodebin ! audioconvert ! audioresample ! autoaudiosink   demux.video_00 ! queue ! decodebin ! ffmpegcolorspace ! videoscale ! autovideosink
 * ]| Play (parse and decode) a .mov file and try to output it to
 * an automatically detected soundcard and videosink. If the MOV file contains
 * compressed audio or video data, this will only work if you have the
 * right decoder elements/plugins installed.
 * </refsect2>
 *
 * Last reviewed on 2006-12-29 (0.10.5)
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gst/gst-i18n-plugin.h"

#include <glib/gprintf.h>
#include <gst/tag/tag.h>

#include "qtatomparser.h"
#include "qtdemux_types.h"
#include "qtdemux_dump.h"
#include "qtdemux_fourcc.h"
#include "qtdemux_lang.h"
#include "qtdemux.h"
#include "qtpalette.h"

#include "gst/riff/riff-media.h"
#include "gst/riff/riff-read.h"

#include <gst/pbutils/pbutils.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef HAVE_ZLIB
# include <zlib.h>
#endif

/* max. size considered 'sane' for non-mdat atoms */
#define QTDEMUX_MAX_ATOM_SIZE (25*1024*1024)

/* if the sample index is larger than this, something is likely wrong */
#ifdef GSTREAMER_LITE
// relaxing the limitation since some long files has more than 50Mb sample index
#define QTDEMUX_MAX_SAMPLE_INDEX_SIZE (500*1024*1024)
#else
#define QTDEMUX_MAX_SAMPLE_INDEX_SIZE (50*1024*1024)
#endif // GSTREAMER_LITE 

/* For converting qt creation times to unix epoch times */
#define QTDEMUX_SECONDS_PER_DAY (60 * 60 * 24)
#define QTDEMUX_LEAP_YEARS_FROM_1904_TO_1970 17
#define QTDEMUX_SECONDS_FROM_1904_TO_1970 (((1970 - 1904) * (guint64) 365 + \
    QTDEMUX_LEAP_YEARS_FROM_1904_TO_1970) * QTDEMUX_SECONDS_PER_DAY)

GST_DEBUG_CATEGORY (qtdemux_debug);

/*typedef struct _QtNode QtNode; */
typedef struct _QtDemuxSegment QtDemuxSegment;
typedef struct _QtDemuxSample QtDemuxSample;

/*struct _QtNode
{
  guint32 type;
  guint8 *data;
  gint len;
};*/

struct _QtDemuxSample
{
  guint32 size;
  gint32 pts_offset;            /* Add this value to timestamp to get the pts */
  guint64 offset;
  guint64 timestamp;            /* DTS In mov time */
  guint32 duration;             /* In mov time */
  gboolean keyframe;            /* TRUE when this packet is a keyframe */
};

/* timestamp is the DTS */
#define QTSAMPLE_DTS(stream,sample) gst_util_uint64_scale ((sample)->timestamp,\
    GST_SECOND, (stream)->timescale)
/* timestamp + offset is the PTS */
#define QTSAMPLE_PTS(stream,sample) gst_util_uint64_scale ((sample)->timestamp + \
    (sample)->pts_offset, GST_SECOND, (stream)->timescale)
/* timestamp + duration - dts is the duration */
#define QTSAMPLE_DUR_DTS(stream,sample,dts) (gst_util_uint64_scale ((sample)->timestamp + \
    (sample)->duration, GST_SECOND, (stream)->timescale) - (dts));
/* timestamp + offset + duration - pts is the duration */
#define QTSAMPLE_DUR_PTS(stream,sample,pts) (gst_util_uint64_scale ((sample)->timestamp + \
    (sample)->pts_offset + (sample)->duration, GST_SECOND, (stream)->timescale) - (pts));

#define QTSAMPLE_KEYFRAME(stream,sample) ((stream)->all_keyframe || (sample)->keyframe)

/*
 * Quicktime has tracks and segments. A track is a continuous piece of
 * multimedia content. The track is not always played from start to finish but
 * instead, pieces of the track are 'cut out' and played in sequence. This is
 * what the segments do.
 *
 * Inside the track we have keyframes (K) and delta frames. The track has its
 * own timing, which starts from 0 and extends to end. The position in the track
 * is called the media_time.
 *
 * The segments now describe the pieces that should be played from this track
 * and are basically tupples of media_time/duration/rate entries. We can have
 * multiple segments and they are all played after one another. An example:
 *
 * segment 1: media_time: 1 second, duration: 1 second, rate 1
 * segment 2: media_time: 3 second, duration: 2 second, rate 2
 *
 * To correctly play back this track, one must play: 1 second of media starting
 * from media_time 1 followed by 2 seconds of media starting from media_time 3
 * at a rate of 2.
 *
 * Each of the segments will be played at a specific time, the first segment at
 * time 0, the second one after the duration of the first one, etc.. Note that
 * the time in resulting playback is not identical to the media_time of the
 * track anymore.
 *
 * Visually, assuming the track has 4 second of media_time:
 *
 *                (a)                   (b)          (c)              (d)
 *         .-----------------------------------------------------------.
 * track:  | K.....K.........K........K.......K.......K...........K... |
 *         '-----------------------------------------------------------'
 *         0              1              2              3              4
 *           .------------^              ^   .----------^              ^
 *          /              .-------------'  /       .------------------'
 *         /              /          .-----'       /
 *         .--------------.         .--------------.
 *         | segment 1    |         | segment 2    |
 *         '--------------'         '--------------'
 *
 * The challenge here is to cut out the right pieces of the track for each of
 * the playback segments. This fortunatly can easily be done with the SEGMENT
 * events of gstreamer.
 *
 * For playback of segment 1, we need to provide the decoder with the keyframe
 * (a), in the above figure, but we must instruct it only to output the decoded
 * data between second 1 and 2. We do this with a SEGMENT event for 1 to 2, time
 * position set to the time of the segment: 0.
 *
 * We then proceed to push data from keyframe (a) to frame (b). The decoder
 * decodes but clips all before media_time 1.
 *
 * After finishing a segment, we push out a new SEGMENT event with the clipping
 * boundaries of the new data.
 *
 * This is a good usecase for the GStreamer accumulated SEGMENT events.
 */

struct _QtDemuxSegment
{
  /* global time and duration, all gst time */
  guint64 time;
  guint64 stop_time;
  guint64 duration;
  /* media time of trak, all gst time */
  guint64 media_start;
  guint64 media_stop;
  gdouble rate;
};

struct _QtDemuxStream
{
  GstPad *pad;

  /* stream type */
  guint32 subtype;
  GstCaps *caps;
  guint32 fourcc;

  /* if the stream has a redirect URI in its headers, we store it here */
  gchar *redirect_uri;

  /* track id */
  guint track_id;
#ifdef GSTREAMER_LITE
  gboolean track_enabled;
#endif // GSTREAMER_LITE
  
  /* duration/scale */
  guint64 duration;             /* in timescale */
  guint32 timescale;

  /* language */
  gchar lang_id[4];             /* ISO 639-2T language code */

  /* our samples */
  guint32 n_samples;
  QtDemuxSample *samples;
  gboolean all_keyframe;        /* TRUE when all samples are keyframes (no stss) */
  guint32 min_duration;         /* duration in timescale of first sample, used for figuring out
                                   the framerate, in timescale units */

  /* if we use chunks or samples */
  gboolean sampled;
  guint padding;

  /* video info */
  gint width;
  gint height;
  /* aspect ratio */
  gint display_width;
  gint display_height;
  gint par_w;
  gint par_h;
  /* Numerator/denominator framerate */
  gint fps_n;
  gint fps_d;
  guint16 bits_per_sample;
  guint16 color_table_id;

  /* audio info */
  gdouble rate;
  gint n_channels;
  guint samples_per_packet;
  guint samples_per_frame;
  guint bytes_per_packet;
  guint bytes_per_sample;
  guint bytes_per_frame;
  guint compression;

  /* when a discontinuity is pending */
  gboolean discont;

  /* list of buffers to push first */
  GSList *buffers;

  /* if we need to clip this buffer. This is only needed for uncompressed
   * data */
  gboolean need_clip;

  /* buffer needs some custom processing, e.g. subtitles */
  gboolean need_process;

  /* current position */
  guint32 segment_index;
  guint32 sample_index;
  guint64 time_position;        /* in gst time */

  /* the Gst segment we are processing out, used for clipping */
  GstSegment segment;

  /* last GstFlowReturn */
  GstFlowReturn last_ret;

  /* quicktime segments */
  guint32 n_segments;
  QtDemuxSegment *segments;
  guint32 from_sample;
  guint32 to_sample;

  gboolean sent_eos;
  GstTagList *pending_tags;
  gboolean send_global_tags;

  GstEvent *pending_event;

  GstByteReader stco;
  GstByteReader stsz;
  GstByteReader stsc;
  GstByteReader stts;
  GstByteReader stss;
  GstByteReader stps;
  GstByteReader ctts;

  gboolean chunks_are_chunks;
  gint64 stbl_index;
  /* stco */
  guint co_size;
  GstByteReader co_chunk;
  guint32 first_chunk;
  guint32 current_chunk;
  guint32 last_chunk;
  guint32 samples_per_chunk;
  guint32 stco_sample_index;
  /* stsz */
  guint32 sample_size;          /* 0 means variable sizes are stored in stsz */
  /* stsc */
  guint32 stsc_index;
  guint32 n_samples_per_chunk;
  guint32 stsc_chunk_index;
  guint32 stsc_sample_index;
  guint64 chunk_offset;
  /* stts */
  guint32 stts_index;
  guint32 stts_samples;
  guint32 n_sample_times;
  guint32 stts_sample_index;
  guint32 stts_time;
  guint32 stts_duration;
  /* stss */
  gboolean stss_present;
  guint32 n_sample_syncs;
  guint32 stss_index;
  /* stps */
  gboolean stps_present;
  guint32 n_sample_partial_syncs;
  guint32 stps_index;
  /* ctts */
  gboolean ctts_present;
  guint32 n_composition_times;
  guint32 ctts_index;
  guint32 ctts_sample_index;
  guint32 ctts_count;
  gint32 ctts_soffset;

  /* fragmented */
  gboolean parsed_trex;
  guint32 def_sample_duration;
  guint32 def_sample_size;
  guint32 def_sample_flags;
};

enum QtDemuxState
{
  QTDEMUX_STATE_INITIAL,        /* Initial state (haven't got the header yet) */
  QTDEMUX_STATE_HEADER,         /* Parsing the header */
  QTDEMUX_STATE_MOVIE,          /* Parsing/Playing the media data */
  QTDEMUX_STATE_BUFFER_MDAT     /* Buffering the mdat atom */
};

static GNode *qtdemux_tree_get_child_by_type (GNode * node, guint32 fourcc);
static GNode *qtdemux_tree_get_child_by_type_full (GNode * node,
    guint32 fourcc, GstByteReader * parser);
static GNode *qtdemux_tree_get_sibling_by_type (GNode * node, guint32 fourcc);
static GNode *qtdemux_tree_get_sibling_by_type_full (GNode * node,
    guint32 fourcc, GstByteReader * parser);

static GstStaticPadTemplate gst_qtdemux_sink_template =
    GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("video/quicktime; video/mj2; audio/x-m4a; "
        "application/x-3gp")
    );

static GstStaticPadTemplate gst_qtdemux_videosrc_template =
GST_STATIC_PAD_TEMPLATE ("video_%02d",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate gst_qtdemux_audiosrc_template =
GST_STATIC_PAD_TEMPLATE ("audio_%02d",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate gst_qtdemux_subsrc_template =
GST_STATIC_PAD_TEMPLATE ("subtitle_%02d",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

GST_BOILERPLATE (GstQTDemux, gst_qtdemux, GstQTDemux, GST_TYPE_ELEMENT);

static void gst_qtdemux_dispose (GObject * object);

static guint32
gst_qtdemux_find_index_linear (GstQTDemux * qtdemux, QtDemuxStream * str,
    guint64 media_time);
static guint32
gst_qtdemux_find_index_for_given_media_offset_linear (GstQTDemux * qtdemux,
    QtDemuxStream * str, gint64 media_offset);

static void gst_qtdemux_set_index (GstElement * element, GstIndex * index);
static GstIndex *gst_qtdemux_get_index (GstElement * element);
static GstStateChangeReturn gst_qtdemux_change_state (GstElement * element,
    GstStateChange transition);
static gboolean qtdemux_sink_activate (GstPad * sinkpad);
static gboolean qtdemux_sink_activate_pull (GstPad * sinkpad, gboolean active);
static gboolean qtdemux_sink_activate_push (GstPad * sinkpad, gboolean active);

static void gst_qtdemux_loop (GstPad * pad);
static GstFlowReturn gst_qtdemux_chain (GstPad * sinkpad, GstBuffer * inbuf);
static gboolean gst_qtdemux_handle_sink_event (GstPad * pad, GstEvent * event);

static gboolean qtdemux_parse_moov (GstQTDemux * qtdemux,
    const guint8 * buffer, guint length);
static gboolean qtdemux_parse_node (GstQTDemux * qtdemux, GNode * node,
    const guint8 * buffer, guint length);
static gboolean qtdemux_parse_tree (GstQTDemux * qtdemux);

#ifdef GSTREAMER_LITE
static gboolean gst_qtdemux_handle_esds (GstQTDemux * qtdemux,
    QtDemuxStream * stream, GNode * esds, GstTagList * list);
#else
static void gst_qtdemux_handle_esds (GstQTDemux * qtdemux,
    QtDemuxStream * stream, GNode * esds, GstTagList * list);
#endif // GSTREAMER_LITE
static GstCaps *qtdemux_video_caps (GstQTDemux * qtdemux,
    QtDemuxStream * stream, guint32 fourcc, const guint8 * stsd_data,
    gchar ** codec_name);
static GstCaps *qtdemux_audio_caps (GstQTDemux * qtdemux,
    QtDemuxStream * stream, guint32 fourcc, const guint8 * data, int len,
    gchar ** codec_name);
static GstCaps *qtdemux_sub_caps (GstQTDemux * qtdemux,
    QtDemuxStream * stream, guint32 fourcc, const guint8 * data,
    gchar ** codec_name);
static gboolean qtdemux_parse_samples (GstQTDemux * qtdemux,
    QtDemuxStream * stream, guint32 n);
static GstFlowReturn qtdemux_expose_streams (GstQTDemux * qtdemux);


static void
gst_qtdemux_base_init (gpointer klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&gst_qtdemux_sink_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&gst_qtdemux_videosrc_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&gst_qtdemux_audiosrc_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&gst_qtdemux_subsrc_template));
  gst_element_class_set_details_simple (element_class, "QuickTime demuxer",
      "Codec/Demuxer",
      "Demultiplex a QuickTime file into audio and video streams",
      "David Schleef <ds@schleef.org>, Wim Taymans <wim@fluendo.com>");

  GST_DEBUG_CATEGORY_INIT (qtdemux_debug, "qtdemux", 0, "qtdemux plugin");
}

static void
gst_qtdemux_class_init (GstQTDemuxClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->dispose = gst_qtdemux_dispose;

  gstelement_class->change_state = GST_DEBUG_FUNCPTR (gst_qtdemux_change_state);

  gstelement_class->set_index = GST_DEBUG_FUNCPTR (gst_qtdemux_set_index);
  gstelement_class->get_index = GST_DEBUG_FUNCPTR (gst_qtdemux_get_index);

  gst_tag_register_musicbrainz_tags ();
}

static void
gst_qtdemux_init (GstQTDemux * qtdemux, GstQTDemuxClass * klass)
{
  qtdemux->sinkpad =
      gst_pad_new_from_static_template (&gst_qtdemux_sink_template, "sink");
  gst_pad_set_activate_function (qtdemux->sinkpad, qtdemux_sink_activate);
  gst_pad_set_activatepull_function (qtdemux->sinkpad,
      qtdemux_sink_activate_pull);
  gst_pad_set_activatepush_function (qtdemux->sinkpad,
      qtdemux_sink_activate_push);
  gst_pad_set_chain_function (qtdemux->sinkpad, gst_qtdemux_chain);
  gst_pad_set_event_function (qtdemux->sinkpad, gst_qtdemux_handle_sink_event);
  gst_element_add_pad (GST_ELEMENT_CAST (qtdemux), qtdemux->sinkpad);

  qtdemux->state = QTDEMUX_STATE_INITIAL;
  qtdemux->pullbased = FALSE;
  qtdemux->posted_redirect = FALSE;
  qtdemux->neededbytes = 16;
  qtdemux->todrop = 0;
  qtdemux->adapter = gst_adapter_new ();
  qtdemux->offset = 0;
  qtdemux->first_mdat = -1;
  qtdemux->got_moov = FALSE;
  qtdemux->mdatoffset = GST_CLOCK_TIME_NONE;
  qtdemux->mdatbuffer = NULL;
  gst_segment_init (&qtdemux->segment, GST_FORMAT_TIME);
}

static void
gst_qtdemux_dispose (GObject * object)
{
  GstQTDemux *qtdemux = GST_QTDEMUX (object);

  if (qtdemux->adapter) {
    g_object_unref (G_OBJECT (qtdemux->adapter));
    qtdemux->adapter = NULL;
  }

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_qtdemux_post_no_playable_stream_error (GstQTDemux * qtdemux)
{
  if (qtdemux->posted_redirect) {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file contains no playable streams.")),
        ("no known streams found, a redirect message has been posted"));
  } else {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file contains no playable streams.")),
        ("no known streams found"));
  }
}

static GstFlowReturn
gst_qtdemux_pull_atom (GstQTDemux * qtdemux, guint64 offset, guint64 size,
    GstBuffer ** buf)
{
  GstFlowReturn flow;

  if (G_UNLIKELY (size == 0)) {
    GstFlowReturn ret;
    GstBuffer *tmp = NULL;

    ret = gst_qtdemux_pull_atom (qtdemux, offset, sizeof (guint32), &tmp);
    if (ret != GST_FLOW_OK)
      return ret;

    size = QT_UINT32 (GST_BUFFER_DATA (tmp));
    GST_DEBUG_OBJECT (qtdemux, "size 0x%08" G_GINT64_MODIFIER "x", size);

    gst_buffer_unref (tmp);
  }

  /* Sanity check: catch bogus sizes (fuzzed/broken files) */
  if (G_UNLIKELY (size > QTDEMUX_MAX_ATOM_SIZE)) {
    if (qtdemux->state != QTDEMUX_STATE_MOVIE && qtdemux->got_moov) {
      /* we're pulling header but already got most interesting bits,
       * so never mind the rest (e.g. tags) (that much) */
      GST_WARNING_OBJECT (qtdemux, "atom has bogus size %" G_GUINT64_FORMAT,
          size);
      return GST_FLOW_UNEXPECTED;
    } else {
      GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
          (_("This file is invalid and cannot be played.")),
          ("atom has bogus size %" G_GUINT64_FORMAT, size));
      return GST_FLOW_ERROR;
    }
  }

  flow = gst_pad_pull_range (qtdemux->sinkpad, offset, size, buf);

  if (G_UNLIKELY (flow != GST_FLOW_OK))
    return flow;

  /* Catch short reads - we don't want any partial atoms */
  if (G_UNLIKELY (GST_BUFFER_SIZE (*buf) < size)) {
    GST_WARNING_OBJECT (qtdemux, "short read: %u < %" G_GUINT64_FORMAT,
        GST_BUFFER_SIZE (*buf), size);
    gst_buffer_unref (*buf);
    *buf = NULL;
    return GST_FLOW_UNEXPECTED;
  }

  return flow;
}

#if 1
static gboolean
gst_qtdemux_src_convert (GstPad * pad, GstFormat src_format, gint64 src_value,
    GstFormat dest_format, gint64 * dest_value)
{
  gboolean res = TRUE;
  QtDemuxStream *stream = gst_pad_get_element_private (pad);
  GstQTDemux *qtdemux = GST_QTDEMUX (gst_pad_get_parent (pad));
  gint32 index;

  if (stream->subtype != FOURCC_vide) {
    res = FALSE;
    goto done;
  }

  switch (src_format) {
    case GST_FORMAT_TIME:
      switch (dest_format) {
        case GST_FORMAT_BYTES:{
          index = gst_qtdemux_find_index_linear (qtdemux, stream, src_value);
          if (-1 == index)
            return FALSE;

          *dest_value = stream->samples[index].offset;

          GST_DEBUG_OBJECT (qtdemux, "Format Conversion Time->Offset :%"
              GST_TIME_FORMAT "->%" G_GUINT64_FORMAT,
              GST_TIME_ARGS (src_value), *dest_value);
          break;
        }
        default:
          res = FALSE;
          break;
      }
      break;
    case GST_FORMAT_BYTES:
      switch (dest_format) {
        case GST_FORMAT_TIME:{
          index =
              gst_qtdemux_find_index_for_given_media_offset_linear (qtdemux,
              stream, src_value);

          if (-1 == index)
            return FALSE;

          *dest_value =
              gst_util_uint64_scale (stream->samples[index].timestamp,
              GST_SECOND, stream->timescale);
          GST_DEBUG_OBJECT (qtdemux, "Format Conversion Offset->Time :%"
              G_GUINT64_FORMAT "->%" GST_TIME_FORMAT,
              src_value, GST_TIME_ARGS (*dest_value));
          break;
        }
        default:
          res = FALSE;
          break;
      }
      break;
    default:
      res = FALSE;
  }

done:
  gst_object_unref (qtdemux);

  return res;
}
#endif

static const GstQueryType *
gst_qtdemux_get_src_query_types (GstPad * pad)
{
  static const GstQueryType src_types[] = {
    GST_QUERY_POSITION,
    GST_QUERY_DURATION,
    GST_QUERY_CONVERT,
    GST_QUERY_FORMATS,
    GST_QUERY_SEEKING,
    0
  };

  return src_types;
}

static gboolean
gst_qtdemux_get_duration (GstQTDemux * qtdemux, gint64 * duration)
{
  gboolean res = TRUE;

  *duration = GST_CLOCK_TIME_NONE;

  if (qtdemux->duration != 0) {
    if (qtdemux->duration != G_MAXINT64 && qtdemux->timescale != 0) {
      *duration = gst_util_uint64_scale (qtdemux->duration,
          GST_SECOND, qtdemux->timescale);
    }
  }
  return res;
}

static gboolean
gst_qtdemux_handle_src_query (GstPad * pad, GstQuery * query)
{
  gboolean res = FALSE;
  GstQTDemux *qtdemux = GST_QTDEMUX (gst_pad_get_parent (pad));

  GST_LOG_OBJECT (pad, "%s query", GST_QUERY_TYPE_NAME (query));

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_POSITION:
      if (GST_CLOCK_TIME_IS_VALID (qtdemux->segment.last_stop)) {
        gst_query_set_position (query, GST_FORMAT_TIME,
            qtdemux->segment.last_stop);
        res = TRUE;
      }
      break;
    case GST_QUERY_DURATION:{
      GstFormat fmt;

      gst_query_parse_duration (query, &fmt, NULL);
      if (fmt == GST_FORMAT_TIME) {
        gint64 duration = -1;

        gst_qtdemux_get_duration (qtdemux, &duration);
        if (duration > 0) {
          gst_query_set_duration (query, GST_FORMAT_TIME, duration);
          res = TRUE;
        }
      }
      break;
    }
    case GST_QUERY_CONVERT:{
      GstFormat src_fmt, dest_fmt;
      gint64 src_value, dest_value = 0;

      gst_query_parse_convert (query, &src_fmt, &src_value, &dest_fmt, NULL);

      res = gst_qtdemux_src_convert (pad,
          src_fmt, src_value, dest_fmt, &dest_value);
      if (res) {
        gst_query_set_convert (query, src_fmt, src_value, dest_fmt, dest_value);
        res = TRUE;
      }
      break;
    }
    case GST_QUERY_FORMATS:
      gst_query_set_formats (query, 2, GST_FORMAT_TIME, GST_FORMAT_BYTES);
      res = TRUE;
      break;
    case GST_QUERY_SEEKING:{
      GstFormat fmt;
      gboolean seekable;

      gst_query_parse_seeking (query, &fmt, NULL, NULL, NULL);
      if (fmt == GST_FORMAT_TIME) {
        gint64 duration = -1;

        gst_qtdemux_get_duration (qtdemux, &duration);
        seekable = TRUE;
        if (!qtdemux->pullbased) {
          GstQuery *q;

          /* we might be able with help from upstream */
          seekable = FALSE;
          q = gst_query_new_seeking (GST_FORMAT_BYTES);
          if (gst_pad_peer_query (qtdemux->sinkpad, q)) {
            gst_query_parse_seeking (q, &fmt, &seekable, NULL, NULL);
            GST_LOG_OBJECT (qtdemux, "upstream BYTE seekable %d", seekable);
          }
          gst_query_unref (q);
        }
        gst_query_set_seeking (query, GST_FORMAT_TIME, seekable, 0, duration);
        res = TRUE;
      }
      break;
    }
    default:
      res = gst_pad_query_default (pad, query);
      break;
  }

#ifdef GSTREAMER_LITE
  if (!res)
      res = gst_pad_query_default (pad, query);
#endif

  gst_object_unref (qtdemux);

  return res;
}

static void
gst_qtdemux_push_tags (GstQTDemux * qtdemux, QtDemuxStream * stream)
{
  if (G_LIKELY (stream->pad)) {
    GST_DEBUG_OBJECT (qtdemux, "Checking pad %s:%s for tags",
        GST_DEBUG_PAD_NAME (stream->pad));

    if (G_UNLIKELY (stream->pending_tags)) {
      GST_DEBUG_OBJECT (qtdemux, "Sending tags %" GST_PTR_FORMAT,
          stream->pending_tags);
      gst_pad_push_event (stream->pad,
          gst_event_new_tag (stream->pending_tags));
      stream->pending_tags = NULL;
    }

    if (G_UNLIKELY (stream->send_global_tags && qtdemux->tag_list)) {
      GST_DEBUG_OBJECT (qtdemux, "Sending global tags %" GST_PTR_FORMAT,
          qtdemux->tag_list);
      gst_pad_push_event (stream->pad,
          gst_event_new_tag (gst_tag_list_copy (qtdemux->tag_list)));
      stream->send_global_tags = FALSE;
    }
  }
}

/* push event on all source pads; takes ownership of the event */
static void
gst_qtdemux_push_event (GstQTDemux * qtdemux, GstEvent * event)
{
  guint n;
  gboolean has_valid_stream = FALSE;
  GstEventType etype = GST_EVENT_TYPE (event);

  GST_DEBUG_OBJECT (qtdemux, "pushing %s event on all source pads",
      GST_EVENT_TYPE_NAME (event));

  for (n = 0; n < qtdemux->n_streams; n++) {
    GstPad *pad;
    QtDemuxStream *stream = qtdemux->streams[n];

    if ((pad = stream->pad)) {
      has_valid_stream = TRUE;

      if (etype == GST_EVENT_EOS) {
        /* let's not send twice */
        if (stream->sent_eos)
          continue;
        stream->sent_eos = TRUE;
      }

      gst_pad_push_event (pad, gst_event_ref (event));
    }
  }

  gst_event_unref (event);

  /* if it is EOS and there are no pads, post an error */
  if (!has_valid_stream && etype == GST_EVENT_EOS) {
    gst_qtdemux_post_no_playable_stream_error (qtdemux);
  }
}

/* push a pending newsegment event, if any from the streaming thread */
static void
gst_qtdemux_push_pending_newsegment (GstQTDemux * qtdemux)
{
  if (qtdemux->pending_newsegment) {
    gst_qtdemux_push_event (qtdemux, qtdemux->pending_newsegment);
    qtdemux->pending_newsegment = NULL;
  }
}

typedef struct
{
  guint64 media_time;
} FindData;

static gint
find_func (QtDemuxSample * s1, guint64 * media_time, gpointer user_data)
{
  if (s1->timestamp > *media_time)
    return 1;

  return -1;
}

/* find the index of the sample that includes the data for @media_time using a
 * binary search.  Only to be called in optimized cases of linear search below.
 *
 * Returns the index of the sample.
 */
static guint32
gst_qtdemux_find_index (GstQTDemux * qtdemux, QtDemuxStream * str,
    guint64 media_time)
{
  QtDemuxSample *result;
  guint32 index;

  /* convert media_time to mov format */
  media_time =
      gst_util_uint64_scale_ceil (media_time, str->timescale, GST_SECOND);

  result = gst_util_array_binary_search (str->samples, str->stbl_index + 1,
      sizeof (QtDemuxSample), (GCompareDataFunc) find_func,
      GST_SEARCH_MODE_BEFORE, &media_time, NULL);

  if (G_LIKELY (result))
    index = result - str->samples;
  else
    index = 0;

  return index;
}



/* find the index of the sample that includes the data for @media_offset using a
 * linear search
 *
 * Returns the index of the sample.
 */
static guint32
gst_qtdemux_find_index_for_given_media_offset_linear (GstQTDemux * qtdemux,
    QtDemuxStream * str, gint64 media_offset)
{
  QtDemuxSample *result = str->samples;
  guint32 index = 0;

  if (result == NULL || str->n_samples == 0)
    return -1;

  if (media_offset == result->offset)
    return index;

  result++;
  while (index < str->n_samples - 1) {
    if (!qtdemux_parse_samples (qtdemux, str, index + 1))
      goto parse_failed;

    if (media_offset < result->offset)
      break;

    index++;
    result++;
  }
  return index;

  /* ERRORS */
parse_failed:
  {
    GST_LOG_OBJECT (qtdemux, "Parsing of index %u failed!", index + 1);
    return -1;
  }
}

/* find the index of the sample that includes the data for @media_time using a
 * linear search, and keeping in mind that not all samples may have been parsed
 * yet.  If possible, it will delegate to binary search.
 *
 * Returns the index of the sample.
 */
static guint32
gst_qtdemux_find_index_linear (GstQTDemux * qtdemux, QtDemuxStream * str,
    guint64 media_time)
{
  guint32 index = 0;
  guint64 mov_time;

  /* convert media_time to mov format */
  mov_time =
      gst_util_uint64_scale_ceil (media_time, str->timescale, GST_SECOND);

  if (mov_time == str->samples[0].timestamp)
    return index;

  /* use faster search if requested time in already parsed range */
  if (str->stbl_index >= 0 &&
      mov_time <= str->samples[str->stbl_index].timestamp)
    return gst_qtdemux_find_index (qtdemux, str, media_time);

  while (index < str->n_samples - 1) {
    if (!qtdemux_parse_samples (qtdemux, str, index + 1))
      goto parse_failed;

    if (mov_time < str->samples[index + 1].timestamp)
      break;

    index++;
  }
  return index;

  /* ERRORS */
parse_failed:
  {
    GST_LOG_OBJECT (qtdemux, "Parsing of index %u failed!", index + 1);
    return -1;
  }
}

/* find the index of the keyframe needed to decode the sample at @index
 * of stream @str.
 *
 * Returns the index of the keyframe.
 */
static guint32
gst_qtdemux_find_keyframe (GstQTDemux * qtdemux, QtDemuxStream * str,
    guint32 index)
{
  guint32 new_index = index;

  if (index >= str->n_samples) {
    new_index = str->n_samples;
    goto beach;
  }

  /* all keyframes, return index */
  if (str->all_keyframe) {
    new_index = index;
    goto beach;
  }

  /* else go back until we have a keyframe */
  while (TRUE) {
    if (str->samples[new_index].keyframe)
      break;

    if (new_index == 0)
      break;

    new_index--;
  }

beach:
  GST_DEBUG_OBJECT (qtdemux, "searching for keyframe index before index %u "
      "gave %u", index, new_index);

  return new_index;
}

/* find the segment for @time_position for @stream
 *
 * Returns -1 if the segment cannot be found.
 */
static guint32
gst_qtdemux_find_segment (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint64 time_position)
{
  gint i;
  guint32 seg_idx;

  GST_LOG_OBJECT (qtdemux, "finding segment for %" GST_TIME_FORMAT,
      GST_TIME_ARGS (time_position));

  /* find segment corresponding to time_position if we are looking
   * for a segment. */
  seg_idx = -1;
  for (i = 0; i < stream->n_segments; i++) {
    QtDemuxSegment *segment = &stream->segments[i];

    GST_LOG_OBJECT (qtdemux,
        "looking at segment %" GST_TIME_FORMAT "-%" GST_TIME_FORMAT,
        GST_TIME_ARGS (segment->time), GST_TIME_ARGS (segment->stop_time));

    /* For the last segment we include stop_time in the last segment */
    if (i < stream->n_segments - 1) {
      if (segment->time <= time_position && time_position < segment->stop_time) {
        GST_LOG_OBJECT (qtdemux, "segment %d matches", i);
        seg_idx = i;
        break;
      }
    } else {
      if (segment->time <= time_position && time_position <= segment->stop_time) {
        GST_LOG_OBJECT (qtdemux, "segment %d matches", i);
        seg_idx = i;
        break;
      }
    }
  }
  return seg_idx;
}

/* move the stream @str to the sample position @index.
 *
 * Updates @str->sample_index and marks discontinuity if needed.
 */
static void
gst_qtdemux_move_stream (GstQTDemux * qtdemux, QtDemuxStream * str,
    guint32 index)
{
  /* no change needed */
  if (index == str->sample_index)
    return;

  GST_DEBUG_OBJECT (qtdemux, "moving to sample %u of %u", index,
      str->n_samples);

  /* position changed, we have a discont */
  str->sample_index = index;
  /* Each time we move in the stream we store the position where we are
   * starting from */
  str->from_sample = index;
  str->discont = TRUE;
}

static void
gst_qtdemux_adjust_seek (GstQTDemux * qtdemux, gint64 desired_time,
    gint64 * key_time, gint64 * key_offset)
{
  guint64 min_offset;
  gint64 min_byte_offset = -1;
  gint n;

  min_offset = desired_time;

  /* for each stream, find the index of the sample in the segment
   * and move back to the previous keyframe. */
  for (n = 0; n < qtdemux->n_streams; n++) {
    QtDemuxStream *str;
    guint32 index, kindex;
    guint32 seg_idx;
    guint64 media_start;
    guint64 media_time;
    guint64 seg_time;
    QtDemuxSegment *seg;

    str = qtdemux->streams[n];

    seg_idx = gst_qtdemux_find_segment (qtdemux, str, desired_time);
    GST_DEBUG_OBJECT (qtdemux, "align segment %d", seg_idx);

    /* segment not found, continue with normal flow */
    if (seg_idx == -1)
      continue;

    /* get segment and time in the segment */
    seg = &str->segments[seg_idx];
    seg_time = desired_time - seg->time;

    /* get the media time in the segment */
    media_start = seg->media_start + seg_time;

    /* get the index of the sample with media time */
    index = gst_qtdemux_find_index_linear (qtdemux, str, media_start);
    GST_DEBUG_OBJECT (qtdemux, "sample for %" GST_TIME_FORMAT " at %u",
        GST_TIME_ARGS (media_start), index);

    /* find previous keyframe */
    kindex = gst_qtdemux_find_keyframe (qtdemux, str, index);

    /* if the keyframe is at a different position, we need to update the
     * requested seek time */
    if (index != kindex) {
      index = kindex;

      /* get timestamp of keyframe */
      media_time =
          gst_util_uint64_scale (str->samples[kindex].timestamp, GST_SECOND,
          str->timescale);
      GST_DEBUG_OBJECT (qtdemux, "keyframe at %u with time %" GST_TIME_FORMAT,
          kindex, GST_TIME_ARGS (media_time));

      /* keyframes in the segment get a chance to change the
       * desired_offset. keyframes out of the segment are
       * ignored. */
      if (media_time >= seg->media_start) {
        guint64 seg_time;

        /* this keyframe is inside the segment, convert back to
         * segment time */
        seg_time = (media_time - seg->media_start) + seg->time;
        if (seg_time < min_offset)
          min_offset = seg_time;
      }
    }

    if (min_byte_offset < 0 || str->samples[index].offset < min_byte_offset)
      min_byte_offset = str->samples[index].offset;
  }

  if (key_time)
    *key_time = min_offset;
  if (key_offset)
    *key_offset = min_byte_offset;
}

static gboolean
gst_qtdemux_convert_seek (GstPad * pad, GstFormat * format,
    GstSeekType cur_type, gint64 * cur, GstSeekType stop_type, gint64 * stop)
{
  gboolean res;
  GstFormat fmt;

  g_return_val_if_fail (format != NULL, FALSE);
  g_return_val_if_fail (cur != NULL, FALSE);
  g_return_val_if_fail (stop != NULL, FALSE);

  if (*format == GST_FORMAT_TIME)
    return TRUE;

  fmt = GST_FORMAT_TIME;
  res = TRUE;
  if (cur_type != GST_SEEK_TYPE_NONE)
    res = gst_pad_query_convert (pad, *format, *cur, &fmt, cur);
  if (res && stop_type != GST_SEEK_TYPE_NONE)
    res = gst_pad_query_convert (pad, *format, *stop, &fmt, stop);

  if (res)
    *format = GST_FORMAT_TIME;

  return res;
}

/* perform seek in push based mode:
   find BYTE position to move to based on time and delegate to upstream
*/
static gboolean
gst_qtdemux_do_push_seek (GstQTDemux * qtdemux, GstPad * pad, GstEvent * event)
{
  gdouble rate;
  GstFormat format;
  GstSeekFlags flags;
  GstSeekType cur_type, stop_type;
  gint64 cur, stop;
  gboolean res;
  gint64 byte_cur;

  GST_DEBUG_OBJECT (qtdemux, "doing push-based seek");

  gst_event_parse_seek (event, &rate, &format, &flags,
      &cur_type, &cur, &stop_type, &stop);

  /* FIXME, always play to the end */
  stop = -1;

  /* only forward streaming and seeking is possible */
  if (rate <= 0)
    goto unsupported_seek;

  /* convert to TIME if needed and possible */
  if (!gst_qtdemux_convert_seek (pad, &format, cur_type, &cur,
          stop_type, &stop))
    goto no_format;

  /* find reasonable corresponding BYTE position,
   * also try to mind about keyframes, since we can not go back a bit for them
   * later on */
  gst_qtdemux_adjust_seek (qtdemux, cur, NULL, &byte_cur);

  if (byte_cur == -1)
    goto abort_seek;

  GST_DEBUG_OBJECT (qtdemux, "Pushing BYTE seek rate %g, "
      "start %" G_GINT64_FORMAT ", stop %" G_GINT64_FORMAT, rate, byte_cur,
      stop);

  if (!(flags & GST_SEEK_FLAG_KEY_UNIT)) {
    GST_DEBUG_OBJECT (qtdemux,
        "Requested seek time: %" GST_TIME_FORMAT ", calculated seek offset: %"
        G_GUINT64_FORMAT, GST_TIME_ARGS (cur), byte_cur);
    GST_OBJECT_LOCK (qtdemux);
    qtdemux->requested_seek_time = cur;
    qtdemux->seek_offset = byte_cur;
    GST_OBJECT_UNLOCK (qtdemux);
  }

  /* BYTE seek event */
  event = gst_event_new_seek (rate, GST_FORMAT_BYTES, flags, cur_type, byte_cur,
      stop_type, stop);
  res = gst_pad_push_event (qtdemux->sinkpad, event);

  return res;

  /* ERRORS */
abort_seek:
  {
    GST_DEBUG_OBJECT (qtdemux, "could not determine byte position to seek to, "
        "seek aborted.");
    return FALSE;
  }
unsupported_seek:
  {
    GST_DEBUG_OBJECT (qtdemux, "unsupported seek, seek aborted.");
    return FALSE;
  }
no_format:
  {
    GST_DEBUG_OBJECT (qtdemux, "unsupported format given, seek aborted.");
    return FALSE;
  }
}

/* perform the seek.
 *
 * We set all segment_indexes in the streams to unknown and
 * adjust the time_position to the desired position. this is enough
 * to trigger a segment switch in the streaming thread to start
 * streaming from the desired position.
 *
 * Keyframe seeking is a little more complicated when dealing with
 * segments. Ideally we want to move to the previous keyframe in
 * the segment but there might not be a keyframe in the segment. In
 * fact, none of the segments could contain a keyframe. We take a
 * practical approach: seek to the previous keyframe in the segment,
 * if there is none, seek to the beginning of the segment.
 *
 * Called with STREAM_LOCK
 */
static gboolean
gst_qtdemux_perform_seek (GstQTDemux * qtdemux, GstSegment * segment)
{
  gint64 desired_offset;
  gint n;

  desired_offset = segment->last_stop;

  GST_DEBUG_OBJECT (qtdemux, "seeking to %" GST_TIME_FORMAT,
      GST_TIME_ARGS (desired_offset));

  if (segment->flags & GST_SEEK_FLAG_KEY_UNIT) {
    gint64 min_offset;

    gst_qtdemux_adjust_seek (qtdemux, desired_offset, &min_offset, NULL);
    GST_DEBUG_OBJECT (qtdemux, "keyframe seek, align to %"
        GST_TIME_FORMAT, GST_TIME_ARGS (min_offset));
    desired_offset = min_offset;
  }

  /* and set all streams to the final position */
  for (n = 0; n < qtdemux->n_streams; n++) {
    QtDemuxStream *stream = qtdemux->streams[n];

    stream->time_position = desired_offset;
    stream->sample_index = -1;
    stream->segment_index = -1;
    stream->last_ret = GST_FLOW_OK;
    stream->sent_eos = FALSE;
  }
  segment->last_stop = desired_offset;
  segment->time = desired_offset;

  /* we stop at the end */
  if (segment->stop == -1)
    segment->stop = segment->duration;

  return TRUE;
}

/* do a seek in pull based mode */
static gboolean
gst_qtdemux_do_seek (GstQTDemux * qtdemux, GstPad * pad, GstEvent * event)
{
  gdouble rate;
  GstFormat format;
  GstSeekFlags flags;
  GstSeekType cur_type, stop_type;
  gint64 cur, stop;
  gboolean flush;
  gboolean update;
  GstSegment seeksegment;
  int i;

  if (event) {
    GST_DEBUG_OBJECT (qtdemux, "doing seek with event");

    gst_event_parse_seek (event, &rate, &format, &flags,
        &cur_type, &cur, &stop_type, &stop);

    /* we have to have a format as the segment format. Try to convert
     * if not. */
    if (!gst_qtdemux_convert_seek (pad, &format, cur_type, &cur,
            stop_type, &stop))
      goto no_format;

    GST_DEBUG_OBJECT (qtdemux, "seek format %s", gst_format_get_name (format));
  } else {
    GST_DEBUG_OBJECT (qtdemux, "doing seek without event");
    flags = 0;
  }

  flush = flags & GST_SEEK_FLAG_FLUSH;

  /* stop streaming, either by flushing or by pausing the task */
  if (flush) {
    /* unlock upstream pull_range */
    gst_pad_push_event (qtdemux->sinkpad, gst_event_new_flush_start ());
    /* make sure out loop function exits */
    gst_qtdemux_push_event (qtdemux, gst_event_new_flush_start ());
  } else {
    /* non flushing seek, pause the task */
    gst_pad_pause_task (qtdemux->sinkpad);
  }

  /* wait for streaming to finish */
  GST_PAD_STREAM_LOCK (qtdemux->sinkpad);

  /* copy segment, we need this because we still need the old
   * segment when we close the current segment. */
  memcpy (&seeksegment, &qtdemux->segment, sizeof (GstSegment));

  if (event) {
    /* configure the segment with the seek variables */
    GST_DEBUG_OBJECT (qtdemux, "configuring seek");
    gst_segment_set_seek (&seeksegment, rate, format, flags,
        cur_type, cur, stop_type, stop, &update);
  }

  /* now do the seek, this actually never returns FALSE */
  gst_qtdemux_perform_seek (qtdemux, &seeksegment);

  /* prepare for streaming again */
  if (flush) {
    gst_pad_push_event (qtdemux->sinkpad, gst_event_new_flush_stop ());
    gst_qtdemux_push_event (qtdemux, gst_event_new_flush_stop ());
  } else if (qtdemux->segment_running) {
    /* we are running the current segment and doing a non-flushing seek,
     * close the segment first based on the last_stop. */
    GST_DEBUG_OBJECT (qtdemux, "closing running segment %" G_GINT64_FORMAT
        " to %" G_GINT64_FORMAT, qtdemux->segment.start,
        qtdemux->segment.last_stop);

    if (qtdemux->segment.rate >= 0) {
      /* FIXME, rate is the product of the global rate and the (quicktime)
       * segment rate. */
      qtdemux->pending_newsegment = gst_event_new_new_segment (TRUE,
          qtdemux->segment.rate, qtdemux->segment.format,
          qtdemux->segment.start, qtdemux->segment.last_stop,
          qtdemux->segment.time);
    } else {                    /* For Reverse Playback */
      guint64 stop;

      if ((stop = qtdemux->segment.stop) == -1)
        stop = qtdemux->segment.duration;
      /* for reverse playback, we played from stop to last_stop. */
      qtdemux->pending_newsegment = gst_event_new_new_segment (TRUE,
          qtdemux->segment.rate, qtdemux->segment.format,
          qtdemux->segment.last_stop, stop, qtdemux->segment.last_stop);
    }
  }

  /* commit the new segment */
  memcpy (&qtdemux->segment, &seeksegment, sizeof (GstSegment));

  if (qtdemux->segment.flags & GST_SEEK_FLAG_SEGMENT) {
    gst_element_post_message (GST_ELEMENT_CAST (qtdemux),
        gst_message_new_segment_start (GST_OBJECT_CAST (qtdemux),
            qtdemux->segment.format, qtdemux->segment.last_stop));
  }

  /* restart streaming, NEWSEGMENT will be sent from the streaming
   * thread. */
  qtdemux->segment_running = TRUE;
  for (i = 0; i < qtdemux->n_streams; i++)
    qtdemux->streams[i]->last_ret = GST_FLOW_OK;

  gst_pad_start_task (qtdemux->sinkpad, (GstTaskFunction) gst_qtdemux_loop,
      qtdemux->sinkpad);

  GST_PAD_STREAM_UNLOCK (qtdemux->sinkpad);

  return TRUE;

  /* ERRORS */
no_format:
  {
    GST_DEBUG_OBJECT (qtdemux, "unsupported format given, seek aborted.");
    return FALSE;
  }
}

static gboolean
qtdemux_ensure_index (GstQTDemux * qtdemux)
{
  guint i;

  GST_DEBUG_OBJECT (qtdemux, "collecting all metadata for all streams");

  /* Build complete index */
  for (i = 0; i < qtdemux->n_streams; i++) {
    QtDemuxStream *stream = qtdemux->streams[i];

    if (!qtdemux_parse_samples (qtdemux, stream, stream->n_samples - 1))
      goto parse_error;
  }
  return TRUE;

  /* ERRORS */
parse_error:
  {
    GST_LOG_OBJECT (qtdemux,
        "Building complete index of stream %u for seeking failed!", i);
    return FALSE;
  }
}

static gboolean
gst_qtdemux_handle_src_event (GstPad * pad, GstEvent * event)
{
  gboolean res = TRUE;
  GstQTDemux *qtdemux = GST_QTDEMUX (gst_pad_get_parent (pad));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
    {
#ifndef GST_DISABLE_GST_DEBUG
      GstClockTime ts = gst_util_get_timestamp ();
#endif
      /* Build complete index for seeking;
       * if not a fragmented file at least */
      if (!qtdemux->fragmented)
        if (!qtdemux_ensure_index (qtdemux))
          goto index_failed;
#ifndef GST_DISABLE_GST_DEBUG
      ts = gst_util_get_timestamp () - ts;
      GST_INFO_OBJECT (qtdemux,
          "Time taken to parse index %" GST_TIME_FORMAT, GST_TIME_ARGS (ts));
#endif
    }
      if (qtdemux->pullbased) {
        res = gst_qtdemux_do_seek (qtdemux, pad, event);
      } else if (qtdemux->state == QTDEMUX_STATE_MOVIE && qtdemux->n_streams &&
          !qtdemux->fragmented) {
        res = gst_qtdemux_do_push_seek (qtdemux, pad, event);
      } else {
        GST_DEBUG_OBJECT (qtdemux,
            "ignoring seek in push mode in current state");
        res = FALSE;
      }
      gst_event_unref (event);
      break;
    case GST_EVENT_QOS:
    case GST_EVENT_NAVIGATION:
      res = FALSE;
      gst_event_unref (event);
      break;
    default:
      res = gst_pad_event_default (pad, event);
      break;
  }

  gst_object_unref (qtdemux);

done:
  return res;

  /* ERRORS */
index_failed:
  {
    GST_ERROR_OBJECT (qtdemux, "Index failed");
    gst_event_unref (event);
    res = FALSE;
    goto done;
  }
}

/* stream/index return sample that is min/max w.r.t. byte position,
 * time is min/max w.r.t. time of samples,
 * the latter need not be time of the former sample */
static void
gst_qtdemux_find_sample (GstQTDemux * qtdemux, gint64 byte_pos, gboolean fw,
    gboolean set, QtDemuxStream ** _stream, gint * _index, gint64 * _time)
{
  gint i, n, index;
  gint64 time, min_time;
  QtDemuxStream *stream;

  min_time = -1;
  stream = NULL;
  index = -1;

  for (n = 0; n < qtdemux->n_streams; ++n) {
    QtDemuxStream *str;
    gint inc;
    gboolean set_sample;

    str = qtdemux->streams[n];
    set_sample = !set;

    if (fw) {
      i = 0;
      inc = 1;
    } else {
      i = str->n_samples - 1;
      inc = -1;
    }
    for (; (i >= 0) && (i < str->n_samples); i += inc) {
      if (str->samples[i].size &&
          ((fw && (str->samples[i].offset >= byte_pos)) ||
              (!fw &&
                  (str->samples[i].offset + str->samples[i].size <=
                      byte_pos)))) {
        /* move stream to first available sample */
        if (set) {
          gst_qtdemux_move_stream (qtdemux, str, i);
          set_sample = TRUE;
        }
        /* determine min/max time */
        time = str->samples[i].timestamp + str->samples[i].pts_offset;
        time = gst_util_uint64_scale (time, GST_SECOND, str->timescale);
        if (min_time == -1 || (!fw && time > min_time) ||
            (fw && time < min_time)) {
          min_time = time;
        }
        /* determine stream with leading sample, to get its position */
        if (!stream || (fw
                && (str->samples[i].offset < stream->samples[index].offset))
            || (!fw
                && (str->samples[i].offset > stream->samples[index].offset))) {
          stream = str;
          index = i;
        }
        break;
      }
    }
    /* no sample for this stream, mark eos */
    if (!set_sample)
      gst_qtdemux_move_stream (qtdemux, str, str->n_samples);
  }

  if (_time)
    *_time = min_time;
  if (_stream)
    *_stream = stream;
  if (_index)
    *_index = index;
}

static gboolean
gst_qtdemux_handle_sink_event (GstPad * sinkpad, GstEvent * event)
{
  GstQTDemux *demux = GST_QTDEMUX (GST_PAD_PARENT (sinkpad));
  gboolean res;

  GST_LOG_OBJECT (demux, "handling %s event", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_NEWSEGMENT:
    {
      GstFormat format;
      gdouble rate, arate;
      gint64 start, stop, time, offset = 0;
      QtDemuxStream *stream;
      gint idx;
      gboolean update;
      GstSegment segment;

      /* some debug output */
      gst_segment_init (&segment, GST_FORMAT_UNDEFINED);
      gst_event_parse_new_segment_full (event, &update, &rate, &arate, &format,
          &start, &stop, &time);
      gst_segment_set_newsegment_full (&segment, update, rate, arate, format,
          start, stop, time);
      GST_DEBUG_OBJECT (demux,
          "received format %d newsegment %" GST_SEGMENT_FORMAT, format,
          &segment);

      /* chain will send initial newsegment after pads have been added */
      if (demux->state != QTDEMUX_STATE_MOVIE || !demux->n_streams) {
        GST_DEBUG_OBJECT (demux, "still starting, eating event");
        goto exit;
      }

      /* we only expect a BYTE segment, e.g. following a seek */
      if (format == GST_FORMAT_BYTES) {
        if (start > 0) {
          gint64 requested_seek_time;
          guint64 seek_offset;

          offset = start;

          GST_OBJECT_LOCK (demux);
          requested_seek_time = demux->requested_seek_time;
          seek_offset = demux->seek_offset;
          demux->requested_seek_time = -1;
          demux->seek_offset = -1;
          GST_OBJECT_UNLOCK (demux);

          if (offset == seek_offset) {
            start = requested_seek_time;
          } else {
            gst_qtdemux_find_sample (demux, start, TRUE, FALSE, NULL, NULL,
                &start);
            start = MAX (start, 0);
          }
        }
        if (stop > 0) {
          gst_qtdemux_find_sample (demux, stop, FALSE, FALSE, NULL, NULL,
              &stop);
          /* keyframe seeking should already arrange for start >= stop,
           * but make sure in other rare cases */
          stop = MAX (stop, start);
        }
      } else {
        GST_DEBUG_OBJECT (demux, "unsupported segment format, ignoring");
        goto exit;
      }

      /* accept upstream's notion of segment and distribute along */
      gst_segment_set_newsegment_full (&demux->segment, update, rate, arate,
          GST_FORMAT_TIME, start, stop, start);
      GST_DEBUG_OBJECT (demux, "Pushing newseg update %d, rate %g, "
          "applied rate %g, format %d, start %" GST_TIME_FORMAT ", "
          "stop %" GST_TIME_FORMAT, update, rate, arate, GST_FORMAT_TIME,
          GST_TIME_ARGS (start), GST_TIME_ARGS (stop));

      gst_qtdemux_push_event (demux,
          gst_event_new_new_segment_full (update, rate, arate, GST_FORMAT_TIME,
              start, stop, start));

      /* clear leftover in current segment, if any */
      gst_adapter_clear (demux->adapter);
      /* set up streaming thread */
      gst_qtdemux_find_sample (demux, offset, TRUE, TRUE, &stream, &idx, NULL);
      demux->offset = offset;
      if (stream) {
        demux->todrop = stream->samples[idx].offset - offset;
        demux->neededbytes = demux->todrop + stream->samples[idx].size;
      } else {
        /* set up for EOS */
        demux->neededbytes = -1;
        demux->todrop = 0;
      }
    exit:
      gst_event_unref (event);
      res = TRUE;
      goto drop;
      break;
    }
    case GST_EVENT_FLUSH_STOP:
    {
      gint i;

      /* clean up, force EOS if no more info follows */
      gst_adapter_clear (demux->adapter);
      demux->offset = 0;
      demux->neededbytes = -1;
      /* reset flow return, e.g. following seek */
      for (i = 0; i < demux->n_streams; i++) {
        demux->streams[i]->last_ret = GST_FLOW_OK;
        demux->streams[i]->sent_eos = FALSE;
      }
      break;
    }
    case GST_EVENT_EOS:
      /* If we are in push mode, and get an EOS before we've seen any streams,
       * then error out - we have nowhere to send the EOS */
      if (!demux->pullbased) {
        gint i;
        gboolean has_valid_stream = FALSE;
        for (i = 0; i < demux->n_streams; i++) {
          if (demux->streams[i]->pad != NULL) {
            has_valid_stream = TRUE;
            break;
          }
        }
        if (!has_valid_stream)
          gst_qtdemux_post_no_playable_stream_error (demux);
      }
      break;
    default:
      break;
  }

  res = gst_pad_event_default (demux->sinkpad, event);

drop:
  return res;
}

static void
gst_qtdemux_set_index (GstElement * element, GstIndex * index)
{
  GstQTDemux *demux = GST_QTDEMUX (element);

  GST_OBJECT_LOCK (demux);
  if (demux->element_index)
    gst_object_unref (demux->element_index);
  if (index) {
    demux->element_index = gst_object_ref (index);
  } else {
    demux->element_index = NULL;
  }
  GST_OBJECT_UNLOCK (demux);
  /* object lock might be taken again */
  if (index)
    gst_index_get_writer_id (index, GST_OBJECT (element), &demux->index_id);
  GST_DEBUG_OBJECT (demux, "Set index %" GST_PTR_FORMAT "for writer id %d",
      demux->element_index, demux->index_id);
}

static GstIndex *
gst_qtdemux_get_index (GstElement * element)
{
  GstIndex *result = NULL;
  GstQTDemux *demux = GST_QTDEMUX (element);

  GST_OBJECT_LOCK (demux);
  if (demux->element_index)
    result = gst_object_ref (demux->element_index);
  GST_OBJECT_UNLOCK (demux);

  GST_DEBUG_OBJECT (demux, "Returning index %" GST_PTR_FORMAT, result);

  return result;
}

static void
gst_qtdemux_stbl_free (QtDemuxStream * stream)
{
  g_free ((gpointer) stream->stco.data);
  stream->stco.data = NULL;
  g_free ((gpointer) stream->stsz.data);
  stream->stsz.data = NULL;
  g_free ((gpointer) stream->stsc.data);
  stream->stsc.data = NULL;
  g_free ((gpointer) stream->stts.data);
  stream->stts.data = NULL;
  g_free ((gpointer) stream->stss.data);
  stream->stss.data = NULL;
  g_free ((gpointer) stream->stps.data);
  stream->stps.data = NULL;
  g_free ((gpointer) stream->ctts.data);
  stream->ctts.data = NULL;
}

static void
gst_qtdemux_stream_free (GstQTDemux * qtdemux, QtDemuxStream * stream)
{
  while (stream->buffers) {
    gst_buffer_unref (GST_BUFFER_CAST (stream->buffers->data));
    stream->buffers = g_slist_delete_link (stream->buffers, stream->buffers);
  }
  if (stream->pad)
    gst_element_remove_pad (GST_ELEMENT_CAST (qtdemux), stream->pad);
  g_free (stream->samples);
  if (stream->caps)
    gst_caps_unref (stream->caps);
  g_free (stream->segments);
  if (stream->pending_tags)
    gst_tag_list_free (stream->pending_tags);
  g_free (stream->redirect_uri);
  /* free stbl sub-atoms */
  gst_qtdemux_stbl_free (stream);
  g_free (stream);
}

static GstStateChangeReturn
gst_qtdemux_change_state (GstElement * element, GstStateChange transition)
{
  GstQTDemux *qtdemux = GST_QTDEMUX (element);
  GstStateChangeReturn result = GST_STATE_CHANGE_FAILURE;

  switch (transition) {
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      break;
    default:
      break;
  }

  result = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PAUSED_TO_READY:{
      gint n;

      qtdemux->state = QTDEMUX_STATE_INITIAL;
      qtdemux->neededbytes = 16;
      qtdemux->todrop = 0;
      qtdemux->pullbased = FALSE;
      qtdemux->posted_redirect = FALSE;
      qtdemux->offset = 0;
      qtdemux->first_mdat = -1;
      qtdemux->got_moov = FALSE;
      qtdemux->mdatoffset = GST_CLOCK_TIME_NONE;
      if (qtdemux->mdatbuffer)
        gst_buffer_unref (qtdemux->mdatbuffer);
      qtdemux->mdatbuffer = NULL;
      if (qtdemux->comp_brands)
        gst_buffer_unref (qtdemux->comp_brands);
      qtdemux->comp_brands = NULL;
      if (qtdemux->tag_list)
        gst_tag_list_free (qtdemux->tag_list);
      qtdemux->tag_list = NULL;
      if (qtdemux->element_index)
        gst_object_unref (qtdemux->element_index);
      qtdemux->element_index = NULL;
      gst_adapter_clear (qtdemux->adapter);
      for (n = 0; n < qtdemux->n_streams; n++) {
        gst_qtdemux_stream_free (qtdemux, qtdemux->streams[n]);
        qtdemux->streams[n] = NULL;
      }
      qtdemux->major_brand = 0;
      qtdemux->n_streams = 0;
      qtdemux->n_video_streams = 0;
      qtdemux->n_audio_streams = 0;
      qtdemux->n_sub_streams = 0;
      gst_segment_init (&qtdemux->segment, GST_FORMAT_TIME);
      qtdemux->requested_seek_time = GST_CLOCK_TIME_NONE;
      qtdemux->seek_offset = 0;
      break;
    }
    default:
      break;
  }

  return result;
}

static void
qtdemux_post_global_tags (GstQTDemux * qtdemux)
{
  if (qtdemux->tag_list) {
    /* all header tags ready and parsed, push them */
    GST_INFO_OBJECT (qtdemux, "posting global tags: %" GST_PTR_FORMAT,
        qtdemux->tag_list);
    /* post now, send event on pads later */
    gst_element_post_message (GST_ELEMENT (qtdemux),
        gst_message_new_tag (GST_OBJECT (qtdemux),
            gst_tag_list_copy (qtdemux->tag_list)));
  }
}

static void
qtdemux_parse_ftyp (GstQTDemux * qtdemux, const guint8 * buffer, gint length)
{
  /* only consider at least a sufficiently complete ftyp atom */
  if (length >= 20) {
    GstBuffer *buf;

    qtdemux->major_brand = QT_FOURCC (buffer + 8);
    GST_DEBUG_OBJECT (qtdemux, "major brand: %" GST_FOURCC_FORMAT,
        GST_FOURCC_ARGS (qtdemux->major_brand));
    buf = qtdemux->comp_brands = gst_buffer_new_and_alloc (length - 16);
    memcpy (GST_BUFFER_DATA (buf), buffer + 16, GST_BUFFER_SIZE (buf));
  }
}

static void
qtdemux_handle_xmp_taglist (GstQTDemux * qtdemux, GstTagList * taglist)
{
  /* Strip out bogus fields */
  if (taglist) {
    gst_tag_list_remove_tag (taglist, GST_TAG_VIDEO_CODEC);

    GST_DEBUG_OBJECT (qtdemux, "Found XMP tags %" GST_PTR_FORMAT, taglist);

    if (qtdemux->tag_list) {
      /* prioritize native tags using _KEEP mode */
      gst_tag_list_insert (qtdemux->tag_list, taglist, GST_TAG_MERGE_KEEP);
      gst_tag_list_free (taglist);
    } else
      qtdemux->tag_list = taglist;
  }
}

static void
qtdemux_parse_uuid (GstQTDemux * qtdemux, const guint8 * buffer, gint length)
{
  static guint8 xmp_uuid[] = { 0xBE, 0x7A, 0xCF, 0xCB,
    0x97, 0xA9, 0x42, 0xE8,
    0x9C, 0x71, 0x99, 0x94,
    0x91, 0xE3, 0xAF, 0xAC
  };
  guint offset;

  offset = (QT_UINT32 (buffer) == 0) ? 16 : 8;

  if (length <= offset + 16) {
    GST_DEBUG_OBJECT (qtdemux, "uuid atom is too short, skipping");
    return;
  }

#ifndef GSTREAMER_LITE
  if (memcmp (buffer + offset, xmp_uuid, 16) == 0) {
    GstBuffer *buf;
    GstTagList *taglist;

    buf = gst_buffer_new ();
    GST_BUFFER_DATA (buf) = (guint8 *) buffer + offset + 16;
    GST_BUFFER_SIZE (buf) = length - offset - 16;

    taglist = gst_tag_list_from_xmp_buffer (buf);
    gst_buffer_unref (buf);

    qtdemux_handle_xmp_taglist (qtdemux, taglist);

  } else {
    GST_DEBUG_OBJECT (qtdemux, "Ignoring unknown uuid");
  }
#endif // GSTREAMER_LITE
}

/* caller verifies at least 8 bytes in buf */
static void
extract_initial_length_and_fourcc (const guint8 * data, guint size,
    guint64 * plength, guint32 * pfourcc)
{
  guint64 length;
  guint32 fourcc;

  length = QT_UINT32 (data);
  GST_DEBUG ("length 0x%08" G_GINT64_MODIFIER "x", length);
  fourcc = QT_FOURCC (data + 4);
  GST_DEBUG ("atom type %" GST_FOURCC_FORMAT, GST_FOURCC_ARGS (fourcc));

  if (length == 0) {
    length = G_MAXUINT32;
  } else if (length == 1 && size >= 16) {
    /* this means we have an extended size, which is the 64 bit value of
     * the next 8 bytes */
    length = QT_UINT64 (data + 8);
    GST_DEBUG ("length 0x%08" G_GINT64_MODIFIER "x", length);
  }

  if (plength)
    *plength = length;
  if (pfourcc)
    *pfourcc = fourcc;
}

static gboolean
qtdemux_parse_mehd (GstQTDemux * qtdemux, GstByteReader * br)
{
  guint32 version = 0;
  guint64 duration = 0;

  if (!gst_byte_reader_get_uint32_be (br, &version))
    goto failed;

  version >>= 24;
  if (version == 1) {
    if (!gst_byte_reader_get_uint64_be (br, &duration))
      goto failed;
  } else {
    guint32 dur = 0;

    if (!gst_byte_reader_get_uint32_be (br, &dur))
      goto failed;
    duration = dur;
  }

  GST_INFO_OBJECT (qtdemux, "mehd duration: %" G_GUINT64_FORMAT, duration);
  qtdemux->duration = duration;

  return TRUE;

failed:
  {
    GST_DEBUG_OBJECT (qtdemux, "parsing mehd failed");
    return FALSE;
  }
}

static gboolean
qtdemux_parse_trex (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint32 * ds_duration, guint32 * ds_size, guint32 * ds_flags)
{
  if (!stream->parsed_trex && qtdemux->moov_node) {
    GNode *mvex, *trex;
    GstByteReader trex_data;

    mvex = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_mvex);
    if (mvex) {
      trex = qtdemux_tree_get_child_by_type_full (mvex, FOURCC_trex,
          &trex_data);
      while (trex) {
        guint32 id = 0, dur = 0, size = 0, flags = 0;

        /* skip version/flags */
        if (!gst_byte_reader_skip (&trex_data, 4))
          goto next;
        if (!gst_byte_reader_get_uint32_be (&trex_data, &id))
          goto next;
        if (id != stream->track_id)
          goto next;
        /* sample description index; ignore */
        if (!gst_byte_reader_get_uint32_be (&trex_data, &dur))
          goto next;
        if (!gst_byte_reader_get_uint32_be (&trex_data, &dur))
          goto next;
        if (!gst_byte_reader_get_uint32_be (&trex_data, &size))
          goto next;
        if (!gst_byte_reader_get_uint32_be (&trex_data, &flags))
          goto next;

        GST_DEBUG_OBJECT (qtdemux, "fragment defaults for stream %d; "
            "duration %d,  size %d, flags 0x%x", stream->track_id,
            dur, size, flags);

        stream->parsed_trex = TRUE;
        stream->def_sample_duration = dur;
        stream->def_sample_size = size;
        stream->def_sample_flags = flags;

      next:
        /* iterate all siblings */
        trex = qtdemux_tree_get_sibling_by_type_full (trex, FOURCC_trex,
            &trex_data);
      }
    }
  }

  *ds_duration = stream->def_sample_duration;
  *ds_size = stream->def_sample_size;
  *ds_size = stream->def_sample_size;

  /* even then, above values are better than random ... */
  if (G_UNLIKELY (!stream->parsed_trex)) {
    GST_WARNING_OBJECT (qtdemux,
        "failed to find fragment defaults for stream %d", stream->track_id);
    return FALSE;
  }

  return TRUE;
}

static gboolean
qtdemux_parse_trun (GstQTDemux * qtdemux, GstByteReader * trun,
    QtDemuxStream * stream, guint32 d_sample_duration, guint32 d_sample_size,
    guint32 d_sample_flags, gint64 moof_offset, gint64 moof_length,
    gint64 * base_offset, gint64 * running_offset)
{
  guint64 timestamp;
  gint32 data_offset = 0;
  guint32 flags = 0, first_flags = 0, samples_count = 0;
  gint i;
  guint8 *data;
  guint entry_size, dur_offset, size_offset, flags_offset = 0, ct_offset = 0;
  QtDemuxSample *sample;
  gboolean ismv = FALSE;

  GST_LOG_OBJECT (qtdemux, "parsing trun stream %d; "
      "default dur %d, size %d, flags 0x%x, base offset %" G_GINT64_FORMAT,
      stream->track_id, d_sample_duration, d_sample_size, d_sample_flags,
      *base_offset);

  if (!gst_byte_reader_skip (trun, 1) ||
      !gst_byte_reader_get_uint24_be (trun, &flags))
    goto fail;

  if (!gst_byte_reader_get_uint32_be (trun, &samples_count))
    goto fail;

  if (flags & TR_DATA_OFFSET) {
    /* note this is really signed */
    if (!gst_byte_reader_get_int32_be (trun, &data_offset))
      goto fail;
    GST_LOG_OBJECT (qtdemux, "trun data offset %d", data_offset);
    /* default base offset = first byte of moof */
    if (*base_offset == -1) {
      GST_LOG_OBJECT (qtdemux, "base_offset at moof");
      *base_offset = moof_offset;
    }
    *running_offset = *base_offset + data_offset;
  } else {
    /* if no offset at all, that would mean data starts at moof start,
     * which is a bit wrong and is ismv crappy way, so compensate
     * assuming data is in mdat following moof */
    if (*base_offset == -1) {
      *base_offset = moof_offset + moof_length + 8;
      GST_LOG_OBJECT (qtdemux, "base_offset assumed in mdat after moof");
      ismv = TRUE;
    }
    if (*running_offset == -1)
      *running_offset = *base_offset;
  }

  GST_LOG_OBJECT (qtdemux, "running offset now %" G_GINT64_FORMAT,
      *running_offset);
  GST_LOG_OBJECT (qtdemux, "trun offset %d, flags 0x%x, entries %d",
      data_offset, flags, samples_count);

  if (flags & TR_FIRST_SAMPLE_FLAGS) {
    if (G_UNLIKELY (flags & TR_SAMPLE_FLAGS)) {
      GST_DEBUG_OBJECT (qtdemux,
          "invalid flags; SAMPLE and FIRST_SAMPLE present, discarding latter");
      flags ^= TR_FIRST_SAMPLE_FLAGS;
    } else {
      if (!gst_byte_reader_get_uint32_be (trun, &first_flags))
        goto fail;
      GST_LOG_OBJECT (qtdemux, "first flags: 0x%x", first_flags);
    }
  }

  /* FIXME ? spec says other bits should also be checked to determine
   * entry size (and prefix size for that matter) */
  entry_size = 0;
  dur_offset = size_offset = 0;
  if (flags & TR_SAMPLE_DURATION) {
    GST_LOG_OBJECT (qtdemux, "entry duration present");
    dur_offset = entry_size;
    entry_size += 4;
  }
  if (flags & TR_SAMPLE_SIZE) {
    GST_LOG_OBJECT (qtdemux, "entry size present");
    size_offset = entry_size;
    entry_size += 4;
  }
  if (flags & TR_SAMPLE_FLAGS) {
    GST_LOG_OBJECT (qtdemux, "entry flags present");
    flags_offset = entry_size;
    entry_size += 4;
  }
  if (flags & TR_COMPOSITION_TIME_OFFSETS) {
    GST_LOG_OBJECT (qtdemux, "entry ct offset present");
    ct_offset = entry_size;
    entry_size += 4;
  }

  if (!qt_atom_parser_has_chunks (trun, samples_count, entry_size))
    goto fail;
  data = (guint8 *) gst_byte_reader_peek_data_unchecked (trun);

  if (stream->n_samples >=
      QTDEMUX_MAX_SAMPLE_INDEX_SIZE / sizeof (QtDemuxSample))
    goto index_too_big;

  GST_DEBUG_OBJECT (qtdemux, "allocating n_samples %u * %u (%.2f MB)",
      stream->n_samples, (guint) sizeof (QtDemuxSample),
      stream->n_samples * sizeof (QtDemuxSample) / (1024.0 * 1024.0));

  /* create a new array of samples if it's the first sample parsed */
  if (stream->n_samples == 0)
    stream->samples = g_try_new0 (QtDemuxSample, samples_count);
  /* or try to reallocate it with space enough to insert the new samples */
  else
    stream->samples = g_try_renew (QtDemuxSample, stream->samples,
        stream->n_samples + samples_count);
  if (stream->samples == NULL)
    goto out_of_memory;

  if (G_UNLIKELY (stream->n_samples == 0)) {
    /* the timestamp of the first sample is also provided by the tfra entry
     * but we shouldn't rely on it as it is at the end of files */
    timestamp = 0;
  } else {
    /* subsequent fragments extend stream */
    timestamp =
        stream->samples[stream->n_samples - 1].timestamp +
        stream->samples[stream->n_samples - 1].duration;
  }
  sample = stream->samples + stream->n_samples;
  for (i = 0; i < samples_count; i++) {
    guint32 dur, size, sflags, ct;

    /* first read sample data */
    if (flags & TR_SAMPLE_DURATION) {
      dur = QT_UINT32 (data + dur_offset);
    } else {
      dur = d_sample_duration;
    }
    if (flags & TR_SAMPLE_SIZE) {
      size = QT_UINT32 (data + size_offset);
    } else {
      size = d_sample_size;
    }
    if (flags & TR_FIRST_SAMPLE_FLAGS) {
      if (i == 0) {
        sflags = first_flags;
      } else {
        sflags = d_sample_flags;
      }
    } else if (flags & TR_SAMPLE_FLAGS) {
      sflags = QT_UINT32 (data + flags_offset);
    } else {
      sflags = d_sample_flags;
    }
    if (flags & TR_COMPOSITION_TIME_OFFSETS) {
      ct = QT_UINT32 (data + ct_offset);
    } else {
      ct = 0;
    }
    data += entry_size;

    /* fill the sample information */
    sample->offset = *running_offset;
    sample->pts_offset = ct;
    sample->size = size;
    sample->timestamp = timestamp;
    sample->duration = dur;
    /* sample-is-difference-sample */
    /* ismv seems to use 0x40 for keyframe, 0xc0 for non-keyframe,
     * now idea how it relates to bitfield other than massive LE/BE confusion */
    sample->keyframe = ismv ? ((sflags & 0xff) == 0x40) : !(sflags & 0x10000);
    *running_offset += size;
    timestamp += dur;
    sample++;
  }

  stream->n_samples += samples_count;

  return TRUE;

fail:
  {
    GST_WARNING_OBJECT (qtdemux, "failed to parse trun");
    return FALSE;
  }
out_of_memory:
  {
    GST_WARNING_OBJECT (qtdemux, "failed to allocate %d samples",
        stream->n_samples);
    return FALSE;
  }
index_too_big:
  {
    GST_WARNING_OBJECT (qtdemux, "not allocating index of %d samples, would "
        "be larger than %uMB (broken file?)", stream->n_samples,
        QTDEMUX_MAX_SAMPLE_INDEX_SIZE >> 20);
    return FALSE;
  }
}

/* find stream with @id */
static inline QtDemuxStream *
qtdemux_find_stream (GstQTDemux * qtdemux, guint32 id)
{
  QtDemuxStream *stream;
  gint i;

  /* check */
  if (G_UNLIKELY (!id)) {
    GST_DEBUG_OBJECT (qtdemux, "invalid track id 0");
    return NULL;
  }

  /* try to get it fast and simple */
  if (G_LIKELY (id <= qtdemux->n_streams)) {
    stream = qtdemux->streams[id - 1];
    if (G_LIKELY (stream->track_id == id))
      return stream;
  }

  /* linear search otherwise */
  for (i = 0; i < qtdemux->n_streams; i++) {
    stream = qtdemux->streams[i];
    if (stream->track_id == id)
      return stream;
  }

  return NULL;
}

static gboolean
qtdemux_parse_tfhd (GstQTDemux * qtdemux, GstByteReader * tfhd,
    QtDemuxStream ** stream, guint32 * default_sample_duration,
    guint32 * default_sample_size, guint32 * default_sample_flags,
    gint64 * base_offset)
{
  guint32 flags = 0;
  guint32 track_id = 0;

  if (!gst_byte_reader_skip (tfhd, 1) ||
      !gst_byte_reader_get_uint24_be (tfhd, &flags))
    goto invalid_track;

  if (!gst_byte_reader_get_uint32_be (tfhd, &track_id))
    goto invalid_track;

  *stream = qtdemux_find_stream (qtdemux, track_id);
  if (G_UNLIKELY (!*stream))
    goto unknown_stream;

  if (flags & TF_BASE_DATA_OFFSET)
    if (!gst_byte_reader_get_uint64_be (tfhd, (guint64 *) base_offset))
      goto invalid_track;

  /* obtain stream defaults */
  qtdemux_parse_trex (qtdemux, *stream,
      default_sample_duration, default_sample_size, default_sample_flags);

  /* FIXME: Handle TF_SAMPLE_DESCRIPTION_INDEX properly */
  if (flags & TF_SAMPLE_DESCRIPTION_INDEX)
    if (!gst_byte_reader_skip (tfhd, 4))
      goto invalid_track;

  if (flags & TF_DEFAULT_SAMPLE_DURATION)
    if (!gst_byte_reader_get_uint32_be (tfhd, default_sample_duration))
      goto invalid_track;

  if (flags & TF_DEFAULT_SAMPLE_SIZE)
    if (!gst_byte_reader_get_uint32_be (tfhd, default_sample_size))
      goto invalid_track;

  if (flags & TF_DEFAULT_SAMPLE_FLAGS)
    if (!gst_byte_reader_get_uint32_be (tfhd, default_sample_flags))
      goto invalid_track;

  return TRUE;

invalid_track:
  {
    GST_WARNING_OBJECT (qtdemux, "invalid track fragment header");
    return FALSE;
  }
unknown_stream:
  {
    GST_DEBUG_OBJECT (qtdemux, "unknown stream in tfhd");
    return TRUE;
  }
}

static gboolean
qtdemux_parse_moof (GstQTDemux * qtdemux, const guint8 * buffer, guint length,
    guint64 moof_offset, QtDemuxStream * stream)
{
  GNode *moof_node, *traf_node, *tfhd_node, *trun_node;
  GstByteReader trun_data, tfhd_data;
  guint32 ds_size = 0, ds_duration = 0, ds_flags = 0;
  gint64 base_offset, running_offset;

  /* NOTE @stream ignored */

  moof_node = g_node_new ((guint8 *) buffer);
  qtdemux_parse_node (qtdemux, moof_node, buffer, length);
  qtdemux_node_dump (qtdemux, moof_node);

  /* unknown base_offset to start with */
  base_offset = running_offset = -1;
  traf_node = qtdemux_tree_get_child_by_type (moof_node, FOURCC_traf);
  while (traf_node) {
    /* Fragment Header node */
    tfhd_node =
        qtdemux_tree_get_child_by_type_full (traf_node, FOURCC_tfhd,
        &tfhd_data);
    if (!tfhd_node)
      goto missing_tfhd;
    if (!qtdemux_parse_tfhd (qtdemux, &tfhd_data, &stream, &ds_duration,
            &ds_size, &ds_flags, &base_offset))
      goto missing_tfhd;
    if (G_UNLIKELY (!stream)) {
      /* we lost track of offset, we'll need to regain it,
       * but can delay complaining until later or avoid doing so altogether */
      base_offset = -2;
      goto next;
    }
    if (G_UNLIKELY (base_offset < -1))
      goto lost_offset;
    /* Track Run node */
    trun_node =
        qtdemux_tree_get_child_by_type_full (traf_node, FOURCC_trun,
        &trun_data);
    while (trun_node) {
      qtdemux_parse_trun (qtdemux, &trun_data, stream,
          ds_duration, ds_size, ds_flags, moof_offset, length, &base_offset,
          &running_offset);
      /* iterate all siblings */
      trun_node = qtdemux_tree_get_sibling_by_type_full (trun_node, FOURCC_trun,
          &trun_data);
    }
    /* if no new base_offset provided for next traf,
     * base is end of current traf */
    base_offset = running_offset;
    running_offset = -1;
  next:
    /* iterate all siblings */
    traf_node = qtdemux_tree_get_sibling_by_type (traf_node, FOURCC_traf);
  }
  g_node_destroy (moof_node);
  return TRUE;

missing_tfhd:
  {
    GST_DEBUG_OBJECT (qtdemux, "missing tfhd box");
    goto fail;
  }
lost_offset:
  {
    GST_DEBUG_OBJECT (qtdemux, "lost offset");
    goto fail;
  }
fail:
  {
    g_node_destroy (moof_node);
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
}

/* might be used if some day we actually use mfra & co
 * for random access to fragments,
 * but that will require quite some modifications and much less relying
 * on a sample array */
#if 0
static gboolean
qtdemux_parse_tfra (GstQTDemux * qtdemux, GNode * tfra_node,
    QtDemuxStream * stream)
{
  guint64 time = 0, moof_offset = 0;
  guint32 ver_flags, track_id, len, num_entries, i;
  guint value_size, traf_size, trun_size, sample_size;
  GstBuffer *buf = NULL;
  GstFlowReturn ret;
  GstByteReader tfra;

  gst_byte_reader_init (&tfra, (guint8 *) tfra_node->data + (4 + 4),
      QT_UINT32 ((guint8 *) tfra_node->data) - (4 + 4));

  if (!gst_byte_reader_get_uint32_be (&tfra, &ver_flags))
    return FALSE;

  if (!(gst_byte_reader_get_uint32_be (&tfra, &track_id) &&
          gst_byte_reader_get_uint32_be (&tfra, &len) &&
          gst_byte_reader_get_uint32_be (&tfra, &num_entries)))
    return FALSE;

  GST_LOG_OBJECT (qtdemux, "id %d == stream id %d ?",
      track_id, stream->track_id);
  if (track_id != stream->track_id) {
    return FALSE;
  }

  value_size = ((ver_flags >> 24) == 1) ? sizeof (guint64) : sizeof (guint32);
  sample_size = (len & 3) + 1;
  trun_size = ((len & 12) >> 2) + 1;
  traf_size = ((len & 48) >> 4) + 1;

  if (num_entries == 0)
    goto no_samples;

  if (!qt_atom_parser_has_chunks (&tfra, num_entries,
          value_size + value_size + traf_size + trun_size + sample_size))
    goto corrupt_file;

  for (i = 0; i < num_entries; i++) {
    qt_atom_parser_get_offset (&tfra, value_size, &time);
    qt_atom_parser_get_offset (&tfra, value_size, &moof_offset);
    qt_atom_parser_get_uint_with_size_unchecked (&tfra, traf_size);
    qt_atom_parser_get_uint_with_size_unchecked (&tfra, trun_size);
    qt_atom_parser_get_uint_with_size_unchecked (&tfra, sample_size);

    GST_LOG_OBJECT (qtdemux,
        "fragment time: %" GST_TIME_FORMAT " moof_offset: %u",
        GST_TIME_ARGS (gst_util_uint64_scale (time, GST_SECOND,
                stream->timescale)), moof_offset);

    ret = gst_qtdemux_pull_atom (qtdemux, moof_offset, 0, &buf);
    if (ret != GST_FLOW_OK)
      goto corrupt_file;
    qtdemux_parse_moof (qtdemux, GST_BUFFER_DATA (buf), GST_BUFFER_SIZE (buf),
        moof_offset, stream);
    gst_buffer_unref (buf);
  }

  return TRUE;

/* ERRORS */
corrupt_file:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DECODE,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
no_samples:
  {
    GST_WARNING_OBJECT (qtdemux, "stream has no samples");
    return FALSE;
  }
}

static gboolean
qtdemux_parse_mfra (GstQTDemux * qtdemux, QtDemuxStream * stream)
{
  GstFlowReturn ret;
  GNode *mfra_node, *tfra_node;
  GstBuffer *buffer;

  if (!qtdemux->mfra_offset)
    return FALSE;

  ret = gst_qtdemux_pull_atom (qtdemux, qtdemux->mfra_offset, 0, &buffer);
  if (ret != GST_FLOW_OK)
    goto corrupt_file;

  mfra_node = g_node_new ((guint8 *) GST_BUFFER_DATA (buffer));
  qtdemux_parse_node (qtdemux, mfra_node, GST_BUFFER_DATA (buffer),
      GST_BUFFER_SIZE (buffer));

  tfra_node = qtdemux_tree_get_child_by_type (mfra_node, FOURCC_tfra);

  while (tfra_node) {
    qtdemux_parse_tfra (qtdemux, tfra_node, stream);
    /* iterate all siblings */
    tfra_node = qtdemux_tree_get_sibling_by_type (tfra_node, FOURCC_tfra);
  }
  g_node_destroy (mfra_node);
  gst_buffer_unref (buffer);

  return TRUE;

corrupt_file:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DECODE,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
}

static GstFlowReturn
qtdemux_parse_mfro (GstQTDemux * qtdemux, guint64 * mfra_offset,
    guint32 * mfro_size)
{
  GstFlowReturn ret = GST_FLOW_ERROR;
  GstBuffer *mfro = NULL;
  guint32 fourcc;
  gint64 len;
  GstFormat fmt = GST_FORMAT_BYTES;

  if (!gst_pad_query_peer_duration (qtdemux->sinkpad, &fmt, &len)) {
    GST_DEBUG_OBJECT (qtdemux, "upstream size not available; "
        "can not locate mfro");
    goto exit;
  }

  ret = gst_qtdemux_pull_atom (qtdemux, len - 16, 16, &mfro);
  if (ret != GST_FLOW_OK)
    goto exit;

  fourcc = QT_FOURCC (GST_BUFFER_DATA (mfro) + 4);
  if (fourcc != FOURCC_mfro)
    goto exit;

  GST_INFO_OBJECT (qtdemux, "Found mfro atom: fragmented mp4 container");
  if (GST_BUFFER_SIZE (mfro) >= 16) {
    GST_DEBUG_OBJECT (qtdemux, "parsing 'mfro' atom");
    *mfro_size = QT_UINT32 (GST_BUFFER_DATA (mfro) + 12);
    if (*mfro_size >= len) {
      GST_WARNING_OBJECT (qtdemux, "mfro.size is invalid");
      ret = GST_FLOW_ERROR;
      goto exit;
    }
    *mfra_offset = len - *mfro_size;
  }

exit:
  if (mfro)
    gst_buffer_unref (mfro);

  return ret;
}

static void
qtdemux_parse_fragmented (GstQTDemux * qtdemux)
{
  GstFlowReturn ret;
  guint32 mfra_size = 0;
  guint64 mfra_offset = 0;

  /* default */
  qtdemux->fragmented = FALSE;

  /* We check here if it is a fragmented mp4 container */
  ret = qtdemux_parse_mfro (qtdemux, &mfra_offset, &mfra_size);
  if (ret == GST_FLOW_OK && mfra_size != 0 && mfra_offset != 0) {
    qtdemux->fragmented = TRUE;
    GST_DEBUG_OBJECT (qtdemux,
        "mfra atom expected at offset %" G_GUINT64_FORMAT, mfra_offset);
    qtdemux->mfra_offset = mfra_offset;
  }
}
#endif

static GstFlowReturn
gst_qtdemux_loop_state_header (GstQTDemux * qtdemux)
{
  guint64 length = 0;
  guint32 fourcc = 0;
  GstBuffer *buf = NULL;
  GstFlowReturn ret = GST_FLOW_OK;
  guint64 cur_offset = qtdemux->offset;

  ret = gst_pad_pull_range (qtdemux->sinkpad, cur_offset, 16, &buf);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto beach;
  if (G_LIKELY (GST_BUFFER_SIZE (buf) >= 8))
    extract_initial_length_and_fourcc (GST_BUFFER_DATA (buf),
        GST_BUFFER_SIZE (buf), &length, &fourcc);
  gst_buffer_unref (buf);

  /* maybe we already got most we needed, so only consider this eof */
  if (G_UNLIKELY (length == 0)) {
    GST_ELEMENT_WARNING (qtdemux, STREAM, DEMUX,
        (_("Invalid atom size.")),
        ("Header atom '%" GST_FOURCC_FORMAT "' has empty length",
            GST_FOURCC_ARGS (fourcc)));
    ret = GST_FLOW_UNEXPECTED;
    goto beach;
  }

  switch (fourcc) {
    case FOURCC_moof:
      /* record for later parsing when needed */
      if (!qtdemux->moof_offset) {
        qtdemux->moof_offset = qtdemux->offset;
      }
      /* fall-through */
    case FOURCC_mdat:
    case FOURCC_free:
    case FOURCC_wide:
    case FOURCC_PICT:
    case FOURCC_pnot:
    {
      GST_LOG_OBJECT (qtdemux,
          "skipping atom '%" GST_FOURCC_FORMAT "' at %" G_GUINT64_FORMAT,
          GST_FOURCC_ARGS (fourcc), cur_offset);
      qtdemux->offset += length;
      break;
    }
    case FOURCC_moov:
    {
      GstBuffer *moov;

      if (qtdemux->got_moov) {
        GST_DEBUG_OBJECT (qtdemux, "Skipping moov atom as we have one already");
        qtdemux->offset += length;
        goto beach;
      }

      ret = gst_pad_pull_range (qtdemux->sinkpad, cur_offset, length, &moov);
      if (ret != GST_FLOW_OK)
        goto beach;
      if (length != GST_BUFFER_SIZE (moov)) {
        /* Some files have a 'moov' atom at the end of the file which contains
         * a terminal 'free' atom where the body of the atom is missing.
         * Check for, and permit, this special case.
         */
        if (GST_BUFFER_SIZE (moov) >= 8) {
          guint8 *final_data = GST_BUFFER_DATA (moov) +
              (GST_BUFFER_SIZE (moov) - 8);
          guint32 final_length = QT_UINT32 (final_data);
          guint32 final_fourcc = QT_FOURCC (final_data + 4);
          if (final_fourcc == FOURCC_free &&
              GST_BUFFER_SIZE (moov) + final_length - 8 == length) {
            /* Ok, we've found that special case. Allocate a new buffer with
             * that free atom actually present. */
            GstBuffer *newmoov = gst_buffer_new_and_alloc (length);
            gst_buffer_copy_metadata (newmoov, moov,
                GST_BUFFER_COPY_FLAGS | GST_BUFFER_COPY_TIMESTAMPS |
                GST_BUFFER_COPY_CAPS);
            memcpy (GST_BUFFER_DATA (newmoov), GST_BUFFER_DATA (moov),
                GST_BUFFER_SIZE (moov));
            memset (GST_BUFFER_DATA (newmoov) + GST_BUFFER_SIZE (moov), 0,
                final_length - 8);
            gst_buffer_unref (moov);
            moov = newmoov;
          }
        }
      }

      if (length != GST_BUFFER_SIZE (moov)) {
        GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
            (_("This file is incomplete and cannot be played.")),
            ("We got less than expected (received %u, wanted %u, offset %"
                G_GUINT64_FORMAT ")",
                GST_BUFFER_SIZE (moov), (guint) length, cur_offset));
        ret = GST_FLOW_ERROR;
        goto beach;
      }
      qtdemux->offset += length;

      qtdemux_parse_moov (qtdemux, GST_BUFFER_DATA (moov), length);
      qtdemux_node_dump (qtdemux, qtdemux->moov_node);

#ifdef GSTREAMER_LITE
      if (!qtdemux_parse_tree (qtdemux))
      {
          ret = GST_FLOW_ERROR;
          break;
      }
#else
      qtdemux_parse_tree (qtdemux);
#endif //GSTREAMER_LITE

      g_node_destroy (qtdemux->moov_node);
      gst_buffer_unref (moov);
      qtdemux->moov_node = NULL;
      qtdemux->got_moov = TRUE;

      break;
    }
    case FOURCC_ftyp:
    {
      GstBuffer *ftyp;

      /* extract major brand; might come in handy for ISO vs QT issues */
      ret = gst_qtdemux_pull_atom (qtdemux, cur_offset, length, &ftyp);
      if (ret != GST_FLOW_OK)
        goto beach;
      qtdemux->offset += length;
      qtdemux_parse_ftyp (qtdemux, GST_BUFFER_DATA (ftyp),
          GST_BUFFER_SIZE (ftyp));
      gst_buffer_unref (ftyp);
      break;
    }
    case FOURCC_uuid:
    {
      GstBuffer *uuid;

      /* uuid are extension atoms */
      ret = gst_qtdemux_pull_atom (qtdemux, cur_offset, length, &uuid);
      if (ret != GST_FLOW_OK)
        goto beach;
      qtdemux->offset += length;
      qtdemux_parse_uuid (qtdemux, GST_BUFFER_DATA (uuid),
          GST_BUFFER_SIZE (uuid));
      gst_buffer_unref (uuid);
      break;
    }
    default:
    {
      GstBuffer *unknown;

      GST_LOG_OBJECT (qtdemux,
          "unknown %08x '%" GST_FOURCC_FORMAT "' of size %" G_GUINT64_FORMAT
          " at %" G_GUINT64_FORMAT, fourcc, GST_FOURCC_ARGS (fourcc), length,
          cur_offset);
      ret = gst_qtdemux_pull_atom (qtdemux, cur_offset, length, &unknown);
      if (ret != GST_FLOW_OK)
        goto beach;
      GST_MEMDUMP ("Unknown tag", GST_BUFFER_DATA (unknown),
          GST_BUFFER_SIZE (unknown));
      gst_buffer_unref (unknown);
      qtdemux->offset += length;
      break;
    }
  }

beach:
  if (ret == GST_FLOW_UNEXPECTED && qtdemux->got_moov) {
    /* digested all data, show what we have */
    ret = qtdemux_expose_streams (qtdemux);

    /* Only post, event on pads is done after newsegment */
    qtdemux_post_global_tags (qtdemux);

    qtdemux->state = QTDEMUX_STATE_MOVIE;
    GST_DEBUG_OBJECT (qtdemux, "switching state to STATE_MOVIE (%d)",
        qtdemux->state);
    return ret;
  }
  return ret;
}

/* Seeks to the previous keyframe of the indexed stream and
 * aligns other streams with respect to the keyframe timestamp
 * of indexed stream. Only called in case of Reverse Playback
 */
static GstFlowReturn
gst_qtdemux_seek_to_previous_keyframe (GstQTDemux * qtdemux)
{
  guint8 n = 0;
  guint32 seg_idx = 0, k_index = 0;
  guint32 ref_seg_idx, ref_k_index;
  guint64 k_pos = 0, last_stop = 0;
  QtDemuxSegment *seg = NULL;
  QtDemuxStream *ref_str = NULL;
  guint64 seg_media_start_mov;  /* segment media start time in mov format */

  /* Now we choose an arbitrary stream, get the previous keyframe timestamp
   * and finally align all the other streams on that timestamp with their
   * respective keyframes */
  for (n = 0; n < qtdemux->n_streams; n++) {
    QtDemuxStream *str = qtdemux->streams[n];

    seg_idx = gst_qtdemux_find_segment (qtdemux, str,
        qtdemux->segment.last_stop);

    /* segment not found, continue with normal flow */
    if (seg_idx == -1)
      continue;

    /* No candidate yet, take that one */
    if (!ref_str) {
      ref_str = str;
      continue;
    }

    /* So that stream has a segment, we prefer video streams */
    if (str->subtype == FOURCC_vide) {
      ref_str = str;
      break;
    }
  }

  if (G_UNLIKELY (!ref_str)) {
    GST_DEBUG_OBJECT (qtdemux, "couldn't find any stream");
    goto eos;
  }

  if (G_UNLIKELY (!ref_str->from_sample)) {
    GST_DEBUG_OBJECT (qtdemux, "reached the beginning of the file");
    goto eos;
  }

  /* So that stream has been playing from from_sample to to_sample. We will
   * get the timestamp of the previous sample and search for a keyframe before
   * that. For audio streams we do an arbitrary jump in the past (10 samples) */
  if (ref_str->subtype == FOURCC_vide) {
    k_index = gst_qtdemux_find_keyframe (qtdemux, ref_str,
        ref_str->from_sample - 1);
  } else {
    if (ref_str->from_sample >= 10)
      k_index = ref_str->from_sample - 10;
    else
      k_index = 0;
  }

  /* get current segment for that stream */
  seg = &ref_str->segments[ref_str->segment_index];
  /* convert seg->media_start to mov format time for timestamp comparison */
  seg_media_start_mov =
      gst_util_uint64_scale (seg->media_start, ref_str->timescale, GST_SECOND);
  /* Crawl back through segments to find the one containing this I frame */
  while (ref_str->samples[k_index].timestamp < seg_media_start_mov) {
    GST_DEBUG_OBJECT (qtdemux, "keyframe position is out of segment %u",
        ref_str->segment_index);
    if (G_UNLIKELY (!ref_str->segment_index)) {
      /* Reached first segment, let's consider it's EOS */
      goto eos;
    }
    ref_str->segment_index--;
    seg = &ref_str->segments[ref_str->segment_index];
    /* convert seg->media_start to mov format time for timestamp comparison */
    seg_media_start_mov =
        gst_util_uint64_scale (seg->media_start, ref_str->timescale,
        GST_SECOND);
  }
  /* Calculate time position of the keyframe and where we should stop */
  k_pos =
      (gst_util_uint64_scale (ref_str->samples[k_index].timestamp, GST_SECOND,
          ref_str->timescale) - seg->media_start) + seg->time;
  last_stop =
      gst_util_uint64_scale (ref_str->samples[ref_str->from_sample].timestamp,
      GST_SECOND, ref_str->timescale);
  last_stop = (last_stop - seg->media_start) + seg->time;

  GST_DEBUG_OBJECT (qtdemux, "preferred stream played from sample %u, "
      "now going to sample %u (pts %" GST_TIME_FORMAT ")", ref_str->from_sample,
      k_index, GST_TIME_ARGS (k_pos));

  /* Set last_stop with the keyframe timestamp we pushed of that stream */
  gst_segment_set_last_stop (&qtdemux->segment, GST_FORMAT_TIME, last_stop);
  GST_DEBUG_OBJECT (qtdemux, "last_stop now is %" GST_TIME_FORMAT,
      GST_TIME_ARGS (last_stop));

  if (G_UNLIKELY (last_stop < qtdemux->segment.start)) {
    GST_DEBUG_OBJECT (qtdemux, "reached the beginning of segment");
    goto eos;
  }

  ref_seg_idx = ref_str->segment_index;
  ref_k_index = k_index;

  /* Align them all on this */
  for (n = 0; n < qtdemux->n_streams; n++) {
    guint32 index = 0;
    guint64 media_start = 0, seg_time = 0;
    QtDemuxStream *str = qtdemux->streams[n];

    /* aligning reference stream again might lead to backing up to yet another
     * keyframe (due to timestamp rounding issues),
     * potentially putting more load on downstream; so let's try to avoid */
    if (str == ref_str) {
      seg_idx = ref_seg_idx;
      seg = &str->segments[seg_idx];
      k_index = ref_k_index;
      GST_DEBUG_OBJECT (qtdemux, "reference stream segment %d, "
          "sample at index %d", ref_str->segment_index, k_index);
    } else {
      seg_idx = gst_qtdemux_find_segment (qtdemux, str, k_pos);
      GST_DEBUG_OBJECT (qtdemux, "align segment %d", seg_idx);

      /* segment not found, continue with normal flow */
      if (seg_idx == -1)
        continue;

      /* get segment and time in the segment */
      seg = &str->segments[seg_idx];
      seg_time = k_pos - seg->time;

      /* get the media time in the segment */
      media_start = seg->media_start + seg_time;

      /* get the index of the sample with media time */
      index = gst_qtdemux_find_index_linear (qtdemux, str, media_start);
      GST_DEBUG_OBJECT (qtdemux, "sample for %" GST_TIME_FORMAT " at %u",
          GST_TIME_ARGS (media_start), index);

      /* find previous keyframe */
      k_index = gst_qtdemux_find_keyframe (qtdemux, str, index);
    }

    /* Remember until where we want to go */
    str->to_sample = str->from_sample - 1;
    /* Define our time position */
    str->time_position =
        (gst_util_uint64_scale (str->samples[k_index].timestamp, GST_SECOND,
            str->timescale) - seg->media_start) + seg->time;
    /* Now seek back in time */
    gst_qtdemux_move_stream (qtdemux, str, k_index);
    GST_DEBUG_OBJECT (qtdemux, "keyframe at %u, time position %"
        GST_TIME_FORMAT " playing from sample %u to %u", k_index,
        GST_TIME_ARGS (str->time_position), str->from_sample, str->to_sample);
  }

  return GST_FLOW_OK;

eos:
  return GST_FLOW_UNEXPECTED;
}

/* activate the given segment number @seg_idx of @stream at time @offset.
 * @offset is an absolute global position over all the segments.
 *
 * This will push out a NEWSEGMENT event with the right values and
 * position the stream index to the first decodable sample before
 * @offset.
 */
static gboolean
gst_qtdemux_activate_segment (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint32 seg_idx, guint64 offset)
{
  GstEvent *event;
  QtDemuxSegment *segment;
  guint32 index, kf_index;
  guint64 seg_time;
  guint64 start, stop, time;
  gdouble rate;

  GST_LOG_OBJECT (qtdemux, "activate segment %d, offset %" G_GUINT64_FORMAT,
      seg_idx, offset);

  /* update the current segment */
  stream->segment_index = seg_idx;

  /* get the segment */
  segment = &stream->segments[seg_idx];

  if (G_UNLIKELY (offset < segment->time)) {
    GST_WARNING_OBJECT (qtdemux, "offset < segment->time %" G_GUINT64_FORMAT,
        segment->time);
    return FALSE;
  }

  /* segment lies beyond total indicated duration */
  if (G_UNLIKELY (qtdemux->segment.duration != -1 &&
          segment->time > qtdemux->segment.duration)) {
    GST_WARNING_OBJECT (qtdemux, "file duration %" G_GINT64_FORMAT
        " < segment->time %" G_GUINT64_FORMAT, qtdemux->segment.duration,
        segment->time);
    return FALSE;
  }

  /* get time in this segment */
  seg_time = offset - segment->time;

  GST_LOG_OBJECT (qtdemux, "seg_time %" GST_TIME_FORMAT,
      GST_TIME_ARGS (seg_time));

  if (G_UNLIKELY (seg_time > segment->duration)) {
    GST_LOG_OBJECT (qtdemux, "seg_time > segment->duration %" GST_TIME_FORMAT,
        GST_TIME_ARGS (segment->duration));
    return FALSE;
  }

  /* qtdemux->segment.stop is in outside-time-realm, whereas
   * segment->media_stop is in track-time-realm.
   *
   * In order to compare the two, we need to bring segment.stop
   * into the track-time-realm */

  stop = qtdemux->segment.stop;
  if (stop == -1)
    stop = qtdemux->segment.duration;
  if (stop == -1)
    stop = segment->media_stop;
  else
    stop =
        MIN (segment->media_stop, stop - segment->time + segment->media_start);

  if (qtdemux->segment.rate >= 0) {
    start = MIN (segment->media_start + seg_time, stop);
    time = offset;
  } else {
    if (segment->media_start >= qtdemux->segment.start) {
      start = segment->media_start;
      time = segment->time;
    } else {
      start = qtdemux->segment.start;
      time = segment->time + (qtdemux->segment.start - segment->media_start);
    }

    start = MAX (segment->media_start, qtdemux->segment.start);
    stop = MIN (segment->media_start + seg_time, stop);
  }

  GST_DEBUG_OBJECT (qtdemux, "newsegment %d from %" GST_TIME_FORMAT
      " to %" GST_TIME_FORMAT ", time %" GST_TIME_FORMAT, seg_idx,
      GST_TIME_ARGS (start), GST_TIME_ARGS (stop), GST_TIME_ARGS (time));

  /* combine global rate with that of the segment */
  rate = segment->rate * qtdemux->segment.rate;

  /* update the segment values used for clipping */
  gst_segment_init (&stream->segment, GST_FORMAT_TIME);
  gst_segment_set_newsegment (&stream->segment, FALSE, rate, GST_FORMAT_TIME,
      start, stop, time);

  /* now prepare and send the segment */
  if (stream->pad) {
    event = gst_event_new_new_segment (FALSE, rate, GST_FORMAT_TIME,
        start, stop, time);
    gst_pad_push_event (stream->pad, event);
    /* assume we can send more data now */
    stream->last_ret = GST_FLOW_OK;
    /* clear to send tags on this pad now */
    gst_qtdemux_push_tags (qtdemux, stream);
  }

  /* and move to the keyframe before the indicated media time of the
   * segment */
  if (qtdemux->segment.rate >= 0) {
    index = gst_qtdemux_find_index_linear (qtdemux, stream, start);
    stream->to_sample = G_MAXUINT32;
    GST_DEBUG_OBJECT (qtdemux, "moving data pointer to %" GST_TIME_FORMAT
        ", index: %u, pts %" GST_TIME_FORMAT, GST_TIME_ARGS (start), index,
        GST_TIME_ARGS (gst_util_uint64_scale (stream->samples[index].timestamp,
                GST_SECOND, stream->timescale)));
  } else {
    index = gst_qtdemux_find_index_linear (qtdemux, stream, stop);
    stream->to_sample = index;
    GST_DEBUG_OBJECT (qtdemux, "moving data pointer to %" GST_TIME_FORMAT
        ", index: %u, pts %" GST_TIME_FORMAT, GST_TIME_ARGS (stop), index,
        GST_TIME_ARGS (gst_util_uint64_scale (stream->samples[index].timestamp,
                GST_SECOND, stream->timescale)));
  }

  /* gst_qtdemux_parse_sample () called from gst_qtdemux_find_index_linear ()
   * encountered an error and printed a message so we return appropriately */
  if (index == -1)
    return FALSE;

  /* we're at the right spot */
  if (index == stream->sample_index) {
    GST_DEBUG_OBJECT (qtdemux, "we are at the right index");
    return TRUE;
  }

  /* find keyframe of the target index */
  kf_index = gst_qtdemux_find_keyframe (qtdemux, stream, index);

/* *INDENT-OFF* */
/* indent does stupid stuff with stream->samples[].timestamp */

  /* if we move forwards, we don't have to go back to the previous
   * keyframe since we already sent that. We can also just jump to
   * the keyframe right before the target index if there is one. */
  if (index > stream->sample_index) {
    /* moving forwards check if we move past a keyframe */
    if (kf_index > stream->sample_index) {
      GST_DEBUG_OBJECT (qtdemux,
          "moving forwards to keyframe at %u (pts %" GST_TIME_FORMAT, kf_index,
          GST_TIME_ARGS (gst_util_uint64_scale (
                  stream->samples[kf_index].timestamp,
                  GST_SECOND, stream->timescale)));
      gst_qtdemux_move_stream (qtdemux, stream, kf_index);
    } else {
      GST_DEBUG_OBJECT (qtdemux,
          "moving forwards, keyframe at %u (pts %" GST_TIME_FORMAT
          " already sent", kf_index,
          GST_TIME_ARGS (gst_util_uint64_scale (
                  stream->samples[kf_index].timestamp,
                  GST_SECOND, stream->timescale)));
    }
  } else {
    GST_DEBUG_OBJECT (qtdemux,
        "moving backwards to keyframe at %u (pts %" GST_TIME_FORMAT, kf_index,
        GST_TIME_ARGS (gst_util_uint64_scale (
                stream->samples[kf_index].timestamp,
                GST_SECOND, stream->timescale)));
    gst_qtdemux_move_stream (qtdemux, stream, kf_index);
  }

/* *INDENT-ON* */

  return TRUE;
}

/* prepare to get the current sample of @stream, getting essential values.
 *
 * This function will also prepare and send the segment when needed.
 *
 * Return FALSE if the stream is EOS.
 */
static gboolean
gst_qtdemux_prepare_current_sample (GstQTDemux * qtdemux,
    QtDemuxStream * stream, guint64 * offset, guint * size, guint64 * timestamp,
    guint64 * duration, gboolean * keyframe)
{
  QtDemuxSample *sample;
  guint64 time_position;
  guint32 seg_idx;

  g_return_val_if_fail (stream != NULL, FALSE);

  time_position = stream->time_position;
  if (G_UNLIKELY (time_position == -1))
    goto eos;

  seg_idx = stream->segment_index;
  if (G_UNLIKELY (seg_idx == -1)) {
    /* find segment corresponding to time_position if we are looking
     * for a segment. */
    seg_idx = gst_qtdemux_find_segment (qtdemux, stream, time_position);

    /* nothing found, we're really eos */
    if (seg_idx == -1)
      goto eos;
  }

  /* different segment, activate it, sample_index will be set. */
  if (G_UNLIKELY (stream->segment_index != seg_idx))
    gst_qtdemux_activate_segment (qtdemux, stream, seg_idx, time_position);

  GST_LOG_OBJECT (qtdemux, "segment active, index = %u of %u",
      stream->sample_index, stream->n_samples);

  if (G_UNLIKELY (stream->sample_index >= stream->n_samples))
    goto eos;

  if (!qtdemux_parse_samples (qtdemux, stream, stream->sample_index)) {
    GST_LOG_OBJECT (qtdemux, "Parsing of index %u failed!",
        stream->sample_index);
    return FALSE;
  }

  /* now get the info for the sample we're at */
  sample = &stream->samples[stream->sample_index];

  *timestamp = QTSAMPLE_PTS (stream, sample);
  *offset = sample->offset;
  *size = sample->size;
  *duration = QTSAMPLE_DUR_PTS (stream, sample, *timestamp);
  *keyframe = QTSAMPLE_KEYFRAME (stream, sample);

  return TRUE;

  /* special cases */
eos:
  {
    stream->time_position = -1;
    return FALSE;
  }
}

/* move to the next sample in @stream.
 *
 * Moves to the next segment when needed.
 */
static void
gst_qtdemux_advance_sample (GstQTDemux * qtdemux, QtDemuxStream * stream)
{
  QtDemuxSample *sample;
  QtDemuxSegment *segment;

  if (G_UNLIKELY (stream->sample_index >= stream->to_sample)) {
    /* Mark the stream as EOS */
    GST_DEBUG_OBJECT (qtdemux,
        "reached max allowed sample %u, mark EOS", stream->to_sample);
    stream->time_position = -1;
    return;
  }

  /* move to next sample */
  stream->sample_index++;

  /* get current segment */
  segment = &stream->segments[stream->segment_index];

  /* reached the last sample, we need the next segment */
  if (G_UNLIKELY (stream->sample_index >= stream->n_samples))
    goto next_segment;

  if (!qtdemux_parse_samples (qtdemux, stream, stream->sample_index)) {
    GST_LOG_OBJECT (qtdemux, "Parsing of index %u failed!",
        stream->sample_index);
    return;
  }

  /* get next sample */
  sample = &stream->samples[stream->sample_index];

  /* see if we are past the segment */
  if (G_UNLIKELY (gst_util_uint64_scale (sample->timestamp,
              GST_SECOND, stream->timescale) >= segment->media_stop))
    goto next_segment;

  if (gst_util_uint64_scale (sample->timestamp, GST_SECOND,
          stream->timescale) >= segment->media_start) {
    /* inside the segment, update time_position, looks very familiar to
     * GStreamer segments, doesn't it? */
    stream->time_position =
        (gst_util_uint64_scale (sample->timestamp, GST_SECOND,
            stream->timescale) - segment->media_start) + segment->time;
  } else {
    /* not yet in segment, time does not yet increment. This means
     * that we are still prerolling keyframes to the decoder so it can
     * decode the first sample of the segment. */
    stream->time_position = segment->time;
  }
  return;

  /* move to the next segment */
next_segment:
  {
    GST_DEBUG_OBJECT (qtdemux, "segment %d ended ", stream->segment_index);

    if (stream->segment_index == stream->n_segments - 1) {
      /* are we at the end of the last segment, we're EOS */
      stream->time_position = -1;
    } else {
      /* else we're only at the end of the current segment */
      stream->time_position = segment->stop_time;
    }
    /* make sure we select a new segment */
    stream->segment_index = -1;
  }
}

static void
gst_qtdemux_sync_streams (GstQTDemux * demux)
{
  gint i;

  if (demux->n_streams <= 1)
    return;

  for (i = 0; i < demux->n_streams; i++) {
    QtDemuxStream *stream;
    GstClockTime end_time;

    stream = demux->streams[i];

    if (!stream->pad)
      continue;

    /* TODO advance time on subtitle streams here, if any some day */

    /* some clips/trailers may have unbalanced streams at the end,
     * so send EOS on shorter stream to prevent stalling others */

    /* do not mess with EOS if SEGMENT seeking */
    if (demux->segment.flags & GST_SEEK_FLAG_SEGMENT)
      continue;

    if (demux->pullbased) {
      /* loop mode is sample time based */
      if (stream->time_position != -1)
        continue;
    } else {
      /* push mode is byte position based */
      if (stream->n_samples &&
          stream->samples[stream->n_samples - 1].offset >= demux->offset)
        continue;
    }

    if (stream->sent_eos)
      continue;

    /* only act if some gap */
    end_time = stream->segments[stream->n_segments - 1].stop_time;
    GST_LOG_OBJECT (demux, "current position: %" GST_TIME_FORMAT
        ", stream end: %" GST_TIME_FORMAT,
        GST_TIME_ARGS (demux->segment.last_stop), GST_TIME_ARGS (end_time));
    if (end_time + 2 * GST_SECOND < demux->segment.last_stop) {
      GST_DEBUG_OBJECT (demux, "sending EOS for stream %s",
          GST_PAD_NAME (stream->pad));
      stream->sent_eos = TRUE;
      gst_pad_push_event (stream->pad, gst_event_new_eos ());
    }
  }
}

/* UNEXPECTED and NOT_LINKED need to be combined. This means that we return:
 *
 *  GST_FLOW_NOT_LINKED: when all pads NOT_LINKED.
 *  GST_FLOW_UNEXPECTED: when all pads UNEXPECTED or NOT_LINKED.
 */
static GstFlowReturn
gst_qtdemux_combine_flows (GstQTDemux * demux, QtDemuxStream * stream,
    GstFlowReturn ret)
{
  gint i;
  gboolean unexpected = FALSE, not_linked = TRUE;

  GST_LOG_OBJECT (demux, "flow return: %s", gst_flow_get_name (ret));

  /* store the value */
  stream->last_ret = ret;

  /* any other error that is not-linked or eos can be returned right away */
  if (G_LIKELY (ret != GST_FLOW_UNEXPECTED && ret != GST_FLOW_NOT_LINKED))
    goto done;

  /* only return NOT_LINKED if all other pads returned NOT_LINKED */
  for (i = 0; i < demux->n_streams; i++) {
    QtDemuxStream *ostream = demux->streams[i];

    ret = ostream->last_ret;

    /* no unexpected or unlinked, return */
    if (G_LIKELY (ret != GST_FLOW_UNEXPECTED && ret != GST_FLOW_NOT_LINKED))
      goto done;

    /* we check to see if we have at least 1 unexpected or all unlinked */
    unexpected |= (ret == GST_FLOW_UNEXPECTED);
    not_linked &= (ret == GST_FLOW_NOT_LINKED);
  }

  /* when we get here, we all have unlinked or unexpected */
  if (not_linked)
    ret = GST_FLOW_NOT_LINKED;
  else if (unexpected)
    ret = GST_FLOW_UNEXPECTED;
done:
  GST_LOG_OBJECT (demux, "combined flow return: %s", gst_flow_get_name (ret));
  return ret;
}

/* the input buffer metadata must be writable. Returns NULL when the buffer is
 * completely cliped */
static GstBuffer *
gst_qtdemux_clip_buffer (GstQTDemux * qtdemux, QtDemuxStream * stream,
    GstBuffer * buf)
{
  gint64 start, stop, cstart, cstop, diff;
  GstClockTime timestamp = GST_CLOCK_TIME_NONE, duration = GST_CLOCK_TIME_NONE;
  guint8 *data;
  guint size;
  gint num_rate, denom_rate;
  gint frame_size;
  gboolean clip_data;

  data = GST_BUFFER_DATA (buf);
  size = GST_BUFFER_SIZE (buf);

  /* depending on the type, setup the clip parameters */
  if (stream->subtype == FOURCC_soun) {
    frame_size = stream->bytes_per_frame;
    num_rate = GST_SECOND;
    denom_rate = (gint) stream->rate;
    clip_data = TRUE;
  } else if (stream->subtype == FOURCC_vide) {
    frame_size = size;
    num_rate = stream->fps_n;
    denom_rate = stream->fps_d;
    clip_data = FALSE;
  } else
    goto wrong_type;

  /* we can only clip if we have a valid timestamp */
  timestamp = GST_BUFFER_TIMESTAMP (buf);
  if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (timestamp)))
    goto no_timestamp;

  if (G_LIKELY (GST_BUFFER_DURATION_IS_VALID (buf))) {
    duration = GST_BUFFER_DURATION (buf);
  } else {
    duration =
        gst_util_uint64_scale_int (size / frame_size, num_rate, denom_rate);
  }

  start = timestamp;
  stop = start + duration;

  if (G_UNLIKELY (!gst_segment_clip (&stream->segment,
              GST_FORMAT_TIME, start, stop, &cstart, &cstop)))
    goto clipped;

  /* see if some clipping happened */
  diff = cstart - start;
  if (diff > 0) {
    timestamp = cstart;
    duration -= diff;

    if (clip_data) {
      /* bring clipped time to samples and to bytes */
      diff = gst_util_uint64_scale_int (diff, denom_rate, num_rate);
      diff *= frame_size;

      GST_DEBUG_OBJECT (qtdemux,
          "clipping start to %" GST_TIME_FORMAT " %"
          G_GUINT64_FORMAT " bytes", GST_TIME_ARGS (cstart), diff);

      data += diff;
      size -= diff;
    }
  }
  diff = stop - cstop;
  if (diff > 0) {
    duration -= diff;

    if (clip_data) {
      /* bring clipped time to samples and then to bytes */
      diff = gst_util_uint64_scale_int (diff, denom_rate, num_rate);
      diff *= frame_size;
      GST_DEBUG_OBJECT (qtdemux,
          "clipping stop to %" GST_TIME_FORMAT " %" G_GUINT64_FORMAT
          " bytes", GST_TIME_ARGS (cstop), diff);
      size -= diff;
    }
  }

  GST_BUFFER_TIMESTAMP (buf) = timestamp;
  GST_BUFFER_DURATION (buf) = duration;
  GST_BUFFER_SIZE (buf) = size;
  GST_BUFFER_DATA (buf) = data;

  return buf;

  /* dropped buffer */
wrong_type:
  {
    GST_DEBUG_OBJECT (qtdemux, "unknown stream type");
    return buf;
  }
no_timestamp:
  {
    GST_DEBUG_OBJECT (qtdemux, "no timestamp on buffer");
    return buf;
  }
clipped:
  {
    GST_DEBUG_OBJECT (qtdemux, "clipped buffer");
    gst_buffer_unref (buf);
    return NULL;
  }
}

/* the input buffer metadata must be writable,
 * but time/duration etc not yet set and need not be preserved */
static GstBuffer *
gst_qtdemux_process_buffer (GstQTDemux * qtdemux, QtDemuxStream * stream,
    GstBuffer * buf)
{
  guint8 *data;
  guint size, nsize = 0;
  gchar *str;

  data = GST_BUFFER_DATA (buf);
  size = GST_BUFFER_SIZE (buf);

  /* not many cases for now */
  if (G_UNLIKELY (stream->fourcc == FOURCC_mp4s)) {
    /* send a one time dvd clut event */
    if (stream->pending_event && stream->pad)
      gst_pad_push_event (stream->pad, stream->pending_event);
    stream->pending_event = NULL;
    /* no further processing needed */
    stream->need_process = FALSE;
  }

  if (G_UNLIKELY (stream->subtype != FOURCC_text)) {
    return buf;
  }

  if (G_LIKELY (size >= 2)) {
    nsize = GST_READ_UINT16_BE (data);
    nsize = MIN (nsize, size - 2);
  }

  GST_LOG_OBJECT (qtdemux, "3GPP timed text subtitle: %d/%d", nsize, size);

  /* takes care of UTF-8 validation or UTF-16 recognition,
   * no other encoding expected */
  str = gst_tag_freeform_string_to_utf8 ((gchar *) data + 2, nsize, NULL);
  if (str) {
    gst_buffer_unref (buf);
    buf = gst_buffer_new ();
    GST_BUFFER_DATA (buf) = GST_BUFFER_MALLOCDATA (buf) = (guint8 *) str;
    GST_BUFFER_SIZE (buf) = strlen (str);
  } else {
    /* may be 0-size subtitle, which is also sent to keep pipeline going */
    GST_BUFFER_DATA (buf) = data + 2;
    GST_BUFFER_SIZE (buf) = nsize;
  }

  /* FIXME ? convert optional subsequent style info to markup */

  return buf;
}

/* Sets a buffer's attributes properly and pushes it downstream.
 * Also checks for additional actions and custom processing that may
 * need to be done first.
 */
static gboolean
gst_qtdemux_decorate_and_push_buffer (GstQTDemux * qtdemux,
    QtDemuxStream * stream, GstBuffer * buf,
    guint64 timestamp, guint64 duration, gboolean keyframe, guint64 position,
    guint64 byte_position)
{
  GstFlowReturn ret = GST_FLOW_OK;

  if (G_UNLIKELY (stream->fourcc == FOURCC_rtsp)) {
    gchar *url;

    url = g_strndup ((gchar *) GST_BUFFER_DATA (buf), GST_BUFFER_SIZE (buf));
    if (url != NULL && strlen (url) != 0) {
      /* we have RTSP redirect now */
      gst_element_post_message (GST_ELEMENT_CAST (qtdemux),
          gst_message_new_element (GST_OBJECT_CAST (qtdemux),
              gst_structure_new ("redirect",
                  "new-location", G_TYPE_STRING, url, NULL)));
      qtdemux->posted_redirect = TRUE;
    } else {
      GST_WARNING_OBJECT (qtdemux, "Redirect URI of stream is empty, not "
          "posting");
    }
    g_free (url);
  }

  /* position reporting */
  if (qtdemux->segment.rate >= 0) {
    gst_segment_set_last_stop (&qtdemux->segment, GST_FORMAT_TIME, position);
    gst_qtdemux_sync_streams (qtdemux);
  }

  if (G_UNLIKELY (!stream->pad)) {
    GST_DEBUG_OBJECT (qtdemux, "No output pad for stream, ignoring");
    gst_buffer_unref (buf);
    goto exit;
  }

  /* send out pending buffers */
  while (stream->buffers) {
    GstBuffer *buffer = (GstBuffer *) stream->buffers->data;

    if (G_UNLIKELY (stream->discont)) {
      GST_LOG_OBJECT (qtdemux, "marking discont buffer");
      GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);
      stream->discont = FALSE;
    }
    gst_buffer_set_caps (buffer, stream->caps);

    gst_pad_push (stream->pad, buffer);

    stream->buffers = g_slist_delete_link (stream->buffers, stream->buffers);
  }

  /* we're going to modify the metadata */
  buf = gst_buffer_make_metadata_writable (buf);

  if (G_UNLIKELY (stream->need_process))
    buf = gst_qtdemux_process_buffer (qtdemux, stream, buf);

  GST_BUFFER_TIMESTAMP (buf) = timestamp;
  GST_BUFFER_DURATION (buf) = duration;
  GST_BUFFER_OFFSET (buf) = -1;
  GST_BUFFER_OFFSET_END (buf) = -1;

  if (G_UNLIKELY (stream->padding)) {
    GST_BUFFER_DATA (buf) += stream->padding;
    GST_BUFFER_SIZE (buf) -= stream->padding;
  }

  if (G_UNLIKELY (qtdemux->element_index)) {
    GstClockTime stream_time;

    stream_time =
        gst_segment_to_stream_time (&stream->segment, GST_FORMAT_TIME,
        timestamp);
    if (GST_CLOCK_TIME_IS_VALID (stream_time)) {
      GST_LOG_OBJECT (qtdemux,
          "adding association %" GST_TIME_FORMAT "-> %"
          G_GUINT64_FORMAT, GST_TIME_ARGS (stream_time), byte_position);
      gst_index_add_association (qtdemux->element_index,
          qtdemux->index_id,
          keyframe ? GST_ASSOCIATION_FLAG_KEY_UNIT :
          GST_ASSOCIATION_FLAG_DELTA_UNIT, GST_FORMAT_TIME, stream_time,
          GST_FORMAT_BYTES, byte_position, NULL);
    }
  }

  if (stream->need_clip)
    buf = gst_qtdemux_clip_buffer (qtdemux, stream, buf);

  if (G_UNLIKELY (buf == NULL))
    goto exit;

  if (G_UNLIKELY (stream->discont)) {
    GST_LOG_OBJECT (qtdemux, "marking discont buffer");
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DISCONT);
    stream->discont = FALSE;
  }

  if (!keyframe)
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT);

  gst_buffer_set_caps (buf, stream->caps);

  GST_LOG_OBJECT (qtdemux,
      "Pushing buffer with time %" GST_TIME_FORMAT ", duration %"
      GST_TIME_FORMAT " on pad %s", GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
      GST_TIME_ARGS (GST_BUFFER_DURATION (buf)), GST_PAD_NAME (stream->pad));

  ret = gst_pad_push (stream->pad, buf);

exit:
  return ret;
}

static GstFlowReturn
gst_qtdemux_loop_state_movie (GstQTDemux * qtdemux)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstBuffer *buf = NULL;
  QtDemuxStream *stream;
  guint64 min_time;
  guint64 offset = 0;
  guint64 timestamp = GST_CLOCK_TIME_NONE;
  guint64 duration = 0;
  gboolean keyframe = FALSE;
  guint size = 0;
  gint index;
  gint i;

  gst_qtdemux_push_pending_newsegment (qtdemux);

  /* Figure out the next stream sample to output, min_time is expressed in
   * global time and runs over the edit list segments. */
  min_time = G_MAXUINT64;
  index = -1;
  for (i = 0; i < qtdemux->n_streams; i++) {
    guint64 position;

    stream = qtdemux->streams[i];
    position = stream->time_position;

    /* position of -1 is EOS */
    if (position != -1 && position < min_time) {
      min_time = position;
      index = i;
    }
  }
  /* all are EOS */
  if (G_UNLIKELY (index == -1)) {
    GST_DEBUG_OBJECT (qtdemux, "all streams are EOS");
    goto eos;
  }

  /* check for segment end */
  if (G_UNLIKELY (qtdemux->segment.stop != -1
          && qtdemux->segment.stop < min_time)) {
    GST_DEBUG_OBJECT (qtdemux, "we reached the end of our segment.");
    goto eos;
  }

  stream = qtdemux->streams[index];

  /* fetch info for the current sample of this stream */
  if (G_UNLIKELY (!gst_qtdemux_prepare_current_sample (qtdemux, stream, &offset,
              &size, &timestamp, &duration, &keyframe)))
    goto eos_stream;

  GST_LOG_OBJECT (qtdemux,
      "pushing from stream %d, offset %" G_GUINT64_FORMAT
      ", size %d, timestamp=%" GST_TIME_FORMAT ", duration %" GST_TIME_FORMAT,
      index, offset, size, GST_TIME_ARGS (timestamp), GST_TIME_ARGS (duration));

  /* hmm, empty sample, skip and move to next sample */
  if (G_UNLIKELY (size <= 0))
    goto next;

  /* last pushed sample was out of boundary, goto next sample */
  if (G_UNLIKELY (stream->last_ret == GST_FLOW_UNEXPECTED))
    goto next;

  GST_LOG_OBJECT (qtdemux, "reading %d bytes @ %" G_GUINT64_FORMAT, size,
      offset);

  ret = gst_qtdemux_pull_atom (qtdemux, offset, size, &buf);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto beach;

  ret = gst_qtdemux_decorate_and_push_buffer (qtdemux, stream, buf,
      timestamp, duration, keyframe, min_time, offset);

  /* combine flows */
  ret = gst_qtdemux_combine_flows (qtdemux, stream, ret);
  /* ignore unlinked, we will not push on the pad anymore and we will EOS when
   * we have no more data for the pad to push */
  if (ret == GST_FLOW_UNEXPECTED)
    ret = GST_FLOW_OK;

next:
  gst_qtdemux_advance_sample (qtdemux, stream);

beach:
  return ret;

  /* special cases */
eos:
  {
    GST_DEBUG_OBJECT (qtdemux, "No samples left for any streams - EOS");
    ret = GST_FLOW_UNEXPECTED;
    goto beach;
  }
eos_stream:
  {
    GST_DEBUG_OBJECT (qtdemux, "No samples left for stream");
    /* EOS will be raised if all are EOS */
    ret = GST_FLOW_OK;
    goto beach;
  }
}

static void
gst_qtdemux_loop (GstPad * pad)
{
  GstQTDemux *qtdemux;
  guint64 cur_offset;
  GstFlowReturn ret;

  qtdemux = GST_QTDEMUX (gst_pad_get_parent (pad));

  cur_offset = qtdemux->offset;
  GST_LOG_OBJECT (qtdemux, "loop at position %" G_GUINT64_FORMAT ", state %d",
      cur_offset, qtdemux->state);

  switch (qtdemux->state) {
    case QTDEMUX_STATE_INITIAL:
    case QTDEMUX_STATE_HEADER:
      ret = gst_qtdemux_loop_state_header (qtdemux);
      break;
    case QTDEMUX_STATE_MOVIE:
      ret = gst_qtdemux_loop_state_movie (qtdemux);
      if (qtdemux->segment.rate < 0 && ret == GST_FLOW_UNEXPECTED) {
        ret = gst_qtdemux_seek_to_previous_keyframe (qtdemux);
      }
      break;
    default:
      /* ouch */
      goto invalid_state;
  }

  /* if something went wrong, pause */
  if (ret != GST_FLOW_OK)
    goto pause;

done:
  gst_object_unref (qtdemux);
  return;

  /* ERRORS */
invalid_state:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, FAILED,
        (NULL), ("streaming stopped, invalid state"));
    qtdemux->segment_running = FALSE;
    gst_pad_pause_task (pad);
    gst_qtdemux_push_event (qtdemux, gst_event_new_eos ());
    goto done;
  }
pause:
  {
    const gchar *reason = gst_flow_get_name (ret);

    GST_LOG_OBJECT (qtdemux, "pausing task, reason %s", reason);

    qtdemux->segment_running = FALSE;
    gst_pad_pause_task (pad);

    /* fatal errors need special actions */
    /* check EOS */
    if (ret == GST_FLOW_UNEXPECTED) {
      if (qtdemux->n_streams == 0) {
        /* we have no streams, post an error */
        gst_qtdemux_post_no_playable_stream_error (qtdemux);
      }
      if (qtdemux->segment.flags & GST_SEEK_FLAG_SEGMENT) {
        gint64 stop;

        /* FIXME: I am not sure this is the right fix. If the sinks are
         * supposed to detect the segment is complete and accumulate
         * automatically, it does not seem to work here. Need more work */
        qtdemux->segment_running = TRUE;

        if ((stop = qtdemux->segment.stop) == -1)
          stop = qtdemux->segment.duration;

        if (qtdemux->segment.rate >= 0) {
          GST_LOG_OBJECT (qtdemux, "Sending segment done, at end of segment");
          gst_element_post_message (GST_ELEMENT_CAST (qtdemux),
              gst_message_new_segment_done (GST_OBJECT_CAST (qtdemux),
                  GST_FORMAT_TIME, stop));
        } else {
          /*  For Reverse Playback */
          GST_LOG_OBJECT (qtdemux, "Sending segment done, at start of segment");
          gst_element_post_message (GST_ELEMENT_CAST (qtdemux),
              gst_message_new_segment_done (GST_OBJECT_CAST (qtdemux),
                  GST_FORMAT_TIME, qtdemux->segment.start));
        }
      } else {
        GST_LOG_OBJECT (qtdemux, "Sending EOS at end of segment");
        gst_qtdemux_push_event (qtdemux, gst_event_new_eos ());
      }
    } else if (ret == GST_FLOW_NOT_LINKED || ret < GST_FLOW_UNEXPECTED) {
      GST_ELEMENT_ERROR (qtdemux, STREAM, FAILED,
          (NULL), ("streaming stopped, reason %s", reason));
      gst_qtdemux_push_event (qtdemux, gst_event_new_eos ());
    }
    goto done;
  }
}

/*
 * next_entry_size
 *
 * Returns the size of the first entry at the current offset.
 * If -1, there are none (which means EOS or empty file).
 */
static guint64
next_entry_size (GstQTDemux * demux)
{
  QtDemuxStream *stream;
  int i;
  int smallidx = -1;
  guint64 smalloffs = (guint64) - 1;
  QtDemuxSample *sample;

  GST_LOG_OBJECT (demux, "Finding entry at offset %" G_GUINT64_FORMAT,
      demux->offset);

  for (i = 0; i < demux->n_streams; i++) {
    stream = demux->streams[i];

    if (stream->sample_index == -1)
      stream->sample_index = 0;

    if (stream->sample_index >= stream->n_samples) {
      GST_LOG_OBJECT (demux, "stream %d samples exhausted", i);
      continue;
    }

    if (!qtdemux_parse_samples (demux, stream, stream->sample_index)) {
      GST_LOG_OBJECT (demux, "Parsing of index %u from stbl atom failed!",
          stream->sample_index);
      return -1;
    }

    sample = &stream->samples[stream->sample_index];

    GST_LOG_OBJECT (demux,
        "Checking Stream %d (sample_index:%d / offset:%" G_GUINT64_FORMAT
        " / size:%" G_GUINT32_FORMAT ")", i, stream->sample_index,
        sample->offset, sample->size);

    if (((smalloffs == -1)
            || (sample->offset < smalloffs)) && (sample->size)) {
      smallidx = i;
      smalloffs = sample->offset;
    }
  }

  GST_LOG_OBJECT (demux,
      "stream %d offset %" G_GUINT64_FORMAT " demux->offset :%"
      G_GUINT64_FORMAT, smallidx, smalloffs, demux->offset);

  if (smallidx == -1)
    return -1;

  stream = demux->streams[smallidx];
  sample = &stream->samples[stream->sample_index];

  if (sample->offset >= demux->offset) {
    demux->todrop = sample->offset - demux->offset;
    return sample->size + demux->todrop;
  }

  GST_DEBUG_OBJECT (demux,
      "There wasn't any entry at offset %" G_GUINT64_FORMAT, demux->offset);
  return -1;
}

static void
gst_qtdemux_post_progress (GstQTDemux * demux, gint num, gint denom)
{
  gint perc = (gint) ((gdouble) num * 100.0 / (gdouble) denom);

  gst_element_post_message (GST_ELEMENT_CAST (demux),
      gst_message_new_element (GST_OBJECT_CAST (demux),
          gst_structure_new ("progress", "percent", G_TYPE_INT, perc, NULL)));
}

static gboolean
qtdemux_seek_offset (GstQTDemux * demux, guint64 offset)
{
  GstEvent *event;
  gboolean res = 0;

  GST_DEBUG_OBJECT (demux, "Seeking to %" G_GUINT64_FORMAT, offset);

  event =
      gst_event_new_seek (1.0, GST_FORMAT_BYTES,
      GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE, GST_SEEK_TYPE_SET, offset,
      GST_SEEK_TYPE_NONE, -1);

  res = gst_pad_push_event (demux->sinkpad, event);

  return res;
}

/* FIXME, unverified after edit list updates */
static GstFlowReturn
gst_qtdemux_chain (GstPad * sinkpad, GstBuffer * inbuf)
{
  GstQTDemux *demux;
  GstFlowReturn ret = GST_FLOW_OK;

  demux = GST_QTDEMUX (gst_pad_get_parent (sinkpad));

  gst_adapter_push (demux->adapter, inbuf);

  /* we never really mean to buffer that much */
  if (demux->neededbytes == -1)
    goto eos;

  GST_DEBUG_OBJECT (demux, "pushing in inbuf %p, neededbytes:%u, available:%u",
      inbuf, demux->neededbytes, gst_adapter_available (demux->adapter));

  while (((gst_adapter_available (demux->adapter)) >= demux->neededbytes) &&
      (ret == GST_FLOW_OK)) {

    GST_DEBUG_OBJECT (demux,
        "state:%d , demux->neededbytes:%d, demux->offset:%" G_GUINT64_FORMAT,
        demux->state, demux->neededbytes, demux->offset);

    switch (demux->state) {
      case QTDEMUX_STATE_INITIAL:{
        const guint8 *data;
        guint32 fourcc;
        guint64 size;

        data = gst_adapter_peek (demux->adapter, demux->neededbytes);

        /* get fourcc/length, set neededbytes */
        extract_initial_length_and_fourcc ((guint8 *) data, demux->neededbytes,
            &size, &fourcc);
        GST_DEBUG_OBJECT (demux, "Peeking found [%" GST_FOURCC_FORMAT "] "
            "size: %" G_GUINT64_FORMAT, GST_FOURCC_ARGS (fourcc), size);
        if (size == 0) {
          GST_ELEMENT_ERROR (demux, STREAM, DEMUX,
              (_("This file is invalid and cannot be played.")),
              ("initial atom '%" GST_FOURCC_FORMAT "' has empty length",
                  GST_FOURCC_ARGS (fourcc)));
          ret = GST_FLOW_ERROR;
          break;
        }
        if (fourcc == FOURCC_mdat) {
          if (demux->n_streams > 0) {
            /* we have the headers, start playback */
            demux->state = QTDEMUX_STATE_MOVIE;
            demux->neededbytes = next_entry_size (demux);
            demux->mdatleft = size;

            /* Only post, event on pads is done after newsegment */
            qtdemux_post_global_tags (demux);

          } else {
            /* no headers yet, try to get them */
            guint bs;
            gboolean res;
            guint64 old, target;

          buffer_data:
            old = demux->offset;
            target = old + size;

            /* try to jump over the atom with a seek */
            res = qtdemux_seek_offset (demux, target);

            if (res) {
              GST_DEBUG_OBJECT (demux, "seek success");
              /* remember the offset fo the first mdat so we can seek back to it
               * after we have the headers */
              if (fourcc == FOURCC_mdat && demux->first_mdat == -1) {
                demux->first_mdat = old;
                GST_DEBUG_OBJECT (demux, "first mdat at %" G_GUINT64_FORMAT,
                    demux->first_mdat);
              }
              /* seek worked, continue reading */
              demux->offset = target;
              demux->neededbytes = 16;
              demux->state = QTDEMUX_STATE_INITIAL;
            } else {
              /* seek failed, need to buffer */
              demux->offset = old;
              GST_DEBUG_OBJECT (demux, "seek failed");
              /* there may be multiple mdat (or alike) buffers */
              /* sanity check */
              if (demux->mdatbuffer)
                bs = GST_BUFFER_SIZE (demux->mdatbuffer);
              else
                bs = 0;
              if (size + bs > 10 * (1 << 20))
                goto no_moov;
              demux->state = QTDEMUX_STATE_BUFFER_MDAT;
              demux->neededbytes = size;
              if (!demux->mdatbuffer)
                demux->mdatoffset = demux->offset;
            }
          }
        } else if (G_UNLIKELY (size > QTDEMUX_MAX_ATOM_SIZE)) {
          GST_ELEMENT_ERROR (demux, STREAM, DEMUX,
              (_("This file is invalid and cannot be played.")),
              ("atom %" GST_FOURCC_FORMAT " has bogus size %" G_GUINT64_FORMAT,
                  GST_FOURCC_ARGS (fourcc), size));
          ret = GST_FLOW_ERROR;
          break;
        } else {
          /* this means we already started buffering and still no moov header,
           * let's continue buffering everything till we get moov */
          if (demux->mdatbuffer && (fourcc != FOURCC_moov))
            goto buffer_data;
          demux->neededbytes = size;
          demux->state = QTDEMUX_STATE_HEADER;
        }
        break;
      }
      case QTDEMUX_STATE_HEADER:{
        const guint8 *data;
        guint32 fourcc;

        GST_DEBUG_OBJECT (demux, "In header");

        data = gst_adapter_peek (demux->adapter, demux->neededbytes);

        /* parse the header */
        extract_initial_length_and_fourcc (data, demux->neededbytes, NULL,
            &fourcc);
        if (fourcc == FOURCC_moov) {
          GST_DEBUG_OBJECT (demux, "Parsing [moov]");

          demux->got_moov = TRUE;

          /* prepare newsegment to send when streaming actually starts */
          if (!demux->pending_newsegment) {
            demux->pending_newsegment =
                gst_event_new_new_segment (FALSE, 1.0, GST_FORMAT_TIME,
                0, GST_CLOCK_TIME_NONE, 0);
          }

          qtdemux_parse_moov (demux, data, demux->neededbytes);
          qtdemux_node_dump (demux, demux->moov_node);
#ifdef GSTREAMER_LITE
          if (!qtdemux_parse_tree (demux))
          {
              g_node_destroy (demux->moov_node);
              demux->moov_node = NULL;
              ret = GST_FLOW_ERROR;
              goto done;
          }
#else
          qtdemux_parse_tree (demux);
#endif //GSTREAMER_LITE
          qtdemux_expose_streams (demux);

          g_node_destroy (demux->moov_node);
          demux->moov_node = NULL;
          GST_DEBUG_OBJECT (demux, "Finished parsing the header");
        } else if (fourcc == FOURCC_moof) {
          if (demux->got_moov && demux->fragmented) {
            GST_DEBUG_OBJECT (demux, "Parsing [moof]");
            if (!qtdemux_parse_moof (demux, data, demux->neededbytes,
                    demux->offset, NULL)) {
              ret = GST_FLOW_ERROR;
              goto done;
            }
          } else {
            GST_DEBUG_OBJECT (demux, "Discarding [moof]");
          }
        } else if (fourcc == FOURCC_ftyp) {
          GST_DEBUG_OBJECT (demux, "Parsing [ftyp]");
          qtdemux_parse_ftyp (demux, data, demux->neededbytes);
        } else if (fourcc == FOURCC_uuid) {
          GST_DEBUG_OBJECT (demux, "Parsing [uuid]");
          qtdemux_parse_uuid (demux, data, demux->neededbytes);
        } else {
          GST_WARNING_OBJECT (demux,
              "Unknown fourcc while parsing header : %" GST_FOURCC_FORMAT,
              GST_FOURCC_ARGS (fourcc));
          /* Let's jump that one and go back to initial state */
        }

        if (demux->mdatbuffer && demux->n_streams) {
          /* the mdat was before the header */
          GST_DEBUG_OBJECT (demux, "We have n_streams:%d and mdatbuffer:%p",
              demux->n_streams, demux->mdatbuffer);
          /* restore our adapter/offset view of things with upstream;
           * put preceding buffered data ahead of current moov data.
           * This should also handle evil mdat, moov, mdat cases and alike */
          gst_adapter_clear (demux->adapter);
          demux->mdatbuffer = NULL;
          demux->offset = demux->mdatoffset;
          demux->neededbytes = next_entry_size (demux);
          demux->state = QTDEMUX_STATE_MOVIE;
          demux->mdatleft = gst_adapter_available (demux->adapter);

          /* Only post, event on pads is done after newsegment */
          qtdemux_post_global_tags (demux);

        } else {
          GST_DEBUG_OBJECT (demux, "Carrying on normally");
          gst_adapter_flush (demux->adapter, demux->neededbytes);

          if (demux->got_moov && demux->first_mdat != -1) {
            gboolean res;

            /* we need to seek back */
            res = qtdemux_seek_offset (demux, demux->first_mdat);
            if (res) {
              demux->offset = demux->first_mdat;
            } else {
              GST_DEBUG_OBJECT (demux, "Seek back failed");
            }
          } else {
            demux->offset += demux->neededbytes;
          }
          demux->neededbytes = 16;
          demux->state = QTDEMUX_STATE_INITIAL;
        }

        break;
      }
      case QTDEMUX_STATE_BUFFER_MDAT:{
        GstBuffer *buf;

        GST_DEBUG_OBJECT (demux, "Got our buffer at offset %" G_GUINT64_FORMAT,
            demux->offset);
        buf = gst_adapter_take_buffer (demux->adapter, demux->neededbytes);
        GST_DEBUG_OBJECT (demux, "mdatbuffer starts with %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (QT_FOURCC (GST_BUFFER_DATA (buf) + 4)));
        if (demux->mdatbuffer)
          demux->mdatbuffer = gst_buffer_join (demux->mdatbuffer, buf);
        else
          demux->mdatbuffer = buf;
        demux->offset += demux->neededbytes;
        demux->neededbytes = 16;
        demux->state = QTDEMUX_STATE_INITIAL;
        gst_qtdemux_post_progress (demux, 1, 1);

        break;
      }
      case QTDEMUX_STATE_MOVIE:{
        GstBuffer *outbuf;
        QtDemuxStream *stream = NULL;
        QtDemuxSample *sample;
        int i = -1;
        guint64 timestamp, duration, position;
        gboolean keyframe;

        GST_DEBUG_OBJECT (demux,
            "BEGIN // in MOVIE for offset %" G_GUINT64_FORMAT, demux->offset);

        if (demux->fragmented) {
          GST_DEBUG_OBJECT (demux, "mdat remaining %" G_GUINT64_FORMAT,
              demux->mdatleft);
          if (G_LIKELY (demux->todrop < demux->mdatleft)) {
            /* if needed data starts within this atom,
             * then it should not exceed this atom */
            if (G_UNLIKELY (demux->neededbytes > demux->mdatleft)) {
              GST_ELEMENT_ERROR (demux, STREAM, DEMUX,
                  (_("This file is invalid and cannot be played.")),
                  ("sample data crosses atom boundary"));
              ret = GST_FLOW_ERROR;
              break;
            }
            demux->mdatleft -= demux->neededbytes;
          } else {
            GST_DEBUG_OBJECT (demux, "data atom emptied; resuming atom scan");
            /* so we are dropping more than left in this atom */
            demux->todrop -= demux->mdatleft;
            demux->neededbytes -= demux->mdatleft;
            demux->mdatleft = 0;
            /* need to resume atom parsing so we do not miss any other pieces */
            demux->state = QTDEMUX_STATE_INITIAL;
            demux->neededbytes = 16;
            break;
          }
        }

        if (demux->todrop) {
          GST_LOG_OBJECT (demux, "Dropping %d bytes", demux->todrop);
          gst_adapter_flush (demux->adapter, demux->todrop);
          demux->neededbytes -= demux->todrop;
          demux->offset += demux->todrop;
        }

        /* first buffer? */
        /* initial newsegment sent here after having added pads,
         * possible others in sink_event */
        if (G_UNLIKELY (demux->pending_newsegment)) {
          gst_qtdemux_push_event (demux, demux->pending_newsegment);
          demux->pending_newsegment = NULL;
          /* clear to send tags on all streams */
          for (i = 0; i < demux->n_streams; i++) {
            gst_qtdemux_push_tags (demux, demux->streams[i]);
          }
        }

        /* Figure out which stream this is packet belongs to */
        for (i = 0; i < demux->n_streams; i++) {
          stream = demux->streams[i];
          if (stream->sample_index >= stream->n_samples)
            continue;
          GST_LOG_OBJECT (demux,
              "Checking stream %d (sample_index:%d / offset:%" G_GUINT64_FORMAT
              " / size:%d)", i, stream->sample_index,
              stream->samples[stream->sample_index].offset,
              stream->samples[stream->sample_index].size);

          if (stream->samples[stream->sample_index].offset == demux->offset)
            break;
        }

        if (G_UNLIKELY (stream == NULL || i == demux->n_streams))
          goto unknown_stream;

        /* Put data in a buffer, set timestamps, caps, ... */
        outbuf = gst_adapter_take_buffer (demux->adapter, demux->neededbytes);
        GST_DEBUG_OBJECT (demux, "stream : %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (stream->fourcc));

        g_return_val_if_fail (outbuf != NULL, GST_FLOW_ERROR);

        sample = &stream->samples[stream->sample_index];

        position = QTSAMPLE_DTS (stream, sample);
        timestamp = QTSAMPLE_PTS (stream, sample);
        duration = QTSAMPLE_DUR_DTS (stream, sample, position);
        keyframe = QTSAMPLE_KEYFRAME (stream, sample);

        ret = gst_qtdemux_decorate_and_push_buffer (demux, stream, outbuf,
            timestamp, duration, keyframe, position, demux->offset);

        /* combine flows */
        ret = gst_qtdemux_combine_flows (demux, stream, ret);

        stream->sample_index++;

        /* update current offset and figure out size of next buffer */
        GST_LOG_OBJECT (demux, "increasing offset %" G_GUINT64_FORMAT " by %u",
            demux->offset, demux->neededbytes);
        demux->offset += demux->neededbytes;
        GST_LOG_OBJECT (demux, "offset is now %" G_GUINT64_FORMAT,
            demux->offset);

        if ((demux->neededbytes = next_entry_size (demux)) == -1) {
          if (demux->fragmented) {
            GST_DEBUG_OBJECT (demux, "(temporarily) out of fragmented samples");
            /* there may be more to follow, only finish this atom */
            demux->todrop = demux->mdatleft;
            demux->neededbytes = demux->todrop;
            break;
          }
          goto eos;
        }
        break;
      }
      default:
        goto invalid_state;
    }
  }

  /* when buffering movie data, at least show user something is happening */
  if (ret == GST_FLOW_OK && demux->state == QTDEMUX_STATE_BUFFER_MDAT &&
      gst_adapter_available (demux->adapter) <= demux->neededbytes) {
    gst_qtdemux_post_progress (demux, gst_adapter_available (demux->adapter),
        demux->neededbytes);
  }
done:
  gst_object_unref (demux);

  return ret;

  /* ERRORS */
unknown_stream:
  {
    GST_ELEMENT_ERROR (demux, STREAM, FAILED, (NULL), ("unknown stream found"));
    ret = GST_FLOW_ERROR;
    goto done;
  }
eos:
  {
    GST_DEBUG_OBJECT (demux, "no next entry, EOS");
    ret = GST_FLOW_UNEXPECTED;
    goto done;
  }
invalid_state:
  {
    GST_ELEMENT_ERROR (demux, STREAM, FAILED,
        (NULL), ("qtdemuxer invalid state %d", demux->state));
    ret = GST_FLOW_ERROR;
    goto done;
  }
no_moov:
  {
    GST_ELEMENT_ERROR (demux, STREAM, FAILED,
        (NULL), ("no 'moov' atom within the first 10 MB"));
    ret = GST_FLOW_ERROR;
    goto done;
  }
}

static gboolean
qtdemux_sink_activate (GstPad * sinkpad)
{
  if (gst_pad_check_pull_range (sinkpad))
    return gst_pad_activate_pull (sinkpad, TRUE);
  else
    return gst_pad_activate_push (sinkpad, TRUE);
}

static gboolean
qtdemux_sink_activate_pull (GstPad * sinkpad, gboolean active)
{
  GstQTDemux *demux = GST_QTDEMUX (GST_PAD_PARENT (sinkpad));

  if (active) {
    demux->pullbased = TRUE;
    demux->segment_running = TRUE;
    return gst_pad_start_task (sinkpad, (GstTaskFunction) gst_qtdemux_loop,
        sinkpad);
  } else {
    demux->segment_running = FALSE;
    return gst_pad_stop_task (sinkpad);
  }
}

static gboolean
qtdemux_sink_activate_push (GstPad * sinkpad, gboolean active)
{
  GstQTDemux *demux = GST_QTDEMUX (GST_PAD_PARENT (sinkpad));

  demux->pullbased = FALSE;

  return TRUE;
}

#ifdef HAVE_ZLIB
static void *
qtdemux_zalloc (void *opaque, unsigned int items, unsigned int size)
{
  return g_malloc (items * size);
}

static void
qtdemux_zfree (void *opaque, void *addr)
{
  g_free (addr);
}

static void *
qtdemux_inflate (void *z_buffer, guint z_length, guint length)
{
  guint8 *buffer;
  z_stream *z;
  int ret;

  z = g_new0 (z_stream, 1);
  z->zalloc = qtdemux_zalloc;
  z->zfree = qtdemux_zfree;
  z->opaque = NULL;

  z->next_in = z_buffer;
  z->avail_in = z_length;

  buffer = (guint8 *) g_malloc (length);
  ret = inflateInit (z);
  while (z->avail_in > 0) {
    if (z->avail_out == 0) {
      length += 1024;
      buffer = (guint8 *) g_realloc (buffer, length);
      z->next_out = buffer + z->total_out;
      z->avail_out = 1024;
    }
    ret = inflate (z, Z_SYNC_FLUSH);
    if (ret != Z_OK)
      break;
  }
  if (ret != Z_STREAM_END) {
    g_warning ("inflate() returned %d", ret);
  }

  g_free (z);
  return buffer;
}
#endif /* HAVE_ZLIB */

static gboolean
qtdemux_parse_moov (GstQTDemux * qtdemux, const guint8 * buffer, guint length)
{
  GNode *cmov;

  qtdemux->moov_node = g_node_new ((guint8 *) buffer);

  GST_DEBUG_OBJECT (qtdemux, "parsing 'moov' atom");
  qtdemux_parse_node (qtdemux, qtdemux->moov_node, buffer, length);

  cmov = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_cmov);
  if (cmov) {
    guint32 method;
    GNode *dcom;
    GNode *cmvd;

    dcom = qtdemux_tree_get_child_by_type (cmov, FOURCC_dcom);
    cmvd = qtdemux_tree_get_child_by_type (cmov, FOURCC_cmvd);
    if (dcom == NULL || cmvd == NULL)
      goto invalid_compression;

    method = QT_FOURCC ((guint8 *) dcom->data + 8);
    switch (method) {
#ifdef HAVE_ZLIB
      case GST_MAKE_FOURCC ('z', 'l', 'i', 'b'):{
        guint uncompressed_length;
        guint compressed_length;
        guint8 *buf;

        uncompressed_length = QT_UINT32 ((guint8 *) cmvd->data + 8);
        compressed_length = QT_UINT32 ((guint8 *) cmvd->data + 4) - 12;
        GST_LOG ("length = %u", uncompressed_length);

        buf =
            (guint8 *) qtdemux_inflate ((guint8 *) cmvd->data + 12,
            compressed_length, uncompressed_length);

        qtdemux->moov_node_compressed = qtdemux->moov_node;
        qtdemux->moov_node = g_node_new (buf);

        qtdemux_parse_node (qtdemux, qtdemux->moov_node, buf,
            uncompressed_length);
        break;
      }
#endif /* HAVE_ZLIB */
      default:
        GST_WARNING_OBJECT (qtdemux, "unknown or unhandled header compression "
            "type %" GST_FOURCC_FORMAT, GST_FOURCC_ARGS (method));
        break;
    }
  }
  return TRUE;

  /* ERRORS */
invalid_compression:
  {
    GST_ERROR_OBJECT (qtdemux, "invalid compressed header");
    return FALSE;
  }
}

static gboolean
qtdemux_parse_container (GstQTDemux * qtdemux, GNode * node, const guint8 * buf,
    const guint8 * end)
{
  while (G_UNLIKELY (buf < end)) {
    GNode *child;
    guint32 len;

    if (G_UNLIKELY (buf + 4 > end)) {
      GST_LOG_OBJECT (qtdemux, "buffer overrun");
      break;
    }
    len = QT_UINT32 (buf);
    if (G_UNLIKELY (len == 0)) {
      GST_LOG_OBJECT (qtdemux, "empty container");
      break;
    }
    if (G_UNLIKELY (len < 8)) {
      GST_WARNING_OBJECT (qtdemux, "length too short (%d < 8)", len);
      break;
    }
    if (G_UNLIKELY (len > (end - buf))) {
      GST_WARNING_OBJECT (qtdemux, "length too long (%d > %d)", len,
          (gint) (end - buf));
      break;
    }

    child = g_node_new ((guint8 *) buf);
    g_node_append (node, child);
    GST_LOG_OBJECT (qtdemux, "adding new node of len %d", len);
    qtdemux_parse_node (qtdemux, child, buf, len);

    buf += len;
  }
  return TRUE;
}

static gboolean
qtdemux_parse_theora_extension (GstQTDemux * qtdemux, QtDemuxStream * stream,
    GNode * xdxt)
{
  int len = QT_UINT32 (xdxt->data);
  guint8 *buf = xdxt->data;
  guint8 *end = buf + len;
  GstBuffer *buffer;

  /* skip size and type */
  buf += 8;
  end -= 8;

  while (buf < end) {
    gint size;
    guint32 type;

    size = QT_UINT32 (buf);
    type = QT_FOURCC (buf + 4);

    GST_LOG_OBJECT (qtdemux, "%p %p", buf, end);

    if (buf + size > end || size <= 0)
      break;

    buf += 8;
    size -= 8;

    GST_WARNING_OBJECT (qtdemux, "have cookie %" GST_FOURCC_FORMAT,
        GST_FOURCC_ARGS (type));

    switch (type) {
      case FOURCC_tCtH:
        buffer = gst_buffer_new_and_alloc (size);
        memcpy (GST_BUFFER_DATA (buffer), buf, size);
        stream->buffers = g_slist_append (stream->buffers, buffer);
        GST_LOG_OBJECT (qtdemux, "parsing theora header");
        break;
      case FOURCC_tCt_:
        buffer = gst_buffer_new_and_alloc (size);
        memcpy (GST_BUFFER_DATA (buffer), buf, size);
        stream->buffers = g_slist_append (stream->buffers, buffer);
        GST_LOG_OBJECT (qtdemux, "parsing theora comment");
        break;
      case FOURCC_tCtC:
        buffer = gst_buffer_new_and_alloc (size);
        memcpy (GST_BUFFER_DATA (buffer), buf, size);
        stream->buffers = g_slist_append (stream->buffers, buffer);
        GST_LOG_OBJECT (qtdemux, "parsing theora codebook");
        break;
      default:
        GST_WARNING_OBJECT (qtdemux,
            "unknown theora cookie %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (type));
        break;
    }
    buf += size;
  }
  return TRUE;
}

static gboolean
qtdemux_parse_node (GstQTDemux * qtdemux, GNode * node, const guint8 * buffer,
    guint length)
{
  guint32 fourcc = 0;
  guint32 node_length = 0;
  const QtNodeType *type;
  const guint8 *end;

  GST_LOG_OBJECT (qtdemux, "qtdemux_parse buffer %p length %u", buffer, length);

  if (G_UNLIKELY (length < 8))
    goto not_enough_data;

  node_length = QT_UINT32 (buffer);
  fourcc = QT_FOURCC (buffer + 4);

  /* ignore empty nodes */
  if (G_UNLIKELY (fourcc == 0 || node_length == 8))
    return TRUE;

  type = qtdemux_type_get (fourcc);

  end = buffer + length;

  GST_LOG_OBJECT (qtdemux,
      "parsing '%" GST_FOURCC_FORMAT "', length=%u, name '%s'",
      GST_FOURCC_ARGS (fourcc), node_length, type->name);

  if (node_length > length)
    goto broken_atom_size;

  if (type->flags & QT_FLAG_CONTAINER) {
    qtdemux_parse_container (qtdemux, node, buffer + 8, end);
  } else {
    switch (fourcc) {
      case FOURCC_stsd:
      {
        if (node_length < 20) {
          GST_LOG_OBJECT (qtdemux, "skipping small stsd box");
          break;
        }
        GST_DEBUG_OBJECT (qtdemux,
            "parsing stsd (sample table, sample description) atom");
        qtdemux_parse_container (qtdemux, node, buffer + 16, end);
        break;
      }
      case FOURCC_mp4a:
      case FOURCC_alac:
      {
        guint32 version;
        guint32 offset;
        guint min_size;

        /* also read alac (or whatever) in stead of mp4a in the following,
         * since a similar layout is used in other cases as well */
        if (fourcc == FOURCC_mp4a)
          min_size = 20;
        else
          min_size = 40;

        /* There are two things we might encounter here: a true mp4a atom, and
           an mp4a entry in an stsd atom. The latter is what we're interested
           in, and it looks like an atom, but isn't really one. The true mp4a
           atom is short, so we detect it based on length here. */
        if (length < min_size) {
          GST_LOG_OBJECT (qtdemux, "skipping small %" GST_FOURCC_FORMAT " box",
              GST_FOURCC_ARGS (fourcc));
          break;
        }

        /* 'version' here is the sound sample description version. Types 0 and
           1 are documented in the QTFF reference, but type 2 is not: it's
           described in Apple header files instead (struct SoundDescriptionV2
           in Movies.h) */
        version = QT_UINT16 (buffer + 16);

        GST_DEBUG_OBJECT (qtdemux, "%" GST_FOURCC_FORMAT " version 0x%08x",
            GST_FOURCC_ARGS (fourcc), version);

        /* parse any esds descriptors */
        switch (version) {
          case 0:
            offset = 0x24;
            break;
          case 1:
            offset = 0x34;
            break;
          case 2:
            offset = 0x48;
            break;
          default:
            GST_WARNING_OBJECT (qtdemux,
                "unhandled %" GST_FOURCC_FORMAT " version 0x%08x",
                GST_FOURCC_ARGS (fourcc), version);
            offset = 0;
            break;
        }
        if (offset)
          qtdemux_parse_container (qtdemux, node, buffer + offset, end);
        break;
      }
      case FOURCC_mp4v:
      case FOURCC_MP4V:
      case FOURCC_fmp4:
      case FOURCC_FMP4:
      {
        const guint8 *buf;
        guint32 version;
        int tlen;

        GST_DEBUG_OBJECT (qtdemux, "parsing in %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (fourcc));
        version = QT_UINT32 (buffer + 16);
        GST_DEBUG_OBJECT (qtdemux, "version %08x", version);
        if (1 || version == 0x00000000) {
          buf = buffer + 0x32;

          /* FIXME Quicktime uses PASCAL string while
           * the iso format uses C strings. Check the file
           * type before attempting to parse the string here. */
          tlen = QT_UINT8 (buf);
          GST_DEBUG_OBJECT (qtdemux, "tlen = %d", tlen);
          buf++;
          GST_DEBUG_OBJECT (qtdemux, "string = %.*s", tlen, (char *) buf);
          /* the string has a reserved space of 32 bytes so skip
           * the remaining 31 */
          buf += 31;
          buf += 4;             /* and 4 bytes reserved */

          GST_MEMDUMP_OBJECT (qtdemux, "mp4v", buf, end - buf);

          qtdemux_parse_container (qtdemux, node, buf, end);
        }
        break;
      }
      case FOURCC_avc1:
      {
        GST_MEMDUMP_OBJECT (qtdemux, "avc1", buffer, end - buffer);
        qtdemux_parse_container (qtdemux, node, buffer + 0x56, end);
        break;
      }
      case FOURCC_mjp2:
      {
        qtdemux_parse_container (qtdemux, node, buffer + 86, end);
        break;
      }
      case FOURCC_meta:
      {
        GST_DEBUG_OBJECT (qtdemux, "parsing meta atom");
        qtdemux_parse_container (qtdemux, node, buffer + 12, end);
        break;
      }
      case FOURCC_XiTh:
      {
        guint32 version;
        guint32 offset;

        version = QT_UINT32 (buffer + 12);
        GST_DEBUG_OBJECT (qtdemux, "parsing XiTh atom version 0x%08x", version);

        switch (version) {
          case 0x00000001:
            offset = 0x62;
            break;
          default:
            GST_DEBUG_OBJECT (qtdemux, "unknown version 0x%08x", version);
            offset = 0;
            break;
        }
        if (offset)
          qtdemux_parse_container (qtdemux, node, buffer + offset, end);
        break;
      }
      case FOURCC_in24:
      {
        qtdemux_parse_container (qtdemux, node, buffer + 0x34, end);
        break;
      }
      default:
        if (!strcmp (type->name, "unknown"))
          GST_MEMDUMP ("Unknown tag", buffer + 4, end - buffer - 4);
        break;
    }
  }
  GST_LOG_OBJECT (qtdemux, "parsed '%" GST_FOURCC_FORMAT "'",
      GST_FOURCC_ARGS (fourcc));
  return TRUE;

/* ERRORS */
not_enough_data:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")),
        ("Not enough data for an atom header, got only %u bytes", length));
    return FALSE;
  }
broken_atom_size:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")),
        ("Atom '%" GST_FOURCC_FORMAT "' has size of %u bytes, but we have only "
            "%u bytes available.", GST_FOURCC_ARGS (fourcc), node_length,
            length));
    return FALSE;
  }
}

static GNode *
qtdemux_tree_get_child_by_type (GNode * node, guint32 fourcc)
{
  GNode *child;
  guint8 *buffer;
  guint32 child_fourcc;

  for (child = g_node_first_child (node); child;
      child = g_node_next_sibling (child)) {
    buffer = (guint8 *) child->data;

    child_fourcc = QT_FOURCC (buffer + 4);

    if (G_UNLIKELY (child_fourcc == fourcc)) {
      return child;
    }
  }
  return NULL;
}

static GNode *
qtdemux_tree_get_child_by_type_full (GNode * node, guint32 fourcc,
    GstByteReader * parser)
{
  GNode *child;
  guint8 *buffer;
  guint32 child_fourcc, child_len;

  for (child = g_node_first_child (node); child;
      child = g_node_next_sibling (child)) {
    buffer = (guint8 *) child->data;

    child_len = QT_UINT32 (buffer);
    child_fourcc = QT_FOURCC (buffer + 4);

    if (G_UNLIKELY (child_fourcc == fourcc)) {
      if (G_UNLIKELY (child_len < (4 + 4)))
        return NULL;
      /* FIXME: must verify if atom length < parent atom length */
      gst_byte_reader_init (parser, buffer + (4 + 4), child_len - (4 + 4));
      return child;
    }
  }
  return NULL;
}

static GNode *
qtdemux_tree_get_sibling_by_type_full (GNode * node, guint32 fourcc,
    GstByteReader * parser)
{
  GNode *child;
  guint8 *buffer;
  guint32 child_fourcc, child_len;

  for (child = g_node_next_sibling (node); child;
      child = g_node_next_sibling (child)) {
    buffer = (guint8 *) child->data;

    child_fourcc = QT_FOURCC (buffer + 4);

    if (child_fourcc == fourcc) {
      if (parser) {
        child_len = QT_UINT32 (buffer);
        if (G_UNLIKELY (child_len < (4 + 4)))
          return NULL;
        /* FIXME: must verify if atom length < parent atom length */
        gst_byte_reader_init (parser, buffer + (4 + 4), child_len - (4 + 4));
      }
      return child;
    }
  }
  return NULL;
}

static GNode *
qtdemux_tree_get_sibling_by_type (GNode * node, guint32 fourcc)
{
  return qtdemux_tree_get_sibling_by_type_full (node, fourcc, NULL);
}

static gboolean
gst_qtdemux_add_stream (GstQTDemux * qtdemux,
    QtDemuxStream * stream, GstTagList * list)
{
  /* consistent default for push based mode */
  gst_segment_init (&stream->segment, GST_FORMAT_TIME);
  gst_segment_set_newsegment (&stream->segment, FALSE, 1.0, GST_FORMAT_TIME,
      0, GST_CLOCK_TIME_NONE, 0);

  if (stream->subtype == FOURCC_vide) {
    gchar *name = g_strdup_printf ("video_%02d", qtdemux->n_video_streams);

    stream->pad =
        gst_pad_new_from_static_template (&gst_qtdemux_videosrc_template, name);
    g_free (name);

    /* fps is calculated base on the duration of the first frames since
     * qt does not have a fixed framerate. */
    if ((stream->n_samples == 1) && (stream->min_duration == 0)) {
      /* still frame */
      stream->fps_n = 0;
      stream->fps_d = 1;
    } else {
      stream->fps_n = stream->timescale;
      if (stream->min_duration == 0)
        stream->fps_d = 1;
      else
        stream->fps_d = stream->min_duration;
    }

    if (stream->caps) {
      gboolean gray;
      gint depth, palette_count;
      const guint32 *palette_data = NULL;

      gst_caps_set_simple (stream->caps,
          "width", G_TYPE_INT, stream->width,
          "height", G_TYPE_INT, stream->height,
          "framerate", GST_TYPE_FRACTION, stream->fps_n, stream->fps_d, NULL);

      /* calculate pixel-aspect-ratio using display width and height */
      GST_DEBUG_OBJECT (qtdemux,
          "video size %dx%d, target display size %dx%d", stream->width,
          stream->height, stream->display_width, stream->display_height);

      if (stream->display_width > 0 && stream->display_height > 0 &&
          stream->width > 0 && stream->height > 0) {
        gint n, d;

        /* calculate the pixel aspect ratio using the display and pixel w/h */
        n = stream->display_width * stream->height;
        d = stream->display_height * stream->width;
        if (n == d)
          n = d = 1;
        GST_DEBUG_OBJECT (qtdemux, "setting PAR to %d/%d", n, d);
        gst_caps_set_simple (stream->caps, "pixel-aspect-ratio",
            GST_TYPE_FRACTION, n, d, NULL);
      }

      /* qt file might have pasp atom */
      if (stream->par_w > 0 && stream->par_h > 0) {
        GST_DEBUG_OBJECT (qtdemux, "par %d:%d", stream->par_w, stream->par_h);
        gst_caps_set_simple (stream->caps, "pixel-aspect-ratio",
            GST_TYPE_FRACTION, stream->par_w, stream->par_h, NULL);
      }

      depth = stream->bits_per_sample;

      /* more than 32 bits means grayscale */
      gray = (depth > 32);
      /* low 32 bits specify the depth  */
      depth &= 0x1F;

      /* different number of palette entries is determined by depth. */
      palette_count = 0;
      if ((depth == 1) || (depth == 2) || (depth == 4) || (depth == 8))
        palette_count = (1 << depth);

      switch (palette_count) {
        case 0:
          break;
        case 2:
          palette_data = ff_qt_default_palette_2;
          break;
        case 4:
          palette_data = ff_qt_default_palette_4;
          break;
        case 16:
          if (gray)
            palette_data = ff_qt_grayscale_palette_16;
          else
            palette_data = ff_qt_default_palette_16;
          break;
        case 256:
          if (gray)
            palette_data = ff_qt_grayscale_palette_256;
          else
            palette_data = ff_qt_default_palette_256;
          break;
        default:
          GST_ELEMENT_WARNING (qtdemux, STREAM, DEMUX,
              (_("The video in this file might not play correctly.")),
              ("unsupported palette depth %d", depth));
          break;
      }
      if (palette_data) {
        GstBuffer *palette;

        /* make sure it's not writable. We leave MALLOCDATA to NULL so that we
         * don't free any of the buffer data. */
        palette = gst_buffer_new ();
        GST_BUFFER_FLAG_SET (palette, GST_BUFFER_FLAG_READONLY);
        GST_BUFFER_DATA (palette) = (guint8 *) palette_data;
        GST_BUFFER_SIZE (palette) = sizeof (guint32) * palette_count;

        gst_caps_set_simple (stream->caps, "palette_data",
            GST_TYPE_BUFFER, palette, NULL);
        gst_buffer_unref (palette);
      } else if (palette_count != 0) {
        GST_ELEMENT_WARNING (qtdemux, STREAM, NOT_IMPLEMENTED,
            (NULL), ("Unsupported palette depth %d. Ignoring stream.", depth));

        gst_object_unref (stream->pad);
        stream->pad = NULL;
      }
    }
    qtdemux->n_video_streams++;
  } else if (stream->subtype == FOURCC_soun) {
    gchar *name = g_strdup_printf ("audio_%02d", qtdemux->n_audio_streams);

    stream->pad =
        gst_pad_new_from_static_template (&gst_qtdemux_audiosrc_template, name);
    g_free (name);
    if (stream->caps) {
      gst_caps_set_simple (stream->caps,
          "rate", G_TYPE_INT, (int) stream->rate,
          "channels", G_TYPE_INT, stream->n_channels, NULL);
    }
    qtdemux->n_audio_streams++;
  } else if (stream->subtype == FOURCC_strm) {
    GST_DEBUG_OBJECT (qtdemux, "stream type, not creating pad");
  } else if (stream->subtype == FOURCC_subp || stream->subtype == FOURCC_text) {
    gchar *name = g_strdup_printf ("subtitle_%02d", qtdemux->n_sub_streams);

    stream->pad =
        gst_pad_new_from_static_template (&gst_qtdemux_subsrc_template, name);
    g_free (name);
    qtdemux->n_sub_streams++;
  } else {
    GST_DEBUG_OBJECT (qtdemux, "unknown stream type");
    goto done;
  }

  if (stream->pad) {
    GST_PAD_ELEMENT_PRIVATE (stream->pad) = stream;

#ifdef GSTREAMER_LITE
    // Add track_id and track_enabled so we can find this pad later on by track ID
    if (stream->caps) {
      gst_caps_set_simple(stream->caps,
          "track_id", G_TYPE_INT, stream->track_id,
          "track_enabled", G_TYPE_BOOLEAN, stream->track_enabled,
          NULL);
	}
#endif // GSTREAMER_LITE

    gst_pad_use_fixed_caps (stream->pad);
    gst_pad_set_event_function (stream->pad, gst_qtdemux_handle_src_event);
    gst_pad_set_query_type_function (stream->pad,
        gst_qtdemux_get_src_query_types);
    gst_pad_set_query_function (stream->pad, gst_qtdemux_handle_src_query);

    GST_DEBUG_OBJECT (qtdemux, "setting caps %" GST_PTR_FORMAT, stream->caps);
    gst_pad_set_caps (stream->pad, stream->caps);

    GST_DEBUG_OBJECT (qtdemux, "adding pad %s %p to qtdemux %p",
        GST_OBJECT_NAME (stream->pad), stream->pad, qtdemux);
    gst_pad_set_active (stream->pad, TRUE);
    gst_element_add_pad (GST_ELEMENT_CAST (qtdemux), stream->pad);
    if (stream->pending_tags)
      gst_tag_list_free (stream->pending_tags);
    stream->pending_tags = list;
    if (list) {
      /* post now, send event on pad later */
      GST_DEBUG_OBJECT (qtdemux, "Posting tags %" GST_PTR_FORMAT, list);
      gst_element_post_message (GST_ELEMENT (qtdemux),
          gst_message_new_tag_full (GST_OBJECT (qtdemux), stream->pad,
              gst_tag_list_copy (list)));
    }
    /* global tags go on each pad anyway */
    stream->send_global_tags = TRUE;
  }
done:
  return TRUE;
}

/* find next atom with @fourcc starting at @offset */
static GstFlowReturn
qtdemux_find_atom (GstQTDemux * qtdemux, guint64 * offset,
    guint64 * length, guint32 fourcc)
{
  GstFlowReturn ret;
  guint32 lfourcc;
  GstBuffer *buf;

  GST_LOG_OBJECT (qtdemux, "finding fourcc %" GST_FOURCC_FORMAT " at offset %"
      G_GUINT64_FORMAT, GST_FOURCC_ARGS (fourcc), *offset);

  while (TRUE) {
    ret = gst_pad_pull_range (qtdemux->sinkpad, *offset, 16, &buf);
    if (G_UNLIKELY (ret != GST_FLOW_OK))
      goto locate_failed;
    if (G_LIKELY (GST_BUFFER_SIZE (buf) != 16)) {
      /* likely EOF */
      ret = GST_FLOW_UNEXPECTED;
      gst_buffer_unref (buf);
      goto locate_failed;
    }
    extract_initial_length_and_fourcc (GST_BUFFER_DATA (buf), 16, length,
        &lfourcc);
    gst_buffer_unref (buf);

    if (G_UNLIKELY (*length == 0)) {
      GST_DEBUG_OBJECT (qtdemux, "invalid length 0");
      ret = GST_FLOW_ERROR;
      goto locate_failed;
    }

    if (lfourcc == fourcc) {
      GST_DEBUG_OBJECT (qtdemux, "found fourcc at offset %" G_GUINT64_FORMAT,
          *offset);
      break;
    } else {
      GST_LOG_OBJECT (qtdemux,
          "skipping atom '%" GST_FOURCC_FORMAT "' at %" G_GUINT64_FORMAT,
          GST_FOURCC_ARGS (fourcc), *offset);
      *offset += *length;
    }
  }

  return GST_FLOW_OK;

locate_failed:
  {
    /* might simply have had last one */
    GST_DEBUG_OBJECT (qtdemux, "fourcc not found");
    return ret;
  }
}

/* should only do something in pull mode */
/* call with OBJECT lock */
static GstFlowReturn
qtdemux_add_fragmented_samples (GstQTDemux * qtdemux)
{
  guint64 length, offset;
  GstBuffer *buf = NULL;
  GstFlowReturn ret = GST_FLOW_OK;
  GstFlowReturn res = TRUE;

  offset = qtdemux->moof_offset;
  GST_DEBUG_OBJECT (qtdemux, "next moof at offset %" G_GUINT64_FORMAT, offset);

  if (!offset) {
    GST_DEBUG_OBJECT (qtdemux, "no next moof");
    return GST_FLOW_UNEXPECTED;
  }

  /* best not do pull etc with lock held */
  GST_OBJECT_UNLOCK (qtdemux);

  ret = qtdemux_find_atom (qtdemux, &offset, &length, FOURCC_moof);
  if (ret != GST_FLOW_OK)
    goto flow_failed;

  ret = gst_qtdemux_pull_atom (qtdemux, offset, length, &buf);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto flow_failed;
  if (!qtdemux_parse_moof (qtdemux, GST_BUFFER_DATA (buf),
          GST_BUFFER_SIZE (buf), offset, NULL)) {
    gst_buffer_unref (buf);
    buf = NULL;
    goto parse_failed;
  }

  gst_buffer_unref (buf);
  buf = NULL;

  offset += length;
  /* look for next moof */
  ret = qtdemux_find_atom (qtdemux, &offset, &length, FOURCC_moof);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto flow_failed;

exit:
  GST_OBJECT_LOCK (qtdemux);

  qtdemux->moof_offset = offset;

  return res;

parse_failed:
  {
    GST_DEBUG_OBJECT (qtdemux, "failed to parse moof");
    offset = 0;
    res = GST_FLOW_ERROR;
    goto exit;
  }
flow_failed:
  {
    /* maybe upstream temporarily flushing */
    if (ret != GST_FLOW_WRONG_STATE) {
      GST_DEBUG_OBJECT (qtdemux, "no next moof");
      offset = 0;
    } else {
      GST_DEBUG_OBJECT (qtdemux, "upstream WRONG_STATE");
      /* resume at current position next time */
    }
    res = ret;
    goto exit;
  }
}

/* initialise bytereaders for stbl sub-atoms */
static gboolean
qtdemux_stbl_init (GstQTDemux * qtdemux, QtDemuxStream * stream, GNode * stbl)
{
  stream->stbl_index = -1;      /* no samples have yet been parsed */

  /* time-to-sample atom */
  if (!qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stts, &stream->stts))
    goto corrupt_file;

  /* copy atom data into a new buffer for later use */
  stream->stts.data = g_memdup (stream->stts.data, stream->stts.size);

  /* skip version + flags */
  if (!gst_byte_reader_skip (&stream->stts, 1 + 3) ||
      !gst_byte_reader_get_uint32_be (&stream->stts, &stream->n_sample_times))
    goto corrupt_file;
  GST_LOG_OBJECT (qtdemux, "%u timestamp blocks", stream->n_sample_times);

  /* make sure there's enough data */
  if (!qt_atom_parser_has_chunks (&stream->stts, stream->n_sample_times, 2 * 4))
    goto corrupt_file;

  /* sync sample atom */
  stream->stps_present = FALSE;
  if ((stream->stss_present =
          ! !qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stss,
              &stream->stss) ? TRUE : FALSE) == TRUE) {
    /* copy atom data into a new buffer for later use */
    stream->stss.data = g_memdup (stream->stss.data, stream->stss.size);

    /* skip version + flags */
    if (!gst_byte_reader_skip (&stream->stss, 1 + 3) ||
        !gst_byte_reader_get_uint32_be (&stream->stss, &stream->n_sample_syncs))
      goto corrupt_file;

    if (stream->n_sample_syncs) {
      /* make sure there's enough data */
      if (!qt_atom_parser_has_chunks (&stream->stss, stream->n_sample_syncs, 4))
        goto corrupt_file;
    }

    /* partial sync sample atom */
    if ((stream->stps_present =
            ! !qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stps,
                &stream->stps) ? TRUE : FALSE) == TRUE) {
      /* copy atom data into a new buffer for later use */
      stream->stps.data = g_memdup (stream->stps.data, stream->stps.size);

      /* skip version + flags */
      if (!gst_byte_reader_skip (&stream->stps, 1 + 3) ||
          !gst_byte_reader_get_uint32_be (&stream->stps,
              &stream->n_sample_partial_syncs))
        goto corrupt_file;

      /* if there are no entries, the stss table contains the real
       * sync samples */
      if (stream->n_sample_partial_syncs) {
        /* make sure there's enough data */
        if (!qt_atom_parser_has_chunks (&stream->stps,
                stream->n_sample_partial_syncs, 4))
          goto corrupt_file;
      }
    }
  }

  /* sample size */
  if (!qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stsz, &stream->stsz))
    goto no_samples;

  /* copy atom data into a new buffer for later use */
  stream->stsz.data = g_memdup (stream->stsz.data, stream->stsz.size);

  /* skip version + flags */
  if (!gst_byte_reader_skip (&stream->stsz, 1 + 3) ||
      !gst_byte_reader_get_uint32_be (&stream->stsz, &stream->sample_size))
    goto corrupt_file;

  if (!gst_byte_reader_get_uint32_be (&stream->stsz, &stream->n_samples))
    goto corrupt_file;

  if (!stream->n_samples)
    goto no_samples;

  /* sample-to-chunk atom */
  if (!qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stsc, &stream->stsc))
    goto corrupt_file;

  /* copy atom data into a new buffer for later use */
  stream->stsc.data = g_memdup (stream->stsc.data, stream->stsc.size);

  /* skip version + flags */
  if (!gst_byte_reader_skip (&stream->stsc, 1 + 3) ||
      !gst_byte_reader_get_uint32_be (&stream->stsc,
          &stream->n_samples_per_chunk))
    goto corrupt_file;

  GST_DEBUG_OBJECT (qtdemux, "n_samples_per_chunk %u",
      stream->n_samples_per_chunk);

  /* make sure there's enough data */
  if (!qt_atom_parser_has_chunks (&stream->stsc, stream->n_samples_per_chunk,
          12))
    goto corrupt_file;


  /* chunk offset */
  if (qtdemux_tree_get_child_by_type_full (stbl, FOURCC_stco, &stream->stco))
    stream->co_size = sizeof (guint32);
  else if (qtdemux_tree_get_child_by_type_full (stbl, FOURCC_co64,
          &stream->stco))
    stream->co_size = sizeof (guint64);
  else
    goto corrupt_file;

  /* copy atom data into a new buffer for later use */
  stream->stco.data = g_memdup (stream->stco.data, stream->stco.size);

  /* skip version + flags */
  if (!gst_byte_reader_skip (&stream->stco, 1 + 3))
    goto corrupt_file;

  /* chunks_are_chunks == 0 means treat chunks as samples */
  stream->chunks_are_chunks = !stream->sample_size || stream->sampled;
  if (stream->chunks_are_chunks) {
    /* skip number of entries */
    if (!gst_byte_reader_skip (&stream->stco, 4))
      goto corrupt_file;

    /* make sure there are enough data in the stsz atom */
    if (!stream->sample_size) {
      /* different sizes for each sample */
      if (!qt_atom_parser_has_chunks (&stream->stsz, stream->n_samples, 4))
        goto corrupt_file;
    }
  } else {
    /* treat chunks as samples */
    if (!gst_byte_reader_get_uint32_be (&stream->stco, &stream->n_samples))
      goto corrupt_file;
  }

  GST_DEBUG_OBJECT (qtdemux, "allocating n_samples %u * %u (%.2f MB)",
      stream->n_samples, (guint) sizeof (QtDemuxSample),
      stream->n_samples * sizeof (QtDemuxSample) / (1024.0 * 1024.0));

  if (stream->n_samples >=
      QTDEMUX_MAX_SAMPLE_INDEX_SIZE / sizeof (QtDemuxSample)) {
    GST_WARNING_OBJECT (qtdemux, "not allocating index of %d samples, would "
        "be larger than %uMB (broken file?)", stream->n_samples,
        QTDEMUX_MAX_SAMPLE_INDEX_SIZE >> 20);
    return FALSE;
  }

  stream->samples = g_try_new0 (QtDemuxSample, stream->n_samples);
  if (!stream->samples) {
    GST_WARNING_OBJECT (qtdemux, "failed to allocate %d samples",
        stream->n_samples);
    return FALSE;
  }


  /* composition time-to-sample */
  if ((stream->ctts_present =
          ! !qtdemux_tree_get_child_by_type_full (stbl, FOURCC_ctts,
              &stream->ctts) ? TRUE : FALSE) == TRUE) {
    /* copy atom data into a new buffer for later use */
    stream->ctts.data = g_memdup (stream->ctts.data, stream->ctts.size);

    /* skip version + flags */
    if (!gst_byte_reader_skip (&stream->ctts, 1 + 3)
        || !gst_byte_reader_get_uint32_be (&stream->ctts,
            &stream->n_composition_times))
      goto corrupt_file;

    /* make sure there's enough data */
    if (!qt_atom_parser_has_chunks (&stream->ctts, stream->n_composition_times,
            4 + 4))
      goto corrupt_file;
  }

  return TRUE;

corrupt_file:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
no_samples:
  {
    gst_qtdemux_stbl_free (stream);
    if (!qtdemux->fragmented) {
      /* not quite good */
      GST_WARNING_OBJECT (qtdemux, "stream has no samples");
      return FALSE;
    } else {
      /* may pick up samples elsewhere */
      return TRUE;
    }
  }
}

/* collect samples from the next sample to be parsed up to sample @n for @stream
 * by reading the info from @stbl
 *
 * This code can be executed from both the streaming thread and the seeking
 * thread so it takes the object lock to protect itself
 */
static gboolean
qtdemux_parse_samples (GstQTDemux * qtdemux, QtDemuxStream * stream, guint32 n)
{
  gint i, j, k;
  QtDemuxSample *samples, *first, *cur, *last;
  guint32 n_samples_per_chunk;
  guint32 n_samples;

  GST_LOG_OBJECT (qtdemux, "parsing samples for stream fourcc %"
      GST_FOURCC_FORMAT ", pad %s", GST_FOURCC_ARGS (stream->fourcc),
      stream->pad ? GST_PAD_NAME (stream->pad) : "(NULL)");

  n_samples = stream->n_samples;

  if (n >= n_samples)
    goto out_of_samples;

  GST_OBJECT_LOCK (qtdemux);
  if (n <= stream->stbl_index)
    goto already_parsed;

  GST_DEBUG_OBJECT (qtdemux, "parsing up to sample %u", n);

  if (!stream->stsz.data) {
    /* so we already parsed and passed all the moov samples;
     * onto fragmented ones */
    g_assert (qtdemux->fragmented);
    goto done;
  }

  /* pointer to the sample table */
  samples = stream->samples;

  /* starts from -1, moves to the next sample index to parse */
  stream->stbl_index++;

  /* keep track of the first and last sample to fill */
  first = &samples[stream->stbl_index];
  last = &samples[n];

  if (stream->chunks_are_chunks) {
    /* set the sample sizes */
    if (stream->sample_size == 0) {
      /* different sizes for each sample */
      for (cur = first; cur <= last; cur++) {
        cur->size = gst_byte_reader_get_uint32_be_unchecked (&stream->stsz);
        GST_LOG_OBJECT (qtdemux, "sample %d has size %u",
            (guint) (cur - samples), cur->size);
      }
    } else {
      /* samples have the same size */
      GST_LOG_OBJECT (qtdemux, "all samples have size %u", stream->sample_size);
      for (cur = first; cur <= last; cur++)
        cur->size = stream->sample_size;
    }
  }

  n_samples_per_chunk = stream->n_samples_per_chunk;
  cur = first;

  for (i = stream->stsc_index; i < n_samples_per_chunk; i++) {
    guint32 last_chunk;

    if (stream->stsc_chunk_index >= stream->last_chunk
        || stream->stsc_chunk_index < stream->first_chunk) {
      stream->first_chunk =
          gst_byte_reader_get_uint32_be_unchecked (&stream->stsc);
      stream->samples_per_chunk =
          gst_byte_reader_get_uint32_be_unchecked (&stream->stsc);
      gst_byte_reader_skip_unchecked (&stream->stsc, 4);

      /* chunk numbers are counted from 1 it seems */
      if (G_UNLIKELY (stream->first_chunk == 0))
        goto corrupt_file;

      --stream->first_chunk;

      /* the last chunk of each entry is calculated by taking the first chunk
       * of the next entry; except if there is no next, where we fake it with
       * INT_MAX */
      if (G_UNLIKELY (i == (stream->n_samples_per_chunk - 1))) {
        stream->last_chunk = G_MAXUINT32;
      } else {
        stream->last_chunk =
            gst_byte_reader_peek_uint32_be_unchecked (&stream->stsc);
        if (G_UNLIKELY (stream->last_chunk == 0))
          goto corrupt_file;

        --stream->last_chunk;
      }

      GST_LOG_OBJECT (qtdemux,
          "entry %d has first_chunk %d, last_chunk %d, samples_per_chunk %d", i,
          stream->first_chunk, stream->last_chunk, stream->samples_per_chunk);

      if (G_UNLIKELY (stream->last_chunk < stream->first_chunk))
        goto corrupt_file;

      if (stream->last_chunk != G_MAXUINT32) {
        if (!qt_atom_parser_peek_sub (&stream->stco,
                stream->first_chunk * stream->co_size,
                (stream->last_chunk - stream->first_chunk) * stream->co_size,
                &stream->co_chunk))
          goto corrupt_file;

      } else {
        stream->co_chunk = stream->stco;
        if (!gst_byte_reader_skip (&stream->co_chunk,
                stream->first_chunk * stream->co_size))
          goto corrupt_file;
      }

      stream->stsc_chunk_index = stream->first_chunk;
    }

    last_chunk = stream->last_chunk;

    if (stream->chunks_are_chunks) {
      for (j = stream->stsc_chunk_index; j < last_chunk; j++) {
        guint32 samples_per_chunk;
        guint64 chunk_offset;

        if (!stream->stsc_sample_index
            && !qt_atom_parser_get_offset (&stream->co_chunk, stream->co_size,
                &stream->chunk_offset))
          goto corrupt_file;

        samples_per_chunk = stream->samples_per_chunk;
        chunk_offset = stream->chunk_offset;

        for (k = stream->stsc_sample_index; k < samples_per_chunk; k++) {
          GST_LOG_OBJECT (qtdemux, "Creating entry %d with offset %"
              G_GUINT64_FORMAT, (guint) (cur - samples), stream->chunk_offset);

          cur->offset = chunk_offset;
          chunk_offset += cur->size;
          cur++;

          if (G_UNLIKELY (cur > last)) {
            /* save state */
            stream->stsc_sample_index = k + 1;
            stream->chunk_offset = chunk_offset;
            stream->stsc_chunk_index = j;
            goto done2;
          }
        }
        stream->stsc_sample_index = 0;
      }
      stream->stsc_chunk_index = j;
    } else {
      cur = &samples[stream->stsc_chunk_index];

      for (j = stream->stsc_chunk_index; j < last_chunk; j++) {
        if (j > n) {
          /* save state */
          stream->stsc_chunk_index = j;
          goto done;
        }

        cur->offset =
            qt_atom_parser_get_offset_unchecked (&stream->co_chunk,
            stream->co_size);

        GST_LOG_OBJECT (qtdemux, "Created entry %d with offset "
            "%" G_GUINT64_FORMAT, j, cur->offset);

        if (stream->samples_per_frame * stream->bytes_per_frame) {
          cur->size =
              (stream->samples_per_chunk * stream->n_channels) /
              stream->samples_per_frame * stream->bytes_per_frame;
        } else {
          cur->size = stream->samples_per_chunk;
        }

        GST_DEBUG_OBJECT (qtdemux,
            "keyframe sample %d: timestamp %" GST_TIME_FORMAT ", size %u",
            j, GST_TIME_ARGS (gst_util_uint64_scale (stream->stco_sample_index,
                    GST_SECOND, stream->timescale)), cur->size);

        cur->timestamp = stream->stco_sample_index;
        cur->duration = stream->samples_per_chunk;
        cur->keyframe = TRUE;
        cur++;

        stream->stco_sample_index += stream->samples_per_chunk;
      }
      stream->stsc_chunk_index = j;
    }
    stream->stsc_index++;
  }

  if (!stream->chunks_are_chunks)
    goto ctts;
done2:
  {
    guint32 n_sample_times;

    n_sample_times = stream->n_sample_times;
    cur = first;

    for (i = stream->stts_index; i < n_sample_times; i++) {
      guint32 stts_samples;
      guint32 stts_duration;
      guint32 stts_time;

      if (stream->stts_sample_index >= stream->stts_samples
          || !stream->stts_sample_index) {

        stream->stts_samples =
            gst_byte_reader_get_uint32_be_unchecked (&stream->stts);
        stream->stts_duration =
            gst_byte_reader_get_uint32_be_unchecked (&stream->stts);

        GST_LOG_OBJECT (qtdemux, "block %d, %u timestamps, duration %u",
            i, stream->stts_samples, stream->stts_duration);

        stream->stts_sample_index = 0;
      }

      stts_samples = stream->stts_samples;
      stts_duration = stream->stts_duration;
      stts_time = stream->stts_time;

      for (j = stream->stts_sample_index; j < stts_samples; j++) {
        GST_DEBUG_OBJECT (qtdemux,
            "sample %d: index %d, timestamp %" GST_TIME_FORMAT,
            (guint) (cur - samples), j,
            GST_TIME_ARGS (gst_util_uint64_scale (stts_time, GST_SECOND,
                    stream->timescale)));

        cur->timestamp = stts_time;
        cur->duration = stts_duration;

        stts_time += stts_duration;
        cur++;

        if (G_UNLIKELY (cur > last)) {
          /* save values */
          stream->stts_time = stts_time;
          stream->stts_sample_index = j + 1;
          goto done3;
        }
      }
      stream->stts_sample_index = 0;
      stream->stts_time = stts_time;
      stream->stts_index++;
    }
    /* fill up empty timestamps with the last timestamp, this can happen when
     * the last samples do not decode and so we don't have timestamps for them.
     * We however look at the last timestamp to estimate the track length so we
     * need something in here. */
    for (; cur < last; cur++) {
      GST_DEBUG_OBJECT (qtdemux,
          "fill sample %d: timestamp %" GST_TIME_FORMAT,
          (guint) (cur - samples),
          GST_TIME_ARGS (gst_util_uint64_scale (stream->stts_time, GST_SECOND,
                  stream->timescale)));
      cur->timestamp = stream->stts_time;
      cur->duration = -1;
    }
  }
done3:
  {
    /* sample sync, can be NULL */
    if (stream->stss_present == TRUE) {
      guint32 n_sample_syncs;

      n_sample_syncs = stream->n_sample_syncs;

      if (!n_sample_syncs) {
        GST_DEBUG_OBJECT (qtdemux, "all samples are keyframes");
        stream->all_keyframe = TRUE;
      } else {
        for (i = stream->stss_index; i < n_sample_syncs; i++) {
          /* note that the first sample is index 1, not 0 */
          guint32 index;

          index = gst_byte_reader_get_uint32_be_unchecked (&stream->stss);

          if (G_LIKELY (index > 0 && index <= n_samples)) {
            index -= 1;
            samples[index].keyframe = TRUE;
            GST_DEBUG_OBJECT (qtdemux, "samples at %u is keyframe", index);
            /* and exit if we have enough samples */
            if (G_UNLIKELY (index >= n)) {
              i++;
              break;
            }
          }
        }
        /* save state */
        stream->stss_index = i;
      }

      /* stps marks partial sync frames like open GOP I-Frames */
      if (stream->stps_present == TRUE) {
        guint32 n_sample_partial_syncs;

        n_sample_partial_syncs = stream->n_sample_partial_syncs;

        /* if there are no entries, the stss table contains the real
         * sync samples */
        if (n_sample_partial_syncs) {
          for (i = stream->stps_index; i < n_sample_partial_syncs; i++) {
            /* note that the first sample is index 1, not 0 */
            guint32 index;

            index = gst_byte_reader_get_uint32_be_unchecked (&stream->stps);

            if (G_LIKELY (index > 0 && index <= n_samples)) {
              index -= 1;
              samples[index].keyframe = TRUE;
              GST_DEBUG_OBJECT (qtdemux, "samples at %u is keyframe", index);
              /* and exit if we have enough samples */
              if (G_UNLIKELY (index >= n)) {
                i++;
                break;
              }
            }
          }
          /* save state */
          stream->stps_index = i;
        }
      }
    } else {
      /* no stss, all samples are keyframes */
      stream->all_keyframe = TRUE;
      GST_DEBUG_OBJECT (qtdemux, "setting all keyframes");
    }
  }

ctts:
  /* composition time to sample */
  if (stream->ctts_present == TRUE) {
    guint32 n_composition_times;
    guint32 ctts_count;
    gint32 ctts_soffset;

    /* Fill in the pts_offsets */
    cur = first;
    n_composition_times = stream->n_composition_times;

    for (i = stream->ctts_index; i < n_composition_times; i++) {
      if (stream->ctts_sample_index >= stream->ctts_count
          || !stream->ctts_sample_index) {
        stream->ctts_count =
            gst_byte_reader_get_uint32_be_unchecked (&stream->ctts);
        stream->ctts_soffset =
            gst_byte_reader_get_int32_be_unchecked (&stream->ctts);
        stream->ctts_sample_index = 0;
      }

      ctts_count = stream->ctts_count;
      ctts_soffset = stream->ctts_soffset;

      for (j = stream->ctts_sample_index; j < ctts_count; j++) {
        cur->pts_offset = ctts_soffset;
        cur++;

        if (G_UNLIKELY (cur > last)) {
          /* save state */
          stream->ctts_sample_index = j + 1;
          goto done;
        }
      }
      stream->ctts_sample_index = 0;
      stream->ctts_index++;
    }
  }
done:
  stream->stbl_index = n;
  /* if index has been completely parsed, free data that is no-longer needed */
  if (n + 1 == stream->n_samples) {
    gst_qtdemux_stbl_free (stream);
    GST_DEBUG_OBJECT (qtdemux,
        "parsed all available samples; checking for more");
    while (n + 1 == stream->n_samples)
      if (qtdemux_add_fragmented_samples (qtdemux) != GST_FLOW_OK)
        break;
  }
  GST_OBJECT_UNLOCK (qtdemux);

  return TRUE;

  /* SUCCESS */
already_parsed:
  {
    GST_LOG_OBJECT (qtdemux,
        "Tried to parse up to sample %u but this sample has already been parsed",
        n);
    /* if fragmented, there may be more */
    if (qtdemux->fragmented && n == stream->stbl_index)
      goto done;
    GST_OBJECT_UNLOCK (qtdemux);
    return TRUE;
  }
  /* ERRORS */
out_of_samples:
  {
    GST_LOG_OBJECT (qtdemux,
        "Tried to parse up to sample %u but there are only %u samples", n + 1,
        stream->n_samples);
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
corrupt_file:
  {
    GST_OBJECT_UNLOCK (qtdemux);
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")), (NULL));
    return FALSE;
  }
}

/* collect all segment info for @stream.
 */
static gboolean
qtdemux_parse_segments (GstQTDemux * qtdemux, QtDemuxStream * stream,
    GNode * trak)
{
  GNode *edts;

  /* parse and prepare segment info from the edit list */
  GST_DEBUG_OBJECT (qtdemux, "looking for edit list container");
  stream->n_segments = 0;
  stream->segments = NULL;
  if ((edts = qtdemux_tree_get_child_by_type (trak, FOURCC_edts))) {
    GNode *elst;
    gint n_segments;
    gint i, count;
    guint64 time, stime;
    guint8 *buffer;

    GST_DEBUG_OBJECT (qtdemux, "looking for edit list");
    if (!(elst = qtdemux_tree_get_child_by_type (edts, FOURCC_elst)))
      goto done;

    buffer = elst->data;

    n_segments = QT_UINT32 (buffer + 12);

    /* we might allocate a bit too much, at least allocate 1 segment */
#ifdef GSTREAMER_LITE
    n_segments = MAX (n_segments, 1);
    if (n_segments < G_MAXSIZE / sizeof(QtDemuxSegment))
        stream->segments = g_new (QtDemuxSegment, n_segments);
    else
        return FALSE;
#else
    stream->segments = g_new (QtDemuxSegment, MAX (n_segments, 1));
#endif // GSTREAMER_LITE

    /* segments always start from 0 */
    time = 0;
    stime = 0;
    count = 0;
    for (i = 0; i < n_segments; i++) {
      guint64 duration;
      guint64 media_time;
      QtDemuxSegment *segment;
      guint32 rate_int;

      media_time = QT_UINT32 (buffer + 20 + i * 12);

      /* -1 media time is an empty segment, just ignore it */
      if (media_time == G_MAXUINT32)
        continue;

      duration = QT_UINT32 (buffer + 16 + i * 12);

      segment = &stream->segments[count++];

      /* time and duration expressed in global timescale */
      segment->time = stime;
      /* add non scaled values so we don't cause roundoff errors */
      time += duration;
      stime = gst_util_uint64_scale (time, GST_SECOND, qtdemux->timescale);
      segment->stop_time = stime;
      segment->duration = stime - segment->time;
      /* media_time expressed in stream timescale */
      segment->media_start =
          gst_util_uint64_scale (media_time, GST_SECOND, stream->timescale);
      segment->media_stop = segment->media_start + segment->duration;
      rate_int = GST_READ_UINT32_BE (buffer + 24 + i * 12);

      if (rate_int <= 1) {
        /* 0 is not allowed, some programs write 1 instead of the floating point
         * value */
        GST_WARNING_OBJECT (qtdemux, "found suspicious rate %" G_GUINT32_FORMAT,
            rate_int);
        segment->rate = 1;
      } else {
        segment->rate = rate_int / 65536.0;
      }

      GST_DEBUG_OBJECT (qtdemux, "created segment %d time %" GST_TIME_FORMAT
          ", duration %" GST_TIME_FORMAT ", media_time %" GST_TIME_FORMAT
          ", rate %g, (%d)", i, GST_TIME_ARGS (segment->time),
          GST_TIME_ARGS (segment->duration),
          GST_TIME_ARGS (segment->media_start), segment->rate, rate_int);
    }
    GST_DEBUG_OBJECT (qtdemux, "found %d non-empty segments", count);
    stream->n_segments = count;
  }
done:

  /* push based does not handle segments, so act accordingly here,
   * and warn if applicable */
  if (!qtdemux->pullbased) {
    GST_WARNING_OBJECT (qtdemux, "streaming; discarding edit list segments");
    /* remove and use default one below, we stream like it anyway */
    g_free (stream->segments);
    stream->segments = NULL;
    stream->n_segments = 0;
  }

  /* no segments, create one to play the complete trak */
  if (stream->n_segments == 0) {
    GstClockTime stream_duration =
        gst_util_uint64_scale (stream->duration, GST_SECOND, stream->timescale);

    if (stream->segments == NULL)
      stream->segments = g_new (QtDemuxSegment, 1);

    /* represent unknown our way */
    if (stream_duration == 0)
      stream_duration = -1;

    stream->segments[0].time = 0;
    stream->segments[0].stop_time = stream_duration;
    stream->segments[0].duration = stream_duration;
    stream->segments[0].media_start = 0;
    stream->segments[0].media_stop = stream_duration;
    stream->segments[0].rate = 1.0;

    GST_DEBUG_OBJECT (qtdemux, "created dummy segment %" GST_TIME_FORMAT,
        GST_TIME_ARGS (stream_duration));
    stream->n_segments = 1;
  }
  GST_DEBUG_OBJECT (qtdemux, "using %d segments", stream->n_segments);

  return TRUE;
}

/*
 * Parses the stsd atom of a svq3 trak looking for
 * the SMI and gama atoms.
 */
static void
qtdemux_parse_svq3_stsd_data (GstQTDemux * qtdemux, GNode * stsd,
    guint8 ** gamma, GstBuffer ** seqh)
{
  guint8 *_gamma = NULL;
  GstBuffer *_seqh = NULL;
  guint8 *stsd_data = stsd->data;
  guint32 length = QT_UINT32 (stsd_data);
  guint16 version;

  if (length < 32) {
    GST_WARNING_OBJECT (qtdemux, "stsd too short");
    goto end;
  }

  stsd_data += 32;
  length -= 32;
  version = QT_UINT16 (stsd_data);
  if (version == 3) {
    if (length >= 70) {
      length -= 70;
      stsd_data += 70;
      while (length > 8) {
        guint32 fourcc, size;
        guint8 *data;
        size = QT_UINT32 (stsd_data);
        fourcc = QT_FOURCC (stsd_data + 4);
        data = stsd_data + 8;

        switch (fourcc) {
          case FOURCC_gama:{
            if (size == 12) {
              _gamma = data;
            } else {
              GST_WARNING_OBJECT (qtdemux, "Unexpected size %" G_GUINT32_FORMAT
                  " for gama atom, expected 12", size);
            }
            break;
          }
          case FOURCC_SMI_:{
            if (size > 16 && QT_FOURCC (data) == FOURCC_SEQH) {
              guint32 seqh_size;
              if (_seqh != NULL) {
                GST_WARNING_OBJECT (qtdemux, "Unexpected second SEQH SMI atom "
                    " found, ignoring");
              } else {
                seqh_size = QT_UINT32 (data + 4);
                if (seqh_size > 0) {
                  _seqh = gst_buffer_new_and_alloc (seqh_size);
                  memcpy (GST_BUFFER_DATA (_seqh), data + 8, seqh_size);
                }
              }
            }
            break;
          }
          default:{
            GST_WARNING_OBJECT (qtdemux, "Unhandled atom %" GST_FOURCC_FORMAT
                " in SVQ3 entry in stsd atom", GST_FOURCC_ARGS (fourcc));
          }
        }

        if (size <= length) {
          length -= size;
          stsd_data += size;
        }
      }
    } else {
      GST_WARNING_OBJECT (qtdemux, "SVQ3 entry too short in stsd atom");
    }
  } else {
    GST_WARNING_OBJECT (qtdemux, "Unexpected version for SVQ3 entry %"
        G_GUINT16_FORMAT, version);
    goto end;
  }

end:
  if (gamma) {
    *gamma = _gamma;
  }
  if (seqh) {
    *seqh = _seqh;
  } else if (_seqh) {
    gst_buffer_unref (_seqh);
  }
}

static gchar *
qtdemux_get_rtsp_uri_from_hndl (GstQTDemux * qtdemux, GNode * minf)
{
  GNode *dinf;
  GstByteReader dref;
  gchar *uri = NULL;

  /*
   * Get 'dinf', to get its child 'dref', that might contain a 'hndl'
   * atom that might contain a 'data' atom with the rtsp uri.
   * This case was reported in bug #597497, some info about
   * the hndl atom can be found in TN1195
   */
  dinf = qtdemux_tree_get_child_by_type (minf, FOURCC_dinf);
  GST_DEBUG_OBJECT (qtdemux, "Trying to obtain rtsp URI for stream trak");

  if (dinf) {
    guint32 dref_num_entries = 0;
    if (qtdemux_tree_get_child_by_type_full (dinf, FOURCC_dref, &dref) &&
        gst_byte_reader_skip (&dref, 4) &&
        gst_byte_reader_get_uint32_be (&dref, &dref_num_entries)) {
      gint i;

      /* search dref entries for hndl atom */
      for (i = 0; i < dref_num_entries; i++) {
        guint32 size = 0, type;
        guint8 string_len = 0;
        if (gst_byte_reader_get_uint32_be (&dref, &size) &&
            qt_atom_parser_get_fourcc (&dref, &type)) {
          if (type == FOURCC_hndl) {
            GST_DEBUG_OBJECT (qtdemux, "Found hndl atom");

            /* skip data reference handle bytes and the
             * following pascal string and some extra 4
             * bytes I have no idea what are */
            if (!gst_byte_reader_skip (&dref, 4) ||
                !gst_byte_reader_get_uint8 (&dref, &string_len) ||
                !gst_byte_reader_skip (&dref, string_len + 4)) {
              GST_WARNING_OBJECT (qtdemux, "Failed to parse hndl atom");
              break;
            }

            /* iterate over the atoms to find the data atom */
            while (gst_byte_reader_get_remaining (&dref) >= 8) {
              guint32 atom_size;
              guint32 atom_type;

              if (gst_byte_reader_get_uint32_be (&dref, &atom_size) &&
                  qt_atom_parser_get_fourcc (&dref, &atom_type)) {
                if (atom_type == FOURCC_data) {
                  const guint8 *uri_aux = NULL;

                  /* found the data atom that might contain the rtsp uri */
                  GST_DEBUG_OBJECT (qtdemux, "Found data atom inside "
                      "hndl atom, interpreting it as an URI");
                  if (gst_byte_reader_peek_data (&dref, atom_size - 8,
                          &uri_aux)) {
                    if (g_strstr_len ((gchar *) uri_aux, 7, "rtsp://") != NULL)
                      uri = g_strndup ((gchar *) uri_aux, atom_size - 8);
                    else
                      GST_WARNING_OBJECT (qtdemux, "Data atom in hndl atom "
                          "didn't contain a rtsp address");
                  } else {
                    GST_WARNING_OBJECT (qtdemux, "Failed to get the data "
                        "atom contents");
                  }
                  break;
                }
                /* skipping to the next entry */
                gst_byte_reader_skip (&dref, atom_size - 8);
              } else {
                GST_WARNING_OBJECT (qtdemux, "Failed to parse hndl child "
                    "atom header");
                break;
              }
            }
            break;
          }
          /* skip to the next entry */
          gst_byte_reader_skip (&dref, size - 8);
        } else {
          GST_WARNING_OBJECT (qtdemux, "Error parsing dref atom");
        }
      }
      GST_DEBUG_OBJECT (qtdemux, "Finished parsing dref atom");
    }
  }
  return uri;
}

static gint
less_than (gconstpointer a, gconstpointer b)
{
  const guint32 *av = a, *bv = b;

  return *av - *bv;
}

/* parse the traks.
 * With each track we associate a new QtDemuxStream that contains all the info
 * about the trak.
 * traks that do not decode to something (like strm traks) will not have a pad.
 */
static gboolean
qtdemux_parse_trak (GstQTDemux * qtdemux, GNode * trak)
{
  GstByteReader tkhd;
  int offset;
  GNode *mdia;
  GNode *mdhd;
  GNode *hdlr;
  GNode *minf;
  GNode *stbl;
  GNode *stsd;
  GNode *mp4a;
  GNode *mp4v;
  GNode *wave;
  GNode *esds;
  GNode *pasp;
  QtDemuxStream *stream;
  GstTagList *list = NULL;
  gchar *codec = NULL;
  const guint8 *stsd_data;
  guint16 lang_code;            /* quicktime lang code or packed iso code */
  guint32 version;
  guint32 tkhd_flags = 0;
  guint8 tkhd_version = 0;
  guint32 fourcc;
  guint value_size, len;

  stream = g_new0 (QtDemuxStream, 1);
  /* new streams always need a discont */
  stream->discont = TRUE;
  /* we enable clipping for raw audio/video streams */
  stream->need_clip = FALSE;
  stream->need_process = FALSE;
  stream->segment_index = -1;
  stream->time_position = 0;
  stream->sample_index = -1;
  stream->last_ret = GST_FLOW_OK;

  if (!qtdemux_tree_get_child_by_type_full (trak, FOURCC_tkhd, &tkhd)
      || !gst_byte_reader_get_uint8 (&tkhd, &tkhd_version)
      || !gst_byte_reader_get_uint24_be (&tkhd, &tkhd_flags))
    goto corrupt_file;

  /* pick between 64 or 32 bits */
  value_size = tkhd_version == 1 ? 8 : 4;
  if (!gst_byte_reader_skip (&tkhd, value_size * 2) ||
      !gst_byte_reader_get_uint32_be (&tkhd, &stream->track_id))
    goto corrupt_file;
#ifdef GSTREAMER_LITE
  // set track_enabled based on bit 0 of flags
  stream->track_enabled = (gboolean)(tkhd_flags & 0x000001);
#endif // GSTREAMER_LITE

  GST_LOG_OBJECT (qtdemux, "track[tkhd] version/flags/id: 0x%02x/%06x/%u",
      tkhd_version, tkhd_flags, stream->track_id);

  if (!(mdia = qtdemux_tree_get_child_by_type (trak, FOURCC_mdia)))
    goto corrupt_file;

  if (!(mdhd = qtdemux_tree_get_child_by_type (mdia, FOURCC_mdhd))) {
    /* be nice for some crooked mjp2 files that use mhdr for mdhd */
    if (qtdemux->major_brand != FOURCC_mjp2 ||
        !(mdhd = qtdemux_tree_get_child_by_type (mdia, FOURCC_mhdr)))
      goto corrupt_file;
  }

  len = QT_UINT32 ((guint8 *) mdhd->data);
  version = QT_UINT32 ((guint8 *) mdhd->data + 8);
  GST_LOG_OBJECT (qtdemux, "track version/flags: %08x", version);
  if (version == 0x01000000) {
    if (len < 38)
      goto corrupt_file;
    stream->timescale = QT_UINT32 ((guint8 *) mdhd->data + 28);
    stream->duration = QT_UINT64 ((guint8 *) mdhd->data + 32);
    lang_code = QT_UINT16 ((guint8 *) mdhd->data + 36);
  } else {
    if (len < 30)
      goto corrupt_file;
    stream->timescale = QT_UINT32 ((guint8 *) mdhd->data + 20);
    stream->duration = QT_UINT32 ((guint8 *) mdhd->data + 24);
    lang_code = QT_UINT16 ((guint8 *) mdhd->data + 28);
  }

  if (lang_code < 0x800) {
    qtdemux_lang_map_qt_code_to_iso (stream->lang_id, lang_code);
  } else {
    stream->lang_id[0] = 0x60 + ((lang_code >> 10) & 0x1F);
    stream->lang_id[1] = 0x60 + ((lang_code >> 5) & 0x1F);
    stream->lang_id[2] = 0x60 + (lang_code & 0x1F);
    stream->lang_id[3] = 0;
  }

  GST_LOG_OBJECT (qtdemux, "track timescale: %" G_GUINT32_FORMAT,
      stream->timescale);
  GST_LOG_OBJECT (qtdemux, "track duration: %" G_GUINT64_FORMAT,
      stream->duration);
  GST_LOG_OBJECT (qtdemux, "track language code/id: 0x%04x/%s",
      lang_code, stream->lang_id);

  if (G_UNLIKELY (stream->timescale == 0 || qtdemux->timescale == 0))
    goto corrupt_file;

  /* fragmented files may have bogus duration in moov */
  if (!qtdemux->fragmented &&
      qtdemux->duration != G_MAXINT64 && stream->duration != G_MAXINT32) {
    guint64 tdur1, tdur2;

    /* don't overflow */
    tdur1 = stream->timescale * (guint64) qtdemux->duration;
    tdur2 = qtdemux->timescale * (guint64) stream->duration;

    /* HACK:
     * some of those trailers, nowadays, have prologue images that are
     * themselves vide tracks as well. I haven't really found a way to
     * identify those yet, except for just looking at their duration. */
    if (tdur1 != 0 && (tdur2 * 10 / tdur1) < 2) {
      GST_WARNING_OBJECT (qtdemux,
          "Track shorter than 20%% (%" G_GUINT64_FORMAT "/%" G_GUINT32_FORMAT
          " vs. %" G_GUINT64_FORMAT "/%" G_GUINT32_FORMAT ") of the stream "
          "found, assuming preview image or something; skipping track",
          stream->duration, stream->timescale, qtdemux->duration,
          qtdemux->timescale);
      g_free (stream);
      return TRUE;
    }
  }

  if (!(hdlr = qtdemux_tree_get_child_by_type (mdia, FOURCC_hdlr)))
    goto corrupt_file;

  GST_LOG_OBJECT (qtdemux, "track type: %" GST_FOURCC_FORMAT,
      GST_FOURCC_ARGS (QT_FOURCC ((guint8 *) hdlr->data + 12)));

  len = QT_UINT32 ((guint8 *) hdlr->data);
  if (len >= 20)
    stream->subtype = QT_FOURCC ((guint8 *) hdlr->data + 16);
  GST_LOG_OBJECT (qtdemux, "track subtype: %" GST_FOURCC_FORMAT,
      GST_FOURCC_ARGS (stream->subtype));

  if (!(minf = qtdemux_tree_get_child_by_type (mdia, FOURCC_minf)))
    goto corrupt_file;

  if (!(stbl = qtdemux_tree_get_child_by_type (minf, FOURCC_stbl)))
    goto corrupt_file;

  /* parse stsd */
  if (!(stsd = qtdemux_tree_get_child_by_type (stbl, FOURCC_stsd)))
    goto corrupt_file;
  stsd_data = (const guint8 *) stsd->data;

  /* stsd should at least have one entry */
  len = QT_UINT32 (stsd_data);
  if (len < 24)
    goto corrupt_file;

  /* and that entry should fit within stsd */
  len = QT_UINT32 (stsd_data + 16);
  if (len > QT_UINT32 (stsd_data) + 16)
    goto corrupt_file;
  GST_LOG_OBJECT (qtdemux, "stsd len:           %d", len);

  stream->fourcc = fourcc = QT_FOURCC (stsd_data + 16 + 4);
  GST_LOG_OBJECT (qtdemux, "stsd type:          %" GST_FOURCC_FORMAT,
      GST_FOURCC_ARGS (stream->fourcc));

  if ((fourcc == FOURCC_drms) || (fourcc == FOURCC_drmi) ||
      ((fourcc & 0xFFFFFF00) == GST_MAKE_FOURCC ('e', 'n', 'c', 0)))
    goto error_encrypted;

  if (stream->subtype == FOURCC_vide) {
    guint32 w = 0, h = 0;

    stream->sampled = TRUE;

    /* version 1 uses some 64-bit ints */
    if (!gst_byte_reader_skip (&tkhd, 56 + value_size)
        || !gst_byte_reader_get_uint32_be (&tkhd, &w)
        || !gst_byte_reader_get_uint32_be (&tkhd, &h))
      goto corrupt_file;

    stream->display_width = w >> 16;
    stream->display_height = h >> 16;

    offset = 16;
    if (len < 86)
      goto corrupt_file;

    stream->width = QT_UINT16 (stsd_data + offset + 32);
    stream->height = QT_UINT16 (stsd_data + offset + 34);
    stream->fps_n = 0;          /* this is filled in later */
    stream->fps_d = 0;          /* this is filled in later */
    stream->bits_per_sample = QT_UINT16 (stsd_data + offset + 82);
    stream->color_table_id = QT_UINT16 (stsd_data + offset + 84);

    GST_LOG_OBJECT (qtdemux, "frame count:   %u",
        QT_UINT16 (stsd_data + offset + 48));

    stream->caps =
        qtdemux_video_caps (qtdemux, stream, fourcc, stsd_data, &codec);
    if (codec) {
      list = gst_tag_list_new ();
      gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
          GST_TAG_VIDEO_CODEC, codec, NULL);
      g_free (codec);
      codec = NULL;
    }

    esds = NULL;
    pasp = NULL;
    /* pick 'the' stsd child */
    mp4v = qtdemux_tree_get_child_by_type (stsd, fourcc);
    if (mp4v) {
      esds = qtdemux_tree_get_child_by_type (mp4v, FOURCC_esds);
      pasp = qtdemux_tree_get_child_by_type (mp4v, FOURCC_pasp);
    }

    if (pasp) {
      const guint8 *pasp_data = (const guint8 *) pasp->data;

      stream->par_w = QT_UINT32 (pasp_data + 8);
      stream->par_h = QT_UINT32 (pasp_data + 12);
    } else {
      stream->par_w = 0;
      stream->par_h = 0;
    }

    if (esds) {
#ifdef GSTREAMER_LITE
        if (!gst_qtdemux_handle_esds (qtdemux, stream, esds, list))
            goto corrupt_file;
#else
        gst_qtdemux_handle_esds (qtdemux, stream, esds, list);
#endif // GSTREAMER_LITE
    } else {
      switch (fourcc) {
        case FOURCC_avc1:
        {
          gint len = QT_UINT32 (stsd_data) - 0x66;
          const guint8 *avc_data = stsd_data + 0x66;

          /* find avcC */
          while (len >= 0x8) {
            gint size;

            if (QT_UINT32 (avc_data) <= len)
              size = QT_UINT32 (avc_data) - 0x8;
            else
              size = len - 0x8;

            if (size < 1)
              /* No real data, so break out */
              break;

            switch (QT_FOURCC (avc_data + 0x4)) {
              case FOURCC_avcC:
              {
                /* parse, if found */
                GstBuffer *buf;

                GST_DEBUG_OBJECT (qtdemux, "found avcC codec_data in stsd");

                /* First 4 bytes are the length of the atom, the next 4 bytes
                 * are the fourcc, the next 1 byte is the version, and the
                 * subsequent bytes are sequence parameter set like data. */
                gst_codec_utils_h264_caps_set_level_and_profile (stream->caps,
                    avc_data + 8 + 1, size - 1);

                buf = gst_buffer_new_and_alloc (size);
                memcpy (GST_BUFFER_DATA (buf), avc_data + 0x8, size);
                gst_caps_set_simple (stream->caps,
                    "codec_data", GST_TYPE_BUFFER, buf, NULL);
                gst_buffer_unref (buf);

                break;
              }
              case FOURCC_btrt:
              {
                guint avg_bitrate, max_bitrate;

                /* bufferSizeDB, maxBitrate and avgBitrate - 4 bytes each */
                if (size < 12)
                  break;

                max_bitrate = QT_UINT32 (avc_data + 0xc);
                avg_bitrate = QT_UINT32 (avc_data + 0x10);

                if (!max_bitrate && !avg_bitrate)
                  break;

                /* Some muxers seem to swap the average and maximum bitrates
                 * (I'm looking at you, YouTube), so we swap for sanity. */
                if (max_bitrate > 0 && max_bitrate < avg_bitrate) {
                  guint temp = avg_bitrate;

                  avg_bitrate = max_bitrate;
                  max_bitrate = temp;
                }

                if (!list)
                  list = gst_tag_list_new ();

                if (max_bitrate > 0 && max_bitrate < G_MAXUINT32) {
                  gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
                      GST_TAG_MAXIMUM_BITRATE, max_bitrate, NULL);
                }
                if (avg_bitrate > 0 && avg_bitrate < G_MAXUINT32) {
                  gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
                      GST_TAG_BITRATE, avg_bitrate, NULL);
                }

                break;
              }

              default:
                break;
            }

            len -= size + 8;
            avc_data += size + 8;
          }

          break;
        }
        case FOURCC_mp4v:
        case FOURCC_MP4V:
        case FOURCC_fmp4:
        case FOURCC_FMP4:
        {
          GNode *glbl;

          GST_DEBUG_OBJECT (qtdemux, "found %" GST_FOURCC_FORMAT,
              GST_FOURCC_ARGS (fourcc));

          /* codec data might be in glbl extension atom */
          glbl = mp4v ?
              qtdemux_tree_get_child_by_type (mp4v, FOURCC_glbl) : NULL;
          if (glbl) {
            guint8 *data;
            GstBuffer *buf;
            gint len;

            GST_DEBUG_OBJECT (qtdemux, "found glbl data in stsd");
            data = glbl->data;
            len = QT_UINT32 (data);
            if (len > 0x8) {
              len -= 0x8;
              buf = gst_buffer_new_and_alloc (len);
              memcpy (GST_BUFFER_DATA (buf), data + 8, len);
              gst_caps_set_simple (stream->caps,
                  "codec_data", GST_TYPE_BUFFER, buf, NULL);
              gst_buffer_unref (buf);
            }
          }
          break;
        }
        case FOURCC_mjp2:
        {
          /* see annex I of the jpeg2000 spec */
          GNode *jp2h, *ihdr, *colr, *mjp2, *field, *prefix, *cmap, *cdef;
          const guint8 *data;
          guint32 fourcc = 0;
          gint ncomp = 0;
          guint32 ncomp_map = 0;
          gint32 *comp_map = NULL;
          guint32 nchan_def = 0;
          gint32 *chan_def = NULL;

          GST_DEBUG_OBJECT (qtdemux, "found mjp2");
          /* some required atoms */
          mjp2 = qtdemux_tree_get_child_by_type (stsd, FOURCC_mjp2);
          if (!mjp2)
            break;
          jp2h = qtdemux_tree_get_child_by_type (mjp2, FOURCC_jp2h);
          if (!jp2h)
            break;

          /* number of components; redundant with info in codestream, but useful
             to a muxer */
          ihdr = qtdemux_tree_get_child_by_type (jp2h, FOURCC_ihdr);
          if (!ihdr || QT_UINT32 (ihdr->data) != 22)
            break;
          ncomp = QT_UINT16 (((guint8 *) ihdr->data) + 16);

          colr = qtdemux_tree_get_child_by_type (jp2h, FOURCC_colr);
          if (!colr)
            break;
          GST_DEBUG_OBJECT (qtdemux, "found colr");
          /* extract colour space info */
          if (QT_UINT8 ((guint8 *) colr->data + 8) == 1) {
            switch (QT_UINT32 ((guint8 *) colr->data + 11)) {
              case 16:
                fourcc = GST_MAKE_FOURCC ('s', 'R', 'G', 'B');
                break;
              case 17:
                fourcc = GST_MAKE_FOURCC ('G', 'R', 'A', 'Y');
                break;
              case 18:
                fourcc = GST_MAKE_FOURCC ('s', 'Y', 'U', 'V');
                break;
              default:
                break;
            }
          }
          if (!fourcc)
            /* colr is required, and only values 16, 17, and 18 are specified,
               so error if we have no fourcc */
            break;

          /* extract component mapping */
          cmap = qtdemux_tree_get_child_by_type (jp2h, FOURCC_cmap);
          if (cmap) {
            guint32 cmap_len = 0;
            int i;
            cmap_len = QT_UINT32 (cmap->data);
            if (cmap_len >= 8) {
              /* normal box, subtract off header */
              cmap_len -= 8;
              /* cmap: { u16 cmp; u8 mtyp; u8 pcol; }* */
              if (cmap_len % 4 == 0) {
                ncomp_map = (cmap_len / 4);
                comp_map = g_new0 (gint32, ncomp_map);
                for (i = 0; i < ncomp_map; i++) {
                  guint16 cmp;
                  guint8 mtyp, pcol;
                  cmp = QT_UINT16 (((guint8 *) cmap->data) + 8 + i * 4);
                  mtyp = QT_UINT8 (((guint8 *) cmap->data) + 8 + i * 4 + 2);
                  pcol = QT_UINT8 (((guint8 *) cmap->data) + 8 + i * 4 + 3);
                  comp_map[i] = (mtyp << 24) | (pcol << 16) | cmp;
                }
              }
            }
          }
          /* extract channel definitions */
          cdef = qtdemux_tree_get_child_by_type (jp2h, FOURCC_cdef);
          if (cdef) {
            guint32 cdef_len = 0;
            int i;
            cdef_len = QT_UINT32 (cdef->data);
            if (cdef_len >= 10) {
              /* normal box, subtract off header and len */
              cdef_len -= 10;
              /* cdef: u16 n; { u16 cn; u16 typ; u16 asoc; }* */
              if (cdef_len % 6 == 0) {
                nchan_def = (cdef_len / 6);
                chan_def = g_new0 (gint32, nchan_def);
                for (i = 0; i < nchan_def; i++)
                  chan_def[i] = -1;
                for (i = 0; i < nchan_def; i++) {
                  guint16 cn, typ, asoc;
                  cn = QT_UINT16 (((guint8 *) cdef->data) + 10 + i * 6);
                  typ = QT_UINT16 (((guint8 *) cdef->data) + 10 + i * 6 + 2);
                  asoc = QT_UINT16 (((guint8 *) cdef->data) + 10 + i * 6 + 4);
                  if (cn < nchan_def) {
                    switch (typ) {
                      case 0:
                        chan_def[cn] = asoc;
                        break;
                      case 1:
                        chan_def[cn] = 0;       /* alpha */
                        break;
                      default:
                        chan_def[cn] = -typ;
                    }
                  }
                }
              }
            }
          }

          gst_caps_set_simple (stream->caps,
              "num-components", G_TYPE_INT, ncomp, NULL);
          gst_caps_set_simple (stream->caps,
              "fourcc", GST_TYPE_FOURCC, fourcc, NULL);

          if (comp_map) {
            GValue arr = { 0, };
            GValue elt = { 0, };
            int i;
            g_value_init (&arr, GST_TYPE_ARRAY);
            g_value_init (&elt, G_TYPE_INT);
            for (i = 0; i < ncomp_map; i++) {
              g_value_set_int (&elt, comp_map[i]);
              gst_value_array_append_value (&arr, &elt);
            }
            gst_structure_set_value (gst_caps_get_structure (stream->caps, 0),
                "component-map", &arr);
            g_value_unset (&elt);
            g_value_unset (&arr);
            g_free (comp_map);
          }

          if (chan_def) {
            GValue arr = { 0, };
            GValue elt = { 0, };
            int i;
            g_value_init (&arr, GST_TYPE_ARRAY);
            g_value_init (&elt, G_TYPE_INT);
            for (i = 0; i < nchan_def; i++) {
              g_value_set_int (&elt, chan_def[i]);
              gst_value_array_append_value (&arr, &elt);
            }
            gst_structure_set_value (gst_caps_get_structure (stream->caps, 0),
                "channel-definitions", &arr);
            g_value_unset (&elt);
            g_value_unset (&arr);
            g_free (chan_def);
          }

          /* some optional atoms */
          field = qtdemux_tree_get_child_by_type (mjp2, FOURCC_fiel);
          prefix = qtdemux_tree_get_child_by_type (mjp2, FOURCC_jp2x);

          /* indicate possible fields in caps */
          if (field) {
            data = (guint8 *) field->data + 8;
            if (*data != 1)
              gst_caps_set_simple (stream->caps, "fields", G_TYPE_INT,
                  (gint) * data, NULL);
          }
          /* add codec_data if provided */
          if (prefix) {
            GstBuffer *buf;
            gint len;

            GST_DEBUG_OBJECT (qtdemux, "found prefix data in stsd");
            data = prefix->data;
            len = QT_UINT32 (data);
            if (len > 0x8) {
              len -= 0x8;
              buf = gst_buffer_new_and_alloc (len);
              memcpy (GST_BUFFER_DATA (buf), data + 8, len);
              gst_caps_set_simple (stream->caps,
                  "codec_data", GST_TYPE_BUFFER, buf, NULL);
              gst_buffer_unref (buf);
            }
          }
          break;
        }
        case FOURCC_SVQ3:
        case FOURCC_VP31:
        {
          GstBuffer *buf;
          GstBuffer *seqh = NULL;
          guint8 *gamma_data = NULL;
          gint len = QT_UINT32 (stsd_data);

          qtdemux_parse_svq3_stsd_data (qtdemux, stsd, &gamma_data, &seqh);
          if (gamma_data) {
            gst_caps_set_simple (stream->caps, "applied-gamma", G_TYPE_DOUBLE,
                QT_FP32 (gamma_data), NULL);
          }
          if (seqh) {
            /* sorry for the bad name, but we don't know what this is, other
             * than its own fourcc */
            gst_caps_set_simple (stream->caps, "seqh", GST_TYPE_BUFFER, seqh,
                NULL);
          }

          GST_DEBUG_OBJECT (qtdemux, "found codec_data in stsd");
          buf = gst_buffer_new_and_alloc (len);
          memcpy (GST_BUFFER_DATA (buf), stsd_data, len);
          gst_caps_set_simple (stream->caps,
              "codec_data", GST_TYPE_BUFFER, buf, NULL);
          gst_buffer_unref (buf);
          break;
        }
        case FOURCC_rle_:
        {
          gst_caps_set_simple (stream->caps,
              "depth", G_TYPE_INT, QT_UINT16 (stsd_data + offset + 82), NULL);
          break;
        }
        case FOURCC_XiTh:
        {
          GNode *xith, *xdxt;

          GST_DEBUG_OBJECT (qtdemux, "found XiTh");
          xith = qtdemux_tree_get_child_by_type (stsd, FOURCC_XiTh);
          if (!xith)
            break;

          xdxt = qtdemux_tree_get_child_by_type (xith, FOURCC_XdxT);
          if (!xdxt)
            break;

          GST_DEBUG_OBJECT (qtdemux, "found XdxT node");
          /* collect the headers and store them in a stream list so that we can
           * send them out first */
          qtdemux_parse_theora_extension (qtdemux, stream, xdxt);
          break;
        }
        case FOURCC_ovc1:
        {
          GNode *ovc1;
          gchar *ovc1_data;
          guint ovc1_len;
          GstBuffer *buf;

          GST_DEBUG_OBJECT (qtdemux, "parse ovc1 header");
          ovc1 = qtdemux_tree_get_child_by_type (stsd, FOURCC_ovc1);
          if (!ovc1)
            break;
          ovc1_data = ovc1->data;
          ovc1_len = QT_UINT32 (ovc1_data);
          if (ovc1_len <= 198) {
            GST_WARNING_OBJECT (qtdemux, "Too small ovc1 header, skipping");
            break;
          }
          buf = gst_buffer_new_and_alloc (ovc1_len - 198);
          memcpy (GST_BUFFER_DATA (buf), ovc1_data + 198, ovc1_len - 198);
          gst_caps_set_simple (stream->caps,
              "codec_data", GST_TYPE_BUFFER, buf, NULL);
          gst_buffer_unref (buf);
          break;
        }
        default:
          break;
      }
    }

    GST_INFO_OBJECT (qtdemux,
        "type %" GST_FOURCC_FORMAT " caps %" GST_PTR_FORMAT,
        GST_FOURCC_ARGS (fourcc), stream->caps);

  } else if (stream->subtype == FOURCC_soun) {
    int version, samplesize;
    guint16 compression_id;

    offset = 32;
    if (len < 36)
      goto corrupt_file;

    version = QT_UINT32 (stsd_data + offset);
    stream->n_channels = QT_UINT16 (stsd_data + offset + 8);
    samplesize = QT_UINT16 (stsd_data + offset + 10);
    compression_id = QT_UINT16 (stsd_data + offset + 12);
    stream->rate = QT_FP32 (stsd_data + offset + 16);

    GST_LOG_OBJECT (qtdemux, "version/rev:      %08x", version);
    GST_LOG_OBJECT (qtdemux, "vendor:           %08x",
        QT_UINT32 (stsd_data + offset + 4));
    GST_LOG_OBJECT (qtdemux, "n_channels:       %d", stream->n_channels);
    GST_LOG_OBJECT (qtdemux, "sample_size:      %d", samplesize);
    GST_LOG_OBJECT (qtdemux, "compression_id:   %d", compression_id);
    GST_LOG_OBJECT (qtdemux, "packet size:      %d",
        QT_UINT16 (stsd_data + offset + 14));
    GST_LOG_OBJECT (qtdemux, "sample rate:      %g", stream->rate);

    if (compression_id == 0xfffe)
      stream->sampled = TRUE;

    /* first assume uncompressed audio */
    stream->bytes_per_sample = samplesize / 8;
    stream->samples_per_frame = stream->n_channels;
    stream->bytes_per_frame = stream->n_channels * stream->bytes_per_sample;
    stream->samples_per_packet = stream->samples_per_frame;
    stream->bytes_per_packet = stream->bytes_per_sample;

    offset = 52;
    switch (fourcc) {
        /* Yes, these have to be hard-coded */
      case FOURCC_MAC6:
      {
        stream->samples_per_packet = 6;
        stream->bytes_per_packet = 1;
        stream->bytes_per_frame = 1 * stream->n_channels;
        stream->bytes_per_sample = 1;
        stream->samples_per_frame = 6 * stream->n_channels;
        break;
      }
      case FOURCC_MAC3:
      {
        stream->samples_per_packet = 3;
        stream->bytes_per_packet = 1;
        stream->bytes_per_frame = 1 * stream->n_channels;
        stream->bytes_per_sample = 1;
        stream->samples_per_frame = 3 * stream->n_channels;
        break;
      }
      case FOURCC_ima4:
      {
        stream->samples_per_packet = 64;
        stream->bytes_per_packet = 34;
        stream->bytes_per_frame = 34 * stream->n_channels;
        stream->bytes_per_sample = 2;
        stream->samples_per_frame = 64 * stream->n_channels;
        break;
      }
      case FOURCC_ulaw:
      case FOURCC_alaw:
      {
        stream->samples_per_packet = 1;
        stream->bytes_per_packet = 1;
        stream->bytes_per_frame = 1 * stream->n_channels;
        stream->bytes_per_sample = 1;
        stream->samples_per_frame = 1 * stream->n_channels;
        break;
      }
      case FOURCC_agsm:
      {
        stream->samples_per_packet = 160;
        stream->bytes_per_packet = 33;
        stream->bytes_per_frame = 33 * stream->n_channels;
        stream->bytes_per_sample = 2;
        stream->samples_per_frame = 160 * stream->n_channels;
        break;
      }
      default:
        break;
    }

    if (version == 0x00010000) {
      switch (fourcc) {
        case FOURCC_twos:
        case FOURCC_sowt:
        case FOURCC_raw_:
          break;
        default:
        {
          /* only parse extra decoding config for non-pcm audio */
          stream->samples_per_packet = QT_UINT32 (stsd_data + offset);
          stream->bytes_per_packet = QT_UINT32 (stsd_data + offset + 4);
          stream->bytes_per_frame = QT_UINT32 (stsd_data + offset + 8);
          stream->bytes_per_sample = QT_UINT32 (stsd_data + offset + 12);

          GST_LOG_OBJECT (qtdemux, "samples/packet:   %d",
              stream->samples_per_packet);
          GST_LOG_OBJECT (qtdemux, "bytes/packet:     %d",
              stream->bytes_per_packet);
          GST_LOG_OBJECT (qtdemux, "bytes/frame:      %d",
              stream->bytes_per_frame);
          GST_LOG_OBJECT (qtdemux, "bytes/sample:     %d",
              stream->bytes_per_sample);

          if (!stream->sampled && stream->bytes_per_packet) {
            stream->samples_per_frame = (stream->bytes_per_frame /
                stream->bytes_per_packet) * stream->samples_per_packet;
            GST_LOG_OBJECT (qtdemux, "samples/frame:    %d",
                stream->samples_per_frame);
          }
          break;
        }
      }
    } else if (version == 0x00020000) {
      union
      {
        gdouble fp;
        guint64 val;
      } qtfp;

      stream->samples_per_packet = QT_UINT32 (stsd_data + offset);
      qtfp.val = QT_UINT64 (stsd_data + offset + 4);
      stream->rate = qtfp.fp;
      stream->n_channels = QT_UINT32 (stsd_data + offset + 12);

      GST_LOG_OBJECT (qtdemux, "samples/packet:   %d",
          stream->samples_per_packet);
      GST_LOG_OBJECT (qtdemux, "sample rate:      %g", stream->rate);
      GST_LOG_OBJECT (qtdemux, "n_channels:       %d", stream->n_channels);

    } else {
      GST_WARNING_OBJECT (qtdemux, "unknown version %08x", version);
    }

    stream->caps = qtdemux_audio_caps (qtdemux, stream, fourcc, NULL, 0,
        &codec);

    switch (fourcc) {
      case FOURCC_in24:
      {
        GNode *enda;
        GNode *in24;

        in24 = qtdemux_tree_get_child_by_type (stsd, FOURCC_in24);

        enda = qtdemux_tree_get_child_by_type (in24, FOURCC_enda);
        if (!enda) {
          wave = qtdemux_tree_get_child_by_type (in24, FOURCC_wave);
          if (wave)
            enda = qtdemux_tree_get_child_by_type (wave, FOURCC_enda);
        }
        if (enda) {
          gst_caps_set_simple (stream->caps,
              "endianness", G_TYPE_INT, G_LITTLE_ENDIAN, NULL);
        }
        break;
      }
      case FOURCC_owma:
      {
        GNode *owma;
        const gchar *owma_data, *codec_name = NULL;
        guint owma_len;
        GstBuffer *buf;
        gint version = 1;
        /* from http://msdn.microsoft.com/en-us/library/dd757720(VS.85).aspx */
        /* FIXME this should also be gst_riff_strf_auds,
         * but the latter one is actually missing bits-per-sample :( */
        typedef struct
        {
          gint16 wFormatTag;
          gint16 nChannels;
          gint32 nSamplesPerSec;
          gint32 nAvgBytesPerSec;
          gint16 nBlockAlign;
          gint16 wBitsPerSample;
          gint16 cbSize;
        } WAVEFORMATEX;
        WAVEFORMATEX *wfex;

        GST_DEBUG_OBJECT (qtdemux, "parse owma");
        owma = qtdemux_tree_get_child_by_type (stsd, FOURCC_owma);
        if (!owma)
          break;
        owma_data = owma->data;
        owma_len = QT_UINT32 (owma_data);
        if (owma_len <= 54) {
          GST_WARNING_OBJECT (qtdemux, "Too small owma header, skipping");
          break;
        }
        wfex = (WAVEFORMATEX *) (owma_data + 36);
        buf = gst_buffer_new_and_alloc (owma_len - 54);
        memcpy (GST_BUFFER_DATA (buf), owma_data + 54, owma_len - 54);
        if (wfex->wFormatTag == 0x0161) {
          codec_name = "Windows Media Audio";
          version = 2;
        } else if (wfex->wFormatTag == 0x0162) {
          codec_name = "Windows Media Audio 9 Pro";
          version = 3;
        } else if (wfex->wFormatTag == 0x0163) {
          codec_name = "Windows Media Audio 9 Lossless";
          /* is that correct? gstffmpegcodecmap.c is missing it, but
           * fluendo codec seems to support it */
          version = 4;
        }

        gst_caps_set_simple (stream->caps,
            "codec_data", GST_TYPE_BUFFER, buf,
            "wmaversion", G_TYPE_INT, version,
            "block_align", G_TYPE_INT, GST_READ_UINT16_LE (&wfex->nBlockAlign),
            "bitrate", G_TYPE_INT, GST_READ_UINT32_LE (&wfex->nAvgBytesPerSec),
            "width", G_TYPE_INT, GST_READ_UINT16_LE (&wfex->wBitsPerSample),
            "depth", G_TYPE_INT, GST_READ_UINT16_LE (&wfex->wBitsPerSample),
            NULL);
        gst_buffer_unref (buf);

        if (codec_name) {
          g_free (codec);
          codec = g_strdup (codec_name);
        }
        break;
      }
      default:
        break;
    }

    if (codec) {
      list = gst_tag_list_new ();
      gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
          GST_TAG_AUDIO_CODEC, codec, NULL);
      g_free (codec);
      codec = NULL;
    }

    mp4a = qtdemux_tree_get_child_by_type (stsd, FOURCC_mp4a);
    wave = NULL;
    esds = NULL;
    if (mp4a) {
      wave = qtdemux_tree_get_child_by_type (mp4a, FOURCC_wave);
      if (wave)
        esds = qtdemux_tree_get_child_by_type (wave, FOURCC_esds);
      if (!esds)
        esds = qtdemux_tree_get_child_by_type (mp4a, FOURCC_esds);
    }


    /* If the fourcc's bottom 16 bits gives 'sm', then the top
       16 bits is a byte-swapped wave-style codec identifier,
       and we can find a WAVE header internally to a 'wave' atom here.
       This can more clearly be thought of as 'ms' as the top 16 bits, and a
       codec id as the bottom 16 bits - but byte-swapped to store in QT (which
       is big-endian).
     */
    if ((fourcc & 0xffff) == (('s' << 8) | 'm')) {
      if (len < offset + 20) {
        GST_WARNING_OBJECT (qtdemux, "No wave atom in MS-style audio");
      } else {
        guint32 datalen = QT_UINT32 (stsd_data + offset + 16);
        const guint8 *data = stsd_data + offset + 16;
        GNode *wavenode;
        GNode *waveheadernode;

        wavenode = g_node_new ((guint8 *) data);
        if (qtdemux_parse_node (qtdemux, wavenode, data, datalen)) {
          const guint8 *waveheader;
          guint32 headerlen;

          waveheadernode = qtdemux_tree_get_child_by_type (wavenode, fourcc);
          if (waveheadernode) {
            waveheader = (const guint8 *) waveheadernode->data;
            headerlen = QT_UINT32 (waveheader);

            if (headerlen > 8) {
              gst_riff_strf_auds *header = NULL;
              GstBuffer *headerbuf;
              GstBuffer *extra;

              waveheader += 8;
              headerlen -= 8;

              headerbuf = gst_buffer_new ();
              GST_BUFFER_DATA (headerbuf) = (guint8 *) waveheader;
              GST_BUFFER_SIZE (headerbuf) = headerlen;

              if (gst_riff_parse_strf_auds (GST_ELEMENT_CAST (qtdemux),
                      headerbuf, &header, &extra)) {
                gst_caps_unref (stream->caps);
                stream->caps = gst_riff_create_audio_caps (header->format, NULL,
                    header, extra, NULL, NULL);

                if (extra)
                  gst_buffer_unref (extra);
              }
            }
          } else
            GST_DEBUG ("Didn't find waveheadernode for this codec");
        }
        g_node_destroy (wavenode);
      }
    } else if (esds) {
#ifdef GSTREAMER_LITE
        if (!gst_qtdemux_handle_esds (qtdemux, stream, esds, list))
            goto corrupt_file;
#else
        gst_qtdemux_handle_esds (qtdemux, stream, esds, list);
#endif // GSTREAMER_LITE
    } else {
      switch (fourcc) {
#if 0
          /* FIXME: what is in the chunk? */
        case FOURCC_QDMC:
        {
          gint len = QT_UINT32 (stsd_data);

          /* seems to be always = 116 = 0x74 */
          break;
        }
#endif
        case FOURCC_QDM2:
        {
          gint len = QT_UINT32 (stsd_data);

          if (len > 0x4C) {
            GstBuffer *buf = gst_buffer_new_and_alloc (len - 0x4C);

            memcpy (GST_BUFFER_DATA (buf), stsd_data + 0x4C, len - 0x4C);
            gst_caps_set_simple (stream->caps,
                "codec_data", GST_TYPE_BUFFER, buf, NULL);
            gst_buffer_unref (buf);
          }
          gst_caps_set_simple (stream->caps,
              "samplesize", G_TYPE_INT, samplesize, NULL);
          break;
        }
        case FOURCC_alac:
        {
          GNode *alac, *wave = NULL;

          /* apparently, m4a has this atom appended directly in the stsd entry,
           * while mov has it in a wave atom */
          alac = qtdemux_tree_get_child_by_type (stsd, FOURCC_alac);
          if (alac) {
            /* alac now refers to stsd entry atom */
            wave = qtdemux_tree_get_child_by_type (alac, FOURCC_wave);
            if (wave)
              alac = qtdemux_tree_get_child_by_type (wave, FOURCC_alac);
            else
              alac = qtdemux_tree_get_child_by_type (alac, FOURCC_alac);
          }
          if (alac) {
            gint len = QT_UINT32 (alac->data);
            GstBuffer *buf;

            if (len < 36) {
              GST_DEBUG_OBJECT (qtdemux,
                  "discarding alac atom with unexpected len %d", len);
            } else {
              /* codec-data contains alac atom size and prefix,
               * ffmpeg likes it that way, not quite gst-ish though ...*/
              buf = gst_buffer_new_and_alloc (len);
              memcpy (GST_BUFFER_DATA (buf), alac->data, len);
              gst_caps_set_simple (stream->caps,
                  "codec_data", GST_TYPE_BUFFER, buf, NULL);
              gst_buffer_unref (buf);
            }
          }
          gst_caps_set_simple (stream->caps,
              "samplesize", G_TYPE_INT, samplesize, NULL);
          break;
        }
        case FOURCC_samr:
        {
          gint len = QT_UINT32 (stsd_data);

          if (len > 0x34) {
            GstBuffer *buf = gst_buffer_new_and_alloc (len - 0x34);

            memcpy (GST_BUFFER_DATA (buf), stsd_data + 0x34, len - 0x34);

            gst_caps_set_simple (stream->caps,
                "codec_data", GST_TYPE_BUFFER, buf, NULL);
            gst_buffer_unref (buf);
          }
          break;
        }
        default:
          break;
      }
    }
    GST_INFO_OBJECT (qtdemux,
        "type %" GST_FOURCC_FORMAT " caps %" GST_PTR_FORMAT,
        GST_FOURCC_ARGS (fourcc), stream->caps);

  } else if (stream->subtype == FOURCC_strm) {
    if (fourcc == FOURCC_rtsp) {
      stream->redirect_uri = qtdemux_get_rtsp_uri_from_hndl (qtdemux, minf);
    } else {
      GST_INFO_OBJECT (qtdemux, "unhandled stream type %" GST_FOURCC_FORMAT,
          GST_FOURCC_ARGS (fourcc));
      goto unknown_stream;
    }
    stream->sampled = TRUE;
  } else if (stream->subtype == FOURCC_subp || stream->subtype == FOURCC_text) {

    stream->sampled = TRUE;

    offset = 16;

    stream->caps =
        qtdemux_sub_caps (qtdemux, stream, fourcc, stsd_data, &codec);
    if (codec) {
      list = gst_tag_list_new ();
      gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
          GST_TAG_SUBTITLE_CODEC, codec, NULL);
      g_free (codec);
      codec = NULL;
    }

    /* hunt for sort-of codec data */
    switch (fourcc) {
      case FOURCC_mp4s:
      {
        guint len;
        const guint8 *data;

        /* look for palette */
        /* target mp4s atom */
        len = QT_UINT32 (stsd_data + offset);
        data = stsd_data + offset;
        /* verify sufficient length,
         * and esds present with decConfigDescr of expected size and position */
        if ((len >= 106 + 8)
            && (QT_FOURCC (data + 8 + 8 + 4) == FOURCC_esds)
            && (QT_UINT16 (data + 8 + 40) == 0x0540)) {
          GstStructure *s;
          guint32 clut[16];
          gint i;

          /* move to decConfigDescr data */
          data = data + 8 + 42;
          for (i = 0; i < 16; i++) {
            clut[i] = QT_UINT32 (data);
            data += 4;
          }

          s = gst_structure_new ("application/x-gst-dvd", "event",
              G_TYPE_STRING, "dvd-spu-clut-change",
              "clut00", G_TYPE_INT, clut[0], "clut01", G_TYPE_INT, clut[1],
              "clut02", G_TYPE_INT, clut[2], "clut03", G_TYPE_INT, clut[3],
              "clut04", G_TYPE_INT, clut[4], "clut05", G_TYPE_INT, clut[5],
              "clut06", G_TYPE_INT, clut[6], "clut07", G_TYPE_INT, clut[7],
              "clut08", G_TYPE_INT, clut[8], "clut09", G_TYPE_INT, clut[9],
              "clut10", G_TYPE_INT, clut[10], "clut11", G_TYPE_INT, clut[11],
              "clut12", G_TYPE_INT, clut[12], "clut13", G_TYPE_INT, clut[13],
              "clut14", G_TYPE_INT, clut[14], "clut15", G_TYPE_INT, clut[15],
              NULL);

          /* store event and trigger custom processing */
          stream->pending_event =
              gst_event_new_custom (GST_EVENT_CUSTOM_DOWNSTREAM, s);
          stream->need_process = TRUE;
        }
        break;
      }
      default:
        break;
    }
  } else {
    goto unknown_stream;
  }

  /* promote to sampled format */
  if (stream->fourcc == FOURCC_samr) {
    /* force mono 8000 Hz for AMR */
    stream->sampled = TRUE;
    stream->n_channels = 1;
    stream->rate = 8000;
  } else if (stream->fourcc == FOURCC_sawb) {
    /* force mono 16000 Hz for AMR-WB */
    stream->sampled = TRUE;
    stream->n_channels = 1;
    stream->rate = 16000;
  } else if (stream->fourcc == FOURCC_mp4a) {
    stream->sampled = TRUE;
  }

  /* collect sample information */
  if (!qtdemux_stbl_init (qtdemux, stream, stbl))
    goto samples_failed;

  if (qtdemux->fragmented) {
    guint32 dummy;
    guint64 offset;

    /* need all moov samples as basis; probably not many if any at all */
    /* prevent moof parsing taking of at this time */
    offset = qtdemux->moof_offset;
    qtdemux->moof_offset = 0;
    if (stream->n_samples &&
        !qtdemux_parse_samples (qtdemux, stream, stream->n_samples - 1)) {
      qtdemux->moof_offset = offset;
      goto samples_failed;
    }
    qtdemux->moof_offset = 0;
    /* movie duration more reliable in this case (e.g. mehd) */
    if (qtdemux->segment.duration &&
        GST_CLOCK_TIME_IS_VALID (qtdemux->segment.duration))
      stream->duration = gst_util_uint64_scale (qtdemux->segment.duration,
          stream->timescale, GST_SECOND);
    /* need defaults for fragments */
    qtdemux_parse_trex (qtdemux, stream, &dummy, &dummy, &dummy);
  }

  /* configure segments */
  if (!qtdemux_parse_segments (qtdemux, stream, trak))
    goto segments_failed;

  /* add some language tag, if useful */
  if (stream->lang_id[0] != '\0' && strcmp (stream->lang_id, "unk") &&
      strcmp (stream->lang_id, "und")) {
    const gchar *lang_code;

    if (!list)
      list = gst_tag_list_new ();

    /* convert ISO 639-2 code to ISO 639-1 */
    lang_code = gst_tag_get_language_code (stream->lang_id);
    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
        GST_TAG_LANGUAGE_CODE, (lang_code) ? lang_code : stream->lang_id, NULL);
  }

  /* now we are ready to add the stream */
  if (qtdemux->n_streams >= GST_QTDEMUX_MAX_STREAMS)
    goto too_many_streams;

  stream->pending_tags = list;
  qtdemux->streams[qtdemux->n_streams] = stream;
  qtdemux->n_streams++;
  GST_DEBUG_OBJECT (qtdemux, "n_streams is now %d", qtdemux->n_streams);

  return TRUE;

/* ERRORS */
corrupt_file:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DEMUX,
        (_("This file is corrupt and cannot be played.")), (NULL));
    g_free (stream);
    return FALSE;
  }
error_encrypted:
  {
    GST_ELEMENT_ERROR (qtdemux, STREAM, DECRYPT, (NULL), (NULL));
    g_free (stream);
    return FALSE;
  }
samples_failed:
segments_failed:
  {
    /* we posted an error already */
    /* free stbl sub-atoms */
    gst_qtdemux_stbl_free (stream);
    g_free (stream);
    return FALSE;
  }
unknown_stream:
  {
    GST_INFO_OBJECT (qtdemux, "unknown subtype %" GST_FOURCC_FORMAT,
        GST_FOURCC_ARGS (stream->subtype));
    g_free (stream);
    return TRUE;
  }
too_many_streams:
  {
    GST_ELEMENT_WARNING (qtdemux, STREAM, DEMUX,
        (_("This file contains too many streams. Only playing first %d"),
            GST_QTDEMUX_MAX_STREAMS), (NULL));
    return TRUE;
  }
}

static GstFlowReturn
qtdemux_expose_streams (GstQTDemux * qtdemux)
{
  gint i;
  GstFlowReturn ret = GST_FLOW_OK;

  GST_DEBUG_OBJECT (qtdemux, "exposing streams");

  for (i = 0; ret == GST_FLOW_OK && i < qtdemux->n_streams; i++) {
    QtDemuxStream *stream = qtdemux->streams[i];
    guint32 sample_num = 0;
    guint samples = 20;
    GArray *durations;
    GstTagList *list;

    GST_DEBUG_OBJECT (qtdemux, "stream %d, id %d, fourcc %" GST_FOURCC_FORMAT,
        i, stream->track_id, GST_FOURCC_ARGS (stream->fourcc));

    if (qtdemux->fragmented) {
      /* need all moov samples first */
      GST_OBJECT_LOCK (qtdemux);
      while (stream->n_samples == 0)
        if ((ret = qtdemux_add_fragmented_samples (qtdemux)) != GST_FLOW_OK)
          break;
      GST_OBJECT_UNLOCK (qtdemux);
    } else {
      /* discard any stray moof */
      qtdemux->moof_offset = 0;
    }

    /* prepare braking */
    if (ret != GST_FLOW_ERROR)
      ret = GST_FLOW_OK;

    /* in pull mode, we should have parsed some sample info by now;
     * and quite some code will not handle no samples.
     * in push mode, we'll just have to deal with it */
    if (G_UNLIKELY (qtdemux->pullbased && !stream->n_samples)) {
      GST_DEBUG_OBJECT (qtdemux, "no samples for stream; discarding");
      gst_qtdemux_stream_free (qtdemux, stream);
      memmove (&(qtdemux->streams[i]), &(qtdemux->streams[i + 1]),
          sizeof (QtDemuxStream *) * (GST_QTDEMUX_MAX_STREAMS - i - 1));
      qtdemux->streams[GST_QTDEMUX_MAX_STREAMS - 1] = NULL;
      qtdemux->n_streams--;
      i--;
      continue;
    }

    /* parse number of initial sample to set frame rate cap */
    while (sample_num < stream->n_samples && sample_num < samples) {
      if (!qtdemux_parse_samples (qtdemux, stream, sample_num))
        break;
      ++sample_num;
    }
    /* collect and sort durations */
    samples = MIN (stream->stbl_index + 1, samples);
    GST_DEBUG_OBJECT (qtdemux, "%d samples for framerate", samples);
    if (samples) {
      durations = g_array_sized_new (FALSE, FALSE, sizeof (guint32), samples);
      sample_num = 0;
      while (sample_num < samples) {
        g_array_append_val (durations, stream->samples[sample_num].duration);
        sample_num++;
      }
      g_array_sort (durations, less_than);
      stream->min_duration = g_array_index (durations, guint32, samples / 2);
      g_array_free (durations, TRUE);
    }

    /* now we have all info and can expose */
    list = stream->pending_tags;
    stream->pending_tags = NULL;
    gst_qtdemux_add_stream (qtdemux, stream, list);
  }

  gst_element_no_more_pads (GST_ELEMENT_CAST (qtdemux));

  /* check if we should post a redirect in case there is a single trak
   * and it is a redirecting trak */
  if (qtdemux->n_streams == 1 && qtdemux->streams[0]->redirect_uri != NULL) {
    GstMessage *m;

    qtdemux_post_global_tags (qtdemux);

    GST_INFO_OBJECT (qtdemux, "Issuing a redirect due to a single track with "
        "an external content");
    m = gst_message_new_element (GST_OBJECT_CAST (qtdemux),
        gst_structure_new ("redirect",
            "new-location", G_TYPE_STRING, qtdemux->streams[0]->redirect_uri,
            NULL));
    gst_element_post_message (GST_ELEMENT_CAST (qtdemux), m);
    qtdemux->posted_redirect = TRUE;
  }

  return ret;
}

/* check if major or compatible brand is 3GP */
static inline gboolean
qtdemux_is_brand_3gp (GstQTDemux * qtdemux, gboolean major)
{
  if (major) {
    return ((qtdemux->major_brand & GST_MAKE_FOURCC (255, 255, 0, 0)) ==
        GST_MAKE_FOURCC ('3', 'g', 0, 0));
  } else if (qtdemux->comp_brands != NULL) {
    guint8 *data = GST_BUFFER_DATA (qtdemux->comp_brands);
    guint size = GST_BUFFER_SIZE (qtdemux->comp_brands);
    gboolean res = FALSE;

    while (size >= 4) {
      res = res || ((QT_FOURCC (data) & GST_MAKE_FOURCC (255, 255, 0, 0)) ==
          GST_MAKE_FOURCC ('3', 'g', 0, 0));
      data += 4;
      size -= 4;
    }
    return res;
  } else {
    return FALSE;
  }
}

/* check if tag is a spec'ed 3GP tag keyword storing a string */
static inline gboolean
qtdemux_is_string_tag_3gp (GstQTDemux * qtdemux, guint32 fourcc)
{
  return fourcc == FOURCC_cprt || fourcc == FOURCC_gnre || fourcc == FOURCC_titl
      || fourcc == FOURCC_dscp || fourcc == FOURCC_perf || fourcc == FOURCC_auth
      || fourcc == FOURCC_albm;
}

static void
qtdemux_tag_add_location (GstQTDemux * qtdemux, const char *tag,
    const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  int offset;
  char *name;
  gchar *data;
  gdouble longitude, latitude, altitude;
  gint len;

  len = QT_UINT32 (node->data);
  if (len <= 14)
    goto short_read;

  data = node->data;
  offset = 14;

  /* TODO: language code skipped */

  name = gst_tag_freeform_string_to_utf8 (data + offset, -1, env_vars);

  if (!name) {
    /* do not alarm in trivial case, but bail out otherwise */
    if (*(data + offset) != 0) {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8, "
          "giving up", tag);
    }
  } else {
    gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
        GST_TAG_GEO_LOCATION_NAME, name, NULL);
    offset += strlen (name);
    g_free (name);
  }

  if (len < offset + 2 + 4 + 4 + 4)
    goto short_read;

  /* +1 +1 = skip null-terminator and location role byte */
  offset += 1 + 1;
  /* table in spec says unsigned, semantics say negative has meaning ... */
  longitude = QT_SFP32 (data + offset);

  offset += 4;
  latitude = QT_SFP32 (data + offset);

  offset += 4;
  altitude = QT_SFP32 (data + offset);

  /* one invalid means all are invalid */
  if (longitude >= -180.0 && longitude <= 180.0 &&
      latitude >= -90.0 && latitude <= 90.0) {
    gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
        GST_TAG_GEO_LOCATION_LATITUDE, latitude,
        GST_TAG_GEO_LOCATION_LONGITUDE, longitude,
        GST_TAG_GEO_LOCATION_ELEVATION, altitude, NULL);
  }

  /* TODO: no GST_TAG_, so astronomical body and additional notes skipped */

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP location");
    return;
  }
}


static void
qtdemux_tag_add_year (GstQTDemux * qtdemux, const char *tag, const char *dummy,
    GNode * node)
{
  guint16 y;
  GDate *date;
  gint len;

  len = QT_UINT32 (node->data);
  if (len < 14)
    return;

  y = QT_UINT16 ((guint8 *) node->data + 12);
  if (y == 0) {
    GST_DEBUG_OBJECT (qtdemux, "year: %u is not a valid year", y);
    return;
  }
  GST_DEBUG_OBJECT (qtdemux, "year: %u", y);

  date = g_date_new_dmy (1, 1, y);
  gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE, tag, date, NULL);
  g_date_free (date);
}

static void
qtdemux_tag_add_classification (GstQTDemux * qtdemux, const char *tag,
    const char *dummy, GNode * node)
{
  int offset;
  char *tag_str = NULL;
  guint8 *entity;
  guint16 table;
  gint len;

  len = QT_UINT32 (node->data);
  if (len <= 20)
    goto short_read;

  offset = 12;
  entity = (guint8 *) node->data + offset;
  if (entity[0] == 0 || entity[1] == 0 || entity[2] == 0 || entity[3] == 0) {
    GST_DEBUG_OBJECT (qtdemux,
        "classification info: %c%c%c%c invalid classification entity",
        entity[0], entity[1], entity[2], entity[3]);
    return;
  }

  offset += 4;
  table = QT_UINT16 ((guint8 *) node->data + offset);

  /* Language code skipped */

  offset += 4;

  /* Tag format: "XXXX://Y[YYYY]/classification info string"
   * XXXX: classification entity, fixed length 4 chars.
   * Y[YYYY]: classification table, max 5 chars.
   */
  tag_str = g_strdup_printf ("----://%u/%s",
      table, (char *) node->data + offset);

  /* memcpy To be sure we're preserving byte order */
  memcpy (tag_str, entity, 4);
  GST_DEBUG_OBJECT (qtdemux, "classification info: %s", tag_str);

  gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_APPEND, tag,
      tag_str, NULL);

  g_free (tag_str);

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP classification");
    return;
  }
}

static gboolean
qtdemux_tag_add_str_full (GstQTDemux * qtdemux, const char *tag,
    const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  GNode *data;
  char *s;
  int len;
  guint32 type;
  int offset;
  gboolean ret = TRUE;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000001 && len > 16) {
      s = gst_tag_freeform_string_to_utf8 ((char *) data->data + 16, len - 16,
          env_vars);
      if (s) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (s));
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE, tag, s,
            NULL);
        g_free (s);
      } else {
        GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8", tag);
      }
    }
  } else {
    len = QT_UINT32 (node->data);
    type = QT_UINT32 ((guint8 *) node->data + 4);
    if ((type >> 24) == 0xa9) {
      /* Type starts with the (C) symbol, so the next 32 bits are
       * the language code, which we ignore */
      offset = 12;
      GST_DEBUG_OBJECT (qtdemux, "found international text tag");
    } else if (len > 14 && qtdemux_is_string_tag_3gp (qtdemux,
            QT_FOURCC ((guint8 *) node->data + 4))) {
      guint32 type = QT_UINT32 ((guint8 *) node->data + 8);

      /* we go for 3GP style encoding if major brands claims so,
       * or if no hope for data be ok UTF-8, and compatible 3GP brand present */
      if (qtdemux_is_brand_3gp (qtdemux, TRUE) ||
          (qtdemux_is_brand_3gp (qtdemux, FALSE) &&
              ((type & 0x00FFFFFF) == 0x0) && (type >> 24 <= 0xF))) {
        offset = 14;
        /* 16-bit Language code is ignored here as well */
        GST_DEBUG_OBJECT (qtdemux, "found 3gpp text tag");
      } else {
        goto normal;
      }
    } else {
    normal:
      offset = 8;
      GST_DEBUG_OBJECT (qtdemux, "found normal text tag");
      ret = FALSE;              /* may have to fallback */
    }
    s = gst_tag_freeform_string_to_utf8 ((char *) node->data + offset,
        len - offset, env_vars);
    if (s) {
      GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (s));
      gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE, tag, s, NULL);
      g_free (s);
      ret = TRUE;
    } else {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8", tag);
    }
  }
  return ret;
}

static void
qtdemux_tag_add_str (GstQTDemux * qtdemux, const char *tag,
    const char *dummy, GNode * node)
{
  qtdemux_tag_add_str_full (qtdemux, tag, dummy, node);
}

static void
qtdemux_tag_add_keywords (GstQTDemux * qtdemux, const char *tag,
    const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  guint8 *data;
  char *s, *t, *k = NULL;
  int len;
  int offset;
  int count;

  /* first try normal string tag if major brand not 3GP */
  if (!qtdemux_is_brand_3gp (qtdemux, TRUE)) {
    if (!qtdemux_tag_add_str_full (qtdemux, tag, dummy, node)) {
      /* hm, that did not work, maybe 3gpp storage in non-3gpp major brand;
       * let's try it 3gpp way after minor safety check */
      data = node->data;
      if (QT_UINT32 (data) < 15 || !qtdemux_is_brand_3gp (qtdemux, FALSE))
        return;
    } else
      return;
  }

  GST_DEBUG_OBJECT (qtdemux, "found 3gpp keyword tag");

  data = node->data;

  len = QT_UINT32 (data);
  if (len < 15)
    goto short_read;

  count = QT_UINT8 (data + 14);
  offset = 15;
  for (; count; count--) {
    gint slen;

    if (offset + 1 > len)
      goto short_read;
    slen = QT_UINT8 (data + offset);
    offset += 1;
    if (offset + slen > len)
      goto short_read;
    s = gst_tag_freeform_string_to_utf8 ((char *) node->data + offset,
        slen, env_vars);
    if (s) {
      GST_DEBUG_OBJECT (qtdemux, "adding keyword %s", GST_STR_NULL (s));
      if (k) {
        t = g_strjoin (",", k, s, NULL);
        g_free (s);
        g_free (k);
        k = t;
      } else {
        k = s;
      }
    } else {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert keyword to UTF-8");
    }
    offset += slen;
  }

done:
  if (k) {
    GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (k));
    gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE, tag, k, NULL);
  }
  g_free (k);

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP keywords");
    goto done;
  }
}

static void
qtdemux_tag_add_num (GstQTDemux * qtdemux, const char *tag1,
    const char *tag2, GNode * node)
{
  GNode *data;
  int len;
  int type;
  int n1, n2;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000000 && len >= 22) {
      n1 = QT_UINT16 ((guint8 *) data->data + 18);
      n2 = QT_UINT16 ((guint8 *) data->data + 20);
      if (n1 > 0) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s=%d", tag1, n1);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
            tag1, n1, NULL);
      }
      if (n2 > 0) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s=%d", tag2, n2);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
            tag2, n2, NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_tmpo (GstQTDemux * qtdemux, const char *tag1, const char *dummy,
    GNode * node)
{
  GNode *data;
  int len;
  int type;
  int n1;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have tempo tag, type=%d,len=%d", type, len);
    /* some files wrongly have a type 0x0f=15, but it should be 0x15 */
    if ((type == 0x00000015 || type == 0x0000000f) && len >= 18) {
      n1 = QT_UINT16 ((guint8 *) data->data + 16);
      if (n1) {
        /* do not add bpm=0 */
        GST_DEBUG_OBJECT (qtdemux, "adding tag %d", n1);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
            tag1, (gdouble) n1, NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_uint32 (GstQTDemux * qtdemux, const char *tag1,
    const char *dummy, GNode * node)
{
  GNode *data;
  int len;
  int type;
  guint32 num;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have %s tag, type=%d,len=%d", tag1, type, len);
    /* some files wrongly have a type 0x0f=15, but it should be 0x15 */
    if ((type == 0x00000015 || type == 0x0000000f) && len >= 20) {
      num = QT_UINT32 ((guint8 *) data->data + 16);
      if (num) {
        /* do not add num=0 */
        GST_DEBUG_OBJECT (qtdemux, "adding tag %d", num);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
            tag1, num, NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_covr (GstQTDemux * qtdemux, const char *tag1, const char *dummy,
    GNode * node)
{
  GNode *data;
  int len;
  int type;
  GstBuffer *buf;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have covr tag, type=%d,len=%d", type, len);
    if ((type == 0x0000000d || type == 0x0000000e) && len > 16) {
      if ((buf = gst_tag_image_data_to_image_buffer ((guint8 *) data->data + 16,
                  len - 16, GST_TAG_IMAGE_TYPE_NONE))) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag size %d", len - 16);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
            tag1, buf, NULL);
        gst_buffer_unref (buf);
      }
    }
  }
}

static void
qtdemux_tag_add_date (GstQTDemux * qtdemux, const char *tag, const char *dummy,
    GNode * node)
{
  GNode *data;
  char *s;
  int len;
  int type;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000001 && len > 16) {
      guint y, m = 1, d = 1;
      gint ret;

      s = g_strndup ((char *) data->data + 16, len - 16);
      GST_DEBUG_OBJECT (qtdemux, "adding date '%s'", s);
      ret = sscanf (s, "%u-%u-%u", &y, &m, &d);
      if (ret >= 1 && y > 1500 && y < 3000) {
        GDate *date;

        date = g_date_new_dmy (d, m, y);
        gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE, tag,
            date, NULL);
        g_date_free (date);
      } else {
        GST_DEBUG_OBJECT (qtdemux, "could not parse date string '%s'", s);
      }
      g_free (s);
    }
  }
}

static void
qtdemux_tag_add_gnre (GstQTDemux * qtdemux, const char *tag, const char *dummy,
    GNode * node)
{
  GNode *data;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);

  /* re-route to normal string tag if major brand says so
   * or no data atom and compatible brand suggests so */
  if (qtdemux_is_brand_3gp (qtdemux, TRUE) ||
      (qtdemux_is_brand_3gp (qtdemux, FALSE) && !data)) {
    qtdemux_tag_add_str (qtdemux, tag, dummy, node);
    return;
  }

  if (data) {
    guint len, type, n;

    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000000 && len >= 18) {
      n = QT_UINT16 ((guint8 *) data->data + 16);
      if (n > 0) {
        const gchar *genre;

        genre = gst_tag_id3_genre_get (n - 1);
        if (genre != NULL) {
          GST_DEBUG_OBJECT (qtdemux, "adding %d [%s]", n, genre);
          gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_REPLACE,
              tag, genre, NULL);
        }
      }
    }
  }
}

static void
qtdemux_add_double_tag_from_str (GstQTDemux * demux, const gchar * tag,
    guint8 * data, guint32 datasize)
{
  gdouble value;
  gchar *datacopy;

  /* make a copy to have \0 at the end */
  datacopy = g_strndup ((gchar *) data, datasize);

  /* convert the str to double */
  if (sscanf (datacopy, "%lf", &value) == 1) {
    GST_DEBUG_OBJECT (demux, "adding tag: %s [%s]", tag, datacopy);
    gst_tag_list_add (demux->tag_list, GST_TAG_MERGE_REPLACE, tag, value, NULL);
  } else {
    GST_WARNING_OBJECT (demux, "Failed to parse double from string: %s",
        datacopy);
  }
  g_free (datacopy);
}


static void
qtdemux_tag_add_revdns (GstQTDemux * demux, const char *tag,
    const char *tag_bis, GNode * node)
{
  GNode *mean;
  GNode *name;
  GNode *data;
  guint32 meansize;
  guint32 namesize;
  guint32 datatype;
  guint32 datasize;
  const gchar *meanstr;
  const gchar *namestr;

  /* checking the whole ---- atom size for consistency */
  if (QT_UINT32 (node->data) <= 4 + 12 + 12 + 16) {
    GST_WARNING_OBJECT (demux, "Tag ---- atom is too small, ignoring");
    return;
  }

  mean = qtdemux_tree_get_child_by_type (node, FOURCC_mean);
  if (!mean) {
    GST_WARNING_OBJECT (demux, "No 'mean' atom found");
    return;
  }

  meansize = QT_UINT32 (mean->data);
  if (meansize <= 12) {
    GST_WARNING_OBJECT (demux, "Small mean atom, ignoring the whole tag");
    return;
  }
  meanstr = ((gchar *) mean->data) + 12;

  name = qtdemux_tree_get_child_by_type (node, FOURCC_name);
  if (!name) {
    GST_WARNING_OBJECT (demux, "'name' atom not found, ignoring tag");
    return;
  }

  namesize = QT_UINT32 (name->data);
  if (namesize <= 12) {
    GST_WARNING_OBJECT (demux, "'name' atom is too small, ignoring tag");
    return;
  }
  namestr = ((gchar *) name->data) + 12;

  /*
   * Data atom is:
   * uint32 - size
   * uint32 - name
   * uint8  - version
   * uint24 - data type
   * uint32 - all 0
   * rest   - the data
   */
  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (!data) {
    GST_WARNING_OBJECT (demux, "No data atom in this tag");
    return;
  }
  datasize = QT_UINT32 (data->data);
  if (datasize <= 16) {
    GST_WARNING_OBJECT (demux, "Data atom too small");
    return;
  }
  datatype = QT_UINT32 (((gchar *) data->data) + 8) & 0xFFFFFF;

  if (strncmp (meanstr, "com.apple.iTunes", meansize - 12) == 0) {
    static const struct
    {
      const gchar name[28];
      const gchar tag[28];
    } tags[] = {
      {
      "replaygain_track_gain", GST_TAG_TRACK_GAIN}, {
      "replaygain_track_peak", GST_TAG_TRACK_PEAK}, {
      "replaygain_album_gain", GST_TAG_ALBUM_GAIN}, {
      "replaygain_album_peak", GST_TAG_ALBUM_PEAK}, {
      "MusicBrainz Track Id", GST_TAG_MUSICBRAINZ_TRACKID}, {
      "MusicBrainz Artist Id", GST_TAG_MUSICBRAINZ_ARTISTID}, {
      "MusicBrainz Album Id", GST_TAG_MUSICBRAINZ_ALBUMID}, {
      "MusicBrainz Album Artist Id", GST_TAG_MUSICBRAINZ_ALBUMARTISTID}
    };
    int i;

    for (i = 0; i < G_N_ELEMENTS (tags); ++i) {
      if (!g_ascii_strncasecmp (tags[i].name, namestr, namesize - 12)) {
        switch (gst_tag_get_type (tags[i].tag)) {
          case G_TYPE_DOUBLE:
            qtdemux_add_double_tag_from_str (demux, tags[i].tag,
                ((guint8 *) data->data) + 16, datasize - 16);
            break;
          case G_TYPE_STRING:
            qtdemux_tag_add_str (demux, tags[i].tag, NULL, node);
            break;
          default:
            /* not reached */
            break;
        }
        break;
      }
    }
    if (i == G_N_ELEMENTS (tags))
      goto unknown_tag;
  } else {
    goto unknown_tag;
  }

  return;

/* errors */
unknown_tag:
  {
    gchar *namestr_dbg;
    gchar *meanstr_dbg;

    meanstr_dbg = g_strndup (meanstr, meansize - 12);
    namestr_dbg = g_strndup (namestr, namesize - 12);

    GST_WARNING_OBJECT (demux, "This tag %s:%s type:%u is not mapped, "
        "file a bug at bugzilla.gnome.org", meanstr_dbg, namestr_dbg, datatype);

    g_free (namestr_dbg);
    g_free (meanstr_dbg);
    return;
  }
}

typedef void (*GstQTDemuxAddTagFunc) (GstQTDemux * demux,
    const char *tag, const char *tag_bis, GNode * node);

/* unmapped tags
FOURCC_pcst -> if media is a podcast -> bool
FOURCC_cpil -> if media is part of a compilation -> bool
FOURCC_pgap -> if media is part of a gapless context -> bool
FOURCC_tven -> the tv episode id e.g. S01E23 -> str
*/

static const struct
{
  guint32 fourcc;
  const gchar *gst_tag;
  const gchar *gst_tag_bis;
  const GstQTDemuxAddTagFunc func;
} add_funcs[] = {
  {
  FOURCC__nam, GST_TAG_TITLE, NULL, qtdemux_tag_add_str}, {
  FOURCC_titl, GST_TAG_TITLE, NULL, qtdemux_tag_add_str}, {
  FOURCC__grp, GST_TAG_GROUPING, NULL, qtdemux_tag_add_str}, {
  FOURCC__wrt, GST_TAG_COMPOSER, NULL, qtdemux_tag_add_str}, {
  FOURCC__ART, GST_TAG_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_aART, GST_TAG_ALBUM_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_perf, GST_TAG_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_auth, GST_TAG_COMPOSER, NULL, qtdemux_tag_add_str}, {
  FOURCC__alb, GST_TAG_ALBUM, NULL, qtdemux_tag_add_str}, {
  FOURCC_albm, GST_TAG_ALBUM, NULL, qtdemux_tag_add_str}, {
  FOURCC_cprt, GST_TAG_COPYRIGHT, NULL, qtdemux_tag_add_str}, {
  FOURCC__cpy, GST_TAG_COPYRIGHT, NULL, qtdemux_tag_add_str}, {
  FOURCC__cmt, GST_TAG_COMMENT, NULL, qtdemux_tag_add_str}, {
  FOURCC__des, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC_desc, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC_dscp, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC__lyr, GST_TAG_LYRICS, NULL, qtdemux_tag_add_str}, {
  FOURCC__day, GST_TAG_DATE, NULL, qtdemux_tag_add_date}, {
  FOURCC_yrrc, GST_TAG_DATE, NULL, qtdemux_tag_add_year}, {
  FOURCC__too, GST_TAG_ENCODER, NULL, qtdemux_tag_add_str}, {
  FOURCC__inf, GST_TAG_COMMENT, NULL, qtdemux_tag_add_str}, {
  FOURCC_trkn, GST_TAG_TRACK_NUMBER, GST_TAG_TRACK_COUNT, qtdemux_tag_add_num}, {
  FOURCC_disk, GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_ALBUM_VOLUME_COUNT,
        qtdemux_tag_add_num}, {
  FOURCC_disc, GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_ALBUM_VOLUME_COUNT,
        qtdemux_tag_add_num}, {
  FOURCC__gen, GST_TAG_GENRE, NULL, qtdemux_tag_add_str}, {
  FOURCC_gnre, GST_TAG_GENRE, NULL, qtdemux_tag_add_gnre}, {
  FOURCC_tmpo, GST_TAG_BEATS_PER_MINUTE, NULL, qtdemux_tag_add_tmpo}, {
  FOURCC_covr, GST_TAG_PREVIEW_IMAGE, NULL, qtdemux_tag_add_covr}, {
  FOURCC_sonm, GST_TAG_TITLE_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soal, GST_TAG_ALBUM_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soar, GST_TAG_ARTIST_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soaa, GST_TAG_ALBUM_ARTIST_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soco, GST_TAG_COMPOSER_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_sosn, GST_TAG_SHOW_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_tvsh, GST_TAG_SHOW_NAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_tvsn, GST_TAG_SHOW_SEASON_NUMBER, NULL, qtdemux_tag_add_uint32}, {
  FOURCC_tves, GST_TAG_SHOW_EPISODE_NUMBER, NULL, qtdemux_tag_add_uint32}, {
  FOURCC_kywd, GST_TAG_KEYWORDS, NULL, qtdemux_tag_add_keywords}, {
  FOURCC_keyw, GST_TAG_KEYWORDS, NULL, qtdemux_tag_add_str}, {
  FOURCC__enc, GST_TAG_ENCODER, NULL, qtdemux_tag_add_str}, {
  FOURCC_loci, GST_TAG_GEO_LOCATION_NAME, NULL, qtdemux_tag_add_location}, {
  FOURCC_clsf, GST_QT_DEMUX_CLASSIFICATION_TAG, NULL,
        qtdemux_tag_add_classification}, {

    /* This is a special case, some tags are stored in this
     * 'reverse dns naming', according to:
     * http://atomicparsley.sourceforge.net/mpeg-4files.html and
     * bug #614471
     */
  FOURCC_____, "", NULL, qtdemux_tag_add_revdns}
};

static void
qtdemux_tag_add_blob (GNode * node, GstQTDemux * demux)
{
  gint len;
  guint8 *data;
  GstBuffer *buf;
  gchar *media_type;
  const gchar *style;
  GstCaps *caps;
  guint i;
  guint8 ndata[4];

  data = node->data;
  len = QT_UINT32 (data);
  buf = gst_buffer_new_and_alloc (len);
  memcpy (GST_BUFFER_DATA (buf), data, len);

  /* heuristic to determine style of tag */
  if (QT_FOURCC (data + 4) == FOURCC_____ ||
      (len > 8 + 12 && QT_FOURCC (data + 12) == FOURCC_data))
    style = "itunes";
  else if (demux->major_brand == FOURCC_qt__)
    style = "quicktime";
  /* fall back to assuming iso/3gp tag style */
  else
    style = "iso";

  /* santize the name for the caps. */
  for (i = 0; i < 4; i++) {
    guint8 d = data[4 + i];
    if (g_ascii_isalnum (d))
      ndata[i] = g_ascii_tolower (d);
    else
      ndata[i] = '_';
  }

  media_type = g_strdup_printf ("application/x-gst-qt-%c%c%c%c-tag",
      ndata[0], ndata[1], ndata[2], ndata[3]);
  GST_DEBUG_OBJECT (demux, "media type %s", media_type);

  caps = gst_caps_new_simple (media_type, "style", G_TYPE_STRING, style, NULL);
  gst_buffer_set_caps (buf, caps);
  gst_caps_unref (caps);
  g_free (media_type);

  GST_DEBUG_OBJECT (demux, "adding private tag; size %d, caps %" GST_PTR_FORMAT,
      GST_BUFFER_SIZE (buf), caps);

  gst_tag_list_add (demux->tag_list, GST_TAG_MERGE_APPEND,
      GST_QT_DEMUX_PRIVATE_TAG, buf, NULL);
  gst_buffer_unref (buf);
}

static void
qtdemux_parse_udta (GstQTDemux * qtdemux, GNode * udta)
{
  GNode *meta;
  GNode *ilst;
  GNode *xmp_;
  GNode *node;
  gint i;

  meta = qtdemux_tree_get_child_by_type (udta, FOURCC_meta);
  if (meta != NULL) {
    ilst = qtdemux_tree_get_child_by_type (meta, FOURCC_ilst);
    if (ilst == NULL) {
      GST_LOG_OBJECT (qtdemux, "no ilst");
      return;
    }
  } else {
    ilst = udta;
    GST_LOG_OBJECT (qtdemux, "no meta so using udta itself");
  }

  GST_DEBUG_OBJECT (qtdemux, "new tag list");
  if (!qtdemux->tag_list)
    qtdemux->tag_list = gst_tag_list_new ();

  i = 0;
  while (i < G_N_ELEMENTS (add_funcs)) {
    node = qtdemux_tree_get_child_by_type (ilst, add_funcs[i].fourcc);
    if (node) {
      gint len;

      len = QT_UINT32 (node->data);
      if (len < 12) {
        GST_DEBUG_OBJECT (qtdemux, "too small tag atom %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (add_funcs[i].fourcc));
      } else {
        add_funcs[i].func (qtdemux, add_funcs[i].gst_tag,
            add_funcs[i].gst_tag_bis, node);
      }
      g_node_destroy (node);
    } else {
      i++;
    }
  }

  /* parsed nodes have been removed, pass along remainder as blob */
  g_node_children_foreach (ilst, G_TRAVERSE_ALL,
      (GNodeForeachFunc) qtdemux_tag_add_blob, qtdemux);

#ifndef GSTREAMER_LITE
  /* parse up XMP_ node if existing */
  xmp_ = qtdemux_tree_get_child_by_type (udta, FOURCC_XMP_);
  if (xmp_ != NULL) {
    GstBuffer *buf;
    GstTagList *taglist;

    buf = gst_buffer_new ();
    GST_BUFFER_DATA (buf) = ((guint8 *) xmp_->data) + 8;
    GST_BUFFER_SIZE (buf) = QT_UINT32 ((guint8 *) xmp_->data) - 8;

    taglist = gst_tag_list_from_xmp_buffer (buf);
    gst_buffer_unref (buf);

    qtdemux_handle_xmp_taglist (qtdemux, taglist);
  } else {
    GST_DEBUG_OBJECT (qtdemux, "No XMP_ node found");
  }
#endif // GSTREAMER_LITE

}

typedef struct
{
  GstStructure *structure;      /* helper for sort function */
  gchar *location;
  guint min_req_bitrate;
  guint min_req_qt_version;
} GstQtReference;

static gint
qtdemux_redirects_sort_func (gconstpointer a, gconstpointer b)
{
  GstQtReference *ref_a = (GstQtReference *) a;
  GstQtReference *ref_b = (GstQtReference *) b;

  if (ref_b->min_req_qt_version != ref_a->min_req_qt_version)
    return ref_b->min_req_qt_version - ref_a->min_req_qt_version;

  /* known bitrates go before unknown; higher bitrates go first */
  return ref_b->min_req_bitrate - ref_a->min_req_bitrate;
}

/* sort the redirects and post a message for the application.
 */
static void
qtdemux_process_redirects (GstQTDemux * qtdemux, GList * references)
{
  GstQtReference *best;
  GstStructure *s;
  GstMessage *msg;
  GValue list_val = { 0, };
  GList *l;

  g_assert (references != NULL);

  references = g_list_sort (references, qtdemux_redirects_sort_func);

  best = (GstQtReference *) references->data;

  g_value_init (&list_val, GST_TYPE_LIST);

  for (l = references; l != NULL; l = l->next) {
    GstQtReference *ref = (GstQtReference *) l->data;
    GValue struct_val = { 0, };

    ref->structure = gst_structure_new ("redirect",
        "new-location", G_TYPE_STRING, ref->location, NULL);

    if (ref->min_req_bitrate > 0) {
      gst_structure_set (ref->structure, "minimum-bitrate", G_TYPE_INT,
          ref->min_req_bitrate, NULL);
    }

    g_value_init (&struct_val, GST_TYPE_STRUCTURE);
    g_value_set_boxed (&struct_val, ref->structure);
    gst_value_list_append_value (&list_val, &struct_val);
    g_value_unset (&struct_val);
    /* don't free anything here yet, since we need best->structure below */
  }

  g_assert (best != NULL);
  s = gst_structure_copy (best->structure);

  if (g_list_length (references) > 1) {
    gst_structure_set_value (s, "locations", &list_val);
  }

  g_value_unset (&list_val);

  for (l = references; l != NULL; l = l->next) {
    GstQtReference *ref = (GstQtReference *) l->data;

    gst_structure_free (ref->structure);
    g_free (ref->location);
    g_free (ref);
  }
  g_list_free (references);

  GST_INFO_OBJECT (qtdemux, "posting redirect message: %" GST_PTR_FORMAT, s);
  msg = gst_message_new_element (GST_OBJECT_CAST (qtdemux), s);
  gst_element_post_message (GST_ELEMENT_CAST (qtdemux), msg);
  qtdemux->posted_redirect = TRUE;
}

/* look for redirect nodes, collect all redirect information and
 * process it.
 */
static gboolean
qtdemux_parse_redirects (GstQTDemux * qtdemux)
{
  GNode *rmra, *rmda, *rdrf;

  rmra = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_rmra);
  if (rmra) {
    GList *redirects = NULL;

    rmda = qtdemux_tree_get_child_by_type (rmra, FOURCC_rmda);
    while (rmda) {
      GstQtReference ref = { NULL, NULL, 0, 0 };
      GNode *rmdr, *rmvc;

      if ((rmdr = qtdemux_tree_get_child_by_type (rmda, FOURCC_rmdr))) {
        ref.min_req_bitrate = QT_UINT32 ((guint8 *) rmdr->data + 12);
        GST_LOG_OBJECT (qtdemux, "data rate atom, required bitrate = %u",
            ref.min_req_bitrate);
      }

      if ((rmvc = qtdemux_tree_get_child_by_type (rmda, FOURCC_rmvc))) {
        guint32 package = QT_FOURCC ((guint8 *) rmvc->data + 12);
        guint version = QT_UINT32 ((guint8 *) rmvc->data + 16);

#ifndef GST_DISABLE_GST_DEBUG
        guint bitmask = QT_UINT32 ((guint8 *) rmvc->data + 20);
#endif
        guint check_type = QT_UINT16 ((guint8 *) rmvc->data + 24);

        GST_LOG_OBJECT (qtdemux,
            "version check atom [%" GST_FOURCC_FORMAT "], version=0x%08x"
            ", mask=%08x, check_type=%u", GST_FOURCC_ARGS (package), version,
            bitmask, check_type);
        if (package == FOURCC_qtim && check_type == 0) {
          ref.min_req_qt_version = version;
        }
      }

      rdrf = qtdemux_tree_get_child_by_type (rmda, FOURCC_rdrf);
      if (rdrf) {
        guint32 ref_type;
        guint8 *ref_data;

        ref_type = QT_FOURCC ((guint8 *) rdrf->data + 12);
        ref_data = (guint8 *) rdrf->data + 20;
        if (ref_type == FOURCC_alis) {
          guint record_len, record_version, fn_len;

          /* MacOSX alias record, google for alias-layout.txt */
          record_len = QT_UINT16 (ref_data + 4);
          record_version = QT_UINT16 (ref_data + 4 + 2);
          fn_len = QT_UINT8 (ref_data + 50);
          if (record_len > 50 && record_version == 2 && fn_len > 0) {
            ref.location = g_strndup ((gchar *) ref_data + 51, fn_len);
          }
        } else if (ref_type == FOURCC_url_) {
          ref.location = g_strdup ((gchar *) ref_data);
        } else {
          GST_DEBUG_OBJECT (qtdemux,
              "unknown rdrf reference type %" GST_FOURCC_FORMAT,
              GST_FOURCC_ARGS (ref_type));
        }
        if (ref.location != NULL) {
          GST_INFO_OBJECT (qtdemux, "New location: %s", ref.location);
          redirects = g_list_prepend (redirects, g_memdup (&ref, sizeof (ref)));
        } else {
          GST_WARNING_OBJECT (qtdemux,
              "Failed to extract redirect location from rdrf atom");
        }
      }

      /* look for others */
      rmda = qtdemux_tree_get_sibling_by_type (rmda, FOURCC_rmda);
    }

    if (redirects != NULL) {
      qtdemux_process_redirects (qtdemux, redirects);
    }
  }
  return TRUE;
}

static GstTagList *
qtdemux_add_container_format (GstQTDemux * qtdemux, GstTagList * tags)
{
  const gchar *fmt;

  if (tags == NULL)
    tags = gst_tag_list_new ();

  if (qtdemux->major_brand == FOURCC_mjp2)
    fmt = "Motion JPEG 2000";
  else if ((qtdemux->major_brand & 0xffff) == GST_MAKE_FOURCC ('3', 'g', 0, 0))
    fmt = "3GP";
  else if (qtdemux->major_brand == FOURCC_qt__)
    fmt = "Quicktime";
  else if (qtdemux->fragmented)
    fmt = "ISO fMP4";
  else
    fmt = "ISO MP4/M4A";

  GST_LOG_OBJECT (qtdemux, "mapped %" GST_FOURCC_FORMAT " to '%s'",
      GST_FOURCC_ARGS (qtdemux->major_brand), fmt);

  gst_tag_list_add (tags, GST_TAG_MERGE_REPLACE, GST_TAG_CONTAINER_FORMAT,
      fmt, NULL);

  return tags;
}

/* we have read th complete moov node now.
 * This function parses all of the relevant info, creates the traks and
 * prepares all data structures for playback
 */
static gboolean
qtdemux_parse_tree (GstQTDemux * qtdemux)
{
  GNode *mvhd;
  GNode *trak;
  GNode *udta;
  GNode *mvex;
  gint64 duration;
  guint64 creation_time;
  GstDateTime *datetime = NULL;
  gint version;

  mvhd = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_mvhd);
  if (mvhd == NULL) {
    GST_LOG_OBJECT (qtdemux, "No mvhd node found, looking for redirects.");
    return qtdemux_parse_redirects (qtdemux);
  }

  version = QT_UINT8 ((guint8 *) mvhd->data + 8);
  if (version == 1) {
    creation_time = QT_UINT64 ((guint8 *) mvhd->data + 12);
    qtdemux->timescale = QT_UINT32 ((guint8 *) mvhd->data + 28);
    qtdemux->duration = QT_UINT64 ((guint8 *) mvhd->data + 32);
  } else if (version == 0) {
    creation_time = QT_UINT32 ((guint8 *) mvhd->data + 12);
    qtdemux->timescale = QT_UINT32 ((guint8 *) mvhd->data + 20);
    qtdemux->duration = QT_UINT32 ((guint8 *) mvhd->data + 24);
  } else {
    GST_WARNING_OBJECT (qtdemux, "Unhandled mvhd version %d", version);
    return FALSE;
  }

  /* Moving qt creation time (secs since 1904) to unix time */
  if (creation_time != 0) {
    if (creation_time > QTDEMUX_SECONDS_FROM_1904_TO_1970) {
      creation_time -= QTDEMUX_SECONDS_FROM_1904_TO_1970;
      datetime = gst_date_time_new_from_unix_epoch_local_time (creation_time);
    } else {
      GST_WARNING_OBJECT (qtdemux, "Can't handle datetimes before 1970 yet, "
          "please file a bug at http://bugzilla.gnome.org");
    }
  }
  if (datetime) {
    if (!qtdemux->tag_list)
      qtdemux->tag_list = gst_tag_list_new ();

    /* Use KEEP as explicit tags should have a higher priority than mvhd tag */
    gst_tag_list_add (qtdemux->tag_list, GST_TAG_MERGE_KEEP, GST_TAG_DATE_TIME,
        datetime, NULL);
    gst_date_time_unref (datetime);
  }

  GST_INFO_OBJECT (qtdemux, "timescale: %u", qtdemux->timescale);
  GST_INFO_OBJECT (qtdemux, "duration: %" G_GUINT64_FORMAT, qtdemux->duration);

  /* check for fragmented file and get some (default) data */
  mvex = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_mvex);
  if (mvex) {
    GNode *mehd;
    GstByteReader mehd_data;

    /* let track parsing or anyone know weird stuff might happen ... */
    qtdemux->fragmented = TRUE;

    /* compensate for total duration */
    mehd = qtdemux_tree_get_child_by_type_full (mvex, FOURCC_mehd, &mehd_data);
    if (mehd)
      qtdemux_parse_mehd (qtdemux, &mehd_data);
  }

  /* set duration in the segment info */
  gst_qtdemux_get_duration (qtdemux, &duration);
  if (duration)
    gst_segment_set_duration (&qtdemux->segment, GST_FORMAT_TIME, duration);

  /* parse all traks */
  trak = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_trak);
  while (trak) {
#ifdef GSTREAMER_LITE
      if (!qtdemux_parse_trak (qtdemux, trak))
          return FALSE;
#else    
    qtdemux_parse_trak (qtdemux, trak);
#endif // GSTREAMER_LITE
    /* iterate all siblings */
    trak = qtdemux_tree_get_sibling_by_type (trak, FOURCC_trak);
  }

  /* find tags */
  udta = qtdemux_tree_get_child_by_type (qtdemux->moov_node, FOURCC_udta);
  if (udta) {
    qtdemux_parse_udta (qtdemux, udta);
  } else {
    GST_LOG_OBJECT (qtdemux, "No udta node found.");
  }

  qtdemux->tag_list = qtdemux_add_container_format (qtdemux, qtdemux->tag_list);

  return TRUE;
}

/* taken from ffmpeg */
static unsigned int
get_size (guint8 * ptr, guint8 ** end)
{
  int count = 4;
  int len = 0;

  while (count--) {
    int c = *ptr;

    ptr++;
    len = (len << 7) | (c & 0x7f);
    if (!(c & 0x80))
      break;
  }
  if (end)
    *end = ptr;
  return len;
}

/* this can change the codec originally present in @list */
#ifdef GSTREAMER_LITE
static gboolean
#else
static void
#endif // GSTREAMER_LITE
gst_qtdemux_handle_esds (GstQTDemux * qtdemux, QtDemuxStream * stream,
    GNode * esds, GstTagList * list)
{
  int len = QT_UINT32 (esds->data);
  guint8 *ptr = esds->data;
  guint8 *end = ptr + len;
  int tag;
  guint8 *data_ptr = NULL;
  int data_len = 0;
  guint8 object_type_id = 0;
  const char *codec_name = NULL;
  GstCaps *caps = NULL;

  GST_MEMDUMP_OBJECT (qtdemux, "esds", ptr, len);
  ptr += 8;
  GST_DEBUG_OBJECT (qtdemux, "version/flags = %08x", QT_UINT32 (ptr));
  ptr += 4;
  while (ptr < end) {
    tag = QT_UINT8 (ptr);
    GST_DEBUG_OBJECT (qtdemux, "tag = %02x", tag);
    ptr++;
    len = get_size (ptr, &ptr);
    GST_DEBUG_OBJECT (qtdemux, "len = %d", len);

    switch (tag) {
      case 0x03:
        GST_DEBUG_OBJECT (qtdemux, "ID %04x", QT_UINT16 (ptr));
        GST_DEBUG_OBJECT (qtdemux, "priority %04x", QT_UINT8 (ptr + 2));
        ptr += 3;
        break;
      case 0x04:{
        guint max_bitrate, avg_bitrate;

        object_type_id = QT_UINT8 (ptr);
        max_bitrate = QT_UINT32 (ptr + 5);
        avg_bitrate = QT_UINT32 (ptr + 9);
        GST_DEBUG_OBJECT (qtdemux, "object_type_id %02x", object_type_id);
        GST_DEBUG_OBJECT (qtdemux, "stream_type %02x", QT_UINT8 (ptr + 1));
        GST_DEBUG_OBJECT (qtdemux, "buffer_size_db %02x", QT_UINT24 (ptr + 2));
        GST_DEBUG_OBJECT (qtdemux, "max bitrate %u", max_bitrate);
        GST_DEBUG_OBJECT (qtdemux, "avg bitrate %u", avg_bitrate);
        if (max_bitrate > 0 && max_bitrate < G_MAXUINT32) {
          gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
              GST_TAG_MAXIMUM_BITRATE, max_bitrate, NULL);
        }
        if (avg_bitrate > 0 && avg_bitrate < G_MAXUINT32) {
          gst_tag_list_add (list, GST_TAG_MERGE_REPLACE, GST_TAG_BITRATE,
              avg_bitrate, NULL);
        }
        ptr += 13;
        break;
      }
      case 0x05:
        GST_MEMDUMP_OBJECT (qtdemux, "data", ptr, len);
#ifdef GSTREAMER_LITE
        if (end - len < ptr)
            return FALSE;
#endif // GSTREAMER_LITE
        data_ptr = ptr;
        data_len = len;
        ptr += len;
        break;
      case 0x06:
        GST_DEBUG_OBJECT (qtdemux, "data %02x", QT_UINT8 (ptr));
        ptr += 1;
        break;
      default:
        GST_ERROR_OBJECT (qtdemux, "parse error");
        break;
    }
  }

  /* object_type_id in the esds atom in mp4a and mp4v tells us which codec is
   * in use, and should also be used to override some other parameters for some
   * codecs. */
  switch (object_type_id) {
    case 0x20:                 /* MPEG-4 */
      /* 4 bytes for the visual_object_sequence_start_code and 1 byte for the
       * profile_and_level_indication */
      if (data_ptr != NULL && data_len >= 5 &&
          GST_READ_UINT32_BE (data_ptr) == 0x000001b0) {
        gst_codec_utils_mpeg4video_caps_set_level_and_profile (stream->caps,
            data_ptr + 4, data_len - 4);
      }
      break;                    /* Nothing special needed here */
    case 0x21:                 /* H.264 */
      codec_name = "H.264 / AVC";
      caps = gst_caps_new_simple ("video/x-h264",
          "stream-format", G_TYPE_STRING, "avc",
          "alignment", G_TYPE_STRING, "au", NULL);
      break;
    case 0x40:                 /* AAC (any) */
    case 0x66:                 /* AAC Main */
    case 0x67:                 /* AAC LC */
    case 0x68:                 /* AAC SSR */
      /* Override channels and rate based on the codec_data, as it's often
       * wrong. */
      /* Only do so for basic setup without HE-AAC extension */
      if (data_ptr && data_len == 2) {
        guint channels, rateindex, rate;

        /* FIXME: add gst_codec_utils_aac_get_{channels|sample_rate}()? */
        channels = (data_ptr[1] & 0x7f) >> 3;
        if (channels > 0 && channels < 7) {
          stream->n_channels = channels;
        } else if (channels == 7) {
          stream->n_channels = 8;
        }

        rateindex = ((data_ptr[0] & 0x7) << 1) | ((data_ptr[1] & 0x80) >> 7);
        rate = gst_codec_utils_aac_get_sample_rate_from_index (rateindex);
        if (rate > 0)
          stream->rate = rate;
      }

      /* Set level and profile if possible */
      if (data_ptr != NULL && data_len >= 2) {
        gst_codec_utils_aac_caps_set_level_and_profile (stream->caps,
            data_ptr, data_len);
      }
      break;
    case 0x60:                 /* MPEG-2, various profiles */
    case 0x61:
    case 0x62:
    case 0x63:
    case 0x64:
    case 0x65:
      codec_name = "MPEG-2 video";

      gst_caps_unref (stream->caps);
      stream->caps = gst_caps_new_simple ("video/mpeg",
          "mpegversion", G_TYPE_INT, 2,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case 0x69:                 /* MP3 has two different values, accept either */
    case 0x6B:
      /* change to mpeg1 layer 3 audio */
      gst_caps_set_simple (stream->caps, "layer", G_TYPE_INT, 3,
          "mpegversion", G_TYPE_INT, 1, NULL);
      codec_name = "MPEG-1 layer 3";
      break;
    case 0x6A:                 /* MPEG-1 */
      codec_name = "MPEG-1 video";

      gst_caps_unref (stream->caps);
      stream->caps = gst_caps_new_simple ("video/mpeg",
          "mpegversion", G_TYPE_INT, 1,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case 0x6C:                 /* MJPEG */
      caps = gst_caps_new_simple ("image/jpeg", NULL);
      codec_name = "Motion-JPEG";
      break;
    case 0x6D:                 /* PNG */
      caps = gst_caps_new_simple ("image/png", NULL);
      codec_name = "PNG still images";
      break;
    case 0x6E:                 /* JPEG2000 */
      codec_name = "JPEG-2000";
      caps = gst_caps_new_simple ("image/x-j2c", "fields", G_TYPE_INT, 1, NULL);
      break;
    case 0xA4:                 /* Dirac */
      codec_name = "Dirac";
      caps = gst_caps_new_simple ("video/x-dirac", NULL);
      break;
    case 0xA5:                 /* AC3 */
      codec_name = "AC-3 audio";
      caps = gst_caps_new_simple ("audio/x-ac3",
          "framed", G_TYPE_BOOLEAN, TRUE, NULL);
      break;
    case 0xE1:                 /* QCELP */
      /* QCELP, the codec_data is a riff tag (little endian) with
       * more info (http://ftp.3gpp2.org/TSGC/Working/2003/2003-05-SanDiego/TSG-C-2003-05-San%20Diego/WG1/SWG12/C12-20030512-006%20=%20C12-20030217-015_Draft_Baseline%20Text%20of%20FFMS_R2.doc). */
      caps = gst_caps_new_simple ("audio/qcelp", NULL);
      codec_name = "QCELP";
      break;
    default:
      break;
  }

  /* If we have a replacement caps, then change our caps for this stream */
  if (caps) {
    gst_caps_unref (stream->caps);
    stream->caps = caps;
  }

  if (codec_name && list)
    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE,
        GST_TAG_AUDIO_CODEC, codec_name, NULL);

  /* Add the codec_data attribute to caps, if we have it */
  if (data_ptr) {
    GstBuffer *buffer;

    buffer = gst_buffer_new_and_alloc (data_len);
    memcpy (GST_BUFFER_DATA (buffer), data_ptr, data_len);

    GST_DEBUG_OBJECT (qtdemux, "setting codec_data from esds");
    GST_MEMDUMP_OBJECT (qtdemux, "codec_data from esds", data_ptr, data_len);

    gst_caps_set_simple (stream->caps, "codec_data", GST_TYPE_BUFFER,
        buffer, NULL);
    gst_buffer_unref (buffer);

#if defined(GSTREAMER_LITE) && defined(OSX)
      switch (object_type_id) {
          case 0x40:                 /* AAC (any) */
          case 0x66:                 /* AAC Main */
          case 0x67:                 /* AAC LC */
          case 0x68:                 /* AAC SSR */
          {
              int esds_len = QT_UINT32 (esds->data);
              GstBuffer* esds_buffer = gst_buffer_new_and_alloc (esds_len);
              memcpy (GST_BUFFER_DATA (esds_buffer), esds->data, esds_len);
              
              GST_DEBUG_OBJECT (qtdemux, "setting esds_data from esds");
              GST_MEMDUMP_OBJECT (qtdemux, "esds_data from esds", esds->data, esds_len);
              
              gst_caps_set_simple (stream->caps, "esds_data", GST_TYPE_BUFFER,
                                   esds_buffer, NULL);
              gst_buffer_unref (esds_buffer);
          }
              break;
          default:
              break;
      }
#endif
	  
  }

#ifdef GSTREAMER_LITE
    return TRUE;
#endif
}

#define _codec(name) \
  do { \
    if (codec_name) { \
      *codec_name = g_strdup (name); \
    } \
  } while (0)

static GstCaps *
qtdemux_video_caps (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint32 fourcc, const guint8 * stsd_data, gchar ** codec_name)
{
  GstCaps *caps;
  const GstStructure *s;
  const gchar *name;

  switch (fourcc) {
    case GST_MAKE_FOURCC ('p', 'n', 'g', ' '):
      _codec ("PNG still images");
      caps = gst_caps_new_simple ("image/png", NULL);
      break;
    case GST_MAKE_FOURCC ('j', 'p', 'e', 'g'):
      _codec ("JPEG still images");
      caps = gst_caps_new_simple ("image/jpeg", NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'j', 'p', 'a'):
    case GST_MAKE_FOURCC ('A', 'V', 'D', 'J'):
    case GST_MAKE_FOURCC ('M', 'J', 'P', 'G'):
    case GST_MAKE_FOURCC ('d', 'm', 'b', '1'):
      _codec ("Motion-JPEG");
      caps = gst_caps_new_simple ("image/jpeg", NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'j', 'p', 'b'):
      _codec ("Motion-JPEG format B");
      caps = gst_caps_new_simple ("video/x-mjpeg-b", NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'j', 'p', '2'):
      _codec ("JPEG-2000");
      /* override to what it should be according to spec, avoid palette_data */
      stream->bits_per_sample = 24;
      caps = gst_caps_new_simple ("image/x-j2c", "fields", G_TYPE_INT, 1, NULL);
      break;
    case GST_MAKE_FOURCC ('S', 'V', 'Q', '3'):
      _codec ("Sorensen video v.3");
      caps = gst_caps_new_simple ("video/x-svq",
          "svqversion", G_TYPE_INT, 3, NULL);
      break;
    case GST_MAKE_FOURCC ('s', 'v', 'q', 'i'):
    case GST_MAKE_FOURCC ('S', 'V', 'Q', '1'):
      _codec ("Sorensen video v.1");
      caps = gst_caps_new_simple ("video/x-svq",
          "svqversion", G_TYPE_INT, 1, NULL);
      break;
    case GST_MAKE_FOURCC ('r', 'a', 'w', ' '):
    {
      guint16 bps;

      _codec ("Raw RGB video");
      bps = QT_UINT16 (stsd_data + 98);
      /* set common stuff */
      caps = gst_caps_new_simple ("video/x-raw-rgb",
          "endianness", G_TYPE_INT, G_BYTE_ORDER, "depth", G_TYPE_INT, bps,
          NULL);

      switch (bps) {
        case 15:
          gst_caps_set_simple (caps,
              "bpp", G_TYPE_INT, 16,
              "endianness", G_TYPE_INT, G_BIG_ENDIAN,
              "red_mask", G_TYPE_INT, 0x7c00,
              "green_mask", G_TYPE_INT, 0x03e0,
              "blue_mask", G_TYPE_INT, 0x001f, NULL);
          break;
        case 16:
          gst_caps_set_simple (caps,
              "bpp", G_TYPE_INT, 16,
              "endianness", G_TYPE_INT, G_BIG_ENDIAN,
              "red_mask", G_TYPE_INT, 0xf800,
              "green_mask", G_TYPE_INT, 0x07e0,
              "blue_mask", G_TYPE_INT, 0x001f, NULL);
          break;
        case 24:
          gst_caps_set_simple (caps,
              "bpp", G_TYPE_INT, 24,
              "endianness", G_TYPE_INT, G_BIG_ENDIAN,
              "red_mask", G_TYPE_INT, 0xff0000,
              "green_mask", G_TYPE_INT, 0x00ff00,
              "blue_mask", G_TYPE_INT, 0x0000ff, NULL);
          break;
        case 32:
          gst_caps_set_simple (caps,
              "bpp", G_TYPE_INT, 32,
              "endianness", G_TYPE_INT, G_BIG_ENDIAN,
              "alpha_mask", G_TYPE_INT, 0xff000000,
              "red_mask", G_TYPE_INT, 0x00ff0000,
              "green_mask", G_TYPE_INT, 0x0000ff00,
              "blue_mask", G_TYPE_INT, 0x000000ff, NULL);
          break;
        default:
          /* unknown */
          break;
      }
      break;
    }
    case GST_MAKE_FOURCC ('y', 'v', '1', '2'):
      _codec ("Raw planar YUV 4:2:0");
      caps = gst_caps_new_simple ("video/x-raw-yuv",
          "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('I', '4', '2', '0'),
          NULL);
      break;
    case GST_MAKE_FOURCC ('y', 'u', 'v', '2'):
    case GST_MAKE_FOURCC ('Y', 'u', 'v', '2'):
      _codec ("Raw packed YUV 4:2:2");
      caps = gst_caps_new_simple ("video/x-raw-yuv",
          "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'),
          NULL);
      break;
    case GST_MAKE_FOURCC ('2', 'v', 'u', 'y'):
    case GST_MAKE_FOURCC ('2', 'V', 'u', 'y'):
      _codec ("Raw packed YUV 4:2:2");
      caps = gst_caps_new_simple ("video/x-raw-yuv",
          "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'),
          NULL);
      break;
    case GST_MAKE_FOURCC ('v', '2', '1', '0'):
      _codec ("Raw packed YUV 10-bit 4:2:2");
      caps = gst_caps_new_simple ("video/x-raw-yuv",
          "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('v', '2', '1', '0'),
          NULL);
      break;
    case GST_MAKE_FOURCC ('r', '2', '1', '0'):
      _codec ("Raw packed RGB 10-bit 4:4:4");
      caps = gst_caps_new_simple ("video/x-raw-rgb",
          "endianness", G_TYPE_INT, G_BIG_ENDIAN, "depth", G_TYPE_INT, 30,
          "bpp", G_TYPE_INT, 32,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN,
          "red_mask", G_TYPE_INT, 0x3ff00000,
          "green_mask", G_TYPE_INT, 0x000ffc00,
          "blue_mask", G_TYPE_INT, 0x000003ff, NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'p', 'e', 'g'):
    case GST_MAKE_FOURCC ('m', 'p', 'g', '1'):
      _codec ("MPEG-1 video");
      caps = gst_caps_new_simple ("video/mpeg", "mpegversion", G_TYPE_INT, 1,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('h', 'd', 'v', '1'): /* HDV 720p30 */
    case GST_MAKE_FOURCC ('h', 'd', 'v', '2'): /* HDV 1080i60 */
    case GST_MAKE_FOURCC ('h', 'd', 'v', '3'): /* HDV 1080i50 */
    case GST_MAKE_FOURCC ('h', 'd', 'v', '5'): /* HDV 720p25 */
    case GST_MAKE_FOURCC ('h', 'd', 'v', '6'): /* HDV 1080i60 */
    case GST_MAKE_FOURCC ('m', 'x', '5', 'n'): /* MPEG2 IMX NTSC 525/60 50mb/s produced by FCP */
    case GST_MAKE_FOURCC ('m', 'x', '5', 'p'): /* MPEG2 IMX PAL 625/60 50mb/s produced by FCP */
    case GST_MAKE_FOURCC ('m', 'x', '4', 'n'): /* MPEG2 IMX NTSC 525/60 40mb/s produced by FCP */
    case GST_MAKE_FOURCC ('m', 'x', '4', 'p'): /* MPEG2 IMX PAL 625/60 40mb/s produced by FCP */
    case GST_MAKE_FOURCC ('m', 'x', '3', 'n'): /* MPEG2 IMX NTSC 525/60 30mb/s produced by FCP */
    case GST_MAKE_FOURCC ('m', 'x', '3', 'p'): /* MPEG2 IMX PAL 625/50 30mb/s produced by FCP */
    case GST_MAKE_FOURCC ('x', 'd', 'v', '2'): /* XDCAM HD 1080i60 */
    case GST_MAKE_FOURCC ('A', 'V', 'm', 'p'): /* AVID IMX PAL */
    case GST_MAKE_FOURCC ('m', 'p', 'g', '2'): /* AVID IMX PAL */
      _codec ("MPEG-2 video");
      caps = gst_caps_new_simple ("video/mpeg", "mpegversion", G_TYPE_INT, 2,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('g', 'i', 'f', ' '):
      _codec ("GIF still images");
      caps = gst_caps_new_simple ("image/gif", NULL);
      break;
    case GST_MAKE_FOURCC ('h', '2', '6', '3'):
    case GST_MAKE_FOURCC ('H', '2', '6', '3'):
    case GST_MAKE_FOURCC ('s', '2', '6', '3'):
    case GST_MAKE_FOURCC ('U', '2', '6', '3'):
      _codec ("H.263");
      /* ffmpeg uses the height/width props, don't know why */
      caps = gst_caps_new_simple ("video/x-h263", NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'p', '4', 'v'):
    case GST_MAKE_FOURCC ('M', 'P', '4', 'V'):
      _codec ("MPEG-4 video");
      caps = gst_caps_new_simple ("video/mpeg", "mpegversion", G_TYPE_INT, 4,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('3', 'i', 'v', 'd'):
    case GST_MAKE_FOURCC ('3', 'I', 'V', 'D'):
      _codec ("Microsoft MPEG-4 4.3");  /* FIXME? */
      caps = gst_caps_new_simple ("video/x-msmpeg",
          "msmpegversion", G_TYPE_INT, 43, NULL);
      break;
    case GST_MAKE_FOURCC ('3', 'I', 'V', '1'):
    case GST_MAKE_FOURCC ('3', 'I', 'V', '2'):
      _codec ("3ivX video");
      caps = gst_caps_new_simple ("video/x-3ivx", NULL);
      break;
    case GST_MAKE_FOURCC ('D', 'I', 'V', '3'):
      _codec ("DivX 3");
      caps = gst_caps_new_simple ("video/x-divx",
          "divxversion", G_TYPE_INT, 3, NULL);
      break;
    case GST_MAKE_FOURCC ('D', 'I', 'V', 'X'):
    case GST_MAKE_FOURCC ('d', 'i', 'v', 'x'):
      _codec ("DivX 4");
      caps = gst_caps_new_simple ("video/x-divx",
          "divxversion", G_TYPE_INT, 4, NULL);
      break;
    case GST_MAKE_FOURCC ('D', 'X', '5', '0'):
      _codec ("DivX 5");
      caps = gst_caps_new_simple ("video/x-divx",
          "divxversion", G_TYPE_INT, 5, NULL);
      break;
    case GST_MAKE_FOURCC ('X', 'V', 'I', 'D'):
    case GST_MAKE_FOURCC ('x', 'v', 'i', 'd'):
      _codec ("XVID MPEG-4");
      caps = gst_caps_new_simple ("video/x-xvid", NULL);
      break;

    case GST_MAKE_FOURCC ('F', 'M', 'P', '4'):
    case GST_MAKE_FOURCC ('U', 'M', 'P', '4'):
      caps = gst_caps_new_simple ("video/mpeg",
          "mpegversion", G_TYPE_INT, 4, NULL);
      if (codec_name)
        *codec_name = g_strdup ("FFmpeg MPEG-4");
      break;

    case GST_MAKE_FOURCC ('c', 'v', 'i', 'd'):
      _codec ("Cinepak");
      caps = gst_caps_new_simple ("video/x-cinepak", NULL);
      break;
    case GST_MAKE_FOURCC ('q', 'd', 'r', 'w'):
      _codec ("Apple QuickDraw");
      caps = gst_caps_new_simple ("video/x-qdrw", NULL);
      break;
    case GST_MAKE_FOURCC ('r', 'p', 'z', 'a'):
      _codec ("Apple video");
      caps = gst_caps_new_simple ("video/x-apple-video", NULL);
      break;
    case GST_MAKE_FOURCC ('a', 'v', 'c', '1'):
      _codec ("H.264 / AVC");
      caps = gst_caps_new_simple ("video/x-h264",
          "stream-format", G_TYPE_STRING, "avc",
          "alignment", G_TYPE_STRING, "au", NULL);
      break;
    case GST_MAKE_FOURCC ('r', 'l', 'e', ' '):
      _codec ("Run-length encoding");
      caps = gst_caps_new_simple ("video/x-rle",
          "layout", G_TYPE_STRING, "quicktime", NULL);
      break;
    case GST_MAKE_FOURCC ('I', 'V', '3', '2'):
    case GST_MAKE_FOURCC ('i', 'v', '3', '2'):
      _codec ("Indeo Video 3");
      caps = gst_caps_new_simple ("video/x-indeo",
          "indeoversion", G_TYPE_INT, 3, NULL);
      break;
    case GST_MAKE_FOURCC ('I', 'V', '4', '1'):
    case GST_MAKE_FOURCC ('i', 'v', '4', '1'):
      _codec ("Intel Video 4");
      caps = gst_caps_new_simple ("video/x-indeo",
          "indeoversion", G_TYPE_INT, 4, NULL);
      break;
    case GST_MAKE_FOURCC ('d', 'v', 'c', 'p'):
    case GST_MAKE_FOURCC ('d', 'v', 'c', ' '):
    case GST_MAKE_FOURCC ('d', 'v', 's', 'd'):
    case GST_MAKE_FOURCC ('D', 'V', 'S', 'D'):
    case GST_MAKE_FOURCC ('d', 'v', 'c', 's'):
    case GST_MAKE_FOURCC ('D', 'V', 'C', 'S'):
    case GST_MAKE_FOURCC ('d', 'v', '2', '5'):
    case GST_MAKE_FOURCC ('d', 'v', 'p', 'p'):
      _codec ("DV Video");
      caps = gst_caps_new_simple ("video/x-dv", "dvversion", G_TYPE_INT, 25,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('d', 'v', '5', 'n'): /* DVCPRO50 NTSC */
    case GST_MAKE_FOURCC ('d', 'v', '5', 'p'): /* DVCPRO50 PAL */
      _codec ("DVCPro50 Video");
      caps = gst_caps_new_simple ("video/x-dv", "dvversion", G_TYPE_INT, 50,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('d', 'v', 'h', '5'): /* DVCPRO HD 50i produced by FCP */
    case GST_MAKE_FOURCC ('d', 'v', 'h', '6'): /* DVCPRO HD 60i produced by FCP */
      _codec ("DVCProHD Video");
      caps = gst_caps_new_simple ("video/x-dv", "dvversion", G_TYPE_INT, 100,
          "systemstream", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('s', 'm', 'c', ' '):
      _codec ("Apple Graphics (SMC)");
      caps = gst_caps_new_simple ("video/x-smc", NULL);
      break;
    case GST_MAKE_FOURCC ('V', 'P', '3', '1'):
      _codec ("VP3");
      caps = gst_caps_new_simple ("video/x-vp3", NULL);
      break;
    case GST_MAKE_FOURCC ('X', 'i', 'T', 'h'):
      _codec ("Theora");
      caps = gst_caps_new_simple ("video/x-theora", NULL);
      /* theora uses one byte of padding in the data stream because it does not
       * allow 0 sized packets while theora does */
      stream->padding = 1;
      break;
    case GST_MAKE_FOURCC ('d', 'r', 'a', 'c'):
      _codec ("Dirac");
      caps = gst_caps_new_simple ("video/x-dirac", NULL);
      break;
    case GST_MAKE_FOURCC ('t', 'i', 'f', 'f'):
      _codec ("TIFF still images");
      caps = gst_caps_new_simple ("image/tiff", NULL);
      break;
    case GST_MAKE_FOURCC ('i', 'c', 'o', 'd'):
      _codec ("Apple Intermediate Codec");
      caps = gst_caps_from_string ("video/x-apple-intermediate-codec");
      break;
    case GST_MAKE_FOURCC ('A', 'V', 'd', 'n'):
      _codec ("AVID DNxHD");
      caps = gst_caps_from_string ("video/x-dnxhd");
      break;
    case GST_MAKE_FOURCC ('V', 'P', '8', '0'):
      _codec ("On2 VP8");
      caps = gst_caps_from_string ("video/x-vp8");
    case FOURCC_ovc1:
      _codec ("VC-1");
      caps = gst_caps_new_simple ("video/x-wmv",
          "wmvversion", G_TYPE_INT, 3,
          "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('W', 'V', 'C', '1'),
          NULL);
      break;
    case GST_MAKE_FOURCC ('k', 'p', 'c', 'd'):
    default:
    {
      char *s;

      s = g_strdup_printf ("video/x-gst-fourcc-%" GST_FOURCC_FORMAT,
          GST_FOURCC_ARGS (fourcc));
      caps = gst_caps_new_simple (s, NULL);
      break;
    }
  }

  /* enable clipping for raw video streams */
  s = gst_caps_get_structure (caps, 0);
  name = gst_structure_get_name (s);
  if (g_str_has_prefix (name, "video/x-raw-")) {
    stream->need_clip = TRUE;
  }
  return caps;
}

static GstCaps *
qtdemux_audio_caps (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint32 fourcc, const guint8 * data, int len, gchar ** codec_name)
{
  GstCaps *caps;
  const GstStructure *s;
  const gchar *name;
  gint endian = 0;

  GST_DEBUG_OBJECT (qtdemux, "resolve fourcc %08x", fourcc);

  switch (fourcc) {
    case GST_MAKE_FOURCC ('N', 'O', 'N', 'E'):
    case GST_MAKE_FOURCC ('r', 'a', 'w', ' '):
      _codec ("Raw 8-bit PCM audio");
      caps = gst_caps_new_simple ("audio/x-raw-int", "width", G_TYPE_INT, 8,
          "depth", G_TYPE_INT, 8, "signed", G_TYPE_BOOLEAN, FALSE, NULL);
      break;
    case GST_MAKE_FOURCC ('t', 'w', 'o', 's'):
      endian = G_BIG_ENDIAN;
      /* fall-through */
    case GST_MAKE_FOURCC ('s', 'o', 'w', 't'):
    {
      gchar *str;
      gint depth;

      if (!endian)
        endian = G_LITTLE_ENDIAN;

      depth = stream->bytes_per_packet * 8;
      str = g_strdup_printf ("Raw %d-bit PCM audio", depth);
      _codec (str);
      g_free (str);
      caps = gst_caps_new_simple ("audio/x-raw-int",
          "width", G_TYPE_INT, depth, "depth", G_TYPE_INT, depth,
          "endianness", G_TYPE_INT, endian,
          "signed", G_TYPE_BOOLEAN, TRUE, NULL);
      break;
    }
    case GST_MAKE_FOURCC ('f', 'l', '6', '4'):
      _codec ("Raw 64-bit floating-point audio");
      caps = gst_caps_new_simple ("audio/x-raw-float", "width", G_TYPE_INT, 64,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN, NULL);
      break;
    case GST_MAKE_FOURCC ('f', 'l', '3', '2'):
      _codec ("Raw 32-bit floating-point audio");
      caps = gst_caps_new_simple ("audio/x-raw-float", "width", G_TYPE_INT, 32,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN, NULL);
      break;
    case FOURCC_in24:
      _codec ("Raw 24-bit PCM audio");
      /* we assume BIG ENDIAN, an enda box will tell us to change this to little
       * endian later */
      caps = gst_caps_new_simple ("audio/x-raw-int", "width", G_TYPE_INT, 24,
          "depth", G_TYPE_INT, 24,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN,
          "signed", G_TYPE_BOOLEAN, TRUE, NULL);
      break;
    case GST_MAKE_FOURCC ('i', 'n', '3', '2'):
      _codec ("Raw 32-bit PCM audio");
      caps = gst_caps_new_simple ("audio/x-raw-int", "width", G_TYPE_INT, 32,
          "depth", G_TYPE_INT, 32,
          "endianness", G_TYPE_INT, G_BIG_ENDIAN,
          "signed", G_TYPE_BOOLEAN, TRUE, NULL);
      break;
    case GST_MAKE_FOURCC ('u', 'l', 'a', 'w'):
      _codec ("Mu-law audio");
      caps = gst_caps_new_simple ("audio/x-mulaw", NULL);
      break;
    case GST_MAKE_FOURCC ('a', 'l', 'a', 'w'):
      _codec ("A-law audio");
      caps = gst_caps_new_simple ("audio/x-alaw", NULL);
      break;
    case 0x0200736d:
    case 0x6d730002:
      _codec ("Microsoft ADPCM");
      /* Microsoft ADPCM-ACM code 2 */
      caps = gst_caps_new_simple ("audio/x-adpcm",
          "layout", G_TYPE_STRING, "microsoft", NULL);
      break;
    case 0x1100736d:
    case 0x6d730011:
      _codec ("DVI/IMA ADPCM");
      caps = gst_caps_new_simple ("audio/x-adpcm",
          "layout", G_TYPE_STRING, "dvi", NULL);
      break;
    case 0x1700736d:
    case 0x6d730017:
      _codec ("DVI/Intel IMA ADPCM");
      /* FIXME DVI/Intel IMA ADPCM/ACM code 17 */
      caps = gst_caps_new_simple ("audio/x-adpcm",
          "layout", G_TYPE_STRING, "quicktime", NULL);
      break;
    case 0x5500736d:
    case 0x6d730055:
      /* MPEG layer 3, CBR only (pre QT4.1) */
    case GST_MAKE_FOURCC ('.', 'm', 'p', '3'):
      _codec ("MPEG-1 layer 3");
      /* MPEG layer 3, CBR & VBR (QT4.1 and later) */
      caps = gst_caps_new_simple ("audio/mpeg", "layer", G_TYPE_INT, 3,
          "mpegversion", G_TYPE_INT, 1, NULL);
      break;
    case 0x20736d:
    case GST_MAKE_FOURCC ('e', 'c', '-', '3'):
      _codec ("EAC-3 audio");
      caps = gst_caps_new_simple ("audio/x-eac3",
          "framed", G_TYPE_BOOLEAN, TRUE, NULL);
      stream->sampled = TRUE;
      break;
    case GST_MAKE_FOURCC ('a', 'c', '-', '3'):
      _codec ("AC-3 audio");
      caps = gst_caps_new_simple ("audio/x-ac3",
          "framed", G_TYPE_BOOLEAN, TRUE, NULL);
      stream->sampled = TRUE;
      break;
    case GST_MAKE_FOURCC ('M', 'A', 'C', '3'):
      _codec ("MACE-3");
      caps = gst_caps_new_simple ("audio/x-mace",
          "maceversion", G_TYPE_INT, 3, NULL);
      break;
    case GST_MAKE_FOURCC ('M', 'A', 'C', '6'):
      _codec ("MACE-6");
      caps = gst_caps_new_simple ("audio/x-mace",
          "maceversion", G_TYPE_INT, 6, NULL);
      break;
    case GST_MAKE_FOURCC ('O', 'g', 'g', 'V'):
      /* ogg/vorbis */
      caps = gst_caps_new_simple ("application/ogg", NULL);
      break;
    case GST_MAKE_FOURCC ('d', 'v', 'c', 'a'):
      _codec ("DV audio");
      caps = gst_caps_new_simple ("audio/x-dv", NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'p', '4', 'a'):
      _codec ("MPEG-4 AAC audio");
      caps = gst_caps_new_simple ("audio/mpeg",
          "mpegversion", G_TYPE_INT, 4, "framed", G_TYPE_BOOLEAN, TRUE,
          "stream-format", G_TYPE_STRING, "raw", NULL);
      break;
    case GST_MAKE_FOURCC ('Q', 'D', 'M', 'C'):
      _codec ("QDesign Music");
      caps = gst_caps_new_simple ("audio/x-qdm", NULL);
      break;
    case GST_MAKE_FOURCC ('Q', 'D', 'M', '2'):
      _codec ("QDesign Music v.2");
      /* FIXME: QDesign music version 2 (no constant) */
      if (data) {
        caps = gst_caps_new_simple ("audio/x-qdm2",
            "framesize", G_TYPE_INT, QT_UINT32 (data + 52),
            "bitrate", G_TYPE_INT, QT_UINT32 (data + 40),
            "blocksize", G_TYPE_INT, QT_UINT32 (data + 44), NULL);
      } else {
        caps = gst_caps_new_simple ("audio/x-qdm2", NULL);
      }
      break;
    case GST_MAKE_FOURCC ('a', 'g', 's', 'm'):
      _codec ("GSM audio");
      caps = gst_caps_new_simple ("audio/x-gsm", NULL);
      break;
    case GST_MAKE_FOURCC ('s', 'a', 'm', 'r'):
      _codec ("AMR audio");
      caps = gst_caps_new_simple ("audio/AMR", NULL);
      break;
    case GST_MAKE_FOURCC ('s', 'a', 'w', 'b'):
      _codec ("AMR-WB audio");
      caps = gst_caps_new_simple ("audio/AMR-WB", NULL);
      break;
    case GST_MAKE_FOURCC ('i', 'm', 'a', '4'):
      _codec ("Quicktime IMA ADPCM");
      caps = gst_caps_new_simple ("audio/x-adpcm",
          "layout", G_TYPE_STRING, "quicktime", NULL);
      break;
    case GST_MAKE_FOURCC ('a', 'l', 'a', 'c'):
      _codec ("Apple lossless audio");
      caps = gst_caps_new_simple ("audio/x-alac", NULL);
      break;
    case GST_MAKE_FOURCC ('Q', 'c', 'l', 'p'):
      _codec ("QualComm PureVoice");
      caps = gst_caps_from_string ("audio/qcelp");
      break;
    case FOURCC_owma:
      _codec ("WMA");
      caps = gst_caps_new_simple ("audio/x-wma", NULL);
      break;
    case GST_MAKE_FOURCC ('q', 't', 'v', 'r'):
      /* ? */
    default:
    {
      char *s;

      s = g_strdup_printf ("audio/x-gst-fourcc-%" GST_FOURCC_FORMAT,
          GST_FOURCC_ARGS (fourcc));
      caps = gst_caps_new_simple (s, NULL);
      break;
    }
  }

  /* enable clipping for raw audio streams */
  s = gst_caps_get_structure (caps, 0);
  name = gst_structure_get_name (s);
  if (g_str_has_prefix (name, "audio/x-raw-")) {
    stream->need_clip = TRUE;
  }
  return caps;
}

static GstCaps *
qtdemux_sub_caps (GstQTDemux * qtdemux, QtDemuxStream * stream,
    guint32 fourcc, const guint8 * stsd_data, gchar ** codec_name)
{
  GstCaps *caps;

  GST_DEBUG_OBJECT (qtdemux, "resolve fourcc %08x", fourcc);

  switch (fourcc) {
    case GST_MAKE_FOURCC ('m', 'p', '4', 's'):
      _codec ("DVD subtitle");
      caps = gst_caps_new_simple ("video/x-dvd-subpicture", NULL);
      break;
    case GST_MAKE_FOURCC ('t', 'e', 'x', 't'):
      _codec ("Quicktime timed text");
      goto text;
    case GST_MAKE_FOURCC ('t', 'x', '3', 'g'):
      _codec ("3GPP timed text");
    text:
      caps = gst_caps_new_simple ("text/plain", NULL);
      /* actual text piece needs to be extracted */
      stream->need_process = TRUE;
      break;
    default:
    {
      char *s;

      s = g_strdup_printf ("text/x-gst-fourcc-%" GST_FOURCC_FORMAT,
          GST_FOURCC_ARGS (fourcc));
      caps = gst_caps_new_simple (s, NULL);
      break;
    }
  }
  return caps;
}
