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

package com.oracle.demo.rich.common;

import jfx.incubator.scene.control.rich.model.StyleAttribute;
import jfx.incubator.scene.control.rich.model.StyleAttrs;

public class Styles {
    // TODO perhaps we should specifically set fonts to be used,
    // and couple that with the app stylesheet
    public static final StyleAttrs TITLE = s("System", 24, true);
    public static final StyleAttrs HEADING = s("System", 18, true);
    public static final StyleAttrs SUBHEADING = s("System", 14, true);
    public static final StyleAttrs BODY = s("System", 12, false);
    public static final StyleAttrs MONOSPACED = s("Monospace", 12, false);

    private static StyleAttrs s(String font, double size, boolean bold) {
        return StyleAttrs.builder().
            setFontFamily(font).
            setFontSize(size).
            setBold(bold).
            build();
    }

    public static StyleAttrs getStyleAttrs(TextStyle st) {
        switch (st) {
        case BODY:
            return BODY;
        case HEADING:
            return HEADING;
        case MONOSPACED:
            return MONOSPACED;
        case TITLE:
            return TITLE;
        case SUBHEADING:
            return SUBHEADING;
        default:
            return BODY;
        }
    }

    public static TextStyle guessTextStyle(StyleAttrs attrs) {
        if (attrs != null) {
            if (attrs.isEmpty()) {
                return TextStyle.BODY;
            }
            StyleAttribute<?>[] keys = {
                StyleAttrs.BOLD,
                StyleAttrs.FONT_FAMILY,
                StyleAttrs.FONT_SIZE
            };
            for (TextStyle st : TextStyle.values()) {
                StyleAttrs a = getStyleAttrs(st);
                if (match(attrs, a, keys)) {
                    return st;
                }
            }
        }
        return null;
    }

    private static boolean match(StyleAttrs attrs, StyleAttrs builtin, StyleAttribute<?>[] keys) {
        for (StyleAttribute<?> k : keys) {
            Object v1 = attrs.get(k);
            Object v2 = builtin.get(k);
            if (!eq(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean eq(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
