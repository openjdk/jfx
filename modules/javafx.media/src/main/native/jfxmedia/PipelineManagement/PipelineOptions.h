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

#ifndef _PIPELINE_OPTIONS_H_
#define _PIPELINE_OPTIONS_H_

#include <string.h>
#include <PipelineManagement/VideoFrame.h>
#include <jni/Logger.h>
#include <list>
#include <string>

using namespace std;
typedef list<string> ContentTypesList;

class CPipelineOptions
{
public:
    enum
    {
        kAudioPlaybackPipeline  = 0,
        kAVPlaybackPipeline     = 1
    };
public:
    CPipelineOptions(int pipelineType=kAVPlaybackPipeline, bool havePreferredFormat = false)
    :   m_PipelineType(pipelineType),
        m_bBufferingEnabled(false),
        m_StreamMimeType(-1),
        m_bHLSModeEnabled(false)
    {}

    virtual ~CPipelineOptions() {}

    inline int  GetPipelineType() { return m_PipelineType; }

    inline void SetBufferingEnabled(bool enabled) { m_bBufferingEnabled = enabled; }
    inline bool GetBufferingEnabled() { return m_bBufferingEnabled; }

    inline void SetStreamMimeType(int streamMimeType) { m_StreamMimeType = streamMimeType; }
    inline int GetStreamMimeType() { return m_StreamMimeType; }

    inline void SetHLSModeEnabled(bool enabled) { m_bHLSModeEnabled = enabled; }
    inline bool GetHLSModeEnabled() { return m_bHLSModeEnabled; }

private:
    int         m_PipelineType;
    bool        m_bBufferingEnabled;
    int         m_StreamMimeType;
    bool        m_bHLSModeEnabled;
};

#endif  //_PIPELINE_OPTIONS_H_
