/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef FontCustomPlatformData_h
#define FontCustomPlatformData_h

#include "FontOrientation.h"
#include "FontRenderingMode.h"
#include "FontWidthVariant.h"
#include "RenderStyleConstants.h"
#include <wtf/Forward.h>
#include <wtf/Noncopyable.h>

#include <JavaRef.h> // todo tav remove when building w/ pch

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
            FontWidthVariant = RegularWidth,
            FontRenderingMode = NormalRenderingMode);

    static bool supportsFormat(const String&);

private:
    JGObject m_data;
};

std::unique_ptr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer&);

}
#endif
