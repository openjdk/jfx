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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.DisplacementMap;

/**
 *
 */
public class DisplacementMapPropertyMetadata extends ComplexPropertyMetadata<DisplacementMap> {
    
    private final DisplacementMap DEFAULT = new DisplacementMap();
    
    private final EffectPropertyMetadata inputMetadata
            = new EffectPropertyMetadata(new PropertyName("input"), //NOI18N
            true /* readWrite */, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata offsetXMetadata
            = new DoublePropertyMetadata(new PropertyName("offsetX"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, DEFAULT.getOffsetX(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata offsetYMetadata
            = new DoublePropertyMetadata(new PropertyName("offsetY"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, DEFAULT.getOffsetY(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata scaleXMetadata
            = new DoublePropertyMetadata(new PropertyName("scaleX"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, DEFAULT.getScaleX(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata scaleYMetadata
            = new DoublePropertyMetadata(new PropertyName("scaleY"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, DEFAULT.getScaleY(), InspectorPath.UNUSED);
    private final BooleanPropertyMetadata wrapMetadata
            = new BooleanPropertyMetadata(new PropertyName("wrap"), //NOI18N
            true /* readWrite */, DEFAULT.isWrap(), InspectorPath.UNUSED);
    private final FloatMapPropertyMetadata mapDataMetadata
            = new FloatMapPropertyMetadata(new PropertyName("mapData"), //NOI18N
            true /* readWrite */, DEFAULT.getMapData(), InspectorPath.UNUSED);

    public DisplacementMapPropertyMetadata(PropertyName name, boolean readWrite, 
            DisplacementMap defaultValue, InspectorPath inspectorPath) {
        super(name, DisplacementMap.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(DisplacementMap value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        inputMetadata.setValue(result, value.getInput());
        offsetXMetadata.setValue(result, value.getOffsetX());
        offsetYMetadata.setValue(result, value.getOffsetY());
        scaleXMetadata.setValue(result, value.getScaleX());
        scaleYMetadata.setValue(result, value.getScaleY());
        wrapMetadata.setValue(result, value.isWrap());
        mapDataMetadata.setValue(result, value.getMapData());

        return result;
    }
}
