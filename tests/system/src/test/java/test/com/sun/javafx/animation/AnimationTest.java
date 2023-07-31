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

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnimationTest {

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
    public void animationOnFXThreadTest() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertTrue(Platform.isFxApplicationThread());
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.play();
            pause.pause();
            pause.stop();
            l.countDown();
        });
        l.await();
    }

    @Test
    public void startAnimationNotOnFXThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        assertThrows(IllegalStateException.class, pause::play);
    }

    @Test
    public void pauseAnimationNotOnFXThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        Platform.runLater(pause::play);
        assertThrows(IllegalStateException.class, pause::pause);
    }

    @Test
    public void stopAnimationNotOnFXThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        Platform.runLater(() -> {
            pause.play();
            pause.pause();
        });
        assertThrows(IllegalStateException.class, pause::stop);
    }
}
