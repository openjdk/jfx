/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

/**
 * Tests bidi text navigation in TextArea.
 */
public class TextAreaCursorMovementTest {

    private static final int SCENE_WIDTH = 200;
    private static final int SCENE_HEIGHT = 250;

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static Robot robot;
    private static volatile Stage stage;
    private static BorderPane root;

    private static TextArea control;
    private static int curIndex;

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown(stage);
    }

    private void moveCursor(boolean forward, boolean isRtl) {
        boolean left = isRtl ^ !forward;
        Util.runAndWait(() -> {
            robot.keyType(left ? KeyCode.LEFT : KeyCode.RIGHT);
        });
    }

    private void addTextFieldContent(String text, boolean isRtl) {
        Util.runAndWait(() -> {
            if (isRtl) {
                control.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            }
            control.setText(text);
        });
    }

    @Test
    public void testRTL() {
        testCursorMovementInRTLText(true);
    }

    @Test
    public void testLTR() {
        testCursorMovementInRTLText(false);
    }

    private void testCursorMovementInRTLText(boolean isRtl) {
        String str = "Arabic يشتر\nشسيرشي";
        addTextFieldContent(str, isRtl);

        curIndex = 0;
        int prev = -1;

        for (int i = 0; i < str.length(); i++) {
            moveCursor(true, isRtl);
            Assertions.assertNotEquals(curIndex, prev);
            prev = curIndex;
        }

        prev = -1;

        for (int i = 0; i < str.length(); i++) {
            moveCursor(false, isRtl);
            Assertions.assertNotEquals(curIndex, prev);
            prev = curIndex;
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            robot = new Robot();

            control = new TextArea();
            control.caretPositionProperty().addListener((ev) -> {
                curIndex = control.getCaretPosition();
            });
            control.setFont(Font.getDefault().font(48));
            // TODO could also test with wrap text off
            control.setWrapText(true);

            root = new BorderPane(control);

            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.setTitle("TextAreaCursorMovementTest");
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
