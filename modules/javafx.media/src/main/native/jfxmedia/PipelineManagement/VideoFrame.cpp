/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

// For UINT_MAX, we cannot use GLib here, since it is shared code between
// GStreamer and AVFoundation.
#include <limits.h>

#include "VideoFrame.h"
#include <Common/VSMemory.h>

//*************************************************************************************************
//********** class CVideoFrame
//*************************************************************************************************
CVideoFrame::CVideoFrame()
:   m_uiWidth(0),
    m_uiHeight(0),
    m_uiEncodedWidth(0),
    m_uiEncodedHeight(0),
    m_typeFrame(UNKNOWN),
    m_bHasAlpha(false),
    m_dTime(0.0),
    m_FrameDirty(false)
{
    Reset();
}

CVideoFrame::~CVideoFrame()
{
}

unsigned int CVideoFrame::GetWidth()
{
    return m_uiWidth;
}

unsigned int CVideoFrame::GetHeight()
{
    return m_uiHeight;
}

unsigned int CVideoFrame::GetEncodedWidth()
{
    return m_uiEncodedWidth;
}

unsigned int CVideoFrame::GetEncodedHeight()
{
    return m_uiEncodedHeight;
}

CVideoFrame::FrameType CVideoFrame::GetType()
{
    return m_typeFrame;
}

bool CVideoFrame::HasAlpha()
{
    return m_bHasAlpha;
}

double CVideoFrame::GetTime()
{
    return m_dTime;
}

unsigned int CVideoFrame::GetPlaneCount()
{
    return m_uiPlaneCount;
}

void CVideoFrame::SetPlaneCount(unsigned int count)
{
    if (count <= MAX_PLANE_COUNT) {
        m_uiPlaneCount = count;
    } else {
        // Should never happen
        m_uiPlaneCount = MAX_PLANE_COUNT;
    }
}

void* CVideoFrame::GetDataForPlane(unsigned int planeIndex)
{
    if (planeIndex < MAX_PLANE_COUNT) {
        return m_pvPlaneData[planeIndex];
    }
    return NULL;
}

unsigned long CVideoFrame::GetSizeForPlane(unsigned int planeIndex)
{
    if (planeIndex < MAX_PLANE_COUNT) {
        return m_pulPlaneSize[planeIndex];
    }
    return 0;
}

unsigned int CVideoFrame::GetStrideForPlane(unsigned int planeIndex)
{
    if (planeIndex < MAX_PLANE_COUNT) {
        return m_puiPlaneStrides[planeIndex];
    }
    return 0;
}

CVideoFrame *CVideoFrame::ConvertToFormat(FrameType type)
{
    return NULL;
}

void CVideoFrame::Reset()
{
    m_uiPlaneCount = 0;
    for (int i = 0; i < MAX_PLANE_COUNT; i++) {
        m_puiPlaneStrides[i] = 0;
        m_pulPlaneSize[i] = 0;
        m_pvPlaneData[i] = NULL;
    }
}

void CVideoFrame::SwapPlanes(unsigned int aa, unsigned int bb)
{
    if (aa != bb && aa < m_uiPlaneCount && bb < m_uiPlaneCount) {
        unsigned int stride = m_puiPlaneStrides[aa];
        m_puiPlaneStrides[aa] = m_puiPlaneStrides[bb];
        m_puiPlaneStrides[bb] = stride;

        unsigned long size = m_pulPlaneSize[aa];
        m_pulPlaneSize[aa] = m_pulPlaneSize[bb];
        m_pulPlaneSize[bb] = size;

        void *vptr = m_pvPlaneData[aa];
        m_pvPlaneData[aa] = m_pvPlaneData[bb];
        m_pvPlaneData[bb] = vptr;
    }
}

unsigned long CVideoFrame::CalcSize(unsigned int a, unsigned int b, bool *pbValid)
{
    if (pbValid == NULL || *(pbValid) == false) {
        return 0;
    }

    if (b > 0 && a <= (UINT_MAX / b)) {
        return (a * b);
    }

    *(pbValid) = false;
    return 0;
}

unsigned long CVideoFrame::AddSize(unsigned long a, unsigned long b, bool *pbValid)
{
    if (pbValid == NULL || *(pbValid) == false) {
        return 0;
    }

    // unsigned long can be 32-bit or 64-bit, make sure it is no more then UINT_MAX
    if (a <= UINT_MAX && b <= UINT_MAX && a <= (UINT_MAX - b)) {
        return (a + b);
    }

    *(pbValid) = false;
    return 0;
}

void* CVideoFrame::CalcPlanePointer(intptr_t baseAddress, unsigned int offset,
                                    unsigned long planeSize, unsigned long baseSize,
                                    bool *pbValid)
{
    if (pbValid == NULL || *(pbValid) == false) {
        return NULL;
    }

    // We will read planeSize bytes from baseAddress starting with offset, so
    // make sure we do not read pass baseSize.
    unsigned long endOfPlane = AddSize(offset, planeSize, pbValid);
    if (*(pbValid)) { // Make sure AddSize() did not failed.
        if (endOfPlane <= baseSize) {
            return (void*)(baseAddress + offset);
        }
    }

    *(pbValid) = false;
    return NULL;
}
