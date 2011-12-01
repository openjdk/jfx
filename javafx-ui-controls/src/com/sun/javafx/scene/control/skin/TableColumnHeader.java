/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableColumn.SortType.DESCENDING;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.WeakListChangeListener;
import javafx.beans.WeakInvalidationListener;


/**
 * Region responsible for painting a single column header.
 */
public class TableColumnHeader extends StackPane {
    
    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    public TableColumnHeader(TableView table, TableColumn tc) {
        this.column = tc;
        this.table = table;

        getStyleClass().setAll("column-header");

        setFocusTraversable(false);

        updateColumnIndex();
        initUI();
        
        table.getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);
        
        if (getTableColumn() != null && getTableView() != null) {
            setSortPos(! getTableColumn().isSortable() ? -1 : getTableView().getSortOrder().indexOf(getTableColumn()));
            getTableView().getSortOrder().addListener(weakSortOrderListener);
        }

        if (getTableColumn() != null) {
            getTableColumn().visibleProperty().addListener(weakVisibleListener);
            getTableColumn().widthProperty().addListener(weakWidthListener);
        }
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/
    
    private final InvalidationListener visibleListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            setVisible(getTableColumn().isVisible());
        }
    };
    
    private ListChangeListener<TableColumn<?,?>> sortOrderListener = new ListChangeListener<TableColumn<?,?>>() {
        @Override public void onChanged(Change<? extends TableColumn<?,?>> c) {
            setSortPos(! getTableColumn().isSortable() ? -1 : getTableView().getSortOrder().indexOf(getTableColumn()));
        }
    };
    
    private InvalidationListener widthListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            // It is this that ensures that when a column is resized that the header
            // visually adjusts its width as necessary.
            isSizeDirty = true;
            requestLayout();
        }
    };
    
    private InvalidationListener sortTypeListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            arrow.setRotate(column.getSortType() == ASCENDING ? 180 : 0.0);
            updateSortGrid();
        }
    };
    
    private ListChangeListener<TableColumn<?,?>> visibleLeafColumnsListener = new ListChangeListener<TableColumn<?,?>>() {
        @Override public void onChanged(Change<? extends TableColumn<?,?>> c) {
            updateColumnIndex();
        }
    };
    
    private WeakInvalidationListener weakVisibleListener = 
            new WeakInvalidationListener(visibleListener);
    private WeakListChangeListener<TableColumn<?,?>> weakSortOrderListener =
            new WeakListChangeListener<TableColumn<?,?>>(sortOrderListener);
    private WeakInvalidationListener weakWidthListener = 
            new WeakInvalidationListener(widthListener);
    private WeakInvalidationListener weakSortTypeListener = 
            new WeakInvalidationListener(sortTypeListener);
    private final WeakListChangeListener weakVisibleLeafColumnsListener =
            new WeakListChangeListener(visibleLeafColumnsListener);
    
    
    
    /***************************************************************************
     *                                                                         *
     * Internal Fields                                                         *
     *                                                                         *
     **************************************************************************/    
    
    private double dragOffset;

    private final TableView table;
    protected TableView getTableView() { return table; }

    @Styleable(property="-fx-size", initial="20")
    private double size;

    private NestedTableColumnHeader nestedColumnHeader;
    NestedTableColumnHeader getNestedColumnHeader() { return nestedColumnHeader; }
    void setNestedColumnHeader(NestedTableColumnHeader nch) { nestedColumnHeader = nch; }

    private final TableColumn column;
    public TableColumn getTableColumn() { return column; }

    private TableHeaderRow tableHeaderRow;
    TableHeaderRow getTableHeaderRow() { return tableHeaderRow; }
    void setTableHeaderRow(TableHeaderRow thr) { tableHeaderRow = thr; }

    private NestedTableColumnHeader parentHeader;
    NestedTableColumnHeader getParentHeader() { return parentHeader; }
    void setParentHeader(NestedTableColumnHeader ph) { parentHeader = ph; }

    // work out where this column currently is within its parent
    private Label label;

    // sort order 
    private int sortPos;
    private StackPane arrow;
    private Label sortOrderLabel;
    private HBox sortOrderDots;
    private GridPane sortArrowGrid;
    private boolean isSortColumn;
    
    private boolean isSizeDirty = false;
    
    boolean isLastVisibleColumn = false;
    private int columnIndex = -1;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/   
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/       

    private void initUI() {
        sortArrowGrid = new GridPane();
        sortArrowGrid.setPadding(new Insets(0, 3, 0, 0));
        
        // TableColumn will be null if we are dealing with the root NestedTableColumnHeader
        if (column == null) return;
        
        // set up mouse events
        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                // pass focus to the table, so that the user immediately sees
                // the focus rectangle around the table control.
                getTableView().requestFocus();

                if (me.isPrimaryButtonDown() && isColumnReorderingEnabled()) {
                    columnReorderingStarted(me);
                }
                me.consume();
            }
        });
        setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                if (me.isPrimaryButtonDown() && isColumnReorderingEnabled()) {
                    columnReordering(me);
                }
                me.consume();
            }
        });
        setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override public void handle(ContextMenuEvent me) {
                 ContextMenu menu = getTableColumn().getContextMenu();
                 if (menu != null) {
                     menu.show(TableColumnHeader.this, me.getScreenX(), me.getScreenY());
                     me.consume();
                }
            }
        });
        setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                if (MouseEvent.impl_getPopupTrigger(me)) return;
                ContextMenu menu = getTableColumn().getContextMenu();
                if (menu != null && menu.isShowing()) return;
                if (getTableHeaderRow().isReordering() && isColumnReorderingEnabled()) {
                    columnReorderingComplete(me);
                } else {
                    sortColumn(getTableColumn(), me.isShiftDown());
                }
                me.consume();
            }
        });

        // --- label
        label = new Label();
        label.setAlignment(Pos.CENTER);
        label.textProperty().bind(column.textProperty());

        // ---- container for the sort arrow
        arrow = new StackPane();
        arrow.getStyleClass().setAll("arrow");
        arrow.setVisible(true);
        arrow.setRotate(column.getSortType() == ASCENDING ? 180.0F : 0.0F);
        column.sortTypeProperty().addListener(weakSortTypeListener);

        // put together the grid
        updateSortGrid();
    }
    
    private void setSortPos(int sortPos) {
        this.sortPos = sortPos;
        updateSortGrid();
    }
    
    private void updateSortGrid() {
        // Fixe for RT-14488
        if (this instanceof NestedTableColumnHeader) return;
        
        isSortColumn = sortPos != -1;
        
        getChildren().clear();
        getChildren().add(label);
        
        sortArrowGrid.getChildren().clear();
        
        if (! isSortColumn) return;
        
        final int sortColumnCount = getTableView().getSortOrder().size();
        boolean showSortOrderDots = sortPos <= 3 && sortColumnCount > 1;
        
        arrow.setVisible(isSortColumn);
        sortArrowGrid.setVisible(isSortColumn);
        getChildren().add(sortArrowGrid);
        
        if (sortPos > 2) {
            if (sortOrderLabel == null) {
                // ---- sort order label (for sort positions greater than 3)
                sortOrderLabel = new Label();
                sortOrderLabel.getStyleClass().add("sort-order");
            }
            
            // only show the label if the sortPos is greater than 3 (for sortPos 
            // values less than three, we show the sortOrderDots instead)
            sortOrderLabel.setText("" + (sortPos + 1));
            sortOrderLabel.setVisible(sortColumnCount > 1);
            
            // update the grid layout
            sortArrowGrid.add(arrow, 1, 1);
            GridPane.setHgrow(arrow, Priority.NEVER);
            GridPane.setVgrow(arrow, Priority.NEVER);
            sortArrowGrid.add(sortOrderLabel, 2, 1);
        } else if (showSortOrderDots) {
            if (sortOrderDots == null) {
                sortOrderDots = new HBox(1);
            }
            
            // show the sort order dots
            int arrowRow = column.getSortType() == ASCENDING ? 1 : 2;
            int dotsRow = column.getSortType() == ASCENDING ? 2 : 1;
            
            sortArrowGrid.add(arrow, 1, arrowRow);
            GridPane.setHalignment(arrow, HPos.CENTER);
            sortArrowGrid.add(sortOrderDots, 1, dotsRow);
            
            updateSortOrderDots(sortPos);
        } else {
            // only show the arrow
            sortArrowGrid.add(arrow, 1, 1);
            GridPane.setHgrow(arrow, Priority.NEVER);
            GridPane.setVgrow(arrow, Priority.ALWAYS);
        }
        
        requestLayout();
    }
    
    private void updateSortOrderDots(int sortPos) {
        double arrowWidth = arrow.prefWidth(-1);
        
        // This is a bit of a hack - we're forcing the arrow to have its CSS 
        // processed so that it has its bounds calculated
        if (arrowWidth == 0.0) {
            arrow.impl_processCSS(true);
            arrowWidth = arrow.prefWidth(-1);
        }
        
        sortOrderDots.getChildren().clear();
        
        double rectWidth = (arrowWidth) / 3.0;
        double rectHeight = rectWidth * 0.618033987;
        
        for (int i = 0; i <= sortPos; i++) {
            Rectangle r = new Rectangle(rectWidth, rectHeight, Color.BLACK);
            sortOrderDots.getChildren().add(r);
        }
        
        sortOrderDots.setPadding(new Insets(1, 0, 1, 0));
        sortOrderDots.setAlignment(Pos.TOP_CENTER);
        sortOrderDots.setMaxHeight(rectHeight + 2);
        sortOrderDots.setMaxWidth(arrowWidth);
    }
    
    private void moveColumn(TableColumn column, int newColumnPos) {
        if (column == null || newColumnPos < 0) return;

        ObservableList<TableColumn> columns = column.getParentColumn() == null ?
            getTableView().getColumns() :
            column.getParentColumn().getColumns();
        
        int currentPos = columns.indexOf(column);
        if (newColumnPos == currentPos) return;

        if (newColumnPos >= columns.size()) {
            newColumnPos = columns.size() - 1;
        }

        List<TableColumn> tempList = new ArrayList<TableColumn>(columns);
        tempList.remove(column);
        tempList.add(newColumnPos, column);
        
        columns.setAll(tempList);
    }
    
    private void updateColumnIndex() {
        TableView tv = getTableView();
        TableColumn tc = getTableColumn();
        columnIndex = tv == null || tc == null ? -1 : tv.getVisibleLeafIndex(tc);
        
        // update the pseudo class state regarding whether this is the last
        // visible cell (i.e. the right-most). 
        boolean old = isLastVisibleColumn;
        isLastVisibleColumn = getTableColumn() != null &&
                columnIndex != -1 && 
                columnIndex == getTableView().getVisibleLeafColumns().size() - 1;
        if (old != isLastVisibleColumn) {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_LAST_VISIBLE);
        }
    }

    private void sortColumn(TableColumn column, boolean addColumn) {
        // we only allow sorting on the leaf columns and columns
        // that actually have comparators defined, and are sortable
        if (column == null || column.getColumns().size() != 0 || column.getComparator() == null || !column.isSortable()) return;
//        final int sortPos = getTable().getSortOrder().indexOf(column);
//        final boolean isSortColumn = sortPos != -1;

        // addColumn is true e.g. when the user is holding down Shift
        if (addColumn) {
            if (!isSortColumn) {
                column.setSortType(ASCENDING);
                getTableView().getSortOrder().add(column);
            } else if (column.getSortType() == ASCENDING) {
                column.setSortType(DESCENDING);
            } else {
                int i = getTableView().getSortOrder().indexOf(column);
                if (i != -1) {
                    getTableView().getSortOrder().remove(i);
                }
            }
        } else {
            // the user has clicked on a column header - we should add this to
            // the TableView sortOrder list if it isn't already there.
            if (isSortColumn && getTableView().getSortOrder().size() == 1) {
                // the column is already being sorted, and it's the only column.
                // We therefore move through the 2nd or 3rd states:
                //   1st click: sort ascending
                //   2nd click: sort descending
                //   3rd click: natural sorting (sorting is switched off) 
                if (column.getSortType() == ASCENDING) {
                    column.setSortType(DESCENDING);
                } else {
                    // remove from sort
                    getTableView().getSortOrder().remove(column);
                }
            } else if (isSortColumn) {
                // the column is already being used to sort, so we toggle its
                // sortAscending property, and also make the column become the
                // primary sort column
                switch (column.getSortType()) {
                    case ASCENDING: column.setSortType(DESCENDING); break;
                    case DESCENDING: column.setSortType(ASCENDING); break;
                }
                
                // to prevent multiple sorts, we make a copy of the sort order
                // list, moving the column value from the current position to 
                // its new position at the front of the list
                List<TableColumn> sortOrder = new ArrayList<TableColumn>(getTableView().getSortOrder());
                sortOrder.remove(column);
                sortOrder.add(0, column);
                getTableView().getSortOrder().setAll(column);
            } else {
                // add to the sort order, in ascending form
                column.setSortType(ASCENDING);
                getTableView().getSortOrder().setAll(column);
            }
        }
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/     

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (isSizeDirty) {
            resize(getTableColumn().getWidth(), getHeight());
            isSizeDirty = false;
        }
        
        double sortWidth = 0;
        double w = getWidth() - getInsets().getLeft() + getInsets().getRight();
        double h = getHeight() - getInsets().getTop() + getInsets().getBottom();
        double x = w;
        
        // a bit hacky, but we REALLY don't want the arrow shape to fluctuate 
        // in size
        if (arrow != null) {
            arrow.setMaxSize(arrow.prefWidth(-1), arrow.prefHeight(-1));
        }

        if (sortArrowGrid.isVisible()) {
            sortWidth = sortArrowGrid.prefWidth(-1);
            x -= sortWidth;
            sortArrowGrid.resize(sortWidth, sortArrowGrid.prefHeight(-1));
            positionInArea(sortArrowGrid, x, getInsets().getTop(), 
                    sortWidth, h, 0, HPos.CENTER, VPos.CENTER);
        }

        if (label != null) {
            double labelWidth = w - sortWidth;
            label.resizeRelocate(getInsets().getLeft(), 0, labelWidth, getHeight());
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        if (getNestedColumnHeader() != null) {
            double width = getNestedColumnHeader().prefWidth(height);

            if (column != null) {
                column.impl_setWidth(width);
            }

            return width;
        } else if (column != null && column.isVisible()) {
            return column.getWidth();
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width) {
        return label == null ? 0 : label.minHeight(width);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        if (getTableColumn() == null) return 0;
        return Math.max(size, label.prefHeight(-1));
    }

    
    
    /***************************************************************************
     *                                                                         *
     * Column Reordering                                                       *
     *                                                                         *
     **************************************************************************/   
    private int newColumnPos;
    
    private boolean isColumnReorderingEnabled() {
        // we only allow for column reordering if there are more than one column
        return getTableView().getVisibleLeafColumns().size() > 1;
    }

    private void columnReorderingStarted(MouseEvent me) {
        // Used to ensure the column ghost is positioned relative to where the
        // user clicked on the column header
        dragOffset = me.getX();

        // Note here that we only allow for reordering of 'root' columns
        getTableHeaderRow().setReorderingColumn(column);
        getTableHeaderRow().setReorderingRegion(this);
    }

    private void columnReordering(MouseEvent me) {
        // this is for handling the column drag to reorder columns.
        // It shows a line to indicate where the 'drop' will be.

        // indicate that we've started dragging so that the dragging
        // line overlay is shown
        getTableHeaderRow().setReordering(true);

        // Firstly we need to determine where to draw the line.
        // Find which column we're over
        TableColumnHeader hoverHeader = null;

        // x represents where the mouse is relative to the parent
        // NestedTableColumnHeader
        final double x = getParentHeader().sceneToLocal(me.getSceneX(), me.getSceneY()).getX();

        // calculate where the ghost column header should be
        double dragX = table.sceneToLocal(me.getSceneX(), me.getSceneY()).getX() - dragOffset;
        getTableHeaderRow().setDragHeaderX(dragX);

        double startX = 0;
        double endX = 0;
        double headersWidth = 0;
        newColumnPos = 0;
        for (TableColumnHeader header : getParentHeader().getColumnHeaders()) {
            double headerWidth = header.prefWidth(-1);
            headersWidth += headerWidth;

            startX = header.getBoundsInParent().getMinX();
            endX = startX + headerWidth;

            if (x >= startX && x < endX) {
                hoverHeader = header;
                break;
            }
            newColumnPos++;
        }

        // hoverHeader will be null if the drag occurs outside of the
        // tableview. In this case we handle the newColumnPos specially
        // and then short-circuit. This results in the drop action
        // resulting in the correct result (the column will drop at
        // the start or end of the table).
        if (hoverHeader == null) {
            newColumnPos = x > headersWidth ? (getParentHeader().getColumns().size() - 1) : 0;
            return;
        }

        // This is the x-axis value midway through hoverHeader. It's
        // used to determine whether the drop should be to the left
        // or the right of hoverHeader.
        double midPoint = startX + (endX - startX) / 2;
        boolean beforeMidPoint = x <= midPoint;

        // Based on where the mouse actually is, we have to shuffle
        // where we want the column to end up. This code handles that.
        int currentPos = getTableView().getVisibleLeafIndex(getTableColumn());
        newColumnPos += newColumnPos > currentPos && beforeMidPoint ?
            -1 : (newColumnPos < currentPos && !beforeMidPoint ? 1 : 0);
        
        double lineX = getTableHeaderRow().sceneToLocal(hoverHeader.localToScene(hoverHeader.getBoundsInLocal())).getMinX();
        lineX = lineX + ((beforeMidPoint) ? (0) : (hoverHeader.getWidth()));
        
        if (lineX >= -0.5 && lineX <= table.getWidth()) {
            getTableHeaderRow().getColumnReorderLine().setTranslateX(lineX);

            // then if this is the first event, we set the property to true
            // so that the line becomes visible until the drop is completed.
            // We also set reordering to true so that the various reordering
            // effects become visible (ghost, transparent overlay, etc).
            getTableHeaderRow().getColumnReorderLine().setVisible(true);
        }
        
        getTableHeaderRow().setReordering(true);
    }

    protected void columnReorderingComplete(MouseEvent me) {
        // Move col from where it is now to the new position.
        moveColumn(getTableColumn(), newColumnPos);

        // cleanup
        getTableHeaderRow().getColumnReorderLine().setTranslateX(0.0F);
        getTableHeaderRow().getColumnReorderLine().setLayoutX(0.0F);
        newColumnPos = 0;

        getTableHeaderRow().setReordering(false);
        getTableHeaderRow().getColumnReorderLine().setVisible(false);
        getTableHeaderRow().setReorderingColumn(null);
        getTableHeaderRow().setReorderingRegion(null);
        dragOffset = 0.0F;
    }

    

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/
    
    private static final String PSEUDO_CLASS_LAST_VISIBLE = "last-visible";

    /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatasprivate implementation detail
      */
     private static class StyleableProperties {
         private static final StyleableProperty SIZE =
            new StyleableProperty(TableColumnHeader.class, "size");

         private static final List<StyleableProperty> STYLEABLES;
         static {

            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(StackPane.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                SIZE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    };

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-size".equals(property)) {
            final double newSize = (Double) value;
            // guard against a 0 or negative size
            this.size = (newSize <= 0) ? (20.0F) : (newSize);
        } else {
            return super.impl_cssSet(property, value);
        }
        return true;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_cssSettable(String property) {
        return property.equals("-fx-size") ? true : super.impl_cssSettable(property);
    }
}
