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

#ifndef _JAVA_PLAYER_EVENT_DISPATCHER_H_
#define _JAVA_PLAYER_EVENT_DISPATCHER_H_

#include <jni.h>

#include <PipelineManagement/AudioTrack.h>
#include <PipelineManagement/VideoTrack.h>
#include <PipelineManagement/SubtitleTrack.h>
#include <PipelineManagement/PlayerEventDispatcher.h>
#include <PipelineManagement/Pipeline.h>
#include <PipelineManagement/VideoFrame.h>
#include <MediaManagement/Media.h>
#include <MediaManagement/MediaWarningListener.h>

using namespace std;

class CJavaPlayerEventDispatcher : public CPlayerEventDispatcher
{
public:
    CJavaPlayerEventDispatcher();
    ~CJavaPlayerEventDispatcher();

    void Init(JNIEnv *env, jobject PlayerInstance, CMedia* pMedia);
    void Dispose();

    virtual bool SendPlayerMediaErrorEvent(int errorCode);
    virtual bool SendPlayerHaltEvent(const char* message, double mstTime);
    virtual bool SendPlayerStateEvent(int newState, double presentTime);
    virtual bool SendNewFrameEvent(CVideoFrame* pVideoFrame);
    virtual bool SendFrameSizeChangedEvent(int width, int height);
    virtual bool SendAudioTrackEvent(CAudioTrack* pTrack);
    virtual bool SendVideoTrackEvent(CVideoTrack* pTrack);
    virtual bool SendSubtitleTrackEvent(CSubtitleTrack* pTrack);
    virtual bool SendMarkerEvent(string name, double time);
    virtual bool SendBufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position);
    virtual bool SendDurationUpdateEvent(double time);
    virtual bool SendAudioSpectrumEvent(double time, double duration);
    virtual void Warning(int warningCode, const char* warningMessage);

private:
    JavaVM *m_PlayerVM;
    jobject m_PlayerInstance;
    jlong   m_MediaReference; // FIXME: Nuke this field, it's completely unused

    static jmethodID m_SendWarningMethod;

    static jmethodID m_SendPlayerMediaErrorEventMethod;
    static jmethodID m_SendPlayerHaltEventMethod;
    static jmethodID m_SendPlayerStateEventMethod;
    static jmethodID m_SendNewFrameEventMethod;
    static jmethodID m_SendFrameSizeChangedEventMethod;
    static jmethodID m_SendAudioTrackEventMethod;
    static jmethodID m_SendVideoTrackEventMethod;
    static jmethodID m_SendSubtitleTrackEventMethod;
    static jmethodID m_SendMarkerEventMethod;
    static jmethodID m_SendBufferProgressEventMethod;
    static jmethodID m_SendDurationUpdateEventMethod;
    static jmethodID m_SendAudioSpectrumEventMethod;

    bool SendToJava_PlayerMediaErrorEvent(int errorCode);
    bool SendToJava_PlayerHaltEvent(const char* message, double time);
    bool SendToJava_PlayerStateEvent(long newJavaState, double presentTime);
    bool SendToJava_NewFrameEvent(CVideoFrame* pVideoFrame);
    bool SendToJava_FrameSizeChangedEvent(int width, int height);
    bool SendToJava_AudioTrackEvent(CAudioTrack* pTrack);
    bool SendToJava_VideoTrackEvent(CVideoTrack* pTrack);
    bool SendToJava_SubtitleTrackEvent(CSubtitleTrack* pTrack);
    bool SendToJava_MarkerEvent(string name, double time);
    bool SendToJava_BufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position);
    bool SendToJava_StopReachedEvent(double time);
    bool SendToJava_DurationUpdateEvent(double time);
    bool SendToJava_AudioSpectrumEvent(double time, double duration);

    static jobject CreateObject(JNIEnv *env, jmethodID *cid,
                                const char* class_name, const char* signature,
                                jvalue* value);
    static jobject CreateBoolean(JNIEnv *env, jboolean boolean_value);
    static jobject CreateInteger(JNIEnv *env, jint int_value);
    static jobject CreateLong(JNIEnv *env, jlong long_value);
    static jobject CreateDouble(JNIEnv *env, jdouble double_value);
    static jobject CreateDuration(JNIEnv *env, jlong duration);
};

#endif // _JAVA_PLAYER_EVENT_DISPATCHER_H_
