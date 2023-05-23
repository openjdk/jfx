/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.demo.rich;

import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.model.SyntaxDecorator;

/**
 * Simple Syntax Highlighter which emphasizes digits.
 */
public class DemoSyntaxDecorator implements SyntaxDecorator {
    private static final String DIGITS = "-fx-fill:magenta;";

    public DemoSyntaxDecorator() {
    }

    @Override
    public TextCell createTextCell(int index, String text) {
        TextCell cell = new TextCell(index);
        if (text != null) {
            int start = 0;
            int sz = text.length();
            boolean num = false;
            for (int i = 0; i < sz; i++) {
                char c = text.charAt(i);
                if (num != Character.isDigit(c)) {
                    if (i > start) {
                        String s = text.substring(start, i);
                        String style = num ? DIGITS : null;
                        cell.addSegment(s, style, null);
                        start = i;
                    }
                    num = !num;
                }
            }
            if (start < sz) {
                String s = text.substring(start);
                String style = num ? DIGITS : null;
                cell.addSegment(s, style, null);
            }
        }
        return cell;
    }
}
