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

// {42ED52C9-3760-4649-90C3-B227ACB719C1}
static const GUID CLSID_Src =
{ 0x42ed52c9, 0x3760, 0x4649, { 0x90, 0xc3, 0xb2, 0x27, 0xac, 0xb7, 0x19, 0xc1 } };

// {bbeea841-0a63-4f52-a7ab-a9b3a84ed38a}
static const GUID CLSID_CMP3DecMediaObject =
{0xbbeea841, 0x0a63, 0x4f52, {0xa7, 0xab, 0xa9, 0xb3, 0xa8, 0x4e, 0xd3, 0x8a}};

struct sInputFormat
{
    GUID type;
    GUID subtype;
    BOOL bFixedSizeSamples;
    BOOL bTemporalCompression;
    ULONG lSampleSize;
    GUID formattype;
    BYTE *pFormat;
    ULONG length;
};

class COutputPin : public CBaseOutputPin
{
public:
    COutputPin(CCritSec *pLock, CBaseFilter *pFilter, HRESULT *phr);
    ~COutputPin();

    HRESULT GetMediaType(int iPosition, CMediaType *pMediaType);
    HRESULT CheckMediaType(const CMediaType *pmt);
    HRESULT DecideBufferSize(IMemAllocator *pAlloc, ALLOCATOR_PROPERTIES *ppropInputRequest);
    HRESULT InitAllocator(IMemAllocator **ppAlloc);

    HRESULT InitMediaType(sInputFormat *pInputFormat);
    HRESULT DeliverSample(GstBuffer *pBuffer);
    HRESULT SetUserData(sUserData *pUserData);
    HRESULT SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData));
    HRESULT CreateAllocator();

private:
    CMediaType m_mediaType;

    CAllocator *m_pAlloc;
    IMemAllocator *m_pIAlloc;
};

class CSrc : public CBaseFilter
{
public:
    CSrc(HRESULT *phr);
    ~CSrc(void);

    CBasePin* GetPin(int n);
    int GetPinCount();

    HRESULT InitMediaType(sInputFormat *pInputFormat);
    HRESULT DeliverSample(GstBuffer *pBuffer);
    HRESULT SetUserData(sUserData *pUserData);
    HRESULT SetReleaseSampleCallback(void (*function)(GstBuffer *pBuffer, sUserData *pUserData));

public:
    COutputPin *m_pPin;

private:
    CCritSec m_Lock;
};
