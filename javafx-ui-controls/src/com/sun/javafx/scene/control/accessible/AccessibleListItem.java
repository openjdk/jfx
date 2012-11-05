/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.GridItemProvider;
import com.sun.javafx.accessible.providers.SelectionItemProvider;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ListView;

/**
 *
 * @author paru
 */
public class AccessibleListItem extends AccessibleControl implements 
        SelectionItemProvider, GridItemProvider {

    Cell listCell;
    static ListView listView = null;
     
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
     
    @Override
    public void addToSelection() {
        listView.getSelectionModel().select(listCell);
    }

    @Override
    public void removeFromSelection() {
        // TODO assuming single selection for now
        listView.getSelectionModel().clearSelection();
    }

    @Override
    public void select() {
        listView.getSelectionModel().select(listCell);
    }

    @Override
    public boolean isSelected() {
        return listCell == listView.getSelectionModel().getSelectedItem();
    }

    @Override
    public AccessibleProvider getSelectionContainer() {
        //return listView.impl_getAccessible();
        try {
            java.lang.reflect.Method method = listView.getClass().getMethod("impl_getAccessible");
            AccessibleProvider provider = (AccessibleProvider)method.invoke(listView);
            return provider ;
            } catch (Exception ex) {}
        return null;
    }

    @Override
    public int getRow() {
        return listView.getItems().indexOf(listCell);
    }

    @Override
    public int getColumn() {
        return 1;
    }

    @Override
    public int getRowSpan() {
        return listView.getItems().size();
       
    }

    @Override
    public int getColumnSpan() {
        return 1;
    }

    @Override
    public AccessibleProvider getContainingGrid() {
//        return listView.impl_getAccessible();
        try {
            java.lang.reflect.Method method = listView.getClass().getMethod("impl_getAccessible");
            AccessibleProvider provider = (AccessibleProvider)method.invoke(listView);
            return provider ;
        } catch (Exception ex) {}
        return null;
    }
    
}
