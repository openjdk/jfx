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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

/*
 * Test for verifying insertion index in TextFlow surrogate pairs
 *
 * There are 4 tests in this file.
 * Steps for testTextFlowInsertionIndexUsingTwoEmojis()
 * 1. Create a TextFlow. Add Text node with surrogate pair to it.
 * 2. Move the cursor and click on the leading side of a surrogate pair text.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor and click on the trailing side of a surrogate pair text.
 * 5. Insertion index should 2 more than the character index.
 *
 * Steps for testTextFlowInsertionIndexUsingMultipleEmojis()
 * 1. Create a TextFlow. Add Text node with multiple emojis (surrogate pairs).
 * 2. Move the cursor to the first character and click.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor continously till last character and check that
 *    character index and insertion index increase monitonically as expected.
 *
 * Steps for testTextFlowInsertionIndexUsingTextAndEmojis()
 * 1. Create a TextFlow. Add Text node with multiple emojis (surrogate pairs).
 * 2. Move the cursor to the first character and click.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor continously till last character and check that
 *    character index and insertion index increase monitonically as expected.
 *
 * Steps for testTextFlowInsertionIndexUsingEmbeddedTextNodes()
 * 1. Create a TextFlow. Add a Text node with text and another with emojis.
 * 2. Move the cursor to the first character and click.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor continously till last character and check that
 *    character index and insertion index increase monitonically as expected.
 *
 * Steps for testTextFlowInsertionIndexWhenMouseMovedOutsideText()
 * 1. Create a TextFlow. Add a Text node with text and emojis.
 * 2. Move the cursor to the first character and click.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor towards bottom of the application window check that
 *    chracter index and insertion index are as expected.
 * This test is implemented to test insertion index initialization
 * when text run is not used to calculate character index.
 *
 * Steps for testTextFlowInsertionIndexUsingWrappedText()
 * 1. Create a TextFlow. Add a Text node with text and emojis whose length is
 *    more than the size of application window.
 * 2. Move the cursor from first character to last character.
 * 3. Character index should increase monotonically as expected.
 * 4. Insertion index should also increase as expected along with character index.
 */

public class TextFlowSurrogatePairInsertionIndexTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Random random;
    static Robot robot;
    static TextFlow textFlow;
    static Text text;
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

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mouseClick(MouseButton.PRIMARY);
        });
    }

    private void moveMouseOverTextFlow(double x, double y) throws Exception {
        mouseClick(textFlow.getLayoutX() + x,
                    textFlow.getLayoutY() + y);
    }

    private void addTwoEmojis() {
        Util.runAndWait(() -> {
            text = new Text("😊😇");
            text.setFont(new Font(48));
            textFlow.getChildren().clear();
            textFlow.getChildren().setAll(text);
        });
    }

    private void addMultipleEmojis() {
        Util.runAndWait(() -> {
            text = new Text("😊😇💙🦋🏁🔥");
            text.setFont(new Font(48));
            textFlow.getChildren().clear();
            textFlow.getChildren().setAll(text);
        });
    }

    private void addTextAndEmojis() {
        Util.runAndWait(() -> {
            text = new Text("Text 😊😇💙🦋🔥");
            text.setFont(new Font(48));
            textFlow.getChildren().clear();
            textFlow.getChildren().setAll(text);
        });
    }

    private void addTwoTextNodes() {
        Util.runAndWait(() -> {
            text = new Text("Text");
            text.setFont(new Font(48));

            emoji = new Text("😊😇");
            emoji.setFont(new Font(48));

            textFlow.getChildren().clear();
            textFlow.getChildren().setAll(text, emoji);
        });
    }

    private void addLongText() {
        Util.runAndWait(() -> {
            text = new Text("[This is text 😀😃😄😁😆 🙂🙃😉😊😇]");
            text.setFont(new Font(48));

            textFlow.getChildren().clear();
            textFlow.getChildren().setAll(text);
        });
    }

    @Test
    public void testTextFlowInsertionIndexUsingTwoEmojis() throws Exception {
        addTwoEmojis();
        Util.waitForIdle(scene);

        moveMouseOverTextFlow(X_LEADING_OFFSET, Y_OFFSET);
        Assert.assertTrue(isLeading);
        Assert.assertEquals(charIndex, insertionIndex);

        moveMouseOverTextFlow(X_TRAILING_OFFSET, Y_OFFSET);
        Assert.assertFalse(isLeading);
        Assert.assertEquals(charIndex, insertionIndex - 2);
    }

    @Test
    public void testTextFlowInsertionIndexUsingMultipleEmojis() throws Exception {
        addMultipleEmojis();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();
        double x = 0.0;
        while (charIndex < textLength - 2) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            }
            x += step();
        }
    }

    @Test
    public void testTextFlowInsertionIndexUsingTextAndEmojis() throws Exception {
        addTextAndEmojis();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();
        double x = 0.0;
        while (charIndex < textLength - 2) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else if (!isLeading && charIndex < 5) {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            }
            x += step();
        }
    }

    @Test
    public void testTextFlowInsertionIndexUsingEmbeddedTextNodes() throws Exception {
        addTwoTextNodes();
        Util.waitForIdle(scene);

        int textLength = text.getText().length();
        textLength += emoji.getText().length();
        double x = 0.0;
        while (charIndex < textLength - 2) {
            moveMouseOverTextFlow(x, Y_OFFSET);
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else if (isSurrogatePair) {
                Assert.assertEquals(charIndex, insertionIndex - 2);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            x += step();
        }
    }

    @Test
    public void testTextFlowInsertionIndexWhenMouseMovedOutsideText() throws Exception {
        addTextAndEmojis();
        Util.waitForIdle(scene);

        double x = 0.0;
        while (x < (HEIGHT - Y_OFFSET)) {
            moveMouseOverTextFlow(X_LEADING_OFFSET, (Y_OFFSET + x));
            if (isLeading) {
                Assert.assertEquals(charIndex, insertionIndex);
            } else {
                Assert.assertEquals(charIndex, insertionIndex - 1);
            }
            x += step();
        }
    }

    @Test
    public void testTextFlowInsertionIndexUsingWrappedText() throws Exception {
        addLongText();
        Util.waitForIdle(scene);

        for (int y = 0; y < 2; y++) {
            for (double x = 0.0; x < (WIDTH - X_LEADING_OFFSET); x += step()) {
                moveMouseOverTextFlow(x, (Y_OFFSET + (Y_OFFSET * (y * 2))));
                if (isLeading) {
                    Assert.assertEquals(charIndex, insertionIndex);
                } else if (isSurrogatePair) {
                    Assert.assertEquals(charIndex, insertionIndex - 2);
                } else {
                    Assert.assertEquals(charIndex, insertionIndex - 1);
                }
            }
        }
    }

    private void handleMouseEvent(MouseEvent event) {
        Point2D point = new Point2D(event.getX(), event.getY());
        HitInfo hitInfo = textFlow.hitTest(point);
        isLeading = hitInfo.isLeading();
        charIndex = hitInfo.getCharIndex();
        insertionIndex = hitInfo.getInsertionIndex();

        String testString = text.getText();
        if (charIndex >= testString.length() && emoji != null) {
            testString += emoji.getText();
        }
        if (charIndex < testString.length()) {
            char c = testString.charAt(charIndex);
            isSurrogatePair = Character.isSurrogate(c);
        }

        Assert.assertTrue(insertionIndex >= 0);
        String s = hitInfo.toString();
        Assert.assertTrue(s != null);
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
        // if any test fails, we can use the seed found in the log to reproduce exact sequence of events
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
