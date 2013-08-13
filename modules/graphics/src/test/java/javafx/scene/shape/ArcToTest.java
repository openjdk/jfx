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

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ArcToTest {

    @Test public void testSetGetX() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new ArcTo(), "x", 123.2, 0.0);
    }
    
    @Test public void testSetGetY() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new ArcTo(), "y", 123.2, 0.0);
    }
    
    @Test public void testSetGetRadiusX() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new ArcTo(), "radiusX", 123.2, 0.0);
    }
    
    @Test public void testSetGetRadiusY() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new ArcTo(), "radiusY", 123.2, 0.0);
    }
    
    
    @Test public void testSetGetXAxisRotation() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new ArcTo(), "xAxisRotation", 123.2, 0.0);
    }
    
    @Test public void testSetGetLargeArcFlag() throws Exception {
        TestUtils.testBooleanPropertyGetterSetter(new ArcTo(), "largeArcFlag");
    }
    
    @Test public void testSetGetSweepFlag() throws Exception {
        TestUtils.testBooleanPropertyGetterSetter(new ArcTo(), "sweepFlag");
    }

    //TODO test addTo

    @Test public void testDoublePropertySynced_X() throws Exception {
        checkSyncedProperty("x", Coords.X, 200.0);
    }

    @Test public void testDoublePropertySynced_Y() throws Exception {
        checkSyncedProperty("y", Coords.Y, 200.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new ArcTo().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private void checkSyncedProperty(String propertyName, Coords coord, double expected)
            throws Exception {
        
        ArcTo arcTo = new ArcTo();
        arcTo.setRadiusX(40.0);arcTo.setRadiusY(50.0);
        arcTo.setX(100.0); arcTo.setY(90.0);
        DoubleProperty v = new SimpleDoubleProperty(100.0);
        Method m = ArcTo.class.getMethod(propertyName + "Property", new Class[] {});
        ((DoubleProperty)m.invoke(arcTo)).bind(v);

        Path path = new Path();
        path.getElements().addAll(new MoveTo(1.0, 1.0), arcTo);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(path);

        v.set(expected);
        NodeTest.syncNode(path);

        //check
        NGPath pgPath = path.impl_getPeer();
        Path2D geometry = pgPath.getGeometry();
        float[] coords = new float[6];
        PathIterator it = (PathIterator)geometry.getPathIterator(null);
        it.next(); it.next(); //path contains [MoveTo], [CubicTo], [CubicTo], [MoveTo]
        int segType = it.currentSegment(coords);
        assertEquals(PathIterator.SEG_CUBICTO, segType);
        assertEquals(expected, coords[coord.ordinal()], 0.001);
    }

    static enum Coords {
        CONTROL_X1,
        CONTROL_Y1,
        CONTROL_X2,
        CONTROL_Y2,
        X,
        Y
    }
}
