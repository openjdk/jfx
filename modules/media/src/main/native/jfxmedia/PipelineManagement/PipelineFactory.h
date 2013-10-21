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

#ifndef _PIPELINE_FACTORY_H_
#define _PIPELINE_FACTORY_H_

#include "Pipeline.h"
#include <Locator/Locator.h>
#include <Utils/Singleton.h>
#include "PipelineOptions.h"
#include <stdint.h>

class CPipelineFactory
{
public:
    static uint32_t GetInstance(CPipelineFactory **ppPipelineFactory);

    virtual ~CPipelineFactory();

    virtual bool CanPlayContentType(string contentType) = 0;
    virtual const ContentTypesList& GetSupportedContentTypes() = 0;

    virtual uint32_t CreatePlayerPipeline(CLocator* locator, CPipelineOptions *pOptions, CPipeline** ppPipeline) = 0;

protected:
    CPipelineFactory();

    CVideoFrame::FrameType  m_videoFrameType;

private:
    typedef Singleton<CPipelineFactory> PFSingleton;
    friend class Singleton<CPipelineFactory>;

    static uint32_t CreateInstance(CPipelineFactory **ppPipelineFactory);
    static PFSingleton       s_Singleton;
};

#endif  //_PIPELINE_FACTORY_H_
