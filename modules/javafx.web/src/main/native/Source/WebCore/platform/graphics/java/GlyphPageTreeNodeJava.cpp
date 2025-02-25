/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "GlyphPage.h"
#include "GraphicsContextJava.h"
#include "Font.h"

namespace WebCore {

bool GlyphPage::fill(std::span<const UChar> characterBuffer)
{
    JNIEnv* env = WTF::GetJavaEnv();

    RefPtr<RQRef> jFont = this->font().platformData().nativeFontData();
    if (!jFont)
        return false;

    JLocalRef<jcharArray> jchars(env->NewCharArray(characterBuffer.size()));
    WTF::CheckAndClearException(env); // OOME
    ASSERT(jchars);
    if (!jchars)
        return false;

    jchar* chars = (jchar*)env->GetPrimitiveArrayCritical(jchars, NULL);
    ASSERT(chars);
    memcpy(chars, characterBuffer.data(), characterBuffer.size() * 2);
    env->ReleasePrimitiveArrayCritical(jchars, chars, 0);

    static jmethodID mid = env->GetMethodID(PG_GetFontClass(env), "getGlyphCodes", "([C)[I");
    ASSERT(mid);
    JLocalRef<jintArray> jglyphs(static_cast<jintArray>(env->CallObjectMethod(*jFont, mid, (jcharArray)jchars)));
    WTF::CheckAndClearException(env);
    ASSERT(jglyphs);
    if (!jglyphs)
        return false;

    Glyph* glyphs = (Glyph*)env->GetPrimitiveArrayCritical(jglyphs, NULL);
    ASSERT(glyphs);

    unsigned step;  // 1 for BMP, 2 for non-BMP
    if (characterBuffer.size() == GlyphPage::size) {
        step = 1;
    } else if (characterBuffer.size() == 2 * GlyphPage::size) {
        step = 2;
    } else {
        ASSERT_NOT_REACHED();
    }

    bool haveGlyphs = false;
    for (unsigned i = 0; i < GlyphPage::size; i++) {
        Glyph glyph = glyphs[i * step];
        if (glyph) {
            haveGlyphs = true;
            setGlyphForIndex(i, glyph,ColorGlyphType::Outline);
        } else
            setGlyphForIndex(i, 0, this->font().colorGlyphType(glyph));
    }
    env->ReleasePrimitiveArrayCritical(jglyphs, glyphs, JNI_ABORT);

    return haveGlyphs;
}

} // namespace WebCore
