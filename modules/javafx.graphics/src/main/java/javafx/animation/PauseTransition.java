/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.util.Duration;

/**
 * This {@code Transition} executes an {@link Animation#onFinished} at the end of its
 * {@link #durationProperty() duration}.
 *
 * <p>
 * Code Segment Example:
 * </p>
 *
 * <pre>
 * <code>
 * import javafx.scene.shape.*;
 * import javafx.animation.*;
 *
 * ...
 *
 *     Rectangle rect = new Rectangle (100, 40, 100, 100);
 *     rect.setArcHeight(50);
 *     rect.setArcWidth(50);
 *     rect.setFill(Color.VIOLET);
 *
 *     RotateTransition rt = new RotateTransition(Duration.millis(3000), rect);
 *     rt.setByAngle(180);
 *     rt.setCycleCount(4f);
 *     rt.setAutoReverse(true);
 *     SequentialTransition seqTransition = new SequentialTransition (
 *         new PauseTransition(Duration.millis(1000)), // wait a second
 *         rt
 *     );
 *     seqTransition.play();
 *
 * ...
 *
 * </code>
 * </pre>
 *
 * @see Transition
 * @see Animation
 *
 * @since JavaFX 2.0
 */
public final class PauseTransition extends TimedTransition {

    /**
     * The constructor of {@code PauseTransition}.
     *
     * @param duration
     *            The duration of the {@code PauseTransition}
     */
    public PauseTransition(Duration duration) {
        setDuration(duration);
        setCycleDuration(duration);
    }

    /**
     * The constructor of {@code PauseTransition}
     */
    public PauseTransition() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interpolate(double frac) {
        // no-op
    }
}
