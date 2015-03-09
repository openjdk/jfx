/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "PlatformMouseEvent.h"
#include "Scrollbar.h"
#include "ScrollbarThemeJava.h"
#include "ScrollView.h"
#include "NotImplemented.h"

#include "JavaEnv.h"
#include "com_sun_webkit_graphics_ScrollBarTheme.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {


ScrollbarTheme* ScrollbarTheme::nativeTheme()
{
    static ScrollbarTheme *s_sharedInstance = new ScrollbarThemeJava();
    return s_sharedInstance;
}

jclass getJScrollBarThemeClass()
{
    static JGClass jScrollbarThemeClass(
        WebCore_GetJavaEnv()->FindClass("com/sun/webkit/graphics/ScrollBarTheme"));
    ASSERT(jScrollbarThemeClass);

    return jScrollbarThemeClass;
}

JLObject getJScrollBarTheme(ScrollbarThemeClient* sb)
{
    ScrollView* sv = sb->root();
    if (!sv) {
        // the scrollbar has been detached
        return 0;
    }
    ASSERT(sv->isFrameView());
    FrameView* fv = (FrameView*)sv;
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

bool ScrollbarThemeJava::paint(ScrollbarThemeClient* scrollbar, GraphicsContext* gc, const IntRect& damageRect)
{
    // platformContext() returns 0 when printing
    if (gc->paintingDisabled() || !gc->platformContext()) {
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
        ptr_to_jlong(scrollbar),
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation(),
        (jint)scrollbar->value(),
        (jint)scrollbar->visibleSize(),
        (jint)scrollbar->totalSize()));
    ASSERT(widgetRef.get());
    CheckAndClearException(env);

    // widgetRef will go into rq's inner refs vector.
    gc->platformContext()->rq().freeSpace(28)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWSCROLLBAR
        << RQRef::create(jtheme)
        << widgetRef
        << (jint)scrollbar->x()
        << (jint)scrollbar->y()
        << (jint)scrollbar->pressedPart()
        << (jint)scrollbar->hoveredPart();

    return false;
}

ScrollbarPart ScrollbarThemeJava::hitTest(ScrollbarThemeClient* scrollbar, const IntPoint& pos)
{
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return (ScrollbarPart)0;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "hitTest",
        "(IIIIIIII)I");
    ASSERT(mid);

    IntPoint p = scrollbar->convertFromContainingWindow(pos);
    int part = env->CallIntMethod(
        jtheme,
        mid,
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation(),
        (jint)scrollbar->value(),
        (jint)scrollbar->visibleSize(),
        (jint)scrollbar->totalSize(),
        (jint)p.x(),
        (jint)p.y());
    CheckAndClearException(env);

    return (ScrollbarPart)part;
}

void ScrollbarThemeJava::invalidatePart(ScrollbarThemeClient* scrollbar, ScrollbarPart)
{
    // FIXME: Do more precise invalidation.
    scrollbar->invalidate();
}

int ScrollbarThemeJava::thumbPosition(ScrollbarThemeClient* scrollbar)
{
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return 0;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getThumbPosition",
        "(IIIIII)I");
    ASSERT(mid);

    int pos = env->CallIntMethod(
        jtheme,
        mid,
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation(),
        (jint)scrollbar->value(),
        (jint)scrollbar->visibleSize(),
        (jint)scrollbar->totalSize());
    CheckAndClearException(env);

    return pos;
}

int ScrollbarThemeJava::thumbLength(ScrollbarThemeClient* scrollbar)
{
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return 0;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getThumbLength",
        "(IIIIII)I");
    ASSERT(mid);

    int len = env->CallIntMethod(
        jtheme,
        mid,
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation(),
        (jint)scrollbar->value(),
        (jint)scrollbar->visibleSize(),
        (jint)scrollbar->totalSize());
    CheckAndClearException(env);

    return len;
}

int ScrollbarThemeJava::trackPosition(ScrollbarThemeClient* scrollbar)
{
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return 0;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getTrackPosition",
        "(III)I");
    ASSERT(mid);

    int pos = env->CallIntMethod(
        jtheme,
        mid,
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation());
    CheckAndClearException(env);

    return pos;
}

int ScrollbarThemeJava::trackLength(ScrollbarThemeClient* scrollbar)
{
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return 0;
    }
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getTrackLength",
        "(III)I");
    ASSERT(mid);

    int len = env->CallIntMethod(
        jtheme,
        mid,
        (jint)scrollbar->width(),
        (jint)scrollbar->height(),
        (jint)scrollbar->orientation());
    CheckAndClearException(env);

    return len;
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

