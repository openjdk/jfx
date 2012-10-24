/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author rrprasad
 */
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
