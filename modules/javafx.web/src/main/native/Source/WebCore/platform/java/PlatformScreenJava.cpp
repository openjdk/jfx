/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "FloatRect.h"
#include "Frame.h"
#include "FrameView.h"
#include "HostWindow.h"
#include "PlatformJavaClasses.h"
#include "PlatformScreen.h"
#include "ScrollView.h"
#include "Widget.h"

#include "NotImplemented.h"

/*
 * The four cached WCRectangle field ids and the two cached WCWidget method ids are gone with
 * initRefs(): the rectangle is returned through a caller-provided float[4] and the two calls
 * are slots on the theme table.
 */

namespace WebCore
{

int screenHorizontalDPI(Widget*)
{
    notImplemented();
    return 0;
}

int screenVerticalDPI(Widget*)
{
    notImplemented();
    return 0;
}

int screenDepth(Widget* w)
{
    if (!w)
        return 24;

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return 24;

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->widget_get_screen_depth)
        return 24;

    int32_t depth = cb->widget_get_screen_depth(j.get());
    wkjCheckAndClearException();

    return depth;
}

int screenDepthPerComponent(Widget* w)
{
    return screenDepth(w) / 3;
}

bool screenIsMonochrome(Widget*)
{
    notImplemented();
    return false;
}

FloatRect getScreenRect(Widget* w, bool available)
{
    if (!w)
        return IntRect(0, 0, 0, 0);

    ASSERT(w->root());
    ASSERT(w->root()->hostWindow());
    PlatformWidget j(w->root()->hostWindow()->platformPageClient());
    if (!j)
        return IntRect(0, 0, 0, 0);

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->widget_get_screen_rect)
        return IntRect(0, 0, 0, 0);

    // A 0 return is the null WCRectangle, which produced the same empty rect.
    float xywh[4] = { 0, 0, 0, 0 };
    int32_t written = cb->widget_get_screen_rect(j.get(), available ? 1 : 0, xywh);
    wkjCheckAndClearException();

    if (!written) {
        return IntRect(0, 0, 0, 0);
    }

    return FloatRect(xywh[0], xywh[1], xywh[2], xywh[3]);
}

FloatRect screenRect(Widget* w)
{
    return getScreenRect(w, false);
}

FloatRect screenAvailableRect(Widget* w)
{
    return getScreenRect(w, true);
}

bool screenHasInvertedColors() //XXX: recheck
{
    return false;
}

bool screenSupportsExtendedColor(Widget*)
{
    return false;
}

} // namespace WebCore
