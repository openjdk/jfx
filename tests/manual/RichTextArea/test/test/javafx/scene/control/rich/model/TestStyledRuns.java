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
package test.javafx.scene.control.rich.model;

import java.io.IOException;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.javafx.scene.control.rich.StringBuilderStyledOutput;

public class TestStyledRuns {
    private final StyleAttrs PLAIN = mk();
    private final StyleAttrs BOLD = mk(StyleAttrs.BOLD);
    private final Object[] SEG1 = { "abcdefgh", PLAIN };
    private final Object[] SEG2 = { "abcdefgh", PLAIN, "ijklmnop", BOLD };
    private final Object[] SEG3 = { "abcdefgh", PLAIN, "ijklmnop", BOLD, "qrstuvwx", PLAIN };
    
    @Test
    public void testReplace() throws IOException {
        replace(
            new Object[] {
                "1122", PLAIN
            },
            new TextPos(0, 2),
            new TextPos(0, 2),
            new Object[] {
                "aa", BOLD
            },
            new Object[] {
                "11", PLAIN,
                "aa", BOLD,
                "22", PLAIN
            }
        );
    }

    @Test
    public void testApplyStyle() throws IOException {
        t(
            SEG1,
            0, 2, 0, 4, BOLD,
            new Object[] {
                "ab", PLAIN,
                "cd", BOLD,
                "efgh", PLAIN
            }
        );
    }
    
    private static StyleAttrs mk(Object... spec) {
        StyleAttrs.Builder b = StyleAttrs.builder();
        for (int i = 0; i < spec.length;) {
            StyleAttribute a = (StyleAttribute)spec[i++];
            Object v;
            if (i < spec.length) {
                v = spec[i];
                if (v instanceof StyleAttribute) {
                    v = null;
                }
            } else {
                v = null;
            }

            if (v == null) {
                v = Boolean.TRUE;
            }
            b.set(a, v);
        }
        return b.build();
    }

    private void t(Object[] initial, int ix1, int off1, int ix2, int off2, StyleAttrs a, Object[] expected) throws IOException {
        EditableRichTextModel m = createModel(initial);
        
        {
            TextPos fin = m.getDocumentEnd();
            StringBuilderStyledOutput out = new StringBuilderStyledOutput();
            m.exportText(TextPos.ZERO, fin, out);
            Object chk = out.getOutput();
            System.out.println(chk);
        }
        
        TextPos start = new TextPos(ix1, off1);
        TextPos end = new TextPos(ix2, off2);
        m.applyStyle(start, end, a);
        
        {
            TStyledOutput out = new TStyledOutput(null);
            TextPos last = m.getDocumentEnd();
            m.exportText(TextPos.ZERO, last, out);
            Object[] result = out.getResult();
            Assertions.assertArrayEquals(expected, result);
        }
    }
    
    private EditableRichTextModel createModel(Object[] initial) {
        EditableRichTextModel m = new EditableRichTextModel();
        TextPos end = m.getDocumentEnd();
        TStyledInput in = new TStyledInput(initial);
        m.replace(null, TextPos.ZERO, end, in, false);
        return m;
    }

    // start - not null
    // end - null for end of document
    private void replace(Object[] initial, TextPos start, TextPos end, Object[] input, Object[] expected) throws IOException {
        EditableRichTextModel m = createModel(initial);
        TStyledInput in = new TStyledInput(input);

        if(end == null) {
            end = m.getDocumentEnd();
        }
        m.replace(null, start, end, in, false);

        TStyledOutput out = new TStyledOutput(null);
        TextPos last = m.getDocumentEnd();
        m.exportText(TextPos.ZERO, last, out);
        Object[] result = out.getResult();
        Assertions.assertArrayEquals(expected, result);
    }
}
