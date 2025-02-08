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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Styleable Property Sheet.
 */
public class StyleablePropertySheet {
    public static void appendTo(OptionPane op, Styleable n) {
        op.section("Styleable");

        ArrayList<CssMetaData<? extends Styleable, ?>> ss = new ArrayList<>(n.getCssMetaData());
        Collections.sort(ss, new Comparator<CssMetaData>() {
            @Override
            public int compare(CssMetaData a, CssMetaData b) {
                return a.getProperty().compareTo(b.getProperty());
            }
        });

        for (CssMetaData md : ss) {
            // TODO
            // Node ed = createOption(n, md);
            // op.option(md.getProperty(), ed);
            String val = md.getProperty(); // + ": " + md.getStyleableProperty(n).getValue();
            op.option(val, null);
        }
    }

    private static Node createOption(Styleable s, CssMetaData md) {
        // TODO move up
//        if (!md.isSettable(s))
//        {
//            return null;
//        }
        return new Label(String.valueOf(md.getStyleableProperty(s).getValue()));
    }
}
