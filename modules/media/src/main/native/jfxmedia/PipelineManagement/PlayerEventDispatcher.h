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

#ifndef _PLAYER_EVENT_DISPATCHER_H_
#define _PLAYER_EVENT_DISPATCHER_H_

#include <list>
#include <string>
#include <stdint.h>

using namespace std;

class CVideoFrame;
class CAudioTrack;
class CVideoTrack;

class CPlayerEventDispatcher
{
public:
    virtual ~CPlayerEventDispatcher() {}

    virtual bool SendPlayerMediaErrorEvent(int errorCode) = 0;
    virtual bool SendPlayerHaltEvent(const char* message, double msgTime) = 0;
    virtual bool SendPlayerStateEvent(int newState, double presentTime) = 0;
    virtual bool SendNewFrameEvent(CVideoFrame* pVideoFrame) = 0;
    virtual bool SendFrameSizeChangedEvent(int width, int height) = 0;
    virtual bool SendAudioTrackEvent(CAudioTrack* pTrack) = 0;
    virtual bool SendVideoTrackEvent(CVideoTrack* pTrack) = 0;
    virtual bool SendMarkerEvent(string name, double time) = 0;
    virtual bool SendBufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position) = 0;
    virtual bool SendDurationUpdateEvent(double time) = 0;
    virtual bool SendAudioSpectrumEvent(double time, double duration) = 0;
    virtual void Warning(int warningCode, const char* warningMessage) = 0;
};
#endif // _PLAYER_EVENT_DISPATCHER_H_
