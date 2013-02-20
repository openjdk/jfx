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
import static com.sun.javafx.test.TestHelper.box;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import org.junit.Test;

public class LayoutBoundsTest {

    public @Test
    void testLayoutBoundsForLeafNode() {
        Rectangle rect = new Rectangle(10, 10, 100, 100);

        assertBoundsEqual(box(10, 10, 100, 100), rect.getLayoutBounds());
    }

    public @Test
    void testLayoutBoundsUpdatedForLeafNode() {
        Rectangle rect = new Rectangle(10, 10, 100, 100);

        rect.setWidth(200);
        assertBoundsEqual(box(10, 10, 200, 100), rect.getLayoutBounds());

        rect.setX(20);
        assertBoundsEqual(box(20, 10, 200, 100), rect.getLayoutBounds());
    }

    public @Test
    void testEffectNotIncludedInLayoutBounds() {
        Rectangle rect = new Rectangle(10, 10, 100, 100);
        DropShadow ds = new DropShadow();
        ds.setOffsetY(5);
        ds.setOffsetX(5);
        rect.setEffect(ds);

        assertBoundsEqual(box(10, 10, 100, 100), rect.getLayoutBounds());
    }

    public @Test
    void testClipNotIncludedInLayoutBounds() {
        Rectangle rect = new Rectangle(10, 10, 100, 100);
        rect.setClip(new Rectangle(25, 25, 25, 25));

        assertBoundsEqual(box(10, 10, 100, 100), rect.getLayoutBounds());
    }

    public @Test
    void testTransformsNotIncludedInLayoutBounds() {
        Rectangle rect = new Rectangle(10, 10, 100, 100);
        rect.getTransforms().add(new Scale(2, 15));
        rect.getTransforms().add(new Translate(20, 30));

        assertBoundsEqual(box(10, 10, 100, 100), rect.getLayoutBounds());

        rect.setTranslateX(50);
        rect.setTranslateY(50);
        rect.setScaleX(.5f);
        rect.setScaleY(.2f);
        rect.setRotate(45);

        assertBoundsEqual(box(10, 10, 100, 100), rect.getLayoutBounds());
    }

    public @Test
    void testLayoutBoundsForGroup() {
        Group group = new Group(new Rectangle(10, 10, 100, 100), new Rectangle(
                110, 10, 200, 100));
        assertBoundsEqual(box(10, 10, 300, 100), group.getLayoutBounds());

        ((Rectangle) group.getChildren().get(0)).setX(50);
        assertBoundsEqual(box(50, 10, 260, 100), group.getLayoutBounds());

        ((Rectangle) group.getChildren().get(0)).setHeight(200);
        assertBoundsEqual(box(50, 10, 260, 200), group.getLayoutBounds());
    }

    public @Test
    void testEffectOnLayoutBoundsForGroup() {
        Rectangle r1 = new Rectangle(10, 10, 100, 100);
        DropShadow ds1 = new DropShadow();
        ds1.setOffsetX(5);
        ds1.setOffsetY(5);
        r1.setEffect(ds1);
        Group group = new Group(r1, new Rectangle(110, 10, 200, 100));

        assertBoundsEqual(box(6, 6, 304, 118), group.getLayoutBounds());

        // setting effect directly on gruop should not affect layout bounds
        DropShadow ds2 = new DropShadow();
        ds2.setOffsetX(5);
        ds2.setOffsetY(5);
        group.setEffect(ds2);
        assertBoundsEqual(box(6, 6, 304, 118), group.getLayoutBounds());
    }

    public @Test
    void testClipOnLayoutBoundsForGroup() {
        Rectangle r1 = new Rectangle(10, 10, 100, 100);
        r1.setClip(new Rectangle(60, 60, 50, 100));
        Group group = new Group(r1, new Rectangle(110, 10, 200, 100));

        assertBoundsEqual(box(60, 10, 250, 100), group.getLayoutBounds());

        // setting clip directly on group should not affect layout bounds
        group.setClip(new Rectangle(50, 50, 20, 20));
        assertBoundsEqual(box(60, 10, 250, 100), group.getLayoutBounds());
    }

    public @Test
    void testTransformsOnLayoutBoundsForGroup() {
        Rectangle r1 = new Rectangle(10, 10, 100, 100);
        r1.getTransforms().add(new Scale(2, 2));
        Group group = new Group(r1, new Rectangle(120, 20, 200, 100));

        assertBoundsEqual(box(20, 20, 300, 200), group.getLayoutBounds());

        // setting clip directly on gruop should not affect layout bounds
        group.getTransforms().add(new Scale(3, 3));
        assertBoundsEqual(box(20, 20, 300, 200), group.getLayoutBounds());

        r1.getTransforms().clear();
        r1.setScaleX(2);
        r1.setScaleY(2);
        r1.setTranslateX(10);
        r1.setTranslateY(10);

        assertBoundsEqual(box(-30, -30, 350, 200), group.getLayoutBounds());

        group.setScaleX(3);
        group.setScaleY(3);
        group.setTranslateX(20);
        group.setTranslateY(20);
        assertBoundsEqual(box(-30, -30, 350, 200), group.getLayoutBounds());
    }

}
