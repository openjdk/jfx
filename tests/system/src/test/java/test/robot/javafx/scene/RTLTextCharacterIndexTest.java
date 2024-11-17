/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

/*
 * Test for verifying character index of Text nodes in RTL orientation.
 *
 * There are 6 tests in this file.
 * Here, the scene node orientation is set to RTL for all the tests.
 * Steps for testTextInfoForRTLEnglishText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add only english text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 4. Character index should change from highest value to lowest
 *    as expected.
 *
 * Steps for testTextInfoForRTLArabicText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add only arabic text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should increment as expected since it is RTL text.
 *
 * Steps for testTextInfoForRTLEnglishArabicText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add both english and arabic text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 4. Character index should change in decreasing order for english text
 *    and in increasing order for arabic text.
 *
 * Steps for testTextInfoForMultiLineRTLEnglishText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add two lines of only english text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 4. Character index should change in decreasing order as expected.
 *
 * Steps for testTextInfoForMultiLineRTLEnglishArabicText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add two lines of both english and arabic text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 4. Character index should change in decreasing order for english text
 *    and increasing order for arabic text.
 *
 * Steps for testTextInfoForMultiLineRTLArabicText()
 * 1. Create a Text node and add it to the scene using a VBox.
 * 2. Add two lines of only arabic text to the Text node.
 * 3. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 4. Character index should change in increasing order as expected.
 */

public class RTLTextCharacterIndexTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Random random;
    static Robot robot;
    static Text text;
    static VBox vBox;

    static volatile Stage stage;
    static volatile Scene scene;

    static final int WIDTH = 500;
    static final int HEIGHT = 200;

    static final int Y_OFFSET = 30;
    static final int X_LEADING_OFFSET = 10;

    boolean isLeading;
    boolean textFlowIsLeading;
    int charIndex;
    int insertionIndex;
    int textFlowCharIndex;
    int textFlowInsertionIndex;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            Window w = scene.getWindow();
            robot.mouseMove(w.getX() + scene.getX() + x,
                    w.getY() + scene.getY() + y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
    }

    private void moveMouseOverText(double x, double y) throws Exception {
        mouseClick(text.getLayoutX() + x,
                    text.getLayoutY() / 2 + y);
    }

    private void addRTLEnglishText() {
        Util.runAndWait(() -> {
            text.setText("This is text");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    private void addRTLArabicText() {
        Util.runAndWait(() -> {
            text.setText("شسيبلاتنم");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    private void addRTLEnglishArabicText() {
        Util.runAndWait(() -> {
            text.setText("Arabic:شسيبلاتنم");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    private void addMultiLineRTLEnglishText() {
        Util.runAndWait(() -> {
            text.setText("This is text\nThis is text");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    private void addMultiLineRTLEnglishArabicText() {
        Util.runAndWait(() -> {
            text.setText("Arabic:شسيبلاتنم\nArabic:شسيبلاتنم");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    private void addMultiLineRTLArabicText() {
        Util.runAndWait(() -> {
            text.setText("شسيبلاتنم شسيبلاتنم\nشسيبلاتنم شسيبلاتنم");
            text.setFont(new Font(48));
            vBox.getChildren().setAll(text);
        });
    }

    @Test
    public void testTextInfoForRTLEnglishText() throws Exception {
        addRTLEnglishText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverText(x, 0);
            if (isLeading) {
                Assertions.assertEquals(charIndex, insertionIndex);
            } else {
                Assertions.assertEquals(charIndex, insertionIndex - 1);
            }
            Assertions.assertTrue(charIndex < textLength);
            x -= step();
        }
    }

    @Test
    public void testTextInfoForRTLArabicText() throws Exception {
        addRTLArabicText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverText(x, 0);
            if (isLeading) {
                Assertions.assertEquals(charIndex, insertionIndex);
            } else {
                Assertions.assertEquals(charIndex, insertionIndex - 1);
            }
            Assertions.assertTrue(charIndex < textLength);
            x -= step();
        }
    }

    @Test
    public void testTextInfoForRTLEnglishArabicText() throws Exception {
        addRTLEnglishArabicText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverText(x, 0);
            if (isLeading) {
                Assertions.assertEquals(charIndex, insertionIndex);
            } else {
                Assertions.assertEquals(charIndex, insertionIndex - 1);
            }
            Assertions.assertTrue(charIndex < textLength);
            x -= step();
        }
    }

    @Test
    public void testTextInfoForMultiLineRTLEnglishText() throws Exception {
        addMultiLineRTLEnglishText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverText(x, (Y_OFFSET * (y * 2)));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < textLength);
                x -= step();
            }
        }
    }

    @Test
    public void testTextInfoForMultiLineRTLEnglishArabicText() throws Exception {
        addMultiLineRTLEnglishArabicText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverText(x, (Y_OFFSET * (y * 2)));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < textLength);
                x -= step();
            }
        }
    }

    @Test
    public void testTextInfoForMultiLineRTLArabicText() throws Exception {
        addMultiLineRTLArabicText();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverText(x, (Y_OFFSET * (y * 2)));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < textLength);
                x -= step();
            }
        }
    }

    private void handleTextMouseEvent(MouseEvent event) {
        PickResult pick = event.getPickResult();
        Node n = pick.getIntersectedNode();

        if (n != null && n instanceof Text t) {
            Point3D p3 = pick.getIntersectedPoint();
            Point2D p = new Point2D(p3.getX(), p3.getY());
            HitInfo hitInfo = t.hitTest(p);

            isLeading = hitInfo.isLeading();
            charIndex = hitInfo.getCharIndex();
            insertionIndex = hitInfo.getInsertionIndex();
        }
    }

    private double step() {
        return 1.0 + random.nextDouble() * 8.0;
    }

    @AfterEach
    public void resetUI() {
        Platform.runLater(() -> {
            text.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleTextMouseEvent);
        });
    }

    @BeforeEach
    public void setupUI() {
        Platform.runLater(() -> {
            text.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleTextMouseEvent);
        });
    }

    @BeforeAll
    public static void initFX() {
        long seed = new Random().nextLong();
        System.out.println("seed=" + seed);
        random = new Random(seed);

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            text = new Text();
            vBox = new VBox();

            scene = new Scene(vBox, WIDTH, HEIGHT);
            scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
