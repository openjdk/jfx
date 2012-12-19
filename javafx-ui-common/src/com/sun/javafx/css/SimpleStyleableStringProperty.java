/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

/**
 * This class extends {@code SimpleStringProperty} and provides a full
 * implementation of a {@code StyleableProperty}. The method 
 * {@link StyleableProperty#getCssMetaData()} is not implemented. 
 * 
 * This class is used to make a {@link javafx.beans.property.StringProperty}, 
 * that would otherwise be implemented as a {@link SimpleStringProperty}, 
 * style&#8209;able by CSS.
 * 
 * @see javafx.beans.property.SimpleStringProperty
 * @see CssMetaData
 * @see StyleableProperty
 */
public abstract class SimpleStyleableStringProperty
    extends SimpleStringProperty implements StyleableProperty<String> {

    /**
     * The constructor of the {@code SimpleStyleableStringProperty}.
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     */
    public SimpleStyleableStringProperty(CssMetaData cssMetaData) {
        super();
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableStringProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public SimpleStyleableStringProperty(CssMetaData cssMetaData, String initialValue) {
        super(initialValue);
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableStringProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code StringProperty}
     * @param name
     *            the name of this {@code StringProperty}
     */
    public SimpleStyleableStringProperty(CssMetaData cssMetaData, Object bean, String name) {
        super(bean, name);
        this.cssMetaData = cssMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(Origin origin, String v) {
        // call set here in case the set method is overriden
        set(v);
        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends String> observable) {
        super.bind(observable);
        origin = Origin.USER;
    }

    /** {@inheritDoc} */
    @Override
    public void set(String v) {
        super.set(v);
        origin = Origin.USER;
    }

    /** {@inheritDoc} */
    @Override
    public final Origin getOrigin() { return origin; }

    /** {@inheritDoc} */
    @Override
    public final CssMetaData getCssMetaData() {
        return cssMetaData;
    }

    private Origin origin = null;
    private final CssMetaData cssMetaData;

}
