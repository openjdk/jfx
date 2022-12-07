/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SceneChangeShouldNotFocusStageTest {
    static Stage stage;
    static CountDownLatch startupLatch = new CountDownLatch(1);

    @Test
    void windowShouldRemainIconified() {
        Util.sleep(2000);
        assertTrue(stage.isIconified(), "Stage should be iconified");
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            Scene scene1 = new Scene(new Pane(new TextField("This is scene1")), 200, 200);
            Scene scene2 = new Scene(new Pane(new TextField("This is scene2")), 200, 200);

            Timeline tl = new Timeline();
            tl.setCycleCount(Animation.INDEFINITE);
            tl.getKeyFrames().addAll(new KeyFrame(Duration.millis(200), e -> stage.setScene(scene1)),
                    new KeyFrame(Duration.millis(400), e -> stage.setScene(scene2)));

            stage.setOnShown(e -> {
                tl.play();
                startupLatch.countDown();
            });

            stage.setIconified(true);
            stage.show();
        }
    }

    public static void waitForLatch(CountDownLatch latch, int seconds, String msg) throws Exception {
        assertTrue(latch.await(seconds, TimeUnit.SECONDS), "Timeout: " + msg);
    }
}
