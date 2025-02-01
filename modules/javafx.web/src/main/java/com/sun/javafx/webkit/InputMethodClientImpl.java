/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.SceneHelper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import com.sun.webkit.Invoker;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.web.WebView;

import com.sun.webkit.InputMethodClient;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCInputMethodEvent;
import com.sun.webkit.graphics.WCPoint;

public final class InputMethodClientImpl
    implements InputMethodClient, ExtendedInputMethodRequests
{
    private static final PlatformLogger log =
            PlatformLogger.getLogger(InputMethodClientImpl.class.getName());
    private final WeakReference<WebView> wvRef;
    private final WebPage webPage;

    // the state of the last setInputMethodState() call.
    private boolean state;

    public InputMethodClientImpl(WebView wv, WebPage webPage) {
        this.wvRef = new WeakReference<>(wv);
        this.webPage = webPage;
        if (webPage != null) {
            webPage.setInputMethodClient(this);
        }
    }

    @Override
    public void activateInputMethods(final boolean doActivate) {
        WebView wv = wvRef.get();
        if (wv != null && wv.getScene() != null) {
            SceneHelper.enableInputMethodEvents(wv.getScene(), doActivate);
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
        List<Integer> underlines = new ArrayList<>();
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

    private <T> T callOnEventThread(Callable<T> callable) {
        FutureTask<T> f = new FutureTask<>(callable);

        Invoker.getInvoker().invokeOnEventThread(f);
        T result = null;
        try {
            result = f.get();
        } catch (ExecutionException ex) {
            log.severe("InputMethodClientImpl " + ex);
        } catch (InterruptedException ex) {
            log.severe("InputMethodClientImpl InterruptedException" + ex);
        }
        return result;
    }

    @Override
    public Point2D getTextLocation(int offset) {
        Point2D result = callOnEventThread((Callable<Point2D>) () -> {
            int[] loc = webPage.getClientTextLocation(offset);
            WCPoint point = webPage.getPageClient().windowToScreen(
                    // We need lower left corner of the char bounds rectangle here
                    new WCPoint(loc[0], loc[1] + loc[3]));
            return new Point2D(point.getIntX(), point.getIntY());
        });

        return result;
    }

    @Override
    public int getLocationOffset(int x, int y) {
        Integer result = callOnEventThread((Callable<Integer>) () -> {
            WCPoint point = webPage.getPageClient().windowToScreen(new WCPoint(0, 0));
            return webPage.getClientLocationOffset(x - point.getIntX(), y - point.getIntY());
        });

        return result != null ? result : 0;
    }

    @Override
    public void cancelLatestCommittedText() {
        // "Undo commit" is not supported.
    }

    @Override
    public String getSelectedText() {
        String result = callOnEventThread((Callable<String>) () -> {
            return webPage.getClientSelectedText();
        });

        return result != null ? result : "";
    }

    @Override
    public int getInsertPositionOffset() {
        Integer result = callOnEventThread((Callable<Integer>) () -> {
            return webPage.getClientInsertPositionOffset();
        });

        return result != null ? result : 0;
    }

    @Override
    public String getCommittedText(int begin, int end) {
        String result = callOnEventThread((Callable<String>) () -> {
            try {
                return webPage.getClientCommittedText().substring(begin, end);
            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(e);
            }
        });

        return result != null ? result : "";
    }

    @Override
    public int getCommittedTextLength() {
        Integer result = callOnEventThread((Callable<Integer>) () -> {
            return webPage.getClientCommittedTextLength();
        });

        return result != null ? result : 0;
    }
}
