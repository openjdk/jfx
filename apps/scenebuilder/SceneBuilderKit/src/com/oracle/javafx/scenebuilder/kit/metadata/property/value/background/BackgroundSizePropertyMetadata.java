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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.background;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.layout.BackgroundSize;

/**
 *
 */
public class BackgroundSizePropertyMetadata extends ComplexPropertyMetadata<BackgroundSize> {

    private final DoublePropertyMetadata widthMetadata
            = new DoublePropertyMetadata(new PropertyName("width"), 
            DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true, 
            BackgroundSize.DEFAULT.getWidth(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata heightMetadata
            = new DoublePropertyMetadata(new PropertyName("height"), 
            DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true, 
            BackgroundSize.DEFAULT.getHeight(), InspectorPath.UNUSED);
    
    private final BooleanPropertyMetadata widthAsPercentageMetadata
            = new BooleanPropertyMetadata(new PropertyName("widthAsPercentage"), 
            true /* readWrite */, BackgroundSize.DEFAULT.isWidthAsPercentage(), InspectorPath.UNUSED);
    private final BooleanPropertyMetadata heightAsPercentageMetadata
            = new BooleanPropertyMetadata(new PropertyName("heightAsPercentage"), 
            true /* readWrite */, BackgroundSize.DEFAULT.isHeightAsPercentage(), InspectorPath.UNUSED);
    private final BooleanPropertyMetadata containMetadata
            = new BooleanPropertyMetadata(new PropertyName("contain"), 
            true /* readWrite */, BackgroundSize.DEFAULT.isContain(), InspectorPath.UNUSED);
    private final BooleanPropertyMetadata coverMetadata
            = new BooleanPropertyMetadata(new PropertyName("cover"), 
            true /* readWrite */, BackgroundSize.DEFAULT.isCover(), InspectorPath.UNUSED);
    
    
    
    public BackgroundSizePropertyMetadata(PropertyName name, 
            boolean readWrite, BackgroundSize defaultValue, InspectorPath inspectorPath) {
        super(name, BackgroundSize.class, readWrite, defaultValue, inspectorPath);
    }
    
    
    /*
     * ComplexPropertyMetadata
     */
    @Override
    protected BackgroundSize castValue(Object value) {
        return (BackgroundSize) value;
    }

    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, BackgroundSize value) {
        widthMetadata.setValue(valueInstance, value.getWidth());
        heightMetadata.setValue(valueInstance, value.getHeight());
        widthAsPercentageMetadata.setValue(valueInstance, value.isWidthAsPercentage());
        heightAsPercentageMetadata.setValue(valueInstance, value.isHeightAsPercentage());
        containMetadata.setValue(valueInstance, value.isContain());
        coverMetadata.setValue(valueInstance, value.isCover());
    }
    
    
}
