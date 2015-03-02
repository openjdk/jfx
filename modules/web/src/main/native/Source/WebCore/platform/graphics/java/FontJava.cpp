/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Font.h"
#include "TextRun.h"
#include "FontData.h"
#include "GlyphBuffer.h"
#include "SimpleFontData.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "RenderingQueue.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"
#include "NotImplemented.h"

#include <stdio.h>

#include "wtf/HashSet.h"

namespace WebCore {

static JLString getJavaString(const TextRun& run)
{
    unsigned length = run.length();
    bool allowTabs = run.allowTabs();
    String ret = run.is8Bit()
        ? String(allowTabs
                ? String(run.characters8())
                : Font::normalizeSpaces(run.characters8(), length))
        : String(allowTabs
                ? String(run.characters16())
                : Font::normalizeSpaces(run.characters16(), length));
    return ret.toJavaString(WebCore_GetJavaEnv());
}

float Font::drawComplexText(GraphicsContext* gc, const TextRun & run, const FloatPoint & point, int from, int to) const
{
    if (!gc) {
        return 0;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID refString_mID = env->GetMethodID(
        PG_GetRenderQueueClass(env),
        "refString",
        "(Ljava/lang/String;)I");
    ASSERT(refString_mID);

    RenderingQueue& rq = gc->platformContext()->rq().freeSpace(10 * JINT_SZ);

    // we need to call refString() after freeSpace(), see RT-19695.
    jint sid = env->CallIntMethod(
        rq.getWCRenderingQueue(),
        refString_mID,
        (jstring)getJavaString(run));
    CheckAndClearException(env);

    rq  << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWSTRING
        << primaryFont()->platformData().nativeFontData()
        << sid
        << (jint)(run.rtl() ? -1 : 0)
        << (jint)from
        << (jint)to
        << (jfloat)point.x()
        << (jfloat)point.y();

    return 0; // tav todo
}

float Font::floatWidthForComplexText(const TextRun& run, HashSet<const SimpleFontData*>* fallbackFonts, GlyphOverflow* /* glyphOverflow */) const
{
    RefPtr<RQRef> jFont = primaryFont()->platformData().nativeFontData();
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

FloatRect Font::selectionRectForComplexText(const TextRun& run,
        const FloatPoint& point, int h, int from, int to) const
{
    RefPtr<RQRef> jFont = primaryFont()->platformData().nativeFontData();
    if (!jFont)
        return FloatRect();

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
    CheckAndClearException(env);

    jdouble* pBnds = (jdouble*)env->GetPrimitiveArrayCritical((jdoubleArray)bnds, NULL);
    FloatRect r(pBnds[0] + point.x(), point.y(), pBnds[2], h);
    env->ReleasePrimitiveArrayCritical(bnds, pBnds, JNI_ABORT);
    return r;
}

int Font::offsetForPositionForComplexText(
    const TextRun& run, float xFloat,
    bool includePartialGlyphs) const
{
    RefPtr<RQRef> jFont = primaryFont()->platformData().nativeFontData();
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

void Font::drawGlyphs(GraphicsContext* gc,
                      const SimpleFontData* font,
                      const GlyphBuffer& glyphBuffer,
                      int from, int numGlyphs,
                      const FloatPoint& point) const
{
    if (!gc) {
        return;
    }

    // we need to call freeSpace() before refIntArr() and refFloatArr(), see RT-19695.
    RenderingQueue& rq = gc->platformContext()->rq().freeSpace(24);

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
        for (int i = 0; i < numGlyphs; ++i) {
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
        << font->platformData().nativeFontData()
        << sid
        << aid
        << (jfloat)point.x()
        << (jfloat)point.y();
}

bool Font::canReturnFallbackFontsForComplexText()
{
    return false;
}

bool Font::canExpandAroundIdeographsInComplexText()
{
    return false;
}

void Font::drawEmphasisMarksForComplexText(
    GraphicsContext* context,
    const TextRun& run,
    const AtomicString& mark,
    const FloatPoint& point,
    int from,
    int to) const
{
    if (loadingCustomFonts())
        return;

    if (to < 0)
        to = run.length();

#if ENABLE(SVG_FONTS)
    // FIXME: Implement for SVG fonts.
    if (primaryFont()->isSVGFont())
        return;
#endif

    if (codePath(run) != Complex)
        drawEmphasisMarksForSimpleText(context, run, mark, point, from, to);
    else
        drawEmphasisMarksForComplexText(context, run, mark, point, from, to);
}

}
