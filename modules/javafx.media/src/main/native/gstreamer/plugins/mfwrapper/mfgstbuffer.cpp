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
    if (ppbBuffer == NULL)
        return E_INVALIDARG;

    if (m_cbMaxLength == 0)
        return E_INVALIDARG;

    HRESULT hr = AllocateOrGetBuffer(ppbBuffer);
    if (FAILED(hr))
        return hr;

    if (pcbMaxLength != NULL)
        (*pcbMaxLength) = m_cbMaxLength;

    if (pcbCurrentLength != NULL)
        (*pcbCurrentLength) = m_cbCurrentLength;

    return S_OK;
}

HRESULT CMFGSTBuffer::Unlock()
{
    if (m_bUnmapGstBuffer)
    {
        gst_buffer_unmap(m_pGstBuffer, &m_GstMapInfo);
        m_bUnmapGstBuffer = FALSE;
    }

    return S_OK;
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

    // If we do not have GStreamer buffer or it is still locked
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

    // If we have GStreamer get buffer cllback set, then call it to get
    // buffer. Otherwsie allocate memory internally.
    if (GetGstBufferCallback != NULL && m_pGstBuffer == NULL)
    {
        GetGstBufferCallback(&m_pGstBuffer, (long)m_cbMaxLength, &m_CallbackData);
        if (m_pGstBuffer == NULL)
            return E_OUTOFMEMORY;

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
    // Lock can be called multiple times, so if we have GStreamer buffer
    // allocated just return it.
    else if (GetGstBufferCallback != NULL && m_pGstBuffer != NULL && m_bUnmapGstBuffer)
    {
        (*ppbBuffer) = m_GstMapInfo.data;
    }
    // Allocate new buffer if needed
    else if (m_pbBuffer == NULL)
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

    return S_OK;
}
