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
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;

/**
 * Paint Option Bound to a Property.
 */
public class PaintOption extends ObjectOption<Paint> {
    public PaintOption(String name, Property<Paint> p) {
        super(name, p);

        addChoice("Red", Color.RED);
        addChoice("Green", Color.GREEN);
        addChoice("Blue", Color.BLUE);
        addChoice("Black", Color.BLACK);
        addChoice("White", Color.WHITE);
        // TODO image
        // TODO linear gradient
        addChoiceSupplier("Linear Gradient", () -> {
            double startX = 0;
            double startY = 0;
            double endX = 1;
            double endY = 1;
            boolean proportional = true;
            CycleMethod cycleMethod = CycleMethod.REPEAT;
            Stop[] stops = {
                new Stop(0, Color.WHITE),
                new Stop(1, Color.BLACK)
            };
            return new LinearGradient(
                startX,
                startY,
                endX,
                endY,
                proportional,
                cycleMethod,
                stops
            );
        });
        // TODO radial gradient
        addChoice("<null>", null);
        selectInitialValue();
    }
}
