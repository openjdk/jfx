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

package javafx.scene.layout;

import static org.junit.Assert.assertEquals;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.junit.Test;

/**
 * Tests baseline offsets on various classes
 *
 */
public class BaselineTest {

    // test isResizable on key base classes

    @Test public void testShapeBaselineAtBottom() {
        Rectangle rect = new Rectangle(100,200);       
        assertEquals(200, rect.getBaselineOffset(),1e-100);
    }
    
    @Test public void testTextBaseline() {
        Text text = new Text("Graphically");
        float size = (float) text.getFont().getSize();
        assertEquals(size, text.getBaselineOffset(),1e-100);
    }

    @Test public void testParentBaselineMatchesFirstChild() {
        Parent p = new MockParent();
        p.layout();
        assertEquals(180, p.getBaselineOffset(),1e-100);
    }

    @Test public void testParentBaselineIgnoresUnmanagedChild() {
        MockParent p = new MockParent();
        Rectangle r = new Rectangle(20,30);
        r.setManaged(false);
        p.getChildren().add(0, r);
        p.layout();
        assertEquals(180, p.getBaselineOffset(),1e-100);
    }
}
