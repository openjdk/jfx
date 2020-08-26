/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.geometry.Side;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import test.util.Util;

/*
 * Unit test for verifying DragPolicies.
 *
 * There are 8 tests in this file.
 * Steps of 4 tests for DragPolicy.REORDER
 * 1. Create TabPane with 4 tabs.
 * 2. Drag tab0 to last after tab3.
 * 3. Verify that tab1 is the first tab after reorder.
 * 4. Verify that a correct permutation change event is received.
 * 5. Verify that getTabs() is also reordered correctly.
 * Repeat the test for four Sides.
 *
 * Steps of 4 tests for DragPolicy.FIXED
 * 1. Create TabPane with 4 tabs.
 * 2. Drag tab0 to last after tab3, the tab0 should not get dragged.
 * 3. Verify that tab0 is still the first tab.
 * 4. Verify that permutation change event is not received.
 * Repeat the test for four Sides.
 */
public class TabPaneDragPolicyTest {
    CountDownLatch[] latches;
    CountDownLatch changeListenerLatch;
    static CountDownLatch startupLatch;
    static Robot robot;
    static TabPane tabPane;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    final int DRAG_DISTANCE = SCENE_WIDTH - 50;
    final int DX = 15;
    final int DY = DX;
    Tab[] tabs;
    Tab expectedTab;
    Tab selectedTab;
    final String PERMUTED_SEQ = "tab1tab2tab3tab0";
    final String REORDER_SEQ = "tab1tab2tab3tab0";
    boolean listenerTestResult = false;
    ReorderChangeListener reorderListener = new ReorderChangeListener();
    FixedChangeListener fixedListener = new FixedChangeListener();

    class ReorderChangeListener implements ListChangeListener<Tab> {
        @Override
        public void onChanged(Change<? extends Tab> c) {
            while (c.next()) {
                if (c.wasPermutated()) {
                    String list = "";
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        list += tabPane.getTabs().get(i).getText();
                    }
                    listenerTestResult = list.equals(PERMUTED_SEQ);
                    list = "";
                    for (Tab t : tabPane.getTabs()) {
                        list += t.getText();
                    }
                    listenerTestResult = listenerTestResult && list.equals(REORDER_SEQ);
                    changeListenerLatch.countDown();
                }
            }
        };
    }

    class FixedChangeListener implements ListChangeListener<Tab> {
        @Override
        public void onChanged(Change<? extends Tab> c) {
            listenerTestResult = false;
        };
    }

    public static void main(String[] args) {
        initFX();
        TabPaneDragPolicyTest test = new TabPaneDragPolicyTest();

        test.testReorderTop();
        test.testReorderBottom();
        test.testReorderLeft();
        test.testReorderRight();

        test.testFixedTop();
        test.testFixedBottom();
        test.testFixedLeft();
        test.testFixedRight();

        exit();
    }

    @Test
    public void testReorderTop() {
        // Disable on Mac until JDK-8213136 is fixed
        assumeTrue(!PlatformUtil.isMac());
        expectedTab = tabs[1];
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.TOP);
        tabPane.getTabs().addListener(reorderListener);
        testReorder(DX, DY, 1, 0, false);
        tabPane.getTabs().removeListener(reorderListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to be "
            + "first tab after reordering.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Incorrect permutation change received", listenerTestResult);
    }

    @Test
    public void testReorderBottom() {
        // Disable on Mac until JDK-8213136 is fixed
        assumeTrue(!PlatformUtil.isMac());
        expectedTab = tabs[1];
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.BOTTOM);
        tabPane.getTabs().addListener(reorderListener);
        testReorder(DX, SCENE_HEIGHT - DY, 1, 0, false);
        tabPane.getTabs().removeListener(reorderListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to be "
            + "first tab after reordering.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Incorrect permutation change received", listenerTestResult);
    }

    @Test
    public void testReorderLeft() {
        // Disable on Mac until JDK-8213136 is fixed
        assumeTrue(!PlatformUtil.isMac());
        expectedTab = tabs[1];
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.LEFT);
        tabPane.getTabs().addListener(reorderListener);
        testReorder(DX, DY, 0, 1, false);
        tabPane.getTabs().removeListener(reorderListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to be "
            + "first tab after reordering.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Incorrect permutation change received", listenerTestResult);
    }

    @Test
    public void testReorderRight() {
        // Disable on Mac until JDK-8213136 is fixed
        assumeTrue(!PlatformUtil.isMac());
        expectedTab = tabs[1];
        setDragPolicyAndSide(TabPane.TabDragPolicy.REORDER, Side.RIGHT);
        tabPane.getTabs().addListener(reorderListener);
        testReorder(SCENE_WIDTH - DX, DY, 0, 1, false);
        tabPane.getTabs().removeListener(reorderListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to be "
            + "first tab after reordering.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Incorrect permutation change received", listenerTestResult);
    }

    @Test
    public void testFixedTop() {
        expectedTab = tabs[0];
        listenerTestResult = true;
        setDragPolicyAndSide(TabPane.TabDragPolicy.FIXED, Side.TOP);
        tabPane.getTabs().addListener(fixedListener);
        testReorder(DX, DY, 1, 0, true);
        tabPane.getTabs().removeListener(fixedListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to remain "
            + "first tab.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Change event should not be received", listenerTestResult);
    }

    @Test
    public void testFixedBottom() {
        expectedTab = tabs[0];
        listenerTestResult = true;
        setDragPolicyAndSide(TabPane.TabDragPolicy.FIXED, Side.BOTTOM);
        tabPane.getTabs().addListener(fixedListener);
        testReorder(DX, SCENE_HEIGHT - DY, 1, 0, true);
        tabPane.getTabs().removeListener(fixedListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to remain "
            + "first tab.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Change event should not be received", listenerTestResult);
    }

    @Test
    public void testFixedLeft() {
        expectedTab = tabs[0];
        listenerTestResult = true;
        setDragPolicyAndSide(TabPane.TabDragPolicy.FIXED, Side.LEFT);
        tabPane.getTabs().addListener(fixedListener);
        testReorder(DX, DY, 0, 1, true);
        tabPane.getTabs().removeListener(fixedListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to remain "
            + "first tab.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Change event should not be received", listenerTestResult);
    }

    @Test
    public void testFixedRight() {
        expectedTab = tabs[0];
        listenerTestResult = true;
        setDragPolicyAndSide(TabPane.TabDragPolicy.FIXED, Side.RIGHT);
        tabPane.getTabs().addListener(fixedListener);
        testReorder(SCENE_WIDTH - DX, DY, 0, 1, true);
        tabPane.getTabs().removeListener(fixedListener);
        selectedTab = (Tab)tabPane.getSelectionModel().getSelectedItem();
        Assert.assertEquals("Expected " + expectedTab.getText() + " to remain "
            + "first tab.", expectedTab.getText(), selectedTab.getText());
        Assert.assertTrue("Change event should not be received", listenerTestResult);
    }

    public void testReorder(int dX, int dY, int xIncr, int yIncr, boolean isFixed) {
        try {
            Thread.sleep(1000); // Wait for tabPane to layout
        } catch (Exception ex) {
            fail("Thread was interrupted." + ex);
        }
        Platform.runLater(() -> {
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX),
                (int)(scene.getWindow().getY() + scene.getY() + dY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        waitForLatch(latches[0], 5, "Timeout waiting tabs[0] to get selected.");

        CountDownLatch pressLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            robot.mousePress(MouseButton.PRIMARY);
            pressLatch.countDown();
        });
        waitForLatch(pressLatch, 5, "Timeout waiting for robot.mousePress(Robot.MOUSE_LEFT_BTN).");
        for (int i = 0; i < DRAG_DISTANCE; i++) {
            final int c = i;
            CountDownLatch moveLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                if (xIncr > 0) {
                    // Top & Bottom
                    robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX) + c,
                        (int)(scene.getWindow().getY() + scene.getY() + dY));
                } else {
                    // Left & Right
                    robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX),
                        (int)(scene.getWindow().getY() + scene.getY() + dY) + c);
                }
                moveLatch.countDown();
            });
            waitForLatch(moveLatch, 5, "Timeout waiting for robot.mouseMove(023).");
        }

        CountDownLatch releaseLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            robot.mouseRelease(MouseButton.PRIMARY);
            releaseLatch.countDown();
        });
        waitForLatch(releaseLatch, 5, "Timeout waiting for robot.mouseRelease(Robot.MOUSE_LEFT_BTN).");

        if (isFixed) {
            Util.runAndWait(() -> tabPane.getSelectionModel().select(tabs[2]));
            waitForLatch(latches[2], 5, "Timeout waiting tabs[2] to get selected.");
            latches[0] = new CountDownLatch(1);
        }

        Platform.runLater(() -> {
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + dX),
                (int)(scene.getWindow().getY() + scene.getY() + dY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        if (isFixed) {
            // For FIXED drag policy, tabs[0] should remain the first tab.
            try {
                Thread.sleep(500); // Wait for ChangeListener to get called.
            } catch (Exception ex) {
                fail("Thread was interrupted." + ex);
            }
            waitForLatch(latches[0], 5, "Timeout waiting tabs[0] to get selected.");
        } else {
            // For REORDER drag policy, tabs[1] should be the first tab.
            waitForLatch(changeListenerLatch, 5, "Timeout waiting ChangeListener to get called.");
            waitForLatch(latches[1], 5, "Timeout waiting tabs[1] to get selected.");
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            scene = new Scene(tabPane, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        waitForLatch(startupLatch, 10, "Timeout waiting for FX runtime to start");
    }

    @AfterClass
    public static void exit() {
        Platform.runLater(() -> {
            stage.hide();
        });
        Platform.exit();
    }

    @Before
    public void setupTest() {
        changeListenerLatch = new CountDownLatch(1);
        latches = new CountDownLatch[4];
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            tabs = new Tab[4];
            for (int i = 0 ; i < 4; ++i) {
                tabs[i] = new Tab("tab" + i);
            }
            tabPane.getTabs().addAll(tabs);
            tabPane.getSelectionModel().select(tabs[2]);
            for (int i = 0 ; i < 4; ++i) {
                final int c = i;
                latches[i] = new CountDownLatch(1);
                tabs[i].setOnSelectionChanged(event -> {
                    latches[c].countDown();
                });
            }
            latch.countDown();
        });
        waitForLatch(latch, 5, "Timeout waiting for setupTest().");
    }

    @After
    public void resetTest() {
        expectedTab = null;
        selectedTab = null;
        listenerTestResult = false;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            for (int i = 0 ; i < 4; ++i) {
                tabs[i].setOnSelectionChanged(null);
            }
            tabPane.getTabs().removeAll(tabs);
            tabs = null;
            latch.countDown();
        });
        waitForLatch(latch, 5, "Timeout waiting for resetTest().");
    }

    public static void waitForLatch(CountDownLatch latch, int seconds, String msg) {
        try {
            if (!latch.await(seconds, TimeUnit.SECONDS)) {
                fail(msg);
            }
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    public void setDragPolicyAndSide(TabPane.TabDragPolicy dragPolicy, Side side) {
        Util.runAndWait(() -> {
            tabPane.setTabDragPolicy(dragPolicy);
            tabPane.setSide(side);
        });
    }
}
