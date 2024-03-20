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

#import "CVVideoFrame.h"
#include <Utils/ColorConverter.h>

OSType gLastFormat = -1;

bool CVVideoFrame::IsFormatSupported(OSType format) {
    switch (format) {
        case kCVPixelFormatType_32BGRA:           // 'BGRA': 32 bit BGRA
        case kCVPixelFormatType_422YpCbCr8:       // '2vuy': Y'CbCr 4:2:2, ordered Cb Y'0 Cr Y'1
//        case kCVPixelFormatType_422YpCbCr8_yuvs:  // 'yuvs': Y'CbCr 4:2:2, ordered Y'0 Cb Y'1 Cr
        case kCVPixelFormatType_420YpCbCr8Planar: // 'y420': planar Y'CbCr 4:2:0
            return true;
    }
    return false;
}

CVVideoFrame::CVVideoFrame(CVPixelBufferRef pixelBuffer, double frameTime, uint64_t frameHostTime)
:
  m_bDisposePixelBuffer(false),
  m_pixelBuffer(pixelBuffer)
{
    // We can assume that buf is retained at least for the duration of this ctor
    // So postpone retaining it until we're absolutely sure we'll use it

    // fail fast
    OSType type = CVPixelBufferGetPixelFormatType(pixelBuffer);
    if (!IsFormatSupported(type)) {
        throw "CVVideoFrame: Invalid PixelFormat";
    }

    m_dTime = frameTime;
    m_frameHostTime = frameHostTime;
    m_uiWidth = (unsigned int)CVPixelBufferGetWidth(pixelBuffer);
    m_uiHeight = (unsigned int)CVPixelBufferGetHeight(pixelBuffer);

    size_t extLeft, extRight, extTop, extBottom;
    CVPixelBufferGetExtendedPixels(pixelBuffer, &extLeft, &extRight, &extTop, &extBottom);

    if (m_uiWidth <= (UINT_MAX - (unsigned int)extLeft) &&
          (m_uiWidth + (unsigned int)extLeft) <= (UINT_MAX - (unsigned int)extRight)) {
        m_uiEncodedWidth = m_uiWidth + (unsigned int)extLeft + (unsigned int)extRight;
    } else {
        throw "CVVideoFrame: Invalid frame size";
    }

    if (m_uiHeight <= (UINT_MAX - (unsigned int)extBottom)) {
        // ignore top, since 0,0 is where base addr starts
        m_uiEncodedHeight = m_uiHeight + (unsigned int)extBottom;
    } else {
        throw "CVVideoFrame: Invalid frame size";
    }

    Reset();
    m_FrameDirty = false;

    if (type != gLastFormat) {
        gLastFormat = type;
    }

    // determine frame type, throw an exception if it's unsupported
    switch (type) {
        case kCVPixelFormatType_32BGRA:            // 'BGRA': 32 bit BGRA
            m_typeFrame = BGRA_PRE;
            break;
        case kCVPixelFormatType_422YpCbCr8: // '2vuy':
//        case kCVPixelFormatType_422YpCbCr8_yuvs: // 'yuvs'
            m_typeFrame = YCbCr_422;
            break;
        case kCVPixelFormatType_420YpCbCr8Planar:
            m_typeFrame = YCbCr_420p;
            break;
        default:
            // ?? should've been caught already
            break;
    }

    if (CVPixelBufferIsPlanar(pixelBuffer)) {
        PreparePlanar();
    } else {
        PrepareChunky();
    }

    // Now retain the pixelBuffer so it doesn't go away
    m_bDisposePixelBuffer = true;
    CVPixelBufferRetain(m_pixelBuffer);
}

CVVideoFrame::~CVVideoFrame()
{
    Dispose();
}

void CVVideoFrame::PrepareChunky()
{
    SetPlaneCount(1);
    m_uiWidth = (unsigned int)CVPixelBufferGetWidth(m_pixelBuffer);
    m_uiHeight = (unsigned int)CVPixelBufferGetHeight(m_pixelBuffer);
    m_bHasAlpha = (m_typeFrame == BGRA_PRE);

    // We MUST lock the base address during the lifetime of this object
    // or else we could cause a crash
    CVReturn cr = CVPixelBufferLockBaseAddress(m_pixelBuffer, kCVPixelBufferLock_ReadOnly);
    if (kCVReturnSuccess != cr) {
        throw "CVVideoFrame: Unable to lock PixelBuffer base address";
    }

    m_pvPlaneData[0] = CVPixelBufferGetBaseAddress(m_pixelBuffer);
    m_puiPlaneStrides[0] = (int)CVPixelBufferGetBytesPerRow(m_pixelBuffer);

    bool bValid = true; // CalcSize() requires bValid to be true when called
    m_pulPlaneSize[0] = CalcSize(m_puiPlaneStrides[0], m_uiEncodedHeight, &bValid);
    if (!bValid) {
        throw "CVVideoFrame: Invalid frame size";
    }
}

void CVVideoFrame::PreparePlanar()
{
    SetPlaneCount((unsigned int)CVPixelBufferGetPlaneCount(m_pixelBuffer));
    m_bHasAlpha = false;

    // We MUST lock the base address during the lifetime of this object
    // or else we could cause a crash
    CVReturn cr = CVPixelBufferLockBaseAddress(m_pixelBuffer, kCVPixelBufferLock_ReadOnly);
    if (kCVReturnSuccess != cr) {
        throw "CVVideoFrame: Unable to lock PixelBuffer base address";
    }

    bool bValid = true; // CalcSize() requires bValid to be true when called
    for (int index = 0; index < GetPlaneCount(); index++) {
        m_puiPlaneStrides[index] = (unsigned int)CVPixelBufferGetBytesPerRowOfPlane(m_pixelBuffer, index);
        unsigned long ulHeightOfPlane = (unsigned long)CVPixelBufferGetHeightOfPlane(m_pixelBuffer, index);
        m_pulPlaneSize[index] = CalcSize(ulHeightOfPlane, m_puiPlaneStrides[index], &bValid);
        if (!bValid) {
            throw "CVVideoFrame: Invalid frame size";
        }
        m_pvPlaneData[index] = CVPixelBufferGetBaseAddressOfPlane(m_pixelBuffer, index);
    }

    if (m_typeFrame == YCbCr_420p) {
        // Need to swap the Cb/Cr planes (like I420)
        SwapPlanes(1, 2);
    }
}

void CVVideoFrame::Dispose()
{
    if (m_bDisposePixelBuffer) {
        CVPixelBufferUnlockBaseAddress(m_pixelBuffer, kCVPixelBufferLock_ReadOnly);
        CVPixelBufferRelease(m_pixelBuffer);
        m_pixelBuffer = NULL;
        m_bDisposePixelBuffer = false;
    }
}

CVideoFrame *CVVideoFrame::ConvertToFormat(FrameType type)
{
    if (YCbCr_422 == m_typeFrame && BGRA_PRE == type) {
        CVPixelBufferRef destPixelBuffer = NULL;
        if (kCVReturnSuccess == CVPixelBufferCreate(NULL, m_uiEncodedWidth, m_uiEncodedHeight,
                                                   k32BGRAPixelFormat, NULL, &destPixelBuffer)) {
            if (kCVReturnSuccess == CVPixelBufferLockBaseAddress(destPixelBuffer, 0)) {
                uint8_t* bgra = (uint8_t*)CVPixelBufferGetBaseAddress(destPixelBuffer);
                int32_t bgraStride = (int32_t)CVPixelBufferGetBytesPerRow(destPixelBuffer);
                uint8_t* srcData = (uint8_t*)m_pvPlaneData[0];

                if (0 == ColorConvert_YCbCr422p_to_BGRA32_no_alpha(bgra,
                                                                  bgraStride,
                                                                  m_uiEncodedWidth,
                                                                  m_uiEncodedHeight,
                                                                  srcData + 1,
                                                                  srcData + 2,
                                                                  srcData,
                                                                  m_puiPlaneStrides[0],
                                                                  m_puiPlaneStrides[0])) {
                    return new CVVideoFrame(destPixelBuffer, m_dTime, m_frameHostTime);
                }
            }
        }
    }

    return NULL;
}
