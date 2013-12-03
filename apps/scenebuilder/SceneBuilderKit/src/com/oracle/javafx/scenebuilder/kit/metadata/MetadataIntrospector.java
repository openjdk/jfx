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
package com.oracle.javafx.scenebuilder.kit.metadata;

import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.CustomComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.ColorPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.IntegerPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.PaintPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.StringPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import static com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath.CUSTOM_SECTION;
import static com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath.CUSTOM_SUB_SECTION;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import javafx.fxml.FXMLLoader;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 *
 * 
 */
class MetadataIntrospector {
    
    private final Class<?> componentClass;
    private final ComponentClassMetadata ancestorMetadata;
    private int counter;
    
    public MetadataIntrospector(Class<?> componentClass, ComponentClassMetadata ancestorMetadata) {
        this.componentClass = componentClass;
        this.ancestorMetadata = ancestorMetadata;
    }
    
    public ComponentClassMetadata introspect() {
        final Set<PropertyMetadata> properties = new HashSet<>();
        final Set<PropertyName> hiddenProperties = Metadata.getMetadata().getHiddenProperties();
        Exception exception;
        
        
        try {
            final Object sample = instantiate();
            final BeanInfo beanInfo = Introspector.getBeanInfo(componentClass);
            for (PropertyDescriptor d : beanInfo.getPropertyDescriptors()) {
                final PropertyName name = new PropertyName(d.getName());
                PropertyMetadata propertyMetadata 
                        = lookupPropertyMetadata(ancestorMetadata, name);
                if ((propertyMetadata == null) 
                        && (hiddenProperties.contains(name) == false)) {
                    propertyMetadata = makePropertyMetadata(name, d, sample);
                    if (propertyMetadata != null) {
                        properties.add(propertyMetadata);
                    }
                }
            }
            exception = null;
        } catch(IOException | IntrospectionException x) {
            exception = x;
        }
        
        final CustomComponentClassMetadata result 
                = new CustomComponentClassMetadata(componentClass,  
                ancestorMetadata, exception);
        result.getProperties().addAll(properties);
        
        return result;
    }
    
    
    /*
     * Private
     */
    
    private Object instantiate() throws IOException {
        final StringBuilder sb = new StringBuilder();
        Object result;
        
        /*
         * <?xml version="1.0" encoding="UTF-8"?>
         * 
         * <?import a.b.C?>
         * 
         * <C/>
         */
        
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        sb.append("<?import ");
        sb.append(componentClass.getCanonicalName());
        sb.append("?>");
        sb.append("<");
        sb.append(componentClass.getSimpleName());
        sb.append("/>\n");
        
        final FXMLLoader fxmlLoader = new FXMLLoader();
        final String fxmlText = sb.toString();
        final byte[] fxmlBytes = fxmlText.getBytes(Charset.forName("UTF-8"));

        try {
            fxmlLoader.setClassLoader(componentClass.getClassLoader());
            result = fxmlLoader.load(new ByteArrayInputStream(fxmlBytes));
        } catch(RuntimeException x) {
            throw new IOException(x);
        }
        
        return result;
    }
    
    
    private PropertyMetadata lookupPropertyMetadata(
            ComponentClassMetadata ccm, PropertyName propertyName) {
        PropertyMetadata result = null;
        
        while ((ccm != null) && (result == null)) {
            result = ccm.lookupProperty(propertyName);
            ccm = ccm.getParentMetadata();
        }
        
        return result;
    }
    
    private PropertyMetadata makePropertyMetadata(PropertyName name, 
            PropertyDescriptor d, Object sample) {
        final PropertyMetadata result;
        
        if (d.getPropertyType() == null) {
            result = null;
        } else if (d.getReadMethod() == null) {
            result = null;
        } else {
            final Class<?> propertyType = canonizeClass(d.getPropertyType());
            final boolean readWrite = d.getWriteMethod() != null;
            final InspectorPath inspectorPath 
                    = new InspectorPath(CUSTOM_SECTION, CUSTOM_SUB_SECTION, counter++);
            
            if (propertyType.isArray()) {
                result = null;
            } else if (propertyType.isEnum()) {
                final Object fallback = propertyType.getEnumConstants()[0];
                result = new EnumerationPropertyMetadata(
                        name,
                        propertyType,
                        readWrite,
                        (Enum<?>)getDefaultValue(sample, d.getReadMethod(), fallback),
                        inspectorPath);
            } else if (propertyType == Boolean.class) {
                result = new BooleanPropertyMetadata(
                        name,
                        readWrite,
                        (Boolean)getDefaultValue(sample, d.getReadMethod(), false),
                        inspectorPath);
            } else if (propertyType == Integer.class) {
                result = new IntegerPropertyMetadata(
                        name,
                        readWrite,
                        (Integer)getDefaultValue(sample, d.getReadMethod(), 0),
                        inspectorPath);
            } else if (propertyType == Double.class) {
                result = new DoublePropertyMetadata(
                        name,
                        DoubleKind.COORDINATE,
                        readWrite,
                        (Double)getDefaultValue(sample, d.getReadMethod(), 0.0),
                        inspectorPath);
            } else if (propertyType == String.class) {
                result = new StringPropertyMetadata(
                        name,
                        readWrite,
                        (String)getDefaultValue(sample, d.getReadMethod(), null),
                        inspectorPath);
            } else if (propertyType == javafx.scene.paint.Color.class) {
                result = new ColorPropertyMetadata(
                        name,
                        readWrite,
                        (Color)getDefaultValue(sample, d.getReadMethod(), null),
                        inspectorPath);
            } else if (propertyType == javafx.scene.paint.Paint.class) {
                result = new PaintPropertyMetadata(
                        name,
                        readWrite,
                        (Paint)getDefaultValue(sample, d.getReadMethod(), null),
                        inspectorPath);
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    private Class<?> canonizeClass(Class<?> c) {
        final Class<?> result;
        
        if (c.equals(boolean.class)) {
            result = Boolean.class;
        } else if (c.equals(double.class)) {
            result = Double.class;
        } else if (c.equals(int.class)) {
            result = Integer.class;
        } else {
            result = c;
        }
        
        return result;
    }
    
    
    private Object getDefaultValue(Object sample, Method readMethod, Object fallback) {
        Object result;
        
        try {
            result = readMethod.invoke(sample);
        } catch(InvocationTargetException|IllegalAccessException x) {
            result = fallback;
        }
        
        return result;
    }
}
