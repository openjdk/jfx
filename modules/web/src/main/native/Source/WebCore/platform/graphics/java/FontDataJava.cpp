/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"


#include "Font.h"
#include "FontData.h"
#include "FontDescription.h"
#include "FontPlatformData.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"
#include "SimpleFontData.h"

#include <wtf/Assertions.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/CString.h>

namespace WebCore {

void SimpleFontData::platformInit()
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
    m_fontMetrics.setLineSpacing(env->CallFloatMethod(*jFont, getLineSpacing_mID));
    CheckAndClearException(env);

    static jmethodID getLineGap_mID = env->GetMethodID(PG_GetFontClass(env),
        "getLineGap", "()F");
    ASSERT(getLineGap_mID);
    m_fontMetrics.setLineGap(env->CallFloatMethod(*jFont, getLineGap_mID));
    CheckAndClearException(env);
}

void SimpleFontData::determinePitch()
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

void SimpleFontData::platformCharWidthInit()
{
    m_avgCharWidth = 0.f;
    m_maxCharWidth = 0.f;
    initCharWidths();
}

void SimpleFontData::platformDestroy()
{
    notImplemented();
}

bool SimpleFontData::containsCharacters(const UChar *characters, int length) const
{
    notImplemented();
    return true;
}

PassRefPtr<SimpleFontData> SimpleFontData::platformCreateScaledFontData(const FontDescription& fontDescription, float scaleFactor) const
{
    return SimpleFontData::create(*m_platformData.derive(scaleFactor), isCustomFont(), false);
}

float SimpleFontData::platformWidthForGlyph(Glyph c) const
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

FloatRect SimpleFontData::platformBoundsForGlyph(Glyph) const
{
    return FloatRect(); //That is OK! platformWidthForGlyph impl is enough.
}


}
