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

#ifndef _VIDEO_FRAME_H_
#define _VIDEO_FRAME_H_

#include <stdlib.h>
#include <stdint.h>

#define MAX_PLANE_COUNT 4

/**
 * class CVideoFrame
 *
 * Class representing a video frame.  Specific engines may have their own derived
 * classes.  For example, CGstVideoFrame.
 */
class CVideoFrame
{
public:
    enum FrameType
    {
        UNKNOWN,
        // NOTE: These MUST be kept in sync with the native types in com.sun.media.jfxmedia.control.VideoFormat
        ARGB = 1,
        BGRA_PRE = 2,
        YCbCr_420p = 100,
        YCbCr_422 = 101,
        YCbCr_422_rev = 102
    };

public:
    CVideoFrame();
    virtual ~CVideoFrame();

    virtual void        Dispose() {}

    double              GetTime();

    unsigned int        GetWidth();
    unsigned int        GetHeight();
    unsigned int        GetEncodedWidth();
    unsigned int        GetEncodedHeight();

    FrameType           GetType();
    bool                HasAlpha();

    unsigned int        GetPlaneCount();
    void                SetPlaneCount(unsigned int count);
    void*               GetDataForPlane(unsigned int planeIndex);
    unsigned long       GetSizeForPlane(unsigned int planeIndex);
    unsigned int        GetStrideForPlane(unsigned int planeIndex);

    virtual CVideoFrame *ConvertToFormat(FrameType type);

    bool                GetFrameDirty() { return m_FrameDirty; }
    void                SetFrameDirty(bool dirty) { m_FrameDirty = dirty; }

protected:
    unsigned int        m_uiWidth;
    unsigned int        m_uiHeight;
    unsigned int        m_uiEncodedWidth;
    unsigned int        m_uiEncodedHeight;
    FrameType           m_typeFrame;
    bool                m_bHasAlpha;
    double              m_dTime;
    bool                m_FrameDirty;

    // frame data buffers
    void*               m_pvPlaneData[MAX_PLANE_COUNT];
    unsigned long       m_pulPlaneSize[MAX_PLANE_COUNT];
    unsigned int        m_puiPlaneStrides[MAX_PLANE_COUNT];

    void Reset();
    void SwapPlanes(unsigned int aa, unsigned int bb);

    // CalcSize(), AddSize(), CalcPlanePointer() requires bValid to be set to
    // true initially, if bValid is false these functions do nothing. It is
    // implemented this way, so all these functions can be chain called without
    // checking bValid after each call. bValid will be set to false only if
    // calculation failed and will never be set to true.
    // Multiplies a and b, bValid set to false if integer overflow detected.
    unsigned long CalcSize(unsigned int a, unsigned int b, bool *pbValid);
    // Adds a and b, bValid set to false if integer overflow detected.
    unsigned long AddSize(unsigned long a, unsigned long b, bool *pbValid);
    // Calculates plane pointer (baseAddress + offset) and checks that calculated
    // pointer within buffer. Returns NULL and sets bValid to false if calculated
    // pointer is invalid.
    void* CalcPlanePointer(intptr_t baseAddress, unsigned int offset,
                           unsigned long planeSize, unsigned long baseSize,
                           bool *pbValid);

private:
    unsigned int        m_uiPlaneCount;
};

#endif  //_VIDEO_FRAME_H_
