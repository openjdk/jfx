/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "ComplexTextController.h"
#include "FloatRect.h"
#include "FontCascade.h"
#include "WKJPlatformJava.h"

#include <wtf/Vector.h>
#include <wtf/text/MakeString.h>

namespace WebCore {

namespace {

int32_t runIsLeftToRight(wkj_ref jRun)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_is_left_to_right)
        return 0;
    return cb->text_run_is_left_to_right(jRun);
}

unsigned runGlyphCount(wkj_ref jRun)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_glyph_count)
        return 0;
    return static_cast<unsigned>(cb->text_run_get_glyph_count(jRun));
}

unsigned runStart(wkj_ref jRun)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_start)
        return 0;
    return static_cast<unsigned>(cb->text_run_get_start(jRun));
}

unsigned runEnd(wkj_ref jRun)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_end)
        return 0;
    return static_cast<unsigned>(cb->text_run_get_end(jRun));
}

unsigned runCharOffset(wkj_ref jRun, unsigned glyphIndex)
{
    if (!runGlyphCount(jRun)) {
        // Return same value as TextRun.getCharOffset() when there is
        // no glyph information available.
        return glyphIndex;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_char_offset)
        return glyphIndex;

    return static_cast<unsigned>(cb->text_run_get_char_offset(jRun, static_cast<int32_t>(glyphIndex)));
}

CGGlyph runGlyph(wkj_ref jRun, unsigned glyphIndex)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_glyph)
        return 0;
    return cb->text_run_get_glyph(jRun, static_cast<int32_t>(glyphIndex));
}

FloatRect runGlyphPosAndAdvance(wkj_ref jRun, unsigned glyphIndex)
{
    if (!runGlyphCount(jRun)) {
        return { };
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->text_run_get_glyph_pos_and_advance)
        return { };

    // The JNI version read the float[4] without testing it for null; the slot reports that
    // case instead of dereferencing nothing.
    float xywh[4] = { 0.f, 0.f, 0.f, 0.f };
    int32_t havePos = cb->text_run_get_glyph_pos_and_advance(jRun, static_cast<int32_t>(glyphIndex), xywh);
    wkjCheckAndClearException();
    if (!havePos)
        return { };

    return FloatRect { xywh[0], xywh[1], xywh[2], xywh[3] };
}

FloatSize runInitialAdvance(wkj_ref jRun)
{
    // FIXME(arajkumar): There is no way to get initial advance from Prism Font implementation.
    // With trial and error I found that glyph 0's x,y position can be used as an alternative
    // for initial advance.
    return runGlyphPosAndAdvance(jRun, 0).location() - FloatPoint();
}

}

ComplexTextController::ComplexTextRun::ComplexTextRun(wkj_ref jRun, const Font& font, const UChar* characters, unsigned stringLocation, unsigned stringLength)
    : m_initialAdvance(runInitialAdvance(jRun))
    , m_font(font)
    , m_characters(characters, stringLength)
    , m_stringLength(stringLength)
    , m_indexBegin(runStart(jRun))
    , m_indexEnd(runEnd(jRun))
    , m_glyphCount(runGlyphCount(jRun))
    , m_stringLocation(stringLocation)
    , m_isLTR(runIsLeftToRight(jRun) != 0)
{
    // Handle empty string runs (line breaks, etc.)
    if (m_stringLength == 0) {
        m_glyphCount = 0;
        return;
    }
   // Fallback run if no glyphs were generated
   if (m_glyphCount == 0) {
       m_glyphCount = 1;
       m_glyphs.grow(m_glyphCount);
       m_baseAdvances.grow(m_glyphCount);
       m_coreTextIndices.grow(m_glyphCount);

       m_glyphs[0] = 0;
       m_baseAdvances[0] = m_initialAdvance;
       m_coreTextIndices[0] = m_stringLocation;
       return;
    }

    m_glyphs.grow(m_glyphCount);
    m_baseAdvances.grow(m_glyphCount);
    // There is no way to get glyph origin from Prism Font implementation.
    // m_glyphOrigins.grow(m_glyphCount);
    m_coreTextIndices.grow(m_glyphCount);

    for (unsigned i = 0; i < m_glyphCount; ++i) {
        // The given string will be broken down into multiple java TextRuns. Each
        // java TextRun will have indicies relative to it's text. So it has to
        // be converted to absolute index w.r.t WebCore String.
        // Refer {CTGlyphLayout, DWGlyphLayout, PangoGlyphLayout}.layout()
        m_coreTextIndices[i] = m_indexBegin + runCharOffset(jRun, i);

        m_glyphs[i]= runGlyph(jRun, i);
        if (m_font->isZeroWidthSpaceGlyph(m_glyphs[i])) {
            m_baseAdvances[i] = { };
            continue;
        }

        auto glyphBox = runGlyphPosAndAdvance(jRun, i);
        m_baseAdvances[i] = glyphBox.size();
    }
}

void ComplexTextController::collectComplexTextRunsForCharacters(std::span<const UChar> characters, unsigned stringLocation, const Font* font)
{
    auto jFont = font ? font->platformData().nativeFontData() : nullptr;
    if (!font) {
        // Create a run of missing glyphs from the primary font.
        m_complexTextRuns.append(ComplexTextRun::create(m_fontCascade->primaryFont(), std::span<const UChar>(characters.data(), characters.size()), stringLocation, 0, characters.size(), m_run->ltr()));
        return;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->font_get_text_runs || !jFont) {
        m_complexTextRuns.append(ComplexTextRun::create(m_fontCascade->primaryFont(), std::span<const UChar>(characters.data(), characters.size()), stringLocation, 0, characters.size(), m_run->ltr()));
        return;
    }

    /*
     * WCFont.getTextRuns(String) returned a WCTextRun[]; the slot writes ids into a buffer and
     * returns the total, so the buffer is sized first from an upper bound that always holds -
     * the runs partition the string, so there can never be more of them than there are code
     * units - and the retry below exists only to honour the contract, not because it fires.
     * Every id written is owned here and released once its ComplexTextRun has been built,
     * which is the scope the JNI local refs had.
     */
    auto text = makeString(characters, characters.size());
    WKJStringArg textArg(text);

    Vector<wkj_ref, 32> runIds(characters.size());
    int32_t total = cb->font_get_text_runs(wkj_ref(*jFont), textArg.data(), textArg.length(),
                                           runIds.span().data(), static_cast<int32_t>(runIds.size()));
    if (total > static_cast<int32_t>(runIds.size())) {
        runIds.grow(static_cast<size_t>(total));
        total = cb->font_get_text_runs(wkj_ref(*jFont), textArg.data(), textArg.length(),
                                       runIds.span().data(), total);
    }
    wkjCheckAndClearException();

    if (total < 0) {
        // Create a run of missing glyphs from the primary font.
        m_complexTextRuns.append(ComplexTextRun::create(m_fontCascade->primaryFont(), std::span<const UChar>(characters.data(), characters.size()), stringLocation, 0, characters.size(), m_run->ltr()));
        return;
    }

    for (int32_t i = 0; i < total; i++) {
        m_complexTextRuns.append(ComplexTextRun::create(runIds[i], *font, characters.data(), stringLocation, characters.size()));
        WKJRelease(runIds[i]);
    }
}

}  // namespace WebCore
