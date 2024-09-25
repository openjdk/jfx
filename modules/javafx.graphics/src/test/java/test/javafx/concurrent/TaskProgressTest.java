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
import javafx.concurrent.TaskShim;
import test.javafx.concurrent.mocks.ProgressingTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Task which always completes normally, but which also progresses from
 * 0 to 20 (inclusive). Initially the indeterminate state is used for
 * progress and maxProgress, but during execution the progress and
 * maxProgress are updated.
 */
public class TaskProgressTest {
    private Task task;

    @BeforeEach
    public void setup() {
        task = new ProgressingTask();
    }

    /************************************************************************
     * Run the task and check that the final progress and maxProgress and
     * percentDone have correct values.
     ***********************************************************************/

    @Test
    public void afterRunningWorkDoneShouldBe_20() {
        task.run();
        assertEquals(20, task.getWorkDone(), 0);
    }

    @Test
    public void afterRunningTotalWorkShouldBe_20() {
        task.run();
        assertEquals(20, task.getTotalWork(), 0);
    }

    @Test
    public void afterRunningProgressShouldBe_1() {
        task.run();
        assertEquals(1, task.getProgress(), 0);
    }

    /************************************************************************
     * Test the updateProgress method on Task, that various inputs lead to
     * expected outputs according to the specification
     ***********************************************************************/

    @Test
    public void updateProgress_Long_0_100() {
        TaskShim.updateProgress(task, 0, 100);
        assertEquals(0, task.getProgress(), 0);
        assertEquals(0, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_n1_100() {
        TaskShim.updateProgress(task, -1, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_n10_100() {
        TaskShim.updateProgress(task, -10, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_MIN_VALUE_100() {
        TaskShim.updateProgress(task, Long.MIN_VALUE, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_10_100() {
        TaskShim.updateProgress(task, 10, 100);
        assertEquals(.1, task.getProgress(), 0);
        assertEquals(10, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_100_100() {
        TaskShim.updateProgress(task, 100, 100);
        assertEquals(1, task.getProgress(), 0);
        assertEquals(100, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_110_100() {
        TaskShim.updateProgress(task, 110, 100);
        assertEquals(1, task.getProgress(), 0);
        assertEquals(100, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_MAX_VALUE_100() {
        TaskShim.updateProgress(task, Long.MAX_VALUE, 100);
        assertEquals(1, task.getProgress(), 0);
        assertEquals(100, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_0_n1() {
        TaskShim.updateProgress(task, 0, -1);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_0_n10() {
        TaskShim.updateProgress(task, 0, -10);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_0_MIN_VALUE() {
        TaskShim.updateProgress(task, 0, Long.MIN_VALUE);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_100_10() {
        TaskShim.updateProgress(task, 100, 10);
        assertEquals(1, task.getProgress(), 0);
        assertEquals(10, task.getWorkDone(), 0);
        assertEquals(10, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Long_100_MAX_VALUE() {
        TaskShim.updateProgress(task, 100, Long.MAX_VALUE);
        assertEquals(100.0 / Long.MAX_VALUE, task.getProgress(), 0);
        assertEquals(100, task.getWorkDone(), 0);
        assertEquals(Long.MAX_VALUE, task.getTotalWork(), 0);
    }

    /* Now test the Double variants (Infinity, NaN)*/
    @Test
    public void updateProgress_Double_Infinity_100() {
        TaskShim.updateProgress(task, Double.POSITIVE_INFINITY, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NInfinity_100() {
        TaskShim.updateProgress(task, Double.NEGATIVE_INFINITY, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NaN_100() {
        TaskShim.updateProgress(task, Double.NaN, 100);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(100, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_0_Infinity() {
        TaskShim.updateProgress(task, 0, Double.POSITIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_0_NInfinity() {
        TaskShim.updateProgress(task, 0, Double.NEGATIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_0_NaN() {
        TaskShim.updateProgress(task, 0, Double.NaN);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_Infinity_Infinity() {
        TaskShim.updateProgress(task, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NInfinity_Infinity() {
        TaskShim.updateProgress(task, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_Infinity_NInfinity() {
        TaskShim.updateProgress(task, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NInfinity_NInfinity() {
        TaskShim.updateProgress(task, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_Infinity_NaN() {
        TaskShim.updateProgress(task, Double.POSITIVE_INFINITY, Double.NaN);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NInfinity_NaN() {
        TaskShim.updateProgress(task, Double.NEGATIVE_INFINITY, Double.NaN);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NaN_Infinity() {
        TaskShim.updateProgress(task, Double.NaN, Double.POSITIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NaN_NInfinity() {
        TaskShim.updateProgress(task, Double.NaN, Double.NEGATIVE_INFINITY);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }

    @Test
    public void updateProgress_Double_NaN_NaN() {
        TaskShim.updateProgress(task, Double.NaN, Double.NaN);
        assertEquals(-1, task.getProgress(), 0);
        assertEquals(-1, task.getWorkDone(), 0);
        assertEquals(-1, task.getTotalWork(), 0);
    }
}
