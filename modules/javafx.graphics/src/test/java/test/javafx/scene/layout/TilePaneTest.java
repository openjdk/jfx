/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import java.util.stream.Stream;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.ParsedValue;
import javafx.css.CssMetaData;
import javafx.css.CssParserShim;
import javafx.css.StyleableProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class TilePaneTest {

    private static final double EPSILON = 0.0000001;

    TilePane tilepane;
    TilePane htilepane;
    TilePane vtilepane;

    @BeforeEach
    public void setUp() {
        tilepane = new TilePane(); // 12 children
        for(int i = 0; i < 6; i++) {
            MockResizable child1 = new MockResizable(50,60, 100,200, 500,600);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(tilepane).addAll(child1, child2);
        }

        htilepane = new TilePane(Orientation.HORIZONTAL); // 8 children
        for(int i = 0; i < 4; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(htilepane).addAll(child1, child2);
        }

        vtilepane = new TilePane(Orientation.VERTICAL); // 8 children
        for(int i = 0; i < 4; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(vtilepane).addAll(child1, child2);
        }

    }

    @Test
    public void testOrientationDefaultsToHorizontal() {
        assertEquals(Orientation.HORIZONTAL, tilepane.getOrientation());
    }

    @Test
    public void testPrefColumnsDefault() {
        assertEquals(5, tilepane.getPrefColumns());
    }

    @Test
    public void testPrefRowsDefault() {
        assertEquals(5, vtilepane.getPrefColumns());
    }

    @Test
    public void testPrefTileWidthDefaultsToUSE_COMPUTED_SIZE() {
        assertEquals(Region.USE_COMPUTED_SIZE, tilepane.getPrefTileWidth(), 0);
    }

    @Test
    public void testPrefTileHeightDefaultsToUSE_COMPUTED_SIZE() {
        assertEquals(Region.USE_COMPUTED_SIZE, tilepane.getPrefTileHeight(), 0);
    }

    @Test
    public void testAlignmentDefaultsToTopLeft() {
        assertEquals(Pos.TOP_LEFT, tilepane.getAlignment());
    }

    @Test
    public void testTileAlignmentDefaultsToCenter() {
        assertEquals(Pos.CENTER, tilepane.getTileAlignment());
    }

    @Test
    public void testTilePaneNulls() {
        tilepane.setAlignment(null);
        tilepane.setTileAlignment(null);
        tilepane.setOrientation(null);

        // this musn't throw NPE
        tilepane.autosize();
        tilepane.layout();

        assertNull(tilepane.getAlignment());
        assertNull(tilepane.getTileAlignment());
        assertNull(tilepane.getOrientation());
        assertNull(tilepane.alignmentProperty().get());
        assertNull(tilepane.tileAlignmentProperty().get());
        assertNull(tilepane.orientationProperty().get());
    }

    @Test
    public void testHorizontalTilePaneMinSize() {
        assertEquals(200, htilepane.minWidth(-1), 1e-100);
        assertEquals(2400, htilepane.minHeight(100), 1e-100);
    }

    @Test
    public void testHorizontalTilePanePrefSize() {
        assertEquals(1000, htilepane.prefWidth(-1), 1e-100);
        assertEquals(600, htilepane.prefHeight(-1), 1e-100);
    }

    @Test
    public void testVerticalTilePaneMinSize() {
        assertEquals(300, vtilepane.minHeight(-1), 1e-100);
        assertEquals(1600, vtilepane.minWidth(300), 1e-100);
    }

    @Test
    public void testVerticalTilePanePrefSize() {
        assertEquals(1500, vtilepane.prefHeight(-1), 1e-100);
        assertEquals(400, vtilepane.prefWidth(-1), 1e-100);
    }


    @Test
    public void testEmptyHorizontalTilePaneMinWidthIsZero() {
        TilePane Tilepane = new TilePane();

        assertEquals(0, Tilepane.minWidth(-1), 0);
    }

    @Test
    public void testEmptyHorizontalTilePaneMinHeightIsZero() {
        TilePane Tilepane = new TilePane();

        assertEquals(0, Tilepane.minHeight(-1), 0);
    }

    @Test
    public void testEmptyVerticalTilePaneMinWidthIsZero() {
        TilePane Tilepane = new TilePane(Orientation.VERTICAL);

        assertEquals(0, Tilepane.minWidth(-1), 0);
    }

    @Test
    public void testEmptyVerticalTilePaneMinHeightIsZero() {
        TilePane Tilepane = new TilePane(Orientation.VERTICAL);

        assertEquals(0, Tilepane.minHeight(-1), 0);
    }

    @Test
    public void testLayoutWithPrefSize() {
        tilepane.autosize();
        tilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(11);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(450, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testLayoutWithLargerThanPrefSize() {
        tilepane.resize(800,800);
        tilepane.layout();

        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(11);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentTopLeft() {
        htilepane.setAlignment(Pos.TOP_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentTopCenter() {
        htilepane.setAlignment(Pos.TOP_CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentTopRight() {
        htilepane.setAlignment(Pos.TOP_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentCenterLeft() {
        htilepane.setAlignment(Pos.CENTER_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(750, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentCenter() {
        htilepane.setAlignment(Pos.CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(750, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentCenterRight() {
        htilepane.setAlignment(Pos.CENTER_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(750, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentBottomLeft() {
        htilepane.setAlignment(Pos.BOTTOM_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentBottomCenter() {
        htilepane.setAlignment(Pos.BOTTOM_CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneAlignmentBottomRight() {
        htilepane.setAlignment(Pos.BOTTOM_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentTopLeft() {
        vtilepane.setAlignment(Pos.TOP_LEFT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentTopCenter() {
        vtilepane.setAlignment(Pos.TOP_CENTER);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentTopRight() {
        vtilepane.setAlignment(Pos.TOP_RIGHT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentCenterLeft() {
        vtilepane.setAlignment(Pos.CENTER_LEFT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentCenter() {
        vtilepane.setAlignment(Pos.CENTER);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentCenterRight() {
        vtilepane.setAlignment(Pos.CENTER_RIGHT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentBottomLeft() {
        vtilepane.setAlignment(Pos.BOTTOM_LEFT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentBottomCenter() {
        vtilepane.setAlignment(Pos.BOTTOM_CENTER);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneAlignmentBottomRight() {
        vtilepane.setAlignment(Pos.BOTTOM_RIGHT);
        vtilepane.resize(700,1000);
        vtilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(vtilepane).get(0);
        Node last = ParentShim.getChildren(vtilepane).get(7);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(550, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentTopLeft() {
        htilepane.setTileAlignment(Pos.TOP_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentTopCenter() {
        htilepane.setTileAlignment(Pos.TOP_CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentTopRight() {
        htilepane.setTileAlignment(Pos.TOP_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(600, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentCenterLeft() {
        htilepane.setTileAlignment(Pos.CENTER_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentCenter() {
        htilepane.setTileAlignment(Pos.CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentCenterRight() {
        htilepane.setTileAlignment(Pos.CENTER_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentBottomLeft() {
        htilepane.setTileAlignment(Pos.BOTTOM_LEFT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentBottomCenter() {
        htilepane.setTileAlignment(Pos.BOTTOM_CENTER);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTileAlignmentBottomRight() {
        htilepane.setTileAlignment(Pos.BOTTOM_RIGHT);
        htilepane.resize(700,1000);
        htilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(htilepane).get(0);
        Node last = ParentShim.getChildren(htilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(800, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testSetMarginConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(TilePane.getMargin(child1));

        Insets margin = new Insets(10,20,30,40);
        TilePane.setMargin(child1, margin);
        assertEquals(margin, TilePane.getMargin(child1));

        TilePane.setMargin(child1, null);
        assertNull(TilePane.getMargin(child1));
    }

    @Test
    public void testMarginConstraint() {
        TilePane tilepane = new TilePane();

        for(int i = 0; i < 6; i++) {
            MockResizable child1 = new MockResizable(50,60, 100,200, 500,600);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(tilepane).addAll(child1, child2);
        }

        // test a handful
        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(11);

        TilePane.setMargin(first, new Insets(10,20,30,40));

        assertEquals(160, tilepane.minWidth(-1), 1e-100);
        assertEquals(720, tilepane.minHeight(-1), 1e-100);
        assertEquals(800, tilepane.prefWidth(-1), 1e-100);
        assertEquals(720, tilepane.prefHeight(-1), 1e-100);

        tilepane.autosize();
        tilepane.layout();

        assertEquals(40, first.getLayoutX(), 1e-100);
        assertEquals(10, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(190, last.getLayoutX(), 1e-100);
        assertEquals(550, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);


    }

    @Test
    public void testSetAlignmentConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(TilePane.getAlignment(child1));

        TilePane.setAlignment(child1, Pos.TOP_LEFT);
        assertEquals(Pos.TOP_LEFT, TilePane.getAlignment(child1));

        TilePane.setAlignment(child1, null);
        assertNull(TilePane.getAlignment(child1));
    }

    @Test
    public void testHorizontalTilePaneAlignmentConstraint() {
        TilePane tilepane = new TilePane();

        for(int i = 0; i < 6; i++) {
            MockResizable child1 = new MockResizable(50,60, 100,200, 500,600);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(tilepane).addAll(child1, child2);
        }

        assertEquals(100, tilepane.minWidth(-1), 1e-100);
        assertEquals(600, tilepane.minHeight(-1), 1e-100);
        assertEquals(500, tilepane.prefWidth(-1), 1e-100);
        assertEquals(600, tilepane.prefHeight(-1), 1e-100);

        tilepane.autosize();
        tilepane.layout();

        // test a handful
        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(11);

        TilePane.setAlignment(last, Pos.TOP_LEFT);

        tilepane.autosize();
        tilepane.layout();

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);

        tilepane.resize(800,800);
        tilepane.layout();
        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(200, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testWithHorizontalBiasedChild() {
        TilePane tilepane = new TilePane();

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100,100);
        Rectangle rect = new Rectangle(150,50);

        ParentShim.getChildren(tilepane).addAll(biased,rect);

        assertEquals(750, tilepane.prefWidth(-1), 1e-100);
        assertEquals(67, tilepane.prefHeight(-1), 1e-100);

        tilepane.autosize();
        tilepane.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(150, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(67, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(150, rect.getLayoutX(), 1e-100);
        assertEquals(9, rect.getLayoutY(), 1e-100);
        assertEquals(150, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, rect.getLayoutBounds().getHeight(), 1e-100);

    }

    @Test
    public void testWithVerticalBiasedChild() {
        TilePane tilepane = new TilePane();

        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100,100);
        Rectangle rect = new Rectangle(50,150);

        ParentShim.getChildren(tilepane).addAll(biased,rect);

        assertEquals(335.00, tilepane.prefWidth(-1), 1e-100);
        assertEquals(150, tilepane.prefHeight(-1), 1e-100);

        tilepane.autosize();
        tilepane.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(67, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(150, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(76, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(50, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(150, rect.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneWithFixedTileWidth() {
        TilePane tilepane = new TilePane();
        tilepane.setPrefColumns(3);
        tilepane.setPrefTileWidth(200);

        for(int i = 0; i < 4; i++) {
            MockResizable child1 = new MockResizable(150,300);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(tilepane).addAll(child1, child2);
        }

        assertEquals(200, tilepane.getTileWidth(), 0);
        assertEquals(300, tilepane.getTileHeight(), 0);

        assertEquals(600, tilepane.prefWidth(-1), 0);
        assertEquals(900, tilepane.prefHeight(-1), 0);

        tilepane.autosize();
        tilepane.layout();

        assertEquals(200, tilepane.getTileWidth(), 0);
        assertEquals(300, tilepane.getTileHeight(), 0);

        // test a handful
        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(700, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneWithFixedTileHeight() {
        TilePane tilepane = new TilePane();
        tilepane.setPrefColumns(3);
        tilepane.setPrefTileHeight(200);

        for(int i = 0; i < 4; i++) {
            MockResizable child1 = new MockResizable(300,150);
            Rectangle child2 = new Rectangle(100, 100);
            ParentShim.getChildren(tilepane).addAll(child1, child2);
        }


        assertEquals(300, tilepane.getTileWidth(), 0);
        assertEquals(200, tilepane.getTileHeight(), 0);

        assertEquals(900, tilepane.prefWidth(-1), 0);
        assertEquals(600, tilepane.prefHeight(-1), 0);

        tilepane.autosize();
        tilepane.layout();

        assertEquals(300, tilepane.getTileWidth(), 0);
        assertEquals(200, tilepane.getTileHeight(), 0);

        // test a handful
        Node first = ParentShim.getChildren(tilepane).get(0);
        Node last = ParentShim.getChildren(tilepane).get(7);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(450, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testHorizontalTilePaneWithPrefSize() {
        TilePane tilepane = new TilePane(Orientation.HORIZONTAL);
        tilepane.setPrefSize(100,100);

        for(int i = 0; i < 8; i++) {
            Rectangle child = new Rectangle(40, 40);
            ParentShim.getChildren(tilepane).add(child);
        }


        assertEquals(40, tilepane.getTileWidth(), 0);
        assertEquals(40, tilepane.getTileHeight(), 0);

        assertEquals(100, tilepane.prefWidth(-1), 0);
        assertEquals(100, tilepane.prefHeight(-1), 0);

        tilepane.autosize();
        tilepane.layout();

        assertEquals(40, tilepane.getTileWidth(), 0);
        assertEquals(40, tilepane.getTileHeight(), 0);

        Node first = ParentShim.getChildren(tilepane).get(0);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, tilepane.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(160, tilepane.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testVerticalTilePaneWithPrefSize() {
        TilePane tilepane = new TilePane(Orientation.VERTICAL);
        tilepane.setPrefSize(100,100);

        for(int i = 0; i < 8; i++) {
            Rectangle child = new Rectangle(40, 40);
            ParentShim.getChildren(tilepane).add(child);
        }

        assertEquals(40, tilepane.getTileWidth(), 0);
        assertEquals(40, tilepane.getTileHeight(), 0);

        assertEquals(100, tilepane.prefWidth(-1), 0);
        assertEquals(100, tilepane.prefHeight(-1), 0);
        tilepane.autosize();
        tilepane.layout();

        assertEquals(40, tilepane.getTileWidth(), 0);
        assertEquals(40, tilepane.getTileHeight(), 0);

        Node first = ParentShim.getChildren(tilepane).get(0);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(160, tilepane.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, tilepane.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test
    public void testTilePaneWithBaselineAlignment() {
        TilePane tilepane = new TilePane(Orientation.HORIZONTAL);
        tilepane.setPrefSize(100,100);
        tilepane.setTileAlignment(Pos.BASELINE_CENTER);

        for(int i = 0; i < 8; i++) {
            Rectangle child = new Rectangle(40, 40);
            ParentShim.getChildren(tilepane).add(child);
        }

        tilepane.autosize();
        tilepane.layout();

        Node first = ParentShim.getChildren(tilepane).get(0);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, tilepane.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(160, tilepane.getLayoutBounds().getHeight(), 1e-100);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    public void testMinAndPrefSizeSnapInsetsAtRenderScale(double scaleX, double scaleY) {
        var horizontal = new TilePane(Orientation.HORIZONTAL);
        horizontal.setPadding(new Insets(9.6));
        horizontal.setPrefColumns(1);
        horizontal.setPrefTileWidth(100);
        horizontal.setPrefTileHeight(100);
        horizontal.getChildren().add(new Region());

        var vertical = new TilePane(Orientation.VERTICAL);
        vertical.setPadding(new Insets(9.6));
        vertical.setPrefRows(1);
        vertical.setPrefTileWidth(100);
        vertical.setPrefTileHeight(100);
        vertical.getChildren().add(new Region());

        Stage stage = showAtScale(scaleX, scaleY, horizontal, vertical);

        try {
            double expectedWidth = horizontal.snapSpaceX(
                horizontal.snappedLeftInset() + horizontal.getTileWidth() + horizontal.snappedRightInset());

            double expectedHeight = vertical.snapSpaceY(
                vertical.snappedTopInset() + vertical.getTileHeight() + vertical.snappedBottomInset());

            assertEquals(expectedWidth, horizontal.minWidth(-1), EPSILON);
            assertEquals(expectedWidth, horizontal.prefWidth(-1), EPSILON);
            assertEquals(expectedHeight, vertical.minHeight(-1), EPSILON);
            assertEquals(expectedHeight, vertical.prefHeight(-1), EPSILON);
        } finally {
            stage.hide();
        }
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    public void testExactFitDoesNotWrapAtRenderScale(double scaleX, double scaleY) {
        double tileWidth = 2 / scaleX;
        double tileHeight = 2 / scaleY;
        double hgap = 1 / scaleX;
        double vgap = 1 / scaleY;
        double twoTileWidth = 5 / scaleX;
        double twoTileHeight = 5 / scaleY;
        double secondTileX = 3 / scaleX;
        double secondTileY = 3 / scaleY;

        MockResizable firstHorizontal = new MockResizable(0, 0);
        MockResizable secondHorizontal = new MockResizable(0, 0);
        TilePane horizontal = new TilePane(Orientation.HORIZONTAL, hgap, 0, firstHorizontal, secondHorizontal);
        horizontal.setPrefColumns(2);
        horizontal.setPrefTileWidth(tileWidth);
        horizontal.setPrefTileHeight(tileHeight);

        MockResizable firstVertical = new MockResizable(0, 0);
        MockResizable secondVertical = new MockResizable(0, 0);
        TilePane vertical = new TilePane(Orientation.VERTICAL, 0, vgap, firstVertical, secondVertical);
        vertical.setPrefRows(2);
        vertical.setPrefTileWidth(tileWidth);
        vertical.setPrefTileHeight(tileHeight);

        Stage stage = showAtScale(scaleX, scaleY, horizontal, vertical);

        try {
            assertEquals(twoTileWidth, horizontal.prefWidth(-1), EPSILON);
            assertEquals(twoTileHeight, vertical.prefHeight(-1), EPSILON);

            horizontal.resize(twoTileWidth, tileHeight);
            horizontal.requestLayout();
            horizontal.layout();

            assertEquals(firstHorizontal.getLayoutY(), secondHorizontal.getLayoutY(), EPSILON);
            assertEquals(secondTileX, secondHorizontal.getLayoutX(), EPSILON);

            vertical.resize(tileWidth, twoTileHeight);
            vertical.requestLayout();
            vertical.layout();

            assertEquals(firstVertical.getLayoutX(), secondVertical.getLayoutX(), EPSILON);
            assertEquals(secondTileY, secondVertical.getLayoutY(), EPSILON);
        } finally {
            stage.hide();
        }
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    public void testComputedTileSizeDoesNotCeilSnappedAggregate(double scaleX, double scaleY) {
        var child = new Region();
        child.setPrefSize(0.1 / scaleX, 0.1 / scaleY);
        TilePane.setMargin(child, new Insets(11 / scaleY, 1 / scaleX, 1 / scaleY, 11 / scaleX));

        var tilePane = new TilePane(child);
        tilePane.setPrefColumns(1);

        Stage stage = showAtScale(scaleX, scaleY, tilePane);

        try {
            double expectedTileWidth = 13 / scaleX;
            double expectedTileHeight = 13 / scaleY;

            assertEquals(expectedTileWidth, tilePane.getTileWidth(), EPSILON);
            assertEquals(expectedTileHeight, tilePane.getTileHeight(), EPSILON);
            assertEquals(expectedTileWidth, tilePane.prefWidth(-1), EPSILON);
            assertEquals(expectedTileHeight, tilePane.prefHeight(-1), EPSILON);
        } finally {
            stage.hide();
        }
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    public void testBiasedChildUsesActualSnappedTileDimension(double scaleX, double scaleY) {
        double naturalWidth = 100 / scaleX;
        double naturalHeight = 100 / scaleY;
        double fixedWidth = 50 / scaleX;
        double fixedHeight = 50 / scaleY;
        double dependentWidth = 200 / scaleX;
        double dependentHeight = 200 / scaleY;

        var horizontalChild = new MockBiased(Orientation.HORIZONTAL, naturalWidth, naturalHeight);
        var horizontal = new TilePane(horizontalChild);
        horizontal.setPrefColumns(1);
        horizontal.setPrefTileWidth(fixedWidth);

        var verticalChild = new MockBiased(Orientation.VERTICAL, naturalWidth, naturalHeight);
        var vertical = new TilePane(Orientation.VERTICAL, verticalChild);
        vertical.setPrefRows(1);
        vertical.setPrefTileHeight(fixedHeight);

        Stage stage = showAtScale(scaleX, scaleY, horizontal, vertical);

        try {
            assertEquals(fixedWidth, horizontal.getTileWidth(), EPSILON);
            assertEquals(dependentHeight, horizontal.getTileHeight(), EPSILON);
            assertEquals(dependentWidth, vertical.getTileWidth(), EPSILON);
            assertEquals(fixedHeight, vertical.getTileHeight(), EPSILON);

            horizontal.resize(fixedWidth, dependentHeight);
            horizontal.requestLayout();
            horizontal.layout();
            assertEquals(fixedWidth, horizontalChild.getWidth(), EPSILON);
            assertEquals(dependentHeight, horizontalChild.getHeight(), EPSILON);

            vertical.resize(dependentWidth, fixedHeight);
            vertical.requestLayout();
            vertical.layout();
            assertEquals(dependentWidth, verticalChild.getWidth(), EPSILON);
            assertEquals(fixedHeight, verticalChild.getHeight(), EPSILON);
        } finally {
            stage.hide();
        }
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    public void testTileSizeCacheTracksRenderScaleAndSnapPolicy(double scaleX, double scaleY) {
        var tilePane = new TilePane();
        tilePane.setPrefTileWidth(0.1);
        tilePane.setPrefTileHeight(0.1);

        var renderScaleX = new SimpleDoubleProperty(1);
        var renderScaleY = new SimpleDoubleProperty(1);
        var stage = new Stage();
        stage.renderScaleXProperty().bind(renderScaleX);
        stage.renderScaleYProperty().bind(renderScaleY);
        stage.setScene(new Scene(tilePane, 100, 100));
        stage.show();

        try {
            assertEquals(1, tilePane.tileWidthProperty().get(), EPSILON);
            assertEquals(1, tilePane.tileHeightProperty().get(), EPSILON);

            renderScaleX.set(scaleX);

            assertEquals(1 / scaleX, tilePane.tileWidthProperty().get(), EPSILON);
            assertEquals(1, tilePane.tileHeightProperty().get(), EPSILON);

            renderScaleY.set(scaleY);

            assertEquals(1 / scaleX, tilePane.tileWidthProperty().get(), EPSILON);
            assertEquals(1 / scaleY, tilePane.tileHeightProperty().get(), EPSILON);

            tilePane.setSnapToPixel(false);
            assertEquals(0.1, tilePane.tileWidthProperty().get(), EPSILON);
            assertEquals(0.1, tilePane.tileHeightProperty().get(), EPSILON);

            tilePane.setSnapToPixel(true);
            assertEquals(1 / scaleX, tilePane.tileWidthProperty().get(), EPSILON);
            assertEquals(1 / scaleY, tilePane.tileHeightProperty().get(), EPSILON);
        } finally {
            stage.hide();
        }
    }

    @Test
    public void testCSSsetPrefTileWidthAndHeight_RT20388() {
        Scene scene = new Scene(tilepane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ParsedValue pv = new CssParserShim().parseExpr("-fx-perf-tile-width","67.0");
        Object val = pv.convert(null);
        try {
            ((StyleableProperty)tilepane.prefTileWidthProperty()).applyStyle(null, val);
            assertEquals(67.0, tilepane.getPrefTileWidth(), 0.00001);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCSSsetPrefRow_RT20437() {
        Scene scene = new Scene(tilepane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ParsedValue pv = new CssParserShim().parseExpr("-fx-perf-rows","2");
        Object val = pv.convert(null);
        try {
            ((StyleableProperty)tilepane.prefRowsProperty()).applyStyle(null, val);
            assertEquals(2, tilepane.getPrefRows(), 0.00001);
        } catch (Exception e) {
            fail(e.toString());
        }
    }


    @Test
    public void testCSSsetPrefColumns_RT22929() {
        Scene scene = new Scene(tilepane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ParsedValue pv = new CssParserShim().parseExpr("-fx-pref-columns","2");
        Object val = pv.convert(null);
        CssMetaData prop = ((StyleableProperty)tilepane.prefColumnsProperty()).getCssMetaData();
        try {
            ((StyleableProperty)tilepane.prefColumnsProperty()).applyStyle(null,val);
            assertEquals(2, tilepane.getPrefColumns(), 0.00001);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    private static Stream<Arguments> renderScales() {
        return Stream.of(
            Arguments.of(1.0, 1.0),
            Arguments.of(1.25, 1.5),
            Arguments.of(1.5, 1.5),
            Arguments.of(1.75, 2.25),
            Arguments.of(1.0, 1.75)
        );
    }

    private static Stage showAtScale(double scaleX, double scaleY, Node... nodes) {
        Pane root = new Pane(nodes);
        Stage stage = new Stage();
        stage.renderScaleXProperty().bind(new SimpleDoubleProperty(scaleX));
        stage.renderScaleYProperty().bind(new SimpleDoubleProperty(scaleY));
        stage.setScene(new Scene(root, 400, 400));
        stage.show();
        return stage;
    }
}
