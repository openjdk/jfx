/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformJavaClasses.h"
#include <wtf/text/MakeString.h>

namespace WebCore {

namespace {

jclass PG_GetTextRun(JNIEnv* env)
{
    static JGClass textRunCls(
        env->FindClass("com/sun/webkit/graphics/WCTextRun"));
    ASSERT(textRunCls);
    return textRunCls;
}

bool jIsLTR(jobject jRun)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "isLeftToRight",
        "()Z");
    ASSERT(mID);

    return env->CallBooleanMethod(jRun, mID) == JNI_TRUE;
}

unsigned jGetGlyphCount(jobject jRun)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getGlyphCount",
        "()I");
    ASSERT(mID);

    return env->CallIntMethod(jRun, mID);
}

unsigned jGetStart(jobject jRun)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getStart",
        "()I");
    ASSERT(mID);

    return env->CallIntMethod(jRun, mID);
}

unsigned jGetEnd(jobject jRun)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getEnd",
        "()I");
    ASSERT(mID);

    return env->CallIntMethod(jRun, mID);
}

unsigned jGetCharOffset(jobject jRun, unsigned glyphIndex)
{
    if (!jGetGlyphCount(jRun)) {
        // Return same value as TextRun.getCharOffset() when there is
        // no glyph information available.
        return glyphIndex;
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getCharOffset",
        "(I)I");
    ASSERT(mID);

    return env->CallIntMethod(jRun, mID, glyphIndex);
}

CGGlyph jGetGlyph(jobject jRun, unsigned glyphIndex)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getGlyph",
        "(I)I");
    ASSERT(mID);

    return env->CallIntMethod(jRun, mID, glyphIndex);
}

FloatRect jGetGlyphPosAndAdvance(jobject jRun, unsigned glyphIndex)
{
    if (!jGetGlyphCount(jRun)) {
        return { };
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mID = env->GetMethodID(
        PG_GetTextRun(env),
        "getGlyphPosAndAdvance",
        "(I)[F");
    ASSERT(mID);

    JLocalRef<jfloatArray> jpos = static_cast<jfloatArray> (env->CallObjectMethod(
                                                              jRun, mID, glyphIndex));
    WTF::CheckAndClearException(env);

    jfloat* pos = static_cast<float*>(env->GetPrimitiveArrayCritical(jpos, 0));
    FloatRect rect = { pos[0], pos[1], pos[2], pos[3] };
    env->ReleasePrimitiveArrayCritical(jpos, pos, 0);
    return rect;
}

FloatSize jGetInitialAdvance(JLObject jRun)
{
    // FIXME(arajkumar): There is no way to get initial advance from Prism Font implementation.
    // With trial and error I found that glyph 0's x,y position can be used as an alternative
    // for initial advance.
    return jGetGlyphPosAndAdvance(jRun, 0).location() - FloatPoint();
}

}

ComplexTextController::ComplexTextRun::ComplexTextRun(JLObject jRun, const Font& font, const UChar* characters, unsigned stringLocation, unsigned stringLength)
    : m_initialAdvance(jGetInitialAdvance(jRun))
    , m_font(font)
    , m_characters(characters)
    , m_stringLength(stringLength)
    , m_indexBegin(jGetStart(jRun))
    , m_indexEnd(jGetEnd(jRun))
    , m_glyphCount(jGetGlyphCount(jobject(jRun)))
    , m_stringLocation(stringLocation)
    , m_isLTR(jIsLTR(jobject(jRun)))
{
    if (!m_glyphCount) {
        // There won't be any glyph when TextRun contains a line break or a soft break.
        // However WebCore expects us to return a empty value for all of it's query,
        // Setting m_glyphCount to 1 does the job.
        m_glyphCount = 1;
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
        m_coreTextIndices[i] = m_indexBegin + jGetCharOffset(jRun, i);

        m_glyphs[i]= jGetGlyph(jRun, i);
        if (m_font.isZeroWidthSpaceGlyph(m_glyphs[i])) {
            m_baseAdvances[i] = { };
            continue;
        }

        auto glyphBox = jGetGlyphPosAndAdvance(jRun, i);
        m_baseAdvances[i] = glyphBox.size();
    }
}

void ComplexTextController::collectComplexTextRunsForCharacters(std::span<const UChar> characters, unsigned stringLocation, const Font* font)
{
    auto jFont = font ? font->platformData().nativeFontData() : nullptr;
    if (!font) {
        // Create a run of missing glyphs from the primary font.
        m_complexTextRuns.append(ComplexTextRun::create(m_font.primaryFont(), characters.data(), stringLocation, characters.size(), 0, characters.size(), m_run.ltr()));
        return;
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID getTextRuns_mID = env->GetMethodID(
        PG_GetFontClass(env),
        "getTextRuns",
        "(Ljava/lang/String;)[Lcom/sun/webkit/graphics/WCTextRun;");
    ASSERT(getTextRuns_mID);

    JLocalRef<jobjectArray> jRuns = static_cast<jobjectArray> (env->CallObjectMethod(
                                                                  *jFont,
                                                                  getTextRuns_mID,
                                                                  jstring(makeString(characters, characters.size()).toJavaString(env))));
    WTF::CheckAndClearException(env);

    if (!jRuns) {
        // Create a run of missing glyphs from the primary font.
        m_complexTextRuns.append(ComplexTextRun::create(m_font.primaryFont(), characters.data(), stringLocation, characters.size(), 0, characters.size(), m_run.ltr()));
        return;
    }

    for (auto i = 0; i < env->GetArrayLength(jobjectArray(jRuns)); i++) {
        auto jRun = env->GetObjectArrayElement(jobjectArray(jRuns), i);
        m_complexTextRuns.append(ComplexTextRun::create(jRun, *font, characters.data(), stringLocation, characters.size()));
    }
}

}  // namespace WebCore
