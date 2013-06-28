/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

final class WCFrameView extends WCWidget {

    WCFrameView(WebPage page) {
        super(page);
    }

    @Override protected void requestFocus() {
        WebPageClient pageClient = getPage().getPageClient();
        if (pageClient != null) {
            pageClient.setFocus(true);
        }
    }

    @Override protected void setCursor(long cursorID) {
        WebPageClient pageClient = getPage().getPageClient();
        if (pageClient != null) {
            pageClient.setCursor(cursorID);
        }
    }
/*
    public Rectangle getVisibleRect() {
        // TODO
        return null;
    }

    public void scrollToVisible(final Rectangle rect) {
        // TODO
    }
*/
}
