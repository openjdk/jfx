/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.util.*;
import java.util.concurrent.*;

/**
 * A dispatcher for Application.invokeLater() that submits deferred runnables
 * to the native system one by one.
 *
 * A native system may by default execute all submitted runnables before
 * actually leaving a nested event loop. However, there is an assumption that a
 * runnable that calls the Application.leaveNestedEventLoop() method must be
 * the last runnable executed in the current inner event loop. The next
 * runnable that might have already been submitted by client code, must be
 * invoked in the context of the outer event loop. To satisfy this requirement
 * the runnables must be submitted to the native system one by one. This allows
 * for fine grain control over executing the deferred tasks when it comes to
 * entering/leaving nested event loops despite of native system limitations.
 */
public final class InvokeLaterDispatcher extends Thread {
    // The runnables queue
    private final BlockingDeque<Runnable> deque = new LinkedBlockingDeque<Runnable>();

    // Main lock
    private final Object LOCK = new StringBuilder("InvokeLaterLock");

    // Indicates if the application has entered a nested event loop
    private boolean nestedEventLoopEntered = false;

    // Indicates if the application is currently leaving a nested event loop
    private volatile boolean leavingNestedEventLoop = false;

    /**
     * An InvokeLaterDispatcher client implements this interface to allow
     * the dispatcher to submit runnables to the native system.
     */
    public static interface InvokeLaterSubmitter {
        /**
         * Submits the runnable to the native system for later execution and
         * returns immediately.
         */
        public void submitForLaterInvocation(Runnable r);
    }
    private final InvokeLaterSubmitter invokeLaterSubmitter;

    public InvokeLaterDispatcher(InvokeLaterSubmitter invokeLaterSubmitter) {
        super("InvokeLaterDispatcher");
        setDaemon(true);

        this.invokeLaterSubmitter = invokeLaterSubmitter;
    }

    private class Future implements Runnable {
        private boolean done = false;
        private final Runnable runnable;

        public Future(Runnable r) {
            this.runnable = r;
        }

        /**
         * Tells whether the runnbale has finished execution.
         *
         * This method must be called under the LOCK lock.
         */
        public boolean isDone() {
            return done;
        }

        @Override public void run() {
            try {
                this.runnable.run();
            } finally {
                synchronized (LOCK) {
                    this.done = true;
                    LOCK.notifyAll();
                }
            }
        }
    }

    @Override public void run() {
        try {
            while (true) {
                Runnable r = deque.takeFirst();

                if (leavingNestedEventLoop) {
                    // Defer invocation of the runnable till the current inner
                    // event loop returns from its enterNestedEventLoop()
                    deque.addFirst(r);
                    synchronized (LOCK) {
                        while (leavingNestedEventLoop) {
                            LOCK.wait();
                        }
                    }
                } else {
                    // Submit the runnable to the native system
                    final Future future = new Future(r);
                    invokeLaterSubmitter.submitForLaterInvocation(future);
                    synchronized (LOCK) {
                        try {
                            while (!future.isDone() && !nestedEventLoopEntered) {
                                LOCK.wait();
                            }
                            // Continue processing other runnables if we entered
                            // an inner event loop while excuting this runnable
                        } finally {
                            nestedEventLoopEntered = false;
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            // OK, let's stop this thread
        }
    }

    public void invokeAndWait(Runnable runnable) {
        final Future future = new Future(runnable);
        invokeLaterSubmitter.submitForLaterInvocation(future);
        synchronized (LOCK) {
            try {
                while (!future.isDone()) {
                    LOCK.wait();
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Schedules execution of a runnable on the event thread.
     *
     * @see Application#invokeLater(Runnable)
     */
    public void invokeLater(Runnable command) {
        deque.addLast(command);
    }

    /**
     * Notifies that a nested event loop is going to be entered.
     *
     * An InvokeLaterDispatcher client is responsible for calling this method
     * just before the enterNestedEventLoop() is dispatched to the native
     * system for actually starting the nested loop.
     */
    public void notifyEnteringNestedEventLoop() {
        synchronized (LOCK) {
            nestedEventLoopEntered = true;
            LOCK.notifyAll();
        }
    }

    /**
     * Notifies that the application is leaving a nested event loop.
     */
    public void notifyLeavingNestedEventLoop() {
        synchronized (LOCK) {
            leavingNestedEventLoop = true;
            LOCK.notifyAll();
        }
    }

    /**
     * Notifies that the application has left a nested event loop.
     */
    public void notifyLeftNestedEventLoop() {
        synchronized (LOCK) {
            leavingNestedEventLoop = false;
            LOCK.notifyAll();
        }
    }
}

