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

package com.sun.javafx.css;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author dgrieve
 */
public class PseudoClassTest {
    
    public PseudoClassTest() {
    }

    @Before
    public void before() {
        PseudoClassState.pseudoClassMap.clear();        
        PseudoClassState.pseudoClasses.clear();       
    }
    
    @Test
    public void testGetState() {
        String pseudoClass = "xyzzy";
        PseudoClass result = PseudoClass.getPseudoClass(pseudoClass);
        assertEquals(pseudoClass, result.getPseudoClassName());
    }

    @Test
    public void testCreateStatesInstance() {
        PseudoClassState result = new PseudoClassState();
        assertNotNull(result);
    }

    @Test
    public void testGetStateThrowsIllegalArgumentExceptionWithNullArg() {
        try {
            PseudoClass state = PseudoClass.getPseudoClass(null);
            fail();
        } catch (IllegalArgumentException exception) {
        }
    }
    
    @Test
    public void testGetStateThrowsIllegalArgumentExceptionWithEmptyArg() {
        try {
            PseudoClass state = PseudoClass.getPseudoClass(" ");
            fail();
        } catch (IllegalArgumentException exception) {
        }
    }
    
    @Test
    public void testState_getPseudoClass() {
        // no different than testing the getPseudoClassName method!
        String pseudoClass = "xyzzy";
        PseudoClass result = PseudoClass.getPseudoClass(pseudoClass);
        assertEquals(pseudoClass, result.getPseudoClassName());        
    }

    @Test
    public void testState_toString() {
        String pseudoClass = "xyzzy";
        PseudoClass result = PseudoClass.getPseudoClass(pseudoClass);
        assertEquals(pseudoClass, result.toString());        
    }

    @Test
    public void testPseudoClassState_add() {
        String pseudoClass = "xyzzy";
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        PseudoClassState states = new PseudoClassState();
        states.add(state);
        
        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = states.iterator();
        while (iter.hasNext()) {
            stateList.add(iter.next());
        }
        
        assertTrue(stateList.contains(state));
    }

    @Test
    public void testPseudoClassState_add_multipleStates() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = states.iterator();
        while (iter.hasNext()) {
            stateList.add(iter.next());
        }

        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            assertTrue(stateList.contains(state));
        }
        
    }
    
    @Test
    public void testPseudoClassState_contains() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            assertTrue(states.contains(state));
        }
        
    }
    
    @Test
    public void testPseudoClassState_contains_negativeTest() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        String[] notExpected = new String[] {
            "six", "seven", "eight"
        };
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        for (int n=0; n<notExpected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(notExpected[n]);
            assertTrue(states.contains(state) == false);
        }
        
    }

    @Test
    public void testPseudoClassState_contains_null() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        assertTrue(states.contains(null) == false);
        
    }
    
    @Test
    public void testPseudoClassState_removeState() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        String[] expected = new String[] {
            "one", "two", "four", "five"
        };
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        states.remove(PseudoClass.getPseudoClass("three"));
        
        assertTrue(states.contains(PseudoClass.getPseudoClass("three"))==false);
        
        // should still have the others.
        for (int n=1; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(states.contains(state));
        }
        
    }

    @Test
    public void testPseudoClassState_containsAll() {
        
        String[] setA = new String[] {
            "zero", "one"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates));
    }    

    @Test
    public void testPseudoClassState_containsAll_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates) == false);
    }    

    @Test
    public void testPseudoClassState_containsAll_whenSetsAreEqual() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates));
    }    

    @Test
    public void testPseudoClassState_containsAll_whenSetsDisjoint() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "six", "seven"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates) == false);
    }

    @Test
    public void testPseudoClassState_containsAll_whenOneSetEmpty() {

        String[] setA = new String[] {
                "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }

        PseudoClassState bStates = new PseudoClassState();

        assertTrue(bStates.containsAll(aStates) == false);
        assertTrue(aStates.containsAll(bStates));

    }

    @Test
    public void testPseudoClassState_containsAll_whenBothSetsEmpty() {

        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = new PseudoClassState();

        assertTrue(bStates.containsAll(aStates));
        assertTrue(aStates.containsAll(bStates));

    }

    @Test
    public void testPseudoClassState_size() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        assertEquals(expected, states.size());
    }
    
    @Test
    public void testPseudoClassState_size_afterRemove() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 4;
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        states.remove(PseudoClass.getPseudoClass("three"));
        
        assertEquals(expected, states.size());
    }

    @Test
    public void testPseudoClassState_size_afterRemoveOnEmpty() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 0;
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.remove(state);
        }
        
        assertEquals(expected, states.size());
    }

    @Test
    public void testPseudoClassState_size_afterAddWhenAlreadyContains() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        PseudoClass state = PseudoClass.getPseudoClass("three");
        states.add(state);
       
        assertEquals(expected, states.size());
    }

    @Test
    public void testPseudoClassState_isEmpty() {
        PseudoClassState states = new PseudoClassState();
        assertTrue(states.isEmpty());
    }

    @Test
    public void testPseudoClassState_isEmpty_negativeTest() {
        PseudoClassState states = new PseudoClassState();
        states.add(PseudoClass.getPseudoClass("pseudo-class"));
        assertTrue(states.isEmpty() == false);
    }
    
    @Test
    public void testPseudoClassState_isEmpty_whenBitMaskLengthGreaterThanOne() {
        PseudoClassState states = new PseudoClassState();
        PseudoClass state = null;
        try {
            for(int n=0; n<512; n++) {
                state = PseudoClass.getPseudoClass("pseudoClass-"+Integer.valueOf(n));
            }
        } catch (IndexOutOfBoundsException exception) {
            fail();
        }
        
        states.add(state);
        assertTrue(states.isEmpty() == false);
    }
    
    @Test
    public void testPseudoClassState_equals() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(aStates.equals(bStates));
    }    
    
    @Test
    public void testPseudoClassState_equals_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "four"            
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertFalse(aStates.equals(bStates));
    }    

    @Test
    public void testPseudoClassState_retainAll() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "one", "two", "three"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[] {
            "one", "two", "three"
        };
        
        aStates.retainAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassState_retainAll_withEmptyStates() {
        
        PseudoClassState bStates = new PseudoClassState();
        PseudoClassState aStates = new PseudoClassState();        
        aStates.retainAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testPseudoClassState_retainAll_withNullArg() {
        
        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = null;
        aStates.retainAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testPseudoClassState_retainAll_disjointYieldsEmpty() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[0];
        
        aStates.retainAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassState_addAll() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[] {
            "zero", "one", "two", "three",
            "four", "five"            
        };
        
        aStates.addAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassState_addAll_withEmptyStates() {
        
        PseudoClassState bStates = new PseudoClassState();
        PseudoClassState aStates = new PseudoClassState();        
        aStates.addAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testPseudoClassState_addAll_withNullArgs() {
        
        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = null;
        aStates.addAll(bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = aStates.iterator();
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testPseudoClassState_getPseudoClasses() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        List<PseudoClass> expected = new ArrayList<PseudoClass>();
        PseudoClassState states = new PseudoClassState();
        for(int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
            expected.add(state);            
        }
        
        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = states.iterator();
        while (iter.hasNext()) {
            stateList.add(iter.next());
        }
        
        assertTrue(expected.containsAll(stateList));
    }
        
    @Test
    public void testPseudoClassState_toString() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassState states = new PseudoClassState();
        for(int n=0; n<pseudoClasses.length; n++) {
            states.add(PseudoClass.getPseudoClass(pseudoClasses[n]));
        }
        
        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = states.iterator();
        while (iter.hasNext()) {
            stateList.add(iter.next());
        }
        String expected = stateList.toString();
        
        String result = states.toString();
        
        assertEquals(expected, result);
    }

    @Test public void testPseudoClassState_iterator() {
        
        PseudoClass[] pseudoClasses = new PseudoClass[4];
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            pseudoClasses[n] = PseudoClass.getPseudoClass(Integer.toString(n));
            states.add(pseudoClasses[n]);
        };
        
        int iterations = 0;
        Iterator<PseudoClass> iter = states.iterator();
        while(iter.hasNext()) {
            
            iterations += 1;
            assertTrue (iterations+">"+pseudoClasses.length, iterations <= pseudoClasses.length);
            
            PseudoClass pseudoClass = iter.next();
            assertEquals (pseudoClass, pseudoClasses[iterations-1]);
        }
        
        assertTrue (pseudoClasses.length+"!="+iterations, pseudoClasses.length == iterations);
        
    }

    @Test public void testPseudoClassState_iterator_withLargeNumberOfPsuedoClasses() {
        
        PseudoClass[] pseudoClasses = new PseudoClass[Long.SIZE*3];
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            pseudoClasses[n] = PseudoClass.getPseudoClass(Integer.toString(n));
            states.add(pseudoClasses[n]);
        };
        
        int iterations = 0;
        Iterator<PseudoClass> iter = states.iterator();
        while(iter.hasNext()) {
            
            iterations += 1;
            assertTrue (iterations+">"+pseudoClasses.length, iterations <= pseudoClasses.length);
            
            PseudoClass pseudoClass = iter.next();
            assertEquals (pseudoClass, pseudoClasses[iterations-1]);
        }
        
        assertTrue (pseudoClasses.length+"!="+iterations, pseudoClasses.length == iterations);
        
    }

    @Test public void testPseudoClassState_iterator_remove() {
        
        PseudoClass[] pseudoClasses = new PseudoClass[4];
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            pseudoClasses[n] = PseudoClass.getPseudoClass(Integer.toString(n));
            states.add(pseudoClasses[n]);
        };
        
        int iterations = 0;
        int nPseudoClasses = pseudoClasses.length;
        Iterator<PseudoClass> iter = states.iterator();
        while(iter.hasNext()) {
                     
            ++iterations; 
            
            if ((iterations % 2) == 0) {
                iter.remove();
                --nPseudoClasses;
                assertFalse(states.contains(pseudoClasses[iterations-1]));
            }
        }
        
        assertTrue (nPseudoClasses+"!="+states.size(), nPseudoClasses == states.size());
        
    }

    @Test public void testPseudoClassState_iterator_remove_withLargeNumberOfPseudoClasses() {
        
        PseudoClass[] pseudoClasses = new PseudoClass[Long.SIZE*3];
        PseudoClassState states = new PseudoClassState();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            pseudoClasses[n] = PseudoClass.getPseudoClass(Integer.toString(n));
            states.add(pseudoClasses[n]);
        };
        
        int iterations = 0;
        int nPseudoClasses = pseudoClasses.length;
        Iterator<PseudoClass> iter = states.iterator();
        while(iter.hasNext()) {
                        
            ++iterations;
            
            if ((iterations % 2) == 0) {
                iter.remove();
                --nPseudoClasses;
                assertFalse(states.contains(pseudoClasses[iterations-1]));
            }
        }
        
        assertTrue (nPseudoClasses+"!="+states.size(), nPseudoClasses == states.size());
        
    }

    int nObservations = 0;

    @Test public void testObservablePseudoClass_listener_add() {


        final PseudoClass[] expectedObservations = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };
        final int nObservationsExpected = expectedObservations.length;
        nObservations = 0;

        ObservableSet<PseudoClass> pseudoClasses = new PseudoClassState();
        pseudoClasses.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasAdded()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementAdded();
                    assertSame(expectedObservations[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        for (int n=0; n<expectedObservations.length; n++) {
            pseudoClasses.add(expectedObservations[n]);
        };

        assertEquals(nObservationsExpected, nObservations);

    }

    @Test public void testObservablePseudoClass_listener_remove() {


        final PseudoClass[] expectedObservations = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };
        final int nObservationsExpected = expectedObservations.length;
        nObservations = 0;

        ObservableSet<PseudoClass> pseudoClasses = new PseudoClassState();
        for (int n=0; n<expectedObservations.length; n++) {
            pseudoClasses.add(expectedObservations[n]);
        };

        pseudoClasses.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasRemoved()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementRemoved();
                    assertSame(expectedObservations[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        for (int n=0; n<expectedObservations.length; n++) {
            pseudoClasses.remove(expectedObservations[n]);
        };

        assertEquals(nObservationsExpected, nObservations);

    }

    @Test public void testObservablePseudoClass_listener_iter_remove() {


        final PseudoClass[] expectedObservations = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };
        final int nObservationsExpected = expectedObservations.length;
        nObservations = 0;

        ObservableSet<PseudoClass> pseudoClasses = new PseudoClassState();
        for (int n=0; n<expectedObservations.length; n++) {
            pseudoClasses.add(expectedObservations[n]);
        };

        pseudoClasses.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasRemoved()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementRemoved();
                    assertSame(expectedObservations[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        for (Iterator<PseudoClass> iter = pseudoClasses.iterator(); iter.hasNext();) {
            iter.remove();
        };

        assertEquals(nObservationsExpected, nObservations);

    }

    @Test public void testObservablePseudoClass_listener_addAll() {


        final PseudoClass[] expectedObservations = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };
        final int nObservationsExpected = expectedObservations.length;
        nObservations = 0;

        Set<PseudoClass> pseudoClassesToAdd = new PseudoClassState();
        for (int n=0; n<expectedObservations.length; n++) {
            pseudoClassesToAdd.add(expectedObservations[n]);
        };

        ObservableSet<PseudoClass> pseudoClasses = new PseudoClassState();
        pseudoClasses.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasAdded()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementAdded();
                    assertSame(expectedObservations[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        pseudoClasses.addAll(pseudoClassesToAdd);

        assertEquals(nObservationsExpected, nObservations);

    }

    @Test public void testObservablePseudoClass_listener_removeAll() {


        final PseudoClass[] pseudoClassesToRemove = new PseudoClass[] {
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        final int nObservationsExpected = pseudoClassesToRemove.length;
        nObservations = 0;

        Set<PseudoClass> other = new PseudoClassState();
        for (int n=0; n<pseudoClassesToRemove.length; n++) {
            other.add(pseudoClassesToRemove[n]);
        };

        ObservableSet<PseudoClass> master = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            master.add(pseudoClasses[n]);
        };

        master.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasRemoved()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementRemoved();
                    assertSame(pseudoClassesToRemove[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        master.removeAll(other);

        assertEquals(nObservationsExpected, nObservations);
        assertEquals(pseudoClasses.length-pseudoClassesToRemove.length, master.size());

    }

    @Test public void testObservablePseudoClass_listener_retainAll() {


        final PseudoClass[] pseudoClassesToRetain = new PseudoClass[] {
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE")

        };

        final PseudoClass[] removedPseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        final int nObservationsExpected = pseudoClassesToRetain.length;
        nObservations = 0;

        Set<PseudoClass> other = new PseudoClassState();
        for (int n=0; n<pseudoClassesToRetain.length; n++) {
            other.add(pseudoClassesToRetain[n]);
        };

        ObservableSet<PseudoClass> master = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            master.add(pseudoClasses[n]);
        };

        master.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasRemoved()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementRemoved();
                    assertSame(removedPseudoClasses[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        master.retainAll(other);

        assertEquals(nObservationsExpected, nObservations);
        assertEquals(pseudoClassesToRetain.length, master.size());

    }

    @Test public void testObservablePseudoClass_listener_clear() {


        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        final int nObservationsExpected = pseudoClasses.length;
        nObservations = 0;

        ObservableSet<PseudoClass> master = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            master.add(pseudoClasses[n]);
        };

        master.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                if (change.wasRemoved()) {
                    assert (nObservations < nObservationsExpected);
                    PseudoClass observed = change.getElementRemoved();
                    assertSame(pseudoClasses[nObservations], observed);
                    nObservations += 1;
                } else {
                    fail();
                }
            }
        });

        master.clear();

        assertEquals(nObservationsExpected, nObservations);
        assertTrue(master.isEmpty());

    }

    @Test public void testObservablePseudoClass_listener_getSet_unmodifiable() {

        final ObservableSet<PseudoClass> master = new PseudoClassState();

        master.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                master.removeListener(this);
                try {
                    ObservableSet set = change.getSet();
                    set.add(PseudoClass.getPseudoClass("TWO"));
                    fail();
                } catch (UnsupportedOperationException e) {
                    // This is the exception we expect from an unmodifiable set
                } catch (Exception other) {
                    fail(other.getMessage());
                }
            }
        });

        master.add(PseudoClass.getPseudoClass("ONE"));

    }


}
