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

package com.sun.webkit;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

final class WatchdogTimer {

    private static final Logger logger =
            Logger.getLogger(WatchdogTimer.class.getName());
    private static final ThreadFactory threadFactory =
            new CustomThreadFactory();


    // The executor is intentionally made a non-static/instance
    // field (as opposed to a static/class field) in order for
    // fwkDestroy() to be able to orderly shutdown the executor
    // and wait for the outstanding runnable, if any, to complete.
    // Currently, there is just a single instance of this class
    // per process, so having a non-static executor should not
    // be a problem.
    private final ScheduledThreadPoolExecutor executor;
    private final Runnable runnable;
    private ScheduledFuture<?> future;


    private WatchdogTimer(final long nativePointer) {
        executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        executor.setRemoveOnCancelPolicy(true);

        runnable = () -> {
            try {
                twkFire(nativePointer);
            } catch (Throwable th) {
                logger.log(WARNING, "Error firing watchdog timer", th);
            }
        };
    }


    private static WatchdogTimer fwkCreate(long nativePointer) {
        return new WatchdogTimer(nativePointer);
    }

    private void fwkStart(double limit) {
        if (future != null) {
            throw new IllegalStateException();
        }
        future = executor.schedule(runnable, (long) (limit * 1000) + 50,
                TimeUnit.MILLISECONDS);
    }

    private void fwkStop() {
        if (future == null) {
            throw new IllegalStateException();
        }
        future.cancel(false);
        future = null;
    }

    private void fwkDestroy() {
        executor.shutdownNow();
        while (true) {
            try {
                if (executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ex) {
                // continue waiting
            }
        }
    }

    private native void twkFire(long nativePointer);

    private static final class CustomThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger index = new AtomicInteger(1);

        private CustomThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    "Watchdog-Timer-" + index.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
