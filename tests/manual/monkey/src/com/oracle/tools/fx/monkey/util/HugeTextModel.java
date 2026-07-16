/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.fx.monkey.util;

import java.text.DecimalFormat;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.BasicTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

/**
 * Huge Sample Model.
 *
 * TODO: tabs, empty lines
 */
public class HugeTextModel extends StyledTextModelViewOnlyBase {
    private static final int SIZE = Integer.MAX_VALUE;
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0");
    private static final String SIZE_FORMATTED = FORMAT.format(SIZE);

    public HugeTextModel() {
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos, boolean forInsert) {
        return StyleAttributeMap.EMPTY;
    }

    @Override
    public String getPlainText(int index) {
        RichParagraph p = getParagraph(index);
        return p.getPlainText();
    }

    private static String getText(TextFlow f) {
        StringBuilder sb = new StringBuilder();
        for (Node n : f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                sb.append(t.getText());
            }
        }
        return sb.toString();
    }

    @Override
    public RichParagraph getParagraph(int ix) {
        // CSS class names: see MainWindow.stylesheet()
        boolean monospaced = (((ix / 100) % 10) == 2);
        boolean large = (ix % 100) == 0;
        RichParagraph.Builder b = RichParagraph.builder();
        String s = FORMAT.format(ix + 1);
        String[] css = monospaced ?
            (large ? new String[] { "monospaced large" } : new String[] { "monospaced" }) :
            (large ? new String[] { "large" } : new String[0]);

        b.addWithInlineAndStyleNames(s, "-fx-fill:darkgreen;", css);
        b.addWithStyleNames(" / ", css);
        b.addWithInlineAndStyleNames(SIZE_FORMATTED, "-fx-fill:black;", css);
        if (monospaced) {
            b.addWithStyleNames(" (monospaced)", css);
        }

        if ((ix % 10) == 9) {
            String words = generateWords(ix + 1);
            b.addWithStyleNames(words, css);
        }
        return b.build();
    }

    private static String generateWords(int ix) {
        String s = String.valueOf(ix);
        StringBuilder sb = new StringBuilder(128);
        for (char c: s.toCharArray()) {
            String digit = getDigit(c);
            sb.append(digit);
        }
        return sb.toString();
    }

    private static String getDigit(char c) {
        switch (c) {
        case '0':
            return " zero";
        case '1':
            return " one";
        case '2':
            return " two";
        case '3':
            return " three";
        case '4':
            return " four";
        case '5':
            return " five";
        case '6':
            return " six";
        case '7':
            return " seven";
        case '8':
            return " eight";
        default:
            return " nine";
        }
    }

    /**
     * Creates read-only huge content for CodeTextModel.
     * @param size the size
     * @return the new BasicTextModel.Content instance
     */
    public static BasicTextModel.Content createContent() {
        return new BasicTextModel.Content() {
            @Override
            public int size() {
                return SIZE;
            }

            @Override
            public String getText(int ix) {
                boolean large = (ix % 100) == 0;
                RichParagraph.Builder b = RichParagraph.builder();
                String s = FORMAT.format(ix + 1);
                StringBuilder sb = new StringBuilder();
                sb.append(s);
                sb.append(" / ");
                sb.append(SIZE_FORMATTED);
                if ((ix % 10) == 9) {
                    String words = generateWords(ix + 1);
                    sb.append(words);
                }
                return sb.toString();
            }

            @Override
            public int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void insertLineBreak(int index, int offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeRange(TextPos start, TextPos end) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isWritable() {
                return false;
            }
        };
    }
}
