/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit;

import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.sun.webkit.WebPage;

public abstract class Accessor {

    public static interface PageAccessor {
        public WebPage getPage(WebEngine w);
    }

    private static PageAccessor pageAccessor;

    public static void setPageAccessor(PageAccessor instance) {
        Accessor.pageAccessor = instance;
    }

    public static WebPage getPageFor(WebEngine w) {
        return pageAccessor.getPage(w);
    }

    public abstract WebEngine getEngine();
    public abstract WebView getView();
    public abstract WebPage getPage();
    public abstract void addChild(Node child);
    public abstract void removeChild(Node child);
    public abstract void addViewListener(InvalidationListener l);
}
