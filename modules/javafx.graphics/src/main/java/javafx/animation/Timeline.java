/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.animation.shared.TimelineClipCore;

/**
 * A {@code Timeline} can be used to define a free form animation of any
 * {@link javafx.beans.value.WritableValue}, for example, all
 * {@link javafx.beans.property.Property JavaFX Properties}.
 * <p>
 * A {@code Timeline}, defined by one or more {@link KeyFrame}s, processes
 * individual {@code KeyFrame} sequentially, in the order specified by
 * {@code KeyFrame.time}. The animated properties, defined as key values in
 * {@code KeyFrame.values}, are interpolated
 * to/from the targeted key values at the specified time of the {@code KeyFrame}
 * to {@code Timeline}'s initial position, depends on {@code Timeline}'s
 * direction.
 * <p>
 * {@code Timeline} processes individual {@code KeyFrame} at or after specified
 * time interval elapsed, it does not guarantee the timing when {@code KeyFrame}
 * is processed.
 * <p>
 * The {@link #cycleDurationProperty()} will be set to the largest time value
 * of Timeline's keyFrames.
 * <p>
 * If a {@code KeyFrame} is not provided for the {@code time==0s} instant, one
 * will be synthesized using the target values that are current at the time
 * {@link #play()} or {@link #playFromStart()} is called.
 * <p>
 * It is not possible to change the {@code keyFrames} of a running {@code Timeline}.
 * If the value of {@code keyFrames} is changed for a running {@code Timeline}, it
 * has to be stopped and started again to pick up the new value.
 * <p>
 * A simple Timeline can be created like this:
 * <pre>{@code
 * final Timeline timeline = new Timeline();
 * timeline.setCycleCount(2);
 * timeline.setAutoReverse(true);
 * timeline.getKeyFrames().add(new KeyFrame(Duration.millis(5000),
 *   new KeyValue (node.translateXProperty(), 25)));
 * timeline.play();
 * }</pre>
 * <p>
 * This Timeline will run for 10s, animating the node by x axis to value 25 and then back to 0 on the second cycle.
 * <p>
 * <b>Warning:</b> A running Timeline is being referenced from the FX runtime. Infinite Timeline
 * might result in a memory leak if not stopped properly. All the objects with animated properties would not be garbage collected.
 *
 * @see Animation
 * @see KeyFrame
 * @see KeyValue
 *
 * @since JavaFX 2.0
 */
public final class Timeline extends Animation {
    /* Package-private for testing purposes */
    final TimelineClipCore clipCore;

    /**
     * Returns the {@link KeyFrame KeyFrames} of this {@code Timeline}.
     * @return the {@link KeyFrame KeyFrames}
     */
    public final ObservableList<KeyFrame> getKeyFrames() {
        return keyFrames;
    }
    private final ObservableList<KeyFrame> keyFrames = new TrackableObservableList<KeyFrame>() {
        @Override
        protected void onChanged(Change<KeyFrame> c) {
            while (c.next()) {
                if (!c.wasPermutated()) {
                    for (final KeyFrame keyFrame : c.getRemoved()) {
                        final String cuePoint = keyFrame.getName();
                        if (cuePoint != null) {
                            getCuePoints().remove(cuePoint);
                        }
                    }
                    for (final KeyFrame keyFrame : c.getAddedSubList()) {
                        final String cuePoint = keyFrame.getName();
                        if (cuePoint != null) {
                            getCuePoints().put(cuePoint, keyFrame.getTime());
                        }
                    }
                    final Duration duration = clipCore.setKeyFrames(getKeyFrames());
                    setCycleDuration(duration);
                }
            }
        }
    };

    /**
     * The constructor of {@code Timeline}.
     *
     * This constructor allows to define a {@link Animation#targetFramerate}.
     *
     * @param targetFramerate
     *            The custom target frame rate for this {@code Timeline}
     * @param keyFrames
     *            The keyframes of this {@code Timeline}
     */
    public Timeline(double targetFramerate, KeyFrame... keyFrames) {
        super(targetFramerate);
        clipCore = new TimelineClipCore(this);
        getKeyFrames().setAll(keyFrames);
    }

    /**
     * The constructor of {@code Timeline}.
     *
     * @param keyFrames
     *            The keyframes of this {@code Timeline}
     */
    public Timeline(KeyFrame... keyFrames) {
        super();
        clipCore = new TimelineClipCore(this);
        getKeyFrames().setAll(keyFrames);
    }

    /**
     * The constructor of {@code Timeline}.
     *
     * This constructor allows to define a {@link Animation#targetFramerate}.
     *
     * @param targetFramerate
     *            The custom target frame rate for this {@code Timeline}
     */
    public Timeline(double targetFramerate) {
        super(targetFramerate);
        clipCore = new TimelineClipCore(this);
    }

    /**
     * The constructor of {@code Timeline}.
     */
    public Timeline() {
        super();
        clipCore = new TimelineClipCore(this);
    }

    // This constructor is only for testing purposes
    Timeline(final AbstractMasterTimer timer) {
        super(timer);
        clipCore = new TimelineClipCore(this);
    }

    @Override
    void doPlayTo(long currentTicks, long cycleTicks) {
        clipCore.playTo(currentTicks);
    }

    @Override
    void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
        sync(false);
        setCurrentTicks(currentTicks);
        clipCore.jumpTo(currentTicks, forceJump);
    }

    @Override
    void setCurrentRate(double currentRate) {
        super.setCurrentRate(currentRate);
        clipCore.notifyCurrentRateChanged();
    }

    @Override
    void doStart(boolean forceSync) {
        super.doStart(forceSync);
        clipCore.start(forceSync);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (parent != null) {
            throw new IllegalStateException("Cannot stop when embedded in another animation");
        }
        if (getStatus() == Status.RUNNING) {
            clipCore.abort();
        }
        super.stop();
    }
}
