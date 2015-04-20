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

#include "GstAudioPlaybackPipeline.h"
#include "GstMediaManager.h"
#include <MediaManagement/MediaTypes.h>
#include <PipelineManagement/AudioTrack.h>
#include <PipelineManagement/PlayerEventDispatcher.h>
#include <MediaManagement/Media.h>
#include <Common/VSMemory.h>
#include <Utils/LowLevelPerf.h>

#define AUDIO_RESUME_DELTA_TIME   10.0 // seconds
#define VIDEO_RESUME_DELTA_TIME   10.0 // seconds
#define STALL_DELTA_TIME           1.0 // seconds

//*************************************************************************************************
//********** class CGstAudioPlaybackPipeline
//*************************************************************************************************

/**
 * CGstAudioPlaybackPipeline::CGstAudioPlaybackPipeline()
 *
 * Constructor
 *
 * @param   elements    GStreamer container of elements
 */
CGstAudioPlaybackPipeline::CGstAudioPlaybackPipeline(const GstElementContainer& elements, int flags, CPipelineOptions* pOptions)
:   CPipeline(pOptions),
    m_Elements(elements),
    m_pAudioEqualizer(NULL),
    m_pAudioSpectrum(NULL),
    m_AudioFlags(flags)
{
    m_dResumeDeltaTime = m_Elements[VIDEO_SINK] ? VIDEO_RESUME_DELTA_TIME : AUDIO_RESUME_DELTA_TIME;

    m_bSeekInvoked = false;
    m_fRate = 1.0F;
    m_audioSourcePadProbeHID = 0L;
    m_ulLastStreamTime = (GstClockTime)0UL;
    m_pBusSource = NULL;
    m_bIgnoreError = FALSE;

    m_StallLock = CJfxCriticalSection::Create();
    m_BufferPosition = 0.0;
    m_bHLSPBFull = false;
    m_StallOnPause = false;

    m_SeekLock = CJfxCriticalSection::Create();
    m_LastSeekTime = -1;

    m_dLastReportedDuration = DURATION_UNKNOWN;

    m_bSetClock = false;
    m_bIsClockSet = false;

    m_StateLock = CJfxCriticalSection::Create();

#if ENABLE_PROGRESS_BUFFER
    m_llLastProgressValueStart = 0;
    m_llLastProgressValuePosition = 0;
    m_llLastProgressValueStop = 0;
    m_bLastProgressValueEOS = 0;
#endif // ENABLE_PROGRESS_BUFFER

    m_audioCodecErrorCode = ERROR_NONE;

    m_pBusCallbackContent = NULL;
}

/**
 * CGstAudioPlaybackPipeline::~CGstAudioPlaybackPipeline()
 *
 * Destructor
 */
CGstAudioPlaybackPipeline::~CGstAudioPlaybackPipeline()
{
#if JFXMEDIA_DEBUG
    g_print ("CGstAudioPlaybackPipeline::~CGstAudioPlaybackPipeline()\n");
#endif
    delete m_SeekLock;
    delete m_StateLock;
    delete m_StallLock;
}

/**
 * CGstAudioPlaybackPipeline::Init()
 *
 * Init an audio-only playback pipeline.  Called by JNI layer.
 */
uint32_t CGstAudioPlaybackPipeline::Init()
{
    bool bStaticDecoderBin = false;

    m_pAudioEqualizer = new (nothrow) CGstAudioEqualizer(m_Elements[AUDIO_EQUALIZER]);
    if (m_pAudioEqualizer == NULL)
        return ERROR_MEMORY_ALLOCATION;

    m_pAudioSpectrum = new (nothrow) CGstAudioSpectrum(m_Elements[AUDIO_SPECTRUM], false);
    if (m_pAudioSpectrum == NULL)
        return ERROR_MEMORY_ALLOCATION;

    if (m_pOptions->GetBufferingEnabled())
        m_bStaticPipeline = false; // Pipeline is dynamic if we have progress buffer

    CMediaManager *pManager = NULL;
    uint32_t ret = CMediaManager::GetInstance(&pManager);
    if (ret != ERROR_NONE)
        return ret;

    m_pBusCallbackContent = new (nothrow) sBusCallbackContent;
    if (m_pBusCallbackContent == NULL)
        return ERROR_MEMORY_ALLOCATION;

    m_pBusCallbackContent->m_pPipeline = this;
    m_pBusCallbackContent->m_DisposeLock = CJfxCriticalSection::Create();
    m_pBusCallbackContent->m_bIsDisposed = false;
    m_pBusCallbackContent->m_bFreeMe = false;

    GstBus *pBus = gst_pipeline_get_bus (GST_PIPELINE (m_Elements[PIPELINE]));
    m_pBusSource = gst_bus_create_watch(pBus);
    g_source_set_callback(m_pBusSource, (GSourceFunc)BusCallback, m_pBusCallbackContent, (GDestroyNotify)BusCallbackDestroyNotify);

    ret = g_source_attach(m_pBusSource, ((CGstMediaManager*)pManager)->m_pMainContext);
    gst_object_unref (pBus);

    if (ret == 0)
    {
        delete m_pBusCallbackContent;
        return ERROR_GSTREAMER_BUS_SOURCE_ATTACH;
    }

    // Check if we have static pipeline
#if TARGET_OS_LINUX | TARGET_OS_MAC | TARGET_OS_WIN32
    if (m_Elements[AV_DEMUXER] == NULL)
        bStaticDecoderBin = true;
#else // TARGET_OS_LINUX | TARGET_OS_MAC | TARGET_OS_WIN32
    if (m_Elements[AUDIO_PARSER] == NULL && m_Elements[AV_DEMUXER] == NULL)
        bStaticDecoderBin = true;
#endif // TARGET_OS_LINUX | TARGET_OS_MAC | TARGET_OS_WIN32

    if (bStaticDecoderBin)
    {
        m_bHasAudio = true;
        PostBuildInit();
    }
    else
    {
        if (m_Elements[AUDIO_PARSER]) // Add method to link parser to decoder.
            g_signal_connect (m_Elements[AUDIO_PARSER], "pad-added", G_CALLBACK (OnParserSrcPadAdded), this);
    }

    // Switch the state
    if (GST_STATE_CHANGE_FAILURE == gst_element_set_state (m_Elements[PIPELINE], GST_STATE_PAUSED))
        return ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;

    return ERROR_NONE;
}

uint32_t CGstAudioPlaybackPipeline::PostBuildInit()
{
    if (m_bHasAudio && !m_bAudioInitDone)
    {
        if (m_Elements[AUDIO_PARSER])
        {
            GstPad *pPad = gst_element_get_static_pad(m_Elements[AUDIO_PARSER], "src");
            if (NULL == pPad)
                return ERROR_GSTREAMER_ELEMENT_GET_PAD;
            m_audioSourcePadProbeHID = gst_pad_add_buffer_probe(pPad, G_CALLBACK(AudioSourcePadProbe), this);
            gst_object_unref(pPad);
        }
        else if (m_Elements[AUDIO_DECODER])
        {
            if (m_AudioFlags & AUDIO_DECODER_HAS_SINK_PROBE) // Add a buffer probe on the sink pad of the decoder
            {
                GstPad *pPad = gst_element_get_static_pad(m_Elements[AUDIO_DECODER], "sink");
                if (NULL == pPad)
                    return ERROR_GSTREAMER_AUDIO_DECODER_SINK_PAD;
                m_audioSinkPadProbeHID = gst_pad_add_buffer_probe(pPad, G_CALLBACK(AudioSinkPadProbe), this);
                gst_object_unref(pPad);
            }

            if (m_AudioFlags & AUDIO_DECODER_HAS_SOURCE_PROBE) // Add a buffer probe on the source pad of the decoder
            {
                GstPad *pPad = gst_element_get_static_pad(m_Elements[AUDIO_DECODER], "src");
                if (NULL == pPad)
                    return ERROR_GSTREAMER_AUDIO_DECODER_SRC_PAD;
                m_audioSourcePadProbeHID = gst_pad_add_buffer_probe(pPad, G_CALLBACK(AudioSourcePadProbe), this);
                gst_object_unref(pPad);
            }
        }

        m_bAudioInitDone = true;
    }

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::OnParserSrcPadAdded()
 *
 * Links the parser source pad to the decoder sink pad and adds a buffer probe to
 * the parser source pad.
 *
 * @param element   The audio parser element.
 * @param pad       The audio parser source pad.
 * @param pPipeline A pointer to the audio pipeline.
 */
void CGstAudioPlaybackPipeline::OnParserSrcPadAdded(GstElement *element, GstPad *pad,
                                                    CGstAudioPlaybackPipeline* pPipeline)
{
    pPipeline->m_pBusCallbackContent->m_DisposeLock->Enter();

    if (pPipeline->m_pBusCallbackContent->m_bIsDisposed)
    {
        pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
        return;
    }

    GstCaps *pCaps = gst_pad_get_caps(pad);

    if (pPipeline->IsCodecSupported(pCaps))
    {
        if (!gst_bin_add (GST_BIN (pPipeline->m_Elements[PIPELINE]), pPipeline->m_Elements[AUDIO_BIN]))
        {
            GTimeVal now;
            g_get_current_time (&now);

            if (NULL != pPipeline->m_pEventDispatcher)
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent ("Failed to add audio bin to pipeline!", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
        }

        gst_element_set_state(pPipeline->m_Elements[AUDIO_BIN], GST_STATE_READY);

        // Get the audio decoder sink pad.
        GstPad *peerPad = gst_element_get_static_pad(pPipeline->m_Elements[AUDIO_BIN], "sink");
        if (NULL == peerPad)
        {
            GTimeVal now;
            g_get_current_time (&now);

            if (NULL != pPipeline->m_pEventDispatcher)
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent ("Failed to retrieve audio bin sink pad!", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
        }

        // Link the audio parser src pad to the audio decode sink pad.
        if (GST_PAD_LINK_OK != gst_pad_link (pad, peerPad))
        {
            GTimeVal now;
            g_get_current_time (&now);

            if (NULL != pPipeline->m_pEventDispatcher)
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent ("Failed to link audio parser with audio bin!\n", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
        }

        if (peerPad != NULL)
        {
            gst_object_unref(peerPad);
            peerPad = NULL;
        }

        pPipeline->m_bHasAudio = true;
        pPipeline->PostBuildInit();

        if (!gst_element_sync_state_with_parent(pPipeline->m_Elements[AUDIO_BIN]))
        {
            GTimeVal now;
            g_get_current_time (&now);

            if (NULL != pPipeline->m_pEventDispatcher)
            {
                if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent ("Failed to start audio bin!\n", (double)GST_TIMEVAL_TO_TIME (now)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
            }
        }
    }

    if (pCaps != NULL)
        gst_caps_unref(pCaps);

    // Disconnect this method from the "pad-added" signal of the audio parser.
    g_signal_handlers_disconnect_by_func(element, (void*)OnParserSrcPadAdded, pPipeline);

    pPipeline->CheckCodecSupport();

    pPipeline->m_pBusCallbackContent->m_DisposeLock->Exit();
}

/**
 * CGstAudioPlaybackPipeline::Dispose()
 *
 * Disposes of resources held by this object. The pipeline should not be used
 * once this method has been invoked.
 */
void CGstAudioPlaybackPipeline::Dispose()
{
#if JFXMEDIA_DEBUG
    g_print ("CGstAudioPlaybackPipeline::Dispose()\n");
#endif

    // Stop pipeline before lock, so all callbacks from pipeline are finished.
    if (m_Elements[PIPELINE])
    {
        gst_element_set_state (m_Elements[PIPELINE], GST_STATE_NULL); // Ignore return value.
    }

    if (m_pBusCallbackContent != NULL)
    {
        m_pBusCallbackContent->m_DisposeLock->Enter();

        if (m_pBusCallbackContent->m_bIsDisposed)
        {
            m_pBusCallbackContent->m_DisposeLock->Exit();
            return;
        }
    }

    if (m_pAudioEqualizer != NULL)
    {
        delete m_pAudioEqualizer;
        m_pAudioEqualizer = NULL;
    }

    if (m_pAudioSpectrum != NULL)
    {
        delete m_pAudioSpectrum;
        m_pAudioSpectrum = NULL;
    }

    // Destroy the pipeline. This should be done after any other cleanup to
    // avert any unexpected contention.
    if (m_Elements[PIPELINE])
    {
        if (m_pBusSource)
        {
            g_source_destroy(m_pBusSource);
            g_source_unref(m_pBusSource);
            m_pBusSource = NULL;
        }

        gst_object_unref (m_Elements[PIPELINE]);
    }
    
    if (m_pBusCallbackContent != NULL)
    {    
        bool bFreeBusCallbackContent = m_pBusCallbackContent->m_bFreeMe;

        m_pBusCallbackContent->m_bIsDisposed = true;

        m_pBusCallbackContent->m_DisposeLock->Exit();

        if (bFreeBusCallbackContent)
        {
            delete m_pBusCallbackContent->m_DisposeLock;
            delete m_pBusCallbackContent;
        }
    }
}

/**
 * CGstAudioPlaybackPipeline::Play()
 *
 * Starts the playback of the media.
 */
uint32_t CGstAudioPlaybackPipeline::Play()
{
    LOWLEVELPERF_EXECTIMESTART("GST_STATE_PLAYING");

    m_StateLock->Enter();
    bool ready = (Finished != m_PlayerState && Error != m_PlayerState && Playing != m_PlayerState);
    if (!ready && Playing == m_PlayerState) // Re-check if we ready with pipeline
    {
        GstState state = GST_STATE_NULL;
        GstState pending = GST_STATE_VOID_PENDING;
        if (gst_element_get_state(m_Elements[PIPELINE], &state, &pending, 0) != GST_STATE_CHANGE_FAILURE)
        {
            if (state == GST_STATE_PAUSED || pending == GST_STATE_PAUSED)
                ready = true;
        }
    }
    m_StateLock->Exit();

    uint32_t ret = ERROR_NONE;
    if (ready)
    {
        if (0.0F == m_fRate)
            // Set playback resumption flag regardless of whether state change succeeds.
            m_bResumePlayOnNonzeroRate = true;
        else if (GST_STATE_CHANGE_FAILURE == gst_element_set_state(m_Elements[PIPELINE], GST_STATE_PLAYING))
               ret = ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;
    }

    return ret;
}

/**
 * CGstAudioPlaybackPipeline::Stop()
 *
 * Stops the playback of the media. It will not reset stream position.
 */
uint32_t CGstAudioPlaybackPipeline::Stop()
{
    if (IsPlayerState(Stopped) || IsPlayerState(Error))
        return ERROR_NONE;

    if (0.0F == m_fRate)
        // Unset playback resumption flag regardless of whether state change succeeds.
        m_bResumePlayOnNonzeroRate = false;
    else
    {
        // Pause playback and seek to beginning of media.
        m_StateLock->Enter();
        m_PlayerPendingState = Stopped;
        m_StateLock->Exit();

        uint32_t uErrCode = InternalPause();
        if (ERROR_NONE != uErrCode)
        {
            m_StateLock->Enter();
            m_PlayerPendingState = Unknown;
            m_StateLock->Exit();
            return uErrCode;
        }
    }

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::Finish()
 *
 * Finishs the playback of the media.
 */
uint32_t CGstAudioPlaybackPipeline::Finish()
{
    uint32_t ret = ERROR_NONE;

    if (IsPlayerState(Finished) || IsPlayerState(Error) || !IsPlayerState(Playing))
        return ERROR_NONE;

    ret = InternalPause();

    return ret;
}

/**
 * CGstAudioPlaybackPipeline::Pause()
 *
 * Pause the playback of the media
 */
uint32_t CGstAudioPlaybackPipeline::Pause()
{
    uint32_t ret = ERROR_NONE;

    if (IsPlayerState(Paused) || IsPlayerState(Error))
        return ERROR_NONE;

    // Check if we really need to pause
    m_StateLock->Enter();
    if (Stopped == m_PlayerState || Stalled == m_PlayerState)
    {
        SetPlayerState(Paused, false);
        m_StateLock->Exit();
        return ERROR_NONE;
    }
    m_PlayerPendingState = Paused;
    m_StateLock->Exit();

    ret = InternalPause();
    if (ret != ERROR_NONE)
    {
        m_StateLock->Enter();
        m_PlayerPendingState = Unknown;
        m_StateLock->Exit();
    }

    return ret;
}

uint32_t CGstAudioPlaybackPipeline::InternalPause()
{
    LOWLEVELPERF_EXECTIMESTART("GST_STATE_PAUSED");

    m_StateLock->Enter();
    bool ready = (((Finished != m_PlayerState || m_bSeekInvoked) || m_PlayerPendingState == Stopped) && Error != m_PlayerState);
    m_bSeekInvoked = false;
    m_StateLock->Exit();

    uint32_t ret = ERROR_NONE;
    // We need to pause if it goes from stop, even if we in Finished state
    if (ready)
    {
        if (0.0F == m_fRate)
            // Unset playback resumption flag regardless of whether state change succeeds.
            m_bResumePlayOnNonzeroRate = false;
        else if (GST_STATE_CHANGE_FAILURE == gst_element_set_state(m_Elements[PIPELINE], GST_STATE_PAUSED))
               ret = ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;
        else
            CheckQueueSize(NULL);
    }

    return ret;
}

uint32_t CGstAudioPlaybackPipeline::SeekPipeline(gint64 seek_time)
{
    GstSeekFlags seekFlags;

    m_SeekLock->Enter();

    m_LastSeekTime = seek_time;

    if (m_fRate < -1.0F || m_fRate > 1.0F)
        seekFlags = (GstSeekFlags)(GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_SKIP);
    else
        seekFlags = (GstSeekFlags)(GST_SEEK_FLAG_FLUSH);// | GST_SEEK_FLAG_KEY_UNIT);

    if (m_Elements[AUDIO_SINK] != NULL && m_bHasAudio && gst_element_seek(m_Elements[AUDIO_SINK], m_fRate, GST_FORMAT_TIME, seekFlags,
        GST_SEEK_TYPE_SET, seek_time,
        GST_SEEK_TYPE_NONE, GST_CLOCK_TIME_NONE))
    {
        m_SeekLock->Exit();
        CheckQueueSize(NULL);
        return ERROR_NONE;
    }
    else if (m_Elements[VIDEO_SINK] != NULL && m_bHasVideo && gst_element_seek(m_Elements[VIDEO_SINK], m_fRate, GST_FORMAT_TIME, seekFlags,
        GST_SEEK_TYPE_SET, seek_time,
        GST_SEEK_TYPE_NONE, GST_CLOCK_TIME_NONE))
    {
        m_SeekLock->Exit();
        CheckQueueSize(NULL);
        return ERROR_NONE;
    }

    m_SeekLock->Exit();

    return ERROR_GSTREAMER_PIPELINE_SEEK;
}

/**
 * CGstAudioPlaybackPipeline::Seek()
 *
 * Seek to a presentation time.
 */
uint32_t CGstAudioPlaybackPipeline::Seek(double dSeekTime)
{
    uint32_t ret = ERROR_NONE;

    m_StateLock->Enter();
    bool notReady = (m_PlayerState != Ready &&
                     m_PlayerState != Playing &&
                     m_PlayerState != Paused &&
                     m_PlayerState != Stopped &&
                     m_PlayerState != Stalled &&
                     m_PlayerState != Finished);

    if (m_PlayerState == Finished)
        m_bSeekInvoked = true;
    m_StateLock->Exit();

    // We should only perform seek in Playing, Paused, Stopped, Stalled or Finished states
    if (notReady)
        return ERROR_NONE;

    ret = SeekPipeline((gint64)(GST_SECOND * dSeekTime));

    // Check if we need to resume pipeline
    m_StateLock->Enter();
    bool resume = (ret == ERROR_NONE && m_PlayerState == Finished && m_PlayerPendingState != Stopped);
    m_StateLock->Exit();

    if (resume)
    {
        if (GST_STATE_CHANGE_FAILURE == gst_element_set_state(m_Elements[PIPELINE], GST_STATE_PLAYING))
            ret = ERROR_GSTREAMER_PIPELINE_STATE_CHANGE;
    }

    return ret;
}

/**
 * CGstAudioPlaybackPipeline::GetDuration()
 *
 * Get the time duration of the media clip.
 *
 * @return  double representing time
 */
uint32_t CGstAudioPlaybackPipeline::GetDuration(double* dDuration)
{
    GstFormat format = GST_FORMAT_TIME;
    gint64    duration = GST_CLOCK_TIME_NONE;

    if (IsPlayerState(Error) || !gst_element_query_duration(m_Elements[PIPELINE], &format, &duration))
    {
        *dDuration = -1.0;
        return ERROR_GSTREAMER_PIPELINE_QUERY_LENGTH;
    }

    if (duration < 0)
        *dDuration = -1.0;
    else
        *dDuration = (double)duration/(double)GST_SECOND;

    m_dLastReportedDuration = *dDuration;

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::GetStreamTime()
 *
 * Get the stream/presentation time of the media clip.
 *
 * @return  true/false
 */
uint32_t CGstAudioPlaybackPipeline::GetStreamTime(double* streamTime)
{
    GstFormat format = GST_FORMAT_TIME;
    gint64    position = GST_CLOCK_TIME_NONE;

#if JFXMEDIA_ENABLE_GST_TRACE
    gst_alloc_trace_set_flags_all ((GstAllocTraceFlags)(GST_ALLOC_TRACE_LIVE | GST_ALLOC_TRACE_MEM_LIVE));
    if (!gst_alloc_trace_available ())
        g_warning ("Trace not available (recompile with trace enabled).");
    else
        gst_alloc_trace_print_live ();
#endif

    m_StateLock->Enter();
    bool notReady = (m_PlayerState == Stopped || m_PlayerState == Error);
    m_StateLock->Exit();

    // If we in Stopped state report 0 for stream time
    if (notReady)
    {
        *streamTime = 0;
        return ERROR_NONE;
    }

    if (!gst_element_query_position(m_Elements[PIPELINE], &format, &position))
    {
        // Position query failed: use timestamp of most recent buffer instead.
        position = (gint64)m_ulLastStreamTime;
    }
    else
    {
        m_ulLastStreamTime = position;
    }

    *streamTime = (double)position/(double)GST_SECOND;

    // GStreamer may report position which is slightly bigger then duration.
    // This is fine due to different rounding errors, but we should not report position which is bigger then duration.
    if (m_dLastReportedDuration == DURATION_UNKNOWN)
    {
        double dDuration = 0;
        if (GetDuration(&dDuration) != ERROR_NONE)
            m_dLastReportedDuration = DURATION_UNKNOWN; // Hopefully duration will be available next time
    }

    if (m_dLastReportedDuration != DURATION_UNKNOWN && m_dLastReportedDuration != DURATION_INDEFINITE && *streamTime > m_dLastReportedDuration)
        *streamTime = m_dLastReportedDuration;

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::SetRate()
 *
 * Set the playback rate.  The rate can be a positive or negative float.
 *
 * @param   fRate   positive/negative float
 */
uint32_t CGstAudioPlaybackPipeline::SetRate(float fRate)
{
    uint32_t ret = ERROR_NONE;

    if (IsPlayerState(Error))
        return ret;

    if (fRate != m_fRate)
    {
        if (0.0F == fRate)
        {
            GstState state;
            gst_element_get_state(m_Elements[PIPELINE], &state, NULL, 0);

            // It's not enough to check only m_PlayerState for playing state. There can be penging message to change the state
            // while we switch the rate.
            bool resume = (state == GST_STATE_PLAYING || IsPlayerState(Stalled));

            if (ERROR_NONE == Pause())
            {
                m_fRate = 0.0F;

                // Set playback resumption flag if currently playing or stalled.
                m_bResumePlayOnNonzeroRate = resume;
            }
            else
                ret = ERROR_GSTREAMER_PIPELINE_SET_RATE_ZERO;
        }
        else
        {
            // Determine current position.
            m_SeekLock->Enter();
            m_fRate = fRate;

            gint64 seek_time = 0;
            if (m_LastSeekTime == -1)
            {
                double streamTime = 0;
                GetStreamTime(&streamTime);
                seek_time = (gint64)(GST_SECOND*streamTime);
            }
            else
            {
                seek_time = m_LastSeekTime;
            }

            if (SeekPipeline(seek_time) == ERROR_NONE)
            {
                m_SeekLock->Exit();

                // Set flag to indicate change from zero rate.
                gboolean rateWasZero = (0.0F == m_fRate);

                // Resume play if resetting from zero rate and flag is set.
                if (rateWasZero && m_bResumePlayOnNonzeroRate)
                    Play(); // Ignore the return value. TOOD: Emit a warning?

                ret = ERROR_NONE;
            }
            else
            {
                m_SeekLock->Exit();
                ret = ERROR_GSTREAMER_PIPELINE_SEEK;
            }
        }
    }

    return ret;
}

/**
 * CGstAudioPlaybackPipeline::GetRate()
 *
 * Init an audio-only playback pipeline.
 *
 * @return  float value for the rate.
 */
uint32_t CGstAudioPlaybackPipeline::GetRate(float* rate)
{
    *rate = m_fRate;
    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::SetVolume()
 *
 * Set the volume for audio playback.
 *
 * @param   fVolume float value between 0.0f and 1.0f.
 */
uint32_t CGstAudioPlaybackPipeline::SetVolume(float volume)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    // Clamp the value
    volume = (volume < 0.0F) ? 0.0F :
             (volume > 1.0F) ? 1.0F :
             volume;

    g_object_set (G_OBJECT (m_Elements[AUDIO_VOLUME]), "volume", volume, NULL);

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::GetVolume()
 *
 * Get the audio volume.
 *
 * @return  a float value between -1.0f and 1.0f
 */
uint32_t CGstAudioPlaybackPipeline::GetVolume(float* volume)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    gdouble dvolume = 1.0F;
    g_object_get (m_Elements[AUDIO_VOLUME], "volume", &dvolume, NULL);

    *volume = (gfloat)dvolume;

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::SetBalance()
 *
 * Set the balance for the audio volume between left and right audio channel.
 *
 * @param   fBalance    float value between -1.0f and 1.0f
 */
uint32_t CGstAudioPlaybackPipeline::SetBalance(float fBalance)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    fBalance = (fBalance < -1.0F) ? -1.0F :
              (fBalance >  1.0F) ?  1.0F :
               fBalance;

    g_object_set (G_OBJECT (m_Elements[AUDIO_BALANCE]), "panorama", fBalance, NULL);

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::GetBalance()
 *
 * Get the audio balance between left and right channel.
 *
 * @return  float value between -1.0f and 1.0f
 */
uint32_t CGstAudioPlaybackPipeline::GetBalance(float* balance)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    gfloat fbalance = 0.0F;
    g_object_get (m_Elements[AUDIO_BALANCE], "panorama", &fbalance, NULL);

    *balance = fbalance;

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::SetAudioSyncDelay()
 *
 * Set an audio sync delay for the audio.  May keep audio and video in sync if video rendering
 * has a longer path.
 *
 * @param   lMillis     time delay in milliseconds
 */
uint32_t CGstAudioPlaybackPipeline::SetAudioSyncDelay(long millis)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    g_object_set (G_OBJECT (m_Elements[AUDIO_SINK]), "ts-offset", (gint64)(millis*GST_MSECOND), NULL);

    return ERROR_NONE;
}

/**
 * CGstAudioPlaybackPipeline::GetAudioSyncDelay()
 *
 * Get the audio sync delay.
 *
 * @return  time delay value in milliseconds.
 */
uint32_t CGstAudioPlaybackPipeline::GetAudioSyncDelay(long* audioSyncDelay)
{
    if (IsPlayerState(Error))
        return ERROR_NONE;

    gint64 nanos = 0;
    g_object_get (m_Elements[AUDIO_SINK], "ts-offset", &nanos, NULL);

    *audioSyncDelay = (long)GST_TIME_AS_MSECONDS(nanos);

    return ERROR_NONE;
}

CAudioEqualizer* CGstAudioPlaybackPipeline::GetAudioEqualizer()
{
    return m_pAudioEqualizer;
}

CAudioSpectrum* CGstAudioPlaybackPipeline::GetAudioSpectrum()
{
    return m_pAudioSpectrum;
}

bool CGstAudioPlaybackPipeline::IsCodecSupported(GstCaps *pCaps)
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
                if (strstr(mimetype, CONTENT_TYPE_MPA) != NULL || // AAC or MPEG
                    strstr(mimetype, CONTENT_TYPE_MP3) != NULL)    // MPEG-1 or -2
                {
                    gint mpegversion = 0;

                    if (gst_structure_get_int(s, "mpegversion", &mpegversion))
                    {
                        if (mpegversion == 4)
                        {
                            gboolean is_supported = FALSE;
                            g_object_set(m_Elements[AUDIO_DECODER], "codec-id", (gint)CODEC_ID_AAC, NULL);
                            g_object_get(m_Elements[AUDIO_DECODER], "is-supported", &is_supported, NULL);
                            if (is_supported)
                            {
                                return TRUE;
                            }
                            else
                            {
                                m_audioCodecErrorCode = ERROR_MEDIA_AAC_FORMAT_UNSUPPORTED;
                                return FALSE;
                            }
                        }
                    }
                }
            }
        }
    }

    return TRUE;
#else // TARGET_OS_WIN32
    return TRUE;
#endif // TRAGET_OS_WIN32
}

bool CGstAudioPlaybackPipeline::CheckCodecSupport()
{
    if (!m_bHasAudio)
    {
        if (m_pEventDispatcher && m_audioCodecErrorCode != ERROR_NONE)
        {
            if (!m_pEventDispatcher->SendPlayerMediaErrorEvent(m_audioCodecErrorCode))
            {
                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
            }
        }

        return FALSE;
    }

    return TRUE;
}

/**
 * CGstAudioPlaybackPipeline::BusCallback()
 *
 * GStreamer message bus for the audio pipeline.
 *
 * @param
 *
 * @return  true/false
 */
gboolean CGstAudioPlaybackPipeline::BusCallback(GstBus* bus, GstMessage* msg, sBusCallbackContent* pBusCallbackContent)
{
    pBusCallbackContent->m_DisposeLock->Enter();

    LOWLEVELPERF_EXECTIMESTART("BusCallback()");

    if (pBusCallbackContent->m_bIsDisposed)
    {
        pBusCallbackContent->m_DisposeLock->Exit();
        return FALSE;
    }

    CGstAudioPlaybackPipeline* pPipeline = pBusCallbackContent->m_pPipeline;

    switch (GST_MESSAGE_TYPE (msg)) {

        case GST_MESSAGE_DURATION:
        {
            if(NULL != pPipeline->m_pEventDispatcher)
            {
                GstFormat format;
                gint64 durationNanos;

                // Parse the message to obtain the value and its format.
                gst_message_parse_duration(msg, &format, &durationNanos);

                // Continue if the format is time.
                if (format == GST_FORMAT_TIME && durationNanos > 0)
                {
                    // Convert the duration from nanoseconds to seconds.
                    double duration = (double)durationNanos/(double)GST_SECOND;

                    // Dispatch the event.
                    if (!pPipeline->m_pEventDispatcher->SendDurationUpdateEvent(duration))
                    {
                        if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_DURATION_UPDATE_EVENT))
                        {
                            LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                        }
                    }
                }
            }
        }
            break;

        case GST_MESSAGE_EOS:
        {
            // In some cases we may receive several GST_MESSAGE_EOS and signal Finsihed state several times.
            // We should enter and signal Finished state only once.
            // GST_MESSAGE_EOS will be send several times, because of bug or design issue in gstbin.
            // gstbin will check all sinks for EOS message and if all sinks posted EOS message it will forward message to application.
            // However, gstbin does not clear EOS message on sinks, which will result in several EOS messages being posted to application.
            // This condition reproduces after EOS-> Seek to restart playback -> EOS (2 messages received).
            if (!pPipeline->IsPlayerState(Finished))
            {
                // Set the state to Finished which may only be exited by seeking back before the finish time.
                pPipeline->SetPlayerState(Finished, false);

                if (pPipeline->m_pOptions->GetHLSModeEnabled())
                    pPipeline->m_bLastProgressValueEOS = FALSE; // Otherwise we will resume playback if we loop and user hits stop
            }
        }
            break;

        case GST_MESSAGE_ERROR:
        {
            gchar  *debug = NULL;
            GError *error = NULL;

            gst_message_parse_error (msg, &error, &debug);

            // Handle connection lost error
            if (error)
            {
                if (pPipeline != NULL && pPipeline->m_pEventDispatcher != NULL && error->domain == GST_RESOURCE_ERROR && error->code == GST_RESOURCE_ERROR_READ)
                {
                    if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_LOCATOR_CONNECTION_LOST))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                    pPipeline->m_bIgnoreError = TRUE;
                    g_error_free (error);
                    if (debug)
                        g_free(debug);
                    break;
                }
                // GstBaseSrc will send GST_STREAM_ERROR_FAILED when connection is lost
                // We need to ignore this error if it was received right after GST_RESOURCE_ERROR_READ
                else if (pPipeline != NULL && pPipeline->m_bIgnoreError && error->domain == GST_STREAM_ERROR && error->code == GST_STREAM_ERROR_FAILED)
                {
                    pPipeline->m_bIgnoreError = FALSE;
                    g_error_free (error);
                    if (debug)
                        g_free(debug);
                    break;
                }
                else if (pPipeline != NULL && pPipeline->m_pEventDispatcher != NULL && error->domain == GST_STREAM_ERROR && 
                    (error->code == GST_STREAM_ERROR_DECODE || error->code == GST_STREAM_ERROR_WRONG_TYPE))
                {
                    if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_MEDIA_INVALID))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                    g_error_free (error);
                    if (debug)
                        g_free(debug);
                    break;
                }
                else if (pPipeline != NULL && pPipeline->m_pEventDispatcher != NULL && error->domain == GST_STREAM_ERROR && 
                    (error->code == GST_STREAM_ERROR_CODEC_NOT_FOUND || 
                     error->code == GST_STREAM_ERROR_FAILED ||
                     error->code == GST_STREAM_ERROR_TYPE_NOT_FOUND))
                {
                    if (pPipeline->m_pOptions->GetHLSModeEnabled())
                    {
                        if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_MEDIA_HLS_FORMAT_UNSUPPORTED))
                        {
                            LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                        }
                    }
                    else
                    {
                        if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_MEDIA_INVALID))
                        {
                            LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                        }
                    }
                    g_error_free (error);
                    if (debug)
                        g_free(debug);
                    break;
                }
            }

            // Clear ignore error in case if we did not receive GST_STREAM_ERROR_FAILED after GST_RESOURCE_ERROR_READ.
            pPipeline->m_bIgnoreError = FALSE;

            // Tear down GStreamer pipeline only if PlayerState is not Error, becuase when GST_MESSAGE_ERROR
            // is generated during state change, we may have infinite loop by getting GST_MESSAGE_ERROR
            // each time when we try to set pipeline to GST_STATE_NULL.
            if (!pPipeline->IsPlayerState(Error))
                gst_element_set_state(pPipeline->m_Elements[PIPELINE], GST_STATE_NULL); // Ignore return value.

            pPipeline->SetPlayerState(Error, true);

            if (error)
            {
                if (NULL != pPipeline->m_pEventDispatcher)
                {
                    if (error->domain == GST_STREAM_ERROR && error->code == GST_STREAM_ERROR_DEMUX)
                    {
                        if (!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_MEDIA_CORRUPTED))
                        {
                            LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                        }
                    }
                    else
                    {
                        if (!pPipeline->m_pEventDispatcher->SendPlayerHaltEvent(error->message, (double)msg->timestamp / GST_SECOND))
                        {
                            if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_HALT_EVENT))
                            {
                                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                            }
                        }
                    }
                }
                g_error_free (error);
            }

            if (debug)
            {
                LOGGER_LOGMSG(LOGGER_DEBUG, debug);
                g_free (debug);
            }
        }
            break;

        case GST_MESSAGE_WARNING:
        {
            gchar  *debug = NULL;
            GError *warning = NULL;

            gst_message_parse_warning (msg, &warning, &debug);

            if (warning)
            {
                pPipeline->m_pEventDispatcher->Warning(WARNING_GSTREAMER_PIPELINE_WARNING,
                                                       (const char*)warning->message);
                LOGGER_LOGMSG(LOGGER_WARNING, warning->message);
                g_error_free (warning);
            }

            if (debug)
            {
                LOGGER_LOGMSG(LOGGER_DEBUG, debug);
                g_free (debug);
            }
        }
            break;

        case GST_MESSAGE_INFO:
        {
            gchar  *debug = NULL;
            GError *info = NULL;

            gst_message_parse_info (msg, &info, &debug);

            if (info)
            {
                pPipeline->m_pEventDispatcher->Warning(WARNING_GSTREAMER_PIPELINE_INFO_ERROR,
                                                       (const char*)info->message);
                LOGGER_LOGMSG(LOGGER_ERROR, info->message);
                g_error_free (info);
            }

            if (debug)
            {
                LOGGER_LOGMSG(LOGGER_DEBUG, debug);
                g_free (debug);
            }
        }
            break;

        case GST_MESSAGE_STATE_CHANGED:
        {
            GstState oldState, newState, pendingState;

            gst_message_parse_state_changed(msg, &oldState, &newState, &pendingState);
#if JFXMEDIA_DEBUG
            if (GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[PIPELINE]))
                g_print ("%s: %s->%s pending(%s)\n",
                        GST_OBJECT_NAME(GST_MESSAGE_SRC(msg)),
                        gst_element_state_get_name(oldState),
                        gst_element_state_get_name(newState),
                        gst_element_state_get_name(pendingState));
#endif

            // Check if we need to set clock
            // Based on GStreamer documentation audio sink should provide clock when it in PAUSED state.
            // In NULL or READY state clock maybe invalid.
            if (!pPipeline->m_bIsClockSet && pPipeline->m_Elements[AUDIO_SINK] != NULL && pPipeline->m_bHasAudio && GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[AUDIO_SINK]) && pendingState == GST_STATE_VOID_PENDING && newState == GST_STATE_READY)
            {
                pPipeline->m_bSetClock = true;
                pPipeline->m_bIsClockSet = true;
            }

            // Check if sink are ready
            if (!pPipeline->m_bDynamicElementsReady)
            {
                if (pPipeline->m_Elements[AUDIO_SINK] == NULL)
                    pPipeline->m_bAudioSinkReady = true;
                else if (GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[AUDIO_SINK]) && newState == GST_STATE_PAUSED && oldState == GST_STATE_READY && pendingState == GST_STATE_VOID_PENDING)
                    pPipeline->m_bAudioSinkReady = true;

                if (pPipeline->m_Elements[VIDEO_SINK] == NULL)
                    pPipeline->m_bVideoSinkReady = true;
                else if (GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[VIDEO_SINK]) && newState == GST_STATE_PAUSED && oldState == GST_STATE_READY && pendingState == GST_STATE_VOID_PENDING)
                    pPipeline->m_bVideoSinkReady = true;

                if (pPipeline->m_bAudioSinkReady && pPipeline->m_bVideoSinkReady)
                    pPipeline->m_bDynamicElementsReady = true;
            }

            // Update clock if needed
            // Audio sink will provide clock when it in paused or playing state.
            // Our pipeline will not find audio sink clock, because we use audio sink inside bin and bin hides clock distribution.
            // When pipeline cannot find clock it will use GstSystemClock, so we need to set correct clock to pipeline.
            if (pPipeline->m_bSetClock && ((pPipeline->m_bStaticPipeline && pPipeline->m_Elements[AUDIO_SINK] != NULL && pPipeline->m_bHasAudio && GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[AUDIO_SINK]) && pendingState == GST_STATE_VOID_PENDING && newState == GST_STATE_PAUSED) || pPipeline->m_bDynamicElementsReady))
            {
                pPipeline->m_bSetClock = false;

                // Get clock from audio sink
                GstClock *clock = gst_element_provide_clock(pPipeline->m_Elements[AUDIO_SINK]);

                // Set it to pipeline only if we have one
                // If we set NULL as clock pipeline will render as fast as possible and we do not want this to happen.
                // In case if we did not get clock, pipeline will use GstSystemClock which is better then using NULL.
                if (clock != NULL)
                {
                    gst_pipeline_set_clock(GST_PIPELINE(pPipeline->m_Elements[PIPELINE]), clock);
                    gst_object_unref(clock);
                }
            }

            // We have special case when we in Paused or Stall state and we going to Stopped or Paused state. In this case
            // newState and oldState will be set to GST_STATE_PAUSED.
            if (GST_MESSAGE_SRC(msg) == GST_OBJECT(pPipeline->m_Elements[PIPELINE])
                && ((pendingState == GST_STATE_VOID_PENDING && newState != oldState && !pPipeline->IsPlayerState(Unknown)) // Regular state change
                || ((pPipeline->IsPlayerPendingState(Stopped) || pPipeline->IsPlayerPendingState(Paused) || pPipeline->m_StallOnPause) && newState == GST_STATE_PAUSED && oldState == GST_STATE_PAUSED && pendingState == GST_STATE_VOID_PENDING) // Special cases for pause, stall and stop
                     || (pPipeline->IsPlayerState(Unknown) && newState == GST_STATE_PAUSED && (oldState == GST_STATE_READY || oldState == GST_STATE_PAUSED) && pendingState == GST_STATE_VOID_PENDING && !pPipeline->m_bStaticPipeline && pPipeline->m_bDynamicElementsReady) // Ready for dynamic pipeline
                     || (pPipeline->IsPlayerState(Unknown) && newState == GST_STATE_PAUSED && oldState == GST_STATE_READY && pendingState == GST_STATE_VOID_PENDING && pPipeline->m_bStaticPipeline))) // Ready for static pipeline
            {
                if (GST_STATE_PAUSED == newState)
                {
                    LOWLEVELPERF_EXECTIMESTOP("GST_STATE_PAUSED");

#if ENABLE_PROGRESS_BUFFER
                    // Update buffer position only if progress buffer got EOS.
                    // In some case progress may not be reported yet, because duration was not available yet.
                    // By now it should be available, so lets update buffer position.
                    if (pPipeline->m_bLastProgressValueEOS)
                        pPipeline->UpdateBufferPosition();
#endif // ENABLE_PROGRESS_BUFFER
                }

                // Update the player state.
                pPipeline->UpdatePlayerState(newState, oldState);
            }
        }
            break;

#if ENABLE_PROGRESS_BUFFER
        case GST_MESSAGE_APPLICATION:       //This currently handles messages from the progress buffer element
        {
            const GstStructure *pStr = gst_message_get_structure(msg);
            if (gst_structure_has_name(pStr, PB_MESSAGE_BUFFERING))
            {
                // See comment to progressbuffer.c:send_position_message for more details.
                const GValue *start_v    = gst_structure_get_value(pStr, "start");
                const GValue *position_v = gst_structure_get_value(pStr, "position");
                const GValue *stop_v     = gst_structure_get_value(pStr, "stop");
                const GValue *eos_v      = gst_structure_get_value(pStr, "eos");

                gint64    start     = g_value_get_int64(start_v);
                gint64    position  = g_value_get_int64(position_v);
                gint64    stop      = g_value_get_int64(stop_v);
                gboolean  eos       = g_value_get_boolean(eos_v); // eos indicates if progress buffer received EOS event.
                                                                    // This mean that progress buffer will not send any progress messages anymore and no more data will be available.

                // When we receive GST_MESSAGE_APPLICATION pipeline may not fully complete transition to PAUSE state.
                // In this case duration will not be available, thus we cannot report progress.
                // Also, file may be very small and in this case progress buffer will able to download all data (no more GST_MESSAGE_APPLICATION)
                // untill pipeline completes transition to PAUSE state. In such case we will never report any progress.
                // To solve this lets save last reported value and update progress when pipeline completed transition to PAUSE state.
                pPipeline->m_llLastProgressValueStart = start;
                pPipeline->m_llLastProgressValuePosition = position;
                pPipeline->m_llLastProgressValueStop = stop;
                pPipeline->m_bLastProgressValueEOS = eos;

                // Update buffer position
                pPipeline->UpdateBufferPosition();
            }
            else if (gst_structure_has_name(pStr, PB_MESSAGE_UNDERRUN))
                pPipeline->BufferUnderrun();
            else if (gst_structure_has_name(pStr, HLS_PB_MESSAGE_STALL))
                pPipeline->HLSBufferStall();
            else if (gst_structure_has_name(pStr, HLS_PB_MESSAGE_RESUME))
                pPipeline->HLSBufferResume(false);
            else if (gst_structure_has_name(pStr, HLS_PB_MESSAGE_HLS_EOS))
                pPipeline->HLSBufferResume(true);
            else if (gst_structure_has_name(pStr, HLS_PB_MESSAGE_FULL))
            {
                pPipeline->m_StallLock->Enter();
                pPipeline->m_bHLSPBFull = true;
                pPipeline->m_StallLock->Exit();
                pPipeline->HLSBufferResume(false);
            }
            else if (gst_structure_has_name(pStr, HLS_PB_MESSAGE_NOT_FULL))
                pPipeline->m_bHLSPBFull = false;
        }
            break;
#endif  //ENABLE_PROGRESS_BUFFER

        case GST_MESSAGE_ELEMENT:
        {
            const GstStructure *pStr = gst_message_get_structure (msg);
            if (gst_structure_has_name(pStr, "spectrum"))
            {
                GstClockTime timestamp, duration;

                if (!gst_structure_get_clock_time (pStr, "timestamp", &timestamp))
                    timestamp = GST_CLOCK_TIME_NONE;

                if (!gst_structure_get_clock_time (pStr, "duration", &duration))
                    duration = GST_CLOCK_TIME_NONE;

                size_t bandsNum = pPipeline->GetAudioSpectrum()->GetBands();

                if (bandsNum > 0)
                {
                    float *magnitudes = new float[bandsNum];
                    float *phases = new float[bandsNum];

                    const GValue *magnitudes_value = gst_structure_get_value(pStr, "magnitude");
                    const GValue *phases_value = gst_structure_get_value(pStr, "phase");
                    for (int i=0; i < bandsNum; i++)
                    {
                        magnitudes[i] = g_value_get_float( gst_value_list_get_value (magnitudes_value, i));
                        phases[i] = g_value_get_float( gst_value_list_get_value (phases_value, i));
                    }
                    pPipeline->GetAudioSpectrum()->UpdateBands((int)bandsNum, magnitudes, phases);

                    delete [] magnitudes;
                    delete [] phases;
                }

                if (!pPipeline->m_pEventDispatcher->SendAudioSpectrumEvent(GST_TIME_AS_SECONDS((double)timestamp),
                    GST_TIME_AS_SECONDS((double)duration)))
                {
                    if(!pPipeline->m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_AUDIO_SPECTRUM_EVENT))
                    {
                        LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                    }
                }
          }

        }
            break;

        case GST_MESSAGE_ASYNC_DONE:
            pPipeline->m_SeekLock->Enter();
            pPipeline->m_LastSeekTime = -1;
            pPipeline->m_SeekLock->Exit();
            break;

        default:
            break;
    }

    LOWLEVELPERF_EXECTIMESTOP("BusCallback()");

    pBusCallbackContent->m_DisposeLock->Exit();

    return TRUE;
}

// This function will be called in 2 cases and it will be always called when no more BusCallbacks is expected:
// 1 - When g_source_destroy() is called from Dispose() and there are no pending or in-progress BusCallbacks. It will be called from Dispose() thread.
// 2 - When g_source_destroy() is called from Dispose() and all pending or in-progress BusCallbacks are done. It will be called from main loop thread and pipeline will be gone at this time.
// So lets figure out who will be responsible to free memory, since DisposeLock is used by Dispose() as well.
void CGstAudioPlaybackPipeline::BusCallbackDestroyNotify(sBusCallbackContent* pBusCallbackContent)
{
    if (pBusCallbackContent)
    {
        bool bFreeMeHere = false;

        pBusCallbackContent->m_DisposeLock->Enter();
        if (pBusCallbackContent->m_bIsDisposed)
            bFreeMeHere = true; // Everything is gone, so free me here.
        else
            pBusCallbackContent->m_bFreeMe = true; // Ask Dispose() when it is done to free me
        pBusCallbackContent->m_DisposeLock->Exit();

        if (bFreeMeHere)
        {
            delete pBusCallbackContent->m_DisposeLock;
            delete pBusCallbackContent;
        }
    }
}

/**
 * CGstAudioPlaybackPipeline::SetPlayerState()
 *
 * Sets our "player" state.  This is not the same as the gst pipeline state.  This function should not be
 * called for normal state changes.  This is for out-of-band changes like stalled condition or EOS.
 *
 */
void CGstAudioPlaybackPipeline::SetPlayerState(PlayerState newPlayerState, bool bSilent)
{
    m_StateLock->Enter();

    // Determine if we need to send an event out
    bool updateState = newPlayerState != m_PlayerState;
    if (updateState)
    {
        if (NULL != m_pEventDispatcher && !bSilent)
        {
            m_PlayerState = newPlayerState;

            if (!m_pEventDispatcher->SendPlayerStateEvent(newPlayerState, 0.0))
            {
                if(!m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_PLAYER_STATE_EVENT))
                {
                    LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
                }
            }
        }
        else
        {
            m_PlayerState = newPlayerState;
        }
    }

    m_StateLock->Exit();

    if ((updateState && newPlayerState == Stalled && m_bLastProgressValueEOS) ||
        (updateState && newPlayerState == Stalled && m_bHLSPBFull))
    {
       Play();
    }
}

/**
 * CGstAudioPlaybackPipeline::IsPlayerState()
 *
 * Synchronously tests if the player state equals to the mentioned
 */
bool CGstAudioPlaybackPipeline::IsPlayerState(PlayerState state)
{
    m_StateLock->Enter();
    bool result = (m_PlayerState == state);
    m_StateLock->Exit();

    return result;
}

/**
 * CGstAudioPlaybackPipeline::IsPlayerPendingState()
 *
 * Synchronously tests if the player pending state equals to the mentioned
 */
bool CGstAudioPlaybackPipeline::IsPlayerPendingState(PlayerState state)
{
    m_StateLock->Enter();
    bool result = (m_PlayerPendingState == state);
    m_StateLock->Exit();

    return result;
}

/**
 * CGstAudioPlaybackPipeline::UpdatePlayerState()
 *
 * Intermediates between Gst pipeline state and our "player" state.  This is called when we get a pipeline
 * state change.
 *
 */
void CGstAudioPlaybackPipeline::UpdatePlayerState(GstState newState, GstState oldState)
{
    m_StateLock->Enter();

    PlayerState newPlayerState = m_PlayerState;
    bool        bSilent = false;

    switch(m_PlayerState)
    {
        case Unknown:
            if((GST_STATE_READY == oldState && GST_STATE_PAUSED == newState) || (GST_STATE_PAUSED == oldState && GST_STATE_PAUSED == newState))
            {
                newPlayerState = Ready;
            }
            break;

        case Ready:
            if(GST_STATE_PAUSED == oldState)
            {
                if(GST_STATE_READY == newState)
                    newPlayerState = Unknown;
                else if(GST_STATE_PLAYING == newState)
                    newPlayerState = Playing;
            }
            break;

        case Playing:
            if(GST_STATE_PLAYING == oldState)
            {
                if(GST_STATE_PAUSED == newState)
                {
                    if(m_PlayerPendingState == Stopped)
                    {
                        m_StallOnPause = false;
                        m_PlayerPendingState = Unknown;
                        newPlayerState = Stopped;
                    }
                    else if (m_StallOnPause && m_PlayerPendingState != Paused)
                    {
                        m_StallOnPause = false;
                        newPlayerState = Stalled;
                    }
                    else if (m_PlayerPendingState == Paused)
                    {
                        m_StallOnPause = false;
                        m_PlayerPendingState = Unknown;
                        newPlayerState = Paused;
                    }
                    else
                    {
                        newPlayerState = Finished;
                    }
                }
            }
            else if(GST_STATE_PAUSED == oldState) // May happen during seek
            {
                if(GST_STATE_PAUSED == newState)
                {
                    if(m_PlayerPendingState == Stopped)
                    {
                        m_StallOnPause = false;
                        m_PlayerPendingState = Unknown;
                        newPlayerState = Stopped;
                    }
                    else if (m_StallOnPause && m_PlayerPendingState != Paused)
                    {
                        m_StallOnPause = false;
                        newPlayerState = Stalled;
                    }
                    else if (m_PlayerPendingState == Paused)
                    {
                        m_StallOnPause = false;
                        m_PlayerPendingState = Unknown;
                        newPlayerState = Paused;
                    }
                }
            }
            break;

        case Paused:
            if(GST_STATE_PAUSED == oldState)
            {
                if(m_PlayerPendingState == Stopped)
                {
                    m_PlayerPendingState = Unknown;
                    newPlayerState = Stopped;
                }
                else
                {
                    if(GST_STATE_PLAYING == newState)
                        newPlayerState = Playing;
                    else if(GST_STATE_READY == newState)
                        newPlayerState = Unknown;
                }
            }
            break;

        case Stopped:
            if(GST_STATE_PAUSED == oldState)
            {
                if (m_PlayerPendingState == Paused && GST_STATE_PAUSED == newState)
                {
                    m_PlayerPendingState = Unknown;
                    newPlayerState = Paused;
                }
                else if(GST_STATE_PLAYING == newState)
                {
                    newPlayerState = Playing;
                }
                else if(GST_STATE_READY == newState)
                {
                    newPlayerState = Unknown;
                }
            }
            break;

        case Stalled:
        {
            if (GST_STATE_PAUSED == oldState && GST_STATE_PLAYING == newState)
                newPlayerState = Playing;
            else if (GST_STATE_PAUSED == oldState && GST_STATE_PAUSED == newState)
            {
                if (m_PlayerPendingState == Stopped)
                {
                    m_PlayerPendingState = Unknown;
                    newPlayerState = Stopped;
                }
                else if (m_PlayerPendingState == Paused)
                {
                    m_PlayerPendingState = Unknown;
                    newPlayerState = Paused;
                }
            }
            break;
        }

        case Finished:
            if(GST_STATE_PLAYING == oldState)
            {
                if(GST_STATE_PAUSED == newState)
                {
                    if(m_PlayerPendingState == Stopped)
                    {
                        m_PlayerPendingState = Unknown;
                        m_bSeekInvoked = false;
                        newPlayerState = Stopped;
                    }
                    // No need to switch to paused state, since Pause is not valid in Finished state
                }
            }
            else if(GST_STATE_PAUSED == oldState)
            {
                if(GST_STATE_PLAYING == newState)
                {
                    // We can go from Finished to Playing only when seek happens (or repeat)
                    // This state change should be silent.
                    newPlayerState = Playing;
                    m_bSeekInvoked = false;
                    bSilent = true;
                }
                else if(GST_STATE_PAUSED == newState)
                {
                    if(m_PlayerPendingState == Stopped)
                    {
                        m_PlayerPendingState = Unknown;
                        m_bSeekInvoked = false;
                        newPlayerState = Stopped;
                    }
                    else
                    {
                        m_bSeekInvoked = false;
                        newPlayerState = Paused;
                    }
                }
            }
            break;

        case Error:
            break;
    }

    SetPlayerState(newPlayerState, bSilent);
    m_StateLock->Exit();
}

//*************************************************************************************************
//* Scanning tracks information
//*************************************************************************************************
void CGstAudioPlaybackPipeline::SendTrackEvent()
{
    if (NULL != m_pEventDispatcher)
    {
        CTrack::Encoding encoding;
        int              channelMask;

        // Detect the encoding type from the information that we have from caps.
        if (m_AudioTrackInfo.mimeType.find("audio/x-raw") != string::npos)
            encoding = CTrack::PCM;
        else if (m_AudioTrackInfo.mimeType.find(CONTENT_TYPE_MPA) != string::npos ||
                 m_AudioTrackInfo.mimeType.find(CONTENT_TYPE_MP3) != string::npos)
        {
            if (m_AudioTrackInfo.mpegversion == 1)
                encoding = (m_AudioTrackInfo.layer == 3) ? CTrack::MPEG1LAYER3 : CTrack::MPEG1AUDIO;
            else if (m_AudioTrackInfo.mpegversion == 4)
                encoding = CTrack::AAC;
            else
                encoding = CTrack::CUSTOM;
        }
        else
            encoding = CTrack::CUSTOM;

        // Detect the channelmask from the number of channels
        switch (m_AudioTrackInfo.channels)
        {
            case 1:
                channelMask = CAudioTrack::FRONT_CENTER;
                break;

            case 2:
                channelMask = CAudioTrack::FRONT_RIGHT | CAudioTrack::FRONT_LEFT;
                break;

            case 4:
                channelMask = CAudioTrack::FRONT_RIGHT | CAudioTrack::FRONT_LEFT | CAudioTrack::REAR_RIGHT | CAudioTrack::REAR_LEFT;
                break;

            case 0:
            default:
                channelMask = CAudioTrack::UNKNOWN;
                break;
        }

        CAudioTrack *p_AudioTrack = new CAudioTrack(m_AudioTrackInfo.trackID,
                                                    m_AudioTrackInfo.mimeType,
                                                    encoding,
                                                    (bool)m_AudioTrackInfo.trackEnabled,
                                                    "und",
                                                    m_AudioTrackInfo.channels,
                                                    channelMask,
                                                    (float)m_AudioTrackInfo.rate);

        if (!m_pEventDispatcher->SendAudioTrackEvent(p_AudioTrack))
        {
            if(!m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_AUDIO_TRACK_EVENT))
            {
                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
            }
        }

        delete p_AudioTrack;
    }
}

gboolean CGstAudioPlaybackPipeline::AudioSinkPadProbe(GstPad* pPad, GstBuffer *pBuffer, CGstAudioPlaybackPipeline* pPipeline)
{
    GstCaps* pCaps = GST_BUFFER_CAPS(pBuffer);
    if (NULL == pCaps || gst_caps_get_size(pCaps) < 1)
    {
        return TRUE;
    }

    GstStructure *pStructure = gst_caps_get_structure(pCaps, 0);
    pPipeline->m_AudioTrackInfo.mimeType = gst_structure_get_name(pStructure);

    gint trackID;
    gboolean enabled;
    if (!gst_structure_get_boolean(pStructure, "track_enabled", &enabled)) {
        enabled = TRUE; // default to enabled if container doesn't support it
    }
    if (!gst_structure_get_int(pStructure, "track_id", &trackID)) {
        trackID = 0; // default audio track ID if none present (can only be one in that case)
    }
    pPipeline->m_AudioTrackInfo.trackEnabled = enabled;
    pPipeline->m_AudioTrackInfo.trackID = (int64_t)trackID;

    // Don't use shortcut evaluation here. Try to get as much as possible.
    gboolean ready = gst_structure_get_int(pStructure, "channels", &pPipeline->m_AudioTrackInfo.channels) &
                     gst_structure_get_int(pStructure, "rate", &pPipeline->m_AudioTrackInfo.rate);

    if (pPipeline->m_AudioTrackInfo.mimeType.find("mpeg") != string::npos)
    {
        ready &= gst_structure_get_int(pStructure, "mpegversion", &pPipeline->m_AudioTrackInfo.mpegversion);
        gst_structure_get_int(pStructure, "layer", &pPipeline->m_AudioTrackInfo.layer); // Layer is optional.
    }

    if (ready)
    {
        pPipeline->SendTrackEvent();

        if (pPipeline->m_audioSourcePadProbeHID)    // Remove source probe if any because we've got all we need.
        {
            GstPad *pPad = gst_element_get_static_pad(pPipeline->m_Elements[AUDIO_DECODER], "src");
            gst_pad_remove_data_probe (pPad, pPipeline->m_audioSourcePadProbeHID);
            gst_object_unref(pPad);
        }
    }

    gst_pad_remove_data_probe (pPad, pPipeline->m_audioSinkPadProbeHID);

    return TRUE;
}

gboolean CGstAudioPlaybackPipeline::AudioSourcePadProbe(GstPad* pPad, GstBuffer *pBuffer, CGstAudioPlaybackPipeline* pPipeline)
{
    GstCaps* pCaps = GST_BUFFER_CAPS(pBuffer);
    if (NULL == pCaps || gst_caps_get_size(pCaps) < 1)
    {
        return TRUE;
    }

    GstStructure *pStructure = gst_caps_get_structure(pCaps, 0);

    // Here we only fill in empty fields. All fields would be empty if this is the only track test probe.
    if (pPipeline->m_AudioTrackInfo.mimeType.empty())
        pPipeline->m_AudioTrackInfo.mimeType = gst_structure_get_name(pStructure);

    if (pPipeline->m_AudioTrackInfo.channels < 0)
        gst_structure_get_int(pStructure, "channels", &pPipeline->m_AudioTrackInfo.channels);

    if (pPipeline->m_AudioTrackInfo.rate < 0)
      gst_structure_get_int(pStructure, "rate", &pPipeline->m_AudioTrackInfo.rate);

    if (pPipeline->m_AudioTrackInfo.mimeType.find("mpeg") != string::npos)
    {
        if (pPipeline->m_AudioTrackInfo.mpegversion < 0)
            gst_structure_get_int(pStructure, "mpegversion", &pPipeline->m_AudioTrackInfo.mpegversion);

        if (pPipeline->m_AudioTrackInfo.layer < 0)
            gst_structure_get_int(pStructure, "layer", &pPipeline->m_AudioTrackInfo.layer);
    }

    pPipeline->SendTrackEvent(); // Send track event anyways. We won't get any more information.

    gst_pad_remove_data_probe (pPad, pPipeline->m_audioSourcePadProbeHID);
    return TRUE; // Don't discard the data.
}

#if ENABLE_PROGRESS_BUFFER
// This callback is called when progressbuffer runs out of data.
// This can happen when we running out of data during playback, because we cannot download data fast enough.
void CGstAudioPlaybackPipeline::BufferUnderrun()
{
    if (IsPlayerState(Stalled) || IsPlayerState(Ready) || IsPlayerState(Error))
        return;

    GstState state, pending_state;
    gst_element_get_state(m_Elements[PIPELINE], &state, &pending_state, 0);

    bool finished = IsPlayerState(Finished);
    double streamTime;
    GetStreamTime(&streamTime);

    m_StallLock->Enter();
    // Make sure we do not have more data in progress buffer.
    // Stall is valid only in PLAY state, when we do seek, pipeline will be in PAUSED state.
    // Stall is not valid in Finished state, but pipeline will be in PLAY state, when we in Finsihed state.
    bool suspend = m_BufferPosition > 0 &&
                   state == GST_STATE_PLAYING && pending_state != GST_STATE_PAUSED &&
                   !m_bLastProgressValueEOS &&
                   !finished;

    m_StallLock->Exit();

    if (suspend)
    {
        m_StallOnPause = true;
        InternalPause();
    }
}

// We do not need to protect this function with mutex, because we
// call it from only one thread (BusCallback).
void CGstAudioPlaybackPipeline::UpdateBufferPosition()
{
    if (NULL != m_pEventDispatcher && m_llLastProgressValueStop > 0)
    {
        double duration;
        GetDuration(&duration);

        if (!m_pEventDispatcher->SendBufferProgressEvent(duration, m_llLastProgressValueStart,
            m_llLastProgressValueStop, m_llLastProgressValuePosition))
        {
            if(!m_pEventDispatcher->SendPlayerMediaErrorEvent(ERROR_JNI_SEND_BUFFER_PROGRESS_EVENT))
            {
                LOGGER_LOGMSG(LOGGER_ERROR, "Cannot send media error event.\n");
            }
        }

        double bufferPosition = duration * m_llLastProgressValuePosition/m_llLastProgressValueStop;

        double streamTime;
        GetStreamTime(&streamTime);

        m_StallLock->Enter();
        m_BufferPosition = bufferPosition;
        m_StallLock->Exit();

        // We need to unblock when we have atleast data for duration of m_dResumeDeltaTime or
        // if progress buffer got eos, since buffer position will not be updated anymore and no more data will be available.
        bool resume = IsPlayerState(Stalled) && ((bufferPosition - streamTime > m_dResumeDeltaTime) || m_bLastProgressValueEOS) && !IsPlayerPendingState(Paused) && !IsPlayerPendingState(Stopped);

        if (resume)
        {
            Play();
        }
    }
}

void CGstAudioPlaybackPipeline::HLSBufferStall()
{
    if (!IsPlayerState(Playing))
        return;

    GstState state, pending_state;
    gst_element_get_state(m_Elements[PIPELINE], &state, &pending_state, 0);

    m_StallLock->Enter();
    // Stall is valid only in PLAY state, when we do seek, pipeline will be in PAUSED state.
    bool suspend = (state == GST_STATE_PLAYING) && (pending_state == GST_STATE_VOID_PENDING) && !m_bLastProgressValueEOS && !m_bHLSPBFull;
    m_StallLock->Exit();

    if (suspend)
    {
        m_StallOnPause = true;
        InternalPause();
    }
}

void CGstAudioPlaybackPipeline::HLSBufferResume(bool bEOS)
{
    m_StallLock->Enter();
    if (bEOS)
        m_bLastProgressValueEOS = bEOS;
    bool resume = (IsPlayerState(Stalled) && !IsPlayerPendingState(Paused) && !IsPlayerPendingState(Stopped)) || (m_bLastProgressValueEOS && IsPlayerState(Playing) && !IsPlayerPendingState(Paused) && !IsPlayerPendingState(Stopped));
    m_StallLock->Exit();

    if (resume)
    {
        Play();
    }
}
#endif // ENABLE_PROGRESS_BUFFER
