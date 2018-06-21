/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "mpegtsdemuxer.h"
#include <libavcodec/avcodec.h>

#define ENABLE_VIDEO
//#define DEBUG_OUTPUT
//#define VERBOSE_DEBUG_AUDIO
//#define VERBOSE_DEBUG_VIDEO
//#define FAKE_ERROR
/***********************************************************************************/
static const int NO_STREAM = -1;

typedef struct
{
    GstPad            *sourcepad;
    volatile gboolean discont;
    GstSegment        segment;

/* needed because 33bit mpeg timestamps wrap around every (approx) 26.5 hrs */
    GstClockTime      last_time;
    GstClockTime      offset_time;

    int               stream_index;
    CodecIDType       codec_id;
} Stream;

typedef enum
{
    UNLIMITED,
    LIMITED
} LimitType;

struct _MpegTSDemuxer
{
    AVElement         parent;

    GstPad            *sinkpad;
    GstAdapter        *sink_adapter;
    guint             offset;
    gboolean          flush_adapter;
    GstFlowReturn     sink_result;

    gint64            adapter_limit_size;
    LimitType         adapter_limit_type;

    Stream            video;
    Stream            audio;

    volatile gboolean is_eos;
    volatile gboolean is_last_buffer_send;
    volatile gboolean is_reading;
    volatile gboolean is_flushing;
    volatile gboolean is_closing;
    gboolean          update;

    AVFormatContext   *context;

    GThread           *reader_thread;
    GMutex            lock;
    GCond             add_cond;
    GCond             del_cond;

    gint              numpads;

#ifdef FAKE_ERROR
    gint              read_bytes;
#endif // FAKE_ERROR

    GstClockTime      base_pts;
};

struct _MpegTSDemuxerClass
{
    AVElementClass parent_class;

    GstPadTemplate *audio_source_template;
    GstPadTemplate *video_source_template;
};


/***********************************************************************************
 * Time format conversion macros
 **********************************************************************************/
#define CLOCK_BASE           9LL
#define PTS_TO_GSTTIME(time) ((gint64)(gst_util_uint64_scale ((time), GST_MSECOND/10, CLOCK_BASE)))
#define MAX_PTS              ((guint64)G_MAXUINT64 >> 31)

/***********************************************************************************/

#define BUFFER_SIZE   4096             // Bytes. Better take it from JavaSource.
#define ADAPTER_LIMIT 40 * BUFFER_SIZE // Initial adapter limit. It grows if unlimited by adding LIMIT_STEP
#define LIMIT_STEP    10 * BUFFER_SIZE

/***********************************************************************************
 * Debug category and pad templates
 ***********************************************************************************/
GST_DEBUG_CATEGORY_STATIC(mpegts_demuxer_debug);
#define GST_CAT_DEFAULT mpegts_demuxer_debug

/*
 * The input capabilities.
 */
#define SINK_CAPS "video/MP2T"

static GstStaticPadTemplate sink_template =
    GST_STATIC_PAD_TEMPLATE ("sink",
                             GST_PAD_SINK,
                             GST_PAD_ALWAYS,
                             GST_STATIC_CAPS (SINK_CAPS));

/*
 * The output capabilities.
 */
static GstStaticPadTemplate audio_source_template =
    GST_STATIC_PAD_TEMPLATE("audio%02d",
                            GST_PAD_SRC,
                            GST_PAD_SOMETIMES,
                            GST_STATIC_CAPS("audio/mpeg, "
                                            "mpegversion = (int) {1, 4}"));

static GstStaticPadTemplate video_source_template =
    GST_STATIC_PAD_TEMPLATE("video%02d",
                            GST_PAD_SRC,
                            GST_PAD_SOMETIMES,
                            GST_STATIC_CAPS("video/x-h264"));

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE(MpegTSDemuxer, mpegts_demuxer, AVElement, TYPE_AVELEMENT);
 ***********************************************************************************/
#define mpegts_demuxer_parent_class parent_class
static void mpegts_demuxer_init          (MpegTSDemuxer      *self);
static void mpegts_demuxer_class_init    (MpegTSDemuxerClass *klass);
static gpointer mpegts_demuxer_parent_class = NULL;
static void     mpegts_demuxer_class_intern_init (gpointer klass)
{
    mpegts_demuxer_parent_class = g_type_class_peek_parent (klass);
    mpegts_demuxer_class_init ((MpegTSDemuxerClass*) klass);
}

GType mpegts_demuxer_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (TYPE_AVELEMENT,
               g_intern_static_string ("MpegTSDemuxer"),
               sizeof (MpegTSDemuxerClass),
               (GClassInitFunc) mpegts_demuxer_class_intern_init,
               sizeof(MpegTSDemuxer),
               (GInstanceInitFunc) mpegts_demuxer_init,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
 * Calss and instance init and forward declarations
 ***********************************************************************************/
static GstStateChangeReturn mpegts_demuxer_change_state(GstElement* element, GstStateChange transition);
static gboolean             mpegts_demuxer_sink_event(GstPad *pad, GstObject *parent, GstEvent *event);
static gboolean             mpegts_demuxer_sink_query (GstPad *pad, GstObject *parent, GstQuery *query);
static GstFlowReturn        mpegts_demuxer_chain(GstPad *pad, GstObject *parent, GstBuffer *buf);
static gboolean             mpegts_demuxer_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active);
static void                 mpegts_demuxer_finalize(GObject *object);
static gpointer             mpegts_demuxer_process_input(gpointer data);

static gboolean             mpegts_demuxer_src_query (GstPad *pad, GstObject *parent, GstQuery *query);
static void                 mpegts_demuxer_init_state(MpegTSDemuxer *demuxer);
static void                 mpegts_demuxer_close(MpegTSDemuxer *demuxer);
//static void                 mpegts_demuxer_state_reset(MpegTSDemuxer *decoder);
static void                 mpegts_demuxer_flush(MpegTSDemuxer *demuxer);
static gboolean             mpegts_demuxer_src_event(GstPad *pad, GstObject *parent, GstEvent *event);

static int                  mpegts_demuxer_read_packet(void *demuxer, uint8_t *buf, int buf_size);
static int64_t              mpegts_demuxer_seek(void *opaque, int64_t offset, int whence);

static void mpegts_demuxer_class_init(MpegTSDemuxerClass *g_class)
{
    GstElementClass *gstelement_class = GST_ELEMENT_CLASS(g_class);

    g_class->audio_source_template = gst_static_pad_template_get (&audio_source_template);
    g_class->video_source_template = gst_static_pad_template_get (&video_source_template);

    gst_element_class_add_pad_template(gstelement_class, g_class->audio_source_template);
    gst_element_class_add_pad_template(gstelement_class, g_class->video_source_template);
    gst_element_class_add_pad_template(gstelement_class, gst_static_pad_template_get (&sink_template));

    gst_element_class_set_metadata(gstelement_class,
                "MPEG2 transport stream parser",
                "Codec/Parser",
                "Parses MPEG2 transport streams",
                "Oracle Corporation");

    G_OBJECT_CLASS (g_class)->finalize = GST_DEBUG_FUNCPTR(mpegts_demuxer_finalize);
    gstelement_class->change_state = mpegts_demuxer_change_state;

    av_register_all();
}

static void mpegts_demuxer_init(MpegTSDemuxer *demuxer)
{
    // Input.
    demuxer->sinkpad = gst_pad_new_from_static_template(&sink_template, "sink");
    gst_pad_set_chain_function(demuxer->sinkpad, GST_DEBUG_FUNCPTR(mpegts_demuxer_chain));
    gst_pad_set_query_function (demuxer->sinkpad, GST_DEBUG_FUNCPTR(mpegts_demuxer_sink_query));
    gst_pad_set_event_function(demuxer->sinkpad, GST_DEBUG_FUNCPTR(mpegts_demuxer_sink_event));
    gst_pad_set_activatemode_function(demuxer->sinkpad, GST_DEBUG_FUNCPTR(mpegts_demuxer_activatemode));
    gst_element_add_pad(GST_ELEMENT(demuxer), demuxer->sinkpad);

    g_mutex_init(&demuxer->lock);
    g_cond_init(&demuxer->add_cond);
    g_cond_init(&demuxer->del_cond);
    demuxer->sink_adapter = gst_adapter_new();
    demuxer->reader_thread = NULL;
    demuxer->numpads = 0;
    demuxer->base_pts = GST_CLOCK_TIME_NONE;
}

static void mpegts_demuxer_finalize(GObject *object)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(object);

    g_mutex_clear(&demuxer->lock);
    g_cond_clear(&demuxer->add_cond);
    g_cond_clear(&demuxer->del_cond);
    g_object_unref(demuxer->sink_adapter);

    G_OBJECT_CLASS (parent_class)->finalize (object);
}

static inline void post_error(MpegTSDemuxer *demuxer, const char* description, int result, int code)
{
    char* error_string = g_strdup_printf("%s: %d (%s)", description, result,
                                         avelement_error_to_string(AVELEMENT(demuxer), result));

#ifdef DEBUG_OUTPUT
    g_print ("MpegTS post_error: %s\n", error_string);
#endif

    gst_element_message_full(GST_ELEMENT(demuxer), GST_MESSAGE_ERROR, GST_STREAM_ERROR, code,
                             error_string, NULL, ("mpegtsdemuxer.c"), ("mpegts_demuxer_error"), 0);
}

static inline void post_message(MpegTSDemuxer *demuxer, const char* message,
                                GstMessageType type, GQuark domain, int code)
{
#ifdef DEBUG_OUTPUT
    g_print ("MpegTS post_message: %s\n", message);
#endif
    gst_element_message_full(GST_ELEMENT(demuxer), type, domain, code,
                             g_strdup(message), NULL, ("mpegtsdemuxer.c"), ("mpegts_demuxer_message"), 0);
}

static inline void post_unsupported_warning(MpegTSDemuxer *demuxer)
{
    post_message(demuxer, "Unsupported stream type", GST_MESSAGE_WARNING, GST_STREAM_ERROR, GST_STREAM_ERROR_NOT_IMPLEMENTED);
}

/***********************************************************************************
 * Chain and activatepush
 ***********************************************************************************/
static gboolean mpegts_demuxer_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active)
{
    gboolean res = FALSE;
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);

#ifdef DEBUG_OUTPUT
    g_print("MpegTS activate_push: %s\n", active ? "ACTIVATE" : "DEACTIVATE");
#endif

    switch (mode) {
        case GST_PAD_MODE_PUSH:
            if (active)
            {
                g_mutex_lock(&demuxer->lock);
                demuxer->sink_result = GST_FLOW_OK;
                g_mutex_unlock(&demuxer->lock);
            }
            else
            {
                g_mutex_lock(&demuxer->lock);
                demuxer->sink_result = GST_FLOW_FLUSHING;
                g_cond_signal(&demuxer->del_cond);
                g_mutex_unlock(&demuxer->lock);
            }
            res = TRUE;

            break;
        case GST_PAD_MODE_PULL:
            res = TRUE;
            break;
        default:
            /* unknown scheduling mode */
            res = FALSE;
            break;
    }

  return res;
}

static GstFlowReturn get_locked_result(MpegTSDemuxer *demuxer)
{
    if (demuxer->is_flushing)
        return GST_FLOW_FLUSHING;

    if (demuxer->is_eos)
        return GST_FLOW_EOS;

    return demuxer->sink_result;
}

static GstFlowReturn mpegts_demuxer_chain(GstPad *pad, GstObject *parent, GstBuffer *buf)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);

    g_mutex_lock(&demuxer->lock);

    GstFlowReturn result = get_locked_result(demuxer);
    while (((gint64)gst_adapter_available(demuxer->sink_adapter) + gst_buffer_get_size(buf)) >= demuxer->adapter_limit_size &&
           result == GST_FLOW_OK)
    {
        g_cond_wait(&demuxer->del_cond, &demuxer->lock);
        result = get_locked_result(demuxer);
    }

    if (result == GST_FLOW_OK)
    {
        gst_adapter_push(demuxer->sink_adapter, buf);
        g_cond_signal(&demuxer->add_cond);
    }
    else
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(buf);
    }

    g_mutex_unlock(&demuxer->lock);

    return result;
}

/***********************************************************************************
 * Sink
 ***********************************************************************************/
static gboolean mpegts_demuxer_push_to_sources(MpegTSDemuxer *demuxer, GstEvent *event)
{
    gboolean ret = TRUE;

    if (demuxer->audio.sourcepad)
        ret &= gst_pad_push_event (demuxer->audio.sourcepad, gst_event_ref (event)); // INLINE - gst_event_ref()

    if (demuxer->video.sourcepad)
        ret &= gst_pad_push_event (demuxer->video.sourcepad, gst_event_ref (event)); // INLINE - gst_event_ref()

    gst_event_unref (event); // INLINE - gst_event_unref()

    return ret;
}

static gboolean mpegts_demuxer_sink_event(GstPad *pad, GstObject *parent, GstEvent *event)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);
    gboolean      result = TRUE;

    switch (GST_EVENT_TYPE(event))
    {
        case GST_EVENT_EOS:
#ifdef DEBUG_OUTPUT
            g_print("MpegTS sinkEvent: EOS\n");
#endif
            g_mutex_lock(&demuxer->lock);
            demuxer->is_eos = TRUE;
            g_cond_signal(&demuxer->add_cond); // Signal read callback to read up remaining data.
            g_mutex_unlock(&demuxer->lock);

            gst_event_unref(event);
            break;

        case GST_EVENT_FLUSH_START:
#ifdef DEBUG_OUTPUT
            g_print("MpegTS sinkEvent: FLUSH_START ...");
#endif
            result = gst_pad_event_default(demuxer->sinkpad, parent, event);

            g_mutex_lock(&demuxer->lock);
            demuxer->is_flushing = TRUE;
            g_cond_signal(&demuxer->del_cond); // Signal _chain() and read callback to wake them up and update state.
            g_cond_signal(&demuxer->add_cond);
            g_mutex_unlock(&demuxer->lock);

            if (demuxer->reader_thread)
            {
                g_thread_join(demuxer->reader_thread);
                demuxer->reader_thread = NULL;
            }

#ifdef DEBUG_OUTPUT
            g_print("done.\n");
#endif
            break;

        case GST_EVENT_FLUSH_STOP: // Stop flushing buffers.
            g_mutex_lock(&demuxer->lock);
            mpegts_demuxer_flush(demuxer);
            demuxer->is_flushing = FALSE; // Unset flag so chain function accepts buffers.
            g_mutex_unlock(&demuxer->lock);
            result = gst_pad_event_default(demuxer->sinkpad, parent, event);

#ifdef DEBUG_OUTPUT
            g_print("MpegTS sinkEvent: FLUSH_STOP\n");
#endif
            break;

        case GST_EVENT_SEGMENT:
        {
            GstSegment segment;
            gst_event_copy_segment(event, &segment);
            gst_event_unref(event);

            g_mutex_lock(&demuxer->lock);
            if (!demuxer->is_closing)
            {
#ifdef DEBUG_OUTPUT
                g_print("MpegTS sinkEvent: NEW_SEGMENT, time=%.2f\n",
                        time != GST_CLOCK_TIME_NONE ? (double)time/GST_SECOND : -1.0);
#endif
                if (segment.format == GST_FORMAT_TIME)
                {
                    gst_segment_copy_into(&segment, &demuxer->audio.segment);
                    gst_segment_copy_into(&segment, &demuxer->video.segment);
                }

                demuxer->audio.discont = demuxer->video.discont = TRUE;
                demuxer->is_eos = FALSE;
                demuxer->is_last_buffer_send = FALSE;
                demuxer->is_reading = TRUE;

                if (!demuxer->reader_thread)
                {
                    demuxer->reader_thread = g_thread_new(NULL, mpegts_demuxer_process_input, demuxer);
#ifdef DEBUG_OUTPUT
                    g_print("MpegTS: Process_input thread created\n");
#endif
                }
                else
                    post_message(demuxer, "Demuxer thread is not null", GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_THREAD);
            }
            g_mutex_unlock(&demuxer->lock);
            break;
        }

        default:
            result = mpegts_demuxer_push_to_sources(demuxer, event);
            break;
    }

    return result;
}

static gboolean mpegts_demuxer_src_event(GstPad *pad, GstObject *parent, GstEvent *event)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);
    return gst_pad_push_event (demuxer->sinkpad, event);
}

/***********************************************************************************
 * Process input stuff
 ***********************************************************************************/
static inline gboolean mpegts_demuxer_expect_more_pads(MpegTSDemuxer *demuxer)
{
    return demuxer->numpads < demuxer->context->nb_streams;
}

static void mpegts_demuxer_add_pad(MpegTSDemuxer *demuxer, GstPad *pad, GstCaps *caps)
{
    GstEvent *caps_event = NULL;
    gst_pad_set_query_function (pad, mpegts_demuxer_src_query);
    gst_pad_set_event_function (pad, mpegts_demuxer_src_event);
    gst_pad_set_active(pad, TRUE);
    gst_pad_use_fixed_caps (pad);
    caps_event = gst_event_new_caps(caps);
    if (caps_event)
        gst_pad_push_event (pad, caps_event);
    gst_caps_unref (caps);
    gst_element_add_pad(GST_ELEMENT(demuxer), pad);
}

static GstBuffer* get_codec_extradata(AVCodecContext *codec)
{
    GstBuffer *codec_data = NULL;
    if (codec->extradata)
    {
        codec_data = gst_buffer_new_allocate(NULL, codec->extradata_size, NULL);
        if (codec_data != NULL)
        {
            gst_buffer_fill(codec_data, 0, codec->extradata, codec->extradata_size);
        }
    }

    return codec_data;
}

static void mpegts_demuxer_check_streams(MpegTSDemuxer *demuxer)
{
    MpegTSDemuxerClass *demuxer_class = MPEGTS_DEMUXER_GET_CLASS(demuxer);
    int i;
    for (i = 0; i < demuxer->context->nb_streams; i++)
    {
        switch (demuxer->context->streams[i]->codec->codec_type)
        {
            case AVMEDIA_TYPE_VIDEO:

                if (demuxer->video.stream_index < 0)
                {
                    AVStream *stream = demuxer->context->streams[i];
#if NEW_CODEC_ID
                    if (stream->codec->codec_id == AV_CODEC_ID_H264)
#else
                    if (stream->codec->codec_id == CODEC_ID_H264)
#endif
                    {
                        demuxer->video.stream_index = i;
                        demuxer->video.codec_id = stream->codec->codec_id;

#ifdef ENABLE_VIDEO
                        gchar *name = g_strdup_printf ("video%02d", i);
                        GstCaps *caps = gst_caps_new_simple ("video/x-h264",
                                                             "hls", G_TYPE_BOOLEAN, TRUE, NULL);

                        GstBuffer *codec_data = get_codec_extradata(stream->codec);
                        if (codec_data)
                            gst_caps_set_simple(caps, "codec_data", GST_TYPE_BUFFER, codec_data, NULL);

                        demuxer->video.sourcepad = gst_pad_new_from_template (demuxer_class->video_source_template, name);
                        mpegts_demuxer_add_pad(demuxer, demuxer->video.sourcepad, caps);
                        g_free(name);
#endif // ENABLE_VIDEO
                        demuxer->numpads++;
                    }
                }
                break;

            case AVMEDIA_TYPE_AUDIO:
                if (demuxer->audio.stream_index < 0)
                {
                    AVStream *stream = demuxer->context->streams[i];
#if NEW_CODEC_ID
                    if (stream->codec->codec_id == AV_CODEC_ID_AAC)
#else
                    if (stream->codec->codec_id == CODEC_ID_AAC)
#endif
                    {
                        demuxer->audio.stream_index = i;
                        demuxer->audio.codec_id = stream->codec->codec_id;

                        gchar *name = g_strdup_printf ("audio%02d", i);
                        GstCaps *caps = gst_caps_new_simple ("audio/mpeg",
                                                    "mpegversion", G_TYPE_INT, 4,
                                                    "channels", G_TYPE_INT, stream->codec->channels,
                                                    "rate", G_TYPE_INT, stream->codec->sample_rate,
                                                    "bitrate", G_TYPE_INT, stream->codec->bit_rate,
                                                    "hls", G_TYPE_BOOLEAN, TRUE, NULL);

                        GstBuffer *codec_data = get_codec_extradata(stream->codec);
                        if (codec_data)
                            gst_caps_set_simple(caps, "codec_data", GST_TYPE_BUFFER, codec_data, NULL);

                        demuxer->audio.sourcepad = gst_pad_new_from_template (demuxer_class->audio_source_template, name);
                        mpegts_demuxer_add_pad(demuxer, demuxer->audio.sourcepad, caps);
                        g_free(name);

                        demuxer->numpads++;
                    }
                }
                break;

            default:
                break;
        }
    }

    if (!mpegts_demuxer_expect_more_pads(demuxer))
    {
        gst_element_no_more_pads(GST_ELEMENT(demuxer));
#ifdef DEBUG_OUTPUT
        g_print("MpegTS: All pads added, no more pads\n");
#endif
    }
}

/***********************************************************************************
 * Push functions
 ***********************************************************************************/
static inline GstBuffer* packet_to_buffer(AVPacket *packet)
{
    GstBuffer* result = gst_buffer_new_allocate(NULL, packet->size, NULL);
    if (result != NULL)
        gst_buffer_fill(result, 0, packet->data, packet->size);
    return result;
}

static inline gboolean same_stream(MpegTSDemuxer *demuxer, Stream *stream, AVPacket *packet)
{
    return demuxer->context->streams[packet->stream_index]->codec->codec_id == stream->codec_id;
}

static GstFlowReturn process_video_packet(MpegTSDemuxer *demuxer, AVPacket *packet)
{
    GstFlowReturn result = GST_FLOW_OK;
    Stream        *stream = &demuxer->video;

    if (!same_stream(demuxer, stream, packet))
        return result;

    GstBuffer     *buffer = NULL;

    GstEvent *newsegment_event = NULL;
    void *buffer_data = av_mallocz(packet->size);
    if (buffer_data != NULL)
    {
        memcpy(buffer_data, packet->data, packet->size);
        buffer = gst_buffer_new_wrapped_full(0, buffer_data, packet->size, 0, packet->size, buffer_data, &av_free);

        if (packet->pts != AV_NOPTS_VALUE)
        {
            if (demuxer->base_pts == GST_CLOCK_TIME_NONE)
            {
                demuxer->base_pts = PTS_TO_GSTTIME(packet->pts) + stream->offset_time;
            }

            gint64 time = PTS_TO_GSTTIME(packet->pts) + stream->offset_time - demuxer->base_pts;
            if (time < 0)
                time = 0;

            if (stream->last_time > 0 && time < (gint64) (stream->last_time - PTS_TO_GSTTIME(G_MAXUINT32)))
            {
                gint64 diff = PTS_TO_GSTTIME(MAX_PTS + 1); // Wraparound occured

#ifdef VERBOSE_DEBUG_VIDEO
                g_print("[Video wraparound]: diff=%lld\n", diff);
#endif

                // Update offset only on second wraparound and continue to count time with diff.
                if (time < ((gint64) stream->last_time - PTS_TO_GSTTIME(MAX_PTS)))
                {
                    stream->offset_time += diff;
#ifdef VERBOSE_DEBUG_VIDEO
                    g_print("[Video wraparound] updating offset_time to %lld: %lld < %lld\n", stream->offset_time, time, ((gint64) stream->last_time - PTS_TO_GSTTIME(MAX_PTS)));
#endif
                }

                time += diff;
            }
#ifdef VERBOSE_DEBUG_VIDEO
            g_print("[Video]: time=%lld (%.4f) offset_time=%lld, last_time=%lld\n", time, (double) time / GST_SECOND, stream->offset_time, stream->last_time);
#endif

            stream->last_time = time;
            GST_BUFFER_TIMESTAMP(buffer) = time;
        }

        if (packet->duration != 0)
            GST_BUFFER_DURATION(buffer) = PTS_TO_GSTTIME(packet->duration);

        g_mutex_lock(&demuxer->lock);
        stream->segment.position = GST_BUFFER_TIMESTAMP(buffer);

        if (stream->discont)
        {
            GstSegment newsegment;
            gst_segment_init(&newsegment, GST_FORMAT_TIME);
            newsegment.flags = stream->segment.flags;
            newsegment.rate = stream->segment.rate;
            newsegment.start = stream->segment.time;
            newsegment.stop = stream->segment.stop;
            newsegment.time = stream->segment.time;
            newsegment.position = stream->segment.position;
            newsegment_event = gst_event_new_segment(&newsegment);

            GST_BUFFER_FLAG_SET(buffer, GST_BUFFER_FLAG_DISCONT);
            stream->discont = FALSE;

#ifdef DEBUG_OUTPUT
            g_print("MpegTS: [Video] NEWSEGMENT: last_stop = %.4f\n", (double) stream->segment.last_stop / GST_SECOND);
#endif
        }
        g_mutex_unlock(&demuxer->lock);
    } else
        result = GST_FLOW_ERROR;

    if (newsegment_event)
        result = gst_pad_push_event(stream->sourcepad, newsegment_event) ? GST_FLOW_OK : GST_FLOW_FLUSHING;

    if (result == GST_FLOW_OK)
        result = gst_pad_push(stream->sourcepad, buffer);
    else
        gst_buffer_unref(buffer);

    return result;
}

static GstFlowReturn process_audio_packet(MpegTSDemuxer *demuxer, AVPacket *packet)
{
    GstFlowReturn result = GST_FLOW_OK;
    Stream *stream = &demuxer->audio;

    if (!same_stream(demuxer, stream, packet))
        return result;

    GstBuffer *buffer = NULL;
    GstEvent *newsegment_event = NULL;
    void *buffer_data = av_mallocz(packet->size);

    if (buffer_data != NULL)
    {
        memcpy(buffer_data, packet->data, packet->size);
        buffer = gst_buffer_new_wrapped_full(0, buffer_data, packet->size, 0, packet->size, buffer_data, &av_free);

        if (packet->pts != AV_NOPTS_VALUE)
        {
            if (demuxer->base_pts == GST_CLOCK_TIME_NONE)
            {
                demuxer->base_pts = PTS_TO_GSTTIME(packet->pts) + stream->offset_time;
            }

            gint64 time = PTS_TO_GSTTIME(packet->pts) + stream->offset_time - demuxer->base_pts;
            if (time < 0)
                time = 0;

            if (stream->last_time > 0 && time < (gint64) (stream->last_time - PTS_TO_GSTTIME(G_MAXUINT32)))
            {
                stream->offset_time += PTS_TO_GSTTIME(MAX_PTS + 1); // Wraparound occured
                time = PTS_TO_GSTTIME(packet->pts) + stream->offset_time;
#ifdef VERBOSE_DEBUG_AUDIO
                g_print("[Audio wraparound] updating offset_time to %lld\n", stream->offset_time);
#endif
            }

#ifdef VERBOSE_DEBUG_AUDIO
            g_print("[Audio]: pts=%lld(%.4f) time=%lld (%.4f) offset_time=%lld last_time=%lld\n",
                    PTS_TO_GSTTIME(packet->pts), (double) PTS_TO_GSTTIME(packet->pts) / GST_SECOND,
                    time, (double) time / GST_SECOND, stream->offset_time, stream->last_time);
#endif

            stream->last_time = time;
            GST_BUFFER_TIMESTAMP(buffer) = time;
        }

        if (packet->duration != 0)
            GST_BUFFER_DURATION(buffer) = PTS_TO_GSTTIME(packet->duration);

        g_mutex_lock(&demuxer->lock);
        stream->segment.position = GST_BUFFER_TIMESTAMP(buffer);

        if (stream->discont)
        {
            GstSegment newsegment;
            gst_segment_init(&newsegment, GST_FORMAT_TIME);
            newsegment.flags = stream->segment.flags;
            newsegment.rate = stream->segment.rate;
            newsegment.start = stream->segment.time;
            newsegment.stop = stream->segment.stop;
            newsegment.time = stream->segment.time;
            newsegment.position = stream->segment.position;
            newsegment_event = gst_event_new_segment(&newsegment);

            GST_BUFFER_FLAG_SET(buffer, GST_BUFFER_FLAG_DISCONT);
            stream->discont = FALSE;

#ifdef DEBUG_OUTPUT
            g_print("MpegTS: [Audio] NEWSEGMENT: last_stop = %.4f\n", (double) stream->segment.last_stop / GST_SECOND);
#endif
        }
        g_mutex_unlock(&demuxer->lock);
    } else
        result = GST_FLOW_ERROR;

    if (newsegment_event)
        result = gst_pad_push_event(stream->sourcepad, newsegment_event) ? GST_FLOW_OK : GST_FLOW_FLUSHING;

    if (result == GST_FLOW_OK)
        result = gst_pad_push(stream->sourcepad, buffer);
    else
        gst_buffer_unref(buffer);

#ifdef VERBOSE_DEBUG_AUDIO
    if (result != GST_FLOW_OK)
        g_print("MpegTS: Audio push failed: %s\n", gst_flow_get_name(result));
#endif
    return result;
}

/***********************************************************************************
 * Process input. Run in a separate thread.
 ***********************************************************************************/
#define BV(value) (value ? "TRUE" : "FALSE")

typedef enum {
    PA_INIT,
    PA_READ_FRAME,
    PA_STOP
} ParseAction;

static ParseAction mpegts_demuxer_read_frame(MpegTSDemuxer *demuxer)
{
    ParseAction   result = PA_READ_FRAME;
    GstFlowReturn flow_result = GST_FLOW_OK;
    AVPacket      packet;
    int           ret = av_read_frame(demuxer->context, &packet);

    switch(ret)
    {
        case 0:
#ifdef ENABLE_VIDEO
            if (packet.stream_index == demuxer->video.stream_index) // Video
                flow_result = process_video_packet(demuxer, &packet);
            else
#endif // ENABLE_VIDEO
            if (packet.stream_index == demuxer->audio.stream_index) // Audio
                flow_result = process_audio_packet(demuxer, &packet);

            if (flow_result != GST_FLOW_OK)
            {
                if (flow_result != GST_FLOW_FLUSHING)
                    post_message(demuxer, "Send packet failed", GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DEMUX);

                result = PA_STOP;
            }
#ifdef FAKE_ERROR
            else if (demuxer->read_bytes > 1000000)
            {
                post_message(demuxer, "Fake error", GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DEMUX);
                result = PA_STOP;
            }
#endif
            break;

        default:
            if (demuxer->is_eos && demuxer->is_last_buffer_send) // Send EOS
                mpegts_demuxer_push_to_sources(demuxer, gst_event_new_eos());
            else
                post_error(demuxer, "LibAV stream parse error", ret, GST_STREAM_ERROR_DEMUX); // Send Error
            result = PA_STOP;
            break;
    }

    av_free_packet(&packet);
    return result;
}

static ParseAction get_init_action(MpegTSDemuxer *demuxer, int ret)
{
    if (ret < 0)
    {
        if (!demuxer->is_flushing && !demuxer->context && mpegts_demuxer_expect_more_pads(demuxer))
            post_error(demuxer, "Demuxer error", ret, GST_STREAM_ERROR_DEMUX);

        return PA_STOP;
    }
    else
        return PA_READ_FRAME;
}

static gpointer mpegts_demuxer_process_input(gpointer data)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(data);
    ParseAction action = PA_INIT;

#ifdef DEBUG_OUTPUT
    g_print("MpegTS: Entered process_input\n");
#endif

    while (demuxer->is_reading)
    {
        switch(action)
        {
        case PA_INIT:
            {
#ifdef DEBUG_OUTPUT
                g_print("MpegTS: action = PA_INIT\n");
#endif

                guchar      *io_buffer = (guchar*)av_malloc(BUFFER_SIZE);
                if (!io_buffer)
                {
                    post_error(demuxer, "LibAV input buffer alloc error", 0, GST_STREAM_ERROR_DEMUX);
                    return NULL;
                }

                AVIOContext *io_context = avio_alloc_context(io_buffer,            // buffer
                                                             BUFFER_SIZE,          // buffer size
                                                             0,                    // read only
                                                             demuxer,              // opaque reference
                                                             mpegts_demuxer_read_packet, // read callback
                                                             NULL,                 // write callback
                                                             mpegts_demuxer_seek); // seek callback

                if (!io_context)
                {
                    post_error(demuxer, "LibAV context alloc error", 0, GST_STREAM_ERROR_DEMUX);
                    return NULL;
                }

                demuxer->context = avformat_alloc_context();
                demuxer->context->pb = io_context;

                demuxer->adapter_limit_type = UNLIMITED;
                demuxer->adapter_limit_size = ADAPTER_LIMIT;

                AVInputFormat* iformat = av_find_input_format("mpegts");

                action = get_init_action(demuxer, avformat_open_input(&demuxer->context, "", iformat, NULL));

                if (action != PA_READ_FRAME)
                    break;

                action = get_init_action(demuxer, avformat_find_stream_info(demuxer->context, NULL));

                g_mutex_lock(&demuxer->lock);
                gint available = gst_adapter_available(demuxer->sink_adapter);
                demuxer->adapter_limit_type = LIMITED;
                gst_adapter_flush(demuxer->sink_adapter, available > demuxer->offset ? demuxer->offset : available);
                demuxer->flush_adapter = TRUE;
                demuxer->offset = 0;
                g_cond_signal(&demuxer->del_cond);
                g_mutex_unlock(&demuxer->lock);

                mpegts_demuxer_check_streams(demuxer);
            }
            break;

        case PA_READ_FRAME:
            //            g_print("action = PA_READ_FRAME, is_eos=%s, is_flushing=%s\n", BV(demuxer->is_eos), BV(demuxer->is_flushing));
            action = mpegts_demuxer_read_frame(demuxer);
            break;

        case PA_STOP:
#ifdef DEBUG_OUTPUT
            g_print("MpegTS: action = PA_STOP\n");
#endif
            demuxer->is_reading = FALSE;

            if (demuxer->context)
            {
                av_free(demuxer->context->pb->buffer);
                av_free(demuxer->context->pb);
                avformat_free_context(demuxer->context);
                demuxer->context = NULL;
            }
            break;

        default:
            break;
        }
    }

#ifdef DEBUG_OUTPUT
    g_print("MpegTS: Exiting process_input\n");
#endif

    return NULL;
}

static int mpegts_demuxer_read_packet(void *opaque, uint8_t *buffer, int size)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(opaque);
    int result = 0;

    g_mutex_lock(&demuxer->lock);
    gint available = gst_adapter_available(demuxer->sink_adapter);
    while (available < demuxer->offset + size &&
           !demuxer->is_eos && !demuxer->is_flushing && demuxer->is_reading)
    {
        if (demuxer->adapter_limit_type == UNLIMITED &&
            demuxer->adapter_limit_size - LIMIT_STEP < demuxer->offset + size)
        {
            demuxer->adapter_limit_size += LIMIT_STEP;
            g_cond_signal(&demuxer->del_cond);
        }
        else
            g_cond_wait(&demuxer->add_cond, &demuxer->lock);

        available = gst_adapter_available(demuxer->sink_adapter);
    }

    if (demuxer->is_reading && !demuxer->is_flushing)
    {
        if (demuxer->is_eos && available <= size) {
            demuxer->is_last_buffer_send = TRUE; // Last buffer
            size = available;
        }

        if (size > 0)
        {
            gst_adapter_copy(demuxer->sink_adapter, buffer, demuxer->offset, size);
            if (demuxer->flush_adapter)
                gst_adapter_flush(demuxer->sink_adapter, size);
            else
                demuxer->offset += size;

            g_cond_signal(&demuxer->del_cond);
            result = size;

#ifdef FAKE_ERROR
            demuxer->read_bytes += size;
#endif
        }
    }
    else
        result = 0; // No more data

    g_mutex_unlock(&demuxer->lock);

#ifdef DEBUG_OUTPUT
    if (result <= 0)
        g_print("MpegTS: read_packet result = %d, is_eos=%s, is_reading=%s, is_flushing=%s\n",
                result, BV(demuxer->is_eos), BV(demuxer->is_reading), BV(demuxer->is_flushing));
#endif
    return result;
}

#ifdef DEBUG_OUTPUT
static const char* whence_to_str(int whence)
{
    switch(whence)
    {
    case SEEK_SET:
        return "SET";
    case SEEK_CUR:
        return "CUR";
    case SEEK_END:
        return "END";
    default:
        return "UNKNOWN";
    }
}
#endif

static int64_t mpegts_demuxer_seek(void *opaque, int64_t offset, int whence)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(opaque);
    int64_t result = -1;

    g_mutex_lock(&demuxer->lock);
    gint available = gst_adapter_available(demuxer->sink_adapter);

    if (whence == SEEK_SET && offset >= 0 && offset < available)
    {
        result = demuxer->offset = offset;
#ifdef DEBUG_OUTPUT
        g_print("MpegTS: demuxer_seek offset=%ld, whence=%s, result=%ld\n", offset, whence_to_str(whence), result);
#endif
    }
    else if (whence == SEEK_END && offset == -1)
    {
        result = demuxer->offset = available + offset;
#ifdef DEBUG_OUTPUT
        g_print("MpegTS: demuxer_seek offset=%ld, whence=%s, result=%ld\n", offset, whence_to_str(whence), result);
#endif
    }

    g_mutex_unlock(&demuxer->lock);
    return result;
}

/***********************************************************************************
 * Query
 ***********************************************************************************/
static gboolean mpegts_demuxer_sink_query (GstPad *pad, GstObject *parent, GstQuery *query)
{
    gboolean result = TRUE;
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);

    switch (GST_QUERY_TYPE(query))
    {
        case GST_QUERY_DURATION:
        {
            GstFormat format;
            gst_query_parse_duration(query, &format, NULL);

            if (format == GST_FORMAT_TIME)
                result = gst_pad_peer_query(pad, query);
            else if (format == GST_FORMAT_BYTES)
            {
                g_mutex_lock(&demuxer->lock);
                int bit_rate = (demuxer->context != NULL) ? demuxer->context->bit_rate : 0;
                g_mutex_unlock(&demuxer->lock);

                if (bit_rate > 0)
                {
                    gint64    duration = GST_CLOCK_TIME_NONE;
                    if (gst_pad_peer_query_duration(pad, GST_FORMAT_TIME, &duration))
                    {
                        // Approximate duration in bytes for a certain time duration and bit rate.
                        if (duration != GST_CLOCK_TIME_NONE)
                            duration = (double)(duration * bit_rate) / GST_SECOND / 8;
                        gst_query_set_duration(query, format, duration);
                    }
                    else
                        result = FALSE;
                }
                else
                    result = gst_pad_peer_query(pad, query);
            }
            break;
        }

        default:
            result = gst_pad_peer_query(pad, query);
            break;
    }

    return result;
}

static gboolean mpegts_demuxer_src_query (GstPad *pad, GstObject *parent, GstQuery *query)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(parent);
    return gst_pad_query(demuxer->sinkpad, query);
}

/***********************************************************************************
 * Init, clean functions
 ***********************************************************************************/
static void init_stream(Stream* stream)
{
    stream->stream_index = NO_STREAM;
    stream->discont = FALSE;
    gst_segment_init(&stream->segment, GST_FORMAT_TIME);
}

static void mpegts_demuxer_init_state(MpegTSDemuxer *demuxer)
{
    demuxer->is_eos = FALSE;
    demuxer->is_last_buffer_send = FALSE;
    demuxer->is_flushing = FALSE;
    demuxer->is_reading = TRUE;
    demuxer->is_closing = FALSE;
    demuxer->context = NULL;
    demuxer->update = FALSE;

    demuxer->adapter_limit_type = UNLIMITED;
    demuxer->adapter_limit_size = ADAPTER_LIMIT;

    init_stream(&demuxer->video);
    init_stream(&demuxer->audio);

    mpegts_demuxer_flush(demuxer);

#ifdef FAKE_ERROR
    demuxer->read_bytes = 0;
#endif

#ifdef DEBUG_OUTPUT
    g_print("MpegTS: demuxer initialized\n");
#endif
}

static void mpegts_demuxer_flush(MpegTSDemuxer *demuxer)
{
    gst_adapter_clear(demuxer->sink_adapter);

    demuxer->offset = 0;
    demuxer->flush_adapter = FALSE;

    demuxer->audio.last_time = demuxer->audio.offset_time = 0;
    demuxer->video.last_time = demuxer->video.offset_time = 0;
}

static void mpegts_demuxer_close(MpegTSDemuxer *demuxer)
{
    g_mutex_lock(&demuxer->lock);
    demuxer->is_reading = FALSE;
    demuxer->is_closing = TRUE;
    g_cond_signal(&demuxer->add_cond);
    g_mutex_unlock(&demuxer->lock);

    if (demuxer->reader_thread)
    {
        g_thread_join(demuxer->reader_thread);
        demuxer->reader_thread = NULL;
    }

    if (demuxer->context)
    {
        av_free(demuxer->context->pb->buffer);
        av_free(demuxer->context->pb);
        avformat_free_context(demuxer->context);
        demuxer->context = NULL;
    }

    mpegts_demuxer_flush(demuxer);

#ifdef DEBUG_OUTPUT
    g_print("MpegTS: demuxer closed\n");
#endif
}

/***********************************************************************************
 * State change
 ***********************************************************************************/
static GstStateChangeReturn
mpegts_demuxer_change_state(GstElement* element, GstStateChange transition)
{
    MpegTSDemuxer *demuxer = MPEGTS_DEMUXER(element);

    switch (transition)
    {
        case GST_STATE_CHANGE_NULL_TO_READY:
            mpegts_demuxer_init_state(demuxer);
            break;
        case GST_STATE_CHANGE_READY_TO_PAUSED:
            //mpegts_demuxer_state_reset(decoder);
            break;
        default:
            break;
    }

    // Change state.
    GstStateChangeReturn ret = GST_ELEMENT_CLASS(parent_class)->change_state(element, transition);
    if (GST_STATE_CHANGE_FAILURE == ret)
        return ret;

    switch (transition)
    {
        case GST_STATE_CHANGE_READY_TO_NULL:
            mpegts_demuxer_close(demuxer);
            break;
        default:
            break;
    }

    return ret;
}

// --------------------------------------------------------------------------
gboolean mpegts_demuxer_plugin_init (GstPlugin* mpegts_demuxer)
{
    GST_DEBUG_CATEGORY_INIT(mpegts_demuxer_debug, MPEGTS_DEMUXER_PLUGIN_NAME,
            0, "JFX libavc based MPEG-TS parser");

    return gst_element_register(mpegts_demuxer, MPEGTS_DEMUXER_PLUGIN_NAME,
            0, TYPE_MPEGTS_DEMUXER);
}
