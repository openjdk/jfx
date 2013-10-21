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


#pragma once

#include <gst/gst.h>

#include <streams.h>

struct sUserData
{
    void *pUserData;
    int output_index;
    bool bFlag1;
};

class CSample : public CMediaSample
{
public:
    CSample(TCHAR *pName, CBaseAllocator *pAllocator, HRESULT *phr) : CMediaSample(pName, pAllocator, phr) { m_pGstBuffer = NULL; }
    GstBuffer *m_pGstBuffer;
};

class CAllocator : public CBaseAllocator
{
public:
    CAllocator(TCHAR *tszName, LPUNKNOWN pUnk, HRESULT *phr);
    ~CAllocator(void);

    STDMETHODIMP GetBuffer(IMediaSample **ppBuffer, REFERENCE_TIME *pStartTime, REFERENCE_TIME *pEndTime, DWORD dwFlags);
    STDMETHODIMP ReleaseBuffer(IMediaSample *pBuffer);
    HRESULT Alloc();
    void Free();
    STDMETHODIMP SetProperties(ALLOCATOR_PROPERTIES* pRequest, ALLOCATOR_PROPERTIES* pActual);

    HRESULT SetUserData(sUserData *pUserData);
    HRESULT SetGstBuffer(GstBuffer *pBuffer); // This function should be called before GetBuffer() (before CBaseOutputPin::GetDeliveryBuffer())
    HRESULT SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData));
    HRESULT SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData));

private:
    GstBuffer *m_pBuffer;
    sUserData m_UserData;
    void (*ReleaseSample)(GstBuffer *pBuffer, sUserData *pUserData);
    void (*GetGstBuffer)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData); // This function will be called before GetBuffer() (before CBaseOutputPin::GetDeliveryBuffer()) if SetGstBuffer was not called
};

