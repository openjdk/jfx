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

import javafx.concurrent.mocks.ProgressingTask;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A Task which always completes normally, but which also progresses from
 * 0 to 20 (inclusive). Initially the indeterminate state is used for
 * progress and maxProgress, but during execution the progress and
 * maxProgress are updated.
 */
public class TaskProgressTest {
    private Task task;

    @Before public void setup() {
        task = new ProgressingTask();
    }

    /************************************************************************
     * Run the concurrent and check that the final progress and maxProgress and
     * percentDone have correct values.
     ***********************************************************************/

    @Test public void afterRunningWorkDoneShouldBe_20() {
        task.run();
        assertEquals(20, task.getWorkDone(), 0);
    }

    @Test public void afterRunningTotalWorkShouldBe_20() {
        task.run();
        assertEquals(20, task.getTotalWork(), 0);
    }

    @Test public void afterRunningProgressShouldBe_1() {
        task.run();
        assertEquals(1, task.getProgress(), 0);
    }
}
