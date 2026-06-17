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

import com.sun.glass.ui.Application;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadlessApplication1Test {

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @Test
    public void invokeAndWaitFromBackgroundThreadTest() {
        assertFalse(Platform.isFxApplicationThread());
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean fail = new AtomicBoolean();
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.startup(() -> {
            assertTrue(Platform.isFxApplicationThread());
            new Thread(() -> {
                assertFalse(Platform.isFxApplicationThread());
                Application.invokeAndWait(counter::incrementAndGet);
                assertEquals(1, counter.get());
                waitLatch.countDown();
            }).start();
        });
        try {
            if (!waitLatch.await(1, TimeUnit.SECONDS)) {
                fail.set(true);
            }
        } catch (InterruptedException e) {
            fail.set(true);
        }
        assertFalse(fail.get());
        assertEquals(0, waitLatch.getCount());
        assertFalse(Platform.isFxApplicationThread());
        Platform.exit();
    }

}
