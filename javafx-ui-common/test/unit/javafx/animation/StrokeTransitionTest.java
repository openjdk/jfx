/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Test;

public class StrokeTransitionTest {
	
	private static Duration DEFAULT_DURATION = Duration.millis(400);
	private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;
	
	private static float EPSILON = 1e-6f;
	private static Duration ONE_SEC = Duration.millis(1000);
	private static Duration TWO_SECS = Duration.millis(2000);
	
	private Shape shape;
	
	@Before
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
		StrokeTransition t0 = new StrokeTransition();
		assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
		assertNull(t0.getFromValue());
		assertNull(t0.getToValue());
		assertNull(t0.getShape());
		assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
		assertNull(t0.getOnFinished());
		
		// duration only
		t0 = new StrokeTransition(ONE_SEC);
		assertEquals(ONE_SEC, t0.getDuration());
		assertNull(t0.getFromValue());
		assertNull(t0.getToValue());
		assertNull(t0.getShape());
		assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
		assertNull(t0.getOnFinished());
		
		// duration and shape
		t0 = new StrokeTransition(TWO_SECS, shape);
		assertEquals(TWO_SECS, t0.getDuration());
		assertNull(t0.getFromValue());
		assertNull(t0.getToValue());
		assertEquals(shape, t0.getShape());
		assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
		assertNull(t0.getOnFinished());
		
		// duration and values
		t0 = new StrokeTransition(TWO_SECS, null, Color.BLACK, Color.WHITE);
		assertEquals(TWO_SECS, t0.getDuration());
		assertColorEquals(Color.BLACK, t0.getFromValue());
		assertColorEquals(Color.WHITE, t0.getToValue());
		assertNull(t0.getShape());
		assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
		assertNull(t0.getOnFinished());
		
		// duration, shape, and values
		t0 = new StrokeTransition(TWO_SECS, shape, Color.BLACK, Color.WHITE);
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
		final StrokeTransition t0 = new StrokeTransition(ONE_SEC, shape, fromValue, toValue);
		
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertColorEquals(Color.color(0.2, 0.3, 0.7, 0.1), shape.getStroke());
		t0.interpolate(0.4);
		assertColorEquals(Color.color(0.44, 0.34, 0.5, 0.42), shape.getStroke());
		t0.interpolate(1.0);
		assertColorEquals(Color.color(0.8, 0.4, 0.2, 0.9), shape.getStroke());
        t0.impl_finished();
	}
	
	@Test
	public void testRedValueCombinations() {
		final StrokeTransition t0 = new StrokeTransition(ONE_SEC, shape, null, Color.WHITE);
		final double originalRed = 0.6;
		final double fromRed = 0.4;
		final Color originalValue = Color.color(originalRed, 1.0, 1.0);
		final Color fromValue = Color.color(fromRed, 1.0, 1.0);
		
		// no from value set
		shape.setStroke(originalValue);
		t0.setFromValue(null);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertColorEquals(originalValue, shape.getStroke());
		t0.impl_finished();
		
		// from-value set
		shape.setStroke(originalValue);
		t0.setFromValue(fromValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertColorEquals(fromValue, shape.getStroke());
		t0.impl_finished();
	}
	
    @Test
    public void testGetTargetNode() {
    	final Color fromValue = Color.color(0.0, 0.4, 0.8, 1.0);
		final Color toValue = Color.color(1.0, 0.8, 0.6, 0.4);
        final StrokeTransition ft = new StrokeTransition(ONE_SEC, shape, fromValue, toValue);
        ft.setInterpolator(Interpolator.LINEAR);
        final Shape shape2 = new Rectangle();
        final ParallelTransition pt = new ParallelTransition();
        pt.getChildren().add(ft);
        pt.setNode(shape2);
        shape.setStroke(Color.WHITE);
        shape2.setStroke(Color.WHITE);

        // node set, parent set
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.interpolate(0.5);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.7), shape.getStroke());
        assertColorEquals(Color.WHITE, shape2.getStroke());
        ft.impl_finished();

        // node null, parent set
        ft.setShape(null);
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.interpolate(0.4);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.7), shape.getStroke());
        assertColorEquals(Color.color(0.4, 0.56, 0.72, 0.76), shape2.getStroke());
        ft.impl_finished();
        
        // node null, parent not shape set
        pt.setNode(new Group());
        assertFalse(ft.impl_startable(true));

        // node null, parent null
        pt.setNode(null);
        assertFalse(ft.impl_startable(true));
    }

    @Test
    public void testCachedValues() {
        final Color fromValue = Color.color(0.0, 0.4, 0.8, 0.2);
        final Color toValue = Color.color(1.0, 0.8, 0.6, 0.4);
        final StrokeTransition ft = new StrokeTransition(ONE_SEC, shape, fromValue, toValue);
        ft.setInterpolator(Interpolator.LINEAR);

        // start
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.setFromValue(Color.WHITE);
        ft.interpolate(0.5);
        assertColorEquals(Color.color(0.5, 0.6, 0.7, 0.3), shape.getStroke());
        ft.impl_finished();
		ft.setFromValue(fromValue);

        // end
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.setToValue(Color.BLACK);
        ft.interpolate(0.2);
        assertColorEquals(Color.color(0.2, 0.48, 0.76, 0.24), shape.getStroke());
        ft.impl_finished();
        ft.setToValue(toValue);

        // shape
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.setShape(null);
        ft.interpolate(0.7);
        assertColorEquals(Color.color(0.7, 0.68, 0.66, 0.34), shape.getStroke());
        ft.impl_finished();
        ft.setShape(shape);

        // interpolator
        assertTrue(ft.impl_startable(true));
        ft.impl_start(true);
        ft.setInterpolator(null);
        ft.interpolate(0.1);
        assertColorEquals(Color.color(0.1, 0.44, 0.78, 0.22), shape.getStroke());
        ft.impl_finished();
        ft.setInterpolator(Interpolator.LINEAR);
    }

	@Test
	public void testStartable() {
		final StrokeTransition t0 = new StrokeTransition(Duration.ONE, shape, Color.WHITE, Color.BLACK);
		final Paint paint2 = new LinearGradient(0, 0, 1, 1, false, null,
                new Stop[] { new Stop(0, Color.RED) });
		assertTrue(t0.impl_startable(true));
		
		// duration is 0
		t0.setDuration(Duration.ZERO);
		assertFalse(t0.impl_startable(true));
		t0.setDuration(Duration.ONE);
		assertTrue(t0.impl_startable(true));
		
		// shape is null
		t0.setShape(null);
		assertFalse(t0.impl_startable(true));
		t0.setShape(shape);
		assertTrue(t0.impl_startable(true));
		
		// interpolator is null
		t0.setInterpolator(null);
		assertFalse(t0.impl_startable(true));
		t0.setInterpolator(Interpolator.LINEAR);
		assertTrue(t0.impl_startable(true));
		
		// fromValue
		t0.setFromValue(null);
		shape.setStroke(paint2);
		assertFalse(t0.impl_startable(true));
		shape.setStroke(Color.BLACK);
		assertTrue(t0.impl_startable(true));
		t0.setFromValue(Color.WHITE);
		shape.setStroke(paint2);
		assertTrue(t0.impl_startable(true));
		
		// toValue
		t0.setToValue(null);
		assertFalse(t0.impl_startable(true));
		t0.setToValue(Color.BLACK);
		assertTrue(t0.impl_startable(true));
	}

	@Test
	public void testEvaluateStartValue() {
		final StrokeTransition t0 = new StrokeTransition(Duration.INDEFINITE, shape, null, Color.WHITE);
		
		// first run
		shape.setStroke(Color.GREY);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		shape.setStroke(Color.TRANSPARENT);
		t0.interpolate(0.0);
		assertColorEquals(Color.GREY, shape.getStroke());
		t0.impl_finished();
		
		// second run
		shape.setStroke(Color.BLACK);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		shape.setStroke(Color.WHITE);
		t0.interpolate(0.0);
		assertColorEquals(Color.BLACK, shape.getStroke());
		t0.impl_finished();
	}

}
