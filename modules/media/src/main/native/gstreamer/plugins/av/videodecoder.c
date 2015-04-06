/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include <libavformat/avformat.h>

GST_DEBUG_CATEGORY_STATIC(videodecoder_debug);
#define GST_CAT_DEFAULT videodecoder_debug

/*
 * The input capabilities.
 */
#define SINK_CAPS                     \
    "video/x-h264"

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
    "format = (fourcc) I420"

static GstStaticPadTemplate source_template =
    GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC,
        GST_PAD_ALWAYS,
        GST_STATIC_CAPS(SOURCE_CAPS));

//#define DEBUG_OUTPUT
//#define VERBOSE_DEBUG

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (VideoDecoder, videodecoder, BaseDecoder, TYPE_BASEDECODER);
 ***********************************************************************************/
static void videodecoder_base_init(gpointer g_class);
static void videodecoder_class_init(VideoDecoderClass *g_class);
static void videodecoder_init(VideoDecoder *object, VideoDecoderClass *g_class);

static GstElementClass *parent_class = NULL;

static void videodecoder_class_init_trampoline(gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *) g_type_class_peek_parent(g_class);
    videodecoder_class_init((VideoDecoderClass *) g_class);
}

GType videodecoder_get_type(void)
{
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter(&gonce_data))
    {
        GType _type = gst_type_register_static_full(TYPE_BASEDECODER,
                g_intern_static_string("VideoDecoder"),
                sizeof (VideoDecoderClass),
                videodecoder_base_init,
                NULL,
                videodecoder_class_init_trampoline,
                NULL,
                NULL,
                sizeof (VideoDecoder),
                0,
                (GInstanceInitFunc) videodecoder_init,
                NULL,
                (GTypeFlags) 0);

        g_once_init_leave(&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
* Base init
***********************************************************************************/

static void videodecoder_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class,
                "Videodecoder",
                "Codec/Decoder/Video",
                "Decode video stream",
                "Oracle Corporation");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&source_template));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_template));
}

/***********************************************************************************
 * Calss and instance init and forward declarations
 ***********************************************************************************/
static GstStateChangeReturn videodecoder_change_state(GstElement* element, GstStateChange transition);
static gboolean             videodecoder_sink_event(GstPad *pad, GstEvent *event);
static GstFlowReturn        videodecoder_chain(GstPad *pad, GstBuffer *buf);

static void                 videodecoder_init_state(VideoDecoder *decoder);
static void                 videodecoder_state_reset(VideoDecoder *decoder);

static void videodecoder_class_init(VideoDecoderClass *klass)
{
    GST_ELEMENT_CLASS(klass)->change_state = videodecoder_change_state;
}

static void videodecoder_init(VideoDecoder *decoder, VideoDecoderClass *gclass)
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
    GstStateChangeReturn ret = parent_class->change_state(element, transition);
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
static gboolean videodecoder_sink_event(GstPad *pad, GstEvent *event)
{
    VideoDecoder *decoder = VIDEODECODER(GST_PAD_PARENT(pad));

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

#ifdef DEBUG_OUTPUT
        case GST_EVENT_NEWSEGMENT:
        {
            GstFormat format;
            gboolean update;
            gdouble rate, applied_rate;
            gint64 start, stop, time;

            gst_event_parse_new_segment_full (event, &update, &rate, &applied_rate, &format, &start, &stop, &time);
            g_print("videodecoder_sink_event: NEW_SEGMENT update=%s, rate=%.1f, format=%d, start=%.3f, stop=%.3f, time=%.3f\n",
                    update ? "TRUE" : "FALSE", rate, format, (double)start/GST_SECOND, (double)stop/GST_SECOND, (double)time/GST_SECOND);

            break;
        }
#endif // DEBUG_OUTPUT

        default:
            break;
    }

    return gst_pad_push_event(BASEDECODER(decoder)->srcpad, event);
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

    basedecoder_init_state(BASEDECODER(decoder));
}

static gboolean videodecoder_configure(VideoDecoder *decoder, GstCaps *sink_caps)
{
    BaseDecoder *base = BASEDECODER(decoder);

    if(gst_caps_get_size(sink_caps) < 1)
        return FALSE;

    GstStructure *s = gst_caps_get_structure(sink_caps, 0);

    // Pass stencil context to init against if there is one.
    basedecoder_set_codec_data(base, s);

#if NEW_CODEC_ID
    base->is_initialized = basedecoder_open_decoder(BASEDECODER(decoder), AV_CODEC_ID_H264);
#else
    base->is_initialized = basedecoder_open_decoder(BASEDECODER(decoder), CODEC_ID_H264);
#endif    
    return base->is_initialized;
}

static void videodecoder_state_reset(VideoDecoder *decoder)
{
    decoder->frame_finished = 1;
    basedecoder_flush(BASEDECODER(decoder));
}

static gboolean videodecoder_configure_sourcepad(VideoDecoder *decoder)
{
    BaseDecoder *base = BASEDECODER(decoder);

#if NEW_CODEC_ID
    int width = base->frame->width;
    int height = base->frame->height;
#else
    int width = base->context->width;
    int height = base->context->height;
#endif // NEW_CODEC_ID 
    
    if (GST_PAD_CAPS(base->srcpad) == NULL ||
        decoder->width != width || decoder->height != height)
    {
        decoder->width = width;
        decoder->height = height;

        decoder->discont = (GST_PAD_CAPS(base->srcpad) != NULL);

        decoder->u_offset = base->frame->linesize[0] * decoder->height;
        decoder->uv_blocksize = base->frame->linesize[1] * decoder->height / 2;

        decoder->v_offset = decoder->u_offset + decoder->uv_blocksize;
        decoder->frame_size = (base->frame->linesize[0] + base->frame->linesize[1]) * decoder->height;

        GstCaps *src_caps = gst_caps_new_simple("video/x-raw-yuv",
                                                "format", GST_TYPE_FOURCC, GST_STR_FOURCC("YV12"),
                                                "width", G_TYPE_INT, decoder->width,
                                                "height", G_TYPE_INT, decoder->height,
                                                "stride-y", G_TYPE_INT, base->frame->linesize[0],
                                                "stride-u", G_TYPE_INT, base->frame->linesize[1],
                                                "stride-v", G_TYPE_INT, base->frame->linesize[2],
                                                "offset-y", G_TYPE_INT, 0,
                                                "offset-u", G_TYPE_INT, decoder->u_offset,
                                                "offset-v", G_TYPE_INT, decoder->v_offset,
                                                "framerate", GST_TYPE_FRACTION, 2997, 100,
                                                NULL);


        if (!gst_pad_set_caps (base->srcpad, src_caps))
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_NEGOTIATION,
                                     g_strdup("Failed to set caps on the sourcepad"), NULL,
                                     ("videodecoder.c"), ("videodecoder_configure"), 0);
            gst_caps_unref(src_caps);
            return FALSE;
        }
        gst_caps_unref(src_caps);
    }

    return TRUE;
}
/***********************************************************************************
 * chain
 ***********************************************************************************/
static GstFlowReturn videodecoder_chain(GstPad *pad, GstBuffer *buf)
{
    VideoDecoder  *decoder = VIDEODECODER(GST_PAD_PARENT(pad));
    BaseDecoder   *base = BASEDECODER(decoder);
    GstFlowReturn  result = GST_FLOW_OK;
    int            num_dec = NO_DATA_USED;

    if (base->is_flushing)  // Reject buffers in flushing state.
    {
        result = GST_FLOW_WRONG_STATE;
        goto _exit;
    }

    if (!base->is_initialized && !videodecoder_configure(decoder, GST_PAD_CAPS(pad)))
    {
        result = GST_FLOW_ERROR;
        goto _exit;
    }

    if (!base->is_hls)
    {
        if (av_new_packet(&decoder->packet, GST_BUFFER_SIZE(buf)) == 0)
        {
            memcpy(decoder->packet.data, GST_BUFFER_DATA(buf), GST_BUFFER_SIZE(buf));
            if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
                base->context->reordered_opaque = GST_BUFFER_TIMESTAMP(buf);
            else
                base->context->reordered_opaque = AV_NOPTS_VALUE;
            num_dec = avcodec_decode_video2(base->context, base->frame, &decoder->frame_finished, &decoder->packet);
            av_free_packet(&decoder->packet);
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
        decoder->packet.data = GST_BUFFER_DATA(buf);
        decoder->packet.size = GST_BUFFER_SIZE(buf);
        if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
            base->context->reordered_opaque = GST_BUFFER_TIMESTAMP(buf);
        else
            base->context->reordered_opaque = AV_NOPTS_VALUE;

        num_dec = avcodec_decode_video2(base->context, base->frame, &decoder->frame_finished, &decoder->packet);
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
            GstBuffer *outbuf = NULL;
            result = gst_pad_alloc_buffer_and_set_caps(base->srcpad, base->context->frame_number,
                                                       decoder->frame_size, GST_PAD_CAPS(base->srcpad), &outbuf);
            if (result != GST_FLOW_OK)
            {
                if (result != GST_FLOW_WRONG_STATE)
                {
                    gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR,
                                             GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                             ("Decoded video buffer allocation failed"), NULL,
                                             ("videodecoder.c"), ("videodecoder_chain"), 0);
                }
            }
            else
            {
                if (base->frame->reordered_opaque != AV_NOPTS_VALUE)
                {
                    GST_BUFFER_TIMESTAMP(outbuf) = base->frame->reordered_opaque;
                    GST_BUFFER_DURATION(outbuf) = GST_BUFFER_DURATION(buf); // Duration for video usually same
                }
                GST_BUFFER_SIZE(outbuf) = decoder->frame_size;

                // Copy image by parts from different arrays.
                memcpy(GST_BUFFER_DATA(outbuf),                     base->frame->data[0], decoder->u_offset);
                memcpy(GST_BUFFER_DATA(outbuf) + decoder->u_offset, base->frame->data[1], decoder->uv_blocksize);
                memcpy(GST_BUFFER_DATA(outbuf) + decoder->v_offset, base->frame->data[2], decoder->uv_blocksize);

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
                g_print("videodecoder: pushing buffer ts=%.4f sec", (double)GST_BUFFER_TIMESTAMP(outbuf)/GST_SECOND);
#endif
                result = gst_pad_push(base->srcpad, outbuf);
#ifdef VERBOSE_DEBUG
                g_print(" done, res=%s\n", gst_flow_get_name(result));
#endif
            }
        }
    }

_exit:
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
