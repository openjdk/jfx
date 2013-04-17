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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler;
import com.sun.javafx.scene.control.behavior.TreeCellBehavior;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.util.Callback;

public class TreeCellSkin<T> extends CellSkinBase<TreeCell<T>, TreeCellBehavior<T>> {

    /*
     * This is rather hacky - but it is a quick workaround to resolve the
     * issue that we don't know maximum width of a disclosure node for a given
     * TreeView. If we don't know the maximum width, we have no way to ensure
     * consistent indentation for a given TreeView.
     *
     * To work around this, we create a single WeakHashMap to store a max
     * disclosureNode width per TreeView. We use WeakHashMap to help prevent
     * any memory leaks.
     * 
     * RT-19656 identifies a related issue, which is that we may not provide
     * indentation to any TreeItems because we have not yet encountered a cell
     * which has a disclosureNode. Once we scroll and encounter one, indentation
     * happens in a displeasing way.
     */
    private static final Map<TreeView<?>, Double> maxDisclosureWidthMap = new WeakHashMap<TreeView<?>, Double>();

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
                    return TreeCellSkin.this;
                }

                @Override public String getName() {
                    return "indent";
                }

                @Override public CssMetaData<TreeCell<?>,Number> getCssMetaData() {
                    return StyleableProperties.INDENT;
                }
            };
        }
        return indent; 
    }
    
    private boolean disclosureNodeDirty = true;
    private TreeItem<?> treeItem;
    
    private MultiplePropertyChangeListenerHandler treeItemListener = new MultiplePropertyChangeListenerHandler(new Callback<String, Void>() {
        @Override public Void call(String p) {
            if ("EXPANDED".equals(p)) {
                updateDisclosureNodeRotation(true);
            }
            return null;
        }
    });
    
    public TreeCellSkin(TreeCell<T> control) {
        super(control, new TreeCellBehavior<T>(control));
        
        updateTreeItem();
        updateDisclosureNodeRotation(false);
        
        registerChangeListener(control.treeItemProperty(), "TREE_ITEM");
        registerChangeListener(control.textProperty(), "TEXT");
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("TREE_ITEM".equals(p)) {
            updateTreeItem();
            disclosureNodeDirty = true;
            getSkinnable().requestLayout();
        } else if ("TEXT".equals(p)) {
            getSkinnable().requestLayout();
        }
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
//            rt.play();
//        } else {
//            disclosureNode.setRotate(toAngle);
//        }
    }
    
    private void updateTreeItem() {
        if (treeItem != null) {
            treeItemListener.unregisterChangeListener(treeItem.expandedProperty());
        }
        treeItem = getSkinnable().getTreeItem();
        if (treeItem != null) {
            treeItemListener.registerChangeListener(treeItem.expandedProperty(), "EXPANDED");
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
        } else {
            disclosureNode.toBack();
        }
        
        // RT-26625: [TreeView, TreeTableView] can lose arrows while scrolling
        // RT-28668: Ensemble tree arrow disappears
        if (disclosureNode.getScene() != null) {
            disclosureNode.impl_processCSS(true);
        }
    }

    @Override protected void updateChildren() {
        super.updateChildren();
        updateDisclosureNode();
    }
    
    @Override protected void layoutChildren(double x, final double y,
            double w, final double h) {
        // RT-25876: can not null-check here as this prevents empty rows from
        // being cleaned out.
        // if (treeItem == null) return;
        
        TreeView tree = getSkinnable().getTreeView();
        if (tree == null) return;
        
        if (disclosureNodeDirty) {
            updateDisclosureNode();
            disclosureNodeDirty = false;
        }
        
        Node disclosureNode = getSkinnable().getDisclosureNode();
        
        int level = TreeView.getNodeLevel(treeItem);
        if (! tree.isShowRoot()) level--;
        double leftMargin = getIndent() * level;

        x += leftMargin;

        // position the disclosure node so that it is at the proper indent
        boolean disclosureVisible = disclosureNode != null && treeItem != null && ! treeItem.isLeaf();

        final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
            maxDisclosureWidthMap.get(tree) : 18;   // RT-19656: default width of default disclosure node
        double disclosureWidth = defaultDisclosureWidth;

        if (disclosureVisible) {
            if (disclosureNode == null || disclosureNode.getScene() == null) {
                updateChildren();
            }
            
            if (disclosureNode != null) {
                disclosureWidth = disclosureNode.prefWidth(h);
                if (disclosureWidth > defaultDisclosureWidth) {
                    maxDisclosureWidthMap.put(tree, disclosureWidth);
                }

                double ph = disclosureNode.prefHeight(disclosureWidth);
                
                disclosureNode.resize(disclosureWidth, ph);
                positionInArea(disclosureNode, x, y,
                        disclosureWidth, ph, /*baseline ignored*/0,
                        HPos.CENTER, VPos.CENTER);
            }
        }

        // determine starting point of the graphic or cell node, and the
        // remaining width available to them
        final int padding = treeItem != null && treeItem.getGraphic() == null ? 0 : 3;
        x += disclosureWidth + padding;
        w -= (leftMargin + disclosureWidth + padding);

        layoutLabelInArea(x, y, w, h);
    }

    @Override protected double computePrefHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        double pref = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
        Node d = getSkinnable().getDisclosureNode();
        return (d == null) ? pref : Math.max(d.prefHeight(-1), pref);
    }
    
    @Override protected double computePrefWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        double labelWidth = super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);

        double pw = snappedLeftInset() + snappedRightInset();

        TreeView tree = getSkinnable().getTreeView();
        if (tree == null) return pw;
        
        if (treeItem == null) return pw;
        
        pw = labelWidth;

        // determine the amount of indentation
        int level = TreeView.getNodeLevel(treeItem);
        if (! tree.isShowRoot()) level--;
        pw += getIndent() * level;

        // include the disclosure node width
        Node disclosureNode = getSkinnable().getDisclosureNode();
        double disclosureNodePrefWidth = disclosureNode == null ? 0 : disclosureNode.prefWidth(-1);
        final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
                maxDisclosureWidthMap.get(tree) : 0;
        pw += Math.max(defaultDisclosureWidth, disclosureNodePrefWidth);

        return pw;
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    /** @treatAsPrivate */
    private static class StyleableProperties {
        
        private static final CssMetaData<TreeCell<?>,Number> INDENT = 
            new CssMetaData<TreeCell<?>,Number>("-fx-indent",
                SizeConverter.getInstance(), 10.0) {
                    
            @Override public boolean isSettable(TreeCell n) {
                DoubleProperty p = ((TreeCellSkin) n.getSkin()).indentProperty();
                return p == null || !p.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(TreeCell n) {
                final TreeCellSkin skin = (TreeCellSkin) n.getSkin();
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
