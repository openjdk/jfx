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

import javafx.scene.NodeTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class LineTest {

    @Test
    public void testPropertyPropagation_visible() throws Exception {
        final Line node = new Line();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test
    public void testPropertyPropagation_startX() throws Exception {
        final Line node = new Line();
        NodeTest.testDoublePropertyPropagation(node, "startX", "x1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_startY() throws Exception {
        final Line node = new Line();
        NodeTest.testDoublePropertyPropagation(node, "startY", "y1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endX() throws Exception {
        final Line node = new Line();
        NodeTest.testDoublePropertyPropagation(node, "endX", "x2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endY() throws Exception {
        final Line node = new Line();
        NodeTest.testDoublePropertyPropagation(node, "endY", "y2", 100, 200);
    }

    @Test public void testBoundPropertySync_startX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "startX", "x1", 10.0);
    }

    @Test public void testBoundPropertySync_startY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "startY", "y1", 50.0);
    }

    @Test public void testBoundPropertySync_endX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "endX", "x2", 200.0);
    }

    @Test public void testBoundPropertySync_endY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "endY", "y2", 300.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new Line().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
