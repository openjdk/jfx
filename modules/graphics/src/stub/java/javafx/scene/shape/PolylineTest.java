/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPolyline;
import javafx.collections.ObservableList;
import javafx.scene.NodeTest;
import org.junit.Test;

import static com.sun.javafx.test.TestHelper.*;
import static org.junit.Assert.*;

public final class PolylineTest {

    @Test public void testVarargConstructor() {
        final StubPolyline polyline = new StubPolyline(1, 2, 3, 4);
        assertEquals(4, polyline.getPoints().size());
        assertEquals(1, polyline.getPoints().get(0), 0.0001);
        assertEquals(2, polyline.getPoints().get(1), 0.0001);
        assertEquals(3, polyline.getPoints().get(2), 0.0001);
        assertEquals(4, polyline.getPoints().get(3), 0.0001);
    }

    @Test public void testPropertyPropagation_emptyPoints() {
        final StubPolyline polyline = new StubPolyline();
        NodeTest.callSyncPGNode(polyline);
        assertPGPolylinePointsEquals(polyline, new double[0]);
    }

    @Test public void testPropertyPropagation_pointsEvenLength() {
        final double[] initialPoints = { 10, 20, 100, 200, 200, 100, 50, 10 };

        final StubPolyline polyline = new StubPolyline(initialPoints);
        NodeTest.callSyncPGNode(polyline);
        assertPGPolylinePointsEquals(polyline, initialPoints);

        final ObservableList<Double> polylinePoints = polyline.getPoints();
        polylinePoints.remove(1);
        polylinePoints.remove(2);

        NodeTest.callSyncPGNode(polyline);
        assertPGPolylinePointsEquals(polyline, 10, 100, 200, 100, 50, 10);
    }

    @Test public void testPropertyPropagation_pointsOddLength() {
        final double[] initialPoints = { 10, 20, 100, 200, 200 };

        final StubPolyline polyline = new StubPolyline(initialPoints);
        NodeTest.callSyncPGNode(polyline);
        assertPGPolylinePointsEquals(polyline, initialPoints);

        final ObservableList<Double> polylinePoints = polyline.getPoints();
        polylinePoints.add(100.0);
        polylinePoints.add(50.0);

        NodeTest.callSyncPGNode(polyline);
        assertPGPolylinePointsEquals(polyline, 10, 20, 100, 200, 200, 100, 50);
    }

    @Test public void testBounds_emptyPoints() {
        final StubPolyline polyline = new StubPolyline();
        assertBoundsEqual(box(0, 0, -1, -1), polyline.getBoundsInLocal());
    }

    @Test public void testBounds_evenPointsLength() {
        final double[] initialPoints = { 100, 100, 200, 100, 200, 200 };

        final StubPolyline polyline = new StubPolyline(initialPoints);
        assertSimilar(box(100, 100, 100, 100), polyline.getBoundsInLocal());

        final ObservableList<Double> polylinePoints = polyline.getPoints();
        polylinePoints.add(200.0);
        polylinePoints.add(300.0);

        assertSimilar(box(100, 100, 100, 200), polyline.getBoundsInLocal());
    }

    @Test public void testBounds_oddPointsLength() {
        final double[] initialPoints = {
            100, 100, 200, 100, 200, 200, 200, 300
        };

        final StubPolyline polyline = new StubPolyline(initialPoints);
        assertSimilar(box(100, 100, 100, 200), polyline.getBoundsInLocal());

        final ObservableList<Double> polylinePoints = polyline.getPoints();
        polylinePoints.remove(6);

        assertSimilar(box(100, 100, 100, 100), polyline.getBoundsInLocal());
    }

    private static void assertPGPolylinePointsEquals(
            final StubPolyline polyline,
            final double... expectedPoints) {
        final StubNGPolyline stubPolyline = polyline.impl_getPeer();
        final float[] pgPoints = stubPolyline.points;

        final int minLength = expectedPoints.length & ~1;
        final int maxLength = expectedPoints.length;

        assertTrue(pgPoints.length >= minLength);
        assertTrue(pgPoints.length <= maxLength);

        int i;

        for (i = 0; i < minLength; ++i) {
            assertEquals(expectedPoints[i], pgPoints[i], 0);
        }

        for (; i < pgPoints.length; ++i) {
            assertEquals(expectedPoints[i], pgPoints[i], 0);
        }
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubPolyline().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private final class StubPolyline extends Polyline {
        public StubPolyline(double... initialPoints) {
            super(initialPoints);
        }

        public StubPolyline() {
            super();
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGPolyline();
        }
    }

    private final class StubNGPolyline extends NGPolyline {
        private float[] points;
        @Override
        public void updatePolyline(float[] points) {
            super.updatePolyline(points);
            this.points = points;
        }
    }
}
