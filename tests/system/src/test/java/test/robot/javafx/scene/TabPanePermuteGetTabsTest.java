/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;

import test.util.Util;

/*
 * Test for verifying that the tab headers are also permuted when the
 * tabs which are already added to TabPane are removed and added back,
 * using TabPane.getTabs().setAll(). See JDK-8222457 for details.
 *
 * Steps:
 * a. testPermuteGetTabsWithSameTabs()
 *    1. Add tabs 0,1,2,3,4
 *    2. Permute the tabs to 4,3,2,1,0 using TabPane.getTabs().setAll().
 *    3. Verify that,
 *       3.1 tab[0] remains selected tab.
 *       3.2 tab[4] is the first tab in tab header.
 *       3.3 Pressing RIGHT key should select tabs in order: tab 3,2,1,0
 *
 * b. testPermuteGetTabsWithMoreTabs()
 *    1. Add tabs 0,1
 *    2. Permute tabs to tab 1,4,3,2,0 using TabPane.getTabs().setAll().
 *    3. Verify that,
 *       3.1 tab[0] should remain selected tab.
 *       3.2 tab[1] is the first tab in tab header.
 *       3.3 Pressing RIGHT key should select tabs in order: tab 4,3,2,0
 *
 * b1. testPermuteGetTabsWithMoreTabs1()
 *    1. Add tabs 0,1
 *    2. Permute tabs to tab 0,1,2,3 using TabPane.getTabs().setAll().
 *    3. Verify that,
 *       3.1 tab[1] should remain selected tab.
 *       3.2 tab[0] is the first tab in tab header.
 *       3.3 Pressing RIGHT key should select tabs in order: tab 1,2,3
 *
 * c. testPermuteGetTabsWithLessTabs()
 *    1. Add tab 3,1 and some(6) more tabs, and select tab 1.
 *    2. Permute tabs to, tab 1,4,3,2 using TabPane.getTabs().setAll().
 *    3. Verify that,
 *       3.1 tab[1] should remain selected tab.
 *       3.2 Pressing RIGHT key should select tabs in order: tab 4,3,2
 */

public class TabPanePermuteGetTabsTest {

    static CountDownLatch startupLatch;
    static Robot robot;
    static TabPane tabPane;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int firstTabdXY = 15;
    static final int NUM_TABS = 5;
    Tab[] tab;
    CountDownLatch[] selectionLatch;

    @Test
    public void testPermuteGetTabsWithSameTabs() {
        // Step #1
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[0], tab[1], tab[2], tab[3], tab[4]);
        });
        delay();
        Assert.assertSame("tab[0], the first tab should be the selected tab.",
            tab[0], tabPane.getSelectionModel().getSelectedItem());

        // Step #2
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[4], tab[3], tab[2], tab[1], tab[0]);
        });
        delay();

        // Step #3.1
        Assert.assertSame("tab[0], The selected tab should remain same after permuting.",
            tab[0], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.2
        // Click on first tab header
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + firstTabdXY),
                    (int) (scene.getWindow().getY() + scene.getY() + firstTabdXY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        waitForLatch(selectionLatch[4], 5, "Timeout: Waiting for tab[4] to get selected.");
        Assert.assertSame("tab[4] should be the first tab after permuting.",
            tab[4], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.3
        for (int i = 3; i >= 0; i--) {
            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.RIGHT);
                robot.keyRelease(KeyCode.RIGHT);
            });
            waitForLatch(selectionLatch[i], 5,
                "Timeout: Waiting for tab[" + i + "] to get selected.");
            Assert.assertSame("tab[" + i + "] should get selected on RIGHT key press.",
                tab[i], tabPane.getSelectionModel().getSelectedItem());
        }
    }

    @Test
    public void testPermuteGetTabsWithMoreTabs() {
        // Step #1
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[0], tab[1]);
        });
        delay();
        Assert.assertSame("tab[0], the first tab should be the selected tab.",
            tab[0], tabPane.getSelectionModel().getSelectedItem());

        // Step #2
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[1], tab[4], tab[3], tab[2], tab[0]);
        });
        delay();

        // Step #3.1
        Assert.assertSame("tab[0], The selected tab should remain same after permuting.",
            tab[0], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.2
        // Click on first tab header
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + firstTabdXY),
                    (int) (scene.getWindow().getY() + scene.getY() + firstTabdXY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        waitForLatch(selectionLatch[1], 5, "Timeout: Waiting for tab[1] to get selected.");
        Assert.assertSame("tab[1] should be the first tab after permuting.",
            tab[1], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.3
        for (int i = 4; i >= 0; i--) {
            if (i == 1) continue;
            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.RIGHT);
                robot.keyRelease(KeyCode.RIGHT);
            });
            waitForLatch(selectionLatch[i], 5,
                "Timeout: Waiting for tab[" + i + "] to get selected.");
            Assert.assertSame("tab[" + i + "] should get selected on RIGHT key press.",
                tab[i], tabPane.getSelectionModel().getSelectedItem());
        }
    }

    // Test for JDK-8237602
    @Test
    public void testPermutGetTabsWithMoreTabs1() {
        // Step #1
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[0], tab[1]);
            tabPane.getSelectionModel().select(tab[1]);
        });
        delay();

        Assert.assertSame("Sanity: tab[1] should be the selected tab.",
            tab[1], tabPane.getSelectionModel().getSelectedItem());

        // Step #2
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[0], tab[1], tab[2], tab[3]);
        });
        delay();

        // Step #3.1
        Assert.assertSame("Sanity: tab[1] should remain selected tab after permuting.",
            tab[1], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.2
        // Click on first tab header
        selectionLatch[0] = new CountDownLatch(1);
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + firstTabdXY),
                    (int) (scene.getWindow().getY() + scene.getY() + firstTabdXY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        delay();

        waitForLatch(selectionLatch[0], 5,
            "Timeout: Waiting for tab[" + 0 + "] to get selected.");
        Assert.assertSame("tab[0] should be first tab after permuting.",
            tab[0], tabPane.getSelectionModel().getSelectedItem());

        // step #3.3
        selectionLatch[1] = new CountDownLatch(1);
        for (int i = 1; i <= 3; i++) {
            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.RIGHT);
                robot.keyRelease(KeyCode.RIGHT);
            });
            waitForLatch(selectionLatch[i], 5,
                "Timeout: Waiting for tab[" + i + "] to get selected.");
            Assert.assertSame("tab[" + i + "] should get selected on RIGHT key press.",
                tab[i], tabPane.getSelectionModel().getSelectedItem());
        }
    }

    @Test
    public void testPermutGetTabsWithLessTabs() {
        // Step #1
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[3], tab[1], new Tab("t1"), new Tab("t2"),
                new Tab("t3"), new Tab("t4"), new Tab("t5"), new Tab("t6"));
            tabPane.getSelectionModel().select(tab[1]);
        });
        delay();

        Assert.assertSame("tab[1] should be the selected tab.",
            tab[1], tabPane.getSelectionModel().getSelectedItem());

        // Step #2
        Util.runAndWait(() -> {
            tabPane.getTabs().setAll(tab[1], tab[4], tab[3], tab[2]);
        });
        delay();

        // Step #3.1
        Assert.assertSame("tab[1] should remain selected tab after permuting.",
            tab[1], tabPane.getSelectionModel().getSelectedItem());

        // Step #3.2
        // Click on first tab header
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + firstTabdXY),
                    (int) (scene.getWindow().getY() + scene.getY() + firstTabdXY));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        for (int i = 4; i >= 2; i--) {
            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.RIGHT);
                robot.keyRelease(KeyCode.RIGHT);
            });
            waitForLatch(selectionLatch[i], 5,
                "Timeout: Waiting for tab[" + i + "] to get selected.");
            Assert.assertSame("tab[" + i + "] should get selected on RIGHT key press.",
                tab[i], tabPane.getSelectionModel().getSelectedItem());
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            robot = new Robot();
            tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            scene = new Scene(tabPane, 300, 100);
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

    @Before
    public void setupTest() {
        Util.runAndWait(() -> {
            tab = new Tab[NUM_TABS];
            selectionLatch = new CountDownLatch[NUM_TABS];
            for (int i = 0; i < NUM_TABS; i++) {
                final int c = i;
                tab[i] = new Tab("tab" + i);
                selectionLatch[i] = new CountDownLatch(1);
                tab[i].setOnSelectionChanged(event -> selectionLatch[c].countDown());
            }
        });
    }

    @AfterClass
    public static void exit() {
        Platform.runLater(() -> {
            stage.hide();
        });
        Platform.exit();
    }

    public static void delay() {
        try {
            Thread.sleep(500); // Wait for tabPane to layout
        } catch (Exception ex) {
            fail("Thread was interrupted." + ex);
        }
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
}
