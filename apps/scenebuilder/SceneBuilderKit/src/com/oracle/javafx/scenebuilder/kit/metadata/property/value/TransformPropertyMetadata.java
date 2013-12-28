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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/**
 *
 */
public class TransformPropertyMetadata extends ComplexPropertyMetadata<Transform> {

    public TransformPropertyMetadata(PropertyName name, boolean readWrite, 
            Transform defaultValue, InspectorPath inspectorPath) {
        super(name, Transform.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    protected Transform castValue(Object value) {
        return (Transform) value;
    }
    
    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, Transform value) {
        
        if (value instanceof Affine) {
            updateFxomInstanceWithAffineValue(valueInstance, (Affine) value);
        } else if (value instanceof Rotate) {
            updateFxomInstanceWithRotateValue(valueInstance, (Rotate) value);
        } else if (value instanceof Scale) {
            throw new UnsupportedOperationException("To be implemented"); // TODO(elp)
        } else if (value instanceof Shear) {
            throw new UnsupportedOperationException("To be implemented"); // TODO(elp)
        } else if (value instanceof Translate) {
            throw new UnsupportedOperationException("To be implemented"); // TODO(elp)
        } else {
            throw new IllegalStateException("Unexpected Transform subclass: " + value);
        }
    }
    
    
    /*
     * Private
     */
    
    private final DoublePropertyMetadata mxxMetadata
            = new DoublePropertyMetadata(new PropertyName("mxx"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 1.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata mxyMetadata
            = new DoublePropertyMetadata(new PropertyName("mxy"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata mxzMetadata
            = new DoublePropertyMetadata(new PropertyName("mxz"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    
    private final DoublePropertyMetadata myxMetadata
            = new DoublePropertyMetadata(new PropertyName("myx"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata myyMetadata
            = new DoublePropertyMetadata(new PropertyName("myy"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 1.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata myzMetadata
            = new DoublePropertyMetadata(new PropertyName("myz"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    
    private final DoublePropertyMetadata mzxMetadata
            = new DoublePropertyMetadata(new PropertyName("mzx"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata mzyMetadata
            = new DoublePropertyMetadata(new PropertyName("mzy"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata mzzMetadata
            = new DoublePropertyMetadata(new PropertyName("mzz"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 1.0, InspectorPath.UNUSED);
    
    private final DoublePropertyMetadata txMetadata
            = new DoublePropertyMetadata(new PropertyName("tx"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata tyMetadata
            = new DoublePropertyMetadata(new PropertyName("ty"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata tzMetadata
            = new DoublePropertyMetadata(new PropertyName("tz"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);

    
    private void updateFxomInstanceWithAffineValue(FXOMInstance valueInstance, Affine value) {
        mxxMetadata.setValue(valueInstance, value.getMxx());
        mxyMetadata.setValue(valueInstance, value.getMxy());
        mxzMetadata.setValue(valueInstance, value.getMxz());
        
        myxMetadata.setValue(valueInstance, value.getMyx());
        myyMetadata.setValue(valueInstance, value.getMyy());
        myzMetadata.setValue(valueInstance, value.getMyz());
        
        mzxMetadata.setValue(valueInstance, value.getMzx());
        mzyMetadata.setValue(valueInstance, value.getMzy());
        mzzMetadata.setValue(valueInstance, value.getMzz());
        
        txMetadata.setValue(valueInstance, value.getTx());
        tyMetadata.setValue(valueInstance, value.getTy());
        tzMetadata.setValue(valueInstance, value.getTz());
    }
    
    private static final Rotate DEFAULT_ROTATE = new Rotate();
    
    private final DoublePropertyMetadata angleMetadata
            = new DoublePropertyMetadata(new PropertyName("angle"), 
            DoublePropertyMetadata.DoubleKind.ANGLE, true, DEFAULT_ROTATE.getAngle(), InspectorPath.UNUSED);
    private final Point3DPropertyMetadata axisMetadata
            = new Point3DPropertyMetadata(new PropertyName("axis"), 
            true, DEFAULT_ROTATE.getAxis(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata pivotXMetadata
            = new DoublePropertyMetadata(new PropertyName("pivotX"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, DEFAULT_ROTATE.getPivotX(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata pivotYMetadata
            = new DoublePropertyMetadata(new PropertyName("pivotY"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, DEFAULT_ROTATE.getPivotY(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata pivotZMetadata
            = new DoublePropertyMetadata(new PropertyName("pivotZ"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, DEFAULT_ROTATE.getPivotZ(), InspectorPath.UNUSED);
    
    private void updateFxomInstanceWithRotateValue(FXOMInstance valueInstance, Rotate value) {
        angleMetadata.setValue(valueInstance, value.getAngle());
        axisMetadata.setValue(valueInstance, value.getAxis());
        pivotXMetadata.setValue(valueInstance, value.getPivotX());
        pivotYMetadata.setValue(valueInstance, value.getPivotY());
        pivotZMetadata.setValue(valueInstance, value.getPivotZ());
    }
}
