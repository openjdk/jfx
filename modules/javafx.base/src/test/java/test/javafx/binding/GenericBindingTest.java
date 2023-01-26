/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.binding;

import test.javafx.beans.InvalidationListenerMock;
import javafx.beans.Observable;
import javafx.beans.binding.*;
import test.javafx.beans.value.ChangeListenerMock;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class GenericBindingTest<T> {

    private static final Object UNDEFINED = null;

    private final ObservableStub dependency1 = new ObservableStub();
    private final ObservableStub dependency2 = new ObservableStub();
    private final T value1;
    private final T value2;
    private final Constructor<BindingMock<T>> bindingMockClassConstructor;

    private BindingMock<T> binding0;
    private BindingMock<T> binding1;
    private BindingMock<T> binding2;
    private InvalidationListenerMock invalidationListener;
    private ChangeListenerMock<Object> changeListener;

    public GenericBindingTest(
            T value1, T value2,
            Class<BindingMock<T>> bindingMockClass) throws Exception {
        this.value1 = value1;
        this.value2 = value2;
        this.bindingMockClassConstructor = bindingMockClass.getConstructor(Observable[].class);
    }

    @Before
    public void setUp() throws Exception {
        // Recreate bindings as they may have been altered by one of the tests
        binding0 = bindingMockClassConstructor.newInstance((Object)new Observable[] {});
        binding1 = bindingMockClassConstructor.newInstance((Object)new Observable[] {dependency1});
        binding2 = bindingMockClassConstructor.newInstance((Object)new Observable[] {dependency1, dependency2});

        invalidationListener = new InvalidationListenerMock();
        changeListener = new ChangeListenerMock<>(UNDEFINED);
        binding0.setValue(value2);
        binding1.setValue(value2);
        binding2.setValue(value2);
    }

    @After
    public void tearDown() {
        binding0.removeListener(invalidationListener);
        binding0.removeListener(changeListener);
        binding1.removeListener(invalidationListener);
        binding1.removeListener(changeListener);
        binding2.removeListener(invalidationListener);
        binding2.removeListener(changeListener);
    }

    @Test
    public void testNoDependencyLazy() {
        binding0.getValue();
        binding0.addListener(invalidationListener);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(true, binding0.isValid());

        // calling getValue()
        binding0.reset();
        binding0.getValue();
        assertEquals(0, binding0.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding0.isValid());
    }

    @Test
    public void testNoDependencyEager() {
        binding0.getValue();
        binding0.addListener(changeListener);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(true, binding0.isValid());

        // calling getValue()
        binding0.reset();
        binding0.getValue();
        assertEquals(0, binding0.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding0.isValid());
    }

    @Test
    public void testSingleDependencyLazy() {
        binding1.getValue();
        binding1.addListener(invalidationListener);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(true, binding1.isValid());

        // fire single change event
        binding1.reset();
        invalidationListener.reset();
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(0, binding1.getComputeValueCounter());
        invalidationListener.check(binding1, 1);
        assertEquals(false, binding1.isValid());

        binding1.getValue();
        assertEquals(1, binding1.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding1.isValid());

        // fire single change event with same value
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(0, binding1.getComputeValueCounter());
        invalidationListener.check(binding1, 1);
        assertEquals(false, binding1.isValid());

        binding1.getValue();
        assertEquals(1, binding1.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding1.isValid());

        // fire two change events with different values
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(0, binding1.getComputeValueCounter());
        invalidationListener.check(binding1, 1);
        assertEquals(false, binding1.isValid());

        binding1.getValue();
        assertEquals(1, binding1.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding1.isValid());

        // fire two change events with same values
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        assertEquals(0, binding1.getComputeValueCounter());
        invalidationListener.check(binding1, 1);
        assertEquals(false, binding1.isValid());

        binding1.getValue();
        assertEquals(1, binding1.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding1.isValid());
    }

    @Test
    public void testSingleDependencyEager() {
        binding1.getValue();
        binding1.addListener(changeListener);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(true, binding1.isValid());

        // fire single change event
        binding1.reset();
        changeListener.reset();
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(1, binding1.getComputeValueCounter());
        changeListener.check(binding1, value2, value1, 1);
        assertEquals(true, binding1.isValid());

        binding1.getValue();
        assertEquals(0, binding1.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding1.isValid());

        // fire single change event with same value
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(1, binding1.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding1.isValid());

        binding1.getValue();
        assertEquals(0, binding1.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding1.isValid());

        // fire two change events
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        binding1.setValue(value1);
        dependency1.fireValueChangedEvent();
        assertEquals(2, binding1.getComputeValueCounter());
        changeListener.check(binding1, value2, value1, 2);
        assertEquals(true, binding1.isValid());

        binding1.getValue();
        assertEquals(0, binding1.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding1.isValid());

        // fire two change events with same value
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        binding1.setValue(value2);
        dependency1.fireValueChangedEvent();
        assertEquals(2, binding1.getComputeValueCounter());
        changeListener.check(binding1, value1, value2, 1);
        assertEquals(true, binding1.isValid());

        binding1.getValue();
        assertEquals(0, binding1.getComputeValueCounter());
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
        assertEquals(true, binding1.isValid());
    }

    @Test
    public void testTwoDependencies() {
        binding2.getValue();
        binding2.addListener(invalidationListener);
        System.gc(); // making sure we did not not overdo weak references
        assertEquals(true, binding2.isValid());

        // fire single change event on first dependency
        binding2.reset();
        invalidationListener.reset();
        dependency1.fireValueChangedEvent();
        assertEquals(0, binding2.getComputeValueCounter());
        invalidationListener.check(binding2, 1);
        assertEquals(false, binding2.isValid());

        binding2.getValue();
        assertEquals(1, binding2.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding2.isValid());

        // fire single change event on second dependency
        binding2.reset();
        dependency2.fireValueChangedEvent();
        assertEquals(0, binding2.getComputeValueCounter());
        invalidationListener.check(binding2, 1);
        assertEquals(false, binding2.isValid());

        binding2.getValue();
        assertEquals(1, binding2.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding2.isValid());

        // fire change events on each dependency
        binding2.reset();
        dependency1.fireValueChangedEvent();
        dependency2.fireValueChangedEvent();
        assertEquals(0, binding2.getComputeValueCounter());
        invalidationListener.check(binding2, 1);
        assertEquals(false, binding2.isValid());

        binding2.getValue();
        assertEquals(1, binding2.getComputeValueCounter());
        invalidationListener.check(null, 0);
        assertEquals(true, binding2.isValid());
    }

    @Test
    public void testUnbindDependencies() {
        // Start by making binding valid:
        binding2.getValue();
        assertTrue(binding2.isValid());

        // Changing dependency1 should cause binding to become invalid:
        dependency1.fireValueChangedEvent();
        assertFalse(binding2.isValid());

        // Make valid again:
        binding2.getValue();
        assertTrue(binding2.isValid());

        // Changing dependency2 should cause binding to become invalid:
        dependency2.fireValueChangedEvent();
        assertFalse(binding2.isValid());

        // Make valid again:
        binding2.getValue();
        assertTrue(binding2.isValid());

        // Remove dependency1:
        binding2.publicUnbind(dependency1);

        // Check that binding2 is no longer affected by changes in dependency1:
        dependency1.fireValueChangedEvent();
        assertTrue(binding2.isValid());

        // But still affected by changes in dependency2:
        dependency2.fireValueChangedEvent();
        assertFalse(binding2.isValid());

        // Make valid again:
        binding2.getValue();
        assertTrue(binding2.isValid());

        // Remove dependency2:
        binding2.publicUnbind(dependency2);

        // Check that binding2 is no longer affected by changes in dependency2:
        dependency2.fireValueChangedEvent();
        assertTrue(binding2.isValid());   // Fixed by 8243115
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {
                Float.MIN_VALUE, Float.MAX_VALUE,
                FloatBindingImpl.class
            },
            {
                Double.MIN_VALUE, Double.MAX_VALUE,
                DoubleBindingImpl.class
            },
            {
                Long.MIN_VALUE, Long.MAX_VALUE,
                LongBindingImpl.class
            },
            {
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                IntegerBindingImpl.class
            },
            {
                true, false,
                BooleanBindingImpl.class
            },
            {
                "Hello World", "Goodbye",
                StringBindingImpl.class
            },
            {
                new Object(), new Object(),
                ObjectBindingImpl.class
            },
            {
                FXCollections.observableArrayList(), FXCollections.observableArrayList(),
                ListBindingImpl.class
            }
        });
    }

    public static class ObservableStub extends ObservableValueBase<Object> {
        @Override public void fireValueChangedEvent() {super.fireValueChangedEvent();}

        @Override
        public Object getValue() {
            return null;
        }
    }

    public static interface BindingMock<T> extends Binding<T> {
        int getComputeValueCounter();
        void reset();
        void setValue(T value);
        void publicUnbind(Observable... observables);
    }

    private static class DoubleBindingImpl extends DoubleBinding implements BindingMock<Number> {

        private int computeValueCounter = 0;
        private double value;

        @Override
        public void setValue(Number value) {this.value = value.doubleValue();}

        public DoubleBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public double computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class FloatBindingImpl extends FloatBinding implements BindingMock<Number> {

        private int computeValueCounter = 0;
        private float value;

        @Override
        public void setValue(Number value) {this.value = value.floatValue();}

        public FloatBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public float computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class LongBindingImpl extends LongBinding implements BindingMock<Number> {

        private int computeValueCounter = 0;
        private long value;

        @Override
        public void setValue(Number value) {this.value = value.longValue();}

        public LongBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public long computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class IntegerBindingImpl extends IntegerBinding implements BindingMock<Number> {

        private int computeValueCounter = 0;
        private int value;

        @Override
        public void setValue(Number value) {this.value = value.intValue();}

        public IntegerBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public int computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class BooleanBindingImpl extends BooleanBinding implements BindingMock<Boolean> {

        private int computeValueCounter = 0;
        private boolean value;

        @Override
        public void setValue(Boolean value) {this.value = value;}

        public BooleanBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public boolean computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class ObjectBindingImpl extends ObjectBinding<Object> implements BindingMock<Object> {

        private int computeValueCounter = 0;
        private Object value;

        @Override
        public void setValue(Object value) {this.value = value;}

        public ObjectBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public Object computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class StringBindingImpl extends StringBinding implements BindingMock<String> {

        private int computeValueCounter = 0;
        private String value;

        @Override
        public void setValue(String value) {this.value = value;}

        public StringBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public String computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }

    private static class ListBindingImpl extends ListBinding<Object> implements BindingMock<ObservableList<Object>> {

        private int computeValueCounter = 0;
        private ObservableList<Object> value;

        @Override
        public void setValue(ObservableList<Object> value) {this.value = value;}

        public ListBindingImpl(Observable... dep) {
            super.bind(dep);
        }

        @Override public int getComputeValueCounter() {
            final int result = computeValueCounter;
            reset();
            return result;
        }

        @Override public void reset() {computeValueCounter = 0;}

        @Override
        public ObservableList<Object> computeValue() {
            computeValueCounter++;
            return value;
        }

        @Override
        public ObservableList<?> getDependencies() {
            fail("Should not reach here");
            return null;
        }

        public void publicUnbind(Observable... observables) {
            super.unbind(observables);
        }
    }


}
