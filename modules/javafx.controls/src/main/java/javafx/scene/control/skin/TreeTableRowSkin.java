/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.TreeTableRowBehavior;

/**
 * Default skin implementation for the {@link TreeTableRow} control.
 *
 * @param <T> the type of the item contained within the row
 * @see TreeTableRow
 * @since 9
 */
public class TreeTableRowSkin<T> extends TableRowSkinBase<TreeItem<T>, TreeTableRow<T>, TreeTableCell<T,?>> {

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    // maps into the TreeTableViewSkin items property via
    // TreeTableViewSkin.treeItemToListMap
    private TreeItem<?> treeItem;
    private boolean disclosureNodeDirty = true;
    private Node graphic;
    private final BehaviorBase<TreeTableRow<T>> behavior;

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TreeTableRowSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TreeTableRowSkin(TreeTableRow<T> control) {
        super(control);

        // install default input map for the TreeTableRow control
        behavior = new TreeTableRowBehavior<>(control);

        updateTreeItem();

        ListenerHelper lh = ListenerHelper.get(this);

        lh.addChangeListener(control.treeItemProperty(), (ev) -> {
            updateTreeItem();
            // There used to be an isDirty = true statement here, but this was
            // determined to be unnecessary and led to performance issues such as
            // those detailed in JDK-8143266
        });

        setupTreeTableViewListeners();
    }

    // FIXME: replace listener to fixedCellSize with direct lookup - JDK-8277000
    private void setupTreeTableViewListeners() {
        TreeTableView<T> treeTableView = getSkinnable().getTreeTableView();
        if (treeTableView == null) {
            registerInvalidationListener(getSkinnable().treeTableViewProperty(), (x) -> {
                unregisterInvalidationListeners(getSkinnable().treeTableViewProperty());
                setupTreeTableViewListeners();
            });
        } else {
            registerChangeListener(treeTableView.treeColumnProperty(), (x) -> {
                updateLeafColumns();
            });

            registerChangeListener(getTreeTableView().fixedCellSizeProperty(), e -> {
                VirtualFlow<TreeTableRow<T>> virtualFlow = getVirtualFlow();
                if (virtualFlow != null) {
                    unregisterChangeListeners(virtualFlow.widthProperty());
                }

                updateCachedFixedSize();
            });
            updateCachedFixedSize();
        }
    }

    private void updateCachedFixedSize() {
        if (getSkinnable() != null) {
            TreeTableView<T> t = getSkinnable().getTreeTableView();
            if (t != null) {
                fixedCellSize = t.getFixedCellSize();
                fixedCellSizeEnabled = fixedCellSize > 0.0;

                if (fixedCellSizeEnabled) {
                    VirtualFlow<TreeTableRow<T>> virtualFlow = getTableViewSkin().getVirtualFlow();
                    if (virtualFlow != null) {
                        registerChangeListener(virtualFlow.widthProperty(), ev -> getSkinnable().requestLayout());
                    }
                }
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private void updateTreeItemGraphic() {
        disclosureNodeDirty = true;
        getSkinnable().requestLayout();
    }

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The amount of space to multiply by the treeItem.level to get the left
     * margin for this tree cell. This is settable from CSS
     */
    private DoubleProperty indent = null;
    public final void setIndent(double value) { indentProperty().set(value); }
    public final double getIndent() { return indent == null ? 10.0 : indent.get(); }
    public final DoubleProperty indentProperty() {
        if (indent == null) {
            indent = new StyleableDoubleProperty(10.0) {
                @Override public Object getBean() {
                    return TreeTableRowSkin.this;
                }

                @Override public String getName() {
                    return "indent";
                }

                @Override public CssMetaData<TreeTableRow<?>,Number> getCssMetaData() {
                    return TreeTableRowSkin.StyleableProperties.INDENT;
                }
            };
        }
        return indent;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void updateChildren() {
        super.updateChildren();

        updateDisclosureNodeAndGraphic();
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(double x, double y, double w, double h) {
        Node disclosureNode = getDisclosureNode();
        if (disclosureNode != null && disclosureNode.getParent() == null) {
            disclosureNodeDirty = true;
        }

        if (disclosureNodeDirty) {
            updateDisclosureNodeAndGraphic();
        }

        super.layoutChildren(x, y, w, h);
    }


    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected TreeTableCell<T, ?> createCell(TableColumnBase tcb) {
        TreeTableColumn tableColumn = (TreeTableColumn<T,?>) tcb;
        TreeTableCell cell = (TreeTableCell) tableColumn.getCellFactory().call(tableColumn);

        cell.updateTableColumn(tableColumn);
        cell.updateTreeTableView(tableColumn.getTreeTableView());

        return cell;
    }

    /** {@inheritDoc} */
    @Override void updateCells() {
        super.updateCells();

        updateDisclosureNodeAndGraphic();
    }

    /** {@inheritDoc} */
    @Override boolean isIndentationRequired() {
        return true;
    }

    /** {@inheritDoc} */
    @Override TableColumnBase getTreeColumn() {
        return getTreeTableView().getTreeColumn();
    }

    /** {@inheritDoc} */
    @Override int getIndentationLevel(TreeTableRow<T> control) {
        return getTreeTableView().getTreeItemLevel(control.getTreeItem());
    }

    /** {@inheritDoc} */
    @Override double getIndentationPerLevel() {
        return getIndent();
    }

    /** {@inheritDoc} */
    @Override Node getDisclosureNode() {
        return getSkinnable().getDisclosureNode();
    }

    @Override boolean isDisclosureNodeVisible() {
        return getDisclosureNode() != null && treeItem != null && ! treeItem.isLeaf();
    }

    @Override boolean isShowRoot() {
        return getTreeTableView().isShowRoot();
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TreeTableColumn<T, ?>> getVisibleLeafColumns() {
        return getTreeTableView() == null ? FXCollections.emptyObservableList() : getTreeTableView().getVisibleLeafColumns();
    }

    /** {@inheritDoc} */
    @Override protected void updateCell(TreeTableCell<T, ?> cell, TreeTableRow<T> row) {
        cell.updateTableRow(row);
    }

    /** {@inheritDoc} */
    @Override protected TreeTableColumn<T, ?> getTableColumn(TreeTableCell cell) {
        return cell.getTableColumn();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Node> graphicProperty() {
        if (treeItem == null) return null;
        return treeItem.graphicProperty();
    }

    private void updateTreeItem() {
        unregisterInvalidationListeners(graphicProperty());
        treeItem = getSkinnable().getTreeItem();
        registerInvalidationListener(graphicProperty(), e -> updateTreeItemGraphic());
        updateDisclosureNodeAndGraphic();
    }

    private TreeTableView<T> getTreeTableView() {
        return getSkinnable().getTreeTableView();
    }

    private void updateDisclosureNodeAndGraphic() {
        disclosureNodeDirty = false;

        if (getSkinnable().isEmpty()) {
            getChildren().remove(graphic);
            return;
        }

        // check for graphic missing
        ObjectProperty<Node> graphicProperty = graphicProperty();
        Node newGraphic = graphicProperty == null ? null : graphicProperty.get();
        if (newGraphic != null) {
            // JDK-8118024: remove the old graphic
            if (newGraphic != graphic) {
                getChildren().remove(graphic);
            }

            if (! getChildren().contains(newGraphic)) {
                getChildren().add(newGraphic);
                graphic = newGraphic;
            }
        }

        // check disclosure node
        Node disclosureNode = getSkinnable().getDisclosureNode();
        if (disclosureNode != null) {
            boolean disclosureVisible = isDisclosureNodeVisible();
            disclosureNode.setVisible(disclosureVisible);

            if (!disclosureVisible) {
                getChildren().remove(disclosureNode);
            } else if (disclosureNode.getParent() == null) {
                getChildren().add(disclosureNode);
            } else {
                disclosureNode.toBack();
            }

            // JDK-8125162: [TreeView, TreeTableView] can lose arrows while scrolling
            // JDK-8124825: Ensemble tree arrow disappears
            if (disclosureNode.getScene() != null) {
                disclosureNode.applyCss();
            }
        }
    }

    // test-only
    TreeTableViewSkin<T> getTableViewSkin() {
        TreeTableView<T> t = getSkinnable().getTreeTableView();
        if (t != null && t.getSkin() instanceof TreeTableViewSkin) {
            return (TreeTableViewSkin)t.getSkin();
        }
        return null;
    }

    // test-only
    TreeItem<T> getTreeItem() {
        return (TreeItem<T>) treeItem;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {

        private static final CssMetaData<TreeTableRow<?>,Number> INDENT =
            new CssMetaData<>("-fx-indent",
                SizeConverter.getInstance(), 10.0) {

            @Override public boolean isSettable(TreeTableRow<?> n) {
                DoubleProperty p = ((TreeTableRowSkin<?>) n.getSkin()).indentProperty();
                return p == null || !p.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(TreeTableRow<?> n) {
                final TreeTableRowSkin<?> skin = (TreeTableRowSkin<?>) n.getSkin();
                return (StyleableProperty<Number>)skin.indentProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(CellSkinBase.getClassCssMetaData());
            styleables.add(INDENT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }


    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        final TreeTableView<T> treeTableView = getSkinnable().getTreeTableView();
        switch (attribute) {
            case SELECTED_ITEMS: {
                if (treeTableView.getSelectionModel() != null) {
                    // FIXME this could be optimised to iterate over cellsMap only
                    // (selectedCells could be big, cellsMap is much smaller)
                    List<Node> selection = new ArrayList<>();
                    int index = getSkinnable().getIndex();
                    for (TreeTablePosition<T,?> pos : treeTableView.getSelectionModel().getSelectedCells()) {
                        if (pos.getRow() == index) {
                            TreeTableColumn<T,?> column = pos.getTableColumn();
                            if (column == null) {
                                /* This is the row-based case */
                                column = treeTableView.getVisibleLeafColumn(0);
                            }
                            TreeTableCell<T,?> cell = cellsMap.get(column).get();
                            if (cell != null) selection.add(cell);
                        }
                        return FXCollections.observableArrayList(selection);
                    }
                }
                return FXCollections.observableArrayList();
            }
            case CELL_AT_ROW_COLUMN: {
                int colIndex = (Integer)parameters[1];
                TreeTableColumn<T,?> column = treeTableView.getVisibleLeafColumn(colIndex);
                if (cellsMap.containsKey(column)) {
                    return cellsMap.get(column).get();
                }
                return null;
            }
            case FOCUS_ITEM: {
                TreeTableView.TreeTableViewFocusModel<T> fm = treeTableView.getFocusModel();
                TreeTablePosition<T,?> focusedCell = fm.getFocusedCell();
                TreeTableColumn<T,?> column = focusedCell.getTableColumn();
                if (column == null) {
                    /* This is the row-based case */
                    column = treeTableView.getVisibleLeafColumn(0);
                }
                if (cellsMap.containsKey(column)) {
                    return cellsMap.get(column).get();
                }
                return null;
            }
            default:
                return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
