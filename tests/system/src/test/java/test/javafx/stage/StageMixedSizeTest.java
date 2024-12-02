/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

public class StageMixedSizeTest {
    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private Stage mainStage;

    @BeforeAll
    static void initFX() {
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

    @Test
    public void testSetWidthOnlyAfterShownOnContentSizeWindow() throws Exception {
        int contentSize = 300;
        int windowWidth = 200;

        createStage((s, sp) -> {
            s.setTitle("Width only after content size window");
            sp.setPrefWidth(contentSize);
            sp.setPrefHeight(contentSize);
        }, (s, sp) -> {
            s.setWidth(windowWidth);
        });

        Assertions.assertEquals(windowWidth, mainStage.getWidth(), "Window width should be " + windowWidth);
    }

    private void createStage(BiConsumer<Stage, StackPane> beforeShown,
                             BiConsumer<Stage, StackPane> afterShown) throws InterruptedException {
        CountDownLatch showLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            mainStage = new Stage();

            var sp = new StackPane();
            sp.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
            if (beforeShown != null) {
                beforeShown.accept(mainStage, sp);
            }

            mainStage.setScene(new Scene(sp));

            mainStage.setOnShown(e -> {
                Timeline timeline = new Timeline();

                long timeLine = 500;
                if (afterShown != null) {
                    timeline.getKeyFrames()
                            .add(new KeyFrame(Duration.millis(timeLine),
                                    ae -> afterShown.accept(mainStage, sp)));
                    timeLine += 500;
                }

                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(timeLine),
                                ae -> showLatch.countDown()));
                timeline.setCycleCount(1);
                timeline.play();
            });

            mainStage.show();
        });

        Util.waitForLatch(showLatch, 5, "Stage failed to setup and show");
        Util.sleep(500);
    }
}
