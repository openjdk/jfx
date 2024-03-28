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

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;

/**
 * Insets Option Bound to a Property.
 */
public class InsetsOption extends ObjectOption<Insets> {
    public InsetsOption(String name, boolean allowNull, ObjectProperty<Insets> p) {
        super(name, p);

        if (allowNull) {
            addChoice("<null>", null);
        }
        addChoice("0", new Insets(0));
        addChoice("10", new Insets(10));
        addChoice("100", new Insets(100));
        addChoice("10, 20, 40, 80", new Insets(10, 20, 40, 80));

        selectInitialValue();
    }
}
