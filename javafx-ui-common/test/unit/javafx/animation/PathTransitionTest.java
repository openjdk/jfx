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
import static org.junit.Assert.assertNull;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.junit.Before;
import org.junit.Test;

public class PathTransitionTest {

    private static Duration DEFAULT_DURATION = Duration.millis(400);
	private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.EASE_BOTH;
	
	private static Duration ONE_SEC = Duration.millis(1000);
	
	private Shape path;
	private Node node;
	
	@Before
	public void setUp() {
		path = new Circle();
		node = new Rectangle();
	}
	
	@Test
	public void testDefaultValues() {
		// empty ctor
		final PathTransition t0 = new PathTransition();
        assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
		assertNull(t0.getNode());
		assertNull(t0.nodeProperty().get());
		assertNull(t0.getPath());
		assertNull(t0.pathProperty().get());
		assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
		assertNull(t0.getOnFinished());
		
		// duration and path
		final PathTransition t1 = new PathTransition(ONE_SEC, path);
		assertEquals(ONE_SEC, t1.getTotalDuration());
		assertNull(t1.getNode());
		assertNull(t1.nodeProperty().get());
		assertEquals(path, t1.getPath());
		assertEquals(path, t1.pathProperty().get());
		assertEquals(DEFAULT_INTERPOLATOR, t1.getInterpolator());
		assertNull(t1.getOnFinished());
		
		// duration, path, and node
		final PathTransition t2 = new PathTransition(ONE_SEC, path, node);
		assertEquals(ONE_SEC, t2.getTotalDuration());
		assertEquals(node, t2.getNode());
		assertEquals(node, t2.nodeProperty().get());
		assertEquals(path, t2.getPath());
		assertEquals(path, t2.pathProperty().get());
		assertEquals(DEFAULT_INTERPOLATOR, t2.getInterpolator());
		assertNull(t2.getOnFinished());
	}
}
