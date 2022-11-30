/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

/**
 * Region responsible for painting the entire row of column headers.
 *
 * @since 9
 * @see javafx.scene.control.TableView
 * @see TableViewSkin
 * @see javafx.scene.control.TreeTableView
 * @see TreeTableViewSkin
 */
public class TableHeaderRow extends StackPane {

    /* *************************************************************************
     *                                                                         *
     * Static Fields                                                           *
     *                                                                         *
     **************************************************************************/


    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    // JDK-8090129: This constant should not be static, because the
    // Locale may change between instances.
    private final String MENU_SEPARATOR =
            ControlResources.getString("TableView.nestedColumnControlMenuSeparator");

    private final VirtualFlow flow;
    final TableViewSkinBase<?,?,?,?,?> tableSkin;
    private Map<TableColumnBase, CheckMenuItem> columnMenuItems = new HashMap<>();
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

    private Region filler;

    /**
     * This is the region where the user can interact with to show/hide columns.
     * It is positioned in the top-right hand corner of the TableHeaderRow, and
     * when clicked shows a PopupMenu consisting of all leaf columns.
     */
    private Pane cornerRegion;
    final DoubleProperty cornerPadding = new SimpleDoubleProperty();

    /**
     * PopupMenu shown to users to allow for them to hide/show columns in the
     * table.
     */
    private ContextMenu columnPopupMenu;

    /**
     * There are two different mouse dragged event handlers in the header code.
     * Firstly, the column reordering functionality, and secondly, the column
     * resizing functionality. Because these are handled in separate classes and
     * with separate event handlers, we occasionally run into the issue where
     * both event handlers were being called, resulting in bad UX. To remove this
     * issue, we lock when the column dragging happens, and prevent resize operations
     * from taking place.
     */
    boolean columnDragLock = false;



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private InvalidationListener tableWidthListener = o -> updateTableWidth();

    private InvalidationListener tablePaddingListener = o -> updateTableWidth();

    // This is necessary for RT-20300 (but was updated for RT-20840)
    private ListChangeListener visibleLeafColumnsListener = c -> getRootHeader().setHeadersNeedUpdate();

    private final ListChangeListener tableColumnsListener = c -> {
        while (c.next()) {
            updateTableColumnListeners(c.getAddedSubList(), c.getRemoved());
        }
    };

    private final InvalidationListener columnTextListener = observable -> {
        TableColumnBase<?,?> column = (TableColumnBase<?,?>) ((StringProperty)observable).getBean();
        CheckMenuItem menuItem = columnMenuItems.get(column);
        if (menuItem != null) {
            menuItem.setText(getText(column.getText(), column));
        }
    };

    private final ChangeListener<Boolean> cornerPaddingListener = (obs, ov, nv) -> updateCornerPadding();

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

    private final WeakChangeListener<Boolean> weakCornerPaddingListener =
            new WeakChangeListener<>(cornerPaddingListener);



    /* *************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TableHeaderRow instance to visually represent the column
     * header area of controls such as {@link javafx.scene.control.TableView} and
     * {@link javafx.scene.control.TreeTableView}.
     *
     * @param skin The skin used by the UI control.
     */
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
        TableSkinUtils.getVisibleLeafColumns(skin).addListener(weakVisibleLeafColumnsListener);

        // popup menu for hiding/showing columns
        columnPopupMenu = new ContextMenu();
        updateTableColumnListeners(TableSkinUtils.getColumns(tableSkin), Collections.<TableColumnBase<?,?>>emptyList());
        TableSkinUtils.getVisibleLeafColumns(skin).addListener(weakTableColumnsListener);
        TableSkinUtils.getColumns(tableSkin).addListener(weakTableColumnsListener);

        // drag header region. Used to indicate the current column being reordered
        dragHeader = new StackPane();
        dragHeader.setVisible(false);
        dragHeader.getStyleClass().setAll("column-drag-header");
        dragHeader.setManaged(false);
        dragHeader.setMouseTransparent(true);
        dragHeader.getChildren().add(dragHeaderLabel);

        // the header lives inside a NestedTableColumnHeader
        NestedTableColumnHeader rootHeader = createRootHeader();
        setRootHeader(rootHeader);
        rootHeader.setFocusTraversable(false);
        rootHeader.setTableHeaderRow(this);

        // The 'filler' area that extends from the right-most column to the edge
        // of the tableview, or up to the 'column control' button
        filler = new Region();
        filler.getStyleClass().setAll("filler");

        // Give focus to the table when an empty area of the header row is clicked.
        // This ensures the user knows that the table has focus.
        setOnMousePressed(e -> {
            skin.getSkinnable().requestFocus();
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

        BooleanProperty tableMenuButtonVisibleProperty = TableSkinUtils.tableMenuButtonVisibleProperty(skin);
        if (tableMenuButtonVisibleProperty != null) {
            cornerRegion.visibleProperty().bind(tableMenuButtonVisibleProperty);
        }

        cornerRegion.setOnMousePressed(me -> {
            // show a popupMenu which lists all columns
            columnPopupMenu.show(cornerRegion, Side.BOTTOM, 0, 0);
            me.consume();
        });
        cornerRegion.visibleProperty().addListener(weakCornerPaddingListener);
        flow.getVbar().visibleProperty().addListener(weakCornerPaddingListener);

        // the actual header
        // the region that is anchored above the vertical scrollbar
        // a 'ghost' of the header being dragged by the user to force column
        // reordering
        getChildren().addAll(filler, rootHeader, cornerRegion, dragHeader);
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/


    /**
     * Indicates if a reordering operation of a column is in progress. The value is {@code true} during a column
     * reordering operation, and {@code false} otherwise. When a column is reordered (for example, by dragging its
     * header), this property is updated automatically. Setting the value manually should be done when a subclass
     * overrides the default reordering behavior. Calling {@link #setReorderingRegion(TableColumnHeader)} before setting
     * this property is required as well.
     *
     * @since 12
     */
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

    public final void setReordering(boolean value) {
        this.reordering.set(value);
    }

    public final boolean isReordering() {
        return reordering.get();
    }

    public final BooleanProperty reorderingProperty() {
        return reordering;
    }

    // --- root header
    /*
     * The header row is actually just one NestedTableColumnHeader that spans
     * the entire width. Nested within this is the TableColumnHeader's and
     * NestedTableColumnHeader's, as necessary. This makes it nice and clean
     * to handle column reordering - we basically enforce the rule that column
     * reordering only occurs within a single NestedTableColumnHeader, and only
     * at that level.
     */
    private ReadOnlyObjectWrapper<NestedTableColumnHeader> rootHeader = new ReadOnlyObjectWrapper<>(this, "rootHeader");
    private final ReadOnlyObjectProperty<NestedTableColumnHeader> rootHeaderProperty() {
        return rootHeader.getReadOnlyProperty();
    }

    /**
     * Returns the root header for all columns. The root header is a {@link NestedTableColumnHeader} that contains the
     * {@code NestedTableColumnHeader}s that represent each column. It spans the entire width of the {@code TableView}.
     * This allows any developer overriding a {@code TableColumnHeader} to easily access the root header and all others
     * {@code TableColumnHeader}s.
     *
     * @return the root header
     * @implNote This design enforces that column reordering occurs only within a single {@code NestedTableColumnHeader}
     * and only at that level.
     * @since 12
     */
    public final NestedTableColumnHeader getRootHeader() {
        return rootHeader.get();
    }

    private final void setRootHeader(NestedTableColumnHeader value) {
        rootHeader.set(value);
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        double x = scrollX;
        double headerWidth = snapSizeX(getRootHeader().prefWidth(-1));
        double prefHeight = getHeight() - snappedTopInset() - snappedBottomInset();
        double cornerWidth = snapSizeX(flow.getVbar().prefWidth(-1));

        // position the main nested header
        getRootHeader().resizeRelocate(x, snappedTopInset(), headerWidth, prefHeight);

        // position the filler region
        final Control control = tableSkin.getSkinnable();
        if (control == null) {
            return;
        }

        final BooleanProperty tableMenuButtonVisibleProperty = TableSkinUtils.tableMenuButtonVisibleProperty(tableSkin);

        final double controlInsets = control.snappedLeftInset() + control.snappedRightInset();
        double fillerWidth = tableWidth - headerWidth + filler.getInsets().getLeft() - controlInsets;
        fillerWidth -= tableMenuButtonVisibleProperty != null && tableMenuButtonVisibleProperty.get() ? cornerWidth : 0;
        filler.setVisible(fillerWidth > 0);
        if (fillerWidth > 0) {
            filler.resizeRelocate(x + headerWidth, snappedTopInset(), fillerWidth, prefHeight);
        }

        // position the top-right rectangle (which sits above the scrollbar if visible, or adds padding to the
        // header of the last visible column if not)
        cornerRegion.resizeRelocate(tableWidth - cornerWidth, snappedTopInset(), cornerWidth, prefHeight);
        updateCornerPadding();
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        return getRootHeader().prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        // we hardcode 24.0 here to avoid RT-37616, where the
        // entire header row would disappear when all columns were hidden.
        double headerPrefHeight = getRootHeader().prefHeight(width);
        headerPrefHeight = headerPrefHeight == 0.0 ? 24.0 : headerPrefHeight;
        return snappedTopInset() + headerPrefHeight + snappedBottomInset();
    }

    /**
     * Called whenever the value of the horizontal scrollbar changes in order to request layout changes, shifting the
     * {@code TableColumnHeader}s.
     * <p>
     * For example, if custom components are added around a {@code TableColumnHeader} (such as icons above), they will
     * also need to be shifted. When overriding, calling {@code super()} is required to shift the {@code
     * TableColumnHeader}s, and it's up to the developer to notify its own custom components of this change.
     *
     * @since 12
     */
    protected void updateScrollX() {
        scrollX = flow.getHbar().isVisible() ? -flow.getHbar().getValue() : 0.0F;
        requestLayout();

        // Fix for RT-36392: without this call even though we call requestLayout()
        // we don't seem to ever see the layoutChildren() method above called,
        // which means the layout is not always updated to use the latest scrollX.
        layout();
    }


    /**
     * Updates the table width when a resize operation occurs. This method is called continuously when the control width
     * is resizing in order to properly clip this {@code TableHeaderRow}. Overriding this method allows a subclass to
     * customize the resizing behavior.
     * <p>
     * Normally, the {@code TableHeaderRow} is using the full space ({@code TableView} width), but in some cases that
     * space may be reduced. For example, if a vertical header that will display the row number is introduced, the
     * {@code TableHeaderRow} would need to be clipped a bit shorter in order not to overlap that vertical header.
     * Calling {@code super()} first when overriding this method allows {@link #getClip()} to compute the right width in
     * order apply a transformation.
     *
     * @since 12
     */
    protected void updateTableWidth() {
        // snapping added for RT-19428
        final Control c = tableSkin.getSkinnable();
        if (c == null) {
            this.tableWidth = 0;
        } else {
            Insets insets = c.getInsets() == null ? Insets.EMPTY : c.getInsets();
            double padding = snapSizeX(insets.getLeft()) + snapSizeX(insets.getRight());
            this.tableWidth = snapSizeX(c.getWidth()) - padding;
        }

        clip.setWidth(tableWidth);
    }

    /**
     * Creates a new NestedTableColumnHeader instance. By default this method should not be overridden, but in some
     * circumstances it makes sense (e.g. testing, or when extreme customization is desired).
     *
     * @return A new NestedTableColumnHeader instance.
     */
    protected NestedTableColumnHeader createRootHeader() {
        return new NestedTableColumnHeader(null);
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the current {@link TableColumnHeader} being moved during reordering.
     *
     * @return the current {@code TableColumnHeader} being moved
     * @since 12
     */
    protected TableColumnHeader getReorderingRegion() {
        return reorderingRegion;
    }

    void setReorderingColumn(TableColumnBase rc) {
        dragHeaderLabel.setText(rc == null ? "" : rc.getText());
    }

    /**
     * Sets the {@code TableColumnHeader} that is being moved during a reordering operation. This is automatically set
     * by the {@code TableColumnHeader} when reordering starts. This method should only be called manually if the
     * default reordering behavior is overridden. Calling {@link #setReordering(boolean)} after the call is required.
     *
     * @param reorderingRegion the {@code TableColumnHeader} being reordered
     * @since 12
     */
    protected void setReorderingRegion(TableColumnHeader reorderingRegion) {
        this.reorderingRegion = reorderingRegion;

        if (reorderingRegion != null) {
            dragHeader.resize(reorderingRegion.getWidth(), dragHeader.getHeight());
        }
    }

    void setDragHeaderX(double dragHeaderX) {
        dragHeader.setTranslateX(dragHeaderX);
    }

    TableColumnHeader getColumnHeaderFor(final TableColumnBase<?,?> col) {
        if (col == null) return null;
        List<TableColumnBase<?,?>> columnChain = new ArrayList<>();
        columnChain.add(col);

        TableColumnBase<?,?> parent = col.getParentColumn();
        while (parent != null) {
            columnChain.add(0, parent);
            parent = parent.getParentColumn();
        }

        // we now have a list from top to bottom of a nested column hierarchy,
        // and we can now navigate down to retrieve the header with ease
        TableColumnHeader currentHeader = getRootHeader();
        for (int depth = 0; depth < columnChain.size(); depth++) {
            // this is the column we are looking for at this depth
            TableColumnBase<?,?> column = columnChain.get(depth);

            // and now we iterate through the nested table column header at this
            // level to get the header
            currentHeader = getColumnHeaderFor(column, currentHeader);
        }
        return currentHeader;
    }

    private TableColumnHeader getColumnHeaderFor(final TableColumnBase<?,?> col, TableColumnHeader currentHeader) {
        if (currentHeader instanceof NestedTableColumnHeader) {
            List<TableColumnHeader> headers = ((NestedTableColumnHeader)currentHeader).getColumnHeaders();

            for (int i = 0; i < headers.size(); i++) {
                TableColumnHeader header = headers.get(i);
                if (header.getTableColumn() == col) {
                    return header;
                }
            }
        }

        return null;
    }

    private void updateTableColumnListeners(List<? extends TableColumnBase<?,?>> added, List<? extends TableColumnBase<?,?>> removed) {
        // remove binding from all removed items
        for (TableColumnBase tc : removed) {
            remove(tc);
        }

        rebuildColumnMenu();
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

    private void rebuildColumnMenu() {
        columnPopupMenu.getItems().clear();

        for (TableColumnBase<?,?> col : TableSkinUtils.getColumns(tableSkin)) {
            // we only create menu items for leaf columns, visible or not
            if (col.getColumns().isEmpty()) {
                createMenuItem(col);
            } else {
                List<TableColumnBase<?,?>> leafColumns = getLeafColumns(col);
                for (TableColumnBase<?,?> _col : leafColumns) {
                    createMenuItem(_col);
                }
            }
        }
    }

    private List<TableColumnBase<?,?>> getLeafColumns(TableColumnBase<?,?> col) {
        List<TableColumnBase<?,?>> leafColumns = new ArrayList<>();

        for (TableColumnBase<?,?> _col : col.getColumns()) {
            if (_col.getColumns().isEmpty()) {
                leafColumns.add(_col);
            } else {
                leafColumns.addAll(getLeafColumns(_col));
            }
        }

        return leafColumns;
    }

    private void createMenuItem(TableColumnBase<?,?> col) {
        CheckMenuItem item = columnMenuItems.get(col);
        if (item == null) {
            item = new CheckMenuItem();
            columnMenuItems.put(col, item);
        }

        // bind column text and isVisible so that the menu item is always correct
        item.setText(getText(col.getText(), col));
        col.textProperty().addListener(weakColumnTextListener);

        // ideally we would have API to observe the binding status of a property,
        // but for now that doesn't exist, so we set this once and then forget
        item.setDisable(col.visibleProperty().isBound());

        // fake bidrectional binding (a real one was used here but resulted in JBS-8136468)
        item.setSelected(col.isVisible());
        final CheckMenuItem _item = item;
        item.selectedProperty().addListener(o -> {
            if (col.visibleProperty().isBound()) return;
            col.setVisible(_item.isSelected());
        });
        col.visibleProperty().addListener(o -> _item.setSelected(col.isVisible()));

        columnPopupMenu.getItems().add(item);
    }

    /*
     * Function used for building the strings in the popup menu
     */
    private String getText(String text, TableColumnBase col) {
        String s = text;
        TableColumnBase parentCol = col.getParentColumn();
        while (parentCol != null) {
            if (isColumnVisibleInHeader(parentCol, TableSkinUtils.getColumns(tableSkin))) {
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

    // When the corner region is visible, and the vertical scrollbar is not,
    // in case the corner region is over the header of the last
    // visible column, if any, we have to consider its width as extra padding
    // for that header, to prevent the content of the latter from being partially
    // covered.
    private void updateCornerPadding() {
        double padding = 0.0;
        if (cornerRegion.isVisible() && !flow.getVbar().isVisible()) {
            double x = cornerRegion.getLayoutX();
            padding = getRootHeader().getColumnHeaders().stream()
                    .filter(header -> header.isLastVisibleColumn)
                    .findFirst()
                    .map(header -> {
                        Bounds bounds = header.localToScene(header.getBoundsInLocal());
                        return bounds.getMinX() <= x && x < bounds.getMaxX() ?
                             cornerRegion.getWidth() : 0.0;
                    })
                    .orElse(0.0);
        }
        cornerPadding.set(padding);
    }

}
