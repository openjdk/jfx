/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include <wkj_constants.h>

#include "Frame.h"
#include "FrameInlines.h"
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
#include "DocumentPage.h"

#include "PlatformJavaClasses.h"

namespace WebCore {


ScrollbarTheme& ScrollbarTheme::nativeTheme()
{
    static ScrollbarTheme *s_sharedInstance = new ScrollbarThemeJava();
    return *s_sharedInstance;
}

/*
 * The com.sun.webkit.graphics.ScrollBarTheme of the page a scrollbar belongs to, owned by
 * the caller. The three early returns are unchanged and each still means "no Java theme":
 * a detached scrollbar, a scrollbar with no page, and a page with no Java WebPage - the
 * last being a utility page, per Page::isUtilityPage.
 */
WKJHandle getJScrollBarTheme(Scrollbar& sb)
{
    FrameView* fv = sb.enabled() ? sb.root() : nullptr;
    if (!fv) {
        // the scrollbar has been detached
        return WKJHandle();
    }

    Page* page = fv->frame().page();
    if (!page) {
        return WKJHandle();
    }

    PageSupplementJava* pageSupplement = PageSupplementJava::from(page);
    if (!pageSupplement || !pageSupplement->jWebPage()) {
        // Non Java Page, might be a utility Page(svg?), refer Page::isUtilityPage
        return WKJHandle();
    }

    WKJHandle jWebPage = pageSupplement->jWebPage();

    WKJHandle jScrollbarTheme = wkjScrollBarThemeForPage(jWebPage.get());
    ASSERT(jScrollbarTheme);

    return jScrollbarTheme;
}

IntRect getPartRect(Scrollbar& scrollbar, ScrollbarPart part) {
    WKJHandle jtheme = getJScrollBarTheme(scrollbar);
    if (!jtheme) {
        return IntRect();
    }

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->scroll_bar_get_part_rect) {
        return IntRect();
    }

    /*
     * The int[4] the C++ used to allocate with NewIntArray and read back under a critical
     * section is now a plain local that Java writes through. A 0 return means Java wrote
     * nothing, which the zero-initialised array made indistinguishable from an empty rect -
     * and an empty rect is what that case produced, so it still does.
     */
    int32_t r[4] = { 0, 0, 0, 0 };
    cb->scroll_bar_get_part_rect(jtheme.get(), wkj_from_ptr(&scrollbar), (int32_t)part, r);
    wkjCheckAndClearException();

    IntRect rect(r[0], r[1], r[2], r[3]);
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

    WKJHandle jtheme = getJScrollBarTheme(scrollbar);
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

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->scroll_bar_create_widget) {
        return false;
    }

    /*
     * The id the slot returns is owned by this frame, so it is adopted into a WKJHandle and
     * RQRef::create adds its own reference - the two steps the JNI code took with the local
     * reference the JNI call returned.
     */
    WKJHandle widget { cb->scroll_bar_create_widget(
        jtheme.get(),
        wkj_from_ptr(&scrollbar),
        (int32_t)scrollbar.width(),
        (int32_t)scrollbar.height(),
        (int32_t)scrollbar.orientation(),
        (int32_t)scrollbar.value(),
        (int32_t)scrollbar.visibleSize(),
        (int32_t)scrollbar.totalSize()) };
    RefPtr<RQRef> widgetRef = RQRef::create(widget.get());
    ASSERT(widgetRef.get());
    wkjCheckAndClearException();

    if (opacity != 1) {
        gc.save();
        gc.clip(damageRect);
        gc.beginTransparencyLayer(opacity);
    }
    // widgetRef will go into rq's inner refs vector.
    gc.platformContext()->rq().freeSpace(28)
        << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_DRAWSCROLLBAR
        << RQRef::create(jtheme.get())
        << widgetRef
        << (int32_t)scrollbar.x()
        << (int32_t)scrollbar.y()
        << (int32_t)scrollbar.pressedPart()
        << (int32_t)scrollbar.hoveredPart();

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

int ScrollbarThemeJava::scrollbarThickness(ScrollbarWidth width, OverlayScrollbarSizeRelevancy relevancy)
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->scroll_bar_get_thickness)
        return 0;

    int thickness = cb->scroll_bar_get_thickness();
    wkjCheckAndClearException();

    return thickness;
}

} //namespace WebCore

