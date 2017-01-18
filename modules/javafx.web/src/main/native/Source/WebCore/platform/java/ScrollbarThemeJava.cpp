/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Chrome.h"
#include "ChromeClientJava.h"
#include "Frame.h"
#include "FrameView.h"
#include "GraphicsContext.h"
#include "HostWindow.h"
#include "Page.h"
#include "PlatformContextJava.h"
#include "Scrollbar.h"
#include "ScrollbarThemeJava.h"
#include "ScrollView.h"
#include "NotImplemented.h"

#include "JavaEnv.h"
#include "com_sun_webkit_graphics_ScrollBarTheme.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {


ScrollbarTheme& ScrollbarTheme::nativeTheme()
{
    static ScrollbarTheme *s_sharedInstance = new ScrollbarThemeJava();
    return *s_sharedInstance;
}

jclass getJScrollBarThemeClass()
{
    static JGClass jScrollbarThemeClass(
        WebCore_GetJavaEnv()->FindClass("com/sun/webkit/graphics/ScrollBarTheme"));
    ASSERT(jScrollbarThemeClass);

    return jScrollbarThemeClass;
}

JLObject getJScrollBarTheme(Scrollbar& sb)
{
    FrameView* fv = sb.root();
    if (!fv) {
        // the scrollbar has been detached
        return 0;
    }
    Page* page = fv->frame().page();
    JLObject jWebPage = ((ChromeClientJava*)&page->chrome().client())->platformPage();

    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid  = env->GetMethodID(
        PG_GetWebPageClass(env),
        "getScrollBarTheme",
        "()Lcom/sun/webkit/graphics/ScrollBarTheme;");
    ASSERT(mid);

    JLObject jScrollbarTheme = env->CallObjectMethod(jWebPage, mid);
    ASSERT(jScrollbarTheme);
    CheckAndClearException(env);

    return jScrollbarTheme;
}

IntRect getPartRect(Scrollbar& scrollbar, ScrollbarPart part) {
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return IntRect();
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID midGetPartRect = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getScrollBarPartRect",
        "(JI[I)V");
    ASSERT(midGetPartRect);
    JLocalRef<jintArray> jrect(env->NewIntArray(4));
    CheckAndClearException(env); // OOME
    ASSERT(jrect);
    env->CallVoidMethod(jtheme,
            midGetPartRect,
            ptr_to_jlong(&scrollbar),
            (jint)part,
            (jintArray)jrect);
    CheckAndClearException(env);

    jint *r = (jint*)env->GetPrimitiveArrayCritical(jrect, 0);
    IntRect rect(r[0], r[1], r[2], r[3]);
    env->ReleasePrimitiveArrayCritical(jrect, r, 0);
    if (rect.isEmpty()) {
        return rect;
    }
    // Bounding box should be absolute location, so adjust according to
    // the position of scrollbar.
    rect.move(scrollbar.x(), scrollbar.y());
    return rect;
}


bool ScrollbarThemeJava::paint(Scrollbar& scrollbar, GraphicsContext& gc, const IntRect& damageRect)
{
    // platformContext() returns 0 when printing
    if (gc.paintingDisabled() || !gc.platformContext()) {
        return true;
    }

    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return false;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "createWidget",
        "(JIIIIII)Lcom/sun/webkit/graphics/Ref;");
    ASSERT(mid);

    RefPtr<RQRef> widgetRef = RQRef::create( env->CallObjectMethod(
        jtheme,
        mid,
        ptr_to_jlong(&scrollbar),
        (jint)scrollbar.width(),
        (jint)scrollbar.height(),
        (jint)scrollbar.orientation(),
        (jint)scrollbar.value(),
        (jint)scrollbar.visibleSize(),
        (jint)scrollbar.totalSize()));
    ASSERT(widgetRef.get());
    CheckAndClearException(env);

    // widgetRef will go into rq's inner refs vector.
    gc.platformContext()->rq().freeSpace(28)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWSCROLLBAR
        << RQRef::create(jtheme)
        << widgetRef
        << (jint)scrollbar.x()
        << (jint)scrollbar.y()
        << (jint)scrollbar.pressedPart()
        << (jint)scrollbar.hoveredPart();

    return false;
}

void ScrollbarThemeJava::invalidatePart(Scrollbar& scrollbar, ScrollbarPart)
{
    // FIXME: Do more precise invalidation.
    scrollbar.invalidate();
}

bool ScrollbarThemeJava::hasThumb(Scrollbar& scrollbar)
{
    return thumbLength(scrollbar) > 0;
}

IntRect ScrollbarThemeJava::backButtonRect(Scrollbar& scrollbar, ScrollbarPart part, bool) {
    return getPartRect(scrollbar, part);
}

IntRect ScrollbarThemeJava::forwardButtonRect(Scrollbar& scrollbar, ScrollbarPart part, bool) {
    return getPartRect(scrollbar, part);
}

IntRect ScrollbarThemeJava::trackRect(Scrollbar& scrollbar, bool) {
    return getPartRect(scrollbar, TrackBGPart);
}

int ScrollbarThemeJava::scrollbarThickness(ScrollbarControlSize controlSize)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
        getJScrollBarThemeClass(),
        "getThickness",
        "()I");
    ASSERT(mid);

    int thickness = env->CallStaticIntMethod(
        getJScrollBarThemeClass(),
        mid);
    CheckAndClearException(env);

    return thickness;
}

} //namespace WebCore

