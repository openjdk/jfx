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

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.NodeTest;
import javafx.scene.Scene;
import org.junit.Test;

import static org.junit.Assert.*;

public class HLineToTest {

     @Test public void testAddTo() throws Exception {
        Path2D path = new Path2D();
        path.moveTo(0f, 0f);
        final HLineTo hLineTo = new HLineTo(1f);
        hLineTo.impl_addTo(path);
        assertEquals(1.0, path.getCurrentPoint().x, 0.0001f);
        assertEquals(0.0, path.getCurrentPoint().y, 0.0001f);
    }

    @Test public void testDoublePropertySynced_X() {
        double expected = 123.4;
        HLineTo hlineTo = new HLineTo(100.0);
        DoubleProperty v = new SimpleDoubleProperty(100.0);
        hlineTo.xProperty().bind(v);
        Path path = new Path();
        path.getElements().addAll(new MoveTo(1.0, 1.0), hlineTo);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(path);

        v.set(expected);
        NodeTest.syncNode(path);

        //check
        NGPath pgPath = path.impl_getPeer();
        Path2D geometry = pgPath.getGeometry();
        float[] coords = new float[6];
        PathIterator it = geometry.getPathIterator(null);
        it.next(); //next is HLineTo segment
        int segType = it.currentSegment(coords);
        assertEquals(segType, PathIterator.SEG_LINETO);
        assertEquals(expected, coords[0], 0.001);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new HLineTo().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
