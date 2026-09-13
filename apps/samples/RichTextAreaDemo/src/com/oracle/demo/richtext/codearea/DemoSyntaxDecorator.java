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

package com.oracle.demo.richtext.codearea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Simple {@code SyntaxDecorator} which emphasizes digits and java keywords.
 * This is just a demo.
 *
 * @author Andy Goryachev
 */
public class DemoSyntaxDecorator implements SyntaxDecorator {
    private static final StyleAttributeMap DIGITS = StyleAttributeMap.builder().setTextColor(Color.MAGENTA).build();
    private static final StyleAttributeMap KEYWORDS = StyleAttributeMap.builder().setTextColor(Color.GREEN).build();
    private static Pattern PATTERN = initPattern();

    public DemoSyntaxDecorator() {
    }

    @Override
    public String toString() {
        return "DemoSyntaxDecorator";
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        String text = model.getPlainText(index);
        RichParagraph.Builder b = RichParagraph.builder();
        int len = text.length();
        if (len > 0) {
            Matcher m = PATTERN.matcher(text);
            int beg = 0;
            while (m.find(beg)) {
                int start = m.start();
                if (start > beg) {
                    b.addSegment(text, beg, start, null);
                }
                int end = m.end();
                boolean digit = (m.end(1) >= 0);
                b.addSegment(text, start, end, digit ? DIGITS : KEYWORDS);
                beg = end;
            }
            if (beg < len) {
                b.addSegment(text, beg, len, null);
            }
        }
        return b.build();
    }

    private static Pattern initPattern() {
        String[] keywords = {
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfpv",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while"
        };

        StringBuilder sb = new StringBuilder();
        // digits
        sb.append("(\\b\\d+\\b)");

        // keywords
        for (String k : keywords) {
            sb.append("|\\b(");
            sb.append(k);
            sb.append(")\\b");
        }
        return Pattern.compile(sb.toString());
    }

    @Override
    public void handleChange(CodeTextModel m, TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        // no-op
    }
}
