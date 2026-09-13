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

import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.concurrent.Worker;
import test.javafx.concurrent.mocks.EpicFailTask;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This tests that state is correct when a Task throws an exception partway through.
 * In this particular case, the progress is updated to 50% before the exception
 * occurs.
 */
public class ServiceExceptionTest extends ServiceTestBase {

    public static Stream<Arguments> implementations() {
        return Stream.of(
            setupServiceFactory(new Exception("Exception")),
            setupServiceFactory(new IllegalArgumentException("IAE")),
            setupServiceFactory(new NullPointerException("NPE")),
            setupServiceFactory(new RuntimeException("RuntimeException"))
        );
    }

    private static Arguments setupServiceFactory(Exception exception) {
        return Arguments.of(
            new TestServiceFactory() {
                @Override public AbstractTask createTestTask() {
                    return new EpicFailTask(exception);
                }
            },
            exception
        );
    }

    /************************************************************************
     * Run the concurrent and check that the exception property is set, and that
     * the value property is null. The progress fields may be in some
     * arbitrary state.
     ***********************************************************************/

    /**
     * Whenever the exception occurs we should have the exception property set
     */
    @ParameterizedTest
    @MethodSource("implementations")
    public void exceptionShouldBeSet(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertSame(exception, service.getException());
        assertSame(exception, service.exceptionProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void exceptionPropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.exceptionProperty().addListener((o, oldValue, newValue) -> passed.set(newValue == exception));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    /**
     * The value should always be null if an exception occurs before the end of the Task.
     */
    @ParameterizedTest
    @MethodSource("implementations")
    public void valueShouldBeNull(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void runningShouldBeFalse(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void runningPropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener((o, oldValue, newValue) -> passed.set(!newValue));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void workDoneShouldBeTen(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertEquals(10, service.getWorkDone(), 0);
        assertEquals(10, service.workDoneProperty().get(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void workDonePropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.workDoneProperty().addListener((observable, oldValue, newValue) -> passed.set(newValue.doubleValue() == 10));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void totalWorkShouldBeTwenty(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertEquals(20, service.getTotalWork(), 0);
        assertEquals(20, service.totalWorkProperty().get(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void totalWorkPropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.totalWorkProperty().addListener((observable, oldValue, newValue) -> passed.set(newValue.doubleValue() == 20));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningProgressShouldBe_FiftyPercent(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertEquals(.5, service.getProgress(), 0);
        assertEquals(.5, service.progressProperty().get(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void progressPropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.progressProperty().addListener((observable, oldValue, newValue) -> passed.set(newValue.doubleValue() == .5));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void stateShouldBe_FAILED(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertSame(Worker.State.FAILED, service.getState());
        assertSame(Worker.State.FAILED, service.stateProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void statePropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.stateProperty().addListener((observable, oldValue, newValue) -> passed.set(newValue == Worker.State.FAILED));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void messageShouldBeLastSetValue(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertEquals("About to fail", service.getMessage());
        assertEquals("About to fail", service.messageProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void messagePropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.messageProperty().addListener((observable, oldValue, newValue) -> passed.set("About to fail".equals(service.getMessage())));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void titleShouldBeLastSetValue(TestServiceFactory factory, Exception exception) {
        setup(factory);
        service.start();
        handleEvents();
        assertEquals("Epic Fail", service.getTitle());
        assertEquals("Epic Fail", service.titleProperty().get());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void titlePropertyNotification(TestServiceFactory factory, Exception exception) {
        setup(factory);
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.titleProperty().addListener((observable, oldValue, newValue) -> passed.set("Epic Fail".equals(service.getTitle())));
        service.start();
        handleEvents();
        assertTrue(passed.get());
    }
}
