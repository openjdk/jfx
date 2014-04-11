/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.mocks.SimpleTask;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A Task which simply completes normally. It never throws an Exception.
 * It also never increments the progress or touches messages or anything
 * else.
 */
public class TaskSimpleTest {
    private Task<String> task;

    @Before public void setup() {
        task = new SimpleTask();

        // Checks that the running property is always correct.
        task.runningProperty().addListener((o, oldValue, newValue) -> {
            Worker.State s = task.getState();
            if (newValue) {
                assertTrue(s == Worker.State.SCHEDULED || s == Worker.State.RUNNING);
            } else {
                assertTrue(s != Worker.State.SCHEDULED && s != Worker.State.RUNNING);
            }
        });
    }

    /************************************************************************
     * Test the initial values for the Task,
     ***********************************************************************/

    @Test public void stateShouldBe_READY_ByDefault() {
        assertEquals(Task.State.READY, task.getState());
    }

    @Test public void workDoneShouldBe_Indeterminate_ByDefault() {
        assertEquals(-1, task.getWorkDone(), 0);
    }

    @Test public void totalWorkShouldBe_Indeterminate_ByDefault() {
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test public void progressShouldBe_Indeterminate_ByDefault() {
        assertEquals(-1, task.getWorkDone(), 0);
    }

    @Test public void valueShouldBe_Null_ByDefault() {
        assertNull(task.getValue());
    }

    @Test public void exceptionShouldBe_Null_ByDefault() {
        assertNull(task.getException());
    }

    @Test public void runningShouldBe_False_ByDefault() {
        assertEquals(false, task.isRunning());
    }

    @Test public void messageShouldBe_EmptyString_ByDefault() {
        assertEquals("", task.getMessage());
    }

    @Test public void titleShouldBe_EmptyString_ByDefault() {
        assertEquals("", task.getTitle());
    }

    @Test public void isCancelledShouldBe_False_ByDefault() {
        assertEquals(false, task.isCancelled());
    }

    @Test public void isDoneShouldBe_False_ByDefault() {
        assertEquals(false, task.isDone());
    }

    /************************************************************************
     * Run the task and make sure that the states SCHEDULED, RUNNING, and
     * SUCCEEDED were all encountered in order. Check the condition at the
     * end of execution. Progress should equal maxProgress (-1 in this case),
     * percentDone should still be -1. value should be "Sentinel" and
     * exception should be null, running should be false.
     ***********************************************************************/

    @Test public void afterRunningStatesShouldHaveBeen_SCHEDULED_RUNNING_SUCCEEDED() {
        final List<Worker.State> states = new ArrayList<Worker.State>();
        task.stateProperty().addListener((observable, oldValue, newValue) -> {
            states.add(newValue);
        });

        task.run();

        assertArrayEquals(states.toArray(), new Worker.State[]{
                Worker.State.SCHEDULED,
                Worker.State.RUNNING,
                Worker.State.SUCCEEDED
        });
    }

    @Test public void afterRunningWorkDoneShouldBe_Indeterminate() {
        task.run();
        assertEquals(-1, task.getWorkDone(), 0);
    }

    @Test public void afterRunningTotalWorkShouldBe_Indeterminate() {
        task.run();
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test public void afterRunningProgressShouldBe_Indeterminate() {
        task.run();
        assertEquals(-1, task.getWorkDone(), 0);
    }

    @Test public void afterRunningValueShouldBe_Finished() {
        task.run();
        assertEquals("Sentinel", task.getValue());
    }

    @Test public void afterRunningExceptionShouldBe_Null() {
        task.run();
        assertNull(task.getException());
    }

    @Test public void afterRunningMessageShouldBe_EmptyString() {
        task.run();
        assertEquals("", task.getMessage());
    }

    @Test public void afterRunningTitleShouldBe_EmptyString() {
        task.run();
        assertEquals("", task.getTitle());
    }

    @Test public void afterRunning_isCancelled_ShouldBe_False() {
        task.run();
        assertEquals(false, task.isCancelled());
    }

    @Test public void afterRunning_isDone_ShouldBe_True() {
        task.run();
        assertEquals(true, task.isDone());
    }

    /************************************************************************
     * TODO Need to resolve what should happen when get() is called
     * The following few tests are setup so that we can test that
     * invoking "get" instead of "run" still results in the state
     * being set correctly for the Task. In theory, the exception,
     * value, and other fields should be updated when get is called (?)
     ***********************************************************************/

}
