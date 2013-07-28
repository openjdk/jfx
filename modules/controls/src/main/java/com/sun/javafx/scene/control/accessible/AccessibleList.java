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

import com.sun.javafx.accessible.providers.GridProvider;
import com.sun.javafx.accessible.providers.SelectionProvider;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.OrientationType;
import com.sun.javafx.accessible.utils.PropertyIds;
import javafx.scene.control.ListView;

public class AccessibleList extends AccessibleControl
                            implements SelectionProvider, GridProvider {

    ListView listView;
    
    /**
     * Constructor
     * 
     * @param listView the associated FX control
     */
    public AccessibleList(ListView listView) {
        super(listView);
        this.listView = listView ; 
       
    }

    /**
     * Gets a property value
     * 
     * @param propertyId
     * 
     * @return the requested property value 
     */
    @Override public Object getPropertyValue(int propertyId) {
        Object retVal = null ;
        switch(propertyId){
            case PropertyIds.NAME:
            case PropertyIds.DESCRIBED_BY:
                // return null for now
                break;
            case PropertyIds.CONTROL_TYPE:
                retVal = ControlTypeIds.LIST;
                break;
            case PropertyIds.IS_KEYBOARD_FOCUSABLE:
                retVal = listView.isFocusTraversable();
                break;
            case PropertyIds.HAS_KEYBOARD_FOCUS:
                retVal = listView.isFocused();
                break;
            case PropertyIds.IS_CONTROL_ELEMENT:
                retVal = true;
                break;
            case PropertyIds.IS_ENABLED:
                retVal = !listView.isDisabled();
                break;
            case PropertyIds.CLASS_NAME:
                retVal = this.getClass().toString();
                break;
            case PropertyIds.ORIENTATION:
                switch(listView.getOrientation()) {
                    case HORIZONTAL: 
                        retVal = OrientationType.OrientationType_Horizontal;
                        break;
                    case VERTICAL:
                        retVal = OrientationType.OrientationType_Vertical;
                        break;
                    default:
                        retVal = OrientationType.OrientationType_None;
                        break;
                }
                break;
            case PropertyIds.IS_OFFSCREEN:
                //TODO check if really offscreen - can be moved up to superclass 
                retVal = true;
                break;
        }   
        return retVal;
    }

    /**
     * Determine if the list supports multiple selection
     * 
     * @return whether or not the list supports multiple selection 
     */
    @Override
    public boolean canSelectMultiple() {
        return false;
    }

    /**
     * Get the Glass objects that are currently selected
     * 
     * @return return the Glass objects that are currently selected
     */
    @Override
    public Object[] getSelection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSelectionRequired() {
        return true;
    }


    /**
     * Get the number of columns in this list
     * 
     * @return the number of columns in this list 
     */
    @Override
    public int getColumnCount() {
        return 1;
    }

    /**
     * Get the number of rows in this list
     * 
     * @return the number of rows in this list
     */
    @Override
    public int getRowCount() {
        return listView.getItems().size();
    }

    /**
     * Gets the accessible Glass object of the specified item in the list.
     * 
     * @param row
     * @param col
     * 
     * @return the accessible Glass object of the specified item in the list
     */
    @Override   
    public Object getItem(int row, int col) {
        throw new UnsupportedOperationException();
    }

    
}
