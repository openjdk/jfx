/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    
}
