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
package com.oracle.javafx.scenebuilder.kit.fxom;

import com.oracle.javafx.scenebuilder.kit.fxom.glue.GlueElement;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * 
 */
public class FXOMIntrinsic extends FXOMObject {
    
    public enum Type {
        FX_INCLUDE,
        FX_REFERENCE,
        FX_COPY,
        UNDEFINED
    }
    
    private final Map<PropertyName, FXOMProperty> properties = new TreeMap<>();
    private Object sourceSceneGraphObject;

    
    FXOMIntrinsic(FXOMDocument document, GlueElement glueElement, Object targetSceneGraphObject) {
        super(document, glueElement, null);
        this.sourceSceneGraphObject = targetSceneGraphObject;
    }
    
    FXOMIntrinsic(FXOMDocument document, Type type, String source) {
        super(document, makeTagNameFromType(type));
        getGlueElement().getAttributes().put("source", source);
    }

    public Type getType() {
        final Type result;
        
        switch(getGlueElement().getTagName()) {
            case "fx:include":
                result = Type.FX_INCLUDE;
                break;
            case "fx:reference":
                result = Type.FX_REFERENCE;
                break;
            case "fx:copy":
                result = Type.FX_COPY;
                break;
            default:
                result = Type.UNDEFINED;
                break;
        }
        
        return result;
    }
    
    public String getSource() {
        return getGlueElement().getAttributes().get("source");
    }
    
    public void setSource(String source) {
        if (source == null) {
            getGlueElement().getAttributes().remove("source");
        } else {
            getGlueElement().getAttributes().put("source", source);
        }
    }

    public Object getSourceSceneGraphObject() {
        return sourceSceneGraphObject;
    }

    public void setSourceSceneGraphObject(Object sourceSceneGraphObject) {
        this.sourceSceneGraphObject = sourceSceneGraphObject;
    }
    
    public Map<PropertyName, FXOMProperty> getProperties() {
        return properties;
    }
    
    public static FXOMIntrinsic newInstance(FXOMIntrinsic source, FXOMDocument targetDocument) {
        final FXOMIntrinsic result;
        
        assert source != null;
        assert targetDocument != null;
        assert source.getFxomDocument() != targetDocument;
        
        result = new FXOMIntrinsic(
                targetDocument,
                source.getType(),
                source.getSource());
        
        result.setFxConstant(source.getFxConstant());
        result.setFxController(source.getFxController());
        result.setFxFactory(source.getFxFactory());
        result.setFxId(source.getFxId());
        result.setFxValue(source.getFxValue());
        
        
        return result;
    }

    /*
     * FXOMObject
     */

    @Override
    public List<FXOMObject> getChildObjects() {
        // Intrinsics have not children
        return Collections.emptyList();
    }


    @Override
    public FXOMObject searchWithSceneGraphObject(Object sceneGraphObject) {
        FXOMObject result;
        
        if (getType() == Type.FX_INCLUDE) {
            result = super.searchWithSceneGraphObject(sceneGraphObject);
        } else {
            result = null;
        }
        
        return result;
    }

    @Override
    public FXOMObject searchWithFxId(String fxId) {
        FXOMObject result;
        
        if (getType() == Type.FX_INCLUDE) {
            result = super.searchWithFxId(fxId);
        } else {
            result = null;
        }
        
        return result;
    }

    @Override
    protected void collectDeclaredClasses(Set<Class<?>> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectNullProperties(List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectPropertiesT(List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectProperties(PropertyName propertyName, List<FXOMProperty> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectReferences(String source, List<FXOMIntrinsic> result) {
        assert result != null;
        
        if ((getType() == Type.FX_REFERENCE) 
                && ((source == null) || source.equals(getSource()))) {
            result.add(this);
        }
    }

    @Override
    protected void collectIncludes(String source, List<FXOMIntrinsic> result) {
        assert result != null;
        
        if ((getType() == Type.FX_INCLUDE) 
                && ((source == null) || source.equals(getSource()))) {
            result.add(this);
        }
    }

    @Override
    protected void collectFxIds(Map<String, FXOMObject> result) {
        final String fxId = getFxId();
        if (fxId != null) {
            result.put(fxId, this);
        }
    }

    @Override
    protected void collectObjectWithSceneGraphObjectClass(Class<?> sceneGraphObjectClass, List<FXOMObject> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectEventHandlers(List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    /*
     * FXOMNode
     */
    
    @Override
    public void documentLocationWillChange(URL newLocation) {
        // Nothing special to do here
    }
    
    
    /*
     * Private
     */
    
    private static String makeTagNameFromType(Type type) {
        final String result;
        
        switch(type) {
            case FX_COPY:
                result = "fx:copy";
                break;
            case FX_REFERENCE:
                result = "fx:reference";
                break;
            case FX_INCLUDE:
                result = "fx:include";
                break;
            default:
                assert false;
                throw new IllegalStateException("Unexpected intrinsic type " + type);
        }
        
        return result;
    }
}
