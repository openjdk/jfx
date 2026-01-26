/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * Tests RichParagraph.
 */
public class RichParagraphTest {
    private static final StyleAttributeMap BOLD = StyleAttributeMap.builder().setBold(true).build();
    private static final StyleAttributeMap ITALIC = StyleAttributeMap.builder().setItalic(true).build();
    private static final StyleAttributeMap PAR = StyleAttributeMap.builder().setBackground(Color.GREEN).build();

    @Test
    public void getPlainText() {
        RichParagraph p = p();
        assertEquals("plainbolditalic", p.getPlainText());
    }

    @Test
    public void getSegmentCount() {
        RichParagraph p = p();
        assertEquals(3, p.getSegmentCount());
    }

    @Test
    public void getSegment() {
        RichParagraph p = p();
        StyledSegment s;
        // 0
        s = p.getSegment(0);
        assertEquals("plain", s.getText());
        assertEquals(StyleAttributeMap.EMPTY, s.getStyleAttributeMap(null));
        // 1
        s = p.getSegment(1);
        assertEquals("bold", s.getText());
        assertEquals(BOLD, s.getStyleAttributeMap(null));
        // 2
        s = p.getSegment(2);
        assertEquals("italic", s.getText());
        assertEquals(ITALIC, s.getStyleAttributeMap(null));
        // 3
        assertThrows(IndexOutOfBoundsException.class, () -> {
            p.getSegment(3);
        });
        // -1
        assertThrows(IndexOutOfBoundsException.class, () -> {
            p.getSegment(-1);
        });
    }

    @Test
    public void getParagraphAttributes() {
        RichParagraph p = p();
        assertEquals(PAR, p.getParagraphAttributes());
    }

    private static RichParagraph p() {
        return RichParagraph.builder().
            addSegment("plain", StyleAttributeMap.EMPTY).
            addSegment("bold", BOLD).
            addSegment("italic", ITALIC).
            setParagraphAttributes(PAR).
            build();
    }
}
