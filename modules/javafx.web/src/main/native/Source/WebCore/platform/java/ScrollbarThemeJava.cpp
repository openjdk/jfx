/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "Frame.h"
#include "FrameView.h"
#include "GraphicsContext.h"
#include "HostWindow.h"
#include "Page.h"
#include "PlatformContextJava.h"
#include "PageSupplementJava.h"
#include "Scrollbar.h"
#include "ScrollbarThemeJava.h"
#include "ScrollView.h"
#include "NotImplemented.h"

#include "PlatformJavaClasses.h"
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
        WTF::GetJavaEnv()->FindClass("com/sun/webkit/graphics/ScrollBarTheme"));
    ASSERT(jScrollbarThemeClass);

    return jScrollbarThemeClass;
}

JLObject getJScrollBarTheme(Scrollbar& sb)
{
    FrameView* fv = sb.enabled() ? sb.root() : nullptr;
    if (!fv) {
        // the scrollbar has been detached
        return 0;
    }

    Page* page = fv->frame().page();
    if (!page) {
        return 0;
    }

    PageSupplementJava* pageSupplement = PageSupplementJava::from(page);
    if (!pageSupplement || !pageSupplement->jWebPage()) {
        // Non Java Page, might be a utility Page(svg?), refer Page::isUtilityPage
        return 0;
    }

    JLObject jWebPage = pageSupplement->jWebPage();

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid  = env->GetMethodID(
        PG_GetWebPageClass(env),
        "getScrollBarTheme",
        "()Lcom/sun/webkit/graphics/ScrollBarTheme;");
    ASSERT(mid);

    JLObject jScrollbarTheme = env->CallObjectMethod(jWebPage, mid);
    ASSERT(jScrollbarTheme);
    WTF::CheckAndClearException(env);

    return jScrollbarTheme;
}

IntRect getPartRect(Scrollbar& scrollbar, ScrollbarPart part) {
    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return IntRect();
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID midGetPartRect = env->GetMethodID(
        getJScrollBarThemeClass(),
        "getScrollBarPartRect",
        "(JI[I)V");
    ASSERT(midGetPartRect);
    JLocalRef<jintArray> jrect(env->NewIntArray(4));
    WTF::CheckAndClearException(env); // OOME
    ASSERT(jrect);
    env->CallVoidMethod(jtheme,
            midGetPartRect,
            ptr_to_jlong(&scrollbar),
            (jint)part,
            (jintArray)jrect);
    WTF::CheckAndClearException(env);

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
        return false;
    }

    JLObject jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return false;
    }

    double opacity = scrollbar.hoveredPart() == NoPart ? scrollbar.opacity() : 1;
    if (!opacity) {
        return true;
    }

    IntRect rect = scrollbar.frameRect();
    if (!rect.intersects(damageRect)) {
        return true;
    }

    JNIEnv* env = WTF::GetJavaEnv();

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
    WTF::CheckAndClearException(env);

    if (opacity != 1) {
        gc.save();
        gc.clip(damageRect);
        gc.beginTransparencyLayer(opacity);
    }
    // widgetRef will go into rq's inner refs vector.
    gc.platformContext()->rq().freeSpace(28)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWSCROLLBAR
        << RQRef::create(jtheme)
        << widgetRef
        << (jint)scrollbar.x()
        << (jint)scrollbar.y()
        << (jint)scrollbar.pressedPart()
        << (jint)scrollbar.hoveredPart();

    if (opacity != 1) {
        gc.endTransparencyLayer();
        gc.restore();
    }

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

int ScrollbarThemeJava::scrollbarThickness(ScrollbarWidth, ScrollbarExpansionState)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
        getJScrollBarThemeClass(),
        "getThickness",
        "()I");
    ASSERT(mid);

    int thickness = env->CallStaticIntMethod(
        getJScrollBarThemeClass(),
        mid);
    WTF::CheckAndClearException(env);

    return thickness;
}

} //namespace WebCore

