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

import java.util.Arrays;
import java.util.Collection;

import javafx.concurrent.mocks.EpicFailTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TaskExceptionTest {
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {new Exception("Exception")},
                {new IllegalArgumentException("IAE")},
                {new NullPointerException("NPE")},
                {new RuntimeException("RuntimeException")}
        });
    }

    private Exception exception;
    private Task task;

    public TaskExceptionTest(Exception th) {
        this.exception = th;
    }

    @Before public void setup() {
        task = new EpicFailTask(exception);
    }

    /************************************************************************
     * Run the task and check that the exception property is set, and that
     * the value property is null. The progress fields may be in some
     * arbitrary state.
     ***********************************************************************/

    @Test public void afterRunningExceptionShouldBeSet() {
        task.run();
        assertNotNull(task.getException());
    }

    @Test public void afterRunningValueShouldBe_Null() {
        task.run();
        assertNull(task.getValue());
    }

    @Test public void afterRunningWorkDoneShouldBe_10() {
        task.run();
        assertEquals(10, task.getWorkDone(), 0);
    }

    @Test public void afterRunningTotalWorkShouldBe_20() {
        task.run();
        assertEquals(20, task.getTotalWork(), 0);
    }

    @Test public void afterRunningProgressShouldBe_FiftyPercent() {
        task.run();
        assertEquals(.5, task.getProgress(), 0);
    }

    @Test public void afterRunningStateShouldBe_FAILED() {
        task.run();
        assertEquals(Worker.State.FAILED, task.getState());
    }
}
