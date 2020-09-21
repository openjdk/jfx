/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "GstAVPlaybackPipeline.h"

#include "GstVideoFrame.h"
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <PipelineManagement/VideoTrack.h>
#include <MediaManagement/Media.h>
#include <jni/Logger.h>
#include <Common/VSMemory.h>
#include <Utils/LowLevelPerf.h>

#define MAX_SIZE_BUFFERS_LIMIT 25
#define MAX_SIZE_BUFFERS_INC   5

//*************************************************************************************************
//********** class CGstAVPlaybackPipeline
//*************************************************************************************************

/**
 * CGstAVPlaybackPipeline::CGstAVPlaybackPipeline()
 *
 * Constructor
 *
 * @param   elements    GStreamer container of elements
 * @param   pOptions    options for the pipeline
 */
CGstAVPlaybackPipeline::CGstAVPlaybackPipeline(const GstElementContainer& elements, int audioFlags, CPipelineOptions* pOptions)
:   CGstAudioPlaybackPipeline(elements, audioFlags, pOptions)
{
    LOGGER_LOGMSG(LOGGER_DEBUG, "CGstAVPlaybackPipeline::CGstAVPlaybackPipeline()");
    m_videoDecoderSrcProbeHID = 0L;
    m_EncodedVideoFrameRate = 24.0F;
    m_SendFrameSizeEvent = TRUE;
    m_FrameWidth = 0;
    m_FrameHeight = 0;
    m_videoCodecErrorCode = ERROR_NONE;
    m_bStaticPipeline = false; // For now all video pipelines are dynamic
}

/**
 * CGstAVPlaybackPipeline::~CGstAVPlaybackPipeline()
 *
 * Destructor
 */
CGstAVPlaybackPipeline::~CGstAVPlaybackPipeline()
{
#if JFXMEDIA_DEBUG
    g_print ("CGstAVPlaybackPipeline::~CGstAVPlaybackPipeline()\n");
#endif
    LOGGER_LOGMSG(LOGGER_DEBUG, "CGstAVPlaybackPipeline::~CGstAVPlaybackPipeline()");
}

/**
 * CGstAVPlaybackPipeline::Init()
 *
 * Initialize the pipeline.
 */
uint32_t CGstAVPlaybackPipeline::Init()
{
    g_signal_connect(m_Elements[AV_DEMUXER], "pad-added", G_CALLBACK (on_pad_added), this);
    g_signal_connect(m_Elements[AV_DEMUXER], "no-more-pads", G_CALLBACK (no_more_pads), this);
    g_signal_connect(m_Elements[AUDIO_QUEUE], "overrun", G_CALLBACK (queue_overrun), this);
    g_signal_connect(m_Elements[VIDEO_QUEUE], "overrun", G_CALLBACK (queue_overrun), this);
    g_signal_connect(m_Elements[AUDIO_QUEUE], "underrun", G_CALLBACK (queue_underrun), this);
    g_signal_connect(m_Elements[VIDEO_QUEUE], "underrun", G_CALLBACK (queue_underrun), this);

    return CGstAudioPlaybackPipeline::Init();
}

uint32_t CGstAVPlaybackPipeline::PostBuildInit()
{
    if (m_bHasVideo && !m_bVideoInitDone)
    {
#if ENABLE_APP_SINK && !ENABLE_NATIVE_SINK
        //Tell it to push signals to us in sync mode so that audio and video are sync'd
        g_object_set (G_OBJECT (m_Elements[VIDEO_SINK]), "emit-signals", TRUE, "sync", TRUE, NULL);

        //Connect the callback
        g_signal_connect (m_Elements[VIDEO_SINK], "new-sample", G_CALLBACK (OnAppSinkHaveFrame), this);
        g_signal_connect (m_Elements[VIDEO_SINK], "new-preroll", G_CALLBACK (OnAppSinkPreroll), this);
#endif

        // Add a buffer probe on the sink pad of the decoder to capture frame rate
        GstPad *pPad = gst_element_get_static_pad(m_Elements[VIDEO_DECODER], "src");
        if (NULL == pPad)
            return ERROR_GSTREAMER_VIDEO_DECODER_SINK_PAD;
        m_videoDecoderSrcProbeHID = gst_pad_add_probe(pPad, GST_PAD_PROBE_TYPE_BUFFER, (GstPadProbeCallback)VideoDecoderSrcProbe, this, NULL);
        gst_object_unref(pPad);

        m_bVideoInitDone = true;
    }

    return CGstAudioPlaybackPipeline::PostBuildInit();
}

/**
 * CGstAVPlaybackPipeline::Dispose()
 *
 * Disposes of resources held by this object. The pipeline should not be used
 * once this method has been invoked.
 */
void CGstAVPlaybackPipeline::Dispose()
{
#if JFXMEDIA_DEBUG
    g_print ("CGstAVPlaybackPipeline::Dispose()\n");
#endif

    if (m_bHasVideo && m_bVideoInitDone)
    {
#if ENABLE_APP_SINK && !ENABLE_NATIVE_SINK
        g_signal_handlers_disconnect_by_func(m_Elements[VIDEO_SINK], (void*)G_CALLBACK(OnAppSinkHaveFrame), this);
        g_signal_handlers_disconnect_by_func(m_Elements[VIDEO_SINK], (void*)G_CALLBACK(OnAppSinkPreroll), this);
#endif
    }

    g_signal_handlers_disconnect_by_func(m_Elements[AUDIO_QUEUE], (void*)G_CALLBACK(queue_overrun), this);
    g_signal_handlers_disconnect_by_func(m_Elements[VIDEO_QUEUE], (void*)G_CALLBACK(queue_overrun), this);
    g_signal_handlers_disconnect_by_func(m_Elements[AUDIO_QUEUE], (void*)G_CALLBACK(queue_underrun), this);
    g_signal_handlers_disconnect_by_func(m_Elements[VIDEO_QUEUE], (void*)G_CALLBACK(queue_underrun), this);

    CGstAudioPlaybackPipeline::Dispose();

    if (!m_bHasAudio && m_Elements[AUDIO_BIN] != NULL)
        gst_object_unref(m_Elements[AUDIO_BIN]);

    if (!m_bHasVideo && m_Elements[VIDEO_BIN] != NULL)
        gst_object_unref(m_Elements[VIDEO_BIN]);
}

bool CGstAVPlaybackPipeline::IsCodecSupported(GstCaps *pCaps)
{
#if TARGET_OS_WIN32
    GstStructure *s = NULL;
    const gchar *mimetype = NULL;

    if (pCaps)
    {
        s = gst_caps_get_structure (pCaps, 0);
        if (s != NULL)
        {
            mimetype = gst_structure_get_name (s);
            if (mimetype != NULL)
            {
                if (strstr(mimetype, "video/x-h264") != NULL) // H.264
                {
                    gboolean is_supported = FALSE;
                    g_object_set(m_Elements[VIDEO_DECODER], "codec-id", (gint)CODEC_ID_AVC1, NULL); // Check for AVC1 (MP4). For HLS we should get error early
                    g_object_get(m_Elements[VIDEO_DECODER], "is-supported", &is_supported, NULL);
                    if (is_supported)
                    {
                        return TRUE;
                    }
                    else
                    {
                        m_videoCodecErrorCode = ERROR_MEDIA_H264_FORMAT_UNSUPPORTED;
                        return FALSE;
                    }
                }
            }
        }
    }

    return CGstAudioPlaybackPipeline::IsCodecSupported(pCaps);
#else // TARGET_OS_WIN32
    GstStructure *s = NULL;
    const gchar *mimetype = NULL;

    if (pCaps)
    {
        s = gst_caps_get_structure (pCaps, 0);
        if (s != NULL)
        {
            mimetype = gst_structure_get_name (s);
            if (mimetype != NULL)
            {
                if (strstr(mimetype, "video/unsupported") != NULL)
                {
                    m_videoCodecErrorCode = ERROR_MEDIA_VIDEO_FORMAT_UNSUPPORTED;
                    return FALSE;
                }
            }
        }
    }

    return CGstAudioPlaybackPipeline::IsCodecSupported(pCaps);
#endif // TRAGET_OS_WIN32
}

bool CGstAVPlaybackPipeline::CheckCodecSupport()
{
    if (!m_bHasVideo)
    {
        if (!CGstAudioPlaybackPipeline::CheckCodecSupport())
        {
            if (m_pEventDispatcher && m_videoCodecErrorCode != ERROR_NONE)
            {
                if (!m_pEventDispatcher->SendPlayerMediaErrorEvent(m_videoCodecErrorCode))
                {
                    LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                }

                return FALSE;
            }
        }
    }
    else
    {
        return CGstAudioPlaybackPipeline::CheckCodecSupport();
    }
    return FALSE;
}

/**
 * CGstAVPlaybackPipeline::SetEncodedVideoFrameRate()
 *
 * Sets the encoded video frame rate data member.
 */
void CGstAVPlaybackPipeline::SetEncodedVideoFrameRate(float frameRate)
{
    m_EncodedVideoFrameRate = frameRate;
}

/**
 * CGstAVPlaybackPipeline::OnAppSinkHaveFrame()
 *
 * AppSink callback that receives frames from GStreamer.
 *
 * @param   pElem       GStreamer element that is calling this callback
 * @param   pPipeline   Pointer to this class, passed back as user data
 */
GstFlowReturn CGstAVPlaybackPipeline::OnAppSinkHaveFrame(GstElement* pElem, CGstAVPlaybackPipeline* pPipeline)
{
    LOWLEVELPERF_RESETCOUNTER("FPS");

    //***** get the buffer from appsink
    GstSample* pSample = gst_app_sink_pull_sample(GST_APP_SINK (pElem));
    if (pSample == NULL)
        return GST_FLOW_OK;

    GstBuffer* pBuffer = gst_sample_get_buffer(pSample);
    if (pBuffer == NULL)
    {
        gst_sample_unref(pSample);
        return GST_FLOW_OK;
    }

    if (pPipeline->m_SendFrameSizeEvent || GST_BUFFER_IS_DISCONT(pBuffer))
        OnAppSinkVideoFrameDiscont(pPipeline, pSample);

    //***** Create a VideoFrame object
    CGstVideoFrame* pVideoFrame = new CGstVideoFrame();
    if (!pVideoFrame->Init(pSample))
    {
        gst_sample_unref(pSample);
        delete pVideoFrame;
        return GST_FLOW_OK;
    }

    if (pVideoFrame->IsValid() && pPipeline->m_pEventDispatcher)
    {
        CPlayerEventDispatcher* pEventDispatcher = pPipeline->m_pEventDispatcher;

        // Send new frame which Java will delete later.
        if (!pEventDispatcher->SendNewFrameEvent(pVideoFrame))
        {
            if(!pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_NEW_FRAME_EVENT))
            {
                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
            }
        }
    }
    else
    {
        delete pVideoFrame;
        if (pPipeline->m_pEventDispatcher != NULL) {
            pPipeline->m_pEventDispatcher->Warning(WARNING_GSTREAMER_INVALID_FRAME,
                                                   "Invalid frame");
        }

    }

// INLINE - gst_sample_unref()
    gst_sample_unref (pSample);

    return GST_FLOW_OK;
}

/**
 * CGstAVPlaybackPipeline::OnAppSinkPreroll()
 *
 * Gets some initial information such as the first frame and the height and width.
 *
 * @param   elements    GStreamer container of elements
 */
GstFlowReturn CGstAVPlaybackPipeline::OnAppSinkPreroll(GstElement* pElem, CGstAVPlaybackPipeline* pPipeline)
{
    LOWLEVELPERF_EXECTIMESTOP("nativeInitNativeMediaManagerToVideoPreroll");

    //***** get the buffer from appsink
    GstSample* pSample = gst_app_sink_pull_preroll(GST_APP_SINK (pElem));

    GstBuffer* pBuffer = gst_sample_get_buffer(pSample);
    if (pBuffer == NULL)
    {
        gst_sample_unref(pSample);
        return GST_FLOW_OK;
    }

    if (pPipeline->m_SendFrameSizeEvent || GST_BUFFER_IS_DISCONT(pBuffer))
        OnAppSinkVideoFrameDiscont(pPipeline, pSample);

    // Send frome 0 up to use as poster frame.
    if(pPipeline->m_pEventDispatcher != NULL)
    {
        CGstVideoFrame* pVideoFrame = new CGstVideoFrame();
        if (!pVideoFrame->Init(pSample))
        {
            // INLINE - gst_sample_unref()
            gst_sample_unref (pSample);
            delete pVideoFrame;
            return GST_FLOW_OK;
        }
        if (pVideoFrame->IsValid()) {
            if (!pPipeline->m_pEventDispatcher->SendNewFrameEvent(pVideoFrame))
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_NEW_FRAME_EVENT))
                {
                    LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                }
            }
        } else {
            delete pVideoFrame;
            if (pPipeline->m_pEventDispatcher != NULL) {
                pPipeline->m_pEventDispatcher->Warning(WARNING_GSTREAMER_INVALID_FRAME, "Invalid frame");
            }
        }
    }

// INLINE - gst_sample_unref()
    gst_sample_unref (pSample);

    return GST_FLOW_OK;
}

void CGstAVPlaybackPipeline::OnAppSinkVideoFrameDiscont(CGstAVPlaybackPipeline* pPipeline, GstSample *pSample)
{
    gint width, height;

    GstCaps* caps = gst_sample_get_caps(pSample);
    if (caps == NULL)
        return;

    const GstStructure* str = gst_caps_get_structure(caps, 0);
    if (str == NULL)
        return;

    if (!gst_structure_get_int(str, "width", &width))
    {
        pPipeline->m_pEventDispatcher->Warning (WARNING_GSTREAMER_PIPELINE_FRAME_SIZE, (char*)"width could not be retrieved from preroll GstBuffer");
        width = 0;
    }
    if (!gst_structure_get_int(str, "height", &height))
    {
        pPipeline->m_pEventDispatcher->Warning (WARNING_GSTREAMER_PIPELINE_FRAME_SIZE, (char*)"height could not be retrieved from preroll GstBuffer");
        height = 0;
    }

    if (pPipeline->m_SendFrameSizeEvent || width != pPipeline->m_FrameWidth || height != pPipeline->m_FrameHeight)
    {
        // Save values for possible later use.
        pPipeline->m_FrameWidth = width;
        pPipeline->m_FrameHeight = height;

        if (pPipeline->m_pEventDispatcher != NULL)
        {
            pPipeline->m_SendFrameSizeEvent = !pPipeline->m_pEventDispatcher->SendFrameSizeChangedEvent(pPipeline->m_FrameWidth, pPipeline->m_FrameHeight);
            if (pPipeline->m_SendFrameSizeEvent)
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_FRAME_SIZE_CHANGED_EVENT))
                {
                    LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                }
            }
        }
        else
            pPipeline->m_SendFrameSizeEvent = TRUE;
    }
}

/**
 * CGstAVPlaybackPipeline::CGstAVPlaybackPipeline()
 *
 *
 *
 * @param
 */
void CGstAVPlaybackPipeline::on_pad_added(GstElement *element, GstPad *pad, CGstAVPlaybackPipeline *pPipeline)
{
    pPipeline->m_pBusCallbackContent->m_DisposeLock->Enter();

    if (pPipeline->m_pBusCallbackContent->m_bIsDisposeInProgress)
    {
        pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
        return;
    }

    GstCaps *pCaps = gst_pad_get_current_caps(pad);
    const GstStructure *pStructure = gst_caps_get_structure(pCaps, 0);
    const gchar* pstrName = gst_structure_get_name(pStructure);
    GstPad *pPad = NULL;
    GstPadLinkReturn ret = GST_PAD_LINK_OK;
    GstStateChangeReturn stateRet = GST_STATE_CHANGE_FAILURE;

    if (g_str_has_prefix(pstrName, "audio"))
    {
         // Ignore additional audio tracks if we already have one.
         // Otherwise files with multiple audio track will fail to play, since
         // we will not able to connect second audio track.
         if (pPipeline->m_bHasAudio)
         {
            if (pCaps != NULL)
                gst_caps_unref(pCaps);

            pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
            return;
        }

        if (pPipeline->IsCodecSupported(pCaps))
        {
            pPad = gst_element_get_static_pad(pPipeline->m_Elements[AUDIO_BIN], "sink");
            gst_bin_add(GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[AUDIO_BIN]);
            stateRet = gst_element_set_state(pPipeline->m_Elements[AUDIO_BIN], GST_STATE_READY);
            if (stateRet == GST_STATE_CHANGE_FAILURE)
            {
                gst_object_ref(pPipeline->m_Elements[AUDIO_BIN]);
                gst_bin_remove(GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[AUDIO_BIN]);
                // Remove handles, so we do not receive any more notifications about pads being added or
                // when we done adding new pads. Since we fail to switch bin state we got fatal error and
                // bus callback will move pipeline into GST_STATE_NULL while holding dispose lock and
                // demux (qtdemux) might deadlock since it will call on_pad_added or no_more_pads
                // and these callback will hold dispose lock as well.
                g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(on_pad_added), pPipeline);
                g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(no_more_pads), pPipeline);
                goto Error;
            }
            if (pPad != NULL)
            {
                ret = gst_pad_link (pad, pPad);
                if (ret != GST_PAD_LINK_OK)
                {
                    gst_element_set_state(pPipeline->m_Elements[AUDIO_BIN], GST_STATE_NULL);
                    gst_object_ref(pPipeline->m_Elements[AUDIO_BIN]);
                    gst_bin_remove(GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[AUDIO_BIN]);
                    // We might need to remove callbacks here as well, but it was not necessary before,
                    // so to avoid any regression we will not do it here.
                    goto Error;
                }
            }
            pPipeline->m_bHasAudio = true;
            pPipeline->PostBuildInit();
            gst_element_sync_state_with_parent(pPipeline->m_Elements[AUDIO_BIN]);
        }
    }
    else if (g_str_has_prefix(pstrName, "video"))
    {
        if (pPipeline->IsCodecSupported(pCaps))
        {
            pPad = gst_element_get_static_pad(pPipeline->m_Elements[VIDEO_BIN], "sink");
            gst_bin_add (GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[VIDEO_BIN]);
            stateRet = gst_element_set_state(pPipeline->m_Elements[VIDEO_BIN], GST_STATE_READY);
            if (stateRet == GST_STATE_CHANGE_FAILURE)
            {
                gst_object_ref(pPipeline->m_Elements[VIDEO_BIN]);
                gst_bin_remove(GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[VIDEO_BIN]);
                g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(on_pad_added), pPipeline);
                g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(no_more_pads), pPipeline);
                goto Error;
            }
            if (pPad != NULL)
            {
                ret = gst_pad_link (pad, pPad);
                if (ret != GST_PAD_LINK_OK)
                {
                    gst_element_set_state(pPipeline->m_Elements[VIDEO_BIN], GST_STATE_NULL);
                    gst_object_ref(pPipeline->m_Elements[VIDEO_BIN]);
                    gst_bin_remove(GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[VIDEO_BIN]);
                    goto Error;
                }
            }
            pPipeline->m_bHasVideo = true;
            pPipeline->PostBuildInit();
            gst_element_sync_state_with_parent(pPipeline->m_Elements[VIDEO_BIN]);
        }
    }

Error:
    // Check if we have error set.
    if (ret != GST_PAD_LINK_OK && pPipeline->m_pEventDispatcher != NULL) {
        // Handle special case for GST_PAD_LINK_NOFORMAT, which means format is not supported
        if (ret == GST_PAD_LINK_NOFORMAT) {
            if (g_str_has_prefix(pstrName, "audio"))
            {
                pPipeline->m_audioCodecErrorCode = ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED;
            }
            else if (g_str_has_prefix(pstrName, "video"))
            {
                pPipeline->m_videoCodecErrorCode = ERROR_MEDIA_VIDEO_FORMAT_UNSUPPORTED;
            }
        } else {
            GTimeVal now;
            g_get_current_time (&now);
            if (g_str_has_prefix(pstrName, "audio"))
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent("Failed to link AV parser to audio bin!", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
            else if (g_str_has_prefix(pstrName, "video"))
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent("Failed to link AV parser to video bin!", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
        }
    }

    if (pPad != NULL)
        gst_object_unref(pPad);
    if (pCaps != NULL)
        gst_caps_unref(pCaps);

    pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
}

/**
 * CGstAVPlaybackPipeline::no_more_pads()
 *
 *
 *
 * @param   elements    GStreamer container of elements
 */
void CGstAVPlaybackPipeline::no_more_pads(GstElement *element, CGstAVPlaybackPipeline *pPipeline)
{
    pPipeline->m_pBusCallbackContent->m_DisposeLock->Enter();

    if (pPipeline->m_pBusCallbackContent->m_bIsDisposeInProgress)
    {
        pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
        return;
    }

    g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(on_pad_added), pPipeline);
    g_signal_handlers_disconnect_by_func(element, (void*)G_CALLBACK(no_more_pads), pPipeline);

    pPipeline->CheckCodecSupport();

    if (!pPipeline->m_bHasAudio)
        pPipeline->m_bAudioSinkReady = true;
    if (!pPipeline->m_bHasVideo)
        pPipeline->m_bVideoSinkReady = true;

    pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
}

void CGstAVPlaybackPipeline::CheckQueueSize(GstElement *element)
{
    guint current_level_buffers = 0;
    guint max_size_buffers = 0;

    if (element == NULL)
    {
        g_object_get(m_Elements[VIDEO_QUEUE], "current-level-buffers", &current_level_buffers, "max_size_buffers", &max_size_buffers, NULL);
        if (current_level_buffers >= max_size_buffers)
        {
            element = m_Elements[VIDEO_QUEUE];
        }
        else
        {
            g_object_get(m_Elements[AUDIO_QUEUE], "current-level-buffers", &current_level_buffers, "max_size_buffers", &max_size_buffers, NULL);
            if (current_level_buffers >= max_size_buffers)
                element = m_Elements[AUDIO_QUEUE];
        }

        if (element == NULL)
            return;
    }

    GstState state, pending_state;
    gst_element_get_state(m_Elements[PIPELINE], &state, &pending_state, 0);

    gboolean inc_size_time = FALSE;
    if (IsPlayerState(Unknown) || m_StallOnPause || (state == GST_STATE_PAUSED && pending_state == GST_STATE_PLAYING) || (state == GST_STATE_PLAYING && pending_state == GST_STATE_PAUSED))
    {

        if (m_Elements[AUDIO_QUEUE] == element)
        {
            g_object_get(m_Elements[VIDEO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
            if (current_level_buffers < MAX_SIZE_BUFFERS_LIMIT)
                inc_size_time = TRUE;
        }
        else if (m_Elements[VIDEO_QUEUE] == element)
        {
            g_object_get(m_Elements[AUDIO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
            if (current_level_buffers < MAX_SIZE_BUFFERS_LIMIT)
                inc_size_time = TRUE;
        }
    }
    else if ((state == GST_STATE_PLAYING && pending_state == GST_STATE_VOID_PENDING) || (state == GST_STATE_PAUSED && pending_state == GST_STATE_PLAYING) || (state == GST_STATE_PAUSED && pending_state == GST_STATE_PAUSED))
    {
        // Do not increment queue if we playing and only have one track
        if (!(m_bHasAudio && m_bHasVideo))
            return;

        if (m_Elements[AUDIO_QUEUE] == element)
        {
            g_object_get(m_Elements[VIDEO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
            if (current_level_buffers == 0)
                inc_size_time = TRUE;
        }
        else if (m_Elements[VIDEO_QUEUE] == element)
        {
            g_object_get(m_Elements[AUDIO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
            if (current_level_buffers == 0)
                inc_size_time = TRUE;
        }
    }

    if (inc_size_time)
    {
        g_object_get(element, "max-size-buffers", &max_size_buffers, NULL);
        max_size_buffers += MAX_SIZE_BUFFERS_INC;
        g_object_set(element, "max-size-buffers", max_size_buffers, NULL);
    }
}

void CGstAVPlaybackPipeline::queue_overrun(GstElement *element, CGstAVPlaybackPipeline *pPipeline)
{
    pPipeline->CheckQueueSize(element);
}

void CGstAVPlaybackPipeline::queue_underrun(GstElement *element, CGstAVPlaybackPipeline *pPipeline)
{
    if (pPipeline->m_pOptions->GetHLSModeEnabled())
    {
        if (pPipeline->m_Elements[AUDIO_QUEUE] == element)
        {
            GstStructure *s = gst_structure_new_empty(HLS_PB_MESSAGE_STALL);
            GstMessage *msg = gst_message_new_application(GST_OBJECT(element), s);
            gst_element_post_message(GST_ELEMENT(element), msg);
        }
    }
    else
    {
        gboolean inc_size_time = FALSE;
        guint current_level_buffers = 0;
        guint max_size_buffers = 0;
        GstState state, pending_state;
        GstElement* inc_element = NULL;

        gst_element_get_state(pPipeline->m_Elements[PIPELINE], &state, &pending_state, 0);

        if ((state == GST_STATE_PLAYING && pending_state == GST_STATE_VOID_PENDING) || (state == GST_STATE_PAUSED && pending_state == GST_STATE_PLAYING) || (state == GST_STATE_PAUSED && pending_state == GST_STATE_PAUSED))
        {
            if (pPipeline->m_Elements[AUDIO_QUEUE] == element)
            {
                g_object_get(pPipeline->m_Elements[VIDEO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
                g_object_get(pPipeline->m_Elements[VIDEO_QUEUE], "max_size_buffers", &max_size_buffers, NULL);
                if (current_level_buffers == max_size_buffers)
                {
                    inc_element = pPipeline->m_Elements[VIDEO_QUEUE];
                    inc_size_time = TRUE;
                }
            }
            else if (pPipeline->m_Elements[VIDEO_QUEUE] == element)
            {
                g_object_get(pPipeline->m_Elements[AUDIO_QUEUE], "current-level-buffers", &current_level_buffers, NULL);
                g_object_get(pPipeline->m_Elements[AUDIO_QUEUE], "max_size_buffers", &max_size_buffers, NULL);
                if (current_level_buffers == max_size_buffers)
                {
                    inc_element = pPipeline->m_Elements[AUDIO_QUEUE];
                    inc_size_time = TRUE;
                }
            }
        }

        if (inc_size_time)
        {
            g_object_get(inc_element, "max-size-buffers", &max_size_buffers, NULL);
            max_size_buffers += MAX_SIZE_BUFFERS_INC;
            g_object_set(inc_element, "max-size-buffers", max_size_buffers, NULL);
        }
    }
}

/**
 * CGstAVPlaybackPipeline::VideoDecoderSrcProbe()
 *
 *
 *
 * @param
 */
GstPadProbeReturn CGstAVPlaybackPipeline::VideoDecoderSrcProbe(GstPad* pPad, GstPadProbeInfo *pInfo, CGstAVPlaybackPipeline* pPipeline)
{
    GstPadProbeReturn ret = GST_PAD_PROBE_OK;
    GstCaps *pCaps = NULL;
    GstPad *pSinkPad = NULL;

    if (pPipeline->m_pEventDispatcher)
    {
        GstStructure *pStructure = NULL;
        bool hasAlpha = false;
        gboolean enabled;

        string           strMimeType;
        CTrack::Encoding encoding;
        gint             width    = 0;
        gint             height   = 0;
        gint             fr_num   = 0;
        gint             fr_denom = 1; // We don't want do divide by zero
        gint trackID;

        // Make sure we got requested probe
        if ((pInfo->type & GST_PAD_PROBE_TYPE_BUFFER) != GST_PAD_PROBE_TYPE_BUFFER || pInfo->data == NULL)
            goto exit;

        // Get resolution and framerate from src pad
        if (NULL == (pCaps = gst_pad_get_current_caps(pPad)) ||
            NULL == (pStructure = gst_caps_get_structure(pCaps, 0)))
            goto exit;

        if (!gst_structure_get_int(pStructure, "width", &width) ||
            !gst_structure_get_int(pStructure, "height", &height) ||
            !gst_structure_get_fraction(pStructure, "framerate", &fr_num, &fr_denom) ||
            0 == fr_denom)
                goto exit;

        float frameRate = (float) fr_num / fr_denom;
        pPipeline->SetEncodedVideoFrameRate(frameRate);

        // Get encoding and track ID from sink pad
        if (pCaps != NULL)
            gst_caps_unref(pCaps);

        pSinkPad = gst_element_get_static_pad(pPipeline->m_Elements[VIDEO_DECODER], "sink");
        if (NULL == pSinkPad ||
            NULL == (pCaps = gst_pad_get_current_caps(pSinkPad)) ||
            NULL == (pStructure = gst_caps_get_structure(pCaps, 0)))
        {
            goto exit;
        }

        strMimeType = gst_structure_get_name(pStructure);

        if (strMimeType.find("video/x-h264") != string::npos) {
            encoding = CTrack::H264;
        } else {
            encoding = CTrack::CUSTOM;
        }

        if (!gst_structure_get_boolean(pStructure, "track_enabled", &enabled)) {
            enabled = TRUE; // treat as enabled if field is not present
        }

        if (!gst_structure_get_int(pStructure, "track_id", &trackID)) {
            trackID = 1; // default to 1 for video track, in case container doesn't have track IDs
        }

        // Create the video track.
        CVideoTrack *p_VideoTrack = new CVideoTrack(
            (int64_t)trackID,
            strMimeType,
            encoding,
            (bool)enabled,
            width, height,
            frameRate,
            hasAlpha);

        // Dispatch the track event.
        if (!pPipeline->m_pEventDispatcher->SendVideoTrackEvent(p_VideoTrack))
        {
            if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_VIDEO_TRACK_EVENT))
            {
                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
            }
        }

        delete p_VideoTrack;
    }

    // Unregister the data probe.
    ret = GST_PAD_PROBE_REMOVE;

exit:
    if (pCaps != NULL)
        gst_caps_unref(pCaps);
    if (pSinkPad != NULL)
        gst_object_unref(pSinkPad);

    return ret;
}
