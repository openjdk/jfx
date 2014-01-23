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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignImage;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.image.Image;

/**
 *
 * 
 */
public class ImagePropertyMetadata extends ComplexPropertyMetadata<DesignImage> {

    private final StringPropertyMetadata urlMetadata
            = new StringPropertyMetadata(new PropertyName("url"), 
            true, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata requestedWidthMetadata
            = new DoublePropertyMetadata(new PropertyName("requestedWidth"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata requestedHeightMetadata
            = new DoublePropertyMetadata(new PropertyName("requestedHeight"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata preserveRatioMetadata
            = new BooleanPropertyMetadata(new PropertyName("preserveRatio"),
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata smoothMetadata
            = new BooleanPropertyMetadata(new PropertyName("smooth"),
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata backgroundLoading
            = new BooleanPropertyMetadata(new PropertyName("backgroundLoading"),
            true /* readWrite */, false /* defaultValue */, InspectorPath.UNUSED);
    
    public ImagePropertyMetadata(PropertyName name, boolean readWrite, 
            DesignImage defaultValue, InspectorPath inspectorPath) {
        super(name, DesignImage.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(DesignImage value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, Image.class);
        
        urlMetadata.setValue(result, value.getLocation());
        requestedWidthMetadata.setValue(result, value.getImage().getRequestedWidth());
        requestedHeightMetadata.setValue(result, value.getImage().getRequestedHeight());
        preserveRatioMetadata.setValue(result, value.getImage().isPreserveRatio());
        smoothMetadata.setValue(result, value.getImage().isSmooth());
        backgroundLoading.setValue(result, value.getImage().isBackgroundLoading());

        return result;
    }
    
    @Override
    public DesignImage makeValueFromFxomInstance(FXOMInstance valueFxomInstance) {
        final String location = urlMetadata.getValue(valueFxomInstance);
        final Image image = (Image)valueFxomInstance.getSceneGraphObject();
        return new DesignImage(image, location);
    }
}
