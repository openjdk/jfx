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
package javafx.css;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * This class extends {@code SimpleDoubleProperty} and provides a full
 * implementation of a {@code StyleableProperty}. The method 
 * {@link StyleableProperty#getCssMetaData()} is not implemented. 
 * 
 * This class is used to make a {@link javafx.beans.property.DoubleProperty}, 
 * that would otherwise be implemented as a {@link SimpleDoubleProperty}, 
 * style&#8209;able by CSS.
 * 
 * @see javafx.beans.property.SimpleDoubleProperty
 * @see CssMetaData
 * @see StyleableProperty
 */
@com.sun.javafx.beans.annotations.NoBuilder
public abstract class SimpleStyleableDoubleProperty
    extends SimpleDoubleProperty implements StyleableProperty<Number> {

    /**
     * The constructor of the {@code SimpleStyleableDoubleProperty}.
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     */
    public SimpleStyleableDoubleProperty(CssMetaData<? extends Styleable, Number> cssMetaData) {
        super();
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableDoubleProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public SimpleStyleableDoubleProperty(CssMetaData<? extends Styleable, Number> cssMetaData, double initialValue) {
        super(initialValue);
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableDoubleProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code DoubleProperty}
     * @param name
     *            the name of this {@code DoubleProperty}
     */
    public SimpleStyleableDoubleProperty(CssMetaData<? extends Styleable, Number> cssMetaData, Object bean, String name) {
        super(bean, name);
        this.cssMetaData = cssMetaData;
    }

    /**
     * The constructor of the {@code SimpleStyleableDoubleProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code DoubleProperty}
     * @param name
     *            the name of this {@code DoubleProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public SimpleStyleableDoubleProperty(CssMetaData<? extends Styleable, Number> cssMetaData, Object bean, String name, double initialValue) {
        super(bean, name, initialValue);
        this.cssMetaData = cssMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, Number v) {
        setValue(v);
        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        super.bind(observable);
        origin = StyleOrigin.USER;
    }

    /** {@inheritDoc} */
    @Override
    public void set(double v) {
        super.set(v);
        origin = StyleOrigin.USER;
    }


    /** {@inheritDoc} */
    @Override
    public final StyleOrigin getStyleOrigin() { return origin; }

    /** {@inheritDoc} */
    @Override
    public final CssMetaData<? extends Styleable, Number> getCssMetaData() {
        return cssMetaData;
    }

    private StyleOrigin origin = null;
    private final CssMetaData<? extends Styleable, Number> cssMetaData;
}
