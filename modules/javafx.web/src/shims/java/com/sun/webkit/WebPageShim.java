/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import com.sun.javafx.webkit.prism.WCBufferedContextShim;
import com.sun.javafx.webkit.prism.PrismInvokerShim;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCMouseEvent;
import com.sun.webkit.event.WCMouseWheelEvent;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCGraphicsManagerShim;
import com.sun.webkit.graphics.WCPageBackBuffer;
import com.sun.webkit.graphics.WCRectangle;
import java.awt.image.BufferedImage;

public class WebPageShim {

    public static int getFramesCount(WebPage page) {
        return page.test_getFramesCount();
    }

    private static WCGraphicsContext setupPageWithGraphics(WebPage page, int x, int y, int w, int h) {
        page.setBounds(x, y, w, h);
        // forces layout and renders the page into RenderQueue.
        page.updateContent(new WCRectangle(x, y, w, h));
        return WCBufferedContextShim.createBufferedContext(w, h);
    }

    public static BufferedImage paint(WebPage page, int x, int y, int w, int h) {
        final WCGraphicsContext gc = setupPageWithGraphics(page, x, y, w, h);
        PrismInvokerShim.runOnRenderThread(() -> {
            page.paint(gc, x, y, w, h);
        });
        return gc.getImage().toBufferedImage();
    }

    public static void mockPrint(WebPage page, int x, int y, int w, int h) {
        final WCGraphicsContext gc = setupPageWithGraphics(page, x, y, w, h);
        // almost equivalent to `PrinterJob.printPage(webview)`
        page.print(gc, x, y, w, h);
    }

    public static void mockPrintByPage(WebPage page, int pageNo, int x, int y, int w, int h) {
        final WCGraphicsContext gc = setupPageWithGraphics(page, x, y, w, h);
        // almost equivalent to `WebEngine.print(printerJob) `
        page.beginPrinting(w, h);
        page.print(gc, pageNo, w);
        page.endPrinting();
    }

    public static void click(WebPage page, int x, int y) {
        WCMouseEvent mousePressEvent =
                new WCMouseEvent(WCMouseEvent.MOUSE_PRESSED, WCMouseEvent.BUTTON1,
                    1, x, y,
                    x, y,
                    System.currentTimeMillis(),
                    false, false, false, false, false);
        WCMouseEvent mouseReleaseEvent =
                new WCMouseEvent(WCMouseEvent.MOUSE_RELEASED, WCMouseEvent.BUTTON1,
                    1, x, y,
                    x, y,
                    System.currentTimeMillis(),
                    false, false, false, false, false);
        page.dispatchMouseEvent(mousePressEvent);
        page.dispatchMouseEvent(mouseReleaseEvent);
    }

    public static void scroll(WebPage page, int x, int y, int deltaX, int deltaY) {
        WCMouseWheelEvent mouseWheelEvent =
                new WCMouseWheelEvent(x, y, x, y,
                    System.currentTimeMillis(),
                    false, false, false, false,
                    deltaX, deltaY);
        page.dispatchMouseWheelEvent(mouseWheelEvent);
    }
}
