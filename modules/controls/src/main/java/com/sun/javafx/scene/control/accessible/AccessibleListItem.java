/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.accessible.AccessibleNode;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.GridItemProvider;
import com.sun.javafx.accessible.providers.SelectionItemProvider;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ListView;

public final class AccessibleListItem extends AccessibleControl implements 
        SelectionItemProvider, GridItemProvider {

    Cell listCell;
    static ListView listView = null;

    /**
     * Constructor
     * 
     * @param listCell The associated list item cell
     */
    public AccessibleListItem(Cell listCell) {
        super(listCell);
        this.listCell = listCell ; 
        Node node = listCell;
        if (listView == null) {
            while (node.getParent() != null) {
                Node parent = node.getParent();
                if (parent instanceof ListView) {
                    listView = (ListView)parent;
                    break;
                }
                node = node.getParent();
            }
        }
    }
    
    /**
     * Get the property value for the specified property ID.
     * 
     * @param propertyId    specifies which property to fetch
     * 
     * @return the requested property
     */
    @Override public Object getPropertyValue(int propertyId) {
        Object retVal = null ;
        switch(propertyId){
            case PropertyIds.NAME:
            case PropertyIds.DESCRIBED_BY:
                retVal = listCell.getText();
                break;
            case PropertyIds.CONTROL_TYPE:
                retVal = ControlTypeIds.LIST_ITEM;
                break;
            case PropertyIds.IS_KEYBOARD_FOCUSABLE:
                retVal = listCell.isFocusTraversable();
                break;
            case PropertyIds.HAS_KEYBOARD_FOCUS:
                retVal = listCell.isFocused();
                break;
            case PropertyIds.IS_CONTROL_ELEMENT:
                retVal = true;
                break;
            case PropertyIds.IS_ENABLED:
                retVal = !listCell.isDisabled();
                break;
            case PropertyIds.CLASS_NAME:
                retVal = this.getClass().toString();
                break;
        }   
        return retVal;
    }
    
    /**
     * Add this object to the selection.
     */
    @Override
    public void addToSelection() {
        listView.getSelectionModel().select(listCell);
    }

    /**
     * Remove this object from the selection.
     */
    @Override
    public void removeFromSelection() {
        // TODO assuming single selection for now
        listView.getSelectionModel().clearSelection();
    }

    /**
     * Select this object.
     */
    @Override
    public void select() {
        listView.getSelectionModel().select(listCell);
    }

    /**
     * Determine if this object is selected.
     * 
     * @return whether or not this object is selected
     */
    @Override
    public boolean isSelected() {
        return listCell == listView.getSelectionModel().getSelectedItem();
    }

    /**
     * Get the Glass accessible for the container.
     * 
     * @return the Glass accessible of the container.
     */
    @Override
    public Object getSelectionContainer() {
        throw new UnsupportedOperationException();
    }

    /**
     * For GridItemProvider Row property
     *
     * @return the row index of this object.
     */
    @Override
    public int getRow() {
        return listView.getItems().indexOf(listCell);
    }

    /**
     * For GridItemProvider Column property
     *
     * @return the column index of this object.
     */
    @Override
    public int getColumn() {
        return 1;
    }

    /**
     * For GridItemProvider RowSpan property
     *
     * @return the the number of rows spanned by this object.
     */
    @Override
    public int getRowSpan() {
        return listView.getItems().size();
       
    }

    /**
     * For GridItemProvider ColumnSpan property
     *
     * @return the the number of columns spanned by this object.
     */
    @Override
    public int getColumnSpan() {
        return 1;
    }

    /**
     * For GridItemProvider ContainingGrid property
     *
     * @return the Glass provider that implements the GridProvider pattern and represents
     *         the container of this object. 
     */
    @Override
    public Object getContainingGrid() {
        throw new UnsupportedOperationException();
    }
    
}
