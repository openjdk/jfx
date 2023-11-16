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

import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.BasePlainTextModel;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledInput;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.scene.paint.Color;

public enum ModelChoice {
    DEMO("Demo"),
    PARAGRAPH("Paragraph Attributes"),
    WRITING_SYSTEMS_EDITABLE("Writing Systems (Editable)"),
    EDITABLE_STYLED("❤ Editable Rich Text Model"),
    BILLION_LINES("1,000,000,000 Lines"),
    NOTEBOOK_STACK("Notebook: Embedded Rich Text Areas"),
    NOTEBOOK("Notebook: Embedded Chart"),
    NOTEBOOK2("Notebook: SQL Queries"),
    EDITABLE_PLAIN("Plaintext with Syntax Highlighting"),
    NULL("null"),
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
    
    public String toString() {
        return name;
    }

    public static StyledTextModel create(ModelChoice ch) {
        if(ch == null) {
            return null;
        }
        
        switch(ch) {
        case BILLION_LINES:
            return new DemoStyledTextModel(1_000_000_000, false);
        case DEMO:
            return new DemoModel();
        case INLINE:
            return new InlineNodesModel();
        case EDITABLE_PLAIN:
            {
                BasePlainTextModel m = new BasePlainTextModel() {
                    private static final String DIGITS = "-fx-fill:magenta;";

                    @Override
                    public RichParagraph getParagraph(int index) {
                        String text = getPlainText(index);
                        RichParagraph p = new RichParagraph();
                        int start = 0;
                        int sz = text.length();
                        boolean num = false;
                        for (int i = 0; i < sz; i++) {
                            char c = text.charAt(i);
                            if (num != Character.isDigit(c)) {
                                if (i > start) {
                                    String s = text.substring(start, i);
                                    String style = num ? DIGITS : null;
                                    p.addSegment(s, style, null);
                                    start = i;
                                }
                                num = !num;
                            }
                        }
                        if (start < sz) {
                            String s = text.substring(start);
                            String style = num ? DIGITS : null;
                            p.addSegment(s, style, null);
                        }
                        return p;
                    }
                };
                return m;
            }
        case EDITABLE_STYLED:
            return new EditableRichTextModel();
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
        case NOTEBOOK_STACK:
            return new NotebookModelStacked();
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
            return SimpleReadOnlyStyledModel.from(WritingSystemsDemo.getText());
        case WRITING_SYSTEMS_EDITABLE:
            return writingSystems();
        case ZERO_LINES:
            return new DemoStyledTextModel(0, false);
        default:
            throw new Error("?" + ch);
        }
    }

    private static StyledTextModel tabs() {
        return SimpleReadOnlyStyledModel.from("0123456789012345678901234567890\n0\n\t1\n\t\t2\n\t\t\t3\n\t\t\t\t4\n0\n");
    }

    private static StyledTextModel writingSystems() {
        StyleAttrs name = StyleAttrs.builder().
            setFontSize(24).
            setTextColor(Color.gray(0.5)).
            build();
        
        StyleAttrs value = StyleAttrs.builder().
            setFontSize(24).
            build();

        EditableRichTextModel m = new EditableRichTextModel();
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

    // TODO add to StyledModel
    private static void append(StyledTextModel m, String text, StyleAttrs style) {
        TextPos p = m.getDocumentEnd();
        m.replace(null, p, p, StyledInput.of(text, style), false);
    }
}
