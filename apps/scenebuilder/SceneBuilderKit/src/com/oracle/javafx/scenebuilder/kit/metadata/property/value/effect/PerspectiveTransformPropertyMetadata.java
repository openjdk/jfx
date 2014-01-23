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
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.PerspectiveTransform;

/**
 *
 */
public class PerspectiveTransformPropertyMetadata extends ComplexPropertyMetadata<PerspectiveTransform> {
    
    private final EffectPropertyMetadata inputMetadata
            = new EffectPropertyMetadata(new PropertyName("input"), //NOI18N
            true /* readWrite */, null, InspectorPath.UNUSED);
    private final DoublePropertyMetadata llxMetadata
            = new DoublePropertyMetadata(new PropertyName("llx"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata llyMetadata
            = new DoublePropertyMetadata(new PropertyName("lly"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata lrxMetadata
            = new DoublePropertyMetadata(new PropertyName("lrx"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata lryMetadata
            = new DoublePropertyMetadata(new PropertyName("lry"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata ulxMetadata
            = new DoublePropertyMetadata(new PropertyName("ulx"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata ulyMetadata
            = new DoublePropertyMetadata(new PropertyName("uly"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata urxMetadata
            = new DoublePropertyMetadata(new PropertyName("urx"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata uryMetadata
            = new DoublePropertyMetadata(new PropertyName("ury"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true /* readWrite */, 0.0, InspectorPath.UNUSED);

    public PerspectiveTransformPropertyMetadata(PropertyName name, boolean readWrite, 
            PerspectiveTransform defaultValue, InspectorPath inspectorPath) {
        super(name, PerspectiveTransform.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(PerspectiveTransform value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        inputMetadata.setValue(result, value.getInput());
        llxMetadata.setValue(result, value.getLlx());
        llyMetadata.setValue(result, value.getLly());
        lrxMetadata.setValue(result, value.getLrx());
        lryMetadata.setValue(result, value.getLry());
        ulxMetadata.setValue(result, value.getUlx());
        ulyMetadata.setValue(result, value.getUly());
        urxMetadata.setValue(result, value.getUrx());
        uryMetadata.setValue(result, value.getUry());

        return result;
    }
}
