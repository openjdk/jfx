/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Frame.h"
#include "NotImplemented.h"
#include "UserStyleSheetLoader.h"

namespace WebCore {

#if FRAME_LOADS_USER_STYLESHEET

void Frame::setUserStyleSheetLocation(const URL& url)
{
    delete m_userStyleSheetLoader;
    m_userStyleSheetLoader = 0;
    if (m_doc && m_doc->docLoader())
        m_userStyleSheetLoader = new UserStyleSheetLoader(m_doc, url.string());
}

void Frame::setUserStyleSheet(const String& styleSheet)
{
    delete m_userStyleSheetLoader;
    m_userStyleSheetLoader = 0;
    if (m_doc)
        m_doc->setUserStyleSheet(styleSheet);
}

#endif

}
