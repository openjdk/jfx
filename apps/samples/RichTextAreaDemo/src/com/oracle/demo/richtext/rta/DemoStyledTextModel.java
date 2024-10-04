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
 *
 * @author Andy Goryachev
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

        b.addWithInlineAndStyleNames(s, "-fx-fill:darkgreen;", css);
        b.addWithStyleNames(" / ", css);
        b.addWithInlineAndStyleNames(sz, "-fx-fill:black;", css);
        if (monospaced) {
            b.addWithStyleNames(" (monospaced)", css);
        }

        if ((ix % 10) == 9) {
            String words = generateWords(ix);
            b.addWithStyleNames(words, css);
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
