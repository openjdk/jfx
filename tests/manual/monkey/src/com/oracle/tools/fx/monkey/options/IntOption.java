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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Spinner;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Int Option Bound to a Property.
 */
public class IntOption extends Spinner<Integer> {
    private final SimpleIntegerProperty property = new SimpleIntegerProperty();

    public IntOption(String name, int min, int max, IntegerProperty p) {
        super(min, max, p == null ? min : p.get());

        FX.name(this, name);
        setEditable(true);

        if (p != null) {
            property.bindBidirectional(p);
        }

        valueProperty().addListener((s, pr, val) -> {
            property.set(val);
        });
    }

    public IntOption(String name, int min, int max, int value) {
        this(name, min, max, null);
        getValueFactory().setValue(value);
    }
}
