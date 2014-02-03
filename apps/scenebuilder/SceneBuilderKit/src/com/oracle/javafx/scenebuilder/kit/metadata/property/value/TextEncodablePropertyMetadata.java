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

/**
 *
 */
public abstract class TextEncodablePropertyMetadata<T> extends SingleValuePropertyMetadata<T> {

    public TextEncodablePropertyMetadata(PropertyName name, Class<T> valueClass, 
            boolean readWrite, T defaultValue, InspectorPath inspectorPath) {
        super(name, valueClass, readWrite, defaultValue, inspectorPath);
    }
    
    public String getValueString(FXOMInstance fxomInstance) {
        return getValue(fxomInstance).toString();
    }

    /*
     * SingleValuePropertyMetadata
     */
    
    @Override
    public T makeValueFromFxomInstance(FXOMInstance valueFxomInstance) {
        return getValueClass().cast(valueFxomInstance.getSceneGraphObject());
    }

    @Override
    public boolean canMakeStringFromValue(T value) {
        return true;
    }

    @Override
    public String makeStringFromValue(T value) {
        assert value != null;
        return value.toString();
    }

    @Override
    public FXOMInstance makeFxomInstanceFromValue(T value, FXOMDocument fxomDocument) {
        throw new RuntimeException("Bug"); //NOI18N
        // Should never be invoked because canMakeStringFromValue() always return true
    }
 }
