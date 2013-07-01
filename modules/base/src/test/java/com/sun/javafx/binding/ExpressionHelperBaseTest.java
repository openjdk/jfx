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

package com.sun.javafx.binding;

import static org.junit.Assert.*;

import javafx.beans.WeakListener;
import org.junit.Test;

public class ExpressionHelperBaseTest {
    
    private static final Object listener = new Object();
    private static final Object listener2 = new Object();
    
    private static final WeakListener validWeakListener = new WeakListener() {
        @Override
        public boolean wasGarbageCollected() {
            return false;
        }
    };
    
    private static final WeakListener gcedWeakListener = new WeakListener() {
        @Override
        public boolean wasGarbageCollected() {
            return true;
        }
    };
    
    @Test
    public void testEmptyArray() {
        Object[] array = new Object[0];
        assertEquals(0, ExpressionHelperBase.trim(0, array));
        assertArrayEquals(new Object[0], array);
        
        array = new Object[1];
        assertEquals(0, ExpressionHelperBase.trim(0, array));
        assertArrayEquals(new Object[1], array);
    }
    
    @Test
    public void testSingleElement() {
        Object[] array = new Object[] {listener};
        assertEquals(1, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {listener}, array);
        
        array = new Object[] {validWeakListener};
        assertEquals(1, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {validWeakListener}, array);
        
        array = new Object[] {gcedWeakListener};
        assertEquals(0, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {null}, array);
        
        array = new Object[] {listener, null};
        assertEquals(1, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {listener, null}, array);
        
        array = new Object[] {validWeakListener, null};
        assertEquals(1, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {validWeakListener, null}, array);
        
        array = new Object[] {gcedWeakListener, null};
        assertEquals(0, ExpressionHelperBase.trim(1, array));
        assertArrayEquals(new Object[] {null, null}, array);
    }
    
    @Test
    public void testMultipleElements() {
        Object[] array = new Object[] {validWeakListener, listener, listener2};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, listener2}, array);
        
        array = new Object[] {listener, validWeakListener, listener2};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, listener2}, array);
        
        array = new Object[] {listener, listener2, validWeakListener};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, listener2, validWeakListener}, array);
        
        array = new Object[] {validWeakListener, listener, listener2, null};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, listener2, null}, array);
        
        array = new Object[] {listener, validWeakListener, listener2, null};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, listener2, null}, array);
        
        array = new Object[] {listener, listener2, validWeakListener, null};
        assertEquals(3, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, listener2, validWeakListener, null}, array);


        array = new Object[] {gcedWeakListener, validWeakListener, listener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null}, array);
        
        array = new Object[] {gcedWeakListener, listener, validWeakListener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null}, array);
        
        array = new Object[] {gcedWeakListener, validWeakListener, listener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null, null}, array);
        
        array = new Object[] {gcedWeakListener, listener, validWeakListener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null, null}, array);
        

        array = new Object[] {validWeakListener, gcedWeakListener, listener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null}, array);
        
        array = new Object[] {listener, gcedWeakListener, validWeakListener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null}, array);
        
        array = new Object[] {validWeakListener, gcedWeakListener, listener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null, null}, array);
        
        array = new Object[] {listener, gcedWeakListener, validWeakListener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null, null}, array);
        

        array = new Object[] {validWeakListener, listener, gcedWeakListener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null}, array);
        
        array = new Object[] {listener, validWeakListener, gcedWeakListener};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null}, array);
        
        array = new Object[] {validWeakListener, listener, gcedWeakListener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {validWeakListener, listener, null, null}, array);
        
        array = new Object[] {listener, validWeakListener, gcedWeakListener, null};
        assertEquals(2, ExpressionHelperBase.trim(3, array));
        assertArrayEquals(new Object[] {listener, validWeakListener, null, null}, array);
        
        
        array = new Object[] {gcedWeakListener, gcedWeakListener};
        assertEquals(0, ExpressionHelperBase.trim(2, array));
        assertArrayEquals(new Object[] {null, null}, array);
        
        array = new Object[] {gcedWeakListener, gcedWeakListener, null};
        assertEquals(0, ExpressionHelperBase.trim(2, array));
        assertArrayEquals(new Object[] {null, null, null}, array);
    }
    
    
}
