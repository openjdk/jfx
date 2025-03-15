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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Tests RTF Import in RichTextModel.
 */
public class RTFImportTest {
    private RichTextModel model;

    @BeforeEach
    public void beforeEach() {
        model = new RichTextModel();
    }

    @Test
    public void characterAttributes() throws Exception {
        initModel(
            """
            {/rtf1/ansi/ansicpg1252/cocoartf2821
            /cocoatextscaling0/cocoaplatform0{/fonttbl/f0/fswiss/fcharset0 Helvetica-Bold;/f1/fswiss/fcharset0 Helvetica;/f2/fswiss/fcharset0 ArialMT;
            /f3/fswiss/fcharset0 Helvetica-Oblique;}
            {/colortbl;/red255/green255/blue255;/red0/green0/blue0;/red251/green0/blue7;}
            {/*/expandedcolortbl;;/cssrgb/c0/c0/c0;/cssrgb/c100000/c0/c0;}
            /margl1440/margr1440/vieww11520/viewh9000/viewkind0
            /deftab720
            /pard/pardeftab720/partightenfactor0

            /f0/b/fs24 /cf2 /expnd0/expndtw0/kerning0
            bold
            /f1/b0 /

            /f2/fs36 font
            /f1/fs24 /
            /pard/pardeftab720/partightenfactor0

            /f3/i /cf2 italic
            /f1/i0 /
            /pard/pardeftab720/partightenfactor0
            /cf2 /strike /strikec2 strikethrough/strike0/striked0 /
            /pard/pardeftab720/partightenfactor0
            /cf3 text color/cf2 /
            /pard/pardeftab720/sl398/sa213/partightenfactor0
            /cf2 /ul /ulc2 underline/ulnone /
            }
            """);

        assertEquals(7, model.size());
        // bold
        int ix = 0;
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // font
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.FONT_FAMILY, "ArialMT");
        checkCharAttr(ix, StyleAttributeMap.FONT_SIZE, 18.0);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // italic
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // strikethrough
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // text color
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.TEXT_COLOR, Color.rgb(251, 0, 7));
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // underline
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.TRUE);
    }

    @Test
    public void paragraphAttributes() throws Exception {
        // TODO
        // background color
        // bullet point
        // first line indent
        // line spacing
        // paragraph direction
        // space (above | below | left | right)
        // text alignment
    }

    private <T> void checkCharAttr(int paragraphIndex, StyleAttribute<T> attribute, T value) {
        TextPos end = model.getEndOfParagraphTextPos(paragraphIndex);
        TextPos p = TextPos.ofLeading(paragraphIndex, end.charIndex() / 2);
        StyleAttributeMap attrs = model.getStyleAttributeMap(null, p);

        assertEquals(value, attrs.get(attribute));
    }

    private void initModel(String mangledRTF) throws Exception {
        // demangle to RTF (replace / with \ characters)
        String rtf = mangledRTF.replace('/', '\\');
        ByteArrayInputStream in = new ByteArrayInputStream(rtf.getBytes(StandardCharsets.US_ASCII));
        model.read(null, DataFormat.RTF, in);
    }
}
