/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/Assertions.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/CString.h>

namespace WebCore {

void Font::platformInit()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont)
        return;

    static jmethodID getXHeight_mID = env->GetMethodID(PG_GetFontClass(env),
        "getXHeight", "()F");
    ASSERT(getXHeight_mID);
    m_fontMetrics.setXHeight(env->CallFloatMethod(*jFont, getXHeight_mID));
    CheckAndClearException(env);

    static jmethodID getCapHeight_mID = env->GetMethodID(PG_GetFontClass(env),
        "getCapHeight", "()F");
    ASSERT(getCapHeight_mID);
    m_fontMetrics.setCapHeight(env->CallFloatMethod(*jFont, getCapHeight_mID));
    CheckAndClearException(env);

    static jmethodID getAscent_mID = env->GetMethodID(PG_GetFontClass(env),
        "getAscent", "()F");
    ASSERT(getAscent_mID);
    m_fontMetrics.setAscent(env->CallFloatMethod(*jFont, getAscent_mID));
    CheckAndClearException(env);

    static jmethodID getDescent_mID = env->GetMethodID(PG_GetFontClass(env),
        "getDescent", "()F");
    ASSERT(getDescent_mID);
    m_fontMetrics.setDescent(env->CallFloatMethod(*jFont, getDescent_mID));
    CheckAndClearException(env);

    static jmethodID getLineSpacing_mID = env->GetMethodID(PG_GetFontClass(env),
        "getLineSpacing", "()F");
    ASSERT(getLineSpacing_mID);
    // Match CoreGraphics metrics.
    m_fontMetrics.setLineSpacing(lroundf(
        env->CallFloatMethod(*jFont, getLineSpacing_mID)));
    CheckAndClearException(env);

    static jmethodID getLineGap_mID = env->GetMethodID(PG_GetFontClass(env),
        "getLineGap", "()F");
    ASSERT(getLineGap_mID);
    m_fontMetrics.setLineGap(env->CallFloatMethod(*jFont, getLineGap_mID));
    CheckAndClearException(env);
}

void Font::determinePitch()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont) {
        m_treatAsFixedPitch = true;
        return;
    }

    static jmethodID hasUniformLineMetrics_mID = env->GetMethodID(
            PG_GetFontClass(env), "hasUniformLineMetrics", "()Z");
    ASSERT(hasUniformLineMetrics_mID);

    m_treatAsFixedPitch = jbool_to_bool(env->CallBooleanMethod(*jFont, hasUniformLineMetrics_mID));
    CheckAndClearException(env);
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
    return Font::create(*m_platformData.derive(scaleFactor), origin(), Interstitial::No);
}

float Font::platformWidthForGlyph(Glyph c) const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont)
        return 0.0f;

    static jmethodID getGlyphWidth_mID = env->GetMethodID(PG_GetFontClass(env),
        "getGlyphWidth", "(I)D");
    ASSERT(getGlyphWidth_mID);

    float res = env->CallDoubleMethod(*jFont, getGlyphWidth_mID, (jint)c);
    CheckAndClearException(env);

    return res;
}

FloatRect Font::platformBoundsForGlyph(Glyph c) const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = m_platformData.nativeFontData();
    if (!jFont) {
        return {};
    }

    static jmethodID getGlyphBoundingBox_mID = env->GetMethodID(PG_GetFontClass(env), "getGlyphBoundingBox", "(I)[F");
    ASSERT(getGlyphBoundingBox_mID);

    jfloatArray boundingBox = (jfloatArray)env->CallObjectMethod(*jFont, getGlyphBoundingBox_mID, (jint)c);
    jfloat *bBox = env->GetFloatArrayElements(boundingBox,0);
    auto bb = FloatRect { bBox[0], bBox[1], bBox[2], bBox[3] };
    env->ReleaseFloatArrayElements(boundingBox, bBox, 0);
    CheckAndClearException(env);
    return bb;
}

}
