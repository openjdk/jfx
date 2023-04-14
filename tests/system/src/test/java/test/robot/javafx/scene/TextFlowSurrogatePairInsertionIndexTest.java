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
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.tk.Toolkit;

import test.util.Util;

/*
 * Test for verifying insertion index in TextFlow surrogate pairs
 *
 * There is 1 test in this file.
 * Steps for testTextFlowSurrogatePairInsertionIndex()
 * 1. Create a TextFlow. Add Text node with surrogate pair to it.
 * 2. Move the cursor and click on the leading side of a surrogate pair text.
 * 3. Insertion index should be same as character index.
 * 4. Move the cursor and click on the trailing side of a surrogate pair text.
 * 5. Insertion index should 2 more than the character index.
 */

public class TextFlowSurrogatePairInsertionIndexTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static TextFlow textFlow;

    static volatile Stage stage;
    static volatile Scene scene;

    boolean isLeading;
    int charIndex;
    int insertionIndex;

    CountDownLatch mouseClickLatch;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
            mouseClickLatch.countDown();
        });
    }

    private void moveMouseToLeadingSide() throws Exception {
        mouseClickLatch = new CountDownLatch(1);
        mouseClick(textFlow.getLayoutX() + (textFlow.getWidth() / 4) - 10,
                    textFlow.getLayoutY() + textFlow.getHeight() / 2);
        Thread.sleep(200); // Small delay to wait for cursor to move and mouse click.
        Util.waitForLatch(mouseClickLatch, 10, "Timeout waiting for mouse click");
    }

    private void moveMouseToTrailingSide() throws Exception {
        mouseClickLatch = new CountDownLatch(1);
        mouseClick(textFlow.getLayoutX() + (textFlow.getWidth() / 4) + 10,
                    textFlow.getLayoutY() + textFlow.getHeight() / 2);
        Thread.sleep(200); // Small delay to wait for cursor to move and mouse click.
        Util.waitForLatch(mouseClickLatch, 10, "Timeout waiting for mouse click");
    }

    @Test
    public void testTextFlowSurrogatePairInsertionIndex() throws Exception {
        moveMouseToLeadingSide();
        Assert.assertTrue(isLeading);
        Assert.assertEquals(charIndex, insertionIndex);

        moveMouseToTrailingSide();
        Assert.assertFalse(isLeading);
        Assert.assertEquals(charIndex, insertionIndex - 2);
    }

    private void handleMouseEvent(MouseEvent event) {
        Point2D point = new Point2D(event.getX(), event.getY());
        HitInfo hitInfo = textFlow.hitTest(point);
        isLeading = hitInfo.isLeading();
        charIndex = hitInfo.getCharIndex();
        insertionIndex = hitInfo.getInsertionIndex();
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
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            Text text = new Text("😊😇");
            text.setFont(new Font(48));
            textFlow = new TextFlow(text);

            scene = new Scene(textFlow);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
