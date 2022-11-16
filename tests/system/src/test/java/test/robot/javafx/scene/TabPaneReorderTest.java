/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.robot.Robot;
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
 * Test for verifying that dragging a tab and dropping it back at
 * it's original position does not change the order of tabs.
 *
 * Test steps
 * 1. Create TabPane with 9 tabs.
 * 2. Drag a tab by dragDistance and drop it back to it's original position.
 * 3. Verify that NO permutation change event is received.
 * 4. Verify that orderof tabs remains same by navigating using right arrow key.
 *
 * Perform the test for four Sides.
 */
public class TabPaneReorderTest {
    static CountDownLatch selectionLatch;
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static HBox root;
    static volatile Scene scene;
    static volatile Stage stage;
    TabPane tabPane;
    int tabPaneWidth;
    int tabPaneHeight;
    int dragDistance;
    static final int SCENE_WIDTH = 400;
    static final int SCENE_HEIGHT = 400;
    static final float DRAG_DISTANCE_PERCENTAGE = 0.25f;
    static final float DRAG_TAB = 4.0f;
    static final int DX = 15;
    static final int DY = DX;
    static final int TAB_COUNT = 9;
    String tabOrder;
    String currentTabOrder;
    boolean isTabListReorderd;
    ListChangeListener<Tab> reorderListener = c -> {
        while (c.next()) {
            if (c.wasPermutated()) {
                isTabListReorderd = true;
            }
        }
    };

    @Test
    public void testReorderTop() {
        tabPane.getSelectionModel().select(0);
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.TOP);
        dragDistance = (int)(tabPaneWidth * DRAG_DISTANCE_PERCENTAGE);
        testReorder((int)(tabPaneWidth / TAB_COUNT * DRAG_TAB), DY, true);
    }

    @Test
    public void testReorderBottom() {
        tabPane.getSelectionModel().select(8);
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.BOTTOM);
        dragDistance = (int)(tabPaneWidth * DRAG_DISTANCE_PERCENTAGE);
        testReorder((int)(tabPaneWidth / TAB_COUNT * DRAG_TAB),
                    tabPaneHeight - DY, true);
    }

    @Test
    public void testReorderLeft() {
        tabPane.getSelectionModel().select(8);
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.LEFT);
        dragDistance = (int)(tabPaneHeight * DRAG_DISTANCE_PERCENTAGE);
        testReorder(DX, (int)(tabPaneHeight / TAB_COUNT * DRAG_TAB), false);
    }

    @Test
    public void testReorderRight() {
        tabPane.getSelectionModel().select(0);
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.RIGHT);
        dragDistance = (int)(tabPaneHeight * DRAG_DISTANCE_PERCENTAGE);
        testReorder(tabPaneWidth - DX,
                    (int)(tabPaneHeight / TAB_COUNT * DRAG_TAB), false);
    }

    private void testReorder(int dX, int dY, boolean isDragInXDir) {
        // Press MouseButton.PRIMARY on a tab
        InvalidationListener selectionChangeListener = e -> {
            selectionLatch.countDown();
        };
        tabPane.getSelectionModel().selectedItemProperty().
                addListener(selectionChangeListener);
        selectionLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX),
                (int)(scene.getWindow().getY() + scene.getY() + dY));
            robot.mousePress(MouseButton.PRIMARY);
        });
        Util.waitForLatch(selectionLatch, 5, "Timeout waiting for the tab to get selected.");
        tabPane.getSelectionModel().selectedItemProperty().
                removeListener(selectionChangeListener);

        int direction = dragDistance / Math.abs(dragDistance);
        // Drag the tab by dragDistance
        for (int i = 0; i != dragDistance; i += direction) {
            moveMouse(dX, dY, isDragInXDir, i);
        }
        // Drag the tab back to it's original position
        for (int i = dragDistance; i != 0; i -= direction) {
            moveMouse(dX, dY, isDragInXDir, i);
        }

        // Drop the tab at original position
        Util.runAndWait(() -> {
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        // Select tab0 and navigate through tab1 to tab8 using right arrow key
        currentTabOrder = "";
        selectionChangeListener = e -> {
            currentTabOrder += tabPane.getSelectionModel().getSelectedItem().getText();
            selectionLatch.countDown();
        };
        tabPane.getSelectionModel().selectedItemProperty().
                addListener(selectionChangeListener);
        selectionLatch = new CountDownLatch(1);
        tabPane.getSelectionModel().select(0);
        Util.waitForLatch(selectionLatch, 5, "Timeout waiting for tab[0] to get selected.");

        for (int i = 1; i < TAB_COUNT; i++) {
            Util.runAndWait(() -> {
                selectionLatch = new CountDownLatch(1);
                robot.keyPress(KeyCode.RIGHT);
                robot.keyRelease(KeyCode.RIGHT);
            });
            Util.waitForLatch(selectionLatch, 5, "Timeout waiting for tab[" +
                         i + "] to get selected.");
        }
        tabPane.getSelectionModel().selectedItemProperty().
                removeListener(selectionChangeListener);

        Assert.assertFalse("Tabs should not be reordered.", isTabListReorderd);
        Assert.assertEquals(tabOrder, currentTabOrder);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            robot = new Robot();
            root = new HBox();
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(l -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    @Before
    public void setupTest() {
        CountDownLatch tabPaneLayoutLatch = new CountDownLatch(2);
        Util.runAndWait(() -> {
            tabOrder = "";
            tabPane = new TabPane();
            for (int i = 0 ; i < TAB_COUNT; ++i) {
                tabPane.getTabs().add(new Tab("tab" + i));
                tabOrder += "tab" + i;
            }
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
            tabPane.widthProperty().addListener(l -> {
                tabPaneWidth = (int)Math.floor(tabPane.getWidth());
                tabPaneLayoutLatch.countDown();
            });
            tabPane.heightProperty().addListener(l -> {
                tabPaneHeight = (int)Math.floor(tabPane.getHeight());
                tabPaneLayoutLatch.countDown();
            });

            tabPane.getTabs().addListener(reorderListener);
            root.getChildren().add(tabPane);
        });
        Util.waitForLatch(tabPaneLayoutLatch, 5, "Timeout waiting for TabPane layout.");
    }

    @After
    public void resetTest() {
        isTabListReorderd = false;
        Util.runAndWait(() -> {
            root.getChildren().clear();
            tabPane.getTabs().removeListener(reorderListener);
            tabPane.getTabs().clear();
            tabPane = null;
        });
    }

    private void setDragPolicyAndSide(TabPane.TabDragPolicy dragPolicy, Side side) {
        Util.runAndWait(() -> {
            tabPane.setTabDragPolicy(dragPolicy);
            tabPane.setSide(side);
        });
    }

    private static void moveMouse(int dX, int dY, boolean isDragInXDir, int d) {
        CountDownLatch moveLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (isDragInXDir) {
                // Top & Bottom
                robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX) + d,
                    (int)(scene.getWindow().getY() + scene.getY() + dY));
            } else {
                // Left & Right
                robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX),
                    (int)(scene.getWindow().getY() + scene.getY() + dY) + d);
            }
            moveLatch.countDown();
        });
        Util.waitForLatch(moveLatch, 5, "Timeout waiting for robot.mouseMove().");
    }
}
