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

#ifndef _GST_AUDIO_PLAYBACK_PIPELINE_H_
#define _GST_AUDIO_PLAYBACK_PIPELINE_H_

#include <Common/ProductFlags.h>
#include <stdint.h>
#include <jfxmedia_errors.h>
#include <Utils/JfxCriticalSection.h>
#include <PipelineManagement/Pipeline.h>
#include <gst/gst.h>
#include "GstPipelineFactory.h"
#include "GstElementContainer.h"
#include "GstAudioEqualizer.h"
#include "GstAudioSpectrum.h"
#include <string>

using namespace std;

// Pluggable audio probes and signal handlers
const int AUDIO_DECODER_HAS_SINK_PROBE   = 1 << 0;
const int AUDIO_DECODER_HAS_SOURCE_PROBE = 1 << 1;

#if TARGET_OS_WIN32
enum CODEC_ID
{
    CODEC_ID_UNKNOWN = 0,
    CODEC_ID_AAC,
    CODEC_ID_H264, // HLS
    CODEC_ID_AVC1, // MP4
};
#endif // TARGET_OS_WIN32

#define DURATION_INDEFINITE -1
#define DURATION_UNKNOWN -2

// Taken from progressbuffer.h
#define PB_MESSAGE_BUFFERING        "pb_buffering"
#define PB_MESSAGE_UNDERRUN         "pb_underrun"

#define HLS_PB_MESSAGE_STALL        "hls_pb_stall"
#define HLS_PB_MESSAGE_RESUME       "hls_pb_resume"
#define HLS_PB_MESSAGE_HLS_EOS      "hls_pb_eos"
#define HLS_PB_MESSAGE_FULL         "hls_pb_full"
#define HLS_PB_MESSAGE_NOT_FULL     "hls_pb_not_full"

class CGstAudioPlaybackPipeline;
struct sBusCallbackContent
{
    CGstAudioPlaybackPipeline* m_pPipeline;
    CJfxCriticalSection*       m_DisposeLock;
    bool                       m_bIsDisposed;
    bool                       m_bFreeMe;
};

/**
 * class CGstAudioPlaybackPipeline
 *
 * Class representing a GStreamer audio-only pipeline.
 */
class CGstAudioPlaybackPipeline : public CPipeline
{
    friend class CGstPipelineFactory;

public:
    virtual uint32_t    Init();
    virtual uint32_t    PostBuildInit();
    virtual void        Dispose();

    virtual uint32_t    Play();
    virtual uint32_t    Stop();
    virtual uint32_t    Pause();
    virtual uint32_t    Finish();

    virtual uint32_t    Seek(double seek_time);

    virtual uint32_t    GetDuration(double* dDuration);
    virtual uint32_t    GetStreamTime(double* dStreamTime);

    virtual uint32_t    SetRate(float rate);
    virtual uint32_t    GetRate(float* rate);

    virtual uint32_t    SetVolume(float volume);
    virtual uint32_t    GetVolume(float* volume);

    virtual uint32_t    SetBalance(float balance);
    virtual uint32_t    GetBalance(float* balance);

    virtual uint32_t    SetAudioSyncDelay(long millis);
    virtual uint32_t    GetAudioSyncDelay(long* millis);

    virtual CAudioEqualizer*    GetAudioEqualizer();
    virtual CAudioSpectrum*     GetAudioSpectrum();

    virtual bool IsCodecSupported(GstCaps *pCaps);
    virtual bool CheckCodecSupport();

    virtual void CheckQueueSize(GstElement *element) {};

protected:
    CGstAudioPlaybackPipeline(const GstElementContainer& elements, int flags, CPipelineOptions* pOptions);
    virtual ~CGstAudioPlaybackPipeline();

    static gboolean     BusCallback(GstBus *pBus, GstMessage *message, sBusCallbackContent* pBusCallbackContent);
    static void         BusCallbackDestroyNotify(sBusCallbackContent* pBusCallbackContent);
    void                SetPlayerState(PlayerState newPlayerState, bool bSilent);
    void                UpdatePlayerState(GstState newState, GstState oldState);
    bool                IsPlayerState(PlayerState state);
    bool                IsPlayerPendingState(PlayerState state);

    sBusCallbackContent* m_pBusCallbackContent;

protected:
    GstElementContainer m_Elements;

    double              m_dResumeDeltaTime;
    float               m_fRate;
    volatile bool       m_bSeekInvoked;
    GstClockTime        m_ulLastStreamTime;
    CGstAudioEqualizer* m_pAudioEqualizer;
    CGstAudioSpectrum*  m_pAudioSpectrum;
    int                 m_audioCodecErrorCode;

    // Stall handling stuff
    volatile bool        m_StallOnPause; // True if paused because of stall condition

#if ENABLE_LOWLEVELPERF
    // Proportion value of QoS event if enabled:
    // http://gstreamer.freedesktop.org/data/doc/gstreamer/head/gstreamer/html/gstreamer-GstEvent.html#gst-event-new-qos
    // For video streams this will be the value at the sink pad of the video sink and for
    // audio-only streams the value at the sink pad of the audio sink.
    double              m_dUpstreamDataRate;
#endif

private:
    static void         OnParserSrcPadAdded(GstElement *element, GstPad *pad, CGstAudioPlaybackPipeline* pPipeline);
    static gboolean     AudioSourcePadProbe(GstPad* pPad, GstBuffer *pBuffer, CGstAudioPlaybackPipeline* pPipeline);
    static gboolean     AudioSinkPadProbe(GstPad* pPad, GstBuffer *pBuffer, CGstAudioPlaybackPipeline* pPipeline);

    void                SendTrackEvent();
    uint32_t            InternalPause();
    uint32_t            SeekPipeline(gint64 seek_time);

#if ENABLE_PROGRESS_BUFFER
    void                BufferUnderrun();
    void                UpdateBufferPosition();
    void                HLSBufferStall();
    void                HLSBufferResume(bool bEOS);
#endif // ENABLE_PROGRESS_BUFFER

    int                 m_AudioFlags;
    gulong              m_audioSinkPadProbeHID;
    gulong              m_audioSourcePadProbeHID;

    // Stall handling stuff
    CJfxCriticalSection* m_StallLock;
    gdouble              m_BufferPosition;
    bool                 m_bHLSPBFull;

    // Seek/Rate
    CJfxCriticalSection* m_SeekLock;
    gint64               m_LastSeekTime;

    // Incrementally filled structure. Earlier it's filled earlier we send AudioTrack event.
    struct AudioTrackInfo
    {
        gboolean trackEnabled;
        int64_t trackID;
        string  mimeType;
        gint    channels;
        gint    rate;
        gint    mpegversion;
        gint    layer;

        AudioTrackInfo() : trackEnabled(FALSE), trackID(0), channels(-1), rate(-1), mpegversion(-1), layer(-1) {}
    };

    AudioTrackInfo      m_AudioTrackInfo;

    GSource*            m_pBusSource;
    gboolean            m_bIgnoreError;
    bool                m_bResumePlayOnNonzeroRate;

    double              m_dLastReportedDuration;

    bool                m_bSetClock;
    bool                m_bIsClockSet;

    CJfxCriticalSection* m_StateLock;

#if ENABLE_PROGRESS_BUFFER
    gint64    m_llLastProgressValueStart;
    gint64    m_llLastProgressValuePosition;
    gint64    m_llLastProgressValueStop;
    gboolean  m_bLastProgressValueEOS;
#endif // ENABLE_PROGRESS_BUFFER
};

#endif  //_GST_AUDIO_PLAYBACK_PIPELINE_H_
