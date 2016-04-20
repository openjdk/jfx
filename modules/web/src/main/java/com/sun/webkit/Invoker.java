/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.perf.PerfLogger;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Invoker {
    private static Invoker instance;

    private static final PerfLogger locksLog = PerfLogger.getLogger("Locks");

    public static synchronized void setInvoker(Invoker invoker) {
        instance = invoker;
    }

    public static synchronized Invoker getInvoker() {
        return instance;
    }

    /*
     * Acquires the lock if it's not yet held by the current thread.
     *
     * @returns {@code true} if the lock has been acquired, otherwise {@code false}
     */
    protected boolean lock(ReentrantLock lock) {
        if (lock.getHoldCount() == 0) {

            lock.lock();
            locksLog.resumeCount(isEventThread() ? "EventThread" : "RenderThread");
            return true;
        }
        return false;
    }

    /*
     * Releases the lock if it's held by the current thread.
     *
     * @returns {@code true} if the lock has been released, otherwise {@code false}
     */
    protected boolean unlock(ReentrantLock lock) {
        if (lock.getHoldCount() != 0) {

            locksLog.suspendCount(isEventThread() ? "EventThread" : "RenderThread");
            lock.unlock();
            return true;
        }
        return false;
    }

    protected abstract boolean isEventThread();

    /**
     * Throws {@link IllegalStateException} if the current thread is not
     * the event thread.
     * @throws IllegalStateException if the current thread is not the event
     *         thread
     */
    public void checkEventThread() {
        if (!isEventThread()) {
            throw new IllegalStateException("Current thread is not event thread"
                    + ", current thread: " + Thread.currentThread().getName());
        }
    }

    public abstract void invokeOnEventThread(Runnable r);

    public abstract void postOnEventThread(Runnable r);
}
