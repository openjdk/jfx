/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import javafx.scene.input.PickResult;
import javafx.scene.Node;
import javafx.geometry.Point3D;

/*
 * Test for verifying character index of Text nodes embedded in TextFlow.
 *
 * There are 7 tests in this file.
 * Steps for testTwoTextNodesCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes with only text.
 * 2. Move the cursor from first character to last character with a random
 *    incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testTextAndEmojiCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes, one with text and one with emojis.
 * 2. Move the cursor from first character to last character with a random
 *    incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testEmojiAndTextCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes, one with emojis and one with text.
 * 2. Move the cursor from first character to last character with a random
 *    incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testTwoEmojiNodesCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes containing only emojis.
 * 2. Move the cursor from first character to last character with a random
 *    incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testWrappedTextNodesCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes which gets wrapped.
 * 2. Move the cursor from first character to last character on both lines
 *    with a random incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testMultiLineTextNodesCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes with emojis where new line is explicitly added.
 * 2. Move the cursor from first character to last character on both lines
 *    with a random incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 *
 * Steps for testMultiLineEmojisCharIndexEmbeddedInTexFlow()
 * 1. Create a TextFlow. Add two Text nodes with emojis where new line is explicitly added.
 * 2. Move the cursor from first character to last character on both lines
 *    with a random incement generated in step() method.
 * 3. Character index should change as expected and it is relative
 *    to the Text node.
 */

public class TextCharacterIndexTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Random random;
    static Robot robot;
    static TextFlow textFlow;
    static Text textOne;
    static Text textTwo;
    static Text textThree;
    static Text emoji;

    static volatile Stage stage;
    static volatile Scene scene;

    static final int WIDTH = 500;
    static final int HEIGHT = 150;

    final int Y_OFFSET = 25;
    final int X_LEADING_OFFSET = 10;
    final int X_TRAILING_OFFSET = 40;

    boolean isLeading;
    boolean isSurrogatePair;
    int charIndex;
    int insertionIndex;
    int textFlowCharIndex;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            Window w = scene.getWindow();
            robot.mouseMove((int) (w.getX() + scene.getX() + x),
                    (int) (w.getY() + scene.getY() + y));
            robot.mouseClick(MouseButton.PRIMARY);
        });
    }

    private void moveMouseOverTextFlow(double x, double y) throws Exception {
        mouseClick(textFlow.getLayoutX() + x,
                    textFlow.getLayoutY() + y);
    }

    private void addTwoTextNodes() {
        Util.runAndWait(() -> {
            textOne = new Text("This is Text");
            textOne.setFont(new Font(48));
            textTwo = new Text("This is Text");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addTextAndEmojisNoes() {
        Util.runAndWait(() -> {
            textOne = new Text("Text: ");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊😇💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addEmojisAndTextNode() {
        Util.runAndWait(() -> {
            textOne = new Text("😊😇💙🦋🏁🔥");
            textOne.setFont(new Font(48));
            textTwo = new Text("Text");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addTwoEmojiNodes() {
        Util.runAndWait(() -> {
            textOne = new Text("😊💙🦋🏁🔥");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addWrappedTextNodes() {
        Util.runAndWait(() -> {
            textOne = new Text("This is Text This is Text This is Text");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addMultiLineTextNodes() {
        Util.runAndWait(() -> {
            textOne = new Text("This is Text This is Text");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addMultiLineEmojisInTextNodes() {
        Util.runAndWait(() -> {
            textOne = new Text("😊💙🦋🏁🔥😊💙🦋🏁🔥😊💙🦋🏁🔥");
            textOne.setFont(new Font(48));
            textTwo = new Text("This is Text");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addMultipleTexNodeMultiLineRepeatedText() {
        Util.runAndWait(() -> {
            textOne = new Text("This is Text");
            textOne.setFont(new Font(48));
            textTwo = new Text("This is Text This is Text");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addMultipleTextNodeAlternativelyRepeatedText() {
        Util.runAndWait(() -> {
            textOne = new Text("Text");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textThree = new Text("Text");
            textThree.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo, textThree);
        });
    }

    private void addMultipleTexNodeMultiLineRepeatedEmojis() {
        Util.runAndWait(() -> {
            textOne = new Text("😊💙🦋🏁🔥");
            textOne.setFont(new Font(48));
            textTwo = new Text("😊💙🦋🏁🔥😊💙🦋🏁🔥");
            textTwo.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo);
        });
    }

    private void addMultipleTextNodeAlternativelyRepeatedEmoji() {
        Util.runAndWait(() -> {
            textOne = new Text("😊💙🦋🏁🔥");
            textOne.setFont(new Font(48));
            textTwo = new Text("Text");
            textTwo.setFont(new Font(48));
            textThree = new Text("😊💙🦋🏁🔥");
            textThree.setFont(new Font(48));
            textFlow.getChildren().setAll(textOne, textTwo, textThree);
        });
    }

    @Test
    public void testTwoTextNodesCharIndexEmbeddedInTextFlow() throws Exception {
        addTwoTextNodes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = 0.0;
        while (x < (WIDTH - X_LEADING_OFFSET)) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
            x += step();
        }
    }

    @Test
    public void testTextAndEmojiCharIndexEmbeddedInTextFlow() throws Exception {
        addTextAndEmojisNoes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = 0.0;
        while (x < (WIDTH - X_LEADING_OFFSET)) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else if (isSurrogatePair) {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
            x += step();
        }
    }

    @Test
    public void testEmojiAndTextCharIndexEmbeddedInTextFlow() throws Exception {
        addEmojisAndTextNode();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = 0.0;
        while (x < (WIDTH - X_LEADING_OFFSET)) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else if (isSurrogatePair) {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
            x += step();
        }
    }

    @Test
    public void testTwoEmojiNodesCharIndexEmbeddedInTextFlow() throws Exception {
        addTwoEmojiNodes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = 0.0;
        while (x < (WIDTH - X_LEADING_OFFSET)) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else if (isSurrogatePair) {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            }
            else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
            x += step();
        }
    }

    @Test
    public void testWrappedTextNodesCharIndexEmbeddedInTextFlow() throws Exception {
        addWrappedTextNodes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
        }
    }

    @Test
    public void testMultiLineTextNodesCharIndexEmbeddedInTextFlow() throws Exception {
        addMultiLineTextNodes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
        }
    }

    @Test
    public void testMultiLineEmojisCharIndexEmbeddedInTextFlow() throws Exception {
        addMultiLineEmojisInTextNodes();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
        }
    }

    @Test
    public void testMultiTextNodesMultiLineRepeatedTextEmbeddedInTextFlow() throws Exception {
        addMultipleTexNodeMultiLineRepeatedText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
        }
    }

    @Test
    public void testMultiTextNodesAlternativelyRepeatedTextEmbeddedInTextFlow() throws Exception {
        addMultipleTextNodeAlternativelyRepeatedText();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, Y_OFFSET);
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
    }

    @Test
    public void testMultiTextNodesMultiLineRepeatedTEmojiEmbeddedInTextFlow() throws Exception {
        addMultipleTexNodeMultiLineRepeatedEmojis();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textOneLength, textTwoLength));
                x += step();
            }
        }
    }

    @Test
    public void testMultiTextNodesAlternativelyRepeatedEmojisEmbeddedInTextFlow() throws Exception {
        addMultipleTextNodeAlternativelyRepeatedEmoji();
        Util.waitForIdle(scene);

        int textOneLength = textOne.getText().length();
        int textTwoLength = textTwo.getText().length();
        int textThreeLength = textTwo.getText().length();

        for (int y = 0; y < 2; y++) {
            double x = 0.0;
            while (x < (WIDTH - X_LEADING_OFFSET)) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
                Assert.assertTrue(charIndex < Math.max(textThreeLength, Math.max(textOneLength, textTwoLength)));
                x += step();
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
            isSurrogatePair = Character.isSurrogate(t.getText().charAt(charIndex));
        }

        Point2D point = new Point2D(event.getX(), event.getY());
        HitInfo textFlowHitInfo = textFlow.hitTest(point);
        textFlowCharIndex = textFlowHitInfo.getCharIndex();
    }

    private double step() {
        return 1.0 + random.nextDouble() * 8.0;
    }

    @After
    public void resetUI() {
        Platform.runLater(() -> {
            textFlow.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
        });
    }

    @Before
    public void setupUI() {
        Platform.runLater(() -> {
            textFlow.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMouseEvent);
        });
    }

    @BeforeClass
    public static void initFX() {
        long seed = new Random().nextLong();
        System.out.println("seed=" + seed);
        random = new Random(seed);

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            textFlow = new TextFlow();
            scene = new Scene(textFlow, WIDTH, HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
