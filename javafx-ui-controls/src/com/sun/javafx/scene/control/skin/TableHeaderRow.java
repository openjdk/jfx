/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.WeakListChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanPropertyBase;
import javafx.scene.control.TableColumnBase;

/**
 * Region responsible for painting the entire row of column headers.
 */
public class TableHeaderRow extends StackPane {
    
    private static final String MENU_SEPARATOR = 
            ControlResources.getString("TableView.nestedColumnControlMenuSeparator");
    
    private final VirtualFlow flow;
    VirtualFlow getVirtualFlow() { return flow; }
//    private final TableView<?> table;
    
    private final TableViewSkinBase tableSkin;
    protected TableViewSkinBase getTableSkin() {
        return this.tableSkin;
    } 

    private Insets tablePadding;
    public void setTablePadding(Insets tablePadding) {
        this.tablePadding = tablePadding;
        updateTableWidth();
    }
    public Insets getTablePadding() {
        return tablePadding == null ? Insets.EMPTY : tablePadding;
    }

    // Vertical line that is shown when columns are being reordered
    private Region columnReorderLine;
    public Region getColumnReorderLine() { return columnReorderLine; }
    public void setColumnReorderLine(Region value) { this.columnReorderLine = value; }

    private double scrollX;

    private double tableWidth;
    public double getTableWidth() { return tableWidth; }
    private void updateTableWidth() {
        // snapping added for RT-19428
        double padding = snapSize(getTablePadding().getLeft()) + snapSize(getTablePadding().getRight());
        this.tableWidth = snapSize(tableSkin.getSkinnable().getWidth()) - padding;
        clip.setWidth(tableWidth);
    }

    private Rectangle clip;

    private BooleanProperty reorderingProperty = new BooleanPropertyBase() {
        @Override protected void invalidated() {
            TableColumnHeader r = getReorderingRegion();
            if (r != null) {
                double dragHeaderHeight = r.getNestedColumnHeader() != null ?
                    r.getNestedColumnHeader().getHeight() :
                    getReorderingRegion().getHeight();

                dragHeader.resize(dragHeader.getWidth(), dragHeaderHeight);
                dragHeader.setTranslateY(getHeight() - dragHeaderHeight);
            }
            dragHeader.setVisible(isReordering());
        }

        @Override
        public Object getBean() {
            return TableHeaderRow.this;
        }

        @Override
        public String getName() {
            return "reordering";
        }
    };
    public final void setReordering(boolean value) { reorderingProperty().set(value); }
    public final boolean isReordering() { return reorderingProperty.get(); }
    public final BooleanProperty reorderingProperty() { return reorderingProperty; }

    private TableColumnHeader reorderingRegion;
    public TableColumnHeader getReorderingRegion() { return reorderingRegion; }

    public void setReorderingColumn(TableColumnBase rc) {
        dragHeaderLabel.setText(rc == null ? "" : rc.getText());
    }

    public void setReorderingRegion(TableColumnHeader reorderingRegion) {
        this.reorderingRegion = reorderingRegion;

        if (reorderingRegion != null) {
            dragHeader.resize(reorderingRegion.getWidth(), dragHeader.getHeight());
        }
    }

    public void setDragHeaderX(double dragHeaderX) {
        dragHeader.setTranslateX(dragHeaderX);
    }

    /**
     * This is the ghosted region representing the table column that is being
     * dragged. It moves along the x-axis but is fixed in the y-axis.
     */
    private StackPane dragHeader;
    private final Label dragHeaderLabel = new Label();

    /*
     * The header row is actually just one NestedTableColumnHeader that spans
     * the entire width. Nested within this is the TableColumnHeader's and
     * NestedTableColumnHeader's, as necessary. This makes it nice and clean
     * to handle column reordering - we basically enforce the rule that column
     * reordering only occurs within a single NestedTableColumnHeader, and only
     * at that level.
     */
    private final NestedTableColumnHeader header;
    
    public NestedTableColumnHeader getRootHeader() {
        return header;
    }

    private Region filler;

    /**
     * This is the region where the user can interact with to show/hide columns.
     * It is positioned in the top-right hand corner of the TableHeaderRow, and
     * when clicked shows a PopupMenu consisting of all leaf columns.
     */
    private Pane cornerRegion;

    /**
     * PopupMenu shown to users to allow for them to hide/show columns in the
     * table.
     */
    private ContextMenu columnPopupMenu;

    
    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/
    
    public TableHeaderRow(final TableViewSkinBase skin) {
//        this.table = table;
        this.tableSkin = skin;
        this.flow = skin.flow;

        getStyleClass().setAll("column-header-background");

        clip = new Rectangle();
        clip.setSmooth(false);
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        updateTableWidth();
        tableSkin.getSkinnable().widthProperty().addListener(weakTableWidthListener);
        skin.getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);

        // --- popup menu for hiding/showing columns
        columnPopupMenu = new ContextMenu();

        updateTableColumnListeners(tableSkin.getColumns(), Collections.<TableColumnBase<?,?>>emptyList());
        tableSkin.getColumns().addListener(weakTableColumnsListener);
        // --- end of popup menu

        // drag header region. Used to indicate the current column being reordered
        dragHeader = new StackPane();
        dragHeader.setVisible(false);
        dragHeader.getStyleClass().setAll("column-drag-header");
        dragHeader.setManaged(false);
        dragHeader.getChildren().add(dragHeaderLabel);

        // the header lives inside a NestedTableColumnHeader
        header = createRootHeader();
        header.setFocusTraversable(false);
        header.setTableHeaderRow(this);

        // The 'filler' area that extends from the right-most column to the edge
        // of the tableview, or up to the 'column control' button
        filler = new Region();
        filler.getStyleClass().setAll("filler");

        // Give focus to the table when an empty area of the header row is clicked.
        // This ensures the user knows that the table has focus.
        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                skin.getSkinnable().requestFocus();
            }
        });

        final StackPane image = new StackPane();
        image.setSnapToPixel(false);
        image.getStyleClass().setAll("show-hide-column-image");
        cornerRegion = new StackPane() {
            @Override protected void layoutChildren() {
                Insets padding = image.getInsets();
                double imageWidth = padding.getLeft() + padding.getRight();
                double imageHeight = padding.getTop() + padding.getBottom();
                
                image.resize(imageWidth, imageHeight);
                positionInArea(image, 0, 0, getWidth(), getHeight() - 3, 
                        0, HPos.CENTER, VPos.CENTER);
            }
        };
        cornerRegion.getStyleClass().setAll("show-hide-columns-button");
        cornerRegion.getChildren().addAll(image);
        cornerRegion.setVisible(tableSkin.tableMenuButtonVisibleProperty().get());
        tableSkin.tableMenuButtonVisibleProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                cornerRegion.setVisible(tableSkin.tableMenuButtonVisibleProperty().get());
                requestLayout();
            }
        });
        cornerRegion.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                // show a popupMenu which lists all columns
                columnPopupMenu.show(cornerRegion, Side.BOTTOM, 0, 0);
                me.consume();
            }
        });

        // the actual header
        // the region that is anchored above the vertical scrollbar
        // a 'ghost' of the header being dragged by the user to force column
        // reordering
        getChildren().addAll(filler, header, cornerRegion, dragHeader);
    }
    
    protected NestedTableColumnHeader createRootHeader() {
        return new NestedTableColumnHeader(tableSkin, null);
    } 
    
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/    
    
    private InvalidationListener tableWidthListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateTableWidth();
        }
    };
    
    private ListChangeListener visibleLeafColumnsListener = new ListChangeListener<TableColumn<?,?>>() {
        @Override public void onChanged(ListChangeListener.Change<? extends TableColumn<?,?>> c) {
            // This is necessary for RT-20300 (but was updated for RT-20840)
            header.setHeadersNeedUpdate();
        }
    };
    
    private final ListChangeListener tableColumnsListener = new ListChangeListener<TableColumn<?,?>>() {
        @Override public void onChanged(Change<? extends TableColumn<?,?>> c) {
            while (c.next()) {
                updateTableColumnListeners(c.getAddedSubList(), c.getRemoved());
            }
        }
    };
    
    private final WeakInvalidationListener weakTableWidthListener = 
            new WeakInvalidationListener(tableWidthListener);
    
    private final WeakListChangeListener weakVisibleLeafColumnsListener =
            new WeakListChangeListener(visibleLeafColumnsListener);
    
    private final WeakListChangeListener weakTableColumnsListener =
            new WeakListChangeListener(tableColumnsListener);
    

    private Map<TableColumnBase, CheckMenuItem> columnMenuItems = new HashMap<TableColumnBase, CheckMenuItem>();
    private void updateTableColumnListeners(List<? extends TableColumnBase<?,?>> added, List<? extends TableColumnBase<?,?>> removed) {
        // remove binding from all removed items
        for (TableColumnBase tc : removed) {
            remove(tc);
        }

        // add listeners to all added items
        for (final TableColumnBase tc : added) {
            add(tc);
        }
    }
    
    private void remove(TableColumnBase<?,?> col) {
        if (col == null) return;
        
        if (col.getColumns().isEmpty()) {
            CheckMenuItem item = columnMenuItems.remove(col);
            if (item == null) return;
            
            item.textProperty().unbind();
            item.selectedProperty().unbindBidirectional(col.visibleProperty());

            columnPopupMenu.getItems().remove(item);
        } else {
            for (TableColumnBase tc : col.getColumns()) {
                remove(tc);
            }
        }
    }
    
    private void add(final TableColumnBase<?,?> col) {
        if (col == null) return;
        
        if (col.getColumns().isEmpty()) {
            CheckMenuItem item = columnMenuItems.get(col);
            if (item == null) {
                item = new CheckMenuItem();
                columnMenuItems.put(col, item);
            }
            
            // bind column text and isVisible so that the menu item is always correct
            item.textProperty().bind(new StringBinding() {
                { super.bind(col.textProperty()); }
                
                @Override protected String computeValue() {
                    return getText(col.getText(), col);
                }
            });
            item.selectedProperty().bindBidirectional(col.visibleProperty());
            
            columnPopupMenu.getItems().add(item);
        } else {
            for (TableColumnBase tc : col.getColumns()) {
                add(tc);
            }
        }
    }

    void updateScrollX() {
        scrollX = flow.getHbar().isVisible() ? -flow.getHbar().getValue() : 0.0F;
        requestLayout();
    }

    @Override protected void layoutChildren() {
        double x = scrollX;
        double headerWidth = snapSize(header.prefWidth(-1));
        double prefHeight = getHeight() - getInsets().getTop() - getInsets().getBottom();
        double cornerWidth = snapSize(flow.getVbar().prefWidth(-1));

        // position the main nested header
        header.resizeRelocate(x, getInsets().getTop(), headerWidth, prefHeight);
        
        // position the filler region
        double border = filler.getBoundsInLocal().getWidth() - filler.getLayoutBounds().getWidth();
        double fillerWidth = tableWidth - headerWidth + border;
        fillerWidth -= tableSkin.tableMenuButtonVisibleProperty().get() ? cornerWidth : 0;
        filler.setVisible(fillerWidth > 0);
        if (fillerWidth > 0) {
            filler.resizeRelocate(x + headerWidth, getInsets().getTop(), fillerWidth, prefHeight);
        }

        // position the top-right rectangle (which sits above the scrollbar)
        cornerRegion.resizeRelocate(tableWidth - cornerWidth, getInsets().getTop(), cornerWidth, prefHeight);
    }

    @Override protected double computePrefWidth(double height) {
        return header.prefWidth(height);
    }

    @Override protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }
    
    @Override protected double computePrefHeight(double width) {
        return getInsets().getTop() + header.prefHeight(width) + getInsets().getBottom();
    }

    //    public function isColumnFullyVisible(col:TableColumn):Number {
    //        if (not col.visible) return 0;
    //
    //        // work out where the header is in 0-based coordinates
    //        var start:Number = scrollX;
    //        for (c in table.visibleLeafColumns) {
    //            if (c == col) break;
    //            start += c.width;
    //        }
    //        var end = start + col.width;
    //
    //        // determine the width of the header (taking into account any scrolling)
    //        var headerWidth = /*scrollX +*/ (clip as Rectangle).width;
    //
    //        return if (start < 0 or end > headerWidth) then start else 0;
    //    }

    /*
     * Function used for building the strings in the popup menu
     */
    private String getText(String text, TableColumnBase col) {
        String s = text;
        TableColumnBase parentCol = col.getParentColumn();
        while (parentCol != null) {
            if (isColumnVisibleInHeader(parentCol, tableSkin.getColumns())) {
                s = parentCol.getText() + MENU_SEPARATOR + s;
            }
            parentCol = parentCol.getParentColumn();
        }
        return s;
    }
    
    // We need to show strings properly. If a column has a parent column which is
    // not inserted into the TableView columns list, it effectively doesn't have
    // a parent column from the users perspective. As such, we shouldn't include
    // the parent column text in the menu. Fixes RT-14482.
    private boolean isColumnVisibleInHeader(TableColumnBase col, List columns) {
        if (col == null) return false;
        
        for (int i = 0; i < columns.size(); i++) {
            TableColumnBase column = (TableColumnBase) columns.get(i);
            if (col.equals(column)) return true;
            
            if (! column.getColumns().isEmpty()) {
                boolean isVisible = isColumnVisibleInHeader(col, column.getColumns());
                if (isVisible) return true;
            }
        }
        
        return false;
    }
}
