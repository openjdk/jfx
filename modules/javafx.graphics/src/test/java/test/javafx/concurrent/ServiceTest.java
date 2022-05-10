/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import test.javafx.concurrent.mocks.SimpleTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.concurrent.Service;
import javafx.concurrent.Service;
import javafx.concurrent.ServiceShim;
import javafx.concurrent.Task;
import javafx.concurrent.Task;
import javafx.concurrent.TaskShim;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 */
public class ServiceTest {
    private Service<String> service;

    @Before public void setup() {
        // I don't use the AbstractService here because I don't want to
        // take advantage of the built in executor / threading stuff
        service = new ServiceShim<String>() {
            @Override public Task<String> createTask() {
                return new SimpleTask();
            }

            @Override public void checkThread() { }
            @Override public void runLater(Runnable r) {
                r.run();
            }
        };
    }

    /******************************************************************
     * Executor Property                                              *
     *****************************************************************/

    /**
     * Tests that the executor property is null by default
     */
    @Test public void executorDefaultsToNull() {
        assertNull(service.getExecutor());
        assertNull(service.executorProperty().get());
    }

    /**
     * Tests that you can set the executor. This will set the executor
     * to some non-default setting and check that the same instance is
     * then set on the service
     */
    @Test public void executorCanBeSet() {
        final Executor e = command -> { };
        service.setExecutor(e);
        assertSame(e, service.getExecutor());
        assertSame(e, service.executorProperty().get());
    }

    /**
     * Tests that you can bind the executor property of a Service
     */
    @Test public void executorCanBeBound() {
        final Executor e = command -> { };
        ObjectProperty<Executor> other = new SimpleObjectProperty<Executor>(e);
        service.executorProperty().bind(other);
        assertSame(e, service.getExecutor());
        assertSame(e, service.executorProperty().get());
        other.set(null);
        assertNull(service.getExecutor());
        assertNull(service.executorProperty().get());
    }

    /**
     * Tests that if you specify a custom executor, then it is used when
     * you attempt to run a service
     */
    @Test public void executorIsUsed() {
        final AtomicBoolean results = new AtomicBoolean(false);
        final Executor e = command -> results.set(true);
        service.setExecutor(e);
        service.start();
        assertTrue(results.get());
    }

    /******************************************************************
     * Test initial values for properties                             *
     *****************************************************************/

    @Test public void stateDefaultsTo_READY() {
        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    @Test public void valueDefaultsToNull() {
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void exceptionDefaultsToNull() {
        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test public void workDoneDefaultsTo_NegativeOne() {
        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test public void totalWorkDefaultsTo_NegativeOne() {
        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test public void progressDefaultsTo_NegativeOne() {
        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, service.progressProperty().get(), 0);
    }

    @Test public void runningDefaultsToFalse() {
        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test public void messageDefaultsToEmptyString() {
        assertEquals("", service.getMessage());
        assertEquals("", service.messageProperty().get());
    }

    @Test public void titleDefaultsToEmptyString() {
        assertEquals("", service.getTitle());
        assertEquals("", service.titleProperty().get());
    }

    /******************************************************************
     * Test that 32 simultaneous services will run concurrently       *
     *****************************************************************/

    // This test should be reliable. Each of the concurrent tasks takes 1 second to complete
    // and several micro / milliseconds to get setup and execute. So 2 seconds should be more
    // than enough time.
    @Test(timeout = 2000) public void testManyServicesRunConcurrently() throws Exception {
        if (PlatformUtil.isWindows()) {
            assumeTrue(Boolean.getBoolean("unstable.test")); // JDK-8284552
        }

        final CountDownLatch latch = new CountDownLatch(32);
        for (int i=0; i<32; i++) {
            Service<Void> s = new ServiceShim<Void>() {
                @Override public void checkThread() { }
                @Override public void runLater(Runnable r) { r.run(); }

                @Override protected Task<Void> createTask() {
                    return new TaskShim<Void>() {
                        @Override protected Void call() throws Exception {
                            Thread.sleep(1000);
                            latch.countDown();
                            return null;
                        }

                        @Override public void runLater(Runnable r) {
                            r.run();
                        }

                        @Override public boolean isFxApplicationThread() {
                            return true;
                        }
                    };

                }
            };
            s.start();
        }
        latch.await();
    }
}
