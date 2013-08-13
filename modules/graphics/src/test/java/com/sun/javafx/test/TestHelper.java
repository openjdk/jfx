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

package com.sun.javafx.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;

public class TestHelper {

    // tests that they are within 1px error of being the same. This is to
    // account
    // for stroke width
    public static void assertSimilar(Bounds expected, Bounds actual) {
        assertEquals("minX", expected.getMinX(), actual.getMinX(), 1);
        assertEquals("minY", expected.getMinY(), actual.getMinY(), 1);
        assertEquals("maxX", expected.getMaxX(), actual.getMaxX(), 1);
        assertEquals("maxY", expected.getMaxY(), actual.getMaxY(), 1);
    }
    
    public static final float EPSILON = 1.0e-4f;

    public static void assertAlmostEquals(float a, float b) {
        assertEquals(a, b, EPSILON);
    }

    public static void assertBoundsEqual(Bounds expected, Bounds actual) {
        if (expected.isEmpty() && actual.isEmpty()) {
            return;
        } else {
            assertEquals(expected, actual);
        }
    }

    public static void assertGroupBounds(Group g) {
        // first figure out what the 'expected' bounds are
        float x1 = 0;
        float y1 = 0;
        float x2 = -1;
        float y2 = -1;
        boolean first = true;
        for (Node n : g.getChildren()) {
            if (n.isVisible() && !n.getBoundsInLocal().isEmpty()) {
                if (first) {
                    x1 = (float) n.getBoundsInParent().getMinX();
                    y1 = (float) n.getBoundsInParent().getMinY();
                    x2 = (float) n.getBoundsInParent().getMaxX();
                    y2 = (float) n.getBoundsInParent().getMaxY();
                    first = false;
                } else {
                    x1 = Math.min(x1, (float) n.getBoundsInParent().getMinX());
                    y1 = Math.min(y1, (float) n.getBoundsInParent().getMinY());
                    x2 = Math.max(x2, (float) n.getBoundsInParent().getMaxX());
                    y2 = Math.max(y2, (float) n.getBoundsInParent().getMaxY());
                }
            }
        }
        Bounds expected = box2(x1, y1, x2, y2);
        assertBoundsEqual(expected, g.getBoundsInLocal());
    }

    public static String formatBounds(Bounds b) {
        return "(" + b.getMinX() + ", " + b.getMinY() + ", " + b.getWidth()
                + ", " + b.getHeight() + ")";
    }

    public static BoundingBox box(int minX, int minY, int width, int height) {
        return box((float) minX, (float) minY, (float) width, (float) height);
    }

    public static BoundingBox box(double minX, double minY, double width, double height) {
        return box((float) minX, (float) minY, (float) width, (float) height);
    }

    public static BoundingBox box(float minX, float minY, float width,
            float height) {
        return new BoundingBox(minX, minY, width, height);
    }

    public static BoundingBox box(float minX, float minY, float minZ, float width,
            float height, float depth) {
        return new BoundingBox(minX, minY, minZ, width, height, depth);
    }
    
    public static BoundingBox box2(int minX, int minY, int maxX, int maxY) {
        return box2((float) minX, (float) minY, (float) maxX, (float) maxY);
    }

    public static BoundingBox box2(float minX, float minY, float maxX,
            float maxY) {
        return box(minX, minY, maxX - minX, maxY - minY);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void assertImmutableList(List list) {
        try {
            list.add(new Object());
            fail("Exception expected while modifying the list.");
        } catch (Exception e) {
            // expected
        }
    }
}
