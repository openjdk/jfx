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
package com.oracle.tools.fx.monkey.sheets;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.options.TextChoiceOption;
import com.oracle.tools.fx.monkey.util.TextTemplates;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Convenience Methods.
 */
public class Options {
    /**
     * Returns the text choice option initialized with common single- or multi-line strings.
     * @param name
     * @param multiLine
     * @param allowEditButton
     * @param prop
     * @return
     */
    public static Node textOption(String name, boolean multiLine, boolean allowEditButton, StringProperty prop) {
        TextChoiceOption op = new TextChoiceOption(name, true, prop);
        Object[] pairs = multiLine ? TextTemplates.multiLineTextPairs() : TextTemplates.singleLineTextPairs();
        Utils.fromPairs(pairs, (k,v) -> op.addChoice(k, v));
        op.selectFirst();
        return op;
    }

    public static Node fixedSizeOption(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 20, 33.4, 50, 100);
    }

    public static Node spacing(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 0.333, 0.5, 1, 2, 10, 20, 33.4, 50, 100);
    }

    public static Node gaps(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 1, 1.5, 4, 10, 20, 33.33, 100);
    }

    public static Node tabPaneConstraints(String name, DoubleProperty p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("Double.NaN", Double.NaN);
        d.selectInitialValue();
        return d;
    }

    public static Node forRegion(String name, Property<Number> p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("USE_COMPUTED_SIZE (-1)", Region.USE_COMPUTED_SIZE);
        d.addChoice("USE_PREF_SIZE (-∞)", Region.USE_PREF_SIZE);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.MIN_VALUE", Double.MIN_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("Double.NaN", Double.NaN);
        d.selectInitialValue();
        return d;
    }

    public static Node lineSpacing(String name, Property<Number> p) {
        return DoubleOption.of(name, p, 0, 0.5, 1, 2, 3.14, 10, 33.33, 100);
    }

    public static Node background(Node owner, String name, Property<Background> p) {
        ObjectOption<Background> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("Black", () -> {
            return Background.fill(Color.BLACK);
        });
        op.addChoiceSupplier("Red", () -> {
            return Background.fill(Color.RED);
        });
        op.addChoiceSupplier("White", () -> {
            return Background.fill(Color.WHITE);
        });
        // TODO let background property track focused and focusWithin properties to change the bg
        // also make sure to removeListener when the background is set to another value
//        op.addChoiceSupplier("Focus(Green), NoFocus(Gray)", () -> {
//            BooleanBinding b = Bindings.createBooleanBinding(
//                () -> {
//                    
//                },
//                owner.focusTraversableProperty(),
//                owner.focusedProperty(),
//                owner.focusWithinProperty()
//            );
//            Background bg = new Background();
//        });
        op.addChoice("<null>", null);
        op.selectInitialValue();
        return op;
    }

    public static Node tabSize(String name, IntegerProperty p) {
        return new IntOption(name, 0, Integer.MAX_VALUE, p);
    }

    public static Node placeholderNode(String name, ObjectProperty<Node> p) {
        ObjectOption<Node> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("Button", () -> new Button("Placeholder"));
        op.addChoiceSupplier("Label", () -> new Label("Placeholder"));
        op.addChoice("<null>", null);
        op.selectInitialValue();
        return op;
    }
}
