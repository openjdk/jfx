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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>
#include <string.h>

#include "vp6decoder.h"


GST_DEBUG_CATEGORY_EXTERN (fxm_plugin_debug);
#define GST_CAT_DEFAULT fxm_plugin_debug

#define ENABLE_POST_PROCESSING  FALSE

#define TMP_INPUT_BUF_INITIAL_SIZE 65536
#define TMP_INPUT_BUF_PADDING      64

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("video/x-vp6; video/x-vp6-flash; video/x-vp6-alpha")
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("video/x-raw-yuv, format=(fourcc)YV12; "
                     "video/x-raw-yvua420p, format=(fourcc)YVUA")
    );


/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (VP6Decoder, vp6decoder, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void vp6decoder_base_init  (gpointer         g_class);
static void vp6decoder_class_init (VP6DecoderClass *g_class);
static void vp6decoder_init       (VP6Decoder      *object,
                                   VP6DecoderClass *g_class);
static GstElementClass *parent_class = NULL;

static void vp6decoder_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *)  g_type_class_peek_parent (g_class);
    vp6decoder_class_init ((VP6DecoderClass *)g_class);
}

GType vp6decoder_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
                                               g_intern_static_string ("VP6Decoder"),
                                               sizeof (VP6DecoderClass),
                                               vp6decoder_base_init,
                                               NULL,
                                               vp6decoder_class_init_trampoline,
                                               NULL,
                                               NULL,
                                               sizeof (VP6Decoder),
                                               0,
                                               (GInstanceInitFunc) vp6decoder_init,
                                               NULL,
                                               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/***********************************************************************************
 * Init stuff
 ***********************************************************************************/
static GstStateChangeReturn vp6decoder_change_state (GstElement* element, GstStateChange transition);

static gboolean vp6decoder_sink_event (GstPad * pad, GstEvent * event);
static gboolean vp6decoder_set_caps (GstPad * pad, GstCaps * caps);
static GstFlowReturn vp6decoder_chain (GstPad * pad, GstBuffer * buf);

static const GstQueryType *vp6decoder_src_query_types (GstPad * pad);
static gboolean vp6decoder_src_query (GstPad * pad, GstQuery * query);
static gboolean vp6decoder_src_event (GstPad * pad, GstEvent * event);

/* QoS helper functions */
static void vp6decoder_reset_qos(VP6Decoder *filter);
static void vp6decoder_update_qos(VP6Decoder *filter, gdouble proportion,
        GstClockTimeDiff diff, GstClockTime timestamp);
static gboolean vp6decoder_do_qos(VP6Decoder* filter, GstClockTime timestamp, gboolean is_keyframe);
static void destroy_vp6_decoders(VP6Decoder *filter);

static int parse_vp6alpha_header(guint8 *packet, int packetSize, guint8 **colorBitstream, int *colorBitstreamSize, guint8 **alphaBitstream, int *alphaBitstreamSize)
{
    /*
         What Google said about the vp6a bitstream header:
         1 byte w/h offset
         3 byte offset to alpha bitstream
         n bytes yv12 bitstream
         n bytes alpha bitstream


         What I discovered:
         1 byte, always zero (maybe w/h offset as above)
         2 byte offset to alpha bitstream
         n bytes yv12 bitstream
         n bytes alpha bitstream

         Bit streams seem to be video/x-vp6, not video/x-vp6-flash
     */
    int offset = (packet[1] << 8) | packet[2];

    // Not sure what other error detection I could do here...
    if (offset + 3 < packetSize) {
        *colorBitstream = packet + 3;
        *colorBitstreamSize = offset - 3; // offset is first partition size
        *alphaBitstream = packet + offset + 3;
        *alphaBitstreamSize = packetSize - (offset + 3);
#if 0
        {
            guint8 *ptr;
            fprintf(stderr, "vp6a packet %p, size %d\n", packet, packetSize);
            fprintf(stderr, " header: %02x %02x %02x\n", packet[0], packet[1], packet[2]);
            ptr = *colorBitstream;
            fprintf(stderr, " color header: %02x %02x %02x %02x %02x %02x %02x %02x\n", ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7]);
            ptr = *alphaBitstream;
            fprintf(stderr, " alpha header: %02x %02x %02x %02x %02x %02x %02x %02x\n", ptr[0], ptr[1], ptr[2], ptr[3], ptr[4], ptr[5], ptr[6], ptr[7]);
            fprintf(stderr, " - width offset %d, height offset %d\n", (packet[0] >> 4) & 0x0f, packet[0] & 0x0f);
            fprintf(stderr, " - alpha offset %d (%x)\n", offset, offset);
        }
#endif
        return 1;
    }

    return 0;
}

/*
 Probe a packet to determine which
 */
static on2_codec_iface_t *probe_vp6_packet(guint8 *packet, int packetSize, gint *stream_width, gint *stream_height)
{
    on2_codec_err_t ret;
    on2_codec_stream_info_t info;
    info.sz = sizeof(on2_codec_stream_info_t);

    // Try each known interface until we get a hit
    // vp6
    ret = on2_codec_peek_stream_info(&on2_codec_vp6_algo, packet, (unsigned int)packetSize, &info);
    if (ON2_CODEC_OK == ret) {
        GST_DEBUG("[probe] Using vp6 decoder\nstream info: w=%u, h=%u, is_kf=%u", info.w, info.h, info.is_kf);

        *stream_width = info.w;
        *stream_height = info.h;

        return &on2_codec_vp6_algo;
    }

    // vp6f
    ret = on2_codec_peek_stream_info(&on2_codec_vp6f_algo, packet, (unsigned int)packetSize, &info);
    if (ON2_CODEC_OK == ret) {
        GST_DEBUG("[probe] Using vp6f decoder\nstream info: w=%u, h=%u, is_kf=%u", info.w, info.h, info.is_kf);

        *stream_width = info.w;
        *stream_height = info.h;

        return &on2_codec_vp6f_algo;
    }

    return NULL;
}

static gboolean check_vp6_decoders(VP6Decoder *filter, guint8 *probePacket, int packetSize)
{
    guint8 *cb = probePacket;
    int cbSize = packetSize;
    on2_codec_iface_t *vp6Algo = NULL, *vp6aAlgo = NULL;
    gint stream_width = 0, stream_height = 0;
    gint alpha_stream_width = 0, alpha_stream_height = 0;

    if (filter->decodeAlpha) {
        guint8 *ab;
        int abSize;

        // get pointer to color bitstream data, we'll use that to probe
        if (!parse_vp6alpha_header(cb, cbSize, &cb, &cbSize, &ab, &abSize)) {
            GST_ERROR("Bad vp6a bitstream detected!");
            return FALSE;
        }

        vp6aAlgo = probe_vp6_packet(ab, abSize, &alpha_stream_width, &alpha_stream_height);
        if (!vp6aAlgo) {
            GST_ERROR("No supported vp6a bitstream detected!");
            return FALSE;
        }
    }

    vp6Algo = probe_vp6_packet(cb, cbSize, &stream_width, &stream_height);
    if (!vp6Algo) {
        GST_ERROR("No supported vp6 bitstream detected!");
        return FALSE;
    }

    // Decoder does not exist or frame size has changed - we need to recreate decoder
    if (!filter->decoder) {
        if (filter->decodeAlpha) {
            if (vp6Algo != vp6aAlgo || stream_width != alpha_stream_width || stream_height != alpha_stream_height) {
                GST_ERROR("Color and alpha bitsreams must have the same parameters!");
                return FALSE;
            }

            filter->alphaDecoder = (on2_codec_ctx_t *)g_malloc(sizeof(on2_codec_ctx_t));
            if (!filter->alphaDecoder) {
                GST_ERROR("Unable to allocate decoder context!");
                return FALSE;
            }

            if (on2_codec_dec_init(filter->alphaDecoder, vp6aAlgo, NULL, ENABLE_POST_PROCESSING ? ON2_CODEC_USE_POSTPROC : 0)) {
                GST_ERROR("Failed to initialize alpha decoder: %s", on2_codec_error(filter->alphaDecoder));
                return FALSE;
            }
        }

        filter->decoder = (on2_codec_ctx_t *)g_malloc(sizeof(on2_codec_ctx_t));
        if (!filter->decoder) {
            GST_ERROR("Unable to allocate decoder context!");
            return FALSE;
        }

        if (on2_codec_dec_init(filter->decoder, vp6Algo, NULL, ENABLE_POST_PROCESSING ? ON2_CODEC_USE_POSTPROC : 0)) {
            GST_ERROR("Failed to initialize decoder: %s", on2_codec_error(filter->decoder));
            return FALSE;
        }
    }
    else if ((stream_width > 0 && stream_width != filter->encoded_width) || 
             (stream_height > 0 && stream_height != filter->encoded_height) ||
             vp6Algo != filter->decoder->iface || 
             filter->alphaDecoder && ((alpha_stream_width > 0 && alpha_stream_width != filter->encoded_width) || 
                                      (alpha_stream_height > 0 && alpha_stream_height != filter->encoded_height) ||
                                      vp6aAlgo != filter->alphaDecoder->iface))    
    {
        GST_ERROR("Dynamic resolution or interface are not supported!");
        return FALSE;
    }

    return TRUE;
}

static void destroy_vp6_decoders(VP6Decoder *filter)
{
    if (filter->decoder) {
        on2_codec_destroy(filter->decoder);
        g_free(filter->decoder);
        filter->decoder = NULL;
    }

    if (filter->alphaDecoder) {
        on2_codec_destroy(filter->alphaDecoder);
        g_free(filter->alphaDecoder);
        filter->alphaDecoder = NULL;
    }
}

static void
vp6decoder_base_init (gpointer gclass)
{
    //fprintf(stderr, "===vp6decoder_base_init()\n");
    GstElementClass *element_class = GST_ELEMENT_CLASS (gclass);

    gst_element_class_set_details_simple(element_class,
    "VP6Decoder",
    "Codec/Decoder/Video",
    "ON2 based VP6 decoder",
    "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&src_factory));
    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&sink_factory));
}

/* initialize the vp6decoderfilter's class */
static void
vp6decoder_class_init (VP6DecoderClass * klass)
{
    //fprintf(stderr, "===vp6decoder_class_init()\n");
    GObjectClass *gobject_class = NULL;
    GstElementClass *gstelement_class = NULL;

    gobject_class = (GObjectClass *) klass;
    gstelement_class = (GstElementClass *) klass;

    gstelement_class->change_state = vp6decoder_change_state;
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void
vp6decoder_init (VP6Decoder * filter, VP6DecoderClass * gclass)
{
    //fprintf(stderr, "===vp6decoder_init()\n");

    //Create sink
    filter->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
    gst_pad_set_event_function (filter->sinkpad, vp6decoder_sink_event);
    gst_pad_set_setcaps_function (filter->sinkpad, vp6decoder_set_caps);
    gst_pad_set_chain_function (filter->sinkpad, vp6decoder_chain);

    gst_element_add_pad (GST_ELEMENT (filter), filter->sinkpad);

    //Create src
    filter->srcpad = gst_pad_new_from_static_template (&src_factory, "src");
    gst_pad_set_query_type_function (filter->srcpad, vp6decoder_src_query_types);
    gst_pad_set_query_function (filter->srcpad, vp6decoder_src_query);
    gst_pad_set_event_function (filter->srcpad, vp6decoder_src_event);
    gst_pad_use_fixed_caps (filter->srcpad);

    gst_element_add_pad (GST_ELEMENT (filter), filter->srcpad);

    //Set defaults
    filter->need_set_caps = TRUE;
    filter->width = 0;
    filter->height = 0;
    filter->encoded_width = 0;
    filter->encoded_height = 0;
    filter->have_par = FALSE;
    filter->par_num = 0;
    filter->par_den = 0;
    filter->framerate_num = 0;
    filter->framerate_den = 0;
    filter->tmp_input_buf = NULL;
    filter->tmp_input_buf_size = 0;

    filter->decoder = NULL;
    filter->alphaDecoder = NULL;

    //Segment
    gst_segment_init(&filter->segment, GST_FORMAT_TIME);

    //QoS
    vp6decoder_reset_qos(filter);
}

static GstStateChangeReturn vp6decoder_change_state (GstElement* element, GstStateChange transition)
{
    VP6Decoder *filter = NULL;
    GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;

    filter = VP6_DECODER (element);
    switch (transition) {
        case GST_STATE_CHANGE_NULL_TO_READY: {
            // make sure we don't have any stale decoder contexts
            destroy_vp6_decoders(filter);

            // Just in case free previous buffer
            if (filter->tmp_input_buf)
            {
                g_free((gpointer)filter->tmp_input_buf);
                filter->tmp_input_buf = NULL;
                filter->tmp_input_buf_size = 0;
            }
            filter->tmp_input_buf = (guint8*)g_malloc(TMP_INPUT_BUF_INITIAL_SIZE + TMP_INPUT_BUF_PADDING);
            filter->tmp_input_buf_size = TMP_INPUT_BUF_INITIAL_SIZE + TMP_INPUT_BUF_PADDING;
            if (filter->tmp_input_buf == NULL)
                return GST_STATE_CHANGE_FAILURE;
        }
            break;
        case GST_STATE_CHANGE_READY_TO_PAUSED:
            filter->need_set_caps = TRUE;
            break;
        case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
            vp6decoder_reset_qos(filter);
            break;
        default:
            break;
    }

    ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);
    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    switch (transition) {
        case GST_STATE_CHANGE_READY_TO_NULL:
            // Free temp buffer
            if (filter->tmp_input_buf) {
                g_free((gpointer)filter->tmp_input_buf);
                filter->tmp_input_buf = NULL;
                filter->tmp_input_buf_size = 0;
            }

            // Destroy decoder
            destroy_vp6_decoders(filter);
            break;
        default:
            break;
    }

    return ret;
}

static gboolean vp6decoder_sink_event (GstPad * pad, GstEvent * event)
{
    gboolean ret = FALSE;
    VP6Decoder *filter = NULL;

    //fprintf(stderr, "===vp6decoder_sink_event() : %s\n",  gst_event_type_get_name(GST_EVENT_TYPE(event)));

    filter = VP6_DECODER (GST_OBJECT_PARENT (pad));
    switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_FLUSH_START:
        //fprintf(stderr, "===vp6decoder_sink_event() : GST_EVENT_FLUSH_START\n");
        ret = gst_pad_push_event (filter->srcpad, event);
        break;
    case GST_EVENT_FLUSH_STOP:
        //fprintf(stderr, "===vp6decoder_sink_event() : GST_EVENT_FLUSH_STOP\n");
        ret = gst_pad_push_event (filter->srcpad, event);
        break;
    case GST_EVENT_EOS:
        //fprintf(stderr, "===vp6decoder_sink_event() : GST_EVENT_EOS\n");
        ret = gst_pad_push_event (filter->srcpad, event);
        break;
    case GST_EVENT_NEWSEGMENT: {
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
        vp6decoder_reset_qos(filter);
        ret = gst_pad_push_event (filter->srcpad, event);
        break;
    }
    default:
        ret = gst_pad_push_event (filter->srcpad, event);
        break;
    }
    return ret;
}

/* this function handles the link with other elements */
static gboolean vp6decoder_set_caps (GstPad * pad, GstCaps * caps)
{
    VP6Decoder *filter = NULL;
    GstStructure *s = NULL;
    const gchar *sinkCapsName = NULL;

    filter = VP6_DECODER (GST_OBJECT_PARENT (pad));
    if (pad == filter->sinkpad) {
        s = gst_caps_get_structure (caps, 0);

        // Check if we have alpha
        sinkCapsName = gst_structure_get_name(s);
        if (sinkCapsName != NULL)
            filter->decodeAlpha = (NULL != strstr("video/x-vp6-alpha", sinkCapsName));

        //Check if we have par
        filter->have_par = gst_structure_get_fraction (s, "pixel-aspect-ratio",
          &filter->par_num, &filter->par_den);
        //parse the par. If the par isn't specified by sink caps, use 1:1
        if (!filter->have_par) {
            filter->par_num = 1;
            filter->par_den = 1;
        }

        // Check if we have framerate. If the framerate isn't specified by sink caps, use 25 fps
        if (!gst_structure_get_fraction (s, "framerate", &filter->framerate_num, &filter->framerate_den))
        {
            filter->framerate_num = 25;
            filter->framerate_den = 1;
        }

        // Get dimensions
        if (!gst_structure_get_int (s, "width", &filter->width))
            filter->width = 0;

        if (!gst_structure_get_int (s, "height", &filter->height))
            filter->height = 0;

        // Calculate encoded sizes from width, height
        filter->encoded_width = filter->width + (filter->width & 1);
        filter->encoded_height = filter->height + (filter->height & 1);

        //Raise flag to change caps on next frame
        filter->need_set_caps = TRUE;
    }

    return TRUE;
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn vp6decoder_chain (GstPad * pad, GstBuffer * buf)
{
    VP6Decoder *filter = NULL;
    GstBuffer *out = NULL;
    GstCaps *caps = NULL;
    guint8 *colorBits = NULL;
    int colorSize = 0;
    guint8 *alphaBits = NULL;
    int alphaSize = 0;
    gint i = 0;
    ADDR_ALIGN on2_image_t *img = NULL;
    ADDR_ALIGN on2_image_t *alphaImg = NULL;
    on2_codec_iter_t iter = NULL;
    gint result = GST_FLOW_OK;
    ADDR_ALIGN gint src_len = 0;

    gint stride_y = 0;
    gint stride_v = 0;
    gint stride_u = 0;
    gint stride_a = 0;

    gint out_size = 0;          //Output buffer size in bytes
    gint out_size_y = 0;        //Buffer size for Y component
    gint out_size_v = 0;       //Buffer size for U&V components
    gint out_size_u = 0;
    gint out_size_a = 0;

    gint offset_y = 0;      //Offset of topmost Y row from the beginning of buffer
    gint offset_u = 0;      //Offset of topmost U row from the beginning of buffer
    gint offset_v = 0;      //Offset of topmost V row from the beginning of buffer
    gint offset_a = 0;      //Offset of topmost A row from the beginning of buffer

    guchar *dest = NULL;        //Destination buffer
    gboolean is_keyframe = FALSE;

    filter = VP6_DECODER (GST_OBJECT_PARENT (pad));

    is_keyframe = !GST_BUFFER_FLAG_IS_SET(buf, GST_BUFFER_FLAG_DELTA_UNIT);

    // probe and allocate proper decoder context
    if (!check_vp6_decoders(filter, GST_BUFFER_DATA(buf), GST_BUFFER_SIZE(buf))) {
// INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);

        gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to check or create decoder context!"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
        return GST_FLOW_ERROR;
    }

    /* Update QoS data and do QoS */
    if (!vp6decoder_do_qos(filter, GST_BUFFER_TIMESTAMP(buf), is_keyframe)) {
        filter->qos_discont = TRUE;
        //fprintf(stderr, "vp6decoder: QoS skipping frame\n");
// INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
        return GST_FLOW_OK;
    }

    /* Workaround for VP6 decoder buffer underflow bug:
     * copy data to intermediate buffer of larger size */
    src_len = GST_BUFFER_SIZE(buf);
    if (filter->tmp_input_buf_size < (src_len + TMP_INPUT_BUF_PADDING))
    {
      filter->tmp_input_buf = (guint8*)g_realloc((gpointer)filter->tmp_input_buf, (src_len + TMP_INPUT_BUF_PADDING));
      filter->tmp_input_buf_size = (src_len + TMP_INPUT_BUF_PADDING);
    }

    if (!filter->tmp_input_buf)
    {
        GST_ERROR("Unable to reallocate tmp buffer");
        gst_buffer_unref (buf);
        return GST_FLOW_ERROR;
    }

    memcpy(filter->tmp_input_buf, GST_BUFFER_DATA (buf), src_len);
    memset(filter->tmp_input_buf+src_len, 0, (filter->tmp_input_buf_size - src_len));

    if (filter->decodeAlpha) {
        if (!parse_vp6alpha_header(filter->tmp_input_buf, src_len, &colorBits, &colorSize, &alphaBits, &alphaSize)) {
// INLINE - gst_buffer_unref()
            gst_buffer_unref (buf);

            gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to parse VP6A header"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
            return GST_FLOW_ERROR;
        }
    } else {
        colorBits = filter->tmp_input_buf;
        colorSize = src_len;
    }

    //Decode frame
    if (on2_codec_decode(filter->decoder, colorBits, colorSize, NULL, 0)) {
// INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
        
        gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to decode VP6 stream"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
        return GST_FLOW_ERROR;
    }
    if (filter->decodeAlpha) {
        if (on2_codec_decode(filter->alphaDecoder, alphaBits, alphaSize, NULL, 0)) {
// INLINE - gst_buffer_unref()
            gst_buffer_unref (buf);
            gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to decode VP6A stream"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
            return GST_FLOW_ERROR;
        }
    }

    img = on2_codec_get_frame(filter->decoder, &iter);
    if (img == 0) {
// INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
        gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("VP6 internal error"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
        return GST_FLOW_ERROR;
    }
    // unflip images if necessary
    if (img->stride[PLANE_Y] < 0) {
        // on2_img_flip only adjust strides and pointers, it leaves memory intact
        // so this is not an expensive operation and it make further math a lot easier to deal with...
        on2_img_flip(img);
    }

    if (filter->decodeAlpha) {
        iter = NULL;
        alphaImg = on2_codec_get_frame(filter->alphaDecoder, &iter);
        if (alphaImg == 0) {
// INLINE - gst_buffer_unref()
            gst_buffer_unref (buf);
            gst_element_message_full(GST_ELEMENT(filter), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("VP6 internal error"), NULL, ("vp6decoder.c"), ("vp6decoder_chain"), 0);
            return GST_FLOW_ERROR;
        }
        if (alphaImg->stride[PLANE_Y] < 0) {
            on2_img_flip(alphaImg);
        }
    }

    if (filter->height == 0)
        filter->height = (gint)img->d_h;
 
    if ((filter->encoded_height == 0) || (filter->encoded_height != (gint)img->d_h)) {
        filter->encoded_height = img->d_h + (img->d_h & 1); // add a row for odd height
        filter->need_set_caps = TRUE;
    }
     
    if (filter->width == 0)
        filter->width = (gint)img->d_w;
 
    if ((filter->encoded_width == 0) || (filter->encoded_width != (gint)img->d_w)) { 
        filter->encoded_width = img->d_w + (img->d_w & 1); // add a pixel for odd width
        filter->need_set_caps = TRUE;
    }

    stride_y = img->stride[PLANE_Y];
    stride_v = img->stride[PLANE_V];
    stride_u = img->stride[PLANE_U];
    if (alphaImg) {
        stride_a = alphaImg->stride[PLANE_Y];
    }

    out_size_y = stride_y * filter->encoded_height;
    out_size_v = stride_v * filter->encoded_height / 2;
    out_size_u = stride_u * filter->encoded_height / 2;
    out_size_a = stride_a * filter->encoded_height;

    // if we have alpha, then place it immediately after luma so we can do a single LUMA+ALPHA texture update in prism
    offset_y = 0;
    if (alphaImg) {
        offset_a = offset_y + out_size_y;
        offset_v = offset_a + out_size_a;
        offset_u = offset_v + out_size_v;
    } else {
        offset_v = offset_y + out_size_y;
        offset_u = offset_v + out_size_v;
        offset_a = 0;
    }

    // check if any settings changed
    // base purely on size since it will represent changes in both offset and stride
    if ((filter->plane_size[PLANE_Y] != out_size_y)
        || (filter->plane_size[PLANE_V] != out_size_v)
        || (filter->plane_size[PLANE_U] != out_size_u)
        || (filter->plane_size[PLANE_ALPHA] != out_size_a))
    {
        filter->plane_size[PLANE_Y] = out_size_y;
        filter->plane_size[PLANE_V] = out_size_v;
        filter->plane_size[PLANE_U] = out_size_u;
        filter->plane_size[PLANE_ALPHA] = out_size_a;
        filter->need_set_caps = TRUE;
    }

    if (filter->need_set_caps) {
        if (filter->decodeAlpha) {
            caps = gst_caps_new_simple ("video/x-raw-yvua420p",
                                        "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC('Y','V','U','A'),
                                        "framerate", GST_TYPE_FRACTION, filter->framerate_num, filter->framerate_den,
                                        "pixel-aspect-ratio", GST_TYPE_FRACTION, filter->par_num, filter->par_den,

                                        "offset-y", G_TYPE_INT, offset_y,
                                        "offset-v", G_TYPE_INT, offset_v,
                                        "offset-u", G_TYPE_INT, offset_u,
                                        "offset-a", G_TYPE_INT, offset_a,

                                        "stride-y", G_TYPE_INT, stride_y,
                                        "stride-v", G_TYPE_INT, stride_v,
                                        "stride-u", G_TYPE_INT, stride_u,
                                        "stride-a", G_TYPE_INT, stride_a,

                                        "width", G_TYPE_INT, filter->width,
                                        "height", G_TYPE_INT, filter->height,

                                        "encoded-width", G_TYPE_INT, filter->encoded_width,
                                        "encoded-height", G_TYPE_INT, filter->encoded_height,

                                        NULL);
        } else {
            caps = gst_caps_new_simple ("video/x-raw-yuv",
                                        "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('Y', 'V', '1', '2'),
                                        "framerate", GST_TYPE_FRACTION, filter->framerate_num, filter->framerate_den,
                                        "pixel-aspect-ratio", GST_TYPE_FRACTION, filter->par_num, filter->par_den,

                                        "offset-y", G_TYPE_INT, offset_y,
                                        "offset-v", G_TYPE_INT, offset_v,
                                        "offset-u", G_TYPE_INT, offset_u,

                                        "stride-y", G_TYPE_INT, stride_y,
                                        "stride-v", G_TYPE_INT, stride_v,
                                        "stride-u", G_TYPE_INT, stride_u,

                                        "width", G_TYPE_INT, filter->width,
                                        "height", G_TYPE_INT, filter->height,

                                        "encoded-width", G_TYPE_INT, filter->encoded_width,
                                        "encoded-height", G_TYPE_INT, filter->encoded_height,

                                        NULL);
        }

        gst_pad_set_caps (filter->srcpad, caps);
        gst_caps_unref (caps);

        filter->need_set_caps = FALSE;
    }

    out_size = out_size_y + out_size_v + out_size_u + out_size_a;

    /* now copy over the area contained in offset_x,offset_y,
    * frame_width, frame_height */
    result = gst_pad_alloc_buffer_and_set_caps (filter->srcpad, GST_BUFFER_OFFSET_NONE,
        out_size, GST_PAD_CAPS (filter->srcpad), &out);

    if (result == GST_FLOW_OK) {
        dest = GST_BUFFER_DATA (out);
        GST_BUFFER_TIMESTAMP(out) = GST_BUFFER_TIMESTAMP(buf);
        GST_BUFFER_DURATION(out) = GST_BUFFER_DURATION(buf);
        GST_BUFFER_OFFSET(out) = GST_BUFFER_OFFSET(buf);
        GST_BUFFER_OFFSET_END(out) = GST_BUFFER_OFFSET_END(buf);
        if (filter->qos_discont || GST_BUFFER_FLAG_IS_SET(buf, GST_BUFFER_FLAG_DISCONT)) {
            GST_BUFFER_FLAG_SET(out, GST_BUFFER_FLAG_DISCONT);
            filter->qos_discont = FALSE;
        }

        memcpy(dest + offset_y, img->planes[PLANE_Y], out_size_y);
        memcpy(dest + offset_v, img->planes[PLANE_V], out_size_v);
        memcpy(dest + offset_u, img->planes[PLANE_U], out_size_u);

        if (alphaImg) {
            memcpy(dest + offset_a, alphaImg->planes[PLANE_Y], out_size_a);
        }

        //Push decoded frame
        result = gst_pad_push (filter->srcpad, out);
    }

    /* don't need it anymore now */
// INLINE - gst_buffer_unref()
    gst_buffer_unref (buf);

    return GST_FLOW_OK;
}

static const GstQueryType *
vp6decoder_src_query_types (GstPad * pad)
{
  static const GstQueryType vp6decoder_src_query_types[] = {
    GST_QUERY_POSITION,
    GST_QUERY_DURATION,
    GST_QUERY_CONVERT,
    GST_QUERY_NONE
  };

  return vp6decoder_src_query_types;
}

static gboolean vp6decoder_src_query (GstPad * pad, GstQuery * query)
{
    VP6Decoder *filter = NULL;
    GstPad *peer = NULL;
    gboolean res = FALSE;

    //fprintf(stderr, "===vp6decoder_src_query %s()\n",  gst_query_type_get_name(GST_QUERY_TYPE(query)));

    filter = VP6_DECODER (GST_OBJECT_PARENT (pad));

    if (!(peer = gst_pad_get_peer (filter->sinkpad))) {
        return FALSE;
    }

    /* forward to peer for total */
    res = gst_pad_query (peer, query);

    gst_object_unref (peer);

    return res;
}

static gboolean vp6decoder_src_event (GstPad * pad, GstEvent * event)
{
    VP6Decoder *filter = NULL;
    gboolean res = FALSE;
    gdouble proportion;
    GstClockTimeDiff diff;
    GstClockTime ts;

    //fprintf(stderr, "===vp6decoder_src_event() %s\n",  gst_event_type_get_name(GST_EVENT_TYPE(event)));

    filter = VP6_DECODER (GST_OBJECT_PARENT (pad));

    switch (GST_EVENT_TYPE(event)) {
    case GST_EVENT_QOS:
        gst_event_parse_qos(event, &proportion, &diff, &ts);
        vp6decoder_update_qos(filter, proportion, diff, ts);
        res = gst_pad_push_event (filter->sinkpad, event);
        break;
    default:
        res = gst_pad_push_event (filter->sinkpad, event);
    }
    return res;
}

/* QoS helper functions */
static void vp6decoder_reset_qos(VP6Decoder *filter)
{
    vp6decoder_update_qos(filter, 0.5, 0, GST_CLOCK_TIME_NONE);
    filter->frames_received = 0;
    filter->keyframes_received = 0;
    filter->delta_sequence = 0;
    filter->qos_dropping = 0;
}

static void vp6decoder_update_qos(VP6Decoder *filter, gdouble proportion,
        GstClockTimeDiff diff, GstClockTime timestamp)
{
    GST_OBJECT_LOCK(filter);

    filter->qos_proportion = proportion;
    filter->qos_diff = diff;
    filter->qos_timestamp = timestamp;

    GST_OBJECT_UNLOCK(filter);
}

static gboolean vp6decoder_do_qos(VP6Decoder* filter, GstClockTime timestamp, gboolean is_keyframe)
{
    GstClockTime segment_ts;
    int expected_delta;
    GstClockTime expected_keyframe_ts;
    GstClockTime qos_running_ts;

    /* Do QoS only while playing */
    if (GST_STATE(filter) != GST_STATE_PLAYING)
        return TRUE;

    segment_ts = gst_segment_to_position(&filter->segment, GST_FORMAT_TIME, timestamp);

    filter->frames_received++;
    if (is_keyframe) {
        /* Keyframes are never dropped */
        filter->keyframes_received++;
        filter->delta_sequence = 0;
        filter->qos_dropping = FALSE;
        return TRUE;
    }

    filter->delta_sequence++;

    /* If dropping frames already, continue dropping until next keyframe*/
    if (filter->qos_dropping)
        return FALSE;

    /* Don't do QoS if last frame arrived on time,
     * timestamp is outside of segment
     * or there's no valid QoS message
     * or no keyframes received yet*/
    if (filter->qos_diff <= 0
                || segment_ts == GST_CLOCK_TIME_NONE
                || filter->qos_timestamp == GST_CLOCK_TIME_NONE
                || filter->keyframes_received == 0)
        return TRUE;

    /* Calculate expected count of frames until keyframe */
    expected_delta = filter->frames_received / filter->keyframes_received + 15 - filter->delta_sequence;

    /* Calculate ts of expected keyframe */
    expected_keyframe_ts = expected_delta * filter->framerate_den * GST_SECOND /
                filter->framerate_num + segment_ts;

    /* Calculate running ts from QoS */
    qos_running_ts = filter->qos_timestamp + filter->qos_diff * 2;

    if (expected_keyframe_ts <= qos_running_ts && filter->qos_proportion >= 4.0) {
        filter->qos_dropping = TRUE;
        return FALSE;
    }

    return TRUE;
}
