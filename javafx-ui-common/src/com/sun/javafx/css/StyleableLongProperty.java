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

import javafx.beans.property.LongPropertyBase;
import javafx.beans.value.ObservableValue;


public abstract class StyleableLongProperty 
    extends LongPropertyBase implements Property<Long> {

    /**
     * The constructor of the {@code StyleableLongProperty}.
     */
    public StyleableLongProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableLongProperty}.
     * 
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableLongProperty(long initialValue) {
        super(initialValue);
    }
    
    Stylesheet.Origin origin = null;
    
    @Override
    public Stylesheet.Origin getOrigin() { return origin; }
    
    @Override
    public void applyStyle(Stylesheet.Origin origin, Long v) {
        // call set here in case it has been overridden in the javafx.beans.property
        set(v.longValue());
        this.origin = origin;
    }
    
    // CSS uses double everywhere
    public void applyStyle(Stylesheet.Origin origin, Double v) {
        // call set here in case it has been overridden in the javafx.beans.property
        set(v.longValue());
        this.origin = origin;
    }
            
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        super.bind(observable);
        origin = Stylesheet.Origin.USER;
    }

    @Override
    public void set(long v) {
        super.set(v);
        origin = Stylesheet.Origin.USER;
    }
    
}
