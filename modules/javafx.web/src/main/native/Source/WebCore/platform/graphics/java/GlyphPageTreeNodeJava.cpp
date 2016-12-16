/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "GlyphPage.h"
#include "GraphicsContextJava.h"
#include "Font.h"

namespace WebCore {

bool GlyphPage::fill(UChar* buffer, unsigned bufferLength)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    RefPtr<RQRef> jFont = this->font().platformData().nativeFontData();
    if (!jFont)
        return false;

    JLocalRef<jcharArray> jchars(env->NewCharArray(bufferLength));
    CheckAndClearException(env); // OOME
    ASSERT(jchars);
    if (!jchars)
        return false;

    jchar* chars = (jchar*)env->GetPrimitiveArrayCritical(jchars, NULL);
    ASSERT(chars);
    memcpy(chars, buffer, bufferLength * 2);
    env->ReleasePrimitiveArrayCritical(jchars, chars, 0);

    static jmethodID mid = env->GetMethodID(PG_GetFontClass(env), "getGlyphCodes", "([C)[I");
    ASSERT(mid);
    JLocalRef<jintArray> jglyphs(static_cast<jintArray>(env->CallObjectMethod(*jFont, mid, (jcharArray)jchars)));
    CheckAndClearException(env);
    ASSERT(jglyphs);
    if (!jglyphs)
        return false;

    Glyph* glyphs = (Glyph*)env->GetPrimitiveArrayCritical(jglyphs, NULL);
    ASSERT(glyphs);

    unsigned step;  // 1 for BMP, 2 for non-BMP
    if (bufferLength == GlyphPage::size) {
        step = 1;
    } else if (bufferLength == 2 * GlyphPage::size) {
        step = 2;
    } else {
        ASSERT_NOT_REACHED();
    }

    bool haveGlyphs = false;
    for (unsigned i = 0; i < GlyphPage::size; i++) {
        Glyph glyph = glyphs[i * step];
        if (glyph) {
            haveGlyphs = true;
            setGlyphForIndex(i, glyph);
        } else
            setGlyphForIndex(i, 0);
    }
    env->ReleasePrimitiveArrayCritical(jglyphs, glyphs, JNI_ABORT);

    return haveGlyphs;
}

} // namespace WebCore
