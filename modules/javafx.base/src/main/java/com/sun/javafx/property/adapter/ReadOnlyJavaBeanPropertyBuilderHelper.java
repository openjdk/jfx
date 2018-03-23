/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.property.adapter;

import java.lang.reflect.Method;
import com.sun.javafx.reflect.ReflectUtil;

/**
 */
public class ReadOnlyJavaBeanPropertyBuilderHelper {

    private static final String IS_PREFIX = "is";
    private static final String GET_PREFIX = "get";

    private String propertyName;
    private Class<?> beanClass;
    private Object bean;
    private String getterName;
    private Method getter;
    private ReadOnlyPropertyDescriptor descriptor;

    public void name(String propertyName) {
        if ((propertyName == null)? this.propertyName != null : !propertyName.equals(this.propertyName)) {
            this.propertyName = propertyName;
            this.descriptor = null;
        }
    }

    public void beanClass(Class<?> beanClass) {
        if ((beanClass == null)? this.beanClass != null : !beanClass.equals(this.beanClass)) {
            ReflectUtil.checkPackageAccess(beanClass);
            this.beanClass = beanClass;
            this.descriptor = null;
        }
    }

    public void bean(Object bean) {
        this.bean = bean;
        if (bean != null) {
            Class<?> newClass = bean.getClass();
            if ((beanClass == null) || !beanClass.isAssignableFrom(newClass)) {
                ReflectUtil.checkPackageAccess(newClass);
                this.beanClass = bean.getClass();
                this.descriptor = null;
            }
        }
    }

    public Object getBean() {
        return bean;
    }

    public void getterName(String getterName) {
        if ((getterName == null)? this.getterName != null : !getterName.equals(this.getterName)) {
            this.getterName = getterName;
            this.descriptor = null;
        }
    }

    public void getter(Method getter) {
        if ((getter == null)? this.getter != null : !getter.equals(this.getter)) {
            this.getter = getter;
            this.descriptor = null;
        }
    }

    public ReadOnlyPropertyDescriptor getDescriptor() throws NoSuchMethodException {
        if (descriptor == null) {
            if ((propertyName == null) || (bean == null)) {
                throw new NullPointerException("Bean and property name have to be specified");
            }
            if (propertyName.isEmpty()) {
                throw new IllegalArgumentException("Property name cannot be empty");
            }
            final String capitalizedName = ReadOnlyPropertyDescriptor.capitalizedName(propertyName);
            if (getter == null) {
                if ((getterName != null) && !getterName.isEmpty()) {
                    getter = beanClass.getMethod(getterName);
                } else {
                    try {
                        getter = beanClass.getMethod(IS_PREFIX + capitalizedName);
                    } catch (NoSuchMethodException e) {
                        getter = beanClass.getMethod(GET_PREFIX + capitalizedName);
                    }
                }
            }
            descriptor = new ReadOnlyPropertyDescriptor(propertyName, beanClass, getter);
        }
        return descriptor;
    }

}
