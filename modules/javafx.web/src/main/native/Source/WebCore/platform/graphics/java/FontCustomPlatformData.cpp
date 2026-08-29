/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "FontCustomPlatformData.h"

#include "FontCreationContext.h"
#include "SharedBuffer.h"
#include "FontDescription.h"
#include "FontPlatformData.h"
#include "WKJPlatformJava.h"

namespace WebCore {

/*
 * m_data is the Java WCFontCustomPlatformData, held for the lifetime of this object. It is a
 * WKJHandle now rather than a global reference; the release point is ~FontCustomPlatformData,
 * which runs the handle destructor. (Declared in platform/graphics/FontCustomPlatformData.h,
 * outside this directory - see the migration report.)
 */
FontCustomPlatformData::FontCustomPlatformData(wkj_ref data, FontPlatformData::CreationData&& cdata)
    :creationData(cdata)
    ,m_data(WKJHandle::retained(data))
    ,m_renderingResourceIdentifier(RenderingResourceIdentifier::generate())
{
}

FontCustomPlatformData::~FontCustomPlatformData()
{
}

FontPlatformData FontCustomPlatformData::fontPlatformData(const FontDescription& fontDescription, bool bold, bool italic, const FontCreationContext&)
{
    int size = fontDescription.computedPixelSize();

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_custom_data_create_font)
        return FontPlatformData(nullptr, size);

    WKJHandle font { cb->font_custom_data_create_font(m_data.get(), size,
                                                      bold ? 1 : 0, italic ? 1 : 0) };
    wkjCheckAndClearException();

    return FontPlatformData(RQRef::create(font.get()), size);
}

RefPtr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer& buffer, const String& itemInCollection)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->create_shared_buffer || !cb->create_font_custom_platform_data)
        return nullptr;

    WKJHandle sharedBuffer { cb->create_shared_buffer(wkj_from_ptr(&buffer)) };
    wkjCheckAndClearException();

    WKJHandle data { cb->create_font_custom_platform_data(sharedBuffer.get()) };
    wkjCheckAndClearException();

    FontPlatformData::CreationData creationData = { buffer, WTF::String::fromUTF8("") };
    return data ? adoptRef(new FontCustomPlatformData(data.get(), WTF::move(creationData))) : nullptr;
}

bool FontCustomPlatformData::supportsFormat(const String& format)
{
    return equalLettersIgnoringASCIICase(format, "truetype"_s)
            || equalLettersIgnoringASCIICase(format, "opentype"_s)
            || equalLettersIgnoringASCIICase(format, "woff"_s);
}

bool FontCustomPlatformData::supportsTechnology(const FontTechnology&)
{
    // FIXME: define supported technologies for this platform (webkit.org/b/256310).
    return true;
}

RefPtr<FontCustomPlatformData> FontCustomPlatformData::create(SharedBuffer& buffer, const String& itemInCollection)
{
     return createFontCustomPlatformData(buffer,itemInCollection);
}

RefPtr<FontCustomPlatformData> FontCustomPlatformData::createMemorySafe(SharedBuffer&, const String&)
{
    return nullptr;
}

}
