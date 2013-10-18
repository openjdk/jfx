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

#ifndef _AUDIO_TRACK_H
#define _AUDIO_TRACK_H

#include "Track.h"

/**
 * class CAudioTrack
 *
 * Class representing an audio frame.
 */
class CAudioTrack : public CTrack
{
public:
    // Channel mask bits.
    static const int UNKNOWN      = 0x00;
    static const int FRONT_LEFT   = 0x01;
    static const int FRONT_RIGHT  = 0x02;
    static const int FRONT_CENTER = 0x04;
    static const int REAR_LEFT    = 0x08;
    static const int REAR_RIGHT   = 0x10;
    static const int REAR_CENTER  = 0x20;

public:
    CAudioTrack(int64_t trackID, string name, Encoding encoding, bool enabled,
                string language, int numChannels, int channelMask, float sampleRate);
    ~CAudioTrack();

    virtual string GetLanguage();
    virtual int    GetNumChannels();
    virtual int    GetChannelMask();
    virtual float  GetSampleRate();

protected:
    CAudioTrack();

protected:
    string m_language;
    int    m_iNumChannels;
    int    m_iChannelMask;
    float  m_fSampleRate;
};

#endif // _AUDIO_TRACK_H
