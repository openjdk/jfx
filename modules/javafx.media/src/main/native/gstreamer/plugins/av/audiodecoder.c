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

#include <stdint.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "audiodecoder.h"
#include <libavformat/avformat.h>
#include <libavutil/samplefmt.h>

GST_DEBUG_CATEGORY_STATIC(audiodecoder_debug);
#define GST_CAT_DEFAULT audiodecoder_debug

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
#define AUDIODECODER_SRC_CAPS \
"audio/x-raw, " \
"format = (string) S16LE, " \
"layout = (string) interleaved, " \
"rate = (int) { 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 }, " \
"channels = (int) [ 1, 2 ]"

static GstStaticPadTemplate src_factory =
        GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC,
        GST_PAD_ALWAYS,
        GST_STATIC_CAPS(AUDIODECODER_SRC_CAPS));

// Uncomment to enable debugging printing
//#define DEBUG_OUTPUT
//#define VERBOSE_DEBUG

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE(AudioDecoder, audiodecoder, BaseDecoder, TYPE_BASEDECODER);
 ***********************************************************************************/
#define audiodecoder_parent_class parent_class
static void audiodecoder_init          (AudioDecoder      *self);
static void audiodecoder_class_init    (AudioDecoderClass *klass);
static gpointer audiodecoder_parent_class = NULL;
static void     audiodecoder_class_intern_init (gpointer klass)
{
    audiodecoder_parent_class = g_type_class_peek_parent (klass);
    audiodecoder_class_init ((AudioDecoderClass*) klass);
}

GType audiodecoder_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (TYPE_BASEDECODER,
               g_intern_static_string ("AudioDecoder"),
               sizeof (AudioDecoderClass),
               (GClassInitFunc) audiodecoder_class_intern_init,
               sizeof(AudioDecoder),
               (GInstanceInitFunc) audiodecoder_init,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

/*
 * Forward declarations.
 */
static GstStateChangeReturn audiodecoder_change_state(GstElement* element,
        GstStateChange transition);
static gboolean audiodecoder_sink_event(GstPad * pad, GstObject *parent, GstEvent * event);
static GstFlowReturn audiodecoder_chain(GstPad * pad, GstObject *parent, GstBuffer * buf);
static gboolean audiodecoder_src_query(GstPad * pad, GstObject *parent, GstQuery* query);
static gboolean audiodecoder_init_state(AudioDecoder *decoder);
static gboolean audiodecoder_open_init(AudioDecoder *decoder, GstCaps* caps);
static gboolean audiodecoder_src_event(GstPad* pad, GstObject *parent, GstEvent* event);

#if DECODE_AUDIO4
static gboolean audiodecoder_is_oformat_supported(int format);
#endif

/* --- GObject vmethod implementations --- */

/*
 * Initialize mpadec's class.
 */
static void audiodecoder_class_init(AudioDecoderClass * klass)
{
    GstElementClass *element_class;

    element_class = GST_ELEMENT_CLASS(klass);

    gst_element_class_set_metadata(element_class,
        "AudioDecoder",
        "Codec/Decoder/Audio",
        "Decode raw MPEG audio stream to mono or stereo-interleaved PCM",
        "Oracle Corporation");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));

    element_class->change_state = audiodecoder_change_state;
}
/*
 * Initialize the new element.
 * Instantiate pads and add them to element.
 * Set pad calback functions.
 * Initialize instance structure.
 */
static void audiodecoder_init(AudioDecoder* decoder)
{
    // Input.
    BaseDecoder *base = BASEDECODER(decoder);

    base->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    if (FALSE == gst_element_add_pad(GST_ELEMENT(decoder), base->sinkpad))
        g_warning("audiodecoder element failed to add sink pad!\n");
    gst_pad_set_chain_function(base->sinkpad, GST_DEBUG_FUNCPTR(audiodecoder_chain));
    gst_pad_set_event_function(base->sinkpad, audiodecoder_sink_event);

    // Output.
    base->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    if (TRUE != gst_element_add_pad(GST_ELEMENT(decoder), base->srcpad))
        g_warning("audiodecoder element failed to add source pad!\n"); //
    gst_pad_set_query_function(base->srcpad, audiodecoder_src_query);
    gst_pad_set_event_function(base->srcpad, audiodecoder_src_event);
    gst_pad_use_fixed_caps(base->srcpad);
}

/**
 * Initialize the AudioDecoder structure. This should happen
 * only once, before decoding begins.
 */
static gboolean audiodecoder_init_state(AudioDecoder *decoder)
{
#if NEW_CODEC_ID
    decoder->codec_id = AV_CODEC_ID_NONE;
#else
    decoder->codec_id = CODEC_ID_NONE;
#endif

#if !DECODE_AUDIO4
    decoder->samples = av_mallocz(AVCODEC_MAX_AUDIO_FRAME_SIZE + FF_INPUT_BUFFER_PADDING_SIZE);
    if (!decoder->samples)
        return FALSE;
#endif

    decoder->total_samples = 0;
    decoder->initial_offset = GST_BUFFER_OFFSET_NONE;
    decoder->duration = GST_CLOCK_TIME_NONE;
    decoder->generate_pts = TRUE;

    decoder->num_channels = 0;
    decoder->sample_rate = 0;
    decoder->bit_rate = 0;

    basedecoder_init_state(BASEDECODER(decoder));
    return TRUE;
}

/**
 * Reset the state of the AudioDecoder structure. This should happen before
 * decoding a new segment.
 */
static void audiodecoder_state_reset(AudioDecoder *decoder)
{
    // Decoder
    basedecoder_flush(BASEDECODER(decoder));

    // Flags
    decoder->is_synced = FALSE;
    decoder->is_discont = TRUE;
}

static void audiodecoder_close_decoder(AudioDecoder *decoder)
{
#if !DECODE_AUDIO4
    if (decoder->samples)
    {
        av_free(decoder->samples);
        decoder->samples = NULL;
    }
#endif

    basedecoder_close_decoder(BASEDECODER(decoder));
}

/*
 * Perform processing needed for state transitions.
 */
static GstStateChangeReturn
audiodecoder_change_state(GstElement* element, GstStateChange transition)
{
    AudioDecoder *decoder = AUDIODECODER(element);
    GstStateChangeReturn ret;

    switch (transition)
    {
        case GST_STATE_CHANGE_NULL_TO_READY:
            if (!audiodecoder_init_state(decoder))
                return GST_STATE_CHANGE_FAILURE;
            break;
        case GST_STATE_CHANGE_READY_TO_PAUSED:
            // Clear the AudioDecoder state.
            audiodecoder_state_reset(decoder);
            break;
        case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
            break;
        default:
            break;
    }

    // Change state.
    ret = GST_ELEMENT_CLASS(parent_class)->change_state(element, transition);
    if (GST_STATE_CHANGE_FAILURE == ret)
        return ret;

    switch (transition)
    {
        case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
            break;
        case GST_STATE_CHANGE_PAUSED_TO_READY:
            audiodecoder_close_decoder(decoder);
            break;
        case GST_STATE_CHANGE_READY_TO_NULL:
            break;
        default:
            break;
    }

    return ret;
}

/*
 * Process events received from upstream. The explicitly events FLUSH_START
 * and FLUSH_STOP are recognized and forwarded; all others are simply forwarded.
 */
static gboolean
audiodecoder_sink_event(GstPad * pad, GstObject *parent, GstEvent * event) {
    AudioDecoder *decoder = AUDIODECODER(parent);
    gboolean ret = FALSE;

    switch (GST_EVENT_TYPE(event))
    {
        case GST_EVENT_FLUSH_START:
        {
            // Start flushing buffers.
            // Set flag so chain function refuses new buffers.
            BASEDECODER(decoder)->is_flushing = TRUE;
            break;
        }

        case GST_EVENT_FLUSH_STOP:
        {
            // Stop flushing buffers.
            audiodecoder_state_reset(decoder);

            // Unset flag so chain function accepts buffers.
            BASEDECODER(decoder)->is_flushing = FALSE;
            break;
        }

        case GST_EVENT_CAPS:
        {
            GstCaps *caps;

            gst_event_parse_caps (event, &caps);
            if (!audiodecoder_open_init(decoder, caps))
            {
                gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_FAILED,
                                 g_strdup("Initialization of audio decoder failed"), NULL, ("audiodecoder.c"), ("audiodecoder_sink_event"), 0);
            }

            // INLINE - gst_event_unref()
            gst_event_unref (event);
            ret = TRUE;
            break;
        }

#ifdef DEBUG_OUTPUT
        case GST_EVENT_NEWSEGMENT:
        {
            GstFormat format;
            gboolean update;
            gdouble rate, applied_rate;
            gint64 start, stop, time;

            gst_event_parse_new_segment_full (event, &update, &rate, &applied_rate, &format, &start, &stop, &time);
            g_print("audiodecoder_sink_event: NEW_SEGMENT update=%s, rate=%f, format=%d, start=%ld, stop=%ld, time=%ld\n",
                    update ? "TRUE" : "FALSE", rate, format, start, stop, time);

            break;
        }
#endif // DEBUG_OUTPUT

        default:
            break;
    }

    // Push the event downstream.
    if (!ret)
        ret = gst_pad_push_event(BASEDECODER(decoder)->srcpad, event);

    return ret;
}

static gboolean
audiodecoder_src_query(GstPad * pad, GstObject *parent, GstQuery * query) {

    AudioDecoder *decoder = AUDIODECODER(parent);
    BaseDecoder  *base = BASEDECODER(parent);

    gboolean result = FALSE; // Set flag indicating that the query has not been handled.
    GstFormat format;
    gint64 value;

    switch (GST_QUERY_TYPE(query)) {
        case GST_QUERY_DURATION:
        {
            // Do not handle query if the stream offset is unknown.
            if ((guint64)-1 == decoder->initial_offset)
            {
                return FALSE;
            }

            // Get the format required by the query.
            gst_query_parse_duration(query, &format, NULL);

            // Handled time-valued query.
            if (format == GST_FORMAT_TIME)
            {
                if(GST_CLOCK_TIME_IS_VALID(decoder->duration))
                {
                    gst_query_set_duration(query, GST_FORMAT_TIME, decoder->duration);
                    result = TRUE;
                }
                else if (gst_pad_peer_query_duration(base->sinkpad, GST_FORMAT_TIME, &value) &&
                         format == GST_FORMAT_TIME)
                {
                    // Get the duration from the sinkpad.
                    gst_query_set_duration(query, GST_FORMAT_TIME, value);
                    decoder->duration = value;
                    result = TRUE;
                }
                else
                {
                    gint64 data_length;
                    if (gst_pad_peer_query_duration(base->sinkpad, GST_FORMAT_BYTES, &data_length))
                    {
                        data_length -= decoder->initial_offset;
                        if (gst_pad_peer_query_convert(base->sinkpad, GST_FORMAT_BYTES, data_length, GST_FORMAT_TIME, &value))
                        {
                            gst_query_set_duration(query, GST_FORMAT_TIME, value);
                            decoder->duration = value;
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
            if (format == GST_FORMAT_TIME && base->is_initialized)
            {
                // Use the sampling rate to convert sample offset to time.
                value = gst_util_uint64_scale_int(decoder->total_samples,
                        GST_SECOND,
                        decoder->sample_rate);

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
    if (result == FALSE)
        result = gst_pad_query_default(pad, parent, query);

    return result;
}

static gboolean audiodecoder_src_event(GstPad* pad, GstObject *parent, GstEvent* event) {
    gboolean result = FALSE;
    BaseDecoder *base = BASEDECODER(parent);

    if (GST_EVENT_TYPE(event) == GST_EVENT_SEEK) {
        gdouble rate; // segment rate
        GstFormat format; // format of the seek values
        GstSeekFlags flags; // the seek flags
        GstSeekType start_type; // the seek type of the start position
        GstSeekType stop_type; // the seek type of the stop position
        gint64 start; // the seek start position in the given format
        gint64 stop; // the seek stop position in the given format

        // Looks like mpegaudioparse has a bug for seeking within ~20 seconds of
        // mp3 file. Seeking will not be correct and offset will be ~15 seconds.
        // DShowWrapper does not let mpegaudioparse to do seek, instead it asks
        // it to convert TIME to BYTES and let source element do seek. This approach
        // works fine on Windows and Linux, so we will do same here.

        // Get seek description from the event.
        gst_event_parse_seek(event, &rate, &format, &flags, &start_type, &start, &stop_type, &stop);
        if (format == GST_FORMAT_TIME) {
            gint64 start_byte = 0;
            if (gst_pad_peer_query_convert(base->sinkpad, GST_FORMAT_TIME, start, GST_FORMAT_BYTES, &start_byte)) {
                result = gst_pad_push_event(base->sinkpad,
                        gst_event_new_seek(rate, GST_FORMAT_BYTES,
                        (GstSeekFlags) (GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE),
                        GST_SEEK_TYPE_SET, start_byte,
                        GST_SEEK_TYPE_NONE, 0));
                if (result) {
                    // INLINE - gst_event_unref()
                    gst_event_unref(event);
                }
            }
        }
    }

    // Push the event upstream only if it was not processed.
    if (!result)
        result = gst_pad_push_event(base->sinkpad, event);

    return result;
}


static gboolean audiodecoder_open_init(AudioDecoder *decoder, GstCaps* caps)
{
    BaseDecoder *base = BASEDECODER(decoder);
    gint         mpeg_version = 0;
    gint         mpeg_layer = 0;
    GstEvent    *caps_event = NULL;

    if(caps && gst_caps_get_size(caps) > 0)
    {
        GstStructure* caps_struct = gst_caps_get_structure(caps, 0);

        if (gst_structure_has_name(caps_struct, "audio/mpeg"))
        {
            if (!gst_structure_get_int(caps_struct, "mpegversion", &mpeg_version))
                mpeg_version = 1;

            if (!gst_structure_get_int(caps_struct, "rate", &decoder->sample_rate))
                decoder->sample_rate = 44100;

            if (!gst_structure_get_int(caps_struct, "bitrate", &decoder->bit_rate))
                decoder->bit_rate = 0;

            gint mpeg_channels;
            if (!gst_structure_get_int(caps_struct, "channels", &mpeg_channels))
                mpeg_channels = 2;

            basedecoder_set_codec_data(base, caps_struct);

            if (4 == mpeg_version)
            {
#if NEW_CODEC_ID
                decoder->codec_id = AV_CODEC_ID_AAC;
#else
                decoder->codec_id = CODEC_ID_AAC;
#endif
                if (base->codec_data) // codec_data is optional for AAC
                {
                    //
                    // Get the number of channels from the Audio Specific Config
                    // which is what is passed in "codec_data"
                    //
                    // Ref: http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
                    //
                    guint8 channel_config = 0;
                    if (base->codec_data_size >= 2)
                    {
                        guint8 freq_index = (base->codec_data[0]&0x07) << 1 | (base->codec_data[1]&0x80) >> 7;
                        if (15 == freq_index)
                        {
                            if (base->codec_data_size >= 5)
                                channel_config = (base->codec_data[4]&0x78) >> 3;
                        }
                        else
                            channel_config = (base->codec_data[1]&0x78) >> 3;
                    }

                    if (channel_config > 0 && channel_config < 7)
                        decoder->num_channels = channel_config;
                    else if (7 == channel_config)
                        decoder->num_channels = 8;
                    else
                        decoder->num_channels = mpeg_channels;
                }
                else
                    decoder->num_channels = mpeg_channels;

                if (decoder->num_channels > 2)
                    decoder->num_channels = 2;

                decoder->samples_per_frame = 1024; // Note: AAC-LC has 960 spf
            }
            else
            {
#if NEW_CODEC_ID
                decoder->codec_id = AV_CODEC_ID_MP3;
#else
                decoder->codec_id = CODEC_ID_MP3;
#endif
                if (!gst_structure_get_int(caps_struct, "layer", &mpeg_layer))
                    mpeg_layer = 3;

                gint mpeg_audio_version;
                if (!gst_structure_get_int(caps_struct, "mpegaudioversion", &mpeg_audio_version))
                {
                    if (decoder->sample_rate >= 32000)
                        mpeg_audio_version = 1; // MPEG-1 audio
                    else if (decoder->sample_rate >= 16000)
                        mpeg_audio_version = 2; // MPEG-2 audio
                    else
                        mpeg_audio_version = 3; // MPEG-2.5 audio
                }

                decoder->num_channels = mpeg_channels;

                if (1 == mpeg_layer)
                    decoder->samples_per_frame = 384;
                else if (2 == mpeg_layer || 1 == mpeg_audio_version)
                    decoder->samples_per_frame = 1152;
                else
                    decoder->samples_per_frame = 576;
            }

#ifdef DEBUG_OUTPUT
            g_print("\nVersion %d rate %d channels %d samples %d layer %d\n",
                    mpeg_version, decoder->sample_rate, decoder->num_channels,
                    decoder->samples_per_frame, mpeg_layer);
#endif
        }
        else
            return FALSE; // Type is not "audio/mpeg"
    }

    if (!base->codec && !basedecoder_open_decoder(base, decoder->codec_id))
    {
        gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_LIBRARY_ERROR, GST_LIBRARY_ERROR_INIT,
                                 g_strdup("Couldn't create audiodecoder"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
        return FALSE;
    }

    // Limit the number of output channels to 2 because audiopanorama element accepts only up to 2 channels
    if (decoder->num_channels > AUDIODECODER_OUT_NUM_CHANNELS)
        decoder->num_channels = AUDIODECODER_OUT_NUM_CHANNELS;

    // Source caps: PCM audio.
    caps = gst_caps_new_simple("audio/x-raw",
                               "format", G_TYPE_STRING, "S16LE",
                               "layout", G_TYPE_STRING, "interleaved",
                               "rate", G_TYPE_INT, decoder->sample_rate,
                               "channels", G_TYPE_INT, decoder->num_channels,
                               NULL);

    decoder->bytes_per_sample = (AUDIODECODER_BITS_PER_SAMPLE/8) * decoder->num_channels;

    // Set the source caps.
    caps_event = gst_event_new_caps(caps);
    if (caps_event)
    {
        base->is_initialized = gst_pad_push_event(base->srcpad, caps_event);
    }

    gst_caps_unref(caps);

    return base->is_initialized;
}

static inline int16_t float_to_int(float sample)
{
    int value = (int)(sample * INT16_MAX);
    return value > INT16_MAX ? INT16_MAX : value < INT16_MIN ? INT16_MIN : (int16_t)value;
}

/*
 * Processes a buffer of MPEG audio data pushed to the sink pad.
 */
static GstFlowReturn audiodecoder_chain(GstPad *pad, GstObject *parent, GstBuffer *buf)
{
    AudioDecoder *decoder = AUDIODECODER(parent);
    BaseDecoder  *base = BASEDECODER(decoder);
    GstFlowReturn ret = GST_FLOW_OK;
    int           num_dec = NO_DATA_USED;
    GstMapInfo    info;
    GstMapInfo    info2;
    gboolean      unmap_buf = FALSE;

#if DECODE_AUDIO4
    gint          got_frame = 0;
    int           sample, ci;
 #else
    gint          outbuf_size = AVCODEC_MAX_AUDIO_FRAME_SIZE;
#endif

#ifdef VERBOSE_DEBUG
    g_print("audiodecoder: ts=%.4f, duration=%.4f ",
            GST_BUFFER_TIMESTAMP_IS_VALID(buf) ? (double)GST_BUFFER_TIMESTAMP(buf)/GST_SECOND : -1.0,
            GST_BUFFER_DURATION_IS_VALID(buf) ? (double)GST_BUFFER_DURATION(buf)/GST_SECOND : -1.0);
#endif

    // If we have incoming buffers with PTS, then use them.
    decoder->generate_pts = !GST_BUFFER_TIMESTAMP_IS_VALID(buf);

    // If between FLUSH_START and FLUSH_STOP, reject new buffers.
    if (base->is_flushing)
    {
        ret = GST_FLOW_FLUSHING;
        goto _exit;
    }

    // Reset state on discont if not after FLUSH_STOP.
    if (GST_BUFFER_IS_DISCONT(buf) && decoder->is_synced)
        audiodecoder_state_reset(decoder);

    if (decoder->initial_offset == GST_BUFFER_OFFSET_NONE)
    {
        decoder->initial_offset = GST_BUFFER_OFFSET_IS_VALID(buf) ?  GST_BUFFER_OFFSET(buf) : 0;
    }

    if (!base->is_initialized)
    {
        ret = GST_FLOW_ERROR;
        goto _exit;
    }

    if (!decoder->is_synced)
    {
        decoder->frame_duration = (guint) (GST_SECOND *
                                           (double) decoder->samples_per_frame /
                                           (double) decoder->sample_rate);

        // Derive sample count using the timestamp.
        guint64 frame_index = GST_BUFFER_TIMESTAMP(buf) / decoder->frame_duration;

        decoder->total_samples = frame_index * decoder->samples_per_frame;

        decoder->is_synced = TRUE;
    }

    av_init_packet(&decoder->packet);
    if (!gst_buffer_map(buf, &info, GST_MAP_READ))
    {
        goto _exit;
    }

    unmap_buf = TRUE;

    decoder->packet.data = info.data;
    decoder->packet.size = info.size;

#if DECODE_AUDIO4
    num_dec = avcodec_decode_audio4(base->context, base->frame, &got_frame, &decoder->packet);
#else
    num_dec = avcodec_decode_audio3(base->context, (int16_t*)decoder->samples, &outbuf_size, &decoder->packet);
#endif


#if DECODE_AUDIO4
    if (num_dec < 0 || !got_frame)
#else
    if (num_dec < 0 || outbuf_size == 0)
#endif
    {
#ifdef DEBUG_OUTPUT
        g_print("audiodecoder_chain error: %s\n", avelement_error_to_string(AVELEMENT(decoder), num_dec));
#endif
        goto _exit;
    }

    GstBuffer *outbuf = NULL;
#if DECODE_AUDIO4
    if (!audiodecoder_is_oformat_supported(base->frame->format))
    {
        gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_CORE_ERROR, GST_CORE_ERROR_NOT_IMPLEMENTED,
                                 g_strdup("Unsupported decoded audio format"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
        goto _exit;
    }

    int outbuf_size = av_samples_get_buffer_size(NULL, decoder->num_channels, base->frame->nb_samples, AV_SAMPLE_FMT_S16, 1);
    if (outbuf_size < 0) {
        goto _exit;
    }
#endif

    outbuf = gst_buffer_new_allocate(NULL, outbuf_size, NULL);
    // Bail out on error.
    if (outbuf == NULL)
    {
        if (ret != GST_FLOW_FLUSHING)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                     g_strdup("Decoded audio buffer allocation failed"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
        }
        goto _exit;
    }

    if (!gst_buffer_map(outbuf, &info2, GST_MAP_WRITE))
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(outbuf);
        gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                     g_strdup("Decoded audio buffer allocation failed"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
        goto _exit;
    }

#if DECODE_AUDIO4
    if (base->frame->format == AV_SAMPLE_FMT_S16P || base->frame->format == AV_SAMPLE_FMT_FLTP)
    {
        // Make sure we received expected data
        int cc = decoder->num_channels;
        for (ci = 0; ci < cc && ci < AUDIODECODER_OUT_NUM_CHANNELS; ci++)
        {
            if (base->frame->data[ci] == NULL)
            {
                gst_buffer_unmap(outbuf, &info2);
                // INLINE - gst_buffer_unref()
                gst_buffer_unref(outbuf);
                gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE,
                                     g_strdup("Audio decoding failed"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
                ret = GST_FLOW_ERROR;
                goto _exit;
            }
        }

        // Reformat the output frame into single buffer.
        int16_t *buffer = (int16_t*)info2.data;
        for (sample = 0; sample < base->frame->nb_samples; sample++)
        {
            for (ci = 0; ci < cc && ci < AUDIODECODER_OUT_NUM_CHANNELS; ci++)
            {
                switch (base->frame->format)
                {
                    case AV_SAMPLE_FMT_S16P:
                        buffer[cc * sample + ci] = ((int16_t*)base->frame->data[ci])[sample];
                        break;
                    case AV_SAMPLE_FMT_FLTP:
                        buffer[cc * sample + ci] = float_to_int(((float*)base->frame->data[ci])[sample]);
                        break;
                }
            }
        }
    }
    else if (base->frame->format == AV_SAMPLE_FMT_S16)
        memcpy(info2.data, base->frame->data[0], info2.size);
    else
    {
        gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_RESOURCE_ERROR, GST_RESOURCE_ERROR_NO_SPACE_LEFT,
                                 g_strdup("Unsupported decoder output format"), NULL, ("audiodecoder.c"), ("audiodecoder_chain"), 0);
        ret = GST_FLOW_ERROR;
        gst_buffer_unmap(outbuf, &info2);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(outbuf);
        goto _exit;
    }

#else
    memcpy(info.data, decoder->samples, info.size);
#endif

    gst_buffer_unmap(outbuf, &info2);

    // Set output buffer properties.
    if (decoder->generate_pts)
    {
        // Calculate the timestamp from the sample count and rate.
        GST_BUFFER_TIMESTAMP(outbuf) = gst_util_uint64_scale_int(decoder->total_samples, GST_SECOND, decoder->sample_rate);
        GST_BUFFER_DURATION(outbuf) = decoder->frame_duration;
    }
    else
    {
        GST_BUFFER_TIMESTAMP(outbuf) = GST_BUFFER_TIMESTAMP(buf);
        GST_BUFFER_DURATION(outbuf) = GST_BUFFER_DURATION(buf);
    }

    GST_BUFFER_OFFSET(outbuf) = decoder->total_samples;
    decoder->total_samples += outbuf_size / decoder->bytes_per_sample;

    GST_BUFFER_OFFSET_END(outbuf) = decoder->total_samples;

    if (decoder->is_discont) {
        GST_BUFFER_FLAG_SET(outbuf, GST_BUFFER_FLAG_DISCONT);
        decoder->is_discont = FALSE;
    }

    if (base->is_flushing)
    {
        // Unref the output buffer.
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(outbuf);

        ret = GST_FLOW_FLUSHING;
        goto _exit;
    }

#ifdef VERBOSE_DEBUG
    g_print("ret=%s, num_dec=%d, Buffer: size=%d, ts=%.4f, duration=%.4f, offset=%ld, offset_end=%ld\n",
            gst_flow_get_name(ret), num_dec, outbuf_size,
            (double)GST_BUFFER_TIMESTAMP(outbuf)/GST_SECOND, (double)GST_BUFFER_DURATION(outbuf)/GST_SECOND,
            GST_BUFFER_OFFSET(outbuf), GST_BUFFER_OFFSET_END(outbuf));
#endif

    ret = gst_pad_push(base->srcpad, outbuf);

_exit:

    if (unmap_buf)
        gst_buffer_unmap(buf, &info);

// INLINE - gst_buffer_unref()
    gst_buffer_unref(buf);

    return ret;
}

#if DECODE_AUDIO4
static gboolean audiodecoder_is_oformat_supported(int format)
{
    return (format == AV_SAMPLE_FMT_S16P || format == AV_SAMPLE_FMT_FLTP ||
            format == AV_SAMPLE_FMT_S16);
}
#endif

// --------------------------------------------------------------------------
gboolean audiodecoder_plugin_init(GstPlugin * audiodecoder) {
    GST_DEBUG_CATEGORY_INIT(audiodecoder_debug, AV_AUDIO_DECODER_PLUGIN_NAME,
            0, "JFX libavc based audiodecoder");

    return gst_element_register(audiodecoder, AV_AUDIO_DECODER_PLUGIN_NAME,
            0, TYPE_AUDIODECODER);
}
