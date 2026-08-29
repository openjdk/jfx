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


#include "Font.h"
#include "FontRanges.h"
#include "FontDescription.h"
#include "FontPlatformData.h"
#include "FontSelector.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"
#include "WKJPlatformJava.h"

#include <wtf/Assertions.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/CString.h>

namespace WebCore {

void Font::platformInit()
{
    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont)
        return;

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb)
        return;

    wkj_ref font = wkj_ref(*jFont);

    if (cb->font_get_x_height) {
        m_fontMetrics.setXHeight(cb->font_get_x_height(font));
        wkjCheckAndClearException();
    }

    if (cb->font_get_cap_height) {
        m_fontMetrics.setCapHeight(cb->font_get_cap_height(font));
        wkjCheckAndClearException();
    }

    if (cb->font_get_ascent) {
        m_fontMetrics.setAscent(cb->font_get_ascent(font));
        wkjCheckAndClearException();
    }

    if (cb->font_get_descent) {
        m_fontMetrics.setDescent(cb->font_get_descent(font));
        wkjCheckAndClearException();
    }

    if (cb->font_get_line_spacing) {
        // Match CoreGraphics metrics.
        m_fontMetrics.setLineSpacing(lroundf(cb->font_get_line_spacing(font)));
        wkjCheckAndClearException();
    }

    if (cb->font_get_line_gap) {
        m_fontMetrics.setLineGap(cb->font_get_line_gap(font));
        wkjCheckAndClearException();
    }
}

void Font::determinePitch()
{
    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont) {
        m_treatAsFixedPitch = true;
        return;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_has_uniform_line_metrics)
        return;

    m_treatAsFixedPitch = cb->font_has_uniform_line_metrics(wkj_ref(*jFont)) != 0;
    wkjCheckAndClearException();
}

void Font::platformCharWidthInit()
{
    m_avgCharWidth = 0.f;
    m_maxCharWidth = 0.f;
    initCharWidths();
}

void Font::platformDestroy()
{
    notImplemented();
}

RefPtr<Font> Font::platformCreateScaledFont(const FontDescription&, float scaleFactor) const
{
    return Font::create(*m_platformData.derive(scaleFactor), origin(), IsInterstitial::No);
}

float Font::platformWidthForGlyph(Glyph c) const
{
    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont)
        return 0.0f;

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_get_glyph_width)
        return 0.0f;

    float res = static_cast<float>(cb->font_get_glyph_width(wkj_ref(*jFont), static_cast<int32_t>(c)));
    wkjCheckAndClearException();

    return res;
}

FloatRect Font::platformBoundsForGlyph(Glyph c) const
{
    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont) {
        return {};
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_get_glyph_bounding_box)
        return {};

    // The JNI version read the float[4] without testing it for null, so a Java implementation
    // returning null crashed here. The slot reports that case instead; nothing else changes.
    float xywh[4] = { 0.f, 0.f, 0.f, 0.f };
    int32_t haveBox = cb->font_get_glyph_bounding_box(wkj_ref(*jFont), static_cast<int32_t>(c), xywh);
    wkjCheckAndClearException();
    if (!haveBox)
        return {};

    return FloatRect { xywh[0], xywh[1], xywh[2], xywh[3] };
}

Path Font::platformPathForGlyph(Glyph) const
{
    notImplemented();
    return Path();
}

bool Font::platformSupportsCodePoint(char32_t character, std::optional<char32_t> variation) const
{
    return variation ? false : glyphForCharacter(character);
}

ResolvedEmojiPolicy FontCascade::resolveEmojiPolicy(FontVariantEmoji fontVariantEmoji, char32_t)
{
    // FIXME: https://bugs.webkit.org/show_bug.cgi?id=259205 We can't return RequireText or RequireEmoji
    // unless we have a way of knowing whether a font/glyph is color or not.
    switch (fontVariantEmoji) {
    case FontVariantEmoji::Normal:
    case FontVariantEmoji::Unicode:
        return ResolvedEmojiPolicy::NoPreference;
    case FontVariantEmoji::Text:
        return ResolvedEmojiPolicy::RequireText;
    case FontVariantEmoji::Emoji:
        return ResolvedEmojiPolicy::RequireEmoji;
    }
    return ResolvedEmojiPolicy::NoPreference;
}

RefPtr<Font> Font::platformCreateHalfWidthFont() const
{
     return nullptr;
}
}
