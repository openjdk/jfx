/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.web.WebView;

import com.sun.webkit.InputMethodClient;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCInputMethodEvent;
import com.sun.webkit.graphics.WCPoint;

public final class InputMethodClientImpl
    implements InputMethodClient, ExtendedInputMethodRequests
{
    private final WeakReference<WebView> wvRef;
    private final WebPage webPage;

    // the state of the last setInputMethodState() call.
    private boolean state;
    
    public InputMethodClientImpl(WebView wv, WebPage webPage) {
        this.wvRef = new WeakReference<WebView>(wv);
        this.webPage = webPage;
        if (webPage != null) {
            webPage.setInputMethodClient(this);
        }
    }

    public void activateInputMethods(final boolean doActivate) {
        WebView wv = wvRef.get();
        if (wv != null && wv.getScene() != null) {
            wv.getScene().impl_enableInputMethodEvents(doActivate);
        }
        state = doActivate;
    }

    public boolean getInputMethodState() {
        return state;
    }

    /**
     * Converts the given InputMethodEvent to a WCInputMethodEvent.
     */
    public static WCInputMethodEvent convertToWCInputMethodEvent(InputMethodEvent ie) {
        List<Integer> underlines = new ArrayList<Integer>();
        StringBuilder composed = new StringBuilder();
        int pos = 0;

        // Scan the given composedText to find input method highlight attribute runs.
        for (InputMethodTextRun run : ie.getComposed()) {
            String rawText = run.getText();

            // Convert highlight information of the attribute run into a
            // CompositionUnderline.
            InputMethodHighlight imh = run.getHighlight();
            underlines.add(pos);
            underlines.add(pos + rawText.length());
            // WebKit CompostionUnderline supports only two kinds of highlighting
            // attributes, thin and thick underlines. The SELECTED_CONVERTED
            // and SELECTED_RAW attributes of JavaFX are mapped to the thick one.
            underlines.add((imh == InputMethodHighlight.SELECTED_CONVERTED ||
                            imh == InputMethodHighlight.SELECTED_RAW) ? 1 : 0);
            pos += rawText.length();
            composed.append(rawText);
        }

        int size = underlines.size();
        // In case there's no highlight information, create an underline element
        // for the entire text
        if (size == 0) {
            underlines.add(0);
            underlines.add(pos);
            underlines.add(0); // thin underline
            size = underlines.size();
        }
        int[] attributes = new int[size];
        for (int i = 0; i < size; i++) {
            attributes[i] = underlines.get(i);
        }

        return new WCInputMethodEvent(ie.getCommitted(), composed.toString(),
                attributes, ie.getCaretPosition());
    }

    // InputMethodRequests implementation
    public Point2D getTextLocation(int offset) {
        int[] loc = webPage.getClientTextLocation(offset);
        WCPoint point = webPage.getPageClient().windowToScreen(
                // We need lower left corner of the char bounds rectangle here
                new WCPoint(loc[0], loc[1] + loc[3]));
        return new Point2D(point.getIntX(), point.getIntY());
    }

    public int getLocationOffset(int x, int y) {
        WCPoint point = webPage.getPageClient().windowToScreen(new WCPoint(0, 0));
        return webPage.getClientLocationOffset(x - point.getIntX(), y - point.getIntY());
    }

    public void cancelLatestCommittedText() {
        // "Undo commit" is not supported.
    }

    public String getSelectedText() {
        return webPage.getClientSelectedText();
    }

    @Override
    public int getInsertPositionOffset() {
        return webPage.getClientInsertPositionOffset();
    }

    @Override
    public String getCommittedText(int begin, int end) {
        try {
            return webPage.getClientCommittedText().substring(begin, end);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int getCommittedTextLength() {
        return webPage.getClientCommittedTextLength();
    }
}
