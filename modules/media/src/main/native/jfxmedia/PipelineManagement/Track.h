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

#ifndef _TRACK_H
#define _TRACK_H

#include <string>
#include <stdint.h>

using namespace std;


/**
 * class CTrack
 *
 * Class representing an either an audio or video track.
 */
class CTrack
{
public:
    enum Encoding
    {
        NONE,

        // Audio encodings
        PCM,                // Uncompressed PCM
        MPEG1AUDIO,         // MPEG1 Audio (layer1,2)
        MPEG1LAYER3,        // MPEG1 Layer3 (mp3)
        AAC,                // Advanced Audio Coding

        // Video encodings
        H264,
        VP6,                // On2 VP6

        // custom encoding
        CUSTOM
    };

public:
    CTrack(int64_t trackID, string name, Encoding encoding, bool enabled);
    virtual ~CTrack();

    bool isEnabled();
    int64_t GetTrackID();
    string    GetName();
    Encoding  GetEncoding();

protected:
    CTrack() {}

protected:
    bool m_trackEnabled;
    int64_t m_trackID;
    string m_name;
    Encoding m_encoding;
};
#endif // _TRACK_H
