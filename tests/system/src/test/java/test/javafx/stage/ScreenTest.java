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

package test.javafx.stage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.stage.Screen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

public class ScreenTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static ObservableList<Screen> screens;
    static volatile boolean screensListenerCalled = false;
    static volatile boolean screensSizeIsZero = false;

    /* This test for JDK-8252446 adds a listener on the ObservableList of
     * screens as the first thing in the platform startup runnable. Even
     * so, it cannot count on getting a call to the listener for the
     * initial list of screens. We don't get one on Windows or Linux. We
     * do get one on Mac, but this isn't guaranteed behavior, so this
     * test might or might not be effective.
     */
    @BeforeAll
    public static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, () -> {
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
    }

    @AfterAll
    public static void exitFX() {
        Util.shutdown();
    }

    @Test
    public void testScreensNotEmpty() {
        assertNotNull(screens);
        assertFalse(screens.size() == 0, "Screens list is empty");
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
        assertFalse(screensSizeIsZero, "Screens list is empty in listener");
    }
}
