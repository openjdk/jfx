/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.ArrayList;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.TextFlow;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Manages TextCells in a sliding window, comprised of the visible area and some number of screenfuls
 * before and after the visible area, for the purposes of layout.
 * The purpose is to estimating the average paragraph height, and to support relative navigation.
 * <p>
 * Uses {@code Vflow.content} coordinates.
 */
public class CellArrangement {
    private final ArrayList<TextCell> cells = new ArrayList<>(32);
    private final double flowWidth;
    private final double flowHeight;
    private final int lineCount;
    private final double contentPaddingTop; // snapped
    private final double contentPaddingBottom; // snapped
    private final Origin origin;
    private int visibleCount;
    private int bottomCount;
    private double unwrappedWidth;
    private double topHeight;
    private double bottomHeight;
    private Node[] left;
    private Node[] right;

    public CellArrangement(VFlow f, double contentPaddingTop, double contentPaddingBottom) {
        this.flowWidth = f.getWidth();
        this.flowHeight = f.getViewPortHeight();
        this.origin = f.getOrigin();
        this.lineCount = f.getParagraphCount();
        this.contentPaddingTop = contentPaddingTop;
        this.contentPaddingBottom = contentPaddingBottom;
    }

    // TODO not called right now, use it to skip reflow when not necessary
    // (may need to include left and right content padding or a sum thereof */
    public boolean isValid(VFlow f, double padLeft, double padTop) {
        return
            (f.getWidth() == flowWidth) &&
            (f.getHeight() == flowHeight) &&
            (f.topCellIndex() == origin.index()) &&
            (contentPaddingTop == padTop) &&
            (contentPaddingBottom == contentPaddingBottom);
    }

    @Override
    public String toString() {
        return
            "CellArrangement{" +
            "origin=" + origin +
            ", topCount=" + topCount() +
            ", bottomCount=" + bottomCount +
            ", visible=" + getVisibleCellCount() +
            ", topHeight=" + topHeight +
            ", bottomHeight=" + bottomHeight +
            ", lineCount=" + lineCount +
            ", average=" + averageHeight() +
            ", unwrapped=" + getUnwrappedWidth() +
            "}";
    }

    public void addCell(TextCell cell) {
        cells.add(cell);
    }

    public void setUnwrappedWidth(double w) {
        unwrappedWidth = w;
    }

    /** returns snapped(ceil) size */
    public double getUnwrappedWidth() {
        return unwrappedWidth;
    }

    public int getVisibleCellCount() {
        return visibleCount;
    }

    public void setVisibleCellCount(int n) {
        visibleCount = n;
    }

    /** finds text position inside the sliding window, in cell coordinates */
    public TextPos getTextPos(double cellX, double cellY) {
        if (lineCount == 0) {
            return TextPos.ZERO;
        }

        int topIx = topIndex();
        int btmIx = bottomIndex();

        int ix = binarySearch(cellY, topIx, btmIx - 1);
        TextCell cell = getCell(ix);
        if (cell != null) {
            Region r = cell.getContent();
            double y = cellY - cell.getY() - r.snappedTopInset();
            if (y < 0) {
                return TextPos.ofLeading(cell.getIndex(), 0);
            } else if (y < cell.getCellHeight()) {
                if (r instanceof TextFlow f) {
                    double x = cellX - f.snappedLeftInset();
                    Point2D p = new Point2D(x - r.getLayoutX(), y - r.getLayoutY());
                    HitInfo h = f.hitTest(p);
                    int ii = h.getInsertionIndex();
                    int ci = h.getCharIndex();
                    boolean leading = h.isLeading();
                    return new TextPos(cell.getIndex(), ii, ci, leading);
                } else {
                    return TextPos.ofLeading(cell.getIndex(), 0);
                }
            }

            int cix = 0;
            if (r instanceof TextFlow f) {
                cix = RichUtils.getTextLength(f);
            }
            return TextPos.ofLeading(cell.getIndex(), cix);
        }

        return TextPos.ZERO;
    }

    /** returns the cell contained in this layout, or null */
    public TextCell getCell(int modelIndex) {
        int ix = modelIndex - origin.index();
        if (ix < 0) {
            if ((ix + topCount()) >= 0) {
                // cells in the top part come after bottom part, and in reverse order
                return cells.get(bottomCount - ix - 1);
            }
        } else if (ix < bottomCount) {
            // cells in the normal (bottom) part
            return cells.get(ix);
        }
        return null;
    }

    /** returns a visible cell, or null */
    public TextCell getVisibleCell(int modelIndex) {
        int ix = modelIndex - origin.index();
        if ((ix >= 0) && (ix < visibleCount)) {
            return cells.get(ix);
        }
        return null;
    }

    /** returns a TextCell from the visible or bottom margin parts, or null */
    public TextCell getCellAt(int ix) {
        if (ix < visibleCount) {
            return cells.get(ix);
        }
        return null;
    }

    /**
     * Creates a CaretInfo.
     * @param target the target region (vflow.content)
     * @param p the text position
     * @return the new CaretInfo
     */
    public CaretInfo getCaretInfo(Region target, TextPos p) {
        if (p != null) {
            int ix = p.index();
            TextCell cell = getCell(ix);
            if (cell != null) {
                int charIndex = p.charIndex();
                boolean leading = p.isLeading();
                PathElement[] path = cell.getCaretShape(target, charIndex, leading);
                if (path != null) {
                    double lineSpacing = cell.getLineSpacing();
                    return CaretInfo.create(lineSpacing, path);
                }
            }
        }
        return null;
    }

    public void removeNodesFrom(Pane p) {
        ObservableList<Node> cs = p.getChildren();
        for (int i = getVisibleCellCount() - 1; i >= 0; --i) {
            TextCell cell = cells.get(i);
            cs.remove(cell);
        }
    }

    public void setBottomCount(int ix) {
        bottomCount = ix;
    }

    /** visible + bottom margin cells */
    public int bottomCount() {
        return bottomCount;
    }

    public int cellCount() {
        return cells.size();
    }

    public void setBottomHeight(double h) {
        bottomHeight = h;
    }

    /** in pixels from the first visible cell to the last cell in the arrangement */
    public double bottomHeight() {
        return bottomHeight;
    }

    public int topCount() {
        return cells.size() - bottomCount;
    }

    public void setTopHeight(double h) {
        topHeight = h;
    }

    public double topHeight() {
        return topHeight;
    }

    public double averageHeight() {
        int sz = cells.size();
        if (sz == 0) {
            return 20; // any reasonable non-zero number would work
        }
        return (topHeight + bottomHeight) / sz;
    }

    public double estimatedMax() {
        return (lineCount - topCount() - bottomCount) * averageHeight() + topHeight + bottomHeight;
    }

    /**
     * finds a model index of a cell that contains the given localY.
     * (in vflow frame of reference).
     * Should not be called with localY outside of this layout sliding window.
     */
    private int binarySearch(double localY, int low, int high) {
        while (low <= high) {
            int mid = (low + high) >>> 1;
            TextCell cell = getCell(mid);
            int cmp = compare(cell, localY);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return low;
    }

    private int compare(TextCell cell, double localY) {
        double y = cell.getY();
        if (localY < y) {
            return 1;
        } else if (localY >= y + cell.getCellHeight()) {
            if (cell.getIndex() == (lineCount - 1)) {
                return 0;
            }
            return -1;
        }
        return 0;
    }

    /** returns a model index of the first cell in the sliding window top margin */
    public int topIndex() {
        return origin.index() - topCount();
    }

    /** returns a model index of the last cell in the sliding window bottom margin + 1 */
    public int bottomIndex() {
        return origin.index() + bottomCount;
    }

    /** returns the new origin after scrolling for delta pixels within the arrangement */
    public Origin moveOrigin(double delta) {
        int topIx = topIndex();
        int btmIx = bottomIndex();
        double y = delta;

        if (delta > 0) {
            // do not scroll beyond the bottom edge
            double max = bottomHeight - flowHeight;
            if (max < 0) {
                return null;
            }
            if (y > max) {
                y = max;
            }
        }

        int ix = binarySearch(y, topIx, btmIx - 1);
        TextCell cell = getCell(ix);
        double off = y - cell.getY();

        // do not scroll beyond the top edge
        if (delta < 0) {
            if (ix == 0) {
                off = Math.max(off, -contentPaddingTop);
            }
        }

        return new Origin(cell.getIndex(), off);
    }

    public void addLeftNode(int index, Node n) {
        if (left == null) {
            left = new Node[visibleCount];
        }
        left[index] = n;
    }

    public void addRightNode(int index, Node n) {
        if (right == null) {
            right = new Node[visibleCount];
        }
        right[index] = n;
    }

    public Node getLeftNodeAt(int index) {
        return left[index];
    }

    public Node getRightNodeAt(int index) {
        return right[index];
    }

    /** last cell at the bottom of the arrangement */
    private TextCell lastBottomCell() {
        if (bottomCount == 0) {
            return null;
        }
        return cells.get(bottomCount - 1);
    }
}
