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

import javafx.scene.control.rich.TextPos;
import javafx.scene.control.rich.model.EditablePlainTextModel;
import javafx.scene.control.rich.model.EditableRichTextModel;
import javafx.scene.control.rich.model.SimpleReadOnlyStyledModel;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyleInfo;
import javafx.scene.control.rich.model.StyledInput;
import javafx.scene.control.rich.model.StyledTextModel;
import javafx.scene.paint.Color;

public enum Models {
    DEMO("Demo"),
    BILLION_LINES("1,000,000,000 Lines"),
    EDITABLE_PLAIN("Editable Plaintext Model"),
    EDITABLE_STYLED("❤ Editable Rich Text Model"),
    NOTEBOOK("Notebook: Embedded Chart"),
    NOTEBOOK2("Notebook: SQL Queries"),
    WRITING_SYSTEMS_EDITABLE("Writing Systems (Editable)"),
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
    
    Models(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }

    public static StyledTextModel create(Models m) {
        if(m == null) {
            return null;
        }
        
        switch(m) {
        case BILLION_LINES:
            return new DemoStyledTextModel(1_000_000_000, false);
        case DEMO:
            return new DemoModel();
        case INLINE:
            return new InlineNodesModel();
        case EDITABLE_PLAIN:
            return new EditablePlainTextModel();
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
        case NULL:
            return null;
        case ONE_LINE:
            return new DemoStyledTextModel(1, false);
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
            throw new Error("?" + m);
        }
    }

    private static StyledTextModel tabs() {
        return SimpleReadOnlyStyledModel.from("0123456789012345678901234567890\n0\n\t1\n\t\t2\n\t\t\t3\n\t\t\t\t4\n0\n");
    }
    
    private static StyledTextModel writingSystems() {
        StyleAttrs a = new StyleAttrs();
        a.set(StyleAttrs.FONT_SIZE, 200);
        a.set(StyleAttrs.TEXT_COLOR, Color.gray(0.5));
        StyleInfo name = StyleInfo.of(a);
        
        a = new StyleAttrs();
        a.set(StyleAttrs.FONT_SIZE, 200);
        StyleInfo value = StyleInfo.of(a);
        
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
    private static void append(StyledTextModel m, String text, StyleInfo style) {
        TextPos p = m.getEndTextPos();
        m.replace(null, p, p, StyledInput.of(text, style));
    }
}
