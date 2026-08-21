/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.rta;

import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.model.RtfFormatHandler;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * This simple, read-only StyledModel demonstrates various paragraph attributes.
 *
 * @author Andy Goryachev
 */
public class ParagraphAttributesDemoModel extends SimpleViewOnlyStyledModel {
    private final static StyleAttributeMap TITLE = StyleAttributeMap.builder().
        setFontSize(24).
        setUnderline(true).
        build();
    private final static StyleAttributeMap BULLET = StyleAttributeMap.builder().
        setSpaceLeft(20).
        setBullet("•").
        build();
    private final static StyleAttributeMap FIRST_LINE_INDENT = StyleAttributeMap.builder().
        setFirstLineIndent(100).
        build();
    private final static StyleAttributeMap HL = StyleAttributeMap.builder().
        setUnderline(true).
        setStrikeThrough(true).
        build();

    public ParagraphAttributesDemoModel() {
        registerDataFormatHandler(RtfFormatHandler.getInstance(), true, false, 1000);
        insert(this);
    }

    public static void insert(SimpleViewOnlyStyledModel m) {
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
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.LIGHTGREEN).
            build());
        m.nl();

        m.addSegment("✓ Translucent Background Color");
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(FX.alpha(Color.LIGHTGREEN, 0.5)).
            build());
        m.nl();

        // space

        m.addSegment("✓ Space ");
        m.addSegment("Above", HL);
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setSpaceAbove(20).
            setBackground(Color.gray(0.95, 0.5)).
            setBullet("•").
            build());
        m.nl();

        m.addSegment("✓ Space ");
        m.addSegment("Below", HL);
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setSpaceBelow(20).
            setBackground(Color.gray(0.9, 0.5)).
            setBullet("◦").
            build());
        m.nl();

        m.addSegment("✓ Space ");
        m.addSegment("Left", HL);
        m.addSegment(" " + words(50));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setSpaceLeft(20).
            setBackground(Color.gray(0.85, 0.5)).
            setBullet("∙").
            build());
        m.nl();

        m.addSegment("✓ Space ");
        m.addSegment("Right", HL);
        m.addSegment(" " + words(10));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setSpaceRight(20).
            setBackground(Color.gray(0.8, 0.5)).
            setBullet("‣").
            build());
        m.nl();

        // text alignment

        m.addSegment("✓ Text Alignment Left " + words(20));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.95, 0.5)).
            setTextAlignment(TextAlignment.LEFT).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Right " + words(20));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.9, 0.5)).
            setTextAlignment(TextAlignment.RIGHT).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Center " + words(20));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.85, 0.5)).
            setTextAlignment(TextAlignment.CENTER).
            build());
        m.nl();

        m.addSegment("✓ Text Alignment Justify " + words(20));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.8, 0.5)).
            setTextAlignment(TextAlignment.JUSTIFY).
            build());
        m.nl();

        // line spacing

        m.addSegment("✓ Line Spacing 0 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.95, 0.5)).
            setLineSpacing(0).
            build());
        m.nl();

        m.addSegment("✓ Line Spacing 20 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttributeMap.builder().
            setBackground(Color.gray(0.9, 0.5)).
            setLineSpacing(20).
            build());
        m.nl();

        m.addSegment("✓ Line Spacing 40 " + words(200));
        m.highlight(50, 100, FX.alpha(Color.RED, 0.4));
        m.setParagraphAttributes(StyleAttributeMap.builder().
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
