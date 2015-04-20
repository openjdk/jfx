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

#include <new>

#include <gst/gst.h>
#include <gst/audio/gstaudioclock.h>

#include "dshowwrapper.h"
#include "Src.h"
#include "Sink.h"

#include <Bdaiface.h>
#include <Dvdmedia.h>
#include <Ks.h>
#include <Codecapi.h>
#include <dmodshow.h>
#include <Dmoreg.h>
#include <Wmcodecdsp.h>
#include <Mmreg.h>
#include <Strsafe.h>

using namespace std;

// Debug
#define MP2T_PTS_DEBUG 0
#define H264_PTS_DEBUG 0
#define AAC_PTS_DEBUG 0
#define EOS_DEBUG 0

enum CODEC_ID
{
    CODEC_ID_UNKNOWN = 0,
    CODEC_ID_AAC,
    CODEC_ID_H264, // HLS
    CODEC_ID_AVC1, // MP4
};

#define MAX_HEADER_SIZE 256
#define INPUT_BUFFERS_BEFORE_ERROR 500

// AAC
WCHAR* szAACDecoders[] = {
    L"{E1F1A0B8-BEEE-490d-BA7C-066C40B5E2B9}", // Microsoft AAC
    L"{19987CEE-DEE8-49DC-98EC-F21380AA9E68}", // MainConcept
    L"{2CCC9657-58A9-41AC-AA39-451202B98FAF}", // DivX
    L"{B51FABD7-8260-4C8A-82AD-6896FCF9AF92}", // MainConcept Demo
    //L"{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}", // Unknown (Uncomment this and comment decoders above to test error case when decoders are not available)
};

DECODER_SETTINGS eAACDecodersSettings[] = {
    DECODER_SETTING_FORCE_STEREO_OUTPUT, // Microsoft AAC
    DECODER_SETTING_NONE, // MainConcept
    DECODER_SETTING_NONE, // DivX
    DECODER_SETTING_NONE, // MainConcept Demo
    //DECODER_SETTING_NONE, // Unknown (Uncomment this and comment decoders above to test error case when decoders are not available)
};

// H.264/AVC
WCHAR* szAVCDecoders[] = {
    L"{212690FB-83E5-4526-8FD7-74478B7939CD}", // Microsft H.264 (CLSID_CMPEG2VidDecoderDS)
    L"{96B9D0ED-8D13-4171-A983-B84D88D627BE}", // MainConcept
    L"{6F513D27-97C3-453C-87FE-B24AE50B1601}", // DivX
    L"{535FD577-2F68-4FDC-934D-CEB0642D0D33}", // MainConcept Demo
    //L"{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}", // Unknown (Uncomment this and comment decoders above to test error case when decoders are not available)
};

MEDIA_FORMAT eAVCDecodersInputFormats[] = {
    MEDIA_FORMAT_VIDEO_AVC1, // Microsoft H.264
    MEDIA_FORMAT_VIDEO_AVC1, // MainConcept
    MEDIA_FORMAT_VIDEO_H264, // DivX
    MEDIA_FORMAT_VIDEO_AVC1, // MainConcept Demo
    //MEDIA_FORMAT_VIDEO_AVC1, // Unknown (Uncomment this and comment decoders above to test error case when decoders are not available)
};

MEDIA_FORMAT eAVCDecodersOutputFormats[] = {
    MEDIA_FORMAT_VIDEO_I420, // Microsoft H.264
    MEDIA_FORMAT_VIDEO_YV12, // MainConcept
    MEDIA_FORMAT_VIDEO_YV12, // DivX
    MEDIA_FORMAT_VIDEO_YV12, // MainConcept Demo
    //MEDIA_FORMAT_VIDEO_I420, // Unknown (Uncomment this and comment decoders above to test error case when decoders are not available)
};

enum
{
    PROP_0,
    PROP_CODEC_ID,
    PROP_IS_SUPPORTED,
};

#pragma pack(push)
#pragma pack(1)

typedef struct {
    guint8 configVersion;
    guint8 avcProfile;
    guint8 profileCompatibility;
    guint8 avcLevel;
    guint8 lengthSizeMinusOne;  // top 6 bits always 1, mask with 0x03
    guint8 spsCount;            // top 3 bits always 1, mask with 0x1f
} AVCCHeader;

#pragma pack(pop)

// MP2T
#define PMT_HEADER_SIZE          12
#define PES_HEADER_SIZE          6
#define PES_OPTIONAL_HEADER_SIZE 3
#define PMT_INFO_SIZE            5
#define CRC32_SIZE               4
#define STREAM_TYPE_H264         0x1B
#define STREAM_TYPE_AAC          0x0F
#define PTS_WRAPAROUND_THRESHOLD 600000000000 // 10 min

GST_DEBUG_CATEGORY_STATIC (gst_dshowwrapper_debug);
#define GST_CAT_DEFAULT gst_dshowwrapper_debug

// The input capabilities
static GstStaticPadTemplate sink_factory =
    GST_STATIC_PAD_TEMPLATE("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS(
    // AAC
    "audio/mpeg, "
    "mpegversion = (int)4; "
    // MP3
    "audio/mpeg, "
    "mpegversion = (int) 1, "
    "layer = (int) [ 1, 3 ], "
    "rate = (int) { 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 }, "
    "channels = (int) [ 1, 2 ]; "
    // H.264
    "video/x-h264; "
    // MPEG-2 Transport Stream
    "video/MP2T"
    ));

// The output capabilities
static GstStaticPadTemplate src_factory =
    GST_STATIC_PAD_TEMPLATE("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS(
    // PCM
    "audio/x-raw-int, "
    "endianness = (int) " G_STRINGIFY (G_LITTLE_ENDIAN) ", "
    "signed = (boolean) true, "
    "width = (int) 16, "
    "depth = (int) 16, "
    "rate = (int) { 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 }, "
    "channels = (int) [ 1, 6 ]; "
    // YV12
    "video/x-raw-yuv, "
    "format=(fourcc)YV12; "
    // H.264
    "video/x-h264; "
    // AAC
    "audio/mpeg, "
    "mpegversion = (int)4"
    ));

// Forward declarations
static void gst_dshowwrapper_dispose(GObject* object);
static void gst_dshowwrapper_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec);
static void gst_dshowwrapper_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec);

static GstStateChangeReturn dshowwrapper_change_state(GstElement* element, GstStateChange transition);
#if ENABLE_CLOCK
static GstClock* dshowwrapper_provide_clock(GstElement *element);
static GstClockTime dshowwrapper_clock_get_time(GstClock *clock, gpointer user_data);
#endif // ENABLE_CLOCK
static GstFlowReturn dshowwrapper_chain (GstPad* pad, GstBuffer* buf);

static const GstQueryType* dshowwrapper_get_sink_query_types (GstPad* pad);
static gboolean dshowwrapper_sink_query (GstPad* pad, GstQuery* query);
static gboolean dshowwrapper_sink_event (GstPad* pad, GstEvent* event);
static gboolean dshowwrapper_sink_set_caps (GstPad * pad, GstCaps * caps);
static gboolean dshowwrapper_activate(GstPad* pad);

static const GstQueryType* dshowwrapper_get_src_query_types (GstPad* pad);
static gboolean dshowwrapper_src_query (GstPad* pad, GstQuery* query);
static gboolean dshowwrapper_src_event (GstPad* pad, GstEvent* event);

static gboolean dshowwrapper_create_src_pad(GstDShowWrapper *decoder, GstPad **ppPad, GstCaps *caps, gchar *name, gboolean check_no_more_pads);

static gboolean dshowwrapper_is_decoder_by_codec_id_supported(gint codec_id);

// Static global mutex to protect ACM wrapper initialization
static HANDLE hMutex = CreateMutex(NULL, FALSE, NULL);

/***********************************************************************************
* Substitution for
* GST_BOILERPLATE (GstDShowWrapper, gst_dshowwrapper, GstElement, GST_TYPE_ELEMENT);
***********************************************************************************/
static GstElementClass *parent_class = NULL;

// GObject vmethod implementations
static void gst_dshowwrapper_base_init (gpointer gclass)
{
    GstElementClass *element_class;

    element_class = GST_ELEMENT_CLASS (gclass);

    gst_element_class_set_details_simple(element_class,
        "DShowWrapper",
        "Codec/Decoder/Audio/Video",
        "Direct Show Wrapper",
        "Oracle Corporation");

    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&src_factory));
    gst_element_class_add_pad_template (element_class,
        gst_static_pad_template_get (&sink_factory));
}

// Initialize dshowwrapper's class.
static void gst_dshowwrapper_class_init (GstDShowWrapperClass *klass)
{
    GstElementClass *gstelement_class = (GstElementClass*)klass;
    GObjectClass *gobject_class = (GObjectClass*)klass;

    gstelement_class->change_state = dshowwrapper_change_state;
#if ENABLE_CLOCK
    gstelement_class->provide_clock = dshowwrapper_provide_clock;
#endif // ENABLE_CLOCK

    gobject_class->dispose = gst_dshowwrapper_dispose;
    gobject_class->set_property = gst_dshowwrapper_set_property;
    gobject_class->get_property = gst_dshowwrapper_get_property;

    g_object_class_install_property (gobject_class, PROP_CODEC_ID,
        g_param_spec_int ("codec-id", "Codec ID", "Codec ID", -1, G_MAXINT, 0,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));

    g_object_class_install_property (gobject_class, PROP_IS_SUPPORTED,
        g_param_spec_boolean ("is-supported", "Is supported", "Is codec ID supported", FALSE,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));
}

static void gst_dshowwrapper_class_init_trampoline (gpointer g_class, gpointer data)
{
    parent_class = (GstElementClass*)g_type_class_peek_parent (g_class);
    gst_dshowwrapper_class_init((GstDShowWrapperClass*)g_class);
}

// Initialize the new element
// Instantiate pads and add them to element
// Set pad calback functions
// Initialize instance structure
static void gst_dshowwrapper_init (GstDShowWrapper *decoder, GstDShowWrapper *g_class)
{
    // Input
    decoder->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
    gst_element_add_pad (GST_ELEMENT (decoder), decoder->sinkpad);
    gst_pad_set_chain_function (decoder->sinkpad, dshowwrapper_chain);
    gst_pad_set_query_type_function(decoder->sinkpad, dshowwrapper_get_sink_query_types);
    gst_pad_set_query_function(decoder->sinkpad, dshowwrapper_sink_query);
    gst_pad_set_event_function(decoder->sinkpad, dshowwrapper_sink_event);
    gst_pad_set_setcaps_function(decoder->sinkpad, dshowwrapper_sink_set_caps);
    gst_pad_set_activate_function(decoder->sinkpad, dshowwrapper_activate);

    // Output
    dshowwrapper_create_src_pad(decoder, &decoder->srcpad[0], NULL, "src", TRUE);
    for (int i = 1; i < MAX_OUTPUT_DS_STREAMS; i++)
        decoder->srcpad[i] = NULL;

    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        decoder->pSink[i] = NULL;
        decoder->pISink[i] = NULL;
        decoder->is_sink_connected[i] = FALSE;
        decoder->offset[i] = 0;
        decoder->out_buffer[i] = NULL;
        decoder->last_pts[i] = GST_CLOCK_TIME_NONE;
        decoder->pMPEG2PIDMap[i] = NULL;
        decoder->eOutputFormat[i] = MEDIA_FORMAT_UNKNOWN;
        decoder->Pid[i] = 0;
        decoder->last_pts[i] = GST_CLOCK_TIME_NONE;
        decoder->offset_pts[i] = 0;
        decoder->is_eos[i] = FALSE;
    }

    decoder->pDSLock = new CCritSec();
    decoder->pGraph = NULL;
    decoder->pMediaControl = NULL;
    decoder->pSrc = NULL;
    decoder->pISrc = NULL;
    decoder->pDecoder = NULL;

    decoder->eInputFormat = MEDIA_FORMAT_UNKNOWN;

    decoder->eDecoderSettings = DECODER_SETTING_NONE;

    decoder->is_flushing = FALSE;
    decoder->is_eos_received = FALSE;

    decoder->enable_pts = FALSE;

    decoder->enable_mp3 = FALSE;
    decoder->acm_wrapper = FALSE;
    decoder->mp3_duration = -1;
    decoder->mp3_id3_size = -1;

    decoder->codec_id = CODEC_ID_UNKNOWN;

    decoder->is_data_produced = FALSE;
    decoder->input_buffers_count = 0;

    decoder->enable_position = FALSE;
    decoder->last_stop = GST_CLOCK_TIME_NONE;

    decoder->force_discontinuity = FALSE;

    decoder->get_pid = FALSE;
    decoder->map_pid = FALSE;
    decoder->first_map_pid = TRUE;
    decoder->skip_flush = FALSE;
    decoder->seek_position = 0;
    decoder->rate = 1.0;

#if ENABLE_CLOCK
    decoder->clock = NULL;
#endif // ENABLE_CLOCK

    decoder->set_base_pts = FALSE;
    decoder->base_pts = GST_CLOCK_TIME_NONE;

    decoder->pending_event = NULL;
}

static void gst_dshowwrapper_dispose(GObject* object)
{
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(object);

#if ENABLE_CLOCK
    if (decoder->clock != NULL)
    {
        gst_audio_clock_invalidate(decoder->clock);
        gst_object_unref(decoder->clock);
        decoder->clock = NULL;
    }
#endif // ENABLE_CLOCK

    if (decoder->pDSLock)
    {
        delete decoder->pDSLock;
        decoder->pDSLock = NULL;
    }

    if (decoder->pending_event)
    {
        // INLINE - gst_event_unref()
        gst_event_unref(decoder->pending_event);
        decoder->pending_event = NULL;
    }

    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        if (decoder->out_buffer[i] != NULL)
        {
            // INLINE - gst_buffer_unref()
            gst_buffer_unref (decoder->out_buffer[i]);
            decoder->out_buffer[i] = NULL;
        }
    }

    G_OBJECT_CLASS(parent_class)->dispose(object);
}

GType gst_dshowwrapper_get_type (void)
{
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = gst_type_register_static_full (GST_TYPE_ELEMENT,
            g_intern_static_string ("GstDShowWrapper"),
            sizeof (GstDShowWrapperClass),
            gst_dshowwrapper_base_init,
            NULL,
            gst_dshowwrapper_class_init_trampoline,
            NULL,
            NULL,
            sizeof (GstDShowWrapper),
            0,
            (GInstanceInitFunc) gst_dshowwrapper_init,
            NULL,
            (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

static void gst_dshowwrapper_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec)
{
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(object);
    switch (property_id)
    {
    case PROP_CODEC_ID:
        decoder->codec_id = g_value_get_int(value);
        break;
    default:
        break;
    }
}

static void gst_dshowwrapper_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec)
{
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(object);
    gboolean is_supported = FALSE;
    switch (property_id)
    {
    case PROP_IS_SUPPORTED:
        is_supported = dshowwrapper_is_decoder_by_codec_id_supported(decoder->codec_id);
        g_value_set_boolean(value, is_supported);
        break;
    default:
        break;
    }
}

static IPin* dshowwrapper_get_pin(IBaseFilter *pFilter, PIN_DIRECTION direction, int index)
{
    IEnumPins *pEnum = NULL;
    IPin *pPin = NULL;
    HRESULT hr = S_OK;
    int count = 0;

    if (pFilter == NULL)
        return NULL;

    hr = pFilter->EnumPins(&pEnum);
    if (FAILED(hr))
        return NULL;

    while(pEnum->Next(1, &pPin, 0) == S_OK)
    {
        PIN_DIRECTION directionThis;

        hr = pPin->QueryDirection(&directionThis);
        if (FAILED(hr))
        {
            pPin->Release();
            pEnum->Release();
            return NULL;
        }

        if (direction == directionThis && count == index)
        {
            // Found a match. Return the IPin pointer to the caller.
            pEnum->Release();
            return pPin;
        }
        else if (direction == directionThis)
        {
            count++;
        }

        // Release the pin for the next time through the loop.
        pPin->Release();
    }

    // No more pins. We did not find a match.
    pEnum->Release();

    return NULL;
}

static IPin* dshowwrapper_get_pin(IBaseFilter *pFilter, PIN_DIRECTION direction, REFGUID majorType, REFGUID subType)
{
    HRESULT hr = S_OK;
    int index = 0;
    IPin *pPin = NULL;

    do
    {
        pPin = dshowwrapper_get_pin(pFilter, direction, index);
        if (pPin == NULL)
            return NULL;

        IEnumMediaTypes *pEnum = NULL;
        AM_MEDIA_TYPE *pmt = NULL;
        hr = pPin->EnumMediaTypes(&pEnum);
        if (FAILED(hr))
            return NULL;

        while (hr = pEnum->Next(1, &pmt, NULL), hr == S_OK)
        {
            if (majorType == pmt->majortype && subType == pmt->subtype)
            {
                DeleteMediaType(pmt);
                return pPin;
            }
            DeleteMediaType(pmt);
        }

        pEnum->Release();
        pPin->Release();

        index++;
    } while (true);

    return NULL;
}

static gboolean dshowwrapper_connect_filters(GstDShowWrapper *decoder, IBaseFilter *pFilter1, IBaseFilter *pFilter2)
{
    HRESULT hr = S_OK;
    gboolean ret = TRUE;
    IPin *pOutput = NULL;
    IPin *pInput = NULL;

    int index = 0;
    do
    {
        pOutput = dshowwrapper_get_pin(pFilter1, PINDIR_OUTPUT, index);
        if (pOutput == NULL)
        {
            ret = FALSE;
            goto done;
        }

        pInput = dshowwrapper_get_pin(pFilter2, PINDIR_INPUT, 0);
        if (pInput == NULL)
        {
            ret = FALSE;
            goto done;
        }

        hr = decoder->pGraph->ConnectDirect(pOutput, pInput, NULL);
        if (SUCCEEDED(hr))
        {
            ret = TRUE;
            goto done;
        }

        if (pOutput != NULL)
        {
            pOutput->Release();
            pOutput = NULL;
        }

        if (pInput != NULL)
        {
            pInput->Release();
            pInput = NULL;
        }

        index++;
    } while (hr != S_OK);

done:
    if (pOutput != NULL)
        pOutput->Release();
    if (pInput != NULL)
        pInput->Release();

    return ret;
}

void dshowwrapper_release_sample(GstBuffer *pBuffer, sUserData *pUserData)
{
    if (pBuffer)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(pBuffer);
    }
}

void dshowwrapper_get_gst_buffer_sink(GstBuffer **ppBuffer, long lSize, sUserData *pUserData)
{
    GstDShowWrapper *decoder = (GstDShowWrapper*)pUserData->pUserData;
    GstFlowReturn ret = GST_FLOW_OK;

    ret = gst_pad_alloc_buffer(decoder->srcpad[pUserData->output_index], decoder->offset[pUserData->output_index], lSize, GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]), ppBuffer);
    if (ret == GST_FLOW_NOT_LINKED) // Re-create src pad
    {
        if (!dshowwrapper_create_src_pad(decoder, &decoder->srcpad[pUserData->output_index], NULL, NULL, TRUE))
        {
            *ppBuffer = NULL;
            return;
        }

        // Try again
        ret = gst_pad_alloc_buffer(decoder->srcpad[pUserData->output_index], decoder->offset[pUserData->output_index], lSize, GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]), ppBuffer);
    }

    if (ret != GST_FLOW_OK)
    {
        *ppBuffer = NULL;
    }
}

void dshowwrapper_deliver_post_process_mp2t(GstBuffer *pBuffer, GstDShowWrapper *decoder, int index)
{
    guint8 *data = NULL;
    guint size = 0;

    if (pBuffer == NULL)
        return;

    data = GST_BUFFER_DATA(pBuffer);
    size = GST_BUFFER_SIZE(pBuffer);

    if (data == NULL || size < 3)
        return;

    if (data[0] == 0x00 && data[1] == 0x00 && data[2] == 0x01) // PES header start
    {
        if ((data[6] & 0x80) == 0x80) // Optional PES header
        {
            __int64 PTS = 0;
            GstClockTime gst_pts = GST_CLOCK_TIME_NONE;

            if ((data[7] & 0x80) == 0x80) // Get PTS
            {
                PTS |= ((__int64)(data[9] & 0x0E) << 29);
                PTS |= (data[10] << 22);
                PTS |= ((data[11] & 0xFE) << 14);
                PTS |= (data[12] << 7);
                PTS |= ((data[13] & 0xFE) >> 1);
                PTS = PTS * 1000000 / 90;

                if (decoder->base_pts == GST_CLOCK_TIME_NONE)
                {
                    {
                        CAutoLock lock(decoder->pPTSLock);
                        if (decoder->base_pts == GST_CLOCK_TIME_NONE)
                        {
                            decoder->base_pts = PTS;
                        }
                    }
                }

                gst_pts = PTS + decoder->offset_pts[index];

                if (GST_CLOCK_TIME_IS_VALID(decoder->last_pts[index]))
                {
                    if (((gst_pts + PTS_WRAPAROUND_THRESHOLD) < (PTS_WRAPAROUND_THRESHOLD*2)) && (gst_pts + PTS_WRAPAROUND_THRESHOLD) < decoder->last_pts[index])
                    {
                        decoder->offset_pts[index] += (0x1FFFFFFFF * 1000000 / 90);
                        gst_pts = PTS + decoder->offset_pts[index];
                    }
                }

                if (gst_pts >= decoder->base_pts && gst_pts > decoder->last_pts[index] && gst_pts - decoder->last_pts[index] < PTS_WRAPAROUND_THRESHOLD)
                    GST_BUFFER_TIMESTAMP(pBuffer) = gst_pts - decoder->base_pts;

                if (!GST_CLOCK_TIME_IS_VALID(decoder->last_pts[index]) || (gst_pts > decoder->last_pts[index] && gst_pts - decoder->last_pts[index] < PTS_WRAPAROUND_THRESHOLD))
                    decoder->last_pts[index] = gst_pts;
            }

            guint8 optional_remaining_header_size = data[8];
            data += (PES_HEADER_SIZE + PES_OPTIONAL_HEADER_SIZE + optional_remaining_header_size);
            size -= (PES_HEADER_SIZE + PES_OPTIONAL_HEADER_SIZE + optional_remaining_header_size);
        }
        else
        {
            data += PES_HEADER_SIZE;
            size -= PES_HEADER_SIZE;
        }

        GST_BUFFER_DATA(pBuffer) = data;
        GST_BUFFER_SIZE(pBuffer) = size;
    }
}

int dshowwrapper_deliver(GstBuffer *pBuffer, sUserData *pUserData)
{
    GstFlowReturn ret = GST_FLOW_OK;
    GstDShowWrapper *decoder = (GstDShowWrapper*)pUserData->pUserData;

    if (decoder->is_eos[pUserData->output_index] || decoder->is_flushing)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(pBuffer);
        return 1; // DShow may not like failures
    }

    decoder->is_data_produced = TRUE;

    decoder->offset[pUserData->output_index] += GST_BUFFER_SIZE(pBuffer);
    GST_BUFFER_OFFSET_END(pBuffer) = decoder->offset[pUserData->output_index];

    // Caps might be change on pad, but buffers may come with old caps, since they we requested before caps change
    if (pUserData->bFlag1 && GST_BUFFER_CAPS(pBuffer) != GST_PAD_CAPS(pBuffer))
    {
        gst_buffer_set_caps(pBuffer, GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]));
        GST_BUFFER_FLAG_SET(pBuffer, GST_BUFFER_FLAG_DISCONT); // Caps changed
    }

    if (decoder->out_buffer[pUserData->output_index] != NULL)
    {
        if (decoder->enable_pts)
        {
            if (GST_BUFFER_TIMESTAMP_IS_VALID(pBuffer) && GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]) = GST_BUFFER_TIMESTAMP(pBuffer) - GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]);
            else
                GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]) = GST_CLOCK_TIME_NONE;
        }
        else
        {
            GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]) = GST_CLOCK_TIME_NONE;
            GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]) = GST_CLOCK_TIME_NONE;
        }

        if (decoder->enable_position && GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && (decoder->last_stop < GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]) || !GST_CLOCK_TIME_IS_VALID(decoder->last_stop)))
            decoder->last_stop = GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]);

        if ((decoder->eInputFormat == MEDIA_FORMAT_AUDIO_AAC || decoder->eInputFormat == MEDIA_FORMAT_VIDEO_H264) && GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]))
        {
            // Do not deliver buffers with backward PTS. GStreamer does not like it.
            // Use it only for uncomressed data. For compressed it is valid to have backward PTS.
            if ((decoder->last_pts[pUserData->output_index] != GST_CLOCK_TIME_NONE && GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]) < decoder->last_pts[pUserData->output_index]) || (gint64)GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]) < 0)
            {
                // INLINE - gst_buffer_unref()
                gst_buffer_unref(decoder->out_buffer[pUserData->output_index]);
                decoder->out_buffer[pUserData->output_index] = NULL;
                decoder->out_buffer[pUserData->output_index] = pBuffer;
                return 1;
            }

            decoder->last_pts[pUserData->output_index] = GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]);
        }

        if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T && (decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_VIDEO_H264 || decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_AUDIO_AAC))
        {
            dshowwrapper_deliver_post_process_mp2t(decoder->out_buffer[pUserData->output_index], decoder, pUserData->output_index);
        }

#if MP2T_PTS_DEBUG
        if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T)
        {
            if (decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_VIDEO_H264)
            {
                if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                    g_print("AMDEBUG MP2T H264 %I64u %I64u\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]), GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]));
                else if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && !GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                    g_print("AMDEBUG MP2T H264 %I64u -1\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]));
                else
                    g_print("AMDEBUG MP2T H264 -1\n");
            }
            if (decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_AUDIO_AAC)
            {
                if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                    g_print("AMDEBUG MP2T AAC  %I64u %I64u\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]), GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]));
                else if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && !GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                    g_print("AMDEBUG MP2T AAC  %I64u -1\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]));
                else
                    g_print("AMDEBUG MP2T AAC  -1\n");
            }
        }
#endif
#if H264_PTS_DEBUG
        if (decoder->eInputFormat == MEDIA_FORMAT_VIDEO_H264)
        {
            if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                g_print("AMDEBUG H264 %I64u %I64u\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]), GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]));
            else if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && !GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                g_print("AMDEBUG H264 %I64u -1\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]));
            else
                g_print("AMDEBUG H264 -1\n");
        }
#endif
#if AAC_PTS_DEBUG
        if (decoder->eInputFormat == MEDIA_FORMAT_AUDIO_AAC)
        {
            if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                g_print("AMDEBUG AAC  %I64u %I64u\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]), GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]));
            else if (GST_BUFFER_TIMESTAMP_IS_VALID(decoder->out_buffer[pUserData->output_index]) && !GST_BUFFER_DURATION_IS_VALID(decoder->out_buffer[pUserData->output_index]))
                g_print("AMDEBUG AAC  %I64u -1\n", GST_BUFFER_TIMESTAMP(decoder->out_buffer[pUserData->output_index]));
            else
                g_print("AMDEBUG AAC  -1\n");
        }
#endif

        ret = gst_pad_push(decoder->srcpad[pUserData->output_index], decoder->out_buffer[pUserData->output_index]);
        decoder->out_buffer[pUserData->output_index] = NULL;

        // Unref pBuffer if we will return
        if (decoder->is_eos[pUserData->output_index] || decoder->is_flushing || ret != GST_FLOW_OK)
        {
            // INLINE - gst_buffer_unref()
            gst_buffer_unref(pBuffer);
        }

        if (decoder->is_eos[pUserData->output_index] || decoder->is_flushing)
            return 1;
        else if (ret != GST_FLOW_OK)
            return 0;
    }

    decoder->out_buffer[pUserData->output_index] = pBuffer;

    return 1;
}

int dshowwrapper_sink_event(int sinkEvent, void *pData, int size, sUserData *pUserData)
{
    GstDShowWrapper *decoder = (GstDShowWrapper*)pUserData->pUserData;

    switch (sinkEvent)
    {
    case SINK_EOS:
        if (decoder->is_eos[pUserData->output_index])
            break;

        if (!decoder->is_data_produced)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to decode stream"), NULL, ("dshowwrapper.c"), ("dshowwrapper_sink_event"), 0);
            decoder->is_data_produced = TRUE; // Do not send more errors
        }

        // Deliver last buffer
        if (decoder->out_buffer[pUserData->output_index] != NULL)
        {
            GST_BUFFER_DURATION(decoder->out_buffer[pUserData->output_index]) = GST_CLOCK_TIME_NONE;
            gst_pad_push(decoder->srcpad[pUserData->output_index], decoder->out_buffer[pUserData->output_index]);
            decoder->out_buffer[pUserData->output_index] = NULL;
        }

        decoder->is_eos[pUserData->output_index] = TRUE;
#if EOS_DEBUG
        if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T && (decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_VIDEO_AVC1 || decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_VIDEO_H264))
            g_print("AMDEBUG EOS MP2T H264\n");
        else if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T && decoder->eOutputFormat[pUserData->output_index] == MEDIA_FORMAT_AUDIO_AAC)
            g_print("AMDEBUG EOS MP2T AAC\n");
        else if (decoder->eInputFormat == MEDIA_FORMAT_VIDEO_AVC1 || decoder->eInputFormat == MEDIA_FORMAT_VIDEO_H264)
            g_print("AMDEBUG EOS H264\n");
        else if (decoder->eInputFormat == MEDIA_FORMAT_AUDIO_AAC)
            g_print("AMDEBUG EOS AAC\n");
#endif
        gst_pad_push_event (decoder->srcpad[pUserData->output_index], gst_event_new_eos());
        break;
    case SINK_CODEC_DATA:
        if (pData != NULL && size > 0)
        {
            GstBuffer *pBuffer = gst_buffer_new_and_alloc(size);
            memcpy(GST_BUFFER_DATA(pBuffer), pData, size);
            GstCaps *caps = gst_caps_copy(GST_PAD_CAPS(decoder->srcpad));
            gst_caps_set_simple(caps, "codec_data", GST_TYPE_BUFFER, pBuffer, NULL);
            gst_pad_set_caps(decoder->srcpad[pUserData->output_index], caps);
            gst_caps_unref(caps);
            // INLINE - gst_buffer_unref()
            gst_buffer_unref (pBuffer);
        }
        break;
    case SINK_AUDIO_RATE:
        if (pData != NULL && size == sizeof(int))
        {
            int rate = *((int*)pData);

            GstCaps *caps = gst_caps_copy(GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]));
            gst_caps_set_simple(caps, "rate", G_TYPE_INT, rate, NULL);
            gst_pad_set_caps(decoder->srcpad[pUserData->output_index], caps);
            gst_caps_unref(caps);
        }
        break;
    case SINK_AUDIO_CHANNELS:
        if (pData != NULL && size == sizeof(int))
        {
            int channels = *((int*)pData);

            GstCaps *caps = gst_caps_copy(GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]));
            gst_caps_set_simple(caps, "channels", G_TYPE_INT, channels, NULL);
            gst_pad_set_caps(decoder->srcpad[pUserData->output_index], caps);
            gst_caps_unref(caps);
        }
        break;
    case SINK_VIDEO_RESOLUTION:
        if (pData != NULL && size == sizeof(__int64))
        {
            __int64 resolution = *((__int64*)pData);
            int width = (resolution >> 32) & 0x00000000FFFFFFFF;
            int height = resolution & 0x00000000FFFFFFFF;

            GstCaps *caps = gst_caps_copy(GST_PAD_CAPS(decoder->srcpad[pUserData->output_index]));
            if (decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] == MEDIA_FORMAT_VIDEO_I420)
            {
                gst_caps_set_simple(caps,
                    "width", G_TYPE_INT, width,
                    "height", G_TYPE_INT, height,
                    "offset-y", G_TYPE_INT, 0,
                    "offset-v", G_TYPE_INT, (1920*height+((1920*height)/4)),
                    "offset-u", G_TYPE_INT, 1920*height,
                    "stride-y", G_TYPE_INT, 1920,
                    "stride-v", G_TYPE_INT, 1920/2,
                    "stride-u", G_TYPE_INT, 1920/2,
                    NULL);
            }
            else if (decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] == MEDIA_FORMAT_VIDEO_YV12)
            {
                gst_caps_set_simple(caps,
                    "width", G_TYPE_INT, width,
                    "height", G_TYPE_INT, height,
                    NULL);
            }
            gst_pad_set_caps(decoder->srcpad[pUserData->output_index], caps);
            gst_caps_unref(caps);
        }
        break;
    default:
        break;
    }

    return 1;
}

static gboolean dshowwrapper_create_graph_sinks(GstDShowWrapper *decoder)
{
    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        if (decoder->pISink[i] && !decoder->is_sink_connected[i])
        {
            int const arraysize = 7;
            WCHAR pszDest[arraysize];
            size_t cbDest = arraysize * sizeof(WCHAR);

            decoder->is_sink_connected[i] = TRUE;

            HRESULT hr = StringCbPrintfW(pszDest, cbDest, L"Sink-%d", i);
            if (FAILED(hr))
            {
                return FALSE;
            }

            hr = decoder->pGraph->AddFilter(decoder->pISink[i], pszDest);
            if (FAILED(hr))
            {
                return FALSE;
            }

            if (!dshowwrapper_connect_filters(decoder, decoder->pDecoder, decoder->pISink[i]))
            {
                return FALSE;
            }
        }
    }

    return TRUE;
}

static gboolean dshowwrapper_create_graph(GstDShowWrapper *decoder)
{
    HRESULT hr = S_OK;

    CAutoLock lock(decoder->pDSLock);

    if (decoder == NULL)
        return FALSE;

    if (decoder->pGraph != NULL)
        return TRUE;

    // Create filter graph
    hr = CoCreateInstance(CLSID_FilterGraph, NULL, CLSCTX_INPROC_SERVER, IID_IFilterGraph, (void**)&decoder->pGraph);
    if (FAILED(hr))
    {
        return FALSE;
    }

    // Add source
    hr = decoder->pGraph->AddFilter(decoder->pISrc, L"Source");
    if (FAILED(hr))
    {
        return FALSE;
    }

    // Add decoder
    hr = decoder->pGraph->AddFilter(decoder->pDecoder, L"Decoder");
    if (FAILED(hr))
    {
        return FALSE;
    }

    if (!dshowwrapper_connect_filters(decoder, decoder->pISrc, decoder->pDecoder))
    {
        return FALSE;
    }

    // Add sinks and connect them
    if (!dshowwrapper_create_graph_sinks(decoder))
        return FALSE;

    //  IMediaFilter
    IMediaFilter *pMediaFilter = NULL;
    hr = decoder->pGraph->QueryInterface(IID_IMediaFilter, (void**)&pMediaFilter);
    if (SUCCEEDED(hr))
    {
        pMediaFilter->SetSyncSource(NULL);
        pMediaFilter->Release();
    }

    // IMediaControl
    hr = decoder->pGraph->QueryInterface(IID_IMediaControl, (void**)&decoder->pMediaControl);
    if (FAILED(hr))
    {
        return FALSE;
    }

    hr = decoder->pMediaControl->Run();
    if (FAILED(hr))
    {
        return FALSE;
    }

    return TRUE;
}

static void dshowwrapper_destroy_graph (GstDShowWrapper *decoder)
{
    bool bCallCoUninitialize = true;

    if (FAILED(CoInitialize(NULL)))
        bCallCoUninitialize = false;

    CAutoLock lock(decoder->pDSLock);

    if (decoder->pPTSLock)
    {
        delete decoder->pPTSLock;
        decoder->pPTSLock = NULL;
    }

    if (decoder->pPIDLock)
    {
        delete decoder->pPIDLock;
        decoder->pPIDLock = NULL;
    }

    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        if (decoder->pMPEG2PIDMap[i] != NULL)
        {
            decoder->pMPEG2PIDMap[i]->Release();
            decoder->pMPEG2PIDMap[i] = NULL;
        }
    }

    if (decoder->pMediaControl != NULL)
    {
        decoder->pMediaControl->Stop();
        OAFilterState fs = 0;
        decoder->pMediaControl->GetState(5000, &fs);
        decoder->pMediaControl->Release();
        decoder->pMediaControl = NULL;
    }

    if (decoder->pISrc != NULL)
    {
        if (decoder->pGraph != NULL)
        {
            decoder->pGraph->RemoveFilter(decoder->pISrc);
        }
        decoder->pISrc->Release();
        decoder->pISrc = NULL;
        decoder->pSrc = NULL;
    }

    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        if (decoder->pISink[i] != NULL)
        {
            if (decoder->pGraph != NULL)
            {
                decoder->pGraph->RemoveFilter(decoder->pISink[i]);
            }
            decoder->pISink[i]->Release();
            decoder->pISink[i] = NULL;
            decoder->pSink[i] = NULL;
        }
    }

#if ENABLE_CLOCK
    if (decoder->pDSClock != NULL)
    {
        decoder->pDSClock->Release();
        decoder->pDSClock = NULL;
    }
#endif // ENABLE_CLOCK

    if (decoder->pDecoder != NULL)
    {
        if (decoder->pGraph != NULL)
        {
            decoder->pGraph->RemoveFilter(decoder->pDecoder);
        }
        decoder->pDecoder->Release();
        decoder->pDecoder = NULL;
    }

    if (decoder->pGraph != NULL)
    {
        decoder->pGraph->Release();
        decoder->pGraph = NULL;
    }

    if (bCallCoUninitialize)
        CoUninitialize();
}

gsize dshowwrapper_get_avc_config(void *in, gsize in_size, BYTE *out, gsize out_size, guint *avcProfile, guint *avcLevel, guint *lengthSizeMinusOne)
{
    guintptr bdata = (guintptr)in;
    AVCCHeader *header = NULL;
    guint ppsCount = 0;
    guint16 structSize = 0;
    guint ii = 0;
    gsize size = 0;
    gsize in_bytes_count = 0;
    gsize out_bytes_count = 0;

    if (in_size < sizeof(AVCCHeader))
        return 0;

    header = (AVCCHeader*)in;

    header->lengthSizeMinusOne &= 0x03;
    header->spsCount &= 0x1F;

    *avcProfile = header->avcProfile;
    *avcLevel = header->avcLevel;
    *lengthSizeMinusOne = header->lengthSizeMinusOne;

    bdata += sizeof(AVCCHeader); // length of first SPS struct, if any
    in_bytes_count += sizeof(AVCCHeader);
    
    for (ii = 0; ii < header->spsCount; ii++) {
        
        if ((in_bytes_count + 2) > in_size)
            return 0;

        structSize = ((guint16)*(guint8*)bdata) << 8;
        bdata++;
        structSize |= (guint16)*(guint8*)bdata;
        bdata++;
        
        out_bytes_count += (structSize + 2);
        if (out_bytes_count > out_size)
            return 0;

        in_bytes_count += structSize;
        if (in_bytes_count > in_size)
            return 0;

        memcpy(out, ((guint8*)bdata - 2), structSize + 2);
        size += structSize + 2;
        out += size;
        bdata += structSize;
    }

    if ((in_bytes_count + 1) > in_size)
            return 0;

    ppsCount = *(guint8*)bdata;
    bdata++;

    in_bytes_count += 1;

    for (ii = 0; ii < ppsCount; ii++) {
        
        if ((in_bytes_count + 2) > in_size)
            return 0;

        structSize = ((guint16)*(guint8*)bdata) << 8;
        bdata++;
        structSize |= (guint16)*(guint8*)bdata;
        bdata++;

        out_bytes_count += (structSize + 2);
        if (out_bytes_count > out_size)
            return 0;

        in_bytes_count += structSize;
        if (in_bytes_count > in_size)
            return 0;

        memcpy(out, ((guint8*)bdata - 2), structSize + 2);
        size += structSize + 2;
        out += size;
        bdata += structSize;
    }

    return size;
}

static gboolean dshowwrapper_is_decoder_by_codec_id_supported(gint codec_id)
{
    IBaseFilter *pFilter = NULL;
    HRESULT hr = S_OK;
    int count = 0;
    CLSID decoderCLSID = GUID_NULL;
    gboolean result = FALSE;
    bool bCallCoUninitialize = true;

    if (FAILED(CoInitialize(NULL)))
        bCallCoUninitialize = false;

    switch(codec_id)
    {
    case CODEC_ID_AAC:
        count = sizeof(szAACDecoders)/sizeof(WCHAR*);
        decoderCLSID = GUID_NULL;
        for (int i = 0; i < count; i++)
        {
            hr = CLSIDFromString(szAACDecoders[i], &decoderCLSID);
            if (SUCCEEDED(hr))
            {
                hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&pFilter);
                if (pFilter != NULL)
                    pFilter->Release();
                if (SUCCEEDED(hr))
                {
                    result = TRUE;
                    goto done;
                }
            }
        }
        break;
    case CODEC_ID_AVC1:
        count = sizeof(szAACDecoders)/sizeof(WCHAR*);
        decoderCLSID = GUID_NULL;
        for (int i = 0; i < count; i++)
        {
            hr = CLSIDFromString(szAVCDecoders[i], &decoderCLSID);
            if (SUCCEEDED(hr))
            {
                hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&pFilter);
                if (pFilter != NULL)
                    pFilter->Release();
                if (SUCCEEDED(hr))
                {
                    result = TRUE;
                    goto done;
                }
            }
        }
        break;
    case CODEC_ID_H264:
        decoderCLSID = GUID_NULL;
        hr = CLSIDFromString(L"{212690FB-83E5-4526-8FD7-74478B7939CD}", &decoderCLSID);
        if (SUCCEEDED(hr))
        {
            hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&pFilter);
            if (pFilter != NULL)
                pFilter->Release();
            if (SUCCEEDED(hr))
            {
                result = TRUE;
                goto done;
            }
        }
        break;
    }

done:
    if (bCallCoUninitialize)
        CoUninitialize();

    return result;
}

static gboolean dshowwrapper_create_ds_source(GstDShowWrapper *decoder, sInputFormat *pInputFormat)
{
    HRESULT hr = S_OK;

    decoder->pSrc = new CSrc(&hr);
    if (decoder->pSrc == NULL || FAILED(hr))
    {
        return FALSE;
    }

    hr = decoder->pSrc->InitMediaType(pInputFormat);
    if (FAILED(hr))
    {
        return FALSE;
    }

    sUserData userData;
    ZeroMemory(&userData, sizeof(sUserData));
    userData.pUserData = (void*)decoder;
    hr = decoder->pSrc->SetUserData(&userData);
    if (FAILED(hr))
    {
        return FALSE;
    }

    hr = decoder->pSrc->SetReleaseSampleCallback(&dshowwrapper_release_sample);
    if (FAILED(hr))
    {
        return FALSE;
    }

    hr = decoder->pSrc->QueryInterface(IID_IBaseFilter, (void**)&decoder->pISrc);
    if (FAILED(hr))
    {
        return FALSE;
    }

    return TRUE;
}

static gboolean dshowwrapper_create_ds_sink(GstDShowWrapper *decoder, sOutputFormat *pOutputFormat, int index, bool setGeneralCallbaks)
{
    HRESULT hr = S_OK;

    decoder->pSink[index] = new CSink(&hr);
    if (decoder->pSink[index] == NULL || FAILED(hr))
    {
        return FALSE;
    }

    hr = decoder->pSink[index]->InitMediaType(pOutputFormat);
    if (FAILED(hr))
    {
        return FALSE;
    }

    sUserData userData;
    ZeroMemory(&userData, sizeof(sUserData));
    userData.pUserData = (void*)decoder;
    userData.output_index = index;
    hr = decoder->pSink[index]->SetUserData(&userData);
    if (FAILED(hr))
    {
        return FALSE;
    }

    if (setGeneralCallbaks)
    {
        hr = decoder->pSink[index]->SetGetGstBufferCallback(&dshowwrapper_get_gst_buffer_sink);
        if (FAILED(hr))
        {
            return FALSE;
        }

        hr = decoder->pSink[index]->SetReleaseSampleCallback(&dshowwrapper_release_sample);
        if (FAILED(hr))
        {
            return FALSE;
        }

        hr = decoder->pSink[index]->SetDeliverCallback(&dshowwrapper_deliver);
        if (FAILED(hr))
        {
            return FALSE;
        }

        hr = decoder->pSink[index]->SetSinkEventCallback(&dshowwrapper_sink_event);
        if (FAILED(hr))
        {
            return FALSE;
        }
    }

    hr = decoder->pSink[index]->QueryInterface(IID_IBaseFilter, (void**)&decoder->pISink[index]);
    if (FAILED(hr))
    {
        return FALSE;
    }

    return TRUE;
}

static gboolean dshowwrapper_load_decoder_aac(GstStructure *s, GstDShowWrapper *decoder)
{
    gboolean ret = FALSE;
    gint rate = 48000;
    gint channels = 2;

    // Load decoder
    int count = sizeof(szAACDecoders)/sizeof(WCHAR*);
    CLSID decoderCLSID = GUID_NULL;
    for (int i = 0; i < count; i++)
    {
        HRESULT hr = CLSIDFromString(szAACDecoders[i], &decoderCLSID);
        if (SUCCEEDED(hr))
        {
            hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
            if (SUCCEEDED(hr))
            {
                // Check if filter supports required media types
                if (i == 0) // Only check Microsoft filters. Other may not report proper types.
                {
                    IPin *pPin = NULL;
                    if (gst_structure_get_int(s, "rate", &rate) && gst_structure_get_int(s, "channels", &channels))
                    {
                        pPin = dshowwrapper_get_pin(decoder->pDecoder, PINDIR_INPUT, MEDIATYPE_Audio, MEDIASUBTYPE_RAW_AAC1);
                    }
                    else
                    {
                        pPin = dshowwrapper_get_pin(decoder->pDecoder, PINDIR_INPUT, MEDIATYPE_Audio, MEDIASUBTYPE_MPEG_ADTS_AAC);
                    }

                    if (pPin == NULL)
                    {
                        decoder->pDecoder->Release();
                        decoder->pDecoder = NULL;
                        continue;
                    }
                    else
                    {
                        pPin->Release();
                    }
                }

                decoder->eDecoderSettings = eAACDecodersSettings[i];
                decoder->eInputFormat = MEDIA_FORMAT_AUDIO_AAC;
                break;
            }
        }
    }

    if (decoder->pDecoder == NULL)
        return FALSE;
        
    // Init input
    sInputFormat inputFormat;
    ZeroMemory(&inputFormat, sizeof(sInputFormat));
    WAVEFORMATEX *wfx = NULL;

    if (gst_structure_get_int(s, "rate", &rate) && gst_structure_get_int(s, "channels", &channels))
    {
        const GValue *v = NULL;
        GstBuffer *codec_data = NULL;
        gint codec_data_size = 0;

        v = gst_structure_get_value(s, "codec_data");
        if (v != NULL)
        {
            codec_data = gst_value_get_buffer(v);
            if (codec_data != NULL)
                codec_data_size = GST_BUFFER_SIZE(codec_data);
        }

        inputFormat.type = MEDIATYPE_Audio;
        inputFormat.subtype = MEDIASUBTYPE_RAW_AAC1;
        inputFormat.bFixedSizeSamples = FALSE;
        inputFormat.bTemporalCompression = TRUE;
        inputFormat.lSampleSize = 1;
        inputFormat.formattype = FORMAT_WaveFormatEx;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(WAVEFORMATEX) + codec_data_size];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(WAVEFORMATEX) + codec_data_size);
        inputFormat.length = sizeof(WAVEFORMATEX) + codec_data_size;

        wfx = (WAVEFORMATEX*)inputFormat.pFormat;
        wfx->wFormatTag = WAVE_FORMAT_RAW_AAC1;
        wfx->nChannels = channels;
        wfx->nSamplesPerSec = rate;
        wfx->nBlockAlign = 1;
        if (codec_data_size > 0)
        {
            wfx->cbSize = codec_data_size;
            memcpy(inputFormat.pFormat + sizeof(WAVEFORMATEX), GST_BUFFER_DATA(codec_data), codec_data_size);
        }
    }
    else
    {
        // ADTS AAC specific
        decoder->enable_position = TRUE;
        decoder->enable_pts = TRUE;

        inputFormat.type = MEDIATYPE_Audio;
        inputFormat.subtype = MEDIASUBTYPE_MPEG_ADTS_AAC;
        inputFormat.bFixedSizeSamples = FALSE;
        inputFormat.bTemporalCompression = TRUE;
        inputFormat.lSampleSize = 1;
        inputFormat.formattype = FORMAT_WaveFormatEx;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(WAVEFORMATEX)];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(WAVEFORMATEX));
        inputFormat.length = sizeof(WAVEFORMATEX);
        wfx = (WAVEFORMATEX*)inputFormat.pFormat;
        wfx->wFormatTag = WAVE_FORMAT_MPEG_ADTS_AAC;
        wfx->nChannels = 2;
        wfx->nSamplesPerSec = 48000;
        wfx->nBlockAlign = 1;
    }

    if (!dshowwrapper_create_ds_source(decoder, &inputFormat))
        goto exit;

    // Init output
    sOutputFormat outputFormat;
    ZeroMemory(&outputFormat, sizeof(sOutputFormat));

    if ((decoder->eDecoderSettings & DECODER_SETTING_FORCE_STEREO_OUTPUT) == DECODER_SETTING_FORCE_STEREO_OUTPUT)
    {
        channels = 2;
        outputFormat.bForceStereoOutput = TRUE;
    }

    // Set srcpad caps
    GstCaps *caps = NULL;
    caps = gst_caps_new_simple ("audio/x-raw-int",
        "rate", G_TYPE_INT, rate,
        "channels", G_TYPE_INT, channels,
        "endianness", G_TYPE_INT, G_LITTLE_ENDIAN,
        "width", G_TYPE_INT, 16,
        "depth", G_TYPE_INT, 16,
        "signed", G_TYPE_BOOLEAN, true,
        NULL);
    gst_pad_set_caps(decoder->srcpad[0], caps);
    gst_caps_unref(caps);

    outputFormat.type = MEDIATYPE_Audio;
    outputFormat.subtype = MEDIASUBTYPE_PCM;
    outputFormat.bFixedSizeSamples = TRUE;
    outputFormat.bTemporalCompression = FALSE;
    outputFormat.lSampleSize = 1;
    outputFormat.formattype = FORMAT_WaveFormatEx;
    outputFormat.pFormat = new (nothrow) BYTE[sizeof(WAVEFORMATEX)];
    if (outputFormat.pFormat == NULL)
        goto exit;
    memset(outputFormat.pFormat, 0, sizeof(WAVEFORMATEX));
    outputFormat.length = sizeof(WAVEFORMATEX);

    wfx = (WAVEFORMATEX*)outputFormat.pFormat;
    wfx->wFormatTag = WAVE_FORMAT_PCM;
    wfx->nChannels = channels;
    wfx->nSamplesPerSec = rate;
    wfx->nAvgBytesPerSec = channels*rate*(16/8);
    wfx->nBlockAlign = channels*(16/8);
    wfx->wBitsPerSample = 16;
    wfx->cbSize = 0;

    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, 0, true))
        goto exit;

    ret = TRUE;

exit:
    if (inputFormat.pFormat != NULL)
        delete [] inputFormat.pFormat;
    if (outputFormat.pFormat != NULL)
        delete [] outputFormat.pFormat;

    return ret;
}

static gboolean dshowwrapper_load_decoder_mp3(GstStructure *s, GstDShowWrapper *decoder)
{
    gboolean ret = FALSE;
    gint layer = 0;

    if (!gst_structure_get_int(s, "layer", &layer))
        return FALSE;

    if (layer != 1 && layer != 2 && layer != 3)
        return FALSE;

    // Load decoder
    HRESULT hr = S_OK;

    if (layer == 3)
    {
        // Try to load DMO Wrapper
        hr = CoCreateInstance(CLSID_DMOWrapperFilter, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
        if (FAILED(hr))
        {
            return FALSE;
        }

        IDMOWrapperFilter *pDmoWrapper;
        hr = decoder->pDecoder->QueryInterface(__uuidof(IDMOWrapperFilter), (void**)&pDmoWrapper);
        if (FAILED(hr))
        {
            return FALSE;
        }

        // Initialize the filter.
        hr = pDmoWrapper->Init(CLSID_CMP3DecMediaObject, DMOCATEGORY_AUDIO_DECODER);
        pDmoWrapper->Release();
        if (FAILED(hr))
        {
            decoder->pDecoder->Release();
            decoder->pDecoder = NULL;

            // Try to load ACM Wrapper
            hr = CoCreateInstance(CLSID_ACMWrapper, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
            if (FAILED(hr))
            {
                return FALSE;
            }

            decoder->enable_mp3 = TRUE;
            decoder->acm_wrapper = TRUE;
        }

        decoder->eInputFormat = MEDIA_FORMAT_AUDIO_MP3;
        decoder->enable_mp3 = TRUE;
    }
    else if (layer == 1 || layer == 2)
    {
        hr = CoCreateInstance(CLSID_CMpegAudioCodec, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
        if (FAILED(hr))
        {
            return FALSE;
        }

        decoder->enable_mp3 = TRUE;
    }

    // Get common values for input and output
    gint rate = 0;
    gint channels = 0;

    if (!gst_structure_get_int(s, "rate", &rate))
        return FALSE;

    if (!gst_structure_get_int(s, "channels", &channels))
        return FALSE;

    // Init input
    sInputFormat inputFormat;
    ZeroMemory(&inputFormat, sizeof(sInputFormat));
    inputFormat.type = MEDIATYPE_Audio;
    inputFormat.bFixedSizeSamples = FALSE;
    inputFormat.bTemporalCompression = TRUE;
    inputFormat.lSampleSize = 1;

    if (layer == 3) // MP3
    {
        inputFormat.subtype = FOURCCMap(0x55);
        inputFormat.formattype = FORMAT_WaveFormatEx;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(MPEGLAYER3WAVEFORMAT)];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(MPEGLAYER3WAVEFORMAT));
        inputFormat.length = sizeof(MPEGLAYER3WAVEFORMAT);

        MPEGLAYER3WAVEFORMAT *wfx = (MPEGLAYER3WAVEFORMAT *)inputFormat.pFormat;
        wfx->wfx.wFormatTag = WAVE_FORMAT_MPEGLAYER3;
        wfx->wfx.cbSize = MPEGLAYER3_WFX_EXTRA_BYTES;
        wfx->wfx.nChannels = channels;
        wfx->wfx.nSamplesPerSec = rate;
        wfx->wfx.nAvgBytesPerSec = 4096;
        wfx->wfx.nBlockAlign = 1;
        wfx->wID = MPEGLAYER3_ID_MPEG;
        wfx->fdwFlags = MPEGLAYER3_FLAG_PADDING_OFF;
        wfx->nBlockSize = 1;
        wfx->nFramesPerBlock = 1;
        wfx->nCodecDelay = 0;
    }
    else if (layer == 1 || layer == 2)
    {
        inputFormat.subtype = MEDIASUBTYPE_MPEG1Payload;
        inputFormat.formattype = FORMAT_WaveFormatEx;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(MPEG1WAVEFORMAT)];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(MPEG1WAVEFORMAT));
        inputFormat.length = sizeof(MPEG1WAVEFORMAT);

        MPEG1WAVEFORMAT *wfx = (MPEG1WAVEFORMAT *)inputFormat.pFormat;
        wfx->wfx.wFormatTag = WAVE_FORMAT_MPEG;
        wfx->wfx.cbSize = 22;
        wfx->wfx.nChannels = channels;
        wfx->wfx.nSamplesPerSec = rate;
        wfx->wfx.nAvgBytesPerSec = 4096;
        wfx->wfx.nBlockAlign = 1;
        if (layer == 1)
            wfx->fwHeadLayer = ACM_MPEG_LAYER1;
        else if (layer == 2)
            wfx->fwHeadLayer = ACM_MPEG_LAYER2;
        wfx->dwHeadBitrate = 0;
    }

    if (!dshowwrapper_create_ds_source(decoder, &inputFormat))
        goto exit;

    // Init output
    sOutputFormat outputFormat;
    ZeroMemory(&outputFormat, sizeof(sOutputFormat));

    if ((decoder->eDecoderSettings & DECODER_SETTING_FORCE_STEREO_OUTPUT) == DECODER_SETTING_FORCE_STEREO_OUTPUT)
    {
        channels = 2;
        outputFormat.bForceStereoOutput = TRUE;
    }

    // Set srcpad caps
    GstCaps *caps = NULL;
    caps = gst_caps_new_simple ("audio/x-raw-int",
        "rate", G_TYPE_INT, rate,
        "channels", G_TYPE_INT, channels,
        "endianness", G_TYPE_INT, G_LITTLE_ENDIAN,
        "width", G_TYPE_INT, 16,
        "depth", G_TYPE_INT, 16,
        "signed", G_TYPE_BOOLEAN, true,
        NULL);

    gst_pad_set_caps(decoder->srcpad[0], caps);
    gst_caps_unref(caps);

    outputFormat.type = MEDIATYPE_Audio;
    outputFormat.subtype = MEDIASUBTYPE_PCM;
    outputFormat.bFixedSizeSamples = TRUE;
    outputFormat.bTemporalCompression = FALSE;
    outputFormat.lSampleSize = 1;
    outputFormat.formattype = FORMAT_WaveFormatEx;
    outputFormat.pFormat = new (nothrow) BYTE[sizeof(WAVEFORMATEX)];
    if (outputFormat.pFormat == NULL)
        goto exit;
    memset(outputFormat.pFormat, 0, sizeof(WAVEFORMATEX));
    outputFormat.length = sizeof(WAVEFORMATEX);

    WAVEFORMATEX *wfx = (WAVEFORMATEX*)outputFormat.pFormat;
    wfx->wFormatTag = WAVE_FORMAT_PCM;
    wfx->nChannels = channels;
    wfx->nSamplesPerSec = rate;
    wfx->nAvgBytesPerSec = channels*rate*(16/8);
    wfx->nBlockAlign = channels*(16/8);
    wfx->wBitsPerSample = 16;
    wfx->cbSize = 0;

    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, 0, true))
        goto exit;

    ret = TRUE;

exit:
    if (inputFormat.pFormat != NULL)
        delete [] inputFormat.pFormat;
    if (outputFormat.pFormat != NULL)
        delete [] outputFormat.pFormat;

    return ret;
}

static void dshowwrapper_load_decoder_h264(GstDShowWrapper *decoder, CODEC_ID codecID)
{
    HRESULT hr = S_OK;
    CLSID decoderCLSID = GUID_NULL;

    if (codecID == CODEC_ID_H264)
    {
        hr = CLSIDFromString(L"{212690FB-83E5-4526-8FD7-74478B7939CD}", &decoderCLSID);
        if (SUCCEEDED(hr))
        {
            hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
            if (SUCCEEDED(hr))
            {
                decoder->eInputFormat = MEDIA_FORMAT_VIDEO_H264;
                decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] = MEDIA_FORMAT_VIDEO_I420;
            }
        }
    }
    else if (codecID == CODEC_ID_AVC1)
    {
        int count = sizeof(szAACDecoders)/sizeof(WCHAR*);
        for (int i = 0; i < count; i++)
        {
            hr = CLSIDFromString(szAVCDecoders[i], &decoderCLSID);
            if (SUCCEEDED(hr))
            {
                hr = CoCreateInstance(decoderCLSID, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
                if (SUCCEEDED(hr))
                {
                    // Check if filter supports required media types
                    if (i == 0) // Only check Microsoft filters. Other may not report proper types.
                    {
                        IPin *pPin = NULL;
                        if (eAVCDecodersInputFormats[i] == MEDIA_FORMAT_VIDEO_AVC1)
                            pPin = dshowwrapper_get_pin(decoder->pDecoder, PINDIR_INPUT, MEDIATYPE_Video, MEDIASUBTYPE_AVC1);
                        else if (eAVCDecodersInputFormats[i] == MEDIA_FORMAT_VIDEO_H264)
                            pPin = dshowwrapper_get_pin(decoder->pDecoder, PINDIR_INPUT, MEDIATYPE_Video, MEDIASUBTYPE_H264);
                        else
                            continue;

                        if (pPin == NULL)
                        {
                            decoder->pDecoder->Release();
                            decoder->pDecoder = NULL;
                            continue;
                        }
                        else
                        {
                            pPin->Release();
                        }
                    }

                    decoder->eInputFormat = eAVCDecodersInputFormats[i];
                    decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] = eAVCDecodersOutputFormats[i];
                    break;
                }
            }
        }
    }

    if (decoder->pDecoder != NULL)
    {
        // Enable video acceleration. If it fails not a problem we will use software only.
        ICodecAPI *pCodecAPI = NULL;
        hr = decoder->pDecoder->QueryInterface(IID_ICodecAPI, (void **) &pCodecAPI);
        if (hr == S_OK)
        {
            hr = pCodecAPI->IsSupported(&CODECAPI_AVDecVideoAcceleration_H264);
            if (hr == S_OK)
            {
                hr = pCodecAPI->IsModifiable(&CODECAPI_AVDecVideoAcceleration_H264);
                if (hr == S_OK)
                {
                    VARIANT value;
                    memset(&value, 0, sizeof(VARIANT));
                    value.vt = VT_UI4;
                    value.ulVal = 1;
                    hr = pCodecAPI->SetValue(&CODECAPI_AVDecVideoAcceleration_H264, &value);
                }
            }

            hr = pCodecAPI->IsSupported(&CODECAPI_AVDecVideoDropPicWithMissingRef);
            if (hr == S_OK)
            {
                hr = pCodecAPI->IsModifiable(&CODECAPI_AVDecVideoDropPicWithMissingRef);
                if (hr == S_OK)
                {
                    VARIANT value;
                    memset(&value, 0, sizeof(VARIANT));
                    value.vt = VT_BOOL;
                    value.ulVal = VARIANT_TRUE;
                    hr = pCodecAPI->SetValue(&CODECAPI_AVDecVideoDropPicWithMissingRef, &value);
                }
            }

            pCodecAPI->Release();
        }
    }
}

static gboolean dshowwrapper_load_decoder_h264(GstStructure *s, GstDShowWrapper *decoder)
{
    gboolean ret = FALSE;

    // Init input
    sInputFormat inputFormat;
    ZeroMemory(&inputFormat, sizeof(sInputFormat));

    gint width = 0;
    gint height = 0;
    const GValue *v = NULL;
    GstBuffer *codec_data = NULL;

    if (gst_structure_get_int(s, "width", &width) && gst_structure_get_int(s, "height", &height))
    {
        // Load AVC1 decoder
        dshowwrapper_load_decoder_h264(decoder, CODEC_ID_AVC1);
        if (decoder->pDecoder == NULL)
            return FALSE;

        v = gst_structure_get_value(s, "codec_data");
        if (v == NULL)
            return FALSE;

        codec_data = gst_value_get_buffer(v);
        if (codec_data == NULL)
            return FALSE;

        // GetAVCConfig
        BYTE header[MAX_HEADER_SIZE];
        gint header_size = 0;
        guint avcProfile = 0;
        guint avcLevel = 0;
        guint lengthSizeMinusOne = 0;
        if (codec_data != NULL && GST_BUFFER_SIZE(codec_data) <= MAX_HEADER_SIZE)
            header_size = dshowwrapper_get_avc_config(GST_BUFFER_DATA(codec_data), GST_BUFFER_SIZE(codec_data), header, 256, &avcProfile, &avcLevel, &lengthSizeMinusOne);
        else
            return FALSE;

        if (header_size <= 0)
            return FALSE;

        inputFormat.type = MEDIATYPE_Video;

        if (decoder->eInputFormat == MEDIA_FORMAT_VIDEO_AVC1)
            inputFormat.subtype = MEDIASUBTYPE_AVC1;
        else if (decoder->eInputFormat == MEDIA_FORMAT_VIDEO_H264)
            inputFormat.subtype = MEDIASUBTYPE_H264;
        else
            return FALSE;

        inputFormat.bFixedSizeSamples = FALSE;
        inputFormat.bTemporalCompression = TRUE;
        inputFormat.lSampleSize = 1;
        inputFormat.formattype = FORMAT_MPEG2Video;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(MPEG2VIDEOINFO) + header_size];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(MPEG2VIDEOINFO) + header_size);
        inputFormat.length = sizeof(MPEG2VIDEOINFO) + header_size;

        MPEG2VIDEOINFO* pbFormat = (MPEG2VIDEOINFO*)inputFormat.pFormat;

        // VIDEOINFOHEADER2
        pbFormat->hdr.rcSource.right = width;
        pbFormat->hdr.rcSource.bottom = height;
        pbFormat->hdr.rcTarget = pbFormat->hdr.rcSource;
        pbFormat->hdr.dwPictAspectRatioX = pbFormat->hdr.rcSource.right;
        pbFormat->hdr.dwPictAspectRatioY = pbFormat->hdr.rcSource.bottom;
        pbFormat->hdr.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        pbFormat->hdr.bmiHeader.biWidth = width;
        pbFormat->hdr.bmiHeader.biHeight = height;
        pbFormat->hdr.bmiHeader.biPlanes = 1;

        // MPEG2VIDEOINFO
        pbFormat->dwFlags = lengthSizeMinusOne + 1;
        memcpy(pbFormat->dwSequenceHeader, header, header_size);
        pbFormat->cbSequenceHeader = header_size;
    }
    else
    {
        // Load H.264 decoder
        dshowwrapper_load_decoder_h264(decoder, CODEC_ID_H264);
        if (decoder->pDecoder == NULL)
            goto exit;

        decoder->enable_pts = TRUE;

        inputFormat.type = MEDIATYPE_Video;
        inputFormat.subtype = MEDIASUBTYPE_H264;
        inputFormat.bFixedSizeSamples = FALSE;
        inputFormat.bTemporalCompression = TRUE;
        inputFormat.lSampleSize = 1;

        inputFormat.formattype = FORMAT_VideoInfo2;
        inputFormat.pFormat = new (nothrow) BYTE[sizeof(VIDEOINFOHEADER2)];
        if (inputFormat.pFormat == NULL)
            goto exit;
        memset(inputFormat.pFormat, 0, sizeof(VIDEOINFOHEADER2));
        inputFormat.length = sizeof(VIDEOINFOHEADER2);

        VIDEOINFOHEADER2 *hdr = (VIDEOINFOHEADER2*)inputFormat.pFormat;
        hdr->rcSource.right = 1920;
        hdr->rcSource.bottom = 1080;
        hdr->rcTarget = hdr->rcSource;
        hdr->dwPictAspectRatioX = hdr->rcSource.right;
        hdr->dwPictAspectRatioY = hdr->rcSource.bottom;
        hdr->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        hdr->bmiHeader.biWidth = hdr->rcSource.right;
        hdr->bmiHeader.biHeight = hdr->rcSource.bottom;
        hdr->bmiHeader.biCompression = '462H';
    }

    if (!dshowwrapper_create_ds_source(decoder, &inputFormat))
        goto exit;

    // Init output
    sOutputFormat outputFormat;
    ZeroMemory(&outputFormat, sizeof(sOutputFormat));

    gint framerate_num = 0;
    gint framerate_den = 0;

    if (!gst_structure_get_int(s, "width", &width))
        width = 1920; // We will change it dynamically

    if (!gst_structure_get_int(s, "height", &height))
        height = 1080; // We will change it dynamically

    if (!gst_structure_get_fraction (s, "framerate", &framerate_num, &framerate_den))
    {
        framerate_num = 2997; // We will change it dynamically
        framerate_den = 100;
    }

    if (decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] == MEDIA_FORMAT_VIDEO_I420)
    {
        // Set srcpad caps
        GstCaps *caps = NULL;
        caps = gst_caps_new_simple ("video/x-raw-yuv",
            "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('Y', 'V', '1', '2'),
            "framerate", GST_TYPE_FRACTION, framerate_num, framerate_den,
            "width", G_TYPE_INT, width,
            "height", G_TYPE_INT, height,
            "offset-y", G_TYPE_INT, 0,
            "offset-v", G_TYPE_INT, (width*height+((width*height)/4)),
            "offset-u", G_TYPE_INT, width*height,
            "stride-y", G_TYPE_INT, width,
            "stride-v", G_TYPE_INT, width/2,
            "stride-u", G_TYPE_INT, width/2,
            NULL);

        gst_pad_set_caps(decoder->srcpad[0], caps);
        gst_caps_unref(caps);

        outputFormat.type = MEDIATYPE_Video;
        outputFormat.subtype = MEDIASUBTYPE_I420;
        outputFormat.bFixedSizeSamples = TRUE;
        outputFormat.bTemporalCompression = FALSE;
        outputFormat.lSampleSize = 1;
        outputFormat.formattype = FORMAT_VideoInfo2;
        outputFormat.pFormat = new (nothrow) BYTE[sizeof(VIDEOINFOHEADER2)];
        if (outputFormat.pFormat == NULL)
            goto exit;
        memset(outputFormat.pFormat, 0, sizeof(VIDEOINFOHEADER2));
        outputFormat.length = sizeof(VIDEOINFOHEADER2);

        VIDEOINFOHEADER2 *hdr = (VIDEOINFOHEADER2*)outputFormat.pFormat;
        hdr->rcSource.right = width;
        hdr->rcSource.bottom = height;
        hdr->rcTarget = hdr->rcSource;
        hdr->dwPictAspectRatioX = width;
        hdr->dwPictAspectRatioY = height;
        hdr->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        hdr->bmiHeader.biWidth = width;
        hdr->bmiHeader.biHeight = height;
        hdr->bmiHeader.biPlanes = 1;
        hdr->bmiHeader.biBitCount = 12;
        hdr->bmiHeader.biCompression = '024I';
    }
    else if (decoder->eOutputFormat[DEFAULT_OUTPUT_DS_STREAM_INDEX] == MEDIA_FORMAT_VIDEO_YV12)
    {
        // Set srcpad caps
        GstCaps *caps = NULL;
        caps = gst_caps_new_simple ("video/x-raw-yuv",
            "format", GST_TYPE_FOURCC, GST_MAKE_FOURCC ('Y', 'V', '1', '2'),
            "framerate", GST_TYPE_FRACTION, framerate_num, framerate_den,
            "width", G_TYPE_INT, width,
            "height", G_TYPE_INT, height,
            NULL);

        gst_pad_set_caps(decoder->srcpad[0], caps);
        gst_caps_unref(caps);

        outputFormat.type = MEDIATYPE_Video;
        outputFormat.subtype = MEDIASUBTYPE_YV12;
        outputFormat.bFixedSizeSamples = TRUE;
        outputFormat.bTemporalCompression = FALSE;
        outputFormat.lSampleSize = 1;
        outputFormat.formattype = FORMAT_VideoInfo2;
        outputFormat.pFormat = new (nothrow) BYTE[sizeof(VIDEOINFOHEADER2)];
        if (outputFormat.pFormat == NULL)
            goto exit;
        memset(outputFormat.pFormat, 0, sizeof(VIDEOINFOHEADER2));
        outputFormat.length = sizeof(VIDEOINFOHEADER2);

        VIDEOINFOHEADER2 *hdr = (VIDEOINFOHEADER2*)outputFormat.pFormat;
        hdr->rcSource.right = width;
        hdr->rcSource.bottom = height;
        hdr->rcTarget = hdr->rcSource;
        hdr->dwPictAspectRatioX = width;
        hdr->dwPictAspectRatioY = height;
        hdr->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        hdr->bmiHeader.biWidth = width;
        hdr->bmiHeader.biHeight = height;
        hdr->bmiHeader.biPlanes = 1;
        hdr->bmiHeader.biBitCount = 12;
        hdr->bmiHeader.biCompression = '21VY';
    }
    else
    {
        goto exit;
    }

    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, 0, true))
        goto exit;

    ret = TRUE;

exit:
    if (inputFormat.pFormat != NULL)
        delete [] inputFormat.pFormat;
    if (outputFormat.pFormat != NULL)
        delete [] outputFormat.pFormat;

    return ret;
}

static void dshowwrapper_mp2t_map_pid(GstDShowWrapper *decoder)
{
    HRESULT hr = S_OK;
    IMpeg2Demultiplexer *pMpeg2Demultiplexer = NULL;
    IPin *pPin = NULL;
    gboolean has_audio = FALSE;
    gboolean has_video = FALSE;
    GstCaps *caps = NULL;
    bool bStartGraph = false;

    {
        CAutoLock lock(decoder->pDSLock);

        if (decoder->pGraph == NULL)
            return;

        if (decoder->srcpad[MP2T_AUDIO_INDEX] == NULL && decoder->srcpad[MP2T_VIDEO_INDEX] == NULL)
        {
            bStartGraph = true;
            decoder->pMediaControl->Stop();
        }

        {
            CAutoLock lock(decoder->pPIDLock);

            decoder->map_pid = FALSE;

            if (decoder->srcpad[MP2T_AUDIO_INDEX] == NULL && decoder->srcpad[MP2T_VIDEO_INDEX] == NULL)
            {
                has_audio = (decoder->Pid[MP2T_AUDIO_INDEX] != 0);
                has_video = (decoder->Pid[MP2T_VIDEO_INDEX] != 0);

                hr = decoder->pDecoder->QueryInterface(IID_IMpeg2Demultiplexer, (void**)&pMpeg2Demultiplexer);
                if (FAILED(hr))
                    goto exit;

                // Audio
                if (has_audio)
                {
                    AM_MEDIA_TYPE mt;
                    sOutputFormat outputFormat;
                    ZeroMemory(&mt, sizeof(AM_MEDIA_TYPE));
                    ZeroMemory(&outputFormat, sizeof(sOutputFormat));

                    mt.majortype = MEDIATYPE_Audio;
                    mt.subtype = MEDIASUBTYPE_MPEG_ADTS_AAC;
                    mt.bFixedSizeSamples = FALSE;
                    mt.bTemporalCompression = TRUE;
                    mt.lSampleSize = 1;
                    mt.formattype = FORMAT_WaveFormatEx;
                    mt.pbFormat = (BYTE*)CoTaskMemAlloc(sizeof(WAVEFORMATEX));
                    if (mt.pbFormat == NULL)
                        goto exit;
                    memset(mt.pbFormat, 0, sizeof(WAVEFORMATEX));
                    mt.cbFormat = sizeof(WAVEFORMATEX);
                    WAVEFORMATEX *wfx = (WAVEFORMATEX*)mt.pbFormat;
                    wfx->wFormatTag = WAVE_FORMAT_MPEG_ADTS_AAC;
                    wfx->nChannels = 2;
                    wfx->nSamplesPerSec = 48000;
                    wfx->nBlockAlign = 1;

                    hr = pMpeg2Demultiplexer->CreateOutputPin(&mt, L"Audio Pin", &pPin);
                    FreeMediaType(mt);
                    if (FAILED(hr))
                        goto exit;
                    hr = pPin->QueryInterface(__uuidof(IMPEG2PIDMap), (void**)&decoder->pMPEG2PIDMap[MP2T_AUDIO_INDEX]);
                    if (FAILED(hr))
                        goto exit;
                    hr = decoder->pMPEG2PIDMap[MP2T_AUDIO_INDEX]->MapPID(1, &decoder->Pid[MP2T_AUDIO_INDEX], MEDIA_TRANSPORT_PAYLOAD);
                    if (FAILED(hr))
                        goto exit;
                    pPin->Release();
                    pPin = NULL;

                    outputFormat.bUseExternalAllocator = TRUE;
                    outputFormat.type = MEDIATYPE_Audio;
                    outputFormat.subtype = MEDIASUBTYPE_MPEG_ADTS_AAC;
                    outputFormat.bFixedSizeSamples = FALSE;
                    outputFormat.bTemporalCompression = TRUE;
                    outputFormat.lSampleSize = 1;
                    outputFormat.formattype = FORMAT_WaveFormatEx;
                    outputFormat.pFormat = new (nothrow) BYTE[sizeof(WAVEFORMATEX)];
                    if (outputFormat.pFormat == NULL)
                        goto exit;
                    memset(outputFormat.pFormat, 0, sizeof(WAVEFORMATEX));
                    outputFormat.length = sizeof(WAVEFORMATEX);
                    wfx = (WAVEFORMATEX*)outputFormat.pFormat;
                    wfx->wFormatTag = WAVE_FORMAT_MPEG_ADTS_AAC;
                    wfx->nChannels = 2;
                    wfx->nSamplesPerSec = 48000;
                    wfx->nBlockAlign = 1;

                    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, MP2T_AUDIO_INDEX, true))
                    {
                        delete [] outputFormat.pFormat;
                        goto exit;
                    }

                    delete [] outputFormat.pFormat;

                    decoder->eOutputFormat[MP2T_AUDIO_INDEX] = MEDIA_FORMAT_AUDIO_AAC;

                    caps = gst_caps_new_simple ("audio/mpeg", "mpegversion", G_TYPE_INT, 4, NULL);
                    if (!dshowwrapper_create_src_pad(decoder, &decoder->srcpad[MP2T_AUDIO_INDEX], caps, NULL, !has_video))
                        goto exit;
                }

                // Video
                if (has_video)
                {
                    AM_MEDIA_TYPE mt;
                    sOutputFormat outputFormat;
                    ZeroMemory(&mt, sizeof(AM_MEDIA_TYPE));
                    ZeroMemory(&outputFormat, sizeof(sOutputFormat));

                    mt.majortype = MEDIATYPE_Video;
                    mt.subtype = MEDIASUBTYPE_H264;

                    hr = pMpeg2Demultiplexer->CreateOutputPin(&mt, L"Video Pin", &pPin);
                    if (FAILED(hr))
                        goto exit;
                    hr = pPin->QueryInterface(__uuidof(IMPEG2PIDMap), (void**)&decoder->pMPEG2PIDMap[MP2T_VIDEO_INDEX]);
                    if (FAILED(hr))
                        goto exit;
                    hr = decoder->pMPEG2PIDMap[MP2T_VIDEO_INDEX]->MapPID(1, &decoder->Pid[MP2T_VIDEO_INDEX], MEDIA_TRANSPORT_PAYLOAD);
                    if (FAILED(hr))
                        goto exit;
                    pPin->Release();
                    pPin = NULL;

                    outputFormat.bUseExternalAllocator = TRUE;
                    outputFormat.type = MEDIATYPE_Video;
                    outputFormat.subtype = MEDIASUBTYPE_H264;

                    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, MP2T_VIDEO_INDEX, true))
                        goto exit;

                    decoder->eOutputFormat[MP2T_VIDEO_INDEX] = MEDIA_FORMAT_VIDEO_H264;

                    caps = gst_caps_new_simple ("video/x-h264", NULL);
                    if (!dshowwrapper_create_src_pad(decoder, &decoder->srcpad[MP2T_VIDEO_INDEX], caps, NULL, TRUE))
                        goto exit;
                }

                if (!dshowwrapper_create_graph_sinks(decoder))
                    goto exit;
            }
            else
            {
                for (int index = 0; index < MAX_OUTPUT_DS_STREAMS; index++)
                {
                    if (decoder->pMPEG2PIDMap[index] && decoder->Pid[index] != 0)
                    {
                        decoder->pMPEG2PIDMap[index]->MapPID(1, &decoder->Pid[index], MEDIA_TRANSPORT_PAYLOAD);
                    }
                }
            }
        }

        if (bStartGraph)
            decoder->pMediaControl->Run();
    }

    if (decoder->first_map_pid)
    {
        decoder->first_map_pid = FALSE;
        decoder->skip_flush = TRUE;
        gst_pad_push_event(decoder->sinkpad, gst_event_new_seek(decoder->rate, GST_FORMAT_TIME, (GstSeekFlags)(GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE), GST_SEEK_TYPE_SET, decoder->seek_position, GST_SEEK_TYPE_NONE, 0));
    }

exit:

    if (pPin != NULL)
        pPin->Release();
    if (pMpeg2Demultiplexer != NULL)
        pMpeg2Demultiplexer->Release();
}

static gboolean dshowwrapper_mp2t_store_pid(GstDShowWrapper *decoder, BYTE streamType, short PID, BYTE *pCodecData, long lSize)
{
    ULONG Pid = PID;
    int index = 0;

    switch (streamType)
    {
    case STREAM_TYPE_H264:
        index = MP2T_VIDEO_INDEX;
        break;

    case STREAM_TYPE_AAC:
        index = MP2T_AUDIO_INDEX;
        break;

    default:
        return FALSE;
    };

    if (decoder->Pid[index] != Pid)
    {
        if (decoder->Pid[index] != 0 && decoder->pMPEG2PIDMap[index])
            decoder->pMPEG2PIDMap[index]->UnmapPID(1, &decoder->Pid[index]);

        decoder->Pid[index] = Pid;
        return TRUE;
    }

    return FALSE;
}

static void dshowwrapper_render_sample_app_mp2t(BYTE *pData, long lSize, sUserData *pUserData)
{
    GstDShowWrapper *decoder = (GstDShowWrapper*)pUserData->pUserData;
    gboolean map_pid = FALSE;

    if (decoder == NULL || !decoder->get_pid || lSize <= PMT_HEADER_SIZE)
        return;

    // PMT Table
    // 0 - ID (0x02)
    // 1 - Check for known bits (0x30)
    // 5 - Check for indicator (table ready) (0x01)
    if (pData[0] == 0x02 && (pData[1]&0x30) == 0x30 && (pData[5]&0x01) == 0x01)
    {
        CAutoLock lock(decoder->pPIDLock);

        short PCR_PID = (((pData[8] << 8) | pData[9]) & 0x1FFF);
        short programInfoLength = (((pData[10] << 8) | pData[11]) & 0x03FF);
        int infoOffset = PMT_HEADER_SIZE + programInfoLength;

        while ((lSize - infoOffset - CRC32_SIZE) >= PMT_INFO_SIZE)
        {
            BYTE *pInfo = (pData + infoOffset);
            BYTE streamType = pInfo[0];
            short PID = (((pInfo[1] << 8) | pInfo[2]) & 0x1FFF);
            short ESInfoLength = (((pInfo[3] << 8) | pInfo[4]) & 0x03FF);

            if (ESInfoLength > 0)
                map_pid |= dshowwrapper_mp2t_store_pid(decoder, streamType, PID, (pInfo + PMT_INFO_SIZE), ESInfoLength);
            else
                map_pid |= dshowwrapper_mp2t_store_pid(decoder, streamType, PID, NULL, 0);

            infoOffset += (PMT_INFO_SIZE + ESInfoLength);
        }

        if (map_pid)
            decoder->map_pid = map_pid;
    }
}

static gboolean dshowwrapper_load_decoder_mp2t(GstStructure *s, GstDShowWrapper *decoder)
{
    gboolean result = FALSE;
    IPin *pPin = NULL;
    IMpeg2Demultiplexer *pMpeg2Demultiplexer = NULL;
    IMPEG2PIDMap *pMPEG2PIDMap = NULL;

    // Load decoder
    HRESULT hr = CoCreateInstance(CLSID_MPEG2Demultiplexer, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter, (void**)&decoder->pDecoder);
    if (FAILED(hr))
        goto exit;

    // Check if we support H.264
    if (!dshowwrapper_is_decoder_by_codec_id_supported(CODEC_ID_H264))
    {
        if (decoder->pDecoder != NULL)
        {
            decoder->pDecoder->Release();
            decoder->pDecoder = NULL;
        }
        gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_CODEC_NOT_FOUND, g_strdup("H.264 decoder not found."), NULL, ("dshowwrapper.c"), ("dshowwrapper_load_decoder_mp2t"), 0);
        goto exit;
    }

    // Remove current pad
    if (decoder->srcpad[0])
    {
        gst_element_remove_pad(GST_ELEMENT(decoder), decoder->srcpad[0]);
        decoder->srcpad[0] = NULL;
    }

    // Init input
    sInputFormat inputFormat;
    ZeroMemory(&inputFormat, sizeof(sInputFormat));

    inputFormat.type = MEDIATYPE_Stream;
    inputFormat.subtype = MEDIASUBTYPE_MPEG2_TRANSPORT;
    inputFormat.bFixedSizeSamples = TRUE;
    inputFormat.bTemporalCompression = TRUE;
    inputFormat.lSampleSize = 1;
    inputFormat.formattype = FORMAT_None;
    inputFormat.pFormat = NULL;
    inputFormat.length = 0;

    if (!dshowwrapper_create_ds_source(decoder, &inputFormat))
        goto exit;

    decoder->eInputFormat = MEDIA_FORMAT_STREAM_MP2T;

    hr = decoder->pDecoder->QueryInterface(IID_IMpeg2Demultiplexer, (void**)&pMpeg2Demultiplexer);
    if (FAILED(hr))
        goto exit;

    // Data pin
    AM_MEDIA_TYPE mt;
    sOutputFormat outputFormat;
    ZeroMemory(&outputFormat, sizeof(sOutputFormat));
    ZeroMemory(&mt, sizeof(AM_MEDIA_TYPE));

    mt.majortype = MEDIATYPE_MPEG2_SECTIONS;
    mt.subtype = MEDIASUBTYPE_MPEG2DATA;

    hr = pMpeg2Demultiplexer->CreateOutputPin(&mt, L"Data Pin", &pPin);
    if (FAILED(hr))
        goto exit;
    hr = pPin->QueryInterface(__uuidof(IMPEG2PIDMap), (void**)&pMPEG2PIDMap);
    if (FAILED(hr))
        goto exit;
    ULONG Pid = 0x0;
    hr = pMPEG2PIDMap->MapPID(1, &Pid, MEDIA_MPEG2_PSI);
    if (FAILED(hr))
        goto exit;
    pMPEG2PIDMap->Release();
    pMPEG2PIDMap = NULL;
    pPin->Release();
    pPin = NULL;

    outputFormat.bUseExternalAllocator = TRUE;
    outputFormat.type = MEDIATYPE_MPEG2_SECTIONS;
    outputFormat.subtype = MEDIASUBTYPE_MPEG2DATA;

    if (!dshowwrapper_create_ds_sink(decoder, &outputFormat, MP2T_DATA_INDEX, false))
        goto exit;

    decoder->pSink[MP2T_DATA_INDEX]->SetRenderSampleAppCallback(&dshowwrapper_render_sample_app_mp2t);

    decoder->get_pid = TRUE;
    decoder->enable_pts = TRUE;

#if ENABLE_CLOCK
    // Create clock
    decoder->clock = gst_audio_clock_new("DShowWrapperMP2TClock", (GstAudioClockGetTimeFunc)dshowwrapper_clock_get_time, decoder);
    if (decoder->clock == NULL)
        goto exit;

    hr = decoder->pDecoder->QueryInterface(IID_IReferenceClock, (void**)&decoder->pDSClock);
    if (FAILED(hr))
        goto exit;
#endif // ENABLE_CLOCK

    decoder->pPTSLock = new CCritSec();
    if (decoder->pPTSLock == NULL)
        goto exit;

    decoder->pPIDLock = new CCritSec();
    if (decoder->pPIDLock == NULL)
        goto exit;

    result = TRUE;

exit:
    if (pMPEG2PIDMap != NULL)
        pMPEG2PIDMap->Release();
    if (pPin != NULL)
        pPin->Release();
    if (pMpeg2Demultiplexer != NULL)
        pMpeg2Demultiplexer->Release();

    return result;
}

static gboolean dshowwrapper_load_decoder(GstCaps *caps, GstDShowWrapper *decoder)
{
    GstStructure *s = NULL;
    const gchar *mimetype = NULL;
    HRESULT hr = S_OK;

    if (caps == NULL || decoder == NULL)
        return FALSE;

    s = gst_caps_get_structure (caps, 0);
    if (s == NULL)
        return FALSE;

    mimetype = gst_structure_get_name (s);
    if (mimetype == NULL)
        return FALSE;

    if (strstr(mimetype, "audio/mpeg") != NULL) // AAC or MP3
    {
        gint mpegversion = 0;

        if (!gst_structure_get_int(s, "mpegversion", &mpegversion))
            return FALSE;

        if (mpegversion == 4) // AAC
            return dshowwrapper_load_decoder_aac(s, decoder);
        else if (mpegversion == 1) // MP3
            return dshowwrapper_load_decoder_mp3(s, decoder);
        else
            return FALSE;
    }
    else if (strstr(mimetype, "video/x-h264") != NULL)
        return dshowwrapper_load_decoder_h264(s, decoder);
    else if (strstr(mimetype, "video/MP2T") != NULL)
        return dshowwrapper_load_decoder_mp2t(s, decoder);
    else
        return FALSE;

    return FALSE;
}

static gboolean dshowwrapper_init_dshow(GstDShowWrapper *decoder, GstCaps *caps)
{
    gboolean ret = TRUE;

    bool bCallCoUninitialize = true;

    if (FAILED(CoInitialize(NULL)))
        bCallCoUninitialize = false;

    if (!dshowwrapper_load_decoder(caps, decoder))
    {
        ret = FALSE;
        goto done;
    }

    if (decoder->acm_wrapper && hMutex)
        WaitForSingleObject(hMutex, INFINITE);

    ret = dshowwrapper_create_graph(decoder);

    if (decoder->acm_wrapper && hMutex)
        ReleaseMutex(hMutex);

    if (!ret)
        goto done;

done:
    if (bCallCoUninitialize)
        CoUninitialize();

    return ret;
}

static gboolean dshowwrapper_create_src_pad(GstDShowWrapper *decoder, GstPad **ppPad, GstCaps *caps, gchar *name, gboolean check_no_more_pads)
{
    gboolean active = FALSE;

    if (ppPad == NULL)
    {
        if (caps)
            gst_caps_unref(caps);
        return FALSE;
    }

    // Remove old src pad if needed
    if (*ppPad != NULL)
    {
        active = gst_pad_is_active(*ppPad);
        if (active)
        {
            gst_pad_set_active(*ppPad, FALSE);
        }
        if (caps == NULL)
            caps = gst_caps_copy(GST_PAD_CAPS(*ppPad));
        gst_element_remove_pad(GST_ELEMENT(decoder), *ppPad);

        *ppPad = NULL;
    }

    *ppPad = gst_pad_new_from_static_template (&src_factory, name);
    gst_pad_set_query_type_function(*ppPad, dshowwrapper_get_src_query_types);
    gst_pad_set_query_function(*ppPad, dshowwrapper_src_query);
    gst_pad_set_event_function(*ppPad, dshowwrapper_src_event);
    gst_pad_use_fixed_caps (*ppPad);

    // Set caps
    if (caps)
    {
        gst_pad_set_caps(*ppPad, caps);
        gst_caps_unref(caps);
    }

    if (active || GST_STATE(decoder) > GST_STATE_READY)
    {
        if (!gst_pad_set_active(*ppPad, TRUE))
            return FALSE;
    }

    if (!gst_element_add_pad(GST_ELEMENT(decoder), *ppPad))
        return FALSE;

    if (decoder->pending_event != NULL && gst_pad_is_linked(*ppPad))
    {
        // INLINE - gst_event_ref()
        gst_pad_push_event(*ppPad, gst_event_ref(decoder->pending_event));
    }

    if (check_no_more_pads)
    {
        bool no_more_pads = true;
        for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
        {
            if (decoder->srcpad[i])
            {
                if (!gst_pad_is_linked(decoder->srcpad[i]))
                {
                    no_more_pads = false;
                    break;
                }
            }
        }

        if (no_more_pads)
            gst_element_no_more_pads(GST_ELEMENT(decoder));
    }

    return TRUE;
}

// Perform processing needed for state transitions.
static GstStateChangeReturn dshowwrapper_change_state (GstElement* element, GstStateChange transition)
{
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(element);
    GstStateChangeReturn ret;

    // Change state.
    ret = parent_class->change_state(element, transition);
    if (GST_STATE_CHANGE_FAILURE == ret)
    {
        return ret;
    }

    switch(transition)
    {
    case GST_STATE_CHANGE_READY_TO_NULL:        
        dshowwrapper_destroy_graph(decoder);
        break;
    default:
        break;
    }

    return ret;
}

#if ENABLE_CLOCK
static GstClock* dshowwrapper_provide_clock(GstElement *element)
{
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(element);

    if (decoder->clock)
        return GST_CLOCK_CAST(gst_object_ref(decoder->clock));
    else
        return NULL;
}

static GstClockTime dshowwrapper_clock_get_time(GstClock *clock, gpointer user_data)
{
    GstDShowWrapper *decoder = (GstDShowWrapper*)user_data;
    if (decoder == NULL)
        return GST_CLOCK_TIME_NONE;

    if (decoder->pDSClock)
    {
        HRESULT hr = S_OK;
        REFERENCE_TIME time = 0;
        hr = decoder->pDSClock->GetTime(&time);
        if (FAILED(hr))
            return GST_CLOCK_TIME_NONE;
        else
        {
            return time * 100;
        }
    }

    return GST_CLOCK_TIME_NONE;
}
#endif // ENABLE_CLOCK

// Processes input buffers
static GstFlowReturn dshowwrapper_chain (GstPad * pad, GstBuffer * buf)
{
    GstFlowReturn ret = GST_FLOW_OK;
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER (GST_OBJECT_PARENT (pad));

    if (decoder->is_flushing || decoder->is_eos_received)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
        return GST_FLOW_WRONG_STATE;
    }

    if (decoder->map_pid)
    {
        // Map pid will reset streaming, so no need for this buffer
        dshowwrapper_mp2t_map_pid(decoder);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref (buf);
        return GST_FLOW_OK;
    }

    if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
    {
        decoder->enable_pts = TRUE;
    }

    if (decoder->enable_mp3)
    {
        if (decoder->mp3_id3_size < 0)
        {
            // Get offset only from first buffer.
            // mpegaudioparse will remove ID3 data and set start offset to actual data.
            // We need this offset to calculate duration without considering metadata size.
            decoder->mp3_id3_size = 0;
            if (GST_BUFFER_OFFSET_IS_VALID(buf))
            {
                decoder->mp3_id3_size = GST_BUFFER_OFFSET(buf);
            }
        }
    }

    // MP2T has too many small buffers and we can get false error here. We will detect stream issues at the EOS anyway.
    if (!decoder->is_data_produced && decoder->eInputFormat != MEDIA_FORMAT_STREAM_MP2T)
    {
        decoder->input_buffers_count++;
        if (decoder->input_buffers_count > INPUT_BUFFERS_BEFORE_ERROR)
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to decode stream"), NULL, ("dshowwrapper.c"), ("dshowwrapper_chain"), 0);
            decoder->is_data_produced = TRUE; // Do not send more errors
        }
    }

    if (decoder->force_discontinuity)
    {
        buf = gst_buffer_make_metadata_writable(buf);
        GST_BUFFER_FLAG_SET(buf, GST_BUFFER_FLAG_DISCONT);
        decoder->force_discontinuity = FALSE;
    }

    if (decoder->is_flushing)
        ret = GST_FLOW_WRONG_STATE;
    else if (FAILED(decoder->pSrc->DeliverSample(buf)))
        ret = GST_FLOW_ERROR;

    return ret;
}

static const GstQueryType* dshowwrapper_get_sink_query_types (GstPad* pad)
{
    static const GstQueryType dshowwrapper_sink_query_types[] = {
        GST_QUERY_CONVERT,
        GST_QUERY_NONE
    };

    return dshowwrapper_sink_query_types;
}

static gboolean dshowwrapper_sink_query (GstPad* pad, GstQuery* query)
{
    gboolean result = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    GstDShowWrapper *decode = GST_DSHOWWRAPPER (parent);

    if (result == FALSE)
    {
        result = gst_pad_query_default(pad, query);
    }

    // Unref the parent object.
    gst_object_unref(parent);

    return result;
}

static gboolean dshowwrapper_push_sink_event(GstDShowWrapper *decoder, GstEvent *event)
{
    gboolean ret = TRUE;

    for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
    {
        if (decoder->srcpad[i] != NULL && gst_pad_is_linked(decoder->srcpad[i]))
            ret |= gst_pad_push_event(decoder->srcpad[i], gst_event_ref(event));  // INLINE - gst_event_ref()
    }

    // INLINE - gst_event_unref()
    gst_event_unref(event);

    return ret;
}

static gboolean dshowwrapper_sink_event(GstPad* pad, GstEvent *event)
{
    gboolean ret = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER (parent);
    GstEvent *newsegment = NULL;

    switch (GST_EVENT_TYPE (event))
    {
    case GST_EVENT_NEWSEGMENT:
        gboolean update;
        GstFormat format;
        gdouble rate, arate;
        gint64 start, stop, time;
        if (decoder->enable_position)
        {
            gst_event_parse_new_segment_full(event, &update, &rate, &arate, &format, &start, &stop, &time);
            if (format == GST_FORMAT_TIME)
            {
                decoder->last_stop = start;
            }
        }
        if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T) // Resend new segment event with GST_FORMAT_TIME
        {
            gst_event_parse_new_segment_full(event, &update, &rate, &arate, &format, &start, &stop, &time);
            // INLINE - gst_event_unref()
            gst_event_unref (event);
            event = gst_event_new_new_segment(update, rate, GST_FORMAT_TIME, 0, stop, time);
            if (decoder->pending_event)
            {
                // INLINE - gst_event_unref()
                gst_event_unref (decoder->pending_event);
            }
            // INLINE - gst_event_ref()
            decoder->pending_event = gst_event_ref(event);
        }

        decoder->force_discontinuity = TRUE;
        ret = dshowwrapper_push_sink_event(decoder, event);
        decoder->is_eos_received = FALSE;
        for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
        {
            decoder->is_eos[i] = FALSE;
        }
        break;
    case GST_EVENT_FLUSH_START:
        {
            CAutoLock lock(decoder->pDSLock);
            HRESULT hr = S_OK;

            if (decoder->skip_flush)
            {
                // INLINE - gst_event_unref()
                gst_event_unref (event);
                break;
            }

            decoder->is_flushing = TRUE;

            ret = dshowwrapper_push_sink_event(decoder, event);

            if (decoder->pMediaControl)
                decoder->pMediaControl->Stop();
        }
        break;
    case GST_EVENT_FLUSH_STOP:
        {
            CAutoLock lock(decoder->pDSLock);
            HRESULT hr = S_OK;

            if (decoder->skip_flush)
            {
                decoder->skip_flush = FALSE;
                // INLINE - gst_event_unref()
                gst_event_unref (event);
                break;
            }

            ret = dshowwrapper_push_sink_event(decoder, event);

            for (int i = 0; i < MAX_OUTPUT_DS_STREAMS; i++)
            {
                decoder->offset[i] = 0;
                decoder->last_pts[i] = GST_CLOCK_TIME_NONE;
                if (decoder->out_buffer[i] != NULL)
                {
                    // INLINE - gst_buffer_unref()
                    gst_buffer_unref(decoder->out_buffer[i]);
                    decoder->out_buffer[i] = NULL;
                }
            }

            if (decoder->pMediaControl)
                decoder->pMediaControl->Run();

            decoder->is_flushing = FALSE;
        }
        break;
    case GST_EVENT_EOS:
        {
            decoder->is_eos_received = TRUE;

            if (decoder->pSrc && decoder->pSrc->m_pPin)
                decoder->pSrc->m_pPin->DeliverEndOfStream();

            // INLINE - gst_event_unref()
            gst_event_unref (event);
            ret = TRUE;
        }
        break;
    default:
        ret = dshowwrapper_push_sink_event(decoder, event);
        break;
    }

    // Unlock the parent object.
    gst_object_unref(parent);

    return ret;
}

static gboolean dshowwrapper_sink_set_caps(GstPad * pad, GstCaps * caps)
{
    gboolean ret = FALSE;
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER (GST_OBJECT_PARENT (pad));

    if (pad == decoder->sinkpad)
    {
        ret = dshowwrapper_init_dshow(decoder, caps);
    }

    return ret;
}

static gboolean dshowwrapper_activate(GstPad *pad)
{
    return gst_pad_activate_push(pad, TRUE);
}

static const GstQueryType* dshowwrapper_get_src_query_types (GstPad * pad)
{
    static const GstQueryType dshowwrapper_src_query_types[] = {
        GST_QUERY_POSITION,
        GST_QUERY_DURATION,
        GST_QUERY_NONE
    };

    return dshowwrapper_src_query_types;
}

static gboolean dshowwrapper_src_query (GstPad * pad, GstQuery * query)
{
    gboolean result = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER(parent);
    GstFormat format = GST_FORMAT_UNDEFINED;

    switch (GST_QUERY_TYPE (query))
    {
    case GST_QUERY_DURATION:
        result = gst_pad_query_default(pad, query);
        if (result == FALSE)
        {
            result = TRUE; // No need to ask again
            if (decoder->enable_mp3 && decoder->mp3_duration == -1 && decoder->mp3_id3_size >= 0)
            {
                gint64 data_length = 0;
                GstFormat format = GST_FORMAT_BYTES;
                if (gst_pad_query_peer_duration(decoder->sinkpad, &format, &data_length))
                {
                    data_length -= decoder->mp3_id3_size;

                    format = GST_FORMAT_TIME;
                    if (gst_pad_query_peer_convert(decoder->sinkpad, GST_FORMAT_BYTES, data_length, &format, &decoder->mp3_duration))
                    {
                        gst_query_set_duration(query, GST_FORMAT_TIME, decoder->mp3_duration);
                    }
                }
            }
            else if (decoder->enable_mp3 && decoder->mp3_duration != -1)
            {
                gst_query_set_duration(query, GST_FORMAT_TIME, decoder->mp3_duration);
            }
        }
        break;
    case GST_QUERY_POSITION:
        if (decoder->enable_position && GST_CLOCK_TIME_IS_VALID(decoder->last_stop))
        {
            GstFormat format = GST_FORMAT_UNDEFINED;
            gst_query_parse_position(query, &format, NULL);
            if (format != GST_FORMAT_TIME) {
                break;
            }
            result = TRUE; // No need to ask again
            gst_query_set_position(query, GST_FORMAT_TIME, decoder->last_stop);
            break;
        }
    default:
        break;
    }

    // Use default query if flag indicates query not handled
    if (result == FALSE)
        result = gst_pad_query_default(pad, query);

    // Unref the parent object
    gst_object_unref(parent);

    return result;
}

static gboolean dshowwrapper_src_event (GstPad* pad, GstEvent* event)
{
    gboolean result = FALSE;
    GstObject *parent = gst_object_get_parent((GstObject*)pad);
    GstDShowWrapper *decoder = GST_DSHOWWRAPPER (parent);

    if (decoder->enable_mp3 || decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T)
    {
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
                if (decoder->enable_mp3)
                {
                    gint64 start_byte = 0;
                    GstFormat format = GST_FORMAT_BYTES;
                    if (gst_pad_query_peer_convert(decoder->sinkpad, GST_FORMAT_TIME, start, &format, &start_byte))
                    {
                        result = gst_pad_push_event(decoder->sinkpad,
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
                else if (decoder->eInputFormat == MEDIA_FORMAT_STREAM_MP2T)
                {
                    int index = 0;
                    decoder->seek_position = start;
                    decoder->rate = rate;
                    decoder->base_pts = GST_CLOCK_TIME_NONE;
                    for (index = 0; index < MAX_OUTPUT_DS_STREAMS; index++)
                    {
                        decoder->offset_pts[index] = 0;
                        decoder->last_pts[index] = 0;
                    }
                }
            }
        }
    }

    // Push the event upstream only if it was not processed.
    if (!result)
        result = gst_pad_push_event(decoder->sinkpad, event);

    // Unlock the parent object.
    gst_object_unref(parent);

    return result;
}

gboolean dshowwrapper_init(GstPlugin* dshowwrapper)
{
    return gst_element_register(dshowwrapper, "dshowwrapper", 512, GST_TYPE_DSHOWWRAPPER);
}
