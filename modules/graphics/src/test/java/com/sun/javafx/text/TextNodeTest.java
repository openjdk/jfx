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

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextNodeTest {

    public TextNodeTest() {
    }

    @Test public void testBounds() {
        Text text = new Text("a");
        Bounds bounds = text.getLayoutBounds();
        text.setText("");
        Bounds bounds2 = text.getLayoutBounds();
        
        //empty text should still have the same height
        assertEquals(bounds.getHeight(), bounds2.getHeight(), 0.00001);
        
    }
    
    public void assertBoundsEquals(PathElement[] boundsShape, 
                                   double x, double y,
                                   double w, double h) {
        assertNotNull(boundsShape);
        assertEquals(5, boundsShape.length);
        assertNotNull(boundsShape[0]);
        assertNotNull(boundsShape[1]);
        assertNotNull(boundsShape[2]);
        assertNotNull(boundsShape[3]);
        assertNotNull(boundsShape[4]);
        assertEquals(boundsShape[0].getClass(), MoveTo.class);
        assertEquals(boundsShape[1].getClass(), LineTo.class);
        assertEquals(boundsShape[2].getClass(), LineTo.class);
        assertEquals(boundsShape[3].getClass(), LineTo.class);
        assertEquals(boundsShape[4].getClass(), LineTo.class);
        MoveTo m = (MoveTo)boundsShape[0];
        LineTo l0 = (LineTo)boundsShape[1];
        LineTo l1 = (LineTo)boundsShape[2];
        LineTo l2 = (LineTo)boundsShape[3];
        LineTo l3 = (LineTo)boundsShape[4];
        double e = 0.00001;
        assertEquals(m.getX(), x, e);
        assertEquals(m.getY(), y, e);
        assertEquals(l0.getX(), x+w, e);
        assertEquals(l0.getY(), y, e);
        assertEquals(l1.getX(), x+w, e);
        assertEquals(l1.getY(), y+h, e);
        assertEquals(l2.getX(), x, e);
        assertEquals(l2.getY(), y+h, e);
        assertEquals(l3.getX(), x, e);
        assertEquals(l3.getY(), y, e);
    }
    
    public void assertCaretEquals(PathElement[] caretShape, 
                                  double x0, double y0,
                                  double x1, double y1) {
        assertNotNull(caretShape);
        assertEquals(2, caretShape.length);
        assertNotNull(caretShape[0]);
        assertNotNull(caretShape[1]);
        assertEquals(caretShape[0].getClass(), MoveTo.class);
        assertEquals(caretShape[1].getClass(), LineTo.class);
        MoveTo m = (MoveTo)caretShape[0];
        LineTo l = (LineTo)caretShape[1];
        double e = 0.00001;
        assertEquals(m.getX(), x0, e);
        assertEquals(m.getY(), y0, e);
        assertEquals(l.getX(), x1, e);
        assertEquals(l.getY(), y1, e);
    }
    
    @SuppressWarnings("deprecation")
    @Test public void testCaretShape() {
        Font font = new Font("Monospaced Regular", 16);
        Text text = new Text("a");
        text.setFont(font);
        text.setTextOrigin(VPos.TOP);
        //based on current implementation, caret just vertical line
        Bounds bounds = text.getLayoutBounds();
        float lineHeight = (float)bounds.getHeight();
        float avgChar = (float)bounds.getWidth();
        PathElement[] empty = {};

        assertEquals(empty, text.getImpl_caretShape()); //initially empty
        
        text.setImpl_caretPosition(0);
        assertCaretEquals(text.getImpl_caretShape(), 0, 0, 0, lineHeight);
        
        text.setImpl_caretPosition(-1);
        assertEquals(empty, text.getImpl_caretShape()); //empty after -1
        
        //set back
        text.setImpl_caretPosition(0);
        assertCaretEquals(text.getImpl_caretShape(), 0, 0, 0, lineHeight);
        
        text.setImpl_caretBias(false);
        text.setText("abc");
        assertEquals(empty, text.getImpl_caretShape()); //empty after setText
        assertEquals(-1, text.getImpl_caretPosition());
        assertEquals(true, text.impl_caretBiasProperty().get());
        
        
        // trailing edges
        text.setImpl_caretPosition(0);
        text.setImpl_caretBias(true);
        assertCaretEquals(text.getImpl_caretShape(), 0, 0, 0, lineHeight);
        text.setImpl_caretPosition(0);
        text.setImpl_caretBias(false);
        assertCaretEquals(text.getImpl_caretShape(), avgChar, 0, avgChar, lineHeight);
        text.setImpl_caretPosition(1);
        text.setImpl_caretBias(true);
        assertCaretEquals(text.getImpl_caretShape(), avgChar, 0, avgChar, lineHeight);
        text.setImpl_caretPosition(1);
        text.setImpl_caretBias(false);
        assertCaretEquals(text.getImpl_caretShape(), avgChar*2, 0, avgChar*2, lineHeight);
        text.setImpl_caretPosition(2);
        text.setImpl_caretBias(true);
        assertCaretEquals(text.getImpl_caretShape(), avgChar*2, 0, avgChar*2, lineHeight);
        text.setImpl_caretPosition(2);
        text.setImpl_caretBias(false);
        assertCaretEquals(text.getImpl_caretShape(), avgChar*3, 0, avgChar*3, lineHeight);

        //test length
        text.setImpl_caretPosition(3);
        text.setImpl_caretBias(true);
        assertCaretEquals(text.getImpl_caretShape(), avgChar*3, 0, avgChar*3, lineHeight);
        text.setImpl_caretPosition(3);
        text.setImpl_caretBias(false);
        assertCaretEquals(text.getImpl_caretShape(), avgChar*3, 0, avgChar*3, lineHeight);
        
        //test out of bounds
        text.setImpl_caretPosition(4);
        text.setImpl_caretBias(true);
        assertEquals(empty, text.getImpl_caretShape());
        text.setImpl_caretPosition(4);
        text.setImpl_caretBias(false);
        assertEquals(empty, text.getImpl_caretShape());
        
        //test empty text
        text.setText("");
        text.setImpl_caretPosition(0);
        text.setImpl_caretBias(true);
        assertCaretEquals(text.getImpl_caretShape(), 0, 0, 0, lineHeight);
    }


    @SuppressWarnings("deprecation")
    @Test public void testSelectionShape() {
        Font font = new Font("Monospaced Regular", 16);
        Text text = new Text("a");
        text.setFont(font);
        text.setTextOrigin(VPos.TOP);
        //based on current implementation, caret just vertical line
        Bounds bounds = text.getLayoutBounds();
        float lineHeight = (float)bounds.getHeight();
        float avgChar = (float)bounds.getWidth();
        PathElement[] empty = {};
        
        assertEquals(empty, text.getImpl_selectionShape()); //initially null
        
        text.setImpl_selectionStart(0);
        assertEquals(empty, text.getImpl_selectionShape()); //set start, but not end
        
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionStart(-1);
        assertEquals(empty, text.getImpl_selectionShape());; //no start

        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionEnd(-1);
        assertEquals(empty, text.getImpl_selectionShape()); //no end

        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionStart(1);
        text.setImpl_selectionEnd(0);
        assertEquals(empty, text.getImpl_selectionShape()); //end > start
        
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(0);
        assertEquals(empty, text.getImpl_selectionShape()); //end == start

        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(3);
        assertEquals(empty, text.getImpl_selectionShape()); //end > length
        
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);

        text.setImpl_selectionStart(3);
        text.setImpl_selectionEnd(5);
        assertEquals(empty, text.getImpl_selectionShape()); //start > length
        
        text.setText("abc");
        assertEquals(empty, text.getImpl_selectionShape()); //setText resets
        assertEquals(-1, text.getImpl_selectionStart());
        assertEquals(-1, text.getImpl_selectionEnd());
        
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(0);
        assertEquals(empty, text.getImpl_selectionShape()); 
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(1);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, avgChar, lineHeight);
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(2);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, 2*avgChar, lineHeight);
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(3);
        assertBoundsEquals(text.getImpl_selectionShape(), 0, 0, 3*avgChar, lineHeight);
        text.setImpl_selectionStart(0);
        text.setImpl_selectionEnd(4);
        assertEquals(empty, text.getImpl_selectionShape()); 
        text.setImpl_selectionStart(1);
        text.setImpl_selectionEnd(2);
        assertBoundsEquals(text.getImpl_selectionShape(), avgChar, 0, avgChar, lineHeight);
        text.setImpl_selectionStart(1);
        text.setImpl_selectionEnd(3);
        assertBoundsEquals(text.getImpl_selectionShape(), avgChar, 0, 2*avgChar, lineHeight);
        text.setImpl_selectionStart(2);
        text.setImpl_selectionEnd(3);
        assertBoundsEquals(text.getImpl_selectionShape(), 2*avgChar, 0, avgChar, lineHeight);
        text.setImpl_selectionStart(3);
        text.setImpl_selectionEnd(3);
        assertEquals(empty, text.getImpl_selectionShape()); 
    }
    
}
