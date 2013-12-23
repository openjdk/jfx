/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.webkit.graphics.WCImageFrame;

final class WCPasteboard {

    private final static Logger log = 
        Logger.getLogger(WCPasteboard.class.getName());

    private static native void initIDs();
    private static final Pasteboard pasteboard;

    static {
        initIDs();
        pasteboard = Utilities.getUtilities().createPasteboard();
    };

    private WCPasteboard() {
    }

    private static String getPlainText() {
        log.fine("getPlainText()");
        return pasteboard.getPlainText();
    }

    private static String getHtml() {
        log.fine("getHtml()");
        return pasteboard.getHtml();
    }

    private static void writePlainText(String text) {
        log.log(Level.FINE, "writePlainText(): text = {0}", new Object[] {text});
        pasteboard.writePlainText(text);
    }

    private static void writeSelection(boolean canSmartCopyOrDelete, String text, String html)
    {
        log.log(Level.FINE, "writeSelection(): canSmartCopyOrDelete = {0},\n text = \n{1}\n html=\n{2}",
                new Object[] {canSmartCopyOrDelete, text, html});
        pasteboard.writeSelection(canSmartCopyOrDelete, text, html);
    }

    private static void writeImage(WCImageFrame img) {
        log.log(Level.FINE, "writeImage(): img = {0}", new Object[] {img});
        pasteboard.writeImage(img);
    }

    private static void writeUrl(String url, String markup) {
        log.log(Level.FINE, "writeUrl(): url = {0}, markup = {1}",
                new Object[] {url, markup});
        pasteboard.writeUrl(url, markup);
    }
}
