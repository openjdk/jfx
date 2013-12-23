/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import com.sun.webkit.graphics.WCRectangle;
import java.util.logging.Level;
import java.util.logging.Logger;

class WCWidget {
    private final static Logger log = Logger.getLogger(WCWidget.class.getName());

    static {
        initIDs();
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private final WebPage page;

    WCWidget(WebPage page) {
        this.page = page;
    }
    
    WebPage getPage() {
        return page;
    }
    
    WCRectangle getBounds() {
        return new WCRectangle(x, y, width, height);
    }

    void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected void destroy() {}

    protected void requestFocus() {}

    protected void setCursor(long cursorID) {}

    protected void setVisible(boolean visible) {}

    private void fwkDestroy() {
        log.log(Level.FINER, "destroy");
        destroy();
    }

    private void fwkSetBounds(int x, int y, int w, int h) {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "setBounds({0}, {1}, {2}, {3})",
                    new Object[] { x, y, w, h });
        }
        setBounds(x, y, w, h);
    }

    private void fwkRequestFocus() {
        log.log(Level.FINER, "requestFocus");
        requestFocus();
    }

    private void fwkSetCursor(long cursorID) {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "setCursor({0})", cursorID);
        }
        setCursor(cursorID);
    }

    private void fwkSetVisible(boolean visible) {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "setVisible({0})", visible);
        }
        setVisible(visible);
    }

    protected int fwkGetScreenDepth() {
        log.log(Level.FINER, "getScreenDepth");
        WebPageClient pageClient = page.getPageClient();
        return pageClient != null
                ? pageClient.getScreenDepth()
                : 24;
    }
    
    protected WCRectangle fwkGetScreenRect(boolean available) {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "getScreenRect({0})", available);
        }
        WebPageClient pageClient = page.getPageClient();
        return pageClient != null
                ? pageClient.getScreenBounds(available)
                : null;
    }

    private static native void initIDs();
}
