/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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


#include "Sink.h"

#include <Ks.h>
#include <Mmreg.h>
#include <Dvdmedia.h>
#include <Dxva.h>

#include "Allocator.h"

CInputPin::CInputPin(CBaseRenderer *pRenderer, HRESULT *phr, LPCWSTR Name) : CRendererInputPin(pRenderer, phr, Name)
{
    m_pIAlloc = NULL;
    m_pAlloc = NULL;
    m_bUseExternalAllocator = false;
    m_bEnableDynamicFormatChanges = false;
}

CInputPin::~CInputPin()
{
    if (m_pIAlloc != NULL)
    {
        m_pIAlloc->Release();
        m_pIAlloc = NULL;
    }
}

HRESULT CInputPin::GetAllocator(IMemAllocator **ppAllocator)
{
    HRESULT hr = S_OK;

    if (m_bUseExternalAllocator)
    {
        *ppAllocator = NULL;
        return VFW_E_NO_ALLOCATOR;
    }

    hr = CreateAllocator();
    if (FAILED(hr))
        return hr;

    if (m_pIAlloc)
    {
        *ppAllocator = m_pIAlloc;
        (*ppAllocator)->AddRef();
        return S_OK;
    }

    return S_OK;
}

HRESULT CInputPin::NotifyAllocator(IMemAllocator *pAllocator, BOOL bReadOnly)
{
    if (m_bUseExternalAllocator)
        return S_OK;

    if (m_pIAlloc == NULL && pAllocator != NULL)
        return S_OK;

    if (m_pIAlloc == pAllocator)
        return S_OK;

    return E_FAIL;
}

HRESULT CInputPin::SetUserData(sUserData *pUserData)
{
    HRESULT hr = S_OK;

    if (m_bUseExternalAllocator)
        return S_OK;

    hr = CreateAllocator();
    if (FAILED(hr))
        return hr;

    hr = m_pAlloc->SetUserData(pUserData);
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT CInputPin::SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    if (m_pAlloc)
        return m_pAlloc->SetReleaseSampleCallback(function);

    return S_OK;
}

HRESULT CInputPin::SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData))
{
    if (m_pAlloc)
        return m_pAlloc->SetGetGstBufferCallback(function);

    return S_OK;
}

HRESULT CInputPin::CreateAllocator()
{
    HRESULT hr = S_OK;

    if (m_pAlloc != NULL)
        return S_OK;

    m_pAlloc = new CAllocator("CAllocator", NULL, &hr);
    if (!m_pAlloc)
    {
        return E_OUTOFMEMORY;
    }

    if (FAILED(hr))
    {
        delete m_pAlloc;
        m_pAlloc = NULL;
        return hr;
    }

    hr = m_pAlloc->QueryInterface(IID_IMemAllocator, (void**)&m_pIAlloc);
    if (FAILED(hr))
    {
        delete m_pAlloc;
        m_pAlloc = NULL;
        return hr;
    }

    return S_OK;
}

HRESULT CInputPin::ReceiveConnection(IPin *pConnector, const AM_MEDIA_TYPE *pmt)
{
    HRESULT hr = CBasePin::ReceiveConnection(pConnector, pmt);
    if (m_bEnableDynamicFormatChanges && hr == VFW_E_ALREADY_CONNECTED)
    {
        CAutoLock cObjectLock(m_pLock);

        CMediaType *pcmt = (CMediaType*)pmt;
        hr = CheckMediaType(pcmt);
        if (hr == S_OK)
        {
            if (m_Connected != pConnector)
            {
                if (m_Connected != NULL)
                {
                    m_Connected->Release();
                }
                m_Connected = pConnector;
                m_Connected->AddRef();
            }

            hr = SetMediaType(pcmt);
            if (SUCCEEDED(hr))
            {
                if (IsEqualGUID(FORMAT_VideoInfo2, pcmt->formattype))
                {
                    VIDEOINFOHEADER2 *vih2 = (VIDEOINFOHEADER2*)pcmt->pbFormat;
                    DWORD dwSize = vih2->bmiHeader.biSizeImage;
                    if (dwSize > 0)
                    {
                        IMemAllocator *pAllocator = NULL;
                        hr = GetAllocator(&pAllocator);
                        if (SUCCEEDED(hr))
                        {
                            ALLOCATOR_PROPERTIES request;
                            ZeroMemory(&request, sizeof(ALLOCATOR_PROPERTIES));
                            hr = pAllocator->GetProperties(&request);
                            if (SUCCEEDED(hr))
                            {
                                request.cbBuffer = dwSize;
                                hr = pAllocator->Decommit();
                                if (SUCCEEDED(hr))
                                {
                                    ALLOCATOR_PROPERTIES actual;
                                    ZeroMemory(&actual, sizeof(ALLOCATOR_PROPERTIES));
                                    hr = pAllocator->SetProperties(&request, &actual);
                                    if (SUCCEEDED(hr))
                                    {
                                        hr = pAllocator->Commit();
                                    }
                                }
                            }

                            pAllocator->Release();
                        }
                    }
                }
            }
        }
    }

    return hr;
}

CSink::CSink(HRESULT *phr) : CBaseRenderer(CLSID_Sink, "CSink", NULL, phr)
{
    ZeroMemory(&m_UserData, sizeof(sUserData));

    DeliverCallback = NULL;
    SinkEventCallback = NULL;
    GetGstBuffer = NULL;
    RenderSampleApp = NULL;

    m_bForceStereoOutput = false;
    m_bUseExternalAllocator = false;
    m_bEnableDynamicFormatChanges = false;

    m_bEOSInProgress = false;
    m_bWorkerThreadExits = false;
}

HRESULT CSink::GetMediaType(int iPosition, CMediaType *pMediaType)
{
    if (IsEqualGUID(m_mediaType.majortype, GUID_NULL) &&
        IsEqualGUID(m_mediaType.subtype, GUID_NULL))
    {
        return VFW_S_NO_MORE_ITEMS;
    }

    if (iPosition < 0)
    {
        return E_INVALIDARG;
    }
    else if (iPosition == 0)
    {
        *pMediaType = m_mediaType;
        return S_OK;
    }
    else if (iPosition > 0)
    {
        return VFW_S_NO_MORE_ITEMS;
    }

    return E_UNEXPECTED;
}

HRESULT CSink::CheckMediaType(const CMediaType *pmt)
{
    if (IsEqualGUID(m_mediaType.majortype, GUID_NULL) &&
        IsEqualGUID(m_mediaType.subtype, GUID_NULL))
    {
        return S_OK;
    }

    if (IsEqualGUID(m_mediaType.majortype, pmt->majortype) &&
        IsEqualGUID(m_mediaType.subtype, pmt->subtype) &&
        IsEqualGUID(m_mediaType.formattype, pmt->formattype))
    {
        if (IsEqualGUID(FORMAT_WaveFormatEx, pmt->formattype))
        {
            WAVEFORMATEX *wfxin = (WAVEFORMATEX*)m_mediaType.pbFormat;
            WAVEFORMATEX *wfxout = (WAVEFORMATEX*)pmt->pbFormat;
            if (wfxout->wFormatTag == WAVE_FORMAT_EXTENSIBLE && wfxout->cbSize >= 22 && wfxout->nChannels > 2)
            {
                WAVEFORMATEXTENSIBLE* wfxeout = (WAVEFORMATEXTENSIBLE*)pmt->pbFormat;
                if (wfxin->nChannels != wfxeout->Format.nChannels && m_bForceStereoOutput)
                {
                    return S_FALSE;
                }
                else if (wfxin->nChannels != wfxeout->Format.nChannels && !m_bForceStereoOutput)
                {
                    if (SinkEventCallback != NULL)
                    {
                        int channels = wfxeout->Format.nChannels;
                        SinkEventCallback(SINK_AUDIO_CHANNELS, (void*)&channels, sizeof(int), &m_UserData);
                    }
                }

                if (wfxin->nSamplesPerSec != wfxeout->Format.nSamplesPerSec)
                {
                    if (SinkEventCallback != NULL)
                    {
                        int rate = wfxeout->Format.nSamplesPerSec;
                        SinkEventCallback(SINK_AUDIO_RATE, (void*)&rate, sizeof(int), &m_UserData);
                    }
                }

                if (SinkEventCallback != NULL)
                    SinkEventCallback(SINK_CODEC_DATA, (void*)pmt->pbFormat, pmt->cbFormat, &m_UserData);

                return S_OK;
            }
            else
            {
                if (wfxin->nChannels != wfxout->nChannels)
                {
                    if (SinkEventCallback != NULL)
                    {
                        int channels = wfxout->nChannels;
                        SinkEventCallback(SINK_AUDIO_CHANNELS, (void*)&channels, sizeof(int), &m_UserData);
                    }
                }

                if (wfxin->nSamplesPerSec != wfxout->nSamplesPerSec)
                {
                    if (SinkEventCallback != NULL)
                    {
                        int rate = wfxout->nSamplesPerSec;
                        SinkEventCallback(SINK_AUDIO_RATE, (void*)&rate, sizeof(int), &m_UserData);
                    }
                }

                return S_OK;
            }
        }
        else if (IsEqualGUID(FORMAT_VideoInfo2, pmt->formattype))
        {
            VIDEOINFOHEADER2 *hdrin = (VIDEOINFOHEADER2*)m_mediaType.pbFormat;
            VIDEOINFOHEADER2 *hdrout = (VIDEOINFOHEADER2*)pmt->pbFormat;

            // width or height
            if (hdrin->rcSource.right != hdrout->rcSource.right || hdrin->rcSource.bottom != hdrout->rcSource.bottom)
            {
                hdrin->rcSource.right = hdrout->rcSource.right;
                hdrin->rcSource.bottom = hdrout->rcSource.bottom;
                hdrin->rcTarget = hdrin->rcSource;

                if (SinkEventCallback != NULL)
                {
                    sVideoResolutionEvent resolution;
                    resolution.width = hdrout->rcSource.right;
                    resolution.height = hdrout->rcSource.bottom;
                    if (m_bEnableDynamicFormatChanges)
                        resolution.offset = resolution.width;
                    else
                        resolution.offset = 1920;
                    SinkEventCallback(SINK_VIDEO_RESOLUTION, (void*)&resolution, sizeof(sVideoResolutionEvent), &m_UserData);
                }
            }

            return S_OK;
        }
        else
        {
            return S_OK;
        }
    }

    return S_FALSE;
}

HRESULT CSink::DoRenderSample(IMediaSample *pMediaSample)
{
    if (RenderSampleApp != NULL)
        return DoRenderSampleApp(pMediaSample);
    else if (((CInputPin*)m_pInputPin)->m_bUseExternalAllocator)
        return DoRenderSampleExternal(pMediaSample);
    else
        return DoRenderSampleInternal(pMediaSample);
}

HRESULT CSink::DoRenderSampleInternal(IMediaSample *pMediaSample)
{
    HRESULT hr = S_OK;
    CSample *pSample = NULL;
    GstBuffer *pBuffer = NULL;
    REFERENCE_TIME start = 0;
    REFERENCE_TIME stop = 0;

    if (DeliverCallback == NULL)
        return S_FALSE;

    pSample = (CSample*)pMediaSample;
    if (pSample == NULL)
        return S_FALSE;

    pBuffer = pSample->m_pGstBuffer;
    pSample->m_pGstBuffer = NULL;
    if (pBuffer == NULL)
        return S_FALSE;

    hr = pMediaSample->GetTime(&start, &stop);
    if (SUCCEEDED(hr))
    {
        if (start < 0)
            start = 0;
        if (stop < 0)
            stop = 0;

        start *= 100;
        stop *= 100;

        GST_BUFFER_TIMESTAMP(pBuffer) = start;
        GST_BUFFER_DURATION(pBuffer) = stop - start;
    }

    gst_buffer_set_size(pBuffer, pMediaSample->GetActualDataLength());

    if (pMediaSample->IsDiscontinuity() == S_OK)
        GST_BUFFER_FLAG_SET(pBuffer, GST_BUFFER_FLAG_DISCONT);

    // Discontinuity is not reliable to check if given media sample should have new caps in GStLite
    bool bUpdateMediaType = false;
    AM_MEDIA_TYPE *pMediaType = NULL;
    hr = pMediaSample->GetMediaType(&pMediaType);
    if (hr == S_OK)
        bUpdateMediaType = true;
    if (pMediaType)
        DeleteMediaType(pMediaType);

    m_UserData.bFlag1 = bUpdateMediaType;

    if (!DeliverCallback(pBuffer, &m_UserData))
        return S_FALSE;

    return S_OK;
}

HRESULT CSink::DoRenderSampleExternal(IMediaSample *pMediaSample)
{
    HRESULT hr = S_OK;
    long lSize = 0;
    GstBuffer *pBuffer = NULL;
    GstMapInfo info;
    BYTE *pData = NULL;
    REFERENCE_TIME start = 0;
    REFERENCE_TIME stop = 0;

    if (pMediaSample == NULL)
        return S_FALSE;

    if (DeliverCallback == NULL)
        return S_FALSE;

    if (GetGstBuffer == NULL)
        return S_FALSE;

    lSize = pMediaSample->GetActualDataLength();
    if (lSize <= 0)
        return S_FALSE;

    GetGstBuffer(&pBuffer, lSize, &m_UserData);
    if (pBuffer == NULL)
        return S_FALSE;

    hr = pMediaSample->GetPointer(&pData);
    if (FAILED(hr) || pData == NULL)
        return S_FALSE;

    if (!gst_buffer_map(pBuffer, &info, GST_MAP_WRITE))
        return S_FALSE;

    memcpy(info.data, pData, lSize);
    gst_buffer_unmap(pBuffer, &info);
    gst_buffer_set_size(pBuffer, lSize);

    hr = pMediaSample->GetTime(&start, &stop);
    if (hr == S_OK)
    {
        if (start < 0)
            start = 0;
        if (stop < 0)
            stop = 0;
        if (stop <= start)
            stop = start + 1;

        start *= 100;
        stop *= 100;

        GST_BUFFER_TIMESTAMP(pBuffer) = start;
        GST_BUFFER_DURATION(pBuffer) = stop - start;
    }
    else if (hr == VFW_S_NO_STOP_TIME)
    {
        if (start < 0)
            start = 0;

        start *= 100;

        GST_BUFFER_TIMESTAMP(pBuffer) = start;
    }

    if (pMediaSample->IsDiscontinuity() == S_OK)
        GST_BUFFER_FLAG_SET(pBuffer, GST_BUFFER_FLAG_DISCONT);

    if (!DeliverCallback(pBuffer, &m_UserData))
        return S_FALSE;

    return S_OK;
}

HRESULT CSink::DoRenderSampleApp(IMediaSample *pMediaSample)
{
    if (pMediaSample == NULL)
        return S_FALSE;

    BYTE *pData = NULL;
    long lSize = 0;

    HRESULT hr = pMediaSample->GetPointer(&pData);
    if (FAILED(hr))
        return hr;

    lSize = pMediaSample->GetActualDataLength();
    if (lSize <= 0)
        return S_FALSE;

    if (RenderSampleApp)
        RenderSampleApp(pData, lSize, &m_UserData);

    return S_OK;
}

CBasePin *CSink::GetPin(int n)
{
    HRESULT hr = S_OK;

    if (n != 0)
        return NULL;

    if (m_pInputPin == NULL)
    {
        m_pInputPin = new CInputPin(this, &hr, L"Input");
        if (m_pInputPin == NULL)
            return NULL;

        if (FAILED(hr))
        {
            delete m_pInputPin;
            m_pInputPin = NULL;
            return NULL;
        }

        ((CInputPin*)m_pInputPin)->m_bUseExternalAllocator = m_bUseExternalAllocator;
        ((CInputPin*)m_pInputPin)->m_bEnableDynamicFormatChanges = m_bEnableDynamicFormatChanges;
    }

    return m_pInputPin;
}

HRESULT CSink::GetSampleTimes(IMediaSample *pMediaSample, REFERENCE_TIME *pStartTime, REFERENCE_TIME *pEndTime)
{
    return S_OK; // The sample should be rendered immediately
}

HRESULT CSink::SendEndOfStream()
{
    HRESULT hr = CBaseRenderer::SendEndOfStream();

    if (m_bEOSDelivered && SinkEventCallback != NULL)
    {
        // Check if sending EOS in progress. If true ignore additional requests.
        m_WorkerLock.Lock();
        if (m_bEOSInProgress || m_bWorkerThreadExits)
        {
            m_WorkerLock.Unlock();
            return S_OK;
        }
        m_WorkerLock.Unlock();

        CallWorker(CMD_SEND_EOS);
    }

    return hr;
}

HRESULT CSink::InitMediaType(sOutputFormat *pOutputFormat)
{
    if (pOutputFormat == NULL)
        return E_FAIL;

    m_mediaType.SetType(&pOutputFormat->type);
    m_mediaType.SetSubtype(&pOutputFormat->subtype);
    if (!pOutputFormat->bFixedSizeSamples)
        m_mediaType.SetVariableSize();
    m_mediaType.SetTemporalCompression(pOutputFormat->bTemporalCompression);
    m_mediaType.SetSampleSize(pOutputFormat->lSampleSize);
    m_mediaType.SetFormatType(&pOutputFormat->formattype);
    if (pOutputFormat->pFormat != NULL && pOutputFormat->length > 0)
    {
        if (!m_mediaType.SetFormat(pOutputFormat->pFormat, pOutputFormat->length))
            return E_FAIL;
    }

    // Save flags
    m_bForceStereoOutput = (pOutputFormat->bForceStereoOutput != 0);
    m_bUseExternalAllocator = (pOutputFormat->bUseExternalAllocator != 0);
    if (m_pInputPin)
        ((CInputPin*)m_pInputPin)->m_bUseExternalAllocator = m_bUseExternalAllocator;
    m_bEnableDynamicFormatChanges = (pOutputFormat->bEnableDynamicFormatChanges != 0);
    if (m_pInputPin)
        ((CInputPin*)m_pInputPin)->m_bEnableDynamicFormatChanges = m_bEnableDynamicFormatChanges;

    return S_OK;
}

HRESULT CSink::SetUserData(sUserData *pUserData)
{
    if (pUserData == NULL)
        ZeroMemory(&m_UserData, sizeof(sUserData));
    else
        m_UserData = *pUserData;

    return S_OK;
}

HRESULT CSink::SetDeliverCallback(int (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    if (function == NULL)
        return E_FAIL;

    DeliverCallback = function;

    return S_OK;
}

HRESULT CSink::SetSinkEventCallback(int (*function)(int sinkEvent, void *pData, int size, sUserData *pUserData))
{
    if (function == NULL)
        return E_FAIL;

    SinkEventCallback = function;

    return S_OK;
}

HRESULT CSink::SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    if (m_pInputPin == NULL)
        GetPin(0);

    CInputPin *pInputPin = (CInputPin*)m_pInputPin;
    if (pInputPin != NULL)
    {
        HRESULT hr = pInputPin->SetUserData(&m_UserData);
        if (FAILED(hr))
            return hr;

        return pInputPin->SetReleaseSampleCallback(function);
    }
    else
    {
        return E_FAIL;
    }
}

HRESULT CSink::SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData))
{
    GetGstBuffer = function;

    if (m_pInputPin == NULL)
        GetPin(0);

    CInputPin *pInputPin = (CInputPin*)m_pInputPin;
    if (pInputPin != NULL)
    {
        HRESULT hr = pInputPin->SetUserData(&m_UserData);
        if (FAILED(hr))
            return hr;

        return pInputPin->SetGetGstBufferCallback(function);
    }
    else
    {
        return E_FAIL;
    }
}

HRESULT CSink::SetRenderSampleAppCallback(void (*function)(BYTE *pData, long lSize, sUserData *pUserData))
{
    RenderSampleApp = function;

    return S_OK;
}

HRESULT CSink::StartWorkerThread()
{
    if (Create())
        return S_OK;
    else
        return E_FAIL;
}

HRESULT CSink::StopWorkerThread()
{
    CallWorker(CMD_EXIT);
    Close(); // Wait for thread to exit

    return S_OK;
}

DWORD CSink::ThreadProc(void)
{
    bool bRun = true;

    while (bRun)
    {
        COMMAND cmd = (COMMAND)GetRequest();
        switch (cmd)
        {
        case CMD_SEND_EOS:
            m_WorkerLock.Lock();
            m_bEOSInProgress = true;
            m_WorkerLock.Unlock();

            Reply(S_OK); // Unblock calling thread

            if (SinkEventCallback)
                SinkEventCallback(SINK_EOS, NULL, 0, &m_UserData);

            m_WorkerLock.Lock();
            m_bEOSInProgress = false;
            m_WorkerLock.Unlock();
            break;
        case CMD_EXIT:
            m_WorkerLock.Lock();
            m_bWorkerThreadExits = true;
            m_WorkerLock.Unlock();

            bRun = false;

            break;
        default:
            bRun = false;
            break;
        }
    }

    Reply(S_FALSE);

    return 0;
}