/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.text;

import javafx.scene.text.Font;

import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.text.PrismTextLayout;

import org.junit.Ignore;
import org.junit.Test;

import sun.font.CharToGlyphMapper;
import static org.junit.Assert.*;

public class TextLayoutTest {
    private String J = "\u3041";  //Japanese not complex
    private String D = "\u0907"; //Devanagari complex
    private String T = "\u0E34"; //Devanagari complex
    
    class TestSpan implements TextSpan {
        String text;
        Object font;
        TestSpan(Object text, Object font) {
            this.text = (String)text;
            this.font = font;
        }
        @Override public String getText() {
            return text;
        }
        @Override public Object getFont() {
            return font;
        }
        @Override public RectBounds getBounds() {
            return null;
        }
    }

    public TextLayoutTest() {
    }

    private void setContent(PrismTextLayout layout, Object... content) {
        int count = content.length / 2;
        TextSpan[] spans = new TextSpan[count];
        int i = 0;
        while (i < content.length) {
            spans[i>>1] = new TestSpan(content[i++], content[i++]);
        }
        layout.setContent(spans);
    }

    private void verifyLayout(PrismTextLayout layout, int lineCount, int runCount, int... glyphCount) {
        TextLine[] lines = layout.getLines();
        assertEquals("lineCount", lineCount, lines.length);
        GlyphList[] runs = layout.getRuns();
        assertEquals("runCount", runCount, runs.length);
        assertEquals("runCount", runCount, glyphCount.length);
        for (int i = 0; i < runs.length; i++) {
            assertEquals("run " +i, glyphCount[i], runs[i].getGlyphCount());
        }
    }
    
    private void verifyComplex(PrismTextLayout layout, boolean... complex) {
        GlyphList[] runs = layout.getRuns();
        for (int i = 0; i < runs.length; i++) {
            assertEquals("run " +i, complex[i], runs[i].isComplex());
        }
    }
    
    @SuppressWarnings("deprecation")
    @Ignore("RT-31357")
    @Test public void buildRuns() {

        PrismTextLayout layout = new PrismTextLayout();
        PGFont font = (PGFont)Font.font("Monaco", 12).impl_getNativeFont();
        PGFont font2 = (PGFont)Font.font("Tahoma", 12).impl_getNativeFont();
        
        /* simple case */
        layout.setContent("hello", font);
        verifyLayout(layout, 1, 1, 5);
        
        /* simple case, two workd*/
        layout.setContent("hello world", font);
        verifyLayout(layout, 1, 1, 11);
        
        /* empty string */
        layout.setContent("", font);
        verifyLayout(layout, 1, 1, 0);
        
        /* line break */
        layout.setContent("\n", font); //first line has the line break (glyphCount=0),
        verifyLayout(layout, 2, 2, 0,0);
        layout.setContent("\r", font);
        verifyLayout(layout, 2, 2, 0,0);
        layout.setContent("\r\n", font);
        verifyLayout(layout, 2, 2, 0,0);
        layout.setContent("a\nb", font);
        verifyLayout(layout, 2, 3, 1, 0, 1);
        layout.setContent("\n\n\r\r\n", font);
        verifyLayout(layout, 5, 5, 0,0,0,0,0);
        
        /* tabs */
        layout.setContent("\t", font);
        verifyLayout(layout, 1, 1, 0);
        layout.setContent("\t\t", font);
        verifyLayout(layout, 1, 2, 0,0);
        layout.setContent("a\tb", font);
        verifyLayout(layout, 1, 3, 1,0,1);

        /* complex */
        layout.setContent("aa"+J+J, font);
        verifyLayout(layout, 1, 1, 4);// no complex (english to japanese)
        verifyComplex(layout, false);
        
        
        layout.setContent(D, font);
        verifyLayout(layout, 1, 1, 1);// complex (english to devanagari)
        verifyComplex(layout, true);

        layout.setContent("aa"+D+D, font);
        verifyLayout(layout, 1, 2, 2,2);// complex (english to devanagari)
        verifyComplex(layout, false, true);

        layout.setContent(D+D+"aa", font);
        verifyLayout(layout, 1, 2, 2,2);// complex (devanagari to english)
        verifyComplex(layout, true, false);
        
        layout.setContent("aa"+D+D+J+J, font);
        verifyLayout(layout, 1, 3, 2,2,2);// complex (english to devanagari to japanese)
        verifyComplex(layout, false, true, false);

        /*Tahoma has Thai but no Hindi, font slot break expected*/
        layout.setContent(D+D+T+T, font2); 
        verifyLayout(layout, 1, 2, 2,2);// complex (devanagari to thai)
        verifyComplex(layout, true, true);
        
        layout.setContent(T+T+D+D+T+T, font2); 
        verifyLayout(layout, 1, 3, 2,2,2);
        verifyComplex(layout, true, true, true);

        layout.setContent(T+T+D+D+"aa", font2); 
        verifyLayout(layout, 1, 3, 2,2,2);
        verifyComplex(layout, true, true, false);
        
        layout.setContent(T+T+"aa"+T+T, font2); 
        verifyLayout(layout, 1, 3, 2,2,2);
        verifyComplex(layout, true, false, true);

        layout.setContent("aa"+D+D+T+T, font2); 
        verifyLayout(layout, 1, 3, 2,2,2);
        verifyComplex(layout, false, true, true);

        /* Rich Text test */
        
        setContent(layout, "hello ", font, "world", font);
        verifyLayout(layout, 1, 2, 6,5);
        verifyComplex(layout, false, false);
        
        setContent(layout, "aaa", font, J+J+J, font);
        verifyLayout(layout, 1, 2, 3,3);
        verifyComplex(layout, false, false);
        
        setContent(layout, "aaa", font, D+D+D, font);
        verifyLayout(layout, 1, 2, 3,3);
        verifyComplex(layout, false, true);

        /* can't merge \r\n in different spans*/
        setContent(layout, "aa\r", font, "\nbb", font);
        verifyLayout(layout, 3, 4, 2,0,0,2);
        verifyComplex(layout, false, false, false, false);

        setContent(layout, "aa\r\n", font, "bb", font);
        verifyLayout(layout, 2, 3, 2,0,2);
        verifyComplex(layout, false, false, false);
        
        /* can't merge surrogate pairs in different spans*/
        setContent(layout, "\uD840\uDC0B", font, "\uD840\uDC89\uD840\uDCA2", font);
        verifyLayout(layout, 1, 2, 2, 4); 
        GlyphList[] runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(3) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
         
        /* Split surrogate pair*/
        setContent(layout, "\uD840\uDC0B\uD840", font, "\uDC89\uD840\uDCA2", font);
        verifyLayout(layout, 1, 2, 3, 3); 
        runs = layout.getRuns();
        assertTrue(runs[0].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(1) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[0].getGlyphCode(2) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);//broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(0) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);//broken pair, results in missing glyph
        assertTrue(runs[1].getGlyphCode(1) != CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        assertTrue(runs[1].getGlyphCode(2) == CharToGlyphMapper.INVISIBLE_GLYPH_ID);
        
    }    
    
}
