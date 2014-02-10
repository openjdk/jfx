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
package com.oracle.javafx.scenebuilder.kit.metadata.property;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 */
public abstract class ValuePropertyMetadata extends PropertyMetadata {
    
    private final boolean readWrite;
    private final InspectorPath inspectorPath;
    
    
    private final Map<Class<?>, Object> defaultValueAlternatives = new HashMap<>();

    public ValuePropertyMetadata(PropertyName name, boolean readWrite, InspectorPath inspectorPath) {
        super(name);
        this.readWrite = readWrite;
        this.inspectorPath = inspectorPath;
    }

    public boolean isReadWrite() {
        return readWrite;
    }


    public InspectorPath getInspectorPath() {
        return inspectorPath;
    }
    
    public abstract Class<?> getValueClass();
    public abstract Object getDefaultValueObject();
    public abstract Object getValueObject(FXOMInstance fxomInstance);
    public abstract void setValueObject(FXOMInstance fxomInstance, Object valueObject);
    
    public Map<Class<?>, Object> getDefaultValueAlternatives() {
        return defaultValueAlternatives;
    }
    
    
    /**
     * Returns true if getName().getResidenceClass() != null.
     * @return true if getName().getResidenceClass() != null.
     */
    public boolean isStaticProperty() {
        return getName().getResidenceClass() != null;
    }
    
    /**
     * Sets the property value in the scene graph object.
     * FXOM instance is unchanged. 
     * Value is lost at next scene graph reconstruction.
     * 
     * @param fxomInstance an fxom instance (never null)
     * @param value a value conform with the property typing
     */
    public void setValueInSceneGraphObject(FXOMInstance fxomInstance, Object value) {
        assert fxomInstance != null;
        assert fxomInstance.getSceneGraphObject() != null;
        getName().setValue(fxomInstance.getSceneGraphObject(), value);
    }
    
    /**
     * Gets the property value in the scene graph object.
     * Result might be different from getValueObject().
     * For example, if Button.text contains a resource key 'button-key'
     * and a resource bundle assign 'OK' to this key:
     *    - getValueObject() -> '%button-key'
     *    - getValueInSceneGraphObject() -> 'OK'
     * 
     * @param fxomInstance an fxom instance (never null)
     * @return value of this property in the scene graph object associated
     *         fxomInstance
     */
    public Object getValueInSceneGraphObject(FXOMInstance fxomInstance) {
        assert fxomInstance != null;
        return getName().getValue(fxomInstance.getSceneGraphObject());
    }
    
    /*
     * Object
     */
    
    @Override
    public int hashCode() {  // To please FindBugs
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {  // To please FindBugs
        if (obj == null) {
            return false;
        }
        if (PropertyMetadata.class != obj.getClass()) {
            return false;
        }
        
        return super.equals(obj);
    }
    
}
