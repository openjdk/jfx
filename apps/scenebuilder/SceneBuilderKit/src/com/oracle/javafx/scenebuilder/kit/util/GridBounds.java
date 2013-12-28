/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.javafx.scenebuilder.kit.util;

/**
 *
 */
public class GridBounds {
    
    private final int minColumnIndex;
    private final int minRowIndex;
    private final int columnSpan;
    private final int rowSpan;

    public GridBounds(int columnIndex, int rowIndex, int columnSpan, int rowSpan) {
        assert (columnSpan >= 0);
        assert (rowSpan >= 0);
        this.minColumnIndex = columnIndex;
        this.minRowIndex = rowIndex;
        this.columnSpan = columnSpan;
        this.rowSpan = rowSpan;
    }

    public int getMinColumnIndex() {
        return minColumnIndex;
    }

    public int getMinRowIndex() {
        return minRowIndex;
    }

    public int getColumnSpan() {
        return columnSpan;
    }

    public int getRowSpan() {
        return rowSpan;
    }
    
    public int getMaxColumnIndex() {
        return minColumnIndex + columnSpan;
    }
    
    public int getMaxRowIndex() {
        return minRowIndex + rowSpan;
    }

    public boolean isEmpty() {
        return (columnSpan == 0) || (rowSpan == 0);
    }
    
    public GridBounds move(int columnDelta, int rowDelta) {
        final int newColumnIndex = minColumnIndex + columnDelta;
        final int newRowIndex = minRowIndex + rowDelta;
        return new GridBounds(newColumnIndex, newRowIndex, columnSpan, rowSpan);
    }
    
    public GridBounds union(GridBounds gridBounds) {
        final int newMinColumnIndex = Math.min(minColumnIndex, gridBounds.minColumnIndex);
        final int newMinRowIndex = Math.min(minRowIndex, gridBounds.minRowIndex);
        final int newMaxColumnIndex = Math.max(getMaxColumnIndex(), gridBounds.getMaxColumnIndex());
        final int newMaxRowIndex = Math.max(getMaxRowIndex(), gridBounds.getMaxRowIndex());
        final int newColumnSpan = newMaxColumnIndex - newMinColumnIndex;
        final int newRowSpan = newMaxRowIndex - newMinRowIndex;
        return new GridBounds(newMinColumnIndex, newMinRowIndex, newColumnSpan, newRowSpan);
    }
}
