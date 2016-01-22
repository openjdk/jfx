/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.control;

import ensemble.EnsembleApp;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Special ListView designed to look like "Text... &gt;" tree list. Perhaps we ought to have customized
 * a TreeView instead of a ListView (as the TreeView already has the data model all defined).
 *
 * This implementation minimizes classes by just having the PopoverTreeList implementing everything
 * (it is the Control, the Skin, and the CellFactory all in one).
 */
public class PopoverTreeList<T> extends ListView<T> implements Callback<ListView<T>, ListCell<T>> {
    protected static final Image RIGHT_ARROW = new Image(
            EnsembleApp.class.getResource("images/popover-arrow.png").toExternalForm());

    public PopoverTreeList(){
        getStyleClass().clear();
        setCellFactory(this);
    }

    @Override public ListCell<T> call(ListView<T> p) {
        return new TreeItemListCell();
    }

    protected void itemClicked(T item) {}

    private class TreeItemListCell extends ListCell<T> implements EventHandler<MouseEvent> {
        private ImageView arrow = new ImageView(RIGHT_ARROW);

        private TreeItemListCell() {
            super();
            getStyleClass().setAll("popover-tree-list-cell");
            setOnMouseClicked(this);
        }

        @Override public void handle(MouseEvent t) {
            itemClicked(getItem());
        }

        @Override protected double computePrefWidth(double height) {
            return 100;
        }

        @Override protected double computePrefHeight(double width) {
            return 44;
        }

        @Override protected void layoutChildren() {
            if (getChildren().size() < 2) getChildren().add(arrow);
            super.layoutChildren();
            final int w = (int)getWidth();
            final int h = (int)getHeight();
            final int centerX = (int)(w/2d);
            final int centerY = (int)(h/2d);
            final Bounds arrowBounds = arrow.getLayoutBounds();
            arrow.setLayoutX(w - arrowBounds.getWidth() - 12);
            arrow.setLayoutY((int)((h - arrowBounds.getHeight())/2d));
        }

        // CELL METHODS
        @Override protected void updateItem(T item, boolean empty) {
            // let super do its work
            super.updateItem(item,empty);
            // update our state
            if (item == null) { // empty item
                setText(null);
                arrow.setVisible(false);
            } else {
                setText(item.toString());
                arrow.setVisible(true);
            }
        }
    }
}
