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

#include "FontPlatformData.h"
#include "FontCustomPlatformData.h"
#include "FontDescription.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"
#include "WKJPlatformJava.h"

#include <wtf/Assertions.h>
#include <wtf/text/CString.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

namespace {

RefPtr<RQRef> getJavaFont(const String& family, float size, bool italic, bool bold)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->get_font)
        return nullptr;

    WKJStringArg familyArg(family);
    WKJHandle wcFont { cb->get_font(familyArg.data(), familyArg.length(),
                                    bold ? 1 : 0, italic ? 1 : 0, size) };

    wkjCheckAndClearException();

    return RQRef::create(wcFont.get());
}
}

/*
 * m_jFont holds the Java WCFont for the whole lifetime of this FontPlatformData: RQRef owns a
 * wkj_ref, and the release point is ~FontPlatformData dropping the last RefPtr<RQRef>, which
 * runs ~RQRef and with it WKJHandle::~WKJHandle -> host->core.release. There is no explicit
 * dispose call and there never was; this replaces the global reference inside RQRef exactly.
 */
FontPlatformData::FontPlatformData(RefPtr<RQRef> font, float size)
    : m_jFont(font)
    , m_size(size)
{
}

std::unique_ptr<FontPlatformData> FontPlatformData::create(
        const FontDescription& fontDescription, const AtomString& family)
{
    RefPtr<RQRef> wcFont = getJavaFont(
            family,
            fontDescription.computedSize(),
            isItalic(fontDescription.fontStyleSlope()),
            fontDescription.weight() >= boldWeightValue());
    return !wcFont ? nullptr : std::make_unique<FontPlatformData>(wcFont, fontDescription.computedSize());
}

std::unique_ptr<FontPlatformData> FontPlatformData::derive(float scaleFactor) const
{
    ASSERT(m_jFont);
    float size = m_size * scaleFactor;

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_derive)
        return nullptr;

    WKJHandle wcFont { cb->font_derive(wkj_ref(*m_jFont), size) };
    wkjCheckAndClearException();

    return std::make_unique<FontPlatformData>(RQRef::create(wcFont.get()), size);
}

bool FontPlatformData::platformIsEqual(const FontPlatformData& other) const
{
    if (m_jFont == other.m_jFont) {
        return true;
    }
    if (!m_jFont || isHashTableDeletedValue() ||
        !other.m_jFont || other.isHashTableDeletedValue()) {
        return false;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_equals)
        return false;

    int32_t res = cb->font_equals(wkj_ref(*m_jFont), wkj_ref(*other.m_jFont));
    wkjCheckAndClearException();

    return res != 0;
}

unsigned FontPlatformData::hash() const
{
    if (!m_jFont || isHashTableDeletedValue()) {
        return (unsigned)-1;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_hash_code)
        return 0;

    int32_t res = cb->font_hash_code(wkj_ref(*m_jFont));
    wkjCheckAndClearException();

    return static_cast<unsigned>(res);
}

#ifndef NDEBUG
String FontPlatformData::description() const
{
    notImplemented();
    return "Java font"_s;
}
#endif //NDEBUG

String FontPlatformData::familyName() const
{
    // FIXME: Not implemented yet.
    return { };
}

}
