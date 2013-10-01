/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.concurrent.mocks.EpicFailTask;
import javafx.concurrent.mocks.SimpleTask;
import javafx.util.Callback;
import javafx.util.Duration;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the ScheduledService.
 */
public class ScheduledServiceTest extends ServiceTestBase {
    private static final Callback<Void, AbstractTask> EPIC_FAIL_FACTORY = new Callback<Void, AbstractTask>() {
        @Override public AbstractTask call(Void param) {
            return new EpicFailTask();
        }
    };

    /**
     * The service that we're going to test. Because a ScheduledService
     * in its default configuration will run forever and because for the
     * sake of testing we've essentially caused ScheduledServiceTest to
     * run as though it is single threaded, we have to make sure that each
     * individual iteration is paused and doesn't occur without an explicit
     * call. So in the test code you can call start(), and then read the wall
     * clock time, and then call iterate() to cause the scheduled service to
     * start the next iteration all without affecting the "wall clock" time
     * inappropriately with the test code. In this way we can test with very
     * fine tolerances in a consistent manner.
     */
    private ScheduledServiceMock s;

    /**
     * If specified by the test BEFORE the service is started, then this
     * task will be used by the service. Defaults to SimpleTask if null.
     */
    private Callback<Void,AbstractTask> taskFactory = null;

    /**
     * A fake "wall clock" time, to keep track of how much
     * time was spent executing a task, and how much time was
     * spent in the delay. We fake out the delay by overriding the
     * "schedule" method in ScheduledServiceMock, and we fake out
     * the task execution time by using a custom task which, when
     * executed, will add to the wall clock time.
     */
    private long wallClock;

    @Override protected TestServiceFactory setupServiceFactory() {
        return new TestServiceFactory() {
            @Override protected AbstractTask createTestTask() {
                return taskFactory == null ? new SimpleTask() : taskFactory.call(null);
            }

            @Override protected Service<String> createService() {
                return new ScheduledServiceMock(this);
            }
        };
    }

    @Override public void setup() {
        super.setup();
        s = (ScheduledServiceMock) service;
        wallClock = 0;
    }

    /**************************************************************************************************
     * Big pile of tests for making sure setting the cumulative period works in a predictable manner  *
     * regardless of what kind of output comes from the back-off algorithm, also taking into          *
     * account the maximum cumulative period value.                                                   *
     *************************************************************************************************/

    @Test public void setCumulativePeriod_MaxIsInfinity_TwoSeconds() {
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.seconds(2), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsInfinity_Negative() {
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsInfinity_NegativeInfinity() {
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsInfinity_NaN() {
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsInfinity_PositiveInfinity() {
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsInfinity_MAX_VALUE() {
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.millis(Double.MAX_VALUE), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_TwoSeconds() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.seconds(2), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_Negative() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_NegativeInfinity() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_NaN() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_PositiveInfinity() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNaN_MAX_VALUE() {
        s.setMaximumCumulativePeriod(Duration.UNKNOWN);
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.millis(Double.MAX_VALUE), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_TwoSeconds() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.seconds(2), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_Negative() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_NegativeInfinity() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_NaN() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_PositiveInfinity() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNull_MAX_VALUE() {
        s.setMaximumCumulativePeriod(null);
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.millis(Double.MAX_VALUE), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_TwoSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.seconds(2), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_TenSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(10));
        assertEquals(Duration.seconds(10), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_TwelveSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(12));
        assertEquals(Duration.seconds(10), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_Negative() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_NegativeInfinity() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_NaN() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_PositiveInfinity() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.seconds(10), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs10_MAX_VALUE() {
        s.setMaximumCumulativePeriod(Duration.seconds(10));
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.seconds(10), s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_TwoSeconds() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_TenSeconds() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(10));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_TwelveSeconds() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(12));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_Negative() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_NegativeInfinity() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_NaN() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_PositiveInfinity() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIs0_MAX_VALUE() {
        s.setMaximumCumulativePeriod(Duration.ZERO);
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_TwoSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_TenSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(10));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_TwelveSeconds() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(12));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_Negative() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(-2));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_NegativeInfinity() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(Double.NEGATIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_NaN() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(Double.NaN));
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_PositiveInfinity() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.seconds(Double.POSITIVE_INFINITY));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    @Test public void setCumulativePeriod_MaxIsNegative_MAX_VALUE() {
        s.setMaximumCumulativePeriod(Duration.seconds(-1));
        s.setCumulativePeriod(Duration.millis(Double.MAX_VALUE));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
    }

    // TODO I think Duration boundary condition checking is wrong. It doesn't use isInfinite, but checks
    // directly with POSITIVE_INFINITY, neglecting to check NEGATIVE_INFINITY.

    // DELAY
    // Test that:
    //     delay (positive, unknown, zero) works the first time
    //     delay is not used on the next iteration
    //     delay works on restart
    //     delay works on reset / start

    @Test public void delayIsHonored_Positive() throws InterruptedException {
        s.setDelay(Duration.seconds(1));
        s.start();
        assertEquals(1000, wallClock);
    }

    @Test public void delayIsHonored_Unknown() throws InterruptedException {
        s.setDelay(Duration.UNKNOWN);
        s.start();
        assertEquals(0, wallClock);
    }

    @Test public void delayIsHonored_Infinite() throws InterruptedException {
        s.setDelay(Duration.INDEFINITE);
        s.start();
        assertEquals(Long.MAX_VALUE, wallClock);
    }

    @Test public void delayIsHonored_ZERO() throws InterruptedException {
        s.setDelay(Duration.ZERO);
        s.start();
        assertEquals(0, wallClock);
    }

    @Test public void delayIsNotUsedOnSubsequentIteration() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        s.iterate();
        assertEquals(4000, wallClock); // 1 sec initial delay + 3 second iteration delay
    }

    @Test public void delayIsUsedOnRestart() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        s.iterate();
        s.cancel();
        wallClock = 0;
        s.restart();
        assertEquals(1000, wallClock);
    }

    @Test public void delayIsUsedOnStartFollowingReset() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        s.iterate();
        s.cancel();
        wallClock = 0;
        s.reset();
        s.start();
        assertEquals(1000, wallClock);
    }

    // PERIOD
    // Test that:
    //     period does not contribute to the delay
    //     amount of time from start of one iteration (run) to another (run) is never < period
    //         run time < period
    //         run time == period
    //         run time > period
    //     start of last period is reset after "reset" call (or restart)

    @Test public void periodDoesNotContributeToDelay() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        assertEquals(1000, wallClock);
    }

    @Test public void executionTimeLessThanPeriod() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        s.iterate();
        assertEquals(4000, wallClock); // 1 sec initial delay + 3 second iteration delay
    }

    @Test public void executionTimeEqualsPeriod() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        wallClock += 3000; // simulate execution time
        s.iterate();
        assertEquals(4000, wallClock);
    }

    @Test public void executionTimeExceedsPeriod() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        wallClock += 6000; // simulate execution time
        s.iterate();
        assertEquals(7000, wallClock);
    }

    @Test public void startOfPeriodIsResetAfterReset() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        wallClock += 6000; // simulate execution time
        s.iterate();
        s.cancel();
        wallClock = 0;
        s.reset();
        s.start();
        s.iterate();
        assertEquals(4000, wallClock);
    }

    @Test public void startOfPeriodIsResetAfterRestart() {
        s.setDelay(Duration.seconds(1));
        s.setPeriod(Duration.seconds(3));
        s.start();
        wallClock += 6000; // simulate execution time
        s.iterate();
        s.cancel();
        wallClock = 0;
        s.reset();
        s.start();
        s.iterate();
        assertEquals(4000, wallClock);
    }

    // COMPUTE BACKOFF
    // Test that:
    //     on task failure, cumulative period is increased according to compute backoff
    //          EXPONENTIAL_BACKOFF_STRATEGY, LOGARITHMIC_BACKOFF_STRATEGY, LINEAR_BACKOFF_STRATEGY, custom backoff

    @Test public void onFailureCumulativePeriodIsIncreased_EXPONENTIAL_BACKOFF_zero() {
        s.setBackoffStrategy(ScheduledService.EXPONENTIAL_BACKOFF_STRATEGY);
        s.setPeriod(Duration.ZERO);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(Math.exp(1)), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_EXPONENTIAL_BACKOFF_one() {
        s.setBackoffStrategy(ScheduledService.EXPONENTIAL_BACKOFF_STRATEGY);
        s.setPeriod(Duration.seconds(1));
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(1000 + (1000 * Math.exp(1))), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_EXPONENTIAL_BACKOFF_indefinite() {
        s.setBackoffStrategy(ScheduledService.EXPONENTIAL_BACKOFF_STRATEGY);
        s.setPeriod(Duration.INDEFINITE);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_EXPONENTIAL_BACKOFF_unknown() {
        s.setBackoffStrategy(ScheduledService.EXPONENTIAL_BACKOFF_STRATEGY);
        s.setPeriod(Duration.UNKNOWN);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LOGARITHMIC_BACKOFF_zero() {
        s.setBackoffStrategy(ScheduledService.LOGARITHMIC_BACKOFF_STRATEGY);
        s.setPeriod(Duration.ZERO);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(Math.log1p(1)), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LOGARITHMIC_BACKOFF_one() {
        s.setBackoffStrategy(ScheduledService.LOGARITHMIC_BACKOFF_STRATEGY);
        s.setPeriod(Duration.seconds(1));
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(1000 + (1000 * Math.log1p(1))), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LOGARITHMIC_BACKOFF_indefinite() {
        s.setBackoffStrategy(ScheduledService.LOGARITHMIC_BACKOFF_STRATEGY);
        s.setPeriod(Duration.INDEFINITE);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LOGARITHMIC_BACKOFF_unknown() {
        s.setBackoffStrategy(ScheduledService.LOGARITHMIC_BACKOFF_STRATEGY);
        s.setPeriod(Duration.UNKNOWN);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LINEAR_BACKOFF_zero() {
        s.setBackoffStrategy(ScheduledService.LINEAR_BACKOFF_STRATEGY);
        s.setPeriod(Duration.ZERO);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(1), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LINEAR_BACKOFF_one() {
        s.setBackoffStrategy(ScheduledService.LINEAR_BACKOFF_STRATEGY);
        s.setPeriod(Duration.seconds(1));
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.millis(1000 + (1000 * 1)), s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LINEAR_BACKOFF_indefinite() {
        s.setBackoffStrategy(ScheduledService.LINEAR_BACKOFF_STRATEGY);
        s.setPeriod(Duration.INDEFINITE);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.INDEFINITE, s.getCumulativePeriod());
    }

    @Test public void onFailureCumulativePeriodIsIncreased_LINEAR_BACKOFF_unknown() {
        s.setBackoffStrategy(ScheduledService.LINEAR_BACKOFF_STRATEGY);
        s.setPeriod(Duration.UNKNOWN);
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertEquals(Duration.UNKNOWN, s.getCumulativePeriod());
    }

    // CUMULATIVE PERIOD
    // Test that:
    //     cumulative period is initially equivalent to period
    //         Cumulative period is set when the service enters "scheduled" state
    //     cumulative period is unchanged after successful iteration
    //     cumulative period is modified on failure (tested in onFailure*** tests)
    //     cumulative period is not modified on cancel

    @Test public void cumulativePeriodSetWhenScheduled() {
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
        s.setPeriod(Duration.seconds(1));
        assertEquals(Duration.ZERO, s.getCumulativePeriod());
        s.start();
        assertEquals(Duration.seconds(1), s.getCumulativePeriod());
    }

    @Test public void cumulativePeriodDoesNotChangeOnSuccessfulRun() {
        s.setPeriod(Duration.seconds(1));
        s.start();
        s.iterate();
        assertEquals(Duration.seconds(1), s.getCumulativePeriod());
    }

    @Test public void cumulativePeriodResetOnSuccessfulRun() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new AbstractTask() {
                    @Override protected String call() throws Exception {
                        int c = counter.incrementAndGet();
                        if (c < 10) throw new Exception("Kaboom!");
                        return "Success";
                    }
                };
            }
        };
        s.setPeriod(Duration.seconds(1));
        s.start();
        for (int i=0; i<8; i++) s.iterate();
        assertTrue(Duration.seconds(1).lessThan(s.getCumulativePeriod()));
        s.iterate();
        assertEquals(Duration.seconds(1), s.getCumulativePeriod());
    }

    @Test public void cumulativePeriodDoesNotChangeOnCancelRun() {
        s.setPeriod(Duration.seconds(1));
        s.start();
        s.iterate();
        s.cancel();
        assertEquals(Duration.seconds(1), s.getCumulativePeriod());
    }

    // RESTART ON FAILURE
    // Test that:
    //     value of true causes a new iteration if the task fails
    //     value of false causes the Service to enter Failed state if the task fails.

    @Test public void restartOnFailure_True() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new EpicFailTask() {
                    @Override protected String call() throws Exception {
                        counter.incrementAndGet();
                        return super.call();
                    }
                };
            }
        };
        s.start();
        assertEquals(Worker.State.SCHEDULED, s.getState());
        s.iterate();
        assertEquals(2, counter.get());
    }

    @Test public void restartOnFailure_False() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new EpicFailTask() {
                    @Override protected String call() throws Exception {
                        counter.incrementAndGet();
                        return super.call();
                    }
                };
            }
        };
        s.setRestartOnFailure(false);
        s.start();
        assertEquals(Worker.State.FAILED, s.getState());
        assertEquals(1, counter.get());
    }

    // MAXIMUM FAILURE COUNT / CURRENT FAILURE COUNT
    // Test that:
    //     service iterates while currentFailureCount < maximumFailureCount
    //     service fails after currentFailureCount == maximumFailureCount
    //         service halts when this happens.
    //     currentFailureCount increments on each failure by 1
    //     currentFailureCount is reset on "reset" and "restart"

    @Test public void serviceIteratesWhile_CurrentFailureCount_IsLessThan_MaximumFailureCount() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new EpicFailTask() {
                    @Override protected String call() throws Exception {
                        counter.incrementAndGet();
                        return super.call();
                    }
                };
            }
        };
        s.setMaximumFailureCount(10);
        s.start();
        while (s.getState() != Worker.State.FAILED) {
            assertEquals(counter.get(), s.getCurrentFailureCount());
            s.iterate();
        }
        assertEquals(10, counter.get());
        assertEquals(counter.get(), s.getCurrentFailureCount());
    }

    @Test public void currentFailureCountIsResetOnRestart() {
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        for (int i=0; i<10; i++) s.iterate();
        taskFactory = null;
        s.restart();
        assertEquals(0, s.getCurrentFailureCount());
    }

    @Test public void currentFailureCountIsResetOnReset() {
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        for (int i=0; i<10; i++) s.iterate();
        s.cancel();
        s.reset();
        assertEquals(0, s.getCurrentFailureCount());
    }

    @Test public void currentFailureCountIsNotResetOnCancel() {
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        for (int i=0; i<10; i++) s.iterate();
        s.cancel();
        assertEquals(11, s.getCurrentFailureCount());
    }

    // LAST VALUE
    // Test that:
    //     last value starts as null
    //     last value is still null if first iteration fails
    //     last value equals value from successful iteration (1 & 2)
    //     last value is cleared on "restart" / "reset"

    @Test public void lastValueIsInitiallyNull() {
        assertNull(s.getLastValue());
    }

    @Test public void lastValueIsNullAfterFailedFirstIteration() {
        taskFactory = EPIC_FAIL_FACTORY;
        s.start();
        assertNull(s.getLastValue());
    }

    @Test public void lastValueIsSetAfterSuccessfulFirstIteration() {
        s.start();
        assertEquals("Sentinel", s.getLastValue());
        assertNull(s.getValue());
    }

    @Test public void lastValueIsSetAfterFailedFirstIterationAndSuccessfulSecondIteration() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new AbstractTask() {
                    @Override protected String call() throws Exception {
                        int c = counter.incrementAndGet();
                        if (c == 1) throw new Exception("Bombed out!");
                        return "Success";
                    }
                };
            }
        };
        s.start();
        assertNull(s.getLastValue());
        assertNull(s.getValue());
        s.iterate();
        assertEquals("Success", s.getLastValue());
        assertNull(s.getValue());
    }

    @Test public void lastValueIsUnchangedAfterSuccessfulFirstIterationAndFailedSecondIteration() {
        final AtomicInteger counter = new AtomicInteger();
        taskFactory = new Callback<Void, AbstractTask>() {
            @Override public AbstractTask call(Void param) {
                return new AbstractTask() {
                    @Override protected String call() throws Exception {
                        int c = counter.incrementAndGet();
                        if (c == 1) return "Success";
                        throw new Exception("Bombed out!");
                    }
                };
            }
        };
        s.start();
        assertEquals("Success", s.getLastValue());
        assertNull(s.getValue());
        s.iterate();
        assertEquals("Success", s.getLastValue());
        assertNull(s.getValue());
    }

    @Test public void lastValueIsClearedOnReset() {
        s.start();
        assertEquals("Sentinel", s.getLastValue());
        s.cancel();
        assertEquals("Sentinel", s.getLastValue());
        s.reset();
        assertNull(s.getLastValue());
    }

    /**
     * Allows us to monkey with how the threading works for the sake of testing.
     * Basically, you just call start() in order to go through an entire first
     * iteration, and a call to iterate() causes it to go through a subsequent
     * iteration. At the end of each iteration, you are in the SCHEDULED state,
     * unless failures occurred while running the task that caused the service
     * to finally enter the FAILED state.
     */
    private final class ScheduledServiceMock extends ScheduledService<String> {
        private TestServiceFactory factory;
        private Task<String> nextTask = null;

        ScheduledServiceMock(TestServiceFactory f) {
            this.factory = f;
        }

        @Override protected Task<String> createTask() {
            factory.currentTask = factory.createTestTask();
            factory.currentTask.test = factory.test;
            return factory.currentTask;
        }

        @Override void checkThread() { }

        @Override void schedule(TimerTask task, long delay) {
            wallClock += delay;
            task.run();
        }

        @Override protected void executeTask(Task<String> task) {
            nextTask = task;
            if (isFreshStart()) iterate();
        }

        @Override long clock() {
            return wallClock;
        }

        @Override boolean isFxApplicationThread() {
            return Thread.currentThread() == factory.appThread;
        }

        void iterate() {
            assert nextTask != null;
            Task<String> task = nextTask;
            nextTask = null;

            super.executeTask(task);
            handleEvents();
        }
    }
}
