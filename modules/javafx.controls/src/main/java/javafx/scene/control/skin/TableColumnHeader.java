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

import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.TableColumnBaseHelper;
import com.sun.javafx.scene.control.TreeTableViewBackingList;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.sun.javafx.scene.control.TableColumnSortTypeWrapper.getSortTypeName;
import static com.sun.javafx.scene.control.TableColumnSortTypeWrapper.getSortTypeProperty;
import static com.sun.javafx.scene.control.TableColumnSortTypeWrapper.isAscending;
import static com.sun.javafx.scene.control.TableColumnSortTypeWrapper.isDescending;
import static com.sun.javafx.scene.control.TableColumnSortTypeWrapper.setSortType;


/**
 * Region responsible for painting a single column header. A subcomponent used by
 * subclasses of {@link TableViewSkinBase}.
 *
 * @since 9
 */
public class TableColumnHeader extends Region {

    /***************************************************************************
     *                                                                         *
     * Static Fields                                                           *
     *                                                                         *
     **************************************************************************/

    static final String DEFAULT_STYLE_CLASS = "column-header";

    // Copied from TableColumn. The value here should always be in-sync with
    // the value in TableColumn
    static final double DEFAULT_COLUMN_WIDTH = 80.0F;



    /***************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private boolean autoSizeComplete = false;

    private double dragOffset;
    private NestedTableColumnHeader nestedColumnHeader;
    private TableHeaderRow tableHeaderRow;
    private NestedTableColumnHeader parentHeader;

    // work out where this column currently is within its parent
    Label label;

    // sort order
    int sortPos = -1;
    private Region arrow;
    private Label sortOrderLabel;
    private HBox sortOrderDots;
    private Node sortArrow;
    private boolean isSortColumn;

    private boolean isSizeDirty = false;

    boolean isLastVisibleColumn = false;

    // package for testing
    int columnIndex = -1;

    private int newColumnPos;

    // the line drawn in the table when a user presses and moves a column header
    // to indicate where the column will be dropped. This is provided by the
    // table skin, but manipulated by the header
    Region columnReorderLine;



    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TableColumnHeader instance to visually represent the given
     * {@link TableColumnBase} instance.
     *
     * @param tc The table column to be visually represented by this instance.
     */
    public TableColumnHeader(final TableColumnBase tc) {
        setTableColumn(tc);
        setFocusTraversable(false);

        initStyleClasses();
        initUI();

        // change listener for multiple properties
        changeListenerHandler = new LambdaMultiplePropertyChangeListenerHandler();
        changeListenerHandler.registerChangeListener(sceneProperty(), e -> updateScene());

        if (getTableColumn() != null) {
            changeListenerHandler.registerChangeListener(tc.idProperty(), e -> setId(tc.getId()));
            changeListenerHandler.registerChangeListener(tc.styleProperty(), e -> setStyle(tc.getStyle()));
            changeListenerHandler.registerChangeListener(tc.widthProperty(), e -> {
                // It is this that ensures that when a column is resized that the header
                // visually adjusts its width as necessary.
                isSizeDirty = true;
                requestLayout();
            });
            changeListenerHandler.registerChangeListener(tc.visibleProperty(), e -> setVisible(getTableColumn().isVisible()));
            changeListenerHandler.registerChangeListener(tc.sortNodeProperty(), e -> updateSortGrid());
            changeListenerHandler.registerChangeListener(tc.sortableProperty(), e -> {
                // we need to notify all headers that a sortable state has changed,
                // in case the sort grid in other columns needs to be updated.
                if (TableSkinUtils.getSortOrder(getTableSkin()).contains(getTableColumn())) {
                    NestedTableColumnHeader root = getTableHeaderRow().getRootHeader();
                    updateAllHeaders(root);
                }
            });
            changeListenerHandler.registerChangeListener(tc.textProperty(), e -> label.setText(tc.getText()));
            changeListenerHandler.registerChangeListener(tc.graphicProperty(), e -> label.setGraphic(tc.getGraphic()));

            setId(tc.getId());
            setStyle(tc.getStyle());
            /* Having TableColumn role parented by TableColumn causes VoiceOver to be unhappy */
            setAccessibleRole(AccessibleRole.TABLE_COLUMN);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    final LambdaMultiplePropertyChangeListenerHandler changeListenerHandler;

    private ListChangeListener<TableColumnBase<?,?>> sortOrderListener = c -> {
        updateSortPosition();
    };

    private ListChangeListener<TableColumnBase<?,?>> visibleLeafColumnsListener = c -> {
        updateColumnIndex();
        updateSortPosition();
    };

    private ListChangeListener<String> styleClassListener = c -> {
        while (c.next()) {
            if (c.wasRemoved()) {
                getStyleClass().removeAll(c.getRemoved());
            }
            if (c.wasAdded()) {
                getStyleClass().addAll(c.getAddedSubList());
            }
        }
    };

    private WeakListChangeListener<TableColumnBase<?,?>> weakSortOrderListener =
            new WeakListChangeListener<TableColumnBase<?,?>>(sortOrderListener);
    private final WeakListChangeListener<TableColumnBase<?,?>> weakVisibleLeafColumnsListener =
            new WeakListChangeListener<TableColumnBase<?,?>>(visibleLeafColumnsListener);
    private final WeakListChangeListener<String> weakStyleClassListener =
            new WeakListChangeListener<String>(styleClassListener);

    private static final EventHandler<MouseEvent> mousePressedHandler = me -> {
        TableColumnHeader header = (TableColumnHeader) me.getSource();
        TableColumnBase tableColumn = header.getTableColumn();

        ContextMenu menu = tableColumn.getContextMenu();
        if (menu != null && menu.isShowing()) {
            menu.hide();
        }

        if (me.isConsumed()) return;
        me.consume();

        header.getTableHeaderRow().columnDragLock = true;

        // pass focus to the table, so that the user immediately sees
        // the focus rectangle around the table control.
        header.getTableSkin().getSkinnable().requestFocus();

        if (me.isPrimaryButtonDown() && header.isColumnReorderingEnabled()) {
            header.columnReorderingStarted(me.getX());
        }
    };

    private static final EventHandler<MouseEvent> mouseDraggedHandler = me -> {
        if (me.isConsumed()) return;
        me.consume();

        TableColumnHeader header = (TableColumnHeader) me.getSource();

        if (me.isPrimaryButtonDown() && header.isColumnReorderingEnabled()) {
            header.columnReordering(me.getSceneX(), me.getSceneY());
        }
    };

    private static final EventHandler<MouseEvent> mouseReleasedHandler = me -> {
        TableColumnHeader header = (TableColumnHeader) me.getSource();
        header.getTableHeaderRow().columnDragLock = false;

        if (me.isPopupTrigger()) return;
        if (me.isConsumed()) return;
        me.consume();

        if (header.getTableHeaderRow().isReordering() && header.isColumnReorderingEnabled()) {
            header.columnReorderingComplete();
        } else if (me.isStillSincePress()) {
            header.sortColumn(me.isShiftDown());
        }
    };

    private static final EventHandler<ContextMenuEvent> contextMenuRequestedHandler = me -> {
        TableColumnHeader header = (TableColumnHeader) me.getSource();
        TableColumnBase tableColumn = header.getTableColumn();

        ContextMenu menu = tableColumn.getContextMenu();
        if (menu != null) {
            menu.show(header, me.getScreenX(), me.getScreenY());
            me.consume();
        }
    };



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- size
    private DoubleProperty size;
    private final double getSize() {
        return size == null ? 20.0 : size.doubleValue();
    }
    private final DoubleProperty sizeProperty() {
        if (size == null) {
            size = new StyleableDoubleProperty(20) {
                @Override
                protected void invalidated() {
                    double value = get();
                    if (value <= 0) {
                        if (isBound()) {
                            unbind();
                        }
                        set(20);
                        throw new IllegalArgumentException("Size cannot be 0 or negative");
                    }
                }



                @Override public Object getBean() {
                    return TableColumnHeader.this;
                }

                @Override public String getName() {
                    return "size";
                }

                @Override public CssMetaData<TableColumnHeader,Number> getCssMetaData() {
                    return StyleableProperties.SIZE;
                }
            };
        }
        return size;
    }


    /**
     * A property that refers to the {@link TableColumnBase} instance that this
     * header is visually represents.
     */
    // --- table column
    private ReadOnlyObjectWrapper<TableColumnBase<?,?>> tableColumn = new ReadOnlyObjectWrapper<>(this, "tableColumn");
    private final void setTableColumn(TableColumnBase<?,?> column) {
        tableColumn.set(column);
    }
    public final TableColumnBase<?,?> getTableColumn() {
        return tableColumn.get();
    }
    public final ReadOnlyObjectProperty<TableColumnBase<?,?>> tableColumnProperty() {
        return tableColumn.getReadOnlyProperty();
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (isSizeDirty) {
            resize(getTableColumn().getWidth(), getHeight());
            isSizeDirty = false;
        }

        double sortWidth = 0;
        double w = snapSizeX(getWidth()) - (snappedLeftInset() + snappedRightInset());
        double h = getHeight() - (snappedTopInset() + snappedBottomInset());
        double x = w;

        // a bit hacky, but we REALLY don't want the arrow shape to fluctuate
        // in size
        if (arrow != null) {
            arrow.setMaxSize(arrow.prefWidth(-1), arrow.prefHeight(-1));
        }

        if (sortArrow != null && sortArrow.isVisible()) {
            sortWidth = sortArrow.prefWidth(-1);
            x -= sortWidth;
            sortArrow.resize(sortWidth, sortArrow.prefHeight(-1));
            positionInArea(sortArrow, x, snappedTopInset(),
                    sortWidth, h, 0, HPos.CENTER, VPos.CENTER);
        }

        if (label != null) {
            double labelWidth = w - sortWidth;
            label.resizeRelocate(snappedLeftInset(), 0, labelWidth, getHeight());
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        if (getNestedColumnHeader() != null) {
            double width = getNestedColumnHeader().prefWidth(height);

            if (getTableColumn() != null) {
                TableColumnBaseHelper.setWidth(getTableColumn(), width);
            }

            return width;
        } else if (getTableColumn() != null && getTableColumn().isVisible()) {
            return snapSizeX(getTableColumn().getWidth());
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
        return Math.max(getSize(), label.prefHeight(-1));
    }

    /** {@inheritDoc} */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /** {@inheritDoc} */
    @Override  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case INDEX: return getIndex(getTableColumn());
            case TEXT: return getTableColumn() != null ? getTableColumn().getText() : null;
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    void initStyleClasses() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        installTableColumnStyleClassListener();
    }

    void installTableColumnStyleClassListener() {
        TableColumnBase tc = getTableColumn();
        if (tc != null) {
            // add in all styleclasses from the table column into the header, and also set up a listener
            // so that any subsequent changes to the table column are also applied to the header
            getStyleClass().addAll(tc.getStyleClass());
            tc.getStyleClass().addListener(weakStyleClassListener);
        }
    }

    NestedTableColumnHeader getNestedColumnHeader() { return nestedColumnHeader; }
    void setNestedColumnHeader(NestedTableColumnHeader nch) { nestedColumnHeader = nch; }

    /**
     * Returns the {@link TableHeaderRow} associated with this {@code TableColumnHeader}.
     *
     * @return the {@code TableHeaderRow} associated with this {@code TableColumnHeader}
     * @since 12
     */
    protected TableHeaderRow getTableHeaderRow() {
        return tableHeaderRow;
    }

    void setTableHeaderRow(TableHeaderRow thr) {
        tableHeaderRow = thr;
        updateTableSkin();
    }

    private void updateTableSkin() {
        // when we get the table header row, we are also given the skin,
        // so this is the time to hook up listeners, etc.
        TableViewSkinBase<?,?,?,?,?> tableSkin = getTableSkin();
        if (tableSkin == null) return;

        updateColumnIndex();
        this.columnReorderLine = tableSkin.getColumnReorderLine();

        if (getTableColumn() != null) {
            updateSortPosition();
            TableSkinUtils.getSortOrder(tableSkin).addListener(weakSortOrderListener);
            TableSkinUtils.getVisibleLeafColumns(tableSkin).addListener(weakVisibleLeafColumnsListener);
        }
    }

    /**
     * Returns the {@code TableViewSkinBase} in which this {@code TableColumnHeader} is inserted. This will return
     * {@code null} until the {@code TableHeaderRow} has been set.
     *
     * @return the {@code TableViewSkinBase} in which this {@code TableColumnHeader} is inserted, or {@code null}
     * @since 12
     */
    protected TableViewSkinBase<?, ?, ?, ?, ?> getTableSkin() {
        return tableHeaderRow == null ? null : tableHeaderRow.tableSkin;
    }

    NestedTableColumnHeader getParentHeader() { return parentHeader; }
    void setParentHeader(NestedTableColumnHeader ph) { parentHeader = ph; }

    // RT-29682: When the sortable property of a TableColumnBase changes this
    // may impact other TableColumnHeaders, as they may need to change their
    // sort order representation. Rather than install listeners across all
    // TableColumn in the sortOrder list for their sortable property, we simply
    // update the sortPosition of all headers whenever the sortOrder property
    // changes, assuming the column is within the sortOrder list.
    private void updateAllHeaders(TableColumnHeader header) {
        if (header instanceof NestedTableColumnHeader) {
            List<TableColumnHeader> children = ((NestedTableColumnHeader)header).getColumnHeaders();
            for (int i = 0; i < children.size(); i++) {
                updateAllHeaders(children.get(i));
            }
        } else {
            header.updateSortPosition();
        }
    }

    private void updateScene() {
        // RT-17684: If the TableColumn widths are all currently the default,
        // we attempt to 'auto-size' based on the preferred width of the first
        // n rows (we can't do all rows, as that could conceivably be an unlimited
        // number of rows retrieved from a very slow (e.g. remote) data source.
        // Obviously, the bigger the value of n, the more likely the default
        // width will be suitable for most values in the column
        final int n = 30;
        if (! autoSizeComplete) {
            if (getTableColumn() == null || getTableColumn().getWidth() != DEFAULT_COLUMN_WIDTH || getScene() == null) {
                return;
            }
            doColumnAutoSize(n);
            autoSizeComplete = true;
        }
    }

    void dispose() {
        TableViewSkinBase tableSkin = getTableSkin();
        if (tableSkin != null) {
            TableSkinUtils.getVisibleLeafColumns(tableSkin).removeListener(weakVisibleLeafColumnsListener);
            TableSkinUtils.getSortOrder(tableSkin).removeListener(weakSortOrderListener);
        }

        changeListenerHandler.dispose();
    }

    private boolean isSortingEnabled() {
        // this used to check if ! PlatformUtil.isEmbedded(), but has been changed
        // to always return true (for now), as we want to support column sorting
        // everywhere
        return true;
    }

    private boolean isColumnReorderingEnabled() {
        // we only allow for column reordering if there are more than one column,
        return !Properties.IS_TOUCH_SUPPORTED && TableSkinUtils.getVisibleLeafColumns(getTableSkin()).size() > 1;
    }

    private void initUI() {
        // TableColumn will be null if we are dealing with the root NestedTableColumnHeader
        if (getTableColumn() == null) return;

        // set up mouse events
        setOnMousePressed(mousePressedHandler);
        setOnMouseDragged(mouseDraggedHandler);
        setOnDragDetected(event -> event.consume());
        setOnContextMenuRequested(contextMenuRequestedHandler);
        setOnMouseReleased(mouseReleasedHandler);

        // --- label
        label = new Label();
        label.setText(getTableColumn().getText());
        label.setGraphic(getTableColumn().getGraphic());
        label.setVisible(getTableColumn().isVisible());

        // ---- container for the sort arrow (which is not supported on embedded
        // platforms)
        if (isSortingEnabled()) {
            // put together the grid
            updateSortGrid();
        }
    }

    private void doColumnAutoSize(int cellsToMeasure) {
        double prefWidth = getTableColumn().getPrefWidth();

        // if the prefWidth has been set, we do _not_ autosize columns
        if (prefWidth == DEFAULT_COLUMN_WIDTH) {
            resizeColumnToFitContent(cellsToMeasure);
        }
    }

    /**
     * Resizes this {@code TableColumnHeader}'s column to fit the width of its content.
     *
     * @implSpec The resulting column width for this implementation is the maximum of the preferred width of the header
     * cell and the preferred width of the first {@code maxRow} cells.
     * <p>
     * Subclasses can either use this method or override it (without the need to call {@code super()}) to provide their
     * custom implementation (such as ones that exclude the header, exclude {@code null} content, compute the minimum
     * width, etc.).
     *
     * @param maxRows the number of rows considered when resizing. If -1 is given, all rows are considered.
     * @since 14
     */
    protected void resizeColumnToFitContent(int maxRows) {
        TableColumnBase<?, ?> tc = getTableColumn();
        if (!tc.isResizable()) return;

        Object control = this.getTableSkin().getSkinnable();
        if (control instanceof TableView) {
            resizeColumnToFitContent((TableView) control, (TableColumn) tc, this.getTableSkin(), maxRows);
        } else if (control instanceof TreeTableView) {
            resizeColumnToFitContent((TreeTableView) control, (TreeTableColumn) tc, this.getTableSkin(), maxRows);
        }
    }

    private <T,S> void resizeColumnToFitContent(TableView<T> tv, TableColumn<T, S> tc, TableViewSkinBase tableSkin, int maxRows) {
        List<?> items = tv.getItems();
        if (items == null || items.isEmpty()) return;

        Callback/*<TableColumn<T, ?>, TableCell<T,?>>*/ cellFactory = tc.getCellFactory();
        if (cellFactory == null) return;

        TableCell<T,?> cell = (TableCell<T, ?>) cellFactory.call(tc);
        if (cell == null) return;

        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(Properties.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);

        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        }

        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            cell.updateTableColumn(tc);
            cell.updateTableView(tv);
            cell.updateIndex(row);

            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                tableSkin.getChildren().add(cell);
                cell.applyCss();
                maxWidth = Math.max(maxWidth, cell.prefWidth(-1));
                tableSkin.getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = tableSkin.getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);

        // RT-23486
        maxWidth += padding;
        if (tv.getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY && tv.getWidth() > 0) {
            if (maxWidth > tc.getMaxWidth()) {
                maxWidth = tc.getMaxWidth();
            }

            int size = tc.getColumns().size();
            if (size > 0) {
                TableColumnHeader columnHeader = getTableHeaderRow().getColumnHeaderFor(tc.getColumns().get(size - 1));
                if (columnHeader != null) {
                    columnHeader.resizeColumnToFitContent(maxRows);
                }
                return;
            }

            TableSkinUtils.resizeColumn(tableSkin, tc, Math.round(maxWidth - tc.getWidth()));
        } else {
            TableColumnBaseHelper.setWidth(tc, maxWidth);
        }
    }

    private <T,S> void resizeColumnToFitContent(TreeTableView<T> ttv, TreeTableColumn<T, S> tc, TableViewSkinBase tableSkin, int maxRows) {
        List<?> items = new TreeTableViewBackingList(ttv);
        if (items == null || items.isEmpty()) return;

        Callback cellFactory = tc.getCellFactory();
        if (cellFactory == null) return;

        TreeTableCell<T,S> cell = (TreeTableCell) cellFactory.call(tc);
        if (cell == null) return;

        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(Properties.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);

        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        }

        TreeTableRow<T> treeTableRow = new TreeTableRow<>();
        treeTableRow.updateTreeTableView(ttv);

        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            treeTableRow.updateIndex(row);
            treeTableRow.updateTreeItem(ttv.getTreeItem(row));

            cell.updateTreeTableColumn(tc);
            cell.updateTreeTableView(ttv);
            cell.updateTreeTableRow(treeTableRow);
            cell.updateIndex(row);

            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                tableSkin.getChildren().add(cell);
                cell.applyCss();

                double w = cell.prefWidth(-1);

                maxWidth = Math.max(maxWidth, w);
                tableSkin.getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = tableSkin.getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);

        // RT-23486
        maxWidth += padding;
        if (ttv.getColumnResizePolicy() == TreeTableView.CONSTRAINED_RESIZE_POLICY && ttv.getWidth() > 0) {

            if (maxWidth > tc.getMaxWidth()) {
                maxWidth = tc.getMaxWidth();
            }

            int size = tc.getColumns().size();
            if (size > 0) {
                TableColumnHeader columnHeader = getTableHeaderRow().getColumnHeaderFor(tc.getColumns().get(size - 1));
                if (columnHeader != null) {
                    columnHeader.resizeColumnToFitContent(maxRows);
                }
                return;
            }

            TableSkinUtils.resizeColumn(tableSkin, tc, Math.round(maxWidth - tc.getWidth()));
        } else {
            TableColumnBaseHelper.setWidth(tc, maxWidth);
        }
    }

    private void updateSortPosition() {
        this.sortPos = ! getTableColumn().isSortable() ? -1 : getSortPosition();
        updateSortGrid();
    }

    private void updateSortGrid() {
        // Fix for RT-14488
        if (this instanceof NestedTableColumnHeader) return;

        getChildren().clear();
        getChildren().add(label);

        // we do not support sorting in embedded devices
        if (! isSortingEnabled()) return;

        isSortColumn = sortPos != -1;
        if (! isSortColumn) {
            if (sortArrow != null) {
                sortArrow.setVisible(false);
            }
            return;
        }

        // RT-28016: if the tablecolumn is not a visible leaf column, we should ignore this
        int visibleLeafIndex = TableSkinUtils.getVisibleLeafIndex(getTableSkin(), getTableColumn());
        if (visibleLeafIndex == -1) return;

        final int sortColumnCount = getVisibleSortOrderColumnCount();
        boolean showSortOrderDots = sortPos <= 3 && sortColumnCount > 1;

        Node _sortArrow = null;
        if (getTableColumn().getSortNode() != null) {
            _sortArrow = getTableColumn().getSortNode();
            getChildren().add(_sortArrow);
        } else {
            GridPane sortArrowGrid = new GridPane();
            _sortArrow = sortArrowGrid;
            sortArrowGrid.setPadding(new Insets(0, 3, 0, 0));
            getChildren().add(sortArrowGrid);

            // if we are here, and the sort arrow is null, we better create it
            if (arrow == null) {
                arrow = new Region();
                arrow.getStyleClass().setAll("arrow");
                arrow.setVisible(true);
                arrow.setRotate(isAscending(getTableColumn()) ? 180.0F : 0.0F);
                changeListenerHandler.registerChangeListener(getSortTypeProperty(getTableColumn()), e -> {
                    updateSortGrid();
                    if (arrow != null) {
                        arrow.setRotate(isAscending(getTableColumn()) ? 180 : 0.0);
                    }
                });
            }

            arrow.setVisible(isSortColumn);

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
                    sortOrderDots = new HBox(0);
                    sortOrderDots.getStyleClass().add("sort-order-dots-container");
                }

                // show the sort order dots
                boolean isAscending = isAscending(getTableColumn());
                int arrowRow = isAscending ? 1 : 2;
                int dotsRow = isAscending ? 2 : 1;

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
        }

        sortArrow = _sortArrow;
        if (sortArrow != null) {
            sortArrow.setVisible(isSortColumn);
        }

        requestLayout();
    }

    private void updateSortOrderDots(int sortPos) {
        double arrowWidth = arrow.prefWidth(-1);

        sortOrderDots.getChildren().clear();

        for (int i = 0; i <= sortPos; i++) {
            Region r = new Region();
            r.getStyleClass().add("sort-order-dot");

            String sortTypeName = getSortTypeName(getTableColumn());
            if (sortTypeName != null && ! sortTypeName.isEmpty()) {
                r.getStyleClass().add(sortTypeName.toLowerCase(Locale.ROOT));
            }

            sortOrderDots.getChildren().add(r);

            // RT-34914: fine tuning the placement of the sort dots. We could have gone to a custom layout, but for now
            // this works fine.
            if (i < sortPos) {
                Region spacer = new Region();
                double lp = sortPos == 1 ? 1 : 0;
                spacer.setPadding(new Insets(0, 1, 0, lp));
                sortOrderDots.getChildren().add(spacer);
            }
        }

        sortOrderDots.setAlignment(Pos.TOP_CENTER);
        sortOrderDots.setMaxWidth(arrowWidth);
    }

    // Package for testing purposes only.
    void moveColumn(TableColumnBase column, final int newColumnPos) {
        if (column == null || newColumnPos < 0) return;

        ObservableList<TableColumnBase<?,?>> columns = getColumns(column);

        final int columnsCount = columns.size();
        final int currentPos = columns.indexOf(column);

        int actualNewColumnPos = newColumnPos;

        // Fix for RT-35141: We need to account for hidden columns.
        // We keep iterating until we see 'requiredVisibleColumns' number of visible columns
        final int requiredVisibleColumns = actualNewColumnPos;
        int visibleColumnsSeen = 0;
        for (int i = 0; i < columnsCount; i++) {
            if (visibleColumnsSeen == (requiredVisibleColumns + 1)) {
                break;
            }

            if (columns.get(i).isVisible()) {
                visibleColumnsSeen++;
            } else {
                actualNewColumnPos++;
            }
        }
        // --- end of RT-35141 fix

        if (actualNewColumnPos >= columnsCount) {
            actualNewColumnPos = columnsCount - 1;
        } else if (actualNewColumnPos < 0) {
            actualNewColumnPos = 0;
        }

        if (actualNewColumnPos == currentPos) return;

        List<TableColumnBase<?,?>> tempList = new ArrayList<>(columns);
        tempList.remove(column);
        tempList.add(actualNewColumnPos, column);

        columns.setAll(tempList);
    }

    private ObservableList<TableColumnBase<?,?>> getColumns(TableColumnBase column) {
        return column.getParentColumn() == null ?
                TableSkinUtils.getColumns(getTableSkin()) :
                column.getParentColumn().getColumns();
    }

    private int getIndex(TableColumnBase<?,?> column) {
        if (column == null) return -1;

        ObservableList<? extends TableColumnBase<?,?>> columns = getColumns(column);

        int index = -1;
        for (int i = 0; i < columns.size(); i++) {
            TableColumnBase<?,?> _column = columns.get(i);
            if (! _column.isVisible()) continue;

            index++;
            if (column.equals(_column)) break;
        }

        return index;
    }

    private void updateColumnIndex() {
//        TableView tv = getTableView();
        TableColumnBase tc = getTableColumn();
        TableViewSkinBase tableSkin = getTableSkin();
        columnIndex = tableSkin == null || tc == null ? -1 :TableSkinUtils.getVisibleLeafIndex(tableSkin,tc);

        // update the pseudo class state regarding whether this is the last
        // visible cell (i.e. the right-most).
        isLastVisibleColumn = getTableColumn() != null &&
                columnIndex != -1 &&
                columnIndex == TableSkinUtils.getVisibleLeafColumns(tableSkin).size() - 1;
        pseudoClassStateChanged(PSEUDO_CLASS_LAST_VISIBLE, isLastVisibleColumn);
    }

    private void sortColumn(final boolean addColumn) {
        if (! isSortingEnabled()) return;

        // we only allow sorting on the leaf columns and columns
        // that actually have comparators defined, and are sortable
        if (getTableColumn() == null || getTableColumn().getColumns().size() != 0 || getTableColumn().getComparator() == null || !getTableColumn().isSortable()) return;
//        final int sortPos = getTable().getSortOrder().indexOf(column);
//        final boolean isSortColumn = sortPos != -1;

        final ObservableList<TableColumnBase<?,?>> sortOrder = TableSkinUtils.getSortOrder(getTableSkin());

        // addColumn is true e.g. when the user is holding down Shift
        if (addColumn) {
            if (!isSortColumn) {
                setSortType(getTableColumn(), TableColumn.SortType.ASCENDING);
                sortOrder.add(getTableColumn());
            } else if (isAscending(getTableColumn())) {
                setSortType(getTableColumn(), TableColumn.SortType.DESCENDING);
            } else {
                int i = sortOrder.indexOf(getTableColumn());
                if (i != -1) {
                    sortOrder.remove(i);
                }
            }
        } else {
            // the user has clicked on a column header - we should add this to
            // the TableView sortOrder list if it isn't already there.
            if (isSortColumn && sortOrder.size() == 1) {
                // the column is already being sorted, and it's the only column.
                // We therefore move through the 2nd or 3rd states:
                //   1st click: sort ascending
                //   2nd click: sort descending
                //   3rd click: natural sorting (sorting is switched off)
                if (isAscending(getTableColumn())) {
                    setSortType(getTableColumn(), TableColumn.SortType.DESCENDING);
                } else {
                    // remove from sort
                    sortOrder.remove(getTableColumn());
                }
            } else if (isSortColumn) {
                // the column is already being used to sort, so we toggle its
                // sortAscending property, and also make the column become the
                // primary sort column
                if (isAscending(getTableColumn())) {
                    setSortType(getTableColumn(), TableColumn.SortType.DESCENDING);
                } else if (isDescending(getTableColumn())) {
                    setSortType(getTableColumn(), TableColumn.SortType.ASCENDING);
                }

                // to prevent multiple sorts, we make a copy of the sort order
                // list, moving the column value from the current position to
                // its new position at the front of the list
                List<TableColumnBase<?,?>> sortOrderCopy = new ArrayList<TableColumnBase<?,?>>(sortOrder);
                sortOrderCopy.remove(getTableColumn());
                sortOrderCopy.add(0, getTableColumn());
                sortOrder.setAll(getTableColumn());
            } else {
                // add to the sort order, in ascending form
                setSortType(getTableColumn(), TableColumn.SortType.ASCENDING);
                sortOrder.setAll(getTableColumn());
            }
        }
    }

    // Because it is possible that some columns are in the sortOrder list but are
    // not themselves sortable, we cannot just do sortOrderList.indexOf(column).
    // Therefore, this method does the proper work required of iterating through
    // and ignoring non-sortable (and null) columns in the sortOrder list.
    private int getSortPosition() {
        if (getTableColumn() == null) {
            return -1;
        }

        final List<TableColumnBase> sortOrder = getVisibleSortOrderColumns();
        int pos = 0;
        for (int i = 0; i < sortOrder.size(); i++) {
            TableColumnBase _tc = sortOrder.get(i);

            if (getTableColumn().equals(_tc)) {
                return pos;
            }

            pos++;
        }

        return -1;
    }

    private List<TableColumnBase> getVisibleSortOrderColumns() {
        final ObservableList<TableColumnBase<?,?>> sortOrder = TableSkinUtils.getSortOrder(getTableSkin());

        List<TableColumnBase> visibleSortOrderColumns = new ArrayList<>();
        for (int i = 0; i < sortOrder.size(); i++) {
            TableColumnBase _tc = sortOrder.get(i);
            if (_tc == null || ! _tc.isSortable() || ! _tc.isVisible()) {
                continue;
            }

            visibleSortOrderColumns.add(_tc);
        }

        return visibleSortOrderColumns;
    }

    // as with getSortPosition above, this method iterates through the sortOrder
    // list ignoring the null and non-sortable columns, so that we get the correct
    // number of columns in the sortOrder list.
    private int getVisibleSortOrderColumnCount() {
        return getVisibleSortOrderColumns().size();
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation: Column Reordering                               *
     *                                                                         *
     **************************************************************************/

    // package for testing
    void columnReorderingStarted(double dragOffset) {
        if (! getTableColumn().isReorderable()) return;

        // Used to ensure the column ghost is positioned relative to where the
        // user clicked on the column header
        this.dragOffset = dragOffset;

        // Note here that we only allow for reordering of 'root' columns
        getTableHeaderRow().setReorderingColumn(getTableColumn());
        getTableHeaderRow().setReorderingRegion(this);
    }

    // package for testing
    void columnReordering(double sceneX, double sceneY) {
        if (! getTableColumn().isReorderable()) return;

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
        final double x = getParentHeader().sceneToLocal(sceneX, sceneY).getX();

        // calculate where the ghost column header should be
        double dragX = getTableSkin().getSkinnable().sceneToLocal(sceneX, sceneY).getX() - dragOffset;
        getTableHeaderRow().setDragHeaderX(dragX);

        double startX = 0;
        double endX = 0;
        double headersWidth = 0;
        newColumnPos = 0;
        for (TableColumnHeader header : getParentHeader().getColumnHeaders()) {
            if (! header.isVisible()) continue;

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
        int currentPos = getIndex(getTableColumn());
        newColumnPos += newColumnPos > currentPos && beforeMidPoint ?
            -1 : (newColumnPos < currentPos && !beforeMidPoint ? 1 : 0);

        double lineX = getTableHeaderRow().sceneToLocal(hoverHeader.localToScene(hoverHeader.getBoundsInLocal())).getMinX();
        lineX = lineX + ((beforeMidPoint) ? (0) : (hoverHeader.getWidth()));

        if (lineX >= -0.5 && lineX <= getTableSkin().getSkinnable().getWidth()) {
            columnReorderLine.setTranslateX(lineX);

            // then if this is the first event, we set the property to true
            // so that the line becomes visible until the drop is completed.
            // We also set reordering to true so that the various reordering
            // effects become visible (ghost, transparent overlay, etc).
            columnReorderLine.setVisible(true);
        }

        getTableHeaderRow().setReordering(true);
    }

    // package for testing
    void columnReorderingComplete() {
        if (! getTableColumn().isReorderable()) return;

        // Move col from where it is now to the new position.
        moveColumn(getTableColumn(), newColumnPos);

        // cleanup
        columnReorderLine.setTranslateX(0.0F);
        columnReorderLine.setLayoutX(0.0F);
        newColumnPos = 0;

        getTableHeaderRow().setReordering(false);
        columnReorderLine.setVisible(false);
        getTableHeaderRow().setReorderingColumn(null);
        getTableHeaderRow().setReorderingRegion(null);
        dragOffset = 0.0F;
    }

    double getDragRectHeight() {
        return getHeight();
    }

    // Used to test whether this column header properly represents the given column.
    // In particular, whether it has child column headers for all child columns
    boolean represents(TableColumnBase<?, ?> column) {
        if (!column.getColumns().isEmpty()) {
            // this column has children, but we are in a TableColumnHeader instance,
            // so the match is bad.
            return false;
        }
        return column == getTableColumn();
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final PseudoClass PSEUDO_CLASS_LAST_VISIBLE =
            PseudoClass.getPseudoClass("last-visible");

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
     private static class StyleableProperties {
         private static final CssMetaData<TableColumnHeader,Number> SIZE =
            new CssMetaData<TableColumnHeader,Number>("-fx-size",
                 SizeConverter.getInstance(), 20.0) {

            @Override
            public boolean isSettable(TableColumnHeader n) {
                return n.size == null || !n.size.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TableColumnHeader n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.sizeProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     *
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }
}
