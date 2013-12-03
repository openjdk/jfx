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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.GradientEncoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;

/**
 *
 */
public class PaintPropertyMetadata extends ComplexPropertyMetadata<Paint> {

    private final ColorPropertyMetadata colorMetadata;
    private final ImagePatternPropertyMetadata imagePatternMetadata;
    private final LinearGradientPropertyMetadata linearGradientMetadata;
    private final RadialGradientPropertyMetadata radialGradientMetadata;

    public PaintPropertyMetadata(PropertyName name, boolean readWrite,
            Paint defaultValue, InspectorPath inspectorPath) {
        super(name, Paint.class, readWrite, defaultValue, inspectorPath);
        colorMetadata = new ColorPropertyMetadata(name, readWrite, null, inspectorPath);
        imagePatternMetadata = new ImagePatternPropertyMetadata(name, readWrite, null, inspectorPath);
        linearGradientMetadata = new LinearGradientPropertyMetadata(name, readWrite, null, inspectorPath);
        radialGradientMetadata = new RadialGradientPropertyMetadata(name, readWrite, null, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    @Override
    protected Paint castValue(Object value) {
        Paint result;

        if (value instanceof Paint) {
            result = (Paint) value;
        } else if (value instanceof String) {
            try {
                result = Paint.valueOf((String) value);
            } catch (IllegalArgumentException x) {
                result = Color.BLACK;
            }
        } else {
            assert value == null;
            result = null;
        }

        return result;
    }

    @Override
    protected void updateFxomPropertyWithValue(FXOMProperty fxomProperty, Paint value) {
        assert value != null;

        if (fxomProperty instanceof FXOMPropertyT) {
//            if (value instanceof Color) {
//                colorMetadata.updateFxomPropertyWithValue(fxomProperty, (Color) value);
//            } else if (value instanceof ImagePattern) {
//                imagePatternMetadata.updateFxomPropertyWithValue(fxomProperty, (ImagePattern) value);
//            } else if (value instanceof LinearGradient) {
//                linearGradientMetadata.updateFxomPropertyWithValue(fxomProperty, (LinearGradient) value);
//            } else {
//                assert value instanceof RadialGradient;
//                radialGradientMetadata.updateFxomPropertyWithValue(fxomProperty, (RadialGradient) value);
//            }
            /*
             * <CCCC property-name="#433444" />
             * <CCCC><property-name>#433444</property-name></CCCC>
             * <CCCC><property-name><String fx:value="#433444"/></CCCC>
             */
            final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
            if (value instanceof Color) {
                final Color color = (Color) value;
                fxomPropertyT.setValue(ColorEncoder.encodeColor(color));
            } else if (value instanceof LinearGradient) {
                final LinearGradient linear = (LinearGradient) value;
                fxomPropertyT.setValue(GradientEncoder.encodeLinearGradient(linear));
            } else if (value instanceof RadialGradient) {
                final RadialGradient radial = (RadialGradient) value;
                fxomPropertyT.setValue(GradientEncoder.encodeRadialGradient(radial));
            }
        } else {
            assert fxomProperty instanceof FXOMPropertyC;
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
            assert fxomPropertyC.getValues().size() == 1;

            FXOMObject valueObject = fxomPropertyC.getValues().get(0);
            if (valueObject instanceof FXOMInstance) {
                final FXOMInstance currentValueInstance = (FXOMInstance) valueObject;
                final Class<?> currentValueClass = currentValueInstance.getDeclaredClass();

                if (currentValueClass != value.getClass()) {
                    // Eg current value is a Color, new value is a RadialGradient
                    final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
                    final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
                    updateFxomInstanceWithValue(valueInstance, value);
                    valueInstance.addToParentProperty(0, fxomPropertyC);
                    valueObject.removeFromParentProperty();
                } else {
                    updateFxomInstanceWithValue(currentValueInstance, value);
                }
            } else {
                assert valueObject instanceof FXOMIntrinsic;

                final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
                final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
                updateFxomInstanceWithValue(valueInstance, value);
                valueInstance.addToParentProperty(0, fxomPropertyC);
                valueObject.removeFromParentProperty();
            }
        }
    }

    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, Paint value) {
        if (value instanceof Color) {
            colorMetadata.updateFxomInstanceWithValue(valueInstance, (Color) value);
        } else if (value instanceof ImagePattern) {
            imagePatternMetadata.updateFxomInstanceWithValue(valueInstance, (ImagePattern) value);
        } else if (value instanceof LinearGradient) {
            linearGradientMetadata.updateFxomInstanceWithValue(valueInstance, (LinearGradient) value);
        } else {
            assert value instanceof RadialGradient;
            radialGradientMetadata.updateFxomInstanceWithValue(valueInstance, (RadialGradient) value);
        }
    }

}
