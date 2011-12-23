/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.concurrent;

import java.util.concurrent.Semaphore;

/**
 * For testing purposes, we use this subclass of Task that will fake out the
 * runLater and isFxApplicationThread calls, such that we can actually run
 * and test Task in a single-threaded manner.
 * <p>
 * In addition, for many tests I need to be able to put the task into a specific
 * state and then check properties. For example, put the task into the
 * scheduled state, then check whether listeners have fired. Or put the task
 * into the cancelled state after it had been in the running state, and then
 * check some properties.
 * <p>
 * Because we will actually run the task in some background thread some times,
 * but will run the task sequentially in other tests, we need to have a
 * mechanism where either can happen. What we will do is use a semaphore for
 * each state. As the state is entered, we will give back a permit for that
 * state. Any code wishing to pick up at the right point can then just acquire
 * the permit to wait for the state to occur and then join.
 */
public abstract class AbstractTask extends Task<String> {
    public final Semaphore scheduledSemaphore = new Semaphore(0);
    public final Semaphore runningSemaphore = new Semaphore(0);
    public final Semaphore succeededSemaphore = new Semaphore(0);
    public final Semaphore cancelledSemaphore = new Semaphore(0);
    public final Semaphore failedSemaphore = new Semaphore(0);

    ServiceTestBase test;
    
    // Simulates scheduling the concurrent for execution
    public void simulateSchedule() {
        setState(State.SCHEDULED);
    }

    // For most tests, we want to pretend that we are on the FX app thread, always.
    @Override boolean isFxApplicationThread() {
        return true;
    }

    // For most tests, we want to just run this stuff immediately
    @Override void runLater(Runnable r) {
        if (test != null) {
            test.eventQueue.add(r);
        } else {
            r.run();
        }
    }

    @Override protected void scheduled() {
        scheduledSemaphore.release();
    }

    @Override protected void running() {
        runningSemaphore.release();
    }

    @Override protected void succeeded() {
        succeededSemaphore.release();
    }

    @Override protected void cancelled() {
        cancelledSemaphore.release();
    }

    @Override protected void failed() {
        failedSemaphore.release();
    }
}
