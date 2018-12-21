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
#include "GlyphBuffer.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"
#include "PlatformContextJava.h"
#include "RenderingQueue.h"

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

void FontCascade::drawGlyphs(GraphicsContext& gc,
                      const Font& font,
                      const GlyphBuffer& glyphBuffer,
                      unsigned from, unsigned numGlyphs,
                      const FloatPoint& point,
                      FontSmoothingMode)
{
    // we need to call freeSpace() before refIntArr() and refFloatArr(), see RT-19695.
    RenderingQueue& rq = gc.platformContext()->rq().freeSpace(24);

    JNIEnv* env = WebCore_GetJavaEnv();

    //prepare Glyphs array
    JLocalRef<jintArray> jGlyphs(env->NewIntArray(numGlyphs));
    ASSERT(jGlyphs);
    {
        jint *bufArray = (jint*)env->GetPrimitiveArrayCritical(jGlyphs, NULL);
        ASSERT(bufArray);
        memcpy(bufArray, glyphBuffer.glyphs(from), sizeof(jint)*numGlyphs);
        env->ReleasePrimitiveArrayCritical(jGlyphs, bufArray, 0);
    }
    static jmethodID refIntArr_mID = env->GetMethodID(
        PG_GetRenderQueueClass(env),
        "refIntArr",
        "([I)I");
    ASSERT(refIntArr_mID);
    jint sid = env->CallIntMethod(
        rq.getWCRenderingQueue(),
        refIntArr_mID,
        (jintArray)jGlyphs);
    CheckAndClearException(env);


    // Prepare Offsets/Advances array
    JLocalRef<jfloatArray> jAdvance(env->NewFloatArray(numGlyphs));
    CheckAndClearException(env);
    ASSERT(jAdvance);
    {
        jfloat *bufArray = env->GetFloatArrayElements(jAdvance, NULL);
        ASSERT(bufArray);
        for (unsigned i = 0; i < numGlyphs; ++i) {
            auto pAdvance = glyphBuffer.advances(from + i);
            if (!pAdvance)
                continue;
            bufArray[i] = jfloat(pAdvance->width());
        }
        env->ReleaseFloatArrayElements(jAdvance, bufArray, 0);
    }
    static jmethodID refFloatArr_mID = env->GetMethodID(
        PG_GetRenderQueueClass(env),
        "refFloatArr",
        "([F)I");
    ASSERT(refFloatArr_mID);
    jint aid = env->CallIntMethod(
        rq.getWCRenderingQueue(),
        refFloatArr_mID,
        (jfloatArray)jAdvance);
    CheckAndClearException(env);

    rq  << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWSTRING_FAST
        << font.platformData().nativeFontData()
        << sid
        << aid
        << (jfloat)point.x()
        << (jfloat)point.y();
}

bool FontCascade::canReturnFallbackFontsForComplexText()
{
    return false;
}

bool FontCascade::canExpandAroundIdeographsInComplexText()
{
    return false;
}

}
