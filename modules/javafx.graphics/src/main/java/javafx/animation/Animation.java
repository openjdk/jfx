/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import java.util.Objects;

import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoublePropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import com.sun.javafx.animation.TickCalculation;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.shared.ClipEnvelope;
import com.sun.scenario.animation.shared.PulseReceiver;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The class {@code Animation} provides the core functionality of all animations
 * used in the JavaFX runtime.
 * <p>
 * An animation can run in a loop by setting {@link #cycleCountProperty() cycleCount}.
 * To make an animation run back and forth while looping, set the
 * {@link #autoReverseProperty() autoReverse} -flag.
 * <p>
 * Call {@link #play()} or {@link #playFromStart()} to play an {@code Animation}
 * . The {@code Animation} progresses in the direction and speed specified by
 * {@link #rateProperty() rate}, and stops when its duration is elapsed. An {@code Animation}
 * with indefinite duration (a {@link #cycleCountProperty() cycleCount} of {@link #INDEFINITE}) runs
 * repeatedly until the {@link #stop()} method is explicitly called, which will
 * stop the running {@code Animation} and reset its play head to the initial
 * position.
 * <p>
 * An {@code Animation} can be paused by calling {@link #pause()}, and the next
 * {@link #play()} call will resume the {@code Animation} from where it was
 * paused.
 * <p>
 * An {@code Animation}'s play head can be randomly positioned, whether it is
 * running or not. If the {@code Animation} is running, the play head jumps to
 * the specified position immediately and continues playing from new position.
 * If the {@code Animation} is not running, the next {@link #play()} will start
 * the {@code Animation} from the specified position.
 * <p>
 * Inverting the value of {@link #rateProperty() rate} toggles the play direction.
 *
 * @see Timeline
 * @see Transition
 *
 * @since JavaFX 2.0
 */
public abstract class Animation {

    static {
        AnimationAccessorImpl.DEFAULT = new AnimationAccessorImpl();
    }

    /**
     * Used as a  value for {@link #cycleCountProperty() cycleCount} to specify an animation that repeats indefinitely,
     * until the {@code stop()} method is called.
     */
    public static final int INDEFINITE = -1;

    /**
     * The possible states for {@link Animation#statusProperty() status}.
     * @since JavaFX 2.0
     */
    public static enum Status {
        /**
         * The paused state.
         */
        PAUSED,
        /**
         * The running state.
         */
        RUNNING,
        /**
         * The stopped state.
         */
        STOPPED
    }

    private static final double EPSILON = 1e-12;

    /**
     * Checks if the rate is effectively 0.
     * @param rate
     * @return true i.f.f. abs(rate) < EPSILON
     */
    static final boolean isNearZero(double rate) {
        return Math.abs(rate) < EPSILON;
    }

    /**
     * Checks if 2 rates are effectively equal.
     * @param rateMagnitude
     * @return true i.f.f. Math.abs(rate1 - rate2) < EPSILON
     */
    private static boolean areNearEqual(double rate1, double rate2) {
        return isNearZero(rate2 - rate1);
    }

    /*
        These four fields and associated methods were moved here from AnimationPulseReceiver
        when that class was removed. They could probably be integrated much cleaner into Animation,
        but to make sure the change was made without introducing regressions, this code was
        moved pretty much verbatim.
     */
    private long startTime;
    private long pauseTime;
    private boolean paused = false;
    private final AbstractPrimaryTimer timer;

    // Access control context, captured whenever we add this pulse receiver to
    // the PrimaryTimer (which is called when an animation is played or resumed)
    @SuppressWarnings("removal")
    private AccessControlContext accessCtrlCtx = null;

    private long now() {
        return TickCalculation.fromNano(timer.nanos());
    }

    @SuppressWarnings("removal")
    private void addPulseReceiver() {
        // Capture the Access Control Context to be used during the animation pulse
        accessCtrlCtx = AccessController.getContext();

        timer.addPulseReceiver(pulseReceiver);
    }

    void startReceiver(long delay) {
        paused = false;
        startTime = now() + delay;
        addPulseReceiver();
    }

    void pauseReceiver() {
        if (!paused) {
            pauseTime = now();
            paused = true;
            timer.removePulseReceiver(pulseReceiver);
        }
    }

    void resumeReceiver() {
        if (paused) {
            final long deltaTime = now() - pauseTime;
            startTime += deltaTime;
            paused = false;
            addPulseReceiver();
        }
    }

    // package private only for the sake of testing
    final PulseReceiver pulseReceiver = new PulseReceiver() {
        @SuppressWarnings("removal")
        @Override public void timePulse(long now) {
            final long elapsedTime = now - startTime;
            if (elapsedTime < 0) {
                return;
            }
            if (accessCtrlCtx == null) {
                throw new IllegalStateException("Error: AccessControlContext not captured");
            }

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                doTimePulse(elapsedTime);
                return null;
            }, accessCtrlCtx);
        }
    };

    private class CurrentRateProperty extends ReadOnlyDoublePropertyBase {
        private double value;

        @Override
        public Object getBean() {
            return Animation.this;
        }

        @Override
        public String getName() {
            return "currentRate";
        }

        @Override
        public double get() {
            return value;
        }

        private void set(double value) {
            this.value = value;
            fireValueChangedEvent();
        }
    }

    private class AnimationReadOnlyProperty<T> extends ReadOnlyObjectPropertyBase<T> {

        private final String name;
        private T value;

        private AnimationReadOnlyProperty(String name, T value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Object getBean() {
            return Animation.this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T get() {
            return value;
        }

        private void set(T value) {
            this.value = value;
            fireValueChangedEvent();
        }
    }

    /**
     * The parent of this {@code Animation}. If this animation has not been
     * added to another animation, such as {@link ParallelTransition} and
     * {@link SequentialTransition}, then parent will be null.
     *
     * @defaultValue null
     */
    Animation parent = null;

    /**
     * The type of ClipEnvelope for the animation is determined by its cycleCount and cycleDuration
     * and is updated when these values change.
     * <p>
     * Package-private for testing purposes
    */
    ClipEnvelope clipEnvelope;

    private boolean lastPlayedFinished = true;

    private boolean lastPlayedForward = true;
    /**
     * Defines the direction/speed at which the {@code Animation} is expected to
     * be played.
     * <p>
     * The absolute value of {@code rate} indicates the speed at which the
     * {@code Animation} is to be played, while the sign of {@code rate}
     * indicates the direction. A positive value of {@code rate} indicates
     * forward play, a negative value indicates backward play and {@code 0.0} to
     * stop a running {@code Animation}.
     * <p>
     * Rate {@code 1.0} is normal play, {@code 2.0} is 2 time normal,
     * {@code -1.0} is backwards, etc.
     *
     * <p>
     * Inverting the rate of a running {@code Animation} will cause the
     * {@code Animation} to reverse direction in place and play back over the
     * portion of the {@code Animation} that has already elapsed.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty rate;
    private static final double DEFAULT_RATE = 1.0;

    public final void setRate(double value) {
        if (rate != null || !areNearEqual(value, DEFAULT_RATE)) {
            rateProperty().set(value);
        }
    }

    public final double getRate() {
        return (rate == null)? DEFAULT_RATE : rate.get();
    }

    public final DoubleProperty rateProperty() {
        if (rate == null) {
            rate = new DoublePropertyBase(DEFAULT_RATE) {

                @Override
                public void invalidated() {
                    final double newRate = getRate();
                    if (isRunningEmbedded()) {
                        if (isBound()) {
                            unbind();
                        }
                        set(oldRate);
                        throw new IllegalArgumentException("Cannot set rate of embedded animation while running.");
                    }
                    if (isNearZero(newRate)) {
                        if (isRunning()) {
                            lastPlayedForward = areNearEqual(getCurrentRate(), oldRate);
                        }
                        doSetCurrentRate(0.0);
                        pauseReceiver();
                    } else {
                        if (isRunning()) {
                            final double currentRate = getCurrentRate();
                            if (isNearZero(currentRate)) {
                                doSetCurrentRate(lastPlayedForward ? newRate : -newRate);
                                resumeReceiver();
                            } else {
                                final boolean playingForward = areNearEqual(currentRate, oldRate);
                                doSetCurrentRate(playingForward ? newRate : -newRate);
                            }
                        }
                        oldRate = newRate;
                    }
                    clipEnvelope.setRate(newRate);
                }

                @Override
                public Object getBean() {
                    return Animation.this;
                }

                @Override
                public String getName() {
                    return "rate";
                }
            };
        }
        return rate;
    }

    private boolean isRunningEmbedded() {
        if (parent == null) {
            return false;
        }
        return !parent.isStopped() || parent.isRunningEmbedded();
    }

    private double oldRate = 1.0;
    /**
     * Read-only variable to indicate current direction/speed at which the
     * {@code Animation} is being played.
     * <p>
     * {@code currentRate} is not necessarily equal to {@code rate}.
     * {@code currentRate} is set to {@code 0.0} when animation is paused or
     * stopped. {@code currentRate} may also point in the opposite direction of {@code rate} during
     * reverse cycles when {@code autoReverse} is {@code true}.
     *
     * @defaultValue 0.0
     */
    private ReadOnlyDoubleProperty currentRate;
    private static final double DEFAULT_CURRENT_RATE = 0.0;

    public final double getCurrentRate() {
        return (currentRate == null) ? DEFAULT_CURRENT_RATE : currentRate.get();
    }

    public final ReadOnlyDoubleProperty currentRateProperty() {
        if (currentRate == null) {
            currentRate = new CurrentRateProperty();
        }
        return currentRate;
    }

    void setCurrentRate(double currentRate) {
//      if (getStatus() == Status.RUNNING) {
          doSetCurrentRate(currentRate);
//      }
  }

    /**
     * The current rate changes in 3 cases:
     * <ol>
     * <li> When the rate is changed.
     * <li> When the status is changed (paused/stopped/resumed/started).
     * <li> When switching between a forwards and backwards cycle.
     * </ol>
     *
     * 1 happens when the user changes the rate of the animation or its root parent.
     * 2 happens when the user changes the status or when the animation is finished.
     * 3 happens when the clip envelope flips the rate when the cycle is alternated, through the accessor
     *
     * @param value the value of the new current rate
     */
    private void doSetCurrentRate(double value) {
        if (currentRate != null || !areNearEqual(value, DEFAULT_CURRENT_RATE)) {
            ((CurrentRateProperty) currentRateProperty()).set(value);
        }
    }

    /**
     * Read-only variable to indicate the duration of one cycle of this
     * {@code Animation}: the time it takes to play from time 0 to the
     * end of the Animation (at the default {@code rate} of
     * 1.0).
     *
     * @defaultValue 0ms
     */
    private ReadOnlyObjectProperty<Duration> cycleDuration;
    private static final Duration DEFAULT_CYCLE_DURATION = Duration.ZERO;

    protected final void setCycleDuration(Duration value) {
        if (cycleDuration != null || !DEFAULT_CYCLE_DURATION.equals(value)) {
            if (value.lessThan(Duration.ZERO)) {
                throw new IllegalArgumentException("Cycle duration cannot be negative");
            }
            ((AnimationReadOnlyProperty<Duration>) cycleDurationProperty()).set(value);
            updateTotalDuration();
        }
    }

    public final Duration getCycleDuration() {
        return (cycleDuration == null) ? DEFAULT_CYCLE_DURATION : cycleDuration.get();
    }

    public final ReadOnlyObjectProperty<Duration> cycleDurationProperty() {
        if (cycleDuration == null) {
            cycleDuration = new AnimationReadOnlyProperty<>("cycleDuration", DEFAULT_CYCLE_DURATION);
        }
        return cycleDuration;
    }

    /**
     * Read-only variable to indicate the total duration of this
     * {@code Animation}, including repeats. An {@code Animation} with a {@code cycleCount}
     * of {@code Animation.INDEFINITE} will have a {@code totalDuration} of
     * {@code Duration.INDEFINITE}.
     *
     * <p>
     * This is set to cycleDuration * cycleCount.
     *
     * @defaultValue 0ms
     */
    private ReadOnlyObjectProperty<Duration> totalDuration;
    private static final Duration DEFAULT_TOTAL_DURATION = Duration.ZERO;

    public final Duration getTotalDuration() {
        return (totalDuration == null) ? DEFAULT_TOTAL_DURATION : totalDuration.get();
    }

    public final ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        if (totalDuration == null) {
            totalDuration = new AnimationReadOnlyProperty<>("totalDuration", DEFAULT_TOTAL_DURATION);
        }
        return totalDuration;
    }

    private void updateTotalDuration() {
        // Implementing the bind eagerly, because cycleCount and
        // cycleDuration should not change that often
        final int cycleCount = getCycleCount();
        final Duration cycleDuration = getCycleDuration();
        final Duration newTotalDuration;
        if (Duration.ZERO.equals(cycleDuration)) newTotalDuration = Duration.ZERO;
        else if (cycleCount == INDEFINITE) newTotalDuration = Duration.INDEFINITE;
        else if (cycleCount <= 1) newTotalDuration = cycleDuration;
        else newTotalDuration = cycleDuration.multiply(cycleCount);
        if (totalDuration != null || !DEFAULT_TOTAL_DURATION.equals(newTotalDuration)) {
            ((AnimationReadOnlyProperty<Duration>) totalDurationProperty()).set(newTotalDuration);
        }
        if (isStopped()) {
            syncClipEnvelope();
            if (newTotalDuration.lessThan(getCurrentTime())) {
                clipEnvelope.jumpTo(TickCalculation.fromDuration(newTotalDuration));
            }
        }
    }

    /**
     * Defines the {@code Animation}'s play head position.
     *
     * @defaultValue 0ms
     */
    private CurrentTimeProperty currentTime;
    private long currentTicks;
    private class CurrentTimeProperty extends ReadOnlyObjectPropertyBase<Duration> {

        @Override
        public Object getBean() {
            return Animation.this;
        }

        @Override
        public String getName() {
            return "currentTime";
        }

        @Override
        public Duration get() {
            return getCurrentTime();
        }

        @Override
        public void fireValueChangedEvent() {
            super.fireValueChangedEvent();
        }

    }

    public final Duration getCurrentTime() {
        return TickCalculation.toDuration(currentTicks);
    }

    public final ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        if (currentTime == null) {
            currentTime = new CurrentTimeProperty();
        }
        return currentTime;
    }

    /**
     * Delays the start of an animation.
     *
     * Cannot be negative. Setting to a negative number will result in {@link IllegalArgumentException}.
     *
     * @defaultValue 0ms
     */
    private ObjectProperty<Duration> delay;
    private static final Duration DEFAULT_DELAY = Duration.ZERO;

    public final void setDelay(Duration value) {
        if (delay != null || !DEFAULT_DELAY.equals(value)) {
            delayProperty().set(value);
        }
    }

    public final Duration getDelay() {
        return (delay == null) ? DEFAULT_DELAY : delay.get();
    }

    public final ObjectProperty<Duration> delayProperty() {
        if (delay == null) {
            delay = new ObjectPropertyBase<>(DEFAULT_DELAY) {

                @Override
                protected void invalidated() {
                    final Duration newDuration = get();
                    if (newDuration.lessThan(Duration.ZERO)) {
                        if (isBound()) {
                            unbind();
                        }
                        set(Duration.ZERO);
                        throw new IllegalArgumentException("Cannot set delay to negative value. Setting to Duration.ZERO");
                    }
                }

                @Override
                public Object getBean() {
                    return Animation.this;
                }

                @Override
                public String getName() {
                    return "delay";
                }
            };
        }
        return delay;
    }

    /**
     * Defines the number of cycles in this animation. The {@code cycleCount}
     * may be {@code INDEFINITE} for animations that repeat indefinitely, but
     * must otherwise be {@literal >} 0.
     * <p>
     * It is not possible to change the {@code cycleCount} of a running
     * {@code Animation}. If the value of {@code cycleCount} is changed for a
     * running {@code Animation}, the animation has to be stopped and started again to pick
     * up the new value.
     *
     * @defaultValue 1
     */
    private IntegerProperty cycleCount;
    private static final int DEFAULT_CYCLE_COUNT = 1;

    public final void setCycleCount(int value) {
        if (cycleCount != null || value != DEFAULT_CYCLE_COUNT) {
            cycleCountProperty().set(value);
        }
    }

    public final int getCycleCount() {
        return (cycleCount == null) ? DEFAULT_CYCLE_COUNT : cycleCount.get();
    }

    public final IntegerProperty cycleCountProperty() {
        if (cycleCount == null) {
            cycleCount = new IntegerPropertyBase(DEFAULT_CYCLE_COUNT) {

                @Override
                public void invalidated() {
                    updateTotalDuration();
                }

                @Override
                public Object getBean() {
                    return Animation.this;
                }

                @Override
                public String getName() {
                    return "cycleCount";
                }
            };
        }
        return cycleCount;
    }

    /**
     * Defines whether this
     * {@code Animation} reverses direction on alternating cycles. If
     * {@code true}, the
     * {@code Animation} will proceed forward on the first cycle,
     * then reverses on the second cycle, and so on. Otherwise, animation will
     * loop such that each cycle proceeds forward from the start.
     *
     * It is not possible to change the {@code autoReverse} flag of a running
     * {@code Animation}. If the value of {@code autoReverse} is changed for a
     * running {@code Animation}, the animation has to be stopped and started again to pick
     * up the new value.
     *
     * @defaultValue false
     */
    private BooleanProperty autoReverse;
    private static final boolean DEFAULT_AUTO_REVERSE = false;

    public final void setAutoReverse(boolean value) {
        if (autoReverse != null || value != DEFAULT_AUTO_REVERSE) {
            autoReverseProperty().set(value);
        }
    }

    public final boolean isAutoReverse() {
        return (autoReverse == null) ? DEFAULT_AUTO_REVERSE : autoReverse.get();
    }

    public final BooleanProperty autoReverseProperty() {
        if (autoReverse == null) {
            autoReverse = new BooleanPropertyBase(DEFAULT_AUTO_REVERSE) {

                @Override
                public Object getBean() {
                    return Animation.this;
                }

                @Override
                public String getName() {
                    return "autoReverse";
                }
            };
        }
        return autoReverse;
    }

    /**
     * The status of the {@code Animation}.
     *
     * An {@code Animation} can be in one of three states:
     * {@link Status#STOPPED}, {@link Status#PAUSED} or {@link Status#RUNNING}.
     */
    private ReadOnlyObjectProperty<Status> status;
    private static final Status DEFAULT_STATUS = Status.STOPPED;

    protected final void setStatus(Status value) {
        if (status != null || !DEFAULT_STATUS.equals(value)) {
            ((AnimationReadOnlyProperty<Status>) statusProperty()).set(value);
        }
    }

    public final Status getStatus() {
        return (status == null) ? DEFAULT_STATUS : status.get();
    }

    public final ReadOnlyObjectProperty<Status> statusProperty() {
        if (status == null) {
            status = new AnimationReadOnlyProperty<>("status", Status.STOPPED);
        }
        return status;
    }

    boolean isStopped() {
        return getStatus() == Status.STOPPED;
    }

    boolean isPaused() {
        return getStatus() == Status.PAUSED;
    }

    boolean isRunning() {
        return getStatus() == Status.RUNNING;
    }

    /**
     * The action to be executed at the conclusion of this {@code Animation}.
     *
     * @defaultValue null
     */
    private ObjectProperty<EventHandler<ActionEvent>> onFinished;
    private static final EventHandler<ActionEvent> DEFAULT_ON_FINISHED = null;

    public final void setOnFinished(EventHandler<ActionEvent> value) {
        if (onFinished != null || value != DEFAULT_ON_FINISHED) {
            onFinishedProperty().set(value);
        }
    }

    public final EventHandler<ActionEvent> getOnFinished() {
        return (onFinished == null) ? DEFAULT_ON_FINISHED : onFinished.get();
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onFinishedProperty() {
        if (onFinished == null) {
            onFinished = new ObjectPropertyBase<>(DEFAULT_ON_FINISHED) {

                @Override
                public Object getBean() {
                    return Animation.this;
                }

                @Override
                public String getName() {
                    return "onFinished";
                }
            };
        }
        return onFinished;
    }

    private ObservableMap<String, Duration> cuePoints;

    /**
     * The cue points can be
     * used to mark important positions of the {@code Animation}. Once a cue
     * point was defined, it can be used as an argument of
     * {@link #jumpTo(String) jumpTo()} and {@link #playFrom(String) playFrom()}
     * to move to the associated position quickly.
     * <p>
     * Every {@code Animation} has two predefined cue points {@code "start"} and
     * {@code "end"}, which are set at the start respectively the end of the
     * {@code Animation}. The predefined cuepoints do not appear in the map,
     * attempts to override them have no effect.
     * <p>
     * Another option to define a cue point in a {@code Animation} is to set the
     * {@link KeyFrame#getName() name} property of a {@link KeyFrame}.
     *
     * @return {@link javafx.collections.ObservableMap} of cue points
     */
    public final ObservableMap<String, Duration> getCuePoints() {
        if (cuePoints == null) {
            cuePoints = FXCollections.observableHashMap();
        }
        return cuePoints;
    }

    /**
     * Jumps to a given position in this {@code Animation}.
     *
     * If the given time is less than {@link Duration#ZERO}, this method will
     * jump to the start of the animation. If the given time is larger than the
     * duration of this {@code Animation}, this method will jump to the end.
     *
     * @param time
     *            the new position
     * @throws NullPointerException
     *             if {@code time} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code time} is {@link Duration#UNKNOWN}
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void jumpTo(Duration time) {
        Objects.requireNonNull(time, "Time needs to be specified");
        if (time.isUnknown()) {
            throw new IllegalArgumentException("The time is invalid");
        }
        if (parent != null) {
            throw new IllegalStateException("Cannot jump when embedded in another animation");
        }

        lastPlayedFinished = false;

        double millis = time.isIndefinite() ? getCycleDuration().toMillis() :
            Utils.clamp(0, time.toMillis(), getTotalDuration().toMillis());
        long ticks = TickCalculation.fromMillis(millis);

        if (isStopped()) {
            syncClipEnvelope();
        }
        clipEnvelope.jumpTo(ticks);
    }

    /**
     * Jumps to a predefined position in this {@code Animation}. This method
     * looks for an entry in cue points and jumps to the associated
     * position, if it finds one.
     * <p>
     * If the cue point is behind the end of this {@code Animation}, calling
     * {@code jumpTo} will result in a jump to the end. If the cue point has a
     * negative {@link javafx.util.Duration} it will result in a jump to the
     * beginning. If the cue point has a value of
     * {@link javafx.util.Duration#UNKNOWN} calling {@code jumpTo} will have no
     * effect for this cue point.
     * <p>
     * There are two predefined cue points {@code "start"} and {@code "end"}
     * which are defined to be at the start respectively the end of this
     * {@code Animation}.
     *
     * @param cuePoint
     *            the name of the cue point
     * @throws NullPointerException
     *             if {@code cuePoint} is {@code null}
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     * @see #getCuePoints()
     */
    public void jumpTo(String cuePoint) {
        Objects.requireNonNull(cuePoint, "CuePoint needs to be specified");
        if ("start".equalsIgnoreCase(cuePoint)) {
            jumpTo(Duration.ZERO);
        } else if ("end".equalsIgnoreCase(cuePoint)) {
            jumpTo(getTotalDuration());
        } else {
            final Duration target = getCuePoints().get(cuePoint);
            if (target != null) {
                jumpTo(target);
            }
        }
    }

    /**
     * A convenience method to play this {@code Animation} from a predefined
     * position. The position has to be predefined in cue points.
     * Calling this method is equivalent to
     *
     * <pre>
     * <code>
     * animation.jumpTo(cuePoint);
     * animation.play();
     * </code>
     * </pre>
     *
     * Note that unlike {@link #playFromStart()} calling this method will not
     * change the playing direction of this {@code Animation}.
     *
     * @param cuePoint
     *            name of the cue point
     * @throws NullPointerException
     *             if {@code cuePoint} is {@code null}
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     * @see #getCuePoints()
     */
    public void playFrom(String cuePoint) {
        jumpTo(cuePoint);
        play();
    }

    /**
     * A convenience method to play this {@code Animation} from a specific
     * position. Calling this method is equivalent to
     *
     * <pre>
     * <code>
     * animation.jumpTo(time);
     * animation.play();
     * </code>
     * </pre>
     *
     * Note that unlike {@link #playFromStart()} calling this method will not
     * change the playing direction of this {@code Animation}.
     *
     * @param time
     *            position where to play from
     * @throws NullPointerException
     *             if {@code time} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code time} is {@link Duration#UNKNOWN}
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void playFrom(Duration time) {
        jumpTo(time);
        play();
    }

    /**
     * Plays an {@code Animation} from initial position in forward direction.
     * <p>
     * It is equivalent to
     * <p>
     * <code>
     *      animation.stop();<br>
     *      animation.setRate = setRate(Math.abs(animation.getRate())); <br>
     *      animation.jumpTo(Duration.ZERO);<br>
     *      animation.play();<br>
     *  </code>
     *
     * <p>
     * Note: <ul>
     * <li>{@code playFromStart()} is an asynchronous call, {@code Animation} may
     * not start immediately. </ul>
     *
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void playFromStart() {
        stop();
        setRate(Math.abs(getRate()));
        jumpTo(Duration.ZERO);
        play();
    }

    /**
     * Plays {@code Animation} from current position in the direction indicated
     * by {@code rate}. If the {@code Animation} is running, it has no effect.
     * <p>
     * When {@code rate} {@literal >} 0 (forward play), if an {@code Animation} is already
     * positioned at the end, the first cycle will not be played, it is
     * considered to have already finished. This also applies to a backward (
     * {@code rate} {@literal <} 0) cycle if an {@code Animation} is positioned at the beginning.
     * However, if the {@code Animation} has {@code cycleCount} {@literal >} 1, following
     * cycle(s) will be played as usual.
     * <p>
     * When the {@code Animation} reaches the end, the {@code Animation} is stopped and
     * the play head remains at the end.
     * <p>
     * To play an {@code Animation} backwards from the end:<br>
     * <code>
     *  animation.setRate(negative rate);<br>
     *  animation.jumpTo(overall duration of animation);<br>
     *  animation.play();<br>
     * </code>
     * <p>
     * Note: <ul>
     * <li>{@code play()} is an asynchronous call, the {@code Animation} may not
     * start immediately. </ul>
     *
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void play() {
        if (parent != null) {
            throw new IllegalStateException("Cannot start when embedded in another animation");
        }
        switch (getStatus()) {
            case STOPPED:
                if (startable(true)) {
                    final double rate = getRate();
                    if (lastPlayedFinished) {
                        jumpTo(rate < 0 ? getTotalDuration() : Duration.ZERO);
                    }
                    doStart(true);
                    startReceiver(TickCalculation.fromDuration(getDelay()));
                    if (isNearZero(rate)) {
                        pauseReceiver();
                    } else {

                    }
                } else {
                    runHandler(getOnFinished());
                }
                break;
            case PAUSED:
                doResume();
                if (!isNearZero(getRate())) {
                    resumeReceiver();
                }
                break;
            case RUNNING: // no-op
        }
    }

    void doStart(boolean forceSync) {
        sync(forceSync);
        setStatus(Status.RUNNING);
        clipEnvelope.start();
        doSetCurrentRate(clipEnvelope.getCurrentRate());
        lastPulse = 0;
    }

    void doResume() {
        setStatus(Status.RUNNING);
        doSetCurrentRate(lastPlayedForward ? getRate() : -getRate());
    }

    /**
     * Stops the animation and resets the play head to its initial position. If
     * the animation is already stopped, this method has no effect.
     * <p>
     * Note: <ul>
     * <li>{@code stop()} is an asynchronous call, the {@code Animation} may not stop
     * immediately. </ul>
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void stop() {
        if (parent != null) {
            throw new IllegalStateException("Cannot stop when embedded in another animation");
        }
        if (!isStopped()) {
            clipEnvelope.abortCurrentPulse();
            doStop();
            jumpTo(Duration.ZERO);
            lastPlayedFinished = true;
        }
    }

    void doStop() {
        if (!paused) {
            timer.removePulseReceiver(pulseReceiver);
        }
        setStatus(Status.STOPPED);
        doSetCurrentRate(0.0);
    }

    /**
     * Pauses the animation. If the animation is not currently running, this
     * method has no effect.
     * <p>
     * Note: <ul>
     * <li>{@code pause()} is an asynchronous call, the {@code Animation} may not pause
     * immediately. </ul>
     * @throws IllegalStateException
     *             if embedded in another animation,
     *                such as {@link SequentialTransition} or {@link ParallelTransition}
     */
    public void pause() {
        if (parent != null) {
            throw new IllegalStateException("Cannot pause when embedded in another animation");
        }
        if (isRunning()) {
            clipEnvelope.abortCurrentPulse();
            pauseReceiver();
            doPause();
        }
    }

    void doPause() {
        final double currentRate = getCurrentRate();
        if (!isNearZero(currentRate)) {
            lastPlayedForward = areNearEqual(getCurrentRate(), getRate());
        }
        doSetCurrentRate(0.0);
        setStatus(Status.PAUSED);
    }

    final void finished() {
        lastPlayedFinished = true;
        doStop();
        runHandler(getOnFinished());
    }

    void runHandler(EventHandler<ActionEvent> handler) {
        if (handler != null) {
            try {
                handler.handle(new ActionEvent(this, null));
            } catch (Exception ex) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
            }
        }
    }

    private final double targetFramerate;
    private final int resolution;
    private long lastPulse;

    /**
     * The target framerate is the maximum framerate at which this {@code Animation}
     * will run, in frames per second. This can be used, for example, to keep
     * particularly complex {@code Animations} from over-consuming system resources.
     * By default, an {@code Animation}'s framerate is not explicitly limited, meaning
     * the {@code Animation} will run at an optimal framerate for the underlying platform.
     *
     * @return the target framerate
     */
    public final double getTargetFramerate() {
        return targetFramerate;
    }

    /**
     * The constructor of {@code Animation}.
     *
     * This constructor allows to define a target framerate.
     *
     * @param targetFramerate
     *            The custom target frame rate for this {@code Animation}
     * @see #getTargetFramerate()
     */
    protected Animation(double targetFramerate) {
        this.targetFramerate = targetFramerate;
        this.resolution = (int) Math.max(1, Math.round(TickCalculation.TICKS_PER_SECOND / targetFramerate));
        this.clipEnvelope = ClipEnvelope.create(this);
        this.timer = Toolkit.getToolkit().getPrimaryTimer();
    }

    /**
     * The constructor of {@code Animation}.
     */
    protected Animation() {
        this.resolution = 1;
        this.targetFramerate = TickCalculation.TICKS_PER_SECOND / Toolkit.getToolkit().getPrimaryTimer().getDefaultResolution();
        this.clipEnvelope = ClipEnvelope.create(this);
        this.timer = Toolkit.getToolkit().getPrimaryTimer();
    }

    // These constructors are only for testing purposes
    Animation(AbstractPrimaryTimer timer) {
        this.resolution = 1;
        this.targetFramerate = TickCalculation.TICKS_PER_SECOND / timer.getDefaultResolution();
        this.clipEnvelope = ClipEnvelope.create(this);
        this.timer = timer;
    }

    // These constructors are only for testing purposes
    Animation(AbstractPrimaryTimer timer, ClipEnvelope clipEnvelope, int resolution) {
        this.resolution = resolution;
        this.targetFramerate = TickCalculation.TICKS_PER_SECOND / resolution;
        this.clipEnvelope = clipEnvelope;
        this.timer = timer;
    }

    boolean startable(boolean forceSync) {
        return (TickCalculation.fromDuration(getCycleDuration()) > 0L) || (!forceSync && clipEnvelope.wasSynched());
    }

    void sync(boolean forceSync) {
        if (forceSync || !clipEnvelope.wasSynched()) {
            syncClipEnvelope();
        }
    }

    private void syncClipEnvelope() {
        final int publicCycleCount = getCycleCount();
        final int internalCycleCount = (publicCycleCount <= 0)
                && (publicCycleCount != INDEFINITE) ? 1 : publicCycleCount;
        clipEnvelope = clipEnvelope.setCycleCount(internalCycleCount);
        clipEnvelope.setCycleDuration(getCycleDuration());
        clipEnvelope.setAutoReverse(isAutoReverse());
    }

    void doTimePulse(long elapsedTime) {
        if (resolution == 1) { // fullspeed
            clipEnvelope.timePulse(elapsedTime);
        } else if (elapsedTime - lastPulse >= resolution) {
            lastPulse = (elapsedTime / resolution) * resolution;
            clipEnvelope.timePulse(elapsedTime);
        }
    }

    abstract void doPlayTo(long currentTicks, long cycleTicks);

    abstract void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump);

    void setCurrentTicks(long ticks) {
        currentTicks = ticks;
        if (currentTime != null) {
            currentTime.fireValueChangedEvent();
        }
    }
}
