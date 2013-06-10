/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javafx.beans.Person;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.binding.Variable;
import javafx.collections.ObservableList;
import sun.util.logging.PlatformLogger.Level;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SelectBindingTest {
    private static final double EPSILON_DOUBLE = 1e-12;
    private static final float EPSILON_FLOAT = 1e-6f;

    public static class POJOPerson {
        private String name;

        public String getName() {
            return name;
        }

        public POJOPerson() {
        }

        public POJOPerson(String name) {
            this.name = name;
        }

    }

    public static class POJONext {
        private Object next;
        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        public Object getNext() {
            return next;
        }

        public void setNext(Object next) {
            Object old = this.next;
            this.next = next;
            pcs.firePropertyChange("next", old, next);
        }

        public void addPropertyChangeListener(String property, PropertyChangeListener pcl) {
            pcs.addPropertyChangeListener(property, pcl);
        }

        public void removePropertyChangeListener(String property, PropertyChangeListener pcl) {
            pcs.removePropertyChangeListener(property, pcl);
        }

    }

    private Variable a;
    private Variable b;
    private Variable c;
    private Variable d;
    private StringBinding select;
    private ObservableList<?> dependencies;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        a = new Variable("a");
        b = new Variable("b");
        c = new Variable("c");
        d = new Variable("d");
        a.setNext(b);
        b.setNext(c);
        select = Bindings.selectString(a.nextProperty(), "next", "name");
        dependencies = select.getDependencies();
    }

    @Test
    public void testObject() {
            final Person person1 = new Person();
            final Person person2 = new Person();
            c.setNext(person1);
            final ObjectBinding<Object> objectBinding = Bindings.select(a.nextProperty(), "next", "next");
            assertEquals(person1, objectBinding.get());
            c.setNext(person2);
            assertEquals(person2, objectBinding.get());
            b.setNext(null);
            assertEquals(null, objectBinding.get());
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJOObject() {
            POJONext pojoA = new POJONext();
            pojoA.setNext(b);
            final Person person1 = new Person();
            final Person person2 = new Person();
            c.setNext(person1);
            final ObjectBinding<Object> objectBinding = Bindings.select(pojoA, "next", "next", "next");
            assertEquals(person1, objectBinding.get());
            c.setNext(person2);
            assertEquals(person2, objectBinding.get());
    }

    @Test
    public void testPOJOObject_2() {
            POJONext pojoC = new POJONext();
            b.setNext(pojoC);
            final Person person1 = new Person();
            final Person person2 = new Person();
            pojoC.setNext(person1);
            final ObjectBinding<Object> objectBinding = Bindings.select(a.nextProperty(), "next", "next");
            assertEquals(person1, objectBinding.get());
            pojoC.setNext(person2);
            assertEquals(person2, objectBinding.get());
    }

    @Test
    public void testPOJOObject_3() {
            final POJOPerson person1 = new POJOPerson("P1");
            final POJOPerson person2 = new POJOPerson("P2");
            c.setNext(person1);
            final ObjectBinding<Object> objectBinding = Bindings.select(a.nextProperty(), "next", "next", "name");
            assertEquals("P1", objectBinding.get());
            c.setNext(person2);
            assertEquals("P2", objectBinding.get());
            b.setNext(null);
            assertEquals(null, objectBinding.get());
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJOBoolean() {

            POJONext pojoA = new POJONext();
            pojoA.setNext(b);

            final Person person = new Person();
            b.setNext(person);
            final BooleanBinding binding1 = Bindings.selectBoolean(pojoA, "next", "next", "retired");
            assertEquals(false, binding1.get());
            person.setRetired(true);
            assertEquals(true, binding1.get());
    }

    @Test
    public void testBoolean() {

            final Person person = new Person();
            b.setNext(person);
            final BooleanBinding binding1 = Bindings.selectBoolean(a.nextProperty(), "next", "retired");
            assertEquals(false, binding1.get());
            person.setRetired(true);
            assertEquals(true, binding1.get());
            b.setNext(null);
            assertEquals(false, binding1.get());
//            log.check(0, "WARNING", 1, "NullPointerException");

            person.setData(false);
            b.setNext(person);
            final BooleanBinding binding2 = Bindings.selectBoolean(a.nextProperty(), "next", "data");
            assertEquals(false, binding2.get());
            person.setData(true);
            assertEquals(true, binding2.get());
            person.setData(null);
            assertEquals(false, binding2.get());
//            log.check(0, "INFO", 1, "NullPointerException");
            b.setNext(null);
            assertEquals(false, binding2.get());
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJODouble() {
            POJONext pojoA = new POJONext();
            pojoA.setNext(b);


            final Person person = new Person();
            person.setSomething(-Math.E);
            b.setNext(person);
            final DoubleBinding binding1 = Bindings.selectDouble(pojoA, "next", "next", "something");
            assertEquals(-Math.E, binding1.get(), EPSILON_DOUBLE);
            person.setSomething(Math.PI);
            assertEquals(Math.PI, binding1.get(), EPSILON_DOUBLE);
    }

    @Test
    public void testDouble() {

            final Person person = new Person();
            person.setSomething(-Math.E);
            b.setNext(person);
            final DoubleBinding binding1 = Bindings.selectDouble(a.nextProperty(), "next", "something");
            assertEquals(-Math.E, binding1.get(), EPSILON_DOUBLE);
            person.setSomething(Math.PI);
            assertEquals(Math.PI, binding1.get(), EPSILON_DOUBLE);
            b.setNext(null);
            assertEquals(0.0, binding1.get(), EPSILON_DOUBLE);
//            log.check(0, "WARNING", 1, "NullPointerException");

            person.setData(-Math.E);
            b.setNext(person);
            final DoubleBinding binding2 = Bindings.selectDouble(a.nextProperty(), "next", "data");
            assertEquals(-Math.E, binding2.get(), EPSILON_DOUBLE);
            person.setData(Math.PI);
            assertEquals(Math.PI, binding2.get(), EPSILON_DOUBLE);
            person.setData(null);
            assertEquals(0.0, binding2.get(), EPSILON_DOUBLE);
//            log.check(0, "INFO", 1, "NullPointerException");
            b.setNext(null);
            assertEquals(0.0, binding2.get(), EPSILON_DOUBLE);
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJOFloat() {
            POJONext pojoA = new POJONext();
            pojoA.setNext(b);

            final Person person = new Person();
            person.setMiles((float)-Math.E);
            b.setNext(person);
            final FloatBinding binding1 = Bindings.selectFloat(pojoA, "next", "next", "miles");
            assertEquals((float)-Math.E, binding1.get(), EPSILON_FLOAT);
            person.setMiles((float)Math.PI);
            assertEquals((float)Math.PI, binding1.get(), EPSILON_FLOAT);
    }

    @Test
    public void testFloat() {

            final Person person = new Person();
            person.setMiles((float)-Math.E);
            b.setNext(person);
            final FloatBinding binding1 = Bindings.selectFloat(a.nextProperty(), "next", "miles");
            assertEquals((float)-Math.E, binding1.get(), EPSILON_FLOAT);
            person.setMiles((float)Math.PI);
            assertEquals((float)Math.PI, binding1.get(), EPSILON_FLOAT);
            b.setNext(null);
            assertEquals(0.0f, binding1.get(), EPSILON_FLOAT);
//            log.check(0, "WARNING", 1, "NullPointerException");

            person.setData((float)-Math.E);
            b.setNext(person);
            final FloatBinding binding2 = Bindings.selectFloat(a.nextProperty(), "next", "data");
            assertEquals((float)-Math.E, binding2.get(), EPSILON_FLOAT);
            person.setData((float)Math.PI);
            assertEquals((float)Math.PI, binding2.get(), EPSILON_FLOAT);
            person.setData(null);
            assertEquals(0.0f, binding2.get(), EPSILON_FLOAT);
//            log.check(0, "INFO", 1, "NullPointerException");
            b.setNext(null);
            assertEquals(0.0f, binding2.get(), EPSILON_FLOAT);
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJOInteger() {

            POJONext pojoA = new POJONext();
            pojoA.setNext(b);

            final Person person = new Person();
            person.setAge(42);
            b.setNext(person);
            final IntegerBinding binding1 = Bindings.selectInteger(pojoA, "next", "next", "age");
            assertEquals(42, binding1.get());
            person.setAge(-18);
            assertEquals(-18, binding1.get());
    }
    @Test
    public void testInteger() {

            final Person person = new Person();
            person.setAge(42);
            b.setNext(person);
            final IntegerBinding binding1 = Bindings.selectInteger(a.nextProperty(), "next", "age");
            assertEquals(42, binding1.get());
            person.setAge(-18);
            assertEquals(-18, binding1.get());
            b.setNext(null);
            assertEquals(0, binding1.get());
//            log.check(0, "WARNING", 1, "NullPointerException");

            person.setData(42);
            b.setNext(person);
            final IntegerBinding binding2 = Bindings.selectInteger(a.nextProperty(), "next", "data");
            assertEquals(42, binding2.get());
            person.setData(-18);
            assertEquals(-18, binding2.get());
            person.setData(null);
            assertEquals(0, binding2.get());
//            log.check(0, "INFO", 1, "NullPointerException");
            b.setNext(null);
            assertEquals(0, binding2.get());
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test
    public void testPOJOLong() {
            POJONext pojoA = new POJONext();
            pojoA.setNext(b);
            final Person person = new Person();
            person.setIncome(1234567890987654321L);
            b.setNext(person);
            final LongBinding binding1 = Bindings.selectLong(pojoA, "next", "next", "income");
            assertEquals(1234567890987654321L, binding1.get());
            person.setIncome(-987654321234567890L);
            assertEquals(-987654321234567890L, binding1.get());
    }

    @Test
    public void testLong() {
            final Person person = new Person();
            person.setIncome(1234567890987654321L);
            b.setNext(person);
            final LongBinding binding1 = Bindings.selectLong(a.nextProperty(), "next", "income");
            assertEquals(1234567890987654321L, binding1.get());
            person.setIncome(-987654321234567890L);
            assertEquals(-987654321234567890L, binding1.get());
            b.setNext(null);
            assertEquals(0L, binding1.get());
//            log.check(0, "WARNING", 1, "NullPointerException");

            person.setData(1234567890987654321L);
            b.setNext(person);
            final LongBinding binding2 = Bindings.selectLong(a.nextProperty(), "next", "data");
            assertEquals(1234567890987654321L, binding2.get());
            person.setData(-987654321234567890L);
            assertEquals(-987654321234567890L, binding2.get());
            person.setData(null);
            assertEquals(0L, binding2.get());
//            log.check(0, "INFO", 1, "NullPointerException");
            b.setNext(null);
            assertEquals(0L, binding2.get());
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @Test(expected=NullPointerException.class)
    public void createWithRootNull(){
        select = Bindings.selectString(null, "next", "name");
    }

    @Test
    public void createWithNoSteps(){

            select = Bindings.selectString(a.nameProperty());
            assertEquals("a", select.get());
            a.setName("b");
            assertEquals("b", select.get());
            a.setName(null);
            assertNull(select.get());
//            log.isEmpty();
    }

    @Test(expected=NullPointerException.class)
    public void createWithOneStepIsNull(){
        select = Bindings.selectString(a.nextProperty(), null, "name");
    }

    @Test
    public void testNullIsReturnedFromAChainWithAPropertyThatIsNotOnTheAvailableObject() {

            select = Bindings.selectString(a.nextProperty(), "dummy", "name");
            assertNull(select.get());
//            log.check(0, "WARNING", 1, "IllegalStateException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAllMembersHaveListeners() {
        // As yet, there should still be no listeners registered
        assertEquals(1, a.numChangedListenersForNext);
        assertEquals(0, a.numChangedListenersForName);
        assertEquals(0, b.numChangedListenersForNext);
        assertEquals(0, b.numChangedListenersForName);
        assertEquals(0, c.numChangedListenersForNext);
        assertEquals(0, c.numChangedListenersForName);
        assertEquals(Arrays.asList(a.nextProperty()), dependencies);
        // Read the value of the select. It should be == "c"
        assertEquals("c", select.get());
        // Now there should be changed listeners for a.value and
        // b.value and c.name
        assertEquals(1, a.numChangedListenersForNext);
        assertEquals(0, a.numChangedListenersForName);
        assertEquals(1, b.numChangedListenersForNext);
        assertEquals(0, b.numChangedListenersForName);
        assertEquals(0, c.numChangedListenersForNext);
        assertEquals(1, c.numChangedListenersForName);
        assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAValidChangeIsBrokenBySettingTheRootToNullThenTheValueIsNull() {

            assertEquals("c", select.get());
            a.setNext(null);
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertNull(select.get());
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAnIncompleteChainIsMadeCompleteThatTheValueIsComputedCorrectly() {

            a.setNext(null);
            select.get(); // force it to be validated, for fun
//            log.reset();
            a.setNext(b);
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertEquals("c", select.get());
            assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
//            log.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAValidChangeIsBrokenBySettingTheMiddleToNullThenTheValueIsNull() {

            assertEquals("c", select.get());
            b.setNext(null);
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertNull(select.get());
            assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty()), dependencies);
//            log.check(0, "WARNING", 1, "NullPointerException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAnIncompleteChainIsMadeCompleteInTheMiddleThatTheValueIsComputedCorrectly() {

            b.setNext(null);
            select.get();
//            log.reset();
            b.setNext(c);
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertEquals("c", select.get());
            assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
//            log.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAValidChangeIsBrokenBySettingTheLastLinkToNullThenTheValueIsNull() {

            assertEquals("c", select.get());
            c.setName("d");
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertEquals("d", select.get());
            assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
//            log.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWhenAnIncompleteChainIsMadeCompleteAtTheEndThatTheValueIsComputedCorrectly() {

            c.setName("d");
            select.get();
            c.setName("c");
            assertEquals(Arrays.asList(a.nextProperty()), dependencies);
            assertEquals("c", select.get());
            assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
//            log.isEmpty();
    }

    @SuppressWarnings("unchecked")
	@Test
    public void testSettingTheRootValueToNullInAChainShouldUnhookAllListeners() {
        select.get();
        a.setNext(null);

        // All of the listeners should have been uninstalled
        assertEquals(0, a.numChangedListenersForName);
        assertEquals(1, a.numChangedListenersForNext);

        assertEquals(0, b.numChangedListenersForName);
        assertEquals(0, b.numChangedListenersForNext);

        assertEquals(0, c.numChangedListenersForName);
        assertEquals(0, c.numChangedListenersForNext);

        assertEquals(0, d.numChangedListenersForName);
        assertEquals(0, d.numChangedListenersForNext);

        assertEquals(Arrays.asList(a.nextProperty()), dependencies);
    }

    /**
     * This test performs 10,000 random operations on the chain of a.b.c
     * (setting different values for each step, sometimes doing multiple
     * assignments between get() calls, random invalidate() calls). After
     * each random iteration, we check to see if the listeners installed on
     * the a, b, c objects are still correct. The goal is to catch any freak
     * situations where we might have installed two listeners on the same
     * property, or failed to remove a listener for a property under some
     * circumstance.
     *
     * Do note that the only listeners that are installed in this method
     * are done by the select binding.
     */
    @SuppressWarnings("unchecked")
	@Test
    public void stressTestRandomOperationsResultInCorrectListenersInstalled() {

            final Level logLevel = Logging.getLogger().level();
            Logging.getLogger().setLevel(Level.SEVERE);
            List<String> steps = new ArrayList<String>();

            Random rand = new Random(System.currentTimeMillis());
            for (int i=0; i<10000; i++) {
                switch(rand.nextInt(20)) {
                    case 0:
                        a.setNext(null);
                        steps.add("Assign a.value to null");
                        break;
                    case 1:
                        a.setNext(b);
                        steps.add("Assign a.value to b");
                        break;
                    case 2:
                        b.setNext(null);
                        steps.add("Assign b.value to null");
                        break;
                    case 3:
                        b.setNext(c);
                        steps.add("Assign b.value to c");
                        break;
                    case 4:
                        c.setNext(null);
                        steps.add("Assign c.value to null");
                        break;
                    case 5:
                        c.setNext(d);
                        steps.add("Assign c.value to d");
                        break;
                    case 6:
                        c.setName(null);
                        steps.add("Assign c.name to null");
                        break;
                    case 7:
                        c.setName("c");
                        steps.add("Assign c.name to 'c'");
                        break;
                    default:
                        select.get();
                        steps.add("Call select.get()");
                }

                // Now validate that the listeners are as we expected
                int expected = 1;
                int depsCount = expected;
                assertEquals(0, a.numChangedListenersForName);
                if (expected != a.numChangedListenersForNext) printSteps(i, steps);
                assertEquals(expected, a.numChangedListenersForNext);


                expected = select.isValid() && a.getNext() == b ? 1 : 0;
                depsCount += expected;
                assertEquals(0, b.numChangedListenersForName);
                if (expected != b.numChangedListenersForNext) printSteps(i, steps);
                assertEquals(expected, b.numChangedListenersForNext);

                expected = select.isValid() && a.getNext() == b && b.getNext() == c ? 1 : 0;
                depsCount += expected;
                assertEquals(0, c.numChangedListenersForNext);
                if (expected != c.numChangedListenersForName) printSteps(i, steps);
                assertEquals(expected, c.numChangedListenersForName);

                assertEquals(0, d.numChangedListenersForName);
                assertEquals(0, d.numChangedListenersForNext);

                switch (depsCount) {
                    case 0:
                    case 1:
                        assertEquals(Arrays.asList(a.nextProperty()), dependencies);
                        break;
                    case 2:
                        assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty()), dependencies);
                        break;
                    case 3:
                        assertEquals(Arrays.asList(a.nextProperty(), b.nextProperty(), c.nameProperty()), dependencies);
                        break;
                    default:
                        fail("Should not reach here");
                }
            }
            Logging.getLogger().setLevel(logLevel);
    }

    private void printSteps(int iteration, List<String> steps) {
        System.err.println("Failed on iteration " + iteration + " for the following observableArrayList of changes");
        for (String s : steps) {
            System.err.println("\t" + s);
        }
    }
}
