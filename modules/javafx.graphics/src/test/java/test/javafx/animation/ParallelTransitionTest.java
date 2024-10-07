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

import java.util.Arrays;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelTransitionTest {

    private static Interpolator DEFAULT_INTERPOLATOR = Interpolator.LINEAR;

    private static Duration ONE_SEC = Duration.millis(1000);
    private static Duration TWO_SECS = Duration.millis(2000);
    private static Duration THREE_SECS = Duration.millis(3000);

    private Node node;
    private Animation child1;
    private Animation child2;
    private Animation child3;

    @BeforeEach
    public void setUp() {
        node = new Rectangle();
        child1 = new AnimationDummy(ONE_SEC);
        child2 = new AnimationDummy(TWO_SECS);
        child3 = new AnimationDummy(THREE_SECS);
    }

    @Test
    public void testDefaultValues() {
        // empty ctor
        final ParallelTransition t0 = new ParallelTransition();
        assertEquals(Duration.ZERO, t0.getTotalDuration());
        assertNull(t0.getNode());
        assertNull(t0.nodeProperty().get());
        assertTrue(t0.getChildren().isEmpty());
        assertEquals(DEFAULT_INTERPOLATOR, t0.getInterpolator());
        assertNull(t0.getOnFinished());

        // node only
        final ParallelTransition t1 = new ParallelTransition(node);
        assertEquals(Duration.ZERO, t1.getTotalDuration());
        assertEquals(node, t1.getNode());
        assertEquals(node, t1.nodeProperty().get());
        assertTrue(t1.getChildren().isEmpty());
        assertEquals(DEFAULT_INTERPOLATOR, t1.getInterpolator());
        assertNull(t1.getOnFinished());

        // child animations only
        final ParallelTransition t2 = new ParallelTransition(child1, child2, child3);
        assertEquals(THREE_SECS, t2.getTotalDuration());
        assertNull(t2.getNode());
        assertNull(t2.nodeProperty().get());
        assertEquals(Arrays.asList(child1, child2, child3), t2.getChildren());
        assertEquals(DEFAULT_INTERPOLATOR, t2.getInterpolator());
        assertNull(t2.getOnFinished());

        // node and child animations
        final ParallelTransition t3 = new ParallelTransition(node, child1, child2, child3);
        assertEquals(THREE_SECS, t3.getTotalDuration());
        assertEquals(node, t3.getNode());
        assertEquals(node, t3.nodeProperty().get());
        assertEquals(Arrays.asList(child1, child2, child3), t3.getChildren());
        assertEquals(DEFAULT_INTERPOLATOR, t3.getInterpolator());
        assertNull(t3.getOnFinished());
    }
}
