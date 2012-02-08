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
 */
package javafx.scene;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

/**
 * Tests various aspects of Picking.
 *
 */
public class PickAndContainsTest {

    /***************************************************************************
     *                                                                         *
     *                              Picking Tests                              *
     *                                                                         *
     **************************************************************************/
    @Test
    public void testScenePickingRect() {
        Rectangle rect = new Rectangle(50, 25, 100, 50);
        Group g = new Group();
        g.getChildren().add(rect);
        Scene scene = new Scene(g);

        assertSame(rect, scene.test_pick(100, 50));
        assertNull(scene.test_pick(0, 0));
        assertNull(scene.test_pick(160, 50));
    }

    @Test
    public void testScenePickingCircle() {
        Circle circle = new Circle(60, 60, 50);
        Group g = new Group();
        g.getChildren().add(circle);
        Scene scene = new Scene(g);

        assertSame(circle, scene.test_pick(100, 50));
        assertNull(scene.test_pick(0, 0));
        assertNull(scene.test_pick(160, 50));
    }

    @Test
    public void testScenePickingGroup() {
        Group grp;
        Rectangle r0;
        Rectangle r1;
        Rectangle r2;
        Group root = new Group();

        root.getChildren().addAll(r0 = new Rectangle(100, 100, 40, 20),
                                grp = new Group(r1 = new Rectangle(0,0,40,20), 
                                                r2 = new Rectangle(200,200,40,20))
                                );

        Scene scene = new Scene(root);

        r1.setId("Rect 1");
        r2.setId("Rect 2");
        
        int pickX = 100;
        int pickY = 100;
        assertSame(r0, scene.test_pick(pickX, pickY));
        assertFalse(grp.contains(pickX, pickY));
        assertTrue(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertFalse(r2.contains(pickX, pickY));

        pickX = 45;
        pickY = 50;
        assertNull(scene.test_pick(pickX, pickY));
        assertFalse(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertFalse(r2.contains(pickX, pickY));

        pickX = 38;
        pickY = 18;
        assertSame(r1, scene.test_pick(pickX, pickY));
        assertTrue(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertTrue(r1.contains(pickX, pickY));
        assertFalse(r2.contains(pickX, pickY));

        pickX = 230;
        pickY = 215;
        assertSame(r2, scene.test_pick(pickX, pickY));
        assertTrue(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertTrue(r2.contains(pickX, pickY));
    }

    @Test
    public void testScenePickingGroupAndClip() {
        Group grp;
        Rectangle r0;
        Rectangle r1;
        Rectangle r2;
        Group root = new Group();
        root.getChildren().addAll(r0 = new Rectangle(100, 100, 40, 20),
                                grp = new Group(r1 = new Rectangle(0,0,40,20),
                                                r2 = new Rectangle(200,200,40,20))
                                );
        Scene scene = new Scene(root);
        r1.setId("Rect 1");
        r1.setClip(new Circle(20,10,10));
        r2.setId("Rect 2");
        grp.setClip(new Circle(120,120,120));

        int pickX = 38;
        int pickY = 18;
        assertNull(scene.test_pick(pickX, pickY));
        assertFalse(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertFalse(r2.contains(pickX, pickY));

        pickX = 230;
        pickY = 215;
        assertNull(scene.test_pick(pickX, pickY));
        assertFalse(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertTrue(r2.contains(pickX, pickY));
    }

    @Test
    public void testSceneGroupPickOnBounds() {
        Group grp;
        Rectangle r0;
        Rectangle r1;
        Rectangle r2;
        Group root = new Group();
        root.getChildren().addAll(r0 = new Rectangle(100, 100, 40, 20),
                grp = new Group(r1 = new Rectangle(0,0,40,20),
                        r2 = new Rectangle(200,200,40,20))
                );
        Scene scene = new Scene(root);

        r1.setId("Rect 1");
        r2.setId("Rect 2");

        grp.setPickOnBounds(true);

        int pickX = 45;
        int pickY = 50;
        assertNull(scene.test_pick(pickX, pickY));
        assertTrue(grp.contains(pickX, pickY));
        assertFalse(r0.contains(pickX, pickY));
        assertFalse(r1.contains(pickX, pickY));
        assertFalse(r2.contains(pickX, pickY));
    }
}

