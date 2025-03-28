/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.stage;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeToSceneTest {

    private static final int ROOT_SIZE = 360;
    private static final int BOUNDS_DELTA = 50;

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Stage mainStage;

    @BeforeAll
    static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterAll
    static void teardown() {
        Util.shutdown();
    }

    @AfterEach
    void afterEach() {
        Util.runAndWait(() -> {
            if (mainStage != null) {
                mainStage.hide();
            }
        });
    }

    private static void assertStageScreenBounds() {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double visualWidth = visualBounds.getWidth() - BOUNDS_DELTA;
        double visualHeight = visualBounds.getHeight() - BOUNDS_DELTA;

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        double width = bounds.getWidth() + BOUNDS_DELTA;
        double height = bounds.getHeight() + BOUNDS_DELTA;

        // There might be small inconsistencies because of decoration or different window managers.
        assertTrue(mainStage.getWidth() >= visualWidth, mainStage.getWidth() + " >= " + visualWidth);
        assertTrue(mainStage.getHeight() >= visualHeight, mainStage.getHeight() + " >= " + visualHeight);

        assertTrue(mainStage.getWidth() <= width, mainStage.getWidth() + " <= " + width);
        assertTrue(mainStage.getHeight() <= height, mainStage.getHeight() + " <= " + height);
    }

    private static void assertStageSceneBounds() {
        // There might be small inconsistencies because of decoration,
        // so we expect the size to be between (inclusive) 360 and 410.
        assertTrue(mainStage.getWidth() >= ROOT_SIZE, mainStage.getWidth() + " >= " + ROOT_SIZE);
        assertTrue(mainStage.getHeight() >= ROOT_SIZE, mainStage.getHeight() + " >= " + ROOT_SIZE);

        int maxThreshold = ROOT_SIZE + BOUNDS_DELTA;
        assertTrue(mainStage.getWidth() <= maxThreshold, mainStage.getWidth() + " <= " + maxThreshold);
        assertTrue(mainStage.getHeight() <= maxThreshold, mainStage.getHeight() + " <= " + maxThreshold);
    }

    private void createAndShowStage(Consumer<Stage> stageConsumer) {
        final CountDownLatch shownLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            mainStage = new Stage();

            Button root = new Button();
            root.setMinSize(ROOT_SIZE, ROOT_SIZE);
            mainStage.setScene(new Scene(root));

            stageConsumer.accept(mainStage);

            shownLatch.countDown();
        });

        Util.waitForLatch(shownLatch, 5, "Stage failed to setup and show");
        Util.sleep(500);
    }

    @Test
    void testInitialSizeOnMaximizedThenSizeToScene() {
        createAndShowStage(stage -> {
            stage.setMaximized(true);
            stage.sizeToScene();
            stage.show();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeOnFullscreenThenSizeToScene() {
        createAndShowStage(stage -> {
            stage.setFullScreen(true);
            stage.sizeToScene();
            stage.show();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeOnSizeToSceneThenMaximized() {
        createAndShowStage(stage -> {
            stage.sizeToScene();
            stage.setMaximized(true);
            stage.show();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeOnSizeToSceneThenFullscreen() {
        createAndShowStage(stage -> {
            stage.sizeToScene();
            stage.setFullScreen(true);
            stage.show();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeAfterShowSizeToSceneThenFullscreen() {
        createAndShowStage(stage -> {
            stage.show();

            stage.sizeToScene();
            stage.setFullScreen(true);
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeAfterShowSizeToSceneThenMaximized() {
        createAndShowStage(stage -> {
            stage.show();

            stage.sizeToScene();
            stage.setMaximized(true);
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeAfterShowFullscreenThenSizeToScene() {
        createAndShowStage(stage -> {
            stage.show();

            stage.setFullScreen(true);
            stage.sizeToScene();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeAfterShowMaximizedThenSizeToScene() {
        createAndShowStage(stage -> {
            stage.show();

            stage.setMaximized(true);
            stage.sizeToScene();
        });

        assertStageScreenBounds();
    }

    @Test
    void testInitialSizeOnSizeToScene() {
        createAndShowStage(stage -> {
            stage.sizeToScene();
            stage.show();
        });

        assertStageSceneBounds();
    }

    @Test
    void testInitialSizeFullscreenOnOffSizeToScene() {
        createAndShowStage(stage -> {
            stage.setWidth(100);
            stage.setHeight(100);

            stage.setFullScreen(true);
            stage.sizeToScene();
            stage.setFullScreen(false);

            stage.show();
        });

        assertStageSceneBounds();
    }

    @Test
    void testInitialSizeSizeToSceneFullscreenOnOff() {
        createAndShowStage(stage -> {
            stage.setWidth(100);
            stage.setHeight(100);

            stage.sizeToScene();
            stage.setFullScreen(true);
            stage.setFullScreen(false);

            stage.show();
        });

        assertStageSceneBounds();
    }

    @Test
    void testInitialSizeMaximizedOnOffSizeToScene() {
        createAndShowStage(stage -> {
            stage.setWidth(100);
            stage.setHeight(100);

            stage.setMaximized(true);
            stage.sizeToScene();
            stage.setMaximized(false);

            stage.show();
        });

        assertStageSceneBounds();
    }

    @Test
    void testInitialSizeSizeToSceneMaximizedOnOff() {
        createAndShowStage(stage -> {
            stage.setWidth(100);
            stage.setHeight(100);

            stage.sizeToScene();
            stage.setMaximized(true);
            stage.setMaximized(false);

            stage.show();
        });

        assertStageSceneBounds();
    }

}
