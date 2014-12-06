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
#include <config.h>
#endif

#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "audioconverter.h"

GST_DEBUG_CATEGORY_STATIC (audioconverter_debug);
#define GST_CAT_DEFAULT audioconverter_debug

/*
 * The input capabilities.
 */
#define AUDIOCONVERTER_SINK_CAPS \
"audio/mpeg, " \
"mpegversion = (int) 1, " \
"layer = (int) [ 1, 3 ], " \
"rate = (int) { 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 }, " \
"channels = (int) [ 1, 2 ]; " \
"audio/mpeg, " \
"mpegversion = (int) {2, 4}"

static GstStaticPadTemplate sink_factory =
GST_STATIC_PAD_TEMPLATE ("sink",
                         GST_PAD_SINK,
                         GST_PAD_ALWAYS,
                         GST_STATIC_CAPS (AUDIOCONVERTER_SINK_CAPS));

/*
 * The output capabilities.
 */
#define AUDIOCONVERTER_SRC_CAPS \
"audio/x-raw-float, " \
"endianness = (int) " G_STRINGIFY (G_LITTLE_ENDIAN) ", " \
"signed = (boolean) true, " \
"width = (int) 32, " \
"depth = (int) 32, " \
"rate = (int) { 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 }, " \
"channels = (int) [ 1, 2 ]"

static GstStaticPadTemplate src_factory =
GST_STATIC_PAD_TEMPLATE ("src",
                         GST_PAD_SRC,
                         GST_PAD_ALWAYS,
                         GST_STATIC_CAPS (AUDIOCONVERTER_SRC_CAPS));

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (GstMpaDec, gst_mpadec, GstElement, GST_TYPE_ELEMENT);
 ***********************************************************************************/
static void audioconverter_base_init  (gpointer g_class);
static void audioconverter_class_init (AudioConverterClass *g_class);
static void audioconverter_init (AudioConverter *object, AudioConverterClass *g_class);

static GstElementClass *parent_class = NULL;

static void audioconverter_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass *)  g_type_class_peek_parent (g_class);
    audioconverter_class_init ((AudioConverterClass *)g_class);
}

GType audioconverter_get_type (void)
{
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
                                               g_intern_static_string ("AudioConverter"),
                                               sizeof (AudioConverterClass),
                                               audioconverter_base_init,
                                               NULL,
                                               audioconverter_class_init_trampoline,
                                               NULL,
                                               NULL,
                                               sizeof (AudioConverter),
                                               0,
                                               (GInstanceInitFunc) audioconverter_init,
                                               NULL,
                                               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/*
 * Forward declarations.
 */
static GstStateChangeReturn audioconverter_change_state (GstElement* element,
                                                         GstStateChange transition);
static gboolean audioconverter_sink_event (GstPad * pad, GstEvent * event);
static GstFlowReturn audioconverter_chain (GstPad * pad, GstBuffer * buf);
static gboolean audioconverter_src_event (GstPad * pad, GstEvent * event);
static gboolean audioconverter_src_query (GstPad * pad, GstQuery* query);
static const GstQueryType * audioconverter_get_src_query_types (GstPad * pad);
static void audioconverter_state_init(AudioConverter *decode);

static void initAudioFormatPCM(Float64 sampleRate, AudioStreamBasicDescription* outputFormat);
static void propertyListener(void *clientData,
                             AudioFileStreamID audioFileStream,
                             AudioFileStreamPropertyID propertyID,
                             UInt32 *flags);
static void packetListener(void *clientData,
                           UInt32 numberBytes,
                           UInt32 numberPackets,
                           const void *inputData,
                           AudioStreamPacketDescription  *packetDescriptions);
static OSStatus retrieveInputData(AudioConverterRef audioConverter,
                                  UInt32* numberDataPackets,
                                  AudioBufferList* bufferList,
                                  AudioStreamPacketDescription** dataPacketDescription,
                                  void* userData);

/* --- GObject vmethod implementations --- */

static void
audioconverter_base_init (gpointer gclass)
{
    GstElementClass *element_class;

    element_class = GST_ELEMENT_CLASS (gclass);

    gst_element_class_set_details_simple(element_class,
        "AudioConverter",
        "Codec/Decoder/Audio",
        "Decode raw MPEG audio stream to mono or stereo-interleaved PCM",
        "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&src_factory));
    gst_element_class_add_pad_template (element_class,
                                        gst_static_pad_template_get (&sink_factory));
}

/*
 * Initialize mpadec's class.
 */
static void
audioconverter_class_init (AudioConverterClass * klass)
{
    GstElementClass *gstelement_class = (GstElementClass *) klass;

    gstelement_class->change_state = audioconverter_change_state;
}

/*
 * Initialize the new element.
 * Instantiate pads and add them to element.
 * Set pad calback functions.
 * Initialize instance structure.
 */
static void
audioconverter_init (AudioConverter * decode,
                     AudioConverterClass * gclass)
{
    // Input.
    decode->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
    if (FALSE == gst_element_add_pad (GST_ELEMENT (decode), decode->sinkpad))
        g_warning ("audioconverter element failed to add sink pad!\n");
    gst_pad_set_chain_function (decode->sinkpad, GST_DEBUG_FUNCPTR(audioconverter_chain));
    gst_pad_set_event_function(decode->sinkpad, audioconverter_sink_event);

    // Output.
    decode->srcpad = gst_pad_new_from_static_template (&src_factory, "src");
    if (TRUE != gst_element_add_pad (GST_ELEMENT (decode), decode->srcpad))
        g_warning ("audioconverter element failed to add source pad!\n");
    gst_pad_set_event_function(decode->srcpad, audioconverter_src_event);
    gst_pad_set_query_function(decode->srcpad, audioconverter_src_query);
    gst_pad_set_query_type_function(decode->srcpad, audioconverter_get_src_query_types);
    gst_pad_use_fixed_caps (decode->srcpad);
}

/* --- GstElement vmethod implementations --- */

/**
 * Initialize the AudioConverter structure. This should happen
 * only once, before decoding begins.
 */
static void
audioconverter_state_init(AudioConverter *decode)
{
    decode->packetDesc = NULL;
    decode->inputData = NULL;

    decode->enable_parser = TRUE;

    decode->audioStreamID = NULL;

    decode->cookieSize = 0;
    decode->cookieData = NULL;

    decode->audioConverter = NULL;
    decode->outPacketDescription = NULL;

    decode->isAudioConverterReady = FALSE;
    decode->isFormatInitialized = FALSE;
    decode->hasAudioPacketTableInfo = FALSE;

    decode->audioDataPacketCount = 0;
    decode->previousDesc = NULL;

    // Flags
    decode->is_initialized = FALSE;
    decode->has_pad_caps = FALSE;

    // Counters
    decode->total_samples = 0;

    // Values
    decode->data_format = AUDIOCONVERTER_DATA_FORMAT_NONE;
    decode->initial_offset = (guint64)-1;
    decode->stream_length = AUDIOCONVERTER_STREAM_LENGTH_UNKNOWN;
    decode->duration = AUDIOCONVERTER_DURATION_UNKNOWN;
}

/**
 * Reset the state of the AudioConverter structure. This should happen before
 * decoding a new segment.
 */
static void
audioconverter_state_reset(AudioConverter *decode)
{
    // Buffer cache
    if (NULL == decode->packetDesc) {
        decode->packetDesc = g_queue_new();
    } else if(!g_queue_is_empty(decode->packetDesc)) {
        guint queueLength = g_queue_get_length(decode->packetDesc);
        int i;
        for(i = 0; i < queueLength; i++) {
            gpointer p = g_queue_pop_head(decode->packetDesc);
            g_free(p);
        }
    }

    // Input data
    if (NULL == decode->inputData) {
        decode->inputData = g_array_sized_new(FALSE, FALSE, sizeof(guint8),
                                              AUDIOCONVERTER_INITIAL_BUFFER_SIZE);
    } else {
        decode->inputData = g_array_set_size(decode->inputData, 0);
    }
    decode->inputOffset = 0;

    // Decoder
    if (NULL != decode->audioConverter) {
        AudioConverterReset(decode->audioConverter);
    }

    // Flags
    decode->is_synced = FALSE;
    decode->is_discont = TRUE;

    // Counters
    decode->total_packets = 0;

    if(NULL != decode->previousDesc) {
        g_free(decode->previousDesc);
        decode->previousDesc = NULL;
    }
}

/*
 * Perform processing needed for state transitions.
 */
static GstStateChangeReturn
audioconverter_change_state (GstElement* element, GstStateChange transition)
{
    AudioConverter *decode = AUDIOCONVERTER(element);
    GstStateChangeReturn ret;

    switch(transition)
    {
        case GST_STATE_CHANGE_NULL_TO_READY:
            audioconverter_state_init(decode);
            break;
        case GST_STATE_CHANGE_READY_TO_PAUSED:
            // Clear the AudioConverter state.
            audioconverter_state_reset(decode);
            break;
        case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
            break;
        default:
            break;
    }

    // Change state.
    ret = parent_class->change_state(element, transition);
    if(GST_STATE_CHANGE_FAILURE == ret)
    {
        return ret;
    }

    switch(transition)
    {
        case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
            break;
        case GST_STATE_CHANGE_PAUSED_TO_READY:
            // Free all allocated memory.
            if(!g_queue_is_empty(decode->packetDesc)) {
                guint queueLength = g_queue_get_length(decode->packetDesc);
                int i;
                for(i = 0; i < queueLength; i++) {
                    gpointer p = g_queue_pop_head(decode->packetDesc);
                    g_free(p);
                }
            }

            g_queue_free(decode->packetDesc);
            decode->packetDesc = NULL;

            g_array_free(decode->inputData, TRUE);
            decode->inputData = NULL;

            if(NULL != decode->audioStreamID) {
                AudioFileStreamClose(decode->audioStreamID);
                decode->audioStreamID = NULL;
            }

            if(NULL != decode->audioConverter) {
                AudioConverterDispose(decode->audioConverter);
                decode->audioConverter = NULL;
            }

            if(NULL != decode->cookieData) {
                g_free(decode->cookieData);
                decode->cookieData = NULL;
            }

            if(NULL != decode->outPacketDescription) {
                g_free(decode->outPacketDescription);
                decode->outPacketDescription = NULL;
            }

            if(NULL != decode->previousDesc) {
                g_free(decode->previousDesc);
                decode->previousDesc = NULL;
            }
            break;
        case GST_STATE_CHANGE_READY_TO_NULL:
            break;
        default:
            break;
    }

    return ret;
}

/*
 * Process events received from upstream. The explicitly handled events are
 * FLUSH_START, FLUSH_STOP, and NEWSEGMENT; all others are forwarded.
 */
static gboolean
audioconverter_sink_event (GstPad * pad, GstEvent * event)
{
    gboolean ret;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    AudioConverter *decode = AUDIOCONVERTER(parent);

#if ENABLE_PRINT_SPEW
    g_print("sink event: %s\n", GST_EVENT_TYPE_NAME(event));
#endif
    switch (GST_EVENT_TYPE (event))
    {
        case GST_EVENT_FLUSH_START:
        {
            // Start flushing buffers.

            // Set flag so chain function refuses new buffers.
            decode->is_flushing = TRUE;

            // Push the event downstream.
            ret = gst_pad_push_event (decode->srcpad, event);
            break;
        }

        case GST_EVENT_FLUSH_STOP:
        {
            // Stop flushing buffers.
            audioconverter_state_reset(decode);

            // Unset flag so chain function accepts buffers.
            decode->is_flushing = FALSE;

            // Push the event downstream.
            ret = gst_pad_push_event (decode->srcpad, event);
            break;
        }
            
        case GST_EVENT_EOS:
        {
            if (decode->is_priming)
            {
                gst_element_message_full(GST_ELEMENT(decode), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("MP3 file must contain 3 MP3 frames."), NULL, ("audioconverter.c"), ("audioconverter_sink_event"), 0);
            }
            
            // Push the event downstream.
            ret = gst_pad_push_event (decode->srcpad, event);
            break;
        }

        default:
            // Push the event downstream.
            ret = gst_pad_push_event (decode->srcpad, event);
            break;
    }

    // Unlock the parent object.
    gst_object_unref(parent);

    return ret;
}

/*
 * Process events received from downstream. The only handled event is SEEK and
 * that only to convert the event from TIME to BYTE format.
 */
static gboolean
audioconverter_src_event (GstPad * pad, GstEvent * event)
{
    gboolean result = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    AudioConverter *decode = AUDIOCONVERTER(parent);

    if (GST_EVENT_TYPE(event) == GST_EVENT_SEEK)
    {
        gdouble rate;           // segment rate
        GstFormat format;       // format of the seek values
        GstSeekFlags flags;     // the seek flags
        GstSeekType start_type; // the seek type of the start position
        GstSeekType stop_type;  // the seek type of the stop position
        gint64 start;           // the seek start position in the given format
        gint64 stop;            // the seek stop position in the given format

        // Get seek description from the event.
        gst_event_parse_seek (event, &rate, &format, &flags, &start_type, &start, &stop_type, &stop);
        if (format == GST_FORMAT_TIME)
        {
            gint64 start_byte = 0;
            GstFormat format = GST_FORMAT_BYTES;
            if (gst_pad_query_peer_convert(decode->sinkpad, GST_FORMAT_TIME, start, &format, &start_byte))
            {
                result = gst_pad_push_event(decode->sinkpad,
                                            gst_event_new_seek(rate, GST_FORMAT_BYTES,
                                                               (GstSeekFlags)(GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE),
                                                               GST_SEEK_TYPE_SET, start_byte,
                                                               GST_SEEK_TYPE_NONE, 0));
                if (result)
                {
                    // INLINE - gst_event_unref()
                    gst_event_unref (event);
                }
            }
            if (!result) {
                SInt64 absolutePacketOffset = start / decode->frame_duration;
                SInt64 absoluteByteOffset;
                UInt32 flags = 0;
                if(noErr == AudioFileStreamSeek(decode->audioStreamID, absolutePacketOffset,
                                                &absoluteByteOffset, &flags)) {
                    start_byte = (gint64)absoluteByteOffset;
                    result = gst_pad_push_event(decode->sinkpad,
                                                gst_event_new_seek(rate, GST_FORMAT_BYTES,
                                                                   (GstSeekFlags)(GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE),
                                                                   GST_SEEK_TYPE_SET, start_byte,
                                                                   GST_SEEK_TYPE_NONE, 0));
                    if (result)
                    {
                        // INLINE - gst_event_unref()
                        gst_event_unref (event);
                    }
                }
            }
        }
    }

    // Push the event upstream only if it was not processed.
    if (!result)
        result = gst_pad_push_event(decode->sinkpad, event);

    // Unlock the parent object.
    gst_object_unref(parent);

    return result;
}

static const GstQueryType *
audioconverter_get_src_query_types (GstPad * pad)
{
    static const GstQueryType audioconverter_src_query_types[] = {
        GST_QUERY_POSITION,
        GST_QUERY_DURATION,
        0
    };

    return audioconverter_src_query_types;
}

static gboolean
audioconverter_src_query (GstPad * pad, GstQuery * query)
{
    // Set flag indicating that the query has not been handled.
    gboolean result = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    AudioConverter *decode = AUDIOCONVERTER(parent);
    GstFormat format;
    gint64 value;

    switch (GST_QUERY_TYPE (query))
    {
        case GST_QUERY_DURATION:
        {
#if ENABLE_PRINT_SPEW
            g_print("Duration query\n");
#endif

            // Do not handle query if the stream offset is unknown.
            if ((guint64)-1 == decode->initial_offset) {
                // Unref the parent object.
                gst_object_unref(parent);
                return FALSE;
            }

            // Get the format required by the query.
            gst_query_parse_duration(query, &format, NULL);

            // Handled time-valued query.
            if (format == GST_FORMAT_TIME) {
                if(AUDIOCONVERTER_DURATION_UNKNOWN != decode->duration) {
#if ENABLE_PRINT_SPEW
                    g_print("STORED DURATION\n");
#endif
                    gst_query_set_duration(query, GST_FORMAT_TIME, decode->duration);
                    result = TRUE;
                } else if (gst_pad_query_peer_duration(decode->sinkpad, &format, &value) &&
                           format == GST_FORMAT_TIME) {
                    // Get the duration from the sinkpad.
                    gst_query_set_duration(query, GST_FORMAT_TIME, value);
                    decode->duration = value;
                    result = TRUE;
#if ENABLE_PRINT_SPEW
                    g_print("SINK PAD TIME DURATION\n");
#endif
                } else {
                    GstFormat fmt = GST_FORMAT_BYTES;
                    gint64 data_length;
                    if (gst_pad_query_peer_duration(decode->sinkpad, &fmt, &data_length)) {
                        data_length -= decode->initial_offset;

                        fmt = GST_FORMAT_TIME;
                        if (gst_pad_query_peer_convert(decode->sinkpad, GST_FORMAT_BYTES, data_length, &fmt, &value)) {
#if ENABLE_PRINT_SPEW
                            g_print("SINK PAD BYTE DURATION\n");
#endif
                            gst_query_set_duration(query, GST_FORMAT_TIME, value);
                            decode->duration = value;
                            result = TRUE;
                        }
                    }
                }
            }
            break;
        }

        case GST_QUERY_POSITION:
        {
            // Get the format required by the query.
            gst_query_parse_position(query, &format, NULL);

            // Handle time-valued query if the decoder is initialized.
            if(format == GST_FORMAT_TIME && decode->is_initialized)
            {
                // Use the sampling rate to convert sample offset to time.
                value = gst_util_uint64_scale_int(decode->total_samples,
                                                  GST_SECOND,
                                                  decode->sampling_rate);

                // Set the position on the query object.
                gst_query_set_position(query, format, value);

                // Set flag indicating that the query has been handled.
                result = TRUE;
            }
        }

        default:
            break;
    }

    // Use default query if flag indicates query not handled.
    if(result == FALSE)
    {
        result = gst_pad_query_default(pad, query);
    }

    // Unref the parent object.
    gst_object_unref(parent);

    return result;
}

/*
 * Processes a buffer of MPEG audio data pushed to the sink pad.
 */
static GstFlowReturn
audioconverter_chain (GstPad * pad, GstBuffer * buf)
{
    AudioConverter *decode = AUDIOCONVERTER(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret      = GST_FLOW_OK;
    guint8 *buf_data       = GST_BUFFER_DATA(buf);
    guint buf_size         = GST_BUFFER_SIZE(buf);
    GstClockTime buf_time  = GST_BUFFER_TIMESTAMP(buf);

    // If between FLUSH_START and FLUSH_STOP, reject new buffers.
    if (decode->is_flushing)
    {
        ret = GST_FLOW_WRONG_STATE;
        goto _exit;
    }

    // Reset state on discont buffer if not after FLUSH_STOP.
    if (GST_BUFFER_IS_DISCONT(buf) && TRUE == decode->is_synced) {
        audioconverter_state_reset(decode);
    }

    if (decode->enable_parser && NULL == decode->audioStreamID) {
        AudioFileTypeID audioStreamTypeHint = kAudioFileM4AType;

        // Try to set a better parser hint from the sink pad caps.
        GstCaps* sink_peer_caps = gst_pad_peer_get_caps(decode->sinkpad);
        if(NULL != sink_peer_caps) {
            if(gst_caps_get_size(sink_peer_caps) > 0) {
                GstStructure* caps_struct = gst_caps_get_structure(sink_peer_caps, 0);
                if(NULL != caps_struct) {
                    const gchar* struct_name = gst_structure_get_name(caps_struct);
                    if(NULL != struct_name) {
                        if(0 == strcmp(struct_name, "audio/mpeg")) {
                            gint mpegversion;
                            if(!gst_structure_get_int(caps_struct, "mpegversion", &mpegversion)) {
                                mpegversion = 1;
                            }

                            if(4 == mpegversion &&
                               NULL != gst_structure_get_value (caps_struct, "codec_data")) {
                                decode->enable_parser = FALSE;
                                decode->data_format = AUDIOCONVERTER_DATA_FORMAT_AAC;

                                const GValue* codec_data_value = gst_structure_get_value (caps_struct, "codec_data");
                                GstBuffer* codec_data_buf = gst_value_get_buffer (codec_data_value);
                                guint8* codec_data = GST_BUFFER_DATA(codec_data_buf);
                                guint codec_data_size = GST_BUFFER_SIZE(codec_data_buf);

                                //
                                // Get the number of channels from the Audio Specific Config
                                // which is what is passed in "codec_data"
                                //
                                // Ref: http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
                                //
                                guint8 channel_config = 0;
                                if (codec_data_size >= 2) {
                                    guint8 freq_index = (codec_data[0]&0x07) << 1 | (codec_data[1]&0x80) >> 7;
                                    if (15 == freq_index) {
                                        if(codec_data_size >= 5) {
                                            channel_config = (codec_data[4]&0x78) >> 3;
                                        }
                                    } else {
                                        channel_config = (codec_data[1]&0x78) >> 3;
                                    }
                                }

                                const GValue* esds_value = gst_structure_get_value (caps_struct, "esds_data");
                                if(esds_value) {
                                    gint rate;
                                    if(!gst_structure_get_int(caps_struct, "rate", &rate)) {
                                        rate = 44100;
                                    }

                                    gint channels;
                                    if(!gst_structure_get_int(caps_struct, "channels", &channels)) {
                                        channels = 2;
                                    }

                                    GstBuffer* esds_buf = gst_value_get_buffer (esds_value);
                                    guint8* esds_data = GST_BUFFER_DATA(esds_buf);
                                    guint esds_size = GST_BUFFER_SIZE(esds_buf);

                                    decode->sampling_rate = rate;
                                    if (channel_config > 0 && channel_config < 7) {
                                        decode->num_channels = channel_config;
                                    } else if (7 == channel_config) {
                                        decode->num_channels = 8;
                                    } else {
                                        decode->num_channels = channels;
                                    }
                                    decode->samples_per_frame = 1024; // XXX Note: AAC-LC has 960 spf

                                    decode->audioInputFormat.mSampleRate = decode->sampling_rate;
                                    decode->audioInputFormat.mFormatID = kAudioFormatMPEG4AAC;
                                    decode->audioInputFormat.mFramesPerPacket = decode->samples_per_frame;
                                    decode->audioInputFormat.mChannelsPerFrame = decode->num_channels;

                                    initAudioFormatPCM(decode->audioInputFormat.mSampleRate,
                                                       &decode->audioOutputFormat);

                                    decode->cookieSize = esds_size - AUDIOCONVERTER_AAC_ESDS_HEADER_SIZE;
                                    decode->cookieData = g_malloc0(decode->cookieSize);
                                    if(NULL != decode->cookieData) {
                                        memcpy(decode->cookieData,
                                               esds_data + AUDIOCONVERTER_AAC_ESDS_HEADER_SIZE,
                                               decode->cookieSize);
                                    }

                                    decode->isFormatInitialized = TRUE;
                                    decode->isAudioConverterReady = TRUE;
                                } else {
                                    gst_caps_unref(sink_peer_caps);

                                    ret = GST_FLOW_ERROR;
                                    goto _exit;
                                }
                            } else {
                                gint layer;
                                if(gst_structure_get_int(caps_struct, "layer", &layer)) {
                                    switch(layer) {
                                        case 1:
                                            audioStreamTypeHint = kAudioFileMP1Type;
                                            break;
                                        case 2:
                                            audioStreamTypeHint = kAudioFileMP2Type;
                                            break;
                                        case 3:
                                        default:
                                            audioStreamTypeHint = kAudioFileMP3Type;
                                            break;
                                    }
                                } else {
                                    audioStreamTypeHint = kAudioFileM4AType;
                                }
                            }
                        }
                    }
                }
            }
            gst_caps_unref(sink_peer_caps);
        }

        if(decode->enable_parser) {
            if(noErr != AudioFileStreamOpen((void*)decode,
                                            propertyListener,
                                            packetListener,
                                            audioStreamTypeHint,
                                            &decode->audioStreamID)) {
#if ENABLE_PRINT_SPEW
                g_print("AudioFileStreamOpen failed\n");
#endif
                ret = GST_FLOW_ERROR;
                goto _exit;
            }
        }
    }

    if(decode->enable_parser) {
        guint32 parserFlags;
        if(!decode->isAudioConverterReady) {
            parserFlags = 0;
        } else {
            //parserFlags = decode->is_synced ? 0 : kAudioFileStreamParseFlag_Discontinuity;
            if(decode->is_synced) {
                parserFlags = 0;
            } else {
                parserFlags = kAudioFileStreamParseFlag_Discontinuity;
                AudioConverterReset(decode->audioConverter);
            }
        }

        OSStatus result = AudioFileStreamParseBytes(decode->audioStreamID, buf_size, buf_data, parserFlags);

        if(noErr != result) {
#if ENABLE_PRINT_SPEW
            g_print("AudioFileStreamParseBytes %d\n", result);
#endif
            ret = GST_FLOW_ERROR;
            goto _exit;
        }
    } else {
        if(!decode->is_synced && NULL != decode->audioConverter) {
            AudioConverterReset(decode->audioConverter);
        }

        AudioStreamPacketDescription packetDescriptions;
        packetDescriptions.mDataByteSize = buf_size;
        packetDescriptions.mStartOffset = 0;
        packetDescriptions.mVariableFramesInPacket = 0;

        packetListener((void*)decode, buf_size, 1, (const void*)buf_data,
                       &packetDescriptions);
    }

    // Return without pushing a buffer if format not derived from stream parser.
    if(!decode->isFormatInitialized) {
        return GST_FLOW_OK;
    }

    // Return without pushing a buffer if format is MPEG audio but no packets are enqueued.
    if(AUDIOCONVERTER_DATA_FORMAT_MPA == decode->data_format && 0 == decode->total_packets) {
        goto _exit; // GST_FLOW_OK
    }

    if(decode->is_synced == FALSE) {
        // Set flags.
        gboolean is_first_frame = !decode->is_initialized;
        decode->is_initialized = TRUE;
        decode->is_synced = TRUE;
        decode->is_priming = TRUE;

        // Save frame description.
        decode->sampling_rate = (guint)decode->audioInputFormat.mSampleRate;
        decode->samples_per_frame = decode->audioInputFormat.mFramesPerPacket;
        decode->frame_duration = (guint)(GST_SECOND*
                                         (double)decode->samples_per_frame/
                                         (double)decode->sampling_rate);

        if(is_first_frame) {
            // Allocate memory for output packet descriptions.
            decode->outPacketDescription = g_malloc(decode->samples_per_frame*sizeof(AudioStreamPacketDescription));
            if(NULL == decode->outPacketDescription) {
                ret = GST_FLOW_ERROR;
                goto _exit;
            }

            // Save first frame offset.
            decode->initial_offset = GST_BUFFER_OFFSET_IS_VALID(buf) ? GST_BUFFER_OFFSET(buf) : 0;

            // Query for the stream length if it was not set from a header.
            if (AUDIOCONVERTER_STREAM_LENGTH_UNKNOWN == decode->stream_length)
            {
                GstFormat sink_format = GST_FORMAT_BYTES;
                gint64 sink_length;

                if (gst_pad_query_peer_duration(decode->sinkpad, &sink_format, &sink_length))
                {
                    decode->stream_length = sink_length;
                }
            }
        }

        // Derive sample count using the timestamp.
        guint64 frame_index = buf_time/decode->frame_duration;
        decode->total_samples = frame_index * decode->samples_per_frame;


        // Set the sink and source pad caps if not already done.
        if (TRUE != decode->has_pad_caps)
        {
            GstCaps* caps = NULL;

            if(AUDIOCONVERTER_DATA_FORMAT_MPA == decode->data_format) {
                // Determine the layer.
                gint layer;
                switch(decode->audioInputFormat.mFormatID) {
                    case kAudioFormatMPEGLayer1:
                        layer = 1;
                        break;
                    case kAudioFormatMPEGLayer2:
                        layer = 2;
                        break;
                    case kAudioFormatMPEGLayer3:
                        layer = 3;
                        break;
                    default:
                        layer = 3;
                        break;
                }

                // Sink caps: MPEG audio.
                caps = gst_caps_new_simple ("audio/mpeg",
                                            "version", G_TYPE_INT, 1,
                                            "layer", G_TYPE_INT, layer,
                                            "rate", G_TYPE_INT, (gint)decode->sampling_rate,
                                            "channels", G_TYPE_INT, (gint)decode->num_channels,
                                            NULL);
            } else if(AUDIOCONVERTER_DATA_FORMAT_AAC == decode->data_format) {
                caps = gst_caps_new_simple ("audio/mpeg",
                                            "mpegversion", G_TYPE_INT, 2,
                                             NULL);
            } else {
                ret = GST_FLOW_ERROR;
                goto _exit;
            }

            if(gst_pad_set_caps (decode->sinkpad, caps) == FALSE)
            {
#if ENABLE_PRINT_SPEW
                g_print("WARNING: COULD NOT SET sinkpad CAPS\n");
#endif
            }
#if ENABLE_PRINT_SPEW
            g_print("sink_caps %s\n", gst_caps_to_string(caps));
#endif

            gst_caps_unref (caps);
            caps = NULL;

            // Source caps: PCM audio.

            // Create the source caps.
            caps = gst_caps_new_simple ("audio/x-raw-float",
                                        "rate", G_TYPE_INT, (gint)decode->sampling_rate,
                                        "channels", G_TYPE_INT,
                                        decode->audioOutputFormat.mChannelsPerFrame, // may not equal num_channels
                                        "endianness", G_TYPE_INT, G_LITTLE_ENDIAN,
                                        "width", G_TYPE_INT, 32,
                                        "depth", G_TYPE_INT, 32,
                                        "signed", G_TYPE_BOOLEAN, TRUE,
                                        NULL);

            // Set the source caps.
            if(gst_pad_set_caps (decode->srcpad, caps) == FALSE)
            {
#if ENABLE_PRINT_SPEW
                g_print("WARNING: COULD NOT SET srcpad CAPS\n");
#endif
            }
#if ENABLE_PRINT_SPEW
            g_print("src_caps %s\n", gst_caps_to_string(caps));
#endif

            gst_caps_unref (caps);
            caps = NULL;

            // Set the source caps flag.
            decode->has_pad_caps = TRUE;
        }
    }

    if(!decode->isAudioConverterReady) {
        // Return without pushing a buffer if converter is not ready.
        goto _exit; // GST_FLOW_OK
    } else if(NULL == decode->audioConverter) {
        // Initialize the converter.
        if(noErr != AudioConverterNew(&decode->audioInputFormat,
                                      &decode->audioOutputFormat,
                                      &decode->audioConverter)) {
#if ENABLE_PRINT_SPEW
            g_print("Failed to initialize AudioConverter\n");
#endif
            // Return an error if converter cannot be initialized.
            ret = GST_FLOW_ERROR;
            goto _exit;
        } else if(NULL != decode->cookieData && noErr != AudioConverterSetProperty(decode->audioConverter,
                                                                            kAudioConverterDecompressionMagicCookie,
                                                                            decode->cookieSize, decode->cookieData)) {
#if ENABLE_PRINT_SPEW
            g_print("Failed to set AudioConverter magic cookie data\n");
#endif
            // Return an error if converter cannot be initialized.
            ret = GST_FLOW_ERROR;
            goto _exit;
        } else if(AUDIOCONVERTER_DATA_FORMAT_AAC == decode->data_format) {
            AudioConverterPrimeInfo primeInfo;
            primeInfo.leadingFrames = 0;
            primeInfo.trailingFrames = 0;
            AudioConverterSetProperty(decode->audioConverter, kAudioConverterPrimeInfo,
                                      sizeof(primeInfo),
                                      &primeInfo);
        }
    }

    // Decoder priming (MPEG audio only).
    if(decode->is_priming &&
       //AUDIOCONVERTER_DATA_FORMAT_MPA == decode->data_format &&
       decode->total_packets >= AUDIOCONVERTER_MPEG_MIN_PACKETS) {
        // Turn off priming if enough packets are enqueued.
        decode->is_priming = FALSE;
    }

    if(decode->is_priming) {
        // Return without pushing a buffer if there are not enough packets enqueued.
        if(g_queue_get_length(decode->packetDesc) < AUDIOCONVERTER_MPEG_MIN_PACKETS) {
            goto _exit; // GST_FLOW_OK;
        } else {
            decode->is_priming = FALSE;
        }
    }

    // Drain the packet queue.
    while(!g_queue_is_empty(decode->packetDesc)) {
        UInt32 outputDataPacketSize = decode->samples_per_frame;

        guint outbuf_size = outputDataPacketSize*decode->audioOutputFormat.mBytesPerPacket;
        GstBuffer *outbuf = NULL;
        ret = gst_pad_alloc_buffer_and_set_caps (decode->srcpad, GST_BUFFER_OFFSET_NONE,
                                                 outbuf_size,
                                                 GST_PAD_CAPS(decode->srcpad), &outbuf);

        // Bail out on error.
        if(ret != GST_FLOW_OK)
        {
            if (ret != GST_FLOW_WRONG_STATE)
            {
                gst_element_message_full(GST_ELEMENT(decode), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_SEEK, g_strdup("Decoded audio buffer allocation failed"), NULL, ("audioconverter.c"), ("audioconverter_chain"), 0);
            }

            goto _exit;
        }

        AudioBufferList outputData;
        outputData.mNumberBuffers = 1;
        outputData.mBuffers[0].mNumberChannels = decode->audioOutputFormat.mChannelsPerFrame;
        outputData.mBuffers[0].mDataByteSize = (UInt32)outputDataPacketSize*decode->audioOutputFormat.mBytesPerFrame;
        outputData.mBuffers[0].mData = GST_BUFFER_DATA(outbuf);
        OSStatus err = AudioConverterFillComplexBuffer(decode->audioConverter,
                                                       retrieveInputData,
                                                       (void*)decode,
                                                       &outputDataPacketSize,
                                                       &outputData,
                                                       decode->outPacketDescription);
        if(noErr != err) {
#if ENABLE_PRINT_SPEW
            g_print("AudioConverterFillComplexBuffer err: %u\n", err);
#endif
            // INLINE - gst_buffer_unref()
            gst_buffer_unref(outbuf);
            ret = GST_FLOW_ERROR;
            goto _exit;
        }

        if(0 == outputDataPacketSize) {
            // INLINE - gst_buffer_unref()
            gst_buffer_unref(outbuf);
            break;
        }

        // Calculate the timestamp from the sample count and rate.
        guint64 timestamp = gst_util_uint64_scale_int(decode->total_samples,
                                                      GST_SECOND,
                                                      decode->sampling_rate);

        // Set output buffer properties.
        GST_BUFFER_TIMESTAMP(outbuf) = timestamp;
        GST_BUFFER_DURATION(outbuf) = decode->frame_duration;
        GST_BUFFER_SIZE(outbuf) = outputDataPacketSize*decode->audioOutputFormat.mBytesPerPacket;
        GST_BUFFER_OFFSET(outbuf) = decode->total_samples;
        GST_BUFFER_OFFSET_END(outbuf) = (decode->total_samples += outputDataPacketSize);
        if(decode->is_discont)
        {
            GST_BUFFER_FLAG_SET (outbuf, GST_BUFFER_FLAG_DISCONT);
            decode->is_discont = FALSE;
        }

        ret = gst_pad_push (decode->srcpad, outbuf);
        if(GST_FLOW_OK != ret) {
            goto _exit;
        }
    }

    // Remove processed bytes from the buffer cache.
    if(decode->inputOffset != 0)
    {
        decode->inputData = g_array_remove_range(decode->inputData, 0,
                                                 decode->inputOffset <= decode->inputData->len ?
                                                 decode->inputOffset : decode->inputData->len);
        decode->inputOffset = 0;
    }

_exit:
    // Unref the input buffer.
    // INLINE - gst_buffer_unref()
    gst_buffer_unref(buf);
    return ret;
}

#if ENABLE_PRINT_SPEW
static void printStreamDesc (AudioStreamBasicDescription* d) {
    g_print ("%lf %d %d %d %d %d %d %d %d\n",
            d->mSampleRate,
            d->mFormatID,
            d->mFormatFlags,
            d->mBytesPerPacket,
            d->mFramesPerPacket,
            d->mBytesPerFrame,
            d->mChannelsPerFrame,
            d->mBitsPerChannel,
            d->mReserved);
}
#endif

// AudioStream and AudioConverter functions
static void initAudioFormatPCM(Float64 sampleRate,
                               AudioStreamBasicDescription* outputFormat) {
    outputFormat->mSampleRate = sampleRate;
    outputFormat->mFormatID = kAudioFormatLinearPCM;
    outputFormat->mFormatFlags = kAudioFormatFlagIsFloat | kAudioFormatFlagIsPacked;
    outputFormat->mBytesPerPacket = 8;
    outputFormat->mFramesPerPacket = 1;
    outputFormat->mBytesPerFrame = 8;
    outputFormat->mChannelsPerFrame = 2;
    outputFormat->mBitsPerChannel = 32;
    outputFormat->mReserved = 0;
}

static void propertyListener(void *clientData,
                             AudioFileStreamID audioFileStream,
                             AudioFileStreamPropertyID propertyID,
                             UInt32 *flags) {
    AudioConverter* decode = (AudioConverter*)clientData;
    UInt32 propertyDataSize;
    UInt32 isReady;
    Boolean isCookieWritable;

    switch(propertyID) {
        case kAudioFileStreamProperty_ReadyToProducePackets:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_ReadyToProducePackets\n");
#endif
            propertyDataSize = sizeof(isReady);
            AudioFileStreamGetProperty(audioFileStream, propertyID,
                                       &propertyDataSize, &isReady);
            if(1 == isReady && TRUE == decode->isFormatInitialized) {
                decode->isAudioConverterReady = TRUE;
                if(decode->hasAudioPacketTableInfo) {
                    UInt64 numFrames = decode->packetTableInfo.mNumberValidFrames;
                    Float64 sampleRate = decode->audioInputFormat.mSampleRate;
                    decode->duration = (gint64)(numFrames/sampleRate*GST_SECOND + 0.5);
#if ENABLE_PRINT_SPEW
                    g_print("duration: %ld\n", decode->duration);
#endif
                }
            }
            break;
        case kAudioFileStreamProperty_FileFormat:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_FileFormat\n");
#endif
            break;
        case kAudioFileStreamProperty_DataFormat:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_DataFormat\n");
#endif
            propertyDataSize = sizeof(decode->audioInputFormat);
            AudioFileStreamGetProperty(audioFileStream, propertyID,
                                       &propertyDataSize, &decode->audioInputFormat);
#if ENABLE_PRINT_SPEW
            printStreamDesc(&decode->audioInputFormat);
#endif
            switch(decode->audioInputFormat.mFormatID) {
                case kAudioFormatMPEGLayer1:
                case kAudioFormatMPEGLayer2:
                case kAudioFormatMPEGLayer3:
                    decode->data_format = AUDIOCONVERTER_DATA_FORMAT_MPA;
                    break;
                case kAudioFormatMPEG4AAC:
                    decode->data_format = AUDIOCONVERTER_DATA_FORMAT_AAC;
                    break;
            }
            decode->sampling_rate = decode->audioInputFormat.mSampleRate;
            decode->samples_per_frame = decode->audioInputFormat.mFramesPerPacket;
            decode->num_channels = decode->audioInputFormat.mChannelsPerFrame;
            initAudioFormatPCM(decode->audioInputFormat.mSampleRate, &decode->audioOutputFormat);
            decode->isFormatInitialized = TRUE;
            break;
        case kAudioFileStreamProperty_FormatList:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_FormatList\n");
#endif
            break;
        case kAudioFileStreamProperty_MagicCookieData:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_MagicCookieData\n");
#endif
            if(AudioFileStreamGetPropertyInfo(audioFileStream, kAudioFileStreamProperty_MagicCookieData,
                                              &decode->cookieSize, &isCookieWritable)) {
                decode->cookieSize = 0;
            }

            if(decode->cookieSize > 0) {
                decode->cookieData = g_malloc0(decode->cookieSize);
                if(NULL != decode->cookieData) {
                    if(AudioFileStreamGetProperty(audioFileStream, kAudioFileStreamProperty_MagicCookieData,
                                                  &decode->cookieSize, decode->cookieData)) {
                        decode->cookieData = NULL;
                    }
                }
            }
            break;
        case kAudioFileStreamProperty_AudioDataByteCount:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_AudioDataByteCount\n");
#endif
            break;
        case kAudioFileStreamProperty_AudioDataPacketCount:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_AudioDataPacketCount\n");
#endif
            propertyDataSize = 8;
            AudioFileStreamGetProperty(audioFileStream, propertyID,
                                       &propertyDataSize, &decode->audioDataPacketCount);
#if ENABLE_PRINT_SPEW
            g_print (">>> audioDataPacketCount: %llu\n", decode->audioDataPacketCount);
#endif
            break;
        case kAudioFileStreamProperty_MaximumPacketSize:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_MaximumPacketSize\n");
#endif
            break;
        case kAudioFileStreamProperty_DataOffset:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_DataOffset\n");
#endif
            break;
        case kAudioFileStreamProperty_ChannelLayout:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_ChannelLayout\n");
#endif
            break;
        case kAudioFileStreamProperty_PacketTableInfo:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_PacketTableInfo\n");
#endif
            propertyDataSize = sizeof(AudioFilePacketTableInfo);
            if(noErr == AudioFileStreamGetProperty(audioFileStream, propertyID,
                                                   &propertyDataSize, &decode->packetTableInfo)) {
                decode->hasAudioPacketTableInfo = TRUE;
            }
#if ENABLE_PRINT_SPEW
            g_print("valid frames %d priming frames %d remainder frames %d\n",
                    (int)decode->packetTableInfo.mNumberValidFrames,
                    decode->packetTableInfo.mPrimingFrames,
                    decode->packetTableInfo.mRemainderFrames);
#endif
            break;
        case kAudioFileStreamProperty_PacketSizeUpperBound:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_PacketSizeUpperBound\n");
#endif
            break;
        case kAudioFileStreamProperty_AverageBytesPerPacket:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_AverageBytesPerPacket\n");
#endif
            break;
        case kAudioFileStreamProperty_BitRate:
#if ENABLE_PRINT_SPEW
            g_print ("kAudioFileStreamProperty_BitRate\n");
#endif
            break;
        default:
#if ENABLE_PRINT_SPEW
            g_print("propertyID: %d\n", propertyID);
#endif
            break;
    }
}

static void packetListener(void *clientData,
                           UInt32 numberBytes,
                           UInt32 numberPackets,
                           const void *inputData,
                           AudioStreamPacketDescription  *packetDescriptions) {
    AudioConverter* decode = (AudioConverter*)clientData;

    int i;
    for(i = 0; i < numberPackets; i++) {
        decode->total_packets++;
        decode->inputData = g_array_append_vals(decode->inputData,
                                                inputData + packetDescriptions[i].mStartOffset,
                                                packetDescriptions[i].mDataByteSize);
        AudioStreamPacketDescription* packetDesc = g_malloc(sizeof(AudioStreamPacketDescription));
        *packetDesc = packetDescriptions[i];
        g_queue_push_tail(decode->packetDesc, packetDesc);
    }
}

OSStatus retrieveInputData(AudioConverterRef                audioConverter,
                           UInt32*                          numberDataPackets,
                           AudioBufferList*                 bufferList,
                           AudioStreamPacketDescription**   dataPacketDescription,
                           void*                            userData) {
    AudioConverter* decode = (AudioConverter*)userData;

    if(!g_queue_is_empty(decode->packetDesc)) {
        guint numPackets;
        if(*numberDataPackets <= g_queue_get_length(decode->packetDesc)) {
            numPackets = *numberDataPackets;
        } else {
            numPackets = g_queue_get_length(decode->packetDesc);
        }

        if (NULL != dataPacketDescription) {
            *dataPacketDescription = g_malloc(numPackets*sizeof(AudioStreamPacketDescription));
            if(NULL == dataPacketDescription) {
                return kAudioConverterErr_UnspecifiedError;
            }
            if(NULL != decode->previousDesc) {
                g_free(decode->previousDesc);
            }
            decode->previousDesc = *dataPacketDescription;
        }

        int i;
        for(i = 0; i < numPackets; i++) {
            bufferList->mBuffers[i].mData = decode->inputData->data + decode->inputOffset;
            AudioStreamPacketDescription* packetDesc = g_queue_pop_head(decode->packetDesc);
            decode->inputOffset += packetDesc->mDataByteSize;
            bufferList->mBuffers[i].mDataByteSize = packetDesc->mDataByteSize;
            bufferList->mBuffers[i].mNumberChannels = decode->audioOutputFormat.mChannelsPerFrame;

            if (NULL != dataPacketDescription) {
                dataPacketDescription[i]->mStartOffset = 0;
                dataPacketDescription[i]->mVariableFramesInPacket = packetDesc->mVariableFramesInPacket;
                dataPacketDescription[i]->mDataByteSize = packetDesc->mDataByteSize;
            }
            g_free(packetDesc);
        }
        *numberDataPackets = numPackets;
    } else {
        *numberDataPackets = 0;
    }

    return 0;
}

// --------------------------------------------------------------------------
gboolean audioconverter_plugin_init (GstPlugin * audioconverter)
{
    /* debug category for fltering log messages
     *
     * exchange the string 'Template audioconverter' with your description
     */
    GST_DEBUG_CATEGORY_INIT (audioconverter_debug, "audioconverter",
                             0, "Template audioconverter");

    gboolean reg_result = gst_element_register (audioconverter, "audioconverter",
                                                512, TYPE_AUDIOCONVERTER);

    return reg_result;
}
