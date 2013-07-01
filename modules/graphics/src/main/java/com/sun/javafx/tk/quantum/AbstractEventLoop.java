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

package com.sun.javafx.tk.quantum;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

abstract class AbstractEventLoop {

    static abstract class Task implements Runnable {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void run() {
            try {
                doRun();
            } finally {
                latch.countDown();
            }
        }

        protected abstract void doRun();

        void await() throws InterruptedException {
            latch.await();
        }
    };

    static final class CallableTask<V> extends Task {

        private V retValue;
        private final Callable<V> c;
        private Exception e;

        CallableTask(final Callable<V> c) {
            this.c = c;
        }

        @Override
        public void doRun() {
            try {
                retValue = c.call();
            } catch (Exception e) {
                this.e = e;
            }
        }

        V getResult() {
            if (e == null) {
                return retValue;
            }
            throw new RuntimeException(e);
        }
    }

    static final class RunnableTask extends Task {

        private final Runnable r;

        RunnableTask(final Runnable r) {
            this.r = r;
        }

        @Override
        public void doRun() {
            r.run();
        }
    }

    public AbstractEventLoop() {
    }

    public void send(final Runnable r) {
        final RunnableTask task = new RunnableTask(r);
        await(task);
    }

    public <V> V send(final Callable<V> c) {
        final CallableTask<V> task = new CallableTask<V>(c);
        await(task);
        return task.getResult();
    }

    protected abstract void schedule(Runnable r);

    public abstract void start();

    public abstract void stop();

    private void await(final Task task) {
        schedule(task);
        try {
            task.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
