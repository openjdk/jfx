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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.StopListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.List;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 *
 */
public class RadialGradientPropertyMetadata extends ComplexPropertyMetadata<RadialGradient> {

    private static final List<Stop> DEFAULT_STOPS 
            = new RadialGradient(0.0, 1.0, 0.0, 0.0, 1.0,
            true /* proportional */, CycleMethod.NO_CYCLE).getStops();
    
    private final DoublePropertyMetadata focusAngleMetadata
            = new DoublePropertyMetadata(new PropertyName("focusAngle"), 
            DoublePropertyMetadata.DoubleKind.ANGLE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata focusDistanceMetadata
            = new DoublePropertyMetadata(new PropertyName("focusDistance"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata centerXMetadata
            = new DoublePropertyMetadata(new PropertyName("centerX"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata centerYMetadata
            = new DoublePropertyMetadata(new PropertyName("centerY"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata radiusMetadata
            = new DoublePropertyMetadata(new PropertyName("radius"), 
            DoublePropertyMetadata.DoubleKind.SIZE, true, 0.0, InspectorPath.UNUSED);
    private final BooleanPropertyMetadata proportionalMetadata
            = new BooleanPropertyMetadata(new PropertyName("proportional"), 
            true, true, InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata cycleMethodMetadata
            = new EnumerationPropertyMetadata(new PropertyName("cycleMethod"),
            CycleMethod.class, true, CycleMethod.NO_CYCLE, InspectorPath.UNUSED);
    private final StopListPropertyMetadata stopsMetadata
            = new StopListPropertyMetadata(new PropertyName("stops"),
            true, DEFAULT_STOPS, InspectorPath.UNUSED);

    public RadialGradientPropertyMetadata(PropertyName name, boolean readWrite, 
            RadialGradient defaultValue, InspectorPath inspectorPath) {
        super(name, RadialGradient.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(RadialGradient value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        focusAngleMetadata.setValue(result, value.getFocusAngle());
        focusDistanceMetadata.setValue(result, value.getFocusDistance());
        centerXMetadata.setValue(result, value.getCenterX());
        centerYMetadata.setValue(result, value.getCenterY());
        radiusMetadata.setValue(result, value.getRadius());
        proportionalMetadata.setValue(result, value.isProportional());
        cycleMethodMetadata.setValue(result, value.getCycleMethod().toString());
        stopsMetadata.setValue(result, value.getStops());

        return result;
    }
    
}
