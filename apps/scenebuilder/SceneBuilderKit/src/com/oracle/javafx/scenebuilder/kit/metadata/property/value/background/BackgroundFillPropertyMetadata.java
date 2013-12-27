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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.InsetsPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.PaintPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.geometry.Insets;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;

/**
 *
 */
public class BackgroundFillPropertyMetadata extends ComplexPropertyMetadata<BackgroundFill> {

    private final PaintPropertyMetadata fillMetadata
            = new PaintPropertyMetadata(new PropertyName("fill"), 
            true /* readWrite */, null /* defaultValue */, InspectorPath.UNUSED);
    private final InsetsPropertyMetadata insetsMetadata
            = new InsetsPropertyMetadata(new PropertyName("insets"), 
            true /* readWrite */, Insets.EMPTY, InspectorPath.UNUSED);
    private final CornerRadiiPropertyMetadata radiiMetadata
            = new CornerRadiiPropertyMetadata(new PropertyName("radii"), 
            true /* readWrite */, CornerRadii.EMPTY, InspectorPath.UNUSED);
            
    public BackgroundFillPropertyMetadata(PropertyName name, 
            boolean readWrite, BackgroundFill defaultValue, InspectorPath inspectorPath) {
        super(name, BackgroundFill.class, readWrite, defaultValue, inspectorPath);
    }
    
    
    /*
     * ComplexPropertyMetadata
     */
    @Override
    protected BackgroundFill castValue(Object value) {
        return (BackgroundFill) value;
    }
    
    @Override
    public void updateFxomInstanceWithValue(FXOMInstance valueInstance, BackgroundFill value) {
        fillMetadata.setValue(valueInstance, value.getFill());
        insetsMetadata.setValue(valueInstance, value.getInsets());
        radiiMetadata.setValue(valueInstance, value.getRadii());
    }
    

}
