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

#include "MediaManager.h"
#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>

#include <PipelineManagement/PipelineFactory.h>
#include <Locator/Locator.h>

#include <platform/gstreamer/GstMediaManager.h>
#include <Utils/JfxCriticalSection.h>
#include <jfxmedia_errors.h>

CMediaManager::MMSingleton CMediaManager::s_Singleton;

//*************************************************************************************************
//********** Empty content types list in case PipelineFactory is not available.
//*************************************************************************************************
const static ContentTypesList EMPTY_LIST;

//*************************************************************************************************
//********** class CMediaManager
//*************************************************************************************************
CMediaManager::CMediaManager()
:   m_uInternalError(ERROR_NONE)
{}

CMediaManager::~CMediaManager()
{}

/**
 * CMediaManager::GetInstance()
 *
 * @return  CMediaManager* singleton
 */
uint32_t CMediaManager::GetInstance(CMediaManager** ppMediaManager)
{
    return s_Singleton.GetInstance(ppMediaManager);
}

/**
 * CMediaManager::CreateInstance() creates an instance of the class
 * This method is used by Singleton class to create the actual instace of a class.
 * When the method is protected of private, Singleton class should be a friend for the class.
 *
 * @return  CMediaManager* instance
 */
uint32_t CMediaManager::CreateInstance(CMediaManager** ppMediaManager)
{
#if ENABLE_PLATFORM_GSTREAMER
#if !defined(TARGET_OS_WIN32) && !defined(TARGET_OS_MAC) && !defined(TARGET_OS_LINUX)
    return ERROR_OS_UNSUPPORTED;
#else
    CGstMediaManager* pGstManager = new(nothrow) CGstMediaManager();
    if (NULL == pGstManager)
        return ERROR_MEMORY_ALLOCATION;

    if (ERROR_NONE != (pGstManager->m_uInternalError = pGstManager->Init()))
        return ERROR_MANAGER_CREATION;

    *ppMediaManager = pGstManager;

    return ERROR_NONE;
#endif  // !defined ...
#else
    return ERROR_PLATFORM_UNSUPPORTED;
#endif  //ENABLE_PLATFORM_GSTREAMER
}

/**
 * CMediaManager::SetWarningListener(const CMediaWarningListener* pWarningListener)
 *
 * Sets the listener to receive notifications of warnings
 * which are not specific to a given pipeline.
 *
 * @param   The listener.
 */
void CMediaManager::SetWarningListener(CMediaWarningListener* pWarningListener)
{
    m_pWarningListener = pWarningListener;
}

bool CMediaManager::CanPlayContentType(string contentType)
{
    CPipelineFactory*   pPipelineFactory = NULL;
    uint32_t            uRetCode;

    uRetCode = CPipelineFactory::GetInstance(&pPipelineFactory);
    if (ERROR_NONE != uRetCode)
        return false;
    else if (NULL == pPipelineFactory)
        return false;

    return pPipelineFactory->CanPlayContentType(contentType);
}

const ContentTypesList& CMediaManager::GetSupportedContentTypes()
{
    CPipelineFactory*   pPipelineFactory = NULL;
    uint32_t            uRetCode;

    uRetCode = CPipelineFactory::GetInstance(&pPipelineFactory);
    if (ERROR_NONE != uRetCode)
        return EMPTY_LIST;
    else if (NULL == pPipelineFactory)
        return EMPTY_LIST;

    return pPipelineFactory->GetSupportedContentTypes();
}

/**
 * CMediaManager::CreatePlayer(CLocator locator)
 *
 * @param   locator
 *
 * @return  Pointer to a new CMedia object.
 */
uint32_t CMediaManager::CreatePlayer(CLocator* pLocator, CPipelineOptions* pOptions, CMedia** ppMedia)
{
    CPipeline*          pPipeline = NULL;
    CPipelineFactory*   pPipelineFactory = NULL;
    uint32_t            uRetCode;

    if (NULL == pLocator)
        return ERROR_LOCATOR_NULL;

    uRetCode = CPipelineFactory::GetInstance(&pPipelineFactory);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    else if (NULL == pPipelineFactory)
        return ERROR_FACTORY_NULL;

    //***** Initialize the return value
    *ppMedia    = NULL;

    //***** If we have a null option object, create one
    if (NULL == pOptions)
    {
        pOptions = new (nothrow)CPipelineOptions();
        if (NULL == pOptions)
            return ERROR_MEMORY_ALLOCATION;
    }

    //***** Try to create a pipeline
    uRetCode = pPipelineFactory->CreatePlayerPipeline(pLocator, pOptions, &pPipeline);

    //***** Create the new CMedia object
    if (ERROR_NONE == uRetCode)
    {
        //***** Try to create a CMedia to associate with the pipeline
        *ppMedia = new(nothrow) CMedia(pPipeline);

        if (NULL == *ppMedia)
        {
            //Cleanup if media creation failed.
            delete pPipeline;
            uRetCode = ERROR_MEDIA_CREATION;
        }
    }

    return uRetCode;
}

/**
 * CMediaManager::CreateMedia(CLocator locator)
 *
 * Creates a media object, given a locator and a set of options.
 *
 * @param   pLocator    pointer to a CLocator object
 * @param   pOptions    pointer to a CPipelienOptions object
 *
 * @return  Pointer to a new CMedia object.
 */
uint32_t CMediaManager::CreateMedia(CLocator* pLocator, CPipelineOptions* pOptions, CMedia** ppMedia)
{
    CPipeline*          pPipeline = NULL;
    CPipelineFactory*   pPipelineFactory = NULL;
    uint32_t            uRetCode;

    if (NULL == pLocator)
        return ERROR_LOCATOR_NULL;

    uRetCode = CPipelineFactory::GetInstance(&pPipelineFactory);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    else if (NULL == pPipelineFactory)
        return ERROR_FACTORY_NULL;

    //***** Initialize the return value
    *ppMedia    = NULL;

    //***** If we have a null option object, create one
    if (NULL == pOptions)
    {
        pOptions = new (nothrow) CPipelineOptions();
        if (NULL == pOptions)
            return ERROR_MEMORY_ALLOCATION;
    }

    //***** Do the real work
    if ((CPipelineOptions::kAudioPlaybackPipeline == pOptions->GetPipelineType()) || (CPipelineOptions::kAVPlaybackPipeline == pOptions->GetPipelineType()))
    {
        //***** Create a player pipleine first
#if JFXMEDIA_DEBUG
        printf("-- CreateMedia : create player pipeline\n");
#endif
        uRetCode = pPipelineFactory->CreatePlayerPipeline(pLocator, pOptions, &pPipeline);

        //***** Create the new CMedia object
        if (ERROR_NONE == uRetCode)
        {
            //***** Create a media object and attach the pipeline to the media object
            *ppMedia    = new(nothrow) CMedia(pPipeline);

            if (NULL == *ppMedia)
            {
                //Cleanup if media creation failed.
                delete pPipeline;
                uRetCode = ERROR_MEDIA_CREATION;
            }
        }
    }

#if JFXMEDIA_DEBUG
        printf("-- CreateMedia : finish\n");
#endif
    return uRetCode;
}
