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
package test.com.sun.glass.ui.headless;

import com.sun.javafx.application.PlatformImplShim;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadlessApplication2Test {

    private final CountDownLatch exitLatch = PlatformImplShim.test_getPlatformExitLatch();
    private Thread fxThread;

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @Test
    public void userThreadIsShutdownOnPlatformExitTest() {
        assertFalse(Platform.isFxApplicationThread());
        AtomicBoolean fail = new AtomicBoolean();
        Platform.startup(() -> {
            assertTrue(Platform.isFxApplicationThread());
            fxThread = Thread.currentThread();
            assertEquals(1, exitLatch.getCount());
            Platform.runLater(Platform::exit);
        });
        try {
            if (!exitLatch.await(1, TimeUnit.SECONDS)) {
                fail.set(true);
            }
        } catch (InterruptedException e) {
            fail.set(true);
        }

        assertEquals(0, exitLatch.getCount());
        assertFalse(fail.get());

        assertNotNull(fxThread);
        try {
            fxThread.join(10);
        }  catch (InterruptedException e) {
            fail.set(true);
        }

        assertFalse(fail.get());
        assertFalse(Thread.getAllStackTraces().keySet().stream().anyMatch(t -> "JavaFX Application Thread".equals(t.getName())));
    }

}
