/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GST_VIDEO_FRAME_H_
#define _GST_VIDEO_FRAME_H_

#include <gst/gst.h>
#include <PipelineManagement/VideoFrame.h>

#define FOURCC_I420 "I420"
#define FOURCC_UYVY "UYVY"

/**
 * class CGstVideoFrame
 *
 * Class representing a GStreamer video frame.  Buffers are only ref counted, not copied.
 */
class CGstVideoFrame : public CVideoFrame
{
public:
    CGstVideoFrame();

    virtual ~CGstVideoFrame();

    /*
     * Initialize a VideoFrame that wraps the given GstBuffer. The frame caps are
     * extracted from the buffer itself.
     */
    bool Init(GstSample* sample);

    virtual void Dispose();

    virtual bool IsValid();

    GstSample *GetGstSample() { return m_pSample; } // sample is NOT referenced on return!

    virtual CVideoFrame *ConvertToFormat(FrameType type);

private:
    void SetFrameCaps(GstCaps *newCaps);

    bool        m_bIsValid;
    bool        m_bHasAlpha;
    GstSample*  m_pSample;
    GstBuffer*  m_pBuffer;
    GstMapInfo  m_Info;
    void*       m_pvBufferBaseAddress;
    unsigned long m_ulBufferSize;
    bool        m_bIsI420;

    CGstVideoFrame *ConvertSwapRGB(FrameType destType);
    CGstVideoFrame *ConvertFromYCbCr420p(FrameType destType);
    CGstVideoFrame *ConvertFromYCbCr422(FrameType destType);
};
#endif  //_GST_VIDEO_FRAME_H_
