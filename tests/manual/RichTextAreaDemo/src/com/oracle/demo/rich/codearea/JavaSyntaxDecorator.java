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

package com.oracle.demo.rich.codearea;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.rich.SyntaxDecorator;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.CodeTextModel;
import jfx.incubator.scene.control.rich.model.RichParagraph;
import jfx.incubator.scene.control.rich.model.StyleAttributeMap;

/**
 * A simple {@code SyntaxDecorator} for Java source files.
 *
 * This is just a demo, as it has no link to the real compiler, does not understand Java language
 * and does not take into account version-specific language features.
 */
public class JavaSyntaxDecorator implements SyntaxDecorator {
    private static final StyleAttributeMap CHARACTER = mkStyle(Color.BLUE);
    private static final StyleAttributeMap COMMENT = mkStyle(Color.RED);
    private static final StyleAttributeMap KEYWORD = mkStyle(Color.GREEN);
    private static final StyleAttributeMap NUMBER = mkStyle(Color.MAGENTA);
    private static final StyleAttributeMap OTHER = mkStyle(Color.BLACK);
    private static final StyleAttributeMap STRING = mkStyle(Color.BLUE);
    private ArrayList<RichParagraph> paragraphs;

    public JavaSyntaxDecorator() {
    }

    @Override
    public String toString() {
        return "JavaSyntaxDecorator";
    }


    @Override
    public void handleChange(CodeTextModel m, TextPos start, TextPos end, int top, int lines, int btm) {
        // in theory, it may reuse the portions that haven't changed
        // but java files are short enough to re-analyze in full each time
        reload(m);
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        if ((paragraphs == null) || (index >= paragraphs.size())) {
            return RichParagraph.builder().build();
        }
        return paragraphs.get(index);
    }

    private static StyleAttributeMap mkStyle(Color c) {
        return StyleAttributeMap.builder().setTextColor(c).build();
    }

    private void reload(CodeTextModel model) {
        String text = getPlainText(model);
        JavaSyntaxAnalyzer a = new JavaSyntaxAnalyzer(text);
        List<JavaSyntaxAnalyzer.Line> res = a.analyze();
        paragraphs = translate(res);
    }

    private String getPlainText(CodeTextModel model) {
        StringBuilder sb = new StringBuilder(65536);
        int sz = model.size();
        boolean nl = false;
        for (int i = 0; i < sz; i++) {
            if (nl) {
                sb.append('\n');
            } else {
                nl = true;
            }
            String s = model.getPlainText(i);
            if (s != null) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private ArrayList<RichParagraph> translate(List<JavaSyntaxAnalyzer.Line> lines) {
        ArrayList<RichParagraph> res = new ArrayList<>(lines.size());
        for (JavaSyntaxAnalyzer.Line line : lines) {
            RichParagraph p = createParagraph(line);
            res.add(p);
        }
        return res;
    }

    private RichParagraph createParagraph(JavaSyntaxAnalyzer.Line line) {
        RichParagraph.Builder b = RichParagraph.builder();
        for (JavaSyntaxAnalyzer.Segment seg : line.getSegments()) {
            JavaSyntaxAnalyzer.Type t = seg.getType();
            String text = seg.getText();
            StyleAttributeMap a = getStyleAttrs(t);
            b.addSegment(text, a);
        }
        return b.build();
    }

    private StyleAttributeMap getStyleAttrs(JavaSyntaxAnalyzer.Type t) {
        switch(t) {
        case CHARACTER:
            return CHARACTER;
        case COMMENT:
            return COMMENT;
        case KEYWORD:
            return KEYWORD;
        case NUMBER:
            return NUMBER;
        case STRING:
            return STRING;
        }
        return OTHER;
    }
}
