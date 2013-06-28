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

package javafx.scene.bounds;

import static com.sun.javafx.test.TestHelper.box;
import static org.junit.Assert.assertEquals;
import javafx.scene.Group;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

public class ClipBoundsTest {
    
    /***************************************************************************
     *                                                                         *
     *                          Clipping and Effects                           *
     *                                                                         *
     *  These tests build upon the simple bounds tests by adding clipping and  *
     *  effects into the mix. These tests assume that boundsInLocal, without   *
     *  any effects or clipping, is correct.                                   *
     *                                                                         *
    /**************************************************************************/

    // a simple test that clipping a rectangle with another rectangle gives
    // the appropriate boundsInLocal
    public @Test void testClippingWithRectangle() {
        // the intersection of the clip & rect will be 10, 10, 90, 90
        Rectangle rect = new Rectangle(100, 100);
        Rectangle clip = new Rectangle(10, 10, 100, 100);
        rect.setClip(clip);

        assertEquals(box(10, 10, 90, 90), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test clipping with a group clips according to the groups bounds
    public @Test void testClippingWithGroup() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group group = new Group(r1, r2);
        Rectangle rect = new Rectangle(100, 100);
        rect.setClip(group);
        
        assertEquals(box(20, 20, 80, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test clipping with a node that is also clipped yields the correct bounds
    public @Test void testClippingWithClippedRectangle() {
        // clip's bounds should be 20, 20, 50, 50
        Rectangle clip = new Rectangle(10, 10, 100, 100);
        Rectangle clipClip = new Rectangle(20, 20, 50, 50);
        clip.setClip(clipClip);
        Rectangle rect = new Rectangle(40, 40);
        rect.setClip(clip);

        assertEquals(box(20, 20, 20, 20), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test clipping with a clipped node where the subclip changes
    public @Test void testSwappingSubClipOnClippingRectangle() {
        // clip's bounds should be 20, 20, 50, 50
        Rectangle clip = new Rectangle(10, 10, 100, 100);
        Rectangle clipClip = new Rectangle(20, 20, 50, 50);
        clip.setClip(clipClip);
        Rectangle rect = new Rectangle(40, 40);
        rect.setClip(clip);

        assertEquals(box(20, 20, 20, 20), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());

        // testing that changing clip's clip changes bounds
        Rectangle clipClip2 = new Rectangle(30, 30, 50, 50);
        clip.setClip(clipClip2);

        assertEquals(box(30, 30, 10, 10), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test clipping with a node with a complex clip yields the correct bounds
    public @Test void testClippingWithComplexClippedRectangle() {
        // clip's bounds should be 20, 20, 50, 50
        Rectangle clip = new Rectangle(10, 10, 100, 100);
        Polygon polyClip = new Polygon(new double[] {
            30, 25, 35, 30, 30, 35, 25, 30,
        });
        clip.setClip(polyClip);
        Rectangle rect = new Rectangle(40, 40);
        rect.setClip(clip);

        assertEquals(box(25, 25, 10, 10), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test clipping with an image results in the right bounds
    // test clipping with text results in the right bounds
    
    // test changing the clips bounds also changes the bounds of the clip parent
    public @Test void testChangingClipBounds() {
        Rectangle clip = new Rectangle(50, 50);
        Rectangle rect = new Rectangle(100, 100);
        rect.setClip(clip);

        assertEquals(box(0, 0, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        clip.setWidth(60);
        clip.setHeight(60);
        assertEquals(box(0, 0, 60, 60), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());        
    }
    
    // test setting a clip changes the bounds
    public @Test void testSettingClip() {
        Rectangle rect = new Rectangle(100, 100);

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setClip(new Rectangle(50, 50));

        assertEquals(box(0, 0, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());        
    }
    
    // test swapping clips changes the bounds
    public @Test void testSwappingClip() {
        Rectangle clip = new Rectangle(50, 50);
        Rectangle rect = new Rectangle(100, 100);
        rect.setClip(clip);
        
        assertEquals(box(0, 0, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setClip(new Rectangle(10, 10, 50, 50));
        assertEquals(box(10, 10, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test removing the clip changes the bounds
    public @Test void testRemovingClip() {
        Rectangle clip = new Rectangle(50, 50);
        Rectangle rect = new Rectangle(100, 100);
        rect.setClip(clip);
        assertEquals(box(0, 0, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setClip(null);
        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test setting the clip and changing the geom of the Node give right bounds
    public @Test void testClippingAndChangingGeometry() {
        Rectangle clip = new Rectangle(50, 50);
        Rectangle rect = new Rectangle(100, 100);
        rect.setClip(clip);
        assertEquals(box(0, 0, 50, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setWidth(20);
        assertEquals(box(0, 0, 20, 50), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

}
