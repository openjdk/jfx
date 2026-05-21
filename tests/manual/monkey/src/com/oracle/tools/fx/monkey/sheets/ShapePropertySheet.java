/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.sheets;

import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.PaintOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Shape Properties Sheet
 */
public class ShapePropertySheet {
    public static void appendTo(OptionPane op, Shape n) {
        op.section("Shape");
        op.option("Fill:", new PaintOption("fill", n.fillProperty()));
        op.option(new BooleanOption("smooth", "smooth", n.smoothProperty()));
        op.option("Stroke:", new PaintOption("stroke", n.strokeProperty()));
        op.option("Stroke Dash Offset:", new DoubleSpinner("strokeDashOffset", 0, 100, 0.1, n.strokeDashOffsetProperty()));
        op.option("Stroke Line Cap:", new EnumOption<>("strokeLineCap", StrokeLineCap.class, n.strokeLineCapProperty()));
        op.option("Stroke Line Join:", new EnumOption<>("strokeLineJoin", StrokeLineJoin.class, n.strokeLineJoinProperty()));
        op.option("Stroke Miter Limit:", new DoubleSpinner("strokeMeterLimit", 0, 100, 0.1, n.strokeMiterLimitProperty()));
        op.option("Stroke Type:", new EnumOption<>("strokeType", StrokeType.class, n.strokeTypeProperty()));
        op.option("Stroke Width:", new DoubleSpinner("strokeWidth", 0, 100, 0.1, n.strokeWidthProperty()));

        NodePropertySheet.appendTo(op, n);
    }
}
