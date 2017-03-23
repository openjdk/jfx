/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.SimpleIntegerProperty;

/**
 * This class extends {@code SimpleIntegerProperty} and provides a full
 * implementation of a {@code StyleableProperty}.
 *
 * This class is used to make a {@link javafx.beans.property.IntegerProperty},
 * that would otherwise be implemented as a {@link SimpleIntegerProperty},
 * styleable by CSS.
 *
 * @see javafx.beans.property.SimpleIntegerProperty
 * @see CssMetaData
 * @see StyleableProperty
 * @see StyleableIntegerProperty
 * @since JavaFX 8.0
 */
public class SimpleStyleableIntegerProperty extends StyleableIntegerProperty {

    private static final Object DEFAULT_BEAN = null;
    private static final String DEFAULT_NAME = "";

    private final Object bean;
    private final String name;
    private final CssMetaData<? extends Styleable, Number> cssMetaData;

    /**
     * The constructor of the {@code SimpleStyleableIntegerProperty}.
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     */
    public SimpleStyleableIntegerProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData) {
        this(cssMetaData, DEFAULT_BEAN, DEFAULT_NAME);
    }

    /**
     * The constructor of the {@code SimpleStyleableIntegerProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public SimpleStyleableIntegerProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData, @NamedArg("initialValue") Integer initialValue) {
        this(cssMetaData, DEFAULT_BEAN, DEFAULT_NAME, initialValue);
    }

    /**
     * The constructor of the {@code SimpleStyleableIntegerProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code IntegerProperty}
     * @param name
     *            the name of this {@code IntegerProperty}
     */
    public SimpleStyleableIntegerProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData, @NamedArg("bean") Object bean, @NamedArg("name") String name) {
        this.bean = bean;
        this.name = (name == null) ? DEFAULT_NAME : name;
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableIntegerProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code IntegerProperty}
     * @param name
     *            the name of this {@code IntegerProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public SimpleStyleableIntegerProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData, @NamedArg("bean") Object bean, @NamedArg("name") String name, @NamedArg("initialValue") Integer initialValue) {
        super(initialValue);
        this.bean = bean;
        this.name = (name == null) ? DEFAULT_NAME : name;
        this.cssMetaData = cssMetaData;
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
    public final CssMetaData<? extends Styleable, Number> getCssMetaData() {
        return cssMetaData;
    }

}
