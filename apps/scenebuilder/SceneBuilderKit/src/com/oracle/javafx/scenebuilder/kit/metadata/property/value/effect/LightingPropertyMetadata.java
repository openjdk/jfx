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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect.light.LightPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.Lighting;

/**
 *
 */
public class LightingPropertyMetadata extends ComplexPropertyMetadata<Lighting> {
    
    private static final Lighting LIGHTING_DEFAULT = new Lighting();
    
    private final EffectPropertyMetadata bumpInputMetadata
            = new EffectPropertyMetadata(new PropertyName("bumpInput"), //NOI18N
            true /* readWrite */, LIGHTING_DEFAULT.getBumpInput(), InspectorPath.UNUSED);
    private final EffectPropertyMetadata contentInputMetadata
            = new EffectPropertyMetadata(new PropertyName("contentInput"), //NOI18N
            true /* readWrite */, LIGHTING_DEFAULT.getContentInput(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata diffuseConstantMetadata
            = new DoublePropertyMetadata(new PropertyName("diffuseConstant"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 
            LIGHTING_DEFAULT.getDiffuseConstant(), InspectorPath.UNUSED);
    private final LightPropertyMetadata lightMetadata
            = new LightPropertyMetadata(new PropertyName("light"), //NOI18N
            true /* readWrite */, LIGHTING_DEFAULT.getLight(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata specularConstantMetadata
            = new DoublePropertyMetadata(new PropertyName("specularConstant"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 
            LIGHTING_DEFAULT.getSpecularConstant(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata specularExponentMetadata
            = new DoublePropertyMetadata(new PropertyName("specularExponent"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 
            LIGHTING_DEFAULT.getSpecularExponent(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata surfaceScaleMetadata
            = new DoublePropertyMetadata(new PropertyName("surfaceScale"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 
            LIGHTING_DEFAULT.getSurfaceScale(), InspectorPath.UNUSED);

    public LightingPropertyMetadata(PropertyName name, boolean readWrite, 
            Lighting defaultValue, InspectorPath inspectorPath) {
        super(name, Lighting.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(Lighting value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        bumpInputMetadata.setValue(result, value.getBumpInput());
        contentInputMetadata.setValue(result, value.getContentInput());
        diffuseConstantMetadata.setValue(result, value.getDiffuseConstant());
        lightMetadata.setValue(result, value.getLight());
        specularConstantMetadata.setValue(result, value.getSpecularConstant());
        specularExponentMetadata.setValue(result, value.getSpecularExponent());
        surfaceScaleMetadata.setValue(result, value.getSurfaceScale());

        return result;
    }
}
