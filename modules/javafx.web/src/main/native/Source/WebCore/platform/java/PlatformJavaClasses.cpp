/*
 * Copyright (c) 2019, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformJavaClasses.h"

namespace WebCore {

WKJHandle wkjRenderThemeForPage(wkj_ref webPage)
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->get_render_theme)
        return WKJHandle();

    /*
     * One slot, two branches, exactly as PG_GetRenderThemeObjectFromPage had: it chose
     * between the static WebPage.fwkGetDefaultRenderTheme() and the instance
     * WebPage.getRenderTheme() on the value of its page argument, and the Java side of this
     * slot makes the same choice on the same value.
     */
    WKJHandle theme(cb->get_render_theme(webPage));
    wkjCheckAndClearException();
    return theme;
}

WKJHandle wkjScrollBarThemeForPage(wkj_ref webPage)
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->get_scroll_bar_theme)
        return WKJHandle();

    WKJHandle theme(cb->get_scroll_bar_theme(webPage));
    wkjCheckAndClearException();
    return theme;
}

} // namespace
