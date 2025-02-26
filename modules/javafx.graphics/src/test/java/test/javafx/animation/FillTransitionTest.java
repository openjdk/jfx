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
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TransitionShim;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FillTransitionTest {

    private static Duration DEFAULT_DURATION = Duration.millis(400);
    private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;

    private static float EPSILON = 1e-6f;
    private static Duration ONE_SEC = Duration.millis(1000);
    private static Duration TWO_SECS = Duration.millis(2000);

    private Shape shape;

    @BeforeEach
    public void setUp() {
        shape = new Rectangle();
    }

    private void assertColorEquals(Color expected, Paint actualPaint) {
        assertTrue(actualPaint instanceof Color);
        final Color actual = (Color)actualPaint;
        assertEquals(expected.getRed(), actual.getRed(), EPSILON);
        assertEquals(expected.getGreen(), actual.getGreen(), EPSILON);
        assertEquals(expected.getBlue(), actual.getBlue(), EPSILON);
        assertEquals(expected.getOpacity(), actual.getOpacity(), EPSILON);
    }

    @Test
    public void testDefaultValues() {
        // empty ctor
        FillTransition t0 = new FillTransition();
        assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
        assertNull(t0.getFromValue());
        assertNull(t0.getToValue());
        assertNull(t0.getShape());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration only
        t0 = new FillTransition(ONE_SEC);
        assertEquals(ONE_SEC, t0.getDuration());
        assertNull(t0.getFromValue());
        assertNull(t0.getToValue());
        assertNull(t0.getShape());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration and shape
        t0 = new FillTransition(TWO_SECS, shape);
        assertEquals(TWO_SECS, t0.getDuration());
        assertNull(t0.getFromValue());
        assertNull(t0.getToValue());
        assertEquals(shape, t0.getShape());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration and values
        t0 = new FillTransition(TWO_SECS, Color.BLACK, Color.WHITE);
        assertEquals(TWO_SECS, t0.getDuration());
        assertColorEquals(Color.BLACK, t0.getFromValue());
        assertColorEquals(Color.WHITE, t0.getToValue());
        assertNull(t0.getShape());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // duration, shape, and values
        t0 = new FillTransition(TWO_SECS, shape, Color.BLACK, Color.WHITE);
        assertEquals(TWO_SECS, t0.getDuration());
        assertColorEquals(Color.BLACK, t0.getFromValue());
        assertColorEquals(Color.WHITE, t0.getToValue());
        assertEquals(shape, t0.getShape());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());
    }

    @Test
    public void testInterpolate() {
        final Color fromValue = Color.color(0.2, 0.3, 0.7, 0.1);
        final Color toValue = Color.color(0.8, 0.4, 0.2, 0.9);
        final FillTransition t0 = new FillTransition(ONE_SEC, shape, fromValue, toValue);

        assertTrue(AnimationShim.startable(t0,false));
        AnimationShim.doStart(t0,false);
        TransitionShim.interpolate(t0,0.0);
        assertColorEquals(Color.color(0.2, 0.3, 0.7, 0.1), shape.getFill());
        TransitionShim.interpolate(t0,0.4);
        assertColorEquals(Color.color(0.44, 0.34, 0.5, 0.42), shape.getFill());
        TransitionShim.interpolate(t0,1.0);
        assertColorEquals(Color.color(0.8, 0.4, 0.2, 0.9), shape.getFill());
        AnimationShim.finished(t0);
    }

    @Test
    public void testValueCombinations() {
        final FillTransition t0 = new FillTransition(ONE_SEC, shape, null, Color.WHITE);
        final double original = 0.6;
        final double from = 0.4;
        final Color originalValue = Color.color(original, original, original);
        final Color fromValue = Color.color(from, from, from);

        // no from value set
        shape.setFill(originalValue);
        t0.setFromValue(null);
        assertTrue(AnimationShim.startable(t0,false));
        AnimationShim.doStart(t0,false);
        TransitionShim.interpolate(t0,0.0);
        assertColorEquals(originalValue, shape.getFill());
        AnimationShim.finished(t0);

        // from-value set
        shape.setFill(originalValue);
        t0.setFromValue(fromValue);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        TransitionShim.interpolate(t0,0.0);
        assertColorEquals(fromValue, shape.getFill());
        AnimationShim.finished(t0);
    }

    @Test
    public void testGetTargetNode() {
        final Color fromValue = Color.color(0.0, 0.4, 0.8, 1.0);
        final Color toValue = Color.color(1.0, 0.8, 0.6, 0.4);
        final FillTransition ft = new FillTransition(ONE_SEC, shape, fromValue, toValue);
        ft.setInterpolator(Interpolator.LINEAR);
        final Shape shape2 = new Rectangle();
        final ParallelTransition pt = new ParallelTransition();
        pt.getChildren().add(ft);
        pt.setNode(shape2);
        shape.setFill(Color.WHITE);
        shape2.setFill(Color.WHITE);

        // node set, parent set
        assertTrue(AnimationShim.startable(ft,false));
        AnimationShim.doStart(ft,false);
        TransitionShim.interpolate(ft,0.5);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.7), shape.getFill());
        assertColorEquals(Color.WHITE, shape2.getFill());
        AnimationShim.finished(ft);

        // node null, parent set
        ft.setShape(null);
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        TransitionShim.interpolate(ft,0.4);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.7), shape.getFill());
        assertColorEquals(Color.color(0.4, 0.56, 0.72, 0.76), shape2.getFill());
        AnimationShim.finished(ft);

        // node null, parent not shape set
        pt.setNode(new Group());
        assertFalse(AnimationShim.startable(ft,true));

        // node null, parent null
        pt.setNode(null);
        assertFalse(AnimationShim.startable(ft,true));
    }

    @Test
    public void testCachedValues() {
        final Color fromValue = Color.color(0.0, 0.4, 0.8, 0.2);
        final Color toValue = Color.color(1.0, 0.8, 0.6, 0.4);
        final FillTransition ft = new FillTransition(ONE_SEC, shape, fromValue, toValue);
        ft.setInterpolator(Interpolator.LINEAR);

        // start
        assertTrue(AnimationShim.startable(ft,false));
        AnimationShim.doStart(ft,false);
        ft.setFromValue(Color.WHITE);
        TransitionShim.interpolate(ft,0.5);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.3), shape.getFill());
        AnimationShim.finished(ft);
        ft.setFromValue(fromValue);

        // end
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setToValue(Color.BLACK);
        TransitionShim.interpolate(ft,0.2);
        assertColorEquals(Color.color(0.2, 0.48, 0.76, 0.24), shape.getFill());
        AnimationShim.finished(ft);
        ft.setToValue(toValue);

        // shape
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setShape(null);
        TransitionShim.interpolate(ft,0.7);
        assertColorEquals(Color.color(0.7, 0.68, 0.66, 0.34), shape.getFill());
        AnimationShim.finished(ft);
        ft.setShape(shape);

        // interpolator
        assertTrue(AnimationShim.startable(ft,true));
        AnimationShim.doStart(ft,true);
        ft.setInterpolator(null);
        TransitionShim.interpolate(ft,0.1);
        assertColorEquals(Color.color(0.1, 0.44, 0.78, 0.22), shape.getFill());
        AnimationShim.finished(ft);
        ft.setInterpolator(Interpolator.LINEAR);
    }

    @Test
    public void testStartable_unsynchronized() {
        final FillTransition t0 = new FillTransition(Duration.ONE, shape, Color.WHITE, Color.BLACK);
        final Paint paint2 = new LinearGradient(0, 0, 1, 1, false, null,
                new Stop[] { new Stop(0, Color.RED) });
        assertTrue(AnimationShim.startable(t0,true));

        // duration is 0
        t0.setDuration(Duration.ZERO);
        assertFalse(AnimationShim.startable(t0,true));
        t0.setDuration(Duration.ONE);
        assertTrue(AnimationShim.startable(t0,true));

        // interpolator is null
        t0.setInterpolator(null);
        assertFalse(AnimationShim.startable(t0,true));
        t0.setInterpolator(Interpolator.LINEAR);
        assertTrue(AnimationShim.startable(t0,true));

        // shape is null
        t0.setShape(null);
        assertFalse(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        t0.setShape(shape);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));

        // fromValue
        t0.setFromValue(null);
        shape.setFill(paint2);
        assertFalse(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        shape.setFill(Color.BLACK);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));
        t0.setFromValue(Color.WHITE);
        shape.setFill(paint2);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));

        // toValue
        t0.setToValue(null);
        assertFalse(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        t0.setToValue(Color.BLACK);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));
    }

    @Test
    public void testStartable_synchronized() {
        final FillTransition t0 = new FillTransition(Duration.ONE, shape, Color.WHITE, Color.BLACK);
        final Paint paint2 = new LinearGradient(0, 0, 1, 1, false, null,
                new Stop[] { new Stop(0, Color.RED) });
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        AnimationShim.finished(t0);

        // shape is null
        t0.setShape(null);
        assertTrue(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        t0.setShape(shape);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));

        // fromValue
        t0.setFromValue(null);
        shape.setFill(paint2);
        assertTrue(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        shape.setFill(Color.BLACK);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));
        t0.setFromValue(Color.WHITE);
        shape.setFill(paint2);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));

        // toValue
        t0.setToValue(null);
        assertTrue(AnimationShim.startable(t0,false));
        assertFalse(AnimationShim.startable(t0,true));
        t0.setToValue(Color.BLACK);
        assertTrue(AnimationShim.startable(t0,false));
        assertTrue(AnimationShim.startable(t0,true));
    }

    @Test
    public void testEvaluateStartValue() {
        final FillTransition t0 = new FillTransition(Duration.INDEFINITE, shape, null, Color.WHITE);

        // first run
        shape.setFill(Color.GREY);
        assertTrue(AnimationShim.startable(t0,false));
        AnimationShim.doStart(t0,false);
        shape.setFill(Color.TRANSPARENT);
        TransitionShim.interpolate(t0,0.0);
        assertColorEquals(Color.GREY, shape.getFill());
        AnimationShim.finished(t0);

        // second run
        shape.setFill(Color.BLACK);
        assertTrue(AnimationShim.startable(t0,true));
        AnimationShim.doStart(t0,true);
        shape.setFill(Color.WHITE);
        TransitionShim.interpolate(t0,0.0);
        assertColorEquals(Color.BLACK, shape.getFill());
        AnimationShim.finished(t0);
    }

}
