/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "GlyphPageTreeNode.h"
#include "GraphicsContextJava.h"
#include "SimpleFontData.h"

namespace WebCore {

bool GlyphPage::fill(
    unsigned offset, unsigned length,
    UChar* buffer, unsigned bufferLength,
    const SimpleFontData* fontData)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = fontData->platformData().nativeFontData();
    if (!jFont)
        return false;

    JLocalRef<jcharArray> jchars(env->NewCharArray(bufferLength));
    CheckAndClearException(env); // OOME
    ASSERT(jchars);

    jchar* chars = (jchar*)env->GetPrimitiveArrayCritical(jchars, NULL);
    ASSERT(chars);
    memcpy(chars, buffer, bufferLength * 2);
    env->ReleasePrimitiveArrayCritical(jchars, chars, 0);

    static jmethodID mid = env->GetMethodID(PG_GetFontClass(env), "getGlyphCodes", "([C)[I");
    ASSERT(mid);
    JLocalRef<jintArray> jglyphs(static_cast<jintArray>(env->CallObjectMethod(*jFont, mid, (jcharArray)jchars)));
    CheckAndClearException(env);

    Glyph* glyphs = (Glyph*)env->GetPrimitiveArrayCritical(jglyphs, NULL);
    ASSERT(glyphs);

    unsigned step;  // 1 for BMP, 2 for non-BMP
    if (bufferLength == length) {
        step = 1;
    } else if (bufferLength == 2 * length) {
        step = 2;
    } else {
        ASSERT_NOT_REACHED();
    }

    bool haveGlyphs = false;
    for (unsigned i = 0; i < length; i++) {
        Glyph glyph = glyphs[i * step];
        if (glyph) {
            haveGlyphs = true;
            setGlyphDataForIndex(offset + i, glyph, fontData);
        } else
            setGlyphDataForIndex(offset + i, 0, 0);
    }
    env->ReleasePrimitiveArrayCritical(jglyphs, glyphs, JNI_ABORT);

    return haveGlyphs;
}

} // namespace WebCore
