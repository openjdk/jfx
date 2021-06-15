/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.helloworld;

import com.sun.glass.ui.Application;
import com.sun.javafx.PlatformUtil;
import java.awt.AWTPermission;
import java.security.AllPermission;
import java.security.Permission;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.FXPermission;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import test.robot.testharness.VisualTestBase;

/**
 * Basic visual tests using glass Robot to sample pixels.
 */
public class CustomSecurityManagerTest extends VisualTestBase {

    private Stage testStage1;
    private Scene testScene1;
    private Stage testStage2;
    private Scene testScene2;

    private static final double TOLERANCE = 0.07;

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    @SuppressWarnings("removal")
    static class MySecurityManager extends SecurityManager {
        private final boolean permissive;

        MySecurityManager(boolean permissive) {
            this.permissive = permissive;
        }

        @Override public void checkPermission(Permission perm) {
            // Grant access if we are permissive
            if (permissive) {
                return;
            }

            // Grant access unless one of the following is required
            if (!(perm instanceof AllPermission)
                    && !(perm instanceof FXPermission)
                    && !(perm instanceof AWTPermission)) {
                return;
            }

            super.checkPermission(perm);
        }

        @Override public void checkPermission(Permission perm, Object context) {
            assertNotNull(context);
            checkPermission(perm);
        }
    }

    @SuppressWarnings("removal")
    @After
    public void cleanup() {
        System.setSecurityManager(null);
    }

    @SuppressWarnings("removal")
    private void doTestOnTopCommon(SecurityManager sm, boolean expectedOnTop) {
        // Skip on Linux due to 8145152
        assumeTrue(!PlatformUtil.isLinux());

        System.setSecurityManager(sm);
        try {
            runAndWait(() -> {
                testStage1 = getStage(false);
                testScene1 = new Scene(new Group(), WIDTH, HEIGHT);
                testScene1.setFill(Color.RED);
                testStage1.setScene(testScene1);
                testStage1.setAlwaysOnTop(true);
                testStage1.show();
            });
            waitFirstFrame();
            runAndWait(() -> {
                testStage2 = getStage(false);
                testScene2 = new Scene(new Group(), WIDTH, HEIGHT);
                testScene2.setFill(Color.GREEN);
                testStage2.setScene(testScene2);
                testStage2.show();
                testStage2.toFront();
            });
            waitFirstFrame();
        } finally {
            System.setSecurityManager(null);
        }
        runAndWait(() -> {
            boolean propertyState = testStage1.alwaysOnTopProperty().get();
            if (expectedOnTop) {
                assertTrue(propertyState);
            } else {
                assertFalse(propertyState);
            }
            Color color = getColor(testScene1, WIDTH / 2, HEIGHT / 2);
            if (expectedOnTop) {
                assertColorEquals(Color.RED, color, TOLERANCE);
            } else {
                assertColorEquals(Color.GREEN, color, TOLERANCE);
            }
        });
    }

    @SuppressWarnings("removal")
    private void doTestFullScreenCommon(SecurityManager sm,
                                        boolean initFullScreen,
                                        boolean expectedFullScreen)
    {
        // Readback of FullScreen window is not stable on Linux
        assumeTrue(!PlatformUtil.isLinux());

        final AtomicInteger screenWidth = new AtomicInteger();
        final AtomicInteger screenHeight = new AtomicInteger();
        runAndWait(() -> {
            screenWidth.set((int)Screen.getPrimary().getBounds().getWidth());
            screenHeight.set((int)Screen.getPrimary().getBounds().getHeight());
        });

        System.setSecurityManager(sm);
        try {
            runAndWait(() -> {
                testStage1 = getStage(false);
                testStage1.initStyle(StageStyle.DECORATED);
                testScene1 = new Scene(new Group(), WIDTH, HEIGHT);
                testScene1.setFill(Color.LIME);
                testStage1.setScene(testScene1);
                if (initFullScreen) {
                    testStage1.setFullScreen(true);
                }
                testStage1.setX((screenWidth.get() - WIDTH) / 2);
                testStage1.setY((screenHeight.get() - HEIGHT) / 2);
                testStage1.show();
                testStage1.toFront();
            });
            waitFirstFrame();
            if (!initFullScreen) {
                runAndWait(() -> {
                    testStage1.setFullScreen(true);
                });
                waitFirstFrame();
            }
        } finally {
            System.setSecurityManager(null);
        }

        // Give full-screen transition time to settle down
        sleep(1000);

        runAndWait(() -> {
            boolean propertyState = testStage1.fullScreenProperty().get();
            if (expectedFullScreen) {
                assertTrue(propertyState);
            } else {
                assertFalse(propertyState);
            }
            for (int row = 0; row < 2; row++) {
                int y = row == 0 ? 1 : screenHeight.get() - 2;
                for (int col = 0; col < 2; col++) {
                    int x = col == 0 ? 1 : screenWidth.get() - 2;
                    Color color = getColor(x, y);
                    if (expectedFullScreen) {
                        assertColorEquals(Color.LIME, color, TOLERANCE);
                    } else {
                        assertColorDoesNotEqual(Color.LIME, color, TOLERANCE);
                    }
                }
            }
        });
    }

    @SuppressWarnings("removal")
    private void doTestRobotCommon(SecurityManager sm, boolean expectedCreateRobot) {
        final AtomicReference<Robot> robot = new AtomicReference<>();
        System.setSecurityManager(sm);
        try {
            runAndWait(() -> {
                try {
                    robot.set(new Robot());
                } catch (SecurityException ex) {
                    robot.set(null);
                }
            });
        } finally {
            System.setSecurityManager(null);
        }
        if (expectedCreateRobot) {
            assertNotNull(robot.get());
        } else {
            assertNull(robot.get());
        }
    }

    @Test(timeout = 15000)
    public void testOnTopNoSecurityManager() {
        doTestOnTopCommon(null, true);
    }

    @Test(timeout = 15000)
    public void testOnTopPermissiveSecurityManager() {
        doTestOnTopCommon(new MySecurityManager(true), true);
    }

    @Test(timeout = 15000)
    public void testOnTopRestrictiveSecurityManager() {
        doTestOnTopCommon(new MySecurityManager(false), false);
    }

    @Test(timeout = 15000)
    public void testFullScreenInitNoSecurityManager() {
        doTestFullScreenCommon(null, true, true);
    }

    @Test(timeout = 15000)
    public void testFullScreenInitPermissiveSecurityManager() {
        doTestFullScreenCommon(new MySecurityManager(true), true, true);
    }

    @Test(timeout = 15000)
    public void testFullScreenInitRestrictiveSecurityManager() {
        doTestFullScreenCommon(new MySecurityManager(false), true, false);
    }

    @Test(timeout = 15000)
    public void testFullScreenAfterNoSecurityManager() {
        doTestFullScreenCommon(null, false, true);
    }

    @Test(timeout = 15000)
    public void testFullScreenAfterPermissiveSecurityManager() {
        doTestFullScreenCommon(new MySecurityManager(true), false, true);
    }

    @Test(timeout = 15000)
    public void testFullScreenAfterRestrictiveSecurityManager() {
        doTestFullScreenCommon(new MySecurityManager(false), false, false);
    }

    @Test(timeout = 15000)
    public void testRobotNoSecurityManager() {
        doTestRobotCommon(null, true);
    }

    @Test(timeout = 15000)
    public void testRobotPermissiveSecurityManager() {
        doTestRobotCommon(new MySecurityManager(true), true);
    }

    @Test(timeout = 15000)
    public void testRobotRestrictiveSecurityManager() {
        doTestRobotCommon(new MySecurityManager(false), false);
    }

}
