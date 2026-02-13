/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNull;

abstract class StageTestBase {
    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private Stage stage = null;

    protected static final int SHORT_WAIT = 300;
    protected static final int MEDIUM_WAIT = 500;
    protected static final int LONG_WAIT = 1000;
    protected static final double SIZING_DELTA = 1.0;
    protected static final double POSITION_DELTA = 1.0;

    /**
     * Creates a Scene for the test stage acoording to the {@link StageStyle}
     * @param stageStyle {@link StageStyle} of the Stage
     * @return a {@link Scene}
     */
    protected Scene createScene(StageStyle stageStyle) {
        if (stageStyle == StageStyle.TRANSPARENT) {
            Region root = createRoot();
            BackgroundFill fill = new BackgroundFill(
                    Color.HOTPINK.deriveColor(0, 1, 1, 0.5),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            );
            root.setBackground(new Background(fill));

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            return scene;
        }

        return new Scene(createRoot(), Color.HOTPINK);
    }

    /**
     * Gets the Scene root
     */
    protected Region createRoot() {
        return new StackPane();
    }

    /**
     * Utility method to setup test Stages according to {@link StageStyle}
     * @param stageStyle The Stage Style.
     * @param pc A consumer to set state properties
     */
    protected void setupStageWithStyle(StageStyle stageStyle, Consumer<Stage> pc) {
        CountDownLatch shownLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            assertNull(stage, "Stage is not null");
            stage = new Stage();
            stage.setAlwaysOnTop(true);
            stage.initStyle(stageStyle);
            stage.setScene(createScene(stageStyle));
            if (pc != null) {
                pc.accept(stage);
            }
            stage.setOnShown(e -> shownLatch.countDown());
            stage.show();
        });

        Util.waitForLatch(shownLatch, 5, "Stage failed to show");
    }

    @BeforeAll
    public static void initFX() {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    /**
     * Hides the test stage after each test
     */
    @AfterEach
    public void cleanup() {
        if (stage != null) {
            CountDownLatch hideLatch = new CountDownLatch(1);
            stage.setOnHidden(e -> hideLatch.countDown());
            Util.runAndWait(stage::hide);
            Util.waitForLatch(hideLatch, 5, "Stage failed to hide");
            stage = null;
        }
    }

    /**
     * @return The stage that is created for each test
     */
    protected Stage getStage() {
        return stage;
    }

    /**
     * Gets the Scene of the test stage.
     * @return The Scene of the test stage.
     */
    protected Scene getScene() {
        return stage.getScene();
    }
}
