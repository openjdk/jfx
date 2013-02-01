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
package com.sun.scenario.animation;


import static org.junit.Assert.assertEquals;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Test;

public class NumberTangentInterpolatorTest {
	
	private static final double EPSILON_DOUBLE = 1e-12;
	private static final double EPSILON_FLOAT = 1e-6;

	private NumberTangentInterpolator interpolator;
	
	@Before
	public void setUp() throws Exception {
		interpolator = NumberTangentInterpolator.create(0, Duration.ZERO);
	}
	
	@Test
	public void testCreate() {
		final NumberTangentInterpolator interpolator1 = NumberTangentInterpolator.create(Math.PI, Duration.millis(2000));
		assertEquals(Math.PI, interpolator1.getInValue(), EPSILON_DOUBLE);
		assertEquals(12000.0, interpolator1.getInTicks(), EPSILON_DOUBLE);
		assertEquals(Math.PI, interpolator1.getOutValue(), EPSILON_DOUBLE);
		assertEquals(12000.0, interpolator1.getOutTicks(), EPSILON_DOUBLE);

		final NumberTangentInterpolator interpolator2 = NumberTangentInterpolator.create(Math.E, Duration.millis(500), -Math.PI, Duration.millis(1000));
		assertEquals(Math.E, interpolator2.getInValue(), EPSILON_DOUBLE);
		assertEquals(3000.0, interpolator2.getInTicks(), EPSILON_DOUBLE);
		assertEquals(-Math.PI, interpolator2.getOutValue(), EPSILON_DOUBLE);
		assertEquals(6000.0, interpolator2.getOutTicks(), EPSILON_DOUBLE);
	}
	
	@Test
	public void testInterpolate_boolean() {
		assertEquals(false, interpolator.interpolate(false, true, 0.0));
		assertEquals(false, interpolator.interpolate(false, true, 0.5));
		assertEquals(true, interpolator.interpolate(false, true, 1.0));
	}

	@Test
	public void testInterpolate_double() {
		assertEquals( 0.0, interpolator.interpolate(0.0, 10.0, 0.0), EPSILON_DOUBLE);
		assertEquals( 5.0, interpolator.interpolate(0.0, 10.0, 0.5), EPSILON_DOUBLE);
		assertEquals(10.0, interpolator.interpolate(0.0, 10.0, 1.0), EPSILON_DOUBLE);
	}

	@Test
	public void testInterpolate_int() {
		assertEquals( 0, interpolator.interpolate(0, 10, 0.0));
		assertEquals( 5, interpolator.interpolate(0, 10, 0.5));
		assertEquals(10, interpolator.interpolate(0, 10, 1.0));
	}

	@Test
	public void testInterpolate_long() {
		assertEquals( 0L, interpolator.interpolate(0L, 10L, 0.0));
		assertEquals( 5L, interpolator.interpolate(0L, 10L, 0.5));
		assertEquals(10L, interpolator.interpolate(0L, 10L, 1.0));
	}

	@Test
	public void testInterpolate_float() {
		assertEquals( 0.0f, interpolator.interpolate(0.0f, 10.0f, 0.0), EPSILON_FLOAT);
		assertEquals( 5.0f, interpolator.interpolate(0.0f, 10.0f, 0.5), EPSILON_FLOAT);
		assertEquals(10.0f, interpolator.interpolate(0.0f, 10.0f, 1.0), EPSILON_FLOAT);
	}

	@Test
	public void testInterpolate_Object() {
		assertEquals("Hello World", interpolator.interpolate("Hello World", "Goodbye World", 0.0));
		assertEquals("Hello World", interpolator.interpolate("Hello World", "Goodbye World", 0.5));
		assertEquals("Goodbye World", interpolator.interpolate("Hello World", "Goodbye World", 1.0));
	}

	@Test
	public void testInterpolate_Number() {
		assertEquals(Integer.valueOf( 0), interpolator.interpolate(Integer.valueOf(0), Integer.valueOf(10), 0.0));
		assertEquals(Integer.valueOf( 5), interpolator.interpolate(Integer.valueOf(0), Integer.valueOf(10), 0.5));
		assertEquals(Integer.valueOf(10), interpolator.interpolate(Integer.valueOf(0), Integer.valueOf(10), 1.0));
	}

}
