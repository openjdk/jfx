/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import com.sun.javafx.css.PseudoClass.State;
import com.sun.javafx.css.PseudoClass.States;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author dgrieve
 */
public class PseudoClassTest {
    
    public PseudoClassTest() {
    }

    @Before
    public void before() {
        PseudoClass.stateMap.clear();        
    }
    
    @Test
    public void testGetState() {
        String pseudoClass = "xyzzy";
        State result = PseudoClass.getState(pseudoClass);
        assertEquals(pseudoClass, result.getPseudoClass());
    }

    @Test
    public void testCreateStatesInstance() {
        States result = PseudoClass.createStatesInstance();
        assertNotNull(result);
    }

    @Test
    public void testCreateStatesInstance_WithList() {
        List<String> pseudoClasses = new ArrayList<String>();
        Collections.addAll(pseudoClasses, "one","two","three");
        States result = PseudoClass.createStatesInstance(pseudoClasses);
        assert(result.getCount() == 3);
    }
    
    @Test
    public void testGetStateThrowsIllegalArgumentExceptionWithNullArg() {
        try {
            State state = PseudoClass.getState(null);
            fail();
        } catch (IllegalArgumentException exception) {
        }
    }
    
    @Test
    public void testGetStateThrowsIllegalArgumentExceptionWithEmptyArg() {
        try {
            State state = PseudoClass.getState(" ");
            fail();
        } catch (IllegalArgumentException exception) {
        }
    }
    
    @Test
    public void testState_getPseudoClass() {
        // no different than testing the getState method!
        String pseudoClass = "xyzzy";
        State result = PseudoClass.getState(pseudoClass);
        assertEquals(pseudoClass, result.getPseudoClass());        
    }

    @Test
    public void testState_toString() {
        String pseudoClass = "xyzzy";
        State result = PseudoClass.getState(pseudoClass);
        assertEquals(pseudoClass, result.toString());        
    }

    @Test
    public void testStates_addState() {
        String pseudoClass = "xyzzy";
        State state = PseudoClass.getState(pseudoClass);
        States states = PseudoClass.createStatesInstance();
        states.addState(state);
        List<State> stateList = states.getStates();
        assertTrue(stateList.contains(state));
    }

    @Test
    public void testStates_addState_multipleStates() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        List<State> stateList = states.getStates();
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            assertTrue(stateList.contains(state));
        }
        
    }
    
    @Test
    public void testStates_containsState() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            assertTrue(states.containsState(state));
        }
        
    }
    
    @Test
    public void testStates_containsState_negativeTest() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        String[] notExpected = new String[] {
            "six", "seven", "eight"
        };
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        for (int n=0; n<notExpected.length; n++) {
            State state = PseudoClass.getState(notExpected[n]);
            assertTrue(states.containsState(state) == false);
        }
        
    }

    @Test
    public void testStates_containsState_null() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };
        
        String[] notExpected = new String[] {
            "six", "seven", "eight"
        };
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        assertTrue(states.containsState(null) == false);
        
    }
    
    @Test
    public void testStates_removeState() {
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        String[] expected = new String[] {
            "one", "two", "four", "five"
        };
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        states.removeState(PseudoClass.getState("three"));
        
        assertTrue(states.containsState(PseudoClass.getState("three"))==false);
        
        // should still have the others.
        for (int n=1; n<expected.length; n++) {
            State state = PseudoClass.getState(expected[n]);
            assertTrue(states.containsState(state));
        }
        
    }

    @Test
    public void testStates_isSubsetOf() {
        
        String[] setA = new String[] {
            "zero", "one"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(aStates.isSubsetOf(bStates));
    }    

    @Test
    public void testStates_isSubsetOf_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(aStates.isSubsetOf(bStates) == false);
    }    

    @Test
    public void testStates_isSubsetOf_whenSetsAreEqual() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(aStates.isSubsetOf(bStates));
    }    

    @Test
    public void testStates_isSubsetOf_whenSetsDisjoint() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three" 
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "six", "seven"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(aStates.isSubsetOf(bStates) == false);
    }        
    
    @Test
    public void testStates_isSupersetOf() {
        
        String[] setA = new String[] {
            "zero", "one"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(bStates.isSupersetOf(aStates));
    }    
    
    @Test
    public void testStates_isSupersetOf_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(bStates.isSupersetOf(aStates) == false);
    }    

    @Test
    public void testStates_isSupersetOf_whenSetsAreEqual() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(bStates.isSupersetOf(aStates));
    }    

    @Test
    public void testStates_isSupersetOf_whenSetsAreDisjoint() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "six", "seven"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(bStates.isSupersetOf(aStates) == false);
    }    
    
    @Test
    public void testStates_getCount() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        assertEquals(expected, states.getCount());
    }
    
    @Test
    public void testStates_getCount_afterRemove() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 4;
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        states.removeState(PseudoClass.getState("three"));
        
        assertEquals(expected, states.getCount());
    }

    @Test
    public void testStates_getCount_afterRemoveOnEmpty() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 0;
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.removeState(state);
        }
        
        assertEquals(expected, states.getCount());
    }

    @Test
    public void testStates_getCount_afterAddWhenAlreadyContains() {
        
        String[] pseudoClasses = new String[] {
            "one", "two", "three", "four", "five"
        };

        int expected = 5;
        
        States states = PseudoClass.createStatesInstance();
        
        for (int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
        }
        
        State state = PseudoClass.getState("three");
        states.addState(state);
       
        assertEquals(expected, states.getCount());
    }

    @Test
    public void testStates_isEmpty() {
        States states = PseudoClass.createStatesInstance();
        assertTrue(states.isEmpty());
    }

    @Test
    public void testStates_isEmpty_negativeTest() {
        States states = PseudoClass.createStatesInstance();
        states.addState(PseudoClass.getState("pseudo-class"));
        assertTrue(states.isEmpty() == false);
    }
    
    @Test
    public void testStates_isEmpty_whenBitMaskLengthGreaterThanOne() {
        States states = PseudoClass.createStatesInstance();
        State state = null;
        try {
            for(int n=0; n<512; n++) {
                state = PseudoClass.getState("pseudoClass-"+Integer.valueOf(n));
            }
        } catch (IndexOutOfBoundsException exception) {
            fail();
        }
        
        states.addState(state);
        assertTrue(states.isEmpty() == false);
    }
    
    @Test
    public void testStates_equals() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "three"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertTrue(aStates.equals(bStates));
    }    
    
    @Test
    public void testStates_equals_negativeTest() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "zero", "one", "two", "four"            
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                        
        assertFalse(aStates.equals(bStates));
    }    

    @Test
    public void testStates_intersectionOf() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five", "one", "two", "three"
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                
        String[] expected = new String[] {
            "one", "two", "three"
        };
        
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            State state = PseudoClass.getState(expected[n]);
            assertTrue(intersection.containsState(state));
        }
    }
    
    @Test
    public void testStates_intersectionOf_withEmptyStates() {
        
        States bStates = PseudoClass.createStatesInstance();
        States aStates = PseudoClass.createStatesInstance();        
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testStates_intersectionOf_withNullArg() {
        
        States bStates = PseudoClass.createStatesInstance();
        States aStates = null;
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testStates_intersectionOf_disjointYieldsEmpty() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                
        String[] expected = new String[0];
        
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            State state = PseudoClass.getState(expected[n]);
            assertTrue(intersection.containsState(state));
        }
    }
    
    @Test
    public void testStates_unionOf() {
        
        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };
        
        States aStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setA.length; n++) {
            aStates.addState(PseudoClass.getState(setA[n]));
        }
        
        String[] setB = new String[] {
            "four", "five"
        };
        States bStates = PseudoClass.createStatesInstance();
        for(int n=0; n<setB.length; n++) {
            bStates.addState(PseudoClass.getState(setB[n]));
        }
                
        String[] expected = new String[] {
            "zero", "one", "two", "three",
            "four", "five"            
        };
        
        States union = States.unionOf(aStates, bStates);
        List<State> states = union.getStates();
        
        assertEquals(expected.length, states.size(), 0.000001);
        
        for (int n=0; n<expected.length; n++) {
            State state = PseudoClass.getState(expected[n]);
            assertTrue(union.containsState(state));
        }
    }
    
    @Test
    public void testStates_unionOf_withEmptyStates() {
        
        States bStates = PseudoClass.createStatesInstance();
        States aStates = PseudoClass.createStatesInstance();        
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        assertEquals(0, states.size(), 0.000001);
    }
    
    @Test
    public void testStates_unionOf_withNullArgs() {
        
        States bStates = PseudoClass.createStatesInstance();
        States aStates = null;
        States intersection = States.intersectionOf(aStates, bStates);
        List<State> states = intersection.getStates();
        assertEquals(0, states.size(), 0.000001);
        
    }
    
    @Test
    public void testStates_getStates() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        List<State> expected = new ArrayList<State>();
        States states = PseudoClass.createStatesInstance();
        for(int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
            expected.add(state);            
        }
        
        List<State> stateList = states.getStates();
        
        assertTrue(expected.containsAll(stateList));
    }

    @Test
    public void testStates_getPseudoClasses() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        
        List<String> expected = new ArrayList<String>();
        States states = PseudoClass.createStatesInstance();
        for(int n=0; n<pseudoClasses.length; n++) {
            State state = PseudoClass.getState(pseudoClasses[n]);
            states.addState(state);
            expected.add(pseudoClasses[n]);            
        }
        
        List<String> pseudoClassList = states.getPseudoClasses();
        
        assertTrue(expected.containsAll(pseudoClassList));
        
    }
        
    @Test
    public void testStates_toString() {
        
        String[] pseudoClasses = new String[] {
            "zero", "one", "two", "three"
        };
        
        States states = PseudoClass.createStatesInstance();
        for(int n=0; n<pseudoClasses.length; n++) {
            states.addState(PseudoClass.getState(pseudoClasses[n]));
        }
        
        List<State> stateList = states.getStates();
        String expected = stateList.toString();
        
        String result = states.toString();
        
        assertEquals(expected, result);
    }
    
    @Test
    public void testState_getState_throwsIndexOutOfBounds() {
    
        int n = 0;
        try {
            while (n < 1024) {
                PseudoClass.getState("pseudoClass-"+Integer.toString(n));
                n += 1;
            }
            fail(Integer.toString(n));
        } catch (IndexOutOfBoundsException exception) {
            assertEquals(1020, n, 0.000001);
        }
    }
}
