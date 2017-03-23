/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.cell;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

/**
 * A class containing a {@link TreeTableCell} implementation that draws a
 * {@link ProgressBar} node inside the cell.
 *
 * @param <S> The type of the elements contained within the TableView.
 * @since JavaFX 8.0
 */
public class ProgressBarTreeTableCell<S> extends TreeTableCell<S, Double> {

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Provides a {@link ProgressBar} that allows easy visualisation of a Number
     * value as it proceeds from 0.0 to 1.0. If the value is -1, the progress
     * bar will appear indeterminate.
     *
     * @param <S> The type of the TreeTableView generic type
     * @return A {@link Callback} that can be inserted into the
     *      {@link TreeTableColumn#cellFactoryProperty() cell factory property} of a
     *      TreeTableColumn, that enables visualisation of a Number as it progresses
     *      from 0.0 to 1.0.
     */
    public static <S> Callback<TreeTableColumn<S,Double>, TreeTableCell<S,Double>> forTreeTableColumn() {
        return param -> new ProgressBarTreeTableCell<S>();
    }



    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final ProgressBar progressBar;

    private ObservableValue<Double> observable;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default {@link ProgressBarTreeTableCell} instance
     */
    public ProgressBarTreeTableCell() {
        this.getStyleClass().add("progress-bar-tree-table-cell");

        this.progressBar = new ProgressBar();
        this.progressBar.setMaxWidth(Double.MAX_VALUE);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
        } else {
            progressBar.progressProperty().unbind();

            final TreeTableColumn<S,Double> column = getTableColumn();
            observable = column == null ? null : column.getCellObservableValue(getIndex());

            if (observable != null) {
                progressBar.progressProperty().bind(observable);
            } else if (item != null) {
                progressBar.setProgress(item);
            }

            setGraphic(progressBar);
        }
    }
}
