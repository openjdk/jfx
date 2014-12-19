/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import javafx.application.Platform;

import java.util.concurrent.Semaphore;

public abstract class TestRunnable implements Runnable {

    private Throwable t;
    private final Semaphore done = new Semaphore(1);

    public abstract void test() throws Exception;

    public final void run() {
        t = null;
        try {
            test();
        } catch (Throwable x) {
            t = x;
        }
        done.release();
    }

    public final void invokeAndWait() throws Exception {
        if (Platform.isFxApplicationThread()) {
            test();
        } else {
            done.acquire();
            Platform.runLater(this);
            done.acquire();
            done.release();
            rethrow(t);
        }
    }

    private void rethrow(Throwable t) throws Exception {
        if (t != null) {
            try {
                throw t;
            } catch (RuntimeException re) {
                throw re;
            } catch (Error e) {
                throw e;
            } catch (Throwable x) {
                throw (RuntimeException) new RuntimeException().initCause(x);
            }
        }
    }

    public final void invokeAndWaitUntilSuccess(long timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;
        boolean passed = false;
        do {
            try {
                invokeAndWait();
                passed = true;
            } catch (Throwable pendingThrowable) {
                Thread.sleep(100);
            }
        } while (System.currentTimeMillis() < endTime && !passed);
        rethrow(t);
    }

    public static void invokeAndWaitUntilSuccess(Testable t, long timeout) throws Exception {
        new TestRunnable() {
            public void test() throws Exception {
                t.test();
            }
        }.invokeAndWaitUntilSuccess(timeout);
    }

    public static void invokeAndWait(Testable t) throws Exception {
        new TestRunnable() {
            public void test() throws Exception {
                t.test();
            }
        }.invokeAndWait();
    }

    public static interface Testable {
        public void test() throws Exception;
    }

}
