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
#include "mfutils.h"

CMFGSTByteStream::CMFGSTByteStream(QWORD qwLength, GstPad *pSinkPad, BOOL bIsSegmentedStream)
{
    m_ulRefCount = 0;

    m_qwPosition = 0;
    m_qwLength = qwLength;
    m_pBytes = NULL;
    m_cbBytes = 0;
    m_cbBytesRead = 0;
    m_pAsyncResult = NULL;
    m_eStreamState = StreamState::Open;
    m_eReadState = ReadState::Idle;
    m_bIsEOS = FALSE;
    m_bIsEOSEventReceived = FALSE;
    m_bIsSegmentedStream = bIsSegmentedStream;
    m_pSinkPad = pSinkPad;

    InitializeCriticalSection(&m_csLock);
}

CMFGSTByteStream::~CMFGSTByteStream()
{
    SafeRelease(&m_pAsyncResult);
    DeleteCriticalSection(&m_csLock);
}

HRESULT CMFGSTByteStream::ReadRangeAvailable()
{
    // Locked section
    {
        ByteStreamLock lock(this);

        if (m_eStreamState != StreamState::Open)
            return S_FALSE;

        if (m_eReadState != ReadState::Waiting)
            return S_FALSE;

        m_eReadState = ReadState::Reading;
    }

    return ReadData();
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

HRESULT CMFGSTByteStream::CompleteReadData(HRESULT readResult)
{
    IMFAsyncResult *pAsyncResult = NULL;

    // Locked section
    {
        ByteStreamLock lock(this);

        // Complete read only if we reading or waiting for data
        if ((m_eReadState == ReadState::Reading ||
             m_eReadState == ReadState::Waiting) &&
             m_pAsyncResult != NULL)
        {
            pAsyncResult = m_pAsyncResult;
            pAsyncResult->AddRef();
            pAsyncResult->SetStatus(readResult);

            m_eReadState = ReadState::Completed;
        }
    }

    if (pAsyncResult)
    {
        HRESULT hr = MFInvokeCallback(pAsyncResult);
        SafeRelease(&pAsyncResult);
        return hr;
    }

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
    ByteStreamLock lock(this);

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

    // Locked section
    {
        ByteStreamLock lock(this);

        if (m_eStreamState == StreamState::Closed)
            return MF_E_OPERATION_CANCELLED;

        if (m_eReadState != ReadState::Idle)
            return MF_E_INVALIDREQUEST;

        // Save read request
        m_pBytes = pb;
        m_cbBytes = cb;
        m_cbBytesRead = 0;

        // Create async result object to signal read completion
        hr = MFCreateAsyncResult(NULL, pCallback, punkState, &m_pAsyncResult);
        if (FAILED(hr))
            return hr;

        m_eReadState = ReadState::Reading;
    }

    return ReadData();
}

HRESULT CMFGSTByteStream::BeginWrite(const BYTE *pb, ULONG cb, IMFAsyncCallback *pCallback, IUnknown *punkState)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Close()
{
    {
        ByteStreamLock lock(this);

        if (m_eStreamState == StreamState::Closed)
            return S_OK;

        m_eStreamState = StreamState::Closed;
    }

    return CompleteReadData(MF_E_OPERATION_CANCELLED);
}

HRESULT CMFGSTByteStream::EndRead(IMFAsyncResult *pResult, ULONG *pcbRead)
{
    HRESULT hr = S_OK;
    IMFAsyncResult *pAsyncResult = NULL;

    if (pResult == NULL || pcbRead == NULL)
        return E_POINTER;

    // Locked section
    {
        ByteStreamLock lock(this);

        if (pResult != m_pAsyncResult)
            return E_INVALIDARG;

        if (m_eReadState != ReadState::Completed)
            return E_INVALIDARG;

        hr = pResult->GetStatus();
        *pcbRead = SUCCEEDED(hr) ? m_cbBytesRead : 0;

        pAsyncResult = m_pAsyncResult;
        m_pAsyncResult = NULL;
        m_pBytes = NULL;
        m_eReadState = ReadState::Idle;
    }

    // Release outside lock in case if we will get any calls back
    SafeRelease(&pAsyncResult);

    return hr;
}

HRESULT CMFGSTByteStream::EndWrite(IMFAsyncResult *pResult, ULONG *pcbWritten)
{
    return E_NOTIMPL;
}

HRESULT CMFGSTByteStream::Flush()
{
    return CompleteReadData(MF_E_OPERATION_CANCELLED);
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
    ByteStreamLock lock(this);

    if (pqwPosition == NULL)
        return E_POINTER;

    (*pqwPosition) = m_qwPosition;

    return S_OK;
}

HRESULT CMFGSTByteStream::GetLength(QWORD *pqwLength)
{
    ByteStreamLock lock(this);

    if (pqwLength == NULL)
        return E_FAIL;

    (*pqwLength) = m_qwLength;

    return S_OK;
}

HRESULT CMFGSTByteStream::IsEndOfStream(BOOL *pfEndOfStream)
{
    ByteStreamLock lock(this);

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
    HRESULT completionResult = S_OK;
    BOOL completeRead = FALSE;
    GstFlowReturn ret = GST_FLOW_ERROR;
    GstBuffer *buf = NULL;

    // Locked section
    {
        ByteStreamLock lock(this);

        // Read data from upstream
        while (m_eStreamState == StreamState::Open && m_eReadState == ReadState::Reading)
        {
            if (m_cbBytesRead == m_cbBytes || m_bIsEOS)
            {
                completeRead = TRUE;
                completionResult = S_OK;
                break; // We done
            }

            // If length known adjust m_cbBytes to make sure we do not read
            // pass EOS. "progressbuffer" or "hlsprogressbuffer" does not handle
            // last buffer nicely and will return EOS if we do not read exact
            // amount of data.
            if (m_qwPosition < m_qwLength && (m_qwPosition + m_cbBytes) > m_qwLength)
                m_cbBytes = (ULONG)(m_qwLength - m_qwPosition);

            if (m_cbBytesRead > m_cbBytes)
            {
                completeRead = TRUE;
                completionResult = E_FAIL;
                break;
            }

            ULONG cbBytes = m_cbBytes - m_cbBytesRead;
            guint64 offset = (guint64)m_qwPosition;

            // Pull data unlocked
            Unlock();
            ret = gst_pad_pull_range(m_pSinkPad, offset, (guint)cbBytes, &buf);
            Lock();

            // Recheck after pull. We might get closed.
            if (m_eStreamState == StreamState::Closed || m_eReadState != ReadState::Reading)
            {
                if (buf != NULL)
                {
                    gst_buffer_unref(buf);
                    buf = NULL;
                }
                break;
            }

            if (ret == GST_FLOW_FLUSHING)
            {
                // Wait for FX_EVENT_RANGE_READY. It will be send when data available.
                m_eReadState = ReadState::Waiting;
                break;
            }

            if (ret == GST_FLOW_EOS)
            {
                m_bIsEOS = TRUE;
                completeRead = TRUE;
                completionResult = S_OK;
                break;
            }

            if (ret != GST_FLOW_OK)
            {
                completeRead = TRUE;
                completionResult = E_FAIL;
                break;
            }

            HRESULT hr = PushDataBufferLocked(buf);
            buf = NULL;

            if (FAILED(hr))
            {
                completeRead = TRUE;
                completionResult = E_FAIL;
                break;
            }
        }
    }

    if (buf != NULL)
        gst_buffer_unref(buf);

    if (!completeRead)
        return S_OK;

    return CompleteReadData(completionResult);
}

HRESULT CMFGSTByteStream::PushDataBufferLocked(GstBuffer* pBuffer)
{
    HRESULT hr = S_OK;

    if (pBuffer == NULL)
        return E_POINTER;

    // Set EOS flag, so we can complete and signal EOS
    if (m_bIsEOSEventReceived)
        m_bIsEOS = TRUE;

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
