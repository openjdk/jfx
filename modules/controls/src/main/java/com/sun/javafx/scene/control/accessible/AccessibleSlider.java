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

import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.providers.RangeValueProvider;
import javafx.scene.control.Slider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

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
