/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.geometry.HPos;
import static javafx.geometry.Orientation.HORIZONTAL;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import static javafx.scene.layout.TilePane.getAlignment;
import static javafx.scene.layout.TilePane.getMargin;

public class CustomTilePane extends TilePane {

    final int pad = 20;

    /**
     * Creates a CustomPane layout.
     */
    public CustomTilePane() {
        super();
    }

    /**
     * Creates a Pane layout.
     * @param children The initial set of children for this pane.
     */
    public CustomTilePane(Node... children) {
        super();
    }

//    @Override protected double computeMinWidth(double height) {
//        return super.computePrefWidth(height);
//    }
//
//    @Override protected double computeMinHeight(double width) {
//        return super.computeMinHeight(width);
//    }
//
//    @Override protected double computeMaxWidth(double height) {
//        return super.computeMaxWidth(height);
//    }
//
//    @Override protected double computeMaxHeight(double width) {
//        return super.computePrefHeight(width);
//    }
//
//    @Override protected double computePrefWidth(double height) {
//        double width = 0;
//        for (Node c : getChildren()) {
//            width = width + c.prefWidth(-1);
//        }
//        return width;
//    }
//
//    double maxHeight = 0;
//    @Override protected double computePrefHeight(double width) {
//        for (Node c : getChildren()) {
//            maxHeight = Math.max(c.prefHeight(-1), maxHeight);
//        }
//        maxHeight += pad;
//        return maxHeight;
//    }
//
//    @Override protected void layoutChildren() {
//        List<Node> sortedChidlren = new ArrayList<>(getChildren());
//        Collections.sort(sortedChidlren, (c1, c2)
//                -> Double.valueOf(c2.prefHeight(-1)).compareTo(
//                        Double.valueOf(c1.prefHeight(-1))));
//        double currentX = pad;
//        for (Node c : sortedChidlren) {
//            double width = c.prefWidth(-1);
//            double height = c.prefHeight(-1);
//            layoutInArea(c, currentX, maxHeight - height, width,
//                    height, 0, HPos.CENTER, VPos.CENTER);
//            currentX = currentX + width + pad;
//        }
//    }

    private Pos getTileAlignmentInternal() {
        Pos localPos = getTileAlignment();
        return localPos == null ? Pos.CENTER : localPos;
    }

    private Pos getAlignmentInternal() {
        Pos localPos = getAlignment();
        return localPos == null ? Pos.TOP_LEFT : localPos;
    }

    private int computeOther(int numNodes, int numCells) {
        double other = (double)numNodes/(double)Math.max(1, numCells);
        return (int)Math.ceil(other);
    }

    private int computeColumns(double width, double tilewidth) {
        return Math.max(1, (int) ((width + snapSpace(getHgap())) / (tilewidth + snapSpace(getHgap()))));
    }

    private int computeRows(double height, double tileheight) {
        return Math.max(1, (int) ((height + snapSpace(getVgap())) / (tileheight + snapSpace(getVgap()))));
    }

    private double computeContentWidth(int columns, double tilewidth) {
        if (columns == 0) {
            return 0;
        }
        return columns * tilewidth + (columns - 1) * snapSpace(getHgap());
    }

    private double computeContentHeight(int rows, double tileheight) {
        if (rows == 0) {
            return 0;
        }
        return rows * tileheight + (rows - 1) * snapSpace(getVgap());
    }

    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
                return 0;
            case CENTER:
                return (width - contentWidth) / 2;
            case RIGHT:
                return width - contentWidth;
            default:
                throw new AssertionError("Unhandled hPos");
        }
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
        switch(vpos) {
            case BASELINE:
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                throw new AssertionError("Unhandled vPos");
        }
    }

    @Override protected void layoutChildren() {

        List<Node> sortedManagedChidlren = new ArrayList<>(getManagedChildren());
        Collections.sort(sortedManagedChidlren, (c1, c2)
                -> Double.valueOf(c2.prefHeight(-1)).compareTo(
                        Double.valueOf(c1.prefHeight(-1))));
        List<Node> managed = sortedManagedChidlren;
        HPos hpos = getAlignmentInternal().getHpos();
        VPos vpos = getAlignmentInternal().getVpos();
        double width = getWidth();
        double height = getHeight();
        double top = snapSpace(getInsets().getTop());
        double left = snapSpace(getInsets().getLeft());
        double bottom = snapSpace(getInsets().getBottom());
        double right = snapSpace(getInsets().getRight());
        double vgap = snapSpace(getVgap());
        double hgap = snapSpace(getHgap());
        double insideWidth = width - left - right;
        double insideHeight = height - top - bottom;

        double tileWidth = getTileWidth() > insideWidth ? insideWidth : getTileWidth();
        double tileHeight = getTileHeight() > insideHeight ? insideHeight : getTileHeight();

        int lastRowRemainder = 0;
        int lastColumnRemainder = 0;
        if (getOrientation() == HORIZONTAL) {
            actualColumns = computeColumns(insideWidth, tileWidth);
            actualRows = computeOther(managed.size(), actualColumns);
            // remainder will be 0 if last row is filled
            lastRowRemainder = hpos != HPos.LEFT?
                 actualColumns - (actualColumns*actualRows - managed.size()) : 0;
        } else {
            // vertical
            actualRows = computeRows(insideHeight, tileHeight);
            actualColumns = computeOther(managed.size(), actualRows);
            // remainder will be 0 if last column is filled
            lastColumnRemainder = vpos != VPos.TOP?
                actualRows - (actualColumns*actualRows - managed.size()) : 0;
        }
        double rowX = left + computeXOffset(insideWidth,
                                            computeContentWidth(actualColumns, tileWidth),
                                            hpos);
        double columnY = top + computeYOffset(insideHeight,
                                            computeContentHeight(actualRows, tileHeight),
                                            vpos);

        double lastRowX = lastRowRemainder > 0?
                          left + computeXOffset(insideWidth,
                                            computeContentWidth(lastRowRemainder, tileWidth),
                                            hpos) :  rowX;
        double lastColumnY = lastColumnRemainder > 0?
                          top + computeYOffset(insideHeight,
                                            computeContentHeight(lastColumnRemainder, tileHeight),
                                            vpos) : columnY;
//        double baselineOffset = getTileAlignmentInternal().getVpos() == VPos.BASELINE ?
//                getAreaBaselineOffset(managed, marginAccessor, i -> tileWidth, tileHeight, false) : -1;
        double baselineOffset = 0;

        int r = 0;
        int c = 0;
        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            double xoffset = r == (actualRows - 1)? lastRowX : rowX;
            double yoffset = c == (actualColumns - 1)? lastColumnY : columnY;

            double tileX = xoffset + (c * (tileWidth + hgap));
            double tileY = yoffset + (r * (tileHeight + vgap));

            Pos childAlignment = getAlignment(child);

            layoutInArea(child, tileX, tileY, tileWidth, tileHeight, baselineOffset,
                    getMargin(child),
                    childAlignment != null? childAlignment.getHpos() : getTileAlignmentInternal().getHpos(),
                    childAlignment != null? childAlignment.getVpos() : getTileAlignmentInternal().getVpos());

            if (getOrientation() == HORIZONTAL) {
                if (++c == actualColumns) {
                    c = 0;
                    r++;
                }
            } else {
                // vertical
                if (++r == actualRows) {
                    r = 0;
                    c++;
                }
            }
        }
    }

    private int actualRows = 0;
    private int actualColumns = 0;

}
