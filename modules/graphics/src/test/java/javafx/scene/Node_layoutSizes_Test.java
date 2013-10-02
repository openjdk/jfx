/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.shape.Rectangle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests for the layout size methods on Node. In particular, we need to ensure that
 * the min/max width/height are both based on pref by default. Also that these methods
 * never (ever) return NaN or negative values.
 */
public class Node_layoutSizes_Test {
    /**
     * The standard case, where the layout bounds is automatically determined by the bounds of
     * the rectangle. I've offset the rectangle x/y to make sure we're using the width and
     * not the maxX for this calculation.
     */
    @Test public void Node_prefWidth_BasedOnLayoutBounds() {
        Rectangle node = new Rectangle(10, 10, 100, 100);
        assertEquals(100, node.prefWidth(-1), 0);
        assertEquals(100, node.prefWidth(5), 0);
    }

    /**
     * A specialized computeLayoutBounds implementation, to make sure it is the
     * layout bounds from which we're gathering this information.
     */
    @Test public void Node_prefWidth_BasedOnLayoutBounds2() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, 50, 50);
            }
        };
        assertEquals(50, node.prefWidth(-1), 0);
        assertEquals(50, node.prefWidth(5), 0);
    }

    /**
     * If the layout bounds has a NaN, it shouldn't leak out through node.prefWidth
     */
    @Test public void Node_prefWidth_BasedOnLayoutBounds_CleansUpAfterBadBounds() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, Double.NaN, 50);
            }
        };
        assertEquals(0, node.prefWidth(-1), 0);
        assertEquals(0, node.prefWidth(5), 0);
    }

    /**
     * If the layout bounds has a negative value, it shouldn't leak out through node.prefWidth
     */
    @Test public void Node_prefWidth_BasedOnLayoutBounds_CleansUpAfterBadBounds2() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, -10, 50);
            }
        };
        assertEquals(0, node.prefWidth(-1), 0);
        assertEquals(0, node.prefWidth(5), 0);
    }

    /**
     * The standard case, where the layout bounds is automatically determined by the bounds of
     * the rectangle. I've offset the rectangle x/y to make sure we're using the height and
     * not the maxY for this calculation.
     */
    @Test public void Node_prefHeight_BasedOnLayoutBounds() {
        Rectangle node = new Rectangle(10, 10, 100, 100);
        assertEquals(100, node.prefHeight(-1), 0);
        assertEquals(100, node.prefHeight(5), 0);
    }

    /**
     * A specialized computeLayoutBounds implementation, to make sure it is the
     * layout bounds from which we're gathering this information.
     */
    @Test public void Node_prefHeight_BasedOnLayoutBounds2() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, 50, 50);
            }
        };
        assertEquals(50, node.prefHeight(-1), 0);
        assertEquals(50, node.prefHeight(5), 0);
    }

    /**
     * If the layout bounds has a NaN, it shouldn't leak out through node.prefHeight
     */
    @Test public void Node_prefHeight_BasedOnLayoutBounds_CleansUpAfterBadBounds() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, 50, Double.NaN);
            }
        };
        assertEquals(0, node.prefHeight(-1), 0);
        assertEquals(0, node.prefHeight(5), 0);
    }

    /**
     * If the layout bounds has a negative value, it shouldn't leak out through node.prefHeight
     */
    @Test public void Node_prefHeight_BasedOnLayoutBounds_CleansUpAfterBadBounds2() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override protected Bounds impl_computeLayoutBounds() {
                return new BoundingBox(0, 0, 50, -10);
            }
        };
        assertEquals(0, node.prefHeight(-1), 0);
        assertEquals(0, node.prefHeight(5), 0);
    }

    /**
     * Make sure minWidth is based on pref width. By overriding prefWidth to
     * be something nonsensical, we know in the test that the pref width is being used
     * and not the layout bounds width.
     */
    @Test public void Node_minWidth_SameAsPrefWidth() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override public double prefWidth(double height) {
                return 500;
            }
        };

        assertEquals(500, node.minWidth(-1), 0);
        assertEquals(500, node.minWidth(5), 0);
    }

    /**
     * Make sure minHeight is based on pref height. By overriding prefHeight to
     * be something nonsensical, we know in the test that the pref height is being used
     * and not the layout bounds height.
     */
    @Test public void Node_minHeight_SameAsPrefHeight() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override public double prefHeight(double height) {
                return 500;
            }
        };

        assertEquals(500, node.minHeight(-1), 0);
        assertEquals(500, node.minHeight(5), 0);
    }

    /**
     * Make sure maxWidth is based on pref width. By overriding prefWidth to
     * be something nonsensical, we know in the test that the pref width is being used
     * and not the layout bounds width.
     */
    @Test public void Node_maxWidth_SameAsPrefWidth() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override public double prefWidth(double height) {
                return 500;
            }
        };

        assertEquals(500, node.maxWidth(-1), 0);
        assertEquals(500, node.maxWidth(5), 0);
    }

    /**
     * Make sure maxHeight is based on pref height. By overriding prefHeight to
     * be something nonsensical, we know in the test that the pref height is being used
     * and not the layout bounds height.
     */
    @Test public void Node_maxHeight_SameAsPrefHeight() {
        Rectangle node = new Rectangle(10, 10, 100, 100) {
            @Override public double prefHeight(double height) {
                return 500;
            }
        };

        assertEquals(500, node.maxHeight(-1), 0);
        assertEquals(500, node.maxHeight(5), 0);
    }
}
