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

#ifndef _GST_AV_PLAYBACK_PIPELINE_H_
#define _GST_AV_PLAYBACK_PIPELINE_H_

#include <Common/ProductFlags.h>
#include <gst/gst.h>
#include <PipelineManagement/PipelineOptions.h>
#include "GstAudioPlaybackPipeline.h"
#include "GstPipelineFactory.h"


/**
 * class CGstAVPlaybackPipeline
 *
 * Class representing a GStreamer audio-video pipeline.
 */
class CGstAVPlaybackPipeline : public CGstAudioPlaybackPipeline
{
    friend class CGstPipelineFactory;

public:
    virtual uint32_t     Init();
    virtual uint32_t     PostBuildInit();
    virtual void         Dispose();

    virtual bool IsCodecSupported(GstCaps *pCaps);
    virtual bool CheckCodecSupport();

    virtual void CheckQueueSize(GstElement *element);

protected:
    CGstAVPlaybackPipeline(const GstElementContainer& elements, int audioFlags, CPipelineOptions* pOptions);
    virtual ~CGstAVPlaybackPipeline();

private:
    static void     on_pad_added(GstElement *element, GstPad *pad, CGstAVPlaybackPipeline* pPipeline);
    static void     no_more_pads(GstElement *element, CGstAVPlaybackPipeline* pPipeline);
    static void     queue_overrun(GstElement *element, CGstAVPlaybackPipeline *pPipeline);
    static void     queue_underrun(GstElement *element, CGstAVPlaybackPipeline *pPipeline);

    static void     OnAppSinkPreroll(GstElement* pElem, CGstAVPlaybackPipeline* pPipeline);
    static void     OnAppSinkHaveFrame(GstElement* pElem, CGstAVPlaybackPipeline* pPipeline);
    static void     OnAppSinkVideoFrameDiscont(CGstAVPlaybackPipeline* pPipeline, GstBuffer *pBuffer);
    static gboolean VideoDecoderSrcProbe(GstPad* pPad, GstBuffer *pBuffer, CGstAVPlaybackPipeline* pPipeline);

    void            SetEncodedVideoFrameRate(float frameRate);
    inline float    GetEncodedVideoFrameRate()
    {
        return m_EncodedVideoFrameRate;
    }

private:
    gboolean                m_SendFrameSizeEvent;
    gint                    m_FrameWidth;
    gint                    m_FrameHeight;
    gulong                  m_videoDecoderSrcProbeHID;
    gfloat                  m_EncodedVideoFrameRate;
    int                     m_videoCodecErrorCode;
};

#endif  //_GST_AV_PLAYBACK_PIPELINE_H_
