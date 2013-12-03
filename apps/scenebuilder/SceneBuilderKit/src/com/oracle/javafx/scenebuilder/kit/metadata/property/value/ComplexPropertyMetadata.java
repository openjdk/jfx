/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

/**
 *
 * 
 */
public abstract class ComplexPropertyMetadata<T> extends SingleValuePropertyMetadata<T> {

    public ComplexPropertyMetadata(PropertyName name, Class<T> valueClass, 
            boolean readWrite, T defaultValue, InspectorPath inspectorPath) {
        super(name, valueClass, readWrite, defaultValue, inspectorPath);
    }
    
    /*
     * SingleValuePropertyMetadata
     */
    
    @Override
    public FXOMProperty makeFxomPropertyFromValue(FXOMInstance fxomInstance, T value) {
        assert fxomInstance != null;
        assert value != null;
        
        final FXOMDocument fxomDocument = fxomInstance.getFxomDocument();
        final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
        updateFxomInstanceWithValue(valueInstance, value);
        return new FXOMPropertyC(fxomDocument, getName(), valueInstance);
    }

    @Override
    protected void updateFxomPropertyWithValue(FXOMProperty fxomProperty, T value) {
        assert value != null;
        assert fxomProperty instanceof FXOMPropertyC; // Because it's *Complex*PropertyMetadata
        
        final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
        assert fxomPropertyC.getValues().size() == 1;
        
        FXOMObject valueObject = fxomPropertyC.getValues().get(0);
        if (valueObject instanceof FXOMInstance) {
            updateFxomInstanceWithValue((FXOMInstance) valueObject, value);
        } else {
            final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
            final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
            updateFxomInstanceWithValue(valueInstance, value);
            valueInstance.addToParentProperty(0, fxomPropertyC);
            valueObject.removeFromParentProperty();
        }
    }
    
    /*
     * To be subclassed
     */
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, T value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
