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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * 
 */
public class EnumerationPropertyMetadata extends ValuePropertyMetadata {
    
    private final Class<?> enumClass;
    private final Enum<?> defaultValue;

    public EnumerationPropertyMetadata(PropertyName name, Class<?> enumClass,
            boolean readWrite, Enum<?> defaultValue, InspectorPath inspectorPath) {
        super(name, readWrite, inspectorPath);
        assert enumClass.isEnum();
        this.enumClass = enumClass;
        this.defaultValue = defaultValue;
    }
    
    public String getValue(FXOMInstance fxomInstance) {
        final String result;
        
        if (isReadWrite()) {
            final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());
            if (fxomProperty == null) {
                // propertyName is not specified in the fxom instance.
                // We return the default value specified in the metadata of the
                // property
                if (defaultValue == null) {
                    result = getValueClass().getEnumConstants()[0].toString();
                } else {
                    result = defaultValue.toString();
                }
            } else {
                assert fxomProperty instanceof FXOMPropertyT;
                final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
                result = fxomPropertyT.getValue();
            }
        } else {
            final Object o = getName().getValue(fxomInstance.getSceneGraphObject());
            if (o == null) {
                result = getValueClass().getEnumConstants()[0].toString();
            } else {
                assert o.getClass() == enumClass;
                result = o.toString();
            }
        }
        
        return result;
    }

    public void setValue(FXOMInstance fxomInstance, String value) {
        assert isReadWrite();
        
        final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());
        if (fxomProperty == null) {
            // propertyName is not specified in the fxom instance.
            if (value != null) {
                // We insert a new fxom property
                final FXOMPropertyT newProperty 
                        = new FXOMPropertyT(fxomInstance.getFxomDocument(),
                        getName(), value);
                newProperty.addToParentInstance(-1, fxomInstance);
            }
        } else {
            assert fxomProperty instanceof FXOMPropertyT;
            final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
            if (value != null) {
                fxomPropertyT.setValue(value);
            } else {
                assert defaultValue == null;
                fxomPropertyT.removeFromParentInstance();
            }
        }
    }
    
    public List<String> getValidValues() {
        final List<String> result = new ArrayList<>();
        
        for (Object e : enumClass.getEnumConstants()) {
            result.add(e.toString());
        }
        
        return result;
    }
    
    /*
     * ValuePropertyMetadata
     */
    
    @Override
    public Class<?> getValueClass() {
        return enumClass;
    }

    @Override
    public Object getDefaultValueObject() {
        return (defaultValue == null) ? null : defaultValue.toString();
    }

    @Override
    public Object getValueObject(FXOMInstance fxomInstance) {
        return getValue(fxomInstance);
    }

    @Override
    public void setValueObject(FXOMInstance fxomInstance, Object valueObject) {
        assert valueObject instanceof String;
        setValue(fxomInstance, (String) valueObject);
    }
}
