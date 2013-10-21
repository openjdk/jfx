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

#import "CVVideoFrame.h"
#include <Utils/ColorConverter.h>

CVVideoFrame::CVVideoFrame(CVPixelBufferRef buf, double frameTime, uint64_t frameHostTime, unsigned long frameNumber)
: 
  pixelBuffer(0),
  frameHostTime(frameHostTime)
{
    pixelBuffer = CVPixelBufferRetain(buf);
    
    // determine frame type, throw an exception if it's unsupported
    if (CVPixelBufferIsPlanar(buf)) {
        throw "CVVideoFrame: Planar buffers not supported";
    }
    
    OSType type = CVPixelBufferGetPixelFormatType(buf);
    switch (type) {
        case k32ARGBPixelFormat:
            m_typeFrame = BGRA_PRE;
            break;
        case '2vuy':
//        case 'yuvs':
            m_typeFrame = YCbCr_422;
            break;
        
        default:
            throw "CVVideoFrame: Invalid PixelFormat";
            break;
    }
    
    // Set up CVideoFrame data members
        // FIXME: encoded width/height??
    m_iWidth = (int)CVPixelBufferGetWidth(pixelBuffer);
    m_iHeight = (int)CVPixelBufferGetHeight(pixelBuffer);
    m_bHasAlpha = (m_typeFrame == BGRA_PRE);
    
    size_t extLeft, extRight, extTop, extBottom;
    CVPixelBufferGetExtendedPixels(pixelBuffer, &extLeft, &extRight, &extTop, &extBottom);
    m_iEncodedWidth = m_iWidth + extLeft + extRight;
    m_iEncodedHeight = m_iHeight + extBottom; // ignore top, since 0,0 is where base addr starts
    
    // Change this if we ever encounter planar CVPixelBuffers
    m_iPlaneCount = 1;
    
    m_piPlaneOffsets[0] = m_piPlaneOffsets[1] = m_piPlaneOffsets[2] = m_piPlaneOffsets[3] = 0;
    
    m_piPlaneStrides[0] = CVPixelBufferGetBytesPerRow(pixelBuffer);
    m_piPlaneStrides[1] = m_piPlaneStrides[2] = m_piPlaneStrides[3] = 0;
    
    m_dTime = frameTime;
    m_ulSize = m_piPlaneStrides[0] * m_iEncodedHeight;
    
    // We MUST lock the base address during the lifetime of this object
    // or else we could cause a crash
    CVReturn cr = CVPixelBufferLockBaseAddress(pixelBuffer, 0);
    if (kCVReturnSuccess != cr) {
        throw "CVVideoFrame: Unable to lock PixelBuffer base address";
    }
    m_pvData = CVPixelBufferGetBaseAddress(pixelBuffer);
    
    m_ulFrameNumber = frameNumber;
    m_FrameDirty = false;
}

CVVideoFrame::~CVVideoFrame()
{
    Dispose();
}

void CVVideoFrame::Dispose()
{
    if (pixelBuffer) {
        CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
        CVPixelBufferRelease(pixelBuffer);
        pixelBuffer = 0;
    }
}

CVideoFrame *CVVideoFrame::ConvertToFormat(FrameType type)
{
    if(YCbCr_422 == m_typeFrame && BGRA_PRE == type) { 
        CVPixelBufferRef destPixelBuffer = NULL;
        if(kCVReturnSuccess == CVPixelBufferCreate(NULL, m_iEncodedWidth, m_iEncodedHeight,
                                                   k32ARGBPixelFormat, NULL, &destPixelBuffer)) {
            if(kCVReturnSuccess == CVPixelBufferLockBaseAddress(destPixelBuffer, 0)) {
                uint8_t* bgra = (uint8_t*)CVPixelBufferGetBaseAddress(destPixelBuffer);
                int32_t bgraStride = (int32_t)CVPixelBufferGetBytesPerRow(destPixelBuffer);
                uint8_t* srcData = (uint8_t*)m_pvData + m_piPlaneOffsets[0];
                
                if(0 == ColorConvert_YCbCr422p_to_BGRA32_no_alpha(bgra,
                                                                  bgraStride,
                                                                  m_iEncodedWidth,
                                                                  m_iEncodedHeight,
                                                                  srcData + 1,
                                                                  srcData + 2,
                                                                  srcData,
                                                                  m_piPlaneStrides[0],
                                                                  m_piPlaneStrides[0])) {
                    return new CVVideoFrame(destPixelBuffer, m_dTime, frameHostTime, m_ulFrameNumber);
                }
            }
        }
    }

    return NULL;
}
