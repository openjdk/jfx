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

#ifndef _GST_PIPELINE_FACTORY_H_
#define _GST_PIPELINE_FACTORY_H_

#include <Locator/Locator.h>
#include <PipelineManagement/PipelineFactory.h>
#include <PipelineManagement/PipelineOptions.h>
#include <platform/gstreamer/GstElementContainer.h>
#include <gst/gst.h>

/**
 * class CGstPipelineFactory
 *
 * Class with information on shared libraries/dll to load and pipleine recipes.
 */
class CGstPipelineFactory : public CPipelineFactory
{
public:
    virtual bool CanPlayContentType(string contentType);
    virtual const ContentTypesList& GetSupportedContentTypes();

    uint32_t           CreatePlayerPipeline(CLocator* locator, CPipelineOptions *pOptions, CPipeline** ppPipeline);
    static GstElement* GetByFactoryName(GstElement* bin, const char* strFactoryName);

    virtual ~CGstPipelineFactory();

private:
    friend class CPipelineFactory;

    CGstPipelineFactory();

    GstCaps*    FrameTypeToCaps(CVideoFrame::FrameType format);
    void        NegotiatePixelFormat(GstElement* pVideoSink, CPipelineOptions* pOptions);

    uint32_t    CreateFLVPipeline(GstElement* source, GstElement* videosink, CPipelineOptions* pOptions, CPipeline** ppPipeline);
    uint32_t    CreateMP4Pipeline(GstElement* source, GstElement* videosink, CPipelineOptions* pOptions, CPipeline** ppPipeline);
    uint32_t    CreateMp3AudioPipeline(GstElement* source, CPipelineOptions* pOptions, CPipeline** ppPipeline);
    uint32_t    CreateWavPcmAudioPipeline(GstElement* source, CPipelineOptions* pOptions, CPipeline **ppPipeline);
    uint32_t    CreateAiffPcmAudioPipeline(GstElement* source, CPipelineOptions* pOptions, CPipeline **ppPipeline);
    uint32_t    CreateHLSPipeline(GstElement* source, GstElement* pVideoSink, CPipelineOptions* pOptions, CPipeline** ppPipeline);

    uint32_t    CreateSourceElement(CLocator* locator, GstElement** ppElement, CPipelineOptions *pOptions);
    GstElement* CreateAudioSinkElement();
    uint32_t    AttachToSource(GstBin* bin, GstElement* source, GstElement* demuxer);

    uint32_t    CreateAudioPipeline(GstElement* source,
                                    const char* strParserName, const char* strDecoderName, bool bConvertFormat,
                                    CPipelineOptions *pOptions, CPipeline** ppPipeline);
    uint32_t    CreateAVPipeline(GstElement* source, const char* strDemultiplexerName,
                                 const char* strAudioDecoderName, bool bConvertFormat, const char* strVideoDecoderName,
                                 GstElement* pVideoSink, CPipelineOptions* pOptions, CPipeline** ppPipeline);


    uint32_t    CreateAudioBin(const char* strParserName, const char* strDecoderName, bool bConvertFormat,
                               GstElementContainer* elements, int* pFlags, GstElement** pAudiobin);
    uint32_t    CreateVideoBin(const char* strDecoderName, GstElement* pVideoSink,
                               GstElementContainer* elements, GstElement** ppVideobin);

    GstElement* CreateElement(const char* strFactoryName);

    // progressbuffer on-pad-added
    static void OnBufferPadAdded(GstElement* element, GstPad* pad, GstElement* peer);

    // javasource signals
    static gint     SourceReadNextBlock(GstElement *src, gpointer data);
    static gint     SourceReadBlock(GstElement *src, guint64 position, guint size, gpointer data);
    static void     SourceCopyBlock(GstElement *src, gpointer buffer, int size, gpointer data);
    static gint64   SourceSeekData(GstElement *src, guint64 offset, gpointer data);
    static void     SourceCloseConnection(GstElement *src, gpointer data);
    static int      SourceProperty(GstElement *src, int prop, int value, gpointer data);
    static int      SourceGetStreamSize(GstElement *src, gpointer data);

    static GstFlowReturn AVSinkAllocAlignedBuffer(GstPad *pad, guint64 offset, guint size, GstCaps *caps, GstBuffer **buf);

private:
    ContentTypesList m_ContentTypes;
};

#endif  //_GST_PIPELINE_FACTORY_H_
