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
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Tests SimpleViewOnlyStyledModel.
 */
public class TestSimpleViewOnlyStyledModel {

    private static final StyleAttributeMap BOLD = StyleAttributeMap.builder().setBold(true).build();
    private static final StyleAttributeMap ITALIC = StyleAttributeMap.builder().setItalic(true).build();
    private static final StyleAttributeMap UNDER = StyleAttributeMap.builder().setUnderline(true).build();
    private SimpleViewOnlyStyledModel model;

    @BeforeEach
    public void beforeEach() {
        model = new SimpleViewOnlyStyledModel();
    }

    @Test
    public void addTextAfterRegion() {
        model.addParagraph(() -> new Region());
        model.addNodeSegment(() -> new Region());
        assertEquals(2, model.size());
    }

    @Test
    public void addTextAfterRegionAfterText() {
        model.addNodeSegment(() -> new Region());
        model.addParagraph(() -> new Region());
        model.addSegment("text");
        assertEquals(3, model.size());
    }

    private void aa(int index, int charIndex, boolean leading, boolean forInsert, StyleAttributeMap expected) {
        int off = charIndex + (leading ? 0 : 1);
        TextPos p = new TextPos(index, off, charIndex, leading);
        StyleAttributeMap a = model.getStyleAttributeMap(null, p, forInsert);
        assertEquals(expected, a);
    }

    @Test
    public void getStyleAttributeMap() {
        model.addSegment("BB", BOLD);
        model.addSegment("II", ITALIC);
        model.nl();
        model.addSegment("X", UNDER);

        // exact
        aa(0, 0, true, false, BOLD);
        aa(0, 0, false, false, BOLD);
        aa(0, 1, true, false, BOLD);
        aa(0, 1, false, false, BOLD);
        aa(0, 2, true, false, ITALIC);
        aa(0, 2, false, false, ITALIC);
        aa(0, 3, true, false, ITALIC);
        aa(0, 3, false, false, ITALIC);
        aa(0, 4, true, false, ITALIC);
        aa(0, 4, false, false, ITALIC);
        aa(0, 999, true, false, ITALIC);
        aa(0, 999, false, false, ITALIC);

        // for insert
        aa(0, 0, true, true, BOLD);
        aa(0, 0, false, true, BOLD);
        aa(0, 1, true, true, BOLD);
        aa(0, 1, false, true, BOLD);
        aa(0, 2, true, true, BOLD);
        aa(0, 2, false, true, ITALIC); // sic!
        aa(0, 3, true, true, ITALIC);
        aa(0, 3, false, true, ITALIC);
        aa(0, 4, true, true, ITALIC);
        aa(0, 4, false, true, ITALIC);
        aa(0, 999, true, true, ITALIC);
        aa(0, 999, false, true, ITALIC);

        // line 2

        // exact
        aa(1, 0, true, false, UNDER);
        aa(1, 0, false, false, UNDER);
        aa(1, 1, true, false, UNDER);
        aa(1, 1, false, false, UNDER);
        aa(1, 999, true, false, UNDER);
        aa(1, 999, false, false, UNDER);

        // for insert

        aa(1, 0, true, true, UNDER);
        aa(1, 0, false, true, UNDER);
        aa(1, 1, true, true, UNDER);
        aa(1, 1, false, true, UNDER);
        aa(1, 999, true, true, UNDER);
        aa(1, 999, false, true, UNDER);

        // beyond eof
        aa(999, 999, false, false, StyleAttributeMap.EMPTY);
        aa(999, 999, false, true, StyleAttributeMap.EMPTY);

        // TODO grapheme clusters
    }
}
