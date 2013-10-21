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

#include "Media.h"
#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <jni/Logger.h>

//*************************************************************************************************
//********** class CMedia
//*************************************************************************************************

/**
 * CMedia::CMedia()
 *
 * Constructor.  A CPipeline object must be associated with the CMedia instance.  It's the only
 * way for the CMedia object to get information about the media itself.
 *
 * @param   pPipe   CPipeline object to associate with the CMedia
 */
CMedia::CMedia(CPipeline *pPipe)
{
    LOGGER_LOGMSG(LOGGER_DEBUG, "CMedia::CMedia()");
    m_pPipeline = pPipe;
}

/**
 * CMedia::~CMedia()
 *
 * Destructor
 */
CMedia::~CMedia()
{
    LOGGER_LOGMSG(LOGGER_DEBUG, "CMedia::~CMedia()");

    if (m_pPipeline != NULL)
    {
        m_pPipeline->Dispose();
        delete m_pPipeline;
        m_pPipeline = NULL;
    }
}

/**
 * CMedia::IsValid(CMedia* pMedia)
 *
 * Checks if the CMedia object is valid
 *
 * @param   pMedia  pointer to a CMedia object
 *
 * @return  true/false
 */
bool CMedia::IsValid(CMedia* pMedia)
{
    bool    bValid = false;

    //***** CMedia is valid if there is a pipeline associated with the media
    bValid = (NULL != pMedia) && (NULL != pMedia->m_pPipeline);

    return bValid;
}

/**
 * CMedia::GetPipeline()
 *
 * Returns a pPointer to the associated CPipeline object.
 */
CPipeline* CMedia::GetPipeline()
{
    return m_pPipeline;
}
