/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import javafx.beans.NamedArg;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * This class extends {@code SimpleBooleanProperty} and provides a full
 * implementation of a {@code StyleableProperty}.
 *
 * This class is used to make a {@link javafx.beans.property.BooleanProperty},
 * that would otherwise be implemented as a {@link SimpleBooleanProperty},
 * styleable by CSS.
 *
 * @see javafx.beans.property.SimpleBooleanProperty
 * @see CssMetaData
 * @see StyleableProperty
 * @see StyleableBooleanProperty
 * @since JavaFX 8.0
 */
public class SimpleStyleableBooleanProperty extends StyleableBooleanProperty {

    /** Sentinel value that indicates that a property was not declared in another class. */
    private static final Class<?> NO_DECLARING_CLASS = SimpleStyleableBooleanProperty.class;
    private static final Object DEFAULT_BEAN = null;
    private static final String DEFAULT_NAME = "";

    private final Object bean;
    private final String name;
    private final CssMetaData<? extends Styleable, Boolean> cssMetaData;
    private Class<?> declaringClass;

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData) {
        this(cssMetaData, DEFAULT_BEAN, DEFAULT_NAME);
    }

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     * @param initialValue the initial value
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
                                          @NamedArg("initialValue") boolean initialValue) {
        this(cssMetaData, DEFAULT_BEAN, DEFAULT_NAME, initialValue);
    }

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     * @param bean the bean of this property
     * @param name the name of this property
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
                                          @NamedArg("bean") Object bean,
                                          @NamedArg("name") String name) {
        this.bean = bean;
        this.name = (name == null) ? DEFAULT_NAME : name;
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     * @param bean the bean of this property
     * @param name the name of this property
     * @param initialValue the initial value
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
                                          @NamedArg("bean") Object bean,
                                          @NamedArg("name") String name,
                                          @NamedArg("initialValue") boolean initialValue) {
        super(initialValue);
        this.bean = bean;
        this.name = (name == null) ? DEFAULT_NAME : name;
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     * @param bean the bean of this property
     * @param declaringClass the class in which this property is declared
     * @param name the name of this property
     * @since 28
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
                                          @NamedArg("bean") Object bean,
                                          @NamedArg("declaringClass") Class<?> declaringClass,
                                          @NamedArg("name") String name) {
        this(cssMetaData, bean, name);
        this.declaringClass = declaringClass != null ? declaringClass : NO_DECLARING_CLASS;
    }

    /**
     * The constructor of the {@code SimpleStyleableBooleanProperty}.
     *
     * @param cssMetaData the {@code CssMetaData} associated with this property
     * @param bean the bean of this property
     * @param declaringClass the class in which this property is declared
     * @param name the name of this property
     * @param initialValue the initial value
     * @since 28
     */
    public SimpleStyleableBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
                                          @NamedArg("bean") Object bean,
                                          @NamedArg("declaringClass") Class<?> declaringClass,
                                          @NamedArg("name") String name,
                                          @NamedArg("initialValue") boolean initialValue) {
        this(cssMetaData, bean, name, initialValue);
        this.declaringClass = declaringClass != null ? declaringClass : NO_DECLARING_CLASS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getBean() {
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public final CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
        return cssMetaData;
    }

    /**
     * {@inheritDoc}
     *
     * @since 28
     */
    @Override
    public Class<?> getDeclaringClass() {
        if (declaringClass != null) {
            return declaringClass != NO_DECLARING_CLASS ? declaringClass : null;
        }

        Class<?> declaringClass = super.getDeclaringClass();
        this.declaringClass = declaringClass != null ? declaringClass : NO_DECLARING_CLASS;
        return declaringClass;
    }
}
