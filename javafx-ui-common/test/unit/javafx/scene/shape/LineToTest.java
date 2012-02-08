/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.NodeTest;
import javafx.scene.Scene;

import org.junit.Test;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.pgstub.StubPath;
import com.sun.javafx.sg.PGPath;

public class LineToTest {

    @Test public void testAddTo() throws Exception {
        //TODO
    }

    @Test public void testDoublePropertySynced_X() {
        double expected = 123.4;
        LineTo lineTo = new LineTo(100.0, 100.0);
        DoubleProperty v = new SimpleDoubleProperty(100.0);
        lineTo.xProperty().bind(v);
        Path path = new Path();
        path.getElements().addAll(new MoveTo(1.0, 1.0), lineTo);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(path);

        v.set(expected);
        NodeTest.syncNode(path);

        //check
        PGPath pgPath = path.impl_getPGPath();
        Path2D geometry = (Path2D)((StubPath)pgPath).getGeometry();
        float[] coords = new float[6];
        PathIterator it = geometry.getPathIterator(null);
        it.next(); //next is lineTo segment
        int segType = it.currentSegment(coords);
        assertEquals(segType, PathIterator.SEG_LINETO);
        assertEquals(expected, coords[0], 0.001);
    }

    @Test public void testDoublePropertySynced_Y() {
        double expected = 432.1;
        LineTo lineTo = new LineTo(100.0, 200.0);
        DoubleProperty v = new SimpleDoubleProperty(200.0);
        lineTo.yProperty().bind(v);
        Path path = new Path();
        path.getElements().addAll(new MoveTo(1.0, 1.0), lineTo);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(path);

        v.set(expected);
        NodeTest.syncNode(path);

        //check
        PGPath pgPath = path.impl_getPGPath();
        Path2D geometry = (Path2D)((StubPath)pgPath).getGeometry();
        float[] coords = new float[6];
        PathIterator it = geometry.getPathIterator(null);
        it.next(); //next is lineTo segment
        int segType = it.currentSegment(coords);
        assertEquals(segType, PathIterator.SEG_LINETO);
        assertEquals(expected, coords[1], 0.001);
    }
}
