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


/**
 *
 */
public class PropertyName implements Comparable<PropertyName> {
    
    private final String name;
    private final Class<?> residenceClass;

    public PropertyName(String propertyName, Class<?> foreignClass) {
        assert propertyName != null;
        
        this.residenceClass = foreignClass;
        this.name = propertyName;
    }

    public PropertyName(String propertyName) {
        this(propertyName, null);
    }
    
    public Class<?> getResidenceClass() {
        return residenceClass;
    }

    public String getName() {
        return name;
    }
    
    public Object getValue(Object sceneGraphObject) {
        final Object result;
        
        if (residenceClass == null) {
            final BeanPropertyIntrospector bpi 
                    = new BeanPropertyIntrospector(sceneGraphObject);
            result = bpi.getValue(name);
        } else {
            final StaticPropertyIntrospector spi 
                    = new StaticPropertyIntrospector(sceneGraphObject, residenceClass);
            result = spi.getValue(name);
        }
        
        return result;
    }
    
    public void setValue(Object sceneGraphObject, Object value) {
        if (residenceClass == null) {
            final BeanPropertyIntrospector bpi 
                    = new BeanPropertyIntrospector(sceneGraphObject);
            bpi.setValue(name, value);
        } else {
            final StaticPropertyIntrospector spi 
                    = new StaticPropertyIntrospector(sceneGraphObject, residenceClass);
            spi.setValue(name, value);
        }
    }
    
    
    public static String makeClassFullName(Class<?> aClass) {
        assert aClass != null;
        
        final StringBuilder result = new StringBuilder();
        result.append(aClass.getSimpleName());
        Class<?> declaringClass = aClass.getDeclaringClass();
        while (declaringClass != null) {
            result.insert(0, '.');
            result.insert(0, declaringClass.getSimpleName());
            declaringClass = declaringClass.getDeclaringClass();
        }
        
        return result.toString();
    }
    
    /*
     * Object
     */
    
    @Override
    public boolean equals(Object o) {
        boolean result;
        
        if (this == o) {
            result = true;
        } else if ((o == null) || (o.getClass() != this.getClass())) {
            result = false;
        } else {
            final PropertyName other = (PropertyName) o;
            
            result = true;
            if (residenceClass == null) {
                result = result && (other.residenceClass == null);
            } else {
                result = result && residenceClass.equals(other.residenceClass);
            }
            result = result && name.equals(other.name);
        }
        
        return result;
    }
            
    
    @Override
    public int hashCode() {
        int result = 7;
        if (residenceClass != null) {
            result = 31 * result + residenceClass.hashCode();
        }
        result = 31 * result + name.hashCode();
        return result;
    }

    
    @Override
    public String toString() {
        final String result;
        
        if (residenceClass == null) {
            result = name;
        } else {
            result = makeClassFullName(residenceClass) + "." + name; //NOI18N
        }
        
        return result;
    }

    /*
     * Comparable
     */
    @Override
    public int compareTo(PropertyName t) {
        int result;
        
        if (this == t) {
            result = 0;
        } else if (t == null) {
            result = -1;
        } else {
            if ((this.residenceClass == null) && (t.residenceClass == null)) {
                result = 0;
            } else if (t.residenceClass == null) {
                result = +1;
            } else if (this.residenceClass == null) {
                result = -1;
            }  else {
                result = residenceClass.getCanonicalName().compareToIgnoreCase(t.residenceClass.getCanonicalName());
            }
            if (result == 0) {
                result = name.compareToIgnoreCase(t.name);
            }
        }
        
        return result;
    }
}
