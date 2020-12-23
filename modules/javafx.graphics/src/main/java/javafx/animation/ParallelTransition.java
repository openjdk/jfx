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

import com.sun.javafx.animation.TickCalculation;
import static com.sun.javafx.animation.TickCalculation.*;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.util.Duration;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.collections.VetoableListDecorator;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * This {@link Transition} plays a list of {@link javafx.animation.Animation
 * Animations} in parallel.
 * <p>
 * Children of this {@code Transition} inherit {@link #nodeProperty() node}, if their
 * {@code node} property is not specified.
 *
 * <p>
 * Code Segment Example:
 * </p>
 *
 * <pre>
 * <code>
 *     Rectangle rect = new Rectangle (100, 40, 100, 100);
 *     rect.setArcHeight(50);
 *     rect.setArcWidth(50);
 *     rect.setFill(Color.VIOLET);
 *
 *     final Duration SEC_2 = Duration.millis(2000);
 *     final Duration SEC_3 = Duration.millis(3000);
 *
 *     FadeTransition ft = new FadeTransition(SEC_3);
 *     ft.setFromValue(1.0f);
 *     ft.setToValue(0.3f);
 *     ft.setCycleCount(2f);
 *     ft.setAutoReverse(true);
 *     TranslateTransition tt = new TranslateTransition(SEC_2);
 *     tt.setFromX(-100f);
 *     tt.setToX(100f);
 *     tt.setCycleCount(2f);
 *     tt.setAutoReverse(true);
 *     RotateTransition rt = new RotateTransition(SEC_3);
 *     rt.setByAngle(180f);
 *     rt.setCycleCount(4f);
 *     rt.setAutoReverse(true);
 *     ScaleTransition st = new ScaleTransition(SEC_2);
 *     st.setByX(1.5f);
 *     st.setByY(1.5f);
 *     st.setCycleCount(2f);
 *     st.setAutoReverse(true);
 *
 *     ParallelTransition pt = new ParallelTransition(rect, ft, tt, rt, st);
 *     pt.play();
 * </code>
 * </pre>
 *
 * @see Transition
 * @see Animation
 *
 * @since JavaFX 2.0
 */
public final class ParallelTransition extends Transition {

    private static final Animation[] EMPTY_ANIMATION_ARRAY = new Animation[0];
    private static final double EPSILON = 1e-12;

    private Animation[] cachedChildren = EMPTY_ANIMATION_ARRAY;
    private long[] durations;
    private long[] delays;
    private double[] rates;
    private long[] offsetTicks;
    private boolean[] forceChildSync;
    private long oldTicks;
    private long cycleTime;
    private boolean childrenChanged = true;
    private boolean toggledRate;

    private final InvalidationListener childrenListener = observable -> {
        childrenChanged = true;
        if (getStatus() == Status.STOPPED) {
            setCycleDuration(computeCycleDuration());
        }
    };

    private final ChangeListener<Number> rateListener = new ChangeListener<Number>() {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (oldValue.doubleValue() * newValue.doubleValue() < 0) {
                for (int i = 0; i < cachedChildren.length; ++i) {
                    Animation child = cachedChildren[i];
                    child.clipEnvelope.setRate(rates[i] * Math.signum(getCurrentRate()));
                }
                toggledRate = true;
            }
        }

    };
    /**
     * This {@link javafx.scene.Node} is used in all child {@link Transition
     * Transitions}, that do not define a target {@code Node} themselves. This
     * can be used if a number of {@code Transitions} should be applied to a
     * single {@code Node}.
     * <p>
     * It is not possible to change the target {@code node} of a running
     * {@code Transition}. If the value of {@code node} is changed for a running
     * {@code Transition}, the animation has to be stopped and started again to
     * pick up the new value.
     */
    private ObjectProperty<Node> node;
    private static final Node DEFAULT_NODE = null;

    public final void setNode(Node value) {
        if ((node != null) || (value != null /* DEFAULT_NODE */)) {
            nodeProperty().set(value);
        }
    }

    public final Node getNode() {
        return (node == null)? DEFAULT_NODE : node.get();
    }

    public final ObjectProperty<Node> nodeProperty() {
        if (node == null) {
            node = new javafx.beans.property.SimpleObjectProperty<Node>(this, "node", DEFAULT_NODE);
        }
        return node;
    }

    private final Set<Animation> childrenSet = new HashSet<Animation>();

    private final ObservableList<Animation> children = new VetoableListDecorator<Animation>(new TrackableObservableList<Animation>() {
        @Override
        protected void onChanged(Change<Animation> c) {
            while (c.next()) {
                for (final Animation animation : c.getRemoved()) {
                    animation.parent = null;
                    animation.rateProperty().removeListener(childrenListener);
                    animation.totalDurationProperty().removeListener(childrenListener);
                    animation.delayProperty().removeListener(childrenListener);
                }
                for (final Animation animation : c.getAddedSubList()) {
                    animation.parent = ParallelTransition.this;
                    animation.rateProperty().addListener(childrenListener);
                    animation.totalDurationProperty().addListener(childrenListener);
                    animation.delayProperty().addListener(childrenListener);
                }
            }
            childrenListener.invalidated(children);
        }
    }) {

        @Override
        protected void onProposedChange(List<Animation> toBeAdded, int... indexes) {
            IllegalArgumentException exception = null;
            for (int i = 0; i < indexes.length; i+=2) {
                for (int idx = indexes[i]; idx < indexes[i+1]; ++idx) {
                    childrenSet.remove(children.get(idx));
                }
            }
            for (Animation child : toBeAdded) {
                if (child == null) {
                    exception = new IllegalArgumentException("Child cannot be null");
                    break;
                }
                if (!childrenSet.add(child)) {
                    exception = new IllegalArgumentException("Attempting to add a duplicate to the list of children");
                    break;
                }
                if (checkCycle(child, ParallelTransition.this)) {
                    exception = new IllegalArgumentException("This change would create cycle");
                    break;
                }
            }

            if (exception != null) {
                childrenSet.clear();
                childrenSet.addAll(children);
                throw exception;
            }
        }

    };

    private static boolean checkCycle(Animation child, Animation parent) {
        Animation a = parent;
        while (a != child) {
            if (a.parent != null) {
                a = a.parent;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * A list of {@link javafx.animation.Animation Animations} that will be
     * played sequentially.
     * <p>
     * It is not possible to change the children of a running
     * {@code ParallelTransition}. If the children are changed for a running
     * {@code ParallelTransition}, the animation has to be stopped and started
     * again to pick up the new value.
     *
     * @return the list of {@link javafx.animation.Animation Animations}
     */
    public final ObservableList<Animation> getChildren() {
        return children;
    }

    /**
     * The constructor of {@code ParallelTransition}.
     *
     * @param node
     *            The target {@link javafx.scene.Node} to be used in child
     *            {@link Transition Transitions} that have no {@code Node} specified
     *            themselves
     * @param children
     *            The child {@link javafx.animation.Animation Animations} of
     *            this {@code ParallelTransition}
     */
    public ParallelTransition(Node node, Animation... children) {
        setInterpolator(Interpolator.LINEAR);
        setNode(node);
        getChildren().setAll(children);
    }

    /**
     * The constructor of {@code ParallelTransition}.
     *
     * @param children
     *            The child {@link javafx.animation.Animation Animations} of
     *            this {@code ParallelTransition}
     */
    public ParallelTransition(Animation... children) {
        this(null, children);
    }

    /**
     * The constructor of {@code ParallelTransition}.
     *
     * @param node
     *            The target {@link javafx.scene.Node} to be used in child
     *            {@link Transition Transitions} that have no {@code Node} specified
     *            themselves
     */
    public ParallelTransition(Node node) {
        setInterpolator(Interpolator.LINEAR);
        setNode(node);
    }

    /**
     * The constructor of {@code ParallelTransition}.
     */
    public ParallelTransition() {
        this((Node) null);
    }

    // For testing purposes
    ParallelTransition(AbstractPrimaryTimer timer) {
        super(timer);
        setInterpolator(Interpolator.LINEAR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Node getParentTargetNode() {
        final Node node = getNode();
        return (node != null) ? node : (parent != null && parent instanceof Transition) ?
                ((Transition)parent).getParentTargetNode() : null;
    }

    private Duration computeCycleDuration() {
        Duration maxTime = Duration.ZERO;
        for (final Animation animation : getChildren()) {
            final double absRate = Math.abs(animation.getRate());
            final Duration totalDuration = (absRate < EPSILON) ?
                    animation.getTotalDuration() : animation.getTotalDuration().divide(absRate);
            final Duration childDuration = totalDuration.add(animation.getDelay());
            if (childDuration.isIndefinite()) {
                return Duration.INDEFINITE;
            } else {
                if (childDuration.greaterThan(maxTime)) {
                    maxTime = childDuration;
                }
            }
        }
        return maxTime;
    }

    private double calculateFraction(long currentTicks, long cycleTicks) {
        final double frac = (double) currentTicks / cycleTicks;
        return (frac <= 0.0) ? 0 : (frac >= 1.0) ? 1.0 : frac;
    }

    private boolean startChild(Animation child, int index) {
        final boolean forceSync = forceChildSync[index];
        if (child.startable(forceSync)) {
            child.clipEnvelope.setRate(rates[index] * Math.signum(getCurrentRate()));
            child.doStart(forceSync);
            forceChildSync[index] = false;
            return true;
        }
        return false;
    }

    @Override
    void sync(boolean forceSync) {
        super.sync(forceSync);
        if ((forceSync && childrenChanged) || (durations == null)) {
            cachedChildren = getChildren().toArray(EMPTY_ANIMATION_ARRAY);
            final int n = cachedChildren.length;
            durations = new long[n];
            delays = new long[n];
            rates = new double[n];
            offsetTicks = new long[n];
            forceChildSync = new boolean[n];
            cycleTime = 0;
            int i = 0;
            for (final Animation animation : cachedChildren) {
                rates[i] = Math.abs(animation.getRate());
                if (rates[i] < EPSILON) {
                    rates[i] = 1;
                }
                durations[i] = fromDuration(animation.getTotalDuration(), rates[i]);
                delays[i] = fromDuration(animation.getDelay());
                cycleTime = Math.max(cycleTime, add(durations[i], delays[i]));
                forceChildSync[i] = true;
                i++;
            }
            childrenChanged = false;
        } else if (forceSync) {
            final int n = forceChildSync.length;
            for (int i=0; i<n; i++) {
                forceChildSync[i] = true;
            }
        }
    }

    @Override
    void doPause() {
        super.doPause();
        for (final Animation animation : cachedChildren) {
            if (animation.getStatus() == Status.RUNNING) {
                animation.doPause();
            }
        }
    }

    @Override
    void doResume() {
        super.doResume();
        int i = 0;
        for (final Animation animation : cachedChildren) {
            if (animation.getStatus() == Status.PAUSED) {
                animation.doResume();
                animation.clipEnvelope.setRate(rates[i] * Math.signum(getCurrentRate()));
            }
            i++;
        }
    }

    @Override
    void doStart(boolean forceSync) {
        super.doStart(forceSync);
        toggledRate = false;
        rateProperty().addListener(rateListener);
        double curRate = getCurrentRate();
        final long currentTicks = TickCalculation.fromDuration(getCurrentTime());
        if (curRate < 0) {
            jumpToEnd();
            if (currentTicks < cycleTime) {
                doJumpTo(currentTicks, cycleTime, false);
            }
        } else {
            jumpToStart();
            if (currentTicks > 0) {
                doJumpTo(currentTicks, cycleTime, false);
            }
        }
    }

    @Override
    void doStop() {
        super.doStop();
        for (final Animation animation : cachedChildren) {
            if (animation.getStatus() != Status.STOPPED) {
                animation.doStop();
            }
        }
        if (childrenChanged) {
            setCycleDuration(computeCycleDuration());
        }
        rateProperty().removeListener(rateListener);
    }

    @Override
    void doPlayTo(long currentTicks, long cycleTicks) {
        setCurrentTicks(currentTicks);
        final double frac = calculateFraction(currentTicks, cycleTicks);
        final long newTicks = Math.max(0, Math.min(getCachedInterpolator().interpolate(0, cycleTicks, frac), cycleTicks));
        if (toggledRate) {
            for (int i = 0; i < cachedChildren.length; ++i) {
                if (cachedChildren[i].getStatus() == Status.RUNNING) {
                    offsetTicks[i] -= Math.signum(getCurrentRate()) * (durations[i] - 2 * (oldTicks - delays[i]));
                }
            }
            toggledRate = false;
        }
        if (getCurrentRate() > 0) {
            int i = 0;
            for (final Animation animation : cachedChildren) {
                if ((newTicks >= delays[i]) && ((oldTicks <= delays[i]) ||
                        ((newTicks < add(delays[i], durations[i])) && (animation.getStatus() == Status.STOPPED)))) {
                    final boolean enteringCycle = oldTicks <= delays[i];
                    if (startChild(animation, i)) {
                        animation.clipEnvelope.jumpTo(0);
                    } else {
                        if (enteringCycle) {
                            final EventHandler<ActionEvent> handler = animation.getOnFinished();
                            if (handler != null) {
                                handler.handle(new ActionEvent(this, null));
                            }
                        }
                        continue;
                    }
                }
                if (newTicks >= add(durations[i], delays[i])) {
                    if (animation.getStatus() == Status.RUNNING) {
                        animation.doTimePulse(sub(durations[i], offsetTicks[i]));
                        offsetTicks[i] = 0;
                    }
                } else if (newTicks > delays[i]) {
                    animation.doTimePulse(sub(newTicks - delays[i], offsetTicks[i]));
                }
                i++;
            }
        } else {
            int i = 0;
            for (final Animation animation : cachedChildren) {
                if (newTicks < add(durations[i], delays[i])) {
                    if ((oldTicks >= add(durations[i], delays[i])) || ((newTicks >= delays[i]) && (animation.getStatus() == Status.STOPPED))){
                        final boolean enteringCycle = oldTicks >= add(durations[i], delays[i]);
                        if (startChild(animation, i)) {
                            animation.clipEnvelope.jumpTo(Math.round(durations[i] * rates[i]));
                        } else {
                            if (enteringCycle) {
                                final EventHandler<ActionEvent> handler = animation.getOnFinished();
                                if (handler != null) {
                                    handler.handle(new ActionEvent(this, null));
                                }
                            }
                            continue;
                        }
                    }
                    if (newTicks <= delays[i]) {
                        if (animation.getStatus() == Status.RUNNING) {
                            animation.doTimePulse(sub(durations[i], offsetTicks[i]));
                            offsetTicks[i] = 0;
                        }
                    } else {
                        animation.doTimePulse(sub( add(durations[i], delays[i]) - newTicks, offsetTicks[i]));
                    }
                }
                i++;
            }
        }
        oldTicks = newTicks;
    }

    @Override
    void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
        setCurrentTicks(currentTicks);
        if (getStatus() == Status.STOPPED && !forceJump) {
            return;
        }
        sync(false);
        final double frac = calculateFraction(currentTicks, cycleTicks);
        final long newTicks = Math.max(0, Math.min(getCachedInterpolator().interpolate(0, cycleTicks, frac), cycleTicks));
        int i = 0;
        for (final Animation animation : cachedChildren) {
            final Status status = animation.getStatus();
            if (newTicks <= delays[i]) {
                offsetTicks[i] = 0;
                if (status != Status.STOPPED) {
                    animation.clipEnvelope.jumpTo(0);
                    animation.doStop();
                } else if(TickCalculation.fromDuration(animation.getCurrentTime()) != 0) {
                    animation.doJumpTo(0, durations[i], true);
                }
            } else if (newTicks >= add(durations[i], delays[i])) {
                offsetTicks[i] = 0;
                if (status != Status.STOPPED) {
                    animation.clipEnvelope.jumpTo(Math.round(durations[i] * rates[i]));
                    animation.doStop();
                } else if (TickCalculation.fromDuration(animation.getCurrentTime()) != durations[i]) {
                    animation.doJumpTo(durations[i], durations[i], true);
                }
            } else {
                if (status == Status.STOPPED) {
                    startChild(animation, i);
                    if (getStatus() == Status.PAUSED) {
                        animation.doPause();
                    }

                    offsetTicks[i] = (getCurrentRate() > 0)? newTicks - delays[i] : add(durations[i], delays[i]) - newTicks;
                } else if (status == Status.PAUSED) {
                    offsetTicks[i] += (newTicks - oldTicks) * Math.signum(this.clipEnvelope.getCurrentRate());
                } else {
                    offsetTicks[i] += (getCurrentRate() > 0) ? newTicks - oldTicks : oldTicks - newTicks;
                }
                animation.clipEnvelope.jumpTo(Math.round(sub(newTicks, delays[i]) * rates[i]));
            }
            i++;
        }
        oldTicks = newTicks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void interpolate(double frac) {
        // no-op
    }

    private void jumpToEnd() {
        for (int i = 0 ; i < cachedChildren.length; ++i) {
            if (forceChildSync[i]) {
                // See explanation in SequentialTransition#jumpToEnd
                cachedChildren[i].sync(true);
            }
            cachedChildren[i].doJumpTo(durations[i], durations[i], true);
        }
    }

    private void jumpToStart() {
        for (int i = cachedChildren.length - 1 ; i >= 0; --i) {
            if (forceChildSync[i]) {
                cachedChildren[i].sync(true);
            }
            cachedChildren[i].doJumpTo(0, durations[i], true);
        }
    }

}
