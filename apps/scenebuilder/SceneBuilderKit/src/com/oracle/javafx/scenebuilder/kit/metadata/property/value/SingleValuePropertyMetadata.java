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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.Objects;

/**
 *
 */
public abstract class SingleValuePropertyMetadata<T> extends ValuePropertyMetadata {
    
    private final Class<T> valueClass;
    private final T defaultValue;

    public SingleValuePropertyMetadata(PropertyName name, Class<T> valueClass, 
            boolean readWrite, T defaultValue, InspectorPath inspectorPath) {
        super(name, readWrite, inspectorPath);
        this.defaultValue = defaultValue;
        this.valueClass = valueClass;
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
    
    public T getValue(FXOMInstance fxomInstance) {
        final T result;
        
        if (isReadWrite()) {
            final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());
            if (fxomProperty == null) {
                // propertyName is not specified in the fxom instance.
                // We return the default value specified in the metadata of the
                // property
                result = defaultValue;
            } else if (fxomProperty instanceof FXOMPropertyT) {
                final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
                final PrefixedValue pv = new PrefixedValue(fxomPropertyT.getValue());
                if (pv.isBindingExpression()) {
                    result = getDefaultValue();
                } else {
                    result = makeValueFromProperty(fxomPropertyT);
                }
            } else if (fxomProperty instanceof FXOMPropertyC) {
                final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
                assert fxomPropertyC.getValues().isEmpty() == false;
                final FXOMObject firstValue = fxomPropertyC.getValues().get(0);
                if (firstValue instanceof FXOMInstance) {
                    result = makeValueFromFxomInstance((FXOMInstance) firstValue);
                } else {
                    result = getDefaultValue();
                }
            } else {
                assert false;
                result = defaultValue;
            }
        } else {
            result = valueClass.cast(getName().getValue(fxomInstance.getSceneGraphObject()));
        }
        
        return result;
    }

    public void setValue(FXOMInstance fxomInstance, T value) {
        assert isReadWrite();
        
        final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());

        if (Objects.equals(value, getDefaultValueObject())) {
            // We must remove the fxom property if any
            if (fxomProperty != null) {
                fxomProperty.removeFromParentInstance();
            }
        } else {
            final FXOMDocument fxomDocument = fxomInstance.getFxomDocument();
            final FXOMProperty newProperty;
            if (canMakeStringFromValue(value)) {
                final String valueString = makeStringFromValue(value);
                newProperty = new FXOMPropertyT(fxomDocument, getName(), valueString);
            } else {
                final FXOMInstance valueInstance = makeFxomInstanceFromValue(value, fxomDocument);
                newProperty = new FXOMPropertyC(fxomDocument, getName(), valueInstance);
            }
            FXOMNodes.updateProperty(fxomInstance, newProperty);
        }
    }
    
    public abstract T makeValueFromString(String string);
    public abstract T makeValueFromFxomInstance(FXOMInstance valueFxomInstance);
    public abstract boolean canMakeStringFromValue(T value);
    public abstract String makeStringFromValue(T value);
    public abstract FXOMInstance makeFxomInstanceFromValue(T value, FXOMDocument fxomDocument);
    
    /* This routine should become abstract and replace makeValueFromString(). */
    public T makeValueFromProperty(FXOMPropertyT fxomProperty) {
        return makeValueFromString(fxomProperty.getValue());
    }
    
    /*
     * ValuePropertyMetadata
     */
    @Override
    public Class<? extends T> getValueClass() {
        return valueClass;
    }

    @Override
    public Object getDefaultValueObject() {
        return defaultValue;
    }

    @Override
    public Object getValueObject(FXOMInstance fxomInstance) {
        return getValue(fxomInstance);
    }

    @Override
    public void setValueObject(FXOMInstance fxomInstance, Object valueObject) {
        setValue(fxomInstance, valueClass.cast(valueObject));
    }
}
