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

import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class ParagraphAttributesDemoModel extends SimpleReadOnlyStyledModel {
    private final StyleAttrs TITLE = StyleAttrs.builder().
        setFontSize(200).
        setUnderline(true).
        build();

    public ParagraphAttributesDemoModel() {
        addSegment("Paragraph Attributes", TITLE);
        nl(2);

        addSegment("✓ Opaque Background Color");
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.LIGHTGREEN).
            build());
        nl();

        addSegment("✓ Translucent Background Color");
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(FX.alpha(Color.LIGHTGREEN, 0.5)).
            build());
        nl();

        // space

        addSegment("✓ Space Above");
        setParagraphAttributes(StyleAttrs.
            builder().
            setSpaceAbove(20).
            setBackground(Color.gray(0.95, 0.5)).
            build());
        nl();

        addSegment("✓ Space Below");
        setParagraphAttributes(StyleAttrs.
            builder().
            setSpaceBelow(20).
            setBackground(Color.gray(0.9, 0.5)).
            build());
        nl();

        addSegment("✓ Space Left");
        setParagraphAttributes(StyleAttrs.
            builder().
            setSpaceLeft(20).
            setBackground(Color.gray(0.85, 0.5)).
            build());
        nl();

        addSegment("✓ Space Right " + words(10));
        setParagraphAttributes(StyleAttrs.
            builder().
            setSpaceRight(20).
            setBackground(Color.gray(0.8, 0.5)).
            build());
        nl();

        // text alignment

        addSegment("✓ Text Alignment Left " + words(20));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.95, 0.5)).
            setTextAlignment(TextAlignment.LEFT).
            build());
        nl();

        addSegment("✓ Text Alignment Right " + words(20));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.9, 0.5)).
            setTextAlignment(TextAlignment.RIGHT).
            build());
        nl();

        addSegment("✓ Text Alignment Center " + words(20));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.85, 0.5)).
            setTextAlignment(TextAlignment.CENTER).
            build());
        nl();

        addSegment("✓ Text Alignment Justify " + words(20));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.8, 0.5)).
            setTextAlignment(TextAlignment.JUSTIFY).
            build());
        nl();

        // line spacing

        addSegment("✓ Line Spacing 0 " + words(200));
        highlight(50, 100, FX.alpha(Color.RED, 0.4));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.95, 0.5)).
            setLineSpacing(0).
            build());
        nl();

        addSegment("✓ Line Spacing 20 " + words(200));
        highlight(50, 100, FX.alpha(Color.RED, 0.4));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.9, 0.5)).
            setLineSpacing(20).
            build());
        nl();

        addSegment("✓ Line Spacing 40 " + words(200));
        highlight(50, 100, FX.alpha(Color.RED, 0.4));
        setParagraphAttributes(StyleAttrs.
            builder().
            setBackground(Color.gray(0.9, 0.5)).
            setLineSpacing(40).
            build());
        nl();
    }

    private static String words(int count) {
        String[] lorem = {
            "Lorem",
            "ipsum",
            "dolor",
            "sit",
            "amet,",
            "consectetur",
            "adipiscing",
            "elit,",
            "sed",
            "do",
            "eiusmod",
            "tempor",
            "incididunt",
            "ut",
            "labore",
            "et",
            "dolore",
            "magna",
            "aliqua"
        };

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<count; i++) {
            if(i > 0) {
                sb.append(' ');
            }
            sb.append(lorem[i % lorem.length]);
        }
        sb.append(".");
        return sb.toString();
    }
}