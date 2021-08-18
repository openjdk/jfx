/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * <p>This class is used to construct the header of a TableView. We take the approach
 * that every TableView header is nested - even if it isn't. This allows for us
 * to use the same code for building a single row of TableColumns as we would
 * with a heavily nested sequences of TableColumns. Because of this, the
 * TableHeaderRow class consists of just one instance of a NestedTableColumnHeader.
 *
 * @since 9
 * @see TableColumnHeader
 * @see TableHeaderRow
 * @see TableColumnBase
 */
public class NestedTableColumnHeader extends TableColumnHeader {

    /* *************************************************************************
     *                                                                         *
     * Static Fields                                                           *
     *                                                                         *
     **************************************************************************/

    static final String DEFAULT_STYLE_CLASS = "nested-column-header";

    private static final int DRAG_RECT_WIDTH = 4;

    private static final String TABLE_COLUMN_KEY = "TableColumn";
    private static final String TABLE_COLUMN_HEADER_KEY = "TableColumnHeader";



    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Represents the actual columns directly contained in this nested column.
     * It does NOT include ANY of the children of these columns, if any exist.
     */
    private ObservableList<? extends TableColumnBase> columns;

    private TableColumnHeader label;

    private ObservableList<TableColumnHeader> columnHeaders;
    private ObservableList<TableColumnHeader> unmodifiableColumnHeaders;

    // used for column resizing
    private double lastX = 0.0F;
    private double dragAnchorX = 0.0;

    // drag rectangle overlays
    private Map<TableColumnBase<?,?>, Rectangle> dragRects = new WeakHashMap<>();

    boolean updateColumns = true;



    /* *************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new NestedTableColumnHeader instance to visually represent the given
     * {@link TableColumnBase} instance.
     *
     * @param tc The table column to be visually represented by this instance.
     */
    public NestedTableColumnHeader(final TableColumnBase tc) {
        super(tc);

        setFocusTraversable(false);

        // init UI
        label = createTableColumnHeader(getTableColumn());
        label.setTableHeaderRow(getTableHeaderRow());
        label.setParentHeader(getParentHeader());
        label.setNestedColumnHeader(this);

        if (getTableColumn() != null) {
            changeListenerHandler.registerChangeListener(getTableColumn().textProperty(), e ->
                    label.setVisible(getTableColumn().getText() != null && ! getTableColumn().getText().isEmpty()));
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private final ListChangeListener<TableColumnBase> columnsListener = c -> {
        setHeadersNeedUpdate();
    };

    private final WeakListChangeListener weakColumnsListener =
            new WeakListChangeListener(columnsListener);

    private static final EventHandler<MouseEvent> rectMousePressed = me -> {
        Rectangle rect = (Rectangle) me.getSource();
        TableColumnBase column = (TableColumnBase) rect.getProperties().get(TABLE_COLUMN_KEY);
        NestedTableColumnHeader header = (NestedTableColumnHeader) rect.getProperties().get(TABLE_COLUMN_HEADER_KEY);

        if (! header.isColumnResizingEnabled()) return;

        // column reordering takes precedence over column resizing, but sometimes the mouse dragged events
        // can be received by both nodes, leading to less than ideal UX, hence the check here.
        if (header.getTableHeaderRow().columnDragLock) return;

        if (me.isConsumed()) return;
        me.consume();

        if (me.getClickCount() == 2 && me.isPrimaryButtonDown()) {
            // the user wants to resize the column such that its
            // width is equal to the widest element in the column
            TableHeaderRow tableHeader = header.getTableHeaderRow();
            TableColumnHeader columnHeader = tableHeader.getColumnHeaderFor(column);
            if (columnHeader != null) {
                columnHeader.resizeColumnToFitContent(-1);
            }
        } else {
            // rather than refer to the rect variable, we just grab
            // it from the source to prevent a small memory leak.
            Rectangle innerRect = (Rectangle) me.getSource();
            double startX = header.getTableHeaderRow().sceneToLocal(innerRect.localToScene(innerRect.getBoundsInLocal())).getMinX() + 2;
            header.dragAnchorX = me.getSceneX();
            header.columnResizingStarted(startX);
        }
    };

    private static final EventHandler<MouseEvent> rectMouseDragged = me -> {
        Rectangle rect = (Rectangle) me.getSource();
        TableColumnBase column = (TableColumnBase) rect.getProperties().get(TABLE_COLUMN_KEY);
        NestedTableColumnHeader header = (NestedTableColumnHeader) rect.getProperties().get(TABLE_COLUMN_HEADER_KEY);

        if (! header.isColumnResizingEnabled()) return;

        // column reordering takes precedence over column resizing, but sometimes the mouse dragged events
        // can be received by both nodes, leading to less than ideal UX, hence the check here.
        if (header.getTableHeaderRow().columnDragLock) return;

        if (me.isConsumed()) return;
        me.consume();

        header.columnResizing(column, me);
    };

    private static final EventHandler<MouseEvent> rectMouseReleased = me -> {
        Rectangle rect = (Rectangle) me.getSource();
        TableColumnBase column = (TableColumnBase) rect.getProperties().get(TABLE_COLUMN_KEY);
        NestedTableColumnHeader header = (NestedTableColumnHeader) rect.getProperties().get(TABLE_COLUMN_HEADER_KEY);

        if (! header.isColumnResizingEnabled()) return;

        // column reordering takes precedence over column resizing, but sometimes the mouse dragged events
        // can be received by both nodes, leading to less than ideal UX, hence the check here.
        if (header.getTableHeaderRow().columnDragLock) return;

        if (me.isConsumed()) return;
        me.consume();

        header.columnResizingComplete(column, me);
    };

    private static final EventHandler<MouseEvent> rectCursorChangeListener = me -> {
        Rectangle rect = (Rectangle) me.getSource();
        TableColumnBase column = (TableColumnBase) rect.getProperties().get(TABLE_COLUMN_KEY);
        NestedTableColumnHeader header = (NestedTableColumnHeader) rect.getProperties().get(TABLE_COLUMN_HEADER_KEY);

        // column reordering takes precedence over column resizing, but sometimes the mouse dragged events
        // can be received by both nodes, leading to less than ideal UX, hence the check here.
        if (header.getTableHeaderRow().columnDragLock) return;

        if (header.getCursor() == null) { // If there's a cursor for the whole header, don't override it
            rect.setCursor(header.isColumnResizingEnabled() && rect.isHover() &&
                    column.isResizable() ? Cursor.H_RESIZE : null);
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Public Methods                                                          *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void dispose() {
        super.dispose();

        if (label != null) {
            label.dispose();
        }

        if (getColumns() != null) {
            getColumns().removeListener(weakColumnsListener);
        }

        for (int i = 0; i < getColumnHeaders().size(); i++) {
            TableColumnHeader header = getColumnHeaders().get(i);
            header.dispose();
        }

        for (Rectangle rect : dragRects.values()) {
            if (rect != null) {
                rect.visibleProperty().unbind();
            }
        }
        dragRects.clear();
        getChildren().clear();

        changeListenerHandler.dispose();
    }

    /**
     * Returns an unmodifiable list of the {@link TableColumnHeader} instances
     * that are children of this NestedTableColumnHeader.
     * @return the unmodifiable list of TableColumnHeader of this NestedTableColumnHeader
     */
    public final ObservableList<TableColumnHeader> getColumnHeaders() {
        if (columnHeaders == null) {
            columnHeaders = FXCollections.<TableColumnHeader>observableArrayList();
            unmodifiableColumnHeaders = FXCollections.unmodifiableObservableList(columnHeaders);
        }
        return unmodifiableColumnHeaders;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        double w = getWidth() - snappedLeftInset() - snappedRightInset();
        double h = getHeight() - snappedTopInset() - snappedBottomInset();

        int labelHeight = 0;

        if (label.isVisible() && getTableColumn() != null) {
            labelHeight = (int) label.prefHeight(-1);
            // label gets to span whole width and sits at top
            label.resize(w, labelHeight);
            label.relocate(snappedLeftInset(), snappedTopInset());
        }

        // children columns need to share the total available width
        double x = snappedLeftInset();
        final double height = snapSizeY(h - labelHeight);
        for (int i = 0, max = getColumnHeaders().size(); i < max; i++) {
            TableColumnHeader n = getColumnHeaders().get(i);
            if (! n.isVisible()) continue;

            double prefWidth = n.prefWidth(height);

            // position the column header in the default location...
            n.resize(prefWidth, height);
            n.relocate(x, labelHeight + snappedTopInset());

//            // ...but, if there are no children of this column, we should ensure
//            // that it is resized vertically such that it goes to the very
//            // bottom of the table header row.
//            if (getTableHeaderRow() != null && n.getCol().getColumns().isEmpty()) {
//                Bounds bounds = getTableHeaderRow().sceneToLocal(n.localToScene(n.getBoundsInLocal()));
//                prefHeight = getTableHeaderRow().getHeight() - bounds.getMinY();
//                n.resize(prefWidth, prefHeight);
//            }

            // shuffle along the x-axis appropriately
            x += prefWidth;

            // position drag overlay to intercept column resize requests
            Rectangle dragRect = dragRects.get(n.getTableColumn());
            if (dragRect != null) {
                dragRect.setHeight(n.getDragRectHeight());
                dragRect.relocate(x - DRAG_RECT_WIDTH / 2, snappedTopInset() + labelHeight);
            }
        }
    }

    // sum up all children columns
    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        checkState();

        double width = 0.0F;

        if (getColumns() != null) {
            for (TableColumnHeader c : getColumnHeaders()) {
                if (c.isVisible()) {
                    width += c.computePrefWidth(height);
                }
            }
        }

        return width;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        checkState();

        double height = 0.0F;

        if (getColumnHeaders() != null) {
            for (TableColumnHeader n : getColumnHeaders()) {
                height = Math.max(height, n.prefHeight(-1));
            }
        }

        double labelHeight = 0.0;
        if (label.isVisible() && getTableColumn() != null) {
            labelHeight = label.prefHeight(-1);
        }

        return height + labelHeight + snappedTopInset() + snappedBottomInset();
    }

    /**
     * Creates a new TableColumnHeader instance for the given TableColumnBase instance. The general pattern for
     * implementing this method is as follows:
     *
     * <ul>
     *     <li>If the given TableColumnBase instance is null, has no child columns, or if the given TableColumnBase
     *         instance equals the TableColumnBase instance returned by calling {@link #getTableColumn()}, then it is
     *         suggested to return a {@link TableColumnHeader} instance comprised of the given column.</li>
     *     <li>Otherwise, we can presume that the given TableColumnBase instance has child columns, and in this case
     *         it is suggested to return a {@link NestedTableColumnHeader} instance instead.</li>
     * </ul>
     *
     * <strong>Note: </strong>In most circumstances this method should not be overridden, but in some circumstances it
     * makes sense (e.g. testing, or when extreme customization is desired).
     *
     * @param col the table column
     * @return A new TableColumnHeader instance.
     */
    protected TableColumnHeader createTableColumnHeader(TableColumnBase col) {
        return col == null || col.getColumns().isEmpty() || col == getTableColumn() ?
                new TableColumnHeader(col) :
                new NestedTableColumnHeader(col);
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    @Override void initStyleClasses() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        installTableColumnStyleClassListener();
    }

    @Override void setTableHeaderRow(TableHeaderRow header) {
        super.setTableHeaderRow(header);

        // it's only now that a skin might be available
        if (getTableSkin() != null) {
            changeListenerHandler.registerChangeListener(TableSkinUtils.columnResizePolicyProperty(getTableSkin()), e -> updateContent());
        }

        label.setTableHeaderRow(header);

        // tell all children columns what TableHeader they belong to
        for (TableColumnHeader c : getColumnHeaders()) {
            c.setTableHeaderRow(header);
        }
    }

    @Override void setParentHeader(NestedTableColumnHeader parentHeader) {
        super.setParentHeader(parentHeader);
        label.setParentHeader(parentHeader);
    }

    ObservableList<? extends TableColumnBase> getColumns() {
        return columns;
    }

    void setColumns(ObservableList<? extends TableColumnBase> newColumns) {
        if (this.columns != null) {
            this.columns.removeListener(weakColumnsListener);
        }

        this.columns = newColumns;

        if (this.columns != null) {
            this.columns.addListener(weakColumnsListener);
        }
    }

    void updateTableColumnHeaders() {
        // watching for changes to the view columns in either table or tableColumn.
        if (getTableColumn() == null && getTableSkin() != null) {
            setColumns(TableSkinUtils.getColumns(getTableSkin()));
        } else if (getTableColumn() != null) {
            setColumns(getTableColumn().getColumns());
        }

        // update the column headers...

        // iterate through all columns, unless we've got no child columns
        // any longer, in which case we should switch to a TableColumnHeader
        // instead
        if (getColumns().isEmpty()) {
            // iterate through all current headers, telling them to clean up
            for (int i = 0; i < getColumnHeaders().size(); i++) {
                TableColumnHeader header = getColumnHeaders().get(i);
                header.dispose();
            }

            // switch out to be a TableColumn instead, if we have a parent header
            NestedTableColumnHeader parentHeader = getParentHeader();
            if (parentHeader != null) {
                List<TableColumnHeader> parentColumnHeaders = parentHeader.getColumnHeaders();
                int index = parentColumnHeaders.indexOf(this);
                if (index >= 0 && index < parentColumnHeaders.size()) {
                    parentColumnHeaders.set(index, createColumnHeader(getTableColumn()));
                }
            } else {
                // otherwise just remove all the columns
                columnHeaders.clear();
            }
        } else {
            List<TableColumnHeader> oldHeaders = new ArrayList<>(getColumnHeaders());
            List<TableColumnHeader> newHeaders = new ArrayList<>();

            for (int i = 0; i < getColumns().size(); i++) {
                TableColumnBase<?,?> column = getColumns().get(i);
                if (column == null || ! column.isVisible()) continue;

                // check if the header already exists and reuse it
                boolean found = false;
                for (int j = 0; j < oldHeaders.size(); j++) {
                    TableColumnHeader oldColumn = oldHeaders.get(j);
                    if (oldColumn.represents(column)) {
                        newHeaders.add(oldColumn);
                        found = true;
                        break;
                    }
                }

                // otherwise create a new table column header
                if (!found) {
                    newHeaders.add(createColumnHeader(column));
                }
            }

            columnHeaders.setAll(newHeaders);

            // dispose all old headers
            oldHeaders.removeAll(newHeaders);
            for (int i = 0; i < oldHeaders.size(); i++) {
                oldHeaders.get(i).dispose();
            }
        }

        // update the content
        updateContent();

        // RT-33596: Do CSS now, as we are in the middle of layout pass and the headers are new Nodes w/o CSS done
        for (TableColumnHeader header : getColumnHeaders()) {
            header.applyCss();
        }
    }

    // Used to test whether this column header properly represents the given column.
    // In particular, whether it has child column headers for all child columns
    boolean represents(TableColumnBase<?, ?> column) {
        if (column.getColumns().isEmpty()) {
            // this column has no children, but we are in a NestedTableColumnHeader instance,
            // so the match is bad.
            return false;
        }

        if (column != getTableColumn()) {
            return false;
        }

        final int columnCount = column.getColumns().size();
        final int headerCount = getColumnHeaders().size();
        if (columnCount != headerCount) {
            return false;
        }

        for (int i = 0; i < columnCount; i++) {
            // we expect the order of all children to match the order of the headers
            TableColumnBase<?,?> childColumn = column.getColumns().get(i);
            TableColumnHeader childHeader = getColumnHeaders().get(i);
            if (!childHeader.represents(childColumn)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override double getDragRectHeight() {
        return label.prefHeight(-1);
    }

    void setHeadersNeedUpdate() {
        updateColumns = true;

        // go through children columns - they should update too
        for (int i = 0; i < getColumnHeaders().size(); i++) {
            TableColumnHeader header = getColumnHeaders().get(i);
            if (header instanceof NestedTableColumnHeader) {
                ((NestedTableColumnHeader)header).setHeadersNeedUpdate();
            }
        }
        requestLayout();
    }

    private void updateContent() {
        // create a temporary list so we only do addAll into the main content
        // observableArrayList once.
        final List<Node> content = new ArrayList<Node>();

        // the label is the region that sits above the children columns
        content.add(label);

        // all children columns
        content.addAll(getColumnHeaders());

        // Small transparent overlays that sit at the start and end of each
        // column to intercept user drag gestures to enable column resizing.
        if (isColumnResizingEnabled()) {
            rebuildDragRects();
            content.addAll(dragRects.values());
        }

        getChildren().setAll(content);
    }

    private void rebuildDragRects() {
        if (! isColumnResizingEnabled()) return;

        getChildren().removeAll(dragRects.values());

        for (Rectangle rect : dragRects.values()) {
            rect.visibleProperty().unbind();
        }
        dragRects.clear();

        List<? extends TableColumnBase> columns = getColumns();

        if (columns == null) {
            return;
        }

        boolean isConstrainedResize = false;
        TableViewSkinBase tableSkin = getTableSkin();
        Callback<ResizeFeaturesBase,Boolean> columnResizePolicy = TableSkinUtils.columnResizePolicyProperty(tableSkin).get();
        if (columnResizePolicy != null) {
            isConstrainedResize =
                    tableSkin instanceof TableViewSkin ? TableView.CONSTRAINED_RESIZE_POLICY.equals(columnResizePolicy) :
                    tableSkin instanceof TreeTableViewSkin ? TreeTableView.CONSTRAINED_RESIZE_POLICY.equals(columnResizePolicy) :
                    false;
        }

        // RT-32547 - don't show resize cursor when in constrained resize mode
        // and there is only one column
        if (isConstrainedResize && TableSkinUtils.getVisibleLeafColumns(tableSkin).size() == 1) {
            return;
        }

        for (int col = 0; col < columns.size(); col++) {
            if (isConstrainedResize && col == getColumns().size() - 1) {
                break;
            }

            final TableColumnBase c = columns.get(col);
            final Rectangle rect = new Rectangle();
            rect.getProperties().put(TABLE_COLUMN_KEY, c);
            rect.getProperties().put(TABLE_COLUMN_HEADER_KEY, this);
            rect.setWidth(DRAG_RECT_WIDTH);
            rect.setHeight(getHeight() - label.getHeight());
            rect.setFill(Color.TRANSPARENT);
            rect.visibleProperty().bind(c.visibleProperty().and(c.resizableProperty()));
            rect.setOnMousePressed(rectMousePressed);
            rect.setOnMouseDragged(rectMouseDragged);
            rect.setOnMouseReleased(rectMouseReleased);
            rect.setOnMouseEntered(rectCursorChangeListener);
            rect.setOnMouseExited(rectCursorChangeListener);

            dragRects.put(c, rect);
        }
    }

    private void checkState() {
        if (updateColumns) {
            updateTableColumnHeaders();
            updateColumns = false;
        }
    }

    private TableColumnHeader createColumnHeader(TableColumnBase col) {
        TableColumnHeader newCol = createTableColumnHeader(col);
        newCol.setTableHeaderRow(getTableHeaderRow());
        newCol.setParentHeader(this);
        return newCol;
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation: Column Resizing                                 *
     *                                                                         *
     **************************************************************************/

    private boolean isColumnResizingEnabled() {
        // this used to check if ! PlatformUtil.isEmbedded(), but has been changed
        // to always return true (for now), as we want to support column resizing
        // everywhere
        return true;
    }

    private void columnResizingStarted(double startX) {
        setCursor(Cursor.H_RESIZE);
        columnReorderLine.setLayoutX(startX);
    }

    private void columnResizing(TableColumnBase col, MouseEvent me) {
        double draggedX = me.getSceneX() - dragAnchorX;
        if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
            draggedX = -draggedX;
        }
        double delta = draggedX - lastX;
        boolean allowed = TableSkinUtils.resizeColumn(getTableSkin(), col, delta);
        if (allowed) {
            lastX = draggedX;
        }
    }

    private void columnResizingComplete(TableColumnBase col, MouseEvent me) {
        setCursor(null);
        columnReorderLine.setTranslateX(0.0F);
        columnReorderLine.setLayoutX(0.0F);
        lastX = 0.0F;
    }
}
