/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "TextBreakIteratorInternalICU.h"

#include <wtf/StdLibExtras.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/CString.h>


namespace WTF {

static const char* UILanguage()
{
    // Chrome's UI language can be different from the OS UI language on Windows.
    // We want to return Chrome's UI language here.
    // DEPRECATED_DEFINE_STATIC_LOCAL(CString, locale, (WebCore::defaultLanguage().latin1()));
    // FIXME-java: Get default language from Java using Locale.getDefault()
    return "en";
}

const char* currentSearchLocaleID()
{
    return UILanguage();
}

const char* currentTextBreakLocaleID()
{
    return UILanguage();
}

} // namespace WebCore
