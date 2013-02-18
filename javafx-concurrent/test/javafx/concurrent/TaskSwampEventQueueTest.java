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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * In this test, the Task is going to attempt to totally swamp the
 * Event Queue by posting message updates very rapidly (that is,
 * the background threaded code isn't going to worry about it,
 * it is just going to send progress updates thousands of times
 * each second, and it is up to the Task implementation to
 * coalesce these down into a very sustainable number of entries
 * on the event queue. Basically, there should only ever be one
 * event on the event queue which gets the most recent value).
 */
public class TaskSwampEventQueueTest {
    private CyclicBarrier barrier;
    private List<Runnable> eventQueue;
    private Task task;
    private Thread th;

    @Before public void setup() {
        barrier = new CyclicBarrier(2);
        eventQueue = new ArrayList<>();
        task = new AbstractTask() {
            @Override protected String call() throws Exception {
                for (int i=0; i<1000; i++) {
                    updateProgress(i, 2000);
                }
                barrier.await(); // I will wait here until the test code is read
                barrier.await(); // I will wait here until the test code tells me to continue
                for (int i=1000; i<=2000; i++) {
                    updateProgress(i, 2000);
                }
                barrier.await(); // I'm done basically
                return "Sentinel";
            }

            @Override boolean isFxApplicationThread() {
                return Thread.currentThread() != th;
            }

            @Override void runLater(Runnable r) {
                eventQueue.add(r);
            }
        };
    }

    @Test public void numberOfEventsOnTheEventQueueShouldNeverBeLarge() throws Exception {
        th = new Thread(task);
        th.start();

        barrier.await();
        // There may actually 2 runnables on the queue, the first is the one that updates
        // the "state" of the Task, and the second is the progress update.
        assertTrue(eventQueue.size() == 2 || eventQueue.size() == 1);
        while (eventQueue.size() > 0) eventQueue.remove(0).run();
        assertEquals(1000 - 1, task.getWorkDone(), 0);
        assertEquals(2000, task.getTotalWork(), 0);
        barrier.await();
        barrier.await();
        // There may be another 2 runnables on the queue, the first is the progress update,
        // the second sets the value & updates the state of the Task.
        assertTrue(eventQueue.size() == 2 || eventQueue.size() == 1);
        while (eventQueue.size() > 0) eventQueue.remove(0).run();
        assertEquals(2000, task.getWorkDone(), 0);
        assertEquals(2000, task.getTotalWork(), 0);
    }
}
