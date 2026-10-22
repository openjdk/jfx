/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import test.util.Util;

/// Tests interaction with tab stop policy and the real layout.
public class TextAreaTabPolicyTest extends Application {

    private static final double TOLERANCE = 1e-4;
    private static final int LONG_TEXT_LENGTH = 300;

    private static CountDownLatch startupLatch;
    private static BorderPane root;
    private TextArea textArea;
    private static TextAreaSkin skin;

    @Override
    public void start(Stage stage) throws Exception {
        setUncaughtExceptionHandler();

        root = new BorderPane();
        stage.setScene(new Scene(root, 800, 400));
        stage.setOnShown(l -> {
            Platform.runLater(() -> startupLatch.countDown());
        });
        stage.show();
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        startupLatch = new CountDownLatch(1);
        Util.launch(startupLatch, TextAreaTabPolicyTest.class);
    }

    @AfterAll
    public static void afterAll() {
        Util.shutdown();
    }

    @BeforeEach
    public void beforeEach() {
        Util.runAndWait(() -> {
            textArea = new TextArea();
            skin = new TextAreaSkin(textArea);
            textArea.setSkin(skin);
            root.setCenter(textArea);
        });
    }

    // TODO could be integrated into Util.launch()
    private static void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, e) -> {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, e);
            }
        });
    }

    // TODO could be integrated into Util.shutdown()
    private static void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    private double caretPos(int index) {
        Rectangle2D r = skin.getCharacterBounds(index);
        return r.getMinX();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        true, true
        true, false
        false, true
        false, false
        """)
    public void testPolicy(boolean wrap, boolean longText) {
        Util.runAndWait(() -> {
            textArea.setWrapText(wrap);
            textArea.setText(
                """
                \tT
                \u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020T
                """);
            if (longText) {
                String s = "x ".repeat(LONG_TEXT_LENGTH / 2);
                textArea.insertText(2, s);
            }
            // scroll to view for convenience, no effect on test
            textArea.home();

            assertTab(longText);
        });

        Util.runAndWait(() -> {
            textArea.setFont(Font.font("Monospaced", 32));
            assertTab(longText);
        });
    }

    private void assertTab(boolean longText) {
        var p1 = caretPos(1);
        var p2 = caretPos(11 + (longText ? LONG_TEXT_LENGTH : 0));
        assertEquals(p1, p2, TOLERANCE);
    }
}
