/* GStreamer
 * Copyright (C) 2009 Igalia S.L.
 * Author: Iago Toral Quiroga <itoral@igalia.com>
 * Copyright (C) 2011 Mark Nauwelaerts <mark.nauwelaerts@collabora.co.uk>.
 * Copyright (C) 2011 Nokia Corporation. All rights reserved.
 *   Contact: Stefan Kost <stefan.kost@nokia.com>
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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * SECTION:gstaudiodecoder
 * @title: GstAudioDecoder
 * @short_description: Base class for audio decoders
 * @see_also: #GstBaseTransform
 *
 * This base class is for audio decoders turning encoded data into
 * raw audio samples.
 *
 * GstAudioDecoder and subclass should cooperate as follows.
 *
 * ## Configuration
 *
 *   * Initially, GstAudioDecoder calls @start when the decoder element
 *     is activated, which allows subclass to perform any global setup.
 *     Base class (context) parameters can already be set according to subclass
 *     capabilities (or possibly upon receive more information in subsequent
 *     @set_format).
 *   * GstAudioDecoder calls @set_format to inform subclass of the format
 *     of input audio data that it is about to receive.
 *     While unlikely, it might be called more than once, if changing input
 *     parameters require reconfiguration.
 *   * GstAudioDecoder calls @stop at end of all processing.
 *
 * As of configuration stage, and throughout processing, GstAudioDecoder
 * provides various (context) parameters, e.g. describing the format of
 * output audio data (valid when output caps have been set) or current parsing state.
 * Conversely, subclass can and should configure context to inform
 * base class of its expectation w.r.t. buffer handling.
 *
 * ## Data processing
 *     * Base class gathers input data, and optionally allows subclass
 *       to parse this into subsequently manageable (as defined by subclass)
 *       chunks.  Such chunks are subsequently referred to as 'frames',
 *       though they may or may not correspond to 1 (or more) audio format frame.
 *     * Input frame is provided to subclass' @handle_frame.
 *     * If codec processing results in decoded data, subclass should call
 *       @gst_audio_decoder_finish_frame to have decoded data pushed
 *       downstream.
 *     * Just prior to actually pushing a buffer downstream,
 *       it is passed to @pre_push.  Subclass should either use this callback
 *       to arrange for additional downstream pushing or otherwise ensure such
 *       custom pushing occurs after at least a method call has finished since
 *       setting src pad caps.
 *     * During the parsing process GstAudioDecoderClass will handle both
 *       srcpad and sinkpad events. Sink events will be passed to subclass
 *       if @event callback has been provided.
 *
 * ## Shutdown phase
 *
 *   * GstAudioDecoder class calls @stop to inform the subclass that data
 *     parsing will be stopped.
 *
 * Subclass is responsible for providing pad template caps for
 * source and sink pads. The pads need to be named "sink" and "src". It also
 * needs to set the fixed caps on srcpad, when the format is ensured.  This
 * is typically when base class calls subclass' @set_format function, though
 * it might be delayed until calling @gst_audio_decoder_finish_frame.
 *
 * In summary, above process should have subclass concentrating on
 * codec data processing while leaving other matters to base class,
 * such as most notably timestamp handling.  While it may exert more control
 * in this area (see e.g. @pre_push), it is very much not recommended.
 *
 * In particular, base class will try to arrange for perfect output timestamps
 * as much as possible while tracking upstream timestamps.
 * To this end, if deviation between the next ideal expected perfect timestamp
 * and upstream exceeds #GstAudioDecoder:tolerance, then resync to upstream
 * occurs (which would happen always if the tolerance mechanism is disabled).
 *
 * In non-live pipelines, baseclass can also (configurably) arrange for
 * output buffer aggregation which may help to redue large(r) numbers of
 * small(er) buffers being pushed and processed downstream. Note that this
 * feature is only available if the buffer layout is interleaved. For planar
 * buffers, the decoder implementation is fully responsible for the output
 * buffer size.
 *
 * On the other hand, it should be noted that baseclass only provides limited
 * seeking support (upon explicit subclass request), as full-fledged support
 * should rather be left to upstream demuxer, parser or alike.  This simple
 * approach caters for seeking and duration reporting using estimated input
 * bitrates.
 *
 * Things that subclass need to take care of:
 *
 *   * Provide pad templates
 *   * Set source pad caps when appropriate
 *   * Set user-configurable properties to sane defaults for format and
 *      implementing codec at hand, and convey some subclass capabilities and
 *      expectations in context.
 *
 *   * Accept data in @handle_frame and provide encoded results to
 *      @gst_audio_decoder_finish_frame.  If it is prepared to perform
 *      PLC, it should also accept NULL data in @handle_frame and provide for
 *      data for indicated duration.
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstaudiodecoder.h"
#include "gstaudioutilsprivate.h"
#include <gst/pbutils/descriptions.h>

#include <string.h>

GST_DEBUG_CATEGORY (audiodecoder_debug);
#define GST_CAT_DEFAULT audiodecoder_debug

enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_LATENCY,
  PROP_TOLERANCE,
  PROP_PLC,
  PROP_MAX_ERRORS
};

#define DEFAULT_LATENCY    0
#define DEFAULT_TOLERANCE  0
#define DEFAULT_PLC        FALSE
#define DEFAULT_DRAINABLE  TRUE
#define DEFAULT_NEEDS_FORMAT  FALSE
#define DEFAULT_MAX_ERRORS GST_AUDIO_DECODER_MAX_ERRORS

typedef struct _GstAudioDecoderContext
{
  /* last negotiated input caps */
  GstCaps *input_caps;

  /* (output) audio format */
  GstAudioInfo info;
  GstCaps *caps;
  gboolean output_format_changed;

  /* parsing state */
  gboolean eos;
  gboolean sync;

  gboolean had_output_data;
  gboolean had_input_data;

  /* misc */
  gint delay;

  /* output */
  gboolean do_plc;
  gboolean do_estimate_rate;
  GstCaps *allocation_caps;
  /* MT-protected (with LOCK) */
  GstClockTime min_latency;
  GstClockTime max_latency;

  GstAllocator *allocator;
  GstAllocationParams params;
} GstAudioDecoderContext;

struct _GstAudioDecoderPrivate
{
  /* activation status */
  gboolean active;

  /* input base/first ts as basis for output ts */
  GstClockTime base_ts;
  /* input samples processed and sent downstream so far (w.r.t. base_ts) */
  guint64 samples;

  /* collected input data */
  GstAdapter *adapter;
  /* tracking input ts for changes */
  GstClockTime prev_ts;
  guint64 prev_distance;
  /* frames obtained from input */
  GQueue frames;
  /* collected output data */
  GstAdapter *adapter_out;
  /* ts and duration for output data collected above */
  GstClockTime out_ts, out_dur;
  /* mark outgoing discont */
  gboolean discont;

  /* subclass gave all it could already */
  gboolean drained;
  /* subclass currently being forcibly drained */
  gboolean force;
  /* input_segment are output_segment identical */
  gboolean in_out_segment_sync;
  /* TRUE if we have an active set of instant rate flags */
  gboolean decode_flags_override;
  GstSegmentFlags decode_flags;

  /* expecting the buffer with DISCONT flag */
  gboolean expecting_discont_buf;

  /* number of samples pushed out via _finish_subframe(), resets on _finish_frame() */
  guint subframe_samples;

  /* input bps estimatation */
  /* global in bytes seen */
  guint64 bytes_in;
  /* global samples sent out */
  guint64 samples_out;
  /* bytes flushed during parsing */
  guint sync_flush;
  /* error count */
  gint error_count;
  /* max errors */
  gint max_errors;

  /* upstream stream tags (global tags are passed through as-is) */
  GstTagList *upstream_tags;

  /* subclass tags */
  GstTagList *taglist;          /* FIXME: rename to decoder_tags */
  GstTagMergeMode decoder_tags_merge_mode;

  gboolean taglist_changed;     /* FIXME: rename to tags_changed */

  /* whether circumstances allow output aggregation */
  gint agg;

  /* reverse playback queues */
  /* collect input */
  GList *gather;
  /* to-be-decoded */
  GList *decode;
  /* reversed output */
  GList *queued;

  /* context storage */
  GstAudioDecoderContext ctx;

  /* properties */
  GstClockTime latency;
  GstClockTime tolerance;
  gboolean plc;
  gboolean drainable;
  gboolean needs_format;

  /* pending serialized sink events, will be sent from finish_frame() */
  GList *pending_events;

  /* flags */
  gboolean use_default_pad_acceptcaps;
};

/* cached quark to avoid contention on the global quark table lock */
#define META_TAG_AUDIO meta_tag_audio_quark
static GQuark meta_tag_audio_quark;

static void gst_audio_decoder_finalize (GObject * object);
static void gst_audio_decoder_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_audio_decoder_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static void gst_audio_decoder_clear_queues (GstAudioDecoder * dec);
static GstFlowReturn gst_audio_decoder_chain_reverse (GstAudioDecoder *
    dec, GstBuffer * buf);

static GstStateChangeReturn gst_audio_decoder_change_state (GstElement *
    element, GstStateChange transition);
static gboolean gst_audio_decoder_sink_eventfunc (GstAudioDecoder * dec,
    GstEvent * event);
static gboolean gst_audio_decoder_src_eventfunc (GstAudioDecoder * dec,
    GstEvent * event);
static gboolean gst_audio_decoder_sink_event (GstPad * pad, GstObject * parent,
    GstEvent * event);
static gboolean gst_audio_decoder_src_event (GstPad * pad, GstObject * parent,
    GstEvent * event);
static gboolean gst_audio_decoder_sink_setcaps (GstAudioDecoder * dec,
    GstCaps * caps);
static GstFlowReturn gst_audio_decoder_chain (GstPad * pad, GstObject * parent,
    GstBuffer * buf);
static gboolean gst_audio_decoder_src_query (GstPad * pad, GstObject * parent,
    GstQuery * query);
static gboolean gst_audio_decoder_sink_query (GstPad * pad, GstObject * parent,
    GstQuery * query);
static void gst_audio_decoder_reset (GstAudioDecoder * dec, gboolean full);

static gboolean gst_audio_decoder_decide_allocation_default (GstAudioDecoder *
    dec, GstQuery * query);
static gboolean gst_audio_decoder_propose_allocation_default (GstAudioDecoder *
    dec, GstQuery * query);
static gboolean gst_audio_decoder_negotiate_default (GstAudioDecoder * dec);
static gboolean gst_audio_decoder_negotiate_unlocked (GstAudioDecoder * dec);
static gboolean gst_audio_decoder_handle_gap (GstAudioDecoder * dec,
    GstEvent * event);
static gboolean gst_audio_decoder_sink_query_default (GstAudioDecoder * dec,
    GstQuery * query);
static gboolean gst_audio_decoder_src_query_default (GstAudioDecoder * dec,
    GstQuery * query);

static gboolean gst_audio_decoder_transform_meta_default (GstAudioDecoder *
    decoder, GstBuffer * outbuf, GstMeta * meta, GstBuffer * inbuf);

static GstFlowReturn
gst_audio_decoder_finish_frame_or_subframe (GstAudioDecoder * dec,
    GstBuffer * buf, gint frames);

static GstElementClass *parent_class = NULL;
static gint private_offset = 0;

static void gst_audio_decoder_class_init (GstAudioDecoderClass * klass);
static void gst_audio_decoder_init (GstAudioDecoder * dec,
    GstAudioDecoderClass * klass);

GType
gst_audio_decoder_get_type (void)
{
  static volatile gsize audio_decoder_type = 0;

  if (g_once_init_enter (&audio_decoder_type)) {
    GType _type;
    static const GTypeInfo audio_decoder_info = {
      sizeof (GstAudioDecoderClass),
      NULL,
      NULL,
      (GClassInitFunc) gst_audio_decoder_class_init,
      NULL,
      NULL,
      sizeof (GstAudioDecoder),
      0,
      (GInstanceInitFunc) gst_audio_decoder_init,
    };

    _type = g_type_register_static (GST_TYPE_ELEMENT,
        "GstAudioDecoder", &audio_decoder_info, G_TYPE_FLAG_ABSTRACT);

    private_offset =
        g_type_add_instance_private (_type, sizeof (GstAudioDecoderPrivate));

    g_once_init_leave (&audio_decoder_type, _type);
  }
  return audio_decoder_type;
}

static inline GstAudioDecoderPrivate *
gst_audio_decoder_get_instance_private (GstAudioDecoder * self)
{
  return (G_STRUCT_MEMBER_P (self, private_offset));
}

static void
gst_audio_decoder_class_init (GstAudioDecoderClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *element_class;
  GstAudioDecoderClass *audiodecoder_class;

  gobject_class = G_OBJECT_CLASS (klass);
  element_class = GST_ELEMENT_CLASS (klass);
  audiodecoder_class = GST_AUDIO_DECODER_CLASS (klass);

  parent_class = g_type_class_peek_parent (klass);

  if (private_offset != 0)
    g_type_class_adjust_private_offset (klass, &private_offset);

  GST_DEBUG_CATEGORY_INIT (audiodecoder_debug, "audiodecoder", 0,
      "audio decoder base class");

  gobject_class->set_property = gst_audio_decoder_set_property;
  gobject_class->get_property = gst_audio_decoder_get_property;
  gobject_class->finalize = gst_audio_decoder_finalize;

  element_class->change_state =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_change_state);

  /* Properties */
  g_object_class_install_property (gobject_class, PROP_LATENCY,
      g_param_spec_int64 ("min-latency", "Minimum Latency",
          "Aggregate output data to a minimum of latency time (ns)",
          0, G_MAXINT64, DEFAULT_LATENCY,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_TOLERANCE,
      g_param_spec_int64 ("tolerance", "Tolerance",
          "Perfect ts while timestamp jitter/imperfection within tolerance (ns)",
          0, G_MAXINT64, DEFAULT_TOLERANCE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PLC,
      g_param_spec_boolean ("plc", "Packet Loss Concealment",
          "Perform packet loss concealment (if supported)",
          DEFAULT_PLC, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstAudioDecoder:max-errors:
   *
   * Maximum number of tolerated consecutive decode errors. See
   * gst_audio_decoder_set_max_errors() for more details.
   *
   * Since: 1.18
   */
  g_object_class_install_property (gobject_class, PROP_MAX_ERRORS,
      g_param_spec_int ("max-errors", "Max errors",
          "Max consecutive decoder errors before returning flow error",
          -1, G_MAXINT, DEFAULT_MAX_ERRORS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  audiodecoder_class->sink_event =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_sink_eventfunc);
  audiodecoder_class->src_event =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_src_eventfunc);
  audiodecoder_class->propose_allocation =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_propose_allocation_default);
  audiodecoder_class->decide_allocation =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_decide_allocation_default);
  audiodecoder_class->negotiate =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_negotiate_default);
  audiodecoder_class->sink_query =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_sink_query_default);
  audiodecoder_class->src_query =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_src_query_default);
  audiodecoder_class->transform_meta =
      GST_DEBUG_FUNCPTR (gst_audio_decoder_transform_meta_default);

  meta_tag_audio_quark = g_quark_from_static_string (GST_META_TAG_AUDIO_STR);
}

static void
gst_audio_decoder_init (GstAudioDecoder * dec, GstAudioDecoderClass * klass)
{
  GstPadTemplate *pad_template;

  GST_DEBUG_OBJECT (dec, "gst_audio_decoder_init");

  dec->priv = gst_audio_decoder_get_instance_private (dec);

  /* Setup sink pad */
  pad_template =
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (klass), "sink");
  g_return_if_fail (pad_template != NULL);

  dec->sinkpad = gst_pad_new_from_template (pad_template, "sink");
  gst_pad_set_event_function (dec->sinkpad,
      GST_DEBUG_FUNCPTR (gst_audio_decoder_sink_event));
  gst_pad_set_chain_function (dec->sinkpad,
      GST_DEBUG_FUNCPTR (gst_audio_decoder_chain));
  gst_pad_set_query_function (dec->sinkpad,
      GST_DEBUG_FUNCPTR (gst_audio_decoder_sink_query));
  gst_element_add_pad (GST_ELEMENT (dec), dec->sinkpad);
  GST_DEBUG_OBJECT (dec, "sinkpad created");

  /* Setup source pad */
  pad_template =
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (klass), "src");
  g_return_if_fail (pad_template != NULL);

  dec->srcpad = gst_pad_new_from_template (pad_template, "src");
  gst_pad_set_event_function (dec->srcpad,
      GST_DEBUG_FUNCPTR (gst_audio_decoder_src_event));
  gst_pad_set_query_function (dec->srcpad,
      GST_DEBUG_FUNCPTR (gst_audio_decoder_src_query));
  gst_element_add_pad (GST_ELEMENT (dec), dec->srcpad);
  GST_DEBUG_OBJECT (dec, "srcpad created");

  dec->priv->adapter = gst_adapter_new ();
  dec->priv->adapter_out = gst_adapter_new ();
  g_queue_init (&dec->priv->frames);

  g_rec_mutex_init (&dec->stream_lock);

  /* property default */
  dec->priv->latency = DEFAULT_LATENCY;
  dec->priv->tolerance = DEFAULT_TOLERANCE;
  dec->priv->plc = DEFAULT_PLC;
  dec->priv->drainable = DEFAULT_DRAINABLE;
  dec->priv->needs_format = DEFAULT_NEEDS_FORMAT;
  dec->priv->max_errors = GST_AUDIO_DECODER_MAX_ERRORS;

  /* init state */
  dec->priv->ctx.min_latency = 0;
  dec->priv->ctx.max_latency = 0;
  gst_audio_decoder_reset (dec, TRUE);
  GST_DEBUG_OBJECT (dec, "init ok");
}

static void
gst_audio_decoder_reset (GstAudioDecoder * dec, gboolean full)
{
  GST_DEBUG_OBJECT (dec, "gst_audio_decoder_reset");

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  if (full) {
    dec->priv->active = FALSE;
    GST_OBJECT_LOCK (dec);
    dec->priv->bytes_in = 0;
    dec->priv->samples_out = 0;
    GST_OBJECT_UNLOCK (dec);
    dec->priv->agg = -1;
    dec->priv->error_count = 0;
    gst_audio_decoder_clear_queues (dec);

    if (dec->priv->taglist) {
      gst_tag_list_unref (dec->priv->taglist);
      dec->priv->taglist = NULL;
    }
    dec->priv->decoder_tags_merge_mode = GST_TAG_MERGE_KEEP_ALL;
    if (dec->priv->upstream_tags) {
      gst_tag_list_unref (dec->priv->upstream_tags);
      dec->priv->upstream_tags = NULL;
    }
    dec->priv->taglist_changed = FALSE;

    gst_segment_init (&dec->input_segment, GST_FORMAT_TIME);
    gst_segment_init (&dec->output_segment, GST_FORMAT_TIME);
    dec->priv->in_out_segment_sync = TRUE;

    g_list_foreach (dec->priv->pending_events, (GFunc) gst_event_unref, NULL);
    g_list_free (dec->priv->pending_events);
    dec->priv->pending_events = NULL;

    if (dec->priv->ctx.allocator)
      gst_object_unref (dec->priv->ctx.allocator);

    GST_OBJECT_LOCK (dec);
    dec->priv->decode_flags_override = FALSE;
    gst_caps_replace (&dec->priv->ctx.input_caps, NULL);
    gst_caps_replace (&dec->priv->ctx.caps, NULL);
    gst_caps_replace (&dec->priv->ctx.allocation_caps, NULL);

    memset (&dec->priv->ctx, 0, sizeof (dec->priv->ctx));

    gst_audio_info_init (&dec->priv->ctx.info);
    GST_OBJECT_UNLOCK (dec);
    dec->priv->ctx.had_output_data = FALSE;
    dec->priv->ctx.had_input_data = FALSE;
  }

  g_queue_foreach (&dec->priv->frames, (GFunc) gst_buffer_unref, NULL);
  g_queue_clear (&dec->priv->frames);
  gst_adapter_clear (dec->priv->adapter);
  gst_adapter_clear (dec->priv->adapter_out);
  dec->priv->out_ts = GST_CLOCK_TIME_NONE;
  dec->priv->out_dur = 0;
  dec->priv->prev_ts = GST_CLOCK_TIME_NONE;
  dec->priv->prev_distance = 0;
  dec->priv->drained = TRUE;
  dec->priv->base_ts = GST_CLOCK_TIME_NONE;
  dec->priv->samples = 0;
  dec->priv->discont = TRUE;
  dec->priv->sync_flush = FALSE;

  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
}

static void
gst_audio_decoder_finalize (GObject * object)
{
  GstAudioDecoder *dec;

  g_return_if_fail (GST_IS_AUDIO_DECODER (object));
  dec = GST_AUDIO_DECODER (object);

  if (dec->priv->adapter) {
    g_object_unref (dec->priv->adapter);
  }
  if (dec->priv->adapter_out) {
    g_object_unref (dec->priv->adapter_out);
  }

  g_rec_mutex_clear (&dec->stream_lock);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static GstEvent *
gst_audio_decoder_create_merged_tags_event (GstAudioDecoder * dec)
{
  GstTagList *merged_tags;

  GST_LOG_OBJECT (dec, "upstream : %" GST_PTR_FORMAT, dec->priv->upstream_tags);
  GST_LOG_OBJECT (dec, "decoder  : %" GST_PTR_FORMAT, dec->priv->taglist);
  GST_LOG_OBJECT (dec, "mode     : %d", dec->priv->decoder_tags_merge_mode);

  merged_tags =
      gst_tag_list_merge (dec->priv->upstream_tags,
      dec->priv->taglist, dec->priv->decoder_tags_merge_mode);

  GST_DEBUG_OBJECT (dec, "merged   : %" GST_PTR_FORMAT, merged_tags);

  if (merged_tags == NULL)
    return NULL;

  if (gst_tag_list_is_empty (merged_tags)) {
    gst_tag_list_unref (merged_tags);
    return NULL;
  }

  return gst_event_new_tag (merged_tags);
}

static gboolean
gst_audio_decoder_push_event (GstAudioDecoder * dec, GstEvent * event)
{
  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEGMENT:{
      GstSegment seg;

      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      gst_event_copy_segment (event, &seg);

      GST_DEBUG_OBJECT (dec, "starting segment %" GST_SEGMENT_FORMAT, &seg);

      dec->output_segment = seg;
      dec->priv->in_out_segment_sync =
          gst_segment_is_equal (&dec->input_segment, &seg);
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
      break;
    }
    default:
      break;
  }

  return gst_pad_push_event (dec->srcpad, event);
}

static gboolean
gst_audio_decoder_negotiate_default (GstAudioDecoder * dec)
{
  GstAudioDecoderClass *klass;
  gboolean res = TRUE;
  GstCaps *caps;
  GstCaps *prevcaps;
  GstQuery *query = NULL;
  GstAllocator *allocator;
  GstAllocationParams params;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);
  g_return_val_if_fail (GST_AUDIO_INFO_IS_VALID (&dec->priv->ctx.info), FALSE);
  g_return_val_if_fail (GST_IS_CAPS (dec->priv->ctx.caps), FALSE);

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  caps = dec->priv->ctx.caps;
  if (dec->priv->ctx.allocation_caps == NULL)
    dec->priv->ctx.allocation_caps = gst_caps_ref (caps);

  GST_DEBUG_OBJECT (dec, "setting src caps %" GST_PTR_FORMAT, caps);

  if (dec->priv->pending_events) {
    GList **pending_events, *l;

    pending_events = &dec->priv->pending_events;

    GST_DEBUG_OBJECT (dec, "Pushing pending events");
    for (l = *pending_events; l;) {
      GstEvent *event = GST_EVENT (l->data);
      GList *tmp;

      if (GST_EVENT_TYPE (event) < GST_EVENT_CAPS) {
        gst_audio_decoder_push_event (dec, l->data);
        tmp = l;
        l = l->next;
        *pending_events = g_list_delete_link (*pending_events, tmp);
      } else {
        l = l->next;
      }
    }
  }

  prevcaps = gst_pad_get_current_caps (dec->srcpad);
  if (!prevcaps || !gst_caps_is_equal (prevcaps, caps))
    res = gst_pad_set_caps (dec->srcpad, caps);
  if (prevcaps)
    gst_caps_unref (prevcaps);

  if (!res)
    goto done;
  dec->priv->ctx.output_format_changed = FALSE;

  query = gst_query_new_allocation (dec->priv->ctx.allocation_caps, TRUE);
  if (!gst_pad_peer_query (dec->srcpad, query)) {
    GST_DEBUG_OBJECT (dec, "didn't get downstream ALLOCATION hints");
  }

  g_assert (klass->decide_allocation != NULL);
  res = klass->decide_allocation (dec, query);

  GST_DEBUG_OBJECT (dec, "ALLOCATION (%d) params: %" GST_PTR_FORMAT, res,
      query);

  if (!res)
    goto no_decide_allocation;

  /* we got configuration from our peer or the decide_allocation method,
   * parse them */
  if (gst_query_get_n_allocation_params (query) > 0) {
    gst_query_parse_nth_allocation_param (query, 0, &allocator, &params);
  } else {
    allocator = NULL;
    gst_allocation_params_init (&params);
  }

  if (dec->priv->ctx.allocator)
    gst_object_unref (dec->priv->ctx.allocator);
  dec->priv->ctx.allocator = allocator;
  dec->priv->ctx.params = params;

done:

  if (query)
    gst_query_unref (query);

  return res;

  /* ERRORS */
no_decide_allocation:
  {
    GST_WARNING_OBJECT (dec, "Subclass failed to decide allocation");
    goto done;
  }
}

static gboolean
gst_audio_decoder_negotiate_unlocked (GstAudioDecoder * dec)
{
  GstAudioDecoderClass *klass = GST_AUDIO_DECODER_GET_CLASS (dec);
  gboolean ret = TRUE;

  if (G_LIKELY (klass->negotiate))
    ret = klass->negotiate (dec);

  return ret;
}

/**
 * gst_audio_decoder_negotiate:
 * @dec: a #GstAudioDecoder
 *
 * Negotiate with downstream elements to currently configured #GstAudioInfo.
 * Unmark GST_PAD_FLAG_NEED_RECONFIGURE in any case. But mark it again if
 * negotiate fails.
 *
 * Returns: %TRUE if the negotiation succeeded, else %FALSE.
 */
gboolean
gst_audio_decoder_negotiate (GstAudioDecoder * dec)
{
  GstAudioDecoderClass *klass;
  gboolean res = TRUE;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_AUDIO_DECODER_STREAM_LOCK (dec);
  gst_pad_check_reconfigure (dec->srcpad);
  if (klass->negotiate) {
    res = klass->negotiate (dec);
    if (!res)
      gst_pad_mark_reconfigure (dec->srcpad);
  }
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return res;
}

/**
 * gst_audio_decoder_set_output_format:
 * @dec: a #GstAudioDecoder
 * @info: #GstAudioInfo
 *
 * Configure output info on the srcpad of @dec.
 *
 * Returns: %TRUE on success.
 **/
gboolean
gst_audio_decoder_set_output_format (GstAudioDecoder * dec,
    const GstAudioInfo * info)
{
  gboolean res = TRUE;
  GstCaps *caps = NULL;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);
  g_return_val_if_fail (GST_AUDIO_INFO_IS_VALID (info), FALSE);

  /* If the audio info can't be converted to caps,
   * it was invalid */
  caps = gst_audio_info_to_caps (info);
  if (!caps) {
    GST_WARNING_OBJECT (dec, "invalid output format");
    return FALSE;
  }

  res = gst_audio_decoder_set_output_caps (dec, caps);
  gst_caps_unref (caps);

  return res;
}

/**
 * gst_audio_decoder_set_output_caps:
 * @dec: a #GstAudioDecoder
 * @caps: (transfer none): (fixed) #GstCaps
 *
 * Configure output caps on the srcpad of @dec. Similar to
 * gst_audio_decoder_set_output_format(), but allows subclasses to specify
 * output caps that can't be expressed via #GstAudioInfo e.g. caps that have
 * caps features.
 *
 * Returns: %TRUE on success.
 *
 * Since: 1.16
 **/
gboolean
gst_audio_decoder_set_output_caps (GstAudioDecoder * dec, GstCaps * caps)
{
  gboolean res = TRUE;
  guint old_rate;
  GstCaps *templ_caps;
  GstAudioInfo info;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);

  GST_DEBUG_OBJECT (dec, "Setting srcpad caps %" GST_PTR_FORMAT, caps);

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  if (!gst_caps_is_fixed (caps))
    goto refuse_caps;

  /* check if caps can be parsed */
  if (!gst_audio_info_from_caps (&info, caps))
    goto refuse_caps;

  /* Only allow caps that are a subset of the template caps */
  templ_caps = gst_pad_get_pad_template_caps (dec->srcpad);
  if (!gst_caps_is_subset (caps, templ_caps)) {
    GST_WARNING_OBJECT (dec, "Requested output format %" GST_PTR_FORMAT
        " do not match template %" GST_PTR_FORMAT, caps, templ_caps);
    gst_caps_unref (templ_caps);
    goto refuse_caps;
  }
  gst_caps_unref (templ_caps);

  /* adjust ts tracking to new sample rate */
  old_rate = GST_AUDIO_INFO_RATE (&dec->priv->ctx.info);
  if (GST_CLOCK_TIME_IS_VALID (dec->priv->base_ts) && old_rate) {
    dec->priv->base_ts +=
        GST_FRAMES_TO_CLOCK_TIME (dec->priv->samples, old_rate);
    dec->priv->samples = 0;
  }

  /* copy the GstAudioInfo */
  GST_OBJECT_LOCK (dec);
  dec->priv->ctx.info = info;
  GST_OBJECT_UNLOCK (dec);

  gst_caps_replace (&dec->priv->ctx.caps, caps);
  dec->priv->ctx.output_format_changed = TRUE;

done:
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return res;

  /* ERRORS */
refuse_caps:
  {
    GST_WARNING_OBJECT (dec, "invalid output format");
    res = FALSE;
    goto done;
  }
}

static gboolean
gst_audio_decoder_sink_setcaps (GstAudioDecoder * dec, GstCaps * caps)
{
  GstAudioDecoderClass *klass;
  gboolean res = TRUE;

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_DEBUG_OBJECT (dec, "caps: %" GST_PTR_FORMAT, caps);

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  if (dec->priv->ctx.input_caps
      && gst_caps_is_equal (dec->priv->ctx.input_caps, caps)) {
    GST_DEBUG_OBJECT (dec, "Caps did not change, not setting again");
    goto done;
  }

  /* NOTE pbutils only needed here */
  /* TODO maybe (only) upstream demuxer/parser etc should handle this ? */
#if 0
  if (!dec->priv->taglist)
    dec->priv->taglist = gst_tag_list_new ();
  dec->priv->taglist = gst_tag_list_make_writable (dec->priv->taglist);
  gst_pb_utils_add_codec_description_to_tag_list (dec->priv->taglist,
      GST_TAG_AUDIO_CODEC, caps);
  dec->priv->taglist_changed = TRUE;
#endif

  if (klass->set_format)
    res = klass->set_format (dec, caps);

  if (res)
    gst_caps_replace (&dec->priv->ctx.input_caps, caps);

done:
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return res;
}

static void
gst_audio_decoder_setup (GstAudioDecoder * dec)
{
  GstQuery *query;
  gboolean res;

  /* check if in live pipeline, then latency messing is no-no */
  query = gst_query_new_latency ();
  res = gst_pad_peer_query (dec->sinkpad, query);
  if (res) {
    gst_query_parse_latency (query, &res, NULL, NULL);
    res = !res;
  }
  gst_query_unref (query);

  /* normalize to bool */
  dec->priv->agg = ! !res;
}

static GstFlowReturn
gst_audio_decoder_push_forward (GstAudioDecoder * dec, GstBuffer * buf)
{
  GstAudioDecoderClass *klass;
  GstAudioDecoderPrivate *priv;
  GstAudioDecoderContext *ctx;
  GstFlowReturn ret = GST_FLOW_OK;
  GstClockTime ts;

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);
  priv = dec->priv;
  ctx = &dec->priv->ctx;

  g_return_val_if_fail (ctx->info.bpf != 0, GST_FLOW_ERROR);

  if (G_UNLIKELY (!buf)) {
    g_assert_not_reached ();
    return GST_FLOW_OK;
  }

  ctx->had_output_data = TRUE;
  ts = GST_BUFFER_TIMESTAMP (buf);

  GST_LOG_OBJECT (dec,
      "clipping buffer of size %" G_GSIZE_FORMAT " with ts %" GST_TIME_FORMAT
      ", duration %" GST_TIME_FORMAT, gst_buffer_get_size (buf),
      GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
      GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));

  /* clip buffer */
  buf = gst_audio_buffer_clip (buf, &dec->output_segment, ctx->info.rate,
      ctx->info.bpf);
  if (G_UNLIKELY (!buf)) {
    GST_DEBUG_OBJECT (dec, "no data after clipping to segment");
    /* only check and return EOS if upstream still
     * in the same segment and interested as such */
    if (dec->priv->in_out_segment_sync) {
      if (dec->output_segment.rate >= 0) {
        if (ts >= dec->output_segment.stop)
          ret = GST_FLOW_EOS;
      } else if (ts < dec->output_segment.start) {
        ret = GST_FLOW_EOS;
      }
    }
    goto exit;
  }

  /* decorate */
  if (G_UNLIKELY (priv->discont)) {
    GST_LOG_OBJECT (dec, "marking discont");
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DISCONT);
    priv->discont = FALSE;
  }

  /* track where we are */
  if (G_LIKELY (GST_BUFFER_TIMESTAMP_IS_VALID (buf))) {
    /* duration should always be valid for raw audio */
    g_assert (GST_BUFFER_DURATION_IS_VALID (buf));
    dec->output_segment.position =
        GST_BUFFER_TIMESTAMP (buf) + GST_BUFFER_DURATION (buf);
  }

  if (klass->pre_push) {
    /* last chance for subclass to do some dirty stuff */
    ret = klass->pre_push (dec, &buf);
    if (ret != GST_FLOW_OK || !buf) {
      GST_DEBUG_OBJECT (dec, "subclass returned %s, buf %p",
          gst_flow_get_name (ret), buf);
      if (buf)
        gst_buffer_unref (buf);
      goto exit;
    }
  }

  GST_LOG_OBJECT (dec,
      "pushing buffer of size %" G_GSIZE_FORMAT " with ts %" GST_TIME_FORMAT
      ", duration %" GST_TIME_FORMAT, gst_buffer_get_size (buf),
      GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
      GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));

  ret = gst_pad_push (dec->srcpad, buf);

exit:
  return ret;
}

/* mini aggregator combining output buffers into fewer larger ones,
 * if so allowed/configured */
static GstFlowReturn
gst_audio_decoder_output (GstAudioDecoder * dec, GstBuffer * buf)
{
  GstAudioDecoderPrivate *priv;
  GstFlowReturn ret = GST_FLOW_OK;
  GstBuffer *inbuf = NULL;

  priv = dec->priv;

  if (G_UNLIKELY (priv->agg < 0))
    gst_audio_decoder_setup (dec);

  if (G_LIKELY (buf)) {
    GST_LOG_OBJECT (dec,
        "output buffer of size %" G_GSIZE_FORMAT " with ts %" GST_TIME_FORMAT
        ", duration %" GST_TIME_FORMAT, gst_buffer_get_size (buf),
        GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
        GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));
  }

again:
  inbuf = NULL;
  if (priv->agg && dec->priv->latency > 0 &&
      priv->ctx.info.layout == GST_AUDIO_LAYOUT_INTERLEAVED) {
    gint av;
    gboolean assemble = FALSE;
    const GstClockTimeDiff tol = 10 * GST_MSECOND;
    GstClockTimeDiff diff = -100 * GST_MSECOND;

    av = gst_adapter_available (priv->adapter_out);
    if (G_UNLIKELY (!buf)) {
      /* forcibly send current */
      assemble = TRUE;
      GST_LOG_OBJECT (dec, "forcing fragment flush");
    } else if (av && (!GST_BUFFER_TIMESTAMP_IS_VALID (buf) ||
            !GST_CLOCK_TIME_IS_VALID (priv->out_ts) ||
            ((diff = GST_CLOCK_DIFF (GST_BUFFER_TIMESTAMP (buf),
                        priv->out_ts + priv->out_dur)) > tol) || diff < -tol)) {
      assemble = TRUE;
      GST_LOG_OBJECT (dec, "buffer %d ms apart from current fragment",
          (gint) (diff / GST_MSECOND));
    } else {
      /* add or start collecting */
      if (!av) {
        GST_LOG_OBJECT (dec, "starting new fragment");
        priv->out_ts = GST_BUFFER_TIMESTAMP (buf);
      } else {
        GST_LOG_OBJECT (dec, "adding to fragment");
      }
      gst_adapter_push (priv->adapter_out, buf);
      priv->out_dur += GST_BUFFER_DURATION (buf);
      av += gst_buffer_get_size (buf);
      buf = NULL;
    }
    if (priv->out_dur > dec->priv->latency)
      assemble = TRUE;
    if (av && assemble) {
      GST_LOG_OBJECT (dec, "assembling fragment");
      inbuf = buf;
      buf = gst_adapter_take_buffer (priv->adapter_out, av);
      GST_BUFFER_TIMESTAMP (buf) = priv->out_ts;
      GST_BUFFER_DURATION (buf) = priv->out_dur;
      priv->out_ts = GST_CLOCK_TIME_NONE;
      priv->out_dur = 0;
    }
  }

  if (G_LIKELY (buf)) {
    if (dec->output_segment.rate > 0.0) {
      ret = gst_audio_decoder_push_forward (dec, buf);
      GST_LOG_OBJECT (dec, "buffer pushed: %s", gst_flow_get_name (ret));
    } else {
      ret = GST_FLOW_OK;
      priv->queued = g_list_prepend (priv->queued, buf);
      GST_LOG_OBJECT (dec, "buffer queued");
    }

    if (inbuf) {
      buf = inbuf;
      goto again;
    }
  }

  return ret;
}

static void
send_pending_events (GstAudioDecoder * dec)
{
  GstAudioDecoderPrivate *priv = dec->priv;
  GList *pending_events, *l;

  pending_events = priv->pending_events;
  priv->pending_events = NULL;

  GST_DEBUG_OBJECT (dec, "Pushing pending events");
  for (l = pending_events; l; l = l->next)
    gst_audio_decoder_push_event (dec, l->data);
  g_list_free (pending_events);
}

/* Iterate the list of pending events, and ensure
 * the current output segment is up to date for
 * decoding */
static void
apply_pending_events (GstAudioDecoder * dec)
{
  GstAudioDecoderPrivate *priv = dec->priv;
  GList *l;

  GST_DEBUG_OBJECT (dec, "Applying pending segments");
  for (l = priv->pending_events; l; l = l->next) {
    GstEvent *event = GST_EVENT (l->data);
    switch (GST_EVENT_TYPE (event)) {
      case GST_EVENT_SEGMENT:{
        GstSegment seg;

        GST_AUDIO_DECODER_STREAM_LOCK (dec);
        gst_event_copy_segment (event, &seg);

        GST_DEBUG_OBJECT (dec, "starting segment %" GST_SEGMENT_FORMAT, &seg);

        dec->output_segment = seg;
        dec->priv->in_out_segment_sync =
            gst_segment_is_equal (&dec->input_segment, &seg);
        GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
        break;
      }
      default:
        break;
    }
  }
}

static GstFlowReturn
check_pending_reconfigure (GstAudioDecoder * dec)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstAudioDecoderContext *ctx;
  gboolean needs_reconfigure;

  ctx = &dec->priv->ctx;

  needs_reconfigure = gst_pad_check_reconfigure (dec->srcpad);
  if (G_UNLIKELY (ctx->output_format_changed ||
          (GST_AUDIO_INFO_IS_VALID (&ctx->info)
              && needs_reconfigure))) {
    if (!gst_audio_decoder_negotiate_unlocked (dec)) {
      gst_pad_mark_reconfigure (dec->srcpad);
      if (GST_PAD_IS_FLUSHING (dec->srcpad))
        ret = GST_FLOW_FLUSHING;
      else
        ret = GST_FLOW_NOT_NEGOTIATED;
    }
  }
  return ret;
}

static gboolean
gst_audio_decoder_transform_meta_default (GstAudioDecoder *
    decoder, GstBuffer * outbuf, GstMeta * meta, GstBuffer * inbuf)
{
  const GstMetaInfo *info = meta->info;
  const gchar *const *tags;
  const gchar *const supported_tags[] = {
    GST_META_TAG_AUDIO_STR,
    GST_META_TAG_AUDIO_CHANNELS_STR,
    NULL,
  };

  tags = gst_meta_api_type_get_tags (info->api);

  if (!tags)
    return TRUE;

  while (*tags) {
    if (!g_strv_contains (supported_tags, *tags))
      return FALSE;
    tags++;
  }

  return TRUE;
}

typedef struct
{
  GstAudioDecoder *decoder;
  GstBuffer *outbuf;
} CopyMetaData;

static gboolean
foreach_metadata (GstBuffer * inbuf, GstMeta ** meta, gpointer user_data)
{
  CopyMetaData *data = user_data;
  GstAudioDecoder *decoder = data->decoder;
  GstAudioDecoderClass *klass = GST_AUDIO_DECODER_GET_CLASS (decoder);
  GstBuffer *outbuf = data->outbuf;
  const GstMetaInfo *info = (*meta)->info;
  gboolean do_copy = FALSE;

  if (gst_meta_api_type_has_tag (info->api, _gst_meta_tag_memory)) {
    /* never call the transform_meta with memory specific metadata */
    GST_DEBUG_OBJECT (decoder, "not copying memory specific metadata %s",
        g_type_name (info->api));
    do_copy = FALSE;
  } else if (klass->transform_meta) {
    do_copy = klass->transform_meta (decoder, outbuf, *meta, inbuf);
    GST_DEBUG_OBJECT (decoder, "transformed metadata %s: copy: %d",
        g_type_name (info->api), do_copy);
  }

  /* we only copy metadata when the subclass implemented a transform_meta
   * function and when it returns %TRUE */
  if (do_copy && info->transform_func) {
    GstMetaTransformCopy copy_data = { FALSE, 0, -1 };
    GST_DEBUG_OBJECT (decoder, "copy metadata %s", g_type_name (info->api));
    /* simply copy then */
    info->transform_func (outbuf, *meta, inbuf,
        _gst_meta_transform_copy, &copy_data);
  }
  return TRUE;
}

/**
 * gst_audio_decoder_finish_subframe:
 * @dec: a #GstAudioDecoder
 * @buf: (transfer full) (allow-none): decoded data
 *
 * Collects decoded data and pushes it downstream. This function may be called
 * multiple times for a given input frame.
 *
 * @buf may be NULL in which case it is assumed that the current input frame is
 * finished. This is equivalent to calling gst_audio_decoder_finish_subframe()
 * with a NULL buffer and frames=1 after having pushed out all decoded audio
 * subframes using this function.
 *
 * When called with valid data in @buf the source pad caps must have been set
 * already.
 *
 * Note that a frame received in #GstAudioDecoderClass.handle_frame() may be
 * invalidated by a call to this function.
 *
 * Returns: a #GstFlowReturn that should be escalated to caller (of caller)
 *
 * Since: 1.16
 */
GstFlowReturn
gst_audio_decoder_finish_subframe (GstAudioDecoder * dec, GstBuffer * buf)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), GST_FLOW_ERROR);

  if (buf == NULL)
    return gst_audio_decoder_finish_frame_or_subframe (dec, NULL, 1);
  else
    return gst_audio_decoder_finish_frame_or_subframe (dec, buf, 0);
}

/**
 * gst_audio_decoder_finish_frame:
 * @dec: a #GstAudioDecoder
 * @buf: (transfer full) (allow-none): decoded data
 * @frames: number of decoded frames represented by decoded data
 *
 * Collects decoded data and pushes it downstream.
 *
 * @buf may be NULL in which case the indicated number of frames
 * are discarded and considered to have produced no output
 * (e.g. lead-in or setup frames).
 * Otherwise, source pad caps must be set when it is called with valid
 * data in @buf.
 *
 * Note that a frame received in #GstAudioDecoderClass.handle_frame() may be
 * invalidated by a call to this function.
 *
 * Returns: a #GstFlowReturn that should be escalated to caller (of caller)
 */
GstFlowReturn
gst_audio_decoder_finish_frame (GstAudioDecoder * dec, GstBuffer * buf,
    gint frames)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), GST_FLOW_ERROR);

  /* no dummy calls please */
  g_return_val_if_fail (frames != 0, GST_FLOW_ERROR);

  return gst_audio_decoder_finish_frame_or_subframe (dec, buf, frames);
}

/* frames == 0 indicates that this is a sub-frame and further sub-frames may
 * follow for the current input frame. */
static GstFlowReturn
gst_audio_decoder_finish_frame_or_subframe (GstAudioDecoder * dec,
    GstBuffer * buf, gint frames)
{
  GstAudioDecoderPrivate *priv;
  GstAudioDecoderContext *ctx;
  GstAudioDecoderClass *klass = GST_AUDIO_DECODER_GET_CLASS (dec);
  GstAudioMeta *meta;
  GstClockTime ts, next_ts;
  gsize size, samples = 0;
  GstFlowReturn ret = GST_FLOW_OK;
  GQueue inbufs = G_QUEUE_INIT;
  gboolean is_subframe = (frames == 0);
  gboolean do_check_resync;

  /* subclass should not hand us no data */
  g_return_val_if_fail (buf == NULL || gst_buffer_get_size (buf) > 0,
      GST_FLOW_ERROR);

  /* if it's a subframe (frames == 0) we must have a valid buffer */
  g_assert (!is_subframe || buf != NULL);

  priv = dec->priv;
  ctx = &dec->priv->ctx;
  meta = buf ? gst_buffer_get_audio_meta (buf) : NULL;
  size = buf ? gst_buffer_get_size (buf) : 0;
  samples = buf ? (meta ? meta->samples : size / ctx->info.bpf) : 0;

  /* must know the output format by now */
  g_return_val_if_fail (buf == NULL || GST_AUDIO_INFO_IS_VALID (&ctx->info),
      GST_FLOW_ERROR);

  GST_LOG_OBJECT (dec,
      "accepting %" G_GSIZE_FORMAT " bytes == %" G_GSIZE_FORMAT
      " samples for %d frames", buf ? size : 0, samples, frames);

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  if (buf != NULL && priv->subframe_samples == 0) {
    ret = check_pending_reconfigure (dec);
    if (ret == GST_FLOW_FLUSHING || ret == GST_FLOW_NOT_NEGOTIATED) {
      gst_buffer_unref (buf);
      goto exit;
    }

    if (priv->pending_events)
      send_pending_events (dec);
  }

  /* sanity checking */
  if (G_LIKELY (buf && ctx->info.bpf)) {
    if (!meta || meta->info.layout == GST_AUDIO_LAYOUT_INTERLEAVED) {
      /* output should be whole number of sample frames */
      if (size % ctx->info.bpf)
        goto wrong_buffer;
      /* output should have no additional padding */
      if (samples != size / ctx->info.bpf)
        goto wrong_samples;
    } else {
      /* can't have more samples than what the buffer fits */
      if (samples > size / ctx->info.bpf)
        goto wrong_samples;
    }
  }

  /* frame and ts book-keeping */
  if (G_UNLIKELY (frames < 0)) {
    if (G_UNLIKELY (-frames - 1 > priv->frames.length)) {
      GST_ELEMENT_WARNING (dec, STREAM, DECODE,
          ("received more decoded frames %d than provided %d", frames,
              priv->frames.length), (NULL));
      frames = 0;
    } else {
      frames = priv->frames.length + frames + 1;
    }
  } else if (G_UNLIKELY (frames > priv->frames.length)) {
    if (G_LIKELY (!priv->force)) {
      GST_ELEMENT_WARNING (dec, STREAM, DECODE,
          ("received more decoded frames %d than provided %d", frames,
              priv->frames.length), (NULL));
    }
    frames = priv->frames.length;
  }

  if (G_LIKELY (priv->frames.length))
    ts = GST_BUFFER_TIMESTAMP (priv->frames.head->data);
  else
    ts = GST_CLOCK_TIME_NONE;

  GST_DEBUG_OBJECT (dec, "leading frame ts %" GST_TIME_FORMAT,
      GST_TIME_ARGS (ts));

  if (is_subframe && priv->frames.length == 0)
    goto subframe_without_pending_input_frame;

  /* this will be skipped in the is_subframe case because frames will be 0 */
  while (priv->frames.length && frames) {
    g_queue_push_tail (&inbufs, g_queue_pop_head (&priv->frames));
    dec->priv->ctx.delay = dec->priv->frames.length;
    frames--;
  }

  if (G_UNLIKELY (!buf))
    goto exit;

  /* lock on */
  if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (priv->base_ts))) {
    priv->base_ts = ts;
    GST_DEBUG_OBJECT (dec, "base_ts now %" GST_TIME_FORMAT, GST_TIME_ARGS (ts));
  }

  /* still no valid ts, track the segment one */
  if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (priv->base_ts)) &&
      dec->output_segment.rate > 0.0) {
    priv->base_ts = dec->output_segment.start;
  }

  /* only check for resync at the beginning of an input/output frame */
  do_check_resync = !is_subframe || priv->subframe_samples == 0;

  /* slightly convoluted approach caters for perfect ts if subclass desires. */
  if (do_check_resync && GST_CLOCK_TIME_IS_VALID (ts)) {
    if (dec->priv->tolerance > 0) {
      GstClockTimeDiff diff;

      g_assert (GST_CLOCK_TIME_IS_VALID (priv->base_ts));
      next_ts = priv->base_ts +
          gst_util_uint64_scale (priv->samples, GST_SECOND, ctx->info.rate);
      GST_LOG_OBJECT (dec,
          "buffer is %" G_GUINT64_FORMAT " samples past base_ts %"
          GST_TIME_FORMAT ", expected ts %" GST_TIME_FORMAT, priv->samples,
          GST_TIME_ARGS (priv->base_ts), GST_TIME_ARGS (next_ts));
      diff = GST_CLOCK_DIFF (next_ts, ts);
      GST_LOG_OBJECT (dec, "ts diff %d ms", (gint) (diff / GST_MSECOND));
      /* if within tolerance,
       * discard buffer ts and carry on producing perfect stream,
       * otherwise resync to ts */
      if (G_UNLIKELY (diff < (gint64) - dec->priv->tolerance ||
              diff > (gint64) dec->priv->tolerance)) {
        GST_DEBUG_OBJECT (dec, "base_ts resync");
        priv->base_ts = ts;
        priv->samples = 0;
      }
    } else {
      GST_DEBUG_OBJECT (dec, "base_ts resync");
      priv->base_ts = ts;
      priv->samples = 0;
    }
  }

  /* delayed one-shot stuff until confirmed data */
  if (priv->taglist && priv->taglist_changed) {
    GstEvent *tags_event;

    tags_event = gst_audio_decoder_create_merged_tags_event (dec);

    if (tags_event != NULL)
      gst_audio_decoder_push_event (dec, tags_event);

    priv->taglist_changed = FALSE;
  }

  buf = gst_buffer_make_writable (buf);
  if (G_LIKELY (GST_CLOCK_TIME_IS_VALID (priv->base_ts))) {
    GST_BUFFER_TIMESTAMP (buf) =
        priv->base_ts +
        GST_FRAMES_TO_CLOCK_TIME (priv->samples, ctx->info.rate);
    GST_BUFFER_DURATION (buf) = priv->base_ts +
        GST_FRAMES_TO_CLOCK_TIME (priv->samples + samples, ctx->info.rate) -
        GST_BUFFER_TIMESTAMP (buf);
  } else {
    GST_BUFFER_TIMESTAMP (buf) = GST_CLOCK_TIME_NONE;
    GST_BUFFER_DURATION (buf) =
        GST_FRAMES_TO_CLOCK_TIME (samples, ctx->info.rate);
  }

  if (klass->transform_meta) {
    if (inbufs.length) {
      GList *l;
      for (l = inbufs.head; l; l = l->next) {
        CopyMetaData data;

        data.decoder = dec;
        data.outbuf = buf;
        gst_buffer_foreach_meta (l->data, foreach_metadata, &data);
      }
    } else if (is_subframe) {
      CopyMetaData data;
      GstBuffer *in_buf;

      /* For subframes we assume a 1:N relationship for now, so we just take
       * metas from the first pending input buf */
      in_buf = g_queue_peek_head (&priv->frames);
      data.decoder = dec;
      data.outbuf = buf;
      gst_buffer_foreach_meta (in_buf, foreach_metadata, &data);
    } else {
      GST_WARNING_OBJECT (dec,
          "Can't copy metadata because input buffers disappeared");
    }
  }

  GST_OBJECT_LOCK (dec);
  priv->samples += samples;
  priv->samples_out += samples;
  GST_OBJECT_UNLOCK (dec);

  /* we got data, so note things are looking up */
  if (G_UNLIKELY (dec->priv->error_count))
    dec->priv->error_count = 0;

  ret = gst_audio_decoder_output (dec, buf);

exit:
  g_queue_foreach (&inbufs, (GFunc) gst_buffer_unref, NULL);
  g_queue_clear (&inbufs);

  if (is_subframe)
    dec->priv->subframe_samples += samples;
  else
    dec->priv->subframe_samples = 0;

  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return ret;

  /* ERRORS */
wrong_buffer:
  {
    /* arguably more of a programming error? */
    GST_ELEMENT_ERROR (dec, STREAM, DECODE, (NULL),
        ("buffer size %" G_GSIZE_FORMAT " not a multiple of %d", size,
            ctx->info.bpf));
    gst_buffer_unref (buf);
    ret = GST_FLOW_ERROR;
    goto exit;
  }
wrong_samples:
  {
    /* arguably more of a programming error? */
    GST_ELEMENT_ERROR (dec, STREAM, DECODE, (NULL),
        ("GstAudioMeta samples (%" G_GSIZE_FORMAT ") are inconsistent with "
            "the buffer size and layout (size/bpf = %" G_GSIZE_FORMAT ")",
            meta->samples, size / ctx->info.bpf));
    gst_buffer_unref (buf);
    ret = GST_FLOW_ERROR;
    goto exit;
  }
subframe_without_pending_input_frame:
  {
    /* arguably more of a programming error? */
    GST_ELEMENT_ERROR (dec, STREAM, DECODE, (NULL),
        ("Received decoded subframe, but no pending frame"));
    gst_buffer_unref (buf);
    ret = GST_FLOW_ERROR;
    goto exit;
  }
}

static GstFlowReturn
gst_audio_decoder_handle_frame (GstAudioDecoder * dec,
    GstAudioDecoderClass * klass, GstBuffer * buffer)
{
  /* Skip decoding and send a GAP instead if
   * GST_SEGMENT_FLAG_TRICKMODE_NO_AUDIO is set and we have timestamps
   * FIXME: We only do this for forward playback atm, because reverse
   * playback would require accumulating GAP events and pushing them
   * out in reverse order as for normal audio samples
   */
  if (G_UNLIKELY (dec->input_segment.rate > 0.0
          && dec->input_segment.flags & GST_SEGMENT_FLAG_TRICKMODE_NO_AUDIO)) {
    if (buffer) {
      GstClockTime ts = GST_BUFFER_PTS (buffer);
      if (GST_CLOCK_TIME_IS_VALID (ts)) {
        GstEvent *event = gst_event_new_gap (ts, GST_BUFFER_DURATION (buffer));

        gst_buffer_unref (buffer);
        GST_LOG_OBJECT (dec, "Skipping decode in trickmode and sending gap");
        gst_audio_decoder_handle_gap (dec, event);
        return GST_FLOW_OK;
      }
    }
  }

  if (G_LIKELY (buffer)) {
    gsize size = gst_buffer_get_size (buffer);
    /* keep around for admin */
    GST_LOG_OBJECT (dec,
        "tracking frame size %" G_GSIZE_FORMAT ", ts %" GST_TIME_FORMAT, size,
        GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buffer)));
    g_queue_push_tail (&dec->priv->frames, buffer);
    dec->priv->ctx.delay = dec->priv->frames.length;
    GST_OBJECT_LOCK (dec);
    dec->priv->bytes_in += size;
    GST_OBJECT_UNLOCK (dec);
  } else {
    GST_LOG_OBJECT (dec, "providing subclass with NULL frame");
  }

  return klass->handle_frame (dec, buffer);
}

/* maybe subclass configurable instead, but this allows for a whole lot of
 * raw samples, so at least quite some encoded ... */
#define GST_AUDIO_DECODER_MAX_SYNC     10 * 8 * 2 * 1024

static GstFlowReturn
gst_audio_decoder_push_buffers (GstAudioDecoder * dec, gboolean force)
{
  GstAudioDecoderClass *klass;
  GstAudioDecoderPrivate *priv;
  GstAudioDecoderContext *ctx;
  GstFlowReturn ret = GST_FLOW_OK;
  GstBuffer *buffer;
  gint av, flush;

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);
  priv = dec->priv;
  ctx = &dec->priv->ctx;

  g_return_val_if_fail (klass->handle_frame != NULL, GST_FLOW_ERROR);

  av = gst_adapter_available (priv->adapter);
  GST_DEBUG_OBJECT (dec, "available: %d", av);

  while (ret == GST_FLOW_OK) {

    flush = 0;
    ctx->eos = force;

    if (G_LIKELY (av)) {
      gint len;
      GstClockTime ts;
      guint64 distance;

      /* parse if needed */
      if (klass->parse) {
        gint offset = 0;

        /* limited (legacy) parsing; avoid whole of baseparse */
        GST_DEBUG_OBJECT (dec, "parsing available: %d", av);
        /* piggyback sync state on discont */
        ctx->sync = !priv->discont;
        ret = klass->parse (dec, priv->adapter, &offset, &len);

        g_assert (offset <= av);
        if (offset) {
          /* jumped a bit */
          GST_DEBUG_OBJECT (dec, "skipped %d; setting DISCONT", offset);
          gst_adapter_flush (priv->adapter, offset);
          flush = offset;
          /* avoid parsing indefinitely */
          priv->sync_flush += offset;
          if (priv->sync_flush > GST_AUDIO_DECODER_MAX_SYNC)
            goto parse_failed;
        }

        if (ret == GST_FLOW_EOS) {
          GST_LOG_OBJECT (dec, "no frame yet");
          ret = GST_FLOW_OK;
          break;
        } else if (ret == GST_FLOW_OK) {
          GST_LOG_OBJECT (dec, "frame at offset %d of length %d", offset, len);
          g_assert (len);
          g_assert (offset + len <= av);
          priv->sync_flush = 0;
        } else {
          break;
        }
      } else {
        len = av;
      }
      /* track upstream ts, but do not get stuck if nothing new upstream */
      ts = gst_adapter_prev_pts (priv->adapter, &distance);
      if (ts != priv->prev_ts || distance <= priv->prev_distance) {
        priv->prev_ts = ts;
        priv->prev_distance = distance;
      } else {
        GST_LOG_OBJECT (dec, "ts == prev_ts; discarding");
        ts = GST_CLOCK_TIME_NONE;
      }
      buffer = gst_adapter_take_buffer (priv->adapter, len);
      buffer = gst_buffer_make_writable (buffer);
      GST_BUFFER_TIMESTAMP (buffer) = ts;
      flush += len;
      priv->force = FALSE;
    } else {
      if (!force)
        break;
      if (!priv->drainable) {
        priv->drained = TRUE;
        break;
      }
      buffer = NULL;
      priv->force = TRUE;
    }

    ret = gst_audio_decoder_handle_frame (dec, klass, buffer);

    /* do not keep pushing it ... */
    if (G_UNLIKELY (!av)) {
      priv->drained = TRUE;
      break;
    }

    av -= flush;
    g_assert (av >= 0);
  }

  GST_LOG_OBJECT (dec, "done pushing to subclass");
  return ret;

  /* ERRORS */
parse_failed:
  {
    GST_ELEMENT_ERROR (dec, STREAM, DECODE, (NULL), ("failed to parse stream"));
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_audio_decoder_drain (GstAudioDecoder * dec)
{
  GstFlowReturn ret;

  if (dec->priv->drained && !dec->priv->gather)
    return GST_FLOW_OK;

  /* Apply any pending events before draining, as that
   * may update the pending segment info */
  apply_pending_events (dec);

  /* dispatch reverse pending buffers */
  /* chain eventually calls upon drain as well, but by that time
   * gather list should be clear, so ok ... */
  if (dec->output_segment.rate < 0.0 && dec->priv->gather)
    gst_audio_decoder_chain_reverse (dec, NULL);
  /* have subclass give all it can */
  ret = gst_audio_decoder_push_buffers (dec, TRUE);
  if (ret != GST_FLOW_OK) {
    GST_WARNING_OBJECT (dec, "audio decoder push buffers failed");
    goto drain_failed;
  }
  /* ensure all output sent */
  ret = gst_audio_decoder_output (dec, NULL);
  if (ret != GST_FLOW_OK)
    GST_WARNING_OBJECT (dec, "audio decoder output failed");

drain_failed:
  /* everything should be away now */
  if (dec->priv->frames.length) {
    /* not fatal/impossible though if subclass/codec eats stuff */
    GST_WARNING_OBJECT (dec, "still %d frames left after draining",
        dec->priv->frames.length);
    g_queue_foreach (&dec->priv->frames, (GFunc) gst_buffer_unref, NULL);
    g_queue_clear (&dec->priv->frames);
  }

  /* discard (unparsed) leftover */
  gst_adapter_clear (dec->priv->adapter);
  return ret;
}

/* hard == FLUSH, otherwise discont */
static GstFlowReturn
gst_audio_decoder_flush (GstAudioDecoder * dec, gboolean hard)
{
  GstAudioDecoderClass *klass;
  GstFlowReturn ret = GST_FLOW_OK;

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_LOG_OBJECT (dec, "flush hard %d", hard);

  if (!hard) {
    ret = gst_audio_decoder_drain (dec);
  } else {
    gst_audio_decoder_clear_queues (dec);
    gst_segment_init (&dec->input_segment, GST_FORMAT_TIME);
    gst_segment_init (&dec->output_segment, GST_FORMAT_TIME);
    dec->priv->error_count = 0;
  }
  /* only bother subclass with flushing if known it is already alive
   * and kicking out stuff */
  if (klass->flush && dec->priv->samples_out > 0)
    klass->flush (dec, hard);
  /* and get (re)set for the sequel */
  gst_audio_decoder_reset (dec, FALSE);

  return ret;
}

static GstFlowReturn
gst_audio_decoder_chain_forward (GstAudioDecoder * dec, GstBuffer * buffer)
{
  GstFlowReturn ret = GST_FLOW_OK;

  /* discard silly case, though maybe ts may be of value ?? */
  if (G_UNLIKELY (gst_buffer_get_size (buffer) == 0)) {
    GST_DEBUG_OBJECT (dec, "discarding empty buffer");
    gst_buffer_unref (buffer);
    goto exit;
  }

  /* grab buffer */
  gst_adapter_push (dec->priv->adapter, buffer);
  buffer = NULL;
  /* new stuff, so we can push subclass again */
  dec->priv->drained = FALSE;

  /* hand to subclass */
  ret = gst_audio_decoder_push_buffers (dec, FALSE);

exit:
  GST_LOG_OBJECT (dec, "chain-done");
  return ret;
}

static void
gst_audio_decoder_clear_queues (GstAudioDecoder * dec)
{
  GstAudioDecoderPrivate *priv = dec->priv;

  g_list_foreach (priv->queued, (GFunc) gst_mini_object_unref, NULL);
  g_list_free (priv->queued);
  priv->queued = NULL;
  g_list_foreach (priv->gather, (GFunc) gst_mini_object_unref, NULL);
  g_list_free (priv->gather);
  priv->gather = NULL;
  g_list_foreach (priv->decode, (GFunc) gst_mini_object_unref, NULL);
  g_list_free (priv->decode);
  priv->decode = NULL;
}

/*
 * Input:
 *  Buffer decoding order:  7  8  9  4  5  6  3  1  2  EOS
 *  Discont flag:           D        D        D  D
 *
 * - Each Discont marks a discont in the decoding order.
 *
 * for vorbis, each buffer is a keyframe when we have the previous
 * buffer. This means that to decode buffer 7, we need buffer 6, which
 * arrives out of order.
 *
 * we first gather buffers in the gather queue until we get a DISCONT. We
 * prepend each incoming buffer so that they are in reversed order.
 *
 *    gather queue:    9  8  7
 *    decode queue:
 *    output queue:
 *
 * When a DISCONT is received (buffer 4), we move the gather queue to the
 * decode queue. This is simply done be taking the head of the gather queue
 * and prepending it to the decode queue. This yields:
 *
 *    gather queue:
 *    decode queue:    7  8  9
 *    output queue:
 *
 * Then we decode each buffer in the decode queue in order and put the output
 * buffer in the output queue. The first buffer (7) will not produce any output
 * because it needs the previous buffer (6) which did not arrive yet. This
 * yields:
 *
 *    gather queue:
 *    decode queue:    7  8  9
 *    output queue:    9  8
 *
 * Then we remove the consumed buffers from the decode queue. Buffer 7 is not
 * completely consumed, we need to keep it around for when we receive buffer
 * 6. This yields:
 *
 *    gather queue:
 *    decode queue:    7
 *    output queue:    9  8
 *
 * Then we accumulate more buffers:
 *
 *    gather queue:    6  5  4
 *    decode queue:    7
 *    output queue:
 *
 * prepending to the decode queue on DISCONT yields:
 *
 *    gather queue:
 *    decode queue:    4  5  6  7
 *    output queue:
 *
 * after decoding and keeping buffer 4:
 *
 *    gather queue:
 *    decode queue:    4
 *    output queue:    7  6  5
 *
 * Etc..
 */
static GstFlowReturn
gst_audio_decoder_flush_decode (GstAudioDecoder * dec)
{
  GstAudioDecoderPrivate *priv = dec->priv;
  GstFlowReturn res = GST_FLOW_OK;
  GstClockTime timestamp;
  GList *walk;

  walk = priv->decode;

  GST_DEBUG_OBJECT (dec, "flushing buffers to decoder");

  /* clear buffer and decoder state */
  gst_audio_decoder_flush (dec, FALSE);

  while (walk) {
    GList *next;
    GstBuffer *buf = GST_BUFFER_CAST (walk->data);

    GST_DEBUG_OBJECT (dec, "decoding buffer %p, ts %" GST_TIME_FORMAT,
        buf, GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)));

    next = g_list_next (walk);
    /* decode buffer, resulting data prepended to output queue */
    gst_buffer_ref (buf);
    res = gst_audio_decoder_chain_forward (dec, buf);

    /* if we generated output, we can discard the buffer, else we
     * keep it in the queue */
    if (priv->queued) {
      GST_DEBUG_OBJECT (dec, "decoded buffer to %p", priv->queued->data);
      priv->decode = g_list_delete_link (priv->decode, walk);
      gst_buffer_unref (buf);
    } else {
      GST_DEBUG_OBJECT (dec, "buffer did not decode, keeping");
    }
    walk = next;
  }

  /* drain any aggregation (or otherwise) leftover */
  gst_audio_decoder_drain (dec);

  /* now send queued data downstream */
  timestamp = GST_CLOCK_TIME_NONE;
  while (priv->queued) {
    GstBuffer *buf = GST_BUFFER_CAST (priv->queued->data);
    GstClockTime duration;

    duration = GST_BUFFER_DURATION (buf);

    /* duration should always be valid for raw audio */
    g_assert (GST_CLOCK_TIME_IS_VALID (duration));

    /* interpolate (backward) if needed */
    if (G_LIKELY (timestamp != -1)) {
      if (timestamp > duration)
        timestamp -= duration;
      else
        timestamp = 0;
    }

    if (!GST_BUFFER_TIMESTAMP_IS_VALID (buf)) {
      GST_LOG_OBJECT (dec, "applying reverse interpolated ts %"
          GST_TIME_FORMAT, GST_TIME_ARGS (timestamp));
      GST_BUFFER_TIMESTAMP (buf) = timestamp;
    } else {
      /* track otherwise */
      timestamp = GST_BUFFER_TIMESTAMP (buf);
      GST_LOG_OBJECT (dec, "tracking ts %" GST_TIME_FORMAT,
          GST_TIME_ARGS (timestamp));
    }

    if (G_LIKELY (res == GST_FLOW_OK)) {
      GST_DEBUG_OBJECT (dec, "pushing buffer %p of size %" G_GSIZE_FORMAT ", "
          "time %" GST_TIME_FORMAT ", dur %" GST_TIME_FORMAT, buf,
          gst_buffer_get_size (buf), GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
          GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));
      /* should be already, but let's be sure */
      buf = gst_buffer_make_writable (buf);
      /* avoid stray DISCONT from forward processing,
       * which have no meaning in reverse pushing */
      GST_BUFFER_FLAG_UNSET (buf, GST_BUFFER_FLAG_DISCONT);
      res = gst_audio_decoder_push_forward (dec, buf);
    } else {
      gst_buffer_unref (buf);
    }

    priv->queued = g_list_delete_link (priv->queued, priv->queued);
  }

  return res;
}

static GstFlowReturn
gst_audio_decoder_chain_reverse (GstAudioDecoder * dec, GstBuffer * buf)
{
  GstAudioDecoderPrivate *priv = dec->priv;
  GstFlowReturn result = GST_FLOW_OK;

  /* if we have a discont, move buffers to the decode list */
  if (!buf || GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DISCONT)) {
    GST_DEBUG_OBJECT (dec, "received discont");
    while (priv->gather) {
      GstBuffer *gbuf;

      gbuf = GST_BUFFER_CAST (priv->gather->data);
      /* remove from the gather list */
      priv->gather = g_list_delete_link (priv->gather, priv->gather);
      /* copy to decode queue */
      priv->decode = g_list_prepend (priv->decode, gbuf);
    }
    /* decode stuff in the decode queue */
    gst_audio_decoder_flush_decode (dec);
  }

  if (G_LIKELY (buf)) {
    GST_DEBUG_OBJECT (dec, "gathering buffer %p of size %" G_GSIZE_FORMAT ", "
        "time %" GST_TIME_FORMAT ", dur %" GST_TIME_FORMAT, buf,
        gst_buffer_get_size (buf), GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)),
        GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));

    /* add buffer to gather queue */
    priv->gather = g_list_prepend (priv->gather, buf);
  }

  return result;
}

static GstFlowReturn
gst_audio_decoder_chain (GstPad * pad, GstObject * parent, GstBuffer * buffer)
{
  GstAudioDecoder *dec;
  GstFlowReturn ret;

  dec = GST_AUDIO_DECODER (parent);

  GST_LOG_OBJECT (dec,
      "received buffer of size %" G_GSIZE_FORMAT " with ts %" GST_TIME_FORMAT
      ", duration %" GST_TIME_FORMAT, gst_buffer_get_size (buffer),
      GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buffer)),
      GST_TIME_ARGS (GST_BUFFER_DURATION (buffer)));

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  if (G_UNLIKELY (dec->priv->ctx.input_caps == NULL && dec->priv->needs_format))
    goto not_negotiated;

  dec->priv->ctx.had_input_data = TRUE;

  if (!dec->priv->expecting_discont_buf &&
      GST_BUFFER_FLAG_IS_SET (buffer, GST_BUFFER_FLAG_DISCONT)) {
    gint64 samples, ts;

    /* track present position */
    ts = dec->priv->base_ts;
    samples = dec->priv->samples;

    GST_DEBUG_OBJECT (dec, "handling discont");
    gst_audio_decoder_flush (dec, FALSE);
    dec->priv->discont = TRUE;

    /* buffer may claim DISCONT loudly, if it can't tell us where we are now,
     * we'll stick to where we were ...
     * Particularly useful/needed for upstream BYTE based */
    if (dec->input_segment.rate > 0.0
        && !GST_BUFFER_TIMESTAMP_IS_VALID (buffer)) {
      GST_DEBUG_OBJECT (dec, "... but restoring previous ts tracking");
      dec->priv->base_ts = ts;
      dec->priv->samples = samples;
    }
  }
  dec->priv->expecting_discont_buf = FALSE;

  if (dec->input_segment.rate > 0.0)
    ret = gst_audio_decoder_chain_forward (dec, buffer);
  else
    ret = gst_audio_decoder_chain_reverse (dec, buffer);

  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return ret;

  /* ERRORS */
not_negotiated:
  {
    GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
    GST_ELEMENT_ERROR (dec, CORE, NEGOTIATION, (NULL),
        ("decoder not initialized"));
    gst_buffer_unref (buffer);
    return GST_FLOW_NOT_NEGOTIATED;
  }
}

/* perform upstream byte <-> time conversion (duration, seeking)
 * if subclass allows and if enough data for moderately decent conversion */
static inline gboolean
gst_audio_decoder_do_byte (GstAudioDecoder * dec)
{
  gboolean ret;

  GST_OBJECT_LOCK (dec);
  ret = dec->priv->ctx.do_estimate_rate && dec->priv->ctx.info.bpf &&
      dec->priv->ctx.info.rate <= dec->priv->samples_out;
  GST_OBJECT_UNLOCK (dec);

  return ret;
}

/* Must be called holding the GST_AUDIO_DECODER_STREAM_LOCK */
static gboolean
gst_audio_decoder_negotiate_default_caps (GstAudioDecoder * dec)
{
  GstCaps *caps, *templcaps;
  gint i;
  gint channels = 0;
  gint rate;
  guint64 channel_mask = 0;
  gint caps_size;
  GstStructure *structure;
  GstAudioInfo info;

  templcaps = gst_pad_get_pad_template_caps (dec->srcpad);
  caps = gst_pad_peer_query_caps (dec->srcpad, templcaps);
  if (caps)
    gst_caps_unref (templcaps);
  else
    caps = templcaps;
  templcaps = NULL;

  if (!caps || gst_caps_is_empty (caps) || gst_caps_is_any (caps))
    goto caps_error;

  GST_LOG_OBJECT (dec, "peer caps  %" GST_PTR_FORMAT, caps);

  /* before fixating, try to use whatever upstream provided */
  caps = gst_caps_make_writable (caps);
  caps_size = gst_caps_get_size (caps);
  if (dec->priv->ctx.input_caps) {
    GstCaps *sinkcaps = dec->priv->ctx.input_caps;
    GstStructure *structure = gst_caps_get_structure (sinkcaps, 0);

    if (gst_structure_get_int (structure, "rate", &rate)) {
      for (i = 0; i < caps_size; i++) {
        gst_structure_set (gst_caps_get_structure (caps, i), "rate",
            G_TYPE_INT, rate, NULL);
      }
    }

    if (gst_structure_get_int (structure, "channels", &channels)) {
      for (i = 0; i < caps_size; i++) {
        gst_structure_set (gst_caps_get_structure (caps, i), "channels",
            G_TYPE_INT, channels, NULL);
      }
    }

    if (gst_structure_get (structure, "channel-mask", GST_TYPE_BITMASK,
            &channel_mask, NULL)) {
      for (i = 0; i < caps_size; i++) {
        gst_structure_set (gst_caps_get_structure (caps, i), "channel-mask",
            GST_TYPE_BITMASK, channel_mask, NULL);
      }
    }
  }

  for (i = 0; i < caps_size; i++) {
    structure = gst_caps_get_structure (caps, i);
    if (gst_structure_has_field (structure, "channels"))
      gst_structure_fixate_field_nearest_int (structure,
          "channels", GST_AUDIO_DEF_CHANNELS);
    else
      gst_structure_set (structure, "channels", G_TYPE_INT,
          GST_AUDIO_DEF_CHANNELS, NULL);
    if (gst_structure_has_field (structure, "rate"))
      gst_structure_fixate_field_nearest_int (structure,
          "rate", GST_AUDIO_DEF_RATE);
    else
      gst_structure_set (structure, "rate", G_TYPE_INT, GST_AUDIO_DEF_RATE,
          NULL);
  }
  caps = gst_caps_fixate (caps);
  structure = gst_caps_get_structure (caps, 0);

  /* Need to add a channel-mask if channels > 2 */
  gst_structure_get_int (structure, "channels", &channels);
  if (channels > 2 && !gst_structure_has_field (structure, "channel-mask")) {
    channel_mask = gst_audio_channel_get_fallback_mask (channels);
    if (channel_mask != 0) {
      gst_structure_set (structure, "channel-mask",
          GST_TYPE_BITMASK, channel_mask, NULL);
    } else {
      GST_WARNING_OBJECT (dec, "No default channel-mask for %d channels",
          channels);
    }
  }

  if (!caps || !gst_audio_info_from_caps (&info, caps))
    goto caps_error;

  GST_OBJECT_LOCK (dec);
  dec->priv->ctx.info = info;
  dec->priv->ctx.caps = caps;
  GST_OBJECT_UNLOCK (dec);

  GST_INFO_OBJECT (dec,
      "Chose default caps %" GST_PTR_FORMAT " for initial gap", caps);

  return TRUE;

caps_error:
  {
    if (caps)
      gst_caps_unref (caps);
    return FALSE;
  }
}

static gboolean
gst_audio_decoder_handle_gap (GstAudioDecoder * dec, GstEvent * event)
{
  gboolean ret;
  GstClockTime timestamp, duration;
  gboolean needs_reconfigure = FALSE;

  /* Ensure we have caps first */
  GST_AUDIO_DECODER_STREAM_LOCK (dec);
  if (!GST_AUDIO_INFO_IS_VALID (&dec->priv->ctx.info)) {
    if (!gst_audio_decoder_negotiate_default_caps (dec)) {
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
      GST_ELEMENT_ERROR (dec, STREAM, FORMAT, (NULL),
          ("Decoder output not negotiated before GAP event."));
      gst_event_unref (event);
      return FALSE;
    }
    needs_reconfigure = TRUE;
  }
  needs_reconfigure = gst_pad_check_reconfigure (dec->srcpad)
      || needs_reconfigure;
  if (G_UNLIKELY (dec->priv->ctx.output_format_changed || needs_reconfigure)) {
    if (!gst_audio_decoder_negotiate_unlocked (dec)) {
      GST_WARNING_OBJECT (dec, "Failed to negotiate with downstream");
      gst_pad_mark_reconfigure (dec->srcpad);
    }
  }
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  gst_event_parse_gap (event, &timestamp, &duration);

  /* time progressed without data, see if we can fill the gap with
   * some concealment data */
  GST_DEBUG_OBJECT (dec,
      "gap event: plc %d, do_plc %d, position %" GST_TIME_FORMAT
      " duration %" GST_TIME_FORMAT,
      dec->priv->plc, dec->priv->ctx.do_plc,
      GST_TIME_ARGS (timestamp), GST_TIME_ARGS (duration));

  if (dec->priv->plc && dec->priv->ctx.do_plc && dec->input_segment.rate > 0.0) {
    GstAudioDecoderClass *klass = GST_AUDIO_DECODER_GET_CLASS (dec);
    GstBuffer *buf;

    /* hand subclass empty frame with duration that needs covering */
    buf = gst_buffer_new ();
    GST_BUFFER_TIMESTAMP (buf) = timestamp;
    GST_BUFFER_DURATION (buf) = duration;
    /* best effort, not much error handling */
    gst_audio_decoder_handle_frame (dec, klass, buf);
    ret = TRUE;
    dec->priv->expecting_discont_buf = TRUE;
    gst_event_unref (event);
  } else {
    GstFlowReturn flowret;

    /* sub-class doesn't know how to handle empty buffers,
     * so just try sending GAP downstream */
    flowret = check_pending_reconfigure (dec);
    if (flowret == GST_FLOW_OK) {
      send_pending_events (dec);
      ret = gst_audio_decoder_push_event (dec, event);
    } else {
      ret = FALSE;
      gst_event_unref (event);
    }
  }
  return ret;
}

static GList *
_flush_events (GstPad * pad, GList * events)
{
  GList *tmp;

  for (tmp = events; tmp; tmp = tmp->next) {
    if (GST_EVENT_TYPE (tmp->data) != GST_EVENT_EOS &&
        GST_EVENT_TYPE (tmp->data) != GST_EVENT_SEGMENT &&
        GST_EVENT_IS_STICKY (tmp->data)) {
      gst_pad_store_sticky_event (pad, GST_EVENT_CAST (tmp->data));
    }
    gst_event_unref (tmp->data);
  }
  g_list_free (events);

  return NULL;
}

static gboolean
gst_audio_decoder_sink_eventfunc (GstAudioDecoder * dec, GstEvent * event)
{
  gboolean ret;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_STREAM_START:
      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      /* finish any data in current segment and clear the decoder
       * to be ready for new stream data */
      gst_audio_decoder_drain (dec);
      gst_audio_decoder_flush (dec, FALSE);

      GST_DEBUG_OBJECT (dec, "received STREAM_START. Clearing taglist");
      /* Flush upstream tags after a STREAM_START */
      if (dec->priv->upstream_tags) {
        gst_tag_list_unref (dec->priv->upstream_tags);
        dec->priv->upstream_tags = NULL;
        dec->priv->taglist_changed = TRUE;
      }
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

      ret = gst_audio_decoder_push_event (dec, event);
      break;
    case GST_EVENT_SEGMENT:
    {
      GstSegment seg;
      GstFormat format;

      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      gst_event_copy_segment (event, &seg);

      format = seg.format;
      if (format == GST_FORMAT_TIME) {
        GST_DEBUG_OBJECT (dec, "received TIME SEGMENT %" GST_SEGMENT_FORMAT,
            &seg);
      } else {
        gint64 nstart;
        GST_DEBUG_OBJECT (dec, "received SEGMENT %" GST_SEGMENT_FORMAT, &seg);
        /* handle newsegment resulting from legacy simple seeking */
        /* note that we need to convert this whether or not enough data
         * to handle initial newsegment */
        if (dec->priv->ctx.do_estimate_rate &&
            gst_pad_query_convert (dec->sinkpad, GST_FORMAT_BYTES, seg.start,
                GST_FORMAT_TIME, &nstart)) {
          /* best attempt convert */
          /* as these are only estimates, stop is kept open-ended to avoid
           * premature cutting */
          GST_DEBUG_OBJECT (dec, "converted to TIME start %" GST_TIME_FORMAT,
              GST_TIME_ARGS (nstart));
          seg.format = GST_FORMAT_TIME;
          seg.start = nstart;
          seg.time = nstart;
          seg.stop = GST_CLOCK_TIME_NONE;
          /* replace event */
          gst_event_unref (event);
          event = gst_event_new_segment (&seg);
        } else {
          GST_DEBUG_OBJECT (dec, "unsupported format; ignoring");
          GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
          gst_event_unref (event);
          ret = FALSE;
          break;
        }
      }

      /* prepare for next segment */
      /* Use the segment start as a base timestamp
       * in case upstream does not come up with anything better
       * (e.g. upstream BYTE) */
      if (format != GST_FORMAT_TIME) {
        dec->priv->base_ts = seg.start;
        dec->priv->samples = 0;
      }

      /* Update the decode flags in the segment if we have an instant-rate
       * override active */
      GST_OBJECT_LOCK (dec);
      if (dec->priv->decode_flags_override) {
        seg.flags &= ~GST_SEGMENT_INSTANT_FLAGS;
        seg.flags |= dec->priv->decode_flags & GST_SEGMENT_INSTANT_FLAGS;
      }

      /* and follow along with segment */
      dec->priv->in_out_segment_sync = FALSE;
      dec->input_segment = seg;
      GST_OBJECT_UNLOCK (dec);

      dec->priv->pending_events =
          g_list_append (dec->priv->pending_events, event);
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

      ret = TRUE;
      break;
    }
    case GST_EVENT_INSTANT_RATE_CHANGE:
    {
      GstSegmentFlags flags;
      GstSegment *seg;

      gst_event_parse_instant_rate_change (event, NULL, &flags);

      GST_OBJECT_LOCK (dec);
      dec->priv->decode_flags_override = TRUE;
      dec->priv->decode_flags = flags;

      /* Update the input segment flags */
      seg = &dec->input_segment;
      seg->flags &= ~GST_SEGMENT_INSTANT_FLAGS;
      seg->flags |= dec->priv->decode_flags & GST_SEGMENT_INSTANT_FLAGS;
      GST_OBJECT_UNLOCK (dec);

      /* Forward downstream */
      ret = gst_pad_event_default (dec->sinkpad, GST_OBJECT_CAST (dec), event);
      break;
    }
    case GST_EVENT_GAP:
      ret = gst_audio_decoder_handle_gap (dec, event);
      break;
    case GST_EVENT_FLUSH_STOP:
      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      /* prepare for fresh start */
      gst_audio_decoder_flush (dec, TRUE);

      dec->priv->pending_events = _flush_events (dec->srcpad,
          dec->priv->pending_events);
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

      /* Forward FLUSH_STOP, it is expected to be forwarded immediately
       * and no buffers are queued anyway. */
      ret = gst_audio_decoder_push_event (dec, event);
      break;

    case GST_EVENT_SEGMENT_DONE:
      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      gst_audio_decoder_drain (dec);
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

      /* Forward SEGMENT_DONE because no buffer or serialized event might come after
       * SEGMENT_DONE and nothing could trigger another _finish_frame() call. */
      if (dec->priv->pending_events)
        send_pending_events (dec);
      ret = gst_audio_decoder_push_event (dec, event);
      break;

    case GST_EVENT_EOS:
      GST_AUDIO_DECODER_STREAM_LOCK (dec);
      gst_audio_decoder_drain (dec);
      GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

      if (dec->priv->ctx.had_input_data && !dec->priv->ctx.had_output_data) {
        GST_ELEMENT_ERROR (dec, STREAM, DECODE,
            ("No valid frames decoded before end of stream"),
            ("no valid frames found"));
      }

      /* Forward EOS because no buffer or serialized event will come after
       * EOS and nothing could trigger another _finish_frame() call. */
      if (dec->priv->pending_events)
        send_pending_events (dec);
      ret = gst_audio_decoder_push_event (dec, event);
      break;

    case GST_EVENT_CAPS:
    {
      GstCaps *caps;

      gst_event_parse_caps (event, &caps);
      ret = gst_audio_decoder_sink_setcaps (dec, caps);
      gst_event_unref (event);
      break;
    }
    case GST_EVENT_TAG:
    {
      GstTagList *tags;

      gst_event_parse_tag (event, &tags);

      if (gst_tag_list_get_scope (tags) == GST_TAG_SCOPE_STREAM) {
        GST_AUDIO_DECODER_STREAM_LOCK (dec);
        if (dec->priv->upstream_tags != tags) {
          if (dec->priv->upstream_tags)
            gst_tag_list_unref (dec->priv->upstream_tags);
          dec->priv->upstream_tags = gst_tag_list_ref (tags);
          GST_INFO_OBJECT (dec, "upstream stream tags: %" GST_PTR_FORMAT, tags);
        }
        gst_event_unref (event);
        event = gst_audio_decoder_create_merged_tags_event (dec);
        dec->priv->taglist_changed = FALSE;
        GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

        /* No tags, go out of here instead of fall through */
        if (!event) {
          ret = TRUE;
          break;
        }
      }

      /* fall through */
    }
    default:
      if (!GST_EVENT_IS_SERIALIZED (event)) {
        ret =
            gst_pad_event_default (dec->sinkpad, GST_OBJECT_CAST (dec), event);
      } else {
        GST_DEBUG_OBJECT (dec, "Enqueuing event %d, %s", GST_EVENT_TYPE (event),
            GST_EVENT_TYPE_NAME (event));
        GST_AUDIO_DECODER_STREAM_LOCK (dec);
        dec->priv->pending_events =
            g_list_append (dec->priv->pending_events, event);
        GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
        ret = TRUE;
      }
      break;
  }
  return ret;
}

static gboolean
gst_audio_decoder_sink_event (GstPad * pad, GstObject * parent,
    GstEvent * event)
{
  GstAudioDecoder *dec;
  GstAudioDecoderClass *klass;
  gboolean ret;

  dec = GST_AUDIO_DECODER (parent);
  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_DEBUG_OBJECT (dec, "received event %d, %s", GST_EVENT_TYPE (event),
      GST_EVENT_TYPE_NAME (event));

  if (klass->sink_event)
    ret = klass->sink_event (dec, event);
  else {
    gst_event_unref (event);
    ret = FALSE;
  }
  return ret;
}

static gboolean
gst_audio_decoder_do_seek (GstAudioDecoder * dec, GstEvent * event)
{
  GstSeekFlags flags;
  GstSeekType start_type, end_type;
  GstFormat format;
  gdouble rate;
  gint64 start, start_time, end_time;
  GstSegment seek_segment;
  guint32 seqnum;

  gst_event_parse_seek (event, &rate, &format, &flags, &start_type,
      &start_time, &end_type, &end_time);

  /* we'll handle plain open-ended flushing seeks with the simple approach */
  if (rate != 1.0) {
    GST_DEBUG_OBJECT (dec, "unsupported seek: rate");
    return FALSE;
  }

  if (start_type != GST_SEEK_TYPE_SET) {
    GST_DEBUG_OBJECT (dec, "unsupported seek: start time");
    return FALSE;
  }

  if ((end_type != GST_SEEK_TYPE_SET && end_type != GST_SEEK_TYPE_NONE) ||
      (end_type == GST_SEEK_TYPE_SET && end_time != GST_CLOCK_TIME_NONE)) {
    GST_DEBUG_OBJECT (dec, "unsupported seek: end time");
    return FALSE;
  }

  if (!(flags & GST_SEEK_FLAG_FLUSH)) {
    GST_DEBUG_OBJECT (dec, "unsupported seek: not flushing");
    return FALSE;
  }

  memcpy (&seek_segment, &dec->output_segment, sizeof (seek_segment));
  gst_segment_do_seek (&seek_segment, rate, format, flags, start_type,
      start_time, end_type, end_time, NULL);
  start_time = seek_segment.position;

  if (!gst_pad_query_convert (dec->sinkpad, GST_FORMAT_TIME, start_time,
          GST_FORMAT_BYTES, &start)) {
    GST_DEBUG_OBJECT (dec, "conversion failed");
    return FALSE;
  }

  seqnum = gst_event_get_seqnum (event);
  event = gst_event_new_seek (1.0, GST_FORMAT_BYTES, flags,
      GST_SEEK_TYPE_SET, start, GST_SEEK_TYPE_NONE, -1);
  gst_event_set_seqnum (event, seqnum);

  GST_DEBUG_OBJECT (dec, "seeking to %" GST_TIME_FORMAT " at byte offset %"
      G_GINT64_FORMAT, GST_TIME_ARGS (start_time), start);

  return gst_pad_push_event (dec->sinkpad, event);
}

static gboolean
gst_audio_decoder_src_eventfunc (GstAudioDecoder * dec, GstEvent * event)
{
  gboolean res;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
    {
      GstFormat format;
      gdouble rate;
      GstSeekFlags flags;
      GstSeekType start_type, stop_type;
      gint64 start, stop;
      gint64 tstart, tstop;
      guint32 seqnum;

      gst_event_parse_seek (event, &rate, &format, &flags, &start_type, &start,
          &stop_type, &stop);
      seqnum = gst_event_get_seqnum (event);

      /* upstream gets a chance first */
      if ((res = gst_pad_push_event (dec->sinkpad, event)))
        break;

      /* if upstream fails for a time seek, maybe we can help if allowed */
      if (format == GST_FORMAT_TIME) {
        if (gst_audio_decoder_do_byte (dec))
          res = gst_audio_decoder_do_seek (dec, event);
        break;
      }

      /* ... though a non-time seek can be aided as well */
      /* First bring the requested format to time */
      if (!(res =
              gst_pad_query_convert (dec->srcpad, format, start,
                  GST_FORMAT_TIME, &tstart)))
        goto convert_error;
      if (!(res =
              gst_pad_query_convert (dec->srcpad, format, stop, GST_FORMAT_TIME,
                  &tstop)))
        goto convert_error;

      /* then seek with time on the peer */
      event = gst_event_new_seek (rate, GST_FORMAT_TIME,
          flags, start_type, tstart, stop_type, tstop);
      gst_event_set_seqnum (event, seqnum);

      res = gst_pad_push_event (dec->sinkpad, event);
      break;
    }
    default:
      res = gst_pad_event_default (dec->srcpad, GST_OBJECT_CAST (dec), event);
      break;
  }
done:
  return res;

  /* ERRORS */
convert_error:
  {
    GST_DEBUG_OBJECT (dec, "cannot convert start/stop for seek");
    goto done;
  }
}

static gboolean
gst_audio_decoder_src_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  GstAudioDecoder *dec;
  GstAudioDecoderClass *klass;
  gboolean ret;

  dec = GST_AUDIO_DECODER (parent);
  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_DEBUG_OBJECT (dec, "received event %d, %s", GST_EVENT_TYPE (event),
      GST_EVENT_TYPE_NAME (event));

  if (klass->src_event)
    ret = klass->src_event (dec, event);
  else {
    gst_event_unref (event);
    ret = FALSE;
  }

  return ret;
}

static gboolean
gst_audio_decoder_decide_allocation_default (GstAudioDecoder * dec,
    GstQuery * query)
{
  GstAllocator *allocator = NULL;
  GstAllocationParams params;
  gboolean update_allocator;

  /* we got configuration from our peer or the decide_allocation method,
   * parse them */
  if (gst_query_get_n_allocation_params (query) > 0) {
    /* try the allocator */
    gst_query_parse_nth_allocation_param (query, 0, &allocator, &params);
    update_allocator = TRUE;
  } else {
    allocator = NULL;
    gst_allocation_params_init (&params);
    update_allocator = FALSE;
  }

  if (update_allocator)
    gst_query_set_nth_allocation_param (query, 0, allocator, &params);
  else
    gst_query_add_allocation_param (query, allocator, &params);
  if (allocator)
    gst_object_unref (allocator);

  return TRUE;
}

static gboolean
gst_audio_decoder_propose_allocation_default (GstAudioDecoder * dec,
    GstQuery * query)
{
  return TRUE;
}

/**
 * gst_audio_decoder_proxy_getcaps:
 * @decoder: a #GstAudioDecoder
 * @caps: (allow-none): initial caps
 * @filter: (allow-none): filter caps
 *
 * Returns caps that express @caps (or sink template caps if @caps == NULL)
 * restricted to rate/channels/... combinations supported by downstream
 * elements.
 *
 * Returns: (transfer full): a #GstCaps owned by caller
 *
 * Since: 1.6
 */
GstCaps *
gst_audio_decoder_proxy_getcaps (GstAudioDecoder * decoder, GstCaps * caps,
    GstCaps * filter)
{
  return __gst_audio_element_proxy_getcaps (GST_ELEMENT_CAST (decoder),
      GST_AUDIO_DECODER_SINK_PAD (decoder),
      GST_AUDIO_DECODER_SRC_PAD (decoder), caps, filter);
}

static GstCaps *
gst_audio_decoder_sink_getcaps (GstAudioDecoder * decoder, GstCaps * filter)
{
  GstAudioDecoderClass *klass;
  GstCaps *caps;

  klass = GST_AUDIO_DECODER_GET_CLASS (decoder);

  if (klass->getcaps)
    caps = klass->getcaps (decoder, filter);
  else
    caps = gst_audio_decoder_proxy_getcaps (decoder, NULL, filter);

  GST_LOG_OBJECT (decoder, "Returning caps %" GST_PTR_FORMAT, caps);

  return caps;
}

static gboolean
gst_audio_decoder_sink_query_default (GstAudioDecoder * dec, GstQuery * query)
{
  GstPad *pad = GST_AUDIO_DECODER_SINK_PAD (dec);
  gboolean res = FALSE;

  GST_LOG_OBJECT (dec, "handling query: %" GST_PTR_FORMAT, query);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_FORMATS:
    {
      gst_query_set_formats (query, 2, GST_FORMAT_TIME, GST_FORMAT_BYTES);
      res = TRUE;
      break;
    }
    case GST_QUERY_CONVERT:
    {
      GstFormat src_fmt, dest_fmt;
      gint64 src_val, dest_val;

      gst_query_parse_convert (query, &src_fmt, &src_val, &dest_fmt, &dest_val);
      GST_OBJECT_LOCK (dec);
      res = __gst_audio_encoded_audio_convert (&dec->priv->ctx.info,
          dec->priv->bytes_in, dec->priv->samples_out,
          src_fmt, src_val, &dest_fmt, &dest_val);
      GST_OBJECT_UNLOCK (dec);
      if (!res)
        goto error;
      gst_query_set_convert (query, src_fmt, src_val, dest_fmt, dest_val);
      break;
    }
    case GST_QUERY_ALLOCATION:
    {
      GstAudioDecoderClass *klass = GST_AUDIO_DECODER_GET_CLASS (dec);

      if (klass->propose_allocation)
        res = klass->propose_allocation (dec, query);
      break;
    }
    case GST_QUERY_CAPS:{
      GstCaps *filter, *caps;

      gst_query_parse_caps (query, &filter);
      caps = gst_audio_decoder_sink_getcaps (dec, filter);
      gst_query_set_caps_result (query, caps);
      gst_caps_unref (caps);
      res = TRUE;
      break;
    }
    case GST_QUERY_ACCEPT_CAPS:{
      if (dec->priv->use_default_pad_acceptcaps) {
        res =
            gst_pad_query_default (GST_AUDIO_DECODER_SINK_PAD (dec),
            GST_OBJECT_CAST (dec), query);
      } else {
        GstCaps *caps;
        GstCaps *allowed_caps;
        GstCaps *template_caps;
        gboolean accept;

        gst_query_parse_accept_caps (query, &caps);

        template_caps = gst_pad_get_pad_template_caps (pad);
        accept = gst_caps_is_subset (caps, template_caps);
        gst_caps_unref (template_caps);

        if (accept) {
          allowed_caps = gst_pad_query_caps (GST_AUDIO_DECODER_SINK_PAD (dec),
              caps);

          accept = gst_caps_can_intersect (caps, allowed_caps);

          gst_caps_unref (allowed_caps);
        }

        gst_query_set_accept_caps_result (query, accept);
        res = TRUE;
      }
      break;
    }
    case GST_QUERY_SEEKING:
    {
      GstFormat format;

      /* non-TIME segments are discarded, so we won't seek that way either */
      gst_query_parse_seeking (query, &format, NULL, NULL, NULL);
      if (format != GST_FORMAT_TIME) {
        GST_DEBUG_OBJECT (dec, "discarding non-TIME SEEKING query");
        res = FALSE;
        break;
      }
      /* fall-through */
    }
    default:
      res = gst_pad_query_default (pad, GST_OBJECT_CAST (dec), query);
      break;
  }

error:
  return res;
}

static gboolean
gst_audio_decoder_sink_query (GstPad * pad, GstObject * parent,
    GstQuery * query)
{
  GstAudioDecoderClass *dec_class;
  GstAudioDecoder *dec;
  gboolean ret = FALSE;

  dec = GST_AUDIO_DECODER (parent);
  dec_class = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_DEBUG_OBJECT (pad, "received query %" GST_PTR_FORMAT, query);

  if (dec_class->sink_query)
    ret = dec_class->sink_query (dec, query);

  return ret;
}

/* FIXME ? are any of these queries (other than latency) a decoder's business ??
 * also, the conversion stuff might seem to make sense, but seems to not mind
 * segment stuff etc at all
 * Supposedly that's backward compatibility ... */
static gboolean
gst_audio_decoder_src_query_default (GstAudioDecoder * dec, GstQuery * query)
{
  GstPad *pad = GST_AUDIO_DECODER_SRC_PAD (dec);
  gboolean res = FALSE;

  GST_LOG_OBJECT (dec, "handling query: %" GST_PTR_FORMAT, query);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_DURATION:
    {
      GstFormat format;

      /* upstream in any case */
      if ((res = gst_pad_query_default (pad, GST_OBJECT_CAST (dec), query)))
        break;

      gst_query_parse_duration (query, &format, NULL);
      /* try answering TIME by converting from BYTE if subclass allows  */
      if (format == GST_FORMAT_TIME && gst_audio_decoder_do_byte (dec)) {
        gint64 value;

        if (gst_pad_peer_query_duration (dec->sinkpad, GST_FORMAT_BYTES,
                &value)) {
          GST_LOG_OBJECT (dec, "upstream size %" G_GINT64_FORMAT, value);
          if (gst_pad_query_convert (dec->sinkpad, GST_FORMAT_BYTES, value,
                  GST_FORMAT_TIME, &value)) {
            gst_query_set_duration (query, GST_FORMAT_TIME, value);
            res = TRUE;
          }
        }
      }
      break;
    }
    case GST_QUERY_POSITION:
    {
      GstFormat format;
      gint64 time, value;

      if ((res = gst_pad_peer_query (dec->sinkpad, query))) {
        GST_LOG_OBJECT (dec, "returning peer response");
        break;
      }

      /* Refuse BYTES format queries. If it made sense to
       * answer them, upstream would have already */
      gst_query_parse_position (query, &format, NULL);

      if (format == GST_FORMAT_BYTES) {
        GST_LOG_OBJECT (dec, "Ignoring BYTES position query");
        break;
      }

      /* we start from the last seen time */
      time = dec->output_segment.position;
      /* correct for the segment values */
      time =
          gst_segment_to_stream_time (&dec->output_segment, GST_FORMAT_TIME,
          time);

      GST_LOG_OBJECT (dec,
          "query %p: our time: %" GST_TIME_FORMAT, query, GST_TIME_ARGS (time));

      /* and convert to the final format */
      if (!(res = gst_pad_query_convert (pad, GST_FORMAT_TIME, time,
                  format, &value)))
        break;

      gst_query_set_position (query, format, value);

      GST_LOG_OBJECT (dec,
          "query %p: we return %" G_GINT64_FORMAT " (format %u)", query, value,
          format);
      break;
    }
    case GST_QUERY_FORMATS:
    {
      gst_query_set_formats (query, 3,
          GST_FORMAT_TIME, GST_FORMAT_BYTES, GST_FORMAT_DEFAULT);
      res = TRUE;
      break;
    }
    case GST_QUERY_CONVERT:
    {
      GstFormat src_fmt, dest_fmt;
      gint64 src_val, dest_val;

      gst_query_parse_convert (query, &src_fmt, &src_val, &dest_fmt, &dest_val);
      GST_OBJECT_LOCK (dec);
      res = gst_audio_info_convert (&dec->priv->ctx.info,
          src_fmt, src_val, dest_fmt, &dest_val);
      GST_OBJECT_UNLOCK (dec);
      if (!res)
        break;
      gst_query_set_convert (query, src_fmt, src_val, dest_fmt, dest_val);
      break;
    }
    case GST_QUERY_LATENCY:
    {
      if ((res = gst_pad_peer_query (dec->sinkpad, query))) {
        gboolean live;
        GstClockTime min_latency, max_latency;

        gst_query_parse_latency (query, &live, &min_latency, &max_latency);
        GST_DEBUG_OBJECT (dec, "Peer latency: live %d, min %"
            GST_TIME_FORMAT " max %" GST_TIME_FORMAT, live,
            GST_TIME_ARGS (min_latency), GST_TIME_ARGS (max_latency));

        GST_OBJECT_LOCK (dec);
        /* add our latency */
        min_latency += dec->priv->ctx.min_latency;
        if (max_latency == -1 || dec->priv->ctx.max_latency == -1)
          max_latency = -1;
        else
          max_latency += dec->priv->ctx.max_latency;
        GST_OBJECT_UNLOCK (dec);

        gst_query_set_latency (query, live, min_latency, max_latency);
      }
      break;
    }
    default:
      res = gst_pad_query_default (pad, GST_OBJECT_CAST (dec), query);
      break;
  }

  return res;
}

static gboolean
gst_audio_decoder_src_query (GstPad * pad, GstObject * parent, GstQuery * query)
{
  GstAudioDecoder *dec;
  GstAudioDecoderClass *dec_class;
  gboolean ret = FALSE;

  dec = GST_AUDIO_DECODER (parent);
  dec_class = GST_AUDIO_DECODER_GET_CLASS (dec);

  GST_DEBUG_OBJECT (pad, "received query %" GST_PTR_FORMAT, query);

  if (dec_class->src_query)
    ret = dec_class->src_query (dec, query);

  return ret;
}

static gboolean
gst_audio_decoder_stop (GstAudioDecoder * dec)
{
  GstAudioDecoderClass *klass;
  gboolean ret = TRUE;

  GST_DEBUG_OBJECT (dec, "gst_audio_decoder_stop");

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  if (klass->stop) {
    ret = klass->stop (dec);
  }

  /* clean up */
  gst_audio_decoder_reset (dec, TRUE);

  if (ret)
    dec->priv->active = FALSE;

  return ret;
}

static gboolean
gst_audio_decoder_start (GstAudioDecoder * dec)
{
  GstAudioDecoderClass *klass;
  gboolean ret = TRUE;

  GST_DEBUG_OBJECT (dec, "gst_audio_decoder_start");

  klass = GST_AUDIO_DECODER_GET_CLASS (dec);

  /* arrange clean state */
  gst_audio_decoder_reset (dec, TRUE);

  if (klass->start) {
    ret = klass->start (dec);
  }

  if (ret)
    dec->priv->active = TRUE;

  return ret;
}

static void
gst_audio_decoder_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAudioDecoder *dec;

  dec = GST_AUDIO_DECODER (object);

  switch (prop_id) {
    case PROP_LATENCY:
      g_value_set_int64 (value, dec->priv->latency);
      break;
    case PROP_TOLERANCE:
      g_value_set_int64 (value, dec->priv->tolerance);
      break;
    case PROP_PLC:
      g_value_set_boolean (value, dec->priv->plc);
      break;
    case PROP_MAX_ERRORS:
      g_value_set_int (value, gst_audio_decoder_get_max_errors (dec));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_audio_decoder_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAudioDecoder *dec;

  dec = GST_AUDIO_DECODER (object);

  switch (prop_id) {
    case PROP_LATENCY:
      dec->priv->latency = g_value_get_int64 (value);
      break;
    case PROP_TOLERANCE:
      dec->priv->tolerance = g_value_get_int64 (value);
      break;
    case PROP_PLC:
      dec->priv->plc = g_value_get_boolean (value);
      break;
    case PROP_MAX_ERRORS:
      gst_audio_decoder_set_max_errors (dec, g_value_get_int (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstStateChangeReturn
gst_audio_decoder_change_state (GstElement * element, GstStateChange transition)
{
  GstAudioDecoder *codec;
  GstAudioDecoderClass *klass;
  GstStateChangeReturn ret;

  codec = GST_AUDIO_DECODER (element);
  klass = GST_AUDIO_DECODER_GET_CLASS (codec);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      if (klass->open) {
        if (!klass->open (codec))
          goto open_failed;
      }
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      if (!gst_audio_decoder_start (codec)) {
        goto start_failed;
      }
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
      if (!gst_audio_decoder_stop (codec)) {
        goto stop_failed;
      }
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (klass->close) {
        if (!klass->close (codec))
          goto close_failed;
      }
      break;
    default:
      break;
  }

  return ret;

start_failed:
  {
    GST_ELEMENT_ERROR (codec, LIBRARY, INIT, (NULL), ("Failed to start codec"));
    return GST_STATE_CHANGE_FAILURE;
  }
stop_failed:
  {
    GST_ELEMENT_ERROR (codec, LIBRARY, INIT, (NULL), ("Failed to stop codec"));
    return GST_STATE_CHANGE_FAILURE;
  }
open_failed:
  {
    GST_ELEMENT_ERROR (codec, LIBRARY, INIT, (NULL), ("Failed to open codec"));
    return GST_STATE_CHANGE_FAILURE;
  }
close_failed:
  {
    GST_ELEMENT_ERROR (codec, LIBRARY, INIT, (NULL), ("Failed to close codec"));
    return GST_STATE_CHANGE_FAILURE;
  }
}

GstFlowReturn
_gst_audio_decoder_error (GstAudioDecoder * dec, gint weight,
    GQuark domain, gint code, gchar * txt, gchar * dbg, const gchar * file,
    const gchar * function, gint line)
{
  if (txt)
    GST_WARNING_OBJECT (dec, "error: %s", txt);
  if (dbg)
    GST_WARNING_OBJECT (dec, "error: %s", dbg);
  dec->priv->error_count += weight;
  dec->priv->discont = TRUE;
  if (dec->priv->max_errors >= 0
      && dec->priv->max_errors < dec->priv->error_count) {
    gst_element_message_full (GST_ELEMENT (dec), GST_MESSAGE_ERROR, domain,
        code, txt, dbg, file, function, line);
    return GST_FLOW_ERROR;
  } else {
    g_free (txt);
    g_free (dbg);
    return GST_FLOW_OK;
  }
}

/**
 * gst_audio_decoder_get_audio_info:
 * @dec: a #GstAudioDecoder
 *
 * Returns: a #GstAudioInfo describing the input audio format
 */
GstAudioInfo *
gst_audio_decoder_get_audio_info (GstAudioDecoder * dec)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), NULL);

  return &dec->priv->ctx.info;
}

/**
 * gst_audio_decoder_set_plc_aware:
 * @dec: a #GstAudioDecoder
 * @plc: new plc state
 *
 * Indicates whether or not subclass handles packet loss concealment (plc).
 */
void
gst_audio_decoder_set_plc_aware (GstAudioDecoder * dec, gboolean plc)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  dec->priv->ctx.do_plc = plc;
}

/**
 * gst_audio_decoder_get_plc_aware:
 * @dec: a #GstAudioDecoder
 *
 * Returns: currently configured plc handling
 */
gint
gst_audio_decoder_get_plc_aware (GstAudioDecoder * dec)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  return dec->priv->ctx.do_plc;
}

/**
 * gst_audio_decoder_set_estimate_rate:
 * @dec: a #GstAudioDecoder
 * @enabled: whether to enable byte to time conversion
 *
 * Allows baseclass to perform byte to time estimated conversion.
 */
void
gst_audio_decoder_set_estimate_rate (GstAudioDecoder * dec, gboolean enabled)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  dec->priv->ctx.do_estimate_rate = enabled;
}

/**
 * gst_audio_decoder_get_estimate_rate:
 * @dec: a #GstAudioDecoder
 *
 * Returns: currently configured byte to time conversion setting
 */
gint
gst_audio_decoder_get_estimate_rate (GstAudioDecoder * dec)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  return dec->priv->ctx.do_estimate_rate;
}

/**
 * gst_audio_decoder_get_delay:
 * @dec: a #GstAudioDecoder
 *
 * Returns: currently configured decoder delay
 */
gint
gst_audio_decoder_get_delay (GstAudioDecoder * dec)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  return dec->priv->ctx.delay;
}

/**
 * gst_audio_decoder_set_max_errors:
 * @dec: a #GstAudioDecoder
 * @num: max tolerated errors
 *
 * Sets numbers of tolerated decoder errors, where a tolerated one is then only
 * warned about, but more than tolerated will lead to fatal error. You can set
 * -1 for never returning fatal errors. Default is set to
 * GST_AUDIO_DECODER_MAX_ERRORS.
 */
void
gst_audio_decoder_set_max_errors (GstAudioDecoder * dec, gint num)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  dec->priv->max_errors = num;
}

/**
 * gst_audio_decoder_get_max_errors:
 * @dec: a #GstAudioDecoder
 *
 * Returns: currently configured decoder tolerated error count.
 */
gint
gst_audio_decoder_get_max_errors (GstAudioDecoder * dec)
{
  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  return dec->priv->max_errors;
}

/**
 * gst_audio_decoder_set_latency:
 * @dec: a #GstAudioDecoder
 * @min: minimum latency
 * @max: maximum latency
 *
 * Sets decoder latency.
 */
void
gst_audio_decoder_set_latency (GstAudioDecoder * dec,
    GstClockTime min, GstClockTime max)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));
  g_return_if_fail (GST_CLOCK_TIME_IS_VALID (min));
  g_return_if_fail (min <= max);

  GST_OBJECT_LOCK (dec);
  dec->priv->ctx.min_latency = min;
  dec->priv->ctx.max_latency = max;
  GST_OBJECT_UNLOCK (dec);

  /* post latency message on the bus */
  gst_element_post_message (GST_ELEMENT (dec),
      gst_message_new_latency (GST_OBJECT (dec)));
}

/**
 * gst_audio_decoder_get_latency:
 * @dec: a #GstAudioDecoder
 * @min: (out) (allow-none): a pointer to storage to hold minimum latency
 * @max: (out) (allow-none): a pointer to storage to hold maximum latency
 *
 * Sets the variables pointed to by @min and @max to the currently configured
 * latency.
 */
void
gst_audio_decoder_get_latency (GstAudioDecoder * dec,
    GstClockTime * min, GstClockTime * max)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_OBJECT_LOCK (dec);
  if (min)
    *min = dec->priv->ctx.min_latency;
  if (max)
    *max = dec->priv->ctx.max_latency;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_parse_state:
 * @dec: a #GstAudioDecoder
 * @sync: (out) (optional): a pointer to a variable to hold the current sync state
 * @eos: (out) (optional): a pointer to a variable to hold the current eos state
 *
 * Return current parsing (sync and eos) state.
 */
void
gst_audio_decoder_get_parse_state (GstAudioDecoder * dec,
    gboolean * sync, gboolean * eos)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  if (sync)
    *sync = dec->priv->ctx.sync;
  if (eos)
    *eos = dec->priv->ctx.eos;
}

/**
 * gst_audio_decoder_set_allocation_caps:
 * @dec: a #GstAudioDecoder
 * @allocation_caps: (allow-none): a #GstCaps or %NULL
 *
 * Sets a caps in allocation query which are different from the set
 * pad's caps. Use this function before calling
 * gst_audio_decoder_negotiate(). Setting to %NULL the allocation
 * query will use the caps from the pad.
 *
 * Since: 1.10
 */
void
gst_audio_decoder_set_allocation_caps (GstAudioDecoder * dec,
    GstCaps * allocation_caps)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  gst_caps_replace (&dec->priv->ctx.allocation_caps, allocation_caps);
}

/**
 * gst_audio_decoder_set_plc:
 * @dec: a #GstAudioDecoder
 * @enabled: new state
 *
 * Enable or disable decoder packet loss concealment, provided subclass
 * and codec are capable and allow handling plc.
 *
 * MT safe.
 */
void
gst_audio_decoder_set_plc (GstAudioDecoder * dec, gboolean enabled)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_LOG_OBJECT (dec, "enabled: %d", enabled);

  GST_OBJECT_LOCK (dec);
  dec->priv->plc = enabled;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_plc:
 * @dec: a #GstAudioDecoder
 *
 * Queries decoder packet loss concealment handling.
 *
 * Returns: TRUE if packet loss concealment is enabled.
 *
 * MT safe.
 */
gboolean
gst_audio_decoder_get_plc (GstAudioDecoder * dec)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);

  GST_OBJECT_LOCK (dec);
  result = dec->priv->plc;
  GST_OBJECT_UNLOCK (dec);

  return result;
}

/**
 * gst_audio_decoder_set_min_latency:
 * @dec: a #GstAudioDecoder
 * @num: new minimum latency
 *
 * Sets decoder minimum aggregation latency.
 *
 * MT safe.
 */
void
gst_audio_decoder_set_min_latency (GstAudioDecoder * dec, GstClockTime num)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_OBJECT_LOCK (dec);
  dec->priv->latency = num;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_min_latency:
 * @dec: a #GstAudioDecoder
 *
 * Queries decoder's latency aggregation.
 *
 * Returns: aggregation latency.
 *
 * MT safe.
 */
GstClockTime
gst_audio_decoder_get_min_latency (GstAudioDecoder * dec)
{
  GstClockTime result;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);

  GST_OBJECT_LOCK (dec);
  result = dec->priv->latency;
  GST_OBJECT_UNLOCK (dec);

  return result;
}

/**
 * gst_audio_decoder_set_tolerance:
 * @dec: a #GstAudioDecoder
 * @tolerance: new tolerance
 *
 * Configures decoder audio jitter tolerance threshold.
 *
 * MT safe.
 */
void
gst_audio_decoder_set_tolerance (GstAudioDecoder * dec, GstClockTime tolerance)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_OBJECT_LOCK (dec);
  dec->priv->tolerance = tolerance;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_tolerance:
 * @dec: a #GstAudioDecoder
 *
 * Queries current audio jitter tolerance threshold.
 *
 * Returns: decoder audio jitter tolerance threshold.
 *
 * MT safe.
 */
GstClockTime
gst_audio_decoder_get_tolerance (GstAudioDecoder * dec)
{
  GstClockTime result;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  GST_OBJECT_LOCK (dec);
  result = dec->priv->tolerance;
  GST_OBJECT_UNLOCK (dec);

  return result;
}

/**
 * gst_audio_decoder_set_drainable:
 * @dec: a #GstAudioDecoder
 * @enabled: new state
 *
 * Configures decoder drain handling.  If drainable, subclass might
 * be handed a NULL buffer to have it return any leftover decoded data.
 * Otherwise, it is not considered so capable and will only ever be passed
 * real data.
 *
 * MT safe.
 */
void
gst_audio_decoder_set_drainable (GstAudioDecoder * dec, gboolean enabled)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_OBJECT_LOCK (dec);
  dec->priv->drainable = enabled;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_drainable:
 * @dec: a #GstAudioDecoder
 *
 * Queries decoder drain handling.
 *
 * Returns: TRUE if drainable handling is enabled.
 *
 * MT safe.
 */
gboolean
gst_audio_decoder_get_drainable (GstAudioDecoder * dec)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), 0);

  GST_OBJECT_LOCK (dec);
  result = dec->priv->drainable;
  GST_OBJECT_UNLOCK (dec);

  return result;
}

/**
 * gst_audio_decoder_set_needs_format:
 * @dec: a #GstAudioDecoder
 * @enabled: new state
 *
 * Configures decoder format needs.  If enabled, subclass needs to be
 * negotiated with format caps before it can process any data.  It will then
 * never be handed any data before it has been configured.
 * Otherwise, it might be handed data without having been configured and
 * is then expected being able to do so either by default
 * or based on the input data.
 *
 * MT safe.
 */
void
gst_audio_decoder_set_needs_format (GstAudioDecoder * dec, gboolean enabled)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  GST_OBJECT_LOCK (dec);
  dec->priv->needs_format = enabled;
  GST_OBJECT_UNLOCK (dec);
}

/**
 * gst_audio_decoder_get_needs_format:
 * @dec: a #GstAudioDecoder
 *
 * Queries decoder required format handling.
 *
 * Returns: TRUE if required format handling is enabled.
 *
 * MT safe.
 */
gboolean
gst_audio_decoder_get_needs_format (GstAudioDecoder * dec)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_AUDIO_DECODER (dec), FALSE);

  GST_OBJECT_LOCK (dec);
  result = dec->priv->needs_format;
  GST_OBJECT_UNLOCK (dec);

  return result;
}

/**
 * gst_audio_decoder_merge_tags:
 * @dec: a #GstAudioDecoder
 * @tags: (allow-none): a #GstTagList to merge, or NULL
 * @mode: the #GstTagMergeMode to use, usually #GST_TAG_MERGE_REPLACE
 *
 * Sets the audio decoder tags and how they should be merged with any
 * upstream stream tags. This will override any tags previously-set
 * with gst_audio_decoder_merge_tags().
 *
 * Note that this is provided for convenience, and the subclass is
 * not required to use this and can still do tag handling on its own.
 */
void
gst_audio_decoder_merge_tags (GstAudioDecoder * dec,
    const GstTagList * tags, GstTagMergeMode mode)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));
  g_return_if_fail (tags == NULL || GST_IS_TAG_LIST (tags));
  g_return_if_fail (mode != GST_TAG_MERGE_UNDEFINED);

  GST_AUDIO_DECODER_STREAM_LOCK (dec);
  if (dec->priv->taglist != tags) {
    if (dec->priv->taglist) {
      gst_tag_list_unref (dec->priv->taglist);
      dec->priv->taglist = NULL;
      dec->priv->decoder_tags_merge_mode = GST_TAG_MERGE_KEEP_ALL;
    }
    if (tags) {
      dec->priv->taglist = gst_tag_list_ref ((GstTagList *) tags);
      dec->priv->decoder_tags_merge_mode = mode;
    }

    GST_DEBUG_OBJECT (dec, "setting decoder tags to %" GST_PTR_FORMAT, tags);
    dec->priv->taglist_changed = TRUE;
  }
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);
}

/**
 * gst_audio_decoder_allocate_output_buffer:
 * @dec: a #GstAudioDecoder
 * @size: size of the buffer
 *
 * Helper function that allocates a buffer to hold an audio frame
 * for @dec's current output format.
 *
 * Returns: (transfer full): allocated buffer
 */
GstBuffer *
gst_audio_decoder_allocate_output_buffer (GstAudioDecoder * dec, gsize size)
{
  GstBuffer *buffer = NULL;
  gboolean needs_reconfigure = FALSE;

  g_return_val_if_fail (size > 0, NULL);

  GST_DEBUG ("alloc src buffer");

  GST_AUDIO_DECODER_STREAM_LOCK (dec);

  needs_reconfigure = gst_pad_check_reconfigure (dec->srcpad);
  if (G_UNLIKELY (dec->priv->ctx.output_format_changed ||
          (GST_AUDIO_INFO_IS_VALID (&dec->priv->ctx.info)
              && needs_reconfigure))) {
    if (!gst_audio_decoder_negotiate_unlocked (dec)) {
      GST_INFO_OBJECT (dec, "Failed to negotiate, fallback allocation");
      gst_pad_mark_reconfigure (dec->srcpad);
      goto fallback;
    }
  }

  buffer =
      gst_buffer_new_allocate (dec->priv->ctx.allocator, size,
      &dec->priv->ctx.params);
  if (!buffer) {
    GST_INFO_OBJECT (dec, "couldn't allocate output buffer");
    goto fallback;
  }

  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return buffer;
fallback:
  buffer = gst_buffer_new_allocate (NULL, size, NULL);
  GST_AUDIO_DECODER_STREAM_UNLOCK (dec);

  return buffer;
}

/**
 * gst_audio_decoder_get_allocator:
 * @dec: a #GstAudioDecoder
 * @allocator: (out) (allow-none) (transfer full): the #GstAllocator
 * used
 * @params: (out) (allow-none) (transfer full): the
 * #GstAllocationParams of @allocator
 *
 * Lets #GstAudioDecoder sub-classes to know the memory @allocator
 * used by the base class and its @params.
 *
 * Unref the @allocator after use it.
 */
void
gst_audio_decoder_get_allocator (GstAudioDecoder * dec,
    GstAllocator ** allocator, GstAllocationParams * params)
{
  g_return_if_fail (GST_IS_AUDIO_DECODER (dec));

  if (allocator)
    *allocator = dec->priv->ctx.allocator ?
        gst_object_ref (dec->priv->ctx.allocator) : NULL;

  if (params)
    *params = dec->priv->ctx.params;
}

/**
 * gst_audio_decoder_set_use_default_pad_acceptcaps:
 * @decoder: a #GstAudioDecoder
 * @use: if the default pad accept-caps query handling should be used
 *
 * Lets #GstAudioDecoder sub-classes decide if they want the sink pad
 * to use the default pad query handler to reply to accept-caps queries.
 *
 * By setting this to true it is possible to further customize the default
 * handler with %GST_PAD_SET_ACCEPT_INTERSECT and
 * %GST_PAD_SET_ACCEPT_TEMPLATE
 *
 * Since: 1.6
 */
void
gst_audio_decoder_set_use_default_pad_acceptcaps (GstAudioDecoder * decoder,
    gboolean use)
{
  decoder->priv->use_default_pad_acceptcaps = use;
}
