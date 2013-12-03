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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.list;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public abstract class ListValuePropertyMetadata<T> extends ValuePropertyMetadata {

    private final Class<T> itemClass;
    private final List<T> defaultValue;
    
    public ListValuePropertyMetadata(PropertyName name, Class<T> itemClass, 
            boolean readWrite, List<T> defaultValue, InspectorPath inspectorPath) {
        super(name, readWrite, inspectorPath);
        this.itemClass = itemClass;
        this.defaultValue = defaultValue;
    }
    
    public Class<T> getItemClass() {
        return itemClass;
    }
    
    public List<T> getValue(FXOMInstance fxomInstance) {
        final List<T> result;
        
        if (isReadWrite()) {
            final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());
            if (fxomProperty == null) {
                // propertyName is not specified in the fxom instance.
                // We return the default value specified in the metadata of the
                // property
                result = defaultValue;
            } else if (fxomProperty instanceof FXOMPropertyT) {
                final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
                result = new ArrayList<>();
                result.add(castItemValue(fxomPropertyT.getValue()));
            } else {
                assert fxomProperty instanceof FXOMPropertyC;
                
                final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
                result = new ArrayList<>();
                for (FXOMObject itemFxomObject : fxomPropertyC.getValues()) {
                    assert itemFxomObject instanceof FXOMInstance;
                    final FXOMInstance itemFxomInstance = (FXOMInstance) itemFxomObject;
                    result.add(castItemValue(itemFxomInstance.getSceneGraphObject()));
                }
            }
        } else {
            final List<?> items = (List<?>)getName().getValue(fxomInstance.getSceneGraphObject());
            result = new ArrayList<>();
            for (Object item : items) {
                result.add(castItemValue(item));
            }
        }
        
        return result;
    }

    public void setValue(FXOMInstance fxomInstance, List<T> value) {
        assert isReadWrite();
        
        final FXOMProperty fxomProperty = fxomInstance.getProperties().get(getName());

        if (Objects.equals(value, getDefaultValueObject())) {
            // We must remove the fxom property if any
            if (fxomProperty != null) {
                fxomProperty.removeFromParentInstance();
            }
        } else {
            if (fxomProperty == null) {
                // propertyName is not specified in the fxom instance.
                // We insert a new fxom property
                final FXOMProperty newProperty;
                if (isItemTextEncodable() && (value.size() == 1)) {
                    final String itemText = itemTextEncoding(value.get(0));
                    newProperty = makeFxomPropertyFromValue(fxomInstance, itemText);
                } else {
                    newProperty = makeFxomPropertyFromValue(fxomInstance, value);
                }
                newProperty.addToParentInstance(-1, fxomInstance);
            } else {
                /*
                 *       \ value |     size = 1           |      size > 1
                 * fxomProperty  |                        |
                 * --------------+------------------------+--------------------
                 * FXOMPropertyT | update FXOMPropertyT   | remove
                 *               |                        | new FXOMPropertyC
                 *               |                        | add
                 *               |           #1           |         #2
                 * --------------+------------------------+--------------------
                 * FXOMPropertyC | if text-encodable #3a  | update FXOMPropertyC
                 *               |   remove               |
                 *               |   new FXOMPropertyT    | 
                 *               |   add                  |
                 *               | else              #3b  |
                 *               |   update FXOMPropertyC |
                 *               |                        |         #4
                 */
                if (value.size() == 1) {
                    if (fxomProperty instanceof FXOMPropertyT) {
                        // Case #1
                        final FXOMPropertyT fxomPropertyT = (FXOMPropertyT) fxomProperty;
                        updateFxomPropertyWithValue(fxomPropertyT, value.get(0));
                    } else {
                        assert fxomProperty instanceof FXOMPropertyC;
                        if (isItemTextEncodable()) {
                            // Case #3a
                            fxomProperty.removeFromParentInstance();
                            final String itemText = itemTextEncoding(value.get(0));
                            final FXOMPropertyT newProperty
                                   = makeFxomPropertyFromValue(fxomInstance, itemText);
                            newProperty.addToParentInstance(-1, fxomInstance);
                        } else {
                            // Case #3b
                            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
                            updateFxomPropertyWithValue(fxomPropertyC, value);
                        }
                    }
                } else {
                    assert value.size() > 1;
                    if (fxomProperty instanceof FXOMPropertyT) {
                        // Case #2
                        fxomProperty.removeFromParentInstance();
                        final FXOMPropertyC newProperty
                               = makeFxomPropertyFromValue(fxomInstance, value);
                        newProperty.addToParentInstance(-1, fxomInstance);
                    } else {
                        // Case #4
                        assert fxomProperty instanceof FXOMPropertyC;
                        final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
                        updateFxomPropertyWithValue(fxomPropertyC, value);
                    }
                }
            }
        }
    }
    
    public FXOMPropertyT makeFxomPropertyFromValue(FXOMInstance fxomInstance, String value) {
        assert fxomInstance != null;
        assert value != null;
        
        final FXOMDocument fxomDocument = fxomInstance.getFxomDocument();
        return new FXOMPropertyT(fxomDocument, getName(), value);
    }
    
    public FXOMPropertyC makeFxomPropertyFromValue(FXOMInstance fxomInstance, List<T> value) {
        assert fxomInstance != null;
        assert value != null;
        assert value.isEmpty() == false;
        
        final FXOMDocument fxomDocument = fxomInstance.getFxomDocument();
        final List<FXOMObject> items = new ArrayList<>();
        for (T item : value) {
            final FXOMInstance itemInstance = new FXOMInstance(fxomDocument, itemClass);
            updateFxomInstanceWithItemValue(itemInstance, item);
            items.add(itemInstance);
        }
        return new FXOMPropertyC(fxomDocument, getName(), items);
    }
    
    protected void updateFxomPropertyWithValue(FXOMPropertyT fxomProperty, T value) {
        
        fxomProperty.setValue(itemTextEncoding(value));
    }
    
    protected void updateFxomPropertyWithValue(FXOMPropertyC fxomProperty, List<T> value) {
        
        final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
        final int currentCount = fxomProperty.getValues().size();
        final int newCount = value.size();
        final int updateCount = Math.min(currentCount, newCount);
        
        // Update items
        for (int i = 0; i < updateCount; i++) {
            final FXOMObject itemFxomObject = fxomProperty.getValues().get(i);
            final T itemValue = value.get(i);
            if (itemFxomObject instanceof FXOMInstance) {
                updateFxomInstanceWithItemValue((FXOMInstance) itemFxomObject, itemValue);
            } else {
                assert itemFxomObject instanceof FXOMIntrinsic;
                final FXOMInstance itemInstance = new FXOMInstance(fxomDocument, itemClass);
                updateFxomInstanceWithItemValue(itemInstance, itemValue);
                itemInstance.addToParentProperty(i, fxomProperty);
                itemFxomObject.removeFromParentProperty();
            }
        }
        
        if (currentCount < newCount) {
            // Add new items
            final int addCount = newCount - currentCount;
            for (int i = 0; i < addCount; i++) {
                final FXOMInstance itemInstance = new FXOMInstance(fxomDocument, itemClass);
                final T itemValue = value.get(currentCount + i);
                updateFxomInstanceWithItemValue(itemInstance, itemValue);
                itemInstance.addToParentProperty(-1, fxomProperty);
            }
        } else {
            // Delete old items
            final int removeCount = currentCount - newCount;
            for (int i = 0; i < removeCount; i++) {
                final FXOMObject itemFxomObject = fxomProperty.getValues().get(newCount);
                itemFxomObject.removeFromParentProperty();
            }
        }
        
    }
    
    
    protected void updateFxomInstanceWithItemValue(FXOMInstance itemInstance, T itemValue) {
        throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
    }
    
    protected abstract T castItemValue(Object value);
    
    protected abstract boolean isItemTextEncodable();
    
    protected abstract String itemTextEncoding(T value);

    
    /*
     * ValuePropertyMetadata
     */
    
    @Override
    public Class<?> getValueClass() {
        return List.class;
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
        assert valueObject instanceof List;
        setValue(fxomInstance, castItemList((List)valueObject));
    }
    
    /*
     * Private
     */
    
    private List<T> castItemList(List<?> valueObject) {
        final List<T> result = new ArrayList<>();
        
        for (Object itemValueObject : valueObject) {
            result.add(castItemValue(itemValueObject));
        }
        
        return result;
    }
}
