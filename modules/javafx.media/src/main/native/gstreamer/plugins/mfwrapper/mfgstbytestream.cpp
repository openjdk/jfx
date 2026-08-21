/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "mfgstbytestream.h"
#include "mftrace.h"

template <class T> void SafeRelease(T **ppT)
{
    if (*ppT)
    {
        (*ppT)->Release();
        *ppT = NULL;
    }
}

CMFGSTByteStream::CMFGSTByteStream(QWORD qwLength, GstPad *pSinkPad, BOOL bIsSegmentedStream)
{
    m_ulRefCount = 0;

    m_bIsSegmentedStream = bIsSegmentedStream;
    m_qwLength = qwLength;
    m_pSinkPad = pSinkPad;

    Reset();

    InitializeCriticalSection(&m_csLock);
}

CMFGSTByteStream::~CMFGSTByteStream()
{
    DeleteCriticalSection(&m_csLock);
}

void CMFGSTByteStream::Reset()
{
    m_pBytes = NULL;
    m_cbBytes = 0;
    m_cbBytesRead = 0;
    m_pAsyncResult = NULL;
    m_readResult = S_OK;
    m_qwPosition = 0;
    m_bWaitForEvent = FALSE;
    m_bIsAborted = FALSE;
    m_bIsEOS = FALSE;
    m_bIsEOSEventReceived = FALSE;
}

HRESULT CMFGSTByteStream::ReadRangeAvailable()
{
    Lock();
    BOOL bWaitForEvent = m_bWaitForEvent;
    m_bWaitForEvent = FALSE;
    Unlock();

    if (bWaitForEvent)
        return ReadData();
    else
        return S_FALSE;
}

void CMFGSTByteStream::SetStreamLength(QWORD qwLength)
{
    Lock();
    m_qwLength = qwLength;
    Unlock();
}

// Even if we reporting MFBYTESTREAM_IS_SEEKABLE to MF to make it happy (will not
// initialized otherwise), we can only issue seek on MF source reader if length
// is known (HTTP/FILE). For HLS we wil forward seek event upstream to handle
// seek.
bool CMFGSTByteStream::IsSeekSupported()
{
    return !m_bIsSegmentedStream;
}

HRESULT CMFGSTByteStream::CompleteReadData(HRESULT hr)
{
    m_readResult = hr;

    if (m_pAsyncResult)
        return MFInvokeCallback(m_pAsyncResult);

    return S_OK;
}

HRESULT CMFGSTByteStream::AbortRead(HRESULT hr)
{
    Lock();
    m_bIsAborted = TRUE;
    m_bWaitForEvent = FALSE;
    Unlock();

    return CompleteReadData(hr);
}

void CMFGSTByteStream::SignalEOS()
{
    Lock();
    m_bIsEOSEventReceived = TRUE;
    Unlock();
}

void CMFGSTByteStream::ClearEOS()
{
    Lock();
    m_bIsEOS = FALSE;
    m_bIsEOSEventReceived = FALSE;
    Unlock();
}

BOOL CMFGSTByteStream::IsReload()
{
    bool bIsReload = false;
    if (m_bIsSegmentedStream && !m_bIsEOSEventReceived)
        bIsReload = true;

    return bIsReload;
}

// IMFByteStream
HRESULT CMFGSTByteStream::BeginRead(BYTE *pb, ULONG cb, IMFAsyncCallback *pCallback, IUnknown *punkState)
{
    HRESULT hr = S_OK;

    if (pb == NULL || pCallback == NULL)
        return E_POINTER;

    if (m_pSinkPad == NULL)
        return E_POINTER;

    Lock();
    BOOL bIsAborted = m_bIsAborted;
    Unlock();

    if (bIsAborted)
        return MF_E_OPERATION_CANCELLED;

    // Reject BeginRead() if we already have pending read
    if (m_pAsyncResult != NULL)
        return MF_E_INVALIDREQUEST;

    // Do not start new read if old one failed
    if (m_readResult != S_OK)
        return m_readResult;

    // Save read request
    m_pBytes = pb;
    m_cbBytes = cb;
    m_cbBytesRead = 0;

    // Create async result object to signal read completion
    hr = MFCreateAsyncResult(NULL, pCallback, punkState, &m_pAsyncResult);
    if (FAILED(hr))
        return hr;

    return ReadData();
}

HRESULT CMFGSTByteStream::BeginWrite(const BYTE *pb, ULONG cb, IMFAsyncCallback *pCallback, IUnknown *punkState)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Close()
{
    // Nothing to close
    return S_OK;
}

HRESULT CMFGSTByteStream::EndRead(IMFAsyncResult *pResult, ULONG *pcbRead)
{
    Lock();
    m_bWaitForEvent = FALSE;
    Unlock();

    if (pResult == NULL || pcbRead == NULL)
        return E_POINTER;

    if (pResult != NULL)
        pResult->SetStatus(m_readResult);

    *pcbRead = m_cbBytesRead;

    SafeRelease(&m_pAsyncResult);

    return S_OK;
}

HRESULT CMFGSTByteStream::EndWrite(IMFAsyncResult *pResult, ULONG *pcbWritten)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Flush()
{
    return AbortRead(MF_E_OPERATION_CANCELLED);
}

HRESULT CMFGSTByteStream::GetCapabilities(DWORD *pdwCapabilities)
{
    if (pdwCapabilities == NULL)
        return E_POINTER;

    (*pdwCapabilities) = MFBYTESTREAM_IS_READABLE |
                         MFBYTESTREAM_IS_SEEKABLE |
                         MFBYTESTREAM_IS_REMOTE;
    return S_OK;
}

HRESULT CMFGSTByteStream::GetCurrentPosition(QWORD *pqwPosition)
{
    if (pqwPosition == NULL)
        return E_POINTER;

    (*pqwPosition) = m_qwPosition;

    return S_OK;
}

HRESULT CMFGSTByteStream::GetLength(QWORD *pqwLength)
{
    if (pqwLength == NULL)
        return E_FAIL;

    (*pqwLength) = m_qwLength;

    return S_OK;
}

HRESULT CMFGSTByteStream::IsEndOfStream(BOOL *pfEndOfStream)
{
    if (pfEndOfStream == NULL)
        return E_POINTER;

    if (m_bIsEOS)
        (*pfEndOfStream) = TRUE;
    else if (m_qwPosition >= m_qwLength)
        (*pfEndOfStream) = TRUE;
    else
        (*pfEndOfStream) = FALSE;

    return S_OK;
}

HRESULT CMFGSTByteStream::Read(BYTE *pb, ULONG cb, ULONG *pcbRead)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Seek(MFBYTESTREAM_SEEK_ORIGIN SeekOrigin, LONGLONG llSeekOffset, DWORD dwSeekFlags, QWORD *pqwCurrentPosition)
{
    HRESULT hr = S_OK;

    if (pqwCurrentPosition == NULL)
        return E_POINTER;

    QWORD qwSeekPosition = 0;
    switch (SeekOrigin)
    {
        case msoBegin:
            qwSeekPosition = llSeekOffset;
            break;
        case msoCurrent:
            qwSeekPosition = m_qwPosition + llSeekOffset;
            break;
        default:
            return E_FAIL;
    }

    hr = SetCurrentPosition(qwSeekPosition);
    if (FAILED(hr))
        return hr;

    *pqwCurrentPosition = m_qwPosition;

    return S_OK;
}

HRESULT CMFGSTByteStream::SetCurrentPosition(QWORD qwPosition)
{
    if (qwPosition > m_qwLength)
    {
        return E_INVALIDARG;
    }

    if (m_qwPosition == qwPosition)
    {
        return S_OK;
    }

    m_qwPosition = qwPosition;

    return S_OK;
}

HRESULT CMFGSTByteStream::SetLength(QWORD qwLength)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Write(const BYTE *pb, ULONG cb, ULONG *pcbWritten)
{
    return E_NOTIMPL;
}

// IUnknown
HRESULT CMFGSTByteStream::QueryInterface(REFIID riid, void **ppvObject)
{
    if (!ppvObject)
    {
        return E_POINTER;
    }
    else if (riid == IID_IUnknown)
    {
        *ppvObject = static_cast<IUnknown *>(static_cast<IMFByteStream *>(this));
    }
    else if (riid == IID_IMFByteStream)
    {
        *ppvObject = static_cast<IMFByteStream *>(this);
    }
    else
    {
        *ppvObject = NULL;
        return E_NOINTERFACE;
    }
    AddRef();
    return S_OK;
}

ULONG CMFGSTByteStream::AddRef()
{
    return InterlockedIncrement(&m_ulRefCount);
}

ULONG CMFGSTByteStream::Release()
{
    ULONG uCount = InterlockedDecrement(&m_ulRefCount);
    if (uCount == 0)
        delete this;
    return uCount;
}

HRESULT CMFGSTByteStream::ReadData()
{
    HRESULT hr = S_OK;
    GstFlowReturn ret = GST_FLOW_ERROR;
    GstBuffer *buf = NULL;
    guint64 offset = 0;
    ULONG cbBytes = 0;

    // Read data from upstream
    do
    {
        // If length known adjust m_cbBytes to make sure we do not read
        // pass EOS. "progressbuffer" or "hlsprogressbuffer" does not handle
        // last buffer nicely and will return EOS if we do not read exact
        // amount of data.
        Lock();
        if (m_bIsAborted)
        {
            Unlock();
            return CompleteReadData(MF_E_OPERATION_CANCELLED);
        }

        if (m_qwPosition < m_qwLength && (m_qwPosition + m_cbBytes) > m_qwLength)
            m_cbBytes = m_qwLength - m_qwPosition;

        if (m_cbBytesRead < m_cbBytes)
        {
            cbBytes = m_cbBytes - m_cbBytesRead;
        }
        else
        {
            Unlock();
            return CompleteReadData(E_FAIL);
        }

        offset = (guint64)m_qwPosition;

        Unlock();

        ret = gst_pad_pull_range(m_pSinkPad, offset, (guint)cbBytes, &buf);
        if (ret == GST_FLOW_FLUSHING)
        {
            // Wait for FX_EVENT_RANGE_READY. It will be send when data available.
            return PrepareWaitForData();
        }
        else if (ret == GST_FLOW_EOS)
        {
            m_bIsEOS = TRUE;
            return CompleteReadData(S_OK);
        }
        else if (ret == GST_FLOW_OK)
        {
            hr = PushDataBuffer(buf);
            if (FAILED(hr))
                return CompleteReadData(E_FAIL);
            else if (m_cbBytesRead == m_cbBytes || m_bIsEOS)
                return CompleteReadData(S_OK);
        }
        else
        {
            return CompleteReadData(E_FAIL);
        }
    } while (SUCCEEDED(hr) && m_cbBytesRead < m_cbBytes);

    return hr;
}

HRESULT CMFGSTByteStream::PushDataBuffer(GstBuffer* pBuffer)
{
    HRESULT hr = S_OK;

    if (pBuffer == NULL)
        return E_POINTER;

    // Set EOS flag, so we can complete and signal EOS
    Lock();
    if (m_bIsEOSEventReceived)
        m_bIsEOS = TRUE;
    Unlock();

    GstMapInfo info;
    gboolean unmap = FALSE;
    if (gst_buffer_map(pBuffer, &info, GST_MAP_READ))
        unmap = TRUE;
    else
        hr = E_FAIL;

    if (SUCCEEDED(hr) && m_cbBytesRead >= m_cbBytes)
        hr = E_FAIL;

    if (SUCCEEDED(hr) && (m_cbBytes - m_cbBytesRead) < info.size)
        hr = E_FAIL;

    if (SUCCEEDED(hr) &&
            memcpy_s(m_pBytes + m_cbBytesRead, m_cbBytes - m_cbBytesRead,
                    info.data, info.size) != 0)
    {
        hr = E_FAIL;
    }

    if (SUCCEEDED(hr))
    {
        m_cbBytesRead += info.size;
        m_qwPosition += info.size;
    }

    if (unmap)
        gst_buffer_unmap(pBuffer, &info);

    // INLINE - gst_buffer_unref()
    gst_buffer_unref(pBuffer);

    return hr;
}

HRESULT CMFGSTByteStream::PrepareWaitForData()
{
    Lock();
    m_bWaitForEvent = TRUE;
    Unlock();

    return S_OK;
}

void CMFGSTByteStream::Lock()
{
    EnterCriticalSection(&m_csLock);
}

void CMFGSTByteStream::Unlock()
{
    LeaveCriticalSection(&m_csLock);
}
