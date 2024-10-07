/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static test.util.Util.TIMEOUT;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.sun.javafx.PlatformUtil;
import test.robot.testharness.VisualTestBase;

/**
 * Test ability to programmatically iconify UNDECORATED and TRANSPARENT stages
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class IconifyTest extends VisualTestBase {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color TOP_COLOR = Color.RED;

    private static final double TOLERANCE = 0.07;

    private Stage bottomStage;
    private Stage topStage;

    public void canIconifyStage(StageStyle stageStyle, boolean resizable) throws Exception {
        final CountDownLatch bottomShownLatch = new CountDownLatch(1);
        final CountDownLatch topShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            // Bottom stage, should be visible after top stage is iconified
            bottomStage = getStage(false);
            Scene bottomScene = new Scene(new Pane(), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
            bottomStage.setOnShown(e -> Platform.runLater(bottomShownLatch::countDown));
            bottomStage.show();
        });

        Assertions.assertTrue(bottomShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for bottom stage to be shown");

        runAndWait(() -> {
            // Top stage, will be inconified
            topStage = getStage(true);
            topStage.initStyle(stageStyle);
            topStage.setResizable(resizable);
            Scene topScene = new Scene(new Pane(), WIDTH, HEIGHT);
            topScene.setFill(TOP_COLOR);
            topStage.setScene(topScene);
            topStage.setX(0);
            topStage.setY(0);
            topStage.setOnShown(e -> Platform.runLater(topShownLatch::countDown));
            topStage.show();
        });

        Assertions.assertTrue(topShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for top stage to be shown");

        sleep(1000);
        runAndWait(() -> {
            assertFalse(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        runAndWait(() -> {
            topStage.setIconified(true);
        });

        sleep(1000);
        runAndWait(() -> {
            assertTrue(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });

        runAndWait(() -> {
            topStage.setIconified(false);
        });

        sleep(1000);
        runAndWait(() -> {
            assertFalse(topStage.isIconified());
            Color color = getColor(100, 100);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @Test
    public void canIconifyDecoratedStage() throws Exception {
        assumeTrue(!PlatformUtil.isLinux()); // Skip due to JDK-8316891
        canIconifyStage(StageStyle.DECORATED, true);
    }

    @Test
    public void canIconifyUndecoratedStage() throws Exception {
        canIconifyStage(StageStyle.UNDECORATED, true);
    }

    @Test
    public void canIconifyTransparentStage() throws Exception {
        canIconifyStage(StageStyle.TRANSPARENT, true);
    }

    @Test
    public void canIconifyNonResizableStage() throws Exception {
        canIconifyStage(StageStyle.DECORATED, false);
    }
}
