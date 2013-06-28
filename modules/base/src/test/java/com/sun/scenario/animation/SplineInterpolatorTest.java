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
package com.sun.scenario.animation;

import javafx.animation.Interpolator;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SplineInterpolatorTest {

    private SplineInterpolator interpolator;

    @Before
    public void setUp() throws Exception {
        interpolator = new SplineInterpolator(0.2, 0.1, 0.3, 0.4);
    }

    private static void testEqualsAndHashCode(Interpolator one, Interpolator another) {
        assertTrue(one.equals(another));
        assertTrue(another.equals(one));
        assertEquals(one.hashCode(), another.hashCode());
    }

    private static void testNotEqualsAndHashCode(Interpolator one, Interpolator another) {
        assertFalse(one.equals(another));
        assertFalse(another.equals(one));
        assertFalse(one.hashCode() == another.hashCode());
    }

    @Test
    public void testEqualsAndHashCode() {
        Interpolator another = new SplineInterpolator(0.2, 0.1, 0.3, 0.4);
        testEqualsAndHashCode(interpolator, another);
    }

    @Test
    public void testNotEqualsAndHashCode() {
        Interpolator another = new SplineInterpolator(0.2, 0.1, 0.3, 0.5);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.3, 0.5, 0.2, 0.1);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.2, 0.1, 0.6, 0.4);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.2, 0.14, 0.3, 0.4);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.25, 0.1, 0.3, 0.4);
        testNotEqualsAndHashCode(interpolator, another);
    }
}
