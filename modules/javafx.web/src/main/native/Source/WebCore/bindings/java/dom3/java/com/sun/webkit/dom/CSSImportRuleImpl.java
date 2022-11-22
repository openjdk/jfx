/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.stylesheets.MediaList;

public class CSSImportRuleImpl extends CSSRuleImpl implements CSSImportRule {
    CSSImportRuleImpl(long peer) {
        super(peer);
    }

    static CSSImportRule getImpl(long peer) {
        return (CSSImportRule)create(peer);
    }


// Attributes
    public String getHref() {
        return getHrefImpl(getPeer());
    }
    native static String getHrefImpl(long peer);

    public MediaList getMedia() {
        return MediaListImpl.getImpl(getMediaImpl(getPeer()));
    }
    native static long getMediaImpl(long peer);

    public CSSStyleSheet getStyleSheet() {
        return CSSStyleSheetImpl.getImpl(getStyleSheetImpl(getPeer()));
    }
    native static long getStyleSheetImpl(long peer);

}

