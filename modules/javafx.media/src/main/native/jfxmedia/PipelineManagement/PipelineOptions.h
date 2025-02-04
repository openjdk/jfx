/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _PIPELINE_OPTIONS_H_
#define _PIPELINE_OPTIONS_H_

#include <list>
#include <string>

using namespace std;
typedef list<string> ContentTypesList;

class CPipelineOptions
{
public:
    enum
    {
        kSingleSourcePipeline  = 0, // Indicates that pipeline is single source. It can be audio or video.
        kAudioSourcePipeline   = 1, // Indicates that pipeline is multi source and audio is secondary stream.
    };
public:
    CPipelineOptions(int pipelineType = kSingleSourcePipeline)
    :   m_PipelineType(pipelineType),
        m_bBufferingEnabled(false),
        m_StreamMimeType(-1),
        m_AudioStreamMimeType(-1),
        m_bHLSModeEnabled(false),
        m_audioFlags(0)
    {}

    virtual ~CPipelineOptions() {}

    inline void SetPipelineType(int pipelineType) { m_PipelineType = pipelineType; }
    inline int  GetPipelineType() { return m_PipelineType; }

    inline void SetBufferingEnabled(bool enabled) { m_bBufferingEnabled = enabled; }
    inline bool GetBufferingEnabled() { return m_bBufferingEnabled; }

    inline void SetContentType(string contentType) { m_ContentType = contentType; }
    inline const string GetContentType() { return m_ContentType; }

    inline void SetStreamMimeType(int streamMimeType) { m_StreamMimeType = streamMimeType; }
    inline int GetStreamMimeType() { return m_StreamMimeType; }

    inline void SetAudioStreamMimeType(int audioStreamMimeType) { m_AudioStreamMimeType = audioStreamMimeType; }
    inline int GetAudioStreamMimeType() { return m_AudioStreamMimeType; }

    inline void SetHLSModeEnabled(bool enabled) { m_bHLSModeEnabled = enabled; }
    inline bool GetHLSModeEnabled() { return m_bHLSModeEnabled; }

    inline void  SetAudioFlags(int audioFlags) { m_audioFlags = audioFlags; }
    inline int  GetAudioFlags() { return m_audioFlags; }

    // Returns true if we need to force default track ID. For multi source streams
    // two demuxers (qtdemux in case of fMP4 HLS with EXT-X-MEDIA) will report same
    // ID, since two demuxers are not aware of each other and that we actually
    // have two streams. Our code expects unique ID. We do not have actual use
    // of IDs except they shoule be unique.
    inline bool ForceDefaultTrackID() { return (m_PipelineType == kAudioSourcePipeline); }

    inline CPipelineOptions* SetStreamParser(string streamParser) { m_StreamParser = streamParser; return this;}
    inline CPipelineOptions* SetAudioStreamParser(string ausioStreamParser) { m_AudioStreamParser = ausioStreamParser; return this;}
    inline CPipelineOptions* SetVideoDecoder(string videoDecoder) { m_VideoDecoder = videoDecoder; return this;}
    inline CPipelineOptions* SetAudioDecoder(string audioDecoder) { m_AudioDecoder = audioDecoder; return this;}

    inline const char* GetStreamParser() { return GetCharFromString(&m_StreamParser); }
    inline const char* GetAudioStreamParser() { return GetCharFromString(&m_AudioStreamParser); }
    inline const char* GetVideoDecoder() { return GetCharFromString(&m_VideoDecoder); }
    inline const char* GetAudioDecoder() { return GetCharFromString(&m_AudioDecoder); }

    inline const char* GetCharFromString(string *str) {
        if (str->empty())
            return NULL;
        else
            return str->c_str();
    }

private:
    int         m_PipelineType;
    bool        m_bBufferingEnabled;
    // ContentType based on content type of main URL.
    string      m_ContentType;
    // Main stream mime type, might be different than ContentType for HLS.
    int         m_StreamMimeType;
    // Audio stream mime type, might be different than ContentType
    // and main stream mime type HLS.
    int         m_AudioStreamMimeType;
    bool        m_bHLSModeEnabled;
    int         m_audioFlags;

    // Audio parser or demultiplexer for main stream
    string      m_StreamParser;
    // Audio parser or demultiplexer for audio stream
    string      m_AudioStreamParser;
    // Audio decoder. Will be used with main stream if audio stream is not
    // present or will be used with audio stream if present
    string      m_AudioDecoder;
    // Video decoder. Always used with main stream.
    string      m_VideoDecoder;
};

#endif  //_PIPELINE_OPTIONS_H_
