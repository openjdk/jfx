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

package test.robot.javafx.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import test.util.Util;
import test.robot.testharness.VisualTestBase;

import static test.util.Util.TIMEOUT;

public class StageAttributesTest extends VisualTestBase {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color TOP_COLOR = Color.RED;

    private static final double TOLERANCE = 0.07;

    private Stage bottomStage;
    private Scene topScene;
    private Stage topStage;

    private void setupStages(boolean overlayed, boolean topShown) throws InterruptedException {
        final CountDownLatch bottomShownLatch = new CountDownLatch(1);
        final CountDownLatch topShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            // Bottom stage, should be visible after top stage is iconified
            bottomStage = getStage(false);
            bottomStage.initStyle(StageStyle.DECORATED);
            Scene bottomScene = new Scene(new Pane(), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
            bottomStage.setOnShown(e -> Platform.runLater(bottomShownLatch::countDown));
            bottomStage.show();
        });

        assertTrue("Timeout waiting for bottom stage to be shown",
            bottomShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        runAndWait(() -> {
            // Top stage, will be inconified
            topStage = getStage(true);
            topStage.initStyle(StageStyle.DECORATED);
            topScene = new Scene(new Pane(), WIDTH, HEIGHT);
            topScene.setFill(TOP_COLOR);
            topStage.setScene(topScene);
            if (overlayed) {
                topStage.setX(0);
                topStage.setY(0);
            } else {
                topStage.setX(WIDTH);
                topStage.setY(HEIGHT);
            }
            if (topShown) {
                topStage.setOnShown(e -> Platform.runLater(topShownLatch::countDown));
                topStage.show();
            }
        });

        if (topShown) {
            assertTrue("Timeout waiting for top stage to be shown",
                topShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        }

        sleep(1000);
    }

    @Test
    public void testIconifiedStage() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(true, true);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);

            topStage.setIconified(true);
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isIconified());
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void testMaximizedStage() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(false, true);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setMaximized(true);
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isMaximized());

            // maximized stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(100);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should show decorations (so not TOP_COLOR)
            Color color = getColor((int)topStage.getX() + 10, (int)topStage.getY() + 10);
            assertColorDoesNotEqual(TOP_COLOR, color, TOLERANCE);
            assertColorDoesNotEqual(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void testFullScreenStage() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(false, true);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setFullScreen(true);
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isFullScreen());

            // fullscreen stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(100);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should NOT show decorations
            Color color = getColor((int)topStage.getX() + 5, (int)topStage.getY() + 5);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void testIconifiedStageBeforeShow() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(true, false);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            // top stage was not shown yet in this case, but the bottom stage should be ready
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setIconified(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isIconified());

            // bottom stage should still be visible
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void testMaximizedStageBeforeShow() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        //  - JDK-8316425
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(false, false);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setMaximized(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isMaximized());

            // maximized stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(100);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should show decorations (so not TOP_COLOR)
            Color color = getColor((int)topStage.getX() + 10, (int)topStage.getY() + 10);
            assertColorDoesNotEqual(TOP_COLOR, color, TOLERANCE);
            assertColorDoesNotEqual(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void testFullScreenStageBeforeShow() throws InterruptedException {
        // Skip on Linux due to:
        //  - JDK-8316423
        //  - JDK-8316425
        assumeTrue(!PlatformUtil.isLinux());

        setupStages(false, false);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setFullScreen(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(1000);

        runAndWait(() -> {
            assertTrue(topStage.isFullScreen());

            // fullscreen stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(100);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should NOT show decorations
            Color color = getColor((int)topStage.getX() + 5, (int)topStage.getY() + 5);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }
}
