/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "TextFlags.h"
#include "RenderStyleConstants.h"
#include <wtf/Forward.h>
#include <wtf/Noncopyable.h>

#include <wtf/java/JavaRef.h> // todo tav remove when building w/ pch

namespace WebCore {

class FontDescription;
class FontPlatformData;
class SharedBuffer;

struct FontCustomPlatformData {
    WTF_MAKE_NONCOPYABLE(FontCustomPlatformData);
public:
    FontCustomPlatformData(const JLObject& data);
    ~FontCustomPlatformData();

    FontPlatformData fontPlatformData(const FontDescription&, bool bold, bool italic);

    static bool supportsFormat(const String&);

private:
    JGObject m_data;
};

std::unique_ptr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer&);

} // namespace WebCore
