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
package ensemble;

import ensemble.control.Popover;
import ensemble.control.PopoverTreeList;
import ensemble.generated.Samples;
import java.util.Comparator;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Popover page that displays a list of samples and sample categories for a given
 * SampleCategory.
 */
public class SamplePopoverTreeList extends PopoverTreeList implements Popover.Page {
    private Popover popover;
    private SampleCategory category;
    private PageBrowser pageBrowser;

    public SamplePopoverTreeList(SampleCategory category, PageBrowser pageBrowser) {
        this.category = category;
        this.pageBrowser = pageBrowser;
        if (category.subCategories!=null) getItems().addAll((Object[])category.subCategories);
        if (category.samples!=null) getItems().addAll((Object[])category.samples);
        getItems().sort(new Comparator() {

            private String getName(Object o) {
                if (o instanceof SampleCategory) {
                    return ((SampleCategory) o).name;
                } else if (o instanceof SampleInfo) {
                    return ((SampleInfo) o).name;
                } else {
                    return "";
                }
            }

            @Override
            public int compare(Object o1, Object o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
    }

    @Override public ListCell call(ListView p) {
        return new SampleItemListCell();
    }

    @Override protected void itemClicked(Object item) {
        if (item instanceof SampleCategory) {
            popover.pushPage(new SamplePopoverTreeList((SampleCategory)item, pageBrowser));
        } else if (item instanceof SampleInfo) {
            popover.hide();
            pageBrowser.goToSample((SampleInfo)item);
        }
    }

    @Override public void setPopover(Popover popover) {
        this.popover = popover;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override public Node getPageNode() {
        return this;
    }

    @Override public String getPageTitle() {
        return "Samples";
    }

    @Override public String leftButtonText() {
        return category == Samples.ROOT ? null : "< Back";
    }

    @Override public void handleLeftButton() {
        popover.popPage();
    }

    @Override public String rightButtonText() {
        return "Done";
    }

    @Override public void handleRightButton() {
        popover.hide();
    }

    @Override public void handleShown() { }
    @Override public void handleHidden() { }


    private class SampleItemListCell extends ListCell implements EventHandler<MouseEvent> {
        private ImageView arrow = new ImageView(RIGHT_ARROW);
        private Region icon = new Region();

        private SampleItemListCell() {
            super();
            getStyleClass().setAll("sample-tree-list-cell");
            setOnMouseClicked(this);
            setGraphic(icon);
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
            if (arrow.getParent() != this) getChildren().add(arrow);
            super.layoutChildren();
            final int w = (int)getWidth();
            final int h = (int)getHeight();
            final Bounds arrowBounds = arrow.getLayoutBounds();
            arrow.setLayoutX(w - arrowBounds.getWidth() - 12);
            arrow.setLayoutY((int)((h - arrowBounds.getHeight())/2d));
        }

        // CELL METHODS
        @Override protected void updateItem(Object item, boolean empty) {
            // let super do its work
            super.updateItem(item,empty);
            // update our state
            if (item == null) { // empty item
                setText(null);
                arrow.setVisible(false);
                icon.getStyleClass().clear();
            } else {
                setText(item.toString());
                arrow.setVisible(true);
                if (item instanceof SampleCategory) {
                    icon.getStyleClass().setAll("folder-icon");
                } else {
                    icon.getStyleClass().setAll("samples-icon");
                }
            }
        }
    }
}