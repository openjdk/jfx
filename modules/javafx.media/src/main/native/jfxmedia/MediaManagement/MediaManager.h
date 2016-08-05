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

#ifndef _MEDIA_MANAGER_H_
#define _MEDIA_MANAGER_H_

#include "Media.h"
#include <Locator/Locator.h>
#include <Utils/MediaWarningDispatcher.h>
#include <Utils/Singleton.h>
#include "MediaWarningListener.h"
#include <PipelineManagement/PipelineOptions.h>
#include <stdint.h>

class CJfxCriticalSection;

/**
 * class CMediaManager
 *
 * Analagous to Jfxmedia's java version.  Entry point to creating a CMedia or CMediaPlayer.
 */
class CMediaManager
{
    friend class CMediaWarningDispatcher;
    friend class CPipeline;

public:
    virtual ~CMediaManager();

    static uint32_t   GetInstance(CMediaManager** ppMediaManager);

    void    SetWarningListener(CMediaWarningListener* pWarningListener);

    bool CanPlayContentType(string contentType);
    const ContentTypesList& GetSupportedContentTypes();

    uint32_t    CreatePlayer(CLocator* pLocator, CPipelineOptions* pOptions, CMedia** ppMedia);
    uint32_t    CreateMedia(CLocator* pLocator, CPipelineOptions* pOptions, CMedia** ppMedia);

protected:
    CMediaManager();

private:
    typedef Singleton<CMediaManager> MMSingleton;
    friend class Singleton<CMediaManager>;

    static uint32_t  CreateInstance(CMediaManager** ppMediaManager);

private:
    static MMSingleton          s_Singleton;
    CMediaWarningListener*      m_pWarningListener;
    uint32_t                    m_uInternalError;
};

#endif  //_MEDIA_MANAGER_H_
