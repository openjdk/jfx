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
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import test.javafx.concurrent.mocks.EpicFailTask;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TaskExceptionTest {

    public static Stream<Arguments> implementations() {
        return Stream.of(
            Arguments.of(new Exception("Exception")),
            Arguments.of(new IllegalArgumentException("IAE")),
            Arguments.of(new NullPointerException("NPE")),
            Arguments.of(new RuntimeException("RuntimeException"))
        );
    }

    private Task task;

    public void setup(Exception exception) {
        task = new EpicFailTask(exception);
    }

    /************************************************************************
     * Run the task and check that the exception property is set, and that
     * the value property is null. The progress fields may be in some
     * arbitrary state.
     ***********************************************************************/

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningExceptionShouldBeSet(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertNotNull(task.getException());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningValueShouldBe_Null(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertNull(task.getValue());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningWorkDoneShouldBe_10(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertEquals(10, task.getWorkDone(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningTotalWorkShouldBe_20(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertEquals(20, task.getTotalWork(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningProgressShouldBe_FiftyPercent(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertEquals(.5, task.getProgress(), 0);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void afterRunningStateShouldBe_FAILED(Exception exception) {
        setup(exception); // NOTE this should be a @BeforeEach call, restore after JUnit5 adds Parametrized classes
        task.run();
        assertEquals(Worker.State.FAILED, task.getState());
    }
}
