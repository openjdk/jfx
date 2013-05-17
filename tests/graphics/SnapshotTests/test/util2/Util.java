/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package util2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import junit.framework.AssertionFailedError;

/**
 * Utility methods for life-cycle testing
 */
public class Util {

    // Test timeout value in milliseconds
    public static final int TIMEOUT = 5000;

    private static interface Future {
        public abstract boolean await(long timeout, TimeUnit unit);
    }

    public static void throwError(Throwable testError) {
        if (testError != null) {
            if (testError instanceof Error) {
                throw (Error)testError;
            } else if (testError instanceof RuntimeException) {
                throw (RuntimeException)testError;
            } else {
                AssertionFailedError err = new AssertionFailedError("Unknown exception");
                err.initCause(testError.getCause());
                throw err;
            }
        } else {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            throw err;
        }
    }

    public static void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ex) {}
    }

    private static Future submit(final Runnable r) {
        final Throwable[] testError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    r.run();
                } catch (Throwable th) {
                    testError[0] = th;
                } finally {
                    latch.countDown();
                }
            }
        });

        Future future = new Future() {
            @Override public boolean await(long timeout, TimeUnit unit) {
                try {
                    if (!latch.await(timeout, unit)) {
                        return false;
                    }
                } catch (InterruptedException ex) {
                    AssertionFailedError err = new AssertionFailedError("Unexpected exception");
                    err.initCause(ex);
                    throw err;
                }

                if (testError[0] != null) {
                    if (testError[0] instanceof Error) {
                        throw (Error)testError[0];
                    } else if (testError[0] instanceof RuntimeException) {
                        throw (RuntimeException)testError[0];
                    } else {
                        AssertionFailedError err = new AssertionFailedError("Unknown execution exception");
                        err.initCause(testError[0].getCause());
                        throw err;
                    }
                }

                return true;
            }
        };

        return future;
    }

    public static void runAndWait(Runnable... runnables) {
        List<Future> futures = new ArrayList(runnables.length);
        int i = 0;
        for (Runnable r : runnables) {
            futures.add(submit(r));
        }

        int count = TIMEOUT / 100;
        while (!futures.isEmpty() && count-- > 0) {
            Iterator<Future> it = futures.iterator();
            while (it.hasNext()) {
                Future future = it.next();
                if (future.await(100, TimeUnit.MILLISECONDS)) {
                    it.remove();
                }
            }
        }

        if (!futures.isEmpty()) {
            throw new AssertionFailedError("Exceeded timeout limit of " + TIMEOUT + " msec");
        }
    }

}
