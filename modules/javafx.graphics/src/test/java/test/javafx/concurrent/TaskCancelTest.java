/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.concurrent;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import test.javafx.concurrent.mocks.EpicFailTask;
import test.javafx.concurrent.mocks.InfiniteTask;
import test.javafx.concurrent.mocks.RunAwayTask;
import test.javafx.concurrent.mocks.SimpleTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what happens to a Task if it is canceled in each of the various
 * states that a Task may be in.
 */
public class TaskCancelTest {
    /**
     * Since the InfiniteTask never ends (normally), I can test it from the
     * ready, scheduled, running, cancelled, and failed states. I can't use
     * it for testing what happens when the task succeeds though.
     */
    private InfiniteTask task;

    @BeforeEach
    public void setup() {
        task = new InfiniteTask();
    }

    /**
     * Since the task begins in the ready state, I can just cancel it and
     * see what happens.
     */
    @Test
    public void cancellingA_READY_TaskShouldChangeStateTo_CANCELLED() {
        assertTrue(task.cancel());
        assertEquals(Worker.State.CANCELLED, task.getState());
        assertTrue(task.isDone());
    }

    /**
     * I have some cheap mechanism for simulating the scheduling of a Task
     * (it just changes the Task state correctly). So put it in the
     * Scheduled state and then cancel it and see what happens.
     */
    @Test
    public void cancellingA_SCHEDULED_TaskShouldChangeStateTo_CANCELLED() {
        task.simulateSchedule();
        assertTrue(task.cancel());
        assertEquals(Worker.State.CANCELLED, task.getState());
        assertTrue(task.isDone());
    }

    /**
     * Since the task is an infinitely running task, and since there is a
     * semaphore on the AbstractTask that tells me when certain state
     * transitions have occurred, I can simply fire up another thread and
     * run the infinite task, and then wait for that semaphore to trip. When
     * it does, I know that we're running (and will never leave that state)
     * so I can go ahead and then cancel it.
     *
     * @throws Exception shouldn't throw anything unless th.join fails
     */
    @Test
    public void cancellingA_RUNNING_TaskShouldChangeStateTo_CANCELLED() throws Exception {
        Thread th = new Thread(task);
        th.start();
        task.runningSemaphore.acquire();
        assertTrue(task.cancel());
        th.join();

        assertEquals(Worker.State.CANCELLED, task.getState());
        // TODO why is this commented out?
//        assertNull(task.getException());
        assertNull(task.getValue());
        assertTrue(task.isDone());
    }

    /**
     * In this case I don't want to use the infinite task, so I'll just
     * use a SimpleTask instead
     */
    @Test
    public void cancellingA_SUCCEEDED_TaskShouldNotChangeTo_CANCELLED() {
        Task t = new SimpleTask();
        t.run();
        assertFalse(t.cancel());
        assertEquals(Worker.State.SUCCEEDED, t.getState());
        assertTrue(t.isDone());
    }

    /**
     * Although I could end up using the infinite task for this one, I'm going
     * to go ahead and reuse the epic fail task instead
     */
    @Test
    public void cancellingA_FAILED_TaskShouldNotChangeTo_CANCELLED() {
        Task t = new EpicFailTask();
        t.run();
        assertFalse(t.cancel());
        assertEquals(Worker.State.FAILED, t.getState());
        assertTrue(t.isDone());
    }

    /**
     *
     */
    @Test
    public void aFreeRunningCancelledTaskReturnValueShouldBeIgnored() throws Exception {
        RunAwayTask runAway = new RunAwayTask() {
                @Override
                protected void loop(int count) throws Exception {
                }
        };
        Thread th = new Thread(runAway);
        th.start();
        runAway.runningSemaphore.acquire();
        assertTrue(runAway.cancel());
        runAway.stopLooping.set(true);
        th.join();

        assertEquals(Worker.State.CANCELLED, runAway.getState());
        // TODO why is this commented out?
//        assertNull(task.getException());
        assertNull(runAway.getValue());
        assertTrue(runAway.isDone());
    }
}
