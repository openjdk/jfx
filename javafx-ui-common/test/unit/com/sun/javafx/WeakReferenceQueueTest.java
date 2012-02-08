/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

public class WeakReferenceQueueTest {
    @Test
    public void testAdd() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String s = new String("Wow!");
        q.add(s);
        assertEquals(1, q.size);
    }
    
    @Test
    public void testRemove() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);
        
        assertEquals(3, q.size);
        q.remove(a);
        q.remove(c);
        assertEquals(1, q.size);
    }
    
    @Test
    public void testCleanup() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);
        
        assertEquals(3, q.size);
        a = null;
        c = null;
        tryGCReallyHard();
        q.cleanup();
        assertEquals(1, q.size);
    }
    
    @Test
    public void testIterator() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);
        
        // This part of the test requires knowledge that iteration
        // is from last to first
        Iterator itr = q.iterator();
        assertTrue(itr.hasNext());
        assertEquals(c, itr.next());
        assertTrue(itr.hasNext());
        assertEquals(b, itr.next());
        assertTrue(itr.hasNext());
        assertEquals(a, itr.next());
        assertFalse(itr.hasNext());
        
        // and for good measure do it again without calling hasNext just
        // to make sure calling hasNext isn't a requirement
        itr = q.iterator();
        assertEquals(c, itr.next());
        assertEquals(b, itr.next());
        assertEquals(a, itr.next());
    }
    
    @Test
    public void testEmptyIterator() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        Iterator itr = q.iterator();
        assertFalse(itr.hasNext());
    }
    
    @Test
    public void testIteratorRemove() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);
        
        Iterator itr = q.iterator();
        itr.next(); // gives me "c"
        itr.remove();
        assertEquals(2, q.size);
        itr.next(); // gives me "b"
        itr.remove();
        assertEquals(1, q.size);
        itr.next(); // gives me "a"
        itr.remove();
        assertEquals(0, q.size);
        
        q.add(a);
        q.add(b);
        q.add(c);
        itr = q.iterator();
        itr.next();
        itr.next(); // gives me "b"
        itr.remove();
        
        itr = q.iterator();
        assertEquals(c, itr.next());
        assertEquals(a, itr.next());
    }
    
    @Test
    public void testIteratingOverSparseQueue() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);
        
        assertEquals(3, q.size);
        a = null;
        c = null;
        tryGCReallyHard();
        q.cleanup();

        Iterator itr = q.iterator();
        assertEquals(b, itr.next());
        assertFalse(itr.hasNext());
    }

    @Test
    public void testIteratingOverSparseQueueWithoutCleanup() {
        WeakReferenceQueue q = new WeakReferenceQueue();
        String a = new String("a");
        q.add(a);
        String b = new String("b");
        q.add(b);
        String c = new String("c");
        q.add(c);

        assertEquals(3, q.size);
        a = null;
        c = null;
        tryGCReallyHard();

        Iterator itr = q.iterator();
        assertEquals(b, itr.next());
        assertFalse(itr.hasNext());
    }

    private void tryGCReallyHard() {
        // produce some garbage to increase the need of performing gc
        for (int i = 0; i < 100000; i++) {
            String s = new String("GARBAGE");
        }

        // now, yell at the VM to run gc
        for (int i = 0; i < 10; i++) {
            System.gc();
            System.gc();
            System.gc();
        }

        // finally, give the VM some idle time to perform gc
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // hope that worked!
    }
}
