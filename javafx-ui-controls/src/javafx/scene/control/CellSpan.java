/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

/**
 * Specifies the amount of spanning a single cell will have in controls such as
 * {@link TableView} and {@link TreeTableView}, when their respective 
 * {@link SpanModel span models} are non-null and returning CellSpan instances. 
 * 
 * <p>A row or column span of one means that the cell will span no more than 
 * usual in that direction, whereas a span of two or more means the cell will 
 * begin to 'overlap' neighbouring cells. Any span of zero or less will be 
 * ignored, with the value being replaced by one.
 */
public final class CellSpan {
    private final int rowSpan;
    private final int columnSpan;

    /**
     * Creates a new immutable CellSpan instance with specified rowSpan and 
     * columnSpan values, assuming that they are greater than zero (any value of
     * less than or equal to zero will be ignored with the span in that direction
     * being set as one).
     * 
     * @param rowSpan The number of cells to span in the vertical direction.
     * @param columnSpan The number of cells to span in the horizontal direction.
     */
    public CellSpan(int rowSpan, int columnSpan) {
        this.rowSpan = rowSpan < 1 ? 1 : rowSpan;
        this.columnSpan = columnSpan < 1 ? 1 : columnSpan;
    }

    /**
     * Returns the amount of spanning to perform in the vertical direction.
     * @return An integer representing how many rows to span, where one represents
     *      no additional span.
     */
    public int getRowSpan() {
        return rowSpan;
    }

    /**
     * Returns the amount of spanning to perform in the horizontal direction.
     * @return An integer representing how many columns to span, where one represents
     *      no additional span.
     */
    public int getColumnSpan() {
        return columnSpan;
    }

    @Override public String toString() {
        return "CellSpan: [ rowSpan: " + rowSpan + ", columnSpan: " + columnSpan + " ] ";
    }
}
