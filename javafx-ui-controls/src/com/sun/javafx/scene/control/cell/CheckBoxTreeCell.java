/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.cell;

import com.sun.javafx.scene.control.CheckBoxTreeItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link TreeCell} implementation that draws a 
 * {@link CheckBox} node inside the cell, along with support for common 
 * interactions (discussed in more depth shortly).
 * 
 * <p>To make creating TreeViews with CheckBoxes easier, a convenience class 
 * called {@link CheckBoxTreeItem} is provided. It is <b>highly</b> recommended 
 * that developers use this class, rather than the regular {@link TreeItem}
 * class, when constructing their TreeView tree structures. Refer to the 
 * CheckBoxTreeItem API documentation for an example on how these two classes
 * can be combined.
 * 
 * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox to 
 * the right of the 'disclosure node' (i.e. the arrow). The item stored in 
 * {@link CheckBoxTreeItem#getValue()} will then have the StringConverter called 
 * on it, and this text will take all remaining horizontal space. Additionally, 
 * by using {@link CheckBoxTreeItem}, the TreeView will automatically handle 
 * situations such as:
 * 
 * <ul>
 *   <li>Clicking on the {@link CheckBox} beside an item that has children will 
 *      result in all children also becoming selected/unselected.
 *   <li>Clicking on the {@link CheckBox} beside an item that has a parent will 
 *      possibly toggle the state of the parent. For example, if you select a 
 *      single child, the parent will become indeterminate (indicating partial 
 *      selection of children). If you proceed to select all children, the 
 *      parent will then show that it too is selected. This is recursive, with 
 *      all parent nodes updating as expected.
 * </ul>
 * 
 * If it is decided that using {@link CheckBoxTreeItem} is not desirable, 
 * then it is necessary to call one of the constructors where a {@link Callback}
 * is provided that can return an {@code ObservableValue<Boolean>}
 * given a {@link TreeItem} instance. This {@code ObservableValue<Boolean>} 
 * should represent the boolean state of the given {@link TreeItem}.
 * 
 * @param <T> The type of the elements contained within the TreeView TreeItem 
 *      instances.
 */
public class CheckBoxTreeCell<T> extends TreeCell<T> {
    private final CheckBox checkBox;
    
    private final StringConverter<TreeItem<T>> converter;
    
    private final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty;
    private ObservableValue<Boolean> booleanProperty;
    
    private BooleanProperty indeterminateProperty;
    
    /**
     * Creates a default {@link CheckBoxTreeCell} that assumes the TreeView is 
     * constructed with {@link CheckBoxTreeItem} instances, rather than the 
     * default {@link TreeItem}.
     * By using {@link CheckBoxTreeItem}, it will internally manage the selected 
     * and indeterminate state of each item in the tree.
     */
    public CheckBoxTreeCell() {
        // getSelectedProperty as anonymous inner class to deal with situation
        // where the user is using CheckBoxTreeItem instances in their tree
        this(new Callback<TreeItem<T>, ObservableValue<Boolean>>() {
            @Override public ObservableValue<Boolean> call(TreeItem<T> item) {
                if (item instanceof CheckBoxTreeItem<?>) {
                    return ((CheckBoxTreeItem<?>)item).selectedProperty();
                }
                return null;
            }
        });
    }
    
    /**
     * Creates a {@link CheckBoxTreeCell} for use in a TreeView control via a 
     * cell factory. Unlike {@link CheckBoxTreeCell#CheckBoxTreeCell()}, this 
     * method does not assume that all TreeItem instances in the TreeView are 
     * {@link CheckBoxTreeItem}.
     * 
     * <p>To call this method, it is necessary to provide a 
     * {@link Callback} that, given an object of type TreeItem<T>, will return 
     * an {@code ObservableValue<Boolean>} that represents whether the given 
     * item is selected or not. This {@code ObservableValue<Boolean>} will be 
     * bound bidirectionally (meaning that the CheckBox in the cell will 
     * set/unset this property based on user interactions, and the CheckBox will 
     * reflect the state of the {@code ObservableValue<Boolean>}, if it changes 
     * externally).
     * 
     * <p>If the items are not {@link CheckBoxTreeItem} instances, it becomes 
     * the developers responsibility to handle updating the state of parent and 
     * children TreeItems. This means that, given a TreeItem, this class will 
     * simply toggles the {@code ObservableValue<Boolean>} that is provided, and 
     * no more. Of course, this functionality can then be implemented externally 
     * by adding observers to the {@code ObservableValue<Boolean>}, and toggling 
     * the state of other properties as necessary.
     * 
     * @param getSelectedProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} that represents whether the given 
     *      item is selected or not.
     */
    public CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty) {
        this(getSelectedProperty, CellUtils.<T>defaultTreeItemStringConverter(), null);
    }
    
    /**
     * Creates a {@link CheckBoxTreeCell} for use in a TreeView control via a 
     * cell factory. Unlike {@link CheckBoxTreeCell#CheckBoxTreeCell()}, this 
     * method does not assume that all TreeItem instances in the TreeView are 
     * {@link CheckBoxTreeItem}.
     * 
     * <p>To call this method, it is necessary to provide a {@link Callback} 
     * that, given an object of type TreeItem<T>, will return an 
     * {@code ObservableValue<Boolean>} that represents whether the given item 
     * is selected or not. This {@code ObservableValue<Boolean>} will be bound 
     * bidirectionally (meaning that the CheckBox in the cell will set/unset 
     * this property based on user interactions, and the CheckBox will reflect
     * the state of the {@code ObservableValue<Boolean>}, if it changes 
     * externally).
     * 
     * <p>If the items are not {@link CheckBoxTreeItem} instances, it becomes 
     * the developers responsibility to handle updating the state of parent and 
     * children TreeItems. This means that, given a TreeItem, this class will 
     * simply toggles the {@code ObservableValue<Boolean>} that is provided, and 
     * no more. Of course, this functionality can then be implemented externally 
     * by adding observers to the {@code ObservableValue<Boolean>}, and toggling 
     * the state of other properties as necessary.
     * 
     * @param getSelectedProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} that represents whether the given 
     *      item is selected or not.
     * @param converter A StringConverter that, give an object of type TreeItem<T>, will 
     *      return a String that can be used to represent the object visually.
     */
    public CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<TreeItem<T>> converter) {
        this(getSelectedProperty, converter, null);
    }

    private CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<TreeItem<T>> converter, 
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getIndeterminateProperty) {
        if (getSelectedProperty == null) {
            throw new NullPointerException("getSelectedProperty can not be null");
        }
        this.getSelectedProperty = getSelectedProperty;
        this.converter = converter;
        
        this.checkBox = new CheckBox();
        this.checkBox.setAllowIndeterminate(false);
        setGraphic(checkBox);
    }
    
    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            // update the node content
            setText(converter.toString(getTreeItem()));
            setGraphic(checkBox);
            
            // uninstall bindings
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            if (indeterminateProperty != null) {
                checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
            }

            // install new bindings.
            // We special case things when the TreeItem is a CheckBoxTreeItem
            if (getTreeItem() instanceof CheckBoxTreeItem) {
                CheckBoxTreeItem<T> cbti = (CheckBoxTreeItem<T>) getTreeItem();
                booleanProperty = cbti.selectedProperty();
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
                
                indeterminateProperty = cbti.indeterminateProperty();
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);
            } else {
                booleanProperty = getSelectedProperty.call(getTreeItem());
                if (booleanProperty != null) {
                    checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
                }
            }
        }
    }
}