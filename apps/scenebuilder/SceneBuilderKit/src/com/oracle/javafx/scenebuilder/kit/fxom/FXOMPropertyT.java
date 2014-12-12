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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.StringListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * 
 */
public class FXOMPropertyT extends FXOMProperty {
    
    /*
     * A FXOMPropertyT represents a property with a single value that
     * can be represented as text : String, Boolean, Integer, Double, Enum...
     * 
     * There are three ways to express such a property in FXML.
     * Let's take Button.text as an example:
     * 
     * 1) <Button text="OK" />
     *      value = "OK"
     *      propertyElement == null
     *      valueElement == null
     * 
     * 2) <Button><text>OK</text></Button>
     *      value = "OK"
     *      propertyElement != null 
     *          && propertyElement.getTagName() == "text"
     *          && propertyElement.getChildren().size() == 0
     *          && propertyElement.getContentText().equals("OK")
     * 
     * 3) <Button><text><String fx:value="OK"/></text></Button>
     *      value = "OK"
     *      propertyElement != null 
     *          && propertyElement.getTagName() == "text"
     *          && propertyElement.getChildren().size() == 1
     *          && propertyElement.getChildren().get(0) == valueElement
     *      valueElement != null
     *          && valueElement.getAttributes().get("fx:value").equals("OK")
     */
    
    private String value;
    private final GlueElement propertyElement;
    private final GlueElement valueElement;

    public FXOMPropertyT(FXOMDocument document, PropertyName name, GlueElement propertyElement, GlueElement valueElement, String value) {
        super(document, name);
        this.propertyElement = propertyElement;
        this.valueElement = valueElement;
        this.value = value;
    }
    
    public FXOMPropertyT(FXOMDocument document, PropertyName name, String value) {
        super(document, name);
        assert value != null;
        this.propertyElement = null;
        this.valueElement = null;
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String newValue) {
        assert newValue != null;
        
        
        if (propertyElement != null) {
            if (valueElement != null) { // Case #3
                final Map<String,String> attributes = valueElement.getAttributes();
                assert attributes.get("fx:value") != null;
                assert attributes.get("fx:value").equals(value);
                attributes.put("fx:value", newValue);
                
            } else { // Case #2
                assert propertyElement.getContentText() != null;
                assert propertyElement.getContentText().equals(value);
                propertyElement.setContentText(newValue);
            }
        } else { // Case #1
            final FXOMInstance parentInstance = getParentInstance();
            if (parentInstance != null) {
                final GlueElement parentElement = parentInstance.getGlueElement();
                final Map<String, String> parentAttributes = parentElement.getAttributes();
                assert parentAttributes.get(getName().toString()).equals(value);
                parentAttributes.put(getName().toString(), newValue);
            }
        }
        
        value = newValue;
    }

    public GlueElement getPropertyElement() {
        return propertyElement;
    }

    public GlueElement getValueElement() {
        return valueElement;
    }

    /*
     * FXOMProperty
     */
    
    @Override
    public void addToParentInstance(int index, FXOMInstance newParentInstance) {
        
        assert newParentInstance != null;
        
        if (getParentInstance() != null) {
            removeFromParentInstance();
        }
        
        setParentInstance(newParentInstance);
        newParentInstance.addProperty(this);
        
        final GlueElement newParentElement = newParentInstance.getGlueElement();
        
        if (propertyElement == null) { // Case #1
            // index is ignored
            final Map<String,String> attributes = newParentElement.getAttributes();
            assert attributes.get(getName().toString()) == null;
            attributes.put(getName().toString(), value);
        } else { // Case #2 or #3
            assert -1 <= index;
            assert index <= newParentElement.getChildren().size();
            propertyElement.addToParent(index, newParentElement);
        }
    }

    
    @Override
    public void removeFromParentInstance() {
        
        assert getParentInstance() != null;
        
        final FXOMInstance currentParentInstance = getParentInstance();
        final GlueElement currentParentElement = currentParentInstance.getGlueElement();
        
        if (propertyElement == null) { // Case #1
            final Map<String,String> attributes = currentParentElement.getAttributes();
            assert attributes.get(getName().toString()) != null;
            attributes.remove(getName().toString());
        } else { // Case #2 or #3
           propertyElement.removeFromParent();
        }

        setParentInstance(null);
        currentParentInstance.removeProperty(this);
    }
    
 
    @Override
    public int getIndexInParentInstance() {
        final int result;
        
        if (getParentInstance() == null) {
            result = -1;
        } else if (propertyElement == null) { // Case #1
            result = -1;
        } else { // Case #2 or #3
            final GlueElement parentElement = getParentInstance().getGlueElement();
            result = parentElement.getChildren().indexOf(propertyElement);
            assert result != -1;
        }
        
        return result;
    }
    
    
    /*
     * FXOMNode
     */
    
    @Override
    public void moveToFxomDocument(FXOMDocument destination) {
        assert destination != null;
        assert destination != getFxomDocument();
        
        documentLocationWillChange(destination.getLocation());
        
        if (getParentInstance() != null) {
            assert getParentInstance().getFxomDocument() == getFxomDocument();
            removeFromParentInstance();
        }
        
        assert getParentInstance() == null;
        assert (propertyElement == null) || (propertyElement.getParent() == null);
        
        if (propertyElement != null) {
            propertyElement.moveToDocument(destination.getGlue());
            assert (valueElement == null) || (valueElement.getDocument() == destination.getGlue());
        }
        changeFxomDocument(destination);
    }

    
    @Override
    protected void changeFxomDocument(FXOMDocument destination) {
        assert destination != null;
        assert destination != getFxomDocument();
        assert (propertyElement == null) || (destination.getGlue() == propertyElement.getDocument());
        
        super.changeFxomDocument(destination);
    }
    
    @Override
    public void documentLocationWillChange(URL newLocation) {
        final URL currentLocation = getFxomDocument().getLocation();
        
        final List<String> currentItems = StringListPropertyMetadata.splitValue(getValue());
        final List<String> newItems = new ArrayList<>();
        int changeCount = 0;
        for (String currentItem : currentItems) {
            final PrefixedValue pv = new PrefixedValue(currentItem);
            if (pv.isDocumentRelativePath()) {
                assert currentLocation != null;

                /*
                 * currentItem is a path relative to currentLocation.
                 * We compute the absolute path and, if new location 
                 * is non null, we relativize the absolute path against
                 * newLocation.
                 */
                final URL assetURL = pv.resolveDocumentRelativePath(currentLocation);
                final String newValue;
                if (newLocation == null) {
                    newValue = assetURL.toString();
                } else {
                    final PrefixedValue pv2 
                            = PrefixedValue.makePrefixedValue(assetURL, newLocation);
                    newValue = pv2.toString();
                }
                newItems.add(newValue);
                changeCount++;
            } else if (pv.isPlainString() && (currentLocation == null)) {

                /*
                 * currentItem is a plain string.
                 * We check if it is an URL.
                 * 
                 * Since currentLocation is null and newLocation non null,
                 * then all URLs should be converted to relative path.
                 */
                assert newLocation != null;
                try {
                    final URL assetURL = new URL(pv.getSuffix());
                    final PrefixedValue pv2 = PrefixedValue.makePrefixedValue(assetURL, newLocation);
                    newItems.add(pv2.toString());
                    changeCount++;
                } catch(MalformedURLException x) {
                    // p.getValue() is not an URL
                    // We keep currentItem unchanged.
                    newItems.add(currentItem);
                }
            } else {
                newItems.add(currentItem);
            }
        }
        assert currentItems.size() == newItems.size();
        
        if (changeCount >= 1) {
            setValue(StringListPropertyMetadata.assembleValue(newItems));
        }
                
    }
    
}
