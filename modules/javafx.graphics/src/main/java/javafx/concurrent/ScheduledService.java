/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import javafx.util.Duration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>The ScheduledService is a {@link Service} which will automatically restart
 * itself after a successful execution, and under some conditions will
 * restart even in case of failure. A new ScheduledService begins in
 * the READY state, just as a normal Service. After calling
 * <code>start</code> or <code>restart</code>, the ScheduledService will
 * enter the SCHEDULED state for the duration specified by <code>delay</code>.
 * </p>
 *
 * <p>Once RUNNING, the ScheduledService will execute its Task. On successful
 * completion, the ScheduledService will transition to the SUCCEEDED state,
 * and then to the READY state and back to the SCHEDULED state. The amount
 * of time the ScheduledService will remain in this state depends on the
 * amount of time between the last state transition to RUNNING, and the
 * current time, and the <code>period</code>. In short, the <code>period</code>
 * defines the minimum amount of time from the start of one run and the start of
 * the next. If the previous execution completed before <code>period</code> expires,
 * then the ScheduledService will remain in the SCHEDULED state until the period
 * expires. If on the other hand the execution took longer than the
 * specified period, then the ScheduledService will immediately transition
 * back to RUNNING. </p>
 *
 * <p>If, while RUNNING, the ScheduledService's Task throws an error or in
 * some other way ends up transitioning to FAILED, then the ScheduledService
 * will either restart or quit, depending on the values for
 * <code>backoffStrategy</code>, <code>restartOnFailure</code>, and
 * <code>maximumFailureCount</code>.</p>
 *
 * <p>If a failure occurs and <code>restartOnFailure</code> is false, then
 * the ScheduledService will transition to FAILED and will stop. To restart
 * a failed ScheduledService, you must call restart manually.</p>
 *
 * <p>If a failure occurs and <code>restartOnFailure</code> is true, then
 * the the ScheduledService <em>may</em> restart automatically. First,
 * the result of calling <code>backoffStrategy</code> will become the
 * new <code>cumulativePeriod</code>. In this way, after each failure, you can cause
 * the service to wait a longer and longer period of time before restarting.
 * Once the task completes successfully, the cumulativePeriod is reset to
 * the value of <code>period</code>.</p>
 *
 * <p>ScheduledService defines static EXPONENTIAL_BACKOFF_STRATEGY and LOGARITHMIC_BACKOFF_STRATEGY
 * implementations, of which LOGARITHMIC_BACKOFF_STRATEGY is the default value for
 * backoffStrategy. After <code>maximumFailureCount</code> is reached, the
 * ScheduledService will transition to FAILED in exactly the same way as if
 * <code>restartOnFailure</code> were false.</p>
 *
 * <p>If the <code>period</code> or <code>delay</code> is changed while the
 * ScheduledService is running, the new values will be taken into account on the
 * next iteration. For example, if the <code>period</code> is increased, then the next time the
 * ScheduledService enters the SCHEDULED state, the new <code>period</code> will be used.
 * Likewise, if the <code>delay</code> is changed, the new value will be honored on
 * the next restart or reset/start.</p>
 *
 * The ScheduledService is typically used for use cases that involve polling. For
 * example, you may want to ping a server on a regular basis to see if there are
 * any updates. Such as ScheduledService might be implemented like this:
 *
 * <pre><code>
 * {@literal ScheduledService<Document> svc = new ScheduledService<Document>()} {
 *     {@literal protected Task<Document> createTask()} {
 *         {@literal return new Task<Document>()} {
 *             protected Document call() {
 *                 // Connect to a Server
 *                 // Get the XML document
 *                 // Parse it into a document
 *                 return document;
 *             }
 *         };
 *     }
 * };
 * svc.setPeriod(Duration.seconds(1));
 * </code></pre>
 *
 * This example will ping the remote server every 1 second.
 *
 * <p>Timing for this class is not absolutely reliable. A very busy event thread might introduce some timing
 * lag into the beginning of the execution of the background Task, so very small values for the period or
 * delay are likely to be inaccurate. A delay or period in the hundreds of milliseconds or larger should be
 * fairly reliable.</p>
 *
 * <p>The ScheduledService in its default configuration has a default <code>period</code> of 0 and a
 * default <code>delay</code> of 0. This will cause the ScheduledService to execute the task immediately
 * upon {@link #start()}, and re-executing immediately upon successful completion.</p>
 *
 * <p>For this purposes of this class, any Duration that answers true to {@link javafx.util.Duration#isUnknown()}
 * will treat that duration as if it were Duration.ZERO. Likewise, any Duration which answers true
 * to {@link javafx.util.Duration#isIndefinite()} will be treated as if it were a duration of Double.MAX_VALUE
 * milliseconds. Any null Duration is treated as Duration.ZERO. Any custom implementation of an backoff strategy
 * callback must be prepared to handle these different potential values.</p>
 *
 * <p>The ScheduledService introduces a new property called {@link #lastValueProperty() lastValue}. The lastValue is the value that
 * was last successfully computed. Because a Service clears its {@code value} property on each run, and
 * because the ScheduledService will reschedule a run immediately after completion (unless it enters the
 * cancelled or failed states), the value property is not overly useful on a ScheduledService. In most cases
 * you will want to instead use the value returned by lastValue.</p>
 *
 * <b>Implementer Note:</b> The {@link #ready()}, {@link #scheduled()}, {@link #running()}, {@link #succeeded()},
 * {@link #cancelled()}, and {@link #failed()} methods are implemented in this class. Subclasses which also
 * override these methods must take care to invoke the super implementation.
 *
 * @param <V> The computed value of the ScheduledService
 * @since JavaFX 8.0
 */
public abstract class ScheduledService<V> extends Service<V> {
    /**
     * A Callback implementation for the <code>backoffStrategy</code> property which
     * will exponentially backoff the period between re-executions in the case of
     * a failure. This computation takes the original period and the number of
     * consecutive failures and computes the backoff amount from that information.
     *
     * <p>If the {@code service} is null, then Duration.ZERO is returned. If the period is 0 then
     * the result of this method will simply be {@code Math.exp(currentFailureCount)}. In all other cases,
     * the returned value is the same as {@code period + (period * Math.exp(currentFailureCount))}.</p>
     */
    public static final Callback<ScheduledService<?>, Duration> EXPONENTIAL_BACKOFF_STRATEGY
            = new Callback<ScheduledService<?>, Duration>() {
        @Override public Duration call(ScheduledService<?> service) {
            if (service == null) return Duration.ZERO;
            final double period = service.getPeriod() == null ? 0 : service.getPeriod().toMillis();
            final double x = service.getCurrentFailureCount();
            return Duration.millis(period == 0 ? Math.exp(x) : period + (period * Math.exp(x)));
        }
    };

    /**
     * A Callback implementation for the <code>backoffStrategy</code> property which
     * will logarithmically backoff the period between re-executions in the case of
     * a failure. This computation takes the original period and the number of
     * consecutive failures and computes the backoff amount from that information.
     *
     * <p>If the {@code service} is null, then Duration.ZERO is returned. If the period is 0 then
     * the result of this method will simply be {@code Math.log1p(currentFailureCount)}. In all other cases,
     * the returned value is the same as {@code period + (period * Math.log1p(currentFailureCount))}.</p>
     */
    public static final Callback<ScheduledService<?>, Duration> LOGARITHMIC_BACKOFF_STRATEGY
            = new Callback<ScheduledService<?>, Duration>() {
        @Override public Duration call(ScheduledService<?> service) {
            if (service == null) return Duration.ZERO;
            final double period = service.getPeriod() == null ? 0 : service.getPeriod().toMillis();
            final double x = service.getCurrentFailureCount();
            return Duration.millis(period == 0 ? Math.log1p(x) : period + (period * Math.log1p(x)));
        }
    };

    /**
     * A Callback implementation for the <code>backoffStrategy</code> property which
     * will linearly backoff the period between re-executions in the case of
     * a failure. This computation takes the original period and the number of
     * consecutive failures and computes the backoff amount from that information.
     *
     * <p>If the {@code service} is null, then Duration.ZERO is returned. If the period is 0 then
     * the result of this method will simply be {@code currentFailureCount}. In all other cases,
     * the returned value is the same as {@code period + (period * currentFailureCount)}.</p>
     */
    public static final Callback<ScheduledService<?>, Duration> LINEAR_BACKOFF_STRATEGY
            = new Callback<ScheduledService<?>, Duration>() {
        @Override public Duration call(ScheduledService<?> service) {
            if (service == null) return Duration.ZERO;
            final double period = service.getPeriod() == null ? 0 : service.getPeriod().toMillis();
            final double x = service.getCurrentFailureCount();
            return Duration.millis(period == 0 ? x : period + (period * x));
        }
    };

    /**
     * This Timer is used to schedule the delays for each ScheduledService. A single timer
     * ought to be able to easily service thousands of ScheduledService objects.
     */
    private static final Timer DELAY_TIMER = new Timer("ScheduledService Delay Timer", true);

    /**
     * The initial delay between when the ScheduledService is first started, and when it will begin
     * operation. This is the amount of time the ScheduledService will remain in the SCHEDULED state,
     * before entering the RUNNING state, following a fresh invocation of {@link #start()} or {@link #restart()}.
     */
    private ObjectProperty<Duration> delay = new SimpleObjectProperty<>(this, "delay", Duration.ZERO);
    public final Duration getDelay() { return delay.get(); }
    public final void setDelay(Duration value) { delay.set(value); }
    public final ObjectProperty<Duration> delayProperty() { return delay; }

    /**
     * The minimum amount of time to allow between the start of the last run and the start of the next run.
     * The actual period (also known as <code>cumulativePeriod</code>)
     * will depend on this property as well as the <code>backoffStrategy</code> and number of failures.
     */
    private ObjectProperty<Duration> period = new SimpleObjectProperty<>(this, "period", Duration.ZERO);
    public final Duration getPeriod() { return period.get(); }
    public final void setPeriod(Duration value) { period.set(value); }
    public final ObjectProperty<Duration> periodProperty() { return period; }

    /**
     * Computes the amount of time to add to the period on each failure. This cumulative amount is reset whenever
     * the the ScheduledService is manually restarted.
     */
    private ObjectProperty<Callback<ScheduledService<?>,Duration>> backoffStrategy =
            new SimpleObjectProperty<>(this, "backoffStrategy", LOGARITHMIC_BACKOFF_STRATEGY);
    public final Callback<ScheduledService<?>,Duration> getBackoffStrategy() { return backoffStrategy.get(); }
    public final void setBackoffStrategy(Callback<ScheduledService<?>, Duration> value) { backoffStrategy.set(value); }
    public final ObjectProperty<Callback<ScheduledService<?>,Duration>> backoffStrategyProperty() { return backoffStrategy; }

    /**
     * Indicates whether the ScheduledService should automatically restart in the case of a failure in the Task.
     */
    private BooleanProperty restartOnFailure = new SimpleBooleanProperty(this, "restartOnFailure", true);
    public final boolean getRestartOnFailure() { return restartOnFailure.get(); }
    public final void setRestartOnFailure(boolean value) { restartOnFailure.set(value); }
    public final BooleanProperty restartOnFailureProperty() { return restartOnFailure; }

    /**
     * The maximum number of times the ScheduledService can fail before it simply ends in the FAILED
     * state. You can of course restart the ScheduledService manually, which will cause the current
     * count to be reset.
     */
    private IntegerProperty maximumFailureCount = new SimpleIntegerProperty(this, "maximumFailureCount", Integer.MAX_VALUE);
    public final int getMaximumFailureCount() { return maximumFailureCount.get(); }
    public final void setMaximumFailureCount(int value) { maximumFailureCount.set(value); }
    public final IntegerProperty maximumFailureCountProperty() { return maximumFailureCount; }

    /**
     * The current number of times the ScheduledService has failed. This is reset whenever the
     * ScheduledService is manually restarted.
     */
    private ReadOnlyIntegerWrapper currentFailureCount = new ReadOnlyIntegerWrapper(this, "currentFailureCount", 0);
    public final int getCurrentFailureCount() { return currentFailureCount.get(); }
    public final ReadOnlyIntegerProperty currentFailureCountProperty() { return currentFailureCount.getReadOnlyProperty(); }
    private void setCurrentFailureCount(int value) {
        currentFailureCount.set(value);
    }

    /**
     * The current cumulative period in use between iterations. This will be the same as <code>period</code>,
     * except after a failure, in which case the result of the backoffStrategy will be used as the cumulative period
     * following each failure. This is reset whenever the ScheduledService is manually restarted or an iteration
     * is successful. The cumulativePeriod is modified when the ScheduledService enters the scheduled state.
     * The cumulativePeriod can be capped by setting the {@code maximumCumulativePeriod}.
     */
    private ReadOnlyObjectWrapper<Duration> cumulativePeriod = new ReadOnlyObjectWrapper<>(this, "cumulativePeriod", Duration.ZERO);
    public final Duration getCumulativePeriod() { return cumulativePeriod.get(); }
    public final ReadOnlyObjectProperty<Duration> cumulativePeriodProperty() { return cumulativePeriod.getReadOnlyProperty(); }
    void setCumulativePeriod(Duration value) { // package private for testing
        // Make sure any null value is turned into ZERO
        Duration newValue = value == null || value.toMillis() < 0 ? Duration.ZERO : value;
        // Cap the newValue based on the maximumCumulativePeriod.
        Duration maxPeriod = maximumCumulativePeriod.get();
        if (maxPeriod != null && !maxPeriod.isUnknown() && !newValue.isUnknown()) {
            if (maxPeriod.toMillis() < 0) {
                newValue = Duration.ZERO;
            } else if (!maxPeriod.isIndefinite() && newValue.greaterThan(maxPeriod)) {
                newValue = maxPeriod;
            }
        }
        cumulativePeriod.set(newValue);
    }

    /**
     * The maximum allowed value for the cumulativePeriod. Setting this value will help ensure that in the case of
     * repeated failures the back-off algorithm doesn't end up producing unreasonably large values for
     * cumulative period. The cumulative period is guaranteed not to be any larger than this value. If the
     * maximumCumulativePeriod is negative, then cumulativePeriod will be capped at 0. If maximumCumulativePeriod
     * is NaN or null, then it will not influence the cumulativePeriod.
     */
    private ObjectProperty<Duration> maximumCumulativePeriod = new SimpleObjectProperty<>(this, "maximumCumulativePeriod", Duration.INDEFINITE);
    public final Duration getMaximumCumulativePeriod() { return maximumCumulativePeriod.get(); }
    public final void setMaximumCumulativePeriod(Duration value) { maximumCumulativePeriod.set(value); }
    public final ObjectProperty<Duration> maximumCumulativePeriodProperty() { return maximumCumulativePeriod; }

    /**
     * The last successfully computed value. During each iteration, the "value" of the ScheduledService will be
     * reset to null, as with any other Service. The "lastValue" however will be set to the most recently
     * successfully computed value, even across iterations. It is reset however whenever you manually call
     * reset or restart.
     */
    private ReadOnlyObjectWrapper<V> lastValue = new ReadOnlyObjectWrapper<>(this, "lastValue", null);
    public final V getLastValue() { return lastValue.get(); }
    public final ReadOnlyObjectProperty<V> lastValueProperty() { return lastValue.getReadOnlyProperty(); }

    /**
     * The timestamp of the last time the task was run. This is used to compute the amount
     * of delay between successive iterations by taking the cumulativePeriod into account.
     */
    private long lastRunTime = 0L;

    /**
     * Whether or not this iteration is a "fresh start", such as the initial call to start,
     * or a call to restart, or a call to reset followed by a call to start.
     */
    private boolean freshStart = true;

    /**
     * This is a TimerTask scheduled with the DELAY_TIMER. All it does is kick off the execution
     * of the actual background Task.
     */
    private TimerTask delayTask = null;

    /**
     * This is set to false when the "cancel" method is called, and reset to true on "reset".
     * We need this so that any time the developer calls 'cancel', even when from within one
     * of the event handlers, it will cause us to transition to the cancelled state.
     */
    private boolean stop = false;

    // This method is invoked by Service to actually execute the task. In the normal implementation
    // in Service, this method will simply delegate to the Executor. In ScheduledService, however,
    // we instead will delay the correct amount of time before we finally invoke executeTaskNow,
    // which is where we end up delegating to the executor.
    @Override protected void executeTask(final Task<V> task) {
        assert task != null;
        checkThread();

        if (freshStart) {
            // The delayTask should have concluded and been made null by this point.
            // If not, then somehow we were paused waiting for another iteration and
            // somebody caused the system to run again. However resetting things should
            // have cleared the delayTask.
            assert delayTask == null;

            // The cumulativePeriod needs to be initialized
            setCumulativePeriod(getPeriod());

            // Pause for the "delay" amount of time and then execute
            final long d = (long) normalize(getDelay());
            if (d == 0) {
                // If the delay is zero or null, then just start immediately
                executeTaskNow(task);
            } else {
                schedule(delayTask = createTimerTask(task), d);
            }
        } else {
            // We are executing as a result of an iteration, not a fresh start.
            // If the runPeriod (time between the last run and now) exceeds the cumulativePeriod, then
            // we need to execute immediately. Otherwise, we will pause until the cumulativePeriod has
            // been reached, and then run.
            double cumulative = normalize(getCumulativePeriod()); // Can never be null.
            double runPeriod = clock() - lastRunTime;
            if (runPeriod < cumulative) {
                // Pause and then execute
                assert delayTask == null;
                schedule(delayTask = createTimerTask(task), (long) (cumulative - runPeriod));
            } else {
                // Execute immediately
                executeTaskNow(task);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Implementation Note: Subclasses which override this method must call this super implementation.
     */
    @Override protected void succeeded() {
        super.succeeded();
        lastValue.set(getValue());
        // Reset the cumulative time
        Duration d = getPeriod();
        setCumulativePeriod(d);
        // Have to save this off, since it will be reset here in a second
        final boolean wasCancelled = stop;
        // Call the super implementation of reset, which will not cause us
        // to think this is a new fresh start.
        superReset();
        assert freshStart == false;
        // If it was cancelled then we will progress from READY to SCHEDULED to CANCELLED so that
        // the lifecycle changes are predictable according to the Service specification.
        if (wasCancelled) {
            cancelFromReadyState();
        } else {
            // Fire it up!
            start();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Implementation Note: Subclasses which override this method must call this super implementation.
     */
    @Override protected void failed() {
        super.failed();
        assert delayTask == null;
        // Restart as necessary
        setCurrentFailureCount(getCurrentFailureCount() + 1);
        if (getRestartOnFailure() && getMaximumFailureCount() > getCurrentFailureCount()) {
            // We've not yet maxed out the number of failures we can
            // encounter, so we're going to iterate
            Callback<ScheduledService<?>,Duration> func = getBackoffStrategy();
            if (func != null) {
                Duration d = func.call(this);
                setCumulativePeriod(d);
            }

            superReset();
            assert freshStart == false;
            start();
        } else {
            // We've maxed out, so do nothing and things will just stop.
        }
    }

    /**
     * {@inheritDoc}
     *
     * Implementation Note: Subclasses which override this method must call this super implementation.
     */
    @Override public void reset() {
        super.reset();
        stop = false;
        setCumulativePeriod(getPeriod());
        lastValue.set(null);
        setCurrentFailureCount(0);
        lastRunTime = 0L;
        freshStart = true;
    }

    /**
     * Cancels any currently running task and stops this scheduled service, such that
     * no additional iterations will occur.
     *
     * @return whether any running task was cancelled, false if no task was cancelled.
     *         In any case, the ScheduledService will stop iterating.
     */
    @Override public boolean cancel() {
        boolean ret = super.cancel();
        stop = true;
        if (delayTask != null) {
            delayTask.cancel();
            delayTask = null;
        }
        return ret;
    }

    /**
     * This method exists only for testing purposes. The normal implementation
     * will delegate to a java.util.Timer, however during testing we want to simply
     * inspect the value for the delay and execute immediately.
     * @param task not null
     * @param delay &gt;= 0
     */
    void schedule(TimerTask task, long delay) {
        DELAY_TIMER.schedule(task, delay);
    }

    /**
     * This method only exists for the sake of testing.
     * @return freshStart
     */
    boolean isFreshStart() { return freshStart; }

    /**
     * Gets the time of the current clock. At runtime this is simply getting the results
     * of System.currentTimeMillis, however during testing this is hammered so as to return
     * a time that works well during testing.
     * @return The clock time
     */
    long clock() {
        return System.currentTimeMillis();
    }

    /**
     * Called by this class when we need to avoid calling this class' implementation of
     * reset which has the side effect of resetting the "freshStart", currentFailureCount,
     * and other state.
     */
    private void superReset() {
        super.reset();
    }

    /**
     * Creates the TimerTask used for delaying execution. The delay can either be due to
     * the initial delay (if this is a freshStart), or it can be the computed delay in order
     * to execute the task on its fixed schedule.
     *
     * @param task must not be null.
     * @return the delay TimerTask.
     */
    private TimerTask createTimerTask(final Task<V> task) {
        assert task != null;
        return new TimerTask() {
            @Override public void run() {
                Runnable r = () -> {
                    executeTaskNow(task);
                    delayTask = null;
                };

                // We must make sure that executeTaskNow is called from the FX thread.
                // This must happen on th FX thread because the super implementation of
                // executeTask is going to call getExecutor so it can use any user supplied
                // executor, and this property can only be read on the FX thread.
                if (isFxApplicationThread()) {
                    r.run();
                } else {
                    runLater(r);
                }
            }
        };
    }

    /**
     * Called when it is time to actually execute the task (any delay has by now been
     * accounted for). Essentially this ends up simply calling the super implementation
     * of executeTask and doing some bookkeeping.
     *
     * @param task must not be null
     */
    private void executeTaskNow(Task<V> task) {
        assert task != null;
        lastRunTime = clock();
        freshStart = false;
        super.executeTask(task);
    }

    /**
     * Normalize our handling of Durations according to the class documentation.
     * @param d can be null
     * @return a double representing the millis.
     */
    private static double normalize(Duration d) {
        if (d == null || d.isUnknown()) return 0;
        if (d.isIndefinite()) return Double.MAX_VALUE;
        return d.toMillis();
    }
}
