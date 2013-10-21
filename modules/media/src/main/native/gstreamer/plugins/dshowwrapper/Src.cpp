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


#include "Src.h"
#include "Allocator.h"

COutputPin::COutputPin(CCritSec *pLock, CBaseFilter *pFilter, HRESULT *phr) : CBaseOutputPin("COutputPin", pFilter, pLock, phr, L"output")
{
    m_pAlloc = NULL;
    m_pIAlloc = NULL;
}

COutputPin::~COutputPin()
{
    if (m_pIAlloc != NULL)
    {
        m_pIAlloc->Release();
        m_pIAlloc = NULL;
    }
}

HRESULT COutputPin::GetMediaType(int iPosition, CMediaType *pMediaType)
{
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

HRESULT COutputPin::CheckMediaType(const CMediaType *pmt)
{
    if (m_mediaType == *pmt)
        return S_OK;

    return S_FALSE;
}

HRESULT COutputPin::DecideBufferSize(IMemAllocator *pAlloc, ALLOCATOR_PROPERTIES *ppropInputRequest)
{
    HRESULT hr = S_OK;

    if (m_pIAlloc == pAlloc) // We only accept our own allocator
    {
        ALLOCATOR_PROPERTIES actual;

        ppropInputRequest->cBuffers = 1;
        ppropInputRequest->cbBuffer = 1;

        hr = m_pIAlloc->SetProperties(ppropInputRequest, &actual);
        if (FAILED(hr))
            return hr;

        hr = m_pIAlloc->Commit();
        if (FAILED(hr))
            return hr;

        return S_OK;
    }

    return E_FAIL;
}

HRESULT COutputPin::InitAllocator(IMemAllocator **ppAlloc)
{
    HRESULT hr = S_OK;

    if (!m_pIAlloc)
        return E_OUTOFMEMORY;

    *ppAlloc = m_pIAlloc;
    (*ppAlloc)->AddRef();

    return S_OK;
}

HRESULT COutputPin::InitMediaType(sInputFormat *pInputFormat)
{
    if (pInputFormat == NULL)
        return E_FAIL;

    m_mediaType.SetType(&pInputFormat->type);
    m_mediaType.SetSubtype(&pInputFormat->subtype);
    if (!pInputFormat->bFixedSizeSamples)
        m_mediaType.SetVariableSize();
    m_mediaType.SetTemporalCompression(pInputFormat->bTemporalCompression);
    m_mediaType.SetSampleSize(pInputFormat->lSampleSize);
    m_mediaType.SetFormatType(&pInputFormat->formattype);
    if (pInputFormat->pFormat != NULL && pInputFormat->length > 0)
    {
        if (!m_mediaType.SetFormat(pInputFormat->pFormat, pInputFormat->length))
            return E_FAIL;
    }

    return S_OK;
}

HRESULT COutputPin::DeliverSample(GstBuffer *pBuffer)
{
    HRESULT hr = S_OK;
    IMediaSample *pSample = NULL;
    REFERENCE_TIME start = -1;
    REFERENCE_TIME stop = -1;

    hr = m_pAlloc->SetGstBuffer(pBuffer);
    if (FAILED(hr))
        return hr;

    hr = GetDeliveryBuffer(&pSample, NULL, NULL, 0);
    if (FAILED(hr))
        return hr;

    // Set media time
    pSample->SetMediaTime(NULL, NULL);

    // Set time
    if (GST_BUFFER_TIMESTAMP_IS_VALID(pBuffer))
    {
        start = GST_BUFFER_TIMESTAMP(pBuffer) / 100;

        if (GST_BUFFER_DURATION_IS_VALID(pBuffer))
        {
            stop = (GST_BUFFER_TIMESTAMP(pBuffer) + GST_BUFFER_DURATION(pBuffer)) / 100;
        }
        else
        {
            stop = start + 1;
        }

        if (stop <= start) // Sometimes it may happen
            stop = start + 1;

        pSample->SetTime(&start, &stop);
    }
    else
    {
        pSample->SetTime(NULL, NULL);
    }

    if (GST_BUFFER_IS_DISCONT(pBuffer))
        pSample->SetDiscontinuity(TRUE);

    if (GST_BUFFER_FLAG_IS_SET(pBuffer, GST_BUFFER_FLAG_PREROLL))
        pSample->SetPreroll(TRUE);

    hr = Deliver(pSample);
    pSample->Release();
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT COutputPin::SetUserData(sUserData *pUserData)
{
    HRESULT hr = S_OK;

    hr = CreateAllocator();
    if (FAILED(hr))
        return hr;

    hr = m_pAlloc->SetUserData(pUserData);
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT COutputPin::SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    HRESULT hr = S_OK;

    hr = m_pAlloc->SetReleaseSampleCallback(function);
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT COutputPin::CreateAllocator()
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

CSrc::CSrc(HRESULT *phr) : CBaseFilter("CSrc", NULL, &m_Lock, CLSID_Src, phr)
{
    HRESULT hr = S_OK;

    m_pPin = new COutputPin(&m_Lock, this, &hr);
    if (m_pPin == NULL || FAILED(hr))
        *phr = E_FAIL;
}

CSrc::~CSrc(void)
{
    if (m_pPin != NULL)
    {
        delete m_pPin;
        m_pPin = NULL;
    }
}

CBasePin* CSrc::GetPin(int n)
{
    if (n == 0)
    {
        if (m_pPin)
            return (CBasePin*)m_pPin;
        else
            return NULL;
    }
    else
    {
        return NULL;
    }
}

int CSrc::GetPinCount()
{
    return 1; // Only 1 output pin
}


HRESULT CSrc::InitMediaType(sInputFormat *pInputFormat)
{
    if (m_pPin != NULL)
        return m_pPin->InitMediaType(pInputFormat);
    else
        return E_FAIL;
}

HRESULT CSrc::DeliverSample(GstBuffer *pBuffer)
{
    if (m_pPin != NULL)
        return m_pPin->DeliverSample(pBuffer);
    else
        return E_FAIL;
}

HRESULT CSrc::SetUserData(sUserData *pUserData)
{
    if (m_pPin != NULL)
        return m_pPin->SetUserData(pUserData);
    else
        return E_FAIL;
}

HRESULT CSrc::SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    if (m_pPin != NULL)
        return m_pPin->SetReleaseSampleCallback(function);
    else
        return E_FAIL;
}
