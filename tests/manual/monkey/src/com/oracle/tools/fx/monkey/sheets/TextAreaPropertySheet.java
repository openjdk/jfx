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

import javafx.scene.control.TextArea;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * TextArea property sheet.
 */
public class TextAreaPropertySheet {
    public static void appendTo(OptionPane op, TextArea control) {
        op.section("TextArea");
        op.option("Preferred Column Count:", new IntOption("prefColumnCount", -1, Integer.MAX_VALUE, control.prefColumnCountProperty()));
        op.option("Preferred Row Count:", new IntOption("prefRowCount", -1, Integer.MAX_VALUE, control.prefRowCountProperty()));
        op.option("Scroll Left: TODO", null); // TODO
        op.option("Scroll Top: TODO", null); // TODO
        op.option(new BooleanOption("wrapText", "wrap text", control.wrapTextProperty()));

        TextInputControlPropertySheet.appendTo(op, true, control);
    }
}
