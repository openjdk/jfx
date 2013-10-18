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

#include "Allocator.h"

CAllocator::CAllocator(TCHAR *tszName, LPUNKNOWN pUnk, HRESULT *phr) : CBaseAllocator(tszName, pUnk, phr)
{
    m_pBuffer = NULL;
    ZeroMemory(&m_UserData, sizeof(sUserData));
    ReleaseSample = NULL;
    GetGstBuffer = NULL;
}

CAllocator::~CAllocator(void)
{
    CSample *pSample = NULL;
    for (;;) {
        pSample = (CSample*)m_lFree.RemoveHead();
        if (pSample != NULL) {
            delete pSample;
        } else {
            break;
        }
    }
}

HRESULT CAllocator::GetBuffer(IMediaSample **ppBuffer, REFERENCE_TIME *pStartTime, REFERENCE_TIME *pEndTime, DWORD dwFlags)
{
    HRESULT hr = S_OK;
    CSample *pSample = NULL;

    if (m_pBuffer == NULL)
    {
        if (GetGstBuffer != NULL)
            GetGstBuffer(&m_pBuffer, m_lSize, &m_UserData);

        if (m_pBuffer == NULL)
            return E_FAIL;
    }

    hr = CBaseAllocator::GetBuffer(ppBuffer, pStartTime, pEndTime, dwFlags);
    if (FAILED(hr))
        return hr;

    pSample = (CSample*)*ppBuffer;
    pSample->m_pGstBuffer = m_pBuffer;
    hr = pSample->SetPointer(GST_BUFFER_DATA(m_pBuffer), GST_BUFFER_SIZE(m_pBuffer));
    m_pBuffer = NULL;
    if (FAILED(hr))
        return hr;

    return S_OK;
}

HRESULT CAllocator::ReleaseBuffer(IMediaSample *pBuffer)
{
    HRESULT hr = S_OK;

    CSample *pSample = (CSample*)pBuffer;
    if (ReleaseSample != NULL)
    {
        ReleaseSample(pSample->m_pGstBuffer, &m_UserData);
        pSample->m_pGstBuffer = NULL;
    }

    hr = CBaseAllocator::ReleaseBuffer(pBuffer);
    if (FAILED(hr))
    {
        return hr;
    }

    return S_OK;
}

HRESULT CAllocator::Alloc()
{
    HRESULT hr = S_OK;

    for (long i = 0; i < m_lCount; i++)
    {
        CSample *pMediaSample = new CSample("CSample", this, &hr);
        if (!pMediaSample)
        {
            return E_OUTOFMEMORY;
        }

        if (FAILED(hr))
        {
            delete pMediaSample;
            return hr;
        }

        m_lFree.Add(pMediaSample);
    }

    return S_OK;
}

void CAllocator::Free()
{
    return;
}

HRESULT CAllocator::SetUserData(sUserData *pUserData)
{
    if (pUserData == NULL)
        ZeroMemory(&m_UserData, sizeof(sUserData));
    else
        m_UserData = *pUserData;

    return S_OK;
}

HRESULT CAllocator::SetGstBuffer(GstBuffer *pBuffer)
{
    if (pBuffer == NULL)
        return E_FAIL;

    m_pBuffer = pBuffer;

    return S_OK;
}

HRESULT CAllocator::SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData))
{
    if (function == NULL)
        return E_FAIL;

    ReleaseSample = function;

    return S_OK;
}

HRESULT CAllocator::SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData))
{
    if (function == NULL)
        return E_FAIL;

    GetGstBuffer = function;

    return S_OK;
}

STDMETHODIMP CAllocator::SetProperties(ALLOCATOR_PROPERTIES* pRequest, ALLOCATOR_PROPERTIES* pActual)
{
    // Do not allocate more then 1 buffers
    if (pRequest->cBuffers > 1)
        pRequest->cBuffers = 1;

    pActual->cbBuffer = m_lSize = pRequest->cbBuffer;
    pActual->cBuffers = m_lCount = pRequest->cBuffers;
    pActual->cbAlign = m_lAlignment = pRequest->cbAlign;
    pActual->cbPrefix = m_lPrefix = pRequest->cbPrefix;

    return S_OK;
}
