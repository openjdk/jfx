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
import javafx.scene.text.TextFlow;
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
 * Test for verifying character index of Text nodes embedded in TextFlow in RTL orientation.
 *
 * There are 5 tests in this file.
 * Here, the scene node orientation is set to RTL for all the tests.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLArabicText()
 * 1. Create a TextFlow. Add a Text nodes with only arabic text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in ascending order as expected.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLEnglishText()
 * 1. Create a TextFlow. Add a Text nodes with only english text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in descending order as expected.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLMultipleTextNodes()
 * 1. Create a TextFlow. Add two Text nodes with english and arabic text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in decreasing order for english text
 *    and in increasing order for arabic text.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLMultipleMultiLineEnglishArabicTextNodes()
 * 1. Create a TextFlow. Add three Text nodes with english and arabic text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in decreasing order for english text
 *    and in increasing order for arabic text.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLMultipleMultiLineEnglishTextNodes()
 * 1. Create a TextFlow. Add three Text nodes with only english text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in decreasing order for english text.
 *
 * Steps for testTextAndTextFlowHitInfoForRTLMultipleMultiLineArabicTextNodes()
 * 1. Create a TextFlow. Add three Text nodes with only arabic text.
 * 2. Move the cursor from right to left with a random
 *    decrement value generated in step() method.
 * 3. Character index should change in increasing order for arabic text.
 *
 */

public class RTLTextFlowCharacterIndexTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Random random;
    static Robot robot;
    static TextFlow textFlow;
    static Text textOne;
    static Text textTwo;
    static Text textThree;
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

    private void moveMouseOverTextFlow(double x, double y) throws Exception {
        mouseClick(textFlow.getLayoutX() + x,
                    textFlow.getLayoutY() + y);
    }

    private void addRTLArabicText() {
        Util.runAndWait(() -> {
            textOne.setText("شسيبلاتنم");
            textOne.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne);
            vBox.getChildren().setAll(textFlow);
        });
    }

    private void addRTLEnglishText() {
        Util.runAndWait(() -> {
            textOne.setText("This is text");
            textOne.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne);
            vBox.getChildren().setAll(textFlow);
        });
    }

    private void addMultiNodeRTLEnglishArabicText() {
        Util.runAndWait(() -> {
            textOne.setText("Arabic:");
            textOne.setFont(new Font(48));
            textTwo.setText("شسيبلاتنم");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
            vBox.getChildren().setAll(textFlow);
        });
    }

    private void addMultiLineMultiNodeRTLEnglishArabicText() {
        Util.runAndWait(() -> {
            textOne.setText("Arabic:");
            textOne.setFont(new Font(48));
            textTwo.setText("شسيبلاتنضصثقفغ");
            textTwo.setFont(new Font(48));
            textThree.setText("حخهعغقثصضشسيبل");
            textThree.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo, textThree);
            vBox.getChildren().setAll(textFlow);
        });
    }

    private void addMutliLineMultiNodeRTLEnglishText() {
        Util.runAndWait(() -> {
            textOne.setText("First line of text");
            textOne.setFont(new Font(48));
            textTwo.setText("Second line of text");
            textTwo.setFont(new Font(48));
            textThree.setText("Third line of text");
            textThree.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo, textThree);
            vBox.getChildren().setAll(textFlow);
        });
    }

    private void addMutliLineMultiNodeRTLArabicText() {
        Util.runAndWait(() -> {
            textOne.setText("شسيبلا تنضصثقفغ");
            textOne.setFont(new Font(48));
            textTwo.setText("حخهعغقث صضشسيبل");
            textTwo.setFont(new Font(48));
            textThree.setText("ضصثقف");
            textThree.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo, textThree);
            vBox.getChildren().setAll(textFlow);
        });
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLArabicText() throws Exception {
        addRTLArabicText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
            Assertions.assertTrue(charIndex < textOneLength);
            Assertions.assertTrue(textFlowCharIndex < textOneLength);
            x -= step();
        }
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLEnglishText() throws Exception {
        addRTLEnglishText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
            Assertions.assertTrue(charIndex < textOneLength);
            Assertions.assertTrue(textFlowCharIndex < textOneLength);
            x -= step();
        }
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLMultipleTextNodes() throws Exception {
        addMultiNodeRTLEnglishArabicText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = WIDTH - X_LEADING_OFFSET;
        while (x > X_LEADING_OFFSET) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
            Assertions.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
            Assertions.assertTrue(textFlowCharIndex < textOneLength + textTwoLength);
            x -= step();
        }
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLMultipleMultiLineEnglishArabicTextNodes() throws Exception {
        addMultiLineMultiNodeRTLEnglishArabicText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();
        int textThreeLength = textThree.getText().length();

        for (int y = 0; y < 3; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < Math.max(textThreeLength, Math.max(textOneLength, textTwoLength)));
                Assertions.assertTrue(textFlowCharIndex < textOneLength + textTwoLength + textThreeLength);
                x -= step();
            }
        }
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLMultipleMultiLineEnglishTextNodes() throws Exception {
        addMutliLineMultiNodeRTLEnglishText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();
        int textThreeLength = textThree.getText().length();

        for (int y = 0; y < 3; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < Math.max(textThreeLength, Math.max(textOneLength, textTwoLength)));
                Assertions.assertTrue(textFlowCharIndex < textOneLength + textTwoLength + textThreeLength);
                x -= step();
            }
        }
    }

    @Test
    public void testTextAndTextFlowHitInfoForRTLMultipleMultiLineArabicTextNodes() throws Exception {
        addMutliLineMultiNodeRTLArabicText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();
        int textThreeLength = textThree.getText().length();

        for (int y = 0; y < 3; y++) {
            double x = WIDTH - X_LEADING_OFFSET;
            while (x > X_LEADING_OFFSET) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assertions.assertEquals(charIndex, insertionIndex);
                } else {
                    Assertions.assertEquals(charIndex, insertionIndex - 1);
                }
                if (textFlowIsLeading) {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex);
                } else {
                    Assertions.assertEquals(textFlowCharIndex, textFlowInsertionIndex - 1);
                }
                Assertions.assertTrue(charIndex < Math.max(textThreeLength, Math.max(textOneLength, textTwoLength)));
                Assertions.assertTrue(textFlowCharIndex < textOneLength + textTwoLength + textThreeLength);
                x -= step();
            }
        }
    }

    private void handleMouseEvent(MouseEvent event) {
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

        Point2D point = new Point2D(event.getX(), event.getY());
        HitInfo textFlowHitInfo = textFlow.hitTest(point);
        textFlowIsLeading = textFlowHitInfo.isLeading();
        textFlowCharIndex = textFlowHitInfo.getCharIndex();
        textFlowInsertionIndex = textFlowHitInfo.getInsertionIndex();
    }

    private double step() {
        return 1.0 + random.nextDouble() * 8.0;
    }

    @AfterEach
    public void resetUI() {
        Platform.runLater(() -> {
            textFlow.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textOne.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textTwo.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textThree.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
        });
    }

    @BeforeEach
    public void setupUI() {
        Platform.runLater(() -> {
            textFlow.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textOne.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textTwo.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
            textThree.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
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
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            textOne = new Text();
            textTwo = new Text();
            textThree = new Text();
            textFlow = new TextFlow();
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
