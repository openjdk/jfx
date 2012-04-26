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
package com.sun.javafx.scene.control.cell;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;

/**
 * A class containing a {@link TableCell} implementation that draws a 
 * {@link ProgressBar} node inside the cell.
 * 
 * @param <S> The type of the elements contained within the TableView.
 */
public class ProgressBarTableCell<S,Double> extends TableCell<S, java.lang.Double> {
    
    private final ProgressBar progressBar;
    private ObservableValue observable;
    
    /**
     * Creates a default {@link ProgressBarTableCell} instance
     */
    public ProgressBarTableCell() {
        this.getStyleClass().add("progress-bar-table-cell");
        
        this.progressBar = new ProgressBar();
        setGraphic(progressBar);
    }
    
    /** {@inheritDoc} */
    @Override public void updateItem(java.lang.Double item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty) {
            setGraphic(null);
        } else {
            progressBar.progressProperty().unbind();
            
            observable = getTableColumn().getCellObservableValue(getIndex());
            if (observable != null) {
                progressBar.progressProperty().bind(observable);
            } else {
                progressBar.setProgress(item);
            }
            
            setGraphic(progressBar);
        }
    }
}