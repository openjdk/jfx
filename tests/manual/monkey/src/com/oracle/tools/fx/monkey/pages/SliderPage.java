/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Slider Page.
 */
public class SliderPage extends TestPaneBase implements HasSkinnable {
    private final Slider control;

    public SliderPage() {
        super("SliderPage");

        control = new Slider() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        OptionPane op = new OptionPane();
        op.section("Slider");
        op.option("Block Increment:", DoubleOption.of("blockIncrement", control.blockIncrementProperty(), 0.0, 1.0));
        op.option("Label Formatter:", createLabelFormatterOption("labelFormatter", control.labelFormatterProperty()));
        op.option("Major Tick Unit:", DoubleOption.of("majorTickUnit", control.majorTickUnitProperty(), 1.0, 10.0));
        op.option("Max:", DoubleOption.of("max", control.maxProperty(), 100.0));
        op.option("Min:", DoubleOption.of("min", control.minProperty(), 0.0));
        op.option("Minor Tick Count:", new IntOption("minorTickUnit", 0, Integer.MAX_VALUE, control.minorTickCountProperty()));
        op.option("Orientation:", new EnumOption<>("orientation", true, Orientation.class, control.orientationProperty()));
        op.option("Value:", DoubleOption.of("value", control.valueProperty(), 0.0));
        op.option(new BooleanOption("valueChanging", "value changing", control.valueChangingProperty()));
        op.option(new BooleanOption("showTickLabels", "show tick labels", control.showTickLabelsProperty()));
        op.option(new BooleanOption("showTickMarks", "show tick marks", control.showTickMarksProperty()));
        op.option(new BooleanOption("snapToTicks", "snap to ticks", control.snapToTicksProperty()));
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private Node createLabelFormatterOption(String name, ObjectProperty<StringConverter<Double>> p) {
        var original = p.get();
        ObjectOption<StringConverter<Double>> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("Quoted", () -> {
            return new StringConverter<Double>() {
                @Override
                public String toString(Double x) {
                    return "\"" + x + "\"";
                }

                @Override
                public Double fromString(String s) {
                    return s == null ? null : Double.parseDouble(s);
                }
            };
        });
        op.addChoiceSupplier("Number", () -> {
            return new StringConverter<Double>() {
                @Override
                public String toString(Double x) {
                    return x == null ? null : String.valueOf(x);
                }

                @Override
                public Double fromString(String s) {
                    return s == null ? null : Double.parseDouble(s);
                }
            };
        });
        op.addChoice("<default>", original);
        op.addChoice("<null>", null);
        op.selectInitialValue();
        return op;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new SliderSkin(control));
    }
}
