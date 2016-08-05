/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "VideoFrame.h"
#include <Common/VSMemory.h>

//*************************************************************************************************
//********** class CVideoFrame
//*************************************************************************************************
CVideoFrame::CVideoFrame()
:   m_iWidth(0),
    m_iHeight(0),
    m_iEncodedWidth(0),
    m_iEncodedHeight(0),
    m_typeFrame(UNKNOWN),
    m_bHasAlpha(false),
    m_dTime(0.0),
    m_FrameDirty(false),
    m_iPlaneCount(1)
{
    m_piPlaneStrides[0] = m_piPlaneStrides[1] = m_piPlaneStrides[2] = m_piPlaneStrides[3] = 0;
    m_pulPlaneSize[0] = m_pulPlaneSize[1] = m_pulPlaneSize[2] = m_pulPlaneSize[3] = 0;
    m_pvPlaneData[0] = m_pvPlaneData[1] = m_pvPlaneData[2] = m_pvPlaneData[3] = NULL;
}

CVideoFrame::~CVideoFrame()
{
}

int CVideoFrame::GetWidth()
{
    return m_iWidth;
}

int CVideoFrame::GetHeight()
{
    return m_iHeight;
}

int CVideoFrame::GetEncodedWidth()
{
    return m_iEncodedWidth;
}

int CVideoFrame::GetEncodedHeight()
{
    return m_iEncodedHeight;
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

int CVideoFrame::GetPlaneCount()
{
    return m_iPlaneCount;
}

void* CVideoFrame::GetDataForPlane(int planeIndex)
{
    if (planeIndex < 4 && planeIndex >= 0) {
        return m_pvPlaneData[planeIndex];
    }
    return NULL;
}

unsigned long CVideoFrame::GetSizeForPlane(int planeIndex)
{
    if (planeIndex < 4 && planeIndex >= 0) {
        return m_pulPlaneSize[planeIndex];
    }
    return 0;
}

int CVideoFrame::GetStrideForPlane(int planeIndex)
{
    if (planeIndex < 4 && planeIndex >= 0) {
        return m_piPlaneStrides[planeIndex];
    }
    return 0;
}

CVideoFrame *CVideoFrame::ConvertToFormat(FrameType type)
{
    return NULL;
}

void CVideoFrame::SwapPlanes(int aa, int bb)
{
    if (aa != bb && aa >= 0 && aa < m_iPlaneCount && bb >= 0 && bb < m_iPlaneCount) {
        int stride = m_piPlaneStrides[aa];
        m_piPlaneStrides[aa] = m_piPlaneStrides[bb];
        m_piPlaneStrides[bb] = stride;

        unsigned long size = m_pulPlaneSize[aa];
        m_pulPlaneSize[aa] = m_pulPlaneSize[bb];
        m_pulPlaneSize[bb] = size;

        void *vptr = m_pvPlaneData[aa];
        m_pvPlaneData[aa] = m_pvPlaneData[bb];
        m_pvPlaneData[bb] = vptr;
    }
}
