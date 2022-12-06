/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * Test program for nested event loop functionality.
 */
public class NestedEventLoopTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    private static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        private Stage primaryStage;

        @Override public void init() {
            NestedEventLoopTest.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(210);
            primaryStage.setHeight(180);

            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, MyApp.class);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    // Verify that we cannot enter a nested event loop on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testMustRunOnAppThread() {
        assertFalse(Platform.isFxApplicationThread());
        Platform.enterNestedEventLoop(new Object());
    }

    // Verify that we can enter and exit a nested event loop
    @Test public void testCanEnterAndExitNestedEventLoop() {
        final long key = 1024L;
        final long result = 2048L;
        final AtomicLong returnedValue = new AtomicLong();

        Util.runAndWait(
                () -> {
                    assertFalse(Platform.isNestedLoopRunning());
                    Long actual = (Long) Platform.enterNestedEventLoop(key);
                    returnedValue.set(actual);
                },
                () -> {
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key, result);
                },
                () -> {
                    assertFalse(Platform.isNestedLoopRunning());
                    assertEquals(result, returnedValue.get());
                }
        );
    }

    // Verify that we cannot enter a nested event loop with the same key twice
    @Test (expected=IllegalArgumentException.class)
    public void testUniqueKeyRequired() {
        final Object key = new Object();
        Util.runAndWait(
                () -> Platform.enterNestedEventLoop(key),
                () -> Platform.enterNestedEventLoop(key),
                () -> Platform.exitNestedEventLoop(key, null)
        );
    }

    // Verify that we cannot enter a nested event loop with a null key
    @Test (expected=NullPointerException.class)
    public void testNonNullKeyRequired() {
        Util.runAndWait(
                () -> Platform.enterNestedEventLoop(null)
        );
    }

    // Verify that we cannot exit a nested event loop with a null key
    @Test (expected=NullPointerException.class)
    public void testNonNullExitKeyRequired() {
        Util.runAndWait(
                () -> Platform.enterNestedEventLoop("validKey"),
                () -> Platform.exitNestedEventLoop(null, null),
                () -> Platform.exitNestedEventLoop("validKey", null)
        );
    }

    // Verify that we cannot exit a nested event loop with a key that has not been used
    @Test (expected=IllegalArgumentException.class)
    public void testExitLoopKeyHasBeenRegistered() {
        Util.runAndWait(
                () -> Platform.enterNestedEventLoop("validKey"),
                () -> Platform.exitNestedEventLoop("invalidKey", null),
                () -> Platform.exitNestedEventLoop("validKey", null)
        );
    }

    // Verify that we can enter and exit multiple nested event loops, in the order they are started
    @Test public void testCanEnterMultipleNestedLoops_andExitInOrder() {
        final long key1 = 1024L;
        final long key2 = 1025L;
        final long result1 = 2048L;
        final long result2 = 2049L;
        final AtomicLong returnedValue1 = new AtomicLong();
        final AtomicLong returnedValue2 = new AtomicLong();
        final AtomicBoolean loopOneRunning = new AtomicBoolean(false);
        final AtomicBoolean loopTwoRunning = new AtomicBoolean(false);

        Util.runAndWait(
                () -> {
                    // enter loop one
                    assertFalse(Platform.isNestedLoopRunning());
                    loopOneRunning.set(true);
                    Long actual = (Long) Platform.enterNestedEventLoop(key1);
                    loopOneRunning.set(false);
                    returnedValue1.set(actual);
                },
                () -> {
                    // enter loop two
                    assertTrue(Platform.isNestedLoopRunning());
                    loopTwoRunning.set(true);
                    Long actual = (Long) Platform.enterNestedEventLoop(key2);
                    loopTwoRunning.set(false);
                    returnedValue2.set(actual);
                },
                () -> {
                    // exit loop two
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key2, result2);
                },
                () -> {
                    // check loop two is done
                    assertTrue(Platform.isNestedLoopRunning());
                    assertTrue(loopOneRunning.get());
                    assertFalse(loopTwoRunning.get());
                    assertEquals(result2, returnedValue2.get());
                },
                () -> {
                    // exit loop one
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key1, result1);
                },
                () -> {
                    // check loop one is done
                    assertFalse(Platform.isNestedLoopRunning());
                    assertFalse(loopOneRunning.get());
                    assertFalse(loopTwoRunning.get());
                    assertEquals(result1, returnedValue1.get());
                }
        );
    }

    // We can only exit the inner-most event loop. If we try to exit an event loop that
    // is not the inner-most, the implementation is supposed to wait until it becomes
    // the inner-most loop.
    @Test public void testCanEnterMultipleNestedLoops_andExitOutOfOrder() {
        final long key1 = 1024L;
        final long key2 = 1025L;
        final long key3 = 1026L;
        final long result1 = 2048L;
        final long result2 = 2049L;
        final long result3 = 2050L;
        final AtomicLong returnedValue1 = new AtomicLong();
        final AtomicLong returnedValue2 = new AtomicLong();
        final AtomicLong returnedValue3 = new AtomicLong();
        final AtomicBoolean loopOneRunning = new AtomicBoolean(false);
        final AtomicBoolean loopTwoRunning = new AtomicBoolean(false);
        final AtomicBoolean loopThreeRunning = new AtomicBoolean(false);

        Util.runAndWait(
                () -> {
                    // enter loop one
                    assertFalse(Platform.isNestedLoopRunning());
                    loopOneRunning.set(true);
                    Long actual = (Long) Platform.enterNestedEventLoop(key1);
                    loopOneRunning.set(false);
                    returnedValue1.set(actual);
                },
                () -> {
                    // enter loop two
                    assertTrue(Platform.isNestedLoopRunning());
                    loopTwoRunning.set(true);
                    Long actual = (Long) Platform.enterNestedEventLoop(key2);
                    loopTwoRunning.set(false);
                    returnedValue2.set(actual);
                },
                () -> {
                    // enter loop three
                    assertTrue(Platform.isNestedLoopRunning());
                    loopThreeRunning.set(true);
                    Long actual = (Long) Platform.enterNestedEventLoop(key3);
                    loopThreeRunning.set(false);
                    returnedValue3.set(actual);
                },
                () -> {
                    // exit loop two - this should block until loop three is exited
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key2, result2);
                },
                () -> {
                    // check loop two is not done, so the returnedValue2 should still be zero.
                    assertTrue(Platform.isNestedLoopRunning());
                    assertTrue(loopOneRunning.get());
                    assertTrue(loopTwoRunning.get());
                    assertTrue(loopThreeRunning.get());
                    assertEquals(0, returnedValue2.get());
                },
                () -> {
                    // exit loop three - this will unblock loop two as well
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key3, result3);
                },
                () -> {
                    // check loop two and three are now both done,
                    // with loop one still running
                    assertTrue(Platform.isNestedLoopRunning());
                    assertTrue(loopOneRunning.get());
                },
                () -> {
                    // exit loop one
                    assertTrue(Platform.isNestedLoopRunning());
                    Platform.exitNestedEventLoop(key1, result1);
                }
        );
        Util.runAndWait(() -> {
            assertFalse(loopTwoRunning.get());
            assertFalse(loopThreeRunning.get());
            assertEquals(result2, returnedValue2.get());
            assertEquals(result3, returnedValue3.get());
        });
        // check loop one is done
        Util.runAndWait(() -> {
            assertFalse(Platform.isNestedLoopRunning());
            assertFalse(loopOneRunning.get());
            assertFalse(loopTwoRunning.get());
            assertFalse(loopThreeRunning.get());
            assertEquals(result1, returnedValue1.get());
        });
    }
}
