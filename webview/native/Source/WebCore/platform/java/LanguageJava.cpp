/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "Language.h"

#include "PlatformString.h"
#include "ChromiumBridge.h"
#include <wtf/Vector.h>

namespace WebCore {

static String platformLanguage()
{
    DEFINE_STATIC_LOCAL(String, computedDefaultLanguage, ());
    if (computedDefaultLanguage.isEmpty())
        computedDefaultLanguage.append(ChromiumBridge::computedDefaultLanguage());
    return computedDefaultLanguage;
}

String platformDefaultLanguage()
{
    return platformLanguage();
}

Vector<String> platformUserPreferredLanguages()
{
    Vector<String> userPreferredLanguages;
    userPreferredLanguages.append(platformLanguage());
    return userPreferredLanguages;
}

} // namespace WebCore
