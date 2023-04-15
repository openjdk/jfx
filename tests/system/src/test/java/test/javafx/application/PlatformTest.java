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

package test.javafx.application;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PlatformTest {

    @BeforeAll
    public static void initFXOnce() {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);

        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testNestedEventLoopChecks() {
        // Check on FX Thread.
        Util.runAndWait(() -> {
            assertFalse(Platform.isNestedLoopRunning());
            assertTrue(Platform.canStartNestedEventLoop());
        });
    }

    @Test
    public void testEnterExitNestedEventLoop() {
        final String key = "key";
        final String value = "value";
        final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();

        Util.runAndWait(() -> {
            // Exit nested event loop after it was started.
            Platform.runLater(() -> {
                try {
                    assertTrue(Platform.isNestedLoopRunning());
                    assertTrue(Platform.canStartNestedEventLoop());
                    Platform.exitNestedEventLoop(key, value);
                } catch (Throwable e) {
                    exceptionRef.set(e);
                }
            });

            Object returnValue = Platform.enterNestedEventLoop(key);
            assertEquals(value, returnValue);
        });

        // We do not expect any exception.
        assertNull(exceptionRef.get(), exceptionRef::toString);
    }

    @Test
    public void testNestedEventLoopChecksNotOnFxThread() {
        assertThrows(IllegalStateException.class, () -> Platform.isNestedLoopRunning());
        assertThrows(IllegalStateException.class, () -> Platform.canStartNestedEventLoop());
    }

    @Test
    public void testEnterExitNestedEventLoopNotOnFxThread() {
        assertThrows(IllegalStateException.class, () -> Platform.enterNestedEventLoop(null));
        assertThrows(IllegalStateException.class, () -> Platform.exitNestedEventLoop(null, null));
    }

    @Test
    public void testCanNotStartNestedEventLoopInTimeline() {
        final CountDownLatch timelineDone = new CountDownLatch(1);
        final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), event -> {
            try {
                assertFalse(Platform.canStartNestedEventLoop());
            } catch (Throwable e) {
                exceptionRef.set(e);
            } finally {
                timelineDone.countDown();
            }
        }));
        timeline.play();

        Util.await(timelineDone);

        // We do not expect any exception.
        assertNull(exceptionRef.get(), exceptionRef::toString);
    }

}
