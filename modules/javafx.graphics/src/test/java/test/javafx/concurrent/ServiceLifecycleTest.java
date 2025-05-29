/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.concurrent.Service;
import javafx.concurrent.ServiceShim;
import javafx.concurrent.Task;
import javafx.concurrent.TaskShim;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import test.javafx.concurrent.mocks.MythicalEvent;
import test.javafx.concurrent.mocks.SimpleTask;

/**
 * Tests various rules regarding the lifecycle of a Service.
 */
public class ServiceLifecycleTest extends ServiceTestBase {
    /**
     * The ManualExecutor is used so that there is some time period between
     * when something is scheduled and when it actually runs, such that the
     * test code has to manually tell it that it can now run.
     */
    protected ManualExecutor executor;

    /**
     * The task to run, which has methods on it to allow me to manually
     * put it into a passing, failed, or whatnot state.
     */
    protected ManualTask task;

    private TestServiceFactory setupServiceFactory() {
        return new TestServiceFactory() {
            @Override public AbstractTask createTestTask() {
                return task = new ManualTask();
            }
        };
    }

    @Override
    protected Executor createExecutor() {
        return executor = new ManualExecutor(super.createExecutor());
    }

    @BeforeEach
    public void setup() {
        super.setup(setupServiceFactory());
    }

    @AfterEach
    public void tearDown() {
        if (task != null) task.finish.set(true);
    }

    /**
     * This class will schedule the task, and then you can execute
     * it manually by calling executeScheduled. In this way I can
     * test when a Service is scheduled but not yet started.
     */
    protected final class ManualExecutor implements Executor {
        private Runnable scheduled;
        private Executor wrapped;

        ManualExecutor(Executor wrapped) {
            this.wrapped = wrapped;
        }

        @Override public void execute(Runnable command) {
            this.scheduled = command;
        }

        public void executeScheduled() {
            wrapped.execute(scheduled);
            // I need to wait until the next "Sentinel" runnable
            // on the queue, which the Task will post when it begins
            // execution.
            handleEvents();
        }
    }

    protected final class ManualTask extends AbstractTask {
        private AtomicBoolean finish = new AtomicBoolean(false);
        private AtomicReference<Exception> exception = new AtomicReference<>();
        private boolean failToCancel = false;

        @Override protected String call() throws Exception {
            runLater(new Sentinel());
            while (!finish.get()) {
                Exception e = exception.get();
                if (e != null) throw e;
            }
            return "Done";
        }

        public void progress(long done, long max) {
            updateProgress(done, max);
        }

        public void message(String msg) {
            updateMessage(msg);
        }

        public void title(String t) {
            updateTitle(t);
        }

        public void fail(Exception e) {
            exception.set(e);
            handleEvents();
        }

        public void complete() {
            finish.set(true);
            handleEvents();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = failToCancel ? false : super.cancel(mayInterruptIfRunning);
            finish.set(true);
            return result;
        }
    }

    /************************************************************************
     * Tests while in the ready state                                       *
     ***********************************************************************/

    @Test
    public void callingStartInReadyStateSchedulesJob() {
        assertNull(executor.scheduled);
        service.start();
        assertNotNull(executor.scheduled);
    }

    @Test
    public void callingStartInReadyMovesToScheduledState() {
        service.start();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingRestartInReadyStateSchedulesJob() {
        assertNull(executor.scheduled);
        service.restart();
        assertNotNull(executor.scheduled);
    }

    @Test
    public void callingRestartInReadyMovesToScheduledState() {
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingCancelInReadyStateMovesToCancelledState() {
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test
    public void callingResetInReadyStateHasNoEffect() {
        service.reset();
        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    /************************************************************************
     * Tests while in the scheduled state                                   *
     ***********************************************************************/

    @Test
    public void callingStartInScheduledStateIsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            service.start();
        });
    }

    @Test
    public void callingCancelInScheduledStateResultsInCancelledState() {
        service.start();
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test
    public void callingRestartInScheduledStateShouldCancelAndReschedule() {
        service.start();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    /**
     * This test differs from callingRestartInScheduledStateShouldCancelAndReschedule
     * in that under some circumstances, the cancel operation on a task may yield
     * a task which is not marked as CANCELLED, such as when it is already run
     * or cancelled). In such a case, the bindings have not fired yet and the
     * state of the service is off. At least, that is what is happening with
     * JDK-8127414. The fix allows this test to pass.
     */
    @Test
    public void callingRestartInScheduledStateShouldCancelAndReschedule_RT_20880() {
        service.start();
        task.failToCancel = true;
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingResetInScheduledStateThrowsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            service.reset();
        });
    }

    @Test
    public void stateChangesToRunningWhenExecutorExecutes() {
        service.start();
        executor.executeScheduled();
        assertSame(Worker.State.RUNNING, service.getState());
        assertSame(Worker.State.RUNNING, service.stateProperty().get());
    }

    @Test
    public void exceptionShouldBeNullInScheduledState() {
        service.start();
        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test
    public void valueShouldBeNullInScheduledState() {
        service.start();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test
    public void runningShouldBeTrueInScheduledState() {
        service.start();
        assertTrue(service.isRunning());
        assertTrue(service.runningProperty().get());
    }

    @Test
    public void runningPropertyNotificationInScheduledState() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener((o, oldValue, newValue) -> passed.set(newValue));
        service.start();
        assertTrue(passed.get());
    }

    @Test
    public void workDoneShouldBeNegativeOneInitiallyInScheduledState() {
        service.start();
        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test
    public void totalWorkShouldBeNegativeOneAtStartOfScheduledState() {
        service.start();
        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test
    public void progressShouldBeNegativeOneAtStartOfScheduledState() {
        service.start();
        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, task.progressProperty().get(), 0);
    }

    @Test
    public void messageShouldBeEmptyStringWhenEnteringScheduledState() {
        service.start();
        assertEquals("", service.getMessage());
        assertEquals("", task.messageProperty().get());
    }

    @Test
    public void titleShouldBeEmptyStringAtStartOfScheduledState() {
        service.start();
        assertEquals("", service.getTitle());
        assertEquals("", task.titleProperty().get());
    }

    /************************************************************************
     * Tests while in the running state                                     *
     ***********************************************************************/

    @Test
    public void callingStartInRunningStateIsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            executor.executeScheduled();
            service.start();
        });
    }

    @Test
    public void callingResetInRunningStateIsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            executor.executeScheduled();
            service.reset();
        });
    }

    @Test
    public void callingRestartInRunningStateCancelsAndReschedules() {
        service.start();
        executor.executeScheduled();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingCancelInRunningStateResultsInCancelledState() {
        service.start();
        executor.executeScheduled();
        service.cancel();
        assertSame(Worker.State.CANCELLED, service.getState());
        assertSame(Worker.State.CANCELLED, service.stateProperty().get());
    }

    @Test
    public void exceptionShouldBeNullInRunningState() {
        service.start();
        executor.executeScheduled();
        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test
    public void valueShouldBeNullInRunningState() {
        service.start();
        executor.executeScheduled();
        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test
    public void runningShouldBeTrueInRunningState() {
        service.start();
        executor.executeScheduled();
        assertTrue(service.isRunning());
        assertTrue(service.runningProperty().get());
    }

    @Test
    public void runningPropertyNotificationInRunningState() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.runningProperty().addListener((o, oldValue, newValue) -> passed.set(newValue));
        service.start();
        executor.executeScheduled();
        assertTrue(passed.get());
    }

    @Test
    public void workDoneShouldBeNegativeOneInitiallyInRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test
    public void workDoneShouldAdvanceTo10() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(10, service.getWorkDone(), 0);
        assertEquals(10, service.workDoneProperty().get(), 0);
    }

    @Test
    public void workDonePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.workDoneProperty().addListener((o, oldValue, newValue) -> passed.set(newValue.doubleValue() == 10));
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test
    public void totalWorkShouldBeNegativeOneAtStartOfRunning() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test
    public void totalWorkShouldBeTwenty() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(20, service.getTotalWork(), 0);
        assertEquals(20, service.totalWorkProperty().get(), 0);
    }

    @Test
    public void totalWorkPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.totalWorkProperty().addListener((o, oldValue, newValue) -> passed.set(newValue.doubleValue() == 20));
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test
    public void progressShouldBeNegativeOneAtStartOfRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, task.progressProperty().get(), 0);
    }

    @Test
    public void afterRunningProgressShouldBe_FiftyPercent() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        assertEquals(.5, service.getProgress(), 0);
        assertEquals(.5, task.progressProperty().get(), 0);
    }

    @Test
    public void progressPropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.progressProperty().addListener((o, oldValue, newValue) -> passed.set(newValue.doubleValue() == .5));
        executor.executeScheduled();
        task.progress(10, 20);
        assertTrue(passed.get());
    }

    @Test
    public void messageShouldBeEmptyStringWhenEnteringRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals("", service.getMessage());
        assertEquals("", task.messageProperty().get());
    }

    @Test
    public void messageShouldBeLastSetValue() {
        service.start();
        executor.executeScheduled();
        task.message("Running");
        assertEquals("Running", service.getMessage());
        assertEquals("Running", task.messageProperty().get());
    }

    @Test
    public void messagePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.messageProperty().addListener((o, oldValue, newValue) -> passed.set("Running".equals(service.getMessage())));
        executor.executeScheduled();
        task.message("Running");
        assertTrue(passed.get());
    }

    @Test
    public void titleShouldBeEmptyStringAtStartOfRunningState() {
        service.start();
        executor.executeScheduled();
        assertEquals("", service.getTitle());
        assertEquals("", task.titleProperty().get());
    }

    @Test
    public void titleShouldBeLastSetValue() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        assertEquals("Title", service.getTitle());
        assertEquals("Title", task.titleProperty().get());
    }

    @Test
    public void titlePropertyNotification() {
        final AtomicBoolean passed = new AtomicBoolean(false);
        service.start();
        task.titleProperty().addListener((o, oldValue, newValue) -> passed.set("Title".equals(service.getTitle())));
        executor.executeScheduled();
        task.title("Title");
        assertTrue(passed.get());
    }

    /************************************************************************
     * Throw an exception in the running state                              *
     ***********************************************************************/

    @Test
    public void callingStartInFailedStateIsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            executor.executeScheduled();
            task.fail(new Exception("anything"));
            service.start();
        });
    }

    @Test
    public void callingResetInFailedStateResetsStateToREADY() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    @Test
    public void callingResetInFailedStateResetsValueToNull() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test
    public void callingResetInFailedStateResetsExceptionToNull() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test
    public void callingResetInFailedStateResetsWorkDoneToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test
    public void callingResetInFailedStateResetsTotalWorkToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test
    public void callingResetInFailedStateResetsProgressToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(10, 20);
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, service.progressProperty().get(), 0);
    }

    @Test
    public void callingResetInFailedStateResetsRunningToFalse() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.reset();

        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test
    public void callingResetInFailedStateResetsMessageToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.message("Message");
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals("", service.getMessage());
        assertEquals("", service.messageProperty().get());
    }

    @Test
    public void callingResetInFailedStateResetsTitleToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        task.fail(new Exception("anything"));
        service.reset();

        assertEquals("", service.getTitle());
        assertEquals("", service.titleProperty().get());
    }

    @Test
    public void callingRestartInFailedStateReschedules() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingCancelInFailedStateResultsInNoChange() {
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("anything"));
        service.cancel();
        assertSame(Worker.State.FAILED, service.getState());
        assertSame(Worker.State.FAILED, service.stateProperty().get());
    }

    /************************************************************************
     * Proper Completion of a task                                          *
     ***********************************************************************/

    @Test
    public void callingStartInSucceededStateIsISE() {
        assertThrows(IllegalStateException.class, () -> {
            service.start();
            executor.executeScheduled();
            task.progress(20, 20);
            task.complete();
            service.start();
        });
    }

    @Test
    public void callingResetInSucceededStateResetsStateToREADY() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertSame(Worker.State.READY, service.getState());
        assertSame(Worker.State.READY, service.stateProperty().get());
    }

    @Test
    public void callingResetInSucceededStateResetsValueToNull() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertNull(service.getValue());
        assertNull(service.valueProperty().get());
    }

    @Test
    public void callingResetInSucceededStateResetsExceptionToNull() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertNull(service.getException());
        assertNull(service.exceptionProperty().get());
    }

    @Test
    public void callingResetInSucceededStateResetsWorkDoneToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getWorkDone(), 0);
        assertEquals(-1, service.workDoneProperty().get(), 0);
    }

    @Test
    public void callingResetInSucceededStateResetsTotalWorkToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getTotalWork(), 0);
        assertEquals(-1, service.totalWorkProperty().get(), 0);
    }

    @Test
    public void callingResetInSucceededStateResetsProgressToNegativeOne() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals(-1, service.getProgress(), 0);
        assertEquals(-1, service.progressProperty().get(), 0);
    }

    @Test
    public void callingResetInSucceededStateResetsRunningToFalse() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertFalse(service.isRunning());
        assertFalse(service.runningProperty().get());
    }

    @Test
    public void callingResetInSucceededStateResetsMessageToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.message("Message");
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals("", service.getMessage());
        assertEquals("", service.messageProperty().get());
    }

    @Test
    public void callingResetInSucceededStateResetsTitleToEmptyString() {
        service.start();
        executor.executeScheduled();
        task.title("Title");
        task.progress(20, 20);
        task.complete();
        service.reset();

        assertEquals("", service.getTitle());
        assertEquals("", service.titleProperty().get());
    }

    @Test
    public void callingRestartInSucceededStateReschedules() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.restart();
        assertSame(Worker.State.SCHEDULED, service.getState());
        assertSame(Worker.State.SCHEDULED, service.stateProperty().get());
    }

    @Test
    public void callingCancelInSucceededStateResultsInNoChange() {
        service.start();
        executor.executeScheduled();
        task.progress(20, 20);
        task.complete();
        service.cancel();
        assertSame(Worker.State.SUCCEEDED, service.getState());
        assertSame(Worker.State.SUCCEEDED, service.stateProperty().get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onReady                                                       *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onReadyPropertyNameShouldMatchMethodName() {
        assertEquals("onReady", service.onReadyProperty().getName());
    }

    @Test
    public void onReadyBeanShouldMatchService() {
        assertSame(service, service.onReadyProperty().getBean());
    }

    @Test
    public void onReadyIsInitializedToNull() {
        assertNull(service.getOnReady());
        assertNull(service.onReadyProperty().get());
    }

    @Test
    public void onReadyFilterCalledBefore_onReady() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.start();
        executor.executeScheduled();
        task.complete();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_READY, workerStateEvent -> filterCalled.set(true));
        service.setOnReady(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Ready state
        service.reset();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void onReadyHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.start();
        executor.executeScheduled();
        task.complete();
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_READY, workerStateEvent -> handlerCalled.set(true));

        // Transition to Ready state
        service.reset();
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onReadyHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        service.start();
        executor.executeScheduled();
        task.complete();
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_READY, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_READY, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_READY, workerStateEvent -> sanity.set(true));

        service.reset();
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onReadyFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        service.start();
        executor.executeScheduled();
        task.complete();
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_READY, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_READY, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_READY, workerStateEvent -> sanity.set(true));

        service.reset();
        assertTrue(sanity.get());
        assertFalse(filterCalled.get());
    }

    @Test
    public void cancelCalledFromOnReady() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.start();
        executor.executeScheduled();
        task.complete();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_READY, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.reset();
        assertEquals(Worker.State.CANCELLED, service.getState());
        assertEquals(1, cancelNotificationCount.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onScheduled                                                   *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onScheduledPropertyNameShouldMatchMethodName() {
        assertEquals("onScheduled", service.onScheduledProperty().getName());
    }

    @Test
    public void onScheduledBeanShouldMatchService() {
        assertSame(service, service.onScheduledProperty().getBean());
    }

    @Test
    public void onScheduledIsInitializedToNull() {
        assertNull(service.getOnScheduled());
        assertNull(service.onScheduledProperty().get());
    }

    @Test
    public void onScheduledFilterCalledBefore_onScheduled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, workerStateEvent -> filterCalled.set(true));
        service.setOnScheduled(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void scheduledCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnScheduled(workerStateEvent -> handlerCalled.set(true));

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().scheduledSemaphore.getQueueLength() == 0);
    }

    @Test
    public void scheduledCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnScheduled(workerStateEvent -> {
            handlerCalled.set(true);
            workerStateEvent.consume();
        });

        // Transition to Scheduled state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().scheduledSemaphore.getQueueLength() == 0);
    }

    @Test
    public void onScheduledHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SCHEDULED, workerStateEvent -> handlerCalled.set(true));

        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onScheduledHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SCHEDULED, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_SCHEDULED, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SCHEDULED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onScheduledFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        assertTrue(sanity.get());
        assertFalse(filterCalled.get());
    }

    @Test
    public void cancelCalledFromOnScheduled() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SCHEDULED, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.start();
        executor.executeScheduled();
        assertEquals(Worker.State.CANCELLED, service.getState());
        assertEquals(1, cancelNotificationCount.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onRunning                                                     *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onRunningPropertyNameShouldMatchMethodName() {
        assertEquals("onRunning", service.onRunningProperty().getName());
    }

    @Test
    public void onRunningBeanShouldMatchService() {
        assertSame(service, service.onRunningProperty().getBean());
    }

    @Test
    public void onRunningIsInitializedToNull() {
        assertNull(service.getOnRunning());
        assertNull(service.onRunningProperty().get());
    }

    @Test
    public void onRunningFilterCalledBefore_onRunning() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, workerStateEvent -> filterCalled.set(true));
        service.setOnRunning(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void runningCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnRunning(workerStateEvent -> handlerCalled.set(true));

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().runningSemaphore.getQueueLength() == 0);
    }

    @Test
    public void runningCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnRunning(workerStateEvent -> {
            handlerCalled.set(true);
            workerStateEvent.consume();
        });

        // Transition to Running state
        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().runningSemaphore.getQueueLength() == 0);
    }

    @Test
    public void onRunningHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, workerStateEvent -> handlerCalled.set(true));

        service.start();
        executor.executeScheduled();
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onRunningHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_RUNNING, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onRunningFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        assertTrue(sanity.get());
        assertFalse(filterCalled.get());
    }

    @Test
    public void cancelCalledFromOnRunning() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_RUNNING, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.start();
        executor.executeScheduled();
        assertEquals(Worker.State.CANCELLED, service.getState());
        assertEquals(1, cancelNotificationCount.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onSucceeded                                                   *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onSucceededPropertyNameShouldMatchMethodName() {
        assertEquals("onSucceeded", service.onSucceededProperty().getName());
    }

    @Test
    public void onSucceededBeanShouldMatchService() {
        assertSame(service, service.onSucceededProperty().getBean());
    }

    @Test
    public void onSucceededIsInitializedToNull() {
        assertNull(service.getOnSucceeded());
        assertNull(service.onSucceededProperty().get());
    }

    @Test
    public void onSucceededFilterCalledBefore_onSucceeded() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, workerStateEvent -> filterCalled.set(true));
        service.setOnSucceeded(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void succeededCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnSucceeded(workerStateEvent -> handlerCalled.set(true));

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().succeededSemaphore.getQueueLength() == 0);
    }

    @Test
    public void succeededCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnSucceeded(workerStateEvent -> {
            handlerCalled.set(true);
            workerStateEvent.consume();
        });

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().succeededSemaphore.getQueueLength() == 0);
    }

    @Test
    public void onSucceededHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, workerStateEvent -> handlerCalled.set(true));

        service.start();
        executor.executeScheduled();
        task.complete();
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onSucceededHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.complete();
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onSucceededFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.complete();
    }

    @RepeatedTest(50)
    @Test
    public void cancelCalledFromOnSucceeded() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.start();
        executor.executeScheduled();
        task.complete();
        assertEquals(Worker.State.SUCCEEDED, service.getState());
        assertEquals(0, cancelNotificationCount.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onCancelled                                                   *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onCancelledPropertyNameShouldMatchMethodName() {
        assertEquals("onCancelled", service.onCancelledProperty().getName());
    }

    @Test
    public void onCancelledBeanShouldMatchService() {
        assertSame(service, service.onCancelledProperty().getBean());
    }

    @Test
    public void onCancelledIsInitializedToNull() {
        assertNull(service.getOnCancelled());
        assertNull(service.onCancelledProperty().get());
    }

    @Test
    public void onCancelledFilterCalledBefore_onCancelled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, workerStateEvent -> filterCalled.set(true));
        service.setOnCancelled(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void cancelledCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnCancelled(workerStateEvent -> handlerCalled.set(true));

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().cancelledSemaphore.getQueueLength() == 0);
    }

    @Test
    public void cancelledCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnCancelled(workerStateEvent -> {
            handlerCalled.set(true);
            workerStateEvent.consume();
        });

        // Transition to Cancelled state
        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().cancelledSemaphore.getQueueLength() == 0);
    }

    @Test
    public void onCancelledHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, workerStateEvent -> handlerCalled.set(true));

        service.start();
        executor.executeScheduled();
        task.cancel();
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onCancelledHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.cancel();
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onCancelledFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.cancel();
        assertTrue(sanity.get());
        assertFalse(filterCalled.get());
    }

    @Test
    public void cancelCalledFromOnCancelled() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.start();
        executor.executeScheduled();
        task.cancel();
        assertEquals(Worker.State.CANCELLED, service.getState());
        assertEquals(1, cancelNotificationCount.get());
    }

    @RepeatedTest(50)
    @Test
    public void cancelCalledFromOnFailed() {
        final AtomicInteger cancelNotificationCount = new AtomicInteger();
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, workerStateEvent -> {
            service.cancel();
        });
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            cancelNotificationCount.incrementAndGet();
        });

        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit"));
        assertEquals(Worker.State.FAILED, service.getState());
        assertEquals(0, cancelNotificationCount.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests for onFailed                                                      *
     *                                                                         *
     **************************************************************************/

    @Test
    public void onFailedPropertyNameShouldMatchMethodName() {
        assertEquals("onFailed", service.onFailedProperty().getName());
    }

    @Test
    public void onFailedBeanShouldMatchService() {
        assertSame(service, service.onFailedProperty().getBean());
    }

    @Test
    public void onFailedIsInitializedToNull() {
        assertNull(service.getOnFailed());
        assertNull(service.onFailedProperty().get());
    }

    @Test
    public void onFailedFilterCalledBefore_onFailed() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean filterCalledFirst = new AtomicBoolean(false);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, workerStateEvent -> filterCalled.set(true));
        service.setOnFailed(workerStateEvent -> filterCalledFirst.set(filterCalled.get()));

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("The End"));
        // Events should have happened
        assertTrue(filterCalledFirst.get());
    }

    @Test
    public void failedCalledAfterHandler() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnFailed(workerStateEvent -> handlerCalled.set(true));

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit Now"));
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().failedSemaphore.getQueueLength() == 0);
    }

    @Test
    public void failedCalledAfterHandlerEvenIfConsumed() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.setOnFailed(workerStateEvent -> handlerCalled.set(true));

        // Transition to Succeeded state
        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit Now"));
        // Events should have happened
        assertTrue(handlerCalled.get() && factory.getCurrentTask().failedSemaphore.getQueueLength() == 0);
    }

    @Test
    public void onFailedHandlerCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, workerStateEvent -> handlerCalled.set(true));

        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Forget about it"));
        // Events should have happened
        assertTrue(handlerCalled.get());
    }

    @Test
    public void removed_onFailedHandlerNotCalled() {
        final AtomicBoolean handlerCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> handler = workerStateEvent -> handlerCalled.set(true);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, handler);
        service.removeEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, handler);
        service.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit"));
        assertTrue(sanity.get());
        assertFalse(handlerCalled.get());
    }

    @Test
    public void removed_onFailedFilterNotCalled() {
        final AtomicBoolean filterCalled = new AtomicBoolean(false);
        final AtomicBoolean sanity = new AtomicBoolean(false);
        EventHandler<WorkerStateEvent> filter = workerStateEvent -> filterCalled.set(true);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, filter);
        service.removeEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, filter);
        service.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, workerStateEvent -> sanity.set(true));

        service.start();
        executor.executeScheduled();
        task.fail(new Exception("Quit"));
        assertTrue(sanity.get());
        assertFalse(filterCalled.get());
    }

    /***************************************************************************
     *                                                                         *
     * Tests that invoking methods from the wrong thread leads to errors, and  *
     * that regardless of which thread starts the Service, all notification    *
     * (events, etc) happen on the FX thread only.                             *
     *                                                                         *
     **************************************************************************/

    @Test
    public void canCreateServiceOnRandomThread() {
        RandomThread random = new RandomThread(() -> {
            DoNothingService s = null;
            try {
                s = new DoNothingService();
            } finally {
                if (s != null) s.shutdown();
            }
        });
        random.test();
    }

    @Test
    public void canGetReferencesToPropertiesOnRandomThread() {
        RandomThread random = new RandomThread(() -> {
            DoNothingService s = null;
            try {
                s = new DoNothingService();
                s.exceptionProperty();
                s.executorProperty();
                s.messageProperty();
                s.progressProperty();
                s.onCancelledProperty();
                s.onFailedProperty();
                s.onReadyProperty();
                s.onRunningProperty();
                s.onScheduledProperty();
                s.onSucceededProperty();
                s.runningProperty();
                s.stateProperty();
                s.titleProperty();
                s.totalWorkProperty();
                s.valueProperty();
                s.workDoneProperty();
            } finally {
                if (s != null) s.shutdown();
            }
        });
        random.test();
    }

    @Test
    public void canInvokeGettersOnRandomThread() {
        RandomThread random = new RandomThread(() -> {
            DoNothingService s = null;
            try {
                s = new DoNothingService();
                s.getException();
                s.getExecutor();
                s.getMessage();
                s.getProgress();
                s.getOnCancelled();
                s.getOnFailed();
                s.getOnReady();
                s.getOnRunning();
                s.getOnScheduled();
                s.getOnSucceeded();
                s.isRunning();
                s.getState();
                s.getTitle();
                s.getTotalWork();
                s.getValue();
                s.getWorkDone();
            } finally {
                if (s != null) s.shutdown();
            }
        });
        random.test();
    }

    @Test
    public void canInvokeSettersOnRandomThread() {
        RandomThread random = new RandomThread(() -> {
            DoNothingService s = null;
            try {
                s = new DoNothingService();
                ServiceShim.setEventHandler(s, WorkerStateEvent.ANY, event -> {
                });
                s.setOnCancelled(event -> {
                });
                s.setOnFailed(event -> {
                });
                s.setOnReady(event -> {
                });
                s.setOnRunning(event -> {
                });
                s.setOnScheduled(event -> {
                });
                s.setOnSucceeded(event -> {
                });
            } finally {
                if (s != null) s.shutdown();
            }
        });
        random.test();
    }

    @Test
    public void canInvokeStartOnRandomThread() {
        RandomThread random = new RandomThread(() -> {
            DoNothingService s = null;
            try {
                s = new DoNothingService();
                s.start();
            } finally {
                if (s != null) s.shutdown();
            }
        });
        random.test();
    }

    @Test
    public void cannotInvokeRestartOnRandomThreadAfterStart() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.restart());
    }

    @Test
    public void cannotInvokeCancelOnRandomThreadAfterStart() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.cancel();
        });
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_1() throws Throwable {
        assertThrowsException(IllegalStateException.class, s ->
                ServiceShim.setEventHandler(s, WorkerStateEvent.ANY, event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_2() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnCancelled(event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_3() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnFailed(event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_4() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnReady(event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_5() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnRunning(event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_6() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnScheduled(event -> { }));
    }

    @Test
    public void cannotInvokeSettersOnRandomThreadAfterStart_7() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> s.setOnSucceeded(event -> { }));
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_1() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getException();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_2() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getExecutor();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_3() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getMessage();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_4() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getProgress();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_5() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnCancelled();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_6() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnFailed();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_7() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnReady();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_8() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnRunning();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_9() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnScheduled();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_10() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getOnSucceeded();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_11() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.isRunning();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_12() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getState();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_13() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getTitle();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_14() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getTotalWork();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_15() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getValue();
        });
    }

    @Test
    public void cannotInvokeGettersOnRandomThreadAfterStart_16() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.getValue();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_1() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.exceptionProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_2() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.executorProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_3() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.messageProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_4() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.progressProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_5() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onCancelledProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_6() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onFailedProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_7() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onReadyProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_8() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onRunningProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_9() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onScheduledProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_10() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.onSucceededProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_11() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.runningProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_12() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.stateProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_13() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.titleProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_14() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.totalWorkProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_15() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.valueProperty();
        });
    }

    @Test
    public void cannotInvokePropertyGettersOnRandomThreadAfterStart_16() throws Throwable {
        assertThrowsException(IllegalStateException.class, s -> {
            s.workDoneProperty();
        });
    }

    private void assertThrowsException(Class<? extends Throwable> exceptionClass, final ServiceTestExecution c) throws Throwable {
        assertThrows(exceptionClass, () -> {
            RandomThread random = new RandomThread(() -> {
                DoNothingService s = null;
                try {
                    s = new DoNothingService();
                    s.start();
                    c.test(s);
                } finally {
                    if (s != null) s.shutdown();
                }
            });

            try {
                random.test();
            } catch (AssertionError er) {
                throw er.getCause();
            }
        });
    }

    private interface ServiceTestExecution {
        public void test(DoNothingService s);
    }

    /**
     * Specialized thread used for checking access to various methods from a "random thread" other
     * than the FX thread. This class has built into it all the supported needed for handling
     * exceptions and so forth, such that assertion errors are raised if an exception occurs
     * on the thread, and also handles blocking until the thread body concludes.
     */
    private static final class RandomThread extends Thread {
        private final CountDownLatch testCompleted = new CountDownLatch(1);
        private Throwable error;

        public RandomThread(Runnable target) {
            super(target);
        }

        @Override public void run() {
            try {
                super.run();
            } catch (Throwable th) {
                error = th;
            } finally {
                testCompleted.countDown();
            }
        }

        public void test() throws AssertionError {
            start();
            try {
                testCompleted.await();
            } catch (InterruptedException e) {
                throw new AssertionError("Test did not complete normally");
            }
            if (error != null) {
                throw new AssertionError(error);
            }
        }
    }

    /**
     * A service which does absolutely nothing and isn't hardwired to believe that
     * the test thread is the FX thread (unlike the other services in these tests)
     */
    private static final class DoNothingService extends ServiceShim {
        private Thread pretendFXThread;
        private ConcurrentLinkedQueue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
        private volatile boolean shutdown = false;

        public DoNothingService() {
            setExecutor(command -> {
                Thread backgroundThread = new Thread(command);
                backgroundThread.start();
            });
        }

        void shutdown() {
            shutdown = true;
        }

        @Override protected Task createTask() {
            return new TaskShim() {
                @Override protected Object call() throws Exception {
                    return null;
                }

                @Override public boolean isFxApplicationThread() {
                    return Thread.currentThread() == pretendFXThread;
                }

                @Override
                public void runLater(Runnable r) {
                    DoNothingService.this.runLater(r);
                }
            };
        }

        @Override public void runLater(Runnable r) {
            eventQueue.add(r);
            if (pretendFXThread == null) {
                pretendFXThread = new Thread() {
                    @Override public void run() {
                        while (!shutdown) {
                            Runnable event = eventQueue.poll();
                            if (event != null) {
                                event.run();
                            }
                        }
                    }
                };
                pretendFXThread.start();
            }
        }

        @Override public boolean isFxApplicationThread() {
            return Thread.currentThread() == pretendFXThread;
        }
    }

    /***************************************************************************
     *                                                                         *
     * A mythical subclass should be able to set an event handler and          *
     * have events fired on the Service work.                                  *
     *                                                                         *
     **************************************************************************/

    @Test
    public void eventFiredOnSubclassWorks() {
        final AtomicBoolean result = new AtomicBoolean(false);
        TestServiceFactory factory = new TestServiceFactory() {
            @Override public AbstractTask createTestTask() {
                return new SimpleTask();
            }

            @Override public Service<String> createService() {
                MythicalService svc = new MythicalService();
                svc.setHandler(mythicalEvent -> result.set(true));
                ServiceShim.fireEvent(svc, new MythicalEvent());
                return svc;
            }
        };
        Service<String> svc = factory.createService();
        svc.start();
        assertTrue(result.get());
    }

    private static final class MythicalService extends ServiceShim<String> {
        public void setHandler(EventHandler<MythicalEvent> h) {
            ServiceShim.setEventHandler(this, MythicalEvent.ANY, h);
        }

        @Override protected Task<String> createTask() {
            return new SimpleTask();
        }

        @Override public void checkThread() { }

        @Override public void runLater(Runnable r) {
            r.run();
        }
    }
}
