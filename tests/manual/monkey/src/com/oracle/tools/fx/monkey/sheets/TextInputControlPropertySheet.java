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

import javafx.scene.control.TextInputControl;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.FontOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Text Input Control Property Sheet.
 */
public class TextInputControlPropertySheet {
    public static void appendTo(OptionPane op, boolean multiLine, TextInputControl control) {
        op.section("TextInputControl");
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Font:", new FontOption("font", false, control.fontProperty()));
        op.option("Prompt Text:", Options.promptText("promptText", true, control.promptTextProperty()));
        op.option("Text:", Options.textOption("text", multiLine, true, control.textProperty()));
        op.option("Text Formatter: TODO", null); // TODO

        ControlPropertySheet.appendTo(op, control);
    }
}
