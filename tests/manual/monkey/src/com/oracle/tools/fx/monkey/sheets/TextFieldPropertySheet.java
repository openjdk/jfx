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

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * TextField property sheet.
 */
public class TextFieldPropertySheet {
    public static void appendTo(OptionPane op, TextField control, Runnable r) {
        op.section("TextField");
        op.option("Alignment:", new EnumOption<>("alignment", false, Pos.class, control.alignmentProperty()));
        op.option("Preferred Column Count:", new IntOption("prefColumnCount", -1, Integer.MAX_VALUE, control.prefColumnCountProperty()));
        if (r != null) {
            r.run();
        }

        TextInputControlPropertySheet.appendTo(op, false, control);
    }
}
