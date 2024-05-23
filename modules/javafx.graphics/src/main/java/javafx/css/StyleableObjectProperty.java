/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ObservableValue;

/**
 * This class extends {@code ObjectPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.ObjectProperty},
 * that would otherwise be implemented as a {@link ObjectPropertyBase},
 * styleable by CSS.
 *
 * @param <T> the property value type
 * @see javafx.beans.property.ObjectPropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableObjectProperty<T>
    extends ObjectPropertyBase<T> implements StyleableProperty<T> {

    /**
     * The constructor of the {@code StyleableObjectProperty}.
     */
    public StyleableObjectProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableObjectProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableObjectProperty(T initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, T v) {
        set(v);
        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends T> observable) {
        super.bind(observable);
        origin = StyleOrigin.USER;
    }

    /** {@inheritDoc} */
    @Override
    public void set(T v) {
        super.set(v);
        origin = StyleOrigin.USER;
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;

}
