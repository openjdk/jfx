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
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Rectangle;

import org.junit.Test;


public class EffetctBoundsTest {

    // test setting an effect on a Node alters the bounds
    public @Test void testBoundsOnRectangleWithShadow() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        rect.setEffect(ds);
        
        assertEquals(box(-2, -2, 104, 104), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test setting an effect on a Node and removing the effect is right bounds
    public @Test void testBoundsOnRectangleWithShadowRemoved() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        rect.setEffect(ds);
        
        assertEquals(box(-2, -2, 104, 104), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setEffect(null);
        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // test setting an effect on a Node and changing the effect params
    //   (which causes the effect bounds to change) will update the bounds
    public @Test void testBoundsOnRectangleWithShadowChanged() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        rect.setEffect(ds);

        assertEquals(box(-2, -2, 104, 104), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());

        ds.setRadius(4);
        assertEquals(box(-3, -3, 106, 106), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
     }

    // test setting chained effects on a Node and changing the effect params
    //   (which causes the effect bounds to change) will update the bounds
    public @Test void testBoundsOnRectangleWithShadowAndGlowChanged() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        Glow g = new Glow();
        g.setInput(ds);

        rect.setEffect(g);

        assertEquals(box(-2, -2, 104, 104), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        ds.setRadius(4);
        assertEquals(box(-3, -3, 106, 106), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test setting an effect on a Node and a Clip works as expected
    public @Test void testBoundsOnRectangleWithShadowAndClip() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        rect.setEffect(ds);
        rect.setClip(new Rectangle(-10, -10, 30, 30));
        
        assertEquals(box(-2, -2, 22, 22), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test setting an effect on a Node alters the bounds
    public @Test void testBoundsOnRectangleWithShadowAndReflection() {
        Rectangle rect = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        Reflection r = new Reflection();
        r.setFraction(0.5f);
        ds.setInput(r);
        rect.setEffect(ds);
        assertEquals(box(-2, -2, 104, 154), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    // test setting an effect on two Nodes and changing the effect params
    //   (which causes the effect bounds to change) will update the bounds
    // of both nodes
    public @Test void testBoundsOnRectanglesWithShadowChanged() {
        Rectangle rect1 = new Rectangle(100, 100);
        Rectangle rect2 = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);

        rect1.setEffect(ds);
        rect2.setEffect(ds);

        assertEquals(box(-2, -2, 104, 104), rect1.getBoundsInLocal());
        assertEquals(rect1.getBoundsInLocal(), rect1.getBoundsInParent());
        assertEquals(box(-2, -2, 104, 104), rect2.getBoundsInLocal());
        assertEquals(rect2.getBoundsInLocal(), rect2.getBoundsInParent());
        ds.setRadius(4);
        assertEquals(box(-3, -3, 106, 106), rect1.getBoundsInLocal());
        assertEquals(rect1.getBoundsInLocal(), rect1.getBoundsInParent());
        assertEquals(box(-3, -3, 106, 106), rect2.getBoundsInLocal());
        assertEquals(rect2.getBoundsInLocal(), rect2.getBoundsInParent());
    }

    // test setting two effects on two Nodes and changing the effect params
    //   (which causes the effect bounds to change) will update the bounds
    // of both nodes
    public @Test void testBoundsOnRectanglesWithShadowAndGlowChanged() {
        Rectangle rect1 = new Rectangle(100, 100);
        Rectangle rect2 = new Rectangle(100, 100);
        DropShadow ds = new DropShadow();
        ds.setRadius(2);
        Glow g = new Glow();
        g.setInput(ds);

        rect1.setEffect(g);
        rect2.setEffect(g);

        assertEquals(box(-2, -2, 104, 104), rect1.getBoundsInLocal());
        assertEquals(rect1.getBoundsInLocal(), rect1.getBoundsInParent());
        assertEquals(box(-2, -2, 104, 104), rect2.getBoundsInLocal());
        assertEquals(rect2.getBoundsInLocal(), rect2.getBoundsInParent());

        ds.setRadius(4);
        assertEquals(box(-3, -3, 106, 106), rect1.getBoundsInLocal());
        assertEquals(rect1.getBoundsInLocal(), rect1.getBoundsInParent());
        assertEquals(box(-3, -3, 106, 106), rect2.getBoundsInLocal());
        assertEquals(rect2.getBoundsInLocal(), rect2.getBoundsInParent());
    }
}
