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

package test.com.sun.javafx.animation;

import java.util.concurrent.CountDownLatch;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnimationTimerTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static Stage primaryStage;

    public static class TestApp extends Application {

        @Override
        public void init() throws Exception {
            assertFalse(Platform.isFxApplicationThread());
        }

        @Override
        public void start(Stage stage) throws Exception {
            primaryStage = stage;
            assertTrue(Platform.isFxApplicationThread());

            startupLatch.countDown();
        }

    }

    @BeforeClass
    public static void setup() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void shutdown() {
        Util.shutdown(primaryStage);
    }

    @Test
    public void animationTimerOnFXThreadTest() throws InterruptedException {
        final CountDownLatch frameCounter = new CountDownLatch(3);
        Platform.runLater(() -> {
            AnimationTimer timer = new AnimationTimer() {
                @Override public void handle(long l) {
                    frameCounter.countDown();
                    if (frameCounter.getCount() == 0L) {
                        stop();
                    }
                }
            };
            assertTrue(Platform.isFxApplicationThread());
            timer.start();
        });
        frameCounter.await();
    }

    @Test
    public void startAnimationTimerNotOnFXThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long l) {}
        };
        assertThrows(IllegalStateException.class, timer::start);
    }

    @Test
    public void stopAnimationTimerNotOnFXThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long l) {
                assertThrows(IllegalStateException.class, () -> stop());
            }
        };
        Platform.runLater(timer::start);
    }
}
