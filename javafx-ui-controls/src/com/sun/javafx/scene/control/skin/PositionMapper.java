/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.Utils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Callback;

/**
 * Base implementation of a helper class used by virtualized Controls such as
 * ListView for mapping between the position of the scroll bar thumb (the
 * scrollbar value) and the position of an item (such as a ListView cell)
 * within the viewport.
 */

public class PositionMapper {
    /**
     * The mapped position, representative of the scrollbar position.
     */
    private ReadOnlyDoubleWrapper position;

    private void setPosition(double value) {
        positionPropertyImpl().set(value);
    }

    public final double getPosition() {
        return position == null ? 0.0 : position.get();
    }

    public final ReadOnlyDoubleProperty positionProperty() {
        return positionPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper positionPropertyImpl() {
        if (position == null) {
            position = new ReadOnlyDoubleWrapper(this, "position");
        }
        return position;
    }
    /**
     * The size of the viewport. For example, in vertical lists, the
     * viewportSize would be the height of the viewport while in horizontal
     * lists it would be the width of the viewport.
     * <p>
     * If the viewport changes size dynamically, then the position will be
     * modified accordingly so as to always be accurate with respect to the
     * viewport, when appropriate.
     */
    private DoubleProperty viewportSize;

    public final void setViewportSize(double value) {
        viewportSizeProperty().set(value);
    }

    public final double getViewportSize() {
        return viewportSize == null ? 0.0 : viewportSize.get();
    }

    public final DoubleProperty viewportSizeProperty() {
        if (viewportSize == null) {
            viewportSize = new SimpleDoubleProperty(this, "viewportSize");
        }
        return viewportSize;
    }
    /**
     * The total number of items to be mapped. This would be, for example,
     * the number of items in the ListView.
     * <p>
     * If the itemCount changes dynamically then the implementation of
     * PositionMapper is responsible for making sure that it adjusts the
     * position accordingly.
     */
    private IntegerProperty itemCount;

    public final void setItemCount(int value) {
        itemCountProperty().set(value);
    }

    public final int getItemCount() {
        return itemCount == null ? 0 : itemCount.get();
    }

    public final IntegerProperty itemCountProperty() {
        if (itemCount == null) {
            itemCount = new SimpleIntegerProperty(this, "itemCount");
        }
        return itemCount;
    }

    /**
     * A function callback which will compute and return the size of the
     * item specified by its index. For example, in a vertical list, the
     * item size would be the height of the cell, whereas in a horizontal
     * list the item size would be the width of the cell.
     */
    private Callback<Integer, Double> itemSize;
    private Callback<Integer, Double> getItemSize() { return itemSize; }
    public void setGetItemSize(Callback<Integer, Double> itemSize) {
        this.itemSize = itemSize;
    }

    /**
     * Given a position value between 0 and 1, compute and return the viewport
     * offset from the "current" cell associated with that position value.
     * That is, if the return value of this function where used as a translation
     * factor for a sheet that contained all the items, then the current
     * item would end up positioned correctly.
     */
    public double computeViewportOffset(double position) {
        double p = Utils.clamp(0, position, 1);
        int count = getItemCount();
        double fractionalPosition = p * count;
        int cellIndex = (int) fractionalPosition;
        double fraction = fractionalPosition - cellIndex;
        double cellSize = getItemSize().call(cellIndex);
        double pixelOffset = cellSize * fraction;
        double viewportOffset = getViewportSize() * p;
        return pixelOffset - viewportOffset;
    }

    /**
     * Given an item index, this function will compute and return the viewport
     * offset from the beginning of the specified item.
     */
    //public abstract function computeOffsetForCell(itemIndex:Integer):Number;

    /**
     * Simply adjusts the position to the given value, clamped between 0 and 1
     * inclusive.
     */
    public void adjustPosition(double pos) {
        setPosition(Utils.clamp(0, pos, 1));
    }

    public void adjustPositionToIndex(int index) {
        if (getItemCount() <= 0) {
            setPosition(0.0f);
        } else {            
            adjustPosition(((double)index) / getItemCount());
        }
    }

    /**
     * Adjust the position based on a delta of pixels. If negative, then the
     * position will be adjusted negatively. If positive, then the position will
     * be adjusted positively. If the pixel amount is too great for the range of
     * the position, then it will be clamped such that position is always
     * strictly between 0 and 1
     */
    public void adjustByPixelAmount(double numPixels) {
        if (numPixels == 0) return;
        // Starting from the current cell, we move in the direction indicated
        // by numPixels one cell at a team. For each cell, we discover how many
        // pixels the "position" line would move within that cell, and adjust
        // our count of numPixels accordingly. When we come to the "final" cell,
        // then we can take the remaining number of pixels and multiply it by
        // the "travel rate" of "p" within that cell to get the delta. Add
        // the delta to "p" to get position.

        // get some basic info about the list and the current cell
        boolean forward = numPixels > 0;
        int count = getItemCount();
        double fractionalPosition = getPosition() * count;
        int cellIndex = (int) fractionalPosition;
        if (forward && cellIndex == count) return;
        double cellSize = getItemSize().call(cellIndex);
        double fraction = fractionalPosition - cellIndex;
        double pixelOffset = cellSize * fraction;

        // compute the percentage of "position" that represents each cell
        double cellPercent = 1.0 / count;

        // To help simplify the algorithm, we pretend as though the current
        // position is at the beginning of the current cell. This reduces some
        // of the corner cases and provides a simpler algorithm without adding
        // any overhead to performance.
        double start = computeOffsetForCell(cellIndex);
        double end = cellSize + computeOffsetForCell(cellIndex + 1);

        // We need to discover the distance that the fictional "position line"
        // would travel within this cell, from its current position to the end.
        double remaining = end - start;

        // Keep track of the number of pixels left to travel
        double n = forward ?
              numPixels + pixelOffset - (getViewportSize() * getPosition()) - start
            : -numPixels + end - (pixelOffset - (getViewportSize() * getPosition()));

        // "p" represents the most recent value for position. This is always
        // based on the edge between two cells, except at the very end of the
        // algorithm where it is added to the computed "p" offset for the final
        // value of Position.
        double p = cellPercent * cellIndex;

        // Loop over the cells one at a time until either we reach the end of
        // the cells, or we find that the "n" will fall within the cell we're on
        while (n > remaining && ((forward && cellIndex < count - 1) || (! forward && cellIndex > 0))) {
            if (forward) cellIndex++; else cellIndex--;
            n -= remaining;
            cellSize = getItemSize().call(cellIndex);
            start = computeOffsetForCell(cellIndex);
            end = cellSize + computeOffsetForCell(cellIndex + 1);
            remaining = end - start;
            p = cellPercent * cellIndex;
        }

        // if remaining is < n, then we must have hit an end, so as a
        // fast path, we can just set position to 1.0 or 0.0 and return
        // because we know we hit the end
        if (n > remaining) {
            setPosition(forward ? 1.0f : 0.0f);
        } else if (forward) {
            double rate = cellPercent / Math.abs(end - start);
            setPosition(p + (rate * n));
        } else {
            double rate = cellPercent / Math.abs(end - start);
            setPosition((p + cellPercent) - (rate * n));
        }
    }

    //public abstract function adjustItemToTop(itemIndex:Integer):Void;
    //public abstract function adjustItemToBottom(itemIndex:Integer):Void;
    //public abstract function adjustItemToView(itemIndex:Integer):Void;

    public int computeCurrentIndex() {
        return (int) (getPosition() * getItemCount());
    }

    /**
     * Given an item index, this function will compute and return the viewport
     * offset from the beginning of the specified item. Notice that because each
     * item has the same percentage of the position dedicated to it, and since
     * we are measuring from the start of each item, this is a very simple
     * calculation.
     */
    public double computeOffsetForCell(int itemIndex) {
        double count = getItemCount();
        double p = Utils.clamp(0, itemIndex, count) / count;
        return -(getViewportSize() * p);
    }
    
    /**
     * Adjust the position based on a chunk of pixels. The position is based
     * on the start of the scrollbar position.
     */
    public void adjustByPixelChunk(double numPixels) {
        setPosition(0);
        adjustByPixelAmount(numPixels);
    }
}
