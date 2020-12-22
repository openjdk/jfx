/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css;

import com.sun.javafx.css.BitSetShim;
import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.PseudoClassStateShim;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        PseudoClassStateShim.pseudoClassMap.clear();
        PseudoClassStateShim.pseudoClasses.clear();
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
        BitSetShim.add(states, state);

        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, state);
        }

        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, state);
        }

        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            assertTrue( BitSetShim.contains(states, state));
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
            BitSetShim.add(states, state);
        }

        for (int n=0; n<notExpected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(notExpected[n]);
            assertTrue( BitSetShim.contains(states, state) == false);
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
            BitSetShim.add(states, state);
        }

        assertTrue( BitSetShim.contains(states, null) == false);

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
            BitSetShim.add(states, state);
        }

        BitSetShim.remove(states, PseudoClass.getPseudoClass("three"));

        assertTrue( BitSetShim.contains(states, PseudoClass.getPseudoClass("three"))==false);

        // should still have the others.
        for (int n=1; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue( BitSetShim.contains(states, state));
        }

    }

    @Test
    public void testPseudoClassState_containsAll() {

        String[] setA = new String[] {
            "zero", "one"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "zero", "one", "two", "three"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertTrue(BitSetShim.containsAll(bStates, aStates));
    }

    @Test
    public void testPseudoClassState_containsAll_negativeTest() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "zero", "one"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertTrue(BitSetShim.containsAll(bStates, aStates) == false);
    }

    @Test
    public void testPseudoClassState_containsAll_whenSetsAreEqual() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "zero", "one", "two", "three"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertTrue(BitSetShim.containsAll(bStates, aStates));
    }

    @Test
    public void testPseudoClassState_containsAll_whenSetsDisjoint() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "four", "five", "six", "seven"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertTrue(BitSetShim.containsAll(bStates, aStates) == false);
    }

    @Test
    public void testPseudoClassState_containsAll_whenOneSetEmpty() {

        String[] setA = new String[] {
                "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        PseudoClassState bStates = new PseudoClassState();

        assertTrue(BitSetShim.containsAll(bStates, aStates) == false);
        assertTrue(BitSetShim.containsAll(aStates, bStates));

    }

    @Test
    public void testPseudoClassState_containsAll_whenBothSetsEmpty() {

        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = new PseudoClassState();

        assertTrue(BitSetShim.containsAll(bStates, aStates));
        assertTrue(BitSetShim.containsAll(aStates, bStates));

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
            BitSetShim.add(states, state);
        }

        assertEquals(expected, BitSetShim.size(states));
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
            BitSetShim.add(states, state);
        }

        BitSetShim.remove(states, PseudoClass.getPseudoClass("three"));

        assertEquals(expected, BitSetShim.size(states));
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
            BitSetShim.add(states, state);
        }

        for (int n=0; n<pseudoClasses.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(pseudoClasses[n]);
            BitSetShim.remove(states, state);
        }

        assertEquals(expected, BitSetShim.size(states));
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
            BitSetShim.add(states, state);
        }

        PseudoClass state = PseudoClass.getPseudoClass("three");
        BitSetShim.add(states, state);

        assertEquals(expected, BitSetShim.size(states));
    }

    @Test
    public void testPseudoClassState_isEmpty() {
        PseudoClassState states = new PseudoClassState();
        assertTrue(BitSetShim.isEmpty(states));
    }

    @Test
    public void testPseudoClassState_isEmpty_negativeTest() {
        PseudoClassState states = new PseudoClassState();
        BitSetShim.add(states, PseudoClass.getPseudoClass("pseudo-class"));
        assertTrue(BitSetShim.isEmpty(states) == false);
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

        BitSetShim.add(states, state);
        assertTrue(BitSetShim.isEmpty(states) == false);
    }

    @Test
    public void testPseudoClassState_equals() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "zero", "one", "two", "three"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertTrue(BitSetShim.equals(aStates, bStates));
    }

    @Test
    public void testPseudoClassState_equals_negativeTest() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "zero", "one", "two", "four"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        assertFalse(BitSetShim.equals(aStates, bStates));
    }

    @Test
    public void testPseudoClassState_retainAll() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "four", "five", "one", "two", "three"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        String[] expected = new String[] {
            "one", "two", "three"
        };

        BitSetShim.retainAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
        while (iter.hasNext()) {
            states.add(iter.next());
        }

        assertEquals(expected.length, states.size(), 0.000001);

        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue( BitSetShim.contains(aStates, state));
        }
    }

    @Test
    public void testPseudoClassState_retainAll_withEmptyStates() {

        PseudoClassState bStates = new PseudoClassState();
        PseudoClassState aStates = new PseudoClassState();
        BitSetShim.retainAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
    }

    @Test
    public void testPseudoClassState_retainAll_withNullArg() {

        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = null;
        BitSetShim.retainAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
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
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        String[] expected = new String[0];

        BitSetShim.retainAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
        while (iter.hasNext()) {
            states.add(iter.next());
        }

        assertEquals(expected.length, states.size(), 0.000001);

        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue( BitSetShim.contains(aStates, state));
        }
    }

    @Test
    public void testPseudoClassState_addAll() {

        String[] setA = new String[] {
            "zero", "one", "two", "three"
        };

        PseudoClassState aStates = new PseudoClassState();
        for(int n=0; n<setA.length; n++) {
            BitSetShim.add(aStates, PseudoClass.getPseudoClass(setA[n]));
        }

        String[] setB = new String[] {
            "four", "five"
        };
        PseudoClassState bStates = new PseudoClassState();
        for(int n=0; n<setB.length; n++) {
            BitSetShim.add(bStates, PseudoClass.getPseudoClass(setB[n]));
        }

        String[] expected = new String[] {
            "zero", "one", "two", "three",
            "four", "five"
        };

        BitSetShim.addAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
        while (iter.hasNext()) {
            states.add(iter.next());
        }

        assertEquals(expected.length, states.size(), 0.000001);

        for (int n=0; n<expected.length; n++) {
            PseudoClass state = PseudoClass.getPseudoClass(expected[n]);
            assertTrue( BitSetShim.contains(aStates, state));
        }
    }

    @Test
    public void testPseudoClassState_addAll_withEmptyStates() {

        PseudoClassState bStates = new PseudoClassState();
        PseudoClassState aStates = new PseudoClassState();
        BitSetShim.addAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
        while (iter.hasNext()) {
            states.add(iter.next());
        }
        assertEquals(0, states.size(), 0.000001);
    }

    @Test
    public void testPseudoClassState_addAll_withNullArgs() {

        PseudoClassState aStates = new PseudoClassState();
        PseudoClassState bStates = null;
        BitSetShim.addAll(aStates, bStates);
        List<PseudoClass> states = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(aStates);
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
            BitSetShim.add(states, state);
            expected.add(state);
        }

        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, PseudoClass.getPseudoClass(pseudoClasses[n]));
        }

        List<PseudoClass> stateList = new ArrayList<PseudoClass>();
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, pseudoClasses[n]);
        };

        int iterations = 0;
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, pseudoClasses[n]);
        };

        int iterations = 0;
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
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
            BitSetShim.add(states, pseudoClasses[n]);
        };

        int iterations = 0;
        int nPseudoClasses = pseudoClasses.length;
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
        while(iter.hasNext()) {

            ++iterations;

            if ((iterations % 2) == 0) {
                iter.remove();
                --nPseudoClasses;
                assertFalse( BitSetShim.contains(states, pseudoClasses[iterations-1]));
            }
        }

        assertTrue (nPseudoClasses+"!="+BitSetShim.size(states), nPseudoClasses == BitSetShim.size(states));

    }

    @Test public void testPseudoClassState_iterator_remove_withLargeNumberOfPseudoClasses() {

        PseudoClass[] pseudoClasses = new PseudoClass[Long.SIZE*3];
        PseudoClassState states = new PseudoClassState();

        for (int n=0; n<pseudoClasses.length; n++) {
            pseudoClasses[n] = PseudoClass.getPseudoClass(Integer.toString(n));
            BitSetShim.add(states, pseudoClasses[n]);
        };

        int iterations = 0;
        int nPseudoClasses = pseudoClasses.length;
        Iterator<PseudoClass> iter = BitSetShim.iterator(states);
        while(iter.hasNext()) {

            ++iterations;

            if ((iterations % 2) == 0) {
                iter.remove();
                --nPseudoClasses;
                assertFalse( BitSetShim.contains(states, pseudoClasses[iterations-1]));
            }
        }

        assertTrue (nPseudoClasses+"!="+BitSetShim.size(states), nPseudoClasses == BitSetShim.size(states));

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
        pseudoClasses.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasAdded()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementAdded();
                assertSame(expectedObservations[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
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

        pseudoClasses.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasRemoved()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementRemoved();
                assertSame(expectedObservations[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
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

        pseudoClasses.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasRemoved()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementRemoved();
                assertSame(expectedObservations[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
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
        pseudoClasses.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasAdded()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementAdded();
                assertSame(expectedObservations[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
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

        ObservableSet<PseudoClass> primary = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            primary.add(pseudoClasses[n]);
        };

        primary.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasRemoved()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementRemoved();
                assertSame(pseudoClassesToRemove[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
            }
        });

        primary.removeAll(other);

        assertEquals(nObservationsExpected, nObservations);
        assertEquals(pseudoClasses.length-pseudoClassesToRemove.length, primary.size());

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

        ObservableSet<PseudoClass> primary = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            primary.add(pseudoClasses[n]);
        };

        primary.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasRemoved()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementRemoved();
                assertSame(removedPseudoClasses[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
            }
        });

        primary.retainAll(other);

        assertEquals(nObservationsExpected, nObservations);
        assertEquals(pseudoClassesToRetain.length, primary.size());

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

        ObservableSet<PseudoClass> primary = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            primary.add(pseudoClasses[n]);
        };

        primary.addListener((SetChangeListener.Change<? extends PseudoClass> change) -> {
            if (change.wasRemoved()) {
                assert (nObservations < nObservationsExpected);
                PseudoClass observed = change.getElementRemoved();
                assertSame(pseudoClasses[nObservations], observed);
                nObservations += 1;
            } else {
                fail();
            }
        });

        primary.clear();

        assertEquals(nObservationsExpected, nObservations);
        assertTrue(primary.isEmpty());

    }

    @Test public void testObservablePseudoClass_listener_getSet_unmodifiable() {

        final ObservableSet<PseudoClass> primary = new PseudoClassState();

        primary.addListener(new SetChangeListener<PseudoClass>() {

            @Override
            public void onChanged(SetChangeListener.Change<? extends PseudoClass> change) {
                primary.removeListener(this);
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

        primary.add(PseudoClass.getPseudoClass("ONE"));

    }

    @Test public void testRetainAllOfEmptySetResultsInEmptySet() {

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        Set<PseudoClass> setA = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            setA.add(pseudoClasses[n]);
        };

        Set<PseudoClass> setB = new PseudoClassState();

        assertTrue(setA.retainAll(setB));

        assertArrayEquals(new long[0], BitSetShim.getBits((PseudoClassState)setA));
    }

    @Test public void testRemoveAllOfSameSetResultsInEmptySet() {

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        Set<PseudoClass> setA = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            setA.add(pseudoClasses[n]);
        };

        assertTrue(setA.removeAll(setA));

        assertArrayEquals(new long[0], BitSetShim.getBits((PseudoClassState)setA));

    }

    @Test public void testRemoveLastBitResultsInEmptySet() {

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        Set<PseudoClass> setA = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            setA.add(pseudoClasses[n]);
        };

        for (int n=0; n<pseudoClasses.length; n++) {
            assertTrue(setA.remove(pseudoClasses[n]));
        };

        assertArrayEquals(new long[0], BitSetShim.getBits((PseudoClassState)setA));

    }

    @Test public void testRemoveLastBitByIteratorResultsInEmptySet() {

        final PseudoClass[] pseudoClasses = new PseudoClass[] {
                PseudoClass.getPseudoClass("ONE"),
                PseudoClass.getPseudoClass("TWO"),
                PseudoClass.getPseudoClass("THREE"),
                PseudoClass.getPseudoClass("FOUR")

        };

        Set<PseudoClass> setA = new PseudoClassState();
        for (int n=0; n<pseudoClasses.length; n++) {
            setA.add(pseudoClasses[n]);
        };

        Iterator<PseudoClass> iterator = setA.iterator();
        while (iterator.hasNext()) {
            iterator.remove();
        }

        assertArrayEquals(new long[0], BitSetShim.getBits((PseudoClassState)setA));

    }

    @Test public void testAddEmptyToEmptyResultsInEmptySet() {

        Set<PseudoClass> setA = new PseudoClassState();
        Set<PseudoClass> setB = new PseudoClassState();
        assertFalse(setA.addAll(setB));
        assertArrayEquals(new long[0], BitSetShim.getBits((PseudoClassState)setA));

    }

}
