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
 * A simple interface used by controls such as {@link TableView} and 
 * {@link TreeTableView} to allow for specification of {@link CellSpan cell spans}
 * for a given row/column intersection.
 * 
 * @see CellSpan
 * @see TableView#spanModelProperty() 
 * @see TreeTableView#spanModelProperty() 
 */
public interface SpanModel<T> {
    
    /**
     * A method that, when called, should return a {@link CellSpan} instance for
     * the given row/column intersection to specify whether there should be any
     * cell spanning for that given cell. It is valid to return null - this simply
     * states that there should be no cell spanning for the given cell.
     * 
     * <p>Note that this method is called very frequently, so it is critical that
     * it be performant. If possible, the CellSpan instances should be pre-built
     * and reused, rather than creating new instances for every request.
     * 
     * @param rowIndex The row index of the cell whose cell span is being requested.
     * @param columnIndex The column index of the cell whose cell span is being requested.
     * @param rowObject The backing object for the entire row, from which the cell
     *      retrieves its value.
     * @param tableColumn A concrete subclass of {@link TableColumnBase} that 
     *      represents the column that the cell is located within.
     * @return A CellSpan representing the amount of spanning that the given cell
     *      intersection should possess, or null if no cell spanning is desired.
     */
    public CellSpan getCellSpanAt(final int rowIndex, final int columnIndex, 
            final T rowObject, final TableColumnBase<?,T> tableColumn);
}
