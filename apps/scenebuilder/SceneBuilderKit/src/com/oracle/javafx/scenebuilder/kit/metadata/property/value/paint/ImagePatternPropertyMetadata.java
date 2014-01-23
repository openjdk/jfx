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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ImagePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignImage;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.paint.ImagePattern;

/**
 *
 */
public class ImagePatternPropertyMetadata extends ComplexPropertyMetadata<ImagePattern> {

    private final ImagePropertyMetadata imageMetadata
            = new ImagePropertyMetadata(new PropertyName("image"), 
            true, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata xMetadata
            = new DoublePropertyMetadata(new PropertyName("x"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata yMetadata
            = new DoublePropertyMetadata(new PropertyName("y"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata widthMetadata
            = new DoublePropertyMetadata(new PropertyName("width"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata heightMetadata
            = new DoublePropertyMetadata(new PropertyName("height"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata proportionalMetadata
            = new BooleanPropertyMetadata(new PropertyName("proportional"), 
            true, true, InspectorPath.UNUSED);

    public ImagePatternPropertyMetadata(PropertyName name, boolean readWrite, 
            ImagePattern defaultValue, InspectorPath inspectorPath) {
        super(name, ImagePattern.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(ImagePattern value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        imageMetadata.setValue(result, new DesignImage(value.getImage()));
        xMetadata.setValue(result, value.getX());
        yMetadata.setValue(result, value.getY());
        widthMetadata.setValue(result, value.getWidth());
        heightMetadata.setValue(result, value.getHeight());
        proportionalMetadata.setValue(result, value.isProportional());
        
        return result;
    }
    
}
