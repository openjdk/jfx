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

import javafx.incubator.scene.control.rich.model.RtfFormatHandler;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * This simple, read-only StyledModel demonstrates various paragraph attributes.
 */
public class ParagraphAttributesDemoModel extends SimpleReadOnlyStyledModel {
    private final static StyleAttrs TITLE = StyleAttrs.builder().
        setFontSize(24).
        setUnderline(true).
        build();
    private final static StyleAttrs BULLET = StyleAttrs.builder().
        setSpaceLeft(20).
        setBullet("•").
        build();
    private final static StyleAttrs FIRST_LINE_INDENT = StyleAttrs.builder().
        setFirstLineIndent(100).
        build();

    public ParagraphAttributesDemoModel() {
        registerDataFormatHandler(new RtfFormatHandler(), true, 1000);
        insert(this);
    }

    public static void insert(SimpleReadOnlyStyledModel m) {
        m.addSegment("Bullet List", TITLE);
        m.nl(2);
        m.setParagraphAttributes(BULLET);
        m.addSegment("This little piggy went to market,");
        m.setParagraphAttributes(BULLET);
        m.nl();
        m.addSegment("This little piggy stayed home,");
        m.setParagraphAttributes(BULLET);
        m.nl();
        m.addSegment("This little piggy had roast beef,");
        m.setParagraphAttributes(BULLET);
        m.nl();
        m.addSegment("This little piggy had none.");
        m.setParagraphAttributes(BULLET);
        m.nl();
        m.addSegment("This little piggy went ...");
        m.setParagraphAttributes(BULLET);
        m.nl();
        m.addSegment("Wee, wee, wee, all the way home!");
        m.setParagraphAttributes(BULLET);
        m.nl(2);

        m.addSegment("First Line Indent", TITLE);
        m.nl(2);
        m.addSegment(words(60));
        m.setParagraphAttributes(FIRST_LINE_INDENT);
        m.nl(2);

        m.addSegment("Paragraph Attributes", TITLE);
        m.nl(2);

        m.addSegment("✓ Opaque Background Color");
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.LIGHTGREEN).
            build());
        m.nl();

        m.addSegment("✓ Translucent Background Color");
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(FX.alpha(Color.LIGHTGREEN, 0.5)).
            build());
        m.nl();

        // space

        m.addSegment("✓ Space Above");
        m.setParagraphAttributes(StyleAttrs.builder().
            setSpaceAbove(20).
            setBackground(Color.gray(0.95, 0.5)).
            setBullet("•").
            build());
        m.nl();

        m.addSegment("✓ Space Below");
        m.setParagraphAttributes(StyleAttrs.builder().
            setSpaceBelow(20).
            setBackground(Color.gray(0.9, 0.5)).
            setBullet("◦").
            build());
        m.nl();

        m.addSegment("✓ Space Left " + words(50));
        m.setParagraphAttributes(StyleAttrs.builder().
            setSpaceLeft(20).
            setBackground(Color.gray(0.85, 0.5)).
            setBullet("∙").
            build());
        m.nl();

        m.addSegment("✓ Space Right " + words(10));
        m.setParagraphAttributes(StyleAttrs.builder().
            setSpaceRight(20).
            setBackground(Color.gray(0.8, 0.5)).
            setBullet("‣").
            build());
        m.nl();

        // text alignment

        m.addSegment("✓ Text Alignment Left " + words(20));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.95, 0.5)).
            setTextAlignment(TextAlignment.LEFT).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Right " + words(20));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.9, 0.5)).
            setTextAlignment(TextAlignment.RIGHT).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Center " + words(20));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.85, 0.5)).
            setTextAlignment(TextAlignment.CENTER).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Justify " + words(20));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.8, 0.5)).
            setTextAlignment(TextAlignment.JUSTIFY).
            build());
        m.nl();

        // line spacing

        m.addSegment("✓ Line Spacing 0 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.95, 0.5)).
            setLineSpacing(0).
            build());
        m.nl();

        m.addSegment("✓ Line Spacing 20 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.9, 0.5)).
            setLineSpacing(20).
            build());
        m.nl();

        m.addSegment("✓ Line Spacing 40 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttrs.builder().
            setBackground(Color.gray(0.9, 0.5)).
            setLineSpacing(40).
            build());
        m.nl();
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
