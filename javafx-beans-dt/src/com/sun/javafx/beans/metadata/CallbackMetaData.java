/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.metadata;

import java.lang.reflect.Method;

/**
 * A CallbackMetaData is a specialization of a PropertyMetaData. All callbacks
 * in JavaFX are simply a special type of property. Although named differently,
 * they all have a data type of {@link javafx.util.Callback}.
 *
 * @author Richard
 */
public class CallbackMetaData extends PropertyMetaData {
    /**
     * This is the type of the param passed to the Callback's call method.
     */
    private Class<?> paramType;

    /**
     * This is the return type of the Callback's call method.
     */
    private Class<?> returnType;

    /**
     * Creates a new CallbackMetaData based on the given beanClass and getter.
     * Both the beanClass and getter must be specified or a NullPointerException
     * will be thrown. The getter must be a method on the specified beanClass,
     * and it must have a return type of {@link javafx.util.Callback}, or an
     * IllegalArgumentException will be thrown.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter on the bean class of the property,
     *        cannot be null and must have a return type of
     *        {@link javafx.util.Callback}
     */
    public CallbackMetaData(Class<?> beanClass, Method getter) {
        super(beanClass, getter);
        init(getter);
    }

    /**
     * A constructor used by BeanMetaData to create a CallbackMetaData without
     * having to do redundant checks and redundant resource bundle lookup.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter, cannot be null
     * @param bundle The bundle, cannot be null
     */
    CallbackMetaData(Class<?> beanClass, Method getter, Resources bundle) {
        super(beanClass, getter, bundle);
        init(getter);
    }

    /**
     * @InheritDoc
     */
    @Override MetaDataAnnotation getMetaDataAnnotation(Method getter) {
        final Callback a = getter.getAnnotation(Callback.class);
        if (a == null) return null;
        return new MetaDataAnnotation() {
            @Override public String displayName() {
                return a.displayName();
            }

            @Override public String shortDescription() {
                return a.shortDescription();
            }

            @Override public String category() {
                return a.category();
            }
        };
    }

    /**
     * Extracts annotation information from the Callback annotation specific to
     * the Callback.
     *
     * @param getter The getter on the bean class of the property
     */
    private void init(Method getter) {
        // Get the annotations on this method. Look for the event specific
        // "paramType" and "returnType" annotations and use them if specified
        Callback callbackAnnotation = getter.getAnnotation(Callback.class);
        paramType = callbackAnnotation == null ?
                Object.class :
                callbackAnnotation.paramType();
        returnType = callbackAnnotation == null ?
                Object.class :
                callbackAnnotation.returnType();
    }

    /**
     * Gets the type of the parameter will be which is passed to the
     * {@link javafx.util.Callback}'s call method.
     *
     * @return The type of the parameter
     */
    public final Class<?> getParamType() {
        return paramType;
    }

    /**
     * Gets the return type of the {@link javafx.util.Callback}'s call method.
     *
     * @return The return type
     */
    public final Class<?> getReturnType() {
        return returnType;
    }
}
