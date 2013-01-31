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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.mocks.EpicFailTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * This tests that state is correct when a Task throws an exception partway through.
 * In this particular case, the progress is updated to 50% before the exception
 * occurs.
 */
@RunWith(Parameterized.class)
public class ServiceExceptionTest extends ServiceTestBase {
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                { new Exception("Exception") },
                { new IllegalArgumentException("IAE") },
                { new NullPointerException("NPE") },
                { new RuntimeException("RuntimeException") }
        });
    }

    private Exception exception;

    public ServiceExceptionTest(Exception th) {
        this.exception = th;
    }

    @Override protected TestServiceFactory setupServiceFactory() {
        return new TestServiceFactory() {
            @Override protected AbstractTask createTestTask() {
                return new EpicFailTask(ServiceExceptionTest.this.exception);
            }
        };
    }

    /************************************************************************
     * Run the concurrent and check that the exception property is set, and that
     * the value property is null. The progress fields may be in some
     * arbitrary state.
     ***********************************************************************/

    /**
     * Whenever the exception occurs we should have the exception property set
     */
    @Test public void exceptionShouldBeSet() {
        service.start();
        handleEvents();
        assertSame(exception, service.getException());
        assertSame(exception, service.exceptionProperty().get());
    }

    @Test public void exceptionPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override public void changed(ObservableValue<? extends Throwable> o,
                                          Throwable oldValue, Throwable newValue) {
                passed.set(newValue == exception);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    /**
     * The value should always be null if an exception occurs before the end of the Task.
     */
    @Test public void valueShouldBeNull() {
        service.start();
        handleEvents();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test public void runningShouldBeFalse() {
        service.start();
        handleEvents();
        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test public void runningPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> o,
                                          Boolean oldValue, Boolean newValue) {
                passed.set(!newValue);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void workDoneShouldBeTen() {
        service.start();
        handleEvents();
        assertEquals(10, service.getWorkDone(), 0);
        assertEquals(10, service.workDoneProperty().get(), 0);
    }

    @Test public void workDonePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == 10);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void totalWorkShouldBeTwenty() {
        service.start();
        handleEvents();
        assertEquals(20, service.getTotalWork(), 0);
        assertEquals(20, service.totalWorkProperty().get(), 0);
    }

    @Test public void totalWorkPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.totalWorkProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == 20);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void afterRunningProgressShouldBe_FiftyPercent() {
        service.start();
        handleEvents();
        assertEquals(.5, service.getProgress(), 0);
        assertEquals(.5, service.progressProperty().get(), 0);
    }

    @Test public void progressPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                passed.set(newValue.doubleValue() == .5);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void stateShouldBe_FAILED() {
        service.start();
        handleEvents();
        assertSame(Worker.State.FAILED, service.getState());
        assertSame(Worker.State.FAILED, service.stateProperty().get());
    }

    @Test public void statePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                passed.set(newValue == Worker.State.FAILED);
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void messageShouldBeLastSetValue() {
        service.start();
        handleEvents();
        assertEquals("About to fail", service.getMessage());
        assertEquals("About to fail", service.messageProperty().get());
    }

    @Test public void messagePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                passed.set("About to fail".equals(service.getMessage()));
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @Test public void titleShouldBeLastSetValue() {
        service.start();
        handleEvents();
        assertEquals("Epic Fail", service.getTitle());
        assertEquals("Epic Fail", service.titleProperty().get());
    }

    @Test public void titlePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                passed.set("Epic Fail".equals(service.getTitle()));
            }
        });
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }
}
