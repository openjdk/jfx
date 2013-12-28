/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit;

import com.sun.webkit.Pasteboard;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCImageFrame;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;


final class PasteboardImpl implements Pasteboard {

    private final Clipboard clipboard = Clipboard.getSystemClipboard();

    PasteboardImpl() {
    }

    @Override public String getPlainText() {
        return clipboard.getString();
    }

    @Override public String getHtml() {
        return clipboard.getHtml();
    }

    @Override public void writePlainText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    @Override public void writeSelection(boolean canSmartCopyOrDelete, String text, String html) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        content.putHtml(html);
        clipboard.setContent(content);
    }
    
    @Override public void writeImage(WCImageFrame wcImage) {
        Object platformImage = WCGraphicsManager.getGraphicsManager().
                toPlatformImage(wcImage.getFrame());
        Image fxImage = Image.impl_fromPlatformImage(platformImage);
        ClipboardContent content = new ClipboardContent();
        content.putImage(fxImage);
        clipboard.setContent(content);
    }

    @Override public void writeUrl(String url, String markup) {
        ClipboardContent content = new ClipboardContent();
        content.putString(url);
        content.putHtml(markup);
        content.putUrl(url);
        clipboard.setContent(content);
    }
}
