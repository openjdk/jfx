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

/*
 * PlatformJavaClasses.h - the C++ glue of the WebCore/platform/java Java-port directory.
 *
 * WHAT THIS FILE USED TO BE. It declared 18 PG_Get*Class() accessors, each a cached
 * lookup of one com.sun.webkit class, plus PL_GetGraphicsManager() and getTimerClass().
 * They existed for one reason: JNI needed a class reference before it could look up a
 * member id. With the WKJHost callback table there is neither, so all 20 are
 * gone - FFM-AUDIT-wtf-webcore.md section 9.2 rules the block WRAPPER, PARITY exact.
 *
 * The name is kept because roughly twenty files across WebCore and WebKitLegacy include it,
 * and because the file still has a job: it is where this directory reaches the theme and
 * filesystem halves of the host table, exactly as WKJPlatformJava.h is for
 * platform/graphics/java and platform/network/java. Everything string-shaped -
 * wkjMakeString, WKJStringArg, wkjFetchString - and wkjCheckAndClearException come from
 * that header, which this one includes; both directories are on the WebCore include path
 * (WebCore/PlatformJava.cmake), so the include resolves from either side.
 *
 * The two accessors that did real work rather than class caching survive below as
 * wkjRenderThemeForPage and wkjScrollBarThemeForPage.
 */

#pragma once

#include <stdint.h>

#include <webkit_java_api.h>

#include <wtf/java/WKJHandle.h>

#include "WKJPlatformJava.h"

namespace WebCore {

/*
 * The installed theme and filesystem tables, or nullptr before wkj_init has run. Every
 * caller tests both the table and the individual slot, because contract 4 lets Java leave
 * any slot NULL.
 */
inline const WKJHostTheme* wkjTheme()
{
    return wkj_host ? &wkj_host->theme : nullptr;
}

inline const WKJHostFileSystem* wkjFileSystem()
{
    return wkj_host ? &wkj_host->filesystem : nullptr;
}

/*
 * The com.sun.webkit.graphics.RenderTheme of a page, owned by the caller.
 *
 * This is PG_GetRenderThemeObjectFromPage, unchanged in behaviour including its two
 * branches: a zero webPage asks WebPage.fwkGetDefaultRenderTheme() and a non-zero one asks
 * that page's getRenderTheme(). Both branches are live.
 */
WKJHandle wkjRenderThemeForPage(wkj_ref webPage);

/*
 * The com.sun.webkit.graphics.ScrollBarTheme of a page, owned by the caller. Unlike the
 * render theme there is no default: a null handle means the scrollbar has no Java theme and
 * the caller skips the work, which is what ScrollbarThemeJava already did with a null
 * reference.
 */
WKJHandle wkjScrollBarThemeForPage(wkj_ref webPage);

} // namespace
