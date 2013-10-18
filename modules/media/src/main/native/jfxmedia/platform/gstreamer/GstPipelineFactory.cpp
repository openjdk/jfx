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
#include "GstPipelineFactory.h"

#include "GstAudioPlaybackPipeline.h"
#include "GstAVPlaybackPipeline.h"

#include <string>
#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <MediaManagement/MediaTypes.h>
#include <Locator/LocatorStream.h>
#include <jfxmedia_errors.h>
#include <gst/gstelement.h>
#include <Utils/LowLevelPerf.h>
#include <algorithm>
#if ENABLE_VIDEOCONVERT
#include <gst/app/gstappsink.h>
#endif

// From HLSConnectionHolder.java
#define HLS_PROP_GET_HLS_MODE   2
#define HLS_PROP_GET_MIMETYPE   3
#define HLS_VALUE_MIMETYPE_MP2T 1
#define HLS_VALUE_MIMETYPE_MP3  2


//*************************************************************************************************
//********** class CGstPipelineFactory
//*************************************************************************************************

CGstPipelineFactory::CGstPipelineFactory()
{
    m_ContentTypes.push_back(CONTENT_TYPE_AIFF);
    m_ContentTypes.push_back(CONTENT_TYPE_MP3);
    m_ContentTypes.push_back(CONTENT_TYPE_MPA);
    m_ContentTypes.push_back(CONTENT_TYPE_WAV);
    m_ContentTypes.push_back(CONTENT_TYPE_JFX);
    m_ContentTypes.push_back(CONTENT_TYPE_FLV);
    m_ContentTypes.push_back(CONTENT_TYPE_FXM);
    m_ContentTypes.push_back(CONTENT_TYPE_MP4);
    m_ContentTypes.push_back(CONTENT_TYPE_M4A);
    m_ContentTypes.push_back(CONTENT_TYPE_M4V);
    m_ContentTypes.push_back(CONTENT_TYPE_M3U8);
    m_ContentTypes.push_back(CONTENT_TYPE_M3U);
}

// Here we can only delete local resources not dependent on other libraries such as GStreamer
// because the destructor is called after the main exits and we possible don't have access
// to library functions or the are incorrect.
CGstPipelineFactory::~CGstPipelineFactory()
{}

bool CGstPipelineFactory::CanPlayContentType(string contentType)
{
    return find(m_ContentTypes.begin(), m_ContentTypes.end(), contentType) != m_ContentTypes.end();
}

const ContentTypesList& CGstPipelineFactory::GetSupportedContentTypes()
{
    return m_ContentTypes;
}

uint32_t CGstPipelineFactory::CreatePlayerPipeline(CLocator* locator, CPipelineOptions *pOptions, CPipeline** ppPipeline)
{
    LOWLEVELPERF_EXECTIMESTART("CGstPipelineFactory::CreatePlayerPipeline()");

    if (NULL == locator)
        return ERROR_LOCATOR_NULL;

    GstElement* pSource;
    uint32_t    uRetCode = CreateSourceElement(locator, &pSource, pOptions);
    if (ERROR_NONE != uRetCode)
        return uRetCode;

    if (locator->GetContentType().empty())
        return ERROR_LOCATOR_CONTENT_TYPE_NULL;

    //***** Initialize the return pipeline
    *ppPipeline = NULL;

    if (CONTENT_TYPE_JFX == locator->GetContentType() ||
        CONTENT_TYPE_FLV == locator->GetContentType() ||
        CONTENT_TYPE_FXM == locator->GetContentType() ||
        CONTENT_TYPE_MP4 == locator->GetContentType() ||
        CONTENT_TYPE_M4A == locator->GetContentType() ||
        CONTENT_TYPE_M4V == locator->GetContentType())
    {
        GstElement* pVideoSink = NULL;
#if ENABLE_APP_SINK && !ENABLE_NATIVE_SINK
        pVideoSink = CreateElement("appsink");
        if (NULL == pVideoSink)
            return ERROR_GSTREAMER_VIDEO_SINK_CREATE;
#endif // !(ENABLE_APP_SINK && !ENABLE_NATIVE_SINK)

        if (CONTENT_TYPE_JFX == locator->GetContentType() ||
            CONTENT_TYPE_FLV == locator->GetContentType() ||
            CONTENT_TYPE_FXM == locator->GetContentType())
        {
            uRetCode = CreateFLVPipeline(pSource, pVideoSink, (CPipelineOptions*) pOptions, ppPipeline);
            if (ERROR_NONE != uRetCode)
                return uRetCode;
        } else if (CONTENT_TYPE_MP4 == locator->GetContentType() ||
                   CONTENT_TYPE_M4A == locator->GetContentType() ||
                   CONTENT_TYPE_M4V == locator->GetContentType())
        {
            uRetCode = CreateMP4Pipeline(pSource, pVideoSink, (CPipelineOptions*) pOptions, ppPipeline);
            if (ERROR_NONE != uRetCode)
                return uRetCode;
        }
    }
    else if (CONTENT_TYPE_MPA == locator->GetContentType() ||
             CONTENT_TYPE_MP3 == locator->GetContentType())
    {
        uRetCode = CreateMp3AudioPipeline(pSource, pOptions, ppPipeline);
        if (ERROR_NONE != uRetCode)
            return uRetCode;
    }
    else if (CONTENT_TYPE_WAV == locator->GetContentType())
    {
        uRetCode = CreateWavPcmAudioPipeline(pSource, pOptions, ppPipeline);
        if (ERROR_NONE != uRetCode)
            return uRetCode;
    }
    else if (CONTENT_TYPE_AIFF == locator->GetContentType())
    {
        uRetCode = CreateAiffPcmAudioPipeline(pSource, pOptions, ppPipeline);
        if (ERROR_NONE != uRetCode)
            return uRetCode;
    }
    else if (CONTENT_TYPE_M3U8 == locator->GetContentType() ||
             CONTENT_TYPE_M3U == locator->GetContentType())
    {
        GstElement* pVideoSink = NULL;
#if ENABLE_APP_SINK && !ENABLE_NATIVE_SINK
        pVideoSink = CreateElement("appsink");
        if (NULL == pVideoSink)
            return ERROR_GSTREAMER_VIDEO_SINK_CREATE;
#endif // !(ENABLE_APP_SINK && !ENABLE_NATIVE_SINK)

        uRetCode = CreateHLSPipeline(pSource, pVideoSink, pOptions, ppPipeline);
        if (ERROR_NONE != uRetCode)
            return uRetCode;
    }
    else
    {
        return ERROR_LOCATOR_UNSUPPORTED_MEDIA_FORMAT;
    }

    if (NULL == *ppPipeline)
        uRetCode = ERROR_PIPELINE_CREATION;

    LOWLEVELPERF_EXECTIMESTOP("CGstPipelineFactory::CreatePlayerPipeline()");

    return uRetCode;
}

/**
  * GstElement* CreateSourceElement(char* uri)
  *
  * @param   locator   Locator of the source media.
  * @param   ppElement Pointer to address of source element.
  * @return  An error code.
  */
uint32_t CGstPipelineFactory::CreateSourceElement(CLocator* locator, GstElement** ppElement, CPipelineOptions *pOptions)
{
    GstElement *source = NULL;

#if ! ENABLE_NATIVE_SOURCE
    switch (locator->GetType())
    {
        case CLocator::kStreamLocatorType:
        {
            CLocatorStream* streamLocator = (CLocatorStream*)locator;
            CStreamCallbacks *callbacks = streamLocator->GetCallbacks();
            
#if TARGET_OS_MAC
            if ((CONTENT_TYPE_M3U8 == locator->GetContentType() || CONTENT_TYPE_M3U == locator->GetContentType()) && callbacks->Property(HLS_PROP_GET_MIMETYPE, 0) != HLS_VALUE_MIMETYPE_MP3)
            {
                callbacks->CloseConnection();
                delete callbacks;
                delete pOptions;
                return ERROR_PLATFORM_UNSUPPORTED;
            }
#endif // TARGET_OS_MAC

            GstElement *javaSource = CreateElement ("javasource");
            if (NULL == javaSource)
                return ERROR_GSTREAMER_ELEMENT_CREATE;

            bool isRandomAccess = callbacks->IsRandomAccess();
            int hlsMode = callbacks->Property(HLS_PROP_GET_HLS_MODE, 0);
            int streamMimeType = callbacks->Property(HLS_PROP_GET_MIMETYPE, 0);
            pOptions->SetHLSModeEnabled(hlsMode == 1);
            pOptions->SetStreamMimeType(streamMimeType);

            g_signal_connect (javaSource, "read-next-block", G_CALLBACK (SourceReadNextBlock), callbacks);
            g_signal_connect (javaSource, "copy-block", G_CALLBACK (SourceCopyBlock), callbacks);
            g_signal_connect (javaSource, "seek-data", G_CALLBACK (SourceSeekData), callbacks);
            g_signal_connect (javaSource, "close-connection", G_CALLBACK (SourceCloseConnection), callbacks);
            g_signal_connect (javaSource, "property", G_CALLBACK (SourceProperty), callbacks);
            g_signal_connect (javaSource, "get-stream-size", G_CALLBACK (SourceGetStreamSize), callbacks);

            if (isRandomAccess)
                g_signal_connect (javaSource, "read-block", G_CALLBACK (SourceReadBlock), callbacks);

            if (hlsMode == 1)
                g_object_set (javaSource, "hls-mode", TRUE, NULL);

            if (streamMimeType == HLS_VALUE_MIMETYPE_MP2T)
                g_object_set (javaSource, "mimetype", CONTENT_TYPE_MP2T, NULL);
            else if (streamMimeType == HLS_VALUE_MIMETYPE_MP3)
                g_object_set (javaSource, "mimetype", CONTENT_TYPE_MPA, NULL);

            g_object_set (javaSource,
                "size", (gint64)locator->GetSizeHint(),
                "is-seekable", (gboolean)callbacks->IsSeekable(),
                "is-random-access", (gboolean)isRandomAccess,
                "location", locator->GetLocation().c_str(),
                NULL);

            bool needBuffer = callbacks->NeedBuffer();
            pOptions->SetBufferingEnabled(needBuffer);

            if (needBuffer)
            {
                g_object_set (javaSource, "stop-on-pause", FALSE, NULL);
                source = gst_bin_new(NULL);
                if (NULL == source)
                    return ERROR_GSTREAMER_BIN_CREATE;

                GstElement *buffer = NULL;
                if (hlsMode == 1)
                    buffer = CreateElement ("hlsprogressbuffer");
                else
                    buffer = CreateElement ("progressbuffer");

                if (NULL == buffer)
                    return ERROR_GSTREAMER_ELEMENT_CREATE;

                gst_bin_add_many(GST_BIN(source), javaSource, buffer, NULL);

                if (!gst_element_link(javaSource, buffer))
                    return ERROR_GSTREAMER_ELEMENT_LINK;
            }
            else
                source = javaSource;
        }
        break;

        default:
            return ERROR_LOCATOR_UNSUPPORTED_TYPE;
        break;
    }
#else // ENABLE_NATIVE_SOURCE
    const gchar* location = locator->GetLocation().c_str();
    if(g_str_has_prefix(location, "file"))
    {
        source = CreateElement("filesrc");
        if (NULL == source)
            return ERROR_GSTREAMER_ELEMENT_CREATE;
        g_object_set (source, "location", location + 7, NULL);
    } else { // assume HTTP
        source = CreateElement("souphttpsrc");
        if (NULL == source)
            return ERROR_GSTREAMER_ELEMENT_CREATE;
        g_object_set (source, "location", location, NULL);
    }
#endif // ENABLE_NATIVE_SOURCE

    *ppElement = source;

    return ERROR_NONE;
}

gint CGstPipelineFactory::SourceReadNextBlock(GstElement *src, gpointer data)
{
    return ((CStreamCallbacks*)data)->ReadNextBlock();
}

gint CGstPipelineFactory::SourceReadBlock(GstElement *src, guint64 position, guint size, gpointer data)
{
    return ((CStreamCallbacks*)data)->ReadBlock(position, size);
}

void CGstPipelineFactory::SourceCopyBlock(GstElement *src, gpointer buffer, int size, gpointer data)
{
    ((CStreamCallbacks*)data)->CopyBlock(buffer, size);
}

gint64 CGstPipelineFactory::SourceSeekData(GstElement *src, guint64 offset, gpointer data)
{
    return (gint64)((CStreamCallbacks*)data)->Seek((int64_t)offset);
}

int CGstPipelineFactory::SourceProperty(GstElement *src, int prop, int value, gpointer data)
{
    return ((CStreamCallbacks*)data)->Property(prop, value);
}

int CGstPipelineFactory::SourceGetStreamSize(GstElement *src, gpointer data)
{
    return ((CStreamCallbacks*)data)->GetStreamSize();
}

void CGstPipelineFactory::SourceCloseConnection(GstElement *src, gpointer data)
{
    CStreamCallbacks* callbacks = (CStreamCallbacks*)data;
    callbacks->CloseConnection();
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceReadNextBlock), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceReadBlock), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceCopyBlock), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceSeekData), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceCloseConnection), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceProperty), callbacks);
    g_signal_handlers_disconnect_by_func (src, (void*)G_CALLBACK (SourceGetStreamSize), callbacks);
    delete callbacks;
}

/**
    * GstElement* CreateAudioSinkElement(char* name)
    *
    * @param   name    The name to assign to the audio sink element.
    * @return  The audio sink element.
    */
GstElement* CGstPipelineFactory::CreateAudioSinkElement()
{
#if TARGET_OS_WIN32
    return CreateElement("directsoundsink");
#elif  TARGET_OS_MAC
    return CreateElement("osxaudiosink");
#elif  TARGET_OS_LINUX
    return CreateElement("alsasink");
#else
    return NULL;
#endif
}

void CGstPipelineFactory::OnBufferPadAdded(GstElement* element, GstPad* pad, GstElement* peer)
{
    uint32_t uErrorCode = ERROR_NONE;

    GstElement* source_bin = GST_ELEMENT_PARENT(element);
    GstElement* pipeline = GST_ELEMENT_PARENT(source_bin);

    GstPad *src_pad = gst_ghost_pad_new("src", pad);
    if (NULL == src_pad)
        uErrorCode = ERROR_GSTREAMER_CREATE_GHOST_PAD;

    if (ERROR_NONE == uErrorCode)
    {
        if (!gst_pad_set_active(src_pad, TRUE) || !gst_element_add_pad(source_bin, src_pad))
            uErrorCode = ERROR_GSTREAMER_ELEMENT_ADD_PAD;

        if (ERROR_NONE == uErrorCode)
        {
            if (!gst_bin_add(GST_BIN(pipeline), peer))
                uErrorCode = ERROR_GSTREAMER_BIN_ADD_ELEMENT;

            if (ERROR_NONE == uErrorCode)
            {
                if (GST_STATE_CHANGE_FAILURE == gst_element_set_state(peer, GST_STATE_READY))
                    uErrorCode = ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;

                if (ERROR_NONE == uErrorCode)
                {
                    if (!gst_element_link(source_bin, peer))
                        uErrorCode = ERROR_GSTREAMER_ELEMENT_LINK;

                    if (ERROR_NONE == uErrorCode)
                        if (!gst_element_sync_state_with_parent(peer))
                            uErrorCode = ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;
                }
            }
        }
    }

    if (ERROR_NONE != uErrorCode)
    {
        GstBus* bus = gst_pipeline_get_bus(GST_PIPELINE (pipeline));
        GError* error = g_error_new (0, uErrorCode, "%s",
                                     "Error in CGstPipelineFactory::OnBufferPadAdded().");
        GstMessage* message = gst_message_new_error (GST_OBJECT (pipeline), error,
                                                     "Error in CGstPipelineFactory::OnBufferPadAdded().");
        gst_bus_post (bus, message);
        gst_object_unref (bus);
    }

    g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(OnBufferPadAdded), peer);
}

uint32_t CGstPipelineFactory::AttachToSource(GstBin* bin, GstElement* source, GstElement* element)
{
    // Look for progressbuffer element in the source
    GstElement* buffer = GetByFactoryName(source, "progressbuffer");
    if (buffer)
    {
#if ENABLE_BREAK_MY_DATA
        GstElement* dataBreaker = CreateElement ("breakmydata");
        g_object_set (G_OBJECT (dataBreaker), "skip", BREAK_MY_DATA_SKIP, "probability", BREAK_MY_DATA_PROBABILITY, NULL);
        if (!gst_bin_add (bin, dataBreaker))
            return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
        if (!gst_element_link(dataBreaker, element))
            return ERROR_GSTREAMER_ELEMENT_LINK;
        g_signal_connect (buffer, "pad-added", G_CALLBACK (OnBufferPadAdded), dataBreaker);
#else
        g_signal_connect (buffer, "pad-added", G_CALLBACK (OnBufferPadAdded), element);
#endif
        gst_object_unref(buffer);
        return ERROR_NONE;
    }

    // Source does not contain "progressbuffer".
    if (!gst_bin_add(bin, element))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;

#if ENABLE_BREAK_MY_DATA
    GstElement* dataBreaker = CreateElement ("breakmydata");
    g_object_set (G_OBJECT (dataBreaker), "skip", BREAK_MY_DATA_SKIP, "probability", BREAK_MY_DATA_PROBABILITY, NULL);
    gst_bin_add (GST_BIN (pipeline), dataBreaker, NULL);
    gst_element_link_many(source, dataBreaker, element);
#else

    // Create src pad on source bin if we have hlsprogressbuffer
    buffer = GetByFactoryName(source, "hlsprogressbuffer");
    if (buffer)
    {
        GstPad* src_pad = gst_element_get_static_pad(buffer, "src");
        if (NULL == src_pad)
            return ERROR_GSTREAMER_ELEMENT_GET_PAD;

        GstPad* ghost_pad = gst_ghost_pad_new("src", src_pad);
        if (NULL == ghost_pad)
        {
            gst_object_unref(src_pad);
            return ERROR_GSTREAMER_CREATE_GHOST_PAD;
        }

        if (!gst_element_add_pad(source, ghost_pad))
        {
            gst_object_unref(src_pad);
            return ERROR_GSTREAMER_ELEMENT_ADD_PAD;
        }

        gst_object_unref(src_pad);

        gst_object_unref(buffer);
    }

    if (!gst_element_link(source, element))
        return ERROR_GSTREAMER_ELEMENT_LINK;
#endif

    return ERROR_NONE;
}

GstFlowReturn CGstPipelineFactory::AVSinkAllocAlignedBuffer(GstPad *pad, guint64 offset, guint size, GstCaps *caps, GstBuffer **buf)
{
    // allocate a new GstBuffer of the given size plus some for padding and alignment
    GstBuffer *newBuffer = NULL;
    guint8 *newData;
    guint8 *alignedData;
    guint alignedSize;

    // Don't fail catastrophically...
    *buf = NULL;

    // allocate a buffer large enough to accommodate 16 byte alignment
    alignedSize = size;
    size += 16;
    newData = (guint8*)g_try_malloc(size);
    if (NULL == newData) {
        return GST_FLOW_ERROR;
    }

    // create empty GstBuffer
    newBuffer = gst_buffer_new();
    if (NULL == newBuffer) {
        g_free(newData);
        return GST_FLOW_ERROR;
    }

    // Now set data, size and mallocdata
    alignedData = (guint8*)(((intptr_t)newData + 15) & ~15);
    gst_buffer_set_data(newBuffer, alignedData, alignedSize);
    GST_BUFFER_MALLOCDATA(newBuffer) = newData;
    GST_BUFFER_OFFSET(newBuffer) = offset;
    gst_buffer_set_caps(newBuffer, caps);
    *buf = newBuffer;

    return GST_FLOW_OK;
}

/**
    *  GstElement* CreateFLVPipeline(GstElement* source, char* demux_factory,
    *                              char* audiodec_factory, char* videodec_factory,
    *                              GstElement* audiosink, GstElement* videosink)
    *
    *  @param  source              Pipeline source element; must not be NULL.
    *  @param  demux_factory       Name of the demuxer factory.
    *  @param  audiodec_factory    Name of the audio decoder factory.
    *  @param  videodec_factory    Name of the video decoder factory.
    *  @param  audiosink           The audio sink element; if NULL one will be created internally.
    *  @param  videosink           The video sink element; if NULL one will be created internally.
    *
    *  @return An audio-visual playback pipeline for FLV playback.
    */
uint32_t CGstPipelineFactory::CreateFLVPipeline(GstElement* source, GstElement* pVideoSink,
                                                CPipelineOptions* pOptions, CPipeline** ppPipeline)
{
#if TARGET_OS_WIN32
    return CreateAVPipeline(source, "flvdemux", "dshowwrapper", false, "vp6decoder", pVideoSink,
                            pOptions, ppPipeline);
#elif TARGET_OS_MAC
    return CreateAVPipeline(source, "flvdemux", "audioconverter", false, "vp6decoder", pVideoSink,
                            pOptions, ppPipeline);
#elif TARGET_OS_LINUX
#if ENABLE_GST_FFMPEG
    return CreateAVPipeline(source, "flvdemux", "ffdec_mp3", true,
                            "ffdec_vp6f", pVideoSink, pOptions, ppPipeline);
#else
    return CreateAVPipeline(source, "flvdemux", "avaudiodecoder", false, "vp6decoder", pVideoSink,
                            pOptions, ppPipeline);
#endif // ENABLE_GST_FFMPEG
#else
    return ERROR_PLATFORM_UNSUPPORTED;
#endif // TARGET_OS_WIN32
}

/**
    *  GstElement* CreateMP4Pipeline(GstElement* source, char* demux_factory,
    *                              char* audiodec_factory, char* videodec_factory,
    *                              GstElement* audiosink, GstElement* videosink)
    *
    *  @param  source              Pipeline source element; must not be NULL.
    *  @param  demux_factory       Name of the demuxer factory.
    *  @param  audiodec_factory    Name of the audio decoder factory.
    *  @param  videodec_factory    Name of the video decoder factory.
    *  @param  audiosink           The audio sink element; if NULL one will be created internally.
    *  @param  videosink           The video sink element; if NULL one will be created internally.
    *
    *  @return An audio-visual playback pipeline for MP4 playback.
    */
uint32_t CGstPipelineFactory::CreateMP4Pipeline(GstElement* source, GstElement* pVideoSink,
                                                CPipelineOptions* pOptions, CPipeline** ppPipeline)
{
#if TARGET_OS_WIN32
    return CreateAVPipeline(source, "qtdemux", "dshowwrapper", true, "dshowwrapper", pVideoSink, pOptions, ppPipeline);
#elif TARGET_OS_MAC
    return CreateAVPipeline(source, "qtdemux", "audioconverter", false, "avcdecoder", pVideoSink, pOptions, ppPipeline);
#elif TARGET_OS_LINUX
#if ENABLE_GST_FFMPEG
    return CreateAVPipeline(source, "qtdemux", "ffdec_aac", true,
                            "ffdec_h264", pVideoSink, pOptions, ppPipeline);
#else // ENABLE_GST_FFMPEG
    return CreateAVPipeline(source, "qtdemux", "avaudiodecoder", false, "avvideodecoder", pVideoSink, pOptions, ppPipeline);
#endif // ENABLE_GST_FFMPEG
#else
    return ERROR_PLATFORM_UNSUPPORTED;
#endif // TARGET_OS_WIN32
}

/**
    *  GstElement* CreateMp3AudioPipeline(GstElement* source, char* audiodec_factory,
    *                                 char* audiosink)
    *
    *  @param  source              Pipeline source element; must not be NULL.
    *  @param  audiosink           The audio sink element; if NULL one will be created internally.
    *
    *  @return An audio playback pipeline.
    */

uint32_t CGstPipelineFactory::CreateMp3AudioPipeline(GstElement* source, CPipelineOptions *pOptions, CPipeline** ppPipeline)
{
#if TARGET_OS_WIN32
    return CreateAudioPipeline(source, "mpegaudioparse", "dshowwrapper", false, pOptions, ppPipeline);
#elif TARGET_OS_MAC
    return CreateAudioPipeline(source, "mpegaudioparse", "audioconverter", false, pOptions, ppPipeline);
#elif TARGET_OS_LINUX
#if ENABLE_GST_FFMPEG
    return CreateAudioPipeline(source, "mpegaudioparse", "ffdec_mp3", true,
                               pOptions, ppPipeline);
#else // ENABLE_GST_FFMPEG
    return CreateAudioPipeline(source, "mpegaudioparse", "avaudiodecoder", false, pOptions, ppPipeline);
#endif // ENABLE_GST_FFMPEG
#else
    return ERROR_PLATFORM_UNSUPPORTED;
#endif // TARGET_OS_WIN32
}

uint32_t CGstPipelineFactory::CreateWavPcmAudioPipeline(GstElement* source, CPipelineOptions *pOptions, CPipeline** ppPipeline)
{
    return CreateAudioPipeline(source, "wavparse", NULL, true, pOptions, ppPipeline);
}

uint32_t CGstPipelineFactory::CreateAiffPcmAudioPipeline(GstElement* source, CPipelineOptions *pOptions, CPipeline** ppPipeline)
{
    return CreateAudioPipeline(source, "aiffparse", NULL, true, pOptions, ppPipeline);
}

uint32_t CGstPipelineFactory::CreateHLSPipeline(GstElement* source, GstElement* pVideoSink, CPipelineOptions* pOptions, CPipeline** ppPipeline)
{
#if TARGET_OS_WIN32
    if (pOptions->GetStreamMimeType() == HLS_VALUE_MIMETYPE_MP2T)
        return CreateAVPipeline(source, "dshowwrapper", "dshowwrapper", true, "dshowwrapper", pVideoSink, pOptions, ppPipeline);
    else if (pOptions->GetStreamMimeType() == HLS_VALUE_MIMETYPE_MP3)
        return CreateAudioPipeline(source, "mpegaudioparse", "dshowwrapper", false, pOptions, ppPipeline);
    else
        return ERROR_PLATFORM_UNSUPPORTED;
#elif TARGET_OS_MAC
    if (pOptions->GetStreamMimeType() == HLS_VALUE_MIMETYPE_MP3)
        return CreateAudioPipeline(source, "mpegaudioparse", "audioconverter", false, pOptions, ppPipeline);
    return ERROR_PLATFORM_UNSUPPORTED;
#elif TARGET_OS_LINUX
    if (pOptions->GetStreamMimeType() == HLS_VALUE_MIMETYPE_MP2T)
        return CreateAVPipeline(source, "avmpegtsdemuxer", "avaudiodecoder", false, "avvideodecoder", pVideoSink, pOptions, ppPipeline);
    else if (pOptions->GetStreamMimeType() == HLS_VALUE_MIMETYPE_MP3)
        return CreateAudioPipeline(source, "mpegaudioparse", "avaudiodecoder", false, pOptions, ppPipeline);
    else
        return ERROR_PLATFORM_UNSUPPORTED;
#else
    return ERROR_PLATFORM_UNSUPPORTED;
#endif // TARGET_OS_WIN32
}

uint32_t CGstPipelineFactory::CreateAudioPipeline(GstElement* source, const char* strParserName, const char* strDecoderName,
                                                  bool bConvertFormat, CPipelineOptions *pOptions, CPipeline** ppPipeline)
{
    uint32_t uRetCode = ERROR_NONE;

    GstElement *pipeline = gst_pipeline_new (NULL);
    if (NULL == pipeline)
        return ERROR_GSTREAMER_PIPELINE_CREATION;
    if(!gst_bin_add(GST_BIN (pipeline), source))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;

    GstElementContainer elements;
    int flags = 0;
    GstElement* audiobin;
    uRetCode = CreateAudioBin(strParserName, strDecoderName, bConvertFormat, &elements, &flags, &audiobin);
    if (ERROR_NONE != uRetCode)
        return uRetCode;

    uRetCode = AttachToSource(GST_BIN (pipeline), source, audiobin);
    if (ERROR_NONE != uRetCode)
        return uRetCode;

    elements.add(PIPELINE, pipeline).
    add(SOURCE, source);

    *ppPipeline = new CGstAudioPlaybackPipeline(elements, flags, pOptions);
    if (NULL == ppPipeline)
        uRetCode = ERROR_MEMORY_ALLOCATION;

    return uRetCode;
}

/**
 *  GstElement* CreateAVPipeline(GstElement* source, char* demux_factory,
 *                              char* audiodec_factory, char* videodec_factory,
 *                              GstElement* audiosink, GstElement* videosink)
 *
 *  @param  source                Pipeline source element; must not be NULL.
 *  @param  strDemultiplexerName  Name of the demuxer factory.
 *  @param  strAudioDecoderName   Name of the audio decoder factory.
 *  @param  bConvertFormat        Add or not an audioconverter.
 *  @param  strVideoDecoderName   Name of the video decoder factory.
 *  @param  videosink             The video sink element; if NULL one will be created internally.
 *  @param  pOptions              Diffferent pipeline options that come alone during creation process.
 *  @param  ppPipeline            Result.
 *
 *  @return An audio-visual playback pipeline.
 */
uint32_t CGstPipelineFactory::CreateAVPipeline(GstElement* source, const char* strDemultiplexerName,
                                               const char* strAudioDecoderName, bool bConvertFormat, const char* strVideoDecoderName,
                                               GstElement* pVideoSink, CPipelineOptions* pOptions, CPipeline** ppPipeline)
{
    uint32_t uRetCode = ERROR_NONE;

    // Pipeline and demuxer
    GstElement *pipeline = gst_pipeline_new (NULL);
    if (NULL == pipeline)
        return ERROR_GSTREAMER_PIPELINE_CREATION;
    GstElement *demuxer  = CreateElement (strDemultiplexerName);
    if (NULL == demuxer)
        return ERROR_GSTREAMER_ELEMENT_CREATE;
    if (!gst_bin_add (GST_BIN (pipeline), source))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;

    uRetCode= AttachToSource(GST_BIN (pipeline), source, demuxer);
    if (ERROR_NONE != uRetCode)
        return uRetCode;

    GstElementContainer elements;
    int audioFlags = 0;
    GstElement *audiobin;
    uRetCode = CreateAudioBin(NULL, strAudioDecoderName, bConvertFormat,
                              &elements, &audioFlags, &audiobin);
    if (ERROR_NONE != uRetCode)
        return uRetCode;

    GstElement *videobin;
    uRetCode = CreateVideoBin(strVideoDecoderName, pVideoSink, &elements, &videobin);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    elements.add(PIPELINE, pipeline).
    add(SOURCE, source).
    add(AV_DEMUXER, demuxer);

    if (elements[VIDEO_DECODER] != NULL && NULL != g_object_class_find_property(G_OBJECT_GET_CLASS(G_OBJECT(elements[VIDEO_DECODER])), "location") &&
        elements[SOURCE] != NULL && NULL != g_object_class_find_property(G_OBJECT_GET_CLASS(G_OBJECT(elements[SOURCE])), "location"))
    {
        gchar* location = NULL;
        g_object_get(G_OBJECT(elements[SOURCE]), "location", &location, NULL);
        g_object_set(G_OBJECT(elements[VIDEO_DECODER]), "location", location, NULL);
    }

    *ppPipeline = new CGstAVPlaybackPipeline(elements, audioFlags, pOptions);
    if( NULL == *ppPipeline)
        return ERROR_MEMORY_ALLOCATION;

    return uRetCode;
}

uint32_t CGstPipelineFactory::CreateAudioBin(const char* strParserName, const char* strDecoderName,
                                             bool bConvertFormat,
                                             GstElementContainer* elements, int* pFlags,
                                             GstElement** ppAudiobin)
{
    if ((NULL == strParserName && NULL == strDecoderName) || NULL == elements || NULL == pFlags || NULL == ppAudiobin)
        return ERROR_FUNCTION_PARAM_NULL;

    *ppAudiobin = gst_bin_new(NULL);
    if (NULL == *ppAudiobin)
        return ERROR_GSTREAMER_BIN_CREATE;

    GstElement* head = NULL;

    GstElement *audioparse = NULL;
    if (NULL != strParserName)
    {
        audioparse = CreateElement (strParserName);
        if (NULL == audioparse)
            return ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED;
        if(!gst_bin_add(GST_BIN(*ppAudiobin), audioparse))
            return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
        head = audioparse;
    }

    GstElement *audioqueue = CreateElement ("queue");
    if (NULL == audioqueue)
        return ERROR_GSTREAMER_ELEMENT_CREATE;
    if (!gst_bin_add(GST_BIN(*ppAudiobin), audioqueue))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
    if (NULL != audioparse)
    {
        gst_element_link(audioparse, audioqueue);
    }

    GstElement* tail = audioqueue;
    if (NULL == head)
    {
        head = audioqueue;
    }

    GstElement *audiodec = NULL;
    if (NULL != strDecoderName)
    {
        audiodec = CreateElement (strDecoderName);
        if (NULL == audiodec)
            return ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED;

        if (!gst_bin_add(GST_BIN(*ppAudiobin), audiodec))
            return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
        gst_element_link(audioqueue, audiodec);
        tail = audiodec;
    }

    if (bConvertFormat)
    {
        GstElement *audioconv  = CreateElement ("audioconvert");
        if (!gst_bin_add(GST_BIN(*ppAudiobin), audioconv))
            return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
        gst_element_link(tail, audioconv);
        tail = audioconv;
    }

    GstElement *audioequalizer = CreateElement ("equalizer-nbands");
    GstElement *audiospectrum = CreateElement ("spectrum");
    if (NULL == audioequalizer || NULL == audiospectrum)
        return ERROR_GSTREAMER_ELEMENT_CREATE;

    GstElement *audiosink  = CreateAudioSinkElement();
    if (NULL == audiosink)
        return ERROR_GSTREAMER_AUDIO_SINK_CREATE;

    gst_bin_add_many(GST_BIN(*ppAudiobin), audioequalizer, audiospectrum, audiosink, NULL);
#if TARGET_OS_WIN32
    if (!gst_element_link_many (tail, audioequalizer, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_AUDIO_BIN;
    tail = audioequalizer;
#else // TARGET_OS_WIN32
    GstElement *audiobal = CreateElement ("audiopanorama");
    if (!gst_bin_add(GST_BIN(*ppAudiobin), audiobal))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
    if (!gst_element_link_many (tail, audioequalizer, audiobal, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_AUDIO_BIN;
    tail = audiobal;
#endif // TARGET_OS_WIN32


    // Add volume element exclusively for Linux. alsamixer sets the system volume.
    // Audiosinks on other platforms allow setting application only volume level.
#if TARGET_OS_LINUX
    GstElement *volume = CreateElement ("volume");
    if (!gst_bin_add(GST_BIN(*ppAudiobin), volume))
        return ERROR_GSTREAMER_BIN_ADD_ELEMENT;
    if (!gst_element_link_many (tail, volume, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_AUDIO_BIN;
    tail = volume;
#endif

    if (!gst_element_link_many (tail, audiospectrum, audiosink, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_AUDIO_BIN;

    GstPad *sink_pad = gst_element_get_static_pad(head, "sink");
    if (NULL == sink_pad)
        return ERROR_GSTREAMER_ELEMENT_GET_PAD;
    GstPad *ghost_pad = gst_ghost_pad_new("sink", sink_pad);
    if (NULL == ghost_pad)
        return ERROR_GSTREAMER_CREATE_GHOST_PAD;
    gst_element_add_pad(*ppAudiobin, ghost_pad);
    gst_object_unref(sink_pad);

    elements->add(AUDIO_BIN, *ppAudiobin).
        add(AUDIO_QUEUE, audioqueue).
        add(AUDIO_EQUALIZER, audioequalizer).
        add(AUDIO_SPECTRUM, audiospectrum).
#if TARGET_OS_WIN32
        add(AUDIO_BALANCE, audiosink).
#else // TARGET_OS_WIN32
        add(AUDIO_BALANCE, audiobal).
#endif // TARGET_OS_WIN32

#if TARGET_OS_LINUX
        add(AUDIO_VOLUME, volume).
#else // TARGET_OS_LINUX
        add(AUDIO_VOLUME, audiosink).
#endif // TARGET_OS_LINUX

        add(AUDIO_SINK, audiosink);

    if (NULL != audioparse)
        elements->add(AUDIO_PARSER, audioparse);

    if (NULL != audiodec)
    {
        elements->add(AUDIO_DECODER, audiodec);
        *pFlags |= AUDIO_DECODER_HAS_SOURCE_PROBE | AUDIO_DECODER_HAS_SINK_PROBE;
    }

    // Switch off limiting of the audioqueue for bytes and buffers.
    g_object_set(audioqueue, "max-size-bytes", (guint)0, "max-size-buffers", (guint)10, "max-size-time", (guint64)0, NULL);

    return ERROR_NONE;
}

uint32_t CGstPipelineFactory::CreateVideoBin(const char* strDecoderName, GstElement* pVideoSink,
                                             GstElementContainer* elements, GstElement** ppVideobin)
{
    *ppVideobin = gst_bin_new(NULL);
    if (NULL == *ppVideobin)
        return ERROR_GSTREAMER_BIN_CREATE;

    GstElement *videodec   = CreateElement (strDecoderName);
    GstElement *videoqueue = CreateElement ("queue");
    if (NULL == videodec || NULL == videoqueue)
        return ERROR_GSTREAMER_ELEMENT_CREATE;

    if(NULL == pVideoSink)
    {
        pVideoSink = CreateElement ("autovideosink");
        if (NULL == pVideoSink)
            return ERROR_GSTREAMER_VIDEO_SINK_CREATE;
    }

#if ENABLE_NATIVE_SINK || ENABLE_VIDEOCONVERT
    GstElement* videoconv = CreateElement ("ffmpegcolorspace");
    if (NULL == videoconv)
        return ERROR_GSTREAMER_ELEMENT_CREATE;

#if ENABLE_VIDEOCONVERT
    GstCaps* appSinkCaps = gst_caps_new_simple("video/x-raw-rgb",
            "bpp", G_TYPE_INT, 32,
            "depth", G_TYPE_INT, 32,
            "red_mask", G_TYPE_INT, 0x0000FF00,
            "green_mask", G_TYPE_INT, 0x00FF0000,
            "blue_mask", G_TYPE_INT, 0xFF000000,
            "alpha_mask", G_TYPE_INT, 0x000000FF,
            NULL);
    gst_app_sink_set_caps(GST_APP_SINK(pVideoSink), appSinkCaps);
#endif

    gst_bin_add_many (GST_BIN (*ppVideobin), videoqueue, videodec, videoconv, pVideoSink, NULL);
    if(!gst_element_link_many (videoqueue, videodec, videoconv, pVideoSink, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_VIDEO_BIN;
#else
    gst_bin_add_many (GST_BIN (*ppVideobin), videoqueue, videodec, pVideoSink, NULL);
    if(!gst_element_link_many (videoqueue, videodec, pVideoSink, NULL))
        return ERROR_GSTREAMER_ELEMENT_LINK_VIDEO_BIN;
#endif
    // set bufferalloc function on videosink so we get aligned frames
    GstPad* sink_pad = gst_element_get_static_pad(pVideoSink, "sink");
    if (NULL != sink_pad)
    {
        gst_pad_set_bufferalloc_function(sink_pad, CGstPipelineFactory::AVSinkAllocAlignedBuffer);
        gst_object_unref(sink_pad);
    }

    sink_pad = gst_element_get_static_pad(videoqueue, "sink");
    if (NULL == sink_pad)
        return ERROR_GSTREAMER_ELEMENT_GET_PAD;

    GstPad* ghost_pad = gst_ghost_pad_new("sink", sink_pad);
    if (NULL == ghost_pad)
    {
        gst_object_unref(sink_pad);
        return ERROR_GSTREAMER_CREATE_GHOST_PAD;
    }
    if (!gst_element_add_pad(*ppVideobin, ghost_pad))
    {
        gst_object_unref(sink_pad);
        return ERROR_GSTREAMER_ELEMENT_ADD_PAD;
    }
    gst_object_unref(sink_pad);

    elements->add(VIDEO_BIN, *ppVideobin).
    add(VIDEO_QUEUE, videoqueue).
    add(VIDEO_DECODER, videodec).
    add(VIDEO_SINK, pVideoSink);

    // Switch off limiting of the videoqueue for bytes and buffers.
    g_object_set(videoqueue, "max-size-bytes", (guint)0, "max-size-buffers", (guint)10, "max-size-time", (guint64)0, NULL);
    g_object_set(pVideoSink, "qos", TRUE, NULL);

    return ERROR_NONE;
}

GstElement* CGstPipelineFactory::CreateElement(const char* strFactoryName)
{
    return gst_element_factory_make (strFactoryName, NULL);
}

GstElement* CGstPipelineFactory::GetByFactoryName(GstElement* bin, const char* strFactoryName)
{
    if (!GST_IS_BIN(bin))
        return NULL;

    GstIterator *it = gst_bin_iterate_elements(GST_BIN(bin));
    GstElement  *item = NULL;
    gboolean    done = FALSE;
    while (!done)
    {
        switch (gst_iterator_next (it, (gpointer*)&item))
        {
            case GST_ITERATOR_OK:
            {
                GstElementFactory* factory = gst_element_get_factory(item);
                if (g_str_has_prefix(GST_PLUGIN_FEATURE_NAME(factory), strFactoryName))
                    done = TRUE;
                else
                    gst_object_unref (item);
                break;
            }
            case GST_ITERATOR_RESYNC:
                gst_iterator_resync (it);
                break;

            case GST_ITERATOR_ERROR:
            case GST_ITERATOR_DONE:
                item = NULL;
                done = TRUE;
                break;
        }
    }
    gst_iterator_free (it);
    return item;
}
