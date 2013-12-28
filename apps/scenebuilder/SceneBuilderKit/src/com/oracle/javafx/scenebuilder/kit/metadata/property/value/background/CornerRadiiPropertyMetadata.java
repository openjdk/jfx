/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.background;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.layout.CornerRadii;

/**
 *
 */
public class CornerRadiiPropertyMetadata extends ComplexPropertyMetadata<CornerRadii> {

    private final DoublePropertyMetadata topLeftHorizontalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("topLeftHorizontalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata topLeftVerticalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("topLeftVerticalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata topRightVerticalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("topRightVerticalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata topRightHorizontalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("topRightHorizontalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata bottomRightHorizontalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("bottomRightHorizontalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata bottomRightVerticalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("bottomRightVerticalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata bottomLeftVerticalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("bottomLeftVerticalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata bottomLeftHorizontalRadiusMetadata
            = new DoublePropertyMetadata(new PropertyName("bottomLeftHorizontalRadius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    
    private final BooleanPropertyMetadata topLeftHorizontalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("topLeftHorizontalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata topLeftVerticalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("topLeftVerticalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata topRightVerticalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("topRightVerticalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata topRightHorizontalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("topRightHorizontalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata bottomRightHorizontalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("bottomRightHorizontalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata bottomRightVerticalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("bottomRightVerticalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata bottomLeftVerticalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("bottomLeftVerticalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata bottomLeftHorizontalRadiusAsPercentMetadata
            = new BooleanPropertyMetadata(new PropertyName("bottomLeftHorizontalRadiusAsPercent"), 
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    

    public CornerRadiiPropertyMetadata(PropertyName name, 
            boolean readWrite, CornerRadii defaultValue, InspectorPath inspectorPath) {
        super(name, CornerRadii.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    protected CornerRadii castValue(Object value) {
        return (CornerRadii) value;
    }
    
    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, CornerRadii value) {
        topLeftHorizontalRadiusMetadata.setValue(valueInstance, value.getTopLeftHorizontalRadius());
        topLeftVerticalRadiusMetadata.setValue(valueInstance, value.getTopLeftVerticalRadius());
        topRightHorizontalRadiusMetadata.setValue(valueInstance, value.getTopRightHorizontalRadius());
        topRightVerticalRadiusMetadata.setValue(valueInstance, value.getTopRightVerticalRadius());
        bottomRightHorizontalRadiusMetadata.setValue(valueInstance, value.getBottomRightHorizontalRadius());
        bottomRightVerticalRadiusMetadata.setValue(valueInstance, value.getBottomRightVerticalRadius());
        bottomLeftVerticalRadiusMetadata.setValue(valueInstance, value.getBottomLeftVerticalRadius());
        bottomLeftHorizontalRadiusMetadata.setValue(valueInstance, value.getBottomLeftHorizontalRadius());
        
        topLeftHorizontalRadiusAsPercentMetadata.setValue(valueInstance, value.isTopLeftHorizontalRadiusAsPercentage());
        topLeftVerticalRadiusAsPercentMetadata.setValue(valueInstance, value.isTopLeftVerticalRadiusAsPercentage());
        topRightVerticalRadiusAsPercentMetadata.setValue(valueInstance, value.isTopRightVerticalRadiusAsPercentage());
        topRightHorizontalRadiusAsPercentMetadata.setValue(valueInstance, value.isTopRightHorizontalRadiusAsPercentage());
        bottomRightHorizontalRadiusAsPercentMetadata.setValue(valueInstance, value.isBottomRightHorizontalRadiusAsPercentage());
        bottomRightVerticalRadiusAsPercentMetadata.setValue(valueInstance, value.isBottomRightVerticalRadiusAsPercentage());
        bottomLeftVerticalRadiusAsPercentMetadata.setValue(valueInstance, value.isBottomLeftVerticalRadiusAsPercentage());
        bottomLeftHorizontalRadiusAsPercentMetadata.setValue(valueInstance, value.isBottomLeftHorizontalRadiusAsPercentage());
    }
}
