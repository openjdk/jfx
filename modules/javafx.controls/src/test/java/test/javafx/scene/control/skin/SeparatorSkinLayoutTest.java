/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Separator;
import javafx.scene.control.SkinBaseShim;
import javafx.scene.control.skin.SeparatorSkin;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class SeparatorSkinLayoutTest {
    private Separator separator;
    private SeparatorSkin skin;
    private Region line;

    @BeforeEach
    public void setup() {
        separator = new Separator();
        skin = new SeparatorSkin(separator);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        separator.setPadding(new Insets(10, 8, 6, 4));
        separator.setSkin(skin);
        // It so happens that SeparatorSkin has exactly one child: line
        line = (Region) SkinBaseShim.getChildren(skin).get(0);
        line.setPadding((new Insets(4, 3, 2, 1)));
        separator.resize(100, 100);
        separator.layout();
    }

    private void assertLineWidthMatchesSkinWidth() {
        Bounds lineBounds = line.getBoundsInParent();
        assertEquals(separator.getWidth() - separator.getInsets().getRight(), lineBounds.getMaxX(), .1);
        assertEquals(separator.getInsets().getLeft(), lineBounds.getMinX(), .1);
    }

    private void assertLineHeightMatchesSkinHeight() {
        Bounds lineBounds = line.getBoundsInParent();
        assertEquals(separator.getInsets().getTop(), lineBounds.getMinY(), .1);
        assertEquals(separator.getHeight() - separator.getInsets().getBottom(), lineBounds.getMaxY(), .1);
    }

    // ----------------------- Horizontal Tests

    /**
     * The line should be as wide as the skin, and as tall as its prefHeight,
     * and be positioned within the padding of the skin
     */
    @Test public void separatorWith_TOP_PositionsLineAtTopOfArea() {
        separator.setValignment(VPos.TOP);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        assertEquals(separator.getInsets().getTop(), lineBounds.getMinY(), .1);
        assertEquals(line.prefHeight(-1), lineBounds.getHeight(), .1);
        assertLineWidthMatchesSkinWidth();
    }

    /**
     * The line should be as wide as the skin, and as tall as its prefHeight,
     * and be positioned centered vertically within the padding of the skin
     */
    @Test public void separatorWith_CENTER_PositionsLineAtCenterOfArea() {
        separator.setValignment(VPos.CENTER);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        final double ch = separator.getHeight() - (separator.getInsets().getTop() + separator.getInsets().getBottom());
        final double centerLine = separator.getInsets().getTop() + (ch / 2.0);
        assertEquals(centerLine - (lineBounds.getHeight() / 2.0), lineBounds.getMinY(), .1);
        assertEquals(line.prefHeight(-1), lineBounds.getHeight(), .1);
        assertLineWidthMatchesSkinWidth();
    }

    /**
     * The line should be as wide as the skin, and as tall as its prefHeight,
     * and be positioned within the bottom padding of the skin
     */
    @Test public void separatorWith_BOTTOM_PositionsLineAtBottomOfArea() {
        separator.setValignment(VPos.BOTTOM);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        final double y = separator.getHeight() - (separator.getInsets().getBottom() + line.prefHeight(-1));
        assertEquals(y, lineBounds.getMinY(), .1);
        assertEquals(line.prefHeight(-1), lineBounds.getHeight(), .1);
        assertLineWidthMatchesSkinWidth();
    }

    // ----------------------- Vertical Tests

    /**
     * The line should be as tall as the skin, and as wide as its prefWidth,
     * and be positioned within the left padding of the skin
     */
    @Test public void separatorWith_LEFT_PositionsLineAtLeftOfArea_vertical() {
        separator.setHalignment(HPos.LEFT);
        separator.setOrientation(Orientation.VERTICAL);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        assertEquals(separator.getInsets().getLeft(), lineBounds.getMinX(), .1);
        assertEquals(line.prefWidth(-1), lineBounds.getWidth(), .1);
        assertLineHeightMatchesSkinHeight();
    }

    /**
     * The line should be as tall as the skin, and as wide as its prefWidth,
     * and be positioned centered within the padding of the skin
     */
    @Test public void separatorWith_CENTER_PositionsLineAtCenterOfArea_vertical() {
        separator.setHalignment(HPos.CENTER);
        separator.setOrientation(Orientation.VERTICAL);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        final double cw = separator.getWidth() - (separator.getInsets().getLeft() + separator.getInsets().getRight());
        final double centerLine = separator.getInsets().getLeft() + (cw / 2.0);
        assertEquals(centerLine - (lineBounds.getWidth() / 2.0), lineBounds.getMinX(), .1);
        assertEquals(line.prefWidth(-1), lineBounds.getWidth(), .1);
        assertLineHeightMatchesSkinHeight();
    }

    /**
     * The line should be as tall as the skin, and as wide as its prefWidth,
     * and be positioned within the right padding of the skin
     */
    @Test public void separatorWith_RIGHT_PositionsLineAtRightOfArea_vertical() {
        separator.setHalignment(HPos.RIGHT);
        separator.setOrientation(Orientation.VERTICAL);
        separator.layout();
        Bounds lineBounds = line.getBoundsInParent();
        final double x = separator.getWidth() - (separator.getInsets().getRight() + line.prefWidth(-1));
        assertEquals(x, lineBounds.getMinX(), .1);
        assertEquals(line.prefWidth(-1), lineBounds.getWidth(), .1);
        assertLineHeightMatchesSkinHeight();
    }

}
