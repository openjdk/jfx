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
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;

/**
 *
 */
public class BlendPropertyMetadata extends ComplexPropertyMetadata<Blend> {
    
    private final EffectPropertyMetadata bottomInputMetadata
            = new EffectPropertyMetadata(new PropertyName("bottomInput"), //NOI18N
            true /* readWrite */, null, InspectorPath.UNUSED);
    private final EffectPropertyMetadata topInputMetadata
            = new EffectPropertyMetadata(new PropertyName("topInput"), //NOI18N
            true /* readWrite */, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata opacityMetadata
            = new DoublePropertyMetadata(new PropertyName("opacity"), //NOI18N
            DoublePropertyMetadata.DoubleKind.OPACITY, true /* readWrite */, 1.0, InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata modeMetadata
            = new EnumerationPropertyMetadata(new PropertyName("mode"), //NOI18N
            BlendMode.class, true, BlendMode.SRC_OVER, InspectorPath.UNUSED);

    public BlendPropertyMetadata(PropertyName name, boolean readWrite, 
            Blend defaultValue, InspectorPath inspectorPath) {
        super(name, Blend.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(Blend value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        bottomInputMetadata.setValue(result, value.getBottomInput());
        topInputMetadata.setValue(result, value.getTopInput());
        opacityMetadata.setValue(result, value.getOpacity());
        modeMetadata.setValue(result, value.getMode().toString());
        
        return result;
    }
}
