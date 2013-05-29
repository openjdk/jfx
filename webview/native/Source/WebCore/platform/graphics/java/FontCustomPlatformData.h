/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef FontCustomPlatformData_h
#define FontCustomPlatformData_h

#include "FontOrientation.h"
#include "FontRenderingMode.h"
#include "FontWidthVariant.h"
#include "TextOrientation.h"
#include <wtf/Forward.h>
#include <wtf/Noncopyable.h>

namespace WebCore {

class FontPlatformData;
class SharedBuffer;

struct FontCustomPlatformData {
    WTF_MAKE_NONCOPYABLE(FontCustomPlatformData);
public:
    FontCustomPlatformData(const JLObject& data);
    ~FontCustomPlatformData();

    FontPlatformData fontPlatformData(
            int size,
            bool bold,
            bool italic,
            FontOrientation = Horizontal,
            TextOrientation = TextOrientationVerticalRight,
            FontWidthVariant = RegularWidth,
            FontRenderingMode = NormalRenderingMode);

    static bool supportsFormat(const String&);

private:
    JGObject m_data;
};

FontCustomPlatformData* createFontCustomPlatformData(SharedBuffer*);

}
#endif
