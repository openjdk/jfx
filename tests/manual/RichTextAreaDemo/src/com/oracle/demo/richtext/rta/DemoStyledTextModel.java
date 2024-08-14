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
package com.oracle.demo.richtext.rta;

import java.text.DecimalFormat;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

/**
 * Demo StyledTextModel.
 * Does not support editing events - populate the model first, then pass it to the control.
 */
public class DemoStyledTextModel extends StyledTextModelViewOnlyBase {
    private final int size;
    private final boolean monospaced;
    private static final DecimalFormat format = new DecimalFormat("#,##0");

    public DemoStyledTextModel(int size, boolean monospaced) {
        this.size = size;
        this.monospaced = monospaced;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
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
        RichParagraph.Builder b = RichParagraph.builder();
        String s = format.format(ix + 1);
        String sz = format.format(size);
        String[] css = monospaced ? new String[] { "monospaced" } : new String[0];

        b.withInlineAndExternalStyles(s, "-fx-fill:darkgreen;", css);
        b.withStyles(" / ", css);
        b.withInlineAndExternalStyles(sz, "-fx-fill:black;", css);
        if (monospaced) {
            b.withStyles(" (monospaced)", css);
        }

        if ((ix % 10) == 9) {
            String words = generateWords(ix);
            b.withStyles(words, css);
        }
        return b.build();
    }

    private String generateWords(int ix) {
        String s = String.valueOf(ix);
        StringBuilder sb = new StringBuilder(128);
        for (char c: s.toCharArray()) {
            String digit = getDigit(c);
            sb.append(digit);
        }
        return sb.toString();
    }

    private String getDigit(char c) {
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
}
