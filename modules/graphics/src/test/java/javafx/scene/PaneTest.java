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

import javafx.scene.layout.MockRegion;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PaneTest {
    

    @Test
    public void testPrefWidthWithResizableChild() {
        Pane pane = new Pane();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        pane.getChildren().add(region);
        pane.layout();

        assertEquals(100, pane.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightWithResizableChild() {
        Pane pane = new Pane();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        pane.getChildren().add(region);
        pane.layout();

        assertEquals(150, pane.prefHeight(-1), 0);
    }

    @Test
    public void testMinAndPreferredSizes() {
        Pane pane = new Pane();
        Rectangle rect = new Rectangle(50,50);
        pane.getChildren().add(rect);
        
        rect.relocate(0, 0);
        
        pane.layout();

        //min size is always equal to insets
        assertEquals(0, pane.minWidth(-1), 1e-100);
        assertEquals(0, pane.minHeight(-1), 1e-100);
        assertEquals(50, pane.prefWidth(-1), 1e-100);
        assertEquals(50, pane.prefHeight(-1), 1e-100);

        rect.setWidth(100);

        assertEquals(0, pane.minWidth(-1), 1e-100);
        assertEquals(0, pane.minHeight(-1), 1e-100);
        assertEquals(100, pane.prefWidth(-1), 1e-100);
        assertEquals(50, pane.prefHeight(-1), 1e-100);

        rect.setHeight(200);

        assertEquals(0, pane.minWidth(-1), 1e-100);
        assertEquals(0, pane.minHeight(-1), 1e-100);
        assertEquals(100, pane.prefWidth(-1), 1e-100);
        assertEquals(200, pane.prefHeight(-1), 1e-100);
    }
    
    @Test
    public void testPrefSizeRespectsBounds() {
        Pane pane = new Pane();
        Node n1 = new MockRegion(100, 100, 10, 10, 1000, 1000);
        n1.relocate(10, 0);
        Node n2 = new MockRegion(0, 0, 200, 200, 100, 100);
        n2.relocate(0, 20);
        
        pane.getChildren().addAll(n1, n2);
        
        pane.layout();
        
        assertEquals(110, pane.prefWidth(-1), 1e-100);
        assertEquals(120, pane.prefHeight(-1), 1e-100);
    }
    
}
