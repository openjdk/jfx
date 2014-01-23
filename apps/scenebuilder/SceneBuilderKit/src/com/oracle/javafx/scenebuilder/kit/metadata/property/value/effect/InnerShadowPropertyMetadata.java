/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.ColorPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

/**
 *
 */
public class InnerShadowPropertyMetadata extends ComplexPropertyMetadata<InnerShadow> {
    
    private final EnumerationPropertyMetadata blurTypeMetadata
            = new EnumerationPropertyMetadata(new PropertyName("blurType"), //NOI18N
            BlurType.class, true, BlurType.THREE_PASS_BOX, InspectorPath.UNUSED);
    private final DoublePropertyMetadata chokeMetadata
            = new DoublePropertyMetadata(new PropertyName("choke"), //NOI18N
            DoublePropertyMetadata.DoubleKind.OPACITY, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final ColorPropertyMetadata colorMetadata
            = new ColorPropertyMetadata(new PropertyName("color"), //NOI18N
            true /* readWrite */, Color.BLACK, InspectorPath.UNUSED);
    private final DoublePropertyMetadata heightMetadata
            = new DoublePropertyMetadata(new PropertyName("height"), //NOI18N
            DoublePropertyMetadata.DoubleKind.SIZE, true /* readWrite */, 21.0, InspectorPath.UNUSED);
    private final EffectPropertyMetadata inputMetadata
            = new EffectPropertyMetadata(new PropertyName("input"), //NOI18N
            true /* readWrite */, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata offsetXMetadata
            = new DoublePropertyMetadata(new PropertyName("offsetX"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata offsetYMetadata
            = new DoublePropertyMetadata(new PropertyName("offsetY"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata radiusMetadata
            = new DoublePropertyMetadata(new PropertyName("radius"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 10.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata widthMetadata
            = new DoublePropertyMetadata(new PropertyName("width"), //NOI18N
            DoublePropertyMetadata.DoubleKind.OPACITY, true /* readWrite */, 21.0, InspectorPath.UNUSED);

    public InnerShadowPropertyMetadata(PropertyName name, boolean readWrite, 
            InnerShadow defaultValue, InspectorPath inspectorPath) {
        super(name, InnerShadow.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(InnerShadow value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        blurTypeMetadata.setValue(result, value.getBlurType().toString());
        chokeMetadata.setValue(result, value.getChoke());
        colorMetadata.setValue(result, value.getColor());
        heightMetadata.setValue(result, value.getHeight());
        inputMetadata.setValue(result, value.getInput());
        offsetXMetadata.setValue(result, value.getOffsetX());
        offsetYMetadata.setValue(result, value.getOffsetY());
        radiusMetadata.setValue(result, value.getRadius());
        widthMetadata.setValue(result, value.getWidth());

        return result;
    }
}
