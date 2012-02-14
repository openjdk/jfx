/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.javafx.animation.TickCalculation.*;

import java.util.Arrays;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.util.Duration;

import com.sun.javafx.collections.TrackableObservableList;

/**
 * This {@link Transition} starts all {@link javafx.animation.Animation
 * Animations} in {@link #children} in sequential order.
 * <p>
 * Children of this {@code Transition} inherit {@link #node} if their
 * {@code node} variable is not specified.
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
 *     PauseTransition pt = new PauseTransition(Duration.millis(1000));
 *     FadeTransition ft = new FadeTransition(SEC_3);
 *     ft.setFromValue(1.0f);
 *     ft.setToValue(0.3f);
 *     ft.setRepeatCount(2f);
 *     ft.setAutoReverse(true);
 *     TranslateTransition tt = new TranslateTransition(SEC_2);
 *     tt.setFromX(-100f);
 *     tt.setToX(100f);
 *     tt.setRepeatCount(2f);
 *     tt.setAutoReverse(true);
 *     RotateTransition rt = new RotateTransition(SEC_3);
 *     rt.setByAngle(180f);
 *     rt.setRepeatCount(4f);
 *     rt.setAutoReverse(true);
 *     ScaleTransition st = new ScaleTransition(SEC_2);
 *     st.setByX(1.5f);
 *     st.setByY(1.5f);
 *     st.setRepeatCount(2f);
 *     st.setAutoReverse(true);
 * 
 *     SequentialTransition seqT = new SequentialTransition (rect, pt, ft, tt, rt, st);
 *     seqT.play();
 * </code>
 * </pre>
 * 
 * @see Transition
 * @see Animation
 * 
 */
public final class SequentialTransition extends Transition {

    private static final Animation[] EMPTY_ANIMATION_ARRAY = new Animation[0];
    private static final int BEFORE = -1;
    private static final double EPSILON = 1e-12;

    private Animation[] cachedChildren = EMPTY_ANIMATION_ARRAY;
    private long[] startTimes;
    private long[] durations;
    private long[] delays;
    private double[] rates;
    private boolean[] forceChildSync;
    private int end;
    private int curIndex;
    private long oldTicks = -1L;
    private long offsetTicks;
    private boolean childrenChanged = true;

    private final InvalidationListener childrenListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            childrenChanged = true;
            if (getStatus() == Status.STOPPED) {
                setCycleDuration(computeCycleDuration());
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
     * {@code Transition}. If the value of {@code node} is changed for a
     * running {@code Transition}, the animation has to be stopped and started again to
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
            node = new SimpleObjectProperty<Node>(this, "node", DEFAULT_NODE);
        }
        return node;
    }

    private final ObservableList<Animation> children = new TrackableObservableList<Animation>() {
        @Override
        protected void onChanged(Change<Animation> c) {
            while(c.next()) {
                for (final Animation animation : c.getRemoved()) {
                    if (animation instanceof Transition) {
                        final Transition transition = (Transition) animation;
                        if (transition.parent == SequentialTransition.this) {
                            transition.parent = null;
                        }
                    }
                    animation.rateProperty().removeListener(childrenListener);
                    animation.totalDurationProperty().removeListener(childrenListener);
                    animation.delayProperty().removeListener(childrenListener);
                }
                for (final Animation animation : c.getAddedSubList()) {
                    if (animation instanceof Transition) {
                        ((Transition) animation).parent = SequentialTransition.this;
                    }
                    animation.rateProperty().addListener(childrenListener);
                    animation.totalDurationProperty().addListener(childrenListener);
                    animation.delayProperty().addListener(childrenListener);
                }
            }
            childrenListener.invalidated(children);
        }
    };

    /**
     * A list of {@link javafx.animation.Animation Animations} that will be
     * played sequentially.
     * <p>
     * It is not possible to change the children of a running
     * {@code SequentialTransition}. If the children are changed for a running
     * {@code SequentialTransition}, the animation has to be stopped and started
     * again to pick up the new value.
     */
    public final ObservableList<Animation> getChildren() {
        return children;
    }

    /**
     * The constructor of {@code SequentialTransition}.
     * 
     * @param node
     *            The target {@link javafx.scene.Node} to be used in child
     *            {@link Transition Transitions} that have no {@code Node} specified
     *            themselves
     * @param children
     *            The child {@link javafx.animation.Animation Animations} of
     *            this {@code SequentialTransition}
     */
    public SequentialTransition(Node node, Animation... children) {
        setInterpolator(Interpolator.LINEAR);
        setNode(node);
        getChildren().setAll(children);
    }

    /**
     * The constructor of {@code SequentialTransition}.
     * 
     * @param children
     *            The child {@link javafx.animation.Animation Animations} of
     *            this {@code SequentialTransition}
     */
    public SequentialTransition(Animation... children) {
        this(null, children);
    }

    /**
     * The constructor of {@code SequentialTransition}.
     * 
     * @param node
     *            The target {@link javafx.scene.Node} to be used in child
     *            {@link Transition Transitions} that have no {@code Node} specified
     *            themselves
     */
    public SequentialTransition(Node node) {
        setInterpolator(Interpolator.LINEAR);
        setNode(node);
    }

    /**
     * The constructor of {@code SequentialTransition}.
     */
    public SequentialTransition() {
        this((Node) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Node getParentTargetNode() {
        final Node _node = getNode();
        return (_node != null) ? _node : ((parent != null) ? parent.getParentTargetNode() : null);
    }

    private Duration computeCycleDuration() {
        Duration currentDur = Duration.ZERO;

        for (final Animation animation : getChildren()) {
            currentDur = currentDur.add(animation.getDelay());
            final double absRate = Math.abs(animation.getRate());
            currentDur = currentDur.add((absRate < EPSILON) ? 
                    animation.getTotalDuration() : animation.getTotalDuration().divide(absRate));
            if (currentDur.isIndefinite()) {
                break;
            }
        }
        return currentDur;
    }

    private double calculateFraction(long currentTicks, long cycleTicks) {
        final double frac = (double) currentTicks / cycleTicks;
        return (frac <= 0.0) ? 0 : (frac >= 1.0) ? 1.0 : frac;
    }

    private int findNewIndex(long ticks) {
        if ((curIndex != BEFORE) 
                && (curIndex != end)
                && (startTimes[curIndex] <= ticks)
                && (ticks <= startTimes[curIndex + 1])) {
            return curIndex;
        }

        final boolean indexUndefined = (curIndex == BEFORE) || (curIndex == end);
        final int fromIndex = (indexUndefined || (ticks < oldTicks)) ? 0 : curIndex + 1;
        final int toIndex = (indexUndefined || (oldTicks < ticks)) ? end : curIndex;
        final int index = Arrays.binarySearch(startTimes, fromIndex, toIndex, ticks);
        return (index < 0) ? -index - 2 : (index > 0) ? index - 1 : 0;
    }

    @Override
    void impl_sync(boolean forceSync) {
        super.impl_sync(forceSync);
        
        if ((forceSync && childrenChanged) || (startTimes == null)) {
            cachedChildren = getChildren().toArray(EMPTY_ANIMATION_ARRAY);
            end = cachedChildren.length;
            startTimes = new long[end + 1];
            durations = new long[end];
            delays = new long[end];
            rates = new double[end];
            forceChildSync = new boolean[end];
            long cycleTicks = 0L;
            int i = 0;
            for (final Animation animation : cachedChildren) {
                startTimes[i] = cycleTicks;
                rates[i] = animation.getRate();
                if (rates[i] < EPSILON) {
                    rates[i] = 1;
                }
                durations[i] = fromDuration(animation.getTotalDuration(), rates[i]);
                delays[i] = fromDuration(animation.getDelay());
                if ((durations[i] == Long.MAX_VALUE) || (delays[i] == Long.MAX_VALUE) || (cycleTicks == Long.MAX_VALUE)) {
                    cycleTicks = Long.MAX_VALUE;
                } else {
                    cycleTicks = add(cycleTicks, add(durations[i], delays[i]));
                }
                forceChildSync[i] = true;
                i++;
            }
            startTimes[end] = cycleTicks;
            childrenChanged = false;
        } else if (forceSync) {
            final int n = forceChildSync.length;
            for (int i=0; i<n; i++) {
                forceChildSync[i] = true;
            }
        }
    }

    @Override
    void impl_start(boolean forceSync) {
        super.impl_start(forceSync);
        curIndex = (getCurrentRate() > 0) ? BEFORE : end;
        offsetTicks = 0L;
    }

    @Override
    void impl_pause() {
        super.impl_pause();
        if ((curIndex != BEFORE) && (curIndex != end)) {
            final Animation current = cachedChildren[curIndex];
            if (current.getStatus() == Status.RUNNING) {
                current.impl_pause();
            }
        }
    }

    @Override
    void impl_resume() {
        super.impl_resume();
        if ((curIndex != BEFORE) && (curIndex != end)) {
            final Animation current = cachedChildren[curIndex];
            if (current.getStatus() == Status.PAUSED) {
                current.impl_resume();
            }
        }
    }

    @Override
    void impl_stop() {
        super.impl_stop();
        if ((curIndex != BEFORE) && (curIndex != end)) {
            final Animation current = cachedChildren[curIndex];
            if (current.getStatus() != Status.STOPPED) {
                current.impl_stop();
            }
        }
        if (childrenChanged) {
            setCycleDuration(computeCycleDuration());
        }
    }

    private boolean startChild(Animation child, int index) {
        final boolean forceSync = forceChildSync[index];
        if (child.impl_startable(forceSync)) {
            child.setRate(rates[index] * Math.signum(getCurrentRate()));
            child.impl_start(forceSync);
            forceChildSync[index] = false;
            return true;
        }
        return false;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_playTo(long currentTicks, long cycleTicks) {
        impl_setCurrentTicks(currentTicks);
        final double frac = calculateFraction(currentTicks, cycleTicks);
        final long newTicks = Math.max(0, Math.min(getCachedInterpolator().interpolate(0, cycleTicks, frac), cycleTicks));
        final int newIndex = findNewIndex(newTicks);
        final Animation current = ((curIndex == BEFORE) || (curIndex == end)) ? null : cachedChildren[curIndex];
        if (curIndex == newIndex) {
            if (getCurrentRate() > 0) {
                final long currentDelay = add(startTimes[curIndex], delays[curIndex]);
                if (newTicks >= currentDelay) {
                    if ((oldTicks <= currentDelay) || (current.getStatus() == Status.STOPPED)) {
                        final boolean enteringCycle = oldTicks <= currentDelay;
                        if (enteringCycle) {
                            current.jumpTo(Duration.ZERO);
                        }
                        if (!startChild(current, curIndex)) {
                            if (enteringCycle) {
                                final EventHandler<ActionEvent> handler = current.getOnFinished();
                                if (handler != null) {
                                    handler.handle(new ActionEvent(this, null));
                                }
                            }
                            oldTicks = newTicks;
                            return;
                        }
                    }
                    final long localTicks = sub(newTicks, currentDelay);
                    if (newTicks >= startTimes[curIndex+1]) {
                        current.impl_timePulse(Long.MAX_VALUE);
                        if (newTicks == cycleTicks) {
                            curIndex = end;
                        }
                    } else {
                        current.impl_timePulse(calcTimePulse(localTicks));
                    }
                }
            } else { // getCurrentRate() < 0
                final long currentDelay = add(startTimes[curIndex], delays[curIndex]);
                if ((oldTicks >= startTimes[curIndex+1]) || ((oldTicks >= currentDelay) && (current.getStatus() == Status.STOPPED))){
                    final boolean enteringCycle = oldTicks >= startTimes[curIndex+1];
                    if (enteringCycle) {
                        current.jumpTo("end");
                    }
                    if (!startChild(current, curIndex)) {
                        if (enteringCycle) {
                            final EventHandler<ActionEvent> handler = current.getOnFinished();
                            if (handler != null) {
                                handler.handle(new ActionEvent(this, null));
                            }
                        }
                        oldTicks = newTicks;
                        return;
                    }
                }
                if (newTicks <= currentDelay) {
                    current.impl_timePulse(Long.MAX_VALUE);
                    if (newTicks == 0) {
                        curIndex = BEFORE;
                    }
                } else {
                    final long localTicks = sub(startTimes[curIndex + 1], newTicks);
                    current.impl_timePulse(calcTimePulse(localTicks));
                }
            }
        } else { // curIndex != newIndex
            if (curIndex < newIndex) {
                if (current != null) {
                    final long oldDelay = add(startTimes[curIndex], delays[curIndex]);
                    if ((oldTicks <= oldDelay) || (current.getStatus() == Status.STOPPED)) {
                        final boolean enteringCycle = oldTicks <= oldDelay;
                        if (enteringCycle) {
                            current.jumpTo(Duration.ZERO);
                        }
                        if (!startChild(current, curIndex)) {
                            if (enteringCycle) {
                                final EventHandler<ActionEvent> handler = current.getOnFinished();
                                if (handler != null) {
                                    handler.handle(new ActionEvent(this, null));
                                }
                            }
                        }
                    }
                    if (current.getStatus() == Status.RUNNING) {
                        current.impl_timePulse(Long.MAX_VALUE);
                    }
                    oldTicks = startTimes[curIndex + 1];
                }
                offsetTicks = 0;
                curIndex++;
                for (; curIndex < newIndex; curIndex++) {
                    final Animation animation = cachedChildren[curIndex];
                    animation.jumpTo(Duration.ZERO);
                    if (startChild(animation, curIndex)) {
                        animation.impl_timePulse(Long.MAX_VALUE);
                    } else {
                        final EventHandler<ActionEvent> handler = animation.getOnFinished();
                        if (handler != null) {
                            handler.handle(new ActionEvent(this, null));
                        }
                    }
                    oldTicks = startTimes[curIndex + 1];
                }
                final Animation newAnimation = cachedChildren[curIndex];
                newAnimation.jumpTo(Duration.ZERO);
                if (startChild(newAnimation, curIndex)) {
                    if (newTicks >= startTimes[curIndex+1]) {
                        newAnimation.impl_timePulse(Long.MAX_VALUE);
                        if (newTicks == cycleTicks) {
                            curIndex = end;
                        }
                    } else {
                        final long localTicks = sub(newTicks, add(startTimes[curIndex], delays[curIndex]));
                        newAnimation.impl_timePulse(calcTimePulse(localTicks));
                    }
                } else {
                    final EventHandler<ActionEvent> handler = newAnimation.getOnFinished();
                    if (handler != null) {
                        handler.handle(new ActionEvent(this, null));
                    }
                }
            } else {
                if (current != null) {
                    final long oldDelay = add(startTimes[curIndex], delays[curIndex]);
                    if ((oldTicks >= startTimes[curIndex+1]) || ((oldTicks >= oldDelay) && (current.getStatus() == Status.STOPPED))){
                        final boolean enteringCycle = oldTicks >= startTimes[curIndex+1];
                        if (enteringCycle) {
                            current.jumpTo("end");
                        }
                        if (!startChild(current, curIndex)) {
                            if (enteringCycle) {
                                final EventHandler<ActionEvent> handler = current.getOnFinished();
                                if (handler != null) {
                                    handler.handle(new ActionEvent(this, null));
                                }
                            }
                        }
                    }
                    if (current.getStatus() == Status.RUNNING) {
                        current.impl_timePulse(Long.MAX_VALUE);
                    }
                    oldTicks = startTimes[curIndex];
                }
                offsetTicks = 0;
                curIndex--;
                for (; curIndex > newIndex; curIndex--) {
                    final Animation animation = cachedChildren[curIndex];
                    animation.jumpTo(toDuration(durations[curIndex], rates[curIndex]));
                    if (startChild(animation, curIndex)) {
                        animation.impl_timePulse(Long.MAX_VALUE);
                    } else {
                        final EventHandler<ActionEvent> handler = animation.getOnFinished();
                        if (handler != null) {
                            handler.handle(new ActionEvent(this, null));
                        }
                    }
                    oldTicks = startTimes[curIndex];
                }
                final Animation newAnimation = cachedChildren[curIndex];
                newAnimation.jumpTo("end");
                if (startChild(newAnimation, curIndex)) {
                    if (newTicks <= add(startTimes[curIndex], delays[curIndex])) {
                        newAnimation.impl_timePulse(Long.MAX_VALUE);
                        if (newTicks == 0) {
                            curIndex = BEFORE;
                        }
                    } else {
                        final long localTicks = sub(startTimes[curIndex + 1], newTicks);
                        newAnimation.impl_timePulse(calcTimePulse(localTicks));
                    }
                } else {
                    final EventHandler<ActionEvent> handler = newAnimation.getOnFinished();
                    if (handler != null) {
                        handler.handle(new ActionEvent(this, null));
                    }
                }
            }
        }
        oldTicks = newTicks;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_jumpTo(long currentTicks, long cycleTicks) {
        impl_sync(false);
        final Status status = getStatus();
        final double frac = calculateFraction(currentTicks, cycleTicks);
        final long newTicks = Math.max(0, Math.min(getCachedInterpolator().interpolate(0, cycleTicks, frac), cycleTicks));
        final int oldIndex = curIndex;
        curIndex = findNewIndex(newTicks);
        final Animation newAnimation = cachedChildren[curIndex];
        if (curIndex != oldIndex) {
            if (status != Status.STOPPED) {
                if ((oldIndex != BEFORE) && (oldIndex != end)) {
                    final Animation oldChild = cachedChildren[oldIndex];
                    if (oldChild.getStatus() != Status.STOPPED) {
                        cachedChildren[oldIndex].impl_stop();
                    }
                }
                startChild(newAnimation, curIndex);
                if (status == Status.PAUSED) {
                    newAnimation.impl_pause();
                }
            }
        }
        // TODO: This does probably not work if animation is paused (getCurrentRate() == 0)
        offsetTicks = (getCurrentRate() < 0)? sub(startTimes[curIndex+1], newTicks) : sub(newTicks, add(startTimes[curIndex], delays[curIndex]));
        newAnimation.jumpTo(toDuration(sub(newTicks, add(startTimes[curIndex], delays[curIndex])), rates[curIndex]));
        oldTicks = newTicks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void interpolate(double frac) {
        // no-op
    }

    private long calcTimePulse(long ticks) {
        return sub(Math.round(ticks * Math.abs(rates[curIndex])), offsetTicks);
    }
}
