/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

//#ifdef HAVE_CONFIG_H
//#  include <config.h>
//#endif

#include <gst/gst.h>
#include <string.h>

#include "flvdemux.h"
#include "flvparser.h"
#include "flvmetadata.h"
#include <fxplugins_common.h>

GST_DEBUG_CATEGORY_EXTERN (fxm_plugin_debug);
#define GST_CAT_DEFAULT fxm_plugin_debug

/* the capabilities of the inputs and outputs.*/
static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("video/x-flv")
    );

static GstStaticPadTemplate audio_src_template =
GST_STATIC_PAD_TEMPLATE ("audio",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate video_src_template =
GST_STATIC_PAD_TEMPLATE ("video",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (FlvDemux, flv_demux, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void flv_demux_base_init     (gpointer       g_class);
static void flv_demux_class_init    (FlvDemuxClass *g_class);
static void flv_demux_init          (FlvDemux      *object,
                                     FlvDemuxClass *g_class);
static GstElementClass *parent_class = NULL;

static void flv_demux_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *)  g_type_class_peek_parent (g_class);
    flv_demux_class_init ((FlvDemuxClass *)g_class);
}

GType flv_demux_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
                                               g_intern_static_string ("FlvDemux"),
                                               sizeof (FlvDemuxClass),
                                               flv_demux_base_init,
                                               NULL,
                                               flv_demux_class_init_trampoline,
                                               NULL,
                                               NULL,
                                               sizeof (FlvDemux),
                                               0,
                                               (GInstanceInitFunc) flv_demux_init,
                                               NULL,
                                               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
 * Init stuff
 ***********************************************************************************/
static void flv_demux_dispose(GObject* object);

static GstStateChangeReturn
            flv_demux_change_state(GstElement * element, GstStateChange transition);

static gboolean flv_demux_sink_event (GstPad * pad, GstEvent * event);
static GstFlowReturn flv_demux_chain (GstPad * pad, GstBuffer * buf);
static gboolean flv_demux_sink_activate (GstPad * sinkpad);
static gboolean flv_demux_sink_activate_pull (GstPad * sinkpad, gboolean active);
static gboolean flv_demux_sink_activate_push (GstPad * sinkpad, gboolean active);
static const GstQueryType * flv_demux_sink_query_types (GstPad * pad);
static gboolean flv_demux_sink_query (GstPad * pad, GstQuery * query);
static void flv_demux_loop (GstPad * pad);

static const GstQueryType * flv_demux_src_query_types (GstPad * pad);
static gboolean flv_demux_src_query (GstPad * pad, GstQuery * query);
static gboolean flv_demux_src_event (GstPad * pad, GstEvent * event);

/*!
 * \brief Creates time-position association of a keyframe in internal index.
 */
static void
flv_demux_index_add_entry(FlvDemux* filter,
        GstClockTime index_time, guint64 index_pos);

/*!
 * \brief Searches for last keyframe prior to specified time in internal index.
 */
static gboolean
flv_demux_index_lookup(FlvDemux* filter,
        GstClockTime time, GstClockTime *index_time, guint64 *index_pos);

/* Base init */
static void
flv_demux_base_init (gpointer gclass)
{
    //fprintf(stderr, "===flv_demux_base_init()\n");
    GstElementClass *element_class = GST_ELEMENT_CLASS (gclass);

    gst_element_class_set_details_simple(element_class,
            "FlvDemux",
            "Coder/Demuxer",
            "Split flv stream to video and audio streams",
            "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&sink_template));
    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&audio_src_template));
    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&video_src_template));
}

/* Initialize the class */
static void
flv_demux_class_init (FlvDemuxClass * klass)
{
    //fprintf(stderr, "===flv_demux_class_init()\n");
    GObjectClass *gobject_class;
    GstElementClass *gstelement_class;

    gobject_class = (GObjectClass *)klass;
    gstelement_class = (GstElementClass *)klass;
    gobject_class->dispose = GST_DEBUG_FUNCPTR (flv_demux_dispose);

    gstelement_class->change_state = GST_DEBUG_FUNCPTR(flv_demux_change_state);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void
flv_demux_init (FlvDemux * filter,
    FlvDemuxClass * gclass)
{
    //fprintf(stderr, "===flv_demux_init()\n");

    //Create sink
    filter->sink_pad = gst_pad_new_from_static_template (&sink_template, "sink");
    gst_pad_set_event_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_event));
    gst_pad_set_chain_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_chain));
    gst_pad_set_activate_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_activate));
    gst_pad_set_activatepull_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_activate_pull));
    gst_pad_set_activatepush_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_activate_push));
    gst_pad_set_query_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_query));
    gst_pad_set_query_type_function (filter->sink_pad,
            GST_DEBUG_FUNCPTR (flv_demux_sink_query_types));
    gst_element_add_pad (GST_ELEMENT (filter), filter->sink_pad);

    //Create adapter
    filter->adapter = gst_adapter_new();

    //Init parser
    flv_parser_init(&filter->parser);
    filter->need_parser_flush = FALSE;

    //Init indexing and seeking
    filter->current_timestamp = GST_CLOCK_TIME_NONE;
    filter->last_file_position = 0;

    filter->keyframes = NULL; // allocated dynamically
    filter->copied_metadata_keyframes = FALSE;
    filter->is_flushing = FALSE;
    gst_segment_init(&filter->segment, GST_FORMAT_TIME);

    //Init metadata
    filter->metadata = flv_metadata_new();
    filter->queued_tags = NULL;

    //Init Source pads
    filter->audio_src_pad = NULL;
    filter->video_src_pad = NULL;

    //Init Audio
    filter->sound_format = 0;
    filter->audio_linked = FALSE;
    filter->audio_offset = 0;
    filter->audio_discont = TRUE;
    filter->audio_prev_timestamp = GST_CLOCK_TIME_NONE;
    filter->audio_frame_duration = GST_CLOCK_TIME_NONE;

    //Init Video
    filter->video_codec_id = 0;
    filter->video_linked = FALSE;
    filter->video_offset = 0;
    filter->video_discont = TRUE;
    filter->video_prev_timestamp = GST_CLOCK_TIME_NONE;
    filter->video_frame_duration = GST_CLOCK_TIME_NONE;
}

static void
flv_demux_dispose(GObject* object)
{
    FlvDemux* filter = FLV_DEMUX(object);

    //Dispose adapter
    if (filter->adapter) {
        gst_adapter_clear(filter->adapter);
        g_object_unref(filter->adapter);
        filter->adapter = NULL;
    }

    //Dispose index
    if (filter->keyframes) {
        GList *keyframe = g_list_first(filter->keyframes);
        while (keyframe)
        {
            FlvKeyframe *entry = (FlvKeyframe*)keyframe->data;
            g_slice_free1(sizeof(FlvKeyframe), entry);
            keyframe = g_list_next(keyframe);
        }
        g_list_free(filter->keyframes);
        filter->keyframes = NULL;
    }

    flv_metadata_free(filter->metadata);

    if (filter->queued_tags) {
        gst_tag_list_free(filter->queued_tags);
    }

    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static GstStateChangeReturn
flv_demux_change_state (GstElement * element, GstStateChange transition)
{
    //fprintf(stderr, "===flv_demux_change_state(0x%02x)\n", transition);
    FlvDemux *filter;
    GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;

    filter = FLV_DEMUX (element);

    switch (transition) {
        case GST_STATE_CHANGE_READY_TO_PAUSED:
                        filter->last_file_position = 0;
                        filter->current_timestamp = 0;
                        filter->need_parser_flush = TRUE;
            break;
        default:
            break;
    }

    ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);
    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    return ret;
}

static gboolean
flv_demux_push_src_event (FlvDemux * filter, GstEvent * event)
{
  gboolean ret = TRUE;

  if (filter->audio_src_pad != NULL)
// INLINE - gst_event_ref()
    ret |= gst_pad_push_event (filter->audio_src_pad, gst_event_ref (event));

  if (filter->video_src_pad != NULL)
// INLINE - gst_event_ref()
    ret |= gst_pad_push_event (filter->video_src_pad, gst_event_ref (event));

// INLINE - gst_event_unref()
  gst_event_unref (event);

  return ret;
}

static gboolean
flv_demux_sink_event (GstPad * pad, GstEvent * event)
{
    gboolean ret = TRUE;
    FlvDemux *filter;

    //fprintf(stderr, "===flv_demux_sink_event(%s)\n", GST_EVENT_TYPE_NAME(event));

    filter = FLV_DEMUX (GST_OBJECT_PARENT (pad));
    switch (GST_EVENT_TYPE (event)) {
        case GST_EVENT_FLUSH_START:
            //fprintf(stderr, "===flv_demux_sink_event(%s)\n", GST_EVENT_TYPE_NAME(event));
            filter->is_flushing = TRUE;
            gst_pad_event_default(filter->sink_pad, event);
            break;
        case GST_EVENT_FLUSH_STOP:
            //fprintf(stderr, "===flv_demux_sink_event(%s)\n", GST_EVENT_TYPE_NAME(event));
            filter->is_flushing = FALSE;
            filter->need_parser_flush = TRUE;
            gst_pad_event_default(filter->sink_pad, event);
            break;
        case GST_EVENT_EOS:
            //fprintf(stderr, "Sending no-more-pads\n");
            gst_element_no_more_pads(GST_ELEMENT(filter));
            flv_demux_push_src_event (filter, event);
            ret = TRUE;
            break;
        case FX_EVENT_RANGE_READY: // This event appears only in pull mode during outrange seeking.
            ret = gst_pad_start_task (pad, (GstTaskFunction) flv_demux_loop, pad);
// INLINE - gst_event_unref()
            gst_event_unref(event);
            break;
        case GST_EVENT_NEWSEGMENT:
        {
            GstFormat format;
            gdouble rate;
            gint64 start, stop, time;
            gboolean update;

            gst_event_parse_new_segment (event, &update, &rate, &format, &start,
                    &stop, &time);

            //Use segment if it has time format, mark that segment needed otherwise.
            if (format == GST_FORMAT_TIME) {
                gst_segment_set_newsegment (&filter->segment, update, rate, format,
                        start, stop, time);
            }
            filter->audio_discont = TRUE;
            filter->video_discont = TRUE;
            break;
         }
        default:
            ret = flv_demux_push_src_event(filter, event);
            break;
    }
    return ret;
}

static gboolean
flv_demux_negotiate_audio_caps(FlvDemux* filter, guint sound_format,
        guint sampling_rate, gboolean is_16bit, gboolean is_stereo)
{
    //fprintf(stderr, "===flv_demux_negotiate_audio_caps()\n");
    gboolean result = FALSE;
    GstCaps *caps = NULL;
    gchar *codec_name = NULL;
    guint rate = 0;
    guint channels = 0;

    if (FLVDEMUX_AUDIO_FORMAT_MP3 != sound_format)
    {
        caps = gst_caps_new_simple ("audio//unsupported", NULL);
        codec_name = "Unsupported";
    }
    else
    {
        caps = gst_caps_new_simple ("audio/mpeg",
            "mpegversion", G_TYPE_INT, 1,
            "layer", G_TYPE_INT, 3,            // In FLV we support only MP3
            "framed", G_TYPE_BOOLEAN, FALSE,
            NULL);
        codec_name = "MPEG 1 Audio";

        if (sampling_rate == 0) {
            rate = 5500;
        } else if (sampling_rate == 1) {
            rate = 11025;
        } else if (sampling_rate == 2) {
            rate = 22050;
        } else if (sampling_rate == 3) {
            rate = 44100;
        }
        channels = (is_stereo) ? 2 : 1;

        gst_caps_set_simple (caps, "rate", G_TYPE_INT, rate, "channels", G_TYPE_INT, channels, NULL);
    }

    result = gst_pad_set_caps (filter->audio_src_pad, caps);
    gst_caps_unref (caps);

    if (result) {
        filter->sound_format = sound_format;

        if (filter->queued_tags == NULL)
            filter->queued_tags = gst_tag_list_new ();

        gst_tag_list_add (filter->queued_tags, GST_TAG_MERGE_REPLACE,
              GST_TAG_AUDIO_CODEC, codec_name, NULL);

        /* Push tags only if we don't expect more tags */
        if (!filter->has_video || filter->video_linked) {
            gst_element_found_tags(GST_ELEMENT(filter), filter->queued_tags);
            filter->queued_tags = NULL;
        }
    }

    return result;
}

static GstFlowReturn
flv_demux_parse_audio_tag(FlvDemux* filter, guchar* data, gsize size)
{
    FlvAudioTag audio_tag;
    GstBuffer* out;
    GstFlowReturn result = GST_FLOW_OK;
    gint parse_result;
    FlvParser* parser = &filter->parser;
    GstClockTime prev_frame_duration;

    parse_result = flv_parser_read_audio_tag(parser, data, size, &audio_tag);
    if (parse_result != FLV_PARSER_OK) {
        //fprintf(stderr, "flv_demux_chain() : Error parsing buffer : %d\n", parse_result);
        return GST_FLOW_ERROR;
    }

    filter->has_audio = TRUE;

    /* Create video pad if it's not initialized yet */
    if (!filter->audio_src_pad) {
        //fprintf(stderr, "Creating audio pad...\n");
        filter->audio_src_pad =
            gst_pad_new_from_template (gst_element_class_get_pad_template
            (GST_ELEMENT_GET_CLASS (filter), "audio"), "audio");

        gst_pad_set_query_type_function (filter->audio_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_query_types));
        gst_pad_set_query_function (filter->audio_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_query));
        gst_pad_set_event_function (filter->audio_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_event));

        /* Set caps for newly crated pad */
        if (!flv_demux_negotiate_audio_caps(filter, audio_tag.sound_format,
                audio_tag.sampling_rate, audio_tag.is_16bit, audio_tag.is_stereo)) {
            gst_object_unref(filter->audio_src_pad);
            filter->audio_src_pad = NULL;
            //fprintf(stderr, "Error creating audio pad.\n");
            return GST_FLOW_ERROR;
        }

        /* Fixate caps */
        gst_pad_use_fixed_caps (filter->audio_src_pad);

        /* Activate pad */
        if (!gst_pad_set_active (filter->audio_src_pad, TRUE)) {
            return GST_FLOW_ERROR;
        }

        /* We need to set caps before adding */
        if (!gst_element_add_pad (GST_ELEMENT (filter), filter->audio_src_pad)) {
            return GST_FLOW_ERROR;
        }

        /* Send no_more_pads only if we don't expect pads */
        if (filter->video_src_pad || !filter->has_video) {
            gst_element_no_more_pads(GST_ELEMENT(filter));
        }
    }

    // If the audio encoding is unsupported return OK as we've parsed the tag anyway.
    if(FLVDEMUX_AUDIO_FORMAT_MP3 != audio_tag.sound_format) {
        if(GST_CLOCK_TIME_NONE == filter->audio_prev_timestamp) {
            gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_WARNING, GST_STREAM_ERROR_CODEC_NOT_FOUND, GST_STREAM_ERROR_DEMUX, g_strdup("No audio decoder for this encoding"), NULL, ("flvdemux.c"), ("flv_demux_parse_audio_tag"), 0);
        }
        filter->audio_prev_timestamp = filter->current_timestamp;
        return GST_FLOW_OK;
    }

    /* Renegotiate caps if changed */
    if (filter->sound_format != audio_tag.sound_format) {
        flv_demux_negotiate_audio_caps(filter, audio_tag.sound_format,
                audio_tag.sampling_rate, audio_tag.is_16bit, audio_tag.is_stereo);
    }

    /* Create buffer and push data */
    result = gst_pad_alloc_buffer_and_set_caps (filter->audio_src_pad,
            GST_BUFFER_OFFSET_NONE, (gint)audio_tag.audio_packet_size,
            GST_PAD_CAPS (filter->audio_src_pad), &out);
    if (result != GST_FLOW_OK) {
        //fprintf(stderr, "Error creating audio buffer : %d\n", result);
        if (result == GST_FLOW_NOT_LINKED) {
            filter->audio_linked = FALSE;
        }
        return result;
    }
    filter->audio_linked = TRUE;

    /* Calculate last frame length */
    if (filter->audio_prev_timestamp != GST_CLOCK_TIME_NONE && !filter->audio_discont) {
        prev_frame_duration = filter->current_timestamp - filter->audio_prev_timestamp;
        if (filter->audio_frame_duration == GST_CLOCK_TIME_NONE
                || filter->audio_frame_duration < prev_frame_duration) {
            filter->audio_frame_duration = prev_frame_duration;
        }
    }
    filter->audio_prev_timestamp = filter->current_timestamp;

    /* Fill in buffer */
    memcpy(GST_BUFFER_DATA (out), data + audio_tag.audio_packet_offset, audio_tag.audio_packet_size);
    GST_BUFFER_TIMESTAMP(out) = filter->current_timestamp;
    GST_BUFFER_DURATION(out) = filter->audio_frame_duration;
    GST_BUFFER_OFFSET(out) = filter->audio_offset++;
    GST_BUFFER_OFFSET_END(out) = filter->audio_offset;

    gst_segment_set_last_stop (&filter->segment, GST_FORMAT_TIME,
            GST_BUFFER_TIMESTAMP (out));

    /* Handle discontinuity */
    if (filter->audio_discont) {
        GstEvent* new_seg_event = NULL;
        GST_BUFFER_FLAG_SET(out, GST_BUFFER_FLAG_DISCONT);
        new_seg_event =
                gst_event_new_new_segment (FALSE, filter->segment.rate,
                filter->segment.format, filter->segment.last_stop,
                filter->segment.stop, filter->segment.time);
        gst_pad_push_event (filter->audio_src_pad, new_seg_event);
        filter->audio_discont = FALSE;
    }

    /* Add index association only if we don't have video */
    if (!filter->has_video) {
        flv_demux_index_add_entry(filter, filter->current_timestamp, filter->last_file_position);
    }

    /* Push data downstream */
    result = gst_pad_push (filter->audio_src_pad, out);

    return result;
}

static gchar* flv_video_mime_type[16] = {
    "video/unsupported", "video/unsupported", "video/unsupported", "video/unsupported",
    "video/x-vp6-flash", "video/x-vp6-alpha", "video/unsupported", "video/unsupported",
    "video/unsupported", "video/unsupported", "video/unsupported", "video/unsupported",
    "video/unsupported", "video/unsupported", "video/unsupported", "video/unsupported"
};

static gchar* flv_video_codec_name[16] = {
    "Unsupported", "Unsupported", "Unsupported", "Unsupported",
    "On2 VP6 Video", "On2 VP6-Alpha Video", "Unsupported", "Unsupported",
    "Unsupported", "Unsupported", "Unsupported", "Unsupported",
    "Unsupported", "Unsupported", "Unsupported", "Unsupported"
};

static gboolean
flv_demux_negotiate_video_caps(FlvDemux* filter, guint codec_id)
{
    gboolean result = FALSE;
    GstCaps *caps = NULL;
    gchar *mime_type = flv_video_mime_type[codec_id];
    gchar *codec_name = flv_video_codec_name[codec_id];

    if (mime_type == NULL || filter->metadata == NULL)
        return FALSE;

    caps = gst_caps_new_simple (mime_type, NULL);

    // Set width and height
    if (filter->metadata->width != 0)
        gst_caps_set_simple (caps, "width", G_TYPE_INT, filter->metadata->width, NULL);

    if (filter->metadata->height != 0)
        gst_caps_set_simple (caps, "height", G_TYPE_INT, filter->metadata->height, NULL);

    // Set framerate
    if (filter->metadata->framerate != 0)
        gst_caps_set_simple (caps, "framerate", GST_TYPE_FRACTION,
                (gint)(filter->metadata->framerate*100), 100, NULL);

    //Set PAR only if both par_x and par_y are defined
    if (filter->metadata->par_x != 0 && filter->metadata->par_y != 0)
        gst_caps_set_simple (caps, "pixel-aspect-ratio", GST_TYPE_FRACTION,
                filter->metadata->par_x, filter->metadata->par_y, NULL);

    result = gst_pad_set_caps (filter->video_src_pad, caps);
    gst_caps_unref (caps);

    if (result) {
        filter->video_codec_id = codec_id;

        if (filter->queued_tags == NULL)
            filter->queued_tags = gst_tag_list_new ();

        gst_tag_list_add (filter->queued_tags, GST_TAG_MERGE_REPLACE,
              GST_TAG_VIDEO_CODEC, codec_name, NULL);

        /* Push tags only if we don't expect more tags */
        if (!filter->has_audio || filter->audio_linked) {
            gst_element_found_tags(GST_ELEMENT(filter), filter->queued_tags);
            filter->queued_tags = NULL;
        }
    }
    return result;
}

static GstFlowReturn
flv_demux_parse_video_tag(FlvDemux* filter, guchar* data, gsize size)
{
    FlvVideoTag video_tag;
    GstBuffer* out;
    GstFlowReturn result = GST_FLOW_OK;
    gint parse_result;
    gboolean need_push_tags = FALSE;
    FlvParser* parser = &filter->parser;
    gboolean is_keyframe;
    GstClockTime prev_frame_duration;

    parse_result = flv_parser_read_video_tag(parser, data, size, &video_tag);
    if (parse_result != FLV_PARSER_OK) {
        //fprintf(stderr, "flv_demux_chain() : Error parsing buffer : %d\n", parse_result);
        return GST_FLOW_ERROR;
    }

    is_keyframe = (video_tag.frame_type == FLV_VIDEO_FRAME_KEY);
    filter->has_video = TRUE;

    /* Create video pad if it's not initialized yet */
    if (!filter->video_src_pad) {
        //fprintf(stderr, "Creating video pad...\n");
        filter->video_src_pad =
            gst_pad_new_from_template (gst_element_class_get_pad_template
            (GST_ELEMENT_GET_CLASS (filter), "video"), "video");

        gst_pad_set_query_type_function (filter->video_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_query_types));
        gst_pad_set_query_function (filter->video_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_query));
        gst_pad_set_event_function (filter->video_src_pad,
            GST_DEBUG_FUNCPTR (flv_demux_src_event));

        /* Set caps for newly crated pad */
        if (!flv_demux_negotiate_video_caps(filter, video_tag.codec_id)) {
            gst_object_unref(filter->video_src_pad);
            filter->video_src_pad = NULL;
            return GST_FLOW_ERROR;
        }
        need_push_tags = TRUE;

        /* Fixate caps */
        gst_pad_use_fixed_caps (filter->video_src_pad);

        /* Activate pad */
        if (!gst_pad_set_active (filter->video_src_pad, TRUE)) {
            return GST_FLOW_ERROR;
        }

        /* We need to set caps before adding */
        if (!gst_element_add_pad (GST_ELEMENT (filter), filter->video_src_pad)) {
            return GST_FLOW_ERROR;
        }

        //Send no_more_pads only if we don't expect pads
        if (filter->audio_src_pad || !filter->has_audio) {
            gst_element_no_more_pads(GST_ELEMENT(filter));
        }
    }

    // If the video encoding is unsupported return OK as we've parsed the tag anyway.
    if(FLVDEMUX_VIDEO_FORMAT_VP6 != video_tag.codec_id &&
       FLVDEMUX_VIDEO_FORMAT_VP6_ALPHA != video_tag.codec_id) {
        if(GST_CLOCK_TIME_NONE == filter->video_prev_timestamp) {
            gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_WARNING, GST_STREAM_ERROR_CODEC_NOT_FOUND, GST_STREAM_ERROR_DEMUX, g_strdup("No video decoder for this encoding"), NULL, ("flvdemux.c"), ("flv_demux_parse_video_tag"), 0);
        }
        filter->video_prev_timestamp = filter->current_timestamp;
        return GST_FLOW_OK;
    }

    /* Renegotiate caps if changed */
    if (filter->video_codec_id != video_tag.codec_id) {
        flv_demux_negotiate_video_caps(filter, video_tag.codec_id);
    }

    /* Push tags if necessary */
    if (filter->queued_tags && need_push_tags) {
        gst_element_found_tags(GST_ELEMENT(filter), filter->queued_tags);
        filter->queued_tags = NULL;
    }

    /* Create buffer and push data */
    result = gst_pad_alloc_buffer_and_set_caps (filter->video_src_pad,
            GST_BUFFER_OFFSET_NONE, (gint)video_tag.video_packet_size,
            GST_PAD_CAPS (filter->video_src_pad), &out);
    if (result != GST_FLOW_OK) {
        if (result == GST_FLOW_NOT_LINKED) {
            filter->video_linked = FALSE;
        }
        return result;
    }
    filter->video_linked = TRUE;

    /* Calculate last frame length */
    if (filter->video_prev_timestamp != GST_CLOCK_TIME_NONE && !filter->video_discont) {
        prev_frame_duration = filter->current_timestamp - filter->video_prev_timestamp;
        if (filter->video_frame_duration == GST_CLOCK_TIME_NONE
                || filter->video_frame_duration < prev_frame_duration) {
            filter->video_frame_duration = prev_frame_duration;
        }
    }
    filter->video_prev_timestamp = filter->current_timestamp;

    /* Fill in buffer */
    memcpy(GST_BUFFER_DATA (out), data + video_tag.video_packet_offset, video_tag.video_packet_size);
    GST_BUFFER_TIMESTAMP(out) = filter->current_timestamp;
    GST_BUFFER_DURATION(out) = filter->video_frame_duration;
    GST_BUFFER_OFFSET(out) = filter->video_offset++;
    GST_BUFFER_OFFSET_END(out) = filter->video_offset;

    if (!is_keyframe)
        GST_BUFFER_FLAG_SET (out, GST_BUFFER_FLAG_DELTA_UNIT);

    gst_segment_set_last_stop (&filter->segment, GST_FORMAT_TIME,
            GST_BUFFER_TIMESTAMP (out));

    /* Handle discontinuity */
    if (filter->video_discont) {
        GstEvent* new_seg_event = NULL;
        GST_BUFFER_FLAG_SET(out, GST_BUFFER_FLAG_DISCONT);
        new_seg_event =
                gst_event_new_new_segment (FALSE, filter->segment.rate,
                filter->segment.format, filter->segment.last_stop,
                filter->segment.stop, filter->segment.time);
        gst_pad_push_event (filter->video_src_pad, new_seg_event);
        filter->video_discont = FALSE;
    }

    /* Add keyframes to index */
    if (is_keyframe) {
        flv_demux_index_add_entry(filter, filter->current_timestamp, filter->last_file_position);
    }

    /* Push data downstream */
    result = gst_pad_push (filter->video_src_pad, out);

    return result;
}

static GstFlowReturn
flv_demux_parse_next_block(FlvDemux* filter, guchar* data, gsize size)
{
    FlvParser* parser = &filter->parser;
    gint  parse_result = FLV_PARSER_OK;
    GstFlowReturn result = GST_FLOW_OK;

    switch (parser->state) {
        case FLV_PARSER_EXPECT_HEADER: {
            FlvHeader header;
            parse_result = flv_parser_read_header(parser, data, size, &header);
            if (parse_result == FLV_PARSER_OK) {
                //fprintf(stderr, "Parsed header. file version : %d, has video : %d, has audio : %d\n",
                //        header.file_version, header.has_video_tags, header.has_audio_tags);
                filter->has_audio = header.has_audio_tags;
                filter->has_video = header.has_video_tags;
            } else {
                //fprintf(stderr, "flv_demux_chain() : Error parsing buffer : %d\n", parse_result);
                result = GST_FLOW_ERROR;
            }
            break;
        }

        case FLV_PARSER_EXPECT_SKIP_BLOCK: {
            parse_result = flv_parser_skip(parser, data, size);
            if (parse_result != FLV_PARSER_OK) {
                //fprintf(stderr, "flv_demux_chain() : Error parsing buffer : %d\n", parse_result);
                result = GST_FLOW_ERROR;
            }
            break;
        }

        case FLV_PARSER_EXPECT_TAG_PREFIX: {
            FlvTagPrefix    tag;
            guint64         file_position = parser->file_position;
            parse_result = flv_parser_read_tag_prefix(parser, data, size, &tag);
            if (parse_result == FLV_PARSER_OK) {
                filter->current_timestamp = (GstClockTime)tag.timestamp * GST_MSECOND;
                filter->last_file_position = file_position;
            } else {
                result = GST_FLOW_ERROR;
            }
            break;
        }

        case FLV_PARSER_EXPECT_VIDEO_TAG_BODY: {
            result = flv_demux_parse_video_tag(filter, data, size);
            break;
        }

        case FLV_PARSER_EXPECT_AUDIO_TAG_BODY: {
            result = flv_demux_parse_audio_tag(filter, data, size);
            break;
        }

        case FLV_PARSER_EXPECT_SCRIPT_DATA_TAG_BODY: {
            FlvScriptDataReader reader;
            parse_result = flv_parser_read_script_data_tag(parser,
                    data, size, &reader);

            if (parse_result == FLV_PARSER_OK && filter->metadata) {
                if (flv_script_data_read(&reader, filter->metadata) &&
                    NULL != filter->metadata->tag_list &&
                    gst_is_tag_list(filter->metadata->tag_list))
                {
                    /* Add discovered tags to queue */
                    if (!filter->queued_tags) {
                        filter->queued_tags = gst_tag_list_copy(filter->metadata->tag_list);
                    } else {
                        GstTagList* temp = NULL;
                        temp = gst_tag_list_merge(filter->queued_tags,
                                filter->metadata->tag_list, GST_TAG_MERGE_REPLACE);
                        gst_tag_list_free(filter->queued_tags);
                        filter->queued_tags = temp;
                    }
                }

                // check if we have a key frame list, if we do copy it over to the main list
                if (!filter->copied_metadata_keyframes && (filter->metadata->keyframes != NULL)) {
                    guint index;
                    GArray *metalist = filter->metadata->keyframes;
                    for (index = 0; index < metalist->len; index++) {
                        FlvKeyframe entry = g_array_index(metalist, FlvKeyframe, index);
                        flv_demux_index_add_entry(filter, entry.time, entry.fileposition);
                    }
                    filter->copied_metadata_keyframes = TRUE;
                }
            } else {
                //fprintf(stderr, "flv_demux_chain() : Error parsing buffer : %d\n", parse_result);
                result = GST_FLOW_ERROR;
            }
            break;
        }
        default: {
            //fprintf(stderr, "flv_demux_chain() : Illegal state\n");
            result = GST_FLOW_ERROR;
            break;
        }
    }

    if (result == GST_FLOW_ERROR) {
        gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DEMUX, g_strdup("Failed to demux FLV stream"), NULL, ("flvdemux.c"), ("flv_demux_parse_next_block"), 0);
    }

    return result;
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn flv_demux_chain (GstPad * pad, GstBuffer * buf)
{
    //fprintf(stderr, "===flv_demux_chain()\n");
    FlvDemux *filter;
    GstAdapter *adapter;
    GstFlowReturn result = GST_FLOW_OK;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (pad));
    adapter = filter->adapter;

    if (filter->need_parser_flush) {
        filter->need_parser_flush = FALSE;
        gst_adapter_clear(adapter);
        if (GST_BUFFER_OFFSET(buf) != 0) {
            flv_parser_seek(&filter->parser, GST_BUFFER_OFFSET(buf));
        } else {
            flv_parser_reset(&filter->parser);
        }
    }

    gst_adapter_push (adapter, buf);

    while(gst_adapter_available(adapter) >= filter->parser.next_block_size) {
        gsize block_size = filter->parser.next_block_size;
        guchar* data = (guchar*)gst_adapter_peek(adapter, (guint)block_size);
        if (data) {
            result = flv_demux_parse_next_block(filter, data, block_size);
            gst_adapter_flush(adapter, (guint)block_size);
            if (result != GST_FLOW_OK)
                break;
        }
    }

    return result;
}

static gboolean flv_demux_do_indexing_pull(FlvDemux* filter,
        GstClockTime indexFrom, GstClockTime indexTo)
{
    FlvParser temp_parser;
    guint64 pos;
    GstClockTime time;
    GstFlowReturn flow_ret;
    FlvParserResult parser_ret;
    GstBuffer* block = NULL;

    /* Init temporary parser */
    flv_parser_init(&temp_parser);

    /* Find last known position and seek parser if necessary */
    if (flv_demux_index_lookup(filter, indexFrom, &time, &pos))
        flv_parser_seek(&temp_parser, pos);

    /* Parse file until beyond required time */
    while (time < indexTo) {
        FlvTagPrefix tag_prefix;

        pos = temp_parser.file_position;
        /* Read additional byte that holds frame info */
        flow_ret = gst_pad_pull_range(filter->sink_pad, temp_parser.file_position,
                (guint)(temp_parser.next_block_size + 1), &block);
        if (flow_ret != GST_FLOW_OK) {
            return FALSE;
        }

        parser_ret = flv_parser_read_tag_prefix(&temp_parser, GST_BUFFER_DATA(block),
                GST_BUFFER_SIZE(block), &tag_prefix);
        if (parser_ret != GST_FLOW_OK) {
// INLINE - gst_buffer_unref()
            gst_buffer_unref(block);
            return FALSE;
        }

        time = tag_prefix.timestamp * GST_MSECOND;

        /* Add index if necessary */
        if (tag_prefix.tag_type == FLV_TAG_TYPE_VIDEO) {
            guint8 frame_info = GST_BUFFER_DATA(block)[temp_parser.parsed_block_size];
            gboolean is_keyframe = ((frame_info & 0xF0) >> 4) == 1;
            if (is_keyframe) {
                flv_demux_index_add_entry(filter, time, pos);
            }
        } else if (tag_prefix.tag_type == FLV_TAG_TYPE_AUDIO && !filter->has_video) {
            flv_demux_index_add_entry(filter, time, pos);
        }
// INLINE - gst_buffer_unref()
        gst_buffer_unref(block);

        /* Seek parser to next tag */
        flv_parser_seek(&temp_parser, temp_parser.file_position + temp_parser.next_block_size);
    }
    return TRUE;
}

static void flv_demux_loop (GstPad * pad)
{
    //fprintf(stderr, "===flv_demux_loop()\n");
    FlvDemux *filter;
    gint result = GST_FLOW_OK;
    GstBuffer* block = NULL;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (pad));

    //fprintf(stderr, "Pulling %d bytes at %d\n", (int)filter->parser.next_block_size,
    //        (int)filter->parser.file_position);
    result = gst_pad_pull_range(pad, filter->parser.file_position,
            (guint)filter->parser.next_block_size, &block);
    if (result == GST_FLOW_OK) {
        result = flv_demux_parse_next_block(filter, GST_BUFFER_DATA(block), GST_BUFFER_SIZE(block));
// INLINE - gst_buffer_unref()
        gst_buffer_unref(block);

        //if (filter->segment.last_stop >= filter->segment.stop)
        //    result = GST_FLOW_UNEXPECTED;

        if (result == GST_FLOW_OK)
            return;
    }

    //Something went wrong
    //const gchar *reason = gst_flow_get_name(result);
    //fprintf(stderr, "pausing task, reason : %s (%d)\n", reason, result);

    gst_pad_pause_task(pad);

    if (result == GST_FLOW_UNEXPECTED) {
        flv_demux_push_src_event(filter, gst_event_new_eos ());
    } else if (result == GST_FLOW_ERROR) {
        gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DEMUX, 
            g_strdup("Failed to demux FLV stream"), NULL, ("flvdemux.c"), ("flv_demux_loop"), 0);
    }
}

/* this function gets called when we activate ourselves in pull mode.
 * We can perform  random access to the resource and we start a task
 * to start reading */
static gboolean
flv_demux_sink_activate_pull (GstPad * sinkpad, gboolean active)
{
    //fprintf(stderr, "===flv_demux_sink_activate_pull(%d)\n", active);
    FlvDemux *filter;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (sinkpad));
    filter->is_pulling = TRUE;

    if (active) {
        return gst_pad_start_task (sinkpad, (GstTaskFunction) flv_demux_loop,
                sinkpad);
    } else {
        //filter->random_access = FALSE;
        return gst_pad_stop_task (sinkpad);
    }
}

/* This function gets called when we activate ourselves in push mode.
 * We cannot seek (ourselves) in the stream */
static gboolean
flv_demux_sink_activate_push (GstPad * sinkpad, gboolean active)
{
    //fprintf(stderr, "===flv_demux_sink_activate_push() : %d\n", active);
    FlvDemux *filter;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (sinkpad));

    filter->is_pulling = FALSE;

    return TRUE;
}

/* If we can pull that's prefered */
static gboolean
flv_demux_sink_activate (GstPad * sinkpad)
{
    //fprintf(stderr, "===flv_demux_sink_activate()\n");
    if (gst_pad_check_pull_range (sinkpad)) {
    //if (FALSE) {
        return gst_pad_activate_pull (sinkpad, TRUE);
    } else {
        return gst_pad_activate_push (sinkpad, TRUE);
    }
}

static const GstQueryType *
flv_demux_src_query_types (GstPad * pad)
{
  static const GstQueryType query_types[] = {
    GST_QUERY_DURATION,
    //GST_QUERY_POSITION,
    //GST_QUERY_LATENCY,
    GST_QUERY_NONE
  };

  return query_types;
}

static gboolean
flv_demux_src_query (GstPad * pad, GstQuery * query)
{
    //fprintf(stderr, "===flv_demux_src_query: (%s)\n", gst_query_type_get_name(GST_QUERY_TYPE(query)));
    gboolean res = TRUE;
    FlvDemux *filter;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (pad));

    switch (GST_QUERY_TYPE(query)) {
        case GST_QUERY_DURATION:
        {
            GstFormat format;

            gst_query_parse_duration(query, &format, NULL);
            /* duration is time only */
            if (format != GST_FORMAT_TIME || !filter->metadata) {
                res = gst_pad_query_default(pad, query);
                break;
            }
            //fprintf(stderr, "===flv_demux_src_query: returning duration %ld\n",  (long int)(filter->metadata->duration / 1000000));
            gst_query_set_duration(query, GST_FORMAT_TIME, filter->metadata->duration);
            break;
        }
        /*case GST_QUERY_POSITION:
        {
            //GstPad* peer = gst_pad_get_peer(filter->sinkpad);
            //res = gst_pad_query(peer, query);
            GstFormat format;

            gst_query_parse_position(query, &format, NULL);
            if (format != GST_FORMAT_TIME) {
                res = FALSE;
                break;
            }
            fprintf(stderr, "===flv_demux_src_query: returning position %ld\n",  (long int)(filter->segment.last_stop / 1000000));
            gst_query_set_position(query, GST_FORMAT_TIME, filter->segment.last_stop);
            break;
        }
         */

        case GST_QUERY_LATENCY:
        default:
        {
            GstPad *peer;

            if ((peer = gst_pad_get_peer(filter->sink_pad))) {
                /* query latency on peer pad */
                res = gst_pad_query(peer, query);
                gst_object_unref(peer);
            } else {
                /* no peer, we don't know */
                res = FALSE;
            }
            break;
        }
    }

    return res;
}

static const GstQueryType*
flv_demux_sink_query_types (GstPad* pad)
{
  static const GstQueryType query_types[] = {
    GST_QUERY_CUSTOM,
    GST_QUERY_NONE
  };

  return query_types;
}

static gboolean
flv_demux_sink_query (GstPad* pad, GstQuery* query)
{
    gboolean result = TRUE;
    switch (GST_QUERY_TYPE(query))
    {
        case GST_QUERY_CUSTOM:
        {
            GstStructure *s = gst_query_get_structure(query);
            if (gst_structure_has_name(s, GETRANGE_QUERY_NAME))
                gst_structure_set(s, GETRANGE_QUERY_SUPPORTS_FIELDNANE,
                                     GETRANGE_QUERY_SUPPORTS_FIELDTYPE,
                                     TRUE,
                                     NULL);
            break;
        }
        default:
            result = gst_pad_query_default(pad, query);
            break;
    }
    return result;
}

static gboolean
flv_demux_seek_pull(FlvDemux* filter, GstEvent* event)
{
    GstFormat format;
    GstSeekFlags flags;
    GstSeekType start_type, stop_type;
    gint64 start, stop;
    gdouble rate;
    gboolean update, flush, keyframe, res = TRUE;
    GstSegment seeksegment;

    gst_event_parse_seek(event, &rate, &format, &flags,
            &start_type, &start, &stop_type, &stop);
#if JFXMEDIA_DEBUG
    fprintf(stderr, "FLV: seek pull : start_type %d, start %lld, stop_type %d, stop %lld\n", start_type, start, stop_type, stop);
#endif
    // Non-time format and negative playback rate are not supported.
    if (format != GST_FORMAT_TIME || rate <= 0) {
        return FALSE;
    }

    flush = flags & GST_SEEK_FLAG_FLUSH;
    keyframe = flags & GST_SEEK_FLAG_KEY_UNIT;

    if (flush) {
        /* Flush start up and downstream to make sure data flow and loops are
           idle */
        //fprintf(stderr, "===flv_demux_seek_pull() : FLUSH\n");
        // Upstream
        gst_pad_push_event (filter->sink_pad, gst_event_new_flush_start ());
        // Downstream
        flv_demux_push_src_event(filter, gst_event_new_flush_start ());
    } else {
        //fprintf(stderr, "===flv_demux_seek_pull() : PAUSE\n");
        /* Pause the pulling task */
        //gst_pad_pause_task (filter->sink_pad);
    }

    /* Take the stream lock */
    GST_PAD_STREAM_LOCK (filter->sink_pad);

    if (flush) {
        /* Stop flushing upstream we need to pull */
        gst_pad_push_event (filter->sink_pad, gst_event_new_flush_stop ());
    }

    /* Work on a copy until we are sure the seek succeeded. */
    memcpy (&seeksegment, &filter->segment, sizeof (GstSegment));

    //fprintf(stderr, "Segment before : %" GST_SEGMENT_FORMAT "\n", &seeksegment);
    /* Apply the seek to our segment */
    gst_segment_set_seek (&seeksegment, rate, format, flags,
            start_type, start, stop_type, stop, &update);
    //fprintf(stderr, "Segment after : %" GST_SEGMENT_FORMAT "\n", &seeksegment);

    if (flush || seeksegment.last_stop != filter->segment.last_stop) {
        /* Do the actual seeking */
        GstClockTime time;
        guint64 pos;

        /* Find nearest keyframe. If there are no index entries yet,
         * parsing will start from the beginning of file */
        if (!flv_demux_index_lookup(filter, seeksegment.start, &time, &pos)) {
            time = 0;
            pos = 0;
        }

        /* Index file up to seek position if it's not indexed yet. */
        if ((seeksegment.start - time) > 5 * GST_SECOND) {
            flv_demux_do_indexing_pull(filter, time, seeksegment.start);
            flv_demux_index_lookup(filter, seeksegment.start, &time, &pos);
        }

        /* Seek parser to keyframe if there's one found.
         * Otherwise reset it to the beginning of file. */
        if (pos != 0) {
            flv_parser_seek(&filter->parser, pos);
        } else {
            flv_parser_reset(&filter->parser);
        }

        /* Adjust segment if it's keyframe seek. */
        if (keyframe) {
            if (time < (GstClockTime)seeksegment.start) {
                seeksegment.start = time;
            }
            seeksegment.last_stop = time;
        }
    }

    if (flush) {
        // Downstream
        flv_demux_push_src_event(filter, gst_event_new_flush_stop ());
    }
    if (res) {
        /* Ok seek succeeded, take the newly configured segment */
        //fprintf(stderr, "Segment final : %" GST_SEGMENT_FORMAT "\n", &seeksegment);
        memcpy (&filter->segment, &seeksegment, sizeof (GstSegment));
        if (filter->segment.flags & GST_SEEK_FLAG_SEGMENT) {
            gst_element_post_message (GST_ELEMENT (filter),
                    gst_message_new_segment_start (GST_OBJECT (filter),
                    filter->segment.format, filter->segment.last_stop));
        }
        filter->audio_discont = TRUE;
        filter->video_discont = TRUE;
    }

    gst_pad_start_task (filter->sink_pad,
            (GstTaskFunction)flv_demux_loop, filter->sink_pad);

    GST_PAD_STREAM_UNLOCK (filter->sink_pad);

    return res;
}

static gboolean
flv_demux_seek_push(FlvDemux* filter, GstEvent* event)
{
    GstFormat format;
    GstSeekFlags flags;
    GstSeekType start_type, stop_type;
    gint64 start, stop;
    gdouble rate;
    gboolean update, flush, keyframe, res = TRUE;
    GstSegment seeksegment;

    gst_event_parse_seek(event, &rate, &format, &flags,
            &start_type, &start, &stop_type, &stop);

    // Non-time format and negative playback rate are not supported.
    if (format != GST_FORMAT_TIME || rate <= 0) {
        return FALSE;
    }

    flush = flags & GST_SEEK_FLAG_FLUSH;
    keyframe = flags & GST_SEEK_FLAG_KEY_UNIT;

    /* Work on a copy until we are sure the seek succeeded. */
    memcpy (&seeksegment, &filter->segment, sizeof (GstSegment));

    //fprintf(stderr, "Segment before : %" GST_SEGMENT_FORMAT "\n", &seeksegment);
    /* Apply the seek to our segment */
    gst_segment_set_seek (&seeksegment, rate, format, flags,
            start_type, start, stop_type, stop, &update);
    //fprintf(stderr, "Segment after : %" GST_SEGMENT_FORMAT "\n", &seeksegment);

    if (flush || seeksegment.last_stop != filter->segment.last_stop) {
        /* Do the actual seeking */
        GstClockTime time;
        guint64 pos;

        /* Find nearest keyframe. If there are no index entries yet,
         * parsing will start from the beginning of file */
        if (!flv_demux_index_lookup(filter, seeksegment.start, &time, &pos)) {
            time = 0;
            pos = 0;
        }

        /* Adjust segment if it's keyframe seek. */
        if (keyframe) {
            if (time < (GstClockTime)seeksegment.start) {
                seeksegment.start = time;
            }
            seeksegment.last_stop = time;
        }

        filter->need_parser_flush = TRUE;

        /* Push seek event adjusted to bytes upstream. */
        res = gst_pad_push_event (filter->sink_pad,
            gst_event_new_seek (seeksegment.rate, GST_FORMAT_BYTES,
                GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE,
                GST_SEEK_TYPE_SET, pos,
                GST_SEEK_TYPE_NONE, 0));
    }

    if (res) {
        /* Ok seek succeeded, take the newly configured segment */
        //fprintf(stderr, "Segment final : %" GST_SEGMENT_FORMAT "\n", &seeksegment);
        memcpy (&filter->segment, &seeksegment, sizeof (GstSegment));
        if (filter->segment.flags & GST_SEEK_FLAG_SEGMENT) {
            gst_element_post_message (GST_ELEMENT (filter),
                    gst_message_new_segment_start (GST_OBJECT (filter),
                    filter->segment.format, filter->segment.last_stop));
        }
        filter->audio_discont = TRUE;
        filter->video_discont = TRUE;
    }

    return res;
}

static gboolean flv_demux_src_event (GstPad * pad, GstEvent * event)
{
    //fprintf(stderr, "===flv_demux_src_event() : %s\n", gst_event_type_get_name(GST_EVENT_TYPE(event)));
    gboolean res = TRUE;
    FlvDemux *filter;

    filter = FLV_DEMUX (GST_OBJECT_PARENT (pad));

    switch (GST_EVENT_TYPE (event)) {
        case GST_EVENT_SEEK:
            if (filter->is_pulling) {
                res = flv_demux_seek_pull(filter, event);
            } else {
                res = flv_demux_seek_push(filter, event);
            }
// INLINE - gst_event_unref()
            gst_event_unref (event);
            break;
        default:
            res = gst_pad_push_event (filter->sink_pad, event);
            break;
    }

    return res;
}

static gint
compare_keyframes(FlvKeyframe *first, FlvKeyframe *second)
{
    if (first->time < second->time) {
        return -1;
    }
    if (first->time == second->time) {
        return 0;
    }
    return 1;
}

static void
flv_demux_index_add_entry(FlvDemux* filter, GstClockTime index_time, guint64 index_pos)
{
    FlvKeyframe frame;
    GList *foundEntry;

    frame.time = index_time;
    frame.fileposition = index_pos;

    // OK if keyframes is NULL, it'll just return NULL
    foundEntry = g_list_find_custom(filter->keyframes, &frame, (GCompareFunc)compare_keyframes);
    if (foundEntry == NULL) {
        // not a duplicate, insert it into the list
#if JFXMEDIA_DEBUG
        fprintf(stderr, " Inserting key frame: time %lld\n", (long long)GST_TIME_AS_MSECONDS(index_time));
#endif
        filter->keyframes = g_list_insert_sorted(filter->keyframes, g_slice_copy(sizeof(FlvKeyframe), &frame), (GCompareFunc)compare_keyframes);
    }
}

static gboolean
flv_demux_index_lookup(FlvDemux* filter,
        GstClockTime time, GstClockTime *index_time, guint64 *index_pos)
{
    FlvKeyframe *frame = NULL;
    GList *keyframes = g_list_first(filter->keyframes);

    if (!keyframes) {
        return FALSE;
    }

    // check endpoints
    if (((FlvKeyframe*)g_list_first(keyframes)->data)->time > time) {
        return FALSE;
    }
    if (((FlvKeyframe*)g_list_last(keyframes)->data)->time < time) {
        return FALSE;
    }

    // I don't think a binary search really buys us anything here, maybe if the table was 10x the size, but these tend to be pretty small
    while (keyframes != NULL) {
        FlvKeyframe *entry = (FlvKeyframe*)keyframes->data;
        if (entry->time == time) {
            frame = entry;
            break;
        } else if (entry->time > time) {
            // went over, previous entry will have to suffice
            frame = (FlvKeyframe*)keyframes->prev->data;
            break;
        }
        keyframes = g_list_next(keyframes);
    }

    if (frame) {
#if JFXMEDIA_DEBUG
        fprintf(stderr, "FLV: Seek index (%0.2f): %0.2fs, %lld\n", (double)GST_TIME_AS_SECONDS(time), (double)GST_TIME_AS_SECONDS(frame->time), (unsigned long long)frame->fileposition);
#endif
        *index_time = frame->time;
        *index_pos = frame->fileposition;
        return TRUE;
    }
    return FALSE;
}
