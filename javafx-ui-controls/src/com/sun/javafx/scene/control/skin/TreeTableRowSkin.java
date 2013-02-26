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

package com.sun.javafx.scene.control.skin;

import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.TreeTableRowBehavior;
import javafx.animation.RotateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Duration;

/**
 *
 */
public class TreeTableRowSkin<T> extends TableRowSkinBase<TreeItem<T>, TreeTableRow<T>, TreeTableRowBehavior<T>, TreeTableCell<T,?>> {
    
    // maps into the TreeTableViewSkin items property via 
    // TreeTableViewSkin.treeItemToListMap
    private SimpleObjectProperty<ObservableList<TreeItem<T>>> itemsProperty;
    private TreeItem<?> treeItem;
    private boolean disclosureNodeDirty = true;
    
    private final ChangeListener<Boolean> treeItemExpandedListener = new ChangeListener<Boolean>() {
        @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean isExpanded) {
            updateDisclosureNodeRotation(true);
        }
    };
    
    public TreeTableRowSkin(TreeTableRow<T> control) {
        super(control, new TreeTableRowBehavior<T>(control));
        
        super.init(control);
        
        updateTreeItem();
        updateDisclosureNodeRotation(false);

        registerChangeListener(control.indexProperty(), "INDEX");
        registerChangeListener(control.treeItemProperty(), "TREE_ITEM");
        registerChangeListener(control.getTreeTableView().treeColumnProperty(), "TREE_COLUMN");
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if ("INDEX".equals(p)) {
            updateCells = true;
//            isDirty = true;
//            getSkinnable().requestLayout();
        } else if ("TREE_ITEM".equals(p)) {
//            updateCells = true;
            updateTreeItem();
            isDirty = true;
//            getSkinnable().requestLayout();
        } else if ("TREE_COLUMN".equals(p)) {
            // Fix for RT-27782: Need to set isDirty to true, rather than the 
            // cheaper updateCells, as otherwise the text indentation will not
            // be recalculated in TreeTableCellSkin.leftLabelPadding()
            isDirty = true;
            getSkinnable().requestLayout();
        }
    }
    
    
    
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
    
    private void updateDisclosureNodeRotation(boolean animate) {
        // no-op, this is now handled in CSS (although we no longer animate)
//        if (treeItem == null || treeItem.isLeaf()) return;
//        
//        Node disclosureNode = getSkinnable().getDisclosureNode();
//        if (disclosureNode == null) return;
//        
//        final boolean isExpanded = treeItem.isExpanded();
//        int fromAngle = isExpanded ? 0 : 90;
//        int toAngle = isExpanded ? 90 : 0;
//
//        if (animate) {
//            RotateTransition rt = new RotateTransition(Duration.millis(200), disclosureNode);
//            rt.setFromAngle(fromAngle);
//            rt.setToAngle(toAngle);
//            rt.playFromStart();
//        } else {
//            disclosureNode.setRotate(toAngle);
//        }
    }
    
    private void updateTreeItem() {
        if (treeItem != null) {
            treeItem.expandedProperty().removeListener(treeItemExpandedListener);
        }
        treeItem = getSkinnable().getTreeItem();
        if (treeItem != null) {
            treeItem.expandedProperty().addListener(treeItemExpandedListener);
        }
        
        updateDisclosureNodeRotation(false);
    }
    
    private void updateDisclosureNode() {
        if (getSkinnable().isEmpty()) return;

        Node disclosureNode = getSkinnable().getDisclosureNode();
        if (disclosureNode == null) return;
        
        boolean disclosureVisible = treeItem != null && ! treeItem.isLeaf();
        disclosureNode.setVisible(disclosureVisible);
            
        if (! disclosureVisible) {
            getChildren().remove(disclosureNode);
        } else if (disclosureNode.getParent() == null) {
            getChildren().add(disclosureNode);
            disclosureNode.toFront();
            
            // RT-26625: [TreeView, TreeTableView] can lose arrows while scrolling
            disclosureNode.impl_processCSS(true);
        } else {
            disclosureNode.toBack();
        }
    }

    private boolean childrenDirty = false;
    @Override protected void updateChildren() {
        super.updateChildren();
        
        updateDisclosureNode();
        
        if (childrenDirty) {
            childrenDirty = false;
            if (cells.isEmpty()) {
                getChildren().clear();
            } else {
                // TODO we can optimise this by only showing cells that are 
                // visible based on the table width and the amount of horizontal
                // scrolling.
                getChildren().addAll(cells);
            }
        }
    }

    @Override protected void layoutChildren(double x, double y, double w, double h) {
        if (disclosureNodeDirty) {
            updateDisclosureNode();
            disclosureNodeDirty = false;
        }
        
        Node disclosureNode = getDisclosureNode();
        if (disclosureNode != null && disclosureNode.getScene() == null) {
            updateDisclosureNode();
        }
        
        super.layoutChildren(x, y, w, h);
    }
    
    @Override protected TreeTableCell<T, ?> getCell(TableColumnBase tcb) {
        TreeTableColumn tableColumn = (TreeTableColumn<T,?>) tcb;
        TreeTableCell cell = (TreeTableCell) tableColumn.getCellFactory().call(tableColumn);
        
        cell.updateTreeTableColumn(tableColumn);
        cell.updateTreeTableView(tableColumn.getTreeTableView());
        
        return cell;
    }

    @Override protected void updateCells(boolean resetChildren) {
        super.updateCells(resetChildren);
        
        if (resetChildren) {
            childrenDirty = true;
            updateChildren();
        }
    }

    @Override protected boolean isIndentationRequired() {
        return true;
    }

    @Override protected TableColumnBase getTreeColumn() {
        return getSkinnable().getTreeTableView().getTreeColumn();
    }
    
    @Override protected int getIndentationLevel(TreeTableRow<T> control) {
        return TreeTableView.getNodeLevel(control.getTreeItem());
    }

    @Override protected double getIndentationPerLevel() {
        return getIndent();
    }

    @Override protected Node getDisclosureNode() {
        return getSkinnable().getDisclosureNode();
    }

    @Override protected boolean isDisclosureNodeVisible() {
        return getDisclosureNode() != null && treeItem != null && ! treeItem.isLeaf();
    }

    @Override protected boolean isShowRoot() {
        return getSkinnable().getTreeTableView().isShowRoot();
    }
    
    @Override protected ObservableList<TreeTableColumn<T, ?>> getVisibleLeafColumns() {
        return getSkinnable().getTreeTableView().getVisibleLeafColumns();
    }

//    @Override protected ObjectProperty<SpanModel<TreeItem<T>>> spanModelProperty() {
//        return getSkinnable().getTreeTableView().spanModelProperty();
//    }

    @Override protected void updateCell(TreeTableCell<T, ?> cell, TreeTableRow<T> row) {
        cell.updateTreeTableRow(row);
    }

    @Override protected ObjectProperty<ObservableList<TreeItem<T>>> itemsProperty() {
        if (itemsProperty != null) {
            return itemsProperty;
        }
        
        TreeTableView<T> treeTable = getSkinnable().getTreeTableView();
        if (TreeTableViewSkin.treeItemToListMap.containsKey(treeTable)) {
            ObservableList<TreeItem<T>> itemsList = TreeTableViewSkin.treeItemToListMap.get(treeTable);
            if (itemsList == null) return null;
            
            this.itemsProperty = new SimpleObjectProperty<ObservableList<TreeItem<T>>>(itemsList);
        }
        
        return this.itemsProperty();
    }

    @Override protected boolean isColumnPartiallyOrFullyVisible(TableColumnBase tc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override protected TreeTableColumn<T, ?> getTableColumnBase(TreeTableCell cell) {
        return cell.getTableColumn();
    }

    @Override protected Node getGraphic() {
        TreeTableRow<T> treeTableRow = getSkinnable();
        if (treeTableRow == null) return null;
        if (treeItem == null) return null;
        
        return treeItem.getGraphic();
    }
    
    @Override protected Control getVirtualFlowOwner() {
        return getSkinnable().getTreeTableView();
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    /** @treatAsPrivate */
    private static class StyleableProperties {
        
        private static final CssMetaData<TreeTableRow<?>,Number> INDENT = 
            new CssMetaData<TreeTableRow<?>,Number>("-fx-indent",
                SizeConverter.getInstance(), 10.0) {
                    
            @Override public boolean isSettable(TreeTableRow n) {
                DoubleProperty p = ((TreeTableRowSkin) n.getSkin()).indentProperty();
                return p == null || !p.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(TreeTableRow n) {
                final TreeTableRowSkin skin = (TreeTableRowSkin) n.getSkin();
                return (StyleableProperty<Number>)skin.indentProperty();
            }
        };
        
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(CellSkinBase.getClassCssMetaData());
            styleables.add(INDENT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    
    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

}
