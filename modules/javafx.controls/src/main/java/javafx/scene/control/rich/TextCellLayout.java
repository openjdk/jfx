/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich;

import java.util.ArrayList;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.rich.util.Bugs;
import javafx.scene.control.rich.util.NewAPI;
import javafx.scene.control.rich.util.Util;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

/**
 * Manages TextCells in the visible area, surrounded by a number of cells before and after the visible area,
 * for the purposes of layout, estimating the average paragraph height, and relative navigation.
 */
public class TextCellLayout {
    private final ArrayList<TextCell> cells = new ArrayList<>(32);
    private final double flowWidth;
    private final double flowHeight;
    private final double lineSpacing;
    private final int lineCount;
    private final Insets contentPadding;
    private final Origin origin;
    private int visibleCount;
    private int bottomCount;
    private double unwrappedWidth;
    private double topHeight;
    private double bottomHeight;
    private Node[] left;
    private Node[] right;
    
    public TextCellLayout(VFlow f) {
        this.flowWidth = f.getWidth();
        this.flowHeight = f.getViewHeight();
        this.origin = f.getOrigin();
        this.lineSpacing = f.lineSpacing();
        this.lineCount = f.getParagraphCount();
        this.contentPadding = f.contentPadding();
    }

    public boolean isValid(VFlow f) {
        return
            (f.getWidth() == flowWidth) &&
            (f.getHeight() == flowHeight) &&
            (f.topCellIndex() == origin.index()) &&
            (f.lineSpacing() == lineSpacing) &&
            (Util.equals(f.contentPadding(), contentPadding));
    }

    public String toString() {
        return
            "TextCellLayout{" +
            origin +
            ", topCount=" + topCount() +
            ", visible=" + getVisibleCellCount() +
            ", bottomCount=" + bottomCount +
            ", topHeight=" + topHeight +
            ", bottomHeight=" + bottomHeight +
            ", lineCount=" + lineCount +
            ", average=" + averageHeight() +
            ", estMax=" + estimatedMax() +
            ", unwrapped=" + getUnwrappedWidth() +
            "}";
    }

    public void addCell(TextCell cell) {
        cells.add(cell);
    }
    
    public void setUnwrappedWidth(double w) {
        unwrappedWidth = w;
    }
    
    public double getUnwrappedWidth() {
        return unwrappedWidth;
    }
    
    public int getVisibleCellCount() {
        return visibleCount;
    }
    
    public void setVisibleCellCount(int n) {
        visibleCount = n;
    }

    /** finds text position inside the sliding window */
    public TextPos getTextPos(double xoffset, double localX, double localY) {
        if (lineCount == 0) {
            return TextPos.ZERO;
        }

        int topIx = topIndex();
        int btmIx = bottomIndex();

        int ix = binarySearch(localY, topIx, btmIx - 1);
        TextCell cell = getCell(ix);
        if (cell != null) {
            Region r = cell.getContent();
            Insets pad = r.getPadding();
            double y = localY - cell.getY() - pad.getTop();
            if (y < 0) {
                return new TextPos(cell.getIndex(), 0);
            } else if (y < cell.getHeight()) {
                // TODO move this to TextCell?
                if (r instanceof TextFlow t) {
                    double x = localX + xoffset - pad.getLeft();
                    Point2D p = new Point2D(x, y);
                    
                    // FIX https://bugs.openjdk.org/browse/JDK-8302511
                    // gives wrong value for emojis
                    //HitInfo h = t.hitTest(p);
                    //if (h != null) {
                    //    return new TextPos(cell.getLineIndex(), h.getInsertionIndex());
                    //}

                    int ii = Bugs.getInsertionIndex(t, p);
                    return new TextPos(cell.getIndex(), ii);
                } else {
                    return new TextPos(cell.getIndex(), 0);
                }
            } else {
                // TODO
            }
        }

        Region r = cell.getContent();
        int cix = 0;
        if (r instanceof TextFlow f) {
            cix = NewAPI.getTextLength(f);
        }
        return new TextPos(cell.getIndex(), cix);
    }

    /** returns the cell contained in this layout, or null */
    public TextCell getCell(int modelIndex) {
        int ix = modelIndex - origin.index();
        if(ix < 0) {
            if((ix + topCount()) >= 0) {
                // cells in the top part come after bottom part, and in reverse order
                return cells.get(bottomCount - ix - 1);
            }
        } else if(ix < bottomCount) {
            // cells in the normal (bottom) part
            return cells.get(ix);
        }
        return null;
    }
    
    /** returns a visible cell, or null */
    public TextCell getVisibleCell(int modelIndex) {
        int ix = modelIndex - origin.index();
        if((ix >= 0) && (ix < visibleCount)) {
            return cells.get(ix);
        }
        return null;
    }
    
    /** returns a TextCell from the visible or bottom margin parts, or null */
    public TextCell getCellAt(int ix) {
        if(ix < visibleCount) {
            return cells.get(ix);
        }
        return null;
    }

    public CaretInfo getCaretInfo(double xoffset, TextPos p) {
        if (p != null) {
            int ix = p.index();
            TextCell cell = getCell(ix);
            if (cell != null) {
                int charIndex = p.offset();
                boolean leading = true; // TODO verify
                PathElement[] pe = cell.getCaretShape(charIndex, leading);
                return translateCaretInfo(xoffset, cell, pe);
            }
        }
        return null;
    }

    private CaretInfo translateCaretInfo(double xoffset, TextCell cell, PathElement[] elements) {
        double x = 0.0;
        double y0 = 0.0;
        double y1 = 0.0;

        double dx = -xoffset;
        double dy = cell.getY();

        int sz = elements.length;
        for (int i = 0; i < sz; i++) {
            PathElement em = elements[i];
            if (em instanceof LineTo m) {
                x = Util.halfPixel(m.getX() + dx);
                y0 = Util.halfPixel(m.getY() + dy);
            } else if (em instanceof MoveTo m) {
                x = Util.halfPixel(m.getX() + dx);
                y1 = Util.halfPixel(m.getY() + dy);
            }
        }

        if (y0 > y1) {
            return new CaretInfo(x, y1, y0);
        } else {
            return new CaretInfo(x, y0, y1);
        }
    }

    public void removeNodesFrom(Pane p) {
        ObservableList<Node> cs = p.getChildren();
        for (int i = getVisibleCellCount() - 1; i >= 0; --i) {
            TextCell cell = cells.get(i);
            cs.remove(cell.getContent());
        }
    }

    public void setBottomCount(int ix) {
        bottomCount = ix;
    }

    public int bottomCount() {
        return bottomCount;
    }

    public void setBottomHeight(double h) {
        bottomHeight = h;
    }

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
        return (topHeight + bottomHeight) / (topCount() + bottomCount);
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
        //System.err.println("    binarySearch off=" + off + ", high=" + high + ", low=" + low); // FIX
        while (low <= high) {
            // TODO might be a problem for 2B-rows models
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
        if(localY < y) {
            return 1;
        } else if(localY >= y + cell.getHeight()) {
            if(cell.getIndex() == (lineCount - 1)) {
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

    /** creates a new Origin from the absolute position [0.0 ... (1.0-normalized.visible.amount)] */
    public Origin fromAbsolutePosition(double pos) {
        int topIx = topIndex();
        int btmIx = bottomIndex();
        int ix = (int)(pos * lineCount);
        if ((ix >= topIx) && (ix < btmIx)) {
            // inside the layout
            double top = topIx / (double)lineCount;
            double btm = btmIx / (double)lineCount;
            double f = (pos - top) / (btm - top); // TODO check for dvi0/infinity/NaN
            double localY = f * (topHeight + bottomHeight) - topHeight;
            
            ix = binarySearch(localY, topIx, btmIx - 1);
            TextCell cell = getCell(ix);
            return new Origin(cell.getIndex(), localY - cell.getY());
        }
        return new Origin(ix, 0.0);
    }

    public Origin computeOrigin(double delta) {
        int topIx = topIndex();
        int btmIx = bottomIndex();
        double y = delta;

        if (delta < 0) {
            // do not scroll above the top edge
            double top = -origin.offset() - topHeight;
            if (y < top) {
                if(topIx == 0) {
                    double topPadding = (contentPadding == null) ? 0.0 : contentPadding.getTop();
                    y = Math.max(y, -topPadding);
                    return new Origin(0, y);
                }
                return new Origin(topIx, 0.0);
            }
        } else {
            // do not scroll past (bottom edge - visible area)
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
}