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
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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

public class ScaleTransitionTest {

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
        final ScaleTransition t0 = new ScaleTransition();
        assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
        assertTrue(Double.isNaN(t0.getFromX()));
        assertTrue(Double.isNaN(t0.getFromY()));
        assertTrue(Double.isNaN(t0.getFromZ()));
        assertTrue(Double.isNaN(t0.getToX()));
        assertTrue(Double.isNaN(t0.getToY()));
        assertTrue(Double.isNaN(t0.getToZ()));
        assertEquals(0.0, t0.getByX(), EPSILON);
        assertEquals(0.0, t0.getByY(), EPSILON);
        assertEquals(0.0, t0.getByZ(), EPSILON);
        assertNull(t0.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration only
        final ScaleTransition t1 = new ScaleTransition(ONE_SEC);
        assertEquals(ONE_SEC, t1.getDuration());
        assertTrue(Double.isNaN(t1.getFromX()));
        assertTrue(Double.isNaN(t1.getFromY()));
        assertTrue(Double.isNaN(t1.getFromZ()));
        assertTrue(Double.isNaN(t1.getToX()));
        assertTrue(Double.isNaN(t1.getToY()));
        assertTrue(Double.isNaN(t1.getToZ()));
        assertEquals(0.0, t1.getByX(), EPSILON);
        assertEquals(0.0, t1.getByY(), EPSILON);
        assertEquals(0.0, t1.getByZ(), EPSILON);
        assertNull(t1.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t1.getInterpolator());
        assertNull(t1.getOnFinished());

        // duration and node
        final ScaleTransition t2 = new ScaleTransition(TWO_SECS, node);
        assertEquals(TWO_SECS, t2.getDuration());
        assertTrue(Double.isNaN(t2.getFromX()));
        assertTrue(Double.isNaN(t2.getFromY()));
        assertTrue(Double.isNaN(t2.getFromZ()));
        assertTrue(Double.isNaN(t2.getToX()));
        assertTrue(Double.isNaN(t2.getToY()));
        assertTrue(Double.isNaN(t2.getToZ()));
        assertEquals(0.0, t2.getByX(), EPSILON);
        assertEquals(0.0, t2.getByY(), EPSILON);
        assertEquals(0.0, t2.getByZ(), EPSILON);
        assertEquals(node, t2.getNode());
        assertEquals(DEFAULT_INTERPOLATOR, t2.getInterpolator());
        assertNull(t2.getOnFinished());
    }

    @Test
    public void testDefaultValuesFromProperties() {
        // empty ctor
        final ScaleTransition t0 = new ScaleTransition();
        assertEquals(DEFAULT_DURATION, t0.durationProperty().get());
        assertTrue(Double.isNaN(t0.fromXProperty().get()));
        assertTrue(Double.isNaN(t0.fromYProperty().get()));
        assertTrue(Double.isNaN(t0.fromZProperty().get()));
        assertTrue(Double.isNaN(t0.toXProperty().get()));
        assertTrue(Double.isNaN(t0.toYProperty().get()));
        assertTrue(Double.isNaN(t0.toZProperty().get()));
        assertEquals(0.0, t0.byXProperty().get(), EPSILON);
        assertEquals(0.0, t0.byYProperty().get(), EPSILON);
        assertEquals(0.0, t0.byZProperty().get(), EPSILON);
        assertNull(t0.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t0.interpolatorProperty().get());
        assertNull(t0.onFinishedProperty().get());

        // duration only
        final ScaleTransition t1 = new ScaleTransition(ONE_SEC);
        assertEquals(ONE_SEC, t1.durationProperty().get());
        assertTrue(Double.isNaN(t1.fromXProperty().get()));
        assertTrue(Double.isNaN(t1.fromYProperty().get()));
        assertTrue(Double.isNaN(t1.fromZProperty().get()));
        assertTrue(Double.isNaN(t1.toXProperty().get()));
        assertTrue(Double.isNaN(t1.toYProperty().get()));
        assertTrue(Double.isNaN(t1.toZProperty().get()));
        assertEquals(0.0, t1.byXProperty().get(), EPSILON);
        assertEquals(0.0, t1.byYProperty().get(), EPSILON);
        assertEquals(0.0, t1.byZProperty().get(), EPSILON);
        assertNull(t1.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t1.interpolatorProperty().get());
        assertNull(t1.onFinishedProperty().get());

        // duration and node
        final ScaleTransition t2 = new ScaleTransition(TWO_SECS, node);
        assertEquals(TWO_SECS, t2.durationProperty().get());
        assertTrue(Double.isNaN(t2.fromXProperty().get()));
        assertTrue(Double.isNaN(t2.fromYProperty().get()));
        assertTrue(Double.isNaN(t2.fromZProperty().get()));
        assertTrue(Double.isNaN(t2.toXProperty().get()));
        assertTrue(Double.isNaN(t2.toYProperty().get()));
        assertTrue(Double.isNaN(t2.toZProperty().get()));
        assertEquals(0.0, t2.byXProperty().get(), EPSILON);
        assertEquals(0.0, t2.byYProperty().get(), EPSILON);
        assertEquals(0.0, t2.byZProperty().get(), EPSILON);
        assertEquals(node, t2.nodeProperty().get());
        assertEquals(DEFAULT_INTERPOLATOR, t2.interpolatorProperty().get());
        assertNull(t2.onFinishedProperty().get());
    }

    @Test
    public void testInterpolate() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        t0.setFromX(0.5);
        t0.setToX(1.0);
        t0.setFromY(1.5);
        t0.setToY(2.0);
        t0.setFromZ(1.5);
        t0.setToZ(0.5);

        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(0.5, node.getScaleX(), EPSILON);
        assertEquals(1.5, node.getScaleY(), EPSILON);
        assertEquals(1.5, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,0.4);
        assertEquals(0.7, node.getScaleX(), EPSILON);
        assertEquals(1.7, node.getScaleY(), EPSILON);
        assertEquals(1.1, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(1.0, node.getScaleX(), EPSILON);
        assertEquals(2.0, node.getScaleY(), EPSILON);
        assertEquals(0.5, node.getScaleZ(), EPSILON);
                AnimationShim.finished(t0);
    }

    @Test
    public void testXValueCombinations() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        final double originalValue = 0.6;
        final double fromValue = 0.4;
        final double toValue = 0.9;
        final double byValue = -0.2;

        // no value set
        node.setScaleX(originalValue);
        t0.setFromX(Double.NaN);
        t0.setToX(Double.NaN);
        t0.setByX(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // only from-value set
        node.setScaleX(originalValue);
        t0.setFromX(fromValue);
        t0.setToX(Double.NaN);
        t0.setByX(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // only to-value set
        node.setScaleX(originalValue);
        t0.setFromX(Double.NaN);
        t0.setToX(toValue);
        t0.setByX(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // only by-value set
        node.setScaleX(originalValue);
        t0.setFromX(Double.NaN);
        t0.setToX(Double.NaN);
        t0.setByX(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue + byValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // from- and to-values set
        node.setScaleX(originalValue);
        t0.setFromX(fromValue);
        t0.setToX(toValue);
        t0.setByX(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // from- and by-values set
        node.setScaleX(originalValue);
        t0.setFromX(fromValue);
        t0.setToX(Double.NaN);
        t0.setByX(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue + byValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // to- and by-values set
        node.setScaleX(originalValue);
        t0.setFromX(Double.NaN);
        t0.setToX(toValue);
        t0.setByX(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // all values set
        node.setScaleX(originalValue);
        t0.setFromX(fromValue);
        t0.setToX(toValue);
        t0.setByX(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleX(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);
    }

    @Test
    public void testYValueCombinations() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        final double originalValue = 0.6;
        final double fromValue = 0.4;
        final double toValue = 0.9;
        final double byValue = -0.2;

        // no value set
        node.setScaleY(originalValue);
        t0.setFromY(Double.NaN);
        t0.setToY(Double.NaN);
        t0.setByY(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // only from-value set
        node.setScaleY(originalValue);
        t0.setFromY(fromValue);
        t0.setToY(Double.NaN);
        t0.setByY(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // only to-value set
        node.setScaleY(originalValue);
        t0.setFromY(Double.NaN);
        t0.setToY(toValue);
        t0.setByY(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // only by-value set
        node.setScaleY(originalValue);
        t0.setFromY(Double.NaN);
        t0.setToY(Double.NaN);
        t0.setByY(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue + byValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // from- and to-values set
        node.setScaleY(originalValue);
        t0.setFromY(fromValue);
        t0.setToY(toValue);
        t0.setByY(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // from- and by-values set
        node.setScaleY(originalValue);
        t0.setFromY(fromValue);
        t0.setToY(Double.NaN);
        t0.setByY(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue + byValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // to- and by-values set
        node.setScaleY(originalValue);
        t0.setFromY(Double.NaN);
        t0.setToY(toValue);
        t0.setByY(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);

        // all values set
        node.setScaleY(originalValue);
        t0.setFromY(fromValue);
        t0.setToY(toValue);
        t0.setByY(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleY(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleY(), EPSILON);
        AnimationShim.finished(t0);
    }

    @Test
    public void testZValueCombinations() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        final double originalValue = 0.6;
        final double fromValue = 0.4;
        final double toValue = 0.9;
        final double byValue = -0.2;

        // no value set
        node.setScaleZ(originalValue);
        t0.setFromZ(Double.NaN);
        t0.setToZ(Double.NaN);
        t0.setByZ(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // only from-value set
        node.setScaleZ(originalValue);
        t0.setFromZ(fromValue);
        t0.setToZ(Double.NaN);
        t0.setByZ(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // only to-value set
        node.setScaleZ(originalValue);
        t0.setFromZ(Double.NaN);
        t0.setToZ(toValue);
        t0.setByZ(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // only by-value set
        node.setScaleZ(originalValue);
        t0.setFromZ(Double.NaN);
        t0.setToZ(Double.NaN);
        t0.setByZ(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(originalValue + byValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // from- and to-values set
        node.setScaleZ(originalValue);
        t0.setFromZ(fromValue);
        t0.setToZ(toValue);
        t0.setByZ(0.0);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // from- and by-values set
        node.setScaleZ(originalValue);
        t0.setFromZ(fromValue);
        t0.setToZ(Double.NaN);
        t0.setByZ(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(fromValue + byValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // to- and by-values set
        node.setScaleZ(originalValue);
        t0.setFromZ(Double.NaN);
        t0.setToZ(toValue);
        t0.setByZ(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(originalValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // all values set
        node.setScaleZ(originalValue);
        t0.setFromZ(fromValue);
        t0.setToZ(toValue);
        t0.setByZ(byValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertEquals(fromValue, node.getScaleZ(), EPSILON);
        TransitionShim.interpolate(t0,1.0);
        assertEquals(toValue, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);
    }

    @Test
    public void testGetTargetNode() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        t0.setInterpolator(Interpolator.LINEAR);
        t0.setFromX(0.5);
        t0.setToX(1.0);
        final Rectangle node2 = new Rectangle();
        final ParallelTransition pt = new ParallelTransition();
        pt.getChildren().add(t0);
        pt.setNode(node2);

        // node set, parent set
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.75, node.getScaleX(), EPSILON);
        assertEquals(1.0, node2.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // node null, parent set
        t0.setNode(null);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.4);
        assertEquals(0.75, node.getScaleX(), EPSILON);
        assertEquals(0.7, node2.getScaleX(), EPSILON);
        AnimationShim.finished(t0);

        // node null, parent null
        pt.setNode(null);
        assertFalse(AnimationShim.startable(t0,true));
    }

    @Test
    public void testCachedValues() {
        final ScaleTransition t0 = new ScaleTransition(ONE_SEC, node);
        t0.setInterpolator(Interpolator.LINEAR);
        t0.setFromX(0.5);
        t0.setToX(1.0);
        t0.setFromY(1.5);
        t0.setToY(2.0);
        t0.setFromZ(1.5);
        t0.setToZ(0.5);

        // start
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        t0.setFromX(0.0);
        t0.setFromY(-1.0);
        t0.setFromZ(0.5);
        TransitionShim.interpolate(t0,0.5);
        assertEquals(0.75, node.getScaleX(), EPSILON);
        assertEquals(1.75, node.getScaleY(), EPSILON);
        assertEquals(1.0,  node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);
        t0.setFromX(0.5);
        t0.setFromY(1.5);
        t0.setFromZ(1.5);

        // end
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        t0.setToX(0.0);
        t0.setFromY(-1.0);
        t0.setFromZ(1.5);
        TransitionShim.interpolate(t0,0.2);
        assertEquals(0.6, node.getScaleX(), EPSILON);
        assertEquals(1.6, node.getScaleY(), EPSILON);
        assertEquals(1.3, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);
        t0.setToX(1.0);
        t0.setToY(2.0);
        t0.setToZ(0.5);

        // node
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        t0.setNode(null);
        TransitionShim.interpolate(t0,0.7);
        assertEquals(0.85, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);
        t0.setNode(node);

        // interpolator
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        t0.setInterpolator(null);
        TransitionShim.interpolate(t0,0.1);
        assertEquals(0.55, node.getScaleX(), EPSILON);
        AnimationShim.finished(t0);
        t0.setInterpolator(Interpolator.LINEAR);
    }

    @Test
    public void testStartable() {
        final ScaleTransition t0 = new ScaleTransition(Duration.ONE, node);
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
        final ScaleTransition t0 = new ScaleTransition(Duration.INDEFINITE, node);
        t0.setToX(2.0);
        t0.setToY(2.0);
        t0.setToZ(2.0);

        // first run
        node.setScaleX( 0.6);
        node.setScaleY( 1.6);
        node.setScaleZ(-0.6);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        node.setScaleX(0.8);
        node.setScaleY(0.8);
        node.setScaleZ(0.8);
        TransitionShim.interpolate(t0,0.0);
        assertEquals( 0.6, node.getScaleX(), EPSILON);
        assertEquals( 1.6, node.getScaleY(), EPSILON);
        assertEquals(-0.6, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);

        // second run
        node.setScaleX( 0.2);
        node.setScaleY(-2.2);
        node.setScaleZ(11.2);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        node.setScaleX(0.8);
        node.setScaleY(0.8);
        node.setScaleZ(0.8);
        TransitionShim.interpolate(t0,0.0);
        assertEquals( 0.2, node.getScaleX(), EPSILON);
        assertEquals(-2.2, node.getScaleY(), EPSILON);
        assertEquals(11.2, node.getScaleZ(), EPSILON);
        AnimationShim.finished(t0);
    }

}
