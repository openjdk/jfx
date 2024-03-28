/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.options;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Boolean Option Bound to a Property.
 */
public class BooleanOption extends CheckBox {
    private final SimpleBooleanProperty property = new SimpleBooleanProperty();

    public BooleanOption(String name, String text, Property<Boolean> p) {
        FX.name(this, name);
        if (p != null) {
            property.bindBidirectional(p);
        }

        setText(text);
        selectedProperty().bindBidirectional(property);
    }

    public BooleanOption(String name, String text, Runnable onChange) {
        this(name, text, (Property)null);

        property.addListener((src, prev, cur) -> {
            try {
                onChange.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public boolean getValue() {
        return property.get();
    }
}
