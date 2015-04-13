package com.sun.javafx.webkit;

import javafx.scene.web.WebView;

/**
 *  Allows to receive Webkit JS Web console messages.
 */
public interface WebConsoleListener {

    public static void setDefaultListener(WebConsoleListener l) {
        WebPageClientImpl.setConsoleListener(l);
    }

    void messageAdded(WebView webView, String message, int lineNumber, String sourceId);
}
