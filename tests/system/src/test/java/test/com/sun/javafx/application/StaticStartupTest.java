/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Platform;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StaticStartupTest {

    @Test (timeout=15000)
    public void testStartupFromClinit() throws Exception {
        Thread thr = new Thread(() -> {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ex) {}
            System.err.println("Test timeout exceeded -- calling System.exit");
            System.exit(1);
        });
        thr.setDaemon(true);
        thr.start();
        StaticClass.doSomething();
    }

    @AfterClass
    public static void teardown() {
        Platform.exit();
    }
}

class StaticClass {

    static CountDownLatch staticLatch = new CountDownLatch(1);
    static Throwable err = null;

    static {
        Platform.startup(() -> {
            staticLatch.countDown();
        });
    }

    static void doSomething() {
        Platform.runLater(() -> {
            try {
                assertEquals(staticLatch.getCount(), 0);
            } catch (Throwable th) {
                throw new AssertionFailedError ("Static latch couldn't be read");
            }
        });
    }
}
