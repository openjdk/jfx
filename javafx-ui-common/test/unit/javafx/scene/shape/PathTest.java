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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package javafx.scene.shape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.NodeTest;
import javafx.scene.Scene;

import org.junit.Test;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.pgstub.StubPath;
import java.util.ArrayList;
import java.util.List;


public class PathTest {

    @Test public void testVarargConstructor() {
        PathElement one = new MoveTo(10, 10);
        PathElement two = new LineTo(20, 20);
        PathElement three = new MoveTo(30, 30);
        Path path = new Path(one, two, three);
        assertEquals(3, path.getElements().size());
        assertSame(one, path.getElements().get(0));
        assertSame(two, path.getElements().get(1));
        assertSame(three, path.getElements().get(2));
    }
    
    @Test public void testListConstructor() {        
        PathElement one = new MoveTo(10, 10);
        PathElement two = new LineTo(20, 20);
        PathElement three = new MoveTo(30, 30);
        
        List<PathElement> listOfElements = new ArrayList<PathElement>();
        listOfElements.add(one);
        listOfElements.add(two);
        listOfElements.add(three);
        Path path = new Path(listOfElements);
        assertEquals(3, path.getElements().size());
        assertSame(one, path.getElements().get(0));
        assertSame(two, path.getElements().get(1));
        assertSame(three, path.getElements().get(2));
    }
    
    @Test public void testBoundPropertySync_FillRule() throws Exception {
        ObjectProperty<FillRule> v = new SimpleObjectProperty<FillRule>(FillRule.EVEN_ODD);
        Path path = new Path();
        path.fillRuleProperty().bind(v);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(path);
        v.set(FillRule.NON_ZERO);
        NodeTest.syncNode(path);

        //check
        Path2D geometry = (Path2D) ((StubPath)path.impl_getPGPath()).getGeometry();
        assertEquals(geometry.getWindingRule(), FillRule.NON_ZERO.ordinal());
    }

    @Test public void testFirstRelativeElement_PathIsEmpty() {
        Path path = new Path();
        final MoveTo moveTo = new MoveTo(10, 10);
        moveTo.setAbsolute(false);
        path.getElements().add(moveTo);
        path.getElements().add(new LineTo(100, 100));
        Path2D geometry = (Path2D) ((StubPath)path.impl_getPGPath()).getGeometry();
        PathIterator piterator = geometry.getPathIterator(null);
        assertTrue(piterator.isDone());//path is empty
    }

     @Test public void testFirstRelativeElement_BoundsAreEmpty() {
        Path path = new Path();
         final MoveTo moveTo = new MoveTo(10, 10);
         moveTo.setAbsolute(false);
         path.getElements().add(moveTo);
         path.getElements().add(new LineTo(100, 100));
        assertTrue(path.getBoundsInLocal().isEmpty() && path.getBoundsInParent().isEmpty());
    }

    @Test public void testFirstElementIsNotMoveTo_PathIsEmpty() {
        Path path = new Path();
        path.getElements().add(new LineTo(10, 10));
        path.getElements().add(new LineTo(100, 100));
        Path2D geometry = (Path2D) ((StubPath)path.impl_getPGPath()).getGeometry();
        PathIterator piterator = geometry.getPathIterator(null);
        assertTrue(piterator.isDone());//path is empty
    }

    @Test public void testFirstElementIsNotMoveTo_BoundsAreEmpty() {
        Path path = new Path();
        path.getElements().add(new LineTo(10, 10));
        path.getElements().add(new LineTo(100, 100));
        assertTrue(path.getBoundsInLocal().isEmpty() && path.getBoundsInParent().isEmpty());
    }

    @Test public void testFillRuleSync() {
        Path path = new Path();
        path.getElements().add(new MoveTo(10, 10));
        path.getElements().add(new LineTo(100, 10));
        path.getElements().add(new LineTo(100, 100));
        path.setFillRule(FillRule.EVEN_ODD);
        NodeTest.syncNode(path);
        Path2D geometry = (Path2D) ((StubPath)path.impl_getPGPath()).getGeometry();
        assertEquals(Path2D.WIND_EVEN_ODD, geometry.getWindingRule());

        path.setFillRule(FillRule.NON_ZERO);
        NodeTest.syncNode(path);
        // internal shape might have changed, getting it again
        geometry = (Path2D) ((StubPath)path.impl_getPGPath()).getGeometry();
        assertEquals(Path2D.WIND_NON_ZERO, geometry.getWindingRule());
    }
}
