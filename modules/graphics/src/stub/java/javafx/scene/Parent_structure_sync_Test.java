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

package javafx.scene;

import com.sun.javafx.pgstub.StubToolkit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.sg.PGGroup;
import com.sun.javafx.tk.Toolkit;
import javafx.stage.Stage;

/**
 * Tests to make sure the synchronization of children between a Parent and PGGroup
 * works as expected.
 */
public class Parent_structure_sync_Test {
    private Rectangle r1, r2, r3, r4, r5;
    private Parent parent;
    private PGGroup pg;
    
    @Before public void setup() {
        parent = new Group();
        r1 = new Rectangle(0, 0, 10, 10);
        r2 = new Rectangle(0, 0, 10, 10);
        r3 = new Rectangle(0, 0, 10, 10);
        r4 = new Rectangle(0, 0, 10, 10);
        r5 = new Rectangle(0, 0, 10, 10);
        pg = (PGGroup) parent.impl_getPGNode();

        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        sync();
    }
    
    private void sync() {
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
    }
    
    @Test public void emptyParentShouldHaveEmptyPGGroup() {
        assertTrue(pg.getChildren().isEmpty());
    }
    
    @Test public void childAddedToEmptyParentShouldBeInPGGroup() {
        parent.getChildren().add(r1);
        sync();
        assertEquals(1, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
    }
    
    @Test public void childrenAddedToEmptyParentShouldAllBeInPGGroup() {
        parent.getChildren().addAll(r1, r2, r3);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void addingAChildToTheBack() {
        parent.getChildren().addAll(r2, r3, r4);
        sync();
        parent.getChildren().add(0, r1);
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }

    @Test public void addingAChildToTheFront() {
        parent.getChildren().addAll(r1, r2, r3);
        sync();
        parent.getChildren().add(r4);
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void addingAChildToTheCenter() {
        parent.getChildren().addAll(r1, r2, r4);
        sync();
        parent.getChildren().add(2, r3);
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void removingAChildFromTheFront() {
        parent.getChildren().addAll(r1, r2, r3, r4);
        sync();
        parent.getChildren().remove(3);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void removingAChildFromTheBack() {
        parent.getChildren().addAll(r4, r1, r2, r3);
        sync();
        parent.getChildren().remove(0);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void removingAChildFromTheCenter() {
        parent.getChildren().addAll(r1, r2, r4, r3);
        sync();
        parent.getChildren().remove(2);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void movingAChildFromTheBackToTheFront() {
        parent.getChildren().addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void movingAChildFromTheBackToTheFrontAndAddingAChild() {
        parent.getChildren().addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        parent.getChildren().add(r5);
        sync();
        assertEquals(5, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
        assertSame(r5.impl_getPGNode(), pg.getChildren().get(4));
    }
    
    @Test public void movingAChildFromTheBackToTheFrontAndRemovingAChild() {
        parent.getChildren().addAll(r3, r1, r4, r2);
        sync();
        r3.toFront();
        parent.getChildren().remove(1);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void movingAChildFromTheBackToTheFrontAndThenRemovingTheChild() {
        parent.getChildren().addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        parent.getChildren().remove(3);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }

    @Test public void movingAChildFromTheCenterToTheFront() {
        parent.getChildren().addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void movingAChildFromTheCenterToTheBack() {
        parent.getChildren().addAll(r2, r3, r1, r4);
        sync();
        r1.toBack();
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void movingAChildFromTheCenterToTheFrontAndAddingAChild() {
        parent.getChildren().addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        parent.getChildren().add(r5);
        sync();
        assertEquals(5, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
        assertSame(r5.impl_getPGNode(), pg.getChildren().get(4));
    }
    
    @Test public void movingAChildFromTheCenterToTheFrontAndRemovingAChild() {
        parent.getChildren().addAll(r1, r2, r3, r4);
        sync();
        r3.toFront();
        parent.getChildren().remove(2);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void movingAChildFromTheCenterToTheFrontAndThenRemovingTheChild() {
        parent.getChildren().addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        parent.getChildren().remove(3);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }

    @Test public void movingAChildFromTheFrontToTheBack() {
        parent.getChildren().addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        sync();
        assertEquals(4, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
    }
    
    @Test public void movingAChildFromTheFrontToTheBackAndAddingAChild() {
        parent.getChildren().addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        parent.getChildren().add(r5);
        sync();
        assertEquals(5, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
        assertSame(r4.impl_getPGNode(), pg.getChildren().get(3));
        assertSame(r5.impl_getPGNode(), pg.getChildren().get(4));
    }
    
    @Test public void movingAChildFromTheFrontToTheBackAndRemovingAChild() {
        parent.getChildren().addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        parent.getChildren().remove(3);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
    
    @Test public void movingAChildFromTheFrontToTheBackAndThenRemovingTheChild() {
        parent.getChildren().addAll(r1, r2, r3, r4);
        sync();
        r4.toBack();
        parent.getChildren().remove(0);
        sync();
        assertEquals(3, pg.getChildren().size());
        assertSame(r1.impl_getPGNode(), pg.getChildren().get(0));
        assertSame(r2.impl_getPGNode(), pg.getChildren().get(1));
        assertSame(r3.impl_getPGNode(), pg.getChildren().get(2));
    }
}
