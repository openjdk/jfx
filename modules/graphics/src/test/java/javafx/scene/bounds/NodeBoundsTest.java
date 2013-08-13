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

import static com.sun.javafx.test.TestHelper.assertBoundsEqual;
import static com.sun.javafx.test.TestHelper.assertSimilar;
import static com.sun.javafx.test.TestHelper.box;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

public class NodeBoundsTest {

    public @Test
    void testBoundsForLeafNode() {
        Rectangle rect = new Rectangle(10, 10, 50, 50);
        assertSimilar(box(10, 10, 50, 50), rect.getBoundsInLocal());
    }

    public @Test
    void testBounds3DForLeafNode() {
        Rectangle rect = new Rectangle(10, 10, 50, 50);
        rect.setTranslateZ(30);
        assertSimilar(box(10, 10, 30, 50, 50, 30), rect.getBoundsInLocal());
    }

    public @Test
    void testBoundsForInvisibleLeafNode() {
        Rectangle rect = new Rectangle(10, 10, 50, 50);
        rect.setVisible(false);

        assertBoundsEqual(box(10, 10, 50, 50), rect.getBoundsInLocal());
        assertBoundsEqual(rect.getBoundsInLocal(), rect.getBoundsInParent());

        rect.setVisible(true);
        assertBoundsEqual(box(10, 10, 50, 50), rect.getBoundsInLocal());
        assertBoundsEqual(rect.getBoundsInLocal(), rect.getBoundsInParent());

        rect.setVisible(false);
        assertBoundsEqual(box(10, 10, 50, 50), rect.getBoundsInLocal());
        assertBoundsEqual(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }

    public @Test
    void testBoundsForLeafNodeUpdatesWhenGeomChanges() {
        Rectangle rect = new Rectangle();

        assertBoundsEqual(box(0, 0, 0, 0), rect.getBoundsInLocal());

        rect.setX(50);
        assertBoundsEqual(box(50, 0, 0, 0), rect.getBoundsInLocal());

        rect.setY(50);
        rect.setWidth(100);
        rect.setHeight(30);
        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInLocal());
    }
}
