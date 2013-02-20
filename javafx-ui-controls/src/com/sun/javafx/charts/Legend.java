/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.charts;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * A chart legend that displays a list of items with symbols in a box
 */
public class Legend extends Region {

    private static final int GAP = 5;

    // -------------- PRIVATE FIELDS ------------------------------------------

    private int rows = 0, columns = 0;
    private ListChangeListener<LegendItem> itemsListener = new ListChangeListener<LegendItem> () {
        @Override public void onChanged(Change<? extends LegendItem> c) {
            getChildren().clear();
            for (LegendItem item : getItems()) getChildren().add(item.label);
            if(isVisible()) requestLayout();
        }
    };

    // -------------- PUBLIC PROPERTIES ----------------------------------------
    /** The legend items should be laid out vertically in columns rather than
     * horizontally in rows
     */
    private BooleanProperty vertical = new BooleanPropertyBase(false) {
        @Override protected void invalidated() {
            requestLayout();
        }

        @Override
        public Object getBean() {
             return Legend.this;
        }

        @Override
        public String getName() {
             return "vertical";
        }
    };
    public final boolean isVertical() { return vertical.get(); }
    public final void setVertical(boolean value) { vertical.set(value); }
    public final BooleanProperty verticalProperty() { return vertical; }

     /** The legend items to display in this legend */
    private ObjectProperty<ObservableList<LegendItem>> items = new ObjectPropertyBase<ObservableList<LegendItem>>() {
        ObservableList<LegendItem> oldItems = null;
         @Override protected void invalidated() {
             if(oldItems!=null) oldItems.removeListener(itemsListener);
             getChildren().clear();
             ObservableList<LegendItem> newItems = get();
             if(newItems != null) {
                 newItems.addListener(itemsListener);
                 for(LegendItem item: newItems) getChildren().add(item.label);
             }
             oldItems = get();
             requestLayout();
         }

         @Override
         public Object getBean() {
             return Legend.this;
         }

         @Override
         public String getName() {
             return "items";
         }
     };
    public final void setItems(ObservableList<LegendItem> value) {itemsProperty().set(value);}
    public final ObservableList<LegendItem> getItems() { return items.get();}
    public final ObjectProperty<ObservableList<LegendItem>> itemsProperty() {return items;}

    // -------------- PROTECTED PROPERTIES ------------------------------------

    // -------------- CONSTRUCTORS ----------------------------------------------

    public Legend() {
        setItems(FXCollections.<LegendItem>observableArrayList());
        getStyleClass().setAll("chart-legend");
    }

    // -------------- METHODS ---------------------------------------------------

    private Dimension2D getTileSize(){
        double maxWidth = 0;
        double maxHeight = 0;
        for(LegendItem item: getItems()) {
            maxWidth = Math.max(maxWidth, item.label.prefWidth(-1));
            maxHeight = Math.max(maxHeight, item.label.prefHeight(-1));
        }
        return new Dimension2D(Math.ceil(maxWidth), Math.ceil(maxHeight));
    }

    @Override protected double computePrefWidth(double height) {
        final double contentHeight = height - getInsets().getTop() - getInsets().getBottom();
        Dimension2D tileSize = getTileSize();
        if(height == -1) {
            if(columns <= 1) return tileSize.getWidth() + getInsets().getLeft() + getInsets().getRight();
        } else {
            rows = (int) Math.floor( contentHeight / (tileSize.getHeight() + GAP) );
            columns = (rows == 0) ? (int)Math.ceil(getItems().size()) : 
                            (int)Math.ceil(getItems().size() / (double)rows);
        }
        if(columns == 1) rows = Math.min(rows, getItems().size());
        return (columns*(tileSize.getWidth()+GAP)) - GAP + getInsets().getLeft() + getInsets().getRight();
    }

    @Override protected double computePrefHeight(double width) {
        final double contentWidth = width - getInsets().getLeft() - getInsets().getRight();
        Dimension2D tileSize = getTileSize();
        if(width == -1) {
            if(rows <= 1) return tileSize.getHeight() + getInsets().getTop() + getInsets().getBottom();
        } else {
            columns = (int) Math.floor( contentWidth / (tileSize.getWidth() + GAP) );
            rows = (columns == 0) ? (int)Math.ceil(getItems().size()) : 
                            (int)Math.ceil(getItems().size() / (double)columns);
        }
        if(rows == 1) columns = Math.min(columns, getItems().size());
        return (rows*(tileSize.getHeight()+GAP)) - GAP + getInsets().getTop() + getInsets().getBottom();
    }

    @Override protected void layoutChildren() {
        Dimension2D tileSize = getTileSize();
        if(isVertical()) {
            double left = getInsets().getLeft();
            outer: for (int col=0; col < columns; col++) {
                double top = getInsets().getTop();
                for (int row=0; row < rows; row++) {
                    int itemIndex = (col*rows) + row;
                    if(itemIndex >= getItems().size()) break outer;
                    getItems().get(itemIndex).label.resizeRelocate(left,top,tileSize.getWidth(),tileSize.getHeight());
                    top += tileSize.getHeight() + GAP;
                }
                left += tileSize.getWidth() + GAP;
            }
        } else {
            double top = getInsets().getTop();
            outer: for (int row=0; row < rows; row++) {
                double left = getInsets().getLeft();
                for (int col=0; col < columns; col++) {
                    int itemIndex = (row*columns) + col;
                    if(itemIndex >= getItems().size()) break outer;
                    getItems().get(itemIndex).label.resizeRelocate(left,top,tileSize.getWidth(),tileSize.getHeight());
                    left += tileSize.getWidth() + GAP;
                }
                top += tileSize.getHeight() + GAP;
            }
        }
    }

    /** A item to be displayed on a Legend */
    public static class LegendItem {

        /** Label used to represent the legend item */
        private Label label = new Label();

        /** The item text */
        private StringProperty text = new StringPropertyBase() {
            @Override protected void invalidated() {
                label.setText(get());
            }

            @Override
            public Object getBean() {
                return LegendItem.this;
            }

            @Override
            public String getName() {
                return "text";
            }
        };
        public final String getText() { return text.getValue(); }
        public final void setText(String value) { text.setValue(value); }
        public final StringProperty textProperty() { return text; }

        /** The symbol to use next to the item text, set to null for no symbol. The default is a simple square of symbolFill */
        //new Rectangle(8,8,null)
        private ObjectProperty<Node> symbol = new ObjectPropertyBase<Node>(new Region()) {
            @Override protected void invalidated() {
                Node symbol = get();
                if(symbol != null) symbol.getStyleClass().setAll("chart-legend-item-symbol");
                label.setGraphic(symbol);
            }

            @Override
            public Object getBean() {
                return LegendItem.this;
            }

            @Override
            public String getName() {
                return "symbol";
            }
        };
        public final Node getSymbol() { return symbol.getValue(); }
        public final void setSymbol(Node value) { symbol.setValue(value); }
        public final ObjectProperty<Node> symbolProperty() { return symbol; }

        public LegendItem(String text) {
            setText(text);
            label.getStyleClass().add("chart-legend-item");
            label.setAlignment(Pos.CENTER_LEFT);
            label.setContentDisplay(ContentDisplay.LEFT);
            label.setGraphic(getSymbol());
            getSymbol().getStyleClass().setAll("chart-legend-item-symbol");
        }

        public LegendItem(String text, Node symbol) {
            this(text);
            setSymbol(symbol);
        }
    }
}

