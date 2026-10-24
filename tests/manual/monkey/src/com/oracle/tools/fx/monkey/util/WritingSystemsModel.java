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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

/**
 * Writing Systems Model for RichTextArea.
 */
public class WritingSystemsModel extends StyledTextModelViewOnlyBase {

    private static final StyleAttributeMap LTR = StyleAttributeMap.of(StyleAttributeMap.TEXT_COLOR, Color.DARKGREEN);
    private final List<RichParagraph> paragraphs = init();

    public WritingSystemsModel() {
    }

    private static List<RichParagraph> init() {
        String[] ss = WritingSystemsDemo.PAIRS;
        ArrayList<RichParagraph> ps = new ArrayList<>(ss.length / 2);
        for (int i = 0; i < ss.length;) {
            String a = ss[i++];
            String b = ss[i++];
            ps.add(createParagraph(a + " " + b));
        }
        return ps;
    }

    private static RichParagraph createParagraph(String text) {
        RichParagraph.Builder b = RichParagraph.builder();
        StyleAttributeMap prev = StyleAttributeMap.EMPTY;
        int start = 0;
        int len = text.length();
        for (int i = 0; i < len;) {
            int codePoint = text.codePointAt(i);
            int ct = Character.charCount(codePoint);
            StyleAttributeMap a = getStyle(codePoint);
            if (!prev.equals(a)) {
                if (i > start) {
                    b.addSegment(text.substring(start, i), prev);
                    prev = a;
                    start = i;
                }
            }
            i += ct;
        }
        if (start < len) {
            b.addSegment(text.substring(start, len), prev);
        }
        return b.build();
    }

    private static StyleAttributeMap getStyle(int codePoint) {
        int type = Character.getDirectionality(codePoint);
        switch(type) {
        case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
        case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
        case Character.DIRECTIONALITY_ARABIC_NUMBER:
        case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
        case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
        case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE:
            return LTR;
        case Character.DIRECTIONALITY_UNDEFINED:
        case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
        case Character.DIRECTIONALITY_EUROPEAN_NUMBER:
        case Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR:
        case Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR:
        case Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR:
        case Character.DIRECTIONALITY_NONSPACING_MARK:
        case Character.DIRECTIONALITY_BOUNDARY_NEUTRAL:
        case Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR:
        case Character.DIRECTIONALITY_SEGMENT_SEPARATOR:
        case Character.DIRECTIONALITY_WHITESPACE:
        case Character.DIRECTIONALITY_OTHER_NEUTRALS:
        case Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING:
        case Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE:
        case Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT:
        case Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE:
        case Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE:
        case Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE:
        default:
            return StyleAttributeMap.EMPTY;
        }
    }

    @Override
    public int size() {
        return paragraphs.size();
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

    @Override
    public RichParagraph getParagraph(int ix) {
        return paragraphs.get(ix);
    }
}
