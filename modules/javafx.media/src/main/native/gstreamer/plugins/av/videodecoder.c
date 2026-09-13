/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "videodecoder.h"
#include "fxplugins_common.h"
#include <libavformat/avformat.h>
#include <libavutil/pixfmt.h>

GST_DEBUG_CATEGORY_STATIC(videodecoder_debug);
#define GST_CAT_DEFAULT videodecoder_debug

enum
{
    PROP_0,
    PROP_CODEC_ID,
    PROP_IS_SUPPORTED,
};

/*
 * The input capabilities.
 */
#define SINK_CAPS    \
    "video/x-h264; " \
    "video/x-h265"

static GstStaticPadTemplate sink_template =
    GST_STATIC_PAD_TEMPLATE ("sink",
                             GST_PAD_SINK,
                             GST_PAD_ALWAYS,
                             GST_STATIC_CAPS (SINK_CAPS));

/*
 * The output capabilities.
 */
#define SOURCE_CAPS           \
    "video/x-raw-yuv, "       \
    "format = (string) I420"

static GstStaticPadTemplate source_template =
    GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC,
        GST_PAD_ALWAYS,
        GST_STATIC_CAPS(SOURCE_CAPS));

//#define DEBUG_OUTPUT
//#define VERBOSE_DEBUG

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE(VideoDecoder, videodecoder, BaseDecoder, TYPE_BASEDECODER);
 ***********************************************************************************/
#define videodecoder_parent_class parent_class
static void videodecoder_init          (VideoDecoder      *self);
static void videodecoder_class_init    (VideoDecoderClass *klass);
static gpointer videodecoder_parent_class = NULL;
static void     videodecoder_class_intern_init (gpointer klass)
{
    videodecoder_parent_class = g_type_class_peek_parent (klass);
    videodecoder_class_init ((VideoDecoderClass*) klass);
}

GType videodecoder_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (TYPE_BASEDECODER,
               g_intern_static_string ("VideoDecoder"),
               sizeof (VideoDecoderClass),
               (GClassInitFunc) videodecoder_class_intern_init,
               sizeof(VideoDecoder),
               (GInstanceInitFunc) videodecoder_init,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
 * Calss and instance init and forward declarations
 ***********************************************************************************/
static GstStateChangeReturn videodecoder_change_state(GstElement* element, GstStateChange transition);
static gboolean             videodecoder_sink_event(GstPad *pad, GstObject *parent, GstEvent *event);
static GstFlowReturn        videodecoder_chain(GstPad *pad, GstObject *parent, GstBuffer *buf);

static void                 videodecoder_init_state(VideoDecoder *decoder);
static void                 videodecoder_state_reset(VideoDecoder *decoder);

static gboolean videodecoder_configure(VideoDecoder *decoder, GstCaps *sink_caps);

static void videodecoder_dispose(GObject* object);
static void videodecoder_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec);
static void videodecoder_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec);

static void videodecoder_class_init(VideoDecoderClass *klass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(klass);
    GObjectClass *gobject_class = (GObjectClass*)klass;

    gst_element_class_set_metadata(element_class,
                "Videodecoder",
                "Codec/Decoder/Video",
                "Decode video stream",
                "Oracle Corporation");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&source_template));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_template));

    element_class->change_state = videodecoder_change_state;

    gobject_class->dispose = videodecoder_dispose;
    gobject_class->set_property = videodecoder_set_property;
    gobject_class->get_property = videodecoder_get_property;

    g_object_class_install_property (gobject_class, PROP_CODEC_ID,
        g_param_spec_int ("codec-id", "Codec ID", "Codec ID", -1, G_MAXINT, 0,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));

    g_object_class_install_property (gobject_class, PROP_IS_SUPPORTED,
        g_param_spec_boolean ("is-supported", "Is supported", "Is codec ID supported", FALSE,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));
}

static void videodecoder_init(VideoDecoder *decoder)
{
    BaseDecoder *base = BASEDECODER(decoder);

    // Input.
    base->sinkpad = gst_pad_new_from_static_template(&sink_template, "sink");
    gst_pad_set_chain_function(base->sinkpad, GST_DEBUG_FUNCPTR(videodecoder_chain));
    gst_pad_set_event_function(base->sinkpad, GST_DEBUG_FUNCPTR(videodecoder_sink_event));
    gst_element_add_pad(GST_ELEMENT(decoder), base->sinkpad);

    // Output.
    base->srcpad = gst_pad_new_from_static_template(&source_template, "src");
    gst_pad_use_fixed_caps(base->srcpad);
    gst_element_add_pad(GST_ELEMENT(decoder), base->srcpad);
}

void videodecoder_close_decoder(VideoDecoder *decoder)
{
#if HEVC_SUPPORT
    if (decoder->dest_frame)
    {
        av_frame_free(&decoder->dest_frame);
        decoder->dest_frame = NULL;
    }

    if (decoder->sws_context)
    {
        decoder->sws_freeContext_func(decoder->sws_context);
        decoder->sws_context = NULL;
    }

    if (decoder->swscale_module)
    {
        dlclose(decoder->swscale_module);
        decoder->swscale_module = NULL;
    }
#endif // HEVC_SUPPORT
}

static void videodecoder_dispose(GObject* object)
{
    VideoDecoder *decoder = VIDEODECODER(object);

    basedecoder_close_decoder(BASEDECODER(decoder));

    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static gboolean videodecoder_is_decoder_by_codec_id_supported(gint codec_id)
{
    switch(codec_id)
    {
    case JFX_CODEC_ID_H265:
#if HEVC_SUPPORT
        return TRUE;
#else // HEVC_SUPPORT
        return FALSE;
#endif // HEVC_SUPPORT
        break;
    case JFX_CODEC_ID_AVC1:
        return TRUE;
        break;
    case JFX_CODEC_ID_H264:
        return TRUE;
        break;
    }

    return FALSE;
}

static void videodecoder_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec)
{
    VideoDecoder *decoder = VIDEODECODER(object);
    switch (property_id)
    {
    case PROP_CODEC_ID:
        decoder->codec_id = g_value_get_int(value);
        break;
    default:
        break;
    }
}

static void videodecoder_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec)
{
    VideoDecoder *decoder = VIDEODECODER(object);
    gboolean is_supported = FALSE;
    switch (property_id)
    {
    case PROP_IS_SUPPORTED:
        is_supported = videodecoder_is_decoder_by_codec_id_supported(decoder->codec_id);
        g_value_set_boolean(value, is_supported);
        break;
    default:
        break;
    }
}

/***********************************************************************************
 * State change handler
 ***********************************************************************************/
static GstStateChangeReturn
videodecoder_change_state(GstElement* element, GstStateChange transition)
{
    VideoDecoder *decoder = VIDEODECODER(element);

    switch (transition)
    {
        case GST_STATE_CHANGE_NULL_TO_READY:
            videodecoder_init_state(decoder);
            break;
        case GST_STATE_CHANGE_READY_TO_PAUSED:
            // Clear the VideoDecoder state.
            videodecoder_state_reset(decoder);
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
        case GST_STATE_CHANGE_PAUSED_TO_READY:
            basedecoder_close_decoder(BASEDECODER(decoder));
            break;
        default:
            break;
    }

    return ret;
}

/***********************************************************************************
 * Sink event handler
 ***********************************************************************************/
static gboolean videodecoder_sink_event(GstPad *pad, GstObject *parent, GstEvent *event)
{
    VideoDecoder *decoder = VIDEODECODER(parent);
    gboolean ret = FALSE;

    switch (GST_EVENT_TYPE(event))
    {
        case GST_EVENT_FLUSH_START:
            // Start flushing buffers.
            // Set flag so chain function refuses new buffers.
            BASEDECODER(decoder)->is_flushing = TRUE;
            break;

        case GST_EVENT_FLUSH_STOP:
            // Stop flushing buffers.
            videodecoder_state_reset(decoder);

            // Unset flag so chain function accepts buffers.
            BASEDECODER(decoder)->is_flushing = FALSE;
            break;

        case GST_EVENT_CAPS:
        {
            GstCaps *caps;

            gst_event_parse_caps (event, &caps);
            if (!videodecoder_configure(decoder, caps))
            {
                gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_FAILED,
                                 g_strdup("Initialization of video decoder failed"), NULL, ("videodecoder.c"), ("videodecoder_sink_event"), 0);
            }

            // INLINE - gst_event_unref()
            gst_event_unref (event);
            ret = TRUE;
            break;
        }

#ifdef DEBUG_OUTPUT
        case GST_EVENT_SEGMENT:
        {
            GstSegment segment;
            gst_event_copy_segment(event, &segment);

            g_print("videodecoder_sink_event: NEW_SEGMENT rate=%.1f, format=%d, start=%.3f, stop=%.3f, time=%.3f\n",
                    segment.rate, segment.format, (double)segment.start/GST_SECOND, (double)segment.stop/GST_SECOND, (double)segment.time/GST_SECOND);

            break;
        }
#endif // DEBUG_OUTPUT

        default:
            break;
    }

    if (!ret)
        ret = gst_pad_push_event(BASEDECODER(decoder)->srcpad, event);

    return ret;
}

/***********************************************************************************
 * chain
 ***********************************************************************************/
static void videodecoder_init_state(VideoDecoder *decoder)
{
    decoder->width = decoder->height = 0;
    decoder->u_offset = 0;
    decoder->v_offset = 0;
    decoder->uv_blocksize = 0;
    decoder->frame_size = 0;
    decoder->discont = FALSE;
    decoder->codec_id = JFX_CODEC_ID_UNKNOWN;
#if HEVC_SUPPORT
    decoder->sws_context = NULL;
    decoder->dest_frame = NULL;
    decoder->swscale_module = NULL;
    decoder->sws_getContext_func = NULL;
    decoder->sws_freeContext_func = NULL;
    decoder->sws_scale_func = NULL;
#endif // HEVC_SUPPORT

    basedecoder_init_state(BASEDECODER(decoder));
}

static gboolean videodecoder_configure(VideoDecoder *decoder, GstCaps *sink_caps)
{
    BaseDecoder *base = BASEDECODER(decoder);
    const gchar *mimetype = NULL;
    gint width = 0;
    gint height = 0;

    if(gst_caps_get_size(sink_caps) < 1)
        return FALSE;

    GstStructure *s = gst_caps_get_structure(sink_caps, 0);

    // Reload decoder if input resolution changed.
    if (gst_structure_get_int(s, "width", &width) && gst_structure_get_int(s, "height", &height))
    {
        if (decoder->width != 0 && decoder->height != 0 &&
                (decoder->width != width || decoder->height != height))
        {
            videodecoder_state_reset(decoder);
            basedecoder_close_decoder(BASEDECODER(decoder));
            videodecoder_close_decoder(decoder);
            videodecoder_init_state(decoder);
        }
    }

    if (base->is_initialized)
        return TRUE;

    // Pass stencil context to init against if there is one.
    basedecoder_set_codec_data(base, s);

    if (s != NULL)
    {
        mimetype = gst_structure_get_name(s);
        if (mimetype != NULL)
        {
            if (strstr(mimetype, "video/x-h264") != NULL)
            {
#if NEW_CODEC_ID
                base->is_initialized = basedecoder_open_decoder(BASEDECODER(decoder), AV_CODEC_ID_H264);
#else
                base->is_initialized = basedecoder_open_decoder(BASEDECODER(decoder), CODEC_ID_H264);
#endif
            }
            else if (strstr(mimetype, "video/x-h265") != NULL)
            {
#if HEVC_SUPPORT
                base->is_initialized = basedecoder_open_decoder(BASEDECODER(decoder), AV_CODEC_ID_HEVC);
#else
                return FALSE;
#endif
            }
        }
    }

    return base->is_initialized;
}

static void videodecoder_state_reset(VideoDecoder *decoder)
{
    decoder->frame_finished = 1;
    basedecoder_flush(BASEDECODER(decoder));
}

#if HEVC_SUPPORT
static gboolean videodecoder_init_converter(VideoDecoder *decoder)
{
    BaseDecoder *base = BASEDECODER(decoder);

    // Load libswscale
    if (decoder->swscale_module == NULL)
    {
        decoder->swscale_module = dlopen("libswscale.so", RTLD_LAZY);
        if (decoder->swscale_module == NULL)
        {
            // Halt playback, since we cannot continue and post user
            // friendly error message that libswscale is required
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                    JFX_GST_ERROR, JFX_GST_MISSING_LIBSWSCALE,
                    g_strdup("Error: libswscale is required for H.265/HEVC 10/12-bit decoding"), NULL,
                    ("videodecoder.c"), ("videodecoder_init_converter"), 0);
            return FALSE;
        }

        decoder->sws_getContext_func = dlsym(decoder->swscale_module, "sws_getContext");
        if (!decoder->sws_getContext_func)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                    JFX_GST_ERROR, JFX_GST_INVALID_LIBSWSCALE,
                    g_strdup("Error: Failed to find \"sws_getContext()\" in libswscale"), NULL,
                    ("videodecoder.c"), ("videodecoder_init_converter"), 0);
            return FALSE;
        }

        decoder->sws_freeContext_func = dlsym(decoder->swscale_module, "sws_freeContext");
        if (!decoder->sws_freeContext_func)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                    JFX_GST_ERROR, JFX_GST_INVALID_LIBSWSCALE,
                    g_strdup("Error: Failed to find \"sws_freeContext()\" in libswscale"), NULL,
                    ("videodecoder.c"), ("videodecoder_init_converter"), 0);
            return FALSE;
        }

        decoder->sws_scale_func = dlsym(decoder->swscale_module, "sws_scale");
        if (!decoder->sws_scale_func)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                    JFX_GST_ERROR, JFX_GST_INVALID_LIBSWSCALE,
                    g_strdup("Error: Failed to find \"sws_scale()\" in libswscale"), NULL,
                    ("videodecoder.c"), ("videodecoder_init_converter"), 0);
            return FALSE;
        }
    }

    if (decoder->dest_frame)
    {
        av_frame_free(&decoder->dest_frame);
        decoder->dest_frame = NULL;
    }

    if (decoder->sws_context)
    {
        decoder->sws_freeContext_func(decoder->sws_context);
        decoder->sws_context = NULL;
    }

    decoder->sws_context =
            decoder->sws_getContext_func(decoder->width, decoder->height,
                                         base->frame->format, decoder->width,
                                         decoder->height, AV_PIX_FMT_YUV420P,
                                         SWS_BILINEAR, NULL, NULL, NULL);

    if (decoder->sws_context == NULL)
        return FALSE;

    decoder->dest_frame = av_frame_alloc();
    if (decoder->dest_frame == NULL)
        return FALSE;

    decoder->dest_frame->format = AV_PIX_FMT_YUV420P;
    decoder->dest_frame->width  = decoder->width;
    decoder->dest_frame->height = decoder->height;
    int ret = av_frame_get_buffer(decoder->dest_frame, 32);
    if (ret < 0)
    {
        av_frame_free(&decoder->dest_frame);
        decoder->dest_frame = NULL;
        decoder->sws_freeContext_func(decoder->sws_context);
        decoder->sws_context = NULL;
        return FALSE;
    }

    return TRUE;
}

static gboolean videodecoder_convert_frame(VideoDecoder *decoder)
{
    BaseDecoder *base = BASEDECODER(decoder);

    if (decoder->sws_context == NULL || decoder->dest_frame == NULL ||
            decoder->sws_scale_func == NULL)
        return FALSE;

    int ret = decoder->sws_scale_func(decoder->sws_context,
                                      (const uint8_t * const*)base->frame->data,
                                      base->frame->linesize,
                                      0,
                                      base->frame->height,
                                      decoder->dest_frame->data,
                                      decoder->dest_frame->linesize);
    if (ret < 0)
        return FALSE;

#if NO_REORDERED_OPAQUE
    decoder->dest_frame->pts = base->frame->pts;
#else // NO_REORDERED_OPAQUE
    decoder->dest_frame->reordered_opaque = base->frame->reordered_opaque;
#endif // NO_REORDERED_OPAQUE

    return TRUE;
}
#endif // HEVC_SUPPORT

static gboolean videodecoder_configure_sourcepad(VideoDecoder *decoder)
{
    BaseDecoder *base = BASEDECODER(decoder);
    gboolean set_linesize = TRUE;
    int linesize0 = 0;
    int linesize1 = 0;
    int linesize2 = 0;

    GstCaps *caps = gst_pad_get_current_caps(base->srcpad);

#if NEW_CODEC_ID
    int width = base->frame->width;
    int height = base->frame->height;
#else
    int width = base->context->width;
    int height = base->context->height;
#endif // NEW_CODEC_ID

    if (caps == NULL ||
        decoder->width != width || decoder->height != height)
    {
        decoder->width = width;
        decoder->height = height;

#if HEVC_SUPPORT
    // Setup scaler and color converter if pixel format is not AV_PIX_FMT_YUV420P.
    // We will get different pixel format for H.265 10-bit such as
    // AV_PIX_FMT_YUV422P10LE. Scaling should not happen if resolution is same.
    if (base->frame->format != AV_PIX_FMT_YUV420P)
    {
        if (!videodecoder_init_converter(decoder))
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                                             GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                             g_strdup("videodecoder_init_convert() failed"), NULL,
                                             ("videodecoder.c"), ("videodecoder_configure_sourcepad"), 0);
            return FALSE;
        }

        if (!videodecoder_convert_frame(decoder))
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                                             GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                             g_strdup("videodecoder_convert_frame() failed"), NULL,
                                             ("videodecoder.c"), ("videodecoder_configure_sourcepad"), 0);

            return FALSE;
        }

        linesize0 = decoder->dest_frame->linesize[0];
        linesize1 = decoder->dest_frame->linesize[1];
        linesize2 = decoder->dest_frame->linesize[2];

        set_linesize = FALSE;
    }
#endif // HEVC_SUPPORT

        decoder->discont = (caps != NULL);

        if (set_linesize)
        {
            linesize0 = base->frame->linesize[0];
            linesize1 = base->frame->linesize[1];
            linesize2 = base->frame->linesize[2];
        }

        decoder->u_offset = linesize0 * decoder->height;
        decoder->uv_blocksize = linesize1 * decoder->height / 2;

        decoder->v_offset = decoder->u_offset + decoder->uv_blocksize;
        decoder->frame_size = (linesize0 + linesize1) * decoder->height;

        GstCaps *src_caps = gst_caps_new_simple("video/x-raw-yuv",
                                                "format", G_TYPE_STRING, "YV12",
                                                "width", G_TYPE_INT, decoder->width,
                                                "height", G_TYPE_INT, decoder->height,
                                                "stride-y", G_TYPE_INT, linesize0,
                                                "stride-u", G_TYPE_INT, linesize1,
                                                "stride-v", G_TYPE_INT, linesize2,
                                                "offset-y", G_TYPE_INT, 0,
                                                "offset-u", G_TYPE_INT, decoder->u_offset,
                                                "offset-v", G_TYPE_INT, decoder->v_offset,
                                                "framerate", GST_TYPE_FRACTION, 2997, 100,
                                                NULL);


        GstEvent *caps_event = gst_event_new_caps(src_caps);
        if (caps_event == NULL || !gst_pad_push_event (base->srcpad, caps_event))
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_NEGOTIATION,
                                     g_strdup("Failed to set caps on the sourcepad"), NULL,
                                     ("videodecoder.c"), ("videodecoder_configure"), 0);
            if (caps)
                gst_caps_unref(caps);
            gst_caps_unref(src_caps);

            return FALSE;
        }
        gst_caps_unref(src_caps);
    }

    if (caps)
        gst_caps_unref(caps);

    return TRUE;
}
/***********************************************************************************
 * chain
 ***********************************************************************************/
static GstFlowReturn videodecoder_chain(GstPad *pad, GstObject *parent, GstBuffer *buf)
{
    VideoDecoder  *decoder = VIDEODECODER(parent);
    BaseDecoder   *base = BASEDECODER(decoder);
    GstFlowReturn  result = GST_FLOW_OK;
    int            num_dec = NO_DATA_USED;
    GstMapInfo     info;
    GstMapInfo     info2;
    gboolean       unmap_buf = FALSE;
    gboolean       set_frame_values = TRUE;
    int64_t        pts = AV_NOPTS_VALUE;
    unsigned int   out_buf_size = 0;
    gboolean       copy_error = FALSE;
    uint8_t*       data0 = NULL;
    uint8_t*       data1 = NULL;
    uint8_t*       data2 = NULL;

    if (base->is_flushing)  // Reject buffers in flushing state.
    {
        result = GST_FLOW_FLUSHING;
        goto _exit;
    }

    if (!base->is_initialized)
    {
        result = GST_FLOW_ERROR;
        goto _exit;
    }

    if (!gst_buffer_map(buf, &info, GST_MAP_READ))
    {
        result = GST_FLOW_ERROR;
        goto _exit;
    }

    unmap_buf = TRUE;

    if (!base->is_hls)
    {
        if (av_new_packet(&decoder->packet, info.size) == 0)
        {
            memcpy(decoder->packet.data, info.data, info.size);
#if NO_REORDERED_OPAQUE
            if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
                decoder->packet.pts = (int64_t)GST_BUFFER_TIMESTAMP(buf);
            else
                decoder->packet.pts = AV_NOPTS_VALUE;
#else // NO_REORDERED_OPAQUE
            if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
                base->context->reordered_opaque = GST_BUFFER_TIMESTAMP(buf);
            else
                base->context->reordered_opaque = AV_NOPTS_VALUE;
#endif // NO_REORDERED_OPAQUE
#if USE_SEND_RECEIVE
            num_dec = avcodec_send_packet(base->context, &decoder->packet);
            if (num_dec == 0)
            {
                num_dec = avcodec_receive_frame(base->context, base->frame);
                if (num_dec == 0)
                    decoder->frame_finished = 1;
                else
                    decoder->frame_finished = 0;
            }
#else
            num_dec = avcodec_decode_video2(base->context, base->frame, &decoder->frame_finished, &decoder->packet);
#endif

#if PACKET_UNREF
            av_packet_unref(&decoder->packet);
#else
            av_free_packet(&decoder->packet);
#endif
        }
        else
        {
            result = GST_FLOW_ERROR;
            goto _exit;
        }
    }
    else
    {
        av_init_packet(&decoder->packet);
        decoder->packet.data = info.data;
        decoder->packet.size = info.size;
#if NO_REORDERED_OPAQUE
        if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
            decoder->packet.pts = (int64_t)GST_BUFFER_TIMESTAMP(buf);
        else
            decoder->packet.pts = AV_NOPTS_VALUE;
#else // NO_REORDERED_OPAQUE
        if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
            base->context->reordered_opaque = GST_BUFFER_TIMESTAMP(buf);
        else
            base->context->reordered_opaque = AV_NOPTS_VALUE;
#endif // NO_REORDERED_OPAQUE

#if USE_SEND_RECEIVE
        num_dec = avcodec_send_packet(base->context, &decoder->packet);
        if (num_dec == 0)
        {
            num_dec = avcodec_receive_frame(base->context, base->frame);
            if (num_dec == 0)
                decoder->frame_finished = 1;
            else
                decoder->frame_finished = 0;
        }
#else
        num_dec = avcodec_decode_video2(base->context, base->frame, &decoder->frame_finished, &decoder->packet);
#endif
    }

    if (num_dec < 0)
    {
        //        basedecoder_flush(base);
#ifdef DEBUG_OUTPUT
        g_print ("videodecoder_chain error: %s\n", avelement_error_to_string(AVELEMENT(decoder), num_dec));
#endif
        goto _exit;
    }

    if (decoder->frame_finished > 0)
    {
        if (!videodecoder_configure_sourcepad(decoder))
            result = GST_FLOW_ERROR;
        else
        {
#if HEVC_SUPPORT
            // Check to see if we need to convert frame to YUV420p
            if (base->frame->format != AV_PIX_FMT_YUV420P)
            {
                if (!videodecoder_convert_frame(decoder))
                {
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                                             GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                             g_strdup("Video frame conversion failed"), NULL,
                                             ("videodecoder.c"), ("videodecoder_chain"), 0);

                    result = GST_FLOW_ERROR;
                    goto _exit;
                }

#if NO_REORDERED_OPAQUE
                pts = decoder->dest_frame->pts;
#else // NO_REORDERED_OPAQUE
                pts = decoder->dest_frame->reordered_opaque;
#endif // NO_REORDERED_OPAQUE
                data0 = decoder->dest_frame->data[0];
                data1 = decoder->dest_frame->data[1];
                data2 = decoder->dest_frame->data[2];
                set_frame_values = FALSE;
            }
#endif // HEVC_SUPPORTf

            if (set_frame_values)
            {
#if NO_REORDERED_OPAQUE
                pts = base->frame->pts;
#else // NO_REORDERED_OPAQUE
                pts = base->frame->reordered_opaque;
#endif // NO_REORDERED_OPAQUE
                data0 = base->frame->data[0];
                data1 = base->frame->data[1];
                data2 = base->frame->data[2];
            }

            GstBuffer *outbuf = gst_buffer_new_allocate(NULL, decoder->frame_size, NULL);
            if (outbuf == NULL)
            {
                if (result != GST_FLOW_FLUSHING)
                {
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                                             GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                             g_strdup("Decoded video buffer allocation failed"), NULL,
                                             ("videodecoder.c"), ("videodecoder_chain"), 0);
                }
            }
            else
            {
#if USE_FRAME_NUM
                GST_BUFFER_OFFSET(outbuf) = base->context->frame_num;
#else // USE_FRAME_NUM
                GST_BUFFER_OFFSET(outbuf) = base->context->frame_number;
#endif // USE_FRAME_NUM
                if (pts != AV_NOPTS_VALUE)
                {
                    GST_BUFFER_TIMESTAMP(outbuf) = pts;
                    GST_BUFFER_DURATION(outbuf) = GST_BUFFER_DURATION(buf); // Duration for video usually same
                }

                if (!gst_buffer_map(outbuf, &info2, GST_MAP_WRITE))
                {
                    // INLINE - gst_buffer_unref()
                    gst_buffer_unref(outbuf);
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                     g_strdup("Decoded video buffer allocation failed"), NULL, ("videodecoder.c"), ("videodecoder_chain"), 0);
                    goto _exit;
                }

                // Copy image by parts from different arrays.
                if (decoder->frame_size > (unsigned int)info2.maxsize) // maxsize should be same or more due to alignment
                {
                    gst_buffer_unmap(outbuf, &info2);
                    // INLINE - gst_buffer_unref()
                    gst_buffer_unref(outbuf);
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                     g_strdup("Wrong buffer size"), NULL, ("videodecoder.c"), ("videodecoder_chain"), 0);
                    goto _exit;
                }

                out_buf_size = decoder->frame_size;
                if (out_buf_size >= decoder->u_offset)
                {
                    memcpy(info2.data, data0, decoder->u_offset);
                    out_buf_size -= decoder->u_offset;
                    if (out_buf_size >= decoder->uv_blocksize &&
                        decoder->uv_blocksize <= decoder->frame_size &&
                        decoder->u_offset <= (decoder->frame_size - decoder->uv_blocksize))
                    {
                        memcpy(info2.data + decoder->u_offset, data1, decoder->uv_blocksize);
                        out_buf_size -= decoder->uv_blocksize;
                        if (out_buf_size >= decoder->uv_blocksize &&
                            decoder->uv_blocksize <= decoder->frame_size &&
                            decoder->v_offset <= (decoder->frame_size - decoder->uv_blocksize))
                        {
                            memcpy(info2.data + decoder->v_offset, data2, decoder->uv_blocksize);
                        }
                        else
                        {
                            copy_error = TRUE;
                        }
                    }
                    else
                    {
                        copy_error = TRUE;
                    }
                }
                else
                {
                    copy_error = TRUE;
                }

                gst_buffer_unmap(outbuf, &info2);

                if (copy_error)
                {
                    // INLINE - gst_buffer_unref()
                    gst_buffer_unref(outbuf);
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                     g_strdup("Copy data failed"), NULL, ("videodecoder.c"), ("videodecoder_chain"), 0);
                    goto _exit;
                }

                GST_BUFFER_OFFSET_END(outbuf) = GST_BUFFER_OFFSET_NONE;

                if (decoder->discont || GST_BUFFER_IS_DISCONT(buf))
                {
#ifdef DEBUG_OUTPUT
                    g_print("Video discont: frame size=%dx%d\n", base->context->width, base->context->height);
#endif
                    GST_BUFFER_FLAG_SET(outbuf, GST_BUFFER_FLAG_DISCONT);
                    decoder->discont = FALSE;
                }


#ifdef VERBOSE_DEBUG
                g_print("videodecoder: pushing buffer ts=%.4f, duration=%.4f\n",
                    GST_BUFFER_TIMESTAMP_IS_VALID(outbuf) ? (double)GST_BUFFER_TIMESTAMP(outbuf)/GST_SECOND : -1.0,
                    GST_BUFFER_DURATION_IS_VALID(outbuf) ? (double)GST_BUFFER_DURATION(outbuf)/GST_SECOND : -1.0);
#endif
                result = gst_pad_push(base->srcpad, outbuf);
#ifdef VERBOSE_DEBUG
                g_print(" done, res=%s\n", gst_flow_get_name(result));
#endif
            }
        }
    }

_exit:
    if (unmap_buf)
        gst_buffer_unmap(buf, &info);
// INLINE - gst_buffer_unref()
    gst_buffer_unref(buf);
    return result;
}

// --------------------------------------------------------------------------
gboolean videodecoder_plugin_init(GstPlugin * videodecoder)
{
    GST_DEBUG_CATEGORY_INIT(videodecoder_debug, AV_VIDEO_DECODER_PLUGIN_NAME,
                            0, "JFX libavc based videodecoder");

    return gst_element_register(videodecoder, AV_VIDEO_DECODER_PLUGIN_NAME,
                                0, TYPE_VIDEODECODER);
}
