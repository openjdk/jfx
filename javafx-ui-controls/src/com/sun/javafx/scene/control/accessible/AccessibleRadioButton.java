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

package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.SelectionItemProvider;
import javafx.scene.control.RadioButton;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;

public class AccessibleRadioButton extends AccessibleControl implements SelectionItemProvider {
    RadioButton radioButton ;
    public AccessibleRadioButton(RadioButton radioButton)
    {
        super(radioButton);
        this.radioButton = radioButton;
        // initialize to receive state change event
        radioButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                RadioButton radioButton = (RadioButton)t.getSource();
                boolean bOldVal, bCurrVal;
                bCurrVal = true;
                if( radioButton.isDisabled() ) 
                    bCurrVal = false;
                bOldVal = !bCurrVal;               
                firePropertyChange(EventIds.AUTOMATION_PROPERTY_CHANGED, bOldVal, bCurrVal);
            }
        });
    }

    //
    // Summary:
    //     Retrieves the value of a property supported by the UI Automation provider.
    //
    // Parameters:
    //   propertyId:
    //     The property identifier.
    //
    // Returns:
    //     The property value, or a null if the property is not supported by this provider,
    //     or System.Windows.Automation.AutomationElementIdentifiers.NotSupported if
    //     it is not supported at all.
    @Override
    public Object getPropertyValue(int propertyId)
    {
        Object retVal = null ;
        switch(propertyId){
            case PropertyIds.NAME:
            case PropertyIds.DESCRIBED_BY:
                retVal = (Object)radioButton.getText() ;
                break;
            case PropertyIds.CONTROL_TYPE:
                retVal = ControlTypeIds.RADIO_BUTTON;
                break;
            case PropertyIds.IS_KEYBOARD_FOCUSABLE:
                retVal = radioButton.isFocusTraversable();
                break;
            case PropertyIds.HAS_KEYBOARD_FOCUS:
                retVal = radioButton.isFocused();
                break;
            case PropertyIds.IS_CONTROL_ELEMENT:
                retVal = true;
                break;
            case PropertyIds.IS_ENABLED:
                retVal = !radioButton.isDisabled();
                break;
            case PropertyIds.CLASS_NAME:
                retVal = this.getClass().toString();
                break;
        }
        return retVal;
    }

    // Summary:
    //     Retrieves an object that provides support for a control pattern on a UI Automation
    //     element.
    //
    // Parameters:
    //   patternId:
    //     Identifier of the pattern.
    //
    // Returns:
    //     Object that implements the pattern interface, or null if the pattern is not
    //     supported.
    @Override
    public Object getPatternProvider(int patternId)
    {
        return (Object)super.getAccessibleElement() ;
    }


    @Override
    public void addToSelection()
    {
    }
    
    @Override
    public void removeFromSelection()
    {
        
    }

    @Override
    public void select()
    {
        
    }
    
    @Override
    public boolean isSelected()
    {
        return radioButton.isSelected();
    }
    
    @Override
    public AccessibleProvider getSelectionContainer()
    {
        return null;
    }
    
}
