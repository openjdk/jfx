/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.AnimationShim;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TransitionShim;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FadeTransitionTest {

    private static Duration DEFAULT_DURATION = Duration.millis(400);
    private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;

    private static double EPSILON = 1e-12;
    private static Duration ONE_SEC = Duration.millis(1000);
    private static Duration TWO_SECS = Duration.millis(2000);

    private Node node;

    @BeforeEach
    public void setUp() {
        node = new Rectangle();
    }

    @Test
    public void testDefaultValues() {
        // empty ctor
        final FadeTransition t0 = new FadeTransition();
        assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
        assertTrue(Double.isNaN(t0.getFromValue()));
        assertTrue(Double.isNaN(t0.getToValue()));
        assertEquals(0.0, t0.getByValue(), EPSILON);
        assertNull(t0.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration only
        final FadeTransition t1 = new FadeTransition(ONE_SEC);
        assertEquals(ONE_SEC, t1.getDuration());
        assertTrue(Double.isNaN(t1.getFromValue()));
        assertTrue(Double.isNaN(t1.getToValue()));
        assertEquals(0.0, t1.getByValue(), EPSILON);
        assertNull(t1.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t1.getInterpolator());
        assertNull(t1.getOnFinished());

        // duration and node
        final FadeTransition t2 = new FadeTransition(TWO_SECS, node);
        assertEquals(TWO_SECS, t2.getDuration());
        assertTrue(Double.isNaN(t2.getFromValue()));
        assertTrue(Double.isNaN(t2.getToValue()));
        assertEquals(0.0, t2.getByValue(), EPSILON);
        assertEquals(node, t2.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t2.getInterpolator());
        assertNull(t2.getOnFinished());
    }

    @Test
    public void testDefaultValuesFromProperties() {
        // empty ctor
        final FadeTransition t0 = new FadeTransition();
        assertEquals(DEFAULT_DURATION, t0.durationProperty().get());
        assertTrue(Double.isNaN(t0.fromValueProperty().get()));
        assertTrue(Double.isNaN(t0.toValueProperty().get()));
        assertEquals(0.0, t0.byValueProperty().get(), EPSILON);
        assertNull(t0.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t0.interpolatorProperty().get());
        assertNull(t0.onFinishedProperty().get());

        // duration only
        final FadeTransition t1 = new FadeTransition(ONE_SEC);
        assertEquals(ONE_SEC, t1.durationProperty().get());
        assertTrue(Double.isNaN(t1.fromValueProperty().get()));
        assertTrue(Double.isNaN(t1.toValueProperty().get()));
        assertEquals(0.0, t1.byValueProperty().get(), EPSILON);
        assertNull(t1.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t1.interpolatorProperty().get());
        assertNull(t1.onFinishedProperty().get());

        // duration and node
        final FadeTransition t2 = new FadeTransition(TWO_SECS, node);
        assertEquals(TWO_SECS, t2.durationProperty().get());
        assertTrue(Double.isNaN(t2.fromValueProperty().get()));
        assertTrue(Double.isNaN(t2.toValueProperty().get()));
        assertEquals(0.0, t2.byValueProperty().get(), EPSILON);
        assertEquals(node, t2.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t2.interpolatorProperty().get());
        assertNull(t2.onFinishedProperty().get());
    }

    @Test
    public void testInterpolate() {
        final FadeTransition t0 = new FadeTransition(ONE_SEC, node);
        t0.setFromValue(0.5);
        t0.setToValue(1.0);

        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.5, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,0.4);
        assertEquals(0.7, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(1.0, node.getOpacity(), EPSILON);
                AnimationShim.finished(t0);
    }

    @Test
    public void testValueCombinations() {
        final FadeTransition t0 = new FadeTransition(ONE_SEC, node);
        final double originalValue = 0.6;
        final double fromValue = 0.4;
        final double toValue = 0.9;
        final double byValue = -0.2;

        // no value set
        node.setOpacity(originalValue);
        t0.setFromValue(Double.NaN);
        t0.setToValue(Double.NaN);
        t0.setByValue(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // only from-value set
        node.setOpacity(originalValue);
        t0.setFromValue(fromValue);
        t0.setToValue(Double.NaN);
        t0.setByValue(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // only to-value set
        node.setOpacity(originalValue);
        t0.setFromValue(Double.NaN);
        t0.setToValue(toValue);
        t0.setByValue(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // only by-value set
        node.setOpacity(originalValue);
        t0.setFromValue(Double.NaN);
        t0.setToValue(Double.NaN);
        t0.setByValue(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue + byValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // from- and to-values set
        node.setOpacity(originalValue);
        t0.setFromValue(fromValue);
        t0.setToValue(toValue);
        t0.setByValue(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // from- and by-values set
        node.setOpacity(originalValue);
        t0.setFromValue(fromValue);
        t0.setToValue(Double.NaN);
        t0.setByValue(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue + byValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // to- and by-values set
        node.setOpacity(originalValue);
        t0.setFromValue(Double.NaN);
        t0.setToValue(toValue);
        t0.setByValue(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // all values set
        node.setOpacity(originalValue);
        t0.setFromValue(fromValue);
        t0.setToValue(toValue);
        t0.setByValue(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);
    }

    @Test
    public void testOutOfBoundValues() {
        final FadeTransition t0 = new FadeTransition(ONE_SEC, node);
        t0.setInterpolator(Interpolator.LINEAR);

        // start < 0.0
        t0.setFromValue(-0.4);
        t0.setToValue(0.6);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.0, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.3, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(0.6, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // start > 1.0
        t0.setFromValue(1.3);
        t0.setToValue(0.3);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(1.0, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.65, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(0.3, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // end < 0.0
        t0.setFromValue(0.2);
        t0.setToValue(-1.2);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.2, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.1, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(0.0, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // end > 1.0
        t0.setFromValue(0.9);
        t0.setToValue(1.9);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.9, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.95, node.getOpacity(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(1.0, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);
    }

    @Test
    public void testGetTargetNode() {
        final FadeTransition ft = new FadeTransition(ONE_SEC, node);
        ft.setInterpolator(Interpolator.LINEAR);
        ft.setFromValue(0.5);
        ft.setToValue(1.0);
        final Rectangle node2 = new Rectangle();
        final ParallelTransition pt = new ParallelTransition();
        pt.getChildren().add(ft);
        pt.setNode(node2);

        // node set, parent set
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        TransitionShim.interpolate(ft,0.5);
        assertEquals(0.75, node.getOpacity(), EPSILON);
        assertEquals(1.0, node2.getOpacity(), EPSILON);
        AnimationShim.finished(ft);

        // node null, parent set
        ft.setNode(null);
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        TransitionShim.interpolate(ft,0.4);
        assertEquals(0.75, node.getOpacity(), EPSILON);
        assertEquals(0.7, node2.getOpacity(), EPSILON);
        AnimationShim.finished(ft);

        // node null, parent null
        pt.setNode(null);
        assertFalse(AnimationShim.startable(ft,true));
    }

    @Test
    public void testCachedValues() {
        final FadeTransition ft = new FadeTransition(ONE_SEC, node);
        ft.setInterpolator(Interpolator.LINEAR);
        ft.setFromValue(0.5);
        ft.setToValue(1.0);

        // start
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setFromValue(0.0);
        TransitionShim.interpolate(ft,0.5);
        assertEquals(0.75, node.getOpacity(), EPSILON);
        AnimationShim.finished(ft);
        ft.setFromValue(0.5);

        // end
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setToValue(0.0);
        TransitionShim.interpolate(ft,0.2);
        assertEquals(0.6, node.getOpacity(), EPSILON);
        AnimationShim.finished(ft);
        ft.setToValue(1.0);

        // node
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setNode(null);
        TransitionShim.interpolate(ft,0.7);
        assertEquals(0.85, node.getOpacity(), EPSILON);
        AnimationShim.finished(ft);
        ft.setNode(node);

        // interpolator
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setInterpolator(null);
        TransitionShim.interpolate(ft,0.1);
        assertEquals(0.55, node.getOpacity(), EPSILON);
        AnimationShim.finished(ft);
        ft.setInterpolator(Interpolator.LINEAR);
    }

    @Test
    public void testStartable() {
        final FadeTransition t0 = new FadeTransition(Duration.ONE, node);
        assertTrue(AnimationShim.startable(t0,true));

        // duration is 0
        t0.setDuration(Duration.ZERO);
        assertFalse(AnimationShim.startable(t0,true));
        t0.setDuration(Duration.ONE);
        assertTrue(AnimationShim.startable(t0,true));

        // node is null
        t0.setNode(null);
        assertFalse(AnimationShim.startable(t0,true));
        t0.setNode(node);
        assertTrue(AnimationShim.startable(t0,true));

        // interpolator is null
        t0.setInterpolator(null);
        assertFalse(AnimationShim.startable(t0,true));
        t0.setInterpolator(Interpolator.LINEAR);
        assertTrue(AnimationShim.startable(t0,true));
    }

    @Test
    public void testEvaluateStartValue() {
        final FadeTransition t0 = new FadeTransition(Duration.INDEFINITE, node);

        // first run
        node.setOpacity(0.6);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        node.setOpacity(0.8);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.6, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);

        // second run
        node.setOpacity(0.2);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        node.setOpacity(0.8);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.2, node.getOpacity(), EPSILON);
        AnimationShim.finished(t0);
    }

}
