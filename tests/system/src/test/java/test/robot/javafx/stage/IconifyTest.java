/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static test.util.Util.TIMEOUT;

/**
 * Test ability to programmatically iconify UNDECORATED and TRANSPARENT stages
 */
public class IconifyTest extends VisualTestBase {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color TOP_COLOR = Color.RED;

    private static final double TOLERANCE = 0.07;

    private Stage bottomStage;
    private Stage topStage;

    public void canIconifyStage(StageStyle stageStyle, boolean resizable) throws Exception {
        final CountDownLatch shownLatch = new CountDownLatch(2);

        runAndWait(() -> {
            // Bottom stage, should be visible after top stage is iconified
            bottomStage = getStage(true);
            Scene bottomScene = new Scene(new Pane(), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
            bottomStage.setOnShown(e -> Platform.runLater(shownLatch::countDown));
            bottomStage.show();

            // Top stage, will be inconified
            topStage = getStage(true);
            topStage.initStyle(stageStyle);
            topStage.setResizable(resizable);
            Scene topScene = new Scene(new Pane(), WIDTH, HEIGHT);
            topScene.setFill(TOP_COLOR);
            topStage.setScene(topScene);
            topStage.setX(0);
            topStage.setY(0);
            topStage.setOnShown(e -> Platform.runLater(shownLatch::countDown));
            topStage.show();
        });

        assertTrue("Timeout waiting for stages to be shown",
            shownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        runAndWait(() -> {
            topStage.toFront();
        });

        sleep(500);
        runAndWait(() -> {
            assertFalse(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        runAndWait(() -> {
            topStage.setIconified(true);
        });

        sleep(500);
        runAndWait(() -> {
            assertTrue(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });

        runAndWait(() -> {
            topStage.setIconified(false);
        });

        sleep(500);
        runAndWait(() -> {
            assertFalse(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @Test(timeout = 15000)
    public void canIconifyDecoratedStage() throws Exception {
        canIconifyStage(StageStyle.DECORATED, true);
    }

    @Test(timeout = 15000)
    public void canIconifyUndecoratedStage() throws Exception {
        canIconifyStage(StageStyle.UNDECORATED, true);
    }

    @Test(timeout = 15000)
    public void canIconifyTransparentStage() throws Exception {
        canIconifyStage(StageStyle.TRANSPARENT, true);
    }

    @Test(timeout = 15000)
    public void canIconifyNonResizableStage() throws Exception {
        canIconifyStage(StageStyle.DECORATED, false);
    }

}
