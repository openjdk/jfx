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
package com.sun.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BidirectionalBindingTest<T> {
	
    private static final float EPSILON_FLOAT = 1e-5f;
    private static final double EPSILON_DOUBLE = 1e-10;

    public static interface Functions<S> {
        PropertyMock<S> create();
    	void bind(PropertyMock<S> obj1, PropertyMock<S> obj2);
    	void unbind(PropertyMock<S> obj1, PropertyMock<S> obj2);
    	BidirectionalBinding createBindingDirectly(PropertyMock<S> op1, PropertyMock<S> op2);
    	void check(S expected, S actual);
    }

    private final Functions<T> func;
    private final T[] v;

    private PropertyMock<T> op1;
    private PropertyMock<T> op2;
    private PropertyMock<T> op3;
    
    public BidirectionalBindingTest(Functions<T> func, T[] v) {
        this.op1 = func.create();
        this.op2 = func.create();
        this.op3 = func.create();
        this.func = func;
        this.v = v;
    }

    @Before
    public void setUp() {
    	op1.setValue(v[0]);
    	op2.setValue(v[1]);
    }
    
    @Test
    public void testBind() {
    	func.bind(op1, op2);
		System.gc(); // making sure we did not not overdo weak references
		func.check(v[1], op1.getValue());
		func.check(v[1], op2.getValue());
		
		op1.setValue(v[2]);
		func.check(v[2], op1.getValue());
		func.check(v[2], op2.getValue());
		
		op2.setValue(v[3]);
		func.check(v[3], op1.getValue());
		func.check(v[3], op2.getValue());
    }

	@Test
	public void testUnbind() {
		// unbind non-existing binding => no-op
		func.unbind(op1, op2);
		
		// unbind properties of different beans
    	func.bind(op1, op2);
		System.gc(); // making sure we did not not overdo weak references
		func.check(v[1], op1.getValue());
		func.check(v[1], op2.getValue());
		
		func.unbind(op1, op2);
		System.gc();
		func.check(v[1], op1.getValue());
		func.check(v[1], op2.getValue());
		
		op1.setValue(v[2]);
		func.check(v[2], op1.getValue());
		func.check(v[1], op2.getValue());
		
		op2.setValue(v[3]);
		func.check(v[2], op1.getValue());
		func.check(v[3], op2.getValue());
	}
	
	@Test
	public void testChaining() {
		op3.setValue(v[2]);
		func.bind(op1, op2);
		func.bind(op2, op3);
		System.gc(); // making sure we did not not overdo weak references
		func.check(v[2], op1.getValue());
		func.check(v[2], op2.getValue());
		func.check(v[2], op3.getValue());
		
		op1.setValue(v[3]);
		func.check(v[3], op1.getValue());
		func.check(v[3], op2.getValue());
		func.check(v[3], op3.getValue());
		
		op2.setValue(v[0]);
		func.check(v[0], op1.getValue());
		func.check(v[0], op2.getValue());
		func.check(v[0], op3.getValue());
		
		op3.setValue(v[1]);
		func.check(v[1], op1.getValue());
		func.check(v[1], op2.getValue());
		func.check(v[1], op3.getValue());
		
		// now unbind 
		func.unbind(op1, op2);
		System.gc(); // making sure we did not not overdo weak references
		func.check(v[1], op1.getValue());
		func.check(v[1], op2.getValue());
		func.check(v[1], op3.getValue());
		
		op1.setValue(v[2]);
		func.check(v[2], op1.getValue());
		func.check(v[1], op2.getValue());
		func.check(v[1], op3.getValue());
		
		op2.setValue(v[3]);
		func.check(v[2], op1.getValue());
		func.check(v[3], op2.getValue());
		func.check(v[3], op3.getValue());
		
		op3.setValue(v[0]);
		func.check(v[2], op1.getValue());
		func.check(v[0], op2.getValue());
		func.check(v[0], op3.getValue());
	}
	
	@Test
	public void testWeakReferencing() {
		func.bind(op1, op2);
		assertEquals(1, op1.getListenerCount());
		assertEquals(1, op2.getListenerCount());
		
		op1 = null;
		System.gc();
		op2.setValue(v[2]);
		assertEquals(0, op2.getListenerCount());
		
		func.bind(op2, op3);
		assertEquals(1, op2.getListenerCount());
        assertEquals(1, op3.getListenerCount());

		op3 = null;
		System.gc();
		op2.setValue(v[0]);
		assertEquals(0, op2.getListenerCount());
	}
	
	@Test
	public void testHashCode() {
		final int hc1 = func.createBindingDirectly(op1, op2).hashCode();
		final int hc2 = func.createBindingDirectly(op2, op1).hashCode();
		assertEquals(hc1, hc2);
	}
	
	
	@Test
	public void testEquals() {
		final BidirectionalBinding golden = func.createBindingDirectly(op1, op2);
		
		assertTrue(golden.equals(golden));
		assertFalse(golden.equals(null));
		assertFalse(golden.equals(op1));
		assertTrue(golden.equals(func.createBindingDirectly(op1, op2)));
		assertTrue(golden.equals(func.createBindingDirectly(op2, op1)));
		assertFalse(golden.equals(func.createBindingDirectly(op1, op3)));
		assertFalse(golden.equals(func.createBindingDirectly(op3, op1)));
		assertFalse(golden.equals(func.createBindingDirectly(op3, op2)));
		assertFalse(golden.equals(func.createBindingDirectly(op2, op3)));
	}
	
	@Test
	public void testEqualsWithGCedProperty() {
		final BidirectionalBinding binding1 = func.createBindingDirectly(op1, op2);
		final BidirectionalBinding binding2 = func.createBindingDirectly(op1, op2);
		final BidirectionalBinding binding3 = func.createBindingDirectly(op2, op1);
		final BidirectionalBinding binding4 = func.createBindingDirectly(op2, op1);
		op1 = null;
		System.gc();

		assertTrue(binding1.equals(binding1));
		assertFalse(binding1.equals(binding2));
		assertFalse(binding1.equals(binding3));

		assertTrue(binding3.equals(binding3));
		assertFalse(binding3.equals(binding1));
		assertFalse(binding3.equals(binding4));
	}
	
	@Test(expected=NullPointerException.class)
	public void testBind_Null_X() {
		func.bind(null, op2);
	}

	@Test(expected=NullPointerException.class)
	public void testBind_X_Null() {
		func.bind(op1, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBind_X_Self() {
		func.bind(op1, op1);
	}
	
	@Test(expected=NullPointerException.class)
	public void testUnbind_Null_X() {
		func.unbind(null, op2);
	}

	@Test(expected=NullPointerException.class)
	public void testUnbind_X_Null() {
		func.unbind(op1, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUnbind_X_Self() {
		func.unbind(op1, op1);
	}
	
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final Boolean[] booleanData = new Boolean[] {true, false, true, false};
        final Double[] doubleData = new Double[] {2348.2345, -92.214, -214.0214, -908.214};
        final Float[] floatData = new Float[] {-3592.9f, 234872.8347f, 3897.274f, 3958.938745f};
        final Integer[] integerData = new Integer[] {248, -9384, -234, -34};
        final Long[] longData = new Long[] {9823984L, 2908934L, -234234L, 9089234L};
        final Object[] objectData = new Object[] {new Object(), new Object(), new Object(), new Object()};
        final String[] stringData = new String[] {"A", "B", "C", "D"};

        return Arrays.asList(new Object[][] {
            // boolean
            {
                new Functions<Boolean>() {
                    @Override
                    public PropertyMock<Boolean> create() {
                        return new BooleanPropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<Boolean> op1, PropertyMock<Boolean> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<Boolean> op1, PropertyMock<Boolean> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Boolean> op1, PropertyMock<Boolean> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Boolean expected, Boolean actual) {
                    	assertEquals(expected, actual);
                    }
                },
                booleanData
            },
            // double
            {
                new Functions<Number>() {
                    @Override
                    public PropertyMock<Number> create() {
                        return new DoublePropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Number expected, Number actual) {
                    	assertEquals(expected.doubleValue(), actual.doubleValue(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            // float
            {
                new Functions<Number>() {
                    @Override
                    public PropertyMock<Number> create() {
                        return new FloatPropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Number expected, Number actual) {
                    	assertEquals(expected.floatValue(), actual.floatValue(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            // integer
            {
                new Functions<Number>() {
                    @Override
                    public PropertyMock<Number> create() {
                        return new IntegerPropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Number expected, Number actual) {
                    	assertEquals(expected.intValue(), actual.intValue());
                    }
                },
                integerData
            },
            // long
            {
                new Functions<Number>() {
                    @Override
                    public PropertyMock<Number> create() {
                        return new LongPropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Number> op1, PropertyMock<Number> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Number expected, Number actual) {
                    	assertEquals(expected.longValue(), actual.longValue());
                    }
                },
                longData
            },
            // object
            {
                new Functions<Object>() {
                    @Override
                    public PropertyMock<Object> create() {
                        return new ObjectPropertyMock<Object>();
                    }
					@Override
                    public void bind(PropertyMock<Object> op1, PropertyMock<Object> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
					@Override
                    public void unbind(PropertyMock<Object> op1, PropertyMock<Object> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
					@Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<Object> op1, PropertyMock<Object> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(Object expected, Object actual) {
                    	assertEquals(expected, actual);
                    }
                },
                objectData
            },
            // string
            {
                new Functions<String>() {
                    @Override
                    public PropertyMock<String> create() {
                        return new StringPropertyMock();
                    }
                    @Override
                    public void bind(PropertyMock<String> op1, PropertyMock<String> op2) {
                        Bindings.bindBidirectional(op1, op2);
                    }
                    @Override
                    public void unbind(PropertyMock<String> op1, PropertyMock<String> op2) {
                        Bindings.unbindBidirectional(op1, op2);
                    }
                    @Override
                    public BidirectionalBinding createBindingDirectly(PropertyMock<String> op1, PropertyMock<String> op2) {
                        return BidirectionalBinding.bind(op1, op2);
                    }
                    @Override
                    public void check(String expected, String actual) {
                    	assertEquals(expected, actual);
                    }
                },
                stringData
            },
        });
    }
    
    private interface PropertyMock<T> extends Property<T> {
        int getListenerCount();
    }
    
    private static class BooleanPropertyMock extends SimpleBooleanProperty implements PropertyMock<Boolean> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super Boolean> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super Boolean> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class DoublePropertyMock extends SimpleDoubleProperty implements PropertyMock<Number> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super Number> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super Number> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class FloatPropertyMock extends SimpleFloatProperty implements PropertyMock<Number> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super Number> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super Number> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class IntegerPropertyMock extends SimpleIntegerProperty implements PropertyMock<Number> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super Number> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super Number> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class LongPropertyMock extends SimpleLongProperty implements PropertyMock<Number> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super Number> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super Number> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class ObjectPropertyMock<T> extends SimpleObjectProperty<T> implements PropertyMock<T> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override
        public void addListener(ChangeListener<? super T> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super T> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
    
    private static class StringPropertyMock extends SimpleStringProperty implements PropertyMock<String> {
        
        private int listenerCount = 0;
        
        @Override
        public int getListenerCount() {
            return listenerCount;
        }
        
        @Override 
        public void addListener(ChangeListener<? super String> listener) {
            super.addListener(listener);
            listenerCount++;
        }
        
        @Override 
        public void removeListener(ChangeListener<? super String> listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
}
