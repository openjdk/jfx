/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
