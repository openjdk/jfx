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

#ifndef __MF_GST_BYTESTREAM_H__
#define __MF_GST_BYTESTREAM_H__

#include <gst/gst.h>

#include <mfapi.h>
#include <mfidl.h>
#include <mfobjects.h>
#include <mferror.h>

// {00000000-0000-0000-0000-000000000000}
static const GUID GUID_NULL =
{ 0x00000000, 0x0000, 0x0000, { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 } };

class CMFGSTByteStream : public IMFByteStream
{
public:
    CMFGSTByteStream(QWORD qwLength, GstPad *pSinkPad, BOOL bIsSegmentedStream);
    ~CMFGSTByteStream();

    void Reset();
    HRESULT ReadRangeAvailable();
    void SetStreamLength(QWORD qwLength);
    bool IsSeekSupported();
    HRESULT CompleteReadData(HRESULT hr);
    void SignalEOS();
    void ClearEOS();
    BOOL IsReload();
    HRESULT AbortRead(HRESULT hr);

    // IMFByteStream
    HRESULT BeginRead(BYTE *pb, ULONG cb, IMFAsyncCallback *pCallback,
            IUnknown *punkState);
    HRESULT BeginWrite(const BYTE *pb, ULONG cb, IMFAsyncCallback *pCallback,
            IUnknown *punkState);
    HRESULT Close();
    HRESULT EndRead(IMFAsyncResult *pResult, ULONG *pcbRead);
    HRESULT EndWrite(IMFAsyncResult *pResult, ULONG *pcbWritten);
    HRESULT Flush();
    HRESULT GetCapabilities(DWORD *pdwCapabilities);
    HRESULT GetCurrentPosition(QWORD *pqwPosition);
    HRESULT GetLength(QWORD *pqwLength);
    HRESULT IsEndOfStream(BOOL *pfEndOfStream);
    HRESULT Read(BYTE *pb, ULONG cb, ULONG *pcbRead);
    HRESULT Seek(MFBYTESTREAM_SEEK_ORIGIN SeekOrigin, LONGLONG llSeekOffset,
            DWORD dwSeekFlags, QWORD *pqwCurrentPosition);
    HRESULT SetCurrentPosition(QWORD qwPosition);
    HRESULT SetLength(QWORD qwLength);
    HRESULT Write(const BYTE *pb, ULONG cb, ULONG *pcbWritten);

    // IUnknown
    HRESULT QueryInterface(REFIID riid, void **ppvObject);
    ULONG AddRef();
    ULONG Release();

private:
    HRESULT ReadData();
    HRESULT PushDataBuffer(GstBuffer *pBuffer);
    HRESULT PrepareWaitForData();

    void Lock();
    void Unlock();

    ULONG m_ulRefCount;

    QWORD m_qwPosition;
    QWORD m_qwLength;

    // Pointer to store read bytes
    BYTE *m_pBytes;
    // Total number of bytes requested
    ULONG m_cbBytes;
    // Bytes read and stored in m_pBytes
    ULONG m_cbBytesRead;
    // Completion result
    IMFAsyncResult *m_pAsyncResult;
    // Read result
    HRESULT m_readResult;

    BOOL m_bWaitForEvent;
    BOOL m_bIsAborted;
    BOOL m_bIsEOS;
    BOOL m_bIsEOSEventReceived;
    // Set to true if source is fragmented MP4
    BOOL m_bIsSegmentedStream;

    CRITICAL_SECTION m_csLock;

    GstPad *m_pSinkPad;
};

#endif // __MF_GST_BYTESTREAM_H__
