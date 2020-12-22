/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sun.javafx.tk.Toolkit;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TransitionShim;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Test;


public class TransitionTest {

    private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;
    private static double EPSILON = 1e-12;

    private TransitionImpl transition;

    @Before
    public void setUp() {
        transition = new TransitionImpl(Duration.millis(1000));
    }

    @Test
    public void testDefaultValues() {
        // emtpy ctor
        Transition t0 = new TransitionImpl(Duration.millis(1000));
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertEquals(6000.0 / Toolkit.getToolkit().getPrimaryTimer().getDefaultResolution(), t0.getTargetFramerate(), EPSILON);

        // setting targetFramerate
        Transition t1 = new TransitionImpl(Duration.millis(1000), 10);
        assertEquals(DEFAULT_INTERPOLATOR, t1.getInterpolator());
        assertEquals(10, t1.getTargetFramerate(), EPSILON);
    }

    @Test
    public void testDefaultValuesFromProperties() {
        assertEquals(DEFAULT_INTERPOLATOR, transition.interpolatorProperty().get());
    }

    @Test
    public void testGetParentTargetNode() {
        final Rectangle node = new Rectangle();

        // parent and parent node set
        final ParallelTransition parent = new ParallelTransition();
        parent.getChildren().add(transition);
        parent.setNode(node);
        assertEquals(node, transition.getParentTargetNode());

        // parent set, parent node null
        parent.setNode(null);
        assertNull(transition.getParentTargetNode());

        // parent null, parent node set
        parent.setNode(node);
        parent.getChildren().clear();
        assertNull(transition.getParentTargetNode());
    }

    @Test
    public void testStart() {
        transition.doStart(true);
        transition.setInterpolator(Interpolator.DISCRETE);
        assertEquals(DEFAULT_INTERPOLATOR, transition.getCachedInterpolator());
        transition.shim_impl_finished();

        transition.doStart(true);
        assertEquals(Interpolator.DISCRETE, transition.getCachedInterpolator());
        transition.shim_impl_finished();
    }

    @Test
    public void testPlayTo() {
        assertTrue(transition.startable(true));

        // normal play with linear interpolator
        transition.setInterpolator(Interpolator.LINEAR);
        transition.doStart(true);
        transition.doPlayTo(0, 2);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doPlayTo(1, 2);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doPlayTo(2, 2);
        assertEquals(1.0, transition.frac, EPSILON);
        transition.shim_impl_finished();

        // normal play with discrete interpolator
        transition.setInterpolator(Interpolator.DISCRETE);
        transition.doStart(true);
        transition.doPlayTo(0, 2);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doPlayTo(1, 2);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doPlayTo(2, 2);
        assertEquals(1.0, transition.frac, EPSILON);

        transition.shim_impl_finished();
    }

    @Test
    public void testJumpTo() {
        // not running
        transition.doJumpTo(0, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);

        // running with linear interpolator
        transition.setInterpolator(Interpolator.LINEAR);
        assertTrue(transition.startable(true));
        transition.doStart(true);
        transition.doJumpTo(0, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, false);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, false);
        assertEquals(1.0, transition.frac, EPSILON);

        // paused with linear interpolator
        transition.doPause();
        transition.doJumpTo(0, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, false);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, false);
        assertEquals(1.0, transition.frac, EPSILON);
        transition.shim_impl_finished();

        // running with discrete interpolator
        transition.setInterpolator(Interpolator.DISCRETE);
        assertTrue(transition.startable(true));
        transition.doStart(true);
        transition.doJumpTo(0, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, false);
        assertEquals(1.0, transition.frac, EPSILON);

        // paused with discrete interpolator
        transition.doPause();
        transition.doJumpTo(0, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, false);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, false);
        assertEquals(1.0, transition.frac, EPSILON);
        transition.shim_impl_finished();
    }



    @Test
    public void testForcedJumpTo() {
        transition.setInterpolator(Interpolator.LINEAR);
        // not running
        transition.doJumpTo(0, 2, true);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, true);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, true);
        assertEquals(1.0, transition.frac, EPSILON);

        // running with linear interpolator
        assertTrue(transition.startable(true));
        transition.doStart(true);
        transition.doJumpTo(0, 2, true);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, true);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, true);
        assertEquals(1.0, transition.frac, EPSILON);

        // paused with linear interpolator
        transition.doPause();
        transition.doJumpTo(0, 2, true);
        assertEquals(0.0, transition.frac, EPSILON);
        transition.doJumpTo(1, 2, true);
        assertEquals(0.5, transition.frac, EPSILON);
        transition.doJumpTo(2, 2, true);
        assertEquals(1.0, transition.frac, EPSILON);
        transition.shim_impl_finished();

    }

    private static class TransitionImpl extends TransitionShim {
        private double frac;

        private TransitionImpl(Duration duration) {
            setCycleDuration(duration);
        }

        private TransitionImpl(Duration duration, double targetFramerate) {
            super(targetFramerate);
            setCycleDuration(duration);
        }

        public void impl_setCurrentTicks(long ticks) {
            // no-op
        }

        @Override
        protected void interpolate(double frac) {
            this.frac = frac;
        }
    }
}
