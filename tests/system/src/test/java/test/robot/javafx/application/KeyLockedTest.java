/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.robot.Robot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import test.util.Util;

/**
 * Test program for Platform::isKeyLocked.
 */
public class KeyLockedTest {

    // Used to start the toolkit before running any test
    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Robot robot;

    @BeforeAll
    public static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);

        if (PlatformUtil.isWindows()) {
            Util.runAndWait(() -> robot = new Robot());
        }
    }

    @AfterAll
    public static void cleanupFX() {
        if (robot != null) {
            // Disable caps lock if it is set
            Platform.runLater(() -> {
                Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.CAPS);
                capsLockState.ifPresent(state -> {
                    if (state) {
                        robot.keyPress(KeyCode.CAPS);
                        robot.keyRelease(KeyCode.CAPS);
                    }
                });
            });
        }
        Util.shutdown();
    }

    @Test
    public void testCallOnTestThread() {
        assertThrows(IllegalStateException.class, () -> {
            // This should throw an exception
            Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.CAPS);
        });
    }

    @Test
    public void testIllegalKeyCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            Util.runAndWait(() -> {
                // This should throw an exception
                Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.A);
            });
        });
    }

    @Test
    public void testCanReadCapsLockState() {
        Util.runAndWait(() -> {
            // Check that we don't get an exception or a null optional.
            Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.CAPS);
            assertNotNull(capsLockState);
            // A result should always be present
            assertTrue(capsLockState.isPresent());
        });
    }

    @Test
    public void testCanReadNumLockState() {
        Util.runAndWait(() -> {
            // Check that we don't get an exception or a null optional.
            Optional<Boolean> numLockState = Platform.isKeyLocked(KeyCode.NUM_LOCK);
            assertNotNull(numLockState);
            // A result should always be present on Windows and Linux
            if (PlatformUtil.isWindows() || PlatformUtil.isLinux()) {
                assertTrue(numLockState.isPresent());
            }
            // A result should never be present on Mac
            if (PlatformUtil.isMac()) {
                assertFalse(numLockState.isPresent());
            }
        });
    }

    @Test
    public void testCapsLockState() {
        // We can set caps lock via robot only on Windows
        assumeTrue(PlatformUtil.isWindows());

        final AtomicBoolean initialCapsLock = new AtomicBoolean(false);
        Util.runAndWait(() -> {
            Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.CAPS);
            assertNotNull(capsLockState);
            assertTrue(capsLockState.isPresent());

            // Read the initial state of the caps lock key and then toggle it
            initialCapsLock.set(capsLockState.get());
            robot.keyPress(KeyCode.CAPS);
            robot.keyRelease(KeyCode.CAPS);
        });
        // Wait for 1/2 second to make sure the state has toggled
        Util.sleep(500);
        Util.runAndWait(() -> {
            Optional<Boolean> capsLockState = Platform.isKeyLocked(KeyCode.CAPS);
            assertNotNull(capsLockState);
            assertTrue(capsLockState.isPresent());
            assertTrue(initialCapsLock.get() != capsLockState.get());
        });
    }
}
