/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ChromiumBridge_h
#define ChromiumBridge_h

#include "PluginData.h"
#include <wtf/Vector.h>

namespace WebCore {
    class ChromiumBridge {
    public:
#if ENABLE(SKIA)
        static const char *getSerifFontFamily();
        static const char *setFixedFontFamily();
        static const char *getSansSerifFontFamily();
        static const char *getStandardFontFamily();
        static const char *getCursiveFontFamily();
        static const char *getFantasyFontFamily();

        static String getFontFamilyForCharacters(const UChar* characters, size_t length);
#endif
        // Plugin -------------------------------------------------------------
        static void plugins(bool refresh, Vector<PluginInfo>*);
        static String computedDefaultLanguage() { return "en"; }
    };
}

#endif
