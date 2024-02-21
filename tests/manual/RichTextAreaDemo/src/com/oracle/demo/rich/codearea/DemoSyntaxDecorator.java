/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.demo.rich.codearea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.incubator.scene.control.rich.CodeTextModel;
import javafx.incubator.scene.control.rich.SyntaxDecorator;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.scene.paint.Color;

/**
 * Simple {@code SyntaxDecorator} which emphasizes digits and java keywords.
 * This is just a demo.
 */
public class DemoSyntaxDecorator implements SyntaxDecorator {
    private static final StyleAttrs DIGITS = StyleAttrs.builder().setTextColor(Color.MAGENTA).build();
    private static final StyleAttrs KEYWORDS = StyleAttrs.builder().setTextColor(Color.GREEN).build();
    private static Pattern PATTERN = initPattern();

    public DemoSyntaxDecorator() {
    }

    @Override
    public String toString() {
        return "DemoSyntaxDecorator";
    }

    @Override
    public void attach(CodeTextModel m) {
    }

    @Override
    public void detach(CodeTextModel m) {
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        String text = model.getPlainText(index);
        RichParagraph.Builder b = RichParagraph.builder();
        if (text != null) {
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
}
