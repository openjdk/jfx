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

package com.oracle.demo.richtext.rta;

import java.io.IOException;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.BasicTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * All the models used in the tester.
 *
 * @author Andy Goryachev
 */
public enum ModelChoice {
    DEMO("Demo"),
    PARAGRAPH("Paragraph Attributes"),
    WRITING_SYSTEMS_EDITABLE("Writing Systems (Editable)"),
    EDITABLE_STYLED("❤ Editable Rich Text Model"),
    BILLION_LINES("2,000,000,000 Lines"),
    NOTEBOOK("Notebook: Embedded Chart"),
    NOTEBOOK2("Notebook: SQL Queries"),
    EDITABLE_PLAIN("Plaintext with Syntax Highlighting"),
    NULL("null"),
    EXAMPLES("Examples"),
    INLINE("Inline Nodes"),
    MONOSPACED("Monospaced"),
    TABS("Tabs"),
    UNEVEN_SMALL("Uneven Small"),
    UNEVEN_LARGE("Uneven Large"),
    WRITING_SYSTEMS("Writing Systems"),
    ZERO_LINES("0 Lines"),
    ONE_LINE("1 Line"),
    TEN_LINES("10 Lines"),
    THOUSAND_LINES("1,000 Lines"),
    LARGE_TEXT("Large text"),
    LARGE_TEXT_LONG("Large Text, Long"),
    NO_LAST_NEWLINE_SHORT("No Last Newline, Short"),
    NO_LAST_NEWLINE_MEDIUM("No Last Newline, Medium"),
    NO_LAST_NEWLINE_LONG("No Last Newline, Long"),
    ;

    private final String name;

    ModelChoice(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static StyledTextModel create(ModelChoice ch) {
        if(ch == null) {
            return null;
        }

        switch(ch) {
        case BILLION_LINES:
            return new DemoStyledTextModel(2_000_000_000, false);
        case DEMO:
            return new DemoModel();
        case EXAMPLES:
            return new ExamplesModel();
        case INLINE:
            return new InlineNodesModel();
        case EDITABLE_PLAIN:
            {
                BasicTextModel m = new BasicTextModel() {
                    private static final String DIGITS = "-fx-fill:magenta;";

                    @Override
                    public RichParagraph getParagraph(int index) {
                        String text = getPlainText(index);
                        RichParagraph.Builder b = RichParagraph.builder();
                        int start = 0;
                        int sz = text.length();
                        boolean num = false;
                        for (int i = 0; i < sz; i++) {
                            char c = text.charAt(i);
                            if (num != Character.isDigit(c)) {
                                if (i > start) {
                                    String s = text.substring(start, i);
                                    String style = num ? DIGITS : null;
                                    b.addWithInlineStyle(s, style);
                                    start = i;
                                }
                                num = !num;
                            }
                        }
                        if (start < sz) {
                            String s = text.substring(start);
                            String style = num ? DIGITS : null;
                            b.addWithInlineStyle(s, style);
                        }
                        return b.build();
                    }
                };
                return m;
            }
        case EDITABLE_STYLED:
            return new RichTextModel();
        case LARGE_TEXT:
            return new LargeTextModel(10);
        case LARGE_TEXT_LONG:
            return new LargeTextModel(5_000);
        case NO_LAST_NEWLINE_SHORT:
            return new NoLastNewlineModel(1);
        case NO_LAST_NEWLINE_MEDIUM:
            return new NoLastNewlineModel(5);
        case NO_LAST_NEWLINE_LONG:
            return new NoLastNewlineModel(300);
        case MONOSPACED:
            return new DemoStyledTextModel(2_000_000_000, true);
        case NOTEBOOK:
            return new NotebookModel();
        case NOTEBOOK2:
            return new NotebookModel2();
        case NULL:
            return null;
        case ONE_LINE:
            return new DemoStyledTextModel(1, false);
        case PARAGRAPH:
            return new ParagraphAttributesDemoModel();
        case TABS:
            return tabs();
        case TEN_LINES:
            return new DemoStyledTextModel(10, false);
        case THOUSAND_LINES:
            return new DemoStyledTextModel(1_000, false);
        case UNEVEN_SMALL:
            return new UnevenStyledTextModel(20);
        case UNEVEN_LARGE:
            return new UnevenStyledTextModel(2000);
        case WRITING_SYSTEMS:
            return writingSystemsPlain();
        case WRITING_SYSTEMS_EDITABLE:
            return writingSystems();
        case ZERO_LINES:
            return new DemoStyledTextModel(0, false);
        default:
            throw new Error("?" + ch);
        }
    }

    private static StyledTextModel writingSystemsPlain() {
        try {
            return SimpleViewOnlyStyledModel.of(WritingSystemsDemo.getText());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static StyledTextModel tabs() {
        try {
            return SimpleViewOnlyStyledModel.of("0123456789012345678901234567890\n0\n\t1\n\t\t2\n\t\t\t3\n\t\t\t\t4\n0\n");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static StyledTextModel writingSystems() {
        StyleAttributeMap name = StyleAttributeMap.builder().
            setFontSize(24).
            setTextColor(Color.gray(0.5)).
            build();

        StyleAttributeMap value = StyleAttributeMap.builder().
            setFontSize(24).
            build();

        RichTextModel m = new RichTextModel();
        String[] ss = WritingSystemsDemo.PAIRS;
        for (int i = 0; i < ss.length;) {
            String s = ss[i++] + ":  ";
            append(m, s, name);

            s = ss[i++];
            append(m, s, value);

            append(m, "\n", null);
        }
        return m;
    }

    // TODO add to StyledModel?
    private static void append(StyledTextModel m, String text, StyleAttributeMap style) {
        TextPos p = m.getDocumentEnd();
        m.replace(null, p, p, StyledInput.of(text, style), false);
    }
}
