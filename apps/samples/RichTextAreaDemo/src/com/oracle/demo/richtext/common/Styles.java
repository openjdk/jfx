/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.common;

import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Static Styles for the Rich Text Editor Demo app.
 *
 * @author Andy Goryachev
 */
public class Styles {
    // TODO perhaps we should specifically set fonts to be used,
    // and couple that with the app stylesheet
    public static final StyleAttributeMap TITLE = mkStyle("System", 24, true);
    public static final StyleAttributeMap HEADING = mkStyle("System", 18, true);
    public static final StyleAttributeMap SUBHEADING = mkStyle("System", 14, true);
    public static final StyleAttributeMap BODY = mkStyle("System", 12, false);
    public static final StyleAttributeMap MONOSPACED = mkStyle("Monospace", 12, false);

    private static final StyleAttribute<?>[] STD_ATTRS = {
        StyleAttributeMap.BOLD,
        StyleAttributeMap.FONT_FAMILY,
        StyleAttributeMap.FONT_SIZE,
        StyleAttributeMap.TEXT_COLOR,
    };

    private static StyleAttributeMap mkStyle(String font, double size, boolean bold) {
        return StyleAttributeMap.builder().
            setFontFamily(font).
            setFontSize(size).
            setBold(bold).
            setTextColor(Color.BLACK).
            build();
    }

    public static StyleAttributeMap getStyleAttributeMap(TextStyle st) {
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

    public static TextStyle guessTextStyle(StyleAttributeMap attrs) {
        if (attrs != null) {
            if (attrs.isEmpty()) {
                return TextStyle.BODY;
            }
            for (TextStyle st : TextStyle.values()) {
                StyleAttributeMap a = getStyleAttributeMap(st);
                if (match(attrs, a, STD_ATTRS)) {
                    return st;
                }
            }
        }
        return null;
    }

    private static boolean match(StyleAttributeMap attrs, StyleAttributeMap builtin, StyleAttribute<?>[] keys) {
        for (StyleAttribute<?> k : keys) {
            Object v1 = attrs.get(k);
            Object v2 = builtin.get(k);
            if (k.getType() == Boolean.class) {
                if (getBoolean(v1) != getBoolean(v2)) {
                    return false;
                }
            } else {
                if (!eq(v1, v2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean getBoolean(Object x) {
        return Boolean.TRUE.equals(x);
    }

    private static boolean eq(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
