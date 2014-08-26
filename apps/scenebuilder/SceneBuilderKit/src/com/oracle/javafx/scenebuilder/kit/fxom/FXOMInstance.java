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
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXMLLoader;

/**
 *
 * 
 */
public class FXOMInstance extends FXOMObject {
    
    private final Map<PropertyName, FXOMProperty> properties = new LinkedHashMap<>();
    private Class<?> declaredClass;
    
    
    FXOMInstance(
            FXOMDocument fxomDocument, 
            GlueElement glueElement, 
            Class<?> declaredClass,
            Object sceneGraphObject,
            List<FXOMProperty> properties) {
        super(fxomDocument, glueElement, sceneGraphObject);
        
        assert declaredClass != null;
        assert glueElement.getTagName().equals("fx:root") 
                || glueElement.getTagName().equals(PropertyName.makeClassFullName(declaredClass))
                || glueElement.getTagName().equals(declaredClass.getCanonicalName());
        assert sceneGraphObject != null;
        assert properties != null;

        this.declaredClass = declaredClass;
        for (FXOMProperty p : properties) {
            this.properties.put(p.getName(), p);
            p.setParentInstance(this);
        }
    }
    
    FXOMInstance(
            FXOMDocument fxomDocument, 
            GlueElement glueElement, 
            List<FXOMProperty> properties) {
        super(fxomDocument, glueElement, null);
        
        assert properties != null;

        this.declaredClass = null;
        for (FXOMProperty p : properties) {
            this.properties.put(p.getName(), p);
            p.setParentInstance(this);
        }
    }
    
    public FXOMInstance(FXOMDocument fxomDocument, Class<?> declaredClass) {
        super(fxomDocument, PropertyName.makeClassFullName(declaredClass));
        this.declaredClass = declaredClass;
    }
    
    public FXOMInstance(FXOMDocument fxomDocument, String tagName) {
        super(fxomDocument, tagName);
        this.declaredClass = null; // This is an unresolved instance
    }
    
    public Class<?> getDeclaredClass() {
        return declaredClass;
    }

    public void setDeclaredClass(Class<?> declaredClass) {
        this.declaredClass = declaredClass;
    }

    public Map<PropertyName, FXOMProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public boolean isFxRoot() {
        return getGlueElement().getTagName().equals("fx:root");
    }
    
    public void toggleFxRoot() {
        
        if (isFxRoot()) {
            assert getType() != null;
            getGlueElement().setTagName(getType());
            getGlueElement().getAttributes().remove(FXMLLoader.ROOT_TYPE_ATTRIBUTE);
        } else {
            assert getType() == null;
            getGlueElement().getAttributes().put(FXMLLoader.ROOT_TYPE_ATTRIBUTE, getGlueElement().getTagName());
            getGlueElement().setTagName("fx:root");
        }
    }
    
    public String getType() {
        return getGlueElement().getAttributes().get(FXMLLoader.ROOT_TYPE_ATTRIBUTE);
    }
    
    /*
     * FXOMObject
     */

    @Override
    public void addToParentCollection(int index, FXOMCollection newParentCollection) {
        super.addToParentCollection(index, newParentCollection);
        
        // May be this object was root : fx:root, type properties must be reset.
        resetRootProperties();
    }

    @Override
    public void addToParentProperty(int index, FXOMPropertyC newParentProperty) {
        super.addToParentProperty(index, newParentProperty); //To change body of generated methods, choose Tools | Templates.
        
        // May be this object was root : fx:root, type properties must be reset.
        resetRootProperties();
    }


    @Override
    public List<FXOMObject> getChildObjects() {
        final List<FXOMObject> result = new ArrayList<>();
        
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyC) {
                final FXOMPropertyC pc = (FXOMPropertyC) p;
                result.addAll(pc.getValues());
            }
        }
        return result;
    }


    @Override
    public FXOMObject searchWithSceneGraphObject(Object sceneGraphObject) {
        FXOMObject result;
        
        result = super.searchWithSceneGraphObject(sceneGraphObject);
        if (result == null) {
            final Iterator<FXOMProperty> it = properties.values().iterator();
            while ((result == null) && it.hasNext()) {
                final FXOMProperty property = it.next();
                if (property instanceof FXOMPropertyC) {
                    FXOMPropertyC propertyC = (FXOMPropertyC) property;
                    final Iterator<FXOMObject> itValue = propertyC.getValues().iterator();
                    while ((result == null) && itValue.hasNext()) {
                        final FXOMObject value = itValue.next();
                        result = value.searchWithSceneGraphObject(sceneGraphObject);
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public FXOMObject searchWithFxId(String fxId) {
        FXOMObject result;
        
        result = super.searchWithFxId(fxId);
        if (result == null) {
            final Iterator<FXOMProperty> it = properties.values().iterator();
            while ((result == null) && it.hasNext()) {
                final FXOMProperty property = it.next();
                if (property instanceof FXOMPropertyC) {
                    FXOMPropertyC propertyC = (FXOMPropertyC) property;
                    final Iterator<FXOMObject> itValue = propertyC.getValues().iterator();
                    while ((result == null) && itValue.hasNext()) {
                        final FXOMObject value = itValue.next();
                        result = value.searchWithFxId(fxId);
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public FXOMNode lookupFirstReference(String fxId, FXOMObject scope) {
        FXOMNode result;
        
        if (this == scope) {
            result = null;
        } else {
            result = null;
            for (FXOMProperty p : properties.values()) {
                if (p instanceof FXOMPropertyT) {
                    final FXOMPropertyT t = (FXOMPropertyT) p;
                    final PrefixedValue pv = new PrefixedValue(t.getValue());
                    if (pv.isExpression()) {
                        final String expression = pv.getSuffix();
                        if (fxId.equals(expression)) {
                            result = p;
                            break;
                        }
                    }
                } else {
                    assert p instanceof FXOMPropertyC;
                    final FXOMPropertyC c = (FXOMPropertyC) p;
                    for (FXOMObject v : c.getValues()) {
                        result = v.lookupFirstReference(fxId, scope);
                        if (result != null) {
                            break;
                        }
                    }
                }
                if (result != null) {
                    break;
                }
            }
        }
        
        return result;
    }

    @Override
    protected void collectDeclaredClasses(Set<Class<?>> result) {
        assert result != null;
        
        if (declaredClass != null) {
            result.add(declaredClass);
        }
        
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyC) {
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectDeclaredClasses(result);
                }
            }
        }
    }

    @Override
    protected void collectProperties(PropertyName propertyName, List<FXOMProperty> result) {
        assert propertyName != null;
        assert result != null;
        
        for (FXOMProperty p : properties.values()) {
            if (p.getName().equals(propertyName)) {
                result.add(p);
            }
            if (p instanceof FXOMPropertyC) {
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectProperties(propertyName, result);
                }
            }
        }
    }

    @Override
    protected void collectNullProperties(List<FXOMPropertyT> result) {
        assert result != null;
        
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyT) {
                final FXOMPropertyT tp = (FXOMPropertyT) p;
                if (tp.getValue().equals("$null")) {
                    result.add(tp);
                }
            } else {
                assert p instanceof FXOMPropertyC;
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectNullProperties(result);
                }
            }
        }
    }

    @Override
    protected void collectPropertiesT(List<FXOMPropertyT> result) {
        assert result != null;
        
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyT) {
                final FXOMPropertyT tp = (FXOMPropertyT) p;
                result.add(tp);
            } else {
                assert p instanceof FXOMPropertyC;
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectPropertiesT(result);
                }
            }
        }
    }

    @Override
    protected void collectReferences(String source, List<FXOMIntrinsic> result) {
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyC) {
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectReferences(source, result);
                }
            }
        }
    }

    @Override
    protected void collectIncludes(String source, List<FXOMIntrinsic> result) {
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyC) {
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectIncludes(source, result);
                }
            }
        }
    }

    @Override
    protected void collectFxIds(Map<String, FXOMObject> result) {
        final String fxId = getFxId();
        if (fxId != null) {
            result.put(fxId, this);
        }
        
        for (FXOMProperty p : properties.values()) {
            if (p instanceof FXOMPropertyC) {
                for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                    v.collectFxIds(result);
                }
            }
        }
    }

    @Override
    protected void collectObjectWithSceneGraphObjectClass(Class<?> sceneGraphObjectClass, List<FXOMObject> result) {
        if (getSceneGraphObject() != null) {
            if (getSceneGraphObject().getClass() == sceneGraphObjectClass) {
                result.add(this);
            }
            for (FXOMProperty p : properties.values()) {
                if (p instanceof FXOMPropertyC) {
                    for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                        v.collectObjectWithSceneGraphObjectClass(sceneGraphObjectClass, result);
                    }
                }
            }
        }
    }

    @Override
    protected void collectEventHandlers(List<FXOMPropertyT> result) {
        if (getSceneGraphObject() != null) {
            for (FXOMProperty p : properties.values()) {
                if (p instanceof FXOMPropertyT) {
                    final FXOMPropertyT pt = (FXOMPropertyT) p;
                    if (pt.getName().getName().startsWith("on") && pt.getValue().startsWith("#")) {
                        result.add(pt);
                    }
                }
            }
            for (FXOMProperty p : properties.values()) {
                if (p instanceof FXOMPropertyC) {
                    for (FXOMObject v : ((FXOMPropertyC)p).getValues()) {
                        v.collectEventHandlers(result);
                    }
                }
            }
        }
    }

    /*
     * FXOMNode
     */
    
    @Override
    protected void changeFxomDocument(FXOMDocument destination) {
        
        super.changeFxomDocument(destination);
        for (FXOMProperty p : properties.values()) {
            p.changeFxomDocument(destination);
        }
    }

    @Override
    public void documentLocationWillChange(URL newLocation) {
        for (FXOMProperty p : properties.values()) {
            p.documentLocationWillChange(newLocation);
        }
    }

    
    /*
     * Package
     */
    
    /* For FXOMProperty.addToParentInstance() private use only */
    void addProperty(FXOMProperty property) {
        assert property.getParentInstance() == this;
        assert properties.get(property.getName()) == null;
        properties.put(property.getName(), property);
    }
    
    /* For FXOMProperty.removeFromParentInstance() private use only */
    void removeProperty(FXOMProperty property) {
        assert property.getParentInstance() == null;
        assert properties.get(property.getName()) == property;
        properties.remove(property.getName());
        
    }
    
    
    /*
     * Private
     */
    
    private void resetRootProperties() {
        if (isFxRoot()) {
            toggleFxRoot();
        }
    }
}
