/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <gst/gst.h>

#include "avcdecoder.h"

// Note: define as non-zero to enable warnings.
#define ENABLE_WARNINGS 1

/***************************************************************/

GST_DEBUG_CATEGORY_STATIC (avcdecoder_debug);
#define GST_CAT_DEFAULT avcdecoder_debug

/*
 * The input capabilities.
 */
static GstStaticPadTemplate sink_factory =
GST_STATIC_PAD_TEMPLATE ("sink",
                         GST_PAD_SINK,
                         GST_PAD_ALWAYS,
                         GST_STATIC_CAPS (
                         "video/x-h264; "
                         "video/x-h265") // Fake caps, so we can connect and post error
                         );

/*
 * The output capabilities.
 */
// Note: For 'yuvs' the format should be "format = (fourcc) YUY2"
static GstStaticPadTemplate src_factory =
GST_STATIC_PAD_TEMPLATE ("src",
                         GST_PAD_SRC,
                         GST_PAD_ALWAYS,
                         GST_STATIC_CAPS ("video/x-raw-ycbcr422, format = (string) UYVY")
                         );

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE (AvcDecoder, avcdecoder, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
#define avcdecoder_parent_class parent_class
static void avcdecoder_init          (AvcDecoder      *self);
static void avcdecoder_class_init    (AvcDecoderClass *klass);
static gpointer avcdecoder_parent_class = NULL;
static void     avcdecoder_class_intern_init (gpointer klass)
{
    avcdecoder_parent_class = g_type_class_peek_parent (klass);
    avcdecoder_class_init ((AvcDecoderClass*) klass);
}

GType avcdecoder_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (GST_TYPE_ELEMENT,
               g_intern_static_string ("AvcDecoder"),
               sizeof (AvcDecoderClass),
               (GClassInitFunc) avcdecoder_class_intern_init,
               sizeof(AvcDecoder),
               (GInstanceInitFunc) avcdecoder_init,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/*
 * Forward declarations.
 */
static GstStateChangeReturn avcdecoder_change_state (GstElement* element, GstStateChange transition);
static gboolean avcdecoder_sink_event (GstPad * pad, GstObject *parent, GstEvent * event);
static GstFlowReturn avcdecoder_chain (GstPad * pad, GstObject *parent, GstBuffer * buf);
static void avcdecoder_dispose(GObject* object);
static void avcdecoder_state_destroy(AvcDecoder *decode);

/* --- GObject vmethod implementations --- */

/*
 * Initialize avcdecoder's class.
 */
static void
avcdecoder_class_init (AvcDecoderClass * klass)
{
    GstElementClass *gstelement_class = (GstElementClass *) klass;
    GObjectClass *gobject_class = (GObjectClass*)klass;

    gst_element_class_set_metadata(gstelement_class,
                                         "AVCDecoder",
                                         "Codec/Decoder/Video",
                                         "Decode raw MPEG-4 H.264 video stream",
                                         "Oracle Corporation");

    gst_element_class_add_pad_template (gstelement_class,
                                        gst_static_pad_template_get (&src_factory));
    gst_element_class_add_pad_template (gstelement_class,
                                        gst_static_pad_template_get (&sink_factory));

    gstelement_class->change_state = avcdecoder_change_state;

    gobject_class->dispose = avcdecoder_dispose;
}

/*
 * Initialize the new element.
 * Instantiate pads and add them to element.
 * Set pad callback functions.
 * Initialize instance structure.
 */
static void
avcdecoder_init (AvcDecoder * decode)
{
    // Input.
    if (NULL == (decode->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink")))
    {
#if ENABLE_WARNINGS
        g_warning ("avcdecoder element failed to create sink pad!\n");
#endif
        return;
    }

    if (FALSE == gst_element_add_pad (GST_ELEMENT (decode), decode->sinkpad))
    {
#if ENABLE_WARNINGS
        g_warning ("avcdecoder element failed to add sink pad!\n");
#endif
    }

    gst_pad_set_chain_function (decode->sinkpad, GST_DEBUG_FUNCPTR(avcdecoder_chain));
    gst_pad_set_event_function(decode->sinkpad, avcdecoder_sink_event);

    // Output.
    if (NULL == (decode->srcpad = gst_pad_new_from_static_template (&src_factory, "src")))
    {
#if ENABLE_WARNINGS
        g_warning ("avcdecoder element failed to create sink pad!\n");
#endif
        return;
    }

    if (TRUE != gst_element_add_pad (GST_ELEMENT (decode), decode->srcpad))
    {
#if ENABLE_WARNINGS
        g_warning ("avcdecoder element failed to add source pad!\n");
#endif
    }

    gst_pad_use_fixed_caps (decode->srcpad);

    g_mutex_init(&decode->mutex);
}

static void
avcdecoder_dispose(GObject* object)
{
    AvcDecoder* decode = AVCDECODER(object);

    avcdecoder_state_destroy (decode);

    g_mutex_clear(&decode->mutex);

    G_OBJECT_CLASS(parent_class)->dispose(object);
}

/* --- GstElement vmethod implementations --- */

/*
 * GCompareDataFunc used to sort GstBuffers into order of ascending timestamp.
 */
static gint
avcdecoder_buffer_compare (gconstpointer a, gconstpointer b, gpointer user_data)
{
    gint ret = 0;

    if (NULL != a && NULL != b)
    {
        const GstBuffer* bufa = (const GstBuffer*)a;
        const GstBuffer* bufb = (const GstBuffer*)b;

        if (GST_BUFFER_TIMESTAMP_IS_VALID(bufa) && GST_BUFFER_TIMESTAMP_IS_VALID(bufb))
        {
            GstClockTime ta = GST_BUFFER_TIMESTAMP(bufa);
            GstClockTime tb = GST_BUFFER_TIMESTAMP(bufb);
            if (ta < tb)
            {
                ret = -1;
            }
            else if (ta > tb)
            {
                ret = 1;
            }
            // else ret = 0 by default.
        }
    }

    return ret;
}

/*
 * Callback which receives decoded video frames from the VDADecoder. The
 * decoded frames are not guaranteed to be in timestamp-order and it is
 * unknown how many frames there are between I-frames. Frames are pushed
 * in the order received to a GAsyncQueue. This data type is used as there
 * is no apparent way without causing a deadlock to lock a sorted queue or
 * sequence by both this callback and the function which sorts the frames
 * in timestamp-order.
 */
static void
avcdecoder_decoder_output_callback (void* userData,
                                    CFDictionaryRef frameInfo,
                                    OSStatus status,
                                    uint32_t infoFlags,
                                    CVImageBufferRef imageBuffer)
{
    AvcDecoder *decode = AVCDECODER (userData);

    if(decode->is_flushing)
    {
        return;
    }

    // Check whether there is a problem.

    gboolean isGap = FALSE;

    if (kVDADecoderNoErr != status)
    {
#if ENABLE_WARNINGS
        g_warning("output callback received status %d\n", (int)status);
#endif
        isGap = TRUE;
    } else if (1UL << 1 == (infoFlags & (1UL << 1))) // XXX hard-coding
    {
#if ENABLE_WARNINGS
        g_warning("output callback called on dropped frame\n");
#endif
        isGap = TRUE;
    } else if (NULL == imageBuffer)
    {
#if ENABLE_WARNINGS
        g_warning ("output callback received NULL image buffer!\n");
#endif
        isGap = TRUE;
    } else if ('2vuy' != CVPixelBufferGetPixelFormatType(imageBuffer))
    {
#if ENABLE_WARNINGS
        g_warning("output callback image buffer format not '2vuy'\n");
#endif
        isGap = TRUE;
    }

    // Retrieve the timestamp and delta flag.

    int64_t timestamp = 0;
    int32_t deltaFlag = 0; // deltaFlag == 0 indicates an intra-frame, non-zero an inter-frame.
    if (NULL != frameInfo)
    {
        CFNumberRef timestampRef = CFDictionaryGetValue(frameInfo, CFSTR("timestamp"));
        if (timestampRef)
        {
            CFNumberGetValue(timestampRef, kCFNumberSInt64Type, &timestamp);
        }
        CFNumberRef deltaFlagRef = CFDictionaryGetValue(frameInfo, CFSTR("deltaFlag"));
        if (deltaFlagRef)
        {
            CFNumberGetValue(deltaFlagRef, kCFNumberSInt32Type, &deltaFlag);
        }
    }

    if (timestamp < decode->segment_start)
    {
        return;
    }

    GstBuffer* buf = NULL;

    if (isGap)
    {
        // Push a flagged, empty buffer it there is a problem.

        buf = gst_buffer_new();
        GST_BUFFER_TIMESTAMP(buf) = timestamp;
        GST_BUFFER_FLAG_SET(buf, GST_BUFFER_FLAG_GAP);
    }
    else
    {
        // Push a valid buffer.

        CVBufferRetain(imageBuffer); // return value equals parameter

        GstPad* srcpad = decode->srcpad;

        size_t width = CVPixelBufferGetWidth(imageBuffer);
        size_t height = CVPixelBufferGetHeight(imageBuffer);
        size_t bytes_per_row = CVPixelBufferGetBytesPerRow(imageBuffer);
        if(!decode->is_stride_set)
        {
            GstCaps *pad_caps = gst_pad_get_current_caps(srcpad);
            if (pad_caps != NULL)
            {
                GstCaps *caps = gst_caps_copy(pad_caps);
                if (caps != NULL)
                {
                    gst_caps_set_simple(caps, "line_stride", G_TYPE_INT, (int)bytes_per_row, NULL);
                    GstEvent *caps_event = gst_event_new_caps(caps);
                    if (caps_event != NULL)
                        gst_pad_push_event(decode->srcpad, caps_event);
                    decode->is_stride_set = TRUE;
                    gst_caps_unref(caps);
                }
                gst_caps_unref(pad_caps);
            }
        }
        if (kCVReturnSuccess == CVPixelBufferLockBaseAddress (imageBuffer, 0))
        {
            void* image_data = CVPixelBufferGetBaseAddress(imageBuffer);
            buf = gst_buffer_new_allocate(NULL, bytes_per_row*height, NULL);
            if (buf != NULL)
            {
                GstMapInfo info;
                if (gst_buffer_map(buf, &info, GST_MAP_WRITE))
                {
                    memcpy (info.data, image_data, info.size);
                    gst_buffer_unmap(buf, &info);
                    GST_BUFFER_TIMESTAMP(buf) = timestamp;
                }
            }

            CVPixelBufferUnlockBaseAddress (imageBuffer, 0); // ignore return value
        }

        CVBufferRelease(imageBuffer);

        if (!buf)
        {
            buf = gst_buffer_new();
            GST_BUFFER_TIMESTAMP(buf) = timestamp;
            GST_BUFFER_FLAG_SET(buf, GST_BUFFER_FLAG_GAP);
        }
    }

    // the callback might be called from several threads
    // need to synchronize ordered_frames queue access
    g_mutex_lock(&decode->mutex);

    g_queue_insert_sorted(decode->ordered_frames, buf, avcdecoder_buffer_compare, NULL);

    GstBuffer* frame;
    GstFlowReturn ret = GST_FLOW_OK;
    while(ret == GST_FLOW_OK && !decode->is_flushing && NULL != (frame = g_queue_peek_head(decode->ordered_frames)))
    {
        GstClockTime ts = GST_BUFFER_TIMESTAMP(frame);
        if(GST_CLOCK_TIME_NONE == decode->previous_timestamp ||         // first frame
           ts <= decode->previous_timestamp + decode->timestamp_ceil || // frame is at next timestamp
           (0 == deltaFlag && ts < timestamp))                          // have newer I-frame
        {
            decode->previous_timestamp = ts;
            g_queue_pop_head(decode->ordered_frames);

            if(GST_BUFFER_FLAG_IS_SET(frame, GST_BUFFER_FLAG_GAP))
            {
                // INLINE - gst_buffer_unref()
                gst_buffer_unref (frame);
            }
            else
            {
                if(decode->is_newsegment)
                {
                    GST_BUFFER_FLAG_SET(frame, GST_BUFFER_FLAG_DISCONT);
                    decode->is_newsegment = FALSE;
                }

                // it's better not to call gst_pad_push under mutex to avoid deadlocks
                g_mutex_unlock(&decode->mutex);
                ret = gst_pad_push(decode->srcpad, frame);
                g_mutex_lock(&decode->mutex);
            }
        }
        else
        {
            break;
        }
    }

    g_mutex_unlock(&decode->mutex);
}

/*
 * GFunc used to unref GstBuffers in a queue.
 */
static void
avcdecoder_element_destroy(gpointer data, gpointer user_data)
{
    if (NULL != data)
    {
        GstBuffer* buf = (GstBuffer*)data;

        // INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
    }
}

/**
 * Initialize the AvcDecoder structure. This should happen
 * only once, before decoding begins.
 */
static void
avcdecoder_state_init(AvcDecoder *decode)
{
    decode->outputCallback = (VDADecoderOutputCallback*)avcdecoder_decoder_output_callback;
    decode->decoder = NULL;
    decode->is_initialized = FALSE;
    decode->is_newsegment = FALSE;
    decode->is_stride_set = FALSE;
    decode->frame_duration = GST_CLOCK_TIME_NONE;
    decode->ordered_frames = g_queue_new();
    decode->segment_start = 0;
}

/**
 * Reset the state of the AvcDecoder structure.
 */
static void
avcdecoder_state_reset(AvcDecoder *decode)
{
    // Flush the decoder.
    if (NULL != decode->decoder)
    {
        OSStatus result = VDADecoderFlush (decode->decoder, 0);
#if ENABLE_WARNINGS
        if (kVDADecoderNoErr != result)
        {
            g_warning ("Could not flush decoder: result code %d\n", (int)result);
        }
#endif
    }

    g_mutex_lock(&decode->mutex);

    // Unref all sorted buffers and clear the associated queue.
    if (NULL != decode->ordered_frames)
    {
        g_queue_foreach(decode->ordered_frames, avcdecoder_element_destroy, NULL);
        g_queue_clear(decode->ordered_frames);
    }

    decode->is_newsegment = FALSE;
    decode->segment_start = 0;

    g_mutex_unlock(&decode->mutex);
}

/**
 * Reset and then destroy the state of the AvcDecoder structure.
 */
static void
avcdecoder_state_destroy(AvcDecoder *decode)
{
    // Reset the state.
    avcdecoder_state_reset(decode);

    // Release the VDADecoder.
    if (NULL != decode->decoder)
    {
        OSStatus result = VDADecoderDestroy (decode->decoder);
#if ENABLE_WARNINGS
        if (kVDADecoderNoErr != result)
        {
            g_warning ("Could not destroy decoder: result code %d\n", (int)result);
        }
#endif
        decode->decoder = NULL;
    }

    // Free the sorted queue.
    if (NULL != decode->ordered_frames)
    {
        g_queue_free(decode->ordered_frames);
        decode->ordered_frames = NULL;
    }
}

/*
 * Perform processing needed for state transitions.
 */
static GstStateChangeReturn
avcdecoder_change_state (GstElement* element, GstStateChange transition)
{
    AvcDecoder *decode = AVCDECODER(element);

    switch(transition)
    {
        case GST_STATE_CHANGE_NULL_TO_READY:
            // Initialize the AvcDecoder structure.
            avcdecoder_state_init (decode);
            break;
        default:
            break;
    }

    // Change state.
    return GST_ELEMENT_CLASS(parent_class)->change_state(element, transition);
}

static GstFlowReturn
avcdecoder_init_decoder (AvcDecoder *decode, GstCaps* videoSpecificCaps)
{
    GstFlowReturn ret = GST_FLOW_OK;
    OSStatus status = kVDADecoderNoErr;
    GstStructure *s = NULL;
    const gchar *mimetype = NULL;

    // Post error to halt playback if we asked to decode HEVC, so we can
    // fallback to OSXPlatform. With soft error we will play audio only for MP4
    // with HEVC and AAC.
    s = gst_caps_get_structure(videoSpecificCaps, 0);
    if (s != NULL)
    {
        mimetype = gst_structure_get_name(s);
        if (mimetype != NULL)
        {
            if (strstr(mimetype, "video/x-h265") != NULL)
            {
                gst_element_message_full(GST_ELEMENT(decode), GST_MESSAGE_ERROR,
                        GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                        g_strdup("GSTPlatform does not support HEVC on macOS, use OSXPlatform instead."),
                        NULL, ("avcdecoder.c"), ("avcdecoder_init_decoder"), 0);

                return GST_FLOW_ERROR;
            }
        }
    }

    // Initialize the element structure.
    if (FALSE == decode->is_initialized)
    {
        // Obtain configuration data from the "codec_data" structure in the sink caps.
        if (NULL == videoSpecificCaps || gst_caps_get_size(videoSpecificCaps) < 1)
        {
            return GST_FLOW_ERROR;
        }

        GstStructure* videoSpecificStructure = gst_caps_get_structure (videoSpecificCaps, 0);

        const GValue *videoSpecificValue = gst_structure_get_value(videoSpecificStructure, "codec_data");
        if (NULL == videoSpecificValue)
        {
            return GST_FLOW_ERROR;
        }

        gint encoded_width;
        if (!gst_structure_get_int (videoSpecificStructure, "width", &encoded_width))
            encoded_width = 0;

        gint encoded_height;
        if (!gst_structure_get_int (videoSpecificStructure, "height", &encoded_height))
            encoded_height = 0;

        gint framerate_num;
        gint framerate_den;
        if (!gst_structure_get_fraction (videoSpecificStructure, "framerate", &framerate_num, &framerate_den))
        {
            framerate_num = 25;
            framerate_den = 1;
        }

        // Calculate frame duration and timestamp bound.
        decode->frame_duration = gst_util_uint64_scale_int_ceil(GST_SECOND, framerate_den, framerate_num);
        decode->timestamp_ceil = (GstClockTime)(1.5*decode->frame_duration + 0.5);

        SInt32 avcWidth = (SInt32)encoded_width;
        SInt32 avcHeight = (SInt32)encoded_height;

        // Set up parameters required to create the VDADecoder.
        CFNumberRef width = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, &avcWidth);
        CFNumberRef height = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, &avcHeight);
        SInt32 sourceFormat = 'avc1';
        CFNumberRef avcFormat = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, &sourceFormat);

        GstBuffer*  videoSpecificBuffer = gst_value_get_buffer (videoSpecificValue);
        GstMapInfo info;
        CFDataRef avcCData = NULL;
        if (gst_buffer_map(videoSpecificBuffer, &info, GST_MAP_READ))
        {
            guint8* videoSpecificData = info.data;
            guint videoSpecificDataLength = info.size;
            avcCData = CFDataCreate(kCFAllocatorDefault, videoSpecificData, videoSpecificDataLength);
            gst_buffer_unmap(videoSpecificBuffer, &info);
        }

        CFMutableDictionaryRef decoderConfiguration = (CFDictionaryCreateMutable(kCFAllocatorDefault, 4, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks));

        CFDictionarySetValue(decoderConfiguration, kVDADecoderConfiguration_Height, height);
        CFDictionarySetValue(decoderConfiguration, kVDADecoderConfiguration_Width, width);
        CFDictionarySetValue(decoderConfiguration, kVDADecoderConfiguration_SourceFormat, avcFormat);
        if (avcCData != NULL)
            CFDictionarySetValue(decoderConfiguration, kVDADecoderConfiguration_avcCData, avcCData);

        // Note: For 'yuvs' the formatType should be kYUVSPixelFormat.
        SInt32 formatType = k2vuyPixelFormat;
        CFNumberRef imgFormat = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, &formatType);
        CFMutableDictionaryRef destinationImageBufferAttributes = CFDictionaryCreateMutable(kCFAllocatorDefault, 2, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);

        // empty IOSurface properties dictionary
        CFDictionaryRef emptyDictionary = CFDictionaryCreate(kCFAllocatorDefault,
                                                             NULL,
                                                             NULL,
                                                             0,
                                                             &kCFTypeDictionaryKeyCallBacks,
                                                             &kCFTypeDictionaryValueCallBacks);

        CFDictionarySetValue(destinationImageBufferAttributes,
                             kCVPixelBufferPixelFormatTypeKey, imgFormat);
        CFDictionarySetValue(destinationImageBufferAttributes,
                             kCVPixelBufferIOSurfacePropertiesKey,
                             emptyDictionary); // XXX probably should delete this.

        // Create the VDADecoder.
        status = VDADecoderCreate(decoderConfiguration,
                                  destinationImageBufferAttributes,
                                  (VDADecoderOutputCallback *)decode->outputCallback,
                                  (void *)decode,
                                  &decode->decoder);

        if (decoderConfiguration)
            CFRelease(decoderConfiguration);
        if (destinationImageBufferAttributes)
            CFRelease(destinationImageBufferAttributes);
        if (emptyDictionary)
            CFRelease(emptyDictionary);
        if (avcCData)
            CFRelease(avcCData);

        if (kVDADecoderNoErr == status)
        {
            // Set the srcpad caps.

            // Note: For 'yuvs' the format should be GST_MAKE_FOURCC ('Y', 'U', 'Y', '2')
            GstCaps* caps = gst_caps_new_simple (
                                                 "video/x-raw-ycbcr422",
                                                 "format", G_TYPE_STRING, "UYVY",
                                                 "framerate", GST_TYPE_FRACTION, framerate_num, framerate_den,
                                                 "width", G_TYPE_INT, encoded_width,
                                                 "height", G_TYPE_INT, encoded_height,
                                                 NULL);
            GstEvent *caps_event = gst_event_new_caps(caps);
            if (caps_event)
                gst_pad_push_event (decode->srcpad, caps_event);
            gst_caps_unref (caps);

            decode->is_initialized = TRUE;
        }
        else
        {
#if ENABLE_WARNINGS
            const char* message;
            switch (status)
            {
                case kVDADecoderHardwareNotSupportedErr:
                    message = "hardware does not support accelerated video decode services";
                    break;
                case kVDADecoderFormatNotSupportedErr:
                    message = "hardware decoder does not support requested output format";
                    break;
                case kVDADecoderConfigurationError:
                    message = "unsupported hardware decoder configuration parameters";
                    break;
                case kVDADecoderDecoderFailedErr:
                    message = "hardware decoder resources in use by another process or cannot decode the source into the requested format";
                    break;
                default:
                    message = "unknown error";
                    break;
            }
            g_warning ("Could not create decoder: result code %d, %s", (int)status, message);
#endif

            // Post an error message to the pipeline bus.
            GError* error = g_error_new (g_quark_from_string("AVCDecoder"), 666, "%s", message);
            GstMessage* msg = gst_message_new_error (GST_OBJECT (decode), error, message);
            gst_element_post_message(GST_ELEMENT(decode), msg);

            ret = GST_FLOW_ERROR;
        }
    }

    return ret;
}

/*
 * FLUSH_START, NEWSEGMENT, and FLUSH_STOP are recognized and forwarded;
 * all others are simply forwarded.
 */
static gboolean
avcdecoder_sink_event (GstPad * pad, GstObject *parent, GstEvent * event)
{
    gboolean ret = FALSE;
    AvcDecoder *decode = AVCDECODER (parent);
    GstSegment segment;

    switch (GST_EVENT_TYPE (event))
    {
        case GST_EVENT_FLUSH_START:
        {
            // Start flushing buffers.

            // Set flag so chain function refuses buffers.
            decode->is_flushing = TRUE;

            break;
        }

        case GST_EVENT_FLUSH_STOP:
        {
            // Stop flushing buffers.
            avcdecoder_state_reset(decode);

            // Unset flag so chain function accepts buffers.
            decode->is_flushing = FALSE;

            break;
        }

        case GST_EVENT_SEGMENT:
        {
            // Set a flag indicating a new segment has begun.
            decode->is_newsegment = TRUE;
            decode->previous_timestamp = GST_CLOCK_TIME_NONE;
            gst_event_copy_segment(event, &segment);
            if(GST_FORMAT_TIME == segment.format)
            {
                decode->segment_start = segment.start;
            }
            break;
        }

        case GST_EVENT_CAPS:
        {
            GstCaps *caps;

            gst_event_parse_caps (event, &caps);
            avcdecoder_init_decoder(decode, caps);

            // INLINE - gst_event_unref()
            gst_event_unref (event);
            ret = TRUE;
            break;
        }

        default:
            break;
    }

    // Push the event downstream.
    if (!ret)
        ret = gst_pad_push_event (decode->srcpad, event);

    return ret;
}

/*
 * Processes a buffer of AVC-encoded video data pushed to the sink pad.
 */
static GstFlowReturn
avcdecoder_chain (GstPad * pad, GstObject *parent, GstBuffer * buf)
{
    GstFlowReturn ret = GST_FLOW_OK;
    AvcDecoder *decode = AVCDECODER (parent);
    OSStatus status = kVDADecoderNoErr;
//    g_print("chain - time %f discont %d flags %d\n",
//            (float)GST_BUFFER_TIMESTAMP(buf)/(float)GST_SECOND,
//            (int)GST_BUFFER_IS_DISCONT(buf), (int)GST_BUFFER_FLAGS(buf));

    // If between FLUSH_START and FLUSH_STOP, reject new buffers.
    if (decode->is_flushing)
    {
        // Unref the input buffer.
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(buf);

        return GST_FLOW_FLUSHING;
    }

    if (GST_FLOW_OK == ret)
    {
        // Set the timestamp of the encoded frame.
        int64_t timestamp = GST_BUFFER_TIMESTAMP (buf);
        CFStringRef timestamp_key = CFSTR("timestamp");
        CFNumberRef timestamp_value = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt64Type, &timestamp);
        int32_t deltaFlag = (int32_t)(GST_BUFFER_FLAG_IS_SET(buf, GST_BUFFER_FLAG_DELTA_UNIT) ?
                                      GST_BUFFER_FLAG_DELTA_UNIT : 0);
        CFStringRef delta_key = CFSTR("deltaFlag");
        CFNumberRef delta_value = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, &deltaFlag);
        CFStringRef keys[2];
        CFNumberRef values[2];
        keys[0] = timestamp_key;
        keys[1] = delta_key;
        values[0] = timestamp_value;
        values[1] = delta_value;
        CFDictionaryRef frame_info = CFDictionaryCreate(kCFAllocatorDefault,
                                                        (const void **)&keys,
                                                        (const void **)&values,
                                                        2,
                                                        &kCFTypeDictionaryKeyCallBacks,
                                                        &kCFTypeDictionaryValueCallBacks);

        GstMapInfo info;
        CFTypeRef buffer = NULL;
        if (gst_buffer_map(buf, &info, GST_MAP_READ))
        {
            buffer = CFDataCreate(kCFAllocatorDefault, info.data, info.size);
            gst_buffer_unmap(buf, &info);

            // Send the encoded frame to the VDADecoder.
            status = VDADecoderDecode (decode->decoder, 0, buffer, frame_info);
            CFRelease(buffer);
            CFRelease(frame_info);

            if (kVDADecoderNoErr != status)
            {
#if ENABLE_WARNINGS
                g_warning ("Could not decode data: result code %d\n", (int)status);
#endif

                // Set an error return code only if this was not a "simple" decoding error.
                if (kVDADecoderDecoderFailedErr != status)
                {
                    ret = GST_FLOW_ERROR;
                }
            }
        }
    }

    // INLINE - gst_buffer_unref()
    gst_buffer_unref (buf);

    return ret;
}

// --------------------------------------------------------------------------
gboolean avcdecoder_plugin_init (GstPlugin * avcdecoder)
{
    /* debug category for fltering log messages
     *
     * exchange the string 'Template avcdecoder' with your description
     */
    GST_DEBUG_CATEGORY_INIT (avcdecoder_debug, "avcdecoder",
                             0, "Template avcdecoder"); // FIXME

    return gst_element_register (avcdecoder, "avcdecoder", 512, TYPE_AVCDECODER);
}
