/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.TextEncodablePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.paint.Color;

/**
 *
 */
public class ColorPropertyMetadata extends TextEncodablePropertyMetadata<Color> {

    private final DoublePropertyMetadata redMetadata
            = new DoublePropertyMetadata(new PropertyName("red"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata greenMetadata
            = new DoublePropertyMetadata(new PropertyName("green"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata blueMetadata
            = new DoublePropertyMetadata(new PropertyName("blue"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata opacityMetadata
            = new DoublePropertyMetadata(new PropertyName("opacity"), 
            DoublePropertyMetadata.DoubleKind.OPACITY, true, 1.0, InspectorPath.UNUSED);

    public ColorPropertyMetadata(PropertyName name, boolean readWrite, 
            Color defaultValue, InspectorPath inspectorPath) {
        super(name, Color.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ValuePropertyMetadata
     */
    
    @Override
    protected Color castValue(Object value) {
        final Color result;
        
        if (value instanceof Color) {
            result = (Color) value;
        } else {
            assert value instanceof String;
            result = Color.valueOf((String)value);
        }
        
        return result;
    }

    /*
     * TextEncodablePropertyMetadata
     */
    
    @Override
    public FXOMProperty makeFxomPropertyFromValue(FXOMInstance fxomInstance, Color value) {
        return new FXOMPropertyT(fxomInstance.getFxomDocument(), 
                getName(), ColorEncoder.encodeColor(value));
    }

    @Override
    protected void updateFxomPropertyWithValue(FXOMProperty fxomProperty, Color value) {
        
        if (fxomProperty instanceof FXOMPropertyT) {
            /*
             * <CCCC property-name="#433444" />
             * <CCCC><property-name>#433444</property-name></CCCC>
             * <CCCC><property-name><String fx:value="#433444"/></CCCC>
             */
            final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
            fxomPropertyT.setValue(ColorEncoder.encodeColor(value));
        } else {
            assert fxomProperty instanceof FXOMPropertyC;
            
            /*
             * <CCCC>
             *      <property-name>
             *          <Color red="43" green="34" blue="44" opacity="0.5"/>
             *      </property-name>
             * </CCCC>
             */
            
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
            assert fxomPropertyC.getValues().size() == 1;

            FXOMObject colorFxomObject = fxomPropertyC.getValues().get(0);
            if (colorFxomObject instanceof FXOMInstance) {
                updateFxomInstanceWithValue((FXOMInstance) colorFxomObject, value);
            } else {
                final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
                final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
                updateFxomInstanceWithValue(valueInstance, value);
                valueInstance.addToParentProperty(0, fxomPropertyC);
                colorFxomObject.removeFromParentProperty();
            }
        }
    }
    
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, Color value) {
        redMetadata.setValue(valueInstance, value.getRed());
        greenMetadata.setValue(valueInstance, value.getGreen());
        blueMetadata.setValue(valueInstance, value.getBlue());
        opacityMetadata.setValue(valueInstance, value.getOpacity());
    }
    
}
