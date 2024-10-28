/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.sun.javafx.PlatformUtil;
import test.robot.testharness.VisualTestBase;

/**
 * Basic visual tests using glass Robot to sample pixels.
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class StageRobotTest extends VisualTestBase {

    private Stage testStage1;
    private Scene testScene1;
    private Stage testStage2;
    private Scene testScene2;

    private static final double TOLERANCE = 0.07;

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    Rectangle2D screenBounds;

    @AfterEach
    public void cleanup() {
    }

    private void doTestOnTopCommon(boolean expectedOnTop) {
        // Skip on Linux due to 8145152
        assumeTrue(!PlatformUtil.isLinux());

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

    private void doTestFullScreenCommon(boolean initFullScreen,
                                        boolean expectedFullScreen)
    {
        // Readback of FullScreen window is not stable on Linux
        assumeTrue(!PlatformUtil.isLinux());

        runAndWait(() -> {
            screenBounds = Screen.getPrimary().getVisualBounds();
        });

        runAndWait(() -> {
            testStage1 = getStage(false);
            testStage1.initStyle(StageStyle.DECORATED);
            testScene1 = new Scene(new Group(), WIDTH, HEIGHT);
            testScene1.setFill(Color.LIME);
            testStage1.setScene(testScene1);
            if (initFullScreen) {
                testStage1.setFullScreen(true);
            }
            testStage1.setX((screenBounds.getWidth() - WIDTH) / 2);
            testStage1.setY((screenBounds.getHeight() - HEIGHT) / 2);
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

        // Give full-screen transition time to settle down
        sleep(1000);

        runAndWait(() -> {
            boolean propertyState = testStage1.fullScreenProperty().get();
            if (expectedFullScreen) {
                assertTrue(propertyState);
            } else {
                assertFalse(propertyState);
            }
            final int offset = 10;
            for (int row = 0; row < 2; row++) {
                int y = row == 0 ? (int)screenBounds.getMinY() + offset : (int)screenBounds.getMaxY() - offset - 1;

                for (int col = 0; col < 2; col++) {
                    int x = col == 0 ? (int)screenBounds.getMinX() + offset : (int)screenBounds.getMaxX() - offset - 1;
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

    @Test
    public void testOnTop() {
        doTestOnTopCommon(true);
    }

    @Test
    public void testFullScreenInit() {
        doTestFullScreenCommon(true, true);
    }

    @Test
    public void testFullScreenAfter() {
        doTestFullScreenCommon(false, true);
    }
}
