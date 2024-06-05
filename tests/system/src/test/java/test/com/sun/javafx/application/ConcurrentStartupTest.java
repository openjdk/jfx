/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.AfterClass;
import org.junit.Test;

public class ConcurrentStartupTest {
    CountDownLatch startupLatch;
    CountDownLatch mainLatch;
    volatile Throwable error = null;

    @Test (timeout=15000)
    public void testStartupReturnBeforeRunnableComplete() throws Exception {
        startupLatch = new CountDownLatch(2);
        mainLatch = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                if (!mainLatch.await(10, TimeUnit.SECONDS)) {
                    error = new AssertionError("Timeout waiting for main latch");
                }
                try {
                    assertEquals("Runnable executed out of order", 2, startupLatch.getCount());
                } catch (Throwable err) {
                    error = err;
                }
            } catch (InterruptedException ex) {
                error = ex;
            }
            startupLatch.countDown();
        });
        Platform.runLater(() -> {
            try {
                assertEquals("Runnable executed out of order", 1, startupLatch.getCount());
            } catch (Throwable err) {
                error = err;
            }
            startupLatch.countDown();
        });
        mainLatch.countDown();
        assertTrue(startupLatch.await(10, TimeUnit.SECONDS));
        if (error != null) {
            if (error instanceof Error) {
                throw (Error) error;
            } else {
                throw (Exception) error;
            }
        }
    }

    @AfterClass
    public static void teardown() {
        Platform.exit();
    }
}
