/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javafx.beans.InvalidationListenerMock;
import javafx.beans.value.ChangeListenerMock;
import javafx.beans.value.ObservableDoubleValueStub;
import javafx.beans.value.ObservableValueStub;

import org.junit.Before;
import org.junit.Test;

public class DoublePropertyBaseTest {

    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final Double UNDEFINED = Double.MAX_VALUE;
	private static final double EPSILON = 1e-12;
	
	private DoublePropertyMock property;
	private InvalidationListenerMock invalidationListener;
	private ChangeListenerMock<Number> changeListener;

	@Before
	public void setUp() throws Exception {
		property = new DoublePropertyMock();
		invalidationListener = new InvalidationListenerMock();
        changeListener = new ChangeListenerMock<Number>(UNDEFINED);
	}

	private void attachInvalidationListener() {
		property.addListener(invalidationListener);
		property.get();
		invalidationListener.reset();
	}

	private void attachChangeListener() {
		property.addListener(changeListener);
		property.get();
		changeListener.reset();
	}

	@Test
	public void testConstructor() {
		final DoubleProperty p1 = new SimpleDoubleProperty();
		assertEquals(0.0, p1.get(), EPSILON);
		assertEquals(Double.valueOf(0.0), p1.getValue(), EPSILON);
		assertFalse(property.isBound());
		
		final DoubleProperty p2 = new SimpleDoubleProperty(-Math.PI);
		assertEquals(-Math.PI, p2.get(), EPSILON);
		assertEquals(Double.valueOf(-Math.PI), p2.getValue(), EPSILON);
		assertFalse(property.isBound());
	}

    @Test
    public void testInvalidationListener() {
        attachInvalidationListener();
        property.set(Math.E);
        invalidationListener.check(property, 1);
        property.removeListener(invalidationListener);
        invalidationListener.reset();
        property.set(Math.PI);
        invalidationListener.check(null, 0);
    }

    @Test
    public void testChangeListener() {
        attachChangeListener();
        property.set(Math.E);
        changeListener.check(property, 0.0, Math.E, 1);
        property.removeListener(changeListener);
        changeListener.reset();
        property.set(Math.PI);
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
    }

	@Test
	public void testLazySet() {
		attachInvalidationListener();

		// set value once
		property.set(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(1);
		invalidationListener.check(property, 1);

		// set same value again
		property.set(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(0);
		invalidationListener.check(null, 0);

		// set value twice without reading
		property.set(Math.PI);
		property.set(-Math.PI);
		assertEquals(-Math.PI, property.get(), EPSILON);
		property.check(1);
		invalidationListener.check(property, 1);
	}

	@Test
	public void testEagerSet() {
		attachChangeListener();

		// set value once
		property.set(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(1);
		changeListener.check(property, 0.0, Math.E, 1);

		// set same value again
		property.set(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(0);
		changeListener.check(null, UNDEFINED, UNDEFINED, 0);

		// set value twice without reading
		property.set(Math.PI);
		property.set(-Math.PI);
		assertEquals(-Math.PI, property.get(), EPSILON);
		property.check(2);
		changeListener.check(property, Math.PI, -Math.PI, 2);
	}

	@Test
	public void testLazySetValue() {
		attachInvalidationListener();

		// set value once
		property.setValue(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(1);
		invalidationListener.check(property, 1);

		// set same value again
		property.setValue(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(0);
		invalidationListener.check(null, 0);

		// set value twice without reading
		property.setValue(Math.PI);
		property.setValue(-Math.PI);
		assertEquals(-Math.PI, property.get(), EPSILON);
		property.check(1);
		invalidationListener.check(property, 1);
	}

	@Test
	public void testEagerSetValue() {
		attachChangeListener();

		// set value once
		property.setValue(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(1);
		changeListener.check(property, 0.0, Math.E, 1);

		// set same value again
		property.setValue(Math.E);
		assertEquals(Math.E, property.get(), EPSILON);
		property.check(0);
		changeListener.check(null, UNDEFINED, UNDEFINED, 0);

		// set value twice without reading
		property.setValue(Math.PI);
		property.setValue(-Math.PI);
		assertEquals(-Math.PI, property.get(), EPSILON);
		property.check(2);
		changeListener.check(property, Math.PI, -Math.PI, 2);
	}

	@Test(expected=RuntimeException.class)
	public void testSetBoundValue() {
		final DoubleProperty v = new SimpleDoubleProperty(Math.PI);
		property.bind(v);
		property.set(Math.PI);
	}

    @Test
    public void testLazyBind() {
        attachInvalidationListener();
        final ObservableDoubleValueStub v = new ObservableDoubleValueStub(Math.PI);

        property.bind(v);
        assertEquals(Math.PI, property.get(), EPSILON);
        assertTrue(property.isBound());
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding once
        v.set(Math.E);
        assertEquals(Math.E, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice without reading
        v.set(Math.PI);
        v.set(-Math.PI);
        assertEquals(-Math.PI, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice to same value
        v.set(Math.PI);
        v.set(Math.PI);
        assertEquals(Math.PI, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testEagerBind() {
        attachChangeListener();
        final ObservableDoubleValueStub v = new ObservableDoubleValueStub(Math.PI);

        property.bind(v);
        assertEquals(Math.PI, property.get(), EPSILON);
        assertTrue(property.isBound());
        property.check(1);
        changeListener.check(property, 0.0, Math.PI, 1);

        // change binding once
        v.set(Math.E);
        assertEquals(Math.E, property.get(), EPSILON);
        property.check(1);
        changeListener.check(property, Math.PI, Math.E, 1);

        // change binding twice without reading
        v.set(Math.PI);
        v.set(-Math.PI);
        assertEquals(-Math.PI, property.get(), EPSILON);
        property.check(2);
        changeListener.check(property, Math.PI, -Math.PI, 2);

        // change binding twice to same value
        v.set(Math.PI);
        v.set(Math.PI);
        assertEquals(Math.PI, property.get(), EPSILON);
        property.check(2);
        changeListener.check(property, -Math.PI, Math.PI, 1);
    }

    @Test
    public void testLazyBindObservableValue() {
        final double value1 = Math.PI;
        final double value2 = Math.E;
        attachInvalidationListener();
        final ObservableValueStub<Number> v = new ObservableValueStub<Number>(value1);

        property.bind(v);
        assertEquals(value1, property.get(), EPSILON);
        assertTrue(property.isBound());
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding once
        v.set(value2);
        assertEquals(value2, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice without reading
        v.set(value1);
        v.set(value2);
        assertEquals(value2, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);

        // change binding twice to same value
        v.set(value1);
        v.set(value1);
        assertEquals(value1, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);

        // set binding to null
        v.set(null);
        assertEquals(0.0, property.get(), EPSILON);
        property.check(1);
        invalidationListener.check(property, 1);
    }

    @Test
    public void testEagerBindObservableValue() {
        final double value1 = Math.PI;
        final double value2 = Math.E;
        attachChangeListener();
        final ObservableValueStub<Number> v = new ObservableValueStub<Number>(value1);

        property.bind(v);
        assertEquals(value1, property.get(), EPSILON);
        assertTrue(property.isBound());
        property.check(1);
        changeListener.check(property, 0.0, value1, 1);

        // change binding once
        v.set(value2);
        assertEquals(value2, property.get(), EPSILON);
        property.check(1);
        changeListener.check(property, value1, value2, 1);

        // change binding twice without reading
        v.set(value1);
        v.set(value2);
        assertEquals(value2, property.get(), EPSILON);
        property.check(2);
        changeListener.check(property, value1, value2, 2);

        // change binding twice to same value
        v.set(value1);
        v.set(value1);
        assertEquals(value1, property.get(), EPSILON);
        property.check(2);
        changeListener.check(property, value2, value1, 1);

        // set binding to null
        v.set(null);
        assertEquals(0.0, property.get(), EPSILON);
        property.check(1);
        changeListener.check(property, value1, 0.0, 1);
    }

	@Test(expected=NullPointerException.class)
	public void testBindToNull() {
		property.bind(null);
	}

	@Test
	public void testRebind() {
		attachInvalidationListener();
		final DoubleProperty v1 = new SimpleDoubleProperty(Math.PI);
		final DoubleProperty v2 = new SimpleDoubleProperty(Math.E);
		property.bind(v1);
		property.get();
		property.reset();
		invalidationListener.reset();
		
		// rebind causes invalidation event
		property.bind(v2);
		assertEquals(Math.E, property.get(), EPSILON);
		assertTrue(property.isBound());
		assertEquals(1, property.counter);
		invalidationListener.check(property, 1);
		property.reset();
		
		// change old binding
		v1.set(-Math.PI);
		assertEquals(Math.E, property.get(), EPSILON);
		assertEquals(0, property.counter);
		invalidationListener.check(null, 0);
		property.reset();
		
		// change new binding
		v2.set(-Math.E);
		assertEquals(-Math.E, property.get(), EPSILON);
		assertEquals(1, property.counter);
		invalidationListener.check(property, 1);
		property.reset();
		
		// rebind to same observable should have no effect
		property.bind(v2);
		assertEquals(-Math.E, property.get(), EPSILON);
		assertTrue(property.isBound());
		assertEquals(0, property.counter);
		invalidationListener.check(null, 0);
	}

    @Test
    public void testUnbind() {
        attachInvalidationListener();
        final DoubleProperty v = new SimpleDoubleProperty(Math.PI);
        property.bind(v);
        property.unbind();
        assertEquals(Math.PI, property.get(), EPSILON);
        assertFalse(property.isBound());
        property.reset();
        invalidationListener.reset();
        
        // change binding
        v.set(Math.E);
        assertEquals(Math.PI, property.get(), EPSILON);
        assertEquals(0, property.counter);
        invalidationListener.check(null, 0);
        property.reset();
        
        // set value
        property.set(-Math.PI);
        assertEquals(-Math.PI, property.get(), EPSILON);
        assertEquals(1, property.counter);
        invalidationListener.check(property, 1);
    }
    
    @Test
    public void testUnbindObservableValue() {
        final double value1 = Math.PI;
        final double value2 = Math.E;
        
        attachInvalidationListener();
        final ObservableValueStub<Number> v = new ObservableValueStub<Number>(value1);
        property.bind(v);
        property.unbind();
        assertEquals(value1, property.get(), EPSILON);
        assertFalse(property.isBound());
        property.reset();
        invalidationListener.reset();
        
        // change binding
        v.set(value2);
        assertEquals(value1, property.get(), EPSILON);
        assertEquals(0, property.counter);
        invalidationListener.check(null, 0);
        property.reset();
        
        // set value
        property.set(value2);
        assertEquals(value2, property.get(), EPSILON);
        assertEquals(1, property.counter);
        invalidationListener.check(property, 1);
    }
    
	@Test
	public void testAddingListenerWillAlwaysReceiveInvalidationEvent() {
		final DoubleProperty v = new SimpleDoubleProperty(Math.PI);
		final InvalidationListenerMock listener2 = new InvalidationListenerMock();
		final InvalidationListenerMock listener3 = new InvalidationListenerMock();

		// setting the property
		property.set(Math.PI);
		property.addListener(listener2);
		listener2.reset();
		property.set(-Math.PI);
		listener2.check(property, 1);
		
		// binding the property
		property.bind(v);
		v.set(Math.E);
		property.addListener(listener3);
		v.get();
		listener3.reset();
		v.set(-Math.E);
		listener3.check(property, 1);
	}
		
	@Test
	public void testToString() {
		final double value1 = Math.E;
		final double value2 = -Math.PI;
		final DoubleProperty v = new SimpleDoubleProperty(value2);
		
		property.set(value1);
		assertEquals("DoubleProperty [value: " + value1 + "]", property.toString());
		
		property.bind(v);
		assertEquals("DoubleProperty [bound, invalid]", property.toString());
		property.get();
		assertEquals("DoubleProperty [bound, value: " + value2 + "]", property.toString());
		v.set(value1);
		assertEquals("DoubleProperty [bound, invalid]", property.toString());
		property.get();
		assertEquals("DoubleProperty [bound, value: " + value1 + "]", property.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final DoubleProperty v1 = new DoublePropertyMock(bean, name);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", name: My name, value: " + 0.0 + "]", v1.toString());
        v1.set(value1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", name: My name, value: " + value1 + "]", v1.toString());
        
        final DoubleProperty v2 = new DoublePropertyMock(bean, NO_NAME_1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + 0.0 + "]", v2.toString());
        v2.set(value1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + value1 + "]", v2.toString());

        final DoubleProperty v3 = new DoublePropertyMock(bean, NO_NAME_2);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + 0.0 + "]", v3.toString());
        v3.set(value1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + value1 + "]", v3.toString());

        final DoubleProperty v4 = new DoublePropertyMock(NO_BEAN, name);
        assertEquals("DoubleProperty [name: My name, value: " + 0.0 + "]", v4.toString());
        v4.set(value1);
        assertEquals("DoubleProperty [name: My name, value: " + value1 + "]", v4.toString());
	}
	
	private static class DoublePropertyMock extends DoublePropertyBase {
        
        private final Object bean;
        private final String name;
		private int counter;
		
        private DoublePropertyMock() {
            this.bean = NO_BEAN;
            this.name = NO_NAME_1;
        }
        
        private DoublePropertyMock(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }
        
		@Override
		protected void invalidated() {
			counter++;
		}
		
		private void check(int expected) {
			assertEquals(expected, counter);
			reset();
		}

		private void reset() {
			counter = 0;
		}

        @Override public Object getBean() { return bean; }

        @Override public String getName() { return name; }
	}

}
