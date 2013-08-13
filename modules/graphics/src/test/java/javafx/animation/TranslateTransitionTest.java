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
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Test;

public class TranslateTransitionTest {
	
	private static Duration DEFAULT_DURATION = Duration.millis(400);
	private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;
	
	private static double EPSILON = 1e-12;
	private static Duration ONE_SEC = Duration.millis(1000);
	private static Duration TWO_SECS = Duration.millis(2000);
	
	private Node node;
	
	@Before
	public void setUp() {
		node = new Rectangle();
	}
	
	@Test
	public void testDefaultValues() {
		// empty ctor
		final TranslateTransition t0 = new TranslateTransition();
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
		final TranslateTransition t1 = new TranslateTransition(ONE_SEC);
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
		final TranslateTransition t2 = new TranslateTransition(TWO_SECS, node);
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
        final TranslateTransition t0 = new TranslateTransition();
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
        final TranslateTransition t1 = new TranslateTransition(ONE_SEC);
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
        final TranslateTransition t2 = new TranslateTransition(TWO_SECS, node);
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
		final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
		t0.setFromX(0.5);
		t0.setToX(1.0);
		t0.setFromY(1.5);
		t0.setToY(2.0);
		t0.setFromZ(1.5);
		t0.setToZ(0.5);
		
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(0.5, node.getTranslateX(), EPSILON);
		assertEquals(1.5, node.getTranslateY(), EPSILON);
		assertEquals(1.5, node.getTranslateZ(), EPSILON);
		t0.interpolate(0.4);
		assertEquals(0.7, node.getTranslateX(), EPSILON);
		assertEquals(1.7, node.getTranslateY(), EPSILON);
		assertEquals(1.1, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(1.0, node.getTranslateX(), EPSILON);
		assertEquals(2.0, node.getTranslateY(), EPSILON);
		assertEquals(0.5, node.getTranslateZ(), EPSILON);
        t0.impl_finished();
	}
	
	@Test
	public void testXValueCombinations() {
		final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
		final double originalValue = 0.6;
		final double fromValue = 0.4;
		final double toValue = 0.9;
		final double byValue = -0.2;

		// no value set
		node.setTranslateX(originalValue);
		t0.setFromX(Double.NaN);
		t0.setToX(Double.NaN);
		t0.setByX(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// only from-value set
		node.setTranslateX(originalValue);
		t0.setFromX(fromValue);
		t0.setToX(Double.NaN);
		t0.setByX(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// only to-value set
		node.setTranslateX(originalValue);
		t0.setFromX(Double.NaN);
		t0.setToX(toValue);
		t0.setByX(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// only by-value set
		node.setTranslateX(originalValue);
		t0.setFromX(Double.NaN);
		t0.setToX(Double.NaN);
		t0.setByX(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue + byValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// from- and to-values set
		node.setTranslateX(originalValue);
		t0.setFromX(fromValue);
		t0.setToX(toValue);
		t0.setByX(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// from- and by-values set
		node.setTranslateX(originalValue);
		t0.setFromX(fromValue);
		t0.setToX(Double.NaN);
		t0.setByX(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue + byValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// to- and by-values set
		node.setTranslateX(originalValue);
		t0.setFromX(Double.NaN);
		t0.setToX(toValue);
		t0.setByX(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();

		// all values set
		node.setTranslateX(originalValue);
		t0.setFromX(fromValue);
		t0.setToX(toValue);
		t0.setByX(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateX(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateX(), EPSILON);
		t0.impl_finished();
	}

	@Test
	public void testYValueCombinations() {
		final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
		final double originalValue = 0.6;
		final double fromValue = 0.4;
		final double toValue = 0.9;
		final double byValue = -0.2;

		// no value set
		node.setTranslateY(originalValue);
		t0.setFromY(Double.NaN);
		t0.setToY(Double.NaN);
		t0.setByY(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// only from-value set
		node.setTranslateY(originalValue);
		t0.setFromY(fromValue);
		t0.setToY(Double.NaN);
		t0.setByY(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// only to-value set
		node.setTranslateY(originalValue);
		t0.setFromY(Double.NaN);
		t0.setToY(toValue);
		t0.setByY(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// only by-value set
		node.setTranslateY(originalValue);
		t0.setFromY(Double.NaN);
		t0.setToY(Double.NaN);
		t0.setByY(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue + byValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// from- and to-values set
		node.setTranslateY(originalValue);
		t0.setFromY(fromValue);
		t0.setToY(toValue);
		t0.setByY(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// from- and by-values set
		node.setTranslateY(originalValue);
		t0.setFromY(fromValue);
		t0.setToY(Double.NaN);
		t0.setByY(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue + byValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// to- and by-values set
		node.setTranslateY(originalValue);
		t0.setFromY(Double.NaN);
		t0.setToY(toValue);
		t0.setByY(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();

		// all values set
		node.setTranslateY(originalValue);
		t0.setFromY(fromValue);
		t0.setToY(toValue);
		t0.setByY(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateY(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateY(), EPSILON);
		t0.impl_finished();
	}

	@Test
	public void testZValueCombinations() {
		final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
		final double originalValue = 0.6;
		final double fromValue = 0.4;
		final double toValue = 0.9;
		final double byValue = -0.2;

		// no value set
		node.setTranslateZ(originalValue);
		t0.setFromZ(Double.NaN);
		t0.setToZ(Double.NaN);
		t0.setByZ(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// only from-value set
		node.setTranslateZ(originalValue);
		t0.setFromZ(fromValue);
		t0.setToZ(Double.NaN);
		t0.setByZ(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// only to-value set
		node.setTranslateZ(originalValue);
		t0.setFromZ(Double.NaN);
		t0.setToZ(toValue);
		t0.setByZ(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// only by-value set
		node.setTranslateZ(originalValue);
		t0.setFromZ(Double.NaN);
		t0.setToZ(Double.NaN);
		t0.setByZ(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(originalValue + byValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// from- and to-values set
		node.setTranslateZ(originalValue);
		t0.setFromZ(fromValue);
		t0.setToZ(toValue);
		t0.setByZ(0.0);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// from- and by-values set
		node.setTranslateZ(originalValue);
		t0.setFromZ(fromValue);
		t0.setToZ(Double.NaN);
		t0.setByZ(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(fromValue + byValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// to- and by-values set
		node.setTranslateZ(originalValue);
		t0.setFromZ(Double.NaN);
		t0.setToZ(toValue);
		t0.setByZ(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(originalValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();

		// all values set
		node.setTranslateZ(originalValue);
		t0.setFromZ(fromValue);
		t0.setToZ(toValue);
		t0.setByZ(byValue);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		t0.interpolate(0.0);
		assertEquals(fromValue, node.getTranslateZ(), EPSILON);
		t0.interpolate(1.0);
		assertEquals(toValue, node.getTranslateZ(), EPSILON);
		t0.impl_finished();
	}

    @Test
    public void testGetTargetNode() {
        final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
        t0.setInterpolator(Interpolator.LINEAR);
		t0.setFromX(0.5);
		t0.setToX(1.0);
        final Rectangle node2 = new Rectangle();
        final ParallelTransition pt = new ParallelTransition();
        pt.getChildren().add(t0);
        pt.setNode(node2);

        // node set, parent set
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.interpolate(0.5);
        assertEquals(0.75, node.getTranslateX(), EPSILON);
        assertEquals(0.0, node2.getTranslateX(), EPSILON);
        t0.impl_finished();

        // node null, parent set
        t0.setNode(null);
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.interpolate(0.4);
        assertEquals(0.75, node.getTranslateX(), EPSILON);
        assertEquals(0.7, node2.getTranslateX(), EPSILON);
        t0.impl_finished();

        // node null, parent null
        pt.setNode(null);
        assertFalse(t0.impl_startable(true));
    }

    @Test
    public void testCachedValues() {
        final TranslateTransition t0 = new TranslateTransition(ONE_SEC, node);
        t0.setInterpolator(Interpolator.LINEAR);
		t0.setFromX(0.5);
		t0.setToX(1.0);
		t0.setFromY(1.5);
		t0.setToY(2.0);
		t0.setFromZ(1.5);
		t0.setToZ(0.5);

        // start
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.setFromX(0.0);
        t0.setFromY(-1.0);
        t0.setFromZ(0.5);
        t0.interpolate(0.5);
        assertEquals(0.75, node.getTranslateX(), EPSILON);
        assertEquals(1.75, node.getTranslateY(), EPSILON);
        assertEquals(1.0,  node.getTranslateZ(), EPSILON);
        t0.impl_finished();
        t0.setFromX(0.5);
		t0.setFromY(1.5);
		t0.setFromZ(1.5);

        // end
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.setToX(0.0);
        t0.setFromY(-1.0);
        t0.setFromZ(1.5);
        t0.interpolate(0.2);
        assertEquals(0.6, node.getTranslateX(), EPSILON);
        assertEquals(1.6, node.getTranslateY(), EPSILON);
        assertEquals(1.3, node.getTranslateZ(), EPSILON);
        t0.impl_finished();
        t0.setToX(1.0);
		t0.setToY(2.0);
		t0.setToZ(0.5);

        // node
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.setNode(null);
        t0.interpolate(0.7);
        assertEquals(0.85, node.getTranslateX(), EPSILON);
        t0.impl_finished();
        t0.setNode(node);

        // interpolator
        assertTrue(t0.impl_startable(true));
        t0.impl_start(true);
        t0.setInterpolator(null);
        t0.interpolate(0.1);
        assertEquals(0.55, node.getTranslateX(), EPSILON);
        t0.impl_finished();
        t0.setInterpolator(Interpolator.LINEAR);
    }

	@Test
	public void testStartable() {
		final TranslateTransition t0 = new TranslateTransition(Duration.ONE, node);
		assertTrue(t0.impl_startable(true));
		
		// duration is 0
		t0.setDuration(Duration.ZERO);
		assertFalse(t0.impl_startable(true));
		t0.setDuration(Duration.ONE);
		assertTrue(t0.impl_startable(true));
		
		// node is null
		t0.setNode(null);
		assertFalse(t0.impl_startable(true));
		t0.setNode(node);
		assertTrue(t0.impl_startable(true));
		
		// interpolator is null
		t0.setInterpolator(null);
		assertFalse(t0.impl_startable(true));
		t0.setInterpolator(Interpolator.LINEAR);
		assertTrue(t0.impl_startable(true));
	}

	@Test
	public void testEvaluateStartValue() {
		final TranslateTransition t0 = new TranslateTransition(Duration.INDEFINITE, node);
        t0.setToX(2.0);
        t0.setToY(2.0);
        t0.setToZ(2.0);
		
		// first run
		node.setTranslateX( 0.6);
		node.setTranslateY( 1.6);
		node.setTranslateZ(-0.6);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		node.setTranslateX(0.8);
		node.setTranslateY(0.8);
		node.setTranslateZ(0.8);
		t0.interpolate(0.0);
		assertEquals( 0.6, node.getTranslateX(), EPSILON);
		assertEquals( 1.6, node.getTranslateY(), EPSILON);
		assertEquals(-0.6, node.getTranslateZ(), EPSILON);
		t0.impl_finished();
		
		// second run
		node.setTranslateX( 0.2);
		node.setTranslateY(-2.2);
		node.setTranslateZ(11.2);
		assertTrue(t0.impl_startable(true));
		t0.impl_start(true);
		node.setTranslateX(0.8);
		node.setTranslateY(0.8);
		node.setTranslateZ(0.8);
		t0.interpolate(0.0);
		assertEquals( 0.2, node.getTranslateX(), EPSILON);
		assertEquals(-2.2, node.getTranslateY(), EPSILON);
		assertEquals(11.2, node.getTranslateZ(), EPSILON);
		t0.impl_finished();
	}

}
