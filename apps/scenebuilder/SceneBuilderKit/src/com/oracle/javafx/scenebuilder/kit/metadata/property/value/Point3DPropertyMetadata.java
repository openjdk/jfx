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
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.geometry.Point3D;

/**
 *
 */
public class Point3DPropertyMetadata extends ComplexPropertyMetadata<Point3D> {

    private final DoublePropertyMetadata xMetadata
            = new DoublePropertyMetadata(new PropertyName("x"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata yMetadata
            = new DoublePropertyMetadata(new PropertyName("y"),  //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata zMetadata
            = new DoublePropertyMetadata(new PropertyName("z"), //NOI18N
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);

    public Point3DPropertyMetadata(PropertyName name, boolean readWrite, 
            Point3D defaultValue, InspectorPath inspectorPath) {
        super(name, Point3D.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(Point3D value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        xMetadata.setValue(result, value.getX());
        yMetadata.setValue(result, value.getY());
        zMetadata.setValue(result, value.getZ());

        return result;
    }
    
}
