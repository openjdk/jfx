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
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.TextField;
import javafx.scene.Group;
import javafx.scene.robot.Robot;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.util.Util;

/*
 * Test for verifying cursor movement in TextField using keyboard.
 * There are 4 tests in this file
 *
 * Steps for testCursorMovementInMixedText()
 * 1. Create a TextField and add text which contains both RTL and LTR text.
 *    Set TextField node orientation to RIGHT_TO_LEFT
 * 2. Move the cursor to the left by pressing LEFT key on keyboard.
 * 3. Keep pressing the LEFT key until end of text.
 * 4. Verify that cursor position changes by checking the caret position.
 * 5. Cursor movement is verified by checking that caret position
 *    is not equal to previous position.
 *
 * Steps for testCursorMovementInMixedTextRTLNotSet()
 * 1. Create a TextField and add text which contains both RTL and LTR text.
 *    Set TextField node orientation to LEFT_TO_RIGHT
 * 2. Move the cursor to the left by pressing LEFT key on keyboard.
 * 3. Keep pressing the LEFT key until end of text.
 * 4. Verify that cursor position changes by checking the caret position.
 * 5. Cursor movement is verified by checking that caret position
 *    is not equal to previous position.
 *
 * Steps for testCursorMovementInLTRText()
 * 1. Create a TextField and add LTR text.
 * 2. Move the cursor to the right by pressing RIGHT key on keyboard.
 * 3. Keep pressing the RIGHT key until end of text.
 * 4. Verify that cursor position changes by checking the caret position.
 * 5. Cursor movement is verified by checking that caret position
 *    is not equal to previous position.
 *
 * Steps for testCursorMovementInRTLText()
 * 1. Create a TextField and add RTL text.
 * 2. Move the cursor to the left by pressing LEFT key on keyboard.
 * 3. Keep pressing the LEFT key until end of text.
 * 4. Verify that cursor position changes by checking the caret position.
 * 5. Cursor movement is verified by checking that caret position
 *    is not equal to previous position.
 */
public class TextFieldCursorMovementTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch caretPositionLatch;
    static Robot robot;
    static TextField textField;

    static volatile Stage stage;
    static volatile Scene scene;

    static int curIndex = 0;
    static int prevIndex = -1;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;

    private void moveCursorToLeft() {
        Util.runAndWait(() -> {
            robot.keyType(KeyCode.LEFT);
        });
    }

    private void moveCursorToRight() {
        Util.runAndWait(() -> {
            robot.keyType(KeyCode.RIGHT);
        });
    }

    private void addTextFieldContent(String text, boolean isRtl) {
        Util.runAndWait(() -> {
            textField.setText(text);
            if (isRtl) {
                textField.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            } else {
                textField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            }
        });
    }

    //JDK-8242616
    @Test
    public void testCursorMovementInMixedText() throws Exception {
        String str = "Arabic يشترشسيرشي";
        addTextFieldContent(str, true);

        for (int i =0; i< str.length(); i++) {
            moveCursorToLeft();
            Assertions.assertNotEquals(curIndex, prevIndex);
            prevIndex = curIndex;
        }
    }

    @Test
    public void testCursorMovementInMixedTextRTLNotSet() throws Exception {
        String str = "Arabic يشترشسيرشي";
        addTextFieldContent(str, false);

        for (int i =0; i< str.length(); i++) {
            moveCursorToRight();
            Assertions.assertNotEquals(curIndex, prevIndex);
            prevIndex = curIndex;
        }
    }

    @Test
    public void testCursorMovementInLTRText() throws Exception {
        String str = "This is a text";
        addTextFieldContent(str, false);

        for (int i =0; i< str.length(); i++) {
            moveCursorToRight();
            Assertions.assertNotEquals(curIndex, prevIndex);
            prevIndex = curIndex;
        }
    }

    @Test
    public void testCursorMovementInRTLText() throws Exception {
        String str = "يشترشسيرشي";
        addTextFieldContent(str, true);

        for (int i =0; i< str.length(); i++) {
            moveCursorToLeft();
            Assertions.assertNotEquals(curIndex, prevIndex);
            prevIndex = curIndex;
        }
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            textField = new TextField();
            textField.caretPositionProperty().addListener((event) -> {
                curIndex = textField.getCaretPosition();
            });
            Group group = new Group(textField);
            scene = new Scene(group, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
