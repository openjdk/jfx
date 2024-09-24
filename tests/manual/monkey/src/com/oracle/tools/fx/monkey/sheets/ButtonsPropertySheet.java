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

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Button(s) property sheet.
 */
public class ButtonsPropertySheet {
    public static void appendTo(OptionPane op, Button n) {
        op.section("Button");
        op.option(new BooleanOption("cancelButton", "cancel button", n.cancelButtonProperty()));
        op.option(new BooleanOption("defaultButton", "default button", n.defaultButtonProperty()));

        LabeledPropertySheet.appendTo(op, "Labeled", false, n);
    }

    public static void appendTo(OptionPane op, CheckBox n) {
        op.section("CheckBox");
        op.option(new BooleanOption("allowIndeterminate", "allow indeterminate", n.allowIndeterminateProperty()));
        op.option(new BooleanOption("indeterminate", "indeterminate", n.indeterminateProperty()));
        op.option(new BooleanOption("selected", "selected", n.selectedProperty()));

        LabeledPropertySheet.appendTo(op, "Labeled", false, n);
    }

    public static void appendTo(OptionPane op, Hyperlink n) {
        op.section("Hyperlink");
        op.option(new BooleanOption("visited", "visited", n.visitedProperty()));

        LabeledPropertySheet.appendTo(op, "Labeled", false, n);
    }

    public static void appendTo(OptionPane op, RadioButton n, ToggleGroup g) {
        op.section("RadioButton");
        op.option(new BooleanOption("selectedButton", "selected", n.selectedProperty()));
        if (g != null)
            op.option(new BooleanOption("toggleGroup", "part of a toggle group", (v) -> {
                n.setToggleGroup(v ? g : null);
            }));

        LabeledPropertySheet.appendTo(op, "Labeled", false, n);
    }

    public static void appendTo(OptionPane op, ToggleButton n, ToggleGroup g) {
        op.section("ToggleButton");
        op.option(new BooleanOption("selectedButton", "selected", n.selectedProperty()));
        if (g != null) {
            op.option(new BooleanOption("toggleGroup", "part of a toggle group", (v) -> {
                n.setToggleGroup(v ? g : null);
            }));
        }

        LabeledPropertySheet.appendTo(op, "Labeled", false, n);
    }
}
