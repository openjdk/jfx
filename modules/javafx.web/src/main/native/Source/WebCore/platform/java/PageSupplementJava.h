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

#pragma once

#include "Supplementable.h"

#include <webkit_java_api.h>

#include <wtf/java/WKJHandle.h>

namespace WebCore {

class Page;
class Frame;

class PageSupplementJava final : public Supplement<WebCore::Page> {
    WTF_MAKE_NONCOPYABLE(PageSupplementJava);
  public:
    /*
     * Adopts nothing: the id is retained here, for the lifetime of the page, which is what
     * the global reference this member used to hold did.
     */
    WEBCORE_EXPORT explicit PageSupplementJava(wkj_ref webPage);

    /*
     * A NEW id for the same Java WebPage, owned by the caller - the exact analogue of the
     * local reference this returned, which the caller let die at the end of its scope. The
     * three consumers outside this directory (URLLoader, SocketStreamHandleImplJava and
     * WebPage::jobjectFromPage) hold it in a WKJHandle.
     */
    WEBCORE_EXPORT WKJHandle jWebPage() const { return m_webPage; }

    WEBCORE_EXPORT static ASCIILiteral supplementName();
    WEBCORE_EXPORT static PageSupplementJava* from(Frame*);
    WEBCORE_EXPORT static PageSupplementJava* from(Page*);

  private:
    WKJHandle m_webPage;
};

}

