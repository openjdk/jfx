/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import java.util.ArrayList;
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
        PseudoClassImpl.stateMap.clear();        
    }
    
    @Test
    public void testGetState() {
        String pseudoClass = "xyzzy";
        PseudoClass result = PseudoClass.getPseudoClass(pseudoClass);
        assertEquals(pseudoClass, result.getPseudoClassName());
    }

    @Test
    public void testCreateStatesInstance() {
        PseudoClassSet result = new PseudoClassSet();
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
    public void testPseudoClassSet_add() {
        String pseudoClass = "xyzzy";
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        PseudoClassSet states = new PseudoClassSet();
        states.add(state);
        List<PseudoClass> stateList = states.getPseudoClasses();
        assertTrue(stateList.contains(state));
    }

    @Test
    public void testPseudoClassSet_add_multipleStates() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        List<PseudoClass> stateList = states.getPseudoClasses();
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            assertTrue(stateList.contains(state));
        }
        
    }
    
    @Test
    public void testPseudoClassSet_contains() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        
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
    public void testPseudoClassSet_contains_negativeTest() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        String[] notExpected = new String[] {
            "six", "seven", "eight"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        
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
    public void testPseudoClassSet_contains_null() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        assertTrue(states.contains(null) == false);
        
    }
    
    @Test
    public void testPseudoClassSet_removeState() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        String[] expected = new String[] {
            "one", "two", "four", "five"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        
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
    public void testPseudoClassSet_containsAll() {
        
        String[] setA = new String[] {
            "zero", "one"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates));
    }    

    @Test
    public void testPseudoClassSet_containsAll_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates) == false);
    }    

    @Test
    public void testPseudoClassSet_containsAll_whenSetsAreEqual() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates));
    }    

    @Test
    public void testPseudoClassSet_containsAll_whenSetsDisjoint() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "six", "seven"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(bStates.containsAll(aStates) == false);
    }        
        
    @Test
    public void testPseudoClassSet_size() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        PseudoClassSet states = new PseudoClassSet();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        assertEquals(expected, states.size());
    }
    
    @Test
    public void testPseudoClassSet_size_afterRemove() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 4;
        
        PseudoClassSet states = new PseudoClassSet();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        states.remove(PseudoClass.getPseudoClass("three"));
        
        assertEquals(expected, states.size());
    }

    @Test
    public void testPseudoClassSet_size_afterRemoveOnEmpty() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 0;
        
        PseudoClassSet states = new PseudoClassSet();
        
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
    public void testPseudoClassSet_size_afterAddWhenAlreadyContains() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        PseudoClassSet states = new PseudoClassSet();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
        }
        
        PseudoClass state = PseudoClass.getPseudoClass("three");
        states.add(state);
       
        assertEquals(expected, states.size());
    }

    @Test
    public void testPseudoClassSet_isEmpty() {
        PseudoClassSet states = new PseudoClassSet();
        assertTrue(states.isEmpty());
    }

    @Test
    public void testPseudoClassSet_isEmpty_negativeTest() {
        PseudoClassSet states = new PseudoClassSet();
        states.add(PseudoClass.getPseudoClass("pseudo-class"));
        assertTrue(states.isEmpty() == false);
    }
    
    @Test
    public void testPseudoClassSet_isEmpty_whenBitMaskLengthGreaterThanOne() {
        PseudoClassSet states = new PseudoClassSet();
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
    public void testPseudoClassSet_equals() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertTrue(aStates.equals(bStates));
    }    
    
    @Test
    public void testPseudoClassSet_equals_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "four"            
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                        
        assertFalse(aStates.equals(bStates));
    }    

    @Test
    public void testPseudoClassSet_retainAll() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "one", "two", "three"
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[] {
            "one", "two", "three"
        };
        
        aStates.retainAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassSet_retainAll_withEmptyStates() {
        
        PseudoClassSet bStates = new PseudoClassSet();
        PseudoClassSet aStates = new PseudoClassSet();        
        aStates.retainAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testPseudoClassSet_retainAll_withNullArg() {
        
        PseudoClassSet aStates = new PseudoClassSet();
        PseudoClassSet bStates = null;
        aStates.retainAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testPseudoClassSet_retainAll_disjointYieldsEmpty() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[0];
        
        aStates.retainAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassSet_addAll() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet aStates = new PseudoClassSet();
        for(int n=0; n<setA.length; n++) {
            aStates.add(PseudoClass.getPseudoClass(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassSet bStates = new PseudoClassSet();
        for(int n=0; n<setB.length; n++) {
            bStates.add(PseudoClass.getPseudoClass(setB[n]));
        }
                
        String[] expected = new String[] {
            "zero", "one", "two", "three",
            "four", "five"            
        };
        
        aStates.addAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue(aStates.contains(state));
        }
    }
    
    @Test
    public void testPseudoClassSet_addAll_withEmptyStates() {
        
        PseudoClassSet bStates = new PseudoClassSet();
        PseudoClassSet aStates = new PseudoClassSet();        
        aStates.addAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testPseudoClassSet_addAll_withNullArgs() {
        
        PseudoClassSet aStates = new PseudoClassSet();
        PseudoClassSet bStates = null;
        aStates.addAll(bStates);
        List<PseudoClass> states = aStates.getPseudoClasses();
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testPseudoClassSet_getPseudoClasses() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        List<PseudoClass> expected = new ArrayList<PseudoClass>();
        PseudoClassSet states = new PseudoClassSet();
        for(int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            states.add(state);
            expected.add(state);            
        }
        
        List<PseudoClass> stateList = states.getPseudoClasses();
        
        assertTrue(expected.containsAll(stateList));
    }
        
    @Test
    public void testPseudoClassSet_toString() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        
        PseudoClassSet states = new PseudoClassSet();
        for(int n=0; n<pseudoClasses.length; n++) {
            states.add(PseudoClass.getPseudoClass(pseudoClasses[n]));
        }
        
        List<PseudoClass> stateList = states.getPseudoClasses();
        String expected = stateList.toString();
        
        String result = states.toString();
        
        assertEquals(expected, result);
    }
    
    @Test
    public void testState_getState_throwsIndexOutOfBounds() {
    
        int n = 0;
        try {
            while (n < 1024) {
                PseudoClass.getPseudoClass("pseudoClass-"+Integer.toString(n));
                n += 1;
            }
            fail(Integer.toString(n));
        } catch (IndexOutOfBoundsException exception) {
            assertEquals(1020, n, 0.000001);
        }
    }
}
