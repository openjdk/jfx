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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

/**
 * Region responsible for painting the entire row of column headers.
 */
public class TableHeaderRow extends StackPane {

    /***************************************************************************
     *                                                                         *
     * Static Fields                                                           *
     *                                                                         *
     **************************************************************************/

    private static final String MENU_SEPARATOR = 
            ControlResources.getString("TableView.nestedColumnControlMenuSeparator");



    /***************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final VirtualFlow flow;

    private final TableViewSkinBase tableSkin;

    private Map<TableColumnBase, CheckMenuItem> columnMenuItems = new HashMap<TableColumnBase, CheckMenuItem>();

    // Vertical line that is shown when columns are being reordered
//    private Region columnReorderLine;

    private double scrollX;

    private double tableWidth;

    private Rectangle clip;

    private TableColumnHeader reorderingRegion;

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

    private BooleanProperty reordering = new SimpleBooleanProperty(this, "reordering", false) {
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
    };



    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/
    
    public TableHeaderRow(final TableViewSkinBase skin) {
        this.tableSkin = skin;
        this.flow = skin.flow;

        getStyleClass().setAll("column-header-background");

        // clip the header so it doesn't show outside of the table bounds
        clip = new Rectangle();
        clip.setSmooth(false);
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // listen to table width to keep header in sync
        updateTableWidth();
        tableSkin.getSkinnable().widthProperty().addListener(weakTableWidthListener);
        tableSkin.getSkinnable().paddingProperty().addListener(weakTablePaddingListener);
        skin.getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);

        // popup menu for hiding/showing columns
        columnPopupMenu = new ContextMenu();
        updateTableColumnListeners(tableSkin.getColumns(), Collections.<TableColumnBase<?,?>>emptyList());
        tableSkin.getColumns().addListener(weakTableColumnsListener);

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

        // build the corner region button for showing the popup menu
        final StackPane image = new StackPane();
        image.setSnapToPixel(false);
        image.getStyleClass().setAll("show-hide-column-image");
        cornerRegion = new StackPane() {
            @Override protected void layoutChildren() {
                double imageWidth = image.snappedLeftInset() + image.snappedRightInset();
                double imageHeight = image.snappedTopInset() + image.snappedBottomInset();
                
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

    private InvalidationListener tablePaddingListener = new InvalidationListener() {
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
    
    private final InvalidationListener columnTextListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            TableColumn<?,?> column = (TableColumn<?,?>) ((StringProperty)observable).getBean();
            CheckMenuItem menuItem = columnMenuItems.get(column);
            if (menuItem != null) {
                menuItem.setText(getText(column.getText(), column));
            }
        }
    };
    
    private final WeakInvalidationListener weakTableWidthListener = 
            new WeakInvalidationListener(tableWidthListener);

    private final WeakInvalidationListener weakTablePaddingListener =
            new WeakInvalidationListener(tablePaddingListener);

    private final WeakListChangeListener weakVisibleLeafColumnsListener =
            new WeakListChangeListener(visibleLeafColumnsListener);
    
    private final WeakListChangeListener weakTableColumnsListener =
            new WeakListChangeListener(tableColumnsListener);
    
    private final WeakInvalidationListener weakColumnTextListener = 
            new WeakInvalidationListener(columnTextListener);



    /***************************************************************************
     *                                                                         *
     * Public Methods                                                          *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        double x = scrollX;
        double headerWidth = snapSize(header.prefWidth(-1));
        double prefHeight = getHeight() - snappedTopInset() - snappedBottomInset();
        double cornerWidth = snapSize(flow.getVbar().prefWidth(-1));

        // position the main nested header
        header.resizeRelocate(x, snappedTopInset(), headerWidth, prefHeight);
        
        // position the filler region
        final Control control = tableSkin.getSkinnable();
        if (control == null) {
            return;
        }

        final double controlInsets = control.snappedLeftInset() + control.snappedRightInset();
        double fillerWidth = tableWidth - headerWidth + filler.getInsets().getLeft() - controlInsets;
        fillerWidth -= tableSkin.tableMenuButtonVisibleProperty().get() ? cornerWidth : 0;
        filler.setVisible(fillerWidth > 0);
        if (fillerWidth > 0) {
            filler.resizeRelocate(x + headerWidth, snappedTopInset(), fillerWidth, prefHeight);
        }

        // position the top-right rectangle (which sits above the scrollbar)
        cornerRegion.resizeRelocate(tableWidth - cornerWidth, snappedTopInset(), cornerWidth, prefHeight);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        return header.prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        return snappedTopInset() + header.prefHeight(width) + snappedBottomInset();
    }

    // protected to allow subclasses to provide a custom root header
    protected NestedTableColumnHeader createRootHeader() {
        return new NestedTableColumnHeader(tableSkin, null);
    }

    // protected to allow subclasses access to the TableViewSkinBase instance
    protected TableViewSkinBase getTableSkin() {
        return this.tableSkin;
    }

    // protected to allow subclasses to modify the horizontal scrolling
    protected void updateScrollX() {
        scrollX = flow.getHbar().isVisible() ? -flow.getHbar().getValue() : 0.0F;
        requestLayout();
    }

    public final void setReordering(boolean value) {
        this.reordering.set(value);
    }

    public final boolean isReordering() {
        return reordering.get();
    }

    public final BooleanProperty reorderingProperty() {
        return reordering;
    }

    public TableColumnHeader getReorderingRegion() {
        return reorderingRegion;
    }

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

    public NestedTableColumnHeader getRootHeader() {
        return header;
    }

    // protected to allow subclass to customise the width, to allow for features
    // such as row headers
    protected void updateTableWidth() {
        // snapping added for RT-19428
        final Control c = tableSkin.getSkinnable();
        if (c == null) {
            this.tableWidth = 0;
        } else {
            Insets insets = c.getInsets() == null ? Insets.EMPTY : c.getInsets();
            double padding = snapSize(insets.getLeft()) + snapSize(insets.getRight());
            this.tableWidth = snapSize(c.getWidth()) - padding;
        }

        clip.setWidth(tableWidth);
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

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

        CheckMenuItem item = columnMenuItems.remove(col);
        if (item != null) {
            col.textProperty().removeListener(weakColumnTextListener);
            item.selectedProperty().unbindBidirectional(col.visibleProperty());

            columnPopupMenu.getItems().remove(item);
        }

        if (! col.getColumns().isEmpty()) {
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
            item.setText(getText(col.getText(), col));
            col.textProperty().addListener(weakColumnTextListener);
            item.selectedProperty().bindBidirectional(col.visibleProperty());

            columnPopupMenu.getItems().add(item);
        } else {
            for (TableColumnBase tc : col.getColumns()) {
                add(tc);
            }
        }
    }

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
