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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.list;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.List;
import javafx.scene.paint.Color;

/**
 *
 */
public class StringListPropertyMetadata extends ListValuePropertyMetadata<String> {

    public StringListPropertyMetadata(PropertyName name, boolean readWrite, 
            List<String> defaultValue, InspectorPath inspectorPath) {
        super(name, String.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * ListValuePropertyMetadata
     */
    
    @Override
    protected String castItemValue(Object value) {
        assert value instanceof Boolean
                || value instanceof Color
                || value instanceof Double
                || value instanceof Integer
                || value instanceof String
                || value instanceof URL
                : "getName()=" + getName() //NOI18N
                + ",value=" + value; //NOI18N
        return value.toString();
    }
    
    @Override
    protected boolean isItemTextEncodable() {
        return true;
    }

    @Override
    protected String itemTextEncoding(String value) {
        return castItemValue(value);
    }

    @Override
    protected void updateFxomInstanceWithItemValue(FXOMInstance itemInstance, String itemValue) {
        assert itemInstance != null;
        assert itemValue != null;
        
        final Class<?> itemClass = itemInstance.getDeclaredClass();
        if (itemClass == URL.class) {
            updateURLInstanceWithItemValue(itemInstance, itemValue);
        } else {
            assert itemClass == Boolean.class
                    || itemClass == Color.class
                    || itemClass == Double.class
                    || itemClass == Integer.class
                    || itemClass == String.class;
            itemInstance.setFxValue(itemValue);
        }
    }
    
    
    /*
     * Private
     */
    
    private static final PropertyName valueName = new PropertyName("value"); //NOI18N
    
    private void updateURLInstanceWithItemValue(FXOMInstance urlInstance, String urlString) {
        final FXOMDocument fxomDocument = urlInstance.getFxomDocument();
        final FXOMProperty valueProperty = urlInstance.getProperties().get(valueName);
        
        if (valueProperty == null) {
            final FXOMPropertyT newProperty 
                    = new FXOMPropertyT(fxomDocument, valueName, urlString);
            newProperty.addToParentInstance(-1,urlInstance);
        } else if (valueProperty instanceof FXOMPropertyT) {
            final FXOMPropertyT valuePropertyT = (FXOMPropertyT) valueProperty;
            valuePropertyT.setValue(urlString);
        } else {
            assert valueProperty instanceof FXOMPropertyC;
            valueProperty.removeFromParentInstance();
            final FXOMPropertyT newProperty 
                    = new FXOMPropertyT(fxomDocument, valueName, urlString);
            newProperty.addToParentInstance(-1,urlInstance);
        }
    }
}
