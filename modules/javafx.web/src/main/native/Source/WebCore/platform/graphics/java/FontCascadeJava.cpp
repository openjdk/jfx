/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "TextRun.h"
#include "LayoutRect.h"
#include "FontRanges.h"
#include "GlyphBuffer.h"
#include "Font.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "RenderingQueue.h"
#include "PlatformContextJava.h"
#include "NotImplemented.h"

#include <wtf/HashSet.h>

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

static JLString getJavaString(const TextRun& run)
{
    unsigned length = run.length();
    bool allowTabs = run.allowTabs();
    String ret = run.is8Bit()
        ? String(allowTabs
                ? run.string()
                : FontCascade::normalizeSpaces(run.characters8(), length))
        : String(allowTabs
                ? run.string()
                : FontCascade::normalizeSpaces(run.characters16(), length));
    return ret.toJavaString(WebCore_GetJavaEnv());
}

float FontCascade::getGlyphsAndAdvancesForComplexText(const TextRun& run, unsigned from, unsigned to, GlyphBuffer& glyphBuffer, ForTextEmphasisOrNot) const {
    RefPtr<RQRef> jFont = primaryFont().platformData().nativeFontData();
    if (!jFont)
        return 0;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID getGlyphsAndAdvances_mID = env->GetMethodID(
        PG_GetFontClass(env),
        "getGlyphsAndAdvances",
        "(Ljava/lang/String;IIZ)Lcom/sun/webkit/graphics/WCGlyphBuffer;");
    ASSERT(getGlyphsAndAdvances_mID);

    jobject jglyphBuffer = env->CallObjectMethod(
        *jFont,
        getGlyphsAndAdvances_mID,
        (jstring)getJavaString(run),
        from,
        to,
        run.rtl());

    CheckAndClearException(env);
    if (!jglyphBuffer)
        return 0;
    static jfieldID glyphs_fID = env->GetFieldID(
        PG_GetGlyphBufferClass(env),
        "glyphs",
        "[I");
    static jfieldID advances_fID = env->GetFieldID(
        PG_GetGlyphBufferClass(env),
        "advances",
        "[F");
    static jfieldID initialAdvance_fID = env->GetFieldID(
        PG_GetGlyphBufferClass(env),
        "initialAdvance",
        "F");
    CheckAndClearException(env);
    // get glyph details from object
    jarray jglyphs = (jarray)env->GetObjectField(jglyphBuffer, glyphs_fID);
    jarray jadvances = (jarray)env->GetObjectField(jglyphBuffer, advances_fID);
    jfloat jinitialAdvance = env->GetFloatField(jglyphBuffer, initialAdvance_fID);
    size_t glyphsCount = env->GetArrayLength(jglyphs);
    ASSERT(env->GetArrayLength(jadvances) == glyphsCount);
    jint* glyphs = (jint*)env->GetPrimitiveArrayCritical(jglyphs, 0);
    jfloat* advances = (jfloat*)env->GetPrimitiveArrayCritical(jadvances, 0);
    for (size_t i = 0; i < glyphsCount; i++) {
        glyphBuffer.add(glyphs[i], &primaryFont(), advances[i]);
    }
    env->ReleasePrimitiveArrayCritical(jglyphs, glyphs, 0);
    env->ReleasePrimitiveArrayCritical(jadvances, advances, 0);
    CheckAndClearException(env);
    return jinitialAdvance;
}

float FontCascade::floatWidthForComplexText(const TextRun& run, HashSet<const Font*>*, GlyphOverflow* /* glyphOverflow */) const
{
    RefPtr<RQRef> jFont = primaryFont().platformData().nativeFontData();
    if (!jFont)
        return 0.0f;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID getStringWidth_mID = env->GetMethodID(
        PG_GetFontClass(env),
        "getStringWidth",
        "(Ljava/lang/String;)D");
    ASSERT(getStringWidth_mID);

    float res = env->CallDoubleMethod(
        *jFont,
        getStringWidth_mID,
        (jstring)getJavaString(run));
    CheckAndClearException(env);

    return res;
}

void FontCascade::adjustSelectionRectForComplexText(const TextRun& run, LayoutRect& selectionRect, unsigned from, unsigned to) const
{
    RefPtr<RQRef> jFont = primaryFont().platformData().nativeFontData();
    if (!jFont)
        return;

    // adjusting to/from bounds due to issue RT-46101
    if (from > run.length()) {
        return;
    }

    if (to > run.length()) {
        to = run.length();
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID getStringBounds_mID = env->GetMethodID(
        PG_GetFontClass(env),
        "getStringBounds",
        "(Ljava/lang/String;IIZ)[D");
    ASSERT(getStringBounds_mID);

    JLocalRef<jdoubleArray> bnds(static_cast<jdoubleArray>(env->CallObjectMethod(
        *jFont,
        getStringBounds_mID,
        (jstring)getJavaString(run),
        jint(from),
        jint(to),
        jboolean(run.rtl()))));

    if (CheckAndClearException(env)) {
        return;
    }

    jdouble* pBnds = (jdouble*)env->GetPrimitiveArrayCritical((jdoubleArray)bnds, NULL);
    FloatRect rect(pBnds[0] + selectionRect.x(), selectionRect.y(), pBnds[2], selectionRect.height()); //XXX recheck
    selectionRect = LayoutRect(rect);
    env->ReleasePrimitiveArrayCritical(bnds, pBnds, JNI_ABORT);
}

int FontCascade::offsetForPositionForComplexText(
    const TextRun& run, float xFloat,
    bool) const
{
    RefPtr<RQRef> jFont = primaryFont().platformData().nativeFontData();
    if (!jFont)
        return 0;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID getOffsetForPosition_mID = env->GetMethodID(
        PG_GetFontClass(env),
        "getOffsetForPosition",
        "(Ljava/lang/String;F)I");
    ASSERT(getOffsetForPosition_mID);

    jint res = env->CallIntMethod(
        *jFont,
        getOffsetForPosition_mID,
        (jstring)getJavaString(run),
        xFloat);
    CheckAndClearException(env);

    return res;
}

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


    //prepare Offsets/Advances array
    JLocalRef<jfloatArray> jAdvance(env->NewFloatArray(numGlyphs));
    CheckAndClearException(env);
    ASSERT(jAdvance);
    {
        jfloat *bufArray = env->GetFloatArrayElements(jAdvance, NULL);
        ASSERT(bufArray);
        for (unsigned i = 0; i < numGlyphs; ++i) {
            const GlyphBufferAdvance *pAdvance = glyphBuffer.advances(from + i);
            bufArray[i] = (jfloat)(pAdvance->width());
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
