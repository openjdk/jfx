/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.providers.RangeValueProvider;
import javafx.scene.control.Slider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
/**
 *
 * @author paru
 */
public class AccessibleSlider extends AccessibleControl implements RangeValueProvider {
    
    Slider slider;
     public AccessibleSlider(Slider slider) {
        super(slider);
        this.slider = slider ; 
        slider.valueProperty().addListener(new ChangeListener<Number>() {
             @Override public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                 firePropertyChange(EventIds.AUTOMATION_PROPERTY_CHANGED, t.intValue(), t1.intValue());
             }
         });
    }
    
    @Override public double getValue() {
        return slider.getValue();
    }

    @Override public boolean isReadOnly() {
        return false;
    }

    @Override public double getLargeValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public double getMinimum() {
        return slider.getMin();
    }

    @Override public double getMaximum() {
        return slider.getMax();
    }

    @Override public double getSmallChange() {
        // Need to store value, to find the difference with the current value
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public Object getPropertyValue(int propertyId) {
        Object retVal = null ;
        switch(propertyId){
            case PropertyIds.NAME:
            case PropertyIds.DESCRIBED_BY:
                retVal = new Double(slider.getValue()).toString();
                break;
            case PropertyIds.CONTROL_TYPE:
                retVal = ControlTypeIds.SLIDER;
                break;
            case PropertyIds.IS_KEYBOARD_FOCUSABLE:
                retVal = slider.isFocusTraversable();
                break;
            case PropertyIds.HAS_KEYBOARD_FOCUS:
                retVal = slider.isFocused();
                break;
            case PropertyIds.IS_CONTROL_ELEMENT:
                retVal = true;
                break;
            case PropertyIds.IS_ENABLED:
                retVal = !slider.isDisabled();
                break;
            case PropertyIds.CLASS_NAME:
                retVal = this.getClass().toString();
                break;
        }
        return retVal;
    }
    
     @Override
    public Object getPatternProvider(int patternId) {
        return (Object)super.getAccessibleElement() ;
    }

}
