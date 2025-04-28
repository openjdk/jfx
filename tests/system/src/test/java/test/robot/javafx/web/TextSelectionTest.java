/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.web;

import java.util.concurrent.CountDownLatch;

import javafx.concurrent.Worker;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import test.robot.testharness.RobotTestBase;
import test.util.Util;

public class TextSelectionTest extends RobotTestBase {

    private static final String html = """
        <html>
        <body><font size="2">&nbsp&nbsp&nbsp&nbsp some text</font></body>
        </html>
        """;

    private static CountDownLatch webviewLoadLatch = new CountDownLatch(1);
    private volatile Color colorBefore;
    private volatile Color colorAfter;

    @BeforeEach
    public void beforeEach() {
        Util.runAndWait(() -> {
            WebView webview = new WebView();
            webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    webviewLoadLatch.countDown();
                }
            });
            webview.getEngine().loadContent(html);
            contentPane.setCenter(webview);
        });
        Util.waitForLatch(webviewLoadLatch, 10, "Timeout waiting for web content to load");
    }

    // ========================== TEST CASE ==========================
    @Test
    @Timeout(value=20)
    public void testTextSelection() {

        Util.sleep(200);
        int x = (int)(scene.getWindow().getX() + scene.getX() + 22);
        int y = (int)(scene.getWindow().getY() + scene.getY() + 15);

        Util.parkCursor(robot);
        Util.runAndWait(() -> colorBefore = robot.getPixelColor(x, y));

        Util.runAndWait(() -> robot.mouseMove(x, y));
        Util.doubleClick(robot);
        Util.sleep(500); // Wait for the selection highlight to be drawn

        Util.parkCursor(robot);
        Util.runAndWait(() -> colorAfter = robot.getPixelColor(x, y));

        Assertions.assertNotEquals(colorBefore, colorAfter,
            "Selection color did not change after double click");
    }
}
