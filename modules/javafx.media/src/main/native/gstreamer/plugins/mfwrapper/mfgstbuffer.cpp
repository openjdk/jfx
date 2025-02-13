/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "mfgstbuffer.h"

CMFGSTBuffer::CMFGSTBuffer(DWORD cbMaxLength)
{
    m_ulRefCount = 0;

    m_ulLockCount = 0;
    InitializeCriticalSection(&m_csBufferLock);

    m_cbMaxLength = cbMaxLength;
    m_cbCurrentLength = 0;
    m_pbBuffer = NULL;

    m_pGstBuffer = NULL;
    m_bUnmapGstBuffer = FALSE;

    ZeroMemory(&m_CallbackData, sizeof(sCallbackData));
    GetGstBufferCallback = NULL;
}

CMFGSTBuffer::~CMFGSTBuffer()
{
    if (m_pbBuffer != NULL)
    {
        delete [] m_pbBuffer;
        m_pbBuffer = NULL;
    }

    if (m_bUnmapGstBuffer)
    {
        gst_buffer_unmap(m_pGstBuffer, &m_GstMapInfo);
        m_bUnmapGstBuffer = FALSE;
    }

    if (m_pGstBuffer != NULL)
    {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(m_pGstBuffer);
        m_pGstBuffer = NULL;
    }

    DeleteCriticalSection(&m_csBufferLock);
}

 // IMFMediaBuffer
HRESULT CMFGSTBuffer::GetCurrentLength(DWORD *pcbCurrentLength)
{
    if (pcbCurrentLength == NULL)
        return E_INVALIDARG;

    (*pcbCurrentLength) = m_cbCurrentLength;

    return S_OK;
}

HRESULT CMFGSTBuffer::GetMaxLength(DWORD *pcbMaxLength)
{
    if (pcbMaxLength == NULL)
        return E_INVALIDARG;

    (*pcbMaxLength) = m_cbMaxLength;

    return S_OK;
}

HRESULT CMFGSTBuffer::SetCurrentLength(DWORD cbCurrentLength)
{
    if (cbCurrentLength > m_cbMaxLength)
        return E_INVALIDARG;

    m_cbCurrentLength = cbCurrentLength;

    if (m_pGstBuffer)
        gst_buffer_set_size(m_pGstBuffer, cbCurrentLength);

    return S_OK;
}

HRESULT CMFGSTBuffer::Lock(BYTE **ppbBuffer, DWORD *pcbMaxLength, DWORD *pcbCurrentLength)
{
    HRESULT hr = E_FAIL;

    if (ppbBuffer == NULL)
        return E_INVALIDARG;

    if (m_cbMaxLength == 0)
        return E_INVALIDARG;

    EnterCriticalSection(&m_csBufferLock);
    // Unlikely Lock() will be called in infinite loop.
    if (m_ulLockCount != ULONG_MAX)
    {
        hr = AllocateOrGetBuffer(ppbBuffer);
        if (SUCCEEDED(hr))
        {
           if (pcbMaxLength != NULL)
                (*pcbMaxLength) = m_cbMaxLength;

            if (pcbCurrentLength != NULL)
                (*pcbCurrentLength) = m_cbCurrentLength;

            // Increment lock count when we provided buffer. Lock() can be called
            // multiple times and memory pointer should stay valid until last
            // Unlock() called. The caller MUST match Lock() / Unlock() calls
            // based on documentation.
            m_ulLockCount++;
        }
    }

    LeaveCriticalSection(&m_csBufferLock);

    return hr;
}

HRESULT CMFGSTBuffer::Unlock()
{
    HRESULT hr = E_FAIL;

    EnterCriticalSection(&m_csBufferLock);
    // If Unlock() called without Lock() we should fail.
    if (m_ulLockCount > 0)
    {
        m_ulLockCount--;
        if (m_ulLockCount == 0 && m_bUnmapGstBuffer)
        {
            gst_buffer_unmap(m_pGstBuffer, &m_GstMapInfo);
            m_bUnmapGstBuffer = FALSE;
        }
        hr = S_OK;
    }
    LeaveCriticalSection(&m_csBufferLock);

    return hr;
}

// IUnknown
HRESULT CMFGSTBuffer::QueryInterface(REFIID riid, void **ppvObject)
{
    if (!ppvObject)
    {
        return E_POINTER;
    }
    else if (riid == IID_IUnknown)
    {
        (*ppvObject) = static_cast<IUnknown *>(static_cast<IMFMediaBuffer *>(this));
    }
    else if (riid == IID_IMFMediaBuffer)
    {
        (*ppvObject) = static_cast<IMFMediaBuffer *>(this);
    }
    else
    {
        (*ppvObject) = NULL;
        return E_NOINTERFACE;
    }
    AddRef();
    return S_OK;
}

ULONG CMFGSTBuffer::AddRef()
{
    return InterlockedIncrement(&m_ulRefCount);
}

ULONG CMFGSTBuffer::Release()
{
    ULONG uCount = InterlockedDecrement(&m_ulRefCount);
    if (uCount == 0)
    {
        delete this;
    }
    return uCount;
}

// GStreamer interface
HRESULT CMFGSTBuffer::GetGstBuffer(GstBuffer **ppBuffer)
{
    if (ppBuffer == NULL)
        return E_INVALIDARG;

    // If we do not have GStreamer buffer or if it is still locked
    // return E_UNEXPECTED. Such condition should not happen, but
    // just in case we need to check for it.
    if (m_pGstBuffer == NULL || m_bUnmapGstBuffer)
        return E_UNEXPECTED;

    (*ppBuffer) = m_pGstBuffer;

    m_pGstBuffer = NULL;

    return S_OK;
}

HRESULT CMFGSTBuffer::SetCallbackData(sCallbackData *pCallbackData)
{
    if (pCallbackData == NULL)
        ZeroMemory(&m_CallbackData, sizeof(sCallbackData));
    else
        m_CallbackData = (*pCallbackData);

    return S_OK;
}

HRESULT CMFGSTBuffer::SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer,
            long lSize, sCallbackData *pCallbackData))
{
    if (function == NULL)
        return E_INVALIDARG;

    GetGstBufferCallback = function;

    return S_OK;
}

HRESULT CMFGSTBuffer::AllocateOrGetBuffer(BYTE **ppbBuffer)
{
    if (ppbBuffer == NULL)
        return E_INVALIDARG;

    // If we have GStreamer get buffer callback set, then call it to get
    // buffer. Otherwise allocate memory internally.
    if (GetGstBufferCallback != NULL)
    {
        // Get buffer if needed
        if (m_pGstBuffer == NULL)
        {
            GetGstBufferCallback(&m_pGstBuffer, (long)m_cbMaxLength, &m_CallbackData);
            if (m_pGstBuffer == NULL)
                return E_OUTOFMEMORY;
        }

        // Lock can be called multiple times, so if we have GStreamer buffer
        // allocated and mapped just return it.
        if (m_bUnmapGstBuffer)
        {
            (*ppbBuffer) = m_GstMapInfo.data;
        }
        else
        {
            // Map buffer and return it.
            if (!gst_buffer_map(m_pGstBuffer, &m_GstMapInfo, GST_MAP_READWRITE))
                return E_FAIL;

            // Just in case check that we got right buffer size.
            // GStreamer buffer can be bigger due to alligment.
            if (m_GstMapInfo.maxsize < m_cbMaxLength)
            {
                gst_buffer_unmap(m_pGstBuffer, &m_GstMapInfo);
                // INLINE - gst_buffer_unref()
                gst_buffer_unref(m_pGstBuffer);
                m_pGstBuffer = NULL;
                return E_FAIL;
            }

            m_bUnmapGstBuffer = TRUE;

            (*ppbBuffer) = m_GstMapInfo.data;
        }
    }
    else
    {
        // Allocate new buffer if needed
        if (m_pbBuffer == NULL)
        {
            m_pbBuffer = new (nothrow) BYTE[m_cbMaxLength];
            if (m_pbBuffer == NULL)
                return E_OUTOFMEMORY;

            (*ppbBuffer) = m_pbBuffer;
        }
        else if (m_pbBuffer != NULL)
        {
            (*ppbBuffer) = m_pbBuffer;
        }
    }

    return S_OK;
}
