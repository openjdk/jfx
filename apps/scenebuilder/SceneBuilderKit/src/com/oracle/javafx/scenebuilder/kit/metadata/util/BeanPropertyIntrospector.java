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
package com.oracle.javafx.scenebuilder.kit.metadata.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class BeanPropertyIntrospector {
    
    private final Object object;
    private final PropertyDescriptor[] propertyDescriptors;
    
    public BeanPropertyIntrospector(Object object) {
        assert object != null;
        this.object = object;
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
            this.propertyDescriptors = beanInfo.getPropertyDescriptors();
        } catch(IntrospectionException x) {
            throw new RuntimeException(x);
        }
    }
    
    public Object getValue(String propertyName) {
        final PropertyDescriptor d = findDescriptor(propertyName);
        final Object result;
        
        if (d != null) {
            try {
                result = d.getReadMethod().invoke(object);
            } catch(InvocationTargetException|IllegalAccessException x) {
                throw new RuntimeException(x);
            }
        } else {
            throw new RuntimeException(propertyName + " not found"); //NOI18N
        }
        
        return result;
    }
    
    
    public void setValue(String propertyName, Object value) {
        final PropertyDescriptor d = findDescriptor(propertyName);
        
        if (d != null) {
            try {
                d.getWriteMethod().invoke(object, value);
            } catch(InvocationTargetException|IllegalAccessException x) {
                throw new RuntimeException(x);
            }
        } else {
            throw new RuntimeException(propertyName + " not found"); //NOI18N
        }
    }
    
    private PropertyDescriptor findDescriptor(String propertyName) {
        assert propertyDescriptors != null;
        int i = 0;
        while ((i < propertyDescriptors.length) 
                && ! propertyDescriptors[i].getName().equals(propertyName)){
            i++;
        }
        
        return (i < propertyDescriptors.length) ? propertyDescriptors[i] : null;
    }
}
