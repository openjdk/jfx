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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * A simple {@code SyntaxDecorator} for Java source files.
 *
 * This is just a demo, as it has no link to the real compiler, does not understand Java language
 * and does not take into account version-specific language features.
 *
 * @author Andy Goryachev
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
            sb.append(s);
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
