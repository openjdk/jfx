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

#ifndef __MF_GST_BUFFER_H__
#define __MF_GST_BUFFER_H__

#include <gst/gst.h>

#include <new>

#include <mfidl.h>

using namespace std;

struct sCallbackData
{
    void *pCallbackData;
};

class CMFGSTBuffer : public IMFMediaBuffer
{
public:
    CMFGSTBuffer(DWORD cbMaxLength);
    ~CMFGSTBuffer();

    // IMFMediaBuffer
    HRESULT GetCurrentLength(DWORD *pcbCurrentLength);
    HRESULT GetMaxLength(DWORD *pcbMaxLength);
    HRESULT SetCurrentLength(DWORD cbCurrentLength);
    HRESULT Lock(BYTE **ppbBuffer, DWORD *pcbMaxLength, DWORD *pcbCurrentLength);
    HRESULT Unlock();

    // IUnknown
    HRESULT QueryInterface(REFIID riid, void **ppvObject);
    ULONG AddRef();
    ULONG Release();

    // GStreamer interface
    HRESULT GetGstBuffer(GstBuffer **ppBuffer);
    HRESULT SetCallbackData(sCallbackData *pCallbackData);
    HRESULT SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer,
            long lSize, sCallbackData *pCallbackData));

private:
    HRESULT AllocateOrGetBuffer(BYTE **ppbBuffer);

    ULONG m_ulRefCount;

    // Used to unlock buffer with last Unlock() call. Lock() / Unlock() can be
    // called multiple times, but the caller should match calls for
    // Lock() / Unlock().
    ULONG m_ulLockCount;
    // Used to protect Lock() / Unlock() which can be called by
    // multiple threads.
    CRITICAL_SECTION m_csBufferLock;

    DWORD m_cbMaxLength;
    DWORD m_cbCurrentLength;
    BYTE *m_pbBuffer;

    GstBuffer *m_pGstBuffer;
    BOOL m_bUnmapGstBuffer;
    GstMapInfo m_GstMapInfo;

    sCallbackData m_CallbackData;
    void (*GetGstBufferCallback)(GstBuffer **ppBuffer, long lSize,
            sCallbackData *pCallbackData);
};

#endif // __MF_GST_BUFFER_H__
