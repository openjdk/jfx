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

#include "Allocator.h"

// {C2D21B70-9810-4FBB-8BE3-EEF9250B0AB4}
static const GUID CLSID_Sink =
{ 0xc2d21b70, 0x9810, 0x4fbb, { 0x8b, 0xe3, 0xee, 0xf9, 0x25, 0xb, 0xa, 0xb4 } };

struct sOutputFormat
{
    GUID type;
    GUID subtype;
    BOOL bFixedSizeSamples;
    BOOL bTemporalCompression;
    ULONG lSampleSize;
    GUID formattype;
    BYTE *pFormat;
    ULONG length;
    BOOL bForceStereoOutput;
    BOOL bUseExternalAllocator;
};

enum SINK_EVENTS
{
    SINK_UNKNOWN_EVENT = 0,
    SINK_EOS,
    SINK_CODEC_DATA,
    SINK_AUDIO_RATE,
    SINK_AUDIO_CHANNELS,
    SINK_VIDEO_RESOLUTION,
};

class CInputPin : public CRendererInputPin
{
public:
    CInputPin(CBaseRenderer *pRenderer, HRESULT *phr, LPCWSTR Name);
    ~CInputPin();

    STDMETHODIMP GetAllocator(IMemAllocator **ppAllocator);
    STDMETHODIMP NotifyAllocator(IMemAllocator *pAllocator, BOOL bReadOnly);

    HRESULT SetUserData(sUserData *pUserData);
    HRESULT SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData));
    HRESULT SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData));
    HRESULT CreateAllocator();

public:
    bool m_bUseExternalAllocator;

private:
    CAllocator *m_pAlloc;
    IMemAllocator *m_pIAlloc;
};

class CSink : public CBaseRenderer
{
public:
    CSink(HRESULT *phr);

    HRESULT GetMediaType(int iPosition, CMediaType *pMediaType);
    HRESULT CheckMediaType(const CMediaType *pmt);
    HRESULT DoRenderSample(IMediaSample *pMediaSample);
    HRESULT DoRenderSampleInternal(IMediaSample *pMediaSample);
    HRESULT DoRenderSampleExternal(IMediaSample *pMediaSample);
    HRESULT DoRenderSampleApp(IMediaSample *pMediaSample);
    CBasePin *GetPin(int n);
    HRESULT GetSampleTimes(IMediaSample *pMediaSample, REFERENCE_TIME *pStartTime, REFERENCE_TIME *pEndTime);
    HRESULT SendEndOfStream();

    HRESULT InitMediaType(sOutputFormat *pOutputFormat);
    HRESULT SetUserData(sUserData *pUserData);
    HRESULT SetDeliverCallback(int (*function)(GstBuffer *pBuffer, sUserData *pUserData));
    HRESULT SetSinkEventCallback(int (*function)(int sinkEvent, void *pData, int size, sUserData *pUserData));
    HRESULT SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData));
    HRESULT SetGetGstBufferCallback(void (*function)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData));
    HRESULT SetRenderSampleAppCallback(void (*function)(BYTE *pData, long lSize, sUserData *pUserData));

private:
    CMediaType m_mediaType;

    sUserData m_UserData;
    int (*DeliverCallback)(GstBuffer *pBuffer, sUserData *pUserData);
    int (*SinkEventCallback)(int sinkEvent, void* pData, int size, sUserData *pUserData);
    void (*GetGstBuffer)(GstBuffer **ppBuffer, long lSize, sUserData *pUserData);
    void (*RenderSampleApp)(BYTE *pData, long lSize, sUserData *pUserData);

    bool m_bForceStereoOutput;
    bool m_bUseExternalAllocator;
};
