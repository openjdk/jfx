/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>

#include <mfwrapper.h>
#include <mfidl.h>
#include <Wmcodecdsp.h>

#include "fxplugins_common.h"

#define PTS_DEBUG 0

enum
{
    PROP_0,
    PROP_CODEC_ID,
    PROP_IS_SUPPORTED,
};

enum
{
    PO_DELIVERED,
    PO_NEED_MORE_DATA,
    PO_FLUSHING,
    PO_FAILED,
};

GST_DEBUG_CATEGORY_STATIC(gst_mfwrapper_debug);
#define GST_CAT_DEFAULT gst_mfwrapper_debug

// The input capabilities
static GstStaticPadTemplate sink_factory =
GST_STATIC_PAD_TEMPLATE("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS(
        // H.265
        "video/x-h265"
    ));

// The output capabilities
static GstStaticPadTemplate src_factory =
GST_STATIC_PAD_TEMPLATE("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS(
        // YV12
        "video/x-raw-yuv, "
        "format=(string)YV12"
    ));

// Forward declarations
static void gst_mfwrapper_dispose(GObject* object);
static void gst_mfwrapper_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec);
static void gst_mfwrapper_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec);

static GstFlowReturn mfwrapper_chain(GstPad* pad, GstObject *parent, GstBuffer* buf);

static gboolean mfwrapper_sink_event(GstPad* pad, GstObject *parent, GstEvent* event);
static gboolean mfwrapper_sink_set_caps(GstPad * pad, GstObject *parent, GstCaps * caps);
static gboolean mfwrapper_activate(GstPad* pad, GstObject *parent);
static gboolean mfwrapper_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active);

static HRESULT mfwrapper_load_decoder(GstMFWrapper *decoder, GstCaps *caps);

static gboolean mfwrapper_is_decoder_by_codec_id_supported(GstMFWrapper *decoder, gint codec_id);

template <class T> void SafeRelease(T **ppT)
{
    if (*ppT)
    {
        (*ppT)->Release();
        *ppT = NULL;
    }
}

/***********************************************************************************
* Substitution for
* G_DEFINE_TYPE (GstMFWrapper, gst_mfwrapper, GstElement, GST_TYPE_ELEMENT);
***********************************************************************************/
#define gst_mfwrapper_parent_class parent_class
static void gst_mfwrapper_init(GstMFWrapper      *self);
static void gst_mfwrapper_class_init(GstMFWrapperClass *klass);
static gpointer gst_mfwrapper_parent_class = NULL;
static void     gst_mfwrapper_class_intern_init(gpointer klass)
{
    gst_mfwrapper_parent_class = g_type_class_peek_parent(klass);
    gst_mfwrapper_class_init((GstMFWrapperClass*)klass);
}

GType gst_mfwrapper_get_type(void)
{
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter(&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple(GST_TYPE_ELEMENT,
            g_intern_static_string("GstMFWrapper"),
            sizeof(GstMFWrapperClass),
            (GClassInitFunc)gst_mfwrapper_class_intern_init,
            sizeof(GstMFWrapper),
            (GInstanceInitFunc)gst_mfwrapper_init,
            (GTypeFlags)0);
        g_once_init_leave(&gonce_data, (gsize)_type);
    }
    return (GType)gonce_data;
}

// Initialize mfwrapper's class.
static void gst_mfwrapper_class_init(GstMFWrapperClass *klass)
{
    GstElementClass *element_class = (GstElementClass*)klass;
    GObjectClass *gobject_class = (GObjectClass*)klass;

    gst_element_class_set_metadata(element_class,
        "MFWrapper",
        "Codec/Decoder/Audio/Video",
        "Media Foundation Wrapper",
        "Oracle Corporation");

    gst_element_class_add_pad_template(element_class,
        gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
        gst_static_pad_template_get(&sink_factory));

    gobject_class->dispose = gst_mfwrapper_dispose;
    gobject_class->set_property = gst_mfwrapper_set_property;
    gobject_class->get_property = gst_mfwrapper_get_property;

    g_object_class_install_property(gobject_class, PROP_CODEC_ID,
        g_param_spec_int("codec-id", "Codec ID", "Codec ID", -1, G_MAXINT, 0,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));

    g_object_class_install_property(gobject_class, PROP_IS_SUPPORTED,
        g_param_spec_boolean("is-supported", "Is supported", "Is codec ID supported", FALSE,
        (GParamFlags)(G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS)));
}

// Initialize the new element
// Instantiate pads and add them to element
// Set pad calback functions
// Initialize instance structure
static void gst_mfwrapper_init(GstMFWrapper *decoder)
{
    // Input
    decoder->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_element_add_pad(GST_ELEMENT(decoder), decoder->sinkpad);
    gst_pad_set_chain_function(decoder->sinkpad, mfwrapper_chain);
    gst_pad_set_event_function(decoder->sinkpad, mfwrapper_sink_event);
    gst_pad_set_activate_function(decoder->sinkpad, mfwrapper_activate);
    gst_pad_set_activatemode_function(decoder->sinkpad, mfwrapper_activatemode);

    // Output
    decoder->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_element_add_pad(GST_ELEMENT(decoder), decoder->srcpad);

    decoder->is_flushing = FALSE;
    decoder->is_eos_received = FALSE;
    decoder->is_eos = FALSE;
    decoder->is_decoder_initialized = FALSE;
    decoder->force_discontinuity = FALSE;
    decoder->force_output_discontinuity = FALSE;

    // Initialize Media Foundation
    bool bCallCoUninitialize = true;

    if (FAILED(CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE)))
        bCallCoUninitialize = false;

    decoder->hr_mfstartup = MFStartup(MF_VERSION, MFSTARTUP_LITE);

    if (bCallCoUninitialize)
        CoUninitialize();

    decoder->pDecoder = NULL;
    decoder->pDecoderOutput = NULL;

    decoder->pColorConvert = NULL;
    decoder->pColorConvertOutput = NULL;

    decoder->header = NULL;
    decoder->header_size = 0;

    decoder->width = 1920;
    decoder->height = 1080;
    decoder->framerate_num = 2997;
    decoder->framerate_den = 100;
}

static void gst_mfwrapper_dispose(GObject* object)
{
    GstMFWrapper *decoder = GST_MFWRAPPER(object);

    SafeRelease(&decoder->pDecoderOutput);
    SafeRelease(&decoder->pDecoder);

    SafeRelease(&decoder->pColorConvertOutput);
    SafeRelease(&decoder->pColorConvert);

    if (decoder->hr_mfstartup == S_OK)
        MFShutdown();

    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_mfwrapper_set_property(GObject *object, guint property_id, const GValue *value, GParamSpec *pspec)
{
    GstMFWrapper *decoder = GST_MFWRAPPER(object);
    switch (property_id)
    {
    case PROP_CODEC_ID:
        decoder->codec_id = g_value_get_int(value);
        break;
    default:
        break;
    }
}

static void gst_mfwrapper_get_property(GObject *object, guint property_id, GValue *value, GParamSpec *pspec)
{
    GstMFWrapper *decoder = GST_MFWRAPPER(object);
    gboolean is_supported = FALSE;
    switch (property_id)
    {
    case PROP_IS_SUPPORTED:
        is_supported = mfwrapper_is_decoder_by_codec_id_supported(decoder, decoder->codec_id);
        g_value_set_boolean(value, is_supported);
        break;
    default:
        break;
    }
}

static gboolean mfwrapper_is_decoder_by_codec_id_supported(GstMFWrapper *decoder, gint codec_id)
{
    HRESULT hr = S_FALSE;

    switch (codec_id)
    {
    case JFX_CODEC_ID_H265:
        // Dummy caps to load H.265 decoder
        GstCaps *caps = gst_caps_new_simple("video/x-h265",
            "width", G_TYPE_INT, 1920,
            "height", G_TYPE_INT, 1080,
            NULL);
        hr = mfwrapper_load_decoder(decoder, caps);
        gst_caps_unref(caps);
        break;
    }

    if (hr == S_OK)
        return TRUE;
    else
        return FALSE;
}

static void mfwrapper_set_src_caps(GstMFWrapper *decoder)
{
    GstCaps *srcCaps = NULL;
    HRESULT hr = S_OK;
    MFT_OUTPUT_STREAM_INFO outputStreamInfo;

    GstCaps *padCaps = gst_pad_get_current_caps(decoder->srcpad);
    if (padCaps == NULL)
    {
        srcCaps = gst_caps_new_simple("video/x-raw-yuv",
            "format", G_TYPE_STRING, "YV12",
            "framerate", GST_TYPE_FRACTION, decoder->framerate_num, decoder->framerate_den,
            "width", G_TYPE_INT, decoder->width,
            "height", G_TYPE_INT, decoder->height,
            "offset-y", G_TYPE_INT, 0,
            "offset-v", G_TYPE_INT, (decoder->width * decoder->height + ((decoder->width * decoder->height) / 4)),
            "offset-u", G_TYPE_INT, decoder->width * decoder->height,
            "stride-y", G_TYPE_INT, decoder->width,
            "stride-v", G_TYPE_INT, decoder->width / 2,
            "stride-u", G_TYPE_INT, decoder->width / 2,
            NULL);
    }
    else
    {
        srcCaps = gst_caps_copy(padCaps);
        gst_caps_unref(padCaps);
        if (srcCaps == NULL)
            return;

        gst_caps_set_simple(srcCaps,
            "width", G_TYPE_INT, decoder->width,
            "height", G_TYPE_INT, decoder->height,
            "offset-y", G_TYPE_INT, 0,
            "offset-v", G_TYPE_INT, (decoder->width * decoder->height + ((decoder->width * decoder->height) / 4)),
            "offset-u", G_TYPE_INT, decoder->width * decoder->height,
            "stride-y", G_TYPE_INT, decoder->width,
            "stride-v", G_TYPE_INT, decoder->width / 2,
            "stride-u", G_TYPE_INT, decoder->width / 2,
            NULL);
    }

    GstEvent *caps_event = gst_event_new_caps(srcCaps);
    if (caps_event)
    {
        gst_pad_push_event(decoder->srcpad, caps_event);
        decoder->force_output_discontinuity = TRUE;
    }
    gst_caps_unref(srcCaps);

    // Allocate or update decoder output buffer
    SafeRelease(&decoder->pDecoderOutput);

    if (SUCCEEDED(hr))
        hr = decoder->pDecoder->GetOutputStreamInfo(0, &outputStreamInfo);

    if (SUCCEEDED(hr))
    {
        if (!((outputStreamInfo.dwFlags & MFT_OUTPUT_STREAM_PROVIDES_SAMPLES) || (outputStreamInfo.dwFlags & MFT_OUTPUT_STREAM_CAN_PROVIDE_SAMPLES)))
        {
            hr = MFCreateSample(&decoder->pDecoderOutput);
            if (SUCCEEDED(hr))
            {
                IMFMediaBuffer *pBuffer = NULL;
                hr = MFCreateMemoryBuffer(outputStreamInfo.cbSize, &pBuffer);
                if (SUCCEEDED(hr))
                    hr = decoder->pDecoderOutput->AddBuffer(pBuffer);
                SafeRelease(&pBuffer);
            }
        }
    }
}

static void mfwrapper_nalu_to_start_code(BYTE *pbBuffer, gsize size)
{
    gint leftSize = size;

    if (pbBuffer == NULL || size < 4)
        return;

    do
    {
        guint naluLen = ((guint)*(guint8*)pbBuffer) << 24;
        naluLen |= ((guint)*(guint8*)(pbBuffer + 1)) << 16;
        naluLen |= ((guint)*(guint8*)(pbBuffer + 2)) << 8;
        naluLen |= ((guint)*(guint8*)(pbBuffer + 3));

        if (naluLen <= 1) // Start code or something wrong
            return;

        pbBuffer[0] = 0x00;
        pbBuffer[1] = 0x00;
        pbBuffer[2] = 0x00;
        pbBuffer[3] = 0x01;

        leftSize -= (naluLen + 4);
        pbBuffer += (naluLen + 4);

    } while (leftSize > 0);
}

static gboolean mfwrapper_process_input(GstMFWrapper *decoder, GstBuffer *buf)
{
    IMFSample *pSample = NULL;
    IMFMediaBuffer *pBuffer = NULL;
    DWORD dwBufferSize = 0;
    BYTE *pbBuffer = NULL;
    GstMapInfo info;
    gboolean unmap_buf = FALSE;
    gboolean unlock_buf = FALSE;

    if (!decoder->pDecoder)
        return FALSE;

    HRESULT hr = MFCreateSample(&pSample);

    if (SUCCEEDED(hr) && decoder->force_discontinuity)
    {
        hr = pSample->SetUINT32(MFSampleExtension_Discontinuity, TRUE);
        decoder->force_discontinuity = FALSE;
    }

    if (SUCCEEDED(hr) && GST_BUFFER_PTS_IS_VALID(buf))
        hr = pSample->SetSampleTime(GST_BUFFER_PTS(buf) / 100);

    if (SUCCEEDED(hr) && GST_BUFFER_DURATION_IS_VALID(buf))
        hr = pSample->SetSampleDuration(GST_BUFFER_DURATION(buf) / 100);

    if (SUCCEEDED(hr) && gst_buffer_map(buf, &info, GST_MAP_READ))
        unmap_buf = TRUE;
    else
        hr = E_FAIL;

    if (SUCCEEDED(hr) && decoder->header != NULL && decoder->header_size > 0)
        dwBufferSize = (DWORD)decoder->header_size + (DWORD)info.size;
    else if (SUCCEEDED(hr))
        dwBufferSize = (DWORD)info.size;

    if (SUCCEEDED(hr))
        hr = MFCreateMemoryBuffer(dwBufferSize, &pBuffer);

    if (SUCCEEDED(hr))
        hr = pBuffer->SetCurrentLength(dwBufferSize);

    if (SUCCEEDED(hr))
        hr = pBuffer->Lock(&pbBuffer, NULL, NULL);

    if (SUCCEEDED(hr))
        unlock_buf = TRUE;

    if (SUCCEEDED(hr) && decoder->header != NULL && decoder->header_size > 0)
    {
        if (dwBufferSize >= decoder->header_size)
        {
            memcpy_s(pbBuffer, dwBufferSize, decoder->header, decoder->header_size);
            pbBuffer += decoder->header_size;
            dwBufferSize -= decoder->header_size;

            if (dwBufferSize >= info.size)
            {
                memcpy_s(pbBuffer, dwBufferSize, info.data, info.size);
                mfwrapper_nalu_to_start_code(pbBuffer, info.size);
            }
            else
            {
                hr = E_FAIL;
            }
        }
        else
        {
            hr = E_FAIL;
        }
    }
    else if (SUCCEEDED(hr))
    {
        memcpy_s(pbBuffer, dwBufferSize, info.data, info.size);
        mfwrapper_nalu_to_start_code(pbBuffer, info.size);
    }

    if (decoder->header != NULL)
    {
        delete[] decoder->header;
        decoder->header = NULL;
        decoder->header_size = 0;
    }

    if (unlock_buf)
        hr = pBuffer->Unlock();

    if (unmap_buf)
        gst_buffer_unmap(buf, &info);

    if (SUCCEEDED(hr))
        hr = pSample->AddBuffer(pBuffer);

    if (SUCCEEDED(hr))
        hr = decoder->pDecoder->ProcessInput(0, pSample, 0);

    gst_buffer_unref(buf);

    SafeRelease(&pBuffer);
    SafeRelease(&pSample);

    if (SUCCEEDED(hr))
        return TRUE;
    else
        return FALSE;
}

static HRESULT mfwrapper_init_colorconvert(GstMFWrapper *decoder)
{
    DWORD dwStatus = 0;

    IMFMediaType *pDecoderOutputType = NULL;
    IMFMediaType *pInputType = NULL;
    IMFMediaType *pConverterOutputType = NULL;
    IMFMediaType *pOutputType = NULL;
    GUID subType;
    UINT32 unDefaultStride = 0;
    UINT32 unNumerator = 0;
    UINT32 unDenominator = 0;
    MFT_OUTPUT_STREAM_INFO outputStreamInfo;

    HRESULT hr = CoCreateInstance(CLSID_VideoProcessorMFT, NULL, CLSCTX_ALL, IID_PPV_ARGS(&decoder->pColorConvert));

    // Set input type
    DWORD dwTypeIndex = 0;
    do
    {
        hr = decoder->pDecoder->GetOutputAvailableType(0, dwTypeIndex, &pDecoderOutputType);
        dwTypeIndex++;

        if (SUCCEEDED(hr))
            hr = pDecoderOutputType->GetGUID(MF_MT_SUBTYPE, &subType);

        if (SUCCEEDED(hr) && !IsEqualGUID(subType, MFVideoFormat_NV12))
        {
            SafeRelease(&pDecoderOutputType);
            hr = E_FAIL;
            continue;
        }

        if (SUCCEEDED(hr))
            hr = MFCreateMediaType(&pInputType);

        if (SUCCEEDED(hr))
            hr = pInputType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);

        if (SUCCEEDED(hr))
            hr = pInputType->SetGUID(MF_MT_SUBTYPE, subType);

        if (SUCCEEDED(hr))
            hr = MFSetAttributeSize(pInputType, MF_MT_FRAME_SIZE, decoder->width, decoder->height);

        if (SUCCEEDED(hr))
            hr = MFSetAttributeRatio(pInputType, MF_MT_FRAME_RATE, decoder->framerate_num, decoder->framerate_den);

        if (SUCCEEDED(hr))
        {
            hr = pDecoderOutputType->GetUINT32(MF_MT_DEFAULT_STRIDE, &unDefaultStride);
            if (SUCCEEDED(hr))
                hr = pInputType->SetUINT32(MF_MT_DEFAULT_STRIDE, unDefaultStride);
        }

        if (SUCCEEDED(hr))
        {
            hr = MFGetAttributeRatio(pDecoderOutputType, MF_MT_PIXEL_ASPECT_RATIO, &unNumerator, &unDenominator);
            if (SUCCEEDED(hr))
                hr = MFSetAttributeRatio(pInputType, MF_MT_PIXEL_ASPECT_RATIO, unNumerator, unDenominator);
        }

        if (SUCCEEDED(hr))
            hr = decoder->pColorConvert->SetInputType(0, pInputType, 0);

        SafeRelease(&pInputType);

        if (SUCCEEDED(hr))
            hr = decoder->pDecoder->SetOutputType(0, pDecoderOutputType, 0);

        SafeRelease(&pDecoderOutputType);

    } while (hr != MF_E_NO_MORE_TYPES && FAILED(hr));

    // Set output type
    dwTypeIndex = 0;
    do
    {
        hr = decoder->pColorConvert->GetOutputAvailableType(0, dwTypeIndex, &pConverterOutputType);
        dwTypeIndex++;

        if (SUCCEEDED(hr))
            hr = pConverterOutputType->GetGUID(MF_MT_SUBTYPE, &subType);

        if (SUCCEEDED(hr) && !(IsEqualGUID(subType, MFVideoFormat_IYUV) || IsEqualGUID(subType, MFVideoFormat_I420)))
        {
            SafeRelease(&pConverterOutputType);
            hr = E_FAIL;
            continue;
        }

        SafeRelease(&pConverterOutputType);

        if (SUCCEEDED(hr))
            hr = MFCreateMediaType(&pOutputType);

        if (SUCCEEDED(hr))
            hr = pOutputType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);

        if (SUCCEEDED(hr))
            hr = pOutputType->SetGUID(MF_MT_SUBTYPE, subType);

        if (SUCCEEDED(hr))
            hr = MFSetAttributeSize(pOutputType, MF_MT_FRAME_SIZE, decoder->width, decoder->height);

        if (SUCCEEDED(hr))
            hr = MFSetAttributeRatio(pOutputType, MF_MT_FRAME_RATE, decoder->framerate_num, decoder->framerate_den);

        if (SUCCEEDED(hr))
            hr = decoder->pColorConvert->SetOutputType(0, pOutputType, 0);

        SafeRelease(&pOutputType);

    } while (hr != MF_E_NO_MORE_TYPES && FAILED(hr));

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->GetOutputStreamInfo(0, &outputStreamInfo);

    if (SUCCEEDED(hr))
    {
        if (!((outputStreamInfo.dwFlags & MFT_OUTPUT_STREAM_PROVIDES_SAMPLES) || (outputStreamInfo.dwFlags & MFT_OUTPUT_STREAM_CAN_PROVIDE_SAMPLES)))
        {
            hr = MFCreateSample(&decoder->pColorConvertOutput);
            if (SUCCEEDED(hr))
            {
                IMFMediaBuffer *pBuffer = NULL;
                hr = MFCreateMemoryBuffer(outputStreamInfo.cbSize, &pBuffer);
                if (SUCCEEDED(hr))
                    hr = decoder->pColorConvertOutput->AddBuffer(pBuffer);
                SafeRelease(&pBuffer);
            }
        }
    }

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->GetInputStatus(0, &dwStatus);

    if (FAILED(hr) || dwStatus != MFT_INPUT_STATUS_ACCEPT_DATA) {
        return hr;
    }

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, NULL);

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_NOTIFY_BEGIN_STREAMING, NULL);

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_NOTIFY_START_OF_STREAM, NULL);

    return hr;
}

static gboolean mfwrapper_convert_output(GstMFWrapper *decoder)
{
    DWORD dwFlags = 0;
    DWORD dwStatus = 0;
    MFT_OUTPUT_DATA_BUFFER outputDataBuffer;
    outputDataBuffer.dwStreamID = 0;
    outputDataBuffer.pSample = decoder->pColorConvertOutput;
    outputDataBuffer.dwStatus = 0;
    outputDataBuffer.pEvents = NULL;
    IMFMediaType *pOutputType = NULL;

    if (decoder->pColorConvert == NULL || decoder->pColorConvertOutput == NULL)
        return FALSE;

    // Extra call to unblock color converter, since it expects ProcessOutput to be called
    // until it returns MF_E_TRANSFORM_NEED_MORE_INPUT
    HRESULT hr = decoder->pColorConvert->ProcessOutput(0, 1, &outputDataBuffer, &dwStatus);

    hr = decoder->pColorConvert->ProcessInput(0, decoder->pDecoderOutput, 0);

    if (SUCCEEDED(hr))
        hr = decoder->pColorConvert->GetOutputStatus(&dwFlags);

    if (SUCCEEDED(hr) && dwFlags != MFT_OUTPUT_STATUS_SAMPLE_READY)
        return FALSE;

    hr = decoder->pColorConvert->ProcessOutput(0, 1, &outputDataBuffer, &dwStatus);
    SafeRelease(&outputDataBuffer.pEvents);
    if (hr == MF_E_TRANSFORM_STREAM_CHANGE)
    {
        if (outputDataBuffer.dwStatus == MFT_OUTPUT_DATA_BUFFER_FORMAT_CHANGE)
        {
            hr = decoder->pColorConvert->GetOutputAvailableType(0, 0, &pOutputType);

            if (SUCCEEDED(hr))
                hr = pOutputType->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_IYUV);

            if (SUCCEEDED(hr))
                hr = decoder->pColorConvert->SetOutputType(0, pOutputType, 0);

            SafeRelease(&pOutputType);
        }
    }
    else if (SUCCEEDED(hr))
    {
        if (outputDataBuffer.dwStatus == 0)
        {
            return TRUE;
        }
    }

    return FALSE;
}

static GstFlowReturn mfwrapper_deliver_sample(GstMFWrapper *decoder, IMFSample *pSample)
{
    GstFlowReturn ret = GST_FLOW_OK;
    LONGLONG llTimestamp = 0;
    LONGLONG llDuration = 0;
    IMFMediaBuffer *pMediaBuffer = NULL;
    BYTE *pBuffer = NULL;
    DWORD cbMaxLength = 0;
    DWORD cbCurrentLength = 0;
    GstMapInfo info;

    HRESULT hr = pSample->ConvertToContiguousBuffer(&pMediaBuffer);

    if (SUCCEEDED(hr))
        hr = pMediaBuffer->Lock(&pBuffer, &cbMaxLength, &cbCurrentLength);

    if (SUCCEEDED(hr) && cbCurrentLength > 0)
    {
        GstBuffer *pGstBuffer = gst_buffer_new_allocate(NULL, cbCurrentLength, NULL);
        if (pGstBuffer == NULL || !gst_buffer_map(pGstBuffer, &info, GST_MAP_WRITE))
        {
            pMediaBuffer->Unlock();
             if (pGstBuffer != NULL)
                gst_buffer_unref(pGstBuffer); // INLINE - gst_buffer_unref()
            return GST_FLOW_ERROR;
        }

        memcpy(info.data, pBuffer, cbCurrentLength);
        gst_buffer_unmap(pGstBuffer, &info);
        gst_buffer_set_size(pGstBuffer, cbCurrentLength);

        hr = pMediaBuffer->Unlock();
        if (SUCCEEDED(hr))
        {
            hr = pSample->GetSampleTime(&llTimestamp);
            GST_BUFFER_TIMESTAMP(pGstBuffer) = llTimestamp * 100;
        }

        if (SUCCEEDED(hr))
        {
            hr = pSample->GetSampleDuration(&llDuration);
            GST_BUFFER_DURATION(pGstBuffer) = llDuration * 100;
        }

        if (SUCCEEDED(hr) && decoder->force_output_discontinuity)
        {
            pGstBuffer = gst_buffer_make_writable(pGstBuffer);
            GST_BUFFER_FLAG_SET(pGstBuffer, GST_BUFFER_FLAG_DISCONT);
            decoder->force_output_discontinuity = FALSE;
        }

#if PTS_DEBUG
        if (GST_BUFFER_TIMESTAMP_IS_VALID(pGstBuffer) && GST_BUFFER_DURATION_IS_VALID(pGstBuffer))
            g_print("JFXMEDIA H265 %I64u %I64u\n", GST_BUFFER_TIMESTAMP(pGstBuffer), GST_BUFFER_DURATION(pGstBuffer));
        else if (GST_BUFFER_TIMESTAMP_IS_VALID(pGstBuffer) && !GST_BUFFER_DURATION_IS_VALID(pGstBuffer))
            g_print("JFXMEDIA H265 %I64u -1\n", GST_BUFFER_TIMESTAMP(pGstBuffer));
        else
            g_print("JFXMEDIA H265 -1\n");
#endif

        ret = gst_pad_push(decoder->srcpad, pGstBuffer);
    }
    else if (SUCCEEDED(hr))
    {
        pMediaBuffer->Unlock();
    }

    SafeRelease(&pMediaBuffer);

    return ret;
}

static gint mfwrapper_process_output(GstMFWrapper *decoder)
{
    IMFMediaType *pOutputType = NULL;
    MFT_OUTPUT_DATA_BUFFER outputDataBuffer;
    outputDataBuffer.dwStreamID = 0;
    outputDataBuffer.pSample = decoder->pDecoderOutput;
    outputDataBuffer.dwStatus = 0;
    outputDataBuffer.pEvents = NULL;
    DWORD dwFlags = 0;
    DWORD dwStatus = 0;
    GstFlowReturn ret = GST_FLOW_OK;

    if (!decoder->pDecoder)
        return PO_FAILED;

    if (decoder->is_eos || decoder->is_flushing)
        return PO_FLUSHING;

    HRESULT hr = decoder->pDecoder->GetOutputStatus(&dwFlags);
    if (SUCCEEDED(hr) && dwFlags != MFT_OUTPUT_STATUS_SAMPLE_READY)
        return PO_NEED_MORE_DATA;

    hr = decoder->pDecoder->ProcessOutput(0, 1, &outputDataBuffer, &dwStatus);
    SafeRelease(&outputDataBuffer.pEvents);
    if (hr == MF_E_TRANSFORM_NEED_MORE_INPUT)
    {
        return PO_NEED_MORE_DATA;
    }
    else if (hr == MF_E_TRANSFORM_STREAM_CHANGE)
    {
        if (outputDataBuffer.dwStatus == MFT_OUTPUT_DATA_BUFFER_FORMAT_CHANGE)
        {
            GUID subType;
            guint width = 0; guint height = 0;
            DWORD dwTypeIndex = 0;

            do
            {
                hr = decoder->pDecoder->GetOutputAvailableType(0, dwTypeIndex, &pOutputType);

                if (SUCCEEDED(hr))
                    hr = pOutputType->GetGUID(MF_MT_SUBTYPE, &subType);

                if (SUCCEEDED(hr))
                    hr = MFGetAttributeSize(pOutputType, MF_MT_FRAME_SIZE, &width, &height);

                if (SUCCEEDED(hr) && (decoder->width != width || decoder->height != height))
                {
                    decoder->width = width;
                    decoder->height = height;
                }

                // If decoder prefers MFVideoFormat_P010, then it means we dealing with 10 or 12-bit
                // HEVC, in this case setup color converter
                if (SUCCEEDED(hr) && dwTypeIndex == 0 && IsEqualGUID(subType, MFVideoFormat_P010))
                {
                    hr = mfwrapper_init_colorconvert(decoder);
                    break;
                }

                dwTypeIndex++;

                // I420 and IYUV are same
                if (SUCCEEDED(hr) && !IsEqualGUID(subType, MFVideoFormat_IYUV) && !IsEqualGUID(subType, MFVideoFormat_I420))
                {
                    SafeRelease(&pOutputType);
                    hr = E_FAIL;
                    continue;
                }

                if (SUCCEEDED(hr))
                    hr = decoder->pDecoder->SetOutputType(0, pOutputType, 0);
            } while (hr != MF_E_NO_MORE_TYPES && FAILED(hr));

            // Update caps on src pad in case if something changed
            if (SUCCEEDED(hr))
                mfwrapper_set_src_caps(decoder);

            SafeRelease(&pOutputType);
        }
    }
    else if (SUCCEEDED(hr))
    {
        if (outputDataBuffer.dwStatus == 0)
        {
            // Check if we need to convert output
            if (decoder->pColorConvert && decoder->pColorConvertOutput)
            {
                if (mfwrapper_convert_output(decoder))
                {
                    ret = mfwrapper_deliver_sample(decoder, decoder->pColorConvertOutput);
                }
            }
            else
            {
                ret = mfwrapper_deliver_sample(decoder, decoder->pDecoderOutput);
            }
        }
    }

    if (decoder->is_eos || decoder->is_flushing || ret != GST_FLOW_OK)
        return PO_FLUSHING;
    else if (SUCCEEDED(hr))
        return PO_DELIVERED;
    else
        return PO_FAILED;
}

// Processes input buffers
static GstFlowReturn mfwrapper_chain(GstPad *pad, GstObject *parent, GstBuffer *buf)
{
    GstFlowReturn ret = GST_FLOW_OK;
    GstMFWrapper *decoder = GST_MFWRAPPER(parent);

    if (decoder->is_flushing || decoder->is_eos_received)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(buf);
        return GST_FLOW_FLUSHING;
    }

    if (!mfwrapper_process_input(decoder, buf))
        return GST_FLOW_FLUSHING;

    gint po_ret = mfwrapper_process_output(decoder);
    if (po_ret != PO_DELIVERED && po_ret != PO_NEED_MORE_DATA)
        return GST_FLOW_FLUSHING;

    if (decoder->is_flushing)
        return GST_FLOW_FLUSHING;

    return ret;
}

static gboolean mfwrapper_push_sink_event(GstMFWrapper *decoder, GstEvent *event)
{
    gboolean ret = TRUE;

    if (gst_pad_is_linked(decoder->srcpad))
        ret = gst_pad_push_event(decoder->srcpad, gst_event_ref(event));  // INLINE - gst_event_ref()

    // INLINE - gst_event_unref()
    gst_event_unref(event);

    return ret;
}

static gboolean mfwrapper_sink_event(GstPad* pad, GstObject *parent, GstEvent *event)
{
    gboolean ret = FALSE;
    GstMFWrapper *decoder = GST_MFWRAPPER(parent);
    HRESULT hr = S_OK;

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_SEGMENT:
    {
        decoder->force_discontinuity = TRUE;
        ret = mfwrapper_push_sink_event(decoder, event);
        decoder->is_eos_received = FALSE;
        decoder->is_eos = FALSE;
    }
    break;
    case GST_EVENT_FLUSH_START:
    {
        decoder->is_flushing = TRUE;

        ret = mfwrapper_push_sink_event(decoder, event);
    }
    break;
    case GST_EVENT_FLUSH_STOP:
    {
        decoder->pDecoder->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, 0);
        if (decoder->pColorConvert)
            decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, 0);

        ret = mfwrapper_push_sink_event(decoder, event);

        decoder->is_flushing = FALSE;
    }
    break;
    case GST_EVENT_EOS:
    {
        decoder->is_eos_received = TRUE;

        // Let decoder know that we got end of stream
        hr = decoder->pDecoder->ProcessMessage(MFT_MESSAGE_NOTIFY_END_OF_STREAM, 0);

        // Ask decoder to produce all remaining data
        if (SUCCEEDED(hr))
            decoder->pDecoder->ProcessMessage(MFT_MESSAGE_COMMAND_DRAIN, 0);

        // Deliver remaining data
        gint po_ret;
        do
        {
            po_ret = mfwrapper_process_output(decoder);
        } while (po_ret == PO_DELIVERED);

        if (decoder->pColorConvert)
        {
            hr = decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_NOTIFY_END_OF_STREAM, 0);
            if (SUCCEEDED(hr))
                hr = decoder->pColorConvert->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, 0);
        }

        // We done pushing all frames. Deliver EOS.
        ret = mfwrapper_push_sink_event(decoder, event);

        decoder->is_eos = TRUE;
    }
    break;
    case GST_EVENT_CAPS:
    {
        GstCaps *caps;

        gst_event_parse_caps(event, &caps);
        if (!mfwrapper_sink_set_caps(pad, parent, caps))
        {
            gst_element_message_full(GST_ELEMENT(decoder), GST_MESSAGE_ERROR, GST_STREAM_ERROR, GST_STREAM_ERROR_DECODE, g_strdup("Failed to decode stream"), NULL, ("mfwrapper.c"), ("mfwrapper_sink_event"), 0);
        }

        // INLINE - gst_event_unref()
        gst_event_unref(event);
        ret = TRUE;
    }
    break;
    default:
        ret = mfwrapper_push_sink_event(decoder, event);
        break;
    }

    return ret;
}

static gboolean mfwrapper_get_mf_media_types(GstCaps *caps, GUID *pMajorType, GUID *pSubType)
{
    GstStructure *s = NULL;
    const gchar *mimetype = NULL;

    if (caps == NULL || pMajorType == NULL || pSubType == NULL)
        return FALSE;

    s = gst_caps_get_structure(caps, 0);
    if (s != NULL)
    {
        mimetype = gst_structure_get_name(s);
        if (mimetype != NULL)
        {
            if (strstr(mimetype, "video/x-h265") != NULL)
            {
                *pMajorType = MFMediaType_Video;
                *pSubType = MFVideoFormat_HEVC;

                return TRUE;
            }
        }
    }

    return FALSE;
}

static HRESULT mfwrapper_load_decoder(GstMFWrapper *decoder, GstCaps *caps)
{
    HRESULT hr = S_OK;
    UINT32 count = 0;

    GUID majorType;
    GUID subType;

    IMFActivate **ppActivate = NULL;

    MFT_REGISTER_TYPE_INFO info = { 0 };

    if (decoder->pDecoder)
        return S_OK;

    if (!mfwrapper_get_mf_media_types(caps, &majorType, &subType))
        return E_FAIL;

    info.guidMajorType = majorType;
    info.guidSubtype = subType;

    hr = MFTEnumEx(MFT_CATEGORY_VIDEO_DECODER,
        MFT_ENUM_FLAG_SYNCMFT | MFT_ENUM_FLAG_LOCALMFT | MFT_ENUM_FLAG_SORTANDFILTER,
        &info,
        NULL,
        &ppActivate,
        &count);
    if (SUCCEEDED(hr) && count == 0)
    {
        hr = E_FAIL;
    }

    if (SUCCEEDED(hr))
    {
        hr = ppActivate[0]->ActivateObject(IID_PPV_ARGS(&decoder->pDecoder));
    }

    for (UINT32 i = 0; i < count; i++)
    {
        ppActivate[i]->Release();
    }

    CoTaskMemFree(ppActivate);

    return hr;
}

gsize mfwrapper_get_hevc_config(void *in, gsize in_size, BYTE *out, gsize out_size)
{
    guintptr bdata = (guintptr)in;
    guint8 arrayCount = 0;
    guint16 nalUnitsCount = 0;
    guint16 nalUnitLength = 0;
    guint ii = 0;
    guint jj = 0;
    gsize in_bytes_count = 22;
    gsize out_bytes_count = 0;
    guint8 startCode[4] = { 0x00, 0x00, 0x00, 0x01 };

    if (in_bytes_count > in_size)
        return 0;

    // Skip first 22 bytes
    bdata += in_bytes_count;

    // Get array count
    arrayCount = *(guint8*)bdata;
    bdata++; in_bytes_count++;

    for (ii = 0; ii < arrayCount; ii++) {
        if ((in_bytes_count + 3) > in_size)
            return 0;

        // Skip 1 byte, not needed
        bdata++; in_bytes_count++;

        // 2 bytes number of nal units in array
        nalUnitsCount = ((guint16)*(guint8*)bdata) << 8;
        bdata++; in_bytes_count++;
        nalUnitsCount |= (guint16)*(guint8*)bdata;
        bdata++; in_bytes_count++;

        for (jj = 0; jj < nalUnitsCount; jj++) {
            if ((in_bytes_count + 2) > in_size)
                return 0;

            nalUnitLength = ((guint16)*(guint8*)bdata) << 8;
            bdata++; in_bytes_count++;
            nalUnitLength |= (guint16)*(guint8*)bdata;
            bdata++; in_bytes_count++;

            if ((out_bytes_count + 4) > out_size)
                return 0;

            // Set start code
            memcpy(out, &startCode[0], sizeof(startCode));
            out += sizeof(startCode); out_bytes_count += sizeof(startCode);

            if ((out_bytes_count + nalUnitLength) > out_size)
                return 0;

            if ((in_bytes_count + nalUnitLength) > in_size)
                return 0;

            // Copy nal unit
            memcpy(out, (guint8*)bdata, nalUnitLength);
            bdata += nalUnitLength; in_bytes_count += nalUnitLength;
            out += nalUnitLength; out_bytes_count += nalUnitLength;
        }
    }

    return out_bytes_count;
}

static HRESULT mfwrapper_set_input_media_type(GstMFWrapper *decoder, GstCaps *caps)
{
    HRESULT hr = S_OK;

    IMFMediaType *pInputType = NULL;
    GUID majorType;
    GUID subType;
    GstStructure *s = NULL;

    s = gst_caps_get_structure(caps, 0);
    if (s == NULL)
        return E_FAIL;

    hr = MFCreateMediaType(&pInputType);

    if (!mfwrapper_get_mf_media_types(caps, &majorType, &subType))
        return E_FAIL;

    if (SUCCEEDED(hr))
        hr = pInputType->SetGUID(MF_MT_MAJOR_TYPE, majorType);

    if (SUCCEEDED(hr))
        hr = pInputType->SetGUID(MF_MT_SUBTYPE, subType);

    if (SUCCEEDED(hr) && gst_structure_get_int(s, "width", (gint*)&decoder->width) && gst_structure_get_int(s, "height", (gint*)&decoder->height))
        hr = MFSetAttributeSize(pInputType, MF_MT_FRAME_SIZE, decoder->width, decoder->height);

    if (SUCCEEDED(hr) && gst_structure_get_fraction(s, "framerate", (gint*)&decoder->framerate_num, (gint*)&decoder->framerate_den))
        hr = MFSetAttributeRatio(pInputType, MF_MT_FRAME_RATE, decoder->framerate_num, decoder->framerate_den);

    if (SUCCEEDED(hr))
        hr = decoder->pDecoder->SetInputType(0, pInputType, 0);

    SafeRelease(&pInputType);

    return hr;
}

static HRESULT mfwrapper_set_output_media_type(GstMFWrapper *decoder, GstCaps *caps)
{
    HRESULT hr = S_OK;

    IMFMediaType *pOutputType = NULL;

    hr = MFCreateMediaType(&pOutputType);

    if (SUCCEEDED(hr))
        hr = pOutputType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);

    if (SUCCEEDED(hr))
        hr = pOutputType->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_IYUV);

    if (SUCCEEDED(hr))
        hr = MFSetAttributeSize(pOutputType, MF_MT_FRAME_SIZE, decoder->width, decoder->height);

    if (SUCCEEDED(hr))
        hr = MFSetAttributeRatio(pOutputType, MF_MT_FRAME_RATE, decoder->framerate_num, decoder->framerate_den);

    if (SUCCEEDED(hr))
        hr = decoder->pDecoder->SetOutputType(0, pOutputType, 0);

    // Set srcpad caps
    mfwrapper_set_src_caps(decoder);

    SafeRelease(&pOutputType);

    return hr;
}

static gboolean mfwrapper_init_mf(GstMFWrapper *decoder, GstCaps *caps)
{
    HRESULT hr = S_OK;
    DWORD dwStatus = 0;
    GstStructure *s = NULL;
    const GValue *codec_data_value = NULL;
    GstBuffer *codec_data = NULL;
    gint skipSize = 0;
    IMFAttributes *pAttributes = NULL;
    UINT32 unFormatChange = FALSE;

    if (!decoder->is_decoder_initialized)
    {
        if (SUCCEEDED(hr))
            hr = mfwrapper_set_input_media_type(decoder, caps);

        if (SUCCEEDED(hr))
            hr = mfwrapper_set_output_media_type(decoder, caps);

        if (SUCCEEDED(hr))
            hr = decoder->pDecoder->GetInputStatus(0, &dwStatus);

        if (FAILED(hr) || dwStatus != MFT_INPUT_STATUS_ACCEPT_DATA) {
            return FALSE;
        }
    }

    if (SUCCEEDED(hr))
        s = gst_caps_get_structure(caps, 0);

    if (s == NULL)
        return FALSE;

    // Get HEVC Config
    GstMapInfo info;

    if (SUCCEEDED(hr))
        codec_data_value = gst_structure_get_value(s, "codec_data");

    if (codec_data_value)
        codec_data = gst_value_get_buffer(codec_data_value);

    if (codec_data != NULL)
    {
        if (gst_buffer_map(codec_data, &info, GST_MAP_READ) && info.size > 0)
        {
            // Free old one if exist
            if (decoder->header)
                delete[] decoder->header;

            decoder->header = new BYTE[info.size * 2]; // Should be enough, since we will only add several 4 bytes start codes to 3 nal units
            if (decoder->header == NULL)
            {
                gst_buffer_unmap(codec_data, &info);
                return FALSE;
            }

            decoder->header_size = mfwrapper_get_hevc_config(info.data, info.size, decoder->header, info.size * 2);
            gst_buffer_unmap(codec_data, &info);

            if (decoder->header_size <= 0)
            {
                delete[] decoder->header;
                decoder->header = NULL;
                return FALSE;
            }
        }
    }

    if (!decoder->is_decoder_initialized)
    {
        if (SUCCEEDED(hr))
            hr = decoder->pDecoder->ProcessMessage(MFT_MESSAGE_COMMAND_FLUSH, NULL);

        if (SUCCEEDED(hr))
            hr = decoder->pDecoder->ProcessMessage(MFT_MESSAGE_NOTIFY_BEGIN_STREAMING, NULL);

        if (SUCCEEDED(hr))
            hr = decoder->pDecoder->ProcessMessage(MFT_MESSAGE_NOTIFY_START_OF_STREAM, NULL);

        if (SUCCEEDED(hr))
            decoder->is_decoder_initialized = TRUE;
    }

    if (SUCCEEDED(hr))
        return TRUE;
    else
        return FALSE;
}

static gboolean mfwrapper_sink_set_caps(GstPad * pad, GstObject *parent, GstCaps * caps)
{
    gboolean ret = FALSE;
    GstMFWrapper *decoder = GST_MFWRAPPER(parent);

    if (pad == decoder->sinkpad)
    {
        ret = mfwrapper_init_mf(decoder, caps);
    }

    return ret;
}

static gboolean mfwrapper_activate(GstPad *pad, GstObject *parent)
{
    return gst_pad_activate_mode(pad, GST_PAD_MODE_PUSH, TRUE);
}

static gboolean mfwrapper_activatemode(GstPad *pad, GstObject *parent, GstPadMode mode, gboolean active)
{
    gboolean res = FALSE;
    GstMFWrapper *decoder = GST_MFWRAPPER(parent);

    switch (mode) {
    case GST_PAD_MODE_PUSH:
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

gboolean mfwrapper_init(GstPlugin* mfwrapper)
{
    return gst_element_register(mfwrapper, "mfwrapper", 512, GST_TYPE_MFWRAPPER);
}
