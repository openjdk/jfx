/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.accessible;

import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import javafx.scene.text.Text;

public class AccessibleText extends AccessibleNode {
    Text text;
    public AccessibleText(Text text) {
        super(text);
        this.text = text ;       
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
                retVal = (Object)this.text.getText() ;
                break;
            case PropertyIds.CONTROL_TYPE:
                retVal = ControlTypeIds.TEXT;
                break;
            case PropertyIds.IS_KEYBOARD_FOCUSABLE:
                retVal = true;
                break;
            case PropertyIds.HAS_KEYBOARD_FOCUS:
                retVal = text.isFocused();
                break;
            case PropertyIds.IS_CONTROL_ELEMENT:
                retVal = true;
                break;
            case PropertyIds.IS_ENABLED:
                retVal = !text.isDisabled();
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
        return null ;
    }

}
