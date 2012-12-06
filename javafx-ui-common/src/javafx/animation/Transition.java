/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.animation.AbstractMasterTimer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/**
 * An abstract class that contains the basic functionalities required by all
 * {@code Transition} based animations, such as {@link PathTransition} and
 * {@link RotateTransition}.
 * <p>
 * This class offers a simple framework to define animation. It provides all the
 * basic functionality defined in {@link Animation}. {@code Transition} requires
 * the implementation of a method {@link #interpolate(double)} which is the
 * called in each frame, while the {@code Transition} is running.
 * <p>
 * In addition an extending class needs to set the duration of a single cycle
 * with {@link Animation#setCycleDuration(javafx.util.Duration)}. This duration
 * is usually set by the user via a duration property (as in
 * {@link FadeTransition#duration}) for example. But it can also be calculated
 * by the extending class as is done in {@link ParallelTransition} and
 * {@link FadeTransition}.
 * <p>
 * Below is a simple example. It creates a small animation that updates the
 * {@code text} property of a {@link javafx.scene.text.Text} node. It starts
 * with an empty {@code String} and adds gradually letter by letter until the
 * full {@code String} was set when the animation finishes.
 * 
 * <pre>
 * {@code
 * 
 * final String content = "Lorem ipsum";
 * final Text text = new Text(10, 20, "");
 * 
 * final Animation animation = new Transition() {
 *     {
 *         setCycleDuration(Duration.millis(2000));
 *     }
 * 
 *     protected void interpolate(double frac) {
 *         final int length = content.length();
 *         final int n = Math.round(length * (float) frac);
 *         text.setText(content.substring(0, n));
 *     }
 * 
 * };
 * 
 * animation.play();
 * }</pre>
 * 
 * @see Animation
 * 
 */
public abstract class Transition extends Animation {

    /**
     * Controls the timing for acceleration and deceleration at each
     * {@code Transition} cycle.
     * <p>
     * This may only be changed prior to starting the transition or after the
     * transition has ended. If the value of {@code interpolator} is changed for
     * a running {@code Transition}, the animation has to be stopped and started again to
     * pick up the new value.
     * <p>
     * Default interpolator is set to {@link Interpolator#EASE_BOTH}.
     * 
     * @defaultValue EASE_BOTH
     */
    private ObjectProperty<Interpolator> interpolator;
    private static final Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;

    public final void setInterpolator(Interpolator value) {
        if ((interpolator != null) || (!DEFAULT_INTERPOLATOR.equals(value))) {
            interpolatorProperty().set(value);
        }
    }

    public final Interpolator getInterpolator() {
        return (interpolator == null) ? DEFAULT_INTERPOLATOR : interpolator.get();
    }

    public final ObjectProperty<Interpolator> interpolatorProperty() {
        if (interpolator == null) {
            interpolator = new SimpleObjectProperty<Interpolator>(
                    this, "interpolator", DEFAULT_INTERPOLATOR
            );
        }
        return interpolator;
    }

    private Interpolator cachedInterpolator;

    /**
     * Returns the {@link Interpolator}, that was set when the
     * {@code Transition} was started.
     * 
     * Changing the {@link #interpolator} of a running {@code Transition} should
     * have no immediate effect. Instead the running {@code Transition} should
     * continue to use the original {@code Interpolator} until it is stopped and
     * started again.
     * 
     * @return the {@code Interpolator} that was set when this
     *         {@code Transition} was started
     */
    protected Interpolator getCachedInterpolator() {
        return cachedInterpolator;
    }

    /**
     * The constructor of {@code Transition}.
     * 
     * This constructor allows to define a {@link #targetFramerate}.
     * 
     * @param targetFramerate
     *            The custom target frame rate for this {@code Transition}
     */
    public Transition(double targetFramerate) {
        super(targetFramerate);
    }

    /**
     * The constructor of {@code Transition}.
     */
    public Transition() {
    }

    // For testing purposes
    Transition(AbstractMasterTimer timer) {
        super(timer);
    }

    /**
     * Returns the target {@link Node} for animation of this {@code Transition}.
     * This method returns {@code node} if it is set, else returns its
     * {@code parent.getTargetNode()} otherwise null.
     */
    protected Node getParentTargetNode() {
        return (parent != null && parent instanceof Transition) ?
                ((Transition)parent).getParentTargetNode() : null;
    }

    /**
     * The method {@code interpolate()} has to be provided by implementations of
     * {@code Transition}. While a {@code Transition} is running, this method is
     * called in every frame.
     * 
     * The parameter defines the current position with the animation. At the
     * start, the fraction will be {@code 0.0} and at the end it will be
     * {@code 1.0}. How the parameter increases, depends on the
     * {@link #interpolatorProperty() interpolator}, e.g. if the
     * {@code interpolator} is {@link Interpolator#LINEAR}, the fraction will
     * increase linear.
     * 
     * This method must not be called by the user directly.
     * 
     * @param frac
     *            The relative position
     */
    protected abstract void interpolate(double frac);

    private double calculateFraction(long currentTicks, long cycleTicks) {
        final double frac = (double) currentTicks / cycleTicks;
        return cachedInterpolator.interpolate(0.0, 1.0, frac);
    }

    @Override
    boolean impl_startable(boolean forceSync) {
        return super.impl_startable(forceSync)
                && ((getInterpolator() != null) || (!forceSync && (cachedInterpolator != null)));
    }

    @Override
    void impl_sync(boolean forceSync) {
        super.impl_sync(forceSync);
        if (forceSync || (cachedInterpolator == null)) {
            cachedInterpolator = getInterpolator();
        }
    }

    @Override
    void impl_playTo(long currentTicks, long cycleTicks) {
        impl_setCurrentTicks(currentTicks);
        interpolate(calculateFraction(currentTicks, cycleTicks));
    }

    @Override
    void impl_jumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
        impl_setCurrentTicks(currentTicks);
        if (getStatus() != Status.STOPPED || forceJump) {
            impl_sync(false);
            interpolate(calculateFraction(currentTicks, cycleTicks));
        }
    }
}
