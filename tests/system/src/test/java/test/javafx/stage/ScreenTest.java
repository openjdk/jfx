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

package test.javafx.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.stage.Screen;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class ScreenTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static ObservableList<Screen> screens;
    static volatile boolean screensListenerCalled = false;
    static volatile boolean screensSizeIsZero = false;

    private static void waitForLatch(CountDownLatch latch, int seconds, String msg) throws Exception {
        assertTrue("Timeout: " + msg, latch.await(seconds, TimeUnit.SECONDS));
    }

    /* This test for JDK-8252446 adds a listener on the ObservableList of
     * screens as the first thing in the platform startup runnable. Even
     * so, it cannot count on getting a call to the listener for the
     * initial list of screens. We don't get one on Windows or Linux. We
     * do get one on Mac, but this isn't guaranteed behavior, so this
     * test might or might not be effective.
     */
    @BeforeClass
    public static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Platform.startup(() -> {
            screens = Screen.getScreens();
            screens.addListener((Change<?> change) -> {
                final int size = screens.size();
                System.err.println("Screens list changed, size = " + size);
                if (size == 0) {
                    screensSizeIsZero = true;
                }
                screensListenerCalled = true;
            });
            Platform.runLater(startupLatch::countDown);
        });
        waitForLatch(startupLatch, 10, "FX runtime failed to start");
    }

    @AfterClass
    public static void exitFX() {
        Platform.exit();
    }

    @Test
    public void testScreensNotEmpty() {
        assertNotNull(screens);
        assertFalse("Screens list is empty", screens.size() == 0);
    }

    @Test
    public void testScreensNotEmptyInListener() {
        // Sleep for some time to see whether we get an initial call to our
        // listener. Since we cannot count on the listener being called at
        // all, we can't use a latch.
        Util.sleep(2000);

        // Skip the test if it isn't called.
        if (!screensListenerCalled) {
            System.err.println("Skipping test: Screens listener not called");
        }
        assumeTrue(screensListenerCalled);
        assertFalse("Screens list is empty in listener", screensSizeIsZero);
    }

}
