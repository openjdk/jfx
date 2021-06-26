/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import com.sun.javafx.binding.BidirectionalBinding;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BidirectionalBindingWithConversionTest<S, T> {

    public static interface Functions<U, V> {
        PropertyMock<U> create0();
        PropertyMock<V> create1();
        void bind(PropertyMock<U> obj0, PropertyMock<V> obj1);
        void unbind(Object obj0, Object obj1);
        Object createBindingDirectly(PropertyMock<U> op0, PropertyMock<V> op1);
        void check0(U obj0, U obj1);
        void check1(V obj0, V obj1);
    }

    private final Functions<S, T> func;
    private final S[] v0;
    private final T[] v1;

    private PropertyMock<S> op0;
    private PropertyMock<T> op1;

    public BidirectionalBindingWithConversionTest(Functions<S, T> func, S[] v0, T[] v1) {
        this.op0 = func.create0();
        this.op1 = func.create1();
        this.func = func;
        this.v0 = v0;
        this.v1 = v1;
    }

    @Before
    public void setUp() {
        op0.setValue(v0[0]);
        op1.setValue(v1[1]);
    }

    @Test
    public void testBind() {
        func.bind(op0, op1);
        System.gc(); // making sure we did not not overdo weak references
        func.check0(v0[1], op0.getValue());
        func.check1(v1[1], op1.getValue());

        op0.setValue(v0[2]);
        func.check0(v0[2], op0.getValue());
        func.check1(v1[2], op1.getValue());

        op1.setValue(v1[3]);
        func.check0(v0[3], op0.getValue());
        func.check1(v1[3], op1.getValue());
    }

    @Test
    public void testUnbind() {
        // unbind non-existing binding => no-op
        func.unbind(op0, op1);

        // unbind properties of different beans
        func.bind(op0, op1);
        System.gc(); // making sure we did not not overdo weak references
        func.check0(v0[1], op0.getValue());
        func.check1(v1[1], op1.getValue());

        func.unbind(op0, op1);
        System.gc();
        func.check0(v0[1], op0.getValue());
        func.check1(v1[1], op1.getValue());

        op0.setValue(v0[2]);
        func.check0(v0[2], op0.getValue());
        func.check1(v1[1], op1.getValue());

        op1.setValue(v1[3]);
        func.check0(v0[2], op0.getValue());
        func.check1(v1[3], op1.getValue());
    }

    @Test
    public void testWeakReferencing() {
        func.bind(op0, op1);
        assertEquals(1, op0.getListenerCount());
        assertEquals(1, op1.getListenerCount());

        op0 = null;
        System.gc();
        op1.setValue(v1[2]);
        assertEquals(0, op1.getListenerCount());

        this.op0 = func.create0();
        func.bind(op0, op1);
        assertEquals(1, op0.getListenerCount());
        assertEquals(1, op1.getListenerCount());

        op1 = null;
        System.gc();
        op0.setValue(v0[3]);
        assertEquals(0, op0.getListenerCount());
    }

    @Test(expected=NullPointerException.class)
    public void testBind_Null_X() {
        func.bind(null, op1);
    }

    @Test(expected=NullPointerException.class)
    public void testBind_X_Null() {
        func.bind(op0, null);
    }

    @Test(expected=NullPointerException.class)
    public void testUnbind_Null_X() {
        func.unbind(null, op1);
    }

    @Test(expected=NullPointerException.class)
    public void testUnbind_X_Null() {
        func.unbind(op0, null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnbind_X_Self() {
        func.unbind(op0, op0);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        final Date[] dates = new Date[] {new Date(), new Date(0), new Date(Integer.MAX_VALUE), new Date(Long.MAX_VALUE)};
        final String[] strings = new String[] {format.format(dates[0]), format.format(dates[1]), format.format(dates[2]), format.format(dates[3])};

        final StringConverter<Date> converter = new StringConverter<Date>() {
            @Override
            public String toString(Date object) {
                return format.format(object);
            }

            @Override
            public Date fromString(String string) {
                try {
                    return format.parse(string);
                } catch (ParseException e) {
                    return null;
                }
            }
        };

        return Arrays.asList(new Object[][] {
            // Format
            {
                new Functions<String, Date>() {
                    @Override
                    public PropertyMock<String> create0() {
                        return new StringPropertyMock();
                    }
                    @Override
                    public PropertyMock<Date> create1() {
                        return new ObjectPropertyMock<Date>();
                    }
                    @Override
                    public void bind(PropertyMock<String> op0, PropertyMock<Date> op1) {
                        Bindings.bindBidirectional(op0, op1, format);
                    }
                    @Override
                    public void unbind(Object op0, Object op1) {
                        Bindings.unbindBidirectional(op0, op1);
                    }
                    @Override
                    public Object createBindingDirectly(PropertyMock<String> op0, PropertyMock<Date> op1) {
                        return BidirectionalBinding.bind(op0, op1, format);
                    }

                    @Override
                    public void check0(String obj0, String obj1) {
                        assertEquals(obj0, obj1);
                    }

                    @Override
                    public void check1(Date obj0, Date obj1) {
                        assertEquals(obj0.toString(), obj1.toString());
                    }
                },
                strings,
                dates
            },
            // Converter
            {
                new Functions<String, Date>() {
                    @Override
                    public PropertyMock<String> create0() {
                        return new StringPropertyMock();
                    }
                    @Override
                    public PropertyMock<Date> create1() {
                        return new ObjectPropertyMock<Date>();
                    }
                    @Override
                    public void bind(PropertyMock<String> op0, PropertyMock<Date> op1) {
                        Bindings.bindBidirectional(op0, op1, converter);
                    }
                    @Override
                    public void unbind(Object op0, Object op1) {
                        Bindings.unbindBidirectional(op0, op1);
                    }
                    @Override
                    public Object createBindingDirectly(PropertyMock<String> op0, PropertyMock<Date> op1) {
                        return BidirectionalBinding.bind(op0, op1, converter);
                    }

                    @Override
                    public void check0(String obj0, String obj1) {
                        assertEquals(obj0, obj1);
                    }

                    @Override
                    public void check1(Date obj0, Date obj1) {
                        assertEquals(obj0.toString(), obj1.toString());
                    }
                },
                strings,
                dates
            },
        });
    }

    private interface PropertyMock<T> extends Property<T> {
        int getListenerCount();
    }

    private static class ObjectPropertyMock<T> extends SimpleObjectProperty<T> implements PropertyMock<T> {

        private int listenerCount = 0;

        @Override
        public int getListenerCount() {
            return listenerCount;
        }

        @Override
        public void addListener(InvalidationListener listener) {
            super.addListener(listener);
            listenerCount++;
        }

        @Override
        public void removeListener(InvalidationListener listener) {
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
        public void addListener(InvalidationListener listener) {
            super.addListener(listener);
            listenerCount++;
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            super.removeListener(listener);
            listenerCount--;
        }
    }
}
